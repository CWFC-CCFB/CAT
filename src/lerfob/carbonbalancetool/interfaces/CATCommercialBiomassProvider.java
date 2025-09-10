/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2025 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service 
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
package lerfob.carbonbalancetool.interfaces;

/**
 * This interface ensures the instance can provide the biomass of its commercial part.
 * @author Mathieu Fortin - September 2025
 */
public interface CATCommercialBiomassProvider {

	/**
	 * Check if the interface is implemented on the fly or not.
	 * @param o the object to be tested
	 * @return true if the interface is implemented and enabled
	 */
	public static boolean checkEligibility(Object o) {
		if (o instanceof CATCommercialBiomassProvider) {
			if (((CATCommercialBiomassProvider) o).isCATCommercialBiomassProviderInterfaceEnabled()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * This method returns the dry commercial biomass (Mg), INCLUDING bark and WITHOUT expansion factor.
	 * @return a double
	 */
	public double getCommercialBiomassMg();
	
	/**
	 * If the predictor benefits from a stochastic implementation, then the sensitivity analysis is enabled.
	 * @return a boolean
	 */
	public default boolean isCommercialBiomassPredictorStochastic() {return false;}

	/**
	 * Provide the current state of this interface for on-the-fly implementation.
	 * @return a boolean
	 */
	public default boolean isCATCommercialBiomassProviderInterfaceEnabled() {
		return true;
	}

}
