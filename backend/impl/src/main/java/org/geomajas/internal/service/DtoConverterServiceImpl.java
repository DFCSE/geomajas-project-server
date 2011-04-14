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

package org.geomajas.internal.service;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.ConvertUtilsBean;
import org.geomajas.configuration.AssociationAttributeInfo;
import org.geomajas.configuration.AssociationType;
import org.geomajas.configuration.AttributeInfo;
import org.geomajas.configuration.PrimitiveAttributeInfo;
import org.geomajas.geometry.Bbox;
import org.geomajas.geometry.Coordinate;
import org.geomajas.geometry.Geometry;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.internal.layer.feature.InternalFeatureImpl;
import org.geomajas.layer.LayerType;
import org.geomajas.layer.VectorLayerService;
import org.geomajas.layer.feature.Attribute;
import org.geomajas.layer.feature.Feature;
import org.geomajas.layer.feature.InternalFeature;
import org.geomajas.layer.feature.attribute.ArrayAttribute;
import org.geomajas.layer.feature.attribute.AssociationAttribute;
import org.geomajas.layer.feature.attribute.AssociationValue;
import org.geomajas.layer.feature.attribute.BooleanAttribute;
import org.geomajas.layer.feature.attribute.CurrencyAttribute;
import org.geomajas.layer.feature.attribute.DateAttribute;
import org.geomajas.layer.feature.attribute.DoubleAttribute;
import org.geomajas.layer.feature.attribute.FloatAttribute;
import org.geomajas.layer.feature.attribute.ImageUrlAttribute;
import org.geomajas.layer.feature.attribute.IntegerAttribute;
import org.geomajas.layer.feature.attribute.LongAttribute;
import org.geomajas.layer.feature.attribute.ManyToOneAttribute;
import org.geomajas.layer.feature.attribute.OneToManyAttribute;
import org.geomajas.layer.feature.attribute.PrimitiveAttribute;
import org.geomajas.layer.feature.attribute.ShortAttribute;
import org.geomajas.layer.feature.attribute.StringAttribute;
import org.geomajas.layer.feature.attribute.UrlAttribute;
import org.geomajas.layer.tile.InternalTile;
import org.geomajas.layer.tile.VectorTile;
import org.geomajas.service.DtoConverterService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Default implementation of DTO converter.
 * 
 * @author Jan De Moerloose
 * @author Joachim Van der Auwera
 * @author Pieter De Graef
 */
@Component()
public class DtoConverterServiceImpl implements DtoConverterService {

	private ConvertUtilsBean cub = new ConvertUtilsBean();

	// -------------------------------------------------------------------------
	// Attribute conversion:
	// -------------------------------------------------------------------------

	/**
	 * Converts a DTO attribute into a generic attribute object.
	 * 
	 * @param attribute
	 *            The DTO attribute.
	 * @return The server side attribute representation. As we don't know at this point what kind of object the
	 *         attribute is (that's a problem for the <code>FeatureModel</code>), we return an <code>Object</code>.
	 */
	public Object toInternal(Attribute<?> attribute) throws GeomajasException {
		if (attribute instanceof PrimitiveAttribute<?>) {
			return toPrimitiveObject((PrimitiveAttribute<?>) attribute);
		} else if (attribute instanceof AssociationAttribute<?>) {
			return toAssociationObject((AssociationAttribute<?>) attribute);
		} else {
			throw new GeomajasException(ExceptionCode.CONVERSION_PROBLEM, attribute);
		}
	}

	/**
	 * Converts a server-side attribute object into a DTO attribute.
	 * 
	 * @param object
	 *            The attribute value.
	 * @param info
	 *            The attribute definition from the configuration.
	 * @return Returns a DTO attribute.
	 */
	public Attribute<?> toDto(Object object, AttributeInfo info) throws GeomajasException {
		if (info instanceof PrimitiveAttributeInfo) {
			if (object instanceof Object[]) {
				return toArrayDto((Object[]) object, (PrimitiveAttributeInfo) info);
			} else {
				return toPrimitiveDto(object, (PrimitiveAttributeInfo) info);
			}
		} else if (info instanceof AssociationAttributeInfo) {
			return toAssociationDto(object, (AssociationAttributeInfo) info);
		} else {
			throw new GeomajasException(ExceptionCode.CONVERSION_PROBLEM, info.getName());
		}
	}

	// -------------------------------------------------------------------------
	// Private methods - Attribute conversion:
	// -------------------------------------------------------------------------

	private Attribute<?> toAssociationDto(Object value, AssociationAttributeInfo associationAttributeInfo)
			throws GeomajasException {
		if (associationAttributeInfo.getType() == AssociationType.MANY_TO_ONE) {
			return new ManyToOneAttribute(createAssociationValue(value, associationAttributeInfo));
		} else if (associationAttributeInfo.getType() == AssociationType.ONE_TO_MANY) {
			// Value should be an array of objects...
			List<AssociationValue> associationValues = new ArrayList<AssociationValue>();
			if (value != null && value instanceof Object[]) {
				Object[] array = (Object[]) value;
				for (Object bean : array) {
					associationValues.add(createAssociationValue(bean, associationAttributeInfo));
				}
			}
			return new OneToManyAttribute(associationValues);
		}
		return null;
	}

	private AssociationValue createAssociationValue(Object value, AssociationAttributeInfo associationAttributeInfo)
			throws GeomajasException {
		if (value == null) {
			return null;
		}
		Map<String, PrimitiveAttribute<?>> attributes = new HashMap<String, PrimitiveAttribute<?>>();
		for (AttributeInfo attributeInfo : associationAttributeInfo.getFeature().getAttributes()) {
			Object propertyValue = getBeanProperty(value, attributeInfo.getName());
			attributes.put(attributeInfo.getName(), (PrimitiveAttribute<?>) toDto(propertyValue, attributeInfo));
		}
		PrimitiveAttribute<?> id = (PrimitiveAttribute<?>) toDto(getBeanProperty(value, associationAttributeInfo
				.getFeature().getIdentifier().getName()), associationAttributeInfo.getFeature().getIdentifier());
		return new AssociationValue(id, attributes);
	}

	private Object getBeanProperty(Object bean, String property) throws GeomajasException {
		PropertyDescriptor d = BeanUtils.getPropertyDescriptor(bean.getClass(), property);
		if (d != null) {
			Method m = d.getReadMethod();
			if (m != null) {
				if (!Modifier.isPublic(m.getDeclaringClass().getModifiers())) {
					m.setAccessible(true);
				}
				Object value;
				try {
					value = m.invoke(bean);
				} catch (Exception e) {
					throw new GeomajasException(e);
				}
				return value;
			}
		}
		return null;
	}

	private Attribute<?> toPrimitiveDto(Object value, PrimitiveAttributeInfo info) {
		switch (info.getType()) {
			case BOOLEAN:
				return new BooleanAttribute((Boolean) convertToClass(value, Boolean.class));
			case SHORT:
				return new ShortAttribute((Short) convertToClass(value, Short.class));
			case INTEGER:
				return new IntegerAttribute((Integer) convertToClass(value, Integer.class));
			case LONG:
				return new LongAttribute((Long) convertToClass(value, Long.class));
			case FLOAT:
				return new FloatAttribute((Float) convertToClass(value, Float.class));
			case DOUBLE:
				return new DoubleAttribute((Double) convertToClass(value, Double.class));
			case CURRENCY:
				return new CurrencyAttribute((String) convertToClass(value, String.class));
			case STRING:
				return new StringAttribute(value == null ? null : value.toString());
			case DATE:
				return new DateAttribute((Date) value);
			case URL:
				return new UrlAttribute((String) value);
			case IMGURL:
				return new ImageUrlAttribute((String) value);
		}
		throw new IllegalArgumentException("Cannot create primitive attribute of type " + info);
	}

	private ArrayAttribute<?> toArrayDto(Object[] value, PrimitiveAttributeInfo info) {
		switch (info.getType()) {
			case BOOLEAN:
				Boolean[] booleans = new Boolean[value.length];
				for (int i = 0; i < value.length; i++) {
					booleans[i] = (Boolean) convertToClass(value[i], Boolean.class);
				}
				return new ArrayAttribute<Boolean>(booleans);
			case SHORT:
				Short[] shorts = new Short[value.length];
				for (int i = 0; i < value.length; i++) {
					shorts[i] = (Short) convertToClass(value[i], Short.class);
				}
				return new ArrayAttribute<Short>(shorts);
			case INTEGER:
				Integer[] ints = new Integer[value.length];
				for (int i = 0; i < value.length; i++) {
					ints[i] = (Integer) convertToClass(value[i], Integer.class);
				}
				return new ArrayAttribute<Integer>(ints);
			case LONG:
				Long[] longs = new Long[value.length];
				for (int i = 0; i < value.length; i++) {
					longs[i] = (Long) convertToClass(value[i], Long.class);
				}
				return new ArrayAttribute<Long>(longs);
			case FLOAT:
				Float[] floats = new Float[value.length];
				for (int i = 0; i < value.length; i++) {
					floats[i] = (Float) convertToClass(value[i], Float.class);
				}
				return new ArrayAttribute<Float>(floats);
			case DOUBLE:
				Double[] doubles = new Double[value.length];
				for (int i = 0; i < value.length; i++) {
					doubles[i] = (Double) convertToClass(value[i], Double.class);
				}
				return new ArrayAttribute<Double>(doubles);
			case CURRENCY:
				String[] values = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					values[i] = (String) convertToClass(value[i], String.class);
				}
				return new ArrayAttribute<String>(values);
			case STRING:
				String[] strings = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					strings[i] = (value[i] == null ? null : value[i].toString());
				}
				return new ArrayAttribute<String>(strings);
			case DATE:
				Date[] dates = new Date[value.length];
				for (int i = 0; i < value.length; i++) {
					dates[i] = (Date) convertToClass(value[i], Date.class);
				}
				return new ArrayAttribute<Date>(dates);
			case URL:
				String[] urls = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					urls[i] = (value[i] == null ? null : value[i].toString());
				}
				return new ArrayAttribute<String>(urls);
			case IMGURL:
				String[] images = new String[value.length];
				for (int i = 0; i < value.length; i++) {
					images[i] = (value[i] == null ? null : value[i].toString());
				}
				return new ArrayAttribute<String>(images);
		}
		throw new IllegalArgumentException("Cannot create primitive attribute of type " + info);
	}

	private Object toAssociationObject(AssociationAttribute<?> primitiveAttribute) {
		// TODO: implement
		return null;
	}

	private Object toPrimitiveObject(PrimitiveAttribute<?> primitive) {
		return primitive.getValue();
	}

	private Object convertToClass(Object object, Class<?> c) {
		if (object == null) {
			return null;
		} else if (c.isInstance(object)) {
			return object;
		}
		return cub.convert(object.toString(), c);
	}

	// -------------------------------------------------------------------------
	// Feature conversion:
	// -------------------------------------------------------------------------

	/**
	 * Convert the server side feature to a DTO feature that can be sent to the client.
	 * 
	 * @param feature
	 *            The server-side feature representation.
	 * @param featureIncludes
	 *            Indicate which aspects of the should be included @see {@link VectorLayerService}
	 * @return Returns the DTO feature.
	 */
	public Feature toDto(InternalFeature feature, int featureIncludes) throws GeomajasException {
		if (feature == null) {
			return null;
		}
		Feature dto = new Feature(feature.getId());
		if ((featureIncludes & VectorLayerService.FEATURE_INCLUDE_ATTRIBUTES) != 0 && null != feature.getAttributes()) {
			dto.setAttributes(new HashMap<String, Attribute>(feature.getAttributes()));
		}
		if ((featureIncludes & VectorLayerService.FEATURE_INCLUDE_LABEL) != 0) {
			dto.setLabel(feature.getLabel());
		}
		if ((featureIncludes & VectorLayerService.FEATURE_INCLUDE_GEOMETRY) != 0) {
			dto.setGeometry(toDto(feature.getGeometry()));
		}
		if ((featureIncludes & VectorLayerService.FEATURE_INCLUDE_STYLE) != 0 && null != feature.getStyleInfo()) {
			dto.setStyleId(feature.getStyleInfo().getStyleId());
		}
		InternalFeatureImpl vFeature = (InternalFeatureImpl) feature;
		dto.setClipped(vFeature.isClipped());
		dto.setUpdatable(feature.isEditable());
		dto.setDeletable(feature.isDeletable());
		return dto;
	}

	public Feature toDto(InternalFeature feature) throws GeomajasException {
		return toDto(feature, VectorLayerService.FEATURE_INCLUDE_ALL);
	}

	/**
	 * Convert a DTO feature to a server-side feature.
	 * 
	 * @param dto
	 *            The DTO feature that comes from the client.
	 * @return Returns a server-side feature object.
	 */
	public InternalFeature toInternal(Feature dto) throws GeomajasException {
		if (dto == null) {
			return null;
		}
		InternalFeatureImpl feature = new InternalFeatureImpl();
		feature.setAttributes(dto.getAttributes());
		feature.setId(dto.getId());
		feature.setLabel(dto.getLabel());
		feature.setGeometry(toInternal(dto.getGeometry()));
		feature.setClipped(dto.isClipped());
		feature.setEditable(dto.isUpdatable());
		feature.setDeletable(dto.isDeletable());
		return feature;
	}

	// -------------------------------------------------------------------------
	// Geometry conversion:
	// -------------------------------------------------------------------------

	/**
	 * Takes in a JTS geometry, and creates a new DTO geometry from it.
	 * 
	 * @param geometry
	 *            The geometry to convert into a DTO geometry.
	 * @return Returns a DTO type geometry, that is serializable.
	 */
	public Geometry toDto(com.vividsolutions.jts.geom.Geometry geometry) throws GeomajasException {
		if (geometry == null) {
			return null;
		}
		int srid = geometry.getSRID();
		int precision = -1;
		PrecisionModel precisionmodel = geometry.getPrecisionModel();
		if (!precisionmodel.isFloating()) {
			precision = (int) Math.log10(precisionmodel.getScale());
		}

		Geometry dto = null;
		if (geometry instanceof Point) {
			dto = new Geometry(Geometry.POINT, srid, precision);
			dto.setCoordinates(convertCoordinates(geometry));
		} else if (geometry instanceof LinearRing) {
			dto = new Geometry(Geometry.LINEAR_RING, srid, precision);
			dto.setCoordinates(convertCoordinates(geometry));
		} else if (geometry instanceof LineString) {
			dto = new Geometry(Geometry.LINE_STRING, srid, precision);
			dto.setCoordinates(convertCoordinates(geometry));
		} else if (geometry instanceof Polygon) {
			dto = new Geometry(Geometry.POLYGON, srid, precision);
			Polygon polygon = (Polygon) geometry;
			Geometry[] geometries = new Geometry[polygon.getNumInteriorRing() + 1];
			for (int i = 0; i < geometries.length; i++) {
				if (i == 0) {
					geometries[i] = toDto(polygon.getExteriorRing());
				} else {
					geometries[i] = toDto(polygon.getInteriorRingN(i - 1));
				}
			}
			dto.setGeometries(geometries);
		} else if (geometry instanceof MultiPoint) {
			dto = new Geometry(Geometry.MULTI_POINT, srid, precision);
			dto.setGeometries(convertGeometries(geometry));
		} else if (geometry instanceof MultiLineString) {
			dto = new Geometry(Geometry.MULTI_LINE_STRING, srid, precision);
			dto.setGeometries(convertGeometries(geometry));
		} else if (geometry instanceof MultiPolygon) {
			dto = new Geometry(Geometry.MULTI_POLYGON, srid, precision);
			dto.setGeometries(convertGeometries(geometry));
		} else {
			throw new GeomajasException(ExceptionCode.CANNOT_CONVERT_GEOMETRY, geometry.getClass().getName());
		}

		return dto;
	}

	/**
	 * Takes in a DTO geometry, and converts it into a JTS geometry.
	 * 
	 * @param geometry
	 *            The DTO geometry to convert into a JTS geometry.
	 * @return Returns a JTS geometry.
	 */
	public com.vividsolutions.jts.geom.Geometry toInternal(Geometry geometry) throws GeomajasException {
		if (geometry == null) {
			return null;
		}
		int srid = geometry.getSrid();
		int precision = geometry.getPrecision();
		PrecisionModel model;
		if (precision == -1) {
			model = new PrecisionModel(PrecisionModel.FLOATING);
		} else {
			model = new PrecisionModel(Math.pow(10, precision));
		}
		GeometryFactory factory = new GeometryFactory(model, srid);
		com.vividsolutions.jts.geom.Geometry jts = null;

		String geometryType = geometry.getGeometryType();
		if (Geometry.POINT.equals(geometryType)) {
			jts = factory.createPoint(convertCoordinates(geometry)[0]);
		} else if (Geometry.LINEAR_RING.equals(geometryType)) {
			jts = factory.createLinearRing(convertCoordinates(geometry));
		} else if (Geometry.LINE_STRING.equals(geometryType)) {
			jts = factory.createLineString(convertCoordinates(geometry));
		} else if (Geometry.POLYGON.equals(geometryType)) {
			LinearRing exteriorRing = (LinearRing) toInternal(geometry.getGeometries()[0]);
			LinearRing[] interiorRings = new LinearRing[geometry.getGeometries().length - 1];
			for (int i = 0; i < interiorRings.length; i++) {
				interiorRings[i] = (LinearRing) toInternal(geometry.getGeometries()[i + 1]);
			}
			jts = factory.createPolygon(exteriorRing, interiorRings);
		} else if (Geometry.MULTI_POINT.equals(geometryType)) {
			Point[] points = new Point[geometry.getGeometries().length];
			jts = factory.createMultiPoint((Point[]) convertGeometries(geometry, points));
		} else if (Geometry.MULTI_LINE_STRING.equals(geometryType)) {
			LineString[] lineStrings = new LineString[geometry.getGeometries().length];
			jts = factory.createMultiLineString((LineString[]) convertGeometries(geometry, lineStrings));
		} else if (Geometry.MULTI_POLYGON.equals(geometryType)) {
			Polygon[] polygons = new Polygon[geometry.getGeometries().length];
			jts = factory.createMultiPolygon((Polygon[]) convertGeometries(geometry, polygons));
		} else {
			throw new GeomajasException(ExceptionCode.CANNOT_CONVERT_GEOMETRY, geometryType);
		}

		return jts;
	}

	// -------------------------------------------------------------------------
	// Private functions converting from JTS to DTO:
	// -------------------------------------------------------------------------

	private Coordinate[] convertCoordinates(com.vividsolutions.jts.geom.Geometry geometry) {
		Coordinate[] coordinates = new Coordinate[geometry.getCoordinates().length];
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i] = new Coordinate(geometry.getCoordinates()[i].x, geometry.getCoordinates()[i].y);
		}
		return coordinates;
	}

	private Geometry[] convertGeometries(com.vividsolutions.jts.geom.Geometry geometry) throws GeomajasException {
		Geometry[] geometries = new Geometry[geometry.getNumGeometries()];
		for (int i = 0; i < geometries.length; i++) {
			geometries[i] = toDto(geometry.getGeometryN(i));
		}
		return geometries;
	}

	// -------------------------------------------------------------------------
	// Private functions converting from DTO to JTS:
	// -------------------------------------------------------------------------

	private com.vividsolutions.jts.geom.Coordinate[] convertCoordinates(Geometry geometry) {
		com.vividsolutions.jts.geom.Coordinate[] coordinates = new com.vividsolutions.jts.geom.Coordinate[geometry
				.getCoordinates().length];
		for (int i = 0; i < coordinates.length; i++) {
			coordinates[i] = new com.vividsolutions.jts.geom.Coordinate(geometry.getCoordinates()[i].getX(), geometry
					.getCoordinates()[i].getY());
		}
		return coordinates;
	}

	private com.vividsolutions.jts.geom.Geometry[] convertGeometries(Geometry geometry,
			com.vividsolutions.jts.geom.Geometry[] geometries) throws GeomajasException {
		for (int i = 0; i < geometries.length; i++) {
			geometries[i] = toInternal(geometry.getGeometries()[i]);
		}
		return geometries;
	}

	// -------------------------------------------------------------------------
	// Tile conversion:
	// -------------------------------------------------------------------------

	/**
	 * Convert a server-side tile representations into a DTO tile.
	 * 
	 * @param tile
	 *            The server-side representation of a tile.
	 * @param crs
	 *            crs to include in features (if any)
	 * @param featureIncludes
	 *            Indicate which aspects of the should be included @see {@link org.geomajas.layer.VectorLayerService}
	 * @return Returns the DTO version that can be sent to the client.
	 */
	public VectorTile toDto(InternalTile tile, String crs, int featureIncludes) throws GeomajasException {
		if (null != tile) {
			VectorTile dto = new VectorTile();
			dto.setClipped(tile.isClipped());
			dto.setCode(tile.getCode());
			dto.setCodes(tile.getCodes());
			dto.setScreenHeight((int) tile.getScreenHeight());
			dto.setScreenWidth((int) tile.getScreenWidth());
			List<Feature> features = new ArrayList<Feature>();
			for (InternalFeature feature : tile.getFeatures()) {
				Feature fdto = toDto(feature, featureIncludes);
				fdto.setCrs(crs);
				features.add(fdto);
			}
			dto.setFeatures(features);
			dto.setFeatureContent(tile.getFeatureContent());
			dto.setLabelContent(tile.getLabelContent());
			dto.setContentType(tile.getContentType());
			return dto;
		}

		return null;
	}

	public VectorTile toDto(InternalTile tile) throws GeomajasException {
		return toDto(tile, null, VectorLayerService.FEATURE_INCLUDE_ALL);
	}

	// -------------------------------------------------------------------------
	// Bounding box conversion:
	// -------------------------------------------------------------------------

	/**
	 * Convert a <code>Bbox</code> to a JTS envelope.
	 * 
	 * @param bbox
	 *            Geomajas <code>Bbox</code>
	 * @return JTS envelope
	 */
	public Envelope toInternal(Bbox bbox) {
		return new Envelope(bbox.getX(), bbox.getMaxX(), bbox.getY(), bbox.getMaxY());
	}

	/**
	 * Convert JTS envelope into a <code>Bbox</code>.
	 * 
	 * @param envelope
	 *            JTS envelope
	 * @return Geomajas <code>Bbox</code>
	 */
	public Bbox toDto(Envelope envelope) {
		return new Bbox(envelope.getMinX(), envelope.getMinY(), envelope.getWidth(), envelope.getHeight());
	}

	/**
	 * Convert a layer type to a geometry class.
	 * 
	 * @param layerType
	 *            layer type
	 * @return JTS class
	 */
	public Class<? extends com.vividsolutions.jts.geom.Geometry> toInternal(LayerType layerType) {
		switch (layerType) {
			case GEOMETRY:
				return com.vividsolutions.jts.geom.Geometry.class;
			case LINESTRING:
				return LineString.class;
			case MULTILINESTRING:
				return MultiLineString.class;
			case POINT:
				return Point.class;
			case MULTIPOINT:
				return MultiPoint.class;
			case POLYGON:
				return Polygon.class;
			case MULTIPOLYGON:
				return MultiPolygon.class;
			case RASTER:
			default:
				return null;
		}
	}

	/**
	 * Convert a geometry class to a layer type.
	 * 
	 * @param geometryClass
	 *            JTS geometry class
	 * @return Geomajas layer type
	 */
	public LayerType toDto(Class<? extends com.vividsolutions.jts.geom.Geometry> geometryClass) {
		if (geometryClass == LineString.class) {
			return LayerType.LINESTRING;
		} else if (geometryClass == MultiLineString.class) {
			return LayerType.MULTILINESTRING;
		} else if (geometryClass == Point.class) {
			return LayerType.POINT;
		} else if (geometryClass == MultiPoint.class) {
			return LayerType.MULTIPOINT;
		} else if (geometryClass == Polygon.class) {
			return LayerType.POLYGON;
		} else if (geometryClass == MultiPolygon.class) {
			return LayerType.MULTIPOLYGON;
		} else {
			return LayerType.GEOMETRY;
		}
	}
}
