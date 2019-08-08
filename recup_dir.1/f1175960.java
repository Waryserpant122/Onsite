jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.calculateChecksumStates = {
	New: 0,
	InProgress: 1,
	Completed: 2,
	Error: 3
};

fdm.controllers.ScheduleDialog = (function () {
	function Class() {

		_.bindAll(this, 'onScheduleRequested', 'onChecksumChanged');

		fdm.models.ScheduleTimetable = Backbone.Model.extend({
			defaults: {
				data: {
					daysEnabled: {
						1: true,
						2: true,
						3: true,
						4: true,
						5: true,
						6: true,
						7: true
					},
					endTime: 172800000,
					startTime: 21600000
				},
				id: 3,
				state: 1,
				type: 3
			}
		});

		fdm.models.ScheduleDialog = Backbone.Model.extend({
			defaults: {
				dialogOpened: false,
				downloadIds: [],
				timetable: new fdm.models.ScheduleTimetable
			}
		});

		this.model = new fdm.models.ScheduleDialog;

		fdmApp.downloads.scheduleDownloadOnDays = this.onScheduleRequested;
	}

	Class.prototype = {

		onScheduleRequested: function(data){

			var json = JSON.parse(data);

			var state = {};
			state.dialogOpened = true;
			state.downloadIds = json.downloadIds;

			this.model.get('timetable').set(json.timetable);
			this.model.set(state);
		},

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

		cancel: function(){

			fdmApp.downloads.cancelScheduleForDownloads({downloadIds: this.model.get('downloadIds')});

			this.model.set(this.model.defaults);
		},

		close: function(){

			this.model.set(this.model.defaults);
		},

		apply: function(){

			var res = {};

			res.downloadIds = this.model.get('downloadIds');
			res.timetable = this.model.get('timetable').toJSON();

			fdmApp.downloads.scheduleDownloads(res);

			this.model.set(this.model.defaults);
		}
	};

	return Class;
})();

