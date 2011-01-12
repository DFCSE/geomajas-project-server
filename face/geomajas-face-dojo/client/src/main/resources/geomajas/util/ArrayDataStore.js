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

dojo.provide("geomajas.util.ArrayDataStore");
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

dojo.declare("ArrayDataStore", null, {

	/**
	 * @fileoverview Array-backed data store
	 * @class A simple datastore backed by an array of objects. Objects are identified by a 
	 * user-specified string attribute.
	 * 
	 * @author Jan De Moerloose
	 * @constructor
	 */
	constructor : function (/* array */ data, /* string */ identifier) {
		this.data = data;
		this.identifier = identifier;
	},

	getValue: function(	/* item */ item, 
						/* attribute-name-string */ attribute, 
						/* value? */ defaultValue){
		return item[attribute] ? item[attribute] : '';
	},

	isItemLoaded: function(/* anything */ something) {
		return true;
	},

	fetch: function(/* Object */ args){
		//	summary:
		//		Given a query and set of defined options, such as a start and count of items to return,
		//		this method executes the query and makes the results available as data items.
		//		Refer to dojo.data.api.Read.fetch() more details.
		//
		//	description:
		//		Given a query like
		//
		//	|	{
		// 	|		query: {name: "Cal*"},
		//	|		start: 30,
		//	|		count: 20,
		//	|		ignoreCase: true,
		//	|		onComplete: function(/* item[] */ items, /* Object */ args){...}
		// 	|	}
		//
		//		will call `onComplete()` with the results of the query (and the argument to this method)

		// convert query to regex (ex: convert "first\last*" to /^first\\last.*$/i) and get matching vals
		var query = "^" + args.query.name
			.replace(/([\\\|\(\)\[\{\^\$\+\?\.\<\>])/g, "\\$1")
			.replace("*", ".*") + "$";
		matcher = new RegExp(query, args.queryOptions.ignoreCase ? "i" : "");
		var items = [];
		for(var i = 0; i < this.data.length; i++){
			var item = this.data[i];
			if(matcher.exec(item[this.identifier])){
				items.push(item);
			}
		}
		var start = args.start || 0,
			end = ("count" in args && args.count != Infinity) ? (start + args.count) : items.length ;
		args.onComplete(items.slice(start, end), args);
		return args; // Object
	},

	close: function(/*dojo.data.api.Request || args || null */ request){
		return;
	},

	getLabel: function(/* item */ item){
		return item[this.identifier];
	},

	getIdentity: function(/* item */ item){
		return item[this.identifier];
	},

	fetchItemByIdentity: function(/* Object */ args){
		//	summary:
		//		Given the identity of an item, this method returns the item that has
		//		that identity through the onItem callback.
		//		Refer to dojo.data.api.Identity.fetchItemByIdentity() for more details.
		//
		//	description:
		//		Given arguments like:
		//
		//	|		{identity: "CA", onItem: function(item){...}
		//
		//		Call `onItem()` with the DOM node `<option value="CA">California</option>`
		for(var i = 0; i < this.data.length; i++){
			var item = this.data[i];
			if(args.identity == item[this.identifier]){
				args.onItem(item);
				break;
			}
		}
	}
});
