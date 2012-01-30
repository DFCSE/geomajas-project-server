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

package org.geomajas.gwt.client.gfx.paintable;

import org.geomajas.geometry.Coordinate;
import org.geomajas.gwt.client.gfx.PainterVisitor;
import org.geomajas.gwt.client.gfx.style.ShapeStyle;
import org.geomajas.gwt.client.spatial.Bbox;

/**
 * <p>
 * A circle that can be drawn onto a <code>GraphicsContext</code>.
 * </p>
 * 
 * @author Pieter De Graef
 */
public class Circle extends AbstractWorldPaintable {

	private ShapeStyle style;

	// -------------------------------------------------------------------------
	// Constructors:
	// -------------------------------------------------------------------------

	/**
	 * constructor setting the id.
	 */
	public Circle(String id) {
		super(id);
	}

	// -------------------------------------------------------------------------
	// WorldPaintable implementation:
	// -------------------------------------------------------------------------

	/**
	 * Everything that can be drawn on the map, must be accessible by a PainterVisitor!
	 * 
	 * @param visitor
	 *            A PainterVisitor object. Comes from a MapWidget.
	 * @param bounds
	 *            Not used here.
	 * @param recursive
	 *            Not used here.
	 */
	public void accept(PainterVisitor visitor, Object group, Bbox bounds, boolean recursive) {
		visitor.visit(this, group);
	}

	// -------------------------------------------------------------------------
	// Getters and setters:
	// -------------------------------------------------------------------------

	public Coordinate getPosition() {
		return ((Bbox) getLocation()).getCenterPoint();
	}

	public void setPosition(Coordinate position) {
		if (getOriginalLocation() != null) {
			Bbox oldBounds = (Bbox) getOriginalLocation();
			Bbox newBounds = (Bbox) oldBounds.clone();
			newBounds.setCenterPoint(position);
			setOriginalLocation(newBounds);
		} else {
			setOriginalLocation(new Bbox(position.getX(), position.getY(), 0, 0));
		}
	}

	public float getRadius() {
		if (getLocation() != null) {
			Bbox oldBounds = (Bbox) getLocation();
			return (float) (0.5f * oldBounds.getWidth());
		} else {
			return 0;
		}
	}

	public void setRadius(float radius) {
		if (getOriginalLocation() != null) {
			Bbox oldBounds = (Bbox) getOriginalLocation();
			Bbox newBounds = (Bbox) oldBounds.clone();
			newBounds.setWidth(2 * radius);
			newBounds.setHeight(2 * radius);
			newBounds.setCenterPoint(oldBounds.getCenterPoint());
			setOriginalLocation(newBounds);
		} else {
			setOriginalLocation(new Bbox(0, 0, 2 * radius, 2 * radius));
		}
	}

	public ShapeStyle getStyle() {
		return style;
	}

	public void setStyle(ShapeStyle style) {
		this.style = style;
	}
}