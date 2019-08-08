

var ChangePathDialog = React.createClass({displayName: "ChangePathDialog",

    mixins: [ButtonMixin, ToolbarDragMixin],

    toolbarDragId: 'js-change-path-dialog',

    dispatcherIndexKeyDown: false,

    getInitialState: function () {

        var state = {};
        state.opened = app.controllers.downloads.model.get('changePathDialogShown');
        state.download = app.controllers.downloads.model.get('changePathDialogDownload');
        state.startDownload = true;
        state.error = false;
        state.lastFolders = fdmApp.downloadWizard.getLastFolders();

        state.newDownloadPath = '';
        if (state.lastFolders.length > 0)
            state.newDownloadPath = state.lastFolders[0];

        state.newDownloadPathValid = false;
        state.newDownloadPathError = '';

        this.onChangeTargetFolder();

        return state;
    },

    _onChange: function() {

        var state = {};
        state.opened = app.controllers.downloads.model.get('changePathDialogShown');
        state.download = app.controllers.downloads.model.get('changePathDialogDownload');
        state.lastFolders = fdmApp.downloadWizard.getLastFolders();

        if (state.lastFolders.length > 0)
            state.newDownloadPath = state.lastFolders[0];
        else if (state.download)
            state.newDownloadPath = state.download.get('outputFilePath');

        if (this.state.newDownloadPath !== state.newDownloadPath)
        {
            state.newDownloadPathValid = false;
            state.newDownloadPathError = '';
        }

        this.onChangeTargetFolder();

        this.setState(state);
    },

    componentDidMount: function() {

        app.controllers.downloads.model.on('change:changePathDialogShown change:changePathDialogDownload', this._onChange, this);

        this.dispatcherIndexKeyDown = FdmDispatcher.register(function(payload) {

            if (!this.state.opened)
                return true;

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);

                if (payload.action.actionType == 'ChangePathDialogFolderCallback')
                    return this.openFolderDialogCallback(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function() {

        app.controllers.downloads.model.off('change:changePathDialogShown change:changePathDialogDownload', this._onChange, this);

        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);
    },

    globalKeyDown: function(content){

        if (content.keyCode === 27){
            this.close();
        }

    },

    close: function(){

        app.controllers.downloads.onChangePathCanceled();

    },

    submit: function(){

        var folder = this.state.newDownloadPath;

        if (folder === null)
        {
            this.setState({
                newDownloadPathValid: false,
                newDownloadPathError: '',
            });
        }
        else if (folder === "")

            this.setState({
                newDownloadPathValid: false,
                newDownloadPathError: __('Selected filepath is not valid.'),
            });
        else{

            fdmApp.system.validateFolder(folder, '', false, true, function(serialized){

                this.setState({
                    newDownloadPathValid: serialized[0],
                    newDownloadPathError: serialized[1],
                });

                if (serialized[0])
                    app.controllers.downloads.changePath(this.state.download.id, folder);

            }.bind(this));
        }
    },

    onKeyDown: function(e){

        if(e.keyCode === 27){

            stopEventBubble(e);
            this.close();
        }
        if(e.keyCode === 13){

            stopEventBubble(e);
            this.submit();
        }
    },

    changeStartDownload: function(e){

        this.setState({
            startDownload: e.target.checked
        });
    },

    divFolderSelectToggle: function(e){

        stopEventBubble(e);
        this.setState({
            divFolderSelectOpened: !this.state.divFolderSelectOpened
        });
    },
    divFolderSelectClose: function(e){

        stopEventBubble(e);

        if (this.state.divFolderSelectOpened)
            this.setState({divFolderSelectOpened: false});
    },
    lastFolderChange: function(value, e){

        stopEventBubble(e);

        this.setState({
            newDownloadPath: value
        });

        this.divFolderSelectClose();
        this.onChangeTargetFolder();
    },
    targetFolderChange: function(event) {

        this.setState({
            newDownloadPath: event.target.value
        });
        this.onChangeTargetFolder();
    },

    validateFolderTimeout: false,

    onChangeTargetFolder: function(){

        this.setValidateFolderTimeout();
    },

    setValidateFolderTimeout: function(){
        if (this.validateFolderTimeout)
            clearTimeout(this.validateFolderTimeout);

        this.validateFolderTimeout = setTimeout(function(){

            var folder = this.state.newDownloadPath;

            if (folder === null)
            {
                this.setState({
                    newDownloadPathValid: false,
                    newDownloadPathError: '',
                });
            }
            else if (folder === "")

                this.setState({
                    newDownloadPathValid: false,
                    newDownloadPathError: __('Selected filepath is not valid.'),
                });
            else{
                fdmApp.system.validateFolder(folder, '', false, true, function(serialized){

                    this.setState({
                        newDownloadPathValid: serialized[0],
                        newDownloadPathError: serialized[1],
                    });

                }.bind(this));
            }

        }.bind(this), 1000);
    },

    openFolderDialog: function(){

        fdmApp.system.openFolderDialog( this.state.newDownloadPath, 'change-path-dialog' );
    },

    openFolderDialogCallback: function(folder){

        this.setState({
            newDownloadPath: folder.targetFolder
        });
        this.onChangeTargetFolder();
    },

    render: function() {

        if (!this.state.opened || !this.state.download)
            return null;

        var download = this.state.download;

        var lastFolders = this.state.lastFolders || [];
        var targetFolder = this.state.newDownloadPath;

        return (
            React.createElement("div", {onKeyDown: this.onKeyDown, 
                 id: "js-change-path-dialog", 
                 onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick, 
                 className: "temporary-style download-wiz-source-info popup__overlay change_path with_note"}, 
                React.createElement("div", {className: "mount"}), 
                React.createElement("div", {className: "popup"}, 
                    React.createElement("div", {className: "header"}, 
                        React.createElement("div", null, __('Change download path')), 
                        React.createElement("div", {className: "close_button"})
                    ), 
                    React.createElement("div", {className: "center"}, 
                        React.createElement("div", null, 
                            React.createElement("div", {className: "center_left"}, 
                                React.createElement("div", {className: "saveto"}, 
                                    React.createElement("span", {className: "title-input"}, __('Enter new path:')), 
                                    React.createElement("div", {className: "cont_form"}, 
                                        React.createElement("div", {className: "clear"}, 
                                            React.createElement("div", {id: "containerTargetFolder", className: "inselect wrapper_inselect", onClick: this.divFolderSelectToggle}, 

                                                React.createElement("div", {className: "input_wrapper", onClick: stopEventBubble}, 
                                                    React.createElement("input", {value: targetFolder, defaultValue: targetFolder, onChange: this.targetFolderChange, 
                                                           id: "targetFolder", autoComplete: "off", spellCheck: "false", type: "text", className: "select-text", 
                                                           onFocus: function(){document.getElementById('containerTargetFolder').className='inselect wrapper_inselect focus'}, 
                                                           onBlur: function(){document.getElementById('containerTargetFolder').className='inselect wrapper_inselect'}})
                                                ), 

                                                React.createElement("div", {className: "transparent_select", style: {display: this.state.divFolderSelectOpened ? 'block' : 'none'}, 
                                                     onClick: this.divFolderSelectClose}), 
                                                React.createElement("div", {className: "dropdown_button"}), 

                                                 this.state.divFolderSelectOpened ?
                                                    React.createElement("div", {className: "list"}, 

                                                        lastFolders.map(function(folder, index){

                                                            if (folder == '')
                                                                return;

                                                            return (
                                                                React.createElement("div", {key: index, onClick: _.partial(this.lastFolderChange, folder)}, React.createElement("span", null, folder))
                                                            );
                                                        }.bind(this))

                                                    )
                                                    : null

                                            ), 
                                            React.createElement("button", {className: "button_folder linkblock", title: __('Select folder'), 
                                                    onClick: this.openFolderDialog, 
                                                    onMouseDown: this.buttonMouseDown})
                                        ), 
                                        React.createElement("label", {className: "link_name for_copy"}, __('Old file location:'), " ", React.createElement("span", {className: "for_copy"}, download.get('outputFilePath')))
                                    )
                                )
                            )
                        ), 
                        React.createElement("div", {className: "note_info"}, 
                            React.createElement("span", null, __('Disk unavailable. Choose new download path.'))
                        )
                    ), 
                    React.createElement("div", {className: "bottom"}, 

                        React.createElement("span", {className: "error-message"}, 
                            React.createElement("span", null, this.state.newDownloadPathError)
                        ), 

                        React.createElement("div", {className: "group_button"}, 

                            React.createElement("button", {className: "left_button cancel linkblock", onClick: this.close, 
                                    onMouseDown: this.buttonMouseDown}, __('Cancel')), 
                            React.createElement("button", {className: "right_button linkblock", onClick: this.submit, 
                                    disabled: !this.state.newDownloadPathValid, 
                                    onMouseDown: this.buttonMouseDown}, __('Resume'))

                        )
                    )
                )
            )
        );
    }
});