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

import com.smartgwt.client.widgets.Canvas;
import org.geomajas.gwt.client.action.ToolbarBaseAction;
import org.geomajas.gwt.client.action.ToolbarCanvas;
import org.geomajas.gwt.client.widget.MapWidget;
import org.geomajas.gwt.client.widget.ScaleSelect;

/**
 * Tool which displays a scale select widget.
 *
 * @author Joachim Van der Auwera
 */
public class ScaleSelectAction extends ToolbarBaseAction implements ToolbarCanvas {

	private ScaleSelect scaleSelect;

	public ScaleSelectAction(MapWidget mapWidget) {
		super("", ""); // dummy icon and tooltip
		scaleSelect = new ScaleSelect(mapWidget);
	}

	/** {@inheritDoc} */
	public Canvas getCanvas() {
		return scaleSelect;
	}
}
