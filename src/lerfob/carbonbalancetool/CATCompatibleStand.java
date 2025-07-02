/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin for AgroParisTech/INRA UMR LERFoB,
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

import repicea.simulation.covariateproviders.plotlevel.AgeYrProvider;
import repicea.simulation.covariateproviders.plotlevel.AreaHaProvider;
import repicea.simulation.covariateproviders.plotlevel.DateYrProvider;
import repicea.simulation.covariateproviders.plotlevel.InterventionResultProvider;
import repicea.simulation.covariateproviders.plotlevel.TreeStatusCollectionsProvider;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider;

/**
 * An interface to ensure the STAND instance is compatible with CAT. <br>
 * <br>
 * It extends several interfaces. The DateYrProvider interface is used to 
 * retrieve the current date at which the stand was measured. The AgeYrProvider 
 * interface is used if the simulation can be run in infinite sequence, i.e. if
 * <ul> 
 * <li>the getManagementType() method returns ManagementType.EvenAged </li>
 * <li>the getApplicationScale() method returns ApplicationScale.Stand </li>
 * <li>the growth simulation has a single simulation (typically a deterministic simulation) </li>
 * </ul>
 * @author Mathieu Fortin - August 2013
 */
public interface CATCompatibleStand extends AreaHaProvider, 
											TreeStatusCollectionsProvider, 
											InterventionResultProvider,
											ManagementTypeProvider,
											ApplicationScaleProvider,
											DateYrProvider,
											AgeYrProvider {

	
	/**
	 * This method returns a CarbonToolCompatibleStand with all its trees
	 * set to cut. It is called only if canBeRunInInfiniteSequence returns true.
	 * Otherwise the method can return null.
	 * @return a CarbonToolCompatibleStand stand
	 */
	public CATCompatibleStand getHarvestedStand();

	
	/**
	 * This method returns the identification of the stand.
	 * @return a String
	 */
	public String getStandIdentification();
	
}
