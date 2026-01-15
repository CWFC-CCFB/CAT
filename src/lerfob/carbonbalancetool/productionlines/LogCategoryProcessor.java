/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.productionlines;

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import repicea.serial.PostUnmarshalling;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.SystemPanel;
import repicea.simulation.treelogger.LogCategory;
import repicea.simulation.treelogger.TreeLoggerParameters;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The LogCategoryProcessor is a specific implementation of Processor for a particular log grade category
 * that comes out of TreeLogger instance.
 * @author Mathieu Fortin - May 2014
 */
@SuppressWarnings("serial")
public class LogCategoryProcessor extends LeftHandSideProcessor implements PostUnmarshalling {

	private enum MessageID implements TextableEnum {
		ALL_SPECIES("Any species", "Toute esp\u00E8ce");

		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {
			return REpiceaTranslator.getString(this);
		}
	}
	
	
	/**
	 * The LogCategoryProcessorButton class is the GUI implementation for 
	 * LogCategoryProcessor. It has a specific icon for better identification in the GUI.
	 * @author Mathieu Fortin - May 2014
	 */
	public static class LogCategoryProcessorButton extends LeftHandSideProcessorButton {

		/**
		 * Constructor.
		 * @param panel	a SystemPanel instance
		 * @param processor the LogCategoryProcessor that owns this button
		 */
		protected LogCategoryProcessorButton(SystemPanel panel, LogCategoryProcessor processor) {
			super(panel, processor);
		}


	}

	@Deprecated
	private LogCategory logCategory;
	private List<LogCategory> logCategories;
	
	/**
	 * Constructor.
	 * @param logCategories an array of LogCategory instance
	 */
	protected LogCategoryProcessor(LogCategory... logCategories) {
		super();
		this.logCategories = new ArrayList<LogCategory>();
		for (LogCategory lc : logCategories) {
			this.logCategories.add(lc);
		}
	}

	/*
	 * To maintain compatibility.
	 */
	LogCategory getFirstLogCategory() {
		return logCategories.get(0);
	}
	
	/**
	 * Checks if the log category is contained in the processor.
	 * @param lc a LogCategory instance
	 * @return a boolean
	 */
	public boolean contains(LogCategory lc) {
		return logCategories.contains(lc);
	}

	private boolean isAggregated() {
		return logCategories.size() > 1;
	}
	
	@Override
	public String getName() {
		if (isAggregated()) {
			return logCategories.get(0).getName();
		} else {
			LogCategory logCategory = getFirstLogCategory();
			String speciesName = logCategory.getSpecies().toString();
			if (speciesName.equals(TreeLoggerParameters.ANY_SPECIES)) {
				speciesName = MessageID.ALL_SPECIES.toString();
			}
			return speciesName + " - " + logCategory.getName();
		}
	}
	
	@Override
	public ProcessorButton getUI(Container container) {
		if (guiInterface == null) {
			guiInterface = new LogCategoryProcessorButton((SystemPanel) container, this);
		}
		return guiInterface;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof LogCategoryProcessor) {
			LogCategoryProcessor processor = (LogCategoryProcessor) obj;
			if (this.logCategories.equals(processor.logCategories)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void postUnmarshallingAction() {
		if (logCategory != null) { // former implementation
			logCategories = new ArrayList<LogCategory>();
			logCategories.add(logCategory);
			logCategory = null;
		}
	}

	
}
