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
package org.geomajas.layer.feature.attribute;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.geomajas.annotation.Api;
import org.geomajas.layer.feature.Attribute;

/**
 * <p>
 * Definition of a value for association attributes. The {@link ManyToOneAttribute} will contain one of these, while the
 * {@link OneToManyAttribute} will contain a whole list of these values.
 * </p>
 * <p>
 * It basically defines the bean that holds the attribute value. These beans all have an identifier and a list of
 * attributes themselves, so that is represented in the value object.
 * </p>
 * 
 * @author Pieter De Graef
 * @author Jan De Moerloose
 * @since 1.6.0
 */
@Api(allMethods = true)
public class AssociationValue implements Serializable {

	private static final long serialVersionUID = 151L;

	private PrimitiveAttribute<?> id;

	private Map<String, Attribute<?>> attributes;

	private boolean primitiveOnly;

	/**
	 * Constructor, create attribute without value (needed for GWT).
	 */
	public AssociationValue() {
	}

	/**
	 * Create attribute with specified value and primitive attributes only.
	 * 
	 * @param id id attribute for association
	 * @param attributes values for the attributes
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public AssociationValue(PrimitiveAttribute<?> id, Map<String, PrimitiveAttribute<?>> attributes) {
		this(id, (Map) attributes, true);
	}

	/**
	 * Create attribute with specified value and optionally restrict attribute values to primitives.
	 * 
	 * @param id id attribute for association
	 * @param attributes values for the attributes
	 * @param primitiveOnly true if only primitive attributes should be allowed, false otherwise
	 * @since 1.9.0
	 */
	public AssociationValue(PrimitiveAttribute<?> id, Map<String, Attribute<?>> attributes, boolean primitiveOnly) {
		this.id = id;
		this.attributes = attributes;
		this.primitiveOnly = primitiveOnly;
		if (primitiveOnly) {
			for (Attribute<?> attribute : attributes.values()) {
				if (!attribute.isPrimitive()) {
					throw new IllegalArgumentException("Expected primitive attributes only");
				}
			}
		}
	}

	/**
	 * Create a clone of this object.
	 * 
	 * @since 1.7.0
	 * @return A new AssociationValue with the same contents.
	 */
	public Object clone() { // NOSONAR super.clone() not supported by GWT
		PrimitiveAttribute<?> idClone = null;
		if (id != null) {
			idClone = (PrimitiveAttribute<?>) id.clone();
		}
		Map<String, Attribute<?>> attrClone = new HashMap<String, Attribute<?>>();
		if (attributes != null) {
			for (Entry<String, Attribute<?>> entry : attributes.entrySet()) {
				attrClone.put(entry.getKey(), (Attribute<?>) entry.getValue().clone());
			}
		}
		return new AssociationValue(idClone, attrClone, primitiveOnly);
	}

	/**
	 * Get the id for the associated object.
	 * 
	 * @return id for associated object
	 */
	public PrimitiveAttribute<?> getId() {
		return id;
	}

	/**
	 * Set the id for the associated object.
	 * 
	 * @param id id for associated object
	 */
	public void setId(PrimitiveAttribute<?> id) {
		this.id = id;
	}

	/**
	 * Get the primitive attributes for the associated object.
	 * 
	 * @return attributes for associated objects
	 * @deprecated replaced by {@link #getAllAttributes()} after introduction of nested associations
	 */
	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Map<String, PrimitiveAttribute<?>> getAttributes() {
		if (!isPrimitiveOnly()) {
			throw new UnsupportedOperationException("Primitive API not supported for nested association values");
		}
		return (Map) attributes;
	}

	/**
	 * Set the attributes for the associated object.
	 * 
	 * @param attributes attributes for associated objects
	 * @deprecated replaced by {@link #setAllAttributes(Map)} after introduction of nested associations
	 */
	@Deprecated
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setAttributes(Map<String, PrimitiveAttribute<?>> attributes) {
		if (!isPrimitiveOnly()) {
			throw new UnsupportedOperationException("Primitive API not supported for nested association values");
		}
		this.attributes = (Map) attributes;
	}

	/**
	 * Get the attributes for the associated object.
	 * 
	 * @return attributes for associated objects
	 * @since 1.9.0
	 */
	public Map<String, Attribute<?>> getAllAttributes() {
		return attributes;
	}

	/**
	 * Set the attributes for the associated object.
	 * 
	 * @param attributes attributes for associated objects
	 * @since 1.9.0
	 */
	public void setAllAttributes(Map<String, Attribute<?>> attributes) {
		this.attributes = attributes;
	}
	
	/**
	 * Convenience method that returns the attribute value for the specified attribute name.
	 *
	 * @param attributeName the name of the attribute
	 * @return the value of the attribute or null if no such attribute exists
	 * @since 1.9.0
	 */
	public Object getAttributeValue(String attributeName) {
		Attribute attribute = getAllAttributes().get(attributeName);
		if (attribute != null) {
			return attribute.getValue();
		}
		return null;
	}

	/**
	 * Returns whether this value can only contain primitive attributes.
	 * 
	 * @return true if only primitive attributes, false otherwise
	 * @since 1.9.0
	 */
	public boolean isPrimitiveOnly() {
		return primitiveOnly;
	}

	/**
	 * Specify whether this value can only contain primitive attributes.
	 * 
	 * @param primitiveOnly true if only primitive attributes, false otherwise
	 * @since 1.9.0
	 */
	public void setPrimitiveOnly(boolean primitiveOnly) {
		this.primitiveOnly = primitiveOnly;
	}
	
	/**
	 * Sets the specified boolean attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setBooleanAttribute(String name, Boolean value) {
		ensureAttributes();
		getAllAttributes().put(name, new BooleanAttribute(value));
	}

	/**
	 * Sets the specified currency attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setCurrencyAttribute(String name, String value) {
		ensureAttributes();
		getAllAttributes().put(name, new CurrencyAttribute(value));
	}

	/**
	 * Sets the specified date attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setDateAttribute(String name, Date value) {
		ensureAttributes();
		getAllAttributes().put(name, new DateAttribute(value));
	}

	/**
	 * Sets the specified double attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setDoubleAttribute(String name, Double value) {
		ensureAttributes();
		getAllAttributes().put(name, new DoubleAttribute(value));
	}

	/**
	 * Sets the specified float attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setFloatAttribute(String name, Float value) {
		ensureAttributes();
		getAllAttributes().put(name, new FloatAttribute(value));
	}

	/**
	 * Sets the specified image URL attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setImageUrlAttribute(String name, String value) {
		ensureAttributes();
		getAllAttributes().put(name, new ImageUrlAttribute(value));
	}
	
	/**
	 * Sets the specified integer attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setIntegerAttribute(String name, Integer value) {
		ensureAttributes();
		getAllAttributes().put(name, new IntegerAttribute(value));
	}
	
	/**
	 * Sets the specified long attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setLongAttribute(String name, Long value) {
		ensureAttributes();
		getAllAttributes().put(name, new LongAttribute(value));
	}

	/**
	 * Sets the specified short attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setShortAttribute(String name, Short value) {
		ensureAttributes();
		getAllAttributes().put(name, new ShortAttribute(value));
	}

	/**
	 * Sets the specified string attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setStringAttribute(String name, String value) {
		ensureAttributes();
		getAllAttributes().put(name, new StringAttribute(value));
	}

	/**
	 * Sets the specified URL attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setUrlAttribute(String name, String value) {
		ensureAttributes();
		getAllAttributes().put(name, new UrlAttribute(value));
	}

	/**
	 * Sets the specified many-to-one attribute to the specified value.
	 * 
	 * @param name name of the attribute
	 * @param value value of the attribute
	 * @since 1.9.0
	 */
	public void setManyToOneAttribute(String name, AssociationValue value) {
		ensureAttributes();
		getAllAttributes().put(name, new ManyToOneAttribute(value));
	}

	/**
	 * Returns a string representation of the value of the form <code>{id=1, attr1=a, attr2=0.5}</code>.
	 * 
	 * @return string value
	 * @since 1.9.0
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder("{id=");
		builder.append(id);
		if (attributes != null) {
			builder.append(", ");
			Iterator<Map.Entry<String, Attribute<?>>> it = attributes.entrySet().iterator();
			if (it.hasNext()) {
				while (it.hasNext()) {
					Map.Entry<String, Attribute<?>> entry = it.next();
					builder.append(entry.getKey()).append("=").append(entry.getValue());
					if (it.hasNext()) {
						builder.append(", ");
					}
				}
			} else {
				// indicate empty map !
				builder.append("attrs={}");
			}
		} else {
			builder.append(", attrs=null");
		}
		return builder.append("}").toString();
	}
	
	private void ensureAttributes() {
		if (attributes == null) {
			attributes = new HashMap<String, Attribute<?>>();
		}
	}



}
