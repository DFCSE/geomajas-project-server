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
package org.geomajas.widget.advancedviews.client.widget;

import java.util.ArrayList;
import java.util.List;

import org.geomajas.gwt.client.map.event.LayerChangedHandler;
import org.geomajas.gwt.client.map.event.LayerLabeledEvent;
import org.geomajas.gwt.client.map.event.LayerShownEvent;
import org.geomajas.gwt.client.map.event.MapModelEvent;
import org.geomajas.gwt.client.map.event.MapModelHandler;
import org.geomajas.gwt.client.map.event.MapViewChangedEvent;
import org.geomajas.gwt.client.map.event.MapViewChangedHandler;
import org.geomajas.gwt.client.map.layer.Layer;
import org.geomajas.gwt.client.map.layer.RasterLayer;
import org.geomajas.gwt.client.widget.MapWidget;
import org.geomajas.widget.advancedviews.client.util.WidgetInfoHelper;
import org.geomajas.widget.advancedviews.configuration.client.ThemesInfo;
import org.geomajas.widget.advancedviews.configuration.client.themes.LayerConfig;
import org.geomajas.widget.advancedviews.configuration.client.themes.RangeConfig;
import org.geomajas.widget.advancedviews.configuration.client.themes.ViewConfig;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.RunAsyncCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;

/**
 * Base class that handles the layer and map Changes. Contains no GUI.
 * 
 * @author Oliver May
 * @author Kristof Heirwegh
 * 
 */
public abstract class AbstractThemeWidget extends Canvas implements MapViewChangedHandler, LayerChangedHandler {

	protected MapWidget mapWidget;

	protected boolean initialized;

	protected ThemesInfo themeInfo;

	protected ViewConfigItem activeViewConfig;

	protected boolean themeChange;

	protected List<ViewConfigItem> viewConfigItems = new ArrayList<AbstractThemeWidget.ViewConfigItem>();

	public AbstractThemeWidget(MapWidget mapWidget) {
		super();
		this.mapWidget = mapWidget;

		mapWidget.getMapModel().addMapModelHandler(new MapModelHandler() {

			public void onMapModelChange(MapModelEvent event) {
				if (!initialized) {
					initialize();
				}
				initialized = true;
			}
		});
		mapWidget.getMapModel().getMapView().addMapViewChangedHandler(this);
	}

	protected void initialize() {
		themeInfo = WidgetInfoHelper.getClientWidgetInfo(ThemesInfo.IDENTIFIER, mapWidget);
		buildWidget();
		for (Layer<?> layer : mapWidget.getMapModel().getLayers()) {
			layer.addLayerChangedHandler(this);
		}
		// Set default viewConfig active is configured
		for (ViewConfigItem viewConfig : viewConfigItems) {
			if (viewConfig.getViewConfig().isActiveByDefault()) {
				activeViewConfig = viewConfig;
			}
		}
	}

	protected abstract void buildWidget();

	protected RangeConfig getRangeConfigForCurrentScale(ViewConfig viewConfig, double scale) {
		for (RangeConfig config : viewConfig.getRangeConfigs()) {
			// @todo: We convert pixelperunit, but this should be done elsewhere.
			if (config.getMaximumScale().getPixelPerUnit() == 0) {
				config.getMaximumScale().setPixelPerUnit(
						config.getMaximumScale().getNumerator()
								/ (config.getMaximumScale().getDenominator() * mapWidget.getPixelPerUnit()));
			}
			if (config.getMinimumScale().getPixelPerUnit() == 0) {
				config.getMinimumScale().setPixelPerUnit(
						config.getMinimumScale().getNumerator()
								/ (config.getMinimumScale().getDenominator() * mapWidget.getPixelPerUnit()));
			}
			double scaleMax = config.getMaximumScale().getPixelPerUnit();
			double scaleMin = config.getMinimumScale().getPixelPerUnit();
			if (scale <= scaleMax && scale >= scaleMin) {
				return config;
			}
		}
		return null;
	}

	protected void activateViewConfig(ViewConfigItem viewConfig) {
		setActiveViewConfig(viewConfig);
		if (null != viewConfig && null != viewConfig.getViewConfig()) {
			renderViewConfig(viewConfig.getViewConfig());
		}
	}

	protected ViewConfigItem getActiveViewConfig() {
		return activeViewConfig;
	}

	protected void setActiveViewConfig(ViewConfigItem viewConfig) {
		this.activeViewConfig = viewConfig;
	}

	protected void renderViewConfig(ViewConfig viewConfig) {
		themeChange = true;
		RangeConfig config = getRangeConfigForCurrentScale(viewConfig, mapWidget.getMapModel().getMapView()
				.getCurrentScale());
		
		if (themeInfo.isHideOtherlayers()) {
			for (Layer<?> layer : mapWidget.getMapModel().getLayers()) {
				layer.setVisible(false);
			}
		}
		for (LayerConfig layerConfig : config.getLayerConfigs()) {
			Layer<?> layer = mapWidget.getMapModel().getLayer(layerConfig.getLayer().getId());
			if (layer != null) {
				layer.setVisible(layerConfig.isVisible());
				if (layer instanceof RasterLayer) {
					((RasterLayer) layer).setOpacity(layerConfig.getTransparency());
				}
			} else {
				GWT.log("ThemeWidget: could not find layer: " + layerConfig.getLayer().getId());
			}
		}
		// LayerShownEvents are run async, we need to deactivate after these.
		GWT.runAsync(new RunAsyncCallback() {
			public void onSuccess() {
				themeChange = false;
			}

			public void onFailure(Throwable reason) {
			}
		});
	}

	// ----------------------------------------------------------
	// -- MapViewChangedHandler --
	// ----------------------------------------------------------

	public void onMapViewChanged(MapViewChangedEvent event) {
		if (null != activeViewConfig && !event.isSameScaleLevel()) {
			renderViewConfig(activeViewConfig.getViewConfig());
		}
	}

	// ----------------------------------------------------------
	// -- LayerChangedHandler --
	// ----------------------------------------------------------

	public void onVisibleChange(LayerShownEvent event) {
		if (!themeChange && getActiveViewConfig() != null && !event.isScaleChange()) {
			activateViewConfig(null);
		}
	}

	public void onLabelChange(LayerLabeledEvent event) {
		// ignore
	}

	/**
	 * Model for Viewconfig.
	 * 
	 * @author Oliver May
	 * 
	 */
	protected static class ViewConfigItem {
		protected ViewConfig viewConfig;
		protected IButton button;

		public ViewConfig getViewConfig() {
			return viewConfig;
		}

		public void setViewConfig(ViewConfig viewConfig) {
			this.viewConfig = viewConfig;
		}

		public IButton getButton() {
			return button;
		}

		public void setButton(IButton button) {
			this.button = button;
		}
	}
}
