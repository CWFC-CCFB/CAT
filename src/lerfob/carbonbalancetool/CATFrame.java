/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
 * Copyright (C) 2022-2024 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service
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
package lerfob.carbonbalancetool;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import lerfob.carbonbalancetool.CATSettings.AssessmentReport;
import lerfob.carbonbalancetool.CATTask.Task;
import lerfob.carbonbalancetool.CATUtility.BiomassParametersWrapper;
import lerfob.carbonbalancetool.CATUtility.ProductionProcessorManagerWrapper;
import lerfob.carbonbalancetool.io.CATGrowthSimulationRecordReader;
import lerfob.carbonbalancetool.io.CATScaleManagementSelectorDialog;
import lerfob.carbonbalancetool.io.CATSpeciesSelectionDialog;
import lerfob.carbonbalancetool.io.CATYieldTableRecordReader;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManagerException;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import repicea.app.SettingMemory;
import repicea.app.UseModeProvider.UseMode;
import repicea.gui.AutomatedHelper;
import repicea.gui.CommonGuiUtility;
import repicea.gui.CommonGuiUtility.FileChooserOutput;
import repicea.gui.REpiceaAWTProperty;
import repicea.gui.REpiceaFrame;
import repicea.gui.REpiceaLookAndFeelMenu;
import repicea.gui.Refreshable;
import repicea.gui.UIControlManager;
import repicea.gui.UIControlManager.CommonControlID;
import repicea.gui.UIControlManager.CommonMenuTitle;
import repicea.gui.components.REpiceaComboBoxOpenButton;
import repicea.gui.components.REpiceaSlider;
import repicea.gui.components.REpiceaSlider.Position;
import repicea.gui.dnd.AcceptableDropComponent;
import repicea.gui.dnd.DropTargetImpl;
import repicea.gui.dnd.LocatedEvent;
import repicea.io.REpiceaFileFilter;
import repicea.io.REpiceaFileFilterList;
import repicea.util.BrowserCaller;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The CarbonAccountingToolDialog is the UI interface of the 
 * CarbonAccountingTool class. 
 * @author M. Fortin - June 2010
 */
@SuppressWarnings({"serial"})
public class CATFrame extends REpiceaFrame implements PropertyChangeListener, 
														ItemListener,
														Refreshable, 
														AcceptableDropComponent<ArrayList<CATCompatibleStand>> {

	static {
		UIControlManager.setTitle(CATFrame.class, CarbonAccountingTool.englishTitle, CarbonAccountingTool.frenchTitle);
		
		try {
			Method callHelp = BrowserCaller.class.getMethod("openUrl", String.class);
			String url = "https://sourceforge.net/p/lerfobforesttools/wiki/CAT/";
			AutomatedHelper helper = new AutomatedHelper(callHelp, new Object[]{url});
			UIControlManager.setHelpMethod(CATFrame.class, helper);
		} catch (Exception e) {}
	}

	public static enum MessageID implements TextableEnum {
		EstimateCarbonBiomass("Carbon estimation in biomass", "Estimation du carbone de la biomasse"),
		HarvestedVolumeLabel("Harvested volume management", "Gestion des volumes r\u00E9colt\u00E9s"),
		ProductionLinesLabel("Production lines", "Lignes de production"),
		WoodDispatcherLabel("Production line supply", "Approvisionnement des lignes de production"),
		Default("<Default>","<D\u00E9faut>"),
		CalculateCarbonBalance("Calculate carbon balance", "Calculer bilan carbone"),
		ShowSomethingOptions("Show pool(s)", "Afficher le(s) pool(s)"),
		OtherOpptions("Other options", "Autres options"),
		BySpecies("By species", "Par esp\u00E8ces"),
		Component("Component", "Composante"),
		DefaultParameters("Default parameters", "Param\u00E8tres par d\u00e9faut"),
		Name("Name", "Nom"),
		CompareScenario("Compare scenarios", "Comparaison de sc\u00E9narios"),
		Status("Status", "Etat"),
		HWP_Parameters("Flux manager (DOM, HWP)", "Gestionnaire de flux (MOM, Produits bois)"),
		Biomass("Biomass parameters", "Param\u00E8tres biomasse"),
		ManageWoodSupply("Manage wood supply", "G\u00E9rer l'approvisionnement"),
		WaitingTask("Waiting", "En attente d'une t\u00E2che"),
		Compartments("Compartments", "Compartiments"),
		Settings("Parameters", "Param\u00E8tres"),
		AvailabbleTreeLoggers("Select a tree logger", "Choisir un module de billonnage"),
		JobDone("Done", "Termin\u00E9"),
		ErrorWhileComputing("An error occured while performing the task: ", 
				"Une erreur est survenue pendant le d\u00E9roulement de la t\u00E2che : "),
		IPCCBiomassParameters("IPCC parameters", "Param\u00E8tres du GIEC"),
		CITEPABiomassParameters("French reporting parameters", "Param\u00E8tres du CITEPA"),
		ModelBasedBiomassParameters("Model-based parameters","Param\u00E8tres du mod\u00E8le"),
		LoggingJob("Logging trees...", "R\u00E9colte des arbres..."),
		WoodPieceJob("Bucking trees...", "Billonnage des arbres..."),
		CarbonCompartmentJob("Calculating carbon...", "Calcul du carbone..."),
		ActualizingCarbon("Actualizing carbon units...", "Actualisation des unit\u00E9s de carbone..."),
		Bucking("Sawing", "Billonnage"),
		Edit("Edit", "Editer"),
		YouAboutToExport("Please select the simulation you want to export: ",
				"Veuillez choisir la simulation que vous d\u00E9sirez exporter ? "),
		ParametersAreInvalid("<html>The flux configuration in the flux manager is invalid for the following reason: <br>",
				"<html>La configuration des flux est incoh\u00E9rente pour la raison suivante: <br>"),
		ReadyToCompute("Ready to perform the assessment.",
				"Pr\u00EAt \u00E0 r\u00E9aliser l'\u00E9valuation."),
		Forest("Forest", "For\u00EAt"),
		SpeciesGroup("Species group", "Group d'esp\u00E8ces"),
		BroadLeaved("Broadleaved species", "Esp\u00E8ces de feuillus"),
		Evergreen("Coniferous species", "Esp\u00E8ces de conif\u00E8res"),
		AboveGroundExpansionFactor("Aboveground biomass expansion factor", "Facteur d'expansion biomasse a\u00E9rienne"),
		BelowGroundExpansionFactor("Belowground biomass expansion factor", "Facteur d'expansion biomasse souterraine"),
		BasicDensityFactors("Basic density (ton dry matter/m3 of greenwood)", "Infradensit\u00E9 (tonne mati\u00E8re anhydre/m3 bois vert)"),
				
		WoodProducts("Wood products", "Produits bois"),
		ProductType("Product type", "Type de produit"),
		ShortLived("Short lived", "Courte dur\u00E9e de vie"),
		LongLived("Long lived", "Longue dur\u00E9e de vie"),
		Proportion("Splitting", "R\u00E9partition"),
		AverageLifeTime("Average lifetime (yrs)", "Dur\u00E9e de vie moyenne (ann\u00E9es)"),
		BurnedForEnergy("Burn wood products for energy when disposed", "Incin\u00E9ration \u00E0 des fins \u00E9nerg\u00E9tiques"),

		Landfill("Landfill site", "D\u00E9charge"),
		DOCf("Degredable Organic Carbon fraction (DOCf)", "Proportion d\u00E9composable du carbone organique (DOCf)"),
		AverageDecompositionTime("Average degradation time (yrs)", "Dur\u00E9e de d\u00E9composition moyenne (ann\u00E9es)"),
		ImportStandList("You are about to import this new stand list. Do you want to proceed?", "Vous \u00EAtes sur le point d'importer cette nouvelle liste de placettes. Voulez-vous continuer ?"),
		NumberOfRunsToDo("Analyzing the realizations", "Analyse des r\u00E9alisations"), 
		PanicButton("Stop the simulation", "Arr\u00EAter la simulation"),
//		CarboneBalance("Carbon balance", "Bilan de carbone")
		CO2Eq("CO2 Eq.", "CO2 Eq."),
		CEq("C Eq.", "C Eq."),
		Units("Units", "Unit\u00E9s"),
		CI("Confidence intervals", "Intervalles de confiance"),
		SensitivityAnalysis("Sensitivity Analysis", "Analyse de sensibilit\u00E9"),
		GlobalWarmingPotential("Global Warming Potential", "Potentiel de r\u00E9chauffement global"),
		ImportYieldTable("Yield table", "Table de production"),
		ImportGrowthSimulation("Growth simulation", "Simulation de croissance"),
		ErrorWhileLoadingData("An error occured while loading the data:", "Une erreur est survenue lors de lecture des donn\u00E9es :"),
		LookAndFeel("Skin", "Pr\u00E9sentation"),
		MEMSParameters("Soil Carbon Parameters", "Param\u00E8tres du carbone du sol");
		
		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}

		@Override
		public String toString() {
			return REpiceaTranslator.getString(this);
		}
	}
	

	private static BufferedImage iconImage;

	protected final CarbonAccountingTool caller;

	protected CATPanelView graphicPanel;

	protected final REpiceaComboBoxOpenButton<ProductionProcessorManagerWrapper> hwpComboBox;
	protected final REpiceaComboBoxOpenButton<BiomassParametersWrapper> biomassComboBox;
	
	private final JMenu file;
	private final JMenu view;
	private final JMenu options;
	private final JMenuItem sensitivityAnalysisMenuItem;
	private final JMenuItem calculateCarbonMenuItem;
	private final JMenuItem stopMenuItem;
	private final JMenuItem close; // after confirmation
	private final JMenuItem help;
	private final JMenu importMenu;
	private final JMenuItem yieldTable;
	private final JMenuItem growthSimulation;
	
	private final JRadioButtonMenuItem calculateInCarbon;
	protected final JRadioButtonMenuItem calculateInCO2;
	protected final REpiceaSlider confidenceIntervalSlider;
	
	private final JRadioButtonMenuItem aR2;
	private final JRadioButtonMenuItem aR4;
	private final JRadioButtonMenuItem aR5;
	
	private final JButton calculateCarbonButton;
	private final JButton stopButton;
	
	protected JProgressBar majorProgressBar;
	private final JLabel majorProgressBarMessage;

	private JProgressBar minorProgressBar;
	private final JLabel minorProgressBarMessage;

	private boolean vetoEnabled;
	
	
	/**
	 * General constructor of this class.
	 * @param caller the CarbonAccountingTool instance
	 * @param owner the parent window if any
	 */
	protected CATFrame(final CarbonAccountingTool caller, Window owner) {
		super(owner);
		this.setCancelOnClose(false);		// no possibility for cancelling here
		this.askUserBeforeExit = true;
		this.caller = caller;
		addPropertyChangeListener(this);
		
		new DropTargetImpl<ArrayList<CATCompatibleStand>>(this, ArrayList.class, DnDConstants.ACTION_REFERENCE);
		
		stopButton = UIControlManager.createCommonButton(CommonControlID.Stop);
		stopButton.setText("");
		stopButton.setMargin(new Insets(2,2,2,2));
		stopButton.setToolTipText(MessageID.PanicButton.toString());

		stopMenuItem = UIControlManager.createCommonMenuItem(CommonControlID.Stop);
		
		minorProgressBarMessage = UIControlManager.getLabel(MessageID.WaitingTask.toString()); // default operation for now
		
		majorProgressBarMessage = UIControlManager.getLabel(MessageID.NumberOfRunsToDo.toString()); // default operation for now
		minorProgressBar = new JProgressBar();
		majorProgressBar = new JProgressBar();
		if (CATSensitivityAnalysisSettings.getInstance().getNumberOfMonteCarloRealizations() > 0) {
			majorProgressBar.setMaximum(CATSensitivityAnalysisSettings.getInstance().getNumberOfMonteCarloRealizations());
		}
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		file = UIControlManager.createCommonMenu(CommonMenuTitle.File);
		menuBar.add(file);

		importMenu = UIControlManager.createCommonMenu(CommonControlID.Import);
		file.add(importMenu);
		file.add(new JSeparator());
		
		yieldTable = UIControlManager.createCommonMenuItem(MessageID.ImportYieldTable);
		importMenu.add(yieldTable);
		growthSimulation = UIControlManager.createCommonMenuItem(MessageID.ImportGrowthSimulation);
		importMenu.add(growthSimulation);
		
		close = UIControlManager.createCommonMenuItem(CommonControlID.Quit);
		file.add(close);

		JMenu actionMenu = new JMenu("Actions");
		menuBar.add(actionMenu);

		calculateCarbonMenuItem = UIControlManager.createCommonMenuItem(MessageID.CalculateCarbonBalance);
		ImageIcon carbonIcon = CommonGuiUtility.retrieveIcon(getClass(), "carbonIcon.png");
		calculateCarbonMenuItem.setIcon(carbonIcon);
		calculateCarbonMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK));
		actionMenu.add(calculateCarbonMenuItem);
		actionMenu.add(stopMenuItem);
	
		calculateCarbonButton = new JButton();
		calculateCarbonButton.setIcon(carbonIcon);
		calculateCarbonButton.setMargin(new Insets(2,2,2,2));
		calculateCarbonButton.setToolTipText(MessageID.CalculateCarbonBalance.toString());

		view = UIControlManager.createCommonMenu(CommonMenuTitle.View);
		menuBar.add(view);
		JMenu lafs = new REpiceaLookAndFeelMenu(this);
		view.add(lafs);
		
		options = UIControlManager.createCommonMenu(CommonMenuTitle.Options);
		menuBar.add(options);
		JMenu units = new JMenu(MessageID.Units.toString());
		options.add(units);
		calculateInCarbon = new JRadioButtonMenuItem(MessageID.CEq.toString());
		units.add(calculateInCarbon);
		calculateInCO2 = new JRadioButtonMenuItem(MessageID.CO2Eq.toString());
		units.add(calculateInCO2);
		ButtonGroup bg = new ButtonGroup();
		bg.add(calculateInCarbon);
		bg.add(calculateInCO2);
		
		JMenu assessmentReport = new JMenu(MessageID.GlobalWarmingPotential.toString());
		options.add(assessmentReport);
		aR2 = new JRadioButtonMenuItem(AssessmentReport.Second.toString());
		assessmentReport.add(aR2);
		aR4 = new JRadioButtonMenuItem(AssessmentReport.Fourth.toString());
		assessmentReport.add(aR4);
		aR5 = new JRadioButtonMenuItem(AssessmentReport.Fifth.toString());
		assessmentReport.add(aR5);
		ButtonGroup bg2 = new ButtonGroup();
		bg2.add(aR2);
		bg2.add(aR4);
		bg2.add(aR5);
		
		switch(CATSettings.selectedAR) {
		case Second:
			aR2.setSelected(true);
			break;
		case Fourth:
			aR4.setSelected(true);
			break;
		case Fifth:
			aR5.setSelected(true);
			break;
		}
		
		

		calculateInCarbon.setSelected(true); // default value;
		
		confidenceIntervalSlider = new REpiceaSlider(Position.North);
		confidenceIntervalSlider.setMinimum(50);
		confidenceIntervalSlider.setMaximum(100);
		confidenceIntervalSlider.setValue((int) (.95 * 100));
		JMenu ciMenu = new JMenu(MessageID.CI.toString());
		ciMenu.add(confidenceIntervalSlider);
		options.add(ciMenu);
		sensitivityAnalysisMenuItem = new JMenuItem(MessageID.SensitivityAnalysis.toString());
		options.add(sensitivityAnalysisMenuItem);
		
		JMenu about = UIControlManager.createCommonMenu(CommonMenuTitle.About);
		menuBar.add(about);
		help = UIControlManager.createCommonMenuItem(CommonControlID.Help);
		about.add(help);

		
		hwpComboBox = new REpiceaComboBoxOpenButton<ProductionProcessorManagerWrapper>(MessageID.HWP_Parameters); 
		hwpComboBox.getComboBox().setModel(new DefaultComboBoxModel<ProductionProcessorManagerWrapper>(caller.getCarbonToolSettings().productionManagerMap.values().toArray(new ProductionProcessorManagerWrapper[]{})));
		ProductionProcessorManagerWrapper currentProductionManagerItem = caller.getCarbonToolSettings().productionManagerMap.get(caller.getCarbonToolSettings().getCurrentProductionProcessorManagerSelection());
		hwpComboBox.getComboBox().setSelectedItem(currentProductionManagerItem);

		biomassComboBox  = new REpiceaComboBoxOpenButton<BiomassParametersWrapper>(MessageID.Biomass); 
		biomassComboBox.getComboBox().setModel(new DefaultComboBoxModel<BiomassParametersWrapper>(caller.getCarbonToolSettings().biomassParametersMap.values().toArray(new BiomassParametersWrapper[]{})));
		BiomassParametersWrapper currentBiomassParametersWrapper = caller.getCarbonToolSettings().biomassParametersMap.get(caller.getCarbonToolSettings().getCurrentBiomassParametersSelection());
		biomassComboBox.getComboBox().setSelectedItem(currentBiomassParametersWrapper);
		
		refreshInterface();
		setSimulationRunning(false);
		createUI();
		setPreferredSize(new Dimension(800,600));
		pack();
	}

	/*
	 * For extended visibility (non-Javadoc)
	 * @see repicea.gui.REpiceaFrame#anchorLocation(java.awt.Point)
	 */
	@Override
	public void anchorLocation(Point location) {
		super.anchorLocation(location);
	}
	
	

	@Override
	protected BufferedImage getBufferedImage() {
		if (iconImage == null) {
			String path = ObjectUtility.getRelativePackagePath(CATFrame.class);
			String iconFilename = path + "LogoLerfob.jpg";
			InputStream in = getClass().getResourceAsStream("/" + iconFilename);
			try {
				iconImage = ImageIO.read(in);
			}
			catch (IOException e) {}
		}
		return iconImage;
	}

	
	private JPanel createStatusAndProgressBarPanel() {
	
		JPanel wrapperPanel = new JPanel(new BorderLayout());
		wrapperPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
		wrapperPanel.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
		JPanel progressBarPanel = new JPanel();
		wrapperPanel.add(progressBarPanel, BorderLayout.CENTER);
		progressBarPanel.setLayout(new BoxLayout(progressBarPanel, BoxLayout.Y_AXIS));
		progressBarPanel.add(Box.createVerticalStrut(10));
		
		JPanel progressBarTaskLabelPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		progressBarTaskLabelPanel.add(minorProgressBarMessage);
		progressBarPanel.add(progressBarTaskLabelPanel);
		minorProgressBar.setStringPainted(true);
		progressBarPanel.add(minorProgressBar);
		progressBarPanel.add(Box.createVerticalStrut(10));
		
		JPanel progressRealizationPanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		progressRealizationPanel.add(majorProgressBarMessage);
		progressBarPanel.add(progressRealizationPanel);
		majorProgressBar.setStringPainted(true);
		majorProgressBar.setString("");
		progressBarPanel.add(majorProgressBar);
		progressBarPanel.add(Box.createVerticalStrut(10));
		
		
		return wrapperPanel;
	}

	/**	
	 * From ActionListener interface.
	 * Buttons management.
	 */
	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource().equals(close)) {
			okAction();
		} else if (evt.getSource().equals(help)) {
			UIControlManager.getHelper(getClass()).callHelp();
		} else if (evt.getSource().equals(calculateCarbonMenuItem) || evt.getSource().equals(calculateCarbonButton)) {
			try {
				caller.calculateCarbon();
				setSimulationRunning(true);
			} catch (Exception e) {
				if (e instanceof ProductionProcessorManagerException) {
					JOptionPane.showMessageDialog(this, 
							MessageID.ParametersAreInvalid.toString() + e.getMessage() + "</html>", 
							REpiceaTranslator.getString(UIControlManager.InformationMessageTitle.Error),
							JOptionPane.ERROR_MESSAGE);
				}
				setSimulationRunning(false);
			} 
		} else if (evt.getSource().equals(stopMenuItem) || evt.getSource().equals(stopButton)) {
			caller.cancelRunningTask();
		} else if (evt.getSource().equals(sensitivityAnalysisMenuItem)) {
			CATSensitivityAnalysisSettings.getInstance().showUI(this);
			redefineProgressBar();
		} else if (evt.getSource().equals(aR2)) {
			CATSettings.setAssessmentReportForGWP(AssessmentReport.Second);
		} else if (evt.getSource().equals(aR4)) {
			CATSettings.setAssessmentReportForGWP(AssessmentReport.Fourth);
		} else if (evt.getSource().equals(aR5)) {
			CATSettings.setAssessmentReportForGWP(AssessmentReport.Fifth);
		} else if (evt.getSource().equals(yieldTable)) {
			constructYieldTable();
		} else if (evt.getSource().equals(growthSimulation)) {
			constructStandListFromGrowthSimulation();
		}
	}
	
	private void constructYieldTable() {
		String yieldTableFilename = getSettingMemory().getProperty("lerfobcat.yieldTableFilename", "");
		try {
			REpiceaFileFilterList fileFilters = new REpiceaFileFilterList(REpiceaFileFilter.CSV);
			FileChooserOutput fileChooserOutput = CommonGuiUtility.browseAction(this, 
					JFileChooser.FILES_ONLY, 
					yieldTableFilename,
					fileFilters,
					JFileChooser.OPEN_DIALOG);
			if (!fileChooserOutput.isValid()) {
				return;
			} 
			CATSpeciesSelectionDialog dlg = new CATSpeciesSelectionDialog(this);
			if (!dlg.isValidated()) {
				return;
			}
			yieldTableFilename = fileChooserOutput.getFilename();
			CATYieldTableRecordReader catRecordReader = new CATYieldTableRecordReader(dlg);
			catRecordReader.initGUIMode(this, UseMode.GUI_MODE, yieldTableFilename);
			catRecordReader.readAllRecords();
			caller.setStandList(catRecordReader.getStandList());
			getSettingMemory().setProperty("lerfobcat.yieldTableFilename", yieldTableFilename);
		} catch (Exception e) {
			if (e instanceof CancellationException) {
				return;
			}
		}
	}

	/**
	 * Provide the SettingMemory instance that
	 * can be used to record filenames.
	 * @return a SettingMemory instance
	 */
	public SettingMemory getSettingMemory() {
		return caller.getSettingMemory();
	}
	
	private void constructStandListFromGrowthSimulation() {
		String growthSimulationFilename = getSettingMemory().getProperty("lerfobcat.growthSimulationFilename", "");
		try {
			REpiceaFileFilterList fileFilters = new REpiceaFileFilterList(REpiceaFileFilter.CSV);
			FileChooserOutput fileChooserOutput = CommonGuiUtility.browseAction(this, 
					JFileChooser.FILES_ONLY, 
					growthSimulationFilename,
					fileFilters,
					JFileChooser.OPEN_DIALOG);
			if (!fileChooserOutput.isValid()) {
				return;
			} 
			growthSimulationFilename = fileChooserOutput.getFilename();
			
			CATScaleManagementSelectorDialog scaleManagDlg = new CATScaleManagementSelectorDialog(this);
			scaleManagDlg.setVisible(true);
			
			if (scaleManagDlg.isCancelled()) {
				return;
			}
			
			CATGrowthSimulationRecordReader catRecordReader = new CATGrowthSimulationRecordReader(scaleManagDlg);
			catRecordReader.initGUIMode(this, UseMode.GUI_MODE, growthSimulationFilename);
			
			catRecordReader.readAllRecords();
			catRecordReader.getSelector().showUI(this);
			if (catRecordReader.getSelector().getUI(null).hasBeenCancelled()) {
				return;
			}
			List<CATCompatibleStand> stands = catRecordReader.getStandList();
			caller.setStandList(stands);
			getSettingMemory().setProperty("lerfobcat.growthSimulationFilename", growthSimulationFilename);
		} catch (Exception e) {
			if (e instanceof CancellationException) {
				return;
			} else {
				CommonGuiUtility.showErrorMessage(MessageID.ErrorWhileLoadingData.toString() + " " + e.getMessage(), this);
			}
		}
	}

	
	protected void setCalculateCarbonButtonsEnabled(boolean bool) {
		boolean isStandListSet = caller.getCarbonCompartmentManager().completeStandList != null;
		calculateCarbonMenuItem.setEnabled(bool && !vetoEnabled && isStandListSet );
		calculateCarbonButton.setEnabled(bool && !vetoEnabled && isStandListSet);
	}
	
	
	protected void redefineProgressBar() {
		majorProgressBar.setMinimum(0);
		majorProgressBar.setMaximum(CATSensitivityAnalysisSettings.getInstance().getNumberOfMonteCarloRealizations());
		majorProgressBar.setValue(0);
		majorProgressBar.setString(majorProgressBar.getValue() + " / " + majorProgressBar.getMaximum());
		refreshInterface();
	}
	
	@Override
	public void okAction() {
		if (!vetoEnabled) {
			super.okAction();
		}
	}
	

	/*	
	 * User interface definition
	 */
	private void createUI() {
		graphicPanel = new CATPanelView(new CATOptionPanel());
		JPanel parameterPanel  = new JPanel(new FlowLayout(FlowLayout.LEFT));
		parameterPanel.add(Box.createHorizontalStrut(5));
		parameterPanel.add(calculateCarbonButton);
		parameterPanel.add(Box.createHorizontalStrut(10));
		parameterPanel.add(stopButton);
		parameterPanel.add(Box.createHorizontalStrut(10));
		parameterPanel.add(biomassComboBox);
		parameterPanel.add(Box.createHorizontalStrut(10));
		parameterPanel.add(new JSeparator());
		parameterPanel.add(hwpComboBox);
		parameterPanel.add(Box.createHorizontalStrut(5));
		
		getContentPane().add(parameterPanel, BorderLayout.NORTH);
		getContentPane().add(graphicPanel, BorderLayout.CENTER);
		getContentPane().add(createStatusAndProgressBarPanel(), BorderLayout.SOUTH);
	}

	protected void displayResult() {
		setSimulationRunning(false);
		graphicPanel.addSimulationResult(caller.getCarbonCompartmentManager().getSimulationSummary());
		refreshInterface();
	}
	
	protected void setSimulationRunning(boolean b) {
		vetoEnabled = b;
		setCalculateCarbonButtonsEnabled(!b);
		stopMenuItem.setEnabled(b);
		stopButton.setEnabled(b);
		options.setEnabled(!b);
		file.setEnabled(!b);
		hwpComboBox.setEnabled(!b);
		biomassComboBox.setEnabled(!b);
	}
	
	protected void displayWarningMessage(String message) {
		Runnable doRun = new Runnable() {
			@Override
			public void run() {
				CommonGuiUtility.showWarningMessage(message, CATFrame.this);
			}
		};
		SwingUtilities.invokeLater(doRun);
	}
	
	private void updateMajorProgressBarValue(int i) {
		majorProgressBar.setValue(i);
		majorProgressBar.setString(majorProgressBar.getValue() + " / " + majorProgressBar.getMaximum());
	}
	
	/**
	 * Listens to the progress of the TreeLogger, the WoodPieceProcessorWorker and the CarbonCompartmentManager. Also
	 * refreshes the Graphic panel when the carbon compartment manager is done.
	 */
	@SuppressWarnings("incomplete-switch")
	public void propertyChange(PropertyChangeEvent evt) {
		if (evt.getPropertyName().equals("state")) {
			if (evt.getNewValue() == SwingWorker.StateValue.DONE) {
				CATTask task = (CATTask) evt.getSource();
				if (task.getFailureReason () == null) {
					String taskName = task.getName();
					if (taskName.equals(CATTask.Task.COMPILE_CARBON.name())) {
						minorProgressBarMessage.setText(REpiceaTranslator.getString(MessageID.JobDone));
						minorProgressBar.setValue(100);
					} else if (taskName.equals(CATTask.Task.DISPLAY_RESULT.name())) {
						updateMajorProgressBarValue(majorProgressBar.getMaximum());
					}
				}
			}
		} else if (evt.getPropertyName().equals("progress")) {
			minorProgressBar.setValue((Integer) evt.getNewValue());
		} else if (evt.getPropertyName().equals("OngoingTask")) {
			Task task = (Task) evt.getNewValue();
			switch (task) {
				case LOG_AND_BUCK_TREES:
					minorProgressBarMessage.setText(REpiceaTranslator.getString(MessageID.LoggingJob));
					updateMajorProgressBarValue(caller.getCarbonCompartmentManager().getMonteCarloRealizationId());
					break;
				case GENERATE_WOODPRODUCTS:
					minorProgressBarMessage.setText(REpiceaTranslator.getString(MessageID.WoodPieceJob));
					break;
				case ACTUALIZE_CARBON:
					minorProgressBarMessage.setText(REpiceaTranslator.getString(MessageID.ActualizingCarbon));
					break;
				case COMPILE_CARBON:
					minorProgressBarMessage.setText(REpiceaTranslator.getString(MessageID.CarbonCompartmentJob));
					break;
			}
		} else if (evt.getPropertyName().equals(REpiceaAWTProperty.WindowAcceptedConfirmed.name()) || evt.getPropertyName().equals(REpiceaAWTProperty.WindowCancelledConfirmed.name())) {
			caller.respondToWindowClosing();
		} 
	}


	@Override
	public void listenTo() {
		close.addActionListener(this);
		help.addActionListener(this);
		stopMenuItem.addActionListener(this);
		stopButton.addActionListener(this);
		calculateCarbonMenuItem.addActionListener(this);
		calculateCarbonButton.addActionListener(this);
		biomassComboBox.getComboBox().addItemListener(this);
		hwpComboBox.getComboBox().addItemListener(this);
		calculateInCO2.addChangeListener(graphicPanel);
		calculateInCarbon.addChangeListener(graphicPanel);
		confidenceIntervalSlider.addPropertyChangeListener(graphicPanel);
		sensitivityAnalysisMenuItem.addActionListener(this);
		aR2.addActionListener(this);
		aR4.addActionListener(this);
		aR5.addActionListener(this);
		yieldTable.addActionListener(this);
		growthSimulation.addActionListener(this);
	}


	@Override
	public void doNotListenToAnymore() {
		close.removeActionListener(this);
		help.removeActionListener(this);
		stopMenuItem.removeActionListener(this);
		stopButton.removeActionListener(this);
		calculateCarbonMenuItem.removeActionListener(this);
		calculateCarbonButton.removeActionListener(this);
		biomassComboBox.getComboBox().removeItemListener(this);
		hwpComboBox.getComboBox().removeItemListener(this);
		calculateInCO2.removeChangeListener(graphicPanel);
		calculateInCarbon.removeChangeListener(graphicPanel);
		confidenceIntervalSlider.removePropertyChangeListener(graphicPanel);
		sensitivityAnalysisMenuItem.removeActionListener(this);
		aR2.removeActionListener(this);
		aR4.removeActionListener(this);
		aR5.removeActionListener(this);
		yieldTable.removeActionListener(this);
		growthSimulation.removeActionListener(this);
	}

	@Override
	public void refreshInterface() {
		String suffix = "";
		if (caller.getCarbonCompartmentManager().getLastCompleteStand() != null) {
			suffix = " - " + caller.getCarbonCompartmentManager().getLastCompleteStand().getStandIdentification();
		}
		setTitle(UIControlManager.getTitle(getClass()) + suffix);
	}

	/*
	 * Entry point of the drag and drop event (non-Javadoc)
	 * @see repicea.gui.dnd.AcceptableDropComponent#acceptThisObject(java.lang.Object, java.awt.dnd.DropTargetDropEvent)
	 */
	@Override
	public void acceptThisObject(ArrayList<CATCompatibleStand> stands, LocatedEvent arg0) {
		acceptTheseStands(stands);
	}

	/**
	 * This method is either called by a drag and drop event or externally by another application. It
	 * asks the user if he/she accepts the import.
	 * 
	 * @param stands an ArrayList of CATCompatibleStand instances
	 */
	public void acceptTheseStands(ArrayList<CATCompatibleStand> stands) {
		if (stands != null && !stands.isEmpty()) {
			CATCompatibleStand lastStand = stands.get(stands.size() - 1);
			String lastStandID = lastStand.getStandIdentification();
			if (JOptionPane.showConfirmDialog(this, 
					MessageID.ImportStandList.toString() + " " + lastStandID, 
					UIControlManager.InformationMessageTitle.Warning.toString(), 
					JOptionPane.YES_NO_CANCEL_OPTION, 
					JOptionPane.WARNING_MESSAGE) == 0) {
				caller.setStandList(stands);
			}
		}
	}


	@Override
	public void itemStateChanged(ItemEvent arg0) {
		if (arg0.getSource().equals(biomassComboBox.getComboBox())) {
			caller.getCarbonToolSettings().setCurrentBiomassParametersSelection(((BiomassParametersWrapper) biomassComboBox.getComboBox().getSelectedItem()).name);
		} else if (arg0.getSource().equals(hwpComboBox.getComboBox())) {
			caller.getCarbonToolSettings().setCurrentProductionProcessorManagerSelection(((ProductionProcessorManagerWrapper) hwpComboBox.getComboBox().getSelectedItem()).name);
		}
	}

	
}

