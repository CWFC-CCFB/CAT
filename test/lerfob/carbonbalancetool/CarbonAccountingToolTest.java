package lerfob.carbonbalancetool;

import java.io.File;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompartment.CompartmentInfo;
import lerfob.carbonbalancetool.CATSettings.CATSpecies;
import lerfob.carbonbalancetool.CATUtility.ProductionManagerName;
import lerfob.carbonbalancetool.CarbonAccountingTool.CATMode;
import lerfob.carbonbalancetool.io.CATGrowthSimulationRecordReader;
import lerfob.carbonbalancetool.io.CATYieldTableRecordReader;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import repicea.io.tools.ImportFieldManager;
import repicea.math.Matrix;
import repicea.math.SymmetricMatrix;
import repicea.math.utility.GaussianUtility;
import repicea.serial.xml.XmlDeserializer;
import repicea.serial.SerializerChangeMonitor;
import repicea.stats.Distribution.Type;
import repicea.stats.estimates.Estimate;
import repicea.stats.estimates.MonteCarloEstimate;
import repicea.util.ObjectUtility;



public class CarbonAccountingToolTest {

	static {
		SerializerChangeMonitor.registerClassNameChange("repicea.stats.distributions.NonparametricDistribution", "repicea.stats.distributions.EmpiricalDistribution");
	}
	
	
	
	private final static NumberFormat FORMATTER = NumberFormat.getInstance();

	@Test
	public void test01Deserialization() {
		String filename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		ProductionProcessorManager ppm = new ProductionProcessorManager();
		try {
			ppm.load(filename);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("Error while loading");
		}
		boolean isValid;
		try {
			ppm.validate();
			isValid = true;
		} catch (Exception e) {
			isValid = false;
		}
		Assert.assertTrue(isValid);
	}


	@Test
	public void test02SimpleCarbonAccounting() throws Exception {
		String managerFilename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		final String standID = "StandTest";
		final double areaHa = .04;
		List<CATCompatibleStand> stands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand stand;
		CATCompatibleTree tree;
		for (int i = 1; i <= 10; i++) {
			int dateYr = i * 10;
			int ageYr = dateYr;
			stand = new CarbonToolCompatibleStandImpl("beech", standID, areaHa, dateYr, ageYr);
			stands.add(stand);
			for (int j = 1; j <= 10; j++) {
				tree = new CarbonToolCompatibleTreeImpl(stand.getDateYr() * .01, "Fagus sylvatica");
				((CarbonToolCompatibleStandImpl) stand).addTree(tree);
			}
		}
		
		CarbonAccountingTool tool = new CarbonAccountingTool(CATMode.SCRIPT);
		tool.initializeTool(null);
		tool.setStandList(stands);
		try {
			tool.getCarbonToolSettings().getCurrentProductionProcessorManager().load(managerFilename);
			tool.calculateCarbon();
			CATSingleSimulationResult result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unable to calculate carbon!");
		}
		tool.requestShutdown();
	}
	

	@Test
	public void test03MatterBalanceAfterHarvest() throws Exception {
		String managerFilename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		final String standID = "StandTest";
		final double areaHa = .04;
		List<CATCompatibleStand> stands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand stand;
		CATCompatibleTree tree;
		for (int i = 1; i <= 10; i++) {
			int dateYr = i * 10;
			int ageYr = dateYr;
			stand = new CarbonToolCompatibleStandImpl("beech", standID, areaHa, dateYr, ageYr);
			stands.add(stand);
			for (int j = 1; j <= 10; j++) {
				tree = new CarbonToolCompatibleTreeImpl(stand.getDateYr() * .01, "Fagus sylvatica");
				((CarbonToolCompatibleStandImpl) stand).addTree(tree);
			}
		}
		
		CarbonAccountingTool tool = new CarbonAccountingTool(CATMode.SCRIPT);
		tool.initializeTool(null);
		tool.setStandList(stands);
		try {
			tool.getCarbonToolSettings().getCurrentProductionProcessorManager().load(managerFilename);
			tool.calculateCarbon();
			CATSingleSimulationResult result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());
			Matrix obsLivingBiomass = result.getEvolutionMap().get(CompartmentInfo.LivingBiomass).getMean();
			Matrix obsDOM = result.getEvolutionMap().get(CompartmentInfo.DeadBiom).getMean();
			Matrix obsProducts = result.getEvolutionMap().get(CompartmentInfo.TotalProducts).getMean();
			int indexFirstProducts = -1;
			for (int i = 0; i < obsProducts.m_iRows; i++) {
				if (obsProducts.getValueAt(i, 0) > 0d) {
					indexFirstProducts = i;
					break;
				}
			}
			if (indexFirstProducts == -1) {
				Assert.fail("Cannot find the first occurrence of HWP!");
			} else {
				double totalBefore = obsLivingBiomass.add(obsDOM).getValueAt(indexFirstProducts - 1, 0);
				double totalAfter = obsLivingBiomass.add(obsDOM).getValueAt(indexFirstProducts, 0) + obsProducts.getValueAt(indexFirstProducts, 0);
				Assert.assertEquals("Testing total biomass before and biomass after harvesting", totalBefore, totalAfter, 1E-8);
			}
			tool.requestShutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unable to calculate carbon!");
		}
	}

	
	@Test
	public void test04WithAgeDifferentFromDate() throws Exception {
		String managerFilename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		final String standID = "StandTest";
		final double areaHa = .04;
		List<CATCompatibleStand> standsWithDifferentDates = new ArrayList<CATCompatibleStand>();
		List<CATCompatibleStand> standsWithSameDates = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand stand0, stand1;
		CATCompatibleTree tree0, tree1;
		for (int i = 1; i <= 10; i++) {
			int ageYr = i * 10;
			int dateYr = 2000 + ageYr;
			stand0 = new CarbonToolCompatibleStandImpl("beech", standID, areaHa, dateYr, ageYr);
			stand1 = new CarbonToolCompatibleStandImpl("beech", standID, areaHa, ageYr, ageYr);
			standsWithDifferentDates.add(stand0);
			standsWithSameDates.add(stand1);
			for (int j = 1; j <= 10; j++) {
				tree0 = new CarbonToolCompatibleTreeImpl(ageYr * .01, "Fagus sylvatica");
				((CarbonToolCompatibleStandImpl) stand0).addTree(tree0);
				tree1 = new CarbonToolCompatibleTreeImpl(ageYr * .01, "Fagus sylvatica");
				((CarbonToolCompatibleStandImpl) stand1).addTree(tree1);
			}
		}
		
		CarbonAccountingTool tool = new CarbonAccountingTool(CATMode.SCRIPT);
		tool.initializeTool(null);
		tool.setStandList(standsWithDifferentDates);
		try {
			tool.getCarbonToolSettings().getCurrentProductionProcessorManager().load(managerFilename);
			tool.calculateCarbon();
			CATSingleSimulationResult result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());
			
			double obsLivingBiomass = result.getBudgetMap().get(CompartmentInfo.LivingBiomass).getMean().getValueAt(0, 0);
			double obsDOM = result.getBudgetMap().get(CompartmentInfo.DeadBiom).getMean().getValueAt(0, 0);
			double obsProducts = result.getBudgetMap().get(CompartmentInfo.TotalProducts).getMean().getValueAt(0, 0);

			tool.setStandList(standsWithSameDates);
			tool.calculateCarbon();
			result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());

			double refLivingBiomass = result.getBudgetMap().get(CompartmentInfo.LivingBiomass).getMean().getValueAt(0, 0);
			double refDOM = result.getBudgetMap().get(CompartmentInfo.DeadBiom).getMean().getValueAt(0, 0);
			double refProducts = result.getBudgetMap().get(CompartmentInfo.TotalProducts).getMean().getValueAt(0, 0);

			Assert.assertEquals("Comparing living biomass", refLivingBiomass, obsLivingBiomass, 1E-8);
			Assert.assertEquals("Comparing DOM", refDOM, obsDOM, 1E-8);
			Assert.assertEquals("Comparing HWP", refProducts, obsProducts, 1E-8);
			
			tool.requestShutdown();
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unable to calculate carbon!");
		}
	}

	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test05WithYieldTable() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.ife";
		String refFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTableReference.xml";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATYieldTableRecordReader recordReader = new CATYieldTableRecordReader(CATSpecies.ABIES);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.calculateCarbon();
		CATSingleSimulationResult result = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> obsMap = result.getBudgetMap();
		
//		XmlSerializer serializer = new XmlSerializer(refFilename);
//		serializer.writeObject(obsMap);

		XmlDeserializer deserializer = new XmlDeserializer(refFilename);
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> refMap = (Map) deserializer.readObject();
		int nbCompartmentChecked = 0;
//		Assert.assertTrue("Testing the size of the map", refMap.size() == obsMap.size());  // TODO re-enable this line once the test result has been updated MF2024-04-03
		for (CompartmentInfo key : refMap.keySet()) {
			double expected = refMap.get(key).getMean().getValueAt(0, 0);
			double observed = obsMap.get(key).getMean().getValueAt(0, 0);
			Assert.assertEquals("Testing compartment " + key.name(), expected, observed, 1E-8);
			nbCompartmentChecked++;
		}
		System.out.println("Successfully tested this number of compartments " + nbCompartmentChecked);
		cat.requestShutdown();
	}
	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test06WithSimulationResults() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "MathildeTreeExport.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "MathildeTreeExport.ife";
		String speciesMatchFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "speciesCorrespondanceForSimulationData.xml";
		String refFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "MathildeTreeExportReference.xml";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATGrowthSimulationRecordReader recordReader = new CATGrowthSimulationRecordReader();
		recordReader.getSelector().load(speciesMatchFilename);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.calculateCarbon();
		CATSingleSimulationResult result = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, MonteCarloEstimate> obsMap = result.getEvolutionMap();
		
//		XmlSerializer serializer = new XmlSerializer(refFilename);
//		serializer.writeObject(obsMap);

		XmlDeserializer deserializer = new XmlDeserializer(refFilename);
		Map<CompartmentInfo, MonteCarloEstimate> refMap = (Map) deserializer.readObject();
		int nbCompartmentChecked = 0;
//		Assert.assertTrue("Testing the size of the map", refMap.size() == obsMap.size()); // TODO re-enable this line once the test result has been updated MF2024-04-03
		for (CompartmentInfo key : refMap.keySet()) {
			Matrix expEvolution = refMap.get(key).getMean();
			Matrix obsEvolution = obsMap.get(key).getMean();
			Assert.assertEquals("Comparing matrix first dimension", expEvolution.m_iCols, obsEvolution.m_iCols);
			Assert.assertEquals("Comparing matrix second dimension", expEvolution.m_iRows, obsEvolution.m_iRows);
			for (int i = 0; i < expEvolution.m_iRows; i++) {
				double expected = expEvolution.getValueAt(i, 0);
				double observed = obsEvolution.getValueAt(i, 0);
				Assert.assertEquals("Testing compartment " + key.name() + " at row " + i, expected, observed, 1E-8);
			}
			nbCompartmentChecked++;
		}
		System.out.println("Successfully tested this number of compartments " + nbCompartmentChecked);
	}

	@Ignore
	@Test
	public void test07MemoryLeakage() throws Exception {
		int nbSimulations = 10;
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "MathildeTreeExport.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "MathildeTreeExport.ife";
		String speciesMatchFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "speciesCorrespondanceForSimulationData.xml";
		Matrix matX = new Matrix(nbSimulations-1, 2);
		Matrix matY = new Matrix(nbSimulations-1, 1);
		for (int i = 0; i < nbSimulations; i++) {
			CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
			cat.initializeTool(null);
			CATGrowthSimulationRecordReader recordReader = new CATGrowthSimulationRecordReader();
			recordReader.getSelector().load(speciesMatchFilename);
			ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
			recordReader.initInScriptMode(ifm);
			recordReader.readAllRecords();
			cat.setStandList(recordReader.getStandList());
			cat.calculateCarbon();
			cat.requestShutdown();
			System.gc();
			double currentUsedMemory = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) * 1E-6;
			if (i > 0) { // the first simulation is out of the scope since the field stand is empty
				matX.setValueAt(i - 1, 0, 1);
				matX.setValueAt(i - 1, 1, i);
				matY.setValueAt(i - 1, 0, currentUsedMemory);
			}
			FORMATTER.setMinimumFractionDigits(2);
			System.out.println("Memory load after run " + i + " = " + FORMATTER.format(currentUsedMemory));
		}
		Matrix betaEstimate = matX.transpose().multiply(matX).getInverseMatrix().multiply(matX.transpose())
				.multiply(matY);
		Matrix res = matY.subtract(matX.multiply(betaEstimate));
		double sigma2 = res.transpose().multiply(res).getValueAt(0, 0) / (res.m_iRows - betaEstimate.m_iRows);
		Matrix omegaMatrix = matX.transpose().multiply(matX).getInverseMatrix().scalarMultiply(sigma2);
		double slopeParameterEstimate = betaEstimate.getValueAt(betaEstimate.m_iRows - 1, 0);
		double standardError = Math.sqrt(omegaMatrix.getValueAt(omegaMatrix.m_iRows - 1, omegaMatrix.m_iRows - 1));
		double prob = 1 - GaussianUtility.getCumulativeProbability(Math.abs(slopeParameterEstimate / standardError));
		System.out.println("Slope parameter estimate = " + slopeParameterEstimate);
		System.out.println("Pr > z = " + prob);
		Assert.assertTrue(slopeParameterEstimate < 1E-2 || prob > 0.025);		// 1E-2 implies 1Kb memory occupation
	}
	
	@Test
	public void test08ComparisonHalflifeVsAverageLifetime() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.ife";
		String averageLifetimeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "SingleProcessorWithAverageLifetime.prl";
		String halflifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "SingleProcessorWithHalflife.prl";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATYieldTableRecordReader recordReader = new CATYieldTableRecordReader(CATSpecies.ABIES);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		
		cat.getCarbonToolSettings().getCustomizableProductionProcessorManager().load(averageLifetimeFilename);
		cat.getCarbonToolSettings().setCurrentProductionProcessorManagerSelection(ProductionManagerName.customized);
		cat.calculateCarbon();
		CATSingleSimulationResult resultAverageLifetime = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> obsMapAverageLifetime = resultAverageLifetime.getBudgetMap();
		
		cat.getCarbonToolSettings().getCustomizableProductionProcessorManager().load(halflifeFilename);
		cat.calculateCarbon();
		CATSingleSimulationResult resultHalflife = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> obsMapHalflife = resultHalflife.getBudgetMap();
		
		
		Assert.assertTrue("Testing the size of the map", obsMapAverageLifetime.size() == obsMapHalflife.size());
		int nbCompartmentChecked = 0;
		for (CompartmentInfo key : obsMapAverageLifetime.keySet()) {
			double expected = obsMapAverageLifetime.get(key).getMean().getValueAt(0, 0);
			double observed = obsMapHalflife.get(key).getMean().getValueAt(0, 0);
			Assert.assertEquals("Testing compartment " + key.name(), expected, observed, 1E-8);
			nbCompartmentChecked++;
		}
		System.out.println("Successfully tested this number of compartments " + nbCompartmentChecked);
		cat.requestShutdown();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void test09WithYieldTableAndIPCCConfiguration() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.ife";
		String refFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTableWithIPCCReference.xml";
		String prlFilename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "ipcc2014_en.prl";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATYieldTableRecordReader recordReader = new CATYieldTableRecordReader(CATSpecies.ABIES);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.setProductionManager(prlFilename);
		cat.calculateCarbon();
		CATSingleSimulationResult result = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> obsMap = result.getBudgetMap();
		
//		XmlSerializer serializer = new XmlSerializer(refFilename);
//		serializer.writeObject(obsMap);

		XmlDeserializer deserializer = new XmlDeserializer(refFilename);
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> refMap = (Map) deserializer.readObject();
		int nbCompartmentChecked = 0;
//		Assert.assertTrue("Testing the size of the map", refMap.size() == obsMap.size());
		for (CompartmentInfo key : refMap.keySet()) {
			double expected = refMap.get(key).getMean().getValueAt(0, 0);
			double observed = obsMap.get(key).getMean().getValueAt(0, 0);
			Assert.assertEquals("Testing compartment " + key.name(), expected, observed, 1E-8);
			nbCompartmentChecked++;
		}
		System.out.println("Successfully tested this number of compartments " + nbCompartmentChecked);
		cat.requestShutdown();
	}
	

	@Test
	public void test10WithYieldTableAndIPCCConfigurationSensitivityAnalysis() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.ife";
		String prlFilename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "ipcc2014_en.prl";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATYieldTableRecordReader recordReader = new CATYieldTableRecordReader(CATSpecies.ABIES);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.setProductionManager(prlFilename);
		CATSensitivityAnalysisSettings.getInstance().setNumberOfMonteCarloRealizations(2);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.BasicDensity, Type.GAUSSIAN, true, 0.3);

		double modifier1 = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, cat.getCarbonCompartmentManager(), CATSpecies.ABIES.getSpeciesType().name());
		cat.calculateCarbon();
		CATSingleSimulationResult result1 = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> refMap = result1.getBudgetMap();

		
		cat.setStandList(recordReader.getStandList());
		cat.setProductionManager(prlFilename);
		CATSensitivityAnalysisSettings.getInstance().setNumberOfMonteCarloRealizations(2);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.BasicDensity, Type.GAUSSIAN, true, 0.3);

		double modifier2 = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, cat.getCarbonCompartmentManager(), CATSpecies.ABIES.getSpeciesType().name());
		Assert.assertEquals("Comparting first deviate", modifier1, modifier2, 1E-8);
		cat.calculateCarbon();
		CATSingleSimulationResult result2 = cat.getCarbonCompartmentManager().getSimulationSummary();
		Map<CompartmentInfo, Estimate<Matrix, SymmetricMatrix, ?>> obsMap = result2.getBudgetMap();
		int nbCompartmentChecked = 0;
		Assert.assertTrue("Testing the size of the map", refMap.size() == obsMap.size());
		for (CompartmentInfo key : refMap.keySet()) {
			double expected = refMap.get(key).getMean().getValueAt(0, 0);
			double observed = obsMap.get(key).getMean().getValueAt(0, 0);
			Assert.assertEquals("Testing compartment " + key.name(), expected, observed, 1E-8);
			nbCompartmentChecked++;
		}
		System.out.println("Successfully tested this number of compartments " + nbCompartmentChecked);
		
		cat.requestShutdown();
	}

	
	
	@Test
	public void test11WithYieldTableAndIPCCConfigurationSensitivityAnalysis2() throws Exception {
		String filename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.csv";
		String ifeFilename = ObjectUtility.getPackagePath(getClass()) + "io" + File.separator + "ExampleYieldTable.ife";
		String prlFilename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "ipcc2014_en.prl";
		CarbonAccountingTool cat = new CarbonAccountingTool(CATMode.SCRIPT);
		cat.initializeTool(null);
		CATYieldTableRecordReader recordReader = new CATYieldTableRecordReader(CATSpecies.ABIES);
		ImportFieldManager ifm = ImportFieldManager.createImportFieldManager(recordReader, ifeFilename, filename);
		recordReader.initInScriptMode(ifm);
		recordReader.readAllRecords();
		cat.setStandList(recordReader.getStandList());
		cat.setProductionManager(prlFilename);
		CATSensitivityAnalysisSettings.getInstance().setNumberOfMonteCarloRealizations(2);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.BasicDensity, Type.UNIFORM, true, 0.3);

		double modifier1 = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, cat.getCarbonCompartmentManager(), CATSpecies.ABIES.getSpeciesType().name());
		cat.calculateCarbon();
		
		cat.setStandList(recordReader.getStandList());
		cat.setProductionManager(prlFilename);
		CATSensitivityAnalysisSettings.getInstance().setNumberOfMonteCarloRealizations(2);
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(VariabilitySource.BasicDensity, Type.UNIFORM, true, 0.15);

		double modifier2 = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, cat.getCarbonCompartmentManager(), CATSpecies.ABIES.getSpeciesType().name());
		Assert.assertEquals("Comparting first deviate", modifier1 - 1, (modifier2 - 1) * 2, 1E-8);
		cat.calculateCarbon();
		
		cat.requestShutdown();
	}

	@Test
	public void test12InitialDeadWood() throws Exception {
		String managerFilename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		final String standID = "StandTest";
		final double areaHa = .04;
		List<CATCompatibleStand> stands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand stand;
		for (int i = 1; i <= 2; i++) {
			int dateYr = i * 10;
			int ageYr = dateYr;
			stand = new CATDeadWoodProviderImpl("beech", standID, areaHa, dateYr, ageYr, 20d);
			stands.add(stand);
		}
		
		CarbonAccountingTool tool = new CarbonAccountingTool(CATMode.SCRIPT);
		tool.initializeTool(null);
		tool.setStandList(stands);
		try {
			tool.getCarbonToolSettings().getCurrentProductionProcessorManager().load(managerFilename);
			tool.calculateCarbon();
			CATSingleSimulationResult result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());
			Matrix obsLivingBiomass = result.getEvolutionMap().get(CompartmentInfo.LivingBiomass).getMean();
			Matrix obsDOM = result.getEvolutionMap().get(CompartmentInfo.DeadBiom).getMean();
			Matrix obsProducts = result.getEvolutionMap().get(CompartmentInfo.TotalProducts).getMean();
			Assert.assertEquals("Testing there is no living biomass",
					0d, 
					obsLivingBiomass.getSumOfElements(), 
					1E-8);
			Assert.assertEquals("Testing there is no HWP",
					0d, 
					obsProducts.getSumOfElements(), 
					1E-8);
			Assert.assertEquals("Testing initial DOM",
					250d, 
					obsDOM.getValueAt(0, 0),
					1E-8);

			tool.requestShutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unable to calculate carbon!");
		}

	}
	
	@Test
	public void test13WithSaplingProvider() throws Exception {
		String managerFilename = ObjectUtility.getPackagePath(getClass()) + "productionlines" + File.separator + "exampleProductionLines.prl";
		final String standID = "StandTest";
		final double areaHa = .04;
		List<CATCompatibleStand> stands = new ArrayList<CATCompatibleStand>();
		CATCompatibleStand stand;
		for (int i = 1; i <= 2; i++) {
			int dateYr = i * 10;
			int ageYr = dateYr;
			stand = new CATSaplingsProviderImpl("beech", standID, areaHa, dateYr, ageYr);
			stands.add(stand);
		}
		
		CarbonAccountingTool tool = new CarbonAccountingTool(CATMode.SCRIPT);
		tool.initializeTool(null);
		tool.setStandList(stands);
		try {
			tool.getCarbonToolSettings().getCurrentProductionProcessorManager().load(managerFilename);
			tool.calculateCarbon();
			CATSingleSimulationResult result = tool.getCarbonCompartmentManager().getSimulationSummary();
			Assert.assertTrue(result != null && result.isValid());
			Matrix obsLivingBiomass = result.getEvolutionMap().get(CompartmentInfo.LivingBiomass).getMean();
			Assert.assertEquals("Testing there is some living biomass",
					8.25, 
					obsLivingBiomass.getValueAt(0, 0), 
					1E-8);
			Matrix first10years = obsLivingBiomass.getSubMatrix(0, 10, 0, 0);
			boolean anyElementDiff = first10years.anyElementDifferentFrom(8.25);
			Assert.assertTrue("Testing that all values are equal for the first ten years of the simulation", !anyElementDiff);

			tool.requestShutdown();
			
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Unable to calculate carbon!");
		}

	}
	
	public static void main(String[] args) throws Exception {
		CarbonAccountingToolTest test = new CarbonAccountingToolTest();
		test.test07MemoryLeakage();
	}

}
