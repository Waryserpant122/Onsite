jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.calculateChecksumStates = {
	New: 0,
	InProgress: 1,
	Completed: 2,
	Error: 3
};

fdm.controllers.CalculateChecksumDialog = (function () {
	function Class() {

		_.bindAll(this, 'onChecksumRequested', 'onChecksumChanged');


		fdm.models.hashFunction = Backbone.Model.extend({
			defaults: {
				id: -1,
				name: "",
				state: fdm.models.calculateChecksumStates.New,
				percent: 0,
				hash: '',
				errorMsg: '',
				calculateRequestTime: 0
			}
		});
		fdm.models.hashFunctionsCollection = Backbone.Collection.extend({
			model: fdm.models.hashFunction
		});

		fdm.models.ChecksumDialog = Backbone.Model.extend({
			defaults: {
				dialogOpened: false,
				download: null,
				currentHashId: 0,
				hashFunctions: new fdm.models.hashFunctionsCollection
			}
		});
		this.model = new fdm.models.ChecksumDialog();

		fdmApp.downloads.onChecksumRequested = this.onChecksumRequested;
		fdmApp.downloads.onChecksumChanged = this.onChecksumChanged;
	}

	Class.prototype = {

		onChecksumRequested: function(data){

			var json = JSON.parse(data);

			if (!json.hash_functions || !json.hash_functions.length)
				return;

			var download_id = json.downloadId;

			var download = app.controllers.downloads.collections.downloads.get(download_id);

			if (!download)
				return;

			this.model.set(this.model.defaults);

			var hash_functions = this.model.get('hashFunctions');
			hash_functions.reset(json.hash_functions);

			var first_function = hash_functions.first();

			this.model.set({
				dialogOpened: true,
				download: download,
				currentHashId: first_function.get('id'),
				calculateRequestTime: parseInt( + new Date() / 1000 )
			});

			this.calculateChecksum(first_function);
		},

		onChecksumChanged: function(data){

			var json = JSON.parse(data);

			var download_id = json.downloadId;
			var download = this.model.get('download');
			if (!download || download.get('id') != download_id)
				return;

			var hash_functions = this.model.get('hashFunctions');

			var f = hash_functions.get(json.id);
			if (f)
				f.set(json);
		},

		changeCurrentHashFunction: function(f){

			if (this.model.get('currentHashId') == f.get('id'))
				return;

			this.model.set({
				currentHashId: f.get('id')
			});

			if (f.get('state') != fdm.models.calculateChecksumStates.Completed)
				this.calculateChecksum(f);
		},

		calculateChecksum: function(hash_func){

			var download = this.model.get('download');
			if (!download)
				return;

			fdmApp.downloads.calculateChecksum({
				downloadId: download.get('id'),
				hash_function: hash_func.get('id')
			});
			hash_func.set({
				state: fdm.models.calculateChecksumStates.InProgress,
				percent: 0
			});
		},

		close: function(){

			var download = this.model.get('download');
			if (!download)
				return;

			fdmApp.downloads.cancelChecksum({downloadId: download.get('id')});

			this.model.set(this.model.defaults);
		}
	};

	return Class;
})();

