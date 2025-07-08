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
package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.List;

import lerfob.carbonbalancetool.interfaces.CATSaplingsProvider;

public class CATSaplingsProviderImpl extends CarbonToolCompatibleStandImpl implements CATSaplingsProvider{

	protected CATSaplingsProviderImpl(String species, 
			String standID, 
			double areaHa, 
			int dateYr, 
			int ageYr) {
		super(species, standID, areaHa, dateYr, ageYr);
	}

	@Override
	public List<CATCompatibleTree> getSaplings() {
		List<CATCompatibleTree> saplings = new ArrayList<CATCompatibleTree>();
		for (int i = 0; i < 10; i++) {
			// TODO Auto-generated method stub
		}
		return saplings;
	}

}
