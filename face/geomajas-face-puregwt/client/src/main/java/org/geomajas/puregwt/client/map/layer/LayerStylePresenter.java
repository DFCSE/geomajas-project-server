/*
 * This is part of Geomajas, a GIS framework, http://www.geomajas.org/.
 *
 * Copyright 2008-2013 Geosparc nv, http://www.geosparc.com/, Belgium.
 *
 * The program is available in open source according to the GNU Affero
 * General Public License. All contributions in this program are covered
 * by the Geomajas Contributors License Agreement. For full licensing
 * details, see LICENSE.txt in the project root.
 */

package org.geomajas.puregwt.client.map.layer;

import org.geomajas.annotation.FutureApi;

import com.google.gwt.user.client.ui.IsWidget;

/**
 * Presenter for a layer style. It points to an image URL and a label that is associated with that image. This
 * combination can for example be used within a legend widget.
 * 
 * @author Pieter De Graef
 * @since 1.0.0
 */
@FutureApi(allMethods = true)
public interface LayerStylePresenter extends IsWidget {

	int getIndex();
}