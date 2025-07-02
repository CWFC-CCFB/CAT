package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

class CarbonToolCompatibleStandImpl implements CATCompatibleStand {

	
	private final double areaHa;
	private final int dateYr;
	private final int ageYr;
	@SuppressWarnings("rawtypes")
	private final Map<StatusClass, Collection> treeMap;
	private final String standID;
	private final String species;
	
	@SuppressWarnings("rawtypes")
	protected CarbonToolCompatibleStandImpl(String species, String standID, double areaHa, int dateYr, int ageYr) {
		this.species = species;
		this.standID = standID;
		this.areaHa = areaHa;
		this.dateYr = dateYr;
		this.ageYr = ageYr;
		treeMap = new HashMap<StatusClass, Collection>();
	}
	
	@SuppressWarnings("unchecked")
	protected void addTree(CATCompatibleTree tree) {
		getTrees(tree.getStatusClass()).add(tree);
	}
	
	@Override
	public double getAreaHa() {return areaHa;}

	@SuppressWarnings("rawtypes")
	@Override
	public Collection getTrees(StatusClass statusClass) {
		if (!treeMap.containsKey(statusClass)) {
			treeMap.put(statusClass, new ArrayList());
		}
		return treeMap.get(statusClass);
	}

	@Override
	public String getStandIdentification() {return standID;}

	@Override
	public int getDateYr() {return dateYr;}


	@Override
	public ManagementType getManagementType() {return ManagementType.EvenAged;}
	
	@SuppressWarnings("rawtypes")
	@Override
	public CATCompatibleStand getHarvestedStand() {
		CarbonToolCompatibleStandImpl newStand = new CarbonToolCompatibleStandImpl(species, standID, areaHa, dateYr, ageYr);
		Collection coll = getTrees(StatusClass.alive);
		for (Object obj : coll) {
			CarbonToolCompatibleTreeImpl tree = (CarbonToolCompatibleTreeImpl) obj;
			CATCompatibleTree clonedTree = tree.clone();
			clonedTree.setStatusClass(StatusClass.cut);
			newStand.addTree(clonedTree);
		}
		return newStand;
	}

	@Override
	public boolean isInterventionResult() {
		return false;
	}

	@Override
	public ApplicationScale getApplicationScale() {return ApplicationScale.Stand;}

	@Override
	public int getAgeYr() {return ageYr;}

}
