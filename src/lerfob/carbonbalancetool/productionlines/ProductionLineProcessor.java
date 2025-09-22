/*
 * This file is part of the lerfob-forestools library.
 *
 * Copyright (C) 2010-2012 Mathieu Fortin for LERFOB INRA/AgroParisTech, 
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
import java.io.Serializable;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.swing.event.CaretEvent;

import lerfob.carbonbalancetool.productionlines.CarbonUnit.BiomassType;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.CarbonUnitStatus;
import lerfob.carbonbalancetool.productionlines.CarbonUnit.Element;
import lerfob.carbonbalancetool.productionlines.EndUseWoodProductCarbonUnitFeature.UseClass;
import repicea.gui.REpiceaUIObject;
import repicea.serial.PostUnmarshalling;
import repicea.simulation.covariateproviders.treelevel.SpeciesTypeProvider.SpeciesType;
import repicea.simulation.covariateproviders.treelevel.TreeStatusProvider.StatusClass;
import repicea.simulation.processsystem.AmountMap;
import repicea.simulation.processsystem.ProcessUnit;
import repicea.simulation.processsystem.Processor;
import repicea.simulation.processsystem.ProcessorButton;
import repicea.simulation.processsystem.SystemPanel;
import repicea.util.REpiceaTranslator;


/**
 * The ProductionLineProcessor class handles all the processing from the wood piece to a particular end product.<p>
 * It includes an intake factor (from 0 to 1), a yield factor (from 0 to 1) and an indicator that specifies
 * whether or not the residual from this processor can be used for energy.
 * @author M. Fortin - September 2010
 */
public final class ProductionLineProcessor extends AbstractProductionLineProcessor implements Serializable, REpiceaUIObject, PostUnmarshalling {
	
	private static final long serialVersionUID = 20101018L;
	
	@Deprecated
	private static ProductionLineProcessor LossProductionLineProcessor;
	
	@Deprecated
	private double averageYield;
	@Deprecated
	private boolean sentToAnotherMarket;
	@Deprecated
	private int selectedMarketToBeSentTo;
	@Deprecated
	private String selectedMarketToBeSentToStr;
	@Deprecated
	private ProductionLineProcessor fatherProcessor;
	@Deprecated
	private List<AbstractExtractionProcessor> extractionProcessors;

	private Processor disposedToProcessor;
		
	private AbstractExtractionProcessor extractionProcessor;
	
	@Deprecated
	private ProductionLine market;
	
	
	/**
	 * Official constructor in GUI mode.
	 */
	protected ProductionLineProcessor() {
		super();
		averageYield = 1d;
		woodProductFeature = new EndUseWoodProductCarbonUnitFeature(this);
	}

	
	/**
	 * Constructor for primary processor. The processor knows the market it belongs to through the 
	 * parameter market.
	 * @param market a WoodProductMarketModel instance
	 */
	@Deprecated
	protected ProductionLineProcessor(ProductionLine market) {
		super();
		this.market = market;
		woodProductFeature = new EndUseWoodProductCarbonUnitFeature(this);
	}

	/**
	 * Constructor for which the average intake and yield would be known.
	 * @param market a WoodProductMarketModel instance
	 * @param averageIntake the average intake as a proportion
	 * @param averageYield the average yield as a proportion
	 */
	@Deprecated
	protected ProductionLineProcessor(ProductionLine market, double averageIntake, double averageYield) {
		this(market);
		setAverageIntake(averageIntake);
		setAverageYield(averageYield);
	}
	
	/**
	 * Constructor for subProcessor. This processor is the child of the fatherProcessor.
	 * @param market a WoodProductMarketModel instance
	 * @param fatherProcessor a WoodProductProcessor instance
	 */
	@Deprecated
	protected ProductionLineProcessor(ProductionLine market, ProductionLineProcessor fatherProcessor) {
		this(market);
		this.fatherProcessor = fatherProcessor;
	}

	/**
	 * Set the processor to which the carbon units are sent 
	 * once they've reached the end of their useful lifetime.
	 * @param p a Processor instance
	 */
	public void setDisposedToProcessor(Processor p) {
		disposedToProcessor = p;
	}

	/**
	 * Provide the processor to which the carbon units are sent 
	 * once they've reached the end of their useful lifetime.
	 * @return a Processor instance (null if the processor has not been set)
	 */
	public Processor getDisposedToProcess() {
		return disposedToProcessor;
	}
	
	AbstractExtractionProcessor getExtractionProcessor() {
		return extractionProcessor;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Collection<ProcessUnit> doProcess(List<ProcessUnit> inputUnits) {
		Collection<ProcessUnit> resultingUnits = new ArrayList<ProcessUnit>();
		if (extractionProcessor != null) {
			resultingUnits.addAll(extractionProcessor.extractAndProcess(this, inputUnits));
		}
		if (!inputUnits.isEmpty()) {
			resultingUnits.addAll(super.doProcess(inputUnits));
		}
		return resultingUnits;
	}
	
	void setExtractionProcessor(AbstractExtractionProcessor p) {
		extractionProcessor = p;
	}

	
	/**
	 * This method returns the average intake (from 0 to 1) taken from the father processor.
	 * @return a double
	 */
	@Deprecated
	public double getAverageIntake() {return averageIntake;}

	@Deprecated
	protected void setAverageIntake(double intake) {
		averageIntake = intake;
		if (intake != 1d) {
			if (fatherProcessor == null) {
				throw new InvalidParameterException("There is a misunderstanding about the father processor!");
			} else {
				fatherProcessor.getSubProcessorIntakes().put(this, (int) (intake * 100));
			}
		}
	}
	

	@Deprecated
	protected void setAverageYield(double averageYield) {this.averageYield = averageYield;}
	
	/**
	 * This method identifies the landfill site processor.
	 * @return a boolean
	 */
	@Deprecated
	protected boolean isLandfillProcessor() {return (woodProductFeature instanceof LandfillCarbonUnitFeature);}

	/**
	 * This method identifies the left in forest processor.
	 * @return a boolean
	 */
	@Deprecated
	protected boolean isLeftInForestProcessor() {
		return !(woodProductFeature instanceof LandfillCarbonUnitFeature) && !(woodProductFeature instanceof EndUseWoodProductCarbonUnitFeature);
	}
	
	@Override
	public ProcessorButton getUI(Container container) {
		if (guiInterface == null) {
			guiInterface = new ProductionLineProcessorButton((SystemPanel) container, this);
		}
		return guiInterface;
	}
	

	@Deprecated
	protected double getAverageYield() {return averageYield;}		// from now on yield should always be 100%
	
	@Deprecated
	protected boolean isPrimaryProcessor() {return fatherProcessor == null;}
	
	/**
	 * Check if the production line processor is final, i.e. it stands for an end-use product.<p>
	 * 
	 * This happens when the production line processor has no sub processors.
	 * @return a boolean
	 */
	protected boolean isFinalProcessor() {return (!hasSubProcessors() && !sentToAnotherMarket);}
	
	@Deprecated
	protected String getProductionLineToBeSentTo() {
		if (selectedMarketToBeSentToStr == null) {
			List<String> productionLineNames = getProductionLine().getManager().getProductionLineNames();
			if (selectedMarketToBeSentTo < productionLineNames.size()) {
				selectedMarketToBeSentToStr = productionLineNames.get(selectedMarketToBeSentTo);
			} else {
				selectedMarketToBeSentToStr = productionLineNames.get(0);
			}
		}
		return selectedMarketToBeSentToStr;
	}

	
	
	/**
	 * This method is recursive. It goes up until it reaches the primary processor and calculates what the initial volume
	 * was before the first transformation.
	 * @param processedVolume the volume after processing (m3)
	 * @return the initial volume before any processing (m3)
	 */
	@Deprecated
	protected double getInitialVolumeBeforeFirstTransformation(double processedVolume) {
		double yieldFactor = (double) 1 / averageYield;
		double initialVolume = processedVolume * yieldFactor;
		if (!isPrimaryProcessor()) {
			initialVolume = fatherProcessor.getInitialVolumeBeforeFirstTransformation(initialVolume);
		}
		return initialVolume;
	}

	@Deprecated
	protected boolean isSentToAnotherMarket() {return sentToAnotherMarket;}


	@SuppressWarnings({ "rawtypes"})
	@Override
	protected List<ProcessUnit> createProcessUnitsFromThisProcessor(ProcessUnit unit, Number intake) {
		List<ProcessUnit> outputUnits = new ArrayList<ProcessUnit>();

		CarbonUnit carbonUnit = (CarbonUnit) unit;
		int dateIndex = carbonUnit.getIndexInTimeScale();

		AmountMap<Element> processedAmountMap = carbonUnit.getAmountMap().multiplyByAScalar(intake.doubleValue() * .01);

		CarbonUnit woodProduct;

		if (!isFinalProcessor()) {
			woodProduct = new CarbonUnit(dateIndex, null, processedAmountMap, carbonUnit);
			outputUnits.add(woodProduct);
			return outputUnits;
		} else {
			woodProduct = new EndUseWoodProductCarbonUnit(dateIndex, (EndUseWoodProductCarbonUnitFeature) woodProductFeature, 
					processedAmountMap,	carbonUnit);
			outputUnits.add(woodProduct);
			return outputUnits;
		}
	}
	

	/**
	 * This method returns a collection of end products that can be produced from this piece of wood. 
	 * This collection is defined by the end product features associated in the tree log category of this 
	 * piece.
	 * @param dateIndex the index of the creation date
	 * @param speciesName the name of the species
	 * @param speciesType a SpeciesType enum
	 * @param statusClass a StatusClass enum
	 * @param amountMap an AmountMap instance
	 * @return a CarbonUnitMap instance 
	 */
	@Deprecated
	protected CarbonUnitMap<CarbonUnitStatus> processWoodPiece(int dateIndex, 
			String speciesName, 
			SpeciesType speciesType, 
			StatusClass statusClass, 
			AmountMap<Element> amountMap) {

		CarbonUnitMap<CarbonUnitStatus> outputMap = new CarbonUnitMap<CarbonUnitStatus>(CarbonUnitStatus.EndUseWoodProduct);

		String sampleUnitID = "";
		
		CarbonUnit woodProduct;
		
		boolean somethingIsLoss = (averageYield != 1d);
		
		AmountMap<Element> processedAmountMap = amountMap.multiplyByAScalar(averageYield);
			
		if (processedAmountMap.get(Element.C) > ProductionProcessorManager.VERY_SMALL) {		// to avoid looping indefinitely
			if (isFinalProcessor()) {
				if (!isLandfillProcessor() && !isLeftInForestProcessor()) {
					woodProduct = new EndUseWoodProductCarbonUnit(
							getInitialVolumeBeforeFirstTransformation(processedAmountMap.get(Element.Volume)),		
							dateIndex, 
							(EndUseWoodProductCarbonUnitFeature) woodProductFeature,
							processedAmountMap,
							speciesName,
							speciesType,
							statusClass);
					outputMap.get(CarbonUnitStatus.EndUseWoodProduct).add(woodProduct);

				} else if (isLandfillProcessor()) {
					LandfillCarbonUnitFeature lfcuf = (LandfillCarbonUnitFeature) woodProductFeature;
					double docf = lfcuf.getDegradableOrganicCarbonFraction();
					
					AmountMap<Element> landFillMapTmp = processedAmountMap.multiplyByAScalar(docf);
					woodProduct = new LandfillCarbonUnit(dateIndex, 
							sampleUnitID, 
							lfcuf, 
							landFillMapTmp, 
							speciesName,
							speciesType,
							statusClass,
							BiomassType.Wood, 
							CarbonUnitStatus.LandFillDegradable);
					getProductionLine().getManager().getCarbonUnits(CarbonUnitStatus.LandFillDegradable).add((LandfillCarbonUnit) woodProduct); 
					
					landFillMapTmp = processedAmountMap.multiplyByAScalar(1 - docf);
					woodProduct = new LandfillCarbonUnit(dateIndex, 
							sampleUnitID, 
							lfcuf, 
							landFillMapTmp, 
							speciesName,
							speciesType,
							statusClass,
							BiomassType.Wood, 
							CarbonUnitStatus.LandFillNonDegradable); 
					getProductionLine().getManager().getCarbonUnits(CarbonUnitStatus.LandFillNonDegradable).add((LandfillCarbonUnit) woodProduct); 
					
				} else {				// is left in the forest
					woodProduct = new CarbonUnit(dateIndex, sampleUnitID, woodProductFeature, processedAmountMap, speciesName, speciesType, statusClass, BiomassType.Wood, null); // woodyDebrisType set to null
					getProductionLine().getManager().getCarbonUnits(CarbonUnitStatus.DeadWood).add(woodProduct);
				}

			} else if (hasSubProcessors()) {
				for (Processor subProcessor : getSubProcessors()) {
					AmountMap<Element> subProcessedMap = processedAmountMap.multiplyByAScalar(((ProductionLineProcessor) subProcessor).getAverageIntake());
					outputMap.add(((ProductionLineProcessor) subProcessor).processWoodPiece(dateIndex, speciesName, speciesType, statusClass, subProcessedMap));
				}
			} else if (isSentToAnotherMarket()) {
				outputMap.add(getProductionLine().getManager().processWoodPieceIntoThisProductionLine(getProductionLineToBeSentTo(), 
						dateIndex,
						speciesName,
						speciesType,
						statusClass,
						processedAmountMap));
			}
		}
		
		if (somethingIsLoss) {
			AmountMap<Element> lossAmountMap = amountMap.multiplyByAScalar(1 - averageYield);
			CarbonUnitMap<CarbonUnitStatus> tmpMap = getLossProcessor().processWoodPiece(dateIndex, speciesName, speciesType, statusClass, lossAmountMap);
			for (Collection<CarbonUnit> carbonUnits : tmpMap.values()) {
				outputMap.get(CarbonUnitStatus.IndustrialLosses).addAll(carbonUnits);
			}
		}
		
		return outputMap;
	}


//	/**
//	 * This class listens to the slider in the Gui interface.
//	 */
//	@Override
//	public void stateChanged(ChangeEvent evt) {
//		if (evt.getSource().equals(formerGuiInterface.yieldSlider)) {
//			double factor = (double) 1 / formerGuiInterface.yieldSlider.getMaximum();
//			this.averageYield = formerGuiInterface.yieldSlider.getValue() * factor;
//			
//		} else if (evt.getSource().equals(formerGuiInterface.intakeSlider)) {
//			double factor = (double) 1 / formerGuiInterface.intakeSlider.getMaximum();
//			setAverageIntake(formerGuiInterface.intakeSlider.getValue() * factor);
//			
//		}
//	}
	
	
//	public ProductionLineProcessorPanel getFormerGuiInterface() {
//		if (formerGuiInterface == null) {
//			formerGuiInterface = new ProductionLineProcessorPanel(this);
//		}
//		return formerGuiInterface;
//	}
	

	/**
	 * This class listens to the text field of its Gui interface.
	 * @param evt a CaretEvent
	 */
	@Override
	public void caretUpdate(CaretEvent evt) {
//		if (evt.getSource().equals(getFormerGuiInterface().processorTextField)) {
//			setName(((JTextField) evt.getSource()).getText());
//		} else {
		super.caretUpdate(evt);
//		}
	}

	
//	@Override
//	public void itemStateChanged(ItemEvent evt) {
//		if (evt.getSource().equals(formerGuiInterface.sendToAnotherMarketCheckBox)) {
//			sentToAnotherMarket = formerGuiInterface.sendToAnotherMarketCheckBox.isSelected();
//			formerGuiInterface.checkWhatFeatureShouldBeEnabled();
//		} else if (evt.getSource().equals(formerGuiInterface.availableMarkets)) {
//			setProductionLineToBeSentTo((String) formerGuiInterface.availableMarkets.getSelectedItem());
//		}
//
//	}

	@Deprecated
	protected ProductionLine getProductionLine() {return market;}
	
	@Deprecated
	protected void setProductionLineToBeSentTo(String productionLine) {
		this.selectedMarketToBeSentToStr = productionLine;
	}
	
	@Deprecated
	protected static ProductionLineProcessor getLandfillProcessor(ProductionLineProcessor fatherProcessor, ProductionLine market) {
		ProductionLineProcessor landfillProcessor = new ProductionLineProcessor(market, fatherProcessor);
		landfillProcessor.setAverageYield(1d);
		landfillProcessor.setAverageIntake(1d);
		landfillProcessor.woodProductFeature = new LandfillCarbonUnitFeature(landfillProcessor);
		landfillProcessor.setName(REpiceaTranslator.getString(ProductionProcessorManagerDialog.MessageID.LandFillMarketLabel));
		return landfillProcessor;
	}

	@Deprecated
	protected static ProductionLineProcessor getLeftInForestProcessor(ProductionLineProcessor fatherProcessor, ProductionLine market) {
		ProductionLineProcessor leftInForestProcessor = new ProductionLineProcessor(market, fatherProcessor);
		leftInForestProcessor.setAverageYield(1d);
		leftInForestProcessor.setAverageIntake(1d);
		leftInForestProcessor.woodProductFeature = new CarbonUnitFeature(leftInForestProcessor);
		leftInForestProcessor.woodProductFeature.setAverageLifetime(10d);
		leftInForestProcessor.setName(REpiceaTranslator.getString(ProductionProcessorManagerDialog.MessageID.LeftInForestLabel));
		return leftInForestProcessor;
	}

	@Deprecated
	private ProductionLineProcessor getLossProcessor() {
		if (LossProductionLineProcessor == null) {
			LossProductionLineProcessor = new ProductionLineProcessor(null, 1, 1);	// with no market
			EndUseWoodProductCarbonUnitFeature feature = (EndUseWoodProductCarbonUnitFeature) LossProductionLineProcessor.getEndProductFeature();
			feature.setAverageLifetime(0);
			feature.setAverageSubstitution(0);
			feature.setDisposable(false);
			feature.setDisposableProportion(0);
			feature.setLCA(null);
			feature.setUseClass(UseClass.INTERNAL_CONSUMPTION);
		}
		return LossProductionLineProcessor; 
	}


	@Override
	public void postUnmarshallingAction() {
		if (extractionProcessors != null  && !extractionProcessors.isEmpty()) {
			extractionProcessor = extractionProcessors.get(0);
		}
	}
	
	/**
	 * Update several characteristics of the EndUseCarbonUnitFeature instance.<p>
	 *
	 * The method updates the following variables:<ul>
	 * <li> the use class;
	 * <li> the type of decay function;
	 * <li> the mode (half-life versus average lifetime);
	 * <li> the average lifetime
	 * </ul>
	 * 
	 * The method has no effect if the CarbonUnitFeature instance is not an
	 * instance of the EndUseCarbonUnitFeature class.
	 * @param feature an IPCCEndUseProductDefaultFeature instance
	 */
	public void updateFeature(EndUseProductDefaultFeature feature) {
		if (getEndProductFeature() instanceof EndUseWoodProductCarbonUnitFeature) {
			((EndUseWoodProductCarbonUnitFeature) getEndProductFeature()).updateFeature(feature);
		}
	}

}
