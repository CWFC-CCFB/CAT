/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2018 Mathieu Fortin AgroParisTech/INRA UMR LERFoB, 
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
package lerfob.carbonbalancetool.interfaces;

import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import repicea.simulation.processsystem.AmountMap;

/**
 * This interface ensures that the wood piece can return its additional element concentrations.
 * @author Mathieu Fortin - February 2018
 */
public interface CATAdditionalElementsProvider {

	/**
	 * This method returns a Map with the concentrations of additional elements (typically N, K, S, P).  
	 * @return an AmountMap instance
	 */
	public AmountMap<Element> getAdditionalElementConcentrations();
	
}
