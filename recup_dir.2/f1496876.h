pyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {
'use strict';

/** @const */ var BookmarkList = bmm.BookmarkList;
/** @const */ var BookmarkTree = bmm.BookmarkTree;
/** @const */ var Command = cr.ui.Command;
/** @const */ var LinkKind = cr.LinkKind;
/** @const */ var ListItem = cr.ui.ListItem;
/** @const */ var Menu = cr.ui.Menu;
/** @const */ var MenuButton = cr.ui.MenuButton;
/** @const */ var Splitter = cr.ui.Splitter;
/** @const */ var TreeItem = cr.ui.TreeItem;

/**
 * An array containing the BookmarkTreeNodes that were deleted in the last
 * deletion action. This is used for implementing undo.
 * @type {?{nodes: Array<Array<BookmarkTreeNode>>, target: EventTarget}}
 */
var lastDeleted;

/**
 *
 * Holds the last DOMTimeStamp when mouse pointer hovers on folder in tree
 * view. Zero means pointer doesn't hover on folder.
 * @type {number}
 */
var lastHoverOnFolderTimeStamp = 0;

/**
 * Holds a function that will undo that last action, if global undo is enabled.
 * @type {Function}
 */
var performGlobalUndo;

/**
 * Holds a link controller singleton. Use getLinkController() rarther than
 * accessing this variabie.
 * @type {cr.LinkController}
 */
var linkController;

/**
 * New Windows are not allowed in Windows 8 metro mode.
 */
var canOpenNewWindows = true;

/**
 * Incognito mode availability can take the following values: ,
 *   - 'enabled' for when both normal and incognito modes are available;
 *   - 'disabled' for when incognito mode is disabled;
 *   - 'forced' for when incognito mode is forced (normal mode is unavailable).
 */
var incognitoModeAvailability = 'enabled';

/**
 * Whether bookmarks can be modified.
 * @type {boolean}
 */
var canEdit = true;

/**
 * @type {TreeItem}
 * @const
 */
var searchTreeItem = new TreeItem({
  bookmarkId: 'q='
});

/**
 * Command shortcut mapping.
 * @const
 */
var commandShortcutMap = cr.isMac ? {
  'edit': 'Enter',
  // On Mac we also allow Meta+Backspace.
  'delete': 'U+007F  U+0008 Meta-U+0008',
  'open-in-background-tab': 'Meta-Enter',
  'open-in-new-tab': 'Shift-Meta-Enter',
  'open-in-same-window': 'Meta-Down',
  'open-in-new-window': 'Shift-Enter',
  'rename-folder': 'Enter',
  // Global undo is Command-Z. It is not in any menu.
  'undo': 'Meta-U+005A',
} : {
  'edit': 'F2',
  'delete': 'U+007F',
  'open-in-background-tab': 'Ctrl-Enter',
  'open-in-new-tab': 'Shift-Ctrl-Enter',
  'open-in-same-window': 'Enter',
  'open-in-new-window': 'Shift-Enter',
  'rename-folder': 'F2',
  // Global undo is Ctrl-Z. It is not in any menu.
  'undo': 'Ctrl-U+005A',
};

/**
 * Mapping for folder id to suffix of UMA. These names will be appeared
 * after "BookmarkManager_NavigateTo_" in UMA dashboard.
 * @const
 */
var folderMetricsNameMap = {
  '1': 'BookmarkBar',
  '2': 'Other',
  '3': 'Mobile',
  'q=': 'Search',
  'subfolder': 'SubFolder',
};

/**
 * Adds an event listener to a node that will remove itself after firing once.
 * @param {!Element} node The DOM node to add the listener to.
 * @param {string} name The name of the event listener to add to.
 * @param {function(Event)} handler Function called when the event fires.
 */
function addOneShotEventListener(node, name, handler) {
  var f = function(e) {
    handler(e);
    node.removeEventListener(name, f);
  };
  node.addEventListener(name, f);
}

// Get the localized strings from the backend via bookmakrManagerPrivate API.
function loadLocalizedStrings(data) {
  // The strings may contain & which we need to strip.
  for (var key in data) {
    data[key] = data[key].replace(/&/, '');
  }

  loadTimeData.data = data;
  i18nTemplate.process(document, loadTimeData);

  searchTreeItem.label = loadTimeData.getString('search');
  searchTreeItem.icon = isRTL() ? 'images/bookmark_manager_search_rtl.png' :
                                  'images/bookmark_manager_search.png';
}

/**
 * Updates the location hash to reflect the current state of the application.
 */
function updateHash() {
  window.location.hash = bmm.tree.selectedItem.bookmarkId;
  updateAllCommands();
}

/**
 * Navigates to a bookmark ID.
 * @param {string} id The ID to navigate to.
 * @param {function()=} opt_callback Function called when list view loaded or
 *     displayed specified folder.
 */
function navigateTo(id, opt_callback) {
  window.location.hash = id;

  var sameParent = bmm.list.parentId == id;
  if (!sameParent)
    updateParentId(id);

  updateAllCommands();

  var metricsId = folderMetricsNameMap[id.replace(/^q=.*/, 'q=')] ||
                  folderMetricsNameMap['subfolder'];
  chrome.metricsPrivate.recordUserAction(
      'BookmarkManager_NavigateTo_' + metricsId);

  if (opt_callback) {
    if (sameParent)
      opt_callback();
    else
      addOneShotEventListener(bmm.list, 'load', opt_callback);
  }
}

/**
 * Updates the parent ID of the bookmark list and selects the correct tree item.
 * @param {string} id The id.
 */
function updateParentId(id) {
  // Setting list.parentId fires 'load' event.
  bmm.list.parentId = id;

  // When tree.selectedItem changed, tree view calls navigatTo() then it
  // calls updateHash() when list view displayed specified folder.
  bmm.tree.selectedItem = bmm.treeLookup[id] || bmm.tree.selectedItem;
}

// Process the location hash. This is called by onhashchange and when the page
// is first loaded.
function processHash() {
  var id = window.location.hash.slice(1);
  if (!id) {
    // If we do not have a hash, select first item in the tree.
    id = bmm.tree.items[0].bookmarkId;
  }

  var valid = false;
  if (/^e=/.test(id)) {
    id = id.slice(2);

    // If hash contains e=, edit the item specified.
    chrome.bookmarks.get(id, function(bookmarkNodes) {
      // Verify the node to edit is a valid node.
      if (!bookmarkNodes || bookmarkNodes.length != 1)
        return;
      var bookmarkNode = bookmarkNodes[0];

      // After the list reloads, edit the desired bookmark.
      var editBookmark = function() {
        var index = bmm.list.dataModel.findIndexById(bookmarkNode.id);
        if (index != -1) {
          var sm = bmm.list.selectionModel;
          sm.anchorIndex = sm.leadIndex = sm.selectedIndex = index;
          scrollIntoViewAndMakeEditable(index);
        }
      };

      var parentId = assert(bookmarkNode.parentId);
      navigateTo(parentId, editBookmark);
    });

    // We handle the two cases of navigating to the bookmark to be edited
    // above. Don't run the standard navigation code below.
    return;
  } else if (/^q=/.test(id)) {
    // In case we got a search hash, update the text input and the
    // bmm.treeLookup to use the new id.
    setSearch(id.slice(2));
    valid = true;
  }

  // Navigate to bookmark 'id' (which may be a query of the form q=query).
  if (valid) {
    updateParentId(id);
  } else {
    // We need to verify that this is a correct ID.
    chrome.bookmarks.get(id, function(items) {
      if (items && items.length == 1)
        updateParentId(id);
    });
  }
}

// Activate is handled by the open-in-same-window-command.
function handleDoubleClickForList(e) {
  if (e.button == 0)
    $('open-in-same-window-command').execute();
}

// The list dispatches an event when the user clicks on the URL or the Show in
// folder part.
function handleUrlClickedForList(e) {
  getLinkController().openUrlFromEvent(e.url, e.originalEvent);
  chrome.bookmarkManagerPrivate.recordLaunch();
}

function handleSearch(e) {
  setSearch(this.value);
}

/**
 * Navigates to the search results for the search text.
 * @param {string} searchText The text to search for.
 */
function setSearch(searchText) {
  if (searchText) {
    // Only update search item if we have a search term. We never want the
    // search item to be for an empty search.
    delete bmm.treeLookup[searchTreeItem.bookmarkId];
    var id = searchTreeItem.bookmarkId = 'q=' + searchText;
    bmm.treeLookup[searchTreeItem.bookmarkId] = searchTreeItem;
  }

  var input = $('term');
  // Do not update the input if the user is actively using the text input.
  if (document.activeElement != input)
    input.value = searchText;

  if (searchText) {
    bmm.tree.add(searchTreeItem);
    bmm.tree.selectedItem = searchTreeItem;
  } else {
    // Go "home".
    bmm.tree.selectedItem = bmm.tree.items[0];
    id = bmm.tree.selectedItem.bookmarkId;
  }

  navigateTo(id);
}

/**
 * This returns the user visible path to the folder where the bookmark is
 * located.
 * @param {number} parentId The ID of the parent folder.
 * @return {string|undefined} The path to the the bookmark,
 */
function getFolder(parentId) {
  var parentNode = bmm.tree.getBookmarkNodeById(parentId);
  if (parentNode) {
    var s = parentNode.title;
    if (parentNode.parentId != bmm.ROOT_ID) {
      return getFolder(parentNode.parentId) + '/' + s;
    }
    return s;
  }
}

function handleLoadForTree(e) {
  processHash();
}

/**
 * Returns a promise for all the URLs in the {@code nodes} and the direct
 * children of {@code nodes}.
 * @param {!Array<BookmarkTreeNode>} nodes .
 * @return {!Promise<Array<string>>} .
 */
function getAllUrls(nodes) {
  var urls = [];

  // Adds the node and all its direct children.
  // TODO(deepak.m1): Here node should exist. When we delete the nodes then
  // datamodel gets updated but still it shows deleted items as selected items
  // and accessing those nodes throws chrome.runtime.lastError. This cause
  // undefined value for node. Please refer https://crbug.com/480935.
  function addNodes(node) {
    if (!node || node.id == 'new')
      return;

    if (node.children) {
      node.children.forEach(function(child) {
        if (!bmm.isFolder(child))
          urls.push(child.url);
      });
    } else {
      urls.push(node.url);
    }
  }

  // Get a future promise for the nodes.
  var promises = nodes.map(function(node) {
    if (bmm.isFolder(assert(node)))
      return bmm.loadSubtree(node.id);
    // Not a folder so we already have all the data we need.
    return Promise.resolve(node);
  });

  return Promise.all(promises).then(function(nodes) {
    nodes.forEach(addNodes);
    return urls;
  });
}

/**
 * Returns the nodes (non recursive) to use for the open commands.
 * @param {HTMLElement} target
 * @return {!Array<BookmarkTreeNode>}
 */
function getNodesForOpen(target) {
  if (target == bmm.tree) {
    if (bmm.tree.selectedItem != searchTreeItem)
      return bmm.tree.selectedFolders;
    // Fall through to use all nodes in the list.
  } else {
    var items = bmm.list.selectedItems;
    if (items.length)
      return items;
  }

  // The list starts off with a null dataModel. We can get here during startup.
  if (!bmm.list.dataModel)
    return [];

  // Return an array based on the dataModel.
  return bmm.list.dataModel.slice();
}

/**
 * Returns a promise that will contain all URLs of all the selected bookmarks
 * and the nested bookmarks for use with the open commands.
 * @param {HTMLElement} target The target list or tree.
 * @return {Promise<Array<string>>} .
 */
function getUrlsForOpenCommands(target) {
  return getAllUrls(getNodesForOpen(target));
}

function notNewNode(node) {
  return node.id != 'new';
}

/**
 * Helper function that updates the canExecute and labels for the open-like
 * commands.
 * @param {!cr.ui.CanExecuteEvent} e The event fired by the command system.
 * @param {!cr.ui.Command} command The command we are currently processing.
 * @param {string} singularId The string id of singular form of the menu label.
 * @param {string} pluralId The string id of menu label if the singular form is
       not used.
 * @param {boolean} commandDisabled Whether the menu item should be disabled
       no matter what bookmarks are selected.
 */
function updateOpenCommand(e, command, singularId, pluralId, commandDisabled) {
  if (singularId) {
    // The command label reflects the selection which might not reflect
    // how many bookmarks will be opened. For example if you right click an
    // empty area in a folder with 1 bookmark the text should still say "all".
    var selectedNodes = getSelectedBookmarkNodes(e.target).filter(notNewNode);
    var singular = selectedNodes.length == 1 && !bmm.isFolder(selectedNodes[0]);
    command.label = loadTimeData.getString(singular ? singularId : pluralId);
  }

  if (commandDisabled) {
    command.disabled = true;
    e.canExecute = false;
    return;
  }

  getUrlsForOpenCommands(assertInstanceof(e.target, HTMLElement)).then(
      function(urls) {
    var disabled = !urls.length;
    command.disabled = disabled;
    e.canExecute = !disabled;
  });
}

/**
 * Calls the backend to figure out if we can paste the clipboard into the active
 * folder.
 * @param {Function=} opt_f Function to call after the state has been updated.
 */
function updatePasteCommand(opt_f) {
  function update(commandId, canPaste) {
    $(commandId).disabled = !canPaste;
  }

  var promises = [];

  // The folders menu.
  // We can not paste into search item in tree.
  if (bmm.tree.selectedItem && bmm.tree.selectedItem != searchTreeItem) {
    promises.push(new Promise(function(resolve) {
      var id = bmm.tree.selectedItem.bookmarkId;
      chrome.bookmarkManagerPrivate.canPaste(id, function(canPaste) {
        update('paste-from-folders-menu-command', canPaste);
        resolve(canPaste);
      });
    }));
  } else {
    // Tree's not loaded yet.
    update('paste-from-folders-menu-command', false);
  }

  // The organize menu.
  var listId = bmm.list.parentId;
  if (bmm.list.isSearch() || !listId) {
    // We cannot paste into search view or the list isn't ready.
    update('paste-from-organize-menu-command', false);
  } else {
    promises.push(new Promise(function(resolve) {
      chrome.bookmarkManagerPrivate.canPaste(listId, function(canPaste) {
        update('paste-from-organize-menu-command', canPaste);
        resolve(canPaste);
      });
    }));
  }

  Promise.all(promises).then(function() {
    var cmd;
    if (document.activeElement == bmm.list)
      cmd = 'paste-from-organize-menu-command';
    else if (document.activeElement == bmm.tree)
      cmd = 'paste-from-folders-menu-command';

    if (cmd)
      update('paste-from-context-menu-command', !$(cmd).disabled);

    if (opt_f) opt_f();
  });
}

function handleCanExecuteForSearchBox(e) {
  var command = e.command;
  switch (command.id) {
    case 'delete-command':
    case 'undo-command':
      // Pass the delete and undo commands through
      // (fixes http://crbug.com/278112).
      e.canExecute = false;
      break;
  }
}

function handleCanExecuteForDocument(e) {
  var command = e.command;
  switch (command.id) {
    case 'import-menu-command':
      e.canExecute = canEdit;
      break;

    case 'export-menu-command':
      // We can always execute the export-menu command.
      e.canExecute = true;
      break;

    case 'sort-command':
      e.canExecute = !bmm.list.isSearch() &&
          bmm.list.dataModel && bmm.list.dataModel.length > 1 &&
          !isUnmodifiable(bmm.tree.getBookmarkNodeById(bmm.list.parentId));
      break;

    case 'undo-command':
      // Because the global undo command has no visible UI, always enable it,
      // and just make it a no-op if undo is not possible.
      e.canExecute = true;
      break;

    default:
      canExecuteForList(e);
      if (!e.defaultPrevented)
        canExecuteForTree(e);
      break;
  }
}

/**
 * Helper function for handling canExecute for the list and the tree.
 * @param {!cr.ui.CanExecuteEvent} e Can execute event object.
 * @param {boolean} isSearch Whether the user is trying to do a command on
 *     search.
 */
function canExecuteShared(e, isSearch) {
  var command = e.command;
  switch (command.id) {
    case 'paste-from-folders-menu-command':
    case 'paste-from-organize-menu-command':
    case 'paste-from-context-menu-command':
      updatePasteCommand();
      break;

    case 'add-new-bookmark-command':
    case 'new-folder-command':
    case 'new-folder-from-folders-menu-command':
      var parentId = computeParentFolderForNewItem();
      var unmodifiable = isUnmodifiable(
          bmm.tree.getBookmarkNodeById(parentId));
      e.canExecute = !isSearch && canEdit && !unmodifiable;
      break;

    case 'open-in-new-tab-command':
      updateOpenCommand(e, command, 'open_in_new_tab', 'open_all', false);
      break;

    case 'open-in-background-tab-command':
      updateOpenCommand(e, command, '', '', false);
      break;

    case 'open-in-new-window-command':
      updateOpenCommand(e, command,
          'open_in_new_window', 'open_all_new_window',
          // Disabled when incognito is forced.
          incognitoModeAvailability == 'forced' || !canOpenNewWindows);
      break;

    case 'open-incognito-window-command':
      updateOpenCommand(e, command,
          'open_incognito', 'open_all_incognito',
          // Not available when incognito is disabled.
          incognitoModeAvailability == 'disabled');
      break;

    case 'undo-delete-command':
      e.canExecute = !!lastDeleted;
      break;
  }
}

/**
 * Helper function for handling canExecute for the list and document.
 * @param {!cr.ui.CanExecuteEvent} e Can execute event object.
 */
function canExecuteForList(e) {
  function hasSelected() {
    return !!bmm.list.selectedItem;
  }

  function hasSingleSelected() {
    return bmm.list.selectedItems.length == 1;
  }

  function canCopyItem(item) {
    return item.id != 'new';
  }

  function canCopyItems() {
    var selectedItems = bmm.list.selectedItems;
    return selectedItems && selectedItems.some(canCopyItem);
  }

  function isSearch() {
    return bmm.list.isSearch();
  }

  var command = e.command;
  switch (command.id) {
    case 'rename-folder-command':
      // Show rename if a single folder is selected.
      var items = bmm.list.selectedItems;
      if (items.length != 1) {
        e.canExecute = false;
        command.hidden = true;
      } else {
        var isFolder = bmm.isFolder(items[0]);
        e.canExecute = isFolder && canEdit && !hasUnmodifiable(items);
        command.hidden = !isFolder;
      }
      break;

    case 'edit-command':
      // Show the edit command if not a folder.
      var items = bmm.list.selectedItems;
      if (items.length != 1) {
        e.canExecute = false;
        command.hidden = false;
      } else {
        var isFolder = bmm.isFolder(items[0]);
        e.canExecute = !isFolder && canEdit && !hasUnmodifiable(items);
        command.hidden = isFolder;
      }
      break;

    case 'show-in-folder-command':
      e.canExecute = isSearch() && hasSingleSelected();
      break;

    case 'delete-command':
    case 'cut-command':
      e.canExecute = canCopyItems() && canEdit &&
          !hasUnmodifiable(bmm.list.selectedItems);
      break;

    case 'copy-command':
      e.canExecute = canCopyItems();
      break;

    case 'open-in-same-window-command':
      e.canExecute = (e.target == bmm.list) && hasSelected();
      break;

    default:
      canExecuteShared(e, isSearch());
  }
}

// Update canExecute for the commands when the list is the active element.
function handleCanExecuteForList(e) {
  if (e.target != bmm.list) return;
  canExecuteForList(e);
}

// Update canExecute for the commands when the tree is the active element.
function handleCanExecuteForTree(e) {
  if (e.target != bmm.tree) return;
  canExecuteForTree(e);
}

function canExecuteForTree(e) {
  function hasSelected() {
    return !!bmm.tree.selectedItem;
  }

  function isSearch() {
    return bmm.tree.selectedItem == searchTreeItem;
  }

  function isTopLevelItem() {
    return bmm.tree.selectedItem &&
           bmm.tree.selectedItem.parentNode == bmm.tree;
  }

  var command = e.command;
  switch (command.id) {
    case 'rename-folder-command':
    case 'rename-folder-from-folders-menu-command':
      command.hidden = false;
      e.canExecute = hasSelected() && !isTopLevelItem() && canEdit &&
          !hasUnmodifiable(bmm.tree.selectedFolders);
      break;

    case 'edit-command':
      command.hidden = true;
      e.canExecute = false;
      break;

    case 'delete-command':
    case 'delete-from-folders-menu-command':
    case 'cut-command':
    case 'cut-from-folders-menu-command':
      e.canExecute = hasSelected() && !isTopLevelItem() && canEdit &&
          !hasUnmodifiable(bmm.tree.selectedFolders);
      break;

    case 'copy-command':
    case 'copy-from-folders-menu-command':
      e.canExecute = hasSelected() && !isTopLevelItem();
      break;

    case 'undo-delete-from-folders-menu-command':
      e.canExecute = lastDeleted && lastDeleted.target == bmm.tree;
      break;

    default:
      canExecuteShared(e, isSearch());
  }
}

/**
 * Update the canExecute state of all the commands.
 */
function updateAllCommands() {
  var commands = document.querySelectorAll('command');
  for (var i = 0; i < commands.length; i++) {
    commands[i].canExecuteChange();
  }
}

function updateEditingCommands() {
  var editingCommands = [
    'add-new-bookmark',
    'cut',
    'cut-from-folders-menu',
    'delete',
    'edit',
    'new-folder',
    'paste-from-context-menu',
    'paste-from-folders-menu',
    'paste-from-organize-menu',
    'rename-folder',
    'sort',
  ];

  chrome.bookmarkManagerPrivate.canEdit(function(result) {
    if (result != canEdit) {
      canEdit = result;
      editingCommands.forEach(function(baseId) {
        $(baseId + '-command').canExecuteChange();
      });
    }
  });
}

function handleChangeForTree(e) {
  navigateTo(bmm.tree.selectedItem.bookmarkId);
}

function handleMenuButtonClicked(e) {
  updateEditingCommands();

  if (e.currentTarget.id == 'folders-menu') {
    $('copy-from-folders-menu-command').canExecuteChange();
    $('undo-delete-from-folders-menu-command').canExecuteChange();
  } else {
    $('copy-command').canExecuteChange();
  }
}

function handleRename(e) {
  var item = e.target;
  var bookmarkNode = item.bookmarkNode;
  chrome.bookmarks.update(bookmarkNode.id, {title: item.label});
  performGlobalUndo = null;  // This can't be undone, so disable global undo.
}

function handleEdit(e) {
  var item = e.target;
  var bookmarkNode = item.bookmarkNode;
  var context = {
    title: bookmarkNode.title
  };
  if (!bmm.isFolder(bookmarkNode))
    context.url = bookmarkNode.url;

  if (bookmarkNode.id == 'new') {
    selectItemsAfterUserAction(/** @type {BookmarkList} */(bmm.list));

    // New page
    context.parentId = bookmarkNode.parentId;
    chrome.bookmarks.create(context, function(node) {
      // A new node was created and will get added to the list due to the
      // handler.
      var dataModel = bmm.list.dataModel;
      var index = dataModel.indexOf(bookmarkNode);
      dataModel.splice(index, 1);

      // Select new item.
      var newIndex = dataModel.findIndexById(node.id);
      if (newIndex != -1) {
        var sm = bmm.list.selectionModel;
        bmm.list.scrollIndexIntoView(newIndex);
        sm.leadIndex = sm.anchorIndex = sm.selectedIndex = newIndex;
      }
    });
  } else {
    // Edit
    chrome.bookmarks.update(bookmarkNode.id, context);
  }
  performGlobalUndo = null;  // This can't be undone, so disable global undo.
}

function handleCancelEdit(e) {
  var item = e.target;
  var bookmarkNode = item.bookmarkNode;
  if (bookmarkNode.id == 'new') {
    var dataModel = bmm.list.dataModel;
    var index = dataModel.findIndexById('new');
    dataModel.splice(index, 1);
  }
}

/**
 * Navigates to the folder that the selected item is in and selects it. This is
 * used for the show-in-folder command.
 */
function showInFolder() {
  var bookmarkNode = bmm.list.selectedItem;
  if (!bookmarkNode)
    return;
  var parentId = bookmarkNode.parentId;

  // After the list is loaded we should select the revealed item.
  function selectItem() {
    var index = bmm.list.dataModel.findIndexById(bookmarkNode.id);
    if (index == -1)
      return;
    var sm = bmm.list.selectionModel;
    sm.anchorIndex = sm.leadIndex = sm.selectedIndex = index;
    bmm.list.scrollIndexIntoView(index);
  }

  var treeItem = bmm.treeLookup[parentId];
  treeItem.reveal();

  navigateTo(parentId, selectItem);
}

/**
 * @return {!cr.LinkController} The link controller used to open links based on
 *     user clicks and keyboard actions.
 */
function getLinkController() {
  return linkController ||
      (linkController = new cr.LinkController(loadTimeData));
}

/**
 * Returns the selected bookmark nodes of the provided tree or list.
 * If |opt_target| is not provided or null the active element is used.
 * Only call this if the list or the tree is focused.
 * @param {EventTarget=} opt_target The target list or tree.
 * @return {!Array} Array of bookmark nodes.
 */
function getSelectedBookmarkNodes(opt_target) {
  return (opt_target || document.activeElement) == bmm.tree ?
      bmm.tree.selectedFolders : bmm.list.selectedItems;
}

/**
 * @param {EventTarget=} opt_target The target list or tree.
 * @return {!Array<string>} An array of the selected bookmark IDs.
 */
function getSelectedBookmarkIds(opt_target) {
  var selectedNodes = getSelectedBookmarkNodes(opt_target);
  selectedNodes.sort(function(a, b) { return a.index - b.index });
  return selectedNodes.map(function(node) {
    return node.id;
  });
}

/**
 * @param {BookmarkTreeNode} node The node to test.
 * @return {boolean} Whether the given node is unmodifiable.
 */
function isUnmodifiable(node) {
  return !!(node && node.unmodifiable);
}

/**
 * @param {Array<BookmarkTreeNode>} nodes A list of BookmarkTreeNodes.
 * @return {boolean} Whether any of the nodes is managed.
 */
function hasUnmodifiable(nodes) {
  return nodes.some(isUnmodifiable);
}

/**
 * Opens the selected bookmarks.
 * @param {cr.LinkKind} kind The kind of link we want to open.
 * @param {HTMLElement=} opt_eventTarget The target of the user initiated event.
 */
function openBookmarks(kind, opt_eventTarget) {
  // If we have selected any folders, we need to find all the bookmarks one
  // level down. We use multiple async calls to getSubtree instead of getting
  // the whole tree since we would like to minimize the amount of data sent.

  var urlsP = getUrlsForOpenCommands(opt_eventTarget ? opt_eventTarget : null);
  urlsP.then(function(urls) {
    getLinkController().openUrls(assert(urls), kind);
    chrome.bookmarkManagerPrivate.recordLaunch();
  });
}

/**
 * Opens an item in the list.
 */
function openItem() {
  var bookmarkNodes = getSelectedBookmarkNodes();
  // If we double clicked or pressed enter on a single folder, navigate to it.
  if (bookmarkNodes.length == 1 && bmm.isFolder(bookmarkNodes[0]))
    navigateTo(bookmarkNodes[0].id);
  else
    openBookmarks(LinkKind.FOREGROUND_TAB);
}

/**
 * Refreshes search results after delete or undo-delete.
 * This ensures children of deleted folders do not remain in results
 */
function updateSearchResults() {
  if (bmm.list.isSearch())
    bmm.list.reload();
}

/**
 * Deletes the selected bookmarks. The bookmarks are saved in memory in case
 * the user needs to undo the deletion.
 * @param {EventTarget=} opt_target The deleter of bookmarks.
 */
function deleteBookmarks(opt_target) {
  var selectedIds = getSelectedBookmarkIds(opt_target);
  if (!selectedIds.length)
    return;

  var filteredIds = getFilteredSelectedBookmarkIds(opt_target);
  lastDeleted = {nodes: [], target: opt_target || document.activeElement};

  function performDelete() {
    // Only remove filtered ids.
    chrome.bookmarkManagerPrivate.removeTrees(filteredIds);
    $('undo-delete-command').canExecuteChange();
    $('undo-delete-from-folders-menu-command').canExecuteChange();
    performGlobalUndo = undoDelete;
  }

  // First, store information about the bookmarks being deleted.
  // Store all selected ids.
  selectedIds.forEach(function(id) {
    chrome.bookmarks.getSubTree(id, function(results) {
      lastDeleted.nodes.push(results);

      // When all nodes have been saved, perform the deletion.
      if (lastDeleted.nodes.length === selectedIds.length) {
        performDelete();
        updateSearchResults();
      }
    });
  });
}

/**
 * Restores a tree of bookmarks under a specified folder.
 * @param {BookmarkTreeNode} node The node to restore.
 * @param {(string|number)=} opt_parentId If a string is passed, it's the ID of
 *     the folder to restore under. If not specified or a number is passed, the
 *     original parentId of the node will be used.
 */
function restoreTree(node, opt_parentId) {
  var bookmarkInfo = {
    parentId: typeof opt_parentId == 'string' ? opt_parentId : node.parentId,
    title: node.title,
    index: node.index,
    url: node.url
  };

  chrome.bookmarks.create(bookmarkInfo, function(result) {
    if (!result) {
      console.error('Failed to restore bookmark.');
      return;
    }

    if (node.children) {
      // Restore the children using the new ID for this node.
      node.children.forEach(function(child) {
        restoreTree(child, result.id);
      });
    }

    updateSearchResults();
  });
}

/**
 * Restores the last set of bookmarks that was deleted.
 */
function undoDelete() {
  lastDeleted.nodes.forEach(function(arr) {
    arr.forEach(restoreTree);
  });
  lastDeleted = null;
  $('undo-delete-command').canExecuteChange();
  $('undo-delete-from-folders-menu-command').canExecuteChange();

  // Only a single level of undo is supported, so disable global undo now.
  performGlobalUndo = null;
}

/**
 * Computes folder for "Add Page" and "Add Folder".
 * @return {string} The id of folder node where we'll create new page/folder.
 */
function computeParentFolderForNewItem() {
  if (document.activeElement == bmm.tree)
    return bmm.list.parentId;
  var selectedItem = bmm.list.selectedItem;
  return selectedItem && bmm.isFolder(selectedItem) ?
      selectedItem.id : bmm.list.parentId;
}

/**
 * Callback for rename folder and edit command. This starts editing for
 * the passed in target, or the selected item.
 * @param {EventTarget=} opt_target The target to start editing. If absent or
 *     null, the selected item will be edited instead.
 */
function editItem(opt_target) {
  if ((opt_target || document.activeElement) == bmm.tree) {
    bmm.tree.selectedItem.editing = true;
  } else {
    var li = bmm.list.getListItem(bmm.list.selectedItem);
    if (li)
      li.editing = true;
  }
}

/**
 * Callback for the new folder command. This creates a new folder and starts
 * a rename of it.
 * @param {EventTarget=} opt_target The target to create a new folder in.
 */
function newFolder(opt_target) {
  performGlobalUndo = null;  // This can't be undone, so disable global undo.

  var parentId = computeParentFolderForNewItem();
  var selectedItems = bmm.list.selectedItems;
  var newIndex;
  // Callback is called after tree and list data model updated.
  function createFolder(callback) {
    if (selectedItems.length == 1 && document.activeElement != bmm.tree &&
        !bmm.isFolder(selectedItems[0]) && selectedItems[0].id != 'new') {
      newIndex = bmm.list.dataModel.indexOf(selectedItems[0]) + 1;
    }
    chrome.bookmarks.create({
      title: loadTimeData.getString('new_folder_name'),
      parentId: parentId,
      index: newIndex
    }, callback);
  }

  if ((opt_target || document.activeElement) == bmm.tree) {
    createFolder(function(newNode) {
      navigateTo(newNode.id, function() {
        bmm.treeLookup[newNode.id].editing = true;
      });
    });
    return;
  }

  function editNewFolderInList() {
    createFolder(function(newNode) {
      var index = newNode.index;
      var sm = bmm.list.selectionModel;
      sm.anchorIndex = sm.leadIndex = sm.selectedIndex = index;
      scrollIntoViewAndMakeEditable(index);
    });
  }

  navigateTo(parentId, editNewFolderInList);
}

/**
 * Scrolls the list item into view and makes it editable.
 * @param {number} index The index of the item to make editable.
 */
function scrollIntoViewAndMakeEditable(index) {
  bmm.list.scrollIndexIntoView(index);
  // onscroll is now dispatched asynchronously so we have to postpone
  // the rest.
  setTimeout(function() {
    var item = bmm.list.getListItemByIndex(index);
    if (item)
      item.editing = true;
  }, 0);
}

/**
 * Adds a page to the current folder. This is called by the
 * add-new-bookmark-command handler.
 */
function addPage() {
  var parentId = computeParentFolderForNewItem();
  var selectedItems = bmm.list.selectedItems;
  var newIndex;
  function editNewBookmark() {
    if (selectedItems.length == 1 && document.activeElement != bmm.tree &&
        !bmm.isFolder(selectedItems[0])) {
      newIndex = bmm.list.dataModel.indexOf(selectedItems[0]) + 1;
    }

    var fakeNode = {
      title: '',
      url: '',
      parentId: parentId,
      index: newIndex,
      id: 'new'
    };
    var dataModel = bmm.list.dataModel;
    var index = dataModel.length;
    if (newIndex != undefined)
      index = newIndex;
    dataModel.splice(index, 0, fakeNode);
    var sm = bmm.list.selectionModel;
    sm.anchorIndex = sm.leadIndex = sm.selectedIndex = index;
    scrollIntoViewAndMakeEditable(index);
  };

  navigateTo(parentId, editNewBookmark);
}

/**
 * This function is used to select items after a user action such as paste, drop
 * add page etc.
 * @param {BookmarkList|BookmarkTree} target The target of the user action.
 * @param {string=} opt_selectedTreeId If provided, then select that tree id.
 */
function selectItemsAfterUserAction(target, opt_selectedTreeId) {
  // We get one onCreated event per item so we delay the handling until we get
  // no more events coming.

  var ids = [];
  var timer;

  function handle(id, bookmarkNode) {
    clearTimeout(timer);
    if (opt_selectedTreeId || bmm.list.parentId == bookmarkNode.parentId)
      ids.push(id);
    timer = setTimeout(handleTimeout, 50);
  }

  function handleTimeout() {
    chrome.bookmarks.onCreated.removeListener(handle);
    chrome.bookmarks.onMoved.removeListener(handle);

    if (opt_selectedTreeId && ids.indexOf(opt_selectedTreeId) != -1) {
      var index = ids.indexOf(opt_selectedTreeId);
      if (index != -1 && opt_selectedTreeId in bmm.treeLookup) {
        bmm.tree.selectedItem = bmm.treeLookup[opt_selectedTreeId];
      }
    } else if (target == bmm.list) {
      var dataModel = bmm.list.dataModel;
      var firstIndex = dataModel.findIndexById(ids[0]);
      var lastIndex = dataModel.findIndexById(ids[ids.length - 1]);
      if (firstIndex != -1 && lastIndex != -1) {
        var selectionModel = bmm.list.selectionModel;
        selectionModel.selectedIndex = -1;
        selectionModel.selectRange(firstIndex, lastIndex);
        selectionModel.anchorIndex = selectionModel.leadIndex = lastIndex;
        bmm.list.focus();
      }
    }

    bmm.list.endBatchUpdates();
  }

  bmm.list.startBatchUpdates();

  chrome.bookmarks.onCreated.addListener(handle);
  chrome.bookmarks.onMoved.addListener(handle);
  timer = setTimeout(handleTimeout, 300);
}

/**
 * Record user action.
 * @param {string} name An user action name.
 */
function recordUserAction(name) {
  chrome.metricsPrivate.recordUserAction('BookmarkManager_Command_' + name);
}

/**
 * The currently selected bookmark, based on where the user is clicking.
 * @return {string} The ID of the currently selected bookmark (could be from
 *     tree view or list view).
 */
function getSelectedId() {
  if (document.activeElement == bmm.tree)
    return bmm.tree.selectedItem.bookmarkId;
  var selectedItem = bmm.list.selectedItem;
  return selectedItem && bmm.isFolder(selectedItem) ?
      selectedItem.id : bmm.tree.selectedItem.bookmarkId;
}

/**
 * Pastes the copied/cutted bookmark into the right location depending whether
 * if it was called from Organize Menu or from Context Menu.
 * @param {string} id The id of the element being pasted from.
 */
function pasteBookmark(id) {
  recordUserAction('Paste');
  selectItemsAfterUserAction(/** @type {BookmarkList} */(bmm.list));
  chrome.bookmarkManagerPrivate.paste(id, getSelectedBookmarkIds());
}

/**
 * Returns true if child is contained in another selected folder.
 * Traces parent nodes up the tree until a selected ancestor or root is found.
 */
function hasSelectedAncestor(parentNode) {
  function contains(arr, item) {
    for (var i = 0; i < arr.length; i++)
        if (arr[i] === item)
          return true;
    return false;
  }

  // Don't search top level, cannot select permanent nodes in search.
  if (parentNode == null || parentNode.id <= 2)
    return false;

  // Found selected ancestor.
  if (contains(getSelectedBookmarkNodes(), parentNode))
    return true;

  // Keep digging.
  return hasSelectedAncestor(
      bmm.tree.getBookmarkNodeById(parentNode.parentId));
}

/**
 * @param {EventTarget=} opt_target A target to get bookmark IDs from.
 * @return {Array<string>} An array of bookmarks IDs.
 */
function getFilteredSelectedBookmarkIds(opt_target) {
  // Remove duplicates from filteredIds and return.
  var filteredIds = [];
  // Selected nodes to iterate through for matches.
  var nodes = getSelectedBookmarkNodes(opt_target);

  for (var i = 0; i < nodes.length; i++)
    if (!hasSelectedAncestor(bmm.tree.getBookmarkNodeById(nodes[i].parentId)))
      filteredIds.splice(0, 0, nodes[i].id);

  return filteredIds;
}

/**
 * Handler for the command event. This is used for context menu of list/tree
 * and organized menu.
 * @param {!Event} e The event object.
 */
function handleCommand(e) {
  var command = e.command;
  var target = assertInstanceof(e.target, HTMLElement);
  switch (command.id) {
    case 'import-menu-command':
      recordUserAction('Import');
      chrome.bookmarks.import();
      break;

    case 'export-menu-command':
      recordUserAction('Export');
      chrome.bookmarks.export();
      break;

    case 'undo-command':
      if (performGlobalUndo) {
        recordUserAction('UndoGlobal');
        performGlobalUndo();
      } else {
        recordUserAction('UndoNone');
      }
      break;

    case 'show-in-folder-command':
      recordUserAction('ShowInFolder');
      showInFolder();
      break;

    case 'open-in-new-tab-command':
    case 'open-in-background-tab-command':
      recordUserAction('OpenInNewTab');
      openBookmarks(LinkKind.BACKGROUND_TAB, target);
      break;

    case 'open-in-new-window-command':
      recordUserAction('OpenInNewWindow');
      openBookmarks(LinkKind.WINDOW, target);
      break;

    case 'open-incognito-window-command':
      recordUserAction('OpenIncognito');
      openBookmarks(LinkKind.INCOGNITO, target);
      break;

    case 'delete-from-folders-menu-command':
      target = bmm.tree;
    case 'delete-command':
      recordUserAction('Delete');
      deleteBookmarks(target);
      break;

    case 'copy-from-folders-menu-command':
      target = bmm.tree;
    case 'copy-command':
      recordUserAction('Copy');
      chrome.bookmarkManagerPrivate.copy(getSelectedBookmarkIds(target),
                                         updatePasteCommand);
      break;

    case 'cut-from-folders-menu-command':
      target = bmm.tree;
    case 'cut-command':
      recordUserAction('Cut');
      chrome.bookmarkManagerPrivate.cut(getSelectedBookmarkIds(target),
                                        function() {
                                          updatePasteCommand();
                                          updateSearchResults();
                                        });
      break;

    case 'paste-from-organize-menu-command':
      pasteBookmark(bmm.list.parentId);
      break;

    case 'paste-from-folders-menu-command':
      pasteBookmark(bmm.tree.selectedItem.bookmarkId);
      break;

    case 'paste-from-context-menu-command':
      pasteBookmark(getSelectedId());
      break;

    case 'sort-command':
      recordUserAction('Sort');
      chrome.bookmarkManagerPrivate.sortChildren(bmm.list.parentId);
      break;


    case 'rename-folder-from-folders-menu-command':
      target = bmm.tree;
    case 'rename-folder-command':
      editItem(target);
      break;

    case 'edit-command':
      recordUserAction('Edit');
      editItem();
      break;

    case 'new-folder-from-folders-menu-command':
      target = bmm.tree;
    case 'new-folder-command':
      recordUserAction('NewFolder');
      newFolder(target);
      break;

    case 'add-new-bookmark-command':
      recordUserAction('AddPage');
      addPage();
      break;

    case 'open-in-same-window-command':
      recordUserAction('OpenInSame');
      openItem();
      break;

    case 'undo-delete-command':
    case 'undo-delete-from-folders-menu-command':
      recordUserAction('UndoDelete');
      undoDelete();
      break;
  }
}

// Execute the copy, cut and paste commands when those events are dispatched by
// the browser. This allows us to rely on the browser to handle the keyboard
// shortcuts for these commands.
function installEventHandlerForCommand(eventName, commandId) {
  function handle(e) {
    if (document.activeElement != bmm.list &&
        document.activeElement != bmm.tree)
      return;
    var command = $(commandId);
    if (!command.disabled) {
      command.execute();
      if (e)
        e.preventDefault();  // Prevent the system beep.
    }
  }
  if (eventName == 'paste') {
    // Paste is a bit special since we need to do an async call to see if we
    // can paste because the paste command might not be up to date.
    document.addEventListener(eventName, function(e) {
      updatePasteCommand(handle);
    });
  } else {
    document.addEventListener(eventName, handle);
  }
}

function initializeSplitter() {
  var splitter = document.querySelector('.main > .splitter');
  Splitter.decorate(splitter);

  var splitterStyle = splitter.previousElementSibling.style;

  // The splitter persists the size of the left component in the local store.
  if ('treeWidth' in window.localStorage)
    splitterStyle.width = window.localStorage['treeWidth'];

  splitter.addEventListener('resize', function(e) {
    window.localStorage['treeWidth'] = splitterStyle.width;
  });
}

function initializeBookmarkManager() {
  // Sometimes the extension API is not initialized.
  if (!chrome.bookmarks)
    console.error('Bookmarks extension API is not available');

  chrome.bookmarkManagerPrivate.getStrings(continueInitializeBookmarkManager);
}

function continueInitializeBookmarkManager(localizedStrings) {
  loadLocalizedStrings(localizedStrings);

  bmm.treeLookup[searchTreeItem.bookmarkId] = searchTreeItem;

  cr.ui.decorate('cr-menu', Menu);
  cr.ui.decorate('button[menu]', MenuButton);
  cr.ui.decorate('command', Command);
  BookmarkList.decorate($('list'));
  BookmarkTree.decorate($('tree'));

  bmm.list.addEventListener('canceledit', handleCancelEdit);
  bmm.list.addEventListener('canExecute', handleCanExecuteForList);
  bmm.list.addEventListener('change', updateAllCommands);
  bmm.list.addEventListener('contextmenu', updateEditingCommands);
  bmm.list.addEventListener('dblclick', handleDoubleClickForList);
  bmm.list.addEventListener('edit', handleEdit);
  bmm.list.addEventListener('rename', handleRename);
  bmm.list.addEventListener('urlClicked', handleUrlClickedForList);

  bmm.tree.addEventListener('canExecute', handleCanExecuteForTree);
  bmm.tree.addEventListener('change', handleChangeForTree);
  bmm.tree.addEventListener('contextmenu', updateEditingCommands);
  bmm.tree.addEventListener('rename', handleRename);
  bmm.tree.addEventListener('load', handleLoadForTree);

  cr.ui.contextMenuHandler.addContextMenuProperty(
      /** @type {!Element} */(bmm.tree));
  bmm.list.contextMenu = $('context-menu');
  bmm.tree.contextMenu = $('context-menu');

  // We listen to hashchange so that we can update the currently shown folder
  // when // the user goes back and forward in the history.
  window.addEventListener('hashchange', processHash);

  document.querySelector('header form').onsubmit =
      /** @type {function(Event=)} */(function(e) {
    setSearch($('term').value);
    e.preventDefault();
  });

  $('term').addEventListener('search', handleSearch);
  $('term').addEventListener('canExecute', handleCanExecuteForSearchBox);

  $('folders-button').addEventListener('click', handleMenuButtonClicked);
  $('organize-button').addEventListener('click', handleMenuButtonClicked);

  document.addEventListener('canExecute', handleCanExecuteForDocument);
  document.addEventListener('command', handleCommand);

  // Listen to copy, cut and paste events and execute the associated commands.
  installEventHandlerForCommand('copy', 'copy-command');
  installEventHandlerForCommand('cut', 'cut-command');
  installEventHandlerForCommand('paste', 'paste-from-organize-menu-command');

  // Install shortcuts
  for (var name in commandShortcutMap) {
    $(name + '-command').shortcut = commandShortcutMap[name];
  }

  // Disable almost all commands at startup.
  var commands = document.querySelectorAll('command');
  for (var i = 0, command; command = commands[i]; ++i) {
    if (command.id != 'import-menu-command' &&
        command.id != 'export-menu-command') {
      command.disabled = true;
    }
  }

  chrome.bookmarkManagerPrivate.canEdit(function(result) {
    canEdit = result;
  });

  chrome.systemPrivate.getIncognitoModeAvailability(function(result) {
    // TODO(rustema): propagate policy value to the bookmark manager when it
    // changes.
    incognitoModeAvailability = result;
  });

  chrome.bookmarkManagerPrivate.canOpenNewWindows(function(result) {
    canOpenNewWindows = result;
  });

  cr.ui.FocusOutlineManager.forDocument(document);
  initializeSplitter();
  bmm.addBookmarkModelListeners();
  dnd.init(selectItemsAfterUserAction);
  bmm.tree.reload();
}

initializeBookmarkManager();
})();
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// TODO(arv): Now that this is driven by a data model, implement a data model
//            that handles the loading and the events from the bookmark backend.

/**
 * @typedef {{childIds: Array<string>}}
 *
 * @see chrome/common/extensions/api/bookmarks.json
 */
var ReorderInfo;

/**
 * @typedef {{parentId: string,
 *            index: number,
 *            oldParentId: string,
 *            oldIndex: number}}
 *
 * @see chrome/common/extensions/api/bookmarks.json
 */
var MoveInfo;

cr.define('bmm', function() {
  'use strict';

  var List = cr.ui.List;
  var ListItem = cr.ui.ListItem;
  var ArrayDataModel = cr.ui.ArrayDataModel;
  var ContextMenuButton = cr.ui.ContextMenuButton;

  /**
   * Basic array data model for use with bookmarks.
   * @param {!Array<!BookmarkTreeNode>} items The bookmark items.
   * @constructor
   * @extends {ArrayDataModel}
   */
  function BookmarksArrayDataModel(items) {
    ArrayDataModel.call(this, items);
  }

  BookmarksArrayDataModel.prototype = {
    __proto__: ArrayDataModel.prototype,

    /**
     * Finds the index of the bookmark with the given ID.
     * @param {string} id The ID of the bookmark node to find.
     * @return {number} The index of the found node or -1 if not found.
     */
    findIndexById: function(id) {
      for (var i = 0; i < this.length; i++) {
        if (this.item(i).id == id)
          return i;
      }
      return -1;
    }
  };

  /**
   * Removes all children and appends a new child.
   * @param {!Node} parent The node to remove all children from.
   * @param {!Node} newChild The new child to append.
   */
  function replaceAllChildren(parent, newChild) {
    var n;
    while ((n = parent.lastChild)) {
      parent.removeChild(n);
    }
    parent.appendChild(newChild);
  }

  /**
   * Creates a new bookmark list.
   * @param {Object=} opt_propertyBag Optional properties.
   * @constructor
   * @extends {cr.ui.List}
   */
  var BookmarkList = cr.ui.define('list');

  BookmarkList.prototype = {
    __proto__: List.prototype,

    /** @override */
    decorate: function() {
      List.prototype.decorate.call(this);
      this.addEventListener('mousedown', this.handleMouseDown_);

      // HACK(arv): http://crbug.com/40902
      window.addEventListener('resize', this.redraw.bind(this));

      // We could add the ContextMenuButton in the BookmarkListItem but it slows
      // down redraws a lot so we do this on mouseovers instead.
      this.addEventListener('mouseover', this.handleMouseOver_.bind(this));

      bmm.list = this;
    },

    /**
     * @param {!BookmarkTreeNode} bookmarkNode
     * @override
     */
    createItem: function(bookmarkNode) {
      return new BookmarkListItem(bookmarkNode);
    },

    /** @private {string} */
    parentId_: '',

    /** @private {number} */
    loadCount_: 0,

    /**
     * Reloads the list from the bookmarks backend.
     */
    reload: function() {
      var parentId = this.parentId;

      var callback = this.handleBookmarkCallback_.bind(this);

      this.loadCount_++;

      if (!parentId)
        callback([]);
      else if (/^q=/.test(parentId))
        chrome.bookmarks.search(parentId.slice(2), callback);
      else
        chrome.bookmarks.getChildren(parentId, callback);
    },

    /**
     * Callback function for loading items.
     * @param {Array<!BookmarkTreeNode>} items The loaded items.
     * @private
     */
    handleBookmarkCallback_: function(items) {
      this.loadCount_--;
      if (this.loadCount_)
        return;

      if (!items) {
        // Failed to load bookmarks. Most likely due to the bookmark being
        // removed.
        cr.dispatchSimpleEvent(this, 'invalidId');
        return;
      }

      this.dataModel = new BookmarksArrayDataModel(items);

      this.fixWidth_();
      cr.dispatchSimpleEvent(this, 'load');
    },

    /**
     * The bookmark node that the list is currently displaying. If we are
     * currently displaying search this returns null.
     * @type {BookmarkTreeNode}
     */
    get bookmarkNode() {
      if (this.isSearch())
        return null;
      var treeItem = bmm.treeLookup[this.parentId];
      return treeItem && treeItem.bookmarkNode;
    },

    /**
     * @return {boolean} Whether we are currently showing search results.
     */
    isSearch: function() {
      return this.parentId_[0] == 'q';
    },

    /**
     * @return {boolean} Whether we are editing an ephemeral item.
     */
    hasEphemeral: function() {
      var dataModel = this.dataModel;
      for (var i = 0; i < dataModel.array_.length; i++) {
        if (dataModel.array_[i].id == 'new')
          return true;
      }
      return false;
    },

    /**
     * Handles mouseover on the list so that we can add the context menu button
     * lazily.
     * @private
     * @param {!Event} e The mouseover event object.
     */
    handleMouseOver_: function(e) {
      var el = e.target;
      while (el && el.parentNode != this) {
        el = el.parentNode;
      }

      if (el && el.parentNode == this &&
          !el.editing &&
          !(el.lastChild instanceof ContextMenuButton)) {
        el.appendChild(new ContextMenuButton);
      }
    },

    /**
     * Dispatches an urlClicked event which is used to open URLs in new
     * tabs etc.
     * @private
     * @param {string} url The URL that was clicked.
     * @param {!Event} originalEvent The original click event object.
     */
    dispatchUrlClickedEvent_: function(url, originalEvent) {
      var event = new Event('urlClicked', {bubbles: true});
      event.url = url;
      event.originalEvent = originalEvent;
      this.dispatchEvent(event);
    },

    /**
     * Handles mousedown events so that we can prevent the auto scroll as
     * necessary.
     * @private
     * @param {!Event} e The mousedown event object.
     */
    handleMouseDown_: function(e) {
      e = /** @type {!MouseEvent} */(e);
      if (e.button == 1) {
        // WebKit no longer fires click events for middle clicks so we manually
        // listen to mouse up to dispatch a click event.
        this.addEventListener('mouseup', this.handleMiddleMouseUp_);

        // When the user does a middle click we need to prevent the auto scroll
        // in case the user is trying to middle click to open a bookmark in a
        // background tab.
        // We do not do this in case the target is an input since middle click
        // is also paste on Linux and we don't want to break that.
        if (e.target.tagName != 'INPUT')
          e.preventDefault();
      }
    },

    /**
     * WebKit no longer dispatches click events for middle clicks so we need
     * to emulate it.
     * @private
     * @param {!Event} e The mouse up event object.
     */
    handleMiddleMouseUp_: function(e) {
      e = /** @type {!MouseEvent} */(e);
      this.removeEventListener('mouseup', this.handleMiddleMouseUp_);
      if (e.button == 1) {
        var el = e.target;
        while (el.parentNode != this) {
          el = el.parentNode;
        }
        var node = el.bookmarkNode;
        if (node && !bmm.isFolder(node))
          this.dispatchUrlClickedEvent_(node.url, e);
      }
      e.preventDefault();
    },

    // Bookmark model update callbacks
    handleBookmarkChanged: function(id, changeInfo) {
      var dataModel = this.dataModel;
      var index = dataModel.findIndexById(id);
      if (index != -1) {
        var bookmarkNode = this.dataModel.item(index);
        bookmarkNode.title = changeInfo.title;
        if ('url' in changeInfo)
          bookmarkNode.url = changeInfo['url'];

        dataModel.updateIndex(index);
      }
    },

    /**
     * @param {string} id
     * @param {ReorderInfo} reorderInfo
     */
    handleChildrenReordered: function(id, reorderInfo) {
      if (this.parentId == id) {
        // We create a new data model with updated items in the right order.
        var dataModel = this.dataModel;
        var items = {};
        for (var i = this.dataModel.length - 1; i >= 0; i--) {
          var bookmarkNode = dataModel.item(i);
          items[bookmarkNode.id] = bookmarkNode;
        }
        var newArray = [];
        for (var i = 0; i < reorderInfo.childIds.length; i++) {
          newArray[i] = items[reorderInfo.childIds[i]];
          newArray[i].index = i;
        }

        this.dataModel = new BookmarksArrayDataModel(newArray);
      }
    },

    handleCreated: function(id, bookmarkNode) {
      if (this.parentId == bookmarkNode.parentId)
        this.dataModel.splice(bookmarkNode.index, 0, bookmarkNode);
    },

    /**
     * @param {string} id
     * @param {MoveInfo} moveInfo
     */
    handleMoved: function(id, moveInfo) {
      if (moveInfo.parentId == this.parentId ||
          moveInfo.oldParentId == this.parentId) {

        var dataModel = this.dataModel;

        if (moveInfo.oldParentId == moveInfo.parentId) {
          // Reorder within this folder

          this.startBatchUpdates();

          var bookmarkNode = this.dataModel.item(moveInfo.oldIndex);
          this.dataModel.splice(moveInfo.oldIndex, 1);
          this.dataModel.splice(moveInfo.index, 0, bookmarkNode);

          this.endBatchUpdates();
        } else {
          if (moveInfo.oldParentId == this.parentId) {
            // Move out of this folder

            var index = dataModel.findIndexById(id);
            if (index != -1)
              dataModel.splice(index, 1);
          }

          if (moveInfo.parentId == this.parentId) {
            // Move to this folder
            var self = this;
            chrome.bookmarks.get(id, function(bookmarkNodes) {
              var bookmarkNode = bookmarkNodes[0];
              dataModel.splice(bookmarkNode.index, 0, bookmarkNode);
            });
          }
        }
      }
    },

    handleRemoved: function(id, removeInfo) {
      var dataModel = this.dataModel;
      var index = dataModel.findIndexById(id);
      if (index != -1)
        dataModel.splice(index, 1);
    },

    /**
     * Workaround for http://crbug.com/40902
     * @private
     */
    fixWidth_: function() {
      var list = bmm.list;
      if (this.loadCount_ || !list)
        return;

      // The width of the list is wrong after its content has changed.
      // Fortunately the reported offsetWidth is correct so we can detect the
      //incorrect width.
      if (list.offsetWidth != list.parentNode.clientWidth - list.offsetLeft) {
        // Set the width to the correct size. This causes the relayout.
        list.style.width = list.parentNode.clientWidth - list.offsetLeft + 'px';
        // Remove the temporary style.width in a timeout. Once the timer fires
        // the size should not change since we already fixed the width.
        window.setTimeout(function() {
          list.style.width = '';
        }, 0);
      }
    }
  };

  /**
   * The ID of the bookmark folder we are displaying.
   */
  cr.defineProperty(BookmarkList, 'parentId', cr.PropertyKind.JS,
                    function() {
                      this.reload();
                    });

  /**
   * The contextMenu property.
   */
  cr.ui.contextMenuHandler.addContextMenuProperty(BookmarkList);
  /** @type {cr.ui.Menu} */
  BookmarkList.prototype.contextMenu;

  /**
   * Creates a new bookmark list item.
   * @param {!BookmarkTreeNode} bookmarkNode The bookmark node this represents.
   * @constructor
   * @extends {cr.ui.ListItem}
   */
  function BookmarkListItem(bookmarkNode) {
    var el = cr.doc.createElement('div');
    el.bookmarkNode = bookmarkNode;
    BookmarkListItem.decorate(el);
    return el;
  }

  /**
   * Decorates an element as a bookmark list item.
   * @param {!HTMLElement} el The element to decorate.
   */
  BookmarkListItem.decorate = function(el) {
    el.__proto__ = BookmarkListItem.prototype;
    el.decorate();
  };

  BookmarkListItem.prototype = {
    __proto__: ListItem.prototype,

    /** @override */
    decorate: function() {
      ListItem.prototype.decorate.call(this);

      var bookmarkNode = this.bookmarkNode;

      this.draggable = true;

      var labelEl = this.ownerDocument.createElement('div');
      labelEl.className = 'label';
      labelEl.textContent = bookmarkNode.title;

      var urlEl = this.ownerDocument.createElement('div');
      urlEl.className = 'url';

      if (bmm.isFolder(bookmarkNode)) {
        this.className = 'folder';
      } else {
        labelEl.style.backgroundImage = getFaviconImageSet(bookmarkNode.url);
        labelEl.style.backgroundSize = '16px';
        urlEl.textContent = bookmarkNode.url;
      }

      this.appendChild(labelEl);
      this.appendChild(urlEl);

      // Initially the ContextMenuButton was added here but it slowed down
      // rendering a lot so it is now added using mouseover.
    },

    /**
     * The ID of the bookmark folder we are currently showing or loading.
     * @type {string}
     */
    get bookmarkId() {
      return this.bookmarkNode.id;
    },

    /**
     * Whether the user is currently able to edit the list item.
     * @type {boolean}
     */
    get editing() {
      return this.hasAttribute('editing');
    },
    set editing(editing) {
      var oldEditing = this.editing;
      if (oldEditing == editing)
        return;

      var url = this.bookmarkNode.url;
      var title = this.bookmarkNode.title;
      var isFolder = bmm.isFolder(this.bookmarkNode);
      var listItem = this;
      var labelEl = this.firstChild;
      var urlEl = labelEl.nextSibling;
      var labelInput, urlInput;

      // Handles enter and escape which trigger reset and commit respectively.
      function handleKeydown(e) {
        // Make sure that the tree does not handle the key.
        e.stopPropagation();

        // Calling list.focus blurs the input which will stop editing the list
        // item.
        switch (e.keyIdentifier) {
          case 'U+001B':  // Esc
            labelInput.value = title;
            if (!isFolder)
              urlInput.value = url;
            // fall through
            cr.dispatchSimpleEvent(listItem, 'canceledit', true);
          case 'Enter':
            if (listItem.parentNode)
              listItem.parentNode.focus();
            break;
          case 'U+0009':  // Tab
            // urlInput is the last focusable element in the page.  If we
            // allowed Tab focus navigation and the page loses focus, we
            // couldn't give focus on urlInput programatically. So, we prevent
            // Tab focus navigation.
            if (document.activeElement == urlInput && !e.ctrlKey &&
                !e.metaKey && !e.shiftKey && !getValidURL(urlInput)) {
              e.preventDefault();
              urlInput.blur();
            }
            break;
        }
      }

      function getValidURL(input) {
        var originalValue = input.value;
        if (!originalValue)
          return null;
        if (input.validity.valid)
          return originalValue;
        // Blink does not do URL fix up so we manually test if prepending
        // 'http://' would make the URL valid.
        // https://bugs.webkit.org/show_bug.cgi?id=29235
        input.value = 'http://' + originalValue;
        if (input.validity.valid)
          return input.value;
        // still invalid
        input.value = originalValue;
        return null;
      }

      function handleBlur(e) {
        // When the blur event happens we do not know who is getting focus so we
        // delay this a bit since we want to know if the other input got focus
        // before deciding if we should exit edit mode.
        var doc = e.target.ownerDocument;
        window.setTimeout(function() {
          var activeElement = doc.hasFocus() && doc.activeElement;
          if (activeElement != urlInput && activeElement != labelInput) {
            listItem.editing = false;
          }
        }, 50);
      }

      var doc = this.ownerDocument;
      if (editing) {
        this.setAttribute('editing', '');
        this.draggable = false;

        labelInput = /** @type {HTMLElement} */(doc.createElement('input'));
        labelInput.placeholder =
            loadTimeData.getString('name_input_placeholder');
        replaceAllChildren(labelEl, labelInput);
        labelInput.value = title;

        if (!isFolder) {
          urlInput = /** @type {HTMLElement} */(doc.createElement('input'));
          urlInput.type = 'url';
          urlInput.required = true;
          urlInput.placeholder =
              loadTimeData.getString('url_input_placeholder');

          // We also need a name for the input for the CSS to work.
          urlInput.name = '-url-input-' + cr.createUid();
          replaceAllChildren(assert(urlEl), urlInput);
          urlInput.value = url;
        }

        var stopPropagation = function(e) {
          e.stopPropagation();
        };

        var eventsToStop =
            ['mousedown', 'mouseup', 'contextmenu', 'dblclick', 'paste'];
        eventsToStop.forEach(function(type) {
          labelInput.addEventListener(type, stopPropagation);
        });
        labelInput.addEventListener('keydown', handleKeydown);
        labelInput.addEventListener('blur', handleBlur);
        cr.ui.limitInputWidth(labelInput, this, 100, 0.5);
        labelInput.focus();
        labelInput.select();

        if (!isFolder) {
          eventsToStop.forEach(function(type) {
            urlInput.addEventListener(type, stopPropagation);
          });
          urlInput.addEventListener('keydown', handleKeydown);
          urlInput.addEventListener('blur', handleBlur);
          cr.ui.limitInputWidth(urlInput, this, 200, 0.5);
        }

      } else {
        // Check that we have a valid URL and if not we do not change the
        // editing mode.
        if (!isFolder) {
          var urlInput = this.querySelector('.url input');
          var newUrl = urlInput.value;
          if (!newUrl) {
            cr.dispatchSimpleEvent(this, 'canceledit', true);
            return;
          }

          newUrl = getValidURL(urlInput);
          if (!newUrl) {
            // In case the item was removed before getting here we should
            // not alert.
            if (listItem.parentNode) {
              // Select the item again.
              var dataModel = this.parentNode.dataModel;
              var index = dataModel.indexOf(this.bookmarkNode);
              var sm = this.parentNode.selectionModel;
              sm.selectedIndex = sm.leadIndex = sm.anchorIndex = index;

              alert(loadTimeData.getString('invalid_url'));
            }
            urlInput.focus();
            urlInput.select();
            return;
          }
          urlEl.textContent = this.bookmarkNode.url = newUrl;
        }

        this.removeAttribute('editing');
        this.draggable = true;

        labelInput = this.querySelector('.label input');
        var newLabel = labelInput.value;
        labelEl.textContent = this.bookmarkNode.title = newLabel;

        if (isFolder) {
          if (newLabel != title) {
            cr.dispatchSimpleEvent(this, 'rename', true);
          }
        } else if (newLabel != title || newUrl != url) {
          cr.dispatchSimpleEvent(this, 'edit', true);
        }
      }
    }
  };

  return {
    BookmarkList: BookmarkList,
    list: /** @type {Element} */(null),  // Set when decorated.
  };
});
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


cr.define('bmm', function() {
  'use strict';

  /**
   * The id of the bookmark root.
   * @type {string}
   * @const
   */
  var ROOT_ID = '0';

  /** @const */ var Tree = cr.ui.Tree;
  /** @const */ var TreeItem = cr.ui.TreeItem;
  /** @const */ var localStorage = window.localStorage;

  var treeLookup = {};

  // Manager for persisting the expanded state.
  var expandedManager = /** @type {EventListener} */({
    /**
     * A map of the collapsed IDs.
     * @type {Object}
     */
    map: 'bookmarkTreeState' in localStorage ?
        /** @type {Object} */(JSON.parse(localStorage['bookmarkTreeState'])) :
        {},

    /**
     * Set the collapsed state for an ID.
     * @param {string} id The bookmark ID of the tree item that was expanded or
     *     collapsed.
     * @param {boolean} expanded Whether the tree item was expanded.
     */
    set: function(id, expanded) {
      if (expanded)
        delete this.map[id];
      else
        this.map[id] = 1;

      this.save();
    },

    /**
     * @param {string} id The bookmark ID.
     * @return {boolean} Whether the tree item should be expanded.
     */
    get: function(id) {
      return !(id in this.map);
    },

    /**
     * Callback for the expand and collapse events from the tree.
     * @param {!Event} e The collapse or expand event.
     */
    handleEvent: function(e) {
      this.set(e.target.bookmarkId, e.type == 'expand');
    },

    /**
     * Cleans up old bookmark IDs.
     */
    cleanUp: function() {
      for (var id in this.map) {
        // If the id is no longer in the treeLookup the bookmark no longer
        // exists.
        if (!(id in treeLookup))
          delete this.map[id];
      }
      this.save();
    },

    timer: null,

    /**
     * Saves the expanded state to the localStorage.
     */
    save: function() {
      clearTimeout(this.timer);
      var map = this.map;
      // Save in a timeout so that we can coalesce multiple changes.
      this.timer = setTimeout(function() {
        localStorage['bookmarkTreeState'] = JSON.stringify(map);
      }, 100);
    }
  });

  // Clean up once per session but wait until things settle down a bit.
  setTimeout(expandedManager.cleanUp.bind(expandedManager), 1e4);

  /**
   * Creates a new tree item for a bookmark node.
   * @param {!Object} bookmarkNode The bookmark node.
   * @constructor
   * @extends {TreeItem}
   */
  function BookmarkTreeItem(bookmarkNode) {
    var ti = new TreeItem({
      label: bookmarkNode.title,
      bookmarkNode: bookmarkNode,
      // Bookmark toolbar and Other bookmarks are not draggable.
      draggable: bookmarkNode.parentId != ROOT_ID
    });
    ti.__proto__ = BookmarkTreeItem.prototype;
    treeLookup[bookmarkNode.id] = ti;
    return ti;
  }

  BookmarkTreeItem.prototype = {
    __proto__: TreeItem.prototype,

    /**
     * The ID of the bookmark this tree item represents.
     * @type {string}
     */
    get bookmarkId() {
      return this.bookmarkNode.id;
    }
  };

  /**
   * Asynchronousy adds a tree item at the correct index based on the bookmark
   * backend.
   *
   * Since the bookmark tree only contains folders the index we get from certain
   * callbacks is not very useful so we therefore have this async call which
   * gets the children of the parent and adds the tree item at the desired
   * index.
   *
   * This also exoands the parent so that newly added children are revealed.
   *
   * @param {!cr.ui.TreeItem} parent The parent tree item.
   * @param {!cr.ui.TreeItem} treeItem The tree item to add.
   * @param {Function=} opt_f A function which gets called after the item has
   *     been added at the right index.
   */
  function addTreeItem(parent, treeItem, opt_f) {
    chrome.bookmarks.getChildren(parent.bookmarkNode.id, function(children) {
      var isFolder = /**
                      * @type {function (BookmarkTreeNode, number,
                      *                  Array<(BookmarkTreeNode)>)}
                      */(bmm.isFolder);
      var index = children.filter(isFolder).map(function(item) {
        return item.id;
      }).indexOf(treeItem.bookmarkNode.id);
      parent.addAt(treeItem, index);
      parent.expanded = true;
      if (opt_f)
        opt_f();
    });
  }


  /**
   * Creates a new bookmark list.
   * @param {Object=} opt_propertyBag Optional properties.
   * @constructor
   * @extends {cr.ui.Tree}
   */
  var BookmarkTree = cr.ui.define('tree');

  BookmarkTree.prototype = {
    __proto__: Tree.prototype,

    decorate: function() {
      Tree.prototype.decorate.call(this);
      this.addEventListener('expand', expandedManager);
      this.addEventListener('collapse', expandedManager);

      bmm.tree = this;
    },

    handleBookmarkChanged: function(id, changeInfo) {
      var treeItem = treeLookup[id];
      if (treeItem)
        treeItem.label = treeItem.bookmarkNode.title = changeInfo.title;
    },

    /**
     * @param {string} id
     * @param {ReorderInfo} reorderInfo
     */
    handleChildrenReordered: function(id, reorderInfo) {
      var parentItem = treeLookup[id];
      // The tree only contains folders.
      var dirIds = reorderInfo.childIds.filter(function(id) {
        return id in treeLookup;
      }).forEach(function(id, i) {
        parentItem.addAt(treeLookup[id], i);
      });
    },

    handleCreated: function(id, bookmarkNode) {
      if (bmm.isFolder(bookmarkNode)) {
        var parentItem = treeLookup[bookmarkNode.parentId];
        var newItem = new BookmarkTreeItem(bookmarkNode);
        addTreeItem(parentItem, newItem);
      }
    },

    /**
     * @param {string} id
     * @param {MoveInfo} moveInfo
     */
    handleMoved: function(id, moveInfo) {
      var treeItem = treeLookup[id];
      if (treeItem) {
        var oldParentItem = treeLookup[moveInfo.oldParentId];
        oldParentItem.remove(treeItem);
        var newParentItem = treeLookup[moveInfo.parentId];
        // The tree only shows folders so the index is not the index we want. We
        // therefore get the children need to adjust the index.
        addTreeItem(newParentItem, treeItem);
      }
    },

    handleRemoved: function(id, removeInfo) {
      var parentItem = treeLookup[removeInfo.parentId];
      var itemToRemove = treeLookup[id];
      if (parentItem && itemToRemove)
        parentItem.remove(itemToRemove);
    },

    insertSubtree: function(folder) {
      if (!bmm.isFolder(folder))
        return;
      var children = folder.children;
      this.handleCreated(folder.id, folder);
      for (var i = 0; i < children.length; i++) {
        var child = children[i];
        this.insertSubtree(child);
      }
    },

    /**
     * Returns the bookmark node with the given ID. The tree only maintains
     * folder nodes.
     * @param {string} id The ID of the node to find.
     * @return {BookmarkTreeNode} The bookmark tree node or null if not found.
     */
    getBookmarkNodeById: function(id) {
      var treeItem = treeLookup[id];
      if (treeItem)
        return treeItem.bookmarkNode;
      return null;
    },

    /**
      * Returns the selected bookmark folder node as an array.
      * @type {!Array} Array of bookmark nodes.
      */
    get selectedFolders() {
       return this.selectedItem && this.selectedItem.bookmarkNode ?
           [this.selectedItem.bookmarkNode] : [];
     },

     /**
     * Fetches the bookmark items and builds the tree control.
     */
    reload: function() {
      /**
       * Recursive helper function that adds all the directories to the
       * parentTreeItem.
       * @param {!cr.ui.Tree|!cr.ui.TreeItem} parentTreeItem The parent tree
       *     element to append to.
       * @param {!Array<BookmarkTreeNode>} bookmarkNodes A list of bookmark
       *     nodes to be added.
       * @return {boolean} Whether any directories where added.
       */
      function buildTreeItems(parentTreeItem, bookmarkNodes) {
        var hasDirectories = false;
        for (var i = 0, bookmarkNode; bookmarkNode = bookmarkNodes[i]; i++) {
          if (bmm.isFolder(bookmarkNode)) {
            hasDirectories = true;
            var item = new BookmarkTreeItem(bookmarkNode);
            parentTreeItem.add(item);
            var children = assert(bookmarkNode.children);
            var anyChildren = buildTreeItems(item, children);
            item.expanded = anyChildren && expandedManager.get(bookmarkNode.id);
          }
        }
        return hasDirectories;
      }

      var self = this;
      chrome.bookmarkManagerPrivate.getSubtree('', true, function(root) {
        self.clear();
        buildTreeItems(self, root[0].children);
        cr.dispatchSimpleEvent(self, 'load');
      });
    },

    /**
     * Clears the tree.
     */
    clear: function() {
      // Remove all fields without recreating the object since other code
      // references it.
      for (var id in treeLookup) {
        delete treeLookup[id];
      }
      this.textContent = '';
    },

    /** @override */
    remove: function(child) {
      Tree.prototype.remove.call(this, child);
      if (child.bookmarkNode)
        delete treeLookup[child.bookmarkNode.id];
    }
  };

  return {
    BookmarkTree: BookmarkTree,
    BookmarkTreeItem: BookmarkTreeItem,
    treeLookup: treeLookup,
    tree: /** @type {Element} */(null),  // Set when decorated.
    ROOT_ID: ROOT_ID
  };
});
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('dnd', function() {
  'use strict';

  /** @const */ var BookmarkList = bmm.BookmarkList;
  /** @const */ var ListItem = cr.ui.ListItem;
  /** @const */ var TreeItem = cr.ui.TreeItem;

  /**
   * Enumeration of valid drop locations relative to an element. These are
   * bit masks to allow combining multiple locations in a single value.
   * @enum {number}
   * @const
   */
  var DropPosition = {
    NONE: 0,
    ABOVE: 1,
    ON: 2,
    BELOW: 4
  };

  /**
   * @type {Object} Drop information calculated in |handleDragOver|.
   */
  var dropDestination = null;

  /**
    * @type {number} Timer id used to help minimize flicker.
    */
  var removeDropIndicatorTimer;

  /**
    * The element currently targeted by a touch.
    * @type {Element}
    */
  var currentTouchTarget;

  /**
    * The element that had a style applied it to indicate the drop location.
    * This is used to easily remove the style when necessary.
    * @type {Element}
    */
  var lastIndicatorElement;

  /**
    * The style that was applied to indicate the drop location.
    * @type {?string}
    */
  var lastIndicatorClassName;

  var dropIndicator = {
    /**
     * Applies the drop indicator style on the target element and stores that
     * information to easily remove the style in the future.
     */
    addDropIndicatorStyle: function(indicatorElement, position) {
      var indicatorStyleName = position == DropPosition.ABOVE ? 'drag-above' :
                               position == DropPosition.BELOW ? 'drag-below' :
                               'drag-on';

      lastIndicatorElement = indicatorElement;
      lastIndicatorClassName = indicatorStyleName;

      indicatorElement.classList.add(indicatorStyleName);
    },

    /**
     * Clears the drop indicator style from the last element was the drop target
     * so the drop indicator is no longer for that element.
     */
    removeDropIndicatorStyle: function() {
      if (!lastIndicatorElement || !lastIndicatorClassName)
        return;
      lastIndicatorElement.classList.remove(lastIndicatorClassName);
      lastIndicatorElement = null;
      lastIndicatorClassName = null;
    },

    /**
      * Displays the drop indicator on the current drop target to give the
      * user feedback on where the drop will occur.
      */
    update: function(dropDest) {
      window.clearTimeout(removeDropIndicatorTimer);

      var indicatorElement = dropDest.element;
      var position = dropDest.position;
      if (dropDest.element instanceof BookmarkList) {
        // For an empty bookmark list use 'drop-above' style.
        position = DropPosition.ABOVE;
      } else if (dropDest.element instanceof TreeItem) {
        indicatorElement = indicatorElement.querySelector('.tree-row');
      }
      dropIndicator.removeDropIndicatorStyle();
      dropIndicator.addDropIndicatorStyle(indicatorElement, position);
    },

    /**
     * Stop displaying the drop indicator.
     */
    finish: function() {
      // The use of a timeout is in order to reduce flickering as we move
      // between valid drop targets.
      window.clearTimeout(removeDropIndicatorTimer);
      removeDropIndicatorTimer = window.setTimeout(function() {
        dropIndicator.removeDropIndicatorStyle();
      }, 100);
    }
  };

  /**
    * Delay for expanding folder when pointer hovers on folder in tree view in
    * milliseconds.
    * @type {number}
    * @const
    */
  // TODO(yosin): EXPAND_FOLDER_DELAY should follow system settings. 400ms is
  // taken from Windows default settings.
  var EXPAND_FOLDER_DELAY = 400;

  /**
    * The timestamp when the mouse was over a folder during a drag operation.
    * Used to open the hovered folder after a certain time.
    * @type {number}
    */
  var lastHoverOnFolderTimeStamp = 0;

  /**
    * Expand a folder if the user has hovered for longer than the specified
    * time during a drag action.
    */
  function updateAutoExpander(eventTimeStamp, overElement) {
    // Expands a folder in tree view when pointer hovers on it longer than
    // EXPAND_FOLDER_DELAY.
    var hoverOnFolderTimeStamp = lastHoverOnFolderTimeStamp;
    lastHoverOnFolderTimeStamp = 0;
    if (hoverOnFolderTimeStamp) {
      if (eventTimeStamp - hoverOnFolderTimeStamp >= EXPAND_FOLDER_DELAY)
        overElement.expanded = true;
      else
        lastHoverOnFolderTimeStamp = hoverOnFolderTimeStamp;
    } else if (overElement instanceof TreeItem &&
                bmm.isFolder(overElement.bookmarkNode) &&
                overElement.hasChildren &&
                !overElement.expanded) {
      lastHoverOnFolderTimeStamp = eventTimeStamp;
    }
  }

  /**
    * Stores the information about the bookmark and folders being dragged.
    * @type {Object}
    */
  var dragData = null;
  var dragInfo = {
    handleChromeDragEnter: function(newDragData) {
      dragData = newDragData;
    },
    clearDragData: function() {
      dragData = null;
    },
    isDragValid: function() {
      return !!dragData;
    },
    isSameProfile: function() {
      return dragData && dragData.sameProfile;
    },
    isDraggingFolders: function() {
      return dragData && dragData.elements.some(function(node) {
        return !node.url;
      });
    },
    isDraggingBookmark: function(bookmarkId) {
      return dragData && dragData.elements.some(function(node) {
        return node.id == bookmarkId;
      });
    },
    isDraggingChildBookmark: function(folderId) {
      return dragData && dragData.elements.some(function(node) {
        return node.parentId == folderId;
      });
    },
    isDraggingFolderToDescendant: function(bookmarkNode) {
      return dragData && dragData.elements.some(function(node) {
        var dragFolder = bmm.treeLookup[node.id];
        var dragFolderNode = dragFolder && dragFolder.bookmarkNode;
        return dragFolderNode && bmm.contains(dragFolderNode, bookmarkNode);
      });
    }
  };

  /**
   * External function to select folders or bookmarks after a drop action.
   * @type {?Function}
   */
  var selectItemsAfterUserAction = null;

  function getBookmarkElement(el) {
    while (el && !el.bookmarkNode) {
      el = el.parentNode;
    }
    return el;
  }

  // If we are over the list and the list is showing search result, we cannot
  // drop.
  function isOverSearch(overElement) {
    return bmm.list.isSearch() && bmm.list.contains(overElement);
  }

  /**
   * Determines the valid drop positions for the given target element.
   * @param {!HTMLElement} overElement The element that we are currently
   *     dragging over.
   * @return {DropPosition} An bit field enumeration of valid drop locations.
   */
  function calculateValidDropTargets(overElement) {
    // Don't allow dropping if there is an ephemeral item being edited.
    if (bmm.list.hasEphemeral())
      return DropPosition.NONE;

    if (!dragInfo.isDragValid() || isOverSearch(overElement))
      return DropPosition.NONE;

    if (dragInfo.isSameProfile() &&
        (dragInfo.isDraggingBookmark(overElement.bookmarkNode.id) ||
         dragInfo.isDraggingFolderToDescendant(overElement.bookmarkNode))) {
      return DropPosition.NONE;
    }

    var canDropInfo = calculateDropAboveBelow(overElement);
    if (canDropOn(overElement))
      canDropInfo |= DropPosition.ON;

    return canDropInfo;
  }

  function calculateDropAboveBelow(overElement) {
    if (overElement instanceof BookmarkList)
      return DropPosition.NONE;

    // We cannot drop between Bookmarks bar and Other bookmarks.
    if (overElement.bookmarkNode.parentId == bmm.ROOT_ID)
      return DropPosition.NONE;

    var isOverTreeItem = overElement instanceof TreeItem;
    var isOverExpandedTree = isOverTreeItem && overElement.expanded;
    var isDraggingFolders = dragInfo.isDraggingFolders();

    // We can only drop between items in the tree if we have any folders.
    if (isOverTreeItem && !isDraggingFolders)
      return DropPosition.NONE;

    // When dragging from a different profile we do not need to consider
    // conflicts between the dragged items and the drop target.
    if (!dragInfo.isSameProfile()) {
      // Don't allow dropping below an expanded tree item since it is confusing
      // to the user anyway.
      return isOverExpandedTree ? DropPosition.ABOVE :
                                  (DropPosition.ABOVE | DropPosition.BELOW);
    }

    var resultPositions = DropPosition.NONE;

    // Cannot drop above if the item above is already in the drag source.
    var previousElem = overElement.previousElementSibling;
    if (!previousElem || !dragInfo.isDraggingBookmark(previousElem.bookmarkId))
      resultPositions |= DropPosition.ABOVE;

    // Don't allow dropping below an expanded tree item since it is confusing
    // to the user anyway.
    if (isOverExpandedTree)
      return resultPositions;

    // Cannot drop below if the item below is already in the drag source.
    var nextElement = overElement.nextElementSibling;
    if (!nextElement || !dragInfo.isDraggingBookmark(nextElement.bookmarkId))
      resultPositions |= DropPosition.BELOW;

    return resultPositions;
  }

  /**
   * Determine whether we can drop the dragged items on the drop target.
   * @param {!HTMLElement} overElement The element that we are currently
   *     dragging over.
   * @return {boolean} Whether we can drop the dragged items on the drop
   *     target.
   */
  function canDropOn(overElement) {
    // We can only drop on a folder.
    if (!bmm.isFolder(overElement.bookmarkNode))
      return false;

    if (!dragInfo.isSameProfile())
      return true;

    if (overElement instanceof BookmarkList) {
      // We are trying to drop an item past the last item. This is
      // only allowed if dragged item is different from the last item
      // in the list.
      var listItems = bmm.list.items;
      var len = listItems.length;
      if (!len || !dragInfo.isDraggingBookmark(listItems[len - 1].bookmarkId))
        return true;
    }

    return !dragInfo.isDraggingChildBookmark(overElement.bookmarkNode.id);
  }

  /**
   * Callback for the dragstart event.
   * @param {Event} e The dragstart event.
   */
  function handleDragStart(e) {
    // Determine the selected bookmarks.
    var target = e.target;
    var draggedNodes = [];
    var isFromTouch = target == currentTouchTarget;

    if (target instanceof ListItem) {
      // Use selected items.
      draggedNodes = target.parentNode.selectedItems;
    } else if (target instanceof TreeItem) {
      draggedNodes.push(target.bookmarkNode);
    }

    // We manage starting the drag by using the extension API.
    e.preventDefault();

    // Do not allow dragging if there is an ephemeral item being edited at the
    // moment.
    if (bmm.list.hasEphemeral())
      return;

    if (draggedNodes.length) {
      // If we are dragging a single link, we can do the *Link* effect.
      // Otherwise, we only allow copy and move.
      e.dataTransfer.effectAllowed = draggedNodes.length == 1 &&
          !bmm.isFolder(draggedNodes[0]) ? 'copyMoveLink' : 'copyMove';

      chrome.bookmarkManagerPrivate.startDrag(draggedNodes.map(function(node) {
        return node.id;
      }), isFromTouch);
    }
  }

  function handleDragEnter(e) {
    e.preventDefault();
  }

  /**
   * Calback for the dragover event.
   * @param {Event} e The dragover event.
   */
  function handleDragOver(e) {
    // Allow DND on text inputs.
    if (e.target.tagName != 'INPUT') {
      // The default operation is to allow dropping links etc to do navigation.
      // We never want to do that for the bookmark manager.
      e.preventDefault();

      // Set to none. This will get set to something if we can do the drop.
      e.dataTransfer.dropEffect = 'none';
    }

    if (!dragInfo.isDragValid())
      return;

    var overElement = getBookmarkElement(e.target) ||
                      (e.target == bmm.list ? bmm.list : null);
    if (!overElement)
      return;

    updateAutoExpander(e.timeStamp, overElement);

    var canDropInfo = calculateValidDropTargets(overElement);
    if (canDropInfo == DropPosition.NONE)
      return;

    // Now we know that we can drop. Determine if we will drop above, on or
    // below based on mouse position etc.

    dropDestination = calcDropPosition(e.clientY, overElement, canDropInfo);
    if (!dropDestination) {
      e.dataTransfer.dropEffect = 'none';
      return;
    }

    e.dataTransfer.dropEffect = dragInfo.isSameProfile() ? 'move' : 'copy';
    dropIndicator.update(dropDestination);
  }

  /**
   * This function determines where the drop will occur relative to the element.
   * @return {?Object} If no valid drop position is found, null, otherwise
   *     an object containing the following parameters:
   *       element - The target element that will receive the drop.
   *       position - A |DropPosition| relative to the |element|.
   */
  function calcDropPosition(elementClientY, overElement, canDropInfo) {
    if (overElement instanceof BookmarkList) {
      // Dropping on the BookmarkList either means dropping below the last
      // bookmark element or on the list itself if it is empty.
      var length = overElement.items.length;
      if (length)
        return {
          element: overElement.getListItemByIndex(length - 1),
          position: DropPosition.BELOW
        };
      return {element: overElement, position: DropPosition.ON};
    }

    var above = canDropInfo & DropPosition.ABOVE;
    var below = canDropInfo & DropPosition.BELOW;
    var on = canDropInfo & DropPosition.ON;
    var rect = overElement.getBoundingClientRect();
    var yRatio = (elementClientY - rect.top) / rect.height;

    if (above && (yRatio <= .25 || yRatio <= .5 && (!below || !on)))
      return {element: overElement, position: DropPosition.ABOVE};
    if (below && (yRatio > .75 || yRatio > .5 && (!above || !on)))
      return {element: overElement, position: DropPosition.BELOW};
    if (on)
      return {element: overElement, position: DropPosition.ON};
    return null;
  }

  function calculateDropInfo(eventTarget, dropDestination) {
    if (!dropDestination || !dragInfo.isDragValid())
      return null;

    var dropPos = dropDestination.position;
    var relatedNode = dropDestination.element.bookmarkNode;
    var dropInfoResult = {
        selectTarget: null,
        selectedTreeId: -1,
        parentId: dropPos == DropPosition.ON ? relatedNode.id :
                                               relatedNode.parentId,
        index: -1,
        relatedIndex: -1
      };

    // Try to find the index in the dataModel so we don't have to always keep
    // the index for the list items up to date.
    var overElement = getBookmarkElement(eventTarget);
    if (overElement instanceof ListItem) {
      dropInfoResult.relatedIndex =
          overElement.parentNode.dataModel.indexOf(relatedNode);
      dropInfoResult.selectTarget = bmm.list;
    } else if (overElement instanceof BookmarkList) {
      dropInfoResult.relatedIndex = overElement.dataModel.length - 1;
      dropInfoResult.selectTarget = bmm.list;
    } else {
      // Tree
      dropInfoResult.relatedIndex = relatedNode.index;
      dropInfoResult.selectTarget = bmm.tree;
      dropInfoResult.selectedTreeId =
          bmm.tree.selectedItem ? bmm.tree.selectedItem.bookmarkId : null;
    }

    if (dropPos == DropPosition.ABOVE)
      dropInfoResult.index = dropInfoResult.relatedIndex;
    else if (dropPos == DropPosition.BELOW)
      dropInfoResult.index = dropInfoResult.relatedIndex + 1;

    return dropInfoResult;
  }

  function handleDragLeave(e) {
    dropIndicator.finish();
  }

  function handleDrop(e) {
    var dropInfo = calculateDropInfo(e.target, dropDestination);
    if (dropInfo) {
      selectItemsAfterUserAction(dropInfo.selectTarget,
                                 dropInfo.selectedTreeId);
      if (dropInfo.index != -1)
        chrome.bookmarkManagerPrivate.drop(dropInfo.parentId, dropInfo.index);
      else
        chrome.bookmarkManagerPrivate.drop(dropInfo.parentId);

      e.preventDefault();
    }
    dropDestination = null;
    dropIndicator.finish();
  }

  function setCurrentTouchTarget(e) {
    // Only set a new target for a single touch point.
    if (e.touches.length == 1)
      currentTouchTarget = getBookmarkElement(e.target);
  }

  function clearCurrentTouchTarget(e) {
    if (getBookmarkElement(e.target) == currentTouchTarget)
      currentTouchTarget = null;
  }

  function clearDragData() {
    dragInfo.clearDragData();
    dropDestination = null;
  }

  function init(selectItemsAfterUserActionFunction) {
    function deferredClearData() {
      setTimeout(clearDragData, 0);
    }

    selectItemsAfterUserAction = selectItemsAfterUserActionFunction;

    document.addEventListener('dragstart', handleDragStart);
    document.addEventListener('dragenter', handleDragEnter);
    document.addEventListener('dragover', handleDragOver);
    document.addEventListener('dragleave', handleDragLeave);
    document.addEventListener('drop', handleDrop);
    document.addEventListener('dragend', deferredClearData);
    document.addEventListener('mouseup', deferredClearData);
    document.addEventListener('mousedown', clearCurrentTouchTarget);
    document.addEventListener('touchcancel', clearCurrentTouchTarget);
    document.addEventListener('touchend', clearCurrentTouchTarget);
    document.addEventListener('touchstart', setCurrentTouchTarget);

    chrome.bookmarkManagerPrivate.onDragEnter.addListener(
        dragInfo.handleChromeDragEnter);
    chrome.bookmarkManagerPrivate.onDragLeave.addListener(deferredClearData);
    chrome.bookmarkManagerPrivate.onDrop.addListener(deferredClearData);
  }
  return {init: init};
});
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('bmm', function() {
  'use strict';

  /**
   * Whether a node contains another node.
   * TODO(yosin): Once JavaScript style guide is updated and linter follows
   * that, we'll remove useless documentations for |parent| and |descendant|.
   * TODO(yosin): bmm.contains() should be method of BookmarkTreeNode.
   * @param {!BookmarkTreeNode} parent .
   * @param {!BookmarkTreeNode} descendant .
   * @return {boolean} Whether the parent contains the descendant.
   */
  function contains(parent, descendant) {
    if (descendant.parentId == parent.id)
      return true;
    // the bmm.treeLookup contains all folders
    var parentTreeItem = bmm.treeLookup[descendant.parentId];
    if (!parentTreeItem || !parentTreeItem.bookmarkNode)
      return false;
    return this.contains(parent, parentTreeItem.bookmarkNode);
  }

  /**
   * @param {!BookmarkTreeNode} node The node to test.
   * @return {boolean} Whether a bookmark node is a folder.
   */
  function isFolder(node) {
    return !('url' in node);
  }

  var loadingPromises = {};

  /**
   * Promise version of chrome.bookmarkManagerPrivate.getSubtree.
   * @param {string} id .
   * @param {boolean} foldersOnly .
   * @return {!Promise<!Array<!BookmarkTreeNode>>} .
   */
  function getSubtreePromise(id, foldersOnly) {
    return new Promise(function(resolve, reject) {
      chrome.bookmarkManagerPrivate.getSubtree(id, foldersOnly, function(node) {
        if (chrome.runtime.lastError) {
          reject(new Error(chrome.runtime.lastError.message));
          return;
        }
        resolve(node);
      });
    });
  }

  /**
   * Loads a subtree of the bookmark tree and returns a {@code Promise} that
   * will be fulfilled when done. This reuses multiple loads so that we do not
   * load the same subtree more than once at the same time.
   * @return {!Promise<!BookmarkTreeNode>} The future promise for the load.
   */
  function loadSubtree(id) {
    if (!loadingPromises[id]) {
      loadingPromises[id] = getSubtreePromise(id, false).then(function(nodes) {
        return nodes && nodes[0];
      }, function(error) {
        console.error(error.message);
      });
      loadingPromises[id].then(function() {
        delete loadingPromises[id];
      });
    }
    return loadingPromises[id];
  }

  /**
   * Loads the entire bookmark tree and returns a {@code Promise} that will
   * be fulfilled when done. This reuses multiple loads so that we do not load
   * the same tree more than once at the same time.
   * @return {!Promise<!BookmarkTreeNode>} The future promise for the load.
   */
  function loadTree() {
    return loadSubtree('');
  }

  var bookmarkCache = {
    /**
     * Removes the cached item from both the list and tree lookups.
     */
    remove: function(id) {
      var treeItem = bmm.treeLookup[id];
      if (treeItem) {
        var items = treeItem.items; // is an HTMLCollection
        for (var i = 0; i < items.length; ++i) {
          var item = items[i];
          var bookmarkNode = item.bookmarkNode;
          delete bmm.treeLookup[bookmarkNode.id];
        }
        delete bmm.treeLookup[id];
      }
    },

    /**
     * Updates the underlying bookmark node for the tree items and list items by
     * querying the bookmark backend.
     * @param {string} id The id of the node to update the children for.
     * @param {Function=} opt_f A funciton to call when done.
     */
    updateChildren: function(id, opt_f) {
      function updateItem(bookmarkNode) {
        var treeItem = bmm.treeLookup[bookmarkNode.id];
        if (treeItem) {
          treeItem.bookmarkNode = bookmarkNode;
        }
      }

      chrome.bookmarks.getChildren(id, function(children) {
        if (children)
          children.forEach(updateItem);

        if (opt_f)
          opt_f(children);
      });
    }
  };

  /**
   * Called when the title of a bookmark changes.
   * @param {string} id The id of changed bookmark node.
   * @param {!Object} changeInfo The information about how the node changed.
   */
  function handleBookmarkChanged(id, changeInfo) {
    if (bmm.tree)
      bmm.tree.handleBookmarkChanged(id, changeInfo);
    if (bmm.list)
      bmm.list.handleBookmarkChanged(id, changeInfo);
  }

  /**
   * Callback for when the user reorders by title.
   * @param {string} id The id of the bookmark folder that was reordered.
   * @param {!Object} reorderInfo The information about how the items where
   *     reordered.
   */
  function handleChildrenReordered(id, reorderInfo) {
    if (bmm.tree)
      bmm.tree.handleChildrenReordered(id, reorderInfo);
    if (bmm.list)
      bmm.list.handleChildrenReordered(id, reorderInfo);
    bookmarkCache.updateChildren(id);
  }

  /**
   * Callback for when a bookmark node is created.
   * @param {string} id The id of the newly created bookmark node.
   * @param {!Object} bookmarkNode The new bookmark node.
   */
  function handleCreated(id, bookmarkNode) {
    if (bmm.list)
      bmm.list.handleCreated(id, bookmarkNode);
    if (bmm.tree)
      bmm.tree.handleCreated(id, bookmarkNode);
    bookmarkCache.updateChildren(bookmarkNode.parentId);
  }

  /**
   * Callback for when a bookmark node is moved.
   * @param {string} id The id of the moved bookmark node.
   * @param {!Object} moveInfo The information about move.
   */
  function handleMoved(id, moveInfo) {
    if (bmm.list)
      bmm.list.handleMoved(id, moveInfo);
    if (bmm.tree)
      bmm.tree.handleMoved(id, moveInfo);

    bookmarkCache.updateChildren(moveInfo.parentId);
    if (moveInfo.parentId != moveInfo.oldParentId)
      bookmarkCache.updateChildren(moveInfo.oldParentId);
  }

  /**
   * Callback for when a bookmark node is removed.
   * @param {string} id The id of the removed bookmark node.
   * @param {!Object} removeInfo The information about removed.
   */
  function handleRemoved(id, removeInfo) {
    if (bmm.list)
      bmm.list.handleRemoved(id, removeInfo);
    if (bmm.tree)
      bmm.tree.handleRemoved(id, removeInfo);

    bookmarkCache.updateChildren(removeInfo.parentId);
    bookmarkCache.remove(id);
  }

  /**
   * Callback for when all bookmark nodes have been deleted.
   */
  function handleRemoveAll() {
    // Reload the list and the tree.
    if (bmm.list)
      bmm.list.reload();
    if (bmm.tree)
      bmm.tree.reload();
  }

  /**
   * Callback for when importing bookmark is started.
   */
  function handleImportBegan() {
    chrome.bookmarks.onCreated.removeListener(handleCreated);
    chrome.bookmarks.onChanged.removeListener(handleBookmarkChanged);
  }

  /**
   * Callback for when importing bookmark node is finished.
   */
  function handleImportEnded() {
    // When importing is done we reload the tree and the list.

    function f() {
      bmm.tree.removeEventListener('load', f);

      chrome.bookmarks.onCreated.addListener(handleCreated);
      chrome.bookmarks.onChanged.addListener(handleBookmarkChanged);

      if (!bmm.list)
        return;

      // TODO(estade): this should navigate to the newly imported folder, which
      // may be the bookmark bar if there were no previous bookmarks.
      bmm.list.reload();
    }

    if (bmm.tree) {
      bmm.tree.addEventListener('load', f);
      bmm.tree.reload();
    }
  }

  /**
   * Adds the listeners for the bookmark model change events.
   */
  function addBookmarkModelListeners() {
    chrome.bookmarks.onChanged.addListener(handleBookmarkChanged);
    chrome.bookmarks.onChildrenReordered.addListener(handleChildrenReordered);
    chrome.bookmarks.onCreated.addListener(handleCreated);
    chrome.bookmarks.onMoved.addListener(handleMoved);
    chrome.bookmarks.onRemoved.addListener(handleRemoved);
    chrome.bookmarks.onImportBegan.addListener(handleImportBegan);
    chrome.bookmarks.onImportEnded.addListener(handleImportEnded);
  };

  return {
    contains: contains,
    isFolder: isFolder,
    loadSubtree: loadSubtree,
    loadTree: loadTree,
    addBookmarkModelListeners: addBookmarkModelListeners
  };
});
<!doctype html>
<html>
<head>
  <link rel="stylesheet" href="main.css">
  <meta charset="utf-8">
  <script src="channel.js"></script>
  <script src="util.js"></script>
  <script src="main.js"></script>
</head>
<body>
  <iframe id="gaia-frame" name="gaia-frame" src="about:blank" frameborder="0"
          sandbox="allow-same-origin allow-scripts allow-popups allow-forms
          allow-pointer-lock"></iframe>
</body>
</html>
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Authenticator class wraps the communications between Gaia and its host.
 */
function Authenticator() {
}

/**
 * Gaia auth extension url origin.
 * @type {string}
 */
Authenticator.THIS_EXTENSION_ORIGIN =
    'chrome-extension://mfffpogegjflfpflabcdkioaeobkgjik';

/**
 * The lowest version of the credentials passing API supported.
 * @type {number}
 */
Authenticator.MIN_API_VERSION_VERSION = 1;

/**
 * The highest version of the credentials passing API supported.
 * @type {number}
 */
Authenticator.MAX_API_VERSION_VERSION = 1;

/**
 * The key types supported by the credentials passing API.
 * @type {Array} Array of strings.
 */
Authenticator.API_KEY_TYPES = [
  'KEY_TYPE_PASSWORD_PLAIN',
];

/**
 * Allowed origins of the hosting page.
 * @type {Array<string>}
 */
Authenticator.ALLOWED_PARENT_ORIGINS = [
  'chrome://oobe',
  'chrome://chrome-signin'
];

/**
 * Singleton getter of Authenticator.
 * @return {Object} The singleton instance of Authenticator.
 */
Authenticator.getInstance = function() {
  if (!Authenticator.instance_) {
    Authenticator.instance_ = new Authenticator();
  }
  return Authenticator.instance_;
};

Authenticator.prototype = {
  email_: null,
  gaiaId_: null,

  // Depending on the key type chosen, this will contain the plain text password
  // or a credential derived from it along with the information required to
  // repeat the derivation, such as a salt. The information will be encoded so
  // that it contains printable ASCII characters only. The exact encoding is TBD
  // when support for key types other than plain text password is added.
  passwordBytes_: null,

  needPassword_: false,
  chooseWhatToSync_: false,
  skipForNow_: false,
  sessionIndex_: null,
  attemptToken_: null,

  // Input params from extension initialization URL.
  inputLang_: undefined,
  intputEmail_: undefined,

  isSAMLFlow_: false,
  gaiaLoaded_: false,
  supportChannel_: null,

  useEafe_: false,
  clientId_: '',

  GAIA_URL: 'https://accounts.google.com/',
  GAIA_PAGE_PATH: 'ServiceLogin?skipvpage=true&sarp=1&rm=hide',
  SERVICE_ID: 'chromeoslogin',
  CONTINUE_URL: Authenticator.THIS_EXTENSION_ORIGIN + '/success.html',
  CONSTRAINED_FLOW_SOURCE: 'chrome',

  initialize: function() {
    var handleInitializeMessage = function(e) {
      if (Authenticator.ALLOWED_PARENT_ORIGINS.indexOf(e.origin) == -1) {
        console.error('Unexpected parent message, origin=' + e.origin);
        return;
      }
      window.removeEventListener('message', handleInitializeMessage);

      var params = e.data;
      params.parentPage = e.origin;
      this.initializeFromParent_(params);
      this.onPageLoad_();
    }.bind(this);

    document.addEventListener('DOMContentLoaded', function() {
      window.addEventListener('message', handleInitializeMessage);
      window.parent.postMessage({'method': 'loginUIDOMContentLoaded'}, '*');
    });
  },

  initializeFromParent_: function(params) {
    this.parentPage_ = params.parentPage;
    this.gaiaUrl_ = params.gaiaUrl || this.GAIA_URL;
    this.gaiaPath_ = params.gaiaPath || this.GAIA_PAGE_PATH;
    this.inputLang_ = params.hl;
    this.inputEmail_ = params.email;
    this.service_ = params.service || this.SERVICE_ID;
    this.continueUrl_ = params.continueUrl || this.CONTINUE_URL;
    this.desktopMode_ = params.desktopMode == '1';
    this.isConstrainedWindow_ = params.constrained == '1';
    this.useEafe_ = params.useEafe || false;
    this.clientId_ = params.clientId || '';
    this.initialFrameUrl_ = params.frameUrl || this.constructInitialFrameUrl_();
    this.initialFrameUrlWithoutParams_ = stripParams(this.initialFrameUrl_);
    this.needPassword_ = params.needPassword == '1';

    // For CrOS 'ServiceLogin' we assume that Gaia is loaded if we recieved
    // 'clearOldAttempts' message. For other scenarios Gaia doesn't send this
    // message so we have to rely on 'load' event.
    // TODO(dzhioev): Do not rely on 'load' event after b/16313327 is fixed.
    this.assumeLoadedOnLoadEvent_ =
        this.gaiaPath_.indexOf('ServiceLogin') !== 0 ||
        this.service_ !== 'chromeoslogin' ||
        this.useEafe_;
  },

  isGaiaMessage_: function(msg) {
    // Not quite right, but good enough.
    return this.gaiaUrl_.indexOf(msg.origin) == 0 ||
           this.GAIA_URL.indexOf(msg.origin) == 0;
  },

  isParentMessage_: function(msg) {
    return msg.origin == this.parentPage_;
  },

  constructInitialFrameUrl_: function() {
    var url = this.gaiaUrl_ + this.gaiaPath_;

    url = appendParam(url, 'service', this.service_);
    // Easy bootstrap use auth_code message as success signal instead of
    // continue URL.
    if (!this.useEafe_)
      url = appendParam(url, 'continue', this.continueUrl_);
    if (this.inputLang_)
      url = appendParam(url, 'hl', this.inputLang_);
    if (this.inputEmail_)
      url = appendParam(url, 'Email', this.inputEmail_);
    if (this.isConstrainedWindow_)
      url = appendParam(url, 'source', this.CONSTRAINED_FLOW_SOURCE);
    return url;
  },

  onPageLoad_: function() {
    window.addEventListener('message', this.onMessage.bind(this), false);
    this.initSupportChannel_();

    if (this.assumeLoadedOnLoadEvent_) {
      var gaiaFrame = $('gaia-frame');
      var handler = function() {
        gaiaFrame.removeEventListener('load', handler);
        if (!this.gaiaLoaded_) {
          this.gaiaLoaded_ = true;
          this.maybeInitialized_();

          if (this.useEafe_ && this.clientId_) {
            // Sends initial handshake message to EAFE. Note this fails with
            // SSO redirect because |gaiaFrame| sits on a different origin.
            gaiaFrame.contentWindow.postMessage({
              clientId: this.clientId_
            }, this.gaiaUrl_);
          }
        }
      }.bind(this);
      gaiaFrame.addEventListener('load', handler);
    }
  },

  initSupportChannel_: function() {
    var supportChannel = new Channel();
    supportChannel.connect('authMain');

    supportChannel.registerMessage('channelConnected', function() {
      // Load the gaia frame after the background page indicates that it is
      // ready, so that the webRequest handlers are all setup first.
      var gaiaFrame = $('gaia-frame');
      gaiaFrame.src = this.initialFrameUrl_;

      if (this.supportChannel_) {
        console.error('Support channel is already initialized.');
        return;
      }
      this.supportChannel_ = supportChannel;

      if (this.desktopMode_) {
        this.supportChannel_.send({
          name: 'initDesktopFlow',
          gaiaUrl: this.gaiaUrl_,
          continueUrl: stripParams(this.continueUrl_),
          isConstrainedWindow: this.isConstrainedWindow_,
          initialFrameUrlWithoutParams: this.initialFrameUrlWithoutParams_
        });

        this.supportChannel_.registerMessage(
            'switchToFullTab', this.switchToFullTab_.bind(this));
      }
      this.supportChannel_.registerMessage(
          'completeLogin', this.onCompleteLogin_.bind(this));
      this.initSAML_();
      this.supportChannel_.send({name: 'resetAuth'});
      this.maybeInitialized_();
    }.bind(this));

    window.setTimeout(function() {
      if (!this.supportChannel_) {
        // Give up previous channel and bind its 'channelConnected' to a no-op.
        supportChannel.registerMessage('channelConnected', function() {});

        // Re-initialize the channel if it is not connected properly, e.g.
        // connect may be called before background script started running.
        this.initSupportChannel_();
      }
    }.bind(this), 200);
  },

  /**
   * Called when one of the initialization stages has finished. If all the
   * needed parts are initialized, notifies parent about successfull
   * initialization.
   */
  maybeInitialized_: function() {
    if (!this.gaiaLoaded_ || !this.supportChannel_)
      return;
    var msg = {
      'method': 'loginUILoaded'
    };
    window.parent.postMessage(msg, this.parentPage_);
  },

  /**
   * Invoked when the background script sends a message to indicate that the
   * current content does not fit in a constrained window.
   * @param {Object=} msg Extra info to send.
   */
  switchToFullTab_: function(msg) {
    var parentMsg = {
      'method': 'switchToFullTab',
      'url': msg.url
    };
    window.parent.postMessage(parentMsg, this.parentPage_);
  },

  /**
   * Invoked when the signin flow is complete.
   * @param {Object=} opt_extraMsg Optional extra info to send.
   */
  completeLogin_: function(opt_extraMsg) {
    var msg = {
      'method': 'completeLogin',
      'email': (opt_extraMsg && opt_extraMsg.email) || this.email_,
      'password': this.passwordBytes_ ||
                  (opt_extraMsg && opt_extraMsg.password),
      'usingSAML': this.isSAMLFlow_,
      'chooseWhatToSync': this.chooseWhatToSync_ || false,
      'skipForNow': (opt_extraMsg && opt_extraMsg.skipForNow) ||
                    this.skipForNow_,
      'sessionIndex': (opt_extraMsg && opt_extraMsg.sessionIndex) ||
                      this.sessionIndex_,
      'gaiaId': (opt_extraMsg && opt_extraMsg.gaiaId) || this.gaiaId_
    };
    window.parent.postMessage(msg, this.parentPage_);
    this.supportChannel_.send({name: 'resetAuth'});
  },

  /**
   * Invoked when support channel is connected.
   */
  initSAML_: function() {
    this.isSAMLFlow_ = false;

    this.supportChannel_.registerMessage(
        'onAuthPageLoaded', this.onAuthPageLoaded_.bind(this));
    this.supportChannel_.registerMessage(
        'onInsecureContentBlocked', this.onInsecureContentBlocked_.bind(this));
    this.supportChannel_.registerMessage(
        'apiCall', this.onAPICall_.bind(this));
    this.supportChannel_.send({
      name: 'setGaiaUrl',
      gaiaUrl: this.gaiaUrl_
    });
    if (!this.desktopMode_ && this.gaiaUrl_.indexOf('https://') == 0) {
      // Abort the login flow when content served over an unencrypted connection
      // is detected on Chrome OS. This does not apply to tests that explicitly
      // set a non-https GAIA URL and want to perform all authentication over
      // http.
      this.supportChannel_.send({
        name: 'setBlockInsecureContent',
        blockInsecureContent: true
      });
    }
  },

  /**
   * Invoked when the background page sends 'onHostedPageLoaded' message.
   * @param {!Object} msg Details sent with the message.
   */
  onAuthPageLoaded_: function(msg) {
    if (msg.isSAMLPage && !this.isSAMLFlow_) {
      // GAIA redirected to a SAML login page. The credentials provided to this
      // page will determine what user gets logged in. The credentials obtained
      // from the GAIA login form are no longer relevant and can be discarded.
      this.isSAMLFlow_ = true;
      this.email_ = null;
      this.gaiaId_ = null;
      this.passwordBytes_ = null;
    }

    window.parent.postMessage({
      'method': 'authPageLoaded',
      'isSAML': this.isSAMLFlow_,
      'domain': extractDomain(msg.url)
    }, this.parentPage_);
  },

  /**
   * Invoked when the background page sends an 'onInsecureContentBlocked'
   * message.
   * @param {!Object} msg Details sent with the message.
   */
  onInsecureContentBlocked_: function(msg) {
    window.parent.postMessage({
      'method': 'insecureContentBlocked',
      'url': stripParams(msg.url)
    }, this.parentPage_);
  },

  /**
   * Invoked when one of the credential passing API methods is called by a SAML
   * provider.
   * @param {!Object} msg Details of the API call.
   */
  onAPICall_: function(msg) {
    var call = msg.call;
    if (call.method == 'initialize') {
      if (!Number.isInteger(call.requestedVersion) ||
          call.requestedVersion < Authenticator.MIN_API_VERSION_VERSION) {
        this.sendInitializationFailure_();
        return;
      }

      this.apiVersion_ = Math.min(call.requestedVersion,
                                  Authenticator.MAX_API_VERSION_VERSION);
      this.initialized_ = true;
      this.sendInitializationSuccess_();
      return;
    }

    if (call.method == 'add') {
      if (Authenticator.API_KEY_TYPES.indexOf(call.keyType) == -1) {
        console.error('Authenticator.onAPICall_: unsupported key type');
        return;
      }
      // Not setting |email_| and |gaiaId_| because this API call will
      // eventually be followed by onCompleteLogin_() which does set it.
      this.apiToken_ = call.token;
      this.passwordBytes_ = call.passwordBytes;
    } else if (call.method == 'confirm') {
      if (call.token != this.apiToken_)
        console.error('Authenticator.onAPICall_: token mismatch');
    } else {
      console.error('Authenticator.onAPICall_: unknown message');
    }
  },

  onGotAuthCode_: function(authCode) {
    window.parent.postMessage({
      'method': 'completeAuthenticationAuthCodeOnly',
      'authCode': authCode
    }, this.parentPage_);
  },

  sendInitializationSuccess_: function() {
    this.supportChannel_.send({name: 'apiResponse', response: {
      result: 'initialized',
      version: this.apiVersion_,
      keyTypes: Authenticator.API_KEY_TYPES
    }});
  },

  sendInitializationFailure_: function() {
    this.supportChannel_.send({
      name: 'apiResponse',
      response: {result: 'initialization_failed'}
    });
  },

  /**
   * Callback invoked for 'completeLogin' message.
   * @param {Object=} msg Message sent from background page.
   */
  onCompleteLogin_: function(msg) {
    if (!msg.email || !msg.gaiaId || !msg.sessionIndex) {
      // On desktop, if the skipForNow message field is set, send it to handler.
      // This does not require the email, gaiaid or session to be valid.
      if (this.desktopMode_ && msg.skipForNow) {
        this.completeLogin_(msg);
      } else {
        console.error('Missing fields to complete login.');
        window.parent.postMessage({method: 'missingGaiaInfo'},
                                  this.parentPage_);
        return;
      }
    }

    // Skip SAML extra steps for desktop flow and non-SAML flow.
    if (!this.isSAMLFlow_ || this.desktopMode_) {
      this.completeLogin_(msg);
      return;
    }

    this.email_ = msg.email;
    this.gaiaId_ = msg.gaiaId;
    // Password from |msg| is not used because ChromeOS SAML flow
    // gets password by asking user to confirm.
    this.skipForNow_ = msg.skipForNow;
    this.sessionIndex_ = msg.sessionIndex;

    if (this.passwordBytes_) {
      // If the credentials passing API was used, login is complete.
      window.parent.postMessage({method: 'samlApiUsed'}, this.parentPage_);
      this.completeLogin_(msg);
    } else if (!this.needPassword_) {
      // If the credentials passing API was not used, the password was obtained
      // by scraping. It must be verified before use. However, the host may not
      // be interested in the password at all. In that case, verification is
      // unnecessary and login is complete.
      this.completeLogin_(msg);
    } else {
      this.supportChannel_.sendWithCallback(
          {name: 'getScrapedPasswords'},
          function(passwords) {
            if (passwords.length == 0) {
              window.parent.postMessage(
                  {method: 'noPassword', email: this.email_},
                  this.parentPage_);
            } else {
              window.parent.postMessage({method: 'confirmPassword',
                                         email: this.email_,
                                         passwordCount: passwords.length},
                                        this.parentPage_);
            }
          }.bind(this));
    }
  },

  onVerifyConfirmedPassword_: function(password) {
    this.supportChannel_.sendWithCallback(
        {name: 'getScrapedPasswords'},
        function(passwords) {
          for (var i = 0; i < passwords.length; ++i) {
            if (passwords[i] == password) {
              this.passwordBytes_ = passwords[i];
              // SAML login is complete when the user has successfully
              // confirmed the password.
              if (this.passwordBytes_ !== null)
                this.completeLogin_();
              return;
            }
          }
          window.parent.postMessage(
              {method: 'confirmPassword', email: this.email_},
              this.parentPage_);
        }.bind(this));
  },

  onMessage: function(e) {
    var msg = e.data;

    if (this.useEafe_) {
      if (msg == '!_{h:\'gaia-frame\'}' && this.isGaiaMessage_(e)) {
        // Sends client ID again on the hello message to work around the SSO
        // signin issue.
        // TODO(xiyuan): Revisit this when EAFE is integrated or for webview.
        $('gaia-frame').contentWindow.postMessage({
          clientId: this.clientId_
        }, this.gaiaUrl_);
      } else if (typeof msg == 'object' &&
                 msg.type == 'authorizationCode' && this.isGaiaMessage_(e)) {
        this.onGotAuthCode_(msg.authorizationCode);
      } else {
        console.error('Authenticator.onMessage: unknown message' +
                      ', msg=' + JSON.stringify(msg));
      }

      return;
    }

    if (msg.method == 'attemptLogin' && this.isGaiaMessage_(e)) {
      // At this point GAIA does not yet know the gaiaId, so its not set here.
      this.email_ = msg.email;
      this.passwordBytes_ = msg.password;
      this.attemptToken_ = msg.attemptToken;
      this.chooseWhatToSync_ = msg.chooseWhatToSync;
      this.isSAMLFlow_ = false;
      if (this.supportChannel_)
        this.supportChannel_.send({name: 'startAuth'});
      else
        console.error('Support channel is not initialized.');
    } else if (msg.method == 'clearOldAttempts' && this.isGaiaMessage_(e)) {
      if (!this.gaiaLoaded_) {
        this.gaiaLoaded_ = true;
        this.maybeInitialized_();
      }
      this.email_ = null;
      this.gaiaId_ = null;
      this.sessionIndex_ = false;
      this.passwordBytes_ = null;
      this.attemptToken_ = null;
      this.isSAMLFlow_ = false;
      this.skipForNow_ = false;
      this.chooseWhatToSync_ = false;
      if (this.supportChannel_) {
        this.supportChannel_.send({name: 'resetAuth'});
        // This message is for clearing saml properties in gaia_auth_host and
        // oobe_screen_oauth_enrollment.
        window.parent.postMessage({
          'method': 'resetAuthFlow',
        }, this.parentPage_);
      }
    } else if (msg.method == 'verifyConfirmedPassword' &&
               this.isParentMessage_(e)) {
      this.onVerifyConfirmedPassword_(msg.password);
    } else if (msg.method == 'redirectToSignin' &&
               this.isParentMessage_(e)) {
      $('gaia-frame').src = this.constructInitialFrameUrl_();
    } else {
      console.error('Authenticator.onMessage: unknown message + origin!?');
    }
  }
};

Authenticator.getInstance().initialize();
/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

html,
body,
iframe {
  height: 100%;
  margin: 0;
  padding: 0;
  width: 100%;
}

iframe {
  overflow: hidden;
}

webview {
  display: inline-block;
  height: 100%;
  margin: 0;
  padding: 0;
  width: 100%;
}
<!doctype html>
<html>
<head>
  <meta charset="utf-8">
  <link rel="stylesheet" href="offline.css">
  <script src="util.js"></script>
  <script src="offline.js"></script>
</head>
<body>
  <div class="wrapper">
    <div class="main content">
      <div class="signin-box">
        <h2 id="sign-in-title"></h2>
        <form id="offline-login-form">
          <label>
            <strong id="email-label" class="email-label"></strong>
            <input type="text" name="email" value="">
            <span role="alert" class="errormsg" id="empty-email-alert">
            </span>
          </label>
          <label>
            <strong id="password-label" class="passwd-label"></strong>
            <input type="password" name="password">
            <span role="alert" class="errormsg" id="empty-password-alert">
            </span>
            <span role="alert" class="errormsg" id="errormsg-alert">
            </span>
          </label>
          <input id="submit-button" type="submit"
              class="g-button g-button-submit">
        </form>
      </div>
    </div>
  </div>
</body>
</html>
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Offline login implementation.
 */

/**
 * Initialize the offline page.
 * @param {Object} params Intialization params passed from parent page.
 */
function load(params) {
  // Setup localized strings.
  $('sign-in-title').textContent = decodeURIComponent(params['stringSignIn']);
  $('email-label').textContent = decodeURIComponent(params['stringEmail']);
  $('password-label').textContent =
      decodeURIComponent(params['stringPassword']);
  $('submit-button').value = decodeURIComponent(params['stringSignIn']);
  $('empty-email-alert').textContent =
      decodeURIComponent(params['stringEmptyEmail']);
  $('empty-password-alert').textContent =
      decodeURIComponent(params['stringEmptyPassword']);
  $('errormsg-alert').textContent = decodeURIComponent(params['stringError']);

  // Setup actions.
  var form = $('offline-login-form');
  form.addEventListener('submit', function(e) {
    // Clear all previous errors.
    form.email.classList.remove('field-error');
    form.password.classList.remove('field-error');
    form.password.classList.remove('form-error');

    if (form.email.value == '') {
      form.email.classList.add('field-error');
      form.email.focus();
    } else if (form.password.value == '') {
      form.password.classList.add('field-error');
      form.password.focus();
    } else {
      var msg = {
        'method': 'offlineLogin',
        'email': form.email.value,
        'password': form.password.value
      };
      window.parent.postMessage(msg, 'chrome://oobe/');
    }
    e.preventDefault();
  });

  var email = params['email'];
  if (email) {
    // Email is present, which means that unsuccessful login attempt has been
    // made. Try to mimic Gaia's behaviour.
    form.email.value = email;
    form.password.classList.add('form-error');
    form.password.focus();
  } else {
    form.email.focus();
  }
  window.parent.postMessage({'method': 'loginUILoaded'}, 'chrome://oobe/');
}

/**
 * Handles initialization message from parent page.
 * @param {MessageEvent} e
 */
function handleInitializeMessage(e) {
  var ALLOWED_PARENT_ORIGINS = [
    'chrome://oobe',
    'chrome://chrome-signin'
  ];

  if (ALLOWED_PARENT_ORIGINS.indexOf(e.origin) == -1)
    return;

  window.removeEventListener('message', handleInitializeMessage);

  var params = e.data;
  params.parentPage = e.origin;
  load(params);
}

document.addEventListener('DOMContentLoaded', function() {
  window.addEventListener('message', handleInitializeMessage);
  window.parent.postMessage({'method': 'loginUIDOMContentLoaded'}, '*');
});
/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */
/* TODO(dbeam): what's wrong with * here? Specificity issues? */
audio,
body,
canvas,
command,
dd,
div,
dl,
dt,
embed,
form,
group,
h1,
h2,
h3,
h4,
h5,
h6,
html,
img,
mark,
meter,
object,
output,
progress,
summary,
td,
time,
tr,
video {
  border: 0;
  margin: 0;
  padding: 0;
}
html {
  background: #fff;
  color: #333;
  direction: ltr;
  font: 81.25% arial, helvetica, sans-serif;
  line-height: 1;
}
h1,
h2,
h3,
h4,
h5,
h6 {
  color: #222;
  font-size: 1.54em;
  font-weight: normal;
  line-height: 24px;
  margin: 0 0 .46em;
}
strong {
  color: #222;
}
body,
html {
  height: 100%;
  min-width: 100%;
  position: absolute;
}
.wrapper {
  min-height: 100%;
  position: relative;
}
.content {
  padding: 0 44px;
}
.main {
  margin: 0 auto;
  padding-bottom: 100px;
  padding-top: 23px;
  width: 650px;
}
button,
input,
select,
textarea {
  font-family: inherit;
  font-size: inherit;
}
input[type=email],
input[type=number],
input[type=password],
input[type=text],
input[type=url] {
  -webkit-box-sizing: border-box;
  background: #fff;
  border: 1px solid #d9d9d9;
  border-radius: 1px;
  border-top: 1px solid #c0c0c0;
  box-sizing: border-box;
  display: inline-block;
  height: 29px;
  margin: 0;
  padding-left: 8px;
}
input[type=email]:hover,
input[type=number]:hover,
input[type=password]:hover,
input[type=text]:hover,
input[type=url]:hover {
  border: 1px solid #b9b9b9;
  border-top: 1px solid #a0a0a0;
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
}
input[type=email]:focus,
input[type=number]:focus,
input[type=password]:focus,
input[type=text]:focus,
input[type=url]:focus {
  border: 1px solid rgb(77, 144, 254);
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.3);
  outline: none;
}
input[type=email][disabled=disabled],
input[type=number][disabled=disabled],
input[type=password][disabled=disabled],
input[type=text][disabled=disabled],
input[type=url][disabled=disabled] {
  background: #f5f5f5;
  border: 1px solid #e5e5e5;
}
input[type=email][disabled=disabled]:hover,
input[type=number][disabled=disabled]:hover,
input[type=password][disabled=disabled]:hover,
input[type=text][disabled=disabled]:hover,
input[type=url][disabled=disabled]:hover {
  box-shadow: none;
}
.g-button {
  -webkit-transition: all 218ms;
  -webkit-user-select: none;
  background-color: #f5f5f5;
  background-image: linear-gradient(to bottom, #f5f5f5, #f1f1f1);
  border: 1px solid rgba(0,0,0,0.1);
  border-radius: 2px;
  color: #555;
  cursor: default;
  display: inline-block;
  font-size: 11px;
  font-weight: bold;
  height: 27px;
  line-height: 27px;
  min-width: 54px;
  padding: 0 8px;
  text-align: center;
  transition: all 218ms;
  user-select: none;
}
*+html .g-button {
  min-width: 70px;
}
button.g-button,
input[type=submit].g-button {
  height: 29px;
  line-height: 29px;
  margin: 0;
  vertical-align: bottom;
}
*+html button.g-button,
*+html input[type=submit].g-button {
  overflow: visible;
}
.g-button:hover {
  -webkit-transition: all 0ms;
  background-color: #f8f8f8;
  background-image: linear-gradient(to bottom, #f8f8f8, #f1f1f1);
  border: 1px solid #c6c6c6;
  box-shadow: 0 1px 1px rgba(0,0,0,0.1);
  color: #333;
  text-decoration: none;
  transition: all 0ms;
}
.g-button:active {
  background-color: #f6f6f6;
  background-image: linear-gradient(to bottom, #f6f6f6, #f1f1f1);
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.1);
}
.g-button:visited {
  color: #666;
}
.g-button-submit {
  background-color: rgb(77, 144, 254);
  background-image:
      linear-gradient(to bottom, rgb(77, 144, 254), rgb(71, 135, 237));
  border: 1px solid rgb(48, 121, 237);
  color: #fff;
  text-shadow: 0 1px rgba(0,0,0,0.1);
}
.g-button-submit:hover {
  background-color: rgb(53, 122, 232);
  background-image:
      linear-gradient(to bottom, rgb(77, 144, 254), rgb(53, 122, 232));
  border: 1px solid rgb(47, 91, 183);
  color: #fff;
  text-shadow: 0 1px rgba(0,0,0,0.3);
}
.g-button-submit:active {
  box-shadow: inset 0 1px 2px rgba(0,0,0,0.3);
}
.g-button-submit:visited {
  color: #fff;
}
.g-button-submit:focus {
  box-shadow: inset 0 0 0 1px #fff;
}
.g-button-submit:focus:hover {
  box-shadow: inset 0 0 0 1px #fff, 0 1px 1px rgba(0,0,0,0.1);
}
.g-button:hover img {
  opacity: .72;
}
.g-button:active img {
  opacity: 1;
}
.errormsg {
  color: rgb(221, 75, 57);
  display: block;
  line-height: 17px;
  margin: .5em 0 0;
}
input[type=email].form-error,
input[type=number].form-error,
input[type=password].form-error,
input[type=text].form-error,
input[type=url].form-error,
input[type=text].field-error,
input[type=password].field-error {
  border: 1px solid rgb(221, 75, 57);
}
html {
  background: transparent;
}
.content {
  width: auto;
}
.main {
  padding-bottom: 12px;
  padding-top: 23px;
}
.signin-box h2 {
  font-size: 16px;
  height: 16px;
  line-height: 17px;
  margin: 0 0 1.2em;
  position: relative;
}
.signin-box label {
  display: block;
  margin: 0 0 1.5em;
}
.signin-box input[type=text],
.signin-box input[type=password] {
  font-size: 15px;
  height: 32px;
  width: 100%;
}
.signin-box .email-label,
.signin-box .passwd-label {
  -webkit-user-select: none;
  display: block;
  font-weight: bold;
  margin: 0 0 .5em;
  user-select: none;
}
.signin-box input[type=submit] {
  font-size: 13px;
  height: 32px;
  margin: 0 1.5em 1.2em 0;
}
.errormsg {
  display: none;
}
.form-error + .errormsg,
.field-error + .errormsg {
  display: block;
}
<!doctype html>
<head>
<meta charset="utf-8">
</head>
<html>
<body></body>
</html>
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Alias for document.getElementById.
 * @param {string} id The ID of the element to find.
 * @return {HTMLElement} The found element or null if not found.
 */
function $(id) {
  return document.getElementById(id);
}

/**
 * Creates a new URL which is the old URL with a GET param of key=value.
 * Copied from ui/webui/resources/js/util.js.
 * @param {string} url The base URL. There is not sanity checking on the URL so
 *     it must be passed in a proper format.
 * @param {string} key The key of the param.
 * @param {string} value The value of the param.
 * @return {string} The new URL.
 */
function appendParam(url, key, value) {
  var param = encodeURIComponent(key) + '=' + encodeURIComponent(value);

  if (url.indexOf('?') == -1)
    return url + '?' + param;
  return url + '&' + param;
}

/**
 * Creates a new URL by striping all query parameters.
 * @param {string} url The original URL.
 * @return {string} The new URL with all query parameters stripped.
 */
function stripParams(url) {
  return url.substring(0, url.indexOf('?')) || url;
}

/**
 * Extract domain name from an URL.
 * @param {string} url An URL string.
 * @return {string} The host name of the URL.
 */
function extractDomain(url) {
  var a = document.createElement('a');
  a.href = url;
  return a.hostname;
}
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview
 * A background script of the auth extension that bridges the communication
 * between the main and injected scripts.
 *
 * Here is an overview of the communication flow when SAML is being used:
 * 1. The main script sends the |startAuth| signal to this background script,
 *    indicating that the authentication flow has started and SAML pages may be
 *    loaded from now on.
 * 2. A script is injected into each SAML page. The injected script sends three
 *    main types of messages to this background script:
 *    a) A |pageLoaded| message is sent when the page has been loaded. This is
 *       forwarded to the main script as |onAuthPageLoaded|.
 *    b) If the SAML provider supports the credential passing API, the API calls
 *       are sent to this background script as |apiCall| messages. These
 *       messages are forwarded unmodified to the main script.
 *    c) The injected script scrapes passwords. They are sent to this background
 *       script in |updatePassword| messages. The main script can request a list
 *       of the scraped passwords by sending the |getScrapedPasswords| message.
 */

/**
 * BackgroundBridgeManager maintains an array of BackgroundBridge, indexed by
 * the associated tab id.
 */
function BackgroundBridgeManager() {
  this.bridges_ = {};
}

BackgroundBridgeManager.prototype = {
  CONTINUE_URL_BASE: 'chrome-extension://mfffpogegjflfpflabcdkioaeobkgjik' +
                     '/success.html',
  // Maps a tab id to its associated BackgroundBridge.
  bridges_: null,

  run: function() {
    chrome.runtime.onConnect.addListener(this.onConnect_.bind(this));

    chrome.webRequest.onBeforeRequest.addListener(
        function(details) {
          if (this.bridges_[details.tabId])
            return this.bridges_[details.tabId].onInsecureRequest(details.url);
        }.bind(this),
        {urls: ['http://*/*', 'file://*/*', 'ftp://*/*']},
        ['blocking']);

    chrome.webRequest.onBeforeSendHeaders.addListener(
        function(details) {
          if (this.bridges_[details.tabId])
            return this.bridges_[details.tabId].onBeforeSendHeaders(details);
          else
            return {requestHeaders: details.requestHeaders};
        }.bind(this),
        {urls: ['*://*/*'], types: ['sub_frame']},
        ['blocking', 'requestHeaders']);

    chrome.webRequest.onHeadersReceived.addListener(
        function(details) {
          if (this.bridges_[details.tabId])
            return this.bridges_[details.tabId].onHeadersReceived(details);
        }.bind(this),
        {urls: ['*://*/*'], types: ['sub_frame']},
        ['blocking', 'responseHeaders']);

    chrome.webRequest.onCompleted.addListener(
        function(details) {
          if (this.bridges_[details.tabId])
            this.bridges_[details.tabId].onCompleted(details);
        }.bind(this),
        {urls: ['*://*/*', this.CONTINUE_URL_BASE + '*'], types: ['sub_frame']},
        ['responseHeaders']);
  },

  onConnect_: function(port) {
    var tabId = this.getTabIdFromPort_(port);
    if (!this.bridges_[tabId])
      this.bridges_[tabId] = new BackgroundBridge(tabId);
    if (port.name == 'authMain') {
      this.bridges_[tabId].setupForAuthMain(port);
      port.onDisconnect.addListener(function() {
        delete this.bridges_[tabId];
      }.bind(this));
    } else if (port.name == 'injected') {
      this.bridges_[tabId].setupForInjected(port);
    } else {
      console.error('Unexpected connection, port.name=' + port.name);
    }
  },

  getTabIdFromPort_: function(port) {
    return port.sender.tab ? port.sender.tab.id : -1;
  }
};

/**
 * BackgroundBridge allows the main script and the injected script to
 * collaborate. It forwards credentials API calls to the main script and
 * maintains a list of scraped passwords.
 * @param {string} tabId The associated tab ID.
 */
function BackgroundBridge(tabId) {
  this.tabId_ = tabId;
  this.passwordStore_ = {};
}

BackgroundBridge.prototype = {
  // The associated tab ID. Only used for debugging now.
  tabId: null,

  // The initial URL loaded in the gaia iframe.  We only want to handle
  // onCompleted() for the frame that loaded this URL.
  initialFrameUrlWithoutParams: null,

  // On process onCompleted() requests that come from this frame Id.
  frameId: -1,

  isDesktopFlow_: false,

  // Whether the extension is loaded in a constrained window.
  // Set from main auth script.
  isConstrainedWindow_: null,

  // Email of the newly authenticated user based on the gaia response header
  // 'google-accounts-signin'.
  email_: null,

  // Gaia Id of the newly authenticated user based on the gaia response
  // header 'google-accounts-signin'.
  gaiaId_: null,

  // Session index of the newly authenticated user based on the gaia response
  // header 'google-accounts-signin'.
  sessionIndex_: null,

  // Gaia URL base that is set from main auth script.
  gaiaUrl_: null,

  // Whether to abort the authentication flow and show an error messagen when
  // content served over an unencrypted connection is detected.
  blockInsecureContent_: false,

  // Whether auth flow has started. It is used as a signal of whether the
  // injected script should scrape passwords.
  authStarted_: false,

  // Whether SAML flow is going.
  isSAML_: false,

  passwordStore_: null,

  channelMain_: null,
  channelInjected_: null,

  /**
   * Sets up the communication channel with the main script.
   */
  setupForAuthMain: function(port) {
    this.channelMain_ = new Channel();
    this.channelMain_.init(port);

    // Registers for desktop related messages.
    this.channelMain_.registerMessage(
        'initDesktopFlow', this.onInitDesktopFlow_.bind(this));

    // Registers for SAML related messages.
    this.channelMain_.registerMessage(
        'setGaiaUrl', this.onSetGaiaUrl_.bind(this));
    this.channelMain_.registerMessage(
        'setBlockInsecureContent', this.onSetBlockInsecureContent_.bind(this));
    this.channelMain_.registerMessage(
        'resetAuth', this.onResetAuth_.bind(this));
    this.channelMain_.registerMessage(
        'startAuth', this.onAuthStarted_.bind(this));
    this.channelMain_.registerMessage(
        'getScrapedPasswords',
        this.onGetScrapedPasswords_.bind(this));
    this.channelMain_.registerMessage(
        'apiResponse', this.onAPIResponse_.bind(this));

    this.channelMain_.send({
      'name': 'channelConnected'
    });
  },

  /**
   * Sets up the communication channel with the injected script.
   */
  setupForInjected: function(port) {
    this.channelInjected_ = new Channel();
    this.channelInjected_.init(port);

    this.channelInjected_.registerMessage(
        'apiCall', this.onAPICall_.bind(this));
    this.channelInjected_.registerMessage(
        'updatePassword', this.onUpdatePassword_.bind(this));
    this.channelInjected_.registerMessage(
        'pageLoaded', this.onPageLoaded_.bind(this));
    this.channelInjected_.registerMessage(
        'getSAMLFlag', this.onGetSAMLFlag_.bind(this));
  },

  /**
   * Handler for 'initDesktopFlow' signal sent from the main script.
   * Only called in desktop mode.
   */
  onInitDesktopFlow_: function(msg) {
    this.isDesktopFlow_ = true;
    this.gaiaUrl_ = msg.gaiaUrl;
    this.isConstrainedWindow_ = msg.isConstrainedWindow;
    this.initialFrameUrlWithoutParams = msg.initialFrameUrlWithoutParams;
  },

  /**
   * Handler for webRequest.onCompleted. It 1) detects loading of continue URL
   * and notifies the main script of signin completion; 2) detects if the
   * current page could be loaded in a constrained window and signals the main
   * script of switching to full tab if necessary.
   */
  onCompleted: function(details) {
    // Only monitors requests in the gaia frame.  The gaia frame is the one
    // where the initial frame URL completes.
    if (details.url.lastIndexOf(
            this.initialFrameUrlWithoutParams, 0) == 0) {
      this.frameId = details.frameId;
    }
    if (this.frameId == -1) {
      // If for some reason the frameId could not be set above, just make sure
      // the frame is more than two levels deep (since the gaia frame is at
      // least three levels deep).
      if (details.parentFrameId <= 0)
        return;
    } else if (details.frameId != this.frameId) {
      return;
    }

    if (details.url.lastIndexOf(backgroundBridgeManager.CONTINUE_URL_BASE, 0) ==
        0) {
      var skipForNow = false;
      if (details.url.indexOf('ntp=1') >= 0)
        skipForNow = true;

      // TOOD(guohui): For desktop SAML flow, show password confirmation UI.
      var passwords = this.onGetScrapedPasswords_();
      var msg = {
        'name': 'completeLogin',
        'email': this.email_,
        'gaiaId': this.gaiaId_,
        'password': passwords[0],
        'sessionIndex': this.sessionIndex_,
        'skipForNow': skipForNow
      };
      this.channelMain_.send(msg);
    } else if (this.isConstrainedWindow_) {
      // The header google-accounts-embedded is only set on gaia domain.
      if (this.gaiaUrl_ && details.url.lastIndexOf(this.gaiaUrl_) == 0) {
        var headers = details.responseHeaders;
        for (var i = 0; headers && i < headers.length; ++i) {
          if (headers[i].name.toLowerCase() == 'google-accounts-embedded')
            return;
        }
      }
      var msg = {
        'name': 'switchToFullTab',
        'url': details.url
      };
      this.channelMain_.send(msg);
    }
  },

  /**
   * Handler for webRequest.onBeforeRequest, invoked when content served over an
   * unencrypted connection is detected. Determines whether the request should
   * be blocked and if so, signals that an error message needs to be shown.
   * @param {string} url The URL that was blocked.
   * @return {!Object} Decision whether to block the request.
   */
  onInsecureRequest: function(url) {
    if (!this.blockInsecureContent_)
      return {};
    this.channelMain_.send({name: 'onInsecureContentBlocked', url: url});
    return {cancel: true};
  },

  /**
   * Handler or webRequest.onHeadersReceived. It reads the authenticated user
   * email from google-accounts-signin-header.
   * @return {!Object} Modified request headers.
   */
  onHeadersReceived: function(details) {
    var headers = details.responseHeaders;

    if (this.gaiaUrl_ && details.url.lastIndexOf(this.gaiaUrl_) == 0) {
      for (var i = 0; headers && i < headers.length; ++i) {
        if (headers[i].name.toLowerCase() == 'google-accounts-signin') {
          var headerValues = headers[i].value.toLowerCase().split(',');
          var signinDetails = {};
          headerValues.forEach(function(e) {
            var pair = e.split('=');
            signinDetails[pair[0].trim()] = pair[1].trim();
          });
          // Remove "" around.
          this.email_ = signinDetails['email'].slice(1, -1);
          this.gaiaId_ = signinDetails['obfuscatedid'].slice(1, -1);
          this.sessionIndex_ = signinDetails['sessionindex'];
          break;
        }
      }
    }

    if (!this.isDesktopFlow_) {
      // Check whether GAIA headers indicating the start or end of a SAML
      // redirect are present. If so, synthesize cookies to mark these points.
      for (var i = 0; headers && i < headers.length; ++i) {
        if (headers[i].name.toLowerCase() == 'google-accounts-saml') {
          var action = headers[i].value.toLowerCase();
          if (action == 'start') {
            this.isSAML_ = true;
            // GAIA is redirecting to a SAML IdP. Any cookies contained in the
            // current |headers| were set by GAIA. Any cookies set in future
            // requests will be coming from the IdP. Append a cookie to the
            // current |headers| that marks the point at which the redirect
            // occurred.
            headers.push({name: 'Set-Cookie',
                          value: 'google-accounts-saml-start=now'});
            return {responseHeaders: headers};
          } else if (action == 'end') {
            this.isSAML_ = false;
            // The SAML IdP has redirected back to GAIA. Add a cookie that marks
            // the point at which the redirect occurred occurred. It is
            // important that this cookie be prepended to the current |headers|
            // because any cookies contained in the |headers| were already set
            // by GAIA, not the IdP. Due to limitations in the webRequest API,
            // it is not trivial to prepend a cookie:
            //
            // The webRequest API only allows for deleting and appending
            // headers. To prepend a cookie (C), three steps are needed:
            // 1) Delete any headers that set cookies (e.g., A, B).
            // 2) Append a header which sets the cookie (C).
            // 3) Append the original headers (A, B).
            //
            // Due to a further limitation of the webRequest API, it is not
            // possible to delete a header in step 1) and append an identical
            // header in step 3). To work around this, a trailing semicolon is
            // added to each header before appending it. Trailing semicolons are
            // ignored by Chrome in cookie headers, causing the modified headers
            // to actually set the original cookies.
            var otherHeaders = [];
            var cookies = [{name: 'Set-Cookie',
                            value: 'google-accounts-saml-end=now'}];
            for (var j = 0; j < headers.length; ++j) {
              if (headers[j].name.toLowerCase().indexOf('set-cookie') == 0) {
                var header = headers[j];
                header.value += ';';
                cookies.push(header);
              } else {
                otherHeaders.push(headers[j]);
              }
            }
            return {responseHeaders: otherHeaders.concat(cookies)};
          }
        }
      }
    }

    return {};
  },

  /**
   * Handler for webRequest.onBeforeSendHeaders.
   * @return {!Object} Modified request headers.
   */
  onBeforeSendHeaders: function(details) {
    if (!this.isDesktopFlow_ && this.gaiaUrl_ &&
        details.url.indexOf(this.gaiaUrl_) == 0) {
      details.requestHeaders.push({
        name: 'X-Cros-Auth-Ext-Support',
        value: 'SAML'
      });
    }
    return {requestHeaders: details.requestHeaders};
  },

  /**
   * Handler for 'setGaiaUrl' signal sent from the main script.
   */
  onSetGaiaUrl_: function(msg) {
    this.gaiaUrl_ = msg.gaiaUrl;
  },

  /**
   * Handler for 'setBlockInsecureContent' signal sent from the main script.
   */
  onSetBlockInsecureContent_: function(msg) {
    this.blockInsecureContent_ = msg.blockInsecureContent;
  },

  /**
   * Handler for 'resetAuth' signal sent from the main script.
   */
  onResetAuth_: function() {
    this.authStarted_ = false;
    this.passwordStore_ = {};
    this.isSAML_ = false;
  },

  /**
   * Handler for 'authStarted' signal sent from the main script.
   */
  onAuthStarted_: function() {
    this.authStarted_ = true;
    this.passwordStore_ = {};
    this.isSAML_ = false;
  },

  /**
   * Handler for 'getScrapedPasswords' request sent from the main script.
   * @return {Array<string>} The array with de-duped scraped passwords.
   */
  onGetScrapedPasswords_: function() {
    var passwords = {};
    for (var property in this.passwordStore_) {
      passwords[this.passwordStore_[property]] = true;
    }
    return Object.keys(passwords);
  },

  /**
   * Handler for 'apiResponse' signal sent from the main script. Passes on the
   * |msg| to the injected script.
   */
  onAPIResponse_: function(msg) {
    this.channelInjected_.send(msg);
  },

  onAPICall_: function(msg) {
    this.channelMain_.send(msg);
  },

  onUpdatePassword_: function(msg) {
    if (!this.authStarted_)
      return;

    this.passwordStore_[msg.id] = msg.password;
  },

  onPageLoaded_: function(msg) {
    if (this.channelMain_)
      this.channelMain_.send({name: 'onAuthPageLoaded',
                              url: msg.url,
                              isSAMLPage: this.isSAML_});
  },

  onGetSAMLFlag_: function(msg) {
    return this.isSAML_;
  }
};

var backgroundBridgeManager = new BackgroundBridgeManager();
backgroundBridgeManager.run();
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview
 * Script to be injected into SAML provider pages, serving three main purposes:
 * 1. Signal hosting extension that an external page is loaded so that the
 *    UI around it should be changed accordingly;
 * 2. Provide an API via which the SAML provider can pass user credentials to
 *    Chrome OS, allowing the password to be used for encrypting user data and
 *    offline login.
 * 3. Scrape password fields, making the password available to Chrome OS even if
 *    the SAML provider does not support the credential passing API.
 */

(function() {
  function APICallForwarder() {
  }

  /**
   * The credential passing API is used by sending messages to the SAML page's
   * |window| object. This class forwards API calls from the SAML page to a
   * background script and API responses from the background script to the SAML
   * page. Communication with the background script occurs via a |Channel|.
   */
  APICallForwarder.prototype = {
    // Channel to which API calls are forwarded.
    channel_: null,

    /**
     * Initialize the API call forwarder.
     * @param {!Object} channel Channel to which API calls should be forwarded.
     */
    init: function(channel) {
      this.channel_ = channel;
      this.channel_.registerMessage('apiResponse',
                                    this.onAPIResponse_.bind(this));

      window.addEventListener('message', this.onMessage_.bind(this));
    },

    onMessage_: function(event) {
      if (event.source != window ||
          typeof event.data != 'object' ||
          !event.data.hasOwnProperty('type') ||
          event.data.type != 'gaia_saml_api') {
        return;
      }
      // Forward API calls to the background script.
      this.channel_.send({name: 'apiCall', call: event.data.call});
    },

    onAPIResponse_: function(msg) {
      // Forward API responses to the SAML page.
      window.postMessage({type: 'gaia_saml_api_reply', response: msg.response},
                         '/');
    }
  };

  /**
   * A class to scrape password from type=password input elements under a given
   * docRoot and send them back via a Channel.
   */
  function PasswordInputScraper() {
  }

  PasswordInputScraper.prototype = {
    // URL of the page.
    pageURL_: null,

    // Channel to send back changed password.
    channel_: null,

    // An array to hold password fields.
    passwordFields_: null,

    // An array to hold cached password values.
    passwordValues_: null,

    // A MutationObserver to watch for dynamic password field creation.
    passwordFieldsObserver: null,

    /**
     * Initialize the scraper with given channel and docRoot. Note that the
     * scanning for password fields happens inside the function and does not
     * handle DOM tree changes after the call returns.
     * @param {!Object} channel The channel to send back password.
     * @param {!string} pageURL URL of the page.
     * @param {!HTMLElement} docRoot The root element of the DOM tree that
     *     contains the password fields of interest.
     */
    init: function(channel, pageURL, docRoot) {
      this.pageURL_ = pageURL;
      this.channel_ = channel;

      this.passwordFields_ = [];
      this.passwordValues_ = [];

      this.findAndTrackChildren(docRoot);

      this.passwordFieldsObserver = new MutationObserver(function(mutations) {
        mutations.forEach(function(mutation) {
          Array.prototype.forEach.call(
            mutation.addedNodes,
            function(addedNode) {
              if (addedNode.nodeType != Node.ELEMENT_NODE)
                return;

              if (addedNode.matches('input[type=password]')) {
                this.trackPasswordField(addedNode);
              } else {
                this.findAndTrackChildren(addedNode);
              }
            }.bind(this));
        }.bind(this));
      }.bind(this));
      this.passwordFieldsObserver.observe(docRoot,
                                          {subtree: true, childList: true});
    },

    /**
     * Find and track password fields that are descendants of the given element.
     * @param {!HTMLElement} element The parent element to search from.
     */
    findAndTrackChildren: function(element) {
      Array.prototype.forEach.call(
          element.querySelectorAll('input[type=password]'), function(field) {
            this.trackPasswordField(field);
          }.bind(this));
    },

    /**
     * Start tracking value changes of the given password field if it is
     * not being tracked yet.
     * @param {!HTMLInputElement} passworField The password field to track.
     */
    trackPasswordField: function(passwordField) {
      var existing = this.passwordFields_.filter(function(element) {
        return element === passwordField;
      });
      if (existing.length != 0)
        return;

      var index = this.passwordFields_.length;
      passwordField.addEventListener(
          'input', this.onPasswordChanged_.bind(this, index));
      this.passwordFields_.push(passwordField);
      this.passwordValues_.push(passwordField.value);
    },

    /**
     * Check if the password field at |index| has changed. If so, sends back
     * the updated value.
     */
    maybeSendUpdatedPassword: function(index) {
      var newValue = this.passwordFields_[index].value;
      if (newValue == this.passwordValues_[index])
        return;

      this.passwordValues_[index] = newValue;

      // Use an invalid char for URL as delimiter to concatenate page url and
      // password field index to construct a unique ID for the password field.
      var passwordId = this.pageURL_.split('#')[0].split('?')[0] + '|' + index;
      this.channel_.send({
        name: 'updatePassword',
        id: passwordId,
        password: newValue
      });
    },

    /**
     * Handles 'change' event in the scraped password fields.
     * @param {number} index The index of the password fields in
     *     |passwordFields_|.
     */
    onPasswordChanged_: function(index) {
      this.maybeSendUpdatedPassword(index);
    }
  };

  function onGetSAMLFlag(channel, isSAMLPage) {
    if (!isSAMLPage)
      return;
    var pageURL = window.location.href;

    channel.send({name: 'pageLoaded', url: pageURL});

    var initPasswordScraper = function() {
      var passwordScraper = new PasswordInputScraper();
      passwordScraper.init(channel, pageURL, document.documentElement);
    };

    if (document.readyState == 'loading') {
      window.addEventListener('readystatechange', function listener(event) {
        if (document.readyState == 'loading')
          return;
        initPasswordScraper();
        window.removeEventListener(event.type, listener, true);
      }, true);
    } else {
      initPasswordScraper();
    }
  }

  var channel = Channel.create();
  channel.connect('injected');
  channel.sendWithCallback({name: 'getSAMLFlag'},
                           onGetSAMLFlag.bind(undefined, channel));

  var apiCallForwarder = new APICallForwarder();
  apiCallForwarder.init(channel);
})();
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Channel to the background script.
 */
function Channel() {
  this.messageCallbacks_ = {};
  this.internalRequestCallbacks_ = {};
}

/** @const */
Channel.INTERNAL_REQUEST_MESSAGE = 'internal-request-message';

/** @const */
Channel.INTERNAL_REPLY_MESSAGE = 'internal-reply-message';

Channel.prototype = {
  // Message port to use to communicate with background script.
  port_: null,

  // Registered message callbacks.
  messageCallbacks_: null,

  // Internal request id to track pending requests.
  nextInternalRequestId_: 0,

  // Pending internal request callbacks.
  internalRequestCallbacks_: null,

  /**
   * Initialize the channel with given port for the background script.
   */
  init: function(port) {
    this.port_ = port;
    this.port_.onMessage.addListener(this.onMessage_.bind(this));
  },

  /**
   * Connects to the background script with the given name.
   */
  connect: function(name) {
    this.port_ = chrome.runtime.connect({name: name});
    this.port_.onMessage.addListener(this.onMessage_.bind(this));
  },

  /**
   * Associates a message name with a callback. When a message with the name
   * is received, the callback will be invoked with the message as its arg.
   * Note only the last registered callback will be invoked.
   */
  registerMessage: function(name, callback) {
    this.messageCallbacks_[name] = callback;
  },

  /**
   * Sends a message to the other side of the channel.
   */
  send: function(msg) {
    this.port_.postMessage(msg);
  },

  /**
   * Sends a message to the other side and invokes the callback with
   * the replied object. Useful for message that expects a returned result.
   */
  sendWithCallback: function(msg, callback) {
    var requestId = this.nextInternalRequestId_++;
    this.internalRequestCallbacks_[requestId] = callback;
    this.send({
      name: Channel.INTERNAL_REQUEST_MESSAGE,
      requestId: requestId,
      payload: msg
    });
  },

  /**
   * Invokes message callback using given message.
   * @return {*} The return value of the message callback or null.
   */
  invokeMessageCallbacks_: function(msg) {
    var name = msg.name;
    if (this.messageCallbacks_[name])
      return this.messageCallbacks_[name](msg);

    console.error('Error: Unexpected message, name=' + name);
    return null;
  },

  /**
   * Invoked when a message is received.
   */
  onMessage_: function(msg) {
    var name = msg.name;
    if (name == Channel.INTERNAL_REQUEST_MESSAGE) {
      var payload = msg.payload;
      var result = this.invokeMessageCallbacks_(payload);
      this.send({
        name: Channel.INTERNAL_REPLY_MESSAGE,
        requestId: msg.requestId,
        result: result
      });
    } else if (name == Channel.INTERNAL_REPLY_MESSAGE) {
      var callback = this.internalRequestCallbacks_[msg.requestId];
      delete this.internalRequestCallbacks_[msg.requestId];
      if (callback)
        callback(msg.result);
    } else {
      this.invokeMessageCallbacks_(msg);
    }
  }
};

/**
 * Class factory.
 * @return {Channel}
 */
Channel.create = function() {
  return new Channel();
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var appId = 'hotword_audio_verification';

chrome.app.runtime.onLaunched.addListener(function() {
  // We need to focus the window if it already exists, since it
  // is created as 'hidden'.
  //
  // Note: If we ever launch on another platform, make sure that this works
  // with window managers that support hiding (e.g. Cmd+h on an app window on
  // Mac).
  var appWindow = chrome.app.window.get(appId);
  if (appWindow) {
    appWindow.focus();
    return;
  }

  chrome.app.window.create('main.html', {
    'frame': 'none',
    'resizable': false,
    'hidden': true,
    'id': appId,
    'innerBounds': {
      'width': 784,
      'height': 448
    }
  });
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var appWindow = chrome.app.window.current();

document.addEventListener('DOMContentLoaded', function() {
  chrome.hotwordPrivate.getLocalizedStrings(function(strings) {
    loadTimeData.data = strings;
    i18nTemplate.process(document, loadTimeData);

    var flow = new Flow();
    flow.startFlow();

    var pressFunction = function(e) {
      // Only respond to 'Enter' key presses.
      if (e.type == 'keyup' && e.keyIdentifier != 'Enter')
        return;

      var classes = e.target.classList;
      if (classes.contains('close') || classes.contains('finish-button')) {
        flow.stopTraining();
        appWindow.close();
        e.preventDefault();
      }
      if (classes.contains('retry-button')) {
        flow.handleRetry();
        e.preventDefault();
      }
    };

    $('steps').addEventListener('click', pressFunction);
    $('steps').addEventListener('keyup', pressFunction);

    $('audio-history-agree').addEventListener('click', function(e) {
      flow.enableAudioHistory();
      e.preventDefault();
    });

    $('hotword-start').addEventListener('click', function(e) {
      flow.advanceStep();
      e.preventDefault();
    });

    $('settings-link').addEventListener('click', function(e) {
      chrome.browser.openTab({'url': 'chrome://settings'}, function() {});
      e.preventDefault();
    });
  });
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {

  // Correspond to steps in the hotword opt-in flow.
  /** @const */ var START = 'start-container';
  /** @const */ var AUDIO_HISTORY = 'audio-history-container';
  /** @const */ var SPEECH_TRAINING = 'speech-training-container';
  /** @const */ var FINISH = 'finish-container';

  /**
   * These flows correspond to the three LaunchModes as defined in
   * chrome/browser/search/hotword_service.h and should be kept in sync
   * with them.
   * @const
   */
  var FLOWS = [
    [START, SPEECH_TRAINING, FINISH],
    [START, AUDIO_HISTORY, SPEECH_TRAINING, FINISH],
    [SPEECH_TRAINING, FINISH]
  ];

  /**
   * The launch mode. This enum needs to be kept in sync with that of
   * the same name in hotword_service.h.
   * @enum {number}
   */
  var LaunchMode = {
    HOTWORD_ONLY: 0,
    HOTWORD_AND_AUDIO_HISTORY: 1,
    RETRAIN: 2
  };

  /**
   * The training state.
   * @enum {string}
   */
  var TrainingState = {
    RESET: 'reset',
    TIMEOUT: 'timeout',
    ERROR: 'error',
  };

  /**
   * Class to control the page flow of the always-on hotword and
   * Audio History opt-in process.
   * @constructor
   */
  function Flow() {
    this.currentStepIndex_ = -1;
    this.currentFlow_ = [];

    /**
     * The mode that this app was launched in.
     * @private {LaunchMode}
     */
    this.launchMode_ = LaunchMode.HOTWORD_AND_AUDIO_HISTORY;

    /**
     * Whether this flow is currently in the process of training a voice model.
     * @private {boolean}
     */
    this.training_ = false;

    /**
     * The current training state.
     * @private {?TrainingState}
     */
    this.trainingState_ = null;

    /**
     * Whether an expected hotword trigger has been received, indexed by
     * training step.
     * @private {boolean[]}
     */
    this.hotwordTriggerReceived_ = [];

    /**
     * Prefix of the element ids for the page that is currently training.
     * @private {string}
     */
    this.trainingPagePrefix_ = 'speech-training';

    /**
     * Whether the speaker model for this flow has been finalized.
     * @private {boolean}
     */
    this.speakerModelFinalized_ = false;

    /**
     * ID of the currently active timeout.
     * @private {?number}
     */
    this.timeoutId_ = null;

    /**
     * Listener for the speakerModelSaved event.
     * @private {Function}
     */
    this.speakerModelFinalizedListener_ =
        this.onSpeakerModelFinalized_.bind(this);

    /**
     * Listener for the hotword trigger event.
     * @private {Function}
     */
    this.hotwordTriggerListener_ =
          this.handleHotwordTrigger_.bind(this);

    // Listen for the user locking the screen.
    chrome.idle.onStateChanged.addListener(
        this.handleIdleStateChanged_.bind(this));

    // Listen for hotword settings changes. This used to detect when the user
    // switches to a different profile.
    if (chrome.hotwordPrivate.onEnabledChanged) {
      chrome.hotwordPrivate.onEnabledChanged.addListener(
          this.handleEnabledChanged_.bind(this));
    }
  }

  /**
   * Advances the current step. Begins training if the speech-training
   * page has been reached.
   */
  Flow.prototype.advanceStep = function() {
    this.currentStepIndex_++;
    if (this.currentStepIndex_ < this.currentFlow_.length) {
      if (this.currentFlow_[this.currentStepIndex_] == SPEECH_TRAINING)
        this.startTraining();
      this.showStep_.apply(this);
    }
  };

  /**
   * Gets the appropriate flow and displays its first page.
   */
  Flow.prototype.startFlow = function() {
    if (chrome.hotwordPrivate && chrome.hotwordPrivate.getLaunchState)
      chrome.hotwordPrivate.getLaunchState(this.startFlowForMode_.bind(this));
  };

  /**
   * Starts the training process.
   */
  Flow.prototype.startTraining = function() {
    // Don't start a training session if one already exists.
    if (this.training_)
      return;

    this.training_ = true;

    if (chrome.hotwordPrivate.onHotwordTriggered &&
        !chrome.hotwordPrivate.onHotwordTriggered.hasListener(
            this.hotwordTriggerListener_)) {
      chrome.hotwordPrivate.onHotwordTriggered.addListener(
          this.hotwordTriggerListener_);
    }

    this.waitForHotwordTrigger_(0);
    if (chrome.hotwordPrivate.startTraining)
      chrome.hotwordPrivate.startTraining();
  };

  /**
   * Stops the training process.
   */
  Flow.prototype.stopTraining = function() {
    if (!this.training_)
      return;

    this.training_ = false;
    if (chrome.hotwordPrivate.onHotwordTriggered) {
      chrome.hotwordPrivate.onHotwordTriggered.
          removeListener(this.hotwordTriggerListener_);
    }
    if (chrome.hotwordPrivate.stopTraining)
      chrome.hotwordPrivate.stopTraining();
  };

  /**
   * Attempts to enable audio history for the signed-in account.
   */
  Flow.prototype.enableAudioHistory = function() {
    // Update UI
    $('audio-history-agree').disabled = true;
    $('audio-history-cancel').disabled = true;

    $('audio-history-error').hidden = true;
    $('audio-history-wait').hidden = false;

    if (chrome.hotwordPrivate.setAudioHistoryEnabled) {
      chrome.hotwordPrivate.setAudioHistoryEnabled(
          true, this.onAudioHistoryRequestCompleted_.bind(this));
    }
  };

  // ---- private methods:

  /**
   * Shows an error if the audio history setting was not enabled successfully.
   * @private
   */
  Flow.prototype.handleAudioHistoryError_ = function() {
    $('audio-history-agree').disabled = false;
    $('audio-history-cancel').disabled = false;

    $('audio-history-wait').hidden = true;
    $('audio-history-error').hidden = false;

    // Set a timeout before focusing the Enable button so that screenreaders
    // have time to announce the error first.
    this.setTimeout_(function() {
        $('audio-history-agree').focus();
    }.bind(this), 50);
  };

  /**
   * Callback for when an audio history request completes.
   * @param {chrome.hotwordPrivate.AudioHistoryState} state The audio history
   *     request state.
   * @private
   */
  Flow.prototype.onAudioHistoryRequestCompleted_ = function(state) {
    if (!state.success || !state.enabled) {
      this.handleAudioHistoryError_();
      return;
    }

    this.advanceStep();
  };

  /**
   * Shows an error if the speaker model has not been finalized.
   * @private
   */
  Flow.prototype.handleSpeakerModelFinalizedError_ = function() {
    if (!this.training_)
      return;

    if (this.speakerModelFinalized_)
      return;

    this.updateTrainingState_(TrainingState.ERROR);
    this.stopTraining();
  };

  /**
   * Handles the speaker model finalized event.
   * @private
   */
  Flow.prototype.onSpeakerModelFinalized_ = function() {
    this.speakerModelFinalized_ = true;
    if (chrome.hotwordPrivate.onSpeakerModelSaved) {
      chrome.hotwordPrivate.onSpeakerModelSaved.removeListener(
          this.speakerModelFinalizedListener_);
    }
    this.stopTraining();
    this.setTimeout_(this.finishFlow_.bind(this), 2000);
  };

  /**
   * Completes the training process.
   * @private
   */
  Flow.prototype.finishFlow_ = function() {
    if (chrome.hotwordPrivate.setHotwordAlwaysOnSearchEnabled) {
      chrome.hotwordPrivate.setHotwordAlwaysOnSearchEnabled(true,
          this.advanceStep.bind(this));
    }
  };

  /**
   * Handles a user clicking on the retry button.
   */
  Flow.prototype.handleRetry = function() {
    if (!(this.trainingState_ == TrainingState.TIMEOUT ||
        this.trainingState_ == TrainingState.ERROR))
      return;

    this.startTraining();
    this.updateTrainingState_(TrainingState.RESET);
  };

  // ---- private methods:

  /**
   * Completes the training process.
   * @private
   */
  Flow.prototype.finalizeSpeakerModel_ = function() {
    if (!this.training_)
      return;

    // Listen for the success event from the NaCl module.
    if (chrome.hotwordPrivate.onSpeakerModelSaved &&
        !chrome.hotwordPrivate.onSpeakerModelSaved.hasListener(
            this.speakerModelFinalizedListener_)) {
      chrome.hotwordPrivate.onSpeakerModelSaved.addListener(
          this.speakerModelFinalizedListener_);
    }

    this.speakerModelFinalized_ = false;
    this.setTimeout_(this.handleSpeakerModelFinalizedError_.bind(this), 30000);
    if (chrome.hotwordPrivate.finalizeSpeakerModel)
      chrome.hotwordPrivate.finalizeSpeakerModel();
  };

  /**
   * Returns the current training step.
   * @param {string} curStepClassName The name of the class of the current
   *     training step.
   * @return {Object} The current training step, its index, and an array of
   *     all training steps. Any of these can be undefined.
   * @private
   */
  Flow.prototype.getCurrentTrainingStep_ = function(curStepClassName) {
    var steps =
        $(this.trainingPagePrefix_ + '-training').querySelectorAll('.train');
    var curStep =
        $(this.trainingPagePrefix_ + '-training').querySelector('.listening');

    return {current: curStep,
            index: Array.prototype.indexOf.call(steps, curStep),
            steps: steps};
  };

  /**
   * Updates the training state.
   * @param {TrainingState} state The training state.
   * @private
   */
  Flow.prototype.updateTrainingState_ = function(state) {
    this.trainingState_ = state;
    this.updateErrorUI_();
  };

  /**
   * Waits two minutes and then checks for a training error.
   * @param {number} index The index of the training step.
   * @private
   */
  Flow.prototype.waitForHotwordTrigger_ = function(index) {
    if (!this.training_)
      return;

    this.hotwordTriggerReceived_[index] = false;
    this.setTimeout_(this.handleTrainingTimeout_.bind(this, index), 120000);
  };

  /**
   * Checks for and handles a training error.
   * @param {number} index The index of the training step.
   * @private
   */
  Flow.prototype.handleTrainingTimeout_ = function(index) {
    if (this.hotwordTriggerReceived_[index])
      return;

    this.timeoutTraining_();
  };

  /**
   * Times out training and updates the UI to show a "retry" message, if
   * currently training.
   * @private
   */
  Flow.prototype.timeoutTraining_ = function() {
    if (!this.training_)
      return;

    this.clearTimeout_();
    this.updateTrainingState_(TrainingState.TIMEOUT);
    this.stopTraining();
  };

  /**
   * Sets a timeout. If any timeout is active, clear it.
   * @param {Function} func The function to invoke when the timeout occurs.
   * @param {number} delay Timeout delay in milliseconds.
   * @private
   */
  Flow.prototype.setTimeout_ = function(func, delay) {
    this.clearTimeout_();
    this.timeoutId_ = setTimeout(function() {
      this.timeoutId_ = null;
      func();
    }, delay);
  };

  /**
   * Clears any currently active timeout.
   * @private
   */
  Flow.prototype.clearTimeout_ = function() {
    if (this.timeoutId_ != null) {
      clearTimeout(this.timeoutId_);
      this.timeoutId_ = null;
    }
  };

  /**
   * Updates the training error UI.
   * @private
   */
  Flow.prototype.updateErrorUI_ = function() {
    if (!this.training_)
      return;

    var trainingSteps = this.getCurrentTrainingStep_('listening');
    var steps = trainingSteps.steps;

    $(this.trainingPagePrefix_ + '-toast').hidden =
        this.trainingState_ != TrainingState.TIMEOUT;
    if (this.trainingState_ == TrainingState.RESET) {
      // We reset the training to begin at the first step.
      // The first step is reset to 'listening', while the rest
      // are reset to 'not-started'.
      var prompt = loadTimeData.getString('trainingFirstPrompt');
      for (var i = 0; i < steps.length; ++i) {
        steps[i].classList.remove('recorded');
        if (i == 0) {
          steps[i].classList.remove('not-started');
          steps[i].classList.add('listening');
        } else {
          steps[i].classList.add('not-started');
          if (i == steps.length - 1)
            prompt = loadTimeData.getString('trainingLastPrompt');
          else
            prompt = loadTimeData.getString('trainingMiddlePrompt');
        }
        steps[i].querySelector('.text').textContent = prompt;
      }

      // Reset the buttonbar.
      $(this.trainingPagePrefix_ + '-processing').hidden = true;
      $(this.trainingPagePrefix_ + '-wait').hidden = false;
      $(this.trainingPagePrefix_ + '-error').hidden = true;
      $(this.trainingPagePrefix_ + '-retry').hidden = true;
    } else if (this.trainingState_ == TrainingState.TIMEOUT) {
      var curStep = trainingSteps.current;
      if (curStep) {
        curStep.classList.remove('listening');
        curStep.classList.add('not-started');
      }

      // Set a timeout before focusing the Retry button so that screenreaders
      // have time to announce the timeout first.
      this.setTimeout_(function() {
        $(this.trainingPagePrefix_ + '-toast').children[1].focus();
      }.bind(this), 50);
    } else if (this.trainingState_ == TrainingState.ERROR) {
      // Update the buttonbar.
      $(this.trainingPagePrefix_ + '-wait').hidden = true;
      $(this.trainingPagePrefix_ + '-error').hidden = false;
      $(this.trainingPagePrefix_ + '-retry').hidden = false;
      $(this.trainingPagePrefix_ + '-processing').hidden = false;

      // Set a timeout before focusing the Retry button so that screenreaders
      // have time to announce the error first.
      this.setTimeout_(function() {
        $(this.trainingPagePrefix_ + '-retry').children[0].focus();
      }.bind(this), 50);
    }
  };

  /**
   * Handles a hotword trigger event and updates the training UI.
   * @private
   */
  Flow.prototype.handleHotwordTrigger_ = function() {
    var trainingSteps = this.getCurrentTrainingStep_('listening');

    if (!trainingSteps.current)
      return;

    var index = trainingSteps.index;
    this.hotwordTriggerReceived_[index] = true;

    trainingSteps.current.querySelector('.text').textContent =
        loadTimeData.getString('trainingRecorded');
    trainingSteps.current.classList.remove('listening');
    trainingSteps.current.classList.add('recorded');

    if (trainingSteps.steps[index + 1]) {
      trainingSteps.steps[index + 1].classList.remove('not-started');
      trainingSteps.steps[index + 1].classList.add('listening');
      this.waitForHotwordTrigger_(index + 1);
      return;
    }

    // Only the last step makes it here.
    var buttonElem = $(this.trainingPagePrefix_ + '-processing').hidden = false;
    this.finalizeSpeakerModel_();
  };

  /**
   * Handles a chrome.idle.onStateChanged event and times out the training if
   * the state is "locked".
   * @param {!string} state State, one of "active", "idle", or "locked".
   * @private
   */
  Flow.prototype.handleIdleStateChanged_ = function(state) {
    if (state == 'locked')
      this.timeoutTraining_();
  };

  /**
   * Handles a chrome.hotwordPrivate.onEnabledChanged event and times out
   * training if the user is no longer the active user (user switches profiles).
   * @private
   */
  Flow.prototype.handleEnabledChanged_ = function() {
    if (chrome.hotwordPrivate.getStatus) {
      chrome.hotwordPrivate.getStatus(function(status) {
        if (status.userIsActive)
          return;

        this.timeoutTraining_();
      }.bind(this));
    }
  };

  /**
   * Gets and starts the appropriate flow for the launch mode.
   * @param {chrome.hotwordPrivate.LaunchState} state Launch state of the
   *     Hotword Audio Verification App.
   * @private
   */
  Flow.prototype.startFlowForMode_ = function(state) {
    this.launchMode_ = state.launchMode;
    assert(state.launchMode >= 0 && state.launchMode < FLOWS.length,
           'Invalid Launch Mode.');
    this.currentFlow_ = FLOWS[state.launchMode];
    if (state.launchMode == LaunchMode.HOTWORD_ONLY) {
      $('intro-description-audio-history-enabled').hidden = false;
    } else if (state.launchMode == LaunchMode.HOTWORD_AND_AUDIO_HISTORY) {
      $('intro-description').hidden = false;
    }

    this.advanceStep();
  };

  /**
   * Displays the current step. If the current step is not the first step,
   * also hides the previous step. Focuses the current step's first button.
   * @private
   */
  Flow.prototype.showStep_ = function() {
    var currentStepId = this.currentFlow_[this.currentStepIndex_];
    var currentStep = document.getElementById(currentStepId);
    currentStep.hidden = false;

    cr.ui.setInitialFocus(currentStep);

    var previousStep = null;
    if (this.currentStepIndex_ > 0)
      previousStep = this.currentFlow_[this.currentStepIndex_ - 1];

    if (previousStep)
      document.getElementById(previousStep).hidden = true;

    chrome.app.window.current().show();
  };

  window.Flow = Flow;
})();
<div id="start-container" hidden>
  <div class="container">
    <span class="close" tabindex="0" role="button"
        i18n-values="aria-label:close"></span>
    <div class="intro-image"></div>
    <div class="intro-text">
      <h1 i18n-content="introTitle"></h1>
      <h2 i18n-content="introSubtitle"></h2>
      <h3 id="intro-description" i18n-content="introDescription" hidden></h3>
      <h3 id="intro-description-audio-history-enabled"
          i18n-content="introDescriptionAudioHistoryEnabled" hidden></h3>
    </div>
    <div class="buttonbar">
      <button id="hotword-start" i18n-content="introStart"></button>
    </div>
  </div>
</div>
<div id="audio-history-container" hidden>
  <div class="container">
    <span class="close" tabindex="0" role="button"
        i18n-values="aria-label:close"></span>
    <div class="header">
      <h1 i18n-content="audioHistoryTitle" aria-live="polite"></h1>
    </div>
    <div class="content">
      <div i18n-content="audioHistoryDescription1"></div>
      <div class="v-spacing"></div>
      <div i18n-values=".innerHTML:audioHistoryDescription2"></div>
      <div class="v-spacing"></div>
      <div i18n-values=".innerHTML:audioHistoryDescription3"></div>
    </div>
    <div class="buttonbar">
      <div class="right">
        <div>
          <button id="audio-history-agree" i18n-content="audioHistoryAgree"
              class="primary">
          </button>
          <button id="audio-history-cancel" i18n-content="cancel"
              class="finish-button">
          </button>
        </div>
      </div>
      <div class="left">
        <div id="audio-history-wait" class="message wait" hidden>
          <span class="icon"></span>
          <span i18n-content="audioHistoryWait" class="text"></span>
        </div>
        <div id="audio-history-error" class="message error" role="alert" hidden>
          <span class="icon"></span>
          <span i18n-content="error" class="text"></span>
        </div>
      </div>
    </div>
  </div>
</div>
<div id="speech-training-container" hidden>
  <div class="container">
    <span class="close" tabindex="0" role="button"
        i18n-values="aria-label:close"></span>
    <div class="header">
      <h1 i18n-content="trainingTitle" aria-live="polite"></h1>
    </div>
    <div id="speech-training-training" class="content">
      <div class="col-2">
        <div i18n-content="trainingDescription"></div>
        <br>
        <h3 i18n-content="trainingSpeak"></h3>
      </div>
      <div class="col-spacing"></div>
      <div class="col-2">
        <div class="train listening">
          <span class="icon"></span>
          <span i18n-content="trainingFirstPrompt"
              class="text"></span>
        </div>
        <div class="train not-started">
          <span class="icon"></span>
          <span i18n-content="trainingMiddlePrompt"
              class="text"></span>
        </div>
        <div class="train not-started">
          <span class="icon"></span>
          <span i18n-content="trainingLastPrompt"
              class="text"></span>
        </div>
      </div>
    </div>
    <div id="speech-training-toast" class="toast" hidden>
      <span i18n-content="trainingTimeout" class="message" role="alert"></span>
      <button i18n-content="trainingRetry" class="retry-button" tabindex="0">
      </button>
    </div>
    <div id="speech-training-processing" class="buttonbar" hidden>
      <div id="speech-training-retry" class="right" hidden>
        <button i18n-content="trainingRetry" class="primary retry-button">
        </button>
      </div>
      <div class="left">
        <div id="speech-training-wait" class="message wait">
          <span class="icon"></span>
          <span i18n-content="finishedWait" class="text"></span>
        </div>
        <div id="speech-training-error" class="message error"
            role="alert" hidden>
          <span class="icon"></span>
          <span i18n-content="error" class="text"></span>
        </div>
      </div>
    </div>
  </div>
</div>
<div id="finish-container" hidden>
  <div class="container">
    <span class="close" tabindex="0" role="button"
        i18n-values="aria-label:close"></span>
    <div class="header">
      <h1 i18n-content="finishedTitle" aria-live="polite"></h1>
    </div>
    <div class="content">
      <h3 i18n-content="finishedListIntro"></h3>
      <div class="check">
        <span class="icon"></span>
        <span i18n-content="finishedListItem1" class="text">
        </span>
      </div>
      <div class="check">
        <span class="icon"></span>
        <span i18n-content="finishedListItem2" class="text">
        </span>
      </div>
      <div i18n-values=".innerHTML:finishedSettings"></div>
      <div i18n-values=".innerHTML:finishedAudioHistory"></div>
    </div>
    <div class="buttonbar">
      <div class="right">
        <button id="done-button" i18n-content="finish"
            class="primary finish-button"></button>
      </div>
    </div>
  </div>
</div>
/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

/* TODO(xdai): Remove hard-coded font-family for 'Roboto'. */

* {
  box-sizing: border-box;
  color: rgba(0, 0, 0, .54);
  font-family: Roboto, 'Noto Sans', sans-serif;
  font-size: 13px;
  margin: 0;
  padding: 0;
}

#start-container * {
  color: #fff;
}

#start-container h2 {
  font-size: 15px;
  font-weight: normal;
  line-height: 24px;
  margin-top: 16px;
}

#start-container h3 {
  font-weight: normal;
  margin: 42px 16px 24px 16px;
}

#start-container div.container {
  background: rgb(66, 133, 244);
}

div.intro-image {
  background: -webkit-image-set(
      url(../images/intro-1x.png) 1x,
      url(../images/intro-2x.png) 2x)
      no-repeat;
  height: 152px;
  left: 24px;
  position: absolute;
  top: 122px;
  width: 304px;
}

div.intro-text {
  left: 328px;
  position: absolute;
  text-align: center;
  top: 116px;
  width: 432px;
}

#start-container div.buttonbar {
  background-color: rgb(51, 103, 214);
  height: 56px;
  padding: 0;
  text-align: center;
}

#start-container .buttonbar button {
  height: 100%;
  margin: 0;
  padding: 0 8px;
  width: 100%;
}

a {
  -webkit-app-region: no-drag;
  color: rgb(51, 103, 214);
  text-decoration: none;
}

button {
  -webkit-app-region: no-drag;
}

body {
  -webkit-app-region: drag;
  background: #ddd;
}

h1 {
  font-size: 20px;
  font-weight: normal;
  line-height: 32px;
}

h3 {
  font-size: 13px;
  line-height: 20px;
}

div.container {
  background: #fff;
  height: 448px;
  position: relative;
  width: 784px;
}

div.header {
  background: -webkit-image-set(
      url(../images/gradient-1x.png) 1x,
      url(../images/gradient-2x.png) 2x)
      no-repeat;
  height: 128px;
  padding: 70px 42px 0 42px;
}

div.header h1 {
  color: #fff;
}

div.content {
  height: 264px;
  line-height: 20px;
  padding: 32px 42px 0 42px;
}

div.content h3 {
  color: rgba(0, 0, 0, .87);
  margin-bottom: 16px;
}

div.col-2 {
  color: rgba(0, 0, 0, .54);
  float: left;
  width: 320px;
}

div.col-spacing {
  float: left;
  height: 216px;
  width: 60px;
}

div.v-spacing {
  height: 8px;
}

a[is='action-link'] {
  display: inline-block;
  font-size: 14px;
  margin-top: 22px;
  text-decoration: none;
  text-transform: uppercase;
}

.train {
  clear: both;
  line-height: 18px;
  margin-bottom: 24px;
}

.train .icon {
  display: inline-block;
  height: 18px;
  margin-right: 8px;
  vertical-align: top;
  width: 18px;
}

.train .text {
  color: rgba(0, 0, 0, .54);
  display: inline-block;
  line-height: 13px;
  padding-top: 3px;
  vertical-align: top;
}

.train.recorded .text {
  color: rgba(66, 133, 244, 1);
}

@-webkit-keyframes rotate {
  from { -webkit-transform: rotate(0); }
  to { -webkit-transform: rotate(359deg); }
}

.train.listening .icon {
  -webkit-animation: rotate 2s linear infinite;
  background: -webkit-image-set(
      url(../images/placeholder-loader-1x.png) 1x,
      url(../images/placeholder-loader-2x.png) 2x)
      no-repeat;
}

.train.not-started .icon {
  background: -webkit-image-set(
      url(../images/ic-check-gray-1x.png) 1x,
      url(../images/ic-check-gray-2x.png) 2x)
      no-repeat;
}

.train.recorded .icon {
  background: -webkit-image-set(
      url(../images/ic-check-blue-1x.png) 1x,
      url(../images/ic-check-blue-2x.png) 2x)
      no-repeat;
}

.check {
  clear: both;
  height: 18px;
  margin-bottom: 24px;
}

.check .icon {
  background: -webkit-image-set(
      url(../images/ic-check-blue-1x.png) 1x,
      url(../images/ic-check-blue-2x.png) 2x)
      no-repeat;
  display: inline-block;
  height: 18px;
  margin-right: 8px;
  vertical-align: top;
  width: 18px;
}

.check .text {
  color: rgba(0, 0, 0, .54);
  display: inline-block;
  height: 18px;
  line-height: 18px;
  padding-top: 2px;
  vertical-align: top;
}

div.buttonbar {
  background-color: rgba(236,239, 241, 1);
  bottom: 0;
  height: 56px;
  padding: 12px;
  position: absolute;
  width: 100%;
}

.buttonbar button {
  background: none;
  border: none;
  display: inline-block;
  font-weight: 700;
  height: 32px;
  line-height: 32px;
  margin-left: 8px;
  min-width: 56px;
  padding: 1px 8px 0 8px;
  text-transform: uppercase;
}

.buttonbar button:disabled {
  opacity: .5;
}

.buttonbar button.grayed-out {
  color: rgba(0, 0, 0, .28);
  text-transform: none;
}

.buttonbar button.primary {
  color: rgb(51, 103, 214);
}

.buttonbar .left {
  float: left;
  text-align: left;
}

.buttonbar .left button:first-child {
  margin-left: 0;
}

.buttonbar .right {
  float: right;
  text-align: right;
}

.buttonbar .message {
  margin: 7px 0 0 2px;
}

.buttonbar .message .icon {
  display: inline-block;
  height: 18px;
  margin-right: 8px;
  vertical-align: top;
  width: 18px;
}

.buttonbar .message.wait .icon {
  -webkit-animation: rotate 2s linear infinite;
  background: -webkit-image-set(
      url(../images/placeholder-loader-1x.png) 1x,
      url(../images/placeholder-loader-2x.png) 2x)
      no-repeat;
}

.buttonbar .message.error .icon {
  background: -webkit-image-set(
      url(../images/ic-error-1x.png) 1x,
      url(../images/ic-error-2x.png) 2x)
      no-repeat;
}

.buttonbar .message .text {
  color: rgba(0, 0, 0, .54);
  display: inline-block;
  line-height: 18px;
  padding-top: 2px;
  vertical-align: top;
}

.buttonbar .message.error .text {
  color: rgb(213, 0, 0);
}

.close {
  -webkit-app-region: no-drag;
  background: -webkit-image-set(
      url(../images/ic-x-white-1x.png) 1x,
      url(../images/ic-x-white-2x.png) 2x)
      center center no-repeat;
  border: none;
  float: right;
  height: 42px;
  opacity: .54;
  width: 42px;
}

.close:hover {
  opacity: 1;
}

.toast {
  background-color: rgb(38, 50, 56);
  bottom: 0;
  height: 52px;
  padding: 10px 12px 0 42px;
  position: absolute;
  width: 100%;
}

.toast .message {
  color: #fff;
  float: left;
  padding: 9px 0 0 0;
}

.toast button {
  background: none;
  border: none;
  color: rgb(58, 218, 255);
  float: right;
  height: 32px;
  margin-left: 18px;
  min-width: 56px;
  padding: 0 8px 0 8px;
  text-transform: uppercase;
}
‰PNG
