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

package org.geomajas.puregwt.client.map;

import java.util.ArrayList;
import java.util.List;

import org.geomajas.configuration.client.ClientLayerInfo;
import org.geomajas.configuration.client.ClientMapInfo;
import org.geomajas.puregwt.client.map.event.EventBus;
import org.geomajas.puregwt.client.map.event.EventBusImpl;
import org.geomajas.puregwt.client.map.event.LayerAddedEvent;
import org.geomajas.puregwt.client.map.event.LayerDeselectedEvent;
import org.geomajas.puregwt.client.map.event.LayerOrderChangedEvent;
import org.geomajas.puregwt.client.map.event.LayerOrderChangedHandler;
import org.geomajas.puregwt.client.map.event.LayerRemovedEvent;
import org.geomajas.puregwt.client.map.event.LayerSelectedEvent;
import org.geomajas.puregwt.client.map.event.LayerSelectionHandler;
import org.geomajas.puregwt.client.map.event.MapCompositionHandler;
import org.geomajas.puregwt.client.map.layer.Layer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Tests for the layersModelImpl class to see if it fires the correct events.
 * 
 * @author Pieter De Graef
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/org/geomajas/spring/geomajasContext.xml", "beansContext.xml", "mapBeans.xml",
		"layerBeans1.xml", "layerBeans2.xml", "layerBeans3.xml" })
public class LayersModelEventTest {

	private static final String LAYER1 = "beans1Layer";

	private static final String LAYER2 = "beans2Layer";

	private static final String LAYER3 = "beans3Layer";

	@Autowired
	@Qualifier(value = "mapBeans")
	private ClientMapInfo mapInfo;

	private EventBus eventBus = new EventBusImpl();

	private int layerCount;

	private String selectId;

	private String deselectId;

	private int fromIndex;

	private int toIndex;

	@Before
	public void checkLayerOrder() {
		List<ClientLayerInfo> layers = new ArrayList<ClientLayerInfo>();
		for (int i = 1; i < 4; i++) {
			for (ClientLayerInfo layerInfo : mapInfo.getLayers()) {
				if (("beans" + i + "Layer").equals(layerInfo.getId())) {
					layers.add(layerInfo);
				}
			}
		}
		mapInfo.setLayers(layers);
	}

	@Test
	public void testInitialize() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		final MapCompositionHandler layerCounter = new MapCompositionHandler() {

			public void onLayerAdded(LayerAddedEvent event) {
				layerCount++;
			}

			public void onLayerRemoved(LayerRemovedEvent event) {
				layerCount--;
			}
		};
		eventBus.addHandler(MapCompositionHandler.TYPE, layerCounter);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));
		Assert.assertEquals(3, layerCount);
		Assert.assertEquals(3, layersModel.getLayerCount());
	}

	@Test
	public void testLayerSelection() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));
		Layer<?> layer1 = layersModel.getLayer(LAYER1);
		Layer<?> layer2 = layersModel.getLayer(LAYER2);

		selectId = null;
		deselectId = null;

		HandlerRegistration reg = eventBus.addHandler(LayerSelectionHandler.TYPE, new LayerSelectionHandler() {

			public void onSelectLayer(LayerSelectedEvent event) {
				selectId = event.getLayer().getId();
			}

			public void onDeselectLayer(LayerDeselectedEvent event) {
				deselectId = event.getLayer().getId();
			}
		});

		layer1.setSelected(true);
		Assert.assertEquals(LAYER1, selectId);
		Assert.assertNull(deselectId);

		layer2.setSelected(true);
		Assert.assertEquals(LAYER1, deselectId);
		Assert.assertEquals(LAYER2, selectId);

		layer2.setSelected(false);
		Assert.assertEquals(LAYER2, deselectId);
		Assert.assertEquals(LAYER2, selectId);

		reg.removeHandler();
	}

	@Test
	public void testMoveLayerDown() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));
		Layer<?> layer1 = layersModel.getLayer(LAYER1);
		Layer<?> layer3 = layersModel.getLayer(LAYER3);

		fromIndex = 342;
		toIndex = 342;

		HandlerRegistration reg = eventBus.addHandler(LayerOrderChangedHandler.TYPE, new LayerOrderChangedHandler() {

			public void onLayerOrderChanged(LayerOrderChangedEvent event) {
				fromIndex = event.getFromIndex();
				toIndex = event.getToIndex();
			}
		});

		layersModel.moveLayerDown(layer1); // Expect no changes, and so no event.
		Assert.assertEquals(342, fromIndex);
		Assert.assertEquals(342, toIndex);

		layersModel.moveLayerDown(layer3);
		Assert.assertEquals(2, fromIndex);
		Assert.assertEquals(1, toIndex);

		reg.removeHandler();
	}

	@Test
	public void testMoveLayerUp() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));
		Layer<?> layer1 = layersModel.getLayer(LAYER1);
		Layer<?> layer3 = layersModel.getLayer(LAYER3);

		fromIndex = 342;
		toIndex = 342;

		HandlerRegistration reg = eventBus.addHandler(LayerOrderChangedHandler.TYPE, new LayerOrderChangedHandler() {

			public void onLayerOrderChanged(LayerOrderChangedEvent event) {
				fromIndex = event.getFromIndex();
				toIndex = event.getToIndex();
			}
		});

		layersModel.moveLayerUp(layer3); // Expect no changes, and so no event.
		Assert.assertEquals(342, fromIndex);
		Assert.assertEquals(342, toIndex);

		layersModel.moveLayerUp(layer1);
		Assert.assertEquals(0, fromIndex);
		Assert.assertEquals(1, toIndex);

		reg.removeHandler();
	}

	@Test
	public void testMoveLayer() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));

		Layer<?> layer1 = layersModel.getLayer(LAYER1);
		Layer<?> layer2 = layersModel.getLayer(LAYER2);
		Layer<?> layer3 = layersModel.getLayer(LAYER3);

		fromIndex = 342;
		toIndex = 342;

		HandlerRegistration reg = eventBus.addHandler(LayerOrderChangedHandler.TYPE, new LayerOrderChangedHandler() {

			public void onLayerOrderChanged(LayerOrderChangedEvent event) {
				fromIndex = event.getFromIndex();
				toIndex = event.getToIndex();
			}
		});

		layersModel.moveLayer(layer1, -1); // Expect no changes, and so no event.
		Assert.assertEquals(342, fromIndex);
		Assert.assertEquals(342, toIndex);

		layersModel.moveLayer(layer2, -1);
		Assert.assertEquals(1, fromIndex);
		Assert.assertEquals(0, toIndex);

		layersModel.moveLayer(layer2, 2);
		Assert.assertEquals(0, fromIndex);
		Assert.assertEquals(2, toIndex);

		layersModel.moveLayer(layer2, 200); // Expect no changes.
		Assert.assertEquals(0, fromIndex);
		Assert.assertEquals(2, toIndex);

		layersModel.moveLayer(layer3, 0);
		Assert.assertEquals(1, fromIndex);
		Assert.assertEquals(0, toIndex);

		reg.removeHandler();
	}

	@Test
	public void testLayerRemoval() {
		LayersModel layersModel = new LayersModelImpl(eventBus);
		layersModel.initialize(mapInfo, new ViewPortImpl(eventBus));

		layerCount = layersModel.getLayerCount();
		Assert.assertEquals(3, layerCount);

		final MapCompositionHandler layerCounter = new MapCompositionHandler() {

			public void onLayerAdded(LayerAddedEvent event) {
				layerCount++;
			}

			public void onLayerRemoved(LayerRemovedEvent event) {
				layerCount--;
			}
		};
		HandlerRegistration reg = eventBus.addHandler(MapCompositionHandler.TYPE, layerCounter);

		layersModel.removeLayer(LAYER1);
		Assert.assertEquals(2, layerCount);
		layersModel.removeLayer(LAYER3);
		Assert.assertEquals(1, layerCount);
		layersModel.removeLayer(LAYER2);
		Assert.assertEquals(0, layerCount);

		reg.removeHandler();
	}
}