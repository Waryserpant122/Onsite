tPosition(input, input.value.length);

        this.setPosition = _.bind(this.setPosition, this);
        this.setPosition();
        window.addEventListener('resize', this.setPosition);

    },
    componentWillUnmount: function(){

        window.removeEventListener('resize', this.setPosition);
    },

    setPosition: function(){
        var offset = $(ReactDOM.findDOMNode(this)).offset();
        _.defer(function(){
            FdmDispatcher.handleViewAction({
                actionType: 'setColorDialogPosition',
                content: {
                    data: {
                        offset: offset
                    }
                }
            })
        });
    },

    changeWidthOfInput: function(e){

        this.resizeInput(e.target);

    },

    resizeInput: function(input){

        var tmp = document.createElement("span");
        tmp.className = "tag_hidden_name";
        tmp.innerHTML = input.value.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
        document.body.appendChild(tmp);
        var theWidth = tmp.getBoundingClientRect().width;
        document.body.removeChild(tmp);
        input.style.width = theWidth + "px";

        this.props.changeInputSizeFn(theWidth + 24);
    },

    click: function(e){
        stopEventBubble(e)
    },

    onChange: function(e){

        this.props.onChangeFn(e);
        this.resizeInput(e.target);

    },

    render: function() {

        var color = this.props.color;

        return (
            React.createElement("input", {id: "edited-tag-input", 
                onKeyDown: this.props.onKeyDownFn, 
                spellCheck: false, 
                onClick: this.click, 
                value: this.props.name, onChange: this.onChange, onBlur: this.props.onBlurFn, 
                type: "text", className: 'new_tag tag_element' + (0.3 * color.r/255 + 0.59 * color.g/255 + 0.11 * color.b/255 > 0.8 ? ' invert' : '')})
        );

    }


});

var TagError = React.createClass({displayName: "TagError",


    getInitialState: function() {
        return {
            position: 'right'
        };
    },

    componentDidMount: function(){

        this.setPosition = _.bind(this.setPosition, this);
        this.setPosition();

        window.addEventListener('resize', this.setPosition);
    },
    componentWillUnmount: function(){

        window.removeEventListener('resize', this.setPosition);
    },

    setPosition: function(){

        var div = ReactDOM.findDOMNode(this);

        var left = $(div).offset().left;
        var width = div.getBoundingClientRect().width;
        var current_width = window.innerWidth;

        //if (this.props.newTag){
        //    this.setState({position: 'left'});
        //}
        //else
        if (this.state.position == 'right'){
            if (current_width < left + width)
                this.setState({position: 'left'});
        }
        else{
            if (left < 10){
                this.setState({position: 'right'});
            }
            else if (current_width - (left + 2*width + 160) > 0 ){
                this.setState({position: 'right'});
            }
        }
    },

    click: function(e){
        stopEventBubble(e)
    },

    render: function() {

        var left = !this.props.onPanel && !this.props.newTag;

        return (
            React.createElement("div", {id: "block-error-id", 
                 style: {marginLeft: this.props.newTag && this.state.position == 'right' ? '160px' : null}, 
                 className: rjs_class({
                 block_error: true,
                 on_panel: this.props.onPanel,
                 //new_tag: this.props.newTag,
                 right: this.state.position == 'right' && !left,
                 left: this.state.position == 'left' || left
                 })}, this.props.error)
        );

    }


});

var Tag = React.createClass({displayName: "Tag",

    getInitialState: function() {

        var state = {};
        state.tagInputSize = 0;

        return state;
    },

    dispatcherIndex: 0,
    componentDidMount: function(){

        _.bindAll(this, 'tagDragEnd', 'tagMove');

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'GlobalKeyDown' && event.keyCode === 27)
                    this.tagDragCancel();
                if (payload.action.actionType == 'GlobalOnWindowFocusLost')
                    this.tagDragCancel();
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));
    },

    componentWillUnmount: function(){

        FdmDispatcher.unregister(this.dispatcherIndex);
    },

    changeInputSize: function(new_size){

        if (this.state.tagInputSize != new_size){
            _.defer(function(){
                FdmDispatcher.handleViewAction({
                    actionType: 'onEditedTagChanged',
                    content: {
                        tagId: this.props.model.id
                    }
                });
            }.bind(this));
        }

        this.setState({tagInputSize: new_size});
    },

    currentDragElement: null,
    currentDragElementWidth: 0,
    tagDragStartTimeout: 0,
    tagDragStart: function(e){

        var model = this.props.model;

        if (model.id == 'new' || model.get('system'))
            return;

        stopEventBubble(e);

        var page_x = e.pageX;
        var page_y = e.pageY;

        if (e.button == 0){

            document.addEventListener('mouseup', this.tagDragEnd);

            this.tagDragStartTimeout = setTimeout(function(){

                this.tagDragStartTimeout = 0;

                FdmDispatcher.handleViewAction({
                    actionType: 'dragNDropTagStart',
                    content: {
                        tagId: this.props.model.id
                    }
                });
                document.addEventListener('mousemove', this.tagMove);

            }.bind(this), 100);
        }
    },

    cloneTag: function(e){

        document.body.classList.add('drag_n_drop_in_progress');

        var drag_el = ReactDOM.findDOMNode(this).cloneNode(true);

        drag_el.style.position = 'absolute';
        drag_el.style.zIndex = 10000;
        drag_el.style.left = (e.pageX + 10) + 'px';
        drag_el.style.top = (e.pageY - 10) + 'px';

        document.body.appendChild(drag_el);
        this.currentDragElement = drag_el;
        this.currentDragElementWidth = drag_el.getBoundingClientRect().width;
    },

    getBottomPanelDownloadOnTarget: function(target){

        if (target.id == 'tab-general-container'){

            var id = app.controllers.bottomPanel.model.get('currentItemId');
            return {id: id};
        }
        else if (target.parentNode){
            return this.getBottomPanelDownloadOnTarget(target.parentNode);
        }

        return false;
    },

    tagDragEnd: function(e){

        if (this.tagDragStartTimeout)
            clearTimeout(this.tagDragStartTimeout);

        document.removeEventListener('mousemove', this.tagMove);
        document.removeEventListener('mouseup', this.tagDragEnd);

        var current_row = app.controllers.downloads.getDownloadOnTarget(e.target);
        if (current_row){
            app.controllers.downloads.onDropTag(current_row.id, this.props.model.id, true);
        }

        if (!current_row){

            current_row = this.getBottomPanelDownloadOnTarget(e.target);
            if (current_row){
                app.controllers.downloads.onDropTag(current_row.id, this.props.model.id, false);
            }
        }

        if (this.currentDragElement){
            document.body.removeChild(this.currentDragElement);
            this.currentDragElement = false;
        }

        FdmDispatcher.handleViewAction({
            actionType: 'dragNDropTagEnd',
            content: {
                tagId: this.props.model.id
            }
        });

        app.controllers.downloads.setDropIsActiveDownload(false);
        document.body.classList.remove('drag_n_drop_in_progress');
    },
    tagDragCancel: function(){

        if (!this.currentDragElement)
            return;

        document.removeEventListener('mousemove', this.tagMove);
        document.removeEventListener('mouseup', this.tagDragEnd);

        if (this.currentDragElement){
            document.body.removeChild(this.currentDragElement);
            this.currentDragElement = false;
        }

        var tag_id = this.props.model.id;

        _.defer(function(){
            FdmDispatcher.handleViewAction({
                actionType: 'dragNDropTagEnd',
                content: {
                    tagId: tag_id
                }
            });
        });

        app.controllers.downloads.setDropIsActiveDownload(false);
        document.body.classList.remove('drag_n_drop_in_progress');
    },
    tagMove: function(e){

        stopEventBubble(e);

        if (!this.currentDragElement){
            this.cloneTag(e);
        }

        var new_pageX = e.pageX;
        var new_pageY = e.pageY;

        if (new_pageX < 10)
            new_pageX = 10;

        if (new_pageY < 20)
            new_pageY = 20;

        if (new_pageX > window.innerWidth - this.currentDragElementWidth - 20)
            new_pageX = window.innerWidth - this.currentDragElementWidth - 20;

        if (new_pageY > window.innerHeight - 50)
            new_pageY = window.innerHeight - 50;

        this.currentDragElement.style.left = (new_pageX + 10) + 'px';
        this.currentDragElement.style.top = (new_pageY - 10) + 'px';

        //if (e.pageX < 0 || e.pageY < 0
        //    || e.pageX > window.innerWidth || e.pageY > window.innerHeight ){
        //
        //    this.tagDragCancel();
        //    return;
        //}

        if (e.target.classList.contains('tag_element')
            || e.target.parentNode && e.target.parentNode.classList
                && e.target.parentNode.classList.contains('tag_element')
            || e.target.parentNode.parentNode && e.target.parentNode.parentNode.classList
                && e.target.parentNode.parentNode.classList.contains('tag_element'))
            return;

        var current_row = app.controllers.downloads.getDownloadOnTarget(e.target);

        var download_id = null;
        if (current_row)
            download_id = current_row.id;

        app.controllers.downloads.setDropIsActiveDownload(download_id);
    },

    render: function() {

        var model = this.props.model;
        var tag_form_opened = this.props.tagFormOpened;

        var tag;
        if (model.id == 'new')
            tag = model;
        else
            tag = model.toJSON();

        if (tag_form_opened){

            var color = {
                r: tag.colorR,
                g: tag.colorG,
                b: tag.colorB
            };

            return (
                React.createElement("div", {className: "wrap"}, 
                    React.createElement("div", {className: "wrap_new_tag"}, 
                        React.createElement("div", {className: "tag_element"}, 

                            React.createElement("span", {className: 'tag_color' + (tag.system ? ' system' : ''), style: {backgroundColor: 'rgb('+tag.colorR+','+tag.colorG+','+tag.colorB+')'}}), 
                    React.createElement("span", {className: 'tag_name' + (0.3 * tag.colorR/255 + 0.59 * tag.colorG/255 + 0.11 * tag.colorB/255 > 0.8 ? ' invert' : ''), title: tag.name}, 
                        React.createElement(TagNameInput, {color: color, name: this.props.editedTagName, onKeyDownFn: this.props.inputOnKeyDownFn, 
                                      onChangeFn: this.props.changeEditedTagNameFn, onBlurFn: this.props.editTagBlurFn, 
                                      changeInputSizeFn: _.bind(this.changeInputSize, this)}), 
                        React.createElement("span", null, " "), 
                        React.createElement("span", {className: "tag_count"}, '(' + this.props.countDownloads + ')')
                    ), 

                            this.props.editedTagError ?
                                React.createElement(TagError, {tagInputSize: this.state.tagInputSize, onPanel: this.props.onPanel, error: this.props.editedTagError})
                                : null

                        )
                    )
                )
            );
        }

        return (

            React.createElement("div", {onMouseDown: this.tagDragStart, 
                className: "wrap"}, 
                React.createElement("div", {className: rjs_class({
                                        tag_element:true,
                                        active:this.props.active
                                        //disable:this.props.disable
                                    }), onClick: _.partial(this.props.chooseTagFn, tag), 

                     onContextMenu: _.partial(this.props.tagContextMenuFn, tag)}, 
                    React.createElement("span", {className: 'tag_color' + (tag.system ? ' system' : ''), 
                          onMouseDown: _.partial(this.props.editTagFn, tag), 
                          style: {backgroundColor: 'rgb('+tag.colorR+','+tag.colorG+','+tag.colorB+')'}}), 
                    React.createElement("span", {className: 'tag_name' + (0.3 * tag.colorR/255 + 0.59 * tag.colorG/255 + 0.11 * tag.colorB/255 > 0.8 ? ' invert' : ''), title: tag.name}, 
                        React.createElement("span", null, tag.name), 
                        React.createElement("span", null, " "), 
                        React.createElement("span", {className: "tag_count"}, '(' + this.props.countDownloads + ')')
                    )
                )

            )

        );

    }


});



var TagsPanel = React.createClass({displayName: "TagsPanel",

    dispatcherIndex: false,
    dispatcherIndexEditTag: false,

    getInitialState: function() {

        var state = app.controllers.tags.model.toJSON();
        state.allTags = app.controllers.tags.collections.allTags;
        state.tagFormOpened = false;
        state.editedTagColor = {};
        state.editedTagId = null;
        state.editedTagName = '';
        state.newTagName = '';
        state.editedTagError = false;
        state.newTagError = false;
        state.statusFilter = app.controllers.downloads.model.get('statusFilter');

        state.visibleTags = [];
        state.hideTags = [];
        state.tagsScrollMaxHeight = window.innerHeight - 248;

        state.filterCounterAll = 0;
        state.filterCounterInProgress = 0;
        state.filterCounterCompleted = 0;
        state.filterCounterActive = 0;
        state.filterCounterInactive = 0;
        state.filterCounterError = 0;

        state.newTagColor = app.controllers.tags.getRandomTagColor();

        state.dragNDropTagInProgress = false;

        return state;
    },

    componentDidMount: function(){

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){
                if (payload.action.actionType == 'changeTagColor')
                    return this.changeTagColor(payload.action.content);
                if (payload.action.actionType == 'closeColoursDialogAction')
                    return this.closeTagColorAction(payload.action.content);
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
                if (payload.action.actionType == 'GlobalOnWindowFocusLost')
                    return this.globalOnWindowFocusLost(payload.action.content);
                if (payload.action.actionType == 'onDownloadTagsChanged')
                    return this.needRecalculatePopularTags(payload.action.content);
                if (payload.action.actionType == 'onTagChanged')
                    return this.needRecalculatePopularTags(payload.action.content);
                if (payload.action.actionType == 'onEditedTagChanged')
                    return this.needRecalculatePopularTags(payload.action.content);
                if (payload.action.actionType == 'tagsShowMoreCloseEvent')
                    return this.showMoreCloseEvent(payload.action.content);
                if (payload.action.actionType == 'needUpdateFilterCounters')
                    return this.needUpdateFilterCounters(payload.action.content);
                if (payload.action.actionType == 'dragNDropTagStart'){
                    this.setState({dragNDropTagInProgress: true});
                }
                if (payload.action.actionType == 'dragNDropTagEnd'){
                    this.setState({dragNDropTagInProgress: false});
                }
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

        this.dispatcherIndexEditTag = FdmDispatcher.register(function(payload) {

            if (payload.source == 'CORE_ACTION' && payload.action.actionType == 'editTag'){
                return this.editTagHandler(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

        app.controllers.tags.model.on('change', this.changeModel, this);
        app.controllers.tags.model.on('change:viewTag', this.changeViewTag, this);
        app.controllers.downloads.model.on('change:statusFilter', this.changeStatusFilter, this);

        app.controllers.tags.collections.allTags.on('add reset remove', this.changeTagsCollection, this);
        app.controllers.tags.collections.allTags.on('change', this.onChangeTags, this);
        app.controllers.downloads.collections.downloads.on('add reset remove', this.onChangeDownloads, this);

        _.bindAll(this, 'setVisibleTags', 'onResize');

        window.addEventListener('resize', this.onResize);

        this.recalculatePopularTags();
        this.needUpdateFilterCounters();
    },

    componentWillUnmount: function(){

        FdmDispatcher.unregister(this.dispatcherIndex);
        FdmDispatcher.unregister(this.dispatcherIndexEditTag);

        app.controllers.tags.model.off('change', this.changeModel, this);
        app.controllers.tags.model.off('change:viewTag', this.changeViewTag, this);
        app.controllers.downloads.model.off('change', this.changeStatusFilter, this);

        app.controllers.tags.collections.allTags.off('add reset remove', this.changeTagsCollection, this);
        app.controllers.tags.collections.allTags.off('change', this.onChangeTags, this);
        app.controllers.downloads.collections.downloads.off('add reset remove', this.onChangeDownloads, this);

        window.removeEventListener('resize', this.onResize);
    },

    onChangeTags: function(){
        this.setState({allTags: app.controllers.tags.collections.allTags});
    },

    changeTagsCollection: function(){
        this.setState({allTags: app.controllers.tags.collections.allTags});
        this.recalculatePopularTags();
    },

    onChangeDownloads: function(){

        this.needRecalculatePopularTags();
        this.needUpdateFilterCounters();
    },

    updateFilterCountersTimeout: false,
    needUpdateFilterCounters: function(){

        if (this.updateFilterCountersTimeout)
            return;

        this.updateFilterCountersTimeout = setTimeout(function(){

            this.updateFilterCountersTimeout = false;
            this.recalculateFilterCounters();

        }.bind(this), 100);

    },

    changeViewTag: function(){
        this.setVisibleTagsDefer();
    },

    onResize: function(){

        if (this.state.tagFormOpened)
            this.closeTagForm();

        this.setVisibleTags();

        this.setMaxHeight4tagsList();
    },

    setMaxHeight4tagsList: function(){

        this.setState({tagsScrollMaxHeight: window.innerHeight - 248});
    },

    popularTags: [],

    setVisibleTagsTimeout: 0,
    setVisibleTagsDefer: function(){

        if (this.setVisibleTagsTimeout > 0)
            return;

        this.setVisibleTagsTimeout = setTimeout(this.setVisibleTags.bind(this), 30);
    },

    setVisibleTags: function(){

        if (this.setVisibleTagsTimeout > 0){

            clearTimeout(this.setVisibleTagsTimeout);
            this.setVisibleTagsTimeout = 0;
        }

        var tags_width = ReactDOM.findDOMNode(this).firstElementChild.offsetWidth - 350 - 84 - 20 - 75;
        var tags = this.popularTags;
        var view_tag = this.state.viewTag;

        if (view_tag && view_tag.get('system'))
            view_tag = null;

        var tmp;

        var edited_tag_is_visible = false;
        if (this.state.tagFormOpened && this.state.editedTagId){

            if (_.findWhere(this.state.visibleTags, {id: this.state.editedTagId}))
                edited_tag_is_visible = true;
        }

        var view_tag_size = 0;
        if (view_tag){

            var view_tag_name = view_tag.get('name');
            if (this.state.tagFormOpened && this.state.editedTagId == view_tag.id)
                view_tag_name = this.state.editedTagName;
            var view_tag_popular = _.findWhere(tags, {id: view_tag.id});
            if (view_tag_popular)
                view_tag_name = view_tag_name + ' (' + view_tag_popular.n + ')';

            tmp = document.createElement("span");
            tmp.className = "tag_hidden_name";
            tmp.innerHTML = view_tag_name.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
            document.body.appendChild(tmp);
            view_tag_size = Math.max(45, tmp.getBoundingClientRect().width + 15 + 12 + 5);
            document.body.removeChild(tmp);
        }

        var visible_tags = [];
        var hide_tags = [];

        var visible = true;
        for (var i = 0; i < tags.length; i++){

            var tag = tags[i];

            if (tag.id < 10)
                continue;

            if (visible){

                var name = tag.name;
                if (this.state.tagFormOpened && this.state.editedTagId == tag.id)
                    name = this.state.editedTagName;
                name = name + ' (' + tag.n + ')';

                tmp = document.createElement("span");
                tmp.className = "tag_hidden_name";
                tmp.innerHTML = name.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
                document.body.appendChild(tmp);
                var tag_width = Math.max(45, tmp.getBoundingClientRect().width + 15 + 12 + 5);
                document.body.removeChild(tmp);

                if (view_tag && view_tag.id != tag.id && tags_width - view_tag_size - tag_width <= 0){

                    view_tag_popular = _.findWhere(tags, {id: view_tag.id});

                    if (view_tag_popular){
                        visible_tags.push(view_tag_popular);
                    }
                    else{
                        visible_tags.push({
                            id: view_tag.id,
                            name: view_tag.get('name')
                        });
                    }

                    visible = false;
                    if (view_tag && tag.id != view_tag.id){
                        hide_tags.push(tag);
                    }

                }
                else if (tags_width - tag_width > 0){
                    visible_tags.push(tag);

                    if (view_tag && tag.id == view_tag.id){
                        view_tag = false;
                    }
                }
                else{
                    if (view_tag && tag.id == view_tag.id){
                        continue;
                    }

                    hide_tags.push(tag);
                    visible = false;
                }

                tags_width -= tag_width;
            }
            else
            {
                if (view_tag && tag.id == view_tag.id)
                    continue;
                hide_tags.push(tag);
            }
        }

        if (this.state.tagFormOpened && this.state.editedTagId){
            if (edited_tag_is_visible && _.findWhere(hide_tags, {id: this.state.editedTagId})) {
                app.controllers.tags.setViewTag(this.state.editedTagId);
            }
        }

        this.setState({
            visibleTags: visible_tags,
            hideTags: hide_tags
        });
    },

    recalculatePopularTagsTimeout: false,
    needRecalculatePopularTags: function(){

        if (this.recalculatePopularTagsTimeout)
            return;

        this.recalculatePopularTagsTimeout = setTimeout(function(){

            this.recalculatePopularTagsTimeout = false;
            this.recalculatePopularTags();

        }.bind(this), 100);
    },

    recalculatePopularTags: function(){

        var tag_id;
        var downloads = app.controllers.downloads.collections.downloads.models;

        var popular_tags = {};

        var all_tags = this.state.allTags;

        if (!all_tags.length)
            return;

        for (var j = all_tags.length - 1; j >= 0; j--){
            tag_id = all_tags.models[j].id;
            popular_tags[tag_id] = {
                id: tag_id,
                name: all_tags.models[j].get('name'),
                n: 0
            };
        }

        for (var i = downloads.length - 1; i >= 0; i--){
            var tags = downloads[i].get('tags');

            var download_type = downloads[i].get('downloadType');

            if (download_type == fdm.models.DownloadType.Trt)
                popular_tags['1']['n']++;

            if (download_type == fdm.models.DownloadType.YouTubeVideo)
                popular_tags['2']['n']++;

            if (tags.length){
                for (var k = tags.length - 1; k >= 0; k--){
                    tag_id = tags[k].id;

                    if (tag_id < 100 || typeof popular_tags[tag_id] == 'undefined')
                        continue;

                    popular_tags[tag_id]['n']++;
                }
            }
        }

        popular_tags = _.values(popular_tags);
        popular_tags = _.sortBy(popular_tags, function(t){ return -t['n']; });

        this.popularTags = popular_tags;

        this.setVisibleTags();

    },

    changeStatusFilter: function(){
        this.setState({
            statusFilter: app.controllers.downloads.model.get('statusFilter')
        });
    },

    changeModel: function(){
        this.setState(app.controllers.tags.model.toJSON());
    },

    changeTagColor: function(action_content){

        if (!action_content.callbackData)
            return;

        var input;
        if (action_content.callbackData.tag_id == 'new'){

            this.setState({
                newTagColor: action_content.color
            });
            input = document.getElementById('edited-new-tag-input');
            if (input)
                input.focus();

        }
        else{
            app.controllers.tagsManageDialog.editTag(parseInt(action_content.callbackData.tag_id), action_content.callbackData.editedTagName, action_content.color);
            input = document.getElementById('edited-tag-input');
            if (input)
                input.focus();
        }

    },

    addNewTag: function(e){

        if (this.state.tagFormOpened){
            this.tagPanelClick(e);
            return;
        }

        stopEventBubble(e);

        var color = app.controllers.tags.getRandomTagColor();

        app.controllers.tagsManageDialog.model.set('tagFormOpened', true);

        this.setState({
            tagFormOpened: true,
            editedTagId: 'new',
            editedTagColor: color,
            editedTagName: '',
            newTagName: '',
            editedTagError: false,
            newTagError: false
        });

        this.openColorDialog('new', color);

    },

    editTag: function(tag, e){

        this.editTagHandler({
            tagId: tag.id
        });
        stopEventBubble(e);
    },

    tagContextMenu: function(tag, e){
        if (this.state.tagFormOpened && this.state.editedTagId != 'new'){
            this.tagPanelClick(e);
            return;
        }

        stopEventBubble(e);

        if (tag.system)
            return;

        fdmApp.menu.showTagPopupMenu(1, tag.id);// 1 - type tagPanel
    },

    editTagHandler: function(content){

        var tag_model = this.state.allTags.get(content.tagId);

        if (!tag_model || tag_model.get('system'))
            return;

        var tag = tag_model.toJSON();

        var color = {
            r: tag.colorR,
            g: tag.colorG,
            b: tag.colorB
        };

        app.controllers.tagsManageDialog.model.set('tagFormOpened', true);

        this.setState({
            tagFormOpened: true,
            editedTagId: tag.id,
            editedTagName: tag.name,
            editedTagColor: color,
            editedTagError: false,
            newTagError: false
        });

        this.openColorDialog(tag.id, color);
    },

    openColorDialog: function(tag_id, color){

        _.defer(function(){ FdmDispatcher.handleViewAction({
            actionType: 'showColoursDialog',
            content: {
                callbackData: {
                    color: color,
                    tag_id: tag_id
                },
                data: {
                    currentColor: color
                }
            }
        })});
    },

    changeEditedTagName: function(e){

        var name = e.target.value;

        if (name.length > 20)
        {
            this.setState({
                editedTagName: name.substring(0, 20)
            });
            stopEventBubble(e);
            return;
        }

        this.setState({
            editedTagName: name
        });

        if (this.state.editedTagError){
            this.setState({
                editedTagError: false
            });
        }
    },

    changeNewTagName: function(e){

        var name = e.target.value;

        if (name.length > 20)
        {
            this.setState({
                newTagName: name.substring(0, 20)
            });
            stopEventBubble(e);
            return;
        }

        this.setState({
            newTagName: name
        });

        if (this.state.newTagError){
            this.setState({
                newTagError: false
            });
        }
    },

    tagPanelClick: function(e){

        stopEventBubble(e);

        if (!this.state.tagFormOpened && this.props.model.get('selectedTag') ){
            app.controllers.tags.selectTagById(null);
            return;
        }
        this.submitTagForm();
    },

    chooseTag: function(tag, e){

        app.controllers.downloads.setStatusFilter(null);
        stopEventBubble(e);
        app.controllers.tags.model.set({showMoreOpened: false});
        if (this.state.tagFormOpened){
            this.tagPanelClick(e);
            return;
        }

        var selectedTag = this.props.model.get('selectedTag');
        if (selectedTag && selectedTag.id == tag.id){
            app.controllers.tags.selectTagById(null);
        }
        else{
            app.controllers.tags.chooseTag(tag);
        }
    },

    closeTagColorAction: function(action_content){

        if (action_content.callbackData && action_content.callbackData.tag_id == 'new'){
            app.controllers.tagsManageDialog.model.set('tagFormOpened', false);
            _.defer(function(){
                FdmDispatcher.handleViewAction({
                    actionType: 'closeColoursDialog',
                    content: {}
                });
            });
            var input = document.getElementById('edited-new-tag-input');
            if (input)
                input.focus();
        }
        else{
            this.submitTagForm();
        }
    },

    inputOnKeyDown: function(e){

        if ( e.keyCode === 27 ) { // ESC

            this.closeTagForm();
            this.setVisibleTagsDefer();
        }
        if ( e.keyCode === 13 ) { // Enter
            this.submitTagForm();
        }
    },

    newInputOnKeyDown: function(e){

        if ( e.keyCode === 27 ) { // ESC

            this.closeTagForm();
        }
        if ( e.keyCode === 13 ) { // Enter
            this.submitTagForm(true);
        }
    },

    submitTagForm: function(new_tag, not_hide_form){

        new_tag = new_tag || false;
        not_hide_form = not_hide_form || false;

        var controller = app.controllers.tagsManageDialog;

        var tag_name = new_tag ? this.state.newTagName : this.state.editedTagName;

        var result;
        if (tag_name.trim() != ''){
            if (new_tag){
                result = controller.onAddNewTag(tag_name, this.state.newTagColor);
            }
            else{
                result = controller.submitNewName(this.state.editedTagId, tag_name);
            }
        }

        if (!result){
            if (!not_hide_form)
                this.closeTagForm();
        }
        else if (result.error){

            var input;
            if (new_tag){
                input = document.getElementById('edited-new-tag-input');
                if (input)
                    input.focus();
                this.setState({
                    newTagError: result.errorMessage
                });
            }
            else{
                input = document.getElementById('edited-tag-input');
                if (input)
                    input.focus();
                this.setState({
                    editedTagError: result.errorMessage
                });
            }
            return false;
        }
        else{
            if (new_tag){

                _.delay(function(){
                        var scroll = document.getElementById('tags-scroll-container');
                        if (scroll){
                            scroll.scrollTop = scroll.scrollHeight;
                        }
                    }, 300);

                var color = app.controllers.tags.getRandomTagColor(this.state.newTagColor);

                this.setState({
                    newTagError: false,
                    newTagName: '',
                    newTagColor: color
                });
                _.defer(function(){
                    FdmDispatcher.handleViewAction({
                        actionType: 'closeColoursDialog',
                        content: {}
                    });
                });
            }
            else
                this.closeTagForm();
            return true;
        }
    },

    closeTagForm: function(){
        this.setState({
            tagFormOpened: false,
            editedTagColor: {},
            editedTagId: null,
            editedTagName: '',
            editedTagError: false,
            newTagError: false,
            newTagName: ''
        });
        app.controllers.tagsManageDialog.model.set('tagFormOpened', false);
        _.defer(function(){
            FdmDispatcher.handleViewAction({
                actionType: 'closeColoursDialog',
                content: {}
            });
        });
    },

    showMoreClick: function(e){

        stopEventBubble(e);

        if (this.state.newTagName != ''){

            var success = this.submitTagForm(true, true);

            if (success){
                this.setState({
                    newTagError: false,
                    newTagName: ''
                });
            }
        }

    },

    showMoreCloseEvent: function(){
        if (this.state.showMoreOpened){

            if (this.state.newTagName != ''){

                var success = this.submitTagForm(true, true);

                if (success){
                    this.setState({
                        newTagError: false,
                        newTagName: ''
                    });
                }
            }
            else{
                app.controllers.tags.model.set({showMoreOpened: !this.state.showMoreOpened});
            }
        }
    },

    showMoreToggle: function(e){

        stopEventBubble(e);
        if (this.state.showMoreOpened){

            if (this.state.newTagName != ''){

                var success = this.submitTagForm(true, true);

                if (success){
                    this.setState({
                        newTagError: false,
                        newTagName: ''
                    });
                }
            }
            else{
                app.controllers.tags.model.set({showMoreOpened: !this.state.showMoreOpened});
            }
        }
        else{
            if (this.state.hideTags.length == 0){
                _.defer( function(){
                    var el = document.getElementById('edited-new-tag-input');
                    if (el)
                        el.focus();
                });
            }
            app.controllers.tags.model.set({showMoreOpened: !this.state.showMoreOpened});
        }
    },

    editTagBlur: function(){

    },

    globalOnWindowFocusLost: function(event){

        if (event.keyCode === 27 && this.state.statusFilterOpened){
            return;
        }

        if (this.state.tagFormOpened || !this.state.showMoreOpened)
            return;

        _.defer(function(){
            FdmDispatcher.handleViewAction({
                actionType: 'closeColoursDialog',
                content: {}
            });
        });
        app.controllers.tags.model.set({showMoreOpened: false});
    },

    globalKeyDown: function(){

        if (event.keyCode === 27 && this.state.statusFilterOpened){
            this.statusFilterClose();
            return;
        }

        if (this.state.tagFormOpened || !this.state.showMoreOpened)
            return;

        if (event.keyCode === 27){
            _.defer(function(){
                FdmDispatcher.handleViewAction({
                    actionType: 'closeColoursDialog',
                    content: {}
                });
            });
            app.controllers.tags.model.set({showMoreOpened: false});
        }
    },

    newTagDataFormat: function(){

        return {
            id: 'new',
            colorR: this.state.editedTagColor.r,
            colorG: this.state.editedTagColor.g,
            colorB: this.state.editedTagColor.b
        };
    },

    recalculateFilterCounters: function(){

        var filters = {
            filterCounterAll: 0,
            filterCounterInProgress: 0,
            filterCounterCompleted: 0,
            filterCounterActive: 0,
            filterCounterInactive: 0,
            filterCounterError: 0
        };

        var downloads = app.controllers.downloads.collections.downloads.models;

        filters.filterCounterAll = downloads.length;

        for (var i = downloads.length -1; i >= 0; i--) {
            var d = downloads[i];

            var state = d.get('state');

            if (state == fdm.models.DownloadState.Completed)
                filters.filterCounterCompleted++;
            else
                filters.filterCounterInProgress++;

            if (state == fdm.models.DownloadState.Downloading /* && d.get('downloadSpeedBytes') > 0 */
                || d.get('uploadSpeedBytes') > 0)
                filters.filterCounterActive++;
            else
                filters.filterCounterInactive++;

            if (state == fdm.models.DownloadState.Error)
                filters.filterCounterError++;
        }

        if (this.state.filterCounterAll != filters.filterCounterAll
            || this.state.filterCounterCompleted != filters.filterCounterCompleted
            || this.state.filterCounterActive != filters.filterCounterActive)
            this.setState(filters);

    },

    setStatusFilter: function(new_value, e){
        app.controllers.tags.selectTagById(null);
        app.controllers.downloads.setStatusFilter(new_value);
    },

    showNewColorDialog: function(){

        var tag = {
            id: 'new'
        };

        var color = this.state.newTagColor;

        //app.controllers.tagsManageDialog.model.set('tagFormOpened', true);

        this.setState({
            //tagFormOpened: true,
            editedTagId: tag.id,
            editedTagName: tag.name,
            editedTagColor: color,
            editedTagError: false
        });

        this.openColorDialog(tag.id, color);

        var input = document.getElementById('edited-new-tag-input');
        if (input)
            input.focus();
        var offset = $('#new_color_dialog_link').offset();
        _.defer(function(){
            FdmDispatcher.handleViewAction({
                actionType: 'setColorDialogPosition',
                content: {
                    data: {
                        offset: offset
                    }
                }
            })
        });
    },

    render: function() {

        var model = this.props.model;
        var all_tags = this.state.allTags;

        var selectedTag = model.get('selectedTag');
        var statusFilter = this.state.statusFilter;

        var trt_tag = {};
        var trt_tag_model = this.state.allTags.get(1);
        if (trt_tag_model){

            trt_tag = trt_tag_model.toJSON();
            var ft = _.findWhere(this.popularTags, {id:1});
            if (ft)
                trt_tag.n = ft.n;
        }

        var youtube_tag = {};
        var youtube_tag_model = this.state.allTags.get(2);
        if (youtube_tag_model){

            youtube_tag = youtube_tag_model.toJSON();
            var fy = _.findWhere(this.popularTags, {id:2});
            if (fy)
                youtube_tag.n = fy.n;
        }

        return (
            React.createElement("div", {
                onClick: this.tagPanelClick, 
                //style={{display: view_model.visible ? 'block' : 'none' }}
                className: rjs_class({
                'header-tags':true
            })}, 

                React.createElement("div", {className: "wrapper-user-tags"}, 

                    React.createElement("div", {onClick: app.controllers.downloads.resetTagsAndFilters, 
                        className: rjs_class({
                    tag_element: true,
                    last_in_block: true,
                    all: true,
                    active: selectedTag === null && statusFilter === null
                    //disable: selectedTag !== null
                    })}, 
                        React.createElement("span", {className: "tag_name", title: __("All")}, 
                            React.createElement("span", null, __('All')), 
                            React.createElement("span", null, " "), 
                            React.createElement("span", {className: "tag_count"}, '(' + this.state.filterCounterAll + ')')
                        )
                    ), 

                    React.createElement("div", {onClick: _.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Active), 
                        className: rjs_class({
                    tag_element: true,
                    all: true,
                    active: statusFilter === fdm.models.DownloadStateFilters.Active
                    //disable: selectedTag !== null
                    })}, 
                        React.createElement("span", {className: "tag_name", title: __("Active")}, 
                            React.createElement("span", null, __('Active')), 
                            React.createElement("span", null, " "), 
                            React.createElement("span", {className: "tag_count"}, '(' + this.state.filterCounterActive + ')')
                        )
                    ), 

                    React.createElement("div", {onClick: _.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Completed), 
                        className: rjs_class({
                    tag_element: true,
                    last_in_block: true,
                    all: true,
                    completed: true,
                    active: statusFilter === fdm.models.DownloadStateFilters.Completed
                    //disable: selectedTag !== null
                    })}, 
                        React.createElement("span", {className: "tag_name", title: __('Completed', false, 'tags panel')}, 
                            React.createElement("span", null, __('Completed', false, 'tags panel')), 
                            React.createElement("span", null, " "), 
                            React.createElement("span", {className: "tag_count"}, '(' + this.state.filterCounterCompleted + ')')
                        )
                    ), 

                    trt_tag && trt_tag.name ?
                    React.createElement("div", {onMouseDown: stopEventBubble, className: "wrap"}, 
                        React.createElement("div", {className: rjs_class({
                                        tag_element:true,
                                        active: selectedTag == trt_tag_model
                                        //disable:this.props.disable
                                    }), onClick: _.partial(this.chooseTag, trt_tag), 

                             onContextMenu: _.partial(this.tagContextMenu, trt_tag)}, 
                    React.createElement("span", {className: 'tag_name' + (0.3 * trt_tag.colorR/255 + 0.59 * trt_tag.colorG/255 + 0.11 * trt_tag.colorB/255 > 0.8 ? ' invert' : ''), 
                          title: __(trt_tag.name)}, 
                        React.createElement("span", null, __(trt_tag.name)), 
                        React.createElement("span", null, " "), 
                        React.createElement("span", {className: "tag_count"}, '(' + trt_tag.n + ')')
                    )
                        )

                    )
                        : null, 

                     youtube_tag && youtube_tag.name ?
                    React.createElement("div", {onMouseDown: stopEventBubble, className: "wrap"}, 
                        React.createElement("div", {className: rjs_class({
                                        tag_element:true,
                                        last_in_block: true,
                                        active: selectedTag == youtube_tag_model
                                        //disable:this.props.disable
                                    }), onClick: _.partial(this.chooseTag, youtube_tag), 

                             onContextMenu: _.partial(this.tagContextMenu, youtube_tag)}, 
                            /*<span className={'tag_color' + (youtube_tag.system ? ' system' : '' )}
                          onMouseDown={stopEventBubble}
                          style={{backgroundColor: 'rgb('+youtube_tag.colorR+','+youtube_tag.colorG+','+youtube_tag.colorB+')'}}></span>*/
                    React.createElement("span", {className: 'tag_name' + (0.3 * youtube_tag.colorR/255 + 0.59 * youtube_tag.colorG/255 + 0.11 * youtube_tag.colorB/255 > 0.8 ? ' invert' : ''), 
                          title: __(youtube_tag.name)}, 
                        React.createElement("span", null, __(youtube_tag.name)), 
                        React.createElement("span", null, " "), 
                        React.createElement("span", {className: "tag_count"}, '(' + youtube_tag.n + ')')
                    )
                        )

                    )
                        : null, 


                     this.state.visibleTags.map(function(tag){

                        var tag_model = all_tags.get(tag.id);

                        if (!tag_model || tag_model.get('system'))
                            return null;

                        return (

                            React.createElement(Tag, {key: tag_model.id, model: tag_model, onPanel: true, 

                                 inputOnKeyDownFn: _.bind(this.inputOnKeyDown, this), 
                                 changeEditedTagNameFn: _.bind(this.changeEditedTagName, this), 
                                 editTagBlurFn: _.bind(this.editTagBlur, this), 
                                 chooseTagFn: _.bind(this.chooseTag, this), 
                                 editTagFn: _.bind(this.editTag, this), 
                                 tagContextMenuFn: _.bind(this.tagContextMenu, this), 
                                 countDownloads: tag.n ? tag.n : 0, 

                                 editedTagName: this.state.editedTagName, 
                                 tagFormOpened: this.state.tagFormOpened && this.state.editedTagId == tag_model.id, 
                                 editedTagError: this.state.editedTagError, 
                                 active: selectedTag == tag_model, 
                                 disable: selectedTag != tag_model && selectedTag !== null})
                        );

                    }.bind(this)), 



                    React.createElement("div", {className: rjs_class({
                        tag_element: true,
                        show_more: this.state.hideTags.length || this.state.showMoreOpened,
                        manage_btn: !this.state.hideTags.length && !this.state.showMoreOpened
                    }), onClick: this.showMoreToggle}, 

                         this.state.showMoreOpened ?

                            React.createElement("div", null, 
                                React.createElement("div", {className: "transparent_tags", 
                                     style: {display: !this.state.dragNDropTagInProgress ? 'block' : 'none'}, 
                                     onClick: this.showMoreToggle, onContextMenu: this.showMoreToggle}), 
                                React.createElement("div", {className: "triangle"}), 
                                React.createElement("div", {onClick: this.showMoreClick, 
                                    className: rjs_class({
                                    wrapper_tags: true,
                                    empty_list: !this.state.hideTags.length
                                })}, 
                                    React.createElement("div", {id: "tags-scroll-container", className: "wrap_manage", 
                                        style: {maxHeight: this.state.tagsScrollMaxHeight + 'px'}}, 

                                        this.state.hideTags.map(function(tag){

                                            var tag_model = all_tags.get(tag.id);

                                            if (!tag_model)
                                                return null;

                                            return (

                                                React.createElement(Tag, {key: tag_model.id, model: tag_model, onPanel: false, 

                                                     inputOnKeyDownFn: _.bind(this.inputOnKeyDown, this), 
                                                     changeEditedTagNameFn: _.bind(this.changeEditedTagName, this), 
                                                     editTagBlurFn: _.bind(this.editTagBlur, this), 
                                                     chooseTagFn: _.bind(this.chooseTag, this), 
                                                     editTagFn: _.bind(this.editTag, this), 
                                                     tagContextMenuFn: _.bind(this.tagContextMenu, this), 
                                                     countDownloads: tag.n ? tag.n : 0, 

                                                     editedTagName: this.state.editedTagName, 
                                                     tagFormOpened: this.state.tagFormOpened && this.state.editedTagId == tag_model.id, 
                                                     editedTagError: this.state.editedTagError, 
                                                     active: selectedTag == tag_model, 
                                                     disable: selectedTag != tag_model && selectedTag !== null})
                                            );

                                        }.bind(this))

                                    ), 
                                    React.createElement("div", {onClick: stopEventBubble, className: rjs_class({
                                        wrap_new_tag: true,
                                        empty_list: !this.state.hideTags.length
                                    })}, 
                                        React.createElement("input", {id: "edited-new-tag-input", onKeyDown: this.newInputOnKeyDown, 
                                               value: this.state.newTagName, defaultValue: this.state.newTagName, 
                                               onChange: this.changeNewTagName, 
                                               className: "name_fortag", type: "text", placeholder: __('Add tag'), autofocus: true}), 

                                        this.state.newTagError ?
                                            React.createElement(TagError, {newTag: true, onPanel: false, error: this.state.newTagError})
                                            : null, 

                                        React.createElement("a", {href: "#", onClick: this.showNewColorDialog, id: "new_color_dialog_link", 
                                           className: "choose_color", 
                                           style: {backgroundColor:
                                               'rgb(' + this.state.newTagColor.r + ',' + this.state.newTagColor.g + ',' + this.state.newTagColor.b + ')'}})
                                    )
                                )
                            )

                            : null

                    ), 


                    React.createElement(TagsColourDialog, null)


                )

                /*
                <div onClick={this.statusFilterToggle} className={rjs_class({
                    'js-status-filter': true,
                    wrapper_inselect: true,
                    top: true,
                    selected: this.state.statusFilter != null
                })}>
                    <div>

                        { this.state.statusFilter == null ?
                            <span>All</span>
                            : null }
                        { this.state.statusFilter == fdm.models.DownloadStateFilters.InProgress ?
                            <span className="filter_name">Downloading</span>
                            : null }
                        { this.state.statusFilter == fdm.models.DownloadStateFilters.Completed ?
                            <span className="filter_name">Completed</span>
                            : null }
                        { this.state.statusFilter == fdm.models.DownloadStateFilters.Active ?
                            <span className="filter_name">Active</span>
                            : null }
                        { this.state.statusFilter == fdm.models.DownloadStateFilters.Inactive ?
                            <span className="filter_name">Inactive</span>
                            : null }
                        { this.state.statusFilter == fdm.models.DownloadStateFilters.Error ?
                            <span className="filter_name">Errors</span>
                            : null }

                        { this.state.statusFilterOpened ?
                            <div className="list">
                                <div className={rjs_class({active: this.state.statusFilter == null})}
                                     onClick={_.partial(this.setStatusFilter, null)}>
                                    <span>All</span>
                                    <span className="count">{this.state.filterCounterAll}</span>
                                </div>
                                <div className={rjs_class({active: this.state.statusFilter == fdm.models.DownloadStateFilters.InProgress})}
                                     onClick={_.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.InProgress)}>
                                    <span>Downloading</span>
                                    <span className="count">{this.state.filterCounterInProgress}</span>
                                </div>
                                <div className={rjs_class({active: this.state.statusFilter == fdm.models.DownloadStateFilters.Completed})}
                                     onClick={_.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Completed)}>
                                    <span>Completed</span>
                                    <span className="count">{this.state.filterCounterCompleted}</span>
                                </div>
                                <div className={rjs_class({active: this.state.statusFilter == fdm.models.DownloadStateFilters.Active})}
                                     onClick={_.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Active)}>
                                    <span>Active</span>
                                    <span className="count">{this.state.filterCounterActive}</span>
                                </div>
                                <div className={rjs_class({active: this.state.statusFilter == fdm.models.DownloadStateFilters.Inactive})}
                                     onClick={_.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Inactive)}>
                                    <span>Inactive</span>
                                    <span className="count">{this.state.filterCounterInactive}</span>
                                </div>

                                {this.state.filterCounterError > 0 || this.state.statusFilter == fdm.models.DownloadStateFilters.Error ?

                                    <div className={rjs_class({active: this.state.statusFilter == fdm.models.DownloadStateFilters.Error})}
                                         onClick={_.partial(this.setStatusFilter, fdm.models.DownloadStateFilters.Error)}>
                                        <span>Error</span>
                                        <span className="count">{this.state.filterCounterError}</span>
                                    </div>

                                    : null }

                            </div>
                            : null }

                    </div>
                    { this.state.statusFilterOpened ?
                    <div onClick={this.statusFilterClose} onContextMenu={this.statusFilterClose}
                        className="transparent_sort" style={{display: 'block'}}></div>
                        : null }
                </div>
                */
            )
        );
    }
});



var TagsColourDialog = React.createClass({displayName: "TagsColourDialog",

    dispatcherIndex: false,
    dispatcherIndexKeyDown: false,
    callbackData: false,

    componentDidMount: function(){

        this.dispatcherIndex = FdmDispatcher.register(function(payload) {

            if (payload.source == 'VIEW_ACTION'){

                if (payload.action.actionType == 'showColoursDialog')
                    this.showColoursDialog(payload.action.content);
                if (payload.action.actionType == 'setColorDialogPosition')
                    return this.setColorDialogPosition(payload.action.content);
                if (payload.action.actionType == 'closeColoursDialog')
                    this.close();
                if (payload.action.actionType == 'GlobalKeyDown')
                    return this.globalKeyDown(payload.action.content);
                if (payload.action.actionType == 'transparentColorsClick')
                    return this.transparentColorsClick(payload.action.content);
                if (payload.action.actionType == 'closeActionByTagPanel')
                    return this.closeActionByTagPanel(payload.action.content);
                if (payload.action.actionType == 'newDownload')
                    return this.closeColpickDialog(payload.action.content);
            }

            return true; // No errors. Needed by promise in Dispatcher.
        }.bind(this));

        $('#addColor').colpick({
            colorScheme:'light',
            layout:'rgbhex',
            onSubmit: this.submitColpickColor,
            onHide: this.hideColpickColor
        });

    },

    componentWillUnmount: function(){

        FdmDispatcher.unregister(this.dispatcherIndex);
        FdmDispatcher.unregister(this.dispatcherIndexKeyDown);

    },

    submitColpickColor: function(hsb,hex,rgb,el){

        this.changeColor(rgb);
        $(el).colpickHide();
        this.setState({
            customizeDialogVisible: false
        });
    },

    hideColpickColor: function(){

        this.setState({
            customizeDialogVisible: false
        });
    },

    getInitialState: function() {
        return {
            visible: false,
            customizeDialogVisible: false,
            offset: {},
            currentColor: {}
        };
    },

    showColoursDialog: function(action_content){

        this.callbackData = action_content.callbackData;

        this.show(action_content.data);
    },

    setColorDialogPosition: function(action_content){

        var input_offset = action_content.data.offset;
        var top = input_offset.top - 60 + 26;
        var left = input_offset.left;
        var window_width = window.innerWidth - 20;
        var window_height = window.innerHeight - 60 - 30;
        var edit_tag_rect = document.getElementById('editTagV2').getBoundingClientRect();
        var width = edit_tag_rect.width;
        var height = edit_tag_rect.height;

        if (left + width >= window_width){
            left -= left + width - window_width;
        }

        if (top + height >= window_height){
            top -= 30 + height;
        }

        this.setState({
            offset: {
                top: top,
                left: left
            }
        });

        this.updateCollorpickerPosition();
    },

    updateCollorpickerPosition: function(){
        if (this.state.customizeDialogVisible){

            var window_width = window.innerWidth - 20;
            var width = document.getElementById('collorpicker').getBoundingClientRect().width;
            var customize_offset = $('#collorpicker').offset();
            var left = customize_offset.left;

            if (left + width >= window_width){
                left -= left + width - window_width;
                document.getElementById('collorpicker').style.left = left + 'px';
            }
        }
    },

    show: function(data){

        this.setState({
            visible: true,
            currentColor: data.currentColor,
            customizeDialogVisible: false
        });
    },

    close: function(e){

        this.setState({
            visible: false,
            customizeDialogVisible: false
        });
        $('#addColor').colpickHide();
    },

    closeActionByTagPanel: function(){
        this.closeAction();
    },

    closeColpickDialog: function () {

        if (this.state.customizeDialogVisible){
            this.setState({customizeDialogVisible: false});
            $('#addColor').colpickHide();
        }
    },

    closeAction: function(e){

        stopEventBubble(e);

        if (this.state.customizeDialogVisible){
            this.setState({customizeDialogVisible: false});
            $('#addColor').colpickHide();
        }
        else{
            _.defer(function() {
                FdmDispatcher.handleViewAction({
                    actionType: 'closeColoursDialogAction',
                    content: {
                        callbackData: this.callbackData
                    }
                });

            }.bind(this));
        }
    },

    onKeyDown: function(e){

        if (event.keyCode === 27)
            this.closeAction(e);
    },

    globalKeyDown:function(event){

        if (!this.state.visible || !this.state.customizeDialogVisible)
            return;

        if (event.keyCode === 27){
            this.closeAction();
        }

    },

    transparentColorsClick:function(content){

        if (!this.state.visible || !this.state.customizeDialogVisible)
            return;

        this.closeAction();
        var input = document.getElementById('edited-tag-input');
        if (input)
            input.focus();
    },

    changeColor: function(color){

        this.setState({
            currentColor: color
        });

        FdmDispatcher.handleViewAction({
            actionType: 'changeTagColor',
            content: {
                color: color,
                callbackData: this.callbackData
            }
        });

    },

    setFocus: function(){

        if (this.callbackData && this.callbackData.tag_id == 'new'){

        }
        else{

            var input = document.getElementById('edited-tag-input');
            if (input)
                input.focus();
        }
    },

    customizeColorClick: function(e) {

        $('#addColor').colpickSetColor(this.state.currentColor);
        $('#addColor').colpickShow(e);

        this.setState({
            customizeDialogVisible: true
        });

        _.defer(_.bind(this.updateCollorpickerPosition, this));

        stopEventBubble(e);
    },

    deleteTag: function(){
        this.closeAction();

        fdmApp.tags.deleteTag(parseInt(this.callbackData.tag_id));
    },

    render: function() {

        //if (!this.state.visible)
        //    return null;

        var current_color = this.state.currentColor;

        return (

            React.createElement("div", {id: "new-tag-colours", style: {display: this.state.visible ? 'block' : 'none'}}, 
                React.createElement("div", {onMouseDown: this.closeAction, onContextMenu: this.closeAction, onKeyDown: this.onKeyDown, className: "trnsprnt_for_tags"}), 

                this.state.customizeDialogVisible ?
                    React.createElement("div", {onClick: this.closeAction, onContextMenu: this.closeAction, onKeyDown: this.onKeyDown, style: {display: 'block'}, className: "transparent_colors"})
                    : null, 

                React.createElement("div", {id: "editTagV2", className: "block-edit-tags", onClick: this.setFocus, 
                     style: {left: this.state.offset.left + 'px', top: this.state.offset.top + 'px'}}, 
                    React.createElement("span", {className: "selected_color", 
                          style: {backgroundColor: 'rgb('+current_color.r+','+current_color.g+','+current_color.b+')'}}), 
                    React.createElement("div", {className: "main-palette"}, 
                        fdm.models.defaultColors.map(function(color, index){
                            return (
                                React.createElement("span", {key: index, onClick: _.partial(this.changeColor, color), 
                                      style: {backgroundColor: 'rgb('+color.r+','+color.g+','+color.b+')'}})
                            );
                        }.bind(this))
                    ), 
                    React.createElement("a", {href: "#", id: "addColor", onClick: this.customizeColorClick}, __('Customize color')), 

                     this.callbackData.tag_id != 'new' ?
                        React.createElement("hr", null)
                        : null, 
                     this.callbackData.tag_id != 'new' ?
                        React.createElement("a", {href: "#", onClick: _.partial(this.deleteTag)}, __('Remove tag'))
                        : null
                )
            )

        );
    }
});
