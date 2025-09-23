/*
 * This file is part of the CAT library.
 *
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

import org.junit.Assert;
import org.junit.Test;

import lerfob.carbonbalancetool.productionlines.DecayFunction.DecayFunctionType;
import lerfob.carbonbalancetool.productionlines.DecayFunction.LifetimeMode;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import repicea.math.Matrix;
import repicea.simulation.HierarchicalLevel;
import repicea.simulation.MonteCarloSimulationCompliantObject;
import repicea.stats.Distribution;
import repicea.stats.estimates.MonteCarloEstimate;

/**
 * Test different DecayFunction settings.
 * @author Mathieu Fortin - December 2023
 */
public class DecayFunctionTest {

	@Test
	public void test01ExponentialFunction() {
		DecayFunction df = new DecayFunction(null, LifetimeMode.AVERAGE, DecayFunctionType.Exponential, 10d);
		Assert.assertEquals("Testing half life", 10d * Math.log(2d), df.halfLifeYr, 1E-8);
		df.setHalfLifeYr(10);
		Assert.assertEquals("Testing half life", 10d / Math.log(2d), df.averageLifetimeYr, 1E-8);
	}
	
	@Test
	public void test02WeibullFunction() {
		DecayFunction df = new DecayFunction(null, LifetimeMode.AVERAGE, DecayFunctionType.Exponential, 10d);
		df.functionType = DecayFunctionType.Weibull;
		df.setAverageLifetimeYr(df.averageLifetimeYr);
		Assert.assertEquals("Testing Weibull lambda", 10.89124421058335, df.weibullLambda, 1E-8);
		Assert.assertEquals("Testing average lifetime", 10, df.averageLifetimeYr, 1E-8);
		df.functionType = DecayFunctionType.Exponential;
		df.setHalfLifeYr(10);
		Assert.assertEquals("Testing average lifetime", 10d / Math.log(2d), df.averageLifetimeYr, 1E-8);
	}

	private static class FakeCompartmentManager implements MonteCarloSimulationCompliantObject {

		int mcReal;
		
		@Override
		public String getSubjectId() {return null;}

		@Override
		public HierarchicalLevel getHierarchicalLevel() {return null;}

		@Override
		public int getMonteCarloRealizationId() {
			return mcReal;
		}
		
	}
	
	@Test
	public void test03SensitivityAnalysisExponentialFunction() {
		CarbonUnitFeature cuf = new CarbonUnitFeature(null);
		DecayFunction df = new DecayFunction(cuf, LifetimeMode.AVERAGE, DecayFunctionType.Exponential, 10d);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				true,
				0.4);
		
		@SuppressWarnings("unused")
		double originalValue = df.getValueAtTime(1, null);

		FakeCompartmentManager fakeManager = new FakeCompartmentManager();
		MonteCarloEstimate est = new MonteCarloEstimate();
		Matrix real;
		for (int i = 0; i < 100000; i++) {
			fakeManager.mcReal = i;		
			double value = df.getValueAtTime(1, fakeManager);
			real = new Matrix(1,1,value,0);
			est.addRealization(real);
		}
		double mean = est.getMean().getValueAt(0, 0);
		double variance = est.getVariance().getValueAt(0, 0);

		double meanRatio = mean / 0.9007840398059188; 
		double varianceRatio = variance / 5.014390094398527E-4;
		
		Assert.assertTrue("Testing mean", Math.abs(1d - meanRatio) < 1E-2);
		Assert.assertTrue("Testing variance", Math.abs(1d - varianceRatio) < 3E-2);

		double value = df.getValueAtTime(1, fakeManager);
		
		Assert.assertEquals("Testing if values for the same iteration do not change", 
				est.getRealizations().get(est.getRealizations().size() -1).getValueAt(0, 0),
				value, 
				1E-8);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				false,
				0.4);
	}

	@Test
	public void test04SensitivityAnalysisWeibullFunction() {
		CarbonUnitFeature cuf = new CarbonUnitFeature(null);
		DecayFunction df = new DecayFunction(cuf, LifetimeMode.AVERAGE, DecayFunctionType.Weibull, 10d);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				true,
				0.4);
		
		@SuppressWarnings("unused")
		double originalValue = df.getValueAtTime(10, null);

		FakeCompartmentManager fakeManager = new FakeCompartmentManager();
		MonteCarloEstimate est = new MonteCarloEstimate();
		Matrix real;
		for (int i = 0; i < 100000; i++) {
			fakeManager.mcReal = i;		
			double value = df.getValueAtTime(10, fakeManager);
			real = new Matrix(1,1,value,0);
			est.addRealization(real);
		}
		double mean = est.getMean().getValueAt(0, 0);
		double variance = est.getVariance().getValueAt(0, 0);

		double meanRatio = mean / 0.47897078997680187; 
		double varianceRatio = variance / 0.07376034188749936;
		
		Assert.assertTrue("Testing mean", Math.abs(1d - meanRatio) < 1E-2);
		Assert.assertTrue("Testing variance", Math.abs(1d - varianceRatio) < 3E-2);

		double value = df.getValueAtTime(10, fakeManager);
		
		Assert.assertEquals("Testing if values for the same iteration do not change", 
				est.getRealizations().get(est.getRealizations().size() -1).getValueAt(0, 0),
				value, 
				1E-8);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				false,
				0.4);
	}

	@Test
	public void test05SensitivityAnalysisDifferentSubjectSameRealization() {
		DecayFunction df = new DecayFunction(new CarbonUnitFeature(null), LifetimeMode.AVERAGE, DecayFunctionType.Weibull, 10d);
		DecayFunction df2 = new DecayFunction(new CarbonUnitFeature(null), LifetimeMode.AVERAGE, DecayFunctionType.Weibull, 10d);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				true,
				0.4);
		
		double originalValue = df.getValueAtTime(10, null);
		double originalValue2 = df2.getValueAtTime(10, null);

		FakeCompartmentManager fakeManager = new FakeCompartmentManager();
		double value = df.getValueAtTime(10, fakeManager);
		double value2 = df2.getValueAtTime(10, fakeManager);

		Assert.assertTrue("Test if stochastic is different from deterministic", Math.abs(value - originalValue) > 1E-8);
		Assert.assertTrue("Test if stochastic is different from deterministic (second shot)", Math.abs(value2 - originalValue2) > 1E-8);

		Assert.assertTrue("Test if stochastic values are different across subject", Math.abs(value2 - value) > 1E-8);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.Lifetime,
				Distribution.Type.GAUSSIAN,
				false,
				0.4);

	}

}
