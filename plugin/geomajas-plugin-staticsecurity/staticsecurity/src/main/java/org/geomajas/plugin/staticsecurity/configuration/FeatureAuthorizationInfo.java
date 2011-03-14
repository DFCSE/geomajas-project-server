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

package org.geomajas.plugin.staticsecurity.configuration;

import org.geomajas.global.Api;
import org.geomajas.layer.feature.InternalFeature;
import org.geomajas.security.BaseAuthorization;
import org.geomajas.security.FeatureAuthorization;

import java.util.List;
import java.util.Map;

/**
 * Allow configuring authorizations, with feature level authorizations.
 *
 * @author Joachim Van der Auwera
 * @since 1.6.0
 */
@Api(allMethods = true)
public class FeatureAuthorizationInfo extends LayerAuthorizationInfo {

	private Map<String, LayerFeatureAuthorizationInfo> layers;

	/**
	 * Set feature authorizations per layer.
	 *
	 * @return feature authorizations per layer
	 */
	public Map<String, LayerFeatureAuthorizationInfo> getLayers() {
		return layers;
	}

	/**
	 * Get the feature authorizations per layer.
	 *
	 * @param layers feature authorizations per layer
	 */
	public void setLayers(Map<String, LayerFeatureAuthorizationInfo> layers) {
		this.layers = layers;
	}

	@Override
	public BaseAuthorization getAuthorization() {
		return new LocalAuthorization(this);
	}

	/**
	 * Authorization implementation class.
	 */
	private static final class LocalAuthorization extends LayerAuthorization implements FeatureAuthorization {

		private FeatureAuthorizationInfo info;

		public LocalAuthorization(FeatureAuthorizationInfo info) {
			super(info);
			this.info = info;
		}

		public boolean isFeatureVisible(String layerId, InternalFeature feature) {
			LayerFeatureAuthorizationInfo layer = info.getLayers().get(layerId);
			return layer != null && check(feature, layer.getVisibleIncludes(), layer.getVisibleExcludes());
		}

		public boolean isFeatureUpdateAuthorized(String layerId, InternalFeature feature) {
			LayerFeatureAuthorizationInfo layer = info.getLayers().get(layerId);
			return layer != null &&
					check(feature, layer.getUpdateAuthorizedIncludes(), layer.getUpdateAuthorizedExcludes());
		}

		public boolean isFeatureUpdateAuthorized(String layerId, InternalFeature orgFeature,
												 InternalFeature newFeature) {
			LayerFeatureAuthorizationInfo layer = info.getLayers().get(layerId);
			return layer != null &&
					check(newFeature, layer.getUpdateAuthorizedIncludes(), layer.getUpdateAuthorizedExcludes());
		}

		public boolean isFeatureDeleteAuthorized(String layerId, InternalFeature feature) {
			LayerFeatureAuthorizationInfo layer = info.getLayers().get(layerId);
			return layer != null &&
					check(feature, layer.getDeleteAuthorizedIncludes(), layer.getDeleteAuthorizedExcludes());
		}

		public boolean isFeatureCreateAuthorized(String layerId, InternalFeature feature) {
			LayerFeatureAuthorizationInfo layer = info.getLayers().get(layerId);
			return layer != null && layer.isCreateAuthorized();
		}

		private boolean check(InternalFeature feature, List<String> includes, List<String> excludes) {
			return check(feature.getId(), includes) && !check(feature.getId(), excludes);
		}
	}
}
