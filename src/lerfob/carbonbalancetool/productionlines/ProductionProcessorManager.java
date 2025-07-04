/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2014 Mathieu Fortin for LERFOB AgroParisTech/INRA, 
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
package lerfob.carbonbalancetool.productionlines;

import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import lerfob.carbonbalancetool.CATCompartmentManager;
import lerfob.carbonbalancetool.CATCompatibleTree;
import lerfob.carbonbalancetool.CarbonAccountingTool;
import lerfob.carbonbalancetool.catdiameterbasedtreelogger.CATDiameterBasedTreeLogger;
import lerfob.carbonbalancetool.interfaces.CATDeadWoodProvider;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManagerDialog.MessageID;
import lerfob.carbonbalancetool.productionlines.WoodyDebrisProcessor.WoodyDebrisProcessorID;
import lerfob.carbonbalancetool.productionlines.affiliere.AffiliereImportReader;
import lerfob.treelogger.basictreelogger.BasicTreeLogger;
import lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLogger;
import lerfob.treelogger.maritimepine.MaritimePineBasicTreeLogger;
import repicea.gui.UIControlManager;
import repicea.gui.permissions.DefaultREpiceaGUIPermission;
import repicea.io.REpiceaFileFilter;
import repicea.io.REpiceaFileFilterList;
import repicea.serial.Memorizable;
import repicea.serial.MemorizerPackage;
import repicea.serial.SerializerChangeMonitor;
import repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider.SpeciesType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.Processor;
import repicea.simulation.processsystem.SystemManager;
import repicea.simulation.processsystem.SystemManagerDialog;
import repicea.simulation.processsystem.TestProcessUnit;
import repicea.simulation.treelogger.LogCategory;
import repicea.simulation.treelogger.TreeLogger;
import repicea.simulation.treelogger.TreeLoggerDescription;
import repicea.simulation.treelogger.TreeLoggerParameters;
import repicea.util.ExtendedFileFilter;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaLogManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.Language;

/**
 * The ProductionProcessorManager class is an implementation of SystemManager
 * for production line design in a context of carbon balance assessment.
 * 
 * @author Mathieu Fortin - May 2014
 */
public class ProductionProcessorManager extends SystemManager implements Memorizable {

	@SuppressWarnings("serial")
	protected static class TreeLoggerInstanceCompatibilityException extends InvalidParameterException {
		protected TreeLoggerInstanceCompatibilityException() {
		}
	}
	
	static {
		SerializerChangeMonitor.registerClassNameChange(
				"lerfob.carbonbalancetool.productionlines.DebarkingProcessor",
				"lerfob.carbonbalancetool.productionlines.BarkExtractionProcessor");
		SerializerChangeMonitor.registerClassNameChange(
				"lerfob.carbonbalancetool.defaulttreelogger.CATDefaultLogCategory",
				"repicea.treelogger.basictreelogger.BasicLogCategory");
		SerializerChangeMonitor.registerClassNameChange(
				"lerfob.carbonbalancetool.defaulttreelogger.CATDefaultTreeLogger",
				"repicea.treelogger.basictreelogger.BasicTreeLogger");
		SerializerChangeMonitor.registerClassNameChange(
				"lerfob.carbonbalancetool.defaulttreelogger.CATDefaultTreeLoggerParameters",
				"repicea.treelogger.basictreelogger.BasicTreeLoggerParameters");
		SerializerChangeMonitor.registerClassNameChange(
				"lerfob.carbonbalancetool.defaulttreelogger.CATDefaultTreeLoggerParametersDialog",
				"repicea.treelogger.basictreelogger.BasicTreeLoggerParametersDialog");
		SerializerChangeMonitor.registerClassNameChange("lerfob.carbonbalancetool.defaulttreelogger.CATWoodPiece",
				"repicea.treelogger.basictreelogger.BasicTreeLoggerWoodPiece");

		SerializerChangeMonitor.registerClassNameChange("repicea.treelogger.basictreelogger.BasicTreeLogger",
				"lerfob.treelogger.basictreelogger.BasicTreeLogger");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.basictreelogger.BasicTreeLoggerParameters",
				"lerfob.treelogger.basictreelogger.BasicTreeLoggerParameters");
		SerializerChangeMonitor.registerClassNameChange("repicea.treelogger.basictreelogger.BasicLogCategory",
				"lerfob.treelogger.basictreelogger.BasicLogCategory");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.europeanbeech.EuropeanBeechBasicTreeLogger",
				"lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLogger");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.europeanbeech.EuropeanBeechBasicTreeLoggerParameters",
				"lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLoggerParameters");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.europeanbeech.EuropeanBeechBasicTreeLogCategory",
				"lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLogCategory");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.europeanbeech.EuropeanBeechBasicTreeLoggerParameters$Grade",
				"lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLoggerParameters$Grade");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.maritimepine.MaritimePineBasicTreeLogger",
				"lerfob.treelogger.maritimepine.MaritimePineBasicTreeLogger");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.maritimepine.MaritimePineBasicTreeLoggerParameters",
				"lerfob.treelogger.maritimepine.MaritimePineBasicTreeLoggerParameters");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.maritimepine.MaritimePineBasicTreeLogCategory",
				"lerfob.treelogger.maritimepine.MaritimePineBasicTreeLogCategory");
		SerializerChangeMonitor.registerClassNameChange(
				"repicea.treelogger.maritimepine.MaritimePineBasicTreeLoggerParameters$Grade",
				"lerfob.treelogger.maritimepine.MaritimePineBasicTreeLoggerParameters$Grade");
	}

	protected static Logger getLogger() {
		return REpiceaLogManager.getLogger(ProductionProcessorManager.class.getSimpleName());
	}
 	
	/**
	 * This class is the file filter for loading and saving production lines.
	 * 
	 * @author Mathieu Fortin - May 2014
	 */
	public static class ProductionLineFileFilter extends FileFilter implements ExtendedFileFilter {

		private String extension = ".prl";

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
			return ProductionProcessorManagerDialog.MessageID.ProductionLineFileExtension.toString();
		}

		@Override
		public String getExtension() {
			return extension;
		}
	}

	static class CarbonTestProcessUnit extends TestProcessUnit {

		CarbonTestProcessUnit() {
			super();
		}

		/*
		 * For extended visibility in this package.
		 */
		@Override
		protected boolean recordProcessor(Processor processor) {
			return super.recordProcessor(processor);
		}

		@Override
		protected TestProcessUnit createNewProcessUnitFromThisOne() {
			CarbonTestProcessUnit ctpu = new CarbonTestProcessUnit();
			ctpu.processorList.addAll(this.processorList);
			return ctpu;
		}

	}

	static class BarkTestProcessUnit extends CarbonTestProcessUnit implements BiomassTypeProvider {

		BarkTestProcessUnit() {
			super();
		}

		@Override
		public BiomassType getBiomassType() {
			return BiomassType.Bark;
		}

		@Override
		protected TestProcessUnit createNewProcessUnitFromThisOne() {
			BarkTestProcessUnit ctpu = new BarkTestProcessUnit();
			ctpu.processorList.addAll(this.processorList);
			return ctpu;
		}

	}



	protected static List<LeftHandSideProcessor> DefaultLeftHandSideProcessors;

	public static final ProductionLineFileFilter ProductionProcessorManagerFileFilter = new ProductionLineFileFilter();

	/**
	 * The VERY_SMALL value serves as threshold when dealing with small quantities.
	 * Below the threshold the quantity is not considered at all.
	 */
	public static final double VERY_SMALL = 1E-12;
//	public static final double VERY_SMALL = 1E-200;

	protected static enum EnhancedMode {
		CreateEndOfLifeLinkLine
	}
	
	public static enum ImportFormat {
		AFFILIERE;
	}

//	public static enum ExportFormat {
//		AFFILIERE;
//	}

	private transient final Vector<TreeLoggerParameters<?>> availableTreeLoggerParameters;

	@SuppressWarnings("rawtypes")
	private TreeLoggerParameters selectedTreeLoggerParameters;

	@SuppressWarnings("rawtypes")
	private transient TreeLogger treeLogger;

	protected final ArrayList<LeftHandSideProcessor> logCategoryProcessors;

	private transient Map<LogCategory, LogCategoryProcessor> logCategoryProcessorIndices = new HashMap<LogCategory, LogCategoryProcessor>();

	private transient CarbonUnitMap<CarbonUnitStatus> carbonUnitMap;

	private transient LeftInForestProcessor deadWoodProcessor;
	
	/**
	 * Constructor.
	 * @param defaultPermission a DefaultREpiceaGUIPermission instance
	 */
	public ProductionProcessorManager(DefaultREpiceaGUIPermission defaultPermission) {
		super(defaultPermission);
		logCategoryProcessors = new ArrayList<LeftHandSideProcessor>();
		availableTreeLoggerParameters = new Vector<TreeLoggerParameters<?>>();

		Vector<TreeLoggerDescription> defaultTreeLoggerDescriptions = new Vector<TreeLoggerDescription>();
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(BasicTreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(CATDiameterBasedTreeLogger.class));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(MaritimePineBasicTreeLogger.class.getName()));
		defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(EuropeanBeechBasicTreeLogger.class.getName()));
		setAvailableTreeLoggers(defaultTreeLoggerDescriptions);
	}

	/**
	 * Constructor with all permissions allowed.
	 */
	public ProductionProcessorManager() {
		this(new DefaultREpiceaGUIPermission(true));
	}

	/**
	 * Import a flux configuration from a particular file under a given format.
	 * @param filename the name of the file
	 * @param iFormat an ImportFormat enum that defines the expected format
	 * @throws FileNotFoundException if the file can be found
	 */
	public void importFrom(String filename, ImportFormat iFormat) throws IOException {
		if (iFormat == null || filename == null) {
			throw new InvalidParameterException("The filename and iFormat arguments must be non null!");
		}
		switch(iFormat) {
		case AFFILIERE:
			AffiliereImportReader reader = new AffiliereImportReader(new File(filename), 
//					AFFiliereStudy.BACCFIRE, 
//					AFFiliereUnit.DryBiomassMg,
					isVisible() ? getUI(null) : null);
			if (reader.isCancelled()) {
				return;
			}
			reset();
			for (Processor p : reader.getProcessors().values()) {
				registerObject(p);
			}
			break;
		default:
			throw new InvalidParameterException("The import format " + iFormat.name() + " is not implemented yet!");
		}
	}
	
//	/**
//	 * Export a flux configuration to a particular file under a given format.
//	 * @param filename the name of the file
//	 * @param eFormat an ExportFormat enum that defines the expected format
//	 * @throws IOException if an IO error occurs
//	 */
//	public void exportTo(String filename, ExportFormat eFormat) throws IOException {
//		if (eFormat == null || filename == null) {
//			throw new InvalidParameterException("The filename and eFormat arguments must be non null!");
//		}
//		switch(eFormat) {
//		case AFFILIERE:
//			new AffiliereExportWriter(getAffiliereJSONFormatRepresentation(), filename);
//			break;
//		default:
//			throw new InvalidParameterException("The export format " + eFormat.name() + " is not implemented yet!");
//		}
//	}

//	private LinkedHashMap<String, Object> getAffiliereJSONFormatRepresentation() {
//		int idDispenser = 1;
//		Map<String, LinkedHashMap<String, Object>> nodeMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
//		Map<Processor, String> processorToIdMap = new HashMap<Processor, String>();
//		for (Processor p : getList()) {
//			String idNode = "node" + idDispenser++;
//			LinkedHashMap<String, Object> nodeRep = ((AbstractProcessor) p).getAffiliereJSONFormatNodeRepresentation(idNode);
//			nodeMap.put(idNode, nodeRep);
//			processorToIdMap.put(p, idNode);
//		}
//		Map<String, Object> linkMap = new LinkedHashMap<String, Object>();
//		for (Processor source : getList()) {
//			for (Processor target : source.getSubProcessors()) {
//				String idLink = "link" + idDispenser++;
//				linkMap.put(idLink, getAffiliereJSONFormatLinkRepresentation(idLink, false, source, target, processorToIdMap)); // false: a typical production processor (not end of life)
//			}
//			if (source instanceof ProductionLineProcessor) {
//				if (((ProductionLineProcessor) source).getDisposedToProcess() != null) {
//					String idLink = "link" + idDispenser++;
//					linkMap.put(idLink, getAffiliereJSONFormatLinkRepresentation(idLink, true, source,
//							((ProductionLineProcessor) source).getDisposedToProcess(), processorToIdMap)); // end of life
//																										// processor
//				}
//			}
//		}
//		LinkedHashMap<String, Object> outputMap = new LinkedHashMap<String, Object>();
//		outputMap.put(AffiliereJSONFormat.L1_VERSION_PROPERTY, "0.8");
//		outputMap.put(AffiliereJSONFormat.L1_NODES_PROPERTY, nodeMap);
//		outputMap.put(AffiliereJSONFormat.L1_LINKS_PROPERTY, linkMap);
//		return outputMap;
//	}

//	private static LinkedHashMap<String, Object> getAffiliereJSONFormatLinkRepresentation(String idLink, 
//			boolean endOfLife, 
//			Processor source, 
//			Processor target,
//			Map<Processor, String> processorToIdMap) {
//		LinkedHashMap<String, Object> oMap = new LinkedHashMap<String, Object>();
//		oMap.put(AffiliereJSONFormat.LINK_IDLINK_PROPERTY, idLink);
//		oMap.put(AffiliereJSONFormat.LINK_IDSOURCE_PROPERTY, processorToIdMap.get(source));
//		oMap.put(AffiliereJSONFormat.LINK_IDTARGET_PROPERTY, processorToIdMap.get(target));
//		oMap.put(AffiliereJSONFormat.LINK_LINKTYPE_PROPERTY, endOfLife ? "EndOfLife" : "Production");
//
//		LinkedHashMap<String, Object> value = new LinkedHashMap<String, Object>();
//		oMap.put(AffiliereJSONFormat.LINK_VALUE_PROPERTY, value);
//		value.put(AffiliereJSONFormat.LINK_VALUE_ISPERCENT_PROPERTY, true);
//		if (endOfLife) {
//			value.put(AffiliereJSONFormat.LINK_VALUE_PERCENT_PROPERTY, 100);
//		} else {
//			value.put(AffiliereJSONFormat.LINK_VALUE_PERCENT_PROPERTY, source.getSubProcessorIntakes().get(target).doubleValue());
//		}
//		
//		oMap.put(AffiliereJSONFormat.LINK_STYLE_PROPERTY, "default");
//		return oMap;
//	}

	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public TreeLogger getSelectedTreeLogger() {
		TreeLoggerParameters<?> parms = getSelectedTreeLoggerParameters();
		if (treeLogger == null
				|| !parms.getTreeLoggerDescription().getTreeLoggerClass().equals(treeLogger.getClass())) {
			treeLogger = parms.createTreeLoggerInstance();
		} else if (!treeLogger.getTreeLoggerParameters().equals(parms)) {
			treeLogger.setTreeLoggerParameters(parms);
		}
		return treeLogger;
	}

	public void resetCarbonUnitMap() {
		logCategoryProcessorIndices.clear();
		getCarbonUnitMap().clear();
	}

	protected TreeLoggerParameters<?> getSelectedTreeLoggerParameters() {
		return selectedTreeLoggerParameters;
	}

	protected void setSelectedTreeLogger(TreeLoggerParameters<?> treeLoggerParameters) {
		if (!isCompatibleWithAvailableTreeLoggerParameters(treeLoggerParameters)) {
			throw new TreeLoggerInstanceCompatibilityException();
		} else {
			selectedTreeLoggerParameters = treeLoggerParameters;
			actualizeTreeLoggerParameters();
		}
	}

	private boolean isCompatibleWithAvailableTreeLoggerParameters(TreeLoggerParameters<?> treeLoggerParameters) {
		boolean matchFound = false;
		int index = -1;
		for (index = 0; index < availableTreeLoggerParameters.size(); index++) {
			if (availableTreeLoggerParameters.get(index).getClass().equals(treeLoggerParameters.getClass())) {
				matchFound = true;
				break;
			}
		}
		if (matchFound) {
			availableTreeLoggerParameters.remove(index);
			availableTreeLoggerParameters.add(index, treeLoggerParameters);
			treeLoggerParameters.setReadWritePermissionGranted(getGUIPermission());
		}
		return matchFound;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void actualizeTreeLoggerParameters() {
		List<LeftHandSideProcessor> formerProcessorList = new ArrayList<LeftHandSideProcessor>();
		formerProcessorList.addAll(logCategoryProcessors);
		List<LeftHandSideProcessor> newProcessorList = new ArrayList<LeftHandSideProcessor>();
		newProcessorList.addAll(getDefaultLeftHandSideProcessors());
		for (Object species : selectedTreeLoggerParameters.getLogCategories().keySet()) {
			List<LogCategory> innerList = (List) selectedTreeLoggerParameters.getLogCategories().get(species);
			for (LogCategory logCategory : innerList) {
				newProcessorList.add(new LogCategoryProcessor(logCategory));
			}
		}
		formerProcessorList.removeAll(newProcessorList);
		for (Processor processor : formerProcessorList) {
			logCategoryProcessors.remove(processor);
			removeObject(processor);
		}
		newProcessorList.removeAll(logCategoryProcessors);
		for (LeftHandSideProcessor processor : newProcessorList) {
			logCategoryProcessors.add(processor);
			registerObject(processor);
		}
	}

	private Collection<? extends LeftHandSideProcessor> getDefaultLeftHandSideProcessors() {
		if (DefaultLeftHandSideProcessors == null) {
			DefaultLeftHandSideProcessors = new ArrayList<LeftHandSideProcessor>();
			DefaultLeftHandSideProcessors.add(new WoodyDebrisProcessor(WoodyDebrisProcessorID.FineWoodyDebris));
			DefaultLeftHandSideProcessors.add(new WoodyDebrisProcessor(WoodyDebrisProcessorID.CommercialWoodyDebris));
			DefaultLeftHandSideProcessors.add(new WoodyDebrisProcessor(WoodyDebrisProcessorID.CoarseWoodyDebris));
		}
		return DefaultLeftHandSideProcessors;
	}

	protected Vector<TreeLoggerParameters<?>> getAvailableTreeLoggerParameters() {
		return availableTreeLoggerParameters;
	}

	@Override
	public REpiceaFileFilterList getFileFilters() {
		return new REpiceaFileFilterList(ProductionProcessorManagerFileFilter, REpiceaFileFilter.JSON);
	}

	@Override
	public SystemManagerDialog getUI(Container parent) {
		if (guiInterface == null) {
			guiInterface = new ProductionProcessorManagerDialog((Window) parent, this);
		}
		return guiInterface;
	}

	@Override
	public MemorizerPackage getMemorizerPackage() {
		MemorizerPackage mp = super.getMemorizerPackage();
		mp.add(logCategoryProcessors);
		mp.add(selectedTreeLoggerParameters);
		return mp;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public void unpackMemorizerPackage(MemorizerPackage wasMemorized) {
		super.unpackMemorizerPackage(wasMemorized);
		ArrayList<LogCategoryProcessor> lcp = (ArrayList) wasMemorized.remove(0);
		logCategoryProcessors.clear();
		logCategoryProcessors.addAll(lcp);
		TreeLoggerParameters tlp = (TreeLoggerParameters) wasMemorized.remove(0);
		setSelectedTreeLogger(tlp);
	}

	@Override
	public void load(String filename) throws IOException {
		try {
			super.load(filename);
		} catch (TreeLoggerInstanceCompatibilityException e) {
			handleTreeLoggerChange(true);	// enable warning because the tree logger instance is not available for this simulation
		}
	}


	private void handleTreeLoggerChange(boolean enableWarning) {
		if (enableWarning) {
			if (guiInterface != null) {
				JOptionPane.showMessageDialog(getUI(null), MessageID.IncompatibleTreeLogger.toString(),
						UIControlManager.InformationMessageTitle.Warning.toString(), JOptionPane.WARNING_MESSAGE);
			} else {
				REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.WARNING, getClass().getSimpleName(),
						MessageID.IncompatibleTreeLogger.toString());
			}
		}
		setSelectedTreeLogger(availableTreeLoggerParameters.get(0));
		if (guiInterface != null) {
			guiInterface.synchronizeUIWithOwner();
		}
	}

	/**
	 * This method sets the list of tree logger descriptions.
	 * 
	 * @param vector a Vector instance containing TreeLoggerdescription objects
	 */
	public void setAvailableTreeLoggers(Vector<TreeLoggerDescription> vector) {
		availableTreeLoggerParameters.clear();
		if (vector == null) {
			vector = new Vector<TreeLoggerDescription>();
		}
		if (vector.isEmpty()) {
			vector.add(new TreeLoggerDescription(BasicTreeLogger.class.getName()));
		}
		for (TreeLoggerDescription description : vector) {
			TreeLoggerParameters<?> treeLoggerParameters = description.instantiateTreeLogger(true)
					.createDefaultTreeLoggerParameters();
			treeLoggerParameters.setReadWritePermissionGranted(getGUIPermission());
			availableTreeLoggerParameters.add(treeLoggerParameters);
		}

		int index = findSelectedTreeLoggerAmongAvailableTreeLoggers();
		if (index > -1) {
			availableTreeLoggerParameters.setElementAt(selectedTreeLoggerParameters, index);
			if (guiInterface != null) { // 20180926 BUG CORRECTION the combobox model was not updated if it already
										// contained the selected tree logger
				guiInterface.synchronizeUIWithOwner();
			}
		} else {
//			if (guiInterface != null) {
//				guiInterface.setVisible(true);
//			}
//			Runnable doRun2 = new Runnable() {
//				@Override
//				public void run() {
			handleTreeLoggerChange(false);		// false: no need to issue a warning here
//				}
//			};
//			SwingUtilities.invokeLater(doRun2);
		}
	}

	private int findSelectedTreeLoggerAmongAvailableTreeLoggers() {
		if (selectedTreeLoggerParameters != null) {
			for (int i = 0; i < availableTreeLoggerParameters.size(); i++) {
				if (availableTreeLoggerParameters.get(i).getClass().equals(selectedTreeLoggerParameters.getClass())) {
					return i;
				}
			}
		}
		return -1;
	}

	@SuppressWarnings("rawtypes")
	protected void addTestUnits(List<ProcessUnit> inputUnits) {
		inputUnits.add(new CarbonTestProcessUnit());
		inputUnits.add(new BarkTestProcessUnit());
	}

	/**
	 * Validate the ProductionProcessorManager instance. <p>
	 * To be valid, all the LogCategoryProcessor must have at least one sub processor. 
	 * Moreover, all the processors must be valid, i.e. the
	 * sum of the fluxes to the sub processors must be equal to 100%.
	 * @throws ProductionProcessorManagerException if the ProductionProcessorManager instance cannot be validated
	 */
	public void validate() throws ProductionProcessorManagerException {
		for (Processor logCategoryProcessor : logCategoryProcessors) {
			if (!logCategoryProcessor.hasSubProcessors()) {
				throw new ProductionProcessorManagerException(
						"This processor should be linked to sub processors: " + logCategoryProcessor.getName());
			}
		}
		for (Processor processor : getList()) {
			if (!processor.isValid()) {
				throw new ProductionProcessorManagerException("This processor is invalid: " + processor.getName());
			}
			if (processor.isPartOfEndlessLoop()) {
				throw new ProductionProcessorManagerException(
						"This processor is part of an endless loop: " + processor.getName());
			}
		}
	}

	/**
	 * Process the wood pieces into wood products.<p>
	 * 
	 * The different kinds of produced carbon units are stored in the carbon unit map, 
	 * which is accessible through the getCarbonUnits(CarbonUnitType) method.
	 * 
	 * @param logCategory a TreeLogCategory instance
	 * @param dateIndex   the index of the date in the time scale
	 * @param samplingUnitID a String that stands for the sampling unit, typically the plot
	 * @param amountMaps a Map which contains the amounts of the different elements
	 * @param tree a CATCompatibleTree instance
	 */
	public void processWoodPiece(LogCategory logCategory, 
			int dateIndex, 
			String samplingUnitID,
			Map<BiomassType, AmountMap<Element>> amountMaps, 
			CATCompatibleTree tree) {
		Processor processor = findLeftHandSideProcessor(logCategory);
		processAmountMap(processor, dateIndex, samplingUnitID, amountMaps, tree.getSpeciesName(), tree.getSpeciesType(), tree.getStatusClass(), null); // woodyDebrisType is set to null
	}

	/**
	 * Create dead wood for the first stand if it is available
	 * @param deadWoodPovider a CATDeadWoodProvide instance
	 * @param dateIndex the date index
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void createDeadWood(CATDeadWoodProvider deadWoodPovider, int dateIndex) {
		double averageLifeTimeYr = 0d;
		if (deadWoodProcessor == null) {
			for (Processor p : getList()) {
				if (p instanceof LeftInForestProcessor) {
					LeftInForestProcessor lp = (LeftInForestProcessor) p;
					if (lp.getEndProductFeature().getDecayFunction().averageLifetimeYr > averageLifeTimeYr) {
						averageLifeTimeYr = lp.getEndProductFeature().getDecayFunction().averageLifetimeYr;
						deadWoodProcessor = lp;
					}
				}
			}
			if (deadWoodProcessor == null) {
				throw new UnsupportedOperationException("The dead wood processor cannot be found in the flux configuration!");
			}
		}

		
		AmountMap<Element> woodAmountMap = new AmountMap<Element>();
		Map<String, Double> biomasses = deadWoodPovider.getDeadWoodBiomassMgForTheseSamplingUnits();
		for (String samplingUnitID : biomasses.keySet()) {
			double biomassMg = biomasses.get(samplingUnitID);
			woodAmountMap.put(Element.Volume, biomassMg / 0.45); // 0.45 an arbitrary factor to get some volume
			woodAmountMap.put(Element.Biomass, biomassMg);
			woodAmountMap.put(Element.C, biomassMg * 0.5); // 0.5 we use the average here because we do not know the species
			CarbonUnit cu = new CarbonUnit(dateIndex, 
					samplingUnitID, 
					null, // no CarbonUnitFeature instance at this point
					woodAmountMap,
					"Unknown", // unknown species
					null, // unknown species type
					StatusClass.dead,
					BiomassType.Wood,
					WoodyDebrisProcessorID.CoarseWoodyDebris);
			Collection<CarbonUnit> units = (Collection) deadWoodProcessor.createProcessUnitsFromThisProcessor(cu, 100); 
			getCarbonUnitMap().add(units);
		}
	}

	
	/**
	 * Process the carbon unit as woody debris.
	 * 
	 * @param dateIndex the index of the date in the time scale
	 * @param samplingUnitID a String that stands for the sampling unit, typically the plot
	 * @param amountMaps a Map which contains the amounts of the different elements
	 * @param tree a CATCompatibleTree instance
	 * @param woodyDebrisType a WoodyDebrisProcessorID enum variable
	 */
	public void processWoodyDebris(int dateIndex, 
			String samplingUnitID,
			Map<BiomassType, AmountMap<Element>> amountMaps, 
			CATCompatibleTree tree,
			WoodyDebrisProcessorID woodyDebrisType) {
		if (woodyDebrisType == null) {
			throw new InvalidParameterException("The woodyDebrisType argument cannot be null!");
		}
		Processor processor = findWoodyDebrisProcessor(woodyDebrisType);
		processAmountMap(processor, dateIndex, samplingUnitID, amountMaps, tree.getSpeciesName(), tree.getSpeciesType(), tree.getStatusClass(), woodyDebrisType);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<CarbonUnit> processAmountMap(Processor processor, 
			int dateIndex, 
			String samplingUnitID,
			Map<BiomassType, AmountMap<Element>> amountMaps, 
			String speciesName, 
			SpeciesType speciesType,
			StatusClass statusClass,
			WoodyDebrisProcessorID woodyDebrisType) {
		List<ProcessUnit> inputUnits = new ArrayList<ProcessUnit>();
		if (!amountMaps.isEmpty()) {
			for (BiomassType bt : amountMaps.keySet()) {
				inputUnits.add(new CarbonUnit(dateIndex, samplingUnitID, null, amountMaps.get(bt), speciesName, speciesType, statusClass, bt, woodyDebrisType));
			}
			Collection<CarbonUnit> processedUnits = (Collection) processor.doProcess(inputUnits);
			getCarbonUnitMap().add(processedUnits);
			return processedUnits;
		} else {
			return new ArrayList<CarbonUnit>();
		}
	}

	/**
	 * This method actualizes the different carbon units. It proceeds in the
	 * following order : 
	 * <ol>
	 * <li> the carbon units in the wood products
	 * <li> the carbon units recycled from the disposed wood products
	 * <li> the carbon units left in the forest
	 * <li> the carbon units at the landfill site
	 * </ol>
	 * @param compartmentManager the CATCompartmentManager instance
	 */
	public void actualizeCarbonUnits(CATCompartmentManager compartmentManager) {
		actualizeCarbonUnitsOfThisType(CarbonUnitStatus.EndUseWoodProduct, compartmentManager);
		actualizeCarbonUnitsOfThisType(CarbonUnitStatus.Recycled, compartmentManager);
		actualizeCarbonUnitsOfThisType(CarbonUnitStatus.DeadWood, compartmentManager);
		actualizeCarbonUnitsOfThisType(CarbonUnitStatus.LandFillDegradable, compartmentManager);
	}

	private void actualizeCarbonUnitsOfThisType(CarbonUnitStatus type, CATCompartmentManager compartmentManager) {
		CarbonUnitList list = getCarbonUnits(type);
		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, getClass().getSimpleName(),
				"Carbon units of type " + type.name() + ". Before actualization, " + list.toString());
		for (int i = 0; i < list.size(); i++) { // the condition based on the size of the list makes sure that newly
												// created HWPs will be actualized.
			CarbonUnit carbonUnit = list.get(i);
			carbonUnit.actualizeCarbon(compartmentManager);
		}

		REpiceaLogManager.logMessage(CarbonAccountingTool.LOGGER_NAME, Level.FINEST, getClass().getSimpleName(),
				"Carbon units of type " + type.name() + " actualized. After actualization, " + list.toString());
	}

	protected CarbonUnitMap<CarbonUnitStatus> getCarbonUnitMap() {
		if (carbonUnitMap == null) {
			carbonUnitMap = new CarbonUnitMap<CarbonUnitStatus>(CarbonUnitStatus.EndUseWoodProduct);
		}
		return carbonUnitMap;
	}

	private WoodyDebrisProcessor findWoodyDebrisProcessor(WoodyDebrisProcessorID processorID) {
		for (LeftHandSideProcessor processor : logCategoryProcessors) {
			if (processor instanceof WoodyDebrisProcessor) {
				if (((WoodyDebrisProcessor) processor).wdpID == processorID) {
					return ((WoodyDebrisProcessor) processor);
				}
			}
		}
		if (processorID == WoodyDebrisProcessorID.CommercialWoodyDebris) {
			return findWoodyDebrisProcessor(WoodyDebrisProcessorID.CoarseWoodyDebris); // in case commercial does not
																						// exist
		}
		return null;
	}

	private LeftHandSideProcessor findLeftHandSideProcessor(LogCategory logCategory) {
		if (!logCategoryProcessorIndices.containsKey(logCategory)) {
			for (LeftHandSideProcessor processor : logCategoryProcessors) {
				if (processor instanceof LogCategoryProcessor) {
					if (((LogCategoryProcessor) processor).logCategory.equals(logCategory)) {
						logCategoryProcessorIndices.put(logCategory, (LogCategoryProcessor) processor);
						break;
					}
				}
			}
		}

		LeftHandSideProcessor outputProcessor = logCategoryProcessorIndices.get(logCategory);
		if (outputProcessor == null) {
			throw new InvalidParameterException(
					"The log category is not recognized by the ProductionProcessorManager instance");
		} else {
			return outputProcessor;
		}
	}

	
	
	/**
	 * This method returns the CarbonUnitList instance that match the type of
	 * carbon.
	 * 
	 * @param type a CarbonUnitType enum (EndUseWoodProduct, Landfill, Recycled,
	 *             LeftInForest)
	 * @return a CarbonUnitList instance
	 */
	public CarbonUnitList getCarbonUnits(CarbonUnitStatus type) {
		return getCarbonUnitMap().get(type);
	}

	@Override
	public void reset() {
		super.reset();
		logCategoryProcessors.clear();
		actualizeTreeLoggerParameters();
	}

	public static void main(String[] args) {
		REpiceaTranslator.setCurrentLanguage(Language.English);
		ConsoleHandler ch = new ConsoleHandler();
		ch.setLevel(Level.FINE);
		ProductionProcessorManager.getLogger().addHandler(ch);
		ProductionProcessorManager.getLogger().setLevel(Level.FINE);
//		REpiceaTranslator.setCurrentLanguage(Language.French);
//		ProductionProcessorManager ppm = new ProductionProcessorManager(new DefaultREpiceaGUIPermission(false));
		ProductionProcessorManager ppm = new ProductionProcessorManager();
		String filename = ObjectUtility.getPackagePath(ppm.getClass()) + File.separator + "library" + File.separator
				+ "hardwood_recycling_en.prl";
		try {
			ppm.load(filename);
			ppm.showUI(null);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
