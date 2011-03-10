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

package org.geomajas.puregwt.client.map.controller;

import org.geomajas.puregwt.client.map.ZoomOption;
import org.geomajas.puregwt.client.spatial.Bbox;

/**
 * Map controller that lets the user drag a rectangle on the map, after which it zooms to that rectangle.
 * 
 * @author Pieter De Graef
 */
public class ZoomToRectangleController extends AbstractRectangleController {

	/**
	 * Effectively zoom to the bounds that the user drew on the map.
	 * 
	 * @param worldBounds
	 *            The result of the user's dragging on the map, expressed in world space.
	 */
	protected void execute(Bbox worldBounds) {
		mapPresenter.getMapModel().getViewPort().applyBounds(worldBounds, ZoomOption.LEVEL_CHANGE);
	}
}