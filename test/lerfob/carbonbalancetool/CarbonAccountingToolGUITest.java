/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2022 Her Majesty the Queen in right of Canada
 * Author: Mathieu Fortin, Canadian Wood Fibre Centre
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

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.logging.Level;

import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
//import org.junit.Ignore;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import lerfob.carbonbalancetool.CATUtility.ProductionManagerName;
import lerfob.carbonbalancetool.CarbonAccountingTool.CATMode;
import lerfob.carbonbalancetool.io.CATGrowthSimulationRecordReader;
import lerfob.carbonbalancetool.io.CATScaleManagementSelectorDialog;
import lerfob.carbonbalancetool.io.CATSpeciesSelectionDialog;
import lerfob.carbonbalancetool.productionlines.ProductionProcessorManagerDialog;
import lerfob.carbonbalancetool.productionlines.affiliere.AffiliereImportExportTest;
import repicea.gui.REpiceaAWTProperty;
import repicea.gui.REpiceaGUITestRobot;
import repicea.gui.UIControlManager;
import repicea.gui.UIControlManager.CommonControlID;
import repicea.gui.UIControlManager.CommonMenuTitle;
import repicea.gui.components.REpiceaMatchSelectorDialog;
import repicea.gui.genericwindows.REpiceaLicenseWindow;
import repicea.io.tools.ImportFieldManagerDialog;
import repicea.simulation.covariateproviders.samplelevel.ApplicationScaleProvider.ApplicationScale;
import repicea.simulation.processsystem.SystemManagerDialog.MessageID;
import repicea.util.ObjectUtility;
import repicea.util.REpiceaLogManager;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CarbonAccountingToolGUITest {
	
	static CarbonAccountingTool CAT;
	static REpiceaGUITestRobot ROBOT;
	
	@BeforeClass
	public static void initTest() throws Exception {
		REpiceaLogManager.getLogger(CarbonAccountingTool.LOGGER_NAME).setLevel(Level.SEVERE);
		CAT = new CarbonAccountingTool(CATMode.FROM_OTHER_APP);		// FROM_OTHER_APP to avoid shutdown with exit 
		Runnable toRun = new Runnable() {
			@Override
			public void run() {
				try {
					CAT.initializeTool();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		ROBOT = new REpiceaGUITestRobot();
		Thread t = ROBOT.startGUI(toRun, REpiceaLicenseWindow.class);
		ROBOT.clickThisButton("acceptLicense");
		ROBOT.clickThisButton(UIControlManager.CommonControlID.Continue.name(), CAT.getUI().getClass());
	}
	
	@AfterClass
	public static void cleanUp() throws Exception {
		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name());
		Runnable doRun = new Runnable() {
			public void run() {
				try {
					ROBOT.clickThisButton(UIControlManager.CommonControlID.Quit.name());
				} catch (InvocationTargetException | InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		ROBOT.startGUI(doRun, JDialog.class);
		ROBOT.shutdown();
		ROBOT.clickThisButton("Yes");
		CAT.guiInterface.dispose();
	}

	@Test
	public void test04SVGExportHappyPath() throws Exception {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.out.println("Test will be skipped because Mac does not support Times font.");
			return;
		}
		JComboBox b = (JComboBox) ROBOT.findComponentWithThisName(CATFrame.MessageID.HWP_Parameters.name() + "_ComboBox");
		Runnable toRun = new Runnable() {
			public void run() {
				b.getModel().setSelectedItem(CAT.getCarbonCompartmentManager().getCarbonToolSettings().productionManagerMap.get(ProductionManagerName.customized));
			}
		};
		SwingUtilities.invokeLater(toRun);
		ROBOT.letDispatchThreadProcess();
		
		toRun = new Runnable() {
			public void run() {
				try {
					ROBOT.clickThisButton(CATFrame.MessageID.HWP_Parameters.name());
				} catch (InvocationTargetException | InterruptedException e) {}
			}
		};
		Thread t = new Thread(toRun);
		t.start();
		
		ROBOT.letDispatchThreadProcess();
		
		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name()); 
		ROBOT.clickThisButton(MessageID.ExportMenu.name()); 
		
		toRun = new Runnable() {
			public void run() {
				try {
					ROBOT.clickThisButton(MessageID.ExportToSVG.name());
				} catch (InvocationTargetException | InterruptedException e) {} 
			}
		};
		ROBOT.startGUI(toRun, JDialog.class);

		String filename = System.getProperty("java.io.tmpdir") + "exportFluxConfiguration.svg";
		File f = new File(filename);
		if (f.exists())
			f.delete();
		ROBOT.fillThisTextField("Filename", filename);
		
		ROBOT.clickThisButton(CommonControlID.Save.name(), REpiceaAWTProperty.SVGFileSaved);
		
		Assert.assertTrue("Testing if svg export file exists", f.exists());

		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name()); 
		ROBOT.clickThisButton(UIControlManager.CommonControlID.Close.name());
	}
	
	@Test
	public void test02ImportAndSimulateYieldTableHappyPath() throws Exception {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.out.println("Test will be skipped because Mac does not support Times font.");
			return;
		}
		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name());
		ROBOT.clickThisButton(UIControlManager.CommonControlID.Import.name()); 
		Runnable toRun = new Runnable() {
			@Override
			public void run() {
				try {
					ROBOT.clickThisButton(CATFrame.MessageID.ImportYieldTable.name()); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		Thread t = ROBOT.startGUI(toRun, JDialog.class);
		String fileToLoad = ObjectUtility.getPackagePath(getClass()) + File.separator + "io" + File.separator + "ExampleYieldTable.csv";
		ROBOT.fillThisTextField("Filename", fileToLoad);
		ROBOT.clickThisButton("Open", CATSpeciesSelectionDialog.class);		
		ROBOT.clickThisButton("Ok", ImportFieldManagerDialog.class);
		ROBOT.clickThisButton("Ok", CATAWTProperty.StandListProperlySet);
		
		List<CATCompatibleStand> stands = CAT.getCarbonCompartmentManager().completeStandList;
		Assert.assertEquals("Testing the number of CATCompatibleStand instances imported", 46, stands.size());
		
		int tabCountBefore = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		ROBOT.clickThisButton(CATFrame.MessageID.CalculateCarbonBalance.name(), CATAWTProperty.CarbonCalculationSuccessful); 
		int tabCountAfter = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		Assert.assertEquals("Testing if a tab has been craeted in the tabbedPane instance", tabCountBefore + 1, tabCountAfter);
	}
	
//	@Ignore
	@Test
	public void test03ImportAndSimulateGrowthSimulationHappyPath() throws Exception {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.out.println("Test will be skipped because Mac does not support Times font.");
			return;
		}
		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name());
		ROBOT.clickThisButton(UIControlManager.CommonControlID.Import.name()); 
		
		Runnable toRun = new Runnable() {
			@Override
			public void run() {
				try {
					ROBOT.clickThisButton(CATFrame.MessageID.ImportGrowthSimulation.name()); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		Thread t = ROBOT.startGUI(toRun, JDialog.class);
		String fileToLoad = ObjectUtility.getPackagePath(getClass()) + File.separator + "io" + File.separator + "MathildeTreeExport.csv";
		ROBOT.fillThisTextField("Filename", fileToLoad);
		ROBOT.clickThisButton("Open", CATScaleManagementSelectorDialog.class);	
		ROBOT.clickThisButton("Ok", ImportFieldManagerDialog.class);		
		ROBOT.clickThisButton("Ok", REpiceaMatchSelectorDialog.class);
		ROBOT.clickThisButton("File");

		toRun = new Runnable() {
			@Override
			public void run() {
				try {
					ROBOT.clickThisButton("Open"); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		ROBOT.startGUI(toRun, JDialog.class);
		
		fileToLoad = ObjectUtility.getPackagePath(getClass()) + File.separator + "io" + File.separator + "speciesCorrespondanceForSimulationData.xml";
		ROBOT.fillThisTextField("Filename", fileToLoad);
		ROBOT.clickThisButton("Open");		
		ROBOT.letDispatchThreadProcess();
		ROBOT.clickThisButton("Ok", CATAWTProperty.StandListProperlySet);
		
		List<CATCompatibleStand> stands = CAT.getCarbonCompartmentManager().completeStandList;
		Assert.assertEquals("Testing the number of CATCompatibleStand instances imported", 8, stands.size());
		
		int tabCountBefore = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		ROBOT.clickThisButton(CATFrame.MessageID.CalculateCarbonBalance.name(), CATAWTProperty.CarbonCalculationSuccessful); 
		int tabCountAfter = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		Assert.assertEquals("Testing if a tab has been craeted in the tabbedPane instance", tabCountBefore + 1, tabCountAfter);
		CATSingleViewPanel singleViewPanel = (CATSingleViewPanel) ROBOT.findComponentWithThisName("Sim 3");
		singleViewPanel.selector.setSelectedIndex(2);
		Assert.assertTrue("Testing if the panel displays the information", singleViewPanel.selector.getSelectedItem() instanceof CATResultHWPPanel);
		singleViewPanel.refreshInterface();
		int u = 0;
	}
	
	
	@Test
	public void test01ImportAndSimulateGrowthSimulationSinglePlotHappyPath() throws Exception {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.out.println("Test will be skipped because Mac does not support Times font.");
			return;
		}

		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name());
		ROBOT.clickThisButton(UIControlManager.CommonControlID.Import.name()); 
		
		Runnable toRun = new Runnable() {
			@Override
			public void run() {
				try {
					ROBOT.clickThisButton(CATFrame.MessageID.ImportGrowthSimulation.name()); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		Thread t = ROBOT.startGUI(toRun, JDialog.class);
		String fileToLoad = ObjectUtility.getPackagePath(getClass()) + File.separator + "io" + File.separator + "art2009UnevenagedSimulation.csv";
		ROBOT.fillThisTextField("Filename", fileToLoad);
		ROBOT.clickThisButton("Open", CATScaleManagementSelectorDialog.class);	
		JComboBox scaleCb = (JComboBox) ROBOT.findComponentWithThisName("scaleComboBox");
		scaleCb.setSelectedItem(ApplicationScale.Stand);
		ROBOT.clickThisButton("Ok", ImportFieldManagerDialog.class);		
		ROBOT.clickThisButton("Ok", REpiceaMatchSelectorDialog.class);
		ROBOT.clickThisButton("File");

		toRun = new Runnable() {
			@Override
			public void run() {
				try {
					ROBOT.clickThisButton("Open"); 
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		
		ROBOT.startGUI(toRun, JDialog.class);
		
		fileToLoad = ObjectUtility.getPackagePath(getClass()) + File.separator + "io" + File.separator + "speciesCorrespondanceForUnevenAgedSimulation.xml";
		ROBOT.fillThisTextField("Filename", fileToLoad);
		ROBOT.clickThisButton("Open");		
		ROBOT.letDispatchThreadProcess();
		ROBOT.clickThisButton("Ok", CATAWTProperty.StandListProperlySet);

		ROBOT.letDispatchThreadProcess();
		ROBOT.clickThisButton("OK");

		ROBOT.letDispatchThreadProcess();

		List<CATCompatibleStand> stands = CAT.getCarbonCompartmentManager().completeStandList;
		Assert.assertEquals("Testing the number of CATCompatibleStand instances imported", 5, stands.size());
		
		int tabCountBefore = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		ROBOT.clickThisButton(CATFrame.MessageID.CalculateCarbonBalance.name(), CATAWTProperty.CarbonCalculationSuccessful); 
		int tabCountAfter = CAT.getUI().graphicPanel.tabbedPane.getTabCount();
		Assert.assertEquals("Testing if a tab has been craeted in the tabbedPane instance", tabCountBefore + 1, tabCountAfter);
		
		CATSingleSimulationResult summary = CAT.getCarbonCompartmentManager().getSimulationSummary();
		Assert.assertTrue("Testing if budget map is not empty", !summary.getBudgetMap().isEmpty());

	}

	@Ignore // TODO Renable this test when the import method has been clarified
	@Test
	public void test05AFFiliereImportHappyPath() throws Exception {
		if (System.getProperty("os.name").toLowerCase().contains("mac")) {
			System.out.println("Test will be skipped because Mac does not support Times font.");
			return;
		}
		JComboBox b = (JComboBox) ROBOT.findComponentWithThisName(CATFrame.MessageID.HWP_Parameters.name() + "_ComboBox");
		Runnable toRun = new Runnable() {
			public void run() {
				b.getModel().setSelectedItem(CAT.getCarbonCompartmentManager().getCarbonToolSettings().productionManagerMap.get(ProductionManagerName.customized));
			}
		};
		SwingUtilities.invokeLater(toRun);
		ROBOT.letDispatchThreadProcess();
		
		ROBOT.clickThisButton(CATFrame.MessageID.HWP_Parameters.name());
		ROBOT.letDispatchThreadProcess();
		
//		toRun = new Runnable() {
//			public void run() {
//				try {
//					ROBOT.clickThisButton(CATFrame.MessageID.HWP_Parameters.name());
//				} catch (InvocationTargetException | InterruptedException e) {}
//			}
//		};
//		Thread t = new Thread(toRun);
//		ROBOT.letDispatchThreadProcess();
//		ROBOT.startGUI(toRun, ProductionProcessorManagerDialog.class);
		
		ROBOT.clickThisButton(UIControlManager.CommonMenuTitle.File.name()); 
		ROBOT.clickThisButton(ProductionProcessorManagerDialog.MessageID.ImportFromOtherSources.name()); 
		
		toRun = new Runnable() {
			public void run() {
				try {
					ROBOT.clickThisButton(ProductionProcessorManagerDialog.MessageID.ImportFromAFFiliere.name(), JDialog.class);
				} catch (InvocationTargetException | InterruptedException e) {} 
			}
		};
		ROBOT.startGUI(toRun, JDialog.class);

		String filename = ObjectUtility.getPackagePath(AffiliereImportExportTest.class) + "EtudesAvecEtiquette4.json";
		ROBOT.fillThisTextField("Filename", filename);
		ROBOT.clickThisButton(CommonControlID.Open.name(), CATAWTProperty.AffiliereImportSuccessful);
		
		int nbProcesses = CAT.getCarbonToolSettings().getCurrentProductionProcessorManager().getList().size();
		Assert.assertEquals("Comparing number of processes", 94, nbProcesses);

		ROBOT.clickThisButton(CommonMenuTitle.File.name());

		toRun = new Runnable() {
			public void run() {
				try {
					ROBOT.clickThisButton(CommonControlID.Open.name());
				} catch (InvocationTargetException | InterruptedException e) {} 
			}
		};
		Thread t = new Thread(toRun);
		t.start();
		ROBOT.letDispatchThreadProcess();

		filename = ObjectUtility.getPackagePath(AffiliereImportExportTest.class) + "hardwood_recycling_en.prl";
		ROBOT.fillThisTextField("Filename", filename);
		ROBOT.clickThisButton(CommonControlID.Open.name());
		ROBOT.letDispatchThreadProcess();
		
		ROBOT.clickThisButton(CommonMenuTitle.File.name());
		ROBOT.clickThisButton(CommonControlID.Close.name());

	}


}
