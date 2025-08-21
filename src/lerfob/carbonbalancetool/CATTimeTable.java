/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2020-2024 His Majesty the King in Right of Canada
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
package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import repicea.simulation.covariateproviders.plotlevel.StochasticInformationProvider;

/**
 * A class to handle the time frame of the simulation and the index related 
 * to the time slot. <p>
 * 
 * It also handles the realizations. The access to the realizations of the stands
 * is granted through the getStandsForThisRealization method.
 * 
 * @author Mathieu Fortin - November 2020
 */
public class CATTimeTable {

	private final int lastStandDate;
	private final int initialAgeYr;
	private final ArrayList<Integer> internalTimeTable;
	private final Map<CATCompatibleStand, Integer> standMap;
	private final Map<CATCompatibleStand, Integer> realizationStandMap;
	private int monteCarloRealizationId;
	private final List<CATCompatibleStand> currentStands;
	
	/**
	 * Constructor with average time step greater than 1 year.
	 * @param stands
	 * @param initialAgeYr
	 * @param nbExtraYears
	 * @param averageTimeStep
	 */
	@Deprecated
	CATTimeTable(List<CATCompatibleStand> stands, int initialAgeYr, int nbExtraYears, int averageTimeStep) {
		this.internalTimeTable = new ArrayList<Integer>();
		this.standMap = new LinkedHashMap<CATCompatibleStand, Integer>();
		this.realizationStandMap = new LinkedHashMap<CATCompatibleStand, Integer>();
		this.currentStands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand lastStand = stands.get(stands.size() - 1);
		this.lastStandDate = lastStand.getDateYr();
		int size = stands.size() + nbExtraYears / averageTimeStep;
		for (int i = 0; i < size; i++) {
			if (i < stands.size()) {
				CATCompatibleStand stand = stands.get(i);
				internalTimeTable.add(stand.getDateYr());
				standMap.put(stand, internalTimeTable.size() - 1);
			} else  {
				internalTimeTable.add(internalTimeTable.get(i - 1) + averageTimeStep);
			}
		}
		this.initialAgeYr = initialAgeYr;
	}

	/**
	 * Constructor with annual time step.
	 * @param stands
	 * @param initialAgeYr
	 * @param nbExtraYears
	 */
	CATTimeTable(List<CATCompatibleStand> stands, int initialAgeYr, int nbExtraYears) {
		this.internalTimeTable = new ArrayList<Integer>();
		this.standMap = new LinkedHashMap<CATCompatibleStand, Integer>();
		this.realizationStandMap = new LinkedHashMap<CATCompatibleStand, Integer>();
		this.currentStands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand lastStand = stands.get(stands.size() - 1);
		this.lastStandDate = lastStand.getDateYr();
//		int startDateYr = stands.get(0).getDateYr();
//		int lastDateYr = lastStandDate + nbExtraYears;
		int currentDateYr = -1;
		for (int i = 0; i < stands.size(); i++) {
			CATCompatibleStand stand = stands.get(i);
			currentDateYr = stand.getDateYr();
			if (i > 0) {
				int lastDateYr;
				while((lastDateYr = internalTimeTable.get(internalTimeTable.size() - 1)) < currentDateYr - 1) {
					internalTimeTable.add(lastDateYr + 1);
				}
			}
			internalTimeTable.add(currentDateYr); 
			standMap.put(stand, internalTimeTable.size()-1);
		}
		for (int i = 0; i < nbExtraYears; i++) {
			internalTimeTable.add(++currentDateYr);
		}
		this.initialAgeYr = initialAgeYr;
	}


	int getIndexOfThisStandOnTheTimeTable(CATCompatibleStand stand) {
		return realizationStandMap.get(stand);
	}
	
	/**
	 * Create a list of integers which are the indices that go from
	 * the date of the initStand to the date of the finalStand.
	 * @param initStand a CATCompatibleStand instance
	 * @param finalStand a CATCompatibleStand instance
	 * @return a List of indices
	 */
	public List<Integer> getIndicesFromTo(CATCompatibleStand initStand, CATCompatibleStand finalStand) {
		int initIndex = getIndexOfThisStandOnTheTimeTable(initStand);
		int finalIndex = getIndexOfThisStandOnTheTimeTable(finalStand);
		if (initIndex > finalIndex) {
			throw new UnsupportedOperationException("The initStand argument has a date later than that of the finalStand argument!");
		}
		List<Integer> outputList = new ArrayList<Integer>();
		for (int i = initIndex; i <= finalIndex; i++) {
			outputList.add(i);
		}
		return outputList;
	}
	
	private int getLastStandDate() {return lastStandDate;}
	int getInitialAgeYr() {return initialAgeYr;}
	
	Vector<Integer> getListOfDatesUntilLastStandDate() {
		Vector<Integer> dates = new Vector<Integer>();
		for (int i = 0; i <= internalTimeTable.lastIndexOf(getLastStandDate()); i++) {
			if (!dates.contains(internalTimeTable.get(i))) {
				dates.add(internalTimeTable.get(i));
			}
		}
		return dates;
	}

	public int size() {return internalTimeTable.size();}
	
	public int getDateYrAtThisIndex(int i) {return internalTimeTable.get(i);}
	
	public int lastIndexOf(int dateYr) {return internalTimeTable.lastIndexOf(dateYr);}

	@SuppressWarnings("unchecked")
	void setMonteCarloRealization(int realizationId) {
		realizationStandMap.clear();
		currentStands.clear();
		if (CATSensitivityAnalysisSettings.getInstance().isModelStochastic()) {
			for (CATCompatibleStand stand : standMap.keySet()) {
				int indexOfThisStand = standMap.get(stand);
				List<Integer> monteCarloIds = ((StochasticInformationProvider<? extends CATCompatibleStand>) stand).getRealizationIds();
				CATCompatibleStand standForThisRealization = ((StochasticInformationProvider<? extends CATCompatibleStand>) stand).getRealization(monteCarloIds.get(realizationId));
				realizationStandMap.put(standForThisRealization, indexOfThisStand);
				currentStands.add(standForThisRealization);
			}
		} else {
			realizationStandMap.putAll(standMap);
			currentStands.addAll(realizationStandMap.keySet());
		}
		monteCarloRealizationId = realizationId;
	}
	
	int getCurrentMonteCarloRealizationId() {
		return monteCarloRealizationId;
	}
	
	List<CATCompatibleStand> getStandsForThisRealization() {
		return currentStands;
	}
	
	
	CATCompatibleStand getLastStandForThisRealization() {
		return currentStands.get(currentStands.size() - 1);
	}
	
	
	List<List<CATCompatibleStand>> getSegments() {
		List<List<CATCompatibleStand>> outputList = new ArrayList<List<CATCompatibleStand>>();
		List<CATCompatibleStand> innerList = null;
		for (int i = 0; i < getStandsForThisRealization().size(); i++) {
			CATCompatibleStand s = getStandsForThisRealization().get(i);
			if (i == 0 || s.isInterventionResult()) {
				innerList = new ArrayList<CATCompatibleStand>();
				outputList.add(innerList);
			}
			innerList.add(s);
		}
		return outputList;
	}
	
	
}
