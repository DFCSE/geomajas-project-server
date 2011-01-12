dojo.provide("geomajas.widget.FeatureDetailEditor");
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
dojo.requireLocalization("geomajas.widget", "featureDetailEditor");

dojo.declare("geomajas.widget.FeatureDetailEditor", [dijit.layout.LayoutContainer, dijit._Templated], {

	widgetsInTemplate : true,
	templatePath : dojo.moduleUrl("geomajas.widget", "html/FeatureDetailEditor.html"),

	mapWidget : null,
	featureEditor : null,
	feature : null,
	
	saveString : "",
	zoomToFitString : "",

	postMixInProperties : function(/*Object*/args, /*Object*/frag, /*Widget*/parent){
		var widgetLocale = dojo.i18n.getLocalization("geomajas.widget", "featureDetailEditor");
		this.saveString = widgetLocale.save;
		this.zoomToFitString = widgetLocale.zoomToFit;
	},
	
	postCreate : function () {
		this.setFeature(this.feature);

		var saveButton = dijit.byId(this.id + ":saveBTN");
		if (saveButton != null) {
			saveButton.onClick = dojo.hitch (this, "save");
		}
		var zoomButton = dijit.byId(this.id + ":zoomBTN");
		if (zoomButton != null) {
			zoomButton.onClick = dojo.hitch (this, "_zoomToFeature");
		}

		var fdt = dijit.byId(this.id + ":fdt");
		if (fdt != null) {
			fdt.onChange = dojo.hitch (this, "_checkEditorValidity");
		}
		this._checkEditorValidity();
	},

	destroy : function (finalize) {
		if (dijit.byId(this.id + ":toolbar")) {
			dijit.byId(this.id + ":toolbar").destroy();
		}
		if (dijit.byId(this.id + ":fdt")) {
			dijit.byId(this.id + ":fdt").destroy();
		}
		this.inherited (arguments);
	},

	setMapWidget : function (mapWidget) {
		this.mapWidget = mapWidget;
		if (mapWidget != null) {
			this.featureEditor = mapWidget.getMapModel().getFeatureEditor();
		}
	},

	setFeature : function (feature) {
		this.feature = feature;
		var fdt = dijit.byId(this.id + ":fdt");
		if (feature != null) {
			fdt.setFeature(this.feature.clone());
			var zoomButton = dijit.byId(this.id + ":zoomBTN");
			if (zoomButton) {
				zoomButton.setLabel("Zoom to "+feature.getLabel());
			}
		}

		if (feature && (!feature.getLayer() || !feature.getLayer().getEditPermissions().isUpdatingAllowed())) {
			var saveButton = dijit.byId(this.id + ":saveBTN");
			saveButton.setDisabled (true);
			saveButton.domNode.style.display = "none";
		}
	},

	getFeature : function () {
		var fdt = dijit.byId(this.id + ":fdt");
		return fdt.getFeature();
	},

	save : function () {
		if (this.featureEditor) {
			var newFeature = this.getFeature();

			if (newFeature == null) {
				log.error ("Adjusted feature could not be retrieved.");
				return;
			}
			this.featureEditor.startEditing([this.feature], [newFeature]);
			var featureTransaction = this.featureEditor.getFeatureTransaction();
			featureTransaction.setGeometryChanged(false);

			var layer = featureTransaction.getLayer();
			var handler = layer.getWorkflowHandler();
			var workflow = handler.startWorkflow(featureTransaction);
			workflow.start();
			this.featureEditor.stopEditing(featureTransaction);
		}
	},

	resize : function (size){
		this.inherited (arguments);
		this.layout();
	},


	//-------------------------------------------------------------------------
	// Private functions.
	//-------------------------------------------------------------------------

	/**
	 * @private
	 * Check each editor for value validity. Disables the OK button if needed.
	 */
	_checkEditorValidity : function (value) {
		var saveButton = dijit.byId(this.id + ":saveBTN");
		if (this.featureEditor == null) {
			saveButton.setDisabled(true);
			return;
		}
		var fdt = dijit.byId(this.id + ":fdt");
		if (saveButton && fdt){
			if (fdt.isValid()) {
				saveButton.setDisabled(false);
			} else {
				saveButton.setDisabled(true);
			}
		}
	},

	/**
	 * @private
	 * Zoom the map to this feature's boundaries.
	 */
	_zoomToFeature : function () {
		if (this.mapWidget != null && this.feature != null) {
			var geometry = this.feature.getGeometry();
			var bounds = geometry.getBounds();
			if (bounds.getWidth() == 0) {
				bounds = bounds.buffer(1);
			} else {
				bounds = bounds.buffer(bounds.getWidth() * 0.1);
			}
			dojo.publish(this.mapWidget.getMapView().getExternalRenderTopic(), [{
				event:"setBounds",
				x:bounds.getX(), 
				y:bounds.getY(), 
				width:bounds.getWidth(), 
				height:bounds.getHeight()
			}]);
		}
	}
});
