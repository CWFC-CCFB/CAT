/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2012 Mathieu Fortin for LERFOB INRA/AgroParisTech
 * Copyright (C) 2023 His Majesty the King in Right of Canada
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

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import repicea.gui.REpiceaUIObject;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldDocument.NumberFieldEvent;
import repicea.gui.components.NumberFormatFieldFactory.NumberFieldListener;
import repicea.math.utility.GammaUtility;
import repicea.simulation.MonteCarloSimulationCompliantObject;
import repicea.simulation.processsystem.ProcessorListTable.MemberHandler;
import repicea.simulation.processsystem.ProcessorListTable.MemberInformation;
import repicea.simulation.processsystem.ResourceReleasable;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The decay function interface is designated for lifetime decreasing functions. This interface serves 
 * to actualize the EndProduct instance throughout their useful lifetime. 
 * @author Mathieu Fortin - October 2010
 */
@SuppressWarnings("serial")
class DecayFunction implements Serializable, REpiceaUIObject, NumberFieldListener, ItemListener, MemberHandler, ResourceReleasable {

	protected static enum MemberLabel implements TextableEnum {
		DecayFunctionType("Decay function type", "Type de d\u00E9croissance"),
		LifetimeMode("Lifetime type", "Type de dur\u00E9e de vie"),
		Lifetime("Lifetime (yr)", "Dur\u00E9e de vie");

		MemberLabel(String englishString, String frenchString) {
			setText(englishString, frenchString);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
		
	}
	

	/**
	 * Enum associated to the implementation of CATDecayFunction.
	 * @author Mathieu Fortin - December 2023
	 */
	static enum DecayFunctionType {
		Exponential,
		Weibull;
	}
	
	static enum LifetimeMode implements TextableEnum {
		HALFLIFE("Half-life (yr)", "Demi-vie (ann\u00E9es)"),
		AVERAGE("Average lifetime (yr)", "Dur\u00E9e de vie moyenne (ann\u00E9es)"); 

		LifetimeMode(String englishText, String frenchText) {
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
	
	private static final double AverageLifeTimeToHalfLifeExponential = Math.log(2d);

	double averageLifetimeYr;
	double halfLifeYr;
	private double weibullBeta = 5d;
	double weibullLambda;
	private transient DecayFunctionPanel userInterface;
	
	protected DecayFunctionType functionType;
	protected LifetimeMode lifetimeMode;
	protected final CarbonUnitFeature feature;
	
	DecayFunction(CarbonUnitFeature feature, LifetimeMode lifetimeMode, DecayFunctionType functionType, double lifetimeYrValue) {
		this.feature = feature;
		this.functionType = functionType;
		this.lifetimeMode = lifetimeMode;
		setLifetime(lifetimeYrValue);
	}
	
	private void setLifetime(double lifetimeYrValue) {
		switch(lifetimeMode) {
		case HALFLIFE:
			setHalfLifeYr(lifetimeYrValue);
			break;
		case AVERAGE:
			setAverageLifetimeYr(lifetimeYrValue);
			break;
		default:
			throw new InvalidParameterException("Lifetime mode " + lifetimeMode.name() + " has not been implemeted yet!");
		}
	}
	
	/**
	 * Provide the value of the infinite integral of the decay function. <p>
	 * This is the average lifetime.
	 * @return the value of the infinite integral (double)
	 */
	double getInfiniteIntegral() {return averageLifetimeYr;}

	void setAverageLifetimeYr(double averageLifetimeYr) {
		if (averageLifetimeYr < 0) {
			throw new InvalidParameterException("The averageLifetimeYr argument must be greater than or equal to 0!");
		}
		setInternalAverageLifetimeYr(averageLifetimeYr);
		this.halfLifeYr = convertAverageLifetimeToHalfLife(averageLifetimeYr);
//		System.out.println(this);
	}
	
	private void setInternalAverageLifetimeYr(double averageLifetimeYr) {
		this.averageLifetimeYr = averageLifetimeYr;
//		this.weibullLambda = averageLifetimeYr / GammaUtility.gamma(1d + 1d / weibullBeta);
		this.weibullLambda = getWeibullLambdaFromAverageLifetime(averageLifetimeYr);
	}
	
	private double getWeibullLambdaFromAverageLifetime(double avgLifetimeYr) {
		return avgLifetimeYr / GammaUtility.gamma(1d + 1d / weibullBeta);
	}
	
	private double convertAverageLifetimeToHalfLife(double averageLifetimeYr) {
		switch(functionType) {
		case Exponential:
			return averageLifetimeYr * AverageLifeTimeToHalfLifeExponential;
		case Weibull:
			return weibullLambda * Math.pow(AverageLifeTimeToHalfLifeExponential, 1d/weibullBeta);
		default:
			throw new InvalidParameterException("Function type " + functionType.name() + " has not been implemented yet!");
		}
	}

	void setHalfLifeYr(double halfLifeYr) {
		if (halfLifeYr < 0) {
			throw new InvalidParameterException("The halfLifeYr argument must be greater than or equal to 0!");
		}
		this.halfLifeYr = halfLifeYr;
		setInternalAverageLifetimeYr(convertHalfLifeToAverageLifetime(halfLifeYr));
	}
	
	private double convertHalfLifeToAverageLifetime(double halfLifeYr) {
		switch(functionType) {
		case Exponential:
			return halfLifeYr / AverageLifeTimeToHalfLifeExponential;
		case Weibull:
			return Math.pow(Math.pow(halfLifeYr, weibullBeta) / AverageLifeTimeToHalfLifeExponential, 1d / weibullBeta) * GammaUtility.gamma(1d + 1d / weibullBeta);
		default:
			throw new InvalidParameterException("Function type " + functionType.name() + " has not been implemented yet!");
		}
	}

	double getValueAtTime(double timeYr, MonteCarloSimulationCompliantObject subject) {
		double avgLifetimeYr = subject != null ?
				averageLifetimeYr * CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.Lifetime, 
						subject, 
						feature.getClass().getCanonicalName() + feature.hashCode()) :
					averageLifetimeYr;
		switch(functionType) {
		case Exponential:
			return Math.exp(- timeYr / avgLifetimeYr);
		case Weibull:
			double wblLambda = getWeibullLambdaFromAverageLifetime(avgLifetimeYr);
			return Math.exp(- Math.pow(timeYr / wblLambda, weibullBeta));
		default:
			throw new InvalidParameterException("Decay function type " + functionType.name() + " has not been implemented yet!");
		}
	}

	@Override
	public DecayFunctionPanel getUI() {
		if (userInterface == null) {
			userInterface = new DecayFunctionPanel(this);
		}
		return userInterface;
	}

	@Override
	public boolean isVisible() {
		return getUI().isVisible();
	}

	double getLifetime() {
		return lifetimeMode == LifetimeMode.AVERAGE ? averageLifetimeYr : halfLifeYr;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if (e.getSource().equals(getUI().decayFunctionList)) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				DecayFunctionType type = (DecayFunctionType) getUI().decayFunctionList.getSelectedItem();
				if (type != functionType) {
					functionType = type;
					setAverageLifetimeYr(averageLifetimeYr);
					((AbstractProcessorButton) feature.getProcessor().getUI()).setChanged(true);
				}
			}
		} else if (e.getSource().equals(getUI().lifetimeModeList)) {
			if (e.getStateChange() == ItemEvent.SELECTED) {
				LifetimeMode mode = (LifetimeMode) getUI().lifetimeModeList.getSelectedItem();
				if (mode != lifetimeMode) {
					lifetimeMode = mode;
					setAverageLifetimeYr(averageLifetimeYr);
					((AbstractProcessorButton) feature.getProcessor().getUI()).setChanged(true);
				}
			}
		}
	}

	@Override
	public void numberChanged(NumberFieldEvent e) {
		if (e.getSource().equals(getUI().lifetimeTextField)) {
			double lifetime = getUI().lifetimeTextField.getValue().doubleValue();
			setLifetime(lifetime);
			((AbstractProcessorButton) feature.getProcessor().getUI()).setChanged(true);
		}
	}
	
	@Override
	public String toString() {
		if (functionType == DecayFunctionType.Exponential) {
			return functionType.name() + "_" + lifetimeMode.name() + "_" + averageLifetimeYr + "_" + halfLifeYr;
		} else {
			return functionType.name() + "_" + weibullLambda + "_" + weibullBeta + "_" + averageLifetimeYr + "_" + halfLifeYr;
		}
	}

	@Override
	public List<MemberInformation> getInformationsOnMembers() {
		List<MemberInformation> memberInfo = new ArrayList<MemberInformation>();	
		memberInfo.add(new MemberInformation(MemberLabel.DecayFunctionType, DecayFunctionType.class, functionType));
		memberInfo.add(new MemberInformation(MemberLabel.LifetimeMode, LifetimeMode.class, lifetimeMode));
		memberInfo.add(new MemberInformation(MemberLabel.Lifetime, double.class, getLifetime()));
		return memberInfo;
	}

	@Override
	public void processChangeToMember(Enum<?> label, Object value) {
		if (label == MemberLabel.DecayFunctionType) {
			functionType = (DecayFunctionType) value;
		} else if (label == MemberLabel.LifetimeMode) {
			lifetimeMode = (LifetimeMode) value;
		} else if (label == MemberLabel.Lifetime) {
			setLifetime((double) value);
		} 
	}

	@Override
	public void releaseResources() {
		if (userInterface != null) {
			userInterface.doNotListenToAnymore();
			userInterface = null;
		}
	}
}
