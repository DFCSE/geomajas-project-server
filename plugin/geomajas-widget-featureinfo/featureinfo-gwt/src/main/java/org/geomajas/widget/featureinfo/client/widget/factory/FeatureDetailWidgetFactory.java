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
package org.geomajas.widget.featureinfo.client.widget.factory;

import org.geomajas.configuration.client.ClientWidgetInfo;
import org.geomajas.gwt.client.i18n.I18nProvider;
import org.geomajas.gwt.client.map.MapView.ZoomOption;
import org.geomajas.gwt.client.map.feature.Feature;
import org.geomajas.gwt.client.map.layer.Layer;
import org.geomajas.gwt.client.map.layer.VectorLayer;
import org.geomajas.gwt.client.spatial.Bbox;
import org.geomajas.gwt.client.util.WidgetLayout;
import org.geomajas.gwt.client.widget.FeatureAttributeWindow;
import org.geomajas.widget.featureinfo.client.widget.DockableWindow;
import org.geomajas.widget.featureinfo.configuration.client.WidgetBuilderInfo;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.toolbar.ToolStrip;

/**
 * With FeatureDetailWidgetFactory you can get FeatureDetail widgets in a
 * general manner. The effective type used depends on configuration.
 * 
 * @author Kristof Heirwegh
 * @author Oliver May
 */
public final class FeatureDetailWidgetFactory {

	private static boolean SELECT_ON_ZOOM = true;

	private FeatureDetailWidgetFactory() {
		// utility class, hide constructor
	}

	public static Window createFeatureDetailWindow(Feature feature, Layer layer, boolean editingAllowed) {
		FeatureDetailWidgetBuilder customBuilder = getCustomBuilder(layer);
		if (customBuilder != null) {
			Window w = customBuilder.createFeatureDetailWindow(feature, editingAllowed);
			if (!(w instanceof DockableWindow)) {
				DockableWindow.mixin(w);
			}
			return w;
		} else if (layer instanceof VectorLayer) {
			FeatureAttributeWindow w = new FeatureAttributeWindow(feature, editingAllowed);
			customize(w, feature);
			return w;
		} else {
			return new RasterLayerAttributeWindow(feature, layer);
		}
	}

	public static Window createFeatureDetailWindow(Feature feature, boolean editingAllowed) {
		return createFeatureDetailWindow(feature, feature.getLayer(), editingAllowed);
	}

	public static boolean isSelectOnZoom() {
		return SELECT_ON_ZOOM;
	}

	/**
	 * Should the feature be selected when zooming to it?.
	 * 
	 * @param selectOnZoom include select on zoom?
	 */
	public static void setSelectOnZoom(boolean selectOnZoom) {
		FeatureDetailWidgetFactory.SELECT_ON_ZOOM = selectOnZoom;
	}

	// ----------------------------------------------------------

	private static FeatureDetailWidgetBuilder getCustomBuilder(Layer layer) {
		FeatureDetailWidgetBuilder b = null;
		try {
			if (layer != null) {
				ClientWidgetInfo cwi = layer.getLayerInfo().getWidgetInfo(WidgetBuilderInfo.IDENTIFIER);
				if (cwi != null) {
					if (cwi instanceof WidgetBuilderInfo) {
						WidgetBuilderInfo lcfdii = (WidgetBuilderInfo) cwi;
						WidgetBuilder wb = WidgetFactory.get(lcfdii);
						if (wb instanceof FeatureDetailWidgetBuilder) {
							return (FeatureDetailWidgetBuilder) wb;
						} else {
							GWT.log("Builder is not of type FeatureDetailWidgetBuilder: " + lcfdii.getBuilderName());
						}
					} else {
						GWT.log("ClientWidgetInfo is not of class type LayerCustomFeatureDetailInfoInfo!! (layer type: "
								+ layer.getServerLayerId() + ")");
					}
				}
			}
		} catch (Exception e) {
			GWT.log("Error getting custom detail widget: " + e.getMessage());
		}
		return b;
	}

	// ----------------------------------------------------------

	private static void customize(FeatureAttributeWindow window, Feature feature) {
		DockableWindow.mixin(window);
		if (isSelectOnZoom()) {
			ToolStrip ts = window.getToolStrip();
			Canvas editBtn = null;
			// hackety hack
			for (Canvas c : ts.getMembers()) {
				if (c instanceof IButton && WidgetLayout.iconEdit.equals(((IButton) c).getIcon())) {
					editBtn = c;
				}
			}
			ts.removeMembers(ts.getMembers()); // clear
			ts.addMember(new SelectingZoomButton(feature));
			if (editBtn != null) {
				ts.addSpacer(2);
				ts.addMember(editBtn);
			}
		}
	}

	/** Definition of the zoom button that zooms to the feature on the map. */
	private static class SelectingZoomButton extends IButton implements com.smartgwt.client.widgets.events.ClickHandler 
	{
		private Feature feature;

		public SelectingZoomButton(Feature feature) {
			setIcon("[ISOMORPHIC]/geomajas/osgeo/zoom-selection.png");
			setShowDisabledIcon(false);
			setTitle(I18nProvider.getAttribute().btnZoomFeature());
			setTooltip(I18nProvider.getAttribute().btnZoomTooltip());
			addClickHandler(this);
			setWidth(150);
			this.feature = feature;
		}

		public void onClick(ClickEvent event) {
			Bbox bounds = feature.getGeometry().getBounds();
			feature.getLayer().getMapModel().getMapView().applyBounds(bounds, ZoomOption.LEVEL_FIT);
			feature.getLayer().getFeatureStore().addFeature(feature);
			feature.getLayer().selectFeature(feature);
		}
	}
}
