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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.simulation.covariateproviders.plotlevel.StochasticInformationProvider;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

/**
 * This class represents the plots in a growth simulation import in CAT.
 * @author Mathieu Fortin - July 2017
 */
public class CATGrowthSimulationCompositeStand implements CATCompatibleStand, StochasticInformationProvider<CATGrowthSimulationPlotSample> {

	private final String standIdentification;
	private final Map<Integer, CATGrowthSimulationPlotSample> realizationMap;
	private final int dateYr;
	protected final CATGrowthSimulationRecordReader reader;
	private ManagementType managementType;
	private ApplicationScale applicationScale;
	private final boolean isInterventionResult;
	
	protected CATGrowthSimulationCompositeStand(int dateYr, String standIdentification, CATGrowthSimulationRecordReader reader, boolean isInterventionResult) {
		this.dateYr = dateYr;
		this.standIdentification = standIdentification;
		this.reader = reader;
		realizationMap = new HashMap<Integer, CATGrowthSimulationPlotSample>();
		managementType = ManagementType.UnevenAged;
		applicationScale = reader.scale;
		this.isInterventionResult = isInterventionResult;
	}
	
	@Override
	public double getAreaHa() {return getRealization(0).getAreaHa();}

	@Override
	public Collection<CATGrowthSimulationTree> getTrees(StatusClass statusClass) {
		Collection<CATGrowthSimulationTree> coll = new ArrayList<CATGrowthSimulationTree>();
		for (CATGrowthSimulationPlotSample plotSample : realizationMap.values()) {
			coll.addAll(plotSample.getTrees(statusClass));
		}
		return coll;
	}
		
	@Override
	public boolean isInterventionResult() {return isInterventionResult;}

	@Override
	public String getStandIdentification() {return standIdentification;}

	@Override
	public int getDateYr() {return dateYr;}

	@Override
	public List<Integer> getRealizationIds() {
		List<Integer> ids = new ArrayList<Integer>();
		ids.addAll(realizationMap.keySet());
		return ids;
	}

	@Override
	public boolean isStochastic() {return getRealizationIds().size() > 1;}

	@Override
	public CATGrowthSimulationPlotSample getRealization(int realizationID) {return realizationMap.get(realizationID);}

	void createRealizationIfNeeded(int realization) {	
		if (!realizationMap.containsKey(realization)) {
			realizationMap.put(realization, createPlotSample());
		}
	}
	
	protected CATGrowthSimulationPlotSample createPlotSample() {
		return new CATGrowthSimulationPlotSample(this);
	}

	@Override
	public ManagementType getManagementType() {return managementType;}

	@Override
	public ApplicationScale getApplicationScale() {return applicationScale;}

	/*
	 * For test purposes
	 */
	void setApplicationScale(ApplicationScale appScale) {applicationScale = appScale;}
	
	/*
	 * For test purposes
	 */
	void setManagementType(ManagementType managType) {managementType = managType;}
	
	@Override
	public CATCompatibleStand getHarvestedStand() {return null;}

	/*
	 * Useless for this class.
	 */
	@Override
	public int getAgeYr() {
		return getDateYr();
	}


}
