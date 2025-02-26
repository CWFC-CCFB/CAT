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

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lerfob.carbonbalancetool.productionlines.LandfillCarbonUnitFeature.LandfillType;
import repicea.gui.UIControlManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

public class LandfillCarbonUnitFeaturePanel extends CarbonUnitFeaturePanel implements ChangeListener {

	private static final long serialVersionUID = 20101118L;
	
	public static enum MessageID implements TextableEnum {
		DegradableOrganicCarbonLabel ("Proportion of degradable carbon", "Proportion de carbone d\u00E9composable"),
		LandfillTypeLabel("Type of solid waste disposal site", "Type de site d'\u00E9mination des d\u00E9chets solides");
		
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
	
	private JLabel degradableOrganicCarbonFractionLabel;
	protected JSlider degradableOrganicCarbonFractionSlider;
	private JLabel landfillTypeLabel;
	protected JComboBox<LandfillType> landFillTypeComboBox;

	
	protected LandfillCarbonUnitFeaturePanel(LandfillCarbonUnitFeature caller) {
		super(caller);
	}

	
	protected LandfillCarbonUnitFeature getCaller() {return (LandfillCarbonUnitFeature) super.getCaller();}
	
	@Override
	protected void initializeFields() {
		super.initializeFields();

		landfillTypeLabel = new JLabel(MessageID.LandfillTypeLabel.toString());
		landFillTypeComboBox = new JComboBox<LandfillType>(LandfillType.values());
		landFillTypeComboBox.setSelectedItem(getCaller().getLandfillType());
		
		degradableOrganicCarbonFractionLabel = new JLabel();

		degradableOrganicCarbonFractionSlider = new JSlider(0,100);
		degradableOrganicCarbonFractionSlider.setMajorTickSpacing(10);
		degradableOrganicCarbonFractionSlider.setPaintTicks(true);
		degradableOrganicCarbonFractionSlider.setPaintLabels(true);
		degradableOrganicCarbonFractionSlider.setValue((int) (getCaller().getDegradableOrganicCarbonFraction() * 100));
		setSliderLabelText();
	}	
	
	@Override
	protected void createUI() {
		setLayout(new BorderLayout());
		setBorder(UIControlManager.getTitledBorder(CarbonUnitFeaturePanel.MessageID.WoodProductFeatureLabel.toString()));
		JPanel setupPanel = new JPanel(new BorderLayout());
		add(setupPanel, BorderLayout.CENTER);
		mainPanel = new JPanel();
		setupPanel.add(mainPanel, BorderLayout.NORTH);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		decayFunctionPanel = getCaller().getDecayFunction().getUI();
		//.createSimpleHorizontalPanel(lifetimeModeList, averageLifetimeTextField, 5, true);
		sourceTextArea = new JTextArea();
		sourceTextArea.setRows(5);
		sourceTextArea.setColumns(25);
		sourceTextArea.setLineWrap(true);
		sourceTextArea.setWrapStyleWord(true);
		sourceTextArea.setText(getCaller().getSourceInfo());
		sourceTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(decayFunctionPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		JPanel degradableOrganicCarbonFractionSliderLabel = UIControlManager.createSimpleHorizontalPanel(degradableOrganicCarbonFractionLabel, degradableOrganicCarbonFractionSlider, 5, true);
		JPanel landfillTypePanel = UIControlManager.createSimpleHorizontalPanel(landfillTypeLabel, landFillTypeComboBox, 5, true);

		mainPanel.add(degradableOrganicCarbonFractionSliderLabel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(landfillTypePanel);
		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(getSourceInfoPanel());
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		degradableOrganicCarbonFractionSlider.setEnabled(b);
		landFillTypeComboBox.setEnabled(b);
	}

	@Override
	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource().equals(degradableOrganicCarbonFractionSlider)) {
			setSliderLabelText();
		}
	}

	private void setSliderLabelText() {
		degradableOrganicCarbonFractionLabel.setText(MessageID.DegradableOrganicCarbonLabel.toString() + "    " + ((Integer) degradableOrganicCarbonFractionSlider.getValue()).toString() + "%");
	}
	
	
	
	@Override
	public void listenTo() {
		super.listenTo();
		degradableOrganicCarbonFractionSlider.addChangeListener(this);
		degradableOrganicCarbonFractionSlider.addChangeListener(getCaller());
		landFillTypeComboBox.addItemListener(getCaller());
	}

	@Override
	public void doNotListenToAnymore() {
		super.doNotListenToAnymore();
		degradableOrganicCarbonFractionSlider.removeChangeListener(this);
		degradableOrganicCarbonFractionSlider.removeChangeListener(getCaller());
		landFillTypeComboBox.removeItemListener(getCaller());
	}
}
