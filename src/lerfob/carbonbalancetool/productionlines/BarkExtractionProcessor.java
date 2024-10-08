/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2020 Her Majesty the Queen in Right of Canada
 * Author: Mathieu Fortin for Canadian Forest Service
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

import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.SystemPanel;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * Special processor for extracting bark from wood pieces.
 * @author Mathieu Fortin - 2020
 */
@SuppressWarnings("serial")
public class BarkExtractionProcessor extends AbstractExtractionProcessor {

	private enum MessageID implements TextableEnum {

		Debarking("Debarking", "Ecor\u00E7age");

		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}

		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
	}
	
	
	
	protected BarkExtractionProcessor() {
		super();
		setName(MessageID.Debarking.toString());		// default name
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected List<ProcessUnit> extract(List<ProcessUnit> processUnits) {
		List<ProcessUnit> extractedUnits = new ArrayList<ProcessUnit>();
		List<ProcessUnit> copyList = new ArrayList<ProcessUnit>();
		copyList.addAll(processUnits);
		for (ProcessUnit p : copyList) {
			if (p instanceof BiomassTypeProvider) {
				if (((BiomassTypeProvider) p).getBiomassType() == BiomassType.Bark) {
					try {
						extractedUnits.add(p);
						processUnits.remove(p);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
		return extractedUnits;
	}





}
