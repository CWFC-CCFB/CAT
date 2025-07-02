/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA 
 * Copyright (C) 2020-2023 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service
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

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureRecognizer;
import java.awt.dnd.DragSource;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.InvalidParameterException;

import javax.swing.JMenuItem;

import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager.EnhancedMode;
import repicea.gui.CommonGuiUtility;
import repicea.gui.Refreshable;
import repicea.gui.dnd.LocatedEvent;
import repicea.gui.popup.REpiceaPopupListener;
import repicea.gui.popup.REpiceaPopupMenu;
import repicea.simulation.processsystem.DragGestureCreateLinkListener;
import repicea.simulation.processsystem.PreProcessorLinkLine;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.SystemPanel;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

@SuppressWarnings("serial")
public class ProductionLineProcessorButton extends AbstractProcessorButton implements ActionListener {

	protected class DragGestureCreateEndOfLifeLinkListener extends DragGestureCreateLinkListener {

		protected DragGestureCreateEndOfLifeLinkListener(ProcessorButton button) {
			super(button);
		}
		
		@Override
		protected PreProcessorLinkLine instantiatePreLink(SystemPanel panel) {
			return new PreEndOfLifeLinkLine(panel, button);
		}
		
		
	}
	
	
	protected class ExtractionPopupMenu extends REpiceaPopupMenu implements Refreshable {

		ExtractionPopupMenu() {
			super(ProductionLineProcessorButton.this, 
					ProductionLineProcessorButton.this.addDebarkerItem,
					ProductionLineProcessorButton.this.removeDebarkerItem,
					ProductionLineProcessorButton.this.addStatusSpeciesTypeSortingItem,
					ProductionLineProcessorButton.this.removeStatusSpeciesTypeSortingItem);
		}

		@Override
		public void refreshInterface() {
			ProductionLineProcessor owner = (ProductionLineProcessor) ProductionLineProcessorButton.this.getOwner();
			ProductionLineProcessorButton.this.addDebarkerItem.setEnabled(owner.getExtractionProcessor() == null);
			ProductionLineProcessorButton.this.removeDebarkerItem.setEnabled(owner.getExtractionProcessor() instanceof BarkExtractionProcessor);
			ProductionLineProcessorButton.this.addStatusSpeciesTypeSortingItem.setEnabled(owner.getExtractionProcessor() == null);
			ProductionLineProcessorButton.this.removeStatusSpeciesTypeSortingItem.setEnabled(owner.getExtractionProcessor() instanceof StatusSpeciesTypeExtractionProcessor);
		}
			
		
		@Override
		public void setVisible(boolean bool) {
			refreshInterface();
			super.setVisible(bool);
		}
		
	}
	
	
	
	private static enum MessageID implements TextableEnum {
		AddDebarkerMessage("Add debarking", "Ajouter \u00E9cor\u00E7age"),
		RemoveDebarkerMessage("Remove debarking", "Eliminer \u00E9cor\u00E7age"),
		AddStatusSpeciesTypeSortingMessage("Add species type or status sorting", "Ajouter tri par \u00E9tat ou type d'esp\u00E8ces"),
		RemoveStatusSpeciesTypeSortingMessage("Remove species type or status sorting", "Eliminer tri par \u00E9tat ou type d'esp\u00E8ces");

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


	protected final DragGestureRecognizer createEndOfLifeLinkRecognizer;
	
	final JMenuItem addDebarkerItem;
	final JMenuItem removeDebarkerItem;
	final JMenuItem addStatusSpeciesTypeSortingItem;
	final JMenuItem removeStatusSpeciesTypeSortingItem;
	
	
	
	/**
	 * Constructor.
	 * @param panel a SystemPanel instance
	 * @param process the Processor instance that owns this button
	 */
	protected ProductionLineProcessorButton(SystemPanel panel, AbstractProductionLineProcessor process) {
		super(panel, process);
		DragSource ds = new DragSource();
		createEndOfLifeLinkRecognizer = ds.createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY, new DragGestureCreateEndOfLifeLinkListener(this));
		createEndOfLifeLinkRecognizer.setComponent(null);
		addDebarkerItem = new JMenuItem(MessageID.AddDebarkerMessage.toString());
		removeDebarkerItem = new JMenuItem(MessageID.RemoveDebarkerMessage.toString());
		addStatusSpeciesTypeSortingItem = new JMenuItem(MessageID.AddStatusSpeciesTypeSortingMessage.toString());
		removeStatusSpeciesTypeSortingItem = new JMenuItem(MessageID.RemoveStatusSpeciesTypeSortingMessage.toString());
		setExtractionPopupMenu();
	}
	
	void setExtractionPopupMenu() {
		ExtractionPopupMenu forkOperationPopupMenu = new ExtractionPopupMenu();
		addMouseListener(new REpiceaPopupListener(forkOperationPopupMenu));
	}
	
	
	@SuppressWarnings("rawtypes")
	protected void setDragMode(Enum mode) {
		super.setDragMode(mode);
		createEndOfLifeLinkRecognizer.setComponent(null);
		boolean isDisposed = false;
		if (getOwner() instanceof ProductionLineProcessor) {
			ProductionLineProcessor processor = (ProductionLineProcessor) getOwner();
			isDisposed = processor.getDisposedToProcess() != null;
		}
		if (isDisposed) {
			createLinkRecognizer.setComponent(null);	// disable the drag & drop
		} else {
			if (mode == EnhancedMode.CreateEndOfLifeLinkLine) {
				if (!getOwner().hasSubProcessors() && !getOwner().isTerminalProcessor()) {
					createEndOfLifeLinkRecognizer.setComponent(this);
				}
			}
		}
	}
	
	@Override
	public void paint(Graphics g) {
		if (!getOwner().hasSubProcessors() && getOwner() instanceof ProductionLineProcessor) {
			setBorderColor(Color.BLUE);
			if (!isSelected()) {
				setBorderWidth(2);
			}
		} else {
			setBorderColor(Color.BLACK);
		}
		super.paint(g);
	}

	private enum ExtractionType {
		Debarking(BarkExtractionProcessor.class), 
		StatusSpeciesTypeSorting(StatusSpeciesTypeExtractionProcessor.class);
		
		final Class<? extends AbstractExtractionProcessor> clazz;
		
		ExtractionType(Class<? extends AbstractExtractionProcessor> clazz) {
			this.clazz = clazz;
		}
	}
		
	private void addExtractionProcessor(ExtractionType type) {
		ProductionLineProcessor parentProcessor = (ProductionLineProcessor) getOwner();
		Point upperStreamProcessorLocation = parentProcessor.getLocation();
		Point newLocation = new Point(upperStreamProcessorLocation.x + 50, upperStreamProcessorLocation.y - 50);
		AbstractExtractionProcessor extProc = createExtractionProcessor(type);
		LocatedEvent evt = new LocatedEvent(this, newLocation);
		ExtendedSystemPanel panel = (ExtendedSystemPanel) CommonGuiUtility.getParentComponent(this, SystemPanel.class);
		panel.addLinkLine(new ExtractionLinkLine(panel, parentProcessor, extProc));
		panel.acceptThisObject(extProc, evt);
	}

	private AbstractExtractionProcessor createExtractionProcessor(ExtractionType type) {
		switch(type) {
		case Debarking:
			return new BarkExtractionProcessor();
		case StatusSpeciesTypeSorting:
			return new StatusSpeciesTypeExtractionProcessor();
		default:
			throw new InvalidParameterException("Extraction type " + type.name() + " has not been implemented yet!" );
		}
	}

	private void removeExtractionProcessor(ExtractionType type) {
		ProductionLineProcessor parentProcessor = (ProductionLineProcessor) getOwner();
		if (parentProcessor.getExtractionProcessor() != null && 
				type.clazz.isAssignableFrom(parentProcessor.getExtractionProcessor().getClass())) {
			AbstractExtractionProcessor forkProcessor = parentProcessor.getExtractionProcessor();
			ExtendedSystemPanel panel = (ExtendedSystemPanel) CommonGuiUtility.getParentComponent(this, SystemPanel.class);
			panel.deleteFeature(forkProcessor.getUI());
		}

	}
	
	@Override
	public void actionPerformed(ActionEvent arg0) {
		if (arg0.getSource().equals(addDebarkerItem)) {
			addExtractionProcessor(ExtractionType.Debarking);
		} else if (arg0.getSource().equals(removeDebarkerItem)) {
			removeExtractionProcessor(ExtractionType.Debarking);
		} else if (arg0.getSource().equals(this.addStatusSpeciesTypeSortingItem)) {
			addExtractionProcessor(ExtractionType.StatusSpeciesTypeSorting);
		} else if (arg0.getSource().equals(removeStatusSpeciesTypeSortingItem)) {
			removeExtractionProcessor(ExtractionType.StatusSpeciesTypeSorting);
		} 
	}

	
}
