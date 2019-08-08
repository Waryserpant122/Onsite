jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.viewModels.BottomPanel_MinHeight = 240;

fdm.models.filePriority = {
	Unknown: -1,
	Skip: 0,
	Low: 1,
	Normal: 4,
	High: 7
};

fdm.controllers.BottomPanel = (function () {

	function Class(apiDownloads, downloadsCtrl) {
		this.apiDownloads = apiDownloads;
		this.downloadsCtrl = downloadsCtrl;

		var self = this;
		_.bindAll(this, 'applyFilePreview', 'refreshFiles', 'refreshProgressMap', 'refreshModelByTime',
			'onLogMessagesHandler', 'onItemPriorityChanged', 'setBatchDownloads', 'batchItemChanged');

		fdm.models.BottomPanel = Backbone.Model.extend({
			defaults: {
				currentItem: null,
				currentItemId: null,
				currentItemDownloadType: null,
				//progress: -1,
				currentTab: fdm.views.BottomPanelTab.General,
				//logs: new Backbone.Collection([]),
				logs: [],
				trackers: new Backbone.Collection([]),
				peers: new Backbone.Collection([]),
				progressMap: false,
				dhtNodes: 0,
				fileTree: new fdm.models.FileTree({type: 'bottom_panel'}),
				thumb: {
                    maxWidth: 260,
                    maxHeight: 260
				},
				//skipRefreshFiles: true,
				domain: "",
				url: "",
				mainViewName: "",
				showThumbnail: false,
				//customSpeedMode: 0, // is set in trafficPanel
				panelVisible: false,
                sortOptions: {
                    sortProp: 'name',
                    descending: true
                },
				batchDownloadsCount: null
			}
		});
		fdmApp.downloads.addEventListener("onItemThumbnailChanged", this.applyFilePreview);
		apiDownloads.onLogMessages = this.onLogMessagesHandler;
		apiDownloads.onItemPriorityChanged = this.onItemPriorityChanged;

		this.model = new fdm.models.BottomPanel();

		var view_state = app.appViewManager.getBottomPanelState();
		if (view_state.visible)
			this.show();
		else
			this.hide();

        this.defaultOrderOptions = {
            name: { sortProp: "name", descending: true },
            size: { sortProp: "size", descending: true },
            progress: { sortProp: "progress", descending: false },
            priority: { sortProp: "priority", descending: false }
        };

        this.treeComparator = _.bind(this.treeComparatorFn, this, this.model.get('sortOptions'));
        this.downloadComparator = _.bind(this.downloadComparatorFn, this, this.model.get('sortOptions'));

        fdm.models.BottomPanelDownloadListCollection = fdm.models.DownloadListCollection.extend({
            comparator: function(){
                return app.controllers.bottomPanel.downloadComparator.apply(this, arguments);
            }
        });

		this.collections = {};
		this.collections.downloads = new fdm.models.BottomPanelDownloadListCollection;

		//this.model.on("change:panelVisible", function(model, new_value){
        //
		//	app.appViewManager.setBottomPanelState('visible', new_value);
        //
		//}, this);

		this.model.on("change:currentItem", function(model, new_item){

            //var img_node = document.getElementById("bottom-panel-file-preview-img");
            //if (img_node)
            //    img_node.src = '';

            this.model.get('trackers').reset([]);
            this.model.get('peers').reset([]);
            this.model.set('progressMap', false);
			this.collections.downloads.reset([]);
            //this.model.get('logs').reset([]);
            this.model.set('logs', []);

            var download_type = false;
            if (new_item)
            	download_type = new_item.get('downloadType');
            var current_tab = this.model.get('currentTab');

            if (new_item && download_type != fdm.models.DownloadType.Trt){
                if (current_tab == fdm.views.BottomPanelTab.Peers
                    || current_tab == fdm.views.BottomPanelTab.Trackers
                    || current_tab == fdm.views.BottomPanelTab.Files
						&& new_item.get('downloadType') != fdm.models.DownloadType.batchDownload)
                    this.model.set('currentTab', fdm.views.BottomPanelTab.General);
            }

            if (new_item && current_tab == fdm.views.BottomPanelTab.Progress &&
				(download_type == fdm.models.DownloadType.batchDownload
				|| download_type == fdm.models.DownloadType.YouTubeVideo
                || download_type == fdm.models.DownloadType.InfoRequest ))
                this.model.set('currentTab', fdm.views.BottomPanelTab.General);

			this.refreshThumb(1);
			this.refreshFiles();
			this.refreshBatchDownloadsCount();
            this.refreshPeers();
            this.refreshProgressMap();
            this.refreshLogs();
            this.refreshDHTNodes();
		}, this);

		this.model.on("change:currentTab", this.onChangeCurrentTab, this);
		this.model.on("change:sortOptions", this.sortOptionsChange, this);

		app.controllers.downloads.model.on('change:currentItem', this.setCurrentItem, this);

		setInterval(this.refreshModelByTime, 2000);
	}
	Class.prototype = {

        sort: function (sortProp) {

            var sortOptions = _.clone(this.model.get('sortOptions'));

            if (sortProp == sortOptions.sortProp ) {
                sortOptions.descending = !sortOptions.descending;
                this.model.set({sortOptions: sortOptions});
            }
            else{
                sortOptions = _.clone(this.defaultOrderOptions[sortProp]);
                this.model.set({sortOptions: sortOptions});
            }
        },

        sortOptionsChange: function () {

        	this.changeTreeComparator();
        	this.changeDownloadComparator();
        },

		changeTreeComparator: function () {

            this.treeComparator = _.bind(this.treeComparatorFn, this, this.model.get('sortOptions'));

            this.model.get('fileTree').sort();
            this.model.get('fileTree').trigger('change');
            this.model.trigger('change:fileTree');
        },

		treeComparatorFn: function(sortOptions, a, b) {

            var sort_prop = sortOptions.sortProp;
            var sort_reverse = !sortOptions.descending;

            var res = 1;
            if (sort_reverse)
                res = -1;

            var av = a.get('data')[sort_prop];
            var bv = b.get('data')[sort_prop];

            if (typeof av == 'string')
                av = av.toLowerCase();
            if (typeof bv == 'string')
                bv = bv.toLowerCase();

            if (sort_prop !== 'name' && bv == av){

                av = a.get('data')['name'];
                av = av.toLowerCase();
                bv = b.get('data')['name'];
                bv = bv.toLowerCase();

                return bv > av ? -1 : 1;
			}

			return bv > av ? -res : res;
        },

		changeDownloadComparator: function () {

            this.downloadComparator = _.bind(this.downloadComparatorFn, this, this.model.get('sortOptions'));

            this.collections.downloads.sort();
            this.collections.downloads.trigger('change');
        },

		downloadComparatorFn: function(sortOptions, a, b) {

            var sort_prop = sortOptions.sortProp;
            var sort_reverse = !sortOptions.descending;

        	switch (sortOptions.sortProp){

                case 'name':
                    sort_prop = 'fileName';
                    break;

                case 'size':
                    sort_prop = 'totalBytes';
                    break;

                // case 'progress':
                //     sort_prop = 'progress';
                //     break;

                // case 'priority':
                //     sort_prop = 'priority';
                //     break;

			}

            var res = 1;
            if (sort_reverse)
                res = -1;

            var av = a.get(sort_prop);
            var bv = b.get(sort_prop);


            if (sort_prop == 'progress'){

                var ap = app.controllers.downloads.getProgressSortPosition(a, true);
                var bp = app.controllers.downloads.getProgressSortPosition(b, true);
                //var ap = fdm.models.downloadState2sort[a.get('state')];
                //var bp = fdm.models.downloadState2sort[b.get('state')];

                if (ap != bp)
                    return ap > bp ? -res : res;
            }

            if (typeof av == 'string')
                av = av.toLowerCase();
            if (typeof bv == 'string')
                bv = bv.toLowerCase();

            if (sort_prop != 'createdDate' && bv == av)
                return a.get('fileName').toLowerCase() < b.get('fileName').toLowerCase() ? -1 : 1;

            return bv > av ? -res : res;
        },

		batchItemChanged: function (download_id, changes) {
			let download = this.collections.downloads.get(download_id);
			if (!download)
				return;
            const current_state = download.get('state');
            if (current_state === fdm.models.DownloadState.WaitingForMetadata ) {
                this.batchItemApplyFilePreview(download.id);
            }
            if (changes.hasOwnProperty('downloadType') && changes.downloadType !== download.get('downloadType')){
                this.batchItemApplyFilePreview(download.id);
            }
			download.set(changes);
		},

		batchItemApplyFilePreview: function (download_id) {
			let download = this.collections.downloads.get(download_id);
			if (!download)
				return;
            download.set("thumbCache", download.get('thumbCache') + 1);
		},

		setCurrent: function (download) {

			var ci = this.collections.downloads.findWhere({current: true});
			if (ci)
				ci.set({current: false});

			if (!download)
				return;

			download.set({current: true});
		},

		setCurrentItem: function() {

			var item = app.controllers.downloads.model.get('currentItem');

			var item_id = null, item_download_type = null;
			if (item){
				item_id = item.id;
				item_download_type = item.get('downloadType');
			}

			var changes = {
				currentItem: item,
				currentItemId: item_id,
				currentItemDownloadType: item_download_type
			};

			this.unsubscribeCurrentItem();
			this.model.set(changes);
			this.subscribeCurrentItem();
		},
		setView: function(view){
			this.model.set("mainViewName", view);
			this.refreshThumb(0);
		},
		onChangeCurrentTab: function(model, value, options){

			this.refreshTrackers();
			this.refreshPeers();
			this.refreshProgressMap();
			this.refreshFiles();
			this.refreshLogs();
		},
		onChangeCurrentItemThumb: function(model, value, options){
//console.log("onChangeCurrentItemThumb: %o", arguments);
			options = options || {};
			this.refreshThumb(0, !!options.force);
		},
		refreshModelByTime: function(){
            // TODO: fix it
            if (this.model.get('panelVisible')){
                this.refreshDHTNodes();
                this.refreshPeers();
                this.refreshTrackers();
            }
		},
		setPeers: function(data){

			if(!this.model.get("currentItem") || data.id != this.model.get("currentItem").get("id"))
				return;

			if (data.peers)
				this.model.get("peers").reset(data.peers);

		},
		refreshPeers: function() {
			if(this.model.get("currentTab") != fdm.views.BottomPanelTab.Peers) return;
			var currentItem = this.model.get("currentItem");

            if(!currentItem || currentItem.get("downloadType") != fdm.models.DownloadType.Trt){
				return;
			}

			this.downloadsCtrl.getDownloadPropertiesPromise(currentItem.get("id"), ["peers"])
				.then(_.bind(this.setPeers, this));

		},
		refreshProgressMap: function() {

            if (this.refreshProgressMapTimeout){
                clearTimeout(this.refreshProgressMapTimeout);
                this.refreshProgressMapTimeout = 0;
            }

			if(this.model.get("currentTab") != fdm.views.BottomPanelTab.Progress)
				return;

            var currentItem = this.model.get("currentItem");

            if(!currentItem){
				return;
			}

			var query = {
                downloadId: currentItem.get("id"),
                chunks_count: 1013
			};

			fdmApp.downloads.getProgressMap(query, function(intervals){

				if (typeof intervals == 'number')
                    intervals = [intervals];

				this.model.set({
                    progressMap: {
                    	size: 1013,
						intervals: intervals
					}
                });

			}.bind(this));

		},
		setTrackers: function(data){

			if(!this.model.get("currentItem") || data.id != this.model.get("currentItem").get("id"))
				return;

			if (data.trackers)
			{
				var trackers = data.trackers;
				trackers.sort(function(a,b){
					return String.naturalCompare(a.url.toLowerCase(), b.url.toLowerCase());
				});
				this.model.get("trackers").reset(trackers);
			}

		},
        refreshTrackers: function() {
            if(this.model.get("currentTab") != fdm.views.BottomPanelTab.Trackers) return;

			var currentItem = this.model.get("currentItem");

			if(!currentItem || currentItem.get("downloadType") != fdm.models.DownloadType.Trt){
				return;
			}

			this.downloadsCtrl.getDownloadPropertiesPromise(currentItem.get("id"), ["trackers"])
				.then(_.bind(this.setTrackers, this));

            //if(currentItem && currentItem.get("downloadType") == fdm.models.DownloadType.Trt){
			//	this.downloadsCtrl.extractPartialModelByIdAsync("refreshTrackers", currentItem.get("id"), [], ["trackers"], true, function(partialModel){
			//		var currentItem = this.model.get("currentItem");
            //
			//		if(partialModel.id != currentItem.get("id")){
			//			return;
			//		}
            //
			//		var trackers = [];
			//		if(partialModel && partialModel.trackers){
			//			trackers = partialModel.trackers;
			//			trackers.sort(function(a,b){
			//				return String.naturalCompare(a.url.toLowerCase(), b.url.toLowerCase());
			//			});
			//		}
			//		this.model.get("trackers").reset(trackers);
			//	}.bind(this));
			//}
		},

		onItemPriorityChanged: function(item_id){

			if(this.model.get("currentItemId") == item_id)
				this.refreshFiles();
		},

		setBatchDownloads: function(downloads){

			this.collections.downloads.set(downloads, {silent: true});
			this.collections.downloads.trigger('reset');
		},

		setFiles: function(data){

			if(!this.model.get("currentItem") || data.id != this.model.get("currentItem").get("id")){
				return;
			}

			var fileTree = [];
			if(data){
				fileTree = data.files;//currentItem.get("files");
				//console.time("build file tree");
				if (typeof fileTree == "string"){
					fileTree = JSON.parse(fileTree);//fdm.fileUtils.fileListToFileTree(plainFiles);
				}
				//console.timeEnd("build file tree");
			}

			var current_item = this.model.get("currentItem");
			var trees = {
				downloadId: current_item.id,
				name: 'root',
				children: [],
				type: 'bottom_panel'
			};
			if (fileTree.length){
				trees.children = fileTree;
			}

			var current_tree = this.model.get("fileTree");
			if (current_tree.get('downloadId') != trees.downloadId){
				current_tree.get('selectedList').reset([]);
				current_tree.get('openedFolders').reset([]);
			}

			var tree_model = new fdm.models.FileTree(trees, {silent: true});
			tree_model.setPriorityChecked();

			this.model.set("fileTree", tree_model);
		},

        onItemDeleted: function() {
        	this.refreshBatchDownloadsCount();
        	this.needRefreshFiles(200);
		},

		refreshFilesTimeout: 0,

		needRefreshFiles: function(timer){

            timer = timer || 3000;

			if (this.refreshFilesTimeout > 0)
				return;

			this.refreshFilesTimeout = setTimeout(this.refreshFiles.bind(this), timer);
		},

		refreshProgressMapTimeout: 0,

        needRefreshProgressMap: function(timer){

            timer = timer || 3000;

			if (this.refreshProgressMapTimeout > 0)
				return;

			this.refreshProgressMapTimeout = setTimeout(this.refreshProgressMap.bind(this), timer);
		},

		changeDownloadType: function(){

			var item = this.model.get('currentItem');
			var item_download_type = null;
			if (item){
				item_download_type = item.get('downloadType');
			}

			this.model.set({
				currentItemDownloadType: item_download_type
			});

			this.refreshLogs()
		},

        refreshBatchDownloadsCount: function() {
            const currentItem = this.model.get("currentItem");
            if (currentItem && currentItem.get('downloadType') === fdm.models.DownloadType.batchDownload) {
                let item_id = currentItem.get("id");
                this.downloadsCtrl.getBatchDownloadIds(item_id)
                    .then(function(d_ids){
                        const currentItem2 = this.model.get("currentItem");
                        if (currentItem2 && currentItem2.get('id') === item_id && d_ids && d_ids.length) {
                            this.model.set("batchDownloadsCount", d_ids.length);
						}
                    }.bind(this))
                    .catch(function(){
                        console.error('getBatchDownloadIds error', arguments);
					}.bind(this));
			} else {
                this.model.set("batchDownloadsCount", 0);
			}
		},

		refreshFiles: function() {

			if (this.refreshFilesTimeout){
				clearTimeout(this.refreshFilesTimeout);
				this.refreshFilesTimeout = 0;
			}

            if(this.model.get('currentTab') != fdm.views.BottomPanelTab.Files) return;

			var currentItem = this.model.get("currentItem");
            var current_tree = this.model.get("fileTree");

            if(!currentItem || current_tree.get('downloadId') != currentItem.id){
                this.model.set("fileTree", new fdm.models.FileTree({type: 'bottom_panel'}));
            }

			if(!currentItem){
				return;
			}

			if (currentItem.get('downloadType') == fdm.models.DownloadType.batchDownload){

				this.downloadsCtrl.getAllBatchDownloads(currentItem.get("id"))
					.then(this.setBatchDownloads)
					.catch(_.partial(this.setBatchDownloads, []));
			}
			else{

				this.downloadsCtrl.getDownloadPropertiesPromise(currentItem.get("id"), ["files"])
					.then(_.bind(this.setFiles, this));
			}

			/*
			this.downloadsCtrl.extractPartialModelByIdAsync("refreshFiles", currentItem.get("id"), ["files"], [], true, function(data){
				if(!this.model.get("currentItem") || data.id != this.model.get("currentItem").get("id")){
					return;
				}
				try{
					var fileTree = [];
					//console.timeEnd("get file list");
					if(data){
						var fileTree = data.files;//currentItem.get("files");
						//console.time("build file tree");
						if (typeof fileTree == "string"){
							fileTree = JSON.parse(fileTree);//fdm.fileUtils.fileListToFileTree(plainFiles);
						}
						//console.timeEnd("build file tree");
					}
                    if(fileTree.length){
                        // Backbone sometimes does not fire 'change:fileTree' event because it thinks data has not changed.
                        // Force fire 'change:fileTree' event with new timestamp value.
                        fileTree[0].timestamp = Date.now();
                    }

                    var current_item = this.model.get("currentItem");
                    var trees = {
                        downloadId: current_item.id,
                        name: 'root',
                        children: [],
                        type: 'bottom_panel'
                    };
                    if (fileTree.length){
                        trees.children = fileTree;
                    }

                    if (current_tree.get('downloadId') != trees.downloadId){
                        current_tree.get('selectedList').reset([]);
                        current_tree.get('openedFolders').reset([]);
                    }

					var tree_model = new fdm.models.FileTree(trees);
					tree_model.setPriorityChecked();

                    this.model.set("fileTree", tree_model);
				}
				catch(e){
					console.error("refreshFiles error:%o, data: %o", e, data);
				}
			}.bind(this));
			*/
		},
		onLogMessagesHandler: function(itemId, messages) {

			var ci = this.model.get('currentItem');

			if (!ci || ci.get('id') != itemId){
				return;
			}

			messages = _.isString(messages) ? JSON.parse(messages) : messages;

			var logs = this.model.get("logs");
			for (var i = 0; i < messages.length; ++i) {
				var m = moment(new Date(messages[i].time * 1000));
				logs.push({text: messages[i].text, datetime: m.format('llll') });
			}

			//logs.add(models, {silent: true});
			//logs.trigger('add');
			//logs = logs.concat(models);
			this.model.set({logs: logs}, {silent: true});
			this.model.trigger('change:logs');
		},
		refreshLogs: function() {
			var currentItem = this.model.get("currentItem");
			var current_tab = this.model.get("currentTab");

			this.model.set('logs', []);
			if (current_tab == fdm.views.BottomPanelTab.Log && currentItem
				&& currentItem.get('downloadType') != fdm.models.DownloadType.InfoRequest)
				fdmApp.downloads.listenToLogUpdates(currentItem.get('id'));
			else
				fdmApp.downloads.listenToLogUpdates(-1);
		},
		refreshThumb: function(priority, force) {
			var currentItem = this.model.get("currentItem");
            var thumb =  this.model.get("thumb");

            if (currentItem == null || thumb.downloadId != currentItem.get("id")){
                thumb.src = false;
                this.model.set("thumb", thumb);
            }

			this.model.set("showThumbnail", this.model.get("mainViewName") == "downloads-compact-view" && currentItem != null);

if(window.logBottomThumbnail) console.time("generate");
			if(this.model.get("showThumbnail") == true){
				var id = currentItem.get("id");
				//fdmApp.system.generateFilePreview("bottom", id, 260, 260, priority, !!force, true);
			}
		},
		applyFilePreview: function(itemId) {
			//if(target != "bottom") return;

            var currentItem = this.model.get("currentItem");
            if(!currentItem || currentItem.get("id") != itemId) {
                // selection is changed while preview was being generated
                //this.model.set("showThumbnail", false);
                return;
            }
            //var thumb = {
            //    downloadId: itemId,
            //    maxWidth: width,
            //    maxHeight: height,
            //    src: imgPath
            //};
            //this.model.set("thumb", thumb);
			currentItem.set("thumbCache", currentItem.get('thumbCache') + 1);
		},
		refreshDHTNodes: function(){

			if(this.model.get("currentTab") != fdm.views.BottomPanelTab.General) return;
            var item = this.model.get("currentItem");
            if (!item || item.get('downloadType') != fdm.models.DownloadType.Trt)
                return;

            var currentDHTN = this.model.get('dhtNodes');
			this.apiDownloads.getDHTNodes(function(dhtNodes){
				this.model.set("dhtNodes", dhtNodes);
			}.bind(this));
		},

		subscribeCurrentItem: function(){
			var item = this.model.get("currentItem");
			if (item) {

                item.on("change:progress change:state change:priorityChanged", this.refreshFiles, this);
                item.on("change:downloadedBytes", this.needRefreshFiles, this);
                item.on("change:downloadedBytes change:state", this.needRefreshProgressMap, this);
                item.on("change:downloadType", this.changeDownloadType, this);
                //item.get('logs').on("add remove reset", this.refreshLogs, this);
			}
		},
		unsubscribeCurrentItem: function(){
			var item = this.model.get("currentItem");
			if (item) {
				item.off("change:progress change:state change:priorityChanged", this.refreshFiles, this);
				item.off("change:downloadedBytes", this.needRefreshFiles, this);
				item.off("change:downloadedBytes change:state", this.needRefreshProgressMap, this);
				item.off("change:downloadType", this.changeDownloadType, this);
                //item.get('logs').off("add remove reset", this.refreshLogs, this);
			}
		},
		showFilesPopupMenu: function(fileIndex) {
			var item = this.model.get("currentItem");
			if ( item && ( fileIndex != null ) ){
				if ( window.fdmApp.menuManager.showPopupMenu ) {
					window.fdmApp.menuManager.showFilesPopupMenu(item.id, fileIndex);
				} 
			}
		},
		launchFile: function(file_index) {

			var item = this.model.get("currentItem");

			if (!item)
				return;

			var download_id = item.get('id');
			if ( download_id && file_index >= 0 ){
				fdmApp.downloads.launchFile(download_id, file_index);
			}
		},
		setPriority: function(file_indexes, new_priority) {

			var item = this.model.get("currentItem");

			if (!item)
				return;

			var download_id = item.get('id');
			if ( download_id && file_indexes.length ){
				fdmApp.downloads.prioritizeNode(download_id, file_indexes, new_priority, function(){
					this.refreshFiles();
				}.bind(this));
			}
		},

		changeTab: function(new_tab_id){
			this.model.set('currentTab', new_tab_id);
		},

		showToggle: function() {
			if (this.model.get('panelVisible')) {
				this.hide();
			}
			else {
				this.show();
			}
		},

		show: function() {
			$("body").addClass("bottom-row-shown");

			var item_visible = false;
			var current_item_id = false;

			var item = this.model.get("currentItem");
			if (item) {

				current_item_id = item.get('id');
				item_visible = app.controllers.downloads.view_model.itemInVisiblePosition(current_item_id);
			}
			this.model.set({panelVisible: true});
			app.appViewManager.setBottomPanelState('visible', true);

			if (item_visible && current_item_id){
				_.defer(function(){
					app.controllers.downloads.view_model.scrollToItemByIdOverView(current_item_id);
				});
			}
		},

		hide: function() {
			$("body").removeClass("bottom-row-shown");
			//$('#scrolling-list, #right-panel').css("height", '100%');
			//$('#bottom-panel').css("height", 0);
			//this.panelVisible = false;
			this.model.set({panelVisible: false});
			app.appViewManager.setBottomPanelState('visible', false);
		},

		removeTag: function (data) {
			if( parseInt(data.id) < 100 ){
				return;
			}
			var current_item = this.model.get('currentItem');
			var id = false;
			if (current_item)
				id = current_item.get('id');
			if (id)
				app.controllers.downloads.deleteTagItemById(id, data.id);
		}
	};
	return Class;
})();

