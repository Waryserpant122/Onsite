

var TestLocalMessage = React.createClass({displayName: "TestLocalMessage",

    render: function() {

        T.setTexts({
            greeting: "###Hello, World!\n ###My name is *{myName}*! \n _howAreYou_",
            howAreYou:  "_How do you do?_"
        });

        return React.createElement(T.span, {
            text: { key: "greeting",
            myName: React.createElement("a", {onClick: alert, href: ""}, "i18n-react")
            }})
    }

});


var TagsPanelMessages = React.createClass({displayName: "TagsPanelMessages",

    getInitialState: function() {

        return {
            defaultTrtClientDialogOpened: app.controllers.settings.model.get('defaultTrtClientDialogOpened'),
            shutDownWhenDone: app.controllers.settings.model.get('shutDownWhenDone'),
            preventSleepAction: app.controllers.settings.model.get('preventSleepAction'),
            preventSleepIfScheduledDownloads: app.controllers.settings.model.get('preventSleepIfScheduledDownloads'),
            showScheduleTip: app.controllers.downloads.model.get('showScheduleTip'),
            showLinkCatchingMsg: app.controllers.settings.model.get('showLinkCatchingMsg')
        }
    },

    componentDidMount: function() {

        app.controllers.settings.model.on("change:defaultTrtClientDialogOpened change:shutDownWhenDone change:showLinkCatchingMsg", this._onChange, this);
        app.controllers.settings.model.on("change:preventSleepAction change:preventSleepIfScheduledDownloads", this._onChange, this);
        app.controllers.downloads.model.on("change:showScheduleTip", this._onChange, this);

        this.afterChanges();
    },

    componentWillUnmount: function() {

        app.controllers.settings.model.off("change:defaultTrtClientDialogOpened change:shutDownWhenDone change:showLinkCatchingMsg", this._onChange, this);
        app.controllers.settings.model.off("change:preventSleepAction change:preventSleepIfScheduledDownloads", this._onChange, this);
        app.controllers.downloads.model.off("change:showScheduleTip", this._onChange, this);

        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);
    },

    _onChange: function() {


        var state = {
            defaultTrtClientDialogOpened: app.controllers.settings.model.get('defaultTrtClientDialogOpened'),
            shutDownWhenDone: app.controllers.settings.model.get('shutDownWhenDone'),
            preventSleepAction: app.controllers.settings.model.get('preventSleepAction'),
            preventSleepIfScheduledDownloads: app.controllers.settings.model.get('preventSleepIfScheduledDownloads'),
            showScheduleTip: app.controllers.downloads.model.get('showScheduleTip'),
            showLinkCatchingMsg: app.controllers.settings.model.get('showLinkCatchingMsg')
        };

        this.afterChanges(state);
        this.setState(state);
    },

    afterChanges: function(state){

        state = state || false;

        if (state){

            if (this.state.defaultTrtClientDialogOpened == state.defaultTrtClientDialogOpened
                && this.state.shutDownWhenDone == state.shutDownWhenDone
                && this.state.preventSleepIfScheduledDownloads == state.preventSleepIfScheduledDownloads
                && this.state.showScheduleTip == state.showScheduleTip
                && this.state.showLinkCatchingMsg == state.showLinkCatchingMsg
            )
                return;

            if (state.defaultTrtClientDialogOpened || state.shutDownWhenDone
                || state.showScheduleTip && state.preventSleepIfScheduledDownloads
                || state.showLinkCatchingMsg)
                $("body").addClass("shut-down-when-done-message");
            else
                $("body").removeClass("shut-down-when-done-message");
        }
        else{

            if (this.state.defaultTrtClientDialogOpened || this.state.shutDownWhenDone
                || this.state.showScheduleTip && this.state.preventSleepIfScheduledDownloads
                || this.state.showLinkCatchingMsg)
                $("body").addClass("shut-down-when-done-message");
            else
                $("body").removeClass("shut-down-when-done-message");
        }
    },

    notShowMessageAgain: function(){

        app.controllers.settings.saveSetting('check-default-torrent-client-at-startup', false);
        this.hideDefaultTrtClientDialog();
    },

    setAsDefault: function(){

        fdmApp.settings.setDefaultTorrentClient(true);
        this.hideDefaultTrtClientDialog();
    },

    hideDefaultTrtClientDialog: function(){

        app.controllers.settings.model.set({
            defaultTrtClientDialogOpened: false
        });
    },

    shutDownWhenDoneDisable: function(){

        app.controllers.settings.saveSetting('shutdown-when-done', false);
        app.controllers.settings.model.set({
            shutDownWhenDone: false
        });
    },

    hideSchedulerTip: function(){

        app.appViewManager.setDownloadsStateOne('scheduleTipAlreadyShown', true);
        app.controllers.downloads.model.set({
            showScheduleTip: false
        });
    },

    closeAutomaticLinkCatchingMsg: function(){

        app.controllers.settings.model.set({
            showLinkCatchingMsg: false
        });
    },

    notShowAutomaticLinkCatchingMsg: function(){

        app.appViewManager.setDownloadsWizardState('linkCatchingMsgShown', true);
        this.closeAutomaticLinkCatchingMsg();
    },

    showSettings: function(){

        app.controllers.settings.showTab('monitoring_tab');
        this.closeAutomaticLinkCatchingMsg();
    },

    render: function() {

        if (this.state.showScheduleTip && this.state.preventSleepIfScheduledDownloads){

            return (

                React.createElement("div", {className: "wrap_default_client"}, 
                    React.createElement("span", null, __('Computer won\'t be put to sleep because there are scheduled downloads')), 

                    React.createElement("div", {onClick: this.hideSchedulerTip, className: "cancel_btn"})
                )
            );
        }

        if (this.state.shutDownWhenDone){

            return (

                React.createElement("div", {className: "wrap_default_client about_sleep"}, 

                    this.state.preventSleepAction == fdm.models.preventSleepAction.Sleep ?

                        React.createElement("span", null, __('Computer will be put to sleep after all downloads are completed.'))
                        : null, 

                    this.state.preventSleepAction == fdm.models.preventSleepAction.Shutdown ?

                        React.createElement("span", null, __('Computer will shut down after all downloads are completed.'))
                        : null, 

                    this.state.preventSleepAction == fdm.models.preventSleepAction.Hibernate ?

                        React.createElement("span", null, __('Computer will hibernate after all downloads are completed.'))
                        : null, 

                    React.createElement("a", {onClick: this.shutDownWhenDoneDisable, href: "#"}, __('Cancel'))
                )

            );
        }

        if (this.state.showLinkCatchingMsg){

            return (

                React.createElement("div", {className: "wrap_default_client"}, 
                    React.createElement("span", null, React.createElement("b", null, __('Tip'), " 