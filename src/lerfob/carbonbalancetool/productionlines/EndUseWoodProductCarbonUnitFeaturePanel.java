/*
 * This file is part of the lerfob-forestools library.
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

import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import lerfob.carbonbalancetool.productionlines.combustion.CombustionEmissions.CombustionProcess;
import repicea.gui.UIControlManager;
import repicea.gui.components.NumberFormatFieldFactory;
import repicea.gui.components.NumberFormatFieldFactory.JFormattedNumericField;
import repicea.gui.components.NumberFormatFieldFactory.Range;
import repicea.gui.components.NumberFormatFieldFactory.Type;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The EndProductFeaturePanel class is the GUI interface of the EndProductFeature class.
 * @author Mathieu Fortin - October 2010
 */
public class EndUseWoodProductCarbonUnitFeaturePanel extends CarbonUnitFeaturePanel implements ChangeListener, 
																								ItemListener {
	
	private static final long serialVersionUID = 20101020L;
	
	public static enum MessageID implements TextableEnum {
		RelativeSubstitution("Relative substitution (Mg CO2 eq. / Funct. Unit)", "Substitution relative (Mg CO2 eq. / Unit\u00E9 fonct.)"),
		UseClassCategory("Use class", "Cat\u00E9gorie d'usage"),
		CombustionProcessName("Combustion process", "Proc\u00E9d\u00E9 de combustion");
		
		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private JComboBox useClassList;
	@SuppressWarnings("rawtypes")
	private JComboBox combustionProcessList;
	
	protected JFormattedNumericField substitutionTextField;

	protected JCheckBox isDisposableCheckBox;

	private JLabel disposableProportionLabel;
	protected JSlider disposableProportionSlider;
	protected JFormattedNumericField biomassFUTextField;
	protected JFormattedNumericField emissionsByFUField;

	/**
	 * The constructor is linked to its representing class.
	 * @param caller a EndProductFeature instance
	 */
	protected EndUseWoodProductCarbonUnitFeaturePanel(EndUseWoodProductCarbonUnitFeature caller) {
		super(caller);
	}

	@Override
	protected EndUseWoodProductCarbonUnitFeature getCaller() {return (EndUseWoodProductCarbonUnitFeature) super.getCaller();}
	
	private static class UseClassComparator implements Comparator<UseClass> {

		private static UseClassComparator Singleton = new UseClassComparator();
		
		private static final Comparator<String> NaturalOrderString = Comparator.<String>naturalOrder();
		
		@Override
		public int compare(UseClass o1, UseClass o2) {
			return NaturalOrderString.compare(o1.toString(), o2.toString());
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "deprecation" })
	@Override
	protected void initializeFields() {
		super.initializeFields();
		
		disposableProportionLabel = new JLabel();
		Dimension dim = new Dimension(getFontMetrics(this.getFont()).getHeight() * 3 , getFontMetrics(this.getFont()).getHeight() * 2);
		disposableProportionLabel.setPreferredSize(dim);

		isDisposableCheckBox = new JCheckBox();
		isDisposableCheckBox.setSelected(getCaller().isDisposed());

		disposableProportionSlider = new JSlider(0,100);
		disposableProportionSlider.setMajorTickSpacing(10);
		disposableProportionSlider.setPaintTicks(true);
		disposableProportionSlider.setPaintLabels(true);

		disposableProportionSlider.setEnabled(isDisposableCheckBox.isSelected());
		disposableProportionSlider.setValue((int) (getCaller()).getDisposableProportion() * 100);
		disposableProportionLabel.setText(((Integer) disposableProportionSlider.getValue()).toString() + "%");

		biomassFUTextField = NumberFormatFieldFactory.createNumberFormatField(Type.Double, Range.Positive, false);
		biomassFUTextField.setText(((Double) getCaller().getBiomassOfFunctionalUnitMg()).toString());
		biomassFUTextField.setPreferredSize(new Dimension(100, biomassFUTextField.getFontMetrics(biomassFUTextField.getFont()).getHeight() + 2));
		
		substitutionTextField = NumberFormatFieldFactory.createNumberFormatField(Type.Double, Range.All, false);
		substitutionTextField.setText(((Double) getCaller().getSubstitutionMgCO2EqByFunctionalUnit(null)).toString());
		substitutionTextField.setPreferredSize(new Dimension(100, substitutionTextField.getFontMetrics(substitutionTextField.getFont()).getHeight() + 2));

		List<UseClass> useClasses = Arrays.asList(UseClass.values());
		Collections.sort(useClasses, UseClassComparator.Singleton);
		
		useClassList = new JComboBox(useClasses.toArray(new UseClass[] {}));
		UseClass useClass = (getCaller()).getUseClass();
		useClassList.setSelectedIndex(useClasses.indexOf(useClass));

		combustionProcessList = new JComboBox(CombustionProcess.values());
		CombustionProcess combustionProcess = getCaller().getCombustionProcess();
		if (combustionProcess == null) {
			combustionProcessList.setSelectedItem(CombustionProcess.None);
		} else {
			combustionProcessList.setSelectedItem(combustionProcess);
		}
		
		emissionsByFUField = NumberFormatFieldFactory.createNumberFormatField(NumberFormatFieldFactory.Type.Double,
				NumberFormatFieldFactory.Range.Positive,
				false);
		emissionsByFUField.setColumns(5);
		emissionsByFUField.setText(((Double) getCaller().getEmissionsMgCO2EqByFunctionalUnit()).toString());

	}
	
	
	@Override
	protected void createUI() {
		super.createUI();

		JPanel biomassFUPanel = UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(EnhancedProcessorInternalDialog.MessageID.FunctionalUnitBiomassLabel),
				biomassFUTextField, 
				5,
				true);
		
		JPanel emissionFUPanel = UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(EnhancedProcessorInternalDialog.MessageID.EmissionsLabel),
				emissionsByFUField, 
				5,
				true);
			
		JPanel averageSubstitutionPanel = UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(MessageID.RelativeSubstitution),
				substitutionTextField, 
				5,
				true);

		JPanel useClassPanel = UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(MessageID.UseClassCategory),
				useClassList, 
				5,
				true);
		
		JPanel combustionProcessPanel = UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(MessageID.CombustionProcessName),
				combustionProcessList, 
				10, 
				true);
		
		
		mainPanel.add(biomassFUPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(emissionFUPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(averageSubstitutionPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(useClassPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(combustionProcessPanel);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(getSourceInfoPanel());
		mainPanel.add(Box.createVerticalStrut(5));
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		useClassList.setEnabled(b);
		substitutionTextField.setEnabled(b);
		isDisposableCheckBox.setEnabled(b);
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource().equals(disposableProportionSlider)) {
			disposableProportionLabel.setText(((Integer) disposableProportionSlider.getValue()).toString() + "%");
		}
	}
	
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource().equals(isDisposableCheckBox)) {
			if (isEnabled()) {
				disposableProportionSlider.setEnabled(isDisposableCheckBox.isSelected());
			}
		} else if (evt.getSource().equals(useClassList)) {
			checkIfIsMeantForEnergy();
//			boolean isMeantForEnergy = getCaller().getUseClass().isMeantForEnergyProduction();
//			if (!isMeantForEnergy) {
//				combustionProcessList.setSelectedItem(CombustionProcess.None);
//			}
//			combustionProcessList.setEnabled(isMeantForEnergy);
		}
	}
	
	
	private void checkIfIsMeantForEnergy() {
		boolean isMeantForEnergy = getCaller().getUseClass().isMeantForEnergyProduction();
		if (!isMeantForEnergy) {
			combustionProcessList.setSelectedItem(CombustionProcess.None);
		}
		combustionProcessList.setEnabled(isMeantForEnergy);
	}
	
	@Override
	public void listenTo() {
		super.listenTo();
		isDisposableCheckBox.addItemListener(getCaller());
		isDisposableCheckBox.addItemListener(this);
		disposableProportionSlider.addChangeListener(this);
		disposableProportionSlider.addChangeListener(getCaller());
		substitutionTextField.addNumberFieldListener(getCaller());
		useClassList.addItemListener(getCaller());
		useClassList.addItemListener(this);
		combustionProcessList.addItemListener(getCaller());
		biomassFUTextField.addNumberFieldListener(getCaller());
		emissionsByFUField.addNumberFieldListener(getCaller());
	}

	@Override
	public void doNotListenToAnymore() {
		super.doNotListenToAnymore();
		isDisposableCheckBox.removeItemListener(getCaller());
		isDisposableCheckBox.removeItemListener(this);
		disposableProportionSlider.removeChangeListener(this);
		disposableProportionSlider.removeChangeListener(getCaller());
		substitutionTextField.removeNumberFieldListener(getCaller());
		useClassList.removeItemListener(getCaller());
		useClassList.removeItemListener(this);
		combustionProcessList.removeItemListener(getCaller());
		biomassFUTextField.removeNumberFieldListener(getCaller());
		emissionsByFUField.removeNumberFieldListener(getCaller());
	}


	/*
	 * Useless for this class (non-Javadoc)
	 * @see repicea.gui.Refreshable#refreshInterface()
	 */
	@Override
	public void refreshInterface() {
		checkIfIsMeantForEnergy();
	}


}
