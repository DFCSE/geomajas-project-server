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

import org.geomajas.annotation.FutureApi;
import org.geomajas.global.UserImplemented;
import org.geomajas.puregwt.client.map.MapPresenter;

import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.MouseDownHandler;
import com.google.gwt.event.dom.client.MouseMoveHandler;
import com.google.gwt.event.dom.client.MouseOutHandler;
import com.google.gwt.event.dom.client.MouseOverHandler;
import com.google.gwt.event.dom.client.MouseUpHandler;
import com.google.gwt.event.dom.client.MouseWheelHandler;

/**
 * <p>
 * General interface for an event controller set on a map that catches different types of mouse events. Implementations
 * than decide how to react on these events. Since a <code>MapController</code> receives the original mouse events, it
 * can influence their behavior. As such only one <code>MapController</code> can be active at any one time on a
 * {@link MapPresenter}.
 * </p>
 * <p>
 * If you only need to react to certain events, within it having any influence on map behavior, take a look at the
 * {@link MapListener} interface. It is a definition for passive mouse event listeners on a map for which many can be
 * registered simultaneously.
 * </p>
 * 
 * @author Pieter De Graef
 * @since 1.0.0
 */
@UserImplemented
@FutureApi(allMethods = true)
public interface MapController extends MouseDownHandler, MouseUpHandler, MouseMoveHandler, MouseOutHandler,
		MouseOverHandler, MouseWheelHandler, DoubleClickHandler {

	/**
	 * Function executed when the controller instance is applied on the map. If something needs initializing, do it
	 * here.
	 * 
	 * @param presenter
	 *            The map presenter onto which this controller has been activated.
	 */
	void onActivate(MapPresenter presenter);

	/**
	 * Function executed when the controller instance is removed from the map. The perfect moment to clean up all the
	 * mess this controller made.
	 * 
	 * @param presenter
	 *            The map presenter onto which this controller has been deactivated.
	 */
	void onDeactivate(MapPresenter presenter);
}