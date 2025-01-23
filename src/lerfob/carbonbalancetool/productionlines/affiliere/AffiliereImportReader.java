/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2023-25 His Majesty the King in Right of Canada
 * Author: Mathieu Fortin, Canadian Forest Service, Canadian Wood Fibre Centre
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
package lerfob.carbonbalancetool.productionlines.affiliere;

import java.awt.Container;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lerfob.carbonbalancetool.productionlines.AbstractProcessor;
import lerfob.carbonbalancetool.productionlines.ProductionLineProcessor;
import py4j.GatewayServer;
import repicea.gui.REpiceaShowableUIWithParent;
import repicea.io.REpiceaFileFilter;
import repicea.simulation.processsystem.Processor;

/**
 * The AffiliereJSONReader class reads a JSON file from AFFILIERE and 
 * converts it into a flux configuration file for CAT.
 * @author Mathieu Fortin - October 2023
 */
public class AffiliereImportReader implements REpiceaShowableUIWithParent {

	/**
	 * An interface to provide access to methods on Python's end.
	 */
	public interface SankeyProxy {

		/**
		 * Read the content of an excel file and convert it
		 * into Sankey objects on Python's end.<p>
		 * This method returns tuples in Python. The tuple is converted into string.
		 * @param filename the path to the Excel file.
		 * @return a String 
		 */
		public String readFromExcel(String filename);

		/**
		 * Write Sankey objects to an Excel file.
		 * @param filename the output filename
		 * @param mode either "a" (append) or "w" (write)
		 * @return null
		 */
		public String writeToExcel(String filename, String mode);

		public boolean clear();

		public String requestShutdown();

	}



	private static final List<String> NodeTypesToBeDiscarded = new ArrayList<String>();
	static {
		NodeTypesToBeDiscarded.add("echange");		// we do not want import / export processors MF2024-03-01
	}

	public enum AFFiliereUnit {
		VolumeM3("1000m3"), 
		DryBiomassMg("1000t");

		final String suffix;

		AFFiliereUnit(String suffix) {
			this.suffix = suffix;
		}
	}

	public enum AFFiliereStudy {
		AFFiliere("AF Fili\u00E8res"),
		Carbone4("Carbone 4"),
		BACCFIRE("BACCFIRE"),
		MFAB("MFAB");

		final String prefix;

		AFFiliereStudy(String prefix) {
			this.prefix = prefix;
		}
	}

	/**
	 * A transition class to AbstractProcessorLinkLine in the processor manager.
	 */
	private static class FutureLink {
		final Processor fatherProcessor;
		final Processor childProcessor;
		final double value;
		final boolean isPercent;
		final boolean mightBeEndOfLifeLink;

		FutureLink(Processor fatherProcessor, Processor childProcessor, double value, boolean isPercent, boolean mightBeEndOfLifeLink) {
			this.fatherProcessor = fatherProcessor;
			this.childProcessor = childProcessor;
			this.value = value;
			this.isPercent = isPercent;
			this.mightBeEndOfLifeLink = mightBeEndOfLifeLink;
		}
	}
	
	@SuppressWarnings("serial")
	static class TagLevels extends LinkedHashMap<String, Boolean> {
		
		boolean isExclusive;
		
	}
	protected static boolean enableGUI = true;
	protected static int OFFSET = 150;

	private transient AffiliereImportReaderDialog guiInterface;
	
	protected final LinkedHashMap<?,?> mappedJSON;
	protected final Map<String, Processor> processors;
	protected final List<Processor> endProductProcessors;
	protected final LinkedHashMap<String, Processor> potentialEOLProcessors;
	protected final AFFiliereStudy study;
	protected final AFFiliereUnit unit;
	protected final Map<String, TagLevels> nodeTags;
//	protected final String endOfLifePrefix;

	private static GatewayServer Server;
	

	/**
	 * General constructor.
	 * @param file the File instance to be read.
	 * @param study an AFFiliereStudy enum
	 * @param unit an AFFiliereUnit enum
	 * @param parent the parent window which can be null
	 * @throws AffiliereException if the file cannot be found or read.
	 */
	@SuppressWarnings("unchecked")
	public AffiliereImportReader(File file, AFFiliereStudy study, AFFiliereUnit unit, Window parent) throws AffiliereException {
		this.study = study == null ? AFFiliereStudy.AFFiliere : study;
		this.unit = unit == null ? AFFiliereUnit.DryBiomassMg : unit;
		if (REpiceaFileFilter.JSON.accept(file)) {
			mappedJSON = AffiliereImportReader.getJSONRepresentation(file);
		} else if (REpiceaFileFilter.XLSX.accept(file)) {
			mappedJSON = AffiliereImportReader.getJSONRepresentationThroughoutOpenSankey(file);
		} else {
			throw new InvalidParameterException("The extension of the input file should be either json or xlsx!");
		}
		nodeTags = formatNodeTagsMap((LinkedHashMap<String,?>) mappedJSON.get(AffiliereJSONFormat.L1_NODETAGS_PROPERTY));
		processors = new HashMap<String, Processor>();
		endProductProcessors = new ArrayList<Processor>();
		potentialEOLProcessors = new LinkedHashMap<String, Processor>();
		screenNodeMap();
		if (enableGUI) {
			showUI(parent);
			if (isCancelled()) {
				return;
			}
		}
		LinkedHashMap<String, LinkedHashMap<String, Object>> screenedLinkJSONMap = screenLinkMap((LinkedHashMap<?,?>) mappedJSON.get(AffiliereJSONFormat.L1_LINKS_PROPERTY));

		List<Processor> childProcessors = new ArrayList<Processor>();

		Map<Processor, List<FutureLink>> linkMap = constructLinkMap(screenedLinkJSONMap, childProcessors);
		addPotentialEndOfLifeLinkToLinkMap(linkMap, potentialEOLProcessors, childProcessors);
		setLinks(linkMap);
	}
	
	
	public boolean isCancelled() {
		return guiInterface != null && guiInterface.isCancelled;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Map<String, TagLevels> formatNodeTagsMap(LinkedHashMap<String, ?> incomingNodeTagMap) throws AffiliereException {
		Map<String, TagLevels> outputMap = new LinkedHashMap<String, TagLevels>();
		for (String tagName : incomingNodeTagMap.keySet()) {
			outputMap.put(tagName, new TagLevels());
			Map<String, ?> innerIncomingMap = (Map) ((Map) incomingNodeTagMap.get(tagName)).get(AffiliereJSONFormat.NODE_TAGS_PROPERTY);
			for (String level : innerIncomingMap.keySet()) {
				String boolStr = ((Map) innerIncomingMap.get(level)).get(AffiliereJSONFormat.SELECTED).toString(); 
				outputMap.get(tagName).put(level, Boolean.parseBoolean(boolStr));
			}
		}
		for (String tagName : outputMap.keySet()) {
			if (tagName.toLowerCase().startsWith(AffiliereJSONFormat.LEVEL)) {
				TagLevels tagLevels = outputMap.get(tagName);
				tagLevels.isExclusive = true;
				int nbTrueCases = 0;
				for (Boolean b : tagLevels.values()) {
					if (b) {
						nbTrueCases++;
					}
				}
				if (nbTrueCases != 1) {
					throw new AffiliereException("This tag should have a single level enabled: " + tagName);
				}
			}
		}
		return outputMap;
	}

	private void setLinks(Map<Processor, List<FutureLink>> linkMap) {
		for (List<FutureLink> futureLinks : linkMap.values()) {
			double sumValues = 0d;
			Boolean lastIsPercent = null;
			for (FutureLink fLink : futureLinks) {
				if (lastIsPercent == null) {
					lastIsPercent = fLink.isPercent;
				} else {
					if (!lastIsPercent.equals(fLink.isPercent)) { // check if the isPercent member is consistent across the links
						throw new InvalidParameterException("It seems the values of some links are mix of percentage and absolute values!");
					}
				}
				sumValues += fLink.value;
			}

			for (FutureLink fLink : futureLinks) {
				if (futureLinks.size() == 1 &&
						fLink.mightBeEndOfLifeLink && 
						fLink.fatherProcessor instanceof ProductionLineProcessor) {
					((ProductionLineProcessor) fLink.fatherProcessor).setDisposedToProcessor(fLink.childProcessor);  // add end of life link here
				} else {
					fLink.fatherProcessor.addSubProcessor(fLink.childProcessor);
					fLink.fatherProcessor.getSubProcessorIntakes().put(fLink.childProcessor, 
							lastIsPercent ? 
									fLink.value : 
										fLink.value / sumValues * 100);
				}
			}
		}

	}

	private void addPotentialEndOfLifeLinkToLinkMap(Map<Processor, List<FutureLink>> linkMap, 
			LinkedHashMap<String, Processor> potentialEOLProcessors,
			List<Processor> childProcessors) {
		// trying to link processor with end of life processor
		for (String name : potentialEOLProcessors.keySet()) {
			Processor fatherProcessor = null;
			Processor childProcessor = potentialEOLProcessors.get(name);
			if (!childProcessors.contains(childProcessor)) {
				for (Processor p : processors.values()) {
					if (!p.equals(childProcessor)) {
						if (name.contains(p.getName())) {
							fatherProcessor = p;
							break;
						}
					}
				}

				if (fatherProcessor != null && !linkMap.containsKey(fatherProcessor)) { // if the father processor is already in the linkMap object, this means there are some links already and it cannot be EOL link
					linkMap.put(fatherProcessor, new ArrayList<FutureLink>());
					linkMap.get(fatherProcessor).add(new FutureLink(fatherProcessor, childProcessor, 100, false, true));
				}
			}
		}
	}

	private Map<Processor, List<FutureLink>> constructLinkMap(LinkedHashMap<String, LinkedHashMap<String, Object>> screenedLinkJSONMap,
			List<Processor> childProcessors) {
		Map<Processor, List<FutureLink>> linkMap = new HashMap<Processor, List<FutureLink>>();
		for (LinkedHashMap<String, Object> innerLinkMap : screenedLinkJSONMap.values()) {
			String idSource = (String) innerLinkMap.get(AffiliereJSONFormat.LINK_IDSOURCE_PROPERTY);
			Processor fatherProcessor = processors.get(idSource);
			String idTarget = (String) innerLinkMap.get(AffiliereJSONFormat.LINK_IDTARGET_PROPERTY);
			Processor childProcessor = processors.get(idTarget);

			LinkedHashMap<?, ?> valueMap = (LinkedHashMap<?,?>) innerLinkMap.get(AffiliereJSONFormat.LINK_VALUE_PROPERTY); 
			String key = study.prefix.trim() + " " + unit.suffix.trim();
			LinkedHashMap<?, ?> studySpecificValueMap = (LinkedHashMap<?,?>) valueMap.get(key);

			boolean isPercent = false;
			double value;
			if (studySpecificValueMap.containsKey(AffiliereJSONFormat.LINK_VALUE_PERCENT_PROPERTY)) {
				value = ((Number) studySpecificValueMap.get(AffiliereJSONFormat.LINK_VALUE_PERCENT_PROPERTY)).doubleValue();
				isPercent = true;
			} else {
				Object o = studySpecificValueMap.get(AffiliereJSONFormat.LINK_VALUE_PROPERTY);
				if (o instanceof String && ((String) o).isEmpty()) {
					value = 0d;
				} else {
					value = ((Number) studySpecificValueMap.get(AffiliereJSONFormat.LINK_VALUE_PROPERTY)).doubleValue(); 
				}
			}
			if (!linkMap.containsKey(fatherProcessor)) {
				linkMap.put(fatherProcessor, new ArrayList<FutureLink>());
			}
			linkMap.get(fatherProcessor).add(new FutureLink(fatherProcessor, childProcessor, value, isPercent, false)); // not an EOL link
			if (!childProcessors.contains(childProcessor)) {
				childProcessors.add(childProcessor);
			}
		}
		return linkMap;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private boolean matchesTags(Map<String, Object> tagMapFromNode) {
		for (String tag : tagMapFromNode.keySet()) {
			if (!nodeTags.containsKey(tag)) {
				throw new UnsupportedOperationException("This tag cannot be found into the nodeTags map: " + tag); // Should this throw an Exception MF20241009
			} else {
				if (!tag.equals("Diagramme") && !tag.equals("Primaire")) {
					Map<String, Boolean> levelMap = nodeTags.get(tag);
					List array  = (List) tagMapFromNode.get(tag);
					boolean canBeProcessed = checkIfCanBeProcessed(tag, array);
					if (!canBeProcessed) {
						return false;
					}
					if (!isThereAtLeastOneTagEnabled(array, levelMap)) {
						return false;
					}
				}
			}
		}
		return true;
	}


	@SuppressWarnings("rawtypes")
	private boolean isEndOfLifeNode(LinkedHashMap<String, Object> oMap) {
		if (oMap.containsKey(AffiliereJSONFormat.NODE_TAGS_WOODTYPE_PROPERTY)) {
			List woodTypes = (List) oMap.get(AffiliereJSONFormat.NODE_TAGS_WOODTYPE_PROPERTY);
			return woodTypes.contains("Fin de vie");
		}
		return false;
	}

	@SuppressWarnings("rawtypes")
	private boolean checkIfCanBeProcessed(String tag, List values) {
		if (tag.equals(AffiliereJSONFormat.NODE_TAGS_NODETYPE_PROPERTY) && values.contains("echange")) { // we remove Import/Export nodes
			return false;
		} else if (tag.equals(AffiliereJSONFormat.NODE_TAGS_WOODTYPE_PROPERTY) && values.contains("Sylviculture")) {
			return false;
		}
		return true;
	}

	private boolean isThereAtLeastOneTagEnabled(List<String> tagArrayFromNode, Map<String, Boolean> levelMap) {
		for (String tagValue : tagArrayFromNode) {
			if (levelMap.containsKey(tagValue) && levelMap.get(tagValue)) {
				return true;
			}
		}
		return false;  // activated but no selection
	}

	@SuppressWarnings("unchecked")
	protected void screenNodeMap() {
		potentialEOLProcessors.clear();
		processors.clear();
		
		LinkedHashMap<?,?> nodeMap = (LinkedHashMap<?,?>) mappedJSON.get(AffiliereJSONFormat.L1_NODES_PROPERTY);
		
		for (Object o : nodeMap.values()) {
			LinkedHashMap<String, Object> oMap = (LinkedHashMap<String, Object>) o;
			LinkedHashMap<String, Object> tagMap = (LinkedHashMap<String, Object>) oMap.get(AffiliereJSONFormat.NODE_TAGS_PROPERTY);
			boolean isSelected = matchesTags(tagMap);
			if (isSelected) {
				String id = oMap.get(AffiliereJSONFormat.NODE_IDNODE_PROPERTY).toString();
				Processor p = AbstractProcessor.createProcessor(oMap);
				if (isEndOfLifeNode(tagMap)) {
					potentialEOLProcessors.put(oMap.get("name").toString(), p);
				}
				processors.put(id, p);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private LinkedHashMap<String, LinkedHashMap<String, Object>> screenLinkMap(LinkedHashMap<?,?> linkMap) {
		LinkedHashMap<String , LinkedHashMap<String, Object>> screenedMap = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
		for (Object k : linkMap.keySet()) {
			String key = (String) k;
			LinkedHashMap<String, Object> innerMap = (LinkedHashMap<String, Object>) linkMap.get(key);
			String idSource = (String) innerMap.get(AffiliereJSONFormat.LINK_IDSOURCE_PROPERTY);
			String idTarget = (String) innerMap.get(AffiliereJSONFormat.LINK_IDTARGET_PROPERTY);
			if (processors.containsKey(idSource) && processors.containsKey(idTarget)) {
				screenedMap.put(key, innerMap);
			}
		}
		return screenedMap;
	}



	protected static String getProperFilenameForPython(String originalFilename) {
		return originalFilename.startsWith("/") ? originalFilename.substring(1) : originalFilename;
	}


	private synchronized static LinkedHashMap<?,?> getJSONRepresentationThroughoutOpenSankey(File f) throws AffiliereException {
		if (Server == null) {
			GatewayServer.turnLoggingOff();
			Server = new GatewayServer();
			Server.start();
		}
		ObjectMapper mapper = new ObjectMapper();
		SankeyProxy sankeyProxy = (SankeyProxy) Server.getPythonServerEntryPoint(new Class[] { SankeyProxy.class });
		String path = getProperFilenameForPython(f.getAbsolutePath());
		long initialTime = System.currentTimeMillis();
		String message = sankeyProxy.readFromExcel(path);
		if (message.toLowerCase().contains("error")) {
			throw new AffiliereException(message);
		} else {
			System.out.println("Reading file took " + (System.currentTimeMillis() - initialTime));
			try {
				LinkedHashMap<?,?> oMap = mapper.readValue(message, LinkedHashMap.class);
				sankeyProxy.clear();
				return oMap;
			} catch (JsonMappingException e1) {
				throw new AffiliereException(e1.getMessage());
			} catch (JsonProcessingException e2) {
				throw new AffiliereException(e2.getMessage());
			}
		}
	}

	private synchronized static LinkedHashMap<?,?> getJSONRepresentation(File f) throws AffiliereException {
		try {
			ObjectMapper mapper = new ObjectMapper();
			LinkedHashMap<?,?> o = mapper.readValue(new FileInputStream(f), LinkedHashMap.class);
			return o;
		} catch (IOException e) {
			throw new AffiliereException(e.getMessage());
		} 
	}

	//	static LinkedHashMap<?,?> readJSONMap(InputStream is) {
	//		JsonReader reader = new JsonReader(is);
	//		LinkedHashMap<?,?> mappedJSON = (LinkedHashMap<?,?>) reader.readObject();
	//		reader.close();
	//		return mappedJSON;
	//	}

	//	private void addToListIfRelevant(Processor p, LinkedHashMap<String, Object> oMap, String key, String expression, List<Processor> outputList) {
	//		if (oMap.containsKey(key) ) {
	//			Object o = oMap.get(key);
	//			String str = ((Object[]) o)[0].toString();
	//			if (str.startsWith(expression)) {
	//				outputList.add(p);
	//			}
	//		}
	//	}
	//	
	//	private static boolean containsUndesiredNodeTypeTag(Object[] tags) {
	//		for (Object o : tags) {
	//			if (NodeTypesToBeDiscarded.contains(o.toString()))
	//				return true;
	//		}
	//		return false;
	//	}


	/**
	 * Provide a map of the processors read by this AffiliereJSONReader instance.<p>
	 * Map keys are the names of the processors whereas the values are the ProductionLineProcessors
	 * instances.
	 * @return a Map
	 */
	public Map<String, Processor> getProcessors() {
		return processors;
	}

	@Override
	public AffiliereImportReaderDialog getUI(Container parent) {
		if (guiInterface == null) {
			guiInterface = new AffiliereImportReaderDialog(this, (Window) parent);
		}
		return guiInterface;
	}

	@Override
	public boolean isVisible() {
		return guiInterface != null && guiInterface.isVisible();
	}

	@Override
	public void showUI(Window parent) {
		if (!isVisible()) {
			getUI(parent).setVisible(true);
		}
	}
	
}
