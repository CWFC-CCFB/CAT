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
import lerfob.carbonbalancetool.CATCompatibleTree;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;

/**
 * This class represents the stand in a yield table import in CAT.
 * @author Mathieu Fortin - June 2016
 */
class CATYieldTableCompatibleStand implements CATCompatibleStand {

	private final String standId;
	private final int dateYr;
	private final boolean isInterventionResult;
	protected final Species species;
	protected final Map<StatusClass, Collection<CATCompatibleTree>> statusClassMap;

	private SpeciesLocale locale;
	
	CATYieldTableCompatibleStand(String standId, 
			int dateYr, 
			boolean isInterventionResult, 
			Species species,
			SpeciesLocale locale) {
		this.standId = standId;
		this.dateYr = dateYr;
		this.isInterventionResult = isInterventionResult;
		statusClassMap = new HashMap<StatusClass, Collection<CATCompatibleTree>>();
		for (StatusClass statusClass : StatusClass.values()) {
			statusClassMap.put(statusClass, new ArrayList<CATCompatibleTree>());
		}
		this.species = species;
		this.locale = locale;
	}

	
	
	@Override
	public String getStandIdentification() {return standId;}

	@Override
	public int getDateYr() {return dateYr;}

	@Override
	public double getAreaHa() {return 1d;}

	@Override
	public Collection<?> getTrees(StatusClass statusClass) {return statusClassMap.get(statusClass);}

	@Override
	public boolean isInterventionResult() {return isInterventionResult;}

	@Override
	public ManagementType getManagementType() {return ManagementType.EvenAged;}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public CATCompatibleStand getHarvestedStand() {
		CATYieldTableCompatibleStand harvestedStand = new CATYieldTableCompatibleStand(standId, dateYr, true, species, locale);
		for (StatusClass statusClass : StatusClass.values()){
			StatusClass newStatusClass = statusClass;
			if (newStatusClass == StatusClass.alive) {
				newStatusClass = StatusClass.cut;
			} 
			Collection<CATCompatibleTree> newTrees = cloneCollection((Collection) getTrees(statusClass), newStatusClass);
			harvestedStand.addTrees(newTrees);
		}
		return harvestedStand;
	}

	private Collection<CATCompatibleTree> cloneCollection(Collection<CATCompatibleTree> trees, StatusClass newStatus) {
		List<CATCompatibleTree> newTrees = new ArrayList<CATCompatibleTree>();
		for (CATCompatibleTree tree : trees) {
			CATYieldTableCompatibleTree newTree = ((CATYieldTableCompatibleTree) tree).getClone();
			newTree.setStatusClass(newStatus);
			newTrees.add(newTree);
		}
		return newTrees;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	void addTree(CATYieldTableCompatibleTree tree) {
		Collection coll = getTrees(tree.getStatusClass());
		coll.add(tree);
		tree.stand = this;
	}

	@SuppressWarnings("rawtypes")
	void addTrees(Collection trees) {
		for (Object t : trees) {
			if (t instanceof CATYieldTableCompatibleTree) {
				addTree((CATYieldTableCompatibleTree) t);
			}
		}
	}

	@Override
	public ApplicationScale getApplicationScale() {return ApplicationScale.Stand;}

	@Override
	public int getAgeYr() {return getDateYr();}



	SpeciesLocale getSpeciesLocale() {return locale;}
	
}
