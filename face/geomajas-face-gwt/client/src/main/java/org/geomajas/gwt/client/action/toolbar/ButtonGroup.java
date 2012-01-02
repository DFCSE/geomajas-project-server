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

package org.geomajas.gwt.client.action.toolbar;

import org.geomajas.gwt.client.action.ConfigurableAction;
import org.geomajas.gwt.client.action.ToolbarBaseAction;
/**
 * Basic {@link ConfigurableAction} implementation that fetches a title and a buttonLayout from the parameters.
 * 
 * @author Emiel Ackermann
 */
public class ButtonGroup extends ToolbarBaseAction implements ConfigurableAction {

	private String buttonLayout;

	public ButtonGroup() {
		super("", "", "");
	}

	public void configure(String key, String value) {
		if ("title".equals(key)) {
			setTitle(value);
		} else if ("buttonLayout".equals(key)) {
			setButtonLayout(value);
		}
	}
	
	public String getButtonLayout() {
		return buttonLayout;
	}

	private void setButtonLayout(String buttonLayout) {
		this.buttonLayout = buttonLayout;
	}
}
