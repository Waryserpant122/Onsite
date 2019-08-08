ploadSpeedUnit > 0)
			uploadSpeed = uploadSpeed/(uploadSpeedUnit * fdmApp.bytesInKB);

		downloadSpeed = Math.min(fdm.speedUtils.formatFloatSpeed(downloadSpeed), downloadSpeedUnit == 2 ? 999.99 : 999999.99);
		uploadSpeed = Math.min(fdm.speedUtils.formatFloatSpeed(uploadSpeed), uploadSpeedUnit == 2 ? 999.99 : 999999.99);

		fdm.models.CustomSpeedDialog = Backbone.Model.extend({
			defaults: {
				downloadSpeedAbsolute: downloadSpeedAbsolute,
				uploadSpeedAbsolute: uploadSpeedAbsolute,
				downloadSpeed: downloadSpeed < 0 ? 0 : downloadSpeed,
				uploadSpeed: uploadSpeed < 0 ? 0 : uploadSpeed,
				downloadSpeedUnit: downloadSpeedUnit,
				uploadSpeedUnit: uploadSpeedUnit,
				downloadLimitEnabled: this._apiSettings['connection-traffic-limit-enabled'],
				uploadLimitEnabled: this._apiSettings['connection-traffic-limit-upload-enabled'],
				dialogIsShown: false
			}
		});
		this.model = new fdm.models.CustomSpeedDialog();

		this.onApplyCallback = function(){};
		this.onCancelCallback = function(){};

		//model
		//this.view_model = new fdm.viewModels.CustomSpeedDialog(this, this.model, this.collections);

	}
	Class.prototype = {
		show: function (onApplyCallback, onCancelCallback) {

			var downloadSpeedAbsolute = this._apiSettings['connection-traffic-limit-value'];
			var uploadSpeedAbsolute = this._apiSettings['connection-traffic-limit-upload-value'];

			if (!this.collections.speedValues.findWhere({value: downloadSpeedAbsolute}))
				downloadSpeedAbsolute = -1;
			if (!this.collections.speedValues.findWhere({value: uploadSpeedAbsolute}))
				uploadSpeedAbsolute = -1;

			var downloadSpeed = downloadSpeedAbsolute;
			var uploadSpeed = uploadSpeedAbsolute;
			var sds = window.app.appViewManager.getSpeedDialogState();
			var downloadSpeedUnit = sds.downloadSpeedUnit;
			var uploadSpeedUnit = sds.uploadSpeedUnit;
			downloadSpeed = downloadSpeed/Math.pow(fdmApp.bytesInKB, downloadSpeedUnit);
			uploadSpeed = uploadSpeed/Math.pow(fdmApp.bytesInKB, uploadSpeedUnit);

			downloadSpeed = Math.min(fdm.speedUtils.formatFloatSpeed(downloadSpeed), downloadSpeedUnit == 2 ? 999.99 : 999999.99);
			uploadSpeed = Math.min(fdm.speedUtils.formatFloatSpeed(uploadSpeed), uploadSpeedUnit == 2 ? 999.99 : 999999.99);

			this.model.set({
				downloadSpeedAbsolute: downloadSpeedAbsolute,
				uploadSpeedAbsolute: uploadSpeedAbsolute,
				downloadSpeed: downloadSpeed < 0 ? 0 : downloadSpeed,
				uploadSpeed: uploadSpeed < 0 ? 0 : uploadSpeed,
				downloadSpeedUnit: downloadSpeedUnit,
				uploadSpeedUnit: uploadSpeedUnit,
				downloadLimitEnabled: this._apiSettings['connection-traffic-limit-enabled'],
				uploadLimitEnabled: this._apiSettings['connection-traffic-limit-upload-enabled']
			});

			this.onApplyCallback = onApplyCallback == null ? function(){} : onApplyCallback;
			this.onCancelCallback = onCancelCallback == null ? function(){} : onCancelCallback;

			this.model.set({dialogIsShown: true});

			app.controllers.downloads.view_model.onFocusLost();
		},
		store: function(){

			var downloadSpeed = parseFloat(this.model.get("downloadSpeedAbsolute"));
			var uploadSpeed = parseFloat(this.model.get("uploadSpeedAbsolute"));

			var changes_map = {};
			changes_map['connection-traffic-limit-enabled'] = downloadSpeed >= 0;
			changes_map['connection-traffic-limit-value'] = downloadSpeed;
			changes_map['connection-traffic-limit-upload-enabled'] = uploadSpeed >= 0;
			changes_map['connection-traffic-limit-upload-value'] = uploadSpeed;

			this._apiSettings.applySettings(changes_map, this.onApplyCallback);
			window.app.appViewManager.hasChanges();
		},

		cancel: function () {
			this.close();
			_.defer(this.onCancelCallback);
		},

		close: function(){
			this.model.set({dialogIsShown: false});
			app.controllers.downloads.view_model.onFocus();
		},

		apply: function () {
			this.store();
			this.close();
		}
	};
	return Class;
})();

/*
fdm.viewModels.CustomSpeedDialog = (function () {
	function Class(controller, model, collections) {
		this._controller = controller;

		_.bindAll(this, "chooseDownloadSpeed", "chooseUploadSpeed", "onShowError", "onClearError");

		// attach to all properties that exist in default
		for (var prop in model.defaults) {
			if (model.defaults.hasOwnProperty(prop)) {
				this[prop] = kb.observable(model, prop);
			}
		}

		// attach to all collections
		for (var collection in collections) {
			if (collections.hasOwnProperty(collection)) {
				this[collection] = kb.collectionObservable(collections[collection]);
			}
		}

		this.dialogIsShown = false;
		this._controller.model.set({dialogIsShown: false});
		this.applyEnabled = true;

		this.selectedUploadSpeed = null;
		this.selectedDownloadSpeed = null;

		ko.track(this);

		this.onApplyCallback = function(){};
		this.onCancelCallback = function(){};
	};

	Class.prototype = {

		chooseDownloadSpeed: function(speedData){
		},

		chooseUploadSpeed: function(speedData){
		},
		
		handleKeydown: function(data, event) {
			if ( event.keyCode === 27 ) { // ESC
				this.cancel();
			}
			else
			if ( event.keyCode === 13 ) { // Enter
				// Change focus for apply new upload and download values.
				$("#custom-speed-dialog").focus();
				this.apply();
			}
			return true;
		},

		onShowError: function(){
			var errorFields = document.getElementById("custom-speed-dialog").querySelectorAll("input."+ko.bindingHandlers.numeric.errorClass);
			//this.applyEnabled = !errorFields.length;
		},

		onClearError: function(){
			var errorFields = document.getElementById("custom-speed-dialog").querySelectorAll("input."+ko.bindingHandlers.numeric.errorClass);
			//this.applyEnabled = !errorFields.length;
		}
	};

	return Class;
})();
*/