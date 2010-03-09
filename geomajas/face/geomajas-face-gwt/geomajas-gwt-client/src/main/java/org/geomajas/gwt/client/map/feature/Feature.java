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

package org.geomajas.gwt.client.map.feature;

import org.geomajas.geometry.Coordinate;
import org.geomajas.gwt.client.gfx.Paintable;
import org.geomajas.gwt.client.gfx.PainterVisitor;
import org.geomajas.gwt.client.map.layer.VectorLayer;
import org.geomajas.gwt.client.spatial.Bbox;
import org.geomajas.gwt.client.spatial.geometry.Geometry;
import org.geomajas.gwt.client.util.GeometryConverter;
import org.geomajas.layer.feature.Attribute;
import org.geomajas.layer.feature.attribute.BooleanAttribute;
import org.geomajas.layer.feature.attribute.CurrencyAttribute;
import org.geomajas.layer.feature.attribute.DateAttribute;
import org.geomajas.layer.feature.attribute.DoubleAttribute;
import org.geomajas.layer.feature.attribute.FloatAttribute;
import org.geomajas.layer.feature.attribute.ImageUrlAttribute;
import org.geomajas.layer.feature.attribute.IntegerAttribute;
import org.geomajas.layer.feature.attribute.LongAttribute;
import org.geomajas.layer.feature.attribute.ShortAttribute;
import org.geomajas.layer.feature.attribute.StringAttribute;
import org.geomajas.layer.feature.attribute.UrlAttribute;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Definition of an object belonging to a {@link VectorLayer}. Simply put, a feature is an object with a unique ID
 * within it's layer, a list of alpha-numerical attributes and a geometry.
 * </p>
 * 
 * @author Pieter De Graef
 */
public class Feature implements Paintable, Cloneable {

	/** Unique identifier, over layers */
	private String id;

	/** Reference to the layer. */
	private VectorLayer layer;

	/** Map of this feature's attributes. */
	private Map<String, Attribute> attributes;

	/** This feature's geometry. */
	private Geometry geometry;

	/** The identifier of this feature's style. */
	private int styleId;

	/** Coordinate for the label. */
	private Coordinate labelPosition;

	/** is the feature clipped ? */
	private boolean clipped;

	/**
	 * Is it allowed for the user in question to edit this feature?
	 */
	private boolean updatable;

	/**
	 * Is it allowed for the user in question to delete this feature?
	 */
	private boolean deletable;

	private String crs;

	// Constructors:

	public Feature() {
		this(null);
	}

	public Feature(VectorLayer layer) {
		this(null, layer);
	}

	public Feature(org.geomajas.layer.feature.Feature dto, VectorLayer layer) {
		this.layer = layer;
		this.attributes = new HashMap<String, Attribute>();
		this.geometry = null;
		this.styleId = 1;
		this.labelPosition = null;
		this.clipped = false;
		if (dto != null) {
			attributes = dto.getAttributes();
			id = dto.getId();
			geometry = GeometryConverter.toGwt(dto.getGeometry());
			styleId = dto.getStyleId();
			crs = dto.getCrs();
			setUpdatable(dto.isUpdatable());
			setDeletable(dto.isDeletable());
		}
	}

	// Paintable implementation:

	public void accept(PainterVisitor visitor, Bbox bounds, boolean recursive) {
		visitor.visit(this);
	}

	public String getId() {
		return id;
	}

	/**
	 * Id which should be used for this feature when rendered as "selected".
	 * 
	 * @return selected id
	 */
	public String getSelectionId() {
		return layer.getMapModel().getId() + "." + layer.getId() + ".selection." + getId();
	}

	public static String getFeatureIdFromSelectionId(String selectionId) {
		String[] ids = selectionId.split("\\."); // It's a regular expression, not literally.
		if (ids.length > 2) {
			return ids[ids.length - 2] + "." + ids[ids.length - 1];
		}
		return null;
	}

	// Class specific functions:

	public Feature clone() {
		Feature feature = new Feature(this.layer);
		if (null != attributes) {
			feature.attributes = new HashMap<String, Attribute>(attributes);
		}
		feature.clipped = clipped;
		feature.labelPosition = labelPosition;
		feature.layer = layer;
		if (null != geometry) {
			feature.geometry = (Geometry) geometry.clone();
		}
		feature.id = id;
		feature.styleId = styleId;
		feature.crs = crs;
		return feature;
	}

	public Object getAttributeValue(String attributeName) {
		Attribute attribute = attributes.get(attributeName);
		if (attribute instanceof BooleanAttribute) {
			return ((BooleanAttribute) attribute).getValue();
		} else if (attribute instanceof CurrencyAttribute) {
			return ((CurrencyAttribute) attribute).getValue();
		} else if (attribute instanceof DateAttribute) {
			return ((DateAttribute) attribute).getValue();
		} else if (attribute instanceof DoubleAttribute) {
			return ((DoubleAttribute) attribute).getValue();
		} else if (attribute instanceof FloatAttribute) {
			return ((FloatAttribute) attribute).getValue();
		} else if (attribute instanceof ImageUrlAttribute) {
			return ((ImageUrlAttribute) attribute).getValue();
		} else if (attribute instanceof IntegerAttribute) {
			return ((IntegerAttribute) attribute).getValue();
		} else if (attribute instanceof LongAttribute) {
			return ((LongAttribute) attribute).getValue();
		} else if (attribute instanceof ShortAttribute) {
			return ((ShortAttribute) attribute).getValue();
		} else if (attribute instanceof StringAttribute) {
			return ((StringAttribute) attribute).getValue();
		} else if (attribute instanceof UrlAttribute) {
			return ((UrlAttribute) attribute).getValue();
		}
		return null;
	}

	/**
	 * Transform this object into a DTO feature.
	 * 
	 * @return dto for this feature
	 */
	public org.geomajas.layer.feature.Feature toDto() {
		org.geomajas.layer.feature.Feature dto = new org.geomajas.layer.feature.Feature();
		dto.setAttributes(attributes);
		dto.setClipped(clipped);
		dto.setId(id);
		dto.setGeometry(GeometryConverter.toDto(geometry));
		dto.setCrs(crs);
		return dto;
	}

	public String getLabel() {
		String attributeName = layer.getLayerInfo().getNamedStyleInfo().getLabelStyle().getLabelAttributeName();
		Object attributeValue = getAttributeValue(attributeName);
		return attributeValue == null ? "null" : attributeValue.toString();
	}

	// Getters and setters:

	/**
	 * Crs as (optionally) set by the backend.
	 *
	 * @return crs for this feature
	 */
	public String getCrs() {
		return crs;
	}

	/**
	 * Get the attributes map, null when it needs to be lazy loaded.
	 *
	 * @return attributes map
	 */
	public Map<String, Attribute> getAttributes() {
		return attributes;
	}

	/**
	 * Get the attributes map, lazy loading it when not yet available.
	 *
	 * @param callback routine to call with value
	 */
	/*
	public Map<String, Attribute> getAttributes(CommandCallback<Map<String, Attribute>> callback) {
		if (null == attributes) {
			lazyLoad(GeomajasConstant.FEATURE_INCLUDE_ATTRIBUTES, callback);
		} else {
			callback.execute(attributes);
		}
	}
	*/

	public void setAttributes(Map<String, Attribute> attributes) {
		this.attributes = attributes;
	}

	/**
	 * Get the feature's geometry, , null when it needs to be lazy loaded.
	 *
	 * @return geometry
	 */
	public Geometry getGeometry() {
		return geometry;
	}

	/**
	 * Get the feature's geometry, lazy loading it when not yet available.
	 *
	 * @param callback routine to call with value
	 */
	/*
	public Geometry getGeometry(CommandCallback<Geometry> callback) {
		if (null == geometry) {
			lazyLoad(GeomajasConstant.FEATURE_INCLUDE_GEOMETRY, callback);
		} else {
			callback.execute(geometry);
		}
	}
	*/

	public void setGeometry(Geometry geometry) {
		this.geometry = geometry;
	}

	public int getStyleId() {
		return styleId;
	}

	public void setStyleId(int styleId) {
		this.styleId = styleId;
	}

	public boolean isSelected() {
		return layer.isFeatureSelected(getId());
	}

	public VectorLayer getLayer() {
		return layer;
	}

	/**
	 * Is the logged in user allowed to edit this feature?
	 * 
	 * @return true when edit/update is allowed for this feature
	 */
	public boolean isUpdatable() {
		return updatable;
	}

	/**
	 * Set whether the logged in user is allowed to edit/update this feature.
	 * 
	 * @param editable
	 *            true when edit/update is allowed for this feature
	 */
	public void setUpdatable(boolean editable) {
		this.updatable = editable;
	}

	/**
	 * Is the logged in user allowed to delete this feature?
	 * 
	 * @return true when delete is allowed for this feature
	 */
	public boolean isDeletable() {
		return deletable;
	}

	/**
	 * Set whether the logged in user is allowed to delete this feature.
	 * 
	 * @param deletable
	 *            true when deleting this feature is allowed
	 */
	public void setDeletable(boolean deletable) {
		this.deletable = deletable;
	}

	/* @todo
	private void lazyLoad(final int featureIncludes, final CommandCallback callback) {
		SearchFeatureRequest request = new SearchFeatureRequest();
		request.setBooleanOperator("OR");
		SearchCriterion[] criteria = new SearchCriterion[1];
		criteria[0] = new SearchCriterion(SearchFeatureRequest.ID_ATTRIBUTE, "=", getId());
		request.setCriteria(criteria);
		request.setCrs(getCrs());
		request.setLayerId(layer.getId());
		request.setMax(0);
		request.setFilter(layer.getFilter());
		request.setFeatureIncludes(featureIncludes);

		GwtCommand command = new GwtCommand("command.feature.Search");
		command.setCommandRequest(request);
		GwtCommandDispatcher.getInstance().execute(command, new CommandCallback() {

			public void execute(CommandResponse response) {
				if (response instanceof SearchFeatureResponse) {
					SearchFeatureResponse resp = (SearchFeatureResponse) response;
					if (null != resp.getFeatures() && resp.getFeatures().length > 0) {
						org.geomajas.layer.feature.Feature dto = resp.getFeatures()[0];
						if ((featureIncludes & GeomajasConstant.FEATURE_INCLUDE_ATTRIBUTES) != 0) {
							attributes = dto.getAttributes();
							callback.execute(attributes);
						}
						if ((featureIncludes & GeomajasConstant.FEATURE_INCLUDE_GEOMETRY) != 0) {
							geometry = GeometryConverter.toGwt(dto.getGeometry());
							callback.execute(geometry);
						}
					}
				}
			}
		});
	}
	*/
}
