package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

class CarbonToolCompatibleStandImpl implements CATCompatibleStand {

	
	final double areaHa;
	final int dateYr;
	final int ageYr;
	@SuppressWarnings("rawtypes")
	final Map<StatusClass, Collection> treeMap;
	final String standID;
	final String species;
	
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
	protected void addTree(CATCompatibleTree tree, StatusClass statusClass) {
		getTrees(statusClass).add(tree);
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
			newStand.addTree(clonedTree, StatusClass.cut);
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
