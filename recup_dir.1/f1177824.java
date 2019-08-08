jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.defaultColors = [
	{"r":213,"g":71,"b":68},{"r":2,"g":18,"b":132},{"r":0,"g":165,"b":138},
	{"r":36,"g":76,"b":68},{"r":108,"g":70,"b":28},{"r":231,"g":114,"b":50},
	{"r":116,"g":29,"b":82},{"r":5,"g":148,"b":202},{"r":126,"g":211,"b":80},
	{"r":255,"g":192,"b":0},{"r":255,"g":163,"b":97},{"r":255,"g":127,"b":121},
	{"r":243,"g":84,"b":176},{"r":209,"g":62,"b":188},{"r":0,"g":201,"b":203},
	{"r":187,"g":203,"b":0},{"r":203,"g":81,"b":0},{"r":203,"g":0,"b":143},
	{"r":96,"g":0,"b":203},{"r":0,"g":81,"b":203},{"r":0,"g":153,"b":203},
	{"r":78,"g":101,"b":110},{"r":46,"g":243,"b":0},{"r":243,"g":0,"b":0}
];

fdm.controllers.Tags = (function () {
	
	function Class(apiTags) {

		fdm.models.TagsCollection = Backbone.Collection.extend({
			comparator: function(a, b) {

				var b_s = b.get('system');
				var a_s = a.get('system');

				if (b_s != a_s){
					return a_s ? -1 : 1;
				}

				return b.get('name').toLowerCase() > a.get('name').toLowerCase() ? -1 : 1;
			}
		});

		this._apiTags = apiTags;

		this.collections = {};
		this.collections.allTags = new fdm.models.TagsCollection;
		
		_.bindAll(this, 'onTagsChanged', 'onTagCreated', 'onTagEdited', 'onTagDeleted', 'getAllTags');
		fdm.models.Tags = Backbone.Model.extend({
			defaults: {
				selectedTag: null,
				viewTag: null,
				showMoreOpened: false,
				statusFilterOpened: false
			}
		});
		this.model = new fdm.models.Tags();

		this._apiTags.addEventListener("onTagsChanged", this.onTagsChanged);
		this._apiTags.addEventListener("onTagCreated", this.onTagCreated);
		this._apiTags.addEventListener("onTagEdited", this.onTagEdited);
		this._apiTags.addEventListener("onTagDeleted", this.onTagDeleted);

		this.model.on('change:viewTag', app.appViewManager.hasChanges, app.appViewManager);
	}

	Class.prototype = {
		init: function(){
			console.log("fdm.controllers.Tags.init");
			var promiseGetAllTags = this.getAllTags();
		
			promiseGetAllTags.then(function(allTagsData){
				this.allTags = allTagsData;
			}.bind(this)).then(function(){
				this.collections.allTags.reset(this.allTags);

                //this.collections.allTags.on('all', function(){
                //    if (typeof rjs_render != 'undefined') rjs_render.add('all');
                //});

				//model
				//this.view_model = new fdm.viewModels.Tags(this, this.model, this.collections);
				
				//binding
				//$("#TagsPanel").load("tags-panel.tpl", function () {
				//	ko.applyBindings(this.view_model, $('#TagsPanel')[0]);
				//}.bind(this));
			}.bind(this)).then(function(){
				this.restoreState();
			}.bind(this));
			
		},
		
		getAllTagsCallbackResult: function(resolve, allTags) {
			resolve(allTags);
		},
		getAllTagsPromiseFunc: function(resolve, reject){
			this._apiTags.getAllTags(_.partial(this.getAllTagsCallbackResult, resolve));
		},
		getAllTags: function () {
			var promise = new Promise(this.getAllTagsPromiseFunc.bind(this));
			return promise;
		},		
		onTagsChanged: function(){
			var promiseAllTags = this.getAllTags();
			promiseAllTags.then(function(allTagsData){
				//this.model.get("allTags").set(allTagsData);
				this.allTags = allTagsData;
				this.collections.allTags.set(this.allTags);
				
			}.bind(this));
		},
		onTagEdited: function(tagData){
			var tagEdited = JSON.parse(tagData);
			this.collections.allTags.add(tagEdited);
			var foundTag = this.collections.allTags.get(tagEdited.id);
			if (foundTag){
				foundTag.set(tagEdited);
			}
			FdmDispatcher.handleViewAction({
				actionType: 'onTagChanged',
				content: {
					tagId: tagEdited.id
				}
			});
		},
		onTagDeleted: function(tagId){
			this.collections.allTags.remove(tagId);
			if (this.model.get('viewTag').id == tagId)
				this.restoreState();
		},
		onTagCreated: function(tagData){
			var tagCreated = JSON.parse(tagData);
			var tag = this.collections.allTags.add(tagCreated);
			//this.model.set({viewTag: tag});
		},
		setViewTag: function(tag_id){
			this.model.set({viewTag: this.collections.allTags.get(tag_id)});
		},
		resetSelectedTag: function(){
			this.model.set('selectedTag', null);
		},
		restoreState: function(){
			var viewState = window.app.appViewManager.getTagsPanelState();

			if (viewState.viewTagId > 0 && this.collections.allTags.get(viewState.viewTagId)){
				this.model.set({viewTag: this.collections.allTags.get(viewState.viewTagId)});
			}
			else{
				var tag_found = false;
				for (var i = 0; i < this.collections.allTags.models.length; i++){
					//if (!this.collections.allTags.models[i].get('system')){
						this.model.set({viewTag: this.collections.allTags.models[i]});
						tag_found = true;
						break;
					//}
				}
				if (!tag_found)
					this.model.set({viewTag: null});
			}

			//this.expanded = viewState.expanded;
			//this.selectTagById(viewState.selectedTagId);

			//if(!viewState.visible){
			//	this.toggleOpen();
			//}
		},
		selectTagById: function(tagId /*can be null or undefined*/){
			var selectedTag = null;
			if (tagId != null){

				selectedTag = this.collections.allTags.get(tagId);
			}
			this.model.set('selectedTag', selectedTag);
			if (selectedTag)
				this.model.set('viewTag', selectedTag);
		},
		chooseTag: function(tagData, element){
			this.selectTagById(tagData.id);
		},
		//toggleOpen: function(){
		//	if (this.visible)
		//	{
		//		this.visible = false;
		//		$("body").addClass("tags-panel-hidden");
		//	}
		//	else
		//	{
		//		this.visible = true;
		//		$("body").removeClass("tags-panel-hidden");
		//	}
		//	//app.controllers.downloads.view_model.applyFilters();
		//	rjs_render.add('all', 'high');
		//},
		//toggleMoreTags: function(elem){
		//	this.expanded = !this.expanded;
		//	rjs_render.add('tag_panel', 'high');
		//},
		//setFocus: function(){
		//	$( "#header" ).focus();
		//},
		getRandomTagColor: function(remove_color){

			remove_color = remove_color || false;

			var new_colours = [];
			fdm.models.defaultColors.map(function(color){

				if (!this.collections.allTags.findWhere({colorR: color.r, colorG: color.g, colorB: color.b})
					&& !(remove_color && remove_color.r == color.r && remove_color.g == color.g && remove_color.b == color.b))
					new_colours.push(color);

			}.bind(this));

			if (new_colours.length)
				return new_colours[_.random(0, new_colours.length - 1)];
			else
				return fdm.models.defaultColors[_.random(0, fdm.models.defaultColors.length - 1)];
		}
	};

	return Class;
})();
