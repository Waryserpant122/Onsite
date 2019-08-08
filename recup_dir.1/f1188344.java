
var EmptyDownloads = React.createClass({displayName: "EmptyDownloads",

    dispatcherIndex: 0,
    getInitialState: function() {

        var state = {};
        state.downloadListBuild = app.controllers.downloads.model.get('downloadListBuild');
        state.currentDownloadsLength = app.controllers.downloads.collections.currentDownloads.length;
        state.allDownloadsLength = app.controllers.downloads.collections.downloads.length;
        state.filterMessage = {};

        return state;
    },

    componentDidMount: function(){

        app.controllers.downloads.model.on('change:downloadListBuild', this.changeDownloadListBuild, this);
        app.controllers.downloads.collections.currentDownloads.on('add reset remove', this.changeCurrentDownloads, this);
        app.controllers.downloads.collections.downloads.on('add reset remove', this.changeAllDownloads, this);

        app.controllers.downloads.model.on('change:activeFilterText change:statusFilter', this.refreshDownloadsFilteredText, this);
        app.controllers.tags.model.on('change:selectedTag', this.refreshDownloadsFilteredText, this);

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){

                if (payload.action.actionType == 'onTagChanged')
                    this.onTagChanged.apply(this);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

    },

    componentWillUnmount: function() {

        app.controllers.downloads.model.off('change:downloadListBuild', this.changeDownloadListBuild, this);
        app.controllers.downloads.collections.currentDownloads.off('add reset remove', this.changeCurrentDownloads, this);
        app.controllers.downloads.collections.downloads.off('add reset remove', this.changeAllDownloads, this);

        app.controllers.downloads.model.off('change:activeFilterText change:statusFilter', this.refreshDownloadsFilteredText, this);
        app.controllers.tags.model.off('change:selectedTag', this.refreshDownloadsFilteredText, this);

        FdmDispatcher.unregister(this.dispatcherIndex);
    },

    onTagChanged: function(){

        this.refreshDownloadsFilteredText();
    },

    changeDownloadListBuild: function(){
        this.setState({downloadListBuild: app.controllers.downloads.model.get('downloadListBuild')});
    },

    changeCurrentDownloads: function(){
        this.setState({currentDownloadsLength: app.controllers.downloads.collections.currentDownloads.length});
    },

    changeAllDownloads: function(){
        this.setState({allDownloadsLength: app.controllers.downloads.collections.downloads.length});
    },

    refreshDownloadsFilteredText: function(){

        var filter_message;

        var activeFilterText = app.controllers.downloads.model.get('activeFilterText');
        var selectedTag = app.controllers.tags.model.get('selectedTag');
        var statusFilter = app.controllers.downloads.model.get('statusFilter');

        if (
            (activeFilterText != '' ? 1 : 0 ) +
            (selectedTag ? 1 : 0) +
            (statusFilter !== null ? 1 : 0) > 1
        ){
            filter_message = {
                type: 'many_filters',
                text: activeFilterText != '' ? '"' + activeFilterText + '"' : false
            };

            this.setState({filterMessage: filter_message});
        }
        else if (selectedTag){

            filter_message = {
                type: 'tag',
                text: '"' + selectedTag.get('name') + '"'
            };

            this.setState({filterMessage: filter_message});
        }
        else if (activeFilterText != ''){

            filter_message = {
                type: 'search',
                text: '"' + activeFilterText + '"'
            };

            this.setState({filterMessage: filter_message});
        }
        else if (statusFilter !== null){

            if (statusFilter == fdm.models.DownloadStateFilters.Active ){

                filter_message = {
                    type: 'statusFilter',
                    text: __('No active downloads')
                };
                this.setState({filterMessage: filter_message});
            }
            if (statusFilter == fdm.models.DownloadStateFilters.Completed){

                filter_message = {
                    type: 'statusFilter',
                    text: __('No completed downloads')
                };
                this.setState({filterMessage: filter_message});
            }
        }
        else{
            filter_message = {
                type: 'unknown'
            };

            this.setState({filterMessage: filter_message});
        }
    },

    mouseDownEmptyDiv: function(){

        FdmDispatcher.handleViewAction({
            actionType: 'tagsShowMoreCloseEvent',
            content: {}
        });

    },

    render: function () {

        if (!this.state.downloadListBuild){
            return (
                React.createElement("div", {className: "first_view"}, 
                    React.createElement("img", {src: "preloading_FDM.GIF", alt: ""})
                )
            );
        }

        if (this.state.currentDownloadsLength > 0)
            return null;

        if (this.state.allDownloadsLength == 0){
            return (
                React.createElement("div", {onMouseDown: this.mouseDownEmptyDiv, className: "temporary-style wrapper_strt_dwlnd"}, 
                    React.createElement("div", {className: "start_add_download"}, 
                        React.createElement("div", {className: "wrapper"}, 
                            React.createElement("div", {className: "big_title"}, __('Drag & Drop')), 
                            React.createElement("div", null, __('URL or torrent here'))
                        )
                    )
                )
            );
        }
        else{

            var filter_message = this.state.filterMessage;

            return (
                React.createElement("div", {className: "filter-no-results"}, 

                    function(){

                        if (filter_message
                            && (filter_message.type == 'search'
                            || filter_message.type == 'many_filters' && filter_message.text)){

                            return (
                                React.createElement("span", {className: "notification"}, 
                                __('No results found for') + ' ', 
                                    React.createElement("span", {className: "user_text"}, filter_message.text), 
                                React.createElement("a", {href: "#", onClick: app.controllers.downloads.resetTagsAndFilters}, __('Show all'))
                            )
                            );
                        }
                        if (filter_message && filter_message.type == 'tag'){
                            return (
                                React.createElement("span", {className: "notification"}, 
                                    __('No results tagged') + ' ', 
                                    React.createElement("span", {className: "user_text"}, filter_message.text), 
                                    ' ' + __('found'), 
                                React.createElement("a", {href: "#", onClick: app.controllers.downloads.resetTagsAndFilters}, __('Show all'))
                            )
                            );
                        }
                        if (filter_message && filter_message.type == 'statusFilter'){
                            return (
                                React.createElement("span", {className: "notification"}, 
                                filter_message.text, 
                                    React.createElement("a", {href: "#", onClick: app.controllers.downloads.resetTagsAndFilters}, __('Show all'))
                            )
                            );
                        }

                        return (
                            React.createElement("span", {className: "notification"}, 
                                __('Nothing found'), 
                                React.createElement("a", {href: "#", onClick: app.controllers.downloads.resetTagsAndFilters}, __('Show all'))
                            )
                        );

                    }()
                )
            );
        }

    }
});

var DownloadEmptyDiv = React.createClass({displayName: "DownloadEmptyDiv",

    render: function () {

        var height = this.props.height;

        return (
            React.createElement("div", {style: {height: height + 'px'}, className: "my-compact completed"}
            )
        );


    }
});


var Download = React.createClass({displayName: "Download",

    getInitialState: function() {

        var state = this.props.download.toJSON();
        state.showFilenameTitle = false;
        state.showErrorTitle = false;

        return state;
    },

    componentDidMount: function(){

        this.nameTitleFix = _.bind(this.nameTitleFix, this);

        this.props.download.on('change:state change:seedingEnabled', this.nameTitleFix);
        this.props.download.on('change', this.downloadChange, this);
        window.addEventListener('resize', this.nameTitleFix);

        this.nameTitleFix();
    },

    componentWillUnmount: function() {

        if (this.titleFixTimeout)
            clearTimeout(this.titleFixTimeout);

        this.props.download.off("change:state change:seedingEnabled", this.nameTitleFix);
        this.props.download.off('change', this.downloadChange, this);
        window.removeEventListener('resize', this.nameTitleFix);
    },

    downloadChange: function(){

        _.defer(function(){
            if (this.isMounted())
                this.setState(this.props.download.toJSON());
        }.bind(this));
    },

    titleFixTimeout: null,

    nameTitleFix: function() {

        var row = ReactDOM.findDOMNode(this);

        if (this.titleFixTimeout)
            clearTimeout(this.titleFixTimeout);
        this.titleFixTimeout = setTimeout(function(){

            var showFilenameTitle = false;
            var showErrorTitle = false;

            var c = row.getElementsByClassName('compact-download-title');
            var n = row.getElementsByClassName('download-title');
            if (c && c.length && n && n.length)
            {
                c = c[0];
                n = n[0];

                if (n && n.firstChild)
                    n = n.firstChild;

                showFilenameTitle = c.getBoundingClientRect().width < n.getBoundingClientRect().width + 12;
            }

            var error_text = this.props.download.getErrorText();

            if (error_text != ''){

                var e = row.getElementsByClassName('error_wrap');
                var s = row.getElementsByClassName('error-message');

                if (e && e.length && s && s.length)
                {
                    e = e[0];
                    s = s[0];

                    showErrorTitle = e.getBoundingClientRect().width < s.getBoundingClientRect().width;
                }
            }

            this.setState({
                showFilenameTitle: showFilenameTitle,
                showErrorTitle: showErrorTitle
            });

        }.bind(this), 1000);
    },

    