/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.pythonaccess;

import lerfob.treelogger.maritimepine.MaritimePineBasicLoggableTree;
import repicea.simulation.species.REpiceaSpecies.Species;

class PythonMaritimePineTree extends PythonCarbonToolCompatibleTree implements MaritimePineBasicLoggableTree {

	
	PythonMaritimePineTree(StatusClass statusClass, 
			double number, 
			double biomassRoots,
			double biomassTrunk, 
			double biomassBranches,
			double dbhCm,
			double dbhCmStandardDeviation) {
		super(Species.Pinus_pinaster, statusClass, number, biomassRoots, biomassTrunk, biomassBranches, dbhCm, dbhCmStandardDeviation);
	}

	@Override
	public double getDbhCm() {return dbhCm;}

	@Override
	public double getDbhCmStandardDeviation() {return dbhCmStandardDeviation;}

	@Override
	public double getHarvestedStumpVolumeM3() {return rootsVolume;}

	@Override
	public double getHarvestedCrownVolumeM3() {return branchesVolume;}

	@SuppressWarnings("deprecation")
	@Override
	public double getBasicWoodDensity() {
		return AverageBasicDensity.MaritimePine.getBasicDensity();			// to ensure that the JUnit test will be validated
	}

	
}
