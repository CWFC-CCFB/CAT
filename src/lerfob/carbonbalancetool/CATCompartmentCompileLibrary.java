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

import java.util.Collection;
import java.util.List;

import lerfob.carbonbalancetool.interfaces.CATSaplingsProvider;
import lerfob.carbonbalancetool.productionlines.CarbonUnit;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnit;
import lerfob.carbonbalancetool.productionlines.LandfillCarbonUnit;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

/**
 * This class contains functions to compute either the integral of the carbon compartment
 * or the evolution of carbon within this compartment.
 * @author Mathieu Fortin - July 2010
 */
class CATCompartmentCompileLibrary {
		
	/**
	 * This method implements the functions for calculating and integrating the carbon over
	 * the forecast period.
	 * @param carbonCompartment = a carbon compartment (CarbonCompartment object)
	 */
	@SuppressWarnings("unchecked")
	void selectCalculatorFunction(CATCompartment carbonCompartment) {
		CATCompartmentManager manager = carbonCompartment.getCompartmentManager();
		
		Collection<? extends CarbonUnit> carbonUnits;
		CATTimeTable timeTable = manager.getTimeTable();
		double[] carbon = new double[timeTable.size()];
		double integratedCarbon = 0d;
		int revolutionPeriod = manager.getRotationLength();
//		CATExponentialFunction decayFunction;
		List<CATCompatibleStand> stands = timeTable.getStandsForThisRealization();
		double areaHa = stands.get(0).getAreaHa();
		CATIntermediateBiomassCarbonMap oMap;
		
		switch (carbonCompartment.getCompartmentID()) {
		
		case Roots:
			oMap = new CATIntermediateBiomassCarbonMap(timeTable, carbonCompartment);
			for (CATCompatibleStand stand : stands) {
				double carbonContent = manager.getCarbonToolSettings().getCurrentBiomassParameters().getBelowGroundCarbonMg(stand.getTrees(StatusClass.alive), manager);
				if (stand instanceof CATSaplingsProvider) {
					carbonContent += manager.getCarbonToolSettings().getCurrentBiomassParameters().getBelowGroundCarbonMg(((CATSaplingsProvider) stand).getSaplings(), manager);
				}
				oMap.put(stand, carbonContent);
			}
			oMap.interpolateIfNeeded();
			carbonCompartment.setIntegratedCarbon(integrateCarbonOverHorizon(carbonCompartment) / revolutionPeriod);
			
			break;
			
		case AbGround:
			oMap = new CATIntermediateBiomassCarbonMap(timeTable, carbonCompartment);
			for (CATCompatibleStand stand : stands) {
				double carbonContent = manager.getCarbonToolSettings().getCurrentBiomassParameters().getAboveGroundCarbonMg(stand.getTrees(StatusClass.alive), manager);
				if (stand instanceof CATSaplingsProvider) {
					carbonContent += manager.getCarbonToolSettings().getCurrentBiomassParameters().getAboveGroundCarbonMg(((CATSaplingsProvider) stand).getSaplings(), manager);
				}
				oMap.put(stand, carbonContent);
			}	
			oMap.interpolateIfNeeded();
			carbonCompartment.setIntegratedCarbon(integrateCarbonOverHorizon(carbonCompartment) / revolutionPeriod);
			break;
			
		// STOCK COMPARTMENTS
		case DeadBiom:
		case Products:
		case LfillDeg:
//			decayFunction = manager.getCarbonToolSettings().getDecayFunction();
			
			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						double[] actualizedCarbon = carbonUnit.getCurrentCarbonArray();	
						for (int j = 0; j < timeTable.size(); j++) {
							carbon[j] += actualizedCarbon[j];
						}
						integratedCarbon += carbonUnit.getIntegratedCarbon(manager);
					}
				}
			}
			
			for (int i = 0; i < timeTable.size(); i++) {
				carbonCompartment.setCarbonIntoArray(i, carbon[i]);
			}
			
			if (isInfiniteSequenceAllowed(carbonCompartment)) {
				carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);
			} else {
				carbonCompartment.setIntegratedCarbon(integrateCarbonOverHorizon(carbonCompartment) / revolutionPeriod);
			}
		
			break;

			
		case CarbEmis:
			double scalingCO2toCFactor = CATSettings.CO2_C_FACTOR;
			if (manager.getCarbonToolSettings().formerImplementation) {
				scalingCO2toCFactor = 1d;
			};

			
			// evolution
			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						carbon[i] += carbonUnit.getTotalNonRenewableCarbonEmissionsMgCO2Eq() * scalingCO2toCFactor;
					}
				}
				integratedCarbon += carbon[i];
				
			}

			for (int i = 0; i < timeTable.size(); i++) {
				if (i > 0) {
					carbon[i] += carbon[i - 1];
				}
			}

			for (int i = 0; i < timeTable.size(); i++) {
				carbonCompartment.setCarbonIntoArray(i, carbon[i]);
			}
			
			carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);
			
			break;

		case EnerSubs:
			
//			decayFunction = manager.getCarbonToolSettings().getDecayFunction();

			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						EndUseWoodProductCarbonUnit endProduct = (EndUseWoodProductCarbonUnit) carbonUnit;
						
						double[] substitutedCarbon = endProduct.getCurrentCarbonSubstitution(manager);	
						for (int j = 0; j < timeTable.size(); j++) {
							carbon[j] += substitutedCarbon[j];
						}
						integratedCarbon += endProduct.getTotalCarbonSubstitution(manager);
					}
				}
			}
			
			for (int i = 0; i < timeTable.size(); i++) {
				if (i > 0) {
					carbon[i] += carbon[i - 1];
				}
			}

			for (int i = 0; i < timeTable.size(); i++) {
				carbonCompartment.setCarbonIntoArray(i, carbon[i]);
			}
			
			carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);

			break;
			
		case WComb:
			double[] heatProduction = new double[timeTable.size()];
			double totalHeatProduction = 0;
			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						EndUseWoodProductCarbonUnit endUseCarbonProduct = (EndUseWoodProductCarbonUnit) carbonUnit;

						double[] currentEmission = endUseCarbonProduct.getCombustionEmissionsArrayCO2Eq();	
						double[] currentHeatProduction = endUseCarbonProduct.getHeatProductionArrayMgWh();
						for (int j = 0; j < timeTable.size(); j++) {
							carbon[j] += currentEmission[j] * CATSettings.CO2_C_FACTOR;
							heatProduction[j] += currentHeatProduction[j];
						}
						
						integratedCarbon += endUseCarbonProduct.getTotalCombustionEmissionsCO2Eq() * CATSettings.CO2_C_FACTOR;
						totalHeatProduction += endUseCarbonProduct.getTotalHeatProductionMgWh();
					}
				}
			}
			
			for (int i = 0; i < timeTable.size(); i++) {
				if (i > 0) {
					carbon[i] += carbon[i - 1];
					heatProduction[i] += heatProduction[i - 1];
				}
			}

			for (int i = 0; i < timeTable.size(); i++) {
				carbonCompartment.setCarbonIntoArray(i, carbon[i]);
			}
			
			carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);
			
			((CATProductCompartment) carbonCompartment).setHeatProductionArray(heatProduction);
			((CATProductCompartment) carbonCompartment).setTotalHeatProduction(totalHeatProduction);
			
			break;
			

		case LfillND:
			
			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						LandfillCarbonUnit landfillCarbonProduct = (LandfillCarbonUnit) carbonUnit;
						integratedCarbon += landfillCarbonProduct.getInitialCarbon();
					}
				}

				carbonCompartment.setCarbonIntoArray(i, integratedCarbon);
			}
			carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);
			break;
			
		case LfillEm:
//			decayFunction = manager.getCarbonToolSettings().getDecayFunction();
			
			for (int i = 0; i < timeTable.size(); i++) {
				carbonUnits = carbonCompartment.getCarbonUnitsArray()[i];
				if (carbonUnits != null && !carbonUnits.isEmpty()) {
					for (CarbonUnit carbonUnit : carbonUnits) {
						LandfillCarbonUnit landfillCarbonProduct = (LandfillCarbonUnit) carbonUnit;

						double[] currentEmission = landfillCarbonProduct.getMethaneEmissionsArrayCO2Eq();	
						for (int j = 0; j < timeTable.size(); j++) {
							carbon[j] += currentEmission[j] * CATSettings.CO2_C_FACTOR;
						}
						integratedCarbon += landfillCarbonProduct.getTotalMethaneEmissionsCO2Eq() * CATSettings.CO2_C_FACTOR;
					}
				}
			}
			
			for (int i = 0; i < timeTable.size(); i++) {
				if (i > 0) {
					carbon[i] += carbon[i - 1];
				}
			}

			for (int i = 0; i < timeTable.size(); i++) {
				carbonCompartment.setCarbonIntoArray(i, carbon[i]);
			}
			
			carbonCompartment.setIntegratedCarbon(integratedCarbon / revolutionPeriod);
			break;
			
		case LivingBiomass:
		case TotalProducts:
		case NetSubs:
			carbonCompartment.mergeWithFatherCompartments();
			break;
		case Humus:
			if (manager.isMEMSEnabled()) {
				for (int i = 0; i < timeTable.size(); i++) {
					double humusMgHa = manager.getMEMS().getCarbonStockMgHaForThisYear(i).getHumusCarbonMgHa();
					carbonCompartment.setCarbonIntoArray(i, humusMgHa * areaHa);
				}
				carbonCompartment.setIntegratedCarbon(integrateCarbonOverHorizon(carbonCompartment) / revolutionPeriod);  // no need to correct for the area because the integration relies on the carbon array which contains area corrected values
			}
			break;
		case MineralSoil:
			if (manager.isMEMSEnabled()) {
				for (int i = 0; i < timeTable.size(); i++) {
					double soilMgHa = manager.getMEMS().getCarbonStockMgHaForThisYear(i).getMineralSoilCarbonMgHa();
					carbonCompartment.setCarbonIntoArray(i, soilMgHa* areaHa);
				}
				carbonCompartment.setIntegratedCarbon(integrateCarbonOverHorizon(carbonCompartment) / revolutionPeriod);  // no need to correct for the area because the integration relies on the carbon array which contains area corrected values
			}
			break;
		case Soil:
			carbonCompartment.mergeWithFatherCompartments();
			break;
		}
	}
	
	private boolean isInfiniteSequenceAllowed(CATCompartment carbonCompartment) {
		return carbonCompartment.getCompartmentManager().isInfiniteSequenceAllowed();
	}
	
	/**
	 * This method computes a numerical approximation of the integral based on the trapezoidal rule.
	 * @param carbonCompartment a carbon compartment (CarbonCompartment object)
	 * @return the total carbon (double)
	 */
	private double integrateCarbonOverHorizon(CATCompartment carbonCompartment) {
		CATTimeTable timeScale = carbonCompartment.getCompartmentManager().getTimeTable();
		boolean isInfiniteSequenceAllowed = isInfiniteSequenceAllowed(carbonCompartment);
		double previousValue, currentValue;
		int currentDateYr, previousDateYr;
		double totalCarbon = 0d;
		
		for (int i = 1; i < timeScale.size(); i++) {		// time scale is now 
			
			if (i == 1 && 
					isInfiniteSequenceAllowed && 
					carbonCompartment.getCompartmentManager().getManagementType() == ManagementType.EvenAged) {	// then add the first years from 0 to the initial measurement of the stand
				int initialAgeYr = timeScale.getInitialAgeYr();
				currentValue = carbonCompartment.getCarbonArray()[i - 1];
				totalCarbon += calculateCarbonForThisPeriod(0, initialAgeYr, 0, currentValue);
			}
			
			currentDateYr = timeScale.getDateYrAtThisIndex(i);
			currentValue = carbonCompartment.getCarbonArray()[i];

			previousDateYr = timeScale.getDateYrAtThisIndex(i - 1);
			previousValue = carbonCompartment.getCarbonArray()[i - 1];
		
			totalCarbon += calculateCarbonForThisPeriod(previousDateYr, 
					currentDateYr, 
					previousValue, 
					currentValue);
		}
		
		return totalCarbon;
	}
	
	private double calculateCarbonForThisPeriod(int date0, int date1, double value0, double value1) {
		if (date1 - date0 > 0) {
			return (value1 + value0) * .5 * (date1 - date0);
		} else {
			return 0d;
		}
	}
	
}
