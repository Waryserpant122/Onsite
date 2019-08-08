jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.SimpleDialogButtons = {
	None:   0,
	OK:     1,
	Cancel: 2
};

fdm.controllers.SimpleDialogs = (function () {
	function Class() {

		_.bindAll(this, 'newDialog');

		fdm.models.SimpleDialogs = Backbone.Model.extend({
			defaults: {
				id: -1,
				opened: false,
				title: 'Title',
				message: 'Message',
				timeout: -1,
				buttons: [1, 2],
				timeoutButton: fdm.models.SimpleDialogButtons.OK
			}

		});
		this.model = new fdm.models.SimpleDialogs({id: fdm.models.UpdaterManagers.Client});

		fdmApp.simpleDialogs.newDialog = this.newDialog;
	}

	Class.prototype = {

		newDialog: function(id, data){

			if (this.timeInterval > 0)
				this.stopTimeInterval();

			var json = JSON.parse(data);

			json.opened = true;
			json.id = id;

			this.model.set(json);

			if (json.timeout > 0)
				this.startTimeInterval();
		},
		close: function(){

			var id = this.model.get('id');
			if (id > 0)
				fdmApp.simpleDialogs.onDialogClosed(id, {buttonClicked:2});

			this.model.set(this.model.defaults);
		},
		submit: function(){

			var id = this.model.get('id');
			if (id > 0)
				fdmApp.simpleDialogs.onDialogClosed(id, {buttonClicked:1});
			this.model.set(this.model.defaults);
		},
		submitByTimeout: function(){

			var id = this.model.get('id');
			if (id > 0)
				fdmApp.simpleDialogs.onDialogClosed(id, {buttonClicked: this.model.get('timeoutButton')});
			this.model.set(this.model.defaults);
		},

		timeInterval: 0,
		startTimeInterval: function(){

			if (this.timeInterval > 0)
				this.stopTimeInterval();

			this.timeInterval = setInterval(_.bind(this.doInterval, this), 1000);
		},
		stopTimeInterval: function(){

			clearInterval(this.timeInterval);
			this.timeInterval = 0;
		},
		doInterval: function(){

			var c = this.model.get('timeout');
			c--;
			this.model.set({timeout: c});

			if (c <= 0){
				this.stopTimeInterval();
				this.submitByTimeout();
			}
		}
	};

	return Class;
})();

