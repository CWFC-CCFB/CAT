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
package lerfob.carbonbalancetool;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lerfob.carbonbalancetool.productionlines.CarbonUnit;
import repicea.math.Matrix;
import repicea.serial.SerializerChangeMonitor;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The CarbonCompartment class handles the calculation of the carbon content for
 * the different compartment (root, aboveground, wood products, etc.)
 * @author Mathieu Fortin - July 2010
 */
@SuppressWarnings("rawtypes")
public class CATCompartment extends CarbonArray implements Comparable {
	
	static {
		SerializerChangeMonitor.registerEnumNameChange("lerfob.carbonbalancetool.CATCompartment$CompartmentInfo", 
				"TotalBiomass",
				"LivingBiomass");
	}
	
	
	protected static enum PoolCategory {
		Forest,
		HarvestedWoodProducts,
		Others
	}
	
	/**
	 * This enum contains basic information on the carbon compartments.
	 * @author Mathieu Fortin - June 2010
	 */
	public static enum CompartmentInfo implements TextableEnum {
		/**
		 * The compartment that contains the root biomass.
		 */
		Roots("Belowground biomass", "Biomasse souterraine", false, new Color(0,100,0)),
		/**
		 * The aboveground biomass (bole, branches and twigs).
		 */
		AbGround("Aboveground biomass", "Biomasse a\u00E9rienne", false, new Color(0,150,0)),
		/**
		 * The dead biomass, i.e. the dead trees and the wood pieces left in the forest
		 */
		DeadBiom("Dead organic matter", "Mati\u00E8re organique morte", true, new Color(0,50,0)),
		/**
		 * The wood products.
		 */
		Products("In use", "En usage", false, new Color(0,0,150)),
		/**
		 * The landfill degradable carbon pool.
		 */
		LfillDeg("At the SWDS (DOCf)", "Au site d'enfouissement (DOCf)", false, new Color(0,0,100)),
		/**
		 * The carbon emissions due to fossil fuel consumption.
		 */
		CarbEmis("Fossil fuel emissions", "Emissions d'origine fossile", false, new Color(150,100,100)),
		/**
		 * The energy substitution.
		 */
		EnerSubs("Energy and material substitution", "Substitution mat\u00E9rielle et \u00E9nerg\u00E9tique", false, new Color(0,100,100)),
		/**
		 * The landfill non degradable carbon pool.
		 */
		LfillND("SWDS (Non-degradable)", "Site d'enfouissement (Non d\u00E9gradable)", false, new Color(0,150,150)),
		/**
		 * The Landfill GHG emissions (CH4)
		 */
		LfillEm("SWDS (Methane emissions)", "Site d'enfouissement (Emissions de m\u00E9thane)", false, new Color(150,150,150)),
		/**
		 * Emissions due to wood combustion 
		 */
		WComb("Wood combustion (CH4, CO and VOC emissions)", "Combustion du bois (Emissions de CH4, CO et COV)", false, new Color(150,150,150)),
		/**
		 * The living biomass, i.e. the aboveground biomass + the belowground biomass.
		 */
		LivingBiomass("Living biomass", "Biomasse vivante", true, new Color(0,200,0)),
		/**
		 * The sum of the living biomass and the wood products.
		 */
		TotalProducts("All HWP", "Tous les produits du bois", true, new Color(0,0,200)),
		/**
		 * The net substitution, i.e. the energy substitution less the carbon emissions.
		 */
		NetSubs("Cumulative net flux", "Flux net cumulatif", true, new Color(200,0,0)),
		/**
		 * The humus layer
		 */
		Humus("Forest floor", "Humus", false, new Color(128,64,64)),
		/**
		 * The soil layer (0-15cm)
		 */
		MineralSoil("Mineral Soil", "Sol Min/u00E9ral", false, new Color(96,48,48)),
		/**
		 * The forest floor plus the soil layer (0-15cm)
		 */
		Soil("Soil", "Sol", true, new Color(96,48,48));

		private static List<CompartmentInfo> naturalOrder;
		private static Map<CompartmentInfo, PoolCategory> poolCategoryMap;
		
		private boolean resultFromGrouping;
		private Color color;

		CompartmentInfo(String englishText, String frenchText, boolean resultFromGrouping, Color color) {
			this.resultFromGrouping = resultFromGrouping;
			this.color = color;
			setText(englishText, frenchText);
		}
				
		
		PoolCategory getPoolCategory() {
			if (poolCategoryMap == null) {
				poolCategoryMap = new HashMap<CompartmentInfo, PoolCategory>();
				
				poolCategoryMap.put(LivingBiomass, PoolCategory.Forest);
				poolCategoryMap.put(AbGround, PoolCategory.Forest);
				poolCategoryMap.put(Roots, PoolCategory.Forest);
				poolCategoryMap.put(DeadBiom, PoolCategory.Forest);
				poolCategoryMap.put(Soil, PoolCategory.Forest);
				poolCategoryMap.put(Humus, PoolCategory.Forest);
				poolCategoryMap.put(MineralSoil, PoolCategory.Forest);
				
				poolCategoryMap.put(TotalProducts, PoolCategory.HarvestedWoodProducts);
				poolCategoryMap.put(Products, PoolCategory.HarvestedWoodProducts);
				poolCategoryMap.put(LfillDeg, PoolCategory.HarvestedWoodProducts);
				
				poolCategoryMap.put(NetSubs, PoolCategory.Others);
				poolCategoryMap.put(EnerSubs, PoolCategory.Others);
				poolCategoryMap.put(CarbEmis, PoolCategory.Others);
				poolCategoryMap.put(LfillEm, PoolCategory.Others);
				poolCategoryMap.put(LfillND, PoolCategory.Others);
				poolCategoryMap.put(WComb, PoolCategory.Others);
			}
			return poolCategoryMap.get(this);
		}
		

		protected boolean isPrimaryCompartment() {return resultFromGrouping;}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {return REpiceaTranslator.getString(this);}

		/**
		 * This method returns the natural order of this enum variable.
		 * @return a List of CompartmentInfo instance
		 */
		public static List<CompartmentInfo> getNaturalOrder() {
			if (naturalOrder == null) {
				naturalOrder = new ArrayList<CompartmentInfo>();
				naturalOrder.add(LivingBiomass);
				naturalOrder.add(AbGround);
				naturalOrder.add(Roots);
				naturalOrder.add(DeadBiom);
				naturalOrder.add(Soil);
				naturalOrder.add(Humus);
				naturalOrder.add(MineralSoil);
				naturalOrder.add(TotalProducts);
				naturalOrder.add(Products);
				naturalOrder.add(LfillDeg);
				naturalOrder.add(NetSubs);
				naturalOrder.add(EnerSubs);
				naturalOrder.add(CarbEmis);
				naturalOrder.add(LfillEm);
				naturalOrder.add(LfillND);
				naturalOrder.add(WComb);
			}
			return naturalOrder;
		}
		
		/**
		 * This method returns the color of the compartment for rendering.
		 * @return a Color instance
		 */
		public Color getColor() {return color;}
	}

	private CATCompartmentManager compartmentManager;
	private CompartmentInfo compartmentInfo;
	private List<CATCompartment> fatherCompartments;
	private boolean isMerged;
	private CATCompartmentCompileLibrary carbonMethodLibrary;
	private double integratedCarbon;

	private Collection<? extends CarbonUnit>[] carbonUnitsCollectionArray;
	

	/**
	 * General constructor for a CarbonCompartment object.
	 * @param compartmentManager = the compartment manager that handles this compartment
	 * @param compartmentInfo = the compartment info of this compartment
	 */
	protected CATCompartment(CATCompartmentManager compartmentManager, CompartmentInfo compartmentInfo) {
		this.compartmentManager = compartmentManager;
		this.compartmentInfo = compartmentInfo;
		this.fatherCompartments = new ArrayList<CATCompartment>();
		this.carbonMethodLibrary = new CATCompartmentCompileLibrary();
	}
	
	/* 
	 * Accessors
	 */
	public CompartmentInfo getCompartmentID() {return compartmentInfo;}
	public CATCompartmentManager getCompartmentManager() {return compartmentManager;}
	protected void addFatherCompartment(CATCompartment carbonCompartment) {fatherCompartments.add(carbonCompartment);}
	
	protected double getIntegratedCarbon() {return integratedCarbon;}
	protected void setIntegratedCarbon(double integratedCarbon) {this.integratedCarbon = integratedCarbon;}
	protected void setCarbonUnitsArray(Collection<? extends CarbonUnit>[] carbonUnitsCollectionArray) {this.carbonUnitsCollectionArray = carbonUnitsCollectionArray;}
	protected Collection<? extends CarbonUnit>[] getCarbonUnitsArray() {return carbonUnitsCollectionArray;}
	
	/**
	 * This method initializes the carbon map array.
	 */
	protected void resetCarbon() {
		initializeCarbonArray(getCompartmentManager().getTimeTable().size());
		integratedCarbon = 0d;
		isMerged = false;
	}
	
	/**
	 * This method return a Matrix with the carbon stock per ha.
	 * @param plotAreaHa the area of the plot in ha
	 * @return a Matrix instance
	 */
	protected Matrix getCarbonEvolution(double plotAreaHa) {
		double areaFactor = 1d / plotAreaHa;
		
		Matrix value = new Matrix(getCompartmentManager().getTimeTable().size(),1);
		
		for (int i = 0; i < value.m_iRows; i++) {
			value.setValueAt(i, 0, getCarbonArray()[i] * areaFactor);
		}
		
		return value;
	}
	
	
	/**
	 * This method returns a random variable that contains the integrated carbon per ha.
	 * @param plotAreaHa the plot area in ha (double) 
	 * @return a double 
	 */
	public double getIntegratedCarbon(double plotAreaHa) {
		double areaFactor = 1d / plotAreaHa;
		return integratedCarbon * areaFactor;
	}
	
	
	/**
	 * This method merges the carbon values of the father compartments into this compartment.
	 */
	protected void mergeWithFatherCompartments() {
		if (!this.fatherCompartments.isEmpty() && !isMerged) {
			for (CATCompartment comp : this.fatherCompartments) {
				comp.mergeWithFatherCompartments();			// recursive method to ensure that compartment with father compartments have been merged
				
				// for the calculated carbon
				for (int i = 0; i < getCompartmentManager().getTimeTable().size(); i++) {
					getCarbonArray()[i] += comp.getCarbonArray()[i];
				}
				
				// for the integrated carbon
				this.integratedCarbon += comp.getIntegratedCarbon(); 
			}
			isMerged = true;
		}
	}
	
	public List<CATCompartment> getFatherCompartment() {return fatherCompartments;} 
	
	
	/**
	 * Calculate and integrate the carbon of this compartment.<p>
	 * The method refers to the CATCompartmentCompileLibrary instance for the proper 
	 * calculation and integration methods.
	 */
	public void calculateAndIntegrateCarbon() {
		carbonMethodLibrary.selectCalculatorFunction(this);
	}
	

	@Override
	public int compareTo (Object o) {
		CATCompartment thatComp = (CATCompartment) o;
		int thatOrder = CompartmentInfo.getNaturalOrder().indexOf(thatComp.getCompartmentID());
		int thisOrder = CompartmentInfo.getNaturalOrder ().indexOf(getCompartmentID());
		if (thisOrder < thatOrder) {
			return -1;
		} else if (thisOrder == thatOrder) {
			return 0;
		} else {
			return 1;
		}
	}

	
//	public static void main(String[] args) {
//		JDialog dlg = new JDialog();
//		dlg.setModal(true);
//		dlg.setLayout(new GridLayout(6,6));
//		for (int i = 0; i < 6; i++) {
//			for (int j = 0; j < 6; j++) {
//				JButton button = new JButton("Allo_" + i + "_" + j);
//				button.setBackground(new Color(i * 50, 100, 100));
//				dlg.add(button);
//			}
//		}
//		dlg.setSize(new Dimension(100,100));
//		dlg.setVisible(true);
//		System.exit(0);
//	}
}
