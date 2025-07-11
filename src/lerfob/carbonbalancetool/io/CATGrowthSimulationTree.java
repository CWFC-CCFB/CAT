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
import lerfob.carbonbalancetool.CATSettings.CATSpecies;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundVolumeProvider;
import lerfob.carbonbalancetool.io.CATGrowthSimulationRecordReader.CATGrowthSimulationFieldID;

/**
 * This class represents the trees in a growth simulation import in CAT.
 * @author Mathieu Fortin - July 2017
 */
class CATGrowthSimulationTree implements CATCompatibleTree,
										CATAboveGroundVolumeProvider,
										CATAboveGroundBiomassProvider,
										CATAboveGroundCarbonProvider,
										CATBelowGroundVolumeProvider,
										CATBelowGroundBiomassProvider,
										CATBelowGroundCarbonProvider {

	private final double commercialVolumeM3;
	private final double numberOfTrees;
	private StatusClass statusClass;
	private final String originalSpeciesName;
	protected final CATGrowthSimulationPlot plot;
	final Double aboveGroundVolumeM3;
	final Double aboveGroundBiomassMg;
	final Double aboveGroundCarbonMg;
	final Double belowGroundVolumeM3;
	final Double belowGroundBiomassMg;
	final Double belowGroundCarbonMg;
	
	CATGrowthSimulationTree(CATGrowthSimulationPlot plot, 
			StatusClass statusClass, 
			double treeOverbarkVolumeDm3, 
			double numberOfTrees, 
			String originalSpeciesName,
			Double aboveGroundVolumeM3,
			Double aboveGroundBiomassMg,
			Double aboveGroundCarbonMg,
			Double belowGroundVolumeM3,
			Double belowGroundBiomassMg,
			Double belowGroundCarbonMg
			) {
		this.plot = plot;
		commercialVolumeM3 = treeOverbarkVolumeDm3 * .001;
		this.numberOfTrees = numberOfTrees;
		this.originalSpeciesName = originalSpeciesName;
		setStatusClass(statusClass);
		this.aboveGroundVolumeM3 = aboveGroundVolumeM3;
		this.aboveGroundBiomassMg = aboveGroundBiomassMg;
		this.aboveGroundCarbonMg = aboveGroundCarbonMg;
		this.belowGroundVolumeM3 = belowGroundVolumeM3;
		this.belowGroundBiomassMg = belowGroundBiomassMg;
		this.belowGroundCarbonMg = belowGroundCarbonMg;
	}
	
	@Override
	public double getCommercialVolumeM3() {return commercialVolumeM3;}

	@Override
	public String getSpeciesName() {return getCATSpecies().toString();}

//	@Override
//	public SpeciesType getSpeciesType() {return getCATSpecies().getSpeciesType();}

	@Override
	public CATSpecies getCATSpecies() {return (CATSpecies) plot.plotSample.compositeStand.reader.getSelector().getMatch(originalSpeciesName);}
	
	@Override
	public double getNumber() {return numberOfTrees;}

	@Override
	public void setStatusClass(StatusClass statusClass) {this.statusClass = statusClass;}

	@Override
	public StatusClass getStatusClass() {return statusClass;}

	@Override
	public boolean isCommercialVolumeOverbark() {
		return true;
	}
	
	@Override
	public double getBelowGroundCarbonMg() {return belowGroundCarbonMg;}

	@Override
	public double getBelowGroundBiomassMg() {return belowGroundBiomassMg;}

	@Override
	public double getBelowGroundVolumeM3() {return belowGroundVolumeM3;}

	@Override
	public double getAboveGroundCarbonMg() {return aboveGroundCarbonMg;}

	@Override
	public double getAboveGroundBiomassMg() {return aboveGroundBiomassMg;}

	@Override
	public double getAboveGroundVolumeM3() {return aboveGroundVolumeM3;}

	private CATGrowthSimulationCompositeStand getCompositeStand() {return plot.plotSample.compositeStand;}
	
	@Override
	public boolean isCATAboveGroundVolumeProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.AboveGroundVolume);
	}

	@Override
	public boolean isCATAboveGroundBiomassProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.AboveGroundBiomass);
	}

	@Override
	public boolean isCATAboveGroundCarbonProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.AboveGroundCarbon);
	}
	
	@Override
	public boolean isCATBelowGroundVolumeProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.BelowGroundVolume);
	}

	@Override
	public boolean isCATBelowGroundBiomassProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.BelowGroundBiomass);
	}
	
	@Override
	public boolean isCATBelowGroundCarbonProviderInterfaceEnabled() {
		return getCompositeStand().isAssociatedInterfaceEnabled(CATGrowthSimulationFieldID.BelowGroundCarbon);
	}

}
