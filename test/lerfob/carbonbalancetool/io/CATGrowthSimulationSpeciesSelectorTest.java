/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2022 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service
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

import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

import repicea.util.ObjectUtility;

public class CATGrowthSimulationSpeciesSelectorTest {

	@Test(expected = IOException.class)
	public void testXXSpeciesSelectorDeserializationWithException() throws IOException {
		String fileToLoad = ObjectUtility.getPackagePath(getClass()) + "speciesCorrespondanceForUnevenAgedSimulation.xml";
		CATGrowthSimulationSpeciesSelectorV2 selector = new CATGrowthSimulationSpeciesSelectorV2(Arrays.asList("patate", "chou"));
		selector.load(fileToLoad);
	}

	@Test
	public void testXXSpeciesSelectorSuccessfulDeserialization() throws IOException {
		String fileToLoad = ObjectUtility.getPackagePath(getClass()) + "speciesCorrespondanceForUnevenAgedSimulation.xml";
		CATGrowthSimulationSpeciesSelectorV2 selector = new CATGrowthSimulationSpeciesSelectorV2(Arrays.asList("???", "BOG", "BOJ", "BOP", "EPB", "ERR", "PET", "PIG", "PRP", "SAB", "SAL", "SOA", "THO"));
		selector.load(fileToLoad);
	}

}
