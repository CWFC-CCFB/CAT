/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB, 
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
package lerfob.carbonbalancetool.biomassparameters;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.biomassparameters.BiomassParameters.Tier2Implementation;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundVolumeProvider;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.ObjectUtility;

public class BiomassParametersTest {

	static class FakeTree implements CATCompatibleTree, CATAboveGroundBiomassProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}

		@Override
		public double getAboveGroundBiomassMg() {return 10;}

		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}
		
	}
	
	static class FakeTree2 implements CATCompatibleTree, CATAboveGroundVolumeProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}


		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}

		@Override
		public double getAboveGroundVolumeM3() {return 10d;}
		
	}

	static class FakeTree3 implements CATCompatibleTree, CATAboveGroundCarbonProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}


		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}

		@Override
		public double getAboveGroundCarbonMg() {return 10d;}
		
	}

	static class FakeTree4 implements CATCompatibleTree, CATBelowGroundVolumeProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}


		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}

		@Override
		public double getBelowGroundVolumeM3() {return 10d;}
		
	}

	static class FakeTree5 implements CATCompatibleTree, CATBelowGroundBiomassProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}


		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}

		@Override
		public double getBelowGroundBiomassMg() {return 10d;}
		
	}

	static class FakeTree6 implements CATCompatibleTree, CATBelowGroundCarbonProvider {

		@Override
		public double getCommercialVolumeM3() {return 1d;}

		@Override
		public boolean isCommercialVolumeOverbark() {return false;}

		@Override
		public String getSpeciesName() {return "Abies balsamea";}

		@Override
		public void setStatusClass(StatusClass statusClass) {}

		@Override
		public StatusClass getStatusClass() {return StatusClass.alive;}


		@Override
		public Species getCATSpecies() {return Species.Abies_spp;}

		@Override
		public double getBelowGroundCarbonMg() {return 10d;}
		
	}

	
	
	@Test
	public void deserializationTest() {
		String filename = ObjectUtility.getPackagePath(getClass()) + "AsInNationalReporting.bpf";
		BiomassParameters bp = new BiomassParameters();
		try {
			bp.load(filename);
		} catch (IOException e) {
			Assert.fail("Deserialization of " + filename + " failed!");
		}
	}
	
	@Test
	public void testingTier2ApproachForAboveGroundVolume() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree2 t = new FakeTree2();
		bp.setReferent(t);
		double actualValue = bp.getAboveGroundVolumeM3(t, null);
		Assert.assertEquals("Testing Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.BranchExpansionFactor, false);
		actualValue = bp.getAboveGroundVolumeM3(t, null);
		Assert.assertEquals("Testing Tier 1 approach", 1.4534, actualValue, 1E-8);
	}
	
	@Test
	public void testingTier2ApproachForAboveGroundBiomass() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree t = new FakeTree();
		bp.setReferent(t);
		double actualValue = bp.getAboveGroundBiomassMg(t, null);
		Assert.assertEquals("Testing Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.BranchExpansionFactor, false);
		actualValue = bp.getAboveGroundBiomassMg(t, null);
		Assert.assertEquals("Testing Tier 1 approach", 0.58136, actualValue, 1E-8);
	}
	
	@Test
	public void testingTier2ApproachForAboveGroundCarbon() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree3 t = new FakeTree3();
		bp.setReferent(t);
		double actualValue = bp.getAboveGroundCarbonMg(t, null);
		Assert.assertEquals("Testing Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.BranchExpansionFactor, false);
		actualValue = bp.getAboveGroundCarbonMg(t, null);
		Assert.assertEquals("Testing Tier 1 approach", 0.29678427999999996, actualValue, 1E-8);
	}

	@Test
	public void testingTier2ApproachForBelowGroundVolume() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree4 t = new FakeTree4();
		bp.setReferent(t);
		double actualValue = bp.getBelowGroundVolumeM3(t, null);
		Assert.assertEquals("Testing Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.RootExpansionFactor, false);
		actualValue = bp.getBelowGroundVolumeM3(t, null);
		Assert.assertEquals("Testing Tier 1 approach", 0.43602, actualValue, 1E-8);
	}
	
	@Test
	public void testingTier2ApproachForBelowGroundBiomass() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree5 t = new FakeTree5();
		bp.setReferent(t);
		double actualValue = bp.getBelowGroundBiomassMg(t, null);
		Assert.assertEquals("Testing Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.RootExpansionFactor, false);
		actualValue = bp.getBelowGroundBiomassMg(t, null);
		Assert.assertEquals("Testing Tier 1 approach", 0.174408, actualValue, 1E-8);
	}
	
	@Test
	public void testingTier2ApproachForBelowGroundCarbon() {
		BiomassParameters bp = new BiomassParameters();
		FakeTree6 t = new FakeTree6();
		bp.setReferent(t);
		double actualValue = bp.getBelowGroundCarbonMg(t, null);
		Assert.assertEquals("Testing biomass value with Tier 2 approach", 10d, actualValue, 1E-8);
		bp.setTier2ImplementationEnabled(Tier2Implementation.RootExpansionFactor, false);
		actualValue = bp.getBelowGroundCarbonMg(t, null);
		Assert.assertEquals("Testing biomass value with Tier 1 approach", 0.08903528399999999, actualValue, 1E-8);
	}

}
