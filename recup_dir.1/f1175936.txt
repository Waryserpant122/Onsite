fdm.models.FileTree = Backbone.TreeModel.extend({
    constructor: function tree() {
        Backbone.TreeModel.prototype.constructor.apply(this, arguments);

        for (var i =0; i < this._children.length; i++)
            this.toggleOpen(this._children.models[i], true);
    },
    defaults: {
        data: {},
        type: undefined,
        checked: false,
        notAllowedSize: false,
        selected: false,
        selectedList: new Backbone.Collection,
        openedFolders: new Backbone.Collection
    },
    render:function(priority){

        this.root().trigger('change');
    },
    refreshTreeNodeState: function(node)
    {
        var checked = node.attributes.checked;
        var selected = node.attributes.selected;
        var not_allowed_size = node.attributes.notAllowedSize;

        var new_state = {checked: checked, selected: selected, notAllowedSize: not_allowed_size};

        if (!node._children.length)
            return new_state;

        var children = node._children.models;

        var all_checked = true;
        var all_not_checked = true;
        var all_selected = true;
        not_allowed_size = false;

        for (var i in children){
            var child = children[i];

            var child_state = this.refreshTreeNodeState(child);

            if (!child_state['checked'])
                all_checked = false;
            if (child_state['checked'] !== false)
                all_not_checked = false;
            if (!child_state['selected'])
                all_selected = false;
            if (child_state['notAllowedSize'] && child_state['checked'])
                not_allowed_size = true;

            if (child_state['checked'] !== new_state['checked'])
                new_state['checked'] = undefined;
            if (child_state['selected'] !== new_state['selected'])
                new_state['selected'] = undefined;
        }
        if (all_checked)
            new_state['checked'] = true;
        if (all_not_checked)
            new_state['checked'] = false;
        if (all_selected)
            new_state['selected'] = true;
        new_state['notAllowedSize'] = not_allowed_size;

        node.set(new_state);

        return new_state;
    },
    toggleChecked:function(file, e){
        var current = file.attributes.checked;
        var new_checked = !current;

        if (file._children.length){
            var all_children = this.getAll(file._children.models);
            for (var i in all_children){
                all_children[i].set('checked', new_checked);
            }

        }
        file.set('checked', new_checked);

        this.refreshTreeNodeState(this);
        this.render('high');
    },
    setCheck:function(node, check){

        node.set('checked', check);

        if (!node._children.length)
            return;

        var children = node._children.models;

        for (var i in children){
            var child = children[i];

            this.setCheck(child, check);
        }
        this.render('high');
    },

    refreshTreeNodeSelection: function(node)
    {
        var selected = this.isSelected(node);
        var new_selected = selected;

        var children = node._children.models;

        if (!children.length)
            return selected;

        var all_selected = true;

        for (var i in children){
            var child = children[i];

            var child_selected = this.refreshTreeNodeSelection(child);

            if (!child_selected)
                all_selected = false;

            if (selected !== child_selected)
                new_selected = null;
        }

        if (all_selected)
            new_selected = true;

        if (node.attributes.index > 0 || node.attributes.index === 0){

            if (selected && !new_selected)
                this.attributes.selectedList.remove({id: node.attributes.index}, {silent: true});
            if (!selected && new_selected)
                this.attributes.selectedList.push({id: node.attributes.index}, {silent: true});
        }

        return selected;
    },
    resetSelection:function(node){

        this.attributes.selectedList.reset([]);

        if (node)
            this.setSelection(node, true, true);

        this.refreshTreeNodeSelection(this);
        this.render();
    },
    clearSelection:function(){

        this.attributes.selectedList.reset([]);
    },
    resetSelectionList:function(list, select){

        this.clearSelection();
        this.setSelectionList(list, select);
    },
    setSelectionList:function(list, select){

        for (var i =0; i < list.length; i++){

            this.setSelection(list[i], select, true);
        }

        this.refreshTreeNodeSelection(this);
        this.render();
    },
    setSelection:function(node, select, silent){

        silent = silent || false;

        if(select)
            this.attributes.selectedList.push({id: node.attributes.index}, {silent: true});
        else
            this.attributes.selectedList.remove({id: node.attributes.index}, {silent: true});

        var children = node._children.models;

        if (children.length){

            for (var i in children){
                var child = children[i];

                this.setSelection(child, select, true);
            }
        }

        if (!silent){

            this.refreshTreeNodeSelection(this);
            this.render();
        }
    },

    getCurrentState: function(){

        var result = {
            checked_not_allowed_size: false,
            has_not_allowed_size: false,
            count_checked: 0,
            checked_size: 0
        };

        if (!this._children.length)
            return result;

        var all = this.getAll(this._children.models);
        for (var i in all){

            var child = all[i];

            if (this.isLeaf(child)){
                var hot_allowed = child.attributes.notAllowedSize;
                if (hot_allowed)
                    result['has_not_allowed_size'] = true;
                if (child.attributes.checked){
                    if (hot_allowed)
                        result['checked_not_allowed_size'] = true;

                    result['count_checked']++;
                    result['checked_size'] += child.attributes.data.size;
                }
            }
        }

        return result;
    },
    setMaxAllowSize: function(max_size){
        var all = [];
        if (this._children.length)
            all = this.getAll(this._children.models);

        var has_changes = false;
        for (var i in all){
            var child = all[i];
            var current_not_allow = child.attributes.notAllowedSize;
            if (this.isLeaf(child) && child.attributes.data.size && child.attributes.data.size > max_size ){
                if (!current_not_allow){
                    child.set('notAllowedSize', true);
                    has_changes = true;
                }
            }
            else{
                if (current_not_allow){
                    child.set('notAllowedSize', false);
                    has_changes = true;
                }
            }
        }
        if (has_changes){
            this.refreshTreeNodeState(this);
            this.render('high');
        }
    },
    buildCheckedFileIndexes: function(){
        var all = [];
        if (this._children.length)
            all = this.getAll(this._children.models);

        var result = [];
        for (var i in all){
            if (all[i].attributes.checked)
                result.push(all[i].attributes.data.index);
        }
        return result;

    },
    buildCheckedUrlsData: function(){
        var all = [];
        if (this._children.length)
            all = this.getAll(this._children.models);

        var result = [];
        for (var i in all){
            result.push({
                id: all[i].attributes.id,
                checked: all[i].attributes.checked,
                data: all[i].attributes.data
            });
        }
        return result;
    },
    getAll: function(models){
        var result = [];
        for (var i in models){
            var model = models[i];
            result.push(model);
            if (model._children.length){
                var r = this.getAll(model._children.models);
                result = result.concat(r);
            }
        }
        return result;
    },
    //checkAll:function(){
    //
    //    var all = [];
    //    if (this._children.length)
    //        all = this.getAll(this._children.models);
    //
    //    for (var i in all){
    //        all[i].set('checked', true);
    //    }
    //},
    isOpen:function(file){
        return this.attributes.openedFolders.get(file.attributes.index) != null;
    },
    toggleOpen:function(file, forceOpened, e){
        var setOpen = forceOpened === null || forceOpened === undefined ?
            !this.isOpen(file) : forceOpened;

        if (setOpen)
            this.attributes.openedFolders.push({id: file.attributes.index});
        else
            this.attributes.openedFolders.remove({id: file.attributes.index});
        this.render('high');
        stopEventBubble(e);
    },
    isSelected:function(file){

        return this.attributes.selectedList.get(file.attributes.index) != null;
    },
    toggleSelection:function(file, forceSelect, e){

        var setSelected = forceSelect === undefined || forceSelect === null ? !this.isSelected(file) : forceSelect;

        this.attributes.selectedList.reset([]);

        if(setSelected){
            this.attributes.selectedList.push({id: file.attributes.index});
        }
        this.render('high');
        stopEventBubble(e);
    },
    isLeaf:function(file){
        return file._children.length == 0;
    },
    isLast:function(){
        return false;
    },

    setNodePriorityChecked: function(node){

        var data = node.get('data');

        if (data && data.priority){
            node.set({checked: data.priority != fdm.models.filePriority.Skip});
        }

        if (node._children.length){

            for (var i = (node._children.length - 1); i >= 0; i-- ){
                this.setNodePriorityChecked(node._children.models[i]);
            }
        }

        return node;
    },

    setPriorityChecked: function(){

        this.setNodePriorityChecked(this);
        this.refreshTreeNodeState(this);
    }
});

fdm.models.YoutubeTreeModel = Backbone.TreeModel.extend({
    constructor: function tree(node) {
        Backbone.Model.prototype.constructor.apply(this, arguments);
        this._children = new this.collectionConstructor([], {

            model : fdm.models.YoutubeTreeModel

        });
        this._children.parent = this;
        if(node && node.children) this.add(node.children);
    }
});

fdm.models.YoutubeFileTree = fdm.models.FileTree.extend({
    constructor: function tree() {
        fdm.models.YoutubeTreeModel.prototype.constructor.apply(this, arguments);

        var has_checked = false;
        for (var i =0; i < this._children.length; i++){

            this.toggleOpen(this._children.models[i], true);
            if (this._children.models[i].get('checked'))
                has_checked = true;
        }

        if (this._children.length && !has_checked)
            this.setCheck(this._children.models[0], true);
    }
});