/*
 * This file is part of the CAT library.
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
 * This interface ensures the instance can provide its carbon content.
 * @author Mathieu Fortin - August 2013
 */
public interface CATCarbonContentRatioProvider {

	/**
	 * This enum variable contains the average carbon content ratio. 
	 * @author M. Fortin - September 2010
	 * @see <a href="http://www.sciencedirect.com/science?_ob=MImg&_imagekey=B6V22-485P72X-1-5V&_cdi=5690&_user=4296857&_pii=S0961953403000333&_origin=browse&_zone=rslt_list_item&_coverDate=10%2F31%2F2003&_sk=999749995&wchp=dGLbVlb-zSkWb&md5=a1a5ce14d9917fa406c71bacc641d09a&ie=/sdarticle.pdf"> 
	 * Lamlol, S.H., and R.A. Savidge. 2003. A reassessment of carbon content in wood: variation within and between 41 North American species. 
	 * Biomass and Bioenergy 25: 381-388. </a>
	 */
	public enum AverageCarbonContent {
		Softwood(0.5105),
		Hardwood(0.4841);
		
		private double ratio;
		
		AverageCarbonContent(double d) {
			this.ratio = d;
		}
		/**
		 * This method returns the average carbon content ratio.
		 * @return the ratio in ton of carbon / ton of dry biomass (double)
		 */
		public double getRatio() {return this.ratio;}
	}
	

	
	/**
	 * This method returns the ratio tons of carbon - tons of dry biomass.
	 * The AverageCarbonContent enum variable can be used as reference for
	 * this method.
	 * @return the ratio tC/t dry biomass (double)
	 */
	public double getCarbonContentRatio();
	
	/**
	 * If the predictor benefits from a stochastic implementation, then the sensitivity analysis is enabled.
	 * @return a boolean
	 */
	public default boolean isCarbonContentRatioPredictorStochastic() {return false;}


}
