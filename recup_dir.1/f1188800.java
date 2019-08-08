
var Updater= React.createClass({displayName: "Updater",

    mixins: [ToolbarDragMixin],

    toolbarDragId: 'download-wiz-add-source',

    getInitialState: function () {

        var state = app.controllers.updater.model.toJSON();
        state.modalDialogOpened = app.controllers.downloadWizard.model.get('addSourcePageIsShown')
            || app.controllers.downloadWizard.model.get('sourceInfoPageIsShown');

        return state;
    },

    _onChange: function() {

        this.setState(app.controllers.updater.model.toJSON());
    },

    componentDidMount: function() {

        app.controllers.downloadWizard.model.on('change:addSourcePageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloadWizard.model.on('change:sourceInfoPageIsShown', this.downloadWizardChangeState, this);
        app.controllers.updater.model.on("change", this._onChange, this);
    },

    componentWillUnmount: function() {

        app.controllers.downloadWizard.model.off('change:addSourcePageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloadWizard.model.off('change:sourceInfoPageIsShown', this.downloadWizardChangeState, this);
        app.controllers.updater.model.off("change", this._onChange, this);
    },

    downloadWizardChangeState: function () {

        this.setState({
            modalDialogOpened: app.controllers.downloadWizard.model.get('addSourcePageIsShown')
            || app.controllers.downloadWizard.model.get('sourceInfoPageIsShown')
        });
    },

    showDialog: function(){
        app.controllers.updater.model.set("dialogShown", true);
    },

    closeDialog: function(e){
        stopEventBubble(e);
        app.controllers.updater.model.set("dialogShown", false);
    },

    resetClick: function(){

        app.controllers.updater.model.reset();
        app.controllers.updater.model.set("dialogShown", false);
    },

    downloadUpdates: function(){

        app.controllers.updater.model.downloadUpdates();
    },

    render: function () {

        if (fdmApp.appUpdateDisabled)
            return null;

        var m = app.controllers.updater.model;

        var waiting =
            (
                (
                    this.state.stage == fdm.models.UpdaterStages.check_updates
                    || this.state.stage == fdm.models.UpdaterStages.download_updates
                    || this.state.stage == fdm.models.UpdaterStages.post_download_check
                    || this.state.stage == fdm.models.UpdaterStages.pre_install_check
                    || this.state.stage == fdm.models.UpdaterStages.install_updates
                )
                && this.state.state == fdm.models.UpdaterStates.in_progress
            )
            ||
            (
                (
                    this.state.stage == fdm.models.UpdaterStages.download_updates
                    || this.state.stage == fdm.models.UpdaterStages.post_download_check
                    || this.state.stage == fdm.models.UpdaterStages.pre_install_check
                    || this.state.stage == fdm.models.UpdaterStages.install_updates
                )
                && this.state.state == fdm.models.UpdaterStates.ready
            )
            ||
            (
                (
                    this.state.stage == fdm.models.UpdaterStages.download_updates
                    || this.state.stage == fdm.models.UpdaterStages.pre_install_check
                )
                && this.state.state == fdm.models.UpdaterStates.finished
            );

        if (this.state.stage == fdm.models.UpdaterStages.check_updates
            && this.state.state == fdm.models.UpdaterStates.ready){
            return null;
        }


        if (!this.state.dialogShown || this.state.modalDialogOpened){

            if (this.state.stage == fdm.models.UpdaterStages.check_updates
                && !(this.state.state == fdm.models.UpdaterStates.finished && this.state.updatesAvailable))
                return null;

            var title = __('Update');
            if (this.state.stage == fdm.models.UpdaterStages.check_updates &&
                (this.state.state != fdm.models.UpdaterStates.finished
                    || !this.state.updatesAvailable))
                title = __('Check for updates');

            return (
                React.createElement("div", {title: title, 
                     onClick: this.showDialog, className: "update_button"}, 
                    React.createElement("div", {className: 'update_icon' + (waiting ? ' waiting' : '') })
                )
            );
        }

        var error_class = this.state.state == fdm.models.UpdaterStates.failed
            && (this.state.stage != fdm.models.UpdaterStages.pre_install_check
                || this.state.getPreInstallCheckFailureReason != 1);

        var progress = 0;
        if (this.state.state == fdm.models.UpdaterStates.in_progress
            && this.state.stage == fdm.models.UpdaterStages.download_updates){

            if (this.state.progress.current > 0 && this.state.progress.max > 0){
                progress =  Math.round(this.state.progress.current/this.state.progress.max * 100);
            }
        }

        return (

            React.createElement("div", {onClick: this.closeDialog, onMouseDown: this.closeDialog, className: "update_button pushed"}, 
                React.createElement("div", {className: 'update_icon' + (waiting ? ' waiting' : '') }), 

                React.createElement("div", {id: "download-wiz-add-source", 
                     onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick, 
                     className: "trnsprtnt_for_updates"}), 

                React.createElement("div", {className: "transparent_updates", style: {display: 'block'}, 
                     onContextMenu: this.closeDialog, onClick: this.closeDialog}), 

                React.createElement("div", {onClick: stopEventBubble, onMouseDown: stopEventBubble, className: rjs_class({
                    wrapper_updates: true,
                    error: error_class
                })}, 

                     this.state.stage == fdm.models.UpdaterStages.check_updates
                        && !( (+ new Date() ) - this.state.preDownloadCheck <= 60000
                    || (+ new Date() ) - this.state.lastDownloadClick <= 10000 ) ?


                        React.createElement("div", null, 

                             this.state.state == fdm.models.UpdaterStates.ready ?

                                React.createElement("div", null, 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.checkForUpdates, m)}, __('Check for updates'))
                                )

                                : null, 
                             this.state.state == fdm.models.UpdaterStates.in_progress ?

                                React.createElement("div", {className: "checking"}, 
                                    React.createElement("div", null, __('Checking for updates...')), React.createElement("br", null), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "compact-progress-line"})
                                    )
                                )

                                : null, 
                             this.state.state == fdm.models.UpdaterStates.failed ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, 
                                        React.createElement("span", null, __('Error:')), 
                                        React.createElement("div", {className: "errors_msg", title: this.state.getLastErrorDescription}, 
                                            this.state.getLastErrorDescription
                                            )
                                    ), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.checkForUpdates, m)}, __('Retry'))
                                )

                                : null, 
                             this.state.state == fdm.models.UpdaterStates.finished ?

                                React.createElement("div", null, 

                                     this.state.updatesAvailable ?

                                        React.createElement("div", null, 
                                            React.createElement("div", null, __('New version is available')), 
                                            React.createElement("div", {className: "btn", onClick: this.downloadUpdates}, __('Update'))
                                        )

                                        :

                                        React.createElement("div", null, 
                                            React.createElement("div", null, __('FDM is up to date!')), 
                                             (+ new Date()) - this.state.lastFinishedCheckForUpdates < 600000 ?
                                                React.createElement("div", {className: "btn", onClick: this.closeDialog}, __('OK'))
                                                :
                                                React.createElement("div", {className: "btn", onClick: _.bind(m.checkForUpdates, m)}, __('Check for updates'))
                                            
                                        )

                                    


                                )

                                : null


                        )

                        : null, 

                     this.state.stage == fdm.models.UpdaterStages.check_updates
                        && ( (+ new Date() ) - this.state.preDownloadCheck <= 60000
                            || (+ new Date() ) - this.state.lastDownloadClick <= 10000 ) ?

                        React.createElement("div", {className: "downloading"}, 
                            React.createElement("span", null, __('Downloading update')), React.createElement("br", null), 
                            React.createElement("div", null, 
                                React.createElement("div", {className: "info-time"}, 
                                    React.createElement("span", {className: "percents"}, "0%")
                                ), 
                                React.createElement("div", {className: "compact-progress-line"}, 
                                    React.createElement("div", {className: "compact-download-progress", style: {width: '0%', display: 'block'}})
                                ), 
                                React.createElement("a", {href: "#", onClick: this.resetClick, className: "cancel_btn"})
                            )
                        )

                        : null, 

                     this.state.stage == fdm.models.UpdaterStages.download_updates ?


                        React.createElement("div", null, 

                             this.state.state == fdm.models.UpdaterStates.ready ?

                                React.createElement("div", {className: "downloading"}, 
                                    React.createElement("span", null, __('Downloading update')), React.createElement("br", null), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "info-time"}, 
                                            React.createElement("span", {className: "percents"}, "0%")
                                        ), 
                                        React.createElement("div", {className: "compact-progress-line"}, 
                                            React.createElement("div", {className: "compact-download-progress", style: {width: '0%', display: 'block'}})
                                        ), 
                                            React.createElement("a", {href: "#", onClick: this.resetClick, className: "cancel_btn"})
                                    )
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.in_progress ?


                                React.createElement("div", {className: "downloading"}, 
                                    React.createElement("div", null, __('Downloading update')), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "info-time"}, 
                                            React.createElement("span", {className: "percents"}, progress + '%')
                                        ), 
                                        React.createElement("div", {className: "compact-progress-line"}, 
                                            React.createElement("div", {className: "compact-download-progress", 
                                                 style: {width: progress + '%'}})
                                        ), 
                                            React.createElement("a", {href: "#", onClick: this.resetClick, className: "cancel_btn"})
                                    )
                                )

                                : null, 


                             this.state.state == fdm.models.UpdaterStates.failed ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, 
                                        React.createElement("span", null, __('Error downloading update:')), 
                                        React.createElement("div", {className: "errors_msg", title: this.state.getLastErrorDescription}, 
                                            this.state.getLastErrorDescription
                                            )
                                    ), 
                                    React.createElement("div", {className: "btn left", onClick: _.bind(m.checkForUpdates, m)}, __('Retry')), 
                                    React.createElement("div", {className: "btn right", onClick: this.resetClick}, __('Cancel'))
                                )

                                : null, 
                             this.state.state == fdm.models.UpdaterStates.finished ?

                                React.createElement("div", {className: "downloading"}, 
                                    React.createElement("div", null, __('Downloading update')), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "info-time"}, 
                                            React.createElement("span", {className: "percents"}, "100%")
                                        ), 
                                        React.createElement("div", {className: "compact-progress-line"}, 
                                            React.createElement("div", {className: "compact-download-progress", 
                                                 style: {width: '100%', display: 'block'}})
                                        ), 
                                            React.createElement("a", {href: "#", onClick: this.resetClick, className: "cancel_btn"})
                                    )
                                )

                                : null

                        )

                        : null, 

                     this.state.stage == fdm.models.UpdaterStages.post_download_check ?

                        React.createElement("div", null, 

                             this.state.state == fdm.models.UpdaterStates.ready
                            || this.state.state == fdm.models.UpdaterStates.in_progress
                            || (+ new Date() ) - this.state.lastDownloadClick < 5 * 60000
                                && this.state.state != fdm.models.UpdaterStates.failed ?

                                React.createElement("div", {className: "checking"}, 
                                    React.createElement("div", null, __('Checking...')), React.createElement("br", null), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "compact-progress-line"})
                                    )
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.failed ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, 
                                        React.createElement("span", null, __('Error:')), 
                                        React.createElement("div", {className: "errors_msg", title: this.state.getLastErrorDescription}, 
                                            this.state.getLastErrorDescription
                                            )
                                    ), 
                                    React.createElement("div", {className: "btn left", onClick: _.bind(m.checkForUpdates, m)}, __('Retry')), 
                                    React.createElement("div", {className: "btn right", onClick: this.resetClick}, __('Cancel'))
                                )

                                : null, 
                             this.state.state == fdm.models.UpdaterStates.finished
                            && (+ new Date() ) - this.state.lastDownloadClick >= 5 * 60000 ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, __('Update downloaded')), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.installUpdates, m)}, __('Install update'))
                                )

                                : null

                        )


                        : null, 

                     this.state.stage == fdm.models.UpdaterStages.pre_install_check ?


                        React.createElement("div", null, 

                             this.state.state == fdm.models.UpdaterStates.ready
                                || this.state.state == fdm.models.UpdaterStates.in_progress
                                || this.state.state == fdm.models.UpdaterStates.finished?

                                React.createElement("div", {className: "checking"}, 
                                    React.createElement("div", null, __('Installing updates')), React.createElement("br", null), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "compact-progress-line"})
                                    )
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.failed
                                && this.state.getPreInstallCheckFailureReason == 1? //enum class failure_reason {unspecified, module_running}

                                React.createElement("div", null, 
                                    React.createElement("div", null, __('Relaunch FDM to update')), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.shutdownUpdateTargetModule, m)}, __('Relaunch'))
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.failed
                            && this.state.getPreInstallCheckFailureReason != 1 ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, 
                                        React.createElement("span", null, __('Error occurred during installation:')), 
                                        React.createElement("div", {className: "errors_msg", title: this.state.getLastErrorDescription}, 
                                            this.state.getLastErrorDescription
                                            )
                                    ), 
                                    React.createElement("div", {className: "btn left", onClick: _.bind(m.checkForUpdates, m)}, __('Retry')), 
                                    React.createElement("div", {className: "btn right", onClick: this.resetClick}, __('Cancel'))
                                )

                                : null

                        )


                        : null, 

                     this.state.stage == fdm.models.UpdaterStages.install_updates ?


                        React.createElement("div", null, 

                             this.state.state == fdm.models.UpdaterStates.ready
                                || this.state.state == fdm.models.UpdaterStates.in_progress?

                                React.createElement("div", {className: "checking"}, 
                                    React.createElement("div", null, __('Installing updates')), React.createElement("br", null), 
                                    React.createElement("div", null, 
                                        React.createElement("div", {className: "compact-progress-line"})
                                    )
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.failed ?

                                React.createElement("div", null, 
                                    React.createElement("div", null, 
                                        React.createElement("span", null, __('Error occurred during installation:')), 
                                        React.createElement("div", {className: "errors_msg", title: this.state.getLastErrorDescription}, this.state.getLastErrorDescription)
                                    ), 
                                    React.createElement("div", {className: "btn left", onClick: _.bind(m.checkForUpdates, m)}, __('Retry')), 
                                    React.createElement("div", {className: "btn right", onClick: this.resetClick}, __('Cancel'))
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.finished
                                && (this.state.getRestartRequired == fdm.models.UpdaterRestartType.module)?

                                React.createElement("div", null, 
                                    React.createElement("div", null, __('Relaunch FDM to update')), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.performRestart, m)}, __('Relaunch'))
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.finished && false // not used
                                && (this.state.getRestartRequired == fdm.models.UpdaterRestartType.os)?


                                React.createElement("div", null, 
                                    React.createElement("div", null, __('Restart computer to complete installation')), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.performRestart, m)}, __('Restart'))
                                )

                                : null, 

                             this.state.state == fdm.models.UpdaterStates.finished
                                && (this.state.getRestartRequired == fdm.models.UpdaterRestartType.none)?

                                React.createElement("div", null, 
                                    React.createElement("div", null, __('FDM is up to date')), 
                                    React.createElement("div", {className: "btn", onClick: _.bind(m.checkForUpdates, m)}, __('Check for updates'))
                                )

                                : null

                        )

                        : null

                )
            )

        );

    }
});