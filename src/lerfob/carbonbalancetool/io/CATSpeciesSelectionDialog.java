/*
 * This file is part of the lerfob-foresttools library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.io;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import repicea.gui.REpiceaDialog;
import repicea.gui.UIControlManager;
import repicea.gui.UIControlManager.CommonControlID;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class CATSpeciesSelectionDialog extends REpiceaDialog implements ActionListener, ItemListener {

	private static enum MessageID implements TextableEnum {
		SpeciesLabel("Please select the appropriate species", "Veuillez s\u00E9lectionner l'esp\u00E8ce appropri\u00E9e"),
		SpeciesLocaleLabel("Please select the region", "Veuillez s\u00E9lectionner la r\u00E9gion"),
		Title("Species selection", "Choix de l'esp\u00E8ce");

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
	
	private final JComboBox<Species> speciesComboBox;
	private final JComboBox<SpeciesLocale> speciesLocaleComboBox;
	private final JButton okButton;
	private boolean isValidated;
	
	
	public CATSpeciesSelectionDialog(Window parent) {
		super(parent);
		setCancelOnClose(true);
		speciesComboBox = new JComboBox<Species>(Species.values());
		speciesComboBox.setEditable(false);
		speciesComboBox.setSelectedIndex(0);

		speciesLocaleComboBox = new JComboBox<SpeciesLocale>(SpeciesLocale.values());
		speciesLocaleComboBox.setEditable(false);
		speciesLocaleComboBox.setSelectedIndex(0);
		
		updateSpeciesComboBox();

		okButton = UIControlManager.createCommonButton(CommonControlID.Ok);
		initUI();
		pack();
		setMinimumSize(getSize());
		setVisible(true);
	}
	
	private void updateSpeciesComboBox() {
		SpeciesLocale currentLocale = (SpeciesLocale) speciesLocaleComboBox.getSelectedItem();
		Species currentSpecies = (Species) speciesComboBox.getSelectedItem();
		List<Species> speciesForCurrentLocale = Species.getSpeciesForThisLocale(currentLocale);
		boolean currentSpeciesIsIn = speciesForCurrentLocale.contains(currentSpecies);
		
		DefaultComboBoxModel<Species> model = new DefaultComboBoxModel<Species>(speciesForCurrentLocale.toArray(new Species[] {}));
		speciesComboBox.setModel(model);
		if (currentSpeciesIsIn) {
			speciesComboBox.setSelectedItem(currentSpecies);
		} else {
			speciesComboBox.setSelectedIndex(0);
		}
	}

	@Override
	public void listenTo() {
		speciesLocaleComboBox.addItemListener(this);
		okButton.addActionListener(this);
	}

	@Override
	public void doNotListenToAnymore() {
		speciesLocaleComboBox.removeItemListener(this);
		okButton.removeActionListener(this);
	}

	@Override
	protected void initUI() {
		setTitle(MessageID.Title.toString());
		setLayout(new BorderLayout());
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		centerPanel.add(Box.createVerticalStrut(10));
		centerPanel.add(UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(MessageID.SpeciesLabel),
				speciesComboBox, 5, true));
		centerPanel.add(Box.createVerticalStrut(10));
		centerPanel.add(UIControlManager.createSimpleHorizontalPanel(UIControlManager.getLabel(MessageID.SpeciesLocaleLabel),
				speciesLocaleComboBox, 5, true));
		centerPanel.add(Box.createVerticalStrut(10));
		add(centerPanel, BorderLayout.NORTH);
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		controlPanel.add(okButton);
		add(controlPanel, BorderLayout.SOUTH);
	}
	

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource().equals(okButton)) {
			okAction();
		}
	}
	
	@Override
	public void okAction() {
		isValidated = true;
		super.okAction();
	}
	
	@Override 
	public void cancelAction() {
		isValidated = false;
		super.cancelAction();
	}

	
	public boolean isValidated() {return isValidated;}
	
	public Species getSpecies() {return (Species) speciesComboBox.getSelectedItem();}
	
	public SpeciesLocale getSpeciesLocale() {return (SpeciesLocale) speciesLocaleComboBox.getSelectedItem();} 
	
	public static void main(String[] args) {
		new CATSpeciesSelectionDialog(null);
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(speciesLocaleComboBox)) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				this.updateSpeciesComboBox();
			}
		}
		
	}

	
}
