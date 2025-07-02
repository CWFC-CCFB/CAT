/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
 * Copyright (C) 2024 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, 
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
package lerfob.carbonbalancetool.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

public class CATGrowthSimulationPlotSample implements CATCompatibleStand {

	protected final CATGrowthSimulationCompositeStand compositeStand;
	
	private final Map<String, CATGrowthSimulationPlot> plotMap;
	
	
	protected CATGrowthSimulationPlotSample(CATGrowthSimulationCompositeStand compositeStand) {
		this.compositeStand = compositeStand;
		this.plotMap = new HashMap<String, CATGrowthSimulationPlot>();
	}
	
	@Override
	public double getAreaHa() {
		double areaHa = 0d;
		for (CATGrowthSimulationPlot plot : plotMap.values()) {
			areaHa += plot.getAreaHa();
		}
		return areaHa;
	}

	@Override
	public Collection<CATGrowthSimulationTree> getTrees(StatusClass statusClass) {
		Collection<CATGrowthSimulationTree> coll = new ArrayList<CATGrowthSimulationTree>();
		for (CATGrowthSimulationPlot plot : getPlotMap().values()) {
			coll.addAll(plot.getTrees(statusClass));
		}
		return coll;
	}

	@Override
	public boolean isInterventionResult() {return false;}

	@Override
	public String getStandIdentification() {return compositeStand.getStandIdentification();}

	@Override
	public int getDateYr() {return compositeStand.getDateYr();}

	void createPlot(String plotID, double plotAreaHa, boolean isInterventionResult) {
		if (!getPlotMap().containsKey(plotID)) {
			getPlotMap().put(plotID, new CATGrowthSimulationPlot(plotID, plotAreaHa, isInterventionResult, this));
		}
	}
	
	Map<String, CATGrowthSimulationPlot> getPlotMap() {return plotMap;}
	
	CATGrowthSimulationPlot getPlot(String plotID) {return getPlotMap().get(plotID);}

	@Override
	public ManagementType getManagementType() {return compositeStand.getManagementType();}

	@Override
	public ApplicationScale getApplicationScale() {return compositeStand.getApplicationScale();}

	@Override
	public CATCompatibleStand getHarvestedStand() {return null;}

	/*
	 * Useless for this class.
	 */
	@Override
	public int getAgeYr() {return getDateYr();}

}
