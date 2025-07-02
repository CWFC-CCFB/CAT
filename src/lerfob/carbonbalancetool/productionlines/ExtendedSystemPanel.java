/*
 * This file is part of the CAT library.
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

import javax.swing.AbstractButton;

import repicea.gui.REpiceaUIObject;
import repicea.simulation.processsystem.SystemLayout;
import repicea.simulation.processsystem.SystemManager;
import repicea.simulation.processsystem.SystemPanel;
import repicea.simulation.processsystem.ValidProcessorLinkLine;

@SuppressWarnings("serial")
public class ExtendedSystemPanel extends SystemPanel {

	protected ExtendedSystemPanel(SystemManager manager, SystemLayout systemLayout) {
		super(manager, systemLayout);
	}

	@Override
	protected void addManagerComponents() {
		super.addManagerComponents();
		for (REpiceaUIObject obj : getListManager().getList()) {
			if (obj instanceof ProductionLineProcessor) {
				ProductionLineProcessor process = (ProductionLineProcessor) obj;
				if (process.getDisposedToProcess() != null) {
					addLinkLine(new EndOfLifeLinkLine(this, process, process.getDisposedToProcess()));
				}
				if (process.getExtractionProcessor() != null)
					addLinkLine(new ExtractionLinkLine(this, process, process.getExtractionProcessor()));	
			}
		}
	}

	/*
	 * For extended visibility.
	 */
	@Override
	protected void addLinkLine(ValidProcessorLinkLine linkLine) {
		super.addLinkLine(linkLine);
	}

	/*
	 * For extended visibility.
	 */
	@Override
	protected void deleteFeature(AbstractButton button) {
		super.deleteFeature(button);
	}
}
