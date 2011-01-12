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

dojo.provide("geomajas.gfx.painter.TilePainter");
dojo.require("geomajas.gfx.Painter");

dojo.declare("TilePainter", Painter, {

	/**
	 * @fileoverview Painter implementation for tiles.
	 * @class Painter implementation for tiles. This painter always paints
	 * in worldspace! Responsible for drawing large amounts of data. These
	 * large amounts of data will be features or labels.
	 * @author Pieter De Graef
	 *
	 * @constructor
	 * @extends Painter
	 */
	constructor : function (mapView) {
		this.mapView = mapView;
		this.transform = new WorldViewTransformation(mapView);
		this.labelStyle = new FontStyle("#FF0000", 12, "Arial,Verdana", "bold", "normal");
	},

	/**
	 * The actual painting function. Paints the tile's feature information as
	 * well as the label information. For the labels it must check with the
	 * VectorLayer object associated with the Tile, if they should be drawn,
	 * and what style they should have.
	 * @param tile A {@link Tile} instance.
	 * @param graphics A GraphicsContext object, responsible for actual drawing.
	 */
	paint : function (tile, graphics) {
		//log.error ("TilePainter.paint : "+tile.getId() + " => "+tile.getBounds().toString());

		// FEATURES: 
		var transformationMatrix = this._createTransformationMatrix(tile);

		// Draw the feature fragment as data:
		if (tile.getFeatureFragment() != null) {
			graphics.drawData(tile.getFeatureFragment(), {
				id : tile.getId(),
				width : tile.getScreenWidth(),
				height : tile.getScreenHeight(),
				transform : transformationMatrix
			});
		}

		// LABELS:

		// Draw the label fragment as data:
		var labelId = this._createLabelId(tile);
		if (tile.getLabelFragment() != null) {
			graphics.drawData(tile.getLabelFragment(), {
				id : labelId,
				width : tile.getScreenWidth(),
				height : tile.getScreenHeight(),
				transform : transformationMatrix
			});
		} 
	},

	deleteShape : function (/*Object*/tile, /*GraphicsContext*/graphics) {
		//log.error ("TilePainter.deleteShape : "+tile.getId());
		graphics.deleteShape(tile.getId());
		graphics.deleteShape(this._createLabelId(tile));
	},



	/**
	 * @private
	 */
	_createLabelId : function (tile) {
		var tileId = tile.getId();
		return tileId.replace("features", "labels");
	},

	/**
	 * @private
	 */
	_createTransformationMatrix : function (tile) {
		// We assume the geometries are in screen space, beginning from a tile's upper-left corner.
	
		var dX = null;
		var dY = null;
		//if(tile.isClipped()){
			// clipped tiles have the pan origin as origin, so no need to translate
			dX = 0;
			dY = 0;
		/*} else {
			// The map has already been translated by this, so we compensate again.
			var trans = this.mapView.getPanToViewTranslation();

			// To find the origin of the tile, we transform it's bounds to view space.
			var viewTileBounds = this.transform.worldBoundsToView (tile.getBounds());
			dX = viewTileBounds.getX() - trans.dx;
			dY = viewTileBounds.getY() - trans.dy;
		}*/
		
		// var sc = this.mapView.getCurrentScale();

		return {xx: 1, xy: 0, yx: 0, yy: 1, dx: dX, dy: dY};
	}

});