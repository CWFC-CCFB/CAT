package lerfob.carbonbalancetool.io;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

public class CATGrowthSimulationPlot implements CATCompatibleStand {

	private final Map<StatusClass, List<CATGrowthSimulationTree>> statusMap;
	private final double areaHa;
	protected final CATGrowthSimulationPlotSample plotSample;
	private final String plotID;
	private final boolean isInterventionResult;
	
	
	CATGrowthSimulationPlot(String plotID, double areaHa, boolean isInterventionResult, CATGrowthSimulationPlotSample plotSample) {
		this.areaHa = areaHa;
		this.plotSample = plotSample;
		this.plotID = plotID;
		statusMap = new HashMap<StatusClass, List<CATGrowthSimulationTree>>();
		for (StatusClass status : StatusClass.values()) {
			statusMap.put(status, new ArrayList<CATGrowthSimulationTree>());
		}
		this.isInterventionResult = isInterventionResult;
	}
	
	
	@Override
	public double getAreaHa() {return areaHa;}

	@Override
	public Collection<CATGrowthSimulationTree> getTrees(StatusClass statusClass) {return statusMap.get(statusClass);}

	@Override
	public boolean isInterventionResult() {return isInterventionResult;}

	@Override
	public String getStandIdentification() {return plotID;}

	@Override
	public int getDateYr() {return plotSample.getDateYr();}

	protected void addTree(CATGrowthSimulationTree tree) {
		getTrees(tree.getStatusClass()).add(tree);
	}


	@Override
	public ManagementType getManagementType() {return plotSample.getManagementType();}


	@Override
	public ApplicationScale getApplicationScale() {return plotSample.getApplicationScale();}


	@Override
	public CATCompatibleStand getHarvestedStand() {return null;}


	/*
	 * Useless for this class.
	 */
	@Override
	public int getAgeYr() {return getDateYr();}
	
}
