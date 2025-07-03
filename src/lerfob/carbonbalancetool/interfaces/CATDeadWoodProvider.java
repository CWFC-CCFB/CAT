/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2025 His Majesty the King in right of Canada
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
package lerfob.carbonbalancetool.interfaces;

import java.util.Map;

/**
 * An interface that ensures a stand instance can provide 
 * its dead wood at the beginning of the simulation.
 * @author Mathieu Fortin - July 2025
 */
public interface CATDeadWoodProvider {

	/**
	 * Provide the deadwood biomass (Mg).
	 * @return a Map whose keys are the sample unit ID and the values are the dead biomasses (Mg)
	 */
	public Map<String, Double> getDeadWoodBiomassMgForThisSampleUnit();	
	
}
