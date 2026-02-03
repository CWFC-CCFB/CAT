package lerfob.carbonbalancetool.pythonaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import lerfob.carbonbalancetool.productionlines.CarbonUnit;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.CarbonUnitList;
import lerfob.carbonbalancetool.productionlines.ProductionLinesTest.CATCompatibleTreeImpl;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.treelogger.douglasfirfcba.DouglasFCBALogCategory;
import lerfob.treelogger.douglasfirfcba.DouglasFCBATreeLogger;
import lerfob.treelogger.douglasfirfcba.DouglasFCBATreeLoggerParameters;
import repicea.serial.xml.XmlDeserializer;
import repicea.serial.xml.XmlSerializer;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;
import repicea.simulation.treelogger.TreeLogger;
import repicea.simulation.treelogger.WoodPiece;
import repicea.util.ObjectUtility;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PythonAccessDouglasFirTest {

	static double ResiduesLeftInForest;
	
	@SuppressWarnings("rawtypes")
	private synchronized static Map getInputMap() throws Exception {
		if (PythonAccessTest.InputMap == null) {
			String filename = ObjectUtility.getPackagePath(PythonAccessTest.class) + "testMapPythonAccess.ref";
			XmlDeserializer deserializer = new XmlDeserializer(filename);
			PythonAccessTest.InputMap = (Map) deserializer.readObject();
		}
		return PythonAccessTest.InputMap;
	}

	
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void test3CompleteWithDouglasFir() throws Exception {
		String refMapFilename = ObjectUtility.getPackagePath(PythonAccessTest.class) + "referenceDouglas.ref";
		
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("douglas");
		pap.setAreaHA(0.1);
		Map inputMap = getInputMap();
		Map<Integer, Map<String, Double>> resultingMap = pap.processStandList("exampleDouglas", inputMap);
		double hwpCarbonIn2013 = resultingMap.get(2013).get("CurrentCarbonHWPMgHa");
		System.out.println("HWP carbon in 2013 = " + hwpCarbonIn2013);
		XmlSerializer serializer = new XmlSerializer(refMapFilename);
		serializer.writeObject(resultingMap);
		
		XmlDeserializer deserializer = new XmlDeserializer(refMapFilename);
		Map<?,?> refMap = (Map) deserializer.readObject();
		
		Collection<Integer> yearsWithBiomass = new ArrayList<Integer>();
		yearsWithBiomass.add(2010);
		yearsWithBiomass.add(2011);
		yearsWithBiomass.add(2012);
		
		Assert.assertEquals(refMap.size(), resultingMap.size());
		int nbValuesCompared = 0;
		Set<Integer> dates = resultingMap.keySet();
		for (Integer key : dates) {
			Map<String, Double> innerResultingMap = resultingMap.get(key);
			Map<?,?> innerRefMap = (Map) refMap.get(key);
			if (innerRefMap == null) {
				Assert.fail();
			} else {
				Assert.assertEquals(innerRefMap.size(), innerResultingMap.size());
			}
			double totalBiomass = 0;
			for (String key2 : innerResultingMap.keySet()) {
				Double resultingValue = innerResultingMap.get(key2);
				Double refValue = (Double) innerRefMap.get(key2);
				if (refValue == null) {
					Assert.fail();
				} else {
					Assert.assertEquals("Comparing " + key2, refValue, resultingValue, 1E-8);
					nbValuesCompared++;
//					if (key2.startsWith("BiomassMgHa") && !key2.contains("STUMPS") && !key2.contains("BRANCHES")) {
					if (key2.startsWith("BiomassMgHa")) {
						totalBiomass += resultingValue;
					}
				}
			}
			System.out.println("Total biomass in HWP at year " + key + " - " + totalBiomass);
			if (yearsWithBiomass.contains(key)) {
				Assert.assertEquals("Comparing biomass for year " + key, 100 - ResiduesLeftInForest, totalBiomass, 1E-8);
			} 
		}
		System.out.println("Successfully compared this number of values: " + nbValuesCompared);
	}

	@Test
	public void test1WithDouglasFirLoggingOnly() throws Exception {
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("douglas");
		pap.setAreaHA(0.1);
	
		TreeLogger<?,?> manager = pap.getCarbonToolSettings().getTreeLogger();
		PythonDouglasFirTree tree = new PythonDouglasFirTree(StatusClass.cut,
				.1,
				PythonAccessPoint.getAverageDryBiomassByTree(0, 1),
				PythonAccessPoint.getAverageDryBiomassByTree(10d, 1),
				PythonAccessPoint.getAverageDryBiomassByTree(0, 1),
				45,
				7.45);

		Collection<PythonDouglasFirTree> trees = new ArrayList<PythonDouglasFirTree>();
		trees.add(tree);
		manager.init(trees);
		manager.run();
		double volume = 0;
		for (Collection<WoodPiece> woodPieces : manager.getWoodPieces().values()) {
			for (WoodPiece woodPiece : woodPieces) {
				if (woodPiece.getLogCategory().getName().equals("Residues")) {
					ResiduesLeftInForest = woodPiece.getWeightedTotalVolumeM3() * tree.getBasicWoodDensity() * .67 / pap.areaHa;	// kept in a static variable to be later deduced from the total in the complete test since a part of this log category is left on the forest floor
				}
				volume += woodPiece.getWeightedTotalVolumeM3();
			}
		}
		double biomass = volume * tree.getBasicWoodDensity() / pap.areaHa;
		Assert.assertEquals("Comparing logged biomasses", 100d, biomass, 1E-8);
	}		

	@Test
	public void test2WithDouglasFirProductionLinesOnly() throws Exception {
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("douglas");
		pap.setAreaHA(0.1);
	
		ProductionProcessorManager manager = pap.getCarbonToolSettings().getCurrentProductionProcessorManager();
		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, 100d);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);
		DouglasFCBATreeLogger treeLogger = (DouglasFCBATreeLogger) manager.getSelectedTreeLogger();

		DouglasFCBATreeLoggerParameters loggerParams = (DouglasFCBATreeLoggerParameters) treeLogger.getTreeLoggerParameters();
		for (DouglasFCBALogCategory logCategory : loggerParams.getLogCategoryList()) {
			manager.resetCarbonUnitMap();
			manager.processWoodPiece(logCategory, 0, "", amountMaps, new CATCompatibleTreeImpl(Species.Pseudotsuga_menziesii, SpeciesLocale.IPCC), StatusClass.cut);
			double volume = 0;
			for (CarbonUnitStatus type : CarbonUnitStatus.values()) {
				CarbonUnitList list = manager.getCarbonUnits(type);
				for (CarbonUnit	unit : list) {
					volume += unit.getAmountMap().get(Element.Volume);
				}
			}
			Assert.assertEquals("Comparing " + logCategory.getName(), 100d, volume, 1E-8);
			System.out.println("Comparing " + logCategory.getName() + " expected = " + 100 + " ; actual = " + volume);
		}
	}		

}
