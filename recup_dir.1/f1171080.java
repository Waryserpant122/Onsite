

var SimpleDialogs = React.createClass({displayName: "SimpleDialogs",

    mixins: [ButtonMixin, ToolbarDragMixin],

    toolbarDragId: 'download-popup-overlay2',

    dispatcherIndexKeyDown: false,

    getInitialState: function () {

        return app.controllers.simpleDialogs.model.toJSON();
    },

    componentDidMount: function() {

        app.controllers.simpleDialogs.model.on('change', this._onChange, this);

        this.dispatcherIndexKeyDown = FdmDispatcher.register(function(payload) {

            if (!this.state.opened)
                return true;

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function() {

        app.controllers.simpleDialogs.model.off('change', this._onChange, this);

        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);
    },

    _onChange: function() {

        this.setState(app.controllers.simpleDialogs.model.toJSON());
    },

    globalKeyDown: function(content){

        if (content.keyCode === 27){
            this.close();
        }

    },

    close: function(){

        app.controllers.simpleDialogs.close();

    },

    submit: function(e){

        app.controllers.simpleDialogs.submit();
    },

    inputChange: function(e){

        stopEventBubble(e);

        app.controllers.simpleDialogs.model.set({
            inputText: e.target.value
        });
    },

    render: function() {

        if (!this.state.opened)
            return null;

        return (

            React.createElement("div", {id: "download-popup-overlay2", 
                 onMouseDown: this.toolbarDragStart, onDoubleClick: this.toolbarDoubleClick, 
                 className: "popup__overlay simple"}, 
                React.createElement("div", null, 
                    React.createElement("div", {className: "mount"}), 
                    React.createElement("div", {className: "simple_dialog"}, 

                        React.createElement("div", {className: "header"}, 
                            React.createElement("div", null, 
                                __(this.state.title)
                            ), 
                            React.createElement("a", {href: "#", className: "close_button", onClick: this.close})
                        ), 

                        React.createElement("div", {className: "center"}, 
                            __(this.state.message)
                        ), 

                        React.createElement("div", {className: "bottom_add_ul bottom"}, 
                            React.createElement("div", {className: "group_button"}, 

                                this.state.buttons.indexOf(fdm.models.SimpleDialogButtons.Cancel) >= 0 ?

                                    React.createElement("button", {className: "left_button cancel linkblock", onClick: this.close, 
                                            onMouseDown: this.buttonMouseDown}, __('Cancel')
                                    + (this.state.timeoutButton == fdm.models.SimpleDialogButtons.Cancel && this.state.timeout > 0 ? ' (' + this.state.timeout + ')' : ''))

                                    : null, 

                                this.state.buttons.indexOf(fdm.models.SimpleDialogButtons.OK) >= 0 ?

                                React.createElement("button", {className: "right_button linkblock", onClick: this.submit, 
                                        onMouseDown: this.buttonMouseDown}, __('OK')
                                    + (this.state.timeoutButton == fdm.models.SimpleDialogButtons.OK && this.state.timeout > 0 ? ' (' + this.state.timeout + ')' : ''))

                                    : null
                            )
                        )
                    )
                )
            )

        );
    }
});