jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.controllers.TrafficPanel = (function () {

	function Class(rootApi, downloadsCtrl, settingsCtrl, bottomPanelCtrl) {
		this._rootApi = rootApi;
		this._downloadsCtrl = downloadsCtrl;
		this._settingsCtrl = settingsCtrl;
		this._bottomPanelCtrl = bottomPanelCtrl;

		var customSpeedMode = this._rootApi.speedMode;
		fdm.models.TrafficPanel = Backbone.Model.extend({
			defaults: {
				customSpeedMode: customSpeedMode,
				totalDownloadSpeed: 0,
				totalUploadSpeed: 0,
				speedDialogOpened: false
			}
		});
		this.model = new fdm.models.TrafficPanel();

		this.model.on("change:customSpeedMode", function(model, rawValue, options){

			this._rootApi.speedMode = rawValue;
			//this._bottomPanelCtrl.model.set("customSpeedMode", rawValue);

		}, this);

		this.collections = {};

		this.collections.downloadSpeedTypes = new Backbone.Collection([
			{id: fdm.models.NetworkSpeedMode.Custom, text: "Manual...", title: "Set limits manually"},
			{id: fdm.models.NetworkSpeedMode.High, text: "High", title: "Download: unlimited, Upload: unlimited"},
			{id: fdm.models.NetworkSpeedMode.Medium, text: "Medium", title: ""},
			{id: fdm.models.NetworkSpeedMode.Low, text: "Low", title: ""}
		]);

		//model
		//this.view_model = new fdm.viewModels.TrafficPanel(this, this.model, this.collections, this._downloadsCtrl.view_model, this._bottomPanelCtrl.view_model, popupMgr);

		//this.view_model.setSpeedMode(this.model.get("customSpeedMode"));
		//this._bottomPanelCtrl.model.set("customSpeedMode", this.model.get("customSpeedMode"));

		this._rootApi.downloads.onTotalSpeedChanged = _.bind(this.refreshTotalSpeed, this);

		settingsCtrl.model.on('change', this.refreshSpeedTypeTitles, this);
		this.refreshSpeedTypeTitles();
	}
	Class.prototype = {

		refreshTotalSpeed: function(downloadSpeed, uploadSpeed){
			this.model.set({
				totalDownloadSpeed: downloadSpeed,
				totalUploadSpeed: uploadSpeed
			})
		},

		refreshSpeedTypeTitles: function(){

			var dst = this.collections.downloadSpeedTypes;
			var sm = app.controllers.settings.model.get('settings');

			var medium_title =
				__('Download:') + ' '
				+ ( sm['tum-medium-restriction-type'] === 0 ? __('unlimited') : fdm.speedUtils.speed2SignDigits(sm['tum-medium-restriction-absolute'])) +
				', ' + __('Upload:') + ' '
				+ ( sm['tum-medium-upload-restriction-type'] === 0 ? __('unlimited') : fdm.speedUtils.speed2SignDigits(sm['tum-medium-upload-restriction-absolute']));
			var low_title =
				__('Download:') + ' '
				+ ( sm['tum-low-restriction-type'] === 0 ? __('unlimited') : fdm.speedUtils.speed2SignDigits(sm['tum-low-restriction-absolute'])) +
				', ' + __('Upload:') + ' '
				+ ( sm['tum-low-upload-restriction-type'] === 0 ? __('unlimited') : fdm.speedUtils.speed2SignDigits(sm['tum-low-upload-restriction-absolute']));

			dst.get(fdm.models.NetworkSpeedMode.Medium).set("title", medium_title);
			dst.get(fdm.models.NetworkSpeedMode.Low).set("title", low_title);
			dst.get(fdm.models.NetworkSpeedMode.Custom).set("title", __("Set limits manually"));
			dst.get(fdm.models.NetworkSpeedMode.High).set("title", __("Download: unlimited, Upload: unlimited"));
		},

		getSettingsSpeedLimitPart: function(modeName){
			var settings = this._rootApi.settings;

			switch(settings['tum-'+modeName+'-restriction-type']){
			//case 2:
			//	var trafficRestrictionPercent = settings['tum-'+modeName+'-restriction-percentage'];
			//	return trafficRestrictionPercent + "%";
			case 1:
				var trafficRestriction = settings['tum-'+modeName+'-restriction-absolute'];
				return fdm.fileUtils.fileSizeIEC(trafficRestriction);
			case 0:
				return "no";
			}
		},

		toggleSpeedDialog: function() {
			if (this.model.get('speedDialogOpened'))
				this.closeSpeedDialog();
			else
				this.openSpeedDialog();
		},
		openSpeedDialog: function() {
			this.model.set({speedDialogOpened: true});
			this._downloadsCtrl.view_model.onFocusLost();
		},
		closeSpeedDialog: function() {
			this.model.set({speedDialogOpened: false});
			this._downloadsCtrl.view_model.onFocus();
		},
		chooseSpeed: function(speed_type){
			var speedMode = speed_type.id;
			if(speedMode == -1){

				this.model.set({customSpeedMode: speedMode});
			}
			else if(speedMode == fdm.models.NetworkSpeedMode.Custom){
				window.app.controllers.customSpeedDialog.show(_.bind(function(){
					this.customSpeedMode = fdm.models.NetworkSpeedMode.Custom;

					this.model.set({customSpeedMode: speedMode});
				}, this));
			}
			else{
				this.model.set({customSpeedMode: speedMode});
			}
			this.closeSpeedDialog();
		},
		bottomPanelVisibility: function() {

			app.controllers.bottomPanel.showToggle();
		}
	};
	return Class;
})();

/*
fdm.viewModels.TrafficPanel = (function () {
	var _controller; // use local variable to hide some members from a view
	var _downloadsView;
	var _bottomPanelView;

	function Class(controller, model, collections, downloadsView, bottomPanelView, popupMgr) {
		_controller = controller;
		_downloadsView = downloadsView;
		_bottomPanelView = bottomPanelView;

		_.bindAll(this, "closeAllPopups", 'closeDownloadPopup');

        this.bottomPanelCanBeOpened = function() {
            return _downloadsView.downloads.length > 0;
        };
        this.needShowBottomPanelOpener = function(){
            return this.bottomPanelCanBeOpened() && !_bottomPanelView.panelVisible;
        };

	};

	Class.prototype = {
		bottomPanelVisibility: function() {
			_bottomPanelView.showToggle();
		},

		closeDownloadPopup: function(){

			_controller.closeSpeedDialog();
		},

		closeAllPopups: function () {
			_controller.closeSpeedDialog();
		},


	};

	return Class;
})();
*/