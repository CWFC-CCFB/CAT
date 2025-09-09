/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2010-2017 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
 * Copyright (C) 2024 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, 
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
package lerfob.carbonbalancetool.io;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import lerfob.carbonbalancetool.CATCompatibleStand;
import repicea.io.tools.ImportFieldElement;
import repicea.io.tools.ImportFieldElement.FieldType;
import repicea.io.tools.LevelProviderEnum;
import repicea.io.tools.REpiceaRecordReader;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.covariateproviders.samplelevel.ManagementTypeProvider.ManagementType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * The CATGrowthSimulationRecordReader class allows to import stochastic or deterministic growth simulation. <p> 
 * 
 * These simulations are assumed to be run at the forest management unit level under uneven-aged management.
 * 
 * @author Mathieu Fortin - July 2017
 */
@SuppressWarnings("serial")
public class CATGrowthSimulationRecordReader extends REpiceaRecordReader {
	
	protected static enum MessageID implements TextableEnum {
		DateDescription("Date (years)","Date (ann\u00E9es)"),
		DateHelp("This field must contains the date. It is an integer.", "Ce champ doit contenir la date. Il s'agit d'un entier."),
		RealizationDescription("Realization", "R\u00E9alisation"),
		RealizationHelp("This field must contain the realization identifier. It is an integer. This field is optional. If it is not specified, CAT considers that the simulation is deterministic.", 
				"Ce champ doit contenir l'identifiant de la r\u00E9alisation. Il s'agit d'un entier. Ce champ est facultatif. S'il n'est pas sp\u00E9cifi\u00E9, CAT consid\u00E8re qu'il s'agit d'un simulation d\u00E9terministe."),
		PlotDescription("Plot ID", "Identifiant de placette"),
		PlotHelp("This field must contain the plot identifier. It is a string.", "Ce champ doit contenir l'identifiant de la placette. Il s'agit d'une cha\u00EEne de caract\u00E8res."),
		PlotAreaDescription("Plot area (ha)", "Surface de la placette (ha)"),
		PlotAreaHelp("This field must contain the plot area in hectare. It is a double.", "Ce champ doit contenir la surface de la placette en hectare. Il s'agit d'un double."),
		InterventionResultDescription("Recently harvested", "R\u00E9cemment r\u00E9colt\u00E9"),
		InterventionResultHelp("This field must contain an integer whose value is 1 if the plot has been recently harvested or 0 otherwise.", "Ce champ doit contenir un entier qui prend la valeur de 1 si la placette a \u00E9t\u00E9 r\u00E9cemment r\u00E9colt\u00E9 ou 0 dans le cas contraire."),
		TreeSpeciesDescription("Species", "Esp\u00E8ce"),
		TreeSpeciesHelp("This field must contain the species name. It is a string", "Ce champ doit contenir le nom de l'esp\u00E8ce. Il s'agit d'une cha\u00EEine de caract\u00E8res."),
		TreeStatusDescription("Tree status", "Etat de l'arbre"),
		TreeStatusHelp("This field must contain the status of the tree. It must be one of these four values:  alive, cut, dead, or windfall.", 
				"Ce champ doit contenir l'\u00E9tat de l'arbre. La valeur doit \u00EAtre l'une des quatre valeurs suivantes: alive, cut, dead, or windfall."),
		TreeDBHDescription("Tree DBH", "Diam\u00E8tre d'arbre"),
		TreeDBHHelp("This field must contain the diameter at breast height (cm). It is a double. This field is optional.",
				"Ce champ doit contenir le diam\u00E8tre \u00E0 1,3 m (cm). Il s'agit d'un double. Ce champ est facultatif."),
		TreeFreqDescription("Tree frequency", "Fr\u00E9quence d'arbre"),
		TreeFreqHelp("This field must contain the number of trees represented by the record in the plot. It is a double. This field is optional. If it is not specified, CAT assumes that the frequency is one.",
				"Ce champ doit contenir le nombre d'arbres repr\u00E9sent\u00E9s par cet enregistrement dans la placette. Il s'agit d'un double. Ce champ est facultatif. S'il n'est pas sp\u00E9cifi\u00E9, CAT utilise une fr\u00E9quence unitaire."),
		TreeVolumeDescription("Tree overbark commercial volume (m3)", "Volume commercial sur \u00E9corce (m3)"),
		TreeVolumeHelp("This field contains the commercial volume (m3) for a single tree. It is a double.", "Ce champ contient le volume commercial (m3) d'un arbre individuel. Il s'agit d'un double."),
		TreeAboveGroundVolumeDescription("Tree overbark aboveground volume (m3)", "Volume a\u00E9rien sur \u00E9corce (m3)"),
		TreeAboveGroundVolumeHelp("This field contains the overbark aboveground volume (m3) for a single tree. It is a double.", "Ce champ contient le volume a\u00E9rien sur \u00E9corce (m3) d'un arbre individuel. Il s'agit d'un double."),
		TreeAboveGroundBiomassDescription("Tree overbark aboveground biomass (Mg)", "Biomasse a\u00E9rienne sur \u00E9corce (Mg)"),
		TreeAboveGroundBiomassHelp("This field contains the overbark aboveground biomass (Mg) for a single tree. It is a double.", "Ce champ contient la biomasse a\u00E9rienne sur \u00E9corce (Mg) d'un arbre individuel. Il s'agit d'un double."),
		TreeAboveGroundCarbonDescription("Tree overbark aboveground carbon (Mg)", "Carbone a\u00E9rien sur \u00E9corce (Mg)"),
		TreeAboveGroundCarbonHelp("This field contains the overbark aboveground carbon (Mg) for a single tree. It is a double.", "Ce champ contient le carbone a\u00E9rien sur \u00E9corce (Mg) d'un arbre individuel. Il s'agit d'un double."),
		TreeBelowGroundVolumeDescription("Tree overbark belowground volume (m3)", "Volume souterrain sur \u00E9corce (m3)"),
		TreeBelowGroundVolumeHelp("This field contains the overbark belowground volume (m3) for a single tree. It is a double.", "Ce champ contient le volume souterrain sur \u00E9corce (m3) d'un arbre individuel. Il s'agit d'un double."),
		TreeBelowGroundBiomassDescription("Tree overbark belowground biomass (Mg)", "Biomasse souterraine sur \u00E9corce (Mg)"),
		TreeBelowGroundBiomassHelp("This field contains the overbark belowground biomass (Mg) for a single tree. It is a double.", "Ce champ contient la biomasse souterraine sur \u00E9corce (Mg) d'un arbre individuel. Il s'agit d'un double."),
		TreeBelowGroundCarbonDescription("Tree overbark belowground carbon (Mg)", "Carbone souterrain sur \u00E9corce (Mg)"),
		TreeBelowGroundCarbonHelp("This field contains the overbark belowground carbon (Mg) for a single tree. It is a double.", "Ce champ contient le carbone souterrain sur \u00E9corce (Mg) d'un arbre individuel. Il s'agit d'un double."),
		InconsistentGrowthSimulation("The number of realizations is inconsistent along the projection!", "Le nombre de r\u00E9alisations n'est pas constant tout au long de la simulation!"),
		TreeCommercialBiomassDescription("Tree overbark commercial biomass (Mg)", "Biomasse commerciale sur \u00E9corce (Mg)"),
		TreeCommercialBiomassHelp("This field contains the overbark commercial biomass (Mg) for a single tree. It is a double.", "Ce champ contient la biomasse commerciale sur \u00E9corce (Mg) d'un arbre individuel. Il s'agit d'un double."),
		;
		
		MessageID(String englishText, String frenchText) {
			setText(englishText, frenchText);
		}
				
		@Override
		public void setText(String englishText, String frenchText) {
			REpiceaTranslator.setString(this, englishText, frenchText);
		}
		
		@Override
		public String toString() {
			return REpiceaTranslator.getString(this);
		}
	}

//	public static boolean TestUnevenAgedInfiniteSequence = false;
	
	protected static enum CATGrowthSimulationFieldLevel {Stand, Tree;}

	protected static enum CATGrowthSimulationFieldID implements LevelProviderEnum {
		Date(CATGrowthSimulationFieldLevel.Stand),
		Realization(CATGrowthSimulationFieldLevel.Stand),
		PlotID(CATGrowthSimulationFieldLevel.Stand),
		PlotArea(CATGrowthSimulationFieldLevel.Stand),
		InterventionResult(CATGrowthSimulationFieldLevel.Stand),
		Species(CATGrowthSimulationFieldLevel.Tree),
		Status(CATGrowthSimulationFieldLevel.Tree),
		DBH(CATGrowthSimulationFieldLevel.Tree),
		Freq(CATGrowthSimulationFieldLevel.Tree),
		Volume(CATGrowthSimulationFieldLevel.Tree),
		AboveGroundVolume(CATGrowthSimulationFieldLevel.Tree),
		AboveGroundBiomass(CATGrowthSimulationFieldLevel.Tree),
		AboveGroundCarbon(CATGrowthSimulationFieldLevel.Tree),
		BelowGroundVolume(CATGrowthSimulationFieldLevel.Tree),
		BelowGroundBiomass(CATGrowthSimulationFieldLevel.Tree),
		BelowGroundCarbon(CATGrowthSimulationFieldLevel.Tree),
		CommercialBiomass(CATGrowthSimulationFieldLevel.Tree)
		;
		
		
		private final CATGrowthSimulationFieldLevel level;
		
		CATGrowthSimulationFieldID(CATGrowthSimulationFieldLevel level) {
			this.level = level;
		}

		@Override
		public CATGrowthSimulationFieldLevel getFieldLevel() {return level;}
	}

	
	private final Map<Integer, Map<Boolean, CATGrowthSimulationCompositeStand>> standMap;
	
	private CATGrowthSimulationSpeciesSelector selector;
	private final List<String> speciesList;
	protected final ApplicationScale scale;
	protected final ManagementType management;
	
	/**
	 * General constructor.
	 * @param scale an ApplicationScale enum
	 * @param management a ManagementType enum
	 */
	public CATGrowthSimulationRecordReader(ApplicationScale scale, ManagementType management) {
		super();
//		this.scale = TestUnevenAgedInfiniteSequence ? ApplicationScale.Stand : ApplicationScale.FMU;
		if (scale == null || management == null) {
			throw new InvalidParameterException("The scale and management arguments must be non null!");
		}
		this.scale = scale;
		this.management = management;
		setPopUpWindowEnabled(true);
		standMap = new TreeMap<Integer, Map<Boolean, CATGrowthSimulationCompositeStand>>();
		speciesList = new ArrayList<String>();
	}

	
	/**
	 * This method returns the selector for the species in CAT.
	 * @return a REpiceaMatchSelector instance
	 */
	public CATGrowthSimulationSpeciesSelector getSelector() {
		if (selector == null) {
			selector = new CATGrowthSimulationSpeciesSelector(speciesList.toArray());
		}
		return selector;
	}
	
	@Override
	protected List<ImportFieldElement> defineFieldsToImport() {
		List<ImportFieldElement> ifeList = new ArrayList<ImportFieldElement>();
		ImportFieldElement ife;
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Date,
				MessageID.DateDescription.toString(), 
				getClass().getSimpleName() + ".dateDescription", 
				false, 
				MessageID.DateHelp.toString(),
				FieldType.Integer);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Realization,
				MessageID.RealizationDescription.toString(), 
				getClass().getSimpleName() + ".realizationDescription", 
				true, 
				MessageID.RealizationHelp.toString(),
				FieldType.Integer);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.PlotID,
				MessageID.PlotDescription.toString(), 
				getClass().getSimpleName() + ".plotDescription", 
				false, 
				MessageID.PlotHelp.toString(),
				FieldType.String);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.PlotArea,
				MessageID.PlotAreaDescription.toString(), 
				getClass().getSimpleName() + ".plotAreaDescription", 
				false, 
				MessageID.PlotAreaHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.InterventionResult,
				MessageID.InterventionResultDescription.toString(), 
				getClass().getSimpleName() + ".interventionResultDescription", 
				true, 
				MessageID.InterventionResultHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Species,
				MessageID.TreeSpeciesDescription.toString(), 
				getClass().getSimpleName() + ".treeSpeciesDescription", 
				false, 
				MessageID.TreeSpeciesHelp.toString(),
				FieldType.String);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Status,
				MessageID.TreeStatusDescription.toString(), 
				getClass().getSimpleName() + ".treeStatusDescription", 
				false, 
				MessageID.TreeStatusHelp.toString(),
				FieldType.String);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.DBH,
				MessageID.TreeDBHDescription.toString(), 
				getClass().getSimpleName() + ".treeDBHDescription", 
				true, 
				MessageID.TreeDBHHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Freq,
				MessageID.TreeFreqDescription.toString(), 
				getClass().getSimpleName() + ".treeFreqDescription", 
				true, 
				MessageID.TreeFreqHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.Volume,
				MessageID.TreeVolumeDescription.toString(), 
				getClass().getSimpleName() + ".treeVolumeDescription", 
				false, 
				MessageID.TreeVolumeHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.CommercialBiomass,
				MessageID.TreeCommercialBiomassDescription.toString(), 
				getClass().getSimpleName() + ".treeCommercialBiomassDescription", 
				true, 
				MessageID.TreeCommercialBiomassHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.AboveGroundVolume,
				MessageID.TreeAboveGroundVolumeDescription.toString(), 
				getClass().getSimpleName() + ".treeAboveGroundVolumeDescription", 
				true, 
				MessageID.TreeAboveGroundVolumeHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.AboveGroundBiomass,
				MessageID.TreeAboveGroundBiomassDescription.toString(), 
				getClass().getSimpleName() + ".treeAboveGroundBiomassDescription", 
				true, 
				MessageID.TreeAboveGroundBiomassHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.AboveGroundCarbon,
				MessageID.TreeAboveGroundCarbonDescription.toString(), 
				getClass().getSimpleName() + ".treeAboveGroundCarbonDescription", 
				true, 
				MessageID.TreeAboveGroundCarbonHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);

		ife = new ImportFieldElement(CATGrowthSimulationFieldID.BelowGroundVolume,
				MessageID.TreeBelowGroundVolumeDescription.toString(), 
				getClass().getSimpleName() + ".treeBelowGroundVolumeDescription", 
				true, 
				MessageID.TreeBelowGroundVolumeHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.BelowGroundBiomass,
				MessageID.TreeBelowGroundBiomassDescription.toString(), 
				getClass().getSimpleName() + ".treeBelowGroundBiomassDescription", 
				true, 
				MessageID.TreeBelowGroundBiomassHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);
		ife = new ImportFieldElement(CATGrowthSimulationFieldID.BelowGroundCarbon,
				MessageID.TreeBelowGroundCarbonDescription.toString(), 
				getClass().getSimpleName() + ".treeBelowGroundCarbonDescription", 
				true, 
				MessageID.TreeBelowGroundCarbonHelp.toString(),
				FieldType.Double);
		ifeList.add(ife);

		
		return ifeList;
	}

	@Override
	protected Enum<?> defineGroupFieldEnum() {return null;}

	@Override
	protected void readLineRecord(Object[] oArray, int lineCounter) throws VariableValueException, Exception {
		int index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Date);
		int dateYr = ((Number) oArray[index]).intValue();

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Realization);
		int realization = 0;			// default value for this optional field
		if (index != -1 && oArray[index] != null) {	// means that a realization field has been specified
			realization = ((Number) oArray[index]).intValue();
		} 

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.PlotID);
		String plotID = oArray[index].toString();

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.PlotArea);
		double plotAreaHa = ((Number) oArray[index]).doubleValue();
		
		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.InterventionResult);
		int interventionResult = 0;
		if (index != -1 && oArray[index] != null) { 	// means that a realization field has been specified
			interventionResult = ((Number) oArray[index]).intValue();
		}
		boolean isInterventionResult = interventionResult == 1;
		
		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Species);
		String originalSpeciesName = oArray[index].toString();
		
		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Status);
		StatusClass statusClass = StatusClass.valueOf(oArray[index].toString().toLowerCase().trim());

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.DBH);
		Double dbhCm = null;
		if (index != -1 && oArray[index] != null) { 	// means that a realization field has been specified
			dbhCm = ((Number) oArray[index]).doubleValue();
		}

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Freq);
		double numberOfTrees = 1d;		// default value for this optional field
		if (index != -1 && oArray[index] != null) { 	// means that a realization field has been specified
			numberOfTrees = ((Number) oArray[index]).doubleValue();
		}

		index = getImportFieldManager().getIndexOfThisField(CATGrowthSimulationFieldID.Volume);
		double treeOverbarkVolumeM3 = ((Number) oArray[index]).doubleValue();
		
		Double abovegroundVolumeM3 = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.AboveGroundVolume, oArray);
		Double abovegroundBiomassMg = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.AboveGroundBiomass, oArray);
		Double abovegroundCarbonMg = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.AboveGroundCarbon, oArray);
		Double belowgroundVolumeM3 = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.BelowGroundVolume, oArray);
		Double belowgroundBiomassMg = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.BelowGroundBiomass, oArray);
		Double belowgroundCarbonM3 = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.BelowGroundCarbon, oArray);
		Double commercialBiomassMg = this.getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID.CommercialBiomass, oArray);
		
		instantiatePlotAndTree(getImportFieldManager().getFileSpecifications()[0], 
				dateYr, 
				realization, 
				plotID, 
				plotAreaHa, 
				isInterventionResult, 
				statusClass, 
				treeOverbarkVolumeM3, 
				numberOfTrees, 
				originalSpeciesName, 
				dbhCm,
				abovegroundVolumeM3,
				abovegroundBiomassMg,
				abovegroundCarbonMg,
				belowgroundVolumeM3,
				belowgroundBiomassMg,
				belowgroundCarbonM3,
				commercialBiomassMg);
	}

	private Double getNonEmptyOptionalFieldValue(CATGrowthSimulationFieldID f, Object[] oArray) throws Exception {
		int index = getImportFieldManager().getIndexOfThisField(f);
		ImportFieldElement field = getImportFieldManager().getField(f);
		Double value = null;
		if (field.getMatchingFieldIndex() != -1) {
			if (oArray[index] == null) {
				throw new Exception("The optional field " + f.name() + " cannot contain any missing values!");
			} else {
				value = ((Number) oArray[index]).doubleValue();
			}
		}
		return value;
	}
		
	protected CATGrowthSimulationCompositeStand createCompositeStand(String standIdentification, int dateYr, boolean scaleDependentInterventionResult, Map<CATGrowthSimulationFieldID, Boolean> interfaceEnablingMap) {
		return new CATGrowthSimulationCompositeStand(dateYr, standIdentification, this, scaleDependentInterventionResult, interfaceEnablingMap);
	}

	private Map<CATGrowthSimulationFieldID, Boolean> createInterfaceEnablingMap(Double aboveGroundVolume,
			Double aboveGroundBiomass,
			Double aboveGroundCarbon,
			Double belowGroundVolume,
			Double belowGroundBiomass,
			Double belowGroundCarbon,
			Double commercialBiomassMg) {
		Map<CATGrowthSimulationFieldID, Boolean> map = new HashMap<CATGrowthSimulationFieldID, Boolean>();
		map.put(CATGrowthSimulationFieldID.AboveGroundVolume, aboveGroundVolume != null);
		map.put(CATGrowthSimulationFieldID.AboveGroundBiomass, aboveGroundBiomass != null);
		map.put(CATGrowthSimulationFieldID.AboveGroundCarbon, aboveGroundCarbon != null);
		map.put(CATGrowthSimulationFieldID.BelowGroundVolume, belowGroundVolume != null);
		map.put(CATGrowthSimulationFieldID.BelowGroundBiomass, belowGroundBiomass != null);
		map.put(CATGrowthSimulationFieldID.BelowGroundCarbon, belowGroundCarbon != null);
		map.put(CATGrowthSimulationFieldID.CommercialBiomass, commercialBiomassMg != null);
		return map;
	}
	
	protected void instantiatePlotAndTree(String standIdentification, 
			int dateYr, 
			int realization, 
			String plotID, 
			double plotAreaHa,
			boolean isInterventionResult, 
			StatusClass statusClass, 
			double treeOverbarkVolumeM3, 
			double numberOfTrees, 
			String originalSpeciesName, 
			Double dbhCm,
			Double aboveGroundVolumeM3,
			Double aboveGroundBiomassMg,
			Double aboveGroundCarbonMg,
			Double belowGroundVolumeM3,
			Double belowGroundBiomassMg,
			Double belowGroundCarbonMg,
			Double commercialBiomassMg) {
		if (!standMap.containsKey(dateYr)) {
			standMap.put(dateYr, new HashMap<Boolean, CATGrowthSimulationCompositeStand>());
		}
		
		Map<Boolean, CATGrowthSimulationCompositeStand> innerMap = standMap.get(dateYr);
		boolean scaleDependentInterventionResult = scale == ApplicationScale.FMU ? false : isInterventionResult;
		if (!innerMap.containsKey(scaleDependentInterventionResult)) {
			innerMap.put(scaleDependentInterventionResult, createCompositeStand(standIdentification, 
																				dateYr, 
																				scaleDependentInterventionResult,
																				createInterfaceEnablingMap(aboveGroundVolumeM3, 
																						aboveGroundBiomassMg, 
																						aboveGroundCarbonMg, 
																						belowGroundVolumeM3, 
																						belowGroundBiomassMg, 
																						belowGroundCarbonMg,
																						commercialBiomassMg)));
		}
		CATGrowthSimulationCompositeStand compositeStand = innerMap.get(scaleDependentInterventionResult);
		
		compositeStand.createRealizationIfNeeded(realization);
		CATGrowthSimulationPlotSample plotSample = compositeStand.getRealization(realization);
		plotSample.createPlot(plotID, plotAreaHa, isInterventionResult);
		CATGrowthSimulationPlot plot = plotSample.getPlot(plotID);
		
		CATGrowthSimulationTree tree = createTree(plot, 
				statusClass, 
				treeOverbarkVolumeM3, 
				numberOfTrees, 
				originalSpeciesName,
				dbhCm,
				aboveGroundVolumeM3, 
				aboveGroundBiomassMg, 
				aboveGroundCarbonMg, 
				belowGroundVolumeM3, 
				belowGroundBiomassMg, 
				belowGroundCarbonMg,
				commercialBiomassMg);
		
		plot.addTree(tree);
		if (!speciesList.contains(originalSpeciesName)) {
			speciesList.add(originalSpeciesName);
		}

	}

	/**
	 * Create a Tree instance. 
	 * @param plot a CATGrowthSimulationPlot instance
	 * @param statusClass a StatusClass enum
	 * @param treeOverbarkVolumeM3 the overbark volume (m3)
	 * @param numberOfTrees the expansion factor 
	 * @param originalSpeciesName the original species name
	 * @param dbhCm DBH (cm)
	 * @param aboveGroundVolumeM3 the aboveground volume (m3), can be null which means it is not provided
	 * @param aboveGroundBiomassMg the aboveground biomass (Mg), can be null which means it is not provided
	 * @param aboveGroundCarbonMg the aboveground carbon (Mg), can be null which means it is not provided
	 * @param belowGroundVolumeM3 the belowground volume (m3), can be null which means it is not provided
	 * @param belowGroundBiomassMg the belowground biomass (Mg), can be null which means it is not provided
	 * @param belowGroundCarbonMg the belowground carbon (Mg), can be null which means it is not provided
	 * @return a CATGrowthSimulationTree instance
	 */
	protected CATGrowthSimulationTree createTree(CATGrowthSimulationPlot plot, 
			StatusClass statusClass, 
			double treeOverbarkVolumeM3, 
			double numberOfTrees, 
			String originalSpeciesName,
			Double dbhCm,
			Double aboveGroundVolumeM3,
			Double aboveGroundBiomassMg,
			Double aboveGroundCarbonMg,
			Double belowGroundVolumeM3,
			Double belowGroundBiomassMg,
			Double belowGroundCarbonMg,
			Double commercialCarbonMg) {
		return dbhCm == null ?
				new CATGrowthSimulationTree(plot, statusClass, treeOverbarkVolumeM3, numberOfTrees, originalSpeciesName,
						aboveGroundVolumeM3, aboveGroundBiomassMg, aboveGroundCarbonMg, belowGroundVolumeM3, belowGroundBiomassMg, belowGroundCarbonMg, commercialCarbonMg) :
					new CATGrowthSimulationTreeWithDBH(plot, statusClass, treeOverbarkVolumeM3, numberOfTrees, originalSpeciesName, dbhCm, 
							aboveGroundVolumeM3, aboveGroundBiomassMg, aboveGroundCarbonMg, belowGroundVolumeM3, belowGroundBiomassMg, belowGroundCarbonMg, commercialCarbonMg);
	}
	
	@Override
	public void readRecordsForThisGroupId(int groupId) throws Exception {
		standMap.clear();
		super.readRecordsForThisGroupId(groupId);
	}

	@Override 
	public void readAllRecords() throws Exception {
		super.readAllRecords();
		ensureValidityInCasesOfEmptyPlots();
	}
	
	
	private void ensureValidityInCasesOfEmptyPlots() throws Exception {
		Map<String, Double> plotIDAndAreaHa = new HashMap<String, Double>();
		List<Integer> refList = null;
		for (Map<Boolean, CATGrowthSimulationCompositeStand> oMap : standMap.values()) {
			for (CATGrowthSimulationCompositeStand stand : oMap.values()) {
				List<Integer> monteCarloIds = stand.getRealizationIds();
				if (refList == null) {
					refList = monteCarloIds;
				} else if (!refList.equals(monteCarloIds)) {
					throw new Exception(MessageID.InconsistentGrowthSimulation.toString());
				}
				for (Integer i : monteCarloIds) {
					CATGrowthSimulationPlotSample plotSample = stand.getRealization(i);
					for (String pId : plotSample.getPlotMap().keySet()) {
						if (!plotIDAndAreaHa.containsKey(pId)) {
							plotIDAndAreaHa.put(pId, plotSample.getPlot(pId).getAreaHa());
						}
					}
				}
			}
		}
		
		for (Map<Boolean, CATGrowthSimulationCompositeStand> oMap : standMap.values()) {
			for (CATGrowthSimulationCompositeStand stand : oMap.values()) {
				for (Integer i : refList) {
					CATGrowthSimulationPlotSample sample = stand.getRealization(i);
					for (String pId : plotIDAndAreaHa.keySet()) {
						if (!sample.getPlotMap().containsKey(pId)) {
							sample.createPlot(pId, plotIDAndAreaHa.get(pId), stand.isInterventionResult()); 
						}
					}
				}
			}
		}
	}

	/**
	 * This method returns the stand list that was last read.
	 * @return a list of CATCompatibleStand instances
	 */
	public List<CATCompatibleStand> getStandList() {
		List<CATCompatibleStand> standList = new ArrayList<CATCompatibleStand>();
		for (Map<Boolean, CATGrowthSimulationCompositeStand> oMap : standMap.values()) {
			standList.add(oMap.get(false));
			if (oMap.containsKey(true)) {
				standList.add(oMap.get(true));
			}
		}
		
//		if (TestUnevenAgedInfiniteSequence) {
//			for (CATCompatibleStand s : standList) {
//				((CATGrowthSimulationCompositeStand) s).setApplicationScale(ApplicationScale.Stand);
//				((CATGrowthSimulationCompositeStand) s).setManagementType(ManagementType.UnevenAged);
//			}
//		}
		
		return standList;
	}
	
	protected List<String> getSpeciesList() {return speciesList;}
}
