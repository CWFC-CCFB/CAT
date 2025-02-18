/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2012 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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


import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import lerfob.carbonbalancetool.CATCompartmentManager;
import lerfob.carbonbalancetool.productionlines.DecayFunction.LifetimeMode;
import lerfob.carbonbalancetool.productionlines.combustion.CombustionEmissions;
import lerfob.carbonbalancetool.productionlines.combustion.CombustionEmissions.CombustionProcess;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldDocument.NumberFieldEvent;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldListener;
import repicea.simulation.processsystem.ProcessorListTable.MemberInformation;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;


/**
 * An EndProductFeature instance contains four elements : a use class, a lifecycle analysis, a lifetime duration, and a substitution factor.
 * @author M. Fortin - October 2010
 */
public class EndUseWoodProductCarbonUnitFeature extends CarbonUnitFeature implements ChangeListener, 
																					ItemListener,
																					NumberFieldListener {

	private static final long serialVersionUID = 20101020L;
	

	protected static enum MemberLabel implements TextableEnum {
		UseClass("Use class", "Usage"),
		Substitution("Substitution (Mg CO2 eq./FU)","Substitution (Mg CO2 eq./UF)");

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
	
	
	
	public static enum UseClass implements TextableEnum {
		NONE("Industrial loss", "Perte industrielle", true), 
		ENERGY("Energy wood", "Bois \u00E9nergie", true), 
		PAPER("Paper", "Papier", false), 
		WRAPPING("Packages", "Emballages", false), 
		FURNITURE("Furniture", "Ameublement", false), 
		BARREL("Staves", "Tonnellerie", false), 
		BUILDING("Building", "Construction", false),
		FIREWOOD("Fire wood", "Bois de feu", true),
		RESIDUALS_FOR_ENERGY("Residues energy", "R\u00E9sidus pour \u00E9nergie", true),
		BRANCHES_FOR_ENERGY("Branches", "Menus bois", true),
		STUMPS_FOR_ENERGY("Stumps", "Souches", true),
		EXTRACTIVE("Wood extractives", "Extractibles du bois", false),
		FUEL("Biofuel", "Biocarburant", false);

		private final boolean meantForEnergyProduction;
		
		UseClass(String englishText, String frenchText, boolean meantForEnergyProduction) {
			setText(englishText, frenchText);
			this.meantForEnergyProduction = meantForEnergyProduction;
		}
		
		public boolean isMeantForEnergyProduction() {return meantForEnergyProduction;}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}

		@Override
		public String toString() {
			return REpiceaTranslator.getString(this);
		}
	}
	

	@Deprecated
	private boolean disposable;
	@Deprecated
	private double disposableProportion;
	private UseClass useClass;
	
	private CombustionProcess combustionProcess;
	
	/**
	 * The average C to m3 of raw material substitution ratio.
	 */
	@Deprecated
	private double averageSubstitution;
	
	private double relativeSubstitutionCO2EqFonctionalUnit;
	
	private double biomassOfFunctionalUnit;		// in Mg
	private double emissionsByFunctionalUnit;	// in Mg
	

	@Deprecated
	private LifeCycleAnalysis lca;

	
	/**
	 * Constructor in GUI mode.
	 * @param processor a ProductionlineProcessor instance
	 */
	protected EndUseWoodProductCarbonUnitFeature(ProductionLineProcessor processor) {
		super(processor);
		useClass = UseClass.NONE;
	}
	
	protected CombustionProcess getCombustionProcess() {return combustionProcess;}
	
	protected double getEmissionsMgCO2EqByFunctionalUnit() {return emissionsByFunctionalUnit;}
	
	protected UseClass getUseClass() {
		if (useClass == null) {
			useClass = UseClass.NONE;
		}
		return this.useClass;
	}
	
	protected void setUseClass(UseClass useClass) {this.useClass = useClass;}

	@Override
	public EndUseWoodProductCarbonUnitFeaturePanel getUI() {
		if (getUserInterfacePanel() == null) {
			setUserInterfacePanel(new EndUseWoodProductCarbonUnitFeaturePanel(this));
		}
		return getUserInterfacePanel();
	}

	protected double getSubstitutionMgCO2EqByFunctionalUnit(CATCompartmentManager manager) {
		if (manager != null) {
			return relativeSubstitutionCO2EqFonctionalUnit * CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.SubstitutionFactors, manager, toString());
		} else {
			return relativeSubstitutionCO2EqFonctionalUnit;
		}
	}
	
	protected double getBiomassOfFunctionalUnitMg() {
		return biomassOfFunctionalUnit;
	}
	
	@Deprecated
	protected double getAverageSubstitution() {
		return averageSubstitution;
	}
	
	@Deprecated
	protected void setAverageSubstitution(double d) {averageSubstitution = d;}
	
	/**
	 * This method returns true if the lost carbon can be sent to the landfill site.
	 * @return a boolean
	 */
	protected boolean isDisposed() {
		boolean isDisposed = disposable || // former implementation
				((ProductionLineProcessor) getProcessor()).getDisposedToProcess() != null; // new implementation
		return isDisposed;
	}
	
	@Deprecated
	protected void setDisposable(boolean disposable) {this.disposable = disposable;}
	
	@Deprecated
	protected double getDisposableProportion() {
		if (((ProductionLineProcessor) getProcessor()).getDisposedToProcess() != null) {		// new implementation
			return 1;
		} else {
			return disposableProportion;		// former implementation
		}
	}
	@Deprecated
	protected void setDisposableProportion(double disposableProportion) {this.disposableProportion = disposableProportion;} 
	
	/**
	 * Returns the combustion emission factors in CO2 eq. for one Mg of dry biomass.
	 * @return a double
	 */
	protected double getCombustionEmissionFactorsInCO2Eq() {
		if (getCombustionProcess() != null && getCombustionProcess() != CombustionProcess.None) {
			return CombustionEmissions.CombustionEmissionsMap.get(getCombustionProcess()).getEmissionFactorInCO2EqForOneMgOfDryBiomass();
		} else {
			return 0d;
		}
	}
	
	/**
	 * Returns the heat production in MgWh for one Mg of dry biomass.
	 * @return a double
	 */
	protected double getHeatProductionMgWh() {
		if (getCombustionProcess() != null && getCombustionProcess() != CombustionProcess.None) {
			return CombustionEmissions.CombustionEmissionsMap.get(getCombustionProcess()).getHeatProductionInMgWhForOneMgOfDryBiomass();
		} else {
			return 0d;
		}
	}
	
	/*
	 * Called when the document in the averageSubstitutionTextField member is updated.
	 */
	@Override
	public void numberChanged(NumberFieldEvent e) {
		if (e.getSource().equals(getUI().substitutionTextField)) {
			double value = Double.parseDouble(getUI().substitutionTextField.getText());
			if (value != relativeSubstitutionCO2EqFonctionalUnit) {
				((AbstractProcessorButton) getProcessor().getUI()).setChanged(true);
				relativeSubstitutionCO2EqFonctionalUnit = value;
			}
		} else if (e.getSource().equals(getUI().biomassFUTextField)) {
			double value = Double.parseDouble(getUI().biomassFUTextField.getText());
			if (value != biomassOfFunctionalUnit) {
				((AbstractProcessorButton) getProcessor().getUI()).setChanged(true);
				biomassOfFunctionalUnit = value;
			}
		} else if (e.getSource().equals(getUI().emissionsByFUField)) {
			double value = Double.parseDouble(getUI().emissionsByFUField.getText());
			if (value != emissionsByFunctionalUnit) {
				((AbstractProcessorButton) getProcessor().getUI()).setChanged(true);
				emissionsByFunctionalUnit = value;
			}
		}
	}


	@Override
	public void stateChanged(ChangeEvent evt) {
		if (evt.getSource().equals(getUserInterfacePanel().disposableProportionSlider)) {
			double factor = (double) 1 / getUserInterfacePanel().disposableProportionSlider.getMaximum();
			disposableProportion = getUserInterfacePanel().disposableProportionSlider.getValue() * factor;
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void itemStateChanged(ItemEvent evt) {
		if (evt.getSource() instanceof JComboBox) {
			Object obj = ((JComboBox) evt.getSource()).getSelectedItem();
			if (obj instanceof UseClass) {
				UseClass newUseClass = (UseClass) obj;
				if (newUseClass != useClass) {
					((AbstractProcessorButton) getProcessor().getUI()).setChanged(true);
					useClass = newUseClass;
				}
			} else if (obj instanceof CombustionProcess) {
				CombustionProcess newCombustionProcess = (CombustionProcess) obj;
				if (newCombustionProcess != combustionProcess) {
					((AbstractProcessorButton) getProcessor().getUI()).setChanged(true);
					combustionProcess = newCombustionProcess;
				}
			}
		} else if (evt.getSource().equals(getUserInterfacePanel().isDisposableCheckBox)) {
			if (getUserInterfacePanel().isEnabled()) {
				disposable = getUserInterfacePanel().isDisposableCheckBox.isSelected();
			}
		}
	}
	
	@Override
	protected EndUseWoodProductCarbonUnitFeaturePanel getUserInterfacePanel() {
		if (super.getUserInterfacePanel() != null) {
			return (EndUseWoodProductCarbonUnitFeaturePanel) super.getUserInterfacePanel();
		} else {
			return null;
		}
	}
	
	@Deprecated
	protected LifeCycleAnalysis getLCA() {return lca;}
	
	@Deprecated
	protected void setLCA(LifeCycleAnalysis lca) {
		this.lca = lca;
	}

	
	@Override
	public List<MemberInformation> getInformationsOnMembers() {
		List<MemberInformation> memberInfo = super.getInformationsOnMembers();
		memberInfo.add(new MemberInformation(AbstractProductionLineProcessor.MemberLabel.FunctionUnitBiomass, double.class, getBiomassOfFunctionalUnitMg()));
		memberInfo.add(new MemberInformation(AbstractProductionLineProcessor.MemberLabel.EmissionFunctionUnit, double.class, getEmissionsMgCO2EqByFunctionalUnit()));
		memberInfo.add(new MemberInformation(MemberLabel.UseClass, UseClass.class, getUseClass()));
		memberInfo.add(new MemberInformation(MemberLabel.Substitution, double.class, relativeSubstitutionCO2EqFonctionalUnit));
		return memberInfo;
	}

	@Override
	public void processChangeToMember(Enum<?> label, Object value) {
		if (label == AbstractProductionLineProcessor.MemberLabel.FunctionUnitBiomass) {
			biomassOfFunctionalUnit = (double) value;
		} else if (label == AbstractProductionLineProcessor.MemberLabel.EmissionFunctionUnit) {
			emissionsByFunctionalUnit = (double) value;
		} else if (label == MemberLabel.UseClass) {
			setUseClass((UseClass) value);
		} else if (label == MemberLabel.Substitution) {
			relativeSubstitutionCO2EqFonctionalUnit = (double) value;
		} else {
			super.processChangeToMember(label, value);
		}
	}

	void updateFeature(EndUseProductDefaultFeature feature) {
		setUseClass(feature.useClass);
		getDecayFunction().functionType = feature.decayFunctionType;
		getDecayFunction().lifetimeMode = feature.lifetimeMode;
		if (feature.lifetimeMode == LifetimeMode.AVERAGE) {
			getDecayFunction().setAverageLifetimeYr(feature.lifetimeYr);
		} else if (feature.lifetimeMode == LifetimeMode.HALFLIFE) {
			getDecayFunction().setHalfLifeYr(feature.lifetimeYr);
		}
	}
	
}
