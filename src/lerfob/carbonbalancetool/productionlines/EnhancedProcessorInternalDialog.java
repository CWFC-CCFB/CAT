/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2012 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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

import java.awt.Window;

import javax.swing.Box;
import javax.swing.JPanel;

import repicea.gui.CommonGuiUtility;
import repicea.gui.REpiceaPanel;
import repicea.gui.UIControlManager;
import repicea.gui.components.NumberFormatFieldFactory;
import repicea.gui.components.NumberFormatFieldFactory.JFormattedNumericField;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldDocument.NumberFieldEvent;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldListener;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.ProcessorInternalDialog;
import repicea.simulation.processsystem.SystemManager;
import repicea.simulation.processsystem.SystemManagerDialog;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * An internal dialog for CAT processors.<p>
 * 
 * The {@link AbstractProcessorButton} class uses an EnhancedProcessorInternalDialog instance 
 * instead of a {@link repicea.simulation.processsystem.ProcessorInternalDialog} instance.
 * 
 * @author Mathieu Fortin - 2012
 */
@SuppressWarnings("serial")
public class EnhancedProcessorInternalDialog extends ProcessorInternalDialog implements NumberFieldListener {

	protected static enum MessageID implements TextableEnum {
		FunctionalUnitBiomassLabel("Dry biomass per functional unit (Mg)", "Biomasse s\u00E8che de l'unit\u00E9 fonctionnelle (Mg)"),
		EmissionsLabel("Emissions (Mg CO2 eq. / Funct. Unit)", "Emissions (Mg CO2 eq. / Unit\u00E9 fonct.)")
		;	

		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
	}
	
	protected JFormattedNumericField functionUnitBiomass;
	protected JFormattedNumericField emissionsByFunctionUnit;
	
	protected EnhancedProcessorInternalDialog(Window parent, ProcessorButton callerButton) {
		super(parent, callerButton);
	}
	
	@Override
	protected void initializeComponents() {
		super.initializeComponents();	
		functionUnitBiomass = NumberFormatFieldFactory.createNumberFormatField(NumberFormatFieldFactory.Type.Double,
				NumberFormatFieldFactory.Range.Positive,
				false);
		functionUnitBiomass.setColumns(5);
		functionUnitBiomass.setText(((Double) getCaller().functionUnitBiomass).toString());
		emissionsByFunctionUnit = NumberFormatFieldFactory.createNumberFormatField(NumberFormatFieldFactory.Type.Double,
				NumberFormatFieldFactory.Range.Positive,
				false);
		emissionsByFunctionUnit.setColumns(5);
		emissionsByFunctionUnit.setText(((Double) getCaller().emissionsByFunctionalUnit).toString());
	}
	
	@Override
	public void setVisible(boolean bool) {
		if (!isVisible() && bool) {
			SystemManagerDialog dlg = (SystemManagerDialog) CommonGuiUtility.getParentComponent(this, SystemManagerDialog.class);
			boolean isEnablingGranted = ((SystemManager) dlg.getWindowOwner()).getGUIPermission().isEnablingGranted();
			functionUnitBiomass.setEnabled(isEnablingGranted);
			emissionsByFunctionUnit.setEnabled(isEnablingGranted);
		}
		super.setVisible(bool);
	}
		
	
	@Override
	public void refreshInterface() {
		setTopComponent();
		REpiceaPanel featurePanel = getCaller().getProcessFeaturesPanel();
		if (featurePanel != null) {
			if (mustDisplayCarbonUnitFeatures(getCaller())) {
				setBottomComponent(featurePanel);
			} else {
				setBottomComponent(new JPanel());
			}
		}
		pack();
		validate();
		repaint();
	}
	
	@Override
	protected AbstractProcessor getCaller() {
		return (AbstractProcessor) super.getCaller();
	}
	
	@Override
	protected JPanel setTopComponent() {
		JPanel topComponent = super.setTopComponent();
		if (usesEmissionsAndFunctionalUnitFromAbstractProcessorClass(getCaller())) {
			JPanel panel = UIControlManager.createSimpleHorizontalPanel(MessageID.FunctionalUnitBiomassLabel, functionUnitBiomass, 5, true);
			topComponent.add(panel);
			topComponent.add(Box.createVerticalStrut(5));
			panel = UIControlManager.createSimpleHorizontalPanel(MessageID.EmissionsLabel, emissionsByFunctionUnit, 5, true);
			topComponent.add(panel);
			topComponent.add(Box.createVerticalStrut(5));
		} else {
			topComponent.add(new JPanel());
		}
		return topComponent;
	}

	
	static boolean usesEmissionsAndFunctionalUnitFromAbstractProcessorClass(AbstractProcessor p) {
		return p instanceof AbstractExtractionProcessor ||
				p instanceof LandfillProcessor || 
				(p instanceof ProductionLineProcessor && p.hasSubProcessors());
	}
	
	static boolean mustDisplayCarbonUnitFeatures(AbstractProcessor p) {
		return !(p instanceof AbstractProductionLineProcessor) || !p.hasSubProcessors() ;
	}
	

	@Override
	public void listenTo() {
		super.listenTo();
		functionUnitBiomass.addNumberFieldListener(this);
		emissionsByFunctionUnit.addNumberFieldListener(this);
	}

	@Override
	public void doNotListenToAnymore() {
		super.doNotListenToAnymore();
		functionUnitBiomass.removeNumberFieldListener(this);
		emissionsByFunctionUnit.removeNumberFieldListener(this);
	}


	@Override
	public void numberChanged(NumberFieldEvent e) {
		if (e.getSource().equals(functionUnitBiomass)) {
			double value = (Double) functionUnitBiomass.getValue();
			if (value != getCaller().functionUnitBiomass) {
				((AbstractProcessorButton) getCaller().getUI()).setChanged(true);
				getCaller().functionUnitBiomass = value;
			}
		} else if (e.getSource().equals(emissionsByFunctionUnit)) {
			double value = (Double) emissionsByFunctionUnit.getValue();
			if (value != getCaller().emissionsByFunctionalUnit) {
				((AbstractProcessorButton) getCaller().getUI()).setChanged(true);
				getCaller().emissionsByFunctionalUnit = value; 
			}
		}
	}
	
}
