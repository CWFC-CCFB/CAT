/*
 * This file is part of the lerfob-foresttools library.
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
package lerfob.carbonbalancetool;

import java.awt.Container;
import java.awt.Window;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.logging.Level;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import lerfob.carbonbalancetool.CATTask.SetProperRealizationTask;
import lerfob.carbonbalancetool.CATTask.Task;
import lerfob.carbonbalancetool.CATUtility.BiomassParametersName;
import lerfob.carbonbalancetool.CATUtility.ProductionManagerName;
import lerfob.carbonbalancetool.catdiameterbasedtreelogger.CATDiameterBasedTreeLogger;
import lerfob.carbonbalancetool.io.CATExportTool;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManagerException;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings;
import lerfob.carbonbalancetool.sensitivityanalysis.CATSensitivityAnalysisSettings.VariabilitySource;
import lerfob.treelogger.basictreelogger.BasicTreeLogger;
import lerfob.treelogger.douglasfirfcba.DouglasFCBATreeLogger;
import lerfob.treelogger.europeanbeech.EuropeanBeechBasicTreeLogger;
import lerfob.treelogger.maritimepine.MaritimePineBasicTreeLogger;
import lerfob.treelogger.mathilde.MathildeTreeLogger;
import repicea.app.AbstractGenericEngine;
import repicea.app.GenericTask;
import repicea.app.SettingMemory;
import repicea.gui.REpiceaShowableUI;
import repicea.gui.REpiceaShowableUIWithParent;
import repicea.gui.genericwindows.REpiceaLicenseWindow;
import repicea.gui.genericwindows.REpiceaSplashWindow;
import repicea.lang.REpiceaSystem;
import repicea.serial.SerializerChangeMonitor;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.treelogger.TreeLoggerCompatibilityCheck;
import repicea.simulation.treelogger.TreeLoggerDescription;
import repicea.simulation.treelogger.TreeLoggerManager;
import repicea.stats.Distribution;
import repicea.util.JarUtility;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaLogManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.Language;

/**
 * The CarbonAccountingTool class implements a tool for the calculation of the carbon balance in a series
 * of CATCompatibleStand instances. <p>
 *
 * The class has a GUI interface and it is in charge of calculating the carbon for all the compartments. It contains a CATCompartmentManager instance,
 * which contains in turn a collection of CATCarbonCompartment objects that can provide their carbon content on their own. 
 * 
 * @author Mathieu Fortin - January 2010
 */
public class CarbonAccountingTool extends AbstractGenericEngine implements REpiceaShowableUIWithParent, REpiceaShowableUI {

	static {
		SerializerChangeMonitor.registerClassNameChange("repicea.simulation.covariateproviders.treelevel.SpeciesNameProvider$SpeciesType", "repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider$SpeciesType");
		
		TreeLoggerManager.registerTreeLoggerName(BasicTreeLogger.class.getName());
		TreeLoggerManager.registerTreeLoggerName(CATDiameterBasedTreeLogger.class.getName());
		TreeLoggerManager.registerTreeLoggerName(EuropeanBeechBasicTreeLogger.class.getName());
		TreeLoggerManager.registerTreeLoggerName(MaritimePineBasicTreeLogger.class.getName());
		
		TreeLoggerManager.registerTreeLoggerName(MathildeTreeLogger.class.getName());
		TreeLoggerManager.registerTreeLoggerName(DouglasFCBATreeLogger.class.getName());
	}

	private static class StandComparator implements Comparator<CATCompatibleStand> {

		@Override
		public int compare(CATCompatibleStand arg0, CATCompatibleStand arg1) {
			if (arg0.getDateYr() < arg1.getDateYr()) {
				return -1;
			} else if (arg0.getDateYr() == arg1.getDateYr()) {
				if (arg0.isInterventionResult()) {
					return 1;
				} else if (arg1.isInterventionResult()) {
					return -1;
				} else {
					return 0;
				}
			} else {
				return 1;
			}
		}
	}
	
	public static String LOGGER_NAME = "CarbonAccountingTool";
	
	protected static final String englishTitle = "Carbon Accounting Tool (CAT)";
	protected static final String frenchTitle = "Outil de comptabilit\u00E9 carbone (CAT)";
	private static final StandComparator StandComparator = new StandComparator();
	protected static boolean hasAlreadyBeenInstanciated = false;
	
	public static enum CATMode {
		/**
		 * Running CAT as a stand alone application with user interface enabled.
		 */
		STANDALONE, 
		/**
		 * Running CAT from another application with user interface enabled.
		 */
		FROM_OTHER_APP, 
		/**
		 * Running CAT in script mode with used interface disabled and clearing of random deviates of the 
		 * sensitivity analysis.
		 */
		SCRIPT;
	}
	
	
	private CATCompartmentManager carbonCompartmentManager;	
	protected boolean finalCutHadToBeCarriedOut;
	
	protected Window parentFrame;
	protected List<CATCompatibleStand> waitingStandList;
	protected transient CATFrame guiInterface;
	protected transient Window owner;
	
	private String biomassParametersFilename;
	private String productionManagerFilename;

	private final CATMode mode;
	private boolean initialized;

	private boolean isShuttedDown;
	
	/**
	 * Constructor for stand alone application.
	 */
	public CarbonAccountingTool() {
		this(CATMode.STANDALONE);
	}
	

	/**
	 * Generic constructor.
	 * @param mode defines how CAT is to be used. See the CATMode enum variable.
	 */
	public CarbonAccountingTool(CATMode mode) {
		super(true); // we start the internal worker at once
		this.mode = mode;
		isShuttedDown = false;
		setSettingMemory(new SettingMemory(REpiceaSystem.getJavaIOTmpDir() + "settingsCarbonTool.ser"));
		
		finalCutHadToBeCarriedOut = false;
				
		Runnable toBeRun = new Runnable () {
			@Override
			public void run () {
				try {
					startApplication();
				} catch (Exception e) {
					throw new RuntimeException("The CarbonCalcultor engine has failed!");
				} 
			}
		};
		
		new Thread(toBeRun, "CarbonAccountingTool").start();
	}

	@Override
	protected void shutdown(int shutdownCode) {
		REpiceaLogManager.logMessage(LOGGER_NAME, Level.INFO, null, "Shutting down CAT...");
		CATSensitivityAnalysisSettings.getInstance().clear();
		isShuttedDown = true;
		if (mode == CATMode.STANDALONE) {		// only the stand alone mode will shutdown the JVM
			System.exit(shutdownCode);
		}
	}

	/**
	 * This method returns true if CAT has been shutted down or false otherwise.
	 * @return a boolean
	 */
	public boolean isShuttedDown() {return isShuttedDown;}
	
	/**
	 * This method returns the settings of the carbon accounting tool.
	 * @return a CarbonAccountingToolSettings instance
	 */
	public CATSettings getCarbonToolSettings() {
		return carbonCompartmentManager.getCarbonToolSettings();
	}
	
	/**
	 * Initialize CAT without parent window.
	 * @throws Exception if CAT has already been initialized
	 */
	public void initializeTool() throws Exception {
		initializeTool(null);
		Vector<TreeLoggerDescription> treeLoggerDescriptions = new Vector<TreeLoggerDescription>();
		treeLoggerDescriptions.add(new TreeLoggerDescription(BasicTreeLogger.class));
		treeLoggerDescriptions.add(new TreeLoggerDescription(CATDiameterBasedTreeLogger.class));
		
		treeLoggerDescriptions.add(new TreeLoggerDescription(MathildeTreeLogger.class));
		treeLoggerDescriptions.add(new TreeLoggerDescription(MaritimePineBasicTreeLogger.class));
		treeLoggerDescriptions.add(new TreeLoggerDescription(EuropeanBeechBasicTreeLogger.class));
		treeLoggerDescriptions.add(new TreeLoggerDescription(DouglasFCBATreeLogger.class));
		getCarbonToolSettings().setTreeLoggerDescriptions(treeLoggerDescriptions);
	}
	
	/**
	 * Initialize the carbon accounting tool either in script or in GUI mode.<p>
	 * This method can be called only once per instance. 
	 * @param parentFrame the parent frame which can be null
	 * @throws Exception if CAT has already been initialized
	 */
	public void initializeTool(Window parentFrame) throws Exception {
		if (initialized) {
			throw new Exception("The Carbon Accounting Tool is already initialized!");
		} else {
			this.parentFrame = parentFrame;
			CATSettings carbonToolSettings = new CATSettings(getSettingMemory());
			carbonCompartmentManager = new CATCompartmentManager(this, carbonToolSettings);
			
			if (mode == CATMode.STANDALONE) {
				String lastLookAndFeelClass = getCarbonToolSettings().getSettingMemory().getProperty("last.look.and.feel.class", UIManager.getSystemLookAndFeelClassName());
				try {
					UIManager.setLookAndFeel(lastLookAndFeelClass);
				} catch (Exception e) {
					REpiceaLogManager.logMessage(LOGGER_NAME, Level.SEVERE, null, "Unable to set the look and feel to " + lastLookAndFeelClass + " : " + e.getMessage());
				}
			}
			
			if (isGuiEnabled()) {
				if (!hasAlreadyBeenInstanciated) {
					String packagePath = ObjectUtility.getRelativePackagePath(CarbonAccountingTool.class);
					String iconPath =  packagePath + "SplashImage.jpg";
					String filePath = JarUtility.getJarFileImInIfAny(CarbonAccountingTool.class);
					String version;
					if (filePath != null) {
						try {
							Manifest m = JarUtility.getManifestFromThisJarFile(filePath);
							version = m.getMainAttributes().get(Attributes.Name.SPECIFICATION_VERSION).toString();				
						} catch (IOException e) {
							version = "Unknown";			
						}
					} else {
						version = "Unknown";			
					}
					
					String bottomSplashWindowString = "Version " + version;
					new REpiceaSplashWindow(iconPath, 4, parentFrame, 500, bottomSplashWindowString, 16);	// 500: image width; 16: font size

					String licensePath = packagePath + "CATLicense_en.html";
					if (REpiceaTranslator.getCurrentLanguage() == REpiceaTranslator.Language.French) {
						licensePath = packagePath + "CATLicense_fr.html";
					}

					REpiceaLicenseWindow licenseDlg;
					try {
						licenseDlg = new REpiceaLicenseWindow(parentFrame, licensePath);
						licenseDlg.setVisible(true);
						if (!licenseDlg.isLicenseAccepted()) {
							addTask(new CATTask(Task.SHUT_DOWN, this));
						} else {
							hasAlreadyBeenInstanciated = true;
						}
					} catch (IOException e) {
						e.printStackTrace();
						addTask(new CATTask(Task.SHUT_DOWN, this));
					}
				}
				addTask(new CATTask(Task.SHOW_INTERFACE, this));
			}
			initialized = true;
		}
	}

	/**
	 * This method sets the list of stands from which the carbon balance should be calculated.
	 * @param standList a List of CarbonToolCompatibleStand instance
	 */
	public void setStandList(List<CATCompatibleStand> standList) {
		Collections.sort(standList, StandComparator);
		waitingStandList = standList;
		addTask(new CATTask(Task.SET_STANDLIST, this));
		addTask(new CATTask(Task.UNLOCK_ENGINE, this));
		if (!isGuiEnabled()) {
			try {
				lockEngine();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	@SuppressWarnings("rawtypes")
	private TreeLoggerCompatibilityCheck getTreeLoggerCompatibilityCheck() {
		Object treeInstance = null;
		outerloop:
		for (CATCompatibleStand stand : carbonCompartmentManager.completeStandList) {
			for (StatusClass status : StatusClass.values()) {
				Collection coll = stand.getTrees(status);
				if (coll != null && !coll.isEmpty()) {
					treeInstance =  coll.iterator().next();
					break outerloop;	// once we have found at least one instance, we get out of the loop
				}
			}
		}
		TreeLoggerCompatibilityCheck c = new TreeLoggerCompatibilityCheck(treeInstance);
		return c;
	}
	
	protected void setStandList() {
		finalCutHadToBeCarriedOut = false;
//		carbonCompartmentManager.clearTreeCollections();
		carbonCompartmentManager.init(waitingStandList);
		setReferentForBiomassParameters(carbonCompartmentManager.completeStandList);
		getCarbonToolSettings().setTreeLoggerDescriptions(findMatchingTreeLoggers(getTreeLoggerCompatibilityCheck()));
		if (isGuiEnabled()) {
			Runnable doRun = new Runnable() {
				@Override
				public void run() {
					getUI().setCalculateCarbonButtonsEnabled(true);
					getUI().redefineProgressBar();
				}
			};
			SwingUtilities.invokeLater(doRun);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void setReferentForBiomassParameters(List<CATCompatibleStand> stands) {
		Object referent = null;
		if (stands != null && !stands.isEmpty()) {
			for (CATCompatibleStand stand : stands) {
				referent = stand;
				Collection<?> coll = null;
				for (StatusClass statusClass : StatusClass.values()) {
					if (coll == null) {
						coll = stand.getTrees(statusClass);
					} else {
						coll.addAll(stand.getTrees(statusClass));
					}
				}
				if (coll != null && !coll.isEmpty()) {
					Object obj = coll.iterator().next();
					if (obj instanceof CATCompatibleTree) {
						referent = obj;
						break;
					}
				}
			}
		}
		getCarbonToolSettings().setReferentForBiomassParameters(referent);
	}

	protected Vector<TreeLoggerDescription> findMatchingTreeLoggers(TreeLoggerCompatibilityCheck check) {
		Vector<TreeLoggerDescription> defaultTreeLoggerDescriptions = new Vector<TreeLoggerDescription>();
		if (check != null) {
			List<TreeLoggerDescription> availableCompatibleTreeLoggerDescription = TreeLoggerManager.getInstance().getCompatibleTreeLoggers(check);
			defaultTreeLoggerDescriptions.addAll(availableCompatibleTreeLoggerDescription);
		} else {
			defaultTreeLoggerDescriptions.add(new TreeLoggerDescription(BasicTreeLogger.class));
		}
		return defaultTreeLoggerDescriptions;
	}
	
	@Override
	protected void firstTasksToDo() {}

	@Override
	protected void decideWhatToDoInCaseOfFailure(GenericTask task) {
		super.decideWhatToDoInCaseOfFailure(task);
		unlockEngine();
		if (isGuiEnabled()) {
			getUI().setSimulationRunning(false);
		}
	}

	/**
	 * This method retrieves the simulation results after calling the calculateCarbon method.
	 * @return a CATSimulationResult instance
	 */
	public CATSimulationResult retrieveSimulationSummary() {
		return getCarbonCompartmentManager().getSimulationSummary();
	}
	
	protected CATCompartmentManager getCarbonCompartmentManager() {return carbonCompartmentManager;}

	/**
	 * Launch the calculation of the different carbon compartments.
	 * @throws InterruptedException if the engine is inadvertently unlocked
	 * @throws ProductionProcessorManagerException if the ProductionProcessorManager instance cannot be validated
	 */
	public void calculateCarbon() throws ProductionProcessorManagerException, InterruptedException {
		if (carbonCompartmentManager.getCarbonToolSettings().isValid()) {
			int nbReals = CATSensitivityAnalysisSettings.getInstance().getNumberOfMonteCarloRealizations();
			if (nbReals < 1) {
				nbReals = 1;
			}
			carbonCompartmentManager.summary = null; // reset the summary before going on
			for (int i = 0; i < nbReals; i++) {
				addTask(new CATTask(Task.RESET_MANAGER, this));
				addTask(new SetProperRealizationTask(this, i));
				addTask(new CATTask(Task.REGISTER_TREES, this));
				addTask(new CATTask(Task.LOG_AND_BUCK_TREES, this));
				addTask(new CATTask(Task.GENERATE_WOODPRODUCTS, this));
				addTask(new CATTask(Task.RETRIEVE_INITIAL_CONDITIONS, this));
				addTask(new CATTask(Task.ACTUALIZE_CARBON, this));
				if (carbonCompartmentManager.isMEMSEnabled()) {
					addTask(new CATTask(Task.RETRIEVE_SOIL_CARBON_INPUT, this));
				}
				addTask(new CATTask(Task.COMPILE_CARBON, this));
			} 
			addTask(new CATTask(Task.UNLOCK_ENGINE, this));
			if (isGuiEnabled()) {
				addTask(new CATTask(Task.DISPLAY_RESULT, this));
			} else {
				lockEngine();
			}
		}
	}

	
	protected boolean isGuiEnabled() {
		return mode == CATMode.STANDALONE || mode == CATMode.FROM_OTHER_APP;
	}

	@Override
	protected void unlockEngine() {super.unlockEngine();}
	
	@Override
	protected void lockEngine() throws InterruptedException {super.lockEngine();}

	
	protected void setFinalCutHadToBeCarriedOut(boolean finalCutHadToBeCarriedOut) {
		this.finalCutHadToBeCarriedOut = finalCutHadToBeCarriedOut;
	}

	/**
	 * Produce an export tool for CAT.<p>
	 * In script mode, the export tool can then be used to export the data to csv or dbf files.
	 * @return a CATExportTool instance
	 */
	public CATExportTool createExportTool() {
		return new CATExportTool(getCarbonCompartmentManager().getCarbonToolSettings().getSettingMemory(), 
				getCarbonCompartmentManager().getSimulationSummary());
	}
	
	/**
	 * By default, closing the gui shuts the engine down. This method must be 
	 * overriden with empty content to disable the automatic shut down.
	 */
	protected void respondToWindowClosing() {
		if (mode == CATMode.STANDALONE) {
			String className = UIManager.getLookAndFeel().getClass().getName();
			getCarbonToolSettings().getSettingMemory().setProperty("last.look.and.feel.class", className);
		}
		addTask(new CATTask(Task.SHUT_DOWN, this));
	}
	
	@Override
	public CATFrame getUI(Container parent) {
		if (owner == null && parent != null && parent instanceof Window) {
			owner = (Window) parent;
		}
		if (guiInterface == null) {
			guiInterface = new CATFrame(this, null);
		}
		return guiInterface;
	}	

	@Override
	public void showUI(Window parent) {
		getUI(parent).setVisible(true);
	}

	@Override
	public CATFrame getUI() {return getUI(null);}

	@Override
	public void showUI() {
		if (owner != null) {
			showUI(owner);
		} else {
			getUI().setVisible(true);
		}
		
	}

	protected void showResult() {
		if (isGuiEnabled()) {
			Runnable job = new Runnable() {
				@Override
				public void run() {
					getUI().displayResult();
				}
			};
			SwingUtilities.invokeLater(job);
		}
	}

	@Override
	public boolean isVisible() {
		return guiInterface != null && guiInterface.isVisible();
	}

	/**
	 * Set the biomass parameters for the simulation in script mode.
	 * @param filename a String that defines the filename (.bpf) of the biomass parameters
	 * @throws InterruptedException if the engine is inadvertently unlocked
	 */
	public void setBiomassParameters(String filename) throws InterruptedException {
		this.biomassParametersFilename = filename;
		addTask(new CATTask(Task.SET_BIOMASS_PARMS, this));
		addTask(new CATTask(Task.UNLOCK_ENGINE, this));
		if (!isGuiEnabled()) {
			lockEngine();
		}
	}

	protected void setBiomassParameters() throws IOException {
		getCarbonToolSettings().getCustomizableBiomassParameters().load(biomassParametersFilename);
		getCarbonToolSettings().setCurrentBiomassParametersSelection(BiomassParametersName.customized);
	}
	
	/**
	 * Set the production lines for the simulation in script mode. 
	 * @param filename a String that defines the filename (.prl) of the processor manager
	 * @throws InterruptedException if the engine is inadvertently unlocked
	 */
	public void setProductionManager(String filename) throws InterruptedException {
		this.productionManagerFilename = filename;
		addTask(new CATTask(Task.SET_PRODUCTION_MANAGER, this));
		addTask(new CATTask(Task.UNLOCK_ENGINE, this));
		if (!isGuiEnabled()) {
			lockEngine();
		}
	}
	
	protected void setProductionManager() throws IOException {
		getCarbonToolSettings().getCustomizableProductionProcessorManager().load(productionManagerFilename);
		getCarbonToolSettings().setCurrentProductionProcessorManagerSelection(ProductionManagerName.customized);
	}
	
	/**
	 * This method sets the different parameters of the sensitivity analysis at the level suggested by the IPCC.
	 * @param source the source of variability
	 * @param type the distribution (either uniform (default) or Gaussian
	 * @param enabled true to enable or false to disable
	 */
	public void setVariabilitySource(VariabilitySource source, Distribution.Type type, boolean enabled) {
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(source, type, enabled, source.getSuggestedIPCCValue() * .01);
	}	

	/**
	 * This method sets the different parameters of the sensitivity analysis
	 * @param source the source of variability
	 * @param type the distribution (either uniform (default) or Gaussian
	 * @param enabled true to enable or false to disable
	 * @param multiplier a value between 0.0 and 0.5 (50%)
	 */
	public void setVariabilitySource(VariabilitySource source, Distribution.Type type, boolean enabled, double multiplier) {
		CATSensitivityAnalysisSettings.getInstance().setVariabilitySource(source, type, enabled, multiplier);
	}

	/**
	 * This method returns true if this CAT instance has been initialized or false otherwise
	 * @return a boolean
	 */
	public boolean isInitialized() {return initialized;}
	
	/**
	 * Entry point for CAT in stand alone modes.
	 * @param args a series of parameters 
	 */
	public static void main(String[] args) {
		String languageOption = "-l";
		String logLevelOption = "-loglevel";
		List<String> arguments = Arrays.asList(args);
		if (arguments.isEmpty()) {
			System.out.println("Usage: [-l <value>] [-loglevel <value>]");
			System.out.println("");
			System.out.println("   l		set the language (either en for English (default) or fr for French)");
			System.out.println("   loglevel	set the log level (either INFO (default), FINE, FINER, or FINEST; see java.util.logging.Level class)");
		}
		String selectedLanguage = REpiceaSystem.retrieveArgument(languageOption, arguments);
		if (selectedLanguage == null) {
			System.out.println("Language set to default (English)!");
			REpiceaTranslator.setCurrentLanguage(Language.English);
		} else if (selectedLanguage.equals("en")) {
			REpiceaTranslator.setCurrentLanguage(Language.English);
		} else if (selectedLanguage.equals("fr")) {
			REpiceaTranslator.setCurrentLanguage(Language.French);
		} else {
			System.err.println("The language option " + selectedLanguage + " is not recognized!");
			return;
		}
		String selectedLogLevel = REpiceaSystem.retrieveArgument(logLevelOption, arguments);
		if (selectedLogLevel == null) {
			System.out.println("Log level set to default (INFO)!");
			REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.INFO);
		} else if (selectedLogLevel == "INFO") {
			REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.INFO);
		} else if (selectedLogLevel == "FINE") {
			REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.FINE);
		} else if (selectedLogLevel == "FINER") {
			REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.FINER);
		} else if (selectedLogLevel == "FINEST") {
			REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.FINEST);
		} else  {
			System.err.println("The log level option " + selectedLogLevel + " is not recognized!");
		}
		CarbonAccountingTool tool = new CarbonAccountingTool();
		try {
			tool.initializeTool();
		} catch (Exception e) {
			System.out.println("Unable to initialize CAT.");
			e.printStackTrace();
		}
	}


	
}
