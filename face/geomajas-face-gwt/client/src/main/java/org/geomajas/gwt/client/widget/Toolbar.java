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

package org.geomajas.gwt.client.widget;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.util.SC;
import org.geomajas.annotation.Api;
import org.geomajas.configuration.Parameter;
import org.geomajas.configuration.client.ClientMapInfo;
import org.geomajas.configuration.client.ClientToolInfo;
import org.geomajas.configuration.client.ClientToolbarInfo;
import org.geomajas.gwt.client.action.ConfigurableAction;
import org.geomajas.gwt.client.action.ToolbarAction;
import org.geomajas.gwt.client.action.ToolbarBaseAction;
import org.geomajas.gwt.client.action.ToolbarCanvas;
import org.geomajas.gwt.client.action.ToolbarModalAction;
import org.geomajas.gwt.client.action.ToolbarWidget;
import org.geomajas.gwt.client.action.event.ToolbarActionDisabledEvent;
import org.geomajas.gwt.client.action.event.ToolbarActionEnabledEvent;
import org.geomajas.gwt.client.action.event.ToolbarActionHandler;
import org.geomajas.gwt.client.action.toolbar.ToolId;
import org.geomajas.gwt.client.action.toolbar.ToolbarRegistry;
import org.geomajas.gwt.client.map.event.MapModelEvent;
import org.geomajas.gwt.client.map.event.MapModelHandler;

import com.smartgwt.client.types.SelectionType;
import com.smartgwt.client.widgets.IButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripSeparator;
import org.geomajas.gwt.client.util.Log;

/**
 * A toolbar that supports two types of buttons:
 * <ul>
 * <li>modal (radiobutton-like) buttons.</li>
 * <li>non-modal or action buttons.</li>
 * </ul>
 * 
 * @author Pieter De Graef
 * @author Joachim Van der Auwera
 * @since 1.6.0
 */
@Api
public class Toolbar extends ToolStrip {

	/**
	 * Button size for small buttons.
	 * @since 1.10.0
	 */
	@Api
	public static final int BUTTON_SIZE_SMALL = 24;

	/**
	 * Button size for large buttons.
	 * @since 1.10.0
	 */
	@Api
	public static final int BUTTON_SIZE_BIG = 32;

	private static final String CONTROLLER_RADIO_GROUP = "graphicsController";

	private MapWidget mapWidget;

	private int buttonSize;

	private boolean initialized;

	// -------------------------------------------------------------------------
	// Constructor:
	// -------------------------------------------------------------------------

	/**
	 * Create a toolbar for the given {@link MapWidget}.
	 *
	 * @param mapWidget map widget for toolbar
	 * @since 1.10.0
	 */
	@Api
	public Toolbar(MapWidget mapWidget) {
		this.mapWidget = mapWidget;
		setButtonSize(BUTTON_SIZE_SMALL);
		setPadding(2);
		setWidth100();
		mapWidget.getMapModel().addMapModelHandler(new MapModelHandler() {

			public void onMapModelChange(MapModelEvent event) {
				initialize(Toolbar.this.mapWidget.getMapModel().getMapInfo());
			}
		});
	}

	// -------------------------------------------------------------------------
	// Public methods:
	// -------------------------------------------------------------------------

	/**
	 * Initialize this widget.
	 * 
	 * @param mapInfo map info
	 */
	public void initialize(ClientMapInfo mapInfo) {
		if (!initialized) {
			ClientToolbarInfo toolbarInfo = mapInfo.getToolbar();
			if (toolbarInfo != null) {
				for (ClientToolInfo tool : toolbarInfo.getTools()) {
					String id = tool.getId();
					if (ToolId.TOOL_SEPARATOR.equals(id)) {
						addToolbarSeparator();
					} else {
						ToolbarBaseAction action = ToolbarRegistry.getToolbarAction(id, mapWidget);
						if (action instanceof ConfigurableAction) {
							for (Parameter parameter : tool.getParameters()) {
								((ConfigurableAction) action).configure(parameter.getName(), parameter.getValue());
							}
						}
						if (action instanceof ToolbarWidget) {
							addMember(((ToolbarWidget) action).getWidget());
						} else if (action instanceof ToolbarCanvas) {
							addMember(((ToolbarCanvas) action).getCanvas());
						} else if (action instanceof ToolbarModalAction) {
							addModalButton((ToolbarModalAction) action);
						} else if (action instanceof ToolbarAction) {
							addActionButton((ToolbarAction) action);
						} else {
							String msg = "Tool with id " + id + " unknown.";
							Log.logError(msg); // console log
							GWT.log(msg); // server side GWT run/debug log (development mode only)
							SC.warn(msg); // in your face
						}
					}
				}
			}
			initialized = true;
		}
	}

	/**
	 * Add a new action button to the toolbar. An action button is the kind of button that executes immediately when
	 * clicked upon. It can not be selected or deselected, it just executes every click.
	 * 
	 * @param action
	 *            The actual action to execute on click.
	 */
	public void addActionButton(final ToolbarAction action) {
		final IButton button = new IButton();
		button.setWidth(buttonSize);
		button.setHeight(buttonSize);
		button.setIconSize(buttonSize - 8);
		button.setIcon(action.getIcon());
		button.setActionType(SelectionType.BUTTON);
		button.addClickHandler(action);
		button.setShowRollOver(false);
		button.setShowFocused(false);
		button.setTooltip(action.getTooltip());
		button.setDisabled(action.isDisabled());

		if (getMembers() != null && getMembers().length > 0) {
			LayoutSpacer spacer = new LayoutSpacer();
			spacer.setWidth(2);
			addMember(spacer);
		}
		action.addToolbarActionHandler(new ToolbarActionHandler() {

			public void onToolbarActionDisabled(ToolbarActionDisabledEvent event) {
				button.setDisabled(true);
			}

			public void onToolbarActionEnabled(ToolbarActionEnabledEvent event) {
				button.setDisabled(false);
			}
		});
		addMember(button);
	}

	/**
	 * Add a new modal button (checkbox) to the toolbar. This kind of button is selected and deselected as the user
	 * clicks upon it. By selecting and deselecting, a certain state will be activate or deactivated, determined by the
	 * given <code>ModalAction</code>.
	 * 
	 * @param modalAction
	 *            The actual action that determines what should happen when the button is selected or deselected.
	 */
	public void addModalButton(final ToolbarModalAction modalAction) {
		final IButton button = new IButton();
		button.setWidth(buttonSize);
		button.setHeight(buttonSize);
		button.setIconSize(buttonSize - 8);
		button.setIcon(modalAction.getIcon());
		button.setActionType(SelectionType.CHECKBOX);
		button.setRadioGroup(CONTROLLER_RADIO_GROUP);
		button.addClickHandler(new ClickHandler() {

			public void onClick(ClickEvent event) {
				if (button.isSelected()) {
					modalAction.onSelect(event);
				} else {
					modalAction.onDeselect(event);
				}
			}
		});
		button.setShowRollOver(false);
		button.setShowFocused(true);
		button.setTooltip(modalAction.getTooltip());
		button.setDisabled(modalAction.isDisabled());

		if (getMembers() != null && getMembers().length > 0) {
			LayoutSpacer spacer = new LayoutSpacer();
			spacer.setWidth(2);
			addMember(spacer);
		}
		modalAction.addToolbarActionHandler(new ToolbarActionHandler() {

			public void onToolbarActionDisabled(ToolbarActionDisabledEvent event) {
				button.setDisabled(true);
			}

			public void onToolbarActionEnabled(ToolbarActionEnabledEvent event) {
				button.setDisabled(false);
			}
		});
		addMember(button);
	}

	/** Add a vertical line to the toolbar. */
	public void addToolbarSeparator() {
		ToolStripSeparator stripSeparator = new ToolStripSeparator();
		stripSeparator.setHeight(8);
		addMember(stripSeparator);
	}

	// -------------------------------------------------------------------------
	// Getters and setters:
	// -------------------------------------------------------------------------

	/**
	 * Get the size of the buttons. Set this before the toolbar is drawn, because afterwards, it can't be changed
	 * anymore.
	 *
	 * @return button size
	 */
	public int getButtonSize() {
		return buttonSize;
	}

	/**
	 * Set the size of the buttons. Use this before the toolbar is drawn, because afterwards, it can't be changed
	 * anymore.
	 * 
	 * @param buttonSize
	 *            The new button size.
	 */
	public void setButtonSize(int buttonSize) {
		this.buttonSize = buttonSize;
		// TODO: resize all buttons
		setHeight(buttonSize);
	}
}
