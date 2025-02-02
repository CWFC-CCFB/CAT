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

import lerfob.carbonbalancetool.CATSettings;
import repicea.simulation.processsystem.AmountMap;


public class LandfillCarbonUnit extends CarbonUnit {
	
	
	/**
	 * Constructor.
	 * @param dateIndex the date index
	 * @param sampleUnitID the sample unit identifier if any
	 * @param landfillCarbonUnitFeature a LandfillCarbonUnitFeature instance
	 * @param amountMap an AmountMap instance
	 * @param speciesName the name of the species
	 * @param speciesType the type of species (Broadleaved or Coniferous)
	 * @param statusClass a StatusClass enum
	 * @param biomassType the type of biomass (wood or bark)
	 * @param landfillStatus a CarbonUnitStatus enum (either LandFillDegradable or LandFillNonDegradable)
	 * @deprecated Use the LandfillCarbonUnit(int, LandfillCarbonUnitFeature, AmountMap, CarbonUnit, CarbonUnitStatus) constructor instead
	 */
	protected LandfillCarbonUnit(int dateIndex, 
							String sampleUnitID,
							LandfillCarbonUnitFeature landfillCarbonUnitFeature,
							AmountMap<Element> amountMap,
							String speciesName,
							SpeciesType speciesType,
							StatusClass statusClass,
							BiomassType biomassType,
							CarbonUnitStatus landfillStatus) {
		super(dateIndex, sampleUnitID, landfillCarbonUnitFeature, amountMap, speciesName, speciesType, statusClass, biomassType, null); // woodyDebrisType set to null
		addStatus(landfillStatus);
	}
	
	/**
	 * Official constructor
	 * @param dateIndex the date index
	 * @param landfillCarbonUnitFeature a LandfillCarbonUnitFeature instance
	 * @param amountMap an AmountMap instance
	 * @param originalCarbonUnit the original CarbonUnit instance from which this LandfillCarbonUnit instance is created
	 * @param landfillStatus a CarbonUnitStatus enum (either LandFillDegradable or LandFillNonDegradable)
	 */
	protected LandfillCarbonUnit(int dateIndex,
			LandfillCarbonUnitFeature landfillCarbonUnitFeature,
			AmountMap<Element> amountMap,
			CarbonUnit originalCarbonUnit,
			CarbonUnitStatus landfillStatus) {
		super(dateIndex, landfillCarbonUnitFeature, amountMap, originalCarbonUnit);
		addStatus(landfillStatus);
	}
	
	@Override
	protected LandfillCarbonUnitFeature getCarbonUnitFeature() {
		return (LandfillCarbonUnitFeature) super.getCarbonUnitFeature();
	}

	/**
	 * This method returns the CH4 carbon equivalent emissions through out the lifetime of the landfill product. 
	 * NOTE: if the array of released carbon is null this method returns null
	 * @return an array of double based on the time scale (in tC eq.)
	 */
	public double[] getMethaneEmissionsArrayCO2Eq() {
		double[] releasedCarbonArray = getReleasedCarbonArray();
		if (releasedCarbonArray != null) {
			double[] co2EqMethaneEmissionsArray = new double[releasedCarbonArray.length];
			for (int i = 0; i < releasedCarbonArray.length; i++) {
				co2EqMethaneEmissionsArray[i] = getCH4EmissionsInCO2EqForAParticularAmountOfCarbon(releasedCarbonArray[i]);
			}
			return co2EqMethaneEmissionsArray;
		} else {
			return null;
		}
	}
	
	/**
	 * This method returns the total CH4 carbon equivalent emissions through out the lifetime of the landfill product.
	 * @return a double (in tC eq.)
	 */
	public double getTotalMethaneEmissionsCO2Eq() {
		return getCH4EmissionsInCO2EqForAParticularAmountOfCarbon(getInitialCarbon());
	}
	
	private double getCH4EmissionsInCO2EqForAParticularAmountOfCarbon(double carbon) {
		final double CH4_part = .5;
//		final double CH4_C_conversion = 16d / 12;		// see Eq.3.6 IPCC guidelines 2006 v.5 Waste p. 3.10 
		double methaneCorrectionFactor = getCarbonUnitFeature().getLandfillType().getMethaneCorrectionFactor();
		return - carbon * methaneCorrectionFactor * CH4_part * CATSettings.C_CH4_FACTOR * (CATSettings.getGlobalWarmingPotential().getCH4Factor() - 1);
	}
	
}
