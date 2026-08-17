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
import java.io.IOException;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import repicea.gui.UIControlManager;
import repicea.gui.components.REpiceaMatchWithEnumObject;
import repicea.gui.components.REpiceaMatchWithEnumSelector;
import repicea.gui.components.REpiceaMatchWithEnumSelectorDialog;
import repicea.serial.MemorizerPackage;
import repicea.serial.UnmarshallingException;
import repicea.serial.xml.XmlDeserializer;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * A class that matches the species code of the import file with those of CAT.
 * @author Mathieu Fortin 2017, 2025, 2026
 */
@SuppressWarnings("deprecation")
public class CATGrowthSimulationSpeciesSelectorV2 extends REpiceaMatchWithEnumSelector<String, Species> {


	/**
	 * A specific class to represent the match with the Species enum
	 */
	static class SpeciesMatch implements REpiceaMatchWithEnumObject<String, Species> {

		private String speciesCode;
		private Species species;
		
		SpeciesMatch(String code, Species species) {
			this.species = species;
			this.speciesCode = code;
		}
		
		@Override
		public REpiceaMatchWithEnumObject<String, Species> getDeepClone() {
			return new SpeciesMatch(speciesCode, species);
		}

		@Override
		public List<Object> getAdditionalFields() {return null;}

		@Override
		public void setValueAt(int indexOfThisAdditionalField, Object value) {
			throw new UnsupportedOperationException("The class " + getClass().getSimpleName() + " does not implement any new fields!");
		}

		@Override
		public Species getValue() {return species;}

		@Override
		public void setValue(Species value) {this.species = value;}

		@Override
		public String getKey() {return speciesCode;}
		
	}
	
	@SuppressWarnings("serial")
	public static class CATGrowthSimulationSpeciesSelectorDialog extends REpiceaMatchWithEnumSelectorDialog {

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
		
		protected CATGrowthSimulationSpeciesSelectorDialog(CATGrowthSimulationSpeciesSelectorV2 caller, Window parent, Object[] columnNames) {
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

	private final List<String> requiredSpeciesCodes;
	
	
	private static SpeciesMatch[] getInitialMatches(List<String> speciesCodes) {
		if (speciesCodes == null || speciesCodes.isEmpty()) {
			throw new InvalidParameterException("The speciesCodes argument must be a non empty list!");
		}
		List<SpeciesMatch> speciesMatches = new ArrayList<SpeciesMatch>();
		for (String code : speciesCodes) {
			speciesMatches.add(new SpeciesMatch(code, Species.Abies_spp));
		}
		return speciesMatches.toArray(new SpeciesMatch[] {});
	}
	
	protected CATGrowthSimulationSpeciesSelectorV2(List<String> speciesCodes) {
		super(getInitialMatches(speciesCodes), 0, ColumnName.values());
		requiredSpeciesCodes = Collections.unmodifiableList(speciesCodes);
	}

	@Override
	public CATGrowthSimulationSpeciesSelectorDialog getUI(Container parent) {
		if (this.guiInterface == null) {
			guiInterface = new CATGrowthSimulationSpeciesSelectorDialog(this, (Window) parent, columnNames);
		}
		return (CATGrowthSimulationSpeciesSelectorDialog) guiInterface;
	}


	@Override
	public void load(String filename) throws IOException {
		XmlDeserializer deserializer = new XmlDeserializer(filename);
		try {
			Object newloadedInstance = deserializer.readObject();
			MemorizerPackage mp;
			if (newloadedInstance instanceof CATGrowthSimulationSpeciesSelector) {
				mp = convertFromDeprecatedVersion((CATGrowthSimulationSpeciesSelector) newloadedInstance);
			} else if (newloadedInstance instanceof CATGrowthSimulationSpeciesSelectorV2) {
				mp = ((CATGrowthSimulationSpeciesSelectorV2) newloadedInstance).getMemorizerPackage();
			} else {
				throw new IOException("The deserialized instance is not compatible: from class " + newloadedInstance.getClass().getSimpleName() + " whereas " + CATGrowthSimulationSpeciesSelectorV2.class.getSimpleName() + " was expected!");
			}
			unpackMemorizerPackage(mp);
			setFilename(filename);
		} catch (UnmarshallingException e1) {
			throw new IOException("A UnmarshallException occurred while loading the file!");
		} catch (RequiredCodeException e2) {
			throw new IOException(e2.getMessage());
		}
	}
	
	
	@Override
	public final MemorizerPackage getMemorizerPackage() {
		MemorizerPackage mp = new MemorizerPackage();
		mp.add((Serializable) matchMaps);
		return mp;
	}


	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public final void unpackMemorizerPackage(MemorizerPackage wasMemorized) {
		Map<Enum<?>, Map<String, SpeciesMatch>> oMap = (Map) wasMemorized.get(0);
		List<String> speciesCode = new ArrayList<String>(oMap.get(REpiceaMatchWithEnumSelector.DefaultSingleCategory.SingleCategory).keySet());
		checkCompatibility(speciesCode);
		matchMaps.clear();
		matchMaps.putAll((Map) wasMemorized.get(0));
	}
	
	private void checkCompatibility(List<String> speciesCode) {
		for (String requiredCode : requiredSpeciesCodes) {
			if (!speciesCode.contains(requiredCode)) {
				throw new RequiredCodeException("Code " + requiredCode + " cannot be found in the deserialized species selector!");
			}
		}
	}
	
	private MemorizerPackage convertFromDeprecatedVersion(CATGrowthSimulationSpeciesSelector newloadedInstance) {
		MemorizerPackage mp = newloadedInstance.getMemorizerPackage();
		TreeMap<String, Species> matchMap = (TreeMap) mp.get(0);
		LinkedHashMap<Enum<?>, Map<String, SpeciesMatch>> formattedMatchMap = new LinkedHashMap<Enum<?>, Map<String, SpeciesMatch>>();
		formattedMatchMap.put(REpiceaMatchWithEnumSelector.DefaultSingleCategory.SingleCategory, new TreeMap<String, SpeciesMatch>());
		for (Entry<String, Species> entry : matchMap.entrySet()) {
			formattedMatchMap.get(REpiceaMatchWithEnumSelector.DefaultSingleCategory.SingleCategory).put(entry.getKey(), new SpeciesMatch(entry.getKey(), entry.getValue()));
		}
		mp.clear();
		mp.add(formattedMatchMap);
		return mp;
	}

	public static void main(String[] args) {
		CATGrowthSimulationSpeciesSelectorV2 selector = new CATGrowthSimulationSpeciesSelectorV2(Arrays.asList("Carotte","Patate"));
		selector.showUI(null);
	}
	
}
