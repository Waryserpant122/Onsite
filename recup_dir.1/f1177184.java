jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");


fdm.controllers.TagsManageDialog = (function () {
	
	function Class(apiTags) {

		this._apiTags = apiTags;
		
		this.collections = {};
		
		_.bindAll(this, 'onTagsChanged', 'onTagCreated', 'onTagEdited', 'onTagDeleted', 'getAllTags', 'submitNewName', 'onAddNewTag');
				
		this.promiseTagsChanged = null;
		var promiseGetAllTags = this.getAllTags();
	
		promiseGetAllTags.then(function(allTagsData){
			fdm.models.TagsManageDialog = Backbone.Model.extend({
				defaults: {
					editedTagId: null,
					editedTagError: '',
					editedTagNewName: null,
					allTags: new Backbone.Collection(allTagsData),
					tagFormOpened: false
				}
			});		
		}.bind(this)).then(function(){
			this.model = new fdm.models.TagsManageDialog();

			this._apiTags.addEventListener("onTagsChanged", this.onTagsChanged);
			this._apiTags.addEventListener("onTagCreated", this.onTagCreated);
			this._apiTags.addEventListener("onTagEdited", this.onTagEdited);
			this._apiTags.addEventListener("onTagDeleted", this.onTagDeleted);

		}.bind(this));
	}

	Class.prototype = {
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
		createTag: function (name, color, downloadIds) {
			var colorR = color.r != undefined ? color.r : -1;
			var colorG = color.g != undefined ? color.g : -1;
			var colorB = color.b != undefined ? color.b : -1;
			this._downloads = downloadIds;
			this._apiTags.createTag(name, parseInt(colorR), parseInt(colorG), parseInt(colorB), function(new_tag_id){
                if (downloadIds && downloadIds.length)
                {
                    for (var i =0; i < downloadIds.length; i++)
                    {
                        app.controllers.downloads.callItemMethodById(downloadIds[i], "addTag", [new_tag_id]);
                    }
                }
            });
		},
		editTag: function (id, name, color) {
			var colorR = color.r != undefined ? color.r : -1;
			var colorG = color.g != undefined ? color.g : -1;
			var colorB = color.b != undefined ? color.b : -1;

			if (colorR < 0 && colorG < 0 && colorB < 0)
				this._apiTags.editTag(id, name);
			else
				this._apiTags.editTag(id, name, parseInt(colorR), parseInt(colorG), parseInt(colorB));
		},
		deleteTag: function (id) {
			this._apiTags.deleteTag(parseInt(id));
		},
		onTagCreated: function(tagData){
			tagCreated = JSON.parse(tagData)
			this.model.get("allTags").add(tagCreated);
		},
		onTagEdited: function(tagData){
			var tagEdited = JSON.parse(tagData)
			var foundTag = this.model.get("allTags").get(tagEdited.id);
			if (foundTag){
				foundTag.set(tagEdited);
				//this.model.set({
				//	editedTagId: null,
				//	editedTagError: '',
				//	editedTagNewName: null
				//});
			}
		},
		onTagDeleted: function(tagId){

			var selected_tag = app.controllers.tags.model.get('selectedTag');
			if (selected_tag && selected_tag.id == tagId)
				app.controllers.tags.model.set('selectedTag', null);

			this.model.get("allTags").remove(tagId);
		},
		onTagsChanged: function(){
			var promiseAllTags = this.getAllTags();
			this.promiseTagsChanged = promiseAllTags.then(function(allTagsData){
				this.model.get("allTags").set(allTagsData);
			}.bind(this));		
		},

		submitNewName:function(id, newName) {

			var result = {error: false};

			var tag_m = this.model;

			if (!newName)
			{
				result.error = true;
				result.errorMessage = __('Empty tag name');
				return result;
			}

			var tag = tag_m.get('allTags').get(id);

			if (parseInt(id) < 100 || !tag)
			{
				result.error = true;
				result.errorMessage = __('Tag not found');
				return result;
			}
			var name = tag.get('name');

			if (name == newName){
				return result;
			}

			if (newName == null || newName == ""){
				return result;
			}
			newName = newName.trim();
			var found = false;
			var tags = tag_m.get('allTags').models;
			for (var i = 0; i < tags.length; i++ ){
				if (tags[i].get('name').toLowerCase() == newName.toLowerCase()){
					found = true;
					break;
				}
			}

			if(found){

				result.error = true;
				result.errorMessage =  __("Tag with the same name already exists.");
				return result;
			}

			var color = {};
			this.editTag(parseInt(id),newName, color);
			return result;
		},

		onAddNewTag: function(name, color) {

			var result = {error: false};

			name = name.trim();

			var newName = name.toLowerCase();
			var found = false;
			var tags =  this.model.get('allTags').models;
			for (var i = 0; i < tags.length; i++ ){
				if (tags[i].get('name').toLowerCase() == newName){
					found = true;
					break;
				}
			}

			//var found = ko.utils.arrayFirst(this.allTags, function(item) {
			//	return name.toLowerCase() === item.name.toLowerCase();
			//});
			if(found){
				result.error = true;
				result.errorMessage = __('Tag with the same name already exists');

				return result;
			}
			var  downloadIds = [];
			//if(!this.customTags){// not wizard
			//	downloadIds = this._controller._downloads;
			//}
			this.createTag(name, color, downloadIds);

			return result;
		}
	};

	return Class;
})();
