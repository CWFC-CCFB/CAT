/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2024 His Majesty the King in Right of Canada
 * Authors: Jean-Francois Lavoie and Mathieu Fortin, Canadian Wood Fibre Centre
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lerfob.carbonbalancetool.CATCompartmentManager;
import lerfob.carbonbalancetool.CATTimeTable;
import lerfob.carbonbalancetool.CarbonArray;
import lerfob.mems.MEMSSite;
import lerfob.mems.SoilCarbonPredictor;
import lerfob.mems.SoilCarbonPredictorCompartments;
import lerfob.mems.SoilCarbonPredictorInput;
import repicea.math.Matrix;
import repicea.serial.UnmarshallingException;
import repicea.serial.xml.XmlDeserializer;
import repicea.util.ObjectUtility;

/**
 * A wrapper of the original MEMS model for easier implementation in CAT.
 * @author J-F Lavoie - April 2024
 */
public class MEMSWrapper {
	
	public static enum SoilCompartmentGroup {
		Humus,
		MineralSoil;
	}
	
	/**
	 * An inner class that handles the carbon from two groups of compartments:
	 * in the humus and in the soil. <p>
	 * All the quantities are assumed to be measured in G/cm2 of C.
	 */
    static class InputCarbonStock {
    	
        public final static double FactorMgHaToGCm2 = 100d;

        private InputCarbonStock() {
            this.humus = 0d;
            this.soil = 0d;
        }

        double humus;
        double soil;
    }
        
	/**
	 * An inner class that handles the carbon from two groups of compartments:
	 * in the humus and in the soil.
	 */
    public static class CarbonStockForReporting extends InputCarbonStock {
    	
        public final static double FactorGCm2ToMgHa = 1d / FactorMgHaToGCm2;
//        public final static double FactorMgHaToGCm2 = 1.0d / FactorGCm2ToMgHa;

        private CarbonStockForReporting() {
        	super();
        }

        /**
         * Set the carbon stock from a SoilCarbonPredictorCompartments instance.<p>
         * 
         * This method is called for reporting. The quantities are automatically 
         * converted into Mg per hectare of C.
         * 
         * @param compartments a SoilCarbonPredictorCompartments instance.
         */
        void setCarbon(SoilCarbonPredictorCompartments compartments) {
            humus = compartments.getLitterBinsgCm2() * FactorGCm2ToMgHa;
            soil = compartments.getSoilBinsgCm2() * FactorGCm2ToMgHa;
        }

        /**
         * Provide the quantity of C in the humus.
         * @return the value (Mg/ha)
         */
        public double getHumusCarbonMgHa() {return humus;}

        /**
         * Provide the quantity of C in the mineral soil.
         * @return the value (Mg/ha)
         */
        public double getMineralSoilCarbonMgHa() {return soil;}
        
    }

    
    
    InputCarbonStock[] inputAnnualStocksGCm2;
    CarbonStockForReporting[] outputAnnualStocksMgHa;
    private CarbonArray inputFromLivingTreesAboveGroundMgHa;
    private CarbonArray inputFromLivingTreesBelowGroundMgHa;
    
    private double[][] dailyTemperatureAcrossSimulation;
    private boolean isFromAir;
    
    SoilCarbonPredictor predictor;
    SoilCarbonPredictorCompartments compartments;
    
    static ConcurrentHashMap<MEMSSite.SiteType, MEMSSite> sites = new ConcurrentHashMap<MEMSSite.SiteType, MEMSSite>();

    MEMSSite.SiteType currentSiteName;
    
    private final CATCompartmentManager manager;

    /**
     * Constructor.
     * @param manager the CATCompartmentManager instance
     */
    public MEMSWrapper(CATCompartmentManager manager) {
    	this.manager = manager;
    }
    
    
    public CarbonArray getInputFromLivingTreesAboveGroundMgHaArray() {
    	return inputFromLivingTreesAboveGroundMgHa;
    }

    public CarbonArray getInputFromLivingTreesBelowGroundMgHaArray() {
    	return inputFromLivingTreesBelowGroundMgHa;
    }

    /**
     * Initialize MEMS with appropriate parameters.<p>
     * This method is called as the carbon compartment manager is reset.
     * @param memsStands a List of MEMSCompatibleStand instances
     */
    public void prepareSimulation(List<MEMSCompatibleStand> memsStands) {

    	CATTimeTable timeTable = manager.getTimeTable();
    	List<Integer> indices = timeTable.getIndicesFromTo(memsStands.get(0), memsStands.get(memsStands.size() - 1));
        // run a simulation to reach stability into compartment bins
        // todo: use what input params ?  and what input npps ?

    	if (indices.get(0) != 0) {
    		throw new UnsupportedOperationException("The initial index should be 0!");
    	}
        int nbYears = indices.size();

        // prepare the carbon stock array
        inputAnnualStocksGCm2 = new InputCarbonStock[nbYears];
        outputAnnualStocksMgHa = new CarbonStockForReporting[nbYears];
        dailyTemperatureAcrossSimulation = new double[nbYears][];
        inputFromLivingTreesAboveGroundMgHa = new CarbonArray(nbYears);
        inputFromLivingTreesBelowGroundMgHa = new CarbonArray(nbYears);

        int maxDateYr = memsStands.get(memsStands.size() -1).getDateYr();
        int j = 0;
        MEMSCompatibleStand memsStand = memsStands.get(j);
        isFromAir = memsStand.isInterventionResult();
        for (Integer i : indices) {
            inputAnnualStocksGCm2[i] = new InputCarbonStock();
            outputAnnualStocksMgHa[i] = new CarbonStockForReporting();
            int dateYr = timeTable.getDateYrAtThisIndex(i);
            while (dateYr > memsStand.getDateYr() && dateYr <= maxDateYr) {
            	j++;
            	if (j < memsStands.size()) {
            		memsStand = memsStands.get(j);
            	} 
            }
            dailyTemperatureAcrossSimulation[i] = memsStand.getMeanDailyTemperatureCForThisYear(dateYr);
        }

        setSiteAndEstimateInitialCarbon(memsStands.get(0));	// the initial stand
    }
   
    /**
     * Set the initial carbon.<p>
     * A 1000-year simulation is run using the mean annual temperature and range from the initial stand.
     * @param stand a MEMSCompatibleStand instance that is the initial stand
     */
    private void setSiteAndEstimateInitialCarbon(MEMSCompatibleStand stand) {
        currentSiteName = stand.getSiteType();

        if (!sites.containsKey(currentSiteName)) {
            String sitesPath = ObjectUtility.getRelativePackagePath(SoilCarbonPredictor.class) + "data" + ObjectUtility.PathSeparator + "sites" + ObjectUtility.PathSeparator;

            // load the site params
            String filename = sitesPath + "mcmcMems_" + currentSiteName.name() + ".zml";
            XmlDeserializer dser = new XmlDeserializer(filename);

            try {
                sites.put(currentSiteName, (MEMSSite) dser.readObject());
            } catch (UnmarshallingException e) {
                throw new RuntimeException(e);
            }
        }

        MEMSSite currentSite = sites.get(currentSiteName);
        currentSite.getInputs().reset();
        
        compartments = new SoilCarbonPredictorCompartments(1.0, 
        		stand.getMeanDailyTemperatureCForThisYear(stand.getDateYr()),
        		stand.isTemperatureFromAir());

        predictor = new SoilCarbonPredictor(false);
        // read the fit params from mha and set them to the Predictor
        predictor.setParms(currentSite.getMetropolisHastingsAlgorithm().getFinalParameterEstimates());

        for (int i = 0; i < 1000; i++) {		
            predictor.predictAnnualCStocks(compartments, currentSite.getInputs());
        }
        
        outputAnnualStocksMgHa[0].setCarbon(compartments);
    }

    /**
     * Add carbon to MEMS input.<p>
     * This carbon is retrieved from the actualization of LeftInForestCarbonUnit instances.
     * @param index the time index as defined in the CATTimeTable instance
     * @param carbonStockMgHa the carbon stock (Mg/ha)
     * @param addToHumus a boolean (true: add to humus; false add to soil)
     */
    public void addCarbonToMEMSInput(int index, double carbonStockMgHa, boolean addToHumus) {
    	if (index < inputAnnualStocksGCm2.length) {	// the soil is no longer updated after the date of the last stand
            if (addToHumus) {
                inputAnnualStocksGCm2[index].humus += carbonStockMgHa * InputCarbonStock.FactorMgHaToGCm2;
            } else {
                inputAnnualStocksGCm2[index].soil += carbonStockMgHa * InputCarbonStock.FactorMgHaToGCm2;
            }
    	}
    }
    
    /**
     * Simulate the carbon stock for each year of the simulation.<p>
     * This method is called immediately after actualizing the carbon unit in all the compartments.
     */
    public void simulate() {
    	// first we add the input from living biomass to the annual input which contains only dead organic matter at this point
        for (int i = 0; i < inputAnnualStocksGCm2.length; i++) {
        	addCarbonToMEMSInput(i, 
        			inputFromLivingTreesAboveGroundMgHa.getCarbonArray()[i], 
        			true); // true add to humus
        	addCarbonToMEMSInput(i, 
        			inputFromLivingTreesBelowGroundMgHa.getCarbonArray()[i], 
        			false); // false add to soil
        }
    	
        MEMSSite currentSite = sites.get(currentSiteName);
        CATTimeTable timeTable = manager.getTimeTable();
        SoilCarbonPredictorInput inputParameters = currentSite.getInputs();
        inputParameters.reset();
        
        for (int i = 1; i < inputAnnualStocksGCm2.length; i++) {
            int yearZero = timeTable.getDateYrAtThisIndex(i - 1);
            int yearCurrent = timeTable.getDateYrAtThisIndex(i);
            int deltaYear = yearCurrent - yearZero;
            if (deltaYear == 0) {
                outputAnnualStocksMgHa[i] = outputAnnualStocksMgHa[i - 1];
            } else {
                InputCarbonStock inputStock = inputAnnualStocksGCm2[i]; 
                inputParameters.setDailyInput(inputStock.humus, inputStock.soil);
            	compartments.setSoilTemperature(dailyTemperatureAcrossSimulation[i], isFromAir);
                for (int y = 0; y < deltaYear; y++) {
                    predictor.predictAnnualCStocks(compartments, inputParameters);
                }

                CarbonStockForReporting outputStock = outputAnnualStocksMgHa[i];
                outputStock.setCarbon(compartments);
            }
        }
    }


	/**
     * Provide the carbon stock in the soil for a particular year.<p>
     * If the yearIndex exceeds that of the last stand, the carbon stock of the
     * last stand is then provided instead.
     * 
     * @param yearIndex the index of the year as defined in the CATTimeTable instance
     * @return a CarbonStockForReporting instance which contains the stocks in the soil and the humus.
     */
    public CarbonStockForReporting getCarbonStockMgHaForThisYear(int yearIndex) {
        return yearIndex >= outputAnnualStocksMgHa.length ?
        		outputAnnualStocksMgHa[outputAnnualStocksMgHa.length - 1] :
        			outputAnnualStocksMgHa[yearIndex];
    }
    
    /**
     * Provide the values of soil inputs in the humus and the mineral soil.
     * @return a Map with SoilCompartmentGroup and Matrix instances as key and values
     */
    public Map<SoilCompartmentGroup, Matrix> getSoilInputsMgHa() {
    	Map<SoilCompartmentGroup, Matrix> outputMap = new HashMap<SoilCompartmentGroup, Matrix>();
    	Matrix aboveGround = new Matrix(inputAnnualStocksGCm2.length, 1);
    	Matrix belowGround = new Matrix(inputAnnualStocksGCm2.length, 1);
    	for (int i = 0; i < inputAnnualStocksGCm2.length; i++) {
    		aboveGround.setValueAt(i, 0, inputAnnualStocksGCm2[i].humus * CarbonStockForReporting.FactorGCm2ToMgHa);
    		belowGround.setValueAt(i, 0, inputAnnualStocksGCm2[i].soil * CarbonStockForReporting.FactorGCm2ToMgHa);
    	}
    	outputMap.put(SoilCompartmentGroup.Humus, aboveGround);
    	outputMap.put(SoilCompartmentGroup.MineralSoil, belowGround);
    	return outputMap;
    }
    
}
