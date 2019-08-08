* flow, and unhides the dialog once the page has loaded.
 * @param {string} url The url of the authorization entry point.
 * @param {Object} win The dialog window that contains this page. Can
 *     be left undefined if the caller does not want to display the
 *     window.
 */
function loadAuthUrlAndShowWindow(url, win) {
  // Send popups from the webview to a normal browser window.
  webview.addEventListener('newwindow', function(e) {
    e.window.discard();
    window.open(e.targetUrl);
  });

  // Request a customized view from GAIA.
  webview.request.onBeforeSendHeaders.addListener(function(details) {
    headers = details.requestHeaders || [];
    headers.push({'name': 'X-Browser-View',
                  'value': 'embedded'});
    return { requestHeaders: headers };
  }, {
    urls: ['https://accounts.google.com/*'],
  }, ['blocking', 'requestHeaders']);

  if (url.toLowerCase().indexOf('https://accounts.google.com/') != 0)
    document.querySelector('.titlebar').classList.add('titlebar-border');

  webview.src = url;
  if (win) {
    webview.addEventListener('loadstop', function() {
      win.show();
    });
  }
}

document.addEventListener('DOMContentLoaded', function() {
  webview = document.querySelector('webview');

  document.querySelector('.titlebar-close-button').onclick = function() {
    window.close();
  };

  chrome.resourcesPrivate.getStrings('identity', function(strings) {
    document.title = strings['window-title'];
  });
});

// Copyright 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

chrome.app.runtime.onLaunched.addListener(function() {
  chrome.app.window.create(
      'chrome://settings-frame/options_settings_app.html',
      {'id': 'settings_app', 'height': 550, 'width': 750});
});
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

body {
  background-color: rgb(82, 86, 89);
  font-family: 'Roboto', 'Noto', sans-serif;
  margin: 0;
}

viewer-page-indicator {
  visibility: hidden;
  z-index: 2;
}

viewer-pdf-toolbar {
  position: fixed;
  width: 100%;
  z-index: 4;
}

#plugin {
  height: 100%;
  position: fixed;
  width: 100%;
  z-index: 1;
}

#sizer {
  position: absolute;
  z-index: 0;
}

@media(max-height: 250px) {
  viewer-pdf-toolbar {
    display: none;
  }
}

@media(max-height: 200px) {
  viewer-zoom-toolbar {
    display: none;
  }
}

@media(max-width: 300px) {
  viewer-zoom-toolbar {
    display: none;
  }
}
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <base href="chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai/">
  <meta charset="utf-8">
  <!-- Must be before any other scripts or Polymer imports. -->
  <script src="chrome://resources/js/polymer_config.js"></script>
  <link rel="import" href="elements/viewer-error-screen/viewer-error-screen.html">
  <link rel="import" href="elements/viewer-page-indicator/viewer-page-indicator.html">
  <link rel="import" href="elements/viewer-page-selector/viewer-page-selector.html">
  <link rel="import" href="elements/viewer-password-screen/viewer-password-screen.html">
  <link rel="import" href="elements/viewer-pdf-toolbar/viewer-pdf-toolbar.html">
  <link rel="import" href="elements/viewer-zoom-toolbar/viewer-zoom-toolbar.html">

  <link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
  <link rel="stylesheet" href="chrome://resources/css/roboto.css">
  <link rel="stylesheet" href="index.css">
</head>
<body>

<viewer-pdf-toolbar id="toolbar" hidden></viewer-pdf-toolbar>

<div id="sizer"></div>
<viewer-password-screen id="password-screen"></viewer-password-screen>

<viewer-zoom-toolbar id="zoom-toolbar"></viewer-zoom-toolbar>

<viewer-page-indicator id="page-indicator"></viewer-page-indicator>

<viewer-error-screen id="error-screen"></viewer-error-screen>

</body>
<script src="toolbar_manager.js"></script>
<script src="viewport.js"></script>
<script src="open_pdf_params_parser.js"></script>
<script src="navigator.js"></script>
<script src="viewport_scroller.js"></script>
<script src="zoom_manager.js"></script>
<script src="pdf_scripting_api.js"></script>
<script src="chrome://resources/js/i18n_template_no_process.js"></script>
<script src="chrome://resources/js/load_time_data.js"></script>
<script src="chrome://resources/js/util.js"></script>
<script src="browser_api.js"></script>
<script src="pdf.js"></script>
<script src="main.js"></script>
</html>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Global PDFViewer object, accessible for testing.
 * @type Object
 */
var viewer;


(function() {
  /**
   * Stores any pending messages received which should be passed to the
   * PDFViewer when it is created.
   * @type Array
   */
  var pendingMessages = [];

  /**
   * Handles events that are received prior to the PDFViewer being created.
   * @param {Object} message A message event received.
   */
  function handleScriptingMessage(message) {
    pendingMessages.push(message);
  }

  /**
   * Initialize the global PDFViewer and pass any outstanding messages to it.
   * @param {Object} browserApi An object providing an API to the browser.
   */
  function initViewer(browserApi) {
    // PDFViewer will handle any messages after it is created.
    window.removeEventListener('message', handleScriptingMessage, false);
    viewer = new PDFViewer(browserApi);
    while (pendingMessages.length > 0)
      viewer.handleScriptingMessage(pendingMessages.shift());
  }

  /**
   * Entrypoint for starting the PDF viewer. This function obtains the browser
   * API for the PDF and constructs a PDFViewer object with it.
   */
  function main() {
    // Set up an event listener to catch scripting messages which are sent prior
    // to the PDFViewer being created.
    window.addEventListener('message', handleScriptingMessage, false);

    createBrowserApi().then(initViewer);
  };

  main();
})();
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * @return {number} Width of a scrollbar in pixels
 */
function getScrollbarWidth() {
  var div = document.createElement('div');
  div.style.visibility = 'hidden';
  div.style.overflow = 'scroll';
  div.style.width = '50px';
  div.style.height = '50px';
  div.style.position = 'absolute';
  document.body.appendChild(div);
  var result = div.offsetWidth - div.clientWidth;
  div.parentNode.removeChild(div);
  return result;
}

/**
 * Return the filename component of a URL, percent decoded if possible.
 * @param {string} url The URL to get the filename from.
 * @return {string} The filename component.
 */
function getFilenameFromURL(url) {
  // Ignore the query and fragment.
  var mainUrl = url.split(/#|\?/)[0];
  var components = mainUrl.split(/\/|\\/);
  var filename = components[components.length - 1];
  try {
    return decodeURIComponent(filename);
  } catch (e) {
    if (e instanceof URIError)
      return filename;
    throw e;
  }
}

/**
 * Called when navigation happens in the current tab.
 * @param {boolean} isInTab Indicates if the PDF viewer is displayed in a tab.
 * @param {boolean} isSourceFileUrl Indicates if the navigation source is a
 *     file:// URL.
 * @param {string} url The url to be opened in the current tab.
 */
function onNavigateInCurrentTab(isInTab, isSourceFileUrl, url) {
  // When the PDFviewer is inside a browser tab, prefer the tabs API because
  // it can navigate from one file:// URL to another.
  if (chrome.tabs && isInTab && isSourceFileUrl)
    chrome.tabs.update({url: url});
  else
    window.location.href = url;
}

/**
 * Called when navigation happens in the new tab.
 * @param {string} url The url to be opened in the new tab.
 */
function onNavigateInNewTab(url) {
  // Prefer the tabs API because it guarantees we can just open a new tab.
  // window.open doesn't have this guarantee.
  if (chrome.tabs)
    chrome.tabs.create({url: url});
  else
    window.open(url);
}

/**
 * Whether keydown events should currently be ignored. Events are ignored when
 * an editable element has focus, to allow for proper editing controls.
 * @param {HTMLElement} activeElement The currently selected DOM node.
 * @return {boolean} True if keydown events should be ignored.
 */
function shouldIgnoreKeyEvents(activeElement) {
  while (activeElement.shadowRoot != null &&
         activeElement.shadowRoot.activeElement != null) {
    activeElement = activeElement.shadowRoot.activeElement;
  }

  return (activeElement.isContentEditable ||
          activeElement.tagName == 'INPUT' ||
          activeElement.tagName == 'TEXTAREA');
}

/**
 * The minimum number of pixels to offset the toolbar by from the bottom and
 * right side of the screen.
 */
PDFViewer.MIN_TOOLBAR_OFFSET = 15;

/**
 * The height of the toolbar along the top of the page. The document will be
 * shifted down by this much in the viewport.
 */
PDFViewer.MATERIAL_TOOLBAR_HEIGHT = 56;

/**
 * Minimum height for the material toolbar to show (px). Should match the media
 * query in index-material.css. If the window is smaller than this at load,
 * leave no space for the toolbar.
 */
PDFViewer.TOOLBAR_WINDOW_MIN_HEIGHT = 250;

/**
 * The light-gray background color used for print preview.
 */
PDFViewer.LIGHT_BACKGROUND_COLOR = '0xFFCCCCCC';

/**
 * The dark-gray background color used for the regular viewer.
 */
PDFViewer.DARK_BACKGROUND_COLOR = '0xFF525659';

/**
 * Creates a new PDFViewer. There should only be one of these objects per
 * document.
 * @constructor
 * @param {!BrowserApi} browserApi An object providing an API to the browser.
 */
function PDFViewer(browserApi) {
  this.browserApi_ = browserApi;
  this.originalUrl_ = this.browserApi_.getStreamInfo().originalUrl;
  this.loadState_ = LoadState.LOADING;
  this.parentWindow_ = null;
  this.parentOrigin_ = null;
  this.isFormFieldFocused_ = false;

  this.delayedScriptingMessages_ = [];

  this.isPrintPreview_ = this.originalUrl_.indexOf(
                             'chrome://print') == 0;
  // Parse open pdf parameters.
  this.paramsParser_ =
      new OpenPDFParamsParser(this.getNamedDestination_.bind(this));
  var toolbarEnabled =
      this.paramsParser_.getUiUrlParams(this.originalUrl_).toolbar &&
      !this.isPrintPreview_;

  // The sizer element is placed behind the plugin element to cause scrollbars
  // to be displayed in the window. It is sized according to the document size
  // of the pdf and zoom level.
  this.sizer_ = $('sizer');
  if (this.isPrintPreview_)
    this.pageIndicator_ = $('page-indicator');
  this.passwordScreen_ = $('password-screen');
  this.passwordScreen_.addEventListener('password-submitted',
                                        this.onPasswordSubmitted_.bind(this));
  this.errorScreen_ = $('error-screen');
  // Can only reload if we are in a normal tab.
  if (chrome.tabs && this.browserApi_.getStreamInfo().tabId != -1) {
    this.errorScreen_.reloadFn = function() {
      chrome.tabs.reload(this.browserApi_.getStreamInfo().tabId);
    }.bind(this);
  }

  // Create the viewport.
  var shortWindow = window.innerHeight < PDFViewer.TOOLBAR_WINDOW_MIN_HEIGHT;
  var topToolbarHeight =
      (toolbarEnabled) ? PDFViewer.MATERIAL_TOOLBAR_HEIGHT : 0;
  this.viewport_ = new Viewport(window,
                                this.sizer_,
                                this.viewportChanged_.bind(this),
                                this.beforeZoom_.bind(this),
                                this.afterZoom_.bind(this),
                                getScrollbarWidth(),
                                this.browserApi_.getDefaultZoom(),
                                topToolbarHeight);

  // Create the plugin object dynamically so we can set its src. The plugin
  // element is sized to fill the entire window and is set to be fixed
  // positioning, acting as a viewport. The plugin renders into this viewport
  // according to the scroll position of the window.
  this.plugin_ = document.createElement('embed');
  // NOTE: The plugin's 'id' field must be set to 'plugin' since
  // chrome/renderer/printing/print_web_view_helper.cc actually references it.
  this.plugin_.id = 'plugin';
  this.plugin_.type = 'application/x-google-chrome-pdf';
  this.plugin_.addEventListener('message', this.handlePluginMessage_.bind(this),
                                false);

  // Handle scripting messages from outside the extension that wish to interact
  // with it. We also send a message indicating that extension has loaded and
  // is ready to receive messages.
  window.addEventListener('message', this.handleScriptingMessage.bind(this),
                          false);

  this.plugin_.setAttribute('src',
                            this.originalUrl_);
  this.plugin_.setAttribute('stream-url',
                            this.browserApi_.getStreamInfo().streamUrl);
  var headers = '';
  for (var header in this.browserApi_.getStreamInfo().responseHeaders) {
    headers += header + ': ' +
        this.browserApi_.getStreamInfo().responseHeaders[header] + '\n';
  }
  this.plugin_.setAttribute('headers', headers);

  var backgroundColor = PDFViewer.DARK_BACKGROUND_COLOR;
  this.plugin_.setAttribute('background-color', backgroundColor);
  this.plugin_.setAttribute('top-toolbar-height', topToolbarHeight);

  if (!this.browserApi_.getStreamInfo().embedded)
    this.plugin_.setAttribute('full-frame', '');
  document.body.appendChild(this.plugin_);

  // Setup the button event listeners.
  this.zoomToolbar_ = $('zoom-toolbar');
  this.zoomToolbar_.addEventListener('fit-to-width',
      this.viewport_.fitToWidth.bind(this.viewport_));
  this.zoomToolbar_.addEventListener('fit-to-page',
      this.fitToPage_.bind(this));
  this.zoomToolbar_.addEventListener('zoom-in',
      this.viewport_.zoomIn.bind(this.viewport_));
  this.zoomToolbar_.addEventListener('zoom-out',
      this.viewport_.zoomOut.bind(this.viewport_));

  if (toolbarEnabled) {
    this.toolbar_ = $('toolbar');
    this.toolbar_.hidden = false;
    this.toolbar_.addEventListener('save', this.save_.bind(this));
    this.toolbar_.addEventListener('print', this.print_.bind(this));
    this.toolbar_.addEventListener('rotate-right',
        this.rotateClockwise_.bind(this));
    // Must attach to mouseup on the plugin element, since it eats mousedown
    // and click events.
    this.plugin_.addEventListener('mouseup',
        this.toolbar_.hideDropdowns.bind(this.toolbar_));

    this.toolbar_.docTitle =
        getFilenameFromURL(this.originalUrl_);
  }

  document.body.addEventListener('change-page', function(e) {
    this.viewport_.goToPage(e.detail.page);
  }.bind(this));

  document.body.addEventListener('navigate', function(e) {
    this.navigator_.navigate(e.detail.uri, e.detail.newtab);
  }.bind(this));

  this.toolbarManager_ =
      new ToolbarManager(window, this.toolbar_, this.zoomToolbar_);

  // Set up the ZoomManager.
  this.zoomManager_ = new ZoomManager(
      this.viewport_, this.browserApi_.setZoom.bind(this.browserApi_),
      this.browserApi_.getInitialZoom());
  this.browserApi_.addZoomEventListener(
      this.zoomManager_.onBrowserZoomChange.bind(this.zoomManager_));

  // Setup the keyboard event listener.
  document.addEventListener('keydown', this.handleKeyEvent_.bind(this));
  document.addEventListener('mousemove', this.handleMouseEvent_.bind(this));
  document.addEventListener('mouseout', this.handleMouseEvent_.bind(this));

  var isInTab = this.browserApi_.getStreamInfo().tabId != -1;
  var isSourceFileUrl =
      this.originalUrl_.indexOf('file://') == 0;
  this.navigator_ = new Navigator(this.originalUrl_,
                                  this.viewport_, this.paramsParser_,
                                  onNavigateInCurrentTab.bind(undefined,
                                                              isInTab,
                                                              isSourceFileUrl),
                                  onNavigateInNewTab);
  this.viewportScroller_ =
      new ViewportScroller(this.viewport_, this.plugin_, window);

  // Request translated strings.
  chrome.resourcesPrivate.getStrings('pdf', this.handleStrings_.bind(this));
}

PDFViewer.prototype = {
  /**
   * @private
   * Handle key events. These may come from the user directly or via the
   * scripting API.
   * @param {KeyboardEvent} e the event to handle.
   */
  handleKeyEvent_: function(e) {
    var position = this.viewport_.position;
    // Certain scroll events may be sent from outside of the extension.
    var fromScriptingAPI = e.fromScriptingAPI;

    if (shouldIgnoreKeyEvents(document.activeElement) || e.defaultPrevented)
      return;

    this.toolbarManager_.hideToolbarsAfterTimeout(e);

    var pageUpHandler = function() {
      // Go to the previous page if we are fit-to-page.
      if (this.viewport_.fittingType == Viewport.FittingType.FIT_TO_PAGE) {
        this.viewport_.goToPage(this.viewport_.getMostVisiblePage() - 1);
        // Since we do the movement of the page.
        e.preventDefault();
      } else if (fromScriptingAPI) {
        position.y -= this.viewport.size.height;
        this.viewport.position = position;
      }
    }.bind(this);
    var pageDownHandler = function() {
      // Go to the next page if we are fit-to-page.
      if (this.viewport_.fittingType == Viewport.FittingType.FIT_TO_PAGE) {
        this.viewport_.goToPage(this.viewport_.getMostVisiblePage() + 1);
        // Since we do the movement of the page.
        e.preventDefault();
      } else if (fromScriptingAPI) {
        position.y += this.viewport.size.height;
        this.viewport.position = position;
      }
    }.bind(this);

    switch (e.keyCode) {
      case 9:  // Tab key.
        this.toolbarManager_.showToolbarsForKeyboardNavigation();
        return;
      case 27:  // Escape key.
        if (!this.isPrintPreview_) {
          this.toolbarManager_.hideSingleToolbarLayer();
          return;
        }
        break;  // Ensure escape falls through to the print-preview handler.
      case 32:  // Space key.
        if (e.shiftKey)
          pageUpHandler();
        else
          pageDownHandler();
        return;
      case 33:  // Page up key.
        pageUpHandler();
        return;
      case 34:  // Page down key.
        pageDownHandler();
        return;
      case 37:  // Left arrow key.
        if (!(e.altKey || e.ctrlKey || e.metaKey || e.shiftKey)) {
          // Go to the previous page if there are no horizontal scrollbars and
          // no form field is focused.
          if (!(this.viewport_.documentHasScrollbars().horizontal ||
                this.isFormFieldFocused_)) {
            this.viewport_.goToPage(this.viewport_.getMostVisiblePage() - 1);
            // Since we do the movement of the page.
            e.preventDefault();
          } else if (fromScriptingAPI) {
            position.x -= Viewport.SCROLL_INCREMENT;
            this.viewport.position = position;
          }
        }
        return;
      case 38:  // Up arrow key.
        if (fromScriptingAPI) {
          position.y -= Viewport.SCROLL_INCREMENT;
          this.viewport.position = position;
        }
        return;
      case 39:  // Right arrow key.
        if (!(e.altKey || e.ctrlKey || e.metaKey || e.shiftKey)) {
          // Go to the next page if there are no horizontal scrollbars and no
          // form field is focused.
          if (!(this.viewport_.documentHasScrollbars().horizontal ||
                this.isFormFieldFocused_)) {
            this.viewport_.goToPage(this.viewport_.getMostVisiblePage() + 1);
            // Since we do the movement of the page.
            e.preventDefault();
          } else if (fromScriptingAPI) {
            position.x += Viewport.SCROLL_INCREMENT;
            this.viewport.position = position;
          }
        }
        return;
      case 40:  // Down arrow key.
        if (fromScriptingAPI) {
          position.y += Viewport.SCROLL_INCREMENT;
          this.viewport.position = position;
        }
        return;
      case 65:  // a key.
        if (e.ctrlKey || e.metaKey) {
          this.plugin_.postMessage({
            type: 'selectAll'
          });
          // Since we do selection ourselves.
          e.preventDefault();
        }
        return;
      case 71: // g key.
        if (this.toolbar_ && (e.ctrlKey || e.metaKey) && e.altKey) {
          this.toolbarManager_.showToolbars();
          this.toolbar_.selectPageNumber();
        }
        return;
      case 219:  // left bracket.
        if (e.ctrlKey)
          this.rotateCounterClockwise_();
        return;
      case 221:  // right bracket.
        if (e.ctrlKey)
          this.rotateClockwise_();
        return;
    }

    // Give print preview a chance to handle the key event.
    if (!fromScriptingAPI && this.isPrintPreview_) {
      this.sendScriptingMessage_({
        type: 'sendKeyEvent',
        keyEvent: SerializeKeyEvent(e)
      });
    } else {
      // Show toolbars as a fallback.
      if (!(e.shiftKey || e.ctrlKey || e.altKey))
        this.toolbarManager_.showToolbars();
    }
  },

  handleMouseEvent_: function(e) {
    if (e.type == 'mousemove')
      this.toolbarManager_.handleMouseMove(e);
    else if (e.type == 'mouseout')
      this.toolbarManager_.hideToolbarsForMouseOut();
  },

  /**
   * @private
   * Rotate the plugin clockwise.
   */
  rotateClockwise_: function() {
    this.plugin_.postMessage({
      type: 'rotateClockwise'
    });
  },

  /**
   * @private
   * Rotate the plugin counter-clockwise.
   */
  rotateCounterClockwise_: function() {
    this.plugin_.postMessage({
      type: 'rotateCounterclockwise'
    });
  },

  fitToPage_: function() {
    this.viewport_.fitToPage();
    this.toolbarManager_.forceHideTopToolbar();
  },

  /**
   * @private
   * Notify the plugin to print.
   */
  print_: function() {
    this.plugin_.postMessage({
      type: 'print'
    });
  },

  /**
   * @private
   * Notify the plugin to save.
   */
  save_: function() {
    this.plugin_.postMessage({
      type: 'save'
    });
  },

  /**
   * Fetches the page number corresponding to the given named destination from
   * the plugin.
   * @param {string} name The namedDestination to fetch page number from plugin.
   */
  getNamedDestination_: function(name) {
    this.plugin_.postMessage({
      type: 'getNamedDestination',
      namedDestination: name
    });
  },

  /**
   * @private
   * Sends a 'documentLoaded' message to the PDFScriptingAPI if the document has
   * finished loading.
   */
  sendDocumentLoadedMessage_: function() {
    if (this.loadState_ == LoadState.LOADING)
      return;
    this.sendScriptingMessage_({
      type: 'documentLoaded',
      load_state: this.loadState_
    });
  },

  /**
   * @private
   * Handle open pdf parameters. This function updates the viewport as per
   * the parameters mentioned in the url while opening pdf. The order is
   * important as later actions can override the effects of previous actions.
   * @param {Object} viewportPosition The initial position of the viewport to be
   *     displayed.
   */
  handleURLParams_: function(viewportPosition) {
    if (viewportPosition.page != undefined)
      this.viewport_.goToPage(viewportPosition.page);
    if (viewportPosition.position) {
      // Make sure we don't cancel effect of page parameter.
      this.viewport_.position = {
        x: this.viewport_.position.x + viewportPosition.position.x,
        y: this.viewport_.position.y + viewportPosition.position.y
      };
    }
    if (viewportPosition.zoom)
      this.viewport_.setZoom(viewportPosition.zoom);
  },

  /**
   * @private
   * Update the loading progress of the document in response to a progress
   * message being received from the plugin.
   * @param {number} progress the progress as a percentage.
   */
  updateProgress_: function(progress) {
    if (this.toolbar_)
      this.toolbar_.loadProgress = progress;

    if (progress == -1) {
      // Document load failed.
      this.errorScreen_.show();
      this.sizer_.style.display = 'none';
      if (this.passwordScreen_.active) {
        this.passwordScreen_.deny();
        this.passwordScreen_.active = false;
      }
      this.loadState_ = LoadState.FAILED;
      this.sendDocumentLoadedMessage_();
    } else if (progress == 100) {
      // Document load complete.
      if (this.lastViewportPosition_)
        this.viewport_.position = this.lastViewportPosition_;
      this.paramsParser_.getViewportFromUrlParams(
          this.originalUrl_,
          this.handleURLParams_.bind(this));
      this.loadState_ = LoadState.SUCCESS;
      this.sendDocumentLoadedMessage_();
      while (this.delayedScriptingMessages_.length > 0)
        this.handleScriptingMessage(this.delayedScriptingMessages_.shift());

      this.toolbarManager_.hideToolbarsAfterTimeout();
    }
  },

  /**
   * @private
   * Load a dictionary of translated strings into the UI. Used as a callback for
   * chrome.resourcesPrivate.
   * @param {Object} strings Dictionary of translated strings
   */
  handleStrings_: function(strings) {
    window.loadTimeData.data = strings;
    i18nTemplate.process(document, loadTimeData);
    this.zoomToolbar_.updateTooltips();
  },

  /**
   * @private
   * An event handler for handling password-submitted events. These are fired
   * when an event is entered into the password screen.
   * @param {Object} event a password-submitted event.
   */
  onPasswordSubmitted_: function(event) {
    this.plugin_.postMessage({
      type: 'getPasswordComplete',
      password: event.detail.password
    });
  },

  /**
   * @private
   * An event handler for handling message events received from the plugin.
   * @param {MessageObject} message a message event.
   */
  handlePluginMessage_: function(message) {
    switch (message.data.type.toString()) {
      case 'documentDimensions':
        this.documentDimensions_ = message.data;
        this.viewport_.setDocumentDimensions(this.documentDimensions_);
        // If we received the document dimensions, the password was good so we
        // can dismiss the password screen.
        if (this.passwordScreen_.active)
          this.passwordScreen_.accept();

        if (this.pageIndicator_)
          this.pageIndicator_.initialFadeIn();

        if (this.toolbar_) {
          this.toolbar_.docLength =
              this.documentDimensions_.pageDimensions.length;
        }
        break;
      case 'email':
        var href = 'mailto:' + message.data.to + '?cc=' + message.data.cc +
            '&bcc=' + message.data.bcc + '&subject=' + message.data.subject +
            '&body=' + message.data.body;
        window.location.href = href;
        break;
      case 'getAccessibilityJSONReply':
        this.sendScriptingMessage_(message.data);
        break;
      case 'getPassword':
        // If the password screen isn't up, put it up. Otherwise we're
        // responding to an incorrect password so deny it.
        if (!this.passwordScreen_.active)
          this.passwordScreen_.active = true;
        else
          this.passwordScreen_.deny();
        break;
      case 'getSelectedTextReply':
        this.sendScriptingMessage_(message.data);
        break;
      case 'goToPage':
        this.viewport_.goToPage(message.data.page);
        break;
      case 'loadProgress':
        this.updateProgress_(message.data.progress);
        break;
      case 'navigate':
        // If in print preview, always open a new tab.
        if (this.isPrintPreview_)
          this.navigator_.navigate(message.data.url, true);
        else
          this.navigator_.navigate(message.data.url, message.data.newTab);
        break;
      case 'setScrollPosition':
        var position = this.viewport_.position;
        if (message.data.x !== undefined)
          position.x = message.data.x;
        if (message.data.y !== undefined)
          position.y = message.data.y;
        this.viewport_.position = position;
        break;
      case 'cancelStreamUrl':
        chrome.mimeHandlerPrivate.abortStream();
        break;
      case 'metadata':
        if (message.data.title) {
          document.title = message.data.title;
        } else {
          document.title =
              getFilenameFromURL(this.originalUrl_);
        }
        this.bookmarks_ = message.data.bookmarks;
        if (this.toolbar_) {
          this.toolbar_.docTitle = document.title;
          this.toolbar_.bookmarks = this.bookmarks;
        }
        break;
      case 'setIsSelecting':
        this.viewportScroller_.setEnableScrolling(message.data.isSelecting);
        break;
      case 'getNamedDestinationReply':
        this.paramsParser_.onNamedDestinationReceived(
            message.data.pageNumber);
        break;
      case 'formFocusChange':
        this.isFormFieldFocused_ = message.data.focused;
        break;
    }
  },

  /**
   * @private
   * A callback that's called before the zoom changes. Notify the plugin to stop
   * reacting to scroll events while zoom is taking place to avoid flickering.
   */
  beforeZoom_: function() {
    this.plugin_.postMessage({
      type: 'stopScrolling'
    });
  },

  /**
   * @private
   * A callback that's called after the zoom changes. Notify the plugin of the
   * zoom change and to continue reacting to scroll events.
   */
  afterZoom_: function() {
    var position = this.viewport_.position;
    var zoom = this.viewport_.zoom;
    this.plugin_.postMessage({
      type: 'viewport',
      zoom: zoom,
      xOffset: position.x,
      yOffset: position.y
    });
    this.zoomManager_.onPdfZoomChange();
  },

  /**
   * @private
   * A callback that's called after the viewport changes.
   */
  viewportChanged_: function() {
    if (!this.documentDimensions_)
      return;

    // Offset the toolbar position so that it doesn't move if scrollbars appear.
    var hasScrollbars = this.viewport_.documentHasScrollbars();
    var scrollbarWidth = this.viewport_.scrollbarWidth;
    var verticalScrollbarWidth = hasScrollbars.vertical ? scrollbarWidth : 0;
    var horizontalScrollbarWidth =
        hasScrollbars.horizontal ? scrollbarWidth : 0;

    // Shift the zoom toolbar to the left by half a scrollbar width. This
    // gives a compromise: if there is no scrollbar visible then the toolbar
    // will be half a scrollbar width further left than the spec but if there
    // is a scrollbar visible it will be half a scrollbar width further right
    // than the spec. In RTL layout, the zoom toolbar is on the left side, but
    // the scrollbar is still on the right, so this is not necessary.
    if (!isRTL()) {
      this.zoomToolbar_.style.right = -verticalScrollbarWidth +
          (scrollbarWidth / 2) + 'px';
    }
    // Having a horizontal scrollbar is much rarer so we don't offset the
    // toolbar from the bottom any more than what the spec says. This means
    // that when there is a scrollbar visible, it will be a full scrollbar
    // width closer to the bottom of the screen than usual, but this is ok.
    this.zoomToolbar_.style.bottom = -horizontalScrollbarWidth + 'px';

    // Update the page indicator.
    var visiblePage = this.viewport_.getMostVisiblePage();

    if (this.toolbar_)
      this.toolbar_.pageNo = visiblePage + 1;

    // TODO(raymes): Give pageIndicator_ the same API as toolbar_.
    if (this.pageIndicator_) {
      this.pageIndicator_.index = visiblePage;
      if (this.documentDimensions_.pageDimensions.length > 1 &&
          hasScrollbars.vertical) {
        this.pageIndicator_.style.visibility = 'visible';
      } else {
        this.pageIndicator_.style.visibility = 'hidden';
      }
    }

    var visiblePageDimensions = this.viewport_.getPageScreenRect(visiblePage);
    var size = this.viewport_.size;
    this.sendScriptingMessage_({
      type: 'viewport',
      pageX: visiblePageDimensions.x,
      pageY: visiblePageDimensions.y,
      pageWidth: visiblePageDimensions.width,
      viewportWidth: size.width,
      viewportHeight: size.height
    });
  },

  /**
   * Handle a scripting message from outside the extension (typically sent by
   * PDFScriptingAPI in a page containing the extension) to interact with the
   * plugin.
   * @param {MessageObject} message the message to handle.
   */
  handleScriptingMessage: function(message) {
    if (this.parentWindow_ != message.source) {
      this.parentWindow_ = message.source;
      this.parentOrigin_ = message.origin;
      // Ensure that we notify the embedder if the document is loaded.
      if (this.loadState_ != LoadState.LOADING)
        this.sendDocumentLoadedMessage_();
    }

    if (this.handlePrintPreviewScriptingMessage_(message))
      return;

    // Delay scripting messages from users of the scripting API until the
    // document is loaded. This simplifies use of the APIs.
    if (this.loadState_ != LoadState.SUCCESS) {
      this.delayedScriptingMessages_.push(message);
      return;
    }

    switch (message.data.type.toString()) {
      case 'getAccessibilityJSON':
      case 'getSelectedText':
      case 'print':
      case 'selectAll':
        this.plugin_.postMessage(message.data);
        break;
    }
  },

  /**
   * @private
   * Handle scripting messages specific to print preview.
   * @param {MessageObject} message the message to handle.
   * @return {boolean} true if the message was handled, false otherwise.
   */
  handlePrintPreviewScriptingMessage_: function(message) {
    if (!this.isPrintPreview_)
      return false;

    switch (message.data.type.toString()) {
      case 'loadPreviewPage':
        this.plugin_.postMessage(message.data);
        return true;
      case 'resetPrintPreviewMode':
        this.loadState_ = LoadState.LOADING;
        if (!this.inPrintPreviewMode_) {
          this.inPrintPreviewMode_ = true;
          this.viewport_.fitToPage();
        }

        // Stash the scroll location so that it can be restored when the new
        // document is loaded.
        this.lastViewportPosition_ = this.viewport_.position;

        // TODO(raymes): Disable these properly in the plugin.
        var printButton = $('print-button');
        if (printButton)
          printButton.parentNode.removeChild(printButton);
        var saveButton = $('save-button');
        if (saveButton)
          saveButton.parentNode.removeChild(saveButton);

        this.pageIndicator_.pageLabels = message.data.pageNumbers;

        this.plugin_.postMessage({
          type: 'resetPrintPreviewMode',
          url: message.data.url,
          grayscale: message.data.grayscale,
          // If the PDF isn't modifiable we send 0 as the page count so that no
          // blank placeholder pages get appended to the PDF.
          pageCount: (message.data.modifiable ?
                      message.data.pageNumbers.length : 0)
        });
        return true;
      case 'sendKeyEvent':
        this.handleKeyEvent_(DeserializeKeyEvent(message.data.keyEvent));
        return true;
    }

    return false;
  },

  /**
   * @private
   * Send a scripting message outside the extension (typically to
   * PDFScriptingAPI in a page containing the extension).
   * @param {Object} message the message to send.
   */
  sendScriptingMessage_: function(message) {
    if (this.parentWindow_ && this.parentOrigin_) {
      var targetOrigin;
      // Only send data back to the embedder if it is from the same origin,
      // unless we're sending it to ourselves (which could happen in the case
      // of tests). We also allow documentLoaded messages through as this won't
      // leak important information.
      if (this.parentOrigin_ == window.location.origin)
        targetOrigin = this.parentOrigin_;
      else if (message.type == 'documentLoaded')
        targetOrigin = '*';
      else
        targetOrigin = this.originalUrl_;
      this.parentWindow_.postMessage(message, targetOrigin);
    }
  },

  /**
   * @type {Viewport} the viewport of the PDF viewer.
   */
  get viewport() {
    return this.viewport_;
  },

  /**
   * Each bookmark is an Object containing a:
   * - title
   * - page (optional)
   * - array of children (themselves bookmarks)
   * @type {Array} the top-level bookmarks of the PDF.
   */
  get bookmarks() {
    return this.bookmarks_;
  }
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/** Idle time in ms before the UI is hidden. */
var HIDE_TIMEOUT = 2000;
/** Time in ms after force hide before toolbar is shown again. */
var FORCE_HIDE_TIMEOUT = 1000;
/**
 * Velocity required in a mousemove to reveal the UI (pixels/ms). This is
 * intended to be high enough that a fast flick of the mouse is required to
 * reach it.
 */
var SHOW_VELOCITY = 10;
/** Distance from the top of the screen required to reveal the toolbars. */
var TOP_TOOLBAR_REVEAL_DISTANCE = 100;
/** Distance from the bottom-right of the screen required to reveal toolbars. */
var SIDE_TOOLBAR_REVEAL_DISTANCE_RIGHT = 150;
var SIDE_TOOLBAR_REVEAL_DISTANCE_BOTTOM = 250;



/**
 * @param {MouseEvent} e Event to test.
 * @return {boolean} True if the mouse is close to the top of the screen.
 */
function isMouseNearTopToolbar(e) {
  return e.y < TOP_TOOLBAR_REVEAL_DISTANCE;
}

/**
 * @param {MouseEvent} e Event to test.
 * @param {Window} window Window to test against.
 * @return {boolean} True if the mouse is close to the bottom-right of the
 * screen.
 */
function isMouseNearSideToolbar(e, window) {
  var atSide = e.x > window.innerWidth - SIDE_TOOLBAR_REVEAL_DISTANCE_RIGHT;
  if (isRTL())
    atSide = e.x < SIDE_TOOLBAR_REVEAL_DISTANCE_RIGHT;
  var atBottom = e.y > window.innerHeight - SIDE_TOOLBAR_REVEAL_DISTANCE_BOTTOM;
  return atSide && atBottom;
}

/**
 * Constructs a Toolbar Manager, responsible for co-ordinating between multiple
 * toolbar elements.
 * @constructor
 * @param {Object} window The window containing the UI.
 * @param {Object} toolbar The top toolbar element.
 * @param {Object} zoomToolbar The zoom toolbar element.
 */
function ToolbarManager(window, toolbar, zoomToolbar) {
  this.window_ = window;
  this.toolbar_ = toolbar;
  this.zoomToolbar_ = zoomToolbar;

  this.toolbarTimeout_ = null;
  this.isMouseNearTopToolbar_ = false;
  this.isMouseNearSideToolbar_ = false;

  this.sideToolbarAllowedOnly_ = false;
  this.sideToolbarAllowedOnlyTimer_ = null;

  this.keyboardNavigationActive = false;

  this.lastMovementTimestamp = null;

  this.window_.addEventListener('resize', this.resizeDropdowns_.bind(this));
  this.resizeDropdowns_();
}

ToolbarManager.prototype = {

  handleMouseMove: function(e) {
    this.isMouseNearTopToolbar_ = this.toolbar_ && isMouseNearTopToolbar(e);
    this.isMouseNearSideToolbar_ = isMouseNearSideToolbar(e, this.window_);

    this.keyboardNavigationActive = false;
    var touchInteractionActive =
        (e.sourceCapabilities && e.sourceCapabilities.firesTouchEvents);

    // Allow the top toolbar to be shown if the mouse moves away from the side
    // toolbar (as long as the timeout has elapsed).
    if (!this.isMouseNearSideToolbar_ && !this.sideToolbarAllowedOnlyTimer_)
      this.sideToolbarAllowedOnly_ = false;

    // Allow the top toolbar to be shown if the mouse moves to the top edge.
    if (this.isMouseNearTopToolbar_)
      this.sideToolbarAllowedOnly_ = false;

    // Tapping the screen with toolbars open tries to close them.
    if (touchInteractionActive && this.zoomToolbar_.isVisible()) {
      this.hideToolbarsIfAllowed();
      return;
    }

    // Show the toolbars if the mouse is near the top or bottom-right of the
    // screen, if the mouse moved fast, or if the touchscreen was tapped.
    if (this.isMouseNearTopToolbar_ || this.isMouseNearSideToolbar_ ||
        this.isHighVelocityMouseMove_(e) || touchInteractionActive) {
      if (this.sideToolbarAllowedOnly_)
        this.zoomToolbar_.show();
      else
        this.showToolbars();
    }
    this.hideToolbarsAfterTimeout();
  },

  /**
   * Whether a mousemove event is high enough velocity to reveal the toolbars.
   * @param {MouseEvent} e Event to test.
   * @return {boolean} true if the event is a high velocity mousemove, false
   * otherwise.
   * @private
   */
  isHighVelocityMouseMove_: function(e) {
    if (e.type == 'mousemove') {
      if (this.lastMovementTimestamp == null) {
        this.lastMovementTimestamp = this.getCurrentTimestamp_();
      } else {
        var movement =
            Math.sqrt(e.movementX * e.movementX + e.movementY * e.movementY);
        var newTime = this.getCurrentTimestamp_();
        var interval = newTime - this.lastMovementTimestamp;
        this.lastMovementTimestamp = newTime;

        if (interval != 0)
          return movement / interval > SHOW_VELOCITY;
      }
    }
    return false;
  },

  /**
   * Wrapper around Date.now() to make it easily replaceable for testing.
   * @return {int}
   * @private
   */
  getCurrentTimestamp_: function() {
    return Date.now();
  },

  /**
   * Display both UI toolbars.
   */
  showToolbars: function() {
    if (this.toolbar_)
      this.toolbar_.show();
    this.zoomToolbar_.show();
  },

  /**
   * Show toolbars and mark that navigation is being performed with
   * tab/shift-tab. This disables toolbar hiding until the mouse is moved or
   * escape is pressed.
   */
  showToolbarsForKeyboardNavigation: function() {
    this.keyboardNavigationActive = true;
    this.showToolbars();
  },

  /**
   * Hide toolbars after a delay, regardless of the position of the mouse.
   * Intended to be called when the mouse has moved out of the parent window.
   */
  hideToolbarsForMouseOut: function() {
    this.isMouseNearTopToolbar_ = false;
    this.isMouseNearSideToolbar_ = false;
    this.hideToolbarsAfterTimeout();
  },

  /**
   * Check if the toolbars are able to be closed, and close them if they are.
   * Toolbars may be kept open based on mouse/keyboard activity and active
   * elements.
   */
  hideToolbarsIfAllowed: function() {
    if (this.isMouseNearSideToolbar_ || this.isMouseNearTopToolbar_)
      return;

    if (this.toolbar_ && this.toolbar_.shouldKeepOpen())
      return;

    if (this.keyboardNavigationActive)
      return;

    // Remove focus to make any visible tooltips disappear -- otherwise they'll
    // still be visible on screen when the toolbar is off screen.
    if ((this.toolbar_ && document.activeElement == this.toolbar_) ||
        document.activeElement == this.zoomToolbar_) {
      document.activeElement.blur();
    }

    if (this.toolbar_)
      this.toolbar_.hide();
    this.zoomToolbar_.hide();
  },

  /**
   * Hide the toolbar after the HIDE_TIMEOUT has elapsed.
   */
  hideToolbarsAfterTimeout: function() {
    if (this.toolbarTimeout_)
      this.window_.clearTimeout(this.toolbarTimeout_);
    this.toolbarTimeout_ = this.window_.setTimeout(
        this.hideToolbarsIfAllowed.bind(this), HIDE_TIMEOUT);
  },

  /**
   * Hide the 'topmost' layer of toolbars. Hides any dropdowns that are open, or
   * hides the basic toolbars otherwise.
   */
  hideSingleToolbarLayer: function() {
    if (!this.toolbar_ || !this.toolbar_.hideDropdowns()) {
      this.keyboardNavigationActive = false;
      this.hideToolbarsIfAllowed();
    }
  },

  /**
   * Hide the top toolbar and keep it hidden until both:
   * - The mouse is moved away from the right side of the screen
   * - 1 second has passed.
   *
   * The top toolbar can be immediately re-opened by moving the mouse to the top
   * of the screen.
   */
  forceHideTopToolbar: function() {
    if (!this.toolbar_)
      return;
    this.toolbar_.hide();
    this.sideToolbarAllowedOnly_ = true;
    this.sideToolbarAllowedOnlyTimer_ = this.window_.setTimeout(function() {
      this.sideToolbarAllowedOnlyTimer_ = null;
    }.bind(this), FORCE_HIDE_TIMEOUT);
  },

  /**
   * Updates the size of toolbar dropdowns based on the positions of the rest of
   * the UI.
   * @private
   */
  resizeDropdowns_: function() {
    if (!this.toolbar_)
      return;
    var lowerBound = this.window_.innerHeight - this.zoomToolbar_.clientHeight;
    this.toolbar_.setDropdownLowerBound(lowerBound);
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Returns the height of the intersection of two rectangles.
 * @param {Object} rect1 the first rect
 * @param {Object} rect2 the second rect
 * @return {number} the height of the intersection of the rects
 */
function getIntersectionHeight(rect1, rect2) {
  return Math.max(0,
      Math.min(rect1.y + rect1.height, rect2.y + rect2.height) -
      Math.max(rect1.y, rect2.y));
}

/**
 * Create a new viewport.
 * @constructor
 * @param {Window} window the window
 * @param {Object} sizer is the element which represents the size of the
 *     document in the viewport
 * @param {Function} viewportChangedCallback is run when the viewport changes
 * @param {Function} beforeZoomCallback is run before a change in zoom
 * @param {Function} afterZoomCallback is run after a change in zoom
 * @param {number} scrollbarWidth the width of scrollbars on the page
 * @param {number} defaultZoom The default zoom level.
 * @param {number} topToolbarHeight The number of pixels that should initially
 *     be left blank above the document for the toolbar.
 */
function Viewport(window,
                  sizer,
                  viewportChangedCallback,
                  beforeZoomCallback,
                  afterZoomCallback,
                  scrollbarWidth,
                  defaultZoom,
                  topToolbarHeight) {
  this.window_ = window;
  this.sizer_ = sizer;
  this.viewportChangedCallback_ = viewportChangedCallback;
  this.beforeZoomCallback_ = beforeZoomCallback;
  this.afterZoomCallback_ = afterZoomCallback;
  this.allowedToChangeZoom_ = false;
  this.zoom_ = 1;
  this.documentDimensions_ = null;
  this.pageDimensions_ = [];
  this.scrollbarWidth_ = scrollbarWidth;
  this.fittingType_ = Viewport.FittingType.NONE;
  this.defaultZoom_ = defaultZoom;
  this.topToolbarHeight_ = topToolbarHeight;

  window.addEventListener('scroll', this.updateViewport_.bind(this));
  window.addEventListener('resize', this.resize_.bind(this));
}

/**
 * Enumeration of page fitting types.
 * @enum {string}
 */
Viewport.FittingType = {
  NONE: 'none',
  FIT_TO_PAGE: 'fit-to-page',
  FIT_TO_WIDTH: 'fit-to-width'
};

/**
 * The increment to scroll a page by in pixels when up/down/left/right arrow
 * keys are pressed. Usually we just let the browser handle scrolling on the
 * window when these keys are pressed but in certain cases we need to simulate
 * these events.
 */
Viewport.SCROLL_INCREMENT = 40;

/**
 * Predefined zoom factors to be used when zooming in/out. These are in
 * ascending order. This should match the list in
 * components/ui/zoom/page_zoom_constants.h
 */
Viewport.ZOOM_FACTORS = [0.25, 0.333, 0.5, 0.666, 0.75, 0.9, 1,
                         1.1, 1.25, 1.5, 1.75, 2, 2.5, 3, 4, 5];

/**
 * The minimum and maximum range to be used to clip zoom factor.
 */
Viewport.ZOOM_FACTOR_RANGE = {
  min: Viewport.ZOOM_FACTORS[0],
  max: Viewport.ZOOM_FACTORS[Viewport.ZOOM_FACTORS.length - 1]
};

/**
 * The width of the page shadow around pages in pixels.
 */
Viewport.PAGE_SHADOW = {top: 3, bottom: 7, left: 5, right: 5};

Viewport.prototype = {
  /**
   * Returns the zoomed and rounded document dimensions for the given zoom.
   * Rounding is necessary when interacting with the renderer which tends to
   * operate in integral values (for example for determining if scrollbars
   * should be shown).
   * @param {number} zoom The zoom to use to compute the scaled dimensions.
   * @return {Object} A dictionary with scaled 'width'/'height' of the document.
   * @private
   */
  getZoomedDocumentDimensions_: function(zoom) {
    if (!this.documentDimensions_)
      return null;
    return {
      width: Math.round(this.documentDimensions_.width * zoom),
      height: Math.round(this.documentDimensions_.height * zoom)
    };
  },

  /**
   * @private
   * Returns true if the document needs scrollbars at the given zoom level.
   * @param {number} zoom compute whether scrollbars are needed at this zoom
   * @return {Object} with 'horizontal' and 'vertical' keys which map to bool
   *     values indicating if the horizontal and vertical scrollbars are needed
   *     respectively.
   */
  documentNeedsScrollbars_: function(zoom) {
    var zoomedDimensions = this.getZoomedDocumentDimensions_(zoom);
    if (!zoomedDimensions) {
      return {
        horizontal: false,
        vertical: false
      };
    }

    // If scrollbars are required for one direction, expand the document in the
    // other direction to take the width of the scrollbars into account when
    // deciding whether the other direction needs scrollbars.
    if (zoomedDimensions.width > this.window_.innerWidth)
      zoomedDimensions.height += this.scrollbarWidth_;
    else if (zoomedDimensions.height > this.window_.innerHeight)
      zoomedDimensions.width += this.scrollbarWidth_;
    return {
      horizontal: zoomedDimensions.width > this.window_.innerWidth,
      vertical: zoomedDimensions.height + this.topToolbarHeight_ >
          this.window_.innerHeight
    };
  },

  /**
   * Returns true if the document needs scrollbars at the current zoom level.
   * @return {Object} with 'x' and 'y' keys which map to bool values
   *     indicating if the horizontal and vertical scrollbars are needed
   *     respectively.
   */
  documentHasScrollbars: function() {
    return this.documentNeedsScrollbars_(this.zoom_);
  },

  /**
   * @private
   * Helper function called when the zoomed document size changes.
   */
  contentSizeChanged_: function() {
    var zoomedDimensions = this.getZoomedDocumentDimensions_(this.zoom_);
    if (zoomedDimensions) {
      this.sizer_.style.width = zoomedDimensions.width + 'px';
      this.sizer_.style.height = zoomedDimensions.height +
          this.topToolbarHeight_ + 'px';
    }
  },

  /**
   * @private
   * Called when the viewport should be updated.
   */
  updateViewport_: function() {
    this.viewportChangedCallback_();
  },

  /**
   * @private
   * Called when the viewport size changes.
   */
  resize_: function() {
    if (this.fittingType_ == Viewport.FittingType.FIT_TO_PAGE)
      this.fitToPageInternal_(false);
    else if (this.fittingType_ == Viewport.FittingType.FIT_TO_WIDTH)
      this.fitToWidth();
    else
      this.updateViewport_();
  },

  /**
   * @type {Object} the scroll position of the viewport.
   */
  get position() {
    return {
      x: this.window_.pageXOffset,
      y: this.window_.pageYOffset - this.topToolbarHeight_
    };
  },

  /**
   * Scroll the viewport to the specified position.
   * @type {Object} position the position to scroll to.
   */
  set position(position) {
    this.window_.scrollTo(position.x, position.y + this.topToolbarHeight_);
  },

  /**
   * @type {Object} the size of the viewport excluding scrollbars.
   */
  get size() {
    var needsScrollbars = this.documentNeedsScrollbars_(this.zoom_);
    var scrollbarWidth = needsScrollbars.vertical ? this.scrollbarWidth_ : 0;
    var scrollbarHeight = needsScrollbars.horizontal ? this.scrollbarWidth_ : 0;
    return {
      width: this.window_.innerWidth - scrollbarWidth,
      height: this.window_.innerHeight - scrollbarHeight
    };
  },

  /**
   * @type {number} the zoom level of the viewport.
   */
  get zoom() {
    return this.zoom_;
  },

  /**
   * @private
   * Used to wrap a function that might perform zooming on the viewport. This is
   * required so that we can notify the plugin that zooming is in progress
   * so that while zooming is taking place it can stop reacting to scroll events
   * from the viewport. This is to avoid flickering.
   */
  mightZoom_: function(f) {
    this.beforeZoomCallback_();
    this.allowedToChangeZoom_ = true;
    f();
    this.allowedToChangeZoom_ = false;
    this.afterZoomCallback_();
  },

  /**
   * @private
   * Sets the zoom of the viewport.
   * @param {number} newZoom the zoom level to zoom to.
   */
  setZoomInternal_: function(newZoom) {
    if (!this.allowedToChangeZoom_) {
      throw 'Called Viewport.setZoomInternal_ without calling ' +
            'Viewport.mightZoom_.';
    }
    // Record the scroll position (relative to the top-left of the window).
    var currentScrollPos = {
      x: this.position.x / this.zoom_,
      y: this.position.y / this.zoom_
    };
    this.zoom_ = newZoom;
    this.contentSizeChanged_();
    // Scroll to the scaled scroll position.
    this.position = {
      x: currentScrollPos.x * newZoom,
      y: currentScrollPos.y * newZoom
    };
  },

  /**
   * Sets the zoom to the given zoom level.
   * @param {number} newZoom the zoom level to zoom to.
   */
  setZoom: function(newZoom) {
    this.fittingType_ = Viewport.FittingType.NONE;
    newZoom = Math.max(Viewport.ZOOM_FACTOR_RANGE.min,
                       Math.min(newZoom, Viewport.ZOOM_FACTOR_RANGE.max));
    this.mightZoom_(function() {
      this.setZoomInternal_(newZoom);
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * @type {number} the width of scrollbars in the viewport in pixels.
   */
  get scrollbarWidth() {
    return this.scrollbarWidth_;
  },

  /**
   * @type {Viewport.FittingType} the fitting type the viewport is currently in.
   */
  get fittingType() {
    return this.fittingType_;
  },

  /**
   * @private
   * @param {integer} y the y-coordinate to get the page at.
   * @return {integer} the index of a page overlapping the given y-coordinate.
   */
  getPageAtY_: function(y) {
    var min = 0;
    var max = this.pageDimensions_.length - 1;
    while (max >= min) {
      var page = Math.floor(min + ((max - min) / 2));
      // There might be a gap between the pages, in which case use the bottom
      // of the previous page as the top for finding the page.
      var top = 0;
      if (page > 0) {
        top = this.pageDimensions_[page - 1].y +
            this.pageDimensions_[page - 1].height;
      }
      var bottom = this.pageDimensions_[page].y +
          this.pageDimensions_[page].height;

      if (top <= y && bottom > y)
        return page;
      else if (top > y)
        max = page - 1;
      else
        min = page + 1;
    }
    return 0;
  },

  /**
   * Returns the page with the greatest proportion of its height in the current
   * viewport.
   * @return {int} the index of the most visible page.
   */
  getMostVisiblePage: function() {
    var firstVisiblePage = this.getPageAtY_(this.position.y / this.zoom_);
    if (firstVisiblePage == this.pageDimensions_.length - 1)
      return firstVisiblePage;

    var viewportRect = {
      x: this.position.x / this.zoom_,
      y: this.position.y / this.zoom_,
      width: this.size.width / this.zoom_,
      height: this.size.height / this.zoom_
    };
    var firstVisiblePageVisibility = getIntersectionHeight(
        this.pageDimensions_[firstVisiblePage], viewportRect) /
        this.pageDimensions_[firstVisiblePage].height;
    var nextPageVisibility = getIntersectionHeight(
        this.pageDimensions_[firstVisiblePage + 1], viewportRect) /
        this.pageDimensions_[firstVisiblePage + 1].height;
    if (nextPageVisibility > firstVisiblePageVisibility)
      return firstVisiblePage + 1;
    return firstVisiblePage;
  },

  /**
   * @private
   * Compute the zoom level for fit-to-page or fit-to-width. |pageDimensions| is
   * the dimensions for a given page and if |widthOnly| is true, it indicates
   * that fit-to-page zoom should be computed rather than fit-to-page.
   * @param {Object} pageDimensions the dimensions of a given page
   * @param {boolean} widthOnly a bool indicating whether fit-to-page or
   *     fit-to-width should be computed.
   * @return {number} the zoom to use
   */
  computeFittingZoom_: function(pageDimensions, widthOnly) {
    // First compute the zoom without scrollbars.
    var zoomWidth = this.window_.innerWidth / pageDimensions.width;
    var zoom;
    var zoomHeight;
    if (widthOnly) {
      zoom = zoomWidth;
    } else {
      zoomHeight = this.window_.innerHeight / pageDimensions.height;
      zoom = Math.min(zoomWidth, zoomHeight);
    }
    // Check if there needs to be any scrollbars.
    var needsScrollbars = this.documentNeedsScrollbars_(zoom);

    // If the document fits, just return the zoom.
    if (!needsScrollbars.horizontal && !needsScrollbars.vertical)
      return zoom;

    var zoomedDimensions = this.getZoomedDocumentDimensions_(zoom);

    // Check if adding a scrollbar will result in needing the other scrollbar.
    var scrollbarWidth = this.scrollbarWidth_;
    if (needsScrollbars.horizontal &&
        zoomedDimensions.height > this.window_.innerHeight - scrollbarWidth) {
      needsScrollbars.vertical = true;
    }
    if (needsScrollbars.vertical &&
        zoomedDimensions.width > this.window_.innerWidth - scrollbarWidth) {
      needsScrollbars.horizontal = true;
    }

    // Compute available window space.
    var windowWithScrollbars = {
      width: this.window_.innerWidth,
      height: this.window_.innerHeight
    };
    if (needsScrollbars.horizontal)
      windowWithScrollbars.height -= scrollbarWidth;
    if (needsScrollbars.vertical)
      windowWithScrollbars.width -= scrollbarWidth;

    // Recompute the zoom.
    zoomWidth = windowWithScrollbars.width / pageDimensions.width;
    if (widthOnly) {
      zoom = zoomWidth;
    } else {
      zoomHeight = windowWithScrollbars.height / pageDimensions.height;
      zoom = Math.min(zoomWidth, zoomHeight);
    }
    return zoom;
  },

  /**
   * Zoom the viewport so that the page-width consumes the entire viewport.
   */
  fitToWidth: function() {
    this.mightZoom_(function() {
      this.fittingType_ = Viewport.FittingType.FIT_TO_WIDTH;
      if (!this.documentDimensions_)
        return;
      // When computing fit-to-width, the maximum width of a page in the
      // document is used, which is equal to the size of the document width.
      this.setZoomInternal_(this.computeFittingZoom_(this.documentDimensions_,
                                                     true));
      var page = this.getMostVisiblePage();
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * @private
   * Zoom the viewport so that a page consumes the entire viewport.
   * @param {boolean} scrollToTopOfPage Set to true if the viewport should be
   *     scrolled to the top of the current page. Set to false if the viewport
   *     should remain at the current scroll position.
   */
  fitToPageInternal_: function(scrollToTopOfPage) {
    this.mightZoom_(function() {
      this.fittingType_ = Viewport.FittingType.FIT_TO_PAGE;
      if (!this.documentDimensions_)
        return;
      var page = this.getMostVisiblePage();
      // Fit to the current page's height and the widest page's width.
      var dimensions = {
        width: this.documentDimensions_.width,
        height: this.pageDimensions_[page].height,
      };
      this.setZoomInternal_(this.computeFittingZoom_(dimensions, false));
      if (scrollToTopOfPage) {
        this.position = {
          x: 0,
          y: this.pageDimensions_[page].y * this.zoom_
        };
      }
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * Zoom the viewport so that a page consumes the entire viewport. Also scrolls
   * the viewport to the top of the current page.
   */
  fitToPage: function() {
    this.fitToPageInternal_(true);
  },

  /**
   * Zoom out to the next predefined zoom level.
   */
  zoomOut: function() {
    this.mightZoom_(function() {
      this.fittingType_ = Viewport.FittingType.NONE;
      var nextZoom = Viewport.ZOOM_FACTORS[0];
      for (var i = 0; i < Viewport.ZOOM_FACTORS.length; i++) {
        if (Viewport.ZOOM_FACTORS[i] < this.zoom_)
          nextZoom = Viewport.ZOOM_FACTORS[i];
      }
      this.setZoomInternal_(nextZoom);
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * Zoom in to the next predefined zoom level.
   */
  zoomIn: function() {
    this.mightZoom_(function() {
      this.fittingType_ = Viewport.FittingType.NONE;
      var nextZoom = Viewport.ZOOM_FACTORS[Viewport.ZOOM_FACTORS.length - 1];
      for (var i = Viewport.ZOOM_FACTORS.length - 1; i >= 0; i--) {
        if (Viewport.ZOOM_FACTORS[i] > this.zoom_)
          nextZoom = Viewport.ZOOM_FACTORS[i];
      }
      this.setZoomInternal_(nextZoom);
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * Go to the given page index.
   * @param {number} page the index of the page to go to. zero-based.
   */
  goToPage: function(page) {
    this.mightZoom_(function() {
      if (this.pageDimensions_.length === 0)
        return;
      if (page < 0)
        page = 0;
      if (page >= this.pageDimensions_.length)
        page = this.pageDimensions_.length - 1;
      var dimensions = this.pageDimensions_[page];
      var toolbarOffset = 0;
      // Unless we're in fit to page mode, scroll above the page by
      // |this.topToolbarHeight_| so that the toolbar isn't covering it
      // initially.
      if (this.fittingType_ != Viewport.FittingType.FIT_TO_PAGE)
        toolbarOffset = this.topToolbarHeight_;
      this.position = {
        x: dimensions.x * this.zoom_,
        y: dimensions.y * this.zoom_ - toolbarOffset
      };
      this.updateViewport_();
    }.bind(this));
  },

  /**
   * Set the dimensions of the document.
   * @param {Object} documentDimensions the dimensions of the document
   */
  setDocumentDimensions: function(documentDimensions) {
    this.mightZoom_(function() {
      var initialDimensions = !this.documentDimensions_;
      this.documentDimensions_ = documentDimensions;
      this.pageDimensions_ = this.documentDimensions_.pageDimensions;
      if (initialDimensions) {
        this.setZoomInternal_(
            Math.min(this.defaultZoom_,
                     this.computeFittingZoom_(this.documentDimensions_, true)));
        this.position = {
          x: 0,
          y: -this.topToolbarHeight_
        };
      }
      this.contentSizeChanged_();
      this.resize_();
    }.bind(this));
  },

  /**
   * Get the coordinates of the page contents (excluding the page shadow)
   * relative to the screen.
   * @param {number} page the index of the page to get the rect for.
   * @return {Object} a rect representing the page in screen coordinates.
   */
  getPageScreenRect: function(page) {
    if (!this.documentDimensions_) {
      return {
        x: 0,
        y: 0,
        width: 0,
        height: 0
      };
    }
    if (page >= this.pageDimensions_.length)
      page = this.pageDimensions_.length - 1;

    var pageDimensions = this.pageDimensions_[page];

    // Compute the page dimensions minus the shadows.
    var insetDimensions = {
      x: pageDimensions.x + Viewport.PAGE_SHADOW.left,
      y: pageDimensions.y + Viewport.PAGE_SHADOW.top,
      width: pageDimensions.width - Viewport.PAGE_SHADOW.left -
          Viewport.PAGE_SHADOW.right,
      height: pageDimensions.height - Viewport.PAGE_SHADOW.top -
          Viewport.PAGE_SHADOW.bottom
    };

    // Compute the x-coordinate of the page within the document.
    // TODO(raymes): This should really be set when the PDF plugin passes the
    // page coordinates, but it isn't yet.
    var x = (this.documentDimensions_.width - pageDimensions.width) / 2 +
        Viewport.PAGE_SHADOW.left;
    // Compute the space on the left of the document if the document fits
    // completely in the screen.
    var spaceOnLeft = (this.size.width -
        this.documentDimensions_.width * this.zoom_) / 2;
    spaceOnLeft = Math.max(spaceOnLeft, 0);

    return {
      x: x * this.zoom_ + spaceOnLeft - this.window_.pageXOffset,
      y: insetDimensions.y * this.zoom_ - this.window_.pageYOffset,
      width: insetDimensions.width * this.zoom_,
      height: insetDimensions.height * this.zoom_
    };
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Creates a new OpenPDFParamsParser. This parses the open pdf parameters
 * passed in the url to set initial viewport settings for opening the pdf.
 * @param {Object} getNamedDestinationsFunction The function called to fetch
 *     the page number for a named destination.
 */
function OpenPDFParamsParser(getNamedDestinationsFunction) {
  this.outstandingRequests_ = [];
  this.getNamedDestinationsFunction_ = getNamedDestinationsFunction;
}

OpenPDFParamsParser.prototype = {
  /**
   * @private
   * Parse zoom parameter of open PDF parameters. If this
   * parameter is passed while opening PDF then PDF should be opened
   * at the specified zoom level.
   * @param {number} zoom value.
   * @param {Object} viewportPosition to store zoom and position value.
   */
  parseZoomParam_: function(paramValue, viewportPosition) {
    var paramValueSplit = paramValue.split(',');
    if ((paramValueSplit.length != 1) && (paramValueSplit.length != 3))
      return;

    // User scale of 100 means zoom value of 100% i.e. zoom factor of 1.0.
    var zoomFactor = parseFloat(paramValueSplit[0]) / 100;
    if (isNaN(zoomFactor))
      return;

    // Handle #zoom=scale.
    if (paramValueSplit.length == 1) {
      viewportPosition['zoom'] = zoomFactor;
      return;
    }

    // Handle #zoom=scale,left,top.
    var position = {x: parseFloat(paramValueSplit[1]),
                    y: parseFloat(paramValueSplit[2])};
    viewportPosition['position'] = position;
    viewportPosition['zoom'] = zoomFactor;
  },

  /**
   * Parse the parameters encoded in the fragment of a URL into a dictionary.
   * @private
   * @param {string} url to parse
   * @return {Object} Key-value pairs of URL parameters
   */
  parseUrlParams_: function(url) {
    var params = {};

    var paramIndex = url.search('#');
    if (paramIndex == -1)
      return params;

    var paramTokens = url.substring(paramIndex + 1).split('&');
    if ((paramTokens.length == 1) && (paramTokens[0].search('=') == -1)) {
      // Handle the case of http://foo.com/bar#NAMEDDEST. This is not
      // explicitly mentioned except by example in the Adobe
      // "PDF Open Parameters" document.
      params['nameddest'] = paramTokens[0];
      return params;
    }

    for (var i = 0; i < paramTokens.length; ++i) {
      var keyValueSplit = paramTokens[i].split('=');
      if (keyValueSplit.length != 2)
        continue;
      params[keyValueSplit[0]] = keyValueSplit[1];
    }

    return params;
  },

  /**
   * Parse PDF url parameters used for controlling the state of UI. These need
   * to be available when the UI is being initialized, rather than when the PDF
   * is finished loading.
   * @param {string} url that needs to be parsed.
   * @return {Object} parsed url parameters.
   */
  getUiUrlParams: function(url) {
    var params = this.parseUrlParams_(url);
    var uiParams = {toolbar: true};

    if ('toolbar' in params && params['toolbar'] == 0)
      uiParams.toolbar = false;

    return uiParams;
  },

  /**
   * Parse PDF url parameters. These parameters are mentioned in the url
   * and specify actions to be performed when opening pdf files.
   * See http://www.adobe.com/content/dam/Adobe/en/devnet/acrobat/
   * pdfs/pdf_open_parameters.pdf for details.
   * @param {string} url that needs to be parsed.
   * @param {Function} callback function to be called with viewport info.
   */
  getViewportFromUrlParams: function(url, callback) {
    var viewportPosition = {};
    viewportPosition['url'] = url;

    var paramsDictionary = this.parseUrlParams_(url);

    if ('page' in paramsDictionary) {
      // |pageNumber| is 1-based, but goToPage() take a zero-based page number.
      var pageNumber = parseInt(paramsDictionary['page']);
      if (!isNaN(pageNumber) && pageNumber > 0)
        viewportPosition['page'] = pageNumber - 1;
    }

    if ('zoom' in paramsDictionary)
      this.parseZoomParam_(paramsDictionary['zoom'], viewportPosition);

    if (viewportPosition.page === undefined &&
        'nameddest' in paramsDictionary) {
      this.outstandingRequests_.push({
        callback: callback,
        viewportPosition: viewportPosition
      });
      this.getNamedDestinationsFunction_(paramsDictionary['nameddest']);
    } else {
      callback(viewportPosition);
    }
  },

  /**
   * This is called when a named destination is received and the page number
   * corresponding to the request for which a named destination is passed.
   * @param {number} pageNumber The page corresponding to the named destination
   *    requested.
   */
  onNamedDestinationReceived: function(pageNumber) {
    var outstandingRequest = this.outstandingRequests_.shift();
    if (pageNumber != -1)
      outstandingRequest.viewportPosition.page = pageNumber;
    outstandingRequest.callback(outstandingRequest.viewportPosition);
  },
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Creates a new Navigator for navigating to links inside or outside the PDF.
 * @param {string} originalUrl The original page URL.
 * @param {Object} viewport The viewport info of the page.
 * @param {Object} paramsParser The object for URL parsing.
 * @param {Function} navigateInCurrentTabCallback The Callback function that
 *    gets called when navigation happens in the current tab.
 * @param {Function} navigateInNewTabCallback The Callback function that gets
 *    called when navigation happens in the new tab.
 */
function Navigator(originalUrl,
                   viewport,
                   paramsParser,
                   navigateInCurrentTabCallback,
                   navigateInNewTabCallback) {
  this.originalUrl_ = originalUrl;
  this.viewport_ = viewport;
  this.paramsParser_ = paramsParser;
  this.navigateInCurrentTabCallback_ = navigateInCurrentTabCallback;
  this.navigateInNewTabCallback_ = navigateInNewTabCallback;
}

Navigator.prototype = {
  /**
   * @private
   * Function to navigate to the given URL. This might involve navigating
   * within the PDF page or opening a new url (in the same tab or a new tab).
   * @param {string} url The URL to navigate to.
   * @param {boolean} newTab Whether to perform the navigation in a new tab or
   *    in the current tab.
   */
  navigate: function(url, newTab) {
    if (url.length == 0)
      return;

    // If |urlFragment| starts with '#', then it's for the same URL with a
    // different URL fragment.
    if (url.charAt(0) == '#') {
      // if '#' is already present in |originalUrl| then remove old fragment
      // and add new url fragment.
      var hashIndex = this.originalUrl_.search('#');
      if (hashIndex != -1)
        url = this.originalUrl_.substring(0, hashIndex) + url;
      else
        url = this.originalUrl_ + url;
    }

    // If there's no scheme, then take a guess at the scheme.
    if (url.indexOf('://') == -1 && url.indexOf('mailto:') == -1)
      url = this.guessUrlWithoutScheme_(url);

    if (!this.isValidUrl_(url))
      return;

    if (newTab) {
      this.navigateInNewTabCallback_(url);
    } else {
      this.paramsParser_.getViewportFromUrlParams(
          url, this.onViewportReceived_.bind(this));
    }
  },

  /**
   * @private
   * Called when the viewport position is received.
   * @param {Object} viewportPosition Dictionary containing the viewport
   *    position.
   */
  onViewportReceived_: function(viewportPosition) {
    var originalUrl = this.originalUrl_;
    var hashIndex = originalUrl.search('#');
    if (hashIndex != -1)
      originalUrl = originalUrl.substring(0, hashIndex);

    var newUrl = viewportPosition.url;
    hashIndex = newUrl.search('#');
    if (hashIndex != -1)
      newUrl = newUrl.substring(0, hashIndex);

    var pageNumber = viewportPosition.page;
    if (pageNumber != undefined && originalUrl == newUrl)
      this.viewport_.goToPage(pageNumber);
    else
      this.navigateInCurrentTabCallback_(viewportPosition.url);
  },

  /**
   * @private
   * Checks if the URL starts with a scheme and s not just a scheme.
   * @param {string} The input URL
   * @return {boolean} Whether the url is valid.
   */
  isValidUrl_: function(url) {
    // Make sure |url| starts with a valid scheme.
    if (url.indexOf('http://') != 0 &&
        url.indexOf('https://') != 0 &&
        url.indexOf('ftp://') != 0 &&
        url.indexOf('file://') != 0 &&
        url.indexOf('mailto:') != 0) {
      return false;
    }

    // Make sure |url| is not only a scheme.
    if (url == 'http://' ||
        url == 'https://' ||
        url == 'ftp://' ||
        url == 'file://' ||
        url == 'mailto:') {
      return false;
    }

    return true;
  },

  /**
   * @private
   * Attempt to figure out what a URL is when there is no scheme.
   * @param {string} The input URL
   * @return {string} The URL with a scheme or the original URL if it is not
   *     possible to determine the scheme.
   */
  guessUrlWithoutScheme_: function(url) {
    // If the original URL is mailto:, that does not make sense to start with,
    // and neither does adding |url| to it.
    // If the original URL is not a valid URL, this cannot make a valid URL.
    // In both cases, just bail out.
    if (this.originalUrl_.startsWith('mailto:') ||
        !this.isValidUrl_(this.originalUrl_)) {
      return url;
    }

    // Check for absolute paths.
    if (url.startsWith('/')) {
      var schemeEndIndex = this.originalUrl_.indexOf('://');
      var firstSlash = this.originalUrl_.indexOf('/', schemeEndIndex + 3);
      // e.g. http://www.foo.com/bar -> http://www.foo.com
      var domain = firstSlash != -1 ?
          this.originalUrl_.substr(0, firstSlash) : this.originalUrl_;
      return domain + url;
    }

    // Check for obvious relative paths.
    var isRelative = false;
    if (url.startsWith('.') || url.startsWith('\\'))
      isRelative = true;

    // In Adobe Acrobat Reader XI, it looks as though links with less than
    // 2 dot separators in the domain are considered relative links, and
    // those with 2 of more are considered http URLs. e.g.
    //
    // www.foo.com/bar -> http
    // foo.com/bar -> relative link
    if (!isRelative) {
      var domainSeparatorIndex = url.indexOf('/');
      var domainName = domainSeparatorIndex == -1 ?
          url : url.substr(0, domainSeparatorIndex);
      var domainDotCount = (domainName.match(/\./g) || []).length;
      if (domainDotCount < 2)
        isRelative = true;
    }

    if (isRelative) {
      var slashIndex = this.originalUrl_.lastIndexOf('/');
      var path = slashIndex != -1 ?
          this.originalUrl_.substr(0, slashIndex) : this.originalUrl_;
      return path + '/' + url;
    }

    return 'http://' + url;
  }
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * @private
 * The period of time in milliseconds to wait between updating the viewport
 * position by the scroll velocity.
 */
ViewportScroller.DRAG_TIMER_INTERVAL_MS_ = 100;

/**
 * @private
 * The maximum drag scroll distance per DRAG_TIMER_INTERVAL in pixels.
 */
ViewportScroller.MAX_DRAG_SCROLL_DISTANCE_ = 100;

/**
 * Creates a new ViewportScroller.
 * A ViewportScroller scrolls the page in response to drag selection with the
 * mouse.
 * @param {Object} viewport The viewport info of the page.
 * @param {Object} plugin The PDF plugin element.
 * @param {Object} window The window containing the viewer.
 */
function ViewportScroller(viewport, plugin, window) {
  this.viewport_ = viewport;
  this.plugin_ = plugin;
  this.window_ = window;
  this.mousemoveCallback_ = null;
  this.timerId_ = null;
  this.scrollVelocity_ = null;
  this.lastFrameTime_ = 0;
}

ViewportScroller.prototype = {
  /**
   * @private
   * Start scrolling the page by |scrollVelocity_| every
   * |DRAG_TIMER_INTERVAL_MS_|.
   */
  startDragScrollTimer_: function() {
    if (this.timerId_ === null) {
      this.timerId_ =
          this.window_.setInterval(this.dragScrollPage_.bind(this),
                                   ViewportScroller.DRAG_TIMER_INTERVAL_MS_);
      this.lastFrameTime_ = Date.now();
    }
  },

  /**
   * @private
   * Stops the drag scroll timer if it is active.
   */
  stopDragScrollTimer_: function() {
    if (this.timerId_ !== null) {
      this.window_.clearInterval(this.timerId_);
      this.timerId_ = null;
      this.lastFrameTime_ = 0;
    }
  },

  /**
   * @private
   * Scrolls the viewport by the current scroll velocity.
   */
  dragScrollPage_: function() {
    var position = this.viewport_.position;
    var currentFrameTime = Date.now();
    var timeAdjustment = (currentFrameTime - this.lastFrameTime_) /
                         ViewportScroller.DRAG_TIMER_INTERVAL_MS_;
    position.y += (this.scrollVelocity_.y * timeAdjustment);
    position.x += (this.scrollVelocity_.x * timeAdjustment);
    this.viewport_.position = position;
    this.lastFrameTime_ = currentFrameTime;
  },

  /**
   * @private
   * Calculate the velocity to scroll while dragging using the distance of the
   * cursor outside the viewport.
   * @param {Object} event The mousemove event.
   * @return {Object} Object with x and y direction scroll velocity.
   */
  calculateVelocity_: function(event) {
    var x = Math.min(Math.max(-event.offsetX,
                              event.offsetX - this.plugin_.offsetWidth, 0),
                     ViewportScroller.MAX_DRAG_SCROLL_DISTANCE_) *
            Math.sign(event.offsetX);
    var y = Math.min(Math.max(-event.offsetY,
                              event.offsetY - this.plugin_.offsetHeight, 0),
                     ViewportScroller.MAX_DRAG_SCROLL_DISTANCE_) *
            Math.sign(event.offsetY);
    return {
      x: x,
      y: y
    };
  },

  /**
   * @private
   * Handles mousemove events. It updates the scroll velocity and starts and
   * stops timer based on scroll velocity.
   * @param {Object} event The mousemove event.
   */
  onMousemove_: function(event) {
    this.scrollVelocity_ = this.calculateVelocity_(event);
    if (!this.scrollVelocity_.x && !this.scrollVelocity_.y)
      this.stopDragScrollTimer_();
    else if (!this.timerId_)
      this.startDragScrollTimer_();
  },

  /**
   * Sets whether to scroll the viewport when the mouse is outside the
   * viewport.
   * @param {boolean} isSelecting Represents selection status.
   */
  setEnableScrolling: function(isSelecting) {
    if (isSelecting) {
      if (!this.mousemoveCallback_)
        this.mousemoveCallback_ = this.onMousemove_.bind(this);
      this.plugin_.addEventListener('mousemove', this.mousemoveCallback_,
                                    false);
    } else {
      this.stopDragScrollTimer_();
      if (this.mousemoveCallback_) {
        this.plugin_.removeEventListener('mousemove', this.mousemoveCallback_,
                                         false);
      }
    }
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Turn a dictionary received from postMessage into a key event.
 * @param {Object} dict A dictionary representing the key event.
 * @return {Event} A key event.
 */
function DeserializeKeyEvent(dict) {
  var e = document.createEvent('Event');
  e.initEvent('keydown');
  e.keyCode = dict.keyCode;
  e.shiftKey = dict.shiftKey;
  e.ctrlKey = dict.ctrlKey;
  e.altKey = dict.altKey;
  e.metaKey = dict.metaKey;
  e.fromScriptingAPI = true;
  return e;
}

/**
 * Turn a key event into a dictionary which can be sent over postMessage.
 * @param {Event} event A key event.
 * @return {Object} A dictionary representing the key event.
 */
function SerializeKeyEvent(event) {
  return {
    keyCode: event.keyCode,
    shiftKey: event.shiftKey,
    ctrlKey: event.ctrlKey,
    altKey: event.altKey,
    metaKey: event.metaKey
  };
}

/**
 * An enum containing a value specifying whether the PDF is currently loading,
 * has finished loading or failed to load.
 */
var LoadState = {
  LOADING: 'loading',
  SUCCESS: 'success',
  FAILED: 'failed'
};

/**
 * Create a new PDFScriptingAPI. This provides a scripting interface to
 * the PDF viewer so that it can be customized by things like print preview.
 * @param {Window} window the window of the page containing the pdf viewer.
 * @param {Object} plugin the plugin element containing the pdf viewer.
 */
function PDFScriptingAPI(window, plugin) {
  this.loadState_ = LoadState.LOADING;
  this.pendingScriptingMessages_ = [];
  this.setPlugin(plugin);

  window.addEventListener('message', function(event) {
    if (event.origin != 'chrome-extension://mhjfbmdgcfjbbpaeojofohoefgiehjai' &&
        event.origin != 'chrome://print') {
      console.error('Received message that was not from the extension: ' +
                    event);
      return;
    }
    switch (event.data.type) {
      case 'viewport':
        if (this.viewportChangedCallback_)
          this.viewportChangedCallback_(event.data.pageX,
                                        event.data.pageY,
                                        event.data.pageWidth,
                                        event.data.viewportWidth,
                                        event.data.viewportHeight);
        break;
      case 'documentLoaded':
        this.loadState_ = event.data.load_state;
        if (this.loadCallback_)
          this.loadCallback_(this.loadState_ == LoadState.SUCCESS);
        break;
      case 'getAccessibilityJSONReply':
        if (this.accessibilityCallback_) {
          this.accessibilityCallback_(event.data.json);
          this.accessibilityCallback_ = null;
        }
        break;
      case 'getSelectedTextReply':
        if (this.selectedTextCallback_) {
          this.selectedTextCallback_(event.data.selectedText);
          this.selectedTextCallback_ = null;
        }
        break;
      case 'sendKeyEvent':
        if (this.keyEventCallback_)
          this.keyEventCallback_(DeserializeKeyEvent(event.data.keyEvent));
        break;
    }
  }.bind(this), false);
}

PDFScriptingAPI.prototype = {
  /**
   * @private
   * Send a message to the extension. If messages try to get sent before there
   * is a plugin element set, then we queue them up and send them later (this
   * can happen in print preview).
   * @param {Object} message The message to send.
   */
  sendMessage_: function(message) {
    if (this.plugin_)
      this.plugin_.postMessage(message, '*');
    else
      this.pendingScriptingMessages_.push(message);
  },

 /**
  * Sets the plugin element containing the PDF viewer. The element will usually
  * be passed into the PDFScriptingAPI constructor but may also be set later.
  * @param {Object} plugin the plugin element containing the PDF viewer.
  */
  setPlugin: function(plugin) {
    this.plugin_ = plugin;

    if (this.plugin_) {
      // Send a message to ensure the postMessage channel is initialized which
      // allows us to receive messages.
      this.sendMessage_({
        type: 'initialize'
      });
      // Flush pending messages.
      while (this.pendingScriptingMessages_.length > 0)
        this.sendMessage_(this.pendingScriptingMessages_.shift());
    }
  },

  /**
   * Sets the callback which will be run when the PDF viewport changes.
   * @param {Function} callback the callback to be called.
   */
  setViewportChangedCallback: function(callback) {
    this.viewportChangedCallback_ = callback;
  },

  /**
   * Sets the callback which will be run when the PDF document has finished
   * loading. If the document is already loaded, it will be run immediately.
   * @param {Function} callback the callback to be called.
   */
  setLoadCallback: function(callback) {
    this.loadCallback_ = callback;
    if (this.loadState_ != LoadState.LOADING && this.loadCallback_)
      this.loadCallback_(this.loadState_ == LoadState.SUCCESS);
  },

  /**
   * Sets a callback that gets run when a key event is fired in the PDF viewer.
   * @param {Function} callback the callback to be called with a key event.
   */
  setKeyEventCallback: function(callback) {
    this.keyEventCallback_ = callback;
  },

  /**
   * Resets the PDF viewer into print preview mode.
   * @param {string} url the url of the PDF to load.
   * @param {boolean} grayscale whether or not to display the PDF in grayscale.
   * @param {Array<number>} pageNumbers an array of the page numbers.
   * @param {boolean} modifiable whether or not the document is modifiable.
   */
  resetPrintPreviewMode: function(url, grayscale, pageNumbers, modifiable) {
    this.loadState_ = LoadState.LOADING;
    this.sendMessage_({
      type: 'resetPrintPreviewMode',
      url: url,
      grayscale: grayscale,
      pageNumbers: pageNumbers,
      modifiable: modifiable
    });
  },

  /**
   * Load a page into the document while in print preview mode.
   * @param {string} url the url of the pdf page to load.
   * @param {number} index the index of the page to load.
   */
  loadPreviewPage: function(url, index) {
    this.sendMessage_({
      type: 'loadPreviewPage',
      url: url,
      index: index
    });
  },

  /**
   * Get accessibility JSON for the document. May only be called after document
   * load.
   * @param {Function} callback a callback to be called with the accessibility
   *     json that has been retrieved.
   * @param {number} [page] the 0-indexed page number to get accessibility data
   *     for. If this is not provided, data about the entire document is
   *     returned.
   * @return {boolean} true if the function is successful, false if there is an
   *     outstanding request for accessibility data that has not been answered.
   */
  getAccessibilityJSON: function(callback, page) {
    if (this.accessibilityCallback_)
      return false;
    this.accessibilityCallback_ = callback;
    var message = {
      type: 'getAccessibilityJSON',
    };
    if (page || page == 0)
      message.page = page;
    this.sendMessage_(message);
    return true;
  },

  /**
   * Select all the text in the document. May only be called after document
   * load.
   */
  selectAll: function() {
    this.sendMessage_({
      type: 'selectAll'
    });
  },

  /**
   * Get the selected text in the document. The callback will be called with the
   * text that is selected. May only be called after document load.
   * @param {Function} callback a callback to be called with the selected text.
   * @return {boolean} true if the function is successful, false if there is an
   *     outstanding request for selected text that has not been answered.
   */
  getSelectedText: function(callback) {
    if (this.selectedTextCallback_)
      return false;
    this.selectedTextCallback_ = callback;
    this.sendMessage_({
      type: 'getSelectedText'
    });
    return true;
  },

  /**
   * Print the document. May only be called after document load.
   */
  print: function() {
    this.sendMessage_({
      type: 'print'
    });
  },

  /**
   * Send a key event to the extension.
   * @param {Event} keyEvent the key event to send to the extension.
   */
  sendKeyEvent: function(keyEvent) {
    this.sendMessage_({
      type: 'sendKeyEvent',
      keyEvent: SerializeKeyEvent(keyEvent)
    });
  },
};

/**
 * Creates a PDF viewer with a scripting interface. This is basically 1) an
 * iframe which is navigated to the PDF viewer extension and 2) a scripting
 * interface which provides access to various features of the viewer for use
 * by print preview and accessibility.
 * @param {string} src the source URL of the PDF to load initially.
 * @return {HTMLIFrameElement} the iframe element containing the PDF viewer.
 */
function PDFCreateOutOfProcessPlugin(src) {
  var client = new PDFScriptingAPI(window);
  var iframe = window.document.createElement('iframe');
  iframe.setAttribute('src', 'pdf_preview.html?' + src);
  // Prevent the frame from being tab-focusable.
  iframe.setAttribute('tabindex', '-1');

  iframe.onload = function() {
    client.setPlugin(iframe.contentWindow);
  };

  // Add the functions to the iframe so that they can be called directly.
  iframe.setViewportChangedCallback =
      client.setViewportChangedCallback.bind(client);
  iframe.setLoadCallback = client.setLoadCallback.bind(client);
  iframe.setKeyEventCallback = client.setKeyEventCallback.bind(client);
  iframe.resetPrintPreviewMode = client.resetPrintPreviewMode.bind(client);
  iframe.loadPreviewPage = client.loadPreviewPage.bind(client);
  iframe.sendKeyEvent = client.sendKeyEvent.bind(client);
  return iframe;
}
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * A class that manages updating the browser with zoom changes.
 */
class ZoomManager {
  /**
   * Constructs a ZoomManager
   * @param {!Viewport} viewport A Viewport for which to manage zoom.
   * @param {Function} setBrowserZoomFunction A function that sets the browser
   *     zoom to the provided value.
   * @param {number} initialZoom The initial browser zoom level.
   */
  constructor(viewport, setBrowserZoomFunction, initialZoom) {
    this.viewport_ = viewport;
    this.setBrowserZoomFunction_ = setBrowserZoomFunction;
    this.browserZoom_ = initialZoom;
    this.changingBrowserZoom_ = null;
  }

  /**
   * Invoked when a browser-initiated zoom-level change occurs.
   * @param {number} newZoom the zoom level to zoom to.
   */
  onBrowserZoomChange(newZoom) {
    // If we are changing the browser zoom level, ignore any browser zoom level
    // change events. Either, the change occurred before our update and will be
    // overwritten, or the change being reported is the change we are making,
    // which we have already handled.
    if (this.changingBrowserZoom_)
      return;

    if (this.floatingPointEquals(this.browserZoom_, newZoom))
      return;

    this.browserZoom_ = newZoom;
    this.viewport_.setZoom(newZoom);
  }

  /**
   * Invoked when an extension-initiated zoom-level change occurs.
   */
  onPdfZoomChange() {
    // If we are already changing the browser zoom level in response to a
    // previous extension-initiated zoom-level change, ignore this zoom change.
    // Once the browser zoom level is changed, we check whether the extension's
    // zoom level matches the most recently sent zoom level.
    if (this.changingBrowserZoom_)
      return;

    let zoom = this.viewport_.zoom;
    if (this.floatingPointEquals(this.browserZoom_, zoom))
      return;

    this.changingBrowserZoom_ = this.setBrowserZoomFunction_(zoom).then(
        function() {
      this.browserZoom_ = zoom;
      this.changingBrowserZoom_ = null;

      // The extension's zoom level may have changed while the browser zoom
      // change was in progress. We call back into onPdfZoomChange to ensure the
      // browser zoom is up to date.
      this.onPdfZoomChange();
    }.bind(this));
  }

  /**
   * Returns whether two numbers are approximately equal.
   * @param {number} a The first number.
   * @param {number} b The second number.
   */
  floatingPointEquals(a, b) {
    let MIN_ZOOM_DELTA = 0.01;
    // If the zoom level is close enough to the current zoom level, don't
    // change it. This avoids us getting into an infinite loop of zoom changes
    // due to floating point error.
    return Math.abs(a - b) <= MIN_ZOOM_DELTA;
  }
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Returns a promise that will resolve to the default zoom factor.
 * @param {!Object} streamInfo The stream object pointing to the data contained
 *     in the PDF.
 * @return {Promise<number>} A promise that will resolve to the default zoom
 *     factor.
 */
function lookupDefaultZoom(streamInfo) {
  // Webviews don't run in tabs so |streamInfo.tabId| is -1 when running within
  // a webview.
  if (!chrome.tabs || streamInfo.tabId < 0)
    return Promise.resolve(1);

  return new Promise(function(resolve, reject) {
    chrome.tabs.getZoomSettings(streamInfo.tabId, function(zoomSettings) {
      resolve(zoomSettings.defaultZoomFactor);
    });
  });
}

/**
 * Returns a promise that will resolve to the initial zoom factor
 * upon starting the plugin. This may differ from the default zoom
 * if, for example, the page is zoomed before the plugin is run.
 * @param {!Object} streamInfo The stream object pointing to the data contained
 *     in the PDF.
 * @return {Promise<number>} A promise that will resolve to the initial zoom
 *     factor.
 */
function lookupInitialZoom(streamInfo) {
  // Webviews don't run in tabs so |streamInfo.tabId| is -1 when running within
  // a webview.
  if (!chrome.tabs || streamInfo.tabId < 0)
    return Promise.resolve(1);

  return new Promise(function(resolve, reject) {
    chrome.tabs.getZoom(streamInfo.tabId, resolve);
  });
}

/**
 * A class providing an interface to the browser.
 */
class BrowserApi {
  /**
   * @constructor
   * @param {!Object} streamInfo The stream object which points to the data
   *     contained in the PDF.
   * @param {number} defaultZoom The default browser zoom.
   * @param {number} initialZoom The initial browser zoom
   *     upon starting the plugin.
   * @param {boolean} manageZoom Whether to manage zoom.
   */
  constructor(streamInfo, defaultZoom, initialZoom, manageZoom) {
    this.streamInfo_ = streamInfo;
    this.defaultZoom_ = defaultZoom;
    this.initialZoom_ = initialZoom;
    this.manageZoom_ = manageZoom;
  }

  /**
   * Returns a promise to a BrowserApi.
   * @param {!Object} streamInfo The stream object pointing to the data
   *     contained in the PDF.
   * @param {boolean} manageZoom Whether to manage zoom.
   */
  static create(streamInfo, manageZoom) {
    return Promise.all([
        lookupDefaultZoom(streamInfo),
        lookupInitialZoom(streamInfo)
    ]).then(function(zoomFactors) {
      return new BrowserApi(
          streamInfo, zoomFactors[0], zoomFactors[1], manageZoom);
    });
  }

  /**
   * Returns the stream info pointing to the data contained in the PDF.
   * @return {Object} The stream info object.
   */
  getStreamInfo() {
    return this.streamInfo_;
  }

  /**
   * Aborts the stream.
   */
  abortStream() {
    if (chrome.mimeHandlerPrivate)
      chrome.mimeHandlerPrivate.abortStream();
  }

  /**
   * Sets the browser zoom.
   * @param {number} zoom The zoom factor to send to the browser.
   * @return {Promise} A promise that will be resolved when the browser zoom
   *     has been updated.
   */
  setZoom(zoom) {
    if (!this.manageZoom_)
      return Promise.resolve();
    return new Promise(function(resolve, reject) {
      chrome.tabs.setZoom(this.streamInfo_.tabId, zoom, resolve);
    }.bind(this));
  }

  /**
   * Returns the default browser zoom factor.
   * @return {number} The default browser zoom factor.
   */
  getDefaultZoom() {
    return this.defaultZoom_;
  }

  /**
   * Returns the initial browser zoom factor.
   * @return {number} The initial browser zoom factor.
   */
  getInitialZoom() {
    return this.initialZoom_;
  }

  /**
   * Adds an event listener to be notified when the browser zoom changes.
   * @param {function} listener The listener to be called with the new zoom
   *     factor.
   */
  addZoomEventListener(listener) {
    if (!this.manageZoom_)
      return;

    chrome.tabs.onZoomChange.addListener(function(zoomChangeInfo) {
      if (zoomChangeInfo.tabId != this.streamInfo_.tabId)
        return;
      listener(zoomChangeInfo.newZoomFactor);
    }.bind(this));
  }
};

/**
 * Creates a BrowserApi for an extension running as a mime handler.
 * @return {Promise<BrowserApi>} A promise to a BrowserApi instance constructed
 *     using the mimeHandlerPrivate API.
 */
function createBrowserApiForMimeHandlerView() {
  return new Promise(function(resolve, reject) {
    chrome.mimeHandlerPrivate.getStreamInfo(resolve);
  }).then(function(streamInfo) {
    let manageZoom = !streamInfo.embedded && streamInfo.tabId != -1;
    return new Promise(function(resolve, reject) {
      if (!manageZoom) {
        resolve();
        return;
      }
      chrome.tabs.setZoomSettings(
          streamInfo.tabId, {mode: 'manual', scope: 'per-tab'}, resolve);
    }).then(function() { return BrowserApi.create(streamInfo, manageZoom); });
  });
}

/**
 * Creates a BrowserApi instance for an extension not running as a mime handler.
 * @return {Promise<BrowserApi>} A promise to a BrowserApi instance constructed
 *     from the URL.
 */
function createBrowserApiForStandaloneExtension() {
  let url = window.location.search.substring(1);
  let streamInfo = {
    streamUrl: url,
    originalUrl: url,
    responseHeaders: {},
    embedded: window.parent != window,
    tabId: -1,
  };
  return new Promise(function(resolve, reject) {
    if (!chrome.tabs) {
      resolve();
      return;
    }
    chrome.tabs.getCurrent(function(tab) {
      streamInfo.tabId = tab.id;
      resolve();
    });
  }).then(function() { return BrowserApi.create(streamInfo, false); });
}

/**
 * Returns a promise that will resolve to a BrowserApi instance.
 * @return {Promise<BrowserApi>} A promise to a BrowserApi instance for the
 *     current environment.
 */
function createBrowserApi() {
  if (window.location.search)
    return createBrowserApiForStandaloneExtension();

  return createBrowserApiForMimeHandlerView();
}
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This is to work-around an issue where this extension is not granted
// permission to access chrome://resources when iframed for print preview.
// See https://crbug.com/444752.
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:root {
  --iron-icon-height: 20px;
  --iron-icon-width: 20px;
  --paper-icon-button: {
    height: 32px;
    padding: 6px;
    width: 32px;
  };
  --paper-icon-button-ink-color: rgb(189, 189, 189);
  --viewer-icon-ink-color: rgb(189, 189, 189);
}
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#item {
  @apply(--layout-center);
  @apply(--layout-horizontal);
  color: rgb(80, 80, 80);
  cursor: pointer;
  font-size: 77.8%;
  height: 30px;
  position: relative;
}

#item:hover {
  background-color: rgb(237, 237, 237);
  color: rgb(20, 20, 20);
}

paper-ripple {
  /* Allowing the ripple to capture pointer events prevents a focus rectangle
   * for showing up for clicks, while still allowing it with tab-navigation.
   * This undoes a paper-ripple bugfix aimed at non-Chrome browsers.
   * TODO(tsergeant): Improve focus in viewer-bookmark so this can be removed
   * (https://crbug.com/5448190).
   */
  pointer-events: auto;
}

#title {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

#expand {
  --iron-icon-height: 16px;
  --iron-icon-width: 16px;
  --paper-icon-button-ink-color: var(--paper-grey-900);
  height: 28px;
  min-width: 28px;
  padding: 6px;
  transition: transform 150ms;
  width: 28px;
}

:host-context([dir=rtl]) #expand {
  transform: rotate(180deg);
}

:host([children-shown]) #expand {
  transform: rotate(90deg);
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-ripple/paper-ripple.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-styles/paper-styles.html">

<dom-module id="viewer-bookmark" attributes="bookmark">
  <link rel="import" type="css" href="viewer-bookmark.css">
  <template>
    <div id="item" on-click="onClick">
      <paper-ripple></paper-ripple>
      <paper-icon-button id="expand" icon="chevron-right"
          on-click="toggleChildren">
      </paper-icon-button>
      <span id="title" tabindex="0">{{bookmark.title}}</span>
    </div>
    <!-- dom-if will stamp the complex bookmark tree lazily as individual nodes
      are opened. -->
    <template is="dom-if" if="{{childrenShown}}" id="sub-bookmarks">
      <template is="dom-repeat" items="{{bookmark.children}}">
        <viewer-bookmark bookmark="{{item}}" depth="{{childDepth}}">
        </viewer-bookmark>
      </template>
    </template>
  </template>
</dom-module>
<script src="viewer-bookmark.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {
  /** Amount that each level of bookmarks is indented by (px). */
  var BOOKMARK_INDENT = 20;

  Polymer({
    is: 'viewer-bookmark',

    properties: {
      /**
       * A bookmark object, each containing a:
       * - title
       * - page (optional)
       * - children (an array of bookmarks)
       */
      bookmark: {
        type: Object,
        observer: 'bookmarkChanged_'
      },

      depth: {
        type: Number,
        observer: 'depthChanged'
      },

      childDepth: Number,

      childrenShown: {
        type: Boolean,
        reflectToAttribute: true,
        value: false
      },

      keyEventTarget: {
        type: Object,
        value: function() {
          return this.$.item;
        }
      }
    },

    behaviors: [
      Polymer.IronA11yKeysBehavior
    ],

    keyBindings: {
      'enter': 'onEnter_',
      'space': 'onSpace_'
    },

    bookmarkChanged_: function() {
      this.$.expand.style.visibility =
          this.bookmark.children.length > 0 ? 'visible' : 'hidden';
    },

    depthChanged: function() {
      this.childDepth = this.depth + 1;
      this.$.item.style.webkitPaddingStart =
          (this.depth * BOOKMARK_INDENT) + 'px';
    },

    onClick: function() {
      if (this.bookmark.hasOwnProperty('page'))
        this.fire('change-page', {page: this.bookmark.page});
      else if (this.bookmark.hasOwnProperty('uri'))
        this.fire('navigate', {uri: this.bookmark.uri, newtab: true});
    },

    onEnter_: function(e) {
      // Don't allow events which have propagated up from the expand button to
      // trigger a click.
      if (e.detail.keyboardEvent.target != this.$.expand)
        this.onClick();
    },

    onSpace_: function(e) {
      // paper-icon-button stops propagation of space events, so there's no need
      // to check the event source here.
      this.onClick();
      // Prevent default space scroll behavior.
      e.detail.keyboardEvent.preventDefault();
    },

    toggleChildren: function(e) {
      this.childrenShown = !this.childrenShown;
      e.stopPropagation();  // Prevent the above onClick handler from firing.
    }
  });
})();
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="../viewer-bookmark/viewer-bookmark.html">

<dom-module id="viewer-bookmarks-content">
  <template>
    <template is="dom-repeat" items="{{bookmarks}}">
      <viewer-bookmark bookmark="{{item}}" depth="0"></viewer-bookmark>
    </template>
  </template>
</dom-module>
<script src="viewer-bookmarks-content.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-bookmarks-content'
});
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

.last-item {
  margin-bottom: 24px;
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/animations/fade-in-animation.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-button/paper-button.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-dialog/paper-dialog.html">

<dom-module id="viewer-error-screen">
  <link rel="import" type="css" href="viewer-error-screen.css">
  <template>
    <paper-dialog id="dialog" modal no-cancel-on-esc-key
        entry-animation="fade-in-animation">
      <div id="load-failed-message" class="last-item"
          i18n-content="pageLoadFailed"></div>
      <div class="buttons" hidden$="{{!reloadFn}}">
        <paper-button on-click="reload" autofocus i18n-content="pageReload">
        </paper-button>
      </div>
    </paper-dialog>
  </template>
</dom-module>
<script src="viewer-error-screen.js"></script>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-error-screen',
  properties: {
    reloadFn: {
      type: Object,
      value: null,
      observer: 'reloadFnChanged_'
    }
  },

  reloadFnChanged_: function() {
    // The default margins in paper-dialog don't work well with hiding/showing
    // the .buttons div. We need to manually manage the bottom margin to get
    // around this.
    if (this.reloadFn)
      this.$['load-failed-message'].classList.remove('last-item');
    else
      this.$['load-failed-message'].classList.add('last-item');
  },

  show: function() {
    this.$.dialog.open();
  },

  reload: function() {
    if (this.reloadFn)
      this.reloadFn();
  }
});
/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:host {
  -webkit-transition: opacity 400ms ease-in-out;
  pointer-events: none;
  position: fixed;
  right: 0;
}

#text {
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 5px;
  color: white;
  float: left;
  font-family: sans-serif;
  font-size: 12px;
  font-weight: bold;
  line-height: 48px;
  text-align: center;
  text-shadow: 1px 1px 1px rgba(0, 0, 0, 0.8);
  width: 62px;
}

#triangle-right {
  border-bottom: 6px solid transparent;
  border-left: 8px solid rgba(0, 0, 0, 0.5);
  border-top: 6px solid transparent;
  display: inline;
  float: left;
  height: 0;
  margin-top: 18px;
  width: 0;
}<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">

<dom-module id="viewer-page-indicator">
  <link rel="import" type="css" href="viewer-page-indicator.css">
  <template>
    <div id="text">{{label}}</div>
    <div id="triangle-right"></div>
  </template>
</dom-module>
<script src="viewer-page-indicator.js"></script>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-page-indicator',

  properties: {
    label: {
      type: String,
      value: '1'
    },

    index: {
      type: Number,
      observer: 'indexChanged'
    },

    pageLabels: {
      type: Array,
      value: null,
      observer: 'pageLabelsChanged'
    }
  },

  timerId: undefined,

  ready: function() {
    var callback = this.fadeIn.bind(this, 2000);
    window.addEventListener('scroll', function() {
      requestAnimationFrame(callback);
    });
  },

  initialFadeIn: function() {
    this.fadeIn(6000);
  },

  fadeIn: function(displayTime) {
    var percent = window.scrollY /
        (document.body.scrollHeight -
         document.documentElement.clientHeight);
    this.style.top = percent *
        (document.documentElement.clientHeight - this.offsetHeight) + 'px';
    this.style.opacity = 1;
    clearTimeout(this.timerId);

    this.timerId = setTimeout(function() {
      this.style.opacity = 0;
      this.timerId = undefined;
    }.bind(this), displayTime);
  },

  pageLabelsChanged: function() {
    this.indexChanged();
  },

  indexChanged: function() {
    if (this.pageLabels)
      this.label = this.pageLabels[this.index];
    else
      this.label = String(this.index + 1);
  }
});
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:host {
  color: #fff;
  font-size: 94.4%;
}

:host ::selection {
  background: rgba(255, 255, 255, 0.3);
}

#pageselector {
  --paper-input-container-underline: {
    visibility: hidden;
  };
  --paper-input-container-underline-focus: {
    visibility: hidden;
  };
  display: inline-block;
  padding: 0;
  width: 1ch;
}

#input {
  -webkit-margin-start: -3px;
  color: #fff;
  line-height: 18px;
  padding: 3px;
  text-align: end;
}

#input:focus,
#input:hover {
  background-color: rgba(0, 0, 0, 0.5);
  border-radius: 2px;
}

#slash {
  padding: 0 3px;
}

#pagelength-spacer {
  display: inline-block;
  text-align: start;
}

#slash,
#pagelength {
  font-size: 76.5%;
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-input/iron-input.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-input/paper-input-container.html">

<dom-module id="viewer-page-selector">
  <link rel="import" type="css" href="viewer-page-selector.css">
  <template>
    <paper-input-container id="pageselector" no-label-float>
      <input id="input" is="iron-input" value="{{pageNo}}"
          prevent-invalid-input allowed-pattern="\d" on-mouseup="select"
          on-change="pageNoCommitted" i18n-values="aria-label:labelPageNumber">
    </paper-input-container>
    <span id="slash"> / </span>
    <span id="pagelength-spacer">
      <span id="pagelength">{{docLength}}</span>
    </span>
  </template>
</dom-module>
<script src="viewer-page-selector.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-page-selector',

  properties: {
    /**
     * The number of pages the document contains.
     */
    docLength: {
      type: Number,
      value: 1,
      observer: 'docLengthChanged'
    },

    /**
     * The current page being viewed (1-based). A change to pageNo is mirrored
     * immediately to the input field. A change to the input field is not
     * mirrored back until pageNoCommitted() is called and change-page is fired.
     */
    pageNo: {
      type: Number,
      value: 1
    }
  },

  pageNoCommitted: function() {
    var page = parseInt(this.$.input.value);

    if (!isNaN(page) && page <= this.docLength && page > 0)
      this.fire('change-page', {page: page - 1});
    else
      this.$.input.value = this.pageNo;
    this.$.input.blur();
  },

  docLengthChanged: function() {
    var numDigits = this.docLength.toString().length;
    this.$.pageselector.style.width = numDigits + 'ch';
    // Set both sides of the slash to the same width, so that the layout is
    // exactly centered.
    this.$['pagelength-spacer'].style.width = numDigits + 'ch';
  },

  select: function() {
    this.$.input.select();
  },

  /**
   * @return {boolean} True if the selector input field is currently focused.
   */
  isActive: function() {
    return this.shadowRoot.activeElement == this.$.input;
  }
});
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/animations/fade-in-animation.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/animations/fade-out-animation.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-button/paper-button.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-dialog/paper-dialog.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-input/paper-input.html">

<dom-module id="viewer-password-screen">
  <template>
    <paper-dialog id="dialog" modal no-cancel-on-esc-key
        entry-animation="fade-in-animation" exit-animation="fade-out-animation">
      <div id="message" i18n-content="passwordPrompt"></div>
      <div class="horizontal layout start">
        <paper-input-container id="password-container" class="flex"
            no-label-float invalid="[[invalid]]">
          <input is="iron-input" id="password" type="password" size="20"
              on-keypress="handleKey" autofocus>
          </input>
          <paper-input-error hidden$="[[!invalid]]"
              i18n-content="passwordInvalid"></paper-input-error>
        </paper-input-container>
        <paper-button id="submit" on-click="submit"
                                  i18n-content="passwordSubmit"></paper-button>
      </div>
    </paper-dialog>
  </template>
</dom-module>
<script src="viewer-password-screen.js"></script>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-password-screen',

  properties: {
    invalid: Boolean,

    active: {
      type: Boolean,
      value: false,
      observer: 'activeChanged'
    }
  },

  ready: function() {
    this.activeChanged();
  },

  accept: function() {
    this.active = false;
  },

  deny: function() {
    this.$.password.disabled = false;
    this.$.submit.disabled = false;
    this.invalid = true;
    this.$.password.focus();
    this.$.password.select();
  },

  handleKey: function(e) {
    if (e.keyCode == 13)
      this.submit();
  },

  submit: function() {
    if (this.$.password.value.length == 0)
      return;
    this.$.password.disabled = true;
    this.$.submit.disabled = true;
    this.fire('password-submitted', {password: this.$.password.value});
  },

  activeChanged: function() {
    if (this.active) {
      this.$.dialog.open();
      this.$.password.focus();
    } else {
      this.$.dialog.close();
    }
  }
});
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:host ::selection {
  background: rgba(255, 255, 255, 0.3);
}

/* We introduce a wrapper aligner element to help with laying out the main
 * toolbar content without changing the bottom-aligned progress bar. */
#aligner {
  @apply(--layout-horizontal);
  @apply(--layout-center);
  padding: 0 16px;
  width: 100%;
}

#title {
  @apply(--layout-flex-5);
  font-size: 77.8%;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

#pageselector-container {
  @apply(--layout-flex-1);
  text-align: center;
  /* The container resizes according to the width of the toolbar. On small
   * screens with large numbers of pages, overflow page numbers without
   * wrapping. */
  white-space: nowrap;
}

#buttons {
  @apply(--layout-flex-5);
  -webkit-user-select: none;
  text-align: end;
}

paper-icon-button {
  -webkit-margin-end: 12px;
}

viewer-toolbar-dropdown {
  -webkit-margin-end: 4px;
}

paper-progress {
  --paper-progress-active-color: var(--google-blue-300);
  --paper-progress-container-color: transparent;
  --paper-progress-height: 3px;
  transition: opacity 150ms;
  width: 100%;
}

paper-toolbar {
  --paper-toolbar-background: rgb(50, 54, 57);
  --paper-toolbar-height: 48px;
  @apply(--shadow-elevation-2dp);
  color: rgb(241, 241, 241);
  font-size: 1.5em;
}

.invisible {
  visibility: hidden;
}

@media(max-width: 675px) {
  #bookmarks,
  #rotate-left {
    display: none;
  }

  #pageselector-container {
    flex: 2;
  }
}

@media(max-width: 450px) {
  #rotate-right {
    display: none;
  }
}

@media(max-width: 400px) {
  #buttons,
  #pageselector-container {
    display: none;
  }
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-icons/image-icons.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-icons/iron-icons.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/animations/slide-up-animation.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/animations/transform-animation.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/neon-animation-runner-behavior.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-icon-button/paper-icon-button.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-progress/paper-progress.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-toolbar/paper-toolbar.html">
<link rel="import" href="../viewer-bookmarks-content/viewer-bookmarks-content.html">
<link rel="import" href="../viewer-page-selector/viewer-page-selector.html">
<link rel="import" href="../viewer-toolbar-dropdown/viewer-toolbar-dropdown.html">

<dom-module id="viewer-pdf-toolbar">
  <link rel="import" type="css" href="../shared-icon-style.css">
  <link rel="import" type="css" href="viewer-pdf-toolbar.css">
  <template>

    <paper-toolbar>
      <div id="aligner" class="middle">
        <span id="title" title="{{docTitle}}">
          <span>{{docTitle}}</span>
        </span>

        <div id="pageselector-container">
          <viewer-page-selector id="pageselector" class="invisible"
              doc-length="{{docLength}}" page-no="{{pageNo}}">
          </viewer-page-selector>
        </div>

        <div id="buttons" class="invisible">
          <paper-icon-button id="rotate-right" icon="image:rotate-right"
              on-click="rotateRight"
              i18n-values="aria-label:tooltipRotateCW;title:tooltipRotateCW">
          </paper-icon-button>

          <paper-icon-button id="download" icon="file-download"
              on-click="download"
              i18n-values="aria-label:tooltipDownload;title:tooltipDownload">
          </paper-icon-button>

          <paper-icon-button id="print" icon="print"
              on-click="print"
              i18n-values="aria-label:tooltipPrint;title:tooltipPrint">
          </paper-icon-button>

          <viewer-toolbar-dropdown id="bookmarks"
                                   hidden$="[[!bookmarks.length]]"
                                   open-icon="bookmark"
                                   closed-icon="bookmark-border"
                                   i18n-values="header:bookmarks">
              <viewer-bookmarks-content bookmarks="{{bookmarks}}">
              </viewer-bookmarks-content>
          </viewer-toolbar-dropdown>
        </div>
      </div>
      <div class="bottom fit">
        <paper-progress id="progress" value="{{loadProgress}}"></paper-progress>
      </div>
    </paper-toolbar>
  </template>
</dom-module>
<script src="viewer-pdf-toolbar.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
(function() {
  Polymer({
    is: 'viewer-pdf-toolbar',

    behaviors: [
      Polymer.NeonAnimationRunnerBehavior
    ],

    properties: {
      /**
       * The current loading progress of the PDF document (0 - 100).
       */
      loadProgress: {
        type: Number,
        observer: 'loadProgressChanged'
      },

      /**
       * The title of the PDF document.
       */
      docTitle: String,

      /**
       * The number of the page being viewed (1-based).
       */
      pageNo: Number,

      /**
       * Tree of PDF bookmarks (or null if the document has no bookmarks).
       */
      bookmarks: {
        type: Object,
        value: null
      },

      /**
       * The number of pages in the PDF document.
       */
      docLength: Number,

      /**
       * Whether the toolbar is opened and visible.
       */
      opened: {
        type: Boolean,
        value: true
      },

      animationConfig: {
        value: function() {
          return {
            'entry': {
              name: 'transform-animation',
              node: this,
              transformFrom: 'translateY(-100%)',
              transformTo: 'translateY(0%)',
              timing: {
                easing: 'cubic-bezier(0, 0, 0.2, 1)',
                duration: 250
              }
            },
            'exit': {
              name: 'slide-up-animation',
              node: this,
              timing: {
                easing: 'cubic-bezier(0.4, 0, 1, 1)',
                duration: 250
              }
            }
          };
        }
      }
    },

    listeners: {
      'neon-animation-finish': '_onAnimationFinished'
    },

    _onAnimationFinished: function() {
      this.style.transform = this.opened ? 'none' : 'translateY(-100%)';
    },

    loadProgressChanged: function() {
      if (this.loadProgress >= 100) {
        this.$.pageselector.classList.toggle('invisible', false);
        this.$.buttons.classList.toggle('invisible', false);
        this.$.progress.style.opacity = 0;
      }
    },

    hide: function() {
      if (this.opened)
        this.toggleVisibility();
    },

    show: function() {
      if (!this.opened) {
        this.toggleVisibility();
      }
    },

    toggleVisibility: function() {
      this.opened = !this.opened;
      this.cancelAnimation();
      this.playAnimation(this.opened ? 'entry' : 'exit');
    },

    selectPageNumber: function() {
      this.$.pageselector.select();
    },

    shouldKeepOpen: function() {
      return this.$.bookmarks.dropdownOpen || this.loadProgress < 100 ||
          this.$.pageselector.isActive();
    },

    hideDropdowns: function() {
      if (this.$.bookmarks.dropdownOpen) {
        this.$.bookmarks.toggleDropdown();
        return true;
      }
      return false;
    },

    setDropdownLowerBound: function(lowerBound) {
      this.$.bookmarks.lowerBound = lowerBound;
    },

    rotateRight: function() {
      this.fire('rotate-right');
    },

    download: function() {
      this.fire('save');
    },

    print: function() {
      this.fire('print');
    }
  });
})();
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:host {
  display: inline-block;
  text-align: start;
}

#container {
  position: absolute;
  /* Controls the position of the dropdown relative to the right of the screen.
   * Default is aligned with the right of the toolbar buttons.
   * TODO(tsergeant): Change the layout of the dropdown so this is not required.
   */
  right: var(--viewer-toolbar-dropdown-right-distance, 36px);
}

:host-context([dir=rtl]) #container {
  left: var(--viewer-toolbar-dropdown-right-distance, 36px);
  right: auto;
}

paper-material {
  background-color: rgb(256, 256, 256);
  border-radius: 4px;
  overflow-y: hidden;
  padding-bottom: 2px;
  width: 260px;
}

#scroll-container {
  max-height: 300px;
  overflow-y: auto;
  padding: 6px 0 4px 0;
}

#icon {
  cursor: pointer;
  display: inline-block;
}

:host([dropdown-open]) #icon {
  background-color: rgb(25, 27, 29);
  border-radius: 4px;
}

#arrow {
  -webkit-margin-start: -12px;
  -webkit-padding-end: 4px;
}

h1 {
  border-bottom: 1px solid rgb(219, 219, 219);
  color: rgb(33, 33, 33);
  font-size: 77.8%;
  font-weight: 500;
  margin: 0;
  padding: 14px 28px;
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/neon-animation/web-animations.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-material/paper-material.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-icon-button/paper-icon-button.html">

<dom-module id="viewer-toolbar-dropdown">
  <link rel="import" type="css" href="../shared-icon-style.css">
  <link rel="import" type="css" href="viewer-toolbar-dropdown.css">
  <template>
    <div on-click="toggleDropdown" id="icon">
      <paper-icon-button id="main-icon" icon="[[dropdownIcon]]"
          aria-label="{{header}}" title="{{header}}">
      </paper-icon-button>
      <iron-icon icon="arrow-drop-down" id="arrow"></iron-icon>
    </div>

    <div id="container">
      <paper-material id="dropdown" style="display: none">
        <h1>{{header}}</h1>
        <div id="scroll-container">
          <content></content>
        </div>
      </paper-material>
    </div>
  </template>
</dom-module>

<script src="viewer-toolbar-dropdown.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {
  /**
   * Size of additional padding in the inner scrollable section of the dropdown.
   */
  var DROPDOWN_INNER_PADDING = 12;

  /** Size of vertical padding on the outer #dropdown element. */
  var DROPDOWN_OUTER_PADDING = 2;

  /** Minimum height of toolbar dropdowns (px). */
  var MIN_DROPDOWN_HEIGHT = 200;

  Polymer({
    is: 'viewer-toolbar-dropdown',

    properties: {
      /** String to be displayed at the top of the dropdown. */
      header: String,

      /** Icon to display when the dropdown is closed. */
      closedIcon: String,

      /** Icon to display when the dropdown is open. */
      openIcon: String,

      /** True if the dropdown is currently open. */
      dropdownOpen: {
        type: Boolean,
        reflectToAttribute: true,
        value: false
      },

      /** Toolbar icon currently being displayed. */
      dropdownIcon: {
        type: String,
        computed: 'computeIcon_(dropdownOpen, closedIcon, openIcon)'
      },

      /** Lowest vertical point that the dropdown should occupy (px). */
      lowerBound: {
        type: Number,
        observer: 'lowerBoundChanged_'
      },

      /**
       * True if the max-height CSS property for the dropdown scroll container
       * is valid. If false, the height will be updated the next time the
       * dropdown is visible.
       */
      maxHeightValid_: false,

      /** Current animation being played, or null if there is none. */
      animation_: Object
    },

    computeIcon_: function(dropdownOpen, closedIcon, openIcon) {
      return dropdownOpen ? openIcon : closedIcon;
    },

    lowerBoundChanged_: function() {
      this.maxHeightValid_ = false;
      if (this.dropdownOpen)
        this.updateMaxHeight();
    },

    toggleDropdown: function() {
      this.dropdownOpen = !this.dropdownOpen;
      if (this.dropdownOpen) {
        this.$.dropdown.style.display = 'block';
        if (!this.maxHeightValid_)
          this.updateMaxHeight();
      }
      this.cancelAnimation_();
      this.playAnimation_(this.dropdownOpen);
    },

    updateMaxHeight: function() {
      var scrollContainer = this.$['scroll-container'];
      var height = this.lowerBound -
          scrollContainer.getBoundingClientRect().top -
          DROPDOWN_INNER_PADDING;
      height = Math.max(height, MIN_DROPDOWN_HEIGHT);
      scrollContainer.style.maxHeight = height + 'px';
      this.maxHeightValid_ = true;
    },

    cancelAnimation_: function() {
      if (this._animation)
        this._animation.cancel();
    },

    /**
     * Start an animation on the dropdown.
     * @param {boolean} isEntry True to play entry animation, false to play
     * exit.
     * @private
     */
    playAnimation_: function(isEntry) {
      this.animation_ = isEntry ? this.animateEntry_() : this.animateExit_();
      this.animation_.onfinish = function() {
        this.animation_ = null;
        if (!this.dropdownOpen)
          this.$.dropdown.style.display = 'none';
      }.bind(this);
    },

    animateEntry_: function() {
      var maxHeight = this.$.dropdown.getBoundingClientRect().height -
          DROPDOWN_OUTER_PADDING;

      if (maxHeight < 0)
        maxHeight = 0;

      var fade = new KeyframeEffect(this.$.dropdown, [
            {opacity: 0},
            {opacity: 1}
          ], {duration: 150, easing: 'cubic-bezier(0, 0, 0.2, 1)'});
      var slide = new KeyframeEffect(this.$.dropdown, [
            {height: '20px', transform: 'translateY(-10px)'},
            {height: maxHeight + 'px', transform: 'translateY(0)'}
          ], {duration: 250, easing: 'cubic-bezier(0, 0, 0.2, 1)'});

      return document.timeline.play(new GroupEffect([fade, slide]));
    },

    animateExit_: function() {
      return this.$.dropdown.animate([
            {transform: 'translateY(0)', opacity: 1},
            {transform: 'translateY(-5px)', opacity: 0}
          ], {duration: 100, easing: 'cubic-bezier(0.4, 0, 1, 1)'});
    }
  });

})();
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#wrapper {
  transition: transform 250ms;
  transition-timing-function: cubic-bezier(0, 0, 0.2, 1);
}

:host([closed]) #wrapper {
  /* 132px roughly flips the location of the button across the right edge of the
   * page. */
  transform: translateX(132px);
  transition-timing-function: cubic-bezier(0.4, 0, 1, 1);
}

:host-context([dir=rtl]):host([closed]) #wrapper {
  transform: translateX(-132px);
}

paper-fab {
  --paper-fab-keyboard-focus-background: var(--viewer-icon-ink-color);
  --paper-fab-mini: {
    height: 36px;
    padding: 8px;
    width: 36px;
  };
  @apply(--shadow-elevation-4dp);
  background-color: rgb(242, 242, 242);
  color: rgb(96, 96, 96);
  overflow: visible;
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-icons/iron-icons.html">
<link rel="import" href="chrome://resources/polymer/v1_0/paper-fab/paper-fab.html">

<dom-module id="viewer-zoom-button">
  <link rel="import" type="css" href="../shared-icon-style.css">
  <link rel="import" type="css" href="viewer-zoom-button.css">
  <template>
    <div id="wrapper">
      <paper-fab id="button" mini icon="[[visibleIcon_]]" on-click="fireClick"
          aria-label$="[[visibleTooltip_]]" title="[[visibleTooltip_]]">
      </paper-fab>
    </div>
  </template>
</dom-module>
<script src="viewer-zoom-button.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'viewer-zoom-button',

  properties: {
    /**
     * Icons to be displayed on the FAB. Multiple icons should be separated with
     * spaces, and will be cycled through every time the FAB is clicked.
     */
    icons: String,

    /**
     * Array version of the list of icons. Polymer does not allow array
     * properties to be set from HTML, so we must use a string property and
     * perform the conversion manually.
     * @private
     */
    icons_: {
      type: Array,
      value: [''],
      computed: 'computeIconsArray_(icons)'
    },

    tooltips: Array,

    closed: {
      type: Boolean,
      reflectToAttribute: true,
      value: false
    },

    delay: {
      type: Number,
      observer: 'delayChanged_'
    },

    /**
     * Index of the icon currently being displayed.
     */
    activeIndex: {
      type: Number,
      value: 0
    },

    /**
     * Icon currently being displayed on the FAB.
     * @private
     */
    visibleIcon_: {
      type: String,
      computed: 'computeVisibleIcon_(icons_, activeIndex)'
    },

    visibleTooltip_: {
      type: String,
      computed: 'computeVisibleTooltip_(tooltips, activeIndex)'
    }
  },

  computeIconsArray_: function(icons) {
    return icons.split(' ');
  },

  computeVisibleIcon_: function(icons, activeIndex) {
    return icons[activeIndex];
  },

  computeVisibleTooltip_: function(tooltips, activeIndex) {
    return tooltips[activeIndex];
  },

  delayChanged_: function() {
    this.$.wrapper.style.transitionDelay = this.delay + 'ms';
  },

  show: function() {
    this.closed = false;
  },

  hide: function() {
    this.closed = true;
  },

  fireClick: function() {
    // We cannot attach an on-click to the entire viewer-zoom-button, as this
    // will include clicks on the margins. Instead, proxy clicks on the FAB
    // through.
    this.fire('fabclick');

    this.activeIndex = (this.activeIndex + 1) % this.icons_.length;
  }
});
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

:host {
  -webkit-user-select: none;
  bottom: 0;
  padding: 48px 0;
  position: fixed;
  right: 0;
  z-index: 3;
}

:host-context([dir=rtl]) {
  left: 0;
  right: auto;
}

#zoom-buttons {
  position: relative;
  right: 48px;
}

:host-context([dir=rtl]) #zoom-buttons {
  left: 48px;
  right: auto;
}

viewer-zoom-button {
  display: block;
}

/* A small gap between the zoom in/zoom out buttons. */
#zoom-out-button {
  margin-top: 10px;
}

/* A larger gap between the fit button and bottom two buttons. */
#zoom-in-button {
  margin-top: 24px;
}
<link rel="import" href="chrome://resources/polymer/v1_0/polymer/polymer.html">
<link rel="import" href="chrome://resources/polymer/v1_0/iron-icons/iron-icons.html">
<link rel="import" href="viewer-zoom-button.html">

<dom-module id="viewer-zoom-toolbar">
  <link rel="import" type="css" href="viewer-zoom-toolbar.css">
  <template>

    <div id="zoom-buttons">
      <viewer-zoom-button id="fit-button" icons="fullscreen-exit fullscreen"
          on-fabclick="fitToggle" delay="100">
      </viewer-zoom-button>
      <viewer-zoom-button id="zoom-in-button" icons="add"
          on-fabclick="zoomIn" delay="50"></viewer-zoom-button>
      <viewer-zoom-button id="zoom-out-button" icons="remove"
          on-fabclick="zoomOut" delay="0"></viewer-zoom-button>
    </div>
  </template>
</dom-module>
<script src="viewer-zoom-toolbar.js"></script>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {

  var FIT_TO_PAGE = 0;
  var FIT_TO_WIDTH = 1;

  Polymer({
    is: 'viewer-zoom-toolbar',

    properties: {
      visible_: {
        type: Boolean,
        value: true
      }
    },

    isVisible: function() {
      return this.visible_;
    },

    /**
     * Change button tooltips to match any changes to loadTimeData.
     */
    updateTooltips: function() {
      this.$['fit-button'].tooltips = [
          loadTimeData.getString('tooltipFitToPage'),
          loadTimeData.getString('tooltipFitToWidth')
      ];
      this.$['zoom-in-button'].tooltips =
          [loadTimeData.getString('tooltipZoomIn')];
      this.$['zoom-out-button'].tooltips =
          [loadTimeData.getString('tooltipZoomOut')];
    },

    fitToggle: function() {
      if (this.$['fit-button'].activeIndex == FIT_TO_WIDTH)
        this.fire('fit-to-width');
      else
        this.fire('fit-to-page');
    },

    zoomIn: function() {
      this.fire('zoom-in');
    },

    zoomOut: function() {
      this.fire('zoom-out');
    },

    show: function() {
      if (!this.visible_) {
        this.visible_ = true;
        this.$['fit-button'].show();
        this.$['zoom-in-button'].show();
        this.$['zoom-out-button'].show();
      }
    },

    hide: function() {
      if (this.visible_) {
        this.visible_ = false;
        this.$['fit-button'].hide();
        this.$['zoom-in-button'].hide();
        this.$['zoom-out-button'].hide();
      }
    },
  });

})();
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/** @fileoverview Various string utility functions */
'use strict';

/**
 * Converts a string to an array of bytes.
 * @param {string} s The string to convert.
 * @param {(Array|Uint8Array)=} bytes The Array-like object into which to store
 *     the bytes. A new Array will be created if not provided.
 * @return {(Array|Uint8Array)} An array of bytes representing the string.
 */
function UTIL_StringToBytes(s, bytes) {
  bytes = bytes || new Array(s.length);
  for (var i = 0; i < s.length; ++i)
    bytes[i] = s.charCodeAt(i);
  return bytes;
}

/**
 * Converts a byte array to a string.
 * @param {(Uint8Array|Array<number>)} b input byte array.
 * @return {string} result.
 */
function UTIL_BytesToString(b) {
  return String.fromCharCode.apply(null, b);
}

/**
 * Converts a byte array to a hex string.
 * @param {(Uint8Array|Array<number>)} b input byte array.
 * @return {string} result.
 */
function UTIL_BytesToHex(b) {
  if (!b) return '(null)';
  var hexchars = '0123456789ABCDEF';
  var hexrep = new Array(b.length * 2);

  for (var i = 0; i < b.length; ++i) {
    hexrep[i * 2 + 0] = hexchars.charAt((b[i] >> 4) & 15);
    hexrep[i * 2 + 1] = hexchars.charAt(b[i] & 15);
  }
  return hexrep.join('');
}

function UTIL_BytesToHexWithSeparator(b, sep) {
  var hexchars = '0123456789ABCDEF';
  var stride = 2 + (sep ? 1 : 0);
  var hexrep = new Array(b.length * stride);

  for (var i = 0; i < b.length; ++i) {
    if (sep) hexrep[i * stride + 0] = sep;
    hexrep[i * stride + stride - 2] = hexchars.charAt((b[i] >> 4) & 15);
    hexrep[i * stride + stride - 1] = hexchars.charAt(b[i] & 15);
  }
  return (sep ? hexrep.slice(1) : hexrep).join('');
}

function UTIL_HexToBytes(h) {
  var hexchars = '0123456789ABCDEFabcdef';
  var res = new Uint8Array(h.length / 2);
  for (var i = 0; i < h.length; i += 2) {
    if (hexchars.indexOf(h.substring(i, i + 1)) == -1) break;
    res[i / 2] = parseInt(h.substring(i, i + 2), 16);
  }
  return res;
}

function UTIL_HexToArray(h) {
  var hexchars = '0123456789ABCDEFabcdef';
  var res = new Array(h.length / 2);
  for (var i = 0; i < h.length; i += 2) {
    if (hexchars.indexOf(h.substring(i, i + 1)) == -1) break;
    res[i / 2] = parseInt(h.substring(i, i + 2), 16);
  }
  return res;
}

function UTIL_equalArrays(a, b) {
  if (!a || !b) return false;
  if (a.length != b.length) return false;
  var accu = 0;
  for (var i = 0; i < a.length; ++i)
    accu |= a[i] ^ b[i];
  return accu === 0;
}

function UTIL_ltArrays(a, b) {
  if (a.length < b.length) return true;
  if (a.length > b.length) return false;
  for (var i = 0; i < a.length; ++i) {
    if (a[i] < b[i]) return true;
    if (a[i] > b[i]) return false;
  }
  return false;
}

function UTIL_gtArrays(a, b) {
  return UTIL_ltArrays(b, a);
}

function UTIL_geArrays(a, b) {
  return !UTIL_ltArrays(a, b);
}

function UTIL_unionArrays(a, b) {
  var obj = {};
  for (var i = 0; i < a.length; i++) {
    obj[a[i]] = a[i];
  }
  for (var i = 0; i < b.length; i++) {
    obj[b[i]] = b[i];
  }
  var union = [];
  for (var k in obj) {
    union.push(obj[k]);
  }
  return union;
}

function UTIL_getRandom(a) {
  var tmp = new Array(a);
  var rnd = new Uint8Array(a);
  window.crypto.getRandomValues(rnd);  // Yay!
  for (var i = 0; i < a; ++i) tmp[i] = rnd[i] & 255;
  return tmp;
}

function UTIL_setFavicon(icon) {
  // Construct a new favion link tag
  var faviconLink = document.createElement('link');
  faviconLink.rel = 'Shortcut Icon';
  faviconLink.type = 'image/x-icon';
  faviconLink.href = icon;

  // Remove the old favion, if it exists
  var head = document.getElementsByTagName('head')[0];
  var links = head.getElementsByTagName('link');
  for (var i = 0; i < links.length; i++) {
    var link = links[i];
    if (link.type == faviconLink.type && link.rel == faviconLink.rel) {
      head.removeChild(link);
    }
  }

  // Add in the new one
  head.appendChild(faviconLink);
}

// Erase all entries in array
function UTIL_clear(a) {
  if (a instanceof Array) {
    for (var i = 0; i < a.length; ++i)
      a[i] = 0;
  }
}

// Type tags used for ASN.1 encoding of ECDSA signatures
/** @const */
var UTIL_ASN_INT = 0x02;
/** @const */
var UTIL_ASN_SEQUENCE = 0x30;

/**
 * Parse SEQ(INT, INT) from ASN1 byte array.
 * @param {(Uint8Array|Array<number>)} a input to parse from.
 * @return {{'r': !Array<number>, 's': !Array<number>}|null}
 */
function UTIL_Asn1SignatureToJson(a) {
  if (a.length < 6) return null;  // Too small to be valid
  if (a[0] != UTIL_ASN_SEQUENCE) return null;
  var l = a[1] & 255;
  if (l & 0x80) return null;  // SEQ.size too large
  if (a.length != 2 + l) return null;  // SEQ size does not match input

  function parseInt(off) {
    if (a[off] != UTIL_ASN_INT) return null;
    var l = a[off + 1] & 255;
    if (l & 0x80) return null;  // INT.size too large
    if (off + 2 + l > a.length) return null;  // Out of bounds
    return a.slice(off + 2, off + 2 + l);
  }

  var r = parseInt(2);
  if (!r) return null;

  var s = parseInt(2 + 2 + r.length);
  if (!s) return null;

  return {'r': r, 's': s};
}

/**
 * Encode a JSON signature {r,s} as an ASN1 SEQ(INT, INT). May modify sig
 * @param {{'r': (!Array<number>|undefined), 's': !Array<number>}} sig
 * @return {!Uint8Array}
 */
function UTIL_JsonSignatureToAsn1(sig) {
  var rbytes = sig.r;
  var sbytes = sig.s;

  // ASN.1 integers are arbitrary length msb first and signed.
  // sig.r and sig.s are 256 bits msb first but _unsigned_, so we must
  // prepend a zero byte in case their high bit is set.
  if (rbytes[0] & 0x80)
    rbytes.unshift(0);
  if (sbytes[0] & 0x80)
    sbytes.unshift(0);

  var len = 4 + rbytes.length + sbytes.length;
  var buf = new Uint8Array(2 + len);
  var i = 0;
  buf[i++] = UTIL_ASN_SEQUENCE;
  buf[i++] = len;

  buf[i++] = UTIL_ASN_INT;
  buf[i++] = rbytes.length;
  buf.set(rbytes, i);
  i += rbytes.length;

  buf[i++] = UTIL_ASN_INT;
  buf[i++] = sbytes.length;
  buf.set(sbytes, i);

  return buf;
}

function UTIL_prepend_zero(s, n) {
  if (s.length == n) return s;
  var l = s.length;
  for (var i = 0; i < n - l; ++i) {
    s = '0' + s;
  }
  return s;
}

// hr:min:sec.milli string
function UTIL_time() {
  var d = new Date();
  var m = UTIL_prepend_zero((d.getMonth() + 1).toString(), 2);
  var t = UTIL_prepend_zero(d.getDate().toString(), 2);
  var H = UTIL_prepend_zero(d.getHours().toString(), 2);
  var M = UTIL_prepend_zero(d.getMinutes().toString(), 2);
  var S = UTIL_prepend_zero(d.getSeconds().toString(), 2);
  var L = UTIL_prepend_zero((d.getMilliseconds() * 1000).toString(), 6);
  return m + t + ' ' + H + ':' + M + ':' + S + '.' + L;
}

var UTIL_events = [];
var UTIL_max_events = 500;

function UTIL_fmt(s) {
  var line = UTIL_time() + ': ' + s;
  if (UTIL_events.push(line) > UTIL_max_events) {
    // Drop from head.
    UTIL_events.splice(0, UTIL_events.length - UTIL_max_events);
  }
  return line;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// WebSafeBase64Escape and Unescape.
function B64_encode(bytes, opt_length) {
  if (!opt_length) opt_length = bytes.length;
  var b64out =
      'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_';
  var result = '';
  var shift = 0;
  var accu = 0;
  var inputIndex = 0;
  while (opt_length--) {
    accu <<= 8;
    accu |= bytes[inputIndex++];
    shift += 8;
    while (shift >= 6) {
      var i = (accu >> (shift - 6)) & 63;
      result += b64out.charAt(i);
      shift -= 6;
    }
  }
  if (shift) {
    accu <<= 8;
    shift += 8;
    var i = (accu >> (shift - 6)) & 63;
    result += b64out.charAt(i);
  }
  return result;
}

// Normal base64 encode; not websafe, including padding.
function base64_encode(bytes, opt_length) {
  if (!opt_length) opt_length = bytes.length;
  var b64out =
      'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';
  var result = '';
  var shift = 0;
  var accu = 0;
  var inputIndex = 0;
  while (opt_length--) {
    accu <<= 8;
    accu |= bytes[inputIndex++];
    shift += 8;
    while (shift >= 6) {
      var i = (accu >> (shift - 6)) & 63;
      result += b64out.charAt(i);
      shift -= 6;
    }
  }
  if (shift) {
    accu <<= 8;
    shift += 8;
    var i = (accu >> (shift - 6)) & 63;
    result += b64out.charAt(i);
  }
  while (result.length % 4) result += '=';
  return result;
}

var B64_inmap =
[
  0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 63, 0, 0,
 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 0, 0, 0, 0, 0, 0,
  0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 0, 0, 0, 0, 64,
  0, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 0, 0, 0, 0, 0
];

function B64_decode(string) {
  var bytes = [];
  var accu = 0;
  var shift = 0;
  for (var i = 0; i < string.length; ++i) {
    var c = string.charCodeAt(i);
    if (c < 32 || c > 127 || !B64_inmap[c - 32]) return [];
    accu <<= 6;
    accu |= (B64_inmap[c - 32] - 1);
    shift += 6;
    if (shift >= 8) {
      bytes.push((accu >> (shift - 8)) & 255);
      shift -= 8;
    }
  }
  return bytes;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Defines a Closeable interface.
 */
'use strict';

/**
 * A closeable interface.
 * @interface
 */
function Closeable() {}

/** Closes this object. */
Closeable.prototype.close = function() {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a countdown-based timer interface.
 */
'use strict';

/**
 * A countdown timer.
 * @interface
 */
function Countdown() {}

/**
 * Sets a new timeout for this timer.
 * @param {number} timeoutMillis how long, in milliseconds, the countdown lasts.
 * @param {Function=} cb called back when the countdown expires.
 * @return {boolean} whether the timeout could be set.
 */
Countdown.prototype.setTimeout = function(timeoutMillis, cb) {};

/** Clears this timer's timeout. Timers that are cleared become expired. */
Countdown.prototype.clearTimeout = function() {};

/**
 * @return {number} how many milliseconds are remaining until the timer expires.
 */
Countdown.prototype.millisecondsUntilExpired = function() {};

/** @return {boolean} whether the timer has expired. */
Countdown.prototype.expired = function() {};

/**
 * Constructs a new clone of this timer, while overriding its callback.
 * @param {Function=} cb callback for new timer.
 * @return {!Countdown} new clone.
 */
Countdown.prototype.clone = function(cb) {};

/**
 * A factory to create countdown timers.
 * @interface
 */
function CountdownFactory() {}

/**
 * Creates a new timer.
 * @param {number} timeoutMillis How long, in milliseconds, the countdown lasts.
 * @param {function()=} opt_cb Called back when the countdown expires.
 * @return {!Countdown} The timer.
 */
CountdownFactory.prototype.createTimer = function(timeoutMillis, opt_cb) {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a countdown-based timer implementation.
 */
'use strict';

/**
 * Constructs a new timer.  The timer has a very limited resolution, and does
 * not attempt to be millisecond accurate. Its intended use is as a
 * low-precision timer that pauses while debugging.
 * @param {!SystemTimer} sysTimer The system timer implementation.
 * @param {number=} timeoutMillis how long, in milliseconds, the countdown
 *     lasts.
 * @param {Function=} cb called back when the countdown expires.
 * @constructor
 * @implements {Countdown}
 */
function CountdownTimer(sysTimer, timeoutMillis, cb) {
  /** @private {!SystemTimer} */
  this.sysTimer_ = sysTimer;
  this.remainingMillis = 0;
  this.setTimeout(timeoutMillis || 0, cb);
}

/** Timer interval */
CountdownTimer.TIMER_INTERVAL_MILLIS = 200;

/**
 * Sets a new timeout for this timer. Only possible if the timer is not
 * currently active.
 * @param {number} timeoutMillis how long, in milliseconds, the countdown lasts.
 * @param {Function=} cb called back when the countdown expires.
 * @return {boolean} whether the timeout could be set.
 */
CountdownTimer.prototype.setTimeout = function(timeoutMillis, cb) {
  if (this.timeoutId)
    return false;
  if (!timeoutMillis || timeoutMillis < 0)
    return false;
  this.remainingMillis = timeoutMillis;
  this.cb = cb;
  if (this.remainingMillis > CountdownTimer.TIMER_INTERVAL_MILLIS) {
    this.timeoutId =
        this.sysTimer_.setInterval(this.timerTick.bind(this),
            CountdownTimer.TIMER_INTERVAL_MILLIS);
  } else {
    // Set a one-shot timer for the last interval.
    this.timeoutId =
        this.sysTimer_.setTimeout(
            this.timerTick.bind(this), this.remainingMillis);
  }
  return true;
};

/** Clears this timer's timeout. Timers that are cleared become expired. */
CountdownTimer.prototype.clearTimeout = function() {
  if (this.timeoutId) {
    this.sysTimer_.clearTimeout(this.timeoutId);
    this.timeoutId = undefined;
  }
  this.remainingMillis = 0;
};

/**
 * @return {number} how many milliseconds are remaining until the timer expires.
 */
CountdownTimer.prototype.millisecondsUntilExpired = function() {
  return this.remainingMillis > 0 ? this.remainingMillis : 0;
};

/** @return {boolean} whether the timer has expired. */
CountdownTimer.prototype.expired = function() {
  return this.remainingMillis <= 0;
};

/**
 * Constructs a new clone of this timer, while overriding its callback.
 * @param {Function=} cb callback for new timer.
 * @return {!Countdown} new clone.
 */
CountdownTimer.prototype.clone = function(cb) {
  return new CountdownTimer(this.sysTimer_, this.remainingMillis, cb);
};

/** Timer callback. */
CountdownTimer.prototype.timerTick = function() {
  this.remainingMillis -= CountdownTimer.TIMER_INTERVAL_MILLIS;
  if (this.expired()) {
    this.sysTimer_.clearTimeout(this.timeoutId);
    this.timeoutId = undefined;
    if (this.cb) {
      this.cb();
    }
  }
};

/**
 * A factory for creating CountdownTimers.
 * @param {!SystemTimer} sysTimer The system timer implementation.
 * @constructor
 * @implements {CountdownFactory}
 */
function CountdownTimerFactory(sysTimer) {
  /** @private {!SystemTimer} */
  this.sysTimer_ = sysTimer;
}

/**
 * Creates a new timer.
 * @param {number} timeoutMillis How long, in milliseconds, the countdown lasts.
 * @param {function()=} opt_cb Called back when the countdown expires.
 * @return {!Countdown} The timer.
 */
CountdownTimerFactory.prototype.createTimer =
    function(timeoutMillis, opt_cb) {
  return new CountdownTimer(this.sysTimer_, timeoutMillis, opt_cb);
};

/**
 * Minimum timeout attenuation, below which a response couldn't be reasonably
 * guaranteed, in seconds.
 * @const
 */
var MINIMUM_TIMEOUT_ATTENUATION_SECONDS = 1;

/**
 * @param {number} timeoutSeconds Timeout value in seconds.
 * @param {number=} opt_attenuationSeconds Attenuation value in seconds.
 * @return {number} The timeout value, attenuated to ensure a response can be
 *     given before the timeout's expiration.
 */
function attenuateTimeoutInSeconds(timeoutSeconds, opt_attenuationSeconds) {
  var attenuationSeconds =
      opt_attenuationSeconds || MINIMUM_TIMEOUT_ATTENUATION_SECONDS;
  if (timeoutSeconds < attenuationSeconds)
    return 0;
  return timeoutSeconds - attenuationSeconds;
}

/**
 * Default request timeout when none is present in the request, in seconds.
 * @const
 */
var DEFAULT_REQUEST_TIMEOUT_SECONDS = 30;

/**
 * Gets the timeout value from the request, if any, substituting
 * opt_defaultTimeoutSeconds or DEFAULT_REQUEST_TIMEOUT_SECONDS if the request
 * does not contain a timeout value.
 * @param {Object} request The request containing the timeout.
 * @param {number=} opt_defaultTimeoutSeconds
 * @return {number} Timeout value, in seconds.
 */
function getTimeoutValueFromRequest(request, opt_defaultTimeoutSeconds) {
  var timeoutValueSeconds;
  if (request.hasOwnProperty('timeoutSeconds')) {
    timeoutValueSeconds = request['timeoutSeconds'];
  } else if (request.hasOwnProperty('timeout')) {
    timeoutValueSeconds = request['timeout'];
  } else if (opt_defaultTimeoutSeconds !== undefined) {
    timeoutValueSeconds = opt_defaultTimeoutSeconds;
  } else {
    timeoutValueSeconds = DEFAULT_REQUEST_TIMEOUT_SECONDS;
  }
  return timeoutValueSeconds;
}

/**
 * Creates a new countdown for the given timeout value, attenuated to ensure a
 * response is given prior to the countdown's expiration, using the given timer
 * factory.
 * @param {CountdownFactory} timerFactory The factory to use.
 * @param {number} timeoutValueSeconds
 * @param {number=} opt_attenuationSeconds Attenuation value in seconds.
 * @return {!Countdown} A countdown timer.
 */
function createAttenuatedTimer(timerFactory, timeoutValueSeconds,
    opt_attenuationSeconds) {
  timeoutValueSeconds = attenuateTimeoutInSeconds(timeoutValueSeconds,
      opt_attenuationSeconds);
  return timerFactory.createTimer(timeoutValueSeconds * 1000);
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// SHA256 in javascript.
//
// SHA256 {
//  SHA256();
//  void reset();
//  void update(byte[] data, opt_length);
//  byte[32] digest();
// }

/** @constructor */
function SHA256() {
  this._buf = new Array(64);
  this._W = new Array(64);
  this._pad = new Array(64);
  this._k = [
   0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5,
   0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
   0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3,
   0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
   0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc,
   0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
   0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7,
   0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
   0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
   0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
   0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3,
   0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
   0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
   0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
   0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208,
   0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2];

  this._pad[0] = 0x80;
  for (var i = 1; i < 64; ++i) this._pad[i] = 0;

  this.reset();
}

/** Reset the hasher */
SHA256.prototype.reset = function() {
  this._chain = [
    0x6a09e667, 0xbb67ae85, 0x3c6ef372, 0xa54ff53a,
    0x510e527f, 0x9b05688c, 0x1f83d9ab, 0x5be0cd19];

  this._inbuf = 0;
  this._total = 0;
};

/** Hash the next block of 64 bytes
 * @param {Array<number>} buf A 64 byte buffer
 */
SHA256.prototype._compress = function(buf) {
  var W = this._W;
  var k = this._k;

  function _rotr(w, r) { return ((w << (32 - r)) | (w >>> r)); };

  // get 16 big endian words
  for (var i = 0; i < 64; i += 4) {
    var w = (buf[i] << 24) |
            (buf[i + 1] << 16) |
            (buf[i + 2] << 8) |
            (buf[i + 3]);
    W[i / 4] = w;
  }

  // expand to 64 words
  for (var i = 16; i < 64; ++i) {
    var s0 = _rotr(W[i - 15], 7) ^ _rotr(W[i - 15], 18) ^ (W[i - 15] >>> 3);
    var s1 = _rotr(W[i - 2], 17) ^ _rotr(W[i - 2], 19) ^ (W[i - 2] >>> 10);
    W[i] = (W[i - 16] + s0 + W[i - 7] + s1) & 0xffffffff;
  }

  var A = this._chain[0];
  var B = this._chain[1];
  var C = this._chain[2];
  var D = this._chain[3];
  var E = this._chain[4];
  var F = this._chain[5];
  var G = this._chain[6];
  var H = this._chain[7];

  for (var i = 0; i < 64; ++i) {
    var S0 = _rotr(A, 2) ^ _rotr(A, 13) ^ _rotr(A, 22);
    var maj = (A & B) ^ (A & C) ^ (B & C);
    var t2 = (S0 + maj) & 0xffffffff;
    var S1 = _rotr(E, 6) ^ _rotr(E, 11) ^ _rotr(E, 25);
    var ch = (E & F) ^ ((~E) & G);
    var t1 = (H + S1 + ch + k[i] + W[i]) & 0xffffffff;

    H = G;
    G = F;
    F = E;
    E = (D + t1) & 0xffffffff;
    D = C;
    C = B;
    B = A;
    A = (t1 + t2) & 0xffffffff;
  }

  this._chain[0] += A;
  this._chain[1] += B;
  this._chain[2] += C;
  this._chain[3] += D;
  this._chain[4] += E;
  this._chain[5] += F;
  this._chain[6] += G;
  this._chain[7] += H;
};

/** Update the hash with additional data
 * @param {Array<number>|Uint8Array} bytes The data
 * @param {number=} opt_length How many bytes to hash, if not all */
SHA256.prototype.update = function(bytes, opt_length) {
  if (!opt_length) opt_length = bytes.length;

  this._total += opt_length;
  for (var n = 0; n < opt_length; ++n) {
    this._buf[this._inbuf++] = bytes[n];
    if (this._inbuf == 64) {
      this._compress(this._buf);
      this._inbuf = 0;
    }
  }
};

/** Update the hash with a specified range from a data buffer
 * @param {Array<number>} bytes The data buffer
 * @param {number} start Starting index of the range in bytes
 * @param {number} end End index, will not be included in range
 */
SHA256.prototype.updateRange = function(bytes, start, end) {
  this._total += (end - start);
  for (var n = start; n < end; ++n) {
    this._buf[this._inbuf++] = bytes[n];
    if (this._inbuf == 64) {
      this._compress(this._buf);
      this._inbuf = 0;
    }
  }
};

/**
 * Optionally update the hash with additional arguments, and return the
 * resulting hash value.
 * @param {...*} var_args Data buffers to hash
 * @return {Array<number>} the SHA256 hash value.
 */
SHA256.prototype.digest = function(var_args) {
  for (var i = 0; i < arguments.length; ++i)
    this.update(arguments[i]);

  var digest = new Array(32);
  var totalBits = this._total * 8;

  // add pad 0x80 0x00*
  if (this._inbuf < 56)
    this.update(this._pad, 56 - this._inbuf);
  else
    this.update(this._pad, 64 - (this._inbuf - 56));

  // add # bits, big endian
  for (var i = 63; i >= 56; --i) {
    this._buf[i] = totalBits & 255;
    totalBits >>>= 8;
  }

  this._compress(this._buf);

  var n = 0;
  for (var i = 0; i < 8; ++i)
    for (var j = 24; j >= 0; j -= 8)
      digest[n++] = (this._chain[i] >> j) & 255;

  return digest;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides an interface representing the browser/extension
 * system's timer interface.
 */
'use strict';

/**
 * An interface representing the browser/extension system's timer interface.
 * @interface
 */
function SystemTimer() {}

/**
 * Sets a single-shot timer.
 * @param {function()} func Called back when the timer expires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
SystemTimer.prototype.setTimeout = function(func, timeoutMillis) {};

/**
 * Clears a previously set timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
SystemTimer.prototype.clearTimeout = function(timeoutId) {};

/**
 * Sets a repeating interval timer.
 * @param {function()} func Called back each time the timer fires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
SystemTimer.prototype.setInterval = function(func, timeoutMillis) {};

/**
 * Clears a previously set interval timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
SystemTimer.prototype.clearInterval = function(timeoutId) {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a low-level gnubby driver based on chrome.hid.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {!chrome.hid.HidConnectInfo} dev The connection to the device.
 * @param {number} id The device's id.
 * @constructor
 * @implements {GnubbyDevice}
 */
function HidGnubbyDevice(gnubbies, dev, id) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  this.dev = dev;
  this.id = id;
  this.txqueue = [];
  this.clients = [];
  this.lockCID = 0;     // channel ID of client holding a lock, if != 0.
  this.lockMillis = 0;  // current lock period.
  this.lockTID = null;  // timer id of lock timeout.
  this.closing = false;  // device to be closed by receive loop.
  this.updating = false;  // device firmware is in final stage of updating.
}

/**
 * Namespace for the HidGnubbyDevice implementation.
 * @const
 */
HidGnubbyDevice.NAMESPACE = 'hid';

/** Destroys this low-level device instance. */
HidGnubbyDevice.prototype.destroy = function() {
  if (!this.dev) return;  // Already dead.

  this.gnubbies_.removeOpenDevice(
      {namespace: HidGnubbyDevice.NAMESPACE, device: this.id});
  this.closing = true;

  console.log(UTIL_fmt('HidGnubbyDevice.destroy()'));

  // Synthesize a close error frame to alert all clients,
  // some of which might be in read state.
  //
  // Use magic CID 0 to address all.
  this.publishFrame_(new Uint8Array([
        0, 0, 0, 0,  // broadcast CID
        GnubbyDevice.CMD_ERROR,
        0, 1,  // length
        GnubbyDevice.GONE]).buffer);

  // Set all clients to closed status and remove them.
  while (this.clients.length != 0) {
    var client = this.clients.shift();
    if (client) client.closed = true;
  }

  if (this.lockTID) {
    window.clearTimeout(this.lockTID);
    this.lockTID = null;
  }

  var dev = this.dev;
  this.dev = null;

  chrome.hid.disconnect(dev.connectionId, function() {
    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('Device ' + dev.connectionId +
          ' couldn\'t be disconnected:'));
      console.warn(UTIL_fmt(chrome.runtime.lastError.message));
      return;
    }
    console.log(UTIL_fmt('Device ' + dev.connectionId + ' closed'));
  });
};

/**
 * Push frame to all clients.
 * @param {ArrayBuffer} f Data to push
 * @private
 */
HidGnubbyDevice.prototype.publishFrame_ = function(f) {
  var old = this.clients;

  var remaining = [];
  var changes = false;
  for (var i = 0; i < old.length; ++i) {
    var client = old[i];
    if (client.receivedFrame(f)) {
      // Client still alive; keep on list.
      remaining.push(client);
    } else {
      changes = true;
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(client.cid) + '] left?'));
    }
  }
  if (changes) this.clients = remaining;
};

/**
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
HidGnubbyDevice.prototype.registerClient = function(who) {
  for (var i = 0; i < this.clients.length; ++i) {
    if (this.clients[i] === who) return;  // Already registered.
  }
  this.clients.push(who);
  if (this.clients.length == 1) {
    // First client? Kick off read loop.
    this.readLoop_();
  }
};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 * Returns number of remaining listeners for this device.
 *     if this had no clients to start with.
 */
HidGnubbyDevice.prototype.deregisterClient = function(who) {
  var current = this.clients;
  if (current.length == 0) return -1;
  this.clients = [];
  for (var i = 0; i < current.length; ++i) {
    var client = current[i];
    if (client !== who) this.clients.push(client);
  }
  return this.clients.length;
};

/**
 * @param {*} who The client.
 * @return {boolean} Whether this device has who as a client.
 */
HidGnubbyDevice.prototype.hasClient = function(who) {
  if (this.clients.length == 0) return false;
  for (var i = 0; i < this.clients.length; ++i) {
    if (who === this.clients[i])
      return true;
  }
  return false;
};

/**
 * Reads all incoming frames and notifies clients of their receipt.
 * @private
 */
HidGnubbyDevice.prototype.readLoop_ = function() {
  //console.log(UTIL_fmt('entering readLoop'));
  if (!this.dev) return;

  if (this.closing) {
    this.destroy();
    return;
  }

  // No interested listeners, yet we hit readLoop().
  // Must be clean-up. We do this here to make sure no transfer is pending.
  if (!this.clients.length) {
    this.closing = true;
    this.destroy();
    return;
  }

  // firmwareUpdate() sets this.updating when writing the last block before
  // the signature. We process that reply with the already pending
  // read transfer but we do not want to start another read transfer for the
  // signature block, since that request will have no reply.
  // Instead we will see the device drop and re-appear on the bus.
  // Current libusb on some platforms gets unhappy when transfer are pending
  // when that happens.
  // TODO: revisit once Chrome stabilizes its behavior.
  if (this.updating) {
    console.log(UTIL_fmt('device updating. Ending readLoop()'));
    return;
  }

  var self = this;
  chrome.hid.receive(
    this.dev.connectionId,
    function(report_id, data) {
      if (chrome.runtime.lastError || !data) {
        console.log(UTIL_fmt('receive got lastError:'));
        console.log(UTIL_fmt(chrome.runtime.lastError.message));
        window.setTimeout(function() { self.destroy(); }, 0);
        return;
      }
      var u8 = new Uint8Array(data);
      console.log(UTIL_fmt('<' + UTIL_BytesToHex(u8)));

      self.publishFrame_(data);

      // Read more.
      window.setTimeout(function() { self.readLoop_(); }, 0);
    }
  );
};

/**
 * Check whether channel is locked for this request or not.
 * @param {number} cid Channel id
 * @param {number} cmd Request command
 * @return {boolean} true if not locked for this request.
 * @private
 */
HidGnubbyDevice.prototype.checkLock_ = function(cid, cmd) {
  if (this.lockCID) {
    // We have an active lock.
    if (this.lockCID != cid) {
      // Some other channel has active lock.

      if (cmd != GnubbyDevice.CMD_SYNC &&
          cmd != GnubbyDevice.CMD_INIT) {
        // Anything but SYNC|INIT gets an immediate busy.
        var busy = new Uint8Array(
            [(cid >> 24) & 255,
             (cid >> 16) & 255,
             (cid >> 8) & 255,
             cid & 255,
             GnubbyDevice.CMD_ERROR,
             0, 1,  // length
             GnubbyDevice.BUSY]);
        // Log the synthetic busy too.
        console.log(UTIL_fmt('<' + UTIL_BytesToHex(busy)));
        this.publishFrame_(busy.buffer);
        return false;
      }

      // SYNC|INIT gets to go to the device to flush OS tx/rx queues.
      // The usb firmware is to alway respond to SYNC/INIT,
      // regardless of lock status.
    }
  }
  return true;
};

/**
 * Update or grab lock.
 * @param {number} cid Channel ID
 * @param {number} cmd Command
 * @param {number} arg Command argument
 * @private
 */
HidGnubbyDevice.prototype.updateLock_ = function(cid, cmd, arg) {
  if (this.lockCID == 0 || this.lockCID == cid) {
    // It is this caller's or nobody's lock.
    if (this.lockTID) {
      window.clearTimeout(this.lockTID);
      this.lockTID = null;
    }

    if (cmd == GnubbyDevice.CMD_LOCK) {
      var nseconds = arg;
      if (nseconds != 0) {
        this.lockCID = cid;
        // Set tracking time to be .1 seconds longer than usb device does.
        this.lockMillis = nseconds * 1000 + 100;
      } else {
        // Releasing lock voluntarily.
        this.lockCID = 0;
      }
    }

    // (re)set the lock timeout if we still hold it.
    if (this.lockCID) {
      var self = this;
      this.lockTID = window.setTimeout(
          function() {
            console.warn(UTIL_fmt(
                'lock for CID ' + Gnubby.hexCid(cid) + ' expired!'));
            self.lockTID = null;
            self.lockCID = 0;
          },
          this.lockMillis);
    }
  }
};

/**
 * Queue command to be sent.
 * If queue was empty, initiate the write.
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command arguments
 */
HidGnubbyDevice.prototype.queueCommand = function(cid, cmd, data) {
  if (!this.dev) return;
  if (!this.checkLock_(cid, cmd)) return;

  var u8 = new Uint8Array(data);
  var f = new Uint8Array(64);

  HidGnubbyDevice.setCid_(f, cid);
  f[4] = cmd;
  f[5] = (u8.length >> 8);
  f[6] = (u8.length & 255);

  var lockArg = (u8.length > 0) ? u8[0] : 0;

  // Fragment over our 64 byte frames.
  var n = 7;
  var seq = 0;
  for (var i = 0; i < u8.length; ++i) {
    f[n++] = u8[i];
    if (n == f.length) {
      this.queueFrame_(f.buffer, cid, cmd, lockArg);

      f = new Uint8Array(64);
      HidGnubbyDevice.setCid_(f, cid);
      cmd = f[4] = seq++;
      n = 5;
    }
  }
  if (n != 5) {
    this.queueFrame_(f.buffer, cid, cmd, lockArg);
  }
};

/**
 * Sets the channel id in the frame.
 * @param {Uint8Array} frame Data frame
 * @param {number} cid The client's channel ID.
 * @private
 */
HidGnubbyDevice.setCid_ = function(frame, cid) {
  frame[0] = cid >>> 24;
  frame[1] = cid >>> 16;
  frame[2] = cid >>> 8;
  frame[3] = cid;
};

/**
 * Updates the lock, and queues the frame for sending. Also begins sending if
 * no other writes are outstanding.
 * @param {ArrayBuffer} frame Data frame
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {number} arg Command argument
 * @private
 */
HidGnubbyDevice.prototype.queueFrame_ = function(frame, cid, cmd, arg) {
  this.updateLock_(cid, cmd, arg);
  var wasEmpty = (this.txqueue.length == 0);
  this.txqueue.push(frame);
  if (wasEmpty) this.writePump_();
};

/**
 * Stuff queued frames from txqueue[] to device, one by one.
 * @private
 */
HidGnubbyDevice.prototype.writePump_ = function() {
  if (!this.dev) return;  // Ignore.

  if (this.txqueue.length == 0) return;  // Done with current queue.

  var frame = this.txqueue[0];

  var self = this;
  function transferComplete() {
    if (chrome.runtime.lastError) {
      console.log(UTIL_fmt('send got lastError:'));
      console.log(UTIL_fmt(chrome.runtime.lastError.message));
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }
    self.txqueue.shift();  // drop sent frame from queue.
    if (self.txqueue.length != 0) {
      window.setTimeout(function() { self.writePump_(); }, 0);
    }
  };

  var u8 = new Uint8Array(frame);

  // See whether this requires scrubbing before logging.
  var alternateLog = Gnubby.hasOwnProperty('redactRequestLog') &&
                     Gnubby['redactRequestLog'](u8);
  if (alternateLog) {
    console.log(UTIL_fmt('>' + alternateLog));
  } else {
    console.log(UTIL_fmt('>' + UTIL_BytesToHex(u8)));
  }

  var u8f = new Uint8Array(64);
  for (var i = 0; i < u8.length; ++i) {
    u8f[i] = u8[i];
  }

  chrome.hid.send(
      this.dev.connectionId,
      0,  // report Id. Must be 0 for our use.
      u8f.buffer,
      transferComplete
  );
};

/**
 * List of legacy HID devices that do not support the F1D0 usage page as
 * mandated by the spec, but still need to be supported.
 * TODO: remove when these devices no longer need to be supported.
 * @const
 */
HidGnubbyDevice.HID_VID_PIDS = [
  {'vendorId': 4176, 'productId': 512}  // Google-specific Yubico HID
];

/**
 * @param {function(Array)} cb Enumeration callback
 * @param {GnubbyEnumerationTypes=} opt_type Which type of enumeration to do.
 */
HidGnubbyDevice.enumerate = function(cb, opt_type) {
  /**
   * One pass using getDevices, and one for each of the hardcoded vid/pids.
   * @const
   */
  var ENUMERATE_PASSES = 1 + HidGnubbyDevice.HID_VID_PIDS.length;
  var numEnumerated = 0;
  var allDevs = [];

  function enumerated(f1d0Enumerated, devs) {
    // Don't double-add a device; it'll just confuse things.
    // We assume the various calls to getDevices() return from the same
    // deviceId pool.
    for (var i = 0; i < devs.length; i++) {
      var dev = devs[i];
      dev.f1d0Only = f1d0Enumerated;
      // Unfortunately indexOf is not usable, since the two calls produce
      // different objects. Compare their deviceIds instead.
      var found = false;
      for (var j = 0; j < allDevs.length; j++) {
        if (allDevs[j].deviceId == dev.deviceId) {
          found = true;
          allDevs[j].f1d0Only &= f1d0Enumerated;
          break;
        }
      }
      if (!found) {
        allDevs.push(dev);
      }
    }
    if (++numEnumerated == ENUMERATE_PASSES) {
      cb(allDevs);
    }
  }

  // Pass 1: usagePage-based enumeration, for FIDO U2F devices. If non-FIDO
  // devices are asked for, "implement" this pass by providing it the empty
  // list. (enumerated requires that it's called once per pass.)
  if (opt_type == GnubbyEnumerationTypes.VID_PID) {
    enumerated(true, []);
  } else {
    chrome.hid.getDevices({filters: [{usagePage: 0xf1d0}]},
        enumerated.bind(null, true));
  }
  // Pass 2: vid/pid-based enumeration, for legacy devices. If FIDO devices
  // are asked for, "implement" this pass by providing it the empty list.
  if (opt_type == GnubbyEnumerationTypes.FIDO_U2F) {
    enumerated(false, []);
  } else {
    for (var i = 0; i < HidGnubbyDevice.HID_VID_PIDS.length; i++) {
      var dev = HidGnubbyDevice.HID_VID_PIDS[i];
      chrome.hid.getDevices({filters: [dev]}, enumerated.bind(null, false));
    }
  }
};

/**
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {number} which The index of the device to open.
 * @param {!chrome.hid.HidDeviceInfo} dev The device to open.
 * @param {function(number, GnubbyDevice=)} cb Called back with the
 *     result of opening the device.
 */
HidGnubbyDevice.open = function(gnubbies, which, dev, cb) {
  chrome.hid.connect(dev.deviceId, function(handle) {
    if (chrome.runtime.lastError) {
      console.log(UTIL_fmt('connect got lastError:'));
      console.log(UTIL_fmt(chrome.runtime.lastError.message));
    }
    if (!handle) {
      console.warn(UTIL_fmt('failed to connect device. permissions issue?'));
      cb(-GnubbyDevice.NODEVICE);
      return;
    }
    var nonNullHandle = /** @type {!chrome.hid.HidConnectInfo} */ (handle);
    var gnubby = new HidGnubbyDevice(gnubbies, nonNullHandle, which);
    cb(-GnubbyDevice.OK, gnubby);
  });
};

/**
 * @param {*} dev A browser API device object
 * @return {GnubbyDeviceId} A device identifier for the device.
 */
HidGnubbyDevice.deviceToDeviceId = function(dev) {
  var hidDev = /** @type {!chrome.hid.HidDeviceInfo} */ (dev);
  var deviceId = {
    namespace: HidGnubbyDevice.NAMESPACE,
    device: hidDev.deviceId
  };
  return deviceId;
};

/**
 * Registers this implementation with gnubbies.
 * @param {Gnubbies} gnubbies Gnubbies registry
 */
HidGnubbyDevice.register = function(gnubbies) {
  var HID_GNUBBY_IMPL = {
    isSharedAccess: true,
    enumerate: HidGnubbyDevice.enumerate,
    deviceToDeviceId: HidGnubbyDevice.deviceToDeviceId,
    open: HidGnubbyDevice.open
  };
  gnubbies.registerNamespace(HidGnubbyDevice.NAMESPACE, HID_GNUBBY_IMPL);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a low-level gnubby driver based on chrome.usb.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {!chrome.usb.ConnectionHandle} dev The device.
 * @param {number} id The device's id.
 * @param {number} inEndpoint The device's in endpoint.
 * @param {number} outEndpoint The device's out endpoint.
 * @constructor
 * @implements {GnubbyDevice}
 */
function UsbGnubbyDevice(gnubbies, dev, id, inEndpoint, outEndpoint) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  this.dev = dev;
  this.id = id;
  this.inEndpoint = inEndpoint;
  this.outEndpoint = outEndpoint;
  this.txqueue = [];
  this.clients = [];
  this.lockCID = 0;     // channel ID of client holding a lock, if != 0.
  this.lockMillis = 0;  // current lock period.
  this.lockTID = null;  // timer id of lock timeout.
  this.closing = false;  // device to be closed by receive loop.
  this.updating = false;  // device firmware is in final stage of updating.
  this.inTransferPending = false;
  this.outTransferPending = false;
}

/**
 * Namespace for the UsbGnubbyDevice implementation.
 * @const
 */
UsbGnubbyDevice.NAMESPACE = 'usb';

/** Destroys this low-level device instance. */
UsbGnubbyDevice.prototype.destroy = function() {
  if (!this.dev) return;  // Already dead.

  this.gnubbies_.removeOpenDevice(
      {namespace: UsbGnubbyDevice.NAMESPACE, device: this.id});
  this.closing = true;

  console.log(UTIL_fmt('UsbGnubbyDevice.destroy()'));

  // Synthesize a close error frame to alert all clients,
  // some of which might be in read state.
  //
  // Use magic CID 0 to address all.
  this.publishFrame_(new Uint8Array([
        0, 0, 0, 0,  // broadcast CID
        GnubbyDevice.CMD_ERROR,
        0, 1,  // length
        GnubbyDevice.GONE]).buffer);

  // Set all clients to closed status and remove them.
  while (this.clients.length != 0) {
    var client = this.clients.shift();
    if (client) client.closed = true;
  }

  if (this.lockTID) {
    window.clearTimeout(this.lockTID);
    this.lockTID = null;
  }

  var dev = this.dev;
  this.dev = null;

  chrome.usb.releaseInterface(dev, 0, function() {
    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('Device ' + dev.handle +
          ' couldn\'t be released:'));
      console.warn(UTIL_fmt(chrome.runtime.lastError.message));
      return;
    }
    console.log(UTIL_fmt('Device ' + dev.handle + ' released'));
    chrome.usb.closeDevice(dev, function() {
      if (chrome.runtime.lastError) {
        console.warn(UTIL_fmt('Device ' + dev.handle +
            ' couldn\'t be closed:'));
        console.warn(UTIL_fmt(chrome.runtime.lastError.message));
        return;
      }
      console.log(UTIL_fmt('Device ' + dev.handle + ' closed'));
    });
  });
};

/**
 * Push frame to all clients.
 * @param {ArrayBuffer} f Data frame
 * @private
 */
UsbGnubbyDevice.prototype.publishFrame_ = function(f) {
  var old = this.clients;

  var remaining = [];
  var changes = false;
  for (var i = 0; i < old.length; ++i) {
    var client = old[i];
    if (client.receivedFrame(f)) {
      // Client still alive; keep on list.
      remaining.push(client);
    } else {
      changes = true;
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(client.cid) + '] left?'));
    }
  }
  if (changes) this.clients = remaining;
};

/**
 * @return {boolean} whether this device is open and ready to use.
 * @private
 */
UsbGnubbyDevice.prototype.readyToUse_ = function() {
  if (this.closing) return false;
  if (!this.dev) return false;

  return true;
};

/**
 * Reads one reply from the low-level device.
 * @private
 */
UsbGnubbyDevice.prototype.readOneReply_ = function() {
  if (!this.readyToUse_()) return;  // No point in continuing.
  if (this.updating) return;  // Do not bother waiting for final update reply.

  var self = this;

  function inTransferComplete(x) {
    self.inTransferPending = false;

    if (!self.readyToUse_()) return;  // No point in continuing.

    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('in bulkTransfer got lastError: '));
      console.warn(UTIL_fmt(chrome.runtime.lastError.message));
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }

    if (x.data) {
      var u8 = new Uint8Array(x.data);
      console.log(UTIL_fmt('<' + UTIL_BytesToHex(u8)));

      self.publishFrame_(x.data);

      // Write another pending request, if any.
      window.setTimeout(
          function() {
            self.txqueue.shift();  // Drop sent frame from queue.
            self.writeOneRequest_();
          },
          0);
    } else {
      console.log(UTIL_fmt('no x.data!'));
      console.log(x);
      window.setTimeout(function() { self.destroy(); }, 0);
    }
  }

  if (this.inTransferPending == false) {
    this.inTransferPending = true;
    chrome.usb.bulkTransfer(
      /** @type {!chrome.usb.ConnectionHandle} */(this.dev),
      { direction: 'in', endpoint: this.inEndpoint, length: 2048 },
      inTransferComplete);
  } else {
    throw 'inTransferPending!';
  }
};

/**
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
UsbGnubbyDevice.prototype.registerClient = function(who) {
  for (var i = 0; i < this.clients.length; ++i) {
    if (this.clients[i] === who) return;  // Already registered.
  }
  this.clients.push(who);
};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 * Returns number of remaining listeners for this device.
 *     if this had no clients to start with.
 */
UsbGnubbyDevice.prototype.deregisterClient = function(who) {
  var current = this.clients;
  if (current.length == 0) return -1;
  this.clients = [];
  for (var i = 0; i < current.length; ++i) {
    var client = current[i];
    if (client !== who) this.clients.push(client);
  }
  return this.clients.length;
};

/**
 * @param {*} who The client.
 * @return {boolean} Whether this device has who as a client.
 */
UsbGnubbyDevice.prototype.hasClient = function(who) {
  if (this.clients.length == 0) return false;
  for (var i = 0; i < this.clients.length; ++i) {
    if (who === this.clients[i])
      return true;
  }
  return false;
};

/**
 * Stuff queued frames from txqueue[] to device, one by one.
 * @private
 */
UsbGnubbyDevice.prototype.writeOneRequest_ = function() {
  if (!this.readyToUse_()) return;  // No point in continuing.

  if (this.txqueue.length == 0) return;  // Nothing to send.

  var frame = this.txqueue[0];

  var self = this;
  function OutTransferComplete(x) {
    self.outTransferPending = false;

    if (!self.readyToUse_()) return;  // No point in continuing.

    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('out bulkTransfer lastError: '));
      console.warn(UTIL_fmt(chrome.runtime.lastError.message));
      window.setTimeout(function() { self.destroy(); }, 0);
      return;
    }

    window.setTimeout(function() { self.readOneReply_(); }, 0);
  };

  var u8 = new Uint8Array(frame);

  // See whether this requires scrubbing before logging.
  var alternateLog = Gnubby.hasOwnProperty('redactRequestLog') &&
                     Gnubby['redactRequestLog'](u8);
  if (alternateLog) {
    console.log(UTIL_fmt('>' + alternateLog));
  } else {
    console.log(UTIL_fmt('>' + UTIL_BytesToHex(u8)));
  }

  if (this.outTransferPending == false) {
    this.outTransferPending = true;
    chrome.usb.bulkTransfer(
        /** @type {!chrome.usb.ConnectionHandle} */(this.dev),
        { direction: 'out', endpoint: this.outEndpoint, data: frame },
        OutTransferComplete);
  } else {
    throw 'outTransferPending!';
  }
};

/**
 * Check whether channel is locked for this request or not.
 * @param {number} cid Channel id
 * @param {number} cmd Command to be sent
 * @return {boolean} true if not locked for this request.
 * @private
 */
UsbGnubbyDevice.prototype.checkLock_ = function(cid, cmd) {
  if (this.lockCID) {
    // We have an active lock.
    if (this.lockCID != cid) {
      // Some other channel has active lock.

      if (cmd != GnubbyDevice.CMD_SYNC &&
          cmd != GnubbyDevice.CMD_INIT) {
        // Anything but SYNC|INIT gets an immediate busy.
        var busy = new Uint8Array(
            [(cid >> 24) & 255,
             (cid >> 16) & 255,
             (cid >> 8) & 255,
             cid & 255,
             GnubbyDevice.CMD_ERROR,
             0, 1,  // length
             GnubbyDevice.BUSY]);
        // Log the synthetic busy too.
        console.log(UTIL_fmt('<' + UTIL_BytesToHex(busy)));
        this.publishFrame_(busy.buffer);
        return false;
      }

      // SYNC|INIT get to go to the device to flush OS tx/rx queues.
      // The usb firmware is to always respond to SYNC|INIT,
      // regardless of lock status.
    }
  }
  return true;
};

/**
 * Update or grab lock.
 * @param {number} cid Channel id
 * @param {number} cmd Command
 * @param {number} arg Command argument
 * @private
 */
UsbGnubbyDevice.prototype.updateLock_ = function(cid, cmd, arg) {
  if (this.lockCID == 0 || this.lockCID == cid) {
    // It is this caller's or nobody's lock.
    if (this.lockTID) {
      window.clearTimeout(this.lockTID);
      this.lockTID = null;
    }

    if (cmd == GnubbyDevice.CMD_LOCK) {
      var nseconds = arg;
      if (nseconds != 0) {
        this.lockCID = cid;
        // Set tracking time to be .1 seconds longer than usb device does.
        this.lockMillis = nseconds * 1000 + 100;
      } else {
        // Releasing lock voluntarily.
        this.lockCID = 0;
      }
    }

    // (re)set the lock timeout if we still hold it.
    if (this.lockCID) {
      var self = this;
      this.lockTID = window.setTimeout(
          function() {
            console.warn(UTIL_fmt(
                'lock for CID ' + Gnubby.hexCid(cid) + ' expired!'));
            self.lockTID = null;
            self.lockCID = 0;
          },
          this.lockMillis);
    }
  }
};

/**
 * Queue command to be sent.
 * If queue was empty, initiate the write.
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command argument data
 */
UsbGnubbyDevice.prototype.queueCommand = function(cid, cmd, data) {
  if (!this.dev) return;
  if (!this.checkLock_(cid, cmd)) return;

  var u8 = new Uint8Array(data);
  var frame = new Uint8Array(u8.length + 7);

  frame[0] = cid >>> 24;
  frame[1] = cid >>> 16;
  frame[2] = cid >>> 8;
  frame[3] = cid;
  frame[4] = cmd;
  frame[5] = (u8.length >> 8);
  frame[6] = (u8.length & 255);

  frame.set(u8, 7);

  var lockArg = (u8.length > 0) ? u8[0] : 0;
  this.updateLock_(cid, cmd, lockArg);

  var wasEmpty = (this.txqueue.length == 0);
  this.txqueue.push(frame.buffer);
  if (wasEmpty) this.writeOneRequest_();
};

/**
 * @const
 */
UsbGnubbyDevice.WINUSB_VID_PIDS = [
  {'vendorId': 4176, 'productId': 529}  // Yubico WinUSB
];

/**
 * @param {function(Array)} cb Enumerate callback
 * @param {GnubbyEnumerationTypes=} opt_type Which type of enumeration to do.
 */
UsbGnubbyDevice.enumerate = function(cb, opt_type) {
  // UsbGnubbyDevices are all non-FIDO devices, so return an empty list if
  // FIDO is what's wanted.
  if (opt_type == GnubbyEnumerationTypes.FIDO_U2F) {
    cb([]);
    return;
  }

  var numEnumerated = 0;
  var allDevs = [];

  function enumerated(devs) {
    allDevs = allDevs.concat(devs);
    if (++numEnumerated == UsbGnubbyDevice.WINUSB_VID_PIDS.length) {
      cb(allDevs);
    }
  }

  for (var i = 0; i < UsbGnubbyDevice.WINUSB_VID_PIDS.length; i++) {
    chrome.usb.getDevices(UsbGnubbyDevice.WINUSB_VID_PIDS[i], enumerated);
  }
};

/**
 * @typedef {?{
 *   address: number,
 *   type: string,
 *   direction: string,
 *   maximumPacketSize: number,
 *   synchronization: (string|undefined),
 *   usage: (string|undefined),
 *   pollingInterval: (number|undefined)
 * }}
 * @see http://developer.chrome.com/apps/usb.html#method-listInterfaces
 */
var InterfaceEndpoint;


/**
 * @typedef {?{
 *   interfaceNumber: number,
 *   alternateSetting: number,
 *   interfaceClass: number,
 *   interfaceSubclass: number,
 *   interfaceProtocol: number,
 *   description: (string|undefined),
 *   endpoints: !Array<!InterfaceEndpoint>
 * }}
 * @see http://developer.chrome.com/apps/usb.html#method-listInterfaces
 */
var InterfaceDescriptor;

/**
 * @param {Gnubbies} gnubbies The gnubbies instances this device is enumerated
 *     in.
 * @param {number} which The index of the device to open.
 * @param {!chrome.usb.Device} dev The device to open.
 * @param {function(number, GnubbyDevice=)} cb Called back with the
 *     result of opening the device.
 */
UsbGnubbyDevice.open = function(gnubbies, which, dev, cb) {
  /** @param {chrome.usb.ConnectionHandle=} handle Connection handle */
  function deviceOpened(handle) {
    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('openDevice got lastError:'));
      console.warn(UTIL_fmt(chrome.runtime.lastError.message));
      console.warn(UTIL_fmt('failed to open device. permissions issue?'));
      cb(-GnubbyDevice.NODEVICE);
      return;
    }
    var nonNullHandle = /** @type {!chrome.usb.ConnectionHandle} */ (handle);
    chrome.usb.listInterfaces(nonNullHandle, function(descriptors) {
      var inEndpoint, outEndpoint;
      for (var i = 0; i < descriptors.length; i++) {
        var descriptor = /** @type {InterfaceDescriptor} */ (descriptors[i]);
        for (var j = 0; j < descriptor.endpoints.length; j++) {
          var endpoint = descriptor.endpoints[j];
          if (inEndpoint == undefined && endpoint.type == 'bulk' &&
              endpoint.direction == 'in') {
            inEndpoint = endpoint.address;
          }
          if (outEndpoint == undefined && endpoint.type == 'bulk' &&
              endpoint.direction == 'out') {
            outEndpoint = endpoint.address;
          }
        }
      }
      if (inEndpoint == undefined || outEndpoint == undefined) {
        console.warn(UTIL_fmt('device lacking an endpoint (broken?)'));
        chrome.usb.closeDevice(nonNullHandle);
        cb(-GnubbyDevice.NODEVICE);
        return;
      }
      // Try getting it claimed now.
      chrome.usb.claimInterface(nonNullHandle, 0, function() {
        if (chrome.runtime.lastError) {
          console.warn(UTIL_fmt('lastError: ' + chrome.runtime.lastError));
          console.log(chrome.runtime.lastError);
        }
        var claimed = !chrome.runtime.lastError;
        if (!claimed) {
          console.warn(UTIL_fmt('failed to claim interface. busy?'));
          // Claim failed? Let the callers know and bail out.
          chrome.usb.closeDevice(nonNullHandle);
          cb(-GnubbyDevice.BUSY);
          return;
        }
        var gnubby = new UsbGnubbyDevice(gnubbies, nonNullHandle, which,
            inEndpoint, outEndpoint);
        cb(-GnubbyDevice.OK, gnubby);
      });
    });
  }

  if (UsbGnubbyDevice.runningOnCrOS === undefined) {
    UsbGnubbyDevice.runningOnCrOS =
        (window.navigator.appVersion.indexOf('; CrOS ') != -1);
  }
  if (UsbGnubbyDevice.runningOnCrOS) {
    chrome.usb.requestAccess(dev, 0, function(success) {
      // Even though the argument to requestAccess is a chrome.usb.Device, the
      // access request is for access to all devices with the same vid/pid.
      // Curiously, if the first chrome.usb.requestAccess succeeds, a second
      // call with a separate device with the same vid/pid fails. Since
      // chrome.usb.openDevice will fail if a previous access request really
      // failed, just ignore the outcome of the access request and move along.
      chrome.usb.openDevice(dev, deviceOpened);
    });
  } else {
    chrome.usb.openDevice(dev, deviceOpened);
  }
};

/**
 * @param {*} dev Chrome usb device
 * @return {GnubbyDeviceId} A device identifier for the device.
 */
UsbGnubbyDevice.deviceToDeviceId = function(dev) {
  var usbDev = /** @type {!chrome.usb.Device} */ (dev);
  var deviceId = {
    namespace: UsbGnubbyDevice.NAMESPACE,
    device: usbDev.device
  };
  return deviceId;
};

/**
 * Registers this implementation with gnubbies.
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 */
UsbGnubbyDevice.register = function(gnubbies) {
  var USB_GNUBBY_IMPL = {
    isSharedAccess: false,
    enumerate: UsbGnubbyDevice.enumerate,
    deviceToDeviceId: UsbGnubbyDevice.deviceToDeviceId,
    open: UsbGnubbyDevice.open
  };
  gnubbies.registerNamespace(UsbGnubbyDevice.NAMESPACE, USB_GNUBBY_IMPL);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview A class for managing all enumerated gnubby devices.
 */
'use strict';

/**
 * @typedef {{
 *   namespace: string,
 *   device: number
 * }}
 */
var GnubbyDeviceId;

/**
 * Ways in which gnubby devices are enumerated.
 * @const
 * @enum {number}
 */
var GnubbyEnumerationTypes = {
  ANY: 0,
  VID_PID: 1,
  FIDO_U2F: 2
};

/**
 * @typedef {{
 *   isSharedAccess: boolean,
 *   enumerate: function(function(Array), GnubbyEnumerationTypes=),
 *   deviceToDeviceId: function(*): GnubbyDeviceId,
 *   open: function(Gnubbies, number, *, function(number, GnubbyDevice=)),
 *   cancelOpen: (undefined|function(Gnubbies, number, *))
 * }}
 */
var GnubbyNamespaceImpl;

/**
 * Manager of opened devices.
 * @constructor
 */
function Gnubbies() {
  /** @private {Object<string, Array>} */
  this.devs_ = {};
  this.pendingEnumerate = [];  // clients awaiting an enumerate
  /**
   * The distinct namespaces registered in this Gnubbies instance, in order of
   * registration.
   * @private {Array<string>}
   */
  this.namespaces_ = [];
  /** @private {Object<string, GnubbyNamespaceImpl>} */
  this.impl_ = {};
  /** @private {Object<string, Object<number|string, !GnubbyDevice>>} */
  this.openDevs_ = {};
  /** @private {Object<string, Object<number, *>>} */
  this.pendingOpens_ = {};  // clients awaiting an open
}

/**
 * Registers a new gnubby namespace, i.e. an implementation of the
 * enumerate/open functions for all devices within a namespace.
 * @param {string} namespace The namespace of the numerator, e.g. 'usb'.
 * @param {GnubbyNamespaceImpl} impl The implementation.
 */
Gnubbies.prototype.registerNamespace = function(namespace, impl) {
  if (!this.impl_.hasOwnProperty(namespace)) {
    this.namespaces_.push(namespace);
  }
  this.impl_[namespace] = impl;
};

/**
 * @param {GnubbyDeviceId} id The device id.
 * @return {boolean} Whether the device is a shared access device.
 */
Gnubbies.prototype.isSharedAccess = function(id) {
  if (!this.impl_.hasOwnProperty(id.namespace)) return false;
  return this.impl_[id.namespace].isSharedAccess;
};

/**
 * @param {GnubbyDeviceId} which The device to remove.
 */
Gnubbies.prototype.removeOpenDevice = function(which) {
  if (this.openDevs_[which.namespace] &&
      this.openDevs_[which.namespace].hasOwnProperty(which.device)) {
    delete this.openDevs_[which.namespace][which.device];
  }
};

/** Close all enumerated devices. */
Gnubbies.prototype.closeAll = function() {
  if (this.inactivityTimer) {
    this.inactivityTimer.clearTimeout();
    this.inactivityTimer = undefined;
  }
  // Close and stop talking to any gnubbies we have enumerated.
  for (var namespace in this.openDevs_) {
    for (var dev in this.openDevs_[namespace]) {
      var deviceId = Number(dev);
      this.openDevs_[namespace][deviceId].destroy();
    }
  }
  this.devs_ = {};
  this.openDevs_ = {};
};

/**
 * @param {string} namespace
 * @return {function(*)} deviceToDeviceId method associated with given namespace
 * @private
 */
Gnubbies.prototype.getDeviceToDeviceId_ = function(namespace) {
  return this.impl_[namespace].deviceToDeviceId;
};

/**
 * @param {function(number, Array<GnubbyDeviceId>)} cb Called back with the
 *     result of enumerating.
 * @param {GnubbyEnumerationTypes=} opt_type Which type of enumeration to do.
 */
Gnubbies.prototype.enumerate = function(cb, opt_type) {
  if (!cb) {
    cb = function(rc, indexes) {
      var msg = 'defaultEnumerateCallback(' + rc;
      if (indexes) {
        msg += ', [';
        for (var i = 0; i < indexes.length; i++) {
          msg += JSON.stringify(indexes[i]);
        }
        msg += ']';
      }
      msg += ')';
      console.log(UTIL_fmt(msg));
    };
  }

  if (!this.namespaces_.length) {
    cb(-GnubbyDevice.OK, []);
    return;
  }

  var namespacesEnumerated = 0;
  var self = this;

  /**
   * @param {string} namespace The namespace that was enumerated.
   * @param {Array<GnubbyDeviceId>} existingDeviceIds Previously enumerated
   *     device IDs (from other namespaces), if any.
   * @param {Array} devs The devices in the namespace.
   */
  function enumerated(namespace, existingDeviceIds, devs) {
    namespacesEnumerated++;
    var lastNamespace = (namespacesEnumerated == self.namespaces_.length);

    if (chrome.runtime.lastError) {
      console.warn(UTIL_fmt('lastError: ' + chrome.runtime.lastError));
      console.log(chrome.runtime.lastError);
      devs = [];
    }

    console.log(UTIL_fmt('Enumerated ' + devs.length + ' gnubbies'));
    console.log(devs);

    var presentDevs = {};
    var deviceIds = [];
    var deviceToDeviceId = self.getDeviceToDeviceId_(namespace);
    for (var i = 0; i < devs.length; ++i) {
      var deviceId = deviceToDeviceId(devs[i]);
      deviceIds.push(deviceId);
      presentDevs[deviceId.device] = devs[i];
    }

    var toRemove = [];
    for (var dev in self.openDevs_[namespace]) {
      if (!presentDevs.hasOwnProperty(dev)) {
        toRemove.push(dev);
      }
    }

    for (var i = 0; i < toRemove.length; i++) {
      dev = toRemove[i];
      if (self.openDevs_[namespace][dev]) {
        self.openDevs_[namespace][dev].destroy();
        delete self.openDevs_[namespace][dev];
      }
    }

    self.devs_[namespace] = devs;
    existingDeviceIds.push.apply(existingDeviceIds, deviceIds);
    if (lastNamespace) {
      while (self.pendingEnumerate.length != 0) {
        var cb = self.pendingEnumerate.shift();
        cb(-GnubbyDevice.OK, existingDeviceIds);
      }
    }
  }

  var deviceIds = [];
  function makeEnumerateCb(namespace) {
    return function(devs) {
      enumerated(namespace, deviceIds, devs);
    }
  }

  this.pendingEnumerate.push(cb);
  if (this.pendingEnumerate.length == 1) {
    for (var i = 0; i < this.namespaces_.length; i++) {
      var namespace = this.namespaces_[i];
      var enumerator = this.impl_[namespace].enumerate;
      enumerator(makeEnumerateCb(namespace), opt_type);
    }
  }
};

/**
 * Amount of time past last activity to set the inactivity timer to, in millis.
 * @const
 */
Gnubbies.INACTIVITY_TIMEOUT_MARGIN_MILLIS = 30000;

/**
 * Private instance of timers based on window's timer functions.
 * @const
 * @private
 */
Gnubbies.SYS_TIMER_ = new WindowTimer();

/**
 * @param {number|undefined} opt_timeoutMillis Timeout in milliseconds
 */
Gnubbies.prototype.resetInactivityTimer = function(opt_timeoutMillis) {
  var millis = opt_timeoutMillis ?
      opt_timeoutMillis + Gnubbies.INACTIVITY_TIMEOUT_MARGIN_MILLIS :
      Gnubbies.INACTIVITY_TIMEOUT_MARGIN_MILLIS;
  if (!this.inactivityTimer) {
    this.inactivityTimer =
        new CountdownTimer(
            Gnubbies.SYS_TIMER_, millis, this.inactivityTimeout_.bind(this));
  } else if (millis > this.inactivityTimer.millisecondsUntilExpired()) {
    this.inactivityTimer.clearTimeout();
    this.inactivityTimer.setTimeout(millis, this.inactivityTimeout_.bind(this));
  }
};

/**
 * Called when the inactivity timeout expires.
 * @private
 */
Gnubbies.prototype.inactivityTimeout_ = function() {
  this.inactivityTimer = undefined;
  for (var namespace in this.openDevs_) {
    for (var dev in this.openDevs_[namespace]) {
      var deviceId = Number(dev);
      console.warn(namespace + ' device ' + deviceId +
          ' still open after inactivity, closing');
      this.openDevs_[namespace][deviceId].destroy();
    }
  }
};

/**
 * Opens and adds a new client of the specified device.
 * @param {GnubbyDeviceId} which Which device to open.
 * @param {*} who Client of the device.
 * @param {function(number, GnubbyDevice=)} cb Called back with the result of
 *     opening the device.
 */
Gnubbies.prototype.addClient = function(which, who, cb) {
  this.resetInactivityTimer();

  var self = this;

  function opened(gnubby, who, cb) {
    if (gnubby.closing) {
      // Device is closing or already closed.
      self.removeClient(gnubby, who);
      if (cb) { cb(-GnubbyDevice.NODEVICE); }
    } else {
      gnubby.registerClient(who);
      if (cb) { cb(-GnubbyDevice.OK, gnubby); }
    }
  }

  function notifyOpenResult(rc) {
    if (self.pendingOpens_[which.namespace]) {
      while (self.pendingOpens_[which.namespace][which.device].length != 0) {
        var client = self.pendingOpens_[which.namespace][which.device].shift();
        client.cb(rc);
      }
      delete self.pendingOpens_[which.namespace][which.device];
    }
  }

  var dev = null;
  var deviceToDeviceId = this.getDeviceToDeviceId_(which.namespace);
  if (this.devs_[which.namespace]) {
    for (var i = 0; i < this.devs_[which.namespace].length; i++) {
      var device = this.devs_[which.namespace][i];
      if (deviceToDeviceId(device).device == which.device) {
        dev = device;
        break;
      }
    }
  }
  if (!dev) {
    // Index out of bounds. Device does not exist in current enumeration.
    this.removeClient(null, who);
    if (cb) { cb(-GnubbyDevice.NODEVICE); }
    return;
  }

  function openCb(rc, opt_gnubby) {
    if (rc) {
      notifyOpenResult(rc);
      return;
    }
    if (!opt_gnubby) {
      notifyOpenResult(-GnubbyDevice.NODEVICE);
      return;
    }
    var gnubby = /** @type {!GnubbyDevice} */ (opt_gnubby);
    if (!self.openDevs_[which.namespace]) {
      self.openDevs_[which.namespace] = {};
    }
    self.openDevs_[which.namespace][which.device] = gnubby;
    while (self.pendingOpens_[which.namespace][which.device].length != 0) {
      var client = self.pendingOpens_[which.namespace][which.device].shift();
      opened(gnubby, client.who, client.cb);
    }
    delete self.pendingOpens_[which.namespace][which.device];
  }

  if (this.openDevs_[which.namespace] &&
      this.openDevs_[which.namespace].hasOwnProperty(which.device)) {
    var gnubby = this.openDevs_[which.namespace][which.device];
    opened(gnubby, who, cb);
  } else {
    var opener = {who: who, cb: cb};
    if (!this.pendingOpens_.hasOwnProperty(which.namespace)) {
      this.pendingOpens_[which.namespace] = {};
    }
    if (this.pendingOpens_[which.namespace].hasOwnProperty(which.device)) {
      this.pendingOpens_[which.namespace][which.device].push(opener);
    } else {
      this.pendingOpens_[which.namespace][which.device] = [opener];
      var openImpl = this.impl_[which.namespace].open;
      openImpl(this, which.device, dev, openCb);
    }
  }
};

/**
 * Called to cancel add client operation
 * @param {GnubbyDeviceId} which Which device to cancel open.
 */
Gnubbies.prototype.cancelAddClient = function(which) {
  var dev = null;
  var deviceToDeviceId = this.getDeviceToDeviceId_(which.namespace);
  if (this.devs_[which.namespace]) {
    for (var i = 0; i < this.devs_[which.namespace].length; i++) {
      var device = this.devs_[which.namespace][i];
      if (deviceToDeviceId(device).device == which.device) {
        dev = device;
        break;
      }
    }
  }

  if (!dev) {
    return;
  }

  if (this.pendingOpens_[which.namespace] &&
      this.pendingOpens_[which.namespace][which.device]) {
    var cancelOpenImpl = this.impl_[which.namespace].cancelOpen;
    if (cancelOpenImpl)
      cancelOpenImpl(this, which.device, dev);
  }
};

/**
 * Removes a client from a low-level gnubby.
 * @param {GnubbyDevice} whichDev The gnubby.
 * @param {*} who The client.
 */
Gnubbies.prototype.removeClient = function(whichDev, who) {
  console.log(UTIL_fmt('Gnubbies.removeClient()'));

  this.resetInactivityTimer();

  // De-register client from all known devices.
  for (var namespace in this.openDevs_) {
    for (var devId in this.openDevs_[namespace]) {
      var deviceId = Number(devId);
      if (isNaN(deviceId))
        deviceId = devId;
      var dev = this.openDevs_[namespace][deviceId];
      if (dev.hasClient(who)) {
        if (whichDev && dev != whichDev) {
          console.warn('Gnubby attached to more than one device!?');
        }
        if (!dev.deregisterClient(who)) {
          dev.destroy();
        }
      }
    }
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a client view of a gnubby, aka USB security key.
 */
'use strict';

/**
 * Creates a Gnubby client. There may be more than one simultaneous Gnubby
 * client of a physical device. This client manages multiplexing access to the
 * low-level device to maintain the illusion that it is the only client of the
 * device.
 * @constructor
 * @param {number=} opt_busySeconds to retry an exchange upon a BUSY result.
 */
function Gnubby(opt_busySeconds) {
  this.dev = null;
  this.gnubbyInstance = ++Gnubby.gnubbyId_;
  this.cid = Gnubby.BROADCAST_CID;
  this.rxframes = [];
  this.synccnt = 0;
  this.rxcb = null;
  this.closed = false;
  this.commandPending = false;
  this.notifyOnClose = [];
  this.busyMillis = (opt_busySeconds ? opt_busySeconds * 1000 : 9500);
}

/**
 * Global Gnubby instance counter.
 * @private {number}
 */
Gnubby.gnubbyId_ = 0;

/**
 * Sets Gnubby's Gnubbies singleton.
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 */
Gnubby.setGnubbies = function(gnubbies) {
  /** @private {Gnubbies} */
  Gnubby.gnubbies_ = gnubbies;
};

/**
 * Return cid as hex string.
 * @param {number} cid to convert.
 * @return {string} hexadecimal string.
 */
Gnubby.hexCid = function(cid) {
  var tmp = [(cid >>> 24) & 255,
             (cid >>> 16) & 255,
             (cid >>> 8) & 255,
             (cid >>> 0) & 255];
  return UTIL_BytesToHex(tmp);
};

/**
 * Cancels open attempt for this gnubby, if available.
 */
Gnubby.prototype.cancelOpen = function() {
  if (this.which)
    Gnubby.gnubbies_.cancelAddClient(this.which);
};

/**
 * Opens the gnubby with the given index, or the first found gnubby if no
 * index is specified.
 * @param {GnubbyDeviceId} which The device to open. If null, the first
 *     gnubby found is opened.
 * @param {GnubbyEnumerationTypes=} opt_type Which type of device to enumerate.
 * @param {function(number)|undefined} opt_cb Called with result of opening the
 *     gnubby.
 * @param {string=} opt_caller Identifier for the caller.
 */
Gnubby.prototype.open = function(which, opt_type, opt_cb, opt_caller) {
  var cb = opt_cb ? opt_cb : Gnubby.defaultCallback;
  if (this.closed) {
    cb(-GnubbyDevice.NODEVICE);
    return;
  }
  this.closingWhenIdle = false;
  if (opt_caller) {
    this.caller_ = opt_caller;
  }

  var self = this;

  function setCid(which) {
    // Set a default channel ID, in case the caller never sets a better one.
    self.cid = Gnubby.defaultChannelId_(self.gnubbyInstance, which);
  }

  var enumerateRetriesRemaining = 3;
  function enumerated(rc, devs) {
    if (!devs.length)
      rc = -GnubbyDevice.NODEVICE;
    if (rc) {
      cb(rc);
      return;
    }
    which = devs[0];
    setCid(which);
    self.which = which;
    Gnubby.gnubbies_.addClient(which, self, function(rc, device) {
      if (rc == -GnubbyDevice.NODEVICE && enumerateRetriesRemaining-- > 0) {
        // We were trying to open the first device, but now it's not there?
        // Do over.
        Gnubby.gnubbies_.enumerate(enumerated, opt_type);
        return;
      }
      self.dev = device;
      cb(rc);
    });
  }

  if (which) {
    setCid(which);
    self.which = which;
    Gnubby.gnubbies_.addClient(which, self, function(rc, device) {
      self.dev = device;
      cb(rc);
    });
  } else {
    Gnubby.gnubbies_.enumerate(enumerated, opt_type);
  }
};

/**
 * Generates a default channel id value for a gnubby instance that won't
 * collide within this application, but may when others simultaneously access
 * the device.
 * @param {number} gnubbyInstance An instance identifier for a gnubby.
 * @param {GnubbyDeviceId} which The device identifer for the gnubby device.
 * @return {number} The channel id.
 * @private
 */
Gnubby.defaultChannelId_ = function(gnubbyInstance, which) {
  var cid = (gnubbyInstance) & 0x00ffffff;
  cid |= ((which.device + 1) << 24);  // For debugging.
  return cid;
};

/**
 * @return {boolean} Whether this gnubby has any command outstanding.
 * @private
 */
Gnubby.prototype.inUse_ = function() {
  return this.commandPending;
};

/** Closes this gnubby. */
Gnubby.prototype.close = function() {
  this.closed = true;

  if (this.dev) {
    console.log(UTIL_fmt('Gnubby.close()'));
    this.rxframes = [];
    this.rxcb = null;
    var dev = this.dev;
    this.dev = null;
    var self = this;
    // Wait a bit in case simpleton client tries open next gnubby.
    // Without delay, gnubbies would drop all idle devices, before client
    // gets to the next one.
    window.setTimeout(
        function() {
          Gnubby.gnubbies_.removeClient(dev, self);
        }, 300);
  }
};

/**
 * Asks this gnubby to close when it gets a chance.
 * @param {Function=} cb called back when closed.
 */
Gnubby.prototype.closeWhenIdle = function(cb) {
  if (!this.inUse_()) {
    this.close();
    if (cb) cb();
    return;
  }
  this.closingWhenIdle = true;
  if (cb) this.notifyOnClose.push(cb);
};

/**
 * Close and notify every caller that it is now closed.
 * @private
 */
Gnubby.prototype.idleClose_ = function() {
  this.close();
  while (this.notifyOnClose.length != 0) {
    var cb = this.notifyOnClose.shift();
    cb();
  }
};

/**
 * Notify callback for every frame received.
 * @param {function()} cb Callback
 * @private
 */
Gnubby.prototype.notifyFrame_ = function(cb) {
  if (this.rxframes.length != 0) {
    // Already have frames; continue.
    if (cb) window.setTimeout(cb, 0);
  } else {
    this.rxcb = cb;
  }
};

/**
 * Called by low level driver with a frame.
 * @param {ArrayBuffer|Uint8Array} frame Data frame
 * @return {boolean} Whether this client is still interested in receiving
 *     frames from its device.
 */
Gnubby.prototype.receivedFrame = function(frame) {
  if (this.closed) return false;  // No longer interested.

  if (!this.checkCID_(frame)) {
    // Not for me, ignore.
    return true;
  }

  this.rxframes.push(frame);

  // Callback self in case we were waiting. Once.
  var cb = this.rxcb;
  this.rxcb = null;
  if (cb) window.setTimeout(cb, 0);

  return true;
};

/**
 * @return {ArrayBuffer|Uint8Array} oldest received frame. Throw if none.
 * @private
 */
Gnubby.prototype.readFrame_ = function() {
  if (this.rxframes.length == 0) throw 'rxframes empty!';

  var frame = this.rxframes.shift();
  return frame;
};

/** Poll from rxframes[].
 * @param {number} cmd Command
 * @param {number} timeout timeout in seconds.
 * @param {?function(...)} cb Callback
 * @private
 */
Gnubby.prototype.read_ = function(cmd, timeout, cb) {
  if (this.closed) { cb(-GnubbyDevice.GONE); return; }
  if (!this.dev) { cb(-GnubbyDevice.GONE); return; }

  var tid = null;  // timeout timer id.
  var callback = cb;
  var self = this;

  var msg = null;
  var seqno = 0;
  var count = 0;

  /**
   * Schedule call to cb if not called yet.
   * @param {number} a Return code.
   * @param {Object=} b Optional data.
   */
  function schedule_cb(a, b) {
    self.commandPending = false;
    if (tid) {
      // Cancel timeout timer.
      window.clearTimeout(tid);
      tid = null;
    }
    var c = callback;
    if (c) {
      callback = null;
      window.setTimeout(function() { c(a, b); }, 0);
    }
    if (self.closingWhenIdle) self.idleClose_();
  };

  function read_timeout() {
    if (!callback || !tid) return;  // Already done.

    console.error(UTIL_fmt(
        '[' + Gnubby.hexCid(self.cid) + '] timeout!'));

    if (self.dev) {
      self.dev.destroy();  // Stop pretending this thing works.
    }

    tid = null;

    schedule_cb(-GnubbyDevice.TIMEOUT);
  };

  function cont_frame() {
    if (!callback || !tid) return;  // Already done.

    var f = new Uint8Array(self.readFrame_());
    var rcmd = f[4];
    var totalLen = (f[5] << 8) + f[6];

    if (rcmd == GnubbyDevice.CMD_ERROR && totalLen == 1) {
      // Error from device; forward.
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(self.cid) + '] error frame ' +
          UTIL_BytesToHex(f)));
      if (f[7] == GnubbyDevice.GONE) {
        self.closed = true;
      }
      schedule_cb(-f[7]);
      return;
    }

    if ((rcmd & 0x80)) {
      // Not an CONT frame, ignore.
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(self.cid) + '] ignoring non-cont frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(cont_frame);
      return;
    }

    var seq = (rcmd & 0x7f);
    if (seq != seqno++) {
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(self.cid) + '] bad cont frame ' +
          UTIL_BytesToHex(f)));
      schedule_cb(-GnubbyDevice.INVALID_SEQ);
      return;
    }

    // Copy payload.
    for (var i = 5; i < f.length && count < msg.length; ++i) {
      msg[count++] = f[i];
    }

    if (count == msg.length) {
      // Done.
      schedule_cb(-GnubbyDevice.OK, msg.buffer);
    } else {
      // Need more CONT frame(s).
      self.notifyFrame_(cont_frame);
    }
  }

  function init_frame() {
    if (!callback || !tid) return;  // Already done.

    var f = new Uint8Array(self.readFrame_());

    var rcmd = f[4];
    var totalLen = (f[5] << 8) + f[6];

    if (rcmd == GnubbyDevice.CMD_ERROR && totalLen == 1) {
      // Error from device; forward.
      // Don't log busy frames, they're "normal".
      if (f[7] != GnubbyDevice.BUSY) {
        console.log(UTIL_fmt(
            '[' + Gnubby.hexCid(self.cid) + '] error frame ' +
            UTIL_BytesToHex(f)));
      }
      if (f[7] == GnubbyDevice.GONE) {
        self.closed = true;
      }
      schedule_cb(-f[7]);
      return;
    }

    if (!(rcmd & 0x80)) {
      // Not an init frame, ignore.
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(self.cid) + '] ignoring non-init frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(init_frame);
      return;
    }

    if (rcmd != cmd) {
      // Not expected ack, read more.
      console.log(UTIL_fmt(
          '[' + Gnubby.hexCid(self.cid) + '] ignoring non-ack frame ' +
          UTIL_BytesToHex(f)));
      self.notifyFrame_(init_frame);
      return;
    }

    // Copy payload.
    msg = new Uint8Array(totalLen);
    for (var i = 7; i < f.length && count < msg.length; ++i) {
      msg[count++] = f[i];
    }

    if (count == msg.length) {
      // Done.
      schedule_cb(-GnubbyDevice.OK, msg.buffer);
    } else {
      // Need more CONT frame(s).
      self.notifyFrame_(cont_frame);
    }
  }

  // Start timeout timer.
  tid = window.setTimeout(read_timeout, 1000.0 * timeout);

  // Schedule read of first frame.
  self.notifyFrame_(init_frame);
};

/**
  * @const
  */
Gnubby.NOTIFICATION_CID = 0;

/**
  * @const
  */
Gnubby.BROADCAST_CID = (0xff << 24) | (0xff << 16) | (0xff << 8) | 0xff;

/**
 * @param {ArrayBuffer|Uint8Array} frame Data frame
 * @return {boolean} Whether frame is for my channel.
 * @private
 */
Gnubby.prototype.checkCID_ = function(frame) {
  var f = new Uint8Array(frame);
  var c = (f[0] << 24) |
          (f[1] << 16) |
          (f[2] << 8) |
          (f[3]);
  return c === this.cid ||
         c === Gnubby.NOTIFICATION_CID;
};

/**
 * Queue command for sending.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @private
 */
Gnubby.prototype.write_ = function(cmd, data) {
  if (this.closed) return;
  if (!this.dev) return;

  this.commandPending = true;

  this.dev.queueCommand(this.cid, cmd, data);
};

/**
 * Writes the command, and calls back when the command's reply is received.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {number} timeout Timeout in seconds.
 * @param {function(number, ArrayBuffer=)} cb Callback
 * @private
 */
Gnubby.prototype.exchange_ = function(cmd, data, timeout, cb) {
  var busyWait = new CountdownTimer(Gnubby.SYS_TIMER_, this.busyMillis);
  var self = this;

  function retryBusy(rc, rc_data) {
    if (rc == -GnubbyDevice.BUSY && !busyWait.expired()) {
      if (Gnubby.gnubbies_) {
        Gnubby.gnubbies_.resetInactivityTimer(timeout * 1000);
      }
      self.write_(cmd, data);
      self.read_(cmd, timeout, retryBusy);
    } else {
      busyWait.clearTimeout();
      cb(rc, rc_data);
    }
  }

  retryBusy(-GnubbyDevice.BUSY, undefined);  // Start work.
};

/**
 * Private instance of timers based on window's timer functions.
 * @const
 * @private
 */
Gnubby.SYS_TIMER_ = new WindowTimer();

/** Default callback for commands. Simply logs to console.
 * @param {number} rc Result status code
 * @param {(ArrayBuffer|Uint8Array|Array<number>|null)} data Result data
 */
Gnubby.defaultCallback = function(rc, data) {
  var msg = 'defaultCallback(' + rc;
  if (data) {
    if (typeof data == 'string') msg += ', ' + data;
    else msg += ', ' + UTIL_BytesToHex(new Uint8Array(data));
  }
  msg += ')';
  console.log(UTIL_fmt(msg));
};

/**
 * Ensures this device has temporary ownership of the USB device, by:
 * 1. Using the INIT command to allocate an unique channel id, if one hasn't
 *    been retrieved before, or
 * 2. Sending a nonce to device, flushing read queue until match.
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.sync = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  if (this.closed) {
    cb(-GnubbyDevice.GONE);
    return;
  }

  var done = false;
  var trycount = 6;
  var tid = null;
  var self = this;

  function returnValue(rc) {
    done = true;
    window.setTimeout(cb.bind(null, rc), 0);
    if (self.closingWhenIdle) self.idleClose_();
  }

  function callback(rc, opt_frame) {
    self.commandPending = false;
    if (tid) {
      window.clearTimeout(tid);
      tid = null;
    }
    completionAction(rc, opt_frame);
  }

  function sendSyncSentinel() {
    var cmd = GnubbyDevice.CMD_SYNC;
    var data = new Uint8Array(1);
    data[0] = ++self.synccnt;
    self.dev.queueCommand(self.cid, cmd, data.buffer);
  }

  function syncSentinelEquals(f) {
    return (f[4] == GnubbyDevice.CMD_SYNC &&
        (f.length == 7 || /* fw pre-0.2.1 bug: does not echo sentinel */
         f[7] == self.synccnt));
  }

  function syncCompletionAction(rc, opt_frame) {
    if (rc) console.warn(UTIL_fmt('sync failed: ' + rc));
    returnValue(rc);
  }

  function sendInitSentinel() {
    var cid = self.cid;
    // If we do not have a specific CID yet, reset to BROADCAST for init.
    if (self.cid == Gnubby.defaultChannelId_(self.gnubbyInstance, self.which)) {
      self.cid = Gnubby.BROADCAST_CID;
      cid = self.cid;
    }
    var cmd = GnubbyDevice.CMD_INIT;
    self.dev.queueCommand(cid, cmd, nonce);
  }

  function initSentinelEquals(f) {
    return (f[4] == GnubbyDevice.CMD_INIT &&
        f.length >= nonce.length + 7 &&
        UTIL_equalArrays(f.subarray(7, nonce.length + 7), nonce));
  }

  function initCmdUnsupported(rc) {
    // Different firmwares fail differently on different inputs, so treat any
    // of the following errors as indicating the INIT command isn't supported.
    return rc == -GnubbyDevice.INVALID_CMD ||
        rc == -GnubbyDevice.INVALID_PAR ||
        rc == -GnubbyDevice.INVALID_LEN;
  }

  function initCompletionAction(rc, opt_frame) {
    // Actual failures: bail out.
    if (rc && !initCmdUnsupported(rc)) {
      console.warn(UTIL_fmt('init failed: ' + rc));
      returnValue(rc);
    }

    var HEADER_LENGTH = 7;
    var MIN_LENGTH = HEADER_LENGTH + 4;  // 4 bytes for the channel id
    if (rc || !opt_frame || opt_frame.length < nonce.length + MIN_LENGTH) {
      // INIT command not supported or is missing the returned channel id:
      // Pick a random cid to try to prevent collisions on the USB bus.
      var rnd = UTIL_getRandom(2);
      self.cid = Gnubby.defaultChannelId_(self.gnubbyInstance, self.which);
      self.cid ^= (rnd[0] << 16) | (rnd[1] << 8);
      // Now sync with that cid, to make sure we've got it.
      setSync();
      timeoutLoop();
      return;
    }
    // Accept the provided cid.
    var offs = HEADER_LENGTH + nonce.length;
    self.cid = (opt_frame[offs] << 24) |
               (opt_frame[offs + 1] << 16) |
               (opt_frame[offs + 2] << 8) |
               opt_frame[offs + 3];
    returnValue(rc);
  }

  function checkSentinel() {
    var f = new Uint8Array(self.readFrame_());

    // Stop on errors and return them.
    if (f[4] == GnubbyDevice.CMD_ERROR &&
        f[5] == 0 && f[6] == 1) {
      if (f[7] == GnubbyDevice.BUSY) {
        // Not spec but some devices do this; retry.
        sendSentinel();
        self.notifyFrame_(checkSentinel);
        return;
      }
      if (f[7] == GnubbyDevice.GONE) {
        // Device disappeared on us.
        self.closed = true;
      }
      callback(-f[7]);
      return;
    }

    // Eat everything else but expected sentinel reply.
    if (!sentinelEquals(f)) {
      // Read more.
      self.notifyFrame_(checkSentinel);
      return;
    }

    // Done.
    callback(-GnubbyDevice.OK, f);
  };

  function timeoutLoop() {
    if (done) return;

    if (trycount == 0) {
      // Failed.
      callback(-GnubbyDevice.TIMEOUT);
      return;
    }

    --trycount;  // Try another one.
    sendSentinel();
    self.notifyFrame_(checkSentinel);
    tid = window.setTimeout(timeoutLoop, 500);
  };

  var sendSentinel;
  var sentinelEquals;
  var nonce;
  var completionAction;

  function setInit() {
    sendSentinel = sendInitSentinel;
    nonce = UTIL_getRandom(8);
    sentinelEquals = initSentinelEquals;
    completionAction = initCompletionAction;
  }

  function setSync() {
    sendSentinel = sendSyncSentinel;
    sentinelEquals = syncSentinelEquals;
    completionAction = syncCompletionAction;
  }

  if (Gnubby.gnubbies_.isSharedAccess(this.which)) {
    setInit();
  } else {
    setSync();
  }
  timeoutLoop();
};

/** Short timeout value in seconds */
Gnubby.SHORT_TIMEOUT = 1;
/** Normal timeout value in seconds */
Gnubby.NORMAL_TIMEOUT = 3;
// Max timeout usb firmware has for smartcard response is 30 seconds.
// Make our application level tolerance a little longer.
/** Maximum timeout in seconds */
Gnubby.MAX_TIMEOUT = 31;

/** Blink led
 * @param {number|ArrayBuffer|Uint8Array} data Command data or number
 *     of seconds to blink
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.blink = function(data, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array([data]);
    data = d.buffer;
  }
  this.exchange_(GnubbyDevice.CMD_PROMPT, data, Gnubby.NORMAL_TIMEOUT, cb);
};

/** Lock the gnubby
 * @param {number|ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.lock = function(data, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array([data]);
    data = d.buffer;
  }
  this.exchange_(GnubbyDevice.CMD_LOCK, data, Gnubby.NORMAL_TIMEOUT, cb);
};

/** Unlock the gnubby
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.unlock = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  var data = new Uint8Array([0]);
  this.exchange_(GnubbyDevice.CMD_LOCK, data.buffer,
      Gnubby.NORMAL_TIMEOUT, cb);
};

/** Request system information data.
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.sysinfo = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  this.exchange_(GnubbyDevice.CMD_SYSINFO, new ArrayBuffer(0),
      Gnubby.NORMAL_TIMEOUT, cb);
};

/** Send wink command
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.wink = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  this.exchange_(GnubbyDevice.CMD_WINK, new ArrayBuffer(0),
      Gnubby.NORMAL_TIMEOUT, cb);
};

/** Send DFU (Device firmware upgrade) command
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.dfu = function(data, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  this.exchange_(GnubbyDevice.CMD_DFU, data, Gnubby.NORMAL_TIMEOUT, cb);
};

/** Ping the gnubby
 * @param {number|ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.ping = function(data, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  if (typeof data == 'number') {
    var d = new Uint8Array(data);
    window.crypto.getRandomValues(d);
    data = d.buffer;
  }
  this.exchange_(GnubbyDevice.CMD_PING, data, Gnubby.NORMAL_TIMEOUT, cb);
};

/** Send a raw APDU command
 * @param {ArrayBuffer|Uint8Array} data Command data
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.apdu = function(data, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  this.exchange_(GnubbyDevice.CMD_APDU, data, Gnubby.MAX_TIMEOUT, cb);
};

/** Reset gnubby
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.reset = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  this.exchange_(GnubbyDevice.CMD_ATR, new ArrayBuffer(0),
      Gnubby.MAX_TIMEOUT, cb);
};

// byte args[3] = [delay-in-ms before disabling interrupts,
//                 delay-in-ms before disabling usb (aka remove),
//                 delay-in-ms before reboot (aka insert)]
/** Send usb test command
 * @param {ArrayBuffer|Uint8Array} args Command data
 * @param {?function(...)} cb Callback
 */
Gnubby.prototype.usb_test = function(args, cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  var u8 = new Uint8Array(args);
  this.exchange_(GnubbyDevice.CMD_USB_TEST, u8.buffer,
      Gnubby.NORMAL_TIMEOUT, cb);
};

/** APDU command with reply
 * @param {ArrayBuffer|Uint8Array} request The request
 * @param {?function(...)} cb Callback
 * @param {boolean=} opt_nowink Do not wink
 */
Gnubby.prototype.apduReply = function(request, cb, opt_nowink) {
  if (!cb) cb = Gnubby.defaultCallback;
  var self = this;

  this.apdu(request, function(rc, data) {
    if (rc == 0) {
      var r8 = new Uint8Array(data);
      if (r8[r8.length - 2] == 0x90 && r8[r8.length - 1] == 0x00) {
        // strip trailing 9000
        var buf = new Uint8Array(r8.subarray(0, r8.length - 2));
        cb(-GnubbyDevice.OK, buf.buffer);
        return;
      } else {
        // return non-9000 as rc
        rc = r8[r8.length - 2] * 256 + r8[r8.length - 1];
        // wink gnubby at hand if it needs touching.
        if (rc == 0x6985 && !opt_nowink) {
          self.wink(function() { cb(rc); });
          return;
        }
      }
    }
    // Warn on errors other than waiting for touch, wrong data, and
    // unrecognized command.
    if (rc != 0x6985 && rc != 0x6a80 && rc != 0x6d00) {
      console.warn(UTIL_fmt('apduReply_ fail: ' + rc.toString(16)));
    }
    cb(rc);
  });
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Gnubby methods related to U2F support.
 */
'use strict';

// Commands and flags of the Gnubby applet
/** Enroll */
Gnubby.U2F_ENROLL = 0x01;
/** Request signature */
Gnubby.U2F_SIGN = 0x02;
/** Request protocol version */
Gnubby.U2F_VERSION = 0x03;

/** Request applet version */
Gnubby.APPLET_VERSION = 0x11;  // First 3 bytes are applet version.

// APDU.P1 flags
/** Test of User Presence required */
Gnubby.P1_TUP_REQUIRED = 0x01;
/** Consume a Test of User Presence */
Gnubby.P1_TUP_CONSUME = 0x02;
/** Test signature only, no TUP. E.g. to check for existing enrollments. */
Gnubby.P1_TUP_TESTONLY = 0x04;
/** Attest with device key */
Gnubby.P1_INDIVIDUAL_KEY = 0x80;

// Version values
/** V1 of the applet. */
Gnubby.U2F_V1 = 'U2F_V1';
/** V2 of the applet. */
Gnubby.U2F_V2 = 'U2F_V2';

/** Perform enrollment
 * @param {Array<number>|ArrayBuffer|Uint8Array} challenge Enrollment challenge
 * @param {Array<number>|ArrayBuffer|Uint8Array} appIdHash Hashed application
 *     id
 * @param {function(...)} cb Result callback
 * @param {boolean=} opt_individualAttestation Request the individual
 *     attestation cert rather than the batch one.
 */
Gnubby.prototype.enroll = function(challenge, appIdHash, cb,
    opt_individualAttestation) {
  var p1 = Gnubby.P1_TUP_REQUIRED | Gnubby.P1_TUP_CONSUME;
  if (opt_individualAttestation) {
    p1 |= Gnubby.P1_INDIVIDUAL_KEY;
  }
  var apdu = new Uint8Array(
      [0x00,
       Gnubby.U2F_ENROLL,
       p1,
       0x00, 0x00, 0x00,
       challenge.length + appIdHash.length]);
  var u8 = new Uint8Array(apdu.length + challenge.length +
      appIdHash.length + 2);
  for (var i = 0; i < apdu.length; ++i) u8[i] = apdu[i];
  for (var i = 0; i < challenge.length; ++i) u8[i + apdu.length] =
    challenge[i];
  for (var i = 0; i < appIdHash.length; ++i) {
    u8[i + apdu.length + challenge.length] = appIdHash[i];
  }
  this.apduReply(u8.buffer, cb);
};

/** Request signature
 * @param {Array<number>|ArrayBuffer|Uint8Array} challengeHash Hashed
 *     signature challenge
 * @param {Array<number>|ArrayBuffer|Uint8Array} appIdHash Hashed application
 *     id
 * @param {Array<number>|ArrayBuffer|Uint8Array} keyHandle Key handle to use
 * @param {function(...)} cb Result callback
 * @param {boolean=} opt_nowink Request signature without winking
 *     (e.g. during enroll)
 */
Gnubby.prototype.sign = function(challengeHash, appIdHash, keyHandle, cb,
                                    opt_nowink) {
  var self = this;
  // The sign command's format is ever-so-slightly different between V1 and V2,
  // so get this gnubby's version prior to sending it.
  this.version(function(rc, opt_data) {
    if (rc) {
      cb(rc);
      return;
    }
    var version = UTIL_BytesToString(new Uint8Array(opt_data || []));
    var apduDataLen =
      challengeHash.length + appIdHash.length + keyHandle.length;
    if (version != Gnubby.U2F_V1) {
      // The V2 sign command includes a length byte for the key handle.
      apduDataLen++;
    }
    var apdu = new Uint8Array(
        [0x00,
         Gnubby.U2F_SIGN,
         Gnubby.P1_TUP_REQUIRED | Gnubby.P1_TUP_CONSUME,
         0x00, 0x00, 0x00,
         apduDataLen]);
    if (opt_nowink) {
      // A signature request that does not want winking.
      // These are used during enroll to figure out whether a gnubby was already
      // enrolled.
      // Tell applet to not actually produce a signature, even
      // if already touched.
      apdu[2] |= Gnubby.P1_TUP_TESTONLY;
    }
    var u8 = new Uint8Array(apdu.length + apduDataLen + 2);
    for (var i = 0; i < apdu.length; ++i) u8[i] = apdu[i];
    for (var i = 0; i < challengeHash.length; ++i) u8[i + apdu.length] =
      challengeHash[i];
    for (var i = 0; i < appIdHash.length; ++i) {
      u8[i + apdu.length + challengeHash.length] = appIdHash[i];
    }
    var keyHandleOffset = apdu.length + challengeHash.length + appIdHash.length;
    if (version != Gnubby.U2F_V1) {
      u8[keyHandleOffset++] = keyHandle.length;
    }
    for (var i = 0; i < keyHandle.length; ++i) {
      u8[i + keyHandleOffset] = keyHandle[i];
    }
    self.apduReply(u8.buffer, cb, opt_nowink);
  });
};

/** Request version information
 * @param {function(...)} cb Callback
 */
Gnubby.prototype.version = function(cb) {
  if (!cb) cb = Gnubby.defaultCallback;
  if (this.version_) {
    cb(-GnubbyDevice.OK, this.version_);
    return;
  }
  var self = this;

  function gotResponse(rc, data) {
    if (!rc) {
      self.version_ = data;
    }
    cb(rc, data);
  }

  var apdu = new Uint8Array([0x00, Gnubby.U2F_VERSION, 0x00, 0x00, 0x00,
      0x00, 0x00]);
  this.apduReply(apdu.buffer, function(rc, data) {
    if (rc == 0x6d00) {
      // Command not implemented. Pretend this is v1.
      var v1 = new Uint8Array(UTIL_StringToBytes(Gnubby.U2F_V1));
      self.version_ = v1.buffer;
      cb(-GnubbyDevice.OK, v1.buffer);
      return;
    }
    if (rc == 0x6700) {
      // Wrong length. Try with non-ISO 7816-4-conforming layout defined in
      // earlier U2F drafts.
      apdu = new Uint8Array([0x00, Gnubby.U2F_VERSION, 0x00, 0x00, 0x00,
          0x00, 0x00, 0x00, 0x00]);
      self.apduReply(apdu.buffer, gotResponse);
      return;
    }
    // Any other response: handle as final result.
    gotResponse(rc, data);
  });
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Contains a factory interface for creating and opening gnubbies.
 */
'use strict';

/**
 * A factory for creating and opening gnubbies.
 * @interface
 */
function GnubbyFactory() {}

/**
 * Enumerates gnubbies.
 * @param {function(number, Array<GnubbyDeviceId>)} cb Enumerate callback
 */
GnubbyFactory.prototype.enumerate = function(cb) {
};

/** @typedef {function(number, Gnubby=)} */
var FactoryOpenCallback;

/**
 * Creates a new gnubby object, and opens the gnubby with the given index.
 * @param {GnubbyDeviceId} which The device to open.
 * @param {boolean} forEnroll Whether this gnubby is being opened for enrolling.
 * @param {FactoryOpenCallback} cb Called with result of opening the gnubby.
 * @param {string=} opt_appIdHash The base64-encoded hash of the app id for
 *     which the gnubby being opened.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @param {string=} opt_caller Identifier for the caller.
 * @return {(function ()|undefined)} Some implementations might return function
 *     that can be used to cancel this pending open operation. Opening device
 *     might take long time or be resource-hungry.
 */
GnubbyFactory.prototype.openGnubby =
    function(which, forEnroll, cb, opt_appIdHash, opt_logMsgUrl, opt_caller) {
};

/**
 * Called during enrollment to check whether a gnubby known not to be enrolled
 * is allowed to enroll in its present state. Upon completion of the check, the
 * callback is called.
 * @param {Gnubby} gnubby The not-enrolled gnubby.
 * @param {string} appIdHash The base64-encoded hash of the app id for which
 *     the gnubby being enrolled.
 * @param {FactoryOpenCallback} cb Called with the result of the prerequisite
 *     check. (A non-zero status indicates failure.)
 */
GnubbyFactory.prototype.notEnrolledPrerequisiteCheck =
    function(gnubby, appIdHash, cb) {
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Contains a simple factory for creating and opening Gnubby
 * instances.
 */
'use strict';

/**
 * @param {Gnubbies} gnubbies Gnubbies singleton instance
 * @constructor
 * @implements {GnubbyFactory}
 */
function UsbGnubbyFactory(gnubbies) {
  /** @private {Gnubbies} */
  this.gnubbies_ = gnubbies;
  Gnubby.setGnubbies(gnubbies);
}

/**
 * Creates a new gnubby object, and opens the gnubby with the given index.
 * @param {GnubbyDeviceId} which The device to open.
 * @param {boolean} forEnroll Whether this gnubby is being opened for enrolling.
 * @param {FactoryOpenCallback} cb Called with result of opening the gnubby.
 * @param {string=} opt_appIdHash The base64-encoded hash of the app id for
 *     which the gnubby being opened.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @param {string=} opt_caller Identifier for the caller.
 * @return {undefined} no open canceller needed for this type of gnubby
 * @override
 */
UsbGnubbyFactory.prototype.openGnubby =
    function(which, forEnroll, cb, opt_appIdHash, opt_logMsgUrl, opt_caller) {
  var gnubby = new Gnubby();
  gnubby.open(which, GnubbyEnumerationTypes.ANY, function(rc) {
    if (rc) {
      cb(rc, gnubby);
      return;
    }
    gnubby.sync(function(rc) {
      cb(rc, gnubby);
    });
  }, opt_caller);
};

/**
 * Enumerates gnubbies.
 * @param {function(number, Array<GnubbyDeviceId>)} cb Enumerate callback
 */
UsbGnubbyFactory.prototype.enumerate = function(cb) {
  this.gnubbies_.enumerate(cb);
};

/**
 * No-op prerequisite check.
 * @param {Gnubby} gnubby The not-enrolled gnubby.
 * @param {string} appIdHash The base64-encoded hash of the app id for which
 *     the gnubby being enrolled.
 * @param {FactoryOpenCallback} cb Called with the result of the prerequisite
 *     check. (A non-zero status indicates failure.)
 */
UsbGnubbyFactory.prototype.notEnrolledPrerequisiteCheck =
    function(gnubby, appIdHash, cb) {
  cb(DeviceStatusCodes.OK_STATUS, gnubby);
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview This file defines the status codes returned by the device.
 */

/**
 * Status codes returned by the gnubby device.
 * @const
 * @enum {number}
 * @export
 */
var DeviceStatusCodes = {};

/**
 * Device operation succeeded.
 * @const
 */
DeviceStatusCodes.OK_STATUS = 0;

/**
 * Device operation wrong length status.
 * @const
 */
DeviceStatusCodes.WRONG_LENGTH_STATUS = 0x6700;

/**
 * Device operation wait touch status.
 * @const
 */
DeviceStatusCodes.WAIT_TOUCH_STATUS = 0x6985;

/**
 * Device operation invalid data status.
 * @const
 */
DeviceStatusCodes.INVALID_DATA_STATUS = 0x6984;

/**
 * Device operation wrong data status.
 * @const
 */
DeviceStatusCodes.WRONG_DATA_STATUS = 0x6a80;

/**
 * Device operation timeout status.
 * @const
 */
DeviceStatusCodes.TIMEOUT_STATUS = -5;

/**
 * Device operation busy status.
 * @const
 */
DeviceStatusCodes.BUSY_STATUS = -6;

/**
 * Device removed status.
 * @const
 */
DeviceStatusCodes.GONE_STATUS = -8;
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Handles web page requests for gnubby enrollment.
 */

'use strict';

/**
 * Handles a U2F enroll request.
 * @param {MessageSender} messageSender The message sender.
 * @param {Object} request The web page's enroll request.
 * @param {Function} sendResponse Called back with the result of the enroll.
 * @return {Closeable} A handler object to be closed when the browser channel
 *     closes.
 */
function handleU2fEnrollRequest(messageSender, request, sendResponse) {
  var sentResponse = false;
  var closeable = null;

  function sendErrorResponse(error) {
    var response = makeU2fErrorResponse(request, error.errorCode,
        error.errorMessage);
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  function sendSuccessResponse(u2fVersion, info, clientData) {
    var enrollChallenges = request['registerRequests'];
    var enrollChallenge =
        findEnrollChallengeOfVersion(enrollChallenges, u2fVersion);
    if (!enrollChallenge) {
      sendErrorResponse({errorCode: ErrorCodes.OTHER_ERROR});
      return;
    }
    var responseData =
        makeEnrollResponseData(enrollChallenge, u2fVersion, info, clientData);
    var response = makeU2fSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, closeable, response, sendResponse);
  }

  function timeout() {
    sendErrorResponse({errorCode: ErrorCodes.TIMEOUT});
  }

  var sender = createSenderFromMessageSender(messageSender);
  if (!sender) {
    sendErrorResponse({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }
  if (sender.origin.indexOf('http://') == 0 && !HTTP_ORIGINS_ALLOWED) {
    sendErrorResponse({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }

  if (!isValidEnrollRequest(request)) {
    sendErrorResponse({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }

  var timeoutValueSeconds = getTimeoutValueFromRequest(request);
  // Attenuate watchdog timeout value less than the enroller's timeout, so the
  // watchdog only fires after the enroller could reasonably have called back,
  // not before.
  var watchdogTimeoutValueSeconds = attenuateTimeoutInSeconds(
      timeoutValueSeconds, MINIMUM_TIMEOUT_ATTENUATION_SECONDS / 2);
  var watchdog = new WatchdogRequestHandler(watchdogTimeoutValueSeconds,
      timeout);
  var wrappedErrorCb = watchdog.wrapCallback(sendErrorResponse);
  var wrappedSuccessCb = watchdog.wrapCallback(sendSuccessResponse);

  var timer = createAttenuatedTimer(
      FACTORY_REGISTRY.getCountdownFactory(), timeoutValueSeconds);
  var logMsgUrl = request['logMsgUrl'];
  var enroller = new Enroller(timer, sender, sendErrorResponse,
      sendSuccessResponse, logMsgUrl);
  watchdog.setCloseable(/** @type {!Closeable} */ (enroller));
  closeable = watchdog;

  var registerRequests = request['registerRequests'];
  var signRequests = getSignRequestsFromEnrollRequest(request);
  enroller.doEnroll(registerRequests, signRequests, request['appId']);

  return closeable;
}

/**
 * Returns whether the request appears to be a valid enroll request.
 * @param {Object} request The request.
 * @return {boolean} Whether the request appears valid.
 */
function isValidEnrollRequest(request) {
  if (!request.hasOwnProperty('registerRequests'))
    return false;
  var enrollChallenges = request['registerRequests'];
  if (!enrollChallenges.length)
    return false;
  var hasAppId = request.hasOwnProperty('appId');
  if (!isValidEnrollChallengeArray(enrollChallenges, !hasAppId))
    return false;
  var signChallenges = getSignChallenges(request);
  // A missing sign challenge array is ok, in the case the user is not already
  // enrolled.
  // A challenge value need not necessarily be supplied with every challenge.
  var challengeRequired = false;
  if (signChallenges &&
      !isValidSignChallengeArray(signChallenges, challengeRequired, !hasAppId))
    return false;
  return true;
}

/**
 * @typedef {{
 *   version: (string|undefined),
 *   challenge: string,
 *   appId: string
 * }}
 */
var EnrollChallenge;

/**
 * @param {Array<EnrollChallenge>} enrollChallenges The enroll challenges to
 *     validate.
 * @param {boolean} appIdRequired Whether the appId property is required on
 *     each challenge.
 * @return {boolean} Whether the given array of challenges is a valid enroll
 *     challenges array.
 */
function isValidEnrollChallengeArray(enrollChallenges, appIdRequired) {
  var seenVersions = {};
  for (var i = 0; i < enrollChallenges.length; i++) {
    var enrollChallenge = enrollChallenges[i];
    var version = enrollChallenge['version'];
    if (!version) {
      // Version is implicitly V1 if not specified.
      version = 'U2F_V1';
    }
    if (version != 'U2F_V1' && version != 'U2F_V2') {
      return false;
    }
    if (seenVersions[version]) {
      // Each version can appear at most once.
      return false;
    }
    seenVersions[version] = version;
    if (appIdRequired && !enrollChallenge['appId']) {
      return false;
    }
    if (!enrollChallenge['challenge']) {
      // The challenge is required.
      return false;
    }
  }
  return true;
}

/**
 * Finds the enroll challenge of the given version in the enroll challlenge
 * array.
 * @param {Array<EnrollChallenge>} enrollChallenges The enroll challenges to
 *     search.
 * @param {string} version Version to search for.
 * @return {?EnrollChallenge} The enroll challenge with the given versions, or
 *     null if it isn't found.
 */
function findEnrollChallengeOfVersion(enrollChallenges, version) {
  for (var i = 0; i < enrollChallenges.length; i++) {
    if (enrollChallenges[i]['version'] == version) {
      return enrollChallenges[i];
    }
  }
  return null;
}

/**
 * Makes a responseData object for the enroll request with the given parameters.
 * @param {EnrollChallenge} enrollChallenge The enroll challenge used to
 *     register.
 * @param {string} u2fVersion Version of gnubby that enrolled.
 * @param {string} registrationData The registration data.
 * @param {string=} opt_clientData The client data, if available.
 * @return {Object} The responseData object.
 */
function makeEnrollResponseData(enrollChallenge, u2fVersion, registrationData,
    opt_clientData) {
  var responseData = {};
  responseData['registrationData'] = registrationData;
  // Echo the used challenge back in the reply.
  for (var k in enrollChallenge) {
    responseData[k] = enrollChallenge[k];
  }
  if (u2fVersion == 'U2F_V2') {
    // For U2F_V2, the challenge sent to the gnubby is modified to be the
    // hash of the client data. Include the client data.
    responseData['clientData'] = opt_clientData;
  }
  return responseData;
}

/**
 * Gets the expanded sign challenges from an enroll request, potentially by
 * modifying the request to contain a challenge value where one was omitted.
 * (For enrolling, the server isn't interested in the value of a signature,
 * only whether the presented key handle is already enrolled.)
 * @param {Object} request The request.
 * @return {Array<SignChallenge>}
 */
function getSignRequestsFromEnrollRequest(request) {
  var signChallenges;
  if (request.hasOwnProperty('registeredKeys')) {
    signChallenges = request['registeredKeys'];
  } else {
    signChallenges = request['signRequests'];
  }
  if (signChallenges) {
    for (var i = 0; i < signChallenges.length; i++) {
      // Make sure each sign challenge has a challenge value.
      // The actual value doesn't matter, as long as it's a string.
      if (!signChallenges[i].hasOwnProperty('challenge')) {
        signChallenges[i]['challenge'] = '';
      }
    }
  }
  return signChallenges;
}

/**
 * Creates a new object to track enrolling with a gnubby.
 * @param {!Countdown} timer Timer for enroll request.
 * @param {!WebRequestSender} sender The sender of the request.
 * @param {function(U2fError)} errorCb Called upon enroll failure.
 * @param {function(string, string, (string|undefined))} successCb Called upon
 *     enroll success with the version of the succeeding gnubby, the enroll
 *     data, and optionally the browser data associated with the enrollment.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @constructor
 */
function Enroller(timer, sender, errorCb, successCb, opt_logMsgUrl) {
  /** @private {Countdown} */
  this.timer_ = timer;
  /** @private {WebRequestSender} */
  this.sender_ = sender;
  /** @private {function(U2fError)} */
  this.errorCb_ = errorCb;
  /** @private {function(string, string, (string|undefined))} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {boolean} */
  this.done_ = false;

  /** @private {Object<string, string>} */
  this.browserData_ = {};
  /** @private {Array<EnrollHelperChallenge>} */
  this.encodedEnrollChallenges_ = [];
  /** @private {Array<SignHelperChallenge>} */
  this.encodedSignChallenges_ = [];
  // Allow http appIds for http origins. (Broken, but the caller deserves
  // what they get.)
  /** @private {boolean} */
  this.allowHttp_ =
      this.sender_.origin ? this.sender_.origin.indexOf('http://') == 0 : false;
  /** @private {Closeable} */
  this.handler_ = null;
}

/**
 * Default timeout value in case the caller never provides a valid timeout.
 */
Enroller.DEFAULT_TIMEOUT_MILLIS = 30 * 1000;

/**
 * Performs an enroll request with the given enroll and sign challenges.
 * @param {Array<EnrollChallenge>} enrollChallenges A set of enroll challenges.
 * @param {Array<SignChallenge>} signChallenges A set of sign challenges for
 *     existing enrollments for this user and appId.
 * @param {string=} opt_appId The app id for the entire request.
 */
Enroller.prototype.doEnroll = function(enrollChallenges, signChallenges,
    opt_appId) {
  /** @private {Array<EnrollChallenge>} */
  this.enrollChallenges_ = enrollChallenges;
  /** @private {Array<SignChallenge>} */
  this.signChallenges_ = signChallenges;
  /** @private {(string|undefined)} */
  this.appId_ = opt_appId;
  var self = this;
  getTabIdWhenPossible(this.sender_).then(function() {
    if (self.done_) return;
    self.approveOrigin_();
  }, function() {
    self.close();
    self.notifyError_({errorCode: ErrorCodes.BAD_REQUEST});
  });
};

/**
 * Ensures the user has approved this origin to use security keys, sending
 * to the request to the handler if/when the user has done so.
 * @private
 */
Enroller.prototype.approveOrigin_ = function() {
  var self = this;
  FACTORY_REGISTRY.getApprovedOrigins()
      .isApprovedOrigin(this.sender_.origin, this.sender_.tabId)
      .then(function(result) {
        if (self.done_) return;
        if (!result) {
          // Origin not approved: rather than give an explicit indication to
          // the web page, let a timeout occur.
          if (self.timer_.expired()) {
            self.notifyTimeout_();
            return;
          }
          var newTimer = self.timer_.clone(self.notifyTimeout_.bind(self));
          self.timer_.clearTimeout();
          self.timer_ = newTimer;
          return;
        }
        self.sendEnrollRequestToHelper_();
      });
};

/**
 * Notifies the caller of a timeout error.
 * @private
 */
Enroller.prototype.notifyTimeout_ = function() {
  this.notifyError_({errorCode: ErrorCodes.TIMEOUT});
};

/**
 * Performs an enroll request with this instance's enroll and sign challenges,
 * by encoding them into a helper request and passing the resulting request to
 * the factory registry's helper.
 * @private
 */
Enroller.prototype.sendEnrollRequestToHelper_ = function() {
  var encodedEnrollChallenges =
      this.encodeEnrollChallenges_(this.enrollChallenges_, this.appId_);
  // If the request didn't contain a sign challenge, provide one. The value
  // doesn't matter.
  var defaultSignChallenge = '';
  var encodedSignChallenges =
      encodeSignChallenges(this.signChallenges_, defaultSignChallenge,
          this.appId_);
  var request = {
    type: 'enroll_helper_request',
    enrollChallenges: encodedEnrollChallenges,
    signData: encodedSignChallenges,
    logMsgUrl: this.logMsgUrl_
  };
  if (!this.timer_.expired()) {
    request.timeout = this.timer_.millisecondsUntilExpired() / 1000.0;
    request.timeoutSeconds = this.timer_.millisecondsUntilExpired() / 1000.0;
  }

  // Begin fetching/checking the app ids.
  var enrollAppIds = [];
  if (this.appId_) {
    enrollAppIds.push(this.appId_);
  }
  for (var i = 0; i < this.enrollChallenges_.length; i++) {
    if (this.enrollChallenges_[i].hasOwnProperty('appId')) {
      enrollAppIds.push(this.enrollChallenges_[i]['appId']);
    }
  }
  // Sanity check
  if (!enrollAppIds.length) {
    console.warn(UTIL_fmt('empty enroll app ids?'));
    this.notifyError_({errorCode: ErrorCodes.BAD_REQUEST});
    return;
  }
  var self = this;
  this.checkAppIds_(enrollAppIds, function(result) {
    if (self.done_) return;
    if (result) {
      self.handler_ = FACTORY_REGISTRY.getRequestHelper().getHandler(request);
      if (self.handler_) {
        var helperComplete =
            /** @type {function(HelperReply)} */
            (self.helperComplete_.bind(self));
        self.handler_.run(helperComplete);
      } else {
        self.notifyError_({errorCode: ErrorCodes.OTHER_ERROR});
      }
    } else {
      self.notifyError_({errorCode: ErrorCodes.BAD_REQUEST});
    }
  });
};

/**
 * Encodes the enroll challenge as an enroll helper challenge.
 * @param {EnrollChallenge} enrollChallenge The enroll challenge to encode.
 * @param {string=} opt_appId The app id for the entire request.
 * @return {EnrollHelperChallenge} The encoded challenge.
 * @private
 */
Enroller.encodeEnrollChallenge_ = function(enrollChallenge, opt_appId) {
  var encodedChallenge = {};
  var version;
  if (enrollChallenge['version']) {
    version = enrollChallenge['version'];
  } else {
    // Version is implicitly V1 if not specified.
    version = 'U2F_V1';
  }
  encodedChallenge['version'] = version;
  encodedChallenge['challengeHash'] = enrollChallenge['challenge'];
  var appId;
  if (enrollChallenge['appId']) {
    appId = enrollChallenge['appId'];
  } else {
    appId = opt_appId;
  }
  if (!appId) {
    // Sanity check. (Other code should fail if it's not set.)
    console.warn(UTIL_fmt('No appId?'));
  }
  encodedChallenge['appIdHash'] = B64_encode(sha256HashOfString(appId));
  return /** @type {EnrollHelperChallenge} */ (encodedChallenge);
};

/**
 * Encodes the given enroll challenges using this enroller's state.
 * @param {Array<EnrollChallenge>} enrollChallenges The enroll challenges.
 * @param {string=} opt_appId The app id for the entire request.
 * @return {!Array<EnrollHelperChallenge>} The encoded enroll challenges.
 * @private
 */
Enroller.prototype.encodeEnrollChallenges_ = function(enrollChallenges,
    opt_appId) {
  var challenges = [];
  for (var i = 0; i < enrollChallenges.length; i++) {
    var enrollChallenge = enrollChallenges[i];
    var version = enrollChallenge.version;
    if (!version) {
      // Version is implicitly V1 if not specified.
      version = 'U2F_V1';
    }

    if (version == 'U2F_V2') {
      var modifiedChallenge = {};
      for (var k in enrollChallenge) {
        modifiedChallenge[k] = enrollChallenge[k];
      }
      // V2 enroll responses contain signatures over a browser data object,
      // which we're constructing here. The browser data object contains, among
      // other things, the server challenge.
      var serverChallenge = enrollChallenge['challenge'];
      var browserData = makeEnrollBrowserData(
          serverChallenge, this.sender_.origin, this.sender_.tlsChannelId);
      // Replace the challenge with the hash of the browser data.
      modifiedChallenge['challenge'] =
          B64_encode(sha256HashOfString(browserData));
      this.browserData_[version] =
          B64_encode(UTIL_StringToBytes(browserData));
      challenges.push(Enroller.encodeEnrollChallenge_(
          /** @type {EnrollChallenge} */ (modifiedChallenge), opt_appId));
    } else {
      challenges.push(
          Enroller.encodeEnrollChallenge_(enrollChallenge, opt_appId));
    }
  }
  return challenges;
};

/**
 * Checks the app ids associated with this enroll request, and calls a callback
 * with the result of the check.
 * @param {!Array<string>} enrollAppIds The app ids in the enroll challenge
 *     portion of the enroll request.
 * @param {function(boolean)} cb Called with the result of the check.
 * @private
 */
Enroller.prototype.checkAppIds_ = function(enrollAppIds, cb) {
  var appIds =
      UTIL_unionArrays(enrollAppIds, getDistinctAppIds(this.signChallenges_));
  FACTORY_REGISTRY.getOriginChecker()
      .canClaimAppIds(this.sender_.origin, appIds)
      .then(this.originChecked_.bind(this, appIds, cb));
};

/**
 * Called with the result of checking the origin. When the origin is allowed
 * to claim the app ids, begins checking whether the app ids also list the
 * origin.
 * @param {!Array<string>} appIds The app ids.
 * @param {function(boolean)} cb Called with the result of the check.
 * @param {boolean} result Whether the origin could claim the app ids.
 * @private
 */
Enroller.prototype.originChecked_ = function(appIds, cb, result) {
  if (!result) {
    this.notifyError_({errorCode: ErrorCodes.BAD_REQUEST});
    return;
  }
  var appIdChecker = FACTORY_REGISTRY.getAppIdCheckerFactory().create();
  appIdChecker.
      checkAppIds(
          this.timer_.clone(), this.sender_.origin, appIds, this.allowHttp_,
          this.logMsgUrl_)
      .then(cb);
};

/** Closes this enroller. */
Enroller.prototype.close = function() {
  if (this.handler_) {
    this.handler_.close();
    this.handler_ = null;
  }
  this.done_ = true;
};

/**
 * Notifies the caller with the error.
 * @param {U2fError} error Error.
 * @private
 */
Enroller.prototype.notifyError_ = function(error) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.errorCb_(error);
};

/**
 * Notifies the caller of success with the provided response data.
 * @param {string} u2fVersion Protocol version
 * @param {string} info Response data
 * @param {string|undefined} opt_browserData Browser data used
 * @private
 */
Enroller.prototype.notifySuccess_ =
    function(u2fVersion, info, opt_browserData) {
  if (this.done_)
    return;
  this.close();
  this.done_ = true;
  this.successCb_(u2fVersion, info, opt_browserData);
};

/**
 * Called by the helper upon completion.
 * @param {EnrollHelperReply} reply The result of the enroll request.
 * @private
 */
Enroller.prototype.helperComplete_ = function(reply) {
  if (reply.code) {
    var reportedError = mapDeviceStatusCodeToU2fError(reply.code);
    console.log(UTIL_fmt('helper reported ' + reply.code.toString(16) +
        ', returning ' + reportedError.errorCode));
    this.notifyError_(reportedError);
  } else {
    console.log(UTIL_fmt('Gnubby enrollment succeeded!!!!!'));
    var browserData;

    if (reply.version == 'U2F_V2') {
      // For U2F_V2, the challenge sent to the gnubby is modified to be the hash
      // of the browser data. Include the browser data.
      browserData = this.browserData_[reply.version];
    }

    this.notifySuccess_(/** @type {string} */ (reply.version),
                        /** @type {string} */ (reply.enrollData),
                        browserData);
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements an enroll handler using USB gnubbies.
 */
'use strict';

/**
 * @param {!EnrollHelperRequest} request The enroll request.
 * @constructor
 * @implements {RequestHandler}
 */
function UsbEnrollHandler(request) {
  /** @private {!EnrollHelperRequest} */
  this.request_ = request;

  /** @private {Array<Gnubby>} */
  this.waitingForTouchGnubbies_ = [];

  /** @private {boolean} */
  this.closed_ = false;
  /** @private {boolean} */
  this.notified_ = false;
}

/**
 * Default timeout value in case the caller never provides a valid timeout.
 * @const
 */
UsbEnrollHandler.DEFAULT_TIMEOUT_MILLIS = 30 * 1000;

/**
 * @param {RequestHandlerCallback} cb Called back with the result of the
 *     request, and an optional source for the result.
 * @return {boolean} Whether this handler could be run.
 */
UsbEnrollHandler.prototype.run = function(cb) {
  var timeoutMillis =
      this.request_.timeoutSeconds ?
      this.request_.timeoutSeconds * 1000 :
      UsbEnrollHandler.DEFAULT_TIMEOUT_MILLIS;
  /** @private {Countdown} */
  this.timer_ = DEVICE_FACTORY_REGISTRY.getCountdownFactory().createTimer(
      timeoutMillis);
  this.enrollChallenges = this.request_.enrollChallenges;
  /** @private {RequestHandlerCallback} */
  this.cb_ = cb;
  this.signer_ = new MultipleGnubbySigner(
      true /* forEnroll */,
      this.signerCompleted_.bind(this),
      this.signerFoundGnubby_.bind(this),
      timeoutMillis,
      this.request_.logMsgUrl);
  return this.signer_.doSign(this.request_.signData);
};

/** Closes this helper. */
UsbEnrollHandler.prototype.close = function() {
  this.closed_ = true;
  for (var i = 0; i < this.waitingForTouchGnubbies_.length; i++) {
    this.waitingForTouchGnubbies_[i].closeWhenIdle();
  }
  this.waitingForTouchGnubbies_ = [];
  if (this.signer_) {
    this.signer_.close();
    this.signer_ = null;
  }
};

/**
 * Called when a MultipleGnubbySigner completes its sign request.
 * @param {boolean} anyPending Whether any gnubbies are pending.
 * @private
 */
UsbEnrollHandler.prototype.signerCompleted_ = function(anyPending) {
  if (!this.anyGnubbiesFound_ || this.anyTimeout_ || anyPending ||
      this.timer_.expired()) {
    this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
  } else {
    // Do nothing: signerFoundGnubby will have been called with each succeeding
    // gnubby.
  }
};

/**
 * Called when a MultipleGnubbySigner finds a gnubby that can enroll.
 * @param {MultipleSignerResult} signResult Signature results
 * @param {boolean} moreExpected Whether the signer expects to report
 *     results from more gnubbies.
 * @private
 */
UsbEnrollHandler.prototype.signerFoundGnubby_ =
    function(signResult, moreExpected) {
  if (!signResult.code) {
    // If the signer reports a gnubby can sign, report this immediately to the
    // caller, as the gnubby is already enrolled. Map ok to WRONG_DATA, so the
    // caller knows what to do.
    this.notifyError_(DeviceStatusCodes.WRONG_DATA_STATUS);
  } else if (SingleGnubbySigner.signErrorIndicatesInvalidKeyHandle(
      signResult.code)) {
    var gnubby = signResult['gnubby'];
    // A valid helper request contains at least one enroll challenge, so use
    // the app id hash from the first challenge.
    var appIdHash = this.request_.enrollChallenges[0].appIdHash;
    DEVICE_FACTORY_REGISTRY.getGnubbyFactory().notEnrolledPrerequisiteCheck(
        gnubby, appIdHash, this.gnubbyPrerequisitesChecked_.bind(this));
  } else {
    // Unexpected error in signing? Send this immediately to the caller.
    this.notifyError_(signResult.code);
  }
};

/**
 * Called with the result of a gnubby prerequisite check.
 * @param {number} rc The result of the prerequisite check.
 * @param {Gnubby=} opt_gnubby The gnubby whose prerequisites were checked.
 * @private
 */
UsbEnrollHandler.prototype.gnubbyPrerequisitesChecked_ =
    function(rc, opt_gnubby) {
  if (rc || this.timer_.expired()) {
    // Do nothing:
    // If the timer is expired, the signerCompleted_ callback will indicate
    // timeout to the caller.
    // If there's an error, this gnubby is ineligible, but there's nothing we
    // can do about that here.
    return;
  }
  // If the callback succeeded, the gnubby is not null.
  var gnubby = /** @type {Gnubby} */ (opt_gnubby);
  this.anyGnubbiesFound_ = true;
  this.waitingForTouchGnubbies_.push(gnubby);
  this.matchEnrollVersionToGnubby_(gnubby);
};

/**
 * Attempts to match the gnubby's U2F version with an appropriate enroll
 * challenge.
 * @param {Gnubby} gnubby Gnubby instance
 * @private
 */
UsbEnrollHandler.prototype.matchEnrollVersionToGnubby_ = function(gnubby) {
  if (!gnubby) {
    console.warn(UTIL_fmt('no gnubby, WTF?'));
    return;
  }
  gnubby.version(this.gnubbyVersioned_.bind(this, gnubby));
};

/**
 * Called with the result of a version command.
 * @param {Gnubby} gnubby Gnubby instance
 * @param {number} rc result of version command.
 * @param {ArrayBuffer=} data version.
 * @private
 */
UsbEnrollHandler.prototype.gnubbyVersioned_ = function(gnubby, rc, data) {
  if (rc) {
    this.removeWrongVersionGnubby_(gnubby);
    return;
  }
  var version = UTIL_BytesToString(new Uint8Array(data || null));
  this.tryEnroll_(gnubby, version);
};

/**
 * Drops the gnubby from the list of eligible gnubbies.
 * @param {Gnubby} gnubby Gnubby instance
 * @private
 */
UsbEnrollHandler.prototype.removeWaitingGnubby_ = function(gnubby) {
  gnubby.closeWhenIdle();
  var index = this.waitingForTouchGnubbies_.indexOf(gnubby);
  if (index >= 0) {
    this.waitingForTouchGnubbies_.splice(index, 1);
  }
};

/**
 * Drops the gnubby from the list of eligible gnubbies, as it has the wrong
 * version.
 * @param {Gnubby} gnubby Gnubby instance
 * @private
 */
UsbEnrollHandler.prototype.removeWrongVersionGnubby_ = function(gnubby) {
  this.removeWaitingGnubby_(gnubby);
  if (!this.waitingForTouchGnubbies_.length) {
    // Whoops, this was the last gnubby.
    this.anyGnubbiesFound_ = false;
    if (this.timer_.expired()) {
      this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
    } else if (this.signer_) {
      this.signer_.reScanDevices();
    }
  }
};

/**
 * Attempts enrolling a particular gnubby with a challenge of the appropriate
 * version.
 * @param {Gnubby} gnubby Gnubby instance
 * @param {string} version Protocol version
 * @private
 */
UsbEnrollHandler.prototype.tryEnroll_ = function(gnubby, version) {
  var challenge = this.getChallengeOfVersion_(version);
  if (!challenge) {
    this.removeWrongVersionGnubby_(gnubby);
    return;
  }
  var challengeValue = B64_decode(challenge['challengeHash']);
  var appIdHash = challenge['appIdHash'];
  var individualAttest =
      DEVICE_FACTORY_REGISTRY.getIndividualAttestation().
          requestIndividualAttestation(appIdHash);
  gnubby.enroll(challengeValue, B64_decode(appIdHash),
      this.enrollCallback_.bind(this, gnubby, version), individualAttest);
};

/**
 * Finds the (first) challenge of the given version in this helper's challenges.
 * @param {string} version Protocol version
 * @return {Object} challenge, if found, or null if not.
 * @private
 */
UsbEnrollHandler.prototype.getChallengeOfVersion_ = function(version) {
  for (var i = 0; i < this.enrollChallenges.length; i++) {
    if (this.enrollChallenges[i]['version'] == version) {
      return this.enrollChallenges[i];
    }
  }
  return null;
};

/**
 * Called with the result of an enroll request to a gnubby.
 * @param {Gnubby} gnubby Gnubby instance
 * @param {string} version Protocol version
 * @param {number} code Status code
 * @param {ArrayBuffer=} infoArray Returned data
 * @private
 */
UsbEnrollHandler.prototype.enrollCallback_ =
    function(gnubby, version, code, infoArray) {
  if (this.notified_) {
    // Enroll completed after previous success or failure. Disregard.
    return;
  }
  switch (code) {
    case -GnubbyDevice.GONE:
        // Close this gnubby.
        this.removeWaitingGnubby_(gnubby);
        if (!this.waitingForTouchGnubbies_.length) {
          // Last enroll attempt is complete and last gnubby is gone.
          this.anyGnubbiesFound_ = false;
          if (this.timer_.expired()) {
            this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
          } else if (this.signer_) {
            this.signer_.reScanDevices();
          }
        }
      break;

    case DeviceStatusCodes.WAIT_TOUCH_STATUS:
    case DeviceStatusCodes.BUSY_STATUS:
    case DeviceStatusCodes.TIMEOUT_STATUS:
      if (this.timer_.expired()) {
        // Record that at least one gnubby timed out, to return a timeout status
        // from the complete callback if no other eligible gnubbies are found.
        /** @private {boolean} */
        this.anyTimeout_ = true;
        // Close this gnubby.
        this.removeWaitingGnubby_(gnubby);
        if (!this.waitingForTouchGnubbies_.length) {
          // Last enroll attempt is complete: return this error.
          console.log(UTIL_fmt('timeout (' + code.toString(16) +
              ') enrolling'));
          this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
        }
      } else {
        DEVICE_FACTORY_REGISTRY.getCountdownFactory().createTimer(
            UsbEnrollHandler.ENUMERATE_DELAY_INTERVAL_MILLIS,
            this.tryEnroll_.bind(this, gnubby, version));
      }
      break;

    case DeviceStatusCodes.OK_STATUS:
      var info = B64_encode(new Uint8Array(infoArray || []));
      this.notifySuccess_(version, info);
      break;

    default:
      console.log(UTIL_fmt('Failed to enroll gnubby: ' + code));
      this.notifyError_(code);
      break;
  }
};

/**
 * How long to delay between repeated enroll attempts, in milliseconds.
 * @const
 */
UsbEnrollHandler.ENUMERATE_DELAY_INTERVAL_MILLIS = 200;

/**
 * Notifies the callback with an error code.
 * @param {number} code The error code to report.
 * @private
 */
UsbEnrollHandler.prototype.notifyError_ = function(code) {
  if (this.notified_ || this.closed_)
    return;
  this.notified_ = true;
  this.close();
  var reply = {
    'type': 'enroll_helper_reply',
    'code': code
  };
  this.cb_(reply);
};

/**
 * @param {string} version Protocol version
 * @param {string} info B64 encoded success data
 * @private
 */
UsbEnrollHandler.prototype.notifySuccess_ = function(version, info) {
  if (this.notified_ || this.closed_)
    return;
  this.notified_ = true;
  this.close();
  var reply = {
    'type': 'enroll_helper_reply',
    'code': DeviceStatusCodes.OK_STATUS,
    'version': version,
    'enrollData': info
  };
  this.cb_(reply);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Queue of pending requests from an origin.
 *
 */
'use strict';

/**
 * Represents a queued request. Once given a token, call complete() once the
 * request is processed (or dropped.)
 * @interface
 */
function QueuedRequestToken() {}

/** Completes (or cancels) this queued request. */
QueuedRequestToken.prototype.complete = function() {};

/**
 * @param {!RequestQueue} queue The queue for this request.
 * @param {number} id An id for this request.
 * @param {function(QueuedRequestToken)} beginCb Called when work may begin on
 *     this request.
 * @param {RequestToken} opt_prev Previous request in the same queue.
 * @param {RequestToken} opt_next Next request in the same queue.
 * @constructor
 * @implements {QueuedRequestToken}
 */
function RequestToken(queue, id, beginCb, opt_prev, opt_next) {
  /** @private {!RequestQueue} */
  this.queue_ = queue;
  /** @private {number} */
  this.id_ = id;
  /** @type {function(QueuedRequestToken)} */
  this.beginCb = beginCb;
  /** @type {RequestToken} */
  this.prev = null;
  /** @type {RequestToken} */
  this.next = null;
  /** @private {boolean} */
  this.completed_ = false;
}

/** Completes (or cancels) this queued request. */
RequestToken.prototype.complete = function() {
  if (this.completed_) {
    // Either the caller called us more than once, or the timer is firing.
    // Either way, nothing more to do here.
    return;
  }
  this.completed_ = true;
  this.queue_.complete(this);
};

/** @return {boolean} Whether this token has already completed. */
RequestToken.prototype.completed = function() {
  return this.completed_;
};

/**
 * @param {!SystemTimer} sysTimer A system timer implementation.
 * @constructor
 */
function RequestQueue(sysTimer) {
  /** @private {!SystemTimer} */
  this.sysTimer_ = sysTimer;
  /** @private {RequestToken} */
  this.head_ = null;
  /** @private {RequestToken} */
  this.tail_ = null;
  /** @private {number} */
  this.id_ = 0;
}

/**
 * Inserts this token into the queue.
 * @param {RequestToken} token Queue token
 * @private
 */
RequestQueue.prototype.insertToken_ = function(token) {
  console.log(UTIL_fmt('token ' + this.id_ + ' inserted'));
  if (this.head_ === null) {
    this.head_ = token;
    this.tail_ = token;
  } else {
    if (!this.tail_) throw 'Non-empty list missing tail';
    this.tail_.next = token;
    token.prev = this.tail_;
    this.tail_ = token;
  }
};

/**
 * Removes this token from the queue.
 * @param {RequestToken} token Queue token
 * @private
 */
RequestQueue.prototype.removeToken_ = function(token) {
  if (token.next) {
    token.next.prev = token.prev;
  }
  if (token.prev) {
    token.prev.next = token.next;
  }
  if (this.head_ === token && this.tail_ === token) {
    this.head_ = this.tail_ = null;
  } else {
    if (this.head_ === token) {
      this.head_ = token.next;
      this.head_.prev = null;
    }
    if (this.tail_ === token) {
      this.tail_ = token.prev;
      this.tail_.next = null;
    }
  }
  token.prev = token.next = null;
};

/**
 * Completes this token's request, and begins the next queued request, if one
 * exists.
 * @param {RequestToken} token Queue token
 */
RequestQueue.prototype.complete = function(token) {
  console.log(UTIL_fmt('token ' + this.id_ + ' completed'));
  var next = token.next;
  this.removeToken_(token);
  if (next) {
    next.beginCb(next);
  }
};

/** @return {boolean} Whether this queue is empty. */
RequestQueue.prototype.empty = function() {
  return this.head_ === null;
};

/**
 * Queues this request, and, if it's the first request, begins work on it.
 * @param {function(QueuedRequestToken)} beginCb Called when work begins on this
 *     request.
 * @param {Countdown} timer Countdown timer
 * @return {QueuedRequestToken} A token for the request.
 */
RequestQueue.prototype.queueRequest = function(beginCb, timer) {
  var startNow = this.empty();
  var token = new RequestToken(this, ++this.id_, beginCb);
  // Clone the timer to set a callback on it, which will ensure complete() is
  // eventually called, even if the caller never gets around to it.
  timer.clone(token.complete.bind(token));
  this.insertToken_(token);
  if (startNow) {
    this.sysTimer_.setTimeout(function() {
      if (!token.completed()) {
        token.beginCb(token);
      }
    }, 0);
  }
  return token;
};

/**
 * @param {!SystemTimer} sysTimer A system timer implementation.
 * @constructor
 */
function OriginKeyedRequestQueue(sysTimer) {
  /** @private {!SystemTimer} */
  this.sysTimer_ = sysTimer;
  /** @private {Object<string, !RequestQueue>} */
  this.requests_ = {};
}

/**
 * Queues this request, and, if it's the first request, begins work on it.
 * @param {string} appId Application Id
 * @param {string} origin Request origin
 * @param {function(QueuedRequestToken)} beginCb Called when work begins on this
 *     request.
 * @param {Countdown} timer Countdown timer
 * @return {QueuedRequestToken} A token for the request.
 */
OriginKeyedRequestQueue.prototype.queueRequest =
    function(appId, origin, beginCb, timer) {
  var key = appId + ' ' + origin;
  if (!this.requests_.hasOwnProperty(key)) {
    this.requests_[key] = new RequestQueue(this.sysTimer_);
  }
  var queue = this.requests_[key];
  return queue.queueRequest(beginCb, timer);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Handles web page requests for gnubby sign requests.
 *
 */

'use strict';

var gnubbySignRequestQueue;

function initRequestQueue() {
  gnubbySignRequestQueue = new OriginKeyedRequestQueue(
      FACTORY_REGISTRY.getSystemTimer());
}

/**
 * Handles a U2F sign request.
 * @param {MessageSender} messageSender The message sender.
 * @param {Object} request The web page's sign request.
 * @param {Function} sendResponse Called back with the result of the sign.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function handleU2fSignRequest(messageSender, request, sendResponse) {
  var sentResponse = false;
  var queuedSignRequest;

  function sendErrorResponse(error) {
    sendResponseOnce(sentResponse, queuedSignRequest,
        makeU2fErrorResponse(request, error.errorCode, error.errorMessage),
        sendResponse);
  }

  function sendSuccessResponse(challenge, info, browserData) {
    var responseData = makeU2fSignResponseDataFromChallenge(challenge);
    addSignatureAndBrowserDataToResponseData(responseData, info, browserData,
        'clientData');
    var response = makeU2fSuccessResponse(request, responseData);
    sendResponseOnce(sentResponse, queuedSignRequest, response, sendResponse);
  }

  var sender = createSenderFromMessageSender(messageSender);
  if (!sender) {
    sendErrorResponse({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }
  if (sender.origin.indexOf('http://') == 0 && !HTTP_ORIGINS_ALLOWED) {
    sendErrorResponse({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }

  queuedSignRequest =
      validateAndEnqueueSignRequest(
          sender, request, sendErrorResponse, sendSuccessResponse);
  return queuedSignRequest;
}

/**
 * Creates a base U2F responseData object from the server challenge.
 * @param {SignChallenge} challenge The server challenge.
 * @return {Object} The responseData object.
 */
function makeU2fSignResponseDataFromChallenge(challenge) {
  var responseData = {
    'keyHandle': challenge['keyHandle']
  };
  return responseData;
}

/**
 * Adds the browser data and signature values to a responseData object.
 * @param {Object} responseData The "base" responseData object.
 * @param {string} signatureData The signature data.
 * @param {string} browserData The browser data generated from the challenge.
 * @param {string} browserDataName The name of the browser data key in the
 *     responseData object.
 */
function addSignatureAndBrowserDataToResponseData(responseData, signatureData,
    browserData, browserDataName) {
  responseData[browserDataName] = B64_encode(UTIL_StringToBytes(browserData));
  responseData['signatureData'] = signatureData;
}

/**
 * Validates a sign request using the given sign challenges name, and, if valid,
 * enqueues the sign request for eventual processing.
 * @param {WebRequestSender} sender The sender of the message.
 * @param {Object} request The web page's sign request.
 * @param {function(U2fError)} errorCb Error callback.
 * @param {function(SignChallenge, string, string)} successCb Success callback.
 * @return {Closeable} Request handler that should be closed when the browser
 *     message channel is closed.
 */
function validateAndEnqueueSignRequest(sender, request, errorCb, successCb) {
  function timeout() {
    errorCb({errorCode: ErrorCodes.TIMEOUT});
  }

  if (!isValidSignRequest(request)) {
    errorCb({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }

  // The typecast is necessary because getSignChallenges can return undefined.
  // On the other hand, a valid sign request can't contain an undefined sign
  // challenge list, so the typecast is safe.
  var signChallenges = /** @type {!Array<SignChallenge>} */ (
      getSignChallenges(request));
  var appId;
  if (request['appId']) {
    appId = request['appId'];
  } else if (signChallenges.length) {
    appId = signChallenges[0]['appId'];
  }
  // Sanity check
  if (!appId) {
    console.warn(UTIL_fmt('empty sign appId?'));
    errorCb({errorCode: ErrorCodes.BAD_REQUEST});
    return null;
  }
  var timeoutValueSeconds = getTimeoutValueFromRequest(request);
  // Attenuate watchdog timeout value less than the signer's timeout, so the
  // watchdog only fires after the signer could reasonably have called back,
  // not before.
  timeoutValueSeconds = attenuateTimeoutInSeconds(timeoutValueSeconds,
      MINIMUM_TIMEOUT_ATTENUATION_SECONDS / 2);
  var watchdog = new WatchdogRequestHandler(timeoutValueSeconds, timeout);
  var wrappedErrorCb = watchdog.wrapCallback(errorCb);
  var wrappedSuccessCb = watchdog.wrapCallback(successCb);

  var timer = createAttenuatedTimer(
      FACTORY_REGISTRY.getCountdownFactory(), timeoutValueSeconds);
  var logMsgUrl = request['logMsgUrl'];

  // Queue sign requests from the same origin, to protect against simultaneous
  // sign-out on many tabs resulting in repeated sign-in requests.
  var queuedSignRequest = new QueuedSignRequest(signChallenges,
      timer, sender, wrappedErrorCb, wrappedSuccessCb, request['challenge'],
      appId, logMsgUrl);
  if (!gnubbySignRequestQueue) {
    initRequestQueue();
  }
  var requestToken = gnubbySignRequestQueue.queueRequest(appId, sender.origin,
      queuedSignRequest.begin.bind(queuedSignRequest), timer);
  queuedSignRequest.setToken(requestToken);

  watchdog.setCloseable(queuedSignRequest);
  return watchdog;
}

/**
 * Returns whether the request appears to be a valid sign request.
 * @param {Object} request The request.
 * @return {boolean} Whether the request appears valid.
 */
function isValidSignRequest(request) {
  var signChallenges = getSignChallenges(request);
  if (!signChallenges) {
    return false;
  }
  var hasDefaultChallenge = request.hasOwnProperty('challenge');
  var hasAppId = request.hasOwnProperty('appId');
  // If the sign challenge array is empty, the global appId is required.
  if (!hasAppId && (!signChallenges || !signChallenges.length)) {
    return false;
  }
  return isValidSignChallengeArray(signChallenges, !hasDefaultChallenge,
      !hasAppId);
}

/**
 * Adapter class representing a queued sign request.
 * @param {!Array<SignChallenge>} signChallenges The sign challenges.
 * @param {Countdown} timer Timeout timer
 * @param {WebRequestSender} sender Message sender.
 * @param {function(U2fError)} errorCb Error callback
 * @param {function(SignChallenge, string, string)} successCb Success callback
 * @param {string|undefined} opt_defaultChallenge A default sign challenge
 *     value, if a request does not provide one.
 * @param {string|undefined} opt_appId The app id for the entire request.
 * @param {string|undefined} opt_logMsgUrl Url to post log messages to
 * @constructor
 * @implements {Closeable}
 */
function QueuedSignRequest(signChallenges, timer, sender, errorCb,
    successCb, opt_defaultChallenge, opt_appId, opt_logMsgUrl) {
  /** @private {!Array<SignChallenge>} */
  this.signChallenges_ = signChallenges;
  /** @private {Countdown} */
  this.timer_ = timer.clone(this.close.bind(this));
  /** @private {WebRequestSender} */
  this.sender_ = sender;
  /** @private {function(U2fError)} */
  this.errorCb_ = errorCb;
  /** @private {function(SignChallenge, string, string)} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.defaultChallenge_ = opt_defaultChallenge;
  /** @private {string|undefined} */
  this.appId_ = opt_appId;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;
  /** @private {boolean} */
  this.begun_ = false;
  /** @private {boolean} */
  this.closed_ = false;
}

/** Closes this sign request. */
QueuedSignRequest.prototype.close = function() {
  if (this.closed_) return;
  var hadBegunSigning = false;
  if (this.begun_ && this.signer_) {
    this.signer_.close();
    hadBegunSigning = true;
  }
  if (this.token_) {
    if (hadBegunSigning) {
      console.log(UTIL_fmt('closing in-progress request'));
    } else {
      console.log(UTIL_fmt('closing timed-out request before processing'));
    }
    this.token_.complete();
  }
  this.closed_ = true;
};

/**
 * @param {QueuedRequestToken} token Token for this sign request.
 */
QueuedSignRequest.prototype.setToken = function(token) {
  /** @private {QueuedRequestToken} */
  this.token_ = token;
};

/**
 * Called when this sign request may begin work.
 * @param {QueuedRequestToken} token Token for this sign request.
 */
QueuedSignRequest.prototype.begin = function(token) {
  if (this.timer_.expired()) {
    console.log(UTIL_fmt('Queued request begun after timeout'));
    this.close();
    this.errorCb_({errorCode: ErrorCodes.TIMEOUT});
    return;
  }
  this.begun_ = true;
  this.setToken(token);
  this.signer_ = new Signer(this.timer_, this.sender_,
      this.signerFailed_.bind(this), this.signerSucceeded_.bind(this),
      this.logMsgUrl_);
  if (!this.signer_.setChallenges(this.signChallenges_, this.defaultChallenge_,
      this.appId_)) {
    token.complete();
    this.errorCb_({errorCode: ErrorCodes.BAD_REQUEST});
  }
  // Signer now has responsibility for maintaining timeout.
  this.timer_.clearTimeout();
};

/**
 * Called when this request's signer fails.
 * @param {U2fError} error The failure reported by the signer.
 * @private
 */
QueuedSignRequest.prototype.signerFailed_ = function(error) {
  this.token_.complete();
  this.errorCb_(error);
};

/**
 * Called when this request's signer succeeds.
 * @param {SignChallenge} challenge The challenge that was signed.
 * @param {string} info The sign result.
 * @param {string} browserData Browser data JSON
 * @private
 */
QueuedSignRequest.prototype.signerSucceeded_ =
    function(challenge, info, browserData) {
  this.token_.complete();
  this.successCb_(challenge, info, browserData);
};

/**
 * Creates an object to track signing with a gnubby.
 * @param {Countdown} timer Timer for sign request.
 * @param {WebRequestSender} sender The message sender.
 * @param {function(U2fError)} errorCb Called when the sign operation fails.
 * @param {function(SignChallenge, string, string)} successCb Called when the
 *     sign operation succeeds.
 * @param {string=} opt_logMsgUrl The url to post log messages to.
 * @constructor
 */
function Signer(timer, sender, errorCb, successCb, opt_logMsgUrl) {
  /** @private {Countdown} */
  this.timer_ = timer.clone();
  /** @private {WebRequestSender} */
  this.sender_ = sender;
  /** @private {function(U2fError)} */
  this.errorCb_ = errorCb;
  /** @private {function(SignChallenge, string, string)} */
  this.successCb_ = successCb;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {boolean} */
  this.challengesSet_ = false;
  /** @private {boolean} */
  this.done_ = false;

  /** @private {Object<string, string>} */
  this.browserData_ = {};
  /** @private {Object<string, SignChallenge>} */
  this.serverChallenges_ = {};
  // Allow http appIds for http origins. (Broken, but the caller deserves
  // what they get.)
  /** @private {boolean} */
  this.allowHttp_ = this.sender_.origin ?
      this.sender_.origin.indexOf('http://') == 0 : false;
  /** @private {Closeable} */
  this.handler_ = null;
}

/**
 * Sets the challenges to be signed.
 * @param {Array<SignChallenge>} signChallenges The challenges to set.
 * @param {string=} opt_defaultChallenge A default sign challenge
 *     value, if a request does not provide one.
 * @param {string=} opt_appId The app id for the entire request.
 * @return {boolean} Whether the challenges could be set.
 */
Signer.prototype.setChallenges = function(signChallenges, opt_defaultChallenge,
    opt_appId) {
  if (this.challengesSet_ || this.done_)
    return false;
  if (this.timer_.expired()) {
    this.notifyError_({errorCode: ErrorCodes.TIMEOUT});
    return true;
  }
  /** @private {Array<SignChallenge>} */
  this.signChallenges_ = signChallenges;
  /** @private {string|undefined} */
  this.defaultChallenge_ = opt_defaultChallenge;
  /** @private {string|undefined} */
  this.appId_ = opt_appId;
  /** @private {boolean} */
  this.challengesSet_ = true;

  this.checkAppIds_();
  return true;
};

/**
 * Checks the app ids of incoming requests.
 * @private
 */
Signer.prototype.checkAppIds_ = function() {
  var appIds = getDistinctAppIds(this.signChallenges_);
  if (this.appId_) {
    appIds = UTIL_unionArrays([this.appId_], appIds);
  }
  if (!appIds || !appIds.length) {
    var error = {
      errorCode: ErrorCodes.BAD_REQUEST,
      errorMessage: 'missing appId'
    };
    this.notifyError_(error);
    return;
  }
  FACTORY_REGISTRY.getOriginChecker()
      .canClaimAppIds(this.sender_.origin, appIds)
      .then(this.originChecked_.bind(this, appIds));
};

/**
 * Called with the result of checking the origin. When the origin is allowed
 * to claim the app ids, begins checking whether the app ids also list the
 * origin.
 * @param {!Array<string>} appIds The app ids.
 * @param {boolean} result Whether the origin could claim the app ids.
 * @private
 */
Signer.prototype.originChecked_ = function(appIds, result) {
  if (!result) {
    var error = {
      errorCode: ErrorCodes.BAD_REQUEST,
      errorMessage: 'bad appId'
    };
    this.notifyError_(error);
    return;
  }
  var appIdChecker = FACTORY_REGISTRY.getAppIdCheckerFactory().create();
  appIdChecker.
      checkAppIds(
          this.timer_.clone(), this.sender_.origin,
          /** @type {!Array<string>} */ (appIds), this.allowHttp_,
          this.logMsgUrl_)
      .then(this.appIdChecked_.bind(this));
};

/**
 * Called with the result of checking app ids.  When the app ids are valid,
 * adds the sign challenges to those being signed.
 * @param {boolean} result Whether the app ids are valid.
 * @private
 */
Signer.prototype.appIdChecked_ = function(result) {
  if (!result) {
    var error = {
      errorCode: ErrorCodes.BAD_REQUEST,
      errorMessage: 'bad appId'
    };
    this.notifyError_(error);
    return;
  }
  if (!this.doSign_()) {
    this.notifyError_({errorCode: ErrorCodes.BAD_REQUEST});
    return;
  }
};

/**
 * Begins signing this signer's challenges.
 * @return {boolean} Whether the challenge could be added.
 * @private
 */
Signer.prototype.doSign_ = function() {
  // Create the browser data for each challenge.
  for (var i = 0; i < this.signChallenges_.length; i++) {
    var challenge = this.signChallenges_[i];
    var serverChallenge;
    if (challenge.hasOwnProperty('challenge')) {
      serverChallenge = challenge['challenge'];
    } else {
      serverChallenge = this.defaultChallenge_;
    }
    if (!serverChallenge) {
      console.warn(UTIL_fmt('challenge missing'));
      return false;
    }
    var keyHandle = challenge['keyHandle'];

    var browserData =
        makeSignBrowserData(serverChallenge, this.sender_.origin,
            this.sender_.tlsChannelId);
    this.browserData_[keyHandle] = browserData;
    this.serverChallenges_[keyHandle] = challenge;
  }

  var encodedChallenges = encodeSignChallenges(this.signChallenges_,
      this.defaultChallenge_, this.appId_, this.getChallengeHash_.bind(this));

  var timeoutSeconds = this.timer_.millisecondsUntilExpired() / 1000.0;
  var request = makeSignHelperRequest(encodedChallenges, timeoutSeconds,
      this.logMsgUrl_);
  this.handler_ =
      FACTORY_REGISTRY.getRequestHelper()
          .getHandler(/** @type {HelperRequest} */ (request));
  if (!this.handler_)
    return false;
  return this.handler_.run(this.helperComplete_.bind(this));
};

/**
 * @param {string} keyHandle The key handle used with the challenge.
 * @param {string} challenge The challenge.
 * @return {string} The hashed challenge associated with the key
 *     handle/challenge pair.
 * @private
 */
Signer.prototype.getChallengeHash_ = function(keyHandle, challenge) {
  return B64_encode(sha256HashOfString(this.browserData_[keyHandle]));
};

/** Closes this signer. */
Signer.prototype.close = function() {
  this.close_();
};

/**
 * Closes this signer, and optionally notifies the caller of error.
 * @param {boolean=} opt_notifying When true, this method is being called in the
 *     process of notifying the caller of an existing status. When false,
 *     the caller is notified with a default error value, ErrorCodes.TIMEOUT.
 * @private
 */
Signer.prototype.close_ = function(opt_notifying) {
  if (this.handler_) {
    this.handler_.close();
    this.handler_ = null;
  }
  this.timer_.clearTimeout();
  if (!opt_notifying) {
    this.notifyError_({errorCode: ErrorCodes.TIMEOUT});
  }
};

/**
 * Notifies the caller of error.
 * @param {U2fError} error Error.
 * @private
 */
Signer.prototype.notifyError_ = function(error) {
  if (this.done_)
    return;
  this.done_ = true;
  this.close_(true);
  this.errorCb_(error);
};

/**
 * Notifies the caller of success.
 * @param {SignChallenge} challenge The challenge that was signed.
 * @param {string} info The sign result.
 * @param {string} browserData Browser data JSON
 * @private
 */
Signer.prototype.notifySuccess_ = function(challenge, info, browserData) {
  if (this.done_)
    return;
  this.done_ = true;
  this.close_(true);
  this.successCb_(challenge, info, browserData);
};

/**
 * Called by the helper upon completion.
 * @param {HelperReply} helperReply The result of the sign request.
 * @param {string=} opt_source The source of the sign result.
 * @private
 */
Signer.prototype.helperComplete_ = function(helperReply, opt_source) {
  if (helperReply.type != 'sign_helper_reply') {
    this.notifyError_({errorCode: ErrorCodes.OTHER_ERROR});
    return;
  }
  var reply = /** @type {SignHelperReply} */ (helperReply);

  if (reply.code) {
    var reportedError = mapDeviceStatusCodeToU2fError(reply.code);
    console.log(UTIL_fmt('helper reported ' + reply.code.toString(16) +
        ', returning ' + reportedError.errorCode));
    this.notifyError_(reportedError);
  } else {
    if (this.logMsgUrl_ && opt_source) {
      var logMsg = 'signed&source=' + opt_source;
      logMessage(logMsg, this.logMsgUrl_);
    }

    var key = reply.responseData['keyHandle'];
    var browserData = this.browserData_[key];
    // Notify with server-provided challenge, not the encoded one: the
    // server-provided challenge contains additional fields it relies on.
    var serverChallenge = this.serverChallenges_[key];
    this.notifySuccess_(serverChallenge, reply.responseData.signatureData,
        browserData);
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview A single gnubby signer wraps the process of opening a gnubby,
 * signing each challenge in an array of challenges until a success condition
 * is satisfied, and finally yielding the gnubby upon success.
 *
 */

'use strict';

/**
 * @typedef {{
 *   code: number,
 *   gnubby: (Gnubby|undefined),
 *   challenge: (SignHelperChallenge|undefined),
 *   info: (ArrayBuffer|undefined)
 * }}
 */
var SingleSignerResult;

/**
 * Creates a new sign handler with a gnubby. This handler will perform a sign
 * operation using each challenge in an array of challenges until its success
 * condition is satisified, or an error or timeout occurs. The success condition
 * is defined differently depending whether this signer is used for enrolling
 * or for signing:
 *
 * For enroll, success is defined as each challenge yielding wrong data. This
 * means this gnubby is not currently enrolled for any of the appIds in any
 * challenge.
 *
 * For sign, success is defined as any challenge yielding ok.
 *
 * The complete callback is called only when the signer reaches success or
 * failure, i.e.  when there is no need for this signer to continue trying new
 * challenges.
 *
 * @param {GnubbyDeviceId} gnubbyId Which gnubby to open.
 * @param {boolean} forEnroll Whether this signer is signing for an attempted
 *     enroll operation.
 * @param {function(SingleSignerResult)}
 *     completeCb Called when this signer completes, i.e. no further results are
 *     possible.
 * @param {Countdown} timer An advisory timer, beyond whose expiration the
 *     signer will not attempt any new operations, assuming the caller is no
 *     longer interested in the outcome.
 * @param {string=} opt_logMsgUrl A URL to post log messages to.
 * @constructor
 */
function SingleGnubbySigner(gnubbyId, forEnroll, completeCb, timer,
    opt_logMsgUrl) {
  /** @private {GnubbyDeviceId} */
  this.gnubbyId_ = gnubbyId;
  /** @private {SingleGnubbySigner.State} */
  this.state_ = SingleGnubbySigner.State.INIT;
  /** @private {boolean} */
  this.forEnroll_ = forEnroll;
  /** @private {function(SingleSignerResult)} */
  this.completeCb_ = completeCb;
  /** @private {Countdown} */
  this.timer_ = timer;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {!Array<!SignHelperChallenge>} */
  this.challenges_ = [];
  /** @private {number} */
  this.challengeIndex_ = 0;
  /** @private {boolean} */
  this.challengesSet_ = false;

  /** @private {!Object<string, number>} */
  this.cachedError_ = [];

  /** @private {(function()|undefined)} */
  this.openCanceller_;
}

/** @enum {number} */
SingleGnubbySigner.State = {
  /** Initial state. */
  INIT: 0,
  /** The signer is attempting to open a gnubby. */
  OPENING: 1,
  /** The signer's gnubby opened, but is busy. */
  BUSY: 2,
  /** The signer has an open gnubby, but no challenges to sign. */
  IDLE: 3,
  /** The signer is currently signing a challenge. */
  SIGNING: 4,
  /** The signer got a final outcome. */
  COMPLETE: 5,
  /** The signer is closing its gnubby. */
  CLOSING: 6,
  /** The signer is closed. */
  CLOSED: 7
};

/**
 * @return {GnubbyDeviceId} This device id of the gnubby for this signer.
 */
SingleGnubbySigner.prototype.getDeviceId = function() {
  return this.gnubbyId_;
};

/**
 * Closes this signer's gnubby, if it's held.
 */
SingleGnubbySigner.prototype.close = function() {
  if (this.state_ == SingleGnubbySigner.State.OPENING) {
    if (this.openCanceller_)
      this.openCanceller_();
  }

  if (!this.gnubby_) return;
  this.state_ = SingleGnubbySigner.State.CLOSING;
  this.gnubby_.closeWhenIdle(this.closed_.bind(this));
};

/**
 * Called when this signer's gnubby is closed.
 * @private
 */
SingleGnubbySigner.prototype.closed_ = function() {
  this.gnubby_ = null;
  this.state_ = SingleGnubbySigner.State.CLOSED;
};

/**
 * Begins signing the given challenges.
 * @param {Array<SignHelperChallenge>} challenges The challenges to sign.
 * @return {boolean} Whether the challenges were accepted.
 */
SingleGnubbySigner.prototype.doSign = function(challenges) {
  if (this.challengesSet_) {
    // Can't add new challenges once they've been set.
    return false;
  }

  if (challenges) {
    console.log(this.gnubby_);
    console.log(UTIL_fmt('adding ' + challenges.length + ' challenges'));
    for (var i = 0; i < challenges.length; i++) {
      this.challenges_.push(challenges[i]);
    }
  }
  this.challengesSet_ = true;

  switch (this.state_) {
    case SingleGnubbySigner.State.INIT:
      this.open_();
      break;
    case SingleGnubbySigner.State.OPENING:
      // The open has already commenced, so accept the challenges, but don't do
      // anything.
      break;
    case SingleGnubbySigner.State.IDLE:
      if (this.challengeIndex_ < challenges.length) {
        // Challenges set: start signing.
        this.doSign_(this.challengeIndex_);
      } else {
        // An empty list of challenges can be set during enroll, when the user
        // has no existing enrolled gnubbies. It's unexpected during sign, but
        // returning WRONG_DATA satisfies the caller in either case.
        var self = this;
        window.setTimeout(function() {
          self.goToError_(DeviceStatusCodes.WRONG_DATA_STATUS);
        }, 0);
      }
      break;
    case SingleGnubbySigner.State.SIGNING:
      // Already signing, so don't kick off a new sign, but accept the added
      // challenges.
      break;
    default:
      return false;
  }
  return true;
};

/**
 * Attempts to open this signer's gnubby, if it's not already open.
 * @private
 */
SingleGnubbySigner.prototype.open_ = function() {
  var appIdHash;
  if (this.challenges_.length) {
    // Assume the first challenge's appId is representative of all of them.
    appIdHash = B64_encode(this.challenges_[0].appIdHash);
  }
  if (this.state_ == SingleGnubbySigner.State.INIT) {
    this.state_ = SingleGnubbySigner.State.OPENING;
    this.openCanceller_ = DEVICE_FACTORY_REGISTRY.getGnubbyFactory().openGnubby(
        this.gnubbyId_,
        this.forEnroll_,
        this.openCallback_.bind(this),
        appIdHash,
        this.logMsgUrl_,
        'singlesigner.js:SingleGnubbySigner.prototype.open_');
  }
};

/**
 * How long to delay retrying a failed open.
 */
SingleGnubbySigner.OPEN_DELAY_MILLIS = 200;

/**
 * How long to delay retrying a sign requiring touch.
 */
SingleGnubbySigner.SIGN_DELAY_MILLIS = 200;

/**
 * @param {number} rc The result of the open operation.
 * @param {Gnubby=} gnubby The opened gnubby, if open was successful (or busy).
 * @private
 */
SingleGnubbySigner.prototype.openCallback_ = function(rc, gnubby) {
  if (this.state_ != SingleGnubbySigner.State.OPENING &&
      this.state_ != SingleGnubbySigner.State.BUSY) {
    // Open completed after close, perhaps? Ignore.
    return;
  }

  switch (rc) {
    case DeviceStatusCodes.OK_STATUS:
      if (!gnubby) {
        console.warn(UTIL_fmt('open succeeded but gnubby is null, WTF?'));
      } else {
        this.gnubby_ = gnubby;
        this.gnubby_.version(this.versionCallback_.bind(this));
      }
      break;
    case DeviceStatusCodes.BUSY_STATUS:
      this.gnubby_ = gnubby;
      this.state_ = SingleGnubbySigner.State.BUSY;
      // If there's still time, retry the open.
      if (!this.timer_ || !this.timer_.expired()) {
        var self = this;
        window.setTimeout(function() {
          if (self.gnubby_) {
            this.openCanceller_ = DEVICE_FACTORY_REGISTRY
              .getGnubbyFactory().openGnubby(
                self.gnubbyId_,
                self.forEnroll_,
                self.openCallback_.bind(self),
                self.logMsgUrl_,
                'singlesigner.js:SingleGnubbySigner.prototype.openCallback_');
          }
        }, SingleGnubbySigner.OPEN_DELAY_MILLIS);
      } else {
        this.goToError_(DeviceStatusCodes.BUSY_STATUS);
      }
      break;
    default:
      // TODO: This won't be confused with success, but should it be
      // part of the same namespace as the other error codes, which are
      // always in DeviceStatusCodes.*?
      this.goToError_(rc, true);
  }
};

/**
 * Called with the result of a version command.
 * @param {number} rc Result of version command.
 * @param {ArrayBuffer=} opt_data Version.
 * @private
 */
SingleGnubbySigner.prototype.versionCallback_ = function(rc, opt_data) {
  if (rc == DeviceStatusCodes.BUSY_STATUS) {
    if (this.timer_ && this.timer_.expired()) {
      this.goToError_(DeviceStatusCodes.TIMEOUT_STATUS);
      return;
    }
    // There's still time: resync and retry.
    var self = this;
    this.gnubby_.sync(function(code) {
      if (code) {
        self.goToError_(code, true);
        return;
      }
      self.gnubby_.version(self.versionCallback_.bind(self));
    });
    return;
  }
  if (rc) {
    this.goToError_(rc, true);
    return;
  }
  this.state_ = SingleGnubbySigner.State.IDLE;
  this.version_ = UTIL_BytesToString(new Uint8Array(opt_data || []));
  this.doSign_(this.challengeIndex_);
};

/**
 * @param {number} challengeIndex Index of challenge to sign
 * @private
 */
SingleGnubbySigner.prototype.doSign_ = function(challengeIndex) {
  if (!this.gnubby_) {
    // Already closed? Nothing to do.
    return;
  }
  if (this.timer_ && this.timer_.expired()) {
    // If the timer is expired, that means we never got a success response.
    // We could have gotten wrong data on a partial set of challenges, but this
    // means we don't yet know the final outcome. In any event, we don't yet
    // know the final outcome: return timeout.
    this.goToError_(DeviceStatusCodes.TIMEOUT_STATUS);
    return;
  }
  if (!this.challengesSet_) {
    this.state_ = SingleGnubbySigner.State.IDLE;
    return;
  }

  this.state_ = SingleGnubbySigner.State.SIGNING;

  if (challengeIndex >= this.challenges_.length) {
    this.signCallback_(challengeIndex, DeviceStatusCodes.WRONG_DATA_STATUS);
    return;
  }

  var challenge = this.challenges_[challengeIndex];
  var challengeHash = challenge.challengeHash;
  var appIdHash = challenge.appIdHash;
  var keyHandle = challenge.keyHandle;
  if (this.cachedError_.hasOwnProperty(keyHandle)) {
    // Cache hit: return wrong data again.
    this.signCallback_(challengeIndex, this.cachedError_[keyHandle]);
  } else if (challenge.version && challenge.version != this.version_) {
    // Sign challenge for a different version of gnubby: return wrong data.
    this.signCallback_(challengeIndex, DeviceStatusCodes.WRONG_DATA_STATUS);
  } else {
    var nowink = false;
    this.gnubby_.sign(challengeHash, appIdHash, keyHandle,
        this.signCallback_.bind(this, challengeIndex),
        nowink);
  }
};

/**
 * @param {number} code The result of a sign operation.
 * @return {boolean} Whether the error indicates the key handle is invalid
 *     for this gnubby.
 */
SingleGnubbySigner.signErrorIndicatesInvalidKeyHandle = function(code) {
  return (code == DeviceStatusCodes.WRONG_DATA_STATUS ||
      code == DeviceStatusCodes.WRONG_LENGTH_STATUS ||
      code == DeviceStatusCodes.INVALID_DATA_STATUS);
};

/**
 * Called with the result of a single sign operation.
 * @param {number} challengeIndex the index of the challenge just attempted
 * @param {number} code the result of the sign operation
 * @param {ArrayBuffer=} opt_info Optional result data
 * @private
 */
SingleGnubbySigner.prototype.signCallback_ =
    function(challengeIndex, code, opt_info) {
  console.log(UTIL_fmt('gnubby ' + JSON.stringify(this.gnubbyId_) +
      ', challenge ' + challengeIndex + ' yielded ' + code.toString(16)));
  if (this.state_ != SingleGnubbySigner.State.SIGNING) {
    console.log(UTIL_fmt('already done!'));
    // We're done, the caller's no longer interested.
    return;
  }

  // Cache certain idempotent errors, re-asking the gnubby to sign it
  // won't produce different results.
  if (SingleGnubbySigner.signErrorIndicatesInvalidKeyHandle(code)) {
    if (challengeIndex < this.challenges_.length) {
      var challenge = this.challenges_[challengeIndex];
      if (!this.cachedError_.hasOwnProperty(challenge.keyHandle)) {
        this.cachedError_[challenge.keyHandle] = code;
      }
    }
  }

  var self = this;
  switch (code) {
    case DeviceStatusCodes.GONE_STATUS:
      this.goToError_(code);
      break;

    case DeviceStatusCodes.TIMEOUT_STATUS:
      this.gnubby_.sync(this.synced_.bind(this));
      break;

    case DeviceStatusCodes.BUSY_STATUS:
      this.doSign_(this.challengeIndex_);
      break;

    case DeviceStatusCodes.OK_STATUS:
      // Lower bound on the minimum length, signature length can vary.
      var MIN_SIGNATURE_LENGTH = 7;
      if (!opt_info || opt_info.byteLength < MIN_SIGNATURE_LENGTH) {
        console.error(UTIL_fmt('Got short response to sign request (' +
            (opt_info ? opt_info.byteLength : 0) + ' bytes), WTF?'));
      }
      if (this.forEnroll_) {
        this.goToError_(code);
      } else {
        this.goToSuccess_(code, this.challenges_[challengeIndex], opt_info);
      }
      break;

    case DeviceStatusCodes.WAIT_TOUCH_STATUS:
      window.setTimeout(function() {
        self.doSign_(self.challengeIndex_);
      }, SingleGnubbySigner.SIGN_DELAY_MILLIS);
      break;

    case DeviceStatusCodes.WRONG_DATA_STATUS:
    case DeviceStatusCodes.WRONG_LENGTH_STATUS:
    case DeviceStatusCodes.INVALID_DATA_STATUS:
      if (this.challengeIndex_ < this.challenges_.length - 1) {
        this.doSign_(++this.challengeIndex_);
      } else if (this.forEnroll_) {
        this.goToSuccess_(code);
      } else {
        this.goToError_(code);
      }
      break;

    default:
      if (this.forEnroll_) {
        this.goToError_(code, true);
      } else if (this.challengeIndex_ < this.challenges_.length - 1) {
        this.doSign_(++this.challengeIndex_);
      } else {
        this.goToError_(code, true);
      }
  }
};

/**
 * Called with the response of a sync command, called when a sign yields a
 * timeout to reassert control over the gnubby.
 * @param {number} code Error code
 * @private
 */
SingleGnubbySigner.prototype.synced_ = function(code) {
  if (code) {
    this.goToError_(code, true);
    return;
  }
  this.doSign_(this.challengeIndex_);
};

/**
 * Switches to the error state, and notifies caller.
 * @param {number} code Error code
 * @param {boolean=} opt_warn Whether to warn in the console about the error.
 * @private
 */
SingleGnubbySigner.prototype.goToError_ = function(code, opt_warn) {
  this.state_ = SingleGnubbySigner.State.COMPLETE;
  var logFn = opt_warn ? console.warn.bind(console) : console.log.bind(console);
  logFn(UTIL_fmt('failed (' + code.toString(16) + ')'));
  var result = { code: code };
  if (!this.forEnroll_ && code == DeviceStatusCodes.WRONG_DATA_STATUS) {
    // When a device yields WRONG_DATA to all sign challenges, and this is a
    // sign request, we don't want to yield to the web page that it's not
    // enrolled just yet: we want the user to tap the device first. We'll
    // report the gnubby to the caller and let it close it instead of closing
    // it here.
    result.gnubby = this.gnubby_;
  } else {
    // Since this gnubby can no longer produce a useful result, go ahead and
    // close it.
    this.close();
  }
  this.completeCb_(result);
};

/**
 * Switches to the success state, and notifies caller.
 * @param {number} code Status code
 * @param {SignHelperChallenge=} opt_challenge The challenge signed
 * @param {ArrayBuffer=} opt_info Optional result data
 * @private
 */
SingleGnubbySigner.prototype.goToSuccess_ =
    function(code, opt_challenge, opt_info) {
  this.state_ = SingleGnubbySigner.State.COMPLETE;
  console.log(UTIL_fmt('success (' + code.toString(16) + ')'));
  var result = { code: code, gnubby: this.gnubby_ };
  if (opt_challenge || opt_info) {
    if (opt_challenge) {
      result['challenge'] = opt_challenge;
    }
    if (opt_info) {
      result['info'] = opt_info;
    }
  }
  this.completeCb_(result);
  // this.gnubby_ is now owned by completeCb_.
  this.gnubby_ = null;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview A multiple gnubby signer wraps the process of opening a number
 * of gnubbies, signing each challenge in an array of challenges until a
 * success condition is satisfied, and yielding each succeeding gnubby.
 *
 */
'use strict';

/**
 * @typedef {{
 *   code: number,
 *   gnubbyId: GnubbyDeviceId,
 *   challenge: (SignHelperChallenge|undefined),
 *   info: (ArrayBuffer|undefined)
 * }}
 */
var MultipleSignerResult;

/**
 * Creates a new sign handler that manages signing with all the available
 * gnubbies.
 * @param {boolean} forEnroll Whether this signer is signing for an attempted
 *     enroll operation.
 * @param {function(boolean)} allCompleteCb Called when this signer completes
 *     sign attempts, i.e. no further results will be produced. The parameter
 *     indicates whether any gnubbies are present that have not yet produced a
 *     final result.
 * @param {function(MultipleSignerResult, boolean)} gnubbyCompleteCb
 *     Called with each gnubby/challenge that yields a final result, along with
 *     whether this signer expects to produce more results. The boolean is a
 *     hint rather than a promise: it's possible for this signer to produce
 *     further results after saying it doesn't expect more, or to fail to
 *     produce further results after saying it does.
 * @param {number} timeoutMillis A timeout value, beyond whose expiration the
 *     signer will not attempt any new operations, assuming the caller is no
 *     longer interested in the outcome.
 * @param {string=} opt_logMsgUrl A URL to post log messages to.
 * @constructor
 */
function MultipleGnubbySigner(forEnroll, allCompleteCb, gnubbyCompleteCb,
    timeoutMillis, opt_logMsgUrl) {
  /** @private {boolean} */
  this.forEnroll_ = forEnroll;
  /** @private {function(boolean)} */
  this.allCompleteCb_ = allCompleteCb;
  /** @private {function(MultipleSignerResult, boolean)} */
  this.gnubbyCompleteCb_ = gnubbyCompleteCb;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;

  /** @private {Array<SignHelperChallenge>} */
  this.challenges_ = [];
  /** @private {boolean} */
  this.challengesSet_ = false;
  /** @private {boolean} */
  this.complete_ = false;
  /** @private {number} */
  this.numComplete_ = 0;
  /** @private {!Object<string, GnubbyTracker>} */
  this.gnubbies_ = {};
  /** @private {Countdown} */
  this.timer_ = DEVICE_FACTORY_REGISTRY.getCountdownFactory()
      .createTimer(timeoutMillis);
  /** @private {Countdown} */
  this.reenumerateTimer_ = DEVICE_FACTORY_REGISTRY.getCountdownFactory()
      .createTimer(timeoutMillis);
}

/**
 * @typedef {{
 *   index: string,
 *   signer: SingleGnubbySigner,
 *   stillGoing: boolean,
 *   errorStatus: number
 * }}
 */
var GnubbyTracker;

/**
 * Closes this signer's gnubbies, if any are open.
 */
MultipleGnubbySigner.prototype.close = function() {
  for (var k in this.gnubbies_) {
    this.gnubbies_[k].signer.close();
  }
  this.reenumerateTimer_.clearTimeout();
  this.timer_.clearTimeout();
  if (this.reenumerateIntervalTimer_) {
    this.reenumerateIntervalTimer_.clearTimeout();
  }
};

/**
 * Begins signing the given challenges.
 * @param {Array<SignHelperChallenge>} challenges The challenges to sign.
 * @return {boolean} whether the challenges were successfully added.
 */
MultipleGnubbySigner.prototype.doSign = function(challenges) {
  if (this.challengesSet_) {
    // Can't add new challenges once they're finalized.
    return false;
  }

  if (challenges) {
    for (var i = 0; i < challenges.length; i++) {
      var decodedChallenge = {};
      var challenge = challenges[i];
      decodedChallenge['challengeHash'] =
          B64_decode(challenge['challengeHash']);
      decodedChallenge['appIdHash'] = B64_decode(challenge['appIdHash']);
      decodedChallenge['keyHandle'] = B64_decode(challenge['keyHandle']);
      if (challenge['version']) {
        decodedChallenge['version'] = challenge['version'];
      }
      this.challenges_.push(decodedChallenge);
    }
  }
  this.challengesSet_ = true;
  this.enumerateGnubbies_();
  return true;
};

/**
 * Signals this signer to rescan for gnubbies. Useful when the caller has
 * knowledge that the last device has been removed, and can notify this class
 * before it will discover it on its own.
 */
MultipleGnubbySigner.prototype.reScanDevices = function() {
  if (this.reenumerateIntervalTimer_) {
    this.reenumerateIntervalTimer_.clearTimeout();
  }
  this.maybeReEnumerateGnubbies_(true);
};

/**
 * Enumerates gnubbies.
 * @private
 */
MultipleGnubbySigner.prototype.enumerateGnubbies_ = function() {
  DEVICE_FACTORY_REGISTRY.getGnubbyFactory().enumerate(
      this.enumerateCallback_.bind(this));
};

/**
 * Called with the result of enumerating gnubbies.
 * @param {number} rc The return code from enumerating.
 * @param {Array<GnubbyDeviceId>} ids The gnubbies enumerated.
 * @private
 */
MultipleGnubbySigner.prototype.enumerateCallback_ = function(rc, ids) {
  if (this.complete_) {
    return;
  }
  if (rc || !ids || !ids.length) {
    this.maybeReEnumerateGnubbies_(true);
    return;
  }
  for (var i = 0; i < ids.length; i++) {
    this.addGnubby_(ids[i]);
  }
  this.maybeReEnumerateGnubbies_(false);
};

/**
 * How frequently to reenumerate gnubbies when none are found, in milliseconds.
 * @const
 */
MultipleGnubbySigner.ACTIVE_REENUMERATE_INTERVAL_MILLIS = 200;

/**
 * How frequently to reenumerate gnubbies when some are found, in milliseconds.
 * @const
 */
MultipleGnubbySigner.PASSIVE_REENUMERATE_INTERVAL_MILLIS = 3000;

/**
 * Reenumerates gnubbies if there's still time.
 * @param {boolean} activeScan Whether to poll more aggressively, e.g. if
 *     there are no devices present.
 * @private
 */
MultipleGnubbySigner.prototype.maybeReEnumerateGnubbies_ =
    function(activeScan) {
  if (this.reenumerateTimer_.expired()) {
    // If the timer is expired, call timeout_ if there aren't any still-running
    // gnubbies. (If there are some still running, the last will call timeout_
    // itself.)
    if (!this.anyPending_()) {
      this.timeout_(false);
    }
    return;
  }
  // Reenumerate more aggressively if there are no gnubbies present than if
  // there are any.
  var reenumerateTimeoutMillis;
  if (activeScan) {
    reenumerateTimeoutMillis =
        MultipleGnubbySigner.ACTIVE_REENUMERATE_INTERVAL_MILLIS;
  } else {
    reenumerateTimeoutMillis =
        MultipleGnubbySigner.PASSIVE_REENUMERATE_INTERVAL_MILLIS;
  }
  if (reenumerateTimeoutMillis >
      this.reenumerateTimer_.millisecondsUntilExpired()) {
    reenumerateTimeoutMillis =
        this.reenumerateTimer_.millisecondsUntilExpired();
  }
  /** @private {Countdown} */
  this.reenumerateIntervalTimer_ =
      DEVICE_FACTORY_REGISTRY.getCountdownFactory().createTimer(
          reenumerateTimeoutMillis, this.enumerateGnubbies_.bind(this));
};

/**
 * Adds a new gnubby to this signer's list of gnubbies. (Only possible while
 * this signer is still signing: without this restriction, the completed
 * callback could be called more than once, in violation of its contract.)
 * If this signer has challenges to sign, begins signing on the new gnubby with
 * them.
 * @param {GnubbyDeviceId} gnubbyId The id of the gnubby to add.
 * @return {boolean} Whether the gnubby was added successfully.
 * @private
 */
MultipleGnubbySigner.prototype.addGnubby_ = function(gnubbyId) {
  var index = JSON.stringify(gnubbyId);
  if (this.gnubbies_.hasOwnProperty(index)) {
    // Can't add the same gnubby twice.
    return false;
  }
  var tracker = {
      index: index,
      errorStatus: 0,
      stillGoing: false,
      signer: null
  };
  tracker.signer = new SingleGnubbySigner(
      gnubbyId,
      this.forEnroll_,
      this.signCompletedCallback_.bind(this, tracker),
      this.timer_.clone(),
      this.logMsgUrl_);
  this.gnubbies_[index] = tracker;
  this.gnubbies_[index].stillGoing =
      tracker.signer.doSign(this.challenges_);
  if (!this.gnubbies_[index].errorStatus) {
    this.gnubbies_[index].errorStatus = 0;
  }
  return true;
};

/**
 * Called by a SingleGnubbySigner upon completion.
 * @param {GnubbyTracker} tracker The tracker object of the gnubby whose result
 *     this is.
 * @param {SingleSignerResult} result The result of the sign operation.
 * @private
 */
MultipleGnubbySigner.prototype.signCompletedCallback_ =
    function(tracker, result) {
  console.log(
      UTIL_fmt((result.code ? 'failure.' : 'success!') +
          ' gnubby ' + tracker.index +
          ' got code ' + result.code.toString(16)));
  if (!tracker.stillGoing) {
    console.log(UTIL_fmt('gnubby ' + tracker.index + ' no longer running!'));
    // Shouldn't ever happen? Disregard.
    return;
  }
  tracker.stillGoing = false;
  tracker.errorStatus = result.code;
  var moreExpected = this.tallyCompletedGnubby_();
  switch (result.code) {
    case DeviceStatusCodes.GONE_STATUS:
      // Squelch removed gnubbies: the caller can't act on them. But if this
      // was the last one, speed up reenumerating.
      if (!moreExpected) {
        this.maybeReEnumerateGnubbies_(true);
      }
      break;

    default:
      // Report any other results directly to the caller.
      this.notifyGnubbyComplete_(tracker, result, moreExpected);
      break;
  }
  if (!moreExpected && this.timer_.expired()) {
    this.timeout_(false);
  }
};

/**
 * Counts another gnubby has having completed, and returns whether more results
 * are expected.
 * @return {boolean} Whether more gnubbies are still running.
 * @private
 */
MultipleGnubbySigner.prototype.tallyCompletedGnubby_ = function() {
  this.numComplete_++;
  return this.anyPending_();
};

/**
 * @return {boolean} Whether more gnubbies are still running.
 * @private
 */
MultipleGnubbySigner.prototype.anyPending_ = function() {
  return this.numComplete_ < Object.keys(this.gnubbies_).length;
};

/**
 * Called upon timeout.
 * @param {boolean} anyPending Whether any gnubbies are awaiting results.
 * @private
 */
MultipleGnubbySigner.prototype.timeout_ = function(anyPending) {
  if (this.complete_) return;
  this.complete_ = true;
  // Defer notifying the caller that all are complete, in case the caller is
  // doing work in response to a gnubbyFound callback and has an inconsistent
  // view of the state of this signer.
  var self = this;
  window.setTimeout(function() {
    self.allCompleteCb_(anyPending);
  }, 0);
};

/**
 * @param {GnubbyTracker} tracker The tracker object of the gnubby whose result
 *     this is.
 * @param {SingleSignerResult} result Result object.
 * @param {boolean} moreExpected Whether more gnubbies may still produce an
 *     outcome.
 * @private
 */
MultipleGnubbySigner.prototype.notifyGnubbyComplete_ =
    function(tracker, result, moreExpected) {
  console.log(UTIL_fmt('gnubby ' + tracker.index + ' complete (' +
      result.code.toString(16) + ')'));
  var signResult = {
    'code': result.code,
    'gnubby': result.gnubby,
    'gnubbyId': tracker.signer.getDeviceId()
  };
  if (result['challenge'])
    signResult['challenge'] = result['challenge'];
  if (result['info'])
    signResult['info'] = result['info'];
  this.gnubbyCompleteCb_(signResult, moreExpected);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a sign handler using USB gnubbies.
 */
'use strict';

var CORRUPT_sign = false;

/**
 * @param {!SignHelperRequest} request The sign request.
 * @constructor
 * @implements {RequestHandler}
 */
function UsbSignHandler(request) {
  /** @private {!SignHelperRequest} */
  this.request_ = request;

  /** @private {boolean} */
  this.notified_ = false;
  /** @private {boolean} */
  this.anyGnubbiesFound_ = false;
  /** @private {!Array<!Gnubby>} */
  this.notEnrolledGnubbies_ = [];
}

/**
 * Default timeout value in case the caller never provides a valid timeout.
 * @const
 */
UsbSignHandler.DEFAULT_TIMEOUT_MILLIS = 30 * 1000;

/**
 * Attempts to run this handler's request.
 * @param {RequestHandlerCallback} cb Called with the result of the request and
 *     an optional source for the sign result.
 * @return {boolean} whether this set of challenges was accepted.
 */
UsbSignHandler.prototype.run = function(cb) {
  if (this.cb_) {
    // Can only handle one request.
    return false;
  }
  /** @private {RequestHandlerCallback} */
  this.cb_ = cb;
  if (!this.request_.signData || !this.request_.signData.length) {
    // Fail a sign request with an empty set of challenges.
    return false;
  }
  var timeoutMillis =
      this.request_.timeoutSeconds ?
      this.request_.timeoutSeconds * 1000 :
      UsbSignHandler.DEFAULT_TIMEOUT_MILLIS;
  /** @private {MultipleGnubbySigner} */
  this.signer_ = new MultipleGnubbySigner(
      false /* forEnroll */,
      this.signerCompleted_.bind(this),
      this.signerFoundGnubby_.bind(this),
      timeoutMillis,
      this.request_.logMsgUrl);
  return this.signer_.doSign(this.request_.signData);
};


/**
 * Called when a MultipleGnubbySigner completes.
 * @param {boolean} anyPending Whether any gnubbies are pending.
 * @private
 */
UsbSignHandler.prototype.signerCompleted_ = function(anyPending) {
  if (!this.anyGnubbiesFound_ || anyPending) {
    this.notifyError_(DeviceStatusCodes.TIMEOUT_STATUS);
  } else if (this.signerError_ !== undefined) {
    this.notifyError_(this.signerError_);
  } else {
    // Do nothing: signerFoundGnubby_ will have returned results from other
    // gnubbies.
  }
};

/**
 * Called when a MultipleGnubbySigner finds a gnubby that has completed signing
 * its challenges.
 * @param {MultipleSignerResult} signResult Signer result object
 * @param {boolean} moreExpected Whether the signer expects to produce more
 *     results.
 * @private
 */
UsbSignHandler.prototype.signerFoundGnubby_ =
    function(signResult, moreExpected) {
  this.anyGnubbiesFound_ = true;
  if (!signResult.code) {
    var gnubby = signResult['gnubby'];
    var challenge = signResult['challenge'];
    var info = new Uint8Array(signResult['info']);
    this.notifySuccess_(gnubby, challenge, info);
  } else if (signResult.code == DeviceStatusCodes.WRONG_DATA_STATUS) {
    var gnubby = signResult['gnubby'];
    this.notEnrolledGnubbies_.push(gnubby);
    this.sendBogusEnroll_(gnubby);
  } else if (!moreExpected) {
    // If the signer doesn't expect more results, return the error directly to
    // the caller.
    this.notifyError_(signResult.code);
  } else {
    // Record the last error, to report from the complete callback if no other
    // eligible gnubbies are found.
    /** @private {number} */
    this.signerError_ = signResult.code;
  }
};

/** @const */
UsbSignHandler.BOGUS_APP_ID_HASH = [
    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
    0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41
];

/** @const */
UsbSignHandler.BOGUS_CHALLENGE_V1 = [
    0x04, 0xA2, 0x24, 0x7D, 0x5C, 0x0B, 0x76, 0xF1,
    0xDC, 0xCD, 0x44, 0xAF, 0x91, 0x9A, 0xA2, 0x3F,
    0x3F, 0xBA, 0x65, 0x9F, 0x06, 0x78, 0x82, 0xFB,
    0x93, 0x4B, 0xBF, 0x86, 0x55, 0x95, 0x66, 0x46,
    0x76, 0x90, 0xDC, 0xE1, 0xE8, 0x6C, 0x86, 0x86,
    0xC3, 0x03, 0x4E, 0x65, 0x52, 0x4C, 0x32, 0x6F,
    0xB6, 0x44, 0x0D, 0x50, 0xF9, 0x16, 0xC0, 0xA3,
    0xDA, 0x31, 0x4B, 0xD3, 0x3F, 0x94, 0xA5, 0xF1,
    0xD3
];

/** @const */
UsbSignHandler.BOGUS_CHALLENGE_V2 = [
    0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
    0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
    0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42,
    0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42, 0x42
];

/**
 * Sends a bogus enroll command to the not-enrolled gnubby, to force the user
 * to tap the gnubby before revealing its state to the caller.
 * @param {Gnubby} gnubby The gnubby to "enroll" on.
 * @private
 */
UsbSignHandler.prototype.sendBogusEnroll_ = function(gnubby) {
  var self = this;
  gnubby.version(function(rc, opt_data) {
    if (rc) {
      self.notifyError_(rc);
      return;
    }
    var enrollChallenge;
    var version = UTIL_BytesToString(new Uint8Array(opt_data || []));
    switch (version) {
      case Gnubby.U2F_V1:
        enrollChallenge = UsbSignHandler.BOGUS_CHALLENGE_V1;
        break;
      case Gnubby.U2F_V2:
        enrollChallenge = UsbSignHandler.BOGUS_CHALLENGE_V2;
        break;
      default:
        self.notifyError_(DeviceStatusCodes.INVALID_DATA_STATUS);
    }
    gnubby.enroll(
        /** @type {Array<number>} */ (enrollChallenge),
        UsbSignHandler.BOGUS_APP_ID_HASH,
        self.enrollCallback_.bind(self, gnubby));
  });
};

/**
 * Called with the result of the (bogus, tap capturing) enroll command.
 * @param {Gnubby} gnubby The gnubby "enrolled".
 * @param {number} code The result of the enroll command.
 * @param {ArrayBuffer=} infoArray Returned data.
 * @private
 */
UsbSignHandler.prototype.enrollCallback_ = function(gnubby, code, infoArray) {
  if (this.notified_)
    return;
  switch (code) {
    case DeviceStatusCodes.WAIT_TOUCH_STATUS:
      this.sendBogusEnroll_(gnubby);
      return;

    case DeviceStatusCodes.OK_STATUS:
      // Got a successful enroll => user tapped gnubby.
      // Send a WRONG_DATA_STATUS finally. (The gnubby is implicitly closed
      // by notifyError_.)
      this.notifyError_(DeviceStatusCodes.WRONG_DATA_STATUS);
      return;
  }
};

/**
 * Reports the result of a successful sign operation.
 * @param {Gnubby} gnubby Gnubby instance
 * @param {SignHelperChallenge} challenge Challenge signed
 * @param {Uint8Array} info Result data
 * @private
 */
UsbSignHandler.prototype.notifySuccess_ = function(gnubby, challenge, info) {
  if (this.notified_)
    return;
  this.notified_ = true;

  gnubby.closeWhenIdle();
  this.close();

  if (CORRUPT_sign) {
    CORRUPT_sign = false;
    info[info.length - 1] = info[info.length - 1] ^ 0xff;
  }
  var responseData = {
    'appIdHash': B64_encode(challenge['appIdHash']),
    'challengeHash': B64_encode(challenge['challengeHash']),
    'keyHandle': B64_encode(challenge['keyHandle']),
    'signatureData': B64_encode(info)
  };
  var reply = {
    'type': 'sign_helper_reply',
    'code': DeviceStatusCodes.OK_STATUS,
    'responseData': responseData
  };
  this.cb_(reply, 'USB');
};

/**
 * Reports error to the caller.
 * @param {number} code error to report
 * @private
 */
UsbSignHandler.prototype.notifyError_ = function(code) {
  if (this.notified_)
    return;
  this.notified_ = true;
  this.close();
  var reply = {
    'type': 'sign_helper_reply',
    'code': code
  };
  this.cb_(reply);
};

/**
 * Closes the MultipleGnubbySigner, if any.
 */
UsbSignHandler.prototype.close = function() {
  while (this.notEnrolledGnubbies_.length != 0) {
    var gnubby = this.notEnrolledGnubbies_.shift();
    gnubby.closeWhenIdle();
  }
  if (this.signer_) {
    this.signer_.close();
    this.signer_ = null;
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Does common handling for requests coming from web pages and
 * routes them to the provided handler.
 */

/**
 * FIDO U2F Javascript API Version
 * @const
 * @type {number}
 */
var JS_API_VERSION = 1.1;

/**
 * Gets the scheme + origin from a web url.
 * @param {string} url Input url
 * @return {?string} Scheme and origin part if url parses
 */
function getOriginFromUrl(url) {
  var re = new RegExp('^(https?://)[^/]*/?');
  var originarray = re.exec(url);
  if (originarray == null) return originarray;
  var origin = originarray[0];
  while (origin.charAt(origin.length - 1) == '/') {
    origin = origin.substring(0, origin.length - 1);
  }
  if (origin == 'http:' || origin == 'https:')
    return null;
  return origin;
}

/**
 * Returns whether the registered key appears to be valid.
 * @param {Object} registeredKey The registered key object.
 * @param {boolean} appIdRequired Whether the appId property is required on
 *     each challenge.
 * @return {boolean} Whether the object appears valid.
 */
function isValidRegisteredKey(registeredKey, appIdRequired) {
  if (appIdRequired && !registeredKey.hasOwnProperty('appId')) {
    return false;
  }
  if (!registeredKey.hasOwnProperty('keyHandle'))
    return false;
  if (registeredKey['version']) {
    if (registeredKey['version'] != 'U2F_V1' &&
        registeredKey['version'] != 'U2F_V2') {
      return false;
    }
  }
  return true;
}

/**
 * Returns whether the array of registered keys appears to be valid.
 * @param {Array<Object>} registeredKeys The array of registered keys.
 * @param {boolean} appIdRequired Whether the appId property is required on
 *     each challenge.
 * @return {boolean} Whether the array appears valid.
 */
function isValidRegisteredKeyArray(registeredKeys, appIdRequired) {
  return registeredKeys.every(function(key) {
    return isValidRegisteredKey(key, appIdRequired);
  });
}

/**
 * Gets the sign challenges from the request. The sign challenges may be the
 * U2F 1.0 variant, signRequests, or the U2F 1.1 version, registeredKeys.
 * @param {Object} request The request.
 * @return {!Array<SignChallenge>|undefined} The sign challenges, if found.
 */
function getSignChallenges(request) {
  if (!request) {
    return undefined;
  }
  var signChallenges;
  if (request.hasOwnProperty('signRequests')) {
    signChallenges = request['signRequests'];
  } else if (request.hasOwnProperty('registeredKeys')) {
    signChallenges = request['registeredKeys'];
  }
  return signChallenges;
}

/**
 * Returns whether the array of SignChallenges appears to be valid.
 * @param {Array<SignChallenge>} signChallenges The array of sign challenges.
 * @param {boolean} challengeValueRequired Whether each challenge object
 *     requires a challenge value.
 * @param {boolean} appIdRequired Whether the appId property is required on
 *     each challenge.
 * @return {boolean} Whether the array appears valid.
 */
function isValidSignChallengeArray(signChallenges, challengeValueRequired,
    appIdRequired) {
  for (var i = 0; i < signChallenges.length; i++) {
    var incomingChallenge = signChallenges[i];
    if (challengeValueRequired &&
        !incomingChallenge.hasOwnProperty('challenge'))
      return false;
    if (!isValidRegisteredKey(incomingChallenge, appIdRequired)) {
      return false;
    }
  }
  return true;
}

/**
 * @param {Object} request Request object
 * @param {MessageSender} sender Sender frame
 * @param {Function} sendResponse Response callback
 * @return {?Closeable} Optional handler object that should be closed when port
 *     closes
 */
function handleWebPageRequest(request, sender, sendResponse) {
  switch (request.type) {
    case MessageTypes.U2F_REGISTER_REQUEST:
      return handleU2fEnrollRequest(sender, request, sendResponse);

    case MessageTypes.U2F_SIGN_REQUEST:
      return handleU2fSignRequest(sender, request, sendResponse);

    case MessageTypes.U2F_GET_API_VERSION_REQUEST:
      sendResponse(
          makeU2fGetApiVersionResponse(request, JS_API_VERSION,
              MessageTypes.U2F_GET_API_VERSION_RESPONSE));
      return null;

    default:
      sendResponse(
          makeU2fErrorResponse(request, ErrorCodes.BAD_REQUEST, undefined,
              MessageTypes.U2F_REGISTER_RESPONSE));
      return null;
  }
}

/**
 * Makes a response to a request.
 * @param {Object} request The request to make a response to.
 * @param {string} responseSuffix How to name the response's type.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The response object.
 */
function makeResponseForRequest(request, responseSuffix, opt_defaultType) {
  var type;
  if (request && request.type) {
    type = request.type.replace(/_request$/, responseSuffix);
  } else {
    type = opt_defaultType;
  }
  var reply = { 'type': type };
  if (request && request.requestId) {
    reply.requestId = request.requestId;
  }
  return reply;
}

/**
 * Makes a response to a U2F request with an error code.
 * @param {Object} request The request to make a response to.
 * @param {ErrorCodes} code The error code to return.
 * @param {string=} opt_detail An error detail string.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The U2F error.
 */
function makeU2fErrorResponse(request, code, opt_detail, opt_defaultType) {
  var reply = makeResponseForRequest(request, '_response', opt_defaultType);
  var error = {'errorCode': code};
  if (opt_detail) {
    error['errorMessage'] = opt_detail;
  }
  reply['responseData'] = error;
  return reply;
}

/**
 * Makes a success response to a web request with a responseData object.
 * @param {Object} request The request to make a response to.
 * @param {Object} responseData The response data.
 * @return {Object} The web error.
 */
function makeU2fSuccessResponse(request, responseData) {
  var reply = makeResponseForRequest(request, '_response');
  reply['responseData'] = responseData;
  return reply;
}

/**
 * Maps a helper's error code from the DeviceStatusCodes namespace to a
 * U2fError.
 * @param {number} code Error code from DeviceStatusCodes namespace.
 * @return {U2fError} An error.
 */
function mapDeviceStatusCodeToU2fError(code) {
  switch (code) {
    case DeviceStatusCodes.WRONG_DATA_STATUS:
      return {errorCode: ErrorCodes.DEVICE_INELIGIBLE};

    case DeviceStatusCodes.TIMEOUT_STATUS:
    case DeviceStatusCodes.WAIT_TOUCH_STATUS:
      return {errorCode: ErrorCodes.TIMEOUT};

    default:
      var reportedError = {
        errorCode: ErrorCodes.OTHER_ERROR,
        errorMessage: 'device status code: ' + code.toString(16)
      };
      return reportedError;
  }
}

/**
 * Sends a response, using the given sentinel to ensure at most one response is
 * sent. Also closes the closeable, if it's given.
 * @param {boolean} sentResponse Whether a response has already been sent.
 * @param {?Closeable} closeable A thing to close.
 * @param {*} response The response to send.
 * @param {Function} sendResponse A function to send the response.
 */
function sendResponseOnce(sentResponse, closeable, response, sendResponse) {
  if (closeable) {
    closeable.close();
  }
  if (!sentResponse) {
    sentResponse = true;
    try {
      // If the page has gone away or the connection has otherwise gone,
      // sendResponse fails.
      sendResponse(response);
    } catch (exception) {
      console.warn('sendResponse failed: ' + exception);
    }
  } else {
    console.warn(UTIL_fmt('Tried to reply more than once!'));
  }
}

/**
 * @param {!string} string Input string
 * @return {Array<number>} SHA256 hash value of string.
 */
function sha256HashOfString(string) {
  var s = new SHA256();
  s.update(UTIL_StringToBytes(string));
  return s.digest();
}

var UNUSED_CID_PUBKEY_VALUE = 'unused';

/**
 * Normalizes the TLS channel ID value:
 * 1. Converts semantically empty values (undefined, null, 0) to the empty
 *     string.
 * 2. Converts valid JSON strings to a JS object.
 * 3. Otherwise, returns the input value unmodified.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel id
 * @return {Object|string} The normalized TLS channel ID value.
 */
function tlsChannelIdValue(opt_tlsChannelId) {
  if (!opt_tlsChannelId) {
    // Case 1: Always set some value for TLS channel ID, even if it's the empty
    // string: this browser definitely supports them.
    return UNUSED_CID_PUBKEY_VALUE;
  }
  if (typeof opt_tlsChannelId === 'string') {
    try {
      var obj = JSON.parse(opt_tlsChannelId);
      if (!obj) {
        // Case 1: The string value 'null' parses as the Javascript object null,
        // so return an empty string: the browser definitely supports TLS
        // channel id.
        return UNUSED_CID_PUBKEY_VALUE;
      }
      // Case 2: return the value as a JS object.
      return /** @type {Object} */ (obj);
    } catch (e) {
      console.warn('Unparseable TLS channel ID value ' + opt_tlsChannelId);
      // Case 3: return the value unmodified.
    }
  }
  return opt_tlsChannelId;
}

/**
 * Creates a browser data object with the given values.
 * @param {!string} type A string representing the "type" of this browser data
 *     object.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeBrowserData(type, serverChallenge, origin, opt_tlsChannelId) {
  var browserData = {
    'typ' : type,
    'challenge' : serverChallenge,
    'origin' : origin
  };
  if (BROWSER_SUPPORTS_TLS_CHANNEL_ID) {
    browserData['cid_pubkey'] = tlsChannelIdValue(opt_tlsChannelId);
  }
  return JSON.stringify(browserData);
}

/**
 * Creates a browser data object for an enroll request with the given values.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeEnrollBrowserData(serverChallenge, origin, opt_tlsChannelId) {
  return makeBrowserData(
      'navigator.id.finishEnrollment', serverChallenge, origin,
      opt_tlsChannelId);
}

/**
 * Creates a browser data object for a sign request with the given values.
 * @param {!string} serverChallenge The server's challenge, as a base64-
 *     encoded string.
 * @param {!string} origin The server's origin, as seen by the browser.
 * @param {Object|string|undefined} opt_tlsChannelId TLS Channel Id
 * @return {string} A string representation of the browser data object.
 */
function makeSignBrowserData(serverChallenge, origin, opt_tlsChannelId) {
  return makeBrowserData(
      'navigator.id.getAssertion', serverChallenge, origin, opt_tlsChannelId);
}

/**
 * Makes a response to a U2F request with an error code.
 * @param {Object} request The request to make a response to.
 * @param {number=} version The JS API version to return.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {Object} The GetJsApiVersionResponse.
 */
function makeU2fGetApiVersionResponse(request, version, opt_defaultType) {
  var reply = makeResponseForRequest(request, '_response', opt_defaultType);
  var data = {'js_api_version': version};
  reply['responseData'] = data;
  return reply;
}

/**
 * Encodes the sign data as an array of sign helper challenges.
 * @param {Array<SignChallenge>} signChallenges The sign challenges to encode.
 * @param {string|undefined} opt_defaultChallenge A default sign challenge
 *     value, if a request does not provide one.
 * @param {string=} opt_defaultAppId The app id to use for each challenge, if
 *     the challenge contains none.
 * @param {function(string, string): string=} opt_challengeHashFunction
 *     A function that produces, from a key handle and a raw challenge, a hash
 *     of the raw challenge. If none is provided, a default hash function is
 *     used.
 * @return {!Array<SignHelperChallenge>} The sign challenges, encoded.
 */
function encodeSignChallenges(signChallenges, opt_defaultChallenge,
    opt_defaultAppId, opt_challengeHashFunction) {
  function encodedSha256(keyHandle, challenge) {
    return B64_encode(sha256HashOfString(challenge));
  }
  var challengeHashFn = opt_challengeHashFunction || encodedSha256;
  var encodedSignChallenges = [];
  if (signChallenges) {
    for (var i = 0; i < signChallenges.length; i++) {
      var challenge = signChallenges[i];
      var keyHandle = challenge['keyHandle'];
      var challengeValue;
      if (challenge.hasOwnProperty('challenge')) {
        challengeValue = challenge['challenge'];
      } else {
        challengeValue = opt_defaultChallenge;
      }
      var challengeHash = challengeHashFn(keyHandle, challengeValue);
      var appId;
      if (challenge.hasOwnProperty('appId')) {
        appId = challenge['appId'];
      } else {
        appId = opt_defaultAppId;
      }
      var encodedChallenge = {
        'challengeHash': challengeHash,
        'appIdHash': B64_encode(sha256HashOfString(appId)),
        'keyHandle': keyHandle,
        'version': (challenge['version'] || 'U2F_V1')
      };
      encodedSignChallenges.push(encodedChallenge);
    }
  }
  return encodedSignChallenges;
}

/**
 * Makes a sign helper request from an array of challenges.
 * @param {Array<SignHelperChallenge>} challenges The sign challenges.
 * @param {number=} opt_timeoutSeconds Timeout value.
 * @param {string=} opt_logMsgUrl URL to log to.
 * @return {SignHelperRequest} The sign helper request.
 */
function makeSignHelperRequest(challenges, opt_timeoutSeconds, opt_logMsgUrl) {
  var request = {
    'type': 'sign_helper_request',
    'signData': challenges,
    'timeout': opt_timeoutSeconds || 0,
    'timeoutSeconds': opt_timeoutSeconds || 0
  };
  if (opt_logMsgUrl !== undefined) {
    request.logMsgUrl = opt_logMsgUrl;
  }
  return request;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a check whether an app id lists an origin.
 */
'use strict';

/**
 * Parses the text as JSON and returns it as an array of strings.
 * @param {string} text Input JSON
 * @return {!Array<string>} Array of origins
 */
function getOriginsFromJson(text) {
  try {
    var urls, i;
    var appIdData = JSON.parse(text);
    if (Array.isArray(appIdData)) {
      // Older format where it is a simple list of facets
      urls = appIdData;
    } else {
      var trustedFacets = appIdData['trustedFacets'];
      if (trustedFacets) {
        var versionBlock;
        for (i = 0; versionBlock = trustedFacets[i]; i++) {
          if (versionBlock['version'] &&
              versionBlock['version']['major'] == 1 &&
              versionBlock['version']['minor'] == 0) {
            urls = versionBlock['ids'];
            break;
          }
        }
      }
      if (typeof urls == 'undefined') {
        throw Error('Could not find trustedFacets for version 1.0');
      }
    }
    var origins = {};
    var url;
    for (i = 0; url = urls[i]; i++) {
      var origin = getOriginFromUrl(url);
      if (origin) {
        origins[origin] = origin;
      }
    }
    return Object.keys(origins);
  } catch (e) {
    console.error(UTIL_fmt('could not parse ' + text));
    return [];
  }
}

/**
 * Retrieves a set of distinct app ids from the sign challenges.
 * @param {Array<SignChallenge>=} signChallenges Input sign challenges.
 * @return {Array<string>} array of distinct app ids.
 */
function getDistinctAppIds(signChallenges) {
  if (!signChallenges) {
    return [];
  }
  var appIds = {};
  for (var i = 0, request; request = signChallenges[i]; i++) {
    var appId = request['appId'];
    if (appId) {
      appIds[appId] = appId;
    }
  }
  return Object.keys(appIds);
}

/**
 * An object that checks one or more appIds' contents against an origin.
 * @interface
 */
function AppIdChecker() {}

/**
 * Checks whether the given origin is allowed by all of the given appIds.
 * @param {!Countdown} timer A timer by which to resolve all provided app ids.
 * @param {string} origin The origin to check.
 * @param {!Array<string>} appIds The app ids to check.
 * @param {boolean} allowHttp Whether to allow http:// URLs.
 * @param {string=} opt_logMsgUrl A log message URL.
 * @return {Promise<boolean>} A promise for the result of the check
 */
AppIdChecker.prototype.checkAppIds =
    function(timer, origin, appIds, allowHttp, opt_logMsgUrl) {};

/**
 * An interface to create an AppIdChecker.
 * @interface
 */
function AppIdCheckerFactory() {}

/**
 * @return {!AppIdChecker} A new AppIdChecker.
 */
AppIdCheckerFactory.prototype.create = function() {};

/**
 * Provides an object to track checking a list of appIds.
 * @param {!TextFetcher} fetcher A URL fetcher.
 * @constructor
 * @implements AppIdChecker
 */
function XhrAppIdChecker(fetcher) {
  /** @private {!TextFetcher} */
  this.fetcher_ = fetcher;
}

/**
 * Checks whether all the app ids provided can be asserted by the given origin.
 * @param {!Countdown} timer A timer by which to resolve all provided app ids.
 * @param {string} origin The origin to check.
 * @param {!Array<string>} appIds The app ids to check.
 * @param {boolean} allowHttp Whether to allow http:// URLs.
 * @param {string=} opt_logMsgUrl A log message URL.
 * @return {Promise<boolean>} A promise for the result of the check
 */
XhrAppIdChecker.prototype.checkAppIds =
    function(timer, origin, appIds, allowHttp, opt_logMsgUrl) {
  if (this.timer_) {
    // Can't use the same object to check appIds more than once.
    return Promise.resolve(false);
  }

  /** @private {!Countdown} */
  this.timer_ = timer;
  /** @private {string} */
  this.origin_ = origin;
  var appIdsMap = {};
  if (appIds) {
    for (var i = 0; i < appIds.length; i++) {
      appIdsMap[appIds[i]] = appIds[i];
    }
  }
  /** @private {Array<string>} */
  this.distinctAppIds_ = Object.keys(appIdsMap);
  /** @private {boolean} */
  this.allowHttp_ = allowHttp;
  /** @private {string|undefined} */
  this.logMsgUrl_ = opt_logMsgUrl;
  if (!this.distinctAppIds_.length)
    return Promise.resolve(false);

  if (this.allAppIdsEqualOrigin_()) {
    // Trivially allowed.
    return Promise.resolve(true);
  } else {
    var self = this;
    // Begin checking remaining app ids.
    var appIdChecks = self.distinctAppIds_.map(self.checkAppId_.bind(self));
    return Promise.all(appIdChecks).then(function(results) {
      return results.every(function(result) {
        return result;
      });
    });
  }
};

/**
 * Checks if a single appId can be asserted by the given origin.
 * @param {string} appId The appId to check
 * @return {Promise<boolean>} A promise for the result of the check
 * @private
 */
XhrAppIdChecker.prototype.checkAppId_ = function(appId) {
  if (appId == this.origin_) {
    // Trivially allowed
    return Promise.resolve(true);
  }
  var p = this.fetchAllowedOriginsForAppId_(appId);
  var self = this;
  return p.then(function(allowedOrigins) {
    if (allowedOrigins.indexOf(self.origin_) == -1) {
      console.warn(UTIL_fmt('Origin ' + self.origin_ +
            ' not allowed by app id ' + appId));
      return false;
    }
    return true;
  });
};

/**
 * @return {boolean} Whether all the app ids being checked are equal to the
 * calling origin.
 * @private
 */
XhrAppIdChecker.prototype.allAppIdsEqualOrigin_ = function() {
  var self = this;
  return this.distinctAppIds_.every(function(appId) {
    return appId == self.origin_;
  });
};

/**
 * Fetches the allowed origins for an appId.
 * @param {string} appId Application id
 * @return {Promise<!Array<string>>} A promise for a list of allowed origins
 *     for appId
 * @private
 */
XhrAppIdChecker.prototype.fetchAllowedOriginsForAppId_ = function(appId) {
  if (!appId) {
    return Promise.resolve([]);
  }

  if (appId.indexOf('http://') == 0 && !this.allowHttp_) {
    console.log(UTIL_fmt('http app ids disallowed, ' + appId + ' requested'));
    return Promise.resolve([]);
  }

  var origin = getOriginFromUrl(appId);
  if (!origin) {
    return Promise.resolve([]);
  }

  var p = this.fetcher_.fetch(appId);
  var self = this;
  return p.then(getOriginsFromJson, function(rc_) {
    var rc = /** @type {number} */(rc_);
    console.log(UTIL_fmt('fetching ' + appId + ' failed: ' + rc));
    if (!(rc >= 400 && rc < 500) && !self.timer_.expired()) {
      // Retry
      return self.fetchAllowedOriginsForAppId_(appId);
    }
    return [];
  });
};

/**
 * A factory to create an XhrAppIdChecker.
 * @implements AppIdCheckerFactory
 * @param {!TextFetcher} fetcher
 * @constructor
 */
function XhrAppIdCheckerFactory(fetcher) {
  /** @private {!TextFetcher} */
  this.fetcher_ = fetcher;
}

/**
 * @return {!AppIdChecker} A new AppIdChecker.
 */
XhrAppIdCheckerFactory.prototype.create = function() {
  return new XhrAppIdChecker(this.fetcher_);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a helper using USB gnubbies.
 */
'use strict';

/**
 * @constructor
 * @extends {GenericRequestHelper}
 */
function UsbHelper() {
  GenericRequestHelper.apply(this, arguments);

  var self = this;
  this.registerHandlerFactory('enroll_helper_request', function(request) {
    return new UsbEnrollHandler(/** @type {EnrollHelperRequest} */ (request));
  });
  this.registerHandlerFactory('sign_helper_request', function(request) {
    return new UsbSignHandler(/** @type {SignHelperRequest} */ (request));
  });
}

inherits(UsbHelper, GenericRequestHelper);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a simple XmlHttpRequest-based text document
 * fetcher.
 *
 */
'use strict';

/**
 * A fetcher of text files.
 * @interface
 */
function TextFetcher() {}

/**
 * @param {string} url The URL to fetch.
 * @param {string?} opt_method The HTTP method to use (default GET)
 * @param {string?} opt_body The request body
 * @return {!Promise<string>} A promise for the fetched text. In case of an
 *     error, this promise is rejected with an HTTP status code.
 */
TextFetcher.prototype.fetch = function(url, opt_method, opt_body) {};

/**
 * @constructor
 * @implements {TextFetcher}
 */
function XhrTextFetcher() {
}

/**
 * @param {string} url The URL to fetch.
 * @param {string?} opt_method The HTTP method to use (default GET)
 * @param {string?} opt_body The request body
 * @return {!Promise<string>} A promise for the fetched text. In case of an
 *     error, this promise is rejected with an HTTP status code.
 */
XhrTextFetcher.prototype.fetch = function(url, opt_method, opt_body) {
  return new Promise(function(resolve, reject) {
    var xhr = new XMLHttpRequest();
    var method = opt_method || 'GET';
    xhr.open(method, url, true);
    xhr.onloadend = function() {
      if (xhr.status != 200) {
        reject(xhr.status);
        return;
      }
      resolve(xhr.responseText);
    };
    xhr.onerror = function() {
      // Treat any network-level errors as though the page didn't exist.
      reject(404);
    };
    if (opt_body)
      xhr.send(opt_body);
    else
      xhr.send();
  });
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a "bottom half" helper to assist with raw requests.
 * This fills the same role as the Authenticator-Specific Module component of
 * U2F documents, although the API is different.
 */
'use strict';

/**
 * @typedef {{
 *   type: string,
 *   timeout: number
 * }}
 */
var HelperRequest;

/**
 * @typedef {{
 *   type: string,
 *   code: (number|undefined)
 * }}
 */
var HelperReply;

/**
 * A helper to process requests.
 * @interface
 */
function RequestHelper() {}

/**
 * Gets a handler for a request.
 * @param {HelperRequest} request The request to handle.
 * @return {RequestHandler} A handler for the request.
 */
RequestHelper.prototype.getHandler = function(request) {};

/**
 * A handler to track an outstanding request.
 * @extends {Closeable}
 * @interface
 */
function RequestHandler() {}

/** @typedef {function(HelperReply, string=)} */
var RequestHandlerCallback;

/**
 * @param {RequestHandlerCallback} cb Called with the result of the request,
 *     and an optional source for the result.
 * @return {boolean} Whether this handler could be run.
 */
RequestHandler.prototype.run = function(cb) {};

/** Closes this handler. */
RequestHandler.prototype.close = function() {};

/**
 * Makes a response to a helper request with an error code.
 * @param {HelperRequest} request The request to make a response to.
 * @param {DeviceStatusCodes} code The error code to return.
 * @param {string=} opt_defaultType The default response type, if none is
 *     present in the request.
 * @return {HelperReply} The helper error response.
 */
function makeHelperErrorResponse(request, code, opt_defaultType) {
  var type;
  if (request && request.type) {
    type = request.type.replace(/_request$/, '_reply');
  } else {
    type = opt_defaultType || 'unknown_type_reply';
  }
  var reply = {
    'type': type,
    'code': /** @type {number} */ (code)
  };
  return reply;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview U2F message types.
 */
'use strict';

/**
 * Message types for messsages to/from the extension
 * @const
 * @enum {string}
 */
var MessageTypes = {
  U2F_REGISTER_REQUEST: 'u2f_register_request',
  U2F_SIGN_REQUEST: 'u2f_sign_request',
  U2F_REGISTER_RESPONSE: 'u2f_register_response',
  U2F_SIGN_RESPONSE: 'u2f_sign_response',
  U2F_GET_API_VERSION_REQUEST: 'u2f_get_api_version_request',
  U2F_GET_API_VERSION_RESPONSE: 'u2f_get_api_version_response'
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a partial copy of goog.inherits, so inheritance works
 * even in the absence of Closure.
 */
'use strict';

// A partial copy of goog.inherits, so inheritance works even in the absence of
// Closure.
function inherits(childCtor, parentCtor) {
  /** @constructor */
  function tempCtor() {
  }
  tempCtor.prototype = parentCtor.prototype;
  childCtor.prototype = new tempCtor;
  childCtor.prototype.constructor = childCtor;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Interface for representing a low-level gnubby device.
 */
'use strict';

/**
 * Low level gnubby 'driver'. One per physical USB device.
 * @interface
 */
function GnubbyDevice() {}

// Commands of the USB interface.
/** Echo data through local processor only */
GnubbyDevice.CMD_PING = 0x81;
/** Perform reset action and read ATR string */
GnubbyDevice.CMD_ATR = 0x82;
/** Send raw APDU */
GnubbyDevice.CMD_APDU = 0x83;
/** Send lock channel command */
GnubbyDevice.CMD_LOCK = 0x84;
/** Obtain system information record */
GnubbyDevice.CMD_SYSINFO = 0x85;
/** Obtain an unused channel ID */
GnubbyDevice.CMD_INIT = 0x86;
/** Control prompt flashing */
GnubbyDevice.CMD_PROMPT = 0x87;
/** Send device identification wink */
GnubbyDevice.CMD_WINK = 0x88;
/** USB test */
GnubbyDevice.CMD_USB_TEST = 0xb9;
/** Device Firmware Upgrade */
GnubbyDevice.CMD_DFU = 0xba;
/** Protocol resync command */
GnubbyDevice.CMD_SYNC = 0xbc;
/** Error response */
GnubbyDevice.CMD_ERROR = 0xbf;

// Low-level error codes.
/** No error */
GnubbyDevice.OK = 0;
/** Invalid command */
GnubbyDevice.INVALID_CMD = 1;
/** Invalid parameter */
GnubbyDevice.INVALID_PAR = 2;
/** Invalid message length */
GnubbyDevice.INVALID_LEN = 3;
/** Invalid message sequencing */
GnubbyDevice.INVALID_SEQ = 4;
/** Message has timed out */
GnubbyDevice.TIMEOUT = 5;
/** Channel is busy */
GnubbyDevice.BUSY = 6;
/** Access denied */
GnubbyDevice.ACCESS_DENIED = 7;
/** Device is gone */
GnubbyDevice.GONE = 8;
/** Verification error */
GnubbyDevice.VERIFY_ERROR = 9;
/** Command requires channel lock */
GnubbyDevice.LOCK_REQUIRED = 10;
/** Sync error */
GnubbyDevice.SYNC_FAIL = 11;
/** Other unspecified error */
GnubbyDevice.OTHER = 127;

// Remote helper errors.
/** Not a remote helper */
GnubbyDevice.NOTREMOTE = 263;
/** Could not reach remote endpoint */
GnubbyDevice.COULDNOTDIAL = 264;

// chrome.usb-related errors.
/** No device */
GnubbyDevice.NODEVICE = 512;
/** More than one device */
GnubbyDevice.TOOMANY = 513;
/** Permission denied */
GnubbyDevice.NOPERMISSION = 666;

/** Destroys this low-level device instance. */
GnubbyDevice.prototype.destroy = function() {};

/**
 * Register a client for this gnubby.
 * @param {*} who The client.
 */
GnubbyDevice.prototype.registerClient = function(who) {};

/**
 * De-register a client.
 * @param {*} who The client.
 * @return {number} The number of remaining listeners for this device, or -1
 *     if this had no clients to start with.
 */
GnubbyDevice.prototype.deregisterClient = function(who) {};

/**
 * @param {*} who The client.
 * @return {boolean} Whether this device has who as a client.
 */
GnubbyDevice.prototype.hasClient = function(who) {};

/**
 * Queue command to be sent.
 * If queue was empty, initiate the write.
 * @param {number} cid The client's channel ID.
 * @param {number} cmd The command to send.
 * @param {ArrayBuffer|Uint8Array} data Command data
 */
GnubbyDevice.prototype.queueCommand = function(cid, cmd, data) {};

/**
 * @typedef {{
 *   vendorId: number,
 *   productId: number
 * }}
 */
var UsbDeviceSpec;
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a "generic" RequestHelper that provides a default
 * response to unknown requests, and supports registering handlers for known
 * requests.
 */
'use strict';

/**
 * @typedef {function(HelperRequest): RequestHandler} */
var RequestHandlerFactory;

/**
 * Implements a "generic" RequestHelper that provides a default
 * response to unknown requests, and supports registering handlers for known
 * @constructor
 * @implements {RequestHelper}
 */
function GenericRequestHelper() {
  /** @private {Object<string, RequestHandlerFactory>} */
  this.handlerFactories_ = {};
}

/**
 * Gets a handler for a request.
 * @param {HelperRequest} request The request to handle.
 * @return {RequestHandler} A handler for the request.
 */
GenericRequestHelper.prototype.getHandler = function(request) {
  if (this.handlerFactories_.hasOwnProperty(request.type)) {
    return this.handlerFactories_[request.type](request);
  }
  return null;
};

/**
 * Registers a handler factory for a given type.
 * @param {string} type The request type.
 * @param {RequestHandlerFactory} factory A factory that can produce a handler
 *     for a request of a given type.
 */
GenericRequestHelper.prototype.registerHandlerFactory =
    function(type, factory) {
  this.handlerFactories_[type] = factory;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Class providing common dependencies for the extension's
 * top half.
 */
'use strict';

/**
 * @param {!AppIdCheckerFactory} appIdCheckerFactory An appId checker factory.
 * @param {!ApprovedOrigins} approvedOrigins An origin approval implementation.
 * @param {!CountdownFactory} countdownFactory A countdown timer factory.
 * @param {!OriginChecker} originChecker An origin checker.
 * @param {!RequestHelper} requestHelper A request helper.
 * @param {!SystemTimer} sysTimer A system timer implementation.
 * @param {!TextFetcher} textFetcher A text fetcher.
 * @constructor
 */
function FactoryRegistry(appIdCheckerFactory, approvedOrigins, countdownFactory,
    originChecker, requestHelper, sysTimer, textFetcher) {
  /** @private {!AppIdCheckerFactory} */
  this.appIdCheckerFactory_ = appIdCheckerFactory;
  /** @private {!ApprovedOrigins} */
  this.approvedOrigins_ = approvedOrigins;
  /** @private {!CountdownFactory} */
  this.countdownFactory_ = countdownFactory;
  /** @private {!OriginChecker} */
  this.originChecker_ = originChecker;
  /** @private {!RequestHelper} */
  this.requestHelper_ = requestHelper;
  /** @private {!SystemTimer} */
  this.sysTimer_ = sysTimer;
  /** @private {!TextFetcher} */
  this.textFetcher_ = textFetcher;
}

/** @return {!AppIdCheckerFactory} An appId checker factory. */
FactoryRegistry.prototype.getAppIdCheckerFactory = function() {
  return this.appIdCheckerFactory_;
};

/** @return {!ApprovedOrigins} An origin approval implementation. */
FactoryRegistry.prototype.getApprovedOrigins = function() {
  return this.approvedOrigins_;
};

/** @return {!CountdownFactory} A countdown factory. */
FactoryRegistry.prototype.getCountdownFactory = function() {
  return this.countdownFactory_;
};

/** @return {!OriginChecker} An origin checker. */
FactoryRegistry.prototype.getOriginChecker = function() {
  return this.originChecker_;
};

/** @return {!RequestHelper} A request helper. */
FactoryRegistry.prototype.getRequestHelper = function() {
  return this.requestHelper_;
};

/** @return {!SystemTimer} A system timer implementation. */
FactoryRegistry.prototype.getSystemTimer = function() {
  return this.sysTimer_;
};

/** @return {!TextFetcher} A text fetcher. */
FactoryRegistry.prototype.getTextFetcher = function() {
  return this.textFetcher_;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Errors reported by top-level request handlers.
 */
'use strict';

/**
 * Response status codes
 * @const
 * @enum {number}
 */
var ErrorCodes = {
  'OK': 0,
  'OTHER_ERROR': 1,
  'BAD_REQUEST': 2,
  'CONFIGURATION_UNSUPPORTED': 3,
  'DEVICE_INELIGIBLE': 4,
  'TIMEOUT': 5
};

/**
 * An error object for responses
 * @typedef {{
 *   errorCode: ErrorCodes,
 *   errorMessage: (?string|undefined)
 * }}
 */
var U2fError;
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Class providing common dependencies for the extension's
 * bottom half.
 */
'use strict';

/**
 * @param {!GnubbyFactory} gnubbyFactory A Gnubby factory.
 * @param {!CountdownFactory} countdownFactory A countdown timer factory.
 * @param {!IndividualAttestation} individualAttestation An individual
 *     attestation implementation.
 * @constructor
 */
function DeviceFactoryRegistry(gnubbyFactory, countdownFactory,
    individualAttestation) {
  /** @private {!GnubbyFactory} */
  this.gnubbyFactory_ = gnubbyFactory;
  /** @private {!CountdownFactory} */
  this.countdownFactory_ = countdownFactory;
  /** @private {!IndividualAttestation} */
  this.individualAttestation_ = individualAttestation;
}

/** @return {!GnubbyFactory} A Gnubby factory. */
DeviceFactoryRegistry.prototype.getGnubbyFactory = function() {
  return this.gnubbyFactory_;
};

/** @return {!CountdownFactory} A countdown factory. */
DeviceFactoryRegistry.prototype.getCountdownFactory = function() {
  return this.countdownFactory_;
};

/** @return {!IndividualAttestation} An individual attestation implementation.
 */
DeviceFactoryRegistry.prototype.getIndividualAttestation = function() {
  return this.individualAttestation_;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a check whether an origin is allowed to assert an
 * app id.
 *
 */
'use strict';

/**
 * Implements half of the app id policy: whether an origin is allowed to claim
 * an app id. For checking whether the app id also lists the origin,
 * @see AppIdChecker.
 * @interface
 */
function OriginChecker() {}

/**
 * Checks whether the origin is allowed to claim the app ids.
 * @param {string} origin The origin claiming the app id.
 * @param {!Array<string>} appIds The app ids being claimed.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
OriginChecker.prototype.canClaimAppIds = function(origin, appIds) {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides an interface to determine whether to request the
 * individual attestation certificate during enrollment.
 */
'use strict';

/**
 * Interface to determine whether to request the individual attestation
 * certificate during enrollment.
 * @interface
 */
function IndividualAttestation() {}

/**
 * @param {string} appIdHash The app id hash.
 * @return {boolean} Whether to request the individual attestation certificate
 *     for this app id.
 */
IndividualAttestation.prototype.requestIndividualAttestation =
    function(appIdHash) {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a Google corp implementation of IndividualAttestation.
 */
'use strict';

/**
 * Google corp implementation of IndividualAttestation that requests
 * individual certificates for corp accounts.
 * @constructor
 * @implements IndividualAttestation
 */
function GoogleCorpIndividualAttestation() {}

/**
 * @param {string} appIdHash The app id hash.
 * @return {boolean} Whether to request the individual attestation certificate
 *     for this app id.
 */
GoogleCorpIndividualAttestation.prototype.requestIndividualAttestation =
    function(appIdHash) {
  return appIdHash == GoogleCorpIndividualAttestation.GOOGLE_CORP_APP_ID_HASH;
};

/**
 * App ID used by Google employee accounts.
 * @const
 */
GoogleCorpIndividualAttestation.GOOGLE_CORP_APP_ID =
    'https://www.gstatic.com/securitykey/a/google.com/origins.json';

/**
 * Hash of the app ID used by Google employee accounts.
 * @const
 */
GoogleCorpIndividualAttestation.GOOGLE_CORP_APP_ID_HASH =
    B64_encode(sha256HashOfString(
        GoogleCorpIndividualAttestation.GOOGLE_CORP_APP_ID));
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides an interface to check whether the user has approved
 * an origin to use security keys.
 *
 */
'use strict';

/**
 * Allows the caller to check whether the user has approved the use of
 * security keys from an origin.
 * @interface
 */
function ApprovedOrigins() {}

/**
 * Checks whether the origin is approved to use security keys. (If not, an
 * approval prompt may be shown.)
 * @param {string} origin The origin to approve.
 * @param {number=} opt_tabId A tab id to display approval prompt in, if
 *     necessary.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
ApprovedOrigins.prototype.isApprovedOrigin = function(origin, opt_tabId) {};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a representation of a web request sender, and
 * utility functions for creating them.
 */
'use strict';

/**
 * @typedef {{
 *   origin: string,
 *   tlsChannelId: (string|undefined),
 *   tabId: (number|undefined)
 * }}
 */
var WebRequestSender;

/**
 * Creates an object representing the sender's origin, and, if available,
 * tab.
 * @param {MessageSender} messageSender The message sender.
 * @return {?WebRequestSender} The sender's origin and tab, or null if the
 *     sender is invalid.
 */
function createSenderFromMessageSender(messageSender) {
  var origin = getOriginFromUrl(/** @type {string} */ (messageSender.url));
  if (!origin) {
    return null;
  }
  var sender = {
    origin: origin
  };
  if (messageSender.tlsChannelId) {
    sender.tlsChannelId = messageSender.tlsChannelId;
  }
  if (messageSender.tab) {
    sender.tabId = messageSender.tab.id;
  }
  return sender;
}

/**
 * Checks whether the given tab could have sent a message from the given
 * origin.
 * @param {Tab} tab The tab to match
 * @param {string} origin The origin to check.
 * @return {Promise} A promise resolved with the tab id if it the tab could,
 *     have sent the request, and rejected if it can't.
 */
function tabMatchesOrigin(tab, origin) {
  // If the tab's origin matches, trust that the request came from this tab.
  if (getOriginFromUrl(tab.url) == origin) {
    return Promise.resolve(tab.id);
  }
  return Promise.reject(false);
}

/**
 * Attempts to ensure that the tabId of the sender is set, using chrome.tabs
 * when available.
 * @param {WebRequestSender} sender The request sender.
 * @return {Promise} A promise resolved once the tabId retrieval is done.
 *     The promise is rejected if the tabId is untrustworthy, e.g. if the
 *     user rapidly switched tabs.
 */
function getTabIdWhenPossible(sender) {
  if (sender.tabId) {
    // Already got it? Done.
    return Promise.resolve(true);
  } else if (!chrome.tabs) {
    // Can't get it? Done. (This happens to packaged apps, which can't access
    // chrome.tabs.)
    return Promise.resolve(true);
  } else {
    return new Promise(function(resolve, reject) {
      chrome.tabs.query({active: true, lastFocusedWindow: true},
          function(tabs) {
            if (!tabs.length) {
              // Safety check.
              reject(false);
              return;
            }
            var tab = tabs[0];
            tabMatchesOrigin(tab, sender.origin).then(function(tabId) {
              sender.tabId = tabId;
              resolve(true);
            }, function() {
              // Didn't match? Check if the debugger is open.
              if (tab.url.indexOf('chrome-devtools://') != 0) {
                reject(false);
                return;
              }
              // Debugger active: find first tab with the sender's origin.
              chrome.tabs.query({active: true}, function(tabs) {
                if (!tabs.length) {
                  // Safety check.
                  reject(false);
                  return;
                }
                var numRejected = 0;
                for (var i = 0; i < tabs.length; i++) {
                  tab = tabs[i];
                  tabMatchesOrigin(tab, sender.origin).then(function(tabId) {
                    sender.tabId = tabId;
                    resolve(true);
                  }, function() {
                    if (++numRejected >= tabs.length) {
                      // None matches: reject.
                      reject(false);
                    }
                  });
                }
              });
            });
          });
    });
  }
}

/**
 * Checks whether the given tab is in the foreground, i.e. is the active tab
 * of the focused window.
 * @param {number} tabId The tab id to check.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
function tabInForeground(tabId) {
  return new Promise(function(resolve, reject) {
      if (!chrome.tabs || !chrome.tabs.get) {
        reject();
        return;
      }
      if (!chrome.windows || !chrome.windows.get) {
        reject();
        return;
      }
      chrome.tabs.get(tabId, function(tab) {
            if (chrome.runtime.lastError) {
              resolve(false);
              return;
            }
            if (!tab.active) {
              resolve(false);
              return;
            }
            chrome.windows.get(tab.windowId, function(aWindow) {
                  resolve(aWindow && aWindow.focused);
                });
          });
  });
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides an implementation of the SystemTimer interface based
 * on window's timer methods.
 */
'use strict';

/**
 * Creates an implementation of the SystemTimer interface based on window's
 * timer methods.
 * @constructor
 * @implements {SystemTimer}
 */
function WindowTimer() {
}

/**
 * Sets a single-shot timer.
 * @param {function()} func Called back when the timer expires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
WindowTimer.prototype.setTimeout = function(func, timeoutMillis) {
  return window.setTimeout(func, timeoutMillis);
};

/**
 * Clears a previously set timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
WindowTimer.prototype.clearTimeout = function(timeoutId) {
  window.clearTimeout(timeoutId);
};

/**
 * Sets a repeating interval timer.
 * @param {function()} func Called back each time the timer fires.
 * @param {number} timeoutMillis How long until the timer fires, in
 *     milliseconds.
 * @return {number} A timeout ID, which can be used to cancel the timer.
 */
WindowTimer.prototype.setInterval = function(func, timeoutMillis) {
  return window.setInterval(func, timeoutMillis);
};

/**
 * Clears a previously set interval timer.
 * @param {number} timeoutId The ID of the timer to clear.
 */
WindowTimer.prototype.clearInterval = function(timeoutId) {
  window.clearInterval(timeoutId);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides a watchdog around a collection of callback functions.
 */
'use strict';

/**
 * Creates a watchdog around a collection of callback functions,
 * ensuring at least one of them is called before the timeout expires.
 * If a timeout function is provided, calls the timeout function upon timeout
 * expiration if none of the callback functions has been called.
 * @param {number} timeoutValueSeconds Timeout value, in seconds.
 * @param {function()=} opt_timeoutCb Callback function to call on timeout.
 * @constructor
 * @implements {Closeable}
 */
function WatchdogRequestHandler(timeoutValueSeconds, opt_timeoutCb) {
  /** @private {number} */
  this.timeoutValueSeconds_ = timeoutValueSeconds;
  /** @private {function()|undefined} */
  this.timeoutCb_ = opt_timeoutCb;
  /** @private {boolean} */
  this.calledBack_ = false;
  /** @private {Countdown} */
  this.timer_ = FACTORY_REGISTRY.getCountdownFactory().createTimer(
      this.timeoutValueSeconds_ * 1000, this.timeout_.bind(this));
  /** @private {Closeable|undefined} */
  this.closeable_ = undefined;
  /** @private {boolean} */
  this.closed_ = false;
}

/**
 * Wraps a callback function, such that the fact that the callback function
 * was or was not called gets tracked by this watchdog object.
 * @param {function(...?)} cb The callback function to wrap.
 * @return {function(...?)} A wrapped callback function.
 */
WatchdogRequestHandler.prototype.wrapCallback = function(cb) {
  return this.wrappedCallback_.bind(this, cb);
};

/** Closes this watchdog. */
WatchdogRequestHandler.prototype.close = function() {
  this.closed_ = true;
  this.timer_.clearTimeout();
  if (this.closeable_) {
    this.closeable_.close();
    this.closeable_ = undefined;
  }
};

/**
 * Sets this watchdog's closeable.
 * @param {!Closeable} closeable The closeable.
 */
WatchdogRequestHandler.prototype.setCloseable = function(closeable) {
  this.closeable_ = closeable;
};

/**
 * Called back when the watchdog expires.
 * @private
 */
WatchdogRequestHandler.prototype.timeout_ = function() {
  if (!this.calledBack_ && !this.closed_) {
    var logMsg = 'Not called back within ' + this.timeoutValueSeconds_ +
        ' second timeout';
    if (this.timeoutCb_) {
      logMsg += ', calling default callback';
      console.warn(UTIL_fmt(logMsg));
      this.timeoutCb_();
    } else {
      console.warn(UTIL_fmt(logMsg));
    }
  }
};

/**
 * Wrapped callback function.
 * @param {function(...?)} cb The callback function to call.
 * @param {...?} var_args The callback function's arguments.
 * @private
 */
WatchdogRequestHandler.prototype.wrappedCallback_ = function(cb, var_args) {
  if (!this.closed_) {
    this.calledBack_ = true;
    this.timer_.clearTimeout();
    var originalArgs = Array.prototype.slice.call(arguments, 1);
    cb.apply(null, originalArgs);
  }
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Logging related utility functions.
 */

/** Posts the log message to the log url.
 * @param {string} logMsg the log message to post.
 * @param {string=} opt_logMsgUrl the url to post log messages to.
 */
function logMessage(logMsg, opt_logMsgUrl) {
  console.log(UTIL_fmt('logMessage("' + logMsg + '")'));

  if (!opt_logMsgUrl) {
    return;
  }

  var audio = new Audio();
  audio.src = opt_logMsgUrl + logMsg;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Provides an implementation of approved origins that relies
 * on the chrome.cryptotokenPrivate.requestPermission API.
 * (and only) allows google.com to use security keys.
 *
 */
'use strict';

/**
 * Allows the caller to check whether the user has approved the use of
 * security keys from an origin.
 * @constructor
 * @implements {ApprovedOrigins}
 */
function CryptoTokenApprovedOrigin() {}

/**
 * Checks whether the origin is approved to use security keys. (If not, an
 * approval prompt may be shown.)
 * @param {string} origin The origin to approve.
 * @param {number=} opt_tabId A tab id to display approval prompt in.
 *     For this implementation, the tabId is always necessary, even though
 *     the type allows undefined.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
CryptoTokenApprovedOrigin.prototype.isApprovedOrigin =
    function(origin, opt_tabId) {
  return new Promise(function(resolve, reject) {
      if (opt_tabId === undefined) {
        resolve(false);
        return;
      }
      var tabId = /** @type {number} */ (opt_tabId);
      tabInForeground(tabId).then(function(result) {
        if (!result) {
          resolve(false);
          return;
        }
        if (!chrome.tabs || !chrome.tabs.get) {
          reject();
          return;
        }
        chrome.tabs.get(tabId, function(tab) {
          if (chrome.runtime.lastError) {
            resolve(false);
            return;
          }
          var tabOrigin = getOriginFromUrl(tab.url);
          resolve(tabOrigin == origin);
        });
      });
  });
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Implements a check whether an origin is allowed to assert an
 * app id based on whether they share the same effective TLD + 1.
 *
 */
'use strict';

/**
 * Implements half of the app id policy: whether an origin is allowed to claim
 * an app id. For checking whether the app id also lists the origin,
 * @see AppIdChecker.
 * @implements OriginChecker
 * @constructor
 */
function CryptoTokenOriginChecker() {
}

/**
 * Checks whether the origin is allowed to claim the app ids.
 * @param {string} origin The origin claiming the app id.
 * @param {!Array<string>} appIds The app ids being claimed.
 * @return {Promise<boolean>} A promise for the result of the check.
 */
CryptoTokenOriginChecker.prototype.canClaimAppIds = function(origin, appIds) {
  var appIdChecks = appIds.map(this.checkAppId_.bind(this, origin));
  return Promise.all(appIdChecks).then(function(results) {
    return results.every(function(result) {
      return result;
    });
  });
};

/**
 * Checks if a single appId can be asserted by the given origin.
 * @param {string} origin The origin.
 * @param {string} appId The appId to check
 * @return {Promise<boolean>} A promise for the result of the check
 * @private
 */
CryptoTokenOriginChecker.prototype.checkAppId_ =
    function(origin, appId) {
  return new Promise(function(resolve, reject) {
    if (!chrome.cryptotokenPrivate) {
      reject();
      return;
    }
    chrome.cryptotokenPrivate.canOriginAssertAppId(origin, appId, resolve);
  });
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview CryptoToken background page
 */

'use strict';

/** @const */
var BROWSER_SUPPORTS_TLS_CHANNEL_ID = true;

/** @const */
var HTTP_ORIGINS_ALLOWED = false;

/** @const */
var LOG_SAVER_EXTENSION_ID = 'fjajfjhkeibgmiggdfehjplbhmfkialk';

// Singleton tracking available devices.
var gnubbies = new Gnubbies();
HidGnubbyDevice.register(gnubbies);
UsbGnubbyDevice.register(gnubbies);

var FACTORY_REGISTRY = (function() {
  var windowTimer = new WindowTimer();
  var xhrTextFetcher = new XhrTextFetcher();
  return new FactoryRegistry(
      new XhrAppIdCheckerFactory(xhrTextFetcher),
      new CryptoTokenApprovedOrigin(),
      new CountdownTimerFactory(windowTimer),
      new CryptoTokenOriginChecker(),
      new UsbHelper(),
      windowTimer,
      xhrTextFetcher);
})();

var DEVICE_FACTORY_REGISTRY = new DeviceFactoryRegistry(
    new UsbGnubbyFactory(gnubbies),
    FACTORY_REGISTRY.getCountdownFactory(),
    new GoogleCorpIndividualAttestation());

/**
 * @param {*} request The received request
 * @return {boolean} Whether the request is a register/enroll request.
 */
function isRegisterRequest(request) {
  if (!request) {
    return false;
  }
  switch (request.type) {
    case MessageTypes.U2F_REGISTER_REQUEST:
      return true;

    default:
      return false;
  }
}

/**
 * Default response callback to deliver a response to a request.
 * @param {*} request The received request.
 * @param {function(*): void} sendResponse A callback that delivers a response.
 * @param {*} response The response to return.
 */
function defaultResponseCallback(request, sendResponse, response) {
  response['requestId'] = request['requestId'];
  try {
    sendResponse(response);
  } catch (e) {
    console.warn(UTIL_fmt('caught: ' + e.message));
  }
}

/**
 * Response callback that delivers a response to a request only when the
 * sender is a foreground tab.
 * @param {*} request The received request.
 * @param {!MessageSender} sender The message sender.
 * @param {function(*): void} sendResponse A callback that delivers a response.
 * @param {*} response The response to return.
 */
function sendResponseToActiveTabOnly(request, sender, sendResponse, response) {
  tabInForeground(sender.tab.id).then(function(result) {
    // If the tab is no longer in the foreground, drop the result: the user
    // is no longer interacting with the tab that originated the request.
    if (result) {
      defaultResponseCallback(request, sendResponse, response);
    }
  });
}

/**
 * Common handler for messages received from chrome.runtime.sendMessage and
 * chrome.runtime.connect + postMessage.
 * @param {*} request The received request
 * @param {!MessageSender} sender The message sender
 * @param {function(*): void} sendResponse A callback that delivers a response
 * @return {Closeable} A Closeable request handler.
 */
function messageHandler(request, sender, sendResponse) {
  var responseCallback;
  if (isRegisterRequest(request)) {
    responseCallback =
        sendResponseToActiveTabOnly.bind(null, request, sender, sendResponse);
  } else {
    responseCallback =
        defaultResponseCallback.bind(null, request, sendResponse);
  }
  var closeable = handleWebPageRequest(/** @type {Object} */(request),
      sender, responseCallback);
  return closeable;
}

/**
 * Listen to individual messages sent from (whitelisted) webpages via
 * chrome.runtime.sendMessage
 * @param {*} request The received request
 * @param {!MessageSender} sender The message sender
 * @param {function(*): void} sendResponse A callback that delivers a response
 * @return {boolean}
 */
function messageHandlerExternal(request, sender, sendResponse) {
  if (sender.id && sender.id === LOG_SAVER_EXTENSION_ID) {
    return handleLogSaverMessage(request);
  }

  messageHandler(request, sender, sendResponse);
  return true;  // Tell Chrome not to destroy sendResponse yet
}
chrome.runtime.onMessageExternal.addListener(messageHandlerExternal);

// Listen to direct connection events, and wire up a message handler on the port
chrome.runtime.onConnectExternal.addListener(function(port) {
  function sendResponse(response) {
    port.postMessage(response);
  }

  var closeable;
  port.onMessage.addListener(function(request) {
    var sender = /** @type {!MessageSender} */ (port.sender);
    closeable = messageHandler(request, sender, sendResponse);
  });
  port.onDisconnect.addListener(function() {
    if (closeable) {
      closeable.close();
    }
  });
});

/**
 * Handles messages from the log-saver app. Temporarily replaces UTIL_fmt with
 * a wrapper that also sends formatted messages to the app.
 * @param {*} request The message received from the app
 * @return {boolean} Used as chrome.runtime.onMessage handler return value
 */
function handleLogSaverMessage(request) {
  if (request === 'start') {
    if (originalUtilFmt_) {
      // We're already sending
      return false;
    }
    originalUtilFmt_ = UTIL_fmt;
    UTIL_fmt = function(s) {
      var line = originalUtilFmt_(s);
      chrome.runtime.sendMessage(LOG_SAVER_EXTENSION_ID, line);
      return line;
    };
  } else if (request === 'stop') {
    if (originalUtilFmt_) {
      UTIL_fmt = originalUtilFmt_;
      originalUtilFmt_ = null;
    }
  }
  return false;
}

/** @private */
var originalUtilFmt_ = null;
<!doctype html>
<html>
<head>
<meta charset="utf-8">
<link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
<script src="js/nacl.js"></script>
<script src="js/wrapper.js"></script>
<script src="js/init.js"></script>
</head>
<body>
</body>
</html>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

// Global holding our NaclBridge.
var whispernetNacl = null;

// Encoders and decoders for each client.
var whisperEncoders = {};
var whisperDecoders = {};

/**
 * Initialize the whispernet encoder and decoder.
 * Call this before any other functions.
 * @param {string} clientId A string identifying the requester.
 * @param {Object} audioParams Audio parameters for token encoding and decoding.
 */
function audioConfig(clientId, audioParams) {
  if (!whispernetNacl) {
    chrome.copresencePrivate.sendInitialized(false);
    return;
  }

  console.log('Configuring encoder and decoder for client ' + clientId);
  whisperEncoders[clientId] =
      new WhisperEncoder(audioParams.paramData, whispernetNacl, clientId);
  whisperDecoders[clientId] =
      new WhisperDecoder(audioParams.paramData, whispernetNacl, clientId);
}

/**
 * Sends a request to whispernet to encode a token.
 * @param {string} clientId A string identifying the requester.
 * @param {Object} params Encode token parameters object.
 */
function encodeTokenRequest(clientId, params) {
  if (whisperEncoders[clientId]) {
    whisperEncoders[clientId].encode(params);
  } else {
    console.error('encodeTokenRequest: Whisper not initialized for client ' +
        clientId);
  }
}

/**
 * Sends a request to whispernet to decode samples.
 * @param {string} clientId A string identifying the requester.
 * @param {Object} params Process samples parameters object.
 */
function decodeSamplesRequest(clientId, params) {
  if (whisperDecoders[clientId]) {
    whisperDecoders[clientId].processSamples(params);
  } else {
    console.error('decodeSamplesRequest: Whisper not initialized for client ' +
        clientId);
  }
}

/**
 * Initialize our listeners and signal that the extension is loaded.
 */
function onWhispernetLoaded() {
  console.log('init: Nacl ready!');

  // Setup all the listeners for the private API.
  chrome.copresencePrivate.onConfigAudio.addListener(audioConfig);
  chrome.copresencePrivate.onEncodeTokenRequest.addListener(encodeTokenRequest);
  chrome.copresencePrivate.onDecodeSamplesRequest.addListener(
      decodeSamplesRequest);

  // This first initialized is sent to indicate that the library is loaded.
  // Every other time, it will be sent only when Chrome wants to reinitialize
  // the encoder and decoder.
  chrome.copresencePrivate.sendInitialized(true);
}

/**
 * Initialize the whispernet Nacl bridge.
 */
function initWhispernet() {
  console.log('init: Starting Nacl bridge.');
  // TODO(rkc): Figure out how to embed the .nmf and the .pexe into component
  // resources without having to rename them to .js.
  whispernetNacl = new NaclBridge('whispernet_proxy.nmf.png',
                                  onWhispernetLoaded);
}

window.addEventListener('DOMContentLoaded', initWhispernet);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Constructor for the Nacl bridge to the whispernet wrapper.
 * @param {string} nmf The relative path to the nmf containing the location of
 * the whispernet Nacl wrapper.
 * @param {function()} readyCallback Callback to be called once we've loaded the
 * whispernet wrapper.
 */
function NaclBridge(nmf, readyCallback) {
  this.readyCallback_ = readyCallback;
  this.callbacks_ = [];
  this.isEnabled_ = false;
  this.naclId_ = this.loadNacl_(nmf);
}

/**
 * Method to send generic byte data to the whispernet wrapper.
 * @param {Object} data Raw data to send to the whispernet wrapper.
 */
NaclBridge.prototype.send = function(data) {
  if (this.isEnabled_) {
    this.embed_.postMessage(data);
  } else {
    console.error('Whisper Nacl Bridge not initialized!');
  }
};

/**
 * Method to add a listener to Nacl messages received by this bridge.
 * @param {function(Event)} messageCallback Callback to receive the messsage.
 */
NaclBridge.prototype.addListener = function(messageCallback) {
  this.callbacks_.push(messageCallback);
};

/**
 * Method that receives Nacl messages and forwards them to registered
 * callbacks.
 * @param {Event} e Event from the whispernet wrapper.
 * @private
 */
NaclBridge.prototype.onMessage_ = function(e) {
  if (this.isEnabled_) {
    this.callbacks_.forEach(function(callback) {
      callback(e);
    });
  }
};

/**
 * Injects the <embed> for this nacl manifest URL, generating a unique ID.
 * @param {string} manifestUrl Url to the nacl manifest to load.
 * @return {number} generated ID.
 * @private
 */
NaclBridge.prototype.loadNacl_ = function(manifestUrl) {
  var id = 'nacl-' + Math.floor(Math.random() * 10000);
  this.embed_ = document.createElement('embed');
  this.embed_.name = 'nacl_module';
  this.embed_.width = 1;
  this.embed_.height = 1;
  this.embed_.src = manifestUrl;
  this.embed_.id = id;
  this.embed_.type = 'application/x-pnacl';

  // Wait for the element to load and callback.
  this.embed_.addEventListener('load', this.onNaclReady_.bind(this));
  this.embed_.addEventListener('error', this.onNaclError_.bind(this));

  // Inject the embed string into the page.
  document.body.appendChild(this.embed_);

  // Listen for messages from the NaCl module.
  window.addEventListener('message', this.onMessage_.bind(this), true);
  return id;
};

/**
 * Called when the Whispernet wrapper is loaded.
 * @private
 */
NaclBridge.prototype.onNaclReady_ = function() {
  this.isEnabled_ = true;
  if (this.readyCallback_)
    this.readyCallback_();
};

/**
 * Callback that handles Nacl errors.
 * @param {string} msg Error string.
 * @private
 */
NaclBridge.prototype.onNaclError_ = function(msg) {
  // TODO(rkc): Handle error from NaCl better.
  console.error('NaCl error', msg);
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * Function to convert an array of bytes to a base64 string
 * TODO(rkc): Change this to use a Uint8array instead of a string.
 * @param {string} bytes String containing the bytes we want to convert.
 * @return {string} String containing the base64 representation.
 */
function bytesToBase64(bytes) {
  var bstr = '';
  for (var i = 0; i < bytes.length; ++i)
    bstr += String.fromCharCode(bytes[i]);
  return btoa(bstr).replace(/=/g, '');
}

/**
 * Function to convert a string to an array of bytes.
 * @param {string} str String to convert.
 * @return {Array} Array containing the string.
 */
function stringToArray(str) {
  var buffer = [];
  for (var i = 0; i < str.length; ++i)
    buffer[i] = str.charCodeAt(i);
  return buffer;
}

/**
 * Creates a whispernet encoder.
 * @constructor
 * @param {Object} params Audio parameters for the whispernet encoder.
 * @param {Object} whisperNacl The NaclBridge object, used to communicate with
 *     the whispernet wrapper.
 * @param {string} clientId A string identifying the requester.
 */
function WhisperEncoder(params, whisperNacl, clientId) {
  this.whisperNacl_ = whisperNacl;
  this.whisperNacl_.addListener(this.onNaclMessage_.bind(this));
  this.clientId_ = clientId;

  var msg = {
    type: 'initialize_encoder',
    client_id: clientId,
    params: params
  };

  this.whisperNacl_.send(msg);
}

/**
 * Method to encode a token.
 * @param {Object} params Encode token parameters object.
 */
WhisperEncoder.prototype.encode = function(params) {
  // Pad the token before decoding it.
  var token = params.token.token;
  while (token.length % 4 > 0)
    token += '=';

  var msg = {
    type: 'encode_token',
    client_id: this.clientId_,
    // Trying to send the token in binary form to Nacl doesn't work correctly.
    // We end up with the correct string + a bunch of extra characters. This is
    // true of returning a binary string too; hence we communicate back and
    // forth by converting the bytes into an array of integers.
    token: stringToArray(atob(token)),
    repetitions: params.repetitions,
    use_dtmf: params.token.audible,
    use_crc: params.tokenParams.crc,
    use_parity: params.tokenParams.parity
  };

  this.whisperNacl_.send(msg);
};

/**
 * Method to handle messages from the whispernet NaCl wrapper.
 * @param {Event} e Event from the whispernet wrapper.
 * @private
 */
WhisperEncoder.prototype.onNaclMessage_ = function(e) {
  var msg = e.data;
  if (msg.type == 'encode_token_response' && msg.client_id == this.clientId_) {
    chrome.copresencePrivate.sendSamples(this.clientId_,
        { token: bytesToBase64(msg.token), audible: msg.audible }, msg.samples);
  }
};

/**
 * Creates a whispernet decoder.
 * @constructor
 * @param {Object} params Audio parameters for the whispernet decoder.
 * @param {Object} whisperNacl The NaclBridge object, used to communicate with
 *     the whispernet wrapper.
 * @param {string} clientId A string identifying the requester.
 */
function WhisperDecoder(params, whisperNacl, clientId) {
  this.whisperNacl_ = whisperNacl;
  this.whisperNacl_.addListener(this.onNaclMessage_.bind(this));
  this.clientId_ = clientId;

  var msg = {
    type: 'initialize_decoder',
    client_id: clientId,
    params: params
  };
  this.whisperNacl_.send(msg);
}

/**
 * Method to request the decoder to process samples.
 * @param {Object} params Process samples parameters object.
 */
WhisperDecoder.prototype.processSamples = function(params) {
  var msg = {
    type: 'decode_tokens',
    client_id: this.clientId_,
    data: params.samples,

    decode_audible: params.decodeAudible,
    token_length_dtmf: params.audibleTokenParams.length,
    crc_dtmf: params.audibleTokenParams.crc,
    parity_dtmf: params.audibleTokenParams.parity,

    decode_inaudible: params.decodeInaudible,
    token_length_dsss: params.inaudibleTokenParams.length,
    crc_dsss: params.inaudibleTokenParams.crc,
    parity_dsss: params.inaudibleTokenParams.parity,
  };

  this.whisperNacl_.send(msg);
};

/**
 * Method to handle messages from the whispernet NaCl wrapper.
 * @param {Event} e Event from the whispernet wrapper.
 * @private
 */
WhisperDecoder.prototype.onNaclMessage_ = function(e) {
  var msg = e.data;
  if (msg.type == 'decode_tokens_response' && msg.client_id == this.clientId_) {
    this.handleCandidates_(msg.tokens, msg.audible);
  }
};

/**
 * Method to receive tokens from the decoder and process and forward them to the
 * token callback registered with us.
 * @param {!Array.string} candidates Array of token candidates.
 * @param {boolean} audible Whether the received candidates are from the audible
 *     decoder or not.
 * @private
 */
WhisperDecoder.prototype.handleCandidates_ = function(candidates, audible) {
  if (!candidates || candidates.length == 0)
    return;

  var returnCandidates = [];
  for (var i = 0; i < candidates.length; ++i) {
    returnCandidates[i] = { token: bytesToBase64(candidates[i]),
                            audible: audible };
  }
  chrome.copresencePrivate.sendFound(this.clientId_, returnCandidates);
};
{
  "program": {
    "portable": {
      "pnacl-translate": { "url": "whispernet_proxy_pnacl.pexe.png?v00008" }
    }
  }
}
PEXE