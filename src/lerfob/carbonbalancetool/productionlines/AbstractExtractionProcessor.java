/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2020-24 His Majesty the King in right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, 
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
package lerfob.carbonbalancetool.productionlines;

import java.awt.Color;
import java.awt.Container;
import java.awt.Graphics;
import java.util.Collection;
import java.util.List;

import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager.CarbonTestProcessUnit;
import repicea.gui.permissions.REpiceaGUIPermission;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.Processor;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.ProcessorListTable.MemberInformation;
import repicea.simulation.processsystem.SystemPanel;

/**
 * The AbstractExtractionProcessor class defines a process that extracts something before splitting
 * the ElementUnit instance (e.g. debarking).
 * 
 * @author Mathieu Fortin - September 2020
 */
@SuppressWarnings("serial")
public abstract class AbstractExtractionProcessor extends AbstractProcessor {

	
	
	protected static class CustomizedREpiceaGUIPermission implements REpiceaGUIPermission {

		@Override
		public boolean isDragGranted() {
			return true;
		}

		@Override
		public boolean isDropGranted() {	// AbstractExtractionProcessor cannot be drag and drop. They must be created through the popup menu.
			return false;
		}

		@Override
		public boolean isSelectionGranted() {
			return true;
		}

		/**
		 * Enabling is mainly set through the SystemComponentMouseAdapter class.<p>
		 * See the ComponentsToBeEnabledDisabled static member of that class for the list of components.
		 */
		@Override
		public boolean isEnablingGranted() {	
			return true;
		}
	}

	/**
	 * A species AbstractProcessorButton for the ExtractionProcessor class.
	 * @author Mathieu Fortin - January 2021
	 */
	protected static class ExtractionProcessorButton extends AbstractProcessorButton {
		protected ExtractionProcessorButton(SystemPanel panel, AbstractExtractionProcessor process) {
			super(panel, process, new CustomizedREpiceaGUIPermission());
		}
		
		
		@Override
		public void paint(Graphics g) {
			if (!getOwner().hasSubProcessors()) {
				setBorderColor(Color.RED);
				setBorderWidth(2);
			} else {
				setBorderColor(Color.BLACK);
				setBorderWidth(1);
			}
			super.paint(g);
		}

	}
	
	@SuppressWarnings("rawtypes")
	protected Collection<ProcessUnit> extractAndProcess(Processor fatherProcessor, List<ProcessUnit> processUnits) {
		List<ProcessUnit> extractedUnits = extract(processUnits);
		if (!extractedUnits.isEmpty()) {
			for (ProcessUnit pu : extractedUnits) {
				if (pu instanceof CarbonTestProcessUnit) {
					((CarbonTestProcessUnit) pu).recordProcessor(fatherProcessor);	// we must also record the father processor otherwise it is simply skipped
//					((CarbonTestProcessUnit) pu).recordProcessor(this);
				}
			}
			return doProcess(extractedUnits);
		} else {
			return extractedUnits;
		}
	}

	/**
	 * Extract the ProcessUnit instances that match some criteria.<p>
	 * These ProcessUnit instances are returned by the method. IMPORTANT: these units must be removed from
	 * the processUnits argument.
	 * @param processUnits a List of ProcessUnit instances
	 * @return the list of units that were extracted
	 */
	@SuppressWarnings("rawtypes")
	protected abstract List<ProcessUnit> extract(List<ProcessUnit> processUnits);
	
	
	@Override
	public ProcessorButton getUI(Container container) {
		if (guiInterface == null) {
			guiInterface = new ExtractionProcessorButton((SystemPanel) container, this);
		}
		return guiInterface;
	}

	@Override
	public List<MemberInformation> getInformationsOnMembers() {
		List<MemberInformation> cellValues = super.getInformationsOnMembers();
		cellValues.add(new MemberInformation(AbstractProductionLineProcessor.MemberLabel.FunctionUnitBiomass, double.class, functionUnitBiomass));
		cellValues.add(new MemberInformation(AbstractProductionLineProcessor.MemberLabel.EmissionFunctionUnit, double.class, emissionsByFunctionalUnit));
		return cellValues;
	}

	@Override
	public void processChangeToMember(Enum<?> label, Object value) {
		if (label == AbstractProductionLineProcessor.MemberLabel.FunctionUnitBiomass) {
			functionUnitBiomass = (double) value;
		} else if (label == AbstractProductionLineProcessor.MemberLabel.EmissionFunctionUnit) {
			emissionsByFunctionalUnit = (double) value;
		} else {
			super.processChangeToMember(label, value);
		}
	}

}
