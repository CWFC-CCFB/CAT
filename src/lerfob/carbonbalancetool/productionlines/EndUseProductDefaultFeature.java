/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2020 Her Majesty the Queen in right of Canada,
 * Author: Mathieu Fortin, Canadian Forest Service, 
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
package lerfob.carbonbalancetool.productionlines;

import java.util.HashMap;
import java.util.Map;

import lerfob.carbonbalancetool.productionlines.DecayFunction.DecayFunctionType;
import lerfob.carbonbalancetool.productionlines.DecayFunction.LifetimeMode;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;

/**
 * A class that implements the IPCC Tier 1 and Tier 2 features for 
 * the end-use products.<p>
 * 
 * An instance of this class contains the following information:<ul>
 * <li> the use class;
 * <li> the type of decay function;
 * <li> the mode (half-life versus average lifetime);
 * <li> the average lifetime
 * </ul>
 * 
 * The tier-1 instances follows the are set following Table 12.3 in the 2019 
 * Refinement to the 2006 IPCC Guidelines. The tier-2 instances are inspired by
 * Fortin et al. (2012).
 * 
 * @see <a href=https://www.ipcc-nggip.iges.or.jp/public/2019rf/pdf/4_Volume4/19R_V4_Ch12_HarvestedWoodProducts.pdf>  
 * Ruter, S., R.W. Matthews, M. Lundblad, A. Sato, and R.A. Hassan. 2019. Harvested Wood Products. 
 * Chapter 12 in 2019 Refinement to the 2006 IPCC Guidelines for National Greenhouse Gas Inventories.
 * </a> 
 * @see <a href=https://doi.org/10.1016/j.foreco.2012.05.031>  
 * Fortin, M., F. Ningre, N. Robert, and M. Frederic. 2012. Quantifying the impact of forest management on the 
 * carbon balance of the forest-wood product chain: A case study applied to even-aged oak stands in France.
 * Forest Ecology and Management 279: 176-188.
 * </a> 
 */
public class EndUseProductDefaultFeature {

	private static Map<UseClass, EndUseProductDefaultFeature> DefaultFeatureMap;
	
	final UseClass useClass;
	final DecayFunctionType decayFunctionType;
	final LifetimeMode lifetimeMode;
	final double lifetimeYr;

	EndUseProductDefaultFeature(UseClass useClass, DecayFunctionType decayFunctionType, LifetimeMode lifetimeMode, double lifetimeYr) {
		this.useClass = useClass;
		this.decayFunctionType = decayFunctionType;
		this.lifetimeMode = lifetimeMode;
		this.lifetimeYr = lifetimeYr;
	}

	/**
	 * Provide a Map of tier-1 and tier-2 default features.<p>
	 * These default features can be used to set the CarbonUnitFeature member
	 * of ProductionLineProcessor instances. The resulting map is 
	 * a copy of a static instance. 
	 * 
	 * @return a Map of UseClass and EndUseProductDefaultFeature instances
	 * @see ProductionLineProcessor#updateFeature(EndUseProductDefaultFeature)
	 */
	public static Map<UseClass, EndUseProductDefaultFeature> getDefaultFeatureMap() {
		if (DefaultFeatureMap == null) {
			DefaultFeatureMap = new HashMap<UseClass, EndUseProductDefaultFeature>();
			// these three come from the 2019 IPCC Refinement 
			DefaultFeatureMap.put(UseClass.BUILDING, new EndUseProductDefaultFeature(UseClass.BUILDING, 
					DecayFunctionType.Exponential, 
					LifetimeMode.HALFLIFE, 35d));
			DefaultFeatureMap.put(UseClass.FURNITURE, new EndUseProductDefaultFeature(UseClass.FURNITURE, 
					DecayFunctionType.Exponential, 
					LifetimeMode.HALFLIFE, 25d));
			DefaultFeatureMap.put(UseClass.PAPER, new EndUseProductDefaultFeature(UseClass.PAPER, 
					DecayFunctionType.Exponential, 
					LifetimeMode.HALFLIFE, 2d));

			// these four were used in Fortin et al. (2012) 
			DefaultFeatureMap.put(UseClass.WRAPPING, new EndUseProductDefaultFeature(UseClass.WRAPPING, 
					DecayFunctionType.Exponential, 
					LifetimeMode.AVERAGE, 6.3));
			DefaultFeatureMap.put(UseClass.ENERGY, new EndUseProductDefaultFeature(UseClass.ENERGY, 
					DecayFunctionType.Exponential, 
					LifetimeMode.AVERAGE, 2.8));
			DefaultFeatureMap.put(UseClass.FIREWOOD, new EndUseProductDefaultFeature(UseClass.FIREWOOD, 
					DecayFunctionType.Exponential, 
					LifetimeMode.AVERAGE, 2.8));
			DefaultFeatureMap.put(UseClass.BARREL, new EndUseProductDefaultFeature(UseClass.BARREL, 
					DecayFunctionType.Exponential, 
					LifetimeMode.AVERAGE, 4d));

		}
		Map<UseClass, EndUseProductDefaultFeature> outputMap = new HashMap<UseClass, EndUseProductDefaultFeature>();
		outputMap.putAll(DefaultFeatureMap);
		return outputMap;
	}
	
}
