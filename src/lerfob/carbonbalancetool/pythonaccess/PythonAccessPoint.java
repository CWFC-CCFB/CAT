/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.pythonaccess;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

import lerfob.carbonbalancetool.CATCompartment.CompartmentInfo;
import lerfob.carbonbalancetool.CATCompatibleStand;
import lerfob.carbonbalancetool.CATSimulationResult;
import lerfob.carbonbalancetool.CATUtilityMaps.MonteCarloEstimateMap;
import lerfob.carbonbalancetool.CATUtilityMaps.UseClassSpeciesMonteCarloEstimateMap;
import lerfob.carbonbalancetool.CarbonAccountingTool;
import lerfob.carbonbalancetool.catdiameterbasedtreelogger.CATDiameterBasedTreeLogger;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.treelogger.basictreelogger.BasicTreeLogger;
import lerfob.treelogger.douglasfirfcba.DouglasFCBATreeLogger;
import lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLogger;
import lerfob.treelogger.maritimepine.MaritimePineBasicTreeLogger;
import py4j.GatewayServer;
import repicea.lang.REpiceaSystem;
import repicea.math.Matrix;
import repicea.math.utility.GaussianUtility;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.treelogger.TreeLoggerCompatibilityCheck;
import repicea.simulation.treelogger.TreeLoggerDescription;
import repicea.stats.distributions.TruncatedGaussianDistribution;
import repicea.util.ObjectUtility;

/**
 * This class is the entry point for a coupling with a Python application. Once exported as a .jar with the main function set to this
 * class, the entry point should use the method processStandList(Map myMap).
 * @author Mathieu Fortin - May 2014
 */
public class PythonAccessPoint extends CarbonAccountingTool {

	private static final String LISTEN = "-listen";
	private static final String CALLBACK = "-callback";
	private static final String VERBOSE = "-verbose";

	private final boolean verbose;
	
	protected Species speciesForSimulation; 
	protected double areaHa = 1d;
	
	/**
	 * Main constructor.
	 * @param verbose a boolean 
	 * @throws Exception if CAT has already been initialized
	 */
	public PythonAccessPoint(boolean verbose) throws Exception {
		super(CATMode.SCRIPT);
		this.verbose = verbose;
		initializeTool(null);
		getCarbonToolSettings().setTreeLoggerDescriptions(findMatchingTreeLoggers(null));
	}

	/**
	 * Constructor for tests.
	 * @throws Exception if CAT has already been initialized
	 */
	protected PythonAccessPoint() throws Exception {
		this(false);
	}
	
	/**
	 * This method sets the species for the simulation
	 * @param species a String either "beech", "pine", "douglas", or "oak"
	 * @throws InterruptedException if the engine is inadvertently unlocked
	 */
	public void setSpecies(String species) throws InterruptedException {
		Species speciesCode;
		if (species.toLowerCase().trim().equals("beech")) {
			speciesCode = Species.Fagus_sylvatica;
		} else if (species.toLowerCase().trim().equals("pine")) {
			speciesCode = Species.Pinus_pinaster;
		} else if (species.toLowerCase().trim().equals("douglas")) {
			speciesCode = Species.Pseudotsuga_menziesii;
		} else if (species.toLowerCase().trim().equals("oak")) {
			speciesCode = Species.Quercus_spp;
		} else {
			throw new InvalidParameterException("Only beech, pine, douglas and oak are accepted as species!");
		}
		setSpeciesAndSettings(speciesCode);
	}


	/**
	 * This method sets the area of the plot to be processed. By default, this area is set
	 * to 1 ha.
	 * @param areaHa the area (ha)
	 */
	public void setAreaHA(double areaHa) {
		this.areaHa = areaHa;
	}
	
	@Override
	protected Vector<TreeLoggerDescription> findMatchingTreeLoggers(TreeLoggerCompatibilityCheck referent) {
		Vector<TreeLoggerDescription> defaultTreeLoggerDescriptions = new Vector<TreeLoggerDescription>();
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(BasicTreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(MaritimePineBasicTreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(EuropeanBeechBasicTreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(DouglasFCBATreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(CATDiameterBasedTreeLogger.class));
		return defaultTreeLoggerDescriptions;
	}

	private void setSpeciesAndSettings(Species speciesCode) throws InterruptedException {
		if (!speciesCode.equals(speciesForSimulation)) {
			speciesForSimulation = speciesCode;
			String filename;
			if (speciesForSimulation.equals(Species.Pinus_pinaster)) {
				filename = ObjectUtility.getRelativePackagePath(getClass()) + "maritimepine.prl";
			} else if (speciesForSimulation.equals(Species.Pseudotsuga_menziesii)) {
				filename = ObjectUtility.getRelativePackagePath(getClass()) + "Douglas_20170703_P_EOL_simplified.prl";
			} else if (speciesForSimulation.equals(Species.Fagus_sylvatica)) {
				filename = ObjectUtility.getRelativePackagePath(getClass()) + "europeanbeech.prl";;
			} else if (speciesForSimulation.equals(Species.Quercus_spp)) {
				filename = ObjectUtility.getRelativePackagePath(getClass()) + "GrandEstForestSector.prl";;
			} else {
				throw new InvalidParameterException("The species is not recognized!");
			}
			System.out.println("Loading settings : " + filename);
			setProductionManager(filename);
		}
		String biomassFilename;
		if (speciesForSimulation.equals(Species.Pseudotsuga_menziesii)) {
			biomassFilename = ObjectUtility.getRelativePackagePath(getClass()) + "biomassParametersDouglasFir.bpf"; // in this one, the basic densities are the default ones
		} else {
			biomassFilename = ObjectUtility.getRelativePackagePath(getClass()) + "biomassParametersBeechPine.bpf";	// in this one, the basic densities are provided by the tree
		}
		setBiomassParameters(biomassFilename);
	}

	
	@Override
	protected void shutdown(int shutdownCode) {
		System.out.println("Shutting down CAT...");
		CATSensitivityAnalysisSettings.getInstance().clear();
		System.exit(shutdownCode);
	}

	@SuppressWarnings("rawtypes")
	private double convertStringToDouble(Map innerMap, String key) {
		if (innerMap.containsKey(key)) {
			return Double.parseDouble(innerMap.get(key).toString());
		} else {
			return 0d;
		}
	}
	
	
	protected List<CATCompatibleStand> createStandList(String standID, Map inputMap) {
		List<Integer> years = new ArrayList<Integer>();
		years.addAll(inputMap.keySet());
		Collections.sort(years);

		final String keyFirstInnerMap = "RECOLTE";
		List<CATCompatibleStand> standList = new ArrayList<CATCompatibleStand>();
		PythonCarbonToolCompatibleStand stand;
		PythonCarbonToolCompatibleTree tree;

		for (Integer dateYr : years) {
			Map innerMap = (Map) ((Map) inputMap.get(dateYr)).get(keyFirstInnerMap);
			
			stand = new PythonCarbonToolCompatibleStand(speciesForSimulation.name(), areaHa, standID, dateYr, ApplicationScale.Stand);
			standList.add(stand);
			
			if (innerMap != null) {
				double nbTreesHa = Double.parseDouble(innerMap.get("NbTrees").toString());
				double meanDbhCm = Double.parseDouble(innerMap.get("DBHmy").toString());
				double weightCrownKg_M2 = convertStringToDouble(innerMap, "Wcrown");		
//				double weightCrownKg_M2 = Double.parseDouble(innerMap.get("Wcrown").toString());
				double weightTrunkKg_M2 = Double.parseDouble(innerMap.get("Wtrunk").toString());
				double dbhStandardDeviation = Double.parseDouble(innerMap.get("DBHect").toString());
				double weightRootsKg_M2 = Double.parseDouble(innerMap.get("Wroots").toString());
				boolean isProcessable = weightCrownKg_M2 >= 0d && weightTrunkKg_M2 >= 0d && weightRootsKg_M2 >= 0d && meanDbhCm > 0;
				if (isProcessable) {
					double nbTrees = nbTreesHa * stand.getAreaHa();
					switch(speciesForSimulation) {
					case Pinus_pinaster:
						tree = new PythonMaritimePineTree(StatusClass.cut,
								nbTrees,
								getAverageDryBiomassByTree(weightRootsKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightTrunkKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightCrownKg_M2, nbTreesHa),
								meanDbhCm,
								dbhStandardDeviation);
						stand.addTree(StatusClass.cut, tree);
						break;
					case Fagus_sylvatica:
						tree = new PythonEuropeanBeechTree(StatusClass.cut,
								nbTrees,
								getAverageDryBiomassByTree(weightRootsKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightTrunkKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightCrownKg_M2, nbTreesHa),
								meanDbhCm,
								dbhStandardDeviation);
						stand.addTree(StatusClass.cut, tree);
						break;
					case Pseudotsuga_menziesii:
						tree = new PythonDouglasFirTree(StatusClass.cut,
								nbTrees,
								getAverageDryBiomassByTree(weightRootsKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightTrunkKg_M2, nbTreesHa),
								getAverageDryBiomassByTree(weightCrownKg_M2, nbTreesHa),
								meanDbhCm,
								dbhStandardDeviation);
						stand.addTree(StatusClass.cut, tree);
						break;
					case Quercus_spp:
						List<TreeFeatures> tfList = splitIntoTrees(nbTrees, meanDbhCm, dbhStandardDeviation, 10);
						for (TreeFeatures tf : tfList) {
							tree = new PythonOakTree(StatusClass.cut,
									nbTrees * tf.massProbForNbStems,
									getAverageDryBiomassByTree(weightRootsKg_M2 * tf.massProbForBiomass, nbTreesHa * tf.massProbForNbStems),
									getAverageDryBiomassByTree(weightTrunkKg_M2 * tf.massProbForBiomass, nbTreesHa * tf.massProbForNbStems),
									getAverageDryBiomassByTree(weightCrownKg_M2 * tf.massProbForBiomass, nbTreesHa * tf.massProbForNbStems),
									tf.meanDbhForThisClass,
									-1);
							stand.addTree(StatusClass.cut, tree);						
						}
						break;
					default:
						throw new InvalidParameterException("The species should be either pine, oak, beech or douglas");
					}
				}
			}
		}
		return standList;
	}
	
	@SuppressWarnings({ "rawtypes" })
	public Map<Integer, Map<String, Double>> processStandList(String standID, Map inputMap) throws Exception {
		long initTime = -1;
		if (verbose) {
			initTime = System.currentTimeMillis();
		}
		List<CATCompatibleStand> standList = createStandList(standID, inputMap);

		// stand list here
		setStandList(standList);
		calculateCarbon();
		CATSimulationResult simulationResult = retrieveSimulationSummary();
		Map<Integer, UseClassSpeciesMonteCarloEstimateMap> productEvolutionMap = simulationResult.getProductEvolutionPerHa();
		
		Matrix carbonInHWP = simulationResult.getEvolutionMap().get(CompartmentInfo.TotalProducts).getMean();
		
		Matrix permanentSeqInLandfill = simulationResult.getEvolutionMap().get(CompartmentInfo.LfillND).getMean();
		Matrix landfillCarbonDegradable = simulationResult.getEvolutionMap().get(CompartmentInfo.LfillDeg).getMean();
		Matrix emissionDueToTransformation = simulationResult.getEvolutionMap().get(CompartmentInfo.CarbEmis).getMean();

		List<Integer> outputYears = new ArrayList<Integer>(productEvolutionMap.keySet());
		Collections.sort(outputYears);
		
		Map<Integer, Map<String, Double>> outputMap = new TreeMap<Integer, Map<String, Double>>();
		
		for (Integer year : outputYears) { 
			if (!outputMap.containsKey(year)) {
				outputMap.put(year, new TreeMap<String, Double>());
			}
			Map<String, Double> innerOutputMap1 = outputMap.get(year);
			UseClassSpeciesMonteCarloEstimateMap innerInputMap2 = productEvolutionMap.get(year);
			for (UseClass useClass : UseClass.values()) {
				if (useClass != UseClass.EXTRACTIVE) {
					String key = "BiomassMgHa" + useClass.name().toUpperCase();
					if (innerInputMap2 != null && innerInputMap2.containsKey(useClass)) {
						MonteCarloEstimateMap amountMap = innerInputMap2.get(useClass).getSumAcrossSpecies();
						innerOutputMap1.put(key, amountMap.get(Element.Biomass).getMean().getValueAt(0, 0));
					} else {
						innerOutputMap1.put(key, 0d);
					}
				}
			}
			innerOutputMap1.put("CurrentCarbonHWPMgHa", carbonInHWP.getValueAt(outputYears.indexOf(year), 0));
			innerOutputMap1.put("LandfillCarbonNDMgHa", permanentSeqInLandfill.getValueAt(outputYears.indexOf(year), 0));
			innerOutputMap1.put("LandfillCarbonDegMgHa", landfillCarbonDegradable.getValueAt(outputYears.indexOf(year), 0));
			innerOutputMap1.put("CEqEmissionTransMgHa", emissionDueToTransformation.getValueAt(outputYears.indexOf(year), 0));
		}
		System.out.println("Stand " + standID + " processed...");
		if (verbose) {
			long totalTime = System.currentTimeMillis() - initTime;
			System.out.println("Time to process stand list " + totalTime + " ms. - Current memory load = " + REpiceaSystem.getCurrentMemoryLoadMb() + " Mb");
		}
		return outputMap;
	}
	
	protected static double getAverageDryBiomassByTree(double kgM2, double nbTreesHa) {
		return kgM2 * 10d / nbTreesHa;		// 10: 0.001 from kg to Mg times 10000 m2/ha
	}

	@Override
	protected void respondToWindowClosing() {}	// Do nothing to avoid the automatic shut down


	private static class TreeFeatures {
		final double dbhCmMin;
		final double dbhCmMax;
		final double meanDbhForThisClass;
		final double massProbForNbStems;
		final TruncatedGaussianDistribution dist;
		double massProbForBiomass;
		TreeFeatures(double dbhCmMin, double dbhCmMax, double density, TruncatedGaussianDistribution dist) {
			this.dbhCmMin = dbhCmMin;
			this.dbhCmMax = dbhCmMax;
			this.massProbForNbStems = density;
			this.dist = dist;
			Matrix lowerBound = new Matrix(1,1);
			lowerBound.setValueAt(0, 0, dbhCmMin);
			this.dist.setLowerBoundValue(lowerBound);
			Matrix upperBound = new Matrix(1,1);
			upperBound.setValueAt(0, 0, dbhCmMax);
			this.dist.setUpperBoundValue(upperBound);
			meanDbhForThisClass = this.dist.getMean().getValueAt(0, 0);
			if (meanDbhForThisClass < dbhCmMin || meanDbhForThisClass > dbhCmMax) {
				throw new InvalidParameterException("It seems the mean dbh of the truncated distribution is inconsistent with the bounds!");
			}
		}
	}
	
	
	private List<TreeFeatures> splitIntoTrees(double nbHa, double meanDbhCm, double stdDev, int nbIntervals) {
		List<TreeFeatures> fList = new ArrayList<TreeFeatures>();
		double q99 = GaussianUtility.getQuantile(0.995);
		double min = meanDbhCm - q99 * stdDev;
		if (min < 1d) {
			min = 1d;
		}
		
		double max = meanDbhCm + q99 * stdDev;
		double coverage = GaussianUtility.getCumulativeProbability((max - meanDbhCm) / stdDev) - GaussianUtility.getCumulativeProbability((min - meanDbhCm) / stdDev);
		double range = max - min;
		double step = range / nbIntervals;
		for (int i = 0; i < nbIntervals; i++) {
			double dmin = min + i * step;
			double dmax = dmin + step;
			double mass = GaussianUtility.getCumulativeProbability((dmax - meanDbhCm) / stdDev) - GaussianUtility.getCumulativeProbability((dmin - meanDbhCm) / stdDev);
			mass /= coverage;	// rescaling due to truncation
			fList.add(new TreeFeatures(dmin, dmax, mass, new TruncatedGaussianDistribution(meanDbhCm, stdDev*stdDev)));
		}
		
		double sumDbhPow3Mass = 0;
		for (TreeFeatures f : fList) {
			sumDbhPow3Mass += f.meanDbhForThisClass * f.meanDbhForThisClass * f.meanDbhForThisClass * f.massProbForNbStems;
		}

		for (TreeFeatures f : fList) {
			f.massProbForBiomass = f.meanDbhForThisClass * f.meanDbhForThisClass * f.meanDbhForThisClass * f.massProbForNbStems / sumDbhPow3Mass;
		}

		return fList;
	}
	
	
	/**
	 * Start method for connection to Python.
	 * @param args the parameters of the server
	 */
	public static void main(String[] args) {
//		System.out.println("Running on repicea " + REpiceaAppVersion.getInstance().getRevision());
//		System.out.println("Running on lerfobforesttools " + LerfobForesttoolsAppVersion.getInstance().getRevision());
		List<String> argumentList = Arrays.asList(args);
		Integer listeningPort = null;
		Integer callbackPort = null;
		
		boolean verbose = false;
		String verbStr = REpiceaSystem.retrieveArgument(VERBOSE, argumentList);
		if (verbStr != null && verbStr.toLowerCase().equals("t")) {
			verbose = true;
		}

		String newListeningPort = REpiceaSystem.retrieveArgument(LISTEN, argumentList);
		String newCallbackPort = REpiceaSystem.retrieveArgument(CALLBACK, argumentList);
		
		if (newListeningPort != null && newCallbackPort != null) {
			try {
				listeningPort = Integer.parseInt(newListeningPort);
				callbackPort = Integer.parseInt(newCallbackPort);
				try {
					if (callbackPort ==  listeningPort) {
						throw new InvalidParameterException("The callback and the listing ports should be different!");
					}
				} catch (NumberFormatException e) {
					System.out.println("Unable to set callback port to " + newCallbackPort +".");
					System.out.println("The listening and callback ports will be set to default values 25333 and 25334 respectively!");
				} catch (InvalidParameterException e) {
					System.out.println(e.getMessage());
					System.out.println("The listening and callback ports will be set to default values 25333 and 25334 respectively!");
				}
			} catch (NumberFormatException e) {
				System.out.println("Unable to listen port " + newListeningPort +".");
				System.out.println("The listening and callback ports will be set to default values 25333 and 25334 respectively!");
			}
		}
		
		try {
			PythonAccessPoint pap = new PythonAccessPoint(verbose);
			
			GatewayServer gatewayServer;
			String portMessage;
			if (listeningPort != null && callbackPort != null) {
				portMessage = "ports " + listeningPort.toString() + " and " + callbackPort.toString() + "...";
				gatewayServer = new GatewayServer(pap, listeningPort, callbackPort, 0, 0, null);
			} else {
				portMessage = "default ports 25333 and 25334...";
				gatewayServer = new GatewayServer(pap); // default port
			}
			gatewayServer.start();
			System.out.println("Gateway Server started on " + portMessage);
		} catch (Exception e) {
			System.out.println("An error occurred while starting the Python gateway server!");
			e.printStackTrace();
		}
	}
	
}
