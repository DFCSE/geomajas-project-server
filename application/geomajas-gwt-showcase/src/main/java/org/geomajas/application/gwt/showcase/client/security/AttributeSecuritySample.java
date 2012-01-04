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

package org.geomajas.application.gwt.showcase.client.security;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.geomajas.application.gwt.showcase.client.i18n.ShowcaseMessages;
import org.geomajas.command.CommandResponse;
import org.geomajas.command.dto.SearchFeatureRequest;
import org.geomajas.command.dto.SearchFeatureResponse;
import org.geomajas.gwt.client.command.CommandCallback;
import org.geomajas.gwt.client.command.GwtCommand;
import org.geomajas.gwt.client.command.GwtCommandDispatcher;
import org.geomajas.gwt.client.map.feature.Feature;
import org.geomajas.gwt.client.map.layer.VectorLayer;
import org.geomajas.gwt.client.widget.FeatureAttributeWindow;
import org.geomajas.gwt.client.widget.MapWidget;
import org.geomajas.gwt.example.base.SamplePanel;
import org.geomajas.gwt.example.base.SamplePanelFactory;
import org.geomajas.layer.feature.SearchCriterion;
import org.geomajas.plugin.staticsecurity.client.Authentication;

/**
 * <p>
 * Sample that tests security on attribute level.
 * </p>
 * 
 * @author Pieter De Graef
 */
public class AttributeSecuritySample extends SamplePanel {

	public static final String TITLE = "AttributeSecurity";

	private static final ShowcaseMessages MESSAGES = GWT.create(ShowcaseMessages.class);

	private MapWidget map;

	private VectorLayer layer;

	private FeatureAttributeWindow featureAttributeWindow;

	public static final SamplePanelFactory FACTORY = new SamplePanelFactory() {

		public SamplePanel createPanel() {
			return new AttributeSecuritySample();
		}
	};

	public Canvas getViewPanel() {
		final VLayout layout = new VLayout();
		layout.setMembersMargin(10);
		layout.setWidth100();
		layout.setHeight100();

		// Create horizontal layout for login buttons:
		HLayout buttonLayout = new HLayout();
		buttonLayout.setMembersMargin(10);
		buttonLayout.setHeight(20);

		// Map with ID duisburgMap is defined in the XML configuration. (mapDuisburg.xml)
		map = new MapWidget("mapBeans", "gwtExample");
		map.setVisible(false);
		layout.addMember(map);
		map.init();

		// Create login handler that re-initializes the map on a successful login:
		final BooleanCallback initMapCallback = new BooleanCallback() {

			public void execute(Boolean value) {
				if (value) {
					map.destroy();
					map = new MapWidget("mapBeans", "gwtExample");
					map.setVisible(false);
					layout.addMember(map);
					map.init();
				}
			}
		};

		// Create a button that logs in user "elvis":
		IButton loginButtonMarino = new IButton(MESSAGES.securityLogInWith("elvis"));
		loginButtonMarino.setWidth(150);
		loginButtonMarino.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				Authentication.getInstance().login("elvis", "elvis", initMapCallback);
				if (null != featureAttributeWindow) {
					featureAttributeWindow.destroy();
					featureAttributeWindow = null;
				}
			}
		});
		buttonLayout.addMember(loginButtonMarino);

		// Create a button that logs in user "luc":
		IButton loginButtonLuc = new IButton(MESSAGES.securityLogInWith("luc"));
		loginButtonLuc.setWidth(150);
		loginButtonLuc.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				Authentication.getInstance().login("luc", "luc", initMapCallback);
				if (null != featureAttributeWindow) {
					featureAttributeWindow.destroy();
					featureAttributeWindow = null;
				}
			}
		});
		buttonLayout.addMember(loginButtonLuc);

		// Set up the search command, that will fetch a feature:
		// Searches for ID=1, but we might as well have created a filter on one of the attributes...
		SearchFeatureRequest request = new SearchFeatureRequest();
		request.setBooleanOperator("AND");
		request.setCrs("EPSG:900913"); // Can normally be acquired from the MapModel.
		request.setLayerId("layerBeans");
		request.setMax(1);
		request.setCriteria(new SearchCriterion[] { new SearchCriterion("id", "=", "1") });
		final GwtCommand command = new GwtCommand(SearchFeatureRequest.COMMAND);
		command.setCommandRequest(request);

		// Create a button that executes the search command:
		IButton editFeatureButton = new IButton(MESSAGES.attributeSecurityButtonTitle());
		editFeatureButton.setWidth(200);
		editFeatureButton.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				layer = (VectorLayer) map.getMapModel().getLayer("clientLayerBeans");
				GwtCommandDispatcher.getInstance().execute(command, new CommandCallback() {

					public void execute(CommandResponse response) {
						if (response instanceof SearchFeatureResponse) {
							SearchFeatureResponse resp = (SearchFeatureResponse) response;
							for (org.geomajas.layer.feature.Feature dtoFeature : resp.getFeatures()) {
								Feature feature = new Feature(dtoFeature, layer);
								if (null != featureAttributeWindow) {
									featureAttributeWindow.destroy();
									featureAttributeWindow = null;
								}
								featureAttributeWindow = new FeatureAttributeWindow(feature, true);
								featureAttributeWindow.setWidth(400);
								layout.addMember(featureAttributeWindow);
							}
						}
					}
				});
			}
		});

		layout.addMember(buttonLayout);
		layout.addMember(editFeatureButton);
		return layout;
	}

	public String getDescription() {
		return MESSAGES.attributeSecurityDescription();
	}

	public String[] getConfigurationFiles() {
		return new String[] { "WEB-INF/security.xml",
				"classpath:org/geomajas/gwt/example/layerBeans.xml",
				"classpath:org/geomajas/gwt/example/mapBeans.xml" };
	}

	public String ensureUserLoggedIn() {
		return "luc";
	}
}
