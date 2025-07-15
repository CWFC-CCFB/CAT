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

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import repicea.gui.REpiceaDialog;
import repicea.gui.UIControlManager;
import repicea.gui.UIControlManager.CommonControlID;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class CATSpeciesSelectionDialog extends REpiceaDialog implements ActionListener {

	private static enum MessageID implements TextableEnum {
		SpeciesLabel("Please select the appropriate species", "Veuillez s\u00E9lectionner l'esp\u00E8ce appropri\u00E9e"),
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
	private final JButton okButton;
	private boolean isValidated;
	
	
	public CATSpeciesSelectionDialog(Window parent) {
		super(parent);
		setCancelOnClose(true);
		speciesComboBox = new JComboBox<Species>(Species.values());
		speciesComboBox.setSelectedIndex(0);
		okButton = UIControlManager.createCommonButton(CommonControlID.Ok);
		initUI();
		pack();
		setMinimumSize(getSize());
		setVisible(true);
	}
	
	@Override
	public void listenTo() {
		okButton.addActionListener(this);
	}

	@Override
	public void doNotListenToAnymore() {
		okButton.removeActionListener(this);
	}

	@Override
	protected void initUI() {
		setTitle(MessageID.Title.toString());
		setLayout(new BorderLayout());
		JLabel label = UIControlManager.getLabel(MessageID.SpeciesLabel);
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(Box.createHorizontalStrut(5));
		panel.add(label);
		panel.add(Box.createHorizontalStrut(5));
		panel.add(speciesComboBox);
		panel.add(Box.createHorizontalStrut(5));
		add(panel, BorderLayout.NORTH);
		
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
	
	public Species getCATSpecies() {return (Species) speciesComboBox.getSelectedItem();}
	
	public static void main(String[] args) {
		new CATSpeciesSelectionDialog(null);
	}

	
}
