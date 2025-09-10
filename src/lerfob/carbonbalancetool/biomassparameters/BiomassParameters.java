/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2013 Mathieu Fortin AgroParisTech/INRA UMR LERFoB
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
package lerfob.carbonbalancetool.biomassparameters;

import java.awt.Component;
import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import javax.swing.filechooser.FileFilter;

import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.CarbonAccountingTool;
import lerfob.carbonbalancetool.biomassparameters.BiomassParametersDialog.MessageID;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATAboveGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATBasicWoodDensityProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATBelowGroundVolumeProvider;
import lerfob.carbonbalancetool.interfaces.CATCarbonContentRatioProvider;
import lerfob.carbonbalancetool.interfaces.CATCommercialBiomassProvider;
import lerfob.carbonbalancetool.interfaces.CATCommercialCarbonProvider;
import lerfob.carbonbalancetool.interfaces.CATSapling;
import lerfob.carbonbalancetool.memsconnectors.MEMSCompatibleTree;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import repicea.gui.REpiceaShowableUIWithParent;
import repicea.gui.Resettable;
import repicea.gui.permissions.DefaultREpiceaGUIPermission;
import repicea.gui.permissions.REpiceaGUIPermission;
import repicea.io.IOUserInterfaceableObject;
import repicea.io.REpiceaFileFilterList;
import repicea.serial.Memorizable;
import repicea.serial.MemorizerPackage;
import repicea.serial.SerializerChangeMonitor;
import repicea.serial.xml.XmlDeserializer;
import repicea.serial.xml.XmlSerializer;
import repicea.simulation.MonteCarloSimulationCompliantObject;
import repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider.SpeciesType;
import repicea.util.ExtendedFileFilter;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaLogManager;

/**
 * The BiomassParameters class handles the conversion from 
 * merchantable volume to carbon. <p>
 * 
 * There are three alternatives for calculating the aboveground biomass:
 * <ol>
 * <li> If the tree provides its commercial biomass, then the biomass expansion factor is applied.
 * <li> If the tree provides its aboveground volume, then the basic density factor is applied.
 * <li> If the tree can't provide either of the above, then the biomass expansion factor and 
 * the basic density factor are applied
 * </ol>
 * 
 * The class goes through these alternatives in order meaning that the commercial biomass alternative
 * is favoured over the other two and so on.
 * 
 * @author Mathieu Fortin - 2013, September 2025
 */
public class BiomassParameters implements REpiceaShowableUIWithParent, IOUserInterfaceableObject, Resettable, Memorizable {

	/**
	 * Define the different conversion factors for which
	 * a Tier 2 approach can be enabled. <p>
	 * 
	 * To enable a Tier 2 approach, the Tree class must implement
	 * some interfaces. 
	 * 
	 * @see <a href=https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20User%20interface/#setting-carbon-balance-parameters>https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20User%20interface</a>
	 * @author Mathieu Fortin - 2013
	 *
	 */
	public static enum Tier2Implementation {
		/**
		 * Associated with belowground expansion factors.
		 */
		RootExpansionFactor,
		/**
		 * Associated with aboveground expansion factors.
		 */
		BranchExpansionFactor,
		BasicWoodDensityFactor,
		CarbonContentFactor
	}
	
	static {
		SerializerChangeMonitor.registerClassNameChange("lerfob.carbonbalancetool.CarbonToolCompatibleTree$SpeciesType",	
				"repicea.simulation.covariateproviders.treelevel.SpeciesNameProvider$SpeciesType");
	}
	
	public static class BiomassParametersFileFilter extends FileFilter implements ExtendedFileFilter {

		private String extension = ".bpf";
		
		@Override
		public boolean accept(File f) {
			if (f.isDirectory() || f.getAbsolutePath().toLowerCase().trim().endsWith(extension)) {
				return true;
			} else {
				return false;
			}
		}

		@Override
		public String getDescription() {
			return MessageID.BiomassParametersFileExtension.toString();
		}

		@Override
		public String getExtension() {return extension;}
	}

	public static final BiomassParametersFileFilter BiomassParameterFileFilter = new BiomassParametersFileFilter();
	
	protected final HashMap<SpeciesType, Double> branchExpansionFactors;
	protected final HashMap<SpeciesType, Double> rootExpansionFactors;

	/*
	 * This biomass density factors are for species groups as in the French context (e.g. Broadleaved vs coniferous). The
	 * basic density factors provided by the CATSpecies class should be used instead.
	 */
	@Deprecated
	protected final HashMap<SpeciesType, Double> basicWoodDensityFactors;
	protected final HashMap<SpeciesType, Double> carbonContentFactors;

	protected boolean rootExpansionFactorFromModelEnabled;
	protected boolean branchExpansionFactorFromModelEnabled;
	protected boolean basicWoodDensityFromModelEnabled;
	protected boolean carbonContentFromModelEnabled;
	
	protected boolean rootExpansionFactorFromModel;
	protected boolean branchExpansionFactorFromModel;
	protected boolean basicWoodDensityFromModel;
	protected boolean carbonContentFromModel;

	
	private transient BiomassParametersDialog guiInterface;
	
	private transient Object referent;	// check the compatibility with the referent when deserializing 

	private String filename;
	
	protected transient REpiceaGUIPermission permissions = new DefaultREpiceaGUIPermission(true);

	private transient Map<CATCompatibleTree, Double> aboveGroundVolumeM3Cache;
	private transient Map<CATCompatibleTree, Double> aboveGroundBiomassMgCache;
	private transient Map<CATCompatibleTree, Double> aboveGroundCarbonMgCache;
	private transient Map<CATCompatibleTree, Double> belowGroundVolumeM3Cache;
	private transient Map<CATCompatibleTree, Double> belowGroundBiomassMgCache;
	private transient Map<CATCompatibleTree, Double> belowGroundCarbonMgCache;
	private transient Map<CATCompatibleTree, Double> commercialVolumeM3Cache;
	private transient Map<CATCompatibleTree, Double> commercialBiomassMgCache;
	private transient Map<CATCompatibleTree, Double> commercialCarbonMgCache;

	
	/**
	 * Constructor with permissions.
	 * @param permissions an REpiceaGUIPermission instance
	 */
	public BiomassParameters(REpiceaGUIPermission permissions) {
		if (permissions != null) {
			this.permissions = permissions;
		}
		branchExpansionFactors = new HashMap<SpeciesType, Double>();
		rootExpansionFactors = new HashMap<SpeciesType, Double>();
		basicWoodDensityFactors = new HashMap<SpeciesType, Double>();
		carbonContentFactors = new HashMap<SpeciesType, Double>();
		reset();
	}

	private Map<CATCompatibleTree, Double> getAboveGroundVolumeM3Cache() {
		if (aboveGroundVolumeM3Cache == null) {
			aboveGroundVolumeM3Cache = new HashMap<CATCompatibleTree, Double>();
		}
		return aboveGroundVolumeM3Cache;
	}
	
	private Map<CATCompatibleTree, Double> getAboveGroundBiomassMgCache() {
		if (aboveGroundBiomassMgCache == null) {
			aboveGroundBiomassMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return aboveGroundBiomassMgCache;
	}

	private Map<CATCompatibleTree, Double> getAboveGroundCarbonMgCache() {
		if (aboveGroundCarbonMgCache == null) {
			aboveGroundCarbonMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return aboveGroundCarbonMgCache;
	}

	private Map<CATCompatibleTree, Double> getBelowGroundVolumeM3Cache() {
		if (belowGroundVolumeM3Cache == null) {
			belowGroundVolumeM3Cache = new HashMap<CATCompatibleTree, Double>();
		}
		return belowGroundVolumeM3Cache;
	}

	private Map<CATCompatibleTree, Double> getBelowGroundBiomassMgCache() {
		if (belowGroundBiomassMgCache == null) {
			belowGroundBiomassMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return belowGroundBiomassMgCache;
	}

	private Map<CATCompatibleTree, Double> getBelowGroundCarbonMgCache() {
		if (belowGroundCarbonMgCache == null) {
			belowGroundCarbonMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return belowGroundCarbonMgCache;
	}

	private Map<CATCompatibleTree, Double> getCommercialVolumeM3Cache() {
		if (commercialVolumeM3Cache == null) {
			commercialVolumeM3Cache = new HashMap<CATCompatibleTree, Double>();
		}
		return commercialVolumeM3Cache;
	}
	
	private Map<CATCompatibleTree, Double> getCommercialBiomassMgCache() {
		if (commercialBiomassMgCache == null) {
			commercialBiomassMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return commercialBiomassMgCache;
	}

	private Map<CATCompatibleTree, Double> getCommercialCarbonMgCache() {
		if (commercialCarbonMgCache == null) {
			commercialCarbonMgCache = new HashMap<CATCompatibleTree, Double>();
		}
		return commercialCarbonMgCache;
	}

	/**
	 * Empty constructor for class.newInstance() call.
	 */
	public BiomassParameters() {
		this(new DefaultREpiceaGUIPermission(true));
	}
	
	/**
	 * Check if the IPCC Tier 2 approach is enabled.
	 * @param item a Tier2Implementation enum
	 * @return a boolean
	 */
	public boolean isTier2ImplementationEnabled(Tier2Implementation item) {
		switch(item) {
		case RootExpansionFactor:
			return this.rootExpansionFactorFromModel;			
		case BranchExpansionFactor:
			return this.branchExpansionFactorFromModel;			
		case BasicWoodDensityFactor:
			return this.basicWoodDensityFromModel;			
		case CarbonContentFactor:
			return this.carbonContentFromModel;			
		default:
			throw new InvalidParameterException("isTier2ImplementationEnabled : This item is not recognized :" + item.name());
		}
	}

	private void logTier2WarningMessage(Tier2Implementation item) {
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, 
				Level.WARNING, 
				"BiomassParameters.setTier2ImplementationEnabled", "Tier 2 implementation for " + item.name() + " was enabled but model implementation doesn't support it.");
	}
	
	/**
	 * Enable an IPCC Tier 2 approach if the model allows it. <p>
	 * To allow the Tier 2 approach the Tree instance must implement
	 * additional interfaces. If an attempt is made to enable a Tier 
	 * 2 approach but the model does not allow it, then a warning is 
	 * issued and the Tier 2 approach remains disabled for the item 
	 * parameter.
	 *  
	 * @see <a href=https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20User%20interface/#setting-carbon-balance-parameters>https://sourceforge.net/p/lerfobforesttools/wiki/CAT%20-%20User%20interface</a>
	 * @param item a Tier2Implementation enum
	 * @param value true to enable or false to disable
	 */
	public void setTier2ImplementationEnabled(Tier2Implementation item, boolean value) {
		switch(item) {
		case RootExpansionFactor:
			if (value && !rootExpansionFactorFromModelEnabled) {
				logTier2WarningMessage(item);
			} else {
				rootExpansionFactorFromModel = value;
			}
			break;
		case BranchExpansionFactor:
			if (value && !branchExpansionFactorFromModelEnabled) {
				logTier2WarningMessage(item);
			} else {
				branchExpansionFactorFromModel = value;
			}
			break;
		case BasicWoodDensityFactor:
			if (value && !basicWoodDensityFromModelEnabled) {
				logTier2WarningMessage(item);
			} else {
				basicWoodDensityFromModel = value;
			}
			break;
		case CarbonContentFactor:
			if (value && !carbonContentFromModelEnabled) {
				logTier2WarningMessage(item);
			} else {
				carbonContentFromModel = value;
			}
			break;
		default:
			throw new InvalidParameterException("setTier2ImplementationEnabled : This item is not recognized :" + item.name());
		}
	}

	/**
	 * Set the referent object, typically a Tree instance.<p>
	 * 
	 * The referent is used to define whether the model allows for
	 * a Tier 2 approach for different factors. In case the model allows 
	 * for a Tier 2 approach, the BiomassParameters instance will 
	 * automatically enable the Tier 2 approach.
	 * 
	 * @see Tier2Implementation
	 * @param referent typically a Tree instance
	 */
	public void setReferent(Object referent) {
		this.referent = referent; 
		testReferent(referent);
	}
	

	@Override
	public REpiceaFileFilterList getFileFilters() {return new REpiceaFileFilterList(BiomassParameterFileFilter);}


	@Override
	public void reset() {
		setFilename(System.getProperty("user.home") + File.separator + BiomassParametersDialog.MessageID.Unnamed.toString());
		branchExpansionFactors.put(SpeciesType.BroadleavedSpecies, 1.612);
		branchExpansionFactors.put(SpeciesType.ConiferousSpecies, 1.300);
		branchExpansionFactorFromModel = false;
		branchExpansionFactorFromModelEnabled = false;
		
		rootExpansionFactors.put(SpeciesType.BroadleavedSpecies, 1.280);
		rootExpansionFactors.put(SpeciesType.ConiferousSpecies, 1.300);
		rootExpansionFactorFromModel = false;
		rootExpansionFactorFromModelEnabled = false;

		basicWoodDensityFactors.put(SpeciesType.BroadleavedSpecies, 0.500);
		basicWoodDensityFactors.put(SpeciesType.ConiferousSpecies, 0.350);
		carbonContentFactors.put(SpeciesType.BroadleavedSpecies, CATCarbonContentRatioProvider.AverageCarbonContent.Hardwood.getRatio());
		carbonContentFactors.put(SpeciesType.ConiferousSpecies, CATCarbonContentRatioProvider.AverageCarbonContent.Softwood.getRatio());
		
		basicWoodDensityFromModel = false;
		basicWoodDensityFromModelEnabled = false;
		carbonContentFromModel = false;
		carbonContentFromModelEnabled = false;
		if (referent != null) {
			testReferent(referent);
		}
	}

	private void testReferent(Object referent) {
		branchExpansionFactorFromModelEnabled = CATAboveGroundVolumeProvider.checkEligibility(referent) || 
												CATAboveGroundBiomassProvider.checkEligibility(referent) || 
												CATAboveGroundCarbonProvider.checkEligibility(referent);
		branchExpansionFactorFromModel = branchExpansionFactorFromModelEnabled;
		rootExpansionFactorFromModelEnabled = 	CATBelowGroundVolumeProvider.checkEligibility(referent) || 
												CATBelowGroundBiomassProvider.checkEligibility(referent) || 
												CATBelowGroundCarbonProvider.checkEligibility(referent);
		rootExpansionFactorFromModel = rootExpansionFactorFromModelEnabled;
		basicWoodDensityFromModelEnabled = referent instanceof CATBasicWoodDensityProvider;
		basicWoodDensityFromModel = basicWoodDensityFromModelEnabled;
		carbonContentFromModelEnabled = referent instanceof CATCarbonContentRatioProvider;
		carbonContentFromModel = carbonContentFromModelEnabled;
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, 
				Level.INFO,
				getClass().getName(),
				"Referent tested for agBEF " + branchExpansionFactorFromModelEnabled + 
									" - bgBEF " + rootExpansionFactorFromModelEnabled +
									" - wood density " + basicWoodDensityFromModelEnabled +
									" - carbon ratio " + carbonContentFromModelEnabled);
	}

	/**
	 * Check the consistency of Tier 2 approaches if they have been enabled. <p>
	 * 
	 * The method will return false if the Tier 2 approach has been enabled but
	 * the model does not allow it.
	 * @return a boolean
	 */
	public boolean isValid() {
		if (!branchExpansionFactorFromModelEnabled && branchExpansionFactorFromModel) {
			return false;
		} else if (!rootExpansionFactorFromModelEnabled && rootExpansionFactorFromModel) {
			return false;
		} else if (!basicWoodDensityFromModelEnabled && basicWoodDensityFromModel) {
			return false;
		} else if (!carbonContentFromModelEnabled && carbonContentFromModel) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Component getUI(Container parent) {
		if (guiInterface == null) {
			guiInterface = new BiomassParametersDialog((Window) parent, this);
		}
		return guiInterface;
	}

	@Override
	public void showUI(Window parent) {
		getUI(parent).setVisible(true);
	}

	@Override
	public void save(String filename) throws IOException {
		setFilename(filename);
		XmlSerializer serializer = new XmlSerializer(filename);
		serializer.writeObject(this);
	}

	public String getName() {
		File file = new File(filename);
		return ObjectUtility.relativizeTheseFile(file.getParentFile(), file).toString();
	}

	@Override
	public void load(String filename) throws IOException {
		XmlDeserializer deserializer = new XmlDeserializer(filename);
		BiomassParameters newManager;
		newManager = (BiomassParameters) deserializer.readObject();
		newManager.setFilename(filename);
		unpackMemorizerPackage(newManager.getMemorizerPackage());
	}

	private void setFilename(String filename) {this.filename = filename;}
	
	@Override
	public String getFilename() {return filename;}

	@Override
	public MemorizerPackage getMemorizerPackage() {
		MemorizerPackage mp = new MemorizerPackage();
		mp.add(filename);
		mp.add(branchExpansionFactors);
		mp.add(rootExpansionFactors);
		mp.add(basicWoodDensityFactors);
		mp.add(carbonContentFactors);
		mp.add(branchExpansionFactorFromModel);
		mp.add(rootExpansionFactorFromModel);
		mp.add(basicWoodDensityFromModel);
		mp.add(carbonContentFromModel);
		return mp;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void unpackMemorizerPackage(MemorizerPackage wasMemorized) {
		branchExpansionFactors.clear();
		rootExpansionFactors.clear();
		basicWoodDensityFactors.clear();
		carbonContentFactors.clear();
		filename = (String) wasMemorized.get(0);
		branchExpansionFactors.putAll((HashMap) wasMemorized.get(1));
		rootExpansionFactors.putAll((HashMap) wasMemorized.get(2));
		basicWoodDensityFactors.putAll((HashMap) wasMemorized.get(3));
		carbonContentFactors.putAll((HashMap) wasMemorized.get(4));
		branchExpansionFactorFromModel = (Boolean) wasMemorized.get(5);
		rootExpansionFactorFromModel = (Boolean) wasMemorized.get(6);
		basicWoodDensityFromModel = (Boolean) wasMemorized.get(7);
		carbonContentFromModel = (Boolean) wasMemorized.get(8);
	}
	
	
	
	
	/**
	 * Provide the basic density factor for this tree.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return a double (Mg/m3)
	 */
	public double getBasicWoodDensityFromThisTree(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		boolean tier2Implementation = basicWoodDensityFromModel && tree instanceof CATBasicWoodDensityProvider;
		boolean isStochastic = false;
		double value;
		if (tier2Implementation) {
			CATBasicWoodDensityProvider t = (CATBasicWoodDensityProvider) tree;
			value = t.getBasicWoodDensity();
			isStochastic = t.isBasicWoodDensityPredictorStochastic();
		} else {
			value = tree.getCATSpecies().getBasicWoodDensity();
		}
		if (subject != null && !isStochastic) {	// isStochastic = false if the provider is not stochastic or if the tree does not implement the provider
			return value * CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
		} else {
			return value;
		}
	}

	/**
	 * Provide the carbon content ratio for this tree, INCLUDING bark.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return a double
	 */
	public double getCarbonContentFromThisTree(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		boolean tier2Implementation = carbonContentFromModel && tree instanceof CATCarbonContentRatioProvider;
		boolean isStochastic = false;
		double value;
		if (tier2Implementation) {
			CATCarbonContentRatioProvider t = (CATCarbonContentRatioProvider) tree;
			value = t.getCarbonContentRatio();
			isStochastic = t.isCarbonContentRatioPredictorStochastic();
		} else {
			value = carbonContentFactors.get(tree.getCATSpecies().getSpeciesType());
		}
		if (subject != null && !isStochastic) {	// isStochastic = false if the provider is not stochastic or if the tree does not implement the provider
			return value * CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.CarbonContent, subject, getGroupId(VariabilitySource.CarbonContent, tree));
		} else {
			return value;
		}
	}

	
	/**
	 * Provide the belowground carbon content for this tree.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the carbon content (Mg)
	 */
	public double getBelowGroundCarbonMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getBelowGroundCarbonMgCache().containsKey(tree)) {
			boolean tier2Implementation = rootExpansionFactorFromModel && CATBelowGroundCarbonProvider.checkEligibility(tree);
			double value;
			if (tier2Implementation) {
				CATBelowGroundCarbonProvider t = (CATBelowGroundCarbonProvider) tree;
				value = t.getBelowGroundCarbonMg() * getExpansionFactor(tree);
				if (!t.isBelowGroundCarbonPredictorStochastic()) {	// will rely on sensitivity analysis instead
					double biomassModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, getGroupId(VariabilitySource.BiomassExpansionFactor, tree));
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					double carbonModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.CarbonContent, subject, getGroupId(VariabilitySource.CarbonContent, tree));
					value *= biomassModifier * woodDensityModifier * carbonModifier;
				}
			} else {
				value = getBelowGroundBiomassMg(tree, subject) * getCarbonContentFromThisTree(tree, subject);
			}
			getBelowGroundCarbonMgCache().put(tree, value);
		}
		return getBelowGroundCarbonMgCache().get(tree);
	}
	
	/**
	 * Provide the belowground biomass of this tree
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the biomass (Mg)
	 */
	public double getBelowGroundBiomassMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getBelowGroundBiomassMgCache().containsKey(tree)) {
			boolean tier2Implementation = rootExpansionFactorFromModel && CATBelowGroundBiomassProvider.checkEligibility(tree);
			double value;
			if (tier2Implementation) {
				CATBelowGroundBiomassProvider t = (CATBelowGroundBiomassProvider) tree;
				value = t.getBelowGroundBiomassMg() * getExpansionFactor(tree);
				if (!t.isBelowGroundBiomassPredictorStochastic() && subject != null) { // will rely on sensitivity analysis instead
					double biomassModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, getGroupId(VariabilitySource.BiomassExpansionFactor, tree));
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					value *= biomassModifier * woodDensityModifier;
				}
			} else {
				value = getAboveGroundBiomassMg(tree, subject) * (rootExpansionFactors.get(tree.getCATSpecies().getSpeciesType()) - 1);		// minus 1 is required because we want to get only the belowground part;
				value *= CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, getGroupId(VariabilitySource.BiomassExpansionFactor, tree));
			}
			getBelowGroundBiomassMgCache().put(tree, value);
		}
		return getBelowGroundBiomassMgCache().get(tree);
	}

	/**
	 * Clear all the caches.
	 */
	public void clearCache() {
		getBelowGroundVolumeM3Cache().clear();
		getBelowGroundBiomassMgCache().clear();
		getBelowGroundCarbonMgCache().clear();
		getAboveGroundVolumeM3Cache().clear();
		getAboveGroundBiomassMgCache().clear();
		getAboveGroundCarbonMgCache().clear();
		getCommercialVolumeM3Cache().clear();
		getCommercialBiomassMgCache().clear();
		getCommercialCarbonMgCache().clear();
	}

	
	
	/**
	 * Provide the belowground volume of a particular tree, INCLUDING bark 
	 * @param tree a CarbonToolCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the volume (m3)
	 */
	public double getBelowGroundVolumeM3(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getBelowGroundVolumeM3Cache().containsKey(tree)) {
			boolean tier2Implementation = rootExpansionFactorFromModel && CATBelowGroundVolumeProvider.checkEligibility(tree);
			double value;
			boolean isStochastic = false;
			if (tier2Implementation) {
				CATBelowGroundVolumeProvider t = (CATBelowGroundVolumeProvider) tree;
				value = t.getBelowGroundVolumeM3() * getExpansionFactor(tree);
				isStochastic = t.isBelowGroundVolumePredictorStochastic();
			} else {
				value = getAboveGroundVolumeM3(tree, subject) * (rootExpansionFactors.get(tree.getCATSpecies().getSpeciesType()) - 1);		// minus 1 is required because we want to get only the belowground part
			}
			
			if (subject != null && !isStochastic) {	// isStochastic = false if the provider is not stochastic or if the tree does not implement the provider
				String groupId = getGroupId(VariabilitySource.BiomassExpansionFactor, tree);
				value *= CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, groupId);
			} 
			getBelowGroundVolumeM3Cache().put(tree, value);
		}
		return getBelowGroundVolumeM3Cache().get(tree);
	}

	
	/**
	 * Provide the aboveground carbon of this tree, INCLUDING bark.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return a double (Mg)
	 */
	public double getAboveGroundCarbonMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getAboveGroundCarbonMgCache().containsKey(tree)) {
			boolean tier2Implementation = branchExpansionFactorFromModel && CATAboveGroundCarbonProvider.checkEligibility(tree); 
			double value;
			if (tier2Implementation) {
				CATAboveGroundCarbonProvider t = (CATAboveGroundCarbonProvider) tree;
				value = t.getAboveGroundCarbonMg() * getExpansionFactor(tree);
				if (!t.isAboveGroundCarbonPredictorStochastic() && subject != null) {	// then rely on sensitivity analysis if enabled
					double biomassModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, getGroupId(VariabilitySource.BiomassExpansionFactor, tree));
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					double carbonModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.CarbonContent, subject, getGroupId(VariabilitySource.CarbonContent, tree));
					value *= biomassModifier * woodDensityModifier * carbonModifier;
				}
			} else {
				value = getAboveGroundBiomassMg(tree, subject) * getCarbonContentFromThisTree(tree, subject);
			}
			getAboveGroundCarbonMgCache().put(tree, value);
		}
		return getAboveGroundCarbonMgCache().get(tree);
	}
	
	/**
	 * Provide the aboveground biomass of this tree.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the aboveground biomass (Mg)
	 */
	public double getAboveGroundBiomassMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getAboveGroundBiomassMgCache().containsKey(tree)) {
			boolean tier2Implementation = (branchExpansionFactorFromModel && CATAboveGroundBiomassProvider.checkEligibility(tree)) || 
					tree instanceof CATSapling; // saplings automatically provide their own biomass
			double value;
			if (tier2Implementation) {
				CATAboveGroundBiomassProvider t = (CATAboveGroundBiomassProvider) tree;
				value = t.getAboveGroundBiomassMg() * getExpansionFactor(tree);
				if (!t.isAboveGroundBiomassPredictorStochastic() && subject != null) { // then rely on sensitivity analysis if enabled
					double biomassModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, getGroupId(VariabilitySource.BiomassExpansionFactor, tree));
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					value *= biomassModifier * woodDensityModifier;
				}
			} else if (tree instanceof CATCommercialBiomassProvider) {		
				value = getCommercialBiomassMg(tree, subject) * branchExpansionFactors.get(tree.getCATSpecies().getSpeciesType());
				if (subject != null) {	
					String subjectId = getGroupId(VariabilitySource.BiomassExpansionFactor, tree);
					value *= CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, subjectId);
				} 
			} else {
				value = getAboveGroundVolumeM3(tree, subject) * getBasicWoodDensityFromThisTree(tree, subject);
			}
			getAboveGroundBiomassMgCache().put(tree, value);
		} 
		return getAboveGroundBiomassMgCache().get(tree);
	}

	
	/**
	 * Provide the aboveground volume of a particular tree (m3), INCLUDING bark and
	 * all weighting factors.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the aboveground volume (m3)
	 */
	public double getAboveGroundVolumeM3(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getAboveGroundVolumeM3Cache().containsKey(tree)) {
			boolean tier2Implementation = branchExpansionFactorFromModel && CATAboveGroundVolumeProvider.checkEligibility(tree);
			boolean isStochastic = false;
			double value;
			if (tier2Implementation) {
				CATAboveGroundVolumeProvider t = (CATAboveGroundVolumeProvider) tree;
				value = t.getAboveGroundVolumeM3() * getExpansionFactor(tree);
				isStochastic = t.isAboveGroundVolumePredictorStochastic();
			} else {
				value = getCommercialVolumeM3(tree) * branchExpansionFactors.get(tree.getCATSpecies().getSpeciesType());
			}
			
			if (subject != null && !isStochastic) {	// isStochastic = false if the provider is not stochastic or if the tree does not implement the provider
				String subjectId = getGroupId(VariabilitySource.BiomassExpansionFactor, tree);
				value *= CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BiomassExpansionFactor, subject, subjectId);
			} 
			getAboveGroundVolumeM3Cache().put(tree, value);
		}
		return getAboveGroundVolumeM3Cache().get(tree);
	}

	/**
	 * Build a string from the species name or species type.<p>
	 * This string is then used as a group for the sensitivity analysis.
	 * @param source a VariabilitySource enum
	 * @param tree a CATCompatibleTree instance
	 * @return a string that stands for the group id
	 * @see VariabilitySource
	 */
	protected String getGroupId(VariabilitySource source, CATCompatibleTree tree) {
		switch(source) {
		case BiomassExpansionFactor:
			if (branchExpansionFactorFromModel || rootExpansionFactorFromModel) {
				return tree.getSpeciesName();
			} else {
				return tree.getCATSpecies().getSpeciesType().name();
			}
		case BasicDensity:
			if (basicWoodDensityFromModel) {
				return tree.getSpeciesName();
			} else {
				return tree.getCATSpecies().getSpeciesType().name();
			}
		case CarbonContent:
			if (carbonContentFromModel) {
				return tree.getSpeciesName();
			} else {
				return tree.getCATSpecies().getSpeciesType().name();
			}
		default:
			return null;
		}
	}
	
	private double getExpansionFactor(CATCompatibleTree tree) {
		return tree.getNumber() * tree.getPlotWeight();
	}
	
	/**
	 * Provide the overbark commercial volume of the tree weighted by the expansion factor.
	 * @param tree a CATCompatibleTree instance
	 * @return the overbark commercial volume (m3)
	 */
	public double getCommercialVolumeM3(CATCompatibleTree tree) {
		if (!getCommercialVolumeM3Cache().containsKey(tree)) {
			double value = getOverbarkCommercialVolumeM3(tree) * getExpansionFactor(tree);
			getCommercialVolumeM3Cache().put(tree, value);
		}
		return getCommercialVolumeM3Cache().get(tree);
	}
	

	/**
	 * This method returns the volume over bark of a CATCompatibleTree instance.
	 * @param tree a CATCompatibleTree instance
	 * @return the volume of a single tree, i.e. WITHOUT any expansion factor.
	 */
	private double getOverbarkCommercialVolumeM3(CATCompatibleTree tree) {
		double commVolume = tree.getCommercialVolumeM3();
		if (!tree.isCommercialVolumeOverbark()) {
			commVolume += tree.getBarkProportionOfWoodVolume() * commVolume;
		}
		return commVolume;
	}
	
	/**
	 * Provide commercial biomass of this tree weighted by the expansion factor.
	 * @param tree a CATCompatibleTree instance
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the commercial biomass (Mg)
	 */
	public double getCommercialBiomassMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getCommercialBiomassMgCache().containsKey(tree)) {
			boolean tier2Implementation = CATCommercialBiomassProvider.checkEligibility(tree);
			double value;
			if (tier2Implementation) {
				CATCommercialBiomassProvider t = (CATCommercialBiomassProvider) tree;
				value = t.getCommercialBiomassMg() * getExpansionFactor(tree);
				if (!t.isCommercialBiomassPredictorStochastic() && subject != null) { // then rely on sensitivity analysis if enabled
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					value *= woodDensityModifier;
				} 
			} else {
				value = getCommercialVolumeM3(tree) * getBasicWoodDensityFromThisTree(tree, subject);
			}
			getCommercialBiomassMgCache().put(tree, value);
		}
		return getCommercialBiomassMgCache().get(tree);
	}

	/**
	 * Provide the carbon in the commercial part of this tree weighted by the expansion factor.
	 * @param tree a CarbonCompatibleTree
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the carbon in the commercial part of the tree (Mg)
	 */
	public double getCommercialCarbonMg(CATCompatibleTree tree, MonteCarloSimulationCompliantObject subject) {
		if (!getCommercialCarbonMgCache().containsKey(tree)) {
			boolean tier2Implementation = CATCommercialCarbonProvider.checkEligibility(tree); 
			double value;
			if (tier2Implementation) {
				CATCommercialCarbonProvider t = (CATCommercialCarbonProvider) tree;
				value = t.getCommercialCarbonMg() * getExpansionFactor(tree);
				if (!t.isCommercialCarbonPredictorStochastic() && subject != null) {	// then rely on sensitivity analysis if enabled
					double woodDensityModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.BasicDensity, subject, getGroupId(VariabilitySource.BasicDensity, tree));
					double carbonModifier = CATSensitivityAnalysisSettings.getInstance().getModifier(VariabilitySource.CarbonContent, subject, getGroupId(VariabilitySource.CarbonContent, tree));
					value *= woodDensityModifier * carbonModifier;
				}
			} else {
				value = getCommercialBiomassMg(tree, subject) * getCarbonContentFromThisTree(tree, subject);
			}
			getCommercialCarbonMgCache().put(tree, value);
		}
		return getCommercialCarbonMgCache().get(tree);
	}
	
	/**
	 * Provide the aboveground carbon for a collection of trees.
	 * @param trees a Collection of CATCompatibleTree instances
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the aboveground carbon content (Mg)
	 */
	public double getAboveGroundCarbonMg(Collection<CATCompatibleTree> trees, MonteCarloSimulationCompliantObject subject) {
		double totalAboveGroundCarbonMg = 0d;
		if (trees != null) {
			for (CATCompatibleTree tree : trees) {
				totalAboveGroundCarbonMg += getAboveGroundCarbonMg(tree, subject);
			}
		}
		return totalAboveGroundCarbonMg;
	}

	/**
	 * Provide the belowground carbon for a collection of trees.
	 * @param trees a Collection of CATCompatibleTree instances
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the belowground carbon content (Mg)
	 */
	public double getBelowGroundCarbonMg(Collection<CATCompatibleTree> trees, MonteCarloSimulationCompliantObject subject) {
		double totalBelowGroundCarbonMg = 0d;
		if (trees != null) {
			for (CATCompatibleTree tree : trees) {
				totalBelowGroundCarbonMg += getBelowGroundCarbonMg(tree, subject);
			}
		}
		return totalBelowGroundCarbonMg;
	}


	/**
	 * Provide the carbon in the commercial part for a collection of trees.
	 * @param trees a Collection of CATCompatibleTree instances
	 * @param subject a MonteCarloSimulationCompliantObject instance (typically the CATCompartmentManager instance)
	 * @return the carbon content in the commercial part of trees (Mg)
	 */
	public double getCommercialCarbonMg(Collection<CATCompatibleTree> trees, MonteCarloSimulationCompliantObject subject) {
		double commercialCarbonMg = 0d;
		if (trees != null) {
			for (CATCompatibleTree tree : trees) {
				commercialCarbonMg += getCommercialCarbonMg(tree, subject);
			}
		}
		return commercialCarbonMg;
	}
	
	/**
	 * Provide the annual carbon from the litterfall.
	 * @param trees a List of MEMSCompatibleTree instances
	 * @param subject a MonteCarloSimulationCompliantObject instance
	 * @return the amount of carbon (Mg)
	 */
	public double getLitterFallAnnualCarbonMg(Collection<MEMSCompatibleTree> trees, MonteCarloSimulationCompliantObject subject) {
		double totalCarbonMg = 0d;
		if (trees != null) {
			for (MEMSCompatibleTree tree : trees) {
				double treeContribution = tree.getAnnualFoliarDetritusCarbonProductionMgYr() + tree.getAnnualBranchDetritusCarbonProductionMgYr();
				totalCarbonMg += treeContribution * getExpansionFactor(tree);
			}
		}
		return totalCarbonMg;
	}

	
	/**
	 * Provide the annual carbon from fine root turnover.
	 * @param trees a List of MEMSCompatibleTree instances
	 * @param subject a MonteCarloSimulationCompliantObject instance
	 * @return the amount of carbon (Mg)
	 */
	public double getFineRootDetritusAnnualCarbonMg(Collection<MEMSCompatibleTree> trees, MonteCarloSimulationCompliantObject subject) {
		double totalCarbonMg = 0d;
		if (trees != null) {
			for (MEMSCompatibleTree tree : trees) {
				double treeContribution = tree.getAnnualFineRootDetritusCarbonProductionMgYr();
				totalCarbonMg += treeContribution * getExpansionFactor(tree);
			}
		}
		return totalCarbonMg;
	}


	@Override
	public boolean isVisible() {
		return guiInterface != null && guiInterface.isVisible();
	}

//	public static void main(String[] args) {
//		BiomassParameters bp = new BiomassParameters();
//		bp.showUI(null);
//	}

}
