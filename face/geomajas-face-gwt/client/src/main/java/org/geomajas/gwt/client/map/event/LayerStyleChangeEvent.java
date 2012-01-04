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
package org.geomajas.gwt.client.map.event;

import org.geomajas.annotation.Api;
import org.geomajas.gwt.client.map.layer.Layer;

import com.google.gwt.event.shared.GwtEvent;

/**
 * Event that reports changes in layer style.
 * 
 * @author Pieter De Graef
 * @since 1.8.0
 */
@Api(allMethods = true)
public class LayerStyleChangeEvent extends GwtEvent<LayerStyleChangedHandler> {

	private Layer<?> layer;

	public LayerStyleChangeEvent(Layer<?> layer) {
		this.layer = layer;
	}

	public Layer<?> getLayer() {
		return layer;
	}

	@Override
	public Type<LayerStyleChangedHandler> getAssociatedType() {
		return LayerStyleChangedHandler.TYPE;
	}

	@Override
	protected void dispatch(LayerStyleChangedHandler layerStyleChangedHandler) {
		layerStyleChangedHandler.onLayerStyleChange(this);
	}
}