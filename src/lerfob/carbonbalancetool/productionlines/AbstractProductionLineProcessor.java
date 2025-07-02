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

import java.util.List;

import repicea.gui.REpiceaPanel;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.ProcessorListTable.MemberInformation;
import repicea.simulation.processsystem.ResourceReleasable;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * An abstract for all Processor-derived classes (except the LogCategoryProcessor), which ensures
 * the Processor-derived class contains a CarbonUnitFeature instance and that this CarbonUnitFeature
 * instance can be shown in the GUI.
 * @author Mathieu Fortin - May 2014
 */
@SuppressWarnings("serial")
public abstract class AbstractProductionLineProcessor extends AbstractProcessor implements ResourceReleasable {

	protected static enum MemberLabel implements TextableEnum {
		FunctionUnitBiomass("Functional unit (FU) biomass (Mg)", "Biomasse de l'unit\u00E9 fonctionnelle (UF, Mg)"),
		EmissionFunctionUnit("Emissions (Mg CO2 eq./FU)", "Emissions (Mg CO2 eq./UF)");

		MemberLabel(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
		
	}
	
	protected CarbonUnitFeature woodProductFeature;

	protected CarbonUnitFeature getEndProductFeature() {return woodProductFeature;}

	/**
	 * Regular constructor for GUI.
	 */
	protected AbstractProductionLineProcessor() {}
	
	@Override
	protected REpiceaPanel getProcessFeaturesPanel() {
		return getEndProductFeature().getUI();
	}

	/*
	 * For extended visibility (non-Javadoc)
	 * @see repicea.simulation.processsystem.Processor#createProcessUnitsFromThisProcessor(repicea.simulation.processsystem.ProcessUnit, int)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	protected abstract List<ProcessUnit> createProcessUnitsFromThisProcessor(ProcessUnit unit, Number intake);
	
	@Override
	public List<MemberInformation> getInformationsOnMembers() {
		List<MemberInformation> cellValues = super.getInformationsOnMembers();
		if (usesEmissionsAndFunctionalUnitFromAbstractProcessorClass()) {
			cellValues.add(new MemberInformation(MemberLabel.FunctionUnitBiomass, double.class, functionUnitBiomass));
			cellValues.add(new MemberInformation(MemberLabel.EmissionFunctionUnit, double.class, emissionsByFunctionalUnit));
			cellValues.add(new MemberInformation(EnhancedProcessorInternalDialog.MessageID.SourceLabel, String.class, getSourceInfo()));
		} 
		if (mustDisplaySpecificAdditionalFeatures()) {
			cellValues.addAll(woodProductFeature.getInformationsOnMembers());
		}
		return cellValues;
	}

	@Override
	public void processChangeToMember(Enum<?> label, Object value) {
		if (label == MemberLabel.FunctionUnitBiomass) {
			if (usesEmissionsAndFunctionalUnitFromAbstractProcessorClass()) {
				functionUnitBiomass = (double) value;
			} else {
				getEndProductFeature().processChangeToMember(label, value);
			}
		} else if (label == MemberLabel.EmissionFunctionUnit) {
			if (usesEmissionsAndFunctionalUnitFromAbstractProcessorClass()) {
				emissionsByFunctionalUnit = (double) value;
			} else {
				getEndProductFeature().processChangeToMember(label, value);
			}
		} else if (label == EnhancedProcessorInternalDialog.MessageID.SourceLabel) {
			if (usesEmissionsAndFunctionalUnitFromAbstractProcessorClass() && !(this instanceof LandfillProcessor)) {
				sourceInfo = value.toString();
			} else {
				getEndProductFeature().processChangeToMember(label, value);
			}
		} 
		else {
			woodProductFeature.processChangeToMember(label, value);
			super.processChangeToMember(label, value);
		}
	}
	
	@Override
	public void releaseResources() {
		super.releaseResources();
		if (woodProductFeature != null) {
			woodProductFeature.releaseResources();
		}
	}
	
}
