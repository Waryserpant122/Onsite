

var CustomSpeedDialog = React.createClass({displayName: "CustomSpeedDialog",

    mixins: [ButtonMixin, ToolbarDragMixin],

    toolbarDragId: 'js-popup-choose-speed',

    dispatcherIndex: 0,

    getInitialState: function () {

        var s = app.controllers.customSpeedDialog.model.toJSON();
        s.divSelectOpened = false;

        return s;
    },

    componentDidMount: function() {

        app.controllers.customSpeedDialog.model.on('change', this._onChange, this);

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function() {

        app.controllers.customSpeedDialog.model.off('change', this._onChange, this);

        FdmDispatcher.unregister(this.dispatcherIndex);
    },

    _onChange: function(){

        var state = app.controllers.customSpeedDialog.model.toJSON();
        //state.bottomPanelVisible = app.controllers.bottomPanel.model.get('panelVisible');

        this.setState(state);
    },

    globalKeyDown: function(){

        if (!this.state.dialogIsShown)
            return;

        if (event.keyCode === 27){
            this.cancel();
        }
    },

    change: function(name, value, e){

        app.controllers.customSpeedDialog.model.set(name, value);
    },

    cancel: function(){
        app.controllers.customSpeedDialog.cancel();
    },

    apply: function(){
        app.controllers.customSpeedDialog.apply();
    },

    divSelectToggle: function(select_name, e){

        if (this.state.divSelectOpened && this.state.divSelectOpened == select_name){

            this.divSelectClose();
        }
        else{

            stopEventBubble(e);
            this.setState({
                divSelectOpened: select_name
            });
        }
    },

    divSelectClose: function(e){

        stopEventBubble(e);
        this.setState({
            divSelectOpened: false
        });

    },

    render: function() {

        if (!this.state.dialogIsShown)
            return null;

        if (!app || !app.controllers || !app.controllers.customSpeedDialog)
            return null;

        var speed_values = app.controllers.customSpeedDialog.collections.speedValues;
        var download_speed_text_tpl = app.controllers.customSpeedDialog.collections.speedValues.findWhere({value: this.state.downloadSpeedAbsolute}).get('text_tpl');
        var download_speed_text = __(download_speed_text_tpl.tpl, download_speed_text_tpl.value);
        var upload_speed_text_tpl = app.controllers.customSpeedDialog.collections.speedValues.findWhere({value: this.state.uploadSpeedAbsolute}).get('text_tpl');
        var upload_speed_text = __(upload_speed_text_tpl.tpl, upload_speed_text_tpl.value);
        return (
            React.createElement("div", {tabIndex: "1", className: "popup-choose-speed", id: "js-popup-choose-speed", 
                 onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick, 
                 "data-bind": "event: { keydown: handleKeydown }"}, 
                React.createElement("div", {className: "mount"}), 
                React.createElement("div", {className: "popup choose-speed"}, 

                    React.createElement("div", {className: "header"}, 
                        React.createElement("div", null, __('Traffic limits')), 
                        React.createElement("div", {className: "close_button", onClick: this.cancel})
                    ), 

                    React.createElement("div", {className: "center-choose-speed center"}, 
                        React.createElement("div", {className: "row-wrap clearfix"}, 

				React.createElement("span", {className: "title-input"}, __('Download speed:')), 

                            React.createElement("div", {className: "choose"}, 

                                 this.state.divSelectOpened && this.state.divSelectOpened == 'download' ?
                                    React.createElement("div", {className: "transparent_select", onMouseDown: this.divSelectClose})
                                    : null, 
                                React.createElement("div", {className: "wrapper_inselect", onClick: _.partial(this.divSelectToggle, 'download')}, 
                                    React.createElement("span", null, download_speed_text), 
                                    React.createElement("div", {className: "dropdown_button", onClick: _.partial(this.divSelectToggle, 'download')}), 

                                     this.state.divSelectOpened && this.state.divSelectOpened == 'download' ?

                                        React.createElement("div", {className: "list"}, 

                                            speed_values.map(function(speed_type, index){

                                                var text_tpl = speed_type.get('text_tpl');

                                                if (speed_type.get('disable')) {
                                                    return (
                                                        React.createElement("div", {className: "disable", key: index, onClick: stopEventBubble}, React.createElement("span", null, 
                                                            __(text_tpl.tpl, text_tpl.value)
                                                        ))
                                                    );
                                                }
                                                else {
                                                    return (
                                                        React.createElement("div", {key: index, onClick: _.partial(this.change, 'downloadSpeedAbsolute', speed_type.get('value'))}, React.createElement("span", null, 
                                                            __(text_tpl.tpl, text_tpl.value)
                                                        ))
                                                    );
                                                }

                                            }.bind(this))

                                        )

                                        : null

                                )
                            )

                        ), 
                        React.createElement("div", {className: "row-wrap clearfix"}, 

				React.createElement("span", {className: "title-input"}, __('Upload speed:')), 


                                React.createElement("div", {className: "choose"}, 

                                     this.state.divSelectOpened && this.state.divSelectOpened == 'upload' ?
                                        React.createElement("div", {className: "transparent_select", onMouseDown: this.divSelectClose})
                                        : null, 

                                    React.createElement("div", {className: "wrapper_inselect", onClick: _.partial(this.divSelectToggle, 'upload')}, 
                                        React.createElement("span", null, upload_speed_text), 

                                        React.createElement("div", {className: "dropdown_button", onClick: _.partial(this.divSelectToggle, 'upload')}), 

                                         this.state.divSelectOpened && this.state.divSelectOpened == 'upload' ?

                                            React.createElement("div", {className: "list"}, 

                                                speed_values.map(function(speed_type, index){

                                                    var text_tpl = speed_type.get('text_tpl');

                                                    if (speed_type.get('disable')) {
                                                        return (
                                                            React.createElement("div", {className: "disable", key: index, onClick: stopEventBubble}, React.createElement("span", null, __(text_tpl.tpl, text_tpl.value)))
                                                        );
                                                    }
                                                    else {
                                                        return (
                                                            React.createElement("div", {key: index, onClick: _.partial(this.change, 'uploadSpeedAbsolute', speed_type.get('value'))}, 
                                                                React.createElement("span", null, __(text_tpl.tpl, text_tpl.value)))
                                                        );
                                                    }

                                                }.bind(this))

                                            )

                                            : null

                                    )
                                )

                        )
                    ), 
                    React.createElement("div", {className: "bottom-choose-speed bottom"}, 
                        React.createElement("div", {className: "group_button"}, 
                            React.createElement("button", {className: "left_button cancel linkblock", title: "", 
                                    onClick: this.cancel, 
                                    onMouseDown: this.buttonMouseDown}, __('Cancel')), 
                            React.createElement("button", {className: "right_button linkblock", title: "", 
                                    onClick: this.apply, 
                                    onMouseDown: this.buttonMouseDown}, __('Apply'))
                        )
                    )
                )
            )
        );
    }
});