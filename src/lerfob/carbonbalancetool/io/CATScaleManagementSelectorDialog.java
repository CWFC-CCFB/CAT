/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2025 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service 
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
package lerfob.carbonbalancetool.io;

import java.awt.BorderLayout;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import repicea.gui.REpiceaControlPanel;
import repicea.gui.REpiceaDialog;
import repicea.gui.UIControlManager;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class CATScaleManagementSelectorDialog extends REpiceaDialog {

	private static enum MessageID implements TextableEnum {

		ApplicationScaleLabel("Application Scale", "Echelle d'application"),
		ManagementLabel("Management Type", "Type d'am\u00E9nagement"),
		LocaleLabel("Region", "R\u00E9gion"),
;

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

	static {
		UIControlManager.setTitle(CATScaleManagementSelectorDialog.class, 
				"What sort of simulation do you want to import?", 
				"Quelle sorte de simulation voulez-vous importer?");
	}

	
	final JComboBox<ApplicationScale> scaleComboBox;
	final JComboBox<ManagementType> managementComboBox;
	final JComboBox<SpeciesLocale> localeComboBox;
	final REpiceaControlPanel controlPanel;
	private boolean isCancelled;
	
	public CATScaleManagementSelectorDialog(Window parent) {
		super(parent);
		scaleComboBox = new JComboBox<ApplicationScale>(ApplicationScale.values());
		scaleComboBox.setEditable(false);
		scaleComboBox.setSelectedItem(ApplicationScale.FMU);
		scaleComboBox.setName("scaleComboBox");
		managementComboBox = new JComboBox<ManagementType>(ManagementType.values());
		managementComboBox.setEditable(false);
		managementComboBox.setSelectedItem(ManagementType.UnevenAged);
		managementComboBox.setName("managementComboBox");
		localeComboBox = new JComboBox<SpeciesLocale>(SpeciesLocale.values());
		localeComboBox.setEditable(false);
		localeComboBox.setSelectedIndex(0);
		controlPanel = new REpiceaControlPanel(this);
		this.setTitle(UIControlManager.getTitle(CATScaleManagementSelectorDialog.class));
		initUI();
	}
	
	
	
	@Override
	public void listenTo() {}

	@Override
	public void doNotListenToAnymore() {}

	@Override
	public void cancelAction() {
		super.cancelAction();
		isCancelled = true;
	}
	
	@Override
	protected void initUI() {
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(controlPanel, BorderLayout.SOUTH);
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		mainPanel.add(Box.createVerticalStrut(5));
		JPanel applicationScalePanel = UIControlManager.createSimpleHorizontalPanel(MessageID.ApplicationScaleLabel, scaleComboBox, 5, true);
		mainPanel.add(applicationScalePanel);
		mainPanel.add(Box.createVerticalStrut(5));
		JPanel managementPanel = UIControlManager.createSimpleHorizontalPanel(MessageID.ManagementLabel, managementComboBox, 5, true);
		mainPanel.add(managementPanel);
		mainPanel.add(Box.createVerticalStrut(5));
		JPanel localePanel = UIControlManager.createSimpleHorizontalPanel(MessageID.LocaleLabel, localeComboBox, 5, true);
		mainPanel.add(localePanel);
		mainPanel.add(Box.createVerticalStrut(5));
		getContentPane().add(mainPanel, BorderLayout.NORTH);
		pack();
		setMinimumSize(getSize());
	}

	ApplicationScale getApplicationScale() {return (ApplicationScale) scaleComboBox.getSelectedItem();}

	ManagementType getManagementType() {return (ManagementType) this.managementComboBox.getSelectedItem();}

	SpeciesLocale getSpeciesLocale() {return (SpeciesLocale) this.localeComboBox.getSelectedItem();}

	public boolean isCancelled() {return isCancelled;}
	
	public static void main(String[] args) {
		CATScaleManagementSelectorDialog dlg = new CATScaleManagementSelectorDialog(null);
		dlg.setVisible(true);
		System.out.println("Has been cancelled? " + dlg.isCancelled);
		System.exit(0);
	}



}
