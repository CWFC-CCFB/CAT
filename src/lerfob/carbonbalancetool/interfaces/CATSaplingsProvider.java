/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2025 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service 
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
package lerfob.carbonbalancetool.interfaces;

import java.util.List;

import lerfob.carbonbalancetool.CATCompatibleTree;

/**
 * An interface that ensures that the stand instance can provide
 * its saplings.<p>
 * The sapling should be of the same class than the merchantable trees. 
 */
public interface CATSaplingsProvider {

	/**
	 * A list of saplings for this stand instance.<p>
	 * This saplings should be of the same class as merchantable trees.
	 * @return a List of CATCompatibleTree
	 */
	public List<CATCompatibleTree> getSaplings();
	
}
