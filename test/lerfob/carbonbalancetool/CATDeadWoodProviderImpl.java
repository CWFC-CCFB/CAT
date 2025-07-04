package lerfob.carbonbalancetool;

import java.util.HashMap;
import java.util.Map;

import lerfob.carbonbalancetool.interfaces.CATDeadWoodProvider;

public class CATDeadWoodProviderImpl extends CarbonToolCompatibleStandImpl implements CATDeadWoodProvider {

	final double deadBiomassMg;
	
	protected CATDeadWoodProviderImpl(String species, 
			String standID, 
			double areaHa, 
			int dateYr, 
			int ageYr,
			double deadBiomassMg) {
		super(species, standID, areaHa, dateYr, ageYr);
		this.deadBiomassMg = deadBiomassMg;
	}
	
	@Override
	public Map<String, Double> getDeadWoodBiomassMgForTheseSamplingUnits() {
		Map<String, Double> outputMap = new HashMap<String, Double>();
		outputMap.put(standID, deadBiomassMg);
		return outputMap;
	}

}
