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

import java.security.InvalidParameterException;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.CATSettings.CATSpecies;

/**
 * A specific implementation of CATCompatibleTree for saplings.<p>
 * This class is the return type of the CATSaplingsProvider interface.
 * @author Mathieu Fortin - July 2025
 */
public final class CATSapling implements CATAboveGroundBiomassProvider,
											CATCompatibleTree {

	final double aboveGroundBiomassMg;
	final CATSpecies species;
	final double expansionFactor;
	final double plotWeight;

	/**
	 * General constructor.
	 * @param aboveGroundBiomassMg the aboveground biomass of one sapling (Mg)
	 * @param species a CATSpecies enum
	 * @param expansionFactor the number of saplings represented by this instance.
	 * @param plotWeight the sampling weight of the plot
	 */
	public CATSapling(double aboveGroundBiomassMg,
			CATSpecies species,
			double expansionFactor,
			double plotWeight) {
		this.aboveGroundBiomassMg = aboveGroundBiomassMg;
		this.species = species;
		if (expansionFactor <= 0) {
			throw new InvalidParameterException("The expansionFactor argument must be positive!");
		}
		this.expansionFactor = expansionFactor;
		if (plotWeight <= 0) {
			throw new InvalidParameterException("The plotWeight argument must be positive!");
		}
		this.plotWeight = plotWeight;
	}

	/**
	 * Constructor for default plot weight.<p>
	 * For plots with a weight of 1. The expansion factor represents the number
	 * of sapling with these characteristics in the plot.
	 * @param aboveGroundBiomassMg the aboveground biomass of one sapling (Mg)
	 * @param species a CATSpecies enum
	 * @param expansionFactor the number of saplings represented by this instance.
	 */
	public CATSapling(double aboveGroundBiomassMg,
			CATSpecies species,
			double expansionFactor) {
		this(aboveGroundBiomassMg, species, expansionFactor, 1d);
	}

	/**
	 * Constructor for tree instance  
	 * @param aboveGroundBiomassMg the aboveground biomass of one sapling (Mg)
	 * @param species a CATSpecies enum
	 */
	public CATSapling(double aboveGroundBiomassMg,
			CATSpecies species) {
		this(aboveGroundBiomassMg, species, 1d, 1d);
	}

	
	@Override
	public double getAboveGroundBiomassMg() {return aboveGroundBiomassMg;}

	@Override
	public double getCommercialVolumeM3() {
		return 0d; // since they are not commercial
	}

	@Override
	public boolean isCommercialVolumeOverbark() {return false;}

	@Override
	public String getSpeciesName() {return getCATSpecies().toString();}

	@Override
	public void setStatusClass(StatusClass statusClass) {
		throw new UnsupportedOperationException("The status class of CATSapling instance cannot be changed!");
	}

	@Override
	public StatusClass getStatusClass() {return StatusClass.alive;}

	@Override
	public CATSpecies getCATSpecies() {return species;}

	@Override
	public double getPlotWeight() {return plotWeight;}
	
	@Override
	public double getNumber() {return expansionFactor;}
}
