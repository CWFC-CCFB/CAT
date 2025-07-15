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

import java.util.ArrayList;
import java.util.List;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.io.tools.ImportFieldElement;
import repicea.io.tools.ImportFieldElement.FieldType;
import repicea.io.tools.LevelProviderEnum;
import repicea.io.tools.REpiceaRecordReader;
import repicea.serial.SerializerChangeMonitor;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class CATYieldTableRecordReader extends REpiceaRecordReader {

	static {
		SerializerChangeMonitor.registerClassNameChange("lerfob.carbonbalancetool.io.CATRecordReader$CATFieldLevel", "lerfob.carbonbalancetool.io.CATYieldTableRecordReader$CATYieldTableFieldLevel");
		SerializerChangeMonitor.registerClassNameChange("lerfob.carbonbalancetool.io.CATRecordReader$CATFieldID", "lerfob.carbonbalancetool.io.CATYieldTableRecordReader$CATYieldTableFieldID");
	}
	
	protected static enum MessageID implements TextableEnum {
		DateDescription("Stand age (years)","Age du peuplement (ann\u00E9es)"),
		DateHelp("This field must contains the age of the stand. It is an integer.", "Ce champ doit contenir l'\u00E2ge du peuplement. Il s'agit d'un entier."),
		StandingVolumeDescription("Standing overbark volume (m3/ha)", "Volume sur pied sur \u00E9corce (m3/ha)"),
		StandingVolumeHelp("This field contains the standing commercial volume (m3/ha). It is a double.", "Ce champ contient le volume commercial sur pied (m3/ha). Il s'agit d'un double."),
		HarvestedVolumeDescription("Harvested overbark volume (m3/ha)", "Volume r\u00E9colt\u00E9 (m3/ha) sur \u00E9corce"),
		HarvestedVolumeHelp("This field contains the harvested commercial volume (m3/ha). It is a double.", "Ce champ contient le volume commercial r\u00E9colt\u00E9 (m3/ha). Il s'agit d'un double.");
		
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

	protected static enum CATYieldTableFieldLevel {Stand;}

	protected static enum CATYieldTableFieldID implements LevelProviderEnum {
		Date,
		StandingVolume,
		HarvestedVolume;
		
		CATYieldTableFieldID() {}

		@Override
		public CATYieldTableFieldLevel getFieldLevel() {
			return CATYieldTableFieldLevel.Stand;
		}
	}

	
	private final List<CATCompatibleStand> standList;
	
	private final Species catSpecies;
	
	
	/**
	 * General constructor.
	 * @param catSpecies a CATSpecies enum that stands for tree species in this stand
	 */
	public CATYieldTableRecordReader(Species catSpecies) {
		super();
		setPopUpWindowEnabled(true);
		this.catSpecies = catSpecies;
		standList = new ArrayList<CATCompatibleStand>();
	}


	@Override
	protected List<ImportFieldElement> defineFieldsToImport() {
		List<ImportFieldElement> ifeList = new ArrayList<ImportFieldElement>();
		ImportFieldElement ife;
		ife = new ImportFieldElement(CATYieldTableFieldID.Date,
				MessageID.DateDescription.toString(), 
				getClass().getSimpleName() + ".dateDescription", 
				false, 
				MessageID.DateHelp.toString(),
				FieldType.Integer);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATYieldTableFieldID.StandingVolume,
				MessageID.StandingVolumeDescription.toString(), 
				getClass().getSimpleName() + ".standingVolumeDescription", 
				false, 
				MessageID.StandingVolumeHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATYieldTableFieldID.HarvestedVolume,
				MessageID.HarvestedVolumeDescription.toString(), 
				getClass().getSimpleName() + ".harvestVolumeDescription", 
				false, 
				MessageID.HarvestedVolumeHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		return ifeList;
	}

	@Override
	protected Enum<?> defineGroupFieldEnum() {return null;}

	@Override
	protected void readLineRecord(Object[] oArray, int lineCounter) throws VariableValueException, Exception {
		int dateYr = Integer.parseInt(oArray[0].toString());
		double standingVolumeM3 = Double.parseDouble(oArray[1].toString());
		double harvestedVolumeM3 = Double.parseDouble(oArray[2].toString());
		CATYieldTableCompatibleStand stand;
		if (harvestedVolumeM3 > 0d) {
			if (standList.size() == 0 || standList.get(standList.size() - 1).getDateYr() != dateYr) { // means that there is no before harvest entry. Need to create one.
				stand = new CATYieldTableCompatibleStand(
						getImportFieldManager().getFileSpecifications()[0],
						dateYr,
						false,
						catSpecies);
				standList.add(stand);
				stand.addTree(new CATYieldTableCompatibleTree(standingVolumeM3 + harvestedVolumeM3, StatusClass.alive)) ;
			}
		}
		stand = new CATYieldTableCompatibleStand(
				getImportFieldManager().getFileSpecifications()[0],
				dateYr,
				harvestedVolumeM3 > 0,
				catSpecies);
		standList.add(stand);
		CATYieldTableCompatibleTree tree;
		if (standingVolumeM3 > 0) {
			tree = new CATYieldTableCompatibleTree(standingVolumeM3, StatusClass.alive);
			stand.addTree(tree);
		}
		if (harvestedVolumeM3 > 0) {
			tree = new CATYieldTableCompatibleTree(harvestedVolumeM3, StatusClass.cut);
			stand.addTree(tree);
		}
	}

	@Override
	protected void checkInputFieldsFormat(Object[] oArray) {
		int index = this.getImportFieldManager().getIndexOfThisField(CATYieldTableFieldID.HarvestedVolume);
		if (oArray[index].toString().isEmpty()) {
			oArray[index] = "0";
		}
		super.checkInputFieldsFormat(oArray);
	}	
	
	
	@Override
	public void readRecordsForThisGroupId(int groupId) throws Exception {
		standList.clear();
		super.readRecordsForThisGroupId(groupId);
	}

	/**
	 * This method returns the stand list that was last read.
	 * @return a list of CATCompatibleStand instances
	 */
	public List<CATCompatibleStand> getStandList() {return standList;}
}
