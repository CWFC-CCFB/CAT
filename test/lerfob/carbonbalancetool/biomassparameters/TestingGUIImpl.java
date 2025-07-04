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

import lerfob.carbonbalancetool.interfaces.CATAboveGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATBasicWoodDensityProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATCarbonContentRatioProvider;

public class TestingGUIImpl {

	public static class FakeReferent implements CATAboveGroundBiomassProvider, 
												CATAboveGroundVolumeProvider, 
												CATAboveGroundCarbonProvider,
												CATBelowGroundCarbonProvider,
												CATBelowGroundBiomassProvider,
												CATBasicWoodDensityProvider,
												CATCarbonContentRatioProvider {

		@Override
		public double getAboveGroundBiomassMg() {
			return 0;
		}

		@Override
		public double getBelowGroundBiomassMg() {
			return 0;
		}

//		@Override
//		public double getBelowGroundVolumeM3() {
//			return 0;
//		}

		@Override
		public double getBelowGroundCarbonMg() {
			return 0;
		}

		@Override
		public double getAboveGroundCarbonMg() {
			return 0;
		}

		@Override
		public double getAboveGroundVolumeM3() {
			return 0;
		}

		@Override
		public double getCarbonContentRatio() {
			return 0;
		}

		@Override
		public double getBasicWoodDensity() {
			return 0;
		}
		
	}

	
	public static void main(String[] args) {
		BiomassParameters param = new BiomassParameters();
		param.setReferent(new FakeReferent());
		param.showUI(null);
		System.exit(0);
	}
	
	
}
