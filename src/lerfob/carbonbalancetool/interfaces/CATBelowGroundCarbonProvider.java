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
package lerfob.carbonbalancetool.interfaces;

/**
 * This interface ensures the instance can provide its dry below ground carbon. 
 * @author Mathieu Fortin - August 2013
 */
public interface CATBelowGroundCarbonProvider {

	
	/**
	 * Check if the interface is implemented on the fly or not.
	 * @param o the object to be tested
	 * @return true if the interface is implemented and enabled
	 */
	public static boolean checkEligibility(Object o) {
		if (o instanceof CATBelowGroundCarbonProvider) {
			if (((CATBelowGroundCarbonProvider) o).isCATBelowGroundCarbonProviderInterfaceEnabled()) {
				return true;
			}
		}
		return false;
	}

	
	/**
	 * This method returns the belowground carbon (Mg), INCLUDING bark and WITHOUT expansion factor.
	 * @return a double
	 */
	public double getBelowGroundCarbonMg();

	/**
	 * If the predictor benefits from a stochastic implementation, then the sensitivity analysis is enabled.
	 * @return a boolean
	 */
	public default boolean isBelowGroundCarbonPredictorStochastic() {return false;}

	/**
	 * Provide the current state of this interface for on-the-fly implementation.
	 * @return a boolean
	 */
	public default boolean isCATBelowGroundCarbonProviderInterfaceEnabled() {
		return true;
	}

}
