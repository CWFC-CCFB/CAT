/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2023-25 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, Canadian Wood Fibre Centre
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed with the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * Please see the license at http://www.gnu.org/copyleft/lesser.html.
 */
package lerfob.carbonbalancetool.productionlines.affiliere;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import lerfob.carbonbalancetool.productionlines.affiliere.AffiliereImportReader.TagLevels;
import repicea.gui.REpiceaControlPanel;
import repicea.gui.REpiceaDialog;
import repicea.gui.UIControlManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
class AffiliereImportReaderDialog extends REpiceaDialog implements ItemListener {

	private static enum MessageID implements TextableEnum {
		CurrentNBProcessorText("Number of processors: ", "Nombre de processeurs : ");

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

	static {
		UIControlManager.setTitle(AffiliereImportReaderDialog.class, 
				"Aggregation levels in function of species and sectors",
				"Niveaux d'agr\u00E9gation en fonction des esp\u00E8ces et des secteurs");
	}
	
	final AffiliereImportReader caller;

	final List<JComboBox<String>> comboBoxes;
	final JTextField nbProcessorField;
	final REpiceaControlPanel controlPanel;
	
	boolean isCancelled;
	
	/**
	 * Constructor.
	 * @param caller an AffiliereImportReader instance which calls this dialog
	 * @param parent the parent window (can be null)
	 */
	AffiliereImportReaderDialog(AffiliereImportReader caller, Window parent) {
		super(parent);
		controlPanel = new REpiceaControlPanel(this);
		this.caller = caller;
		comboBoxes = new ArrayList<JComboBox<String>>();
		for (String tagName : caller.nodeTags.keySet()) {
			TagLevels tagLevels = caller.nodeTags.get(tagName);
			if (tagLevels.isExclusive) {
				JComboBox<String> cBox = new JComboBox<String>();
				comboBoxes.add(cBox);
				cBox.setName(tagName);
				ComboBoxModel<String> model = new DefaultComboBoxModel<String>(tagLevels.keySet().toArray(new String[] {}));
				for (String level : tagLevels.keySet()) {
					if (tagLevels.get(level)) {
						cBox.setSelectedItem(level);
						break;
					}
				}
				cBox.setModel(model);
			}
		}
		nbProcessorField = new JTextField();
		nbProcessorField.setEditable(false);
		updateNbProcessors();
		initUI();
		pack();
		setMinimumSize(getSize());
	}
	
	private void updateNbProcessors() {
		nbProcessorField.setText(caller.processors.size() + "");
	}
	
	@Override
	public void listenTo() {
		for (JComboBox<String> cb : comboBoxes) {
			cb.addItemListener(this);
		}
	}

	@Override
	public void doNotListenToAnymore() {
		for (JComboBox<String> cb : comboBoxes) {
			cb.removeItemListener(this);
		}
	}

	@Override
	public void okAction() {
		isCancelled = false;
		super.okAction();
	}

	@Override
	public void cancelAction() {
		isCancelled = true;
		super.cancelAction();
	}
	
	@Override
	protected void initUI() {
		setTitle(UIControlManager.getTitle(getClass()));
		getContentPane().setLayout(new BorderLayout());
		JPanel panel = new JPanel(new GridLayout(0,2));
		getContentPane().add(panel, BorderLayout.CENTER);
		
		for (JComboBox<String> cb : comboBoxes) {
			JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
			leftPanel.add(Box.createHorizontalStrut(4));
			leftPanel.add(UIControlManager.getLabel(cb.getName()));
			leftPanel.add(Box.createHorizontalStrut(4));
			panel.add(leftPanel);
			JPanel rightPanel = new JPanel(new BorderLayout());
			rightPanel.add(Box.createVerticalStrut(2), BorderLayout.NORTH);
			rightPanel.add(Box.createHorizontalStrut(4), BorderLayout.WEST);
			rightPanel.add(cb, BorderLayout.CENTER);
			rightPanel.add(Box.createHorizontalStrut(4), BorderLayout.EAST);
			rightPanel.add(Box.createVerticalStrut(2), BorderLayout.SOUTH);
			panel.add(rightPanel);
		}
		
		JPanel nbProcessorsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		nbProcessorsPanel.add(Box.createGlue());
		nbProcessorsPanel.add(UIControlManager.getLabel(MessageID.CurrentNBProcessorText));
		nbProcessorsPanel.add(Box.createHorizontalStrut(2));
		nbProcessorsPanel.add(nbProcessorField);
		nbProcessorsPanel.add(Box.createHorizontalStrut(2));
		panel.add(nbProcessorsPanel);
		
		getContentPane().add(controlPanel, BorderLayout.SOUTH);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource() instanceof JComboBox) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				JComboBox<String> thisComboBox = (JComboBox<String>) e.getSource();
				String tagName = thisComboBox.getName(); 
				String selectedLevel = thisComboBox.getSelectedItem().toString();
				TagLevels tagLevel = caller.nodeTags.get(tagName);
				for (String l : tagLevel.keySet()) {
					tagLevel.put(l, l.equals(selectedLevel));
				}
				caller.screenNodeMap();
				updateNbProcessors();
			}
		}
	}
	
	

}
