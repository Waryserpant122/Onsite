

var TrafficPanel = React.createClass({displayName: "TrafficPanel",

    dispatcherIndexKeyDown: false,

    getInitialState: function () {

        var state = app.controllers.trafficPanel.model.toJSON();
        state.bottomPanelVisible = app.controllers.bottomPanel.model.get('panelVisible');

        state.currentItem = app.controllers.downloads.model.get('currentItem');
        state.countSelected = app.controllers.downloads.model.get('countSelected');
        state.selectedSize = app.controllers.downloads.model.get('selectedSize');
        state.lowDiskSpaceWarnings = app.controllers.downloads.model.get('lowDiskSpaceWarnings');
        state.transparentColorsShow = false;
        state.currentDownloadsLength = app.controllers.downloads.collections.currentDownloads.length;
        state.settingsOpened = app.controllers.settings.model.get('opened');

        state.tumSnailModeEnabled = app.controllers.settings.model.get('settings')['tum-snail-mode-enabled'];

        return state;
    },

    componentDidMount: function() {

        app.controllers.trafficPanel.model.on('change', this._onChange, this);
        app.controllers.bottomPanel.model.on('change:panelVisible', this._onChange, this);
        app.controllers.downloads.model.on('change:currentItem change:countSelected change:selectedSize', this.changeDownload, this);
        app.controllers.downloads.model.on('change:lowDiskSpaceWarnings', this.changeLowDiskSpaceWarnings, this);
        app.controllers.downloads.collections.currentDownloads.on('add reset remove', this.changeCurrentDownloads, this);

        app.controllers.settings.model.on('change:opened', this.changeSettings, this);
        app.controllers.settings.model.on('change', this.changeSnailMode, this);

        this.dispatcherIndexKeyDown = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
                if (payload.action.actionType == 'closeColoursDialog')
                    this.closeColoursDialog(payload.action.content);
                if (payload.action.actionType == 'showColoursDialog')
                    this.showColoursDialog(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function() {

        app.controllers.trafficPanel.model.off('change', this._onChange, this);
        app.controllers.bottomPanel.model.off('change:panelVisible', this._onChange, this);
        app.controllers.downloads.model.off('change:currentItem change:countSelected change:selectedSize', this.changeDownload, this);
        app.controllers.downloads.model.off('change:lowDiskSpaceWarnings', this.changeLowDiskSpaceWarnings, this);
        app.controllers.downloads.collections.currentDownloads.off('add reset remove', this.changeCurrentDownloads, this);

        app.controllers.settings.model.off('change:opened', this.changeSettings, this);
        app.controllers.settings.model.off('change', this.changeSnailMode, this);

        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);
    },

    changeLowDiskSpaceWarnings: function(){

        this.setState({
            lowDiskSpaceWarnings: app.controllers.downloads.model.get('lowDiskSpaceWarnings')
        });
    },

    changeSnailMode: function(){

        var snail_mode = app.controllers.settings.model.get('settings')['tum-snail-mode-enabled'];

        if (snail_mode != this.state.tumSnailModeEnabled)
            this.setState({tumSnailModeEnabled: snail_mode});
    },

    changeSettings: function(){

        this.setState({
            settingsOpened: app.controllers.settings.model.get('opened')
        });
    },

    _onChange: function(){

        var state = app.controllers.trafficPanel.model.toJSON();
        state.bottomPanelVisible = app.controllers.bottomPanel.model.get('panelVisible');

        this.setState(state);
    },

    changeDownload: function(){

        this.setState({
            currentItem:  app.controllers.downloads.model.get('currentItem'),
            countSelected:  app.controllers.downloads.model.get('countSelected'),
            selectedSize:  app.controllers.downloads.model.get('selectedSize')
        });
    },

    changeCurrentDownloads: function(){
        this.setState({currentDownloadsLength: app.controllers.downloads.collections.currentDownloads.length});
    },

    closeSpeedDialog: function(e){

        stopEventBubble(e);
        app.controllers.trafficPanel.closeSpeedDialog();
    },

    toggleSpeedDialog: function(e){

        stopEventBubble(e);
        app.controllers.trafficPanel.toggleSpeedDialog()
    },

    chooseSpeed: function(speed_type, e){

        stopEventBubble(e);
        app.controllers.trafficPanel.chooseSpeed(speed_type);
    },

    showBottomPanel: function(){

        app.controllers.trafficPanel.bottomPanelVisibility();
    },

    globalKeyDown: function(event){

        if (!this.state.speedDialogOpened)
            return;

        if (event.keyCode == 27){
            app.controllers.trafficPanel.closeSpeedDialog();
        }
    },

    closeColoursDialog: function(){

        this.setState({transparentColorsShow: false});
    },

    showColoursDialog: function(){

        this.setState({transparentColorsShow: true});
    },

    closeActionByTagPanel: function(){

        FdmDispatcher.handleViewAction({
            actionType: 'closeActionByTagPanel',
            content: {}
        });
    },

    snailModeToggle: function(){

        app.controllers.settings.saveSetting('tum-snail-mode-enabled', !this.state.tumSnailModeEnabled);
    },

    render: function() {

        var downloads_length = this.state.currentDownloadsLength;

        var selectedDownloadSpeed = this.state.customSpeedMode;
        var selectedUploadSpeed = this.state.customSpeedMode;

        return (
            React.createElement("div", null, 

                React.createElement("div", {className: "modal-dialog-layer", onClick: this.closeSpeedDialog, onContextMenu: this.closeSpeedDialog, 
                     style: {display: this.state.speedDialogOpened ? 'block' : 'none'}}), 

                React.createElement("div", {onClick: this.closeSpeedDialog, 
                    className: rjs_class({
                    'traffic-bar': true,
                    show_panel: this.state.speedDialogOpened
                })}, 

                    React.createElement("div", {style: {display: this.state.transparentColorsShow ? 'block' : 'none'}, 
                        id: "traffic_panel_transparent_colors", onClick: this.closeActionByTagPanel, className: "transparent_colors"}), 

                    React.createElement("div", {title: __('Snail mode frees bandwidth without stopping downloads.'), 
                        className: rjs_class({
                        snail: true
                    })}, 

                        React.createElement("div", {onClick: this.snailModeToggle, 
                             className: rjs_class({
                                 snail_button: true,
                                 pushed: this.state.tumSnailModeEnabled
                             })}), 

                        this.state.tumSnailModeEnabled ?
                            React.createElement("span", {className: "snail_text"}, __('Snail mode'))
                            : null

                    ), 

                    !this.state.tumSnailModeEnabled ?

                    React.createElement("ul", {id: "traffic-bar-btn"}, 
                        React.createElement("li", {className: "speed-chooser-block"}, 
                            React.createElement("ul", {className: "speed-chooser-wrap", onClick: this.toggleSpeedDialog}, 
                                React.createElement("li", {id: "traffic-down-speed-chooser", 
                                    className: rjs_class({
                                    open: this.state.speedDialogOpened,
                                    'down-speed-chooser' : true,
                                    manual: selectedDownloadSpeed != null && selectedDownloadSpeed == fdm.models.NetworkSpeedMode.Custom,
                                    low: selectedDownloadSpeed != null && selectedDownloadSpeed == fdm.models.NetworkSpeedMode.Low,
                                    medium: selectedDownloadSpeed != null && selectedDownloadSpeed == fdm.models.NetworkSpeedMode.Medium,
                                    high: selectedDownloadSpeed != null && selectedDownloadSpeed == fdm.models.NetworkSpeedMode.High
                                })}, 
                                React.createElement("span", {className: "current-down-speed"}, 
                                fdm.speedUtils.speed2SignDigits(this.state.totalDownloadSpeed)
                                ), 

                                    this.state.speedDialogOpened ?

                                        React.createElement("ul", {className: "speed-chooser"}, 

                                             app.controllers.trafficPanel.collections.downloadSpeedTypes.models.map(function(speed_type){

                                                return (
                                                    React.createElement("li", {key: speed_type.id, title: speed_type.get('title'), onClick: _.partial(this.chooseSpeed, speed_type), 
                                                        className: rjs_class({
                                                    selected : selectedDownloadSpeed == speed_type.id,
                                                    manual: speed_type.id == fdm.models.NetworkSpeedMode.Custom,
                                                    low: speed_type.id == fdm.models.NetworkSpeedMode.Low,
                                                    medium: speed_type.id == fdm.models.NetworkSpeedMode.Medium,
                                                    high: speed_type.id == fdm.models.NetworkSpeedMode.High
                                                })}, 
                                                        React.createElement("span", null, __(speed_type.get('text')))
                                                    )
                                                );

                                            }.bind(this)) 


                                        )

                                        : null

                                ), 


                                React.createElement("li", {id: "traffic-up-speed-chooser", 
                                    className: rjs_class({
                                    'up-speed-chooser' : true,
                                    manual: selectedUploadSpeed != null && selectedUploadSpeed == fdm.models.NetworkSpeedMode.Custom,
                                    low: selectedUploadSpeed != null && selectedUploadSpeed == fdm.models.NetworkSpeedMode.Low,
                                    medium: selectedUploadSpeed != null && selectedUploadSpeed == fdm.models.NetworkSpeedMode.Medium,
                                    high: selectedUploadSpeed != null && selectedUploadSpeed == fdm.models.NetworkSpeedMode.High
                                })}, 
                                React.createElement("span", {id: "traffic-current-upload-speed", className: "current-up-speed", 
                                      "data-bind": "clickBubble: false"}, 
                                    fdm.speedUtils.speed2SignDigits(this.state.totalUploadSpeed)
                                )
                                    /*
                                    <ul className="speed-chooser" id="speed-chooser-upload">


                                        { model.uploadSpeedTypes.map(function(speed_type){

                                            return (
                                                <li key={speed_type.id} title={speed_type.title} onClick={_.bind(model.chooseUploadSpeed, model, speed_type)}
                                                    className={rjs_class({
                                                    selected : selectedUploadSpeed == speed_type,
                                                    manual: speed_type.id == fdm.models.NetworkSpeedMode.Custom,
                                                    low: speed_type.id == fdm.models.NetworkSpeedMode.Low,
                                                    medium: speed_type.id == fdm.models.NetworkSpeedMode.Medium,
                                                    high: speed_type.id == fdm.models.NetworkSpeedMode.High
                                                })}>
                                                    <span>{speed_type.text}</span>
                                                </li>
                                            );

                                        }) }
                                    </ul>
                                    */
                                )

                            )
                        )
                    )

                        : null, 

                    function(){

                        if (this.state.lowDiskSpaceWarnings && this.state.lowDiskSpaceWarnings.length){

                            return (
                                React.createElement("div", {className: "wrapper_right"}, 

                                    React.createElement("div", {className: "download_title error"}, 
                                        __('Disk space needed:') + ' ', 

                                        React.createElement("span", null, 
                                            fdm.sizeUtils.bytesAsText(this.state.lowDiskSpaceWarnings[0].requiredSpace - this.state.lowDiskSpaceWarnings[0].availableSpace), 
                                            " ", __('on'), " ", 
                                            fdmApp.platform == 'win' ? __('disk %1', this.state.lowDiskSpaceWarnings[0].name) : this.state.lowDiskSpaceWarnings[0].name, 

                                            this.state.lowDiskSpaceWarnings.length > 1 ?

                                                React.createElement("span", null, 
                                                    " ", __('and'), " ", 
                                                    fdm.sizeUtils.bytesAsText(this.state.lowDiskSpaceWarnings[1].requiredSpace - this.state.lowDiskSpaceWarnings[1].availableSpace), 
                                                    " ", __('on'), " ", 
                                                    fdmApp.platform == 'win' ? __('disk %1', this.state.lowDiskSpaceWarnings[1].name) : this.state.lowDiskSpaceWarnings[1].name
                                                )
                                                : null

                                        ), 

                                        downloads_length > 0 && !this.state.settingsOpened ?
                                            React.createElement("div", {className: "bottom-panel-opener", onClick: this.showBottomPanel, 
                                                title: this.state.bottomPanelVisible ? __('Hide bottom panel') : __('Show bottom panel')})
                                            : null
                                    )

                                )
                            );
                        }

                        if (downloads_length > 0 && !this.state.settingsOpened){

                            return (
                                React.createElement("div", {className: "wrapper_right", onClick: this.showBottomPanel}, 

                                    this.state.countSelected ?
                                        React.createElement("div", {className: "download_title"}, 
                                            __('%n files selected.', this.state.countSelected) +
                                            (this.state.selectedSize > 0 ? ' ' + __('Total size:') + ' ' + fdm.sizeUtils.bytesAsText(this.state.selectedSize) : '')

                                        )
                                        : null, 

                                    this.state.currentItem && this.state.countSelected == 0 ?
                                        React.createElement("div", {className: "download_title"}, this.state.currentItem.get('fileName'))
                                        : null, 

                                    this.state.currentItem ?
                                        React.createElement("div", {className: "bottom-panel-opener", 
                                             title: this.state.bottomPanelVisible ? __('Hide details') : __('Show details')})
                                        : null
                                )
                            );
                        }

                    }.apply(this)

                )

            )
        );
    }
});