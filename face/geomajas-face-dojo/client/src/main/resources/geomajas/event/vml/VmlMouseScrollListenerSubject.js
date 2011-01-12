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

dojo.provide("geomajas.event.vml.VmlMouseScrollListenerSubject");
dojo.require("dojox.collections.Dictionary");
dojo.require("geomajas.event.HtmlMouseEvent");
dojo.require("geomajas.event.ListenerSubject");

dojo.extend(MouseScrollListenerSubject, {

	_afterInit : function () {
		this.listeners = new dojox.collections.Dictionary();
		dojo.connect(this.subject, "onmousewheel", this, "scroll");
	},

	/**
	 * Add a new MouseScrollListener object to the subject SVG element.
	 * @param listener The MouseScrollListener object to be added to the list.
	 */
	addListener : function (/*MouseScrollListener*/listener) {
		if (!this.listeners.contains(listener.getName())) {
			this.listeners.add(listener.getName(), listener);
		}
	},

	/**
	 * Remove an existing MouseScrollListener object from the list. If the listener was not
	 * found, nothing happens.
	 * @param listener The MouseScrollListener object to be removed from the list. 
	 */
	removeListener : function (/*MouseScrollListener*/listener) {
		if (this.listeners.contains(listener.getName())) {
			this.listeners.remove(listener.getName());
		}
	},

	scroll : function (evt) {	
		var event = new HtmlMouseEvent(evt); 
		var e = this.listeners.getIterator();
		while(e.get()) {
		 	e.element.value.mouseScrolled(event);
		}
	}

});