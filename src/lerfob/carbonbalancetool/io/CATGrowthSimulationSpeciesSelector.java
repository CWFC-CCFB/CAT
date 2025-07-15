/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
 * Copyright (C) 2025 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, 
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

import java.awt.Container;
import java.awt.Window;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lerfob.carbonbalancetool.CATSettings.CATSpecies;
import repicea.gui.UIControlManager;
import repicea.gui.components.REpiceaMatchSelector;
import repicea.gui.components.REpiceaMatchSelectorDialog;
import repicea.serial.PostUnmarshalling;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * A class that matches the species in the file with those of CAT.
 * @author Mathieu Fortin 2017, 2025
 */
@SuppressWarnings("deprecation")
public class CATGrowthSimulationSpeciesSelector extends REpiceaMatchSelector<Enum<?>> implements PostUnmarshalling {


	@SuppressWarnings("serial")
	public static class CATGrowthSimulationSpeciesSelectorDialog extends REpiceaMatchSelectorDialog {

		protected static enum MessageID implements TextableEnum {
			Instruction("Please select the species available in CAT to match those found in your input file", 
					"Veuillez associer les esp\u00E8ces reconnues par CAT \u00E0 celles de votre fichier d'entr\u00E9e")
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

		static {
			UIControlManager.setTitle(CATGrowthSimulationSpeciesSelectorDialog.class, "Species correspondance","Correspondance entre les esp\u00E8ces");
		}
		
		protected CATGrowthSimulationSpeciesSelectorDialog(CATGrowthSimulationSpeciesSelector caller, Window parent, Object[] columnNames) {
			super(caller, parent, columnNames);
		}

		protected JPanel getMainPanel() {
			JPanel pane = new JPanel();
			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));

			pane.add(new JLabel(MessageID.Instruction.toString()));
			pane.add(Box.createVerticalStrut(10));
			JScrollPane scrollPane = new JScrollPane(getTable());
			pane.add(createSimplePanel(scrollPane, 20));
			pane.add(Box.createVerticalStrut(10));
			return pane;

		}
		
		
		
	}
	
	
	protected static enum ColumnName implements TextableEnum {
		SpeciesNameInFile("Species name in file", "Nom d'esp\u00E8ce dans le fichier"),
		SpeciesNameInCAT("Species name in CAT", "Nom d'esp\u00E8ce dans CAT"),
		;
		
		ColumnName(String englishText, String frenchText) {
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

	protected CATGrowthSimulationSpeciesSelector(Object[] toBeMatched) {
		super(toBeMatched, Species.values(), 0, ColumnName.values());
	}

	@Override
	public CATGrowthSimulationSpeciesSelectorDialog getUI(Container parent) {
		if (this.guiInterface == null) {
			guiInterface = new CATGrowthSimulationSpeciesSelectorDialog(this, (Window) parent, columnNames);
		}
		return (CATGrowthSimulationSpeciesSelectorDialog) guiInterface;
	}

	@Override
	public void postUnmarshallingAction() {
		if (!potentialMatches.isEmpty() && potentialMatches.get(0) instanceof CATSpecies) {
			potentialMatches.replaceAll(p -> ((CATSpecies) p).species);
			matchMap.replaceAll((k,v) -> ((CATSpecies) v).species);
		}
		int u = 0;
	}
	
	
	public static void main(String[] args) {
		CATGrowthSimulationSpeciesSelector selector = new CATGrowthSimulationSpeciesSelector(new Object[] {"Carotte","Patate"});
		selector.showUI(null);
	}
}
