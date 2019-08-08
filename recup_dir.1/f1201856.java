jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.models.DownloadState = {
	Completed: 0,
	Paused: 1,
	Downloading: 2,
    Error: 3,
    WaitingForMetadata: 4,
    Checking: 5,
    Pausing : 6,
    Reconnecting: 7,
	FileProcessing: 8
	//Allocating: 9
};
fdm.models.CheckingState = {
    Unknown: -1,
    // checking files
    Files: 0,
    // checking resume data
    ResumeData: 1,
    // allocating disk space
    Allocating: 2,
    // queued for checking
    Queued: 3,
};
fdm.models.DownloadStateFilters = {
	InProgress: 0,
	Completed: 1,
	Active: 2,
	Inactive: 3,
	Error: 4
};
fdm.models.DownloadStateTitles = Object.keys(fdm.models.DownloadState);


fdm.models.deleteDialogChoice = {
	notSave: 0,
	fromDisk: 1,
	fromList: 2
};

fdm.models.downloadPriority = {
    Unknown: 0,
    Low: 1,
    Normal: 2,
    High: 3
};

fdm.models.pauseReason = {
    ByUser: 1,
	LowDiskSpace: 2,
    Redirection: 3
};

fdm.controllers.DownloadList = (function () {
	var _apiSystem;
	var _ctrlDownloadWizard;
	var _apiDownloads;

	function Class(apiSource, apiSystem, downloadWizard, initCallback) {

		fdm.models.ToolbarActions = Backbone.Model.extend({
			defaults: {
				forSelected: false,
				forAll: true,
				SelectionCanBeDownloaded: false,
				SelectionCanBePaused: false,
				SelectionCanBeRemoved: false,
				SelectionCanBeMovable: false,
				SelectionCanBeRestarted: false
			}
		});

		fdm.models.DownloadList = Backbone.Model.extend({
			defaults: {
				toolbarActions: new fdm.models.ToolbarActions,
				downloadListBuild: false,
				currentItem: null,
				selection: new Backbone.Collection([]),
				downloads: new Backbone.Collection([]),
				selectedList: new Backbone.Collection([]),
				allSelected: false,
				countSelected: 0,
				selectedSize: false,
				sortOptions: {
					id: 1,
					sortProp: 'createdDate',
					descending: false
				},
				defaultOrderOptions: {
					createdDate: { id: 1, sortProp: "createdDate", descending: false },
					fileName: { id: 2, sortProp: "fileName", descending: true },
					downloadSpeedBytes: { id: 3, sortProp: "downloadSpeedBytes", descending: false },
					totalBytes: { id: 3, sortProp: "totalBytes", descending: true },
					progress: { id: 4, sortProp: "progress", descending: false }
				},
				activeFilterText: "",
				thumbPriority: 1,
				showDeletePopupDialog: false,
				downloadsExistsOnDisk: [],
				deleteDialogChoice: fdm.models.deleteDialogChoice.notSave,
				deleteDialogCheckbox: 0,
				statusFilter: null,
				lowDiskSpaceWarnings: false,
                hasDownloadingItems: false,
				showScheduleTip: false,
                changeUrlDialogShown: false,
                changeUrlDownloadProperties: {},
                changePathDialogShown: false,
                changePathDialogDownload: false,
			}
		});
		fdm.models.DownloadItem = Backbone.Model.extend({
			defaults: {
				id: -1,
				current: false,
				state: 0,
				//checking,
				status: 'Paused',
				downloadType: 0,
				errorText: "",
				fileName: "",
				totalBytes: 0,
				downloadedBytes: 0,
				//remainingTime: 0,
				url: "",
				domain: "",
				createdDate: 0,
				completionDate: 0,
				progress: -1,
				checkingProgress: 0,
				sections: 0,
				downloadSpeedBytes: 0,
				downloadSpeedLimit: 0,
				thumb: "",
				targetFolder: "",
				outputFilePath: "",
				rootPath: "",
				comment: "",
				tags: [],//new Backbone.Collection(rawItem.tags),
				files: [],
				filesCount: 0,
				filesChosenCount: 0,
				logs: new Backbone.Collection([]),
				isMovable: true,
				isMoving: false,
                moveProgress: 0,
				isScheduled: false,
				isPreallocating: false,
				isQueued: false,
				priorityChanged: -1,
				thumbCache: 0,
				bottomThumbPriority: 1,
				bottomThumbShowed: false,
				seedingEnabled: false,
				uploadedBytes: 0,
				seedsCount: 0,
				seedsConnectedStat: 0,
				seedsAllStat: 0,
				uploadSpeedBytes: 0,
				uploadSpeedLimitBytes: 0,
				shareRatio: 0,
				peersCount: 0,
				peersConnectedStat: 0,
				peersAllStat: 0,
				availability: 0,
				dropIsActive: false,
				batchId: false,
				priority: 0,
                missingFiles: false,
                missingStorage: false,
                pauseReason: null,
			},
			coreLastState: null,
			changeStateTimeout: null,

			setCoreLastState: function(){
				if (this.coreLastState !== null && this.coreLastState != this.get('state')){

					var changes = {};
					changes[this.id] = {
						state: this.coreLastState
					};

					_.defer(_.partial(app.controllers.downloads.onItemChangedHandlerV2, changes));
				}

				this.coreLastState = null;
				this.changeStateTimeout = null;
			},

			lastRemainingTimeValue: -1,
			calcRemainingTimestamp: -1,

			getRemainingTime: function(){

				var remaining_bytes = this.attributes.totalBytes - this.attributes.downloadedBytes;
				var download_speed_bytes = this.attributes.downloadSpeedBytes;

				var current_timestamp = parseInt( + new Date() / 1000 );

				var real_remaining_time = this.calcRemainingSec(remaining_bytes, download_speed_bytes);
				var last_remaining_time = this.lastRemainingTimeValue - (current_timestamp - this.calcRemainingTimestamp);

				var abs = Math.abs(real_remaining_time - last_remaining_time);
				var error_percent = Math.max(abs/real_remaining_time * 100, abs/last_remaining_time * 100);
				var time_diff = current_timestamp - this.calcRemainingTimestamp;

				if (time_diff > 20
					|| last_remaining_time <= 0
					|| error_percent > 100
					|| error_percent > 30 && time_diff > 5
					|| error_percent > 10 && time_diff > 10
					|| real_remaining_time < 10000
				){
					this.calcRemainingTimestamp = current_timestamp;
					this.lastRemainingTimeValue = real_remaining_time;

					return real_remaining_time;
				}
				else{

					return this.lastRemainingTimeValue - time_diff * 1000;
				}
			},

			calcRemainingSec: function(rb, dsb){

				var remaining_bytes = rb;
				var download_speed_bytes = dsb;

				var remaining_sec = -1;
				if(remaining_bytes > 0 && download_speed_bytes > 0) {
					remaining_sec = (remaining_bytes / download_speed_bytes) * 1000;
				}

				return remaining_sec;
			},

			getThumbnailUrl:function(type){
				type = type || 'downloads';
				var size;

				if (fdmApp.isFake)
					return 'thumbnail.png';
				var s;
				if (type == 'bottom-panel'){

					var priority = this.get('bottomThumbPriority');
					//if (!this.get('bottomThumbShowed')){
					//	priority = app.controllers.downloads.model.get('thumbPriority');
					//	priority++;
					//	this.set('bottomThumbShowed', true);
					//	this.set('bottomThumbPriority', priority);
					//	app.controllers.downloads.model.set('thumbPriority', priority);
					//}
					s = Math.ceil(fdmApp.screenDensityFactor * 260);
					size = '&width=' + s + '&height=' + s + '&borderless=1&priority=' + priority;
				}
				else{
					s = Math.ceil(fdmApp.screenDensityFactor * 32);
					size = '&width=' + s + '&height=' + s + '&borderless=0';
                }
				return 'http://preview/download/?id='+this.get('id')+size+'&cache='+( fdmApp.start_time + this.get('thumbCache'));
			},

			isNoSize: function () {

				var state = this.get('state');
				var requesting_info = this.get('downloadType') == fdm.models.DownloadType.InfoRequest;

				if (this.get('isMoving'))
					return false;

				return state == fdm.models.DownloadState.FileProcessing
					//|| state == fdm.models.DownloadState.Allocating
					|| state == fdm.models.DownloadState.Reconnecting
					|| requesting_info
					|| this.get('totalBytes') < 0
					&& (state == fdm.models.DownloadState.Downloading
					|| state == fdm.models.DownloadState.WaitingForMetadata
					|| state == fdm.models.DownloadState.Pausing
					|| state == fdm.models.DownloadState.Paused)
					|| state == fdm.models.DownloadState.Checking && (
                        this.get('checking') == fdm.models.CheckingState.Allocating
                        || this.get('checking') == fdm.models.CheckingState.ResumeData
                        || this.get('checking') == fdm.models.CheckingState.Queued
                    );
			},

			getNoSizeStatus: function () {

				var state = this.get('state');
				var downloadType = this.get('downloadType');
				var isQueued = this.get('isQueued');

				if (downloadType == fdm.models.DownloadType.InfoRequest){

					return __('Requesting info...');
				}
				if (this.get('isMoving')){

					return __('Moving');
				}
				if (state==fdm.models.DownloadState.Downloading && isQueued){

					return __('Queued');
				}
				if (state==fdm.models.DownloadState.Downloading && !isQueued){
					return __('Unknown file size');
				}
				if (state==fdm.models.DownloadState.FileProcessing){
					return __('Merging media streams');
				}
				if (state==fdm.models.DownloadState.Reconnecting){

					return __('Reconnecting...');
				}
				if (state==fdm.models.DownloadState.Paused || state==fdm.models.DownloadState.Pausing){

					return __('Paused');
				}
				if (state==fdm.models.DownloadState.WaitingForMetadata){

					return __('Downloading metadata');
				}

                if (state==fdm.models.DownloadState.Checking){

                    var checking = this.get('checking');

                    if (checking == fdm.models.CheckingState.Allocating)
                        return __('Allocating disk space...');

                    if (checking == fdm.models.CheckingState.ResumeData)
                        return __('Checking resume data...');

                    if (checking == fdm.models.CheckingState.Queued)
                        return __('Queued for checking...');
                }
			},
			getErrorText: function () {

				var text = this.get('errorText');

				if (this.get('missingFiles'))
                    text = __('File is missing');
				if (this.get('missingStorage'))
                    text = __('Disk is missing');

				if (text.indexOf('HTTP Error') === 0){
					var pos = text.indexOf(':');
					if (pos > 0){
						return text.substring(0, pos);
					}
				}

				return text;
			}
		});
		fdm.models.DownloadListCollection = Backbone.Collection.extend({
			model: fdm.models.DownloadItem
		});
		fdm.models.CurrentDownloadListCollection = fdm.models.DownloadListCollection.extend({
			comparator: _.bind(this.sortDownloads, this)
		});
		this.collections = {};
		_apiDownloads = apiSource;
		_apiSystem = apiSystem;
		_ctrlDownloadWizard = downloadWizard;


		_.bindAll(this, 'refreshCreatedDates', 'applyFilePreview',
			'onItemAddedHandler', 'onItemChangedHandlerV2','onItemDeletedHandler',
			'onDownloadListBuilt', 'onItemCompletedHandler', 'noDownloadsExist',
			'allDownloadsFiltered', 'selectDownload', 'resetTagsAndFilters', 'lowDiskSpaceWarning',
            'onFullyRestored', 'onDownloadingStateChanged', 'onSelectAllDownloads', 'onChangeUrlRequested',
			'openFolderCallback', 'onRestartDownloads');

        fdmApp.system.addEventListener("openFolderCallback", this.openFolderCallback);


		this.collections.downloads = new fdm.models.DownloadListCollection;
		this.collections.currentDownloads = new fdm.models.CurrentDownloadListCollection;
		this.collections.selectedList = new fdm.models.DownloadListCollection;
		this.collections.currentSelectedList = new fdm.models.DownloadListCollection;

		this.model = new fdm.models.DownloadList();

		this.model.set(window.app.appViewManager.getDownloadsState());

		this.model.on('change:deleteDialogChoice change:deleteDialogCheckbox', app.appViewManager.hasChanges, app.appViewManager);

		app.controllers.tags.model.on('change:selectedTag', this.changeSelectedTag, this);
		this.collections.downloads.on('add reset remove', this.needUpdateCurrentDownloads, this);
		this.collections.downloads.on('remove', this.needShowTagsPanelTip, this);
		this.collections.downloads.on('remove', this.checkCurrentItem, this);
		this.model.on('change:statusFilter', this.changeStatusFilter, this);
		this.model.on('change:activeFilterText', this.changeActiveFilterText, this);
		this.model.on('change:sortOptions', this.collections.currentDownloads.sort, this.collections.currentDownloads);
		this.model.on('change:currentItem', this.updateToolbarActions, this);

		this.collections.selectedList.on('add remove reset', this.changeSelectedList, this);
		this.collections.currentDownloads.on('add remove reset', this.changeCurrentDownloads, this);
		this.collections.currentSelectedList.on('add remove reset', this.updateToolbarActions, this);

		_apiDownloads.onSectionCompleted = function(){};
		_apiDownloads.onSpeedChanged = function(){};
		_apiDownloads.onItemProgressChanged = function(){};
		_apiDownloads.onItemChanged = function(){};
		_apiDownloads.onItemPriorityChanged = function(){};

		_apiDownloads.onItemAdded = this.onItemAddedHandler;
		_apiDownloads.onItemDeleted = this.onItemDeletedHandler;
		_apiDownloads.onDownloadListBuilt = this.onDownloadListBuilt;
		_apiDownloads.lowDiskSpaceWarning = this.lowDiskSpaceWarning;
		//_apiDownloads.onFileNameChanged = this.onFileNameChangedHandler;
		//_apiDownloads.onLogMessages = this.onLogMessagesHandler;
		_apiDownloads.onItemCompleted = this.onItemCompletedHandler;
		_apiDownloads.onFullyRestored = this.onFullyRestored;
		_apiDownloads.onDownloadingStateChanged = this.onDownloadingStateChanged;
		fdmApp.downloads.addEventListener("onItemThumbnailChanged", this.applyFilePreview);

		_apiDownloads.selectDownload = this.selectDownload;

		_apiDownloads.onItemChanged2 = this.onItemChangedHandlerV2;

		_apiDownloads.onSelectAllDownloads = this.onSelectAllDownloads;

        _apiDownloads.onChangeUrlRequested = this.onChangeUrlRequested;

        _apiDownloads.restartDownloads = this.onRestartDownloads;

		var view_model = new fdm.viewModels.DownloadList(this, this.collections, this.model);
		this.view_model = view_model;

		//binding, downloads template is already in the page
        initCallback();

		this._promiseOld = [];

		var now = new Date();
		var midnight = new Date(now.getFullYear(), now.getMonth(), now.getDate() + 1);
		var delta = Math.abs(midnight.getTime() - now.getTime());
		setTimeout(this.refreshCreatedDates, delta);
	}

	Class.prototype = {

        onChangeUrlRequested: function (download_id) {

            this.getDownloadsPropertiesPromise([download_id], ['fileName', 'totalBytes', 'url', 'outputFilePath'])
                .then(function(result){

                	if (result && result.hasOwnProperty('properties')){

                        this.model.set({
                            changeUrlDialogShown: true,
                            changeUrlDownloadProperties: result.properties
                        });

                    }

				}.bind(this));

        },
        onChangeUrlCanceled: function () {

        	this.model.set({
                changeUrlDialogShown: false,
                changeUrlDownloadId: false
			});

        },
        changeUrl: function (download_id, url, start_download) {

        	this.model.set({
                changeUrlDialogShown: false,
                changeUrlDownloadId: false
			});

            fdmApp.downloads.changeUrl(download_id, url, function(){

                if (start_download)
                    this.startDownload(download_id);

            }.bind(this));
        },


        onRestartDownloads: function (download_id) {

            var download = this.collections.downloads.get(download_id);

            if (download){

            	if (download.get('missingStorage'))
            		this.onChangePathRequested(download);
            	else
                    this.callItemMethodById(download.get("id"), 'restart');
            }
        },

        onChangePathRequested: function (download) {

            if (download){

                this.model.set({
                    changePathDialogShown: true,
                    changePathDialogDownload: download,
                });
            }
        },
        onChangePathCanceled: function () {

            this.model.set({
                changePathDialogShown: false,
                changePathDialogDownload: false
            });

        },
        changePath: function (download_id, path, start_download) {

            this.model.set({
                changePathDialogShown: false,
                changePathDialogDownload: false
            });

            fdmApp.downloads.moveTo(download_id, path, true);
        },
        openFolderCallback: function (targetFolder, flag) {

            if (flag == 'change-path-dialog')
            {
                FdmDispatcher.handleViewAction({
                    actionType: 'ChangePathDialogFolderCallback',
                    content: {
                        targetFolder: targetFolder
                    }
                });
            }
        },

		needShowTagsPanelTip: function(){

			var c = this.model.get('showScheduleTip');
			if (this.collections.downloads.findWhere({isScheduled: true})){

				if (!c)
					this.model.set({showScheduleTip: true});
			}
			else{

				if (c)
					this.model.set({showScheduleTip: false});
			}
		},

		onSelectAllDownloads: function(){

			this.selectAll();
		},

        onDownloadingStateChanged: function (state) {

            if (typeof state == 'string')
                state = JSON.parse(state);

            this.model.set({
                hasDownloadingItems: state.downloading
            })
        },

        onFullyRestored: function (download_id) {

			this.applyFilePreview(download_id);
		},

		lowDiskSpaceWarning: function(warnings){

			if (typeof warnings == 'string')
				warnings = JSON.parse(warnings);

			this.model.set({lowDiskSpaceWarnings: warnings});
		},
		getEventUid: function(item){
			return item.id
		},
		refreshCreatedDates: function(){
			setTimeout(this.refreshCreatedDates, 1000 * 3600 * 24);

			var apendix = (new Date()).getDate() % 2 ? (-1) : 1; // to add or remove 1 for a balance
			var downloads = this.collections.downloads.models;
			for (var i = 0, l = downloads.length; i < l; ++i) {
				var dld = downloads[i];
				var created = dld.get("createdDate");
				dld.set("createdDate", created + apendix);
			}
		},
		calcRemainingSec: function(rawItem){
			var remainingBytes = rawItem.totalBytes - rawItem.downloadedBytes;
			remainingBytes = remainingBytes > 0 ? remainingBytes : 0;
			var remainingSec = undefined;
			if(remainingBytes && rawItem.downloadSpeedBytes) {
				remainingSec = remainingBytes / rawItem.downloadSpeedBytes;
			}
			remainingSec = remainingSec != undefined ? remainingSec * 1000 : -1;
			return remainingSec;
		},
		cachedItemToModelSync: function(cachedItem){
			var rawItem = cachedItem.rawItem;

			var folder = this.getTargetFolder(rawItem.outputFilePath);

			var rawModel = {
				id: cachedItem.id,
				state: rawItem.state,
				status: fdm.models.DownloadStateTitles[rawItem.state],
				downloadType: rawItem.downloadType,
				errorText: rawItem.errorText,
				fileName: rawItem.fileName,
				totalBytes: rawItem.totalBytes,
				downloadedBytes: rawItem.downloadedBytes,
				//remainingTime: this.calcRemainingSec(rawItem),
				url: rawItem.url,
				domain: rawItem.domain,
				createdDate: typeof rawItem.createdDate === "number" ?
					rawItem.createdDate : rawItem.createdDate.getTime(),
				progress: rawItem.progress,
				checkingProgress: rawItem.checkingProgress,
				sections: rawItem.sections,
				downloadSpeedBytes: rawItem.downloadSpeedBytes,
				downloadSpeedLimit: rawItem.downloadSpeedLimit,
				targetFolder: folder,
				outputFilePath: rawItem.outputFilePath,
				rootPath: rawItem.rootPath,
				comment: rawItem.comment,
				tags: JSON.parse(rawItem.tags),//new Backbone.Collection(rawItem.tags),
				//torrent: this.createTorrent(cachedItem.rawTorrent),// || null,
				//files: _.map(rawItem.files, _.clone),
				logs: new Backbone.Collection([]),
				isMovable: rawItem.isMovable,
				isMoving: rawItem.isMoving,
                moveProgress: rawItem.moveProgress,
				isScheduled: rawItem.isScheduled,
				batchId: rawItem.batchId,
				filesCount: rawItem.filesCount,
				filesChosenCount: rawItem.filesChosenCount
			};
			//if(rawItem.state == fdm.models.DownloadState.Error)
			//	rawModel.status += rawItem.errorText;
			var model = this.collections.downloads.get(rawModel.id);
			if(model) {
				// do store logs
				if(model.get("logs"))
					rawModel.logs = model.get("logs");//_.pluck(model.get("logs").models, 'attributes'); // model.get("logs");
			}
			return rawModel;
		},

		onDownloadListBuilt: function () {

            this.addAllDownloads();
		},

		getTargetFolder: function(outputFilePath){

			var path_pos;
			if (fdmApp.platform == 'mac')
				path_pos = outputFilePath.lastIndexOf('/');
			else
				path_pos = outputFilePath.lastIndexOf('\\');

			if (path_pos >0)
				return outputFilePath.substring(0, (path_pos + 1));

			return outputFilePath;
		},

		formatDownloadArgs: function(download_args){

			var result = download_args;

			if(download_args.hasOwnProperty("createdDate")){

				if (typeof download_args.createdDate === "object")
					result.createdDate = download_args.createdDate.getTime();
				else if (typeof download_args.createdDate === "string")
					result.createdDate = new Date(download_args.createdDate).getTime();
			}

			if(download_args.hasOwnProperty("outputFilePath")){

				var outputFilePath = download_args.outputFilePath;

				result.targetFolder = this.getTargetFolder(outputFilePath);
			}

			if(download_args.hasOwnProperty("state")){

				result.status = fdm.models.DownloadStateTitles[download_args.state];
			}

			return result;
		},

		getDownloadModels: function(result){

			if (!_.isArray(result) && _.isObject(result))
				result = [result];

			var downloads = [];
			for (var i =0; i < result.length; i++){

				var download = this.formatDownloadArgs(result[i]['properties']);
				downloads.push(download);
			}

			return downloads;
		},

		setAllDownloads: function(result){

			if (!_.isArray(result) && _.isObject(result))
				result = [result];

			_.defer(_.bind(function(){

				this.model.set({downloadListBuild: true});
			}, this));

			var downloads = [];
			for (var i =0; i < result.length; i++){

				var download = this.formatDownloadArgs(result[i]['properties']);

				if (download.batchId > 0)
					continue;

				//download['remainingTime'] = this.calcRemainingSec(download);
				downloads.push(download);
			}

			this.collections.downloads.set(downloads, {silent: true, remove: false});
			this.collections.downloads.trigger('reset');
			var max_id = Math.max.apply(null, this.collections.downloads.pluck('id'));

			this.view_model.doAfterDownloadAdding(max_id);

		},

		addAllDownloads: function(){

			var properties = ["id", "state", "downloadType", "errorText", "fileName", "totalBytes", "downloadedBytes",
				"url", "domain", "createdDate", "progress", "checkingProgress", "completionDate", "sections",
				"downloadSpeedBytes","downloadSpeedLimit", "outputFilePath", "rootPath", "comment", "tags", "isMovable",
				"isMoving", "moveProgress", "isQueued", "isScheduled", "batchId", "priority",
				"missingFiles", "missingStorage", "pauseReason",
				//for torrent
				"seedingEnabled", "uploadedBytes", "seedsCount", "seedsConnectedStat", "seedsAllStat",
				"uploadSpeedBytes", "uploadSpeedLimitBytes", "shareRatio", "peersCount", "peersConnectedStat",
				"peersAllStat", "availability", "comment", "filesCount", "filesChosenCount", "checking"];

			this.getDownloadsPropertiesPromise([], properties)
				.then(_.bind(this.setAllDownloads, this));
		},

		getAllBatchDownloads: function(batch_id){

			var properties = ["id", "state", "downloadType", "errorText", "fileName", "totalBytes", "downloadedBytes",
				"url", "domain", "createdDate", "progress", "checkingProgress", "sections",
				"downloadSpeedBytes","downloadSpeedLimit", "outputFilePath", "rootPath", "comment", "tags", "isMovable",
				"isMoving", "moveProgress", "isQueued", "isScheduled", "batchId", "priority",
                "missingFiles", "missingStorage", "pauseReason",
				//for torrent
				"seedingEnabled", "uploadedBytes", "seedsCount", "seedsConnectedStat", "seedsAllStat",
				"uploadSpeedBytes", "uploadSpeedLimitBytes", "shareRatio", "peersCount", "peersConnectedStat",
				"peersAllStat", "availability", "comment", "filesCount", "filesChosenCount", "checking"];

			return this.getBatchDownloadIds(batch_id)
				.then(_.bind(function(download_ids){

					return this.getDownloadsPropertiesPromise(download_ids, properties);
				}, this))
				.then(_.bind(this.getDownloadModels, this));
		},

		getBatchDownloadIds: function(batch_id, properties){

			return new Promise(function(resolve, reject) {

				fdmApp.batchDownloads.getDownloadsIds(batch_id, function(result){

					if (typeof result == 'string')
						result = JSON.parse(result);

					if (!result.length)
						reject(result);
					else
						resolve(result);
				});
			});
		},

		applyFilePreview: function(itemId) {

			var model = this.collections.downloads.get(itemId);
			if (model) {
                model.set("thumbCache", model.get('thumbCache') + 1);
			} else {
                app.controllers.bottomPanel.batchItemApplyFilePreview(itemId);
			}
		},

		startDownload: function (download_id) {

            this.getDownloadsPropertiesPromise([download_id], ['state', 'downloadType'])
                .then(function(result){

                    var toggleFn;

                    if (result && result.hasOwnProperty('properties')){

                        switch (result.properties.state) {
                            case fdm.models.DownloadState.Completed:
                                toggleFn = "hardRestart";
                                break;
                            case fdm.models.DownloadState.Downloading:
                            case fdm.models.DownloadState.WaitingForMetadata:
                            case fdm.models.DownloadState.Checking:
                            case fdm.models.DownloadState.Reconnecting:
                                toggleFn = "play";
                                break;
                            case fdm.models.DownloadState.Error:
                                if (result.properties.downloadType === fdm.models.DownloadType.InfoRequest)
                                    toggleFn = "play";
                                else
                                    toggleFn = "restart";
                                break;
                            case fdm.models.DownloadState.Paused:
                            case fdm.models.DownloadState.Pausing:
                                toggleFn = "play";
                                break;
                            default:
                                toggleFn = "play";
                                return;
                        }

                        if (toggleFn)
                        	this.callItemMethodById(download_id, toggleFn);

                    }

                }.bind(this));
        },

		toggleDownload: function(model) {

			if (model.get('isScheduled')){

				fdmApp.downloads.showScheduleDownloads({downloadIds: [model.get('id')]});
				return;
			}

			var toggleFn;
            var new_state = false;
			switch (model.get("state")) {
                case fdm.models.DownloadState.Completed:
					toggleFn = "openFolder";
					break;
				case fdm.models.DownloadState.Downloading:
				//case fdm.models.DownloadState.Allocating:
				case fdm.models.DownloadState.WaitingForMetadata:
				case fdm.models.DownloadState.Checking:
				case fdm.models.DownloadState.Reconnecting:
                    new_state = fdm.models.DownloadState.Pausing;
					toggleFn = "pause";
					break;
				case fdm.models.DownloadState.Error:
					if (model.get("downloadType") == fdm.models.DownloadType.InfoRequest)
						toggleFn = "play";
					else
						toggleFn = "restart";
					break;
				case fdm.models.DownloadState.Paused:
				case fdm.models.DownloadState.Pausing:
                    new_state = fdm.models.DownloadState.Downloading;
					toggleFn = "play";
					break;
				default:
					// no support another case
					console.warn("toggleDownload is called for unsupported case (%o)", model.get("state"));
					return;
			}

            if (model.get('missingFiles') && !model.get('missingStorage'))
                toggleFn = "restart";
			else if (model.get('missingStorage') && model.get("state") === fdm.models.DownloadState.Completed)
			{
                this.onChangePathRequested(model);
                return;
			}

            if (new_state !== false){

				var changes = {};
				changes[model.id] = {
					state: new_state
				};

				if (model.changeStateTimeout)
					clearTimeout(model.changeStateTimeout);
				model.changeStateTimeout = null;
				app.controllers.downloads.onItemChangedHandlerV2(changes);
				model.changeStateTimeout = setTimeout(_.bind(model.setCoreLastState, model), 1000);
            }

			this.callItemMethodById(model.get("id"), toggleFn);
		},
		onItemAddedHandler: function (download_id, type, data) {

			var download_data;
			if (typeof data == 'string')
				download_data = JSON.parse(data);
			else
				download_data = data;

			if (download_data.hasOwnProperty('batchId') && download_data.batchId > 0)
				return;

			if (type === fdm.models.DownloadType.batchDownload) {
			    if (download_data.state === fdm.models.DownloadState.Paused) {
                    download_data.state = fdm.models.DownloadState.WaitingForMetadata;
                    download_data.status = fdm.models.DownloadStateTitles[fdm.models.DownloadState.WaitingForMetadata];
                }
            }

			download_data.id = download_id;
			download_data.downloadType = type;
			download_data = this.formatDownloadArgs(download_data);

			var download = this.collections.downloads.get(download_id);
			if (download){
				download.set(download_data)
			}
			else{
				this.collections.downloads.add(download_data);
				this.view_model.doAfterDownloadAdding(download_id);

                // app.controllers.sharer.onDownloadAdded(download_id);
			}
		},

        idsToDelete: {},
        currentIndexBeforeDeleted: false,

		onItemDeletedHandler: function(id) {

            var ci = this.model.get("currentItem");

			var download_in_list = this.collections.downloads.get(id);

            if (ci && ci.get('downloadType') == fdm.models.DownloadType.batchDownload){
                app.controllers.bottomPanel.onItemDeleted();
            }

            delete this.idsToDelete[id];

            var delete_items_length = Object.keys(this.idsToDelete).length;

            var current_index = this.currentIndexBeforeDeleted;
            if (delete_items_length == 0){
                if (current_index === false){
                    var currentItem = this.model.get("currentItem");
                    if (currentItem)
                        current_index = this.getDownloadIndex(currentItem.get('id'));
                }
            }

            // clear selection to avoid dead references
            //for(var i = 0, l = this._rawItemCache.length; i < l; i++) {
            //    if(this._rawItemCache[i].id == id){
            //        this._rawItemCache.splice(i, 1);
            //        break;
            //    }
            //}
            this.collections.downloads.remove(id);

            _.defer(
                function(){
                    this.collections.downloads.trigger('remove')
                }.bind(this)
            );

            if (delete_items_length == 0){

				if (download_in_list){

					this.clearSelection();
					if (current_index !== false){
						var downloads = this.getDownloads();
						if (current_index == downloads.length - 1)
						    current_index--;
						else
                            current_index++;
						if (downloads.length){

							if (downloads.length <= current_index) {
								this.setCurrentItem(downloads[downloads.length - 1]);
							}
							else{
								this.setCurrentItem(downloads[current_index]);
							}
						}
						else
							this.setCurrentItem(this.collections.downloads.first());
					}
					else{
						this.setCurrentItem(this.collections.downloads.first());
					}
				}

                this.currentIndexBeforeDeleted = false;
            }
        },

		updateToolbarActionsTimeout: false,
		needUpdateToolbarActions: function(){

			if (this.updateToolbarActionsTimeout)
				return;

			this.updateToolbarActionsTimeout = setTimeout(function(){

				//_.defer(_.bind(function(){

					this.updateToolbarActionsTimeout = false;
					this.updateToolbarActions();
				//}, this));

			}.bind(this), 50);
		},

		sortCurrentDownloadsTimeout: false,
		needSortCurrentDownloads: function(){

			if (this.sortCurrentDownloadsTimeout)
				return;

			this.sortCurrentDownloadsTimeout = setTimeout(function(){

				//_.defer(_.bind(function(){

					this.sortCurrentDownloadsTimeout = false;
					this.collections.currentDownloads.sort();
				//}, this));

			}.bind(this), 50);
		},

		updateCurrentDownloadsTimeout: false,
		needUpdateCurrentDownloads: function(){

			if (this.updateCurrentDownloadsTimeout)
				return;

			this.updateCurrentDownloadsTimeout = setTimeout(function(){

				//_.defer(_.bind(function(){

					this.updateCurrentDownloadsTimeout = false;
					this.updateCurrentDownloads();
				//}, this));

			}.bind(this), 50);
		},

		onItemChangedHandlerV2: function(items_changes){

			if (typeof items_changes == "string")
				items_changes = JSON.parse(items_changes);

			var statusFilter = this.model.get('statusFilter');

			var need_update_toolbar_actions = false;
			var need_update_current_downloads = false;
			var need_update_filter_counters = false;
			var need_sort_current_downloads = false;
			var tags_changed = false;
			var may_be_show_tags_tip = false;

			var selectedTag = false;
			if (window.app.controllers.tags && window.app.controllers.tags.model)
				selectedTag = window.app.controllers.tags.model.get('selectedTag');

			var sort_prop = false;
			var sortOptions = this.model.get('sortOptions');
			if (sortOptions)
				sort_prop = sortOptions.sortProp;

			_.each(items_changes, function(download_data, download_id){

				if (download_data.hasOwnProperty('batchId') && download_data.batchId > 0){

					var ci = this.model.get("currentItem");
					if (ci && ci.get('id') == download_data.batchId)
						app.controllers.bottomPanel.batchItemChanged(download_id, download_data);

					return;
				}

				var download = this.collections.downloads.get(download_id);

				if (!download)
					return;

				var current_state = download.get('state');

				download_data = this.formatDownloadArgs(download_data);

				if (download_data.hasOwnProperty('state') && download_data.state != current_state){

					need_update_toolbar_actions = true;
					if (statusFilter)
						need_update_current_downloads = true;

					if (current_state == fdm.models.DownloadState.Completed
						|| download_data.state == fdm.models.DownloadState.Completed)
						need_update_filter_counters = true;
					if (current_state == fdm.models.DownloadState.Downloading
						|| download_data.state == fdm.models.DownloadState.Downloading){

							need_update_filter_counters = true;
							if (statusFilter == fdm.models.DownloadStateFilters.Active)
								need_update_current_downloads = true;
					}

					if (current_state == fdm.models.DownloadState.WaitingForMetadata )
						this.applyFilePreview(download.id);

					// if (download_data.state == fdm.models.DownloadState.Completed)
					// 	app.controllers.sharer.onDownloadCompleted();
				}


				if (download_data.hasOwnProperty('downloadType') && download_data.downloadType != download.get('downloadType')){

					this.applyFilePreview(download.id);

					if (download_data.downloadType == fdm.models.DownloadType.Trt
						|| download_data.downloadType == fdm.models.DownloadType.YouTubeVideo){

						need_update_filter_counters = true;
						tags_changed = true;
					}
				}

				if (download_data.hasOwnProperty('isMoving') && !download_data.isMoving && download.get('isMoving'))
                {
                    this.removeFromSelection(download);
                }

				if (download_data.hasOwnProperty('isMoving') && download_data.isMoving != download.get('isMoving')
					|| download_data.hasOwnProperty('isMovable') && download_data.isMoving != download.get('isMovable')
				){
					need_update_toolbar_actions = true;
					if (statusFilter)
						need_update_current_downloads = true;
				}

				if (download_data.hasOwnProperty('totalBytes') && download_data.totalBytes != download.get('totalBytes')
					|| download_data.hasOwnProperty('downloadedBytes') && download_data.downloadedBytes != download.get('downloadedBytes')
					|| download_data.hasOwnProperty('downloadSpeedBytes') && download_data.downloadSpeedBytes != download.get('downloadSpeedBytes')
				){
					var remaining_args = {
						totalBytes: download_data.hasOwnProperty('totalBytes') ? download_data.totalBytes : download.get('totalBytes'),
						downloadedBytes: download_data.hasOwnProperty('downloadedBytes') ? download_data.downloadedBytes : download.get('downloadedBytes'),
						downloadSpeedBytes: download_data.hasOwnProperty('downloadSpeedBytes') ? download_data.downloadSpeedBytes : download.get('downloadSpeedBytes')
					};

					//download_data['remainingTime'] = this.calcRemainingSec(remaining_args);
				}

				//if (download_data.hasOwnProperty('downloadSpeedBytes')
				//	&& download_data.downloadSpeedBytes != download.get('downloadSpeedBytes')
				//	&& (download_data.downloadSpeedBytes == 0 || download.get('downloadSpeedBytes') == 0)){
                //
				//	need_update_filter_counters = true;
				//	if (statusFilter == fdm.models.DownloadStateFilters.Active)
				//		need_update_current_downloads = true;
				//}
				if (download_data.hasOwnProperty('uploadSpeedBytes')
					&& download_data.uploadSpeedBytes != download.get('uploadSpeedBytes')
					&& (download_data.uploadSpeedBytes == 0 || download.get('uploadSpeedBytes') == 0)){

					need_update_filter_counters = true;
					if (statusFilter == fdm.models.DownloadStateFilters.Active)
						need_update_current_downloads = true;
				}

				if (download_data.hasOwnProperty('tags')){

					tags_changed = true;
				}

				if (sort_prop == 'downloadSpeedBytes' && download_data.hasOwnProperty('downloadSpeedBytes')){

					need_sort_current_downloads = true;
				}

				if (sort_prop == 'totalBytes' && download_data.hasOwnProperty('totalBytes')
					&& download_data.totalBytes != download.get('totalBytes')){

					need_sort_current_downloads = true;
				}

				if (sort_prop == 'progress' &&
					( download_data.hasOwnProperty('state') && download_data.state != current_state
					|| download_data.hasOwnProperty('progress') && download_data.progress != download.get('progress')
					|| download_data.hasOwnProperty('isQueued') && download_data.isQueued != download.get('isQueued')
					|| download_data.hasOwnProperty('isMoving') && download_data.isMoving != download.get('isMoving')
					|| download_data.hasOwnProperty('seedingEnabled') && download_data.seedingEnabled != download.get('seedingEnabled')
					)){

					need_sort_current_downloads = true;
				}

				if ( download.changeStateTimeout && download_data.hasOwnProperty('state')){

					download.coreLastState = download_data.state;
					delete download_data.status;
					delete download_data.state;
				}

				if (download_data.hasOwnProperty('isScheduled'))
					may_be_show_tags_tip = true;

				download.set(download_data);

			}, this);

			if (tags_changed){
				_.defer(function(){
					FdmDispatcher.handleViewAction({
						actionType: 'onDownloadTagsChanged',
						content: {}
					})});

				if (selectedTag)
					need_update_current_downloads = true;
			}

			if (need_update_filter_counters){
				_.defer(function(){
					FdmDispatcher.handleViewAction({
						actionType: 'needUpdateFilterCounters',
						content: {}
					})});
			}

			if (may_be_show_tags_tip){
				_.defer(this.needShowTagsPanelTip.bind(this));
			}

			if (need_update_current_downloads){

				this.needUpdateCurrentDownloads();
			}
			else if (need_update_toolbar_actions){

				this.needUpdateToolbarActions();
				if (need_sort_current_downloads)
					this.needSortCurrentDownloads();
			}
		},

		onItemCompletedHandler: function(itemId) {

			var model = this.collections.downloads.get(itemId);
			if (!model) {
				return;
			}
			var completed_time = +new Date();
            model.set('completedTime', completed_time);
            setTimeout(
                function(){
					model.set('completedTime', completed_time - 1);
                },
                2000
            );
			model.set('thumbCache', model.get('thumbCache') + 1);
		},

		getDownloadPropertiesPromise: function(download_id, properties){

			return new Promise(function(resolve, reject) {

				if (properties.indexOf('id') < 0){
					properties.push('id');
				}

				fdmApp.downloads.getDownloadProperties(download_id, properties, function(result){

					resolve(result);
				});
			});
		},

		getDownloadsPropertiesPromise: function(downloads_id, properties){

			return new Promise(function(resolve, reject) {

				if (properties.indexOf('id') < 0){
					properties.push('id');
				}

				fdmApp.downloads.getDownloadsProperties(downloads_id, properties, function(result){

					resolve(result);
				});
			});
		},

		callItemMethodById: function(itemId, methodName, args){

			args = args || [];

			var fn = fdmApp.downloads[methodName];
			if(!_.isFunction(fn)){
				console.warn("callItemMethodById: method '%o' is not available", methodName);
				return null;
			}

			var fn_args = ([]).concat(itemId, args);
			fn(fn_args);
		},
		filterByText: function(text) {
			this.view_model.filterByText(text);
		},
		addFromClipboard: function() {
			var source = _apiSystem.getClipboardText();
			if (typeof source == 'string')
				source = source.trim();
			if ( source == undefined || source.length == 0 ){
				return;
			}
			_ctrlDownloadWizard.newDownload(source);
		},
		launchItemById: function(id){
			this.callItemMethodById(id, "launch");
		},
		addTagItemById: function(id, tagId){
			this.callItemMethodById(id, "addTag", [tagId]);
		},
		deleteTagItemById: function(id, tagId){
			this.callItemMethodById(id, "removeTag", [tagId]);
		},

		showDeletePopupDialog: function(exists){

			this.model.set({
                downloadsExistsOnDisk: exists,
				showDeletePopupDialog: true
			});
		},
		hideDeletePopupDialog: function(){
			this.model.set('showDeletePopupDialog', false);
		},

		deleteAction: function(){

			var downloads = this.getDownloadsToMoveAndRemove();
			if (!downloads || !downloads.length)
				return;

			if (this.model.get('deleteDialogChoice') == fdm.models.deleteDialogChoice.fromList){
				this.deleteSelected(false);
			}
			else{

				var ids = _.pluck(downloads, 'id');

				_apiDownloads.downloadsExistsOnDisk(ids, ['id', 'existsOnDisk'], function(existsOnDisk){

					if (!_.isArray(existsOnDisk))
						existsOnDisk = [existsOnDisk];

					var downloads_exists = [];

					_.each(existsOnDisk, function(download_data){

						if (download_data.existsOnDisk)
							downloads_exists.push(download_data.id);
					});

					if (downloads_exists.length > 0)
						this.showDeletePopupDialog(downloads_exists);
					else
						this.deleteSelected(false);

				}.bind(this));
			}
		},

		deleteSelected: function (isDeleteFiles) {

			var downloads = this.getDownloadsToMoveAndRemove();
			if (!downloads || !downloads.length)
				return;

			var ids = _.pluck(downloads, 'id');

            var current_index = false;
            var currentItem = this.model.get("currentItem");
            if (currentItem)
                current_index = this.getDownloadIndex(currentItem.get('id'));
            this.currentIndexBeforeDeleted = current_index;
            for (var i in ids){
                this.idsToDelete[ids[i]] = 1;
            }
			_apiDownloads.deleteByIds(ids, isDeleteFiles);

			//TODO: add confirmation
			//TODO: add handler
		},

		UISetState: function(downloads, state){

			if (!downloads.length)
				return;

			downloads.map(function(model){

				model.set({
					status: fdm.models.DownloadStateTitles[state],
					state: state
				});
				if (model.changeStateTimeout){

					clearTimeout(model.changeStateTimeout);
				}
				model.changeStateTimeout = setTimeout(_.bind(model.setCoreLastState, model), 1000);
			});

			_.defer(function(){
				FdmDispatcher.handleViewAction({
					actionType: 'needUpdateFilterCounters',
					content: {}
				})});

			var statusFilter = this.model.get('statusFilter');

			if (statusFilter){
				this.needUpdateCurrentDownloads();
			}
			else{
				this.needUpdateToolbarActions();

				var sort_prop = false;
				var sortOptions = this.model.get('sortOptions');
				if (sortOptions)
					sort_prop = sortOptions.sortProp;

				if (sort_prop == 'progress')
					this.needSortCurrentDownloads();
			}
		},

		stopSelected: function(){
            var downloads = this.getSelectedDownloads();

            this.pauseList(downloads);
		},
		stopAll: function(){
            var downloads = this.collections.downloads.models;

            this.pauseList(downloads);
		},
		pauseExceptSelected: function(download_id){

			var current_download = this.collections.downloads.get(download_id);

			var ids = [];

			if (!current_download){

				ids.push(download_id);
			}
			else{

				var downloads = this.getSelectedDownloads();
				if (downloads.length < 1)
					downloads = [ current_download ];

				downloads.map(function(model){
					var state = model.get('state');
					if (this.stateCanBePaused(state)){
						ids.push(model.id);
					}
				}.bind(this));
			}

			_apiDownloads.pauseExceptIds(ids);
		},

        pauseList: function (list) {

            var ids = [];// _.pluck(downloads, 'id');
            var change_state = [];

            list.map(function(model){
                var state = model.get('state');
                if (this.stateCanBePaused(state)){
                    ids.push(model.id);
                    change_state.push(model);
                }
            }.bind(this));
            _apiDownloads.stopByIds(ids);
            this.UISetState(change_state, fdm.models.DownloadState.Paused);
        },

		startList: function (list) {

            var ids = [];// _.pluck(downloads, 'id');
            var change_state = [];

            list.map(function(model){
                var state = model.get('state');
                if (this.stateCanBeStarted(state)){
                    ids.push(model.id);
                    change_state.push(model);
                }
            }.bind(this));
            _apiDownloads.startByIds(ids);
            this.UISetState(change_state, fdm.models.DownloadState.Downloading);
        },

		startSelected: function(){
            var downloads = this.getSelectedDownloads();

            this.startList(downloads);
		},
		startAll: function(){
			var downloads = this.collections.downloads.models;

            this.startList(downloads);
		},
		restartSelected: function(){
            var downloads = this.getSelectedDownloads();
			var change_state = [];

            downloads.map(function(model){
                var state = model.get('state');
                if (state == fdm.models.DownloadState.Error || model.get('missingFiles') && !model.get('missingStorage')){

					change_state.push(model);
					this.callItemMethodById(model.get("id"), 'restart');
                }
            }.bind(this));
			this.UISetState(change_state, fdm.models.DownloadState.Downloading);
		},

		stateCanBePaused: function(state){

			return state == fdm.models.DownloadState.Downloading
			|| state == fdm.models.DownloadState.Checking
			|| state == fdm.models.DownloadState.WaitingForMetadata
			|| state == fdm.models.DownloadState.Reconnecting;
			//|| state == fdm.models.DownloadState.Allocating

		},
		stateCanBeStarted: function(state){

			return state == fdm.models.DownloadState.Paused
				|| state == fdm.models.DownloadState.Pausing;
		},

		moveSelected: function(){

			var downloads = this.getDownloadsToMoveAndRemove();
			if (!downloads || !downloads.length)
				return;

			var ids = _.pluck(downloads, 'id');

			_apiDownloads.moveToByIds(ids);
		},
		getSelectedIds: function () {

            var downloads = this.getSelectedDownloads();
            return _.pluck(downloads, 'id');
		},
		clearSelection: function () {

			if (this.collections.selectedList.length > 0){
				this.model.set("allSelected", false);
				this.collections.selectedList.reset();
			}
		},
		isSelected: function ( data ) {
			return this.collections.selectedList.get( data.id ) !== undefined;
		},
		isSelectedId: function ( download_id ) {
			return this.collections.selectedList.get( download_id ) !== undefined;
		},
		selectAll: function () {

            var downloads = this.getDownloads();
            this.collections.selectedList.reset(downloads);
			this.model.set("allSelected", true);
		},
		toggleSelectAll: function(){

			var all_selected = this.model.get('allSelected');
			if(all_selected === false){
				this.selectAll();
			}
			else{
				this.clearSelection();
			}
		},
		addToSelection: function ( items ) {
			this.collections.selectedList.add(items);
		},
		removeFromSelection: function ( item ) {
			this.collections.selectedList.remove(item);
		},
		resetSelection: function ( items ) {
			this.collections.selectedList.reset(items);
		},
		setCurrentItemById: function (download_id) {

			var item = this.collections.downloads.get(download_id);
			if (item)
				this.setCurrentItem(item);
		},
		setCurrentItem: function (item) {

			var ci = this.model.get("currentItem");
			if (ci)
				ci.set('current', false);
			if (item){
				item.set('current', true);
				this.model.set("currentItem", item );
			}
			else
				this.model.set("currentItem", null );

            if (item && fdmApp.platform == 'mac')
                fdmApp.downloads.setQuickLookItem(item.id);
		},
		selectRange: function (from, to) {

			if(from == null || from.id == null){
				console.warn("selectRange: 'from' parameter is null");
				return;
			}
			if(to == null || to.id == null){
				console.warn("selectRange: 'to' parameter is null");
				return;
			}
			var fromIndex = this.getDownloadIndex( from.id ),
				toIndex = this.getDownloadIndex( to.id );
			fromIndex = fromIndex == -1 ? 0 : fromIndex;
			toIndex = toIndex == -1 ? 0 : toIndex;

			if ( fromIndex == toIndex ) {
				this.addToSelection(to);
				return;
			}

			var reverse = fromIndex > toIndex;
			if ( reverse ) {
				var _ref;
				_ref = [to, from], from = _ref[0], to = _ref[1];
				var _ref2;
				_ref2 = [toIndex, fromIndex], fromIndex = _ref2[0], toIndex = _ref2[1];
			}
			var range = [];
			for ( var i = fromIndex; i <= toIndex; i++ ) {
				range.push(this.getDownloadByIndex(i));
			}
			this.collections.selectedList.add(range, {silent: true});
			this.collections.selectedList.trigger("reset");
		},
        removeSelectRange: function (from, to) {
            //console.time("select");
            if(from == null || from.id == null){
                console.warn("selectRange: 'from' parameter is null");
                return;
            }
            if(to == null || to.id == null){
                console.warn("selectRange: 'to' parameter is null");
                return;
            }
            var fromIndex = this.getDownloadIndex( from.id ),
                toIndex = this.getDownloadIndex( to.id );
            fromIndex = fromIndex == -1 ? 0 : fromIndex;
            toIndex = toIndex == -1 ? 0 : toIndex;

            if ( fromIndex == toIndex ) {
				this.addToSelection(to);
                return;
            }

            var reverse = fromIndex > toIndex;
            if ( reverse ) {
                var _ref;
                _ref = [to, from], from = _ref[0], to = _ref[1];
                var _ref2;
                _ref2 = [toIndex, fromIndex], fromIndex = _ref2[0], toIndex = _ref2[1];
            }
            var range = [];
            for ( var i = fromIndex; i <= toIndex; i++ ) {
                range.push(this.getDownloadByIndex(i));
            }
			this.collections.selectedList.remove(range, {silent: true});
			this.collections.selectedList.trigger("reset");
        },
		getDownloadByIndex: function ( index ) {

            var downloads = this.getDownloads();
            return downloads[index];
		},
		getDownloadIndex: function ( id ) {

            var download = this.collections.currentDownloads.get(id);
            var downloads = this.getDownloads();
            return _.indexOf(downloads, download);
		},

		getDownloads: function(){

			return this.collections.currentDownloads.models;
		},

		getProgressSortPosition: function(m, for_bottom_panel){

        	var for_bottom_panel = for_bottom_panel || false;

			var state = m.get('state');

			var r;

			if (m.get('isMoving')) {

				r = 60;
			}
			else {

				switch (state){

					case fdm.models.DownloadState.Downloading:

						if (m.get('isQueued'))
							r = 50;
						else
							r = 0;
						break;

					case fdm.models.DownloadState.Reconnecting:

						r = 10;
						break;

					case fdm.models.DownloadState.FileProcessing:

						r = 20;
						break;

					case fdm.models.DownloadState.Checking:

						r = 30;
						break;

					case fdm.models.DownloadState.WaitingForMetadata:

						r = 40;
						break;

					case fdm.models.DownloadState.Pausing:
					case fdm.models.DownloadState.Paused:

						r = 70;
						break;

					case fdm.models.DownloadState.Error:

						r = 80;
						break;

					case fdm.models.DownloadState.Completed:

						if (for_bottom_panel)
							r = 0;
						else {
                            if (m.get('downloadType') == fdm.models.DownloadType.Trt && m.get('seedingEnabled'))
                                r = 90;
                            else
                                r = 100;
						}
						break;

					default :

						r = 110;
				}
			}

			return r;
		},

		sortDownloads: function(a, b){

			var sortOptions = this.model.get('sortOptions');

			var sort_prop = sortOptions.sortProp;
			var sort_reverse = !sortOptions.descending;

			var res = 1;
			if (sort_reverse)
				res = -1;

			if (sortOptions == 'createdDate')
				res = -res;

			var show_completed_date = this.model.get('statusFilter') == fdm.models.DownloadStateFilters.Completed;

			if (sort_prop == 'createdDate' && show_completed_date)
				sort_prop = 'completionDate';

			var av = a.get(sort_prop);
			var bv = b.get(sort_prop);

			if (sort_prop == 'progress'){

				var ap = this.getProgressSortPosition(a);
				var bp = this.getProgressSortPosition(b);
				//var ap = fdm.models.downloadState2sort[a.get('state')];
				//var bp = fdm.models.downloadState2sort[b.get('state')];

				if (ap != bp)
					return ap > bp ? -res : res;

				// if (ap == bp){
                //
				// 	if (show_completed_date)
				// 		return a.get('completionDate') < b.get('completionDate') ? 1 : -1;
				// 	else
				// 		return a.get('createdDate') < b.get('createdDate') ? 1 : -1;
				// }
			}

			if (typeof av == 'string')
				av = av.toLowerCase();
			if (typeof bv == 'string')
				bv = bv.toLowerCase();

			if (show_completed_date && sort_prop != 'completionDate' && bv == av)
				return a.get('completionDate') < b.get('completionDate') ? 1 : -1;

			if (sort_prop != 'createdDate' && bv == av)
				return a.get('createdDate') < b.get('createdDate') ? 1 : -1;

			return bv > av ? -res : res;
		},

		getSelectedDownloads: function(){
			return this.collections.currentSelectedList.models;
		},

		getDownloadsToMoveAndRemove: function(){

			if (this.collections.currentSelectedList.length > 0)
				return this.collections.currentSelectedList.models;
			else{
				var current_item = this.model.get('currentItem');
				if (current_item && this.collections.currentDownloads.get(current_item.id)){
					return [current_item];
				}
			}
			return [];
		},

		filter: function(model){

			var selectedTag = false;
			if (window.app.controllers.tags && window.app.controllers.tags.model)
				selectedTag = window.app.controllers.tags.model.get('selectedTag');

			if (selectedTag)
			{
				var has_tag = false;

				if (selectedTag.id < 100){

					var download_type = model.get('downloadType');
					if (selectedTag.id == 1 && download_type == fdm.models.DownloadType.Trt)
						has_tag = true;
					if (selectedTag.id == 2 && download_type == fdm.models.DownloadType.YouTubeVideo)
						has_tag = true;
				}
				else{

					var tags = model.get("tags");
					for (var i = 0; i < tags.length; ++i)
					{
						if (tags[i].id == selectedTag.id)
						{
							has_tag = true;
							break;
						}
					}
				}

				if (!has_tag)
					return false;
			}

			var statusFilter = this.model.get('statusFilter');

			if (statusFilter !== null){
				var state = model.get("state");

				switch (statusFilter) {
					case fdm.models.DownloadStateFilters.InProgress:

						if (state == fdm.models.DownloadState.Completed)
							return false;
						break;

					case fdm.models.DownloadStateFilters.Completed:

						if (state != fdm.models.DownloadState.Completed)
							return false;
						break;

					case fdm.models.DownloadStateFilters.Active:

						var upload = model.get('uploadSpeedBytes');

						if ( ( state != fdm.models.DownloadState.Downloading /* || model.get('downloadSpeedBytes') <= 0 */ )
							&& (typeof upload == 'undefined' || upload <= 0))
							return false;
						break;

					case fdm.models.DownloadStateFilters.Inactive:

						if (state == fdm.models.DownloadState.Downloading && model.get('downloadSpeedBytes') > 0 || model.get('uploadSpeedBytes') > 0)
							return false;
						break;

					case fdm.models.DownloadStateFilters.Error:

						if (state != fdm.models.DownloadState.Error)
							return false;
						break;
				}
			}

			var text = this.model.get('activeFilterText');
			if(text == "") {
				return true;
			}

			return model.get("fileName").toLowerCase().indexOf(text.toLowerCase()) >= 0;
		},

		sort: function (sortProp) {

			var sortOptions = this.model.get('sortOptions');

			if (sortProp == sortOptions.sortProp ) {
				sortOptions.descending = !sortOptions.descending;
				this.model.set({sortOptions: sortOptions});
				this.model.trigger('change:sortOptions', sortOptions);
			}
			else{
				sortOptions = _.clone(this.model.get('defaultOrderOptions')[sortProp]);
				this.model.set({sortOptions: sortOptions});
			}

			window.app.appViewManager.hasChanges();
		},

		setStatusFilter: function(new_value){

			this.model.set({statusFilter: new_value});
		},

		noDownloadsExist: function(){

			return this.collections.downloads.length == 0;
		},

		allDownloadsFiltered: function(){

			return this.getDownloads().length == 0;
		},

		getToolbarActions: function(for_toolbar_template){

			var selection_list = this.getSelectedDownloads();

			var selection_ids = _.pluck(selection_list, 'id');

			var selectionCanBeRemoved = selection_list.length > 0;
			var selectionCanBeDownloaded = false;
			var selectionCanBePaused = false;
			var selectionCanBeRestarted = false;
			var selectionCanBeMovable = selection_list.length > 0;


			for(var i= 0; i < selection_list.length; ++i ) {

				var is_moving = selection_list[i].get('isMoving');
				var child_state = selection_list[i].get('state');

				if (!is_moving){
					if (child_state == fdm.models.DownloadState.Paused || child_state == fdm.models.DownloadState.Pausing)
						selectionCanBeDownloaded = true;
					if (child_state == fdm.models.DownloadState.Error)
						selectionCanBeRestarted = true;
					if(child_state == fdm.models.DownloadState.Downloading ||
						child_state == fdm.models.DownloadState.WaitingForMetadata ||
						//child_state == fdm.models.DownloadState.Allocating ||
						child_state == fdm.models.DownloadState.Reconnecting ||
						child_state == fdm.models.DownloadState.Checking){
						selectionCanBePaused = true;
					}
				}

				if (is_moving || selection_list[i].get('isMovable') == false)
					selectionCanBeMovable = false;
			}

			return {
				SelectionCanBeDownloaded: selectionCanBeDownloaded,
				SelectionCanBePaused: selectionCanBePaused,
				SelectionCanBeRemoved: selectionCanBeRemoved,
				SelectionCanBeMovable: selectionCanBeMovable,
				SelectionCanBeRestarted: selectionCanBeRestarted,
				selectedIds: selection_ids
			};
		},

		selectDownload: function(download_id){

			var download = this.collections.downloads.get(download_id);
			if (!download)
				return;

			var download_index = this.getDownloadIndex(download_id);
			if (download_index < 0){
				this.model.set({activeFilterText: ''});
				app.controllers.tags.resetSelectedTag();
				download_index = this.getDownloadIndex(download_id);
			}

			if (download_index >= 0){

				this.setCurrentItem(download);
				this.view_model.scrollToItemById(download_id);
			}
		},

		resetTagsAndFilters: function(){

			window.app.controllers.tags.selectTagById(null);

			this.model.set({
				statusFilter: this.model.defaults.statusFilter,
				activeFilterText: this.model.defaults.activeFilterText
			});
		},

		resetFilters: function(){

			this.model.set({
				statusFilter: this.model.defaults.statusFilter,
				activeFilterText: this.model.defaults.activeFilterText
			});
		},

		updateToolbarActions: function(){

			var result = {
				forSelected: false,
				forAll: true,
				canBeDownloaded: false,
				canBePaused: false,
				canBeRemoved: false,
				canBeMovable: false,
				canBeRestarted: false
			};

			var downloads = this.collections.currentSelectedList;
			if (downloads.length > 0){
				result.forSelected = true;
				result.forAll = false;
				result.canBeRemoved = true;
				result.canBeMovable = true;
			}
			else{
				result.forSelected = false;
				result.forAll = true;
				downloads = this.collections.currentDownloads;

				var current_item = this.model.get('currentItem');
				if (current_item && downloads.get(current_item.id)){
					result.canBeRemoved = true;
					if (!current_item.get('isMoving') && current_item.get('isMovable')){
						result.canBeMovable = true;
					}
				}
			}

			if (downloads.length == 0){
				this.model.get('toolbarActions').set(result);
				return;
			}

			downloads.map(function(download){

				var state = download.get('state');
				var is_moving = download.get('isMoving');

				if (!is_moving){
					if (this.stateCanBeStarted(state))
						result.canBeDownloaded = true;
					if (state == fdm.models.DownloadState.Error)
						result.canBeRestarted = true;
					if(this.stateCanBePaused(state)){
						result.canBePaused = true;
					}
				}

				if (result.forSelected && (is_moving || download.get('isMovable') == false))
					result.canBeMovable = false;
			}.bind(this));

			this.model.get('toolbarActions').set(result);
		},

		checkCurrentItem: function(){

            var ci = this.model.get('currentItem');

            if (ci){
                if (!this.collections.downloads.get(ci.id))
                    this.setCurrentItem(false);
            }

			/*
			var ci = this.model.get('currentItem');

			if (ci){
				if (!this.collections.currentDownloads.get(ci.id)){

					this.setCurrentItem(false);
					if (app.controllers.bottomPanel.model.get('panelVisible'))
						app.controllers.bottomPanel.hide();
				}
			}
			*/
		},

		changeCurrentDownloads: function(){

			// this.checkCurrentItem();

			this.changeSelectedList();
			this.updateToolbarActions();
		},

		changeSelectedTag: function(){

			this.clearSelection();
			this.updateCurrentDownloads();
		},

		changeStatusFilter: function(){

			this.clearSelection();
			this.updateCurrentDownloads();
			this.collections.currentDownloads.sort();
		},

		changeActiveFilterText: function(){

			this.clearSelection();
			this.updateCurrentDownloads();
		},

		updateCurrentDownloads: function(){

			this.collections.currentDownloads.set(this.collections.downloads.filter(this.filter, this), {silent: true});
			this.collections.currentDownloads.trigger('reset');
		},

		changeSelectedList: function(){

			var selection = _.filter(this.collections.selectedList.models, this.filter, this);
			var prev_value = this.model.get('allSelected');

			var downloads = this.getDownloads();
			var has_selected = false;
			var all_selected = true;

			var count_selected = 0;
			var selected_size = 0;

			var new_value = false;

			if(selection.length && downloads.length){
                for(var i in downloads){

					if ( this.isSelectedId(downloads[i].id)){
						count_selected++;
						var s = downloads[i].get('downloadedBytes');
						if (s > 0)
							selected_size += s;
						has_selected = true;
					}
					else{
						all_selected = false;
					}
				}
				new_value = all_selected;
				if (has_selected != all_selected){
					new_value = undefined;
				}
			}

			this.model.set({
				allSelected: new_value,
				countSelected: count_selected,
				selectedSize: selected_size
			});
			this.collections.currentSelectedList.reset(selection);

			this.updateToolbarActions();
		},

		fakeAdd: function() {

			window.app.controllers.downloadWizard.newDownload(undefined);
		},

		getDownloadOnTarget: function(target){

			if (target.nodeName.toLowerCase() == 'li' && target.classList.contains('row')){
				var id = target.getAttribute('data-id');
				var index = target.getAttribute('data-index');

				return {id: id, index: index};
			}
			else if (target.parentNode){
				return this.getDownloadOnTarget(target.parentNode);
			}

			return null;
		},
		currentDropIsActiveElement: null,
		setDropIsActiveDownload: function(download_id){

			if (download_id !== this.currentDropIsActiveElement){

				var prev_model = this.collections.downloads.get(this.currentDropIsActiveElement);

				if (prev_model)
					prev_model.set({dropIsActive: false});

				var current_model = this.collections.downloads.get(download_id);
				if (current_model)
					current_model.set({dropIsActive: true});

				this.currentDropIsActiveElement = download_id;
			}
		},
		onDropTag: function(download_id, tag_id, added_for_selected){

			added_for_selected = added_for_selected || false;

			var download = this.collections.downloads.get(download_id);
			if (!download)
				return;

			if (added_for_selected && app.controllers.downloads.collections.currentSelectedList.get(download_id)){

				app.controllers.downloads.collections.currentSelectedList.map(function(model){

					this.addTagItemById(model.id, tag_id);
				}.bind(this))

			}
			else{
				this.addTagItemById(download_id, tag_id);
			}
		}

	};

	return Class;
})();

fdm.viewModels.DownloadList = (function () {
	var _controller; // use local variable to hide some members from a view
	var _view;

	function Class(controller, collections, model) {
		_controller = controller;
		_view = this;

		_.bindAll(this, "launchItem", "selectItemByMouse", "handleKeydown", "showPopupMenu");
	}

	Class.prototype = {

		refreshDownloadList: function () {

		},

		selectionStart: null,
		selectionStartSelected: false,

		selectItemByMouse: function(data, event) {

            //if (app.controllers.tagsManageDialog.view_model.isManagerTagsOpen)
            //    return;

            var dataModel = data;

            if ( ! _controller.getDownloadByIndex(0) ) {
                return;// true;
            }

            if ( ( event.button != 0 || event.button == 0 && fdmApp.platform == 'mac' && event.ctrlKey)
				&& _controller.isSelected( dataModel ) ) {
                return;// false;
            }

			var ctrlKey = false;
			if (fdmApp.platform == 'mac')
				ctrlKey = event.metaKey;
			else
				ctrlKey = event.ctrlKey;

            if ( ! ctrlKey &&  ! event.shiftKey ) {

				this.selectionStart = dataModel;
				this.selectionStartSelected = _controller.isSelected(dataModel);

                //this.selectionStart = null;
                //this.selectionStartSelected = false;
                //if ( dataModel === _controller.model.get("currentItem") ) {
                //    var dontSelect = _controller.isSelected( dataModel ) && _controller.getSelectedIds().length === 1;
                //}
                //if ( dontSelect ) {
                //    return;
                //} else {
                    _controller.clearSelection();
                    //_controller.addToSelection(dataModel);
                //}
            }
			else if ( event.shiftKey ) {

				if (!this.selectionStart){
					var current = _controller.model.get("currentItem");
					this.selectionStart = current;
					this.selectionStartSelected = _controller.isSelected(current);
				}

                //if(this.selection.length == 0){
                //    // if no selection, Shift is not taken into account. Selection is single.
                //    this.selectionStart = dataModel;
					//this.selectionStartSelected = _controller.isSelected(dataModel);
                //}
                //if(this.selection.length == 1){
					//var current = _controller.model.get("currentItem");
                //    this.selectionStart = current;
					//this.selectionStartSelected = _controller.isSelected(current);
                //}
                //if ( !ctrlKey ) { // if not Ctrl clear selection
                //    _controller.clearSelection();
                //}
                // if the window has no focus, this.selectionStart or _controller.model.get("currentItem") can be null
                _controller.selectRange( this.selectionStart || _controller.model.get("currentItem") || dataModel, dataModel );

            }
            else if ( ctrlKey ) {
                if ( _controller.isSelected( dataModel ) ) {
                    _controller.removeFromSelection( dataModel )
                } else {
                    _controller.addToSelection( dataModel )
                }
                this.selectionStart = dataModel;
                this.selectionStartSelected = _controller.isSelected(dataModel);
            }
			_controller.setCurrentItem(dataModel);
            this.scrollToItemById(dataModel.get("id"));
		},

		handleKeyup: function(data, event) {
			if ( event.keyCode === 16 ) { // shift pressed
				this.shiftDown = false;
			}
		},

		handleKeydown: function(data, event) {

            if ($("#scrolling-list").hasClass("innactive"))
                return;

            var downloads = _controller.getDownloads();

			var ctrlKey = false;
			if (fdmApp.platform == 'mac')
				ctrlKey = event.metaKey;
			else
				ctrlKey = event.ctrlKey;

			if (ctrlKey && event.keyCode == '70'){ // CTRL+F

				$('#search-input-text').focus();
				stopEventBubble(event);
				return true;
			}

            if (!_.contains([13,27,32,33,34,35,36,38,40,46,65,86,97], event.keyCode)){
                if(ctrlKey){
					var current = _controller.model.get("currentItem");
					if (current){
						this.selectionStart = current;
						this.selectionStartSelected = _controller.isSelected(current);
					}
                }
                return true;
            }

            if (event.keyCode == 86) { // CTRL+V
                if(ctrlKey) {
                    _controller.addFromClipboard();
                    stopEventBubble(event);
                    return false;
                }
                return true;
            }
            if (!_controller.getDownloadByIndex(0)){
                // skip hot keys because the code which is located at the bottom works with selection.
                return true
            }

            if ( event.keyCode === 27 ) { // ESC
                _controller.clearSelection();

                return;
            }

            if ( event.keyCode === 46 ) { // delete

				_controller.deleteAction();
				return;
            }

            if ( /\b65|97\b/.test(event.keyCode) ) { // CTRL+A
				stopEventBubble(event);
                return ctrlKey ? _controller.selectAll() : true
            }

            var prevId = null;
            var prevI = _controller.model.get("currentItem");
            if (prevI)
                prevId = prevI.get('id');

			if (event.keyCode == 13) { // Enter

				if (prevI){
					this.launchItem(prevI);
				}
				return true;
			}

            //if (!prevId)
            //    _controller.model.set("currentItem", _controller.getDownloadByIndex(0));

            var index = _controller.getDownloadIndex( prevId );

            if (/\b38|40\b/.test(event.keyCode)) { // up / down
                event.keyCode === 38 ? --index : ++index
            }

            if(event.keyCode === 35) { // END
                index = downloads.length - 1;
            }
            if(event.keyCode === 36) { // HOME
                index = 0;
            }
            if(/\b33|34\b/.test(event.keyCode)) { // PG UP / DOWN
                var up = event.keyCode === 33;
                var count_on_page = this.getCountItemsOnPage();

                if ( up )
                    index = index - count_on_page;
                else
                    index = index + count_on_page;
            }

            index = Math.max( 0, Math.min( index, downloads.length - 1 ) );
			var nextItem = _controller.getDownloadByIndex( index );

            var preventReselect = nextItem && _controller.isSelected( nextItem ) && _controller.getSelectedIds().length < 2;

            if ( ! event.shiftKey && ! ctrlKey && ! preventReselect ) {
                //_controller.clearSelection();
                this.selectionStart = null;
                this.selectionStartSelected = false;
            }

            if ( event.keyCode === 32 ) { // space
                if (fdmApp.platform == 'mac')
                {
                    var id = _controller.model.get("currentItem").get('id');
					this.scrollToItemById(nextItem.get('id'));

                    var top = 0, left = 0, width = 0, height = 0;
					var download_row = document.getElementById('row_' + id);
                    if (download_row)
                    {
						var img_container = download_row.getElementsByClassName('compact-preview-img');

						if (img_container && img_container.length > 0){

							var img = img_container[0];
							var rect = img.getBoundingClientRect();

							top = rect.top;
							left = rect.left;
							width = rect.width;
							height = rect.height;
						}
                    }
                    fdmApp.downloads.toggleQuickLook(left, top, width, height);
                }
                else
                {
					var dataModel =  _controller.model.get("currentItem");
                    if ( _controller.isSelected( dataModel ) ) {
                        _controller.removeFromSelection( dataModel )
                    } else {
                        _controller.addToSelection( dataModel )
                    }
                }
            }

            if ( nextItem ) {

                //if ( nextItem === _controller.model.get("currentItem") && event.keyCode != 32 ) {
                //    if ( preventReselect ) {
                //        return
                //    }
                //}

                if ( ctrlKey ) {

					if (prevI){
						if (!this.selectionStartSelected)
							_controller.addToSelection( prevI );
						else
							_controller.removeFromSelection( prevI );
					}

					if (!this.selectionStartSelected)
						_controller.addToSelection( nextItem );
					else
						_controller.removeFromSelection( nextItem );
                }
				else if ( event.shiftKey ) {
					//    _controller.clearSelection();
					//
					_controller.selectRange(this.selectionStart || _controller.model.get("currentItem"), nextItem);
				}

				_controller.setCurrentItem(nextItem);
                this.scrollToItemById(nextItem.get('id'));
                stopEventBubble(event);
            }
		},

		needShowPopupMenu: function(model, e){
            if (e.button == 2 || e.button == 0 && fdmApp.platform == 'mac' && e.ctrlKey)
                this.showPopupMenu(model, e);
        },

		lastPopupMenuDownloadId: false,

		showPopupMenu: function(model) {
			var ids;

			window.app.closeAllPopups();
			_controller.setCurrentItem(model);

			if (!_controller.isSelected(model)){
				_controller.clearSelection();
				ids = [model.id];
			}
			else{
				ids = _controller.getSelectedIds();
				if (!ids || ids.length == 0)
					ids = [model.id];
			}

			if ( ids ) {

                fdmApp.menuManager.showPopupMenu(ids, model.id, app.controllers.bottomPanel.model.get('panelVisible'));
			}

			var m_id = model.id;

			this.lastPopupMenuDownloadId = m_id;

			setTimeout(function(){
				this.lastPopupMenuDownloadId = m_id;
			}.bind(this), 100)
		},

		closePopupMenu: function(){

			if (fdmApp.platform == 'win'){
				setTimeout(function(){
					this.lastPopupMenuDownloadId = false;
				}.bind(this), 200);
			}
			else{
				this.lastPopupMenuDownloadId = false;
			}
		},

		openMainMenu: function() {
			app.controllers.menu.toggleMenu();
		},

		closeMenu: function ( data, event ) {
			if (this.menuVisible) {
				var menuName = $("#transparent-overlay").data('opener');
				$(menuName).click();
			}
		},

		bottomPanelVisibility: function() {
			window.app.controllers.bottomPanel.showToggle();
		},

        updateDownloadsFocus: function(){
            if (!window.app.mainFrameInFocus) {
                //console.log('updateDownloadsFocus !window.app.mainFrameInFocus');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.downloadWizard.model.get('addSourcePageIsShown')) {
                //console.log('updateDownloadsFocus addSourcePageIsShown');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.downloadWizard.model.get('sourceInfoPageIsShown')) {
                //console.log('updateDownloadsFocus sourceInfoPageIsShown');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.menu.model.get('opened')) {
                //console.log('updateDownloadsFocus menuVisible');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.trafficPanel.model.get('speedDialogOpened')) {
                //console.log('updateDownloadsFocus menuVisible');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.customSpeedDialog.model.get('dialogIsShown')) {
                //console.log('updateDownloadsFocus menuVisible');
                $("#scrolling-list").addClass("innactive");
            }
            else if (app.controllers.settings.model.get('opened')) {
                //console.log('updateDownloadsFocus menuVisible');
                $("#scrolling-list").addClass("innactive");
            }
            //else if (app.controllers.tagsManageDialog.view_model.isManagerTagsOpen) {
            //    //console.log('updateDownloadsFocus isManagerTagsOpen');
            //    $("#scrolling-list").addClass("innactive");
            //}
            else{
                if (document.activeElement){
                    var active_el = document.activeElement;
                    if (active_el.tagName.toLowerCase() == 'input' && active_el.id == 'search-input-text') {
                        $("#scrolling-list").addClass("innactive");
                    }
                    else{
                        $("#scrolling-list").removeClass("innactive");
                    }
                }
                else{
                    $("#scrolling-list").removeClass("innactive");
                }
            }
        },

		onFocusLost: function(){
            this.updateDownloadsFocus();
		},

		onFocus: function(){
            this.updateDownloadsFocus();
		},

		launchItem: function(model) {
			_controller.launchItemById(model.id);
		},

		removeSelected: function(){
			_controller.deleteAction();
		},

		restartSelected: function(){
			_controller.restartSelected();// Remove from List
		},

		stopSelected: function(){
			_controller.stopSelected();
		},

		stopAll: function(){
			_controller.stopAll();
		},

		startSelected: function(){
			_controller.startSelected();
		},

		startAll: function(){
			_controller.startAll();
		},

		moveSelected: function(){
			_controller.moveSelected();
		},

		setFocus: function(){
            $( "#header" ).focus();
		},

		scrollToItemById: function(id){

            var current_item =  _controller.model.get("currentItem");
            if (current_item == null)
            {
                var item = _controller.collections.currentDownloads.get(id);
                if (item)
					_controller.setCurrentItem(item);
            }

            this.scrollToItemByIdOverView(id);
		},

		setDownloadListFocus: function(){
            this.onFocus();
		},
		removeDownloadListFocus: function(){
            this.onFocusLost();
		},

		setSearchFocus: function(){
			this.setDownloadListFocus();
		},
		setSearchLostFocus: function(){
			this.removeDownloadListFocus();
		},

        doAfterTrackersAdding: function (id) {

            var item = _controller.collections.downloads.get(id);

            if (!item){
                var properties = ["id", "batchId"];

                _controller.getDownloadsPropertiesPromise([id], properties)
                    .then(function(batch_data){

                    	if (batch_data && batch_data.id && batch_data.properties && batch_data.properties.batchId)
							this.doAfterDownloadAdding(batch_data.properties.batchId);
                    	else
                            this.doAfterDownloadAdding(id);

					}.bind(this));
			}
			else
				this.doAfterDownloadAdding(id);

        },
		doAfterDownloadAdding: function (id) {

			_controller.changeSelectedList();
			_controller.updateCurrentDownloads();
			//_controller.model.set("allSelected", false);
			_controller.resetFilters();

			var item = _controller.collections.downloads.get(id);

			var selectedTag = false;
			if (window.app.controllers.tags && window.app.controllers.tags.model)
				selectedTag = window.app.controllers.tags.model.get('selectedTag');

			if (selectedTag)
			{
				var has_tag = false;
				var tags = item.get("tags");
				for (var i = 0; i < tags.length; ++i)
				{
					if (tags[i].id == selectedTag.id)
					{
						has_tag = true;
						break;
					}
				}
				if (!has_tag)
					app.controllers.tags.resetSelectedTag();
			}

			if (item){
				_controller.setCurrentItem(item);
				//_controller.clearSelection();
				//_controller.addToSelection(item);
			}
			else{
				item = _controller.collections.currentDownloads.last();
				_controller.setCurrentItem(item);
				//_controller.clearSelection();
				//_controller.addToSelection(item);
			}

			_.defer(function(){

				_.defer(_.bind(this.scrollToItemById, this, id));
			}.bind(this));

			var f = app.controllers.downloads.collections.downloads.findWhere({isScheduled: true});
			if (f)
				_controller.needShowTagsPanelTip();
		},

        row_height: 40,

        getCountItemsOnPage: function(){

            var scroll_container = document.getElementById('downloads-scroll-container');
            var offsetHeight = window.innerHeight;

            if (scroll_container)
            {
                offsetHeight = scroll_container.offsetHeight;
            }

            return Math.round(offsetHeight/this.row_height);
        },

        getItemByScrollPosition: function(scroll_pos){

			var i = Math.ceil(scroll_pos/this.row_height);

			return _controller.getDownloadByIndex(i);
        },

        getViewItemsPositions: function(count_items)
        {

            var scroll_container = document.getElementById('downloads-scroll-container');
            var scrollTop = 0;
            var offsetHeight = window.innerHeight;

            if (scroll_container)
            {
                scrollTop = scroll_container.scrollTop;
            }

            var start = Math.round(scrollTop/this.row_height);

            var id_start = Math.max(start - 5, 0);
            var count_in_page = Math.round(offsetHeight/this.row_height);

            var id_end = Math.min(start + count_in_page + 2, count_items - 1);

            var before_view = id_start;
            var after_view = Math.max(count_items - 1 - id_end, 0);

            return {id_start : id_start, id_end : id_end,
                    before_view: before_view, after_view: after_view,
                    before_height: before_view * this.row_height, after_height: after_view * this.row_height}
        },

        scroll2itemIndex: function(index, last_child){

			last_child = last_child || false;

            var scroll_container = document.getElementById('downloads-scroll-container');

            if (!scroll_container)
                return;

            var current_scroll = scroll_container.scrollTop;
            var offsetHeight = scroll_container.offsetHeight - 2;
            var top_positions = this.row_height * index;

			var fix = 0;
			if (last_child)
				fix = 2;

            if (current_scroll > top_positions)
                scroll_container.scrollTop = top_positions + fix;
            else if (top_positions > current_scroll + offsetHeight - this.row_height)
				scroll_container.scrollTop = top_positions - offsetHeight + this.row_height + fix;
        },

		itemInVisiblePosition: function(id){

			var download = _controller.collections.currentDownloads.get(id);

			if (!download)
				return false;

			var downloads = _controller.getDownloads();
			var index = _.indexOf(downloads, download);

			if (index < 0)
				return false;

			var scroll_container = document.getElementById('downloads-scroll-container');

			if (!scroll_container)
				return false;

			var current_scroll = scroll_container.scrollTop;
			var offsetHeight = scroll_container.offsetHeight - 2;
			var top_positions = this.row_height * index;

			if (current_scroll > top_positions || top_positions > current_scroll + offsetHeight - this.row_height)
				return false;
			else
				return true;
		},

        scrollToItemByIdOverView: function(id){

            var download = _controller.collections.currentDownloads.get(id);

            if (download)
            {
                var downloads = _controller.getDownloads();
                var index = _.indexOf(downloads, download);

                if (index >= 0)
                {
					var last = index == downloads.length - 1;
                    this.scroll2itemIndex(index, last);
                }
            }
        },

        lastClickAsCheckbox:0,
        clickAsCheckbox: function(data, event) {

            stopEventBubble(event);
            this.setFocus();
            var current_date = +new Date();
            if (current_date - this.lastClickAsCheckbox < 250)
                return;
            this.lastClickAsCheckbox = current_date;

			if (event.shiftKey == false)
			{
				if (fdmApp.platform == 'mac')
					event.metaKey = true;
				else
					event.ctrlKey = true;
			}
            this.selectItemByMouse(data, event);
        }

	};

	return Class;
})();
