/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB, 
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
package lerfob.carbonbalancetool.interfaces;

/**
 * This interface ensures the instance can provide its basic wood density.
 * @author Mathieu Fortin - August 2013
 */
public interface CATBasicWoodDensityProvider {

	/**
	 * This enum is deprecated. <br>
	 * Use the basic wood density from the CATSettings class instead.
	 */
	@Deprecated
	public enum AverageBasicDensity {
		SessileOak(.685, 12d),		// Nepveu Table IV.3 12% moisture content
		EuropeanBeech(.720, 12d),	// Nepveu Table IV.3 12% moisture content
		DouglasFir(.520, 12d),		// Nepveu Table IV.3 12% moisture content
		SilverFir(.460, 12d),		// Nepveu Table IV.3 12% moisture content
		NorwaySpruce(.450, 12d),	// Nepveu Table IV.3 12% moisture content
		MaritimePine(.550, 12d),	// Nepveu Table IV.3 12% moisture content
		
		// USA
		SugarMaple(.560, 30d),		// USDA Agriculture Handbook 72 table 4.2
		YellowBirch(.550, 30d),		// USDA Agriculture Handbook 72 table 4.2
		AmericanBeech(.560, 30d),	// USDA Agriculture Handbook 72 table 4.2
		NorthernRedOak(.560, 30d),	// USDA Agriculture Handbook 72 table 4.2
		PaperBirch(.480, 30d),		// USDA Agriculture Handbook 72 table 4.2
		InteriorNorthDouglasFir(.450, 30d),	// USDA Agriculture Handbook 72 table 4.2
		
		// Canada
		CanadianDouglasFir(.450, 30d),	// USDA Agriculture Handbook 72 table 4.3
		CanadianBalsamFir(.340, 30d),	// USDA Agriculture Handbook 72 table 4.3
		CanadianBlackSpruce(.410, 30d),// USDA Agriculture Handbook 72 table 4.3
		CanadianRedSpruce(.380, 30d),	// USDA Agriculture Handbook 72 table 4.3
		CanadianWhiteSpruce(.350, 30d),// USDA Agriculture Handbook 72 table 4.3
		;				
		
		private double basicDensityGreen;
		private final static double CGV = 0.617;
		
		//TODO the following function works only when initial density is at 12%
		AverageBasicDensity(double density, double moistureContent) {
			this.basicDensityGreen = density / ((1d + 0.01* moistureContent) * (CGV * 0.01 * (30d - moistureContent) + 1));	// eq2.5 in Guilley, E. 2000. La densit� du bois du ch�ne sessile. Th�se ENGREF.
		}
		
		public double getBasicDensity() {return this.basicDensityGreen;}
	}

	/**
	 * This method returns the basic wood density calculated as 
	 * the oven dry weight / green volume (e.g. 30% of moisture content). 
	 * The LoggableTree.AverageBasicDensity enum variable already provides 
	 * some basic densities for several northamerican and european species.
	 * @return the basic wood density (Mg/m3)
	 */
	public double getBasicWoodDensity();
	
	/**
	 * If the predictor benefits from a stochastic implementation, then the sensitivity analysis is enabled.
	 * @return a boolean
	 */
	public default boolean isBasicWoodDensityPredictorStochastic() {return false;}


}
