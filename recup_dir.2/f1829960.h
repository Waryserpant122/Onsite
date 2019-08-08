ad i18n-values="dir:textdirection;lang:language">
<meta name="viewport" content="width=device-width" />
<style>
* {
  box-sizing: border-box;
  -webkit-user-select: none;
}

body {
  cursor: default;
  font-family: sans-serif;
  padding: 0;
}

#debug-div {
  display: -webkit-box;
  position: fixed;
  top: 0px;
  left: 50%;
  border: 1px solid red;
}

tabbox tabpanels {
  padding: 10px;
}

</style>
<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#info-view {
  -webkit-box-flex: 1;
  overflow: auto;
  padding: 10px;
}

#info-view * {
  -webkit-user-select: text;
}

#info-view[selected] {
  -webkit-box-orient: vertical;
  display: -webkit-box;
}

#info-view h3,
#info-view ul {
  margin-bottom: 0;
  margin-top: 0;
}

#info-view > div {
  margin-bottom: 1em;
}

#info-view .row-title {
  font-weight: bold;
}

#info-view table {
  border-collapse: collapse;
  cursor: auto;
  table-layout: fixed;
  width: 100%;
}

#info-view table,
#info-view th,
#info-view td {
  border: 1px solid #777;
  padding-left: 4px;
  padding-right: 4px;
  text-align: top;
}

#info-view td {
  overflow-x: auto;
}

#info-view .feature-green {
  color: rgb(0, 128, 0);
}

#info-view .feature-yellow {
  color: rgb(128, 128, 0);
}

#info-view .feature-red {
  color: rgb(255, 0, 0);
}
</style>
<link rel="stylesheet" href="chrome://resources/css/tabs.css">
<link rel="stylesheet" href="chrome://resources/css/widgets.css">
<script src="chrome://resources/js/cr.js"></script>
<script src="chrome://resources/js/cr/event_target.js"></script>
<script src="chrome://resources/js/cr/ui.js"></script>
<script src="chrome://resources/js/cr/ui/focus_outline_manager.js"></script>
<script src="chrome://resources/js/cr/ui/tabs.js"></script>
<script src="chrome://resources/js/load_time_data.js"></script>
<script src="chrome://resources/js/util.js"></script>
<script src="gpu_internals.js"></script>
<script src="strings.js"></script>
</head>
<body>
  <div id="debug-div">
  </div>
  <!--
Copyright (c) 2012 The Chromium Authors. All rights reserved.
Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.
-->
<tabpanel id="info-view">
  <div>
    <h3>Graphics Feature Status</h3>
    <ul class="feature-status-list">
    </ul>
  </div>

  <div class='workarounds-div'>
    <h3>Driver Bug Workarounds</h3>
    <ul class="workarounds-list">
    </ul>
  </div>

  <div class='problems-div'>
    <h3>Problems Detected</h3>
    <ul class="problems-list">
    </ul>
  </div>

  <div>
    <h3>Version Information</h3>
    <div id="client-info"></div>
  </div>

  <div>
    <h3>Driver Information</h3>
    <div id="basic-info"></div>
  </div>

  <div>
    <h3>Compositor Information</h3>
    <div id="compositor-info"></div>
  </div>

  <div>
    <h3>GpuMemoryBuffers Status</h3>
    <div id="gpu-memory-buffer-info"></div>
  </div>

  <div class="diagnostics">
    <h3>Diagnostics</h3>
    <div class="diagnostics-loading">... loading ...</div>
    <div id="diagnostics-table">None</div>
  </div>

  <div id="log-messages" jsdisplay="values.length">
    <h3>Log Messages</h3>
    <ul>
      <li jsselect="values">
        <span jscontent="header"></span>: <span jscontent="message"></span>
      </li>
    </ul>
  </div>

  <!-- templates -->
  <div style="display:none">
    <div id="info-view-table-template">
      <table id="info-view-table">
        <colgroup>
          <col style="width: 25%" />
          <col style="width: 75%" />
        </colgroup>
        <tr jsselect="value">
          <td jsdisplay="!(value instanceof Array)">
            <span class="row-title" jscontent="description">title</span>
          </td>
          <td jsdisplay="!(value instanceof Array)">
            <span jscontent="value">value</span>
          </td>
          <td jsdisplay="value instanceof Array" colspan=2>
            <span jscontent="description" class="row-title"></span>
            <div transclude="info-view-table-template"></div>
          </td>
        </tr>
      </table>
    </div>
  </div>
</tabpanel>

  <script src="chrome://resources/js/i18n_template.js"></script>
  <script src="chrome://resources/js/jstemplate_compiled.js"></script>
</body>
</html>
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
cr.define('gpu', function() {
  /**
   * This class provides a 'bridge' for communicating between javascript and the
   * browser. When run outside of WebUI, e.g. as a regular webpage, it provides
   * synthetic data to assist in testing.
   * @constructor
   */
  function BrowserBridge() {
    // If we are not running inside WebUI, output chrome.send messages
    // to the console to help with quick-iteration debugging.
    this.debugMode_ = (chrome.send === undefined && console.log);
    if (this.debugMode_) {
      var browserBridgeTests = document.createElement('script');
      browserBridgeTests.src = './gpu_internals/browser_bridge_tests.js';
      document.body.appendChild(browserBridgeTests);
    }

    this.nextRequestId_ = 0;
    this.pendingCallbacks_ = [];
    this.logMessages_ = [];

    // Tell c++ code that we are ready to receive GPU Info.
    if (!this.debugMode_) {
      chrome.send('browserBridgeInitialized');
      this.beginRequestClientInfo_();
      this.beginRequestLogMessages_();
    }
  }

  BrowserBridge.prototype = {
    __proto__: cr.EventTarget.prototype,

    applySimulatedData_: function applySimulatedData(data) {
      // set up things according to the simulated data
      this.gpuInfo_ = data.gpuInfo;
      this.clientInfo_ = data.clientInfo;
      this.logMessages_ = data.logMessages;
      cr.dispatchSimpleEvent(this, 'gpuInfoUpdate');
      cr.dispatchSimpleEvent(this, 'clientInfoChange');
      cr.dispatchSimpleEvent(this, 'logMessagesChange');
    },

    /**
     * Returns true if the page is hosted inside Chrome WebUI
     * Helps have behavior conditional to emulate_webui.py
     */
    get debugMode() {
      return this.debugMode_;
    },

    /**
     * Sends a message to the browser with specified args. The
     * browser will reply asynchronously via the provided callback.
     */
    callAsync: function(submessage, args, callback) {
      var requestId = this.nextRequestId_;
      this.nextRequestId_ += 1;
      this.pendingCallbacks_[requestId] = callback;
      if (!args) {
        chrome.send('callAsync', [requestId.toString(), submessage]);
      } else {
        var allArgs = [requestId.toString(), submessage].concat(args);
        chrome.send('callAsync', allArgs);
      }
    },

    /**
     * Called by gpu c++ code when client info is ready.
     */
    onCallAsyncReply: function(requestId, args) {
      if (this.pendingCallbacks_[requestId] === undefined) {
        throw new Error('requestId ' + requestId + ' is not pending');
      }
      var callback = this.pendingCallbacks_[requestId];
      callback(args);
      delete this.pendingCallbacks_[requestId];
    },

    /**
     * Get gpuInfo data.
     */
    get gpuInfo() {
      return this.gpuInfo_;
    },

    /**
     * Called from gpu c++ code when GPU Info is updated.
     */
    onGpuInfoUpdate: function(gpuInfo) {
      this.gpuInfo_ = gpuInfo;
      cr.dispatchSimpleEvent(this, 'gpuInfoUpdate');
    },

    /**
     * This function begins a request for the ClientInfo. If it comes back
     * as undefined, then we will issue the request again in 250ms.
     */
    beginRequestClientInfo_: function() {
      this.callAsync('requestClientInfo', undefined, (function(data) {
        if (data === undefined) { // try again in 250 ms
          window.setTimeout(this.beginRequestClientInfo_.bind(this), 250);
        } else {
          this.clientInfo_ = data;
          cr.dispatchSimpleEvent(this, 'clientInfoChange');
        }
      }).bind(this));
    },

    /**
     * Returns information about the currently running Chrome build.
     */
    get clientInfo() {
      return this.clientInfo_;
    },

    /**
     * This function checks for new GPU_LOG messages.
     * If any are found, a refresh is triggered.
     */
    beginRequestLogMessages_: function() {
      this.callAsync('requestLogMessages', undefined,
          (function(messages) {
            if (messages.length != this.logMessages_.length) {
              this.logMessages_ = messages;
              cr.dispatchSimpleEvent(this, 'logMessagesChange');
            }
            // check again in 250 ms
            window.setTimeout(this.beginRequestLogMessages_.bind(this), 250);
          }).bind(this));
    },

    /**
     * Returns an array of log messages issued by the GPU process, if any.
     */
    get logMessages() {
      return this.logMessages_;
    },

    /**
     * Returns the value of the "Sandboxed" row.
     */
    isSandboxedForTesting : function() {
      for (i = 0; i < this.gpuInfo_.basic_info.length; ++i) {
        var info = this.gpuInfo_.basic_info[i];
        if (info.description == "Sandboxed")
          return info.value;
      }
      return false;
    }
  };

  return {
    BrowserBridge: BrowserBridge
  };
});

// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


/**
 * @fileoverview This view displays information on the current GPU
 * hardware.  Its primary usefulness is to allow users to copy-paste
 * their data in an easy to read format for bug reports.
 */
cr.define('gpu', function() {
  /**
   * Provides information on the GPU process and underlying graphics hardware.
   * @constructor
   * @extends {cr.ui.TabPanel}
   */
  var InfoView = cr.ui.define(cr.ui.TabPanel);

  InfoView.prototype = {
    __proto__: cr.ui.TabPanel.prototype,

    decorate: function() {
      cr.ui.TabPanel.prototype.decorate.apply(this);

      browserBridge.addEventListener('gpuInfoUpdate', this.refresh.bind(this));
      browserBridge.addEventListener('logMessagesChange',
                                     this.refresh.bind(this));
      browserBridge.addEventListener('clientInfoChange',
                                     this.refresh.bind(this));
      this.refresh();
    },

    /**
    * Updates the view based on its currently known data
    */
    refresh: function(data) {
      // Client info
      if (browserBridge.clientInfo) {
        var clientInfo = browserBridge.clientInfo;

        var commandLineParts = clientInfo.command_line.split(' ');
        commandLineParts.shift(); // Pop off the exe path
        var commandLineString = commandLineParts.join(' ')

        this.setTable_('client-info', [
          {
            description: 'Data exported',
            value: (new Date()).toLocaleString()
          },
          {
            description: 'Chrome version',
            value: clientInfo.version
          },
          {
            description: 'Operating system',
            value: clientInfo.operating_system
          },
          {
            description: 'Software rendering list version',
            value: clientInfo.blacklist_version
          },
          {
            description: 'Driver bug list version',
            value: clientInfo.driver_bug_list_version
          },
          {
            description: 'ANGLE commit id',
            value: clientInfo.angle_commit_id
          },
          {
            description: '2D graphics backend',
            value: clientInfo.graphics_backend
          },
          {
            description: 'Command Line Args',
            value: commandLineString
          }]);
      } else {
        this.setText_('client-info', '... loading...');
      }

      // Feature map
      var featureLabelMap = {
        '2d_canvas': 'Canvas',
        'gpu_compositing': 'Compositing',
        'webgl': 'WebGL',
        'multisampling': 'WebGL multisampling',
        'flash_3d': 'Flash',
        'flash_stage3d': 'Flash Stage3D',
        'flash_stage3d_baseline': 'Flash Stage3D Baseline profile',
        'texture_sharing': 'Texture Sharing',
        'video_decode': 'Video Decode',
        'video_encode': 'Video Encode',
        'panel_fitting': 'Panel Fitting',
        'rasterization': 'Rasterization',
        'multiple_raster_threads': 'Multiple Raster Threads',
        'native_gpu_memory_buffers': 'Native GpuMemoryBuffers',
      };

      var statusMap =  {
        'disabled_software': {
          'label': 'Software only. Hardware acceleration disabled',
          'class': 'feature-yellow'
        },
        'disabled_off': {
          'label': 'Disabled',
          'class': 'feature-red'
        },
        'disabled_off_ok': {
          'label': 'Disabled',
          'class': 'feature-yellow'
        },
        'unavailable_software': {
          'label': 'Software only, hardware acceleration unavailable',
          'class': 'feature-yellow'
        },
        'unavailable_off': {
          'label': 'Unavailable',
          'class': 'feature-red'
        },
        'unavailable_off_ok': {
          'label': 'Unavailable',
          'class': 'feature-yellow'
        },
        'enabled_readback': {
          'label': 'Hardware accelerated but at reduced performance',
          'class': 'feature-yellow'
        },
        'enabled_force': {
          'label': 'Hardware accelerated on all pages',
          'class': 'feature-green'
        },
        'enabled': {
          'label': 'Hardware accelerated',
          'class': 'feature-green'
        },
        'enabled_on': {
          'label': 'Enabled',
          'class': 'feature-green'
        },
        'enabled_force_on': {
          'label': 'Force enabled',
          'class': 'feature-green'
        },
      };

      // GPU info, basic
      var diagnosticsDiv = this.querySelector('.diagnostics');
      var diagnosticsLoadingDiv = this.querySelector('.diagnostics-loading');
      var featureStatusList = this.querySelector('.feature-status-list');
      var problemsDiv = this.querySelector('.problems-div');
      var problemsList = this.querySelector('.problems-list');
      var workaroundsDiv = this.querySelector('.workarounds-div');
      var workaroundsList = this.querySelector('.workarounds-list');
      var gpuInfo = browserBridge.gpuInfo;
      var i;
      if (gpuInfo) {
        // Not using jstemplate here for blacklist status because we construct
        // href from data, which jstemplate can't seem to do.
        if (gpuInfo.featureStatus) {
          // feature status list
          featureStatusList.textContent = '';
          for (var featureName in gpuInfo.featureStatus.featureStatus) {
            var featureStatus =
                gpuInfo.featureStatus.featureStatus[featureName];
            var featureEl = document.createElement('li');

            var nameEl = document.createElement('span');
            if (!featureLabelMap[featureName])
              console.log('Missing featureLabel for', featureName);
            nameEl.textContent = featureLabelMap[featureName] + ': ';
            featureEl.appendChild(nameEl);

            var statusEl = document.createElement('span');
            var statusInfo = statusMap[featureStatus];
            if (!statusInfo) {
              console.log('Missing status for ', featureStatus);
              statusEl.textContent = 'Unknown';
              statusEl.className = 'feature-red';
            } else {
              statusEl.textContent = statusInfo['label'];
              statusEl.className = statusInfo['class'];
            }
            featureEl.appendChild(statusEl);

            featureStatusList.appendChild(featureEl);
          }

          // problems list
          if (gpuInfo.featureStatus.problems.length) {
            problemsDiv.hidden = false;
            problemsList.textContent = '';
            for (i = 0; i < gpuInfo.featureStatus.problems.length; i++) {
              var problem = gpuInfo.featureStatus.problems[i];
              var problemEl = this.createProblemEl_(problem);
              problemsList.appendChild(problemEl);
            }
          } else {
            problemsDiv.hidden = true;
          }

          // driver bug workarounds list
          if (gpuInfo.featureStatus.workarounds.length) {
            workaroundsDiv.hidden = false;
            workaroundsList.textContent = '';
            for (i = 0; i < gpuInfo.featureStatus.workarounds.length; i++) {
              var workaroundEl = document.createElement('li');
              workaroundEl.textContent = gpuInfo.featureStatus.workarounds[i];
              workaroundsList.appendChild(workaroundEl);
            }
          } else {
            workaroundsDiv.hidden = true;
          }

        } else {
          featureStatusList.textContent = '';
          problemsList.hidden = true;
          workaroundsList.hidden = true;
        }

        if (gpuInfo.basic_info)
          this.setTable_('basic-info', gpuInfo.basic_info);
        else
          this.setTable_('basic-info', []);

        if (gpuInfo.compositorInfo)
          this.setTable_('compositor-info', gpuInfo.compositorInfo);
        else
          this.setTable_('compositor-info', []);

        if (gpuInfo.gpuMemoryBufferInfo)
          this.setTable_('gpu-memory-buffer-info', gpuInfo.gpuMemoryBufferInfo);
        else
          this.setTable_('gpu-memory-buffer-info', []);

        if (gpuInfo.diagnostics) {
          diagnosticsDiv.hidden = false;
          diagnosticsLoadingDiv.hidden = true;
          $('diagnostics-table').hidden = false;
          this.setTable_('diagnostics-table', gpuInfo.diagnostics);
        } else if (gpuInfo.diagnostics === null) {
          // gpu_internals.cc sets diagnostics to null when it is being loaded
          diagnosticsDiv.hidden = false;
          diagnosticsLoadingDiv.hidden = false;
          $('diagnostics-table').hidden = true;
        } else {
          diagnosticsDiv.hidden = true;
        }
      } else {
        this.setText_('basic-info', '... loading ...');
        diagnosticsDiv.hidden = true;
        featureStatusList.textContent = '';
        problemsDiv.hidden = true;
      }

      // Log messages
      jstProcess(new JsEvalContext({values: browserBridge.logMessages}),
                 $('log-messages'));
    },

    createProblemEl_: function(problem) {
      var problemEl;
      problemEl = document.createElement('li');

      // Description of issue
      var desc = document.createElement('a');
      desc.textContent = problem.description;
      problemEl.appendChild(desc);

      // Spacing ':' element
      if (problem.crBugs.length + problem.webkitBugs.length > 0) {
        var tmp = document.createElement('span');
        tmp.textContent = ': ';
        problemEl.appendChild(tmp);
      }

      var nbugs = 0;
      var j;

      // crBugs
      for (j = 0; j < problem.crBugs.length; ++j) {
        if (nbugs > 0) {
          var tmp = document.createElement('span');
          tmp.textContent = ', ';
          problemEl.appendChild(tmp);
        }

        var link = document.createElement('a');
        var bugid = parseInt(problem.crBugs[j]);
        link.textContent = bugid;
        link.href = 'http://crbug.com/' + bugid;
        problemEl.appendChild(link);
        nbugs++;
      }

      for (j = 0; j < problem.webkitBugs.length; ++j) {
        if (nbugs > 0) {
          var tmp = document.createElement('span');
          tmp.textContent = ', ';
          problemEl.appendChild(tmp);
        }

        var link = document.createElement('a');
        var bugid = parseInt(problem.webkitBugs[j]);
        link.textContent = bugid;

        link.href = 'https://bugs.webkit.org/show_bug.cgi?id=' + bugid;
        problemEl.appendChild(link);
        nbugs++;
      }

      if (problem.affectedGpuSettings.length > 0) {
        var brNode = document.createElement('br');
        problemEl.appendChild(brNode);

        var iNode = document.createElement('i');
        problemEl.appendChild(iNode);

        var headNode = document.createElement('span');
        if (problem.tag == 'disabledFeatures')
          headNode.textContent = 'Disabled Features: ';
        else  // problem.tag == 'workarounds'
          headNode.textContent = 'Applied Workarounds: ';
        iNode.appendChild(headNode);
        for (j = 0; j < problem.affectedGpuSettings.length; ++j) {
          if (j > 0) {
            var separateNode = document.createElement('span');
            separateNode.textContent = ', ';
            iNode.appendChild(separateNode);
          }
          var nameNode = document.createElement('span');
          if (problem.tag == 'disabledFeatures')
            nameNode.classList.add('feature-red');
          else  // problem.tag == 'workarounds'
            nameNode.classList.add('feature-yellow');
          nameNode.textContent = problem.affectedGpuSettings[j];
          iNode.appendChild(nameNode);
        }
      }

      return problemEl;
    },

    setText_: function(outputElementId, text) {
      var peg = document.getElementById(outputElementId);
      peg.textContent = text;
    },

    setTable_: function(outputElementId, inputData) {
      var template = jstGetTemplate('info-view-table-template');
      jstProcess(new JsEvalContext({value: inputData}),
                 template);

      var peg = document.getElementById(outputElementId);
      if (!peg)
        throw new Error('Node ' + outputElementId + ' not found');

      peg.innerHTML = '';
      peg.appendChild(template);
    }
  };

  return {
    InfoView: InfoView
  };
});


var browserBridge;

/**
 * Main entry point. called once the page has loaded.
 */
function onLoad() {
  browserBridge = new gpu.BrowserBridge();

  // Create the views.
  cr.ui.decorate('#info-view', gpu.InfoView);
}

document.addEventListener('DOMContentLoaded', onLoad);
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <title>IndexedDB</title>
  <link rel="stylesheet" href="chrome://resources/css/tabs.css">
  <link rel="stylesheet" href="chrome://resources/css/widgets.css">
  <style>/* Copyright (c) 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.indexeddb-summary {
    background-color: rgb(235, 239, 249);
    border-top: 1px solid rgb(156, 194, 239);
    margin-bottom: 6px;
    margin-top: 12px;
    padding: 3px;
    font-weight: bold;
}

.indexeddb-item {
    margin-bottom: 15px;
    margin-top: 6px;
    position: relative;
}

.indexeddb-url {
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

.indexeddb-database {
    margin-bottom: 6px;
    margin-top: 6px;
    margin-left: 12px;

    position: relative;
}

.indexeddb-database > div {
    margin-left: 12px;
}

.indexeddb-connection-count {
    margin: 0 8px;
}
.indexeddb-connection-count.pending {
    font-weight: bold;
}

.indexeddb-path {
    display: block;
    margin-left: 1em;
}

.indexeddb-transaction-list {
    margin-left: 10px;
    border-collapse: collapse;
}

.indexeddb-transaction-list th,
.indexeddb-transaction-list td {
    padding: 2px 10px;
    min-width: 50px;
    max-width: 75px;
}

td.indexeddb-transaction-scope {
    min-width: 200px;
    max-width: 500px;
}

.indexeddb-transaction-list th {
    background-color: rgb(249, 249, 249);
    border: 1px solid rgb(156, 194, 239);
    font-weight: normal;
    text-align: left;
}

.indexeddb-transaction {
    background-color: rgb(235, 239, 249);
    border-bottom: 2px solid white;
}

.indexeddb-transaction.created {
    font-weight: italic;
}
.indexeddb-transaction.started {
    font-weight: bold;
}
.indexeddb-transaction.running {
    font-weight: bold;
}
.indexeddb-transaction.committing {
    font-weight: bold;
}
.indexeddb-transaction.blocked {
}

.indexeddb-transaction.started .indexeddb-transaction-state {
    background-color: rgb(249, 249, 235);
}
.indexeddb-transaction.running .indexeddb-transaction-state {
    background-color: rgb(235, 249, 235);
}
.indexeddb-transaction.committing .indexeddb-transaction-state {
    background-color: rgb(235, 235, 249);
}
.indexeddb-transaction.blocked .indexeddb-transaction-state {
    background-color: rgb(249, 235, 235);
}

.controls a {
    -webkit-margin-end: 16px;
    color: #777;
}
</style>
</head>
<body i18n-values=".style.fontFamily:fontfamily;.style.fontSize:fontsize">
    <!-- templates -->
    <div style="display:none">
        <div id="indexeddb-list-template"
             jsvalues="$partition_path:$this.partition_path">
            <div class="indexeddb-summary">
                <span jsdisplay="$this.partition_path">
                    <span>Instances in: </span>
                    <span jscontent="$this.partition_path"></span>
                </span>
                <span jsdisplay="!$this.partition_path">
                    <span>Instances: Incognito </span>
                </span>
                <span jscontent="'(' + $this.idbs.length + ')'"></span>
            </div>
            <div class="indexeddb-item" jsselect="$this.idbs">
                <a class="indexeddb-url" jscontent="url" jsvalues="href:url"
                   target="_blank"></a>
                <div class="indexeddb-size">
                    <span>Size:</span>
                    <span jscontent="size"></span>
                </div>
                <div class="indexeddb-last-modified">
                    <span>Last modified:</span>
                    <span jscontent="new Date(last_modified)"></span>
                </div>
                <div>
                    <span>Open connections:</span>
                    <span class="connection-count"
                          jsvalues=".idb_origin_url:url;.idb_partition_path:$partition_path"
                          jscontent="connection_count">
                </div>
                <div class="indexeddb-paths">
                    <span>Paths:</span>
                    <span class="indexeddb-path" jscontent="$this" jsselect="$this.paths"></span>
                </div>
                <div class="controls">
                    <a href="#" class="force-close"
                       jsvalues=".idb_origin_url:url;.idb_partition_path:$partition_path">Force close</a>
                    <a href="#" class="download"
                       jsvalues=".idb_origin_url:url;.idb_partition_path:$partition_path">Download</a>
                    <span class="download-status" style="display: none">Loading...</span>
                </div>
                <div class="indexeddb-database" jsselect="$this.databases">

                  <span>Open database:</span>
                  <span jscontent="name"></span>

                  <div>
                    <span>Connections:</span>

                    <span class="indexeddb-connection-count"
                          jsdisplay="connection_count">
                      <span>open:</span>
                      <span jscontent="connection_count"></span>
                    </span>

                    <span class="indexeddb-connection-count pending"
                          jsdisplay="pending_opens">
                      <span>pending opens:</span>
                      <span jscontent="pending_opens"></span>
                    </span>

                    <span class="indexeddb-connection-count pending"
                          jsdisplay="pending_upgrades">
                      <span>pending upgrades:</span>
                      <span jscontent="pending_upgrades"></span>
                    </span>

                    <span class="indexeddb-connection-count pending"
                          jsdisplay="running_upgrades">
                      <span>running upgrades:</span>
                      <span jscontent="running_upgrades"></span>
                    </span>

                    <span class="indexeddb-connection-count pending"
                          jsdisplay="pending_deletes">
                      <span>pending deletes:</span>
                      <span jscontent="pending_deletes"></span>
                    </span>

                  </div>
                  <div jsdisplay="$this.transactions &amp;&amp;
                                  $this.transactions.length">
                    <span>Transactions:</span>

                    <table class="indexeddb-transaction-list">
                      <tbody>
                        <tr>
                          <th title="Process ID of the tab or SharedWorker that created the transaction">
                            Process ID
                          </th>
                          <th title="Transaction ID (unique within Process)">
                            ID
                          </th>
                          <th title="Type of transaction">
                            Mode
                          </th>
                          <th title="Names of object stores used by the transaction">
                            Scope
                          </th>
                          <th title="Number of requests that have been executed">
                            Completed Requests
                          </th>
                          <th title="Number of requests that have not yet been executed">
                            Pending Requests
                          </th>
                          <th title="Time since transaction creation">
                            Age (ms)
                          </th>
                          <th title="Time since transaction started">
                            Runtime (ms)
                          </th>
                          <th title="Status in the transaction queue">
                            Status
                          </th>
                        </tr>
                        <tr class="indexeddb-transaction"
                            jsselect="$this.transactions"
                            jseval="this.classList.add($this.status)">

                          <td class="indexeddb-transaction-pid"
                              jscontent="pid">
                          </td>

                          <td class="indexeddb-transaction-tid"
                              jscontent="tid">
                          </td>

                          <td class="indexeddb-transaction-mode"
                              jscontent="mode">
                          </td>

                          <td class="indexeddb-transaction-scope"
                              jscontent="'[ ' + scope.join(', ') + ' ]'">
                          </td>

                          <td class="indexeddb-transaction-requests-complete"
                              jscontent="tasks_completed">
                          </td>

                          <td class="indexeddb-transaction-requests-pending"
                              jscontent="tasks_scheduled - tasks_completed">
                          </td>

                          <td class="indexeddb-transaction-age"
                              jscontent="Math.round(age)">
                          </td>

                          <td class="indexeddb-transaction-age">
                            <span jsdisplay="status == 'started' || status == 'running' || status == 'committing'"
                                  jscontent="Math.round(runtime)">
                            </span>
                          </td>

                          <td class="indexeddb-transaction-state"
                              jscontent="status">
                          </td>
                        </tr>
                      </tbody>
                    </table>
                  </div>
                </div>
            </div>
        </div>
    </div>
    <h1>IndexedDB</h1>
    <div class="content">
        <div id="indexeddb-list">
    </div>
    <script src="chrome://resources/js/util.js"></script>
    <script src="chrome://resources/js/cr.js"></script>
    <script src="indexeddb_internals.js"></script>
    <script src="chrome://resources/js/load_time_data.js"></script>
    <script src="chrome://resources/js/jstemplate_compiled.js"></script>
    <script src="strings.js"></script>
    <script src="chrome://resources/js/i18n_template.js"></script>
</body>
</html>
// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('indexeddb', function() {
  'use strict';

  function initialize() {
    chrome.send('getAllOrigins');
  }

  function progressNodeFor(link) {
    return link.parentNode.querySelector('.download-status');
  }

  function downloadOriginData(event) {
    var link = event.target;
    progressNodeFor(link).style.display = 'inline';
    chrome.send('downloadOriginData', [link.idb_partition_path,
                                       link.idb_origin_url]);
    return false;
  }

  function forceClose(event) {
    var link = event.target;
    progressNodeFor(link).style.display = 'inline';
    chrome.send('forceClose', [link.idb_partition_path,
                               link.idb_origin_url]);
    return false;
  }

  function withNode(selector, partition_path, origin_url, callback) {
    var links = document.querySelectorAll(selector);
    for (var i = 0; i < links.length; ++i) {
      var link = links[i];
      if (partition_path == link.idb_partition_path &&
          origin_url == link.idb_origin_url) {
        callback(link);
      }
    }
  }
  // Fired from the backend after the data has been zipped up, and the
  // download manager has begun downloading the file.
  function onOriginDownloadReady(partition_path, origin_url, connection_count) {
    withNode('a.download', partition_path, origin_url, function(link) {
      progressNodeFor(link).style.display = 'none';
    });
    withNode('.connection-count', partition_path, origin_url, function(span) {
      span.innerText = connection_count;
    });
  }

  function onForcedClose(partition_path, origin_url, connection_count) {
    withNode('a.force-close', partition_path, origin_url, function(link) {
      progressNodeFor(link).style.display = 'none';
    });
    withNode('.connection-count', partition_path, origin_url, function(span) {
      span.innerText = connection_count;
    });
  }

  // Fired from the backend with a single partition's worth of
  // IndexedDB metadata.
  function onOriginsReady(origins, partition_path) {
    var template = jstGetTemplate('indexeddb-list-template');
    var container = $('indexeddb-list');
    container.appendChild(template);
    jstProcess(new JsEvalContext({ idbs: origins,
                                   partition_path: partition_path}), template);

    var downloadLinks = container.querySelectorAll('a.download');
    for (var i = 0; i < downloadLinks.length; ++i) {
      downloadLinks[i].addEventListener('click', downloadOriginData, false);
    }
    var forceCloseLinks = container.querySelectorAll('a.force-close');
    for (i = 0; i < forceCloseLinks.length; ++i) {
      forceCloseLinks[i].addEventListener('click', forceClose, false);
    }
  }

  return {
    initialize: initialize,
    onForcedClose: onForcedClose,
    onOriginDownloadReady: onOriginDownloadReady,
    onOriginsReady: onOriginsReady,
  };
});

document.addEventListener('DOMContentLoaded', indexeddb.initialize);
/* Copyright (c) 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.indexeddb-summary {
    background-color: rgb(235, 239, 249);
    border-top: 1px solid rgb(156, 194, 239);
    margin-bottom: 6px;
    margin-top: 12px;
    padding: 3px;
    font-weight: bold;
}

.indexeddb-item {
    margin-bottom: 15px;
    margin-top: 6px;
    position: relative;
}

.indexeddb-url {
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

.indexeddb-database {
    margin-bottom: 6px;
    margin-top: 6px;
    margin-left: 12px;

    position: relative;
}

.indexeddb-database > div {
    margin-left: 12px;
}

.indexeddb-connection-count {
    margin: 0 8px;
}
.indexeddb-connection-count.pending {
    font-weight: bold;
}

.indexeddb-path {
    display: block;
    margin-left: 1em;
}

.indexeddb-transaction-list {
    margin-left: 10px;
    border-collapse: collapse;
}

.indexeddb-transaction-list th,
.indexeddb-transaction-list td {
    padding: 2px 10px;
    min-width: 50px;
    max-width: 75px;
}

td.indexeddb-transaction-scope {
    min-width: 200px;
    max-width: 500px;
}

.indexeddb-transaction-list th {
    background-color: rgb(249, 249, 249);
    border: 1px solid rgb(156, 194, 239);
    font-weight: normal;
    text-align: left;
}

.indexeddb-transaction {
    background-color: rgb(235, 239, 249);
    border-bottom: 2px solid white;
}

.indexeddb-transaction.created {
    font-weight: italic;
}
.indexeddb-transaction.started {
    font-weight: bold;
}
.indexeddb-transaction.running {
    font-weight: bold;
}
.indexeddb-transaction.committing {
    font-weight: bold;
}
.indexeddb-transaction.blocked {
}

.indexeddb-transaction.started .indexeddb-transaction-state {
    background-color: rgb(249, 249, 235);
}
.indexeddb-transaction.running .indexeddb-transaction-state {
    background-color: rgb(235, 249, 235);
}
.indexeddb-transaction.committing .indexeddb-transaction-state {
    background-color: rgb(235, 235, 249);
}
.indexeddb-transaction.blocked .indexeddb-transaction-state {
    background-color: rgb(249, 235, 235);
}

.controls a {
    -webkit-margin-end: 16px;
    color: #777;
}
<!--
Copyright 2013 The Chromium Authors. All rights reserved.
Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.
-->
<!DOCTYPE html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <title i18n-content="Media Internals"></title>
  <style>/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

html,
body,
#container {
  margin: 0;
  padding: 0;
  width: 100%;
  height: 100%;
  font-family:Arial;
}

tabbox {
  margin-top: 10px;
}

tab {
  -webkit-user-select: none;
}

body tabpanels {
  box-shadow: none;
}

tabpanel {
  padding: 10px;
}

table {
  font-family: sans-serif;
  -webkit-font-smoothing: antialiased;
  font-size: 115%;
  width: auto;
  overflow: auto;
  display: block;
}
th {
  background-color: #4AA9E4;
  font-weight: normal;
  color: white;
  padding: 2px;
  text-align: center;
  min-width: 230px;
}
td {
  background-color: rgb(238, 238, 238);
  padding: 2px;
  color: rgb(111, 111, 111);
  word-wrap: break-word;
  min-width: 230px;
}

h1,
h2,
h3 {
  color: rgb(50,50,50);
}

#container {
  display: flex;
  flex-direction: row;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  align-content: stretch;
}

#container > * {
  padding: 0;
  padding-left: 25px;
  margin: 0;
}

#list-wrapper {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  align-items: flex-start;
  align-content: stretch;
}

#player-list-wrapper,
#audio-component-list-wrapper {
  flex-grow: 1;
  align-self: stretch;
  min-width: 200px;
  overflow: auto;
}

#player-list-wrapper ul,
#player-list-wrapper li,
#audio-component-list-wrapper ul,
#audio-component-list-wrapper li {
  padding: 0px;
  list-style-type: none;
}
#list-wrapper button {
  padding: 0px;
}

.property-wrapper,
#log-wrapper {
  align-self: stretch;
  display:block;
  flex-grow: 0.25;
  overflow: auto;
  margin-bottom: 10px;
}

#video-capture-capabilities-wrapper {
  flex-grow: 0.5;
  align-self: stretch;
  overflow: auto;
}

#log-wrapper > thead {
  position: fixed;
}

#graphs li {
  list-style-type: none;
}

#clipboard-textarea {
  position: absolute;
  width: 50%;
  height: 50%;

  left: 25%;
  top: 25%;
}

.hiddenClipboard {
  display: none;
}

.timestamp {
  min-width: 115px;
}

#video-capture-capabilities-table {
  margin-bottom:30px;
}

#video-capture-capabilities-table th,
#video-capture-capabilities-table td {
  min-width:120px;
}

#video-capture-capabilities-table td {
  padding:5px;
}

#video-capture-capabilities-table tr td {
  font-size:13px;
  text-align:center;
}

#video-capture-capabilities-table .video-capture-formats-table th,
#video-capture-capabilities-table .video-capture-formats-table td {
  text-align:right;
  min-width:80px;
}

#video-capture-capabilities-table .video-capture-formats-table th {
  background:none;
  color:#666;
  font-size:13px;
  font-weight:bold;
}

#video-capture-capabilities-table .video-capture-formats-table td {
  padding:2px;
}

.show-none-if-empty:empty:after {
  content: "none";
  color: rgba(0, 0, 0, .5);
}

label.selectable-button {
  -webkit-appearance: button;
  -webkit-user-select: none;
  padding: 2px 5px;
  margin-bottom: 5px;
}

input.selectable-button {
  display: none;
}

input.selectable-button:checked + label.selectable-button {
  background-color: #4AA9E4;
  color: white;
}

.no-players-selected #players .property-wrapper,
.no-players-selected #players #log-wrapper {
  display: none;
}

.no-components-selected #audio .property-wrapper {
  display: none;
}

</style>
  <script src="chrome://resources/js/cr.js"></script>
  <script src="chrome://resources/js/cr/ui.js"></script>
  <script src="chrome://resources/js/cr/ui/focus_outline_manager.js"></script>
  <script src="chrome://resources/js/util.js"></script>
  <script src="chrome://resources/js/cr/ui/tabs.js"></script>
  <link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
  <link rel="stylesheet" href="chrome://resources/css/tabs.css">
</head>

<body>
  <tabbox>
    <tabs>
      <tab>Players</tab>
      <tab>Audio</tab>
      <tab>Video Capture</tab>
    </tabs>
    <tabpanels>
      <tabpanel id="players">
        <div id="list-wrapper">
          <div id="player-list-wrapper">
            <h2>Players</h2>
            <ul id="player-list" class="show-none-if-empty"></ul>
          </div>
        </div>
        <div class="property-wrapper">
          <h2>
            Player Properties
            <button class="copy-button">Copy to clipboard</button>
          </h2>
          <table id="player-property-table">
            <thead>
              <tr>
                <th>Property</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody></tbody>
          </table>
        </div>
        <div id="log-wrapper">
          <h2>
            Log <input id="filter-text" type="text" placeholder="property filter">
          </h2>
          <table id="log">
            <thead>
              <tr>
                <th class="timestamp">Timestamp</th>
                <th>Property</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody></tbody>
          </table>
        </div>
        <ul id="graphs"></ul>
      </tabpanel>
      <tabpanel id="audio">
        <div id="audio-component-list-wrapper">
          <h2>Input Controllers</h2>
          <ul id="audio-input-controller-list" class="show-none-if-empty"></ul>
        </div>
        <div id="audio-component-list-wrapper">
          <h2>Output Controllers</h2>
          <ul id="audio-output-controller-list" class="show-none-if-empty"></ul>
        </div>
        <div id="audio-component-list-wrapper">
          <h2>Output Streams</h2>
          <ul id="audio-output-stream-list" class="show-none-if-empty"></ul>
        </div>
        <div class="property-wrapper">
          <h2>
            <span id="audio-property-name"></span> Properties
            <button class="copy-button">Copy to clipboard</button>
          </h2>
          <table id="audio-property-table">
            <thead>
              <tr>
                <th>Property</th>
                <th>Value</th>
              </tr>
            </thead>
            <tbody></tbody>
          </table>
        </div>
      </tabpanel>
      <tabpanel id="video-capture">
        <div id="video-capture-capabilities-wrapper">
          <h2>
            <span>Video Capture Device Capabilities</span>
            <button id="video-capture-capabilities-copy-button">
              Copy to clipboard
            </button>
          </h2>
          <table id="video-capture-capabilities-table">
            <thead>
              <tr>
                <th>Device Name</th>
                <th>Formats</th>
                <th>Capture API</th>
                <th>Device ID</th>
              </tr>
            </thead>
            <tbody id="video-capture-capabilities-tbody" class="show-none-if-empty"></tbody>
          </table>
        </div>
      </tabpanel>
    </tabpanels>
  </tabbox>
  <textarea id="clipboard-textarea" class="hiddenClipboard"></textarea>
  <script src="media_internals.js"></script>
</body>
</html>
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var media = {};

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * A global object that gets used by the C++ interface.
 */
var media = (function() {
  'use strict';

  var manager = null;

  // A number->string mapping that is populated through the backend that
  // describes the phase that the network entity is in.
  var eventPhases = {};

  // A number->string mapping that is populated through the backend that
  // describes the type of event sent from the network.
  var eventTypes = {};

  // A mapping of number->CacheEntry where the number is a unique id for that
  // network request.
  var cacheEntries = {};

  // A mapping of url->CacheEntity where the url is the url of the resource.
  var cacheEntriesByKey = {};

  var requrestURLs = {};

  var media = {
    BAR_WIDTH: 200,
    BAR_HEIGHT: 25
  };

  /**
   * Users of |media| must call initialize prior to calling other methods.
   */
  media.initialize = function(theManager) {
    manager = theManager;
  };

  media.onReceiveAudioStreamData = function(audioStreamData) {
    for (var component in audioStreamData) {
      media.updateAudioComponent(audioStreamData[component]);
    }
  };

  media.onReceiveVideoCaptureCapabilities = function(videoCaptureCapabilities) {
    manager.updateVideoCaptureCapabilities(videoCaptureCapabilities)
  }

  media.onReceiveConstants = function(constants) {
    for (var key in constants.eventTypes) {
      var value = constants.eventTypes[key];
      eventTypes[value] = key;
    }

    for (var key in constants.eventPhases) {
      var value = constants.eventPhases[key];
      eventPhases[value] = key;
    }
  };

  media.cacheForUrl = function(url) {
    return cacheEntriesByKey[url];
  };

  media.onNetUpdate = function(updates) {
    updates.forEach(function(update) {
      var id = update.source.id;
      if (!cacheEntries[id])
        cacheEntries[id] = new media.CacheEntry;

      switch (eventPhases[update.phase] + '.' + eventTypes[update.type]) {
        case 'PHASE_BEGIN.DISK_CACHE_ENTRY_IMPL':
          var key = update.params.key;

          // Merge this source with anything we already know about this key.
          if (cacheEntriesByKey[key]) {
            cacheEntriesByKey[key].merge(cacheEntries[id]);
            cacheEntries[id] = cacheEntriesByKey[key];
          } else {
            cacheEntriesByKey[key] = cacheEntries[id];
          }
          cacheEntriesByKey[key].key = key;
          break;

        case 'PHASE_BEGIN.SPARSE_READ':
          cacheEntries[id].readBytes(update.params.offset,
                                      update.params.buff_len);
          cacheEntries[id].sparse = true;
          break;

        case 'PHASE_BEGIN.SPARSE_WRITE':
          cacheEntries[id].writeBytes(update.params.offset,
                                       update.params.buff_len);
          cacheEntries[id].sparse = true;
          break;

        case 'PHASE_BEGIN.URL_REQUEST_START_JOB':
          requrestURLs[update.source.id] = update.params.url;
          break;

        case 'PHASE_NONE.HTTP_TRANSACTION_READ_RESPONSE_HEADERS':
          // Record the total size of the file if this was a range request.
          var range = /content-range:\s*bytes\s*\d+-\d+\/(\d+)/i.exec(
              update.params.headers);
          var key = requrestURLs[update.source.id];
          delete requrestURLs[update.source.id];
          if (range && key) {
            if (!cacheEntriesByKey[key]) {
              cacheEntriesByKey[key] = new media.CacheEntry;
              cacheEntriesByKey[key].key = key;
            }
            cacheEntriesByKey[key].size = range[1];
          }
          break;
      }
    });
  };

  media.onRendererTerminated = function(renderId) {
    util.object.forEach(manager.players_, function(playerInfo, id) {
      if (playerInfo.properties['render_id'] == renderId) {
        manager.removePlayer(id);
      }
    });
  };

  media.updateAudioComponent = function(component) {
    var uniqueComponentId = component.owner_id + ':' + component.component_id;
    switch (component.status) {
      case 'closed':
        manager.removeAudioComponent(
            component.component_type, uniqueComponentId);
        break;
      default:
        manager.updateAudioComponent(
            component.component_type, uniqueComponentId, component);
        break;
    }
  };

  media.onPlayerOpen = function(id, timestamp) {
    manager.addPlayer(id, timestamp);
  };

  media.onMediaEvent = function(event) {
    var source = event.renderer + ':' + event.player;

    // Although this gets called on every event, there is nothing we can do
    // because there is no onOpen event.
    media.onPlayerOpen(source);
    manager.updatePlayerInfoNoRecord(
        source, event.ticksMillis, 'render_id', event.renderer);
    manager.updatePlayerInfoNoRecord(
        source, event.ticksMillis, 'player_id', event.player);

    var propertyCount = 0;
    util.object.forEach(event.params, function(value, key) {
      key = key.trim();

      // These keys get spammed *a lot*, so put them on the display
      // but don't log list.
      if (key === 'buffer_start' ||
          key === 'buffer_end' ||
          key === 'buffer_current' ||
          key === 'is_downloading_data') {
        manager.updatePlayerInfoNoRecord(
            source, event.ticksMillis, key, value);
      } else {
        manager.updatePlayerInfo(source, event.ticksMillis, key, value);
      }
      propertyCount += 1;
    });

    if (propertyCount === 0) {
      manager.updatePlayerInfo(
          source, event.ticksMillis, 'event', event.type);
    }
  };

  // |chrome| is not defined during tests.
  if (window.chrome && window.chrome.send) {
    chrome.send('getEverything');
  }
  return media;
}());

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Some utility functions that don't belong anywhere else in the
 * code.
 */

var util = (function() {
  var util = {};
  util.object = {};
  /**
   * Calls a function for each element in an object/map/hash.
   *
   * @param obj The object to iterate over.
   * @param f The function to call on every value in the object.  F should have
   * the following arguments: f(value, key, object) where value is the value
   * of the property, key is the corresponding key, and obj is the object that
   * was passed in originally.
   * @param optObj The object use as 'this' within f.
   */
  util.object.forEach = function(obj, f, optObj) {
    'use strict';
    var key;
    for (key in obj) {
      if (obj.hasOwnProperty(key)) {
        f.call(optObj, obj[key], key, obj);
      }
    }
  };
  util.millisecondsToString = function(timeMillis) {
    function pad(num) {
      num = num.toString();
      if (num.length < 2) {
        return '0' + num;
      }
      return num;
    }

    var date = new Date(timeMillis);
    return pad(date.getUTCHours()) + ':' + pad(date.getUTCMinutes()) + ':' +
        pad(date.getUTCSeconds()) + ' ' + pad((date.getMilliseconds()) % 1000);
  };

  return util;
}());

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('media', function() {
  'use strict';

  /**
   * This class represents a file cached by net.
   */
  function CacheEntry() {
    this.read_ = new media.DisjointRangeSet;
    this.written_ = new media.DisjointRangeSet;
    this.available_ = new media.DisjointRangeSet;

    // Set to true when we know the entry is sparse.
    this.sparse = false;
    this.key = null;
    this.size = null;

    // The <details> element representing this CacheEntry.
    this.details_ = document.createElement('details');
    this.details_.className = 'cache-entry';
    this.details_.open = false;

    // The <details> summary line. It contains a chart of requested file ranges
    // and the url if we know it.
    var summary = document.createElement('summary');

    this.summaryText_ = document.createTextNode('');
    summary.appendChild(this.summaryText_);

    summary.appendChild(document.createTextNode(' '));

    // Controls to modify this CacheEntry.
    var controls = document.createElement('span');
    controls.className = 'cache-entry-controls';
    summary.appendChild(controls);
    summary.appendChild(document.createElement('br'));

    // A link to clear recorded data from this CacheEntry.
    var clearControl = document.createElement('a');
    clearControl.href = 'javascript:void(0)';
    clearControl.onclick = this.clear.bind(this);
    clearControl.textContent = '(clear entry)';
    controls.appendChild(clearControl);

    this.details_.appendChild(summary);

    // The canvas for drawing cache writes.
    this.writeCanvas = document.createElement('canvas');
    this.writeCanvas.width = media.BAR_WIDTH;
    this.writeCanvas.height = media.BAR_HEIGHT;
    this.details_.appendChild(this.writeCanvas);

    // The canvas for drawing cache reads.
    this.readCanvas = document.createElement('canvas');
    this.readCanvas.width = media.BAR_WIDTH;
    this.readCanvas.height = media.BAR_HEIGHT;
    this.details_.appendChild(this.readCanvas);

    // A tabular representation of the data in the above canvas.
    this.detailTable_ = document.createElement('table');
    this.detailTable_.className = 'cache-table';
    this.details_.appendChild(this.detailTable_);
  }

  CacheEntry.prototype = {
    /**
     * Mark a range of bytes as read from the cache.
     * @param {int} start The first byte read.
     * @param {int} length The number of bytes read.
     */
    readBytes: function(start, length) {
      start = parseInt(start);
      length = parseInt(length);
      this.read_.add(start, start + length);
      this.available_.add(start, start + length);
      this.sparse = true;
    },

    /**
     * Mark a range of bytes as written to the cache.
     * @param {int} start The first byte written.
     * @param {int} length The number of bytes written.
     */
    writeBytes: function(start, length) {
      start = parseInt(start);
      length = parseInt(length);
      this.written_.add(start, start + length);
      this.available_.add(start, start + length);
      this.sparse = true;
    },

    /**
     * Merge this CacheEntry with another, merging recorded ranges and flags.
     * @param {CacheEntry} other The CacheEntry to merge into this one.
     */
    merge: function(other) {
      this.read_.merge(other.read_);
      this.written_.merge(other.written_);
      this.available_.merge(other.available_);
      this.sparse = this.sparse || other.sparse;
      this.key = this.key || other.key;
      this.size = this.size || other.size;
    },

    /**
     * Clear all recorded ranges from this CacheEntry and redraw this.details_.
     */
    clear: function() {
      this.read_ = new media.DisjointRangeSet;
      this.written_ = new media.DisjointRangeSet;
      this.available_ = new media.DisjointRangeSet;
      this.generateDetails();
    },

    /**
     * Helper for drawCacheReadsToCanvas() and drawCacheWritesToCanvas().
     *
     * Accepts the entries to draw, a canvas fill style, and the canvas to
     * draw on.
     */
    drawCacheEntriesToCanvas: function(entries, fillStyle, canvas) {
      // Don't bother drawing anything if we don't know the total size.
      if (!this.size) {
        return;
      }

      var width = canvas.width;
      var height = canvas.height;
      var context = canvas.getContext('2d');
      var fileSize = this.size;

      context.fillStyle = '#aaa';
      context.fillRect(0, 0, width, height);

      function drawRange(start, end) {
        var left = start / fileSize * width;
        var right = end / fileSize * width;
        context.fillRect(left, 0, right - left, height);
      }

      context.fillStyle = fillStyle;
      entries.map(function(start, end) {
        drawRange(start, end);
      });
    },

    /**
     * Draw cache writes to the given canvas.
     *
     * It should consist of a horizontal bar with highlighted sections to
     * represent which parts of a file have been written to the cache.
     *
     * e.g. |xxxxxx----------x|
     */
    drawCacheWritesToCanvas: function(canvas) {
      this.drawCacheEntriesToCanvas(this.written_, '#00a', canvas);
    },

    /**
     * Draw cache reads to the given canvas.
     *
     * It should consist of a horizontal bar with highlighted sections to
     * represent which parts of a file have been read from the cache.
     *
     * e.g. |xxxxxx----------x|
     */
    drawCacheReadsToCanvas: function(canvas) {
      this.drawCacheEntriesToCanvas(this.read_, '#0a0', canvas);
    },

    /**
     * Update this.details_ to contain everything we currently know about
     * this file.
     */
    generateDetails: function() {
      function makeElement(tag, content) {
        var toReturn = document.createElement(tag);
        toReturn.textContent = content;
        return toReturn;
      }

      this.details_.id = this.key;
      this.summaryText_.textContent = this.key || 'Unknown File';

      this.detailTable_.textContent = '';
      var header = document.createElement('thead');
      var footer = document.createElement('tfoot');
      var body = document.createElement('tbody');
      this.detailTable_.appendChild(header);
      this.detailTable_.appendChild(footer);
      this.detailTable_.appendChild(body);

      var headerRow = document.createElement('tr');
      headerRow.appendChild(makeElement('th', 'Read From Cache'));
      headerRow.appendChild(makeElement('th', 'Written To Cache'));
      header.appendChild(headerRow);

      var footerRow = document.createElement('tr');
      var footerCell = document.createElement('td');
      footerCell.textContent = 'Out of ' + (this.size || 'unkown size');
      footerCell.setAttribute('colspan', 2);
      footerRow.appendChild(footerCell);
      footer.appendChild(footerRow);

      var read = this.read_.map(function(start, end) {
        return start + ' - ' + end;
      });
      var written = this.written_.map(function(start, end) {
        return start + ' - ' + end;
      });

      var length = Math.max(read.length, written.length);
      for (var i = 0; i < length; i++) {
        var row = document.createElement('tr');
        row.appendChild(makeElement('td', read[i] || ''));
        row.appendChild(makeElement('td', written[i] || ''));
        body.appendChild(row);
      }

      this.drawCacheWritesToCanvas(this.writeCanvas);
      this.drawCacheReadsToCanvas(this.readCanvas);
    },

    /**
     * Render this CacheEntry as a <li>.
     * @return {HTMLElement} A <li> representing this CacheEntry.
     */
    toListItem: function() {
      this.generateDetails();

      var result = document.createElement('li');
      result.appendChild(this.details_);
      return result;
    }
  };

  return {
    CacheEntry: CacheEntry
  };
});

// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('media', function() {

  /**
   * This class represents a collection of non-intersecting ranges. Ranges
   * specified by (start, end) can be added and removed at will. It is used to
   * record which sections of a media file have been cached, e.g. the first and
   * last few kB plus several MB in the middle.
   *
   * Example usage:
   * someRange.add(0, 100);     // Contains 0-100.
   * someRange.add(150, 200);   // Contains 0-100, 150-200.
   * someRange.remove(25, 75);  // Contains 0-24, 76-100, 150-200.
   * someRange.add(25, 149);    // Contains 0-200.
   */
  function DisjointRangeSet() {
    this.ranges_ = {};
  }

  DisjointRangeSet.prototype = {
    /**
     * Deletes all ranges intersecting with (start ... end) and returns the
     * extents of the cleared area.
     * @param {int} start The start of the range to remove.
     * @param {int} end The end of the range to remove.
     * @param {int} sloppiness 0 removes only strictly overlapping ranges, and
     *                         1 removes adjacent ones.
     * @return {Object} The start and end of the newly cleared range.
     */
    clearRange: function(start, end, sloppiness) {
      var ranges = this.ranges_;
      var result = {start: start, end: end};

      for (var rangeStart in this.ranges_) {
        rangeEnd = this.ranges_[rangeStart];
        // A range intersects another if its start lies within the other range
        // or vice versa.
        if ((rangeStart >= start && rangeStart <= (end + sloppiness)) ||
            (start >= rangeStart && start <= (rangeEnd + sloppiness))) {
          delete ranges[rangeStart];
          result.start = Math.min(result.start, rangeStart);
          result.end = Math.max(result.end, rangeEnd);
        }
      }

      return result;
    },

    /**
     * Adds a range to this DisjointRangeSet.
     * Joins adjacent and overlapping ranges together.
     * @param {int} start The beginning of the range to add, inclusive.
     * @param {int} end The end of the range to add, inclusive.
     */
    add: function(start, end) {
      if (end < start)
        return;

      // Remove all touching ranges.
      result = this.clearRange(start, end, 1);
      // Add back a single contiguous range.
      this.ranges_[Math.min(start, result.start)] = Math.max(end, result.end);
    },

    /**
     * Combines a DisjointRangeSet with this one.
     * @param {DisjointRangeSet} ranges A DisjointRangeSet to be squished into
     * this one.
     */
    merge: function(other) {
      var ranges = this;
      other.forEach(function(start, end) { ranges.add(start, end); });
    },

    /**
     * Removes a range from this DisjointRangeSet.
     * Will split existing ranges if necessary.
     * @param {int} start The beginning of the range to remove, inclusive.
     * @param {int} end The end of the range to remove, inclusive.
     */
    remove: function(start, end) {
      if (end < start)
        return;

      // Remove instersecting ranges.
      result = this.clearRange(start, end, 0);

      // Add back non-overlapping ranges.
      if (result.start < start)
        this.ranges_[result.start] = start - 1;
      if (result.end > end)
        this.ranges_[end + 1] = result.end;
    },

    /**
     * Iterates over every contiguous range in this DisjointRangeSet, calling a
     * function for each (start, end).
     * @param {function(int, int)} iterator The function to call on each range.
     */
    forEach: function(iterator) {
      for (var start in this.ranges_)
        iterator(start, this.ranges_[start]);
    },

    /**
     * Maps this DisjointRangeSet to an array by calling a given function on the
     * start and end of each contiguous range, sorted by start.
     * @param {function(int, int)} mapper Maps a range to an array element.
     * @return {Array} An array of each mapper(range).
     */
    map: function(mapper) {
      var starts = [];
      for (var start in this.ranges_)
        starts.push(parseInt(start));
      starts.sort(function(a, b) {
        return a - b;
      });

      var ranges = this.ranges_;
      var results = starts.map(function(s) {
        return mapper(s, ranges[s]);
      });

      return results;
    },

    /**
     * Finds the maximum value present in any of the contained ranges.
     * @return {int} The maximum value contained by this DisjointRangeSet.
     */
    max: function() {
      var max = -Infinity;
      for (var start in this.ranges_)
        max = Math.max(max, this.ranges_[start]);
      return max;
    },
  };

  return {
    DisjointRangeSet: DisjointRangeSet
  };
});

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview A class for keeping track of the details of a player.
 */

var PlayerInfo = (function() {
  'use strict';

  /**
   * A class that keeps track of properties on a media player.
   * @param id A unique id that can be used to identify this player.
   */
  function PlayerInfo(id) {
    this.id = id;
    // The current value of the properties for this player.
    this.properties = {};
    // All of the past (and present) values of the properties.
    this.pastValues = {};

    // Every single event in the order in which they were received.
    this.allEvents = [];
    this.lastRendered = 0;

    this.firstTimestamp_ = -1;
  }

  PlayerInfo.prototype = {
    /**
     * Adds or set a property on this player.
     * This is the default logging method as it keeps track of old values.
     * @param timestamp  The time in milliseconds since the Epoch.
     * @param key A String key that describes the property.
     * @param value The value of the property.
     */
    addProperty: function(timestamp, key, value) {
      // The first timestamp that we get will be recorded.
      // Then, all future timestamps are deltas of that.
      if (this.firstTimestamp_ === -1) {
        this.firstTimestamp_ = timestamp;
      }

      if (typeof key !== 'string') {
        throw new Error(typeof key + ' is not a valid key type');
      }

      this.properties[key] = value;

      if (!this.pastValues[key]) {
        this.pastValues[key] = [];
      }

      var recordValue = {
        time: timestamp - this.firstTimestamp_,
        key: key,
        value: value
      };

      this.pastValues[key].push(recordValue);
      this.allEvents.push(recordValue);
    },

    /**
     * Adds or set a property on this player.
     * Does not keep track of old values.  This is better for
     * values that get spammed repeatedly.
     * @param timestamp The time in milliseconds since the Epoch.
     * @param key A String key that describes the property.
     * @param value The value of the property.
     */
    addPropertyNoRecord: function(timestamp, key, value) {
      this.addProperty(timestamp, key, value);
      this.allEvents.pop();
    }
  };

  return PlayerInfo;
}());

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Keeps track of all the existing PlayerInfo and
 * audio stream objects and is the entry-point for messages from the backend.
 *
 * The events captured by Manager (add, remove, update) are relayed
 * to the clientRenderer which it can choose to use to modify the UI.
 */
var Manager = (function() {
  'use strict';

  function Manager(clientRenderer) {
    this.players_ = {};
    this.audioComponents_ = [];
    this.clientRenderer_ = clientRenderer;
  }

  Manager.prototype = {
    /**
     * Updates an audio-component.
     * @param componentType Integer AudioComponent enum value; must match values
     * from the AudioLogFactory::AudioComponent enum.
     * @param componentId The unique-id of the audio-component.
     * @param componentData The actual component data dictionary.
     */
    updateAudioComponent: function(componentType, componentId, componentData) {
      if (!(componentType in this.audioComponents_))
        this.audioComponents_[componentType] = {};
      if (!(componentId in this.audioComponents_[componentType])) {
        this.audioComponents_[componentType][componentId] = componentData;
      } else {
        for (var key in componentData) {
          this.audioComponents_[componentType][componentId][key] =
              componentData[key];
        }
      }
      this.clientRenderer_.audioComponentAdded(
          componentType, this.audioComponents_[componentType]);
    },

    /**
     * Removes an audio-stream from the manager.
     * @param id The unique-id of the audio-stream.
     */
    removeAudioComponent: function(componentType, componentId) {
      if (!(componentType in this.audioComponents_) ||
          !(componentId in this.audioComponents_[componentType])) {
        return;
      }

      delete this.audioComponents_[componentType][componentId];
      this.clientRenderer_.audioComponentRemoved(
          componentType, this.audioComponents_[componentType]);
    },

    /**
     * Adds a player to the list of players to manage.
     */
    addPlayer: function(id) {
      if (this.players_[id]) {
        return;
      }
      // Make the PlayerProperty and add it to the mapping
      this.players_[id] = new PlayerInfo(id);
      this.clientRenderer_.playerAdded(this.players_, this.players_[id]);
    },

    /**
     * Attempts to remove a player from the UI.
     * @param id The ID of the player to remove.
     */
    removePlayer: function(id) {
      var playerRemoved = this.players_[id];
      delete this.players_[id];
      this.clientRenderer_.playerRemoved(this.players_, playerRemoved);
    },

    updatePlayerInfoNoRecord: function(id, timestamp, key, value) {
      if (!this.players_[id]) {
        console.error('[updatePlayerInfo] Id ' + id + ' does not exist');
        return;
      }

      this.players_[id].addPropertyNoRecord(timestamp, key, value);
      this.clientRenderer_.playerUpdated(this.players_,
                                         this.players_[id],
                                         key,
                                         value);
    },

    /**
     *
     * @param id The unique ID that identifies the player to be updated.
     * @param timestamp The timestamp of when the change occured.  This
     * timestamp is *not* normalized.
     * @param key The name of the property to be added/changed.
     * @param value The value of the property.
     */
    updatePlayerInfo: function(id, timestamp, key, value) {
      if (!this.players_[id]) {
        console.error('[updatePlayerInfo] Id ' + id + ' does not exist');
        return;
      }

      this.players_[id].addProperty(timestamp, key, value);
      this.clientRenderer_.playerUpdated(this.players_,
                                         this.players_[id],
                                         key,
                                         value);
    },

    parseVideoCaptureFormat_: function(format) {
      /**
       * Example:
       *
       * format:
       *   "(160x120)@30.000fps, pixel format: PIXEL_FORMAT_I420, storage: CPU"
       *
       * formatDict:
       *   {'resolution':'1280x720', 'fps': '30.00', "storage: "CPU" }
       */
      var parts = format.split(', ');
      var formatDict = {};
      for (var i in parts) {
        var kv = parts[i].split(': ');
        if (kv.length == 2) {
          if (kv[0] == 'pixel format') {
            // The camera does not actually output I420,
            // so this info is misleading.
            continue;
          }
          formatDict[kv[0]] = kv[1];
        } else {
          kv = parts[i].split("@");
          if (kv.length == 2) {
            formatDict['resolution'] = kv[0].replace(/[)(]/g, '');
            // Round down the FPS to 2 decimals.
            formatDict['fps'] =
                parseFloat(kv[1].replace(/fps$/, '')).toFixed(2);
          }
        }
      }

      return formatDict;
    },

    updateVideoCaptureCapabilities: function(videoCaptureCapabilities) {
      // Parse the video formats to be structured for the table.
      for (var i in videoCaptureCapabilities) {
        for (var j in videoCaptureCapabilities[i]['formats']) {
          videoCaptureCapabilities[i]['formats'][j] =
              this.parseVideoCaptureFormat_(
                    videoCaptureCapabilities[i]['formats'][j]);
        }
      }

      // The keys of each device to be shown in order of appearance.
      var videoCaptureDeviceKeys = ['name','formats','captureApi','id'];

      this.clientRenderer_.redrawVideoCaptureCapabilities(
          videoCaptureCapabilities, videoCaptureDeviceKeys);
    }
  };

  return Manager;
}());

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var ClientRenderer = (function() {
  var ClientRenderer = function() {
    this.playerListElement = document.getElementById('player-list');
    this.audioPropertiesTable =
        document.getElementById('audio-property-table').querySelector('tbody');
    this.playerPropertiesTable =
        document.getElementById('player-property-table').querySelector('tbody');
    this.logTable = document.getElementById('log').querySelector('tbody');
    this.graphElement = document.getElementById('graphs');
    this.audioPropertyName = document.getElementById('audio-property-name');

    this.selectedPlayer = null;
    this.selectedAudioComponentType = null;
    this.selectedAudioComponentId = null;
    this.selectedAudioCompontentData = null;

    this.selectedPlayerLogIndex = 0;

    this.filterFunction = function() { return true; };
    this.filterText = document.getElementById('filter-text');
    this.filterText.onkeyup = this.onTextChange_.bind(this);

    this.bufferCanvas = document.createElement('canvas');
    this.bufferCanvas.width = media.BAR_WIDTH;
    this.bufferCanvas.height = media.BAR_HEIGHT;

    this.clipboardTextarea = document.getElementById('clipboard-textarea');
    var clipboardButtons = document.getElementsByClassName('copy-button');
    for (var i = 0; i < clipboardButtons.length; i++) {
      clipboardButtons[i].onclick = this.copyToClipboard_.bind(this);
    }

    this.hiddenKeys = ['component_id', 'component_type', 'owner_id'];

    // Tell CSS to hide certain content prior to making selections.
    document.body.classList.add(ClientRenderer.Css_.NO_PLAYERS_SELECTED);
    document.body.classList.add(ClientRenderer.Css_.NO_COMPONENTS_SELECTED);
  };

  /**
   * CSS classes added / removed in JS to trigger styling changes.
   * @private @enum {string}
   */
  ClientRenderer.Css_ = {
    NO_PLAYERS_SELECTED: 'no-players-selected',
    NO_COMPONENTS_SELECTED: 'no-components-selected',
    SELECTABLE_BUTTON: 'selectable-button'
  };

  function removeChildren(element) {
    while (element.hasChildNodes()) {
      element.removeChild(element.lastChild);
    }
  };

  function createSelectableButton(id, groupName, text, select_cb) {
    // For CSS styling.
    var radioButton = document.createElement('input');
    radioButton.classList.add(ClientRenderer.Css_.SELECTABLE_BUTTON);
    radioButton.type = 'radio';
    radioButton.id = id;
    radioButton.name = groupName;

    var buttonLabel = document.createElement('label');
    buttonLabel.classList.add(ClientRenderer.Css_.SELECTABLE_BUTTON);
    buttonLabel.setAttribute('for', radioButton.id);
    buttonLabel.appendChild(document.createTextNode(text));

    var fragment = document.createDocumentFragment();
    fragment.appendChild(radioButton);
    fragment.appendChild(buttonLabel);

    // Listen to 'change' rather than 'click' to keep styling in sync with
    // button behavior.
    radioButton.addEventListener('change', function() {
      select_cb();
    });

    return fragment;
  };

  function selectSelectableButton(id) {
    var element = document.getElementById(id);
    if (!element) {
      console.error('failed to select button with id: ' + id);
      return;
    }

    element.checked = true;
  }

  ClientRenderer.prototype = {
    /**
     * Called when an audio component is added to the collection.
     * @param componentType Integer AudioComponent enum value; must match values
     * from the AudioLogFactory::AudioComponent enum.
     * @param components The entire map of components (name -> dict).
     */
    audioComponentAdded: function(componentType, components) {
      this.redrawAudioComponentList_(componentType, components);

      // Redraw the component if it's currently selected.
      if (this.selectedAudioComponentType == componentType &&
          this.selectedAudioComponentId &&
          this.selectedAudioComponentId in components) {
        // TODO(chcunningham): This path is used both for adding and updating
        // the components. Split this up to have a separate update method.
        // At present, this selectAudioComponent call is key to *updating* the
        // the property table for existing audio components.
        this.selectAudioComponent_(
            componentType, this.selectedAudioComponentId,
            components[this.selectedAudioComponentId]);
      }
    },

    /**
     * Called when an audio component is removed from the collection.
     * @param componentType Integer AudioComponent enum value; must match values
     * from the AudioLogFactory::AudioComponent enum.
     * @param components The entire map of components (name -> dict).
     */
    audioComponentRemoved: function(componentType, components) {
      this.redrawAudioComponentList_(componentType, components);
    },

    /**
     * Called when a player is added to the collection.
     * @param players The entire map of id -> player.
     * @param player_added The player that is added.
     */
    playerAdded: function(players, playerAdded) {
      this.redrawPlayerList_(players);
    },

    /**
     * Called when a player is removed from the collection.
     * @param players The entire map of id -> player.
     * @param playerRemoved The player that was removed.
     */
    playerRemoved: function(players, playerRemoved) {
      if (playerRemoved === this.selectedPlayer) {
        removeChildren(this.playerPropertiesTable);
        removeChildren(this.logTable);
        removeChildren(this.graphElement);
        document.body.classList.add(ClientRenderer.Css_.NO_PLAYERS_SELECTED);
      }
      this.redrawPlayerList_(players);
    },

    /**
     * Called when a property on a player is changed.
     * @param players The entire map of id -> player.
     * @param player The player that had its property changed.
     * @param key The name of the property that was changed.
     * @param value The new value of the property.
     */
    playerUpdated: function(players, player, key, value) {
      if (player === this.selectedPlayer) {
        this.drawProperties_(player.properties, this.playerPropertiesTable);
        this.drawLog_();
        this.drawGraphs_();
      }
      if (key === 'name' || key === 'url') {
        this.redrawPlayerList_(players);
      }
    },

    createVideoCaptureFormatTable: function(formats) {
      if (!formats || formats.length == 0)
        return document.createTextNode('No formats');

      var table = document.createElement('table');
      var thead = document.createElement('thead');
      var theadRow = document.createElement('tr');
      for (var key in formats[0]) {
        var th = document.createElement('th');
        th.appendChild(document.createTextNode(key));
        theadRow.appendChild(th);
      }
      thead.appendChild(theadRow);
      table.appendChild(thead);
      var tbody = document.createElement('tbody');
      for (var i=0; i < formats.length; ++i) {
        var tr = document.createElement('tr')
        for (var key in formats[i]) {
          var td = document.createElement('td');
          td.appendChild(document.createTextNode(formats[i][key]));
          tr.appendChild(td);
        }
        tbody.appendChild(tr);
      }
      table.appendChild(tbody);
      table.classList.add('video-capture-formats-table');
      return table;
    },

    redrawVideoCaptureCapabilities: function(videoCaptureCapabilities, keys) {
      var copyButtonElement =
          document.getElementById('video-capture-capabilities-copy-button');
      copyButtonElement.onclick = function() {
        window.prompt('Copy to clipboard: Ctrl+C, Enter',
                      JSON.stringify(videoCaptureCapabilities))
      }

      var videoTableBodyElement  =
          document.getElementById('video-capture-capabilities-tbody');
      removeChildren(videoTableBodyElement);

      for (var component in videoCaptureCapabilities) {
        var tableRow =  document.createElement('tr');
        var device = videoCaptureCapabilities[ component ];
        for (var i in keys) {
          var value = device[keys[i]];
          var tableCell = document.createElement('td');
          var cellElement;
          if ((typeof value) == (typeof [])) {
            cellElement = this.createVideoCaptureFormatTable(value);
          } else {
            cellElement = document.createTextNode(
                ((typeof value) == 'undefined') ? 'n/a' : value);
          }
          tableCell.appendChild(cellElement)
          tableRow.appendChild(tableCell);
        }
        videoTableBodyElement.appendChild(tableRow);
      }
    },

    getAudioComponentName_ : function(componentType, id) {
      var baseName;
      switch (componentType) {
        case 0:
        case 1:
          baseName = 'Controller';
          break;
        case 2:
          baseName = 'Stream';
          break;
        default:
          baseName = 'UnknownType'
          console.error('Unrecognized component type: ' + componentType);
          break;
      }
      return baseName + ' ' + id;
    },

    getListElementForAudioComponent_ : function(componentType) {
      var listElement;
      switch (componentType) {
        case 0:
          listElement = document.getElementById(
              'audio-input-controller-list');
          break;
        case 1:
          listElement = document.getElementById(
              'audio-output-controller-list');
          break;
        case 2:
          listElement = document.getElementById(
              'audio-output-stream-list');
          break;
        default:
          console.error('Unrecognized component type: ' + componentType);
          listElement = null;
          break;
      }
      return listElement;
    },

    redrawAudioComponentList_: function(componentType, components) {
      // Group name imposes rule that only one component can be selected
      // (and have its properties displayed) at a time.
      var buttonGroupName = 'audio-components';

      var listElement = this.getListElementForAudioComponent_(componentType);
      if (!listElement) {
        console.error('Failed to find list element for component type: ' +
            componentType);
        return;
      }

      var fragment = document.createDocumentFragment();
      for (id in components) {
        var li = document.createElement('li');
        var button_cb = this.selectAudioComponent_.bind(
                this, componentType, id, components[id]);
        var friendlyName = this.getAudioComponentName_(componentType, id);
        li.appendChild(createSelectableButton(
            id, buttonGroupName, friendlyName, button_cb));
        fragment.appendChild(li);
      }
      removeChildren(listElement);
      listElement.appendChild(fragment);

      if (this.selectedAudioComponentType &&
          this.selectedAudioComponentType == componentType &&
          this.selectedAudioComponentId in components) {
        // Re-select the selected component since the button was just recreated.
        selectSelectableButton(this.selectedAudioComponentId);
      }
    },

    selectAudioComponent_: function(
          componentType, componentId, componentData) {
      document.body.classList.remove(
         ClientRenderer.Css_.NO_COMPONENTS_SELECTED);

      this.selectedAudioComponentType = componentType;
      this.selectedAudioComponentId = componentId;
      this.selectedAudioCompontentData = componentData;
      this.drawProperties_(componentData, this.audioPropertiesTable);

      removeChildren(this.audioPropertyName);
      this.audioPropertyName.appendChild(document.createTextNode(
          this.getAudioComponentName_(componentType, componentId)));
    },

    redrawPlayerList_: function(players) {
      // Group name imposes rule that only one component can be selected
      // (and have its properties displayed) at a time.
      var buttonGroupName = 'player-buttons';

      var fragment = document.createDocumentFragment();
      for (id in players) {
        var player = players[id];
        var usableName = player.properties.name ||
            player.properties.url ||
            'Player ' + player.id;

        var li = document.createElement('li');
        var button_cb = this.selectPlayer_.bind(this, player);
        li.appendChild(createSelectableButton(
            id, buttonGroupName, usableName, button_cb));
        fragment.appendChild(li);
      }
      removeChildren(this.playerListElement);
      this.playerListElement.appendChild(fragment);

      if (this.selectedPlayer && this.selectedPlayer.id in players) {
        // Re-select the selected player since the button was just recreated.
        selectSelectableButton(this.selectedPlayer.id);
      }
    },

    selectPlayer_: function(player) {
      document.body.classList.remove(ClientRenderer.Css_.NO_PLAYERS_SELECTED);

      this.selectedPlayer = player;
      this.selectedPlayerLogIndex = 0;
      this.selectedAudioComponentType = null;
      this.selectedAudioComponentId = null;
      this.selectedAudioCompontentData = null;
      this.drawProperties_(player.properties, this.playerPropertiesTable);

      removeChildren(this.logTable);
      removeChildren(this.graphElement);
      this.drawLog_();
      this.drawGraphs_();
    },

    drawProperties_: function(propertyMap, propertiesTable) {
      removeChildren(propertiesTable);
      var sortedKeys = Object.keys(propertyMap).sort();
      for (var i = 0; i < sortedKeys.length; ++i) {
        var key = sortedKeys[i];
        if (this.hiddenKeys.indexOf(key) >= 0)
          continue;

        var value = propertyMap[key];
        var row = propertiesTable.insertRow(-1);
        var keyCell = row.insertCell(-1);
        var valueCell = row.insertCell(-1);

        keyCell.appendChild(document.createTextNode(key));
        valueCell.appendChild(document.createTextNode(value));
      }
    },

    appendEventToLog_: function(event) {
      if (this.filterFunction(event.key)) {
        var row = this.logTable.insertRow(-1);

        var timestampCell = row.insertCell(-1);
        timestampCell.classList.add('timestamp');
        timestampCell.appendChild(document.createTextNode(
            util.millisecondsToString(event.time)));
        row.insertCell(-1).appendChild(document.createTextNode(event.key));
        row.insertCell(-1).appendChild(document.createTextNode(event.value));
      }
    },

    drawLog_: function() {
      var toDraw = this.selectedPlayer.allEvents.slice(
          this.selectedPlayerLogIndex);
      toDraw.forEach(this.appendEventToLog_.bind(this));
      this.selectedPlayerLogIndex = this.selectedPlayer.allEvents.length;
    },

    drawGraphs_: function() {
      function addToGraphs(name, graph, graphElement) {
        var li = document.createElement('li');
        li.appendChild(graph);
        li.appendChild(document.createTextNode(name));
        graphElement.appendChild(li);
      }

      var url = this.selectedPlayer.properties.url;
      if (!url) {
        return;
      }

      var cache = media.cacheForUrl(url);

      var player = this.selectedPlayer;
      var props = player.properties;

      var cacheExists = false;
      var bufferExists = false;

      if (props['buffer_start'] !== undefined &&
          props['buffer_current'] !== undefined &&
          props['buffer_end'] !== undefined &&
          props['total_bytes'] !== undefined) {
        this.drawBufferGraph_(props['buffer_start'],
                              props['buffer_current'],
                              props['buffer_end'],
                              props['total_bytes']);
        bufferExists = true;
      }

      if (cache) {
        if (player.properties['total_bytes']) {
          cache.size = Number(player.properties['total_bytes']);
        }
        cache.generateDetails();
        cacheExists = true;

      }

      if (!this.graphElement.hasChildNodes()) {
        if (bufferExists) {
          addToGraphs('buffer', this.bufferCanvas, this.graphElement);
        }
        if (cacheExists) {
          addToGraphs('cache read', cache.readCanvas, this.graphElement);
          addToGraphs('cache write', cache.writeCanvas, this.graphElement);
        }
      }
    },

    drawBufferGraph_: function(start, current, end, size) {
      var ctx = this.bufferCanvas.getContext('2d');
      var width = this.bufferCanvas.width;
      var height = this.bufferCanvas.height;
      ctx.fillStyle = '#aaa';
      ctx.fillRect(0, 0, width, height);

      var scale_factor = width / size;
      var left = start * scale_factor;
      var middle = current * scale_factor;
      var right = end * scale_factor;

      ctx.fillStyle = '#a0a';
      ctx.fillRect(left, 0, middle - left, height);
      ctx.fillStyle = '#aa0';
      ctx.fillRect(middle, 0, right - middle, height);
    },

    copyToClipboard_: function() {
      if (!this.selectedPlayer && !this.selectedAudioCompontentData) {
        return;
      }
      var properties = this.selectedAudioCompontentData ||
          this.selectedPlayer.properties;
      var stringBuffer = [];

      for (var key in properties) {
        var value = properties[key];
        stringBuffer.push(key.toString());
        stringBuffer.push(': ');
        stringBuffer.push(value.toString());
        stringBuffer.push('\n');
      }

      this.clipboardTextarea.value = stringBuffer.join('');
      this.clipboardTextarea.classList.remove('hiddenClipboard');
      this.clipboardTextarea.focus();
      this.clipboardTextarea.select();

      // Hide the clipboard element when it loses focus.
      this.clipboardTextarea.onblur = function(event) {
        setTimeout(function(element) {
          event.target.classList.add('hiddenClipboard');
        }, 0);
      };
    },

    onTextChange_: function(event) {
      var text = this.filterText.value.toLowerCase();
      var parts = text.split(',').map(function(part) {
        return part.trim();
      }).filter(function(part) {
        return part.trim().length > 0;
      });

      this.filterFunction = function(text) {
        text = text.toLowerCase();
        return parts.length === 0 || parts.some(function(part) {
          return text.indexOf(part) != -1;
        });
      };

      if (this.selectedPlayer) {
        removeChildren(this.logTable);
        this.selectedPlayerLogIndex = 0;
        this.drawLog_();
      }
    },
  };

  return ClientRenderer;
})();


media.initialize(new Manager(new ClientRenderer()));
cr.ui.decorate('tabbox', cr.ui.TabBox);
{
  "manifest_version":  1,
  "name": "mojo:catalog",
  "display_name": "Catalog",
  "capabilities": {
    "required": {
      "mojo:shell": { "classes": [ "all_users" ] }      
    }
  }
}
{"capabilities": {"required": {"*": {"interfaces": ["*"]}, "mojo:shell": {"interfaces": ["*"], "classes": ["client_process", "instance_name"]}}}, "display_name": "Content Browser", "name": "exe:content_browser", "manifest_version": 1}{"display_name": "Content Renderer", "name": "exe:content_renderer", "capabilities": {"mojo:mus": ["mus::mojom::Gpu"]}}<!doctype html>
<html>
<!--
Copyright 2015 The Chromium Authors. All rights reserved.
Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.
-->
<head>
  <meta charset="utf-8">
  <title>Network errors</title>
  <meta name="viewport" content="width=device-width">
  <link rel="stylesheet" href="chrome://resources/css/roboto.css">
  <link rel="stylesheet" href="chrome://resources/css/chrome_shared.css">
  <style>/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

body {
  margin: 10px 10px 30px;
}

ul {
  line-height: 1.7em;
  padding-left: 15px;
}

a {
  word-break: break-word;
}</style>
  <script src="chrome://resources/js/cr.js"></script>
  <script src="chrome://resources/js/load_time_data.js"></script>
  <script src="chrome://resources/js/util.js"></script>
  <script src="strings.js"></script>
  <script src="network_errors_listing.js"></script>
</head>
<body>
  <h1>Network errors</h1>
  <div id="pages" class="list"></div>

  <script src="chrome://resources/js/i18n_template.js"></script>
</body>
</html>// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('errorCodes', function() {
  'use strict';

  /**
   * Generate the page content.
   * @param {Array.<Object>} errorCodes Error codes array consisting of a
   *    numerical error ID and error code string.
   */
  function listErrorCodes(errorCodes) {
    var errorPageUrl = 'chrome://network-error/';
    var errorCodesList = document.createElement('ul');
    for (var i = 0; i < errorCodes.length; i++) {
      var listEl = document.createElement('li');
      var errorCodeLinkEl = document.createElement('a');
      errorCodeLinkEl.href = errorPageUrl + errorCodes[i].errorId;
      errorCodeLinkEl.textContent = errorCodes[i].errorCode + ' (' +
          errorCodes[i].errorId + ')';
      listEl.appendChild(errorCodeLinkEl);
      errorCodesList.appendChild(listEl);
    }
    $('pages').appendChild(errorCodesList);
  }

  function initialize() {
    var xhr = new XMLHttpRequest();
    xhr.open('GET', 'network-error-data.json');
    xhr.addEventListener('load', function(e) {
      if (xhr.status === 200) {
        try {
          var data = JSON.parse(xhr.responseText);
          listErrorCodes(data['errorCodes']);
        } catch (e) {
          $('pages').innerText = 'Could not parse the error codes data. ' +
              'Try reloading the page.';
        }
      }
    });
    xhr.send();
  }

  return {
    initialize: initialize
  };
});

document.addEventListener('DOMContentLoaded', errorCodes.initialize);/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

body {
  margin: 10px 10px 30px;
}

ul {
  line-height: 1.7em;
  padding-left: 15px;
}

a {
  word-break: break-word;
}<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <title>ServiceWorker</title>
  <link rel="stylesheet" href="chrome://resources/css/tabs.css">
  <link rel="stylesheet" href="chrome://resources/css/widgets.css">
  <style>/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.serviceworker-summary {
    background-color: rgb(235, 239, 249);
    border-top: 1px solid rgb(156, 194, 239);
    margin-bottom: 6px;
    margin-top: 12px;
    padding: 3px;
    font-weight: bold;
}

.serviceworker-item {
    margin-bottom: 15px;
    margin-top: 6px;
    position: relative;
}

.serviceworker-registration {
    padding: 5px;
}

.serviceworker-scope {
    color: rgb(85, 102, 221);
    display: inline-block;
    padding-bottom: 1px;
    padding-top: 4px;
    text-decoration: none;
    white-space: nowrap;
}

.serviceworker-version {
    padding-bottom: 3px;
    padding-left: 10px;
}

.controls a {
    -webkit-margin-end: 16px;
    color: #777;
}
</style>
</head>
<body i18n-values=".style.fontFamily:fontfamily;.style.fontSize:fontsize">
  <!-- templates -->
  <div style="display:none">
    <div id="serviceworker-version-template" class="serviceworker-version">
      <div class="serviceworker-status">
        <span>Installation Status:</span>
        <span jscontent="$this.status"></span>
      </div>
      <div class="serviceworker-running-status">
        <span>Running Status:</span>
        <span jscontent="$this.running_status"></span>
      </div>
      <div class="serviceworker-script_url">
        <span>Script:</span>
        <span jscontent="$this.script_url"></span>
      </div>
      <div class="serviceworker-vid">
        <span>Version ID:</span>
        <span jscontent="$this.version_id"></span>
      </div>
      <div class="serviceworker-pid">
        <span>Renderer process ID:</span>
        <span jscontent="$this.process_id"></span>
      </div>
      <div class="serviceworker-tid">
        <span>Renderer thread ID:</span>
        <span jscontent="$this.thread_id"></span>
      </div>
      <div class="serviceworker-rid">
        <span>DevTools agent route ID:</span>
        <span jscontent="$this.devtools_agent_route_id"></span>
      </div>
      <div>
        <div>Log:</div>
        <textarea class="serviceworker-log"
            jsvalues=".partition_id:$partition_id;.version_id:$this.version_id"
            rows="3" cols="120" readonly jscontent="$this.log"></textarea>
      </div>
      <div class="worker-controls">
        <button href="#" class="stop"
            jsvalues=".cmdArgs:{partition_id:$partition_id,version_id:version_id}"
            jsdisplay="$this.running_status == 'RUNNING'">Stop</button>
        <button href="#" class="inspect"
            jsvalues=".cmdArgs:{process_host_id:process_host_id,devtools_agent_route_id:devtools_agent_route_id}"
            jsdisplay="$this.running_status == 'RUNNING'">Inspect</button>
        <span class="operation-status" style="display: none">Running...</span>
      </div>
    </div>
    <div id="serviceworker-registration-template"
        class="serviceworker-registration">
      <div class="serviceworker-scope">
        <span>Scope:</span>
        <span jscontent="scope"></span>
      </div>
      <div class="serviceworker-rid">
        <span>Registration ID:</span>
        <span jscontent="registration_id"></span>
        <span jsdisplay="$this.unregistered">(unregistered)</span>
      </div>
      <div jsselect="$this.active">
        Active worker:
        <div transclude="serviceworker-version-template"></div>
      </div>
      <div jsselect="$this.waiting">
        Waiting worker:
        <div transclude="serviceworker-version-template"></div>
      </div>
      <div class="registration-controls" jsdisplay="!$this.unregistered">
        <button href="#" class="unregister"
            jsvalues=".cmdArgs:{partition_id:$partition_id,scope:scope}">
          Unregister
        </button>
        <button href="#" class="start"
            jsdisplay="$this.active.running_status != 'RUNNING'"
             jsvalues=".cmdArgs:{partition_id:$partition_id,scope:scope}">
          Start
        </button>
        <span class="operation-status" style="display: none">Running...</span>
      </div>
    </div>
    <div id="serviceworker-list-template"
        jsvalues="$partition_id:$this.partition_id;.partition_id:$this.partition_id"
        jsdisplay="$this.stored_registrations.length + $this.unregistered_registrations.length + $this.unregistered_versions.length > 0">
      <div class="serviceworker-summary">
        <span jsdisplay="$this.partition_path">
          <span>Registrations in: </span>
          <span jscontent="$this.partition_path"></span>
        </span>
        <span jsdisplay="!$this.partition_path">
          <span>Registrations: Incognito </span>
        </span>
        <span jscontent="'(' + $this.stored_registrations.length + ')'"></span>
      </div>
      <div class="serviceworker-item" jsselect="$this.stored_registrations">
        <div transclude="serviceworker-registration-template"></div>
      </div>
      <div class="serviceworker-item"
          jsselect="$this.unregistered_registrations">
        <div transclude="serviceworker-registration-template"></div>
      </div>
      <div class="serviceworker-item" jsselect="$this.unregistered_versions">
        Unregistered worker:
        <div transclude="serviceworker-version-template"></div>
      </div>
    </div>
    <div id="serviceworker-options-template">
      <div class="checkbox">
        <label>
            <input type="checkbox" class="debug_on_start"
                   jsvalues=".checked:$this.debug_on_start">
            <span>
              Open DevTools window and pause JavaScript execution on Service Worker startup for debugging.
            </span>
        </label>
      </div>
    </div>
  </div>
  <h1>ServiceWorker</h1>
  <div class="content">
    <div id="serviceworker-options"></div>
    <div id="serviceworker-list"></div>
  </div>
  <script src="chrome://resources/js/util.js"></script>
  <script src="chrome://resources/js/cr.js"></script>
  <script src="serviceworker_internals.js"></script>
  <script src="chrome://resources/js/load_time_data.js"></script>
  <script src="chrome://resources/js/jstemplate_compiled.js"></script>
  <script src="strings.js"></script>
  <script src="chrome://resources/js/i18n_template.js"></script>
</body>
</html>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('serviceworker', function() {
  'use strict';

  function initialize() {
    update();
  }

  function update() {
      chrome.send('GetOptions');
      chrome.send('getAllRegistrations');
  }

  function onOptions(options) {
    var template;
    var container = $('serviceworker-options');
    if (container.childNodes) {
      template = container.childNodes[0];
    }
    if (!template) {
      template = jstGetTemplate('serviceworker-options-template');
      container.appendChild(template);
    }
    jstProcess(new JsEvalContext(options), template);
    var inputs = container.querySelectorAll('input[type=\'checkbox\']');
    for (var i = 0; i < inputs.length; ++i) {
      if (!inputs[i].hasClickEvent) {
        inputs[i].addEventListener('click', (function(event) {
          chrome.send('SetOption',
                      [event.target.className, event.target.checked]);
        }).bind(this), false);
        inputs[i].hasClickEvent = true;
      }
    }
  }

  function progressNodeFor(link) {
    return link.parentNode.querySelector('.operation-status');
  }

  // All commands are completed with 'onOperationComplete'.
  var COMMANDS = ['stop', 'inspect', 'unregister', 'start'];
  function commandHandler(command) {
    return function(event) {
      var link = event.target;
      progressNodeFor(link).style.display = 'inline';
      sendCommand(command, link.cmdArgs, (function(status) {
        progressNodeFor(link).style.display = 'none';
      }).bind(null, link));
      return false;
    };
  };

  var commandCallbacks = [];
  function sendCommand(command, args, callback) {
    var callbackId = 0;
    while (callbackId in commandCallbacks) {
      callbackId++;
    }
    commandCallbacks[callbackId] = callback;
    chrome.send(command, [callbackId, args]);
  }

  // Fired from the backend after the command call has completed.
  function onOperationComplete(status, callbackId) {
    var callback = commandCallbacks[callbackId];
    delete commandCallbacks[callbackId];
    if (callback) {
      callback(status);
    }
    update();
  }

  var allLogMessages = {};
  // Set log for a worker version.
  function fillLogForVersion(container, partition_id, version) {
    if (!version) {
      return;
    }
    if (!(partition_id in allLogMessages)) {
      allLogMessages[partition_id] = {};
    }
    var logMessages = allLogMessages[partition_id];
    if (version.version_id in logMessages) {
      version.log = logMessages[version.version_id];
    } else {
      version.log = '';
    }
    var logAreas = container.querySelectorAll('textarea.serviceworker-log');
    for (var i = 0; i < logAreas.length; ++i) {
      var logArea = logAreas[i];
      if (logArea.partition_id == partition_id &&
          logArea.version_id == version.version_id) {
        logArea.value = version.log;
      }
    }
  }

  // Get the unregistered workers.
  // |unregistered_registrations| will be filled with the registrations which
  // are in |live_registrations| but not in |stored_registrations|.
  // |unregistered_versions| will be filled with the versions which
  // are in |live_versions| but not in |stored_registrations| nor in
  // |live_registrations|.
  function getUnregisteredWorkers(stored_registrations,
                                  live_registrations,
                                  live_versions,
                                  unregistered_registrations,
                                  unregistered_versions) {
    var registration_id_set = {};
    var version_id_set = {};
    stored_registrations.forEach(function(registration) {
      registration_id_set[registration.registration_id] = true;
    });
    [stored_registrations, live_registrations].forEach(function(registrations) {
      registrations.forEach(function(registration) {
        [registration.active, registration.waiting].forEach(function(version) {
          if (version) {
            version_id_set[version.version_id] = true;
          }
        });
      });
    });
    live_registrations.forEach(function(registration) {
      if (!registration_id_set[registration.registration_id]) {
        registration.unregistered = true;
        unregistered_registrations.push(registration);
      }
    });
    live_versions.forEach(function(version) {
      if (!version_id_set[version.version_id]) {
        unregistered_versions.push(version);
      }
    });
  }

  // Fired once per partition from the backend.
  function onPartitionData(live_registrations,
                           live_versions,
                           stored_registrations,
                           partition_id,
                           partition_path) {
    var unregistered_registrations = [];
    var unregistered_versions = [];
    getUnregisteredWorkers(stored_registrations,
                           live_registrations,
                           live_versions,
                           unregistered_registrations,
                           unregistered_versions);
    var template;
    var container = $('serviceworker-list');
    // Existing templates are keyed by partition_id. This allows
    // the UI to be updated in-place rather than refreshing the
    // whole page.
    for (var i = 0; i < container.childNodes.length; ++i) {
      if (container.childNodes[i].partition_id == partition_id) {
        template = container.childNodes[i];
      }
    }
    // This is probably the first time we're loading.
    if (!template) {
      template = jstGetTemplate('serviceworker-list-template');
      container.appendChild(template);
    }
    var fillLogFunc = fillLogForVersion.bind(this, container, partition_id);
    stored_registrations.forEach(function(registration) {
      [registration.active, registration.waiting].forEach(fillLogFunc);
    });
    unregistered_registrations.forEach(function(registration) {
      [registration.active, registration.waiting].forEach(fillLogFunc);
    });
    unregistered_versions.forEach(fillLogFunc);
    jstProcess(new JsEvalContext({
                 stored_registrations: stored_registrations,
                 unregistered_registrations: unregistered_registrations,
                 unregistered_versions: unregistered_versions,
                 partition_id: partition_id,
                 partition_path: partition_path}),
               template);
    for (var i = 0; i < COMMANDS.length; ++i) {
      var handler = commandHandler(COMMANDS[i]);
      var links = container.querySelectorAll('button.' + COMMANDS[i]);
      for (var j = 0; j < links.length; ++j) {
        if (!links[j].hasClickEvent) {
          links[j].addEventListener('click', handler, false);
          links[j].hasClickEvent = true;
        }
      }
    }
  }

  function onRunningStateChanged(partition_id, version_id) {
    update();
  }

  function onErrorReported(partition_id,
                           version_id,
                           process_id,
                           thread_id,
                           error_info) {
    outputLogMessage(partition_id,
                     version_id,
                     'Error: ' + JSON.stringify(error_info) + '\n');
  }

  function onConsoleMessageReported(partition_id,
                                    version_id,
                                    process_id,
                                    thread_id,
                                    message) {
    outputLogMessage(partition_id,
                     version_id,
                     'Console: ' + JSON.stringify(message) + '\n');
  }

  function onVersionStateChanged(partition_id, version_id) {
    update();
  }

  function onRegistrationStored(scope) {
    update();
  }

  function onRegistrationDeleted(scope) {
    update();
  }

  function outputLogMessage(partition_id, version_id, message) {
    if (!(partition_id in allLogMessages)) {
      allLogMessages[partition_id] = {};
    }
    var logMessages = allLogMessages[partition_id];
    if (version_id in logMessages) {
      logMessages[version_id] += message;
    } else {
      logMessages[version_id] = message;
    }

    var logAreas = document.querySelectorAll('textarea.serviceworker-log');
    for (var i = 0; i < logAreas.length; ++i) {
      var logArea = logAreas[i];
      if (logArea.partition_id == partition_id &&
          logArea.version_id == version_id) {
        logArea.value += message;
      }
    }
  }

  return {
    initialize: initialize,
    onOptions: onOptions,
    onOperationComplete: onOperationComplete,
    onPartitionData: onPartitionData,
    onRunningStateChanged: onRunningStateChanged,
    onErrorReported: onErrorReported,
    onConsoleMessageReported: onConsoleMessageReported,
    onVersionStateChanged: onVersionStateChanged,
    onRegistrationStored: onRegistrationStored,
    onRegistrationDeleted: onRegistrationDeleted,
  };
});

document.addEventListener('DOMContentLoaded', serviceworker.initialize);
/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.serviceworker-summary {
    background-color: rgb(235, 239, 249);
    border-top: 1px solid rgb(156, 194, 239);
    margin-bottom: 6px;
    margin-top: 12px;
    padding: 3px;
    font-weight: bold;
}

.serviceworker-item {
    margin-bottom: 15px;
    margin-top: 6px;
    position: relative;
}

.serviceworker-registration {
    padding: 5px;
}

.serviceworker-scope {
    color: rgb(85, 102, 221);
    display: inline-block;
    padding-bottom: 1px;
    padding-top: 4px;
    text-decoration: none;
    white-space: nowrap;
}

.serviceworker-version {
    padding-bottom: 3px;
    padding-left: 10px;
}

.controls a {
    -webkit-margin-end: 16px;
    color: #777;
}
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
  <head>
    <meta charset="utf-8">
    <title>WebRTC Internals</title>
    <style>/* Copyright (c) 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */


.peer-connection-dump-root {
  font-size: 0.8em;
  padding-bottom: 3px;
}

.update-log-container {
  float: left;
  width: 50em;
  overflow: auto;
}

.ssrc-info-block {
  color: #999;
  font-size: 0.8em;
}

.stats-graph-container {
  clear: both;
  margin: 0.5em 0 0.5em 0;
}

.stats-graph-sub-container {
  float: left;
  margin: 0.5em;
}

.stats-graph-sub-container > div {
  float: left;
}

.stats-graph-sub-container > div:first-child {
  float: none;
}

.stats-table-container {
  float: left;
  padding: 0 0 0 0;
  width: 50em;
  overflow: auto;
}

.stats-table-container >div:first-child {
  font-size: 0.8em;
  font-weight: bold;
  text-align: center;
  padding: 0 0 1em 0;
}

.stats-table-active-connection {
  font-weight: bold;
}

body {
  font-family: 'Lucida Grande', sans-serif;
}

table {
  border: none;
  margin: 0 1em 1em 0;
}

td {
  border: none;
  font-size: 0.8em;
  padding: 0 1em 0.5em 0;
  min-width: 10em;
  word-break: break-all;
}

table > tr {
  vertical-align: top;
}

th {
  border: none;
  font-size: 0.8em;
  padding: 0 0 0.5em 0;
}

.tab-head {
  background-color: rgb(220, 220, 220);
  margin: 10px 2px 0 2px;
  text-decoration: underline;
  cursor: pointer;
  display: inline-block;
  overflow: hidden;
  width: 20em;
  height: 3em;
}

.active-tab-head {
  background-color: turquoise;
  font-weight: bold;
}

.tab-body {
  border: 1px solid turquoise;
  border-top-width: 3px;
  padding: 0 10px 500px 10px;
  display: none;
}

.active-tab-body {
  display: block;
}

.user-media-request-div-class {
  background-color: lightgray;
  margin: 10px 0 10px 0;
}

.user-media-request-div-class > div {
  margin: 5px 0 5px 0;
}

.audio-recordings-info {
  max-width: 60em;
}
</style>
    <script src="chrome://resources/js/util.js"></script>
    <script src="webrtc_internals.js"></script>
  </head>
  <body>
    <p id='content-root'>
    </p>
  </body>
</html>
// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var USER_MEDIA_TAB_ID = 'user-media-tab-id';

var tabView = null;
var ssrcInfoManager = null;
var peerConnectionUpdateTable = null;
var statsTable = null;
var dumpCreator = null;
/** A map from peer connection id to the PeerConnectionRecord. */
var peerConnectionDataStore = {};
/** A list of getUserMedia requests. */
var userMediaRequests = [];

/** A simple class to store the updates and stats data for a peer connection. */
var PeerConnectionRecord = (function() {
  /** @constructor */
  function PeerConnectionRecord() {
    /** @private */
    this.record_ = {
      constraints: {},
      rtcConfiguration: [],
      stats: {},
      updateLog: [],
      url: '',
    };
  };

  PeerConnectionRecord.prototype = {
    /** @override */
    toJSON: function() {
      return this.record_;
    },

    /**
     * Adds the initilization info of the peer connection.
     * @param {string} url The URL of the web page owning the peer connection.
     * @param {Array} rtcConfiguration
     * @param {!Object} constraints Media constraints.
     */
    initialize: function(url, rtcConfiguration, constraints) {
      this.record_.url = url;
      this.record_.rtcConfiguration = rtcConfiguration;
      this.record_.constraints = constraints;
    },

    /**
     * @param {string} dataSeriesId The TimelineDataSeries identifier.
     * @return {!TimelineDataSeries}
     */
    getDataSeries: function(dataSeriesId) {
      return this.record_.stats[dataSeriesId];
    },

    /**
     * @param {string} dataSeriesId The TimelineDataSeries identifier.
     * @param {!TimelineDataSeries} dataSeries The TimelineDataSeries to set to.
     */
    setDataSeries: function(dataSeriesId, dataSeries) {
      this.record_.stats[dataSeriesId] = dataSeries;
    },

    /**
     * @param {!Object} update The object contains keys "time", "type", and
     *   "value".
     */
    addUpdate: function(update) {
      var time = new Date(parseFloat(update.time));
      this.record_.updateLog.push({
        time: time.toLocaleString(),
        type: update.type,
        value: update.value,
      });
    },
  };

  return PeerConnectionRecord;
})();

// The maximum number of data points bufferred for each stats. Old data points
// will be shifted out when the buffer is full.
var MAX_STATS_DATA_POINT_BUFFER_SIZE = 1000;

// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * A TabView provides the ability to create tabs and switch between tabs. It's
 * responsible for creating the DOM and managing the visibility of each tab.
 * The first added tab is active by default and the others hidden.
 */
var TabView = (function() {
  'use strict';

  /**
   * @constructor
   * @param {Element} root The root DOM element containing the tabs.
   */
  function TabView(root) {
    this.root_ = root;
    this.ACTIVE_TAB_HEAD_CLASS_ = 'active-tab-head';
    this.ACTIVE_TAB_BODY_CLASS_ = 'active-tab-body';
    this.TAB_HEAD_CLASS_ = 'tab-head';
    this.TAB_BODY_CLASS_ = 'tab-body';

    /**
     * A mapping for an id to the tab elements.
     * @type {!Object<!TabDom>}
     * @private
     */
    this.tabElements_ = {};

    this.headBar_ = null;
    this.activeTabId_ = null;
    this.initializeHeadBar_();
  }

  // Creates a simple object containing the tab head and body elements.
  function TabDom(h, b) {
    this.head = h;
    this.body = b;
  }

  TabView.prototype = {
    /**
     * Adds a tab with the specified id and title.
     * @param {string} id
     * @param {string} title
     * @return {!Element} The tab body element.
     */
    addTab: function(id, title) {
      if (this.tabElements_[id])
        throw 'Tab already exists: ' + id;

      var head = document.createElement('span');
      head.className = this.TAB_HEAD_CLASS_;
      head.textContent = title;
      head.title = title;
      this.headBar_.appendChild(head);
      head.addEventListener('click', this.switchTab_.bind(this, id));

      var body = document.createElement('div');
      body.className = this.TAB_BODY_CLASS_;
      body.id = id;
      this.root_.appendChild(body);

      this.tabElements_[id] = new TabDom(head, body);

      if (!this.activeTabId_) {
        this.switchTab_(id);
      }
      return this.tabElements_[id].body;
    },

    /** Removes the tab. @param {string} id */
    removeTab: function(id) {
      if (!this.tabElements_[id])
        return;
      this.tabElements_[id].head.parentNode.removeChild(
          this.tabElements_[id].head);
      this.tabElements_[id].body.parentNode.removeChild(
          this.tabElements_[id].body);

      delete this.tabElements_[id];
      if (this.activeTabId_ == id) {
        this.switchTab_(Object.keys(this.tabElements_)[0]);
      }
    },

    /**
     * Switches the specified tab into view.
     *
     * @param {string} activeId The id the of the tab that should be switched to
     *     active state.
     * @private
     */
    switchTab_: function(activeId) {
      if (this.activeTabId_ && this.tabElements_[this.activeTabId_]) {
        this.tabElements_[this.activeTabId_].body.classList.remove(
            this.ACTIVE_TAB_BODY_CLASS_);
        this.tabElements_[this.activeTabId_].head.classList.remove(
            this.ACTIVE_TAB_HEAD_CLASS_);
      }
      this.activeTabId_ = activeId;
      if (this.tabElements_[activeId]) {
        this.tabElements_[activeId].body.classList.add(
            this.ACTIVE_TAB_BODY_CLASS_);
        this.tabElements_[activeId].head.classList.add(
            this.ACTIVE_TAB_HEAD_CLASS_);
      }
    },

    /** Initializes the bar containing the tab heads. */
    initializeHeadBar_: function() {
      this.headBar_ = document.createElement('div');
      this.root_.appendChild(this.headBar_);
      this.headBar_.style.textAlign = 'center';
    },
  };
  return TabView;
})();

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * A TimelineDataSeries collects an ordered series of (time, value) pairs,
 * and converts them to graph points.  It also keeps track of its color and
 * current visibility state.
 * It keeps MAX_STATS_DATA_POINT_BUFFER_SIZE data points at most. Old data
 * points will be dropped when it reaches this size.
 */
var TimelineDataSeries = (function() {
  'use strict';

  /**
   * @constructor
   */
  function TimelineDataSeries() {
    // List of DataPoints in chronological order.
    this.dataPoints_ = [];

    // Default color.  Should always be overridden prior to display.
    this.color_ = 'red';
    // Whether or not the data series should be drawn.
    this.isVisible_ = true;

    this.cacheStartTime_ = null;
    this.cacheStepSize_ = 0;
    this.cacheValues_ = [];
  }

  TimelineDataSeries.prototype = {
    /**
     * @override
     */
    toJSON: function() {
      if (this.dataPoints_.length < 1)
        return {};

      var values = [];
      for (var i = 0; i < this.dataPoints_.length; ++i) {
        values.push(this.dataPoints_[i].value);
      }
      return {
        startTime: this.dataPoints_[0].time,
        endTime: this.dataPoints_[this.dataPoints_.length - 1].time,
        values: JSON.stringify(values),
      };
    },

    /**
     * Adds a DataPoint to |this| with the specified time and value.
     * DataPoints are assumed to be received in chronological order.
     */
    addPoint: function(timeTicks, value) {
      var time = new Date(timeTicks);
      this.dataPoints_.push(new DataPoint(time, value));

      if (this.dataPoints_.length > MAX_STATS_DATA_POINT_BUFFER_SIZE)
        this.dataPoints_.shift();
    },

    isVisible: function() {
      return this.isVisible_;
    },

    show: function(isVisible) {
      this.isVisible_ = isVisible;
    },

    getColor: function() {
      return this.color_;
    },

    setColor: function(color) {
      this.color_ = color;
    },

    getCount: function() {
      return this.dataPoints_.length;
    },
    /**
     * Returns a list containing the values of the data series at |count|
     * points, starting at |startTime|, and |stepSize| milliseconds apart.
     * Caches values, so showing/hiding individual data series is fast.
     */
    getValues: function(startTime, stepSize, count) {
      // Use cached values, if we can.
      if (this.cacheStartTime_ == startTime &&
          this.cacheStepSize_ == stepSize &&
          this.cacheValues_.length == count) {
        return this.cacheValues_;
      }

      // Do all the work.
      this.cacheValues_ = this.getValuesInternal_(startTime, stepSize, count);
      this.cacheStartTime_ = startTime;
      this.cacheStepSize_ = stepSize;

      return this.cacheValues_;
    },

    /**
     * Returns the cached |values| in the specified time period.
     */
    getValuesInternal_: function(startTime, stepSize, count) {
      var values = [];
      var nextPoint = 0;
      var currentValue = 0;
      var time = startTime;
      for (var i = 0; i < count; ++i) {
        while (nextPoint < this.dataPoints_.length &&
               this.dataPoints_[nextPoint].time < time) {
          currentValue = this.dataPoints_[nextPoint].value;
          ++nextPoint;
        }
        values[i] = currentValue;
        time += stepSize;
      }
      return values;
    }
  };

  /**
   * A single point in a data series.  Each point has a time, in the form of
   * milliseconds since the Unix epoch, and a numeric value.
   * @constructor
   */
  function DataPoint(time, value) {
    this.time = time;
    this.value = value;
  }

  return TimelineDataSeries;
})();

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.



/**
 * Get the ssrc if |report| is an ssrc report.
 *
 * @param {!Object} report The object contains id, type, and stats, where stats
 *     is the object containing timestamp and values, which is an array of
 *     strings, whose even index entry is the name of the stat, and the odd
 *     index entry is the value.
 * @return {?string} The ssrc.
 */
function GetSsrcFromReport(report) {
  if (report.type != 'ssrc') {
    console.warn("Trying to get ssrc from non-ssrc report.");
    return null;
  }

  // If the 'ssrc' name-value pair exists, return the value; otherwise, return
  // the report id.
  // The 'ssrc' name-value pair only exists in an upcoming Libjingle change. Old
  // versions use id to refer to the ssrc.
  //
  // TODO(jiayl): remove the fallback to id once the Libjingle change is rolled
  // to Chrome.
  if (report.stats && report.stats.values) {
    for (var i = 0; i < report.stats.values.length - 1; i += 2) {
      if (report.stats.values[i] == 'ssrc') {
        return report.stats.values[i + 1];
      }
    }
  }
  return report.id;
};

/**
 * SsrcInfoManager stores the ssrc stream info extracted from SDP.
 */
var SsrcInfoManager = (function() {
  'use strict';

  /**
   * @constructor
   */
  function SsrcInfoManager() {
    /**
     * Map from ssrc id to an object containing all the stream properties.
     * @type {!Object<!Object<string>>}
     * @private
     */
    this.streamInfoContainer_ = {};

    /**
     * The string separating attibutes in an SDP.
     * @type {string}
     * @const
     * @private
     */
    this.ATTRIBUTE_SEPARATOR_ = /[\r,\n]/;

    /**
     * The regex separating fields within an ssrc description.
     * @type {RegExp}
     * @const
     * @private
     */
    this.FIELD_SEPARATOR_REGEX_ = / .*:/;

    /**
     * The prefix string of an ssrc description.
     * @type {string}
     * @const
     * @private
     */
    this.SSRC_ATTRIBUTE_PREFIX_ = 'a=ssrc:';

    /**
     * The className of the ssrc info parent element.
     * @type {string}
     * @const
     */
    this.SSRC_INFO_BLOCK_CLASS = 'ssrc-info-block';
  }

  SsrcInfoManager.prototype = {
    /**
     * Extracts the stream information from |sdp| and saves it.
     * For example:
     *     a=ssrc:1234 msid:abcd
     *     a=ssrc:1234 label:hello
     *
     * @param {string} sdp The SDP string.
     */
    addSsrcStreamInfo: function(sdp) {
      var attributes = sdp.split(this.ATTRIBUTE_SEPARATOR_);
      for (var i = 0; i < attributes.length; ++i) {
        // Check if this is a ssrc attribute.
        if (attributes[i].indexOf(this.SSRC_ATTRIBUTE_PREFIX_) != 0)
          continue;

        var nextFieldIndex = attributes[i].search(this.FIELD_SEPARATOR_REGEX_);

        if (nextFieldIndex == -1)
          continue;

        var ssrc = attributes[i].substring(this.SSRC_ATTRIBUTE_PREFIX_.length,
                                           nextFieldIndex);
        if (!this.streamInfoContainer_[ssrc])
          this.streamInfoContainer_[ssrc] = {};

        // Make |rest| starting at the next field.
        var rest = attributes[i].substring(nextFieldIndex + 1);
        var name, value;
        while (rest.length > 0) {
          nextFieldIndex = rest.search(this.FIELD_SEPARATOR_REGEX_);
          if (nextFieldIndex == -1)
            nextFieldIndex = rest.length;

          // The field name is the string before the colon.
          name = rest.substring(0, rest.indexOf(':'));
          // The field value is from after the colon to the next field.
          value = rest.substring(rest.indexOf(':') + 1, nextFieldIndex);
          this.streamInfoContainer_[ssrc][name] = value;

          // Move |rest| to the start of the next field.
          rest = rest.substring(nextFieldIndex + 1);
        }
      }
    },

    /**
     * @param {string} sdp The ssrc id.
     * @return {!Object<string>} The object containing the ssrc infomation.
     */
    getStreamInfo: function(ssrc) {
      return this.streamInfoContainer_[ssrc];
    },

    /**
     * Populate the ssrc information into |parentElement|, each field as a
     * DIV element.
     *
     * @param {!Element} parentElement The parent element for the ssrc info.
     * @param {string} ssrc The ssrc id.
     */
    populateSsrcInfo: function(parentElement, ssrc) {
      if (!this.streamInfoContainer_[ssrc])
        return;

      parentElement.className = this.SSRC_INFO_BLOCK_CLASS;

      var fieldElement;
      for (var property in this.streamInfoContainer_[ssrc]) {
        fieldElement = document.createElement('div');
        parentElement.appendChild(fieldElement);
        fieldElement.textContent =
            property + ':' + this.streamInfoContainer_[ssrc][property];
      }
    }
  };

  return SsrcInfoManager;
})();

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

//
// This file contains helper methods to draw the stats timeline graphs.
// Each graph represents a series of stats report for a PeerConnection,
// e.g. 1234-0-ssrc-abcd123-bytesSent is the graph for the series of bytesSent
// for ssrc-abcd123 of PeerConnection 0 in process 1234.
// The graphs are drawn as CANVAS, grouped per report type per PeerConnection.
// Each group has an expand/collapse button and is collapsed initially.
//

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * A TimelineGraphView displays a timeline graph on a canvas element.
 */
var TimelineGraphView = (function() {
  'use strict';

  // Maximum number of labels placed vertically along the sides of the graph.
  var MAX_VERTICAL_LABELS = 6;

  // Vertical spacing between labels and between the graph and labels.
  var LABEL_VERTICAL_SPACING = 4;
  // Horizontal spacing between vertically placed labels and the edges of the
  // graph.
  var LABEL_HORIZONTAL_SPACING = 3;
  // Horizintal spacing between two horitonally placed labels along the bottom
  // of the graph.
  var LABEL_LABEL_HORIZONTAL_SPACING = 25;

  // Length of ticks, in pixels, next to y-axis labels.  The x-axis only has
  // one set of labels, so it can use lines instead.
  var Y_AXIS_TICK_LENGTH = 10;

  var GRID_COLOR = '#CCC';
  var TEXT_COLOR = '#000';
  var BACKGROUND_COLOR = '#FFF';

  var MAX_DECIMAL_PRECISION = 2;
  /**
   * @constructor
   */
  function TimelineGraphView(divId, canvasId) {
    this.scrollbar_ = {position_: 0, range_: 0};

    this.graphDiv_ = $(divId);
    this.canvas_ = $(canvasId);

    // Set the range and scale of the graph.  Times are in milliseconds since
    // the Unix epoch.

    // All measurements we have must be after this time.
    this.startTime_ = 0;
    // The current rightmost position of the graph is always at most this.
    this.endTime_ = 1;

    this.graph_ = null;

    // Horizontal scale factor, in terms of milliseconds per pixel.
    this.scale_ = 1000;

    // Initialize the scrollbar.
    this.updateScrollbarRange_(true);
  }

  TimelineGraphView.prototype = {
    setScale: function(scale) {
      this.scale_ = scale;
    },

    // Returns the total length of the graph, in pixels.
    getLength_: function() {
      var timeRange = this.endTime_ - this.startTime_;
      // Math.floor is used to ignore the last partial area, of length less
      // than this.scale_.
      return Math.floor(timeRange / this.scale_);
    },

    /**
     * Returns true if the graph is scrolled all the way to the right.
     */
    graphScrolledToRightEdge_: function() {
      return this.scrollbar_.position_ == this.scrollbar_.range_;
    },

    /**
     * Update the range of the scrollbar.  If |resetPosition| is true, also
     * sets the slider to point at the rightmost position and triggers a
     * repaint.
     */
    updateScrollbarRange_: function(resetPosition) {
      var scrollbarRange = this.getLength_() - this.canvas_.width;
      if (scrollbarRange < 0)
        scrollbarRange = 0;

      // If we've decreased the range to less than the current scroll position,
      // we need to move the scroll position.
      if (this.scrollbar_.position_ > scrollbarRange)
        resetPosition = true;

      this.scrollbar_.range_ = scrollbarRange;
      if (resetPosition) {
        this.scrollbar_.position_ = scrollbarRange;
        this.repaint();
      }
    },

    /**
     * Sets the date range displayed on the graph, switches to the default
     * scale factor, and moves the scrollbar all the way to the right.
     */
    setDateRange: function(startDate, endDate) {
      this.startTime_ = startDate.getTime();
      this.endTime_ = endDate.getTime();

      // Safety check.
      if (this.endTime_ <= this.startTime_)
        this.startTime_ = this.endTime_ - 1;

      this.updateScrollbarRange_(true);
    },

    /**
     * Updates the end time at the right of the graph to be the current time.
     * Specifically, updates the scrollbar's range, and if the scrollbar is
     * all the way to the right, keeps it all the way to the right.  Otherwise,
     * leaves the view as-is and doesn't redraw anything.
     */
    updateEndDate: function(opt_date) {
      this.endTime_ = opt_date || (new Date()).getTime();
      this.updateScrollbarRange_(this.graphScrolledToRightEdge_());
    },

    getStartDate: function() {
      return new Date(this.startTime_);
    },

    /**
     * Replaces the current TimelineDataSeries with |dataSeries|.
     */
    setDataSeries: function(dataSeries) {
      // Simply recreates the Graph.
      this.graph_ = new Graph();
      for (var i = 0; i < dataSeries.length; ++i)
        this.graph_.addDataSeries(dataSeries[i]);
      this.repaint();
    },

    /**
    * Adds |dataSeries| to the current graph.
    */
    addDataSeries: function(dataSeries) {
      if (!this.graph_)
        this.graph_ = new Graph();
      this.graph_.addDataSeries(dataSeries);
      this.repaint();
    },

    /**
     * Draws the graph on |canvas_|.
     */
    repaint: function() {
      this.repaintTimerRunning_ = false;

      var width = this.canvas_.width;
      var height = this.canvas_.height;
      var context = this.canvas_.getContext('2d');

      // Clear the canvas.
      context.fillStyle = BACKGROUND_COLOR;
      context.fillRect(0, 0, width, height);

      // Try to get font height in pixels.  Needed for layout.
      var fontHeightString = context.font.match(/([0-9]+)px/)[1];
      var fontHeight = parseInt(fontHeightString);

      // Safety check, to avoid drawing anything too ugly.
      if (fontHeightString.length == 0 || fontHeight <= 0 ||
          fontHeight * 4 > height || width < 50) {
        return;
      }

      // Save current transformation matrix so we can restore it later.
      context.save();

      // The center of an HTML canvas pixel is technically at (0.5, 0.5).  This
      // makes near straight lines look bad, due to anti-aliasing.  This
      // translation reduces the problem a little.
      context.translate(0.5, 0.5);

      // Figure out what time values to display.
      var position = this.scrollbar_.position_;
      // If the entire time range is being displayed, align the right edge of
      // the graph to the end of the time range.
      if (this.scrollbar_.range_ == 0)
        position = this.getLength_() - this.canvas_.width;
      var visibleStartTime = this.startTime_ + position * this.scale_;

      // Make space at the bottom of the graph for the time labels, and then
      // draw the labels.
      var textHeight = height;
      height -= fontHeight + LABEL_VERTICAL_SPACING;
      this.drawTimeLabels(context, width, height, textHeight, visibleStartTime);

      // Draw outline of the main graph area.
      context.strokeStyle = GRID_COLOR;
      context.strokeRect(0, 0, width - 1, height - 1);

      if (this.graph_) {
        // Layout graph and have them draw their tick marks.
        this.graph_.layout(
            width, height, fontHeight, visibleStartTime, this.scale_);
        this.graph_.drawTicks(context);

        // Draw the lines of all graphs, and then draw their labels.
        this.graph_.drawLines(context);
        this.graph_.drawLabels(context);
      }

      // Restore original transformation matrix.
      context.restore();
    },

    /**
     * Draw time labels below the graph.  Takes in start time as an argument
     * since it may not be |startTime_|, when we're displaying the entire
     * time range.
     */
    drawTimeLabels: function(context, width, height, textHeight, startTime) {
      // Draw the labels 1 minute apart.
      var timeStep = 1000 * 60;

      // Find the time for the first label.  This time is a perfect multiple of
      // timeStep because of how UTC times work.
      var time = Math.ceil(startTime / timeStep) * timeStep;

      context.textBaseline = 'bottom';
      context.textAlign = 'center';
      context.fillStyle = TEXT_COLOR;
      context.strokeStyle = GRID_COLOR;

      // Draw labels and vertical grid lines.
      while (true) {
        var x = Math.round((time - startTime) / this.scale_);
        if (x >= width)
          break;
        var text = (new Date(time)).toLocaleTimeString();
        context.fillText(text, x, textHeight);
        context.beginPath();
        context.lineTo(x, 0);
        context.lineTo(x, height);
        context.stroke();
        time += timeStep;
      }
    },

    getDataSeriesCount: function() {
      if (this.graph_)
        return this.graph_.dataSeries_.length;
      return 0;
    },

    hasDataSeries: function(dataSeries) {
      if (this.graph_)
        return this.graph_.hasDataSeries(dataSeries);
      return false;
    },

  };

  /**
   * A Graph is responsible for drawing all the TimelineDataSeries that have
   * the same data type.  Graphs are responsible for scaling the values, laying
   * out labels, and drawing both labels and lines for its data series.
   */
  var Graph = (function() {
    /**
     * @constructor
     */
    function Graph() {
      this.dataSeries_ = [];

      // Cached properties of the graph, set in layout.
      this.width_ = 0;
      this.height_ = 0;
      this.fontHeight_ = 0;
      this.startTime_ = 0;
      this.scale_ = 0;

      // The lowest/highest values adjusted by the vertical label step size
      // in the displayed range of the graph. Used for scaling and setting
      // labels.  Set in layoutLabels.
      this.min_ = 0;
      this.max_ = 0;

      // Cached text of equally spaced labels.  Set in layoutLabels.
      this.labels_ = [];
    }

    /**
     * A Label is the label at a particular position along the y-axis.
     * @constructor
     */
    function Label(height, text) {
      this.height = height;
      this.text = text;
    }

    Graph.prototype = {
      addDataSeries: function(dataSeries) {
        this.dataSeries_.push(dataSeries);
      },

      hasDataSeries: function(dataSeries) {
        for (var i = 0; i < this.dataSeries_.length; ++i) {
          if (this.dataSeries_[i] == dataSeries)
            return true;
        }
        return false;
      },

      /**
       * Returns a list of all the values that should be displayed for a given
       * data series, using the current graph layout.
       */
      getValues: function(dataSeries) {
        if (!dataSeries.isVisible())
          return null;
        return dataSeries.getValues(this.startTime_, this.scale_, this.width_);
      },

      /**
       * Updates the graph's layout.  In particular, both the max value and
       * label positions are updated.  Must be called before calling any of the
       * drawing functions.
       */
      layout: function(width, height, fontHeight, startTime, scale) {
        this.width_ = width;
        this.height_ = height;
        this.fontHeight_ = fontHeight;
        this.startTime_ = startTime;
        this.scale_ = scale;

        // Find largest value.
        var max = 0, min = 0;
        for (var i = 0; i < this.dataSeries_.length; ++i) {
          var values = this.getValues(this.dataSeries_[i]);
          if (!values)
            continue;
          for (var j = 0; j < values.length; ++j) {
            if (values[j] > max)
              max = values[j];
            else if (values[j] < min)
              min = values[j];
          }
        }

        this.layoutLabels_(min, max);
      },

      /**
       * Lays out labels and sets |max_|/|min_|, taking the time units into
       * consideration.  |maxValue| is the actual maximum value, and
       * |max_| will be set to the value of the largest label, which
       * will be at least |maxValue|. Similar for |min_|.
       */
      layoutLabels_: function(minValue, maxValue) {
        if (maxValue - minValue < 1024) {
          this.layoutLabelsBasic_(minValue, maxValue, MAX_DECIMAL_PRECISION);
          return;
        }

        // Find appropriate units to use.
        var units = ['', 'k', 'M', 'G', 'T', 'P'];
        // Units to use for labels.  0 is '1', 1 is K, etc.
        // We start with 1, and work our way up.
        var unit = 1;
        minValue /= 1024;
        maxValue /= 1024;
        while (units[unit + 1] && maxValue - minValue >= 1024) {
          minValue /= 1024;
          maxValue /= 1024;
          ++unit;
        }

        // Calculate labels.
        this.layoutLabelsBasic_(minValue, maxValue, MAX_DECIMAL_PRECISION);

        // Append units to labels.
        for (var i = 0; i < this.labels_.length; ++i)
          this.labels_[i] += ' ' + units[unit];

        // Convert |min_|/|max_| back to unit '1'.
        this.min_ *= Math.pow(1024, unit);
        this.max_ *= Math.pow(1024, unit);
      },

      /**
       * Same as layoutLabels_, but ignores units.  |maxDecimalDigits| is the
       * maximum number of decimal digits allowed.  The minimum allowed
       * difference between two adjacent labels is 10^-|maxDecimalDigits|.
       */
      layoutLabelsBasic_: function(minValue, maxValue, maxDecimalDigits) {
        this.labels_ = [];
        var range = maxValue - minValue;
        // No labels if the range is 0.
        if (range == 0) {
          this.min_ = this.max_ = maxValue;
          return;
        }

        // The maximum number of equally spaced labels allowed.  |fontHeight_|
        // is doubled because the top two labels are both drawn in the same
        // gap.
        var minLabelSpacing = 2 * this.fontHeight_ + LABEL_VERTICAL_SPACING;

        // The + 1 is for the top label.
        var maxLabels = 1 + this.height_ / minLabelSpacing;
        if (maxLabels < 2) {
          maxLabels = 2;
        } else if (maxLabels > MAX_VERTICAL_LABELS) {
          maxLabels = MAX_VERTICAL_LABELS;
        }

        // Initial try for step size between conecutive labels.
        var stepSize = Math.pow(10, -maxDecimalDigits);
        // Number of digits to the right of the decimal of |stepSize|.
        // Used for formating label strings.
        var stepSizeDecimalDigits = maxDecimalDigits;

        // Pick a reasonable step size.
        while (true) {
          // If we use a step size of |stepSize| between labels, we'll need:
          //
          // Math.ceil(range / stepSize) + 1
          //
          // labels.  The + 1 is because we need labels at both at 0 and at
          // the top of the graph.

          // Check if we can use steps of size |stepSize|.
          if (Math.ceil(range / stepSize) + 1 <= maxLabels)
            break;
          // Check |stepSize| * 2.
          if (Math.ceil(range / (stepSize * 2)) + 1 <= maxLabels) {
            stepSize *= 2;
            break;
          }
          // Check |stepSize| * 5.
          if (Math.ceil(range / (stepSize * 5)) + 1 <= maxLabels) {
            stepSize *= 5;
            break;
          }
          stepSize *= 10;
          if (stepSizeDecimalDigits > 0)
            --stepSizeDecimalDigits;
        }

        // Set the min/max so it's an exact multiple of the chosen step size.
        this.max_ = Math.ceil(maxValue / stepSize) * stepSize;
        this.min_ = Math.floor(minValue / stepSize) * stepSize;

        // Create labels.
        for (var label = this.max_; label >= this.min_; label -= stepSize)
          this.labels_.push(label.toFixed(stepSizeDecimalDigits));
      },

      /**
       * Draws tick marks for each of the labels in |labels_|.
       */
      drawTicks: function(context) {
        var x1;
        var x2;
        x1 = this.width_ - 1;
        x2 = this.width_ - 1 - Y_AXIS_TICK_LENGTH;

        context.fillStyle = GRID_COLOR;
        context.beginPath();
        for (var i = 1; i < this.labels_.length - 1; ++i) {
          // The rounding is needed to avoid ugly 2-pixel wide anti-aliased
          // lines.
          var y = Math.round(this.height_ * i / (this.labels_.length - 1));
          context.moveTo(x1, y);
          context.lineTo(x2, y);
        }
        context.stroke();
      },

      /**
       * Draws a graph line for each of the data series.
       */
      drawLines: function(context) {
        // Factor by which to scale all values to convert them to a number from
        // 0 to height - 1.
        var scale = 0;
        var bottom = this.height_ - 1;
        if (this.max_)
          scale = bottom / (this.max_ - this.min_);

        // Draw in reverse order, so earlier data series are drawn on top of
        // subsequent ones.
        for (var i = this.dataSeries_.length - 1; i >= 0; --i) {
          var values = this.getValues(this.dataSeries_[i]);
          if (!values)
            continue;
          context.strokeStyle = this.dataSeries_[i].getColor();
          context.beginPath();
          for (var x = 0; x < values.length; ++x) {
            // The rounding is needed to avoid ugly 2-pixel wide anti-aliased
            // horizontal lines.
            context.lineTo(
                x, bottom - Math.round((values[x] - this.min_) * scale));
          }
          context.stroke();
        }
      },

      /**
       * Draw labels in |labels_|.
       */
      drawLabels: function(context) {
        if (this.labels_.length == 0)
          return;
        var x = this.width_ - LABEL_HORIZONTAL_SPACING;

        // Set up the context.
        context.fillStyle = TEXT_COLOR;
        context.textAlign = 'right';

        // Draw top label, which is the only one that appears below its tick
        // mark.
        context.textBaseline = 'top';
        context.fillText(this.labels_[0], x, 0);

        // Draw all the other labels.
        context.textBaseline = 'bottom';
        var step = (this.height_ - 1) / (this.labels_.length - 1);
        for (var i = 1; i < this.labels_.length; ++i)
          context.fillText(this.labels_[i], x, step * i);
      }
    };

    return Graph;
  })();

  return TimelineGraphView;
})();


var STATS_GRAPH_CONTAINER_HEADING_CLASS = 'stats-graph-container-heading';

var RECEIVED_PROPAGATION_DELTA_LABEL =
    'googReceivedPacketGroupPropagationDeltaDebug';
var RECEIVED_PACKET_GROUP_ARRIVAL_TIME_LABEL =
    'googReceivedPacketGroupArrivalTimeDebug';

// Specifies which stats should be drawn on the 'bweCompound' graph and how.
var bweCompoundGraphConfig = {
  googAvailableSendBandwidth: {color: 'red'},
  googTargetEncBitrateCorrected: {color: 'purple'},
  googActualEncBitrate: {color: 'orange'},
  googRetransmitBitrate: {color: 'blue'},
  googTransmitBitrate: {color: 'green'},
};

// Converts the last entry of |srcDataSeries| from the total amount to the
// amount per second.
var totalToPerSecond = function(srcDataSeries) {
  var length = srcDataSeries.dataPoints_.length;
  if (length >= 2) {
    var lastDataPoint = srcDataSeries.dataPoints_[length - 1];
    var secondLastDataPoint = srcDataSeries.dataPoints_[length - 2];
    return (lastDataPoint.value - secondLastDataPoint.value) * 1000 /
           (lastDataPoint.time - secondLastDataPoint.time);
  }

  return 0;
};

// Converts the value of total bytes to bits per second.
var totalBytesToBitsPerSecond = function(srcDataSeries) {
  return totalToPerSecond(srcDataSeries) * 8;
};

// Specifies which stats should be converted before drawn and how.
// |convertedName| is the name of the converted value, |convertFunction|
// is the function used to calculate the new converted value based on the
// original dataSeries.
var dataConversionConfig = {
  packetsSent: {
    convertedName: 'packetsSentPerSecond',
    convertFunction: totalToPerSecond,
  },
  bytesSent: {
    convertedName: 'bitsSentPerSecond',
    convertFunction: totalBytesToBitsPerSecond,
  },
  packetsReceived: {
    convertedName: 'packetsReceivedPerSecond',
    convertFunction: totalToPerSecond,
  },
  bytesReceived: {
    convertedName: 'bitsReceivedPerSecond',
    convertFunction: totalBytesToBitsPerSecond,
  },
  // This is due to a bug of wrong units reported for googTargetEncBitrate.
  // TODO (jiayl): remove this when the unit bug is fixed.
  googTargetEncBitrate: {
    convertedName: 'googTargetEncBitrateCorrected',
    convertFunction: function (srcDataSeries) {
      var length = srcDataSeries.dataPoints_.length;
      var lastDataPoint = srcDataSeries.dataPoints_[length - 1];
      if (lastDataPoint.value < 5000)
        return lastDataPoint.value * 1000;
      return lastDataPoint.value;
    }
  }
};


// The object contains the stats names that should not be added to the graph,
// even if they are numbers.
var statsNameBlackList = {
  'ssrc': true,
  'googTrackId': true,
  'googComponent': true,
  'googLocalAddress': true,
  'googRemoteAddress': true,
  'googFingerprint': true,
};

var graphViews = {};

// Returns number parsed from |value|, or NaN if the stats name is black-listed.
function getNumberFromValue(name, value) {
  if (statsNameBlackList[name])
    return NaN;
  return parseFloat(value);
}

// Adds the stats report |report| to the timeline graph for the given
// |peerConnectionElement|.
function drawSingleReport(peerConnectionElement, report) {
  var reportType = report.type;
  var reportId = report.id;
  var stats = report.stats;
  if (!stats || !stats.values)
    return;

  for (var i = 0; i < stats.values.length - 1; i = i + 2) {
    var rawLabel = stats.values[i];
    // Propagation deltas are handled separately.
    if (rawLabel == RECEIVED_PROPAGATION_DELTA_LABEL) {
      drawReceivedPropagationDelta(
          peerConnectionElement, report, stats.values[i + 1]);
      continue;
    }
    var rawDataSeriesId = reportId + '-' + rawLabel;
    var rawValue = getNumberFromValue(rawLabel, stats.values[i + 1]);
    if (isNaN(rawValue)) {
      // We do not draw non-numerical values, but still want to record it in the
      // data series.
      addDataSeriesPoints(peerConnectionElement,
                          rawDataSeriesId,
                          rawLabel,
                          [stats.timestamp],
                          [stats.values[i + 1]]);
      continue;
    }

    var finalDataSeriesId = rawDataSeriesId;
    var finalLabel = rawLabel;
    var finalValue = rawValue;
    // We need to convert the value if dataConversionConfig[rawLabel] exists.
    if (dataConversionConfig[rawLabel]) {
      // Updates the original dataSeries before the conversion.
      addDataSeriesPoints(peerConnectionElement,
                          rawDataSeriesId,
                          rawLabel,
                          [stats.timestamp],
                          [rawValue]);

      // Convert to another value to draw on graph, using the original
      // dataSeries as input.
      finalValue = dataConversionConfig[rawLabel].convertFunction(
          peerConnectionDataStore[peerConnectionElement.id].getDataSeries(
              rawDataSeriesId));
      finalLabel = dataConversionConfig[rawLabel].convertedName;
      finalDataSeriesId = reportId + '-' + finalLabel;
    }

    // Updates the final dataSeries to draw.
    addDataSeriesPoints(peerConnectionElement,
                        finalDataSeriesId,
                        finalLabel,
                        [stats.timestamp],
                        [finalValue]);

    // Updates the graph.
    var graphType = bweCompoundGraphConfig[finalLabel] ?
                    'bweCompound' : finalLabel;
    var graphViewId =
        peerConnectionElement.id + '-' + reportId + '-' + graphType;

    if (!graphViews[graphViewId]) {
      graphViews[graphViewId] = createStatsGraphView(peerConnectionElement,
                                                     report,
                                                     graphType);
      var date = new Date(stats.timestamp);
      graphViews[graphViewId].setDateRange(date, date);
    }
    // Adds the new dataSeries to the graphView. We have to do it here to cover
    // both the simple and compound graph cases.
    var dataSeries =
        peerConnectionDataStore[peerConnectionElement.id].getDataSeries(
            finalDataSeriesId);
    if (!graphViews[graphViewId].hasDataSeries(dataSeries))
      graphViews[graphViewId].addDataSeries(dataSeries);
    graphViews[graphViewId].updateEndDate();
  }
}

// Makes sure the TimelineDataSeries with id |dataSeriesId| is created,
// and adds the new data points to it. |times| is the list of timestamps for
// each data point, and |values| is the list of the data point values.
function addDataSeriesPoints(
    peerConnectionElement, dataSeriesId, label, times, values) {
  var dataSeries =
    peerConnectionDataStore[peerConnectionElement.id].getDataSeries(
        dataSeriesId);
  if (!dataSeries) {
    dataSeries = new TimelineDataSeries();
    peerConnectionDataStore[peerConnectionElement.id].setDataSeries(
        dataSeriesId, dataSeries);
    if (bweCompoundGraphConfig[label]) {
      dataSeries.setColor(bweCompoundGraphConfig[label].color);
    }
  }
  for (var i = 0; i < times.length; ++i)
    dataSeries.addPoint(times[i], values[i]);
}

// Draws the received propagation deltas using the packet group arrival time as
// the x-axis. For example, |report.stats.values| should be like
// ['googReceivedPacketGroupArrivalTimeDebug', '[123456, 234455, 344566]',
//  'googReceivedPacketGroupPropagationDeltaDebug', '[23, 45, 56]', ...].
function drawReceivedPropagationDelta(peerConnectionElement, report, deltas) {
  var reportId = report.id;
  var stats = report.stats;
  var times = null;
  // Find the packet group arrival times.
  for (var i = 0; i < stats.values.length - 1; i = i + 2) {
    if (stats.values[i] == RECEIVED_PACKET_GROUP_ARRIVAL_TIME_LABEL) {
      times = stats.values[i + 1];
      break;
    }
  }
  // Unexpected.
  if (times == null)
    return;

  // Convert |deltas| and |times| from strings to arrays of numbers.
  try {
    deltas = JSON.parse(deltas);
    times = JSON.parse(times);
  } catch (e) {
    console.log(e);
    return;
  }

  // Update the data series.
  var dataSeriesId = reportId + '-' + RECEIVED_PROPAGATION_DELTA_LABEL;
  addDataSeriesPoints(
      peerConnectionElement,
      dataSeriesId,
      RECEIVED_PROPAGATION_DELTA_LABEL,
      times,
      deltas);
  // Update the graph.
  var graphViewId = peerConnectionElement.id + '-' + reportId + '-' +
      RECEIVED_PROPAGATION_DELTA_LABEL;
  var date = new Date(times[times.length - 1]);
  if (!graphViews[graphViewId]) {
    graphViews[graphViewId] = createStatsGraphView(
        peerConnectionElement,
        report,
        RECEIVED_PROPAGATION_DELTA_LABEL);
    graphViews[graphViewId].setScale(10);
    graphViews[graphViewId].setDateRange(date, date);
    var dataSeries = peerConnectionDataStore[peerConnectionElement.id]
        .getDataSeries(dataSeriesId);
    graphViews[graphViewId].addDataSeries(dataSeries);
  }
  graphViews[graphViewId].updateEndDate(date);
}

// Get report types for SSRC reports. Returns 'audio' or 'video' where this type
// can be deduced from existing stats labels. Otherwise empty string for
// non-SSRC reports or where type (audio/video) can't be deduced.
function getSsrcReportType(report) {
  if (report.type != 'ssrc')
    return '';
  if (report.stats && report.stats.values) {
    // Known stats keys for audio send/receive streams.
    if (report.stats.values.indexOf('audioOutputLevel') != -1 ||
        report.stats.values.indexOf('audioInputLevel') != -1) {
      return 'audio';
    }
    // Known stats keys for video send/receive streams.
    // TODO(pbos): Change to use some non-goog-prefixed stats when available for
    // video.
    if (report.stats.values.indexOf('googFrameRateReceived') != -1 ||
        report.stats.values.indexOf('googFrameRateSent') != -1) {
      return 'video';
    }
  }
  return '';
}

// Ensures a div container to hold all stats graphs for one track is created as
// a child of |peerConnectionElement|.
function ensureStatsGraphTopContainer(peerConnectionElement, report) {
  var containerId = peerConnectionElement.id + '-' +
      report.type + '-' + report.id + '-graph-container';
  var container = $(containerId);
  if (!container) {
    container = document.createElement('details');
    container.id = containerId;
    container.className = 'stats-graph-container';

    peerConnectionElement.appendChild(container);
    container.innerHTML ='<summary><span></span></summary>';
    container.firstChild.firstChild.className =
        STATS_GRAPH_CONTAINER_HEADING_CLASS;
    container.firstChild.firstChild.textContent =
        'Stats graphs for ' + report.id;
    var statsType = getSsrcReportType(report);
    if (statsType != '')
      container.firstChild.firstChild.textContent += ' (' + statsType + ')';

    if (report.type == 'ssrc') {
      var ssrcInfoElement = document.createElement('div');
      container.firstChild.appendChild(ssrcInfoElement);
      ssrcInfoManager.populateSsrcInfo(ssrcInfoElement,
                                       GetSsrcFromReport(report));
    }
  }
  return container;
}

// Creates the container elements holding a timeline graph
// and the TimelineGraphView object.
function createStatsGraphView(
    peerConnectionElement, report, statsName) {
  var topContainer = ensureStatsGraphTopContainer(peerConnectionElement,
                                                  report);

  var graphViewId =
      peerConnectionElement.id + '-' + report.id + '-' + statsName;
  var divId = graphViewId + '-div';
  var canvasId = graphViewId + '-canvas';
  var container = document.createElement("div");
  container.className = 'stats-graph-sub-container';

  topContainer.appendChild(container);
  container.innerHTML = '<div>' + statsName + '</div>' +
      '<div id=' + divId + '><canvas id=' + canvasId + '></canvas></div>';
  if (statsName == 'bweCompound') {
      container.insertBefore(
          createBweCompoundLegend(peerConnectionElement, report.id),
          $(divId));
  }
  return new TimelineGraphView(divId, canvasId);
}

// Creates the legend section for the bweCompound graph.
// Returns the legend element.
function createBweCompoundLegend(peerConnectionElement, reportId) {
  var legend = document.createElement('div');
  for (var prop in bweCompoundGraphConfig) {
    var div = document.createElement('div');
    legend.appendChild(div);
    div.innerHTML = '<input type=checkbox checked></input>' + prop;
    div.style.color = bweCompoundGraphConfig[prop].color;
    div.dataSeriesId = reportId + '-' + prop;
    div.graphViewId =
        peerConnectionElement.id + '-' + reportId + '-bweCompound';
    div.firstChild.addEventListener('click', function(event) {
        var target =
            peerConnectionDataStore[peerConnectionElement.id].getDataSeries(
                event.target.parentNode.dataSeriesId);
        target.show(event.target.checked);
        graphViews[event.target.parentNode.graphViewId].repaint();
    });
  }
  return legend;
}

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


/**
 * Maintains the stats table.
 * @param {SsrcInfoManager} ssrcInfoManager The source of the ssrc info.
 */
var StatsTable = (function(ssrcInfoManager) {
  'use strict';

  /**
   * @param {SsrcInfoManager} ssrcInfoManager The source of the ssrc info.
   * @constructor
   */
  function StatsTable(ssrcInfoManager) {
    /**
     * @type {SsrcInfoManager}
     * @private
     */
    this.ssrcInfoManager_ = ssrcInfoManager;
  }

  StatsTable.prototype = {
    /**
     * Adds |report| to the stats table of |peerConnectionElement|.
     *
     * @param {!Element} peerConnectionElement The root element.
     * @param {!Object} report The object containing stats, which is the object
     *     containing timestamp and values, which is an array of strings, whose
     *     even index entry is the name of the stat, and the odd index entry is
     *     the value.
     */
    addStatsReport: function(peerConnectionElement, report) {
      var statsTable = this.ensureStatsTable_(peerConnectionElement, report);

      if (report.stats) {
        this.addStatsToTable_(statsTable,
                              report.stats.timestamp, report.stats.values);
      }
    },

    /**
     * Ensure the DIV container for the stats tables is created as a child of
     * |peerConnectionElement|.
     *
     * @param {!Element} peerConnectionElement The root element.
     * @return {!Element} The stats table container.
     * @private
     */
    ensureStatsTableContainer_: function(peerConnectionElement) {
      var containerId = peerConnectionElement.id + '-table-container';
      var container = $(containerId);
      if (!container) {
        container = document.createElement('div');
        container.id = containerId;
        container.className = 'stats-table-container';
        var head = document.createElement('div');
        head.textContent = 'Stats Tables';
        container.appendChild(head);
        peerConnectionElement.appendChild(container);
      }
      return container;
    },

    /**
     * Ensure the stats table for track specified by |report| of PeerConnection
     * |peerConnectionElement| is created.
     *
     * @param {!Element} peerConnectionElement The root element.
     * @param {!Object} report The object containing stats, which is the object
     *     containing timestamp and values, which is an array of strings, whose
     *     even index entry is the name of the stat, and the odd index entry is
     *     the value.
     * @return {!Element} The stats table element.
     * @private
     */
     ensureStatsTable_: function(peerConnectionElement, report) {
      var tableId = peerConnectionElement.id + '-table-' + report.id;
      var table = $(tableId);
      if (!table) {
        var container = this.ensureStatsTableContainer_(peerConnectionElement);
        var details = document.createElement('details');
        container.appendChild(details);

        var summary = document.createElement('summary');
        summary.textContent = report.id;
        details.appendChild(summary);

        table = document.createElement('table');
        details.appendChild(table);
        table.id = tableId;
        table.border = 1;

        table.innerHTML = '<tr><th colspan=2></th></tr>';
        table.rows[0].cells[0].textContent = 'Statistics ' + report.id;
        if (report.type == 'ssrc') {
            table.insertRow(1);
            table.rows[1].innerHTML = '<td colspan=2></td>';
            this.ssrcInfoManager_.populateSsrcInfo(
                table.rows[1].cells[0], GetSsrcFromReport(report));
        }
      }
      return table;
    },

    /**
     * Update |statsTable| with |time| and |statsData|.
     *
     * @param {!Element} statsTable Which table to update.
     * @param {number} time The number of miliseconds since epoch.
     * @param {Array<string>} statsData An array of stats name and value pairs.
     * @private
     */
    addStatsToTable_: function(statsTable, time, statsData) {
      var date = new Date(time);
      this.updateStatsTableRow_(statsTable, 'timestamp', date.toLocaleString());
      for (var i = 0; i < statsData.length - 1; i = i + 2) {
        this.updateStatsTableRow_(statsTable, statsData[i], statsData[i + 1]);
      }
    },

    /**
     * Update the value column of the stats row of |rowName| to |value|.
     * A new row is created is this is the first report of this stats.
     *
     * @param {!Element} statsTable Which table to update.
     * @param {string} rowName The name of the row to update.
     * @param {string} value The new value to set.
     * @private
     */
    updateStatsTableRow_: function(statsTable, rowName, value) {
      var trId = statsTable.id + '-' + rowName;
      var trElement = $(trId);
      if (!trElement) {
        trElement = document.createElement('tr');
        trElement.id = trId;
        statsTable.firstChild.appendChild(trElement);
        trElement.innerHTML = '<td>' + rowName + '</td><td></td>';
      }
      trElement.cells[1].textContent = value;

      // Highlights the table for the active connection.
      if (rowName == 'googActiveConnection' && value == true)
        statsTable.parentElement.classList.add('stats-table-active-connection');
    }
  };

  return StatsTable;
})();

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


/**
 * The data of a peer connection update.
 * @param {number} pid The id of the renderer.
 * @param {number} lid The id of the peer conneciton inside a renderer.
 * @param {string} type The type of the update.
 * @param {string} value The details of the update.
 * @constructor
 */
var PeerConnectionUpdateEntry = function(pid, lid, type, value) {
  /**
   * @type {number}
   */
  this.pid = pid;

  /**
   * @type {number}
   */
  this.lid = lid;

  /**
   * @type {string}
   */
  this.type = type;

  /**
   * @type {string}
   */
  this.value = value;
};


/**
 * Maintains the peer connection update log table.
 */
var PeerConnectionUpdateTable = (function() {
  'use strict';

  /**
   * @constructor
   */
  function PeerConnectionUpdateTable() {
    /**
     * @type {string}
     * @const
     * @private
     */
    this.UPDATE_LOG_ID_SUFFIX_ = '-update-log';

    /**
     * @type {string}
     * @const
     * @private
     */
    this.UPDATE_LOG_CONTAINER_CLASS_ = 'update-log-container';

    /**
     * @type {string}
     * @const
     * @private
     */
    this.UPDATE_LOG_TABLE_CLASS = 'update-log-table';
  }

  PeerConnectionUpdateTable.prototype = {
    /**
     * Adds the update to the update table as a new row. The type of the update
     * is set to the summary of the cell; clicking the cell will reveal or hide
     * the details as the content of a TextArea element.
     *
     * @param {!Element} peerConnectionElement The root element.
     * @param {!PeerConnectionUpdateEntry} update The update to add.
     */
    addPeerConnectionUpdate: function(peerConnectionElement, update) {
      var tableElement = this.ensureUpdateContainer_(peerConnectionElement);

      var row = document.createElement('tr');
      tableElement.firstChild.appendChild(row);

      var time = new Date(parseFloat(update.time));
      row.innerHTML = '<td>' + time.toLocaleString() + '</td>';

      if (update.value.length == 0) {
        row.innerHTML += '<td>' + update.type + '</td>';
        return;
      }

      row.innerHTML += '<td><details><summary>' + update.type +
          '</summary></details></td>';

      var valueContainer = document.createElement('pre');
      var details = row.cells[1].childNodes[0];
      details.appendChild(valueContainer);
      valueContainer.textContent = update.value;
    },

    /**
     * Makes sure the update log table of the peer connection is created.
     *
     * @param {!Element} peerConnectionElement The root element.
     * @return {!Element} The log table element.
     * @private
     */
    ensureUpdateContainer_: function(peerConnectionElement) {
      var tableId = peerConnectionElement.id + this.UPDATE_LOG_ID_SUFFIX_;
      var tableElement = $(tableId);
      if (!tableElement) {
        var tableContainer = document.createElement('div');
        tableContainer.className = this.UPDATE_LOG_CONTAINER_CLASS_;
        peerConnectionElement.appendChild(tableContainer);

        tableElement = document.createElement('table');
        tableElement.className = this.UPDATE_LOG_TABLE_CLASS;
        tableElement.id = tableId;
        tableElement.border = 1;
        tableContainer.appendChild(tableElement);
        tableElement.innerHTML = '<tr><th>Time</th>' +
            '<th class="update-log-header-event">Event</th></tr>';
      }
      return tableElement;
    }
  };

  return PeerConnectionUpdateTable;
})();

// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.


/**
 * Provides the UI for dump creation.
 */
var DumpCreator = (function() {
  /**
   * @param {Element} containerElement The parent element of the dump creation
   *     UI.
   * @constructor
   */
  function DumpCreator(containerElement) {
    /**
     * The root element of the dump creation UI.
     * @type {Element}
     * @private
     */
    this.root_ = document.createElement('details');

    this.root_.className = 'peer-connection-dump-root';
    containerElement.appendChild(this.root_);
    var summary = document.createElement('summary');
    this.root_.appendChild(summary);
    summary.textContent = 'Create Dump';
    var content = document.createElement('div');
    this.root_.appendChild(content);

    content.innerHTML = '<div><a><button>' +
        'Download the PeerConnection updates and stats data' +
        '</button></a></div>' +
        '<p><label><input type=checkbox>' +
        'Enable diagnostic audio recordings</label></p>' +
        '<p class=audio-recordings-info>A diagnostic audio recording is used' +
        ' for analyzing audio problems. It consists of two files and contains' +
        ' the audio played out from the speaker and recorded from the' +
        ' microphone and is saved to the local disk. Checking this box will' +
        ' enable the recording for ongoing WebRTC calls and for future WebRTC' +
        ' calls. When the box is unchecked or this page is closed, all' +
        ' ongoing recordings will be stopped and this recording' +
        ' functionality will be disabled for future WebRTC calls. Recordings' +
        ' in multiple tabs are supported as well as multiple recordings in' +
        ' the same tab. When enabling, you select a base filename to which' +
        ' suffixes will be appended as</p>' +
        '<p><div>&lt;base filename&gt;.&lt;render process ID&gt;' +
        '.aec_dump.&lt;recording ID&gt;</div>' +
        '<div>&lt;base filename&gt;.&lt;render process ID&gt;' +
        '.source_input.&lt;stream ID&gt;.wav</div></p>' +
        '<p class=audio-recordings-info>If recordings are disabled and then' +
        ' enabled using the same base filename, the microphone recording file' +
        ' will be overwritten, and the AEC dump file will be appended to and' +
        ' may become invalid. It is recommended to choose a new base filename' +
        ' each time or move the produced files before enabling again.</p>' +
        '<p><label><input type=checkbox>' +
        'Enable diagnostic packet and event recording</label></p>' +
        '<p class=audio-recordings-info>A diagnostic packet and event' +
        ' recording can be used for analyzing various issues related to' +
        ' thread starvation, jitter buffers or bandwidth estimation. Two' +
        ' types of data are logged. First, incoming and outgoing RTP headers' +
        ' and RTCP packets are logged. These do not include any audio or' +
        ' video information, nor any other types of personally identifiable' +
        ' information (so no IP addresses or URLs). Checking this box will' +
        ' enable the recording for currently ongoing WebRTC calls. When' +
        ' the box is unchecked or this page is closed, all active recordings' +
        ' will be stopped. Recording in multiple tabs or multiple recordings' +
        ' in the same tab is currently not supported. When enabling, a' +
        ' filename for the recording can be selected. If an existing file is' +
        ' selected, it will be overwritten. </p>';
    content.getElementsByTagName('a')[0].addEventListener(
        'click', this.onDownloadData_.bind(this));
    content.getElementsByTagName('input')[0].addEventListener(
        'click', this.onAudioDebugRecordingsChanged_.bind(this));
    content.getElementsByTagName('input')[1].addEventListener(
        'click', this.onEventLogRecordingsChanged_.bind(this));
  }

  DumpCreator.prototype = {
    // Mark the diagnostic audio recording checkbox checked.
    enableAudioDebugRecordings: function() {
      this.root_.getElementsByTagName('input')[0].checked = true;
    },

    // Mark the diagnostic audio recording checkbox unchecked.
    disableAudioDebugRecordings: function() {
      this.root_.getElementsByTagName('input')[0].checked = false;
    },

    // Mark the event log recording checkbox checked.
    enableEventLogRecordings: function() {
      this.root_.getElementsByTagName('input')[1].checked = true;
    },

    // Mark the event log recording checkbox unchecked.
    disableEventLogRecordings: function() {
      this.root_.getElementsByTagName('input')[1].checked = false;
    },

    /**
     * Downloads the PeerConnection updates and stats data as a file.
     *
     * @private
     */
    onDownloadData_: function() {
      var dump_object =
      {
        'getUserMedia': userMediaRequests,
        'PeerConnections': peerConnectionDataStore,
      };
      var textBlob = new Blob([JSON.stringify(dump_object, null, ' ')],
                              {type: 'octet/stream'});
      var URL = window.URL.createObjectURL(textBlob);

      var anchor = this.root_.getElementsByTagName('a')[0];
      anchor.href = URL;
      anchor.download = 'webrtc_internals_dump.txt';
      // The default action of the anchor will download the URL.
    },

    /**
     * Handles the event of toggling the audio debug recordings state.
     *
     * @private
     */
    onAudioDebugRecordingsChanged_: function() {
      var enabled = this.root_.getElementsByTagName('input')[0].checked;
      if (enabled) {
        chrome.send('enableAudioDebugRecordings');
      } else {
        chrome.send('disableAudioDebugRecordings');
      }
    },

    /**
     * Handles the event of toggling the event log recordings state.
     *
     * @private
     */
    onEventLogRecordingsChanged_: function() {
      var enabled = this.root_.getElementsByTagName('input')[1].checked;
      if (enabled) {
        chrome.send('enableEventLogRecordings');
      } else {
        chrome.send('disableEventLogRecordings');
      }
    },
  };
  return DumpCreator;
})();



function initialize() {
  dumpCreator = new DumpCreator($('content-root'));
  tabView = new TabView($('content-root'));
  ssrcInfoManager = new SsrcInfoManager();
  peerConnectionUpdateTable = new PeerConnectionUpdateTable();
  statsTable = new StatsTable(ssrcInfoManager);

  chrome.send('finishedDOMLoad');

  // Requests stats from all peer connections every second.
  window.setInterval(requestStats, 1000);
}
document.addEventListener('DOMContentLoaded', initialize);


/** Sends a request to the browser to get peer connection statistics. */
function requestStats() {
  if (Object.keys(peerConnectionDataStore).length > 0)
    chrome.send('getAllStats');
}


/**
 * A helper function for getting a peer connection element id.
 *
 * @param {!Object<number>} data The object containing the pid and lid of the
 *     peer connection.
 * @return {string} The peer connection element id.
 */
function getPeerConnectionId(data) {
  return data.pid + '-' + data.lid;
}


/**
 * Extracts ssrc info from a setLocal/setRemoteDescription update.
 *
 * @param {!PeerConnectionUpdateEntry} data The peer connection update data.
 */
function extractSsrcInfo(data) {
  if (data.type == 'setLocalDescription' ||
      data.type == 'setRemoteDescription') {
    ssrcInfoManager.addSsrcStreamInfo(data.value);
  }
}


/**
 * A helper function for appending a child element to |parent|.
 *
 * @param {!Element} parent The parent element.
 * @param {string} tag The child element tag.
 * @param {string} text The textContent of the new DIV.
 * @return {!Element} the new DIV element.
 */
function appendChildWithText(parent, tag, text) {
  var child = document.createElement(tag);
  child.textContent = text;
  parent.appendChild(child);
  return child;
}

/**
 * Helper for adding a peer connection update.
 *
 * @param {Element} peerConnectionElement
 * @param {!PeerConnectionUpdateEntry} update The peer connection update data.
 */
function addPeerConnectionUpdate(peerConnectionElement, update) {
  peerConnectionUpdateTable.addPeerConnectionUpdate(peerConnectionElement,
                                                    update);
  extractSsrcInfo(update);
  peerConnectionDataStore[peerConnectionElement.id].addUpdate(update);
}


/** Browser message handlers. */


/**
 * Removes all information about a peer connection.
 *
 * @param {!Object<number>} data The object containing the pid and lid of a peer
 *     connection.
 */
function removePeerConnection(data) {
  var element = $(getPeerConnectionId(data));
  if (element) {
    delete peerConnectionDataStore[element.id];
    tabView.removeTab(element.id);
  }
}


/**
 * Adds a peer connection.
 *
 * @param {!Object} data The object containing the pid, lid, url,
 *     rtcConfiguration, and constraints of a peer connection.
 */
function addPeerConnection(data) {
  var id = getPeerConnectionId(data);

  if (!peerConnectionDataStore[id]) {
    peerConnectionDataStore[id] = new PeerConnectionRecord();
  }
  peerConnectionDataStore[id].initialize(
      data.url, data.rtcConfiguration, data.constraints);

  var peerConnectionElement = $(id);
  if (!peerConnectionElement) {
    peerConnectionElement = tabView.addTab(id, data.url + ' [' + id + ']');
  }

  var p = document.createElement('p');
  p.textContent = data.url + ', ' + data.rtcConfiguration + ', ' +
      data.constraints;
  peerConnectionElement.appendChild(p);

  return peerConnectionElement;
}


/**
 * Adds a peer connection update.
 *
 * @param {!PeerConnectionUpdateEntry} data The peer connection update data.
 */
function updatePeerConnection(data) {
  var peerConnectionElement = $(getPeerConnectionId(data));
  addPeerConnectionUpdate(peerConnectionElement, data);
}


/**
 * Adds the information of all peer connections created so far.
 *
 * @param {Array<!Object>} data An array of the information of all peer
 *     connections. Each array item contains pid, lid, url, rtcConfiguration,
 *     constraints, and an array of updates as the log.
 */
function updateAllPeerConnections(data) {
  for (var i = 0; i < data.length; ++i) {
    var peerConnection = addPeerConnection(data[i]);

    var log = data[i].log;
    if (!log)
      continue;
    for (var j = 0; j < log.length; ++j) {
      addPeerConnectionUpdate(peerConnection, log[j]);
    }
  }
  requestStats();
}


/**
 * Handles the report of stats.
 *
 * @param {!Object} data The object containing pid, lid, and reports, where
 *     reports is an array of stats reports. Each report contains id, type,
 *     and stats, where stats is the object containing timestamp and values,
 *     which is an array of strings, whose even index entry is the name of the
 *     stat, and the odd index entry is the value.
 */
function addStats(data) {
  var peerConnectionElement = $(getPeerConnectionId(data));
  if (!peerConnectionElement)
    return;

  for (var i = 0; i < data.reports.length; ++i) {
    var report = data.reports[i];
    statsTable.addStatsReport(peerConnectionElement, report);
    drawSingleReport(peerConnectionElement, report);
  }
}


/**
 * Adds a getUserMedia request.
 *
 * @param {!Object} data The object containing rid {number}, pid {number},
 *     origin {string}, audio {string}, video {string}.
 */
function addGetUserMedia(data) {
  userMediaRequests.push(data);

  if (!$(USER_MEDIA_TAB_ID)) {
    tabView.addTab(USER_MEDIA_TAB_ID, 'GetUserMedia Requests');
  }

  var requestDiv = document.createElement('div');
  requestDiv.className = 'user-media-request-div-class';
  requestDiv.rid = data.rid;
  $(USER_MEDIA_TAB_ID).appendChild(requestDiv);

  appendChildWithText(requestDiv, 'div', 'Caller origin: ' + data.origin);
  appendChildWithText(requestDiv, 'div', 'Caller process id: ' + data.pid);
  appendChildWithText(requestDiv, 'span', 'Audio Constraints').style.fontWeight
      = 'bold';
  appendChildWithText(requestDiv, 'div', data.audio);

  appendChildWithText(requestDiv, 'span', 'Video Constraints').style.fontWeight
      = 'bold';
  appendChildWithText(requestDiv, 'div', data.video);
}


/**
 * Removes the getUserMedia requests from the specified |rid|.
 *
 * @param {!Object} data The object containing rid {number}, the render id.
 */
function removeGetUserMediaForRenderer(data) {
  for (var i = userMediaRequests.length - 1; i >= 0; --i) {
    if (userMediaRequests[i].rid == data.rid)
      userMediaRequests.splice(i, 1);
  }

  var requests = $(USER_MEDIA_TAB_ID).childNodes;
  for (var i = 0; i < requests.length; ++i) {
    if (requests[i].rid == data.rid)
      $(USER_MEDIA_TAB_ID).removeChild(requests[i]);

  }
  if ($(USER_MEDIA_TAB_ID).childNodes.length == 0)
    tabView.removeTab(USER_MEDIA_TAB_ID);
}


/**
 * Notification that the audio debug recordings file selection dialog was
 * cancelled, i.e. recordings have not been enabled.
 */
function audioDebugRecordingsFileSelectionCancelled() {
  dumpCreator.disableAudioDebugRecordings();
}


/**
 * Notification that the event log recordings file selection dialog was
 * cancelled, i.e. recordings have not been enabled.
 */
function eventLogRecordingsFileSelectionCancelled() {
  dumpCreator.disableEventLogRecordings();
}

/**
 * Set
 */
function enableAudioDebugRecordings() {
  dumpCreator.enableAudioDebugRecordings();
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/bindings", [
  "mojo/public/js/router",
  "mojo/public/js/core",
], function(router, core) {

  var Router = router.Router;

  var kProxyProperties = Symbol("proxyProperties");
  var kStubProperties = Symbol("stubProperties");

  // Public proxy class properties that are managed at runtime by the JS
  // bindings. See ProxyBindings below.
  function ProxyProperties(receiver) {
    this.receiver = receiver;
  }

  // TODO(hansmuller): remove then after 'Client=' has been removed from Mojom.
  ProxyProperties.prototype.getLocalDelegate = function() {
    return this.local && StubBindings(this.local).delegate;
  }

  // TODO(hansmuller): remove then after 'Client=' has been removed from Mojom.
  ProxyProperties.prototype.setLocalDelegate = function(impl) {
    if (this.local)
      StubBindings(this.local).delegate = impl;
    else
      throw new Error("no stub object");
  }

  function connectionHandle(connection) {
    return connection &&
        connection.router &&
        connection.router.connector_ &&
        connection.router.connector_.handle_;
  }

  ProxyProperties.prototype.close = function() {
    var handle = connectionHandle(this.connection);
    if (handle)
      core.close(handle);
  }

  // Public stub class properties that are managed at runtime by the JS
  // bindings. See StubBindings below.
  function StubProperties(delegate) {
    this.delegate = delegate;
  }

  StubProperties.prototype.close = function() {
    var handle = connectionHandle(this.connection);
    if (handle)
      core.close(handle);
  }

  // The base class for generated proxy classes.
  function ProxyBase(receiver) {
    this[kProxyProperties] = new ProxyProperties(receiver);

    // TODO(hansmuller): Temporary, for Chrome backwards compatibility.
    if (receiver instanceof Router)
      this.receiver_ = receiver;
  }

  // The base class for generated stub classes.
  function StubBase(delegate) {
    this[kStubProperties] = new StubProperties(delegate);
  }

  // TODO(hansmuller): remove everything except the connection property doc
  // after 'Client=' has been removed from Mojom.

  // Provides access to properties added to a proxy object without risking
  // Mojo interface name collisions. Unless otherwise specified, the initial
  // value of all properties is undefined.
  //
  // ProxyBindings(proxy).connection - The Connection object that links the
  //   proxy for a remote Mojo service to an optional local stub for a local
  //   service. The value of ProxyBindings(proxy).connection.remote == proxy.
  //
  // ProxyBindings(proxy).local  - The "local" stub object whose delegate
  //   implements the proxy's Mojo client interface.
  //
  // ProxyBindings(proxy).setLocalDelegate(impl) - Sets the implementation
  //   delegate of the proxy's client stub object. This is just shorthand
  //   for |StubBindings(ProxyBindings(proxy).local).delegate = impl|.
  //
  // ProxyBindings(proxy).getLocalDelegate() - Returns the implementation
  //   delegate of the proxy's client stub object. This is just shorthand
  //   for |StubBindings(ProxyBindings(proxy).local).delegate|.

  function ProxyBindings(proxy) {
    return (proxy instanceof ProxyBase) ? proxy[kProxyProperties] : proxy;
  }

  // TODO(hansmuller): remove the remote doc after 'Client=' has been
  // removed from Mojom.

  // Provides access to properties added to a stub object without risking
  // Mojo interface name collisions. Unless otherwise specified, the initial
  // value of all properties is undefined.
  //
  // StubBindings(stub).delegate - The optional implementation delegate for
  //  the Mojo interface stub.
  //
  // StubBindings(stub).connection - The Connection object that links an
  //   optional proxy for a remote service to this stub. The value of
  //   StubBindings(stub).connection.local == stub.
  //
  // StubBindings(stub).remote - A proxy for the the stub's Mojo client
  //   service.

  function StubBindings(stub) {
    return stub instanceof StubBase ?  stub[kStubProperties] : stub;
  }

  var exports = {};
  exports.EmptyProxy = ProxyBase;
  exports.EmptyStub = StubBase;
  exports.ProxyBase = ProxyBase;
  exports.ProxyBindings = ProxyBindings;
  exports.StubBase = StubBase;
  exports.StubBindings = StubBindings;
  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/buffer", function() {

  var kHostIsLittleEndian = (function () {
    var endianArrayBuffer = new ArrayBuffer(2);
    var endianUint8Array = new Uint8Array(endianArrayBuffer);
    var endianUint16Array = new Uint16Array(endianArrayBuffer);
    endianUint16Array[0] = 1;
    return endianUint8Array[0] == 1;
  })();

  var kHighWordMultiplier = 0x100000000;

  function Buffer(sizeOrArrayBuffer) {
    if (sizeOrArrayBuffer instanceof ArrayBuffer)
      this.arrayBuffer = sizeOrArrayBuffer;
    else
      this.arrayBuffer = new ArrayBuffer(sizeOrArrayBuffer);

    this.dataView = new DataView(this.arrayBuffer);
    this.next = 0;
  }

  Object.defineProperty(Buffer.prototype, "byteLength", {
    get: function() { return this.arrayBuffer.byteLength; }
  });

  Buffer.prototype.alloc = function(size) {
    var pointer = this.next;
    this.next += size;
    if (this.next > this.byteLength) {
      var newSize = (1.5 * (this.byteLength + size)) | 0;
      this.grow(newSize);
    }
    return pointer;
  };

  function copyArrayBuffer(dstArrayBuffer, srcArrayBuffer) {
    (new Uint8Array(dstArrayBuffer)).set(new Uint8Array(srcArrayBuffer));
  }

  Buffer.prototype.grow = function(size) {
    var newArrayBuffer = new ArrayBuffer(size);
    copyArrayBuffer(newArrayBuffer, this.arrayBuffer);
    this.arrayBuffer = newArrayBuffer;
    this.dataView = new DataView(this.arrayBuffer);
  };

  Buffer.prototype.trim = function() {
    this.arrayBuffer = this.arrayBuffer.slice(0, this.next);
    this.dataView = new DataView(this.arrayBuffer);
  };

  Buffer.prototype.getUint8 = function(offset) {
    return this.dataView.getUint8(offset);
  }
  Buffer.prototype.getUint16 = function(offset) {
    return this.dataView.getUint16(offset, kHostIsLittleEndian);
  }
  Buffer.prototype.getUint32 = function(offset) {
    return this.dataView.getUint32(offset, kHostIsLittleEndian);
  }
  Buffer.prototype.getUint64 = function(offset) {
    var lo, hi;
    if (kHostIsLittleEndian) {
      lo = this.dataView.getUint32(offset, kHostIsLittleEndian);
      hi = this.dataView.getUint32(offset + 4, kHostIsLittleEndian);
    } else {
      hi = this.dataView.getUint32(offset, kHostIsLittleEndian);
      lo = this.dataView.getUint32(offset + 4, kHostIsLittleEndian);
    }
    return lo + hi * kHighWordMultiplier;
  }

  Buffer.prototype.getInt8 = function(offset) {
    return this.dataView.getInt8(offset);
  }
  Buffer.prototype.getInt16 = function(offset) {
    return this.dataView.getInt16(offset, kHostIsLittleEndian);
  }
  Buffer.prototype.getInt32 = function(offset) {
    return this.dataView.getInt32(offset, kHostIsLittleEndian);
  }
  Buffer.prototype.getInt64 = function(offset) {
    var lo, hi;
    if (kHostIsLittleEndian) {
      lo = this.dataView.getUint32(offset, kHostIsLittleEndian);
      hi = this.dataView.getInt32(offset + 4, kHostIsLittleEndian);
    } else {
      hi = this.dataView.getInt32(offset, kHostIsLittleEndian);
      lo = this.dataView.getUint32(offset + 4, kHostIsLittleEndian);
    }
    return lo + hi * kHighWordMultiplier;
  }

  Buffer.prototype.getFloat32 = function(offset) {
    return this.dataView.getFloat32(offset, kHostIsLittleEndian);
  }
  Buffer.prototype.getFloat64 = function(offset) {
    return this.dataView.getFloat64(offset, kHostIsLittleEndian);
  }

  Buffer.prototype.setUint8 = function(offset, value) {
    this.dataView.setUint8(offset, value);
  }
  Buffer.prototype.setUint16 = function(offset, value) {
    this.dataView.setUint16(offset, value, kHostIsLittleEndian);
  }
  Buffer.prototype.setUint32 = function(offset, value) {
    this.dataView.setUint32(offset, value, kHostIsLittleEndian);
  }
  Buffer.prototype.setUint64 = function(offset, value) {
    var hi = (value / kHighWordMultiplier) | 0;
    if (kHostIsLittleEndian) {
      this.dataView.setInt32(offset, value, kHostIsLittleEndian);
      this.dataView.setInt32(offset + 4, hi, kHostIsLittleEndian);
    } else {
      this.dataView.setInt32(offset, hi, kHostIsLittleEndian);
      this.dataView.setInt32(offset + 4, value, kHostIsLittleEndian);
    }
  }

  Buffer.prototype.setInt8 = function(offset, value) {
    this.dataView.setInt8(offset, value);
  }
  Buffer.prototype.setInt16 = function(offset, value) {
    this.dataView.setInt16(offset, value, kHostIsLittleEndian);
  }
  Buffer.prototype.setInt32 = function(offset, value) {
    this.dataView.setInt32(offset, value, kHostIsLittleEndian);
  }
  Buffer.prototype.setInt64 = function(offset, value) {
    var hi = Math.floor(value / kHighWordMultiplier);
    if (kHostIsLittleEndian) {
      this.dataView.setInt32(offset, value, kHostIsLittleEndian);
      this.dataView.setInt32(offset + 4, hi, kHostIsLittleEndian);
    } else {
      this.dataView.setInt32(offset, hi, kHostIsLittleEndian);
      this.dataView.setInt32(offset + 4, value, kHostIsLittleEndian);
    }
  }

  Buffer.prototype.setFloat32 = function(offset, value) {
    this.dataView.setFloat32(offset, value, kHostIsLittleEndian);
  }
  Buffer.prototype.setFloat64 = function(offset, value) {
    this.dataView.setFloat64(offset, value, kHostIsLittleEndian);
  }

  var exports = {};
  exports.Buffer = Buffer;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/codec", [
  "mojo/public/js/unicode",
  "mojo/public/js/buffer",
], function(unicode, buffer) {

  var kErrorUnsigned = "Passing negative value to unsigned";
  var kErrorArray = "Passing non Array for array type";
  var kErrorString = "Passing non String for string type";
  var kErrorMap = "Passing non Map for map type";

  // Memory -------------------------------------------------------------------

  var kAlignment = 8;

  function align(size) {
    return size + (kAlignment - (size % kAlignment)) % kAlignment;
  }

  function isAligned(offset) {
    return offset >= 0 && (offset % kAlignment) === 0;
  }

  // Constants ----------------------------------------------------------------

  var kArrayHeaderSize = 8;
  var kStructHeaderSize = 8;
  var kMessageHeaderSize = 24;
  var kMessageWithRequestIDHeaderSize = 32;
  var kMapStructPayloadSize = 16;

  var kStructHeaderNumBytesOffset = 0;
  var kStructHeaderVersionOffset = 4;

  var kEncodedInvalidHandleValue = 0xFFFFFFFF;

  // Decoder ------------------------------------------------------------------

  function Decoder(buffer, handles, base) {
    this.buffer = buffer;
    this.handles = handles;
    this.base = base;
    this.next = base;
  }

  Decoder.prototype.align = function() {
    this.next = align(this.next);
  };

  Decoder.prototype.skip = function(offset) {
    this.next += offset;
  };

  Decoder.prototype.readInt8 = function() {
    var result = this.buffer.getInt8(this.next);
    this.next += 1;
    return result;
  };

  Decoder.prototype.readUint8 = function() {
    var result = this.buffer.getUint8(this.next);
    this.next += 1;
    return result;
  };

  Decoder.prototype.readInt16 = function() {
    var result = this.buffer.getInt16(this.next);
    this.next += 2;
    return result;
  };

  Decoder.prototype.readUint16 = function() {
    var result = this.buffer.getUint16(this.next);
    this.next += 2;
    return result;
  };

  Decoder.prototype.readInt32 = function() {
    var result = this.buffer.getInt32(this.next);
    this.next += 4;
    return result;
  };

  Decoder.prototype.readUint32 = function() {
    var result = this.buffer.getUint32(this.next);
    this.next += 4;
    return result;
  };

  Decoder.prototype.readInt64 = function() {
    var result = this.buffer.getInt64(this.next);
    this.next += 8;
    return result;
  };

  Decoder.prototype.readUint64 = function() {
    var result = this.buffer.getUint64(this.next);
    this.next += 8;
    return result;
  };

  Decoder.prototype.readFloat = function() {
    var result = this.buffer.getFloat32(this.next);
    this.next += 4;
    return result;
  };

  Decoder.prototype.readDouble = function() {
    var result = this.buffer.getFloat64(this.next);
    this.next += 8;
    return result;
  };

  Decoder.prototype.decodePointer = function() {
    // TODO(abarth): To correctly decode a pointer, we need to know the real
    // base address of the array buffer.
    var offsetPointer = this.next;
    var offset = this.readUint64();
    if (!offset)
      return 0;
    return offsetPointer + offset;
  };

  Decoder.prototype.decodeAndCreateDecoder = function(pointer) {
    return new Decoder(this.buffer, this.handles, pointer);
  };

  Decoder.prototype.decodeHandle = function() {
    return this.handles[this.readUint32()] || null;
  };

  Decoder.prototype.decodeString = function() {
    var numberOfBytes = this.readUint32();
    var numberOfElements = this.readUint32();
    var base = this.next;
    this.next += numberOfElements;
    return unicode.decodeUtf8String(
        new Uint8Array(this.buffer.arrayBuffer, base, numberOfElements));
  };

  Decoder.prototype.decodeArray = function(cls) {
    var numberOfBytes = this.readUint32();
    var numberOfElements = this.readUint32();
    var val = new Array(numberOfElements);
    if (cls === PackedBool) {
      var byte;
      for (var i = 0; i < numberOfElements; ++i) {
        if (i % 8 === 0)
          byte = this.readUint8();
        val[i] = (byte & (1 << i % 8)) ? true : false;
      }
    } else {
      for (var i = 0; i < numberOfElements; ++i) {
        val[i] = cls.decode(this);
      }
    }
    return val;
  };

  Decoder.prototype.decodeStruct = function(cls) {
    return cls.decode(this);
  };

  Decoder.prototype.decodeStructPointer = function(cls) {
    var pointer = this.decodePointer();
    if (!pointer) {
      return null;
    }
    return cls.decode(this.decodeAndCreateDecoder(pointer));
  };

  Decoder.prototype.decodeArrayPointer = function(cls) {
    var pointer = this.decodePointer();
    if (!pointer) {
      return null;
    }
    return this.decodeAndCreateDecoder(pointer).decodeArray(cls);
  };

  Decoder.prototype.decodeStringPointer = function() {
    var pointer = this.decodePointer();
    if (!pointer) {
      return null;
    }
    return this.decodeAndCreateDecoder(pointer).decodeString();
  };

  Decoder.prototype.decodeMap = function(keyClass, valueClass) {
    this.skip(4); // numberOfBytes
    this.skip(4); // version
    var keys = this.decodeArrayPointer(keyClass);
    var values = this.decodeArrayPointer(valueClass);
    var val = new Map();
    for (var i = 0; i < keys.length; i++)
      val.set(keys[i], values[i]);
    return val;
  };

  Decoder.prototype.decodeMapPointer = function(keyClass, valueClass) {
    var pointer = this.decodePointer();
    if (!pointer) {
      return null;
    }
    var decoder = this.decodeAndCreateDecoder(pointer);
    return decoder.decodeMap(keyClass, valueClass);
  };

  // Encoder ------------------------------------------------------------------

  function Encoder(buffer, handles, base) {
    this.buffer = buffer;
    this.handles = handles;
    this.base = base;
    this.next = base;
  }

  Encoder.prototype.align = function() {
    this.next = align(this.next);
  };

  Encoder.prototype.skip = function(offset) {
    this.next += offset;
  };

  Encoder.prototype.writeInt8 = function(val) {
    this.buffer.setInt8(this.next, val);
    this.next += 1;
  };

  Encoder.prototype.writeUint8 = function(val) {
    if (val < 0) {
      throw new Error(kErrorUnsigned);
    }
    this.buffer.setUint8(this.next, val);
    this.next += 1;
  };

  Encoder.prototype.writeInt16 = function(val) {
    this.buffer.setInt16(this.next, val);
    this.next += 2;
  };

  Encoder.prototype.writeUint16 = function(val) {
    if (val < 0) {
      throw new Error(kErrorUnsigned);
    }
    this.buffer.setUint16(this.next, val);
    this.next += 2;
  };

  Encoder.prototype.writeInt32 = function(val) {
    this.buffer.setInt32(this.next, val);
    this.next += 4;
  };

  Encoder.prototype.writeUint32 = function(val) {
    if (val < 0) {
      throw new Error(kErrorUnsigned);
    }
    this.buffer.setUint32(this.next, val);
    this.next += 4;
  };

  Encoder.prototype.writeInt64 = function(val) {
    this.buffer.setInt64(this.next, val);
    this.next += 8;
  };

  Encoder.prototype.writeUint64 = function(val) {
    if (val < 0) {
      throw new Error(kErrorUnsigned);
    }
    this.buffer.setUint64(this.next, val);
    this.next += 8;
  };

  Encoder.prototype.writeFloat = function(val) {
    this.buffer.setFloat32(this.next, val);
    this.next += 4;
  };

  Encoder.prototype.writeDouble = function(val) {
    this.buffer.setFloat64(this.next, val);
    this.next += 8;
  };

  Encoder.prototype.encodePointer = function(pointer) {
    if (!pointer)
      return this.writeUint64(0);
    // TODO(abarth): To correctly encode a pointer, we need to know the real
    // base address of the array buffer.
    var offset = pointer - this.next;
    this.writeUint64(offset);
  };

  Encoder.prototype.createAndEncodeEncoder = function(size) {
    var pointer = this.buffer.alloc(align(size));
    this.encodePointer(pointer);
    return new Encoder(this.buffer, this.handles, pointer);
  };

  Encoder.prototype.encodeHandle = function(handle) {
    this.handles.push(handle);
    this.writeUint32(this.handles.length - 1);
  };

  Encoder.prototype.encodeString = function(val) {
    var base = this.next + kArrayHeaderSize;
    var numberOfElements = unicode.encodeUtf8String(
        val, new Uint8Array(this.buffer.arrayBuffer, base));
    var numberOfBytes = kArrayHeaderSize + numberOfElements;
    this.writeUint32(numberOfBytes);
    this.writeUint32(numberOfElements);
    this.next += numberOfElements;
  };

  Encoder.prototype.encodeArray =
      function(cls, val, numberOfElements, encodedSize) {
    if (numberOfElements === undefined)
      numberOfElements = val.length;
    if (encodedSize === undefined)
      encodedSize = kArrayHeaderSize + cls.encodedSize * numberOfElements;

    this.writeUint32(encodedSize);
    this.writeUint32(numberOfElements);

    if (cls === PackedBool) {
      var byte = 0;
      for (i = 0; i < numberOfElements; ++i) {
        if (val[i])
          byte |= (1 << i % 8);
        if (i % 8 === 7 || i == numberOfElements - 1) {
          Uint8.encode(this, byte);
          byte = 0;
        }
      }
    } else {
      for (var i = 0; i < numberOfElements; ++i)
        cls.encode(this, val[i]);
    }
  };

  Encoder.prototype.encodeStruct = function(cls, val) {
    return cls.encode(this, val);
  };

  Encoder.prototype.encodeStructPointer = function(cls, val) {
    if (val == null) {
      // Also handles undefined, since undefined == null.
      this.encodePointer(val);
      return;
    }
    var encoder = this.createAndEncodeEncoder(cls.encodedSize);
    cls.encode(encoder, val);
  };

  Encoder.prototype.encodeArrayPointer = function(cls, val) {
    if (val == null) {
      // Also handles undefined, since undefined == null.
      this.encodePointer(val);
      return;
    }

    var numberOfElements = val.length;
    if (!Number.isSafeInteger(numberOfElements) || numberOfElements < 0)
      throw new Error(kErrorArray);

    var encodedSize = kArrayHeaderSize + ((cls === PackedBool) ?
        Math.ceil(numberOfElements / 8) : cls.encodedSize * numberOfElements);
    var encoder = this.createAndEncodeEncoder(encodedSize);
    encoder.encodeArray(cls, val, numberOfElements, encodedSize);
  };

  Encoder.prototype.encodeStringPointer = function(val) {
    if (val == null) {
      // Also handles undefined, since undefined == null.
      this.encodePointer(val);
      return;
    }
    // Only accepts string primivites, not String Objects like new String("foo")
    if (typeof(val) !== "string") {
      throw new Error(kErrorString);
    }
    var encodedSize = kArrayHeaderSize + unicode.utf8Length(val);
    var encoder = this.createAndEncodeEncoder(encodedSize);
    encoder.encodeString(val);
  };

  Encoder.prototype.encodeMap = function(keyClass, valueClass, val) {
    var keys = new Array(val.size);
    var values = new Array(val.size);
    var i = 0;
    val.forEach(function(value, key) {
      values[i] = value;
      keys[i++] = key;
    });
    this.writeUint32(kStructHeaderSize + kMapStructPayloadSize);
    this.writeUint32(0);  // version
    this.encodeArrayPointer(keyClass, keys);
    this.encodeArrayPointer(valueClass, values);
  }

  Encoder.prototype.encodeMapPointer = function(keyClass, valueClass, val) {
    if (val == null) {
      // Also handles undefined, since undefined == null.
      this.encodePointer(val);
      return;
    }
    if (!(val instanceof Map)) {
      throw new Error(kErrorMap);
    }
    var encodedSize = kStructHeaderSize + kMapStructPayloadSize;
    var encoder = this.createAndEncodeEncoder(encodedSize);
    encoder.encodeMap(keyClass, valueClass, val);
  };

  // Message ------------------------------------------------------------------

  var kMessageInterfaceIdOffset = kStructHeaderSize;
  var kMessageNameOffset = kMessageInterfaceIdOffset + 4;
  var kMessageFlagsOffset = kMessageNameOffset + 4;
  var kMessageRequestIDOffset = kMessageFlagsOffset + 8;

  var kMessageExpectsResponse = 1 << 0;
  var kMessageIsResponse      = 1 << 1;

  function Message(buffer, handles) {
    this.buffer = buffer;
    this.handles = handles;
  }

  Message.prototype.getHeaderNumBytes = function() {
    return this.buffer.getUint32(kStructHeaderNumBytesOffset);
  };

  Message.prototype.getHeaderVersion = function() {
    return this.buffer.getUint32(kStructHeaderVersionOffset);
  };

  Message.prototype.getName = function() {
    return this.buffer.getUint32(kMessageNameOffset);
  };

  Message.prototype.getFlags = function() {
    return this.buffer.getUint32(kMessageFlagsOffset);
  };

  Message.prototype.isResponse = function() {
    return (this.getFlags() & kMessageIsResponse) != 0;
  };

  Message.prototype.expectsResponse = function() {
    return (this.getFlags() & kMessageExpectsResponse) != 0;
  };

  Message.prototype.setRequestID = function(requestID) {
    // TODO(darin): Verify that space was reserved for this field!
    this.buffer.setUint64(kMessageRequestIDOffset, requestID);
  };


  // MessageBuilder -----------------------------------------------------------

  function MessageBuilder(messageName, payloadSize) {
    // Currently, we don't compute the payload size correctly ahead of time.
    // Instead, we resize the buffer at the end.
    var numberOfBytes = kMessageHeaderSize + payloadSize;
    this.buffer = new buffer.Buffer(numberOfBytes);
    this.handles = [];
    var encoder = this.createEncoder(kMessageHeaderSize);
    encoder.writeUint32(kMessageHeaderSize);
    encoder.writeUint32(0);  // version.
    encoder.writeUint32(0);  // interface ID.
    encoder.writeUint32(messageName);
    encoder.writeUint32(0);  // flags.
    encoder.writeUint32(0);  // padding.
  }

  MessageBuilder.prototype.createEncoder = function(size) {
    var pointer = this.buffer.alloc(size);
    return new Encoder(this.buffer, this.handles, pointer);
  };

  MessageBuilder.prototype.encodeStruct = function(cls, val) {
    cls.encode(this.createEncoder(cls.encodedSize), val);
  };

  MessageBuilder.prototype.finish = function() {
    // TODO(abarth): Rather than resizing the buffer at the end, we could
    // compute the size we need ahead of time, like we do in C++.
    this.buffer.trim();
    var message = new Message(this.buffer, this.handles);
    this.buffer = null;
    this.handles = null;
    this.encoder = null;
    return message;
  };

  // MessageWithRequestIDBuilder -----------------------------------------------

  function MessageWithRequestIDBuilder(messageName, payloadSize, flags,
                                       requestID) {
    // Currently, we don't compute the payload size correctly ahead of time.
    // Instead, we resize the buffer at the end.
    var numberOfBytes = kMessageWithRequestIDHeaderSize + payloadSize;
    this.buffer = new buffer.Buffer(numberOfBytes);
    this.handles = [];
    var encoder = this.createEncoder(kMessageWithRequestIDHeaderSize);
    encoder.writeUint32(kMessageWithRequestIDHeaderSize);
    encoder.writeUint32(1);  // version.
    encoder.writeUint32(0);  // interface ID.
    encoder.writeUint32(messageName);
    encoder.writeUint32(flags);
    encoder.writeUint32(0);  // padding.
    encoder.writeUint64(requestID);
  }

  MessageWithRequestIDBuilder.prototype =
      Object.create(MessageBuilder.prototype);

  MessageWithRequestIDBuilder.prototype.constructor =
      MessageWithRequestIDBuilder;

  // MessageReader ------------------------------------------------------------

  function MessageReader(message) {
    this.decoder = new Decoder(message.buffer, message.handles, 0);
    var messageHeaderSize = this.decoder.readUint32();
    this.payloadSize = message.buffer.byteLength - messageHeaderSize;
    var version = this.decoder.readUint32();
    var interface_id = this.decoder.readUint32();
    if (interface_id != 0) {
      throw new Error("Receiving non-zero interface ID. Associated interfaces " +
                      "are not yet supported.");
    }
    this.messageName = this.decoder.readUint32();
    this.flags = this.decoder.readUint32();
    // Skip the padding.
    this.decoder.skip(4);
    if (version >= 1)
      this.requestID = this.decoder.readUint64();
    this.decoder.skip(messageHeaderSize - this.decoder.next);
  }

  MessageReader.prototype.decodeStruct = function(cls) {
    return cls.decode(this.decoder);
  };

  // Built-in types -----------------------------------------------------------

  // This type is only used with ArrayOf(PackedBool).
  function PackedBool() {
  }

  function Int8() {
  }

  Int8.encodedSize = 1;

  Int8.decode = function(decoder) {
    return decoder.readInt8();
  };

  Int8.encode = function(encoder, val) {
    encoder.writeInt8(val);
  };

  Uint8.encode = function(encoder, val) {
    encoder.writeUint8(val);
  };

  function Uint8() {
  }

  Uint8.encodedSize = 1;

  Uint8.decode = function(decoder) {
    return decoder.readUint8();
  };

  Uint8.encode = function(encoder, val) {
    encoder.writeUint8(val);
  };

  function Int16() {
  }

  Int16.encodedSize = 2;

  Int16.decode = function(decoder) {
    return decoder.readInt16();
  };

  Int16.encode = function(encoder, val) {
    encoder.writeInt16(val);
  };

  function Uint16() {
  }

  Uint16.encodedSize = 2;

  Uint16.decode = function(decoder) {
    return decoder.readUint16();
  };

  Uint16.encode = function(encoder, val) {
    encoder.writeUint16(val);
  };

  function Int32() {
  }

  Int32.encodedSize = 4;

  Int32.decode = function(decoder) {
    return decoder.readInt32();
  };

  Int32.encode = function(encoder, val) {
    encoder.writeInt32(val);
  };

  function Uint32() {
  }

  Uint32.encodedSize = 4;

  Uint32.decode = function(decoder) {
    return decoder.readUint32();
  };

  Uint32.encode = function(encoder, val) {
    encoder.writeUint32(val);
  };

  function Int64() {
  }

  Int64.encodedSize = 8;

  Int64.decode = function(decoder) {
    return decoder.readInt64();
  };

  Int64.encode = function(encoder, val) {
    encoder.writeInt64(val);
  };

  function Uint64() {
  }

  Uint64.encodedSize = 8;

  Uint64.decode = function(decoder) {
    return decoder.readUint64();
  };

  Uint64.encode = function(encoder, val) {
    encoder.writeUint64(val);
  };

  function String() {
  };

  String.encodedSize = 8;

  String.decode = function(decoder) {
    return decoder.decodeStringPointer();
  };

  String.encode = function(encoder, val) {
    encoder.encodeStringPointer(val);
  };

  function NullableString() {
  }

  NullableString.encodedSize = String.encodedSize;

  NullableString.decode = String.decode;

  NullableString.encode = String.encode;

  function Float() {
  }

  Float.encodedSize = 4;

  Float.decode = function(decoder) {
    return decoder.readFloat();
  };

  Float.encode = function(encoder, val) {
    encoder.writeFloat(val);
  };

  function Double() {
  }

  Double.encodedSize = 8;

  Double.decode = function(decoder) {
    return decoder.readDouble();
  };

  Double.encode = function(encoder, val) {
    encoder.writeDouble(val);
  };

  function PointerTo(cls) {
    this.cls = cls;
  }

  PointerTo.prototype.encodedSize = 8;

  PointerTo.prototype.decode = function(decoder) {
    var pointer = decoder.decodePointer();
    if (!pointer) {
      return null;
    }
    return this.cls.decode(decoder.decodeAndCreateDecoder(pointer));
  };

  PointerTo.prototype.encode = function(encoder, val) {
    if (!val) {
      encoder.encodePointer(val);
      return;
    }
    var objectEncoder = encoder.createAndEncodeEncoder(this.cls.encodedSize);
    this.cls.encode(objectEncoder, val);
  };

  function NullablePointerTo(cls) {
    PointerTo.call(this, cls);
  }

  NullablePointerTo.prototype = Object.create(PointerTo.prototype);

  function ArrayOf(cls, length) {
    this.cls = cls;
    this.length = length || 0;
  }

  ArrayOf.prototype.encodedSize = 8;

  ArrayOf.prototype.dimensions = function() {
    return [this.length].concat(
      (this.cls instanceof ArrayOf) ? this.cls.dimensions() : []);
  }

  ArrayOf.prototype.decode = function(decoder) {
    return decoder.decodeArrayPointer(this.cls);
  };

  ArrayOf.prototype.encode = function(encoder, val) {
    encoder.encodeArrayPointer(this.cls, val);
  };

  function NullableArrayOf(cls) {
    ArrayOf.call(this, cls);
  }

  NullableArrayOf.prototype = Object.create(ArrayOf.prototype);

  function Handle() {
  }

  Handle.encodedSize = 4;

  Handle.decode = function(decoder) {
    return decoder.decodeHandle();
  };

  Handle.encode = function(encoder, val) {
    encoder.encodeHandle(val);
  };

  function NullableHandle() {
  }

  NullableHandle.encodedSize = Handle.encodedSize;

  NullableHandle.decode = Handle.decode;

  NullableHandle.encode = Handle.encode;

  function Interface() {
  }

  Interface.encodedSize = 8;

  Interface.decode = function(decoder) {
    var handle = decoder.decodeHandle();
    // Ignore the version field for now.
    decoder.readUint32();

    return handle;
  };

  Interface.encode = function(encoder, val) {
    encoder.encodeHandle(val);
    // Set the version field to 0 for now.
    encoder.writeUint32(0);
  };

  function NullableInterface() {
  }

  NullableInterface.encodedSize = Interface.encodedSize;

  NullableInterface.decode = Interface.decode;

  NullableInterface.encode = Interface.encode;

  function MapOf(keyClass, valueClass) {
    this.keyClass = keyClass;
    this.valueClass = valueClass;
  }

  MapOf.prototype.encodedSize = 8;

  MapOf.prototype.decode = function(decoder) {
    return decoder.decodeMapPointer(this.keyClass, this.valueClass);
  };

  MapOf.prototype.encode = function(encoder, val) {
    encoder.encodeMapPointer(this.keyClass, this.valueClass, val);
  };

  function NullableMapOf(keyClass, valueClass) {
    MapOf.call(this, keyClass, valueClass);
  }

  NullableMapOf.prototype = Object.create(MapOf.prototype);

  var exports = {};
  exports.align = align;
  exports.isAligned = isAligned;
  exports.Message = Message;
  exports.MessageBuilder = MessageBuilder;
  exports.MessageWithRequestIDBuilder = MessageWithRequestIDBuilder;
  exports.MessageReader = MessageReader;
  exports.kArrayHeaderSize = kArrayHeaderSize;
  exports.kMapStructPayloadSize = kMapStructPayloadSize;
  exports.kStructHeaderSize = kStructHeaderSize;
  exports.kEncodedInvalidHandleValue = kEncodedInvalidHandleValue;
  exports.kMessageHeaderSize = kMessageHeaderSize;
  exports.kMessageWithRequestIDHeaderSize = kMessageWithRequestIDHeaderSize;
  exports.kMessageExpectsResponse = kMessageExpectsResponse;
  exports.kMessageIsResponse = kMessageIsResponse;
  exports.Int8 = Int8;
  exports.Uint8 = Uint8;
  exports.Int16 = Int16;
  exports.Uint16 = Uint16;
  exports.Int32 = Int32;
  exports.Uint32 = Uint32;
  exports.Int64 = Int64;
  exports.Uint64 = Uint64;
  exports.Float = Float;
  exports.Double = Double;
  exports.String = String;
  exports.NullableString = NullableString;
  exports.PointerTo = PointerTo;
  exports.NullablePointerTo = NullablePointerTo;
  exports.ArrayOf = ArrayOf;
  exports.NullableArrayOf = NullableArrayOf;
  exports.PackedBool = PackedBool;
  exports.Handle = Handle;
  exports.NullableHandle = NullableHandle;
  exports.Interface = Interface;
  exports.NullableInterface = NullableInterface;
  exports.MapOf = MapOf;
  exports.NullableMapOf = NullableMapOf;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/connection", [
  "mojo/public/js/bindings",
  "mojo/public/js/connector",
  "mojo/public/js/core",
  "mojo/public/js/router",
], function(bindings, connector, core, router) {

  var Router = router.Router;
  var EmptyProxy = bindings.EmptyProxy;
  var EmptyStub = bindings.EmptyStub;
  var ProxyBindings = bindings.ProxyBindings;
  var StubBindings = bindings.StubBindings;
  var TestConnector = connector.TestConnector;
  var TestRouter = router.TestRouter;

  // TODO(hansmuller): the proxy receiver_ property should be receiver$

  function BaseConnection(localStub, remoteProxy, router) {
    this.router_ = router;
    this.local = localStub;
    this.remote = remoteProxy;

    this.router_.setIncomingReceiver(localStub);
    if (this.remote)
      this.remote.receiver_ = router;

    // Validate incoming messages: remote responses and local requests.
    var validateRequest = localStub && localStub.validator;
    var validateResponse = remoteProxy && remoteProxy.validator;
    var payloadValidators = [];
    if (validateRequest)
      payloadValidators.push(validateRequest);
    if (validateResponse)
      payloadValidators.push(validateResponse);
    this.router_.setPayloadValidators(payloadValidators);
  }

  BaseConnection.prototype.close = function() {
    this.router_.close();
    this.router_ = null;
    this.local = null;
    this.remote = null;
  };

  BaseConnection.prototype.encounteredError = function() {
    return this.router_.encounteredError();
  };

  function Connection(
      handle, localFactory, remoteFactory, routerFactory, connectorFactory) {
    var routerClass = routerFactory || Router;
    var router = new routerClass(handle, connectorFactory);
    var remoteProxy = remoteFactory && new remoteFactory(router);
    var localStub = localFactory && new localFactory(remoteProxy);
    BaseConnection.call(this, localStub, remoteProxy, router);
  }

  Connection.prototype = Object.create(BaseConnection.prototype);

  // The TestConnection subclass is only intended to be used in unit tests.
  function TestConnection(handle, localFactory, remoteFactory) {
    Connection.call(this,
                    handle,
                    localFactory,
                    remoteFactory,
                    TestRouter,
                    TestConnector);
  }

  TestConnection.prototype = Object.create(Connection.prototype);

  // Return a handle for a message pipe that's connected to a proxy
  // for remoteInterface. Used by generated code for outgoing interface&
  // (request) parameters: the caller is given the generated proxy via
  // |proxyCallback(proxy)| and the generated code sends the handle
  // returned by this function.
  function bindProxy(proxyCallback, remoteInterface) {
    var messagePipe = core.createMessagePipe();
    if (messagePipe.result != core.RESULT_OK)
      throw new Error("createMessagePipe failed " + messagePipe.result);

    var proxy = new remoteInterface.proxyClass;
    var router = new Router(messagePipe.handle0);
    var connection = new BaseConnection(undefined, proxy, router);
    ProxyBindings(proxy).connection = connection;
    if (proxyCallback)
      proxyCallback(proxy);

    return messagePipe.handle1;
  }

  // Return a handle for a message pipe that's connected to a stub for
  // localInterface. Used by generated code for outgoing interface
  // parameters: the caller  is given the generated stub via
  // |stubCallback(stub)| and the generated code sends the handle
  // returned by this function. The caller is responsible for managing
  // the lifetime of the stub and for setting it's implementation
  // delegate with: StubBindings(stub).delegate = myImpl;
  function bindImpl(stubCallback, localInterface) {
    var messagePipe = core.createMessagePipe();
    if (messagePipe.result != core.RESULT_OK)
      throw new Error("createMessagePipe failed " + messagePipe.result);

    var stub = new localInterface.stubClass;
    var router = new Router(messagePipe.handle0);
    var connection = new BaseConnection(stub, undefined, router);
    StubBindings(stub).connection = connection;
    if (stubCallback)
      stubCallback(stub);

    return messagePipe.handle1;
  }

  // Return a remoteInterface proxy for handle. Used by generated code
  // for converting incoming interface parameters to proxies.
  function bindHandleToProxy(handle, remoteInterface) {
    if (!core.isHandle(handle))
      throw new Error("Not a handle " + handle);

    var proxy = new remoteInterface.proxyClass;
    var router = new Router(handle);
    var connection = new BaseConnection(undefined, proxy, router);
    ProxyBindings(proxy).connection = connection;
    return proxy;
  }

  // Return a localInterface stub for handle. Used by generated code
  // for converting incoming interface& request parameters to localInterface
  // stubs. The caller can specify the stub's implementation of localInterface
  // like this: StubBindings(stub).delegate = myStubImpl.
  function bindHandleToStub(handle, localInterface) {
    if (!core.isHandle(handle))
      throw new Error("Not a handle " + handle);

    var stub = new localInterface.stubClass;
    var router = new Router(handle);
    var connection = new BaseConnection(stub, undefined, router);
    StubBindings(stub).connection = connection;
    return stub;
  }

  var exports = {};
  exports.Connection = Connection;
  exports.TestConnection = TestConnection;

  exports.bindProxy = bindProxy;
  exports.bindImpl = bindImpl;
  exports.bindHandleToProxy = bindHandleToProxy;
  exports.bindHandleToStub = bindHandleToStub;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/connector", [
  "mojo/public/js/buffer",
  "mojo/public/js/codec",
  "mojo/public/js/core",
  "mojo/public/js/support",
], function(buffer, codec, core, support) {

  function Connector(handle) {
    if (!core.isHandle(handle))
      throw new Error("Connector: not a handle " + handle);
    this.handle_ = handle;
    this.dropWrites_ = false;
    this.error_ = false;
    this.incomingReceiver_ = null;
    this.readWatcher_ = null;
    this.errorHandler_ = null;

    if (handle) {
      this.readWatcher_ = support.watch(handle,
                                        core.HANDLE_SIGNAL_READABLE,
                                        this.readMore_.bind(this));
    }
  }

  Connector.prototype.close = function() {
    if (this.readWatcher_) {
      support.cancelWatch(this.readWatcher_);
      this.readWatcher_ = null;
    }
    if (this.handle_ != null) {
      core.close(this.handle_);
      this.handle_ = null;
    }
  };

  Connector.prototype.accept = function(message) {
    if (this.error_)
      return false;

    if (this.dropWrites_)
      return true;

    var result = core.writeMessage(this.handle_,
                                   new Uint8Array(message.buffer.arrayBuffer),
                                   message.handles,
                                   core.WRITE_MESSAGE_FLAG_NONE);
    switch (result) {
      case core.RESULT_OK:
        // The handles were successfully transferred, so we don't own them
        // anymore.
        message.handles = [];
        break;
      case core.RESULT_FAILED_PRECONDITION:
        // There's no point in continuing to write to this pipe since the other
        // end is gone. Avoid writing any future messages. Hide write failures
        // from the caller since we'd like them to continue consuming any
        // backlog of incoming messages before regarding the message pipe as
        // closed.
        this.dropWrites_ = true;
        break;
      default:
        // This particular write was rejected, presumably because of bad input.
        // The pipe is not necessarily in a bad state.
        return false;
    }
    return true;
  };

  Connector.prototype.setIncomingReceiver = function(receiver) {
    this.incomingReceiver_ = receiver;
  };

  Connector.prototype.setErrorHandler = function(handler) {
    this.errorHandler_ = handler;
  };

  Connector.prototype.encounteredError = function() {
    return this.error_;
  };

  Connector.prototype.readMore_ = function(result) {
    for (;;) {
      var read = core.readMessage(this.handle_,
                                  core.READ_MESSAGE_FLAG_NONE);
      if (this.handle_ == null) // The connector has been closed.
        return;
      if (read.result == core.RESULT_SHOULD_WAIT)
        return;
      if (read.result != core.RESULT_OK) {
        this.error_ = true;
        if (this.errorHandler_)
          this.errorHandler_.onError(read.result);
        return;
      }
      var messageBuffer = new buffer.Buffer(read.buffer);
      var message = new codec.Message(messageBuffer, read.handles);
      if (this.incomingReceiver_) {
          this.incomingReceiver_.accept(message);
      }
    }
  };

  // The TestConnector subclass is only intended to be used in unit tests. It
  // doesn't automatically listen for input messages. Instead, you need to
  // call waitForNextMessage to block and wait for the next incoming message.
  function TestConnector(handle) {
    Connector.call(this, handle);
  }

  TestConnector.prototype = Object.create(Connector.prototype);

  TestConnector.prototype.waitForNextMessage = function() {
    var wait = core.wait(this.handle_, core.HANDLE_SIGNAL_READABLE,
                         core.DEADLINE_INDEFINITE);
    this.readMore_(wait.result);
  }

  var exports = {};
  exports.Connector = Connector;
  exports.TestConnector = TestConnector;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/router", [
  "mojo/public/js/codec",
  "mojo/public/js/core",
  "mojo/public/js/connector",
  "mojo/public/js/validator",
], function(codec, core, connector, validator) {

  var Connector = connector.Connector;
  var MessageReader = codec.MessageReader;
  var Validator = validator.Validator;

  function Router(handle, connectorFactory) {
    if (!core.isHandle(handle))
      throw new Error("Router constructor: Not a handle");
    if (connectorFactory === undefined)
      connectorFactory = Connector;
    this.connector_ = new connectorFactory(handle);
    this.incomingReceiver_ = null;
    this.nextRequestID_ = 0;
    this.completers_ = new Map();
    this.payloadValidators_ = [];

    this.connector_.setIncomingReceiver({
        accept: this.handleIncomingMessage_.bind(this),
    });
    this.connector_.setErrorHandler({
        onError: this.handleConnectionError_.bind(this),
    });
  }

  Router.prototype.close = function() {
    this.completers_.clear();  // Drop any responders.
    this.connector_.close();
  };

  Router.prototype.accept = function(message) {
    this.connector_.accept(message);
  };

  Router.prototype.reject = function(message) {
    // TODO(mpcomplete): no way to trasmit errors over a Connection.
  };

  Router.prototype.acceptAndExpectResponse = function(message) {
    // Reserve 0 in case we want it to convey special meaning in the future.
    var requestID = this.nextRequestID_++;
    if (requestID == 0)
      requestID = this.nextRequestID_++;

    message.setRequestID(requestID);
    var result = this.connector_.accept(message);
    if (!result)
      return Promise.reject(Error("Connection error"));

    var completer = {};
    this.completers_.set(requestID, completer);
    return new Promise(function(resolve, reject) {
      completer.resolve = resolve;
      completer.reject = reject;
    });
  };

  Router.prototype.setIncomingReceiver = function(receiver) {
    this.incomingReceiver_ = receiver;
  };

  Router.prototype.setPayloadValidators = function(payloadValidators) {
    this.payloadValidators_ = payloadValidators;
  };

  Router.prototype.encounteredError = function() {
    return this.connector_.encounteredError();
  };

  Router.prototype.handleIncomingMessage_ = function(message) {
    var noError = validator.validationError.NONE;
    var messageValidator = new Validator(message);
    var err = messageValidator.validateMessageHeader();
    for (var i = 0; err === noError && i < this.payloadValidators_.length; ++i)
      err = this.payloadValidators_[i](messageValidator);

    if (err == noError)
      this.handleValidIncomingMessage_(message);
    else
      this.handleInvalidIncomingMessage_(message, err);
  };

  Router.prototype.handleValidIncomingMessage_ = function(message) {
    if (message.expectsResponse()) {
      if (this.incomingReceiver_) {
        this.incomingReceiver_.acceptWithResponder(message, this);
      } else {
        // If we receive a request expecting a response when the client is not
        // listening, then we have no choice but to tear down the pipe.
        this.close();
      }
    } else if (message.isResponse()) {
      var reader = new MessageReader(message);
      var requestID = reader.requestID;
      var completer = this.completers_.get(requestID);
      this.completers_.delete(requestID);
      completer.resolve(message);
    } else {
      if (this.incomingReceiver_)
        this.incomingReceiver_.accept(message);
    }
  }

  Router.prototype.handleInvalidIncomingMessage_ = function(message, error) {
    this.close();
  }

  Router.prototype.handleConnectionError_ = function(result) {
    this.completers_.forEach(function(value) {
      value.reject(result);
    });
    this.close();
  };

  // The TestRouter subclass is only intended to be used in unit tests.
  // It defeats valid message handling and delgates invalid message handling.

  function TestRouter(handle, connectorFactory) {
    Router.call(this, handle, connectorFactory);
  }

  TestRouter.prototype = Object.create(Router.prototype);

  TestRouter.prototype.handleValidIncomingMessage_ = function() {
  };

  TestRouter.prototype.handleInvalidIncomingMessage_ =
      function(message, error) {
        this.validationErrorHandler(error);
      };

  var exports = {};
  exports.Router = Router;
  exports.TestRouter = TestRouter;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Defines functions for translating between JavaScript strings and UTF8 strings
 * stored in ArrayBuffers. There is much room for optimization in this code if
 * it proves necessary.
 */
define("mojo/public/js/unicode", function() {
  /**
   * Decodes the UTF8 string from the given buffer.
   * @param {ArrayBufferView} buffer The buffer containing UTF8 string data.
   * @return {string} The corresponding JavaScript string.
   */
  function decodeUtf8String(buffer) {
    return decodeURIComponent(escape(String.fromCharCode.apply(null, buffer)));
  }

  /**
   * Encodes the given JavaScript string into UTF8.
   * @param {string} str The string to encode.
   * @param {ArrayBufferView} outputBuffer The buffer to contain the result.
   * Should be pre-allocated to hold enough space. Use |utf8Length| to determine
   * how much space is required.
   * @return {number} The number of bytes written to |outputBuffer|.
   */
  function encodeUtf8String(str, outputBuffer) {
    var utf8String = unescape(encodeURIComponent(str));
    if (outputBuffer.length < utf8String.length)
      throw new Error("Buffer too small for encodeUtf8String");
    for (var i = 0; i < outputBuffer.length && i < utf8String.length; i++)
      outputBuffer[i] = utf8String.charCodeAt(i);
    return i;
  }

  /**
   * Returns the number of bytes that a UTF8 encoding of the JavaScript string
   * |str| would occupy.
   */
  function utf8Length(str) {
    var utf8String = unescape(encodeURIComponent(str));
    return utf8String.length;
  }

  var exports = {};
  exports.decodeUtf8String = decodeUtf8String;
  exports.encodeUtf8String = encodeUtf8String;
  exports.utf8Length = utf8Length;
  return exports;
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("mojo/public/js/validator", [
  "mojo/public/js/codec",
], function(codec) {

  var validationError = {
    NONE: 'VALIDATION_ERROR_NONE',
    MISALIGNED_OBJECT: 'VALIDATION_ERROR_MISALIGNED_OBJECT',
    ILLEGAL_MEMORY_RANGE: 'VALIDATION_ERROR_ILLEGAL_MEMORY_RANGE',
    UNEXPECTED_STRUCT_HEADER: 'VALIDATION_ERROR_UNEXPECTED_STRUCT_HEADER',
    UNEXPECTED_ARRAY_HEADER: 'VALIDATION_ERROR_UNEXPECTED_ARRAY_HEADER',
    ILLEGAL_HANDLE: 'VALIDATION_ERROR_ILLEGAL_HANDLE',
    UNEXPECTED_INVALID_HANDLE: 'VALIDATION_ERROR_UNEXPECTED_INVALID_HANDLE',
    ILLEGAL_POINTER: 'VALIDATION_ERROR_ILLEGAL_POINTER',
    UNEXPECTED_NULL_POINTER: 'VALIDATION_ERROR_UNEXPECTED_NULL_POINTER',
    MESSAGE_HEADER_INVALID_FLAGS:
        'VALIDATION_ERROR_MESSAGE_HEADER_INVALID_FLAGS',
    MESSAGE_HEADER_MISSING_REQUEST_ID:
        'VALIDATION_ERROR_MESSAGE_HEADER_MISSING_REQUEST_ID',
    DIFFERENT_SIZED_ARRAYS_IN_MAP:
        'VALIDATION_ERROR_DIFFERENT_SIZED_ARRAYS_IN_MAP',
    INVALID_UNION_SIZE: 'VALIDATION_ERROR_INVALID_UNION_SIZE',
    UNEXPECTED_NULL_UNION: 'VALIDATION_ERROR_UNEXPECTED_NULL_UNION',
  };

  var NULL_MOJO_POINTER = "NULL_MOJO_POINTER";

  function isStringClass(cls) {
    return cls === codec.String || cls === codec.NullableString;
  }

  function isHandleClass(cls) {
    return cls === codec.Handle || cls === codec.NullableHandle;
  }

  function isInterfaceClass(cls) {
    return cls === codec.Interface || cls === codec.NullableInterface;
  }

  function isNullable(type) {
    return type === codec.NullableString || type === codec.NullableHandle ||
        type === codec.NullableInterface ||
        type instanceof codec.NullableArrayOf ||
        type instanceof codec.NullablePointerTo;
  }

  function Validator(message) {
    this.message = message;
    this.offset = 0;
    this.handleIndex = 0;
  }

  Object.defineProperty(Validator.prototype, "offsetLimit", {
    get: function() { return this.message.buffer.byteLength; }
  });

  Object.defineProperty(Validator.prototype, "handleIndexLimit", {
    get: function() { return this.message.handles.length; }
  });

  // True if we can safely allocate a block of bytes from start to
  // to start + numBytes.
  Validator.prototype.isValidRange = function(start, numBytes) {
    // Only positive JavaScript integers that are less than 2^53
    // (Number.MAX_SAFE_INTEGER) can be represented exactly.
    if (start < this.offset || numBytes <= 0 ||
        !Number.isSafeInteger(start) ||
        !Number.isSafeInteger(numBytes))
      return false;

    var newOffset = start + numBytes;
    if (!Number.isSafeInteger(newOffset) || newOffset > this.offsetLimit)
      return false;

    return true;
  }

  Validator.prototype.claimRange = function(start, numBytes) {
    if (this.isValidRange(start, numBytes)) {
      this.offset = start + numBytes;
      return true;
    }
    return false;
  }

  Validator.prototype.claimHandle = function(index) {
    if (index === codec.kEncodedInvalidHandleValue)
      return true;

    if (index < this.handleIndex || index >= this.handleIndexLimit)
      return false;

    // This is safe because handle indices are uint32.
    this.handleIndex = index + 1;
    return true;
  }

  Validator.prototype.validateHandle = function(offset, nullable) {
    var index = this.message.buffer.getUint32(offset);

    if (index === codec.kEncodedInvalidHandleValue)
      return nullable ?
          validationError.NONE : validationError.UNEXPECTED_INVALID_HANDLE;

    if (!this.claimHandle(index))
      return validationError.ILLEGAL_HANDLE;
    return validationError.NONE;
  }

  Validator.prototype.validateInterface = function(offset, nullable) {
    return this.validateHandle(offset, nullable);
  }

  Validator.prototype.validateStructHeader =
      function(offset, minNumBytes, minVersion) {
    if (!codec.isAligned(offset))
      return validationError.MISALIGNED_OBJECT;

    if (!this.isValidRange(offset, codec.kStructHeaderSize))
      return validationError.ILLEGAL_MEMORY_RANGE;

    var numBytes = this.message.buffer.getUint32(offset);
    var version = this.message.buffer.getUint32(offset + 4);

    // Backward compatibility is not yet supported.
    if (numBytes < minNumBytes || version < minVersion)
      return validationError.UNEXPECTED_STRUCT_HEADER;

    if (!this.claimRange(offset, numBytes))
      return validationError.ILLEGAL_MEMORY_RANGE;

    return validationError.NONE;
  }

  Validator.prototype.validateMessageHeader = function() {
    var err = this.validateStructHeader(0, codec.kMessageHeaderSize, 0);
    if (err != validationError.NONE)
      return err;

    var numBytes = this.message.getHeaderNumBytes();
    var version = this.message.getHeaderVersion();

    var validVersionAndNumBytes =
        (version == 0 && numBytes == codec.kMessageHeaderSize) ||
        (version == 1 &&
         numBytes == codec.kMessageWithRequestIDHeaderSize) ||
        (version > 1 &&
         numBytes >= codec.kMessageWithRequestIDHeaderSize);
    if (!validVersionAndNumBytes)
      return validationError.UNEXPECTED_STRUCT_HEADER;

    var expectsResponse = this.message.expectsResponse();
    var isResponse = this.message.isResponse();

    if (version == 0 && (expectsResponse || isResponse))
      return validationError.MESSAGE_HEADER_MISSING_REQUEST_ID;

    if (isResponse && expectsResponse)
      return validationError.MESSAGE_HEADER_INVALID_FLAGS;

    return validationError.NONE;
  }

  // Returns the message.buffer relative offset this pointer "points to",
  // NULL_MOJO_POINTER if the pointer represents a null, or JS null if the
  // pointer's value is not valid.
  Validator.prototype.decodePointer = function(offset) {
    var pointerValue = this.message.buffer.getUint64(offset);
    if (pointerValue === 0)
      return NULL_MOJO_POINTER;
    var bufferOffset = offset + pointerValue;
    return Number.isSafeInteger(bufferOffset) ? bufferOffset : null;
  }

  Validator.prototype.decodeUnionSize = function(offset) {
    return this.message.buffer.getUint32(offset);
  };

  Validator.prototype.decodeUnionTag = function(offset) {
    return this.message.buffer.getUint32(offset + 4);
  };

  Validator.prototype.validateArrayPointer = function(
      offset, elementSize, elementType, nullable, expectedDimensionSizes,
      currentDimension) {
    var arrayOffset = this.decodePointer(offset);
    if (arrayOffset === null)
      return validationError.ILLEGAL_POINTER;

    if (arrayOffset === NULL_MOJO_POINTER)
      return nullable ?
          validationError.NONE : validationError.UNEXPECTED_NULL_POINTER;

    return this.validateArray(arrayOffset, elementSize, elementType,
                              expectedDimensionSizes, currentDimension);
  }

  Validator.prototype.validateStructPointer = function(
      offset, structClass, nullable) {
    var structOffset = this.decodePointer(offset);
    if (structOffset === null)
      return validationError.ILLEGAL_POINTER;

    if (structOffset === NULL_MOJO_POINTER)
      return nullable ?
          validationError.NONE : validationError.UNEXPECTED_NULL_POINTER;

    return structClass.validate(this, structOffset);
  }

  Validator.prototype.validateUnion = function(
      offset, unionClass, nullable) {
    var size = this.message.buffer.getUint32(offset);
    if (size == 0) {
      return nullable ?
          validationError.NONE : validationError.UNEXPECTED_NULL_UNION;
    }

    return unionClass.validate(this, offset);
  }

  Validator.prototype.validateNestedUnion = function(
      offset, unionClass, nullable) {
    var unionOffset = this.decodePointer(offset);
    if (unionOffset === null)
      return validationError.ILLEGAL_POINTER;

    if (unionOffset === NULL_MOJO_POINTER)
      return nullable ?
          validationError.NONE : validationError.UNEXPECTED_NULL_UNION;

    return this.validateUnion(unionOffset, unionClass, nullable);
  }

  // This method assumes that the array at arrayPointerOffset has
  // been validated.

  Validator.prototype.arrayLength = function(arrayPointerOffset) {
    var arrayOffset = this.decodePointer(arrayPointerOffset);
    return this.message.buffer.getUint32(arrayOffset + 4);
  }

  Validator.prototype.validateMapPointer = function(
      offset, mapIsNullable, keyClass, valueClass, valueIsNullable) {
    // Validate the implicit map struct:
    // struct {array<keyClass> keys; array<valueClass> values};
    var structOffset = this.decodePointer(offset);
    if (structOffset === null)
      return validationError.ILLEGAL_POINTER;

    if (structOffset === NULL_MOJO_POINTER)
      return mapIsNullable ?
          validationError.NONE : validationError.UNEXPECTED_NULL_POINTER;

    var mapEncodedSize = codec.kStructHeaderSize + codec.kMapStructPayloadSize;
    var err = this.validateStructHeader(structOffset, mapEncodedSize, 0);
    if (err !== validationError.NONE)
        return err;

    // Validate the keys array.
    var keysArrayPointerOffset = structOffset + codec.kStructHeaderSize;
    err = this.validateArrayPointer(
        keysArrayPointerOffset, keyClass.encodedSize, keyClass, false, [0], 0);
    if (err !== validationError.NONE)
        return err;

    // Validate the values array.
    var valuesArrayPointerOffset = keysArrayPointerOffset + 8;
    var valuesArrayDimensions = [0]; // Validate the actual length below.
    if (valueClass instanceof codec.ArrayOf)
      valuesArrayDimensions =
          valuesArrayDimensions.concat(valueClass.dimensions());
    var err = this.validateArrayPointer(valuesArrayPointerOffset,
                                        valueClass.encodedSize,
                                        valueClass,
                                        valueIsNullable,
                                        valuesArrayDimensions,
                                        0);
    if (err !== validationError.NONE)
        return err;

    // Validate the lengths of the keys and values arrays.
    var keysArrayLength = this.arrayLength(keysArrayPointerOffset);
    var valuesArrayLength = this.arrayLength(valuesArrayPointerOffset);
    if (keysArrayLength != valuesArrayLength)
      return validationError.DIFFERENT_SIZED_ARRAYS_IN_MAP;

    return validationError.NONE;
  }

  Validator.prototype.validateStringPointer = function(offset, nullable) {
    return this.validateArrayPointer(
        offset, codec.Uint8.encodedSize, codec.Uint8, nullable, [0], 0);
  }

  // Similar to Array_Data<T>::Validate()
  // mojo/public/cpp/bindings/lib/array_internal.h

  Validator.prototype.validateArray =
      function (offset, elementSize, elementType, expectedDimensionSizes,
                currentDimension) {
    if (!codec.isAligned(offset))
      return validationError.MISALIGNED_OBJECT;

    if (!this.isValidRange(offset, codec.kArrayHeaderSize))
      return validationError.ILLEGAL_MEMORY_RANGE;

    var numBytes = this.message.buffer.getUint32(offset);
    var numElements = this.message.buffer.getUint32(offset + 4);

    // Note: this computation is "safe" because elementSize <= 8 and
    // numElements is a uint32.
    var elementsTotalSize = (elementType === codec.PackedBool) ?
        Math.ceil(numElements / 8) : (elementSize * numElements);

    if (numBytes < codec.kArrayHeaderSize + elementsTotalSize)
      return validationError.UNEXPECTED_ARRAY_HEADER;

    if (expectedDimensionSizes[currentDimension] != 0 &&
        numElements != expectedDimensionSizes[currentDimension]) {
      return validationError.UNEXPECTED_ARRAY_HEADER;
    }

    if (!this.claimRange(offset, numBytes))
      return validationError.ILLEGAL_MEMORY_RANGE;

    // Validate the array's elements if they are pointers or handles.

    var elementsOffset = offset + codec.kArrayHeaderSize;
    var nullable = isNullable(elementType);

    if (isHandleClass(elementType))
      return this.validateHandleElements(elementsOffset, numElements, nullable);
    if (isInterfaceClass(elementType))
      return this.validateInterfaceElements(
          elementsOffset, numElements, nullable);
    if (isStringClass(elementType))
      return this.validateArrayElements(
          elementsOffset, numElements, codec.Uint8, nullable, [0], 0);
    if (elementType instanceof codec.PointerTo)
      return this.validateStructElements(
          elementsOffset, numElements, elementType.cls, nullable);
    if (elementType instanceof codec.ArrayOf)
      return this.validateArrayElements(
          elementsOffset, numElements, elementType.cls, nullable,
          expectedDimensionSizes, currentDimension + 1);

    return validationError.NONE;
  }

  // Note: the |offset + i * elementSize| computation in the validateFooElements
  // methods below is "safe" because elementSize <= 8, offset and
  // numElements are uint32, and 0 <= i < numElements.

  Validator.prototype.validateHandleElements =
      function(offset, numElements, nullable) {
    var elementSize = codec.Handle.encodedSize;
    for (var i = 0; i < numElements; i++) {
      var elementOffset = offset + i * elementSize;
      var err = this.validateHandle(elementOffset, nullable);
      if (err != validationError.NONE)
        return err;
    }
    return validationError.NONE;
  }

  Validator.prototype.validateInterfaceElements =
      function(offset, numElements, nullable) {
    var elementSize = codec.Interface.encodedSize;
    for (var i = 0; i < numElements; i++) {
      var elementOffset = offset + i * elementSize;
      var err = this.validateInterface(elementOffset, nullable);
      if (err != validationError.NONE)
        return err;
    }
    return validationError.NONE;
  }

  // The elementClass parameter is the element type of the element arrays.
  Validator.prototype.validateArrayElements =
      function(offset, numElements, elementClass, nullable,
               expectedDimensionSizes, currentDimension) {
    var elementSize = codec.PointerTo.prototype.encodedSize;
    for (var i = 0; i < numElements; i++) {
      var elementOffset = offset + i * elementSize;
      var err = this.validateArrayPointer(
          elementOffset, elementClass.encodedSize, elementClass, nullable,
          expectedDimensionSizes, currentDimension);
      if (err != validationError.NONE)
        return err;
    }
    return validationError.NONE;
  }

  Validator.prototype.validateStructElements =
      function(offset, numElements, structClass, nullable) {
    var elementSize = codec.PointerTo.prototype.encodedSize;
    for (var i = 0; i < numElements; i++) {
      var elementOffset = offset + i * elementSize;
      var err =
          this.validateStructPointer(elementOffset, structClass, nullable);
      if (err != validationError.NONE)
        return err;
    }
    return validationError.NONE;
  }

  var exports = {};
  exports.validationError = validationError;
  exports.Validator = Validator;
  return exports;
});
<html>
<head>
<title>CEF remote debugging</title>
<style>
</style>

<script>
function onLoad() {
  var tabs_list_request = new XMLHttpRequest();
  tabs_list_request.open("GET", "/json/list?t=" + new Date().getTime(), true);
  tabs_list_request.onreadystatechange = onReady;
  tabs_list_request.send();
}

function onReady() {
  if(this.readyState == 4 && this.status == 200) {
    if(this.response != null)
      var responseJSON = JSON.parse(this.response);
      for (var i = 0; i < responseJSON.length; ++i)
        appendItem(responseJSON[i]);
  }
}

function appendItem(item_object) {
  var frontend_ref;
  if (item_object.devtoolsFrontendUrl) {
    frontend_ref = document.createElement("a");
    frontend_ref.href = item_object.devtoolsFrontendUrl;
    frontend_ref.title = item_object.title;
  } else {
    frontend_ref = document.createElement("div");
    frontend_ref.title = "The tab already has active debugging session";
  }

  var text = document.createElement("div");
  if (item_object.title)
    text.innerText = item_object.title;
  else
    text.innerText = "(untitled tab)";
  text.style.cssText = "background-image:url(" + item_object.faviconUrl + ")";
  frontend_ref.appendChild(text);

  var item = document.createElement("p");
  item.appendChild(frontend_ref);

  document.getElementById("items").appendChild(item);
}
</script>
</head>
<body onload='onLoad()'>
  <div id='caption'>Inspectable WebContents</div>
  <div id='items'></div>
</body>
</html>
// Copyright (c) 2008-2014 Marshall A. Greenblatt. Portions Copyright (c)
// 2006-2009 Google Inc. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the name Chromium Embedded
// Framework nor the names of its contributors may be used to endorse
// or promote products derived from this software without specific prior
// written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
<!DOCTYPE HTML>

<!--
about:version template page
-->

<html id="t" i18n-values="dir:textdirection;">
  <head>
    <title>About Version</title>

    <style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

body {
  background-color: white;
  color: black;
  font-family: Helvetica,Arial,sans-serif;
  margin: 0;
}

#outer {
  margin-left: auto;
  margin-right: auto;
  margin-top: 10px;
  width: 820px;
}

#inner {
  padding-top: 10px;
  width: 550px;
}

.label {
  -webkit-padding-end: 5px;
  font-size: 0.9em;
  font-weight: bold;
  text-align: end;
  white-space: nowrap;
}

.label:after {
  content: ':';
}

#logo {
  float: right;
  margin-left: 40px;
  text-align: right;
  width: 200px;
}

#company {
  font-size: 0.7em;
  text-align: right;
}

#copyright {
  font-size: 0.7em;
  text-align: right;
}

.value {
  font-family: monospace;
  max-width: 430px;
  padding-left: 5px;
}
</style>

  </head>

  <body>
    <div id="outer">
      <div id="logo">
        <div id="company">Chromium Embedded Framework (CEF)</div>
        <div id="copyright">Copyright &copy; $$YEAR$$ The Chromium Embedded Framework Authors.<br/>All rights reserved.<br/><a href="chrome://license">license</a> | <a href="chrome://credits">credits</a></div>
      </div>
      <table id="inner" cellpadding="0" cellspacing="0" border="0">
        <tr>
          <td class="label" valign="top">CEF</td>
          <td class="value">$$CEF$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">Chromium</td>
          <td class="value">$$CHROMIUM$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">OS</td>
          <td class="value">$$OS$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">WebKit</td>
          <td class="value">$$WEBKIT$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">JavaScript</td>
          <td class="value">$$JAVASCRIPT$$</td>
        </tr>
          <tr><td class="label" valign="top">Flash</td>
          <td class="value" id="flash">$$FLASH$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">User Agent</td>
          <td class="value">$$USERAGENT$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">Command Line</td>
          <td class="value">$$COMMANDLINE$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">Module Path</td>
          <td class="value">$$MODULEPATH$$</td>
        </tr>
        <tr>
          <td class="label" valign="top">Cache Path</td>
          <td class="value">$$CACHEPATH$$</td>
        </tr>
      </table>
    </div>
  </body>

</html>

// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This file defines extension APIs implemented in CEF.
// See extensions/common/features/* to understand this file, in particular
// feature.h, simple_feature.h, and base_feature_provider.h.

{
  // From chrome/common/extensions/api/_api_features.json.
  // Required by the PDF extension which is hosted in a guest view.
  "mimeHandlerViewGuestInternal": {
    "internal": true,
    "contexts": "all",
    "channel": "stable",
    "matches": ["<all_urls>"]
  }
}
{
  // chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai
  "manifest_version": 2,
  "key": "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDN6hM0rsDYGbzQPQfOygqlRtQgKUXMfnSjhIBL7LnReAVBEd7ZmKtyN2qmSasMl4HZpMhVe2rPWVVwBDl6iyNE/Kok6E6v6V3vCLGsOpQAuuNVye/3QxzIldzG/jQAdWZiyXReRVapOhZtLjGfywCvlWq7Sl/e3sbc0vWybSDI2QIDAQAB",
  "name": "<NAME>",
  "version": "1",
  "description": "",
  "offline_enabled": true,
  "incognito": "split",
  "permissions": [
    "<all_urls>",
    "resourcesPrivate"
  ],
  "mime_types": [
    "application/pdf"
  ],
  "content_security_policy": "script-src 'self' blob: filesystem: chrome://resources; object-src * blob: filesystem: data:; plugin-types application/x-google-chrome-pdf",
  "mime_types_handler": "index.html",
  "web_accessible_resources": [
    "*.js",
    "*.html",
    "*.css",
    "*.png"
  ]
}
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, user-scalable=no">
<script>
function setMessage(msg) {
  document.getElementById('message').textContent = msg;
}
function notifyDidFinishLoading() {
  if (plugin.didFinishLoading)
    plugin.didFinishLoading();
}
</script>

<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

html, body {
  -webkit-user-select: none;
  font-family: sans-serif;
  height: 100%;
  margin: 0;
  overflow: hidden;
  text-align: center;
  width: 100%;
}

h1 {
  font-size: 10pt;
  font-weight: normal;
  padding: 0pt 10pt;
visibility: hidden;
}

#outer:hover h1, #outer:hover #close {
  visibility: visible;
}

p {
  font-size: 8pt;
  padding: 0pt 14pt;
}

#outer {
  align-items: center;
  border: 1px black solid;
  box-sizing: border-box;
  display: flex;
  height: 100%;
  justify-content: center;
  position: absolute;
  width: 100%;
}

#close {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAiElEQVR42r2RsQrDMAxEBRdl8SDcX8lQPGg1GBI6lvz/h7QyRRXV0qUULwfvwZ1tenw5PxToRPWMC52eA9+WDnlh3HFQ/xBQl86NFYJqeGflkiogrOvVlIFhqURFVho3x1moGAa3deMs+LS30CAhBN5nNxeT5hbJ1zwmji2k+aF6NENIPf/hs54f0sZFUVAMigAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAA9UlEQVR4Xu3UsWrCUByH0fMEouiuhrg4xohToJVGH0CHLBncEwfx/VvIFHLJBWmHDvKbv7PcP9f3L/fXwBsApZSRpUpEgbOnxwiReng6x4AvjdrNXRLkibubWqMcB9Yujk7qjhjmtZOji/U4wELuoBwQXa50kFsQA5jK+kQ/l5kSA4ZEK5Fo+3kcCIlGM8ijQEhUqkEeBUKiUPTyl4C5vZ1cbmdv/iqwclXY6aZwtXoFSLQqhVwmkytUWglxAMG7T0yCu4gD0v7ZBKeVxoEwFxIxYBPmIWEzDnyEeUj4HAfYdvmMcGYdsSUGsOzlIbHEv/uV38APrreiBRBIs3QAAAAASUVORK5CYII=) 2x);
  background-position: right top;
  background-repeat: no-repeat;
  cursor: pointer;
  height: 14px;
  position: absolute;
  right: 3px;
  top: 3px;
visibility: hidden;
  width: 14px;
}

#close[data-plugin-type='document'] {
  display: none;
}

#close:hover {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAqUlEQVR4XqWRMQqEMBBF/1E8Ra6x6V3FRnS9QbCxtJg6Z7CzE9lTiIXXyUb3C8EULixDIMM8Zt4kcDfxM5A45U+cgeXnC1tREgkzAgob3hiq3CUHvGLG4FTQoSgxQGDrzN8WTLBGnx2IVDksen9GH7Z9hA5E6uxABMJyCHDMCEGHzugLQPPlBCBNGq+5YtpnGw1Bv+te15ypljTpVzdak5Opy+z+qf//zQ+Lg+07ay5KsgAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAB4UlEQVR42u2VsWoCQRBAh+MUFP0C1V9QD4NEOxs9xBQHQVCwSJFWVBAtBNXCxk6wTkBJYUTwEwQLC61E8QP0NzZzt5g5726DkC7EYWHZ8T3WndkV2C/jLwn4hwVYBIdLn9vkLp79QcBCTDMiy3w2gQ9XeTYkEHA8vqj2rworXu3HF1YFfSWgp5QFnKVLvYvzDEKEZ5hW70oXOCtcEbQLIkx7+IQtfMBSOjU6XEF4oyOdYInZbXyOuajjDlpNeQgleIUJKUz4BDMledhqOu/AzVSmzZ49CUjCC0yvim98iqtJT2L2jKsqczsdok9XrHNexaww415lnTNwn6CM/KxJIR8bnUZHPhLO6yMoIyk2pNjLewFuE5AiY1KMMQx8Q7hQYFek4AkjxXFe1rsF84I/BTFQMGL+1Lxwl4DwdtM1gjwKohgxyLtG7SYpxALqugOMcfOKN+bFXeBsLB1uulNcRqq7/tt36k41zoL6QlxGjtd6lrahiqCi1iOFYyvXuxY8yzK33VnvUivbLlOlj/jktm0s3YnXrNIXXufHNxuOGasi8S68zkwrlnV8ZcJJsTIUxbLgQcFZWE8N0gau2p40VVcM0gYeFpSRK6445UhBuKiRgiyKw+34rLt59nb1/7+RwReVkaFtqvNBuwAAAABJRU5ErkJggg==) 2x);
}

#close:active {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAQklEQVR4AWP4TwBSTQGDHcMZIIYAKA9VwRkwtINJgyCaCTAlCBaKAoQ+hFmoCqBKENKkK8C0gpAjCXuTyICiQ2QBAPSwyG3ByZlCAAAAAElFTkSuQmCC) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAA/ElEQVR4Xu3UsWrCUBiG4efGlIBoIMFbcnYolYJ3pg4iKGrGYFTRwaUFhYAekiDt0EG++X2W83N8/3J/DbwBMJJSsdQItcDY1VlCOImzq3Ed8OmicHASB3ns5KBw8VUNpDJrW7uAiJ3sbK1l0mqArpmFTUlQ5jYWZrrUAUSmT0SZm4qoA56JvVhs/5g3A7RLolA85A1ASOTye65NMxASK6syfxGITMzvMxG9CvRkliWwlOm9AsSOcitzU1NzK7mjuBkQvHtLK7iLBiB5PhttJSGpB8I8vM6kDuiHeUjoVwMfYR4SRtUAw1veIZzOjRhSBzCoyKFjgH/3K7+BHzg+Cgw0eSW3AAAAAElFTkSuQmCC) 2x);
}
</style>
<style>
body {
  background-color: rgb(187, 187, 187);
}

#plugin_icon {
  opacity: .6;
}
</style>
</head>

<body id="t" onload="notifyDidFinishLoading();">
<div i18n-values="title:name" id="outer">
  <div id="inner">
<div><img id="plugin_icon" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEEAAABBCAQAAAAk/gHOAAAIhklEQVR4Xr2ZXYidRxnHfzPvxzl7dpPdZLfaZGOzkqQ1kUAtrWkQ1FAI1kJDoSLpjfRGQUUR9K5eibdi9ULJjQjeWCJY0haMNHdS1MYKSqtma82ar2bbTXaTc/bsOe87IwzDAw8PJ3vnvMy7u/P1/Of5mv+86yLwWr381WvPDo+72rNdialO7oNI1KNxuU3GjHhjz9mDZ06PAFzkpcU3XmkerqgocWgQEYfT07VAC0AqAii9dQsB9/cHn/z6FXCv1uffjEc7dCgoBQC4Cfs0Io12LJio2locjpYG/69nHznWdy9+89JPp+jQxVPgcEq8IwJOizJ7NCAsNAETpK2hZfGH3/5+eeXLVQJQUuDxOLYvUWoCZ/wj4EQkqgaZAeC4/BQ/KR77eV10qagSBG0Al6oz4p3SkR2HALCQtZb681/8hXd1kYQ70CZQv2PaHfKAmh2lTQyIQFZ/e6iY9h4vC2oADsQ9NcTAiAF9BoxlTyIOp3arAToZL6HqvBUv6GUvsqvs0TUHOMJhDjBDn8bGj967AJOi/vZ2KPf82TLHCb7A0zzD05xkP5uMzC7FXDZ/mHavABj12R1Mc5IjdAk0VBzkFPu4SyvjJGyteNGkzqNeATAer70YIkfZR0PIwsbs5BAwFpgWihQVjgKRErt7nZ7VsmMeoiWqJQugIcqyNkmjAelMglcoLRgFB1o6xq1aWoLds8DXa9ji75kDTch5rlKCOniucYsa8RoZKSOk2MQeLQQscuWOBedZoxCLwjkuMEspoBJgAkFlAZ2aooJXYtxFB1QSL0AK1vkl38iL1bzGr1hkFk9QlnaWKcgaQZ0tUIposT55mMfhTXbosMkmvTT+Llco2E0HR1Qu3BIJOQQta3DiOwEoRUni09Cyh8Aqq7QJSJkhtLT0OMF0EhGZYS+OQQbdphrTqN1M0WWdMaUyqQagtaB2scUSn05LBoY4+rl/ipIuSFhu8gSf4iZjajxTuFS7+CzgRbaYwWnSYqKjRBQk6YKAx+f3DNATmJ6oF2SOuYmM6R32q6jSviK+oHBNICEOsaFlTnq+CrjbLDLWEUBUY1R2jHgkrg2J1fuUat9SIuv4DFoEqwdCAigQHEHYQIdqG0aIFZ+qPqIKvNGdBhJAfEH4nksQukQrYLIODNSg+KLAkzdoX5AOgUNQIWT5n4yfbCYtSHqtL3iZojyhEBFBCcLsxPBnI3A7g5aW47bsSMnHTrDRHW2/4c/WHfWs0l5GCm6zzv2wjf21eCu8osntDQ0hZ9loWVO0eBnTZIuX1HQobRQoSm7DDWCKXhINe1liKW1qaE9KvCASfIE+m0Cky1Xe4iIrFEQrcJKSJQqe4gYDljjFlzjNMzxMZNPMLJXb5ONnxIiCKd7kVW5QsMgRnmRLRFlfsEEbgJNscR9HmWFMyywnGPFHGnF2laD1zir+yQYt59lggQ43eY9ZHssAJ13xrd93eZ5GSH5DyTHe4wo71Zwy70zxnA6X+RsDujxANxlhhnMcpdJkzrqWucTeVvDGfIQu/Xx6CoR81iuC5uiwmzkKikxkugz4C48TwAbaxBxq80RFZIuQTSEJOiQISZQwHUeFA+Xfyzxu438bPWhCR9a2NmDZSiMERd0cXg2eMeLiNulpmlvMM6CR3n7yCK1B3+g0S0g28+wiqMlDPiMnhwnI1BOMJn7EC/yAf1NDJnqXuEpPG4pSuDJIgq74HA8ReYn/0KMCRtzPPGFSnlTnScjr3eAPLHCDc5zmY7R43uW3rLGgyH1O0EExpi0OcIhpIs9xhsv0iOzmOcJEgiYFJ7aHLWrm6bDBGbrsYpUbwC7RiUDA8ls8BS2Biue5zpiKjzA10QmlgoqYHi6tVDPiFjdpEw+pcFoiJTrrS7BEYpq0X/WItXGUNLQTaVtM/YmqUlPRyiUpCkVCDKFYExR8yDpTmrwq1XmGXGeVfSwkfdnz0uHYIR8BHEgmUJc7gWDukoFG8UGn9jnkZc4xRwXAKZ5giLMsi8iI1mRR5T+aO0bB5tngDk5bWGj8mLP8jiV6FEnIK8DnGSPw5e34BI2l+gJI+YL+PONZzxAsNw78l99wlB1UQKQGVnifBYLiz6kyy6rocDLf9FiHyvdDewJAYIW99Cilr2bIECSwg2jNsUCjVrbJHKGv5rbT0OqJkjnXqClwBGnrUon6dewcYGhuUagWMUR6ZDpAS7AQUj3MBcYUuaUFlpiX0doUe2kzTM2ttBbEF3QJBJt4AMfH+SRXKShyKv4sx7L6tcEAegR9kbexkemrzfaZ6QhuxfS+wjx3icCIQzxKJJg1HA6fnpZctAFsdozq21DBNQbMMRI2mXMbjpLIHlaYwSef38m6fK92+YkMGTFkjVLrUwvXxC3gkeCh4AP+xN7keD2qJAoCLXe4zQpv08n2fZ+bTGfXHDCkzx36DLjDZnp38UCwGgXlCyqxAjiGnKdJ0T9Hl5AvNmNWucYVdrELl6Au8zIfTVQMNujzAWt8yIiaTqo9CoIRHuVQj3JY60SMo0oxPWbE9bT7f0jaqtlPhQfA0/A2FxmnvjI5aYcHxCTgRCz2N+ELimbIpCSs1gEJxrUKdjCj/EgdbhPygTgt6O8L5O9LOroRWIYlSl8xMf/FSdTWuqPQV7WjAOYDJjbr6xaVJ+2V3mq0ZEQtwYI330oFijWDVawWmB4jXDvkAEoutscrTUDVRHX2KbFmeW1AgRQNcPE5esuEcvHXN4+3FKhiVa4EWRqXi7G8BRBAgDYc/D2b5ZGfXftaebi1/z0w1g6gdmGF258RXaJy7tHyd8+y5iI/3vfn17sPdinwRD14u38D2/dEwRGUcRq2lr/3rUcv846LwFvTF1549+TGI0i5l3qxDnZPXdgSmP3rode/c3Zmg0uMXR7kuI85dvD/K3e5lXgd/wNMUxeBJGbvdAAAAABJRU5ErkJggg=="></div>

    <h1 id="message" i18n-content="message"></h1>
  </div>
  <div id="close" i18n-values="title:hide;data-plugin-type:pluginType"
      onclick="plugin.hide();">
  </div>
</div>
</body>
</html>
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, user-scalable=no">
<script>
  window.onload = function() {
    if (plugin.didFinishLoading)
      plugin.didFinishLoading();
  };

  window.onkeydown = function(e) {
    if (e.keyIdentifier == 'Enter' || e.keyIdentifier == 'U+0020') {
      plugin.load();
      e.preventDefault();
    }
  };
</script>
<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

html, body {
  -webkit-user-select: none;
  font-family: sans-serif;
  height: 100%;
  margin: 0;
  overflow: hidden;
  text-align: center;
  width: 100%;
}

h1 {
  font-size: 10pt;
  font-weight: normal;
  padding: 0pt 10pt;
visibility: hidden;
}

#outer:hover h1, #outer:hover #close {
  visibility: visible;
}

p {
  font-size: 8pt;
  padding: 0pt 14pt;
}

#outer {
  align-items: center;
  border: 1px black solid;
  box-sizing: border-box;
  display: flex;
  height: 100%;
  justify-content: center;
  position: absolute;
  width: 100%;
}

#close {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAiElEQVR42r2RsQrDMAxEBRdl8SDcX8lQPGg1GBI6lvz/h7QyRRXV0qUULwfvwZ1tenw5PxToRPWMC52eA9+WDnlh3HFQ/xBQl86NFYJqeGflkiogrOvVlIFhqURFVho3x1moGAa3deMs+LS30CAhBN5nNxeT5hbJ1zwmji2k+aF6NENIPf/hs54f0sZFUVAMigAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAA9UlEQVR4Xu3UsWrCUByH0fMEouiuhrg4xohToJVGH0CHLBncEwfx/VvIFHLJBWmHDvKbv7PcP9f3L/fXwBsApZSRpUpEgbOnxwiReng6x4AvjdrNXRLkibubWqMcB9Yujk7qjhjmtZOji/U4wELuoBwQXa50kFsQA5jK+kQ/l5kSA4ZEK5Fo+3kcCIlGM8ijQEhUqkEeBUKiUPTyl4C5vZ1cbmdv/iqwclXY6aZwtXoFSLQqhVwmkytUWglxAMG7T0yCu4gD0v7ZBKeVxoEwFxIxYBPmIWEzDnyEeUj4HAfYdvmMcGYdsSUGsOzlIbHEv/uV38APrreiBRBIs3QAAAAASUVORK5CYII=) 2x);
  background-position: right top;
  background-repeat: no-repeat;
  cursor: pointer;
  height: 14px;
  position: absolute;
  right: 3px;
  top: 3px;
visibility: hidden;
  width: 14px;
}

#close[data-plugin-type='document'] {
  display: none;
}

#close:hover {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAqUlEQVR4XqWRMQqEMBBF/1E8Ra6x6V3FRnS9QbCxtJg6Z7CzE9lTiIXXyUb3C8EULixDIMM8Zt4kcDfxM5A45U+cgeXnC1tREgkzAgob3hiq3CUHvGLG4FTQoSgxQGDrzN8WTLBGnx2IVDksen9GH7Z9hA5E6uxABMJyCHDMCEGHzugLQPPlBCBNGq+5YtpnGw1Bv+te15ypljTpVzdak5Opy+z+qf//zQ+Lg+07ay5KsgAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAB4UlEQVR42u2VsWoCQRBAh+MUFP0C1V9QD4NEOxs9xBQHQVCwSJFWVBAtBNXCxk6wTkBJYUTwEwQLC61E8QP0NzZzt5g5726DkC7EYWHZ8T3WndkV2C/jLwn4hwVYBIdLn9vkLp79QcBCTDMiy3w2gQ9XeTYkEHA8vqj2rworXu3HF1YFfSWgp5QFnKVLvYvzDEKEZ5hW70oXOCtcEbQLIkx7+IQtfMBSOjU6XEF4oyOdYInZbXyOuajjDlpNeQgleIUJKUz4BDMledhqOu/AzVSmzZ49CUjCC0yvim98iqtJT2L2jKsqczsdok9XrHNexaww415lnTNwn6CM/KxJIR8bnUZHPhLO6yMoIyk2pNjLewFuE5AiY1KMMQx8Q7hQYFek4AkjxXFe1rsF84I/BTFQMGL+1Lxwl4DwdtM1gjwKohgxyLtG7SYpxALqugOMcfOKN+bFXeBsLB1uulNcRqq7/tt36k41zoL6QlxGjtd6lrahiqCi1iOFYyvXuxY8yzK33VnvUivbLlOlj/jktm0s3YnXrNIXXufHNxuOGasi8S68zkwrlnV8ZcJJsTIUxbLgQcFZWE8N0gau2p40VVcM0gYeFpSRK6445UhBuKiRgiyKw+34rLt59nb1/7+RwReVkaFtqvNBuwAAAABJRU5ErkJggg==) 2x);
}

#close:active {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAQAAAC1+jfqAAAAQklEQVR4AWP4TwBSTQGDHcMZIIYAKA9VwRkwtINJgyCaCTAlCBaKAoQ+hFmoCqBKENKkK8C0gpAjCXuTyICiQ2QBAPSwyG3ByZlCAAAAAElFTkSuQmCC) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAQAAADZc7J/AAAA/ElEQVR4Xu3UsWrCUBiG4efGlIBoIMFbcnYolYJ3pg4iKGrGYFTRwaUFhYAekiDt0EG++X2W83N8/3J/DbwBMJJSsdQItcDY1VlCOImzq3Ed8OmicHASB3ns5KBw8VUNpDJrW7uAiJ3sbK1l0mqArpmFTUlQ5jYWZrrUAUSmT0SZm4qoA56JvVhs/5g3A7RLolA85A1ASOTye65NMxASK6syfxGITMzvMxG9CvRkliWwlOm9AsSOcitzU1NzK7mjuBkQvHtLK7iLBiB5PhttJSGpB8I8vM6kDuiHeUjoVwMfYR4SRtUAw1veIZzOjRhSBzCoyKFjgH/3K7+BHzg+Cgw0eSW3AAAAAElFTkSuQmCC) 2x);
}
</style>
<style>
#outer {
  border: none;
  cursor: pointer;
}

#shielding {
  background-color: rgba(0, 0, 0, 0.5);
  height: 100%;
  left: 0px;
  position: absolute;
  top: 0px;
  width: 100%;
  z-index: 2;
}

#plugin-icon {
  opacity: 0.8;
  max-height: 100%;
  max-width: 100%;
  min-width: 0;
  min-height: 0;
}

#plugin-icon:hover {
  opacity: 0.95;
}

#poster {
  height: 100%;
  object-fit: contain;
  width: 100%;
  z-index: 1;
}

#inner-container {
  align-items: center;
  display: flex;
  height: 100%;
  justify-content: center;
  left: 0px;
  max-height: 100%;
  max-width: 100%;
  position: absolute;
  top: 0px;
  width: 100%;
  z-index: 2;
}
</style>
<base i18n-values="href:baseurl">
</head>

<body>
  <div i18n-values="title:name" id="outer">
    <img id="poster" i18n-values="srcset:poster">
    <div id="shielding"></div>
    <div id="inner-container"
         i18n-values=".style.width:visibleWidth;.style.height:visibleHeight">
      <img id="plugin-icon" src="data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAADIAAAAyCAYAAAAeP4ixAAAAAXNSR0IArs4c6QAAA8dJREFUaAXdWs9vEkEUZkvZQloNxJoY40kxJsDNo9GDif+Bf4CHpjEmJPoPeMce1F408dYjQePBgycv3vRG4GBMPREPLSlpCUWggN+3ncVl6PJrZ5a6k0x2Z2fnve/befPmzcwaIbVpGeLCyEsiG5L4Pso9kbu4nkj1CysSaAQ5trm5uV6pVB4eHx+/brVan3u93s9+v19Fbolc5TPW8R2+yzZsK2TIpPFYf6LS6Pb29rX9/f0NgPsIsA3kWVODbSmDsigT2TdCZjabvVyv158D9eGsyMe8f0iZlA0yJrK2FE4mkxer1eoTmMjvMYA8VVE2dVAXmHC8KU1mPp9Pdjqd755QztCYuqgTLJT1TrRYLN7T2Qtu/KiTukGGY8dTiu3t7T2CoqabMh+eN4kBLOjd5kpRQcIHrJNVCDIz94zJLoX4RfaEzK4pzGzqMRPmIFvEmJCRy2ViEg5gsjej2/PTO8lgJ5WJTbjmsePFpA+fJGzR9cQIFq4mZnBWPY8mJX84YhQRwJnhTFSEHXK7mcrNZvMdGjBQ1JqIFb0y4sUMBm3Q7Dl2KpVKd5jx1Xa1MgFWEWgO9UqEEagKxSSBL7WSy+WuIrJ9r0Kmmwxihi4uIwYpJkJxtzZTPxdE7IXVaq1We4bGWkyNmMHAmvGpMIQFzqppmg8GtNTccCXYSCQSb8rl8n2Q+aVG7D8pxEzs9pNlrtam/uQTXnT0iC2f14guUyN2yOcSO7TCpecEfFNXuxChHvY+Te0phCkzNWInB8uWw+HwLWrSnGxTe6vS1AT2JYtIJBK5oZmEU/yfTCbzbWtr62673f7grJjnXmC3xvoaupq7HUrSGNOScaoytSoEr1lMcHNB1uJDWZWpWdhtIj7g1qvCcltQUUe+pFfViHR+xBi82EY8Hn+Be9dodqTl8ANit1wix0ZtuE57ifPKFczMOyDxCtrmJRES2PvskR4WK7uYJZPa4Z8qiMIh3E6lUjuGYVz3qpPYyYHd2+t2uz+8CpyiPXVxQnycTqe/qCBBnQK7RaR7cHDwdQogXl5xmtJLCJrblGQQAjt39q2gcR22Ns8mNMfXUDpjHqEp6VqfNMSO/oCfzjBeaWzl/GojYTzonBwdHX0a0PJ+o82UnNAE5qHDosAsdUk0EJsPJBKY7SCSCcQGHYmEgrJlSi7B2MQmE6RAHCucUoEXC8JBj00mEEdvNplAHIbaZMxCoXDTzwMg6lJ9PG2TCcQPAzYZXv/7XzicZALxU41M6Fz85jR02uNEOOc9NzN4dMz1ObMsX9uPZ38BvwZw6z92PI4AAAAASUVORK5CYII=" />
    </div>
  </div>
  <script>
    document.getElementById('poster').onerror = function() {
      this.hidden = true;
    };

    document.getElementById('outer').onclick = function() {
      plugin.load();
    };

    window.resizePoster = function(marginLeft, marginTop, width, height) {
      var container = document.getElementById('inner-container');
      container.style.marginLeft = marginLeft;
      container.style.marginTop = marginTop;
      container.style.width = width;
      container.style.height = height;

      if (plugin.didFinishIconRepositionForTesting) {
        // Defer until reflow complete.
        window.setTimeout(function() {
          plugin.didFinishIconRepositionForTesting();
        });
      }
    };
  </script>
</body>
</html>
{
  "x-version": 29,
  "google-talk": {
    "mime_types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "'Google Talk Plugin' and 'Google Talk Plugin Video Accelerator' use two completely different versioning schemes, so we can't define a minimum version."
      }
    ],
    "name": "Google Talk",
    "group_name_matcher": "*Google Talk*"
  },
  "java-runtime-environment": {
    "mime_types": [
      "application/x-java-applet",
      "application/x-java-applet;jpi-version=1.7.0_05",
      "application/x-java-applet;version=1.1",
      "application/x-java-applet;version=1.1.1",
      "application/x-java-applet;version=1.1.2",
      "application/x-java-applet;version=1.1.3",
      "application/x-java-applet;version=1.2",
      "application/x-java-applet;version=1.2.1",
      "application/x-java-applet;version=1.2.2",
      "application/x-java-applet;version=1.3",
      "application/x-java-applet;version=1.3.1",
      "application/x-java-applet;version=1.4",
      "application/x-java-applet;version=1.4.1",
      "application/x-java-applet;version=1.4.2",
      "application/x-java-applet;version=1.5",
      "application/x-java-applet;version=1.6",
      "application/x-java-applet;version=1.7",
      "application/x-java-bean",
      "application/x-java-bean;jpi-version=1.7.0_05",
      "application/x-java-bean;version=1.1",
      "application/x-java-bean;version=1.1.1",
      "application/x-java-bean;version=1.1.2",
      "application/x-java-bean;version=1.1.3",
      "application/x-java-bean;version=1.2",
      "application/x-java-bean;version=1.2.1",
      "application/x-java-bean;version=1.2.2",
      "application/x-java-bean;version=1.3",
      "application/x-java-bean;version=1.3.1",
      "application/x-java-bean;version=1.4",
      "application/x-java-bean;version=1.4.1",
      "application/x-java-bean;version=1.4.2",
      "application/x-java-bean;version=1.5",
      "application/x-java-bean;version=1.6",
      "application/x-java-bean;version=1.7",
      "application/x-java-vm",
      "application/x-java-vm-npruntime"
    ],
    "versions": [
      {
        "version": "10.45",
        "status": "requires_authorization",
        "comment": "Java SE 7u45"
      }
    ],
    "lang": "en-US",
    "name": "Java(TM)",
    "help_url": "https://support.google.com/chrome/?p=plugin_java",
    "url": "http://java.com/download",
    "displayurl": true,
    "group_name_matcher": "Java*"
  },
  "ibm-java-runtime-environment": {
    "mime_types": [
      "application/x-java-applet",
      "application/x-java-applet;jpi-version=1.7.0_05",
      "application/x-java-applet;version=1.1",
      "application/x-java-applet;version=1.1.1",
      "application/x-java-applet;version=1.1.2",
      "application/x-java-applet;version=1.1.3",
      "application/x-java-applet;version=1.2",
      "application/x-java-applet;version=1.2.1",
      "application/x-java-applet;version=1.2.2",
      "application/x-java-applet;version=1.3",
      "application/x-java-applet;version=1.3.1",
      "application/x-java-applet;version=1.4",
      "application/x-java-applet;version=1.4.1",
      "application/x-java-applet;version=1.4.2",
      "application/x-java-applet;version=1.5",
      "application/x-java-applet;version=1.6",
      "application/x-java-applet;version=1.7",
      "application/x-java-bean",
      "application/x-java-bean;jpi-version=1.7.0_05",
      "application/x-java-bean;version=1.1",
      "application/x-java-bean;version=1.1.1",
      "application/x-java-bean;version=1.1.2",
      "application/x-java-bean;version=1.1.3",
      "application/x-java-bean;version=1.2",
      "application/x-java-bean;version=1.2.1",
      "application/x-java-bean;version=1.2.2",
      "application/x-java-bean;version=1.3",
      "application/x-java-bean;version=1.3.1",
      "application/x-java-bean;version=1.4",
      "application/x-java-bean;version=1.4.1",
      "application/x-java-bean;version=1.4.2",
      "application/x-java-bean;version=1.5",
      "application/x-java-bean;version=1.6",
      "application/x-java-bean;version=1.7",
      "application/x-java-vm",
      "application/x-java-vm-npruntime"
    ],
    "versions": [
    ],
    "name": "IBM Java",
    "group_name_matcher": "*IBM*Java*"
  },
  "realplayer": {
    "mime_types": [
      "audio/vnd.rn-realaudio",
      "video/vnd.rn-realvideo",
      "audio/x-pn-realaudio-plugin",
      "audio/x-pn-realaudio"
    ],
    "versions": [
      {
        "version": "15.0.2.71",
        "status": "requires_authorization",
        "reference": "http://service.real.com/realplayer/security/02062012_player/en/"
      }
    ],
    "lang": "en-US",
    "name": "RealPlayer",
    "help_url": "https://support.google.com/chrome/?p=plugin_real",
    "url": "http://forms.real.com/real/realone/download.html?type=rpsp_us",
    "group_name_matcher": "*RealPlayer*"
  },
  "adobe-flash-player": {
    "mime_types": [
      "application/futuresplash",
      "application/x-shockwave-flash"
    ],
    "versions": [
      {
        "version": "21.0.0.242",
        "status": "requires_authorization",
        "reference": "https://helpx.adobe.com/security/products/flash-player/apsb16-15.html"
      }
    ],
    "lang": "en-US",
    "name": "Adobe Flash Player",
    "help_url": "https://support.google.com/chrome/?p=plugin_flash",
    "url": "https://support.google.com/chrome/answer/6258784",
    "displayurl": true,
    "group_name_matcher": "*Shockwave Flash*"
  },
  "adobe-shockwave": {
    "mime_types": [
      "application/x-director"
    ],
    "versions": [
      {
        "version": "12.1.0.150",
        "status": "requires_authorization",
        "reference": "https://helpx.adobe.com/security/products/shockwave/apsb14-10.html"
      }
    ],
    "lang": "en-US",
    "name": "Adobe Shockwave Player",
    "help_url": "https://support.google.com/chrome/?p=plugin_shockwave",
    "url": "http://fpdownload.macromedia.com/get/shockwave/default/english/win95nt/latest/Shockwave_Installer_Slim.exe",
    "group_name_matcher": "*Shockwave for Director*"
  },
  "adobe-reader": {
    "mime_types": [
      "application/pdf",
      "application/vnd.adobe.x-mars",
      "application/vnd.adobe.xdp+xml",
      "application/vnd.adobe.xfd+xml",
      "application/vnd.adobe.xfdf",
      "application/vnd.fdf"
    ],
    "versions": [
      {
        "version": "10.1.13",
        "status": "requires_authorization",
        "reference": "https://helpx.adobe.com/security/products/reader/apsb14-28.html"
      },
      {
        "version": "11",
        "status": "out_of_date"
      },
      {
        "version": "11.0.10",
        "status": "requires_authorization",
        "reference": "https://helpx.adobe.com/security/products/reader/apsb14-28.html"
      }
    ],
    "lang": "en-US",
    "name": "Adobe Reader",
    "help_url": "https://support.google.com/chrome/?p=plugin_pdf",
    "url": "https://get.adobe.com/reader/",
    "displayurl": true,
    "group_name_matcher": "*Adobe Acrobat*"
  },
  "apple-quicktime": {
    "mime_types": [
      "application/sdp",
      "application/x-mpeg",
      "application/x-rtsp",
      "application/x-sdp",
      "audio/3ggp",
      "audio/3ggp2",
      "audio/aac",
      "audio/ac3",
      "audio/aiff",
      "audio/amr",
      "audio/basic",
      "audio/mid",
      "audio/midi",
      "audio/mp4",
      "audio/mpeg",
      "audio/vnd.qcelp",
      "audio/wav",
      "audio/x-aac",
      "audio/x-ac3",
      "audio/x-aiff",
      "audio/x-caf",
      "audio/x-gsm",
      "audio/x-m4a",
      "audio/x-m4b",
      "audio/x-m4p",
      "audio/x-midi",
      "audio/x-mpeg",
      "audio/x-wav",
      "image/jp2",
      "image/jpeg2000",
      "image/jpeg2000-image",
      "image/pict",
      "image/png",
      "image/x-jpeg2000-image",
      "image/x-macpaint",
      "image/x-pict",
      "image/x-png",
      "image/x-quicktime",
      "image/x-sgi",
      "image/x-targa",
      "video/3ggp",
      "video/3ggp2",
      "video/flc",
      "video/mp4",
      "video/mpeg",
      "video/quicktime",
      "video/sd-video",
      "video/x-m4v",
      "video/x-mpeg"
    ],
    "versions": [
      {
        "version": "7.7.6",
        "status": "requires_authorization",
        "reference": "http://support.apple.com/kb/HT203092"
      }
    ],
    "lang": "en-US",
    "name": "QuickTime Player",
    "help_url": "https://support.google.com/chrome/?p=plugin_quicktime",
    "url": "http://appldnld.apple.com/QuickTime/041-3089.20111026.Sxpr4/QuickTimeInstaller.exe",
    "group_name_matcher": "*QuickTime Plug-in*"
  },
  "windows-media-player": {
    "mime_types": [
    ],
    "lang": "en-US",
    "name": "Windows Media Player",
    "help_url": "https://support.google.com/chrome/?p=plugin_wmp",
    "url": "http://www.interoperabilitybridges.com/wmp-extension-for-chrome",
    "displayurl": true,
    "group_name_matcher": "*Windows Media Player*"
  },
  "divx-player": {
    "mime_types": [
      "video/divx",
      "video/x-matroska"
    ],
    "versions": [
      {
        "version": "1.4.3.4",
        "status": "requires_authorization"
      }
    ],
    "lang": "en-US",
    "name": "DivX Web Player",
    "help_url": "https://support.google.com/chrome/?p=plugin_divx",
    "url": "http://download.divx.com/player/divxdotcom/DivXWebPlayerInstaller.exe",
    "group_name_matcher": "*DivX Web Player*"
  },
  "silverlight": {
    "mime_types": [
      "application/x-silverlight",
      "application/x-silverlight-2"
    ],
    "versions": [
      {
        "version": "5.1.40416.0",
        "status": "requires_authorization",
        "reference": "https://support.microsoft.com/kb/3056819"
      }
    ],
    "lang": "en-US",
    "name": "Silverlight",
    "url": "http://go.microsoft.com/fwlink/?LinkID=149156",
    "group_name_matcher": "*Silverlight*"
  },
  "microsoft-office": {
    "mime_types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "Microsoft Office has no version information."
      }
    ],
    "name": "Microsoft Office",
    "group_name_matcher": "*Microsoft Office*"
  },
  "nvidia-3d": {
    "mime_types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "NVidia 3D has no version information."
      }
    ],
    "name": "NVIDIA 3D",
    "group_name_matcher": "*NVIDIA 3D*"
  },
  "google-chrome-pdf": {
    "mime_types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "fully_trusted",
        "comment": "Google Chrome PDF Viewer has no version information."
      }
    ],
    "name": "Chrome PDF Viewer",
    "group_name_matcher": "*Chrome PDF Viewer*"
  },
  "chromium-pdf": {
    "mime_types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "fully_trusted",
        "comment": "Chromium PDF Viewer has no version information."
      }
    ],
    "name": "Chromium PDF Viewer",
    "group_name_matcher": "*Chromium PDF Viewer*"
  },
  "google-update": {
    "mime-types": [
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "Google Update plugin is versioned but kept automatically up-to-date"
      }
    ],
    "name": "Google Update",
    "group_name_matcher": "Google Update"
  },
  "facebook-video-calling": {
    "mime_types": [
      "application/skypesdk-plugin"
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "We do not track version information for the Facebook Video Calling Plugin."
      }
    ],
    "lang": "en-US",
    "name": "Facebook Video Calling",
    "url": "https://www.facebook.com/chat/video/videocalldownload.php",
    "group_name_matcher": "*Facebook Video*"
  },
  "google-earth": {
    "mime_types": [
      "application/geplugin"
    ],
    "versions": [
      {
        "version": "0",
        "status": "requires_authorization",
        "comment": "We do not track version information for the Google Earth Plugin."
      }
    ],
    "lang": "en-US",
    "name": "Google Earth",
    "url": "http://www.google.com/earth/explore/products/plugin.html",
    "group_name_matcher": "*Google Earth*"
  }
}
<!-- Generated by licenses.py; do not edit. --><!doctype html>
<html>
<head>
<meta charset="utf-8">
<title>Credits</title>
<link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
<style>
body {
  background-color: white;
  font-size: 84%;
  max-width: 1020px;
}
.page-title {
  font-size: 164%;
  font-weight: bold;
}
.product {
  background-color: #c3d9ff;
  border-radius: 5px;
  margin-top: 16px;
  overflow: auto;
  padding: 2px;
}
.product .title {
  float: left;
  font-size: 110%;
  font-weight: bold;
  margin: 3px;
}
.product .homepage {
  float: right;
  margin: 3px;
  text-align: right;
}
.product .homepage::after {
  content: " - ";
}
.product .show {
  float: right;
  margin: 3px;
  text-align: right;
}
.licence {
  background-color: #e8eef7;
  border-radius: 3px;
  clear: both;
  display: none;
  padding: 16px;
}
.licence h3 {
  margin-top: 0;
}
.licence pre {
  white-space: pre-wrap;
}
.dialog #print-link,
.dialog .homepage {
  display: none;
}
</style>
</head>
<body>
<span class="page-title" style="float:left;">Credits</span>
<a id="print-link" href="#" style="float:right;">Print</a>
<div style="clear:both; overflow:auto;"><!-- Chromium <3s the following projects -->
<div class="product">
<span class="title">Accessibility Audit library, from Accessibility Developer Tools</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://raw.githubusercontent.com/GoogleChrome/accessibility-developer-tools/master/dist/js/axs_testing.js">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Almost Native Graphics Layer Engine</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://code.google.com/p/angleproject/">homepage</a></span>
<div class="licence">
<pre>// Copyright (C) 2002-2013 The ANGLE Project Authors. 
// All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//
//     Redistributions of source code must retain the above copyright
//     notice, this list of conditions and the following disclaimer.
//
//     Redistributions in binary form must reproduce the above 
//     copyright notice, this list of conditions and the following
//     disclaimer in the documentation and/or other materials provided
//     with the distribution.
//
//     Neither the name of TransGaming Inc., Google Inc., 3DLabs Inc.
//     Ltd., nor the names of their contributors may be used to endorse
//     or promote products derived from this software without specific
//     prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
// FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
// COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
// INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
// BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
// LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
// ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Android</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://source.android.com">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Android Crazy Linker</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/chromium/src.git/+/master/third_party/android_crazy_linker/">homepage</a></span>
<div class="licence">
<pre>// Copyright 2014 The Chromium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

/*
 * Copyright (C) 2012 The Android Open Source Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
 * AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */
</pre>
</div>
</div>


<div class="product">
<span class="title">Android Explicit Synchronization</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://source.android.com">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Android Open Source Project - App Compat Library</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://android.googlesource.com/platform/frameworks/support">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Android Open Source Project - Settings App</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://android.googlesource.com/platform/packages/apps/Settings/">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Android bionic libc</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://android.googlesource.com/platform/bionic/+/master/libc/">homepage</a></span>
<div class="licence">
<pre>   Copyright (c) 2014, Linaro Limited
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
       * Neither the name of the Linaro nor the
         names of its contributors may be used to endorse or promote products
         derived from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

   strchr - find a character in a string

   Copyright (c) 2014, ARM Limited
   All rights Reserved.
   Copyright (c) 2014, Linaro Ltd.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
       * Neither the name of the company nor the names of its contributors
         may be used to endorse or promote products derived from this
         software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

 Copyright (c) 1993 John Brezak
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:
 1. Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in the
    documentation and/or other materials provided with the distribution.
 3. The name of the author may be used to endorse or promote products
    derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR `AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

====================================================
Copyright (C) 1993 by Sun Microsystems, Inc. All rights reserved.

Developed at SunPro, a Sun Microsystems, Inc. business.
Permission to use, copy, modify, and distribute this
software is freely granted, provided that this notice
is preserved.

-------------------------------------------------------------------

Based on the UCB version with the ID appearing below.
This is ANSIish only when "multibyte character == plain character".

Copyright (c) 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 1995, 1996, 1997, and 1998 WIDE Project.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the project nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE PROJECT AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE PROJECT OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2004, 2005, 2008  Internet Systems Consortium, Inc. ("ISC")
Copyright (C) 1995-1999, 2001, 2003  Internet Software Consortium.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE
OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (C) 2004, 2005, 2008  Internet Systems Consortium, Inc. ("ISC")
Copyright (C) 1997-2001  Internet Software Consortium.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE
OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (C) 2006 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2006 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2008 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2008 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2008 The Android Open Source Project
All rights reserved.
Copyright (c) 2013-2014, NVIDIA Corporation.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2009 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2010 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2010 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2010 The Android Open Source Project
Copyright (c) 2008 ARM Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the company may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY ARM LTD ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL ARM LTD BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Android adaptation and tweak by Jim Huang &lt;jserv@0xlab.org&gt;.

-------------------------------------------------------------------

Copyright (C) 2011 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2012 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2012 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2013 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2013 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2013 The Android Open Source Project
All rights reserved.
Copyright (c) 2013-2014 NVIDIA Corporation.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2013 The Android Open Source Project
Copyright (c) 2014, NVIDIA CORPORATION.  All rights reserved.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2014 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2014 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (C) 2015 The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

-------------------------------------------------------------------

Copyright (C) 2015 The Android Open Source Project
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
 * Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in
   the documentation and/or other materials provided with the
   distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED
AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1980, 1983, 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by the University of
   California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.


Portions Copyright (c) 1993 by Digital Equipment Corporation.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies, and that
the name of Digital Equipment Corporation not be used in advertising or
publicity pertaining to distribution of the document or software without
specific, written prior permission.

THE SOFTWARE IS PROVIDED "AS IS" AND DIGITAL EQUIPMENT CORP. DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS.   IN NO EVENT SHALL DIGITAL EQUIPMENT
CORPORATION BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1982, 1986, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1982, 1986, 1993
   The Regents of the University of California.  All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1983, 1987, 1989
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1983, 1989
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1983, 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1983, 1990, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

Portions Copyright (c) 1993 by Digital Equipment Corporation.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies, and that
the name of Digital Equipment Corporation not be used in advertising or
publicity pertaining to distribution of the document or software without
specific, written prior permission.

THE SOFTWARE IS PROVIDED "AS IS" AND DIGITAL EQUIPMENT CORP. DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS.   IN NO EVENT SHALL DIGITAL EQUIPMENT
CORPORATION BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1983, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1983, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1985
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1985 Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1985, 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

Portions Copyright (c) 1993 by Digital Equipment Corporation.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies, and that
the name of Digital Equipment Corporation not be used in advertising or
publicity pertaining to distribution of the document or software without
specific, written prior permission.

THE SOFTWARE IS PROVIDED "AS IS" AND DIGITAL EQUIPMENT CORP. DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS.   IN NO EVENT SHALL DIGITAL EQUIPMENT
CORPORATION BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1985, 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1985, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1985, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1987 Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1987, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1987, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1988 Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1988 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1988, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software written by Ken Arnold and
published in UNIX Review, Vol. 6, No. 8.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms are permitted
provided that the above copyright notice and this paragraph are
duplicated in all such forms and that any documentation,
advertising materials, and other materials related to such
distribution and use acknowledge that the software was developed
by the University of California, Berkeley. The name of the
University may not be used to endorse or promote products derived
from this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.

-------------------------------------------------------------------

Copyright (c) 1989 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989 The Regents of the University of California.
All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Roger L. Snyder.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989, 1993
   The Regents of the University of California.  All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1989, 1993, 1994
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990 The Regents of the University of California.
All rights reserved.

This code is derived from locore.s.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990 The Regents of the University of California.
All rights reserved.

This code is derived from software contributed to Berkeley by
Chris Torek.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990 The Regents of the University of California.
All rights reserved.

This code is derived from software contributed to Berkeley by
William Jolitz.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Chris Torek.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Donn Seeley at UUNET Technologies, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Donn Seeley at UUNET Technologies, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993
   The Regents of the University of California.  All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993, 1994
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1990, 1993, 1994
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Chris Torek.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1991 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1991, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1991, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Berkeley Software Design, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1991, 1993
   The Regents of the University of California.  All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

This code is derived from software contributed to Berkeley by
Hugh Smith at The University of Guelph.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1991, 1993, 1995,
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Havard Eidnes.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992 Henry Spencer.
Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Henry Spencer of the University of Toronto.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992 The Regents of the University of California.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Ralph Campbell.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Ralph Campbell. This file is derived from the MIPS RISC
Architecture book by Gerry Kane.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.

This software was developed by the Computer Systems Engineering group
at Lawrence Berkeley Laboratory under DARPA contract BG 91-66 and
contributed to Berkeley.

All advertising materials mentioning features or use of this software
must display the following acknowledgement:
   This product includes software developed by the University of
   California, Lawrence Berkeley Laboratory.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by the University of
   California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993
   The Regents of the University of California.  All rights reserved.
(c) UNIX System Laboratories, Inc.
All or some portions of this file are derived from material licensed
to the University of California by American Telephone and Telegraph
Co. or Unix System Laboratories, Inc. and are reproduced herein with
the permission of UNIX System Laboratories, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993, 1994
   The Regents of the University of California.  All rights reserved.

This code is derived from software contributed to Berkeley by
Henry Spencer.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1992, 1993, 1994 Henry Spencer.

This code is derived from software contributed to Berkeley by
Henry Spencer.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by the University of
   California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1993 Martin Birgmeier
All rights reserved.

You may redistribute unmodified or modified versions of this source
code provided that the above copyright notice and this and the
following conditions are retained.

This software is provided ``as is'', and comes with no warranties
of any kind. I shall in no event be liable for anything that happens
to anyone/anything when using this software.

-------------------------------------------------------------------

Copyright (c) 1994 SigmaSoft, Th. Lockert &lt;tholo@sigmasoft.com&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1996 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND INTERNET SOFTWARE CONSORTIUM DISCLAIMS
ALL WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL INTERNET SOFTWARE
CONSORTIUM BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1996, David Mazieres &lt;dm@uun.org&gt;
Copyright (c) 2008, Damien Miller &lt;djm@openbsd.org&gt;
Copyright (c) 2013, Markus Friedl &lt;markus@openbsd.org&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1996-1998, 2008 Theo de Raadt
Copyright (c) 1997, 2008-2009 Todd C. Miller

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1997 Mark Brinicombe
Copyright (c) 2010 Android Open Source Project.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by Mark Brinicombe
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997 Niklas Hallqvist.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1997 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997, 1998 The NetBSD Foundation, Inc.
All rights reserved.

This code was contributed to The NetBSD Foundation by Klaus Klein.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
       This product includes software developed by the NetBSD
       Foundation, Inc. and its contributors.
4. Neither the name of The NetBSD Foundation nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997, 1998, 1999, 2004 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Luke Mewburn.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997, 1998, 1999, 2004 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Luke Mewburn; and by Jason R. Thorpe.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
       This product includes software developed by the NetBSD
       Foundation, Inc. and its contributors.
4. Neither the name of The NetBSD Foundation nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1997, 2005 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1998 Softweyr LLC.  All rights reserved.

strtok_r, from Berkeley strtok
Oct 13, 1998 by Wes Peters &lt;wes@softweyr.com&gt;

Copyright (c) 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notices, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notices, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY SOFTWEYR LLC, THE REGENTS AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL SOFTWEYR LLC, THE
REGENTS, OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1998 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Klaus Klein.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
       This product includes software developed by the NetBSD
       Foundation, Inc. and its contributors.
4. Neither the name of The NetBSD Foundation nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1998 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 1998 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES,
INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL
THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 1999
   David E. O'Brien
Copyright (c) 1988, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2000 Ben Harris.
Copyright (C) 1995, 1996, 1997, and 1998 WIDE Project.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the project nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE PROJECT AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE PROJECT OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2000 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Atsushi Onoe.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
   This product includes software developed by the NetBSD
   Foundation, Inc. and its contributors.
4. Neither the name of The NetBSD Foundation nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2000 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Dieter Baron and Thomas Klausner.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2001 Mike Barcroft &lt;mike@FreeBSD.org&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2001 Wasabi Systems, Inc.
All rights reserved.

Written by Frank van der Linden for Wasabi Systems, Inc.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
     This product includes software developed for the NetBSD Project by
     Wasabi Systems, Inc.
4. The name of Wasabi Systems, Inc. may not be used to endorse
   or promote products derived from this software without specific prior
   written permission.

THIS SOFTWARE IS PROVIDED BY WASABI SYSTEMS, INC. ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL WASABI SYSTEMS, INC
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2001-2002 Opsycon AB  (www.opsycon.se / www.opsycon.com)

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2001-2002 Opsycon AB  (www.opsycon.se / www.opsycon.com)

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of Opsycon AB nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 Daniel Hartmeier
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

   - Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   - Redistributions in binary form must reproduce the above
     copyright notice, this list of conditions and the following
     disclaimer in the documentation and/or other materials provided
     with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 Marc Espie.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE OPENBSD PROJECT AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE OPENBSD
PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Christos Zoulas.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 Tim J. Robbins
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 Tim J. Robbins.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Sponsored in part by the Defense Advanced Research Projects
Agency (DARPA) and Air Force Research Laboratory, Air Force
Materiel Command, USAF, under agreement number F39502-99-1-0512.

-------------------------------------------------------------------

Copyright (c) 2002, 2003 Tim J. Robbins.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002-2004 Tim J. Robbins
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2002-2004 Tim J. Robbins.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2003 Constantin S. Svintsoff &lt;kostik@iclub.nsu.ru&gt;

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The names of the authors may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2003 David Schultz &lt;das@FreeBSD.ORG&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2003 Networks Associates Technology, Inc.
All rights reserved.

Portions of this software were developed for the FreeBSD Project by
Jacques A. Vidrine, Safeport Network Services, and Network
Associates Laboratories, the Security Research Division of Network
Associates, Inc. under DARPA/SPAWAR contract N66001-01-C-8035
("CBOSS"), as part of the DARPA CHATS research program.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2003 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Sponsored in part by the Defense Advanced Research Projects
Agency (DARPA) and Air Force Research Laboratory, Air Force
Materiel Command, USAF, under agreement number F39502-99-1-0512.

-------------------------------------------------------------------

Copyright (c) 2004 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Christos Zoulas.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
       This product includes software developed by the NetBSD
       Foundation, Inc. and its contributors.
4. Neither the name of The NetBSD Foundation nor the names of its
   contributors may be used to endorse or promote products derived
   from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1995,1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1995-1999 by Internet Software Consortium

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1995-1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1996,1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1996-1999 by Internet Software Consortium

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1996-1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1997,1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004 by Internet Systems Consortium, Inc. ("ISC")
Portions Copyright (c) 1996-1999 by Internet Software Consortium.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2004, 2005 David Schultz &lt;das@FreeBSD.ORG&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2005 Tim J. Robbins.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2005 by Internet Systems Consortium, Inc. ("ISC")
Copyright (c) 1995-1999 by Internet Software Consortium

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT
OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2007 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2007-2008  Michael G Schwern

This software originally derived from Paul Sheer's pivotal_gmtime_r.c.

The MIT License:

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2007-2008  Michael G Schwern

This software originally derived from Paul Sheer's pivotal_gmtime_r.c.

The MIT License:

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.

Origin: http://code.google.com/p/y2038
Modified for Bionic by the Android Open Source Project

-------------------------------------------------------------------

Copyright (c) 2008  Android Open Source Project (query id randomization)
Copyright (c) 1985, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2008 Otto Moerbeek &lt;otto@drijf.net&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2008 Todd C. Miller &lt;millert@openbsd.org&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2008, Damien Miller &lt;djm@openbsd.org&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2009 David Schultz &lt;das@FreeBSD.org&gt;
All rights reserved.

Copyright (c) 2011 The FreeBSD Foundation
All rights reserved.
Portions of this software were developed by David Chisnall
under sponsorship from the FreeBSD Foundation.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2009 David Schultz &lt;das@FreeBSD.org&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2009 The NetBSD Foundation, Inc.

This code is derived from software contributed to The NetBSD Foundation
by Roy Marples.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2010 MIPS Technologies, Inc.

All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

     * Redistributions of source code must retain the above copyright
       notice, this list of conditions and the following disclaimer.
     * Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the following disclaimer
       in the documentation and/or other materials provided with
       the distribution.
     * Neither the name of MIPS Technologies Inc. nor the names of its
       contributors may be used to endorse or promote products derived
       from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2010 Todd C. Miller &lt;Todd.Miller@courtesan.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2010, 2011, 2012, 2013 Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2010, Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011 David Chisnall
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011 Ed Schouten &lt;ed@FreeBSD.org&gt;
                   David Chisnall &lt;theraven@FreeBSD.org&gt;
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011 Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011 Martin Pieuchot &lt;mpi@openbsd.org&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2011 Martin Pieuchot &lt;mpi@openbsd.org&gt;
Copyright (c) 2009 Ted Unangst

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2011 The Android Open Source Project
Copyright (c) 2008 ARM Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the company may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY ARM LTD ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL ARM LTD BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011, 2012, 2013 Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011, Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2011, VMware, Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the VMware, Inc. nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL VMWARE, INC. OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2012, Linaro Limited
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
       * Neither the name of the Linaro nor the
         names of its contributors may be used to endorse or promote products
         derived from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2012, Linaro Limited
   All rights reserved.
   Copyright (c) 2014, NVIDIA Corporation.  All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
       * Neither the name of the Linaro nor the
         names of its contributors may be used to endorse or promote products
         derived from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2012-2015
     MIPS Technologies, Inc., California.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the MIPS Technologies, Inc., nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE MIPS TECHNOLOGIES, INC. ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE MIPS TECHNOLOGIES, INC. BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2013
     MIPS Technologies, Inc., California.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the MIPS Technologies, Inc., nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE MIPS TECHNOLOGIES, INC. ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE MIPS TECHNOLOGIES, INC. BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2013 ARM Ltd
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. The name of the company may not be used to endorse or promote
   products derived from this software without specific prior written
   permission.

THIS SOFTWARE IS PROVIDED BY ARM LTD ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL ARM LTD BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED
TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2013 Antoine Jacoutot &lt;ajacoutot@openbsd.org&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Copyright (c) 2013 The NetBSD Foundation, Inc.
All rights reserved.

This code is derived from software contributed to The NetBSD Foundation
by Christos Zoulas.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE NETBSD FOUNDATION, INC. AND CONTRIBUTORS
``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE FOUNDATION OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2014
     Imagination Technologies Limited.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. Neither the name of the MIPS Technologies, Inc., nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY IMAGINATION TECHNOLOGIES LIMITED ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL IMAGINATION TECHNOLOGIES LIMITED BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2014 Theo de Raadt &lt;deraadt@openbsd.org&gt;
Copyright (c) 2014 Bob Beck &lt;beck@obtuse.com&gt;

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

Emulation of getentropy(2) as documented at:
http://www.openbsd.org/cgi-bin/man.cgi/OpenBSD-current/man2/getentropy.2

-------------------------------------------------------------------

Copyright (c) 2014, Intel Corporation
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice,
    * this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above copyright notice,
    * this list of conditions and the following disclaimer in the documentation
    * and/or other materials provided with the distribution.

    * Neither the name of Intel Corporation nor the names of its contributors
    * may be used to endorse or promote products derived from this software
    * without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c) 2014, Linaro Limited
   All rights reserved.

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are met:
       * Redistributions of source code must retain the above copyright
         notice, this list of conditions and the following disclaimer.
       * Redistributions in binary form must reproduce the above copyright
         notice, this list of conditions and the following disclaimer in the
         documentation and/or other materials provided with the distribution.
       * Neither the name of the Linaro nor the
         names of its contributors may be used to endorse or promote products
         derived from this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
   "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
   LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
   A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
   HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
   SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
   LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
   DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
   THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
   (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c)1999 Citrus Project,
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c)1999, 2000, 2001 Citrus Project,
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c)2001 Citrus Project,
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright (c)2003 Citrus Project,
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Copyright 1997 Niels Provos &lt;provos@physnet.uni-hamburg.de&gt;
Copyright 2008 Damien Miller &lt;djm@openbsd.org&gt;
All rights reserved.

Theo de Raadt &lt;deraadt@openbsd.org&gt; came up with the idea of using
such a mathematical system to generate more random (yet non-repeating)
ids to solve the resolver/named problem.  But Niels designed the
actual system based on the constraints.

Later modified by Damien Miller to wrap the LCG output in a 15-bit
permutation generator based on a Luby-Rackoff block cipher. This
ensures the output is non-repeating and preserves the MSB twiddle
trick, but makes it more resistant to LCG prediction.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

Copyright 2008  Android Open Source Project (source port randomization)
Copyright (c) 1985, 1989, 1993
   The Regents of the University of California.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.
3. All advertising materials mentioning features or use of this software
   must display the following acknowledgement:
    This product includes software developed by the University of
    California, Berkeley and its contributors.
4. Neither the name of the University nor the names of its contributors
   may be used to endorse or promote products derived from this software
   without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

Portions Copyright (C) 2004, 2005, 2008, 2009  Internet Systems Consortium, Inc. ("ISC")
Portions Copyright (C) 1996-2003  Internet Software Consortium.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS" AND ISC DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS.  IN NO EVENT SHALL ISC BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE
OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.

-------------------------------------------------------------------

Portions Copyright (c) 1993 by Digital Equipment Corporation.

Permission to use, copy, modify, and distribute this software for any
purpose with or without fee is hereby granted, provided that the above
copyright notice and this permission notice appear in all copies, and that
the name of Digital Equipment Corporation not be used in advertising or
publicity pertaining to distribution of the document or software without
specific, written prior permission.

THE SOFTWARE IS PROVIDED "AS IS" AND DIGITAL EQUIPMENT CORP. DISCLAIMS ALL
WARRANTIES WITH REGARD TO THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS.   IN NO EVENT SHALL DIGITAL EQUIPMENT
CORPORATION BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS
ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS
SOFTWARE.

-------------------------------------------------------------------

Portions Copyright (c) 1995 by International Business Machines, Inc.

International Business Machines, Inc. (hereinafter called IBM) grants
permission under its copyrights to use, copy, modify, and distribute this
Software with or without fee, provided that the above copyright notice and
all paragraphs of this notice appear in all copies, and that the name of IBM
not be used in connection with the marketing of any product incorporating
the Software or modifications thereof, without specific, written prior
permission.

To the extent it has a right to do so, IBM grants an immunity from suit
under its patents, if any, for the use, sale or manufacture of products to
the extent that such products are used for performing Domain Name System
dynamic updates in TCP/IP networks by means of the Software.  No immunity is
granted for any product per se or for any other function of any product.

THE SOFTWARE IS PROVIDED "AS IS", AND IBM DISCLAIMS ALL WARRANTIES,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
PARTICULAR PURPOSE.  IN NO EVENT SHALL IBM BE LIABLE FOR ANY SPECIAL,
DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER ARISING
OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE, EVEN
IF IBM IS APPRISED OF THE POSSIBILITY OF SUCH DAMAGES.

-------------------------------------------------------------------

Portions Copyright(C) 1995, Jason Downs.  All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR(S) ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR(S) BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
SUCH DAMAGE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 1998 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 1998, 1999 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 1998, 2000 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 1998-2000 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 1998-2001 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

The author of this software is David M. Gay.

Copyright (C) 2000 by Lucent Technologies
All Rights Reserved

Permission to use, copy, modify, and distribute this software and
its documentation for any purpose and without fee is hereby
granted, provided that the above copyright notice appear in all
copies and that both that the copyright notice and this
permission notice and warranty disclaimer appear in supporting
documentation, and that the name of Lucent or any of its entities
not be used in advertising or publicity pertaining to
distribution of the software without specific, written prior
permission.

LUCENT DISCLAIMS ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS.
IN NO EVENT SHALL LUCENT OR ANY OF ITS ENTITIES BE LIABLE FOR ANY
SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER
IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION,
ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF
THIS SOFTWARE.

-------------------------------------------------------------------

memchr - find a character in a memory zone

Copyright (c) 2014, ARM Limited
All rights Reserved.
Copyright (c) 2014, Linaro Ltd.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the company nor the names of its contributors
      may be used to endorse or promote products derived from this
      software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

-------------------------------------------------------------------

</pre>
</div>
</div>


<div class="product">
<span class="title">Apple sample code</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://developer.apple.com/">homepage</a></span>
<div class="licence">
<pre>Disclaimer: IMPORTANT:  This Apple software is supplied to you by Apple
Inc. ("Apple") in consideration of your agreement to the following
terms, and your use, installation, modification or redistribution of
this Apple software constitutes acceptance of these terms.  If you do
not agree with these terms, please do not use, install, modify or
redistribute this Apple software.

In consideration of your agreement to abide by the following terms, and
subject to these terms, Apple grants you a personal, non-exclusive
license, under Apple's copyrights in this original Apple software (the
"Apple Software"), to use, reproduce, modify and redistribute the Apple
Software, with or without modifications, in source and/or binary forms;
provided that if you redistribute the Apple Software in its entirety and
without modifications, you must retain this notice and the following
text and disclaimers in all such redistributions of the Apple Software.
Neither the name, trademarks, service marks or logos of Apple Inc. may
be used to endorse or promote products derived from the Apple Software
without specific prior written permission from Apple.  Except as
expressly stated in this notice, no other rights or licenses, express or
implied, are granted by Apple herein, including but not limited to any
patent rights that may be infringed by your derivative works or by other
works in which the Apple Software may be incorporated.

The Apple Software is provided by Apple on an "AS IS" basis.  APPLE
MAKES NO WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION
THE IMPLIED WARRANTIES OF NON-INFRINGEMENT, MERCHANTABILITY AND FITNESS
FOR A PARTICULAR PURPOSE, REGARDING THE APPLE SOFTWARE OR ITS USE AND
OPERATION ALONE OR IN COMBINATION WITH YOUR PRODUCTS.

IN NO EVENT SHALL APPLE BE LIABLE FOR ANY SPECIAL, INDIRECT, INCIDENTAL
OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) ARISING IN ANY WAY OUT OF THE USE, REPRODUCTION,
MODIFICATION AND/OR DISTRIBUTION OF THE APPLE SOFTWARE, HOWEVER CAUSED
AND WHETHER UNDER THEORY OF CONTRACT, TORT (INCLUDING NEGLIGENCE),
STRICT LIABILITY OR OTHERWISE, EVEN IF APPLE HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

Copyright (C) 2009 Apple Inc. All Rights Reserved.</pre>
</div>
</div>


<div class="product">
<span class="title">AsyncTask</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/android_tools.git/+/master/sdk/sources/android-23/android/os/AsyncTask.java">homepage</a></span>
<div class="licence">
<pre>Notice for all the files in this folder.
------------------------------------------------------------


   
   Copyright (c) 2005-2008, The Android Open Source Project

   Licensed under the Apache License, Version 2.0 (the "License"); you may not
   use this file except in compliance with the License.
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
   WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
   License for the specific language governing permissions and limitations under
   the License.

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2011 Google Inc. All Rights Reserved.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

</pre>
</div>
</div>


<div class="product">
<span class="title">BSDiff</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.daemonology.net/bsdiff/">homepage</a></span>
<div class="licence">
<pre>Copyright 2003-2005 Colin Percival
All rights reserved

Redistribution and use in source and binary forms, with or without
modification, are permitted providing that the following conditions 
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Blackmagic DeckLink SDK - Mac</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://sw.blackmagicdesign.com/DeckLink/v10.5.2/Blackmagic_DeckLink_SDK_10.5.2.zip?Key-Pair-Id=APKAJTKA3ZJMJRQITVEA&amp;Signature=YRlsDaU0gNjrPNSoPp1IoTBQDavl09RGMnj1exrwAP+jbrNvSX2EuYTqOn2twguM+pQ8W0cqmIl/IHSVnEXJgQB6Eh57x+ba79t9z3fPsD8lb/a6PtK4qlFeqpLK5UBQ9yl18zxtHIZCnBBIeBNoj0G2CxX2Z4IXHVJmZ1KTbECIGaLyj+tBonW+cgIBQ7yw0dQHxGLJD+xzlCrZOXpGRmhJUBs981yLrnfZ7/LirvrHT+8CyzajzEgl9xBB7TFiZUh2DLXf1BvC4NeH0g/OnYRiR7F0VWh/ZiQM/KjCPbn2MajPo5Og0jVjzxYJbIhZf0HhB6ZN0ZI8aaiMMkmHMg==&amp;Expires=1448991563">homepage</a></span>
<div class="licence">
<pre>Extracted from mac/include/DeckLinkAPI.h:

** Copyright (c) 2014 Blackmagic Design
**
** Permission is hereby granted, free of charge, to any person or organization
** obtaining a copy of the software and accompanying documentation covered by
** this license (the "Software") to use, reproduce, display, distribute,
** execute, and transmit the Software, and to prepare derivative works of the
** Software, and to permit third-parties to whom the Software is furnished to
** do so, all subject to the following:
** 
** The copyright notices in the Software and this entire statement, including
** the above license grant, this restriction and the following disclaimer,
** must be included in all copies of the Software, in whole or in part, and
** all derivative works of the Software, unless such copies or derivative
** works are solely in the form of machine-executable object code generated by
** a source language processor.
** 
** THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
** IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
** FITNESS FOR A PARTICULAR PURPOSE, TITLE AND NON-INFRINGEMENT. IN NO EVENT
** SHALL THE COPYRIGHT HOLDERS OR ANYONE DISTRIBUTING THE SOFTWARE BE LIABLE
** FOR ANY DAMAGES OR OTHER LIABILITY, WHETHER IN CONTRACT, TORT OR OTHERWISE,
** ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
** DEALINGS IN THE SOFTWARE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Braille Translation Library</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/liblouis/liblouis">homepage</a></span>
<div class="licence">
<pre>(Copied from src/liblouis/liblouis.h.in)

/* liblouis Braille Translation and Back-Translation Library

   Based on the Linux screenreader BRLTTY, copyright (C) 1999-2006 by
   The BRLTTY Team

   Copyright (C) 2004, 2005, 2006, 2009 ViewPlus Technologies, Inc.
   www.viewplus.com and JJB Software, Inc. www.jjb-software.com

   liblouis is free software: you can redistribute it and/or modify it
   under the terms of the GNU Lesser General Public License as
   published by the Free Software Foundation, either version 3 of the
   License, or (at your option) any later version.

   liblouis is distributed in the hope that it will be useful, but
   WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
   Lesser General Public License for more details.

   You should have received a copy of the GNU Lesser General Public
   License along with this program. If not, see
   &lt;http://www.gnu.org/licenses/&gt;.

   Maintained by John J. Boyer john.boyer@abilitiessoft.com
   */

</pre>
</div>
</div>


<div class="product">
<span class="title">Breakpad, An open-source multi-platform crash reporting system</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/breakpad/breakpad">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2006, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


COPYRIGHT AND PERMISSION NOTICE

Copyright (c) 1996 - 2011, Daniel Stenberg, &lt;daniel@haxx.se&gt;.

All rights reserved.

Permission to use, copy, modify, and distribute this software for any purpose
with or without fee is hereby granted, provided that the above copyright
notice and this permission notice appear in all copies.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF THIRD PARTY RIGHTS. IN
NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of a copyright holder shall not
be used in advertising or otherwise to promote the sale, use or other dealings
in this Software without prior written authorization of the copyright holder.


Copyright (c) 1999 Apple Computer, Inc. All rights reserved.

@APPLE_LICENSE_HEADER_START@

This file contains Original Code and/or Modifications of Original Code
as defined in and that are subject to the Apple Public Source License
Version 2.0 (the 'License'). You may not use this file except in
compliance with the License. Please obtain a copy of the License at
http://www.opensource.apple.com/apsl/ and read it before using this
file.

The Original Code and all software distributed under the License are
distributed on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER
EXPRESS OR IMPLIED, AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES,
INCLUDING WITHOUT LIMITATION, ANY WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE, QUIET ENJOYMENT OR NON-INFRINGEMENT.
Please see the License for the specific language governing rights and
limitations under the License.

@APPLE_LICENSE_HEADER_END@


Copyright 2007-2008 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy
of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Brotli</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/google/brotli">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2009, 2010, 2013-2015 by the Brotli Authors.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Chrome Custom Tabs - Example and Usage</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/external/github.com/GoogleChrome/custom-tabs-client">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.
</pre>
</div>
</div>


<div class="product">
<span class="title">ChromeVox</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://code.google.com/p/google-axs-chrome/">homepage</a></span>
<div class="licence">
<pre>// Copyright 2013 Google Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Closure compiler</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://github.com/google/closure-compiler">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Cocoa extension code from Camino</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://caminobrowser.org/">homepage</a></span>
<div class="licence">
<pre>/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
</pre>
</div>
</div>


<div class="product">
<span class="title">Compact Language Detection</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://src.chromium.org/viewvc/chrome/trunk/src/third_party/cld/">homepage</a></span>
<div class="licence">
<pre>// Copyright (c) 2010 The Chromium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Compact Language Detection 2</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/CLD2Owners/cld2">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Crashpad</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://crashpad.chromium.org/">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Darwin</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.opensource.apple.com/">homepage</a></span>
<div class="licence">
<pre>APPLE PUBLIC SOURCE LICENSE Version 2.0 -  August 6, 2003

Please read this License carefully before downloading this software.  By
downloading or using this software, you are agreeing to be bound by the terms of
this License.  If you do not or cannot agree to the terms of this License,
please do not download or use the software.

Apple Note:  In January 2007, Apple changed its corporate name from "Apple
Computer, Inc." to "Apple Inc."  This change has been reflected below and
copyright years updated, but no other changes have been made to the APSL 2.0.

1.	General; Definitions.  This License applies to any program or other work
which Apple Inc. ("Apple") makes publicly available and which contains a notice
placed by Apple identifying such program or work as "Original Code" and stating
that it is subject to the terms of this Apple Public Source License version 2.0
("License").  As used in this License:

1.1	 "Applicable Patent Rights" mean:  (a) in the case where Apple is the
grantor of rights, (i) claims of patents that are now or hereafter acquired,
owned by or assigned to Apple and (ii) that cover subject matter contained in
the Original Code, but only to the extent necessary to use, reproduce and/or
distribute the Original Code without infringement; and (b) in the case where You
are the grantor of rights, (i) claims of patents that are now or hereafter
acquired, owned by or assigned to You and (ii) that cover subject matter in Your
Modifications, taken alone or in combination with Original Code.

1.2	"Contributor" means any person or entity that creates or contributes to the
creation of Modifications.

1.3	 "Covered Code" means the Original Code, Modifications, the combination of
Original Code and any Modifications, and/or any respective portions thereof.

1.4	"Externally Deploy" means: (a) to sublicense, distribute or otherwise make
Covered Code available, directly or indirectly, to anyone other than You; and/or
(b) to use Covered Code, alone or as part of a Larger Work, in any way to
provide a service, including but not limited to delivery of content, through
electronic communication with a client other than You.

1.5	"Larger Work" means a work which combines Covered Code or portions thereof
with code not governed by the terms of this License.

1.6	"Modifications" mean any addition to, deletion from, and/or change to, the
substance and/or structure of the Original Code, any previous Modifications, the
combination of Original Code and any previous Modifications, and/or any
respective portions thereof.  When code is released as a series of files, a
Modification is:  (a) any addition to or deletion from the contents of a file
containing Covered Code; and/or (b) any new file or other representation of
computer program statements that contains any part of Covered Code.

1.7	"Original Code" means (a) the Source Code of a program or other work as
originally made available by Apple under this License, including the Source Code
of any updates or upgrades to such programs or works made available by Apple
under this License, and that has been expressly identified by Apple as such in
the header file(s) of such work; and (b) the object code compiled from such
Source Code and originally made available by Apple under this License

1.8	"Source Code" means the human readable form of a program or other work that
is suitable for making modifications to it, including all modules it contains,
plus any associated interface definition files, scripts used to control
compilation and installation of an executable (object code).

1.9	"You" or "Your" means an individual or a legal entity exercising rights
under this License.  For legal entities, "You" or "Your" includes any entity
which controls, is controlled by, or is under common control with, You, where
"control" means (a) the power, direct or indirect, to cause the direction or
management of such entity, whether by contract or otherwise, or (b) ownership of
fifty percent (50%) or more of the outstanding shares or beneficial ownership of
such entity.

2.	Permitted Uses; Conditions &amp; Restrictions.   Subject to the terms and
conditions of this License, Apple hereby grants You, effective on the date You
accept this License and download the Original Code, a world-wide, royalty-free,
non-exclusive license, to the extent of Apple's Applicable Patent Rights and
copyrights covering the Original Code, to do the following:

2.1	Unmodified Code.  You may use, reproduce, display, perform, internally
distribute within Your organization, and Externally Deploy verbatim, unmodified
copies of the Original Code, for commercial or non-commercial purposes, provided
that in each instance:

(a)	You must retain and reproduce in all copies of Original Code the copyright
and other proprietary notices and disclaimers of Apple as they appear in the
Original Code, and keep intact all notices in the Original Code that refer to
this License; and

(b) 	You must include a copy of this License with every copy of Source Code of
Covered Code and documentation You distribute or Externally Deploy, and You may
not offer or impose any terms on such Source Code that alter or restrict this
License or the recipients' rights hereunder, except as permitted under Section
6.

2.2	Modified Code.  You may modify Covered Code and use, reproduce, display,
perform, internally distribute within Your organization, and Externally Deploy
Your Modifications and Covered Code, for commercial or non-commercial purposes,
provided that in each instance You also meet all of these conditions:

(a)	You must satisfy all the conditions of Section 2.1 with respect to the
Source Code of the Covered Code;

(b)	You must duplicate, to the extent it does not already exist, the notice in
Exhibit A in each file of the Source Code of all Your Modifications, and cause
the modified files to carry prominent notices stating that You changed the files
and the date of any change; and

(c)	If You Externally Deploy Your Modifications, You must make Source Code of
all Your Externally Deployed Modifications either available to those to whom You
have Externally Deployed Your Modifications, or publicly available.  Source Code
of Your Externally Deployed Modifications must be released under the terms set
forth in this License, including the license grants set forth in Section 3
below, for as long as you Externally Deploy the Covered Code or twelve (12)
months from the date of initial External Deployment, whichever is longer. You
should preferably distribute the Source Code of Your Externally Deployed
Modifications electronically (e.g. download from a web site).

2.3	Distribution of Executable Versions.  In addition, if You Externally Deploy
Covered Code (Original Code and/or Modifications) in object code, executable
form only, You must include a prominent notice, in the code itself as well as in
related documentation, stating that Source Code of the Covered Code is available
under the terms of this License with information on how and where to obtain such
Source Code.

2.4	Third Party Rights.  You expressly acknowledge and agree that although
Apple and each Contributor grants the licenses to their respective portions of
the Covered Code set forth herein, no assurances are provided by Apple or any
Contributor that the Covered Code does not infringe the patent or other
intellectual property rights of any other entity. Apple and each Contributor
disclaim any liability to You for claims brought by any other entity based on
infringement of intellectual property rights or otherwise. As a condition to
exercising the rights and licenses granted hereunder, You hereby assume sole
responsibility to secure any other intellectual property rights needed, if any.
For example, if a third party patent license is required to allow You to
distribute the Covered Code, it is Your responsibility to acquire that license
before distributing the Covered Code.

3.	Your Grants.  In consideration of, and as a condition to, the licenses
granted to You under this License, You hereby grant to any person or entity
receiving or distributing Covered Code under this License a non-exclusive,
royalty-free, perpetual, irrevocable license, under Your Applicable Patent
Rights and other intellectual property rights (other than patent) owned or
controlled by You, to use, reproduce, display, perform, modify, sublicense,
distribute and Externally Deploy Your Modifications of the same scope and extent
as Apple's licenses under Sections 2.1 and 2.2 above.

4.	Larger Works.  You may create a Larger Work by combining Covered Code with
other code not governed by the terms of this License and distribute the Larger
Work as a single product.  In each such instance, You must make sure the
requirements of this License are fulfilled for the Covered Code or any portion
thereof.

5.	Limitations on Patent License.   Except as expressly stated in Section 2, no
other patent rights, express or implied, are granted by Apple herein. 
Modifications and/or Larger Works may require additional patent licenses from
Apple which Apple may grant in its sole discretion.

6.	Additional Terms.  You may choose to offer, and to charge a fee for,
warranty, support, indemnity or liability obligations and/or other rights
consistent with the scope of the license granted herein ("Additional Terms") to
one or more recipients of Covered Code. However, You may do so only on Your own
behalf and as Your sole responsibility, and not on behalf of Apple or any
Contributor. You must obtain the recipient's agreement that any such Additional
Terms are offered by You alone, and You hereby agree to indemnify, defend and
hold Apple and every Contributor harmless for any liability incurred by or
claims asserted against Apple or such Contributor by reason of any such
Additional Terms.

7.	Versions of the License.  Apple may publish revised and/or new versions of
this License from time to time.  Each version will be given a distinguishing
version number.  Once Original Code has been published under a particular
version of this License, You may continue to use it under the terms of that
version. You may also choose to use such Original Code under the terms of any
subsequent version of this License published by Apple.  No one other than Apple
has the right to modify the terms applicable to Covered Code created under this
License.

8.	NO WARRANTY OR SUPPORT.  The Covered Code may contain in whole or in part
pre-release, untested, or not fully tested works.  The Covered Code may contain
errors that could cause failures or loss of data, and may be incomplete or
contain inaccuracies.  You expressly acknowledge and agree that use of the
Covered Code, or any portion thereof, is at Your sole and entire risk.  THE
COVERED CODE IS PROVIDED "AS IS" AND WITHOUT WARRANTY, UPGRADES OR SUPPORT OF
ANY KIND AND APPLE AND APPLE'S LICENSOR(S) (COLLECTIVELY REFERRED TO AS "APPLE"
FOR THE PURPOSES OF SECTIONS 8 AND 9) AND ALL CONTRIBUTORS EXPRESSLY DISCLAIM
ALL WARRANTIES AND/OR CONDITIONS, EXPRESS OR IMPLIED, INCLUDING, BUT NOT LIMITED
TO, THE IMPLIED WARRANTIES AND/OR CONDITIONS OF MERCHANTABILITY, OF SATISFACTORY
QUALITY, OF FITNESS FOR A PARTICULAR PURPOSE, OF ACCURACY, OF QUIET ENJOYMENT,
AND NONINFRINGEMENT OF THIRD PARTY RIGHTS.  APPLE AND EACH CONTRIBUTOR DOES NOT
WARRANT AGAINST INTERFERENCE WITH YOUR ENJOYMENT OF THE COVERED CODE, THAT THE
FUNCTIONS CONTAINED IN THE COVERED CODE WILL MEET YOUR REQUIREMENTS, THAT THE
OPERATION OF THE COVERED CODE WILL BE UNINTERRUPTED OR ERROR-FREE, OR THAT
DEFECTS IN THE COVERED CODE WILL BE CORRECTED.  NO ORAL OR WRITTEN INFORMATION
OR ADVICE GIVEN BY APPLE, AN APPLE AUTHORIZED REPRESENTATIVE OR ANY CONTRIBUTOR
SHALL CREATE A WARRANTY.  You acknowledge that the Covered Code is not intended
for use in the operation of nuclear facilities, aircraft navigation,
communication systems, or air traffic control machines in which case the failure
of the Covered Code could lead to death, personal injury, or severe physical or
environmental damage.

9.	LIMITATION OF LIABILITY. TO THE EXTENT NOT PROHIBITED BY LAW, IN NO EVENT
SHALL APPLE OR ANY CONTRIBUTOR BE LIABLE FOR ANY INCIDENTAL, SPECIAL, INDIRECT
OR CONSEQUENTIAL DAMAGES ARISING OUT OF OR RELATING TO THIS LICENSE OR YOUR USE
OR INABILITY TO USE THE COVERED CODE, OR ANY PORTION THEREOF, WHETHER UNDER A
THEORY OF CONTRACT, WARRANTY, TORT (INCLUDING NEGLIGENCE), PRODUCTS LIABILITY OR
OTHERWISE, EVEN IF APPLE OR SUCH CONTRIBUTOR HAS BEEN ADVISED OF THE POSSIBILITY
OF SUCH DAMAGES AND NOTWITHSTANDING THE FAILURE OF ESSENTIAL PURPOSE OF ANY
REMEDY. SOME JURISDICTIONS DO NOT ALLOW THE LIMITATION OF LIABILITY OF
INCIDENTAL OR CONSEQUENTIAL DAMAGES, SO THIS LIMITATION MAY NOT APPLY TO YOU. In
no event shall Apple's total liability to You for all damages (other than as may
be required by applicable law) under this License exceed the amount of fifty
dollars ($50.00).

10.	Trademarks.  This License does not grant any rights to use the trademarks
or trade names  "Apple", "Mac", "Mac OS", "QuickTime", "QuickTime Streaming
Server" or any other trademarks, service marks, logos or trade names belonging
to Apple (collectively "Apple Marks") or to any trademark, service mark, logo or
trade name belonging to any Contributor.  You agree not to use any Apple Marks
in or as part of the name of products derived from the Original Code or to
endorse or promote products derived from the Original Code other than as
expressly permitted by and in strict compliance at all times with Apple's third
party trademark usage guidelines which are posted at
http://www.apple.com/legal/guidelinesfor3rdparties.html.

11.	Ownership. Subject to the licenses granted under this License, each
Contributor retains all rights, title and interest in and to any Modifications
made by such Contributor.  Apple retains all rights, title and interest in and
to the Original Code and any Modifications made by or on behalf of Apple ("Apple
Modifications"), and such Apple Modifications will not be automatically subject
to this License.  Apple may, at its sole discretion, choose to license such
Apple Modifications under this License, or on different terms from those
contained in this License or may choose not to license them at all.

12.	Termination.

12.1	Termination.  This License and the rights granted hereunder will
terminate:

(a)	automatically without notice from Apple if You fail to comply with any
term(s) of this License and fail to cure such breach within 30 days of becoming
aware of such breach; (b)	immediately in the event of the circumstances
described in Section 13.5(b); or (c)	automatically without notice from Apple if
You, at any time during the term of this License, commence an action for patent
infringement against Apple; provided that Apple did not first commence an action
for patent infringement against You in that instance.

12.2	Effect of Termination.  Upon termination, You agree to immediately stop
any further use, reproduction, modification, sublicensing and distribution of
the Covered Code.  All sublicenses to the Covered Code which have been properly
granted prior to termination shall survive any termination of this License. 
Provisions which, by their nature, should remain in effect beyond the
termination of this License shall survive, including but not limited to Sections
3, 5, 8, 9, 10, 11, 12.2 and 13.  No party will be liable to any other for
compensation, indemnity or damages of any sort solely as a result of terminating
this License in accordance with its terms, and termination of this License will
be without prejudice to any other right or remedy of any party.

13. 	Miscellaneous.

13.1	Government End Users.   The Covered Code is a "commercial item" as defined
in FAR 2.101.  Government software and technical data rights in the Covered Code
include only those rights customarily provided to the public as defined in this
License. This customary commercial license in technical data and software is
provided in accordance with FAR 12.211 (Technical Data) and 12.212 (Computer
Software) and, for Department of Defense purchases, DFAR 252.227-7015 (Technical
Data -- Commercial Items) and 227.7202-3 (Rights in Commercial Computer Software
or Computer Software Documentation).  Accordingly, all U.S. Government End Users
acquire Covered Code with only those rights set forth herein.

13.2	Relationship of Parties.  This License will not be construed as creating
an agency, partnership, joint venture or any other form of legal association
between or among You, Apple or any Contributor, and You will not represent to
the contrary, whether expressly, by implication, appearance or otherwise.

13.3	Independent Development.   Nothing in this License will impair Apple's
right to acquire, license, develop, have others develop for it, market and/or
distribute technology or products that perform the same or similar functions as,
or otherwise compete with, Modifications, Larger Works, technology or products
that You may develop, produce, market or distribute.

13.4	Waiver; Construction.  Failure by Apple or any Contributor to enforce any
provision of this License will not be deemed a waiver of future enforcement of
that or any other provision.  Any law or regulation which provides that the
language of a contract shall be construed against the drafter will not apply to
this License.

13.5	Severability.  (a) If for any reason a court of competent jurisdiction
finds any provision of this License, or portion thereof, to be unenforceable,
that provision of the License will be enforced to the maximum extent permissible
so as to effect the economic benefits and intent of the parties, and the
remainder of this License will continue in full force and effect.  (b)
Notwithstanding the foregoing, if applicable law prohibits or restricts You from
fully and/or specifically complying with Sections 2 and/or 3 or prevents the
enforceability of either of those Sections, this License will immediately
terminate and You must immediately discontinue any use of the Covered Code and
destroy all copies of it that are in your possession or control.

13.6	Dispute Resolution.  Any litigation or other dispute resolution between
You and Apple relating to this License shall take place in the Northern District
of California, and You and Apple hereby consent to the personal jurisdiction of,
and venue in, the state and federal courts within that District with respect to
this License. The application of the United Nations Convention on Contracts for
the International Sale of Goods is expressly excluded.

13.7	Entire Agreement; Governing Law.  This License constitutes the entire
agreement between the parties with respect to the subject matter hereof.  This
License shall be governed by the laws of the United States and the State of
California, except that body of California law concerning conflicts of law.

Where You are located in the province of Quebec, Canada, the following clause
applies:  The parties hereby confirm that they have requested that this License
and all related documents be drafted in English.  Les parties ont exigé que le
présent contrat et tous les documents connexes soient rédigés en anglais.

EXHIBIT A.

"Portions Copyright (c) 1999-2007 Apple Inc.  All Rights Reserved.

This file contains Original Code and/or Modifications of Original Code as
defined in and that are subject to the Apple Public Source License Version 2.0
(the 'License').  You may not use this file except in compliance with the
License.  Please obtain a copy of the License at
http://www.opensource.apple.com/apsl/ and read it before using this file.

The Original Code and all software distributed under the License are distributed
on an 'AS IS' basis, WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESS OR IMPLIED,
AND APPLE HEREBY DISCLAIMS ALL SUCH WARRANTIES, INCLUDING WITHOUT LIMITATION,
ANY WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, QUIET
ENJOYMENT OR NON-INFRINGEMENT.  Please see the License for the specific language
governing rights and limitations under the License." 
</pre>
</div>
</div>


<div class="product">
<span class="title">David M. Gay's floating point routines</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.netlib.org/fp/">homepage</a></span>
<div class="licence">
<pre>/****************************************************************
 *
 * The author of this software is David M. Gay.
 *
 * Copyright (c) 1991, 2000, 2001 by Lucent Technologies.
 *
 * Permission to use, copy, modify, and distribute this software for any
 * purpose without fee is hereby granted, provided that this entire notice
 * is included in all copies of any software which is or includes a copy
 * or modification of this software and in all copies of the supporting
 * documentation for such software.
 *
 * THIS SOFTWARE IS BEING PROVIDED "AS IS", WITHOUT ANY EXPRESS OR IMPLIED
 * WARRANTY.  IN PARTICULAR, NEITHER THE AUTHOR NOR LUCENT MAKES ANY
 * REPRESENTATION OR WARRANTY OF ANY KIND CONCERNING THE MERCHANTABILITY
 * OF THIS SOFTWARE OR ITS FITNESS FOR ANY PARTICULAR PURPOSE.
 *
 ***************************************************************/
</pre>
</div>
</div>


<div class="product">
<span class="title">Error Prone</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://errorprone.info/">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Expat XML Parser</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://sourceforge.net/projects/expat/">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 1998, 1999, 2000 Thai Open Source Software Center Ltd
                               and Clark Cooper
Copyright (c) 2001, 2002, 2003, 2004, 2005, 2006 Expat maintainers.

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
"Software"), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Flot Javascript/JQuery library for creating graphs</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.flotcharts.org">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2007-2013 IOLA and Ole Laursen

Permission is hereby granted, free of charge, to any person
obtaining a copy of this software and associated documentation
files (the "Software"), to deal in the Software without
restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the
Software is furnished to do so, subject to the following
conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.
</pre>
</div>
</div>


<div class="product">
<span class="title">GifPlayer Animated GIF Library</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://android-gifview.googlecode.com/svn/!svn/bc/8/trunk/">homepage</a></span>
<div class="licence">
<pre>                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Google Cache Invalidation API</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/chromium/src/+/master/third_party/cacheinvalidation/README.chromium">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Google Cardboard</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/googlesamples/cardboard-java/">homepage</a></span>
<div class="licence">
<pre>   Copyright (c) 2014, Google Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.


                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS


</pre>
</div>
</div>


<div class="product">
<span class="title">Google Input Tools</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/googlei18n/google-input-tools.git">homepage</a></span>
<div class="licence">
<pre>                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright 2013 Google Inc.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.</pre>
</div>
</div>


<div class="product">
<span class="title">Google Toolbox for Mac</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/google/google-toolbox-for-mac">homepage</a></span>
<div class="licence">
<pre>See src/COPYING
</pre>
</div>
</div>


<div class="product">
<span class="title">Hardware Composer Plus</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://chromium.googlesource.com/chromium/src/third_party/hwcplus/">homepage</a></span>
<div class="licence">
<pre>// Copyright 2014 The Chromium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Headless Android Heap Analyzer</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/square/haha">homepage</a></span>
<div class="licence">
<pre>perflib, guava:

                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

================================================================================
trove4j:

This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation; either version 2.1 of the License, or (at your option) any
later version. This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
for more details. You should have received a copy of the GNU Lesser General
Public License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA


Two classes (HashFunctions and PrimeFinder) included in Trove are licensed
under the following terms:

Copyright (c) 1999 CERN - European Organization for Nuclear Research.
Permission to use, copy, modify, distribute and sell this software and its
documentation for any purpose is hereby granted without fee, provided that the
above copyright notice appear in all copies and that both that copyright notice
and this permission notice appear in supporting documentation. CERN makes no
representations about the suitability of this software for any purpose. It is
provided "as is" without expressed or implied warranty.

</pre>
</div>
</div>


<div class="product">
<span class="title">IAccessible2 COM interfaces for accessibility</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.linuxfoundation.org/collaborate/workgroups/accessibility/iaccessible2">homepage</a></span>
<div class="licence">
<pre>/*************************************************************************
 *
 *  IAccessible2 IDL Specification 
 * 
 *  Copyright (c) 2007, 2010 Linux Foundation 
 *  Copyright (c) 2006 IBM Corporation 
 *  Copyright (c) 2000, 2006 Sun Microsystems, Inc. 
 *  All rights reserved. 
 *   
 *   
 *  Redistribution and use in source and binary forms, with or without 
 *  modification, are permitted provided that the following conditions 
 *  are met: 
 *   
 *   1. Redistributions of source code must retain the above copyright 
 *      notice, this list of conditions and the following disclaimer. 
 *   
 *   2. Redistributions in binary form must reproduce the above 
 *      copyright notice, this list of conditions and the following 
 *      disclaimer in the documentation and/or other materials 
 *      provided with the distribution. 
 *
 *   3. Neither the name of the Linux Foundation nor the names of its 
 *      contributors may be used to endorse or promote products 
 *      derived from this software without specific prior written 
 *      permission. 
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND 
 *  CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, 
 *  INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF 
 *  MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR 
 *  CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, 
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT 
 *  NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
 *  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR 
 *  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, 
 *  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 *   
 *  This BSD License conforms to the Open Source Initiative "Simplified 
 *  BSD License" as published at: 
 *  http://www.opensource.org/licenses/bsd-license.php 
 *   
 *  IAccessible2 is a trademark of the Linux Foundation. The IAccessible2 
 *  mark may be used in accordance with the Linux Foundation Trademark 
 *  Policy to indicate compliance with the IAccessible2 specification. 
 * 
 ************************************************************************/ 
</pre>
</div>
</div>


<div class="product">
<span class="title">ISimpleDOM COM interfaces for accessibility</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://developer.mozilla.org/en-US/docs/Accessibility/AT-APIs">homepage</a></span>
<div class="licence">
<pre>/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is mozilla.org code.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2002
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
</pre>
</div>
</div>


<div class="product">
<span class="title">International Phone Number Library</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/googlei18n/libphonenumber/">homepage</a></span>
<div class="licence">
<pre>                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
</pre>
</div>
</div>


<div class="product">
<span class="title">JMake</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/pantsbuild/jmake">homepage</a></span>
<div class="licence">
<pre>		    GNU GENERAL PUBLIC LICENSE
		       Version 2, June 1991

 Copyright (C) 1989, 1991 Free Software Foundation, Inc.
 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

			    Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
License is intended to guarantee your freedom to share and change free
software--to make sure the software is free for all its users.  This
General Public License applies to most of the Free Software
Foundation's software and to any other program whose authors commit to
using it.  (Some other Free Software Foundation software is covered by
the GNU Library General Public License instead.)  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
this service if you wish), that you receive source code or can get it
if you want it, that you can change the software or use pieces of it
in new free programs; and that you know you can do these things.

  To protect your rights, we need to make restrictions that forbid
anyone to deny you these rights or to ask you to surrender the rights.
These restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must give the recipients all the rights that
you have.  You must make sure that they, too, receive or can get the
source code.  And you must show them these terms so they know their
rights.

  We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

  Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software.  If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors' reputations.

  Finally, any free program is threatened constantly by software
patents.  We wish to avoid the danger that redistributors of a free
program will individually obtain patent licenses, in effect making the
program proprietary.  To prevent this, we have made it clear that any
patent must be licensed for everyone's free use or not licensed at all.

  The precise terms and conditions for copying, distribution and
modification follow.

		    GNU GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License applies to any program or other work which contains
a notice placed by the copyright holder saying it may be distributed
under the terms of this General Public License.  The "Program", below,
refers to any such program or work, and a "work based on the Program"
means either the Program or any derivative work under copyright law:
that is to say, a work containing the Program or a portion of it,
either verbatim or with modifications and/or translated into another
language.  (Hereinafter, translation is included without limitation in
the term "modification".)  Each licensee is addressed as "you".

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running the Program is not restricted, and the output from the Program
is covered only if its contents constitute a work based on the
Program (independent of having been made by running the Program).
Whether that is true depends on what the Program does.

  1. You may copy and distribute verbatim copies of the Program's
source code as you receive it, in any medium, provided that you
conspicuously and appropriately publish on each copy an appropriate
copyright notice and disclaimer of warranty; keep intact all the
notices that refer to this License and to the absence of any warranty;
and give any other recipients of the Program a copy of this License
along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.

  2. You may modify your copy or copies of the Program or any portion
of it, thus forming a work based on the Program, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a
    notice that there is no warranty (or else, saying that you provide
    a warranty) and that users may redistribute the program under
    these conditions, and telling the user how to view a copy of this
    License.  (Exception: if the Program itself is interactive but
    does not normally print such an announcement, your work based on
    the Program is not required to print an announcement.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Program,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Program, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.

  3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections
    1 and 2 above on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your
    cost of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer
    to distribute corresponding source code.  (This alternative is
    allowed only for noncommercial distribution and only if you
    received the program in object code or executable form with such
    an offer, in accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it.  For an executable work, complete source
code means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to
control compilation and installation of the executable.  However, as a
special exception, the source code distributed need not include
anything that is normally distributed (in either source or binary
form) with the major components (compiler, kernel, and so on) of the
operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering
access to copy from a designated place, then offering equivalent
access to copy the source code from the same place counts as
distribution of the source code, even though third parties are not
compelled to copy the source along with the object code.

  4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License.  Any attempt
otherwise to copy, modify, sublicense or distribute the Program is
void, and will automatically terminate your rights under this License.
However, parties who have received copies, or rights, from you under
this License will not have their licenses terminated so long as such
parties remain in full compliance.

  5. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Program or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Program or works based on it.

  6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions.  You may not impose any further
restrictions on the recipients' exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties to
this License.

  7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Program at all.  For example, if a patent
license would not permit royalty-free redistribution of the Program by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.

  8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License
may add an explicit geographical distribution limitation excluding
those countries, so that distribution is permitted only in or among
countries not thus excluded.  In such case, this License incorporates
the limitation as if written in the body of this License.

  9. The Free Software Foundation may publish revised and/or new versions
of the General Public License from time to time.  Such new versions will
be similar in spirit to the present version, but may differ in detail to
address new problems or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and "any
later version", you have the option of following the terms and conditions
either of that version or of any later version published by the Free
Software Foundation.  If the Program does not specify a version number of
this License, you may choose any version ever published by the Free Software
Foundation.

  10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the author
to ask for permission.  For software which is copyrighted by the Free
Software Foundation, write to the Free Software Foundation; we sometimes
make exceptions for this.  Our decision will be guided by the two goals
of preserving the free status of all derivatives of our free software and
of promoting the sharing and reuse of software generally.

			    NO WARRANTY

  11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY
FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED
OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
REPAIR OR CORRECTION.

  12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING
OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED
TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY
YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER
PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGES.

		     END OF TERMS AND CONDITIONS

	    How to Apply These Terms to Your New Programs

  If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

  To do so, attach the following notices to the program.  It is safest
to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least
the "copyright" line and a pointer to where the full notice is found.

    &lt;one line to give the program's name and a brief idea of what it does.&gt;
    Copyright (C) &lt;year&gt;  &lt;name of author&gt;

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA


Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the appropriate
parts of the General Public License.  Of course, the commands you use may
be called something other than `show w' and `show c'; they could even be
mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the program, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the program
  `Gnomovision' (which makes passes at compilers) written by James Hacker.

  &lt;signature of Ty Coon&gt;, 1 April 1989
  Ty Coon, President of Vice

This General Public License does not permit incorporating your program into
proprietary programs.  If your program is a subroutine library, you may
consider it more useful to permit linking proprietary applications with the
library.  If this is what you want to do, use the GNU Lesser General
Public License instead of this License.

</pre>
</div>
</div>


<div class="product">
<span class="title">Khronos header files</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.khronos.org/registry">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2007-2010 The Khronos Group Inc.

Permission is hereby granted, free of charge, to any person obtaining a
copy of this software and/or associated documentation files (the
"Materials"), to deal in the Materials without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Materials, and to
permit persons to whom the Materials are furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be included
in all copies or substantial portions of the Materials.

THE MATERIALS ARE PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
MATERIALS OR THE USE OR OTHER DEALINGS IN THE MATERIALS.


SGI FREE SOFTWARE LICENSE B (Version 2.0, Sept. 18, 2008)

Copyright (C) 1992 Silicon Graphics, Inc. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice including the dates of first publication and either
this permission notice or a reference to http://oss.sgi.com/projects/FreeB/
shall be included in all copies or substantial portions of the Software. 

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL SILICON
GRAPHICS, INC. BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN
AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Except as contained in this notice, the name of Silicon Graphics, Inc. shall
not be used in advertising or otherwise to promote the sale, use or other
dealings in this Software without prior written authorization from Silicon
Graphics, Inc.
</pre>
</div>
</div>


<div class="product">
<span class="title">LCOV - the LTP GCOV extension</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://ltp.sourceforge.net/coverage/lcov.php">homepage</a></span>
<div class="licence">
<pre>		    GNU GENERAL PUBLIC LICENSE
		       Version 2, June 1991

 Copyright (C) 1989, 1991 Free Software Foundation, Inc.,
 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

			    Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
License is intended to guarantee your freedom to share and change free
software--to make sure the software is free for all its users.  This
General Public License applies to most of the Free Software
Foundation's software and to any other program whose authors commit to
using it.  (Some other Free Software Foundation software is covered by
the GNU Lesser General Public License instead.)  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
this service if you wish), that you receive source code or can get it
if you want it, that you can change the software or use pieces of it
in new free programs; and that you know you can do these things.

  To protect your rights, we need to make restrictions that forbid
anyone to deny you these rights or to ask you to surrender the rights.
These restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must give the recipients all the rights that
you have.  You must make sure that they, too, receive or can get the
source code.  And you must show them these terms so they know their
rights.

  We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

  Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software.  If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors' reputations.

  Finally, any free program is threatened constantly by software
patents.  We wish to avoid the danger that redistributors of a free
program will individually obtain patent licenses, in effect making the
program proprietary.  To prevent this, we have made it clear that any
patent must be licensed for everyone's free use or not licensed at all.

  The precise terms and conditions for copying, distribution and
modification follow.

		    GNU GENERAL PUBLIC LICENSE
   TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

  0. This License applies to any program or other work which contains
a notice placed by the copyright holder saying it may be distributed
under the terms of this General Public License.  The "Program", below,
refers to any such program or work, and a "work based on the Program"
means either the Program or any derivative work under copyright law:
that is to say, a work containing the Program or a portion of it,
either verbatim or with modifications and/or translated into another
language.  (Hereinafter, translation is included without limitation in
the term "modification".)  Each licensee is addressed as "you".

Activities other than copying, distribution and modification are not
covered by this License; they are outside its scope.  The act of
running the Program is not restricted, and the output from the Program
is covered only if its contents constitute a work based on the
Program (independent of having been made by running the Program).
Whether that is true depends on what the Program does.

  1. You may copy and distribute verbatim copies of the Program's
source code as you receive it, in any medium, provided that you
conspicuously and appropriately publish on each copy an appropriate
copyright notice and disclaimer of warranty; keep intact all the
notices that refer to this License and to the absence of any warranty;
and give any other recipients of the Program a copy of this License
along with the Program.

You may charge a fee for the physical act of transferring a copy, and
you may at your option offer warranty protection in exchange for a fee.

  2. You may modify your copy or copies of the Program or any portion
of it, thus forming a work based on the Program, and copy and
distribute such modifications or work under the terms of Section 1
above, provided that you also meet all of these conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a
    notice that there is no warranty (or else, saying that you provide
    a warranty) and that users may redistribute the program under
    these conditions, and telling the user how to view a copy of this
    License.  (Exception: if the Program itself is interactive but
    does not normally print such an announcement, your work based on
    the Program is not required to print an announcement.)

These requirements apply to the modified work as a whole.  If
identifiable sections of that work are not derived from the Program,
and can be reasonably considered independent and separate works in
themselves, then this License, and its terms, do not apply to those
sections when you distribute them as separate works.  But when you
distribute the same sections as part of a whole which is a work based
on the Program, the distribution of the whole must be on the terms of
this License, whose permissions for other licensees extend to the
entire whole, and thus to each and every part regardless of who wrote it.

Thus, it is not the intent of this section to claim rights or contest
your rights to work written entirely by you; rather, the intent is to
exercise the right to control the distribution of derivative or
collective works based on the Program.

In addition, mere aggregation of another work not based on the Program
with the Program (or with a work based on the Program) on a volume of
a storage or distribution medium does not bring the other work under
the scope of this License.

  3. You may copy and distribute the Program (or a work based on it,
under Section 2) in object code or executable form under the terms of
Sections 1 and 2 above provided that you also do one of the following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections
    1 and 2 above on a medium customarily used for software interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your
    cost of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer
    to distribute corresponding source code.  (This alternative is
    allowed only for noncommercial distribution and only if you
    received the program in object code or executable form with such
    an offer, in accord with Subsection b above.)

The source code for a work means the preferred form of the work for
making modifications to it.  For an executable work, complete source
code means all the source code for all modules it contains, plus any
associated interface definition files, plus the scripts used to
control compilation and installation of the executable.  However, as a
special exception, the source code distributed need not include
anything that is normally distributed (in either source or binary
form) with the major components (compiler, kernel, and so on) of the
operating system on which the executable runs, unless that component
itself accompanies the executable.

If distribution of executable or object code is made by offering
access to copy from a designated place, then offering equivalent
access to copy the source code from the same place counts as
distribution of the source code, even though third parties are not
compelled to copy the source along with the object code.

  4. You may not copy, modify, sublicense, or distribute the Program
except as expressly provided under this License.  Any attempt
otherwise to copy, modify, sublicense or distribute the Program is
void, and will automatically terminate your rights under this License.
However, parties who have received copies, or rights, from you under
this License will not have their licenses terminated so long as such
parties remain in full compliance.

  5. You are not required to accept this License, since you have not
signed it.  However, nothing else grants you permission to modify or
distribute the Program or its derivative works.  These actions are
prohibited by law if you do not accept this License.  Therefore, by
modifying or distributing the Program (or any work based on the
Program), you indicate your acceptance of this License to do so, and
all its terms and conditions for copying, distributing or modifying
the Program or works based on it.

  6. Each time you redistribute the Program (or any work based on the
Program), the recipient automatically receives a license from the
original licensor to copy, distribute or modify the Program subject to
these terms and conditions.  You may not impose any further
restrictions on the recipients' exercise of the rights granted herein.
You are not responsible for enforcing compliance by third parties to
this License.

  7. If, as a consequence of a court judgment or allegation of patent
infringement or for any other reason (not limited to patent issues),
conditions are imposed on you (whether by court order, agreement or
otherwise) that contradict the conditions of this License, they do not
excuse you from the conditions of this License.  If you cannot
distribute so as to satisfy simultaneously your obligations under this
License and any other pertinent obligations, then as a consequence you
may not distribute the Program at all.  For example, if a patent
license would not permit royalty-free redistribution of the Program by
all those who receive copies directly or indirectly through you, then
the only way you could satisfy both it and this License would be to
refrain entirely from distribution of the Program.

If any portion of this section is held invalid or unenforceable under
any particular circumstance, the balance of the section is intended to
apply and the section as a whole is intended to apply in other
circumstances.

It is not the purpose of this section to induce you to infringe any
patents or other property right claims or to contest validity of any
such claims; this section has the sole purpose of protecting the
integrity of the free software distribution system, which is
implemented by public license practices.  Many people have made
generous contributions to the wide range of software distributed
through that system in reliance on consistent application of that
system; it is up to the author/donor to decide if he or she is willing
to distribute software through any other system and a licensee cannot
impose that choice.

This section is intended to make thoroughly clear what is believed to
be a consequence of the rest of this License.

  8. If the distribution and/or use of the Program is restricted in
certain countries either by patents or by copyrighted interfaces, the
original copyright holder who places the Program under this License
may add an explicit geographical distribution limitation excluding
those countries, so that distribution is permitted only in or among
countries not thus excluded.  In such case, this License incorporates
the limitation as if written in the body of this License.

  9. The Free Software Foundation may publish revised and/or new versions
of the General Public License from time to time.  Such new versions will
be similar in spirit to the present version, but may differ in detail to
address new problems or concerns.

Each version is given a distinguishing version number.  If the Program
specifies a version number of this License which applies to it and "any
later version", you have the option of following the terms and conditions
either of that version or of any later version published by the Free
Software Foundation.  If the Program does not specify a version number of
this License, you may choose any version ever published by the Free Software
Foundation.

  10. If you wish to incorporate parts of the Program into other free
programs whose distribution conditions are different, write to the author
to ask for permission.  For software which is copyrighted by the Free
Software Foundation, write to the Free Software Foundation; we sometimes
make exceptions for this.  Our decision will be guided by the two goals
of preserving the free status of all derivatives of our free software and
of promoting the sharing and reuse of software generally.

			    NO WARRANTY

  11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO WARRANTY
FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE LAW.  EXCEPT WHEN
OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS AND/OR OTHER PARTIES
PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED
OR IMPLIED, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.  THE ENTIRE RISK AS
TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.  SHOULD THE
PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING,
REPAIR OR CORRECTION.

  12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN WRITING
WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY MODIFY AND/OR
REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU FOR DAMAGES,
INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING
OUT OF THE USE OR INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED
TO LOSS OF DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY
YOU OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER
PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF THE
POSSIBILITY OF SUCH DAMAGES.

		     END OF TERMS AND CONDITIONS

	    How to Apply These Terms to Your New Programs

  If you develop a new program, and you want it to be of the greatest
possible use to the public, the best way to achieve this is to make it
free software which everyone can redistribute and change under these terms.

  To do so, attach the following notices to the program.  It is safest
to attach them to the start of each source file to most effectively
convey the exclusion of warranty; and each file should have at least
the "copyright" line and a pointer to where the full notice is found.

    &lt;one line to give the program's name and a brief idea of what it does.&gt;
    Copyright (C) &lt;year&gt;  &lt;name of author&gt;

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

Also add information on how to contact you by electronic and paper mail.

If the program is interactive, make it output a short notice like this
when it starts in an interactive mode:

    Gnomovision version 69, Copyright (C) year name of author
    Gnomovision comes with ABSOLUTELY NO WARRANTY; for details type `show w'.
    This is free software, and you are welcome to redistribute it
    under certain conditions; type `show c' for details.

The hypothetical commands `show w' and `show c' should show the appropriate
parts of the General Public License.  Of course, the commands you use may
be called something other than `show w' and `show c'; they could even be
mouse-clicks or menu items--whatever suits your program.

You should also get your employer (if you work as a programmer) or your
school, if any, to sign a "copyright disclaimer" for the program, if
necessary.  Here is a sample; alter the names:

  Yoyodyne, Inc., hereby disclaims all copyright interest in the program
  `Gnomovision' (which makes passes at compilers) written by James Hacker.

  &lt;signature of Ty Coon&gt;, 1 April 1989
  Ty Coon, President of Vice

This General Public License does not permit incorporating your program into
proprietary programs.  If your program is a subroutine library, you may
consider it more useful to permit linking proprietary applications with the
library.  If this is what you want to do, use the GNU Lesser General
Public License instead of this License.
</pre>
</div>
</div>


<div class="product">
<span class="title">LZMA SDK</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.7-zip.org/sdk.html">homepage</a></span>
<div class="licence">
<pre>LZMA SDK is placed in the public domain.
</pre>
</div>
</div>


<div class="product">
<span class="title">LeakCanary</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/square/leakcanary">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.</pre>
</div>
</div>


<div class="product">
<span class="title">LevelDB: A Fast Persistent Key-Value Store</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/google/leveldb.git">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2011 The LevelDB Authors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

   * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
   * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">MediaController android sample.</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://android.googlesource.com/platform/development/+/master/samples/Support4Demos/src/com/example/android/supportv4/media/MediaController.java">homepage</a></span>
<div class="licence">
<pre>
                                 Apache License
                           Version 2.0, January 2004
                        http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

      "License" shall mean the terms and conditions for use, reproduction,
      and distribution as defined by Sections 1 through 9 of this document.

      "Licensor" shall mean the copyright owner or entity authorized by
      the copyright owner that is granting the License.

      "Legal Entity" shall mean the union of the acting entity and all
      other entities that control, are controlled by, or are under common
      control with that entity. For the purposes of this definition,
      "control" means (i) the power, direct or indirect, to cause the
      direction or management of such entity, whether by contract or
      otherwise, or (ii) ownership of fifty percent (50%) or more of the
      outstanding shares, or (iii) beneficial ownership of such entity.

      "You" (or "Your") shall mean an individual or Legal Entity
      exercising permissions granted by this License.

      "Source" form shall mean the preferred form for making modifications,
      including but not limited to software source code, documentation
      source, and configuration files.

      "Object" form shall mean any form resulting from mechanical
      transformation or translation of a Source form, including but
      not limited to compiled object code, generated documentation,
      and conversions to other media types.

      "Work" shall mean the work of authorship, whether in Source or
      Object form, made available under the License, as indicated by a
      copyright notice that is included in or attached to the work
      (an example is provided in the Appendix below).

      "Derivative Works" shall mean any work, whether in Source or Object
      form, that is based on (or derived from) the Work and for which the
      editorial revisions, annotations, elaborations, or other modifications
      represent, as a whole, an original work of authorship. For the purposes
      of this License, Derivative Works shall not include works that remain
      separable from, or merely link (or bind by name) to the interfaces of,
      the Work and Derivative Works thereof.

      "Contribution" shall mean any work of authorship, including
      the original version of the Work and any modifications or additions
      to that Work or Derivative Works thereof, that is intentionally
      submitted to Licensor for inclusion in the Work by the copyright owner
      or by an individual or Legal Entity authorized to submit on behalf of
      the copyright owner. For the purposes of this definition, "submitted"
      means any form of electronic, verbal, or written communication sent
      to the Licensor or its representatives, including but not limited to
      communication on electronic mailing lists, source code control systems,
      and issue tracking systems that are managed by, or on behalf of, the
      Licensor for the purpose of discussing and improving the Work, but
      excluding communication that is conspicuously marked or otherwise
      designated in writing by the copyright owner as "Not a Contribution."

      "Contributor" shall mean Licensor and any individual or Legal Entity
      on behalf of whom a Contribution has been received by Licensor and
      subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      copyright license to reproduce, prepare Derivative Works of,
      publicly display, publicly perform, sublicense, and distribute the
      Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of
      this License, each Contributor hereby grants to You a perpetual,
      worldwide, non-exclusive, no-charge, royalty-free, irrevocable
      (except as stated in this section) patent license to make, have made,
      use, offer to sell, sell, import, and otherwise transfer the Work,
      where such license applies only to those patent claims licensable
      by such Contributor that are necessarily infringed by their
      Contribution(s) alone or by combination of their Contribution(s)
      with the Work to which such Contribution(s) was submitted. If You
      institute patent litigation against any entity (including a
      cross-claim or counterclaim in a lawsuit) alleging that the Work
      or a Contribution incorporated within the Work constitutes direct
      or contributory patent infringement, then any patent licenses
      granted to You under this License for that Work shall terminate
      as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the
      Work or Derivative Works thereof in any medium, with or without
      modifications, and in Source or Object form, provided that You
      meet the following conditions:

      (a) You must give any other recipients of the Work or
          Derivative Works a copy of this License; and

      (b) You must cause any modified files to carry prominent notices
          stating that You changed the files; and

      (c) You must retain, in the Source form of any Derivative Works
          that You distribute, all copyright, patent, trademark, and
          attribution notices from the Source form of the Work,
          excluding those notices that do not pertain to any part of
          the Derivative Works; and

      (d) If the Work includes a "NOTICE" text file as part of its
          distribution, then any Derivative Works that You distribute must
          include a readable copy of the attribution notices contained
          within such NOTICE file, excluding those notices that do not
          pertain to any part of the Derivative Works, in at least one
          of the following places: within a NOTICE text file distributed
          as part of the Derivative Works; within the Source form or
          documentation, if provided along with the Derivative Works; or,
          within a display generated by the Derivative Works, if and
          wherever such third-party notices normally appear. The contents
          of the NOTICE file are for informational purposes only and
          do not modify the License. You may add Your own attribution
          notices within Derivative Works that You distribute, alongside
          or as an addendum to the NOTICE text from the Work, provided
          that such additional attribution notices cannot be construed
          as modifying the License.

      You may add Your own copyright statement to Your modifications and
      may provide additional or different license terms and conditions
      for use, reproduction, or distribution of Your modifications, or
      for any such Derivative Works as a whole, provided Your use,
      reproduction, and distribution of the Work otherwise complies with
      the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise,
      any Contribution intentionally submitted for inclusion in the Work
      by You to the Licensor shall be under the terms and conditions of
      this License, without any additional terms or conditions.
      Notwithstanding the above, nothing herein shall supersede or modify
      the terms of any separate license agreement you may have executed
      with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade
      names, trademarks, service marks, or product names of the Licensor,
      except as required for reasonable and customary use in describing the
      origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or
      agreed to in writing, Licensor provides the Work (and each
      Contributor provides its Contributions) on an "AS IS" BASIS,
      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
      implied, including, without limitation, any warranties or conditions
      of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A
      PARTICULAR PURPOSE. You are solely responsible for determining the
      appropriateness of using or redistributing the Work and assume any
      risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory,
      whether in tort (including negligence), contract, or otherwise,
      unless required by applicable law (such as deliberate and grossly
      negligent acts) or agreed to in writing, shall any Contributor be
      liable to You for damages, including any direct, indirect, special,
      incidental, or consequential damages of any character arising as a
      result of this License or out of the use or inability to use the
      Work (including but not limited to damages for loss of goodwill,
      work stoppage, computer failure or malfunction, or any and all
      other commercial damages or losses), even if such Contributor
      has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing
      the Work or Derivative Works thereof, You may choose to offer,
      and charge a fee for, acceptance of support, warranty, indemnity,
      or other liability obligations and/or rights consistent with this
      License. However, in accepting such obligations, You may act only
      on Your own behalf and on Your sole responsibility, not on behalf
      of any other Contributor, and only if You agree to indemnify,
      defend, and hold each Contributor harmless for any liability
      incurred by, or claims asserted against, such Contributor by reason
      of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS

   APPENDIX: How to apply the Apache License to your work.

      To apply the Apache License to your work, attach the following
      boilerplate notice, with the fields enclosed by brackets "[]"
      replaced with your own identifying information. (Don't include
      the brackets!)  The text should be enclosed in the appropriate
      comment syntax for the file format. We also recommend that a
      file or class name and description of purpose be included on the
      same "printed page" as the copyright notice for easier
      identification within third-party archives.

   Copyright [yyyy] [name of copyright owner]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
</pre>
</div>
</div>


<div class="product">
<span class="title">Mozilla Personal Security Manager</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://mxr.mozilla.org/mozilla-central/source/security/manager/">homepage</a></span>
<div class="licence">
<pre>/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the Netscape security libraries.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
</pre>
</div>
</div>


<div class="product">
<span class="title">NSBezierPath additions from Sean Patrick O'Brien</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.seanpatrickobrien.com/journal/posts/3">homepage</a></span>
<div class="licence">
<pre>Copyright 2008 MolokoCacao
All rights reserved

Redistribution and use in source and binary forms, with or without
modification, are permitted providing that the following conditions 
are met:
1. Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">NVidia Control X Extension Library</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://cgit.freedesktop.org/~aplattner/nvidia-settings/">homepage</a></span>
<div class="licence">
<pre>/*
 * Copyright (c) 2008 NVIDIA, Corporation
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice (including the next
 * paragraph) shall be included in all copies or substantial portions of the
 * Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */</pre>
</div>
</div>


<div class="product">
<span class="title">Netscape Portable Runtime (NSPR)</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.mozilla.org/projects/nspr/">homepage</a></span>
<div class="licence">
<pre>/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the Netscape Portable Runtime (NSPR).
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1998-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
</pre>
</div>
</div>


<div class="product">
<span class="title">Network Security Services (NSS)</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.mozilla.org/projects/security/pki/nss/">homepage</a></span>
<div class="licence">
<pre>/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is the Netscape security libraries.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by the Initial Developer are Copyright (C) 1994-2000
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */
</pre>
</div>
</div>


<div class="product">
<span class="title">OTS (OpenType Sanitizer)</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://github.com/khaledhosny/ots.git">homepage</a></span>
<div class="licence">
<pre>// Copyright (c) 2009 The Chromium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">OpenH264</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.openh264.org/">homepage</a></span>
<div class="licence">
<pre>Copyright (c) 2013, Cisco Systems
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</pre>
</div>
</div>


<div class="product">
<span class="title">OpenMAX DL</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="https://silver.arm.com/download/Software/Graphics/OX000-BU-00010-r1p0-00bet0/OX000-BU-00010-r1p0-00bet0.tgz">homepage</a></span>
<div class="licence">
<pre>Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file in the root of the source tree. All
contributing project authors may be found in the AUTHORS file in the
root of the source tree.

The files were originally licensed by ARM Limited.

The following files:

    * dl/api/omxtypes.h
    * dl/sp/api/omxSP.h

are licensed by Khronos:

Copyright © 2005-2008 The Khronos Group Inc. All Rights Reserved. 

These materials are protected by copyright laws and contain material 
proprietary to the Khronos Group, Inc.  You may use these materials 
for implementing Khronos specifications, without altering or removing 
any trademark, copyright or other notice from the specification.

Khronos Group makes no, and expressly disclaims any, representations 
or warranties, express or implied, regarding these materials, including, 
without limitation, any implied warranties of merchantability or fitness 
for a particular purpose or non-infringement of any intellectual property. 
Khronos Group makes no, and expressly disclaims any, warranties, express 
or implied, regarding the correctness, accuracy, completeness, timeliness, 
and reliability of these materials. 

Under no circumstances will the Khronos Group, or any of its Promoters, 
Contributors or Members or their respective partners, officers, directors, 
employees, agents or representatives be liable for any damages, whether 
direct, indirect, special or consequential damages for lost revenues, 
lost profits, or otherwise, arising from or in connection with these 
materials.

Khronos and OpenMAX are trademarks of the Khronos Group Inc. 
</pre>
</div>
</div>


<div class="product">
<span class="title">PDFium</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://code.google.com/p/pdfium/">homepage</a></span>
<div class="licence">
<pre>// Copyright 2014 PDFium Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">PLY (Python Lex-Yacc)</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.dabeaz.com/ply/ply-3.4.tar.gz">homepage</a></span>
<div class="licence">
<pre>PLY (Python Lex-Yacc)                   Version 3.4

Copyright (C) 2001-2011,
David M. Beazley (Dabeaz LLC)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

* Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.  
* Redistributions in binary form must reproduce the above copyright notice, 
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.  
* Neither the name of the David Beazley or Dabeaz LLC may be used to
  endorse or promote products derived from this software without
  specific prior written permission. 

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.</pre>
</div>
</div>


<div class="product">
<span class="title">Paul Hsieh's SuperFastHash</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.azillionmonkeys.com/qed/hash.html">homepage</a></span>
<div class="licence">
<pre>Paul Hsieh OLD BSD license

Copyright (c) 2010, Paul Hsieh
All rights reserved.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this
  list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.
* Neither my name, Paul Hsieh, nor the names of any other contributors to the
  code use may not be used to endorse or promote products derived from this
  software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Polymer</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://www.polymer-project.org">homepage</a></span>
<div class="licence">
<pre>// Copyright (c) 2012 The Polymer Authors. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are
// met:
//
//    * Redistributions of source code must retain the above copyright
// notice, this list of conditions and the following disclaimer.
//    * Redistributions in binary form must reproduce the above
// copyright notice, this list of conditions and the following disclaimer
// in the documentation and/or other materials provided with the
// distribution.
//    * Neither the name of Google Inc. nor the names of its
// contributors may be used to endorse or promote products derived from
// this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
// LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
// A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
// OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
// SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
// LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
// DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
// THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
</pre>
</div>
</div>


<div class="product">
<span class="title">Proguard</span>
<a class="show" href="#">show license</a>
<span class="homepage"><a href="http://proguard.sourceforge.net/">homepage</a></span>
<div class="licence">
<pre>                    GNU GENERAL PUBLIC LICENSE
                       Version 2, June 1991

 Copyright (C) 1989, 1991 Free Software Foundation, Inc.
     51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 Everyone is permitted to copy and distribute verbatim copies
 of this license document, but changing it is not allowed.

                            Preamble

  The licenses for most software are designed to take away your
freedom to share and change it.  By contrast, the GNU General Public
License is intended to guarantee your freedom to share and change free
software--to make sure the software is free for all its users.  This
General Public License applies to most of the Free Software
Foundation's software and to any other program whose authors commit to
using it.  (Some other Free Software Foundation software is covered by
the GNU Library General Public License instead.)  You can apply it to
your programs, too.

  When we speak of free software, we are referring to freedom, not
price.  Our General Public Licenses are designed to make sure that you
have the freedom to distribute copies of free software (and charge for
this service if you wish), that you receive source code or can get it
if you want it, that you can change the software or use pieces of it
in new free programs; and that you know you can do these things.

  To protect your rights, we need to make restrictions that forbid
anyone to deny you these rights or to ask you to surrender the rights.
These restrictions translate to certain responsibilities for you if you
distribute copies of the software, or if you modify it.

  For example, if you distribute copies of such a program, whether
gratis or for a fee, you must give the recipients all the rights that
you have.  You must make sure that they, too, receive or can get the
source code.  And you must show them these terms so they know their
rights.

  We protect your rights with two steps: (1) copyright the software, and
(2) offer you this license which gives you legal permission to copy,
distribute and/or modify the software.

  Also, for each author's protection and ours, we want to make certain
that everyone understands that there is no warranty for this free
software.  If the software is modified by someone else and passed on, we
want its recipients to know that what they have is not the original, so
that any problems introduced by others will not reflect on the original
authors' reputations.

  Finally, any free program is threatened constantly by software
patents.  We wish to avoid the danger that redistributors of a free
program will individually obtain patent licenses, in effect making the
program proprietary.  To prevent this, we have made it clear that any
patent must be licensed for everyone's free use or not licensed at all.

  The precise terms and conditions for copying, distribution and
modification follow.
