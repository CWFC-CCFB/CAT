/*
 * This file is part of the CAT library.
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

import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import repicea.gui.REpiceaPanel;
import repicea.gui.UIControlManager;
import repicea.util.REpiceaTranslator;
import repicea.util.REpiceaTranslator.TextableEnum;

/**
 * A panel with additional characteristics for {@link AbstractProductionLineProcessor} instances.<p>
 * The {@link AbstractProductionLineProcessor} instance provides this panel to the {@link EnhancedProcessorInternalDialog} instance
 * when the user double clicks on the {@link repicea.simulation.processsystem.ProcessorButton} instance in the flux configuration.
 * @author Mathieu Fortin - 2012
 */
public class CarbonUnitFeaturePanel extends REpiceaPanel {
	
	private static final long serialVersionUID = 20101020L;

	public static enum MessageID implements TextableEnum {
		WoodProductFeatureLabel("Specific features", "Caract\u00E9ristiques sp\u00E9cifiques"),
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
	
	
	private CarbonUnitFeature caller;
	
	protected JPanel mainPanel;
	protected DecayFunctionPanel decayFunctionPanel;
	protected JTextArea sourceTextArea;

	protected CarbonUnitFeaturePanel(CarbonUnitFeature caller) {
		super();
		setCaller(caller);
		initializeFields();
		createUI();
	}

	protected CarbonUnitFeature getCaller() {return caller;}
	protected void setCaller(CarbonUnitFeature caller) {this.caller = caller;}
	
	protected void initializeFields() {} // this method is required by derived classes so leave it there
	
	protected void createUI() {
		setLayout(new BorderLayout());
		setBorder(UIControlManager.getTitledBorder(MessageID.WoodProductFeatureLabel.toString()));
		JPanel setupPanel = new JPanel(new BorderLayout());
		add(setupPanel, BorderLayout.CENTER);
		mainPanel = new JPanel();
		setupPanel.add(mainPanel, BorderLayout.NORTH);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));

		decayFunctionPanel = getCaller().getDecayFunction().getUI();
		//.createSimpleHorizontalPanel(lifetimeModeList, averageLifetimeTextField, 5, true);
		sourceTextArea = new JTextArea();
		sourceTextArea.setRows(5);
		sourceTextArea.setColumns(25);
		sourceTextArea.setLineWrap(true);
		sourceTextArea.setWrapStyleWord(true);
		sourceTextArea.setText(getCaller().getSourceInfo());
		sourceTextArea.setBorder(BorderFactory.createLineBorder(Color.BLACK));

		mainPanel.add(Box.createVerticalStrut(5));
		mainPanel.add(decayFunctionPanel);
		mainPanel.add(Box.createVerticalStrut(10));
		mainPanel.add(getSourceInfoPanel());
		mainPanel.add(Box.createVerticalStrut(5));
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		decayFunctionPanel.setEnabled(b);
	}

	@Override
	public void listenTo() {
		sourceTextArea.getDocument().addDocumentListener(getCaller());
	}

	@Override
	public void doNotListenToAnymore() {
		sourceTextArea.getDocument().removeDocumentListener(getCaller());
	}

	protected final JPanel getSourceInfoPanel() {
		JPanel panel = UIControlManager.createSimpleHorizontalPanel(EnhancedProcessorInternalDialog.MessageID.SourceLabel, sourceTextArea, 5, true);
		return panel;
	}
	
	/*
	 * Useless for this class (non-Javadoc)
	 * @see repicea.gui.Refreshable#refreshInterface()
	 */
	@Override
	public void refreshInterface() {}


}
