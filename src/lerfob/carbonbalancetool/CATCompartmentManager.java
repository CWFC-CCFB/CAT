/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB, 
 * Copyright (C) 2019-2024 His Majesty the King in right of Canada
 * Author, Mathieu Fortin, Canadian Forest Service
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
import java.util.TreeMap;
import java.util.logging.Level;

import lerfob.carbonbalancetool.CATCompartment.CompartmentInfo;
import lerfob.carbonbalancetool.memsconnectors.MEMSCompatibleStand;
import lerfob.carbonbalancetool.memsconnectors.MEMSCompatibleTree;
import lerfob.carbonbalancetool.memsconnectors.MEMSWrapper;
import lerfob.carbonbalancetool.productionlines.CarbonUnit;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnitList;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnit;
import lerfob.carbonbalancetool.productionlines.ProductionLineManager;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import repicea.simulation.HierarchicalLevel;
import repicea.simulation.MonteCarloSimulationCompliantObject;
import repicea.simulation.covariateproviders.plotlevel.StochasticInformationProvider;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.SamplingUnitIDProvider;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.util.REpiceaLogManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("deprecation")
public class CATCompartmentManager implements MonteCarloSimulationCompliantObject {

	private static enum MessageID implements TextableEnum {
		UnevenAgedWarning("Carbon balance calculation under infinite sequence will be allowed. Its validity will depend on how similar the initial and final stands of the simulation are.",
				"Le calcul du bilan de carbon en s\u00E9quence infinie sera possible. La validit\u00E9 de ce calcul d\u00E9pendra toutefois de la ressemblance entre les peuplements initial et final de la simulation."),
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
	
	
	private static int NumberOfExtraYrs = 80;	// number of years after the final cut

//	/**
//	 * The treeCollections member is a four-level map.<p>
//	 * The keys are:<ol>
//	 * <li> the StatusClass enum
//	 * <li> the CATCompatibleStand instance
//	 * <li> the samplingUnitId (String)
//	 * <li> the species (String)
//	 * </ol>
//	 * The value is a Collection of CATCompatibleTree instances
//	 */
//	private final Map<StatusClass, Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>>> treeCollections;

	final TreeCollectionManager treeCollManager;
	
	/**
	 * A List of all the species found in the CATCompatibleStand instances.<p>
	 * This list is set when the {@link CATCompartmentManager#init(List)} method is called.
	 */
	private final List<String> speciesList;

	/**
	 * This member includes all the realizations and should not be used within the realization.
	 */
	List<CATCompatibleStand> completeStandList;
	
	private CATSettings carbonAccountingToolSettings;		// reference to the extractor settings
	
	private Map<CompartmentInfo, CATCompartment> carbonCompartments;
	private int rotationLength;
	private boolean isInfiniteSequenceAllowed;
	private CATTimeTable timeTable;

	private boolean isSimulationValid;
	private int nbSimulations = 0;
	private final CarbonAccountingTool caller;
	private ManagementType managementType;
	
	protected CATSingleSimulationResult summary;
	private boolean isMEMSEnabled;
	private final MEMSWrapper memsWrapper; 
	
	
	/**
	 * Constructor.
	 * @param caller the CarbonAccountingTool instance
	 * @param settings a CATSettings instance
	 */
	protected CATCompartmentManager(CarbonAccountingTool caller, CATSettings settings) {
		this.caller = caller;
//		treeCollections = new HashMap<StatusClass, Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>>>();
		treeCollManager = new TreeCollectionManager();
		speciesList = new ArrayList<String>();
		
		this.carbonAccountingToolSettings = settings;
		this.carbonCompartments = new TreeMap<CompartmentInfo, CATCompartment>();	// TreeMap to make sure the merge compartments are not called before the regular compartment
		isSimulationValid = false;

		memsWrapper = new MEMSWrapper(this);

		initializeCompartments();
	}
		
	/**
	 * Indicate whether MEMS soil module is enabled. 
	 * @return a boolean
	 */
	public boolean isMEMSEnabled() {return isMEMSEnabled;}
	
	/**
	 * Trees are registered in the treeCollections map and the treeRegister map immediately after the manager has been reset following
	 * the triggering of the calculateCarbon action.
	 * @param statusClass a StatusClass enum
	 * @param stand a CATCompatibleStand instance
	 * @param tree a CATCompatibleTree instance
	 */
	protected void registerTree(StatusClass statusClass, CATCompatibleStand stand, CATCompatibleTree tree) {
		treeCollManager.add(statusClass, stand, tree);
	}

	
	int getDateIndexForThisTree(CATCompatibleTree tree) {
		CATCompatibleStand stand = treeCollManager.getStandOfThisTree(tree);
		return stand == null ?
				-1 :
					getTimeTable().getIndexOfThisStandOnTheTimeTable(stand);
	}
	
	int getDateIndexOfPreviousStandForThisTree(CATCompatibleTree tree) {
		CATCompatibleStand stand = treeCollManager.getStandOfThisTree(tree);
		if (stand != null) {
			int currentIndexOfThisStandAmongStands = getTimeTable().getStandsForThisRealization().lastIndexOf(stand);
			if (currentIndexOfThisStandAmongStands > 0) {	// must be at least in the second slot
				stand = getTimeTable().getStandsForThisRealization().get(currentIndexOfThisStandAmongStands - 1); // get the previous stand
				return getTimeTable().getIndexOfThisStandOnTheTimeTable(stand);
			}
		} 
		return -1;
	}
	
	private void clearTreeCollections() {
		treeCollManager.clear();
//		speciesList.clear();  // is now set through the init method
	}

	protected List<String> getSpeciesList() {
		return speciesList;
	}


	protected void setSimulationValid(boolean isSimulationValid) {
		this.isSimulationValid = isSimulationValid;
	}
	
	private boolean canBeRunInInfiniteSequence(CATCompatibleStand lastStand, int nRealizations) {
		if (lastStand.getApplicationScale() == ApplicationScale.Stand && nRealizations == 1) {	// we can hardly deal with multiple realizations in an infinite sequence because the stand may be ready for final harvesting in one realization but not in the others
			if (lastStand.getManagementType() == ManagementType.EvenAged) {
				return true; 
			} else { // then uneven-aged management type
				if (lastStand.isInterventionResult()) { // but last stand has been harvested; 
					REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.WARNING, null, MessageID.UnevenAgedWarning.toString());
					if (caller.isGuiEnabled()) {
						caller.getUI().displayWarningMessage(MessageID.UnevenAgedWarning.toString());
					}
					return true;
				}
			}
		}
		return false; // any other case
	}
	
	/**
	 * Initialize the carbon balance simulation. <p>
	 * This method determines 
	 * <ul>
	 * <li> the management type (even aged or uneven aged)
	 * <li> the rotation length (or cutting cycle in case of uneven-aged management)
	 * <li> whether the simulation can be run in infinite sequence
	 * <li> the number of Monte Carlo realizations (set to 1 if the model is deterministic)
	 * <li> the list of species found in the simulation
	 * </ul> 
	 * @param stands a List of CATCompatibleStand instances
	 */
	@SuppressWarnings("unchecked")
	public void init(List<CATCompatibleStand> stands) {
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Setting stand list and creating last stand if needs be...");
		this.completeStandList = stands;
		if (stands != null) {
			CATCompatibleStand lastStand = stands.get(stands.size() - 1);
			managementType = lastStand.getManagementType();
			int nRealizations = getNumberOfRealizations(lastStand);
			isInfiniteSequenceAllowed = canBeRunInInfiniteSequence(lastStand, nRealizations);
			boolean isStochastic = isStochastic(lastStand);
			CATSensitivityAnalysisSettings.getInstance().setModelStochastic(isStochastic);
			CATSensitivityAnalysisSettings.getInstance().setNumberOfMonteCarloRealizations(nRealizations);
			int nbExtraYears = 0;
			int initialAgeYr = -999;
			if (isInfiniteSequenceAllowed && lastStand.getManagementType() == ManagementType.EvenAged) {
				if (!lastStand.getTrees(StatusClass.alive).isEmpty()) { // even aged but the last stand has not been fully harvested
					CATCompatibleStand stand = lastStand.getHarvestedStand();
					stands.add(stand);
					lastStand = stand;
					caller.setFinalCutHadToBeCarriedOut(true);
				} 					
				rotationLength = lastStand.getAgeYr();
				initialAgeYr = stands.get(0).getAgeYr();
				nbExtraYears = NumberOfExtraYrs;
			} else { // infinite sequence might be allowed but management is uneven-aged
				rotationLength = lastStand.getDateYr() - stands.get(0).getDateYr();
			}
				
			timeTable = new CATTimeTable(stands, initialAgeYr, nbExtraYears);

			// scan all the trees to identify the different species and store their codes in the speciesList member
			speciesList.clear();
			for (CATCompatibleStand s : stands) {
				for (StatusClass sc : StatusClass.values()) {
					Collection<CATCompatibleTree> trees = s.getTrees(sc);
					for (CATCompatibleTree tree : trees) {
						if (!speciesList.contains(tree.getSpeciesName())) {
							speciesList.add(tree.getSpeciesName());
						}
					}
				}
			}

			// check if mems must be enabled
			CATCompatibleStand firstStand = stands.get(0);
			if (firstStand.getApplicationScale() == ApplicationScale.Stand &&
					firstStand instanceof MEMSCompatibleStand && 
					((MEMSCompatibleStand) firstStand).getSiteType() != null) {
				isMEMSEnabled = true;	// default
				outer:
				for (CATCompatibleStand s : stands) {
					for (StatusClass sc : StatusClass.values()) {
						Collection<CATCompatibleTree> trees = s.getTrees(sc);
						for (CATCompatibleTree tree : trees) {
							if (!(tree instanceof MEMSCompatibleTree)) {
								isMEMSEnabled = false;	// false if at least one tree does not implement MEMSCompatibleTree
								break outer;
							}
						}
					}
				}
			} else { // if application scale is not stand then mems is disabled
				isMEMSEnabled = false;
			}
		}
	}
	
	
	/**
	 * Check if the stand implements Monte Carlo features and retrieve the number of Monte Carlo 
	 * realizations.  
	 * @return the number of Monte Carlo realizations or 1 if either the stand does not implement
	 * Monte Carlo feature or these are not compatible
	 */
	private int getNumberOfRealizations(CATCompatibleStand stand) {
		if (stand instanceof StochasticInformationProvider) {
			StochasticInformationProvider<?> stochProv = (StochasticInformationProvider<?>) stand;
			List<Integer> monteCarloIds = stochProv.getRealizationIds();
			if (stochProv.isStochastic() && stochProv.getRealization(monteCarloIds.get(0)) instanceof CATCompatibleStand) {
				return monteCarloIds.size();
			}
		} 
		return 1;
	}

	private boolean isStochastic(CATCompatibleStand stand) {
		if (stand instanceof StochasticInformationProvider) {
			StochasticInformationProvider<?> stochProv = (StochasticInformationProvider<?>) stand;
			List<Integer> monteCarloIds = stochProv.getRealizationIds();
			if (stochProv.isStochastic() && stochProv.getRealization(monteCarloIds.get(0)) instanceof CATCompatibleStand) {
				return true;
			}
		} 
		return false;
	}

	static String getSamplingUnitID(Object treeOrStand) {
		return treeOrStand instanceof SamplingUnitIDProvider ? 
				((SamplingUnitIDProvider) treeOrStand).getSamplingUnitID() : 
					"";
	}
	
	/**
	 * The first task to be carried out when the calculateCarbon action is triggered is to reset the
	 * manager.
	 */
	protected void resetManager() {
		clearTreeCollections();
		resetCompartments();

		if (getCarbonToolSettings().formerImplementation) {
			ProductionLineManager productionLines = carbonAccountingToolSettings.getProductionLines();
			productionLines.resetCarbonUnitMap();
		} else {
			getCarbonToolSettings().getCurrentProductionProcessorManager().resetCarbonUnitMap();
		}
	}

	
	/**
	 * This method returns the TimeScale instance the simulation has been run with.
	 * @return a CATTimeTable instance
	 */
	public CATTimeTable getTimeTable() {return timeTable;}
	
	public CATSettings getCarbonToolSettings() {return carbonAccountingToolSettings;}

	@SuppressWarnings({ "unchecked"})
	protected void resetCompartmentsAndSetCarbonUnitCollections() {
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, null, "Resetting compartment...");
		CarbonUnitList joinEndUseProductRecyclageList = new CarbonUnitList();
		CarbonUnitList leftInForestList;
		CarbonUnitList degradableLandfillList;
		CarbonUnitList nonDegradableLandfillList;
		if 	(!getCarbonToolSettings().formerImplementation) {
			ProductionProcessorManager productionLineManager = getCarbonToolSettings().getCurrentProductionProcessorManager();
			joinEndUseProductRecyclageList.addAll(productionLineManager.getCarbonUnits(CarbonUnitStatus.EndUseWoodProduct));
			joinEndUseProductRecyclageList.addAll(productionLineManager.getCarbonUnits(CarbonUnitStatus.Recycled));
			leftInForestList = productionLineManager.getCarbonUnits(CarbonUnitStatus.DeadWood);
			degradableLandfillList = productionLineManager.getCarbonUnits(CarbonUnitStatus.LandFillDegradable);
			nonDegradableLandfillList = productionLineManager.getCarbonUnits(CarbonUnitStatus.LandFillNonDegradable);
		} else {
			ProductionLineManager productionLineManager = getCarbonToolSettings().getProductionLines();
			joinEndUseProductRecyclageList.addAll(productionLineManager.getCarbonUnits(CarbonUnitStatus.EndUseWoodProduct));
			joinEndUseProductRecyclageList.addAll(productionLineManager.getCarbonUnits(CarbonUnitStatus.Recycled));
			leftInForestList = productionLineManager.getCarbonUnits(CarbonUnitStatus.DeadWood);
			degradableLandfillList = productionLineManager.getCarbonUnits(CarbonUnitStatus.LandFillDegradable);
			nonDegradableLandfillList = productionLineManager.getCarbonUnits(CarbonUnitStatus.LandFillNonDegradable);
			
		}
		Collection<EndUseWoodProductCarbonUnit>[] endUseWoodProductCarbonUnits = (Collection<EndUseWoodProductCarbonUnit>[]) formatCarbonUnits(getTimeTable(), joinEndUseProductRecyclageList);
		Collection<CarbonUnit>[] leftInForestCarbonUnits = formatCarbonUnits(getTimeTable(), leftInForestList);
		Collection<CarbonUnit>[] degradableLandfillCarbonUnits = formatCarbonUnits(getTimeTable(), degradableLandfillList);
		Collection<CarbonUnit>[] nonDegradableLandfillCarbonUnits = formatCarbonUnits(getTimeTable(), nonDegradableLandfillList);


		resetCompartments();

		carbonCompartments.get(CompartmentInfo.CarbEmis).setCarbonUnitsArray(endUseWoodProductCarbonUnits);
		carbonCompartments.get(CompartmentInfo.EnerSubs).setCarbonUnitsArray(endUseWoodProductCarbonUnits);
		carbonCompartments.get(CompartmentInfo.Products).setCarbonUnitsArray(endUseWoodProductCarbonUnits);
		carbonCompartments.get(CompartmentInfo.WComb).setCarbonUnitsArray(endUseWoodProductCarbonUnits);

		// dead biomass
		carbonCompartments.get(CompartmentInfo.DeadBiom).setCarbonUnitsArray(leftInForestCarbonUnits);
	
		// landfill wood products
		carbonCompartments.get(CompartmentInfo.LfillDeg).setCarbonUnitsArray(degradableLandfillCarbonUnits);
		carbonCompartments.get(CompartmentInfo.LfillEm).setCarbonUnitsArray(degradableLandfillCarbonUnits);
		carbonCompartments.get(CompartmentInfo.LfillND).setCarbonUnitsArray(nonDegradableLandfillCarbonUnits);
	}
	
	
	/**
	 * This method is called just before calculating the carbon in the compartments. It deletes all the carbon values in the different compartments.
	 */
	private void resetCompartments() {
		for (CATCompartment compartment : this.carbonCompartments.values()) {
			compartment.resetCarbon();
		}
	}
	

	/**
	 * This method initializes the different carbon compartments.
	 * @throws Exception
	 */
	private void initializeCompartments() {
		for (CompartmentInfo compartmentInfo : CompartmentInfo.values()) {
			switch (compartmentInfo) {
			case Roots:
			case AbGround:
			case DeadBiom:
			case LfillDeg:
			case LfillND:
			case LfillEm:
				carbonCompartments.put(compartmentInfo,	new CATCompartment(this, compartmentInfo));
				break;
			case CarbEmis:
			case Products:
			case EnerSubs:
			case WComb:
				carbonCompartments.put(compartmentInfo, new CATProductCompartment(this, compartmentInfo));
				break;
			case LivingBiomass:
				CATCompartment standing = new CATCompartment(this, compartmentInfo);
				standing.addFatherCompartment(carbonCompartments.get(CompartmentInfo.AbGround));
				standing.addFatherCompartment(carbonCompartments.get(CompartmentInfo.Roots));
				this.carbonCompartments.put(compartmentInfo, standing);
				break;
			case TotalProducts:
				CATCompartment overallNet = new CATCompartment(this, compartmentInfo);
				overallNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.Products));
				overallNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.LfillDeg));
				this.carbonCompartments.put(compartmentInfo, overallNet);
				break;
			case NetSubs:
				CATCompartment substitutionNet = new CATCompartment(this, compartmentInfo);
				substitutionNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.EnerSubs));
				substitutionNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.CarbEmis));
				substitutionNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.LfillEm));
				substitutionNet.addFatherCompartment(carbonCompartments.get(CompartmentInfo.LfillND));
				this.carbonCompartments.put(compartmentInfo, substitutionNet);
				break;
			case Humus:
				carbonCompartments.put(compartmentInfo,	new CATCompartment(this, compartmentInfo));
				break;
			case MineralSoil:
				carbonCompartments.put(compartmentInfo,	new CATCompartment(this, compartmentInfo));
				break;
			case Soil:
				CATCompartment soil = new CATCompartment(this, compartmentInfo);
				soil.addFatherCompartment(carbonCompartments.get(CompartmentInfo.Humus));
				soil.addFatherCompartment(carbonCompartments.get(CompartmentInfo.MineralSoil));
				carbonCompartments.put(compartmentInfo,	soil);
				break;
			}
		}

	}
	

	public Map<CompartmentInfo, CATCompartment> getCompartments() {return this.carbonCompartments;}

	/**
	 * This method returns the last stand from the list of stands. 
	 * @return a CarbonToolCompatibleStand or null if the list is null or empty
	 */
	public CATCompatibleStand getLastCompleteStand() {
		if (completeStandList != null && !completeStandList.isEmpty()) {
			return completeStandList.get(completeStandList.size() - 1);
		} else {
			return null;
		}
	}
		
	ApplicationScale getApplicationScale() {
		if (completeStandList != null && !completeStandList.isEmpty()) {
			return completeStandList.get(0).getApplicationScale();
		} else {
			return null;
		}
	}
	
//	/**
//	 * Return the list of stands as set by the init method. This method should not be called within
//	 * the realization because the stands refer to wrappers that contain all the realizations. 
//	 * Within the realization, the method CATTimeTable.getStandsForThisRealization should be called 
//	 * instead.
//	 * @return a List of CATCompatibleStand instances
//	 */
//	protected List<CATCompatibleStand> getStandList() {return stands;}
	
	
	/**
	 * This method returns the rotation length in year.
	 * @return an integer
	 */
	protected int getRotationLength() {return rotationLength;}


	/**
	 * This method format the end products into an array of collection of end products. The array has the time scale as index.
	 * @param timeScale a TimeScale instance
	 * @param endProductsCollections a Collection of Collections of EndProduct instances
	 * @return an array of Collections of EndProduct instances
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Collection[] formatCarbonUnits(CATTimeTable timeScale, Collection<? extends CarbonUnit> carbonProducts) {
		Collection<CarbonUnit>[] outputArray = new Collection[timeScale.size()];
		for (int i = 0; i < outputArray.length; i++) {
			outputArray[i] = new ArrayList<CarbonUnit>();
		}
		
		if (carbonProducts!= null && !carbonProducts.isEmpty()) {
			for (CarbonUnit carbonUnit : carbonProducts) {
				outputArray[carbonUnit.getIndexInTimeScale()].add(carbonUnit);
			}
		}
	
		return outputArray;
	}

	/**
	 * This method returns a summary of simulation.
	 * @return a CarbonAccountingToolExportSummary instance if the simulation has been carried out or null otherwise
	 */
	protected CATSingleSimulationResult getSimulationSummary() {
		if (isSimulationValid) {
			if (summary == null) {
				summary = new CATSingleSimulationResult("Sim " + ++nbSimulations, this);
			}
			return summary; 
		} else {
			return null;
		}
	}

	protected void storeResults() {
		getSimulationSummary().updateResult(this);
	}
		
	protected boolean isInfiniteSequenceAllowed() {return isInfiniteSequenceAllowed;}
	protected ManagementType getManagementType() {return managementType;}

	/**
	 * Set the proper realization in the CATTimeTable instance and
	 * prepare the simulation with the soil carbon module if it is
	 * enables.
	 * @param realizationId the index of the realization
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void setRealization(int realizationId) {
		getTimeTable().setMonteCarloRealization(realizationId);
		if (isMEMSEnabled()) {
			memsWrapper.prepareSimulation((List) getTimeTable().getStandsForThisRealization());
		}
	}

	@Override
	public int getMonteCarloRealizationId() {
		return getTimeTable().getCurrentMonteCarloRealizationId();
	}
	
	/*
	 * Useless for sensitivity analysis (non-Javadoc)
	 * @see repicea.simulation.MonteCarloSimulationCompliantObject#getSubjectId()
	 */
	@Override
	public String getSubjectId() {return null;}

	/*
	 * Useless for sensitivity analysis (non-Javadoc)
	 * @see repicea.simulation.MonteCarloSimulationCompliantObject#getHierarchicalLevel()
	 */
	@Override
	public HierarchicalLevel getHierarchicalLevel() {return null;}

	/**
	 * Provide the MEMSWrapper instance.
	 * @return a MEMSWrapper instance
	 */
	public MEMSWrapper getMEMS() { 
		return memsWrapper;
	}

}


