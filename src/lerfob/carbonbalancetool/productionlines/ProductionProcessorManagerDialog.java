/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed with the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * Please see the license at http://www.gnu.org/copyleft/lesser.html.
 */
package lerfob.carbonbalancetool.productionlines;

import java.awt.BorderLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import lerfob.carbonbalancetool.CATAWTProperty;
import lerfob.carbonbalancetool.CATFrame;
import lerfob.carbonbalancetool.productionlines.LandfillProcessor.LandfillProcessorButton;
import lerfob.carbonbalancetool.productionlines.LeftInForestProcessor.LeftInForestProcessorButton;
import lerfob.carbonbalancetool.productionlines.LogCategoryProcessor.LogCategoryProcessorButton;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager.ImportFormat;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorToolPanel.CreateEndOfLifeLinkButton;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorToolPanel.CreateLandfillProcessorButton;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorToolPanel.CreateLeftInForestProcessorButton;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorToolPanel.CreateProductionLineProcessorButton;
import lerfob.carbonbalancetool.productionlines.WoodyDebrisProcessor.WoodyDebrisProcessorButton;
import repicea.app.SettingMemory;
import repicea.gui.AutomatedHelper;
import repicea.gui.CommonGuiUtility;
import repicea.gui.CommonGuiUtility.FileChooserOutput;
import repicea.gui.OwnedWindow;
import repicea.gui.REpiceaAWTEvent;
import repicea.gui.REpiceaAWTProperty;
import repicea.gui.UIControlManager;
import repicea.gui.components.REpiceaComboBoxOpenButton;
import repicea.io.REpiceaFileFilter;
import repicea.io.REpiceaFileFilterList;
import repicea.serial.SerializerChangeMonitor;
import repicea.simulation.processsystem.Processor;
import repicea.simulation.processsystem.ProcessorListTable;
import repicea.simulation.processsystem.SystemLayout;
import repicea.simulation.processsystem.SystemManagerDialog;
import repicea.simulation.processsystem.SystemPanel;
import repicea.simulation.processsystem.UISetup;
import repicea.simulation.treelogger.TreeLoggerParameters;
import repicea.simulation.treelogger.TreeLoggerParametersDialog;
import repicea.util.BrowserCaller;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class ProductionProcessorManagerDialog extends SystemManagerDialog implements OwnedWindow, 
																					PropertyChangeListener,
																					ItemListener {
	
	static {
		SerializerChangeMonitor.registerClassNameChange("lerfob.carbonbalancetool.productionlines.BroadleavedExtractionProcessor", "lerfob.carbonbalancetool.productionlines.StatusSpeciesTypeExtractionProcessor");
	}
	
	static {
		UISetup.Icons.put(CreateLeftInForestProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "IconLeftInForest.png"));
		UISetup.Icons.put(LeftInForestProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "IconLeftInForest.png"));
		UISetup.Icons.put(CreateLandfillProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "IconLandfillSite.png"));
		UISetup.Icons.put(LandfillProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "IconLandfillSite.png"));
		UISetup.Icons.put(CreateEndOfLifeLinkButton.class.getName(),CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "endOfLifelinkIcon.png"));
		UISetup.Icons.put(LogCategoryProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "logIcon.png"));
		UISetup.Icons.put(WoodyDebrisProcessorButton.class.getName(), CommonGuiUtility.retrieveIcon(ProductionProcessorManagerDialog.class, "logIcon.png"));
		UIControlManager.setTitle(ProductionProcessorManagerDialog.class, "Flux manager", "Gestionnaire de flux");
		
		try {
			Method callHelp = BrowserCaller.class.getMethod("openUrl", String.class);
			String url = "https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20Flux%20Manager/";
			AutomatedHelper helper = new AutomatedHelper(callHelp, new Object[]{url});
			UIControlManager.setHelpMethod(ProductionProcessorManagerDialog.class, helper);
		} catch (Exception e) {}

	}


	public static enum MessageID implements TextableEnum {
		LandFillMarketLabel("Solid waste disposal site", "Site d'\u00E9mination des d\u00E9chets solides"),
		LeftInForestLabel("Dead organic matter", "Mati\u00E8re organique morte"),
		ProductionLineFileExtension("Flux configuration file (*.prl)", "fichier de configuration de flux (*.prl)"),
		Default("Default","D\u00E9faut"),
		BuckingModelLabel("Bucking module", "Mod\u00E8le de billonnage"),
		ProcessorButtonToolTip("Create a processor", "Cr\u00E9er un processeur"),
		LandfillButtonToolTip("Create a solid waste disposal site", "Cr\u00E9er un site d'\u00E9mination des d\u00E9chets solides"),
		LeftInForestButtonToolTip("Leave on forest floor", "Laisser en for\u00EAt"),
		EndOfLifeLinkButtonToolTip("End of life destination", "Destination en fin de vie"),
		IncompatibleTreeLogger("The tree logger is incompatible and will be replaced by the default tree logger!", "Le module de billonnage est incompatible et sera remplac\u00E9 par le module de billonnage par d\u00E9faut!"),
		ExamplesOfFluxConfigurations("Examples of flux configurations", "Exemples de configurations de flux"),
		ImportFromOtherSources("Import", "Importer"),
		ImportFromAFFiliere("From AFFiliere (.json)", "A partir d'AFFili\u00E8ere (.json)"),
		ErrorImportFromAffiliere("An error occurred while importing data from AFFiliere", "Une erreur est survenue lors de l'importation des donn\u00E9es depuis AFFili\u00E8re"),
		ErrorExportFromAffiliere("An error occurred while exporting data to AFFiliere", "Une erreur est survenue lors de l'exportation des donn\u00E9es vers AFFili\u00E8re"),
		ExportToAFFiliere("To AFFiliere (.xlsx)", "Vers AFFili\u00E8ere (.xlsx)");
		
		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}

		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString (this, englishText, frenchText);
		}

		@Override
		public String toString() {
			return REpiceaTranslator.getString(this);
		}
	}
	
	static class InternalProcessorListTable extends ProcessorListTable {

		InternalProcessorListTable(ProductionProcessorManager caller) {
			super(caller);
		}
		
		protected List<Processor> getProcessorList() {
			List<Processor> processors = new ArrayList<Processor>();
			for (Processor p : caller.getList()) {
				if (p instanceof AbstractProductionLineProcessor || p instanceof AbstractExtractionProcessor) {
					processors.add(p);
				}
			}
			return processors;
		}
		
	}
	
	

	static {
		UISetup.ToolTips.put(CreateProductionLineProcessorButton.class.getName(), MessageID.ProcessorButtonToolTip.toString());
		UISetup.ToolTips.put(CreateEndOfLifeLinkButton.class.getName(), MessageID.EndOfLifeLinkButtonToolTip.toString());
		UISetup.ToolTips.put(CreateLeftInForestProcessorButton.class.getName(), MessageID.LeftInForestButtonToolTip.toString());
		UISetup.ToolTips.put(CreateLandfillProcessorButton.class.getName(), MessageID.LandfillButtonToolTip.toString());
	}

	
	protected REpiceaComboBoxOpenButton<TreeLoggerParameters<?>> comboBoxPanel;
	private JComboBox<TreeLoggerParameters<?>> treeLoggerComboBox;
	
	protected JMenuItem downloadExamplesMenuItem;
	protected JMenu importFromOtherSourcesMenu;
	protected JMenuItem importFromAFFiliere;
//	protected JMenuItem exportToAFFiliere;
	
	
	/**
	 * Constructor.
	 * @param parent the parent window
	 * @param systemManager the ProductionProcessorManager instance whose parameters are to be displayed
	 */
	protected ProductionProcessorManagerDialog(Window parent, ProductionProcessorManager systemManager) {
		super(parent, systemManager);
	}
	
	@Override
	protected void setToolPanel() {
		toolPanel = new ProductionProcessorToolPanel(systemPanel);
	}
	

	@Override
	protected void init() {
		super.init();
		comboBoxPanel = new REpiceaComboBoxOpenButton<TreeLoggerParameters<?>>(MessageID.BuckingModelLabel, getCaller().getGUIPermission());
		treeLoggerComboBox = comboBoxPanel.getComboBox();
		downloadExamplesMenuItem = new JMenuItem(MessageID.ExamplesOfFluxConfigurations.toString());
		importFromOtherSourcesMenu = UIControlManager.createCommonMenu(MessageID.ImportFromOtherSources);
		importFromAFFiliere = UIControlManager.createCommonMenuItem(MessageID.ImportFromAFFiliere);
//		exportToAFFiliere = UIControlManager.createCommonMenuItem(MessageID.ExportToAFFiliere);
	}

	@Override
	protected void initUI() {
		initTreeLoggerComboBox();
		super.initUI();
	}

	@Override
	protected void addComponentsToFluxViewPanel() {
		fluxViewPanel.add(comboBoxPanel, BorderLayout.NORTH);
		super.addComponentsToFluxViewPanel();
	}
	
	@Override
	public void setVisible(boolean bool) {
		super.setVisible(bool);
		Window w = getOwner();
		if (w != null) {
			if (bool) {
				w.setEnabled(false);
			} else {
				w.setEnabled(true);
			}
		}
	}
	
	protected ProcessorListTable createProcessorListPanel() {
		return new InternalProcessorListTable(getCaller());
	}
	
	
	@Override
	protected JMenu createFileMenu() {
		JMenu fileMenu = super.createFileMenu();
//		export.add(this.exportToAFFiliere);
		importFromOtherSourcesMenu.add(importFromAFFiliere);
		fileMenu.insert(importFromOtherSourcesMenu, 4);
		return fileMenu;
	}
	
	@Override
	protected JMenu createAboutMenu() {
		JMenu about = super.createAboutMenu();
		about.add(downloadExamplesMenuItem);
		return about;
	}
		
	@Override
	protected ProductionProcessorManager getCaller() {
		return (ProductionProcessorManager) super.getCaller();
	}

	protected void initTreeLoggerComboBox() {
		Vector<TreeLoggerParameters<?>> modelValues = new Vector<TreeLoggerParameters<?>>();
		modelValues.addAll(getCaller().getAvailableTreeLoggerParameters());
		treeLoggerComboBox.setModel(new DefaultComboBoxModel<TreeLoggerParameters<?>>(modelValues));
	}

	@Override
	protected SystemPanel createSystemPanel() {
		return new ExtendedSystemPanel(getCaller(), createSystemLayout());
	}
	
	@Override
	protected SystemLayout createSystemLayout() {
		return new ExtendedSystemLayout();
	}
	
	@Override
	public void listenTo() {
		super.listenTo();
		treeLoggerComboBox.addItemListener(this);
		comboBoxPanel.addComboBoxEntryPropertyListener(this);
		downloadExamplesMenuItem.addActionListener(this);
		importFromAFFiliere.addActionListener(this);
//		exportToAFFiliere.addActionListener(this);
	}
	
	@Override
	public void doNotListenToAnymore() {
		super.doNotListenToAnymore();
		treeLoggerComboBox.removeItemListener(this);
		comboBoxPanel.removeComboBoxEntryPropertyListener(this);
		downloadExamplesMenuItem.removeActionListener(this);
		importFromAFFiliere.removeActionListener(this);
//		exportToAFFiliere.removeActionListener(this);
	}

	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(treeLoggerComboBox) && arg0.getStateChange () == ItemEvent.SELECTED) {
			getCaller().setSelectedTreeLogger((TreeLoggerParameters<?>) treeLoggerComboBox.getSelectedItem());
			firePropertyChange(REpiceaAWTProperty.SynchronizeWithOwner, null, this);
			firePropertyChange(REpiceaAWTProperty.ActionPerformed, null, this);
		} else {
			super.itemStateChanged(arg0);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource().equals(downloadExamplesMenuItem)) {
			downloadExamplesAction();
		} else if (evt.getSource().equals(importFromAFFiliere)) {
			SettingMemory sm = getSettingMemory();
			String importFilename = sm != null ?
					sm.getProperty("importToAffiliere.filename", "") :
						"";
			FileChooserOutput fileChooserOutput = CommonGuiUtility.browseAction(this,
					JFileChooser.FILES_ONLY,
					importFilename,
					new REpiceaFileFilterList(REpiceaFileFilter.JSON),
					JFileChooser.OPEN_DIALOG);
			if (fileChooserOutput.isValid()) {
				if (sm != null) {
					sm.setProperty("importToAffiliere.filename", fileChooserOutput.getFilename());
				}
				try {
					getCaller().importFrom(fileChooserOutput.getFilename(), ImportFormat.AFFILIERE);
					REpiceaAWTEvent.fireEvent(new REpiceaAWTEvent(this, CATAWTProperty.AffiliereImportSuccessful));
					synchronizeUIWithOwner();
				} catch (Exception e) {
					String message = MessageID.ErrorImportFromAffiliere.toString() + System.lineSeparator() + e.getMessage();
					ProductionProcessorManager.getLogger().log(Level.SEVERE, message, e);
					CommonGuiUtility.showErrorMessage(message, this);
				}
			}
//		} else if (evt.getSource().equals(exportToAFFiliere)) {
//			SettingMemory sm = getSettingMemory();
//			String exportFilename = sm != null ?
//					sm.getProperty("exportToAffiliere.filename", "") :
//						"";
//			FileChooserOutput fileChooserOutput = CommonGuiUtility.browseAction(this,
//					JFileChooser.FILES_ONLY,
//					exportFilename, 
//					new REpiceaFileFilterList(REpiceaFileFilter.XLSX),
//					JFileChooser.SAVE_DIALOG);
//			if (fileChooserOutput.isValid()) {
//				if (sm != null) {
//					sm.setProperty("exportToAffiliere.filename", fileChooserOutput.getFilename());
//				}
//				try {
//					getCaller().exportTo(fileChooserOutput.getFilename(), ExportFormat.AFFILIERE);
//					REpiceaAWTEvent.fireEvent(new REpiceaAWTEvent(this, CATAWTProperty.AffiliereExportSuccessful));
//				} catch (Exception e) {
//					String message = MessageID.ErrorExportFromAffiliere.toString() + System.lineSeparator() + e.getMessage();
//					ProductionProcessorManager.getLogger().log(Level.SEVERE, message, e);
//					CommonGuiUtility.showErrorMessage(message, this);
//				}
//			}
//			
		} else {
			super.actionPerformed(evt);
		} 
	}
	
	private SettingMemory getSettingMemory() {
		CATFrame mainFrame = (CATFrame) this.getOwner();
		return mainFrame != null ? 
			mainFrame.getSettingMemory() :
				null;
	}
	
	private void downloadExamplesAction() {
		String url = "https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20Flux%20Manager//#examples-of-complex-flux-configurations";
		BrowserCaller.openUrl(url);
	}
	
	@Override
	public void synchronizeUIWithOwner() {
		super.synchronizeUIWithOwner();
		if (isVisible()) {
			doNotListenToAnymore();
		}
		initTreeLoggerComboBox();
		treeLoggerComboBox.setSelectedItem(getCaller().getSelectedTreeLoggerParameters());
		if (isVisible()) {
			listenTo();
		}
	}
	
	
	@Override
	public void propertyChange(PropertyChangeEvent arg0) {
		if (arg0.getSource() != null) {
			if (TreeLoggerParametersDialog.class.isAssignableFrom(arg0.getSource().getClass()) && arg0.getPropertyName().equals(REpiceaAWTProperty.WindowAcceptedConfirmed.name())) {
				if (!getCaller().getSelectedTreeLoggerParameters().isParameterDialogCanceled()) {
					getCaller().setSelectedTreeLogger((TreeLoggerParameters<?>) treeLoggerComboBox.getSelectedItem());
					firePropertyChange(REpiceaAWTProperty.SynchronizeWithOwner, null, this);
					firePropertyChange(REpiceaAWTProperty.ActionPerformed, null, this);
				}
			}
		}
	}


}
