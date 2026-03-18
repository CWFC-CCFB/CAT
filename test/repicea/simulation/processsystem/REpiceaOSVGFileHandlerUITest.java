/*
 * This file is part of the repicea library.
 *
 * Copyright (C) 2009-2021 Mathieu Fortin for Rouge-Epicea
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
package repicea.simulation.processsystem;

import java.awt.FlowLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

import org.junit.Assert;
import org.junit.Test;

import repicea.gui.UIControlManager.CommonControlID;
import repicea.gui.icons.IconFactory;

public class REpiceaOSVGFileHandlerUITest {

	static {
		System.out.println("RUNNING " + REpiceaOSVGFileHandlerUITest.class.getName());
	}

	@Test
	public void testSVGExportOfPngImage() throws Exception {
		String filename = System.getProperty("java.io.tmpdir") + "testButton.svg";
		File f = new File(filename);
		if (f.exists()) f.delete();
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button = new JButton();
		button.setIcon(IconFactory.getIcon(CommonControlID.Open));
		panel.add(button);
		JDialog dialog = new JDialog();
		dialog.getContentPane().add(panel);
		dialog.pack();
		dialog.setVisible(true);
		try {
			REpiceaOSVGFileHandlerUI.saveAsSVN(dialog.getContentPane(), filename);
			Assert.assertTrue("Testing if the file has been created", f.exists());
		} catch (Exception e) {
			throw e;
		} finally {
			dialog.setVisible(false);
		}
	}

	@Test
	public void testSVGExportWithTextAndEncoding() throws Exception {
		String filename = System.getProperty("java.io.tmpdir") + "testButton2.svg";
		File f = new File(filename);
		if (f.exists()) f.delete();
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button = new JButton("\u00E9 \u00EA");
		button.setIcon(IconFactory.getIcon(CommonControlID.Open));
		panel.add(button);
		JDialog dialog = new JDialog();
		dialog.getContentPane().add(panel);
		dialog.pack();
		dialog.setVisible(true);
		try {
			REpiceaOSVGFileHandlerUI.saveAsSVN(dialog.getContentPane(), filename);
			Assert.assertTrue("Testing if the file has been created", f.exists());
		} catch (Exception e) {
			throw e;
		} finally {
			dialog.setVisible(false);
		}
	}

	
}
