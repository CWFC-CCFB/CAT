/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2018 Mathieu Fortin 
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
package lerfob.carbonbalancetool.catdiameterbasedtreelogger;

import java.awt.Window;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JOptionPane;

import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLoggerParametersDialog;
import repicea.gui.UIControlManager;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.treelogger.TreeLoggerAWTProperty;
import repicea.simulation.treelogger.TreeLoggerParametersDialog;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class CATDiameterBasedTreeLoggerParametersDialog extends DiameterBasedTreeLoggerParametersDialog {

	static {
		UIControlManager.setTitle(CATDiameterBasedTreeLoggerParametersDialog.class, "CAT Tree Logger based on diameter at breast height", "Module de billonnage de CAT bas\u00E9 sur le diam\u00E8tre \u00EA 1,3 m");
	}

	
	private static enum MessageID implements TextableEnum {
		PleaseChooseOneSpecies("Please select a species in this list:", "Veuillez s\u00E9lectionner une esp\u00E8ce dans cette liste :"),
		ImpossibleToAddSpecies("There is no species left that can be added to the list!", "Il ne reste plus d'esp\u00E8ce pouvant \00EAtre ajout\u00E9e \u00E0 la liste!");

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
	
	protected CATDiameterBasedTreeLoggerParametersDialog(Window window, CATDiameterBasedTreeLoggerParameters params) {
		super(window, params);
		mnFile.setEnabled(true);
		mnEdit.setEnabled(true);
		mnSpecies.setEnabled(true);			//	the species can be changed 
		mnLogGrade.setEnabled(false);			// the log grade cannot be changed either
		mnTools.setEnabled(false);
		logGradeGoDown.setEnabled(false);		// the log grade cannot be changed either
		logGradeGoUp.setEnabled(false);
	}

	
	private List<Species> retrieveSpeciesInCurrentList() {
		List<Species> currentCATSpecies = new ArrayList<Species>();
		for (Object key : getTreeLoggerParameters().getLogCategories().keySet()) {
			if (key instanceof Species) {
				currentCATSpecies.add((Species) key);
			}
		}
		return currentCATSpecies;
	}

	
	@Override
	protected CATDiameterBasedTreeLoggerParameters getTreeLoggerParameters() {
		return (CATDiameterBasedTreeLoggerParameters) super.getTreeLoggerParameters();
	}
	
	@Override
	protected void speciesAddAction() {
		Species newSpecies;
		List<Species> availableSpecies = new ArrayList<Species>();
		availableSpecies.addAll(Arrays.asList(Species.values()));
		availableSpecies.removeAll(retrieveSpeciesInCurrentList());
		if (availableSpecies.isEmpty()) {
			JOptionPane.showMessageDialog(this, 
					REpiceaTranslator.getString(MessageID.ImpossibleToAddSpecies), 
					REpiceaTranslator.getString(TreeLoggerParametersDialog.MessageID.AddANewSpecies),
					JOptionPane.INFORMATION_MESSAGE);
			// TODO FP display a message here
			return;
		} else {
			newSpecies = (Species) JOptionPane.showInputDialog(this, 
					REpiceaTranslator.getString(MessageID.PleaseChooseOneSpecies),
					REpiceaTranslator.getString(TreeLoggerParametersDialog.MessageID.AddANewSpecies),
					JOptionPane.QUESTION_MESSAGE,
					null,
					availableSpecies.toArray(),
					availableSpecies.get(0));
			if (newSpecies == null) {		// means it has been canceled
				return;
			}
		}
		getTreeLoggerParameters().createLogCategoriesForThisSpecies(newSpecies);
		redefineSpeciesList();
		int selectedIndex = 0;
		for (Object speciesName : params.getLogCategories().keySet()) {
			if (speciesName.equals(newSpecies)) {
				speciesList.setSelectedIndex(selectedIndex);
				return;
			}
			selectedIndex++;
		}
		speciesList.setSelectedIndex(0);		// in case no match has been found
		firePropertyChange(TreeLoggerAWTProperty.SpeciesAdded, null, this);
	}


}
