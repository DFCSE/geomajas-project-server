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
package org.geomajas.layer.wms;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.PostConstruct;

import org.geomajas.configuration.Parameter;
import org.geomajas.configuration.RasterLayerInfo;
import org.geomajas.configuration.client.ScaleInfo;
import org.geomajas.geometry.Bbox;
import org.geomajas.geometry.Crs;
import org.geomajas.geometry.CrsTransform;
import org.geomajas.global.Api;
import org.geomajas.global.ExceptionCode;
import org.geomajas.global.GeomajasException;
import org.geomajas.layer.LayerException;
import org.geomajas.layer.RasterLayer;
import org.geomajas.layer.feature.Attribute;
import org.geomajas.layer.feature.Feature;
import org.geomajas.layer.feature.attribute.StringAttribute;
import org.geomajas.layer.tile.RasterTile;
import org.geomajas.layer.tile.TileCode;
import org.geomajas.service.DispatcherUrlService;
import org.geomajas.service.DtoConverterService;
import org.geomajas.service.GeoService;
import org.geotools.GML;
import org.geotools.GML.Version;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;

/**
 * <p>
 * Layer model for accessing raster data from WMS servers. It has support for most WMS versions: up to 1.3.0. When using
 * this layer, note that the following fields are required:
 * <ul>
 * <li><b>baseWmsUrl</b>: The base URL to the WMS server.</li>
 * <li><b>format</b>: The format for the returned images.</li>
 * <li><b>version</b>: The version of WMS to use.</li>
 * <li><b>styles</b>: The styles to use when rendering the WMS images.</li>
 * <li><b>useProxy</b>: Set to true to use a proxy for rendering the WMS, hiding the URL from the client.
 * This automatically happens when setting the authentication object.</li>
 * </ul>
 * There always is the option of adding additional parameters to the WMS GetMap requests, by filling the
 * <code>parameters</code> list. Such parameters could include optional WMS GetMap parameters, such as "transparency",
 * but also "user" and "password".
 * </p>
 * <p>
 * This layer also supports BASIC and DIGEST authentication. To use this functionality, set the
 * <code>authentication</code> field.
 * </p>
 *
 * @author Jan De Moerloose
 * @author Pieter De Graef
 * @author Oliver May
 * @author Joachim Van der Auwera
 * @since 1.7.1
 */
@Api
public class WmsLayer implements RasterLayer, LayerFeatureInfoSupport {

	private final Logger log = LoggerFactory.getLogger(WmsLayer.class);

	private List<Resolution> resolutions = new ArrayList<Resolution>();

	// @NotNull this seems to cause problems, it is tested in @PostConstruct anyway
	private String baseWmsUrl;

	private String format = "image/png";
	private String version = "1.1.1";
	private String styles = "";
	private List<Parameter> parameters;

	private boolean enableFeatureInfoSupport;

	private RasterLayerInfo layerInfo;

	private Crs crs;

	private String id;

	private WmsAuthentication authentication;

	private boolean useProxy;

	@Autowired
	private GeoService geoService;

	@Autowired
	private WmsHttpService httpService;

	@Autowired
	private DtoConverterService converterService;

	@Autowired(required = false)
	private DispatcherUrlService dispatcherUrlService;

	/**
	 * Return the layers identifier.
	 *
	 * @return layer id
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the layer identifier.
	 *
	 * @param id layer id
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Return the layer info object.
	 *
	 * @since 1.7.1
	 */
	public RasterLayerInfo getLayerInfo() {
		return layerInfo;
	}

	/**
	 * Get coordinate reference system for this layer.
	 *
	 * @return Coordinate reference system for this layer.
	 * @deprecated use {@link org.geomajas.layer.LayerService#getCrs(org.geomajas.layer.Layer)}
	 */
	@Deprecated
	public CoordinateReferenceSystem getCrs() {
		return crs;
	}

	@PostConstruct
	protected void postConstruct() throws GeomajasException {
		if (null == baseWmsUrl) {
			throw new GeomajasException(ExceptionCode.PARAMETER_MISSING, "baseWmsUrl");
		}

		crs = geoService.getCrs2(getLayerInfo().getCrs());

		// calculate resolutions
		List<ScaleInfo> scales = layerInfo.getZoomLevels();
		if (scales != null) {
			List<Double> r = new ArrayList<Double>();
			for (ScaleInfo scale : scales) {
				r.add(1. / scale.getPixelPerUnit());
			}

			// sort in decreasing order !!!
			Collections.sort(r);
			Collections.reverse(r);

			int level = 0;
			for (double resolution : r) {
				resolutions.add(
						new Resolution(resolution, level++, layerInfo.getTileWidth(), layerInfo.getTileHeight()));
			}
		}
	}

	/**
	 * Set the layer configuration.
	 *
	 * @param layerInfo layer information
	 * @throws LayerException oops
	 * @since 1.7.1
	 */
	@Api
	public void setLayerInfo(RasterLayerInfo layerInfo) throws LayerException {
		this.layerInfo = layerInfo;
	}

	/** {@inheritDoc}. */
	public List<Feature> getFeaturesByLocation(Coordinate layerCoordinate, double layerScale, int pixelTolerance)
			throws LayerException {
		if (!isEnableFeatureInfoSupport()) {
			return Collections.emptyList();
		}

		List<Feature> features = new ArrayList<Feature>();

		Resolution bestResolution = getResolutionForScale(layerScale);
		RasterGrid grid = getRasterGrid(new Envelope(layerCoordinate), bestResolution.getTileWidth(),
				bestResolution.getTileHeight(), layerScale);
		int x = (int) (((layerCoordinate.x - grid.getLowerLeft().x) * bestResolution.getTileWidthPx())
				/ grid.getTileWidth());
		int y = (int) (bestResolution.getTileHeightPx() - (((layerCoordinate.y - grid.getLowerLeft().y) *
				bestResolution .getTileHeightPx()) / grid.getTileHeight()));

		Bbox layerBox = new Bbox(grid.getLowerLeft().x, grid.getLowerLeft().y,
				grid.getTileWidth(), grid.getTileHeight());

		InputStream stream = null;
		try {
			String url = formatGetFeatureInfoUrl(bestResolution.getTileWidthPx(), bestResolution.getTileHeightPx(),
					layerBox, x, y);
			log.debug("getFeaturesByLocation: {} {} {} {}",
					new Object[] {layerCoordinate, layerScale, pixelTolerance, url});
			GML gml = new GML(Version.GML3);

			stream = httpService.getStream(url, getAuthentication());
			FeatureCollection<?, SimpleFeature> collection = gml.decodeFeatureCollection(stream);
			FeatureIterator<SimpleFeature> it = collection.features();

			while (it.hasNext()) {
				features.add(toDto(it.next()));
			}
		} catch (LayerException le) {
			throw le;
		} catch (Exception e) {
			throw new LayerException(e, ExceptionCode.UNEXPECTED_PROBLEM);
		} finally {
			if (null != stream) {
				try {
					stream.close();
				} catch (IOException ioe) {
					// ignore, closing anyway
				}
			}
		}

		return features;
	}

	private Feature toDto(SimpleFeature feature) {
		if (feature == null) {
			return null;
		}
		Feature dto = new Feature(feature.getID());

		HashMap<String, Attribute> attributes = new HashMap<String, Attribute>();

		for (AttributeDescriptor desc : feature.getType().getAttributeDescriptors()) {
			Object obj = feature.getAttribute(desc.getName());
			if (null != obj) {
				attributes.put(desc.getLocalName(), new StringAttribute(obj.toString()));
			}
		}
		dto.setAttributes(attributes);
		dto.setId(feature.getID());

		dto.setUpdatable(false);
		dto.setDeletable(false);
		return dto;

	}

	/**
	 * Paints the specified bounds optimized for the specified scale in pixel/unit.
	 *
	 * @param targetCrs Coordinate reference system used for bounds
	 * @param bounds bounds to request images for
	 * @param scale scale or zoom level (unit?)
	 * @return a list of raster images that covers the bounds
	 * @throws GeomajasException oops
	 */
	public List<RasterTile> paint(CoordinateReferenceSystem targetCrs, Envelope bounds, double scale)
			throws GeomajasException {
		Envelope layerBounds = bounds;
		double layerScale = scale;
		CrsTransform layerToMap = null;
		boolean needTransform = !crs.equals(targetCrs);

		try {
			// We don't necessarily need to split into same CRS and different CRS cases, the latter implementation uses
			// identity transform if crs's are equal for map and layer but might introduce bugs in rounding and/or
			// conversions.
			if (needTransform) {
				layerToMap = geoService.getCrsTransform(crs, targetCrs);
				CrsTransform mapToLayer = geoService.getCrsTransform(targetCrs, crs);

				// Translate the map coordinates to layer coordinates, assumes equal x-y orientation
				layerBounds = geoService.transform(bounds, mapToLayer);
				layerScale = bounds.getWidth() * scale / layerBounds.getWidth();
			}
		} catch (MismatchedDimensionException e) {
			throw new GeomajasException(e, ExceptionCode.RENDER_DIMENSION_MISMATCH);
		}
		layerBounds = clipBounds(layerBounds);
		if (layerBounds.isNull()) {
			return new ArrayList<RasterTile>(0);
		}

		// Grid is in layer coordinate space!
		Resolution bestResolution = getResolutionForScale(layerScale);
		RasterGrid grid = getRasterGrid(layerBounds, bestResolution.getTileWidth(),
				bestResolution.getTileHeight(), layerScale);

		// We calculate the first tile's screen box with this assumption
		List<RasterTile> result = new ArrayList<RasterTile>();
		for (int i = grid.getXmin(); i < grid.getXmax(); i++) {
			for (int j = grid.getYmin(); j < grid.getYmax(); j++) {
				double x = grid.getLowerLeft().x + (i - grid.getXmin()) * grid.getTileWidth();
				double y = grid.getLowerLeft().y + (j - grid.getYmin()) * grid.getTileHeight();
				// layer coordinates
				Bbox worldBox;
				Bbox layerBox;
				if (needTransform) {
					layerBox = new Bbox(x, y, grid.getTileWidth(), grid.getTileHeight());
					// Transforming back to map coordinates will only result in a proper grid if the transformation
					// is nearly affine
					worldBox = geoService.transform(layerBox, layerToMap);
				} else {
					worldBox = new Bbox(x, y, grid.getTileWidth(), grid.getTileHeight());
					layerBox = worldBox;
				}
				// Rounding to avoid white space between raster tiles lower-left becomes upper-left in inverted y-space
				Bbox screenBox = new Bbox(Math.round(scale * worldBox.getX()), -Math.round(scale * worldBox.getMaxY()),
						Math.round(scale * worldBox.getMaxX()) - Math.round(scale * worldBox.getX()), Math.round(scale
						* worldBox.getMaxY())
						- Math.round(scale * worldBox.getY()));

				RasterTile image = new RasterTile(screenBox, getId() + "." + bestResolution.getLevel() + "." + i
						+ "," + j);

				image.setCode(new TileCode(bestResolution.getLevel(), i, j));
				String url = formatUrl(bestResolution.getTileWidthPx(), bestResolution.getTileHeightPx(), layerBox);
				image.setUrl(url);
				result.add(image);
			}
		}

		return result;
	}

	private String getWmsTargetUrl() {
		if (useProxy || null != authentication) {
			if (null != dispatcherUrlService) {
				String url = dispatcherUrlService.getDispatcherUrl();
				if (!url.endsWith("/")) {
					url += "/";
				}
				return url + "wms/" + getId() + "/";
			} else {
				return "./d/wms/" + getId() + "/";
			}
		} else {
			return baseWmsUrl;
		}
	}

	private String formatGetFeatureInfoUrl(int width, int height, Bbox box, int x, int y) throws GeomajasException {
		// Always use direct url
		StringBuilder url = formatBaseUrl(baseWmsUrl, width, height, box);
		String layers = getId();
		if (layerInfo.getDataSourceName() != null) {
			layers = layerInfo.getDataSourceName();
		}
		url.append("&QUERY_LAYERS=");
		url.append(layers);
		url.append("&request=GetFeatureInfo");
		url.append("&X=");
		url.append(Integer.toString(x));
		url.append("&Y=");
		url.append(Integer.toString(y));
		url.append("&INFO_FORMAT=application/vnd.ogc.gml");
		return url.toString();
	}

	private String formatUrl(int width, int height, Bbox box) throws GeomajasException {
		StringBuilder url = formatBaseUrl(getWmsTargetUrl(), width, height, box);
		url.append("&request=GetMap");
		return url.toString();
	}

	/**
	 * Build the base part of the url (doesn't change for getMap or getFeatureInfo requests).
	 *
	 * @param targetUrl base url
	 * @param width image width
	 * @param height image height
	 * @param box bounding box
	 * @return base WMS url
	 * @throws GeomajasException missing parameter
	 */
	private StringBuilder formatBaseUrl(String targetUrl, int width, int height, Bbox box) throws GeomajasException {
		StringBuilder url = new StringBuilder(targetUrl);
		int pos = url.lastIndexOf("?");
		if (pos > 0) {
			url.append("&SERVICE=WMS");
		} else {
			url.append("?SERVICE=WMS");
		}
		String layers = getId();
		if (layerInfo.getDataSourceName() != null) {
			layers = layerInfo.getDataSourceName();
		}
		url.append("&layers=");
		url.append(layers);
		url.append("&WIDTH=");
		url.append(Integer.toString(width));
		url.append("&HEIGHT=");
		url.append(Integer.toString(height));
		DecimalFormat decimalFormat = new DecimalFormat(); // create new as this is not thread safe
		decimalFormat.setDecimalSeparatorAlwaysShown(false);
		decimalFormat.setGroupingUsed(false);
		decimalFormat.setMinimumFractionDigits(0);
		decimalFormat.setMaximumFractionDigits(100);
		DecimalFormatSymbols symbols = new DecimalFormatSymbols();
		symbols.setDecimalSeparator('.');
		decimalFormat.setDecimalFormatSymbols(symbols);

		url.append("&bbox=");
		url.append(decimalFormat.format(box.getX()));
		url.append(",");
		url.append(decimalFormat.format(box.getY()));
		url.append(",");
		url.append(decimalFormat.format(box.getMaxX()));
		url.append(",");
		url.append(decimalFormat.format(box.getMaxY()));
		url.append("&format=");
		url.append(format);
		url.append("&version=");
		url.append(version);
		if ("1.3.0".equals(version)) {
			url.append("&crs=");
		} else {
			url.append("&srs=");
		}
		url.append(layerInfo.getCrs());
		url.append("&styles=" );
		url.append(styles);
		if (null != parameters) {
			for (Parameter p : parameters) {
				url.append("&");
				url.append(p.getName());
				url.append("=");
				url.append(p.getValue());
			}
		}
		return url;
	}

	private Resolution getResolutionForScale(double scale) {
		if (null == resolutions || resolutions.size() == 0) {
			return calculateBestQuadTreeResolution(scale);
		} else {
			double screenResolution = 1.0 / scale;
			if (screenResolution >= resolutions.get(0).getResolution()) {
				return resolutions.get(0);
			} else if (screenResolution <= resolutions.get(resolutions.size() - 1).getResolution()) {
				return resolutions.get(resolutions.size() - 1);
			} else {
				for (int i = 0; i < resolutions.size() - 1; i++) {
					Resolution upper = resolutions.get(i);
					Resolution lower = resolutions.get(i + 1);
					if (screenResolution <= upper.getResolution() && screenResolution >= lower.getResolution()) {
						if ((upper.getResolution() - screenResolution) >
								2 * (screenResolution - lower.getResolution())) {
							return lower;
						} else {
							return upper;
						}
					}
				}
			}
		}
		// should not occur !!!!
		return resolutions.get(resolutions.size() - 1);
	}

	private Resolution calculateBestQuadTreeResolution(double scale) {
		double screenResolution = 1.0 / scale;
		// based on quad tree created by subdividing the maximum extent
		Bbox bbox = layerInfo.getMaxExtent();
		double maxWidth = bbox.getWidth();
		double maxHeight = bbox.getHeight();

		int tileWidth = layerInfo.getTileWidth();
		int tileHeight = layerInfo.getTileHeight();

		Resolution upper = new Resolution(Math.max(maxWidth / tileWidth, maxHeight / tileHeight), 0, tileWidth,
				tileHeight);
		if (screenResolution >= upper.getResolution()) {
			return upper;
		} else {
			int level = 0;
			Resolution lower = upper; // set value to avoid possible NPE
			while (screenResolution < upper.getResolution()) {
				lower = upper;
				level++;
				double width = maxWidth / Math.pow(2, level);
				double height = maxHeight / Math.pow(2, level);
				upper = new Resolution(Math.max(width / tileWidth, height / tileHeight), level, tileWidth,
						tileHeight);
			}
			if ((screenResolution - upper.getResolution()) > 2 * (lower.getResolution() - screenResolution)) {
				return lower;
			} else {
				return upper;
			}
		}
	}

	private RasterGrid getRasterGrid(Envelope bounds, double width, double height, double scale) {
		// slightly adjust the width and height so it becomes integer for the
		// current scale
		double realWidth = Math.round(width * scale) / scale;
		double realHeight =  Math.round(height * scale) / scale;

		Bbox bbox = getLayerInfo().getMaxExtent();
		int ymin = (int) Math.floor((bounds.getMinY() - bbox.getY()) / realHeight);
		int ymax = (int) Math.ceil((bounds.getMaxY() - bbox.getY()) / realHeight);
		int xmin = (int) Math.floor((bounds.getMinX() - bbox.getX()) / realWidth);
		int xmax = (int) Math.ceil((bounds.getMaxX() - bbox.getX()) / realWidth);
		// same adjustment for corner
		double realXmin = ((int) (bbox.getX() * scale)) / scale;
		double realYmin = ((int) (bbox.getY() * scale)) / scale;
		Coordinate lowerLeft = new Coordinate(realXmin + xmin * realWidth, realYmin + ymin * realHeight);
		return new RasterGrid(lowerLeft, xmin, ymin, xmax, ymax, realWidth, realHeight);
	}

	private Envelope clipBounds(Envelope bounds) {
		Envelope maxExtent = converterService.toInternal(layerInfo.getMaxExtent());
		return bounds.intersection(maxExtent);
	}

	public String getBaseWmsUrl() {
		return baseWmsUrl;
	}

	/**
	 * Set the base URL to the WMS server.
	 *
	 * @param baseWmsUrl base URL for the WMS server
	 * @since 1.7.1
	 */
	@Api
	public void setBaseWmsUrl(String baseWmsUrl) {
		this.baseWmsUrl = baseWmsUrl;
	}

	public String getFormat() {
		return format;
	}

	/**
	 * Set file format to request.
	 *
	 * @param format file format. For allowed values, check your WMS server.
	 * @since 1.7.1
	 */
	@Api
	public void setFormat(String format) {
		this.format = format;
	}

	/**
	 * Set WMS version to use.
	 *
	 * @param version WMS version. For allowed values, check your WMS server.
	 * @since 1.7.1
	 */
	@Api
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Set the styles.
	 *
	 * @param styles styles. For allowed values, check your WMS server.
	 * @since 1.7.1
	 */
	@Api
	public void setStyles(String styles) {
		this.styles = styles;
	}

	/**
	 * Set additional parameters to include in all WMS <code>getMap</code> requests.
	 *
	 * @param parameters parameters. For possible keys and values, check your WMS server.
	 * @since 1.7.1
	 */
	@Api
	public void setParameters(List<Parameter> parameters) {
		this.parameters = parameters;
	}

	/**
	 * Get the authentication object.
	 *
	 * @return authentication object
	 */
	public WmsAuthentication getAuthentication() {
		return authentication;
	}

	/**
	 * <p>
	 * Set the authentication object. This configuration object provides support for basic and digest HTTP
	 * authentication on the WMS server. If no HTTP authentication is required, leave this empty.
	 * </p>
	 * <p>
	 * Note that there is still the option of adding a user name and password as HTTP parameters, as some WMS server
	 * support. To do that, just add <code>parameters</code>.
	 * </p>
	 *
	 * @param authentication authentication object
	 * @since 1.8.0
	 */
	@Api
	public void setAuthentication(WmsAuthentication authentication) {
		this.authentication = authentication;
	}

	/**
	 * Set whether the WMS request should use a proxy. This is automatically done when the authentication object is set.
	 * When the WMS request is proxied, the credentials and WMS base address are hidden from the client.
	 *
	 * @param useProxy true when request needs to use the proxy
	 * @since 1.8.0
	 */
	@Api
	public void setUseProxy(boolean useProxy) {
		this.useProxy = useProxy;
	}

	/**
	 * Set whether the WMS layer should support feature info support. This allows to retrieve feature info from a
	 * raster layer. This only makes sense if the WMS layer is based on some kind of feature store like a database.
	 *
	 * @param enableFeatureInfoSupport whether feature info support is enabled for this layer
	 * @since 1.9.0
	 */
	@Api
	public void setEnableFeatureInfoSupport(boolean enableFeatureInfoSupport) {
		this.enableFeatureInfoSupport = enableFeatureInfoSupport;
	}

	/**
	 * Get whether the WMS layer should support feature info support.
	 *
	 * @return the enableFeatureInfoSupport true if feature info support is enabled
	 */
	public boolean isEnableFeatureInfoSupport() {
		return enableFeatureInfoSupport;
	}

	/**
	 * Grid definition for a WMS layer. It is used internally in the WMS layer.
	 *
	 * @author Jan De Moerloose
	 * @author Pieter De Graef
	 */
	private static class RasterGrid {

		private Coordinate lowerLeft;

		private int xmin;

		private int ymin;

		private int xmax;

		private int ymax;

		private double tileWidth;

		private double tileHeight;

		RasterGrid(Coordinate lowerLeft, int xmin, int ymin, int xmax, int ymax, double tileWidth, double tileHeight) {
			super();
			this.lowerLeft = lowerLeft;
			this.xmin = xmin;
			this.ymin = ymin;
			this.xmax = xmax;
			this.ymax = ymax;
			this.tileWidth = tileWidth;
			this.tileHeight = tileHeight;
		}

		public Coordinate getLowerLeft() {
			return lowerLeft;
		}

		public double getTileHeight() {
			return tileHeight;
		}

		public double getTileWidth() {
			return tileWidth;
		}

		public int getXmax() {
			return xmax;
		}

		public int getXmin() {
			return xmin;
		}

		public int getYmax() {
			return ymax;
		}

		public int getYmin() {
			return ymin;
		}
	}

	/**
	 * Single resolution definition for a WMS layer. This class is used internally in the WMS layer, and therefore has
	 * no public constructors.
	 *
	 * @author Jan De Moerloose
	 * @author Pieter De Graef
	 */
	private static class Resolution {

		private double resolution;

		private int level;

		private int tileWidth;

		private int tileHeight;

		/**
		 * Constructor that immediately requires all fields.
		 *
		 * @param resolution The actual resolution value. This is the reverse of the scale.
		 * @param level The level in the quad tree.
		 * @param tileWidth The width of a tile at the given tile level.
		 * @param tileHeight The height of a tile at the given tile level.
		 */
		Resolution(double resolution, int level, int tileWidth, int tileHeight) {
			this.resolution = resolution;
			this.level = level;
			this.tileWidth = tileWidth;
			this.tileHeight = tileHeight;
		}

		public int getLevel() {
			return level;
		}

		public int getTileHeightPx() {
			return tileHeight;
		}

		public int getTileWidthPx() {
			return tileWidth;
		}

		public double getTileHeight() {
			return tileHeight * resolution;
		}

		public double getTileWidth() {
			return tileWidth * resolution;
		}

		public double getResolution() {
			return resolution;
		}
	}

}