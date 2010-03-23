/*
 * This file is part of Geomajas, a component framework for building
 * rich Internet applications (RIA) with sophisticated capabilities for the
 * display, analysis and management of geographic information.
 * It is a building block that allows developers to add maps
 * and other geographic data capabilities to their web applications.
 *
 * Copyright 2008-2010 Geosparc, http://www.geosparc.com, Belgium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.geomajas.gwt.client.map.feature.operation;

import org.geomajas.geometry.Coordinate;
import org.geomajas.gwt.client.map.feature.Feature;
import org.geomajas.gwt.client.map.feature.TransactionGeomIndex;
import org.geomajas.gwt.client.spatial.geometry.Geometry;
import org.geomajas.gwt.client.spatial.geometry.GeometryEditor;
import org.geomajas.gwt.client.spatial.geometry.LineString;
import org.geomajas.gwt.client.spatial.geometry.LinearRing;
import org.geomajas.gwt.client.spatial.geometry.MultiLineString;
import org.geomajas.gwt.client.spatial.geometry.MultiPolygon;
import org.geomajas.gwt.client.spatial.geometry.Point;
import org.geomajas.gwt.client.spatial.geometry.Polygon;
import org.geomajas.gwt.client.spatial.geometry.operation.InsertCoordinateOperation;
import org.geomajas.gwt.client.spatial.geometry.operation.RemoveCoordinateOperation;

/**
 * ???
 *
 * @author Pieter De Graef
 */
public class AddCoordinateOp extends GeometryEditor implements FeatureOperation {

	private TransactionGeomIndex index;

	private Coordinate coordinate;

	public AddCoordinateOp(TransactionGeomIndex index, Coordinate coordinate) {
		this.index = index;
		this.coordinate = coordinate;
	}

	public void execute(Feature feature) {
		Geometry geometry = feature.getGeometry();
		if (geometry instanceof MultiPolygon) {
			execute((MultiPolygon) geometry);
		} else if (geometry instanceof MultiLineString) {
			execute((MultiLineString) geometry);
		} else if (geometry instanceof Polygon) {
			execute((Polygon) geometry);
		} else if (geometry instanceof LineString) {
			feature.setGeometry(execute((LineString) geometry));
		} else if (geometry instanceof Point) {
			execute((Point) geometry);
		}
	}

	public void undo(Feature feature) {
		Geometry geometry = feature.getGeometry();
		if (geometry instanceof MultiPolygon) {
			undo((MultiPolygon) geometry);
		} else if (geometry instanceof MultiLineString) {
			undo((MultiLineString) geometry);
		} else if (geometry instanceof Polygon) {
			undo((Polygon) geometry);
		} else if (geometry instanceof LineString) {
			feature.setGeometry(undo((LineString) geometry));
		} else if (geometry instanceof Point) {
			undo((Point) geometry);
		}
	}

	// -------------------------------------------------------------------------
	// Private execution functions:
	// -------------------------------------------------------------------------

	private void execute(MultiPolygon multiPolygon) {
		if (index.getGeometryIndex() >= multiPolygon.getNumGeometries()) {
			Polygon[] polygons = new Polygon[multiPolygon.getNumGeometries() + 1];
			for (int i = 0; i < multiPolygon.getNumGeometries(); i++) {
				polygons[i] = (Polygon) multiPolygon.getGeometryN(i);
			}
			LinearRing exteriorRing = multiPolygon.getGeometryFactory().createLinearRing(
					new Coordinate[] {coordinate});
			polygons[multiPolygon.getNumGeometries()] = multiPolygon.getGeometryFactory().createPolygon(exteriorRing,
					null);
			setPolygons(multiPolygon, polygons);
		} else {
			execute((Polygon) multiPolygon.getGeometryN(index.getGeometryIndex()));
		}
	}

	private void execute(MultiLineString multiLineString) {
		if (index.getGeometryIndex() >= multiLineString.getNumGeometries()) {
			LineString[] lineStrings = new LineString[multiLineString.getNumGeometries() + 1];
			for (int i = 0; i < multiLineString.getNumGeometries(); i++) {
				lineStrings[i] = (LineString) multiLineString.getGeometryN(i);
			}
			lineStrings[multiLineString.getNumGeometries()] = multiLineString.getGeometryFactory().createLineString(
					new Coordinate[] {coordinate});
			setLineStrings(multiLineString, lineStrings);
		} else {
			LineString lineString = execute((LineString) multiLineString.getGeometryN(index.getGeometryIndex()));
			setLineStringN(multiLineString, lineString, index.getGeometryIndex());
		}
	}

	private void execute(Polygon polygon) {
		LinearRing ring = null;
		if (index.isExteriorRing()) {
			ring = polygon.getExteriorRing();
			if (ring == null) {
				ring = polygon.getGeometryFactory().createLinearRing(new Coordinate[] {coordinate});
				setExteriorRing(polygon, ring);
			} else {
				setExteriorRing(polygon, (LinearRing) execute(ring));
			}
		} else if (index.getInteriorRingIndex() >= 0) {
			if (index.getGeometryIndex() >= polygon.getNumInteriorRing()) {
				LinearRing[] interiorRings = new LinearRing[polygon.getNumInteriorRing() + 1];
				for (int i = 0; i < polygon.getNumInteriorRing(); i++) {
					interiorRings[i] = polygon.getInteriorRingN(i);
				}
				interiorRings[polygon.getNumInteriorRing()] = polygon.getGeometryFactory().createLinearRing(
						new Coordinate[] {coordinate});
				setInteriorRings(polygon, interiorRings);
			} else {
				ring = polygon.getInteriorRingN(index.getInteriorRingIndex());
				LineString lineString = execute(ring);
				setInteriorRingN(polygon, (LinearRing) lineString, index.getInteriorRingIndex());
			}
		}
	}

	private LineString execute(LineString lineString) {
		InsertCoordinateOperation op = new InsertCoordinateOperation(lineString.getNumPoints(), coordinate);
		return (LineString) op.execute(lineString);
	}

	private void execute(Point point) {
		if (point.isEmpty()) {
			setCoordinate(point, coordinate);
		}
	}

	// -------------------------------------------------------------------------
	// Private undo functions:
	// -------------------------------------------------------------------------

	private void undo(MultiPolygon multiPolygon) {
		undo((Polygon) multiPolygon.getGeometryN(index.getGeometryIndex()));
	}

	private void undo(MultiLineString multiLineString) {
		LineString lineString = undo((LineString) multiLineString.getGeometryN(index.getGeometryIndex()));
		setLineStringN(multiLineString, lineString, index.getGeometryIndex());
	}

	private void undo(Polygon polygon) {
		LinearRing ring = null;
		if (index.isExteriorRing()) {
			ring = polygon.getExteriorRing();
			LineString lineString = undo(ring);
			setExteriorRing(polygon, (LinearRing) lineString);
		} else if (index.getInteriorRingIndex() >= 0) {
			ring = polygon.getInteriorRingN(index.getInteriorRingIndex());
			LineString lineString = undo(ring);
			setInteriorRingN(polygon, (LinearRing) lineString, index.getInteriorRingIndex());
		}
	}

	private LineString undo(LineString lineString) {
		RemoveCoordinateOperation op = new RemoveCoordinateOperation(lineString.getNumPoints());
		return (LineString) op.execute(lineString);
	}

	private void undo(Point point) {
		setCoordinate(point, null);
	}
}
