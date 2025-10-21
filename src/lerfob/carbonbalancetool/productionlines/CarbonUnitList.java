/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2012 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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
package lerfob.carbonbalancetool.productionlines;

import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;

/**
 * This class handles the addition of carbon units in its own list. If a similar carbon unit is found in the
 * list, then the carbon unit that was supposed to be added is merged instead. This makes it possible to save
 * memory space. The equals() method serves to define if two carbon units can be merged.
 * @author Mathieu Fortin - April  2011
 */
@SuppressWarnings("serial")
public class CarbonUnitList extends ArrayList<CarbonUnit> {
	
//	private static final long serialVersionUID = 20110413L;

	final Map<String, Map<Integer, List<CarbonUnit>>> speciesMap;
	
	public CarbonUnitList() {
		speciesMap = new HashMap<String, Map<Integer, List<CarbonUnit>>>();
	}
	
	
	@Override
	public boolean add(CarbonUnit carbonUnit) {
		String speciesName = carbonUnit.getSpeciesName();
		int dateIndex = carbonUnit.dateIndex;
		if (!speciesMap.containsKey(speciesName)) {
			List<CarbonUnit> cuList = new ArrayList<CarbonUnit>();
			cuList.add(carbonUnit);
			speciesMap.put(speciesName, new HashMap<Integer, List<CarbonUnit>>());
			speciesMap.get(speciesName).put(dateIndex, cuList);
			super.add(carbonUnit);
		} else {
			Map<Integer, List<CarbonUnit>> innerMap = speciesMap.get(speciesName);
			if (!innerMap.containsKey(dateIndex)) {
				innerMap.put(dateIndex, new ArrayList<CarbonUnit>());
			}
			List<CarbonUnit> innerList = innerMap.get(dateIndex);
			int index = innerList.indexOf(carbonUnit);
			if (index != -1) {
				innerList.get(index).addProcessUnit(carbonUnit);
			} else {
				innerList.add(carbonUnit);
				super.add(carbonUnit);
			}
		}
		return true;
	}

	@Override
	public boolean addAll(Collection<? extends CarbonUnit> coll) {
		if (!coll.isEmpty()) {
			for (Object obj : coll) {
				add((CarbonUnit) obj);
			}
		}
		return true;
	}
	
	@Override
	public String toString() {
		double volume = 0;
		for (CarbonUnit unit : this) {
			volume += unit.getAmountMap().get(Element.Volume);
		}
		return "Volume = " + volume;
	}
	

	/**
	 * Filter the CarbonUnitList instance.
	 * @param clazz a Class of CarbonUnit
	 * @param methodName the method name
	 * @param expectedValue the expected value
	 * @return a CarbonUnitList instance
	 */
	public CarbonUnitList filterList(Class<? extends CarbonUnit> clazz, String methodName, Object expectedValue) {
		try {
			CarbonUnitList subList = new CarbonUnitList();
			Method method = clazz.getDeclaredMethod(methodName);
			for (CarbonUnit carbonUnit : this) {
				Object res = method.invoke(carbonUnit);
				if (expectedValue.equals(res)) {
					subList.add(carbonUnit);
				}
			}
			return subList;
		} catch (Exception e) {
			throw new InvalidParameterException("Unable to filter the CarbonUnitList instance with this method name : " + methodName);
		}
	}
	

	public void clear() {
		speciesMap.clear();
		super.clear();
	}
	

	
}
