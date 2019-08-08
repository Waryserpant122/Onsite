
BottomPanel = React.createClass({displayName: "BottomPanel",

    dispatcherIndex: false,

    getInitialState: function() {

        var view_state = app.appViewManager.getBottomPanelState();
        var bottom_panel_height = view_state && view_state.height ? view_state.height : fdm.viewModels.BottomPanel_MinHeight;
        bottom_panel_height = this.normalizeBottomHeight(bottom_panel_height);

        return {
            showSelectedList: false && app.controllers.downloads.model.get('countSelected') > 0,
            currentItemId: app.controllers.bottomPanel.model.get('currentItemId'),
            currentItemDownloadType: app.controllers.bottomPanel.model.get('currentItemDownloadType'),
            currentTab: app.controllers.bottomPanel.model.get('currentTab'),
            bottomPanelHeight: bottom_panel_height,
            panelVisible: app.controllers.bottomPanel.model.get('panelVisible'),
            downloadsMouseSlideInProgress: false,
            batchDownloadsCount: app.controllers.bottomPanel.model.get('batchDownloadsCount'),
        };
    },

    componentDidMount: function(){

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){

                if (payload.action.actionType == 'downloadsMouseSlideStart')
                    this.downloadsMouseSlideStart(payload.action.content);
                if (payload.action.actionType == 'downloadsMouseSlideEnd')
                    return this.downloadsMouseSlideEnd(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

        _.bindAll(this, 'onResize', 'mouseUpResize', 'mouseMoveResize');

        window.addEventListener('resize', this.onResize);
        //app.controllers.downloads.model.on('change:countSelected', this.changeSelected, this);
        app.controllers.bottomPanel.model.on('change:currentItemId change:currentItemDownloadType', this.changeCurrentItem, this);
        app.controllers.bottomPanel.model.on('change:currentTab', this.changeCurrentTab, this);
        app.controllers.bottomPanel.model.on('change:panelVisible', this.changePanelVisible, this);
        app.controllers.bottomPanel.model.on('change:batchDownloadsCount', this.changeBatchDownloadsCount, this);
    },

    componentWillUnmount:function(){

        window.removeEventListener('resize', this.onResize);
        //app.controllers.downloads.model.off('change:countSelected', this.changeSelected, this);
        app.controllers.bottomPanel.model.off('change:currentItemId change:currentItemDownloadType', this.changeCurrentItem, this);
        app.controllers.bottomPanel.model.off('change:currentTab', this.changeCurrentTab, this);
        app.controllers.bottomPanel.model.off('change:panelVisible', this.changePanelVisible, this);
        app.controllers.bottomPanel.model.off('change:batchDownloadsCount', this.changeBatchDownloadsCount, this);

        FdmDispatcher.unregister(this.dispatcherIndex);
    },

    downloadsMouseSlideStart: function(){

        this.setState({downloadsMouseSlideInProgress: true});
    },
    downloadsMouseSlideEnd: function(){

        this.setState({downloadsMouseSlideInProgress: false});
    },

    changePanelVisible: function(){

        this.setState({panelVisible: app.controllers.bottomPanel.model.get('panelVisible')});
    },

    changeBatchDownloadsCount: function(){

        this.setState({batchDownloadsCount: app.controllers.bottomPanel.model.get('batchDownloadsCount')});
    },

    onResize: function(){

        this.fixPanelHeight();
    },

    fixPanelHeight: function(){

        var height = this.state.bottomPanelHeight;
        height = this.normalizeBottomHeight(height);

        if (height != this.state.bottomPanelHeight){

            app.appViewManager.setBottomPanelState('height', height);
            this.setState({bottomPanelHeight: height});

            FdmDispatcher.handleViewAction({
                actionType: 'changeBottomPanelHeight',
                content: {}
            });
        }
    },

    normalizeBottomHeight: function(height) {

        var min = 150;

        var topMin = 350;
        var bodyHeight = window.innerHeight;

        if(height < min) {
            height = min;
        }
        if((bodyHeight - height) < topMin) {
            height = bodyHeight - topMin;
        }

        return height;
    },

    mouseMoveResize: function(e){

        if (e.pageY == this.startPageY)
            return;

        this.startPageY = 0;

        var traffic_bar_height = 31;
        var bottom_panel_margin = 15;
        var height =  window.innerHeight - e.pageY - traffic_bar_height - bottom_panel_margin;
        height = this.normalizeBottomHeight(height);

        if (height != this.state.bottomPanelHeight){

            app.appViewManager.setBottomPanelState('height', height);
            this.setState({bottomPanelHeight: height});

            FdmDispatcher.handleViewAction({
                actionType: 'changeBottomPanelHeight',
                content: {}
            });
        }
    },

    mouseUpResize: function(e){

        this.startPageY = 0;

        window.removeEventListener('mousemove', this.mouseMoveResize);
        window.removeEventListener('mouseup', this.mouseUpResize);

        this.setState({bottomPanelResizing: false});

    },

    startPageY: 0,
    startResizing: function(e){
        stopEventBubble(e);

        this.startPageY = e.pageY;

        window.addEventListener('mousemove', this.mouseMoveResize);
        window.addEventListener('mouseup', this.mouseUpResize);

        this.setState({bottomPanelResizing: true});

    },

    //changeSelected: function(){
    //    this.setState({showSelectedList: false && app.controllers.downloads.model.get('countSelected') > 0});
    //},

    //currentItemOnChange: function(){
    //    _.defer(function(){
    //        this.setState({
    //            currentItem: this.state.currentItem
    //        });
    //    }.bind(this));
    //},

    changeCurrentItem: function(){

        this.setState({
            currentItemId: app.controllers.bottomPanel.model.get('currentItemId'),
            currentItemDownloadType: app.controllers.bottomPanel.model.get('currentItemDownloadType')
        });
    },

    changeCurrentTab: function(){

        this.setState({
            currentTab: app.controllers.bottomPanel.model.get('currentTab')
        });
    },

    hideBottomPanel: function(){
        app.controllers.bottomPanel.hide();
    },

    changeTab: function(new_tab, e){
        app.controllers.bottomPanel.changeTab(new_tab);
    },

    render: function() {

        if (!this.state.currentItemId)
            return null;

        var current_tab = this.state.currentTab;
        var is_trt = this.state.currentItemDownloadType == fdm.models.DownloadType.Trt;
        var is_batch = this.state.currentItemDownloadType == fdm.models.DownloadType.batchDownload;
        var is_youtube = this.state.currentItemDownloadType == fdm.models.DownloadType.YouTubeVideo;

        var files_count = 0;
        if (is_batch) {
            files_count = this.state.batchDownloadsCount;
        }

        return (

            React.createElement("div", {id: "bottom-panel", style: {
            height: this.state.bottomPanelHeight + 'px',
            display: this.state.panelVisible ? 'block' : 'none'
            }, className: "bottom-row"}, 

                React.createElement("div", {id: "bottom_trsprnt_scroll", className: "bottom_trsprnt_scroll", 
                     style: {
                        height: this.state.bottomPanelHeight + 47 + 'px',
                        display: this.state.downloadsMouseSlideInProgress ? 'block' : 'none'
                     }}), 

                React.createElement("div", {id: "top_trsprnt_scroll", className: "top_trsprnt_scroll", style: {
                    display: this.state.downloadsMouseSlideInProgress ? 'block' : 'none'
                }}), 

                this.state.bottomPanelResizing ?
                    React.createElement("div", {className: "modal-dialog-layer", 
                         style: {cursor: 'ns-resize !important'}})
                    : null, 

                React.createElement("div", {className: "bottom-row-header"}, 

                    !this.state.downloadsMouseSlideInProgress ?
                        React.createElement("div", {onMouseDown: this.startResizing, 
                            className: "resize-block bottom-panel-dragger"})
                        : null, 

                     !this.state.showSelectedList && this.state.currentItemId ?
                        React.createElement("ul", {className: "tabs"}, 
                            React.createElement("li", {className: rjs_class({general: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.General}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.General)}, __('General')), 
                            React.createElement("li", {style: {display: is_batch || is_youtube || this.state.currentItemDownloadType == fdm.models.DownloadType.InfoRequest ? 'none' : 'inline-block'}, 
                                className: rjs_class({progress: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.Progress}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.Progress)}, __('Progress')), 
                            React.createElement("li", {style: {display: is_trt || is_batch ? 'inline-block' : 'none'}, 
                                className: rjs_class({files: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.Files}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.Files)}, __('Files') + (files_count > 0 ? ' (' + files_count + ')' : '')), 
                            React.createElement("li", {style: {display: is_trt ? 'inline-block' : 'none'}, 
                                className: rjs_class({trackers: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.Trackers}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.Trackers)}, __('Trackers')), 
                            React.createElement("li", {style: {display: is_trt ? 'inline-block' : 'none'}, 
                                className: rjs_class({peers: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.Peers}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.Peers)}, __('Peers')), 
                            React.createElement("li", {className: rjs_class({log: true, 'tab-active': current_tab == fdm.views.BottomPanelTab.Log}), 
                                onMouseDown: _.partial(this.changeTab, fdm.views.BottomPanelTab.Log)}, __('Log'))
                        )
                        : null, 

                    React.createElement("div", {className: "close-icon", title: __('Hide details'), onClick: this.hideBottomPanel})
                ), 

                /*this.state.showSelectedList ?

                    <BottomPanelSelectedList />

                    :

                    (this.state.currentItem ?

                        <BottomPanelCurrentItem key={this.state.currentItem.id}
                            bottomPanelHeight={this.state.bottomPanelHeight}
                            currentItem={this.state.currentItem}/>

                        :

                        <div className="no-item-selected">
                            <span>Select an item to view advanced information</span>
                        </div>)
                */

                React.createElement(BottomPanelCurrentItem, {bottomPanelHeight: this.state.bottomPanelHeight})

            )

        );
    }

});

/*
BottomPanelSelectedList = React.createClass({

    getInitialState: function() {

        return {
            showSelectedList: app.controllers.downloads.model.get('countSelected') > 0
        };
    },

    componentDidMount: function(){

        app.controllers.downloads.model.on('change:countSelected', this.changeSelected, this);

    },

    componentWillUnmount:function(){

        app.controllers.downloads.model.off('change:countSelected', this.changeSelected, this);
    },

    changeSelected: function(){

        this.setState({showSelectedList: app.controllers.downloads.model.get('countSelected') > 0});
    },

    render: function() {



        return (

            <div>{__('selected')}</div>

        );

    }


});
*/


var BottomPanelCurrentItem = React.createClass({displayName: "BottomPanelCurrentItem",

    getInitialState: function() {

        return {

            currentItem: app.controllers.bottomPanel.model.get('currentItem'),

            //fileTree: app.controllers.bottomPanel.model.get('fileTree'),
            trackers: app.controllers.bottomPanel.model.get('trackers').models,
            peers: app.controllers.bottomPanel.model.get('peers').models,
            //logs: app.controllers.bottomPanel.model.get('logs').models,

            thumb: app.controllers.bottomPanel.model.get('thumb'),
            dhtNodes: app.controllers.bottomPanel.model.get('dhtNodes'),
            currentTab: app.controllers.bottomPanel.model.get('currentTab'),
            sortOptions: app.controllers.bottomPanel.model.get('sortOptions'),
            thumbMaxWidth: this.calcThumbMaxWidth(),

            bottomPanelResizing: false,
            filesColumnSize: {
                fileName: 460,
                fileSize: 100,
                fileProgress: 147,
                filePriority: 121
            },
            trackersColumnSize: {
                trackerName: 150,
                trackerStatus: 85,
                trackerUpdateIn: 117
            },
            peersColumnSize: {
                peerIp: 128,
                peerClient: 145,
                peerFlags: 121,
                peerPercents: 40,
                peerSpeed: 150,
                //peerDownSpeed: 95,
                //peerUpSpeed: 95,
                peerReqs: 65,
                peerDownloaded: 85,
                peerUploaded: 85
            }
        };
    },

    componentDidMount: function(){

        _.bindAll(this, 'onResize');

        _.defer(this.onResize);
        window.addEventListener('resize', this.onResize);

        app.controllers.bottomPanel.model.on('change:currentTab', this.changeCurrentTub, this);
        app.controllers.bottomPanel.model.on('change:thumb change:thumbCache change:dhtNodes change:sortOptions', this.changeModel, this);

        app.controllers.bottomPanel.model.on('change:currentTab change:panelVisible', this.fixTableWidths, this);

        //app.controllers.bottomPanel.model.on('change:fileTree', this.newFileTree, this);
        //app.controllers.bottomPanel.model.get('fileTree').on('change', this.changeFileTree, this);

        app.controllers.bottomPanel.model.on('change:currentItem', this.newCurrentItem, this);
        var currentItem = app.controllers.bottomPanel.model.get('currentItem');
        if (currentItem)
            currentItem.on('change', this.changeCurrentItem, this);

        app.controllers.bottomPanel.model.get('trackers').on('add reset remove', this.changeTrackers, this);
        app.controllers.bottomPanel.model.get('peers').on('add reset remove', this.changePeers, this);
        //app.controllers.bottomPanel.model.get('logs').on('add reset remove', this.changeLogs, this);
    },

    componentWillUnmount:function(){

        window.removeEventListener('resize', this.onResize);

        app.controllers.bottomPanel.model.off('change:currentTab', this.changeCurrentTub, this);
        app.controllers.bottomPanel.model.off('change:thumb change:thumbCache change:dhtNodes change:sortOptions', this.changeModel, this);

        app.controllers.bottomPanel.model.off('change:currentTab change:panelVisible', this.fixTableWidths, this);

        if (this.state.currentItem)
            this.state.currentItem.off('change', this.changeCurrentItem, this);
        app.controllers.bottomPanel.model.off('change:currentItem', this.newCurrentItem, this);

        //app.controllers.bottomPanel.model.off('change:fileTree', this.newFileTree, this);
        //if (this.state.fileTree)
        //    this.state.fileTree.off('change', this.changeFileTree, this);

        app.controllers.bottomPanel.model.get('trackers').off('add reset remove', this.changeTrackers, this);
        app.controllers.bottomPanel.model.get('peers').off('add reset remove', this.changePeers, this);
        //app.controllers.bottomPanel.model.get('logs').off('add reset remove', this.changeLogs, this);
    },

    calcThumbMaxWidth: function(){

        return window.innerWidth - 670;
    },

    changeCurrentTub: function(){

        this.setState({
            currentTab: app.controllers.bottomPanel.model.get('currentTab')
        });
    },

    changeModel: function(){
        this.setState({
            thumb: app.controllers.bottomPanel.model.get('thumb'),
            dhtNodes: app.controllers.bottomPanel.model.get('dhtNodes'),
            sortOptions: app.controllers.bottomPanel.model.get('sortOptions')
        });
    },

    changeCurrentItem: function(){

        //if (this.state.currentTab != fdm.views.BottomPanelTab.General)
        //    return;

        if (this.isMounted())
            this.setState({currentItem: app.controllers.bottomPanel.model.get('currentItem')});
    },
    newCurrentItem: function(){

        if (this.state.currentItem)
            this.state.currentItem.off('change', this.changeCurrentItem, this);

        var currentItem = app.controllers.bottomPanel.model.get('currentItem');
        if (currentItem)
            currentItem.on('change', this.changeCurrentItem, this);

        this.setState({currentItem: currentItem});
    },

    //changeFileTree: function(){
    //
    //    this.setState({fileTree: app.controllers.bottomPanel.model.get('fileTree')});
    //},
    //newFileTree: function(){
    //    if (this.state.fileTree)
    //        this.state.fileTree.off('change', this.changeFileTree, this);
    //
    //    var file_tree = app.controllers.bottomPanel.model.get('fileTree');
    //    file_tree.on('change', this.changeFileTree, this);
    //    this.setState({fileTree: file_tree});
    //},

    changeTrackers: function(){

        this.setState({trackers: app.controllers.bottomPanel.model.get('trackers').models});
    },
    changePeers: function(){

        this.setState({peers: app.controllers.bottomPanel.model.get('peers').models});
    },
    //changeLogs: function(){
    //
    //    this.setState({logs: app.controllers.bottomPanel.model.get('logs')});
    //},

    onResize: function(){

        this.fixTableWidths();

    },

    columnMinSize: {

        filesColumnSize: {
            fileName: 136,
            fileSize: 68,
            fileProgress: 130,
            // filePriority: 67,
            filePriority: 134
        },
        trackersColumnSize: {
            trackerName: 150,
            trackerStatus: 85,
            trackerUpdateIn: 117
        },
        peersColumnSize: {
            peerIp: 128,
            peerClient: 145,
            peerFlags: 121,
            peerPercents: 40,
            peerSpeed: 150,
            //peerDownSpeed: 95,
            //peerUpSpeed: 95,
            peerReqs: 65,
            peerDownloaded: 85,
            peerUploaded: 85
        }
        //logsColumnSize: {
        //    logDate: 96,
        //    logTime: 61,
        //    logInformation: 100
        //}
    },

    fixTableWidths: function(){

        var a = {
            filesColumnSize: this.state.filesColumnSize,
            trackersColumnSize: this.state.trackersColumnSize,
            peersColumnSize: this.state.peersColumnSize
            //logsColumnSize: this.state.logsColumnSize
        };

        var width = false;
        var bp = document.getElementById('bottom-panel');
        if (bp && bp.firstElementChild && bp.firstElementChild.offsetWidth)
            width = document.getElementById('bottom-panel').firstElementChild.offsetWidth;

        if (!width || width < 100){
            width = window.innerWidth - 22;
        }
        var current_width = width - 25; //15 for scroll

        var new_state = {};
        for (var group in a){

            var table_size = a[group];
            var min_size;
            var new_size;

            var sum = _.values(table_size).reduce(function(a, b) {return a + b});

            var k = current_width / sum;

            var added_size = 0;
            for (var i in table_size){
                new_size = Math.floor(table_size[i] * k);

                min_size = this.columnMinSize[group][i];

                if (min_size > new_size){
                    added_size += min_size - new_size;
                    new_size = min_size;
                }
                table_size[i] = new_size
            }

            if (added_size > 0){

                var keys = _.keys(table_size);
                var key;
                while ((key = keys.pop()) && added_size > 0){
                    min_size = this.columnMinSize[group][key];
                    new_size = table_size[key] - added_size;
                    if (new_size < min_size){
                        var diff = table_size[key] - min_size;
                        table_size[key] = min_size;
                        added_size -= diff;
                    }
                    else{
                        table_size[key] = table_size[key] - added_size;
                        added_size = 0;
                    }
                }
            }

            new_state[group] = table_size;
        }

        new_state.thumbMaxWidth = this.calcThumbMaxWidth();

        this.setState(new_state);
    },

    resizeColumn: function(group, e){

        e.preventDefault();
        var resizer = e.target;

        var current_raw_column = resizer.parentNode;
        var current_column_id = current_raw_column.getAttribute('data-resizeid');
        var start_width = parseInt(current_raw_column.offsetWidth);
        var start_page_x = e.pageX;

        var table = resizer.parentNode.parentNode;
        //var table_width = parseInt(table.offsetWidth);

        var raw_columns = resizer.parentNode.parentNode.childNodes;

        var columns = {};
        var left_columns = {};
        var right_columns = {};

        var left = true;
        var i;
        for (i = 0; i < raw_columns.length; i++){
            var k = raw_columns[i];

            var k_id = k.getAttribute('data-resizeid');

            var r = {
                id: k_id,
                current: false,
                column: k,
                start_width: k.offsetWidth,
                min_width: this.columnMinSize[group][k_id]
            };

            if (k == current_raw_column)
            {
                r.current = true;
                left = false;
            }
            else{
                if (left)
                    left_columns[k_id] = r;
                else
                    right_columns[k_id] = r;
            }
            columns[k_id] = r;
        }

        $(document).mousemove(_.bind(function(e){

            var diff = e.pageX - start_page_x;

            if (columns[current_column_id].start_width + diff < columns[current_column_id].min_width)
                diff = columns[current_column_id].min_width - columns[current_column_id].start_width;


            var diff_tmp = diff;

            var count_right = _.size(right_columns);

            var max_diff_available = 0;
            var needed_right_changes = diff_tmp/count_right;
            for (i in right_columns){
                k = right_columns[i];

                max_diff_available += k.start_width - k.min_width;

                if (k.start_width - needed_right_changes < k.min_width){
                    diff_tmp = diff_tmp - (k.start_width - k.min_width);
                    count_right--;
                    needed_right_changes = diff_tmp/count_right;
                }
            }

            var real_diff = 0;
            var new_state = this.state[group];

            for (i in right_columns){
                k = right_columns[i];

                if (k.start_width - needed_right_changes < k.min_width){
                    new_state[k.id] = k.min_width;
                    real_diff += k.start_width - k.min_width;
                }
                else{
                    real_diff += needed_right_changes;
                    new_state[k.id] = k.start_width - needed_right_changes;
                }
            }

            new_state[current_column_id] = start_width + real_diff;

            var ss = {};
            ss[group] = new_state;

            this.setState(ss);

        }, this));

        $(document).mouseup(function(e){
            $(document).unbind('mousemove');
        });

    },

    setSeeding: function(e){

        var new_value = e.target.checked;
        fdmApp.downloads.setSeeding(this.state.currentItem.id, new_value);

    },

    peerProgress: function(peer) {

        if (peer.flags & 0xC0) // connecting
            return '';

        return (peer.progress * 100).toFixed();

    },

    peerStatus: function(peer) {

        // http://superuser.com/q/415185

        var flags = peer.flags;
        var halfOpen = flags & 0x80;
        var handshake = flags & 0x40;
        var queued = flags & 0x100;
        var snubbed = flags & 0x1000;

        if (halfOpen || handshake)
            return 'Connecting...';
        if (queued)
            return 'Queued';

        var interested = flags & 0x1;
        var choked = flags & 0x2;
        var remoteInterested = flags & 0x4;
        var remoteChoked = flags & 0x8;
        var optimisticUnchoke = flags & 0x800;
        var holepunched = flags & 0x8000;
        var i2p = flags & 0x10000;
        var utp = flags & 0x20000;
        var ssl = flags & 0x40000;
        var rc4Encrypted = flags & 0x100000;
        var plaintextEncrypted = flags & 0x200000;

        var source = peer.source;
        var dht = source & 0x2;
        var pex = source & 0x4;
        var lsd = source & 0x8;
        var incoming = source & 0x20;

        var result = '';

        if (interested)
            result += remoteChoked ? 'd' : 'D';
        else
            result += '  ';

        if (remoteInterested)
            result += choked ? 'u' : 'U';
        else
            result += '  ';

        if (snubbed)
            result = 'S';

        result += ' ';

        if (holepunched)
            result += 'h';

        if (optimisticUnchoke)
            result += 'O';

        if (utp)
            result += 'P';

        if (ssl || rc4Encrypted || plaintextEncrypted)
            result += 'E';

        if (dht)
            result += 'H';

        if (pex)
            result += 'X';

        if (lsd)
            result += 'L';

        if (incoming)
            result += 'I';

        if (i2p)
            result += ' [I2P]';

        return result;
//        return '0x' + peer.flags.toString(16);
    },

    tagContextMenu: function(tag, e){

        console.error(tag);
    },

    browse: function(url){

        fdmApp.system.browseUrl(url);
    },

    sort: function (sort_column) {

        app.controllers.bottomPanel.sort(sort_column);
    },

    render: function() {

        if (!this.state.currentItem)
            return null;

        var current_item = this.state.currentItem.toJSON();
        var current_tab =  this.state.currentTab;

        var thumb = this.state.thumb;

        var is_trt = current_item.downloadType == fdm.models.DownloadType.Trt;
        var requesting_info = current_item.downloadType == fdm.models.DownloadType.InfoRequest;
        var trackers = this.state.trackers;
        var logs = this.state.logs;
        var peers = this.state.peers;

        var moving_progress = current_item.isMoving;// && current_item.moveProgress > 0;

        var state = current_item.state;
        if (current_item.missingFiles && !current_item.missingStorage)
            state = fdm.models.DownloadState.Error;

        var current_process = __(current_item.status);
        if (state == fdm.models.DownloadState.Completed && is_trt && current_item.seedingEnabled)
            current_process = __('Seeding');
        if (state == fdm.models.DownloadState.WaitingForMetadata)
            current_process = __('Downloading metadata');
        if (state == fdm.models.DownloadState.FileProcessing)
            current_process = __('Merging media streams');
        //if (state == fdm.models.DownloadState.Allocating)
        //    current_process = 'Allocating disk space...';
        if (state == fdm.models.DownloadState.Pausing)
            current_process = __('Paused');
        if (state == fdm.models.DownloadState.Downloading)
            current_process = '';
        if (state == fdm.models.DownloadState.Checking)
        {
            if (current_item.checking == fdm.models.CheckingState.Allocating)
                current_process = __('Allocating disk space...');
            else if (current_item.checking == fdm.models.CheckingState.ResumeData)
                current_process = __('Checking resume data...');
            else if (current_item.checking == fdm.models.CheckingState.Queued)
                current_process = __('Queued for checking...');
            else
                current_process = __('Checking files...') + ' ' + (current_item.checkingProgress >= 0 ? current_item.checkingProgress +'%' : '');
        }
        if (state == fdm.models.DownloadState.Downloading && current_item.isPreallocating)
            current_process = __('Downloading, allocating disk space...');
        if (requesting_info)
            current_process = __('Requesting download info...');
        if (moving_progress)
            current_process = __('Moving') + '... ' + (current_item.moveProgress >= 0 ? current_item.moveProgress +'%' : '');

        var filesColumnSize = this.state.filesColumnSize;
        var trackersColumnSize = this.state.trackersColumnSize;
        var peersColumnSize = this.state.peersColumnSize;
        //var logsColumnSize = this.state.logsColumnSize;

        var unknown_size = current_item.totalBytes < 0
        || state == fdm.models.DownloadState.FileProcessing
        || state == fdm.models.DownloadState.Checking && (
                current_item.checking == fdm.models.CheckingState.Allocating
                || current_item.checking == fdm.models.CheckingState.ResumeData
                || current_item.checking == fdm.models.CheckingState.Queued
            )
        //|| state == fdm.models.DownloadState.Allocating
        || state == fdm.models.DownloadState.Reconnecting
        || current_item.isMoving && !moving_progress || requesting_info;

        var comment = current_item.comment;
        if (is_trt && current_item.comment && current_item.comment != ''){
            comment = comment.replace(/(http:\/\/|https:\/\/)(\S*)(\s|$)/ig, '<a href="$1$2" onclick="fdmApp.system.browseUrl(\'$1$2\')">$1$2</a> ');
        }

        var remaining_time = this.state.currentItem.getRemainingTime();


        var error_text = this.state.currentItem.getErrorText();

        // var show_error = false;
        // if (current_item.missingFiles || current_item.missingStorage ||
        //     !moving_progress && (state==fdm.models.DownloadState.Error || state==fdm.models.DownloadState.Reconnecting))
        //     show_error = true;

        var show_error = state==fdm.models.DownloadState.Error || current_item.missingFiles || current_item.missingStorage;

        if (!show_error && state === fdm.models.DownloadState.Paused && current_item.pauseReason === fdm.models.pauseReason.LowDiskSpace)
        {
            show_error = true;
            error_text = __('Low disk space');
        }

        // var error_text = current_item.errorText;
        if (state == fdm.models.DownloadState.Error
            && current_item.downloadType == fdm.models.DownloadType.batchDownload && error_text == '')
            error_text = __('Some files corrupted');

        if (current_tab == fdm.views.BottomPanelTab.General){
            return (
                React.createElement("div", {style: {display: current_tab == fdm.views.BottomPanelTab.General ? 'block' : 'none'}, 
                     id: "tab-general-container", className: 'tab-general tab-content ' + cssByState(current_item)}, 

                    React.createElement("div", {className: "general-wrap"}, 

                        React.createElement("div", {id: "bottom-panel-file-preview-container", className: "bottom-panel-img"}, 

                            React.createElement("img", {style: {maxWidth: Math.min(this.state.thumbMaxWidth, thumb.maxWidth) + 'px',
                            maxHeight: (this.props.bottomPanelHeight - 50) + 'px',
                            marginBottom: '-10px'}, 
                                 src: this.state.currentItem.getThumbnailUrl("bottom-panel"), 
                                 id: "bottom-panel-file-preview-img", 
                                 alt: "", onDrag: stopEventBubble, onDrop: stopEventBubble, onDragStart: stopEventBubble})

                        ), 

                        React.createElement("div", {className: "general-tab-wrap"}, 
                            React.createElement("div", {className: "stretch-element"}, 
                                React.createElement("div", {className: rjs_class({
                                    'wrap-title-dwnld' : true,
                                    without_tags: !current_item.tags || !current_item.tags.length
                                })}, 
                                    React.createElement("span", {className: "download-detalization-title"}, 

                                        requesting_info ?
                                            React.createElement("span", {className: "for_copy title"}, current_item.url)
                                            :
                                            React.createElement("span", {className: "for_copy title"}, current_item.fileName), 
                                        

                                        React.createElement("span", {className: "wrapper-user-tags", 
                                              style: {visibility: current_item.tags && current_item.tags.length ? 'visible' : 'hidden'}}, 

                                             current_item.tags && current_item.tags.map(function(tag, index){

                                                if (tag.system)
                                                    return null;

                                                return (
                                                    React.createElement("span", {onContextMenu: function(){
                                                        if (!tag.system)
                                                            fdmApp.menu.showTagPopupMenu(2, tag.id, current_item.id); // 2 - type tagOnDownload
                                                    }, 
                                                        key: tag.id, className: "tag_element"}, 
                                                        React.createElement("span", {className: "tag_color system", style: {backgroundColor: 'rgb('+tag.colorR+','+tag.colorG+','+tag.colorB+')'}}), 
                                                        React.createElement("span", {className: 'name tag_name' + (0.3 * tag.colorR/255 + 0.59 * tag.colorG/255 + 0.11 * tag.colorB/255 > 0.8 ? ' invert' : ''), 
                                                              title: tag.name}, tag.name)
                                                    )
                                                );

                                            })

                                        )
                                    )
                                ), 

                                state == fdm.models.DownloadState.Completed && !current_item.isMoving && !requesting_info && !show_error ?

                                    React.createElement("div", {className: rjs_class({
                                        wrap_completed_info: is_trt,
                                        wrap_completed: !is_trt
                                    })}, 

                                        current_item.filesCount > 0 && current_item.filesCount > current_item.filesChosenCount ?
                                            React.createElement("span", {className: "count"}, __('Completed %n / %2 files', [current_item.filesChosenCount, current_item.filesCount]))
                                            :
                                            React.createElement("span", {className: "count"}, __('Completed')), 
                                        

                                         is_trt ?
                                            React.createElement("span", {className: "seeding"}, 
                                                React.createElement("input", {onChange: this.setSeeding, 
                                                       checked: current_item.seedingEnabled, type: "checkbox", id: "enable_seeding"}), 
                                                React.createElement("label", {htmlFor: "enable_seeding"}, __('Enable seeding'))
                                            )
                                            : null

                                    )

                                    :

                                    React.createElement("div", {className: rjs_class({
                                    'progress-wrap': true,
                                    unknown_size: unknown_size,
                                    is_queued: state == fdm.models.DownloadState.Downloading && current_item.isQueued,
                                    downloading_paused_line: !current_item.isMoving && !requesting_info &&
                                         (state == fdm.models.DownloadState.Paused
                                         || state == fdm.models.DownloadState.Pausing
                                         || show_error)
                                        })}, 

                                        show_error && !current_item.isMoving ?

                                            current_item.progress == 100 ?
                                                React.createElement("span", {className: "wrap_completed_error"}, __('Completed'))
                                                :
                                                show_error && unknown_size ?
                                                    null :
                                                    React.createElement("span", {className: "wrap_completed_percents percents"},  (!current_item.progress || current_item.progress < 0 ? 0 : current_item.progress ) + '%')
                                            :

                                            [
                                                React.createElement("div", {key: "k2", className: "progress-line-wrap"}, 

                                                    moving_progress ?
                                                        React.createElement("div", {className: "progress", 
                                                             style: {width: ( current_item.moveProgress < 0 ? 0 : current_item.moveProgress) + '%'}})
                                                        :

                                                        state == fdm.models.DownloadState.Checking ?
                                                        React.createElement("div", {className: "progress", 
                                                             style: {width: ( current_item.checkingProgress < 0 ? 0 : current_item.checkingProgress) + '%'}})
                                                            :
                                                        React.createElement("div", {className: "progress", 
                                                             style: {width: ( current_item.progress < 0 ? 0 : current_item.progress) + '%'}})
                                                    
                                                ),
                                                React.createElement("span", {key: "k1", style: {
                                                    display: current_item.progress < 0 || unknown_size || moving_progress
                                                        || state == fdm.models.DownloadState.Checking ? 'none' : 'block',
                                                    paddingRight: 0,
                                                    width: 'auto'
                                                }, 
                                                      className: "percents"}, current_item.progress + '%'),
                                                state == fdm.models.DownloadState.Downloading && !current_item.isQueued
                                                    && remaining_time > 0 && !requesting_info ?
                                                    React.createElement("span", {key: "k3", style: {paddingRight: 0, width: 'auto'}, 
                                                        className: "percents"}, " ", '(' + __('%1 left', fdm.timeUtils.remainingTime(remaining_time)).trim() + ')'
                                                    )
                                                    : null,
                                                state == fdm.models.DownloadState.Downloading && current_item.isQueued && !requesting_info ?
                                                    React.createElement("span", {key: "k3", style: {paddingRight: 0, width: 'auto'}, 
                                                          className: "percents"}, " ", __('Queued'))
                                                    : null
                                            ], 

                                        

                                         (!show_error || current_item.isMoving)
                                            && error_text != '' && state != fdm.models.DownloadState.Downloading ?
                                            [
                                                React.createElement("span", {key: "k3", className: "process moving"}, current_item.isMoving && !moving_progress ? __('Moving') : current_process),
                                                React.createElement("span", {key: "k4", className: "error-message spec_error"}, error_text)
                                            ]
                                            : null, 

                                        !show_error
                                            && ( error_text == '' || state == fdm.models.DownloadState.Downloading ) ?
                                            React.createElement("span", {className: "process"}, current_item.isMoving && !moving_progress ? __('Moving') : current_process)
                                            : null, 

                                         !current_item.isMoving && show_error ?
                                            React.createElement("span", {className: "error-message"}, error_text)
                                            : null

                                    ), 

                                

                                React.createElement("div", {className: "general-info"}, 

                                    React.createElement("div", {className: "wrapper_table"}, 

                                        React.createElement("table", null, 

                                            React.createElement("tbody", null, 

                                            React.createElement("tr", null, 

                                                (function(){

                                                    var r = [];

                                                    if (state == fdm.models.DownloadState.Completed){

                                                        r.push(
                                                            React.createElement("td", {key: "s1", className: "title"}, 
                                                                current_item.filesCount > 0 && current_item.filesCount > current_item.filesChosenCount ?
                                                                    React.createElement("span", null, __('Selected size:'))
                                                                    :
                                                                    React.createElement("span", null, __('Total size:'))
                                                                
                                                            )
                                                        );
                                                        r.push(
                                                            React.createElement("td", {key: "s2"}, 
                                                                 current_item.totalBytes >= 0 ?
                                                                    React.createElement("span", {className: "value"}, fdm.sizeUtils.bytesAsText(current_item.totalBytes))
                                                                    :
                                                                    React.createElement("span", {style: {color: '#737373'}, className: "value grey"}, "-")
                                                                
                                                            )
                                                        );

                                                        return r;
                                                    }

                                                    r.push(
                                                        React.createElement("td", {key: "s3", className: "title"}, 
                                                            state == fdm.models.DownloadState.Checking ?
                                                                React.createElement("span", null, __('Checked size:'))
                                                                :
                                                                React.createElement("span", null, __('Downloaded:'))
                                                            
                                                        )
                                                    );

                                                    r.push(
                                                        React.createElement("td", {key: "s4"}, 
                                                                React.createElement("span", {className: "value"}, 
                                                                    fdm.sizeUtils.allBytesAsText(current_item.downloadedBytes, current_item.totalBytes)
                                                                )
                                                        )
                                                    );

                                                    return r;

                                                }).apply(this), 


                                                 is_trt ?
                                                    [
                                                        React.createElement("td", {key: "t1", className: "title"}, 
                                                            React.createElement("span", null, __('Uploaded:'))
                                                        ),

                                                        React.createElement("td", {key: "t2"}, 
                                                            React.createElement("span", {className: "value"}, fdm.sizeUtils.bytesAsText(current_item.uploadedBytes))
                                                        ),

                                                        React.createElement("td", {key: "t3", className: "title"}, 
                                                            React.createElement("span", null, __('Share ratio:'))
                                                        ),

                                                        React.createElement("td", {key: "t4"}, 
                                                             current_item.shareRatio && current_item.shareRatio.toFixed(2) > 0 ?
                                                                React.createElement("span", {className: "value"}, current_item.shareRatio.toFixed(2))
                                                                :
                                                                React.createElement("span", {className: "value"}, "0")
                                                            
                                                        )

                                                    ]
                                                    :
                                                    [
                                                        React.createElement("td", {key: "t1"}),
                                                        React.createElement("td", {key: "t2"}),
                                                        React.createElement("td", {key: "t3"}),
                                                        React.createElement("td", {key: "t4"})
                                                    ]

                                                

                                            ), 

                                            React.createElement("tr", null, 
                                                React.createElement("td", {className: "title"}, 
                                                    React.createElement("span", null, __('Download speed:'))
                                                ), 
                                                React.createElement("td", null, 
                                                     state == fdm.models.DownloadState.Downloading ?
                                                        React.createElement("span", {className: "value"}, fdm.speedUtils.speed2SignDigits(current_item.downloadSpeedBytes))
                                                        :
                                                        React.createElement("span", {style: {color: '#737373'}, className: "value grey"}, "-")
                                                    
                                                ), 

                                                 is_trt ?

                                                    [
                                                        React.createElement("td", {key: "u1", className: "title"}, 
                                                            React.createElement("span", null, __('Upload speed:'))
                                                        ),
                                                        React.createElement("td", {key: "u2"}, 
                                                             !current_item.seedingEnabled
                                                            || [fdm.models.DownloadState.Downloading,
                                                                fdm.models.DownloadState.Completed].indexOf(state) < 0 ?
                                                                React.createElement("span", {style: {color: '#737373'}, className: "value grey"}, "-")
                                                                : null, 
                                                             current_item.seedingEnabled
                                                            && [fdm.models.DownloadState.Downloading,
                                                                fdm.models.DownloadState.Completed].indexOf(state) >= 0 ?
                                                                React.createElement("span", {className: "value"}, fdm.speedUtils.speed2SignDigits(current_item.uploadSpeedBytes))
                                                                : null
                                                        ),
                                                        React.createElement("td", {key: "u3", className: "title"}, 
                                                            React.createElement("span", null, __('Availability:'))
                                                        ),
                                                        React.createElement("td", {key: "u4"}, 
                                                             state == fdm.models.DownloadState.Downloading && current_item.availability ?
                                                                React.createElement("span", {className: "value"}, parseInt(current_item.availability).toFixed(3))
                                                                : null, 
                                                             state == fdm.models.DownloadState.Downloading && !current_item.availability ?
                                                                React.createElement("span", {className: "value"}, "0")
                                                                : null, 
                                                             state != fdm.models.DownloadState.Downloading ?
                                                                React.createElement("span", {style: {color: '#737373'}, className: "value grey"}, "-")
                                                                : null
                                                        )
                                                    ]

                                                    :
                                                    [
                                                    React.createElement("td", {key: "u1"}),
                                                    React.createElement("td", {key: "u2"}),
                                                    React.createElement("td", {key: "u3"}),
                                                    React.createElement("td", {key: "u4"})
                                                    ]
                                                

                                            ), 



                                            React.createElement("tr", null, 
                                                 is_trt ?
                                                    React.createElement("td", {className: "title"}, 
                                                        React.createElement("span", null, __('Seeds:'))
                                                    )
                                                    : null, 
                                                 is_trt ?
                                                    React.createElement("td", null, 
                                                         current_item.seedsAllStat ?
                                                            React.createElement("span", {className: "value"}, current_item.seedsConnectedStat + ' (' + current_item.seedsAllStat + ')')
                                                            :
                                                            React.createElement("span", {className: "value"}, "0")
                                                        
                                                    )
                                                    : null, 

                                                 is_trt ?

                                                    [
                                                        React.createElement("td", {key: "p1", className: "title"}, 
                                                            React.createElement("span", null, __('Peers:'))
                                                        ),

                                                        React.createElement("td", {key: "p2"}, 
                                                             current_item.seedingEnabled && current_item.peersAllStat ?
                                                                React.createElement("span", {className: "value"}, current_item.peersConnectedStat + ' (' + current_item.peersAllStat + ')')
                                                                : null, 
                                                             current_item.seedingEnabled && !current_item.peersAllStat ?
                                                                React.createElement("span", {className: "value"}, "0")
                                                                : null, 
                                                             !current_item.seedingEnabled ?
                                                                React.createElement("span", {style: {color: '#737373'}, className: "value grey"}, "-")
                                                                : null
                                                        ),

                                                        React.createElement("td", {key: "p3", className: "title"}, 
                                                            React.createElement("span", null, __('DHT nodes:'))
                                                        ),
                                                        React.createElement("td", {key: "p4"}, 
                                                            React.createElement("span", {className: "value"}, parseInt(this.state.dhtNodes))
                                                        )
                                                    ]

                                                    :
                                                    [
                                                        React.createElement("td", {key: "p1"}),
                                                        React.createElement("td", {key: "p2"}),
                                                        React.createElement("td", {key: "p3"}),
                                                        React.createElement("td", {key: "p4"})
                                                    ]
                                                

                                            ), 



                                             !is_trt ?
                                                React.createElement("tr", null, 
                                                    React.createElement("td", {className: "title"}, 
                                                        React.createElement("span", null, __('Domain:'))
                                                    ), 
                                                    React.createElement("td", {colSpan: "5", key: "p4"}, 
                                                        React.createElement("span", {className: "value domain", title: current_item.url}, current_item.domain)
                                                    )
                                                )
                                                : null

                                            )

                                        )

                                    ), 

                                    React.createElement("div", null, 

                                        !current_item.isMoving ?
                                            React.createElement("div", {className: "info_path"}, 
                                                !current_item.missingStorage ?
                                                    React.createElement("span", {onClick: function(){fdmApp.downloads.openFolder(current_item.id)}, className: "folder_icon"})
                                                        :
                                                    React.createElement("span", {className: "folder_icon", style: {opacity: 0.7}}), 
                                                
                                                React.createElement("span", {className: "value path for_copy", title: current_item.targetFolder}, current_item.targetFolder)
                                            )
                                            : null, 

                                         is_trt && current_item.comment && current_item.comment != '' ?
                                            React.createElement("div", {className: "info_comment grey"}, 
                                                React.createElement("span", {dangerouslySetInnerHTML: {__html: comment}, className: "value cmnt for_copy"})
                                            )
                                            : null, 

                                         !is_trt && current_item.url && current_item.url != '' ?
                                            React.createElement("div", {className: "info_comment grey"}, 
                                                React.createElement("span", {className: "value cmnt"}, 
                                                    React.createElement("a", {href: current_item.url, className: "for_copy", onClick: _.partial(this.browse, current_item.url)}, 
                                                        current_item.url.length > 500 ? current_item.url.substr(0, 500) + '...' : current_item.url
                                                    )
                                                )
                                            )
                                            : null
                                    )
                                )
                            )
                        )
                    )
                )
            );
        }


        if (current_tab == fdm.views.BottomPanelTab.Progress){

            return (
                React.createElement("div", {className: "tab-log tab-content display_flex"}, 
                    React.createElement(ProgressMap, null)
                )
            );

        }

        if (current_tab == fdm.views.BottomPanelTab.Files){

            var sortOptions = this.state.sortOptions;

            return (
                React.createElement("div", {className: "tab-files tab-content display_flex"}, 
                    React.createElement("ul", {className: "table-headers"}, 
                        React.createElement("li", {className: rjs_class({
                                'file-name': true,
                                'sort-up': sortOptions.sortProp == 'name' && sortOptions.descending,
                                'sort-down': sortOptions.sortProp == 'name' && !sortOptions.descending
                            }), 
                            onClick: _.partial(this.sort, 'name'), 
                            style: {width: filesColumnSize.fileName + 'px'}, 
                            "data-resizeid": "fileName"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Name')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'filesColumnSize'), 
                               id: "resizer-test-name", className: "resizer"})
                        ), 
                        React.createElement("li", {className: rjs_class({
                                'file-size': true,
                                'sort-up': sortOptions.sortProp == 'size' && sortOptions.descending,
                                'sort-down': sortOptions.sortProp == 'size' && !sortOptions.descending
                            }), 
                            onClick: _.partial(this.sort, 'size'), 
                            style: {width: filesColumnSize.fileSize + 'px'}, 
                            "data-resizeid": "fileSize"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Size')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'filesColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {className: rjs_class({
                                'file-progress': true,
                                'sort-up': sortOptions.sortProp == 'progress' && sortOptions.descending,
                                'sort-down': sortOptions.sortProp == 'progress' && !sortOptions.descending
                            }), 
                            onClick: _.partial(this.sort, 'progress'), 
                            style: {width: filesColumnSize.fileProgress + 'px'}, 
                            "data-resizeid": "fileProgress"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Progress')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'filesColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {className: rjs_class({
                                'file-priority': true,
                                'sort-up': sortOptions.sortProp == 'priority' && sortOptions.descending,
                                'sort-down': sortOptions.sortProp == 'priority' && !sortOptions.descending
                            }), 
                            onClick: _.partial(this.sort, 'priority'), 
                            style: {width: filesColumnSize.filePriority + 'px'}, 
                            "data-resizeid": "filePriority"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Priority'))
                        )
                    ), 

                    current_item.downloadType == fdm.models.DownloadType.batchDownload ?
                        React.createElement(DownloadsTable, {filesColumnSize: filesColumnSize, isMoving: current_item.isMoving})
                        :
                        React.createElement(FilesTable, {filesColumnSize: filesColumnSize, isMoving: current_item.isMoving})
                    

                )
            );

        }

        if (current_tab == fdm.views.BottomPanelTab.Trackers){
            return (

                React.createElement("div", {className: "tab-trackers tab-content display_flex"}, 

                    React.createElement("ul", {className: "table-headers"}, 
                        React.createElement("li", {className: "tracker-name", style: {width: trackersColumnSize.trackerName + 'px'}, 
                            "data-resizeid": "trackerName"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('URL')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'trackersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {className: "tracker-status", style: {width: trackersColumnSize.trackerStatus + 'px'}, 
                            "data-resizeid": "trackerStatus"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Status')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'trackersColumnSize'), 
                               className: "resizer"})), 
                        React.createElement("li", {className: "tracker-update-in", style: {width: trackersColumnSize.trackerUpdateIn + 'px'}, 
                            "data-resizeid": "trackerUpdateIn"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Next announce'))
                        )
                    ), 
                    React.createElement("ul", {className: "trackers-info"}, 

                         trackers && trackers.map(function(tracker_child, index){

                            var tracker = tracker_child.toJSON();

                            return (
                                React.createElement("li", {key: index, className: "tracker"}, 
                                    React.createElement("div", {style: {width: trackersColumnSize.trackerName + 'px'}, 
                                         className: "tracker-name"}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, tracker.url)
                                    ), 
                                    React.createElement("div", {style: {width: trackersColumnSize.trackerStatus + 'px'}, 
                                         className: "tracker-status"}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy", title: tracker.status}, tracker.status)
                                    ), 
                                    React.createElement("div", {style: {width: trackersColumnSize.trackerUpdateIn + 'px'}, 
                                         className: "tracker-update"}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, 
                                            tracker.nextAnnounce == 0 ? '' : (tracker.nextAnnounce < 0 ? '---' : moment().add('s', tracker.nextAnnounce).from(moment()))
                                        )
                                    )
                                )
                            );

                        })
                    )

                )

            );
        }

        if (current_tab == fdm.views.BottomPanelTab.Peers){
            return (
                React.createElement("div", {className: "tab-peers tab-content display_flex"}, 
                    React.createElement("ul", {className: "table-headers"}, 
                        React.createElement("li", {style: {width: peersColumnSize.peerIp + 'px'}, 
                            "data-resizeid": "peerIp"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('IP')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerClient + 'px'}, 
                            "data-resizeid": "peerClient"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Client')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerFlags + 'px'}, 
                            "data-resizeid": "peerFlags"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Flags')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerPercents + 'px'}, 
                            "data-resizeid": "peerPercents"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, "%"), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 

                        React.createElement("li", {style: {width: peersColumnSize.peerSpeed + 'px'}, 
                            "data-resizeid": "peerSpeed"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Speed')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerReqs + 'px'}, 
                            "data-resizeid": "peerReqs"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Requests')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerDownloaded + 'px'}, 
                            "data-resizeid": "peerDownloaded"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Downloaded')), 
                            React.createElement("b", {onMouseDown: _.partial(this.resizeColumn, 'peersColumnSize'), 
                               className: "resizer"})
                        ), 
                        React.createElement("li", {style: {width: peersColumnSize.peerUploaded + 'px'}, 
                            "data-resizeid": "peerUploaded"}, 
                            React.createElement("span", {className: "rubber-ellipsis"}, __('Uploaded'))
                        )
                    ), 

                    React.createElement("ul", {className: "peers-info"}, 

                         current_tab == fdm.views.BottomPanelTab.Peers && peers.map(function(peer_child, index){

                            var peer = peer_child.toJSON();

                            return (
                                React.createElement("li", {key: index, className: "peer"}, 
                                    React.createElement("div", {style: {width: peersColumnSize.peerIp + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, peer.ip)
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerClient + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, peer.client)
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerFlags + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, this.peerStatus(peer))
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerPercents + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, this.peerProgress(peer))
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerSpeed + 'px'}}, 

                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, 

                                            function(){

                                                var dlSpeed = peer.downloadSpeed;
                                                var ulSpeed = peer.uploadSpeed;

                                                var resultSpeedText = [];
                                                if (dlSpeed > 0)
                                                    resultSpeedText.push( [
                                                        React.createElement("span", {key: "4", className: "arrow_dwn"}),
                                                        React.createElement("span", {key: "5"}, fdm.speedUtils.speed2SignDigits(dlSpeed))
                                                    ] );

                                                if (dlSpeed > 0 && ulSpeed > 0)
                                                    resultSpeedText.push(React.createElement("span", {key: "6"}, "; "));

                                                if (ulSpeed > 0)
                                                    resultSpeedText.push( [
                                                        React.createElement("span", {key: "7", className: "arrow_up"}),
                                                        React.createElement("span", {key: "8"}, fdm.speedUtils.speed2SignDigits(ulSpeed))
                                                    ]);

                                                return (
                                                    React.createElement("span", {className: "download-speed"}, 
                                                        resultSpeedText
                                                    )
                                                );


                                            }()

                                         )

                                    ), 
                                    /*<div style={{width: peersColumnSize.peerDownSpeed + 'px'}}>
                                        <span className="rubber-ellipsis">{fdm.speedUtils.speed2SignDigits(peer.downloadSpeed)}</span>
                                    </div>
                                    <div style={{width: peersColumnSize.peerUpSpeed + 'px'}}>
                                        <span className="rubber-ellipsis">{fdm.speedUtils.speed2SignDigits(peer.uploadSpeed)}</span>
                                    </div>*/
                                    React.createElement("div", {style: {width: peersColumnSize.peerReqs + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, peer.requests > 0 ? peer.requests : '')
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerDownloaded + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, fdm.sizeUtils.bytesAsText(peer.downloadedBytes))
                                    ), 
                                    React.createElement("div", {style: {width: peersColumnSize.peerUploaded + 'px'}}, 
                                        React.createElement("span", {className: "rubber-ellipsis for_copy"}, fdm.sizeUtils.bytesAsText(peer.uploadedBytes))
                                    )
                                )
                            );

                        }.bind(this))

                    )
                )
            );
        }

        if (current_tab == fdm.views.BottomPanelTab.Log){
            return (
                React.createElement("div", {className: "tab-log tab-content display_flex"}, 

                    React.createElement(LogsTable, null)

                )
            );
        }
    }
});

var LogScrollPosition = {
    scrollPosition: {},

    setScrollPosition: function(download_id, scroll_top){
        this.scrollPosition[download_id] = scroll_top;
    },
    removeScrollPosition: function(download_id){
        delete this.scrollPosition[download_id];
    },
    getScrollPosition: function(download_id){
        return this.scrollPosition[download_id];
    }
};

var LogsTable = React.createClass({displayName: "LogsTable",

    getInitialState: function() {

        var logs = app.controllers.bottomPanel.model.get('logs');
        var view_info = this.getViewItemsPositions(logs.length, true);
        return {
            currentItemId: app.controllers.bottomPanel.model.get('currentItemId'),
            logs: logs,
            viewInfo: view_info,
            showAll: false
        };
    },

    componentDidMount: function(){

        app.controllers.bottomPanel.model.on('change:logs', this.changeLogs, this);
        app.controllers.bottomPanel.model.on('change:currentItemId', this.changeItem, this);
        var scroll_container = ReactDOM.findDOMNode(this);
        scroll_container.addEventListener('scroll', this.changeScroll);

        if (this.state.logs.length > 0){

            var last_scroll_position = LogScrollPosition.getScrollPosition(this.state.currentItemId);
            if (last_scroll_position > 0 || last_scroll_position === 0)
                scroll_container.scrollTop = last_scroll_position;
            else
                scroll_container.scrollTop = scroll_container.scrollHeight;
        }

        app.controllers.bottomPanel.model.on('change:panelVisible', this.changeScrollDefer, this);
    },

    componentWillUnmount:function(){

        app.controllers.bottomPanel.model.off('change:logs', this.changeLogs, this);
        app.controllers.bottomPanel.model.off('change:currentItemId', this.changeItem, this);
        var scroll_container = ReactDOM.findDOMNode(this);
        scroll_container.removeEventListener('scroll', this.changeScroll);

        app.controllers.bottomPanel.model.off('change:panelVisible', this.changeScrollDefer, this);
    },

    changeItem: function(){

        this.setState({
            currentItemId: app.controllers.bottomPanel.model.get('currentItemId'),
            showAll: false
        });
    },

    changeScrollDefer: function(){

        _.defer(function(){
            if (this.isMounted())
                this.changeScroll();
        }.bind(this));
    },

    changeScroll: function(){

        var view_info = this.getViewItemsPositions(this.state.logs.length);
        this.setState({
            viewInfo: view_info
        });

        if (this.state.logs.length > 0){

            if (view_info.scroll_in_down)
                LogScrollPosition.removeScrollPosition(this.state.currentItemId);
            else{

                var scroll_container = ReactDOM.findDOMNode(this);
                if (scroll_container)
                    LogScrollPosition.setScrollPosition(this.state.currentItemId, scroll_container.scrollTop);
            }
        }
    },

    changeLogs: function(){

        var logs = app.controllers.bottomPanel.model.get('logs');
        var view_info = this.getViewItemsPositions(logs.length);

        var need_change_scroll = false;
        if (this.state.logs.length == 0 && logs.length > 0
            || this.state.viewInfo && this.state.viewInfo.count_items === 0)
            need_change_scroll = true;

        this.setState({
            logs: logs,
            viewInfo: view_info
        });

        var last_scroll_position = false;
        if (need_change_scroll)
            last_scroll_position = LogScrollPosition.getScrollPosition(this.state.currentItemId);

        var scroll_container;
        if (last_scroll_position > 0 || last_scroll_position === 0){

            scroll_container = ReactDOM.findDOMNode(this);
            if (scroll_container)
                scroll_container.scrollTop = last_scroll_position;
        }
        else if (view_info.scroll_in_down){

            scroll_container = ReactDOM.findDOMNode(this);
            if (scroll_container)
                scroll_container.scrollTop = scroll_container.scrollHeight;
        }
    },

    rowHeight: 21,

    getViewItemsPositions: function(count_items, first){

        var first = first || false;

        var scroll_container = false;
        if (!first && this.isMounted())
            scroll_container = ReactDOM.findDOMNode(this);

        var scrollTop = 0;
        var offsetHeight = window.innerHeight;

        var scroll_in_down = true;
        if (scroll_container)
        {
            scrollTop = scroll_container.scrollTop;
            scroll_in_down = scroll_container.scrollHeight - scroll_container.scrollTop - scroll_container.getBoundingClientRect().height < 10;
        }

        scrollTop = Math.min(scrollTop, count_items * this.rowHeight);

        var start = Math.round(scrollTop/this.rowHeight);

        var diff = this.state && this.state.showAll ? 50 : 20;

        var id_start = Math.max(start - diff, 0);
        if (!first && this.state.viewInfo && id_start > 0)
        {
            if (Math.abs(id_start - this.state.viewInfo.id_start) < 5)
                id_start = this.state.viewInfo.id_start;
        }

        var count_in_page = Math.round(offsetHeight/this.rowHeight);

        var id_end = Math.min(start + count_in_page + 2 * diff, count_items - 1);
        //var id_end = start + count_in_page + 2 * diff;
        //if (Math.abs(id_end - current_view_info.id_end) < 2)
        //    id_end = current_view_info.id_end;
        //id_end = Math.min(id_end, count_items - 1);

        var before_view = id_start;
        var after_view = Math.max(count_items - 1 - id_end, 0);

        return {id_start : id_start, id_end : id_end, count_items: count_items,
            before_view: before_view, after_view: after_view,
            before_height: before_view * this.rowHeight, after_height: after_view * this.rowHeight,
            scroll_in_down: scroll_in_down}

    },

    logMouseDown: function(e){

        if (!this.state.showAll && window.innerWidth - e.pageX > 25 ){

            this.state.showAll = true;

            var view_info = this.getViewItemsPositions(this.state.logs.length);
            this.setState({
                viewInfo: view_info
            });
        }
    },

    logContextMenu: function(e){

        if (this.state.currentItemId > 0 || this.state.currentItemId === 0)
            fdmApp.menu.showLogPopupMenu(this.state.currentItemId);
    },

    render: function(){

        var view_info = this.state.viewInfo;

        var logs = this.state.logs;
        //var show_all = this.state.showAll;
        //
        //if (show_all){
        //    view_info = {
        //        before_view: 0,
        //        before_height: 0,
        //        after_view: 0,
        //        after_height: 0,
        //        id_start: 0,
        //        id_end: (logs.length - 1)
        //    }
        //}

        var rows = [];
        if (view_info.before_view > 0){

            rows.push(
                React.createElement("li", {key: "before", className: "log-row", style: {height: view_info.before_height + 'px'}})
            );
        }

        for (var i = view_info.id_start; i <= view_info.id_end; i++) {

            var log =  logs[i];

            rows.push(
                React.createElement("li", {key: i, className: "log-row"}, 
                    React.createElement("div", {className: "log_content"}, 
                        React.createElement("span", {className: "date"}, log.datetime + ' '), React.createElement("span", {className: "info"}, log.text)
                    )
                )
            );

        }
        if (view_info.after_view > 0){

            rows.push(
                React.createElement("li", {key: "after", className: "log-row", style: {height: view_info.after_height + 'px'}})
            );
        }

        return (
            React.createElement("ul", {id: "log-info", 
                onContextMenu: this.logContextMenu, 
                onMouseDown: this.logMouseDown, className: "log-info"}, 

                rows

            )
        );
    }
});



var BottomMouseSlideMixin = {

    rowHeight: 24,

    selectDownloadsOnSlide: function(e)
    {
        var start_row = this.mouseSlideDownloadStart;

        if (start_row == null)
            return;

        var current_row = this.getDownloadOnTarget(e.target);

        if (current_row == this.mouseSlideDownloadEnd)
            return;

        if (!current_row){

            var scroll_dom = ReactDOM.findDOMNode(this);
            var scroll_rect = scroll_dom.getBoundingClientRect();

            var scroll_top = scroll_dom.scrollTop;
            var scroll_height = scroll_dom.scrollHeight;

            var top = scroll_rect.top;

            var new_scroll_top;
            var download_position;
            if (e.pageY > scroll_rect.bottom + 10){
                new_scroll_top = Math.max(0, scroll_top + Math.min(30, (e.pageY - (scroll_rect.height + top + 10))));
                scroll_dom.scrollTop = new_scroll_top;

                download_position = new_scroll_top + scroll_rect.height - 47;

            }
            if (e.pageY < top - 10){
                new_scroll_top = scroll_dom.scrollTop = Math.min((scroll_height - scroll_rect.height), (scroll_top - Math.min(30, (top - e.pageY - 10))));

                scroll_dom.scrollTop = new_scroll_top;
                download_position = new_scroll_top - 10;
            }
            if (download_position){
                current_row = this.getItemByScrollPosition(download_position);
            }
        }

        if (current_row && (current_row.id != start_row.id || this.mouseSlideHasSelect)){

            var start_download  = this.getItemById(start_row.id);
            var current_download  = this.getItemById(current_row.id);

            if (start_download && current_download){

                var ctrlKey = false;
                if (fdmApp.platform == 'mac')
                    ctrlKey = e.metaKey;
                else
                    ctrlKey = e.ctrlKey;

                if (!ctrlKey && !this.startInCheckbox){
                    this.clearSelection();
                }

                if (e.shiftKey && this.mouseSlideDownloadEnd && current_row.id != this.mouseSlideDownloadEnd.id){
                    var end_download  = this.getItemById(this.mouseSlideDownloadEnd.id);
                    this.removeSelectRange( start_download, end_download );
                }

                if ((ctrlKey || this.startInCheckbox) && this.startInSelection)
                    this.removeSelectRange( start_download, current_download );
                else
                    this.selectRange( start_download, current_download );
                this.mouseSlideHasSelect = true;

                this.mouseSlideDownloadEnd = current_download;
                stopEventBubble(e);
            }
        }
    }

};


var FilesTable = React.createClass({displayName: "FilesTable",

    templateName: 'FilesTable',

    mixins: [BottomMouseSlideMixin, MouseSlideMixin],

    getInitialState: function() {

        return this.getState(true);
    },

    componentDidMount: function(){

        _.bindAll(this, 'mouseSlideEnd', 'mouseSlideStart', 'mouseSlideMove',
            'mouseSlideOver', 'mouseSlideOut', 'mouseNotMove');

        var node = ReactDOM.findDOMNode(this);
        if (node)
            node.addEventListener('mousedown', this.mouseSlideStart);

        app.controllers.bottomPanel.model.on('change:fileTree', this.newFileTree, this);
        var fileTree = app.controllers.bottomPanel.model.get('fileTree');
        if (fileTree)
            fileTree.on('change', this.changeFileTree, this);

        var scroll_container = ReactDOM.findDOMNode(this);
        scroll_container.addEventListener('scroll', this.changeScroll);

        app.controllers.bottomPanel.model.on('change:panelVisible', this.changeScrollDefer, this);
    },

    componentWillUnmount:function(){

        if (this.state.currentItem)
            this.state.currentItem.off('change', this.changeFileTree, this);
        app.controllers.bottomPanel.model.off('change:currentItem', this.newFileTree, this);

        var scroll_container = ReactDOM.findDOMNode(this);
        scroll_container.removeEventListener('scroll', this.changeScroll);

        app.controllers.bottomPanel.model.off('change:panelVisible', this.changeScrollDefer, this);
    },

    getState: function(first){

        first = first || false;

        var file_tree = app.controllers.bottomPanel.model.get('fileTree');

        if (!file_tree){
            return {
                fileTree: file_tree,
                openedFolders: {},
                selectedFiles: {},
                one_file: false,
                filesList: []
            };
        }

        var selected_files = {};
        var opened_folders = {};

        var selected = file_tree.get('selectedList').models;
        var opened = file_tree.get('openedFolders').models;

        var i;
        for (i = 0; i < selected.length; i++ ){
            selected_files[selected[i].get('id')] = selected[i];
        }
        for (i = 0; i < opened.length; i++ ){
            opened_folders[opened[i].get('id')] = opened[i];
        }

        var one_file = false;
        if (file_tree && file_tree._children && file_tree._children.models
            && file_tree._children.models.length == 1 && file_tree._children.models[0]._children.models.length == 0)
            one_file = true;

        var state = {
            fileTree: file_tree,
            openedFolders: opened_folders,
            selectedFiles: selected_files,
            oneFile: one_file
        };

        var files_list = [];
        var level = 0;

        if (file_tree._children.models.length > 0)
            this.getFilesList(state, files_list, level, file_tree._children.models[0]);

        state.filesList = files_list;

        state.viewInfo = this.getViewItemsPositions(files_list.length, first);

        return state;
    },

    getItemById: function (id) {

        return _.findWhere(this.state.filesList, {index: id});
    },

    getItemByScrollPosition: function (scroll_position) {

        var i = Math.ceil(scroll_position/this.rowHeight);

        if (this.state.filesList[i])
            return {id: this.state.filesList[i].index, index: i};

        return false;
    },

    getDownloadOnTarget: function (target) {

        if (target.nodeName.toLowerCase() == 'li' && target.classList.contains('tree_node')){
            var id = parseInt(target.getAttribute('data-id'));

            var list = _.pluck(this.state.filesList, 'index');

            var index = list.indexOf(id);

            return {id: id, index: index};
        }
        else if (target.parentNode){
            return this.getDownloadOnTarget(target.parentNode);
        }

        return null;
    },

    changeScrollDefer: function(){

        _.defer(function(){
            if (this.isMounted())
                this.changeScroll();
        }.bind(this));
    },



    changeKeyboardNavigate: function(){

        var tree_in_focus = app.controllers.keyboardNavigation.model.get('currentForm') == 'main'
            && app.controllers.keyboardNavigation.model.get('itemInFocus') == 'bottom_panel_content';

        if (this.state.treeInFocus != tree_in_focus)
            this.setState({treeInFocus: tree_in_focus});
    },

    scrollToItemByIndex: function(index){

        var scroll_container = ReactDOM.findDOMNode(this);

        if (!scroll_container)
            return;

        var current_scroll = scroll_container.scrollTop;
        var offsetHeight = scroll_container.offsetHeight - 4;
        var top_positions = this.rowHeight * index;

        if (current_scroll > top_positions)
            scroll_container.scrollTop = top_positions;
        else if (top_positions > current_scroll + offsetHeight - this.rowHeight)
            scroll_container.scrollTop = top_positions - offsetHeight + this.rowHeight;

    },

    getCurrentRow: function(){

        if (this.state.currentIndex > 0 || this.state.currentIndex === 0)
            return _.findWhere(this.state.filesList, {index: this.state.currentIndex});

        return false;
    },

    toggleSelectedCheck: function(){

        var row = this.getCurrentRow();

        if (row)
            app.controllers.downloadWizard.toggleChecked(row['treeNode']);
    },

    closeSelectedFolder: function(){

        var row = this.getCurrentRow();

        if (row && !row['is_leaf'] && row['is_open'])
            this.state.fileTree.toggleOpen(row['treeNode']);
    },

    openSelectedFolder: function(){

        var row = this.getCurrentRow();

        if (row && !row['is_leaf'] && !row['is_open'])
            this.state.fileTree.toggleOpen(row['treeNode']);
    },

    keyboardNavigate: function(content){

        if (content.keyName == 'enter'){
            //this.toggleSelectedCheck();
        }
        else{

            if (['left'].indexOf(content.keyName) >= 0){

                this.closeSelectedFolder();
            }
            if (['right'].indexOf(content.keyName) >= 0){

                this.openSelectedFolder();
            }
            if (['up', 'down'].indexOf(content.keyName) >= 0){

                var list = _.pluck(this.state.filesList, 'index');

                var current_index = -1;
                if (this.state.currentIndex > 0 || this.state.currentIndex === 0)
                    current_index = list.indexOf(this.state.currentIndex);

                var next_index;
                if (content.keyName == 'up')
                    next_index = current_index - 1;
                if (content.keyName == 'down')
                    next_index = current_index + 1;

                if (next_index > list.length - 1)
                    next_index = 0;
                if (next_index < 0)
                    next_index = list.length - 1;

                this.setState({
                    currentIndex: list[next_index]
                });

                this.scrollToItemByIndex(next_index);
            }
        }

    },

    getTreeIndexByFileId: function(file_id){

        var list = _.pluck(this.state.filesList, 'index');

        return list.indexOf(file_id);
    },




    changeScroll: function(){

        var view_info = this.getViewItemsPositions(this.state.filesList.length);
        this.setState({
            viewInfo: view_info
        });
    },

    getFilesList: function(state, files_list, level, treeNode){

        var file_index = treeNode.attributes.index;

        var is_open = state.openedFolders[file_index] != undefined;
        var is_selected = state.selectedFiles[file_index];
        var is_leaf = treeNode._children.length == 0;

        files_list.push({
            index: file_index,
            treeNode: treeNode,
            level: level,
            is_open: is_open,
            is_selected: is_selected,
            is_leaf: is_leaf
        });

        if (is_open && treeNode._children.models.length > 0){
            treeNode._children.models.map(function(file){

                this.getFilesList(state, files_list, (level + 1), file);

            }.bind(this));
        }
    },

    getViewItemsPositions: function(count_items, first){

        var first = first || false;

        var scroll_container = false;
        if (!first && this.isMounted())
            scroll_container = ReactDOM.findDOMNode(this);

        var scrollTop = 0;
        var offsetHeight = window.innerHeight;

        if (scroll_container)
        {
            scrollTop = scroll_container.scrollTop;
        }

        scrollTop = Math.min(scrollTop, count_items * this.rowHeight);

        var start = Math.round(scrollTop/this.rowHeight);

        var id_start = Math.max(start - 20, 0);
        var count_in_page = Math.round(offsetHeight/this.rowHeight);

        var id_end = Math.min(start + count_in_page + 20, count_items - 1);

        var before_view = id_start;
        var after_view = Math.max(count_items - 1 - id_end, 0);

        return {id_start : id_start, id_end : id_end,
            before_view: before_view, after_view: after_view,
            before_height: before_view * this.rowHeight, after_height: after_view * this.rowHeight}

    },

    newFileTree: function(){

        if (this.state.fileTree)
            this.state.fileTree.off('change', this.changeFileTree, this);

        var fileTree = app.controllers.bottomPanel.model.get('fileTree');
        if (fileTree)
            fileTree.on('change', this.changeFileTree, this);

        this.changeFileTree();
    },

    changeFileTree: function(){

        if (this.isMounted())
            this.setState(this.getState());
    },

    lastMouseDown: false,
    lastFileIndex: false,

    toggleOpen: function(treeNode, e){

        stopEventBubble(e);

        if (fdmApp.platform == 'mac' && e.ctrlKey || e.button == 2){
            return;
        }

        var current_date = +new Date();
        var index = treeNode.get('index');
        if (index == this.lastFileIndex && this.lastMouseDown && current_date - this.lastMouseDown < 200)
            return;
        this.lastMouseDown = current_date;
        this.lastFileIndex = index;

        var root_tree = this.state.fileTree;
        root_tree.toggleOpen(treeNode);

    },

    onMouseDown: function(row, e){

        if (fdmApp.platform == 'mac' && e.ctrlKey || e.button == 2 || !row){
            return;
        }

        stopEventBubble(e);

        var treeNode = row.treeNode;

        if (!treeNode)
            return;

        var current_date = +new Date();
        var index = treeNode.get('index');
        if (index == this.lastFileIndex && this.lastMouseDown && current_date - this.lastMouseDown < 400)
            return;
        this.lastMouseDown = current_date;
        this.lastFileIndex = index;

        var ctrlKey = false;
        if (fdmApp.platform == 'mac')
            ctrlKey = e.metaKey;
        else
            ctrlKey = e.ctrlKey;


        if ((this.state.currentIndex > 0 || this.state.currentIndex === 0) && e.shiftKey){

            this.selectInterval(row, this.state.currentIndex, index);
        }
        else {

            this.setState({
                currentIndex: index
            });

            if (ctrlKey){
                if (row['is_selected'])
                    this.state.fileTree.setSelection(treeNode, false);
                else
                    this.state.fileTree.setSelection(treeNode, true);
            }
            else
                this.state.fileTree.resetSelection(treeNode, true);
        }

    },

    selectInterval: function(row, startIndex, endIndex){

        var list = _.pluck(this.state.filesList, 'index');

        var start_pos = list.indexOf(startIndex);
        var end_pos = list.indexOf(endIndex);

        if (start_pos < 0 || end_pos < 0){

            this.setState({
                currentIndex: row.treeNode.get('index')
            });
            this.state.fileTree.resetSelection(row.treeNode, true);

            return;
        }

        var select = true;//typeof(this.state.fileTree.get('selectedList')[startIndex]) != "undefined";

        var tree_node_list = [];
        for (var i = Math.min(start_pos, end_pos); i <= Math.max(start_pos, end_pos); i++){

            tree_node_list.push(this.state.filesList[i].treeNode);
        }
        this.state.fileTree.resetSelectionList(tree_node_list, select);
    },

    getRengeList: function (start_download, end_download) {

        var list = _.pluck(this.state.filesList, 'index');

        var start_pos = list.indexOf(start_download.index);
        var end_pos = list.indexOf(end_download.index);

        if (start_pos < 0 || end_pos < 0)
            return false;

        var tree_node_list = [];
        for (var i = Math.min(start_pos, end_pos); i <= Math.max(start_pos, end_pos); i++){

            tree_node_list.push(this.state.filesList[i].treeNode);
        }

        return tree_node_list;
    },
    clearSelection: function () {

        this.state.fileTree.clearSelection();
    },
    removeSelectRange: function(start_download, end_download){

        var list = this.getRengeList(start_download, end_download);

        this.state.fileTree.setSelectionList(list, false);
    },
    selectRange: function(start_download, end_download){

        var list = this.getRengeList(start_download, end_download);

        this.state.fileTree.setSelectionList(list, true);
    },

    launchFile: function(treeNode, e){

        stopEventBubble(e);
        app.controllers.bottomPanel.launchFile(treeNode.get('index'));
    },

    lastCheckboxClick: 0,
    checkboxClick: function(row, e){

        var n = + new Date();
        if (n - this.lastCheckboxClick < 200)
            return;

        this.lastCheckboxClick = n;

        if (this.props.isMoving)
            return;

        var treeNode = row.treeNode;

        var new_priority;
        if (treeNode.get('checked'))
            new_priority = fdm.models.filePriority.Skip;
        else
            new_priority = fdm.models.filePriority.Normal;

        var index = treeNode.get('index');

        if (row['is_selected']){

            if (!this.state.currentIndex){
                this.setState({
                    currentIndex: index
                });
            }

            app.controllers.bottomPanel.setPriority(Object.keys(this.state.selectedFiles), new_priority);
        }
        else{

            this.setState({
                currentIndex: index
            });

            this.state.fileTree.resetSelectionList([treeNode], true);

            app.controllers.bottomPanel.setPriority([index], new_priority);
        }
    },

    onContextMenu: function(treeNode, e){

        stopEventBubble(e);

        if (!this.state.selectedFiles[treeNode.get('index')]){
            this.setState({
                currentIndex: treeNode.get('index')
            });

            this.state.fileTree.resetSelection(treeNode);
        }

        var files = [];
        this.state.fileTree.get('selectedList').map(function(node){

            files.push(node.get('id'));
        });

        app.controllers.bottomPanel.showFilesPopupMenu(files);
    },

    changePriority: function(type, file_index, priority){

        var values = [];
        for (var k in fdm.models.filePriority)
            values.push(fdm.models.filePriority[k]);

        var key = values.indexOf(priority);
        if ( type == 'up' && priority != fdm.models.filePriority.High ){

            app.controllers.bottomPanel.setPriority([file_index], values[(key+1)]);
        }
        if ( type == 'down' && priority != fdm.models.filePriority.Skip && priority != fdm.models.filePriority.Low ){

            app.controllers.bottomPanel.setPriority([file_index], values[(key-1)]);
        }
    },


    renderTreeNode: function(filesList, level, id_end){

        var rows = [];

        while (this.renderIndex <= id_end){

            var file_row = filesList[this.renderIndex];
            var next_row = filesList[this.renderIndex + 1];

            if (file_row['level'] > level){

                rows.push(
                    React.createElement("li", {className: rjs_class({
                        opened: is_open,
                        file: is_leaf,
                        once_file: one_file,
                        folder: !is_leaf,
                        'tree-node-selected': is_selected
                        })}, 
                        React.createElement("ul", {className: "files-table"}, 
                            this.renderTreeNode(filesList, (level + 1), id_end)
                        )
                    )
                );
            }
            else if (file_row['level'] < level){

                this.renderIndex--;

                return rows;
            }
            else{

                var treeNode = file_row['treeNode'];

                var one_file = this.state.oneFile;

                var filesColumnSize = this.props.filesColumnSize;

                var file_data = treeNode.attributes.data;
                var file_index = treeNode.attributes.index;
                var checked = treeNode.attributes.checked;

                var is_open = file_row['is_open'];
                var is_selected = file_row['is_selected'];
                var is_leaf = file_row['is_leaf'];

                var is_moving = this.props.isMoving;

                var is_current = this.state.currentIndex === file_index;

                rows.push(
                    React.createElement("li", {key: file_index, 
                        className: rjs_class({
                    tree_node: true,
                    opened: is_open,
                    file: is_leaf,
                    once_file: one_file,
                    folder: !is_leaf,
                    current: is_current,
                    'tree-node-selected': is_selected }), 
                        "data-id": file_index, 
                        onMouseDown: _.partial(this.onMouseDown, treeNode), 
                        onDoubleClick: stopEventBubble}, 
                        React.createElement("div", {className: "triangle", 
                             onMouseDown: _.partial(this.toggleOpen, treeNode), 
                             onContextMenu: stopEventBubble}), 
                        React.createElement("div", {className: "file-name", style: {width: (filesColumnSize.fileName + (one_file ? 20 : 0)) + 'px'}}, 
                            React.createElement("span", {className: "pad", 
                                  onMouseDown: _.partial(this.onMouseDown, file_row), 
                                  onDoubleClick: _.partial(this.launchFile, treeNode), 
                                  onContextMenu: _.partial(this.onContextMenu, treeNode)}, 
                                React.createElement("span", {onClick: _.partial(this.checkboxClick, file_row), 
                                      onMouseDown: stopEventBubble, 
                                      onDoubleClick: stopEventBubble, 
                                      className: rjs_class({
                                fake_checkbox: true,
                                checked: checked,
                                indeterminate: checked === undefined,
                                disabled: is_moving
                                })}), file_data.name
                            )
                        ), 
                        React.createElement("div", {className: "file-size", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.fileSize + 'px'}}, 
                            React.createElement("span", {className: "rubber-ellipsis", 
                                  onMouseDown: _.partial(this.onMouseDown, file_row), 
                                  onDoubleClick: _.partial(this.launchFile, treeNode), 
                                  onContextMenu: _.partial(this.onContextMenu, treeNode)}, 
                                fdm.sizeUtils.bytesAsText(file_data.size)
                            )
                        ), 
                        React.createElement("div", {className: "file-progress", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.fileProgress + 'px'}}, 
                            React.createElement("span", {className: "pad", 
                                  onMouseDown: _.partial(this.onMouseDown, file_row), 
                                  onDoubleClick: _.partial(this.launchFile, treeNode), 
                                  onContextMenu: _.partial(this.onContextMenu, treeNode)}, 
                                React.createElement("span", {className: "progress-pre-wrap"}, 
                                    React.createElement("span", {className: "progress-wrap"}, 
                                        React.createElement("span", {className: "progress", style: {width: (file_data.progress < 0 ? 0 : file_data.progress) + '%'}})
                                    ), 
                                React.createElement("span", {className: "percents", style: {display: file_data.progress < 0 ? 'none' : 'block'}}, 
                                    Math.floor(file_data.progress) + '%')
                                )
                            )
                        ), 
                        React.createElement("div", {className: "file-priority", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.filePriority + 'px'}}, 
                            React.createElement("span", {className: "rubber-ellipsis", 
                                  onMouseDown: _.partial(this.onMouseDown, file_row), 
                                  onDoubleClick: _.partial(this.launchFile, treeNode), 
                                  onContextMenu: _.partial(this.onContextMenu, treeNode)}, 
                                file_data.priority == fdm.models.filePriority.Skip ? '' :
                                    file_data.priority == fdm.models.filePriority.Low ? __('Low') :
                                        file_data.priority == fdm.models.filePriority.Normal ? __('Normal') :
                                            file_data.priority == fdm.models.filePriority.High ? __('High') : null
                            ), 

                            file_data.priority != fdm.models.filePriority.Skip ?
                            React.createElement("div", {className: "priority_buttons"}, 
                                React.createElement("div", {className: rjs_class({
                                    priority_button_up: true, disabled: file_data.priority == fdm.models.filePriority.High
                                }), onClick: _.partial(this.changePriority, 'up', file_index, file_data.priority)}), 
                                React.createElement("div", {className: rjs_class({
                                    priority_button_down: true, disabled: file_data.priority == fdm.models.filePriority.Low
                                }), onClick: _.partial(this.changePriority, 'down', file_index, file_data.priority)}), 
                                React.createElement("div", {className: "priority_button_text"}, __('Priority'))
                            )
                                : null

                        ), 


                        next_row && next_row['level'] > level && ++this.renderIndex ?

                            React.createElement("ul", {className: "files-table"}, 

                                this.renderTreeNode(filesList, (level + 1), id_end)

                            )

                            : null

                    )
                );
            }

            this.renderIndex++;
        }

        return rows;

    },

    renderIndex: 0,

    render: function () {

        if (!this.state.fileTree)
            return null;

        var filesList = this.state.filesList;

        var view_info = this.state.viewInfo;
        this.renderIndex = view_info.id_start;
        var rows = this.renderTreeNode(filesList, 0, view_info.id_end);

        return (
            React.createElement("ul", {className: "files-table"}, 

                 view_info.before_height > 0 ?
                    React.createElement("li", {key: "before", style: {height: view_info.before_height + 'px'}})
                    : null, 

                rows, 

                 view_info.after_height > 0 ?
                    React.createElement("li", {key: "after", style: {height: view_info.after_height + 'px'}})
                    : null
            )
        );
    }
});


var DownloadsTable = React.createClass({displayName: "DownloadsTable",

    templateName: 'DownloadsTable',

    mixins: [BottomMouseSlideMixin, MouseSlideMixin],

    dispatcherIndex: false,

    getInitialState: function() {

        var downloads = app.controllers.bottomPanel.collections.downloads;

        var state = {};
        //state.hasScroll = false;
        state.downloads = downloads;
        state.currentIndex = false;
        state.selectedFiles = {};
        //state.selectedList = app.controllers.downloads.collections.currentSelectedList;
        //state.viewInfo = app.controllers.downloads.view_model.getViewItemsPositions(downloads.length);

        return state;
    },

    componentDidMount: function(){

        _.bindAll(this, 'mouseSlideEnd', 'mouseSlideStart', 'mouseSlideMove',
            'mouseSlideOver', 'mouseSlideOut', 'mouseNotMove');

        //_.bindAll(this, 'mouseSlideEnd', 'mouseSlideStart', 'changeScroll', 'changeSize', 'mouseSlideMove',
        //    'mouseSlideOver', 'mouseSlideOut', 'mouseNotMove');
        //window.addEventListener('resize', this.changeSize);

        var downloads = ReactDOM.findDOMNode(this);

        if (downloads){
            // downloads.addEventListener('scroll', this.changeScroll);
            downloads.addEventListener('mousedown', this.mouseSlideStart);
        }

        app.controllers.bottomPanel.collections.downloads.on('add reset remove sort', this.changeDownloads, this);
        //app.controllers.downloads.collections.currentSelectedList.on('add reset remove', this.changeSelected, this);
        //app.controllers.bottomPanel.model.on('change:panelVisible', this.needUpdateScrollFlag, this);

        //this.dispatcherIndex = FdmDispatcher.register(function(payload) {
        //
        //    if (payload.source == 'VIEW_ACTION'){
        //
        //        if (payload.action.actionType == 'changeBottomPanelHeight')
        //            this.needUpdateScrollFlag.apply(this);
        //        /*if (payload.action.actionType == 'WindowOnFocus')
        //         this.windowOnFocus.apply(this);*/
        //    }
        //
        //    return true; // No errors. Needed by promise in Dispatcher.
        //}.bind(this));
    },

    componentWillUnmount: function() {

        //window.removeEventListener('resize', this.changeSize);
        //document.getElementById('downloads-scroll-container').removeEventListener('scroll', this.changeScroll);
        //document.getElementById('downloads-scroll-container').removeEventListener('mousedown', this.mouseSlideStart);
        app.controllers.bottomPanel.collections.downloads.off('add reset remove sort', this.changeDownloads, this);
        //app.controllers.downloads.collections.currentSelectedList.off('add reset remove', this.changeSelected, this);

        //app.controllers.bottomPanel.model.off('change:panelVisible', this.needUpdateScrollFlag, this);
        //FdmDispatcher.unregister(this.dispatcherIndex);
    },

    getItemById: function (id) {

        return this.state.downloads.get(id);
    },

    getDownloadOnTarget: function (target) {

        if (target.nodeName.toLowerCase() == 'li' && target.classList.contains('tree_node')){
            var id = target.getAttribute('data-id');
            var list = this.state.downloads.pluck('id');
            var index = list.indexOf(id);

            return {id: id, index: index};
        }
        else if (target.parentNode){
            return this.getDownloadOnTarget(target.parentNode);
        }

        return null;
    },

    getItemByScrollPosition: function (scroll_position) {

        var i = Math.ceil(scroll_position/this.rowHeight);

        return this.state.downloads.models[i];
    },

    changeDownloads: function(){

        var downloads = app.controllers.bottomPanel.collections.downloads;
        this.setState({
            downloads: downloads
            //viewInfo: app.controllers.downloads.view_model.getViewItemsPositions(downloads.length)
        });
        //_.defer(_.bind(this.needUpdateScrollFlag, this));
    },

    onContextMenu: function (download, e) {

        this.setState({
            currentIndex: download.id
        });

        var list = [];
        if (this.state.selectedFiles[download.id]){

            list = Object.keys(this.state.selectedFiles);
        }
        else {

            this.resetSelection(download);
            list.push(download.id);
        }

        fdmApp.menuManager.showPopupMenu(list, download.id, app.controllers.bottomPanel.model.get('panelVisible'));
    },

    onMouseDown: function(download, e){

        if (fdmApp.platform == 'mac' && e.ctrlKey || e.button == 2){
            return;
        }

        stopEventBubble(e);

        var current_date = +new Date();
        var index = download.get('id');
        if (index == this.lastFileIndex && this.lastMouseDown && current_date - this.lastMouseDown < 400)
            return;
        this.lastMouseDown = current_date;
        this.lastFileIndex = index;

        var ctrlKey = false;
        if (fdmApp.platform == 'mac')
            ctrlKey = e.metaKey;
        else
            ctrlKey = e.ctrlKey;

        if ((this.state.currentIndex > 0 || this.state.currentIndex === 0) && e.shiftKey){

            this.resetSelectInterval(download, this.state.currentIndex, index);
        }
        else {

            this.setState({
                currentIndex: index
            });

            if (ctrlKey){
                if (this.state.selectedFiles[download.id])
                    this.setSelection(download, false);
                else
                    this.setSelection(download, true);
            }
            else
                this.resetSelection(download, true);
        }
    },

    clearSelection: function () {

        this.setState({
            selectedFiles: {}
        });
    },

    setSelection: function (download, select) {

        var s = this.state.selectedFiles;

        if (select && !s[download.id]){

            s[download.id] = download;

            this.setState({
                selectedFiles: s
            });
        }
        else if (!select && s[download.id]){

            delete s[download.id];

            this.setState({
                selectedFiles: s
            });
        }
    },

    resetSelection: function (download) {

        var s = {};
        s[download.id] = download;

        this.setState({
            selectedFiles: s
        });
    },

    resetSelectionList: function (downloads) {

        this.setState({
            selectedFiles: {}
        });

        this.selectionList(downloads);
    },

    selectionList: function (downloads) {

        var s = this.state.selectedFiles;

        for (var i =0; i < downloads.length; i++){

            var d = downloads[i];

            s[d.id] = d;
        }

        this.setState({
            selectedFiles: s
        });
    },

    removeSelectRange: function (start_download, current_download) {

        var list = this.state.downloads.pluck('id');

        var start_pos = list.indexOf(start_download.id);
        var end_pos = list.indexOf(current_download.id);

        if (start_pos < 0 || end_pos < 0){

            return;
        }

        var tree_node_list = [];
        for (var i = Math.min(start_pos, end_pos); i <= Math.max(start_pos, end_pos); i++){

            tree_node_list.push(this.state.downloads.models[i]);
        }

        var s = this.state.selectedFiles;

        for (var i =0; i < tree_node_list.length; i++){

            var d = tree_node_list[i];

            if (s[d.id])
                delete s[d.id];
        }

        this.setState({
            selectedFiles: s
        });
    },

    selectRange: function (start_download, current_download) {

        this.selectInterval(start_download, start_download.id, current_download.id);
    },

    resetSelectInterval: function(download, startIndex, endIndex){

        this.setState({
            selectedFiles: {}
        });

        this.selectInterval(download, startIndex, endIndex);
    },

    selectInterval: function(download, startIndex, endIndex){

        var list = this.state.downloads.pluck('id');

        var start_pos = list.indexOf(startIndex);
        var end_pos = list.indexOf(endIndex);

        if (start_pos < 0 || end_pos < 0){

            this.setState({
                currentIndex: download.id
            });

            return;
        }

        var select = true;//typeof(this.state.fileTree.get('selectedList')[startIndex]) != "undefined";

        var tree_node_list = [];
        for (var i = Math.min(start_pos, end_pos); i <= Math.max(start_pos, end_pos); i++){

            tree_node_list.push(this.state.downloads.models[i]);
        }
        this.selectionList(tree_node_list, select);
    },

    // onContextMenu: function(treeNode, e){
    //
    //     stopEventBubble(e);
    //
    //     if (!this.state.selectedFiles[treeNode.get('index')]){
    //         this.setState({
    //             currentIndex: treeNode.get('index')
    //         });
    //
    //         this.state.fileTree.resetSelection(treeNode);
    //     }
    //
    //     var files = [];
    //     this.state.fileTree.get('selectedList').map(function(node){
    //
    //         files.push(node.get('id'));
    //     });
    //
    //     app.controllers.bottomPanel.showFilesPopupMenu(files);
    // },


    checkboxClick: function (download, e) {

        this.setState({
            currentIndex: download.id
        });

        stopEventBubble(e);

        if (download.get('isScheduled'))
        {
            app.controllers.downloads.toggleDownload(download);
            return;
        }

        if (!this.enableCheckbox(download))
            return;

        var state = download.get('state');

        var checked_checkbox = this.checkedCheckbox(download);

        if (this.state.selectedFiles[download.id]){

            var list = [];

            _.map(this.state.selectedFiles, function(d){

                list.push(d);

                // if (this.enableCheckbox(d) && checked_checkbox == this.checkedCheckbox(d))
                //     app.controllers.downloads.toggleDownload(d);

            }.bind(this));

            if (checked_checkbox)
                app.controllers.downloads.pauseList(list);
            else
                app.controllers.downloads.startList(list);
        }
        else {

            this.resetSelection(download, true);
            app.controllers.downloads.toggleDownload(download);
        }
    },

    checkedCheckbox: function (download) {

        var state = download.get('state');

        return !(state == fdm.models.DownloadState.Paused
        || state == fdm.models.DownloadState.Paused || state == fdm.models.DownloadState.Error);
    },

    enableCheckbox: function (download) {

        if (download.get('isMoving'))
            return false;

        return [
                fdm.models.DownloadState.Downloading,
                fdm.models.DownloadState.WaitingForMetadata,
                fdm.models.DownloadState.Checking,
                fdm.models.DownloadState.Reconnecting,
                fdm.models.DownloadState.Paused,
                fdm.models.DownloadState.Pausing,
                fdm.models.DownloadState.Error
            ].indexOf(download.get('state')) >= 0;
    },


    render: function() {

        return (
            React.createElement("ul", {className: "files-table"}, 

                this.state.downloads.map(function(download, i){

                    return (
                        React.createElement(BottomPanelDownload, {
                              key: download.id, 
                              index: i, 
                              download: download, 
                              filesColumnSize: this.props.filesColumnSize, 
                              onDownloadMouseDown: this.onMouseDown, 
                              onContextMenu: this.onContextMenu, 
                              checkboxClick: this.checkboxClick, 
                              current: this.state.currentIndex == download.id, 
                              selected: true && this.state.selectedFiles[download.id]}
                        )
                    );

                }.bind(this))

            )
        );
    }
});


var BottomPanelDownload = React.createClass({displayName: "BottomPanelDownload",

    getInitialState: function() {

        var state = this.props.download.toJSON();
        state.showFilenameTitle = false;
        state.showErrorTitle = false;

        return state;
    },

    componentDidMount: function(){

        this.props.download.on('change', this.downloadChange, this);
    },

    componentWillUnmount: function() {

        this.props.download.off('change', this.downloadChange, this);
    },

    downloadChange: function(){

        this.setState(this.props.download.toJSON());
    },

    getErrorText: function(text){

        if (text.indexOf('HTTP Error') === 0){
            var pos = text.indexOf(':');
            if (pos > 0){
                return text.substring(0, pos);
            }
        }

        return text;

    },

    needToggleBottomPanel: false,
    lastDownloadClick: 0,
    togglePanelTimeout: 0,
    onDownloadClick: function(){

        var current_date = +new Date();
        if (current_date - this.lastDownloadClick < 350){

            this.onDownloadDoubleClick();
            return;
        }
        this.lastDownloadClick = current_date;

        if (app.controllers.downloads.view_model.lastPopupMenuDownloadId === this.props.download.id)
            return;

        if (this.needToggleBottomPanel) {

            this.togglePanelTimeout = setTimeout(function(){

                app.controllers.bottomPanel.showToggle();
            }.bind(this), 350);
        }
    },

    onDownloadDoubleClick: function(){

        this.props.view_model.launchItem(this.props.download);
        this.needToggleBottomPanel = false;
        clearTimeout(this.togglePanelTimeout);
    },

    onDownloadMouseDown: function(e){

        this.props.onDownloadMouseDown(this.props.download, e)
    },

    setCurrent: function () {

        app.controllers.bottomPanel.setCurrent(this.props.download);
    },

    onContextMenu: function (e) {

        this.props.onContextMenu(this.props.download, e);
    },

    checkboxClick: function (e) {

        this.props.checkboxClick(this.props.download, e);
    },

    enableCheckbox: function () {

        if (this.state.isMoving)
            return false;

        return [
                fdm.models.DownloadState.Downloading,
                fdm.models.DownloadState.WaitingForMetadata,
                fdm.models.DownloadState.Checking,
                fdm.models.DownloadState.Reconnecting,
                fdm.models.DownloadState.Paused,
                fdm.models.DownloadState.Pausing,
                fdm.models.DownloadState.Error
            ].indexOf(this.state.state) >= 0;
    },

    launchFile: function () {

        app.controllers.downloads.launchItemById(this.state.id);
    },

    changePriority: function(type){

        if ( type == 'up' && this.state.priority != fdm.models.downloadPriority.High ){

            fdmApp.downloads.prioritizeDownloads({
                downloadIds: [ this.props.download.id ],
                new_priority: (this.state.priority+1)

            })
        }
        if ( type == 'down' && this.state.priority != fdm.models.downloadPriority.Low ){

            fdmApp.downloads.prioritizeDownloads({
                downloadIds: [ this.props.download.id ],
                new_priority: (this.state.priority-1)

            })
        }
    },

    getPreviewWrapper: function() {
        const row = ReactDOM.findDOMNode(this);
        let r = row.getElementsByClassName('yt_preview_wr');
        if (r && r.length) {
            return r[0];
        }
        return null;
    },

    showVideoImageTooltip: function(e){
        const download = this.props.download;
        const state = download.get('state');
        if (state===fdm.models.DownloadState.Error || state===fdm.models.DownloadState.Reconnecting
            || state===fdm.models.DownloadState.WaitingForMetadata
            || download.get('missingFiles') || download.get('missingStorage')
            || download.get('downloadType') === fdm.models.DownloadType.InfoRequest) {
            return;
        }
        let wripper = this.getPreviewWrapper();
        if (!wripper) {
            return;
        }
        const rect = wripper.parentNode.getBoundingClientRect();
        wripper.style.display = 'flex';
        wripper.style.top = (rect.top - 10) + 'px';
    },

    hideVideoImageTooltip: function(){
        let wripper = this.getPreviewWrapper();
        if (!wripper) {
            return;
        }
        wripper.style.display = 'none';
    },

    render: function() {

        var is_leaf = true;
        var is_open = false;
        var one_file = true;

        var download =  this.props.download;

        var filesColumnSize = this.props.filesColumnSize;
        var is_scheduled = download.get('isScheduled');

        var state = this.state.state;

        var enable_checkbox = this.enableCheckbox();

        var checked_checkbox = !(state == fdm.models.DownloadState.Paused
            || state == fdm.models.DownloadState.Paused || state == fdm.models.DownloadState.Error);

        var button_title = null;
        if (enable_checkbox)
            button_title = titleByActionButton(this.state);

        var no_size = download.isNoSize();
        if (!no_size && state == fdm.models.DownloadState.Downloading && this.state.isQueued)
            no_size = true;

        var requesting_info = download.get('downloadType') == fdm.models.DownloadType.InfoRequest;

        var show_error_message = state==fdm.models.DownloadState.Error || state==fdm.models.DownloadState.Reconnecting
            || this.state.missingFiles || this.state.missingStorage
            || state === fdm.models.DownloadState.Paused && this.state.pauseReason === fdm.models.pauseReason.LowDiskSpace;

        return (

            React.createElement("li", {className: rjs_class({
                    tree_node: true,
                    opened: is_open,
                    file: is_leaf,
                    once_file: one_file,
                    folder: !is_leaf,
                    'tree-node-selected': this.props.selected,
                    current: this.props.current,
                    timer_state: is_scheduled
            }), 
                "data-id": download.id, 
                onMouseDown: this.onDownloadMouseDown, 
                onDoubleClick: stopEventBubble}, 
                React.createElement("div", {className: "triangle"}), 
                React.createElement("div", {className: "file-name", style: {width: (filesColumnSize.fileName + (one_file ? 20 : 0)) + 'px'}}, 
                            React.createElement("span", {className: "pad", 
                                title: requesting_info ? this.state.url : this.state.fileName, 
                                onMouseOver: this.showVideoImageTooltip, 
                                onMouseOut: this.hideVideoImageTooltip, 
                                onMouseDown: this.onDownloadMouseDown, 
                                onDoubleClick: this.launchFile, 
                                onContextMenu: this.onContextMenu}, 

                                React.createElement("span", {
                                    onMouseDown: stopEventBubble, 
                                    onClick: this.checkboxClick, 
                                    title: button_title, 
                                    onDoubleClick: stopEventBubble, 
                                    className: rjs_class({
                                        fake_checkbox: true,
                                        checked: checked_checkbox,
                                        //indeterminate: checked === undefined,
                                        disabled: !enable_checkbox
                                    })}), 

                                requesting_info ? this.state.url : this.state.fileName

                            )
                ), 
                React.createElement("div", {className: "file-size", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.fileSize + 'px'}}, 
                            React.createElement("span", {className: "rubber-ellipsis", 
                                  onMouseDown: this.onDownloadMouseDown, 
                                  onDoubleClick: this.launchFile, 
                                  onContextMenu: this.onContextMenu}, 

                                fdm.sizeUtils.bytesAsText(this.state.totalBytes)
                            )
                ), 
                React.createElement("div", {className: "file-progress", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.fileProgress + 'px'}}, 
                            React.createElement("span", {className: "pad", 
                                  onMouseDown: this.onDownloadMouseDown, 
                                  onDoubleClick: this.launchFile, 
                                  onContextMenu: this.onContextMenu}, 

                                state==fdm.models.DownloadState.Error || state==fdm.models.DownloadState.Reconnecting
                                    || this.state.missingFiles || this.state.missingStorage ?

                                    React.createElement("span", {className: "progress-pre-wrap"}, 
                                        /*<span className="progress-wrap">
                                         <span className="progress" style={{width: (this.state.progress < 0 ? 0 : this.state.progress) + '%'}}></span>
                                         </span>*/
                                        React.createElement("span", {className: "percents"}, 
                                            React.createElement("span", {title: download.getErrorText(), 
                                                  className: rjs_class({
                                                      'error-message': true,
                                                      left: this.state.isMoving
                                                  })}, download.getErrorText())
                                        )
                                    )

                                : state === fdm.models.DownloadState.Paused && this.state.pauseReason === fdm.models.pauseReason.LowDiskSpace ?
                                        React.createElement("span", {className: "progress-pre-wrap"}, 
                                            React.createElement("span", {className: "percents"}, 
                                                React.createElement("span", {className: rjs_class({
                                                    'error-message': true,
                                                    left: this.state.isMoving
                                                })}, 
                                                    __('Low disk space')
                                                )
                                            )
                                        )
                                : no_size ?

                                    React.createElement("span", {className: "progress-pre-wrap"}, 
                                        /*<span className="progress-wrap">
                                        <span className="progress" style={{width: (this.state.progress < 0 ? 0 : this.state.progress) + '%'}}></span>
                                        </span>*/
                                        React.createElement("span", {className: "percents"}, 
                                            download.getNoSizeStatus()
                                        )
                                    )

                                :
                                    React.createElement("span", {className: "progress-pre-wrap"}, 
                                        React.createElement("span", {className: "progress-wrap"}, 
                                            React.createElement("span", {className: "progress", style: {width: (this.state.progress < 0 ? 0 : this.state.progress) + '%'}})
                                        ), 
                                    React.createElement("span", {className: "percents", style: {display: this.state.progress < 0 ? 'none' : 'block'}}, 
                                        Math.floor(this.state.progress) + '%')
                                    )
                                

                            )
                ), 
                React.createElement("div", {className: "file-priority", style: {display: is_leaf ? 'block' : 'none', width: filesColumnSize.filePriority + 'px'}}, 
                            React.createElement("span", {className: "rubber-ellipsis", 
                                  onMouseDown: this.onDownloadMouseDown, 
                                  onDoubleClick: this.launchFile, 
                                  onContextMenu: this.onContextMenu}, 
                                this.state.priority == fdm.models.downloadPriority.Low ? __('Low') :
                                    this.state.priority == fdm.models.downloadPriority.Normal ? __('Normal') :
                                        this.state.priority == fdm.models.downloadPriority.High ? __('High') : null
                            ), 

                    checked_checkbox && state!=fdm.models.DownloadState.Completed ?
                    React.createElement("div", {className: "priority_buttons"}, 
                        React.createElement("div", {className: rjs_class({
                            priority_button_up: true, disabled: this.state.priority == fdm.models.downloadPriority.High
                        }), onClick: _.partial(this.changePriority, 'up')}), 
                        React.createElement("div", {className: rjs_class({
                            priority_button_down: true, disabled: this.state.priority == fdm.models.downloadPriority.Low
                        }), onClick: _.partial(this.changePriority, 'down')}), 
                        React.createElement("div", {className: "priority_button_text"}, __('Priority'))
                    )
                        : null

                ), 

                React.createElement("div", {className: "yt_preview_wr"}, 
                    React.createElement("img", {src: download.getThumbnailUrl('bottom-panel'), alt: ""})
                )

            )

        );


    }
});


var ProgressMap = React.createClass({displayName: "ProgressMap",

    dispatcherIndex: 0,
    getInitialState: function() {

        var state = {};
        state.background = {
            columns_count: 0,
            rows_count: 0
        };

        state.visible = app.controllers.bottomPanel.model.get('panelVisible')
            && app.controllers.bottomPanel.model.get('currentTab') == fdm.views.BottomPanelTab.Progress
            && app.controllers.bottomPanel.model.get('currentItemId') > 0;

        state.map = app.controllers.bottomPanel.model.get('progressMap');

        return state;
    },

    componentDidMount: function(){

        _.bindAll(this, 'onResize');

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){

                if (payload.action.actionType == 'changeBottomPanelHeight'){
                    this.onResize();
                    return true;
                }
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

        window.addEventListener('resize', this.onResize);
        app.controllers.bottomPanel.model.on('change:currentItemId change:currentItemDownloadType', this.changeCurrentItem, this);
        app.controllers.bottomPanel.model.on('change:currentTab', this.onResize, this);
        app.controllers.bottomPanel.model.on('change:progressMap', this.refreshMap, this);
        app.controllers.bottomPanel.model.on('change:panelVisible change:currentTab', this.changePanelVisible, this);

        this.calcBackgroundSize();
    },

    componentWillUnmount:function(){

        window.removeEventListener('resize', this.onResize);
        app.controllers.bottomPanel.model.off('change:currentItemId change:currentItemDownloadType', this.changeCurrentItem, this);
        app.controllers.bottomPanel.model.off('change:currentTab', this.onResize, this);
        app.controllers.bottomPanel.model.off('change:progressMap', this.refreshMap, this);
        app.controllers.bottomPanel.model.off('change:panelVisible change:currentTab', this.changePanelVisible, this);

        FdmDispatcher.unregister(this.dispatcherIndex);
    },

    onResize: function () {

        this.calcBackgroundSize();
    },

    changePanelVisible: function () {

        var visible = app.controllers.bottomPanel.model.get('panelVisible')
            && app.controllers.bottomPanel.model.get('currentTab') == fdm.views.BottomPanelTab.Progress
            && app.controllers.bottomPanel.model.get('currentItemId') > 0;

        this.setState({
            visible: visible
        });

        if (visible)
            _.defer(this.calcBackgroundSize);
    },

    changeCurrentItem: function () {

        this.refreshMap();
        this.changePanelVisible();
    },

    refreshMap: function () {

        this.setState({
            map: app.controllers.bottomPanel.model.get('progressMap')
        });
    },

    pointSize: 10,

    calcBackgroundSize: function () {

        if (!this.state.visible)
            return;

        try{
            var row = ReactDOM.findDOMNode(this);
        }catch (e){
            return;
        }

        if (!row)
            return;
        var wr = row.getElementsByClassName('progress_background_wrapper');

        if (!wr || !wr.length)
            return;

        wr = wr[0];

        var rect = wr.getBoundingClientRect();

        var background = {};

        background.columns_count = Math.floor((rect.width - 8) / this.pointSize);
        background.rows_count = Math.floor((rect.height - 8) / this.pointSize);

        this.setState({
            background: background
        });

    },

    render: function () {

        if (!this.state.visible)
            return (
                React.createElement("div", {className: "log-info"}, 
                    React.createElement("div", {className: "progress_background_wrapper"})
                )
            );

        var map = this.state.map;

        /*
        if (map === false)
            return (
                <div className="log-info">
                    <div className="progress_background_wrapper"></div>
                </div>
            );
            */

        if (!map)
            map = {
                intervals: [1013],
                size: 1013,
            };

        var size = map.size;
        var core_intervals = map.intervals;

        if (core_intervals.length == 0)
            core_intervals = [size];

        if (!size || !core_intervals.length)
            return (
                React.createElement("div", {className: "log-info"}, 
                    React.createElement("div", {className: "progress_background_wrapper"})
                )
            );

        var map_count = size;
        var columns_count = this.state.background.columns_count;
        var rows_count = this.state.background.rows_count;

        var map_points_scale =  map_count > 0 ?  (columns_count * rows_count) / map_count : 0;

        var intervals = [];
        var interval = false;


        var current_start = 0;

        for (var i = 0; i < core_intervals.length; i++){

            var i_val = core_intervals[i];

            if (i_val <= map_count)
            {
                current_start += i_val;
                continue;
            }

            var i_length = i_val - (map_count + 1);

            interval = {start: current_start, mapSize: i_length};
            intervals.push(interval);
            current_start += i_length;
        }

        var point_size = this.pointSize;

        var current_row = 0;

        return (
            React.createElement("div", {className: "progress-info"}, 
                React.createElement("div", {className: "progress_background_wrapper"}), 
                React.createElement("div", {style: {
                        width: columns_count * point_size,
                        height: rows_count * point_size,
                        marginLeft: - (columns_count * point_size / 2),
                        // marginTop: - (rows_count * point_size / 2),
                    }, 
                    className: "progress_background"}, 

                intervals.map(function (interval, c) {

                    var i_left, i_width;

                    var row_start = current_row * columns_count;
                    var row_end = (current_row + 1) *  columns_count;
                    var interval_start = Math.round(interval.start * map_points_scale);
                    var interval_end = Math.round((interval.start + interval.mapSize) * map_points_scale);

                    var n = 0;
                    while (row_end < interval_start && n < 50){
                        n++;
                        current_row++;
                        row_start = current_row * columns_count;
                        row_end = (current_row + 1) *  columns_count;
                    }


                    var divs = [];

                    n = 0;
                    while (row_end < interval_end && n < 100){

                        i_left = Math.max(0, interval_start - row_start) * point_size;
                        i_width = Math.min(row_end - interval_start, row_end - row_start) * point_size;

                        divs.push(
                            React.createElement("div", {className: "progress_downloaded", 
                                 style: {
                                     top: current_row * point_size,
                                     left: i_left,
                                     width: i_width
                                 }})
                        );

                        n++;
                        current_row++;
                        row_start = current_row * columns_count;
                        row_end = (current_row + 1) * columns_count;
                    }

                    i_left = Math.max(0, interval_start - row_start) * point_size;
                    i_width = Math.min(interval_end - interval_start, row_end - interval_start, interval_end - row_start, row_end - row_start) * point_size;

                    divs.push(
                        React.createElement("div", {className: "progress_downloaded", 
                             style: {
                                 top: current_row * point_size,
                                 left: i_left,
                                 width: i_width
                             }})
                    )

                    return divs;

                })

                )



            )
        );
    }
});