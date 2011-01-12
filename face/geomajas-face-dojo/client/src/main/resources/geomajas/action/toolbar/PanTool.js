dojo.provide("geomajas.action.toolbar.PanTool");
/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2011 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */
dojo.require("geomajas.action.ToolbarTool");
dojo.require("geomajas.controller.PanController");

dojo.declare("PanTool", ToolbarTool, {

	/**
	 * @fileoverview Activate/Deactivate pan mode (DynamicToolbar).
	 * @class Tool for panning. Uses a {@link PanController} for
	 * mouse-event handling.
	 * @author Pieter De Graef
	 *
	 * @constructor
	 * @extends ToolbarTool
	 * @param id Unique identifier
	 * @param mapWidget MapWidget this tool has effect on.
	 */
	constructor : function (id, mapWidget, hideLabelsOnDrag) {
		/** Unique identifier */
		this.id = id;

		/** The image for this tool's button. */
		this.image = "panIcon";

		/** The tooltip reference. */
		this.tooltip = this.tooltipLocale.PanTool;
		
		/** Reference to the MapWidget. */
		this.mapWidget = mapWidget;

		/** MouseListener that defines the actions for mouse-events on the map */
		this.controller = new PanController (mapWidget, hideLabelsOnDrag);
	},

	/**
	 * Add the PanController to the MapWidget.
	 * @param event Standard browser mouse-event.
	 */
	onSelect : function (event) {
		this.selected = true;
		this.mapWidget.setController(this.controller);
		this.mapWidget.setCursor("move");
	},

	/**
	 * Remove the PanController from the MapWidget.
	 * @param event Standard browser mouse-event.
	 */
	onDeSelect : function (event) {
		this.selected = false;
		this.mapWidget.setCursor("default");
		this.mapWidget.setController(null);
	}

});
