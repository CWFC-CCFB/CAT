/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2018 Mathieu Fortin 
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
package lerfob.carbonbalancetool.catdiameterbasedtreelogger;

import java.util.List;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLogCategory;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLogger;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedWoodPiece;
import repicea.simulation.covariateproviders.treelevel.DbhCmProvider;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.treelogger.LoggableTree;
import repicea.simulation.treelogger.TreeLoggerCompatibilityCheck;

/**
 * The CATDiameterBasedTreeLogger class implements a tree logger for CAT. It is 
 * based on a default species. However, the user can add different specification 
 * for some species.
 * @author Mathieu Fortin - December 2018
 */
public class CATDiameterBasedTreeLogger extends DiameterBasedTreeLogger {


	@Override
	public CATDiameterBasedTreeLoggerParameters createDefaultTreeLoggerParameters() {
		return new CATDiameterBasedTreeLoggerParameters();
	}
	
	@Override
	public CATCompatibleTree getEligible(LoggableTree t) {
		if (isCompatibleTree(t)) {
			return (CATCompatibleTree) t;
		} else {
			return null;
		}
	}

	
	private boolean isCompatibleTree(Object treeInstance) {
		return treeInstance instanceof CATCompatibleTree && treeInstance instanceof DbhCmProvider; 
	}
	
	
	@Override
	public boolean isCompatibleWith(TreeLoggerCompatibilityCheck check) {
		return isCompatibleTree(check.getTreeInstance());
	}

	@Override
	public CATDiameterBasedTreeLoggerParameters getTreeLoggerParameters() {
		return (CATDiameterBasedTreeLoggerParameters) super.getTreeLoggerParameters();
	}
	
	
	@Override
	protected void logThisTree(LoggableTree tree) {
		Species species = ((CATCompatibleTree) tree).getCATSpecies();
		List<DiameterBasedTreeLogCategory> logCategories = getTreeLoggerParameters().getSpeciesLogCategories(species);
		List<DiameterBasedWoodPiece> pieces;
		for (DiameterBasedTreeLogCategory logCategory : logCategories) {
			pieces = ((CATDiameterBasedTreeLogCategory) logCategory).extractFromTree(tree, params);
			if (pieces != null && !pieces.isEmpty()) {
				for (DiameterBasedWoodPiece piece : pieces) {
					addWoodPiece(tree, piece);	
				}
				if (shouldBreakAfterGettingPieces) {
					break;
				}
			} 
		}
	}


}
