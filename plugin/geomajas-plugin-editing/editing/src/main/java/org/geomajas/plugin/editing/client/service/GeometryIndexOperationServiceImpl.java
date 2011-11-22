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

package org.geomajas.plugin.editing.client.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.geomajas.geometry.Coordinate;
import org.geomajas.geometry.Geometry;
import org.geomajas.plugin.editing.client.event.GeometryEditInsertEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditMoveEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditRemoveEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditShapeChangedEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditStartEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditStartHandler;
import org.geomajas.plugin.editing.client.event.GeometryEditStopEvent;
import org.geomajas.plugin.editing.client.event.GeometryEditStopHandler;
import org.geomajas.plugin.editing.client.operation.DeleteGeometryOperation;
import org.geomajas.plugin.editing.client.operation.DeleteVertexOperation;
import org.geomajas.plugin.editing.client.operation.GeometryIndexOperation;
import org.geomajas.plugin.editing.client.operation.GeometryOperationFailedException;
import org.geomajas.plugin.editing.client.operation.InsertGeometryOperation;
import org.geomajas.plugin.editing.client.operation.InsertVertexOperation;
import org.geomajas.plugin.editing.client.operation.MoveVertexOperation;

import com.google.gwt.event.shared.EventBus;

/**
 * ...
 * 
 * @author Pieter De Graef
 */
public class GeometryIndexOperationServiceImpl implements GeometryIndexOperationService, GeometryEditStartHandler,
		GeometryEditStopHandler {

	private Stack<OperationSequence> undoQueue;

	private Stack<OperationSequence> redoQueue;

	private GeometryEditingServiceImpl service;

	private GeometryIndexService indexService;

	private EventBus eventBus;

	private OperationSequence current;

	protected GeometryIndexOperationServiceImpl(GeometryEditingServiceImpl service, EventBus eventBus) {
		this.service = service;
		this.eventBus = eventBus;
		indexService = service.getIndexService();
		undoQueue = new Stack<OperationSequence>();
		redoQueue = new Stack<OperationSequence>();

		service.addGeometryEditStartHandler(this);
		service.addGeometryEditStopHandler(this);
	}

	// ------------------------------------------------------------------------
	// GeometryEditWorkflowHandler implementation:
	// ------------------------------------------------------------------------

	public void onGeometryEditStart(GeometryEditStartEvent event) {
		undoQueue.clear();
		redoQueue.clear();
	}

	public void onGeometryEditStop(GeometryEditStopEvent event) {
		undoQueue.clear();
		redoQueue.clear();
	}

	// ------------------------------------------------------------------------
	// Methods concerning "UNDO/REDO":
	// ------------------------------------------------------------------------

	public boolean canUndo() {
		return undoQueue.size() > 0;
	}

	public boolean canRedo() {
		return redoQueue.size() > 0;
	}

	public void undo() throws GeometryOperationFailedException {
		if (!canUndo()) {
			throw new GeometryOperationFailedException("Cannot perform UNDO. No operation sequence could be found.");
		}
		stopOperationSequence();
		OperationSequence sequence = undoQueue.pop();
		redoQueue.add(sequence);
		sequence.undo(service.getGeometry());
		eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
	}

	public void redo() throws GeometryOperationFailedException {
		if (!canRedo()) {
			throw new GeometryOperationFailedException("Cannot perform REDO. No operation sequence could be found.");
		}
		stopOperationSequence();
		OperationSequence sequence = redoQueue.pop();
		undoQueue.add(sequence);
		sequence.redo(service.getGeometry());
		eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
	}

	// ------------------------------------------------------------------------
	// Operation sequence manipulation:
	// ------------------------------------------------------------------------

	public void startOperationSequence() throws GeometryOperationFailedException {
		if (current != null) {
			throw new GeometryOperationFailedException("An operation sequence has already been started.");
		}
		current = new OperationSequence();
		redoQueue.clear();
	}

	public void stopOperationSequence() {
		if (isOperationSequenceActive() && !current.isEmpty()) {
			undoQueue.add(current);
			eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
		}
		current = null;
	}

	public boolean isOperationSequenceActive() {
		return current != null;
	}

	// ------------------------------------------------------------------------
	// Supported operations:
	// ------------------------------------------------------------------------

	public void move(List<GeometryIndex> indices, List<List<Coordinate>> coordinates)
			throws GeometryOperationFailedException {
		if (indices == null || coordinates == null || indices.size() == 0 || coordinates.size() == 0) {
			throw new GeometryOperationFailedException("Illegal arguments passed; nothing to move.");
		}

		Geometry geometry = service.getGeometry();
		OperationSequence seq = null;
		if (isOperationSequenceActive()) {
			seq = current;
		} else {
			seq = new OperationSequence();
			redoQueue.clear();
		}
		for (int i = 0; i < indices.size(); i++) {
			if (indexService.getType(indices.get(i)) == GeometryIndexType.TYPE_VERTEX) {
				GeometryIndexOperation op = new MoveVertexOperation(indexService, coordinates.get(i).get(0));
				op.execute(geometry, indices.get(i));
				seq.addOperation(op);
			} else {
				throw new GeometryOperationFailedException("Can only move vertices. Other types not suported.");
			}
		}
		if (!isOperationSequenceActive()) {
			undoQueue.add(seq);
		}
		eventBus.fireEvent(new GeometryEditMoveEvent(geometry, indices));
		if (!isOperationSequenceActive()) {
			eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
		}
	}

	public void insert(List<GeometryIndex> indices, List<List<Coordinate>> coordinates)
			throws GeometryOperationFailedException {
		if (indices == null || coordinates == null || indices.size() == 0 || coordinates.size() == 0) {
			throw new GeometryOperationFailedException("Illegal arguments passed; nothing to insert.");
		}

		Geometry geometry = service.getGeometry();
		OperationSequence seq = null;
		if (isOperationSequenceActive()) {
			seq = current;
		} else {
			seq = new OperationSequence();
			redoQueue.clear();
		}
		for (int i = 0; i < indices.size(); i++) {
			switch (indexService.getType(indices.get(i))) {
				case TYPE_GEOMETRY:
					throw new GeometryOperationFailedException("Cannot insert new geometries (yet).");
				default:
					GeometryIndexOperation op = new InsertVertexOperation(indexService, coordinates.get(i).get(0));
					op.execute(geometry, indices.get(i));
					seq.addOperation(op);
			}
		}
		if (!isOperationSequenceActive()) {
			undoQueue.add(seq);
		}
		eventBus.fireEvent(new GeometryEditInsertEvent(geometry, indices));
		if (!isOperationSequenceActive()) {
			eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
		}
	}

	public void remove(List<GeometryIndex> indices) throws GeometryOperationFailedException {
		if (indices == null || indices.size() == 0) {
			throw new GeometryOperationFailedException("Illegal arguments passed; nothing to delete.");
		}

		Geometry geometry = service.getGeometry();
		OperationSequence seq = null;
		if (isOperationSequenceActive()) {
			seq = current;
		} else {
			seq = new OperationSequence();
			redoQueue.clear();
		}
		for (int i = 0; i < indices.size(); i++) {
			GeometryIndexOperation op;
			switch (indexService.getType(indices.get(i))) {
				case TYPE_GEOMETRY:
					op = new DeleteGeometryOperation(indexService);
					break;
				default:
					op = new DeleteVertexOperation(indexService);
			}
			op.execute(geometry, indices.get(i));
			seq.addOperation(op);
		}
		if (!isOperationSequenceActive()) {
			undoQueue.add(seq);
		}
		eventBus.fireEvent(new GeometryEditRemoveEvent(geometry, indices));
		if (!isOperationSequenceActive()) {
			eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
		}
	}

	public GeometryIndex addEmptyChild() throws GeometryOperationFailedException {
		Geometry geometry = service.getGeometry();
		OperationSequence seq = null;
		if (isOperationSequenceActive()) {
			seq = current;
		} else {
			seq = new OperationSequence();
			redoQueue.clear();
		}

		GeometryIndexOperation operation = null;
		if (Geometry.POLYGON.equals(geometry.getGeometryType())) {
			operation = new InsertGeometryOperation(indexService, new Geometry(Geometry.LINEAR_RING,
					geometry.getSrid(), geometry.getPrecision()));
		} else if (Geometry.MULTI_POINT.equals(geometry.getGeometryType())) {
			operation = new InsertGeometryOperation(indexService, new Geometry(Geometry.POINT, geometry.getSrid(),
					geometry.getPrecision()));
		} else if (Geometry.MULTI_LINE_STRING.equals(geometry.getGeometryType())) {
			operation = new InsertGeometryOperation(indexService, new Geometry(Geometry.LINE_STRING,
					geometry.getSrid(), geometry.getPrecision()));
		} else if (Geometry.MULTI_POLYGON.equals(geometry.getGeometryType())) {
			operation = new InsertGeometryOperation(indexService, new Geometry(Geometry.POLYGON, geometry.getSrid(),
					geometry.getPrecision()));
		}
		if (operation != null) {
			// Execute the operation:
			GeometryIndex index;
			if (geometry.getGeometries() == null) {
				index = indexService.create(GeometryIndexType.TYPE_GEOMETRY, 0);
			} else {
				index = indexService.create(GeometryIndexType.TYPE_GEOMETRY, geometry.getGeometries().length);
			}
			operation.execute(geometry, index);

			// Add the operation to the queue (if not part of a sequence):
			seq.addOperation(operation);
			if (!isOperationSequenceActive()) {
				undoQueue.add(seq);
			}
			if (!isOperationSequenceActive()) {
				eventBus.fireEvent(new GeometryEditShapeChangedEvent(service.getGeometry()));
			}
			return index;
		}
		throw new GeometryOperationFailedException("Can't add a new geometry to the given geometry.");
	}

	// ------------------------------------------------------------------------
	// Private classes:
	// ------------------------------------------------------------------------

	/**
	 * Private definition of a sequence of operations. All operations added to this sequence are regarded as a single
	 * entity.
	 * 
	 * @author Pieter De Graef
	 */
	private class OperationSequence {

		private List<GeometryIndexOperation> operations = new ArrayList<GeometryIndexOperation>();

		public void addOperation(GeometryIndexOperation operation) {
			operations.add(operation);
		}

		public Geometry undo(Geometry geometry) throws GeometryOperationFailedException {
			// TODO loop over all operations for undo/redo is not very performing.
			for (int i = operations.size() - 1; i >= 0; i--) {
				GeometryIndexOperation op = operations.get(i);
				geometry = op.getInverseOperation().execute(geometry, op.getGeometryIndex());
			}
			return geometry;
		}

		public Geometry redo(Geometry geometry) throws GeometryOperationFailedException {
			for (GeometryIndexOperation op : operations) {
				geometry = op.execute(geometry, op.getGeometryIndex());
			}
			return geometry;
		}

		public boolean isEmpty() {
			return operations.isEmpty();
		}
	}
}