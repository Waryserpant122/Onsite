jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.MenuActionsBitmask = {
	SelectionCanBeDownloaded : 1 << 0,
	SelectionCanBeMoved      : 1 << 1,
	SelectionCanBePaused     : 1 << 2,
	SelectionCanBeRemoved    : 1 << 3,
	SelectionCanBeRestarted  : 1 << 4,
	CanOpenTrtFile       : 1 << 5,
	CanShowSettings          : 1 << 6,
	CanAddDownloadFromUrl    : 1 << 7,
	CanCheckForUpdates       : 1 << 8
};

fdm.controllers.Menu = (function () {
	function Class(apiMenu, apiSystem) {
		this.collections = {};
		this._apiMenu = apiMenu;
		this._apiSystem = apiSystem;

		_.bindAll(this, 'addDownload', 'showSettings', 'openTrtFileHandler', 'openTrtFileCallback',
			'getMenuActionsEnabled', 'editTagHandler', 'removeTagHandler', 'chooseFilesHandler',
			'checkForUpdatesHandler', 'pauseExceptSelected');

		fdm.models.Menu = Backbone.Model.extend({
			defaults: {
				opened: false,
				showMenuGreenMarker: false,
			}
		});
		this.model = new fdm.models.Menu();

		//this.view_model = new fdm.viewModels.Menu(this, this.collections);
		this._apiMenu.onAddDownload = this.addDownload;
		this._apiMenu.onShowSettings = this.showSettings;
		this._apiMenu.onOpenTorrentFile = this.openTrtFileHandler;
		this._apiSystem.addEventListener("openFileCallback", this.openTrtFileCallback);

		this._apiMenu.editTag = this.editTagHandler;
		this._apiMenu.removeTag = this.removeTagHandler;

		this._apiMenu.getMenuActionsEnabled = this.getMenuActionsEnabled;
		this._apiMenu.startSelected = this.startSelected;
		this._apiMenu.pauseSelected = this.pauseSelected;
		this._apiMenu.removeSelected = this.removeSelected;
		this._apiMenu.restartSelected = this.restartSelected;
		this._apiMenu.moveSelected = this.moveSelected;
		this._apiMenu.addDownloadFromURL = this.addDownloadFromURL;
		this._apiMenu.chooseFiles = this.chooseFilesHandler;
        this._apiMenu.showInfo = this.showInfoHandler;
		this._apiMenu.checkForUpdates = this.checkForUpdatesHandler;
		this._apiMenu.pauseExceptSelected = this.pauseExceptSelected;
		this._apiMenu.onHide = this.onHidePopupMenuHandler;

		var showMenuGreenMarker = false;
		var sharerState = app.appViewManager.getSharerState()
		if (sharerState && !sharerState.menuMarkerShown) {
			showMenuGreenMarker = true;
		}

		this.model.set({
			showMenuGreenMarker: showMenuGreenMarker
		});
	}

	Class.prototype = {
		pauseExceptSelected: function(download_id){

			app.controllers.downloads.pauseExceptSelected(download_id);
		},
		setDownloadsFocus: function() {
            $( "#header" ).focus();
		},
		onCloseCallbackDownloadWizard: function() {
            $( "#header" ).focus();
		},
		addDownload: function () {
			app.controllers.downloadWizard.newDownload(undefined);
		},
		showSettings: function () {
			//app.controllers.settings.show(this.setDownloadsFocus);
			app.controllers.settings.show();
		},
        openTrtFileHandler: function (source) {
            if (source != null && source != "") {
                app.controllers.downloadWizard.addDownloadFromTrtFile(source);
            }
        },
        openTrtFile: function () {

			if (app.controllers.settings.model.get('opened'))
				app.controllers.settings.close();
			this.closeMenu();
            this._apiSystem.openTorrentFileDialog(['menu']);
		},
        importUrlsFromClipboard: function () {

			if (app.controllers.settings.model.get('opened'))
				app.controllers.settings.close();
			this.closeMenu();
            this._apiMenu.importUrlsFromClipboard();
		},
        openTrtFileCallback: function (filePath, flag) {
            if (flag == 'menu'){
                _.defer(_.bind(this.openTrtFileHandler, this, filePath));
            }
        },
		openWebSite: function(){
			this.closeMenu();
			this._apiMenu.launchVisitForum();
		},
		contactSupport: function(){
			this.closeMenu();
			this._apiMenu.launchContact();
		},
		voteForFeatures: function(){
			this.closeMenu();
			this._apiMenu.voteForFeatures();
		},
		openAbout: function(){
			this.closeMenu();
			this._apiMenu.launchAbout();
		},
        shareWithFriends: function(){
			this.closeMenu();
            app.controllers.sharer.showSharerFromMenu();
		},
        openMosaicDialog: function(){
			this.mosaicStatMosaicClick();
			this.closeMenu();
			fdmApp.system.browseUrl('http://up.freedownloadmanager.org/js_stat.php?navigate_to_mosaic=1');
		},
        // vicoinsBundle: function(){
		// 	this.closeMenu();
        //     app.controllers.bundles.showFromMenu();
		// },
		checkForUpdates: function(){
			if (app.controllers.settings.model.get('opened'))
				app.controllers.settings.close();
			this.closeMenu();
			app.controllers.updater.checkForUpdates();
		},
		checkForUpdatesHandler: function(){
			this.checkForUpdates();
		},
		exitApp: function(){
			this.closeMenu();
			this._apiMenu.launchExit();
		},
		editTagHandler: function(tag_id){

			FdmDispatcher.handleCoreAction({
				actionType: 'editTag',
				content: {
					tagId: tag_id
				}
			});

		},
		chooseFilesHandler: function(download_id){

			app.controllers.downloads.setCurrentItemById(download_id);
			app.controllers.bottomPanel.changeTab(fdm.views.BottomPanelTab.Files);

			if (!app.controllers.bottomPanel.model.get('panelVisible')){
				app.controllers.bottomPanel.show();
			}

		},
        showInfoHandler: function(download_id){

			app.controllers.downloads.setCurrentItemById(download_id);

			if (!app.controllers.bottomPanel.model.get('panelVisible')){
				app.controllers.bottomPanel.show();
			}

		},
		removeTagHandler: function(tag_id){
			fdmApp.tagManager.deleteTag(parseInt(tag_id));
		},
		getMenuActionsEnabled: function(){

			var show_settings = app.controllers.settings.model.get('opened');
			var wizard_opened = app.controllers.downloadWizard.isVisible();

			var menu_actions = {};
			if (!show_settings && !wizard_opened)
				menu_actions = app.controllers.downloads.getToolbarActions();
			else
				menu_actions.selectedIds = [];

			menu_actions.CanShowSettings = !show_settings && !wizard_opened;
			menu_actions.CanOpenTrtFile = !show_settings && !wizard_opened;
			menu_actions.CanAddDownloadFromUrl = !show_settings && !wizard_opened;
			menu_actions.CanCheckForUpdates = !wizard_opened;

            var bitmask = 0;
			for (var k in menu_actions){
				var v = menu_actions[k];
                if (v && typeof fdm.models.MenuActionsBitmask[k] != 'undefined')
                    bitmask = bitmask | fdm.models.MenuActionsBitmask[k];
			}

			return {
				selectedIds: menu_actions.selectedIds,
                actionsEnabledBitmask: bitmask
			};
		},
		startSelected: function(){

			app.controllers.downloads.view_model.startSelected()

		},
		pauseSelected: function(){

			app.controllers.downloads.view_model.stopSelected()

		},
		removeSelected: function(){

			app.controllers.downloads.view_model.removeSelected();

		},
		restartSelected: function(){

			app.controllers.downloads.view_model.restartSelected();

		},
		moveSelected: function(){

			app.controllers.downloads.view_model.moveSelected()

		},
		addDownloadFromURL: function(){

			app.controllers.downloads.fakeAdd();
		},

		openSettings: function(){
			this.closeMenu();
			this.showSettings();
		},

		closeMenu: function(){
			this.mosaicStatMenuClose();
			this.model.set({opened: false});
			app.controllers.downloads.view_model.onFocus();
		},

		openMenu: function(){
			this.mosaicStatMenuOpen();

			this.model.set({opened: true});
			app.controllers.downloads.view_model.onFocusLost();

			this.hideMenuGreenMarker();
		},

		hideMenuGreenMarker: function() {
			// app.appViewManager.setSharerState('menuMarkerShown', false);
			if (this.model.get('showMenuGreenMarker')) {
				this.model.set({
					showMenuGreenMarker: false
				})
				app.appViewManager.setSharerState('menuMarkerShown', true);
			}
		},

		toggleMenu: function(){
			if (this.model.get('opened')){
				this.closeMenu();
			}
			else {
				this.openMenu();
			}
		},

		onHidePopupMenuHandler: function(){

			app.controllers.downloads.view_model.closePopupMenu();
		},

		mosaicStat: {},
		mosaicStatMenuOpen: function () {
			this.mosaicStat = {
				mosaicClick: false
			};
			if (this.model.get('showMenuGreenMarker')) {
				this.mosaicStat.greenMenuMarker = true;
			} else {
				this.mosaicStat.greenMenuMarker = false;
			}
		},
		mosaicStatMosaicClick: function () {
			this.mosaicStat.mosaicClick = true;
		},
		mosaicStatMenuClose: function () {
			fdmApp.system.getUuid(function (uuid) {
				uuid = fdmApp.isFake ? "3828e79d-69e6-43c6-a4e3-ce08cc872fe7" : uuid.uuid;
				fdm.statUtils.mosaicStat(uuid, this.mosaicStat.greenMenuMarker, this.mosaicStat.mosaicClick);
				this.mosaicStat = {};
			}.bind(this));

		},
	};

	return Class;
})();

//fdm.viewModels.Menu = (function () {
//	function Class(controller, model, popupMgr) {
//		this._controller = controller;
//
//		_.bindAll(this, 'toggleMenu', 'closeMenu');
//
//	};
//
//	Class.prototype = {
//		toggleMenu: function(){
//			this._controller.toggleMenu();
//		},
//		closeMenu: function(){
//
//			this._controller.closeMenu();
//		},
//		openMenu: function(){
//
//			this._controller.openMenu();
//		},
//		addDownload: function () {
//			this.closeMenu();
//			this._controller.addDownload();
//		}
//	};
//
//	return Class;
//})();

