/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2024 His Majesty the King in Right of Canada
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
package lerfob.carbonbalancetool.memsconnectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompartment.CompartmentInfo;
import lerfob.carbonbalancetool.CATSimulationResult;
import lerfob.carbonbalancetool.CarbonAccountingTool;
import lerfob.carbonbalancetool.CarbonAccountingTool.CATMode;
import lerfob.carbonbalancetool.CarbonAccountingToolTest;
import lerfob.carbonbalancetool.io.CATExportTool;
import lerfob.carbonbalancetool.io.CATGrowthSimulationCompositeStand;
import lerfob.carbonbalancetool.io.CATGrowthSimulationPlot;
import lerfob.carbonbalancetool.io.CATGrowthSimulationPlotSample;
import lerfob.carbonbalancetool.io.CATGrowthSimulationRecordReader;
import lerfob.carbonbalancetool.io.CATGrowthSimulationTreeWithDBH;
import lerfob.mems.MEMSSite.SiteType;
import lerfob.mems.SoilCarbonPredictorCompartments;
import repicea.io.javacsv.CSVReader;
import repicea.io.tools.ImportFieldManager;
import repicea.math.Matrix;
import repicea.math.SymmetricMatrix;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.stats.estimates.Estimate;
import repicea.stats.estimates.MonteCarloEstimate;
import repicea.util.ObjectUtility;

public class MEMSConnectorsTest {

	@SuppressWarnings("serial")
	static class CATGrowthSimulationRecordReaderHacked extends CATGrowthSimulationRecordReader {
		
		public CATGrowthSimulationRecordReaderHacked(ApplicationScale scale, ManagementType management) {
			super(scale, management);
		}

		@Override
		protected CATGrowthSimulationTreeHacked createTree(CATGrowthSimulationPlot plot, 
				StatusClass statusClass, 
				double treeOverbarkVolumeDm3, 
				double numberOfTrees, 
				String originalSpeciesName,
				Double dbhCm,
				Double aboveGroundVolumeM3,
				Double aboveGroundBiomassMg,
				Double aboveGroundCarbonMg,
				Double belowGroundVolumeM3,
				Double belowGroundBiomassMg,
				Double belowGroundCarbonMg,
				Double commercialBiomassMg) {
			return new CATGrowthSimulationTreeHacked(plot, statusClass, treeOverbarkVolumeDm3, numberOfTrees, originalSpeciesName, dbhCm);
		}

		@Override
		protected CATGrowthSimulationCompositeStand createCompositeStand(String standIdentification, int dateYr, boolean scaleDependentInterventionResult, Map<CATGrowthSimulationFieldID, Boolean> interfaceEnablingMap) {
			return new CATGrowthSimulationCompositeStandHacked(dateYr, standIdentification, this, scaleDependentInterventionResult);
		}
	}
	
	static class CATGrowthSimulationPlotSampleHacked extends CATGrowthSimulationPlotSample implements MEMSCompatibleStand {
		
		protected CATGrowthSimulationPlotSampleHacked(CATGrowthSimulationCompositeStandHacked compositeStand) {
			super(compositeStand);
		}

		@Override
		public SiteType getSiteType() {
			return ((CATGrowthSimulationCompositeStandHacked) compositeStand).getSiteType();
		}

		@Override
		public double[] getMeanDailyTemperatureCForThisYear(int year) {
			return ((CATGrowthSimulationCompositeStandHacked) compositeStand).getMeanDailyTemperatureCForThisYear(year);
		}

		@Override
		public boolean isTemperatureFromAir() {
			return ((CATGrowthSimulationCompositeStandHacked) compositeStand).isTemperatureFromAir();
		}
		
	}
	
	
	static class CATGrowthSimulationTreeHacked extends CATGrowthSimulationTreeWithDBH implements MEMSCompatibleTree {

		CATGrowthSimulationTreeHacked(CATGrowthSimulationPlot plot, 
				StatusClass statusClass, 
				double treeVolumeDm3,
				double numberOfTrees, 
				String originalSpeciesName,
				double dbhCm) {
			super(plot, statusClass, treeVolumeDm3, numberOfTrees, originalSpeciesName, dbhCm,
					null, null, null, null, null, null, null); // all above and below ground values set to null.
		}

		@Override
		public double getStemBasalAreaM2() {
			return Math.PI * getDbhCm() * getDbhCm() * 0.000025;
		}

		
		/**
		 * This implementation is based on Finer et al. (2011).
		 * @see <a href=https://doi.org/10.1016/j.foreco.2011.08.042> Finer, L., M. Ohashi, K. Noguchi, and 
		 * Y. Hirano. 2011. Fine root production and turnover in forest ecosystems in relation to stand and 
		 * environmental characteristics. Forest Ecology and Management 262(11): 2008-2023</a>
		 */
		@Override
		public double getAnnualFineRootDetritusCarbonProductionMgYr() {
			return (1.55 * Math.log(getStemBasalAreaM2()) + 9.408) * .001;
		}

		@Override
		public double getAnnualBranchDetritusCarbonProductionMgYr() {
			return getAnnualFoliarDetritusCarbonProductionMgYr() * .5;
		}

		@Override
		public double getFoliarBiomassMg() {
			// Useless in this context
			return 0;
		}
	}

	static class CATGrowthSimulationCompositeStandHacked extends CATGrowthSimulationCompositeStand implements MEMSCompatibleStand {

		CATGrowthSimulationCompositeStandHacked(int dateYr, String standIdentification, CATGrowthSimulationRecordReader reader, boolean isInterventionResult) {
			super(dateYr, standIdentification, reader, isInterventionResult, null); // Map set to null
		}

		@Override
		public SiteType getSiteType() {return SiteType.Montmorency;}

		private double getMeanAnnualTemperatureC() {
			return 3.8; // between Jan 1 2013 to Dec 31st 2016 at Foret Montmorency 
		}

		private double getAnnualTemperatureRange() {
			double minTemp = -9.48;   // between Jan 1 2013 to Dec 31st 2016 at Foret Montmorency
			double maxTemp = 17.79;   // between Jan 1 2013 to Dec 31st 2016 at Foret Montmorency
			return maxTemp - minTemp;
		}
		
		@Override
		protected CATGrowthSimulationPlotSample createPlotSample() {
			return new CATGrowthSimulationPlotSampleHacked(this);
		}

		@Override
		public double[] getMeanDailyTemperatureCForThisYear(int year) {
			return SoilCarbonPredictorCompartments.createDailyTemperatureFromMeanAndRange(getMeanAnnualTemperatureC(), getAnnualTemperatureRange());
		}
		
		@Override
		public boolean isTemperatureFromAir() {
			return false;
		}
	}

	@Ignore	// need to be fixed when everything else is ok MF20240809
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testMEMSIntegration01() throws Exception {
//		CATGrowthSimulationRecordReader.TestUnevenAgedInfiniteSequence = true;	// this way we get the application scale set to stand
		String filename = ObjectUtility.getPackagePath(CarbonAccountingToolTest.class) + "io" + File.separator + "MathildeTreeExport.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "MathildeTreeExportWithDBH.ife";
//		String refFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTableReference.xml";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATGrowthSimulationRecordReaderHacked recordReader = new CATGrowthSimulationRecordReaderHacked(ApplicationScale.Stand, ManagementType.UnevenAged);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.calculateCarbon();
		CATSimulationResult simResults = cat.retrieveSimulationSummary();
		Estimate<Matrix, SymmetricMatrix, ?> estimate = simResults.getEvolutionMap().get(CompartmentInfo.Soil);
		Matrix evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 36, evolSoil.m_iRows);
		Assert.assertEquals("Testing second entry", 68.19255976058204, evolSoil.getValueAt(1, 0), 1E-8);
		Assert.assertEquals("Testing first last", 115.38980090253273, evolSoil.getValueAt(35, 0), 1E-8);

		estimate = simResults.getEvolutionMap().get(CompartmentInfo.Humus);
		evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 36, evolSoil.m_iRows);
		Assert.assertEquals("Testing second entry", 26.655653185108896, evolSoil.getValueAt(1, 0), 1E-8);
		Assert.assertEquals("Testing first last", 59.66286553497835, evolSoil.getValueAt(35, 0), 1E-8);

		estimate = simResults.getEvolutionMap().get(CompartmentInfo.MineralSoil);
		evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 36, evolSoil.m_iRows);
		Assert.assertEquals("Testing second entry", 41.536906575473125, evolSoil.getValueAt(1, 0), 1E-8);
		Assert.assertEquals("Testing first last", 55.726935367554375, evolSoil.getValueAt(35, 0), 1E-8);
		
		estimate = simResults.getBudgetMap().get(CompartmentInfo.Soil);
		evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 1, evolSoil.m_iRows);
		Assert.assertEquals("Testing entry", 97.9033534714863, evolSoil.getValueAt(0, 0), 1E-8);

		estimate = simResults.getBudgetMap().get(CompartmentInfo.Humus);
		evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 1, evolSoil.m_iRows);
		Assert.assertEquals("Testing entry", 38.33797503396882, evolSoil.getValueAt(0, 0), 1E-8);

		estimate = simResults.getBudgetMap().get(CompartmentInfo.MineralSoil);
		evolSoil = estimate.getMean();
		Assert.assertEquals("Testing nb of entries", 1, evolSoil.m_iRows);
		Assert.assertEquals("Testing entry", 59.56537843751747, evolSoil.getValueAt(0, 0), 1E-8);
//		CATGrowthSimulationRecordReader.TestUnevenAgedInfiniteSequence = false;	// set the static variable to its original value
		
		MonteCarloEstimate mineralSoilInput = simResults.getMineralSoilCarbonInputMgHa();
		Assert.assertTrue("Mineral soil input not null", mineralSoilInput != null);
		Matrix mineralSoilInputMean = mineralSoilInput.getMean();
		Assert.assertEquals("Testing value at slot 10", 2.5501334035, mineralSoilInputMean.getValueAt(10, 0), 1E-8);
				
		MonteCarloEstimate humusInput = simResults.getHumusCarbonInputMgHa();
		Assert.assertTrue("Humus input not null", humusInput != null);
		Matrix humusInputMean = humusInput.getMean();
		Assert.assertEquals("Testing value at slot 10", 3.3595302841845043, humusInputMean.getValueAt(10, 0), 1E-8);
		
		CATExportTool exportTool = cat.createExportTool();
		List<Enum> selectedOptions = new ArrayList<Enum>();
		selectedOptions.add(CATExportTool.ExportOption.SoilCarbonInput);
		exportTool.setSelectedOptions(selectedOptions);
		String exportFilename = ObjectUtility.getPackagePath(getClass()) + "soilInputTest.csv"; 
		exportTool.setFilename(exportFilename);
		exportTool.exportRecordSets();
		
		CSVReader reader = new CSVReader(exportFilename);
		Object[] record = null;
		int i = 0;
		while (i < reader.getRecordCount()) {
			record = reader.nextRecord();	
			i++;
		}
		reader.close();
		Assert.assertEquals("Comparing last humus input", 5.4459231510046395, Double.parseDouble(record[2].toString()), 1E-8);
		Assert.assertEquals("Comparing last mineral soil input", 3.914118550371999, Double.parseDouble(record[3].toString()), 1E-8);
	}
	
}
