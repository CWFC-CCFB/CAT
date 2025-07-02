/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2009-2014 Mathieu Fortin for Rouge-Epicea
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
package repicea.simulation.processsystem;

import java.awt.Container;
import java.awt.Point;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JTextField;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

import repicea.gui.REpiceaPanel;
import repicea.gui.REpiceaUIObject;
import repicea.gui.REpiceaUIObjectWithParent;
import repicea.simulation.processsystem.ProcessorListTable.MemberHandler;
import repicea.simulation.processsystem.ProcessorListTable.MemberInformation;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * A Processor instance represents a node in the flux configuration.<p>
 * Its UI is a {@link ProcessorButton} instance. Once double-clicked, the ProcessorButton instance
 * creates a {@link ProcessorInternalDialog} instance, which contains the characteristics of the Processor.
 * @author Mathieu Fortin - 2014
 * @see ProcessorButton
 */
@SuppressWarnings("serial")
public class Processor implements REpiceaUIObjectWithParent, REpiceaUIObject, CaretListener, Serializable, MemberHandler, ResourceReleasable {

	protected static enum MemberLabel implements TextableEnum {
		NameField("Name", "Nom");
		
		MemberLabel(String englishName, String frenchName) {
			setText(englishName, frenchName);
		}

		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
	}
	
	private Point originalLocation;
	protected transient ProcessorButton guiInterface;
	
	private Map<Processor, Number> subProcessorIntakes;
	protected List<Processor> subProcessors;
	private String name;
	
	protected boolean isTerminal;
	
	@Deprecated
	protected double averageIntake;
	private boolean markedAsPartOfEndlessLoop;
		
	
	/**
	 * General constructor.
	 */
	public Processor() {
		subProcessors = new ArrayList<Processor>();
		name = SystemManagerDialog.MessageID.Unnamed.toString();
	}

	/**
	 * General constructor with name.
	 * @param name the name of the processor instance
	 */
	public Processor(String name) {
		this();
		this.name = name;
	}
	
	
	/**
	 * A terminal processor cannot have any subprocessor by definition. <p>
	 * 
	 * This method returns true if the processor belong to this category.
	 * @return a boolean
	 */
	public boolean isTerminalProcessor() {return isTerminal;}
	
	
	/**
	 * Process the ProcessUnit through this processor and eventually its subprocessors.
	 * @param inputUnits a List of ProcessUnit instances sent to this Processor instance
	 * @return a Collection of ProcessUnit instances as a result of the process
	 */
	@SuppressWarnings("rawtypes")
	public Collection<ProcessUnit> doProcess(List<ProcessUnit> inputUnits) {
		Collection<ProcessUnit> coll = new ArrayList<ProcessUnit>();
		
		for (ProcessUnit inputUnit : inputUnits) {	// check if some input units are TestProcessUnit instances
			if (inputUnit instanceof TestProcessUnit) {
				if (((TestProcessUnit) inputUnit).recordProcessor(this)) {
					coll.add(inputUnit);		// it stops the recursive method here
				}
			}
		}		
		inputUnits.removeAll(coll); // and make sure that they won't follow the recursive method further on
		
		for (Processor subProcessor : getSubProcessorIntakes().keySet()) { // split the units into sub units and send them to subprocessors if any
			Number intake = getSubProcessorIntakes().get(subProcessor);
			for (ProcessUnit inputUnit : inputUnits) {
				coll.addAll(subProcessor.doProcess(subProcessor.createProcessUnits(inputUnit, intake)));
			}
		}
		if (!hasSubProcessors()) {
			coll.addAll(inputUnits);
		}
		return coll;
	}
	
	@SuppressWarnings("rawtypes")
	protected final List<ProcessUnit> createProcessUnits(ProcessUnit inputUnit, Number intake) {
		if (inputUnit instanceof TestProcessUnit) {
			List<ProcessUnit> outputUnits = new ArrayList<ProcessUnit>();
//			outputUnits.add(new TestProcessUnit(((TestProcessUnit) inputUnit).processorList));
			outputUnits.add(((TestProcessUnit) inputUnit).createNewProcessUnitFromThisOne());
			return outputUnits;
		} else {
			return createProcessUnitsFromThisProcessor(inputUnit, intake);
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List<ProcessUnit> createProcessUnitsFromThisProcessor(ProcessUnit inputUnit, Number intake) {
		List<ProcessUnit> outputUnits = new ArrayList<ProcessUnit>();
		ProcessUnit unit = new ProcessUnit(inputUnit.getAmountMap().multiplyByAScalar(intake.doubleValue() * .01));
		outputUnits.add(unit);
		return outputUnits;
	}
	
	/**
	 * This method adds a process to the sub processes. 
	 * @param processor a Process instance
	 */
	public void addSubProcessor(Processor processor) {
		if (!subProcessors.contains(processor)) {
			subProcessors.add(processor);
			int intake = (int) (processor.averageIntake * 100);
			getSubProcessorIntakes().put(processor, intake);
		}
	}
	
	/**
	 * This method removes a process from the sub processes. 
	 * @param processor a Process instance
	 */
	public void removeSubProcessor(Processor processor) {
		subProcessors.remove(processor);
		getSubProcessorIntakes().remove(processor);
	}
	
	/**
	 * This method returns the processes that are subordinated to this one.
	 * @return a List of Processor instances.
	 */
	public List<Processor> getSubProcessors() {return subProcessors;}

	/**
	 * This method returns the processor intakes in a map.
	 * @return a Map with Processor instances and Integer from 0 to 100 as keys and values
	 */
	public Map<Processor, Number> getSubProcessorIntakes() {
		if (subProcessorIntakes == null) {
			subProcessorIntakes = new HashMap<Processor, Number>();
			for (Processor processor : subProcessors) {		// if so, means we are dealing with the former implementation
				int intake = (int) (processor.averageIntake * 100);
				subProcessorIntakes.put(processor, intake);
			}
		}
		return subProcessorIntakes;
	}

	/**
	 * Provide the original location (x,y) of the processor.<p>
	 * Here the zoom factor is assumed to be 1. The Point instance
	 * returned by this method is a copy.
	 * @return a Point instance
	 */
	public final Point getOriginalLocation() {
		return originalLocation == null ? null : new Point(originalLocation);
	}

	/**
	 * Provide the location of the Processor instance in the SystemPanel. <p>
	 * The original location is converted to relative (to the zoom factor).
	 * @return a Point instance
	 */
	public final Point getLocation() {
		return getOriginalLocation() == null ? null : SystemLayout.convertOriginalToRelative(getOriginalLocation());
	}
	
	
	
	/**
	 * Set the original location in the Processor instance. <p>
	 * The argument might be given as a relative point if the zoom factor is .5 for instance.
	 * This relative point is transformed in order to obtain the absolute point as if the zoom factor 
	 * was 1.
	 * @param relativePoint a Point instance
	 */
	public final void setOriginalLocation(Point relativePoint) {
		if (relativePoint == null) {
			originalLocation = relativePoint;
		} else {
			this.originalLocation = SystemLayout.convertRelativeToOriginal(relativePoint);
		}
	}
		
	@Override
	public ProcessorButton getUI(Container container) {
		if (guiInterface == null) {
			guiInterface = new ProcessorButton((SystemPanel) container, this);
		}
		return guiInterface;
	}

	/**
	 * This method returns the name of the processor.
	 * @return a String
	 */
	public String getName() {return name;}
	
	/**
	 * This method sets the name of the processor.
	 * @param str a String instance
	 */
	public void setName(String str) {name = str;}
	
	
	@Override 
	public String toString() {return getName();}

	@Override
	public ProcessorButton getUI() {
		if (guiInterface == null) {
			throw new NullPointerException("The getGuiInterface(Container) should be called first!");
		}
		return guiInterface;
	}

	@Override
	public void caretUpdate(CaretEvent evt) {
		if (evt.getSource() instanceof JTextField) {
			JTextField txtField = (JTextField) evt.getSource();
			String newLabel = txtField.getText();
			if (!newLabel.equals(getName())) {
				getUI().setChanged(true);
				setName(newLabel);
				getUI().setLabel();
			}
		}
	}
	
	/**
	 * This method returns true if the processor has one or many subprocessors tied to itself or false otherwise.
	 * @return a boolean
	 */
	public boolean hasSubProcessors() {return !getSubProcessors().isEmpty();}

	/**
	 * This method checks if the wood processor is valid as well as all its subprocessors.
	 * @return true if everything is alright
	 */
	public boolean isValid() {
		if (hasSubProcessors()) {
			double sum = 0;
			for (Number intake : getSubProcessorIntakes().values()) {
				sum += intake.doubleValue();
			}
			if (sum != 100d) {
			}
			boolean isValid = Math.abs(sum - 100d) < 1E-12;
			return isValid;
		} else {
			return true;
		}
	}
	
	public boolean isPartOfEndlessLoop() {return this.markedAsPartOfEndlessLoop;}
	
	/**
	 * This method returns other process features when the processor button is double clicked.
	 * @return a REpiceaPanel instance
	 */
	protected REpiceaPanel getProcessFeaturesPanel() {return null;}
	
	@Override
	public boolean isVisible() {
		return guiInterface != null && guiInterface.isVisible();
	}

	protected void setPartOfEndlessLoop(boolean bool) {
		this.markedAsPartOfEndlessLoop = bool;
	}

	@Override
	public List<MemberInformation> getInformationsOnMembers() {
		List<MemberInformation> cellValues = new ArrayList<MemberInformation>();
		cellValues.add(new MemberInformation(MemberLabel.NameField, String.class, name));
		return cellValues;
	}
	
	@Override
	public void processChangeToMember(Enum<?> label, Object value) {
		if (label == MemberLabel.NameField) {
			setName(value.toString());
		}
	}

	@Override
	public void releaseResources() {
		if (guiInterface != null) {
			guiInterface.releaseResources();
		}
	}

}
