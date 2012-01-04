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

package org.geomajas.puregwt.client.map.render.event;

import org.geomajas.annotation.Api;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that is fired when a scale level has been rendered.
 * 
 * @author Pieter De Graef
 * @since 1.0.0
 */
@Api(allMethods = true)
public class ScaleLevelRenderedEvent extends GwtEvent<ScaleLevelRenderedHandler> {

	private final double scale;

	public ScaleLevelRenderedEvent(double scale) {
		this.scale = scale;
	}

	@Override
	public Type<ScaleLevelRenderedHandler> getAssociatedType() {
		return ScaleLevelRenderedHandler.TYPE;
	}

	@Override
	protected void dispatch(ScaleLevelRenderedHandler handler) {
		handler.onScaleLevelRendered(this);
	}

	/**
	 * Get the scale level that was just rendered.
	 * 
	 * @return The scale level that was just rendered.
	 */
	public double getScale() {
		return scale;
	}
}