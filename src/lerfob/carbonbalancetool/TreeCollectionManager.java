package lerfob.carbonbalancetool;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;

public class TreeCollectionManager {

	/**
	 * The treeCollections member is a four-level map.<p>
	 * The keys are:<ol>
	 * <li> the StatusClass enum
	 * <li> the CATCompatibleStand instance
	 * <li> the samplingUnitId (String)
	 * <li> the species (String)
	 * </ol>
	 * The value is a Collection of CATCompatibleTree instances
	 */
	private final Map<StatusClass, Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>>> completeCollections;

	private final Map<StatusClass, Collection<CATCompatibleTree>> statusPooledCollections;

	private final Map<CATCompatibleTree, StatusClass> treeToStatusMap;

	/**
	 * A map whose keys are the trees and values are the corresponding stands.<p>
	 * This map is used to provide a date index in conjunction with the timeTable member.
	 */
	private final Map<CATCompatibleTree, CATCompatibleStand> treeToStandMap;

	TreeCollectionManager() {
		completeCollections = new HashMap<StatusClass, Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>>>();
		statusPooledCollections = new HashMap<StatusClass, Collection<CATCompatibleTree>>();
		treeToStatusMap = new HashMap<CATCompatibleTree, StatusClass>();
		treeToStandMap = new HashMap<CATCompatibleTree, CATCompatibleStand>();
	}
	
	
	CATCompatibleStand getStandOfThisTree(CATCompatibleTree tree) {
		return treeToStandMap.get(tree);
	}
	
	/**
	 * Trees are registered in the treeCollections map and the treeRegister map immediately after the manager has been reset following
	 * the triggering of the calculateCarbon action.
	 * @param statusClass a StatusClass enum
	 * @param stand a CATCompatibleStand instance
	 * @param tree a CATCompatibleTree instance
	 */
	void add(StatusClass statusClass, CATCompatibleStand stand, CATCompatibleTree tree) {
		Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>> innerMap = completeCollections.get(statusClass);
		if (!innerMap.containsKey(stand)) {
			innerMap.put(stand, new HashMap<String, Map<String, Collection<CATCompatibleTree>>>());
		}
		
		Map<String, Map<String, Collection<CATCompatibleTree>>> innerInnerMap = innerMap.get(stand);
		
		String samplingUnitID = CATCompartmentManager.getSamplingUnitID(tree); 
		
		if (!innerInnerMap.containsKey(samplingUnitID)) {
			innerInnerMap.put(samplingUnitID, new HashMap<String, Collection<CATCompatibleTree>>());
		}
		
		Map<String, Collection<CATCompatibleTree>> mostInsideMap = innerInnerMap.get(samplingUnitID);
		if (!mostInsideMap.containsKey(tree.getSpeciesName())) {
			mostInsideMap.put(tree.getSpeciesName(), new ArrayList<CATCompatibleTree>());
		}
		
		Collection<CATCompatibleTree> trees = mostInsideMap.get(tree.getSpeciesName());
		trees.add(tree);
		statusPooledCollections.get(statusClass).add(tree);
		treeToStatusMap.put(tree, statusClass);
		treeToStandMap.put(tree, stand);
	}

	Collection<CATCompatibleTree> getTreeOfThisStatusInThisStand(StatusClass statusClass, CATCompatibleStand stand) {
		Collection<CATCompatibleTree> outputColl = new ArrayList<CATCompatibleTree>();
		Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>> innerMap = completeCollections.get(statusClass);
		if (innerMap.containsKey(stand)) {
			Map<String, Map<String, Collection<CATCompatibleTree>>> mostInnerMap = innerMap.get(stand);
			for (Map<String, Collection<CATCompatibleTree>> oMap : mostInnerMap.values()) {
				for (Collection<CATCompatibleTree> trees : oMap.values()) {
					outputColl.addAll(trees);
				}
			}
		}
		return outputColl;
	}
	
	

	/**
	 * Return the second-level Map from the treeCollections member.<p>
	 * These second-level map are needed for the logging, bucking and transformation of trees into
	 * harvest wood products.
	 * @param statusClass a StatusClass enum
	 * @return a Map instance
	 */
	protected Map<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>> getTrees(StatusClass statusClass) {
		return completeCollections.get(statusClass);
	}

	/**
	 * Return a single level map with all the trees pooled.
	 * @param statusClass a StatusClass enum
	 * @return a Map instance
	 */
	Collection<CATCompatibleTree> getAllTreesOfThisStatus(StatusClass statusClass) {
		return statusPooledCollections.get(statusClass);
	}

	StatusClass getStatusOfThisTree(CATCompatibleTree tree) {
		return treeToStatusMap.get(tree);
	}
	
	
	void clear() {
		completeCollections.clear();
		treeToStandMap.clear();
		statusPooledCollections.clear();
		treeToStatusMap.clear();

		for (StatusClass sc : StatusClass.values()) {
			completeCollections.put(sc, new HashMap<CATCompatibleStand, Map<String, Map<String, Collection<CATCompatibleTree>>>>());
			statusPooledCollections.put(sc, new ArrayList<CATCompatibleTree>());
		}
	}
	
	
}
