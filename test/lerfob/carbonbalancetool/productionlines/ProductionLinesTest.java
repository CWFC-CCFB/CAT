package lerfob.carbonbalancetool.productionlines;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import lerfob.carbonbalancetool.productionlines.WoodyDebrisProcessor.WoodyDebrisProcessorID;
import repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider.SpeciesType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.Processor;
import repicea.simulation.species.REpiceaSpecies.Species;
import repicea.util.ObjectUtility;

@SuppressWarnings("deprecation")
public class ProductionLinesTest {

	public static class CATCompatibleTreeImpl implements CATCompatibleTree {

		final Species species;
		
		public CATCompatibleTreeImpl(Species species) {
			this.species = species;
		}
		
		@Override
		public double getCommercialVolumeM3() {
			return 0;
		}

		@Override
		public boolean isCommercialVolumeOverbark() {
			return true;
		}

		@Override
		public String getSpeciesName() {
			return species.name();
		}

		@Override
		public Species getCATSpecies() {return species;}
		
	}
	
	
	/**
	 * Tests if a production line file can be read successfully.
	 */
	@Test
	public void test01ProductionLineDeserialisation() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + "oakProductionLines20121112.prl";
			ProductionLineManager wpmm = new ProductionLineManager();
			wpmm.load(filename);
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test02BiomassBalanceInProductionLines() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + "oakProductionLines20121112.prl";
			ProductionLineManager wpmm = new ProductionLineManager();
			wpmm.load(filename);

			double volume = 1d;
			double carbonContent = .5;
			double basicWoodDensity = .5;

			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume);
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);

			for (String productionLine : wpmm.getProductionLineNames()) {
				System.out.println("Testing " + productionLine);
				wpmm.resetCarbonUnitMap();
				wpmm.processWoodPiece(productionLine, 2010, Species.Fagus_sylvatica.name(), Species.Fagus_sylvatica.getSpeciesType(), StatusClass.cut, amountMap);
				CarbonUnitList list = new CarbonUnitList();
				for (CarbonUnitStatus type : CarbonUnitStatus.values()) {
					list.addAll(wpmm.getCarbonUnits(type));
				}

				double totalVolume = 0d;
				for (CarbonUnit unit : list) {
					totalVolume += unit.getAmountMap().get(Element.Volume);
				}

				Assert.assertEquals("Test for production line : " + productionLine,
						volume, 
						totalVolume, 
						1E-12);


			}			

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 

	}

	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@SuppressWarnings("rawtypes")
	@Test
	public void test03BiomassBalanceInProductionLinesWithFormerNewImplementation() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + "oakProductionLines20121112.prl";
			ProductionLineManager wpmm = new ProductionLineManager();
			wpmm.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;

			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume);
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			
			CarbonUnit carbonUnit = new CarbonUnit(2013, "", null, amountMap, Species.Abies_spp.name(), Species.Abies_spp.getSpeciesType(), StatusClass.cut, BiomassType.Wood, null); // last null stands for woodyDebrisType 
			
			int index = wpmm.getProductionLineNames().indexOf("Sawing");
			if (index == -1) {
				throw new Exception("This production line does not exist : " + "Sawing");
			} else {
				ProductionLine model = wpmm.getContent().get(index);
				Processor processor = model.getPrimaryProcessor();
				List<ProcessUnit> processUnits = new ArrayList<ProcessUnit>();
				processUnits.add(carbonUnit);
				Collection<ProcessUnit> outputUnits = processor.doProcess(processUnits);
				
				double totalVolume = 0d;
				for (ProcessUnit unit : outputUnits) {
					totalVolume += ((CarbonUnit) unit).getAmountMap().get(Element.Volume);
				}

				Assert.assertEquals("Test for production line : " + "Sawing",
						volume, 
						totalVolume, 
						1E-12);
			}			

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 


	}


	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test04BiomassBalanceInProductionLinesWithNewImplementation() {
		try {
			String filename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "hardwood_simple_en.prl";
			ProductionProcessorManager processorManager = new ProductionProcessorManager();
			processorManager.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;

			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume);
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, amountMap);

			List<Processor> processors = processorManager.getPrimaryProcessors();
			int i = processors.size() - 1;
			LogCategoryProcessor sawingProcessor = null;
			while (i >= 0) {
				Processor p = processors.get(i);
				if (p instanceof LogCategoryProcessor) {
					sawingProcessor = (LogCategoryProcessor) p;
					break;
				}
				i--;
			}

//			Collection<CarbonUnit> endProducts = processorManager.processWoodPiece(sawingProcessor.logCategory, 2015, amountMap);
			processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
			Collection<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
			for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
				endProducts.addAll(processorManager.getCarbonUnits(status));
			}
			double totalVolume = 0d;
			for (CarbonUnit unit : endProducts) {
				totalVolume += unit.getAmountMap().get(Element.Volume);
			}

			Assert.assertEquals("Test for production line : " + "Sawing",
					volume, 
					totalVolume, 
					1E-12);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 

	}

	
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static void testBiomassBalanceInProductionLinesWithNewImplementationAndDebarkingUsingThisFile(String filename) {
		try {
			ProductionProcessorManager processorManager = new ProductionProcessorManager();
			processorManager.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;
			double barkProportion = 0.11;
			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume * (1-barkProportion));
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, amountMap);

			amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume * (barkProportion));
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			amountMaps.put(BiomassType.Bark, amountMap);

			List<Processor> processors = processorManager.getPrimaryProcessors();
			List<LogCategoryProcessor> logCategoryProcessors = (List) processors.stream()
					.filter(l -> l instanceof LogCategoryProcessor)	// we keep only the entries with values set to true
					.collect(Collectors.toList());

			for (LogCategoryProcessor logCategoryProcessor : logCategoryProcessors) {
				processorManager.resetCarbonUnitMap();
				processorManager.validate();
				processorManager.processWoodPiece(logCategoryProcessor.getFirstLogCategory(), 2015, "", amountMaps,  new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);			
				Collection<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
				for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
					endProducts.addAll(processorManager.getCarbonUnits(status));
				}
				double totalVolumeWood = 0d;
				double totalVolumeBark = 0d;
				for (CarbonUnit unit : endProducts) {
					if (unit.getBiomassType() == BiomassType.Wood) {
						totalVolumeWood += unit.getAmountMap().get(Element.Volume);
					} else {
						totalVolumeBark += unit.getAmountMap().get(Element.Volume);
					}
				}

				Assert.assertEquals("Test for production line : " + "Sawing",
						volume * (1 - barkProportion), 
						totalVolumeWood, 
						1E-12);
				Assert.assertEquals("Test for production line : " + "Sawing",
						volume * (barkProportion), 
						totalVolumeBark, 
						1E-12);
			}

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 

	}
	
	
	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test05BiomassBalanceInProductionLinesWithNewImplementationAndDebarking() {
		String filename = ObjectUtility.getPackagePath(getClass()) + "testHardwoodSimpleWithDebarking.prl";
		testBiomassBalanceInProductionLinesWithNewImplementationAndDebarkingUsingThisFile(filename);
	}

	/**
	 * Testing Quebec industrial sector 
	 */
	@Test
	public void test06BiomassBalanceInProductionLinesOfQuebecIndustrialSector() {
		String filename = ObjectUtility.getPackagePath(getClass()) + "QuebecIndustrialSectorReference.prl";
		testBiomassBalanceInProductionLinesWithNewImplementationAndDebarkingUsingThisFile(filename);
	}

	
	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test07BiomassBalanceInWoodyDebris() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + "testHardwoodSimpleWithDebarking.prl";
			ProductionProcessorManager processorManager = new ProductionProcessorManager();
			processorManager.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;
			double barkProportion = 0.11;
			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume * (1-barkProportion));
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, amountMap);

			amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume * (barkProportion));
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			amountMaps.put(BiomassType.Bark, amountMap);

			processorManager.processWoodyDebris(2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut, WoodyDebrisProcessorID.CommercialWoodyDebris);
			Collection<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
			for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
				endProducts.addAll(processorManager.getCarbonUnits(status));
			}
			double totalVolumeWood = 0d;
			double totalVolumeBark = 0d;
			for (CarbonUnit unit : endProducts) {
				if (unit.getBiomassType() == BiomassType.Wood) {
					totalVolumeWood += unit.getAmountMap().get(Element.Volume);
				} else {
					totalVolumeBark += unit.getAmountMap().get(Element.Volume);
				}
			}

			Assert.assertEquals("Test for production line : " + "Sawing",
					volume * (1 - barkProportion), 
					totalVolumeWood, 
					1E-12);
			Assert.assertEquals("Test for production line : " + "Sawing",
					volume * (barkProportion), 
					totalVolumeBark, 
					1E-12);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 
	}

	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test08CumulativeEmissionsWithValueForEachProcess() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + File.separator
					+ "testHardwood_simple_enWithEmissions.prl";
			ProductionProcessorManager processorManager = new ProductionProcessorManager();
			processorManager.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;

			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume);
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, amountMap);
			
			List<Processor> processors = processorManager.getPrimaryProcessors();
			int i = processors.size() - 1;
			LogCategoryProcessor sawingProcessor = null;
			while (i >= 0) {
				Processor p = processors.get(i);
				if (p instanceof LogCategoryProcessor) {
					sawingProcessor = (LogCategoryProcessor) p;
					break;
				}
				i--;
			}
//			Collection<CarbonUnit> endProducts = processorManager.processWoodPiece(sawingProcessor.logCategory, 2015, amountMap);
			processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
				
			Collection<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
			for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
				endProducts.addAll(processorManager.getCarbonUnits(status));
			}

			double actualEmissions = 0d;
			for (CarbonUnit unit : endProducts) {
				actualEmissions += unit.getAmountMap().get(Element.EmissionsCO2Eq);
			}

			double expectedEmissions = 2.5;
			Assert.assertEquals("Test for production line : " + "Sawing",
					expectedEmissions, 
					actualEmissions, 
					1E-12);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 

	}

	/**
	 * Tests if the amount in is equal to the amount out
	 */
	@Test
	public void test09CumulativeEmissionsWithValueForEachProcess2() {
		try {
			String filename = ObjectUtility.getPackagePath(getClass()) + File.separator
					+ "testHardwood_simple_enWithEmissions2.prl";
			ProductionProcessorManager processorManager = new ProductionProcessorManager();
			processorManager.load(filename);

			double volume = 100d;
			double carbonContent = .5;
			double basicWoodDensity = .5;

			AmountMap<Element> amountMap = new AmountMap<Element>();
			amountMap.put(Element.Volume, volume);
			amountMap.put(Element.Biomass, volume * basicWoodDensity);
			amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
			Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
			amountMaps.put(BiomassType.Wood, amountMap);
			
			List<Processor> processors = processorManager.getPrimaryProcessors();
			int i = processors.size() - 1;
			LogCategoryProcessor sawingProcessor = null;
			while (i >= 0) {
				Processor p = processors.get(i);
				if (p instanceof LogCategoryProcessor) {
					sawingProcessor = (LogCategoryProcessor) p;
					break;
				}
				i--;
			}
//			Collection<CarbonUnit> endProducts = processorManager.processWoodPiece(sawingProcessor.logCategory, 2015, amountMap);
			processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
				
			Collection<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
			for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
				endProducts.addAll(processorManager.getCarbonUnits(status));
			}

			double actualEmissions = 0d;
			for (CarbonUnit unit : endProducts) {
				actualEmissions += unit.getAmountMap().get(Element.EmissionsCO2Eq);
			}

			double expectedEmissions = 8.5;
			Assert.assertEquals("Test for production line : " + "Sawing",
					expectedEmissions, 
					actualEmissions, 
					1E-12);

		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} 

	}


	@Test
	public void test10BroadleavedSorting1() throws IOException {
		ProductionProcessorManager processorManager = new ProductionProcessorManager();
		String filename = ObjectUtility.getRelativePackagePath(getClass()) + "testHardwoodRecyclingWithBroadleavedSorting.prl";
		processorManager.load(filename);

		double volume = 100d;
		double carbonContent = .5;
		double basicWoodDensity = .5;

		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, volume);
		amountMap.put(Element.Biomass, volume * basicWoodDensity);
		amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);

		List<Processor> processors = processorManager.getPrimaryProcessors();
		int i = processors.size() - 1;
		LogCategoryProcessor sawingProcessor = null;
		while (i >= 0) {
			Processor p = processors.get(i);
			if (p instanceof LogCategoryProcessor) {
				sawingProcessor = (LogCategoryProcessor) p;
				break;
			}
			i--;
		}

		processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
		processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Fagus_sylvatica), StatusClass.cut);
		List<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
		for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
			endProducts.addAll(processorManager.getCarbonUnits(status));
		}
		
		double sumVolumeStaves = 0d;
		double sumVolumeConstruction = 0d;
		for (CarbonUnit unit : endProducts) {
			if (unit instanceof EndUseWoodProductCarbonUnit) {
				if (((EndUseWoodProductCarbonUnit) unit).getCarbonUnitFeature().getUseClass() == UseClass.BARREL) {
					sumVolumeStaves += ((EndUseWoodProductCarbonUnit) unit).getBiomassMgAtCreationDate();
				} else if (((EndUseWoodProductCarbonUnit) unit).getCarbonUnitFeature().getUseClass() == UseClass.BUILDING) {
					sumVolumeConstruction += ((EndUseWoodProductCarbonUnit) unit).getBiomassMgAtCreationDate();
				}
			}
		}
		
		Assert.assertEquals("Testing volume of barrels", 50d, sumVolumeStaves, 1E-8);
		Assert.assertEquals("Testing volume of construction", 20d, sumVolumeConstruction, 1E-8);
	}

	
	@Test
	public void test11BroadleavedSorting2() throws IOException {
		ProductionProcessorManager processorManager = new ProductionProcessorManager();
		String filename = ObjectUtility.getRelativePackagePath(getClass()) + "testHardwoodRecyclingWithBroadleavedSorting.prl";
		processorManager.load(filename);

		double volume = 100d;
		double carbonContent = .5;
		double basicWoodDensity = .5;

		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, volume);
		amountMap.put(Element.Biomass, volume * basicWoodDensity);
		amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);

		List<Processor> processors = processorManager.getPrimaryProcessors();
		int i = processors.size() - 1;
		LogCategoryProcessor sawingProcessor = null;
		while (i >= 0) {
			Processor p = processors.get(i);
			if (p instanceof LogCategoryProcessor) {
				sawingProcessor = (LogCategoryProcessor) p;
				break;
			}
			i--;
		}

		processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
		processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Fagus_sylvatica), StatusClass.dead);
		List<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
		for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
			endProducts.addAll(processorManager.getCarbonUnits(status));
		}
		
		double sumVolumeStaves = 0d;
		double sumVolumeConstruction = 0d;
		for (CarbonUnit unit : endProducts) {
			if (unit instanceof EndUseWoodProductCarbonUnit) {
				if (((EndUseWoodProductCarbonUnit) unit).getCarbonUnitFeature().getUseClass() == UseClass.BARREL) {
					sumVolumeStaves += ((EndUseWoodProductCarbonUnit) unit).getBiomassMgAtCreationDate();
				} else if (((EndUseWoodProductCarbonUnit) unit).getCarbonUnitFeature().getUseClass() == UseClass.BUILDING) {
					sumVolumeConstruction += ((EndUseWoodProductCarbonUnit) unit).getBiomassMgAtCreationDate();
				}
			}
		}
		
		Assert.assertEquals("Testing volume of barrels", 0d, sumVolumeStaves, 1E-8);
		Assert.assertEquals("Testing volume of construction", 40d, sumVolumeConstruction, 1E-8);
	}

	
	private static List<CarbonUnit> runSimpleSimulationWithThisFile(String filename) throws IOException {
		ProductionProcessorManager processorManager = new ProductionProcessorManager();
		processorManager.load(filename);

		double volume = 100d;
		double carbonContent = .5;
		double basicWoodDensity = .5;

		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, volume);
		amountMap.put(Element.Biomass, volume * basicWoodDensity);
		amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);

		List<Processor> processors = processorManager.getPrimaryProcessors();
		int i = processors.size() - 1;
		LogCategoryProcessor sawingProcessor = null;
		while (i >= 0) {
			Processor p = processors.get(i);
			if (p instanceof LogCategoryProcessor) {
				sawingProcessor = (LogCategoryProcessor) p;
				break;
			}
			i--;
		}

		processorManager.processWoodPiece(sawingProcessor.getFirstLogCategory(), 2015, "", amountMaps, new CATCompatibleTreeImpl(Species.Abies_spp), StatusClass.cut);
		List<CarbonUnit> endProducts = new ArrayList<CarbonUnit>();
		for (CarbonUnitStatus status : CarbonUnitStatus.values()) {
			endProducts.addAll(processorManager.getCarbonUnits(status));
		}
		return endProducts;
	}
	
	/*
	 * Here we test the same flux configuration except that one of the end-use product has its biomass of the functional 
	 * unit and associated emissions are set to 1 in the Processor instance. Since this is a end-use product, there should
	 * not be any emissions on that front because they are accounted for in the CarbonUnitFeature instance. This can happen 
	 * if the Processor was initially an intermediate processor and then became a final processor.
	 */
	@Test
	public void test12EmissionsFromFunctionalUnit() throws IOException {
		String filename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "hardwood_recycling_en.prl";
		Collection<CarbonUnit> endProducts = runSimpleSimulationWithThisFile(filename);
		Assert.assertTrue("Checking collection is not empty", !endProducts.isEmpty());
		double sumEmissionsCO2 = 0;
		for (CarbonUnit cu : endProducts) {
			sumEmissionsCO2 += cu.getTotalNonRenewableCarbonEmissionsMgCO2Eq();
		}
		System.out.println("Emissions = " + sumEmissionsCO2);
		Assert.assertEquals("Testing if emissions are null", 0d, sumEmissionsCO2, 1E-8);
		
		String modifiedFilename = ObjectUtility.getPackagePath(getClass()) + "hardwood_recycling_enModified.prl";
		endProducts = runSimpleSimulationWithThisFile(modifiedFilename);
		Assert.assertTrue("Checking collection is not empty", !endProducts.isEmpty());
		double sumEmissionsCO2_2 = 0;
		for (CarbonUnit cu : endProducts) {
			sumEmissionsCO2_2 += cu.getTotalNonRenewableCarbonEmissionsMgCO2Eq();
		}
		System.out.println("Emissions = " + sumEmissionsCO2_2);
		Assert.assertEquals("Testing if emissions are null", 0d, sumEmissionsCO2_2, 1E-8);
	}

	/*
	 * Here we test if the XML and JSON deserializations
	 * yield the same carbon units.
	 */
	@Test
	public void test13MatterBalanceFromXmlSerializationVsJSONSerialization() throws IOException {
		String filename = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator + "hardwood_recycling_en.prl";
		List<CarbonUnit> endProductsXML = runSimpleSimulationWithThisFile(filename);
		Assert.assertEquals("Testing list size", 2, endProductsXML.size());
		
		double sum1 = 0d;
		for (CarbonUnit unit : endProductsXML) {
			sum1 += unit.getInitialCarbon();
		}
		
		String modifiedFilename = ObjectUtility.getPackagePath(getClass()) + "hardwood_recycling_en.json";
		List<CarbonUnit> endProductsJSON = runSimpleSimulationWithThisFile(modifiedFilename);
		Assert.assertEquals("Testing list size", 2, endProductsJSON.size());

		double sum2 = 0d;
		for (CarbonUnit unit : endProductsJSON) {
			sum2 += unit.getInitialCarbon();
		}

		Assert.assertEquals("Checking sum of initial carbons", sum1, sum2, 1E-8);
		
	}

	/*
	 * Test carbon unit aggregation with BACCFIRE flux configuration.
	 */
	@Test
	public void test14CarbonUnitAggregationUsingBACCFIREFluxConfiguration() throws IOException {
		String filename = ObjectUtility.getPackagePath(getClass()) + "20250717_schema_filiere_Prod_End_2020_groupes_essences_Henri.prl";
		ProductionProcessorManager processorManager = new ProductionProcessorManager();
		processorManager.load(filename);

		double volume = 1d;
		double carbonContent = .5;
		double basicWoodDensity = .5;

		AmountMap<Element> amountMap = new AmountMap<Element>();
		amountMap.put(Element.Volume, volume);
		amountMap.put(Element.Biomass, volume * basicWoodDensity);
		amountMap.put(Element.C, volume * basicWoodDensity * carbonContent);
		Map<BiomassType, AmountMap<Element>> amountMaps = new HashMap<BiomassType, AmountMap<Element>>();
		amountMaps.put(BiomassType.Wood, amountMap);
		for (Processor p : processorManager.getPrimaryProcessors()) {
			if (p instanceof LogCategoryProcessor) {
				Collection<CarbonUnit> cus = processorManager.processAmountMap(p, 
						0, 
						"mySamplingID", 
						amountMaps, 
						"mySpecies",
						SpeciesType.BroadleavedSpecies,
						StatusClass.cut, 
						null);
				Assert.assertEquals("Testing carbon unit collection size", 1096, cus.size());
				CarbonUnitList cuList = new CarbonUnitList();
				for (CarbonUnit cu : cus) {
					cuList.add(cu);
				}
				Assert.assertEquals("Testing carbon unit list size (after aggregation)", 71, cuList.size());
				break;
			}
		}
	}
}
