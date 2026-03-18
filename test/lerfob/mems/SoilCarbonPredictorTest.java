/*
 * This file is part of the mems library.
 *
 * Copyright (C) 2022-23 His Majesty the King in Right of Canada
 * Authors: Jean-Francois Lavoie and Mathieu Fortin, Canadian Wood Fibre Centre
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
package lerfob.mems;

import static java.lang.Math.abs;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import lerfob.carbonbalancetool.CarbonAccountingToolTest;
import repicea.serial.UnmarshallingException;
import repicea.serial.xml.XmlDeserializer;
import repicea.stats.data.DataSet;
import repicea.util.ObjectUtility;

public class SoilCarbonPredictorTest {

	static {
		System.out.println("RUNNING " + SoilCarbonPredictorTest.class.getName());
	}

    String GetDataPath() {
        return ObjectUtility.getPackagePath(getClass()) + "data" + ObjectUtility.PathSeparator;
    }

    @Test
    public void IterationBalanceTest() {
        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 0.0, 15d, 5.58, 1.21, 47.8, 7.62);
        predictor.predictDailyCStockChanges(compartments, inputs);

        double sum = compartments.getSumOfDailyChange();

        Assert.assertEquals("The resulting balance should be near zero", 0.0, sum, 1.0E-12);
    }

    @Test
    public void IterationBalanceTestWithTMod() {
        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 0.0, 15d, 5.58, 1.21, 47.8, 7.62);
        predictor.predictDailyCStockChanges(compartments, inputs, 1.5);

        double sum = compartments.getSumOfDailyChange();

        Assert.assertEquals("The resulting balance should be near zero", 0.0, sum, 1.0E-12);
    }

    @Test
    public void IterationBalanceTestWithInput() {
        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 1.0, 15d, 5.58, 1.21, 47.8, 7.62);
        predictor.predictDailyCStockChanges(compartments, inputs, 0.0);

        double sum = compartments.getSumOfDailyChange();

        Assert.assertEquals("The resulting balance should be near 1.0", 1.0, sum, 1.0E-12);
    }

    @Test
    public void SoilTemperatureTest() {
        // This test verifies that Eq49_getSoilTemperature returns valid values by averaging the results on one year and compares it with the input MAT

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        double MAT = 1.0;
        double Trange = 30.0;
        double result = 0.0;
        for (int day = 1; day <= 365; day++)
            result += SoilCarbonPredictorEquation.Eq49_getSoilTemperatureFromMeanAndRange(day, MAT, Trange);

        result = result / 365.0;

        Assert.assertEquals("The resulting average should be near the original MAT", MAT, result, 1.0E-12);
    }

    @Test
    public void SoilTemperatureModifierTest() {
        // This test verifies that Eq48 returns values that are plausible

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        double MAT = 1.0;
        double Trange = 30.0;
        for (int day = 1; day <= 365; day++) {
            double temp = SoilCarbonPredictorEquation.Eq49_getSoilTemperatureFromMeanAndRange(day, MAT, Trange);
            double tMod = SoilCarbonPredictorEquation.Weibull_getTemperatureModifier(predictor, temp);
            System.out.println ("SoilTemperatureModifierTest : day = " + day + ", TSoil = " + temp + ", tMOD = " + tMod);
            Assert.assertTrue("TMod should be greater than 0", tMod >= 0.0);
            Assert.assertTrue("TMod should be lower than 1.0", tMod <= 1.0);
        }
    }

    @Test
    public void IterationPerformanceTest() {
        int nbIterations = 10 * 365;

        Instant before, after;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 10.0, 15d, 5.58, 1.21, 47.8, 7.62);

        before = Instant.now();

//        SoilCarbonPredictorCompartments cChange;

        for (int i = 0; i < nbIterations; i++)
        {
            predictor.predictDailyCStocks(compartments, inputs, 1d);
//            compartments.add(cChange);
        }

        after = Instant.now();

        long elapsed = ChronoUnit.MILLIS.between(before,after);
        System.out.println("Performance test executed " + nbIterations + " iterations in " + elapsed + " milliseconds");
        Assert.assertTrue("Performance test should execute a full 10 year cycle in less than 100 ms", elapsed < 100);
    }

    @Test
    public void IterationPerformanceTestAnnual() {
        int nbIterations = 10;

        Instant before, after;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 10.0, 24.0);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 10.0, 15d, 5.58, 1.21, 47.8, 7.62);

        before = Instant.now();

//        SoilCarbonPredictorCompartments cChange;

        for (int i = 0; i < nbIterations; i++)
        {
            predictor.predictAnnualCStocks(compartments, inputs);
        }

        after = Instant.now();

        long elapsed = ChronoUnit.MILLIS.between(before,after);
        System.out.println("Performance test executed " + nbIterations + " iterations in " + elapsed + " milliseconds");
        Assert.assertTrue("Performance test should execute a full 10 year cycle in less than 100 ms", elapsed < 100);
    }

    @Test
    public void IterationStabilizationTestMontmorencyForest() {
        int nbYears = 1000;

        Instant before, after;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        double MAT = 3.8;  // between Jan 1 2013 to Dec 31st 2016 at MM
        double MinTemp = -9.48; // between Jan 1 2013 to Dec 31st 2016 at MM
        double MaxTemp = 17.79;  // between Jan 1 2013 to Dec 31st 2016 at MM
        double Trange = MaxTemp - MinTemp;

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, MAT, Trange);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.MontmorencyForest, 304.0, 54.72, 15, 4.22, 0.7918, 66.97, 3.80);

        before = Instant.now();

        DataSet ds = new DataSet(Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11"));
        ds.addObservation(compartments.getBins());

        for (int j = 0; j < nbYears; j++) {
            predictor.predictAnnualCStocks(compartments, inputs);
            ds.addObservation(compartments.getBins());
        }

        after = Instant.now();

        long elapsed = ChronoUnit.MILLIS.between(before,after);
        System.out.println("Stabilization test executed " + nbYears + " year iterations in " + elapsed + " milliseconds");

        ds.indexFieldType();
        try {
            ds.save(GetDataPath() + "stabilizationTest_MontmorencyForest.csv");
        } catch (IOException e) {
            // do nothing if we couldn't save the results - we might be running integration tests
        }

        System.out.println("IterationStabilizationTestMontmorencyForest C1-C6 : " + compartments.getLitterBinsgCm2() + ", C5-C9-C10 : " + compartments.getSoilBinsgCm2());
        System.out.println("Elapsed time type 1 = " + predictor.elapsedTimes[0]);
        System.out.println("Elapsed time type 2 = " + predictor.elapsedTimes[1]);
        System.out.println("Elapsed time type 3 = " + predictor.elapsedTimes[2]);
        System.out.println("Elapsed time type 4 = " + predictor.elapsedTimes[3]);
    }

    @Test
    public void IterationStabilizationTestMontmorencyForestFitParms() throws UnmarshallingException {
        int nbYears = 100;

        Instant before, after;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        double MAT = 3.8;  // between Jan 1 2013 to Dec 31st 2016 at MM
        double MinTemp = -9.48; // between Jan 1 2013 to Dec 31st 2016 at MM
        double MaxTemp = 17.79;  // between Jan 1 2013 to Dec 31st 2016 at MM
        double Trange = MaxTemp - MinTemp;

        String filename = ObjectUtility.getRelativePackagePath(SoilCarbonPredictor.class) + 
        		"data" + ObjectUtility.PathSeparator + 
        		"sites" + ObjectUtility.PathSeparator + "mcmcMems_Montmorency.zml";
        System.out.println("loading " + filename);

        XmlDeserializer dser = new XmlDeserializer(filename);

        MEMSSite site = (MEMSSite) dser.readObject();

        // read the fit params from mha and set them to the Predictor
        predictor.setParms(site.mha.getFinalParameterEstimates());

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, MAT, Trange);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.MontmorencyForest, 304.0, 54.72, 15, 4.22, 0.7918, 66.97, 3.80);

        before = Instant.now();

        DataSet ds = new DataSet(Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11"));
        ds.addObservation(compartments.getBins());

        for (int j = 0; j < nbYears; j++) {
            predictor.predictAnnualCStocks(compartments, inputs);
            ds.addObservation(compartments.getBins());
        }

        after = Instant.now();

        long elapsed = ChronoUnit.MILLIS.between(before,after);
        System.out.println("Stabilization test executed " + nbYears + " year iterations in " + elapsed + " milliseconds");

        ds.indexFieldType();
        try {
            ds.save(GetDataPath() + "stabilizationTest_MontmorencyForest_fitParms.csv");
        } catch (IOException e) {
            // do nothing if we couldn't save the results - we might be running integration tests
        }

        System.out.println("IterationStabilizationTestMontmorencyForest C1-C6 : " + compartments.getLitterBinsgCm2() + ", C5-C9-C10 : " + compartments.getSoilBinsgCm2());
        System.out.println("Elapsed time type 1 = " + predictor.elapsedTimes[0]);
        System.out.println("Elapsed time type 2 = " + predictor.elapsedTimes[1]);
        System.out.println("Elapsed time type 3 = " + predictor.elapsedTimes[2]);
        System.out.println("Elapsed time type 4 = " + predictor.elapsedTimes[3]);
    }

    @Test
    public void IterationStabilizationTestWithNullTMod() {
        int nbYears = 1000;

        Instant before, after;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        double MAT = -1000.0;
        double Trange = 30.0;

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(100.0, MAT, Trange);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.MontmorencyForest, 0.0, 0.0, 15, 4.22, 0.7918, 66.97, 3.80);

        before = Instant.now();

        DataSet ds = new DataSet(Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11"));
        ds.addObservation(compartments.getBins());

        for (int j = 0; j < nbYears; j++) {
            predictor.predictAnnualCStocks(compartments, inputs);
            ds.addObservation(compartments.getBins());
        }

        after = Instant.now();

        long elapsed = ChronoUnit.MILLIS.between(before,after);
        System.out.println("Stabilization test executed " + nbYears + " year iterations in " + elapsed + " milliseconds");

        ds.indexFieldType();
        try {
            ds.save(GetDataPath() + "stabilizationTest_NullTMod.csv");
        } catch (IOException e) {
            // do nothing if we couldn't save the results - we might be running integration tests
        }

        System.out.println("IterationStabilizationTestWithNullTMod C1-C6 : " + compartments.getLitterBinsgCm2() + ", C5-C9-C10 : " + compartments.getSoilBinsgCm2());
    }

    @Test
    public void IterationStabilizationTestCheckCompartmentRatios() {

        // this test performs two simulations with different input rates, and then will compare the compartment ratios to make sure they are constant no matter the input value
        int nbIterations = 365;
        int nbYears = 1000;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        // Perform simulation with 1g input per day
        SoilCarbonPredictorCompartments compartments_1g = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs_1g = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 1.0, 15d, 5.58, 1.21, 47.8, 7.62);

//        SoilCarbonPredictorCompartments cChange_1g = new SoilCarbonPredictorCompartments(0.0);

        for (int j = 0; j < nbYears; j++) {
            for (int i = 0; i < nbIterations; i++) {
                predictor.predictDailyCStocks(compartments_1g, inputs_1g, 1d);
            }
        }

        // Perform simulation with 2g input per day
        SoilCarbonPredictorCompartments compartments_2g = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs_2g = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 2.0, 15d, 5.58, 1.21, 47.8, 7.62);

 //      SoilCarbonPredictorCompartments cChange_2g = new SoilCarbonPredictorCompartments(0.0);

        for (int j = 0; j < nbYears; j++) {
            for (int i = 0; i < nbIterations; i++) {
                predictor.predictDailyCStocks(compartments_2g, inputs_2g, 1d);
            }
        }

        double C1C2C3C4C6_1g = compartments_1g.getLitterBinsgCm2();

        double C1C2C3C4C6_2g = compartments_2g.getLitterBinsgCm2();

        final double epsilon = 1e-10;

        double C1Ratio1g = compartments_1g.C1 / C1C2C3C4C6_1g;
        double C1Ratio2g = compartments_2g.C1 / C1C2C3C4C6_2g;
        Assert.assertEquals(C1Ratio1g, C1Ratio2g, epsilon);

        double C2Ratio1g = compartments_1g.C2 / C1C2C3C4C6_1g;
        double C2Ratio2g = compartments_2g.C2 / C1C2C3C4C6_2g;
        Assert.assertEquals(C2Ratio1g, C2Ratio2g, epsilon);

        double C3Ratio1g = compartments_1g.C3 / C1C2C3C4C6_1g;
        double C3Ratio2g = compartments_2g.C3 / C1C2C3C4C6_2g;
        Assert.assertEquals(C3Ratio1g, C3Ratio2g, epsilon);

        double C4Ratio1g = compartments_1g.C4 / C1C2C3C4C6_1g;
        double C4Ratio2g = compartments_2g.C4 / C1C2C3C4C6_2g;
        Assert.assertEquals(C4Ratio1g, C4Ratio2g, epsilon);

        double C6Ratio1g = compartments_1g.C6 / C1C2C3C4C6_1g;
        double C6Ratio2g = compartments_2g.C6 / C1C2C3C4C6_2g;
        Assert.assertEquals(C6Ratio1g, C6Ratio2g, epsilon);
    }

    @Ignore
    @Test
    // TODO : see what we should do with this test
    public void IterationStabilizationTestWithPerturbation() {

        // this test will simulate 500 years with 1g, then 500 years with 2g input and will count the number of years needed to reach ratio stability again
        int nbIterations = 365;
        int nbYears = 1000;

        int ratioPerturbedYearCounter = 0;

        SoilCarbonPredictor predictor = new SoilCarbonPredictor(false);

        SoilCarbonPredictorCompartments compartments = new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        SoilCarbonPredictorInput inputs = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 1.0, 15d, 5.58, 1.21, 47.8, 7.62);
        SoilCarbonPredictorInput inputsAfterPerturbation = new SoilCarbonPredictorInput(SoilCarbonPredictorInput.LandType.Unknown, 2.0, 15d, 5.58, 1.21, 47.8, 7.62);

        SoilCarbonPredictorCompartments cChange =new SoilCarbonPredictorCompartments(1.0, 5d, 20d);

        DataSet ds = new DataSet(Arrays.asList("C1", "C2", "C3", "C4", "C5", "C6", "C7", "C8", "C9", "C10", "C11"));
        ds.addObservation(compartments.getBins());

        double C1Ratio = 0.0;
        double C2Ratio = 0.0;
        double C3Ratio = 0.0;
        double C4Ratio = 0.0;
        double C6Ratio = 0.0;

        // simulate nbYears without perturbation to reach equilibrium
        for (int j = 0; j < nbYears / 2; j++) {
            for (int i = 0; i < nbIterations; i++) {
                predictor.predictDailyCStocks(compartments, inputs, 1d);
            }
            ds.addObservation(compartments.getBins());

            double C1C2C3C4C6 = compartments.getLitterBinsgCm2();

            C1Ratio = compartments.C1 / C1C2C3C4C6;
            C2Ratio = compartments.C2 / C1C2C3C4C6;
            C3Ratio = compartments.C3 / C1C2C3C4C6;
            C4Ratio = compartments.C4 / C1C2C3C4C6;
            C6Ratio = compartments.C6 / C1C2C3C4C6;
        }

        final double epsilon = 1e-10;

        // simulate nbYears with perturbation (2g instead of 1g)
        for (int j = 0; j < nbYears / 2; j++) {
            for (int i = 0; i < nbIterations; i++) {
                predictor.predictDailyCStocks(compartments, inputsAfterPerturbation, 1d);
            }
            ds.addObservation(compartments.getBins());

            double C1C2C3C4C6 = compartments.getLitterBinsgCm2();

            double C1RatioPerturb = compartments.C1 / C1C2C3C4C6;
            double C2RatioPerturb = compartments.C2 / C1C2C3C4C6;
            double C3RatioPerturb = compartments.C3 / C1C2C3C4C6;
            double C4RatioPerturb = compartments.C4 / C1C2C3C4C6;
            double C6RatioPerturb = compartments.C6 / C1C2C3C4C6;

            if (abs(C1RatioPerturb - C1Ratio) > epsilon ||
                abs(C2RatioPerturb - C2Ratio) > epsilon ||
                abs(C3RatioPerturb - C3Ratio) > epsilon ||
                abs(C4RatioPerturb - C4Ratio) > epsilon ||
                abs(C6RatioPerturb - C6Ratio) > epsilon)
                ratioPerturbedYearCounter++;
        }

        ds.indexFieldType();
        try {
            ds.save(GetDataPath() + "stabilizationTest_perturbation.csv");
        } catch (IOException e) {
            // do nothing if we couldn't save the results - we might be running integration tests
        }

        Assert.assertTrue(ratioPerturbedYearCounter < 10);
    }
    
   
    
    
}