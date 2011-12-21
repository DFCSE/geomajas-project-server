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
package org.geomajas.plugin.editing.server.command;

import java.util.ArrayList;
import java.util.List;

import org.geomajas.annotation.Api;
import org.geomajas.command.Command;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.plugin.editing.dto.GeometrySplitRequest;
import org.geomajas.plugin.editing.dto.GeometrySplitResponse;
import org.geomajas.service.DtoConverterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Command that splits up a given geometry using a splitting line.<br/>
 * TODO find a better way to split geometries....
 * 
 * @author Pieter De Graef
 * @since 1.0.0
 */
@Component
@Api(allMethods = true)
public class GeometrySplitCommand implements Command<GeometrySplitRequest, GeometrySplitResponse> {

	public static final double DELTA = 0.00001;

	@Autowired
	private DtoConverterService converter;

	public GeometrySplitResponse getEmptyCommandResponse() {
		return new GeometrySplitResponse();
	}

	public void execute(GeometrySplitRequest request, GeometrySplitResponse response) throws Exception {
		// Check the parameters:
		if (null == request.getGeometry()) {
			throw new GeomajasException(ExceptionCode.PARAMETER_MISSING, "geometry");
		}
		if (null == request.getSplitLine()) {
			throw new GeomajasException(ExceptionCode.PARAMETER_MISSING, "splittingLine");
		}

		// Convert geometries to JTS model:
		Geometry geometry = converter.toInternal(request.getGeometry());
		Geometry splittingLine = converter.toInternal(request.getSplitLine());

		// Calculate the splitting algorithm:
		Geometry buffered = splittingLine.buffer(DELTA);
		Geometry diff = geometry.difference(buffered);

		// Try to return the same type of geometry as was originally given:
		List<org.geomajas.geometry.Geometry> geometries = new ArrayList<org.geomajas.geometry.Geometry>();
		if (org.geomajas.geometry.Geometry.POLYGON.equals(request.getGeometry().getGeometryType())) {
			if (diff instanceof Polygon) {
				geometries.add(converter.toDto(diff));
			} else if (diff instanceof MultiPolygon) {
				for (int i = 0; i < diff.getNumGeometries(); i++) {
					geometries.add(converter.toDto(diff.getGeometryN(i)));
				}
			}
		} else if (org.geomajas.geometry.Geometry.MULTI_POLYGON.equals(request.getGeometry().getGeometryType())) {
			if (diff instanceof Polygon) {
				MultiPolygon mp = diff.getFactory().createMultiPolygon(new Polygon[] { (Polygon) diff });
				geometries.add(converter.toDto(mp));
			} else if (diff instanceof MultiPolygon) {
				geometries.add(converter.toDto(diff));
			}
		}

		// Only support for Polygon and MultiPolygon, otherwise throw an exception...
		if (geometries.size() == 0) {
			throw new GeomajasException();
		}
		response.setGeometries(geometries);
	}
}