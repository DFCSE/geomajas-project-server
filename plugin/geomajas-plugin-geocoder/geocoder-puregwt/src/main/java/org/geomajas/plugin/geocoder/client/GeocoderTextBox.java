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

package org.geomajas.plugin.geocoder.client;

import org.geomajas.plugin.geocoder.client.event.SelectAlternativeEvent;
import org.geomajas.plugin.geocoder.client.event.SelectAlternativeHandler;
import org.geomajas.plugin.geocoder.client.event.SelectLocationEvent;
import org.geomajas.plugin.geocoder.client.event.SelectLocationHandler;
import org.geomajas.puregwt.client.map.MapPresenter;

import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.TextBox;

/**
 * Geocoder widget that navigates to a location when "enter" is pressed.
 * 
 * @author Pieter De Graef
 */
public class GeocoderTextBox extends TextBox {

	private GeocoderPresenter geocoderPresenter;

	public GeocoderTextBox(final MapPresenter mapPresenter) {
		geocoderPresenter = new GeocoderPresenter(mapPresenter, this);

		addKeyPressHandler(new KeyPressHandler() {

			public void onKeyPress(KeyPressEvent event) {
				if (event.getNativeEvent().getKeyCode() == KeyCodes.KEY_ENTER) {
					goToLocation();
				}
			}
		});
	}

	public void goToLocation() {
		geocoderPresenter.goToLocation(getValue());
	}

	/**
	 * Get the regular expression which is used to select which geocoder services to use.
	 *
	 * @return geocoder selection regular expression
	 */
	public String getServicePattern() {
		return geocoderPresenter.getServicePattern();
	}

	/**
	 * Set the regular expression which is used to select which geocoder services to use.
	 *
	 * @param servicePattern geocoder selection regular expression
	 */
	public void setServicePattern(String servicePattern) {
		geocoderPresenter.setServicePattern(servicePattern);
	}

	/**
	 * Set the select alternative handler.
	 * <p/>
	 * There can only be one handler, the default displays the alternatives in a window on the map widget.
	 *
	 * @param handler select alternative handler
	 * @return handler registration.
	 */
	public HandlerRegistration setSelectAlternativeHandler(SelectAlternativeHandler handler) {
		return geocoderPresenter.setSelectAlternativeHandler(handler);
	}

	/**
	 * Set the select location handler.
	 * <p/>
	 * There can only be one handler, the default zooms the map widget to the selected location.
	 *
	 * @param handler select location handler
	 * @return handler registration.
	 */
	public HandlerRegistration setSelectLocationHandler(SelectLocationHandler handler) {
		return geocoderPresenter.setSelectLocationHandler(handler);
	}

	@Override
	public void fireEvent(GwtEvent<?> event) {
		if (event instanceof SelectLocationEvent || event instanceof SelectAlternativeEvent) {
			geocoderPresenter.fireEvent(event);
		} else {
			super.fireEvent(event);
		}
	}
}
