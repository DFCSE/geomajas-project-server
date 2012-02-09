/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2012 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.plugin.editing.puregwt.client;

import org.geomajas.plugin.editing.client.event.GeometryEditStartEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditStartHandler;
import org.geomajas.plugin.editing.client.event.GeometryEditStopEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditStopHandler;
import org.geomajas.plugin.editing.client.service.GeometryEditService;
import org.geomajas.plugin.editing.client.service.GeometryEditServiceImpl;
import org.geomajas.plugin.editing.client.snap.SnapService;
import org.geomajas.plugin.editing.puregwt.client.controller.EditGeometryBaseController;
import org.geomajas.plugin.editing.puregwt.client.gfx.GeometryRenderer;
import org.geomajas.puregwt.client.controller.MapController;
import org.geomajas.puregwt.client.map.MapPresenter;

/**
 * Central editor for geometries on the map.
 * 
 * @author Pieter De Graef
 */
public class GeometryEditor implements GeometryEditStartHandler, GeometryEditStopHandler {

	private final MapPresenter mapPresenter;

	private final GeometryRenderer renderer;

	private final GeometryEditService editService;

	private final SnapService snappingService;

	private final EditGeometryBaseController baseController;

	private MapController previousController;

	// ------------------------------------------------------------------------
	// Constructors:
	// ------------------------------------------------------------------------

	public GeometryEditor(MapPresenter mapPresenter) {
		this.mapPresenter = mapPresenter;

		// Initialize the editing service:
		editService = new GeometryEditServiceImpl();
		editService.addGeometryEditStartHandler(this);
		editService.addGeometryEditStopHandler(this);

		// Initialize the rest:
		snappingService = new SnapService();
		baseController = new EditGeometryBaseController(editService, snappingService);
		renderer = new GeometryRenderer(mapPresenter, editService);
	}

	// ------------------------------------------------------------------------
	// GeometryEditStartHandler implementation:
	// ------------------------------------------------------------------------

	public void onGeometryEditStart(GeometryEditStartEvent event) {
		// Store the current controller:
		previousController = mapPresenter.getMapController();
		mapPresenter.setMapController(baseController);
	}

	// ------------------------------------------------------------------------
	// GeometryEditStopHandler implementation:
	// ------------------------------------------------------------------------

	public void onGeometryEditStop(GeometryEditStopEvent event) {
		// Restore the original map controller:
		mapPresenter.setMapController(previousController);
	}

	// ------------------------------------------------------------------------
	// Getters and setters:
	// ------------------------------------------------------------------------

	public MapPresenter getMapPresenter() {
		return mapPresenter;
	}

	public GeometryRenderer getRenderer() {
		return renderer;
	}

	public GeometryEditService getEditService() {
		return editService;
	}

	public SnapService getSnappingService() {
		return snappingService;
	}
}