/*
 * This file is part of the mems library.
 *
 * Copyright (C) 2022-23 His Majesty the King in Right of Canada
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
package lerfob.mems;

import repicea.serial.PostUnmarshalling;

/**
 * The SoilCarbonPredictorInput class contains the parameters needed for the calculation of the inputs in the 
 * different compartments.
 * @author Jean-Francois Lavoie and Mathieu Fortin - February 2023
 */
public class SoilCarbonPredictorInput implements PostUnmarshalling {

    public enum LandType
    {
        Unknown(1.0, 0.45 , 0.2, 300, 20),
        GrassLand(1.1, 0.35, 0.15, 260, 15),
        BroadleafForest(1.32, 0.40, 0.27, 290,25),
        MixedForest(0.87, 0.38, 0.30, 340, 27.5),
        ConiferousForest(0.41, 0.35, 0.32, 390, 30),
        MontmorencyForest(1.38, 0.35,  0.26, 75, 18);

        double N_lit;			// teneur en azote du materiau d'entree en % [0-100]
        double f_sol;			// la fraction extractible a l'eau chaude de l'apport de litiere
        double f_lig;			// fraction insoluble dans l'acide de l'apport de litiere
        double Rdepmax;         // Maximum rooting depth (cm)
        double Rdep50;          // Depth to which 50% of the root mass is distributed

        LandType(double N_lit, double f_sol, double f_lig, double Rdepmax, double Rdep50) {
            this.N_lit = N_lit;
            this.f_sol = f_sol;
            this.f_lig = f_lig;
            this.Rdepmax = Rdepmax;
            this.Rdep50 = Rdep50;
        }
    }

    final LandType landType;
    double originalCT_i;			// apport quotidien total de carbone provenant de la source externe i le jour j
    double CT_i;			// apport quotidien total de carbone provenant de la source externe i le jour j
    final double soil_pH;			// pH du sol simule
    final double bulkDensity;		// bulk density (densite volumetrique) du sol simule
    final double sandProportion;	// contenu en sable (%) du sol simule [0-100]
    final double rockProportion;  // fraction de roches (%) du sol simule [0-100]
    Double depthCm;
    
    /**
     * Constructor.
     * @param landType the landType.  This auto-selects driving variables for the simulation.
     * @param CT_i the daily input from external source i
     * @param depth_cm soil depth being considered (cm)
     * @param soil_pH soil pH
     * @param bulkDensity the bulk density
     * @param sandProportion the sand proportion in percent
     * @param rockProportion the rock proportion in percent
     */
    public SoilCarbonPredictorInput(LandType landType,
    		double CT_i, 
    		double depth_cm,
    		double soil_pH,
    		double bulkDensity, 
    		double sandProportion, 
    		double rockProportion) {
        this.landType = landType;
        this.CT_i = CT_i;
        this.originalCT_i = CT_i;
        this.depthCm = depth_cm;
        this.soil_pH = soil_pH;
        this.bulkDensity = bulkDensity;
        this.sandProportion = sandProportion;
        this.rockProportion = rockProportion;
    }

    /**
     * Constructor.
     * @param landType the landType.  This auto-selects driving variables for the simulation.
     * @param Annual_NPP_aboveGround_CgM2 the annual aboveground net primary production
	 * @param Annual_NPP_belowGround_CgM2 the annual belowground net primary production
     * @param depth_cm soil depth being considered (cm)
     * @param soil_pH soil pH
     * @param bulkDensity the bulk density
     * @param sandProportion the sand proportion in percent
     * @param rockProportion the rock proportion in percent
     */
    public SoilCarbonPredictorInput(LandType landType,
                                    double Annual_NPP_aboveGround_CgM2,
                                    double Annual_NPP_belowGround_CgM2,
                                    double depth_cm,
                                    double soil_pH,
                                    double bulkDensity,
                                    double sandProportion,
                                    double rockProportion) {
    	this(landType, 
    		getCT_iFromAnnualNPP(landType, depth_cm, Annual_NPP_aboveGround_CgM2, Annual_NPP_belowGround_CgM2),
    		depth_cm,
    		soil_pH,
    		bulkDensity,
    		sandProportion,
    		rockProportion);
    }
    
    static double getCT_iFromAnnualNPP(LandType landType, double depth_cm, double Annual_NPP_aboveGround_CgM2, double Annual_NPP_belowGround_CgM2) {
        double correctedDailyBelowGroundCarbon = SoilCarbonPredictorEquation.Eq53_getCorrectedBelowGroundCarbonInput(Annual_NPP_belowGround_CgM2 / 365.0, 
        		depth_cm, 
        		landType.Rdep50, 
        		landType.Rdepmax);
        return correctedDailyBelowGroundCarbon + Annual_NPP_aboveGround_CgM2 / 365.0;
    }
    
    public void setDailyInput(double annual_NPP_aboveGround_CgM2, double annual_NPP_belowGround_CgM2) {
    	if (depthCm == 0d) {
    		throw new UnsupportedOperationException("The depth has not been set. Presumably the wrong constructor has been used.");
    	}
    	CT_i = getCT_iFromAnnualNPP(landType, depthCm, annual_NPP_aboveGround_CgM2, annual_NPP_belowGround_CgM2);
    }

	@Override
	public void postUnmarshallingAction() {
		if (depthCm == null) {
			depthCm = 15d;
		}
		if (originalCT_i == 0d) {
			originalCT_i = CT_i;
		}
	}
	
	public void reset() {
		CT_i = originalCT_i;
	}
	
}
