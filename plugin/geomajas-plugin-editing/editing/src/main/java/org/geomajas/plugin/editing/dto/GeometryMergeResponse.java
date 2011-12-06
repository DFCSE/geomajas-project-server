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
package org.geomajas.plugin.editing.dto;

import org.geomajas.annotation.Api;
import org.geomajas.command.CommandResponse;
import org.geomajas.geometry.Geometry;

/**
 * Result object for {@link org.geomajas.command.geometry.MergePolygonCommand}.
 * 
 * @author Pieter De Graef
 * @since 1.0.0
 */
@Api
public class GeometryMergeResponse extends CommandResponse {

	private static final long serialVersionUID = 100L;

	private Geometry geometry;

	public GeometryMergeResponse() {
	}

	public Geometry getGeometry() {
		return geometry;
	}

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}
}