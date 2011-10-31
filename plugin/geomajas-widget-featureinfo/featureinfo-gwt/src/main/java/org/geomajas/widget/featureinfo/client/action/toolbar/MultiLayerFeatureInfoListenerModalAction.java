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
package org.geomajas.widget.featureinfo.client.action.toolbar;

import org.geomajas.gwt.client.action.ConfigurableAction;
import org.geomajas.gwt.client.action.ToolbarModalAction;
import org.geomajas.gwt.client.widget.MapWidget;
import org.geomajas.widget.featureinfo.client.FeatureInfoMessages;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.events.ClickEvent;

/**
 * Tool which shows a list of nearby features. By clicking on a feature in the list details of that feature will be
 * shown.
 * 
 * @author An Buyle
 * @author Oliver May
 */
public class MultiLayerFeatureInfoListenerModalAction extends ToolbarModalAction implements ConfigurableAction {

	private MapWidget mapWidget;

	private MultiLayerFeatureInfoListener listener;

	/**
	 * Number of pixels that describes the tolerance allowed when searching nearby features.
	 */
	private int pixelTolerance = 10; /* default value */

	private FeatureInfoMessages messages = GWT.create(FeatureInfoMessages.class);

	/**
	 * Constructor.
	 * @param mapWidget the mapwidget where this action should work on
	 */
	public MultiLayerFeatureInfoListenerModalAction(MapWidget mapWidget) {
		super("[ISOMORPHIC]/geomajas/osgeo/info.png", null);
		setTitle(messages.nearbyFeaturesModalActionTitle());
		setTooltip(messages.nearbyFeaturesModalActionTooltip());
		this.mapWidget = mapWidget;
		listener = new MultiLayerFeatureInfoListener(mapWidget, pixelTolerance);
	}

	/* (non-Javadoc)
	 * @see org.geomajas.gwt.client.action.ConfigurableAction#configure(java.lang.String, java.lang.String)
	 */
	public void configure(String key, String value) {
		if ("pixelTolerance".equals(key)) {
			setPixelTolerance(Integer.parseInt(value));
		}
		
	}

	/* (non-Javadoc)
	 * @see org.geomajas.gwt.client.action.ToolbarModalAction#onSelect(com.smartgwt.client.widgets.events.ClickEvent)
	 */
	@Override
	public void onSelect(ClickEvent event) {
		mapWidget.addListener(listener);
	}

	/* (non-Javadoc)
	 * @see org.geomajas.gwt.client.action.ToolbarModalAction#onDeselect(com.smartgwt.client.widgets.events.ClickEvent)
	 */
	@Override
	public void onDeselect(ClickEvent event) {
		mapWidget.removeListener(listener);
	}

	/**
	 * Set the tolerance in pixels, when clicking on the map, the features in this distance around the mouse click will 
	 * be returned.
	 * @param pixelTolerance distance in pixels
	 */
	public void setPixelTolerance(int pixelTolerance) {
		this.pixelTolerance = pixelTolerance;
		listener.setPixelTolerance(pixelTolerance);
	}

	/**
	 * @return the current pixel tolerance
	 */
	public int getPixelTolerance() {
		return pixelTolerance;
	}
}