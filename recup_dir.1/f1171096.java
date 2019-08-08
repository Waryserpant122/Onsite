

var TableHeaderInfo = React.createClass({displayName: "TableHeaderInfo",

    getInitialState: function() {

        return {
            allSelected: app.controllers.downloads.model.get('allSelected'),
            sortOptions: app.controllers.downloads.model.get('sortOptions'),
            statusFilter: app.controllers.downloads.model.get('statusFilter')
        };
    },

    componentDidMount: function(){

        app.controllers.downloads.model.on('change:allSelected change:sortOptions change:statusFilter', this.changeState, this);
    },

    componentWillUnmount:function(){

        app.controllers.downloads.model.off('change:allSelected change:sortOptions change:statusFilter', this.changeState, this);
    },

    changeState: function(){
        this.setState({
            allSelected: app.controllers.downloads.model.get('allSelected'),
            sortOptions: app.controllers.downloads.model.get('sortOptions'),
            statusFilter: app.controllers.downloads.model.get('statusFilter')
        });
    },

    lastToggleSelectAll: 0,

    toggleSelectAll: function(e){

        var current_date = +new Date();
        if (current_date - this.lastToggleSelectAll < 250)
            return;
        this.lastToggleSelectAll = current_date;
        app.controllers.downloads.toggleSelectAll();
    },

    sort: function(sort_column){

        app.controllers.downloads.sort(sort_column);
    },

    render: function() {

        var sortOptions = this.state.sortOptions;
        var allSelected = this.state.allSelected;

        return (
            React.createElement("div", {id: "compact-view-info", className: "compact-view-info"}, 
                React.createElement("ul", null, 
                    React.createElement("li", {className: "compact-view-checkbox-wrap", "data-prop": "state", "data-direction": "asc"}, 
                        React.createElement("input", {className: rjs_class({
                            indeterminate: allSelected === undefined
                        }), 
                            type: "checkbox", id: "compact-header-checkbox", 
                            checked: allSelected === true}), 
                        React.createElement("label", {htmlFor: "compact-header-checkbox", title: __('Select All'), onClick: this.toggleSelectAll}), 
                        React.createElement("b", {className: "resizer"})
                    ), 
                    React.createElement("li", {onClick: _.partial(this.sort, 'fileName'), 
                        className: rjs_class({
                    'compact-view-name': true,
                    'sort-up': sortOptions.sortProp == 'fileName' && sortOptions.descending,
                    'sort-down': sortOptions.sortProp == 'fileName' && !sortOptions.descending
                    })}, 
                        React.createElement("span", {className: "rubber-ellipsis"}, __('Name')), 
                        React.createElement("b", {className: "resizer"})
                    ), 
                    React.createElement("li", {onClick: _.partial(this.sort, 'progress'), 
                        className: rjs_class({
                    'compact-view-progress': true,
                    'sort-up': sortOptions.sortProp == 'progress' && sortOptions.descending,
                    'sort-down': sortOptions.sortProp == 'progress' && !sortOptions.descending
                    })}, 
                        React.createElement("span", {className: "rubber-ellipsis"}, __('Status')), 
                        React.createElement("b", {className: "resizer"})
                    ), 
                    React.createElement("li", {onClick: _.partial(this.sort, 'downloadSpeedBytes'), 
                        className: rjs_class({
                    'compact-view-speed': true,
                    'sort-up': sortOptions.sortProp == 'downloadSpeedBytes' && sortOptions.descending,
                    'sort-down': sortOptions.sortProp == 'downloadSpeedBytes' && !sortOptions.descending
                    })}, 
                        React.createElement("span", {className: "rubber-ellipsis"}, __('Speed')), 
                        React.createElement("b", {className: "resizer"})
                    ), 
                    React.createElement("li", {onClick: _.partial(this.sort, 'totalBytes'), 
                        className: rjs_class({
                    'compact-view-size': true,
                    'sort-up': sortOptions.sortProp == 'totalBytes' && sortOptions.descending,
                    'sort-down': sortOptions.sortProp == 'totalBytes' && !sortOptions.descending
                    })}, 
                        React.createElement("span", {className: "rubber-ellipsis"}, __('Size')), 
                        React.createElement("b", {className: "resizer"})
                    ), 
                    React.createElement("li", {onClick: _.partial(this.sort, 'createdDate'), 
                        style: {overflow: 'hidden'}, 
                        className: rjs_class({
                    'compact-view-date': true,
                    'sort-up': sortOptions.sortProp == 'createdDate' && sortOptions.descending,
                    'sort-down': sortOptions.sortProp == 'createdDate' && !sortOptions.descending
                    })}, 
                        React.createElement("span", {className: "rubber-ellipsis"},  this.state.statusFilter == fdm.models.DownloadStateFilters.Completed ? __('Completed') : __('Added'))
                    )
                )
            )
        );
    }
});