ionManager.prototype.updateListeners.call(this);
      if (this.enabled())
        this.startSession();
    }
  };

  return {
    AlwaysOnManager: AlwaysOnManager
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

/**
 * @fileoverview This is the audio client content script injected into eligible
 *  Google.com and New tab pages for interaction between the Webpage and the
 *  Hotword extension.
 */

(function() {
  /**
   * @constructor
   */
  var AudioClient = function() {
    /** @private {Element} */
    this.speechOverlay_ = null;

    /** @private {number} */
    this.checkSpeechUiRetries_ = 0;

    /**
     * Port used to communicate with the audio manager.
     * @private {?Port}
     */
    this.port_ = null;

    /**
     * Keeps track of the effects of different commands. Used to verify that
     * proper UIs are shown to the user.
     * @private {Object<AudioClient.CommandToPage, Object>}
     */
    this.uiStatus_ = null;

    /**
     * Bound function used to handle commands sent from the page to this script.
     * @private {Function}
     */
    this.handleCommandFromPageFunc_ = null;
  };

  /**
   * Messages sent to the page to control the voice search UI.
   * @enum {string}
   */
  AudioClient.CommandToPage = {
    HOTWORD_VOICE_TRIGGER: 'vt',
    HOTWORD_STARTED: 'hs',
    HOTWORD_ENDED: 'hd',
    HOTWORD_TIMEOUT: 'ht',
    HOTWORD_ERROR: 'he'
  };

  /**
   * Messages received from the page used to indicate voice search state.
   * @enum {string}
   */
  AudioClient.CommandFromPage = {
    SPEECH_START: 'ss',
    SPEECH_END: 'se',
    SPEECH_RESET: 'sr',
    SHOWING_HOTWORD_START: 'shs',
    SHOWING_ERROR_MESSAGE: 'sem',
    SHOWING_TIMEOUT_MESSAGE: 'stm',
    CLICKED_RESUME: 'hcc',
    CLICKED_RESTART: 'hcr',
    CLICKED_DEBUG: 'hcd'
  };

  /**
   * Errors that are sent to the hotword extension.
   * @enum {string}
   */
  AudioClient.Error = {
    NO_SPEECH_UI: 'ac1',
    NO_HOTWORD_STARTED_UI: 'ac2',
    NO_HOTWORD_TIMEOUT_UI: 'ac3',
    NO_HOTWORD_ERROR_UI: 'ac4'
  };

  /**
   * @const {string}
   * @private
   */
  AudioClient.HOTWORD_EXTENSION_ID_ = 'nbpagnldghgfoolbancepceaanlmhfmd';

  /**
   * Number of times to retry checking a transient error.
   * @const {number}
   * @private
   */
  AudioClient.MAX_RETRIES = 3;

  /**
   * Delay to wait in milliseconds before rechecking for any transient errors.
   * @const {number}
   * @private
   */
  AudioClient.RETRY_TIME_MS_ = 2000;

  /**
   * DOM ID for the speech UI overlay.
   * @const {string}
   * @private
   */
  AudioClient.SPEECH_UI_OVERLAY_ID_ = 'spch';

  /**
   * @const {string}
   * @private
   */
  AudioClient.HELP_CENTER_URL_ =
      'https://support.google.com/chrome/?p=ui_hotword_search';

  /**
   * @const {string}
   * @private
   */
  AudioClient.CLIENT_PORT_NAME_ = 'chwcpn';

  /**
   * Existence of the Audio Client.
   * @const {string}
   * @private
   */
  AudioClient.EXISTS_ = 'chwace';

  /**
   * Checks for the presence of speech overlay UI DOM elements.
   * @private
   */
  AudioClient.prototype.checkSpeechOverlayUi_ = function() {
    if (!this.speechOverlay_) {
      window.setTimeout(this.delayedCheckSpeechOverlayUi_.bind(this),
                        AudioClient.RETRY_TIME_MS_);
    } else {
      this.checkSpeechUiRetries_ = 0;
    }
  };

  /**
   * Function called to check for the speech UI overlay after some time has
   * passed since an initial check. Will either retry triggering the speech
   * or sends an error message depending on the number of retries.
   * @private
   */
  AudioClient.prototype.delayedCheckSpeechOverlayUi_ = function() {
    this.speechOverlay_ = document.getElementById(
        AudioClient.SPEECH_UI_OVERLAY_ID_);
    if (!this.speechOverlay_) {
      if (this.checkSpeechUiRetries_++ < AudioClient.MAX_RETRIES) {
        this.sendCommandToPage_(AudioClient.CommandToPage.VOICE_TRIGGER);
        this.checkSpeechOverlayUi_();
      } else {
        this.sendCommandToExtension_(AudioClient.Error.NO_SPEECH_UI);
      }
    } else {
      this.checkSpeechUiRetries_ = 0;
    }
  };

  /**
   * Checks that the triggered UI is actually displayed.
   * @param {AudioClient.CommandToPage} command Command that was send.
   * @private
   */
  AudioClient.prototype.checkUi_ = function(command) {
    this.uiStatus_[command].timeoutId =
        window.setTimeout(this.failedCheckUi_.bind(this, command),
                          AudioClient.RETRY_TIME_MS_);
  };

  /**
   * Function called when the UI verification is not called in time. Will either
   * retry the command or sends an error message, depending on the number of
   * retries for the command.
   * @param {AudioClient.CommandToPage} command Command that was sent.
   * @private
   */
  AudioClient.prototype.failedCheckUi_ = function(command) {
    if (this.uiStatus_[command].tries++ < AudioClient.MAX_RETRIES) {
      this.sendCommandToPage_(command);
      this.checkUi_(command);
    } else {
      this.sendCommandToExtension_(this.uiStatus_[command].error);
    }
  };

  /**
   * Confirm that an UI element has been shown.
   * @param {AudioClient.CommandToPage} command UI to confirm.
   * @private
   */
  AudioClient.prototype.verifyUi_ = function(command) {
    if (this.uiStatus_[command].timeoutId) {
      window.clearTimeout(this.uiStatus_[command].timeoutId);
      this.uiStatus_[command].timeoutId = null;
      this.uiStatus_[command].tries = 0;
    }
  };

  /**
   * Sends a command to the audio manager.
   * @param {string} commandStr command to send to plugin.
   * @private
   */
  AudioClient.prototype.sendCommandToExtension_ = function(commandStr) {
    if (this.port_)
      this.port_.postMessage({'cmd': commandStr});
  };

  /**
   * Handles a message from the audio manager.
   * @param {{cmd: string}} commandObj Command from the audio manager.
   * @private
   */
  AudioClient.prototype.handleCommandFromExtension_ = function(commandObj) {
    var command = commandObj['cmd'];
    if (command) {
      switch (command) {
        case AudioClient.CommandToPage.HOTWORD_VOICE_TRIGGER:
          this.sendCommandToPage_(command);
          this.checkSpeechOverlayUi_();
          break;
        case AudioClient.CommandToPage.HOTWORD_STARTED:
          this.sendCommandToPage_(command);
          this.checkUi_(command);
          break;
        case AudioClient.CommandToPage.HOTWORD_ENDED:
          this.sendCommandToPage_(command);
          break;
        case AudioClient.CommandToPage.HOTWORD_TIMEOUT:
          this.sendCommandToPage_(command);
          this.checkUi_(command);
          break;
        case AudioClient.CommandToPage.HOTWORD_ERROR:
          this.sendCommandToPage_(command);
          this.checkUi_(command);
          break;
      }
    }
  };

  /**
   * @param {AudioClient.CommandToPage} commandStr Command to send.
   * @private
   */
  AudioClient.prototype.sendCommandToPage_ = function(commandStr) {
    window.postMessage({'type': commandStr}, '*');
  };

  /**
   * Handles a message from the html window.
   * @param {!MessageEvent} messageEvent Message event from the window.
   * @private
   */
  AudioClient.prototype.handleCommandFromPage_ = function(messageEvent) {
    if (messageEvent.source == window && messageEvent.data.type) {
      var command = messageEvent.data.type;
      switch (command) {
        case AudioClient.CommandFromPage.SPEECH_START:
          this.speechActive_ = true;
          this.sendCommandToExtension_(command);
          break;
        case AudioClient.CommandFromPage.SPEECH_END:
          this.speechActive_ = false;
          this.sendCommandToExtension_(command);
          break;
        case AudioClient.CommandFromPage.SPEECH_RESET:
          this.speechActive_ = false;
          this.sendCommandToExtension_(command);
          break;
        case 'SPEECH_RESET':  // Legacy, for embedded NTP.
          this.speechActive_ = false;
          this.sendCommandToExtension_(AudioClient.CommandFromPage.SPEECH_END);
          break;
        case AudioClient.CommandFromPage.CLICKED_RESUME:
          this.sendCommandToExtension_(command);
          break;
        case AudioClient.CommandFromPage.CLICKED_RESTART:
          this.sendCommandToExtension_(command);
          break;
        case AudioClient.CommandFromPage.CLICKED_DEBUG:
          window.open(AudioClient.HELP_CENTER_URL_, '_blank');
          break;
        case AudioClient.CommandFromPage.SHOWING_HOTWORD_START:
          this.verifyUi_(AudioClient.CommandToPage.HOTWORD_STARTED);
          break;
        case AudioClient.CommandFromPage.SHOWING_ERROR_MESSAGE:
          this.verifyUi_(AudioClient.CommandToPage.HOTWORD_ERROR);
          break;
        case AudioClient.CommandFromPage.SHOWING_TIMEOUT_MESSAGE:
          this.verifyUi_(AudioClient.CommandToPage.HOTWORD_TIMEOUT);
          break;
      }
    }
  };

  /**
   * Initialize the content script.
   */
  AudioClient.prototype.initialize = function() {
    if (AudioClient.EXISTS_ in window)
      return;
    window[AudioClient.EXISTS_] = true;

    // UI verification object.
    this.uiStatus_ = {};
    this.uiStatus_[AudioClient.CommandToPage.HOTWORD_STARTED] = {
      timeoutId: null,
      tries: 0,
      error: AudioClient.Error.NO_HOTWORD_STARTED_UI
    };
    this.uiStatus_[AudioClient.CommandToPage.HOTWORD_TIMEOUT] = {
      timeoutId: null,
      tries: 0,
      error: AudioClient.Error.NO_HOTWORD_TIMEOUT_UI
    };
    this.uiStatus_[AudioClient.CommandToPage.HOTWORD_ERROR] = {
      timeoutId: null,
      tries: 0,
      error: AudioClient.Error.NO_HOTWORD_ERROR_UI
    };

    this.handleCommandFromPageFunc_ = this.handleCommandFromPage_.bind(this);
    window.addEventListener('message', this.handleCommandFromPageFunc_, false);
    this.initPort_();
  };

  /**
   * Initialize the communications port with the audio manager. This
   * function will be also be called again if the audio-manager
   * disconnects for some reason (such as the extension
   * background.html page being reloaded).
   * @private
   */
  AudioClient.prototype.initPort_ = function() {
    this.port_ = chrome.runtime.connect(
        AudioClient.HOTWORD_EXTENSION_ID_,
        {'name': AudioClient.CLIENT_PORT_NAME_});
    // Note that this listen may have to be destroyed manually if AudioClient
    // is ever destroyed on this tab.
    this.port_.onDisconnect.addListener(
        (function(e) {
          if (this.handleCommandFromPageFunc_) {
            window.removeEventListener(
                'message', this.handleCommandFromPageFunc_, false);
          }
          delete window[AudioClient.EXISTS_];
        }).bind(this));

    // See note above.
    this.port_.onMessage.addListener(
        this.handleCommandFromExtension_.bind(this));

    if (this.speechActive_) {
      this.sendCommandToExtension_(AudioClient.CommandFromPage.SPEECH_START);
    } else {
      // It's possible for this script to be injected into the page after it has
      // completed loaded (i.e. when prerendering). In this case, this script
      // won't receive a SPEECH_RESET from the page to forward onto the
      // extension. To make up for this, always send a SPEECH_RESET. This means
      // in most cases, the extension will receive SPEECH_RESET twice, one from
      // this sendCommandToExtension_ and the one forwarded from the page. But
      // that's OK and the extension can handle it.
      this.sendCommandToExtension_(AudioClient.CommandFromPage.SPEECH_RESET);
    }
  };

  // Initializes as soon as the code is ready, do not wait for the page.
  new AudioClient().initialize();
})();
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Base class for managing hotwording sessions.
   * @param {!hotword.StateManager} stateManager Manager of global hotwording
   *     state.
   * @param {!hotword.constants.SessionSource} sessionSource Source of the
   *     hotword session request.
   * @constructor
   */
  function BaseSessionManager(stateManager, sessionSource) {
    /**
     * Manager of global hotwording state.
     * @protected {!hotword.StateManager}
     */
    this.stateManager = stateManager;

    /**
     * Source of the hotword session request.
     * @private {!hotword.constants.SessionSource}
     */
    this.sessionSource_ = sessionSource;

    /**
     * Chrome event listeners. Saved so that they can be de-registered when
     * hotwording is disabled.
     * @private
     */
    this.sessionRequestedListener_ = this.handleSessionRequested_.bind(this);
    this.sessionStoppedListener_ = this.handleSessionStopped_.bind(this);

    // Need to setup listeners on startup, otherwise events that caused the
    // event page to start up, will be lost.
    this.setupListeners_();

    this.stateManager.onStatusChanged.addListener(function() {
      hotword.debug('onStatusChanged');
      this.updateListeners();
    }.bind(this));
  }

  BaseSessionManager.prototype = {
    /**
     * Return whether or not this session type is enabled.
     * @protected
     * @return {boolean}
     */
    enabled: assertNotReached,

    /**
     * Called when the hotwording session is stopped.
     * @protected
     */
    onSessionStop: function() {
    },

    /**
     * Starts a launcher hotwording session.
     * @param {hotword.constants.TrainingMode=} opt_mode The mode to start the
     *     recognizer in.
     */
    startSession: function(opt_mode) {
      this.stateManager.startSession(
          this.sessionSource_,
          function() {
            chrome.hotwordPrivate.setHotwordSessionState(true, function() {});
          },
          this.handleHotwordTrigger.bind(this),
          opt_mode);
    },

    /**
     * Stops a launcher hotwording session.
     * @private
     */
    stopSession_: function() {
      this.stateManager.stopSession(this.sessionSource_);
      this.onSessionStop();
    },

    /**
     * Handles a hotword triggered event.
     * @param {?Object} log Audio log data, if audio logging is enabled.
     * @protected
     */
    handleHotwordTrigger: function(log) {
      hotword.debug('Hotword triggered: ' + this.sessionSource_, log);
      chrome.hotwordPrivate.notifyHotwordRecognition('search',
                                                     log,
                                                     function() {});
    },

    /**
     * Handles a hotwordPrivate.onHotwordSessionRequested event.
     * @private
     */
    handleSessionRequested_: function() {
      hotword.debug('handleSessionRequested_: ' + this.sessionSource_);
      this.startSession();
    },

    /**
     * Handles a hotwordPrivate.onHotwordSessionStopped event.
     * @private
     */
    handleSessionStopped_: function() {
      hotword.debug('handleSessionStopped_: ' + this.sessionSource_);
      this.stopSession_();
    },

    /**
     * Set up event listeners.
     * @private
     */
    setupListeners_: function() {
      if (chrome.hotwordPrivate.onHotwordSessionRequested.hasListener(
              this.sessionRequestedListener_)) {
        return;
      }

      chrome.hotwordPrivate.onHotwordSessionRequested.addListener(
          this.sessionRequestedListener_);
      chrome.hotwordPrivate.onHotwordSessionStopped.addListener(
          this.sessionStoppedListener_);
    },

    /**
     * Remove event listeners.
     * @private
     */
    removeListeners_: function() {
      chrome.hotwordPrivate.onHotwordSessionRequested.removeListener(
          this.sessionRequestedListener_);
      chrome.hotwordPrivate.onHotwordSessionStopped.removeListener(
          this.sessionStoppedListener_);
    },

    /**
     * Update event listeners based on the current hotwording state.
     * @protected
     */
    updateListeners: function() {
      if (this.enabled()) {
        this.setupListeners_();
      } else {
        this.removeListeners_();
        this.stopSession_();
      }
    }
  };

  return {
    BaseSessionManager: BaseSessionManager
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword.constants', function() {
'use strict';

/**
 * Number of seconds of audio to record when logging is enabled.
 * @const {number}
 */
var AUDIO_LOG_SECONDS = 2;

/**
 * Timeout in seconds, for detecting false positives with a hotword stream.
 * @const {number}
 */
var HOTWORD_STREAM_TIMEOUT_SECONDS = 2;

/**
 * Hotword data shared module extension's ID.
 * @const {string}
 */
var SHARED_MODULE_ID = 'lccekmodgklaepjeofjdjpbminllajkg';

/**
 * Path to shared module data.
 * @const {string}
 */
var SHARED_MODULE_ROOT = '_modules/' + SHARED_MODULE_ID;

/**
 * Name used by the content scripts to create communications Ports.
 * @const {string}
 */
var CLIENT_PORT_NAME = 'chwcpn';

/**
 * The field name to specify the command among pages.
 * @const {string}
 */
var COMMAND_FIELD_NAME = 'cmd';

/**
 * The speaker model file name.
 * @const {string}
 */
var SPEAKER_MODEL_FILE_NAME = 'speaker_model.data';

/**
 * The training utterance file name prefix.
 * @const {string}
 */
var UTTERANCE_FILE_PREFIX = 'utterance-';

/**
 * The training utterance file extension.
 * @const {string}
 */
var UTTERANCE_FILE_EXTENSION = '.raw';

/**
 * The number of training utterances required to train the speaker model.
 * @const {number}
 */
var NUM_TRAINING_UTTERANCES = 3;

/**
 * The size of the file system requested for reading the speaker model and
 * utterances. This number should always be larger than the combined file size,
 * currently 576338 bytes as of February 2015.
 * @const {number}
 */
var FILE_SYSTEM_SIZE_BYTES = 1048576;

/**
 * Time to wait for expected messages, in milliseconds.
 * @enum {number}
 */
var TimeoutMs = {
  SHORT: 200,
  NORMAL: 500,
  LONG: 2000
};

/**
 * The URL of the files used by the plugin.
 * @enum {string}
 */
var File = {
  RECOGNIZER_CONFIG: 'hotword.data',
};

/**
 * Errors emitted by the NaClManager.
 * @enum {string}
 */
var Error = {
  NACL_CRASH: 'nacl_crash',
  TIMEOUT: 'timeout',
};

/**
 * Event types supported by NaClManager.
 * @enum {string}
 */
var Event = {
  READY: 'ready',
  TRIGGER: 'trigger',
  SPEAKER_MODEL_SAVED: 'speaker model saved',
  ERROR: 'error',
  TIMEOUT: 'timeout',
};

/**
 * Messages for communicating with the NaCl recognizer plugin. These must match
 * constants in <google3>/hotword_plugin.c
 * @enum {string}
 */
var NaClPlugin = {
  RESTART: 'r',
  SAMPLE_RATE_PREFIX: 'h',
  MODEL_PREFIX: 'm',
  STOP: 's',
  LOG: 'l',
  DSP: 'd',
  BEGIN_SPEAKER_MODEL: 'b',
  ADAPT_SPEAKER_MODEL: 'a',
  FINISH_SPEAKER_MODEL: 'f',
  SPEAKER_MODEL_SAVED: 'sm_saved',
  REQUEST_MODEL: 'model',
  MODEL_LOADED: 'model_loaded',
  READY_FOR_AUDIO: 'audio',
  STOPPED: 'stopped',
  HOTWORD_DETECTED: 'hotword',
  MS_CONFIGURED: 'ms_configured',
  TIMEOUT: 'timeout'
};

/**
 * Messages sent from the injected scripts to the Google page.
 * @enum {string}
 */
var CommandToPage = {
  HOTWORD_VOICE_TRIGGER: 'vt',
  HOTWORD_STARTED: 'hs',
  HOTWORD_ENDED: 'hd',
  HOTWORD_TIMEOUT: 'ht',
  HOTWORD_ERROR: 'he'
};

/**
 * Messages sent from the Google page to the extension or to the
 * injected script and then passed to the extension.
 * @enum {string}
 */
var CommandFromPage = {
  SPEECH_START: 'ss',
  SPEECH_END: 'se',
  SPEECH_RESET: 'sr',
  SHOWING_HOTWORD_START: 'shs',
  SHOWING_ERROR_MESSAGE: 'sem',
  SHOWING_TIMEOUT_MESSAGE: 'stm',
  CLICKED_RESUME: 'hcc',
  CLICKED_RESTART: 'hcr',
  CLICKED_DEBUG: 'hcd',
  WAKE_UP_HELPER: 'wuh',
  // Command specifically for the opt-in promo below this line.
  // User has explicitly clicked 'no'.
  CLICKED_NO_OPTIN: 'hcno',
  // User has opted in.
  CLICKED_OPTIN: 'hco',
  // User clicked on the microphone.
  PAGE_WAKEUP: 'wu'
};

/**
 * Source of a hotwording session request.
 * @enum {string}
 */
var SessionSource = {
  LAUNCHER: 'launcher',
  NTP: 'ntp',
  ALWAYS: 'always',
  TRAINING: 'training'
};

/**
 * The mode to start the hotword recognizer in.
 * @enum {string}
 */
var RecognizerStartMode = {
  NORMAL: 'normal',
  NEW_MODEL: 'new model',
  ADAPT_MODEL: 'adapt model'
};

/**
 * MediaStream open success/errors to be reported via UMA.
 * DO NOT remove or renumber values in this enum. Only add new ones.
 * @enum {number}
 */
var UmaMediaStreamOpenResult = {
  SUCCESS: 0,
  UNKNOWN: 1,
  NOT_SUPPORTED: 2,
  PERMISSION_DENIED: 3,
  CONSTRAINT_NOT_SATISFIED: 4,
  OVERCONSTRAINED: 5,
  NOT_FOUND: 6,
  ABORT: 7,
  SOURCE_UNAVAILABLE: 8,
  PERMISSION_DISMISSED: 9,
  INVALID_STATE: 10,
  DEVICES_NOT_FOUND: 11,
  INVALID_SECURITY_ORIGIN: 12,
  MAX: 12
};

/**
 * UMA metrics.
 * DO NOT change these enum values.
 * @enum {string}
 */
var UmaMetrics = {
  TRIGGER: 'Hotword.HotwordTrigger',
  MEDIA_STREAM_RESULT: 'Hotword.HotwordMediaStreamResult',
  NACL_PLUGIN_LOAD_RESULT: 'Hotword.HotwordNaClPluginLoadResult',
  NACL_MESSAGE_TIMEOUT: 'Hotword.HotwordNaClMessageTimeout',
  TRIGGER_SOURCE: 'Hotword.HotwordTriggerSource'
};

/**
 * Message waited for by NaCl plugin, to be reported via UMA.
 * DO NOT remove or renumber values in this enum. Only add new ones.
 * @enum {number}
 */
var UmaNaClMessageTimeout = {
  REQUEST_MODEL: 0,
  MODEL_LOADED: 1,
  READY_FOR_AUDIO: 2,
  STOPPED: 3,
  HOTWORD_DETECTED: 4,
  MS_CONFIGURED: 5,
  MAX: 5
};

/**
 * NaCl plugin load success/errors to be reported via UMA.
 * DO NOT remove or renumber values in this enum. Only add new ones.
 * @enum {number}
 */
var UmaNaClPluginLoadResult = {
  SUCCESS: 0,
  UNKNOWN: 1,
  CRASH: 2,
  NO_MODULE_FOUND: 3,
  MAX: 3
};

/**
 * Source of hotword triggering, to be reported via UMA.
 * DO NOT remove or renumber values in this enum. Only add new ones.
 * @enum {number}
 */
var UmaTriggerSource = {
  LAUNCHER: 0,
  NTP_GOOGLE_COM: 1,
  ALWAYS_ON: 2,
  TRAINING: 3,
  MAX: 3
};

/**
 * The browser UI language.
 * @const {string}
 */
var UI_LANGUAGE = (chrome.i18n && chrome.i18n.getUILanguage) ?
      chrome.i18n.getUILanguage() : '';

return {
  AUDIO_LOG_SECONDS: AUDIO_LOG_SECONDS,
  CLIENT_PORT_NAME: CLIENT_PORT_NAME,
  COMMAND_FIELD_NAME: COMMAND_FIELD_NAME,
  FILE_SYSTEM_SIZE_BYTES: FILE_SYSTEM_SIZE_BYTES,
  HOTWORD_STREAM_TIMEOUT_SECONDS: HOTWORD_STREAM_TIMEOUT_SECONDS,
  NUM_TRAINING_UTTERANCES: NUM_TRAINING_UTTERANCES,
  SHARED_MODULE_ID: SHARED_MODULE_ID,
  SHARED_MODULE_ROOT: SHARED_MODULE_ROOT,
  SPEAKER_MODEL_FILE_NAME: SPEAKER_MODEL_FILE_NAME,
  UI_LANGUAGE: UI_LANGUAGE,
  UTTERANCE_FILE_EXTENSION: UTTERANCE_FILE_EXTENSION,
  UTTERANCE_FILE_PREFIX: UTTERANCE_FILE_PREFIX,
  CommandToPage: CommandToPage,
  CommandFromPage: CommandFromPage,
  Error: Error,
  Event: Event,
  File: File,
  NaClPlugin: NaClPlugin,
  RecognizerStartMode: RecognizerStartMode,
  SessionSource: SessionSource,
  TimeoutMs: TimeoutMs,
  UmaMediaStreamOpenResult: UmaMediaStreamOpenResult,
  UmaMetrics: UmaMetrics,
  UmaNaClMessageTimeout: UmaNaClMessageTimeout,
  UmaNaClPluginLoadResult: UmaNaClPluginLoadResult,
  UmaTriggerSource: UmaTriggerSource
};

});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Class used to keep this extension alive. When started, this calls an
   * extension API on a regular basis which resets the event page keep-alive
   * timer.
   * @constructor
   */
  function KeepAlive() {
    this.timeoutId_ = null;
  }

  KeepAlive.prototype = {
    /**
     * Start the keep alive process. Safe to call multiple times.
     */
    start: function() {
      if (this.timeoutId_ == null)
        this.timeoutId_ = setTimeout(this.handleTimeout_.bind(this), 1000);
    },

    /**
     * Stops the keep alive process. Safe to call multiple times.
     */
    stop: function() {
      if (this.timeoutId_ != null) {
        clearTimeout(this.timeoutId_);
        this.timeoutId_ = null;
      }
    },

    /**
     * Handle the timer timeout. Calls an extension API and schedules the next
     * timeout.
     * @private
     */
    handleTimeout_: function() {
      // Dummy extensions API call used to keep this event page alive by
      // resetting the shutdown timer.
      chrome.runtime.getPlatformInfo(function(info) {});

      this.timeoutId_ = setTimeout(this.handleTimeout_.bind(this), 1000);
    }
  };

  return {
    KeepAlive: KeepAlive
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Class used to manage the interaction between hotwording and the launcher
   * (app list).
   * @param {!hotword.StateManager} stateManager
   * @constructor
   * @extends {hotword.BaseSessionManager}
   */
  function LauncherManager(stateManager) {
    hotword.BaseSessionManager.call(this,
                                    stateManager,
                                    hotword.constants.SessionSource.LAUNCHER);
  }

  LauncherManager.prototype = {
    __proto__: hotword.BaseSessionManager.prototype,

    /** @override */
    enabled: function() {
      return this.stateManager.isSometimesOnEnabled();
    },

    /** @override */
    onSessionStop: function() {
      chrome.hotwordPrivate.setHotwordSessionState(false, function() {});
    }
  };

  return {
    LauncherManager: LauncherManager
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Wrapper around console.log allowing debug log message to be enabled during
   * development.
   * @param {...*} varArgs
   */
  function debug(varArgs) {
    if (hotword.DEBUG || window.localStorage['hotword.DEBUG'])
      console.log.apply(console, arguments);
  }

  return {
    DEBUG: false,
    debug: debug
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {
  'use strict';

  /**
   * @fileoverview This extension provides hotword triggering capabilites to
   * Chrome.
   *
   * This extension contains all the JavaScript for loading and managing the
   * hotword detector. The hotword detector and language model data will be
   * provided by a shared module loaded from the web store.
   *
   * IMPORTANT! Whenever adding new events, the extension version number MUST be
   * incremented.
   */

  // Hotwording state.
  var stateManager = new hotword.StateManager();
  var pageAudioManager = new hotword.PageAudioManager(stateManager);
  var alwaysOnManager = new hotword.AlwaysOnManager(stateManager);
  var launcherManager = new hotword.LauncherManager(stateManager);
  var trainingManager = new hotword.TrainingManager(stateManager);

  // Detect when hotword settings have changed.
  chrome.hotwordPrivate.onEnabledChanged.addListener(function() {
    stateManager.updateStatus();
  });

  // Detect a request to delete the speaker model.
  chrome.hotwordPrivate.onDeleteSpeakerModel.addListener(function() {
    hotword.TrainingManager.handleDeleteSpeakerModel();
  });

  // Detect a request for the speaker model existence.
  chrome.hotwordPrivate.onSpeakerModelExists.addListener(function() {
    hotword.TrainingManager.handleSpeakerModelExists();
  });

  // Detect when the shared module containing the NaCL module and language model
  // is installed.
  chrome.management.onInstalled.addListener(function(info) {
    if (info.id == hotword.constants.SHARED_MODULE_ID) {
      hotword.debug('Shared module installed, reloading extension.');
      chrome.runtime.reload();
    }
  });
}());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword.metrics', function() {
  'use strict';

  /**
   * Helper function to record enum values in UMA.
   * @param {!string} name
   * @param {!number} value
   * @param {!number} maxValue
   */
  function recordEnum(name, value, maxValue) {
    var metricDesc = {
      'metricName': name,
      'type': 'histogram-linear',
      'min': 1,
      'max': maxValue,
      'buckets': maxValue + 1
    };
    chrome.metricsPrivate.recordValue(metricDesc, value);
  }

  return {
    recordEnum: recordEnum
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
'use strict';

/**
 * Class used to manage the state of the NaCl recognizer plugin. Handles all
 * control of the NaCl plugin, including creation, start, stop, trigger, and
 * shutdown.
 *
 * @param {boolean} loggingEnabled Whether audio logging is enabled.
 * @param {boolean} hotwordStream Whether the audio input stream is from a
 *     hotword stream.
 * @constructor
 * @extends {cr.EventTarget}
 */
function NaClManager(loggingEnabled, hotwordStream) {
  /**
   * Current state of this manager.
   * @private {hotword.NaClManager.ManagerState_}
   */
  this.recognizerState_ = ManagerState_.UNINITIALIZED;

  /**
   * The window.timeout ID associated with a pending message.
   * @private {?number}
   */
  this.naclTimeoutId_ = null;

  /**
   * The expected message that will cancel the current timeout.
   * @private {?string}
   */
  this.expectingMessage_ = null;

  /**
   * Whether the plugin will be started as soon as it stops.
   * @private {boolean}
   */
  this.restartOnStop_ = false;

  /**
   * NaCl plugin element on extension background page.
   * @private {?HTMLEmbedElement}
   */
  this.plugin_ = null;

  /**
   * URL containing hotword-model data file.
   * @private {string}
   */
  this.modelUrl_ = '';

  /**
   * Media stream containing an audio input track.
   * @private {?MediaStream}
   */
  this.stream_ = null;

  /**
   * The mode to start the recognizer in.
   * @private {?chrome.hotwordPrivate.RecognizerStartMode}
   */
  this.startMode_ = hotword.constants.RecognizerStartMode.NORMAL;

  /**
   * Whether audio logging is enabled.
   * @private {boolean}
   */
  this.loggingEnabled_ = loggingEnabled;

  /**
   * Whether the audio input stream is from a hotword stream.
   * @private {boolean}
   */
  this.hotwordStream_ = hotwordStream;

  /**
   * Audio log of X seconds before hotword triggered.
   * @private {?Object}
   */
  this.preambleLog_ = null;
};

/**
 * States this manager can be in. Since messages to/from the plugin are
 * asynchronous (and potentially queued), it's not possible to know what state
 * the plugin is in. However, track a state machine for NaClManager based on
 * what messages are sent/received.
 * @enum {number}
 * @private
 */
NaClManager.ManagerState_ = {
  UNINITIALIZED: 0,
  LOADING: 1,
  STOPPING: 2,
  STOPPED: 3,
  STARTING: 4,
  RUNNING: 5,
  ERROR: 6,
  SHUTDOWN: 7,
};
var ManagerState_ = NaClManager.ManagerState_;
var Error_ = hotword.constants.Error;
var UmaNaClMessageTimeout_ = hotword.constants.UmaNaClMessageTimeout;
var UmaNaClPluginLoadResult_ = hotword.constants.UmaNaClPluginLoadResult;

NaClManager.prototype.__proto__ = cr.EventTarget.prototype;

/**
 * Called when an error occurs. Dispatches an event.
 * @param {!hotword.constants.Error} error
 * @private
 */
NaClManager.prototype.handleError_ = function(error) {
  var event = new Event(hotword.constants.Event.ERROR);
  event.data = error;
  this.dispatchEvent(event);
};

/**
 * Record the result of loading the NaCl plugin to UMA.
 * @param {!hotword.constants.UmaNaClPluginLoadResult} error
 * @private
 */
NaClManager.prototype.logPluginLoadResult_ = function(error) {
  hotword.metrics.recordEnum(
      hotword.constants.UmaMetrics.NACL_PLUGIN_LOAD_RESULT,
      error,
      UmaNaClPluginLoadResult_.MAX);
};

/**
 * Set a timeout. Only allow one timeout to exist at any given time.
 * @param {!function()} func
 * @param {number} timeout
 * @private
 */
NaClManager.prototype.setTimeout_ = function(func, timeout) {
  assert(!this.naclTimeoutId_, 'Timeout already exists');
  this.naclTimeoutId_ = window.setTimeout(
      function() {
        this.naclTimeoutId_ = null;
        func();
      }.bind(this), timeout);
};

/**
 * Clears the current timeout.
 * @private
 */
NaClManager.prototype.clearTimeout_ = function() {
  window.clearTimeout(this.naclTimeoutId_);
  this.naclTimeoutId_ = null;
};

/**
 * Starts a stopped or stopping hotword recognizer (NaCl plugin).
 * @param {hotword.constants.RecognizerStartMode} mode The mode to start the
 *     recognizer in.
 */
NaClManager.prototype.startRecognizer = function(mode) {
  this.startMode_ = mode;
  if (this.recognizerState_ == ManagerState_.STOPPED) {
    this.preambleLog_ = null;
    this.recognizerState_ = ManagerState_.STARTING;
    if (mode == hotword.constants.RecognizerStartMode.NEW_MODEL) {
      hotword.debug('Starting Recognizer in START training mode');
      this.sendDataToPlugin_(hotword.constants.NaClPlugin.BEGIN_SPEAKER_MODEL);
    } else if (mode == hotword.constants.RecognizerStartMode.ADAPT_MODEL) {
      hotword.debug('Starting Recognizer in ADAPT training mode');
      this.sendDataToPlugin_(hotword.constants.NaClPlugin.ADAPT_SPEAKER_MODEL);
    } else {
      hotword.debug('Starting Recognizer in NORMAL mode');
      this.sendDataToPlugin_(hotword.constants.NaClPlugin.RESTART);
    }
    // Normally, there would be a waitForMessage_(READY_FOR_AUDIO) here.
    // However, this message is sent the first time audio data is read and in
    // some cases (ie. using the hotword stream), this won't happen until a
    // potential hotword trigger is seen. Having a waitForMessage_() would time
    // out in this case, so just leave it out. This ends up sacrificing a bit of
    // error detection in the non-hotword-stream case, but I think we can live
    // with that.
  } else if (this.recognizerState_ == ManagerState_.STOPPING) {
    // Wait until the plugin is stopped before trying to start it.
    this.restartOnStop_ = true;
  } else {
    throw 'Attempting to start NaCl recogniser not in STOPPED or STOPPING ' +
        'state';
  }
};

/**
 * Stops the hotword recognizer.
 */
NaClManager.prototype.stopRecognizer = function() {
  if (this.recognizerState_ == ManagerState_.STARTING) {
    // If the recognizer is stopped before it finishes starting, it causes an
    // assertion to be raised in waitForMessage_() since we're waiting for the
    // READY_FOR_AUDIO message. Clear the current timeout and expecting message
    // since we no longer expect it and may never receive it.
    this.clearTimeout_();
    this.expectingMessage_ = null;
  }
  this.sendDataToPlugin_(hotword.constants.NaClPlugin.STOP);
  this.recognizerState_ = ManagerState_.STOPPING;
  this.waitForMessage_(hotword.constants.TimeoutMs.NORMAL,
                       hotword.constants.NaClPlugin.STOPPED);
};

/**
 * Saves the speaker model.
 */
NaClManager.prototype.finalizeSpeakerModel = function() {
  if (this.recognizerState_ == ManagerState_.UNINITIALIZED ||
      this.recognizerState_ == ManagerState_.ERROR ||
      this.recognizerState_ == ManagerState_.SHUTDOWN ||
      this.recognizerState_ == ManagerState_.LOADING) {
    return;
  }
  this.sendDataToPlugin_(hotword.constants.NaClPlugin.FINISH_SPEAKER_MODEL);
};

/**
 * Checks whether the file at the given path exists.
 * @param {!string} path Path to a file. Can be any valid URL.
 * @return {boolean} True if the patch exists.
 * @private
 */
NaClManager.prototype.fileExists_ = function(path) {
  var xhr = new XMLHttpRequest();
  xhr.open('HEAD', path, false);
  try {
    xhr.send();
  } catch (err) {
    return false;
  }
  if (xhr.readyState != xhr.DONE || xhr.status != 200) {
    return false;
  }
  return true;
};

/**
 * Creates and returns a list of possible languages to check for hotword
 * support.
 * @return {!Array<string>} Array of languages.
 * @private
 */
NaClManager.prototype.getPossibleLanguages_ = function() {
  // Create array used to search first for language-country, if not found then
  // search for language, if not found then no language (empty string).
  // For example, search for 'en-us', then 'en', then ''.
  var langs = new Array();
  if (hotword.constants.UI_LANGUAGE) {
    // Chrome webstore doesn't support uppercase path: crbug.com/353407
    var language = hotword.constants.UI_LANGUAGE.toLowerCase();
    langs.push(language);  // Example: 'en-us'.
    // Remove country to add just the language to array.
    var hyphen = language.lastIndexOf('-');
    if (hyphen >= 0) {
      langs.push(language.substr(0, hyphen));  // Example: 'en'.
    }
  }
  langs.push('');
  return langs;
};

/**
 * Creates a NaCl plugin object and attaches it to the page.
 * @param {!string} src Location of the plugin.
 * @return {!HTMLEmbedElement} NaCl plugin DOM object.
 * @private
 */
NaClManager.prototype.createPlugin_ = function(src) {
  var plugin = /** @type {HTMLEmbedElement} */(document.createElement('embed'));
  plugin.src = src;
  plugin.type = 'application/x-nacl';
  document.body.appendChild(plugin);
  return plugin;
};

/**
 * Initializes the NaCl manager.
 * @param {!string} naclArch Either 'arm', 'x86-32' or 'x86-64'.
 * @param {!MediaStream} stream A stream containing an audio source track.
 * @return {boolean} True if the successful.
 */
NaClManager.prototype.initialize = function(naclArch, stream) {
  assert(this.recognizerState_ == ManagerState_.UNINITIALIZED,
         'Recognizer not in uninitialized state. State: ' +
         this.recognizerState_);
  assert(this.plugin_ == null);
  var langs = this.getPossibleLanguages_();
  var i, j;
  // For country-lang variations. For example, when combined with path it will
  // attempt to find: '/x86-32_en-gb/', else '/x86-32_en/', else '/x86-32_/'.
  for (i = 0; i < langs.length; i++) {
    var folder = hotword.constants.SHARED_MODULE_ROOT + '/_platform_specific/' +
        naclArch + '_' + langs[i] + '/';
    var dataSrc = folder + hotword.constants.File.RECOGNIZER_CONFIG;
    var pluginSrc = hotword.constants.SHARED_MODULE_ROOT + '/hotword_' +
        langs[i] + '.nmf';
    var dataExists = this.fileExists_(dataSrc) && this.fileExists_(pluginSrc);
    if (!dataExists) {
      continue;
    }

    var plugin = this.createPlugin_(pluginSrc);
    if (!plugin || !plugin.postMessage) {
      document.body.removeChild(plugin);
      this.recognizerState_ = ManagerState_.ERROR;
      return false;
    }
    this.plugin_ = plugin;
    this.modelUrl_ = chrome.extension.getURL(dataSrc);
    this.stream_ = stream;
    this.recognizerState_ = ManagerState_.LOADING;

    plugin.addEventListener('message',
                            this.handlePluginMessage_.bind(this),
                            false);

    plugin.addEventListener('crash',
                            function() {
                              this.handleError_(Error_.NACL_CRASH);
                              this.logPluginLoadResult_(
                                  UmaNaClPluginLoadResult_.CRASH);
                            }.bind(this),
                            false);
    return true;
  }
  this.recognizerState_ = ManagerState_.ERROR;
  this.logPluginLoadResult_(UmaNaClPluginLoadResult_.NO_MODULE_FOUND);
  return false;
};

/**
 * Shuts down the NaCl plugin and frees all resources.
 */
NaClManager.prototype.shutdown = function() {
  if (this.plugin_ != null) {
    document.body.removeChild(this.plugin_);
    this.plugin_ = null;
  }
  this.clearTimeout_();
  this.recognizerState_ = ManagerState_.SHUTDOWN;
  if (this.stream_)
    this.stream_.getAudioTracks()[0].stop();
  this.stream_ = null;
};

/**
 * Sends data to the NaCl plugin.
 * @param {!string|!MediaStreamTrack} data Command to be sent to NaCl plugin.
 * @private
 */
NaClManager.prototype.sendDataToPlugin_ = function(data) {
  assert(this.recognizerState_ != ManagerState_.UNINITIALIZED,
         'Recognizer in uninitialized state');
  this.plugin_.postMessage(data);
};

/**
 * Waits, with a timeout, for a message to be received from the plugin. If the
 * message is not seen within the timeout, dispatch an 'error' event and go into
 * the ERROR state.
 * @param {number} timeout Timeout, in milliseconds, to wait for the message.
 * @param {!string} message Message to wait for.
 * @private
 */
NaClManager.prototype.waitForMessage_ = function(timeout, message) {
  assert(this.expectingMessage_ == null, 'Cannot wait for message: ' +
      message + ', already waiting for message ' + this.expectingMessage_);
  this.setTimeout_(
      function() {
        this.recognizerState_ = ManagerState_.ERROR;
        this.handleError_(Error_.TIMEOUT);
        switch (this.expectingMessage_) {
          case hotword.constants.NaClPlugin.REQUEST_MODEL:
            var metricValue = UmaNaClMessageTimeout_.REQUEST_MODEL;
            break;
          case hotword.constants.NaClPlugin.MODEL_LOADED:
            var metricValue = UmaNaClMessageTimeout_.MODEL_LOADED;
            break;
          case hotword.constants.NaClPlugin.READY_FOR_AUDIO:
            var metricValue = UmaNaClMessageTimeout_.READY_FOR_AUDIO;
            break;
          case hotword.constants.NaClPlugin.STOPPED:
            var metricValue = UmaNaClMessageTimeout_.STOPPED;
            break;
          case hotword.constants.NaClPlugin.HOTWORD_DETECTED:
            var metricValue = UmaNaClMessageTimeout_.HOTWORD_DETECTED;
            break;
          case hotword.constants.NaClPlugin.MS_CONFIGURED:
            var metricValue = UmaNaClMessageTimeout_.MS_CONFIGURED;
            break;
        }
        hotword.metrics.recordEnum(
            hotword.constants.UmaMetrics.NACL_MESSAGE_TIMEOUT,
            metricValue,
            UmaNaClMessageTimeout_.MAX);
      }.bind(this), timeout);
  this.expectingMessage_ = message;
};

/**
 * Called when a message is received from the plugin. If we're waiting for that
 * message, cancel the pending timeout.
 * @param {string} message Message received.
 * @private
 */
NaClManager.prototype.receivedMessage_ = function(message) {
  if (message == this.expectingMessage_) {
    this.clearTimeout_();
    this.expectingMessage_ = null;
  }
};

/**
 * Handle a REQUEST_MODEL message from the plugin.
 * The plugin sends this message immediately after starting.
 * @private
 */
NaClManager.prototype.handleRequestModel_ = function() {
  if (this.recognizerState_ != ManagerState_.LOADING) {
    return;
  }
  this.logPluginLoadResult_(UmaNaClPluginLoadResult_.SUCCESS);
  this.sendDataToPlugin_(
      hotword.constants.NaClPlugin.MODEL_PREFIX + this.modelUrl_);
  this.waitForMessage_(hotword.constants.TimeoutMs.LONG,
                       hotword.constants.NaClPlugin.MODEL_LOADED);

  // Configure logging in the plugin. This can be configured any time before
  // starting the recognizer, and now is as good a time as any.
  if (this.loggingEnabled_) {
    this.sendDataToPlugin_(
        hotword.constants.NaClPlugin.LOG + ':' +
        hotword.constants.AUDIO_LOG_SECONDS);
  }

  // If the audio stream is from a hotword stream, tell the plugin.
  if (this.hotwordStream_) {
    this.sendDataToPlugin_(
        hotword.constants.NaClPlugin.DSP + ':' +
        hotword.constants.HOTWORD_STREAM_TIMEOUT_SECONDS);
  }
};

/**
 * Handle a MODEL_LOADED message from the plugin.
 * The plugin sends this message after successfully loading the language model.
 * @private
 */
NaClManager.prototype.handleModelLoaded_ = function() {
  if (this.recognizerState_ != ManagerState_.LOADING) {
    return;
  }
  this.sendDataToPlugin_(this.stream_.getAudioTracks()[0]);
  this.waitForMessage_(hotword.constants.TimeoutMs.LONG,
                       hotword.constants.NaClPlugin.MS_CONFIGURED);
};

/**
 * Handle a MS_CONFIGURED message from the plugin.
 * The plugin sends this message after successfully configuring the audio input
 * stream.
 * @private
 */
NaClManager.prototype.handleMsConfigured_ = function() {
  if (this.recognizerState_ != ManagerState_.LOADING) {
    return;
  }
  this.recognizerState_ = ManagerState_.STOPPED;
  this.dispatchEvent(new Event(hotword.constants.Event.READY));
};

/**
 * Handle a READY_FOR_AUDIO message from the plugin.
 * The plugin sends this message after the recognizer is started and
 * successfully receives and processes audio data.
 * @private
 */
NaClManager.prototype.handleReadyForAudio_ = function() {
  if (this.recognizerState_ != ManagerState_.STARTING) {
    return;
  }
  this.recognizerState_ = ManagerState_.RUNNING;
};

/**
 * Handle a HOTWORD_DETECTED message from the plugin.
 * The plugin sends this message after detecting the hotword.
 * @private
 */
NaClManager.prototype.handleHotwordDetected_ = function() {
  if (this.recognizerState_ != ManagerState_.RUNNING) {
    return;
  }
  // We'll receive a STOPPED message very soon.
  this.recognizerState_ = ManagerState_.STOPPING;
  this.waitForMessage_(hotword.constants.TimeoutMs.NORMAL,
                       hotword.constants.NaClPlugin.STOPPED);
  var event = new Event(hotword.constants.Event.TRIGGER);
  event.log = this.preambleLog_;
  this.dispatchEvent(event);
};

/**
 * Handle a STOPPED message from the plugin.
 * This plugin sends this message after stopping the recognizer. This can happen
 * either in response to a stop request, or after the hotword is detected.
 * @private
 */
NaClManager.prototype.handleStopped_ = function() {
  this.recognizerState_ = ManagerState_.STOPPED;
  if (this.restartOnStop_) {
    this.restartOnStop_ = false;
    this.startRecognizer(this.startMode_);
  }
};

/**
 * Handle a TIMEOUT message from the plugin.
 * The plugin sends this message when it thinks the stream is from a DSP and
 * a hotword wasn't detected within a timeout period after arrival of the first
 * audio samples.
 * @private
 */
NaClManager.prototype.handleTimeout_ = function() {
  if (this.recognizerState_ != ManagerState_.RUNNING) {
    return;
  }
  this.recognizerState_ = ManagerState_.STOPPED;
  this.dispatchEvent(new Event(hotword.constants.Event.TIMEOUT));
};

/**
 * Handle a SPEAKER_MODEL_SAVED message from the plugin.
 * The plugin sends this message after writing the model to a file.
 * @private
 */
NaClManager.prototype.handleSpeakerModelSaved_ = function() {
  this.dispatchEvent(new Event(hotword.constants.Event.SPEAKER_MODEL_SAVED));
};

/**
 * Handles a message from the NaCl plugin.
 * @param {!Event} msg Message from NaCl plugin.
 * @private
 */
NaClManager.prototype.handlePluginMessage_ = function(msg) {
  if (msg['data']) {
    if (typeof(msg['data']) == 'object') {
      // Save the preamble for delivery to the trigger handler when the trigger
      // message arrives.
      this.preambleLog_ = msg['data'];
      return;
    }
    this.receivedMessage_(msg['data']);
    switch (msg['data']) {
      case hotword.constants.NaClPlugin.REQUEST_MODEL:
        this.handleRequestModel_();
        break;
      case hotword.constants.NaClPlugin.MODEL_LOADED:
        this.handleModelLoaded_();
        break;
      case hotword.constants.NaClPlugin.MS_CONFIGURED:
        this.handleMsConfigured_();
        break;
      case hotword.constants.NaClPlugin.READY_FOR_AUDIO:
        this.handleReadyForAudio_();
        break;
      case hotword.constants.NaClPlugin.HOTWORD_DETECTED:
        this.handleHotwordDetected_();
        break;
      case hotword.constants.NaClPlugin.STOPPED:
        this.handleStopped_();
        break;
      case hotword.constants.NaClPlugin.TIMEOUT:
        this.handleTimeout_();
        break;
      case hotword.constants.NaClPlugin.SPEAKER_MODEL_SAVED:
        this.handleSpeakerModelSaved_();
        break;
    }
  }
};

return {
  NaClManager: NaClManager
};

});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Class used to manage the interaction between hotwording and the
   * NTP/google.com. Injects a content script to interact with NTP/google.com
   * and updates the global hotwording state based on interaction with those
   * pages.
   * @param {!hotword.StateManager} stateManager
   * @constructor
   */
  function PageAudioManager(stateManager) {
    /**
     * Manager of global hotwording state.
     * @private {!hotword.StateManager}
     */
    this.stateManager_ = stateManager;

    /**
     * Mapping between tab ID and port that is connected from the injected
     * content script.
     * @private {!Object<number, Port>}
     */
    this.portMap_ = {};

    /**
     * Chrome event listeners. Saved so that they can be de-registered when
     * hotwording is disabled.
     */
    this.connectListener_ = this.handleConnect_.bind(this);
    this.tabCreatedListener_ = this.handleCreatedTab_.bind(this);
    this.tabUpdatedListener_ = this.handleUpdatedTab_.bind(this);
    this.tabActivatedListener_ = this.handleActivatedTab_.bind(this);
    this.microphoneStateChangedListener_ =
        this.handleMicrophoneStateChanged_.bind(this);
    this.windowFocusChangedListener_ = this.handleChangedWindow_.bind(this);
    this.messageListener_ = this.handleMessageFromPage_.bind(this);

    // Need to setup listeners on startup, otherwise events that caused the
    // event page to start up, will be lost.
    this.setupListeners_();

    this.stateManager_.onStatusChanged.addListener(function() {
      this.updateListeners_();
      this.updateTabState_();
    }.bind(this));
  };

  var CommandToPage = hotword.constants.CommandToPage;
  var CommandFromPage = hotword.constants.CommandFromPage;

  PageAudioManager.prototype = {
    /**
     * Helper function to test if a URL path is eligible for hotwording.
     * @param {!string} url URL to check.
     * @param {!string} base Base URL to compare against..
     * @return {boolean} True if url is an eligible hotword URL.
     * @private
     */
    checkUrlPathIsEligible_: function(url, base) {
      if (url == base ||
          url == base + '/' ||
          url.indexOf(base + '/_/chrome/newtab?') == 0 ||  // Appcache NTP.
          url.indexOf(base + '/?') == 0 ||
          url.indexOf(base + '/#') == 0 ||
          url.indexOf(base + '/webhp') == 0 ||
          url.indexOf(base + '/search') == 0 ||
          url.indexOf(base + '/imghp') == 0) {
        return true;
      }
      return false;
    },

    /**
     * Determines if a URL is eligible for hotwording. For now, the valid pages
     * are the Google HP and SERP (this will include the NTP).
     * @param {!string} url URL to check.
     * @return {boolean} True if url is an eligible hotword URL.
     * @private
     */
    isEligibleUrl_: function(url) {
      if (!url)
        return false;

      var baseGoogleUrls = [
        'https://encrypted.google.',
        'https://images.google.',
        'https://www.google.'
      ];
      // TODO(amistry): Get this list from a file in the shared module instead.
      var tlds = [
        'at',
        'ca',
        'com',
        'com.au',
        'com.mx',
        'com.br',
        'co.jp',
        'co.kr',
        'co.nz',
        'co.uk',
        'co.za',
        'de',
        'es',
        'fr',
        'it',
        'ru'
      ];

      // Check for the new tab page first.
      if (this.checkUrlPathIsEligible_(url, 'chrome://newtab'))
        return true;

      // Check URLs with each type of local-based TLD.
      for (var i = 0; i < baseGoogleUrls.length; i++) {
        for (var j = 0; j < tlds.length; j++) {
          var base = baseGoogleUrls[i] + tlds[j];
          if (this.checkUrlPathIsEligible_(url, base))
            return true;
        }
      }
      return false;
    },

    /**
     * Locates the current active tab in the current focused window and
     * performs a callback with the tab as the parameter.
     * @param {function(?Tab)} callback Function to call with the
     *     active tab or null if not found. The function's |this| will be set to
     *     this object.
     * @private
     */
    findCurrentTab_: function(callback) {
      chrome.windows.getAll(
          {'populate': true},
          function(windows) {
            for (var i = 0; i < windows.length; ++i) {
              if (!windows[i].focused)
                continue;

              for (var j = 0; j < windows[i].tabs.length; ++j) {
                var tab = windows[i].tabs[j];
                if (tab.active) {
                  callback.call(this, tab);
                  return;
                }
              }
            }
            callback.call(this, null);
          }.bind(this));
    },

    /**
     * This function is called when a tab is activated (comes into focus).
     * @param {Tab} tab Current active tab.
     * @private
     */
    activateTab_: function(tab) {
      if (!tab) {
        this.stopHotwording_();
        return;
      }
      if (tab.id in this.portMap_) {
        this.startHotwordingIfEligible_();
        return;
      }
      this.stopHotwording_();
      this.prepareTab_(tab);
    },

    /**
     * Prepare a new or updated tab by injecting the content script.
     * @param {!Tab} tab Newly updated or created tab.
     * @private
     */
    prepareTab_: function(tab) {
      if (!this.isEligibleUrl_(tab.url))
        return;

      chrome.tabs.executeScript(
          tab.id,
          {'file': 'audio_client.js'},
          function(results) {
            if (chrome.runtime.lastError) {
              // Ignore this error. For new tab pages, even though the URL is
              // reported to be chrome://newtab/, the actual URL is a
              // country-specific google domain. Since we don't have permission
              // to inject on every page, an error will happen when the user is
              // in an unsupported country.
              //
              // The property still needs to be accessed so that the error
              // condition is cleared. If it isn't, exectureScript will log an
              // error the next time it is called.
            }
          });
    },

    /**
     * Updates hotwording state based on the state of current tabs/windows.
     * @private
     */
    updateTabState_: function() {
      this.findCurrentTab_(this.activateTab_);
    },

    /**
     * Handles a newly created tab.
     * @param {!Tab} tab Newly created tab.
     * @private
     */
    handleCreatedTab_: function(tab) {
      this.prepareTab_(tab);
    },

    /**
     * Handles an updated tab.
     * @param {number} tabId Id of the updated tab.
     * @param {{status: string}} info Change info of the tab.
     * @param {!Tab} tab Updated tab.
     * @private
     */
    handleUpdatedTab_: function(tabId, info, tab) {
      // Chrome fires multiple update events: undefined, loading and completed.
      // We perform content injection on loading state.
      if (info['status'] != 'loading')
        return;

      this.prepareTab_(tab);
    },

    /**
     * Handles a tab that has just become active.
     * @param {{tabId: number}} info Information about the activated tab.
     * @private
     */
    handleActivatedTab_: function(info) {
      this.updateTabState_();
    },

    /**
     * Handles the microphone state changing.
     * @param {boolean} enabled Whether the microphone is now enabled.
     * @private
     */
    handleMicrophoneStateChanged_: function(enabled) {
      if (enabled) {
        this.updateTabState_();
        return;
      }

      this.stopHotwording_();
    },

    /**
     * Handles a change in Chrome windows.
     * Note: this does not always trigger in Linux.
     * @param {number} windowId Id of newly focused window.
     * @private
     */
    handleChangedWindow_: function(windowId) {
      this.updateTabState_();
    },

    /**
     * Handles a content script attempting to connect.
     * @param {!Port} port Communications port from the client.
     * @private
     */
    handleConnect_: function(port) {
      if (port.name != hotword.constants.CLIENT_PORT_NAME)
        return;

      var tab = /** @type {!Tab} */(port.sender.tab);
      // An existing port from the same tab might already exist. But that port
      // may be from the previous page, so just overwrite the port.
      this.portMap_[tab.id] = port;
      port.onDisconnect.addListener(function() {
        this.handleClientDisconnect_(port);
      }.bind(this));
      port.onMessage.addListener(function(msg) {
        this.handleMessage_(msg, port.sender, port.postMessage);
      }.bind(this));
    },

    /**
     * Handles a client content script disconnect.
     * @param {Port} port Disconnected port.
     * @private
     */
    handleClientDisconnect_: function(port) {
      var tabId = port.sender.tab.id;
      if (tabId in this.portMap_ && this.portMap_[tabId] == port) {
        // Due to a race between port disconnection and tabs.onUpdated messages,
        // the port could have changed.
        delete this.portMap_[port.sender.tab.id];
      }
      this.stopHotwordingIfIneligibleTab_();
    },

    /**
     * Disconnect all connected clients.
     * @private
     */
    disconnectAllClients_: function() {
      for (var id in this.portMap_) {
        var port = this.portMap_[id];
        port.disconnect();
        delete this.portMap_[id];
      }
    },

    /**
     * Sends a command to the client content script on an eligible tab.
     * @param {hotword.constants.CommandToPage} command Command to send.
     * @param {number} tabId Id of the target tab.
     * @private
     */
    sendClient_: function(command, tabId) {
      if (tabId in this.portMap_) {
        var message = {};
        message[hotword.constants.COMMAND_FIELD_NAME] = command;
        this.portMap_[tabId].postMessage(message);
      }
    },

    /**
     * Sends a command to all connected clients.
     * @param {hotword.constants.CommandToPage} command Command to send.
     * @private
     */
    sendAllClients_: function(command) {
      for (var idStr in this.portMap_) {
        var id = parseInt(idStr, 10);
        assert(!isNaN(id), 'Tab ID is not a number: ' + idStr);
        this.sendClient_(command, id);
      }
    },

    /**
     * Handles a hotword trigger. Sends a trigger message to the currently
     * active tab.
     * @private
     */
    hotwordTriggered_: function() {
      this.findCurrentTab_(function(tab) {
        if (tab)
          this.sendClient_(CommandToPage.HOTWORD_VOICE_TRIGGER, tab.id);
      });
    },

    /**
     * Starts hotwording.
     * @private
     */
    startHotwording_: function() {
      this.stateManager_.startSession(
          hotword.constants.SessionSource.NTP,
          function() {
            this.sendAllClients_(CommandToPage.HOTWORD_STARTED);
          }.bind(this),
          this.hotwordTriggered_.bind(this));
    },

    /**
     * Starts hotwording if the currently active tab is eligible for hotwording
     * (e.g. google.com).
     * @private
     */
    startHotwordingIfEligible_: function() {
      this.findCurrentTab_(function(tab) {
        if (!tab) {
          this.stopHotwording_();
          return;
        }
        if (this.isEligibleUrl_(tab.url))
          this.startHotwording_();
      });
    },

    /**
     * Stops hotwording.
     * @private
     */
    stopHotwording_: function() {
      this.stateManager_.stopSession(hotword.constants.SessionSource.NTP);
      this.sendAllClients_(CommandToPage.HOTWORD_ENDED);
    },

    /**
     * Stops hotwording if the currently active tab is not eligible for
     * hotwording (i.e. google.com).
     * @private
     */
    stopHotwordingIfIneligibleTab_: function() {
      this.findCurrentTab_(function(tab) {
        if (!tab) {
          this.stopHotwording_();
          return;
        }
        if (!this.isEligibleUrl_(tab.url))
          this.stopHotwording_();
      });
    },

    /**
     * Handles a message from the content script injected into the page.
     * @param {!Object} request Request from the content script.
     * @param {!MessageSender} sender Message sender.
     * @param {!function(Object)} sendResponse Function for sending a response.
     * @private
     */
    handleMessage_: function(request, sender, sendResponse) {
      switch (request[hotword.constants.COMMAND_FIELD_NAME]) {
        // TODO(amistry): Handle other messages such as CLICKED_RESUME and
        // CLICKED_RESTART, if necessary.
        case CommandFromPage.SPEECH_START:
          this.stopHotwording_();
          break;
        case CommandFromPage.SPEECH_END:
        case CommandFromPage.SPEECH_RESET:
          this.startHotwording_();
          break;
      }
    },


    /**
     * Handles a message directly from the NTP/HP/SERP.
     * @param {!Object} request Message from the sender.
     * @param {!MessageSender} sender Information about the sender.
     * @param {!function(HotwordStatus)} sendResponse Callback to respond
     *     to sender.
     * @return {boolean} Whether to maintain the port open to call sendResponse.
     * @private
     */
    handleMessageFromPage_: function(request, sender, sendResponse) {
      switch (request.type) {
        case CommandFromPage.PAGE_WAKEUP:
          if (sender.tab && this.isEligibleUrl_(sender.tab.url)) {
            chrome.hotwordPrivate.getStatus(
                true /* getOptionalFields */,
                this.statusDone_.bind(
                    this,
                    request.tab || sender.tab || {incognito: true},
                    sendResponse));
            return true;
          }

          // Do not show the opt-in promo for ineligible urls.
          this.sendResponse_({'doNotShowOptinMessage': true}, sendResponse);
          break;
        case CommandFromPage.CLICKED_OPTIN:
          chrome.hotwordPrivate.setEnabled(true);
          break;
        // User has explicitly clicked 'no thanks'.
        case CommandFromPage.CLICKED_NO_OPTIN:
          chrome.hotwordPrivate.setEnabled(false);
          break;
      }
      return false;
    },

    /**
     * Sends a message directly to the sending page.
     * @param {!HotwordStatus} response The response to send to the sender.
     * @param {!function(HotwordStatus)} sendResponse Callback to respond
     *     to sender.
     * @private
     */
    sendResponse_: function(response, sendResponse) {
      try {
        sendResponse(response);
      } catch (err) {
        // Suppress the exception thrown by sendResponse() when the page doesn't
        // specify a response callback in the call to
        // chrome.runtime.sendMessage().
        // Unfortunately, there doesn't appear to be a way to detect one-way
        // messages without explicitly saying in the message itself. This
        // message is defined as a constant in
        // extensions/renderer/messaging_bindings.cc
        if (err.message == 'Attempting to use a disconnected port object')
          return;
        throw err;
      }
    },

    /**
     * Sends the response to the tab.
     * @param {Tab} tab The tab that the request was sent from.
     * @param {function(HotwordStatus)} sendResponse Callback function to
     *     respond to sender.
     * @param {HotwordStatus} hotwordStatus Status of the hotword extension.
     * @private
     */
    statusDone_: function(tab, sendResponse, hotwordStatus) {
      var response = {'doNotShowOptinMessage': true};

      // If always-on is available, then we do not show the promo, as the promo
      // only works with the sometimes-on pref.
      if (!tab.incognito && hotwordStatus.available &&
          !hotwordStatus.enabledSet && !hotwordStatus.alwaysOnAvailable) {
        response = hotwordStatus;
      }

      this.sendResponse_(response, sendResponse);
    },

    /**
     * Set up event listeners.
     * @private
     */
    setupListeners_: function() {
      if (chrome.runtime.onConnect.hasListener(this.connectListener_))
        return;

      chrome.runtime.onConnect.addListener(this.connectListener_);
      chrome.tabs.onCreated.addListener(this.tabCreatedListener_);
      chrome.tabs.onUpdated.addListener(this.tabUpdatedListener_);
      chrome.tabs.onActivated.addListener(this.tabActivatedListener_);
      chrome.windows.onFocusChanged.addListener(
          this.windowFocusChangedListener_);
      chrome.hotwordPrivate.onMicrophoneStateChanged.addListener(
          this.microphoneStateChangedListener_);
      if (chrome.runtime.onMessage.hasListener(this.messageListener_))
        return;
      chrome.runtime.onMessageExternal.addListener(
          this.messageListener_);
    },

    /**
     * Remove event listeners.
     * @private
     */
    removeListeners_: function() {
      chrome.runtime.onConnect.removeListener(this.connectListener_);
      chrome.tabs.onCreated.removeListener(this.tabCreatedListener_);
      chrome.tabs.onUpdated.removeListener(this.tabUpdatedListener_);
      chrome.tabs.onActivated.removeListener(this.tabActivatedListener_);
      chrome.windows.onFocusChanged.removeListener(
          this.windowFocusChangedListener_);
      chrome.hotwordPrivate.onMicrophoneStateChanged.removeListener(
          this.microphoneStateChangedListener_);
      // Don't remove the Message listener, as we want them listening all
      // the time,
    },

    /**
     * Update event listeners based on the current hotwording state.
     * @private
     */
    updateListeners_: function() {
      if (this.stateManager_.isSometimesOnEnabled()) {
        this.setupListeners_();
      } else {
        this.removeListeners_();
        this.stopHotwording_();
        this.disconnectAllClients_();
      }
    }
  };

  return {
    PageAudioManager: PageAudioManager
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Trivial container class for session information.
   * @param {!hotword.constants.SessionSource} source Source of the hotword
   *     session.
   * @param {!function()} triggerCb Callback invoked when the hotword has
   *     triggered.
   * @param {!function()} startedCb Callback invoked when the session has
   *     been started successfully.
   * @param {function()=} opt_modelSavedCb Callback invoked when the speaker
   *     model has been saved successfully.
   * @constructor
   * @struct
   * @private
   */
  function Session_(source, triggerCb, startedCb, opt_modelSavedCb) {
    /**
     * Source of the hotword session request.
     * @private {!hotword.constants.SessionSource}
     */
    this.source_ = source;

     /**
      * Callback invoked when the hotword has triggered.
      * @private {!function()}
      */
    this.triggerCb_ = triggerCb;

    /**
     * Callback invoked when the session has been started successfully.
     * @private {?function()}
     */
    this.startedCb_ = startedCb;

    /**
     * Callback invoked when the session has been started successfully.
     * @private {?function()}
     */
    this.speakerModelSavedCb_ = opt_modelSavedCb;
  }

  /**
   * Class to manage hotwording state. Starts/stops the hotword detector based
   * on user settings, session requests, and any other factors that play into
   * whether or not hotwording should be running.
   * @constructor
   */
  function StateManager() {
    /**
     * Current state.
     * @private {hotword.StateManager.State_}
     */
    this.state_ = State_.STOPPED;

    /**
     * Current hotwording status.
     * @private {?chrome.hotwordPrivate.StatusDetails}
     */
    this.hotwordStatus_ = null;

    /**
     * NaCl plugin manager.
     * @private {?hotword.NaClManager}
     */
    this.pluginManager_ = null;

    /**
     * Currently active hotwording sessions.
     * @private {!Array<Session_>}
     */
    this.sessions_ = [];

    /**
     * The mode to start the recognizer in.
     * @private {!hotword.constants.RecognizerStartMode}
     */
    this.startMode_ = hotword.constants.RecognizerStartMode.NORMAL;

    /**
     * Event that fires when the hotwording status has changed.
     * @type {!ChromeEvent}
     */
    this.onStatusChanged = new chrome.Event();

    /**
     * Hotword trigger audio notification... a.k.a The Chime (tm).
     * @private {!HTMLAudioElement}
     */
    this.chime_ =
        /** @type {!HTMLAudioElement} */(document.createElement('audio'));

    /**
     * Chrome event listeners. Saved so that they can be de-registered when
     * hotwording is disabled.
     * @private
     */
    this.idleStateChangedListener_ = this.handleIdleStateChanged_.bind(this);
    this.startupListener_ = this.handleStartup_.bind(this);

    /**
     * Whether this user is locked.
     * @private {boolean}
     */
    this.isLocked_ = false;

    /**
     * Current state of audio logging.
     * This is tracked separately from hotwordStatus_ because we need to restart
     * the hotword detector when this value changes.
     * @private {boolean}
     */
    this.loggingEnabled_ = false;

    /**
     * Current state of training.
     * This is tracked separately from |hotwordStatus_| because we need to
     * restart the hotword detector when this value changes.
     * @private {!boolean}
     */
    this.trainingEnabled_ = false;

    /**
     * Helper class to keep this extension alive while the hotword detector is
     * running in always-on mode.
     * @private {!hotword.KeepAlive}
     */
    this.keepAlive_ = new hotword.KeepAlive();

    // Get the initial status.
    chrome.hotwordPrivate.getStatus(this.handleStatus_.bind(this));

    // Setup the chime and insert into the page.
    // Set preload=none to prevent an audio output stream from being created
    // when the extension loads.
    this.chime_.preload = 'none';
    this.chime_.src = chrome.extension.getURL(
        hotword.constants.SHARED_MODULE_ROOT + '/audio/chime.wav');
    document.body.appendChild(this.chime_);

    // In order to remove this listener, it must first be added. This handles
    // the case on first Chrome startup where this event is never registered,
    // so can't be removed when it's determined that hotwording is disabled.
    // Why not only remove the listener if it exists? Extension API events have
    // two parts to them, the Javascript listeners, and a browser-side component
    // that wakes up the extension if it's an event page. The browser-side
    // wake-up event is only removed when the number of javascript listeners
    // becomes 0. To clear the browser wake-up event, a listener first needs to
    // be added, then removed in order to drop the count to 0 and remove the
    // event.
    chrome.runtime.onStartup.addListener(this.startupListener_);
  }

  /**
   * @enum {number}
   * @private
   */
  StateManager.State_ = {
    STOPPED: 0,
    STARTING: 1,
    RUNNING: 2,
    ERROR: 3,
  };
  var State_ = StateManager.State_;

  var UmaMediaStreamOpenResults_ = {
    // These first error are defined by the MediaStream spec:
    // http://w3c.github.io/mediacapture-main/getusermedia.html#idl-def-MediaStreamError
    'NotSupportedError':
        hotword.constants.UmaMediaStreamOpenResult.NOT_SUPPORTED,
    'PermissionDeniedError':
        hotword.constants.UmaMediaStreamOpenResult.PERMISSION_DENIED,
    'ConstraintNotSatisfiedError':
        hotword.constants.UmaMediaStreamOpenResult.CONSTRAINT_NOT_SATISFIED,
    'OverconstrainedError':
        hotword.constants.UmaMediaStreamOpenResult.OVERCONSTRAINED,
    'NotFoundError': hotword.constants.UmaMediaStreamOpenResult.NOT_FOUND,
    'AbortError': hotword.constants.UmaMediaStreamOpenResult.ABORT,
    'SourceUnavailableError':
        hotword.constants.UmaMediaStreamOpenResult.SOURCE_UNAVAILABLE,
    // The next few errors are chrome-specific. See:
    // content/renderer/media/user_media_client_impl.cc
    // (UserMediaClientImpl::GetUserMediaRequestFailed)
    'PermissionDismissedError':
        hotword.constants.UmaMediaStreamOpenResult.PERMISSION_DISMISSED,
    'InvalidStateError':
        hotword.constants.UmaMediaStreamOpenResult.INVALID_STATE,
    'DevicesNotFoundError':
        hotword.constants.UmaMediaStreamOpenResult.DEVICES_NOT_FOUND,
    'InvalidSecurityOriginError':
        hotword.constants.UmaMediaStreamOpenResult.INVALID_SECURITY_ORIGIN
  };

  var UmaTriggerSources_ = {
    'launcher': hotword.constants.UmaTriggerSource.LAUNCHER,
    'ntp': hotword.constants.UmaTriggerSource.NTP_GOOGLE_COM,
    'always': hotword.constants.UmaTriggerSource.ALWAYS_ON,
    'training': hotword.constants.UmaTriggerSource.TRAINING
  };

  StateManager.prototype = {
    /**
     * Request status details update. Intended to be called from the
     * hotwordPrivate.onEnabledChanged() event.
     */
    updateStatus: function() {
      chrome.hotwordPrivate.getStatus(this.handleStatus_.bind(this));
    },

    /**
     * @return {boolean} True if google.com/NTP/launcher hotwording is enabled.
     */
    isSometimesOnEnabled: function() {
      assert(this.hotwordStatus_,
             'No hotwording status (isSometimesOnEnabled)');
      // Although the two settings are supposed to be mutually exclusive, it's
      // possible for both to be set. In that case, always-on takes precedence.
      return this.hotwordStatus_.enabled &&
          !this.hotwordStatus_.alwaysOnEnabled;
    },

    /**
     * @return {boolean} True if always-on hotwording is enabled.
     */
    isAlwaysOnEnabled: function() {
      assert(this.hotwordStatus_, 'No hotword status (isAlwaysOnEnabled)');
      return this.hotwordStatus_.alwaysOnEnabled &&
          !this.hotwordStatus_.trainingEnabled;
    },

    /**
     * @return {boolean} True if training is enabled.
     */
    isTrainingEnabled: function() {
      assert(this.hotwordStatus_, 'No hotword status (isTrainingEnabled)');
      return this.hotwordStatus_.trainingEnabled;
    },

    /**
     * Callback for hotwordPrivate.getStatus() function.
     * @param {chrome.hotwordPrivate.StatusDetails} status Current hotword
     *     status.
     * @private
     */
    handleStatus_: function(status) {
      hotword.debug('New hotword status', status);
      this.hotwordStatus_ = status;
      this.updateStateFromStatus_();

      this.onStatusChanged.dispatch();
    },

    /**
     * Updates state based on the current status.
     * @private
     */
    updateStateFromStatus_: function() {
      if (!this.hotwordStatus_)
        return;

      if (this.hotwordStatus_.enabled ||
          this.hotwordStatus_.alwaysOnEnabled ||
          this.hotwordStatus_.trainingEnabled) {
        // Detect changes to audio logging and kill the detector if that setting
        // has changed.
        if (this.hotwordStatus_.audioLoggingEnabled != this.loggingEnabled_)
          this.shutdownDetector_();
        this.loggingEnabled_ = this.hotwordStatus_.audioLoggingEnabled;

        // If the training state has changed, we need to first shut down the
        // detector so that we can restart in a different mode.
        if (this.hotwordStatus_.trainingEnabled != this.trainingEnabled_)
          this.shutdownDetector_();
        this.trainingEnabled_ = this.hotwordStatus_.trainingEnabled;

        // Start the detector if there's a session and the user is unlocked, and
        // stops it otherwise.
        if (this.sessions_.length && !this.isLocked_ &&
            this.hotwordStatus_.userIsActive) {
          this.startDetector_();
        } else {
          this.shutdownDetector_();
        }

        if (!chrome.idle.onStateChanged.hasListener(
                this.idleStateChangedListener_)) {
          chrome.idle.onStateChanged.addListener(
              this.idleStateChangedListener_);
        }
        if (!chrome.runtime.onStartup.hasListener(this.startupListener_))
          chrome.runtime.onStartup.addListener(this.startupListener_);
      } else {
        // Not enabled. Shut down if running.
        this.shutdownDetector_();

        chrome.idle.onStateChanged.removeListener(
            this.idleStateChangedListener_);
        // If hotwording isn't enabled, don't start this component extension on
        // Chrome startup. If a user enables hotwording, the status change
        // event will be fired and the onStartup event will be registered.
        chrome.runtime.onStartup.removeListener(this.startupListener_);
      }
    },

    /**
     * Starts the hotword detector.
     * @private
     */
    startDetector_: function() {
      // Last attempt to start detector resulted in an error.
      if (this.state_ == State_.ERROR) {
        // TODO(amistry): Do some error rate tracking here and disable the
        // extension if we error too often.
      }

      if (!this.pluginManager_) {
        this.state_ = State_.STARTING;
        var isHotwordStream = this.isAlwaysOnEnabled() &&
            this.hotwordStatus_.hotwordHardwareAvailable;
        this.pluginManager_ = new hotword.NaClManager(this.loggingEnabled_,
                                                      isHotwordStream);
        this.pluginManager_.addEventListener(hotword.constants.Event.READY,
                                             this.onReady_.bind(this));
        this.pluginManager_.addEventListener(hotword.constants.Event.ERROR,
                                             this.onError_.bind(this));
        this.pluginManager_.addEventListener(hotword.constants.Event.TRIGGER,
                                             this.onTrigger_.bind(this));
        this.pluginManager_.addEventListener(hotword.constants.Event.TIMEOUT,
                                             this.onTimeout_.bind(this));
        this.pluginManager_.addEventListener(
            hotword.constants.Event.SPEAKER_MODEL_SAVED,
            this.onSpeakerModelSaved_.bind(this));
        chrome.runtime.getPlatformInfo(function(platform) {
          var naclArch = platform.nacl_arch;

          // googDucking set to false so that audio output level from other tabs
          // is not affected when hotword is enabled. https://crbug.com/357773
          // content/common/media/media_stream_options.cc
          // When always-on is enabled, request the hotword stream.
          // Optional because we allow power users to bypass the hardware
          // detection via a flag, and hence the hotword stream may not be
          // available.
          var constraints = /** @type {googMediaStreamConstraints} */
              ({audio: {optional: [
                { googDucking: false },
                { googHotword: this.isAlwaysOnEnabled() }
              ]}});
          navigator.webkitGetUserMedia(
              /** @type {MediaStreamConstraints} */ (constraints),
              function(stream) {
                hotword.metrics.recordEnum(
                    hotword.constants.UmaMetrics.MEDIA_STREAM_RESULT,
                    hotword.constants.UmaMediaStreamOpenResult.SUCCESS,
                    hotword.constants.UmaMediaStreamOpenResult.MAX);
                // The detector could have been shut down before the stream
                // finishes opening.
                if (this.pluginManager_ == null) {
                  stream.getAudioTracks()[0].stop();
                  return;
                }

                if (this.isAlwaysOnEnabled())
                  this.keepAlive_.start();
                if (!this.pluginManager_.initialize(naclArch, stream)) {
                  this.state_ = State_.ERROR;
                  this.shutdownPluginManager_();
                }
              }.bind(this),
              function(error) {
                if (error.name in UmaMediaStreamOpenResults_) {
                  var metricValue = UmaMediaStreamOpenResults_[error.name];
                } else {
                  var metricValue =
                      hotword.constants.UmaMediaStreamOpenResult.UNKNOWN;
                }
                hotword.metrics.recordEnum(
                    hotword.constants.UmaMetrics.MEDIA_STREAM_RESULT,
                    metricValue,
                    hotword.constants.UmaMediaStreamOpenResult.MAX);
                this.state_ = State_.ERROR;
                this.pluginManager_ = null;
              }.bind(this));
        }.bind(this));
      } else if (this.state_ != State_.STARTING) {
        // Don't try to start a starting detector.
        this.startRecognizer_();
      }
    },

    /**
     * Start the recognizer plugin. Assumes the plugin has been loaded and is
     * ready to start.
     * @private
     */
    startRecognizer_: function() {
      assert(this.pluginManager_, 'No NaCl plugin loaded');
      if (this.state_ != State_.RUNNING) {
        this.state_ = State_.RUNNING;
        if (this.isAlwaysOnEnabled())
          this.keepAlive_.start();
        this.pluginManager_.startRecognizer(this.startMode_);
      }
      for (var i = 0; i < this.sessions_.length; i++) {
        var session = this.sessions_[i];
        if (session.startedCb_) {
          session.startedCb_();
          session.startedCb_ = null;
        }
      }
    },

    /**
     * Stops the hotword detector, if it's running.
     * @private
     */
    stopDetector_: function() {
      this.keepAlive_.stop();
      if (this.pluginManager_ && this.state_ == State_.RUNNING) {
        this.state_ = State_.STOPPED;
        this.pluginManager_.stopRecognizer();
      }
    },

    /**
     * Shuts down and removes the plugin manager, if it exists.
     * @private
     */
    shutdownPluginManager_: function() {
      this.keepAlive_.stop();
      if (this.pluginManager_) {
        this.pluginManager_.shutdown();
        this.pluginManager_ = null;
      }
    },

    /**
     * Shuts down the hotword detector.
     * @private
     */
    shutdownDetector_: function() {
      this.state_ = State_.STOPPED;
      this.shutdownPluginManager_();
    },

    /**
     * Finalizes the speaker model. Assumes the plugin has been loaded and
     * started.
     */
    finalizeSpeakerModel: function() {
      assert(this.pluginManager_,
             'Cannot finalize speaker model: No NaCl plugin loaded');
      if (this.state_ != State_.RUNNING) {
        hotword.debug('Cannot finalize speaker model: NaCl plugin not started');
        return;
      }
      this.pluginManager_.finalizeSpeakerModel();
    },

    /**
     * Handle the hotword plugin being ready to start.
     * @private
     */
    onReady_: function() {
      if (this.state_ != State_.STARTING) {
        // At this point, we should not be in the RUNNING state. Doing so would
        // imply the hotword detector was started without being ready.
        assert(this.state_ != State_.RUNNING, 'Unexpected RUNNING state');
        this.shutdownPluginManager_();
        return;
      }
      this.startRecognizer_();
    },

    /**
     * Handle an error from the hotword plugin.
     * @private
     */
    onError_: function() {
      this.state_ = State_.ERROR;
      this.shutdownPluginManager_();
    },

    /**
     * Handle hotword triggering.
     * @param {!Event} event Event containing audio log data.
     * @private
     */
    onTrigger_: function(event) {
      this.keepAlive_.stop();
      hotword.debug('Hotword triggered!');
      chrome.metricsPrivate.recordUserAction(
          hotword.constants.UmaMetrics.TRIGGER);
      assert(this.pluginManager_, 'No NaCl plugin loaded on trigger');
      // Detector implicitly stops when the hotword is detected.
      this.state_ = State_.STOPPED;

      // Play the chime.
      this.chime_.play();

      // Implicitly clear the top session. A session needs to be started in
      // order to restart the detector.
      if (this.sessions_.length) {
        var session = this.sessions_.pop();
        session.triggerCb_(event.log);

        hotword.metrics.recordEnum(
            hotword.constants.UmaMetrics.TRIGGER_SOURCE,
            UmaTriggerSources_[session.source_],
            hotword.constants.UmaTriggerSource.MAX);
      }

      // If we're in always-on mode, shut down the hotword detector. The hotword
      // stream requires that we close and re-open it after a trigger, and the
      // only way to accomplish this is to shut everything down.
      if (this.isAlwaysOnEnabled())
        this.shutdownDetector_();
    },

    /**
     * Handle hotword timeout.
     * @private
     */
    onTimeout_: function() {
      hotword.debug('Hotword timeout!');

      // We get this event when the hotword detector thinks there's a false
      // trigger. In this case, we need to shut down and restart the detector to
      // re-arm the DSP.
      this.shutdownDetector_();
      this.updateStateFromStatus_();
    },

    /**
     * Handle speaker model saved.
     * @private
     */
    onSpeakerModelSaved_: function() {
      hotword.debug('Speaker model saved!');

      if (this.sessions_.length) {
        // Only call the callback of the the top session.
        var session = this.sessions_[this.sessions_.length - 1];
        if (session.speakerModelSavedCb_)
          session.speakerModelSavedCb_();
      }
    },

    /**
     * Remove a hotwording session from the given source.
     * @param {!hotword.constants.SessionSource} source Source of the hotword
     *     session request.
     * @private
     */
    removeSession_: function(source) {
      for (var i = 0; i < this.sessions_.length; i++) {
        if (this.sessions_[i].source_ == source) {
          this.sessions_.splice(i, 1);
          break;
        }
      }
    },

    /**
     * Start a hotwording session.
     * @param {!hotword.constants.SessionSource} source Source of the hotword
     *     session request.
     * @param {!function()} startedCb Callback invoked when the session has
     *     been started successfully.
     * @param {!function()} triggerCb Callback invoked when the hotword has
     * @param {function()=} modelSavedCb Callback invoked when the speaker model
     *     has been saved.
     * @param {hotword.constants.RecognizerStartMode=} opt_mode The mode to
     *     start the recognizer in.
     */
    startSession: function(source, startedCb, triggerCb,
                           opt_modelSavedCb, opt_mode) {
      if (this.isTrainingEnabled() && opt_mode) {
        this.startMode_ = opt_mode;
      } else {
        this.startMode_ = hotword.constants.RecognizerStartMode.NORMAL;
      }
      hotword.debug('Starting session for source: ' + source);
      this.removeSession_(source);
      this.sessions_.push(new Session_(source, triggerCb, startedCb,
                                       opt_modelSavedCb));
      this.updateStateFromStatus_();
    },

    /**
     * Stops a hotwording session.
     * @param {!hotword.constants.SessionSource} source Source of the hotword
     *     session request.
     */
    stopSession: function(source) {
      hotword.debug('Stopping session for source: ' + source);
      this.removeSession_(source);
      // If this is a training session then switch the start mode back to
      // normal.
      if (source == hotword.constants.SessionSource.TRAINING)
        this.startMode_ = hotword.constants.RecognizerStartMode.NORMAL;
      this.updateStateFromStatus_();
    },

    /**
     * Handles a chrome.idle.onStateChanged event.
     * @param {!string} state State, one of "active", "idle", or "locked".
     * @private
     */
    handleIdleStateChanged_: function(state) {
      hotword.debug('Idle state changed: ' + state);
      var oldLocked = this.isLocked_;
      if (state == 'locked')
        this.isLocked_ = true;
      else
        this.isLocked_ = false;

      if (oldLocked != this.isLocked_)
        this.updateStateFromStatus_();
    },

    /**
     * Handles a chrome.runtime.onStartup event.
     * @private
     */
    handleStartup_: function() {
      // Nothing specific needs to be done here. This function exists solely to
      // be registered on the startup event.
    }
  };

  return {
    StateManager: StateManager
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('hotword', function() {
  'use strict';

  /**
   * Class used to manage speaker training. Starts a hotwording session
   * if training is on, and automatically restarts the detector when a
   * a hotword is triggered.
   * @param {!hotword.StateManager} stateManager
   * @constructor
   * @extends {hotword.BaseSessionManager}
   */
  function TrainingManager(stateManager) {
    /**
     * Chrome event listeners. Saved so that they can be de-registered when
     * hotwording is disabled.
     * @private
     */
    this.finalizedSpeakerModelListener_ =
        this.handleFinalizeSpeakerModel_.bind(this);

    hotword.BaseSessionManager.call(this,
                                    stateManager,
                                    hotword.constants.SessionSource.TRAINING);
  }

  /**
   * Handles a success event on mounting the file system event and deletes
   * the user data files.
   * @param {FileSystem} fs The FileSystem object.
   * @private
   */
  TrainingManager.deleteFiles_ = function(fs) {
    fs.root.getFile(hotword.constants.SPEAKER_MODEL_FILE_NAME, {create: false},
        TrainingManager.deleteFile_, TrainingManager.fileErrorHandler_);

    for (var i = 0; i < hotword.constants.NUM_TRAINING_UTTERANCES; ++i) {
      fs.root.getFile(hotword.constants.UTTERANCE_FILE_PREFIX + i +
          hotword.constants.UTTERANCE_FILE_EXTENSION,
          {create: false},
          TrainingManager.deleteFile_, TrainingManager.fileErrorHandler_);
    }
  };

  /**
   * Deletes a file.
   * @param {FileEntry} fileEntry The FileEntry object.
   * @private
   */
  TrainingManager.deleteFile_ = function(fileEntry) {
    if (fileEntry.isFile) {
      hotword.debug('File found: ' + fileEntry.fullPath);
      if (hotword.DEBUG || window.localStorage['hotword.DEBUG']) {
        fileEntry.getMetadata(function(md) {
          hotword.debug('File size: ' + md.size);
        });
      }
      fileEntry.remove(function() {
          hotword.debug('File removed: ' + fileEntry.fullPath);
      }, TrainingManager.fileErrorHandler_);
    }
  };

  /**
   * Handles a failure event on mounting the file system event.
   * @param {FileError} e The FileError object.
   * @private
   */
  TrainingManager.fileErrorHandler_ = function(e) {
      hotword.debug('File error: ' + e.code);
  };

  /**
   * Handles a failure event on checking for the existence of the speaker model.
   * @param {FileError} e The FileError object.
   * @private
   */
  TrainingManager.sendNoSpeakerModelResponse_ = function(e) {
    chrome.hotwordPrivate.speakerModelExistsResult(false);
  };

  /**
   * Handles a success event on mounting the file system and checks for the
   * existence of the speaker model.
   * @param {FileSystem} fs The FileSystem object.
   * @private
   */
  TrainingManager.speakerModelExists_ = function(fs) {
    fs.root.getFile(hotword.constants.SPEAKER_MODEL_FILE_NAME, {create: false},
        TrainingManager.sendSpeakerModelExistsResponse_,
        TrainingManager.sendNoSpeakerModelResponse_);
  };

  /**
   * Sends a response through the HotwordPrivateApi indicating whether
   * the speaker model exists.
   * @param {FileEntry} fileEntry The FileEntry object.
   * @private
   */
  TrainingManager.sendSpeakerModelExistsResponse_ = function(fileEntry) {
    if (fileEntry.isFile) {
      hotword.debug('File found: ' + fileEntry.fullPath);
      if (hotword.DEBUG || window.localStorage['hotword.DEBUG']) {
        fileEntry.getMetadata(function(md) {
          hotword.debug('File size: ' + md.size);
        });
      }
    }
    chrome.hotwordPrivate.speakerModelExistsResult(fileEntry.isFile);
  };

  /**
   * Handles a request to delete the speaker model.
   */
  TrainingManager.handleDeleteSpeakerModel = function() {
    window.webkitRequestFileSystem(PERSISTENT,
        hotword.constants.FILE_SYSTEM_SIZE_BYTES,
        TrainingManager.deleteFiles_,
        TrainingManager.fileErrorHandler_);
  };

  /**
   * Handles a request for the speaker model existence.
   */
  TrainingManager.handleSpeakerModelExists = function() {
    window.webkitRequestFileSystem(PERSISTENT,
        hotword.constants.FILE_SYSTEM_SIZE_BYTES,
        TrainingManager.speakerModelExists_,
        TrainingManager.fileErrorHandler_);
  };

  TrainingManager.prototype = {
    __proto__: hotword.BaseSessionManager.prototype,

    /** @override */
     enabled: function() {
       return this.stateManager.isTrainingEnabled();
     },

    /** @override */
    updateListeners: function() {
      hotword.BaseSessionManager.prototype.updateListeners.call(this);

      if (this.enabled()) {
        // Detect when the speaker model needs to be finalized.
        if (!chrome.hotwordPrivate.onFinalizeSpeakerModel.hasListener(
                this.finalizedSpeakerModelListener_)) {
          chrome.hotwordPrivate.onFinalizeSpeakerModel.addListener(
              this.finalizedSpeakerModelListener_);
        }
        this.startSession(hotword.constants.RecognizerStartMode.NEW_MODEL);
      } else {
        chrome.hotwordPrivate.onFinalizeSpeakerModel.removeListener(
            this.finalizedSpeakerModelListener_);
      }
    },

    /** @override */
    handleHotwordTrigger: function(log) {
      if (this.enabled()) {
        hotword.BaseSessionManager.prototype.handleHotwordTrigger.call(
            this, log);
        this.startSession(hotword.constants.RecognizerStartMode.ADAPT_MODEL);
      }
    },

    /** @override */
    startSession: function(opt_mode) {
      this.stateManager.startSession(
          this.sessionSource_,
          function() {
            chrome.hotwordPrivate.setHotwordSessionState(true, function() {});
          },
          this.handleHotwordTrigger.bind(this),
          this.handleSpeakerModelSaved_.bind(this),
          opt_mode);
    },

    /**
     * Handles a hotwordPrivate.onFinalizeSpeakerModel event.
     * @private
     */
    handleFinalizeSpeakerModel_: function() {
      if (this.enabled())
        this.stateManager.finalizeSpeakerModel();
    },

    /**
     * Handles a hotwordPrivate.onFinalizeSpeakerModel event.
     * @private
     */
    handleSpeakerModelSaved_: function() {
      if (this.enabled())
        chrome.hotwordPrivate.notifySpeakerModelSaved();
    },
  };

  return {
    TrainingManager: TrainingManager
  };
});
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
<meta charset="utf-8">
<link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
<link rel="stylesheet" href="chrome://resources/css/apps/common.css"></link>
<link rel="stylesheet" href="chrome://resources/css/apps/topbutton_bar.css"></link>
<style>/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */


html {
  height: 100%;
}

body {
  background-color: #fbfbfb;
  height: 100%;
  margin: 0;
  overflow: auto;
  padding: 0;
  width: 100%;
}

[hidden] {
  display: none !important;
}

.title-bar {
  -webkit-align-items: center;
  -webkit-app-region: drag;
  background-color: #fff;
  box-shadow: 0 1px #d0d0d0;
  color: rgb(80, 80, 82);
  display: -webkit-flex;
  font-size: 15px;
  height: 48px;
}

.title-bar #page-title {
  -webkit-flex: 1 1 auto;
  -webkit-margin-start: 20px;
}

.title-bar .button-bar {
  -webkit-flex: 0 1 auto;
}

.content {
  color: #646464;
  font-size: 12px;
  margin: 20px;
}

.content #description-text {
  border-color: #c8c8c8;
  box-sizing: border-box;
  height: 120px;
  line-height: 18px;
  padding: 10px;
  resize: none;
  width: 100%;
}

.content .text-field-container {
  -webkit-align-items: center;
  -webkit-padding-start: 10px;
  display: -webkit-flex;
  height: 29px;
  margin-top: 10px;
}

.content .text-field-container > label {
  -webkit-flex: 0 1 auto;
  width: 100px;
}

.content .text-field-container > input[type=text] {
  -webkit-flex: 1 1 auto;
  -webkit-padding-start: 5px;
  border: 1px solid;
  border-color: #c8c8c8;
  color: #585858;
  height: 100%;
}

.content .text-field-container > input[type=checkbox] {
  margin-right: 9px;
}

.content .checkbox-field-container {
  -webkit-align-items: center;
  display: -webkit-flex;
  height: 29px;
}

#screenshot-container {
  margin-top: 10px;
}

.content #screenshot-image {
  -webkit-margin-start: 100px;
  display: block;
  height: 60px;
  margin-top: 40px;
  transition: all 250ms ease;
}

.content #screenshot-image:hover {
  -webkit-margin-start: 80px;
  height: 125px;
  margin-top: 80px;
  z-index: 1;
}

.content #screenshot-image.wide-screen {
  height: auto;
  width: 100px;
}

.content #screenshot-image.wide-screen:hover {
  height: auto;
  width: 200px;
}

.content #privacy-note {
  color: #969696;
  font-size: 10px;
  line-height: 15px;
  margin-bottom: 20px;
  margin-top: 20px;
}

.content .buttons-pane {
  display: -webkit-flex;
  justify-content: flex-end
}

.content .remove-file-button {
  -webkit-margin-start: 5px;
  background-color: transparent;
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close.png) 2x);
  background-position: 50% 80%;
  background-repeat: no-repeat;
  border: none;
  height: 16px;
  pointer-events: auto;
  width: 16px;
}

.content .remove-file-button:hover {
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close_hover.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close_hover.png) 2x);
}

.content .remove-file-button:active {
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close_pressed.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close_pressed.png) 2x);
}

.content #attach-file-note {
  -webkit-margin-start: 112px;
  margin-bottom: 10px;
  margin-top: 10px;
}

.content .attach-file-notification {
  color: rgb(204, 0, 0);
  font-weight: bold;
}

button.white-button {
  -webkit-margin-end: 10px;
  color: #000;
}

button.blue-button {
  color: #fff;
  text-shadow: 1px sharp drop shadow rgb(45, 106, 218);
}
</style>

<script src="chrome://resources/js/load_time_data.js"></script>
<script src="chrome://resources/js/i18n_template_no_process.js"></script>
<script src="chrome://resources/js/util.js"></script>
<script src="../js/take_screenshot.js"></script>
<script src="../js/topbar_handlers.js"></script>
<script src="../js/feedback.js"></script>
</head>
<body>
  <div id="title-bar" class="title-bar">
    <span id="page-title" i18n-content="page-title"></span>
    <span class="topbutton-bar">
      <button class="minimize-button" id="minimize-button" tabindex="-1">
      </button>
      <button class="close-button" id="close-button" tabindex="-1">
      </button>
    </span>
  </div>
  <div id="content-pane" class="content">
    <textarea id="description-text" aria-labelledby="title-bar"></textarea>
    <div id="page-url" class="text-field-container">
      <label id="page-url-label" i18n-content="page-url"></label>
      <input id="page-url-text" aria-labelledby="page-url-label" type="text">
    </div>
    <!-- User e-mail -->
    <div id="user-email" class="text-field-container">
      <label id="user-email-label" i18n-content="user-email"></label>
      <input id="user-email-text" aria-labelledby="user-email-label" type="text">
    </div>
    <!-- Attach a file -->
    <div id="attach-file-container" class="text-field-container">
      <label id="attach-file-label" i18n-content="attach-file-label"></label>
      <input id="attach-file" type="file" aria-labelledby="attach-file-label">
      <div id="custom-file-container" hidden>
        <label id="attached-filename-text"></label>
        <button id="remove-attached-file" class="remove-file-button"></button>
      </div>
      <div id="attach-error" class="attach-file-notification"
          i18n-content="attach-file-to-big" hidden></div>
    </div>
    <div id="attach-file-note" i18n-content="attach-file-note"></div>
    <!-- Screenshot -->
    <div id="screenshot-container" class="checkbox-field-container">
      <input id="screenshot-checkbox" type="checkbox" aria-labelledby="screenshot-label">
      <label id="screenshot-label" i18n-content="screenshot"></label>
      <img id="screenshot-image" alt="screenshot">
    </div>
    <!-- System Information -->
    <div class="checkbox-field-container">
      <input id="sys-info-checkbox" type="checkbox" aria-labelledby="sys-info-label" checked>
      <label id="sys-info-label" i18n-values=".innerHTML:sys-info"></label>
      </span>
    </div>

    <!-- Privacy node -->
    <div id="privacy-note" i18n-values=".innerHTML:privacy-note"></div>
    <!-- Buttons -->
    <div class="buttons-pane">
      <button id="cancel-button" type="submit"
          class="white-button" i18n-content="cancel">
      </button>
      <button id="send-report-button" type="submit"
          class="blue-button" i18n-content="send-report">
      </button>
    </div>
  </div>
</body>
</html>
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
    <title i18n-content="sysinfoPageTitle"></title>
    <link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
    <style>/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#anchor {
  display: none;
}

body {
  font-size: 84%;
  margin: 0;
  min-width: 45em;
  padding: 0.75em;
}

.global-button {
  margin: 1px 3px 0 3px;
}

h1,
h2 {
  margin: 0;
}

h1 {
  color: rgb(74, 142, 230);
  font-size: 110%;
  font-weight: bold;
  padding: 0;
}

h2 {
  -webkit-padding-end: 1em;
  -webkit-padding-start: 0;
  color: rgb(58, 117, 189);
  display: inline-block;
  font-size: 110%;
  font-weight: normal;
}

#header {
  background: rgb(82, 150, 222);
  background-size: 100%;
  border: 1px solid rgb(58, 117, 189);
  border-radius: 6px;
  color: white;
  margin-bottom: 0.75em;
  overflow: hidden;
  padding: 0.5em 0;
  position: relative;
  text-shadow: 0 0 2px black;
}

html[dir='rtl'] #header {
  padding: 0.6em 0 0.75em 1em;
}

#header h1 {
  color: white;
  display: inline;
}

div#header h1::before {
  /* grit doesn't flatten -webkit-mask, so define the properties separately
  * for now. */
  -webkit-mask-image: url(data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSIyNHB4IiBoZWlnaHQ9IjI0cHgiIHZpZXdCb3g9IjAgMCA0OCA0OCIgZmlsbD0iIzAwMDAwMCI+CiAgICA8cGF0aCBkPSJNMCAwaDQ4djQ4SDB6IiBmaWxsPSJub25lIi8+CiAgICA8cGF0aCBkPSJNMzguODYgMjUuOTVjLjA4LS42NC4xNC0xLjI5LjE0LTEuOTVzLS4wNi0xLjMxLS4xNC0xLjk1bDQuMjMtMy4zMWMuMzgtLjMuNDktLjg0LjI0LTEuMjhsLTQtNi45M2MtLjI1LS40My0uNzctLjYxLTEuMjItLjQzbC00Ljk4IDIuMDFjLTEuMDMtLjc5LTIuMTYtMS40Ni0zLjM4LTEuOTdMMjkgNC44NGMtLjA5LS40Ny0uNS0uODQtMS0uODRoLThjLS41IDAtLjkxLjM3LS45OS44NGwtLjc1IDUuM2MtMS4yMi41MS0yLjM1IDEuMTctMy4zOCAxLjk3TDkuOSAxMC4xYy0uNDUtLjE3LS45NyAwLTEuMjIuNDNsLTQgNi45M2MtLjI1LjQzLS4xNC45Ny4yNCAxLjI4bDQuMjIgMy4zMUM5LjA2IDIyLjY5IDkgMjMuMzQgOSAyNHMuMDYgMS4zMS4xNCAxLjk1bC00LjIyIDMuMzFjLS4zOC4zLS40OS44NC0uMjQgMS4yOGw0IDYuOTNjLjI1LjQzLjc3LjYxIDEuMjIuNDNsNC45OC0yLjAxYzEuMDMuNzkgMi4xNiAxLjQ2IDMuMzggMS45N2wuNzUgNS4zYy4wOC40Ny40OS44NC45OS44NGg4Yy41IDAgLjkxLS4zNy45OS0uODRsLjc1LTUuM2MxLjIyLS41MSAyLjM1LTEuMTcgMy4zOC0xLjk3bDQuOTggMi4wMWMuNDUuMTcuOTcgMCAxLjIyLS40M2w0LTYuOTNjLjI1LS40My4xNC0uOTctLjI0LTEuMjhsLTQuMjItMy4zMXpNMjQgMzFjLTMuODcgMC03LTMuMTMtNy03czMuMTMtNyA3LTcgNyAzLjEzIDcgNy0zLjEzIDctNyA3eiIvPgo8L3N2Zz4K);
  -webkit-mask-position: center;
  -webkit-mask-repeat: no-repeat;
  -webkit-mask-size: 24px;
  background-color: white;
  content: '';
  display: inline-block;
  height: 20px;
  vertical-align: middle;
  width: 37px;
}

#header p {
  -webkit-padding-start: 0.4em;
  color: white;
  display: inline;
  font-size: 84%;
  font-style: italic;
}

.list {
  border-collapse: collapse;
  font-size: 84%;
  line-height: 200%;
  width: 100%;
}

.list:not(.filtered) tr:nth-child(odd) td {
  background: rgb(239, 243, 255);
}

.list td {
  font-family: 'Courier New', monospace;
  line-height: 1.4em;
  padding: 0 0.5em;
  padding-top: 0.35em;
  vertical-align: top;
}

.list tr td:nth-last-child(1),
.list tr th:nth-last-child(1) {
  -webkit-padding-end: 1em;
}

.list:not(.filtered) .tab .name {
  -webkit-padding-start: 1.5em;
}

.list .name {
  width: 20%;
}

.list .button-cell {
  width: 7%;
}

.list .name div {
  height: 1.6em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.button-hidden {
  display: none;
}

.number-expanded,
.number-collapsed  {
  text-align: left;
  text-overflow: ellipsis;
  width: 80%;
}

html[dir='rtl'] .number-expanded,
html[dir='rtl'] .number-collapsed  {
  text-align: right;
}

tr > *:nth-child(1),
tr > *:nth-child(2) {
  -webkit-border-end: 1px solid rgb(181, 198, 222);
}

.name {
  background-position: 5em center;
  background-repeat: no-repeat;
}

.stat-value {
  text-overflow: ellipsis;
  white-space: pre-wrap;
}

html[dir='rtl'] #details .name {
  background-position-left: auto;
  background-position-right: 5em;
}

.number-collapsed .stat-value {
  display: none;
}

.number-expanded .stat-value {
  display: auto;
}

#status {
  color: rgb(255, 0, 0);
  margin: .5em 0;
}
</style>
    <script src="chrome://resources/js/util.js"></script>
    <script src="chrome://resources/js/i18n_template_no_process.js"></script>
    <script src="chrome://resources/js/jstemplate_compiled.js"></script>
    <script src="../js/sys_info.js"></script>
    <style>
      html, body {
        overflow: visible;
      }
    </style>
  </head>
  <body>
    <div id="header">
      <h1 id="title" i18n-content="sysinfoPageTitle"></h1>
      <p id="description" i18n-content="sysinfoPageDescription"></p>
    </div>
    <div id="content">
      <h2 id="tableTitle" i18n-content="sysinfoPageTableTitle"></h2>
      <div id="anchor"></div>
      <button id="expandAllBtn" class="global-button" i18n-content="sysinfoPageExpandAllBtn"></button>
      <button id="collapseAllBtn" class="global-button" i18n-content="sysinfoPageCollapseAllBtn"></button>
      <p id="status" i18n-content="sysinfoPageStatusLoading"></p>
      <table class="list" id="detailsTable"></table>
    </div>
  </body>
</html>// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @type {number}
 * @const
 */
var FEEDBACK_WIDTH = 500;
/**
 * @type {number}
 * @const
 */
var FEEDBACK_HEIGHT = 585;

/**
 * @type {string}
 * @const
 */
var FEEDBACK_DEFAULT_WINDOW_ID = 'default_window';

// To generate a hashed extension ID, use a sha-256 hash, all in lower case.
// Example:
//   echo -n 'abcdefghijklmnopqrstuvwxyzabcdef' | sha1sum | \
//       awk '{print toupper($1)}'
var whitelistedExtensionIds = [
  '12E618C3C6E97495AAECF2AC12DEB082353241C6', // QuickOffice
  '3727DD3E564B6055387425027AD74C58784ACC15', // QuickOffice
  '2FC374607C2DF285634B67C64A2E356C607091C3', // QuickOffice
  '2843C1E82A9B6C6FB49308FDDF4E157B6B44BC2B', // G+ Photos
  '5B5DA6D054D10DB917AF7D9EAE3C56044D1B0B03', // G+ Photos
  '986913085E3E3C3AFDE9B7A943149C4D3F4C937B', // Feedback Extension
  '7AE714FFD394E073F0294CFA134C9F91DB5FBAA4', // Connectivity Diagnostics
  'C7DA3A55C2355F994D3FDDAD120B426A0DF63843', // Connectivity Diagnostics
  '75E3CFFFC530582C583E4690EF97C70B9C8423B7', // Connectivity Diagnostics
  '32A1BA997F8AB8DE29ED1BA94AAF00CF2A3FEFA7', // Connectivity Diagnostics
  'A291B26E088FA6BA53FFD72F0916F06EBA7C585A', // Chrome OS Recovery Tool
  'D7986543275120831B39EF28D1327552FC343960', // Chrome OS Recovery Tool
  '8EBDF73405D0B84CEABB8C7513C9B9FA9F1DC2CE', // GetHelp app.
  '97B23E01B2AA064E8332EE43A7A85C628AADC3F2', // Chrome Remote Desktop Dev
  '9E527CDA9D7C50844E8A5DB964A54A640AE48F98', // Chrome Remote Desktop Stable
  'DF52618D0B040D8A054D8348D2E84DDEEE5974E7', // Chrome Remote Desktop QA
  '269D721F163E587BC53C6F83553BF9CE2BB143CD', // Chrome Remote Desktop QA backup
  'C449A798C495E6CF7D6AF10162113D564E67AD12', // Chrome Remote Desktop Apps V2
  '981974CD1832B87BE6B21BE78F7249BB501E0DE6', // Play Movies Dev
  '32FD7A816E47392C92D447707A89EB07EEDE6FF7', // Play Movies Nightly
  '3F3CEC4B9B2B5DC2F820CE917AABDF97DB2F5B49', // Play Movies Beta
  'F92FAC70AB68E1778BF62D9194C25979596AA0E6', // Play Movies Stable
  '0F585FB1D0FDFBEBCE1FEB5E9DFFB6DA476B8C9B', // Hangouts Extension
  '2D22CDB6583FD0A13758AEBE8B15E45208B4E9A7', // Hangouts Extension
  '49DA0B9CCEEA299186C6E7226FD66922D57543DC', // Hangouts Extension
  'E7E2461CE072DF036CF9592740196159E2D7C089', // Hangouts Extension
  'A74A4D44C7CFCD8844830E6140C8D763E12DD8F3', // Hangouts Extension
  '312745D9BF916161191143F6490085EEA0434997', // Hangouts Extension
  '53041A2FA309EECED01FFC751E7399186E860B2C', // Hangouts Extension
  '0F42756099D914A026DADFA182871C015735DD95', // Hangouts Extension
  '1B7734733E207CCE5C33BFAA544CA89634BF881F', // GLS nightly
  'E2ACA3D943A3C96310523BCDFD8C3AF68387E6B7', // GLS stable
  '11B478CEC461C766A2DC1E5BEEB7970AE06DC9C2', // http://crbug.com/463552
  '0EFB879311E9EFBB7C45251F89EC655711B1F6ED', // http://crbug.com/463552
  '9193D3A51E2FE33B496CDA53EA330423166E7F02', // http://crbug.com/463552
  'F9119B8B18C7C82B51E7BC6FF816B694F2EC3E89', // http://crbug.com/463552
  'BA007D8D52CC0E2632EFCA03ACD003B0F613FD71', // http://crbug.com/470411
  '5260FA31DE2007A837B7F7B0EB4A47CE477018C8', // http://crbug.com/470411
  '4F4A25F31413D9B9F80E61D096DEB09082515267', // http://crbug.com/470411
  'FBA0DE4D3EFB5485FC03760F01F821466907A743', // http://crbug.com/470411
  'E216473E4D15C5FB14522D32C5F8DEAAB2CECDC6', // http://crbug.com/470411
  '676A08383D875E51CE4C2308D875AE77199F1413', // http://crbug.com/473845
  '869A23E11B308AF45A68CC386C36AADA4BE44A01', // http://crbug.com/473845
  'E9CE07C7EDEFE70B9857B312E88F94EC49FCC30F', // http://crbug.com/473845
  'A4577D8C2AF4CF26F40CBCA83FFA4251D6F6C8F8', // http://crbug.com/478929
  'A8208CCC87F8261AFAEB6B85D5E8D47372DDEA6B', // http://crbug.com/478929
  'B620CF4203315F9F2E046EDED22C7571A935958D', // http://crbug.com/510270
  'B206D8716769728278D2D300349C6CB7D7DE2EF9', // http://crbug.com/510270
  'EFCF5358672FEE04789FD2EC3638A67ADEDB6C8C', // http://crbug.com/514696
  'FAD85BC419FE00995D196312F53448265EFA86F1', // http://crbug.com/516527
  'F33B037DEDA65F226B7409C2ADB0CF3F8565AB03', // http://crbug.com/541769
  '969C788BCBC82FBBE04A17360CA165C23A419257', // http://crbug.com/541769
  '3BC3740BFC58F06088B300274B4CFBEA20136342', // http://crbug.com/541769
];

/**
 * Used to generate unique IDs for FeedbackRequest objects.
 * @type {number}
 */
var lastUsedId = 0;

/**
 * A FeedbackRequest object represents a unique feedback report, requested by an
 * instance of the feedback window. It contains the system information specific
 * to this report, the full feedbackInfo, and callbacks to send the report upon
 * request.
 */
class FeedbackRequest {
  constructor(feedbackInfo) {
    this.id_ = ++lastUsedId;
    this.feedbackInfo_ = feedbackInfo;
    this.onSystemInfoReadyCallback_ = null;
    this.isSystemInfoReady_ = false;
    this.reportIsBeingSent_ = false;
    this.isRequestCanceled_ = false;
    this.useSystemInfo_ = false;
  }

  /**
   * Called when the system information is sent from the C++ side.
   * @param {Object} sysInfo The received system information.
   */
  getSystemInformationCallback(sysInfo) {
    if (this.isRequestCanceled_) {
      // If the window had been closed before the system information was
      // received, we skip the rest of the operations and return immediately.
      return;
    }

    this.isSystemInfoReady_ = true;

    // Combine the newly received system information with whatever system
    // information we have in the feedback info (if any).
    if (this.feedbackInfo_.systemInformation) {
      this.feedbackInfo_.systemInformation =
          this.feedbackInfo_.systemInformation.concat(sysInfo);
    } else {
      this.feedbackInfo_.systemInformation = sysInfo;
    }

    if (this.onSystemInfoReadyCallback_ != null) {
      this.onSystemInfoReadyCallback_();
      this.onSystemInfoReadyCallback_ = null;
    }
  }

  /**
   * Retrieves the system information for this request object.
   * @param {function()} callback Invoked to notify the listener that the system
   * information has been received.
   */
  getSystemInformation(callback) {
    if (this.isSystemInfoReady_) {
      callback();
      return;
    }

    this.onSystemInfoReadyCallback_ = callback;
    // The C++ side must reply to the callback specific to this object.
    var boundCallback = this.getSystemInformationCallback.bind(this);
    chrome.feedbackPrivate.getSystemInformation(boundCallback);
  }

  /**
   * Sends the feedback report represented by the object, either now if system
   * information is ready, or later once it is.
   * @param {boolean} useSystemInfo True if the user would like the system
   * information to be sent with the report.
   */
  sendReport(useSystemInfo) {
    this.reportIsBeingSent_ = true;
    this.useSystemInfo_ = useSystemInfo;
    if (useSystemInfo && !this.isSystemInfoReady_) {
      this.onSystemInfoReadyCallback_ = this.sendReportNow;
      return;
    }

    this.sendReportNow();
  }

  /**
   * Sends the report immediately and removes this object once the report is
   * sent.
   */
  sendReportNow() {
    if (!this.useSystemInfo_) {
      // Clear the system information if the user doesn't want it to be sent.
      this.feedbackInfo_.systemInformation = null;
    }

    /** @const */ var ID = this.id_;
    chrome.feedbackPrivate.sendFeedback(this.feedbackInfo_,
        function(result) {
          console.log('Feedback: Report sent for request with ID ' + ID);
        });
  }

  /**
   * Handles the event when the feedback UI window corresponding to this
   * FeedbackRequest instance is closed.
   */
  onWindowClosed() {
    if (!this.reportIsBeingSent_)
      this.isRequestCanceled_ = true;
  }
};

/**
 * Function to determine whether or not a given extension id is whitelisted to
 * invoke the feedback UI. If the extension is whitelisted, the callback to
 * start the Feedback UI will be called.
 * @param {string} id the id of the sender extension.
 * @param {Function} startFeedbackCallback The callback function that will
 *     will start the feedback UI.
 * @param {Object} feedbackInfo The feedback info object to pass to the
 *     start feedback UI callback.
 */
function senderWhitelisted(id, startFeedbackCallback, feedbackInfo) {
  crypto.subtle.digest('SHA-1', new TextEncoder().encode(id)).then(
      function(hashBuffer) {
    var hashString = '';
    var hashView = new Uint8Array(hashBuffer);
    for (var i = 0; i < hashView.length; ++i) {
      var n = hashView[i];
      hashString += n < 0x10 ? '0' : '';
      hashString += n.toString(16);
    }
    if (whitelistedExtensionIds.indexOf(hashString.toUpperCase()) != -1)
      startFeedbackCallback(feedbackInfo);
  });
}

/**
 * Callback which gets notified once our feedback UI has loaded and is ready to
 * receive its initial feedback info object.
 * @param {Object} request The message request object.
 * @param {Object} sender The sender of the message.
 * @param {function(Object)} sendResponse Callback for sending a response.
 */
function feedbackReadyHandler(request, sender, sendResponse) {
  if (request.ready)
    chrome.runtime.sendMessage({sentFromEventPage: true});
}

/**
 * Callback which gets notified if another extension is requesting feedback.
 * @param {Object} request The message request object.
 * @param {Object} sender The sender of the message.
 * @param {function(Object)} sendResponse Callback for sending a response.
 */
function requestFeedbackHandler(request, sender, sendResponse) {
  if (request.requestFeedback)
    senderWhitelisted(sender.id, startFeedbackUI, request.feedbackInfo);
}

/**
 * Callback which starts up the feedback UI.
 * @param {Object} feedbackInfo Object containing any initial feedback info.
 */
function startFeedbackUI(feedbackInfo) {
  var win = chrome.app.window.get(FEEDBACK_DEFAULT_WINDOW_ID);
  if (win) {
    win.show();
    return;
  }
  chrome.app.window.create('html/default.html', {
      frame: 'none',
      id: FEEDBACK_DEFAULT_WINDOW_ID,
      width: FEEDBACK_WIDTH,
      height: FEEDBACK_HEIGHT,
      hidden: true,
      resizable: false },
      function(appWindow) {
        var request = new FeedbackRequest(feedbackInfo);

        // The feedbackInfo member of the new window should refer to the one in
        // its corresponding FeedbackRequest object to avoid copying and
        // duplicatations.
        appWindow.contentWindow.feedbackInfo = request.feedbackInfo_;

        // Define some functions for the new window so that it can call back
        // into here.

        // Define a function for the new window to get the system information.
        appWindow.contentWindow.getSystemInformation = function(callback) {
          request.getSystemInformation(callback);
        };

        // Define a function to request sending the feedback report.
        appWindow.contentWindow.sendFeedbackReport = function(useSystemInfo) {
          request.sendReport(useSystemInfo);
        };

        // Observe when the window is closed.
        appWindow.onClosed.addListener(function() {
          request.onWindowClosed();
        });
      });
}

chrome.runtime.onMessage.addListener(feedbackReadyHandler);
chrome.runtime.onMessageExternal.addListener(requestFeedbackHandler);
chrome.feedbackPrivate.onFeedbackRequested.addListener(startFeedbackUI);
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/** @type {string}
 * @const
 */
var FEEDBACK_LANDING_PAGE =
    'https://support.google.com/chrome/go/feedback_confirmation';

/** @type {number}
 * @const
 */
var MAX_ATTACH_FILE_SIZE = 3 * 1024 * 1024;

/**
 * @type {number}
 * @const
 */
var FEEDBACK_MIN_WIDTH = 500;

/**
 * @type {number}
 * @const
 */
var FEEDBACK_MIN_HEIGHT = 585;

/**
 * @type {number}
 * @const
 */
var FEEDBACK_MIN_HEIGHT_LOGIN = 482;

/** @type {number}
 * @const
 */
var CONTENT_MARGIN_HEIGHT = 40;

/** @type {number}
 * @const
 */
var MAX_SCREENSHOT_WIDTH = 100;

/** @type {string}
 * @const
 */
var SYSINFO_WINDOW_ID = 'sysinfo_window';

/** @type {string}
 * @const
 */
var STATS_WINDOW_ID = 'stats_window';

/**
 * Feedback flow defined in feedback_private.idl.
 * @enum {string}
 */
var FeedbackFlow = {
  REGULAR: 'regular',  // Flow in a regular user session.
  LOGIN: 'login'       // Flow on the login screen.
};

var attachedFileBlob = null;
var lastReader = null;

/**
 * Determines whether the system information associated with this instance of
 * the feedback window has been received.
 * @type {boolean}
 */
var isSystemInfoReady = false;

/**
 * The callback used by the sys_info_page to receive the event that the system
 * information is ready.
 * @type {function(sysInfo)}
 */
var sysInfoPageOnSysInfoReadyCallback = null;

/**
 * Reads the selected file when the user selects a file.
 * @param {Event} fileSelectedEvent The onChanged event for the file input box.
 */
function onFileSelected(fileSelectedEvent) {
  $('attach-error').hidden = true;
  var file = fileSelectedEvent.target.files[0];
  if (!file) {
    // User canceled file selection.
    attachedFileBlob = null;
    return;
  }

  if (file.size > MAX_ATTACH_FILE_SIZE) {
    $('attach-error').hidden = false;

    // Clear our selected file.
    $('attach-file').value = '';
    attachedFileBlob = null;
    return;
  }

  attachedFileBlob = file.slice();
}

/**
 * Clears the file that was attached to the report with the initial request.
 * Instead we will now show the attach file button in case the user wants to
 * attach another file.
 */
function clearAttachedFile() {
  $('custom-file-container').hidden = true;
  attachedFileBlob = null;
  feedbackInfo.attachedFile = null;
  $('attach-file').hidden = false;
}

/**
 * Creates a closure that creates or shows a window with the given url.
 * @param {string} windowId A string with the ID of the window we are opening.
 * @param {string} url The destination URL of the new window.
 * @return {function()} A function to be called to open the window.
 */
function windowOpener(windowId, url) {
  return function(e) {
    e.preventDefault();
    chrome.app.window.create(url, {id: windowId});
  };
}

/**
 * Opens a new window with chrome://slow_trace, downloading performance data.
 */
function openSlowTraceWindow() {
  chrome.app.window.create(
      'chrome://slow_trace/tracing.zip#' + feedbackInfo.traceId);
}

/**
 * Sends the report; after the report is sent, we need to be redirected to
 * the landing page, but we shouldn't be able to navigate back, hence
 * we open the landing page in a new tab and sendReport closes this tab.
 * @return {boolean} True if the report was sent.
 */
function sendReport() {
  if ($('description-text').value.length == 0) {
    var description = $('description-text');
    description.placeholder = loadTimeData.getString('no-description');
    description.focus();
    return false;
  }

  // Prevent double clicking from sending additional reports.
  $('send-report-button').disabled = true;
  console.log('Feedback: Sending report');
  if (!feedbackInfo.attachedFile && attachedFileBlob) {
    feedbackInfo.attachedFile = { name: $('attach-file').value,
                                  data: attachedFileBlob };
  }

  feedbackInfo.description = $('description-text').value;
  feedbackInfo.pageUrl = $('page-url-text').value;
  feedbackInfo.email = $('user-email-text').value;

  var useSystemInfo = false;
  var useHistograms = false;
  if ($('sys-info-checkbox') != null &&
      $('sys-info-checkbox').checked) {
    // Send histograms along with system info.
    useSystemInfo = useHistograms = true;
  }


  feedbackInfo.sendHistograms = useHistograms;

  // If the user doesn't want to send the screenshot.
  if (!$('screenshot-checkbox').checked)
    feedbackInfo.screenshot = null;

  // Request sending the report, show the landing page (if allowed), and close
  // this window right away. The FeedbackRequest object that represents this
  // report will take care of sending the report in the background.
  sendFeedbackReport(useSystemInfo);
  if (feedbackInfo.flow != FeedbackFlow.LOGIN)
    window.open(FEEDBACK_LANDING_PAGE, '_blank');
  window.close();
  return true;
}

/**
 * Click listener for the cancel button.
 * @param {Event} e The click event being handled.
 */
function cancel(e) {
  e.preventDefault();
  window.close();
}

/**
 * Converts a blob data URL to a blob object.
 * @param {string} url The data URL to convert.
 * @return {Blob} Blob object containing the data.
 */
function dataUrlToBlob(url) {
  var mimeString = url.split(',')[0].split(':')[1].split(';')[0];
  var data = atob(url.split(',')[1]);
  var dataArray = [];
  for (var i = 0; i < data.length; ++i)
    dataArray.push(data.charCodeAt(i));

  return new Blob([new Uint8Array(dataArray)], {type: mimeString});
}



function resizeAppWindow() {
  // We pick the width from the titlebar, which has no margins.
  var width = $('title-bar').scrollWidth;
  if (width < FEEDBACK_MIN_WIDTH)
    width = FEEDBACK_MIN_WIDTH;

  // We get the height by adding the titlebar height and the content height +
  // margins. We can't get the margins for the content-pane here by using
  // style.margin - the variable seems to not exist.
  var height = $('title-bar').scrollHeight +
      $('content-pane').scrollHeight + CONTENT_MARGIN_HEIGHT;

  var minHeight = FEEDBACK_MIN_HEIGHT;
  if (feedbackInfo.flow == FeedbackFlow.LOGIN)
    minHeight = FEEDBACK_MIN_HEIGHT_LOGIN;
  height = Math.max(height, minHeight);

  chrome.app.window.current().resizeTo(width, height);
}

/**
 * A callback to be invoked when the background page of this extension receives
 * the system information.
 */
function onSystemInformation() {
  isSystemInfoReady = true;
  // In case the sys_info_page needs to be notified by this event, do so.
  if (sysInfoPageOnSysInfoReadyCallback != null) {
    sysInfoPageOnSysInfoReadyCallback(feedbackInfo.systemInformation);
    sysInfoPageOnSysInfoReadyCallback = null;
  }
}

/**
 * Initializes our page.
 * Flow:
 * .) DOMContent Loaded        -> . Request feedbackInfo object
 *                                . Setup page event handlers
 * .) Feedback Object Received -> . take screenshot
 *                                . request email
 *                                . request System info
 *                                . request i18n strings
 * .) Screenshot taken         -> . Show Feedback window.
 */
function initialize() {
  // Add listener to receive the feedback info object.
  chrome.runtime.onMessage.addListener(function(request, sender, sendResponse) {
    if (request.sentFromEventPage) {
      if (!feedbackInfo.flow)
        feedbackInfo.flow = FeedbackFlow.REGULAR;

      $('description-text').textContent = feedbackInfo.description;
      if (feedbackInfo.pageUrl)
        $('page-url-text').value = feedbackInfo.pageUrl;

      takeScreenshot(function(screenshotCanvas) {
        // We've taken our screenshot, show the feedback page without any
        // further delay.
        window.webkitRequestAnimationFrame(function() {
          resizeAppWindow();
        });
        chrome.app.window.current().show();

        var screenshotDataUrl = screenshotCanvas.toDataURL('image/png');
        $('screenshot-image').src = screenshotDataUrl;
        $('screenshot-image').classList.toggle('wide-screen',
            $('screenshot-image').width > MAX_SCREENSHOT_WIDTH);
        feedbackInfo.screenshot = dataUrlToBlob(screenshotDataUrl);
      });

      chrome.feedbackPrivate.getUserEmail(function(email) {
        $('user-email-text').value = email;
      });

      // Initiate getting the system info.
      isSystemInfoReady = false;
      getSystemInformation(onSystemInformation);

      // An extension called us with an attached file.
      if (feedbackInfo.attachedFile) {
        $('attached-filename-text').textContent =
            feedbackInfo.attachedFile.name;
        attachedFileBlob = feedbackInfo.attachedFile.data;
        $('custom-file-container').hidden = false;
        $('attach-file').hidden = true;
      }

      // No URL and file attachment for login screen feedback.
      if (feedbackInfo.flow == FeedbackFlow.LOGIN) {
        $('page-url').hidden = true;
        $('attach-file-container').hidden = true;
        $('attach-file-note').hidden = true;
      }



      chrome.feedbackPrivate.getStrings(function(strings) {
        loadTimeData.data = strings;
        i18nTemplate.process(document, loadTimeData);

        if ($('sys-info-url')) {
          // Opens a new window showing the full anonymized system+app
          // information.
          $('sys-info-url').onclick = function() {
            var win = chrome.app.window.get(SYSINFO_WINDOW_ID);
            if (win) {
              win.show();
              return;
            }
            chrome.app.window.create(
              '/html/sys_info.html', {
                frame: 'chrome',
                id: SYSINFO_WINDOW_ID,
                width: 600,
                height: 400,
                hidden: false,
                resizable: true
              }, function(appWindow) {
                // Define functions for the newly created window.

                // Gets the full system information for the new window.
                appWindow.contentWindow.getFullSystemInfo =
                    function(callback) {
                      if (isSystemInfoReady) {
                        callback(feedbackInfo.systemInformation);
                        return;
                      }

                      sysInfoPageOnSysInfoReadyCallback = callback;
                    };

                // Returns the loadTimeData for the new window.
                appWindow.contentWindow.getLoadTimeData = function() {
                  return loadTimeData;
                };
            });
          };
        }
        if ($('histograms-url')) {
          // Opens a new window showing the histogram metrics.
          $('histograms-url').onclick =
              windowOpener(STATS_WINDOW_ID, 'chrome://histograms');
        }
        // Make sure our focus starts on the description field.
        $('description-text').focus();
      });
    }
  });

  window.addEventListener('DOMContentLoaded', function() {
    // Ready to receive the feedback object.
    chrome.runtime.sendMessage({ready: true});

    // Setup our event handlers.
    $('attach-file').addEventListener('change', onFileSelected);
    $('send-report-button').onclick = sendReport;
    $('cancel-button').onclick = cancel;
    $('remove-attached-file').onclick = clearAttachedFile;

  });
}

initialize();
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * The global load time data that contains the localized strings that we will
 * get from the main page when this page first loads.
 */
var loadTimeData = null;

function getValueDivForButton(button) {
  return $(button.id.substr(0, button.id.length - 4));
}

function getButtonForValueDiv(valueDiv) {
  return $(valueDiv.id + '-btn');
}

/**
 * Toggles whether an item is collapsed or expanded.
 */
function changeCollapsedStatus() {
  var valueDiv = getValueDivForButton(this);
  if (valueDiv.parentNode.className == 'number-collapsed') {
    valueDiv.parentNode.className = 'number-expanded';
    this.textContent = loadTimeData.getString('sysinfoPageCollapseBtn');
  } else {
    valueDiv.parentNode.className = 'number-collapsed';
    this.textContent = loadTimeData.getString('sysinfoPageExpandBtn');
  }
}

/**
 * Collapses all log items.
 */
function collapseAll() {
  var valueDivs = document.getElementsByClassName('stat-value');
  for (var i = 0; i < valueDivs.length; i++) {
    var button = getButtonForValueDiv(valueDivs[i]);
    if (button && button.className != 'button-hidden') {
      button.textContent = loadTimeData.getString('sysinfoPageExpandBtn');
      valueDivs[i].parentNode.className = 'number-collapsed';
    }
  }
}

/**
 * Expands all log items.
 */
function expandAll() {
  var valueDivs = document.getElementsByClassName('stat-value');
  for (var i = 0; i < valueDivs.length; i++) {
    var button = getButtonForValueDiv(valueDivs[i]);
    if (button && button.className != 'button-hidden') {
      button.textContent = loadTimeData.getString('sysinfoPageCollapseBtn');
      valueDivs[i].parentNode.className = 'number-expanded';
    }
  }
}

/**
 * Collapse only those log items with multi-line values.
 */
function collapseMultiLineStrings() {
  var valueDivs = document.getElementsByClassName('stat-value');
  var nameDivs = document.getElementsByClassName('stat-name');
  for (var i = 0; i < valueDivs.length; i++) {
    var button = getButtonForValueDiv(valueDivs[i]);
    button.onclick = changeCollapsedStatus;
    if (valueDivs[i].scrollHeight > (nameDivs[i].scrollHeight * 2)) {
      button.className = '';
      button.textContent = loadTimeData.getString('sysinfoPageExpandBtn');
      valueDivs[i].parentNode.className = 'number-collapsed';
    } else {
      button.className = 'button-hidden';
      valueDivs[i].parentNode.className = 'number';
    }
  }
}

function createNameCell(key) {
  var nameCell = document.createElement('td');
  nameCell.setAttribute('class', 'name');
  var nameDiv = document.createElement('div');
  nameDiv.setAttribute('class', 'stat-name');
  nameDiv.appendChild(document.createTextNode(key));
  nameCell.appendChild(nameDiv);
  return nameCell;
}

function createButtonCell(key) {
  var buttonCell = document.createElement('td');
  buttonCell.setAttribute('class', 'button-cell');
  var button = document.createElement('button');
  button.setAttribute('id', '' + key + '-value-btn');
  buttonCell.appendChild(button);
  return buttonCell;
}

function createValueCell(key, value) {
  var valueCell = document.createElement('td');
  var valueDiv = document.createElement('div');
  valueDiv.setAttribute('class', 'stat-value');
  valueDiv.setAttribute('id', '' + key + '-value');
  valueDiv.appendChild(document.createTextNode(value));
  valueCell.appendChild(valueDiv);
  return valueCell;
}

function createTableRow(key, value) {
  var row = document.createElement('tr');
  row.appendChild(createNameCell(key));
  row.appendChild(createButtonCell(key));
  row.appendChild(createValueCell(key, value));
  return row;
}

/**
 * Builds the system information table row by row.
 * @param {Element} table The DOM table element to which the newly created rows
 * will be appended.
 * @param {Object} systemInfo The system information that will be used to fill
 * the table.
 */
function createTable(table, systemInfo) {
  for (var key in systemInfo) {
    var item = systemInfo[key];
    table.appendChild(createTableRow(item['key'], item['value']));
  }
}

/**
 * The callback which will be invoked by the parent window, when the system
 * information is ready.
 * @param {Object} systemInfo The system information that will be used to fill
 * the table.
 */
function onSysInfoReady(systemInfo) {
  createTable($('detailsTable'), systemInfo);

  $('collapseAllBtn').onclick = collapseAll;
  $('expandAllBtn').onclick = expandAll;

  collapseMultiLineStrings();

  $('status').textContent = '';
}

/**
 * Initializes the page when the window is loaded.
 */
window.onload = function() {
  loadTimeData = getLoadTimeData();
  i18nTemplate.process(document, loadTimeData);
  getFullSystemInfo(onSysInfoReady);
};
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Function to take the screenshot of the current screen.
 * @param {function(HTMLCanvasElement)} callback Callback for returning the
 *                                      canvas with the screenshot on it.
 */
function takeScreenshot(callback) {
  var screenshotStream = null;
  var video = document.createElement('video');

  video.addEventListener('canplay', function(e) {
    if (screenshotStream) {
      var canvas = document.createElement('canvas');
      canvas.setAttribute('width', video.videoWidth);
      canvas.setAttribute('height', video.videoHeight);
      canvas.getContext('2d').drawImage(
          video, 0, 0, video.videoWidth, video.videoHeight);

      video.pause();
      video.src = '';

      screenshotStream.getVideoTracks()[0].stop();
      screenshotStream = null;

      callback(canvas);
    }
  }, false);

  navigator.webkitGetUserMedia(
    {
      video: {
        mandatory: {
          chromeMediaSource: 'screen',
          maxWidth: 4096,
          maxHeight: 2560
        }
      }
    },
    function(stream) {
      if (stream) {
        screenshotStream = stream;
        video.src = window.URL.createObjectURL(screenshotStream);
        video.play();
      }
    },
    function(err) {
      console.error('takeScreenshot failed: ' +
          err.name + '; ' + err.message + '; ' + err.constraintName);
    }
  );
}
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Setup handlers for the minimize and close topbar buttons.
 */
function initializeHandlers() {
  $('minimize-button').addEventListener('click', function(e) {
    e.preventDefault();
    chrome.app.window.current().minimize();
  });

  $('minimize-button').addEventListener('mousedown', function(e) {
    e.preventDefault();
  });

  $('close-button').addEventListener('click', function() {
    window.close();
  });

  $('close-button').addEventListener('mousedown', function(e) {
    e.preventDefault();
  });
}

window.addEventListener('DOMContentLoaded', initializeHandlers);
/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */


html {
  height: 100%;
}

body {
  background-color: #fbfbfb;
  height: 100%;
  margin: 0;
  overflow: auto;
  padding: 0;
  width: 100%;
}

[hidden] {
  display: none !important;
}

.title-bar {
  -webkit-align-items: center;
  -webkit-app-region: drag;
  background-color: #fff;
  box-shadow: 0 1px #d0d0d0;
  color: rgb(80, 80, 82);
  display: -webkit-flex;
  font-size: 15px;
  height: 48px;
}

.title-bar #page-title {
  -webkit-flex: 1 1 auto;
  -webkit-margin-start: 20px;
}

.title-bar .button-bar {
  -webkit-flex: 0 1 auto;
}

.content {
  color: #646464;
  font-size: 12px;
  margin: 20px;
}

.content #description-text {
  border-color: #c8c8c8;
  box-sizing: border-box;
  height: 120px;
  line-height: 18px;
  padding: 10px;
  resize: none;
  width: 100%;
}

.content .text-field-container {
  -webkit-align-items: center;
  -webkit-padding-start: 10px;
  display: -webkit-flex;
  height: 29px;
  margin-top: 10px;
}

.content .text-field-container > label {
  -webkit-flex: 0 1 auto;
  width: 100px;
}

.content .text-field-container > input[type=text] {
  -webkit-flex: 1 1 auto;
  -webkit-padding-start: 5px;
  border: 1px solid;
  border-color: #c8c8c8;
  color: #585858;
  height: 100%;
}

.content .text-field-container > input[type=checkbox] {
  margin-right: 9px;
}

.content .checkbox-field-container {
  -webkit-align-items: center;
  display: -webkit-flex;
  height: 29px;
}

#screenshot-container {
  margin-top: 10px;
}

.content #screenshot-image {
  -webkit-margin-start: 100px;
  display: block;
  height: 60px;
  margin-top: 40px;
  transition: all 250ms ease;
}

.content #screenshot-image:hover {
  -webkit-margin-start: 80px;
  height: 125px;
  margin-top: 80px;
  z-index: 1;
}

.content #screenshot-image.wide-screen {
  height: auto;
  width: 100px;
}

.content #screenshot-image.wide-screen:hover {
  height: auto;
  width: 200px;
}

.content #privacy-note {
  color: #969696;
  font-size: 10px;
  line-height: 15px;
  margin-bottom: 20px;
  margin-top: 20px;
}

.content .buttons-pane {
  display: -webkit-flex;
  justify-content: flex-end
}

.content .remove-file-button {
  -webkit-margin-start: 5px;
  background-color: transparent;
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close.png) 2x);
  background-position: 50% 80%;
  background-repeat: no-repeat;
  border: none;
  height: 16px;
  pointer-events: auto;
  width: 16px;
}

.content .remove-file-button:hover {
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close_hover.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close_hover.png) 2x);
}

.content .remove-file-button:active {
  background-image: -webkit-image-set(
      url(chrome://resources/images/apps/button_butter_bar_close_pressed.png) 1x,
      url(chrome://resources/images/2x/apps/button_butter_bar_close_pressed.png) 2x);
}

.content #attach-file-note {
  -webkit-margin-start: 112px;
  margin-bottom: 10px;
  margin-top: 10px;
}

.content .attach-file-notification {
  color: rgb(204, 0, 0);
  font-weight: bold;
}

button.white-button {
  -webkit-margin-end: 10px;
  color: #000;
}

button.blue-button {
  color: #fff;
  text-shadow: 1px sharp drop shadow rgb(45, 106, 218);
}
‰PNG
