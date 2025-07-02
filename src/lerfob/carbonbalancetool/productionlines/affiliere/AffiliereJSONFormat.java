/*
 * This file is part of the CAT library.
 *
 * Copyright (C) 2024 His Majesty the King in Right of Canada
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

/**
 * A class with the information on the the JSON format of Affiliere.
 * @author Mathieu Fortin - April 2024
 */
public class AffiliereJSONFormat {

	// First level properties
	public static final String L1_VERSION_PROPERTY = "version";
	public static final String L1_NODES_PROPERTY = "nodes";
	public static final String L1_LINKS_PROPERTY = "links";
	public static final String L1_NODETAGS_PROPERTY = "nodeTags";

	public static final String ACTIVATED = "activated";
	
	// Second level properties for nodes
	public static final String NODE_NAME_PROPERTY = "name";
	public static final String NODE_X_COORD_PROPERTY = "x";
	public static final String NODE_Y_COORD_PROPERTY = "y";
	public static final String NODE_IDNODE_PROPERTY = "idNode";
	public static final String NODE_TAGS_PROPERTY = "tags";
	public static final String NODE_DIMENSIONS_PROPERTY = "dimensions";

	// Third level properties for nodes/tags
	public static final String NODE_TAGS_NODETYPE_PROPERTY = "Type de noeud";
	public static final String NODE_TAGS_WOODTYPE_PROPERTY = "Type de bois";

	public static final String NODE_END_OF_LIFE_PREFIX = "Collect";
	
	// Second level properties for links
	public static final String LINK_IDLINK_PROPERTY = "idLink";
	public static final String LINK_IDSOURCE_PROPERTY = "idSource";
	public static final String LINK_IDTARGET_PROPERTY = "idTarget";
	public static final String LINK_LINKTYPE_PROPERTY = "linkType";
	public static final String LINK_VALUE_PROPERTY = "value";
	public static final String LINK_STYLE_PROPERTY = "style";
	
	// Third level properties for link/values
	public static final String LINK_VALUE_ISPERCENT_PROPERTY = "is_percent";
	public static final String LINK_VALUE_PERCENT_PROPERTY = "percent";

	public static final String SELECTED = "selected";

	public static final String LEVEL = "niveau";

}
