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

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATBasicWoodDensityProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundVolumeProvider;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;

/**
 * This internal class is actually a wrapper for the trees that are sent to the PythonAccessPoint class.
 * @author Mathieu Fortin - May 2014
 */
abstract class PythonCarbonToolCompatibleTree implements 	CATCompatibleTree, 
															TreeStatusProvider,
															CATBasicWoodDensityProvider,
															CATAboveGroundVolumeProvider, 
															CATBelowGroundVolumeProvider {

	final Species species;
	StatusClass statusClass; 
	final double number; 
	final double rootsVolume;
	final double branchesVolume;
	final double trunkVolume;
	final double dbhCm;
	final double dbhCmStandardDeviation;

	PythonCarbonToolCompatibleTree(Species species,
			StatusClass statusClass, 
			double number, 
			double biomassRoots,
			double biomassTrunk,
			double biomassBranches,
			double dbhCm,
			double dbhCmStandardDeviation) {
		this.species = species;
		setStatusClass(statusClass);
		this.number = number;
		this.branchesVolume = biomassBranches / getBasicWoodDensity();
		this.rootsVolume = biomassRoots / getBasicWoodDensity();
		this.trunkVolume = biomassTrunk / getBasicWoodDensity();
		this.dbhCm = dbhCm;
		this.dbhCmStandardDeviation = dbhCmStandardDeviation;
	}
	
	
	@Override
	public double getNumber() {return number;}

	@Override
	public double getCommercialVolumeM3() {return trunkVolume;}

	@Override
	public String getSpeciesName() {return species.name();}

	@Override
	public void setStatusClass(StatusClass statusClass) {this.statusClass = statusClass;}

	@Override
	public StatusClass getStatusClass() {return statusClass;}

	@Override
	public double getBasicWoodDensity() {return getCATSpecies().getBasicWoodDensity(getSpeciesLocale());}

	@Override
	public Species getCATSpecies() {return species;}
	
	@Override
	public double getAboveGroundVolumeM3() {return branchesVolume + trunkVolume;}

	@Override
	public double getBelowGroundVolumeM3() {return rootsVolume;}

	@Override
	public boolean isCommercialVolumeOverbark() {return true;}

	@Override
	public SpeciesLocale getSpeciesLocale() {return SpeciesLocale.IPCC;}
}
