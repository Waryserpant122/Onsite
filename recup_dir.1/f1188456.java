

var Toolbar = React.createClass({displayName: "Toolbar",

    mixins: [ToolbarDragMixin],

    toolbarDragId: 'toolbar',

    getInitialState: function() {
        var model = app.controllers.downloads.model;
        return {
            calculateChecksumOpened: app.controllers.calculateChecksumDialog.model.get('dialogOpened'),
            changeUrlDialogShown: app.controllers.downloads.model.get('changeUrlDialogShown') || app.controllers.downloads.model.get('changePathDialogShown'),
            activeFilterText: model.get('activeFilterText'),
            searchInFocus: false,
            downloadWizardOpened: false,
            deleteDialogOpened: model.get('showDeletePopupDialog'),
            deleteDialogChoice: model.get('deleteDialogChoice'),
            customSpeedDialogOpened: app.controllers.customSpeedDialog.model.get('dialogIsShown'),
            simpleDialogOpened: app.controllers.simpleDialogs.model.get('opened'),
            scheduleDialogOpened: app.controllers.scheduleDialog.model.get('dialogOpened'),
            menuOpened: app.controllers.menu.model.get('opened'),
            showMenuGreenMarker: app.controllers.menu.model.get('showMenuGreenMarker'),
            sharerOpened: app.controllers.menu.model.get('dialogOpened'),
            toolbarActions: app.controllers.downloads.model.get('toolbarActions').toJSON(),
            settingsOpened: app.controllers.settings.model.get('opened'),
            activeFilterTextSettings: app.controllers.settings.model.get('activeFilterText')
        };
    },

    componentDidMount: function() {

        app.controllers.downloadWizard.model.on('change:addSourcePageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloadWizard.model.on('change:sourceInfoPageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloads.model.on('change:showDeletePopupDialog change:deleteDialogChoice', this.deleteDialogChangeState, this);
        app.controllers.downloads.model.on('change:activeFilterText', this.activeFilterTextChange, this);
        app.controllers.customSpeedDialog.model.on('change:dialogIsShown', this.customSpeedDialogChangeState, this);
        app.controllers.simpleDialogs.model.on('change:opened', this.simpleDialogChangeState, this);
        app.controllers.scheduleDialog.model.on('change:dialogOpened', this.scheduleDialogChangeState, this);
        app.controllers.menu.model.on('change:opened change:showMenuGreenMarker', this.menuChange, this);
        app.controllers.sharer.model.on('change:dialogOpened', this.sharerChange, this);
        app.controllers.settings.model.on('change:opened change:activeFilterText', this.settingsChanges, this);

        var toolbarActions = app.controllers.downloads.model.get('toolbarActions').on('change', this.changeToolbarActions, this);

        app.controllers.calculateChecksumDialog.model.on('change:dialogOpened', this.calculateChecksumDialogChange, this);
        app.controllers.downloads.model.on('change:changeUrlDialogShown change:changePathDialogShown', this.changeUrlDialogState, this);
    },

    componentWillUnmount: function() {
        app.controllers.downloadWizard.model.off('change:addSourcePageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloadWizard.model.off('change:sourceInfoPageIsShown', this.downloadWizardChangeState, this);
        app.controllers.downloads.model.off('change:showDeletePopupDialog change:deleteDialogChoice', this.deleteDialogChangeState, this);
        app.controllers.downloads.model.off('change:activeFilterText', this.activeFilterTextChange, this);
        app.controllers.customSpeedDialog.model.off('change:dialogIsShown', this.customSpeedDialogChangeState, this);
        app.controllers.simpleDialogs.model.off('change:opened', this.simpleDialogChangeState, this);
        app.controllers.scheduleDialog.model.off('change:dialogOpened', this.scheduleDialogChangeState, this);
        app.controllers.menu.model.off('change:opened change:showMenuGreenMarker', this.menuChange, this);
        app.controllers.sharer.model.off('change:dialogOpened', this.sharerChange, this);
        app.controllers.settings.model.off('change:opened change:activeFilterText', this.settingsChanges, this);

        var toolbarActions = app.controllers.downloads.model.get('toolbarActions').off('change', this.changeToolbarActions, this);

        app.controllers.calculateChecksumDialog.model.off('change:dialogOpened', this.calculateChecksumDialogChange, this);
        app.controllers.downloads.model.off('change:changeUrlDialogShown change:changePathDialogShown', this.changeUrlDialogState, this);
    },

    calculateChecksumDialogChange: function(){

        this.setState({
            calculateChecksumOpened: app.controllers.calculateChecksumDialog.model.get('dialogOpened')
        });
    },

    changeUrlDialogState: function(){

        this.setState({
            changeUrlDialogShown: app.controllers.downloads.model.get('changeUrlDialogShown')
                || app.controllers.downloads.model.get('changePathDialogShown')
        });
    },

    settingsChanges: function(){

        this.setState({
            settingsOpened: app.controllers.settings.model.get('opened'),
            activeFilterTextSettings: app.controllers.settings.model.get('activeFilterText')
        });
    },

    changeToolbarActions: function(){

        this.setState({toolbarActions: app.controllers.downloads.model.get('toolbarActions').toJSON()});

    },

    downloadWizardChangeState: function(){

        var opened = app.controllers.downloadWizard.model.get('addSourcePageIsShown') || app.controllers.downloadWizard.model.get('sourceInfoPageIsShown');
        this.setState({downloadWizardOpened: opened});
    },

    deleteDialogChangeState: function(){

        this.setState({
            deleteDialogOpened: app.controllers.downloads.model.get('showDeletePopupDialog'),
            deleteDialogChoice: app.controllers.downloads.model.get('deleteDialogChoice')
        });
    },

    customSpeedDialogChangeState: function(){

        this.setState({deleteDialogOpened: app.controllers.customSpeedDialog.model.get('dialogIsShown')});
    },

    simpleDialogChangeState: function(){

        this.setState({
            simpleDialogOpened: app.controllers.simpleDialogs.model.get('opened')
        });
    },

    scheduleDialogChangeState: function(){

        this.setState({
            scheduleDialogOpened: app.controllers.scheduleDialog.model.get('dialogOpened')
        });
    },

    activeFilterTextChange: function(){
        this.setState({activeFilterText: app.controllers.downloads.model.get('activeFilterText')});
    },

    menuChange: function(){
        this.setState({
            menuOpened: app.controllers.menu.model.get('opened'),
            showMenuGreenMarker: app.controllers.menu.model.get('showMenuGreenMarker')
        });
    },

    sharerChange: function(){
        this.setState({sharerOpened: app.controllers.sharer.model.get('dialogOpened')});
    },

    searchChange: function(value){

        stopEventBubble(event);
        if (this.state.settingsOpened)
            app.controllers.settings.model.set('activeFilterText', value);
        else
            app.controllers.downloads.model.set('activeFilterText', value);
    },

    searchHandleChange: function(event) {
        var value = event.target.value;

        this.searchChange(value);
        stopEventBubble(event);
    },
    searchHandleKyeDown: function(e) {

        if (e.keyCode == 27){
            stopEventBubble(e);
            this.searchChange('');
            document.getElementById('search-input-text').blur();
        }
    },

    backClick: function(){

        app.controllers.settings.close();
    },

    searchClick: function(e){

        if (!this.state.searchInFocus){
            stopEventBubble(e);
            this.setState({searchInFocus: true});
            _.defer(function(){
                document.getElementById('search-input-text').focus();
            });
        }
    },

    checkSearchAutoFillInterval: 0,

    checkSearchAutoFill: function(){

        var value = document.getElementById('search-input-text').value;
        if (this.state.settingsOpened && value != this.state.activeFilterTextSettings
            || !this.state.settingsOpened && value != this.state.activeFilterText){
            this.searchChange(value);
        }
    },

    searchOnBlur: function(){

        this.setState({searchInFocus: false});
        app.controllers.downloads.view_model.setSearchLostFocus();

        clearTimeout(this.checkSearchAutoFillInterval);
    },

    searchOnFocus: function(){

        this.setState({searchInFocus: true});
        app.controllers.downloads.view_model.setSearchFocus();

        this.checkSearchAutoFillInterval = setInterval(_.bind(this.checkSearchAutoFill, this), 100);
    },

    openDownloadWizard: function(){

        app.controllers.downloads.fakeAdd();
    },

    render: function() {

        var view_model = app.controllers.downloads.view_model;
        var activeFilterText = this.state.activeFilterText;
        if (this.state.settingsOpened)
            activeFilterText = this.state.activeFilterTextSettings;

        var toolbar_actions = this.state.toolbarActions;

        var buttons = [];
        if (toolbar_actions.forAll){
            buttons.push(
                toolbar_actions.canBeDownloaded ?
                    React.createElement("div", {key: "start_all", className: "start-download", 
                         title: __('Start all'), onClick: _.bind(view_model.startAll, view_model)})
                    :
                    React.createElement("div", {key: "start_all_disable", className: "start-download disable", title: __('Start all')})
            );
            buttons.push(
                toolbar_actions.canBePaused ?
                    React.createElement("div", {key: "pause_all", className: "pause-download", 
                         title: __('Pause all'), onClick: _.bind(view_model.stopAll, view_model)})
                    :
                    React.createElement("div", {key: "pause_all_disable", className: "pause-download disable", title: __('Pause all')})
            );
        }
        else{
            buttons.push(
                toolbar_actions.canBeDownloaded ?
                    React.createElement("div", {key: "start_selection", className: "start-download selected", 
                         onClick: _.bind(view_model.startSelected, view_model), title: __('Start selected')})
                    :
                    React.createElement("div", {key: "start_selection_disable", className: "start-download disable selected", title: __('Start selected')})
            );
            buttons.push(
                toolbar_actions.canBePaused ?
                    React.createElement("div", {key: "pause_selection", className: "pause-download selected", 
                         title: __('Pause selected'), onClick: _.bind(view_model.stopSelected, view_model)})
                    :
                    React.createElement("div", {key: "pause_selection_disable", className: "pause-download disable selected", title: __('Pause selected')})
            );
        }

        var delete_button_text;

        switch (this.state.deleteDialogChoice){

            case fdm.models.deleteDialogChoice.fromDisk:

                if (fdmApp.platform == 'mac')
                    delete_button_text = __('Move to trash');
                else
                    delete_button_text = __('Delete files');
                break;

            case fdm.models.deleteDialogChoice.fromList:

                delete_button_text = __('Remove from list');
                break;

            default :

                if (!toolbar_actions.canBeRemoved)
                    delete_button_text = __('Delete files');
                else
                    delete_button_text = __('Delete selected');
        }


        return (
            React.createElement("div", {id: "toolbar", onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick, 
                 className: rjs_class({
                'header-nav': true,
                opacity: this.state.downloadWizardOpened || this.state.deleteDialogOpened
                    || this.state.customSpeedDialogOpened || this.state.calculateChecksumOpened
                    || this.state.simpleDialogOpened || this.state.scheduleDialogOpened || this.state.sharerOpened
                    || this.state.changeUrlDialogShown
            })}, 
                React.createElement("div", {id: "toolbar_disable_block", className: "disable_block"}), 
                React.createElement("div", {className: "button_back", onClick: this.backClick}), 
                React.createElement("div", {className: "add-download", onClick: this.openDownloadWizard, 
                    title: __('Add new download...')}, 
                    React.createElement("span", null)
                ), 

            /*<!-- test buttons --
                    <div className="test-buttons" data-bind="click: leakReadSettings, localeTitle:'Leak'" title="">
                        leak
                    </div>
                    <div className="test-buttons" onclick="window.location.reload();" title="Reload location">
                        reload
                    </div>
                    <div className="test-buttons" onclick="external.utils.reload();" title="External reload">
                        utils.reload
                    </div>
                    <div className="test-buttons" onclick="external.utils.emptyProcessWorkingSet();" title="Empty process working set">
                        empty ws
                    </div>
                    -- end test buttons -->*/

                    React.createElement("div", {className: "wrap_actions"}, 

                        buttons, 

                        /*
                        {button_actions.SelectionCanBeDownloaded ?
                            <div className="start-download"
                                title={__('Start all')} onClick={_.bind(view_model.startAll, view_model)} ></div>
                            :
                            <div className="start-download disable" title={__('Start all')}></div>
                        }

                        {button_actions.SelectionCanBePaused ?
                            <div className="pause-download"
                                title={__('Pause all')} onClick={_.bind(view_model.stopAll, view_model)}></div>
                            :
                            <div className="pause-download disable" title={__('Pause all')}></div>
                        }
                        {toolbar_actions.canBeDownloaded ?
                            <div className="start-download"
                                 onClick={_.bind(view_model.startSelected, view_model)} title={__('Start selected')}></div>
                            :
                            <div className="start-download disable" title={__('Start selected')}></div>
                        }

                        {toolbar_actions.canBePaused ?
                            <div className="pause-download"
                                 title={__('Pause selected')} onClick={_.bind(view_model.stopSelected, view_model)}></div>
                            :
                            <div className="pause-download disable" title={__('Pause selected')}></div>
                        }
                        */

                        toolbar_actions.canBeRemoved ?
                            React.createElement("div", {className: rjs_class({
                                    'cancel-download': true,
                                    selected: !toolbar_actions.forAll
                                }), title: delete_button_text, 
                                 onClick: _.bind(view_model.removeSelected, view_model)})
                            :
                            React.createElement("div", {className: rjs_class({
                                    'cancel-download': true,
                                    disable: true,
                                    selected: !toolbar_actions.forAll
                                }), title: delete_button_text})
                        

                    ), 

                toolbar_actions.canBeMovable ?
                    React.createElement("div", {className: rjs_class({
                        action_folder: true,
                        selected: !toolbar_actions.forAll
                    }), 
                         title: __('Move to...'), onClick: _.bind(view_model.moveSelected, view_model)})
                    :
                    React.createElement("div", {className: "action_folder disable", title: __('Move to...')}), 
                
                    /*<!--Search: start-->*/
                React.createElement(ReactCSSTransitionGroup, {transitionName: "header", 
                                         transitionEnterTimeout: 500, transitionLeaveTimeout: 300}, 
                    React.createElement("div", {title:  this.state.searchInFocus || activeFilterText ? '' : __('Search'), 
                        className: rjs_class({
                        'header-search': true,
                        show_search: this.state.searchInFocus || activeFilterText || this.state.settingsOpened,
                        hide_search: !(this.state.searchInFocus || activeFilterText || this.state.settingsOpened)
                    }), 
                        onMouseDown: this.searchClick}, 



                        React.createElement("input", {className: "input-text", id: "search-input-text", type: "search", name: "", autoComplete: "off", placeholder: __('Search'), results: "5", 
                            onMouseDown: function(e){if (fdmApp.platform == 'mac' && ( e.button != 0 || e.button == 0 && e.ctrlKey) )fdmApp.menuManager.closePopupMenu();}, 
                            onFocus: this.searchOnFocus, 
                            onBlur: this.searchOnBlur, 
                            onChange: this.searchHandleChange, value: activeFilterText, onKeyDown: this.searchHandleKyeDown})


                    )
                ), 
                     /*<!--Search: END-->*/

                    React.createElement(Updater, null), 

                fdmApp.isFake ?

                    React.createElement("div", null, 
                        React.createElement("div", {className: "wrapper-add-tags"}, 
                            React.createElement("a", {href: "#", onClick: fdmAppFakes.updater.next, title: "next update status (testing)"}, " next ")
                        )
                    )

                    : null, 


                     /*<!--Add tags: end-->*/
                     /*<!--VIEWS AND SETTINGS: START-->*/
                    React.createElement("div", {className: "views-settings"}, 
                     /*<!--<div className="btn-updates">
                             <span className="btn-updates-icon"></span>
                         </div>-->*/


                        React.createElement("div", {id: "button-menu", className: rjs_class({
                            button_menu: true,
                            pushed: this.state.menuOpened,
                            notification: this.state.showMenuGreenMarker
                        }), 
                             title: __('Main menu'), 
                            onClick: function(){view_model.openMainMenu(); return false}, 
                            "data-bind": "clickBubble: false"}, 
                            React.createElement("span", {className: "settings-icon"})
                        )
                    )
                    /*<!--VIEWS AND SETTINGS: END-->*/

                )
        );
    }
});
