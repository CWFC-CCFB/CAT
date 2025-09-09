package lerfob.carbonbalancetool.pythonaccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import repicea.io.javacsv.CSVReader;
import repicea.util.ObjectUtility;

public class PythonAccessTest {

	static Map<?,?> InputMap;
	
	

	@Test
	public void testWithRealDataFromGoPlusBeech() throws Exception {
		final String keyName = "RECOLTE";
		final String shortFilename = "Hetre_SKH810000-8025_new.csv";
		String refMapFilename = ObjectUtility.getPackagePath(PythonAccessTest.class) + shortFilename;

		CSVReader reader = null;
		List<String> harvestFieldnames = new ArrayList<String>();
		List<String> resultFieldnames = new ArrayList<String>();
		
		Map<Integer, Map<String, Map<String, Double>>> inputMap = new HashMap<Integer, Map<String, Map<String, Double>>>();
		
		try {
			reader = new CSVReader(refMapFilename);
			for (int i = 0; i < reader.getFieldCount(); i++) {
				String fieldName = reader.getHeader().getField(i).getName();
				if (fieldName.contains(keyName)) {
					harvestFieldnames.add(fieldName);
				} else  if (fieldName.contains("MgHa")) {
					resultFieldnames.add(fieldName);
				}
			}
			int nbTreesAtYearMinus1 = Integer.MIN_VALUE;
			Object[] record;
			while ((record = reader.nextRecord()) != null) {
				Map<String, Map<String, Double>> innerMap = new HashMap<String, Map<String, Double>>();
				int year = Integer.parseInt(record[0].toString());
				inputMap.put(year, innerMap);
				int nbTrees = Integer.parseInt(record[record.length - 1].toString());
				int diffTrees = 0;
				boolean isHarvested = false;
				if (nbTreesAtYearMinus1 != Integer.MIN_VALUE && nbTreesAtYearMinus1 > nbTrees) {
					diffTrees = nbTreesAtYearMinus1 - nbTrees;
					innerMap.put(keyName, new HashMap<String, Double>());
					isHarvested = true;
					innerMap.get(keyName).put("NbTrees", (double) diffTrees);
				}
				nbTreesAtYearMinus1 = nbTrees;

				if (isHarvested) {
					for (int i = 0; i < harvestFieldnames.size(); i ++) {
						String oldFieldname = harvestFieldnames.get(i);
						double value = Double.parseDouble(record[reader.getHeader().getIndexOfThisField(oldFieldname)].toString());
						String newFieldname = oldFieldname.substring(oldFieldname.indexOf(keyName) + keyName.length() + 1);
						innerMap.get(keyName).put(newFieldname, value);
					}
				}
				
				innerMap.put("RESULTS", new HashMap<String, Double>());
				for (int i = 0; i < resultFieldnames.size(); i++) {
					String oldFieldname = resultFieldnames.get(i);
					String valueString = record[reader.getHeader().getIndexOfThisField(oldFieldname)].toString();
					if (valueString.isEmpty()) {
						valueString = "0";
					}
					if (oldFieldname.equals("CeqEmissionTransMgHa")) {
						oldFieldname = "CEqEmissionTransMgHa";
					}
					double value = Double.parseDouble(valueString);
					innerMap.get("RESULTS").put(oldFieldname, value);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("beech");
		
		pap.setAreaHA(0.1);
		
		Map<Integer, Map<String, Double>> resultingMap = pap.processStandList(shortFilename, inputMap);

		Assert.assertEquals("Testing map sizes", inputMap.size(), resultingMap.size());

		for (Integer year : inputMap.keySet()) {
			if (!resultingMap.containsKey(year)) {
				Assert.fail("The resulting map does not contain year " + year);
			} else {
				Map<String, Double> innerRefMap = inputMap.get(year).get("RESULTS");
				Map<String, Double> innerActualMap = resultingMap.get(year);
				innerActualMap.remove("BiomassMgHaFUEL");  // This class was added after the test was created
				Assert.assertEquals("Testing inner map sizes", innerRefMap.size(), innerActualMap.size());
				for (String key : innerRefMap.keySet()) {
					if (!innerActualMap.containsKey(key)) {
						Assert.fail("The inner resulting map does not contain key " + key);
					} else {
						double refValue = innerRefMap.get(key);
						double actualValue = innerActualMap.get(key);
						Assert.assertEquals("Testing value for key " + key, refValue, actualValue, 1E-8);
					}
				}
			}
		}
		
		System.out.println("Successfully compared file : " + shortFilename);
	}

	
	
	@Test
	public void testWithRealDataFromGoPlusPine() throws Exception {
		final String keyName = "RECOLTE";
		final String shortFilename = "PinMaritime_PCS300000-7345_new.csv";
		String refMapFilename = ObjectUtility.getPackagePath(PythonAccessTest.class) + shortFilename;

		CSVReader reader = null;
		List<String> harvestFieldnames = new ArrayList<String>();
		List<String> resultFieldnames = new ArrayList<String>();
		
		Map<Integer, Map<String, Map<String, Double>>> inputMap = new HashMap<Integer, Map<String, Map<String, Double>>>();
		
		try {
			reader = new CSVReader(refMapFilename);
			for (int i = 0; i < reader.getFieldCount(); i++) {
				String fieldName = reader.getHeader().getField(i).getName();
				if (fieldName.contains(keyName)) {
					harvestFieldnames.add(fieldName);
				} else  if (fieldName.contains("MgHa")) {
					resultFieldnames.add(fieldName);
				}
			}
			int nbTreesAtYearMinus1 = Integer.MIN_VALUE;
			Object[] record;
			while ((record = reader.nextRecord()) != null) {
				Map<String, Map<String, Double>> innerMap = new HashMap<String, Map<String, Double>>();
				int year = Integer.parseInt(record[0].toString());
				inputMap.put(year, innerMap);
				int nbTrees = Integer.parseInt(record[record.length - 1].toString());
				int diffTrees = 0;
				boolean isHarvested = false;
				if (nbTreesAtYearMinus1 != Integer.MIN_VALUE && nbTreesAtYearMinus1 > nbTrees) {
					diffTrees = nbTreesAtYearMinus1 - nbTrees;
					innerMap.put(keyName, new HashMap<String, Double>());
					isHarvested = true;
					innerMap.get(keyName).put("NbTrees", (double) diffTrees);
				}
				nbTreesAtYearMinus1 = nbTrees;

				if (isHarvested) {
					for (int i = 0; i < harvestFieldnames.size(); i ++) {
						String oldFieldname = harvestFieldnames.get(i);
						double value = Double.parseDouble(record[reader.getHeader().getIndexOfThisField(oldFieldname)].toString());
						String newFieldname = oldFieldname.substring(oldFieldname.indexOf(keyName) + keyName.length() + 1);
						innerMap.get(keyName).put(newFieldname, value);
					}
				}
				
				innerMap.put("RESULTS", new HashMap<String, Double>());
				for (int i = 0; i < resultFieldnames.size(); i++) {
					String oldFieldname = resultFieldnames.get(i);
					String valueString = record[reader.getHeader().getIndexOfThisField(oldFieldname)].toString();
					if (valueString.isEmpty()) {
						valueString = "0";
					}
					if (oldFieldname.equals("CeqEmissionTransMgHa")) {
						oldFieldname = "CEqEmissionTransMgHa";
					}
					double value = Double.parseDouble(valueString);
					innerMap.get("RESULTS").put(oldFieldname, value);
				}
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (reader != null) {
				reader.close();
			}
		}
		
		PythonAccessPoint pap = new PythonAccessPoint();
		pap.setSpecies("pine");
		
		pap.setAreaHA(0.1);
		
		Map<Integer, Map<String, Double>> resultingMap = pap.processStandList(shortFilename, inputMap);

		Assert.assertEquals("Testing map sizes", inputMap.size(), resultingMap.size());

		for (Integer year : inputMap.keySet()) {
			if (!resultingMap.containsKey(year)) {
				Assert.fail("The resulting map does not contain year " + year);
			} else {
				Map<String, Double> innerRefMap = inputMap.get(year).get("RESULTS");
				Map<String, Double> innerActualMap = resultingMap.get(year);
				innerActualMap.remove("BiomassMgHaFUEL");  // This class was added after the test was created
				Assert.assertEquals("Testing inner map sizes", innerRefMap.size(), innerActualMap.size());
				for (String key : innerRefMap.keySet()) {
					if (!innerActualMap.containsKey(key)) {
						Assert.fail("The inner resulting map does not contain key " + key);
					} else {
						double refValue = innerRefMap.get(key);
						double actualValue = innerActualMap.get(key);
						Assert.assertEquals("Testing value for key " + key, refValue, actualValue, 1E-8);
					}
				}
			}
		}
		
		System.out.println("Successfully compared file : " + shortFilename);
	}

	
}
