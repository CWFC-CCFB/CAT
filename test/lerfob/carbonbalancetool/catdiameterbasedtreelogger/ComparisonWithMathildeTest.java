/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.predictor.mathilde.MathildeTreeSpeciesProvider.MathildeTreeSpecies;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLoggerParameters;
import lerfob.treelogger.mathilde.MathildeLoggableTree;
import lerfob.treelogger.mathilde.MathildeTreeLogger;
import lerfob.treelogger.mathilde.MathildeTreeLoggerParameters;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;
import repicea.simulation.treelogger.WoodPiece;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.Language;

public class ComparisonWithMathildeTest {

	class MathildeLoggableTreeImpl implements MathildeLoggableTree, CATCompatibleTree {
		
		final MathildeTreeSpecies species;
		final double dbhCm;
		
		MathildeLoggableTreeImpl(MathildeTreeSpecies species, double dbhCm) {
			this.species = species;
			this.dbhCm = dbhCm;
		}
		
		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public String getSpeciesName() {return species.toString();}

		@Override
		public MathildeTreeSpecies getMathildeTreeSpecies() {return species;}

		@Override
		public double getDbhCm() {return dbhCm;}

		@Override
		public Species getCATSpecies() {
			switch(species) {
			case CARPINUS:
				return Species.Carpinus_betulus;
			case QUERCUS:
				return Species.Quercus_spp;
			case FAGUS:
				return Species.Fagus_sylvatica;
			case OTHERS:
				return Species.Betula_spp;
			default:
				return null;
			}
		}

		@Override
		public SpeciesLocale getSpeciesLocale() {return SpeciesLocale.IPCC;}
		
	}

	private static Language languageBefore;
	
	@BeforeClass
	public static void doThisBefore() {
		languageBefore = REpiceaTranslator.getCurrentLanguage();
		REpiceaTranslator.setCurrentLanguage(Language.English);
	}

	@Test
	public void testOakLargeLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.QUERCUS, 50d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testOakSmallLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.QUERCUS, 45d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testOakEnergyWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.QUERCUS, 15d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}


	@Test
	public void testBeechLargeLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.FAGUS, 50d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testBeechSmallLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.FAGUS, 30d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testBeechEnergyWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.FAGUS, 15d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testHornbeamLargeLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.CARPINUS, 50d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testHornbeamSmallLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.CARPINUS, 30d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testHornbeamEnergyWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.CARPINUS, 15d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testOthersLargeLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.OTHERS, 50d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testOthersSmallLumberWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.OTHERS, 30d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	@Test
	public void testOthersEnergyWood() throws IOException {
		MathildeLoggableTreeImpl loggableTree = new MathildeLoggableTreeImpl(MathildeTreeSpecies.OTHERS, 15d);
		Collection<MathildeLoggableTreeImpl> trees = new ArrayList<MathildeLoggableTreeImpl>();
		trees.add(loggableTree);
		commonCode(trees, loggableTree);
	}

	private void commonCode(Collection<MathildeLoggableTreeImpl> trees, MathildeLoggableTreeImpl loggableTree) throws IOException {
		MathildeTreeLogger matTreeLogger = new MathildeTreeLogger();
		matTreeLogger.setTreeLoggerParameters(matTreeLogger.createDefaultTreeLoggerParameters());
		
		String filename = ObjectUtility.getPackagePath(getClass()) + "MathildeInspiredTreeLoggerParameters.tlp";
		CATDiameterBasedTreeLogger catTreeLogger = new CATDiameterBasedTreeLogger();
		CATDiameterBasedTreeLoggerParameters parms = (CATDiameterBasedTreeLoggerParameters) CATDiameterBasedTreeLoggerParameters.loadFromFile(filename);
		catTreeLogger.setTreeLoggerParameters(parms);

		
		matTreeLogger.init(trees);
		matTreeLogger.run();
		
		catTreeLogger.init(trees);
		catTreeLogger.run();
		
		Map<String, WoodPiece> mathildeWoodPieces = convertToMap(matTreeLogger.getWoodPieces().get(loggableTree));
		Map<String, WoodPiece> catWoodPieces = convertToMap(catTreeLogger.getWoodPieces().get(loggableTree)); 
		
		Assert.assertTrue(mathildeWoodPieces.size() == catWoodPieces.size());

		double expected = findValueInMap(mathildeWoodPieces, MathildeTreeLoggerParameters.Grade.LargeLumberWood.toString());
		double actual = findValueInMap(catWoodPieces,  DiameterBasedTreeLoggerParameters.Grade.LargeLumberWood.toString());
		Assert.assertEquals("Testing volume", expected, actual, 1E-8);
		
		expected = findValueInMap(mathildeWoodPieces, MathildeTreeLoggerParameters.Grade.SmallLumberWood.toString());
		actual = findValueInMap(catWoodPieces,  DiameterBasedTreeLoggerParameters.Grade.SmallLumberWood.toString());
		Assert.assertEquals("Testing volume", expected, actual, 1E-8);

		expected = findValueInMap(mathildeWoodPieces, MathildeTreeLoggerParameters.Grade.EnergyWood.toString());
		actual = findValueInMap(catWoodPieces,  DiameterBasedTreeLoggerParameters.Grade.EnergyWood.toString());
		Assert.assertEquals("Testing volume", expected, actual, 1E-8);
	}
	
	private double findValueInMap(Map<String, WoodPiece> oMap, String key) {
		if (oMap.containsKey(key)) {
			return oMap.get(key).getWeightedTotalVolumeM3();
		} else {
			return -1;
		}
	}
	
	private Map<String, WoodPiece> convertToMap(Collection<WoodPiece> coll) {
		Map<String, WoodPiece> outputMap = new HashMap<String, WoodPiece>();
		for (WoodPiece piece: coll) {
			outputMap.put(piece.getLogCategory().getName(), piece);
		}
		return outputMap;
	}
	
	@AfterClass
	public static void doThatAfter() {
		REpiceaTranslator.setCurrentLanguage(languageBefore);
	}
	
	
}
