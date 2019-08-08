jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");


fdm.models.preventSleepAction = {
	Shutdown: 0,
	Sleep: 1,
	Hibernate: 2
};
fdm.models.chooseThemeValues = {
    Light: 0,
    Dark: 1
};

fdm.controllers.Settings = (function () {

	function Class() {

		fdm.models.speedValues = [
			{value: 32 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 32}},
			{value: 64 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 64}},
			{value: 128 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 128}},
			{value: 256 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 256}},
			{value: 512 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 512}},
			{value: 1 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: fdmApp.bytesInKB}},
			{value: 1.5 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 1.5 * fdmApp.bytesInKB}},
			{value: 2 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 2 * fdmApp.bytesInKB}},
			{value: 4 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 4 * fdmApp.bytesInKB}}
		];

		fdm.models.Settings = Backbone.Model.extend({
			defaults: {
				opened: false,
				defaultTrtClientDialogOpened: false,
				shutDownWhenDone: false,
				activeFilterText: '',
				//currentTab: 'general', // general, tum, history
				settings: {},
				resetSettingsDialogOpened: false,
				language: '',
				systemLocale: '',
				installedTranslations: [],
				showLinkCatchingMsg: false,
                showFFExtensionUpdater: false,
                chooseTheme: null,
			},
			sync: function(){

				var res = {};

				for(var index in fdmApp.settings) {
					if (fdmApp.settings.hasOwnProperty(index)) {
						var attr = fdmApp.settings[index];
						if (typeof attr != 'function' && index[0] != '_'){
							res[index] = attr;
						}
					}
				}

				this.set({
					settings: res,
					shutDownWhenDone: res['shutdown-when-done'],
					preventSleepAction: res['prevent-sleep-action'],
					language: res['locale-value'],
					preventSleepIfScheduledDownloads: res['prevent-sleep-if-scheduled-downloads']
				});
			},
			proxySettingsNames: [
				"proxy-http-settings-address",
				"proxy-http-settings-port",
				"proxy-http-settings-login-name",
				"proxy-http-settings-login-password",
				"proxy-https-settings-address",
				"proxy-https-settings-port",
				"proxy-https-settings-login-name",
				"proxy-https-settings-login-password",
				"proxy-ftp-settings-address",
				"proxy-ftp-settings-port",
				"proxy-ftp-settings-login-name",
				"proxy-ftp-settings-login-password"
			],
			syncProxy: function(){

				var settings = this.get('settings');

				this.proxySettingsNames.forEach(function(name){

					settings[name] = fdmApp.settings[name];
				});

				this.trigger('change');
			}
		});


		this.model = new fdm.models.Settings;

		//this.model.on('change:shutDownWhenDone change:defaultTorrentClientDialogOpened', this.tagsPanelDialogChanged, this);

		var tr = fdmApp.localization.installedTranslations();
		if (typeof tr === 'string')
			tr = [tr];

		this.model.set({
			installedTranslations: tr.sort(),
			systemLocale: fdmApp.localization.systemLocale()
		});

		this.model.sync();

		this.dispatcherIndex = FdmDispatcher.register(function(payload) {

			if (payload.source == 'VIEW_ACTION'){
				if (payload.action.actionType == 'WindowOnFocus')
					return this.windowOnFocus();
			}

			return true; // No errors. Needed by promise in Dispatcher.
		}.bind(this));

		_.bindAll(this, 'openFolderCallback', 'onSettingsChanged', 'newLanguageLoaded', 'showFfExtensionUpdater');

		fdmApp.system.addEventListener("openFolderCallback", this.openFolderCallback);
		fdmApp.settings.onSettingsChanged = this.onSettingsChanged;
		fdmApp.system.showFFExtensionUpdater = this.showFfExtensionUpdater;
		fdmApp.system.showFfExtensionUpdater = this.showFfExtensionUpdater;
		fdmApp.system.onJsonTaskFromWebExtension = this.onJsonTaskFromWebExtension;

		fdmApp.localization.allStringsLoaded = this.newLanguageLoaded;

        this.model.set(window.app.appViewManager.getSettingsState());
        this.model.on('change:chooseTheme', app.appViewManager.hasChanges, app.appViewManager);
        this.model.on('change:chooseTheme', this.setTheme, this);
        this.setTheme();

		if (!fdmApp.AssociateWithTorrentsDisabled && this.model.get('settings')['check-default-torrent-client-at-startup']){

			var fdm_is_default = fdmApp.settings.checkDefaultTorrentClient();

			if (!fdm_is_default)
				this.model.set({
                    defaultTrtClientDialogOpened: true
				})
		}
	}

	Class.prototype = {

        onJsonTaskFromWebExtension: function(message) {
            if (typeof message === 'string') {
                message = JSON.parse(message);
            }
            if (message.type === 'optionsClick') {
                app.controllers.settings.showTab('monitoring_tab');
                fdmApp.system.activateMainWindow();
            }
        },

        setTheme: function() {
        	const theme = this.model.get('chooseTheme');
        	if (theme === fdm.models.chooseThemeValues.Light) {
        		document.body.classList.remove('dark_theme');
			} else {
                document.body.classList.add('dark_theme');
			}
		},

        showFfExtensionUpdater: function() {
        	this.model.set({
                showFFExtensionUpdater: true
			});
		},

		initLocalization: function(){

			var current_language = this.model.get('language');
			if (current_language == '')
				current_language = this.model.get('systemLocale');

			var all_l = this.model.get('installedTranslations');
			if ( all_l.indexOf(current_language) < 0 )
				current_language = 'en';

			var strings;
			if (fdmApp.isFake)
				strings = fdmApp.localization.getAllStrings(current_language);
			else
				strings = fdmApp.localization.getAllStrings();

			Strings.setAllStrings(current_language, strings);

			_.each(document.body.classList, function(class_name){

				if (all_l.indexOf(class_name) >= 0){

					document.body.classList.remove(class_name);
				}
			});

			document.body.classList.add(current_language);
		},
		setLanguage: function(new_locale){

			this.saveSetting('locale-value', new_locale);
			this.model.set({language: new_locale});

			if (fdmApp.isFake)
				this.newLanguageLoaded(new_locale);

		},
		newLanguageLoaded: function(new_locale){

			//if (this.model.get('language') == new_locale){

				var strings;
				if (fdmApp.isFake)
					strings = fdmApp.localization.getAllStrings(new_locale);
				else
					strings = fdmApp.localization.getAllStrings();

				Strings.setAllStrings(new_locale, strings);
				startReactApp();
			//}

			var all_l = this.model.get('installedTranslations');

			_.each(document.body.classList, function(class_name){

				if (all_l.indexOf(class_name) >= 0){

					document.body.classList.remove(class_name);
				}
			});

			document.body.classList.add(new_locale);
		},
		onSettingsChanged: function () {

			this.model.sync();
		},
		//tagsPanelDialogChanged: function(){
        //
		//	if (this.model.get('shutDownWhenDone') || this.model.get('defaultTorrentClientDialogOpened')){
        //
		//		$("body").addClass("shut-down-when-done-message");
		//	}
		//	else{
		//		$("body").removeClass("shut-down-when-done-message");
		//	}
		//},
		openFolderCallback: function (targetFolder, flag) {

			if (flag == 'settings-default-folder')
			{
				FdmDispatcher.handleViewAction({
					actionType: 'SettingsDefaultFolderCallback',
					content: {
						targetFolder: targetFolder
					}
				});
			}
		},
		windowOnFocus: function(){

			if (this.model.get('opened')){
				this.model.sync();
			}
		},
		show: function () {
			//app.closeAllPopups();

			document.body.classList.add('show_settings');

			if (this.model.get('resetSettingsDialogOpened'))
				this.model.set({
					resetSettingsDialogOpened: false
				});

			this.model.sync();
			this.model.set({
				opened: true,
				activeFilterText: ''
			});

			//this.settings.set(this._rawSettings);
			//this.view_model.show(onCloseCallback);
			//this._rawSettings.onDialogInit();
			//this.changedSettings = {};
		},
		showTab: function (tab_id) {

			this.show();

			_.defer(function(){
				FdmDispatcher.handleViewAction({
					actionType: 'showSettingsTab',
					content: {
						tabId: tab_id
					}
				});
			});
		},
		cancel: function(){
			this.close();
		},
		close: function(){

			document.body.classList.remove('show_settings');

			if (this.model.get('resetSettingsDialogOpened'))
				this.model.set({
					resetSettingsDialogOpened: false
				});

			var opened = this.model.get('opened');
			if (opened){
				this.model.set({
					opened: false,
					activeFilterText: ''
				});
				this.model.sync();
			}
		},
		applyTimeout: 0,
		applyProxyTimeout: 0,
		saveSetting: function(name, value){
			if (fdmApp.settings.hasOwnProperty(name)){
				fdmApp.settings[name] = value;
				this.model.get('settings')[name] = value;
				this.model.trigger('change');

				if (name == 'locale-value')
					this.model.set({language: value});

				if (name == 'proxy-settings-source'){

					if (this.applyProxyTimeout > 0)
						clearTimeout(this.applyProxyTimeout);
					this.applyProxyTimeout = setTimeout(function(){
						this.applyProxyTimeout = 0;
						fdmApp.settings.onDialogApply();
						setTimeout(function(){
							this.model.syncProxy();
						}.bind(this), 100);

					}.bind(this), 100);
				}
				else{
					if (this.applyTimeout > 0)
						clearTimeout(this.applyTimeout);
					this.applyTimeout = setTimeout(function(){
						this.applyTimeout = 0;
						fdmApp.settings.onDialogApply();
					}.bind(this), 1000);
				}
			}
		},

		checkStoreSettings: function() {
			var rawAttributes = this.settings.attributes;
			
			if (false && rawAttributes["virus-check-enabled"]) {
				var checkValueAntivirusPathSettings = this._rawSettings.checkAntivirusPathSettings (rawAttributes["virus-check-predefined-app-enabled"], 
												rawAttributes["virus-check-predefined-app-value"], rawAttributes["virus-check-custom-app-path"]);
				
				// Check Antivirus Path Setting
				if (!checkValueAntivirusPathSettings) {
					// Try manually determine path to predefined Antivirus.
					this.settings.set("virus-check-custom-app-enabled", true);
					this.settings.set("virus-check-predefined-app-enabled", false);
					
					var virusPredefinedItem = this.collections.antiViruses.get(rawAttributes["virus-check-predefined-app-value"]);
					this.settings.set("virus-check-custom-app-args", virusPredefinedItem.attributes.args);

                    var customAntivirusPath = window.fdmApp.system.openFileDialog([{name:"Application",extensions:"*.exe", TODO:"see other openFileDialog example in fdm.menu.js"}]);
					if (customAntivirusPath != "") {
						this.settings.set("virus-check-custom-app-path", pathCustomAntivirusPath);
					}
					else {
						this.settings.set("virus-check-enabled", false);
					}
					return false;
				}
				
				// Check Antivirus custom Arguments
				if (!this._rawSettings.checkAntivirusCustomArgSettings(rawAttributes["virus-check-custom-app-args"])) {
					return false;
				}
				
				// Check Antivirus custom Extensions
				if (!this._rawSettings.checkAntivirusExtSettings(rawAttributes["virus-check-file-extensions"])) {
					return false;
				}
			}
			return true;
		},
		store: function () {
			var rawAttributes = this.settings.attributes;
			if (!this.checkStoreSettings()) {
				return false;
			}
			// Store only changed property.
			if (Object.keys(this.changedSettings).length > 0)
			{
				var changes_map = {};
				for (var prop in this.changedSettings) {
					if (rawAttributes.hasOwnProperty(prop)) {
						changes_map[prop] = rawAttributes[prop];
						//this._rawSettings[prop] = rawAttributes[prop];
					}
				}
				this._rawSettings.applySettings(changes_map, function(){
					this._rawSettings.onDialogApply();
				}.bind(this));
			}

			return true;
		},

		clearFolderHistory: function() {
			this._rawSettings.clearFolderHistory();
		},

		clearDownloadHistory: function() {
			this._rawSettings.clearDownloadHistory();
		},

		chooseCustomTargetFolder: function() {
			fdmApp.system.openFolderDialog(this.settings.get("target-folder-user-defined-value"), 'settings-choose-folder');
		},

		chooseCustomTargetFolderCallback: function(targetFolder, flag) {
			if (flag == 'settings-choose-folder')
				this.settings.set("target-folder-user-defined-value", targetFolder);
		},

		chooseAntivirusPath: function() {
			this.settings.set("virus-check-custom-app-path",
                window.fdmApp.system.openFileDialog([{name:"Application",extensions:"*.exe", TODO:"see other openFileDialog example in fdm.menu.js"}]));
		},

		toggleSkipSmaller: function() {
			var disabled = !$('#monitoring-skip-smaller-enabled').prop('checked');
			if(disabled)
				this.settings.set("monitoring-skip-smaller-value", 0);
		},
		openChromePluginPage: function(){
			this._rawSettings.openChromePluginPage();
		},
        openFirefoxPluginPage: function(){
			this._rawSettings.openFirefoxPluginPage();
		},
		openChromeDownloadPage: function(){
			this._rawSettings.openChromeDownloadPage();
		},
		getRaw: function(prop){
			return this._rawSettings[prop];
		},
		resetSettings: function(){

			fdmApp.settings.resetToDefault(function(){

				app.controllers.downloads.model.set({
					deleteDialogChoice: fdm.models.deleteDialogChoice.notSave,
					deleteDialogCheckbox: 0
				});
				this.model.sync();
			}.bind(this));
		}
	};

	return Class;
})();
