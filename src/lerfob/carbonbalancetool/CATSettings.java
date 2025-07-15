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
package lerfob.carbonbalancetool;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import lerfob.carbonbalancetool.CATUtility.BiomassParametersName;
import lerfob.carbonbalancetool.CATUtility.BiomassParametersWrapper;
import lerfob.carbonbalancetool.CATUtility.ProductionManagerName;
import lerfob.carbonbalancetool.CATUtility.ProductionProcessorManagerWrapper;
import lerfob.carbonbalancetool.biomassparameters.BiomassParameters;
import lerfob.carbonbalancetool.productionlines.ProductionLineManager;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManager;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManagerException;
import lerfob.carbonbalancetool.woodpiecedispatcher.WoodPieceDispatcher;
import lerfob.mems.MEMSSite.SiteType;
import lerfob.treelogger.basictreelogger.BasicTreeLogger;
import lerfob.treelogger.basictreelogger.BasicTreeLoggerParameters;
import repicea.app.SettingMemory;
import repicea.gui.permissions.DefaultREpiceaGUIPermission;
import repicea.simulation.Parameterizable;
import repicea.simulation.covariateproviders.treelevel.BarkProportionProvider;
import repicea.simulation.covariateproviders.treelevel.BasicWoodDensityProvider;
import repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider;
import repicea.simulation.species.REpiceaSpecies;
import repicea.simulation.treelogger.TreeLogger;
import repicea.simulation.treelogger.TreeLoggerDescription;
import repicea.simulation.treelogger.TreeLoggerParameters;
import repicea.simulation.treelogger.TreeLoggerWrapper;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The CarbonAccountingToolSettings class handles the production lines, the wood piece dispatcher
 * and the biomass parameters. 
 * @author Mathieu Fortin - February 2014
 */
@SuppressWarnings("deprecation")
public final class CATSettings {
	
	
	/**
	 * The enum representing the species. <p>
	 * 
	 * This enum makes it possible to obtain the basic density and the bark
	 * proportion following an IPCC Tier 1 approach. The proportion of bark volume was taken from 
	 * Miles, P.D. and W.B. Smith. 2009. Specific gravity and other properties of wood
	 * and bark for 156 tree species found in North America. USDA Forest Service,
	 * Northern Research Station. Research Note NRS-38.
	 * @author Mathieu Fortin - August 2020
	 * @deprecated Use REpiceaSpecies.Species instead of CATSpecies
	 */
	@Deprecated
	public static enum CATSpecies implements BasicWoodDensityProvider, 
											SpeciesTypeProvider,
											BarkProportionProvider {
		
		ABIES(REpiceaSpecies.Species.Abies_spp),
		ACER(REpiceaSpecies.Species.Acer_spp),
		ALNUS(REpiceaSpecies.Species.Alnus_spp),
		BETULA(REpiceaSpecies.Species.Betula_spp),
		CARPINUS_BETULUS(REpiceaSpecies.Species.Carpinus_betulus), // this one is from IPCC guidelines 2003
		CASTANEA_SATIVA(REpiceaSpecies.Species.Castanea_sativa),	// this one is from IPCC guidelines 2003
		FAGUS_SYLVATICA(REpiceaSpecies.Species.Fagus_sylvatica),
		FRAXINUS(REpiceaSpecies.Species.Fraxinus_spp),
		JUGLANS(REpiceaSpecies.Species.Juglans_spp),	// this one is from IPCC guidelines 2003
		LARIX_DECIDUA(REpiceaSpecies.Species.Larix_decidua),
		PICEA_ABIES(REpiceaSpecies.Species.Picea_abies),
		PICEA_SITCHENSIS(REpiceaSpecies.Species.Picea_sitchensis),
		PINUS_PINASTER(REpiceaSpecies.Species.Pinus_pinaster),
		PINUS_RADIATA(REpiceaSpecies.Species.Pinus_radiata),
		PINUS_STROBUS(REpiceaSpecies.Species.Pinus_strobus),
		PINUS_SYLVESTRIS(REpiceaSpecies.Species.Pinus_sylvestris),
		POPULUS(REpiceaSpecies.Species.Populus_spp),
		PRUNUS(REpiceaSpecies.Species.Prunus_spp),
		PSEUDOTSUGA_MENZIESII(REpiceaSpecies.Species.Pseudotsuga_menziesii),
		QUERCUS(REpiceaSpecies.Species.Quercus_spp),
		SALIX(REpiceaSpecies.Species.Salix_spp),
		THUJA_PLICATA(REpiceaSpecies.Species.Thuja_plicata), // this one is from IPCC guidelines 2003
		TILIA(REpiceaSpecies.Species.Tilia_spp),
		TSUGA(REpiceaSpecies.Species.Tsuga_spp) // this one is from IPCC guidelines 2003
		;

		public final REpiceaSpecies.Species species;
		
		CATSpecies(REpiceaSpecies.Species species) {
			this.species = species;
		};

		
		@Override
		public String toString() {return REpiceaTranslator.getString(species);}

		@Override
		public double getBasicWoodDensity() {return species.getBasicWoodDensity();}
		
		@Override
		public SpeciesType getSpeciesType() {return species.getSpeciesType();}
		
		/**
		 * This method returns the CATSpecies defined by the speciesName parameter.
		 * @param speciesName a String
		 * @return a CATSpecies instance
		 */
		public static CATSpecies getCATSpeciesFromThisString(String speciesName) {
			String sp = speciesName.trim().replace(" ", "_").toUpperCase();
			return CATSpecies.valueOf(sp);
		}

		@Override
		public double getBarkProportionOfWoodVolume() {
			return species.getBarkProportionOfWoodVolume();
		}
		
	}

	
	public static enum AssessmentReport implements TextableEnum {
		Second("Second Assessment Report", "Deuxi\u00E8me rapport d'\u00E9valuation"),
		Fourth("Fourth Assessment Report", "Quatri\u00E8me rapport d'\u00E9valuation"),
		Fifth("Fifth Assessment Report", "Cinqui\u00E8me rapport d'\u00E9valuation");

		AssessmentReport(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
		
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}

		@Override
		public String toString() {return REpiceaTranslator.getString(this);}
	}
	
	public static class GWP {
		
		private final double ch4ToCo2Eq;
		private final double n2oToCo2Eq;
		private final double coToCo2Eq;
		
		GWP(double ch4ToCo2Eq, double n2oToCo2Eq, double coToCo2Eq) {
			this.ch4ToCo2Eq = ch4ToCo2Eq;
			this.n2oToCo2Eq = n2oToCo2Eq;
			this.coToCo2Eq = coToCo2Eq;
		}
		
		/**
		 * This method returns the CO2 eq of the CH4 gas
		 * @return a double
		 */
		public double getCH4Factor() {return ch4ToCo2Eq;}
		
		/**
		 * This method returns the CO2 eq of the N2O gas
		 * @return a double
		 */
		public double getN2OFactor() {return n2oToCo2Eq;}
		
		
		/**
		 * This method returns the CO2 eq of the CO gas
		 * @return a double
		 */
		public double getCOFactor() {return coToCo2Eq;}
	}

	private static Map<AssessmentReport, GWP> GlobalWarmingPotentialMap = new HashMap<AssessmentReport, GWP>();
	static {
		GlobalWarmingPotentialMap.put(AssessmentReport.Second, new GWP(21, 310, 1.9));	// from IPCC(1995, WG1 Table 2.9 p. 121)
		GlobalWarmingPotentialMap.put(AssessmentReport.Fourth, new GWP(25, 298, 1.9));	// from IPCC(2007, Table 2.14 and paragraph p. 214)
		GlobalWarmingPotentialMap.put(AssessmentReport.Fifth, new GWP(28, 265, 1.9));	// from IPCC(2013, Ch.8 p.714 and Table 8.SM.16)
	}

	private final SettingMemory settings;
	protected boolean formerImplementation;
	
	public static final double CO2_C_FACTOR = 12d / 44;
	public static final double C_C02_FACTOR = 44d / 12;
	public static final double CH4_C_FACTOR = 12d / 16;
	public static final double C_CH4_FACTOR = 16d / 12;

	protected static AssessmentReport selectedAR = AssessmentReport.Fifth;
	
	@Deprecated
	private TreeLoggerWrapper treeLoggerWrapper;
	@Deprecated
	private final ProductionLineManager productionLines;
	@Deprecated
	private final WoodPieceDispatcher woodSupply;
	@Deprecated
	private Vector<TreeLoggerDescription> treeLoggerDescriptions;

	protected final Map<ProductionManagerName, ProductionProcessorManagerWrapper> productionManagerMap = new LinkedHashMap<ProductionManagerName, ProductionProcessorManagerWrapper>();
	protected final Map<BiomassParametersName, BiomassParametersWrapper> biomassParametersMap = new LinkedHashMap<BiomassParametersName, BiomassParametersWrapper>();
	
	private ProductionManagerName currentProcessorManager = ProductionManagerName.values()[0];
	private BiomassParametersName currentBiomassParameters = BiomassParametersName.values()[0]; 
	private SiteType currentMemsParameters = SiteType.values()[0];
	
	/**
	 * Constructor.
	 * @param settings a SettingMemory instance
	 */
	public CATSettings(SettingMemory settings) {			
		this.settings = settings;
		readProcessorManagers();
		readBiomassParameters();
		readMemsParameters();
		// TODO read mems parameters
		productionLines = new ProductionLineManager();
		treeLoggerWrapper = new TreeLoggerWrapper();
		woodSupply = new WoodPieceDispatcher(treeLoggerWrapper, productionLines);
		treeLoggerDescriptions = new Vector<TreeLoggerDescription>();
	}
	

	/**
	 * This method returns the Global Warming Potential according
	 * to the selected assessment report.
	 * @return a GWP instance
	 */
	public static GWP getGlobalWarmingPotential() {
		return GlobalWarmingPotentialMap.get(selectedAR);
	}
	
	/**
	 * This method set the assessment report for the Global Warming Potential factors.
	 * @param aR an AssessmentReport enum variable
	 */
	public static void setAssessmentReportForGWP(AssessmentReport aR) {
		selectedAR = aR;
	}
	
	private void readBiomassParameters() {
		BiomassParameters biomassParameters;
		String relativePathname = ObjectUtility.getRelativePackagePath(BiomassParameters.class) + "library" + ObjectUtility.PathSeparator;
		for (BiomassParametersName biomassParameterNames : BiomassParametersName.values()) {
			if (biomassParameterNames == BiomassParametersName.customized) {
				biomassParameters = new BiomassParameters();
				biomassParametersMap.put(biomassParameterNames, new BiomassParametersWrapper(biomassParameterNames, biomassParameters));
			} else {
				try {
					biomassParameters = new BiomassParameters(new DefaultREpiceaGUIPermission(false));
					String filename = relativePathname + biomassParameterNames.name().toLowerCase() + 
							BiomassParameters.BiomassParameterFileFilter.getExtension(); 
					biomassParameters.load(filename);
					biomassParametersMap.put(biomassParameterNames, new BiomassParametersWrapper(biomassParameterNames, biomassParameters));
				} catch (Exception e) {}
			}
		}
	}

	private void readProcessorManagers() {
		ProductionProcessorManager productionProcessorManager;
		String relativePathname = ObjectUtility.getRelativePackagePath(ProductionProcessorManager.class) + "library" + ObjectUtility.PathSeparator;
		for (ProductionManagerName processorManagerName : ProductionManagerName.values()) {
			if (processorManagerName == ProductionManagerName.customized) {
				productionProcessorManager = new ProductionProcessorManager();
				productionManagerMap.put(processorManagerName, new ProductionProcessorManagerWrapper(processorManagerName, productionProcessorManager));
			} else {
				try {
					productionProcessorManager = new ProductionProcessorManager(new DefaultREpiceaGUIPermission(false));
					String filename = relativePathname + processorManagerName.name().toLowerCase() + 
							"_" + REpiceaTranslator.getCurrentLanguage().getCode() + 
							ProductionProcessorManager.ProductionProcessorManagerFileFilter.getExtension(); 
					productionProcessorManager.load(filename);
					productionManagerMap.put(processorManagerName, new ProductionProcessorManagerWrapper(processorManagerName, productionProcessorManager));
				} catch (Exception e) {
					System.out.println("Unable to read processor manager : " + processorManagerName.name());
				}
			}
		}
	}
	
	private void readMemsParameters() {
		// TODO implement the reading of the parameters here MF20240517
	}

	/**
	 * This method returns the customizable production manager.
	 * @return a ProductionProcessorManager instance
	 */
	protected ProductionProcessorManager getCustomizableProductionProcessorManager() {return productionManagerMap.get(ProductionManagerName.customized).manager;}
	
	
	/**
	 * This method returns the customizable biomass parameters manager.
	 * @return a BiomassParameters instance
	 */
	protected BiomassParameters getCustomizableBiomassParameters() {return biomassParametersMap.get(BiomassParametersName.customized).manager;}

	/**
	 * This method returns the currently selected ProductionProcessorManager instance.
	 * @return a ProductionProcessorManager instance
	 */
	public ProductionProcessorManager getCurrentProductionProcessorManager() {
		return productionManagerMap.get(currentProcessorManager).manager;
	}

	/**
	 * This method returns the currently selected BiomassParameters instance.
	 * @return a BiomassParameters instance
	 */
	public BiomassParameters getCurrentBiomassParameters() {
		return biomassParametersMap.get(currentBiomassParameters).manager;
	}

	/**
	 * This method sets the possible tree logger options.
	 * @param treeLoggerDescriptions a Vector of TreeLoggerDescription instances
	 */
	public void setTreeLoggerDescriptions(Vector<TreeLoggerDescription> treeLoggerDescriptions) {
		ProductionProcessorManager manager = productionManagerMap.get(ProductionManagerName.customized).manager;
		if (manager != null) {
			manager.setAvailableTreeLoggers(treeLoggerDescriptions);
		}
	}

	protected void setReferentForBiomassParameters(Object referent) {
		BiomassParameters manager = biomassParametersMap.get(BiomassParametersName.customized).manager;
		if (manager != null) {
			manager.setReferent(referent);
		}
	}
	
	/**
	 * Set the current selection of biomass parameter
	 * @param bPar a BiomassParametersName enum
	 */
	public void setCurrentBiomassParametersSelection(BiomassParametersName bPar) {
		currentBiomassParameters = bPar;
	}

	protected BiomassParametersName getCurrentBiomassParametersSelection() {return currentBiomassParameters;}
	
	/**
	 * Set the current selection of production manager
	 * @param pMan a ProductionManagerName enum
	 */
	public void setCurrentProductionProcessorManagerSelection(ProductionManagerName pMan) {
		currentProcessorManager = pMan;
	}
	
	protected ProductionManagerName getCurrentProductionProcessorManagerSelection() {return currentProcessorManager;}

	/**
	 * Set the current MEMS parameters.
	 * @param site a SiteName enum
	 */
	public void setCurrentMEMSParametersSelection(SiteType site) {
		currentMemsParameters = site;
	}
	
	protected SiteType getCurrentMEMSParametersSelection() {return currentMemsParameters;}

	
	/**
	 * This method returns the list of possible tree loggers.
	 * @return a Vector of TreeLoggerDescription instances
	 */
	@Deprecated
	protected Vector<TreeLoggerDescription> getTreeLoggerDescriptions() {
		return treeLoggerDescriptions;
	}


	/**
	 * Check the validity of the settings.
	 * @return a boolean true if valid or false otherwise
	 * @throws ProductionProcessorManagerException if the ProductionProcessorManager instance cannot be validated
	 */
	public boolean isValid() throws ProductionProcessorManagerException {
		if (formerImplementation) { 			// former implementation
			if (treeLoggerWrapper.getTreeLogger() != null) {
				if (woodSupply.isValid()) {
					return true;
				}
			}
			return false;
		} else {
			getCurrentProductionProcessorManager().validate();
			return true;
		}
	}


//	public CATExponentialFunction getDecayFunction() {return decayFunction;}
	

	private void loadFromParameter(Parameterizable params, String filename) {
		try {
			params.loadFromFile(filename);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}


	protected SettingMemory getSettingMemory() {return settings;}


	/**
	 * This method sets the tree logger instance.
	 * @param selectedItem a String (default) or a TreeLoggerDescription instance
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public void setHarvestVolumeMethod(Object selectedItem) {
		formerImplementation = true;
		TreeLogger<?,?> treeLogger = null;
		if (selectedItem instanceof TreeLoggerDescription) {
			treeLogger = ((TreeLoggerDescription) selectedItem).instantiateTreeLogger(true);
			treeLoggerWrapper.setTreeLogger(treeLogger);
		} else if (selectedItem.toString().equals(CATFrame.MessageID.Default.toString())) {
			treeLogger = new BasicTreeLogger();
			TreeLoggerParameters params = new BasicTreeLoggerParameters();
			((BasicTreeLogger) treeLogger).setTreeLoggerParameters((BasicTreeLoggerParameters) params);
			treeLoggerWrapper.setTreeLogger(treeLogger);
		} else {
			loadFromParameter(treeLoggerWrapper, selectedItem.toString());
		}
	}

	/**
	 * This method sets the wood supply.  
	 * @param selectedItem the filename without the path (which is assumed to be the workspace)
	 */
	@Deprecated
	public void setWoodSupply(Object selectedItem) {
		formerImplementation = true;
		if (selectedItem.equals(CATFrame.MessageID.Default.toString())) {
			woodSupply.reset();
		} else {
			loadFromParameter(woodSupply, selectedItem.toString());
		}
	}
	
	/**
	 * This method sets the production lines.  
	 * @param selectedItem the filename without the path (which is assumed to be the workspace)
	 */
	@Deprecated
	public void setProductionLines(Object selectedItem) {
		formerImplementation = true;
		if (selectedItem.equals(CATFrame.MessageID.Default.toString())) {
			productionLines.setToDefaultValue();
		} else {
			loadFromParameter(productionLines, selectedItem.toString());
		}
	}

	/**
	 * Initialize the settings in in script mode.	
	 * @param treeLogger a TreeLogger instance
	 * @param productionLinesFilename the path to the production lines file
	 * @param woodDispatcherFilename the path to the wood dispatcher file 
	 * @throws IOException if some file cannot be read
	 */
	@SuppressWarnings("rawtypes")
	@Deprecated
	public void initialize(TreeLogger treeLogger, String productionLinesFilename, String woodDispatcherFilename) throws IOException {
		formerImplementation = true;
		treeLoggerWrapper.setTreeLogger(treeLogger);
		productionLines.load(productionLinesFilename);
		woodSupply.load(woodDispatcherFilename);
	}
	
	/**
	 * This method returns the TreeLogger instance of the settings.
	 * @return a TreeLogger instance
	 */
	public TreeLogger<?,?> getTreeLogger() {
		if (formerImplementation) {
			return treeLoggerWrapper.getTreeLogger();
		} else {
			return getCurrentProductionProcessorManager().getSelectedTreeLogger();
		}
	}

	/**
	 * This method returns the production lines.
	 * @return a ProductionLineManager instance
	 */
	@Deprecated
	protected ProductionLineManager getProductionLines() {return productionLines;}

	/**
	 * This method returns the wood piece dispatcher.
	 * @return a WoodPieceDispatcher instance
	 */
	@Deprecated
	protected WoodPieceDispatcher getWoodSupplySetup() {return woodSupply;}

}
