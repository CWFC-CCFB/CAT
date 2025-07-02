/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2012 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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
package lerfob.carbonbalancetool.productionlines;


import java.util.Collection;
import java.util.List;

import lerfob.carbonbalancetool.CATCompartmentManager;
import lerfob.carbonbalancetool.CATSettings;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.util.ObjectUtility;


/**
 * A EndUseWoodProductCarbonUnit object results from a TreeLogger job. Each EndProduct object is given its lca member 
 * and its UseClass member by a EndProductFeature object contained in the TreeLogCategory object
 * @author M.Fortin - May 2010
 */
public class EndUseWoodProductCarbonUnit extends CarbonUnit {

	/**
	 * Raw volume in m3 of wood material
	 */
	@Deprecated
	private double rawRoundWoodVolume;	// this member is required in order to calculate the emissions with respect to the input round wood volume
	
	/**
	 * The constructor of this class.
	 * @param initialVolumeBeforeFirstTransformation the volume before the processing (double) (m3)
	 * @param dateIndex the creation date index of the time scale
	 * @param carbonUnitFeature a EndProductFeature instance that defines the end product
	 * @param amountMap an AmountMap instance
	 * @param speciesName the species name 
	 * @param speciesType a SpeciesType enum
	 * @param statusClass a StatusClass enum
	 * @deprecated Use the EndUseWoodProductCarbonUnit(int, EndUseWoodProductCarbonUnitFeature, AmountMap, CarbonUnit) constructor instead.
	 */
	protected EndUseWoodProductCarbonUnit(double initialVolumeBeforeFirstTransformation,
			int dateIndex,
			EndUseWoodProductCarbonUnitFeature carbonUnitFeature,
			AmountMap<Element> amountMap,
			String speciesName,
			SpeciesType speciesType,
			StatusClass statusClass) {
		super(dateIndex, "", carbonUnitFeature, amountMap, speciesName, speciesType, statusClass, BiomassType.Wood, null); // woodyDebrisType set to null
		this.rawRoundWoodVolume = initialVolumeBeforeFirstTransformation;
	}
	
	
	/**
	 * Official constructor for this class.
	 * @param dateIndex the date index
	 * @param carbonUnitFeature an EndUseWoodProductCarbonUnitFeature instance
	 * @param amountMap an AmountMap instance
	 * @param originalCarbonUnit the original CarbonUnit instance from which this EndUseWoodProductCarbonUnit instance is created
	 */
	protected EndUseWoodProductCarbonUnit(int dateIndex,
			EndUseWoodProductCarbonUnitFeature carbonUnitFeature,
			AmountMap<Element> amountMap,
			CarbonUnit originalCarbonUnit) {
		super(dateIndex, carbonUnitFeature, amountMap, originalCarbonUnit);
		addStatus(CarbonUnitStatus.EndUseWoodProduct);
		AbstractProcessor.updateProcessEmissions(getAmountMap(), carbonUnitFeature.getBiomassOfFunctionalUnitMg(), carbonUnitFeature.getEmissionsMgCO2EqByFunctionalUnit());
	}

	/**
	 * This method returns the volume of the product as it was created.
	 * @return a double
	 */
	public double getProcessedVolumeAtCreationDate() {return getAmountMap().get(Element.Volume);}
	
	/**
	 * This method returns the dry biomass of the product as it was created.
	 * @return a double
	 */
	public double getBiomassMgAtCreationDate() {return getAmountMap().get(Element.Biomass);}
	
	/**
	 * This method returns the number of functional units in this carbon unit.
	 * @return a double
	 */
	public double getNumberOfFunctionalUnits() {
		if (getCarbonUnitFeature().getBiomassOfFunctionalUnitMg() != 0d) {
			return getBiomassMgAtCreationDate() / getCarbonUnitFeature().getBiomassOfFunctionalUnitMg();	
		} else {
			return 0d;
		}
	}
	
	/**
	 * This method actualizes the EndProduct instance on a basis that is specified through the time scale parameter. Landfill products are retrieved 
	 * through a static collection in the manager.
	 * @param compartmentManager a CATCompartmentManager instance
	 */
	@SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
	@Override
	protected void actualizeCarbon(CATCompartmentManager compartmentManager) {
		super.actualizeCarbon(compartmentManager);

		if (getCarbonUnitFeature().isDisposed()) {
			double[] releasedCarbonArray = getReleasedCarbonArray();
			double proportion;
			for (int i = getIndexInTimeScale(); i < getTimeTable().size(); i++) {
				proportion = releasedCarbonArray[i] / getInitialCarbon();
				AmountMap<Element> updatedMap = getAmountMap().multiplyByAScalar(proportion * getCarbonUnitFeature().getDisposableProportion());
				AbstractProductionLineProcessor disposedToProcessor = (AbstractProductionLineProcessor) ((ProductionLineProcessor) getCarbonUnitFeature().getProcessor()).getDisposedToProcess();
				if (updatedMap.get(Element.Volume) > 0) {
					if (disposedToProcessor != null) { // new implementation
						CarbonUnit newUnit = new CarbonUnit(i, null, updatedMap, this);
						newUnit.getAmountMap().put(Element.EmissionsCO2Eq, 0d);		// reset the emissions to 0 after useful lifetime - otherwise there is a double count
						List<ProcessUnit> disposedUnits = disposedToProcessor.createProcessUnitsFromThisProcessor(newUnit, 100);
						Collection<CarbonUnit> processedUnits = (Collection) disposedToProcessor.doProcess(disposedUnits);
						for (CarbonUnit carbonUnit : processedUnits) {
							if (carbonUnit.getLastStatus().equals(CarbonUnitStatus.EndUseWoodProduct)) {
								carbonUnit.addStatus(CarbonUnitStatus.Recycled);
							}
						}
						compartmentManager.getCarbonToolSettings().getCurrentProductionProcessorManager().getCarbonUnitMap().add(processedUnits);
					} else {	// former implementation
						((ProductionLineProcessor) getCarbonUnitFeature().getProcessor()).getProductionLine().getManager().sendToTheLandfill(i, getSpeciesName(), getSpeciesType(), getStatusClass(), updatedMap);	
					}
				}
			}
		}
	}
	
	
	private boolean isNewImplementation() {
		return rawRoundWoodVolume == 0d;
	}
	
	/**
	 * This method returns the total substitution effect of this product. NOTE: substitution is given in terms of tC eq. / raw volume
	 * @param manager the CarbonCompartmentManager instance
	 * @return the substitution in tC (double)
	 */
	public double getTotalCarbonSubstitution(CATCompartmentManager manager) {
		if (isNewImplementation()) {
			return getSubstitutionPerFunctionalUnit(getNumberOfFunctionalUnits(), manager);
		} else {	// former implementation
			return getSubstitutionPerFunctionalUnit(rawRoundWoodVolume, null);
		}
	}

	/**
	 * This method returns an array that contains the substitution for specific lost volumes. NOTE : returns
	 * null if the product is not actualized.
	 * @param manager the CarbonCompartmentManager instance
	 * @return an array of double
	 */
	public double[] getCurrentCarbonSubstitution(CATCompartmentManager manager) {
		if (isActualized()) {
			if (isNewImplementation()) {
				double[] outputArray = new double[getTimeTable().size()];
				outputArray[getIndexInTimeScale()] = getTotalCarbonSubstitution(manager);
				return outputArray;
			} else {
				double ratioToGetRawRoundWoodVolume = rawRoundWoodVolume / getProcessedVolumeAtCreationDate();
				double volumeFactor = getProcessedVolumeAtCreationDate() / getInitialCarbon();
				double[] releasedCarbonArray = getReleasedCarbonArray();
				double[] outputArray = new double[getTimeTable().size()];
				double volume;
				for (int i = 0; i < getTimeTable().size(); i++) {
					volume = releasedCarbonArray[i] * volumeFactor;
					volume *= ratioToGetRawRoundWoodVolume;
					outputArray[i] = getSubstitutionPerFunctionalUnit(volume, null);	// since the substitution is given as a ratio between tC eq : roundWoodVolume in m3.
				}
				return outputArray;
			}
		} else {
			return null;
		}
	}

	

	/**
	 * This method returns the use class of this EndProduct instance.
	 * @return the use class (UseClass)
	 */
	public UseClass getUseClass() {return getCarbonUnitFeature().getUseClass();}
	

	/**
	 * This method computes the carbon emissions related to the processing of 
	 * this end product.
	 * @return the carbon emissions in tC Eq. (double) (emissions denoted by negative values)
	 */
	@SuppressWarnings("deprecation")
	@Override
	public double getTotalNonRenewableCarbonEmissionsMgCO2Eq() {
		if (getAmountMap().containsKey(Element.EmissionsCO2Eq)) {		// new implementation
			return super.getTotalNonRenewableCarbonEmissionsMgCO2Eq();
		} else {			// former implementation
			double emission = 0d;
			if (getCarbonUnitFeature().getLCA() != null) {		// lca is supposed to be instantiated at creation date
				emission = - getCarbonUnitFeature().getLCA().getCarbonEmissionPerM3() * rawRoundWoodVolume;
			}
			return emission;
		}
	}
	

	@Override
	protected void addProcessUnit(ProcessUnit<Element> carbonUnit) {
		super.addProcessUnit(carbonUnit);
		this.rawRoundWoodVolume += ((EndUseWoodProductCarbonUnit) carbonUnit).rawRoundWoodVolume;
	}

	/**
	 * This method returns the substitution in C eq.
	 * @param manager a CATCompartmentManager instance
	 * @return the substitution in C eq. (double)
	 */
	@SuppressWarnings("deprecation")
	private double getSubstitutionPerFunctionalUnit(double nbFunctionalUnits, CATCompartmentManager manager) {
		if (isNewImplementation()) {
			return nbFunctionalUnits * getCarbonUnitFeature().getSubstitutionMgCO2EqByFunctionalUnit(manager) * CATSettings.CO2_C_FACTOR; // Conversion to C eq.
		} else {
			return nbFunctionalUnits * getCarbonUnitFeature().getAverageSubstitution();
		}
	}

	@Override
	protected EndUseWoodProductCarbonUnitFeature getCarbonUnitFeature() {
		return (EndUseWoodProductCarbonUnitFeature) super.getCarbonUnitFeature();
	}
	
	
	private double getCombustionEmissionsInCO2EqForAParticularAmountOfDryBiomass(double dryBiomassMg) {
		double emissionFactor = getCarbonUnitFeature().getCombustionEmissionFactorsInCO2Eq();
		if (emissionFactor > 0) {
			return - dryBiomassMg * emissionFactor;
		} else {
			return 0d;
		}
	}

	private double getHeatProductionForAParticularAmountOfDryBiomass(double dryBiomassMg) {
		double heatFactor = getCarbonUnitFeature().getHeatProductionMgWh();
		return heatFactor * dryBiomassMg;
	}


	/**
	 * This method returns the total emissions due to combusion in CO2 eq.
	 * @return a double
	 */
	public double getTotalCombustionEmissionsCO2Eq() {
		return getCombustionEmissionsInCO2EqForAParticularAmountOfDryBiomass(getAmountMap().get(Element.Biomass));
	}

	
	/**
	 * This method returns the emissions through out the lifetime of the end use wood product. 
	 * NOTE: if the array of released carbon is null this method returns null
	 * @return an array of double based on the time scale 
	 */
	public double[] getCombustionEmissionsArrayCO2Eq() {
		double[] releasedCarbonArray = getReleasedCarbonArray();
		if (releasedCarbonArray != null) {
			double carbonToBiomassFactor = getAmountMap().get(Element.Biomass) / getInitialCarbon(); 
			double[] releasedBiomass = ObjectUtility.multiplyArrayByScalar(releasedCarbonArray, carbonToBiomassFactor);
			double[] combustionEmissionsCO2Eq = new double[releasedBiomass.length];
			for (int i = 0; i < releasedBiomass.length; i++) {
				combustionEmissionsCO2Eq[i] = getCombustionEmissionsInCO2EqForAParticularAmountOfDryBiomass(releasedBiomass[i]);
			}
			return combustionEmissionsCO2Eq;
		} else {
			return null;
		}
	}

	/**
	 * This method returns the emissions through out the lifetime of the end use wood product. 
	 * NOTE: if the array of released carbon is null this method returns null
	 * @return an array of double based on the time scale 
	 */
	public double[] getHeatProductionArrayMgWh() {
		double[] releasedCarbonArray = getReleasedCarbonArray();
		if (releasedCarbonArray != null) {
			double carbonToBiomassFactor = getAmountMap().get(Element.Biomass) / getInitialCarbon(); 
			double[] releasedBiomass = ObjectUtility.multiplyArrayByScalar(releasedCarbonArray, carbonToBiomassFactor);
			double[] heatProductionArrayMgWh = new double[releasedBiomass.length];
			for (int i = 0; i < releasedBiomass.length; i++) {
				heatProductionArrayMgWh[i] = getHeatProductionForAParticularAmountOfDryBiomass(releasedBiomass[i]);
			}
			return heatProductionArrayMgWh;
		} else {
			return null;
		}
	}
	
	
	/**
	 * This method returns the total heat production from this carbon unit in KWh.
	 * @return a double
	 */
	public double getTotalHeatProductionMgWh() {
		return getHeatProductionForAParticularAmountOfDryBiomass(getAmountMap().get(Element.Biomass));
	}
}