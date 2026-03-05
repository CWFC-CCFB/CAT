package lerfob.carbonbalancetool.pythonaccess;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompatibleStand;
import lerfob.carbonbalancetool.catdiameterbasedtreelogger.CATDiameterBasedTreeLogger;
import lerfob.carbonbalancetool.catdiameterbasedtreelogger.CATDiameterBasedTreeLoggerParameters;
import lerfob.carbonbalancetool.productionlines.CarbonUnit;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import lerfob.carbonbalancetool.productionlines.CarbonUnitList;
import lerfob.carbonbalancetool.productionlines.ProductionLinesTest.CATCompatibleTreeImpl;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.treelogger.diameterbasedtreelogger.DiameterBasedTreeLogCategory;
import repicea.serial.xml.XmlDeserializer;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.simulation.species.REpiceaSpecies.SpeciesLocale;
import repicea.simulation.treelogger.TreeLogger;
import repicea.simulation.treelogger.WoodPiece;
import repicea.util.ObjectUtility;

public class PythonAccessOakTest {

	@SuppressWarnings("rawtypes")
	private synchronized static Map getInputMap() throws Exception {
		if (PythonAccessTest.InputMap == null) {
			String filename = ObjectUtility.getPackagePath(PythonAccessTest.class) + "testMapPythonAccess.ref";
			XmlDeserializer deserializer = new XmlDeserializer(filename);
			PythonAccessTest.InputMap = (Map) deserializer.readObject();
		}
		return PythonAccessTest.InputMap;
	}

	@Test
	public void testStandListGeneration() throws Exception {
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("oak");
		pap.setAreaHA(0.1);
		List<CATCompatibleStand> stands = pap.createStandList("exampleOak", getInputMap());
		for (CATCompatibleStand stand : stands) {
			if (stand.getDateYr() <= 2012) {
				double sumBiomass = 0;
				for (Object o : stand.getTrees(StatusClass.cut)) {
					PythonCarbonToolCompatibleTree t = (PythonCarbonToolCompatibleTree) o;
					sumBiomass += t.getCommercialVolumeM3() * t.getBasicWoodDensity() * t.getNumber();
				}
				Assert.assertEquals("Comparing biomass for year " + stand.getDateYr(), 10, sumBiomass, 1E-8);
			}
		}
	}
	
	@SuppressWarnings({ "rawtypes" })
	@Test
	public void testCompleteWithOak() throws Exception {
		String refMapFilename = ObjectUtility.getPackagePath(PythonAccessTest.class) + "referenceOak.ref";
		
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("oak");
		pap.setAreaHA(0.1);
		
		Map<Integer, Map<String, Double>> resultingMap = pap.processStandList("exampleOak", getInputMap());
//		XmlSerializer serializer = new XmlSerializer(refMapFilename);
//		serializer.writeObject(resultingMap);
		
		Collection<Integer> yearsWithBiomass = new ArrayList<Integer>();
		yearsWithBiomass.add(2010);
		yearsWithBiomass.add(2011);
		yearsWithBiomass.add(2012);

		XmlDeserializer deserializer = new XmlDeserializer(refMapFilename);
		Map<?,?> refMap = (Map) deserializer.readObject();
		
		Assert.assertEquals(refMap.size(), resultingMap.size());
		int nbValuesCompared = 0;
		for (Integer key : resultingMap.keySet()) {
			Map<String, Double> innerResultingMap = resultingMap.get(key);
			innerResultingMap.remove("BiomassMgHaINDUSTRIAL_RESIDUES");  // This class was added after the test was created
			Map<?,?> innerRefMap = (Map) refMap.get(key);
			if (innerRefMap == null) {
				Assert.fail();
			} 
			double totalBiomass = 0;
			for (String key2 : innerResultingMap.keySet()) {
				Double resultingValue = innerResultingMap.get(key2);
				Double refValue = (Double) innerRefMap.get(key2.contains(UseClass.INTERNAL_CONSUMPTION.name()) ?
						key2.replace(UseClass.INTERNAL_CONSUMPTION.name(), "NONE") :
							key2);
				if (refValue == null) {
					Assert.fail();
				} else {
					Assert.assertEquals("Comparing " + key2, refValue, resultingValue, 1E-8);
					nbValuesCompared++;
					if (key2.startsWith("BiomassMgHa") && !key2.contains("STUMPS") && !key2.contains("BRANCHES")) {
						totalBiomass += resultingValue;
					}
				}
			}
			System.out.println("Total biomass in HWP at year " + key + " - " + totalBiomass);
			if (yearsWithBiomass.contains(key)) {
				Assert.assertEquals("Comparing biomass for year " + key, 100, totalBiomass, 1E-8);
			}
		}
		System.out.println("Successfully compared this number of values: " + nbValuesCompared);
	}

	
	@Test
	public void testWithOakProductionLinesOnly() throws Exception {
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("oak");
		pap.setAreaHA(0.1);
	
		ProductionProcessorManager manager = pap.getCarbonToolSettings().getCurrentProductionProcessorManager();
		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, 100d);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);
		
		CATDiameterBasedTreeLogger treeLogger = (CATDiameterBasedTreeLogger) manager.getSelectedTreeLogger();
		CATDiameterBasedTreeLoggerParameters loggerParams = (CATDiameterBasedTreeLoggerParameters) treeLogger.getTreeLoggerParameters();
		for (DiameterBasedTreeLogCategory logCategory : loggerParams.getLogCategoryList()) {
			manager.resetCarbonUnitMap();
			manager.processWoodPiece(logCategory, 0, "", amountMaps, new CATCompatibleTreeImpl(Species.Quercus_spp, SpeciesLocale.IPCC), StatusClass.cut);
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

	
	@Test
	public void testWithOakLoggingOnly() throws Exception {
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("oak");
		pap.setAreaHA(0.1);
	
		TreeLogger<?,?> manager = pap.getCarbonToolSettings().getTreeLogger();
		
		PythonOakTree tree = new PythonOakTree(StatusClass.cut,
				.1,
				PythonAccessPoint.getAverageDryBiomassByTree(0, 1),
				PythonAccessPoint.getAverageDryBiomassByTree(10d, 1),
				PythonAccessPoint.getAverageDryBiomassByTree(0, 1),
				45,
				7.45);

		Collection<PythonOakTree> trees = new ArrayList<PythonOakTree>();
		trees.add(tree);
		manager.init(trees);
		manager.run();
		double volume = 0;
		for (Collection<WoodPiece> woodPieces : manager.getWoodPieces().values()) {
			for (WoodPiece woodPiece : woodPieces) {
				volume += woodPiece.getWeightedTotalVolumeM3();
			}
		}
		double biomass = volume * Species.Quercus_spp.getBasicWoodDensity(SpeciesLocale.IPCC) / pap.areaHa;
		Assert.assertEquals("Comparing logged biomasses", 100d, biomass, 1E-8);
	}		

}
