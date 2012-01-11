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

package org.geomajas.puregwt.client.map.layer;

import junit.framework.Assert;

import org.geomajas.configuration.client.ClientMapInfo;
import org.geomajas.configuration.client.ClientVectorLayerInfo;
import org.geomajas.puregwt.client.GeomajasTestModule;
import org.geomajas.puregwt.client.event.LayerHideEvent;
import org.geomajas.puregwt.client.event.LayerShowEvent;
import org.geomajas.puregwt.client.event.LayerVisibilityHandler;
import org.geomajas.puregwt.client.event.LayerVisibilityMarkedEvent;
import org.geomajas.puregwt.client.map.ViewPort;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Test-cases for the {@link VectorLayer} class.
 * 
 * @author Pieter De Graef
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/org/geomajas/spring/geomajasContext.xml", "layerBeans1.xml", "mapBeans.xml" })
public class VectorLayerEventTest {

	private static final Injector INJECTOR = Guice.createInjector(new GeomajasTestModule());

	@Autowired
	@Qualifier(value = "mapBeans")
	private ClientMapInfo mapInfo;

	private ClientVectorLayerInfo layerInfo;

	private EventBus eventBus;

	private ViewPort viewPort;

	private int count;

	private boolean isShowing;

	@Before
	public void before() {
		eventBus = new SimpleEventBus();
		viewPort = INJECTOR.getInstance(ViewPort.class);
		viewPort.initialize(mapInfo, eventBus);
		viewPort.setMapSize(1000, 1000);
		layerInfo = (ClientVectorLayerInfo) mapInfo.getLayers().get(0);
	}

	@Test
	public void testMarkedAsVisibleEvents() {
		VectorLayer layer = new VectorLayer(layerInfo, viewPort, eventBus);
		count = 0;

		eventBus.addHandler(LayerVisibilityHandler.TYPE, new LayerVisibilityHandler() {

			public void onVisibilityMarked(LayerVisibilityMarkedEvent event) {
				count++;
			}

			public void onShow(LayerShowEvent event) {
			}

			public void onHide(LayerHideEvent event) {
			}
		});

		// Scale between 6 and 20 is OK:
		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(0)); // 32 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(0, count);

		layer.setMarkedAsVisible(true);
		Assert.assertEquals(1, count);
		layer.setMarkedAsVisible(false);
		Assert.assertEquals(2, count);
	}

	@Test
	public void testShowHideEvents() {
		VectorLayer layer = new VectorLayer(layerInfo, viewPort, eventBus);

		eventBus.addHandler(LayerVisibilityHandler.TYPE, new LayerVisibilityHandler() {

			public void onVisibilityMarked(LayerVisibilityMarkedEvent event) {
			}

			public void onShow(LayerShowEvent event) {
				isShowing = true;
			}

			public void onHide(LayerHideEvent event) {
				isShowing = false;
			}
		});

		// Scale between 6 and 20 is OK:
		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(0)); // 32 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(1)); // 16 -> OK
		Assert.assertTrue(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(2)); // 8 -> OK
		Assert.assertTrue(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(3)); // 4 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(4)); // 2 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(5)); // 1 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		// Mark as invisible:
		layer.setMarkedAsVisible(false);
		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(0)); // 32 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		viewPort.applyScale(viewPort.getZoomStrategy().getZoomStepScale(1)); // 16 -> NOK
		Assert.assertFalse(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);

		// Mark as visible while scale is within limits:
		layer.setMarkedAsVisible(true);
		Assert.assertTrue(layer.isShowing());
		Assert.assertEquals(layer.isShowing(), isShowing);
	}
}