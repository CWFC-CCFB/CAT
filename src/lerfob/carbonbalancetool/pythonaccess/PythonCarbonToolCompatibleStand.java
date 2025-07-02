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

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

/**
 * This internal class is actually a wrapper for the stands that are sent to the PythonAccessPoint class.
 * @author Mathieu Fortin - May 2014
 */
@SuppressWarnings("rawtypes")
class PythonCarbonToolCompatibleStand implements CATCompatibleStand, Comparable {

	
	final Map<StatusClass, List<PythonCarbonToolCompatibleTree>> trees;

	private final double areaHa;
	private final String standID;
	private final int dateYr;
	private final List<String> speciesList;
	private final ApplicationScale appScale;
	
	PythonCarbonToolCompatibleStand(String species, double areaHa, String standID, int dateYr, ApplicationScale appScale) {
		trees = new HashMap<StatusClass, List<PythonCarbonToolCompatibleTree>>();
		speciesList = new ArrayList<String>();
		speciesList.add(species);
		this.areaHa = areaHa;
		this.standID = standID;
		this.dateYr = dateYr;
		this.appScale = appScale;
	}
	
	
	
	@Override
	public double getAreaHa() {return areaHa;}

	@Override
	public Collection getTrees(StatusClass statusClass) {
		if (trees.containsKey(statusClass)) {
			return trees.get(statusClass);
		} else {
			return new ArrayList<PythonCarbonToolCompatibleTree>();
		}
	}

	@Override
	public String getStandIdentification() {return standID;}

	@Override
	public int getDateYr() {return dateYr;}

	void addTree(StatusClass statusClass, PythonCarbonToolCompatibleTree tree) {
		if (!trees.containsKey(statusClass)) {
			trees.put(statusClass, new ArrayList<PythonCarbonToolCompatibleTree>());
		}
		trees.get(statusClass).add(tree);
	}



	@Override
	public int compareTo(Object arg0) {
		if (arg0 instanceof PythonCarbonToolCompatibleStand) {
			PythonCarbonToolCompatibleStand stand = (PythonCarbonToolCompatibleStand) arg0;
			if (getDateYr() < stand.getDateYr()) {
				return -1;
			} else if (getDateYr() == stand.getDateYr()) {
				return 0;
			} else {
				return 1;
			}
		}
		throw new InvalidParameterException("The instance is not a valid instance of PythonCarbonToolCompatibleStand");
	}
	
	@Override
	public String toString() {return "Stand " + getDateYr();}



	/*
	 * Useless for this class (non-Javadoc)
	 * @see repicea.simulation.covariateproviders.standlevel.InterventionResultProvider#isInterventionResult()
	 */
	@Override
	public boolean isInterventionResult() {
		return false;
	}

	@Override
	public ManagementType getManagementType() {return ManagementType.UnevenAged;}

	@Override
	public ApplicationScale getApplicationScale() {return appScale;}

	@Override
	public CATCompatibleStand getHarvestedStand() {return null;}

	@Override
	public int getAgeYr() {return getDateYr();}
	
}
