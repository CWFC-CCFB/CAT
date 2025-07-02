/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB, 
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
package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import lerfob.carbonbalancetool.biomassparameters.BiomassParameters;
import lerfob.carbonbalancetool.memsconnectors.MEMSCompatibleTree;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.ProductionLineManager;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.carbonbalancetool.productionlines.WoodyDebrisProcessor.WoodyDebrisProcessorID;
import repicea.app.AbstractGenericTask;
import repicea.gui.REpiceaAWTEvent;
import repicea.lang.MemoryWatchDog;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.treelogger.LoggableTree;
import repicea.simulation.treelogger.TreeLogger;
import repicea.simulation.treelogger.WoodPiece;
import repicea.util.REpiceaLogManager;

@SuppressWarnings({ "serial", "deprecation" })
public class CATTask extends AbstractGenericTask {
	
	/**
	 * This enum defines the different tasks performed by the InternalSwingWorker class.
	 * @author Mathieu Fortin - December 2010
	 */
	public static enum Task {
		LOG_AND_BUCK_TREES(true), 
		GENERATE_WOODPRODUCTS(true), 
		ACTUALIZE_CARBON(true),
		RETRIEVE_SOIL_CARBON_INPUT(true),
		COMPILE_CARBON(true),
		SET_REALIZATION(false),
		SHUT_DOWN(false),
		SET_STANDLIST(false),
		UNLOCK_ENGINE(false),
		SHOW_INTERFACE(false),
		/**
		 * First task to be carried out when the calculateCarbon action is triggered.
		 * @see CATCompartmentManager#resetManager()
		 */
		RESET_MANAGER(false), 
		/**
		 * Second task to be carried out when the calculateCarbon action is triggered
		 */
		REGISTER_TREES(false),
		DISPLAY_RESULT(false),
		SET_BIOMASS_PARMS(false),
		SET_PRODUCTION_MANAGER(false);
	
		private boolean longTask;
		private static int NumberOfLongTasks = -1;	
		
		Task(boolean longTask) {
			this.longTask = longTask;
		}
	
		protected static int getNumberOfLongTasks() {
			if (NumberOfLongTasks == -1) {
				NumberOfLongTasks = 0;
				for (Task task : Task.values()) {
					if (task.longTask) {
						NumberOfLongTasks++;
					}
				}
			}
			return NumberOfLongTasks;
		}
	}

	protected static class SetProperRealizationTask extends CATTask {

		private final int realizationID;
		
		protected SetProperRealizationTask(CarbonAccountingTool caller, int realizationID) {
			super(Task.SET_REALIZATION, caller);
			this.realizationID = realizationID;
		}
		
	}
	
	private Task currentTask;
	
	private CarbonAccountingTool caller;
	
	public CATTask(Task currentTask, CarbonAccountingTool caller) {
		this.currentTask = currentTask;
		this.setName(currentTask.name());
		this.caller = caller;
		if (caller.guiInterface != null) {	// if the interface is enabled then the interface listens to this worker (for the progress bar implementation)
			super.addPropertyChangeListener(caller.guiInterface);
		}
	}

	
	@Override
	protected void doThisJob() throws Exception {
		switch (currentTask) {
		case RESET_MANAGER:
			caller.getCarbonCompartmentManager().resetManager();
			break;
		case SET_REALIZATION:
			caller.getCarbonCompartmentManager().setRealization(((SetProperRealizationTask) this).realizationID);
			break;
		case REGISTER_TREES:
			registerTrees();
			break;
		case DISPLAY_RESULT:
			caller.showResult();
			REpiceaAWTEvent.fireEvent(new REpiceaAWTEvent(CATAWTProperty.CarbonCalculationSuccessful));
			break;
		case SHOW_INTERFACE:
			caller.showUI();
			break;
		case SET_STANDLIST:
			caller.setStandList();
			REpiceaAWTEvent.fireEvent(new REpiceaAWTEvent(this, CATAWTProperty.StandListProperlySet));
			break;
		case LOG_AND_BUCK_TREES:
			firePropertyChange("OngoingTask", null, currentTask);
			logAndBuckTrees();
			break;
		case GENERATE_WOODPRODUCTS:
			firePropertyChange("OngoingTask", null, currentTask);
			createEndUseWoodProductsFromWoodPieces();
			break;
		case ACTUALIZE_CARBON:
			firePropertyChange("OngoingTask", null, currentTask);
			actualizeCarbon();
			break;
		case RETRIEVE_SOIL_CARBON_INPUT:
			firePropertyChange("OngoingTask", null, currentTask);
			retrieveSoilInputFromLivingTreesAndSimulate();
			break;
		case COMPILE_CARBON:
			firePropertyChange("OngoingTask", null, currentTask);
			calculateCarbonInCompartments();
			break;
		case SHUT_DOWN:
			firePropertyChange("Cleaning memory", null, currentTask);
			caller.requestShutdown();
			break;
		case UNLOCK_ENGINE:
			firePropertyChange("Unlocking Engine", null, currentTask);
			caller.unlockEngine();
			break;
		case SET_BIOMASS_PARMS:
			firePropertyChange("Setting biomass parameters", null, currentTask);
			caller.setBiomassParameters();
			break;
		case SET_PRODUCTION_MANAGER:
			firePropertyChange("Setting production manager", null, currentTask);
			caller.setProductionManager();
			break;
		}
	}

	@SuppressWarnings("unchecked")
	private void retrieveSoilInputFromLivingTreesAndSimulate() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();

		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Adding soil carbon input from living trees...");
		
		BiomassParameters biomassParameters = manager.getCarbonToolSettings().getCurrentBiomassParameters();
		CATIntermediateBiomassCarbonMap aboveGroundMap = new CATIntermediateBiomassCarbonMap(manager.getTimeTable(), manager.getMEMS().getInputFromLivingTreesAboveGroundMgHaArray());
		CATIntermediateBiomassCarbonMap belowGroundMap = new CATIntermediateBiomassCarbonMap(manager.getTimeTable(), manager.getMEMS().getInputFromLivingTreesBelowGroundMgHaArray());
		for (CATCompatibleStand s : manager.getTimeTable().getStandsForThisRealization()) {
			double soilCarbonMgInputFromLitterFall = biomassParameters.getLitterFallAnnualCarbonMg((Collection<MEMSCompatibleTree>) s.getTrees(StatusClass.alive), manager);
			aboveGroundMap.put(s, soilCarbonMgInputFromLitterFall / s.getAreaHa());
			double soilCargonMgInputFromFineRootTurnover = biomassParameters.getFineRootDetritusAnnualCarbonMg((Collection<MEMSCompatibleTree>) s.getTrees(StatusClass.alive), manager);
			belowGroundMap.put(s, soilCargonMgInputFromFineRootTurnover / s.getAreaHa());
		}
		aboveGroundMap.interpolateIfNeeded();
		belowGroundMap.interpolateIfNeeded();  // after this line the two carbon arrays are automatically filled in the MEMSWrapper instance
		manager.getMEMS().simulate();
	}

	@SuppressWarnings("unchecked")
	private void registerTrees() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Creating last stand if needs be and registering trees...");

		List<CATCompatibleStand> stands = manager.getTimeTable().getStandsForThisRealization();

		// retrieve the loggable trees
		Collection<CATCompatibleTree> retrievedTreesFromStep;
		for (CATCompatibleStand stand : stands) {
			for (StatusClass statusClass : StatusClass.values()) {
				retrievedTreesFromStep = stand.getTrees(statusClass);
				if (!retrievedTreesFromStep.isEmpty()) {
					for (CATCompatibleTree t : retrievedTreesFromStep) {
						if (statusClass != StatusClass.alive) {
							manager.registerTree(statusClass, stand, t);
						}
					} 
				}
			}
		}
	}


	/**
	 * Task no 1 : log the trees and buck them into wood pieces
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void logAndBuckTrees() throws Exception {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null,"Bucking harvested trees into wood pieces...");

		manager.setSimulationValid(false);

		TreeLogger logger = caller.getCarbonToolSettings().getTreeLogger();
		if (!manager.getTrees(StatusClass.cut).isEmpty()) {
			if (caller.guiInterface != null) {
				logger.addTreeLoggerListener(caller.getUI()); 
			}
			logger.init(convertMapIntoCollectionOfLoggableTrees());		
			logger.run();		// woodPieces collection is cleared here
			if (caller.guiInterface != null) {
				logger.removeTreeLoggerListener(caller.getUI()); 
			}			
			setProgress((int) (100 * (double) 1 / Task.getNumberOfLongTasks()));
		} else {
			logger.getWoodPieces().clear();
		}
	}
	

	private ProductionProcessorManager getProcessorManager() {return caller.getCarbonToolSettings().getCurrentProductionProcessorManager();}
	
	/**
	 * Task no 2 : process the logs into end use wood products
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	private void createEndUseWoodProductsFromWoodPieces() throws Exception {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		ApplicationScale applicationScale = manager.getApplicationScale();

		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Creating HWP from wood pieces...");
		
		BiomassParameters biomassParameters = manager.getCarbonToolSettings().getCurrentBiomassParameters();
		getProcessorManager().resetCarbonUnitMap();
		if (!caller.getCarbonToolSettings().getTreeLogger().getWoodPieces().isEmpty()) {
			int numberOfTreesProcessed = 0;
			double progressFactor = (double) 100 / caller.getCarbonToolSettings().getTreeLogger().getWoodPieces().size() / Task.values().length;
			TreeLogger treeLogger = caller.getCarbonToolSettings().getTreeLogger();
			outerLoop:
				for (LoggableTree t : (Collection<LoggableTree>) treeLogger.getWoodPieces().keySet()) {

					String samplingUnitID = CATCompartmentManager.getSamplingUnitID((CATCompatibleTree) t);
					MemoryWatchDog.checkAvailableMemory();		// memory check before going further on

					CATCompatibleTree tree = (CATCompatibleTree) t;
					int currentDateIndex = manager.getDateIndexForThisTree(tree);
					int nbYearsToPreviousMeasurement = getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(manager, tree);
					double annualBreakdownRatio = getAnnualBreakdownRatio(applicationScale, nbYearsToPreviousMeasurement);
					double carbonContentRatio = biomassParameters.getCarbonContentFromThisTree(tree, manager);
					double basicWoodDensityMgM3 = biomassParameters.getBasicWoodDensityFromThisTree(tree, manager);

					Collection<WoodPiece> woodPieces = (Collection<WoodPiece>) treeLogger.getWoodPieces().get(t);
					double totalAboveGroundWoodPieceCarbonMg = 0d;
					double totalBelowGroundWoodPieceCarbonMg = 0d;
					for (WoodPiece woodPiece : woodPieces) {
						if (isCancelled()) {
							break outerLoop;
						}

						if (woodPiece.getLogCategory().isFromStump()) {
							totalBelowGroundWoodPieceCarbonMg += woodPiece.getWeightedTotalVolumeM3() * basicWoodDensityMgM3 * carbonContentRatio;
						} else {
							totalAboveGroundWoodPieceCarbonMg += woodPiece.getWeightedTotalVolumeM3() * basicWoodDensityMgM3 * carbonContentRatio;
						}

						AmountMap<Element> nutrientConcentrations = null;

						if (woodPiece instanceof CATAdditionalElementsProvider) {
							nutrientConcentrations = ((CATAdditionalElementsProvider) woodPiece).getAdditionalElementConcentrations();
						}

						AmountMap<Element> woodAmountMap = new AmountMap<Element>();
						double woodVolumeM3 = woodPiece.getWeightedWoodVolumeM3() * annualBreakdownRatio;

						double woodBiomassMg = woodVolumeM3 * basicWoodDensityMgM3;
						double woodCarbonMg = woodBiomassMg * carbonContentRatio;
						woodAmountMap.put(Element.Volume, woodVolumeM3);
						woodAmountMap.put(Element.Biomass, woodBiomassMg);
						woodAmountMap.put(Element.C, woodCarbonMg);

						if (nutrientConcentrations != null) {
							AmountMap nutrientAmounts = nutrientConcentrations.multiplyByAScalar(woodBiomassMg * basicWoodDensityMgM3);	// the amounts are expressed here in kg
							cleanAmountMapOfAdditionalElementsBeforeMerging(nutrientAmounts);	// To make sure volume biomass and carbon will not be double counted
							woodAmountMap.putAll(nutrientAmounts);
						}

						Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
						amountMaps.put(BiomassType.Wood, woodAmountMap);

						AmountMap<Element> barkAmountMap = new AmountMap<Element>();
						double barkVolumeM3 = woodPiece.getWeightedBarkVolumeM3() * annualBreakdownRatio;
						double barkBiomassMg = barkVolumeM3 * basicWoodDensityMgM3; // TODO should be the bark basic density here
						double barkCarbonMg = barkBiomassMg * carbonContentRatio;   // TODO should be the bark content ratio here
						barkAmountMap.put(Element.Volume, barkVolumeM3);
						barkAmountMap.put(Element.Biomass, barkBiomassMg);
						barkAmountMap.put(Element.C, barkCarbonMg);

						if (nutrientConcentrations != null) {
							AmountMap nutrientAmounts = nutrientConcentrations.multiplyByAScalar(woodPiece.getWeightedBarkVolumeM3() * basicWoodDensityMgM3);	// the amounts are expressed here in kg
							cleanAmountMapOfAdditionalElementsBeforeMerging(nutrientAmounts);	// To make sure volume biomass and carbon will not be double counted
							barkAmountMap.putAll(nutrientAmounts);
						}
						amountMaps.put(BiomassType.Bark, barkAmountMap);

						if (shouldBeBrokenDownAnnually(applicationScale, nbYearsToPreviousMeasurement)) {
							for (int i = 0; i < nbYearsToPreviousMeasurement; i++) {
								getProcessorManager().processWoodPiece(woodPiece.getLogCategory(), 
										currentDateIndex - i, 
										samplingUnitID, 
										amountMaps, 
										(CATCompatibleTree) woodPiece.getTreeFromWhichComesThisPiece());

							}
						} else {
							getProcessorManager().processWoodPiece(woodPiece.getLogCategory(), 
									currentDateIndex, 
									samplingUnitID, 
									amountMaps, 
									(CATCompatibleTree) woodPiece.getTreeFromWhichComesThisPiece());
						}

					}

					double totalAboveGroundCarbonMg = biomassParameters.getAboveGroundCarbonMg(tree, manager);
					double unconsideredAboveGroundCarbonMg = totalAboveGroundCarbonMg - totalAboveGroundWoodPieceCarbonMg;
					processUnaccountedCarbon((CATCompatibleTree) t,
							unconsideredAboveGroundCarbonMg, 
							currentDateIndex, 
							samplingUnitID,
							WoodyDebrisProcessorID.FineWoodyDebris,
							applicationScale);
					double totalBelowGroundCarbonMg = biomassParameters.getBelowGroundCarbonMg(tree, manager);
					double unconsideredBelowGroundCarbonMg = totalBelowGroundCarbonMg - totalBelowGroundWoodPieceCarbonMg;
					processUnaccountedCarbon((CATCompatibleTree) t,
							unconsideredBelowGroundCarbonMg, 
							currentDateIndex, 
							samplingUnitID,
							WoodyDebrisProcessorID.CoarseWoodyDebris,
							applicationScale);
					numberOfTreesProcessed++;
					setProgress((int) (numberOfTreesProcessed * progressFactor + (double) (currentTask.ordinal()) * 100 / Task.getNumberOfLongTasks()));
				}
		}
		createWoodyDebris(manager.getTrees(StatusClass.dead), WoodyDebrisProcessorID.CoarseWoodyDebris);
		createWoodyDebris(manager.getTrees(StatusClass.dead), WoodyDebrisProcessorID.CommercialWoodyDebris);
		createWoodyDebris(manager.getTrees(StatusClass.dead), WoodyDebrisProcessorID.FineWoodyDebris);
		createWoodyDebris(manager.getTrees(StatusClass.windfall), WoodyDebrisProcessorID.CoarseWoodyDebris);
		createWoodyDebris(manager.getTrees(StatusClass.windfall), WoodyDebrisProcessorID.CommercialWoodyDebris);
		createWoodyDebris(manager.getTrees(StatusClass.windfall), WoodyDebrisProcessorID.FineWoodyDebris);
	}

	private int getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(CATCompartmentManager manager, CATCompatibleTree tree) {
		int currentDateIndex = manager.getDateIndexForThisTree(tree);
		int previousDateIndex = manager.getDateIndexOfPreviousStandForThisTree(tree);

		int nbYears;
		if (previousDateIndex == -1 && currentDateIndex == 0) { // happens if the first stand is a harvested stand
			nbYears = 0;
		} else {
			nbYears = manager.getTimeTable().getDateYrAtThisIndex(currentDateIndex) - manager.getTimeTable().getDateYrAtThisIndex(previousDateIndex);
		}
		return nbYears;
	}
	
	private double getAnnualBreakdownRatio(ApplicationScale applicationScale, int nbYearsToPreviousMeasurement) {
		double extendToAllYearRatio;
		if (shouldBeBrokenDownAnnually(applicationScale, nbYearsToPreviousMeasurement)) {
			extendToAllYearRatio = 1d / nbYearsToPreviousMeasurement;
		} else {
			extendToAllYearRatio = 1d;
		}
		return extendToAllYearRatio;
	}

	private boolean shouldBeBrokenDownAnnually(ApplicationScale applicationScale, int nbYearsToPreviousMeasurement) {
		return applicationScale == ApplicationScale.FMU && nbYearsToPreviousMeasurement > 0;
	}
	
	private void cleanAmountMapOfAdditionalElementsBeforeMerging(AmountMap<Element> additionalElement) {
		additionalElement.remove(Element.Volume);
		additionalElement.remove(Element.Biomass);
		additionalElement.remove(Element.C);
	}
	
	
	private void processUnaccountedCarbon(CATCompatibleTree tree, 
			double carbonMg, 
			int dateIndex, 
			String samplingUnitID, 
			WoodyDebrisProcessorID WoodDebrisType,
			ApplicationScale applicationScale) {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		int nbYearsToPreviousMeasurement = getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(manager, tree);
		double annualBreakdownRatio = getAnnualBreakdownRatio(applicationScale, nbYearsToPreviousMeasurement);
		
		
		BiomassParameters biomassParameters = manager.getCarbonToolSettings().getCurrentBiomassParameters();
		double carbonContentRatio = biomassParameters.getCarbonContentFromThisTree(tree, manager);
		double basicWoodDensityMgM3 = biomassParameters.getBasicWoodDensityFromThisTree(tree, manager);
		if (carbonMg > 0) {
			double brokenDownCarbonMg = carbonMg * annualBreakdownRatio;
			double propWood = 1d / (1d + tree.getBarkProportionOfWoodVolume());	// assumes that density of the bark is approximately equal to that of the wood MF2021-09-20

			double woodCarbonMg = brokenDownCarbonMg * propWood;
			
			double woodBiomassMg = woodCarbonMg / carbonContentRatio; 
			double woodVolumeM3 = woodBiomassMg / basicWoodDensityMgM3;
			AmountMap<Element> woodAmountMap = new AmountMap<Element>();		// No calculation for nutrients left in the forest here
			woodAmountMap.put(Element.Volume, woodVolumeM3);
			woodAmountMap.put(Element.Biomass, woodBiomassMg);
			woodAmountMap.put(Element.C, woodCarbonMg);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, woodAmountMap);
			
			double barkCarbonMg = brokenDownCarbonMg - woodCarbonMg;
			double barkBiomassMg = barkCarbonMg / carbonContentRatio; 
			double barkVolumeM3 = barkBiomassMg /  basicWoodDensityMgM3;
			AmountMap<Element> barkAmountMap = new AmountMap<Element>();						// No calculation for nutrients left in the forest here
			barkAmountMap.put(Element.Volume, barkVolumeM3);
			barkAmountMap.put(Element.Biomass, barkBiomassMg);
			barkAmountMap.put(Element.C, barkCarbonMg);
			amountMaps.put(BiomassType.Bark, barkAmountMap);

			if (shouldBeBrokenDownAnnually(applicationScale, nbYearsToPreviousMeasurement)) {
				for (int i = 0; i < nbYearsToPreviousMeasurement; i++) {
					getProcessorManager().processWoodyDebris(dateIndex - i, 
							samplingUnitID, 
							amountMaps, 
							tree,
//							tree.getSpeciesName(), 
//							tree.getSpeciesType(), 
//							tree.getStatusClass(),
							WoodDebrisType);
				}
			} else {
				getProcessorManager().processWoodyDebris(dateIndex, 
						samplingUnitID, 
						amountMaps, 
						tree,
//						tree.getSpeciesName(), 
//						tree.getSpeciesType(),
//						tree.getStatusClass(),
						WoodDebrisType);
			}
		}
	}

	private void createWoodyDebris(Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>> treeMap, WoodyDebrisProcessorID type) {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		BiomassParameters biomassParameters = manager.getCarbonToolSettings().getCurrentBiomassParameters();
		for (CATCompatibleStand stand : treeMap.keySet()) {
			if (isCancelled()) {
				break;
			}
			int dateIndex = caller.getCarbonCompartmentManager().getTimeTable().getIndexOfThisStandOnTheTimeTable(stand);
			Map<String, Map<String, Collection<CATCompatibleTree>>> oMap = treeMap.get(stand);
			for (String samplingUnitID : oMap.keySet()) {
				Map<String, Collection<CATCompatibleTree>> oInnerMap = oMap.get(samplingUnitID);
				for (String speciesName : oInnerMap.keySet()) {
					Collection<CATCompatibleTree> trees = oInnerMap.get(speciesName);
					for (CATCompatibleTree t : trees) {
						double carbonMg = 0d;
						switch(type) {
						case FineWoodyDebris:
							carbonMg = biomassParameters.getAboveGroundCarbonMg(t, manager) - biomassParameters.getCommercialCarbonMg(t, manager);
							break;
						case CommercialWoodyDebris:
							carbonMg = biomassParameters.getCommercialCarbonMg(t, manager);
							break;
						case CoarseWoodyDebris:
							carbonMg = biomassParameters.getBelowGroundCarbonMg(t, manager);
							break;
						}
						processUnaccountedCarbon(t, carbonMg, dateIndex, samplingUnitID, type, manager.getApplicationScale());
					}
				}				
			}
		}
	}
	
	/**
	 * Task no 3 : actualize the carbon units through time
	 * @throws Exception
	 */
	private void actualizeCarbon() throws Exception {
		setProgress((int) ((double) (currentTask.ordinal()) * 100d / Task.values().length));
		if (!caller.getCarbonToolSettings().formerImplementation) {
			getProcessorManager().actualizeCarbonUnits(caller.getCarbonCompartmentManager());
		} else {
			ProductionLineManager marketManager = caller.getCarbonToolSettings().getProductionLines();
			marketManager.actualizeCarbonUnits(caller.getCarbonCompartmentManager());
		}
		setProgress((int) ((double) (currentTask.ordinal() + 1) * 100d / Task.getNumberOfLongTasks()));
	}
	
	
	/**
	 * Task 4 : calculate the carbon in the different compartments.
	 * @throws Exception
	 */
	private void calculateCarbonInCompartments() throws Exception {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Calculating carbon in the different compartments...");
		
		manager.resetCompartmentsAndSetCarbonUnitCollections();
		
		double progressFactor = (double) 100d / manager.getCompartments().size() / Task.values().length;
		int compIter = 0;
		for (CATCompartment carbonCompartment : manager.getCompartments().values()) {
			if (isCancelled()) {
				break;
			}
			carbonCompartment.calculateAndIntegrateCarbon();
			compIter++;
			setProgress((int) (compIter * progressFactor + (double) (currentTask.ordinal() * 100 / Task.getNumberOfLongTasks())));
			REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Integrated carbon in compartment " + carbonCompartment.getCompartmentID().name() + " = " + carbonCompartment.getIntegratedCarbon());
		}
		manager.setSimulationValid(true);
		manager.storeResults();
	}

	private Collection<CATCompatibleTree> convertMapIntoCollectionOfLoggableTrees() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		Collection<CATCompatibleTree> loggableTreesCollection = new ArrayList<CATCompatibleTree>();
		for (Map<String, Map<String, Collection<CATCompatibleTree>>> oMap : manager.getTrees(StatusClass.cut).values()) {
			for (Map<String, Collection<CATCompatibleTree>> oInnerMap : oMap.values()) {
				for (Collection<CATCompatibleTree> oColl : oInnerMap.values()) {
					loggableTreesCollection.addAll(oColl);
				}
			}
		}
		return loggableTreesCollection;
	}
	

//	private double getCarbonFromCarbonUnitList(Collection<CarbonUnit> carbonUnits) {
//		double sum = 0;
//		for (CarbonUnit unit : carbonUnits) {
//			sum += unit.getInitialCarbon();
//		}
//		return sum;
//	}
	

	
}
	

