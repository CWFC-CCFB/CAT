/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2021 Her Majesty the Queen in right of Canada
 * Author: Mathieu Fortin for Canadian Wood Fibre Centre  
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed with the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * Please see the license at http://www.gnu.org/copyleft/lesser.html.
 */
package lerfob.carbonbalancetool.pythonaccess;

import repicea.simulation.covariateproviders.treelevel.DbhCmProvider;
import repicea.simulation.species.REpiceaSpecies.Species;

class PythonOakTree extends PythonCarbonToolCompatibleTree implements DbhCmProvider {

	
	PythonOakTree(StatusClass statusClass, 
			double number, 
			double biomassRoots,
			double biomassTrunk, 
			double biomassBranches,
			double dbhCm,
			double dbhCmStandardDeviation) {
		super(Species.Quercus_spp, 
				statusClass, 
				number, 
				biomassRoots, 
				biomassTrunk, 
				biomassBranches, 
				dbhCm, 
				dbhCmStandardDeviation);
	}

	@Override
	public double getDbhCm() {return dbhCm;}
	
}
