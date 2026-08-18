/*
 * This file is part of the lerfob-foresttools library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.io;

import lerfob.carbonbalancetool.CATCompatibleTree;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;

/**
 * This class represents the trees in a yield table import in CAT. It is actually a fake class that ensures the 
 * compatibility with CAT. It is set to a single tree having the volume per ha of the stand.
 * @author Mathieu Fortin - June 2017
 */
class CATYieldTableCompatibleTree implements CATCompatibleTree, TreeStatusProvider {

	private final double volumeM3;
	private StatusClass statusClass;
	CATYieldTableCompatibleStand stand;
	final boolean overBark;
	
	
	CATYieldTableCompatibleTree(double underbarkVolumeM3, StatusClass statusClass, boolean overBark) {
		this.volumeM3 = underbarkVolumeM3;
		setStatusClass(statusClass);
		this.overBark = overBark;
	}
		
	@Override
	public double getCommercialVolumeM3() {return volumeM3;}

	@Override
	public String getSpeciesName() {return stand.species.toString();}

	@Override
	public void setStatusClass(StatusClass statusClass) {
		this.statusClass = statusClass;
	}

	@Override
	public StatusClass getStatusClass() {return statusClass;}

	public CATYieldTableCompatibleTree getClone() {
		return new CATYieldTableCompatibleTree(volumeM3, statusClass, overBark);
	}

	@Override
	public boolean isCommercialVolumeOverbark() {return overBark;}

	@Override
	public Species getREpiceaSpecies() {return stand.species;}
	
	@Override
	public SpeciesLocale getSpeciesLocale() {return this.stand.getSpeciesLocale();}

}
