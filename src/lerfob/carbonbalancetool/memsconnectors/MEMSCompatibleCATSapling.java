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
package lerfob.carbonbalancetool.memsconnectors;

import lerfob.carbonbalancetool.interfaces.CATSapling;
import repicea.simulation.species.REpiceaSpecies.Species;

/**
 * A MEMS compatible CATSapling class.
 * @author Mathieu Fortin - August 2025
 */
public final class MEMSCompatibleCATSapling extends CATSapling implements MEMSCompatibleTree {

	final double stemBasalAreaM2;
	final double annualFineRootDetritusCarbonProductionMgYr;
	final double foliarBiomassMg;
	final double annualBranchDetritusCarbonProductionMgYr;

	/**
	 * Constructor.
	 * @param aboveGroundBiomassMg the aboveground biomass of one sapling (Mg)
	 * @param species a CATSpecies enum
	 * @param expansionFactor the number of saplings represented by this instance.
	 * @param plotWeight the sampling weight of the plot
	 * @param stemBasalAreaM2 the sapling basal area (m2)
	 * @param annualFineRootDetritusCarbonProductionMgYr the annual fine root detritus (Mg/Yr of C)
	 * @param foliarBiomassMg the foliar biomass (Mg)
	 * @param annualBranchDetritusCarbonProductionMgYr the annual branch detritus (Mg/Yr of C)
	 */
	public MEMSCompatibleCATSapling(double aboveGroundBiomassMg,
			Species species,
			double expansionFactor,
			double plotWeight,
			double stemBasalAreaM2,
			double annualFineRootDetritusCarbonProductionMgYr,
			double foliarBiomassMg,
			double annualBranchDetritusCarbonProductionMgYr) {
		super(aboveGroundBiomassMg, species, expansionFactor, plotWeight);
		this.stemBasalAreaM2 =  stemBasalAreaM2;
		this.annualFineRootDetritusCarbonProductionMgYr = annualFineRootDetritusCarbonProductionMgYr;
		this.foliarBiomassMg = foliarBiomassMg;
		this.annualBranchDetritusCarbonProductionMgYr = annualBranchDetritusCarbonProductionMgYr;
	}

	@Override
	public double getStemBasalAreaM2() {return stemBasalAreaM2;}

	@Override
	public double getAnnualFineRootDetritusCarbonProductionMgYr() {return annualFineRootDetritusCarbonProductionMgYr;}

	@Override
	public double getFoliarBiomassMg() {return foliarBiomassMg;}

	@Override
	public double getAnnualBranchDetritusCarbonProductionMgYr() {return annualBranchDetritusCarbonProductionMgYr;}

}
