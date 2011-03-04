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

package org.geomajas.puregwt.client;

import org.geomajas.puregwt.client.command.CommandService;
import org.geomajas.puregwt.client.spatial.GeometryFactory;

import com.google.gwt.inject.client.GinModules;
import com.google.gwt.inject.client.Ginjector;

/**
 * Ginjector specific for the Geomajas pure GWT client.
 * 
 * @author Pieter De Graef
 */
@GinModules(GeomajasGinModule.class)
public interface GeomajasGinjector extends Ginjector {

	CommandService getCommandService();

	GeometryFactory getGeometryFactory();
}