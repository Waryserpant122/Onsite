ame, url, isdir,
    size, size_string, date_modified, date_modified_string) {
  if (name == ".")
    return;

  var root = document.location.pathname;
  if (root.substr(-1) !== "/")
    root += "/";

  var tbody = document.getElementById("tbody");
  var row = document.createElement("tr");
  var file_cell = document.createElement("td");
  var link = document.createElement("a");

  link.className = isdir ? "icon dir" : "icon file";

  if (name == "..") {
    link.href = root + "..";
    link.innerText = document.getElementById("parentDirText").innerText;
    link.className = "icon up";
    size = 0;
    size_string = "";
    date_modified = 0;
    date_modified_string = "";
  } else {
    if (isdir) {
      name = name + "/";
      url = url + "/";
      size = 0;
      size_string = "";
    } else {
      link.draggable = "true";
      link.addEventListener("dragstart", onDragStart, false);
    }
    link.innerText = name;
    link.href = root + url;
  }
  file_cell.dataset.value = name;
  file_cell.appendChild(link);

  row.appendChild(file_cell);
  row.appendChild(createCell(size, size_string));
  row.appendChild(createCell(date_modified, date_modified_string));

  tbody.appendChild(row);
}

function onDragStart(e) {
  var el = e.srcElement;
  var name = el.innerText.replace(":", "");
  var download_url_data = "application/octet-stream:" + name + ":" + el.href;
  e.dataTransfer.setData("DownloadURL", download_url_data);
  e.dataTransfer.effectAllowed = "copy";
}

function createCell(value, text) {
  var cell = document.createElement("td");
  cell.setAttribute("class", "detailsColumn");
  cell.dataset.value = value;
  cell.innerText = text;
  return cell;
}

function start(location) {
  var header = document.getElementById("header");
  header.innerText = header.innerText.replace("LOCATION", location);

  document.getElementById("title").innerText = header.innerText;
}

function onListingParsingError() {
  var box = document.getElementById("listingParsingErrorBox");
  box.innerHTML = box.innerHTML.replace("LOCATION", encodeURI(document.location)
      + "?raw");
  box.style.display = "block";
}

function sortTable(column) {
  var theader = document.getElementById("theader");
  var oldOrder = theader.cells[column].dataset.order || '1';
  oldOrder = parseInt(oldOrder, 10)
  var newOrder = 0 - oldOrder;
  theader.cells[column].dataset.order = newOrder;

  var tbody = document.getElementById("tbody");
  var rows = tbody.rows;
  var list = [], i;
  for (i = 0; i < rows.length; i++) {
    list.push(rows[i]);
  }

  list.sort(function(row1, row2) {
    var a = row1.cells[column].dataset.value;
    var b = row2.cells[column].dataset.value;
    if (column) {
      a = parseInt(a, 10);
      b = parseInt(b, 10);
      return a > b ? newOrder : a < b ? oldOrder : 0;
    }

    // Column 0 is text.
    // Also the parent directory should always be sorted at one of the ends.
    if (b == ".." | a > b) {
      return newOrder;
    } else if (a == ".." | a < b) {
      return oldOrder;
    } else {
      return 0;
    }
  });

  // Appending an existing child again just moves it.
  for (i = 0; i < list.length; i++) {
    tbody.appendChild(list[i]);
  }
}
</script>

<style>

  h1 {
    border-bottom: 1px solid #c0c0c0;
    margin-bottom: 10px;
    padding-bottom: 10px;
    white-space: nowrap;
  }

  table {
    border-collapse: collapse;
  }

  th {
    cursor: pointer;
  }

  td.detailsColumn {
    -webkit-padding-start: 2em;
    text-align: end;
    white-space: nowrap;
  }

  a.icon {
    -webkit-padding-start: 1.5em;
    text-decoration: none;
  }

  a.icon:hover {
    text-decoration: underline;
  }

  a.file {
    background : url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAIAAACQkWg2AAAABnRSTlMAAAAAAABupgeRAAABHUlEQVR42o2RMW7DIBiF3498iHRJD5JKHurL+CRVBp+i2T16tTynF2gO0KSb5ZrBBl4HHDBuK/WXACH4eO9/CAAAbdvijzLGNE1TVZXfZuHg6XCAQESAZXbOKaXO57eiKG6ft9PrKQIkCQqFoIiQFBGlFIB5nvM8t9aOX2Nd18oDzjnPgCDpn/BH4zh2XZdlWVmWiUK4IgCBoFMUz9eP6zRN75cLgEQhcmTQIbl72O0f9865qLAAsURAAgKBJKEtgLXWvyjLuFsThCSstb8rBCaAQhDYWgIZ7myM+TUBjDHrHlZcbMYYk34cN0YSLcgS+wL0fe9TXDMbY33fR2AYBvyQ8L0Gk8MwREBrTfKe4TpTzwhArXWi8HI84h/1DfwI5mhxJamFAAAAAElFTkSuQmCC ") left top no-repeat;
  }

  a.dir {
    background : url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAd5JREFUeNqMU79rFUEQ/vbuodFEEkzAImBpkUabFP4ldpaJhZXYm/RiZWsv/hkWFglBUyTIgyAIIfgIRjHv3r39MePM7N3LcbxAFvZ2b2bn22/mm3XMjF+HL3YW7q28YSIw8mBKoBihhhgCsoORot9d3/ywg3YowMXwNde/PzGnk2vn6PitrT+/PGeNaecg4+qNY3D43vy16A5wDDd4Aqg/ngmrjl/GoN0U5V1QquHQG3q+TPDVhVwyBffcmQGJmSVfyZk7R3SngI4JKfwDJ2+05zIg8gbiereTZRHhJ5KCMOwDFLjhoBTn2g0ghagfKeIYJDPFyibJVBtTREwq60SpYvh5++PpwatHsxSm9QRLSQpEVSd7/TYJUb49TX7gztpjjEffnoVw66+Ytovs14Yp7HaKmUXeX9rKUoMoLNW3srqI5fWn8JejrVkK0QcrkFLOgS39yoKUQe292WJ1guUHG8K2o8K00oO1BTvXoW4yasclUTgZYJY9aFNfAThX5CZRmczAV52oAPoupHhWRIUUAOoyUIlYVaAa/VbLbyiZUiyFbjQFNwiZQSGl4IDy9sO5Wrty0QLKhdZPxmgGcDo8ejn+c/6eiK9poz15Kw7Dr/vN/z6W7q++091/AQYA5mZ8GYJ9K0AAAAAASUVORK5CYII= ") left top no-repeat;
  }

  a.up {
    background : url("data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAmlJREFUeNpsU0toU0EUPfPysx/tTxuDH9SCWhUDooIbd7oRUUTMouqi2iIoCO6lceHWhegy4EJFinWjrlQUpVm0IIoFpVDEIthm0dpikpf3ZuZ6Z94nrXhhMjM3c8895977BBHB2PznK8WPtDgyWH5q77cPH8PpdXuhpQT4ifR9u5sfJb1bmw6VivahATDrxcRZ2njfoaMv+2j7mLDn93MPiNRMvGbL18L9IpF8h9/TN+EYkMffSiOXJ5+hkD+PdqcLpICWHOHc2CC+LEyA/K+cKQMnlQHJX8wqYG3MAJy88Wa4OLDvEqAEOpJd0LxHIMdHBziowSwVlF8D6QaicK01krw/JynwcKoEwZczewroTvZirlKJs5CqQ5CG8pb57FnJUA0LYCXMX5fibd+p8LWDDemcPZbzQyjvH+Ki1TlIciElA7ghwLKV4kRZstt2sANWRjYTAGzuP2hXZFpJ/GsxgGJ0ox1aoFWsDXyyxqCs26+ydmagFN/rRjymJ1898bzGzmQE0HCZpmk5A0RFIv8Pn0WYPsiu6t/Rsj6PauVTwffTSzGAGZhUG2F06hEc9ibS7OPMNp6ErYFlKavo7MkhmTqCxZ/jwzGA9Hx82H2BZSw1NTN9Gx8ycHkajU/7M+jInsDC7DiaEmo1bNl1AMr9ASFgqVu9MCTIzoGUimXVAnnaN0PdBBDCCYbEtMk6wkpQwIG0sn0PQIUF4GsTwLSIFKNqF6DVrQq+IWVrQDxAYQC/1SsYOI4pOxKZrfifiUSbDUisif7XlpGIPufXd/uvdvZm760M0no1FZcnrzUdjw7au3vu/BVgAFLXeuTxhTXVAAAAAElFTkSuQmCC ") left top no-repeat;
  }

  html[dir=rtl] a {
    background-position-x: right;
  }

  #listingParsingErrorBox {
    border: 1px solid black;
    background: #fae691;
    padding: 10px;
    display: none;
  }
</style>

<title id="title"></title>

</head>

<body>

<div id="listingParsingErrorBox" i18n-values=".innerHTML:listingParsingErrorBoxText"></div>

<span id="parentDirText" style="display:none" i18n-content="parentDirText"></span>

<h1 id="header" i18n-content="header"></h1>

<table>
  <thead>
    <tr class="header" id="theader">
      <th i18n-content="headerName" onclick="javascript:sortTable(0);"></th>
      <th class="detailsColumn" i18n-content="headerSize" onclick="javascript:sortTable(1);"></th>
      <th class="detailsColumn" i18n-content="headerDateModified" onclick="javascript:sortTable(2);"></th>
    </tr>
  </thead>
  <tbody id="tbody">
  </tbody>
</table>

</body>

</html>
<!doctype html>
<html>
<!--
Copyright (c) 2013 The Chromium Authors. All rights reserved.
Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.
-->
<head>
  <meta charset="utf-8">
  <title>Accessibility</title>
  <link rel="stylesheet" href="chrome://resources/css/chrome_shared.css">
  <style>/*
 * Copyright (c) 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

body {
  font-family: Arial, sans-serif;
  font-size: 12px;
  margin: 10px;
  min-width: 47em;
  padding-bottom: 65px;
}

img {
  float: left;
  height: 16px;
  padding-right: 5px;
  width: 16px;
}

.row {
  border-bottom: 1px solid #A0A0A0;
  padding: 5px;
}

.url {
  color: #A0A0A0;
}

</style>
  <script src="chrome://resources/js/cr.js"></script>
  <script src="chrome://resources/js/load_time_data.js"></script>
  <script src="chrome://resources/js/action_link.js"></script>
  <script src="chrome://resources/js/util.js"></script>
  <script src="strings.js"></script>
  <script src="accessibility.js"></script>
</head>
<body>
  <h1>Accessibility</h1>
  <div id="global" class="row">Global accessibility mode:
    <a is="action-link" role="button" id="toggle_global" aria-labelledby="global"></a>
  </div>
  <div id="internal" class="row">Show internal accessibility tree instead of native:
    <a is="action-link" role="button" id="toggle_internal" aria-labelledby="internal"></a>
  </div>
  <div id="pages" class="list"></div>
  <script src="chrome://resources/js/i18n_template.js"></script>
</body>
</html>
/*
 * Copyright (c) 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

body {
  font-family: Arial, sans-serif;
  font-size: 12px;
  margin: 10px;
  min-width: 47em;
  padding-bottom: 65px;
}

img {
  float: left;
  height: 16px;
  padding-right: 5px;
  width: 16px;
}

.row {
  border-bottom: 1px solid #A0A0A0;
  padding: 5px;
}

.url {
  color: #A0A0A0;
}

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('accessibility', function() {
  'use strict';

  // Keep in sync with view_message_enums.h
  var AccessibilityModeFlag = {
    Platform: 1 << 0,
    FullTree: 1 << 1
  }

  var AccessibilityMode = {
    Off: 0,
    Complete:
        AccessibilityModeFlag.Platform | AccessibilityModeFlag.FullTree,
    EditableTextOnly: AccessibilityModeFlag.Platform,
    TreeOnly: AccessibilityModeFlag.FullTree
  }

  function isAccessibilityComplete(mode) {
    return ((mode & AccessibilityMode.Complete) == AccessibilityMode.Complete);
  }

  function requestData() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', 'targets-data.json', false);
    xhr.send(null);
    if (xhr.status === 200) {
      console.log(xhr.responseText);
      return JSON.parse(xhr.responseText);
    }
    return [];
  }

  // TODO(aboxhall): add a mechanism to request individual and global a11y
  // mode, xhr them on toggle... or just re-requestData and be smarter about
  // ID-ing rows?

  function toggleAccessibility(data, element) {
    chrome.send('toggleAccessibility',
                [String(data.processId), String(data.routeId)]);
    var a11y_was_on = (element.textContent.match(/on/) != null);
    element.textContent = ' accessibility ' + (a11y_was_on ? ' off' : ' on');
    var row = element.parentElement;
    if (a11y_was_on) {
      while (row.lastChild != element)
        row.removeChild(row.lastChild);
    } else {
      row.appendChild(document.createTextNode(' | '));
      row.appendChild(createShowAccessibilityTreeElement(data, row, false));
    }
  }

  function requestAccessibilityTree(data, element) {
    chrome.send('requestAccessibilityTree',
                [String(data.processId), String(data.routeId)]);
  }

  function toggleGlobalAccessibility() {
    chrome.send('toggleGlobalAccessibility');
    document.location.reload(); // FIXME see TODO above
  }

  function toggleInternalTree() {
    chrome.send('toggleInternalTree');
    document.location.reload(); // FIXME see TODO above
  }

  function initialize() {
    console.log('initialize');
    var data = requestData();

    addGlobalAccessibilityModeToggle(data['global_a11y_mode']);

    addInternalTreeToggle(data['global_internal_tree_mode']);

    $('pages').textContent = '';

    var list = data['list'];
    for (var i = 0; i < list.length; i++) {
      addToPagesList(list[i]);
    }
  }

  function addGlobalAccessibilityModeToggle(global_a11y_mode) {
    var full_a11y_on = isAccessibilityComplete(global_a11y_mode);
    $('toggle_global').textContent = (full_a11y_on ? 'on' : 'off');
    $('toggle_global').setAttribute('aria-pressed',
                                    (full_a11y_on ? 'true' : 'false'));
    $('toggle_global').addEventListener('click',
                                        toggleGlobalAccessibility);
  }

  function addInternalTreeToggle(global_internal_tree_mode) {
    var on = global_internal_tree_mode;
    $('toggle_internal').textContent = (on ? 'on' : 'off');
    $('toggle_internal').setAttribute('aria-pressed',
                                      (on ? 'true' : 'false'));
    $('toggle_internal').addEventListener('click',
                                          toggleInternalTree);
  }

  function addToPagesList(data) {
    // TODO: iterate through data and pages rows instead
    var id = data['processId'] + '.' + data['routeId'];
    var row = document.createElement('div');
    row.className = 'row';
    row.id = id;
    formatRow(row, data);

    row.processId = data.processId;
    row.routeId = data.routeId;

    var list = $('pages');
    list.appendChild(row);
  }

  function formatRow(row, data) {
    if (!('url' in data)) {
      if ('error' in data) {
        row.appendChild(createErrorMessageElement(data, row));
        return;
      }
    }
    var properties = ['favicon_url', 'name', 'url'];
    for (var j = 0; j < properties.length; j++)
      row.appendChild(formatValue(data, properties[j]));

    row.appendChild(createToggleAccessibilityElement(data));
    if (isAccessibilityComplete(data['a11y_mode'])) {
      row.appendChild(document.createTextNode(' | '));
      if ('tree' in data) {
        row.appendChild(createShowAccessibilityTreeElement(data, row, true));
        row.appendChild(document.createTextNode(' | '));
        row.appendChild(createHideAccessibilityTreeElement(row.id));
        row.appendChild(createAccessibilityTreeElement(data));
      }
      else {
        row.appendChild(createShowAccessibilityTreeElement(data, row, false));
        if ('error' in data)
          row.appendChild(createErrorMessageElement(data, row));
      }
    }
  }

  function formatValue(data, property) {
    var value = data[property];

    if (property == 'favicon_url') {
      var faviconElement = document.createElement('img');
      if (value)
        faviconElement.src = value;
      faviconElement.alt = "";
      return faviconElement;
    }

    var text = value ? String(value) : '';
    if (text.length > 100)
      text = text.substring(0, 100) + '\u2026';  // ellipsis

    var span = document.createElement('span');
    span.textContent = ' ' + text + ' ';
    span.className = property;
    return span;
  }

  function createToggleAccessibilityElement(data) {
    var link = document.createElement('a', 'action-link');
    link.setAttribute('role', 'button');
    var full_a11y_on = isAccessibilityComplete(data['a11y_mode']);
    link.textContent = 'accessibility ' + (full_a11y_on ? 'on' : 'off');
    link.setAttribute('aria-pressed', (full_a11y_on ? 'true' : 'false'));
    link.addEventListener('click',
                          toggleAccessibility.bind(this, data, link));
    return link;
  }

  function createShowAccessibilityTreeElement(data, row, opt_refresh) {
    var link = document.createElement('a', 'action-link');
    link.setAttribute('role', 'button');
    if (opt_refresh)
      link.textContent = 'refresh accessibility tree';
    else
      link.textContent = 'show accessibility tree';
    link.id = row.id + ':showTree';
    link.addEventListener('click',
                          requestAccessibilityTree.bind(this, data, link));
    return link;
  }

  function createHideAccessibilityTreeElement(id) {
    var link = document.createElement('a', 'action-link');
    link.setAttribute('role', 'button');
    link.textContent = 'hide accessibility tree';
    link.addEventListener('click',
                          function() {
        $(id + ':showTree').textContent = 'show accessibility tree';
        var existingTreeElements = $(id).getElementsByTagName('pre');
        for (var i = 0; i < existingTreeElements.length; i++)
          $(id).removeChild(existingTreeElements[i]);
        var row = $(id);
        while (row.lastChild != $(id + ':showTree'))
          row.removeChild(row.lastChild);
    });
    return link;
  }

  function createErrorMessageElement(data) {
    var errorMessageElement = document.createElement('div');
    var errorMessage = data.error;
    errorMessageElement.innerHTML = errorMessage + '&nbsp;';
    var closeLink = document.createElement('a');
    closeLink.href='#';
    closeLink.textContent = '[close]';
    closeLink.addEventListener('click', function() {
        var parentElement = errorMessageElement.parentElement;
        parentElement.removeChild(errorMessageElement);
        if (parentElement.childElementCount == 0)
          parentElement.parentElement.removeChild(parentElement);
    });
    errorMessageElement.appendChild(closeLink);
    return errorMessageElement;
  }

  function showTree(data) {
    var id = data.processId + '.' + data.routeId;
    var row = $(id);
    if (!row)
      return;

    row.textContent = '';
    formatRow(row, data);
  }

  function createAccessibilityTreeElement(data) {
    var treeElement = document.createElement('pre');
    var tree = data.tree;
    treeElement.textContent = tree;
    return treeElement;
  }

  return {
    initialize: initialize,
    showTree: showTree
  };
});

document.addEventListener('DOMContentLoaded', accessibility.initialize);
<!--
  Copyright 2015 The Chromium Authors. All rights reserved.
  Use of this source code is governed by a BSD-style license that can be
  found in the LICENSE file.
-->
<!DOCTYPE html>
<html i18n-values="dir:textdirection;lang:language">
  <head>
    <meta charset="utf-8">
    <title>AppCache</title>
    <link rel="stylesheet" href="chrome://resources/css/tabs.css">
    <link rel="stylesheet" href="chrome://resources/css/widgets.css">
    <style>/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.appcache-summary {
  background-color: rgb(235, 239, 249);
  border-top: 1px solid rgb(156, 194, 239);
  margin-bottom: 6px;
  margin-top: 12px;
  padding: 3px;
  font-weight: bold;
}

.appcache-item {
  margin-bottom: 15px;
  margin-top: 6px;
  position: relative;
}

.appcache-url {
  color: rgb(85, 102, 221);
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  padding-bottom: 1px;
  padding-top: 4px;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.appcache-info-template-table {
  table-layout: fixed;
}

.appcache-info-properties {
  word-wrap: break-word;
}

.appcache-info-url {
  color: rgb(85, 102, 221);
  display: inline-block;
}

.appcache-manifest-url {
  color: rgb(85, 102, 221);
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  padding-bottom: 1px;
  padding-top: 4px;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.appcache-info-item {
  margin-bottom: 10px;
  margin-top: 6px;
  margin-left: 6px;
  position: relative;
}

.appcache-manifest-commands a {
  -webkit-margin-end: 16px;
  color: #777;
}

.appcache-info-template-file-url {
  width: 200px;
  height:25px;
}

.appcache-info-template-file-url {
  color: rgb(60, 102, 221);
}
</style>
  </head>
  <body i18n-values=".style.fontFamily:fontfamily;.style.fontSize:fontsize">
    <!-- templates -->
    <div style="display:none">
      <div id="appcache-list-template"
           jsvalues=".partitionPath:$this.partition_path">
        <div class="appcache-summary">
          <span jsdisplay="$this.partition_path">
            <span>Instances in: </span>
            <span jscontent="$this.partition_path"></span>
          </span>
          <span jsdisplay="!$this.partition_path">
            <span>Instances: Incognito </span>
          </span>
          <span jscontent="'(' + $this.appcache_vector.length + ')'">
          </span>
        </div>
        <div class="appcache-item" jsselect="$this.appcache_vector">
          <a class="appcache-url" jscontent="originURL"
                                  jsvalues="href:originURL"
                                  target="_blank"></a>
          <div jsselect="manifests">
            <div class="appcache-info-item" jsvalues="$manifestURL:manifestURL;
                                            $groupId:groupId;">
              <div class="appcache-manifest">
                <span> Manifest: </span>
                <a class="appcache-info-url" jscontent="manifestURL"
                                             jsvalues="href:manifestURL;"
                                             target="_blank"></a>
              </div>
              <div class="appcache-size">
                <span> Size: </span>
                <span jscontent="size"></span>
              </div>
              <div class="appcache-dates">
                <ul>
                  <li>
                    <span> Creation Time: </span>
                    <span jscontent="new Date(creationTime)"></span>
                  </li>
                  <li>
                    <span> Last Access Time: </span>
                    <span jscontent="new Date(lastAccessTime)"></span>
                  </li>
                  <li>
                    <span> Last Update Time: </span>
                    <span jscontent="new Date(lastUpdateTime)"></span>
                  </li>
                </ul>
              </div>
              <div class="appcache-manifest-commands"
                   jsvalues=".manifestURL:$manifestURL;.groupId:$groupId">
                <table>
                  <tr>
                    <td>
                      <a href="#" class="remove-manifest"> Remove Item </a>
                    </td>
                    <td>
                      <a href="#" class="view-details"> View Details </a>
                    </td>
                  </tr>
                </table>
              </div>
              <div class="appcache-details"
                   jsvalues=".manifestURL:$manifestURL;.groupId:$groupId;">
              </div>
            </div>
          </div>
        </div>
      </div>

      <div id="appcache-info-template" jsselect="$this.items">
        <span>
          <a href="#" class="appcache-info-template-file-url"
                      jscontent="fileUrl" jsvalues=".responseId:responseId">
          </a>
        </span>
        <span jscontent="size"></span>
        <span jscontent="properties"></span>
      </div>
    </div>
    <h1>Application Cache</h1>
    <div class="content">
      <div id="appcache-list">
      </div>
      <script src="chrome://resources/js/util.js"></script>
      <script src="chrome://resources/js/cr.js"></script>
      <script src="appcache_internals.js"></script>
      <script src="chrome://resources/js/load_time_data.js"></script>
      <script src="chrome://resources/js/jstemplate_compiled.js"></script>
      <script src="strings.js"></script>
      <script src="chrome://resources/js/i18n_template.js"></script>
  </body>
</html>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('appcache', function() {
  'use strict';

  var VIEW_DETAILS_TEXT = 'View Details';
  var HIDE_DETAILS_TEXT = 'Hide Details';
  var GET_ALL_APPCACHE = 'getAllAppCache';
  var DELETE_APPCACHE = 'deleteAppCache';
  var GET_APPCACHE_DETAILS = 'getAppCacheDetails';
  var GET_FILE_DETAILS = 'getFileDetails';

  var manifestsToView = [];
  var manifestsToDelete = [];
  var fileDetailsRequests = [];

  function Manifest(url, path, link) {
    this.url = url;
    this.path = path;
    this.link = link;
  }

  function FileRequest(fileURL, manifestURL, path, groupId, responseId) {
    this.fileURL = fileURL;
    this.partitionPath = path;
    this.manifestURL = manifestURL;
    this.groupId = groupId;
    this.responseId = responseId;
  }

  function getFirstAncestor(node, selector) {
    while(node) {
      if (selector(node)) {
        break;
      }
      node = node.parentNode;
    }
    return node;
  }

  function getItemByProperties(list, properties, values) {
    return list.find(function(candidate) {
      return properties.every(function(key, i) {
        return candidate[key] == values[i];
      });
    }) || null;
  }

  function removeFromList(list, item, properties) {
    var pos = 0;
    while (pos < list.length) {
      var candidate = list[pos];
      if (properties.every(function(key) {
        return candidate[key] == item[key];
      })) {
        list.splice(pos, 1);
      } else {
        pos++;
      }
    }
  }

  function initialize() {
    chrome.send(GET_ALL_APPCACHE);
  }


  function onAllAppCacheInfoReady(partition_path, data) {
    var template = jstGetTemplate('appcache-list-template');
    var container = $('appcache-list');
    container.appendChild(template);
    jstProcess(new JsEvalContext(
        {appcache_vector: data, partition_path: partition_path}),
        template);
    var removeLinks = container.querySelectorAll('a.remove-manifest');
    for (var i = 0; i < removeLinks.length; ++i) {
      removeLinks[i].onclick = deleteAppCacheInfoEventHandler;
    }
    var viewLinks = container.querySelectorAll('a.view-details');
    for (i = 0; i < viewLinks.length; ++i) {
      viewLinks[i].onclick = getAppCacheInfoEventHandler;
    }
  }

  function getAppCacheInfoEventHandler(event) {
    var link = event.target;
    if (link.text.indexOf(VIEW_DETAILS_TEXT) === -1) {
      hideAppCacheInfo(link);
    } else {
      var manifestURL = getFirstAncestor(link, function(node) {
        return !!node.manifestURL;
      }).manifestURL;
      var partitionPath = getFirstAncestor(link, function(node) {
        return !!node.partitionPath;
      }).partitionPath;
      var manifest = new Manifest(manifestURL, partitionPath, link);
      if (getItemByProperties(manifestsToView,
                              ['url', 'path'],
                              [manifestURL, partitionPath])) {
        return;
      }
      manifestsToView.push(manifest);
      chrome.send(GET_APPCACHE_DETAILS, [partitionPath, manifestURL]);
    }
  }

  function hideAppCacheInfo(link) {
    getFirstAncestor(link, function(node) {
      return node.className === 'appcache-info-item';
    }).querySelector('.appcache-details').innerHTML = '';
    link.text = VIEW_DETAILS_TEXT;
  }

  function onAppCacheDetailsReady(manifestURL, partitionPath, details) {
    if (!details) {
      console.log('Cannot show details for "' + manifestURL + '" on partition '
                   + '"' + partitionPath + '".');
      return;
    }
    var manifest = getItemByProperties(
        manifestsToView, ['url', 'path'], [manifestURL, partitionPath]);
    var link = manifest.link;
    removeFromList(manifestsToView, manifest, ['url', 'path']);
    var container = getFirstAncestor(link, function(node) {
      return node.className === 'appcache-info-item';
    }).querySelector('.appcache-details');
    var template = jstGetTemplate('appcache-info-template');
    container.appendChild(template);
    jstProcess(
        new JsEvalContext({items: simplifyAppCacheInfo(details)}), template);
    var fileLinks =
        container.querySelectorAll('a.appcache-info-template-file-url');
    for (var i = 0; i < fileLinks.length; ++i) {
      fileLinks[i].onclick = getFileContentsEventHandler;
    }
    link.text = HIDE_DETAILS_TEXT;
  }

  function simplifyAppCacheInfo(detailsVector) {
    var simpleVector = [];
    for (var index = 0; index < detailsVector.length; ++index) {
      var details = detailsVector[index];
      var properties = [];
      if (details.isManifest) {
        properties.push('Manifest');
      }
      if (details.isExplicit) {
        properties.push('Explicit');
      }
      if (details.isMaster) {
        properties.push('Master');
      }
      if (details.isForeign) {
        properties.push('Foreign');
      }
      if (details.isIntercept) {
        properties.push('Intercept');
      }
      if (details.isFallback) {
        properties.push('Fallback');
      }
      properties = properties.join(',');
      simpleVector.push({
        size : details.size,
        properties : properties,
        fileUrl : details.url,
        responseId : details.responseId
      });
    }
    return simpleVector;
  }

  function deleteAppCacheInfoEventHandler(event) {
    var link = event.target;
    var manifestURL = getFirstAncestor(link, function(node) {
      return !!node.manifestURL;
    }).manifestURL;
    var partitionPath = getFirstAncestor(link, function(node) {
      return !!node.partitionPath;
    }).partitionPath;
    var manifest = new Manifest(manifestURL, partitionPath, link);
    manifestsToDelete.push(manifest);
    chrome.send(DELETE_APPCACHE, [partitionPath, manifestURL]);
  }

  function onAppCacheInfoDeleted(partitionPath, manifestURL, deleted) {
    var manifest = getItemByProperties(
        manifestsToDelete, ['url', 'path'], [manifestURL, partitionPath]);
    if (manifest && deleted) {
      var link = manifest.link;
      var appcacheItemNode = getFirstAncestor(link, function(node) {
        return node.className === 'appcache-info-item';
      });
      var appcacheNode = getFirstAncestor(link, function(node) {
        return node.className === 'appcache-item';
      });
      appcacheItemNode.parentNode.removeChild(appcacheItemNode);
      if (appcacheNode.querySelectorAll('.appcache-info-item').length === 0) {
        appcacheNode.parentNode.removeChild(appcacheNode);
      }
    } else if (!deleted) {
      // For some reason, the delete command failed.
      console.log('Manifest "' + manifestURL + '" on partition "'
                  + partitionPath + ' cannot be accessed.');
    }
  }

  function getFileContentsEventHandler(event) {
    var link = event.target;
    var partitionPath = getFirstAncestor(link, function(node) {
      return !!node.partitionPath;
    }).partitionPath;
    var manifestURL = getFirstAncestor(link, function(node) {
      return !!node.manifestURL;
    }).manifestURL;
    var groupId = getFirstAncestor(link, function(node) {
      return !!node.groupId;
    }).groupId;
    var responseId = link.responseId;

    if (!getItemByProperties(fileDetailsRequests,
                            ['manifestURL', 'groupId', 'responseId'],
                            [manifestURL, groupId, responseId])) {
      var fileRequest = new FileRequest(link.innerText, manifestURL,
                                        partitionPath, groupId, responseId);
      fileDetailsRequests.push(fileRequest);
      chrome.send(GET_FILE_DETAILS,
        [partitionPath, manifestURL, groupId, responseId]);
    }
  }

  function onFileDetailsFailed(response, code) {
    var request =
      getItemByProperties(
        fileDetailsRequests,
        ['manifestURL', 'groupId', 'responseId'],
        [response.manifestURL, response.groupId, response.responseId]);
    console.log('Failed to get file information for file "'
                + request.fileURL + '" from partition "'
                + request.partitionPath + '" (net result code:' + code +').');
    removeFromList(
      fileDetailsRequests,
      request,
      ['manifestURL', 'groupId', 'responseId']);
  }

  function onFileDetailsReady(response, headers, raw_data) {
    var request =
      getItemByProperties(
        fileDetailsRequests,
        ['manifestURL', 'groupId', 'responseId'],
        [response.manifestURL, response.groupId, response.responseId]);
    removeFromList(
      fileDetailsRequests,
      request,
      ['manifestURL', 'groupId', 'responseId']);
    var doc = window.open().document;
    var head = document.createElement('head');
    doc.title = 'File Details: '.concat(request.fileURL);
    var headersDiv = doc.createElement('div');
    headersDiv.innerHTML = headers;
    doc.body.appendChild(headersDiv);
    var hexDumpDiv = doc.createElement('div');
    hexDumpDiv.innerHTML = raw_data;
    var linkToManifest = doc.createElement('a');
    linkToManifest.style.color = "#3C66DD";
    linkToManifest.href = request.fileURL;
    linkToManifest.target = '_blank';
    linkToManifest.innerHTML = request.fileURL;

    doc.body.appendChild(linkToManifest);
    doc.body.appendChild(headersDiv);
    doc.body.appendChild(hexDumpDiv);

    copyStylesFrom(document, doc);
  }

  function copyStylesFrom(src, dest) {
    var styles = src.querySelector('style');
    if (dest.getElementsByTagName('style').length < 1) {
      dest.head.appendChild(dest.createElement('style'));
    }
    var destStyle=  dest.querySelector('style');
    var tmp = '';
    for (var i = 0; i < styles.length; ++i) {
      tmp += styles[i].innerHTML;
    }
    destStyle.innerHTML = tmp;
  }

  return {
    initialize: initialize,
    onAllAppCacheInfoReady: onAllAppCacheInfoReady,
    onAppCacheInfoDeleted: onAppCacheInfoDeleted,
    onAppCacheDetailsReady : onAppCacheDetailsReady,
    onFileDetailsReady : onFileDetailsReady,
    onFileDetailsFailed: onFileDetailsFailed
  };

});

document.addEventListener('DOMContentLoaded', appcache.initialize);
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.appcache-summary {
  background-color: rgb(235, 239, 249);
  border-top: 1px solid rgb(156, 194, 239);
  margin-bottom: 6px;
  margin-top: 12px;
  padding: 3px;
  font-weight: bold;
}

.appcache-item {
  margin-bottom: 15px;
  margin-top: 6px;
  position: relative;
}

.appcache-url {
  color: rgb(85, 102, 221);
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  padding-bottom: 1px;
  padding-top: 4px;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.appcache-info-template-table {
  table-layout: fixed;
}

.appcache-info-properties {
  word-wrap: break-word;
}

.appcache-info-url {
  color: rgb(85, 102, 221);
  display: inline-block;
}

.appcache-manifest-url {
  color: rgb(85, 102, 221);
  display: inline-block;
  max-width: 500px;
  overflow: hidden;
  padding-bottom: 1px;
  padding-top: 4px;
  text-decoration: none;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.appcache-info-item {
  margin-bottom: 10px;
  margin-top: 6px;
  margin-left: 6px;
  position: relative;
}

.appcache-manifest-commands a {
  -webkit-margin-end: 16px;
  color: #777;
}

.appcache-info-template-file-url {
  width: 200px;
  height:25px;
}

.appcache-info-template-file-url {
  color: rgb(60, 102, 221);
}
‰PNG
