/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB
 * Copyright (C) 2020-2025 His Majesty the King in right of Canada
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
package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import lerfob.carbonbalancetool.biomassparameters.BiomassParameters;
import lerfob.carbonbalancetool.interfaces.CATAdditionalElementsProvider;
import lerfob.carbonbalancetool.interfaces.CATDeadWoodProvider;
import lerfob.carbonbalancetool.interfaces.CATSaplingsProvider;
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
	
//	public static long[] ElapsedTime = new long[5];
	
	
	/**
	 * This enum defines the different tasks performed by the InternalSwingWorker class. <p>
	 * Long tasks, i.e. those with the boolean set to true, should be listed first.
	 * @author Mathieu Fortin - December 2010
	 */
	public static enum Task {
		LOG_AND_BUCK_TREES(true), 
		GENERATE_WOODPRODUCTS(true), 
		ACTUALIZE_CARBON(true),
		RETRIEVE_SOIL_CARBON_INPUT(true),
		COMPILE_CARBON(true),
		RETRIEVE_INITIAL_CONDITIONS(false),
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
//		long initialTime;
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
		case RETRIEVE_INITIAL_CONDITIONS:
			retrieveInitialConditions();
			break;
		case LOG_AND_BUCK_TREES:
//			initialTime = System.currentTimeMillis();
			firePropertyChange("OngoingTask", null, currentTask);
			logAndBuckTrees();
//			ElapsedTime[0] += System.currentTimeMillis() - initialTime;
			break;
		case GENERATE_WOODPRODUCTS:
//			initialTime = System.currentTimeMillis();
			firePropertyChange("OngoingTask", null, currentTask);
			createEndUseWoodProductsFromWoodPieces();
//			ElapsedTime[1] += System.currentTimeMillis() - initialTime;
			break;
		case ACTUALIZE_CARBON:
//			initialTime = System.currentTimeMillis();
			firePropertyChange("OngoingTask", null, currentTask);
			actualizeCarbon();
//			ElapsedTime[2] += System.currentTimeMillis() - initialTime;
			break;
		case RETRIEVE_SOIL_CARBON_INPUT:
			firePropertyChange("OngoingTask", null, currentTask);
			retrieveSoilInputFromLivingTreesAndSimulate();
			break;
		case COMPILE_CARBON:
//			initialTime = System.currentTimeMillis();
			firePropertyChange("OngoingTask", null, currentTask);
			calculateCarbonInCompartments();
//			ElapsedTime[3] += System.currentTimeMillis() - initialTime;
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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void retrieveSoilInputFromLivingTreesAndSimulate() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();

		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Adding soil carbon input from living trees...");
		
		BiomassParameters biomassParameters = manager.getCarbonToolSettings().getCurrentBiomassParameters();
		CATIntermediateBiomassCarbonMap aboveGroundMap = new CATIntermediateBiomassCarbonMap(manager.getTimeTable(), manager.getMEMS().getInputFromLivingTreesAboveGroundMgHaArray());
		CATIntermediateBiomassCarbonMap belowGroundMap = new CATIntermediateBiomassCarbonMap(manager.getTimeTable(), manager.getMEMS().getInputFromLivingTreesBelowGroundMgHaArray());
		for (CATCompatibleStand s : manager.getTimeTable().getStandsForThisRealization()) {
			Collection<MEMSCompatibleTree> aliveTrees = (Collection) manager.treeCollManager.getTreeOfThisStatusInThisStand(StatusClass.alive, s);
			double soilCarbonMgInputFromLitterFall = biomassParameters.getLitterFallAnnualCarbonMg(aliveTrees, manager);
			aboveGroundMap.put(s, soilCarbonMgInputFromLitterFall / s.getAreaHa());
			double soilCargonMgInputFromFineRootTurnover = biomassParameters.getFineRootDetritusAnnualCarbonMg(aliveTrees, manager);
			belowGroundMap.put(s, soilCargonMgInputFromFineRootTurnover / s.getAreaHa());
		}
		aboveGroundMap.interpolateIfNeeded();
		belowGroundMap.interpolateIfNeeded();  // after this line the two carbon arrays are automatically filled in the MEMSWrapper instance
		manager.getMEMS().simulate();
	}

	@SuppressWarnings("unchecked")
	private void registerTrees() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Registering trees...");

		List<CATCompatibleStand> stands = manager.getTimeTable().getStandsForThisRealization();

		final Collection<CATCompatibleTree> retrievedTreesFromStep = new ArrayList<CATCompatibleTree>();
		for (CATCompatibleStand stand : stands) {
			for (StatusClass statusClass : StatusClass.values()) {
				retrievedTreesFromStep.clear();
				retrievedTreesFromStep.addAll(stand.getTrees(statusClass));
				if (statusClass == StatusClass.alive && stand instanceof CATSaplingsProvider) {
					retrievedTreesFromStep.addAll(((CATSaplingsProvider) stand).getSaplings());
				}
				if (!retrievedTreesFromStep.isEmpty()) {
					for (CATCompatibleTree t : retrievedTreesFromStep) {
							manager.registerTree(statusClass, stand, t);
					} 
				}
			}
		}
	}


	private void retrieveInitialConditions() {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Retrieving initial conditions if available...");

		List<CATCompatibleStand> stands = manager.getTimeTable().getStandsForThisRealization();
		CATCompatibleStand firstStand = stands.get(0);
		if (firstStand instanceof CATDeadWoodProvider) {
			int dateIndex = manager.getTimeTable().getIndexOfThisStandOnTheTimeTable(firstStand);
			manager.getCarbonToolSettings().getCurrentProductionProcessorManager().createDeadWood((CATDeadWoodProvider) firstStand, dateIndex);
		}
	}
	
	/**
	 * Task: log the trees and buck them into wood pieces
	 * @throws Exception
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void logAndBuckTrees() throws Exception {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null,"Bucking harvested trees into wood pieces...");

		manager.setSimulationValid(false);

		TreeLogger logger = caller.getCarbonToolSettings().getTreeLogger();
		Collection<CATCompatibleTree> cutTrees = manager.treeCollManager.getAllTreesOfThisStatus(StatusClass.cut);
		if (!cutTrees.isEmpty()) {
			if (caller.guiInterface != null) {
				logger.addTreeLoggerListener(caller.getUI()); 
			}
//			logger.init(convertMapIntoCollectionOfLoggableTrees());		
			logger.init(cutTrees);
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
	 * Task: process the logs into end use wood products
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "rawtypes"})
	private void createEndUseWoodProductsFromWoodPieces() throws Exception {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		ApplicationScale applicationScale = manager.getApplicationScale();

		final StatusClass cutStatus = StatusClass.cut;
		
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
					int currentDateIndex = manager.getDateIndexForThisTree(tree, cutStatus);
					int nbYearsToPreviousMeasurement = getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(manager, tree, cutStatus);
					double annualBreakdownRatio = getAnnualBreakdownRatio(applicationScale, nbYearsToPreviousMeasurement);

					double commercialVolumeM3 = biomassParameters.getCommercialVolumeM3(tree);
					double volumeM3ToBiomassMgFactor = biomassParameters.getCommercialBiomassMg(tree, manager) / commercialVolumeM3;
					double volumeM3ToCarbonMgFactor = biomassParameters.getCommercialCarbonMg(tree, manager) / commercialVolumeM3;
					Collection<WoodPiece> woodPieces = (Collection<WoodPiece>) treeLogger.getWoodPieces().get(t);
					
					double totalAboveGroundWoodPieceCarbonMg = 0d;
					double totalAboveGroundWoodPieceBiomassMg = 0d;
					double totalAboveGroundWoodPieceVolumeM3 = 0d;
					
					double totalBelowGroundWoodPieceCarbonMg = 0d;
					double totalBelowGroundWoodPieceBiomassMg = 0d;
					double totalBelowGroundWoodPieceVolumeM3 = 0d;
					
					for (WoodPiece woodPiece : woodPieces) {
						if (isCancelled()) {
							break outerLoop;
						}

						double woodPieceWeightedTotalVolumeM3 = woodPiece.getWeightedTotalVolumeM3();
						if (woodPieceWeightedTotalVolumeM3 > 0d) {
							if (woodPiece.getLogCategory().isFromStump()) {
								totalBelowGroundWoodPieceVolumeM3 += woodPieceWeightedTotalVolumeM3;
								totalBelowGroundWoodPieceBiomassMg += woodPieceWeightedTotalVolumeM3 * volumeM3ToBiomassMgFactor;
								totalBelowGroundWoodPieceCarbonMg += woodPieceWeightedTotalVolumeM3 * volumeM3ToCarbonMgFactor;
							} else {
								totalAboveGroundWoodPieceVolumeM3 += woodPieceWeightedTotalVolumeM3;
								totalAboveGroundWoodPieceBiomassMg += woodPieceWeightedTotalVolumeM3 * volumeM3ToBiomassMgFactor;
								totalAboveGroundWoodPieceCarbonMg += woodPieceWeightedTotalVolumeM3 * volumeM3ToCarbonMgFactor;
							}

							AmountMap<Element> nutrientConcentrations = null;

							if (woodPiece instanceof CATAdditionalElementsProvider) {
								nutrientConcentrations = ((CATAdditionalElementsProvider) woodPiece).getAdditionalElementConcentrations();
							}

							AmountMap<Element> woodAmountMap = new AmountMap<Element>();
							double woodVolumeM3 = woodPiece.getWeightedWoodVolumeM3() * annualBreakdownRatio;
							double woodBiomassMg = woodVolumeM3 * volumeM3ToBiomassMgFactor;
							double woodCarbonMg = woodVolumeM3 * volumeM3ToCarbonMgFactor;
							
//							if (Double.isNaN(woodVolumeM3)) {
//								int u = 0;
//							}
//							if (Double.isNaN(woodBiomassMg)) {
//								int u = 0;
//							}
//							if (Double.isNaN(woodCarbonMg)) {
//								int u = 0;
//							}
							woodAmountMap.put(Element.Volume, woodVolumeM3);
							woodAmountMap.put(Element.Biomass, woodBiomassMg);
							woodAmountMap.put(Element.C, woodCarbonMg);

							if (nutrientConcentrations != null) {
								AmountMap nutrientAmounts = nutrientConcentrations.multiplyByAScalar(woodBiomassMg);	// the amounts are expressed here in kg
								cleanAmountMapOfAdditionalElementsBeforeMerging(nutrientAmounts);	// To make sure volume biomass and carbon will not be double counted
								woodAmountMap.putAll(nutrientAmounts);
							}

							Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
							amountMaps.put(BiomassType.Wood, woodAmountMap);

							AmountMap<Element> barkAmountMap = new AmountMap<Element>();
							double barkVolumeM3 = woodPiece.getWeightedBarkVolumeM3() * annualBreakdownRatio;
							double barkBiomassMg = barkVolumeM3 * volumeM3ToBiomassMgFactor; // TODO should be the bark basic density here
							double barkCarbonMg = barkVolumeM3 * volumeM3ToCarbonMgFactor;   // TODO should be the bark content ratio here
//							if (Double.isNaN(barkVolumeM3)) {
//								int u = 0;
//							}
//							if (Double.isNaN(barkBiomassMg)) {
//								int u = 0;
//							}
//							if (Double.isNaN(barkCarbonMg)) {
//								int u = 0;
//							}
							barkAmountMap.put(Element.Volume, barkVolumeM3);
							barkAmountMap.put(Element.Biomass, barkBiomassMg);
							barkAmountMap.put(Element.C, barkCarbonMg);

							if (nutrientConcentrations != null) {
								AmountMap nutrientAmounts = nutrientConcentrations.multiplyByAScalar(barkBiomassMg);	// the amounts are expressed here in kg
								cleanAmountMapOfAdditionalElementsBeforeMerging(nutrientAmounts);	// To make sure volume biomass and carbon will not be double counted
								barkAmountMap.putAll(nutrientAmounts);
							}
							amountMaps.put(BiomassType.Bark, barkAmountMap);
							CATCompatibleTree treeOfThisWoodPiece = (CATCompatibleTree) woodPiece.getTreeFromWhichComesThisPiece();

							if (shouldBeBrokenDownAnnually(applicationScale, nbYearsToPreviousMeasurement)) {
								for (int i = 0; i < nbYearsToPreviousMeasurement; i++) {
									getProcessorManager().processWoodPiece(woodPiece.getLogCategory(), 
											currentDateIndex - i, 
											samplingUnitID, 
											amountMaps, 
											treeOfThisWoodPiece,
											cutStatus);

								}
							} else {
								getProcessorManager().processWoodPiece(woodPiece.getLogCategory(), 
										currentDateIndex, 
										samplingUnitID, 
										amountMaps, 
										treeOfThisWoodPiece,
										cutStatus);
							}
						}
					}

					double totalAboveGroundCarbonMg = biomassParameters.getAboveGroundCarbonMg(tree, manager);
					double totalAboveGroundBiomassMg = biomassParameters.getAboveGroundBiomassMg(tree, manager);
					double totalAboveGroundVolumeM3 = biomassParameters.getAboveGroundVolumeM3(tree, manager); 
					
					double unconsideredAboveGroundCarbonMg = totalAboveGroundCarbonMg - totalAboveGroundWoodPieceCarbonMg;
					double unconsideredAboveGroundBiomassMg = totalAboveGroundBiomassMg - totalAboveGroundWoodPieceBiomassMg;
					double unconsideredAboveGroundVolumeM3 = totalAboveGroundVolumeM3 - totalAboveGroundWoodPieceVolumeM3;
					
					processUnaccountedCarbon((CATCompatibleTree) t,
							cutStatus,
							unconsideredAboveGroundCarbonMg,
							unconsideredAboveGroundBiomassMg,
							unconsideredAboveGroundVolumeM3,
							currentDateIndex, 
							samplingUnitID,
							WoodyDebrisProcessorID.FineWoodyDebris,
							applicationScale);
					
					double totalBelowGroundCarbonMg = biomassParameters.getBelowGroundCarbonMg(tree, manager);
					double totalBelowGroundBiomassMg = biomassParameters.getBelowGroundBiomassMg(tree, manager);
					double totalBelowGroundVolumeM3 = biomassParameters.getBelowGroundVolumeM3(tree, manager);
					
					double unconsideredBelowGroundCarbonMg = totalBelowGroundCarbonMg - totalBelowGroundWoodPieceCarbonMg;
					double unconsideredBelowGroundBiomassMg = totalBelowGroundBiomassMg - totalBelowGroundWoodPieceBiomassMg;
					double unconsideredBelowGroundVolumeM3 = totalBelowGroundVolumeM3 - totalBelowGroundWoodPieceVolumeM3;
					
					processUnaccountedCarbon((CATCompatibleTree) t,
							cutStatus,
							unconsideredBelowGroundCarbonMg, 
							unconsideredBelowGroundBiomassMg,
							unconsideredBelowGroundVolumeM3,
							currentDateIndex, 
							samplingUnitID,
							WoodyDebrisProcessorID.CoarseWoodyDebris,
							applicationScale);
					numberOfTreesProcessed++;
					setProgress((int) (numberOfTreesProcessed * progressFactor + (double) (currentTask.ordinal()) * 100 / Task.getNumberOfLongTasks()));
				}
		}
		createWoodyDebris(StatusClass.dead, WoodyDebrisProcessorID.CoarseWoodyDebris);
		createWoodyDebris(StatusClass.dead, WoodyDebrisProcessorID.CommercialWoodyDebris);
		createWoodyDebris(StatusClass.dead, WoodyDebrisProcessorID.FineWoodyDebris);
		createWoodyDebris(StatusClass.windfall, WoodyDebrisProcessorID.CoarseWoodyDebris);
		createWoodyDebris(StatusClass.windfall, WoodyDebrisProcessorID.CommercialWoodyDebris);
		createWoodyDebris(StatusClass.windfall, WoodyDebrisProcessorID.FineWoodyDebris);
	}

	private int getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(CATCompartmentManager manager, CATCompatibleTree tree, StatusClass statusClass) {
		int currentDateIndex = manager.getDateIndexForThisTree(tree, statusClass);
		int previousDateIndex = manager.getDateIndexOfPreviousStandForThisTree(tree, statusClass);

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
			StatusClass statusClass,
			double carbonMg, 
			double biomassMg,
			double volumeM3,
			int dateIndex, 
			String samplingUnitID, 
			WoodyDebrisProcessorID WoodDebrisType,
			ApplicationScale applicationScale) {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		int nbYearsToPreviousMeasurement = getNumberOfYearsBetweenStandOfThisTreeAndPreviousStand(manager, tree, statusClass);
		double annualBreakdownRatio = getAnnualBreakdownRatio(applicationScale, nbYearsToPreviousMeasurement);
		
		if (carbonMg > 0) {
			double brokenDownCarbonMg = carbonMg * annualBreakdownRatio;
			double brokenDownBiomassMg = biomassMg * annualBreakdownRatio;
			double brokenDownVolumeM3 = volumeM3 * annualBreakdownRatio;
			
			double propWood = 1d / (1d + tree.getBarkProportionOfWoodVolume(tree.getSpeciesLocale()));	// assumes that density of the bark is approximately equal to that of the wood MF2021-09-20

			double woodCarbonMg = brokenDownCarbonMg * propWood;
			double woodBiomassMg = brokenDownBiomassMg * propWood; 
			double woodVolumeM3 = brokenDownVolumeM3 * propWood;
			
			AmountMap<Element> woodAmountMap = new AmountMap<Element>();		// No calculation for nutrients left in the forest here
			woodAmountMap.put(Element.Volume, woodVolumeM3);
			woodAmountMap.put(Element.Biomass, woodBiomassMg);
			woodAmountMap.put(Element.C, woodCarbonMg);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, woodAmountMap);
			
			double barkCarbonMg = brokenDownCarbonMg - woodCarbonMg;
			double barkBiomassMg = brokenDownBiomassMg - woodBiomassMg; 
			double barkVolumeM3 = brokenDownVolumeM3 - woodVolumeM3;
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
							statusClass,
							WoodDebrisType);
				}
			} else {
				getProcessorManager().processWoodyDebris(dateIndex, 
						samplingUnitID, 
						amountMaps, 
						tree,
						statusClass,
						WoodDebrisType);
			}
		}
	}

	private void createWoodyDebris(StatusClass statusClass, WoodyDebrisProcessorID type) {
		CATCompartmentManager manager = caller.getCarbonCompartmentManager();
		Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>> treeMap = manager.treeCollManager.getTrees(statusClass);
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
						double carbonMg = 0d, biomassMg = 0d, volumeM3 = 0d;
						switch(type) {
						case FineWoodyDebris:
							carbonMg = biomassParameters.getAboveGroundCarbonMg(t, manager) - biomassParameters.getCommercialCarbonMg(t, manager);
							biomassMg = biomassParameters.getAboveGroundBiomassMg(t, manager) - biomassParameters.getCommercialBiomassMg(t, manager);
							volumeM3 = biomassParameters.getAboveGroundVolumeM3(t, manager) - biomassParameters.getCommercialVolumeM3(t);
							break;
						case CommercialWoodyDebris:
							carbonMg = biomassParameters.getCommercialCarbonMg(t, manager);
							biomassMg = biomassParameters.getCommercialBiomassMg(t, manager);
							volumeM3 = biomassParameters.getCommercialVolumeM3(t);
							break;
						case CoarseWoodyDebris:
							carbonMg = biomassParameters.getBelowGroundCarbonMg(t, manager);
							biomassMg = biomassParameters.getBelowGroundBiomassMg(t, manager);
							volumeM3 = biomassParameters.getBelowGroundVolumeM3(t, manager);
							break;
						}
						processUnaccountedCarbon(t, statusClass, carbonMg, biomassMg, volumeM3, dateIndex, samplingUnitID, type, manager.getApplicationScale());
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

}
	

