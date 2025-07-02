/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2020 Her Majesty the Queen in right of Canada,
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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Stroke;

import repicea.simulation.processsystem.SystemPanel;
import repicea.simulation.processsystem.ValidProcessorLinkLine;

/**
 * The ExtractionLinkLine class represents a link to a processor that is run before splitting
 * the ElementUnit instance. A typical example would be that of debarking where the bark is treated
 * in a different manner than the wood.
 * @author Mathieu Fortin - September 2020
 */
@SuppressWarnings("serial")
public class ExtractionLinkLine extends ValidProcessorLinkLine {

	protected static final Stroke ExtractionLinkLineStrokeDefault = new BasicStroke(2, 
			BasicStroke.CAP_ROUND, 
			BasicStroke.JOIN_MITER,
			1, 
			new float[]{5,3,2,3},
			0);

	
	
	protected ExtractionLinkLine(SystemPanel panel, ProductionLineProcessor fatherProcessor, AbstractExtractionProcessor sonProcessor) {
		super(panel, fatherProcessor.getUI(panel), sonProcessor.getUI(panel));
		setAnchorPositions(AnchorPosition.TOP, AnchorPosition.LEFT);

		((ProductionLineProcessor) fatherProcessor).setExtractionProcessor(sonProcessor);
		sonProcessor.getUI(panel).addComponentListener(this);
		setBackground(Color.LIGHT_GRAY);
		setBorderPainted(false);
	}

	@Override
	public void setSelected(boolean bool) {}		// the link cannot be selected and deleted
	
	@Override
	protected void finalize() {
		super.finalize();
		ProductionLineProcessor fatherProcessor =  (ProductionLineProcessor) getFatherAnchor().getOwner();
		fatherProcessor.setExtractionProcessor(null);
	}

	@Override
	protected void setStroke(Graphics2D g2) {
		g2.setColor(Color.MAGENTA);
		g2.setStroke(ExtractionLinkLineStrokeDefault);
	}

	@Override
	protected boolean shouldChangeBeRecorded() {return false;}	// No need to record the change here 
																// because the link is to be deleted 
																// at the same than the process when 
																// pressing CTRL-Z. MF2020-09-21
}
