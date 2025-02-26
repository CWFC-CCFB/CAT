/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2025 His Majesty the King in Right of Canada
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
package lerfob.carbonbalancetool.productionlines.affiliere;

import java.awt.Point;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import repicea.simulation.processsystem.Processor;

/**
 * A package class to set the location of the processor immediately
 * after importing a flux configuration from AFFiliere.
 * @author Mathieu Fortin - January 2025
 */
class AffiliereLocationPointer {
	
	private final int spaceBetweenRows;
	private final int spaceBetweenColumns;

	private int m_row;
	private int m_colForHWP;
	private int m_col;
	private final int initialRow;

	private final Map<Integer, List<Integer>> locationMap;
	
	/**
	 * 
	 * @param startingRow
	 * @param startingCol
	 * @param spaceBetweenRows
	 * @param spaceBetweenColumns
	 */
	AffiliereLocationPointer(int startingRow, int startingCol, int spaceBetweenRows, int spaceBetweenColumns) {
		if (spaceBetweenRows <= 0 || spaceBetweenColumns <= 0) {
			throw new InvalidParameterException("The spaceBetweenRows and spaceBetweenColumns must be greater than 0!");
		}
		this.spaceBetweenColumns = spaceBetweenColumns;
		this.spaceBetweenRows = spaceBetweenRows;
		m_row = startingRow;
		initialRow = startingRow;
		m_col = startingCol;
		locationMap = new HashMap<Integer, List<Integer>>();
	}

	private boolean isLocationAvailable(int row, int col) {
		if (locationMap.containsKey(row) && locationMap.get(row).contains(col)) {
			return false;
		}
		return true;
	}

	private void registerLocation(int row, int col) {
		if (!locationMap.containsKey(row)) {
			locationMap.put(row, new ArrayList<Integer>());
		}
		locationMap.get(row).add(col);
	}
	
	private Point producePointFromPointer(boolean isEndProductProcessor) {
		if (!isLocationAvailable(m_row, m_col)) {
			m_row++;
		}
		int col = m_col;
		int row = m_row;
		if (isEndProductProcessor) {
			if (col > m_colForHWP) {
				m_colForHWP = col;
			}
			m_row++;
		}
		m_col++;
		registerLocation(row, col);
		return new Point(col * spaceBetweenColumns, row * spaceBetweenRows);
	}

	void setLayout(Map<String, List<Processor>> entryProcessors, List<Processor> processorsWithNoChildren) {
		List<Processor> processorsWithLocation = new ArrayList<Processor>();
		for (Processor p : entryProcessors.get("default")) {
			setLocationForThisProcessorAndChildProcessors(p, processorsWithNoChildren, processorsWithLocation);
		}
		List<Processor> endProcessorsInEOLSector = new ArrayList<Processor>();
		for (Processor p : processorsWithNoChildren) {
			if (processorsWithLocation.contains(p)) {
				int y = p.getOriginalLocation().y;
				p.setOriginalLocation(new Point(spaceBetweenColumns * m_colForHWP, y));
			} else {
				endProcessorsInEOLSector.add(p);
			}
		}
		m_col = m_colForHWP + 1;
		m_row = initialRow;
		for (Processor p : entryProcessors.get("EOL")) {
			setLocationForThisProcessorAndChildProcessors(p, processorsWithNoChildren, processorsWithLocation);
		}
		for (Processor p : endProcessorsInEOLSector) {
			if (processorsWithLocation.contains(p)) {
				int y = p.getOriginalLocation().y;
				p.setOriginalLocation(new Point(spaceBetweenColumns * m_colForHWP, y));
			} else {
				throw new UnsupportedOperationException("This processor is causing problem: " + p.getName());
			}
		}

	}

	private void setLocationForThisProcessorAndChildProcessors(Processor p, List<Processor> processorsWithNoChildren, List<Processor> processorsWithLocation) {
		if (!processorsWithLocation.contains(p)) {
			p.setOriginalLocation(producePointFromPointer(processorsWithNoChildren.contains(p)));
			processorsWithLocation.add(p);
			for (Processor subProcessor : p.getSubProcessors()) {
				setLocationForThisProcessorAndChildProcessors(subProcessor, processorsWithNoChildren, processorsWithLocation);
			}
			m_col--;
		} 
	}

}
