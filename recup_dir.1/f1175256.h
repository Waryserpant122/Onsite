// backbone-tree-model 0.9.9
(function(root, factory) {
	// Set up Backbone appropriately for the environment. Start with AMD.
	if (typeof define === 'function' && define.amd) {
		define(['underscore', 'backbone', 'exports'], function(_, Backbone, exports) {
			// Export global even in AMD case in case this script is loaded with
			// others that may still expect a global Backbone.
			return root.BackboneTreeModel = factory(root, exports, _, Backbone);
		});

		// Next for Node.js or CommonJS.
	} else if (typeof exports !== 'undefined') {
		var _ = require('underscore');
		var Backbone = require('backbone');
		module.exports = factory(root, exports, _, Backbone);

		// Finally, as a browser global.
	} else {
		root.BackboneTreeModel = factory(root, {}, root._, root.Backbone);
	}
}(this, function(root, BackboneTreeModel, _, Backbone) {

	var ArrMethods = {
		where: function(attrs) {
			var children = [];
			_.each(this, function(model) {
				children = children.concat(model.where(attrs));
			});
			return wrapArray(_.uniq(children));
		}
	};
	var wrapArray = function(array) { return _.extend(array, ArrMethods); };

	var TreeModel = Backbone.TreeModel = Backbone.Model.extend({
		constructor: function tree(node) {
			Backbone.Model.prototype.constructor.apply(this, arguments);
			this._children = new this.collectionConstructor([], {

				model : Backbone.TreeModel,

				comparator: function(){

                    return app.controllers.bottomPanel.treeComparator.apply(this, arguments);
				}


			});
			this._children.parent = this;
			if(node && node.children) this.add(node.children);
		},

		collectionConstructor : null,

		/**
		 * returns JSON object representing tree, account for branch changes
		 */
		toJSON: function() {
			var jsonObj = _.clone(_.omit(this.attributes, 'children'));
			var children = this._children.toJSON();
			if(children.length) jsonObj.children = children;
			return jsonObj;
		},

		/**
		 * returns JSON object representing tree, account for branch changes
		 */
		sort: function() {

			if (this._children.length){

                this._children.map(function(child){
                	child.sort();
				});

				this._children.sort();
            }

		},

		/**
		 * returns descendant matching :id
		 */
		find: function(id) { return this.findWhere({id: id}); },

		/**
		 * return first matched descendant
		 */
		findWhere: function(attrs) { return this.where(attrs, true); },

		/**
		 * return all matched descendants
		 */
		where: function(attrs, first, excludeCurrentNode) {
			var children = [], matchedNode;

			// manual (non-collection method) check on the current node
			if(!excludeCurrentNode && _.where([this.toJSON()], attrs)[0]) children.push(this);

			if(first) {
				// return if first/current node is a match
				if(children[0]) return children[0];

				// return first matched node in children collection
				matchedNode = this._children.where(attrs, true);
				if(_.isArray(matchedNode)) matchedNode = matchedNode[0];
				if(matchedNode instanceof Backbone.Model) return matchedNode;

				// recursive call on children children
				for(var i=0, len=this._children.length; i<len; i++) {
					matchedNode = this._children.at(i).where(attrs, true, true);
					if(matchedNode) return matchedNode;
				}
			} else {
				// add all matched children
				children = children.concat(this._children.where(attrs));

				// recursive call on children children
				this._children.each(function(node) {
					children = children.concat(node.where(attrs, false, true));
				});

				// return all matched children
				return wrapArray(children);
			}
		},

		/**
		 * returns true if current node is root node
		 */
		isRoot: function() { return this.parent() === null; },

		/**
		 * returns the root for any node
		 */
		root: function() { return this.parent() && this.parent().root() || this; },

		/**
		 * checks if current node contains argument node
		 */
		contains: function(node) {
			if(!node || !(node.isRoot && node.parent) || node.isRoot()) return false;
			var parent = node.parent();
			return (parent === this) || this.contains(parent);
		},

		/**
		 * returns the parent node
		 */
		parent: function() { return this.collection && this.collection.parent || null; },

		/**
		 * returns the children Backbone Collection if children children exist
		 */
		children: function() { return this._children.length && this._children || null; },

		/**
		 * returns index of node relative to collection
		 */
		index: function() {
			if(this.isRoot()) return null;
			return this.collection.indexOf(this);
		},

		/**
		 * returns the node to the right
		 */
		next: function() {
			if(this.isRoot()) return null;
			var currentIndex = this.index();
			if(currentIndex < this.collection.length-1) {
				return this.collection.at(currentIndex+1);
			} else {
				return null;
			}
		},

		/**
		 * returns the node to the left
		 */
		prev: function() {
			if(this.isRoot()) return null;
			var currentIndex = this.index();
			if(currentIndex > 0) {
				return this.collection.at(currentIndex-1);
			} else {
				return null;
			}
		},

		/**
		 * removes current node if no attributes arguments is passed,
		 * otherswise remove matched children or first matched node
		 */
		remove: function(attrs, first) {
			if(!attrs) {
				if(this.isRoot()) return false; // can't remove root node
				this.collection.remove(this);
				return true;
			} else {
				if(first) {
					this.where(attrs, true).remove();
				} else {
					_.each(this.where(attrs), function(node) {
						if(node.collection) node.remove();
					});
				}
				return this;
			}
		},

		/**
		 * removes all children children
		 */
		empty: function() {
			this._children.reset();
			return this;
		},

		/**
		 * add child/children children to Backbone Collection
		 */
		add: function(node) {
			if(node instanceof Backbone.Model && node.collection) node.collection.remove(node);
			this._children.add.apply(this._children, arguments);
			return this;
		},

		/**
		 * inserts a node before the current node
		 */
		insertBefore: function(node) {
			if(!this.isRoot()) {
				if(node instanceof Backbone.Model && node.collection) node.collection.remove(node);
				this.parent().add(node, {at: this.index()});
			}
			return this;
		},

		/**
		 * inserts a node after the current node
		 */
		insertAfter: function(node) {
			if(!this.isRoot()) {
				if(node instanceof Backbone.Model && node.collection) node.collection.remove(node);
				this.parent().add(node, {at: this.index()+1});
			}
			return this;
		},

		/**
		 * shorthand for getting/inserting children before
		 */
		before: function(children) {
			if(children) return this.insertBefore(children);
			return this.prev();
		},

		/**
		 * shorthand for getting/inserting children before
		 */
		after: function(children) {
			if(children) return this.insertAfter(children);
			return this.next();
		}
	});

	var TreeCollection = Backbone.TreeCollection = Backbone.Collection.extend({
		model: TreeModel,
		where: function(attrs, opts) {
			if(opts && opts.deep) {
				var children = [];
				this.each(function(model) {
					children = children.concat(model.where(attrs));
				});
				return wrapArray(children);
			} else {
				return Backbone.Collection.prototype.where.apply(this, arguments);
			}
		}
	});

	TreeModel.prototype.collectionConstructor = TreeCollection;
	TreeModel._ = _;
	TreeModel.Backbone = Backbone;

	return TreeModel;

}));
