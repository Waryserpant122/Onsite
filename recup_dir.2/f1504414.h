t|, and registers it
// to be removed once this GuestViewEvents object is garbage collected.
GuestViewEvents.prototype.addScopedListener = function(
    evt, listener, listenerOpts) {
  this.listenersToBeRemoved.push({ 'evt': evt, 'listener': listener });
  evt.addListener(listener, listenerOpts);
};

// Sets up the handling of events.
GuestViewEvents.prototype.setupEvents = function() {
  // An array of registerd event listeners that should be removed when this
  // GuestViewEvents is garbage collected.
  this.listenersToBeRemoved = [];
  MessagingNatives.BindToGC(this, function(listenersToBeRemoved) {
    for (var i = 0; i != listenersToBeRemoved.length; ++i) {
      listenersToBeRemoved[i].evt.removeListener(
          listenersToBeRemoved[i].listener);
      listenersToBeRemoved[i] = null;
    }
  }.bind(undefined, this.listenersToBeRemoved), -1 /* portId */);

  // Set up the GuestView events.
  for (var eventName in GuestViewEvents.EVENTS) {
    this.setupEvent(eventName, GuestViewEvents.EVENTS[eventName]);
  }

  // Set up the derived view's events.
  var events = this.getEvents();
  for (var eventName in events) {
    this.setupEvent(eventName, events[eventName]);
  }
};

// Sets up the handling of the |eventName| event.
GuestViewEvents.prototype.setupEvent = function(eventName, eventInfo) {
  if (!eventInfo.internal) {
    this.setupEventProperty(eventName);
  }

  var listenerOpts = { instanceId: this.view.viewInstanceId };
  if (eventInfo.handler) {
    this.addScopedListener(eventInfo.evt, this.weakWrapper(function(e) {
      this[eventInfo.handler](e, eventName);
    }), listenerOpts);
    return;
  }

  // Internal events are not dispatched as DOM events.
  if (eventInfo.internal) {
    return;
  }

  this.addScopedListener(eventInfo.evt, this.weakWrapper(function(e) {
    var domEvent = this.makeDomEvent(e, eventName);
    this.view.dispatchEvent(domEvent);
  }), listenerOpts);
};

// Constructs a DOM event based on the info for the |eventName| event provided
// in either |GuestViewEvents.EVENTS| or getEvents().
GuestViewEvents.prototype.makeDomEvent = function(event, eventName) {
  var eventInfo =
      GuestViewEvents.EVENTS[eventName] || this.getEvents()[eventName];

  // Internal events are not dispatched as DOM events.
  if (eventInfo.internal) {
    return null;
  }

  var details = { bubbles: true };
  if (eventInfo.cancelable) {
    details.cancelable = true;
  }
  var domEvent = new Event(eventName, details);
  if (eventInfo.fields) {
    $Array.forEach(eventInfo.fields, function(field) {
      if (event[field] !== undefined) {
        domEvent[field] = event[field];
      }
    }.bind(this));
  }

  return domEvent;
};

// Adds an 'on<event>' property on the view, which can be used to set/unset
// an event handler.
GuestViewEvents.prototype.setupEventProperty = function(eventName) {
  var propertyName = 'on' + eventName.toLowerCase();
  $Object.defineProperty(this.view.element, propertyName, {
    get: function() {
      return this.on[propertyName];
    }.bind(this),
    set: function(value) {
      if (this.on[propertyName]) {
        this.view.element.removeEventListener(eventName, this.on[propertyName]);
      }
      this.on[propertyName] = value;
      if (value) {
        this.view.element.addEventListener(eventName, value);
      }
    }.bind(this),
    enumerable: true
  });
};

// returns a wrapper for |func| with a weak reference to |this|.
GuestViewEvents.prototype.weakWrapper = function(func) {
  var viewInstanceId = this.view.viewInstanceId;
  return function() {
    var view = GuestViewInternalNatives.GetViewFromID(viewInstanceId);
    if (!view) {
      return;
    }
    return $Function.apply(func, view.events, $Array.slice(arguments));
  };
};

// Implemented by the derived event manager, if one exists.
GuestViewEvents.prototype.getEvents = function() { return {}; };

// Exports.
exports.$set('GuestViewEvents', GuestViewEvents);
exports.$set('CreateEvent', CreateEvent);
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// --site-per-process overrides for guest_view_container.js

var GuestViewContainer = require('guestViewContainer').GuestViewContainer;
var IdGenerator = requireNative('id_generator');

GuestViewContainer.prototype.createInternalElement$ = function() {
  var iframeElement = document.createElement('iframe');
  iframeElement.style.width = '100%';
  iframeElement.style.height = '100%';
  privates(iframeElement).internal = this;
  return iframeElement;
};

GuestViewContainer.prototype.attachWindow$ = function() {
  var generatedId = IdGenerator.GetNextId();
  // Generate an instance id for the container.
  this.onInternalInstanceId(generatedId);
  return true;
};

GuestViewContainer.prototype.willAttachElement = function () {
  if (this.deferredAttachCallback) {
    this.deferredAttachCallback();
    this.deferredAttachCallback = null;
  }
};
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// --site-per-process overrides for guest_view.js.

var GuestView = require('guestView').GuestView;
var GuestViewImpl = require('guestView').GuestViewImpl;
var GuestViewInternalNatives = requireNative('guest_view_internal');
var ResizeEvent = require('guestView').ResizeEvent;

var getIframeContentWindow = function(viewInstanceId) {
  var view = GuestViewInternalNatives.GetViewFromID(viewInstanceId);
  if (!view)
    return null;

  var internalIframeElement = privates(view).internalElement;
  if (internalIframeElement)
    return internalIframeElement.contentWindow;

  return null;
};

// Internal implementation of attach().
GuestViewImpl.prototype.attachImpl$ = function(
    internalInstanceId, viewInstanceId, attachParams, callback) {
  var view = GuestViewInternalNatives.GetViewFromID(viewInstanceId);
  if (!view.elementAttached) {
    // Defer the attachment until the <webview> element is attached.
    view.deferredAttachCallback = this.attachImpl$.bind(
        this, internalInstanceId, viewInstanceId, attachParams, callback);
    return;
  };

  // Check the current state.
  if (!this.checkState('attach')) {
    this.handleCallback(callback);
    return;
  }

  // Callback wrapper function to store the contentWindow from the attachGuest()
  // callback, handle potential attaching failure, register an automatic detach,
  // and advance the queue.
  var callbackWrapper = function(callback, contentWindow) {
    // Check if attaching failed.
    contentWindow = getIframeContentWindow(viewInstanceId);
    if (!contentWindow) {
      this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
      this.internalInstanceId = 0;
    } else {
      // Only update the contentWindow if attaching is successful.
      this.contentWindow = contentWindow;
    }

    this.handleCallback(callback);
  };

  attachParams['instanceId'] = viewInstanceId;
  var contentWindow = getIframeContentWindow(viewInstanceId);
  // |contentWindow| is used to retrieve the RenderFrame in cpp.
  GuestViewInternalNatives.AttachIframeGuest(
      internalInstanceId, this.id, attachParams, contentWindow,
      callbackWrapper.bind(this, callback));

  this.internalInstanceId = internalInstanceId;
  this.state = GuestViewImpl.GuestState.GUEST_STATE_ATTACHED;

  // Detach automatically when the container is destroyed.
  GuestViewInternalNatives.RegisterDestructionCallback(
      internalInstanceId, this.weakWrapper(function() {
    if (this.state != GuestViewImpl.GuestState.GUEST_STATE_ATTACHED ||
        this.internalInstanceId != internalInstanceId) {
      return;
    }

    this.internalInstanceId = 0;
    this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
  }, viewInstanceId));
};

// Internal implementation of create().
GuestViewImpl.prototype.createImpl$ = function(createParams, callback) {
  // Check the current state.
  if (!this.checkState('create')) {
    this.handleCallback(callback);
    return;
  }

  // Callback wrapper function to store the guestInstanceId from the
  // createGuest() callback, handle potential creation failure, and advance the
  // queue.
  var callbackWrapper = function(callback, guestInfo) {
    this.id = guestInfo.id;

    // Check if creation failed.
    if (this.id === 0) {
      this.state = GuestViewImpl.GuestState.GUEST_STATE_START;
      this.contentWindow = null;
    }

    ResizeEvent.addListener(this.callOnResize, {instanceId: this.id});
    this.handleCallback(callback);
  };

  this.sendCreateRequest(createParams, callbackWrapper.bind(this, callback));

  this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements a wrapper for a guestview that manages its
// creation, attaching, and destruction.

var CreateEvent = require('guestViewEvents').CreateEvent;
var EventBindings = require('event_bindings');
var GuestViewInternal =
    require('binding').Binding.create('guestViewInternal').generate();
var GuestViewInternalNatives = requireNative('guest_view_internal');

// Events.
var ResizeEvent = CreateEvent('guestViewInternal.onResize');

// Error messages.
var ERROR_MSG_ALREADY_ATTACHED = 'The guest has already been attached.';
var ERROR_MSG_ALREADY_CREATED = 'The guest has already been created.';
var ERROR_MSG_INVALID_STATE = 'The guest is in an invalid state.';
var ERROR_MSG_NOT_ATTACHED = 'The guest is not attached.';
var ERROR_MSG_NOT_CREATED = 'The guest has not been created.';

// Properties.
var PROPERTY_ON_RESIZE = 'onresize';

// Contains and hides the internal implementation details of |GuestView|,
// including maintaining its state and enforcing the proper usage of its API
// fucntions.
function GuestViewImpl(guestView, viewType, guestInstanceId) {
  if (guestInstanceId) {
    this.id = guestInstanceId;
    this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
  } else {
    this.id = 0;
    this.state = GuestViewImpl.GuestState.GUEST_STATE_START;
  }
  this.actionQueue = [];
  this.contentWindow = null;
  this.guestView = guestView;
  this.pendingAction = null;
  this.viewType = viewType;
  this.internalInstanceId = 0;

  this.setupOnResize();
}

// Possible states.
GuestViewImpl.GuestState = {
  GUEST_STATE_START: 0,
  GUEST_STATE_CREATED: 1,
  GUEST_STATE_ATTACHED: 2
};

// Sets up the onResize property on the GuestView.
GuestViewImpl.prototype.setupOnResize = function() {
  $Object.defineProperty(this.guestView, PROPERTY_ON_RESIZE, {
    get: function() {
      return this[PROPERTY_ON_RESIZE];
    }.bind(this),
    set: function(value) {
      this[PROPERTY_ON_RESIZE] = value;
    }.bind(this),
    enumerable: true
  });

  this.callOnResize = function(e) {
    if (!this[PROPERTY_ON_RESIZE]) {
      return;
    }
    this[PROPERTY_ON_RESIZE](e);
  }.bind(this);
};

// Callback wrapper that is used to call the callback of the pending action (if
// one exists), and then performs the next action in the queue.
GuestViewImpl.prototype.handleCallback = function(callback) {
  if (callback) {
    callback();
  }
  this.pendingAction = null;
  this.performNextAction();
};

// Perform the next action in the queue, if one exists.
GuestViewImpl.prototype.performNextAction = function() {
  // Make sure that there is not already an action in progress, and that there
  // exists a queued action to perform.
  if (!this.pendingAction && this.actionQueue.length) {
    this.pendingAction = this.actionQueue.shift();
    this.pendingAction();
  }
};

// Check the current state to see if the proposed action is valid. Returns false
// if invalid.
GuestViewImpl.prototype.checkState = function(action) {
  // Create an error prefix based on the proposed action.
  var errorPrefix = 'Error calling ' + action + ': ';

  // Check that the current state is valid.
  if (!(this.state >= 0 && this.state <= 2)) {
    window.console.error(errorPrefix + ERROR_MSG_INVALID_STATE);
    return false;
  }

  // Map of possible errors for each action. For each action, the errors are
  // listed for states in the order: GUEST_STATE_START, GUEST_STATE_CREATED,
  // GUEST_STATE_ATTACHED.
  var errors = {
    'attach': [ERROR_MSG_NOT_CREATED, null, ERROR_MSG_ALREADY_ATTACHED],
    'create': [null, ERROR_MSG_ALREADY_CREATED, ERROR_MSG_ALREADY_CREATED],
    'destroy': [null, null, null],
    'detach': [ERROR_MSG_NOT_ATTACHED, ERROR_MSG_NOT_ATTACHED, null],
    'setSize': [ERROR_MSG_NOT_CREATED, null, null]
  };

  // Check that the proposed action is a real action.
  if (errors[action] == undefined) {
    window.console.error(errorPrefix + ERROR_MSG_INVALID_ACTION);
    return false;
  }

  // Report the error if the proposed action is found to be invalid for the
  // current state.
  var error;
  if (error = errors[action][this.state]) {
    window.console.error(errorPrefix + error);
    return false;
  }

  return true;
};

// Returns a wrapper function for |func| with a weak reference to |this|. This
// implementation of weakWrapper() requires a provided |viewInstanceId| since
// GuestViewImpl does not store this ID.
GuestViewImpl.prototype.weakWrapper = function(func, viewInstanceId) {
  return function() {
    var view = GuestViewInternalNatives.GetViewFromID(viewInstanceId);
    if (view && view.guest) {
      return $Function.apply(func,
                             privates(view.guest).internal,
                             $Array.slice(arguments));
    }
  };
};

// Internal implementation of attach().
GuestViewImpl.prototype.attachImpl$ = function(
    internalInstanceId, viewInstanceId, attachParams, callback) {
  // Check the current state.
  if (!this.checkState('attach')) {
    this.handleCallback(callback);
    return;
  }

  // Callback wrapper function to store the contentWindow from the attachGuest()
  // callback, handle potential attaching failure, register an automatic detach,
  // and advance the queue.
  var callbackWrapper = function(callback, contentWindow) {
    // Check if attaching failed.
    if (!contentWindow) {
      this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
      this.internalInstanceId = 0;
    } else {
      // Only update the contentWindow if attaching is successful.
      this.contentWindow = contentWindow;
    }

    this.handleCallback(callback);
  };

  attachParams['instanceId'] = viewInstanceId;
  GuestViewInternalNatives.AttachGuest(internalInstanceId,
                                       this.id,
                                       attachParams,
                                       callbackWrapper.bind(this, callback));

  this.internalInstanceId = internalInstanceId;
  this.state = GuestViewImpl.GuestState.GUEST_STATE_ATTACHED;

  // Detach automatically when the container is destroyed.
  GuestViewInternalNatives.RegisterDestructionCallback(
      internalInstanceId, this.weakWrapper(function() {
    if (this.state != GuestViewImpl.GuestState.GUEST_STATE_ATTACHED ||
        this.internalInstanceId != internalInstanceId) {
      return;
    }

    this.internalInstanceId = 0;
    this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
  }, viewInstanceId));
};

// Internal implementation of create().
GuestViewImpl.prototype.createImpl$ = function(createParams, callback) {
  // Check the current state.
  if (!this.checkState('create')) {
    this.handleCallback(callback);
    return;
  }

  // Callback wrapper function to store the guestInstanceId from the
  // createGuest() callback, handle potential creation failure, and advance the
  // queue.
  var callbackWrapper = function(callback, guestInfo) {
    this.id = guestInfo.id;
    this.contentWindow =
        GuestViewInternalNatives.GetContentWindow(guestInfo.contentWindowId);

    // Check if creation failed.
    if (this.id === 0) {
      this.state = GuestViewImpl.GuestState.GUEST_STATE_START;
      this.contentWindow = null;
    }

    ResizeEvent.addListener(this.callOnResize, {instanceId: this.id});
    this.handleCallback(callback);
  };

  this.sendCreateRequest(createParams, callbackWrapper.bind(this, callback));

  this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
};

GuestViewImpl.prototype.sendCreateRequest = function(
    createParams, boundCallback) {
  GuestViewInternal.createGuest(this.viewType, createParams, boundCallback);
};

// Internal implementation of destroy().
GuestViewImpl.prototype.destroyImpl = function(callback) {
  // Check the current state.
  if (!this.checkState('destroy')) {
    this.handleCallback(callback);
    return;
  }

  if (this.state == GuestViewImpl.GuestState.GUEST_STATE_START) {
    // destroy() does nothing in this case.
    this.handleCallback(callback);
    return;
  }

  // If this guest is attached, then detach it first.
  if (!!this.internalInstanceId) {
    GuestViewInternalNatives.DetachGuest(this.internalInstanceId);
  }

  GuestViewInternal.destroyGuest(this.id,
                                 this.handleCallback.bind(this, callback));

  // Reset the state of the destroyed guest;
  this.contentWindow = null;
  this.id = 0;
  this.internalInstanceId = 0;
  this.state = GuestViewImpl.GuestState.GUEST_STATE_START;
  if (ResizeEvent.hasListener(this.callOnResize)) {
    ResizeEvent.removeListener(this.callOnResize);
  }
};

// Internal implementation of detach().
GuestViewImpl.prototype.detachImpl = function(callback) {
  // Check the current state.
  if (!this.checkState('detach')) {
    this.handleCallback(callback);
    return;
  }

  GuestViewInternalNatives.DetachGuest(
      this.internalInstanceId,
      this.handleCallback.bind(this, callback));

  this.internalInstanceId = 0;
  this.state = GuestViewImpl.GuestState.GUEST_STATE_CREATED;
};

// Internal implementation of setSize().
GuestViewImpl.prototype.setSizeImpl = function(sizeParams, callback) {
  // Check the current state.
  if (!this.checkState('setSize')) {
    this.handleCallback(callback);
    return;
  }

  GuestViewInternal.setSize(this.id, sizeParams,
                            this.handleCallback.bind(this, callback));
};

// The exposed interface to a guestview. Exposes in its API the functions
// attach(), create(), destroy(), and getId(). All other implementation details
// are hidden.
function GuestView(viewType, guestInstanceId) {
  privates(this).internal = new GuestViewImpl(this, viewType, guestInstanceId);
}

// Attaches the guestview to the container with ID |internalInstanceId|.
GuestView.prototype.attach = function(
    internalInstanceId, viewInstanceId, attachParams, callback) {
  var internal = privates(this).internal;
  internal.actionQueue.push(internal.attachImpl$.bind(
      internal, internalInstanceId, viewInstanceId, attachParams, callback));
  internal.performNextAction();
};

// Creates the guestview.
GuestView.prototype.create = function(createParams, callback) {
  var internal = privates(this).internal;
  internal.actionQueue.push(internal.createImpl$.bind(
      internal, createParams, callback));
  internal.performNextAction();
};

// Destroys the guestview. Nothing can be done with the guestview after it has
// been destroyed.
GuestView.prototype.destroy = function(callback) {
  var internal = privates(this).internal;
  internal.actionQueue.push(internal.destroyImpl.bind(internal, callback));
  internal.performNextAction();
};

// Detaches the guestview from its container.
// Note: This is not currently used.
GuestView.prototype.detach = function(callback) {
  var internal = privates(this).internal;
  internal.actionQueue.push(internal.detachImpl.bind(internal, callback));
  internal.performNextAction();
};

// Adjusts the guestview's sizing parameters.
GuestView.prototype.setSize = function(sizeParams, callback) {
  var internal = privates(this).internal;
  internal.actionQueue.push(internal.setSizeImpl.bind(
      internal, sizeParams, callback));
  internal.performNextAction();
};

// Returns the contentWindow for this guestview.
GuestView.prototype.getContentWindow = function() {
  var internal = privates(this).internal;
  return internal.contentWindow;
};

// Returns the ID for this guestview.
GuestView.prototype.getId = function() {
  var internal = privates(this).internal;
  return internal.id;
};

// Exports
exports.$set('GuestView', GuestView);
exports.$set('GuestViewImpl', GuestViewImpl);
exports.$set('ResizeEvent', ResizeEvent);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This function takes an object |imageSpec| with the key |path| -
// corresponding to the internet URL to be translated - and optionally
// |width| and |height| which are the maximum dimensions to be used when
// converting the image.
function loadImageData(imageSpec, callbacks) {
  var path = imageSpec.path;
  var img = new Image();
  if (typeof callbacks.onerror === 'function') {
    img.onerror = function() {
      callbacks.onerror({ problem: 'could_not_load', path: path });
    };
  }
  img.onload = function() {
    var canvas = document.createElement('canvas');

    if (img.width <= 0 || img.height <= 0) {
      callbacks.onerror({ problem: 'image_size_invalid', path: path});
      return;
    }

    var scaleFactor = 1;
    if (imageSpec.width && imageSpec.width < img.width)
      scaleFactor = imageSpec.width / img.width;

    if (imageSpec.height && imageSpec.height < img.height) {
      var heightScale = imageSpec.height / img.height;
      if (heightScale < scaleFactor)
        scaleFactor = heightScale;
    }

    canvas.width = img.width * scaleFactor;
    canvas.height = img.height * scaleFactor;

    var canvas_context = canvas.getContext('2d');
    canvas_context.clearRect(0, 0, canvas.width, canvas.height);
    canvas_context.drawImage(img, 0, 0, canvas.width, canvas.height);
    try {
      var imageData = canvas_context.getImageData(
          0, 0, canvas.width, canvas.height);
      if (typeof callbacks.oncomplete === 'function') {
        callbacks.oncomplete(
            imageData.width, imageData.height, imageData.data.buffer);
      }
    } catch (e) {
      if (typeof callbacks.onerror === 'function') {
        callbacks.onerror({ problem: 'data_url_unavailable', path: path });
      }
    }
  }
  img.src = path;
}

function on_complete_index(index, err, loading, finished, callbacks) {
  return function(width, height, imageData) {
    delete loading[index];
    finished[index] = { width: width, height: height, data: imageData };
    if (err)
      callbacks.onerror(index);
    if ($Object.keys(loading).length == 0)
      callbacks.oncomplete(finished);
  }
}

function loadAllImages(imageSpecs, callbacks) {
  var loading = {}, finished = [],
      index, pathname;

  for (var index = 0; index < imageSpecs.length; index++) {
    loading[index] = imageSpecs[index];
    loadImageData(imageSpecs[index], {
      oncomplete: on_complete_index(index, false, loading, finished, callbacks),
      onerror: on_complete_index(index, true, loading, finished, callbacks)
    });
  }
}

exports.$set('loadImageData', loadImageData);
exports.$set('loadAllImages', loadAllImages);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// -----------------------------------------------------------------------------
// NOTE: If you change this file you need to touch
// extension_renderer_resources.grd to have your change take effect.
// -----------------------------------------------------------------------------

//==============================================================================
// This file contains a class that implements a subset of JSON Schema.
// See: http://www.json.com/json-schema-proposal/ for more details.
//
// The following features of JSON Schema are not implemented:
// - requires
// - unique
// - disallow
// - union types (but replaced with 'choices')
//
// The following properties are not applicable to the interface exposed by
// this class:
// - options
// - readonly
// - title
// - description
// - format
// - default
// - transient
// - hidden
//
// There are also these departures from the JSON Schema proposal:
// - function and undefined types are supported
// - null counts as 'unspecified' for optional values
// - added the 'choices' property, to allow specifying a list of possible types
//   for a value
// - by default an "object" typed schema does not allow additional properties.
//   if present, "additionalProperties" is to be a schema against which all
//   additional properties will be validated.
//==============================================================================

var loadTypeSchema = require('utils').loadTypeSchema;
var CHECK = requireNative('logging').CHECK;

function isInstanceOfClass(instance, className) {
  while ((instance = instance.__proto__)) {
    if (instance.constructor.name == className)
      return true;
  }
  return false;
}

function isOptionalValue(value) {
  return typeof(value) === 'undefined' || value === null;
}

function enumToString(enumValue) {
  if (enumValue.name === undefined)
    return enumValue;

  return enumValue.name;
}

/**
 * Validates an instance against a schema and accumulates errors. Usage:
 *
 * var validator = new JSONSchemaValidator();
 * validator.validate(inst, schema);
 * if (validator.errors.length == 0)
 *   console.log("Valid!");
 * else
 *   console.log(validator.errors);
 *
 * The errors property contains a list of objects. Each object has two
 * properties: "path" and "message". The "path" property contains the path to
 * the key that had the problem, and the "message" property contains a sentence
 * describing the error.
 */
function JSONSchemaValidator() {
  this.errors = [];
  this.types = [];
}

JSONSchemaValidator.messages = {
  invalidEnum: "Value must be one of: [*].",
  propertyRequired: "Property is required.",
  unexpectedProperty: "Unexpected property.",
  arrayMinItems: "Array must have at least * items.",
  arrayMaxItems: "Array must not have more than * items.",
  itemRequired: "Item is required.",
  stringMinLength: "String must be at least * characters long.",
  stringMaxLength: "String must not be more than * characters long.",
  stringPattern: "String must match the pattern: *.",
  numberFiniteNotNan: "Value must not be *.",
  numberMinValue: "Value must not be less than *.",
  numberMaxValue: "Value must not be greater than *.",
  numberIntValue: "Value must fit in a 32-bit signed integer.",
  numberMaxDecimal: "Value must not have more than * decimal places.",
  invalidType: "Expected '*' but got '*'.",
  invalidTypeIntegerNumber:
      "Expected 'integer' but got 'number', consider using Math.round().",
  invalidChoice: "Value does not match any valid type choices.",
  invalidPropertyType: "Missing property type.",
  schemaRequired: "Schema value required.",
  unknownSchemaReference: "Unknown schema reference: *.",
  notInstance: "Object must be an instance of *."
};

/**
 * Builds an error message. Key is the property in the |errors| object, and
 * |opt_replacements| is an array of values to replace "*" characters with.
 */
JSONSchemaValidator.formatError = function(key, opt_replacements) {
  var message = this.messages[key];
  if (opt_replacements) {
    for (var i = 0; i < opt_replacements.length; i++) {
      message = message.replace("*", opt_replacements[i]);
    }
  }
  return message;
};

/**
 * Classifies a value as one of the JSON schema primitive types. Note that we
 * don't explicitly disallow 'function', because we want to allow functions in
 * the input values.
 */
JSONSchemaValidator.getType = function(value) {
  var s = typeof value;

  if (s == "object") {
    if (value === null) {
      return "null";
    } else if (Object.prototype.toString.call(value) == "[object Array]") {
      return "array";
    } else if (Object.prototype.toString.call(value) ==
               "[object ArrayBuffer]") {
      return "binary";
    }
  } else if (s == "number") {
    if (value % 1 == 0) {
      return "integer";
    }
  }

  return s;
};

/**
 * Add types that may be referenced by validated schemas that reference them
 * with "$ref": <typeId>. Each type must be a valid schema and define an
 * "id" property.
 */
JSONSchemaValidator.prototype.addTypes = function(typeOrTypeList) {
  function addType(validator, type) {
    if (!type.id)
      throw new Error("Attempt to addType with missing 'id' property");
    validator.types[type.id] = type;
  }

  if (typeOrTypeList instanceof Array) {
    for (var i = 0; i < typeOrTypeList.length; i++) {
      addType(this, typeOrTypeList[i]);
    }
  } else {
    addType(this, typeOrTypeList);
  }
}

/**
 * Returns a list of strings of the types that this schema accepts.
 */
JSONSchemaValidator.prototype.getAllTypesForSchema = function(schema) {
  var schemaTypes = [];
  if (schema.type)
    $Array.push(schemaTypes, schema.type);
  if (schema.choices) {
    for (var i = 0; i < schema.choices.length; i++) {
      var choiceTypes = this.getAllTypesForSchema(schema.choices[i]);
      schemaTypes = $Array.concat(schemaTypes, choiceTypes);
    }
  }
  var ref = schema['$ref'];
  if (ref) {
    var type = this.getOrAddType(ref);
    CHECK(type, 'Could not find type ' + ref);
    schemaTypes = $Array.concat(schemaTypes, this.getAllTypesForSchema(type));
  }
  return schemaTypes;
};

JSONSchemaValidator.prototype.getOrAddType = function(typeName) {
  if (!this.types[typeName])
    this.types[typeName] = loadTypeSchema(typeName);
  return this.types[typeName];
};

/**
 * Returns true if |schema| would accept an argument of type |type|.
 */
JSONSchemaValidator.prototype.isValidSchemaType = function(type, schema) {
  if (type == 'any')
    return true;

  // TODO(kalman): I don't understand this code. How can type be "null"?
  if (schema.optional && (type == "null" || type == "undefined"))
    return true;

  var schemaTypes = this.getAllTypesForSchema(schema);
  for (var i = 0; i < schemaTypes.length; i++) {
    if (schemaTypes[i] == "any" || type == schemaTypes[i] ||
        (type == "integer" && schemaTypes[i] == "number"))
      return true;
  }

  return false;
};

/**
 * Returns true if there is a non-null argument that both |schema1| and
 * |schema2| would accept.
 */
JSONSchemaValidator.prototype.checkSchemaOverlap = function(schema1, schema2) {
  var schema1Types = this.getAllTypesForSchema(schema1);
  for (var i = 0; i < schema1Types.length; i++) {
    if (this.isValidSchemaType(schema1Types[i], schema2))
      return true;
  }
  return false;
};

/**
 * Validates an instance against a schema. The instance can be any JavaScript
 * value and will be validated recursively. When this method returns, the
 * |errors| property will contain a list of errors, if any.
 */
JSONSchemaValidator.prototype.validate = function(instance, schema, opt_path) {
  var path = opt_path || "";

  if (!schema) {
    this.addError(path, "schemaRequired");
    return;
  }

  // If this schema defines itself as reference type, save it in this.types.
  if (schema.id)
    this.types[schema.id] = schema;

  // If the schema has an extends property, the instance must validate against
  // that schema too.
  if (schema.extends)
    this.validate(instance, schema.extends, path);

  // If the schema has a $ref property, the instance must validate against
  // that schema too. It must be present in this.types to be referenced.
  var ref = schema["$ref"];
  if (ref) {
    if (!this.getOrAddType(ref))
      this.addError(path, "unknownSchemaReference", [ ref ]);
    else
      this.validate(instance, this.getOrAddType(ref), path)
  }

  // If the schema has a choices property, the instance must validate against at
  // least one of the items in that array.
  if (schema.choices) {
    this.validateChoices(instance, schema, path);
    return;
  }

  // If the schema has an enum property, the instance must be one of those
  // values.
  if (schema.enum) {
    if (!this.validateEnum(instance, schema, path))
      return;
  }

  if (schema.type && schema.type != "any") {
    if (!this.validateType(instance, schema, path))
      return;

    // Type-specific validation.
    switch (schema.type) {
      case "object":
        this.validateObject(instance, schema, path);
        break;
      case "array":
        this.validateArray(instance, schema, path);
        break;
      case "string":
        this.validateString(instance, schema, path);
        break;
      case "number":
      case "integer":
        this.validateNumber(instance, schema, path);
        break;
    }
  }
};

/**
 * Validates an instance against a choices schema. The instance must match at
 * least one of the provided choices.
 */
JSONSchemaValidator.prototype.validateChoices =
    function(instance, schema, path) {
  var originalErrors = this.errors;

  for (var i = 0; i < schema.choices.length; i++) {
    this.errors = [];
    this.validate(instance, schema.choices[i], path);
    if (this.errors.length == 0) {
      this.errors = originalErrors;
      return;
    }
  }

  this.errors = originalErrors;
  this.addError(path, "invalidChoice");
};

/**
 * Validates an instance against a schema with an enum type. Populates the
 * |errors| property, and returns a boolean indicating whether the instance
 * validates.
 */
JSONSchemaValidator.prototype.validateEnum = function(instance, schema, path) {
  for (var i = 0; i < schema.enum.length; i++) {
    if (instance === enumToString(schema.enum[i]))
      return true;
  }

  this.addError(path, "invalidEnum",
                [$Array.join($Array.map(schema.enum, enumToString), ", ")]);
  return false;
};

/**
 * Validates an instance against an object schema and populates the errors
 * property.
 */
JSONSchemaValidator.prototype.validateObject =
    function(instance, schema, path) {
  if (schema.properties) {
    for (var prop in schema.properties) {
      // It is common in JavaScript to add properties to Object.prototype. This
      // check prevents such additions from being interpreted as required
      // schema properties.
      // TODO(aa): If it ever turns out that we actually want this to work,
      // there are other checks we could put here, like requiring that schema
      // properties be objects that have a 'type' property.
      if (!$Object.hasOwnProperty(schema.properties, prop))
        continue;

      var propPath = path ? path + "." + prop : prop;
      if (schema.properties[prop] == undefined) {
        this.addError(propPath, "invalidPropertyType");
      } else if (prop in instance && !isOptionalValue(instance[prop])) {
        this.validate(instance[prop], schema.properties[prop], propPath);
      } else if (!schema.properties[prop].optional) {
        this.addError(propPath, "propertyRequired");
      }
    }
  }

  // If "instanceof" property is set, check that this object inherits from
  // the specified constructor (function).
  if (schema.isInstanceOf) {
    if (!isInstanceOfClass(instance, schema.isInstanceOf))
      this.addError(propPath, "notInstance", [schema.isInstanceOf]);
  }

  // Exit early from additional property check if "type":"any" is defined.
  if (schema.additionalProperties &&
      schema.additionalProperties.type &&
      schema.additionalProperties.type == "any") {
    return;
  }

  // By default, additional properties are not allowed on instance objects. This
  // can be overridden by setting the additionalProperties property to a schema
  // which any additional properties must validate against.
  for (var prop in instance) {
    if (schema.properties && prop in schema.properties)
      continue;

    // Any properties inherited through the prototype are ignored.
    if (!$Object.hasOwnProperty(instance, prop))
      continue;

    var propPath = path ? path + "." + prop : prop;
    if (schema.additionalProperties)
      this.validate(instance[prop], schema.additionalProperties, propPath);
    else
      this.addError(propPath, "unexpectedProperty");
  }
};

/**
 * Validates an instance against an array schema and populates the errors
 * property.
 */
JSONSchemaValidator.prototype.validateArray = function(instance, schema, path) {
  var typeOfItems = JSONSchemaValidator.getType(schema.items);

  if (typeOfItems == 'object') {
    if (schema.minItems && instance.length < schema.minItems) {
      this.addError(path, "arrayMinItems", [schema.minItems]);
    }

    if (typeof schema.maxItems != "undefined" &&
        instance.length > schema.maxItems) {
      this.addError(path, "arrayMaxItems", [schema.maxItems]);
    }

    // If the items property is a single schema, each item in the array must
    // have that schema.
    for (var i = 0; i < instance.length; i++) {
      this.validate(instance[i], schema.items, path + "." + i);
    }
  } else if (typeOfItems == 'array') {
    // If the items property is an array of schemas, each item in the array must
    // validate against the corresponding schema.
    for (var i = 0; i < schema.items.length; i++) {
      var itemPath = path ? path + "." + i : String(i);
      if (i in instance && !isOptionalValue(instance[i])) {
        this.validate(instance[i], schema.items[i], itemPath);
      } else if (!schema.items[i].optional) {
        this.addError(itemPath, "itemRequired");
      }
    }

    if (schema.additionalProperties) {
      for (var i = schema.items.length; i < instance.length; i++) {
        var itemPath = path ? path + "." + i : String(i);
        this.validate(instance[i], schema.additionalProperties, itemPath);
      }
    } else {
      if (instance.length > schema.items.length) {
        this.addError(path, "arrayMaxItems", [schema.items.length]);
      }
    }
  }
};

/**
 * Validates a string and populates the errors property.
 */
JSONSchemaValidator.prototype.validateString =
    function(instance, schema, path) {
  if (schema.minLength && instance.length < schema.minLength)
    this.addError(path, "stringMinLength", [schema.minLength]);

  if (schema.maxLength && instance.length > schema.maxLength)
    this.addError(path, "stringMaxLength", [schema.maxLength]);

  if (schema.pattern && !schema.pattern.test(instance))
    this.addError(path, "stringPattern", [schema.pattern]);
};

/**
 * Validates a number and populates the errors property. The instance is
 * assumed to be a number.
 */
JSONSchemaValidator.prototype.validateNumber =
    function(instance, schema, path) {
  // Forbid NaN, +Infinity, and -Infinity.  Our APIs don't use them, and
  // JSON serialization encodes them as 'null'.  Re-evaluate supporting
  // them if we add an API that could reasonably take them as a parameter.
  if (isNaN(instance) ||
      instance == Number.POSITIVE_INFINITY ||
      instance == Number.NEGATIVE_INFINITY )
    this.addError(path, "numberFiniteNotNan", [instance]);

  if (schema.minimum !== undefined && instance < schema.minimum)
    this.addError(path, "numberMinValue", [schema.minimum]);

  if (schema.maximum !== undefined && instance > schema.maximum)
    this.addError(path, "numberMaxValue", [schema.maximum]);

  // Check for integer values outside of -2^31..2^31-1.
  if (schema.type === "integer" && (instance | 0) !== instance)
    this.addError(path, "numberIntValue", []);

  if (schema.maxDecimal && instance * Math.pow(10, schema.maxDecimal) % 1)
    this.addError(path, "numberMaxDecimal", [schema.maxDecimal]);
};

/**
 * Validates the primitive type of an instance and populates the errors
 * property. Returns true if the instance validates, false otherwise.
 */
JSONSchemaValidator.prototype.validateType = function(instance, schema, path) {
  var actualType = JSONSchemaValidator.getType(instance);
  if (schema.type == actualType ||
      (schema.type == "number" && actualType == "integer")) {
    return true;
  } else if (schema.type == "integer" && actualType == "number") {
    this.addError(path, "invalidTypeIntegerNumber");
    return false;
  } else {
    this.addError(path, "invalidType", [schema.type, actualType]);
    return false;
  }
};

/**
 * Adds an error message. |key| is an index into the |messages| object.
 * |replacements| is an array of values to replace '*' characters in the
 * message.
 */
JSONSchemaValidator.prototype.addError = function(path, key, replacements) {
  $Array.push(this.errors, {
    path: path,
    message: JSONSchemaValidator.formatError(key, replacements)
  });
};

/**
 * Resets errors to an empty list so you can call 'validate' again.
 */
JSONSchemaValidator.prototype.resetErrors = function() {
  this.errors = [];
};

exports.$set('JSONSchemaValidator', JSONSchemaValidator);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define('keep_alive', [
    'content/public/renderer/frame_service_registry',
    'extensions/common/mojo/keep_alive.mojom',
    'mojo/public/js/core',
], function(serviceProvider, mojom, core) {

  /**
   * An object that keeps the background page alive until closed.
   * @constructor
   * @alias module:keep_alive~KeepAlive
   */
  function KeepAlive() {
    /**
     * The handle to the keep-alive object in the browser.
     * @type {!MojoHandle}
     * @private
     */
    this.handle_ = serviceProvider.connectToService(mojom.KeepAlive.name);
  }

  /**
   * Removes this keep-alive.
   */
  KeepAlive.prototype.close = function() {
    core.close(this.handle_);
  };

  var exports = {};

  return {
    /**
     * Creates a keep-alive.
     * @return {!module:keep_alive~KeepAlive} A new keep-alive.
     */
    createKeepAlive: function() { return new KeepAlive(); }
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("extensions/common/mojo/keep_alive.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
], function(bindings, codec, connection, core, validator) {


  function KeepAliveProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  KeepAliveProxy.prototype = Object.create(bindings.ProxyBase.prototype);

  function KeepAliveStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  KeepAliveStub.prototype = Object.create(bindings.StubBase.prototype);

  KeepAliveStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    default:
      return false;
    }
  };

  KeepAliveStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateKeepAliveRequest(messageValidator) {
    return validator.validationError.NONE;
  }

  function validateKeepAliveResponse(messageValidator) {
    return validator.validationError.NONE;
  }

  var KeepAlive = {
    name: 'extensions::KeepAlive',
    proxyClass: KeepAliveProxy,
    stubClass: KeepAliveStub,
    validateRequest: validateKeepAliveRequest,
    validateResponse: null,
  };
  KeepAliveStub.prototype.validator = validateKeepAliveRequest;
  KeepAliveProxy.prototype.validator = null;

  var exports = {};
  exports.KeepAlive = KeepAlive;

  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var GetAvailability = requireNative('v8_context').GetAvailability;
var GetGlobal = requireNative('sendRequest').GetGlobal;

// Utility for setting chrome.*.lastError.
//
// A utility here is useful for two reasons:
//  1. For backwards compatibility we need to set chrome.extension.lastError,
//     but not all contexts actually have access to the extension namespace.
//  2. When calling across contexts, the global object that gets lastError set
//     needs to be that of the caller. We force callers to explicitly specify
//     the chrome object to try to prevent bugs here.

/**
 * Sets the last error for |name| on |targetChrome| to |message| with an
 * optional |stack|.
 */
function set(name, message, stack, targetChrome) {
  if (!targetChrome) {
    var errorMessage = name + ': ' + message;
    if (stack != null && stack != '')
      errorMessage += '\n' + stack;
    throw new Error('No chrome object to set error: ' + errorMessage);
  }
  clear(targetChrome);  // in case somebody has set a sneaky getter/setter

  var errorObject = { message: message };
  if (GetAvailability('extension.lastError').is_available)
    targetChrome.extension.lastError = errorObject;

  assertRuntimeIsAvailable();

  // We check to see if developers access runtime.lastError in order to decide
  // whether or not to log it in the (error) console.
  privates(targetChrome.runtime).accessedLastError = false;
  $Object.defineProperty(targetChrome.runtime, 'lastError', {
      configurable: true,
      get: function() {
        privates(targetChrome.runtime).accessedLastError = true;
        return errorObject;
      },
      set: function(error) {
        errorObject = errorObject;
      }});
};

/**
 * Check if anyone has checked chrome.runtime.lastError since it was set.
 * @param {Object} targetChrome the Chrome object to check.
 * @return boolean True if the lastError property was set.
 */
function hasAccessed(targetChrome) {
  assertRuntimeIsAvailable();
  return privates(targetChrome.runtime).accessedLastError === true;
}

/**
 * Check whether there is an error set on |targetChrome| without setting
 * |accessedLastError|.
 * @param {Object} targetChrome the Chrome object to check.
 * @return boolean Whether lastError has been set.
 */
function hasError(targetChrome) {
  if (!targetChrome)
    throw new Error('No target chrome to check');

  assertRuntimeIsAvailable();
  if ('lastError' in targetChrome.runtime)
    return true;

  return false;
};

/**
 * Clears the last error on |targetChrome|.
 */
function clear(targetChrome) {
  if (!targetChrome)
    throw new Error('No target chrome to clear error');

  if (GetAvailability('extension.lastError').is_available)
   delete targetChrome.extension.lastError;

  assertRuntimeIsAvailable();
  delete targetChrome.runtime.lastError;
  delete privates(targetChrome.runtime).accessedLastError;
};

function assertRuntimeIsAvailable() {
  // chrome.runtime should always be available, but maybe it's disappeared for
  // some reason? Add debugging for http://crbug.com/258526.
  var runtimeAvailability = GetAvailability('runtime.lastError');
  if (!runtimeAvailability.is_available) {
    throw new Error('runtime.lastError is not available: ' +
                    runtimeAvailability.message);
  }
  if (!chrome.runtime)
    throw new Error('runtime namespace is null or undefined');
}

/**
 * Runs |callback(args)| with last error args as in set().
 *
 * The target chrome object is the global object's of the callback, so this
 * method won't work if the real callback has been wrapped (etc).
 */
function run(name, message, stack, callback, args) {
  var global = GetGlobal(callback);
  var targetChrome = global && global.chrome;
  set(name, message, stack, targetChrome);
  try {
    $Function.apply(callback, undefined, args);
  } finally {
    reportIfUnchecked(name, targetChrome, stack);
    clear(targetChrome);
  }
}

/**
 * Checks whether chrome.runtime.lastError has been accessed if set.
 * If it was set but not accessed, the error is reported to the console.
 *
 * @param {string=} name - name of API.
 * @param {Object} targetChrome - the Chrome object to check.
 * @param {string=} stack - Stack trace of the call up to the error.
 */
function reportIfUnchecked(name, targetChrome, stack) {
  if (hasAccessed(targetChrome) || !hasError(targetChrome))
    return;
  var message = targetChrome.runtime.lastError.message;
  console.error("Unchecked runtime.lastError while running " +
      (name || "unknown") + ": " + message + (stack ? "\n" + stack : ""));
}

exports.$set('clear', clear);
exports.$set('hasAccessed', hasAccessed);
exports.$set('hasError', hasError);
exports.$set('set', set);
exports.$set('run', run);
exports.$set('reportIfUnchecked', reportIfUnchecked);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// chrome.runtime.messaging API implementation.
// TODO(robwu): Fix this indentation.

  // TODO(kalman): factor requiring chrome out of here.
  var chrome = requireNative('chrome').GetChrome();
  var Event = require('event_bindings').Event;
  var lastError = require('lastError');
  var logActivity = requireNative('activityLogger');
  var logging = requireNative('logging');
  var messagingNatives = requireNative('messaging_natives');
  var processNatives = requireNative('process');
  var utils = require('utils');
  var messagingUtils = require('messaging_utils');

  // The reserved channel name for the sendRequest/send(Native)Message APIs.
  // Note: sendRequest is deprecated.
  var kRequestChannel = "chrome.extension.sendRequest";
  var kMessageChannel = "chrome.runtime.sendMessage";
  var kNativeMessageChannel = "chrome.runtime.sendNativeMessage";

  // Map of port IDs to port object.
  var ports = {__proto__: null};

  // Change even to odd and vice versa, to get the other side of a given
  // channel.
  function getOppositePortId(portId) { return portId ^ 1; }

  // Port object.  Represents a connection to another script context through
  // which messages can be passed.
  function PortImpl(portId, opt_name) {
    this.portId_ = portId;
    this.name = opt_name;

    // Note: Keep these schemas in sync with the documentation in runtime.json
    var portSchema = {
      __proto__: null,
      name: 'port',
      $ref: 'runtime.Port',
    };
    var messageSchema = {
      __proto__: null,
      name: 'message',
      type: 'any',
      optional: true,
    };
    var options = {
      __proto__: null,
      unmanaged: true,
    };
    this.onDisconnect = new Event(null, [portSchema], options);
    this.onMessage = new Event(null, [messageSchema, portSchema], options);
    this.onDestroy_ = null;
  }
  $Object.setPrototypeOf(PortImpl.prototype, null);

  // Sends a message asynchronously to the context on the other end of this
  // port.
  PortImpl.prototype.postMessage = function(msg) {
    // JSON.stringify doesn't support a root object which is undefined.
    if (msg === undefined)
      msg = null;
    msg = $JSON.stringify(msg);
    if (msg === undefined) {
      // JSON.stringify can fail with unserializable objects. Log an error and
      // drop the message.
      //
      // TODO(kalman/mpcomplete): it would be better to do the same validation
      // here that we do for runtime.sendMessage (and variants), i.e. throw an
      // schema validation Error, but just maintain the old behaviour until
      // there's a good reason not to (http://crbug.com/263077).
      console.error('Illegal argument to Port.postMessage');
      return;
    }
    messagingNatives.PostMessage(this.portId_, msg);
  };

  // Disconnects the port from the other end.
  PortImpl.prototype.disconnect = function() {
    messagingNatives.CloseChannel(this.portId_, true);
    this.destroy_();
  };

  PortImpl.prototype.destroy_ = function() {
    if (this.onDestroy_) {
      this.onDestroy_();
      this.onDestroy_ = null;
    }
    privates(this.onDisconnect).impl.destroy_();
    privates(this.onMessage).impl.destroy_();
    // TODO(robwu): Remove port lifetime management because it is completely
    // handled in the browser. The renderer's only roles are
    // 1) rejecting ports so that the browser knows that the renderer is not
    //    interested in the port (this is merely an optimization)
    // 2) acknowledging port creations, so that the browser knows that the port
    //    was successfully created (from the perspective of the extension), but
    //    then closed for some non-fatal reason.
    // 3) notifying the browser of explicit port closure via .disconnect().
    // In other cases (navigations), the browser automatically cleans up the
    //    port.
    messagingNatives.PortRelease(this.portId_);
    delete ports[this.portId_];
  };

  // Returns true if the specified port id is in this context. This is used by
  // the C++ to avoid creating the javascript message for all the contexts that
  // don't care about a particular message.
  function hasPort(portId) {
    return portId in ports;
  };

  // Hidden port creation function.  We don't want to expose an API that lets
  // people add arbitrary port IDs to the port list.
  function createPort(portId, opt_name) {
    if (ports[portId])
      throw new Error("Port '" + portId + "' already exists.");
    var port = new Port(portId, opt_name);
    ports[portId] = port;
    messagingNatives.PortAddRef(portId);
    return port;
  };

  // Helper function for dispatchOnRequest.
  function handleSendRequestError(isSendMessage,
                                  responseCallbackPreserved,
                                  sourceExtensionId,
                                  targetExtensionId,
                                  sourceUrl) {
    var errorMsg;
    var eventName = isSendMessage ? 'runtime.onMessage' : 'extension.onRequest';
    if (isSendMessage && !responseCallbackPreserved) {
      errorMsg =
        'The chrome.' + eventName + ' listener must return true if you ' +
        'want to send a response after the listener returns';
    } else {
      errorMsg =
        'Cannot send a response more than once per chrome.' + eventName +
        ' listener per document';
    }
    errorMsg += ' (message was sent by extension' + sourceExtensionId;
    if (sourceExtensionId && sourceExtensionId !== targetExtensionId)
      errorMsg += ' for extension ' + targetExtensionId;
    if (sourceUrl)
      errorMsg += ' for URL ' + sourceUrl;
    errorMsg += ').';
    lastError.set(eventName, errorMsg, null, chrome);
  }

  // Helper function for dispatchOnConnect
  function dispatchOnRequest(portId, channelName, sender,
                             sourceExtensionId, targetExtensionId, sourceUrl,
                             isExternal) {
    var isSendMessage = channelName == kMessageChannel;
    var requestEvent = null;
    if (isSendMessage) {
      if (chrome.runtime) {
        requestEvent = isExternal ? chrome.runtime.onMessageExternal
                                  : chrome.runtime.onMessage;
      }
    } else {
      if (chrome.extension) {
        requestEvent = isExternal ? chrome.extension.onRequestExternal
                                  : chrome.extension.onRequest;
      }
    }
    if (!requestEvent)
      return false;
    if (!requestEvent.hasListeners())
      return false;
    var port = createPort(portId, channelName);

    function messageListener(request) {
      var responseCallbackPreserved = false;
      var responseCallback = function(response) {
        if (port) {
          port.postMessage(response);
          privates(port).impl.destroy_();
          port = null;
        } else {
          // We nulled out port when sending the response, and now the page
          // is trying to send another response for the same request.
          handleSendRequestError(isSendMessage, responseCallbackPreserved,
                                 sourceExtensionId, targetExtensionId);
        }
      };
      // In case the extension never invokes the responseCallback, and also
      // doesn't keep a reference to it, we need to clean up the port. Do
      // so by attaching to the garbage collection of the responseCallback
      // using some native hackery.
      //
      // If the context is destroyed before this has a chance to execute,
      // BindToGC knows to release |portId| (important for updating C++ state
      // both in this renderer and on the other end). We don't need to clear
      // any JavaScript state, as calling destroy_() would usually do - but
      // the context has been destroyed, so there isn't any JS state to clear.
      messagingNatives.BindToGC(responseCallback, function() {
        if (port) {
          privates(port).impl.destroy_();
          port = null;
        }
      }, portId);
      var rv = requestEvent.dispatch(request, sender, responseCallback);
      if (isSendMessage) {
        responseCallbackPreserved =
            rv && rv.results && $Array.indexOf(rv.results, true) > -1;
        if (!responseCallbackPreserved && port) {
          // If they didn't access the response callback, they're not
          // going to send a response, so clean up the port immediately.
          privates(port).impl.destroy_();
          port = null;
        }
      }
    }

    privates(port).impl.onDestroy_ = function() {
      port.onMessage.removeListener(messageListener);
    };
    port.onMessage.addListener(messageListener);

    var eventName = isSendMessage ? "runtime.onMessage" : "extension.onRequest";
    if (isExternal)
      eventName += "External";
    logActivity.LogEvent(targetExtensionId,
                         eventName,
                         [sourceExtensionId, sourceUrl]);
    return true;
  }

  // Called by native code when a channel has been opened to this context.
  function dispatchOnConnect(portId,
                             channelName,
                             sourceTab,
                             sourceFrameId,
                             guestProcessId,
                             guestRenderFrameRoutingId,
                             sourceExtensionId,
                             targetExtensionId,
                             sourceUrl,
                             tlsChannelId) {
    // Only create a new Port if someone is actually listening for a connection.
    // In addition to being an optimization, this also fixes a bug where if 2
    // channels were opened to and from the same process, closing one would
    // close both.
    var extensionId = processNatives.GetExtensionId();

    // messaging_bindings.cc should ensure that this method only gets called for
    // the right extension.
    logging.CHECK(targetExtensionId == extensionId);

    if (ports[getOppositePortId(portId)])
      return false;  // this channel was opened by us, so ignore it

    // Determine whether this is coming from another extension, so we can use
    // the right event.
    var isExternal = sourceExtensionId != extensionId;

    var sender = {};
    if (sourceExtensionId != '')
      sender.id = sourceExtensionId;
    if (sourceUrl)
      sender.url = sourceUrl;
    if (sourceTab)
      sender.tab = sourceTab;
    if (sourceFrameId >= 0)
      sender.frameId = sourceFrameId;
    if (typeof guestProcessId !== 'undefined' &&
        typeof guestRenderFrameRoutingId !== 'undefined') {
      // Note that |guestProcessId| and |guestRenderFrameRoutingId| are not
      // standard fields on MessageSender and should not be exposed to drive-by
      // extensions; it is only exposed to component extensions.
      logging.CHECK(processNatives.IsComponentExtension(),
          "GuestProcessId can only be exposed to component extensions.");
      sender.guestProcessId = guestProcessId;
      sender.guestRenderFrameRoutingId = guestRenderFrameRoutingId;
    }
    if (typeof tlsChannelId != 'undefined')
      sender.tlsChannelId = tlsChannelId;

    // Special case for sendRequest/onRequest and sendMessage/onMessage.
    if (channelName == kRequestChannel || channelName == kMessageChannel) {
      return dispatchOnRequest(portId, channelName, sender,
                               sourceExtensionId, targetExtensionId, sourceUrl,
                               isExternal);
    }

    var connectEvent = null;
    if (chrome.runtime) {
      connectEvent = isExternal ? chrome.runtime.onConnectExternal
                                : chrome.runtime.onConnect;
    }
    if (!connectEvent)
      return false;
    if (!connectEvent.hasListeners())
      return false;

    var port = createPort(portId, channelName);
    port.sender = sender;
    if (processNatives.manifestVersion < 2)
      port.tab = port.sender.tab;

    var eventName = (isExternal ?
        "runtime.onConnectExternal" : "runtime.onConnect");
    connectEvent.dispatch(port);
    logActivity.LogEvent(targetExtensionId,
                         eventName,
                         [sourceExtensionId]);
    return true;
  };

  // Called by native code when a channel has been closed.
  function dispatchOnDisconnect(portId, errorMessage) {
    var port = ports[portId];
    if (port) {
      // Update the renderer's port bookkeeping, without notifying the browser.
      messagingNatives.CloseChannel(portId, false);
      if (errorMessage)
        lastError.set('Port', errorMessage, null, chrome);
      try {
        port.onDisconnect.dispatch(port);
      } finally {
        privates(port).impl.destroy_();
        lastError.clear(chrome);
      }
    }
  };

  // Called by native code when a message has been sent to the given port.
  function dispatchOnMessage(msg, portId) {
    var port = ports[portId];
    if (port) {
      if (msg)
        msg = $JSON.parse(msg);
      port.onMessage.dispatch(msg, port);
    }
  };

  // Shared implementation used by tabs.sendMessage and runtime.sendMessage.
  function sendMessageImpl(port, request, responseCallback) {
    if (port.name != kNativeMessageChannel)
      port.postMessage(request);

    if (port.name == kMessageChannel && !responseCallback) {
      // TODO(mpcomplete): Do this for the old sendRequest API too, after
      // verifying it doesn't break anything.
      // Go ahead and disconnect immediately if the sender is not expecting
      // a response.
      port.disconnect();
      return;
    }

    function sendResponseAndClearCallback(response) {
      // Save a reference so that we don't re-entrantly call responseCallback.
      var sendResponse = responseCallback;
      responseCallback = null;
      if (arguments.length === 0) {
        // According to the documentation of chrome.runtime.sendMessage, the
        // callback is invoked without any arguments when an error occurs.
        sendResponse();
      } else {
        sendResponse(response);
      }
    }


    // Note: make sure to manually remove the onMessage/onDisconnect listeners
    // that we added before destroying the Port, a workaround to a bug in Port
    // where any onMessage/onDisconnect listeners added but not removed will
    // be leaked when the Port is destroyed.
    // http://crbug.com/320723 tracks a sustainable fix.

    function disconnectListener() {
      if (!responseCallback)
        return;

      if (lastError.hasError(chrome)) {
        sendResponseAndClearCallback();
      } else {
        lastError.set(
            port.name, 'The message port closed before a reponse was received.',
            null, chrome);
        try {
          sendResponseAndClearCallback();
        } finally {
          lastError.clear(chrome);
        }
      }
    }

    function messageListener(response) {
      try {
        if (responseCallback)
          sendResponseAndClearCallback(response);
      } finally {
        port.disconnect();
      }
    }

    privates(port).impl.onDestroy_ = function() {
      port.onDisconnect.removeListener(disconnectListener);
      port.onMessage.removeListener(messageListener);
    };
    port.onDisconnect.addListener(disconnectListener);
    port.onMessage.addListener(messageListener);
  };

  function sendMessageUpdateArguments(functionName, hasOptionsArgument) {
    // skip functionName and hasOptionsArgument
    var args = $Array.slice(arguments, 2);
    var alignedArgs = messagingUtils.alignSendMessageArguments(args,
        hasOptionsArgument);
    if (!alignedArgs)
      throw new Error('Invalid arguments to ' + functionName + '.');
    return alignedArgs;
  }

  function Port() {
    privates(Port).constructPrivate(this, arguments);
  }
  utils.expose(Port, PortImpl, {
    functions: [
      'disconnect',
      'postMessage',
    ],
    properties: [
      'name',
      'onDisconnect',
      'onMessage',
    ],
  });

exports.$set('kRequestChannel', kRequestChannel);
exports.$set('kMessageChannel', kMessageChannel);
exports.$set('kNativeMessageChannel', kNativeMessageChannel);
exports.$set('Port', Port);
exports.$set('createPort', createPort);
exports.$set('sendMessageImpl', sendMessageImpl);
exports.$set('sendMessageUpdateArguments', sendMessageUpdateArguments);

// For C++ code to call.
exports.$set('hasPort', hasPort);
exports.$set('dispatchOnConnect', dispatchOnConnect);
exports.$set('dispatchOnDisconnect', dispatchOnDisconnect);
exports.$set('dispatchOnMessage', dispatchOnMessage);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Routines used to normalize arguments to messaging functions.

function alignSendMessageArguments(args, hasOptionsArgument) {
  // Align missing (optional) function arguments with the arguments that
  // schema validation is expecting, e.g.
  //   extension.sendRequest(req)     -> extension.sendRequest(null, req)
  //   extension.sendRequest(req, cb) -> extension.sendRequest(null, req, cb)
  if (!args || !args.length)
    return null;
  var lastArg = args.length - 1;

  // responseCallback (last argument) is optional.
  var responseCallback = null;
  if (typeof args[lastArg] == 'function')
    responseCallback = args[lastArg--];

  var options = null;
  if (hasOptionsArgument && lastArg >= 1) {
    // options (third argument) is optional. It can also be ambiguous which
    // argument it should match. If there are more than two arguments remaining,
    // options is definitely present:
    if (lastArg > 1) {
      options = args[lastArg--];
    } else {
      // Exactly two arguments remaining. If the first argument is a string,
      // it should bind to targetId, and the second argument should bind to
      // request, which is required. In other words, when two arguments remain,
      // only bind options when the first argument cannot bind to targetId.
      if (!(args[0] === null || typeof args[0] == 'string'))
        options = args[lastArg--];
    }
  }

  // request (second argument) is required.
  var request = args[lastArg--];

  // targetId (first argument, extensionId in the manifest) is optional.
  var targetId = null;
  if (lastArg >= 0)
    targetId = args[lastArg--];

  if (lastArg != -1)
    return null;
  if (hasOptionsArgument)
    return [targetId, request, options, responseCallback];
  return [targetId, request, responseCallback];
}

exports.$set('alignSendMessageArguments', alignSendMessageArguments);
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Custom bindings for the mime handler API.
 */

var binding = require('binding').Binding.create('mimeHandlerPrivate');

var NO_STREAM_ERROR =
    'Streams are only available from a mime handler view guest.';
var STREAM_ABORTED_ERROR = 'Stream has been aborted.';

var servicePromise = Promise.all([
    requireAsync('content/public/renderer/frame_service_registry'),
    requireAsync('extensions/common/api/mime_handler.mojom'),
    requireAsync('mojo/public/js/router'),
]).then(function(modules) {
  var serviceProvider = modules[0];
  var mojom = modules[1];
  var routerModule = modules[2];
  return new mojom.MimeHandlerService.proxyClass(new routerModule.Router(
      serviceProvider.connectToService(mojom.MimeHandlerService.name)));
});

// Stores a promise to the GetStreamInfo() result to avoid making additional
// calls in response to getStreamInfo() calls.
var streamInfoPromise;

function throwNoStreamError() {
  throw new Error(NO_STREAM_ERROR);
}

function createStreamInfoPromise() {
  return servicePromise.then(function(service) {
    return service.getStreamInfo();
  }).then(function(result) {
    if (!result.stream_info)
      throw new Error(STREAM_ABORTED_ERROR);
    return result.stream_info;
  }, throwNoStreamError);
}

function constructStreamInfoDict(streamInfo) {
  var headers = {};
  for (var header of streamInfo.response_headers) {
    headers[header[0]] = header[1];
  }
  return {
    mimeType: streamInfo.mime_type,
    originalUrl: streamInfo.original_url,
    streamUrl: streamInfo.stream_url,
    tabId: streamInfo.tab_id,
    embedded: !!streamInfo.embedded,
    responseHeaders: headers,
  };
}

binding.registerCustomHook(function(bindingsAPI) {
  var apiFunctions = bindingsAPI.apiFunctions;
  apiFunctions.setHandleRequestWithPromise('getStreamInfo', function() {
    if (!streamInfoPromise)
      streamInfoPromise = createStreamInfoPromise();
    return streamInfoPromise.then(constructStreamInfoDict);
  });

  apiFunctions.setHandleRequestWithPromise('abortStream', function() {
    return servicePromise.then(function(service) {
      return service.abortStream().then(function() {});
    }).catch(throwNoStreamError);
  });
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("extensions/common/api/mime_handler.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
], function(bindings, codec, connection, core, validator) {

  function StreamInfo(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  StreamInfo.prototype.initDefaults_ = function() {
    this.mime_type = null;
    this.original_url = null;
    this.stream_url = null;
    this.tab_id = 0;
    this.embedded = false;
    this.response_headers = null;
  };
  StreamInfo.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  StreamInfo.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, StreamInfo.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StreamInfo.mime_type
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StreamInfo.original_url
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StreamInfo.stream_url
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;



    
    // validate StreamInfo.response_headers
    err = messageValidator.validateMapPointer(offset + codec.kStructHeaderSize + 32, false, codec.String, codec.String, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  StreamInfo.encodedSize = codec.kStructHeaderSize + 40;

  StreamInfo.decode = function(decoder) {
    var packed;
    var val = new StreamInfo();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.mime_type = decoder.decodeStruct(codec.String);
    val.original_url = decoder.decodeStruct(codec.String);
    val.stream_url = decoder.decodeStruct(codec.String);
    val.tab_id = decoder.decodeStruct(codec.Int32);
    val.embedded = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.response_headers = decoder.decodeMapPointer(codec.String, codec.String);
    return val;
  };

  StreamInfo.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(StreamInfo.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.mime_type);
    encoder.encodeStruct(codec.String, val.original_url);
    encoder.encodeStruct(codec.String, val.stream_url);
    encoder.encodeStruct(codec.Int32, val.tab_id);
    encoder.encodeStruct(codec.Uint8, val.embedded);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeMapPointer(codec.String, codec.String, val.response_headers);
  };
  function MimeHandlerService_GetStreamInfo_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MimeHandlerService_GetStreamInfo_Params.prototype.initDefaults_ = function() {
  };
  MimeHandlerService_GetStreamInfo_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MimeHandlerService_GetStreamInfo_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MimeHandlerService_GetStreamInfo_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MimeHandlerService_GetStreamInfo_Params.encodedSize = codec.kStructHeaderSize + 0;

  MimeHandlerService_GetStreamInfo_Params.decode = function(decoder) {
    var packed;
    var val = new MimeHandlerService_GetStreamInfo_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  MimeHandlerService_GetStreamInfo_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MimeHandlerService_GetStreamInfo_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function MimeHandlerService_GetStreamInfo_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MimeHandlerService_GetStreamInfo_ResponseParams.prototype.initDefaults_ = function() {
    this.stream_info = null;
  };
  MimeHandlerService_GetStreamInfo_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MimeHandlerService_GetStreamInfo_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MimeHandlerService_GetStreamInfo_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MimeHandlerService_GetStreamInfo_ResponseParams.stream_info
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, StreamInfo, true);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MimeHandlerService_GetStreamInfo_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  MimeHandlerService_GetStreamInfo_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MimeHandlerService_GetStreamInfo_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.stream_info = decoder.decodeStructPointer(StreamInfo);
    return val;
  };

  MimeHandlerService_GetStreamInfo_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MimeHandlerService_GetStreamInfo_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(StreamInfo, val.stream_info);
  };
  function MimeHandlerService_AbortStream_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MimeHandlerService_AbortStream_Params.prototype.initDefaults_ = function() {
  };
  MimeHandlerService_AbortStream_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MimeHandlerService_AbortStream_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MimeHandlerService_AbortStream_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MimeHandlerService_AbortStream_Params.encodedSize = codec.kStructHeaderSize + 0;

  MimeHandlerService_AbortStream_Params.decode = function(decoder) {
    var packed;
    var val = new MimeHandlerService_AbortStream_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  MimeHandlerService_AbortStream_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MimeHandlerService_AbortStream_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function MimeHandlerService_AbortStream_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MimeHandlerService_AbortStream_ResponseParams.prototype.initDefaults_ = function() {
  };
  MimeHandlerService_AbortStream_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MimeHandlerService_AbortStream_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MimeHandlerService_AbortStream_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MimeHandlerService_AbortStream_ResponseParams.encodedSize = codec.kStructHeaderSize + 0;

  MimeHandlerService_AbortStream_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MimeHandlerService_AbortStream_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  MimeHandlerService_AbortStream_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MimeHandlerService_AbortStream_ResponseParams.encodedSize);
    encoder.writeUint32(0);
  };
  var kMimeHandlerService_GetStreamInfo_Name = 0;
  var kMimeHandlerService_AbortStream_Name = 1;

  function MimeHandlerServiceProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  MimeHandlerServiceProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  MimeHandlerServiceProxy.prototype.getStreamInfo = function() {
    var params = new MimeHandlerService_GetStreamInfo_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMimeHandlerService_GetStreamInfo_Name,
          codec.align(MimeHandlerService_GetStreamInfo_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MimeHandlerService_GetStreamInfo_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MimeHandlerService_GetStreamInfo_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MimeHandlerServiceProxy.prototype.abortStream = function() {
    var params = new MimeHandlerService_AbortStream_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMimeHandlerService_AbortStream_Name,
          codec.align(MimeHandlerService_AbortStream_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MimeHandlerService_AbortStream_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MimeHandlerService_AbortStream_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };

  function MimeHandlerServiceStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  MimeHandlerServiceStub.prototype = Object.create(bindings.StubBase.prototype);
  MimeHandlerServiceStub.prototype.getStreamInfo = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.getStreamInfo && bindings.StubBindings(this).delegate.getStreamInfo();
  }
  MimeHandlerServiceStub.prototype.abortStream = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.abortStream && bindings.StubBindings(this).delegate.abortStream();
  }

  MimeHandlerServiceStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    default:
      return false;
    }
  };

  MimeHandlerServiceStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kMimeHandlerService_GetStreamInfo_Name:
      var params = reader.decodeStruct(MimeHandlerService_GetStreamInfo_Params);
      return this.getStreamInfo().then(function(response) {
        var responseParams =
            new MimeHandlerService_GetStreamInfo_ResponseParams();
        responseParams.stream_info = response.stream_info;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMimeHandlerService_GetStreamInfo_Name,
            codec.align(MimeHandlerService_GetStreamInfo_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MimeHandlerService_GetStreamInfo_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMimeHandlerService_AbortStream_Name:
      var params = reader.decodeStruct(MimeHandlerService_AbortStream_Params);
      return this.abortStream().then(function(response) {
        var responseParams =
            new MimeHandlerService_AbortStream_ResponseParams();
        var builder = new codec.MessageWithRequestIDBuilder(
            kMimeHandlerService_AbortStream_Name,
            codec.align(MimeHandlerService_AbortStream_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MimeHandlerService_AbortStream_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateMimeHandlerServiceRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kMimeHandlerService_GetStreamInfo_Name:
        if (message.expectsResponse())
          paramsClass = MimeHandlerService_GetStreamInfo_Params;
      break;
      case kMimeHandlerService_AbortStream_Name:
        if (message.expectsResponse())
          paramsClass = MimeHandlerService_AbortStream_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateMimeHandlerServiceResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kMimeHandlerService_GetStreamInfo_Name:
        if (message.isResponse())
          paramsClass = MimeHandlerService_GetStreamInfo_ResponseParams;
        break;
      case kMimeHandlerService_AbortStream_Name:
        if (message.isResponse())
          paramsClass = MimeHandlerService_AbortStream_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var MimeHandlerService = {
    name: 'extensions::mime_handler::MimeHandlerService',
    proxyClass: MimeHandlerServiceProxy,
    stubClass: MimeHandlerServiceStub,
    validateRequest: validateMimeHandlerServiceRequest,
    validateResponse: validateMimeHandlerServiceResponse,
  };
  MimeHandlerServiceStub.prototype.validator = validateMimeHandlerServiceRequest;
  MimeHandlerServiceProxy.prototype.validator = validateMimeHandlerServiceResponse;

  var exports = {};
  exports.StreamInfo = StreamInfo;
  exports.MimeHandlerService = MimeHandlerService;

  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Routines used to validate and normalize arguments.
// TODO(benwells): unit test this file.

var JSONSchemaValidator = require('json_schema').JSONSchemaValidator;

var schemaValidator = new JSONSchemaValidator();

// Validate arguments.
function validate(args, parameterSchemas) {
  if (args.length > parameterSchemas.length)
    throw new Error("Too many arguments.");
  for (var i = 0; i < parameterSchemas.length; i++) {
    if (i in args && args[i] !== null && args[i] !== undefined) {
      schemaValidator.resetErrors();
      schemaValidator.validate(args[i], parameterSchemas[i]);
      if (schemaValidator.errors.length == 0)
        continue;
      var message = "Invalid value for argument " + (i + 1) + ". ";
      for (var i = 0, err;
          err = schemaValidator.errors[i]; i++) {
        if (err.path) {
          message += "Property '" + err.path + "': ";
        }
        message += err.message;
        message = message.substring(0, message.length - 1);
        message += ", ";
      }
      message = message.substring(0, message.length - 2);
      message += ".";
      throw new Error(message);
    } else if (!parameterSchemas[i].optional) {
      throw new Error("Parameter " + (i + 1) + " (" +
          parameterSchemas[i].name + ") is required.");
    }
  }
}

// Generate all possible signatures for a given API function.
function getSignatures(parameterSchemas) {
  if (parameterSchemas.length === 0)
    return [[]];
  var signatures = [];
  var remaining = getSignatures($Array.slice(parameterSchemas, 1));
  for (var i = 0; i < remaining.length; i++)
    $Array.push(signatures, $Array.concat([parameterSchemas[0]], remaining[i]))
  if (parameterSchemas[0].optional)
    return $Array.concat(signatures, remaining);
  return signatures;
};

// Return true if arguments match a given signature's schema.
function argumentsMatchSignature(args, candidateSignature) {
  if (args.length != candidateSignature.length)
    return false;
  for (var i = 0; i < candidateSignature.length; i++) {
    var argType =  JSONSchemaValidator.getType(args[i]);
    if (!schemaValidator.isValidSchemaType(argType,
        candidateSignature[i]))
      return false;
  }
  return true;
};

// Finds the function signature for the given arguments.
function resolveSignature(args, definedSignature) {
  var candidateSignatures = getSignatures(definedSignature);
  for (var i = 0; i < candidateSignatures.length; i++) {
    if (argumentsMatchSignature(args, candidateSignatures[i]))
      return candidateSignatures[i];
  }
  return null;
};

// Returns a string representing the defined signature of the API function.
// Example return value for chrome.windows.getCurrent:
// "windows.getCurrent(optional object populate, function callback)"
function getParameterSignatureString(name, definedSignature) {
  var getSchemaTypeString = function(schema) {
    var schemaTypes = schemaValidator.getAllTypesForSchema(schema);
    var typeName = schemaTypes.join(" or ") + " " + schema.name;
    if (schema.optional)
      return "optional " + typeName;
    return typeName;
  };
  var typeNames = $Array.map(definedSignature, getSchemaTypeString);
  return name + "(" + typeNames.join(", ") + ")";
};

// Returns a string representing a call to an API function.
// Example return value for call: chrome.windows.get(1, callback) is:
// "windows.get(int, function)"
function getArgumentSignatureString(name, args) {
  var typeNames = $Array.map(args, JSONSchemaValidator.getType);
  return name + "(" + typeNames.join(", ") + ")";
};

// Finds the correct signature for the given arguments, then validates the
// arguments against that signature. Returns a 'normalized' arguments list
// where nulls are inserted where optional parameters were omitted.
// |args| is expected to be an array.
function normalizeArgumentsAndValidate(args, funDef) {
  if (funDef.allowAmbiguousOptionalArguments) {
    validate(args, funDef.definition.parameters);
    return args;
  }
  var definedSignature = funDef.definition.parameters;
  var resolvedSignature = resolveSignature(args, definedSignature);
  if (!resolvedSignature)
    throw new Error("Invocation of form " +
        getArgumentSignatureString(funDef.name, args) +
        " doesn't match definition " +
        getParameterSignatureString(funDef.name, definedSignature));
  validate(args, resolvedSignature);
  var normalizedArgs = [];
  var ai = 0;
  for (var si = 0; si < definedSignature.length; si++) {
    // Handle integer -0 as 0.
    if (JSONSchemaValidator.getType(args[ai]) === "integer" && args[ai] === 0)
      args[ai] = 0;
    if (definedSignature[si] === resolvedSignature[ai])
      $Array.push(normalizedArgs, args[ai++]);
    else
      $Array.push(normalizedArgs, null);
  }
  return normalizedArgs;
};

// Validates that a given schema for an API function is not ambiguous.
function isFunctionSignatureAmbiguous(functionDef) {
  if (functionDef.allowAmbiguousOptionalArguments)
    return false;
  var signaturesAmbiguous = function(signature1, signature2) {
    if (signature1.length != signature2.length)
      return false;
    for (var i = 0; i < signature1.length; i++) {
      if (!schemaValidator.checkSchemaOverlap(
          signature1[i], signature2[i]))
        return false;
    }
    return true;
  };
  var candidateSignatures = getSignatures(functionDef.parameters);
  for (var i = 0; i < candidateSignatures.length; i++) {
    for (var j = i + 1; j < candidateSignatures.length; j++) {
      if (signaturesAmbiguous(candidateSignatures[i], candidateSignatures[j]))
        return true;
    }
  }
  return false;
};

exports.$set('isFunctionSignatureAmbiguous', isFunctionSignatureAmbiguous);
exports.$set('normalizeArgumentsAndValidate', normalizeArgumentsAndValidate);
exports.$set('schemaValidator', schemaValidator);
exports.$set('validate', validate);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var exceptionHandler = require('uncaught_exception_handler');
var lastError = require('lastError');
var logging = requireNative('logging');
var natives = requireNative('sendRequest');
var validate = require('schemaUtils').validate;

// All outstanding requests from sendRequest().
var requests = {};

// Used to prevent double Activity Logging for API calls that use both custom
// bindings and ExtensionFunctions (via sendRequest).
var calledSendRequest = false;

// Runs a user-supplied callback safely.
function safeCallbackApply(name, request, callback, args) {
  try {
    $Function.apply(callback, request, args);
  } catch (e) {
    exceptionHandler.handle('Error in response to ' + name, e, request.stack);
  }
}

// Callback handling.
function handleResponse(requestId, name, success, responseList, error) {
  // The chrome objects we will set lastError on. Really we should only be
  // setting this on the callback's chrome object, but set on ours too since
  // it's conceivable that something relies on that.
  var callerChrome = chrome;

  try {
    var request = requests[requestId];
    logging.DCHECK(request != null);

    // lastError needs to be set on the caller's chrome object no matter what,
    // though chances are it's the same as ours (it will be different when
    // calling API methods on other contexts).
    if (request.callback) {
      var global = natives.GetGlobal(request.callback);
      callerChrome = global ? global.chrome : callerChrome;
    }

    lastError.clear(chrome);
    if (callerChrome !== chrome)
      lastError.clear(callerChrome);

    if (!success) {
      if (!error)
        error = "Unknown error.";
      lastError.set(name, error, request.stack, chrome);
      if (callerChrome !== chrome)
        lastError.set(name, error, request.stack, callerChrome);
    }

    if (request.customCallback) {
      safeCallbackApply(name,
                        request,
                        request.customCallback,
                        $Array.concat([name, request, request.callback],
                                      responseList));
    } else if (request.callback) {
      // Validate callback in debug only -- and only when the
      // caller has provided a callback. Implementations of api
      // calls may not return data if they observe the caller
      // has not provided a callback.
      if (logging.DCHECK_IS_ON() && !error) {
        if (!request.callbackSchema.parameters)
          throw new Error(name + ": no callback schema defined");
        validate(responseList, request.callbackSchema.parameters);
      }
      safeCallbackApply(name, request, request.callback, responseList);
    }

    if (error && !lastError.hasAccessed(chrome)) {
      // The native call caused an error, but the developer might not have
      // checked runtime.lastError.
      lastError.reportIfUnchecked(name, callerChrome, request.stack);
    }
  } finally {
    delete requests[requestId];
    lastError.clear(chrome);
    if (callerChrome !== chrome)
      lastError.clear(callerChrome);
  }
}

function prepareRequest(args, argSchemas) {
  var request = {};
  var argCount = args.length;

  // Look for callback param.
  if (argSchemas.length > 0 &&
      argSchemas[argSchemas.length - 1].type == "function") {
    request.callback = args[args.length - 1];
    request.callbackSchema = argSchemas[argSchemas.length - 1];
    --argCount;
  }

  request.args = [];
  for (var k = 0; k < argCount; k++) {
    request.args[k] = args[k];
  }

  return request;
}

// Send an API request and optionally register a callback.
// |optArgs| is an object with optional parameters as follows:
// - customCallback: a callback that should be called instead of the standard
//   callback.
// - forIOThread: true if this function should be handled on the browser IO
//   thread.
// - preserveNullInObjects: true if it is safe for null to be in objects.
// - stack: An optional string that contains the stack trace, to be displayed
//   to the user if an error occurs.
function sendRequest(functionName, args, argSchemas, optArgs) {
  calledSendRequest = true;
  if (!optArgs)
    optArgs = {};
  var request = prepareRequest(args, argSchemas);
  request.stack = optArgs.stack || exceptionHandler.getExtensionStackTrace();
  if (optArgs.customCallback) {
    request.customCallback = optArgs.customCallback;
  }

  var hasCallback = request.callback || optArgs.customCallback;
  var requestId =
      natives.StartRequest(functionName, request.args, hasCallback,
                           optArgs.forIOThread, optArgs.preserveNullInObjects);
  request.id = requestId;
  requests[requestId] = request;
}

function getCalledSendRequest() {
  return calledSendRequest;
}

function clearCalledSendRequest() {
  calledSendRequest = false;
}

exports.$set('sendRequest', sendRequest);
exports.$set('getCalledSendRequest', getCalledSendRequest);
exports.$set('clearCalledSendRequest', clearCalledSendRequest);
exports.$set('safeCallbackApply', safeCallbackApply);

// Called by C++.
exports.$set('handleResponse', handleResponse);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Custom bindings for the Serial API.
 *
 * The bindings are implemented by asynchronously delegating to the
 * serial_service module. The functions that apply to a particular connection
 * are delegated to the appropriate method on the Connection object specified by
 * the ID parameter.
 */

var binding = require('binding').Binding.create('serial');
var context = requireNative('v8_context');
var eventBindings = require('event_bindings');
var utils = require('utils');

var serialServicePromise = function() {
  // getBackgroundPage is not available in unit tests so fall back to the
  // current page's serial_service module.
  if (!chrome.runtime.getBackgroundPage)
    return requireAsync('serial_service');

  // Load the serial_service module from the background page if one exists. This
  // is necessary for serial connections created in one window to be usable
  // after that window is closed. This is necessary because the Mojo async
  // waiter only functions while the v8 context remains.
  return utils.promise(chrome.runtime.getBackgroundPage).then(function(bgPage) {
    return context.GetModuleSystem(bgPage).requireAsync('serial_service');
  }).catch(function() {
    return requireAsync('serial_service');
  });
}();

function forwardToConnection(methodName) {
  return function(connectionId) {
    var args = $Array.slice(arguments, 1);
    return serialServicePromise.then(function(serialService) {
      return serialService.getConnection(connectionId);
    }).then(function(connection) {
      return $Function.apply(connection[methodName], connection, args);
    });
  };
}

function addEventListeners(connection, id) {
  connection.onData = function(data) {
    eventBindings.dispatchEvent(
        'serial.onReceive', [{connectionId: id, data: data}]);
  };
  connection.onError = function(error) {
    eventBindings.dispatchEvent(
        'serial.onReceiveError', [{connectionId: id, error: error}]);
  };
}

serialServicePromise.then(function(serialService) {
  return serialService.getConnections().then(function(connections) {
    for (var entry of connections) {
      var connection = entry[1];
      addEventListeners(connection, entry[0]);
      connection.resumeReceives();
    };
  });
});

binding.registerCustomHook(function(bindingsAPI) {
  var apiFunctions = bindingsAPI.apiFunctions;
  apiFunctions.setHandleRequestWithPromise('getDevices', function() {
    return serialServicePromise.then(function(serialService) {
      return serialService.getDevices();
    });
  });

  apiFunctions.setHandleRequestWithPromise('connect', function(path, options) {
    return serialServicePromise.then(function(serialService) {
      return serialService.createConnection(path, options);
    }).then(function(result) {
      addEventListeners(result.connection, result.info.connectionId);
      return result.info;
    }).catch (function(e) {
      throw new Error('Failed to connect to the port.');
    });
  });

  apiFunctions.setHandleRequestWithPromise(
      'disconnect', forwardToConnection('close'));
  apiFunctions.setHandleRequestWithPromise(
      'getInfo', forwardToConnection('getInfo'));
  apiFunctions.setHandleRequestWithPromise(
      'update', forwardToConnection('setOptions'));
  apiFunctions.setHandleRequestWithPromise(
      'getControlSignals', forwardToConnection('getControlSignals'));
  apiFunctions.setHandleRequestWithPromise(
      'setControlSignals', forwardToConnection('setControlSignals'));
  apiFunctions.setHandleRequestWithPromise(
      'flush', forwardToConnection('flush'));
  apiFunctions.setHandleRequestWithPromise(
      'setPaused', forwardToConnection('setPaused'));
  apiFunctions.setHandleRequestWithPromise(
      'send', forwardToConnection('send'));

  apiFunctions.setHandleRequestWithPromise('getConnections', function() {
    return serialServicePromise.then(function(serialService) {
      return serialService.getConnections();
    }).then(function(connections) {
      var promises = [];
      for (var connection of connections.values()) {
        promises.push(connection.getInfo());
      }
      return Promise.all(promises);
    });
  });
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("device/serial/serial.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
    "device/serial/data_stream.mojom",
], function(bindings, codec, connection, core, validator, data_stream$) {
  var SendError = {};
  SendError.NONE = 0;
  SendError.DISCONNECTED = SendError.NONE + 1;
  SendError.PENDING = SendError.DISCONNECTED + 1;
  SendError.TIMEOUT = SendError.PENDING + 1;
  SendError.SYSTEM_ERROR = SendError.TIMEOUT + 1;
  var ReceiveError = {};
  ReceiveError.NONE = 0;
  ReceiveError.DISCONNECTED = ReceiveError.NONE + 1;
  ReceiveError.TIMEOUT = ReceiveError.DISCONNECTED + 1;
  ReceiveError.DEVICE_LOST = ReceiveError.TIMEOUT + 1;
  ReceiveError.BREAK = ReceiveError.DEVICE_LOST + 1;
  ReceiveError.FRAME_ERROR = ReceiveError.BREAK + 1;
  ReceiveError.OVERRUN = ReceiveError.FRAME_ERROR + 1;
  ReceiveError.BUFFER_OVERFLOW = ReceiveError.OVERRUN + 1;
  ReceiveError.PARITY_ERROR = ReceiveError.BUFFER_OVERFLOW + 1;
  ReceiveError.SYSTEM_ERROR = ReceiveError.PARITY_ERROR + 1;
  var DataBits = {};
  DataBits.NONE = 0;
  DataBits.SEVEN = DataBits.NONE + 1;
  DataBits.EIGHT = DataBits.SEVEN + 1;
  var ParityBit = {};
  ParityBit.NONE = 0;
  ParityBit.NO = ParityBit.NONE + 1;
  ParityBit.ODD = ParityBit.NO + 1;
  ParityBit.EVEN = ParityBit.ODD + 1;
  var StopBits = {};
  StopBits.NONE = 0;
  StopBits.ONE = StopBits.NONE + 1;
  StopBits.TWO = StopBits.ONE + 1;

  function DeviceInfo(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  DeviceInfo.prototype.initDefaults_ = function() {
    this.path = null;
    this.vendor_id = 0;
    this.has_vendor_id = false;
    this.has_product_id = false;
    this.product_id = 0;
    this.display_name = null;
  };
  DeviceInfo.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  DeviceInfo.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, DeviceInfo.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate DeviceInfo.path
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;





    
    // validate DeviceInfo.display_name
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, true)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  DeviceInfo.encodedSize = codec.kStructHeaderSize + 24;

  DeviceInfo.decode = function(decoder) {
    var packed;
    var val = new DeviceInfo();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.path = decoder.decodeStruct(codec.String);
    val.vendor_id = decoder.decodeStruct(codec.Uint16);
    packed = decoder.readUint8();
    val.has_vendor_id = (packed >> 0) & 1 ? true : false;
    val.has_product_id = (packed >> 1) & 1 ? true : false;
    decoder.skip(1);
    val.product_id = decoder.decodeStruct(codec.Uint16);
    decoder.skip(1);
    decoder.skip(1);
    val.display_name = decoder.decodeStruct(codec.NullableString);
    return val;
  };

  DeviceInfo.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(DeviceInfo.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.path);
    encoder.encodeStruct(codec.Uint16, val.vendor_id);
    packed = 0;
    packed |= (val.has_vendor_id & 1) << 0
    packed |= (val.has_product_id & 1) << 1
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.encodeStruct(codec.Uint16, val.product_id);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.NullableString, val.display_name);
  };
  function ConnectionOptions(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  ConnectionOptions.prototype.initDefaults_ = function() {
    this.bitrate = 0;
    this.data_bits = DataBits.NONE;
    this.parity_bit = ParityBit.NONE;
    this.stop_bits = StopBits.NONE;
    this.cts_flow_control = false;
    this.has_cts_flow_control = false;
  };
  ConnectionOptions.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  ConnectionOptions.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, ConnectionOptions.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;







    return validator.validationError.NONE;
  };

  ConnectionOptions.encodedSize = codec.kStructHeaderSize + 24;

  ConnectionOptions.decode = function(decoder) {
    var packed;
    var val = new ConnectionOptions();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.bitrate = decoder.decodeStruct(codec.Uint32);
    val.data_bits = decoder.decodeStruct(codec.Int32);
    val.parity_bit = decoder.decodeStruct(codec.Int32);
    val.stop_bits = decoder.decodeStruct(codec.Int32);
    packed = decoder.readUint8();
    val.cts_flow_control = (packed >> 0) & 1 ? true : false;
    val.has_cts_flow_control = (packed >> 1) & 1 ? true : false;
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  ConnectionOptions.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(ConnectionOptions.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint32, val.bitrate);
    encoder.encodeStruct(codec.Int32, val.data_bits);
    encoder.encodeStruct(codec.Int32, val.parity_bit);
    encoder.encodeStruct(codec.Int32, val.stop_bits);
    packed = 0;
    packed |= (val.cts_flow_control & 1) << 0
    packed |= (val.has_cts_flow_control & 1) << 1
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function ConnectionInfo(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  ConnectionInfo.prototype.initDefaults_ = function() {
    this.bitrate = 0;
    this.data_bits = DataBits.NONE;
    this.parity_bit = ParityBit.NONE;
    this.stop_bits = StopBits.NONE;
    this.cts_flow_control = false;
  };
  ConnectionInfo.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  ConnectionInfo.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, ConnectionInfo.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;






    return validator.validationError.NONE;
  };

  ConnectionInfo.encodedSize = codec.kStructHeaderSize + 24;

  ConnectionInfo.decode = function(decoder) {
    var packed;
    var val = new ConnectionInfo();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.bitrate = decoder.decodeStruct(codec.Uint32);
    val.data_bits = decoder.decodeStruct(codec.Int32);
    val.parity_bit = decoder.decodeStruct(codec.Int32);
    val.stop_bits = decoder.decodeStruct(codec.Int32);
    val.cts_flow_control = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  ConnectionInfo.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(ConnectionInfo.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint32, val.bitrate);
    encoder.encodeStruct(codec.Int32, val.data_bits);
    encoder.encodeStruct(codec.Int32, val.parity_bit);
    encoder.encodeStruct(codec.Int32, val.stop_bits);
    encoder.encodeStruct(codec.Uint8, val.cts_flow_control);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function HostControlSignals(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  HostControlSignals.prototype.initDefaults_ = function() {
    this.dtr = false;
    this.has_dtr = false;
    this.rts = false;
    this.has_rts = false;
  };
  HostControlSignals.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  HostControlSignals.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, HostControlSignals.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;





    return validator.validationError.NONE;
  };

  HostControlSignals.encodedSize = codec.kStructHeaderSize + 8;

  HostControlSignals.decode = function(decoder) {
    var packed;
    var val = new HostControlSignals();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    packed = decoder.readUint8();
    val.dtr = (packed >> 0) & 1 ? true : false;
    val.has_dtr = (packed >> 1) & 1 ? true : false;
    val.rts = (packed >> 2) & 1 ? true : false;
    val.has_rts = (packed >> 3) & 1 ? true : false;
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  HostControlSignals.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(HostControlSignals.encodedSize);
    encoder.writeUint32(0);
    packed = 0;
    packed |= (val.dtr & 1) << 0
    packed |= (val.has_dtr & 1) << 1
    packed |= (val.rts & 1) << 2
    packed |= (val.has_rts & 1) << 3
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function DeviceControlSignals(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  DeviceControlSignals.prototype.initDefaults_ = function() {
    this.dcd = false;
    this.cts = false;
    this.ri = false;
    this.dsr = false;
  };
  DeviceControlSignals.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  DeviceControlSignals.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, DeviceControlSignals.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;





    return validator.validationError.NONE;
  };

  DeviceControlSignals.encodedSize = codec.kStructHeaderSize + 8;

  DeviceControlSignals.decode = function(decoder) {
    var packed;
    var val = new DeviceControlSignals();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    packed = decoder.readUint8();
    val.dcd = (packed >> 0) & 1 ? true : false;
    val.cts = (packed >> 1) & 1 ? true : false;
    val.ri = (packed >> 2) & 1 ? true : false;
    val.dsr = (packed >> 3) & 1 ? true : false;
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  DeviceControlSignals.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(DeviceControlSignals.encodedSize);
    encoder.writeUint32(0);
    packed = 0;
    packed |= (val.dcd & 1) << 0
    packed |= (val.cts & 1) << 1
    packed |= (val.ri & 1) << 2
    packed |= (val.dsr & 1) << 3
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function SerialService_GetDevices_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  SerialService_GetDevices_Params.prototype.initDefaults_ = function() {
  };
  SerialService_GetDevices_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  SerialService_GetDevices_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, SerialService_GetDevices_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  SerialService_GetDevices_Params.encodedSize = codec.kStructHeaderSize + 0;

  SerialService_GetDevices_Params.decode = function(decoder) {
    var packed;
    var val = new SerialService_GetDevices_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  SerialService_GetDevices_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(SerialService_GetDevices_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function SerialService_GetDevices_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  SerialService_GetDevices_ResponseParams.prototype.initDefaults_ = function() {
    this.devices = null;
  };
  SerialService_GetDevices_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  SerialService_GetDevices_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, SerialService_GetDevices_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_GetDevices_ResponseParams.devices
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 0, 8, new codec.PointerTo(DeviceInfo), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  SerialService_GetDevices_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  SerialService_GetDevices_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new SerialService_GetDevices_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.devices = decoder.decodeArrayPointer(new codec.PointerTo(DeviceInfo));
    return val;
  };

  SerialService_GetDevices_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(SerialService_GetDevices_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeArrayPointer(new codec.PointerTo(DeviceInfo), val.devices);
  };
  function SerialService_Connect_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  SerialService_Connect_Params.prototype.initDefaults_ = function() {
    this.path = null;
    this.options = null;
    this.connection = null;
    this.sink = null;
    this.source = null;
    this.source_client = null;
  };
  SerialService_Connect_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  SerialService_Connect_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, SerialService_Connect_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.path
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.options
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 8, ConnectionOptions, true);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.connection
    err = messageValidator.validateHandle(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.sink
    err = messageValidator.validateHandle(offset + codec.kStructHeaderSize + 20, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.source
    err = messageValidator.validateHandle(offset + codec.kStructHeaderSize + 24, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerialService_Connect_Params.source_client
    err = messageValidator.validateInterface(offset + codec.kStructHeaderSize + 28, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  SerialService_Connect_Params.encodedSize = codec.kStructHeaderSize + 40;

  SerialService_Connect_Params.decode = function(decoder) {
    var packed;
    var val = new SerialService_Connect_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.path = decoder.decodeStruct(codec.String);
    val.options = decoder.decodeStructPointer(ConnectionOptions);
    val.connection = decoder.decodeStruct(codec.Handle);
    val.sink = decoder.decodeStruct(codec.Handle);
    val.source = decoder.decodeStruct(codec.Handle);
    val.source_client = decoder.decodeStruct(codec.Interface);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  SerialService_Connect_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(SerialService_Connect_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.path);
    encoder.encodeStructPointer(ConnectionOptions, val.options);
    encoder.encodeStruct(codec.Handle, val.connection);
    encoder.encodeStruct(codec.Handle, val.sink);
    encoder.encodeStruct(codec.Handle, val.source);
    encoder.encodeStruct(codec.Interface, val.source_client);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function Connection_GetInfo_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_GetInfo_Params.prototype.initDefaults_ = function() {
  };
  Connection_GetInfo_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_GetInfo_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_GetInfo_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_GetInfo_Params.encodedSize = codec.kStructHeaderSize + 0;

  Connection_GetInfo_Params.decode = function(decoder) {
    var packed;
    var val = new Connection_GetInfo_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  Connection_GetInfo_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_GetInfo_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function Connection_GetInfo_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_GetInfo_ResponseParams.prototype.initDefaults_ = function() {
    this.info = null;
  };
  Connection_GetInfo_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_GetInfo_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_GetInfo_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Connection_GetInfo_ResponseParams.info
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, ConnectionInfo, true);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_GetInfo_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  Connection_GetInfo_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new Connection_GetInfo_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.info = decoder.decodeStructPointer(ConnectionInfo);
    return val;
  };

  Connection_GetInfo_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_GetInfo_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(ConnectionInfo, val.info);
  };
  function Connection_SetOptions_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_SetOptions_Params.prototype.initDefaults_ = function() {
    this.options = null;
  };
  Connection_SetOptions_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_SetOptions_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_SetOptions_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Connection_SetOptions_Params.options
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, ConnectionOptions, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_SetOptions_Params.encodedSize = codec.kStructHeaderSize + 8;

  Connection_SetOptions_Params.decode = function(decoder) {
    var packed;
    var val = new Connection_SetOptions_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.options = decoder.decodeStructPointer(ConnectionOptions);
    return val;
  };

  Connection_SetOptions_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_SetOptions_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(ConnectionOptions, val.options);
  };
  function Connection_SetOptions_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_SetOptions_ResponseParams.prototype.initDefaults_ = function() {
    this.success = false;
  };
  Connection_SetOptions_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_SetOptions_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_SetOptions_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  Connection_SetOptions_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  Connection_SetOptions_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new Connection_SetOptions_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.success = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  Connection_SetOptions_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_SetOptions_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint8, val.success);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function Connection_SetControlSignals_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_SetControlSignals_Params.prototype.initDefaults_ = function() {
    this.signals = null;
  };
  Connection_SetControlSignals_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_SetControlSignals_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_SetControlSignals_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Connection_SetControlSignals_Params.signals
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, HostControlSignals, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_SetControlSignals_Params.encodedSize = codec.kStructHeaderSize + 8;

  Connection_SetControlSignals_Params.decode = function(decoder) {
    var packed;
    var val = new Connection_SetControlSignals_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.signals = decoder.decodeStructPointer(HostControlSignals);
    return val;
  };

  Connection_SetControlSignals_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_SetControlSignals_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(HostControlSignals, val.signals);
  };
  function Connection_SetControlSignals_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_SetControlSignals_ResponseParams.prototype.initDefaults_ = function() {
    this.success = false;
  };
  Connection_SetControlSignals_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_SetControlSignals_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_SetControlSignals_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  Connection_SetControlSignals_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  Connection_SetControlSignals_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new Connection_SetControlSignals_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.success = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  Connection_SetControlSignals_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_SetControlSignals_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint8, val.success);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function Connection_GetControlSignals_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_GetControlSignals_Params.prototype.initDefaults_ = function() {
  };
  Connection_GetControlSignals_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_GetControlSignals_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_GetControlSignals_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_GetControlSignals_Params.encodedSize = codec.kStructHeaderSize + 0;

  Connection_GetControlSignals_Params.decode = function(decoder) {
    var packed;
    var val = new Connection_GetControlSignals_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  Connection_GetControlSignals_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_GetControlSignals_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function Connection_GetControlSignals_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_GetControlSignals_ResponseParams.prototype.initDefaults_ = function() {
    this.signals = null;
  };
  Connection_GetControlSignals_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_GetControlSignals_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_GetControlSignals_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Connection_GetControlSignals_ResponseParams.signals
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, DeviceControlSignals, true);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_GetControlSignals_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  Connection_GetControlSignals_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new Connection_GetControlSignals_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.signals = decoder.decodeStructPointer(DeviceControlSignals);
    return val;
  };

  Connection_GetControlSignals_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_GetControlSignals_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(DeviceControlSignals, val.signals);
  };
  function Connection_Flush_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_Flush_Params.prototype.initDefaults_ = function() {
  };
  Connection_Flush_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_Flush_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_Flush_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Connection_Flush_Params.encodedSize = codec.kStructHeaderSize + 0;

  Connection_Flush_Params.decode = function(decoder) {
    var packed;
    var val = new Connection_Flush_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  Connection_Flush_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_Flush_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function Connection_Flush_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  Connection_Flush_ResponseParams.prototype.initDefaults_ = function() {
    this.success = false;
  };
  Connection_Flush_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Connection_Flush_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Connection_Flush_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  Connection_Flush_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  Connection_Flush_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new Connection_Flush_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.success = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  Connection_Flush_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Connection_Flush_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint8, val.success);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  var kSerialService_GetDevices_Name = 0;
  var kSerialService_Connect_Name = 1;

  function SerialServiceProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  SerialServiceProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  SerialServiceProxy.prototype.getDevices = function() {
    var params = new SerialService_GetDevices_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kSerialService_GetDevices_Name,
          codec.align(SerialService_GetDevices_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(SerialService_GetDevices_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(SerialService_GetDevices_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  SerialServiceProxy.prototype.connect = function(path, options, connection, sink, source, source_client) {
    var params = new SerialService_Connect_Params();
    params.path = path;
    params.options = options;
    params.connection = core.isHandle(connection) ? connection : connection.bindProxy(connection, Connection);
    params.sink = core.isHandle(sink) ? sink : connection.bindProxy(sink, data_stream$.DataSink);
    params.source = core.isHandle(source) ? source : connection.bindProxy(source, data_stream$.DataSource);
    params.source_client = core.isHandle(source_client) ? source_client : connection.bindImpl(source_client, data_stream$.DataSourceClient);
    var builder = new codec.MessageBuilder(
        kSerialService_Connect_Name,
        codec.align(SerialService_Connect_Params.encodedSize));
    builder.encodeStruct(SerialService_Connect_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };

  function SerialServiceStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  SerialServiceStub.prototype = Object.create(bindings.StubBase.prototype);
  SerialServiceStub.prototype.getDevices = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.getDevices && bindings.StubBindings(this).delegate.getDevices();
  }
  SerialServiceStub.prototype.connect = function(path, options, connection, sink, source, source_client) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.connect && bindings.StubBindings(this).delegate.connect(path, options, connection.bindHandleToStub(connection, Connection), connection.bindHandleToStub(sink, data_stream$.DataSink), connection.bindHandleToStub(source, data_stream$.DataSource), connection.bindHandleToProxy(source_client, data_stream$.DataSourceClient));
  }

  SerialServiceStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kSerialService_Connect_Name:
      var params = reader.decodeStruct(SerialService_Connect_Params);
      this.connect(params.path, params.options, params.connection, params.sink, params.source, params.source_client);
      return true;
    default:
      return false;
    }
  };

  SerialServiceStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kSerialService_GetDevices_Name:
      var params = reader.decodeStruct(SerialService_GetDevices_Params);
      return this.getDevices().then(function(response) {
        var responseParams =
            new SerialService_GetDevices_ResponseParams();
        responseParams.devices = response.devices;
        var builder = new codec.MessageWithRequestIDBuilder(
            kSerialService_GetDevices_Name,
            codec.align(SerialService_GetDevices_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(SerialService_GetDevices_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateSerialServiceRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kSerialService_GetDevices_Name:
        if (message.expectsResponse())
          paramsClass = SerialService_GetDevices_Params;
      break;
      case kSerialService_Connect_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = SerialService_Connect_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateSerialServiceResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kSerialService_GetDevices_Name:
        if (message.isResponse())
          paramsClass = SerialService_GetDevices_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var SerialService = {
    name: 'device::serial::SerialService',
    proxyClass: SerialServiceProxy,
    stubClass: SerialServiceStub,
    validateRequest: validateSerialServiceRequest,
    validateResponse: validateSerialServiceResponse,
  };
  SerialServiceStub.prototype.validator = validateSerialServiceRequest;
  SerialServiceProxy.prototype.validator = validateSerialServiceResponse;
  var kConnection_GetInfo_Name = 0;
  var kConnection_SetOptions_Name = 1;
  var kConnection_SetControlSignals_Name = 2;
  var kConnection_GetControlSignals_Name = 3;
  var kConnection_Flush_Name = 4;

  function ConnectionProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  ConnectionProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  ConnectionProxy.prototype.getInfo = function() {
    var params = new Connection_GetInfo_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kConnection_GetInfo_Name,
          codec.align(Connection_GetInfo_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(Connection_GetInfo_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(Connection_GetInfo_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  ConnectionProxy.prototype.setOptions = function(options) {
    var params = new Connection_SetOptions_Params();
    params.options = options;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kConnection_SetOptions_Name,
          codec.align(Connection_SetOptions_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(Connection_SetOptions_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(Connection_SetOptions_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  ConnectionProxy.prototype.setControlSignals = function(signals) {
    var params = new Connection_SetControlSignals_Params();
    params.signals = signals;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kConnection_SetControlSignals_Name,
          codec.align(Connection_SetControlSignals_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(Connection_SetControlSignals_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(Connection_SetControlSignals_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  ConnectionProxy.prototype.getControlSignals = function() {
    var params = new Connection_GetControlSignals_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kConnection_GetControlSignals_Name,
          codec.align(Connection_GetControlSignals_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(Connection_GetControlSignals_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(Connection_GetControlSignals_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  ConnectionProxy.prototype.flush = function() {
    var params = new Connection_Flush_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kConnection_Flush_Name,
          codec.align(Connection_Flush_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(Connection_Flush_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(Connection_Flush_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };

  function ConnectionStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  ConnectionStub.prototype = Object.create(bindings.StubBase.prototype);
  ConnectionStub.prototype.getInfo = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.getInfo && bindings.StubBindings(this).delegate.getInfo();
  }
  ConnectionStub.prototype.setOptions = function(options) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.setOptions && bindings.StubBindings(this).delegate.setOptions(options);
  }
  ConnectionStub.prototype.setControlSignals = function(signals) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.setControlSignals && bindings.StubBindings(this).delegate.setControlSignals(signals);
  }
  ConnectionStub.prototype.getControlSignals = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.getControlSignals && bindings.StubBindings(this).delegate.getControlSignals();
  }
  ConnectionStub.prototype.flush = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.flush && bindings.StubBindings(this).delegate.flush();
  }

  ConnectionStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    default:
      return false;
    }
  };

  ConnectionStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kConnection_GetInfo_Name:
      var params = reader.decodeStruct(Connection_GetInfo_Params);
      return this.getInfo().then(function(response) {
        var responseParams =
            new Connection_GetInfo_ResponseParams();
        responseParams.info = response.info;
        var builder = new codec.MessageWithRequestIDBuilder(
            kConnection_GetInfo_Name,
            codec.align(Connection_GetInfo_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(Connection_GetInfo_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kConnection_SetOptions_Name:
      var params = reader.decodeStruct(Connection_SetOptions_Params);
      return this.setOptions(params.options).then(function(response) {
        var responseParams =
            new Connection_SetOptions_ResponseParams();
        responseParams.success = response.success;
        var builder = new codec.MessageWithRequestIDBuilder(
            kConnection_SetOptions_Name,
            codec.align(Connection_SetOptions_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(Connection_SetOptions_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kConnection_SetControlSignals_Name:
      var params = reader.decodeStruct(Connection_SetControlSignals_Params);
      return this.setControlSignals(params.signals).then(function(response) {
        var responseParams =
            new Connection_SetControlSignals_ResponseParams();
        responseParams.success = response.success;
        var builder = new codec.MessageWithRequestIDBuilder(
            kConnection_SetControlSignals_Name,
            codec.align(Connection_SetControlSignals_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(Connection_SetControlSignals_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kConnection_GetControlSignals_Name:
      var params = reader.decodeStruct(Connection_GetControlSignals_Params);
      return this.getControlSignals().then(function(response) {
        var responseParams =
            new Connection_GetControlSignals_ResponseParams();
        responseParams.signals = response.signals;
        var builder = new codec.MessageWithRequestIDBuilder(
            kConnection_GetControlSignals_Name,
            codec.align(Connection_GetControlSignals_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(Connection_GetControlSignals_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kConnection_Flush_Name:
      var params = reader.decodeStruct(Connection_Flush_Params);
      return this.flush().then(function(response) {
        var responseParams =
            new Connection_Flush_ResponseParams();
        responseParams.success = response.success;
        var builder = new codec.MessageWithRequestIDBuilder(
            kConnection_Flush_Name,
            codec.align(Connection_Flush_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(Connection_Flush_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateConnectionRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kConnection_GetInfo_Name:
        if (message.expectsResponse())
          paramsClass = Connection_GetInfo_Params;
      break;
      case kConnection_SetOptions_Name:
        if (message.expectsResponse())
          paramsClass = Connection_SetOptions_Params;
      break;
      case kConnection_SetControlSignals_Name:
        if (message.expectsResponse())
          paramsClass = Connection_SetControlSignals_Params;
      break;
      case kConnection_GetControlSignals_Name:
        if (message.expectsResponse())
          paramsClass = Connection_GetControlSignals_Params;
      break;
      case kConnection_Flush_Name:
        if (message.expectsResponse())
          paramsClass = Connection_Flush_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateConnectionResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kConnection_GetInfo_Name:
        if (message.isResponse())
          paramsClass = Connection_GetInfo_ResponseParams;
        break;
      case kConnection_SetOptions_Name:
        if (message.isResponse())
          paramsClass = Connection_SetOptions_ResponseParams;
        break;
      case kConnection_SetControlSignals_Name:
        if (message.isResponse())
          paramsClass = Connection_SetControlSignals_ResponseParams;
        break;
      case kConnection_GetControlSignals_Name:
        if (message.isResponse())
          paramsClass = Connection_GetControlSignals_ResponseParams;
        break;
      case kConnection_Flush_Name:
        if (message.isResponse())
          paramsClass = Connection_Flush_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var Connection = {
    name: 'device::serial::Connection',
    proxyClass: ConnectionProxy,
    stubClass: ConnectionStub,
    validateRequest: validateConnectionRequest,
    validateResponse: validateConnectionResponse,
  };
  ConnectionStub.prototype.validator = validateConnectionRequest;
  ConnectionProxy.prototype.validator = validateConnectionResponse;

  var exports = {};
  exports.SendError = SendError;
  exports.ReceiveError = ReceiveError;
  exports.DataBits = DataBits;
  exports.ParityBit = ParityBit;
  exports.StopBits = StopBits;
  exports.DeviceInfo = DeviceInfo;
  exports.ConnectionOptions = ConnectionOptions;
  exports.ConnectionInfo = ConnectionInfo;
  exports.HostControlSignals = HostControlSignals;
  exports.DeviceControlSignals = DeviceControlSignals;
  exports.SerialService = SerialService;
  exports.Connection = Connection;

  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("device/serial/serial_serialization.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
    "device/serial/serial.mojom",
    "device/serial/data_stream_serialization.mojom",
], function(bindings, codec, connection, core, validator, serial$, data_stream_serialization$) {

  function ConnectionState(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  ConnectionState.prototype.initDefaults_ = function() {
    this.connectionId = 0;
    this.paused = false;
    this.persistent = false;
    this.name = "";
    this.receiveTimeout = 0;
    this.sendTimeout = 0;
    this.bufferSize = 4096;
  };
  ConnectionState.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  ConnectionState.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, ConnectionState.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;




    
    // validate ConnectionState.name
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;




    return validator.validationError.NONE;
  };

  ConnectionState.encodedSize = codec.kStructHeaderSize + 32;

  ConnectionState.decode = function(decoder) {
    var packed;
    var val = new ConnectionState();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.connectionId = decoder.decodeStruct(codec.Uint32);
    packed = decoder.readUint8();
    val.paused = (packed >> 0) & 1 ? true : false;
    val.persistent = (packed >> 1) & 1 ? true : false;
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.name = decoder.decodeStruct(codec.String);
    val.receiveTimeout = decoder.decodeStruct(codec.Uint32);
    val.sendTimeout = decoder.decodeStruct(codec.Uint32);
    val.bufferSize = decoder.decodeStruct(codec.Uint32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  ConnectionState.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(ConnectionState.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint32, val.connectionId);
    packed = 0;
    packed |= (val.paused & 1) << 0
    packed |= (val.persistent & 1) << 1
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.String, val.name);
    encoder.encodeStruct(codec.Uint32, val.receiveTimeout);
    encoder.encodeStruct(codec.Uint32, val.sendTimeout);
    encoder.encodeStruct(codec.Uint32, val.bufferSize);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function SerializedConnection(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  SerializedConnection.prototype.initDefaults_ = function() {
    this.state = null;
    this.queuedReceiveError = serial$.ReceiveError.NONE;
    this.queuedReceiveData = null;
    this.connection = null;
    this.sender = null;
    this.receiver = null;
  };
  SerializedConnection.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  SerializedConnection.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, SerializedConnection.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerializedConnection.state
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, ConnectionState, false);
    if (err !== validator.validationError.NONE)
        return err;


    
    // validate SerializedConnection.queuedReceiveData
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 16, 1, codec.Int8, true, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerializedConnection.connection
    err = messageValidator.validateInterface(offset + codec.kStructHeaderSize + 24, false);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerializedConnection.sender
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 32, data_stream_serialization$.SerializedDataSender, false);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate SerializedConnection.receiver
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 40, data_stream_serialization$.SerializedDataReceiver, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  SerializedConnection.encodedSize = codec.kStructHeaderSize + 48;

  SerializedConnection.decode = function(decoder) {
    var packed;
    var val = new SerializedConnection();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.state = decoder.decodeStructPointer(ConnectionState);
    val.queuedReceiveError = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.queuedReceiveData = decoder.decodeArrayPointer(codec.Int8);
    val.connection = decoder.decodeStruct(codec.Interface);
    val.sender = decoder.decodeStructPointer(data_stream_serialization$.SerializedDataSender);
    val.receiver = decoder.decodeStructPointer(data_stream_serialization$.SerializedDataReceiver);
    return val;
  };

  SerializedConnection.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(SerializedConnection.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(ConnectionState, val.state);
    encoder.encodeStruct(codec.Int32, val.queuedReceiveError);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeArrayPointer(codec.Int8, val.queuedReceiveData);
    encoder.encodeStruct(codec.Interface, val.connection);
    encoder.encodeStructPointer(data_stream_serialization$.SerializedDataSender, val.sender);
    encoder.encodeStructPointer(data_stream_serialization$.SerializedDataReceiver, val.receiver);
  };

  var exports = {};
  exports.ConnectionState = ConnectionState;
  exports.SerializedConnection = SerializedConnection;

  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define('serial_service', [
    'content/public/renderer/frame_service_registry',
    'data_receiver',
    'data_sender',
    'device/serial/serial.mojom',
    'device/serial/serial_serialization.mojom',
    'mojo/public/js/core',
    'mojo/public/js/router',
    'stash_client',
], function(serviceProvider,
            dataReceiver,
            dataSender,
            serialMojom,
            serialization,
            core,
            routerModule,
            stashClient) {
  /**
   * A Javascript client for the serial service and connection Mojo services.
   *
   * This provides a thick client around the Mojo services, exposing a JS-style
   * interface to serial connections and information about serial devices. This
   * converts parameters and result between the Apps serial API types and the
   * Mojo types.
   */

  var service = new serialMojom.SerialService.proxyClass(
      new routerModule.Router(
          serviceProvider.connectToService(serialMojom.SerialService.name)));

  function getDevices() {
    return service.getDevices().then(function(response) {
      return $Array.map(response.devices, function(device) {
        var result = {path: device.path};
        if (device.has_vendor_id)
          result.vendorId = device.vendor_id;
        if (device.has_product_id)
          result.productId = device.product_id;
        if (device.display_name)
          result.displayName = device.display_name;
        return result;
      });
    });
  }

  var DATA_BITS_TO_MOJO = {
    undefined: serialMojom.DataBits.NONE,
    'seven': serialMojom.DataBits.SEVEN,
    'eight': serialMojom.DataBits.EIGHT,
  };
  var STOP_BITS_TO_MOJO = {
    undefined: serialMojom.StopBits.NONE,
    'one': serialMojom.StopBits.ONE,
    'two': serialMojom.StopBits.TWO,
  };
  var PARITY_BIT_TO_MOJO = {
    undefined: serialMojom.ParityBit.NONE,
    'no': serialMojom.ParityBit.NO,
    'odd': serialMojom.ParityBit.ODD,
    'even': serialMojom.ParityBit.EVEN,
  };
  var SEND_ERROR_TO_MOJO = {
    undefined: serialMojom.SendError.NONE,
    'disconnected': serialMojom.SendError.DISCONNECTED,
    'pending': serialMojom.SendError.PENDING,
    'timeout': serialMojom.SendError.TIMEOUT,
    'system_error': serialMojom.SendError.SYSTEM_ERROR,
  };
  var RECEIVE_ERROR_TO_MOJO = {
    undefined: serialMojom.ReceiveError.NONE,
    'disconnected': serialMojom.ReceiveError.DISCONNECTED,
    'device_lost': serialMojom.ReceiveError.DEVICE_LOST,
    'timeout': serialMojom.ReceiveError.TIMEOUT,
    'break': serialMojom.ReceiveError.BREAK,
    'frame_error': serialMojom.ReceiveError.FRAME_ERROR,
    'overrun': serialMojom.ReceiveError.OVERRUN,
    'buffer_overflow': serialMojom.ReceiveError.BUFFER_OVERFLOW,
    'parity_error': serialMojom.ReceiveError.PARITY_ERROR,
    'system_error': serialMojom.ReceiveError.SYSTEM_ERROR,
  };

  function invertMap(input) {
    var output = {};
    for (var key in input) {
      if (key == 'undefined')
        output[input[key]] = undefined;
      else
        output[input[key]] = key;
    }
    return output;
  }
  var DATA_BITS_FROM_MOJO = invertMap(DATA_BITS_TO_MOJO);
  var STOP_BITS_FROM_MOJO = invertMap(STOP_BITS_TO_MOJO);
  var PARITY_BIT_FROM_MOJO = invertMap(PARITY_BIT_TO_MOJO);
  var SEND_ERROR_FROM_MOJO = invertMap(SEND_ERROR_TO_MOJO);
  var RECEIVE_ERROR_FROM_MOJO = invertMap(RECEIVE_ERROR_TO_MOJO);

  function getServiceOptions(options) {
    var out = {};
    if (options.dataBits)
      out.data_bits = DATA_BITS_TO_MOJO[options.dataBits];
    if (options.stopBits)
      out.stop_bits = STOP_BITS_TO_MOJO[options.stopBits];
    if (options.parityBit)
      out.parity_bit = PARITY_BIT_TO_MOJO[options.parityBit];
    if ('ctsFlowControl' in options) {
      out.has_cts_flow_control = true;
      out.cts_flow_control = options.ctsFlowControl;
    }
    if ('bitrate' in options)
      out.bitrate = options.bitrate;
    return out;
  }

  function convertServiceInfo(result) {
    if (!result.info)
      throw new Error('Failed to get ConnectionInfo.');
    return {
      ctsFlowControl: !!result.info.cts_flow_control,
      bitrate: result.info.bitrate || undefined,
      dataBits: DATA_BITS_FROM_MOJO[result.info.data_bits],
      stopBits: STOP_BITS_FROM_MOJO[result.info.stop_bits],
      parityBit: PARITY_BIT_FROM_MOJO[result.info.parity_bit],
    };
  }

  // Update client-side options |clientOptions| from the user-provided
  // |options|.
  function updateClientOptions(clientOptions, options) {
    if ('name' in options)
      clientOptions.name = options.name;
    if ('receiveTimeout' in options)
      clientOptions.receiveTimeout = options.receiveTimeout;
    if ('sendTimeout' in options)
      clientOptions.sendTimeout = options.sendTimeout;
    if ('bufferSize' in options)
      clientOptions.bufferSize = options.bufferSize;
    if ('persistent' in options)
      clientOptions.persistent = options.persistent;
  };

  function Connection(connection, router, receivePipe, receiveClientPipe,
                      sendPipe, id, options) {
    var state = new serialization.ConnectionState();
    state.connectionId = id;
    updateClientOptions(state, options);
    var receiver = new dataReceiver.DataReceiver(
        receivePipe, receiveClientPipe, state.bufferSize,
        serialMojom.ReceiveError.DISCONNECTED);
    var sender = new dataSender.DataSender(sendPipe, state.bufferSize,
                                           serialMojom.SendError.DISCONNECTED);
    this.init_(state,
               connection,
               router,
               receiver,
               sender,
               null,
               serialMojom.ReceiveError.NONE);
    connections_.set(id, this);
    this.startReceive_();
  }

  // Initializes this Connection from the provided args.
  Connection.prototype.init_ = function(state,
                                        connection,
                                        router,
                                        receiver,
                                        sender,
                                        queuedReceiveData,
                                        queuedReceiveError) {
    this.state_ = state;

    // queuedReceiveData_ or queuedReceiveError_ will store the receive result
    // or error, respectively, if a receive completes or fails while this
    // connection is paused. At most one of the the two may be non-null: a
    // receive completed while paused will only set one of them, no further
    // receives will be performed while paused and a queued result is dispatched
    // before any further receives are initiated when unpausing.
    if (queuedReceiveError != serialMojom.ReceiveError.NONE)
      this.queuedReceiveError_ = {error: queuedReceiveError};
    if (queuedReceiveData) {
      this.queuedReceiveData_ = new ArrayBuffer(queuedReceiveData.length);
      new Int8Array(this.queuedReceiveData_).set(queuedReceiveData);
    }
    this.router_ = router;
    this.remoteConnection_ = connection;
    this.receivePipe_ = receiver;
    this.sendPipe_ = sender;
    this.sendInProgress_ = false;
  };

  Connection.create = function(path, options) {
    options = options || {};
    var serviceOptions = getServiceOptions(options);
    var pipe = core.createMessagePipe();
    var sendPipe = core.createMessagePipe();
    var receivePipe = core.createMessagePipe();
    var receivePipeClient = core.createMessagePipe();
    service.connect(path,
                    serviceOptions,
                    pipe.handle0,
                    sendPipe.handle0,
                    receivePipe.handle0,
                    receivePipeClient.handle0);
    var router = new routerModule.Router(pipe.handle1);
    var connection = new serialMojom.Connection.proxyClass(router);
    return connection.getInfo().then(convertServiceInfo).then(function(info) {
      return Promise.all([info, allocateConnectionId()]);
    }).catch(function(e) {
      router.close();
      core.close(sendPipe.handle1);
      core.close(receivePipe.handle1);
      core.close(receivePipeClient.handle1);
      throw e;
    }).then(function(results) {
      var info = results[0];
      var id = results[1];
      var serialConnectionClient = new Connection(connection,
                                                  router,
                                                  receivePipe.handle1,
                                                  receivePipeClient.handle1,
                                                  sendPipe.handle1,
                                                  id,
                                                  options);
      var clientInfo = serialConnectionClient.getClientInfo_();
      for (var key in clientInfo) {
        info[key] = clientInfo[key];
      }
      return {
        connection: serialConnectionClient,
        info: info,
      };
    });
  };

  Connection.prototype.close = function() {
    this.router_.close();
    this.receivePipe_.close();
    this.sendPipe_.close();
    clearTimeout(this.receiveTimeoutId_);
    clearTimeout(this.sendTimeoutId_);
    connections_.delete(this.state_.connectionId);
    return true;
  };

  Connection.prototype.getClientInfo_ = function() {
    return {
      connectionId: this.state_.connectionId,
      paused: this.state_.paused,
      persistent: this.state_.persistent,
      name: this.state_.name,
      receiveTimeout: this.state_.receiveTimeout,
      sendTimeout: this.state_.sendTimeout,
      bufferSize: this.state_.bufferSize,
    };
  };

  Connection.prototype.getInfo = function() {
    var info = this.getClientInfo_();
    return this.remoteConnection_.getInfo().then(convertServiceInfo).then(
        function(result) {
      for (var key in result) {
        info[key] = result[key];
      }
      return info;
    }).catch(function() {
      return info;
    });
  };

  Connection.prototype.setOptions = function(options) {
    updateClientOptions(this.state_, options);
    var serviceOptions = getServiceOptions(options);
    if ($Object.keys(serviceOptions).length == 0)
      return true;
    return this.remoteConnection_.setOptions(serviceOptions).then(
        function(result) {
      return !!result.success;
    }).catch(function() {
      return false;
    });
  };

  Connection.prototype.getControlSignals = function() {
    return this.remoteConnection_.getControlSignals().then(function(result) {
      if (!result.signals)
        throw new Error('Failed to get control signals.');
      var signals = result.signals;
      return {
        dcd: !!signals.dcd,
        cts: !!signals.cts,
        ri: !!signals.ri,
        dsr: !!signals.dsr,
      };
    });
  };

  Connection.prototype.setControlSignals = function(signals) {
    var controlSignals = {};
    if ('dtr' in signals) {
      controlSignals.has_dtr = true;
      controlSignals.dtr = signals.dtr;
    }
    if ('rts' in signals) {
      controlSignals.has_rts = true;
      controlSignals.rts = signals.rts;
    }
    return this.remoteConnection_.setControlSignals(controlSignals).then(
        function(result) {
      return !!result.success;
    });
  };

  Connection.prototype.flush = function() {
    return this.remoteConnection_.flush().then(function(result) {
      return !!result.success;
    });
  };

  Connection.prototype.setPaused = function(paused) {
    this.state_.paused = paused;
    if (paused) {
      clearTimeout(this.receiveTimeoutId_);
      this.receiveTimeoutId_ = null;
    } else if (!this.receiveInProgress_) {
      this.startReceive_();
    }
  };

  Connection.prototype.send = function(data) {
    if (this.sendInProgress_)
      return Promise.resolve({bytesSent: 0, error: 'pending'});

    if (this.state_.sendTimeout) {
      this.sendTimeoutId_ = setTimeout(function() {
        this.sendPipe_.cancel(serialMojom.SendError.TIMEOUT);
      }.bind(this), this.state_.sendTimeout);
    }
    this.sendInProgress_ = true;
    return this.sendPipe_.send(data).then(function(bytesSent) {
      return {bytesSent: bytesSent};
    }).catch(function(e) {
      return {
        bytesSent: e.bytesSent,
        error: SEND_ERROR_FROM_MOJO[e.error],
      };
    }).then(function(result) {
      if (this.sendTimeoutId_)
        clearTimeout(this.sendTimeoutId_);
      this.sendTimeoutId_ = null;
      this.sendInProgress_ = false;
      return result;
    }.bind(this));
  };

  Connection.prototype.startReceive_ = function() {
    this.receiveInProgress_ = true;
    var receivePromise = null;
    // If we have a queued receive result, dispatch it immediately instead of
    // starting a new receive.
    if (this.queuedReceiveData_) {
      receivePromise = Promise.resolve(this.queuedReceiveData_);
      this.queuedReceiveData_ = null;
    } else if (this.queuedReceiveError_) {
      receivePromise = Promise.reject(this.queuedReceiveError_);
      this.queuedReceiveError_ = null;
    } else {
      receivePromise = this.receivePipe_.receive();
    }
    receivePromise.then(this.onDataReceived_.bind(this)).catch(
        this.onReceiveError_.bind(this));
    this.startReceiveTimeoutTimer_();
  };

  Connection.prototype.onDataReceived_ = function(data) {
    this.startReceiveTimeoutTimer_();
    this.receiveInProgress_ = false;
    if (this.state_.paused) {
      this.queuedReceiveData_ = data;
      return;
    }
    if (this.onData) {
      this.onData(data);
    }
    if (!this.state_.paused) {
      this.startReceive_();
    }
  };

  Connection.prototype.onReceiveError_ = function(e) {
    clearTimeout(this.receiveTimeoutId_);
    this.receiveInProgress_ = false;
    if (this.state_.paused) {
      this.queuedReceiveError_ = e;
      return;
    }
    var error = e.error;
    this.state_.paused = true;
    if (this.onError)
      this.onError(RECEIVE_ERROR_FROM_MOJO[error]);
  };

  Connection.prototype.startReceiveTimeoutTimer_ = function() {
    clearTimeout(this.receiveTimeoutId_);
    if (this.state_.receiveTimeout && !this.state_.paused) {
      this.receiveTimeoutId_ = setTimeout(this.onReceiveTimeout_.bind(this),
                                          this.state_.receiveTimeout);
    }
  };

  Connection.prototype.onReceiveTimeout_ = function() {
    if (this.onError)
      this.onError('timeout');
    this.startReceiveTimeoutTimer_();
  };

  Connection.prototype.serialize = function() {
    connections_.delete(this.state_.connectionId);
    this.onData = null;
    this.onError = null;
    var handle = this.router_.connector_.handle_;
    this.router_.connector_.handle_ = null;
    this.router_.close();
    clearTimeout(this.receiveTimeoutId_);
    clearTimeout(this.sendTimeoutId_);

    // Serializing receivePipe_ will cancel an in-progress receive, which would
    // pause the connection, so save it ahead of time.
    var paused = this.state_.paused;
    return Promise.all([
        this.receivePipe_.serialize(),
        this.sendPipe_.serialize(),
    ]).then(function(serializedComponents) {
      var queuedReceiveError = serialMojom.ReceiveError.NONE;
      if (this.queuedReceiveError_)
        queuedReceiveError = this.queuedReceiveError_.error;
      this.state_.paused = paused;
      var serialized = new serialization.SerializedConnection();
      serialized.state = this.state_;
      serialized.queuedReceiveError = queuedReceiveError;
      serialized.queuedReceiveData =
          this.queuedReceiveData_ ? new Int8Array(this.queuedReceiveData_) :
                                    null;
      serialized.connection = handle;
      serialized.receiver = serializedComponents[0];
      serialized.sender = serializedComponents[1];
      return serialized;
    }.bind(this));
  };

  Connection.deserialize = function(serialized) {
    var serialConnection = $Object.create(Connection.prototype);
    var router = new routerModule.Router(serialized.connection);
    var connection = new serialMojom.Connection.proxyClass(router);
    var receiver = dataReceiver.DataReceiver.deserialize(serialized.receiver);
    var sender = dataSender.DataSender.deserialize(serialized.sender);

    // Ensure that paused and persistent are booleans.
    serialized.state.paused = !!serialized.state.paused;
    serialized.state.persistent = !!serialized.state.persistent;
    serialConnection.init_(serialized.state,
                           connection,
                           router,
                           receiver,
                           sender,
                           serialized.queuedReceiveData,
                           serialized.queuedReceiveError);
    serialConnection.awaitingResume_ = true;
    var connectionId = serialized.state.connectionId;
    connections_.set(connectionId, serialConnection);
    if (connectionId >= nextConnectionId_)
      nextConnectionId_ = connectionId + 1;
    return serialConnection;
  };

  // Resume receives on a deserialized connection.
  Connection.prototype.resumeReceives = function() {
    if (!this.awaitingResume_)
      return;
    this.awaitingResume_ = false;
    if (!this.state_.paused)
      this.startReceive_();
  };

  // All accesses to connections_ and nextConnectionId_ other than those
  // involved in deserialization should ensure that
  // connectionDeserializationComplete_ has resolved first.
  var connectionDeserializationComplete_ = stashClient.retrieve(
      'serial', serialization.SerializedConnection).then(function(decoded) {
    if (!decoded)
      return;
    return Promise.all($Array.map(decoded, Connection.deserialize));
  });

  // The map of connection ID to connection object.
  var connections_ = new Map();

  // The next connection ID to be allocated.
  var nextConnectionId_ = 0;

  function getConnections() {
    return connectionDeserializationComplete_.then(function() {
      return new Map(connections_);
    });
  }

  function getConnection(id) {
    return getConnections().then(function(connections) {
      if (!connections.has(id))
        throw new Error('Serial connection not found.');
      return connections.get(id);
    });
  }

  function allocateConnectionId() {
    return connectionDeserializationComplete_.then(function() {
      return nextConnectionId_++;
    });
  }

  stashClient.registerClient(
      'serial', serialization.SerializedConnection, function() {
    return connectionDeserializationComplete_.then(function() {
      var clientPromises = [];
      for (var connection of connections_.values()) {
        if (connection.state_.persistent)
          clientPromises.push(connection.serialize());
        else
          connection.close();
      }
      return Promise.all($Array.map(clientPromises, function(promise) {
        return promise.then(function(serialization) {
          return {
            serialization: serialization,
            monitorHandles: !serialization.paused,
          };
        });
      }));
    });
  });

  return {
    getDevices: getDevices,
    createConnection: Connection.create,
    getConnection: getConnection,
    getConnections: getConnections,
    // For testing.
    Connection: Connection,
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var SetIconCommon = requireNative('setIcon').SetIconCommon;
var sendRequest = require('sendRequest').sendRequest;

function loadImagePath(path, callback) {
  var img = new Image();
  img.onerror = function() {
    console.error('Could not load action icon \'' + path + '\'.');
  };
  img.onload = function() {
    var canvas = document.createElement('canvas');
    canvas.width = img.width;
    canvas.height = img.height;

    var canvas_context = canvas.getContext('2d');
    canvas_context.clearRect(0, 0, canvas.width, canvas.height);
    canvas_context.drawImage(img, 0, 0, canvas.width, canvas.height);
    var imageData = canvas_context.getImageData(0, 0, canvas.width,
                                                canvas.height);
    callback(imageData);
  };
  img.src = path;
}

function smellsLikeImageData(imageData) {
  // See if this object at least looks like an ImageData element.
  // Unfortunately, we cannot use instanceof because the ImageData
  // constructor is not public.
  //
  // We do this manually instead of using JSONSchema to avoid having these
  // properties show up in the doc.
  return (typeof imageData == 'object') && ('width' in imageData) &&
         ('height' in imageData) && ('data' in imageData);
}

function verifyImageData(imageData) {
  if (!smellsLikeImageData(imageData)) {
    throw new Error(
        'The imageData property must contain an ImageData object or' +
        ' dictionary of ImageData objects.');
  }
}

/**
 * Normalizes |details| to a format suitable for sending to the browser,
 * for example converting ImageData to a binary representation.
 *
 * @param {ImageDetails} details
 *   The ImageDetails passed into an extension action-style API.
 * @param {Function} callback
 *   The callback function to pass processed imageData back to. Note that this
 *   callback may be called reentrantly.
 */
function setIcon(details, callback) {
  // Note that iconIndex is actually deprecated, and only available to the
  // pageAction API.
  // TODO(kalman): Investigate whether this is for the pageActions API, and if
  // so, delete it.
  if ('iconIndex' in details) {
    callback(details);
    return;
  }

  if ('imageData' in details) {
    if (smellsLikeImageData(details.imageData)) {
      var imageData = details.imageData;
      details.imageData = {};
      details.imageData[imageData.width.toString()] = imageData;
    } else if (typeof details.imageData == 'object' &&
               Object.getOwnPropertyNames(details.imageData).length !== 0) {
      for (var sizeKey in details.imageData) {
        verifyImageData(details.imageData[sizeKey]);
      }
    } else {
      verifyImageData(false);
    }

    callback(SetIconCommon(details));
    return;
  }

  if ('path' in details) {
    if (typeof details.path == 'object') {
      details.imageData = {};
      var detailKeyCount = 0;
      for (var iconSize in details.path) {
        ++detailKeyCount;
        loadImagePath(details.path[iconSize], function(size, imageData) {
          details.imageData[size] = imageData;
          if (--detailKeyCount == 0)
            callback(SetIconCommon(details));
        }.bind(null, iconSize));
      }
      if (detailKeyCount == 0)
        throw new Error('The path property must not be empty.');
    } else if (typeof details.path == 'string') {
      details.imageData = {};
      loadImagePath(details.path, function(imageData) {
        details.imageData[imageData.width.toString()] = imageData;
        delete details.path;
        callback(SetIconCommon(details));
      });
    }
    return;
  }
  throw new Error('Either the path or imageData property must be specified.');
}

exports.$set('setIcon', setIcon);
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define('stash_client', [
    'async_waiter',
    'content/public/renderer/frame_service_registry',
    'extensions/common/mojo/stash.mojom',
    'mojo/public/js/buffer',
    'mojo/public/js/codec',
    'mojo/public/js/core',
    'mojo/public/js/router',
], function(asyncWaiter, serviceProvider, stashMojom, bufferModule,
            codec, core, routerModule) {
  /**
   * @module stash_client
   */

  var service = new stashMojom.StashService.proxyClass(new routerModule.Router(
      serviceProvider.connectToService(stashMojom.StashService.name)));

  /**
   * A callback invoked to obtain objects to stash from a particular client.
   * @callback module:stash_client.StashCallback
   * @return {!Promise<!Array<!Object>>|!Array<!Object>} An array of objects to
   *     stash or a promise that will resolve to an array of objects to stash.
   *     The exact type of each object should match the type passed alongside
   *     this callback.
   */

  /**
   * A stash client registration.
   * @constructor
   * @private
   * @alias module:stash_client~Registration
   */
  function Registration(id, type, callback) {
    /**
     * The client id.
     * @type {string}
     * @private
     */
    this.id_ = id;

    /**
     * The type of the objects to be stashed.
     * @type {!Object}
     * @private
     */
    this.type_ = type;

    /**
     * The callback to invoke to obtain the objects to stash.
     * @type {module:stash_client.StashCallback}
     * @private
     */
    this.callback_ = callback;
  }

  /**
   * Serializes and returns this client's stashable objects.
   * @return
   * {!Promise<!Array<module:extensions/common/stash.mojom.StashedObject>>} The
   * serialized stashed objects.
   */
  Registration.prototype.serialize = function() {
    return Promise.resolve(this.callback_()).then($Function.bind(
        function(stashedObjects) {
      if (!stashedObjects)
        return [];
      return $Array.map(stashedObjects, function(stashed) {
        var builder = new codec.MessageBuilder(
            0, codec.align(this.type_.encodedSize));
        builder.encodeStruct(this.type_, stashed.serialization);
        var encoded = builder.finish();
        return new stashMojom.StashedObject({
          id: this.id_,
          data: new Uint8Array(encoded.buffer.arrayBuffer),
          stashed_handles: encoded.handles,
          monitor_handles: stashed.monitorHandles,
        });
      }, this);
    }, this)).catch(function(e) { return []; });
  };

  /**
   * The registered stash clients.
   * @type {!Array<!Registration>}
   */
  var clients = [];

  /**
   * Registers a client to provide objects to stash during shut-down.
   *
   * @param {string} id The id of the client. This can be passed to retrieve to
   *     retrieve the stashed objects.
   * @param {!Object} type The type of the objects that callback will return.
   * @param {module:stash_client.StashCallback} callback The callback that
   *     returns objects to stash.
   * @alias module:stash_client.registerClient
   */
  function registerClient(id, type, callback) {
    clients.push(new Registration(id, type, callback));
  }

  var retrievedStash = service.retrieveStash().then(function(result) {
    if (!result || !result.stash)
      return {};
    var stashById = {};
    $Array.forEach(result.stash, function(stashed) {
      if (!stashById[stashed.id])
        stashById[stashed.id] = [];
      stashById[stashed.id].push(stashed);
    });
    return stashById;
  }, function() {
    // If the stash is not available, act as if the stash was empty.
    return {};
  });

  /**
   * Retrieves the objects that were stashed with the given |id|, deserializing
   * them into structs with type |type|.
   *
   * @param {string} id The id of the client. This should be unique to this
   *     client and should be passed as the id to registerClient().
   * @param {!Object} type The mojo struct type that was serialized into the
   *     each stashed object.
   * @return {!Promise<!Array<!Object>>} The stashed objects. The exact type of
   *     each object is that of the |type| parameter.
   * @alias module:stash_client.retrieve
   */
  function retrieve(id, type) {
    return retrievedStash.then(function(stash) {
      var stashedObjects = stash[id];
      if (!stashedObjects)
        return Promise.resolve([]);

      return Promise.all($Array.map(stashedObjects, function(stashed) {
        var encodedData = new ArrayBuffer(stashed.data.length);
        new Uint8Array(encodedData).set(stashed.data);
        var reader = new codec.MessageReader(new codec.Message(
            new bufferModule.Buffer(encodedData), stashed.stashed_handles));
        var decoded = reader.decodeStruct(type);
        return decoded;
      }));
    });
  }

  function saveStashForTesting() {
    Promise.all($Array.map(clients, function(client) {
      return client.serialize();
    })).then(function(stashedObjects) {
      var flattenedObjectsToStash = [];
      $Array.forEach(stashedObjects, function(stashedObjects) {
        flattenedObjectsToStash =
            $Array.concat(flattenedObjectsToStash, stashedObjects);
      });
      service.addToStash(flattenedObjectsToStash);
    });
  }

  return {
    registerClient: registerClient,
    retrieve: retrieve,
    saveStashForTesting: saveStashForTesting,
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("extensions/common/mojo/stash.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
], function(bindings, codec, connection, core, validator) {

  function StashedObject(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  StashedObject.prototype.initDefaults_ = function() {
    this.id = null;
    this.data = null;
    this.stashed_handles = null;
    this.monitor_handles = false;
  };
  StashedObject.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  StashedObject.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, StashedObject.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StashedObject.id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StashedObject.data
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 8, 1, codec.Uint8, false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StashedObject.stashed_handles
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 16, 4, codec.Handle, false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  StashedObject.encodedSize = codec.kStructHeaderSize + 32;

  StashedObject.decode = function(decoder) {
    var packed;
    var val = new StashedObject();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.id = decoder.decodeStruct(codec.String);
    val.data = decoder.decodeArrayPointer(codec.Uint8);
    val.stashed_handles = decoder.decodeArrayPointer(codec.Handle);
    val.monitor_handles = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  StashedObject.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(StashedObject.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.id);
    encoder.encodeArrayPointer(codec.Uint8, val.data);
    encoder.encodeArrayPointer(codec.Handle, val.stashed_handles);
    encoder.encodeStruct(codec.Uint8, val.monitor_handles);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function StashService_AddToStash_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  StashService_AddToStash_Params.prototype.initDefaults_ = function() {
    this.stashed_objects = null;
  };
  StashService_AddToStash_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  StashService_AddToStash_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, StashService_AddToStash_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StashService_AddToStash_Params.stashed_objects
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 0, 8, new codec.PointerTo(StashedObject), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  StashService_AddToStash_Params.encodedSize = codec.kStructHeaderSize + 8;

  StashService_AddToStash_Params.decode = function(decoder) {
    var packed;
    var val = new StashService_AddToStash_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.stashed_objects = decoder.decodeArrayPointer(new codec.PointerTo(StashedObject));
    return val;
  };

  StashService_AddToStash_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(StashService_AddToStash_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeArrayPointer(new codec.PointerTo(StashedObject), val.stashed_objects);
  };
  function StashService_RetrieveStash_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  StashService_RetrieveStash_Params.prototype.initDefaults_ = function() {
  };
  StashService_RetrieveStash_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  StashService_RetrieveStash_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, StashService_RetrieveStash_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  StashService_RetrieveStash_Params.encodedSize = codec.kStructHeaderSize + 0;

  StashService_RetrieveStash_Params.decode = function(decoder) {
    var packed;
    var val = new StashService_RetrieveStash_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  StashService_RetrieveStash_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(StashService_RetrieveStash_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function StashService_RetrieveStash_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  StashService_RetrieveStash_ResponseParams.prototype.initDefaults_ = function() {
    this.stash = null;
  };
  StashService_RetrieveStash_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  StashService_RetrieveStash_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, StashService_RetrieveStash_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate StashService_RetrieveStash_ResponseParams.stash
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 0, 8, new codec.PointerTo(StashedObject), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  StashService_RetrieveStash_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  StashService_RetrieveStash_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new StashService_RetrieveStash_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.stash = decoder.decodeArrayPointer(new codec.PointerTo(StashedObject));
    return val;
  };

  StashService_RetrieveStash_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(StashService_RetrieveStash_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeArrayPointer(new codec.PointerTo(StashedObject), val.stash);
  };
  var kStashService_AddToStash_Name = 0;
  var kStashService_RetrieveStash_Name = 1;

  function StashServiceProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  StashServiceProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  StashServiceProxy.prototype.addToStash = function(stashed_objects) {
    var params = new StashService_AddToStash_Params();
    params.stashed_objects = stashed_objects;
    var builder = new codec.MessageBuilder(
        kStashService_AddToStash_Name,
        codec.align(StashService_AddToStash_Params.encodedSize));
    builder.encodeStruct(StashService_AddToStash_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  StashServiceProxy.prototype.retrieveStash = function() {
    var params = new StashService_RetrieveStash_Params();
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kStashService_RetrieveStash_Name,
          codec.align(StashService_RetrieveStash_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(StashService_RetrieveStash_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(StashService_RetrieveStash_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };

  function StashServiceStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  StashServiceStub.prototype = Object.create(bindings.StubBase.prototype);
  StashServiceStub.prototype.addToStash = function(stashed_objects) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.addToStash && bindings.StubBindings(this).delegate.addToStash(stashed_objects);
  }
  StashServiceStub.prototype.retrieveStash = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.retrieveStash && bindings.StubBindings(this).delegate.retrieveStash();
  }

  StashServiceStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kStashService_AddToStash_Name:
      var params = reader.decodeStruct(StashService_AddToStash_Params);
      this.addToStash(params.stashed_objects);
      return true;
    default:
      return false;
    }
  };

  StashServiceStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kStashService_RetrieveStash_Name:
      var params = reader.decodeStruct(StashService_RetrieveStash_Params);
      return this.retrieveStash().then(function(response) {
        var responseParams =
            new StashService_RetrieveStash_ResponseParams();
        responseParams.stash = response.stash;
        var builder = new codec.MessageWithRequestIDBuilder(
            kStashService_RetrieveStash_Name,
            codec.align(StashService_RetrieveStash_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(StashService_RetrieveStash_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateStashServiceRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kStashService_AddToStash_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = StashService_AddToStash_Params;
      break;
      case kStashService_RetrieveStash_Name:
        if (message.expectsResponse())
          paramsClass = StashService_RetrieveStash_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateStashServiceResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kStashService_RetrieveStash_Name:
        if (message.isResponse())
          paramsClass = StashService_RetrieveStash_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var StashService = {
    name: 'extensions::StashService',
    proxyClass: StashServiceProxy,
    stubClass: StashServiceStub,
    validateRequest: validateStashServiceRequest,
    validateResponse: validateStashServiceResponse,
  };
  StashServiceStub.prototype.validator = validateStashServiceRequest;
  StashServiceProxy.prototype.validator = validateStashServiceResponse;

  var exports = {};
  exports.StashedObject = StashedObject;
  exports.StashService = StashService;

  return exports;
});// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// test_custom_bindings.js
// mini-framework for ExtensionApiTest browser tests

var binding = require('binding').Binding.create('test');

var environmentSpecificBindings = require('test_environment_specific_bindings');
var GetExtensionAPIDefinitionsForTest =
    requireNative('apiDefinitions').GetExtensionAPIDefinitionsForTest;
var GetAPIFeatures = requireNative('test_features').GetAPIFeatures;
var natives = requireNative('test_native_handler');
var uncaughtExceptionHandler = require('uncaught_exception_handler');
var userGestures = requireNative('user_gestures');

var RunWithNativesEnabled = requireNative('v8_context').RunWithNativesEnabled;
var GetModuleSystem = requireNative('v8_context').GetModuleSystem;

binding.registerCustomHook(function(api) {
  var chromeTest = api.compiledApi;
  var apiFunctions = api.apiFunctions;

  chromeTest.tests = chromeTest.tests || [];

  var currentTest = null;
  var lastTest = null;
  var testsFailed = 0;
  var testCount = 1;
  var failureException = 'chrome.test.failure';

  // Helper function to get around the fact that function names in javascript
  // are read-only, and you can't assign one to anonymous functions.
  function testName(test) {
    return test ? (test.name || test.generatedName) : "(no test)";
  }

  function testDone() {
    environmentSpecificBindings.testDone(chromeTest.runNextTest);
  }

  function allTestsDone() {
    if (testsFailed == 0) {
      chromeTest.notifyPass();
    } else {
      chromeTest.notifyFail('Failed ' + testsFailed + ' of ' +
                             testCount + ' tests');
    }
  }

  var pendingCallbacks = 0;

  apiFunctions.setHandleRequest('callbackAdded', function() {
    pendingCallbacks++;

    var called = null;
    return function() {
      if (called != null) {
        var redundantPrefix = 'Error\n';
        chromeTest.fail(
          'Callback has already been run. ' +
          'First call:\n' +
          $String.slice(called, redundantPrefix.length) + '\n' +
          'Second call:\n' +
          $String.slice(new Error().stack, redundantPrefix.length));
      }
      called = new Error().stack;

      pendingCallbacks--;
      if (pendingCallbacks == 0) {
        chromeTest.succeed();
      }
    };
  });

  apiFunctions.setHandleRequest('runNextTest', function() {
    // There may have been callbacks which were interrupted by failure
    // exceptions.
    pendingCallbacks = 0;

    lastTest = currentTest;
    currentTest = chromeTest.tests.shift();

    if (!currentTest) {
      allTestsDone();
      return;
    }

    try {
      chromeTest.log("( RUN      ) " + testName(currentTest));
      uncaughtExceptionHandler.setHandler(function(message, e) {
        if (e !== failureException)
          chromeTest.fail('uncaught exception: ' + message);
      });
      currentTest.call();
    } catch (e) {
      uncaughtExceptionHandler.handle(e.message, e);
    }
  });

  apiFunctions.setHandleRequest('fail', function(message) {
    chromeTest.log("(  FAILED  ) " + testName(currentTest));

    var stack = {};
    Error.captureStackTrace(stack, chromeTest.fail);

    if (!message)
      message = "FAIL (no message)";

    message += "\n" + stack.stack;
    console.log("[FAIL] " + testName(currentTest) + ": " + message);
    testsFailed++;
    testDone();

    // Interrupt the rest of the test.
    throw failureException;
  });

  apiFunctions.setHandleRequest('succeed', function() {
    console.log("[SUCCESS] " + testName(currentTest));
    chromeTest.log("(  SUCCESS )");
    testDone();
  });

  apiFunctions.setHandleRequest('runWithNativesEnabled', function(callback) {
    RunWithNativesEnabled(callback);
  });

  apiFunctions.setHandleRequest('getModuleSystem', function(context) {
    return GetModuleSystem(context);
  });

  apiFunctions.setHandleRequest('assertTrue', function(test, message) {
    chromeTest.assertBool(test, true, message);
  });

  apiFunctions.setHandleRequest('assertFalse', function(test, message) {
    chromeTest.assertBool(test, false, message);
  });

  apiFunctions.setHandleRequest('assertBool',
                                function(test, expected, message) {
    if (test !== expected) {
      if (typeof(test) == "string") {
        if (message)
          message = test + "\n" + message;
        else
          message = test;
      }
      chromeTest.fail(message);
    }
  });

  apiFunctions.setHandleRequest('checkDeepEq', function(expected, actual) {
    if ((expected === null) != (actual === null))
      return false;

    if (expected === actual)
      return true;

    if (typeof(expected) !== typeof(actual))
      return false;

    for (var p in actual) {
      if ($Object.hasOwnProperty(actual, p) &&
          !$Object.hasOwnProperty(expected, p)) {
        return false;
      }
    }
    for (var p in expected) {
      if ($Object.hasOwnProperty(expected, p) &&
          !$Object.hasOwnProperty(actual, p)) {
        return false;
      }
    }

    for (var p in expected) {
      var eq = true;
      switch (typeof(expected[p])) {
        case 'object':
          eq = chromeTest.checkDeepEq(expected[p], actual[p]);
          break;
        case 'function':
          eq = (typeof(actual[p]) != 'undefined' &&
                expected[p].toString() == actual[p].toString());
          break;
        default:
          eq = (expected[p] == actual[p] &&
                typeof(expected[p]) == typeof(actual[p]));
          break;
      }
      if (!eq)
        return false;
    }
    return true;
  });

  apiFunctions.setHandleRequest('assertEq',
                                function(expected, actual, message) {
    var error_msg = "API Test Error in " + testName(currentTest);
    if (message)
      error_msg += ": " + message;
    if (typeof(expected) == 'object') {
      if (!chromeTest.checkDeepEq(expected, actual)) {
        error_msg += "\nActual: " + $JSON.stringify(actual) +
                     "\nExpected: " + $JSON.stringify(expected);
        chromeTest.fail(error_msg);
      }
      return;
    }
    if (expected != actual) {
      chromeTest.fail(error_msg +
                       "\nActual: " + actual + "\nExpected: " + expected);
    }
    if (typeof(expected) != typeof(actual)) {
      chromeTest.fail(error_msg +
                       " (type mismatch)\nActual Type: " + typeof(actual) +
                       "\nExpected Type:" + typeof(expected));
    }
  });

  apiFunctions.setHandleRequest('assertNoLastError', function() {
    if (chrome.runtime.lastError != undefined) {
      chromeTest.fail("lastError.message == " +
                       chrome.runtime.lastError.message);
    }
  });

  apiFunctions.setHandleRequest('assertLastError', function(expectedError) {
    chromeTest.assertEq(typeof(expectedError), 'string');
    chromeTest.assertTrue(chrome.runtime.lastError != undefined,
        "No lastError, but expected " + expectedError);
    chromeTest.assertEq(expectedError, chrome.runtime.lastError.message);
  });

  apiFunctions.setHandleRequest('assertThrows',
                                function(fn, self, args, message) {
    chromeTest.assertTrue(typeof fn == 'function');
    try {
      fn.apply(self, args);
      chromeTest.fail('Did not throw error: ' + fn);
    } catch (e) {
      if (e != failureException && message !== undefined) {
        if (message instanceof RegExp) {
          chromeTest.assertTrue(message.test(e.message),
                                e.message + ' should match ' + message)
        } else {
          chromeTest.assertEq(message, e.message);
        }
      }
    }
  });

  function safeFunctionApply(func, args) {
    try {
      if (func)
        return $Function.apply(func, undefined, args);
    } catch (e) {
      var msg = "uncaught exception " + e;
      chromeTest.fail(msg);
    }
  };

  // Wrapper for generating test functions, that takes care of calling
  // assertNoLastError() and (optionally) succeed() for you.
  apiFunctions.setHandleRequest('callback', function(func, expectedError) {
    if (func) {
      chromeTest.assertEq(typeof(func), 'function');
    }
    var callbackCompleted = chromeTest.callbackAdded();

    return function() {
      if (expectedError == null) {
        chromeTest.assertNoLastError();
      } else {
        chromeTest.assertLastError(expectedError);
      }

      var result;
      if (func) {
        result = safeFunctionApply(func, arguments);
      }

      callbackCompleted();
      return result;
    };
  });

  apiFunctions.setHandleRequest('listenOnce', function(event, func) {
    var callbackCompleted = chromeTest.callbackAdded();
    var listener = function() {
      event.removeListener(listener);
      safeFunctionApply(func, arguments);
      callbackCompleted();
    };
    event.addListener(listener);
  });

  apiFunctions.setHandleRequest('listenForever', function(event, func) {
    var callbackCompleted = chromeTest.callbackAdded();

    var listener = function() {
      safeFunctionApply(func, arguments);
    };

    var done = function() {
      event.removeListener(listener);
      callbackCompleted();
    };

    event.addListener(listener);
    return done;
  });

  apiFunctions.setHandleRequest('callbackPass', function(func) {
    return chromeTest.callback(func);
  });

  apiFunctions.setHandleRequest('callbackFail', function(expectedError, func) {
    return chromeTest.callback(func, expectedError);
  });

  apiFunctions.setHandleRequest('runTests', function(tests) {
    chromeTest.tests = tests;
    testCount = chromeTest.tests.length;
    chromeTest.runNextTest();
  });

  apiFunctions.setHandleRequest('getApiDefinitions', function() {
    return GetExtensionAPIDefinitionsForTest();
  });

  apiFunctions.setHandleRequest('getApiFeatures', function() {
    return GetAPIFeatures();
  });

  apiFunctions.setHandleRequest('isProcessingUserGesture', function() {
    return userGestures.IsProcessingUserGesture();
  });

  apiFunctions.setHandleRequest('runWithUserGesture', function(callback) {
    chromeTest.assertEq(typeof(callback), 'function');
    return userGestures.RunWithUserGesture(callback);
  });

  apiFunctions.setHandleRequest('runWithoutUserGesture', function(callback) {
    chromeTest.assertEq(typeof(callback), 'function');
    return userGestures.RunWithoutUserGesture(callback);
  });

  apiFunctions.setHandleRequest('setExceptionHandler', function(callback) {
    chromeTest.assertEq(typeof(callback), 'function');
    uncaughtExceptionHandler.setHandler(callback);
  });

  apiFunctions.setHandleRequest('getWakeEventPage', function() {
    return natives.GetWakeEventPage();
  });

  environmentSpecificBindings.registerHooks(api);
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Handles uncaught exceptions thrown by extensions. By default this is to
// log an error message, but tests may override this behaviour.
var handler = function(message, e) {
  console.error(message);
};

/**
 * Append the error description and stack trace to |message|.
 *
 * @param {string} message - The prefix of the error message.
 * @param {Error|*} e - The thrown error object. This object is potentially
 *   unsafe, because it could be generated by an extension.
 * @param {string=} priorStackTrace - The stack trace to be appended to the
 *   error message. This stack trace must not include stack frames of |e.stack|,
 *   because both stack traces are concatenated. Overlapping stack traces will
 *   confuse extension developers.
 * @return {string} The formatted error message.
 */
function formatErrorMessage(message, e, priorStackTrace) {
  if (e)
    message += ': ' + safeErrorToString(e, false);

  var stack;
  try {
    // If the stack was set, use it.
    // |e.stack| could be void in the following common example:
    // throw "Error message";
    stack = $String.self(e && e.stack);
  } catch (e) {}

  // If a stack is not provided, capture a stack trace.
  if (!priorStackTrace && !stack)
    stack = getStackTrace();

  stack = filterExtensionStackTrace(stack);
  if (stack)
    message += '\n' + stack;

  // If an asynchronouse stack trace was set, append it.
  if (priorStackTrace)
    message += '\n' + priorStackTrace;

  return message;
}

function filterExtensionStackTrace(stack) {
  if (!stack)
    return '';
  // Remove stack frames in the stack trace that weren't associated with the
  // extension, to not confuse extension developers with internal details.
  stack = $String.split(stack, '\n');
  stack = $Array.filter(stack, function(line) {
    return $String.indexOf(line, 'chrome-extension://') >= 0;
  });
  return $Array.join(stack, '\n');
}

function getStackTrace() {
  var e = {};
  $Error.captureStackTrace(e, getStackTrace);
  return e.stack;
}

function getExtensionStackTrace() {
  return filterExtensionStackTrace(getStackTrace());
}

/**
 * Convert an object to a string.
 *
 * @param {Error|*} e - A thrown object (possibly user-supplied).
 * @param {boolean=} omitType - Whether to try to serialize |e.message| instead
 *   of |e.toString()|.
 * @return {string} The error message.
 */
function safeErrorToString(e, omitType) {
  try {
    return $String.self(omitType && e.message || e);
  } catch (e) {
    // This error is exceptional and could be triggered by
    // throw {toString: function() { throw 'Haha' } };
    return '(cannot get error message)';
  }
}

/**
 * Formats the error message and invokes the error handler.
 *
 * @param {string} message - Error message prefix.
 * @param {Error|*} e - Thrown object.
 * @param {string=} priorStackTrace - Error message suffix.
 * @see formatErrorMessage
 */
exports.$set('handle', function(message, e, priorStackTrace) {
  message = formatErrorMessage(message, e, priorStackTrace);
  handler(message, e);
});

// |newHandler| A function which matches |handler|.
exports.$set('setHandler', function(newHandler) {
  handler = newHandler;
});

exports.$set('getStackTrace', getStackTrace);
exports.$set('getExtensionStackTrace', getExtensionStackTrace);
exports.$set('safeErrorToString', safeErrorToString);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var nativeDeepCopy = requireNative('utils').deepCopy;
var schemaRegistry = requireNative('schema_registry');
var CHECK = requireNative('logging').CHECK;
var DCHECK = requireNative('logging').DCHECK;
var WARNING = requireNative('logging').WARNING;

/**
 * An object forEach. Calls |f| with each (key, value) pair of |obj|, using
 * |self| as the target.
 * @param {Object} obj The object to iterate over.
 * @param {function} f The function to call in each iteration.
 * @param {Object} self The object to use as |this| in each function call.
 */
function forEach(obj, f, self) {
  for (var key in obj) {
    if ($Object.hasOwnProperty(obj, key))
      $Function.call(f, self, key, obj[key]);
  }
}

/**
 * Assuming |array_of_dictionaries| is structured like this:
 * [{id: 1, ... }, {id: 2, ...}, ...], you can use
 * lookup(array_of_dictionaries, 'id', 2) to get the dictionary with id == 2.
 * @param {Array<Object<?>>} array_of_dictionaries
 * @param {string} field
 * @param {?} value
 */
function lookup(array_of_dictionaries, field, value) {
  var filter = function (dict) {return dict[field] == value;};
  var matches = $Array.filter(array_of_dictionaries, filter);
  if (matches.length == 0) {
    return undefined;
  } else if (matches.length == 1) {
    return matches[0]
  } else {
    throw new Error("Failed lookup of field '" + field + "' with value '" +
                    value + "'");
  }
}

function loadTypeSchema(typeName, defaultSchema) {
  var parts = $String.split(typeName, '.');
  if (parts.length == 1) {
    if (defaultSchema == null) {
      WARNING('Trying to reference "' + typeName + '" ' +
              'with neither namespace nor default schema.');
      return null;
    }
    var types = defaultSchema.types;
  } else {
    var schemaName = $Array.join($Array.slice(parts, 0, parts.length - 1), '.');
    var types = schemaRegistry.GetSchema(schemaName).types;
  }
  for (var i = 0; i < types.length; ++i) {
    if (types[i].id == typeName)
      return types[i];
  }
  return null;
}

/**
 * Sets a property |value| on |obj| with property name |key|. Like
 *
 *     obj[key] = value;
 *
 * but without triggering setters.
 */
function defineProperty(obj, key, value) {
  $Object.defineProperty(obj, key, {
    __proto__: null,
    configurable: true,
    enumerable: true,
    writable: true,
    value: value,
  });
}

/**
 * Takes a private class implementation |privateClass| and exposes a subset of
 * its methods |functions| and properties |properties| and |readonly| to a
 * public wrapper class that should be passed in. Within bindings code, you can
 * access the implementation from an instance of the wrapper class using
 * privates(instance).impl, and from the implementation class you can access
 * the wrapper using this.wrapper (or implInstance.wrapper if you have another
 * instance of the implementation class).
 *
 * |publicClass| should be a constructor that calls constructPrivate() like so:
 *
 *     privates(publicClass).constructPrivate(this, arguments);
 *
 * @param {function} publicClass The publicly exposed wrapper class. This must
 *     be a named function, and the name appears in stack traces.
 * @param {Object} privateClass The class implementation.
 * @param {{superclass: ?Function,
 *          functions: ?Array<string>,
 *          properties: ?Array<string>,
 *          readonly: ?Array<string>}} exposed The names of properties on the
 *     implementation class to be exposed. |superclass| represents the
 *     constructor of the class to be used as the superclass of the exposed
 *     class; |functions| represents the names of functions which should be
 *     delegated to the implementation; |properties| are gettable/settable
 *     properties and |readonly| are read-only properties.
 */
function expose(publicClass, privateClass, exposed) {
  DCHECK(!(privateClass.prototype instanceof $Object.self));

  $Object.setPrototypeOf(exposed, null);

  // This should be called by publicClass.
  privates(publicClass).constructPrivate = function(self, args) {
    if (!(self instanceof publicClass)) {
      throw new Error('Please use "new ' + publicClass.name + '"');
    }
    // The "instanceof publicClass" check can easily be spoofed, so we check
    // whether the private impl is already set before continuing.
    var privateSelf = privates(self);
    if ('impl' in privateSelf) {
      throw new Error('Object ' + publicClass.name + ' is already constructed');
    }
    var privateObj = $Object.create(privateClass.prototype);
    $Function.apply(privateClass, privateObj, args);
    privateObj.wrapper = self;
    privateSelf.impl = privateObj;
  };

  function getPrivateImpl(self) {
    var impl = privates(self).impl;
    if (!(impl instanceof privateClass)) {
      // Either the object is not constructed, or the property descriptor is
      // used on a target that is not an instance of publicClass.
      throw new Error('impl is not an instance of ' + privateClass.name);
    }
    return impl;
  }

  var publicClassPrototype = {
    // The final prototype will be assigned at the end of this method.
    __proto__: null,
    constructor: publicClass,
  };

  if ('functions' in exposed) {
    $Array.forEach(exposed.functions, function(func) {
      publicClassPrototype[func] = function() {
        var impl = getPrivateImpl(this);
        return $Function.apply(impl[func], impl, arguments);
      };
    });
  }

  if ('properties' in exposed) {
    $Array.forEach(exposed.properties, function(prop) {
      $Object.defineProperty(publicClassPrototype, prop, {
        __proto__: null,
        enumerable: true,
        get: function() {
          return getPrivateImpl(this)[prop];
        },
        set: function(value) {
          var impl = getPrivateImpl(this);
          delete impl[prop];
          impl[prop] = value;
        }
      });
    });
  }

  if ('readonly' in exposed) {
    $Array.forEach(exposed.readonly, function(readonly) {
      $Object.defineProperty(publicClassPrototype, readonly, {
        __proto__: null,
        enumerable: true,
        get: function() {
          return getPrivateImpl(this)[readonly];
        },
      });
    });
  }

  // The prototype properties have been installed. Now we can safely assign an
  // unsafe prototype and export the class to the public.
  var superclass = exposed.superclass || $Object.self;
  $Object.setPrototypeOf(publicClassPrototype, superclass.prototype);
  publicClass.prototype = publicClassPrototype;

  return publicClass;
}

/**
 * Returns a deep copy of |value|. The copy will have no references to nested
 * values of |value|.
 */
function deepCopy(value) {
  return nativeDeepCopy(value);
}

/**
 * Wrap an asynchronous API call to a function |func| in a promise. The
 * remaining arguments will be passed to |func|. Returns a promise that will be
 * resolved to the result passed to the callback or rejected if an error occurs
 * (if chrome.runtime.lastError is set). If there are multiple results, the
 * promise will be resolved with an array containing those results.
 *
 * For example,
 * promise(chrome.storage.get, 'a').then(function(result) {
 *   // Use result.
 * }).catch(function(error) {
 *   // Report error.message.
 * });
 */
function promise(func) {
  var args = $Array.slice(arguments, 1);
  DCHECK(typeof func == 'function');
  return new Promise(function(resolve, reject) {
    args.push(function() {
      if (chrome.runtime.lastError) {
        reject(new Error(chrome.runtime.lastError));
        return;
      }
      if (arguments.length <= 1)
        resolve(arguments[0]);
      else
        resolve($Array.slice(arguments));
    });
    $Function.apply(func, null, args);
  });
}

exports.$set('forEach', forEach);
exports.$set('loadTypeSchema', loadTypeSchema);
exports.$set('lookup', lookup);
exports.$set('defineProperty', defineProperty);
exports.$set('expose', expose);
exports.$set('deepCopy', deepCopy);
exports.$set('promise', promise);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements helper objects for the dialog, newwindow, and
// permissionrequest <webview> events.

var MessagingNatives = requireNative('messaging_natives');
var WebViewConstants = require('webViewConstants').WebViewConstants;
var WebViewInternal = require('webViewInternal').WebViewInternal;

var PERMISSION_TYPES = ['media',
                        'geolocation',
                        'pointerLock',
                        'download',
                        'loadplugin',
                        'filesystem',
                        'fullscreen'];

// -----------------------------------------------------------------------------
// WebViewActionRequest object.

// Default partial implementation of a webview action request.
function WebViewActionRequest(webViewImpl, event, webViewEvent, interfaceName) {
  this.webViewImpl = webViewImpl;
  this.event = event;
  this.webViewEvent = webViewEvent;
  this.interfaceName = interfaceName;
  this.guestInstanceId = this.webViewImpl.guest.getId();
  this.requestId = event.requestId;
  this.actionTaken = false;

  // Add on the request information specific to the request type.
  for (var infoName in this.event.requestInfo) {
    this.event[infoName] = this.event.requestInfo[infoName];
    this.webViewEvent[infoName] = this.event.requestInfo[infoName];
  }
}

// Performs the default action for the request.
WebViewActionRequest.prototype.defaultAction = function() {
  // Do nothing if the action has already been taken or the requester is
  // already gone (in which case its guestInstanceId will be stale).
  if (this.actionTaken ||
      this.guestInstanceId != this.webViewImpl.guest.getId()) {
    return;
  }

  this.actionTaken = true;
  WebViewInternal.setPermission(this.guestInstanceId, this.requestId,
                                'default', '', function(allowed) {
    if (allowed) {
      return;
    }
    this.showWarningMessage();
  }.bind(this));
};

// Called to handle the action request's event.
WebViewActionRequest.prototype.handleActionRequestEvent = function() {
  // Construct the interface object and attach it to |webViewEvent|.
  var request = this.getInterfaceObject();
  this.webViewEvent[this.interfaceName] = request;

  var defaultPrevented = !this.webViewImpl.dispatchEvent(this.webViewEvent);
  // Set |webViewEvent| to null to break the circular reference to |request| so
  // that the garbage collector can eventually collect it.
  this.webViewEvent = null;
  if (this.actionTaken) {
    return;
  }

  if (defaultPrevented) {
    // Track the lifetime of |request| with the garbage collector.
    var portId = -1;  // (hack) there is no Extension Port to release
    MessagingNatives.BindToGC(request, this.defaultAction.bind(this), portId);
  } else {
    this.defaultAction();
  }
};

// Displays a warning message when an action request is blocked by default.
WebViewActionRequest.prototype.showWarningMessage = function() {
  window.console.warn(this.WARNING_MSG_REQUEST_BLOCKED);
};

// This function ensures that each action is taken at most once.
WebViewActionRequest.prototype.validateCall = function() {
  if (this.actionTaken) {
    throw new Error(this.ERROR_MSG_ACTION_ALREADY_TAKEN);
  }
  this.actionTaken = true;
};

// The following are implemented by the specific action request.

// Returns the interface object for this action request.
WebViewActionRequest.prototype.getInterfaceObject = undefined;

// Error/warning messages.
WebViewActionRequest.prototype.ERROR_MSG_ACTION_ALREADY_TAKEN = undefined;
WebViewActionRequest.prototype.WARNING_MSG_REQUEST_BLOCKED = undefined;

// -----------------------------------------------------------------------------
// Dialog object.

// Represents a dialog box request (e.g. alert()).
function Dialog(webViewImpl, event, webViewEvent) {
  WebViewActionRequest.call(this, webViewImpl, event, webViewEvent, 'dialog');

  this.handleActionRequestEvent();
}

Dialog.prototype.__proto__ = WebViewActionRequest.prototype;

Dialog.prototype.getInterfaceObject = function() {
  return {
    ok: function(user_input) {
      this.validateCall();
      user_input = user_input || '';
      WebViewInternal.setPermission(
          this.guestInstanceId, this.requestId, 'allow', user_input);
    }.bind(this),
    cancel: function() {
      this.validateCall();
      WebViewInternal.setPermission(
          this.guestInstanceId, this.requestId, 'deny');
    }.bind(this)
  };
};

Dialog.prototype.showWarningMessage = function() {
  var VOWELS = ['a', 'e', 'i', 'o', 'u'];
  var dialogType = this.event.messageType;
  var article = (VOWELS.indexOf(dialogType.charAt(0)) >= 0) ? 'An' : 'A';
  this.WARNING_MSG_REQUEST_BLOCKED = this.WARNING_MSG_REQUEST_BLOCKED.
      replace('%1', article).replace('%2', dialogType);
  window.console.warn(this.WARNING_MSG_REQUEST_BLOCKED);
};

Dialog.prototype.ERROR_MSG_ACTION_ALREADY_TAKEN =
    WebViewConstants.ERROR_MSG_DIALOG_ACTION_ALREADY_TAKEN;
Dialog.prototype.WARNING_MSG_REQUEST_BLOCKED =
    WebViewConstants.WARNING_MSG_DIALOG_REQUEST_BLOCKED;

// -----------------------------------------------------------------------------
// NewWindow object.

// Represents a new window request.
function NewWindow(webViewImpl, event, webViewEvent) {
  WebViewActionRequest.call(this, webViewImpl, event, webViewEvent, 'window');

  this.handleActionRequestEvent();
}

NewWindow.prototype.__proto__ = WebViewActionRequest.prototype;

NewWindow.prototype.getInterfaceObject = function() {
  return {
    attach: function(webview) {
      this.validateCall();
      if (!webview || !webview.tagName || webview.tagName != 'WEBVIEW') {
        throw new Error(ERROR_MSG_WEBVIEW_EXPECTED);
      }

      var webViewImpl = privates(webview).internal;
      // Update the partition.
      if (this.event.partition) {
        webViewImpl.onAttach(this.event.partition);
      }

      var attached = webViewImpl.attachWindow$(this.event.windowId);
      if (!attached) {
        window.console.error(ERROR_MSG_NEWWINDOW_UNABLE_TO_ATTACH);
      }

      if (this.guestInstanceId != this.webViewImpl.guest.getId()) {
        // If the opener is already gone, then its guestInstanceId will be
        // stale.
        return;
      }

      // If the object being passed into attach is not a valid <webview>
      // then we will fail and it will be treated as if the new window
      // was rejected. The permission API plumbing is used here to clean
      // up the state created for the new window if attaching fails.
      WebViewInternal.setPermission(this.guestInstanceId, this.requestId,
                                    attached ? 'allow' : 'deny');
    }.bind(this),
    discard: function() {
      this.validateCall();
      if (!this.guestInstanceId) {
        // If the opener is already gone, then we won't have its
        // guestInstanceId.
        return;
      }
      WebViewInternal.setPermission(
          this.guestInstanceId, this.requestId, 'deny');
    }.bind(this)
  };
};

NewWindow.prototype.ERROR_MSG_ACTION_ALREADY_TAKEN =
    WebViewConstants.ERROR_MSG_NEWWINDOW_ACTION_ALREADY_TAKEN;
NewWindow.prototype.WARNING_MSG_REQUEST_BLOCKED =
    WebViewConstants.WARNING_MSG_NEWWINDOW_REQUEST_BLOCKED;

// -----------------------------------------------------------------------------
// PermissionRequest object.

// Represents a permission request (e.g. to access the filesystem).
function PermissionRequest(webViewImpl, event, webViewEvent) {
  WebViewActionRequest.call(this, webViewImpl, event, webViewEvent, 'request');

  if (!this.validPermissionCheck()) {
    return;
  }

  this.handleActionRequestEvent();
}

PermissionRequest.prototype.__proto__ = WebViewActionRequest.prototype;

PermissionRequest.prototype.allow = function() {
  this.validateCall();
  WebViewInternal.setPermission(this.guestInstanceId, this.requestId, 'allow');
};

PermissionRequest.prototype.deny = function() {
  this.validateCall();
  WebViewInternal.setPermission(this.guestInstanceId, this.requestId, 'deny');
};

PermissionRequest.prototype.getInterfaceObject = function() {
  var request = {
    allow: this.allow.bind(this),
    deny: this.deny.bind(this)
  };

  // Add on the request information specific to the request type.
  for (var infoName in this.event.requestInfo) {
    request[infoName] = this.event.requestInfo[infoName];
  }

  return $Object.freeze(request);
};

PermissionRequest.prototype.showWarningMessage = function() {
  window.console.warn(
      this.WARNING_MSG_REQUEST_BLOCKED.replace('%1', this.event.permission));
};

// Checks that the requested permission is valid. Returns true if valid.
PermissionRequest.prototype.validPermissionCheck = function() {
  if (PERMISSION_TYPES.indexOf(this.event.permission) < 0) {
    // The permission type is not allowed. Trigger the default response.
    this.defaultAction();
    return false;
  }
  return true;
};

PermissionRequest.prototype.ERROR_MSG_ACTION_ALREADY_TAKEN =
    WebViewConstants.ERROR_MSG_PERMISSION_ACTION_ALREADY_TAKEN;
PermissionRequest.prototype.WARNING_MSG_REQUEST_BLOCKED =
    WebViewConstants.WARNING_MSG_PERMISSION_REQUEST_BLOCKED;

// -----------------------------------------------------------------------------

// FullscreenPermissionRequest object.

// Represents a fullscreen permission request.
function FullscreenPermissionRequest(webViewImpl, event, webViewEvent) {
  PermissionRequest.call(this, webViewImpl, event, webViewEvent);
}

FullscreenPermissionRequest.prototype.__proto__ = PermissionRequest.prototype;

FullscreenPermissionRequest.prototype.allow = function() {
  PermissionRequest.prototype.allow.call(this);
  // Now make the <webview> element go fullscreen.
  this.webViewImpl.makeElementFullscreen();
};

// -----------------------------------------------------------------------------

var WebViewActionRequests = {
  WebViewActionRequest: WebViewActionRequest,
  Dialog: Dialog,
  NewWindow: NewWindow,
  PermissionRequest: PermissionRequest,
  FullscreenPermissionRequest: FullscreenPermissionRequest
};

// Exports.
exports.$set('WebViewActionRequests', WebViewActionRequests);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements the public-facing API functions for the <webview> tag.

var WebViewInternal = require('webViewInternal').WebViewInternal;
var WebViewImpl = require('webView').WebViewImpl;

// An array of <webview>'s public-facing API methods. Methods without custom
// implementations will be given default implementations that call into the
// internal API method with the same name in |WebViewInternal|. For example, a
// method called 'someApiMethod' would be given the following default
// implementation:
//
// WebViewImpl.prototype.someApiMethod = function(var_args) {
//   if (!this.guest.getId()) {
//     return false;
//   }
//   var args = $Array.concat([this.guest.getId()], $Array.slice(arguments));
//   $Function.apply(WebViewInternal.someApiMethod, null, args);
//   return true;
// };
//
// These default implementations come from createDefaultApiMethod() in
// web_view.js.
var WEB_VIEW_API_METHODS = [
  // Add content scripts for the guest page.
  'addContentScripts',

  // Navigates to the previous history entry.
  'back',

  // Returns whether there is a previous history entry to navigate to.
  'canGoBack',

  // Returns whether there is a subsequent history entry to navigate to.
  'canGoForward',

  // Clears browsing data for the WebView partition.
  'clearData',

  // Injects JavaScript code into the guest page.
  'executeScript',

  // Initiates a find-in-page request.
  'find',

  // Navigates to the subsequent history entry.
  'forward',

  // Returns Chrome's internal process ID for the guest web page's current
  // process.
  'getProcessId',

  // Returns the user agent string used by the webview for guest page requests.
  'getUserAgent',

  // Gets the current zoom factor.
  'getZoom',

  // Gets the current zoom mode of the webview.
  'getZoomMode',

  // Navigates to a history entry using a history index relative to the current
  // navigation.
  'go',

  // Injects CSS into the guest page.
  'insertCSS',

  // Indicates whether or not the webview's user agent string has been
  // overridden.
  'isUserAgentOverridden',

  // Loads a data URL with a specified base URL used for relative links.
  // Optionally, a virtual URL can be provided to be shown to the user instead
  // of the data URL.
  'loadDataWithBaseUrl',

  // Prints the contents of the webview.
  'print',

  // Removes content scripts for the guest page.
  'removeContentScripts',

  // Reloads the current top-level page.
  'reload',

  // Override the user agent string used by the webview for guest page requests.
  'setUserAgentOverride',

  // Changes the zoom factor of the page.
  'setZoom',

  // Changes the zoom mode of the webview.
  'setZoomMode',

  // Stops loading the current navigation if one is in progress.
  'stop',

  // Ends the current find session.
  'stopFinding',

  // Forcibly kills the guest web page's renderer process.
  'terminate'
];

// -----------------------------------------------------------------------------
// Custom API method implementations.

WebViewImpl.prototype.addContentScripts = function(rules) {
  return WebViewInternal.addContentScripts(this.viewInstanceId, rules);
};

WebViewImpl.prototype.back = function(callback) {
  return this.go(-1, callback);
};

WebViewImpl.prototype.canGoBack = function() {
  return this.entryCount > 1 && this.currentEntryIndex > 0;
};

WebViewImpl.prototype.canGoForward = function() {
  return this.currentEntryIndex >= 0 &&
      this.currentEntryIndex < (this.entryCount - 1);
};

WebViewImpl.prototype.executeScript = function(var_args) {
  return this.executeCode(WebViewInternal.executeScript,
                          $Array.slice(arguments));
};

WebViewImpl.prototype.forward = function(callback) {
  return this.go(1, callback);
};

WebViewImpl.prototype.getProcessId = function() {
  return this.processId;
};

WebViewImpl.prototype.getUserAgent = function() {
  return this.userAgentOverride || navigator.userAgent;
};

WebViewImpl.prototype.insertCSS = function(var_args) {
  return this.executeCode(WebViewInternal.insertCSS, $Array.slice(arguments));
};

WebViewImpl.prototype.isUserAgentOverridden = function() {
  return !!this.userAgentOverride &&
      this.userAgentOverride != navigator.userAgent;
};

WebViewImpl.prototype.loadDataWithBaseUrl = function(
    dataUrl, baseUrl, virtualUrl) {
  if (!this.guest.getId()) {
    return;
  }
  WebViewInternal.loadDataWithBaseUrl(
      this.guest.getId(), dataUrl, baseUrl, virtualUrl, function() {
        // Report any errors.
        if (chrome.runtime.lastError != undefined) {
          window.console.error(
              'Error while running webview.loadDataWithBaseUrl: ' +
                  chrome.runtime.lastError.message);
        }
      });
};

WebViewImpl.prototype.print = function() {
  return this.executeScript({code: 'window.print();'});
};

WebViewImpl.prototype.removeContentScripts = function(names) {
  return WebViewInternal.removeContentScripts(this.viewInstanceId, names);
};

WebViewImpl.prototype.setUserAgentOverride = function(userAgentOverride) {
  this.userAgentOverride = userAgentOverride;
  if (!this.guest.getId()) {
    // If we are not attached yet, then we will pick up the user agent on
    // attachment.
    return false;
  }
  WebViewInternal.overrideUserAgent(this.guest.getId(), userAgentOverride);
  return true;
};

WebViewImpl.prototype.setZoom = function(zoomFactor, callback) {
  if (!this.guest.getId()) {
    this.cachedZoomFactor = zoomFactor;
    return false;
  }
  this.cachedZoomFactor = 1;
  WebViewInternal.setZoom(this.guest.getId(), zoomFactor, callback);
  return true;
};

// -----------------------------------------------------------------------------

WebViewImpl.getApiMethods = function() {
  return WEB_VIEW_API_METHODS;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements the attributes of the <webview> tag.

var GuestViewAttributes = require('guestViewAttributes').GuestViewAttributes;
var WebViewConstants = require('webViewConstants').WebViewConstants;
var WebViewImpl = require('webView').WebViewImpl;
var WebViewInternal = require('webViewInternal').WebViewInternal;

// -----------------------------------------------------------------------------
// AllowScalingAttribute object.

// Attribute that specifies whether scaling is allowed in the webview.
function AllowScalingAttribute(view) {
  GuestViewAttributes.BooleanAttribute.call(
      this, WebViewConstants.ATTRIBUTE_ALLOWSCALING, view);
}

AllowScalingAttribute.prototype.__proto__ =
    GuestViewAttributes.BooleanAttribute.prototype;

AllowScalingAttribute.prototype.handleMutation = function(oldValue, newValue) {
  if (!this.view.guest.getId())
  return;

  WebViewInternal.setAllowScaling(this.view.guest.getId(), this.getValue());
};

// -----------------------------------------------------------------------------
// AllowTransparencyAttribute object.

// Attribute that specifies whether transparency is allowed in the webview.
function AllowTransparencyAttribute(view) {
  GuestViewAttributes.BooleanAttribute.call(
      this, WebViewConstants.ATTRIBUTE_ALLOWTRANSPARENCY, view);
}

AllowTransparencyAttribute.prototype.__proto__ =
    GuestViewAttributes.BooleanAttribute.prototype;

AllowTransparencyAttribute.prototype.handleMutation = function(oldValue,
                                                               newValue) {
  if (!this.view.guest.getId())
    return;

  WebViewInternal.setAllowTransparency(this.view.guest.getId(),
                                       this.getValue());
};

// -----------------------------------------------------------------------------
// AutosizeDimensionAttribute object.

// Attribute used to define the demension limits of autosizing.
function AutosizeDimensionAttribute(name, view) {
  GuestViewAttributes.IntegerAttribute.call(this, name, view);
}

AutosizeDimensionAttribute.prototype.__proto__ =
    GuestViewAttributes.IntegerAttribute.prototype;

AutosizeDimensionAttribute.prototype.handleMutation = function(
    oldValue, newValue) {
  if (!this.view.guest.getId())
    return;

  this.view.guest.setSize({
    'enableAutoSize': this.view.attributes[
      WebViewConstants.ATTRIBUTE_AUTOSIZE].getValue(),
    'min': {
      'width': this.view.attributes[
          WebViewConstants.ATTRIBUTE_MINWIDTH].getValue(),
      'height': this.view.attributes[
          WebViewConstants.ATTRIBUTE_MINHEIGHT].getValue()
    },
    'max': {
      'width': this.view.attributes[
          WebViewConstants.ATTRIBUTE_MAXWIDTH].getValue(),
      'height': this.view.attributes[
          WebViewConstants.ATTRIBUTE_MAXHEIGHT].getValue()
    }
  });
  return;
};

// -----------------------------------------------------------------------------
// AutosizeAttribute object.

// Attribute that specifies whether the webview should be autosized.
function AutosizeAttribute(view) {
  GuestViewAttributes.BooleanAttribute.call(
      this, WebViewConstants.ATTRIBUTE_AUTOSIZE, view);
}

AutosizeAttribute.prototype.__proto__ =
    GuestViewAttributes.BooleanAttribute.prototype;

AutosizeAttribute.prototype.handleMutation =
    AutosizeDimensionAttribute.prototype.handleMutation;

// -----------------------------------------------------------------------------
// NameAttribute object.

// Attribute that sets the guest content's window.name object.
function NameAttribute(view) {
  GuestViewAttributes.Attribute.call(
      this, WebViewConstants.ATTRIBUTE_NAME, view);
}

NameAttribute.prototype.__proto__ = GuestViewAttributes.Attribute.prototype

NameAttribute.prototype.handleMutation = function(oldValue, newValue) {
  oldValue = oldValue || '';
  newValue = newValue || '';
  if (oldValue === newValue || !this.view.guest.getId())
    return;

  WebViewInternal.setName(this.view.guest.getId(), newValue);
};

NameAttribute.prototype.setValue = function(value) {
  value = value || '';
  if (value === '')
    this.view.element.removeAttribute(this.name);
  else
    this.view.element.setAttribute(this.name, value);
};

// -----------------------------------------------------------------------------
// PartitionAttribute object.

// Attribute representing the state of the storage partition.
function PartitionAttribute(view) {
  GuestViewAttributes.Attribute.call(
      this, WebViewConstants.ATTRIBUTE_PARTITION, view);
  this.validPartitionId = true;
}

PartitionAttribute.prototype.__proto__ =
    GuestViewAttributes.Attribute.prototype;

PartitionAttribute.prototype.handleMutation = function(oldValue, newValue) {
  newValue = newValue || '';

  // The partition cannot change if the webview has already navigated.
  if (!this.view.attributes[
          WebViewConstants.ATTRIBUTE_SRC].beforeFirstNavigation) {
    window.console.error(WebViewConstants.ERROR_MSG_ALREADY_NAVIGATED);
    this.setValueIgnoreMutation(oldValue);
    return;
  }
  if (newValue == 'persist:') {
    this.validPartitionId = false;
    window.console.error(
        WebViewConstants.ERROR_MSG_INVALID_PARTITION_ATTRIBUTE);
  }
};

PartitionAttribute.prototype.detach = function() {
  this.validPartitionId = true;
};

// -----------------------------------------------------------------------------
// SrcAttribute object.

// Attribute that handles the location and navigation of the webview.
function SrcAttribute(view) {
  GuestViewAttributes.Attribute.call(
      this, WebViewConstants.ATTRIBUTE_SRC, view);
  this.setupMutationObserver();
  this.beforeFirstNavigation = true;
}

SrcAttribute.prototype.__proto__ = GuestViewAttributes.Attribute.prototype;

SrcAttribute.prototype.setValueIgnoreMutation = function(value) {
  GuestViewAttributes.Attribute.prototype.setValueIgnoreMutation.call(
      this, value);
  // takeRecords() is needed to clear queued up src mutations. Without it, it is
  // possible for this change to get picked up asyncronously by src's mutation
  // observer |observer|, and then get handled even though we do not want to
  // handle this mutation.
  this.observer.takeRecords();
}

SrcAttribute.prototype.handleMutation = function(oldValue, newValue) {
  // Once we have navigated, we don't allow clearing the src attribute.
  // Once <webview> enters a navigated state, it cannot return to a
  // placeholder state.
  if (!newValue && oldValue) {
    // src attribute changes normally initiate a navigation. We suppress
    // the next src attribute handler call to avoid reloading the page
    // on every guest-initiated navigation.
    this.setValueIgnoreMutation(oldValue);
    return;
  }
  this.parse();
};

SrcAttribute.prototype.attach = function() {
  this.parse();
};

SrcAttribute.prototype.detach = function() {
  this.beforeFirstNavigation = true;
};

// The purpose of this mutation observer is to catch assignment to the src
// attribute without any changes to its value. This is useful in the case
// where the webview guest has crashed and navigating to the same address
// spawns off a new process.
SrcAttribute.prototype.setupMutationObserver =
    function() {
  this.observer = new MutationObserver(function(mutations) {
    $Array.forEach(mutations, function(mutation) {
      var oldValue = mutation.oldValue;
      var newValue = this.getValue();
      if (oldValue != newValue) {
        return;
      }
      this.handleMutation(oldValue, newValue);
    }.bind(this));
  }.bind(this));
  var params = {
    attributes: true,
    attributeOldValue: true,
    attributeFilter: [this.name]
  };
  this.observer.observe(this.view.element, params);
};

SrcAttribute.prototype.parse = function() {
  if (!this.view.elementAttached ||
      !this.view.attributes[
          WebViewConstants.ATTRIBUTE_PARTITION].validPartitionId ||
      !this.getValue()) {
    return;
  }

  if (!this.view.guest.getId()) {
    if (this.beforeFirstNavigation) {
      this.beforeFirstNavigation = false;
      this.view.createGuest();
    }
    return;
  }

  WebViewInternal.navigate(this.view.guest.getId(), this.getValue());
};

// -----------------------------------------------------------------------------

// Sets up all of the webview attributes.
WebViewImpl.prototype.setupAttributes = function() {
  this.attributes[WebViewConstants.ATTRIBUTE_ALLOWSCALING] =
      new AllowScalingAttribute(this);
  this.attributes[WebViewConstants.ATTRIBUTE_ALLOWTRANSPARENCY] =
      new AllowTransparencyAttribute(this);
  this.attributes[WebViewConstants.ATTRIBUTE_AUTOSIZE] =
      new AutosizeAttribute(this);
  this.attributes[WebViewConstants.ATTRIBUTE_NAME] =
      new NameAttribute(this);
  this.attributes[WebViewConstants.ATTRIBUTE_PARTITION] =
      new PartitionAttribute(this);
  this.attributes[WebViewConstants.ATTRIBUTE_SRC] =
      new SrcAttribute(this);

  var autosizeAttributes = [WebViewConstants.ATTRIBUTE_MAXHEIGHT,
                            WebViewConstants.ATTRIBUTE_MAXWIDTH,
                            WebViewConstants.ATTRIBUTE_MINHEIGHT,
                            WebViewConstants.ATTRIBUTE_MINWIDTH];
  for (var i = 0; autosizeAttributes[i]; ++i) {
    this.attributes[autosizeAttributes[i]] =
        new AutosizeDimensionAttribute(autosizeAttributes[i], this);
  }
};
// Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module contains constants used in webview.

// Container for the webview constants.
var WebViewConstants = {
  // Attributes.
  ATTRIBUTE_ALLOWTRANSPARENCY: 'allowtransparency',
  ATTRIBUTE_ALLOWSCALING: 'allowscaling',
  ATTRIBUTE_AUTOSIZE: 'autosize',
  ATTRIBUTE_MAXHEIGHT: 'maxheight',
  ATTRIBUTE_MAXWIDTH: 'maxwidth',
  ATTRIBUTE_MINHEIGHT: 'minheight',
  ATTRIBUTE_MINWIDTH: 'minwidth',
  ATTRIBUTE_NAME: 'name',
  ATTRIBUTE_PARTITION: 'partition',
  ATTRIBUTE_SRC: 'src',

  // Error/warning messages.
  ERROR_MSG_ALREADY_NAVIGATED: '<webview>: ' +
      'The object has already navigated, so its partition cannot be changed.',
  ERROR_MSG_CANNOT_INJECT_SCRIPT: '<webview>: ' +
      'Script cannot be injected into content until the page has loaded.',
  ERROR_MSG_DIALOG_ACTION_ALREADY_TAKEN: '<webview>: ' +
      'An action has already been taken for this "dialog" event.',
  ERROR_MSG_NEWWINDOW_ACTION_ALREADY_TAKEN: '<webview>: ' +
      'An action has already been taken for this "newwindow" event.',
  ERROR_MSG_PERMISSION_ACTION_ALREADY_TAKEN: '<webview>: ' +
      'Permission has already been decided for this "permissionrequest" event.',
  ERROR_MSG_INVALID_PARTITION_ATTRIBUTE: '<webview>: ' +
      'Invalid partition attribute.',
  WARNING_MSG_DIALOG_REQUEST_BLOCKED: '<webview>: %1 %2 dialog was blocked.',
  WARNING_MSG_NEWWINDOW_REQUEST_BLOCKED: '<webview>: A new window was blocked.',
  WARNING_MSG_PERMISSION_REQUEST_BLOCKED: '<webview>: ' +
      'The permission request for "%1" has been denied.'
};

exports.$set('WebViewConstants', $Object.freeze(WebViewConstants));
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Event management for WebView.

var CreateEvent = require('guestViewEvents').CreateEvent;
var DeclarativeWebRequestSchema =
    requireNative('schema_registry').GetSchema('declarativeWebRequest');
var EventBindings = require('event_bindings');
var GuestViewEvents = require('guestViewEvents').GuestViewEvents;
var GuestViewInternalNatives = requireNative('guest_view_internal');
var IdGenerator = requireNative('id_generator');
var WebRequestEvent = require('webRequestInternal').WebRequestEvent;
var WebRequestSchema =
    requireNative('schema_registry').GetSchema('webRequest');
var WebViewActionRequests =
    require('webViewActionRequests').WebViewActionRequests;

var WebRequestMessageEvent = CreateEvent('webViewInternal.onMessage');

function WebViewEvents(webViewImpl) {
  GuestViewEvents.call(this, webViewImpl);

  this.setupWebRequestEvents();
  this.view.maybeSetupContextMenus();
}

WebViewEvents.prototype.__proto__ = GuestViewEvents.prototype;

// A dictionary of <webview> extension events to be listened for. This
// dictionary augments |GuestViewEvents.EVENTS| in guest_view_events.js. See the
// documentation there for details.
WebViewEvents.EVENTS = {
  'close': {
    evt: CreateEvent('webViewInternal.onClose')
  },
  'consolemessage': {
    evt: CreateEvent('webViewInternal.onConsoleMessage'),
    fields: ['level', 'message', 'line', 'sourceId']
  },
  'contentload': {
    evt: CreateEvent('webViewInternal.onContentLoad')
  },
  'dialog': {
    cancelable: true,
    evt: CreateEvent('webViewInternal.onDialog'),
    fields: ['defaultPromptText', 'messageText', 'messageType', 'url'],
    handler: 'handleDialogEvent'
  },
  'droplink': {
    evt: CreateEvent('webViewInternal.onDropLink'),
    fields: ['url']
  },
  'exit': {
    evt: CreateEvent('webViewInternal.onExit'),
    fields: ['processId', 'reason']
  },
  'exitfullscreen': {
    evt: CreateEvent('webViewInternal.onExitFullscreen'),
    fields: ['url'],
    handler: 'handleFullscreenExitEvent',
    internal: true
  },
  'findupdate': {
    evt: CreateEvent('webViewInternal.onFindReply'),
    fields: [
      'searchText',
      'numberOfMatches',
      'activeMatchOrdinal',
      'selectionRect',
      'canceled',
      'finalUpdate'
    ]
  },
  'framenamechanged': {
    evt: CreateEvent('webViewInternal.onFrameNameChanged'),
    handler: 'handleFrameNameChangedEvent',
    internal: true
  },
  'loadabort': {
    cancelable: true,
    evt: CreateEvent('webViewInternal.onLoadAbort'),
    fields: ['url', 'isTopLevel', 'code', 'reason'],
    handler: 'handleLoadAbortEvent'
  },
  'loadcommit': {
    evt: CreateEvent('webViewInternal.onLoadCommit'),
    fields: ['url', 'isTopLevel'],
    handler: 'handleLoadCommitEvent'
  },
  'loadprogress': {
    evt: CreateEvent('webViewInternal.onLoadProgress'),
    fields: ['url', 'progress']
  },
  'loadredirect': {
    evt: CreateEvent('webViewInternal.onLoadRedirect'),
    fields: ['isTopLevel', 'oldUrl', 'newUrl']
  },
  'loadstart': {
    evt: CreateEvent('webViewInternal.onLoadStart'),
    fields: ['url', 'isTopLevel']
  },
  'loadstop': {
    evt: CreateEvent('webViewInternal.onLoadStop')
  },
  'newwindow': {
    cancelable: true,
    evt: CreateEvent('webViewInternal.onNewWindow'),
    fields: [
      'initialHeight',
      'initialWidth',
      'targetUrl',
      'windowOpenDisposition',
      'name'
    ],
    handler: 'handleNewWindowEvent'
  },
  'permissionrequest': {
    cancelable: true,
    evt: CreateEvent('webViewInternal.onPermissionRequest'),
    fields: [
      'identifier',
      'lastUnlockedBySelf',
      'name',
      'permission',
      'requestMethod',
      'url',
      'userGesture'
    ],
    handler: 'handlePermissionEvent'
  },
  'responsive': {
    evt: CreateEvent('webViewInternal.onResponsive'),
    fields: ['processId']
  },
  'sizechanged': {
    evt: CreateEvent('webViewInternal.onSizeChanged'),
    fields: ['oldHeight', 'oldWidth', 'newHeight', 'newWidth'],
    handler: 'handleSizeChangedEvent'
  },
  'unresponsive': {
    evt: CreateEvent('webViewInternal.onUnresponsive'),
    fields: ['processId']
  },
  'zoomchange': {
    evt: CreateEvent('webViewInternal.onZoomChange'),
    fields: ['oldZoomFactor', 'newZoomFactor']
  }
};

WebViewEvents.prototype.setupWebRequestEvents = function() {
  var request = {};
  var createWebRequestEvent = function(webRequestEvent) {
    return this.weakWrapper(function() {
      if (!this[webRequestEvent.name]) {
        this[webRequestEvent.name] =
            new WebRequestEvent(
                'webViewInternal.' + webRequestEvent.name,
                webRequestEvent.parameters,
                webRequestEvent.extraParameters, webRequestEvent.options,
                this.view.viewInstanceId);
      }
      return this[webRequestEvent.name];
    });
  }.bind(this);

  var createDeclarativeWebRequestEvent = function(webRequestEvent) {
    return this.weakWrapper(function() {
      if (!this[webRequestEvent.name]) {
        // The onMessage event gets a special event type because we want
        // the listener to fire only for messages targeted for this particular
        // <webview>.
        var EventClass = webRequestEvent.name === 'onMessage' ?
            DeclarativeWebRequestEvent : EventBindings.Event;
        this[webRequestEvent.name] =
            new EventClass(
                'webViewInternal.declarativeWebRequest.' + webRequestEvent.name,
                webRequestEvent.parameters,
                webRequestEvent.options,
                this.view.viewInstanceId);
      }
      return this[webRequestEvent.name];
    });
  }.bind(this);

  for (var i = 0; i < DeclarativeWebRequestSchema.events.length; ++i) {
    var eventSchema = DeclarativeWebRequestSchema.events[i];
    var webRequestEvent = createDeclarativeWebRequestEvent(eventSchema);
    Object.defineProperty(
        request,
        eventSchema.name,
        {
          get: webRequestEvent,
          enumerable: true
        }
        );
  }

  // Populate the WebRequest events from the API definition.
  for (var i = 0; i < WebRequestSchema.events.length; ++i) {
    var webRequestEvent = createWebRequestEvent(WebRequestSchema.events[i]);
    Object.defineProperty(
        request,
        WebRequestSchema.events[i].name,
        {
          get: webRequestEvent,
          enumerable: true
        }
        );
  }

  this.view.setRequestPropertyOnWebViewElement(request);
};

WebViewEvents.prototype.getEvents = function() {
  return WebViewEvents.EVENTS;
};

WebViewEvents.prototype.handleDialogEvent = function(event, eventName) {
  var webViewEvent = this.makeDomEvent(event, eventName);
  new WebViewActionRequests.Dialog(this.view, event, webViewEvent);
};

WebViewEvents.prototype.handleFrameNameChangedEvent = function(event) {
  this.view.onFrameNameChanged(event.name);
};

WebViewEvents.prototype.handleFullscreenExitEvent = function(event, eventName) {
  document.webkitCancelFullScreen();
};

WebViewEvents.prototype.handleLoadAbortEvent = function(event, eventName) {
  var showWarningMessage = function(code, reason) {
    var WARNING_MSG_LOAD_ABORTED = '<webview>: ' +
        'The load has aborted with error %1: %2.';
    window.console.warn(
        WARNING_MSG_LOAD_ABORTED.replace('%1', code).replace('%2', reason));
  };
  var webViewEvent = this.makeDomEvent(event, eventName);
  if (this.view.dispatchEvent(webViewEvent)) {
    showWarningMessage(event.code, event.reason);
  }
};

WebViewEvents.prototype.handleLoadCommitEvent = function(event, eventName) {
  this.view.onLoadCommit(event.baseUrlForDataUrl,
                         event.currentEntryIndex,
                         event.entryCount,
                         event.processId,
                         event.url,
                         event.isTopLevel);
  var webViewEvent = this.makeDomEvent(event, eventName);
  this.view.dispatchEvent(webViewEvent);
};

WebViewEvents.prototype.handleNewWindowEvent = function(event, eventName) {
  var webViewEvent = this.makeDomEvent(event, eventName);
  new WebViewActionRequests.NewWindow(this.view, event, webViewEvent);
};

WebViewEvents.prototype.handlePermissionEvent = function(event, eventName) {
  var webViewEvent = this.makeDomEvent(event, eventName);
  if (event.permission === 'fullscreen') {
    new WebViewActionRequests.FullscreenPermissionRequest(
        this.view, event, webViewEvent);
  } else {
    new WebViewActionRequests.PermissionRequest(this.view, event, webViewEvent);
  }
};

WebViewEvents.prototype.handleSizeChangedEvent = function(event, eventName) {
  var webViewEvent = this.makeDomEvent(event, eventName);
  this.view.onSizeChanged(webViewEvent);
};

function DeclarativeWebRequestEvent(opt_eventName,
                                    opt_argSchemas,
                                    opt_eventOptions,
                                    opt_webViewInstanceId) {
  var subEventName = opt_eventName + '/' + IdGenerator.GetNextId();
  EventBindings.Event.call(this,
                           subEventName,
                           opt_argSchemas,
                           opt_eventOptions,
                           opt_webViewInstanceId);

  var view = GuestViewInternalNatives.GetViewFromID(opt_webViewInstanceId || 0);
  if (!view) {
    return;
  }
  view.events.addScopedListener(WebRequestMessageEvent, function() {
    // Re-dispatch to subEvent's listeners.
    $Function.apply(this.dispatch, this, $Array.slice(arguments));
  }.bind(this), {instanceId: opt_webViewInstanceId || 0});
}

DeclarativeWebRequestEvent.prototype.__proto__ = EventBindings.Event.prototype;

// Exports.
exports.$set('WebViewEvents', WebViewEvents);
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements experimental API for <webview>.
// See web_view.js and web_view_api_methods.js for details.
//
// <webview> Experimental API is only available on canary and channels of
// Chrome.

var WebViewImpl = require('webView').WebViewImpl;
var WebViewInternal = require('webViewInternal').WebViewInternal;

// An array of <webview>'s experimental API methods.  See |WEB_VIEW_API_METHODS|
// in web_view_api_methods.js for more details.
var WEB_VIEW_EXPERIMENTAL_API_METHODS = [
  // Captures the visible region of the WebView contents into a bitmap.
  'captureVisibleRegion'
];

// Registers the experimantal WebVIew API when available.
WebViewImpl.maybeGetExperimentalApiMethods = function() {
  return WEB_VIEW_EXPERIMENTAL_API_METHODS;
};
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

exports.$set(
    'WebViewInternal',
    require('binding').Binding.create('webViewInternal').generate());
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This module implements WebView (<webview>) as a custom element that wraps a
// BrowserPlugin object element. The object element is hidden within
// the shadow DOM of the WebView element.

var DocumentNatives = requireNative('document_natives');
var GuestView = require('guestView').GuestView;
var GuestViewContainer = require('guestViewContainer').GuestViewContainer;
var GuestViewInternalNatives = requireNative('guest_view_internal');
var WebViewConstants = require('webViewConstants').WebViewConstants;
var WebViewEvents = require('webViewEvents').WebViewEvents;
var WebViewInternal = require('webViewInternal').WebViewInternal;

// Represents the internal state of <webview>.
function WebViewImpl(webviewElement) {
  GuestViewContainer.call(this, webviewElement, 'webview');
  this.cachedZoom = 1;
  this.setupElementProperties();
  new WebViewEvents(this, this.viewInstanceId);
}

WebViewImpl.prototype.__proto__ = GuestViewContainer.prototype;

WebViewImpl.VIEW_TYPE = 'WebView';

// Add extra functionality to |this.element|.
WebViewImpl.setupElement = function(proto) {
  // Public-facing API methods.
  var apiMethods = WebViewImpl.getApiMethods();

  // Add the experimental API methods, if available.
  var experimentalApiMethods = WebViewImpl.maybeGetExperimentalApiMethods();
  apiMethods = $Array.concat(apiMethods, experimentalApiMethods);

  // Create default implementations for undefined API methods.
  var createDefaultApiMethod = function(m) {
    return function(var_args) {
      if (!this.guest.getId()) {
        return false;
      }
      var args = $Array.concat([this.guest.getId()], $Array.slice(arguments));
      $Function.apply(WebViewInternal[m], null, args);
      return true;
    };
  };
  for (var i = 0; i != apiMethods.length; ++i) {
    if (WebViewImpl.prototype[apiMethods[i]] == undefined) {
      WebViewImpl.prototype[apiMethods[i]] =
          createDefaultApiMethod(apiMethods[i]);
    }
  }

  // Forward proto.foo* method calls to WebViewImpl.foo*.
  GuestViewContainer.forwardApiMethods(proto, apiMethods);
};

// Initiates navigation once the <webview> element is attached to the DOM.
WebViewImpl.prototype.onElementAttached = function() {
  // Mark all attributes as dirty on attachment.
  for (var i in this.attributes) {
    this.attributes[i].dirty = true;
  }
  for (var i in this.attributes) {
    this.attributes[i].attach();
  }
};

// Resets some state upon detaching <webview> element from the DOM.
WebViewImpl.prototype.onElementDetached = function() {
  this.guest.destroy();
  for (var i in this.attributes) {
    this.attributes[i].dirty = false;
  }
  for (var i in this.attributes) {
    this.attributes[i].detach();
  }
};

// Sets the <webview>.request property.
WebViewImpl.prototype.setRequestPropertyOnWebViewElement = function(request) {
  Object.defineProperty(
      this.element,
      'request',
      {
        value: request,
        enumerable: true
      }
  );
};

WebViewImpl.prototype.setupElementProperties = function() {
  // We cannot use {writable: true} property descriptor because we want a
  // dynamic getter value.
  Object.defineProperty(this.element, 'contentWindow', {
    get: function() {
      return this.guest.getContentWindow();
    }.bind(this),
    // No setter.
    enumerable: true
  });
};

WebViewImpl.prototype.onSizeChanged = function(webViewEvent) {
  var newWidth = webViewEvent.newWidth;
  var newHeight = webViewEvent.newHeight;

  var element = this.element;

  var width = element.offsetWidth;
  var height = element.offsetHeight;

  // Check the current bounds to make sure we do not resize <webview>
  // outside of current constraints.
  var maxWidth = this.attributes[
    WebViewConstants.ATTRIBUTE_MAXWIDTH].getValue() || width;
  var minWidth = this.attributes[
    WebViewConstants.ATTRIBUTE_MINWIDTH].getValue() || width;
  var maxHeight = this.attributes[
    WebViewConstants.ATTRIBUTE_MAXHEIGHT].getValue() || height;
  var minHeight = this.attributes[
    WebViewConstants.ATTRIBUTE_MINHEIGHT].getValue() || height;

  minWidth = Math.min(minWidth, maxWidth);
  minHeight = Math.min(minHeight, maxHeight);

  if (!this.attributes[WebViewConstants.ATTRIBUTE_AUTOSIZE].getValue() ||
      (newWidth >= minWidth &&
      newWidth <= maxWidth &&
      newHeight >= minHeight &&
      newHeight <= maxHeight)) {
    element.style.width = newWidth + 'px';
    element.style.height = newHeight + 'px';
    // Only fire the DOM event if the size of the <webview> has actually
    // changed.
    this.dispatchEvent(webViewEvent);
  }
};

WebViewImpl.prototype.createGuest = function() {
  this.guest.create(this.buildParams(), function() {
    this.attachWindow$();
  }.bind(this));
};

WebViewImpl.prototype.onFrameNameChanged = function(name) {
  this.attributes[WebViewConstants.ATTRIBUTE_NAME].setValueIgnoreMutation(name);
};

// Updates state upon loadcommit.
WebViewImpl.prototype.onLoadCommit = function(
    baseUrlForDataUrl, currentEntryIndex, entryCount,
    processId, url, isTopLevel) {
  this.baseUrlForDataUrl = baseUrlForDataUrl;
  this.currentEntryIndex = currentEntryIndex;
  this.entryCount = entryCount;
  this.processId = processId;
  if (isTopLevel) {
    // Touching the src attribute triggers a navigation. To avoid
    // triggering a page reload on every guest-initiated navigation,
    // we do not handle this mutation.
    this.attributes[
        WebViewConstants.ATTRIBUTE_SRC].setValueIgnoreMutation(url);
  }
};

WebViewImpl.prototype.onAttach = function(storagePartitionId) {
  this.attributes[WebViewConstants.ATTRIBUTE_PARTITION].setValueIgnoreMutation(
      storagePartitionId);
};

WebViewImpl.prototype.buildContainerParams = function() {
  var params = { 'initialZoomFactor': this.cachedZoomFactor,
                 'userAgentOverride': this.userAgentOverride };
  for (var i in this.attributes) {
    var value = this.attributes[i].getValueIfDirty();
    if (value)
      params[i] = value;
  }
  return params;
};

WebViewImpl.prototype.attachWindow$ = function(opt_guestInstanceId) {
  // If |opt_guestInstanceId| was provided, then a different existing guest is
  // being attached to this webview, and the current one will get destroyed.
  if (opt_guestInstanceId) {
    if (this.guest.getId() == opt_guestInstanceId) {
      return true;
    }
    this.guest.destroy();
    this.guest = new GuestView('webview', opt_guestInstanceId);
  }

  return GuestViewContainer.prototype.attachWindow$.call(this);
};

// Shared implementation of executeScript() and insertCSS().
WebViewImpl.prototype.executeCode = function(func, args) {
  if (!this.guest.getId()) {
    window.console.error(WebViewConstants.ERROR_MSG_CANNOT_INJECT_SCRIPT);
    return false;
  }

  var webviewSrc = this.attributes[WebViewConstants.ATTRIBUTE_SRC].getValue();
  if (this.baseUrlForDataUrl) {
    webviewSrc = this.baseUrlForDataUrl;
  }

  args = $Array.concat([this.guest.getId(), webviewSrc],
                       $Array.slice(args));
  $Function.apply(func, null, args);
  return true;
}

// Requests the <webview> element wihtin the embedder to enter fullscreen.
WebViewImpl.prototype.makeElementFullscreen = function() {
  GuestViewInternalNatives.RunWithGesture(function() {
    this.element.webkitRequestFullScreen();
  }.bind(this));
};

// Implemented when the ChromeWebView API is available.
WebViewImpl.prototype.maybeSetupContextMenus = function() {};

// Implemented when the experimental WebView API is available.
WebViewImpl.maybeGetExperimentalApiMethods = function() {
  return [];
};

GuestViewContainer.registerElement(WebViewImpl);

// Exports.
exports.$set('WebViewImpl', WebViewImpl);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the chrome.app.runtime API.

var binding = require('binding').Binding.create('app.runtime');

var AppViewGuestInternal =
    require('binding').Binding.create('appViewGuestInternal').generate();
var eventBindings = require('event_bindings');
var fileSystemHelpers = requireNative('file_system_natives');
var GetIsolatedFileSystem = fileSystemHelpers.GetIsolatedFileSystem;
var entryIdManager = require('entryIdManager');

eventBindings.registerArgumentMassager('app.runtime.onEmbedRequested',
    function(args, dispatch) {
  var appEmbeddingRequest = args[0];
  var id = appEmbeddingRequest.guestInstanceId;
  delete appEmbeddingRequest.guestInstanceId;
  appEmbeddingRequest.allow = function(url) {
    AppViewGuestInternal.attachFrame(url, id);
  };

  appEmbeddingRequest.deny = function() {
    AppViewGuestInternal.denyRequest(id);
  };

  dispatch([appEmbeddingRequest]);
});

eventBindings.registerArgumentMassager('app.runtime.onLaunched',
    function(args, dispatch) {
  var launchData = args[0];
  if (launchData.items) {
    // An onLaunched corresponding to file_handlers in the app's manifest.
    var items = [];
    var numItems = launchData.items.length;
    var itemLoaded = function(err, item) {
      if (err) {
        console.error('Error getting fileEntry, code: ' + err.code);
      } else {
        $Array.push(items, item);
      }
      if (--numItems === 0) {
        var data = {
          isKioskSession: launchData.isKioskSession,
          isPublicSession: launchData.isPublicSession,
          source: launchData.source
        };
        if (items.length !== 0) {
          data.id = launchData.id;
          data.items = items;
        }
        dispatch([data]);
      }
    };
    $Array.forEach(launchData.items, function(item) {
      var fs = GetIsolatedFileSystem(item.fileSystemId);
      if (item.isDirectory) {
        fs.root.getDirectory(item.baseName, {}, function(dirEntry) {
          entryIdManager.registerEntry(item.entryId, dirEntry);
          itemLoaded(null, {entry: dirEntry});
        }, function(fileError) {
          itemLoaded(fileError);
        });
      } else {
        fs.root.getFile(item.baseName, {}, function(fileEntry) {
          entryIdManager.registerEntry(item.entryId, fileEntry);
          itemLoaded(null, {entry: fileEntry, type: item.mimeType});
        }, function(fileError) {
          itemLoaded(fileError);
        });
      }
    });
  } else {
    // Default case. This currently covers an onLaunched corresponding to
    // url_handlers in the app's manifest.
    dispatch([launchData]);
  }
});

exports.$set('binding', binding.generate());
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the app_window API.

var appWindowNatives = requireNative('app_window_natives');
var runtimeNatives = requireNative('runtime');
var Binding = require('binding').Binding;
var Event = require('event_bindings').Event;
var forEach = require('utils').forEach;
var renderFrameObserverNatives = requireNative('renderFrameObserverNatives');

var appWindowData = null;
var currentAppWindow = null;
var currentWindowInternal = null;

var kSetBoundsFunction = 'setBounds';
var kSetSizeConstraintsFunction = 'setSizeConstraints';

// Bounds class definition.
var Bounds = function(boundsKey) {
  privates(this).boundsKey_ = boundsKey;
};
Object.defineProperty(Bounds.prototype, 'left', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].left;
  },
  set: function(left) {
    this.setPosition(left, null);
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'top', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].top;
  },
  set: function(top) {
    this.setPosition(null, top);
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'width', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].width;
  },
  set: function(width) {
    this.setSize(width, null);
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'height', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].height;
  },
  set: function(height) {
    this.setSize(null, height);
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'minWidth', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].minWidth;
  },
  set: function(minWidth) {
    updateSizeConstraints(privates(this).boundsKey_, { minWidth: minWidth });
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'maxWidth', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].maxWidth;
  },
  set: function(maxWidth) {
    updateSizeConstraints(privates(this).boundsKey_, { maxWidth: maxWidth });
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'minHeight', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].minHeight;
  },
  set: function(minHeight) {
    updateSizeConstraints(privates(this).boundsKey_, { minHeight: minHeight });
  },
  enumerable: true
});
Object.defineProperty(Bounds.prototype, 'maxHeight', {
  get: function() {
    return appWindowData[privates(this).boundsKey_].maxHeight;
  },
  set: function(maxHeight) {
    updateSizeConstraints(privates(this).boundsKey_, { maxHeight: maxHeight });
  },
  enumerable: true
});
Bounds.prototype.setPosition = function(left, top) {
  updateBounds(privates(this).boundsKey_, { left: left, top: top });
};
Bounds.prototype.setSize = function(width, height) {
  updateBounds(privates(this).boundsKey_, { width: width, height: height });
};
Bounds.prototype.setMinimumSize = function(minWidth, minHeight) {
  updateSizeConstraints(privates(this).boundsKey_,
                        { minWidth: minWidth, minHeight: minHeight });
};
Bounds.prototype.setMaximumSize = function(maxWidth, maxHeight) {
  updateSizeConstraints(privates(this).boundsKey_,
                        { maxWidth: maxWidth, maxHeight: maxHeight });
};

var appWindow = Binding.create('app.window');
appWindow.registerCustomHook(function(bindingsAPI) {
  var apiFunctions = bindingsAPI.apiFunctions;

  apiFunctions.setCustomCallback('create',
      function(name, request, callback, windowParams) {
    var view = null;

    // When window creation fails, |windowParams| will be undefined.
    if (windowParams && windowParams.frameId) {
      view = appWindowNatives.GetFrame(
          windowParams.frameId, true /* notifyBrowser */);
    }

    if (!view) {
      // No route to created window. If given a callback, trigger it with an
      // undefined object.
      if (callback)
        callback();
      return;
    }

    if (windowParams.existingWindow) {
      // Not creating a new window, but activating an existing one, so trigger
      // callback with existing window and don't do anything else.
      if (callback)
        callback(view.chrome.app.window.current());
      return;
    }

    // Initialize appWindowData in the newly created JS context
    if (view.chrome.app) {
      view.chrome.app.window.initializeAppWindow(windowParams);
    } else {
      var sandbox_window_message = 'Creating sandboxed window, it doesn\'t ' +
          'have access to the chrome.app API.';
      if (callback) {
        sandbox_window_message = sandbox_window_message +
            ' The chrome.app.window.create callback will be called, but ' +
            'there will be no object provided for the sandboxed window.';
      }
      console.warn(sandbox_window_message);
    }

    if (callback) {
      if (!view || !view.chrome.app /* sandboxed window */) {
        callback(undefined);
        return;
      }

      var willCallback =
          renderFrameObserverNatives.OnDocumentElementCreated(
              windowParams.frameId,
              function(success) {
                if (success) {
                  callback(view.chrome.app.window.current());
                } else {
                  callback(undefined);
                }
              });
      if (!willCallback) {
        callback(undefined);
      }
    }
  });

  apiFunctions.setHandleRequest('current', function() {
    if (!currentAppWindow) {
      console.error('The JavaScript context calling ' +
                    'chrome.app.window.current() has no associated AppWindow.');
      return null;
    }
    return currentAppWindow;
  });

  apiFunctions.setHandleRequest('getAll', function() {
    var views = runtimeNatives.GetExtensionViews(-1, 'APP_WINDOW');
    return $Array.map(views, function(win) {
      return win.chrome.app.window.current();
    });
  });

  apiFunctions.setHandleRequest('get', function(id) {
    var windows = $Array.filter(chrome.app.window.getAll(), function(win) {
      return win.id == id;
    });
    return windows.length > 0 ? windows[0] : null;
  });

  apiFunctions.setHandleRequest('canSetVisibleOnAllWorkspaces', function() {
    return /Mac/.test(navigator.platform) || /Linux/.test(navigator.userAgent);
  });

  // This is an internal function, but needs to be bound into a closure
  // so the correct JS context is used for global variables such as
  // currentWindowInternal, appWindowData, etc.
  apiFunctions.setHandleRequest('initializeAppWindow', function(params) {
    currentWindowInternal =
        Binding.create('app.currentWindowInternal').generate();
    var AppWindow = function() {
      this.innerBounds = new Bounds('innerBounds');
      this.outerBounds = new Bounds('outerBounds');
    };
    forEach(currentWindowInternal, function(key, value) {
      // Do not add internal functions that should not appear in the AppWindow
      // interface. They are called by Bounds mutators.
      if (key !== kSetBoundsFunction && key !== kSetSizeConstraintsFunction)
        AppWindow.prototype[key] = value;
    });
    AppWindow.prototype.moveTo = $Function.bind(window.moveTo, window);
    AppWindow.prototype.resizeTo = $Function.bind(window.resizeTo, window);
    AppWindow.prototype.contentWindow = window;
    AppWindow.prototype.onClosed = new Event();
    AppWindow.prototype.onWindowFirstShownForTests = new Event();
    AppWindow.prototype.close = function() {
      this.contentWindow.close();
    };
    AppWindow.prototype.getBounds = function() {
      // This is to maintain backcompatibility with a bug on Windows and
      // ChromeOS, which returns the position of the window but the size of
      // the content.
      var innerBounds = appWindowData.innerBounds;
      var outerBounds = appWindowData.outerBounds;
      return { left: outerBounds.left, top: outerBounds.top,
               width: innerBounds.width, height: innerBounds.height };
    };
    AppWindow.prototype.setBounds = function(bounds) {
      updateBounds('bounds', bounds);
    };
    AppWindow.prototype.isFullscreen = function() {
      return appWindowData.fullscreen;
    };
    AppWindow.prototype.isMinimized = function() {
      return appWindowData.minimized;
    };
    AppWindow.prototype.isMaximized = function() {
      return appWindowData.maximized;
    };
    AppWindow.prototype.isAlwaysOnTop = function() {
      return appWindowData.alwaysOnTop;
    };
    AppWindow.prototype.alphaEnabled = function() {
      return appWindowData.alphaEnabled;
    };
    AppWindow.prototype.handleWindowFirstShownForTests = function(callback) {
      // This allows test apps to get have their callback run even if they
      // call this after the first show has happened.
      if (this.firstShowHasHappened) {
        callback();
        return;
      }
      this.onWindowFirstShownForTests.addListener(callback);
    }

    Object.defineProperty(AppWindow.prototype, 'id', {get: function() {
      return appWindowData.id;
    }});

    // These properties are for testing.
    Object.defineProperty(
        AppWindow.prototype, 'hasFrameColor', {get: function() {
      return appWindowData.hasFrameColor;
    }});

    Object.defineProperty(AppWindow.prototype, 'activeFrameColor',
                          {get: function() {
      return appWindowData.activeFrameColor;
    }});

    Object.defineProperty(AppWindow.prototype, 'inactiveFrameColor',
                          {get: function() {
      return appWindowData.inactiveFrameColor;
    }});

    appWindowData = {
      id: params.id || '',
      innerBounds: {
        left: params.innerBounds.left,
        top: params.innerBounds.top,
        width: params.innerBounds.width,
        height: params.innerBounds.height,

        minWidth: params.innerBounds.minWidth,
        minHeight: params.innerBounds.minHeight,
        maxWidth: params.innerBounds.maxWidth,
        maxHeight: params.innerBounds.maxHeight
      },
      outerBounds: {
        left: params.outerBounds.left,
        top: params.outerBounds.top,
        width: params.outerBounds.width,
        height: params.outerBounds.height,

        minWidth: params.outerBounds.minWidth,
        minHeight: params.outerBounds.minHeight,
        maxWidth: params.outerBounds.maxWidth,
        maxHeight: params.outerBounds.maxHeight
      },
      fullscreen: params.fullscreen,
      minimized: params.minimized,
      maximized: params.maximized,
      alwaysOnTop: params.alwaysOnTop,
      hasFrameColor: params.hasFrameColor,
      activeFrameColor: params.activeFrameColor,
      inactiveFrameColor: params.inactiveFrameColor,
      alphaEnabled: params.alphaEnabled
    };
    currentAppWindow = new AppWindow;
  });
});

function boundsEqual(bounds1, bounds2) {
  if (!bounds1 || !bounds2)
    return false;
  return (bounds1.left == bounds2.left && bounds1.top == bounds2.top &&
          bounds1.width == bounds2.width && bounds1.height == bounds2.height);
}

function dispatchEventIfExists(target, name) {
  // Sometimes apps like to put their own properties on the window which
  // break our assumptions.
  var event = target[name];
  if (event && (typeof event.dispatch == 'function'))
    event.dispatch();
  else
    console.warn('Could not dispatch ' + name + ', event has been clobbered');
}

function updateAppWindowProperties(update) {
  if (!appWindowData)
    return;

  var oldData = appWindowData;
  update.id = oldData.id;
  appWindowData = update;

  var currentWindow = currentAppWindow;

  if (!boundsEqual(oldData.innerBounds, update.innerBounds))
    dispatchEventIfExists(currentWindow, "onBoundsChanged");

  if (!oldData.fullscreen && update.fullscreen)
    dispatchEventIfExists(currentWindow, "onFullscreened");
  if (!oldData.minimized && update.minimized)
    dispatchEventIfExists(currentWindow, "onMinimized");
  if (!oldData.maximized && update.maximized)
    dispatchEventIfExists(currentWindow, "onMaximized");

  if ((oldData.fullscreen && !update.fullscreen) ||
      (oldData.minimized && !update.minimized) ||
      (oldData.maximized && !update.maximized))
    dispatchEventIfExists(currentWindow, "onRestored");

  if (oldData.alphaEnabled !== update.alphaEnabled)
    dispatchEventIfExists(currentWindow, "onAlphaEnabledChanged");
};

function onAppWindowShownForTests() {
  if (!currentAppWindow)
    return;

  if (!currentAppWindow.firstShowHasHappened)
    dispatchEventIfExists(currentAppWindow, "onWindowFirstShownForTests");

  currentAppWindow.firstShowHasHappened = true;
}

function onAppWindowClosed() {
  if (!currentAppWindow)
    return;
  dispatchEventIfExists(currentAppWindow, "onClosed");
}

function updateBounds(boundsType, bounds) {
  if (!currentWindowInternal)
    return;

  currentWindowInternal.setBounds(boundsType, bounds);
}

function updateSizeConstraints(boundsType, constraints) {
  if (!currentWindowInternal)
    return;

  forEach(constraints, function(key, value) {
    // From the perspective of the API, null is used to reset constraints.
    // We need to convert this to 0 because a value of null is interpreted
    // the same as undefined in the browser and leaves the constraint unchanged.
    if (value === null)
      constraints[key] = 0;
  });

  currentWindowInternal.setSizeConstraints(boundsType, constraints);
}

exports.$set('binding', appWindow.generate());
exports.$set('onAppWindowClosed', onAppWindowClosed);
exports.$set('updateAppWindowProperties', updateAppWindowProperties);
exports.$set('appWindowShownForTests', onAppWindowShownForTests);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var Event = require('event_bindings').Event;
var forEach = require('utils').forEach;
// Note: Beware sneaky getters/setters when using GetAvailbility(). Use safe/raw
// variables as arguments.
var GetAvailability = requireNative('v8_context').GetAvailability;
var exceptionHandler = require('uncaught_exception_handler');
var lastError = require('lastError');
var logActivity = requireNative('activityLogger');
var logging = requireNative('logging');
var process = requireNative('process');
var schemaRegistry = requireNative('schema_registry');
var schemaUtils = require('schemaUtils');
var utils = require('utils');
var sendRequestHandler = require('sendRequest');

var contextType = process.GetContextType();
var extensionId = process.GetExtensionId();
var manifestVersion = process.GetManifestVersion();
var sendRequest = sendRequestHandler.sendRequest;

// Stores the name and definition of each API function, with methods to
// modify their behaviour (such as a custom way to handle requests to the
// API, a custom callback, etc).
function APIFunctions(namespace) {
  this.apiFunctions_ = {};
  this.unavailableApiFunctions_ = {};
  this.namespace = namespace;
}

APIFunctions.prototype.register = function(apiName, apiFunction) {
  this.apiFunctions_[apiName] = apiFunction;
};

// Registers a function as existing but not available, meaning that calls to
// the set* methods that reference this function should be ignored rather
// than throwing Errors.
APIFunctions.prototype.registerUnavailable = function(apiName) {
  this.unavailableApiFunctions_[apiName] = apiName;
};

APIFunctions.prototype.setHook_ =
    function(apiName, propertyName, customizedFunction) {
  if ($Object.hasOwnProperty(this.unavailableApiFunctions_, apiName))
    return;
  if (!$Object.hasOwnProperty(this.apiFunctions_, apiName))
    throw new Error('Tried to set hook for unknown API "' + apiName + '"');
  this.apiFunctions_[apiName][propertyName] = customizedFunction;
};

APIFunctions.prototype.setHandleRequest =
    function(apiName, customizedFunction) {
  var prefix = this.namespace;
  return this.setHook_(apiName, 'handleRequest',
    function() {
      var ret = $Function.apply(customizedFunction, this, arguments);
      // Logs API calls to the Activity Log if it doesn't go through an
      // ExtensionFunction.
      if (!sendRequestHandler.getCalledSendRequest())
        logActivity.LogAPICall(extensionId, prefix + "." + apiName,
            $Array.slice(arguments));
      return ret;
    });
};

APIFunctions.prototype.setHandleRequestWithPromise =
    function(apiName, customizedFunction) {
  var prefix = this.namespace;
  return this.setHook_(apiName, 'handleRequest', function() {
      var name = prefix + '.' + apiName;
      logActivity.LogAPICall(extensionId, name, $Array.slice(arguments));
      var stack = exceptionHandler.getExtensionStackTrace();
      var callback = arguments[arguments.length - 1];
      var args = $Array.slice(arguments, 0, arguments.length - 1);
      var keepAlivePromise = requireAsync('keep_alive').then(function(module) {
        return module.createKeepAlive();
      });
      $Function.apply(customizedFunction, this, args).then(function(result) {
        if (callback) {
          sendRequestHandler.safeCallbackApply(name, {'stack': stack}, callback,
                                               [result]);
        }
      }).catch(function(error) {
        if (callback) {
          var message = exceptionHandler.safeErrorToString(error, true);
          lastError.run(name, message, stack, callback);
        }
      }).then(function() {
        keepAlivePromise.then(function(keepAlive) {
          keepAlive.close();
        });
      });
    });
};

APIFunctions.prototype.setUpdateArgumentsPostValidate =
    function(apiName, customizedFunction) {
  return this.setHook_(
    apiName, 'updateArgumentsPostValidate', customizedFunction);
};

APIFunctions.prototype.setUpdateArgumentsPreValidate =
    function(apiName, customizedFunction) {
  return this.setHook_(
    apiName, 'updateArgumentsPreValidate', customizedFunction);
};

APIFunctions.prototype.setCustomCallback =
    function(apiName, customizedFunction) {
  return this.setHook_(apiName, 'customCallback', customizedFunction);
};

function CustomBindingsObject() {
}

CustomBindingsObject.prototype.setSchema = function(schema) {
  // The functions in the schema are in list form, so we move them into a
  // dictionary for easier access.
  var self = this;
  self.functionSchemas = {};
  $Array.forEach(schema.functions, function(f) {
    self.functionSchemas[f.name] = {
      name: f.name,
      definition: f
    }
  });
};

// Get the platform from navigator.appVersion.
function getPlatform() {
  var platforms = [
    [/CrOS Touch/, "chromeos touch"],
    [/CrOS/, "chromeos"],
    [/Linux/, "linux"],
    [/Mac/, "mac"],
    [/Win/, "win"],
  ];

  for (var i = 0; i < platforms.length; i++) {
    if ($RegExp.exec(platforms[i][0], navigator.appVersion)) {
      return platforms[i][1];
    }
  }
  return "unknown";
}

function isPlatformSupported(schemaNode, platform) {
  return !schemaNode.platforms ||
      $Array.indexOf(schemaNode.platforms, platform) > -1;
}

function isManifestVersionSupported(schemaNode, manifestVersion) {
  return !schemaNode.maximumManifestVersion ||
      manifestVersion <= schemaNode.maximumManifestVersion;
}

function isSchemaNodeSupported(schemaNode, platform, manifestVersion) {
  return isPlatformSupported(schemaNode, platform) &&
      isManifestVersionSupported(schemaNode, manifestVersion);
}

function createCustomType(type) {
  var jsModuleName = type.js_module;
  logging.CHECK(jsModuleName, 'Custom type ' + type.id +
                ' has no "js_module" property.');
  // This list contains all types that has a js_module property. It is ugly to
  // hard-code them here, but the number of APIs that use js_module has not
  // changed since the introduction of js_modules in crbug.com/222156.
  // This whitelist serves as an extra line of defence to avoid exposing
  // arbitrary extension modules when the |type| definition is poisoned.
  var whitelistedModules = [
    'ChromeDirectSetting',
    'ChromeSetting',
    'ContentSetting',
    'StorageArea',
  ];
  logging.CHECK($Array.indexOf(whitelistedModules, jsModuleName) !== -1,
                'Module ' + jsModuleName + ' does not define a custom type.');
  var jsModule = require(jsModuleName);
  logging.CHECK(jsModule, 'No module ' + jsModuleName + ' found for ' +
                type.id + '.');
  var customType = jsModule[jsModuleName];
  logging.CHECK(customType, jsModuleName + ' must export itself.');
  customType.prototype = new CustomBindingsObject();
  customType.prototype.setSchema(type);
  return customType;
}

var platform = getPlatform();

function Binding(apiName) {
  this.apiName_ = apiName;
  this.apiFunctions_ = new APIFunctions(apiName);
  this.customEvent_ = null;
  this.customHooks_ = [];
};

Binding.create = function(apiName) {
  return new Binding(apiName);
};

Binding.prototype = {
  // The API through which the ${api_name}_custom_bindings.js files customize
  // their API bindings beyond what can be generated.
  //
  // There are 2 types of customizations available: those which are required in
  // order to do the schema generation (registerCustomEvent and
  // registerCustomType), and those which can only run after the bindings have
  // been generated (registerCustomHook).

  // Registers a custom event type for the API identified by |namespace|.
  // |event| is the event's constructor.
  registerCustomEvent: function(event) {
    this.customEvent_ = event;
  },

  // Registers a function |hook| to run after the schema for all APIs has been
  // generated.  The hook is passed as its first argument an "API" object to
  // interact with, and second the current extension ID. See where
  // |customHooks| is used.
  registerCustomHook: function(fn) {
    $Array.push(this.customHooks_, fn);
  },

  // TODO(kalman/cduvall): Refactor this so |runHooks_| is not needed.
  runHooks_: function(api, schema) {
    $Array.forEach(this.customHooks_, function(hook) {
      if (!isSchemaNodeSupported(schema, platform, manifestVersion))
        return;

      if (!hook)
        return;

      hook({
        apiFunctions: this.apiFunctions_,
        schema: schema,
        compiledApi: api
      }, extensionId, contextType);
    }, this);
  },

  // Generates the bindings from the schema for |this.apiName_| and integrates
  // any custom bindings that might be present.
  generate: function() {
    // NB: It's important to load the schema during generation rather than
    // setting it beforehand so that we're more confident the schema we're
    // loading is real, and not one that was injected by a page intercepting
    // Binding.generate.
    // Additionally, since the schema is an object returned from a native
    // handler, its properties don't have the custom getters/setters that a page
    // may have put on Object.prototype, and the object is frozen by v8.
    var schema = schemaRegistry.GetSchema(this.apiName_);

    function shouldCheckUnprivileged() {
      var shouldCheck = 'unprivileged' in schema;
      if (shouldCheck)
        return shouldCheck;

      $Array.forEach(['functions', 'events'], function(type) {
        if ($Object.hasOwnProperty(schema, type)) {
          $Array.forEach(schema[type], function(node) {
            if ('unprivileged' in node)
              shouldCheck = true;
          });
        }
      });
      if (shouldCheck)
        return shouldCheck;

      for (var property in schema.properties) {
        if ($Object.hasOwnProperty(schema, property) &&
            'unprivileged' in schema.properties[property]) {
          shouldCheck = true;
          break;
        }
      }
      return shouldCheck;
    }
    var checkUnprivileged = shouldCheckUnprivileged();

    // TODO(kalman/cduvall): Make GetAvailability handle this, then delete the
    // supporting code.
    if (!isSchemaNodeSupported(schema, platform, manifestVersion)) {
      console.error('chrome.' + schema.namespace + ' is not supported on ' +
                    'this platform or manifest version');
      return undefined;
    }

    var mod = {};

    var namespaces = $String.split(schema.namespace, '.');
    for (var index = 0, name; name = namespaces[index]; index++) {
      mod[name] = mod[name] || {};
      mod = mod[name];
    }

    if (schema.types) {
      $Array.forEach(schema.types, function(t) {
        if (!isSchemaNodeSupported(t, platform, manifestVersion))
          return;

        // Add types to global schemaValidator; the types we depend on from
        // other namespaces will be added as needed.
        schemaUtils.schemaValidator.addTypes(t);

        // Generate symbols for enums.
        var enumValues = t['enum'];
        if (enumValues) {
          // Type IDs are qualified with the namespace during compilation,
          // unfortunately, so remove it here.
          logging.DCHECK($String.substr(t.id, 0, schema.namespace.length) ==
                             schema.namespace);
          // Note: + 1 because it ends in a '.', e.g., 'fooApi.Type'.
          var id = $String.substr(t.id, schema.namespace.length + 1);
          mod[id] = {};
          $Array.forEach(enumValues, function(enumValue) {
            // Note: enums can be declared either as a list of strings
            // ['foo', 'bar'] or as a list of objects
            // [{'name': 'foo'}, {'name': 'bar'}].
            enumValue = $Object.hasOwnProperty(enumValue, 'name') ?
                enumValue.name : enumValue;
            if (enumValue) {  // Avoid setting any empty enums.
              // Make all properties in ALL_CAPS_STYLE.
              //
              // The built-in versions of $String.replace call other built-ins,
              // which may be clobbered. Instead, manually build the property
              // name.
              //
              // If the first character is a digit (we know it must be one of
              // a digit, a letter, or an underscore), precede it with an
              // underscore.
              var propertyName = ($RegExp.exec(/\d/, enumValue[0])) ? '_' : '';
              for (var i = 0; i < enumValue.length; ++i) {
                var next;
                if (i > 0 && $RegExp.exec(/[a-z]/, enumValue[i-1]) &&
                    $RegExp.exec(/[A-Z]/, enumValue[i])) {
                  // Replace myEnum-Foo with my_Enum-Foo:
                  next = '_' + enumValue[i];
                } else if ($RegExp.exec(/\W/, enumValue[i])) {
                  // Replace my_Enum-Foo with my_Enum_Foo:
                  next = '_';
                } else {
                  next = enumValue[i];
                }
                propertyName += next;
              }
              // Uppercase (replace my_Enum_Foo with MY_ENUM_FOO):
              propertyName = $String.toUpperCase(propertyName);
              mod[id][propertyName] = enumValue;
            }
          });
        }
      }, this);
    }

    // TODO(cduvall): Take out when all APIs have been converted to features.
    // Returns whether access to the content of a schema should be denied,
    // based on the presence of "unprivileged" and whether this is an
    // extension process (versus e.g. a content script).
    function isSchemaAccessAllowed(itemSchema) {
      return (contextType == 'BLESSED_EXTENSION') ||
             schema.unprivileged ||
             itemSchema.unprivileged;
    };

    // Setup Functions.
    if (schema.functions) {
      $Array.forEach(schema.functions, function(functionDef) {
        if (functionDef.name in mod) {
          throw new Error('Function ' + functionDef.name +
                          ' already defined in ' + schema.namespace);
        }

        if (!isSchemaNodeSupported(functionDef, platform, manifestVersion)) {
          this.apiFunctions_.registerUnavailable(functionDef.name);
          return;
        }

        var apiFunction = {};
        apiFunction.definition = functionDef;
        var apiFunctionName = schema.namespace + '.' + functionDef.name;
        apiFunction.name = apiFunctionName;

        if (!GetAvailability(apiFunctionName).is_available ||
            (checkUnprivileged && !isSchemaAccessAllowed(functionDef))) {
          this.apiFunctions_.registerUnavailable(functionDef.name);
          return;
        }

        // TODO(aa): It would be best to run this in a unit test, but in order
        // to do that we would need to better factor this code so that it
        // doesn't depend on so much v8::Extension machinery.
        if (logging.DCHECK_IS_ON() &&
            schemaUtils.isFunctionSignatureAmbiguous(apiFunction.definition)) {
          throw new Error(
              apiFunction.name + ' has ambiguous optional arguments. ' +
              'To implement custom disambiguation logic, add ' +
              '"allowAmbiguousOptionalArguments" to the function\'s schema.');
        }

        this.apiFunctions_.register(functionDef.name, apiFunction);

        mod[functionDef.name] = $Function.bind(function() {
          var args = $Array.slice(arguments);
          if (this.updateArgumentsPreValidate)
            args = $Function.apply(this.updateArgumentsPreValidate, this, args);

          args = schemaUtils.normalizeArgumentsAndValidate(args, this);
          if (this.updateArgumentsPostValidate) {
            args = $Function.apply(this.updateArgumentsPostValidate,
                                   this,
                                   args);
          }

          sendRequestHandler.clearCalledSendRequest();

          var retval;
          if (this.handleRequest) {
            retval = $Function.apply(this.handleRequest, this, args);
          } else {
            var optArgs = {
              customCallback: this.customCallback
            };
            retval = sendRequest(this.name, args,
                                 this.definition.parameters,
                                 optArgs);
          }
          sendRequestHandler.clearCalledSendRequest();

          // Validate return value if in sanity check mode.
          if (logging.DCHECK_IS_ON() && this.definition.returns)
            schemaUtils.validate([retval], [this.definition.returns]);
          return retval;
        }, apiFunction);
      }, this);
    }

    // Setup Events
    if (schema.events) {
      $Array.forEach(schema.events, function(eventDef) {
        if (eventDef.name in mod) {
          throw new Error('Event ' + eventDef.name +
                          ' already defined in ' + schema.namespace);
        }
        if (!isSchemaNodeSupported(eventDef, platform, manifestVersion))
          return;

        var eventName = schema.namespace + "." + eventDef.name;
        if (!GetAvailability(eventName).is_available ||
            (checkUnprivileged && !isSchemaAccessAllowed(eventDef))) {
          return;
        }

        var options = eventDef.options || {};
        if (eventDef.filters && eventDef.filters.length > 0)
          options.supportsFilters = true;

        var parameters = eventDef.parameters;
        if (this.customEvent_) {
          mod[eventDef.name] = new this.customEvent_(
              eventName, parameters, eventDef.extraParameters, options);
        } else {
          mod[eventDef.name] = new Event(eventName, parameters, options);
        }
      }, this);
    }

    function addProperties(m, parentDef) {
      var properties = parentDef.properties;
      if (!properties)
        return;

      forEach(properties, function(propertyName, propertyDef) {
        if (propertyName in m)
          return;  // TODO(kalman): be strict like functions/events somehow.
        if (!isSchemaNodeSupported(propertyDef, platform, manifestVersion))
          return;
        if (!GetAvailability(schema.namespace + "." +
              propertyName).is_available ||
            (checkUnprivileged && !isSchemaAccessAllowed(propertyDef))) {
          return;
        }

        // |value| is eventually added to |m|, the exposed API. Make copies
        // of everything from the schema. (The schema is also frozen, so as long
        // as we don't make any modifications, shallow copies are fine.)
        var value;
        if ($Array.isArray(propertyDef.value))
          value = $Array.slice(propertyDef.value);
        else if (typeof propertyDef.value === 'object')
          value = $Object.assign({}, propertyDef.value);
        else
          value = propertyDef.value;

        if (value) {
          // Values may just have raw types as defined in the JSON, such
          // as "WINDOW_ID_NONE": { "value": -1 }. We handle this here.
          // TODO(kalman): enforce that things with a "value" property can't
          // define their own types.
          var type = propertyDef.type || typeof(value);
          if (type === 'integer' || type === 'number') {
            value = parseInt(value);
          } else if (type === 'boolean') {
            value = value === 'true';
          } else if (propertyDef['$ref']) {
            var ref = propertyDef['$ref'];
            var type = utils.loadTypeSchema(propertyDef['$ref'], schema);
            logging.CHECK(type, 'Schema for $ref type ' + ref + ' not found');
            var constructor = createCustomType(type);
            var args = value;
            // For an object propertyDef, |value| is an array of constructor
            // arguments, but we want to pass the arguments directly (i.e.
            // not as an array), so we have to fake calling |new| on the
            // constructor.
            value = { __proto__: constructor.prototype };
            $Function.apply(constructor, value, args);
            // Recursively add properties.
            addProperties(value, propertyDef);
          } else if (type === 'object') {
            // Recursively add properties.
            addProperties(value, propertyDef);
          } else if (type !== 'string') {
            throw new Error('NOT IMPLEMENTED (extension_api.json error): ' +
                'Cannot parse values for type "' + type + '"');
          }
          m[propertyName] = value;
        }
      });
    };

    addProperties(mod, schema);

    // This generate() call is considered successful if any functions,
    // properties, or events were created.
    var success = ($Object.keys(mod).length > 0);

    // Special case: webViewRequest is a vacuous API which just copies its
    // implementation from declarativeWebRequest.
    //
    // TODO(kalman): This would be unnecessary if we did these checks after the
    // hooks (i.e. this.runHooks_(mod)). The reason we don't is to be very
    // conservative with running any JS which might actually be for an API
    // which isn't available, but this is probably overly cautious given the
    // C++ is only giving us APIs which are available. FIXME.
    if (schema.namespace == 'webViewRequest') {
      success = true;
    }

    // Special case: runtime.lastError is only occasionally set, so
    // specifically check its availability.
    if (schema.namespace == 'runtime' &&
        GetAvailability('runtime.lastError').is_available) {
      success = true;
    }

    if (!success) {
      var availability = GetAvailability(schema.namespace);
      // If an API was available it should have been successfully generated.
      logging.DCHECK(!availability.is_available,
                     schema.namespace + ' was available but not generated');
      console.error('chrome.' + schema.namespace + ' is not available: ' +
                    availability.message);
      return;
    }

    this.runHooks_(mod, schema);
    return mod;
  }
};

exports.$set('Binding', Binding);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the contextMenus API.

var binding = require('binding').Binding.create('contextMenus');
var contextMenusHandlers = require('contextMenusHandlers');

binding.registerCustomHook(function(bindingsAPI) {
  var apiFunctions = bindingsAPI.apiFunctions;

  var handlers = contextMenusHandlers.create(false /* isWebview */);

  apiFunctions.setHandleRequest('create', handlers.requestHandlers.create);

  apiFunctions.setCustomCallback('create', handlers.callbacks.create);

  apiFunctions.setCustomCallback('remove', handlers.callbacks.remove);

  apiFunctions.setCustomCallback('update', handlers.callbacks.update);

  apiFunctions.setCustomCallback('removeAll', handlers.callbacks.removeAll);
});

exports.$set('binding', binding.generate());
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Implementation of custom bindings for the contextMenus API.
// This is used to implement the contextMenus API for extensions and for the
// <webview> tag (see chrome_web_view_experimental.js).

var contextMenuNatives = requireNative('context_menus');
var sendRequest = require('sendRequest').sendRequest;
var Event = require('event_bindings').Event;
var lastError = require('lastError');

// Add the bindings to the contextMenus API.
function createContextMenusHandlers(isWebview) {
  var eventName = isWebview ? 'webViewInternal.contextMenus' : 'contextMenus';
  // Some dummy value for chrome.contextMenus instances.
  // Webviews use positive integers, and 0 to denote an invalid webview ID.
  // The following constant is -1 to avoid any conflicts between webview IDs and
  // extensions.
  var INSTANCEID_NON_WEBVIEW = -1;

  // Generates a customCallback for a given method. |handleCallback| will be
  // invoked with |request.args| as parameters.
  function createCustomCallback(handleCallback) {
    return function(name, request, callback) {
      if (lastError.hasError(chrome)) {
        if (callback)
          callback();
        return;
      }
      var args = request.args;
      if (!isWebview) {
        // <webview>s have an extra item in front of the parameter list, which
        // specifies the viewInstanceId of the webview. This is used to hide
        // context menu events in one webview from another.
        // The non-webview chrome.contextMenus API is not called with such an
        // ID, so we prepend an ID to match the function signature.
        args = $Array.concat([INSTANCEID_NON_WEBVIEW], args);
      }
      $Function.apply(handleCallback, null, args);
      if (callback)
        callback();
    };
  }

  var contextMenus = {};
  contextMenus.handlers = {};
  contextMenus.event = new Event(eventName);

  contextMenus.getIdFromCreateProperties = function(createProperties) {
    if (typeof createProperties.id !== 'undefined')
      return createProperties.id;
    return createProperties.generatedId;
  };

  contextMenus.handlersForId = function(instanceId, id) {
    if (!contextMenus.handlers[instanceId]) {
      contextMenus.handlers[instanceId] = {
        generated: {},
        string: {}
      };
    }
    if (typeof id === 'number')
      return contextMenus.handlers[instanceId].generated;
    return contextMenus.handlers[instanceId].string;
  };

  contextMenus.ensureListenerSetup = function() {
    if (contextMenus.listening) {
      return;
    }
    contextMenus.listening = true;
    contextMenus.event.addListener(function(info) {
      var instanceId = INSTANCEID_NON_WEBVIEW;
      if (isWebview) {
        instanceId = info.webviewInstanceId;
        // Don't expose |webviewInstanceId| via the public API.
        delete info.webviewInstanceId;
      }

      var id = info.menuItemId;
      var onclick = contextMenus.handlersForId(instanceId, id)[id];
      if (onclick) {
        $Function.apply(onclick, null, arguments);
      }
    });
  };

  // To be used with apiFunctions.setHandleRequest
  var requestHandlers = {};
  // To be used with apiFunctions.setCustomCallback
  var callbacks = {};

  requestHandlers.create = function() {
    var createProperties = isWebview ? arguments[1] : arguments[0];
    createProperties.generatedId = contextMenuNatives.GetNextContextMenuId();
    var optArgs = {
      customCallback: this.customCallback,
    };
    sendRequest(this.name, arguments, this.definition.parameters, optArgs);
    return contextMenus.getIdFromCreateProperties(createProperties);
  };

  callbacks.create =
      createCustomCallback(function(instanceId, createProperties) {
    var id = contextMenus.getIdFromCreateProperties(createProperties);
    var onclick = createProperties.onclick;
    if (onclick) {
      contextMenus.ensureListenerSetup();
      contextMenus.handlersForId(instanceId, id)[id] = onclick;
    }
  });

  callbacks.remove = createCustomCallback(function(instanceId, id) {
    delete contextMenus.handlersForId(instanceId, id)[id];
  });

  callbacks.update =
      createCustomCallback(function(instanceId, id, updateProperties) {
    var onclick = updateProperties.onclick;
    if (onclick) {
      contextMenus.ensureListenerSetup();
      contextMenus.handlersForId(instanceId, id)[id] = onclick;
    } else if (onclick === null) {
      // When onclick is explicitly set to null, remove the event listener.
      delete contextMenus.handlersForId(instanceId, id)[id];
    }
  });

  callbacks.removeAll = createCustomCallback(function(instanceId) {
    delete contextMenus.handlers[instanceId];
  });

  return {
    requestHandlers: requestHandlers,
    callbacks: callbacks
  };
}

exports.$set('create', createContextMenusHandlers);
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the declarativeWebRequest API.

var binding = require('binding').Binding.create('declarativeWebRequest');

var utils = require('utils');
var validate = require('schemaUtils').validate;

binding.registerCustomHook(function(api) {
  var declarativeWebRequest = api.compiledApi;

  // Returns the schema definition of type |typeId| defined in |namespace|.
  function getSchema(typeId) {
    return utils.lookup(api.schema.types,
                        'id',
                        'declarativeWebRequest.' + typeId);
  }

  // Helper function for the constructor of concrete datatypes of the
  // declarative webRequest API.
  // Makes sure that |this| contains the union of parameters and
  // {'instanceType': 'declarativeWebRequest.' + typeId} and validates the
  // generated union dictionary against the schema for |typeId|.
  function setupInstance(instance, parameters, typeId) {
    for (var key in parameters) {
      if ($Object.hasOwnProperty(parameters, key)) {
        instance[key] = parameters[key];
      }
    }
    instance.instanceType = 'declarativeWebRequest.' + typeId;
    var schema = getSchema(typeId);
    validate([instance], [schema]);
  }

  // Setup all data types for the declarative webRequest API.
  declarativeWebRequest.RequestMatcher = function(parameters) {
    setupInstance(this, parameters, 'RequestMatcher');
  };
  declarativeWebRequest.CancelRequest = function(parameters) {
    setupInstance(this, parameters, 'CancelRequest');
  };
  declarativeWebRequest.RedirectRequest = function(parameters) {
    setupInstance(this, parameters, 'RedirectRequest');
  };
  declarativeWebRequest.SetRequestHeader = function(parameters) {
    setupInstance(this, parameters, 'SetRequestHeader');
  };
  declarativeWebRequest.RemoveRequestHeader = function(parameters) {
    setupInstance(this, parameters, 'RemoveRequestHeader');
  };
  declarativeWebRequest.AddResponseHeader = function(parameters) {
    setupInstance(this, parameters, 'AddResponseHeader');
  };
  declarativeWebRequest.RemoveResponseHeader = function(parameters) {
    setupInstance(this, parameters, 'RemoveResponseHeader');
  };
  declarativeWebRequest.RedirectToTransparentImage =
      function(parameters) {
    setupInstance(this, parameters, 'RedirectToTransparentImage');
  };
  declarativeWebRequest.RedirectToEmptyDocument = function(parameters) {
    setupInstance(this, parameters, 'RedirectToEmptyDocument');
  };
  declarativeWebRequest.RedirectByRegEx = function(parameters) {
    setupInstance(this, parameters, 'RedirectByRegEx');
  };
  declarativeWebRequest.IgnoreRules = function(parameters) {
    setupInstance(this, parameters, 'IgnoreRules');
  };
  declarativeWebRequest.AddRequestCookie = function(parameters) {
    setupInstance(this, parameters, 'AddRequestCookie');
  };
  declarativeWebRequest.AddResponseCookie = function(parameters) {
    setupInstance(this, parameters, 'AddResponseCookie');
  };
  declarativeWebRequest.EditRequestCookie = function(parameters) {
    setupInstance(this, parameters, 'EditRequestCookie');
  };
  declarativeWebRequest.EditResponseCookie = function(parameters) {
    setupInstance(this, parameters, 'EditResponseCookie');
  };
  declarativeWebRequest.RemoveRequestCookie = function(parameters) {
    setupInstance(this, parameters, 'RemoveRequestCookie');
  };
  declarativeWebRequest.RemoveResponseCookie = function(parameters) {
    setupInstance(this, parameters, 'RemoveResponseCookie');
  };
  declarativeWebRequest.SendMessageToExtension = function(parameters) {
    setupInstance(this, parameters, 'SendMessageToExtension');
  };
});

exports.$set('binding', binding.generate());
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the Display Source API.

var binding = require('binding').Binding.create('displaySource');
var chrome = requireNative('chrome').GetChrome();
var lastError = require('lastError');
var natives = requireNative('display_source');
var logging = requireNative('logging');

var callbacksInfo = {};

function callbackWrapper(callback, method, message) {
  if (callback == undefined)
    return;

  try {
    if (message !== null)
      lastError.set(method, message, null, chrome);
    callback();
  } finally {
    lastError.clear(chrome);
  }
}

function callCompletionCallback(callbackId, error_message) {
  try {
    var callbackInfo = callbacksInfo[callbackId];
    logging.DCHECK(callbackInfo != null);
    callbackWrapper(callbackInfo.callback, callbackInfo.method, error_message);
  } finally {
    delete callbacksInfo[callbackId];
  }
}

binding.registerCustomHook(function(bindingsAPI, extensionId) {
  var apiFunctions = bindingsAPI.apiFunctions;
  apiFunctions.setHandleRequest(
      'startSession', function(sessionInfo, callback) {
        try {
          var callId = natives.StartSession(sessionInfo);
          callbacksInfo[callId] = {
            callback: callback,
            method: 'displaySource.startSession'
          };
        } catch (e) {
          callbackWrapper(callback, 'displaySource.startSession', e.message);
        }
      });
  apiFunctions.setHandleRequest(
      'terminateSession', function(sink_id, callback) {
        try {
          var callId = natives.TerminateSession(sink_id);
          callbacksInfo[callId] = {
            callback: callback,
            method: 'displaySource.terminateSession'
          };
        } catch (e) {
          callbackWrapper(
              callback, 'displaySource.terminateSession', e.message);
        }
      });
});

exports.$set('binding', binding.generate());
// Called by C++.
exports.$set('callCompletionCallback', callCompletionCallback);
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the extension API.

var binding = require('binding').Binding.create('extension');

var messaging = require('messaging');
var runtimeNatives = requireNative('runtime');
var GetExtensionViews = runtimeNatives.GetExtensionViews;
var chrome = requireNative('chrome').GetChrome();

var inIncognitoContext = requireNative('process').InIncognitoContext();
var sendRequestIsDisabled = requireNative('process').IsSendRequestDisabled();
var contextType = requireNative('process').GetContextType();
var manifestVersion = requireNative('process').GetManifestVersion();

// This should match chrome.windows.WINDOW_ID_NONE.
//
// We can't use chrome.windows.WINDOW_ID_NONE directly because the
// chrome.windows API won't exist unless this extension has permission for it;
// which may not be the case.
var WINDOW_ID_NONE = -1;

binding.registerCustomHook(function(bindingsAPI, extensionId) {
  var extension = bindingsAPI.compiledApi;
  if (manifestVersion < 2) {
    chrome.self = extension;
    extension.inIncognitoTab = inIncognitoContext;
  }
  extension.inIncognitoContext = inIncognitoContext;

  var apiFunctions = bindingsAPI.apiFunctions;

  apiFunctions.setHandleRequest('getViews', function(properties) {
    var windowId = WINDOW_ID_NONE;
    var type = 'ALL';
    if (properties) {
      if (properties.type != null) {
        type = properties.type;
      }
      if (properties.windowId != null) {
        windowId = properties.windowId;
      }
    }
    return GetExtensionViews(windowId, type);
  });

  apiFunctions.setHandleRequest('getBackgroundPage', function() {
    return GetExtensionViews(-1, 'BACKGROUND')[0] || null;
  });

  apiFunctions.setHandleRequest('getExtensionTabs', function(windowId) {
    if (windowId == null)
      windowId = WINDOW_ID_NONE;
    return GetExtensionViews(windowId, 'TAB');
  });

  apiFunctions.setHandleRequest('getURL', function(path) {
    path = String(path);
    if (!path.length || path[0] != '/')
      path = '/' + path;
    return 'chrome-extension://' + extensionId + path;
  });

  // Alias several messaging deprecated APIs to their runtime counterparts.
  var mayNeedAlias = [
    // Types
    'Port',
    // Functions
    'connect', 'sendMessage', 'connectNative', 'sendNativeMessage',
    // Events
    'onConnect', 'onConnectExternal', 'onMessage', 'onMessageExternal'
  ];
  $Array.forEach(mayNeedAlias, function(alias) {
    // Checking existence isn't enough since some functions are disabled via
    // getters that throw exceptions. Assume that any getter is such a function.
    if (chrome.runtime &&
        $Object.hasOwnProperty(chrome.runtime, alias) &&
        chrome.runtime.__lookupGetter__(alias) === undefined) {
      extension[alias] = chrome.runtime[alias];
    }
  });

  apiFunctions.setUpdateArgumentsPreValidate('sendRequest',
      $Function.bind(messaging.sendMessageUpdateArguments,
                     null, 'sendRequest', false /* hasOptionsArgument */));

  apiFunctions.setHandleRequest('sendRequest',
                                function(targetId, request, responseCallback) {
    if (sendRequestIsDisabled)
      throw new Error(sendRequestIsDisabled);
    var port = chrome.runtime.connect(targetId || extensionId,
                                      {name: messaging.kRequestChannel});
    messaging.sendMessageImpl(port, request, responseCallback);
  });

  if (sendRequestIsDisabled) {
    extension.onRequest.addListener = function() {
      throw new Error(sendRequestIsDisabled);
    };
    if (contextType == 'BLESSED_EXTENSION') {
      extension.onRequestExternal.addListener = function() {
        throw new Error(sendRequestIsDisabled);
      };
    }
  }
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// -----------------------------------------------------------------------------
// NOTE: If you change this file you need to touch renderer_resources.grd to
// have your change take effect.
// -----------------------------------------------------------------------------

// Partial implementation of the Greasemonkey API, see:
// http://wiki.greasespot.net/Greasemonkey_Manual:APIs

function GM_addStyle(css) {
  var parent = document.getElementsByTagName("head")[0];
  if (!parent) {
    parent = document.documentElement;
  }
  var style = document.createElement("style");
  style.type = "text/css";
  var textNode = document.createTextNode(css);
  style.appendChild(textNode);
  parent.appendChild(style);
}

function GM_xmlhttpRequest(details) {
  function setupEvent(xhr, url, eventName, callback) {
    xhr[eventName] = function () {
      var isComplete = xhr.readyState == 4;
      var responseState = {
        responseText: xhr.responseText,
        readyState: xhr.readyState,
        responseHeaders: isComplete ? xhr.getAllResponseHeaders() : "",
        status: isComplete ? xhr.status : 0,
        statusText: isComplete ? xhr.statusText : "",
        finalUrl: isComplete ? url : ""
      };
      callback(responseState);
    };
  }

  var xhr = new XMLHttpRequest();
  var eventNames = ["onload", "onerror", "onreadystatechange"];
  for (var i = 0; i < eventNames.length; i++ ) {
    var eventName = eventNames[i];
    if (eventName in details) {
      setupEvent(xhr, details.url, eventName, details[eventName]);
    }
  }

  xhr.open(details.method, details.url);

  if (details.overrideMimeType) {
    xhr.overrideMimeType(details.overrideMimeType);
  }
  if (details.headers) {
    for (var header in details.headers) {
      xhr.setRequestHeader(header, details.headers[header]);
    }
  }
  xhr.send(details.data ? details.data : null);
}

function GM_openInTab(url) {
  window.open(url, "");
}

function GM_log(message) {
  window.console.log(message);
}

(function() {
  function generateGreasemonkeyStub(name) {
    return function() {
      console.log("%s is not supported.", name);
    };
  }

  var apis = ["GM_getValue", "GM_setValue", "GM_registerMenuCommand"];
  for (var i = 0, api; api = apis[i]; i++) {
    window[api] = generateGreasemonkeyStub(api);
  }
})();
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the i18n API.

var binding = require('binding').Binding.create('i18n');

var i18nNatives = requireNative('i18n');
var GetL10nMessage = i18nNatives.GetL10nMessage;
var GetL10nUILanguage = i18nNatives.GetL10nUILanguage;
var DetectTextLanguage = i18nNatives.DetectTextLanguage;

binding.registerCustomHook(function(bindingsAPI, extensionId) {
  var apiFunctions = bindingsAPI.apiFunctions;

  apiFunctions.setUpdateArgumentsPreValidate('getMessage', function() {
    var args = $Array.slice(arguments);

    // The first argument is the message, and should be a string.
    var message = args[0];
    if (typeof(message) !== 'string') {
      console.warn(extensionId + ': the first argument to getMessage should ' +
                   'be type "string", was ' + message +
                   ' (type "' + typeof(message) + '")');
      args[0] = String(message);
    }

    return args;
  });

  apiFunctions.setHandleRequest('getMessage',
                                function(messageName, substitutions) {
    return GetL10nMessage(messageName, substitutions, extensionId);
  });

  apiFunctions.setHandleRequest('getUILanguage', function() {
    return GetL10nUILanguage();
  });

  apiFunctions.setHandleRequest('detectLanguage', function(text, callback) {
    window.setTimeout(function() {
      var response = DetectTextLanguage(text);
      callback(response);
    }, 0);
  });
});

exports.$set('binding', binding.generate());
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Custom bindings for the mojoPrivate API.
 */

let binding = require('binding').Binding.create('mojoPrivate');

binding.registerCustomHook(function(bindingsAPI) {
  let apiFunctions = bindingsAPI.apiFunctions;

  apiFunctions.setHandleRequest('define', function(name, deps, factory) {
    define(name, deps || [], factory);
  });

  apiFunctions.setHandleRequest('requireAsync', function(moduleName) {
    return requireAsync(moduleName);
  });
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the Permissions API.

var binding = require('binding').Binding.create('permissions');

var Event = require('event_bindings').Event;

// These custom binding are only necessary because it is not currently
// possible to have a union of types as the type of the items in an array.
// Once that is fixed, this entire file should go away.
// See,
// https://code.google.com/p/chromium/issues/detail?id=162044
// https://code.google.com/p/chromium/issues/detail?id=162042
// TODO(bryeung): delete this file.
binding.registerCustomHook(function(api) {
  var apiFunctions = api.apiFunctions;
  var permissions = api.compiledApi;

  function maybeConvertToObject(str) {
    var parts = $String.split(str, '|');
    if (parts.length != 2)
      return str;

    var ret = {};
    ret[parts[0]] = JSON.parse(parts[1]);
    return ret;
  }

  function convertObjectPermissionsToStrings() {
    if (arguments.length < 1)
      return arguments;

    var args = arguments[0].permissions;
    if (!args)
      return arguments;

    for (var i = 0; i < args.length; i += 1) {
      if (typeof(args[i]) == 'object') {
        var a = args[i];
        var keys = $Object.keys(a);
        if (keys.length != 1) {
          throw new Error("Too many keys in object-style permission.");
        }
        arguments[0].permissions[i] = keys[0] + '|' +
            JSON.stringify(a[keys[0]]);
      }
    }

    return arguments;
  }

  // Convert complex permissions to strings so they validate against the schema
  apiFunctions.setUpdateArgumentsPreValidate(
      'contains', convertObjectPermissionsToStrings);
  apiFunctions.setUpdateArgumentsPreValidate(
      'remove', convertObjectPermissionsToStrings);
  apiFunctions.setUpdateArgumentsPreValidate(
      'request', convertObjectPermissionsToStrings);

  // Convert complex permissions back to objects
  apiFunctions.setCustomCallback('getAll',
      function(name, request, callback, response) {
        for (var i = 0; i < response.permissions.length; i += 1) {
          response.permissions[i] =
              maybeConvertToObject(response.permissions[i]);
        }

        // Since the schema says Permissions.permissions contains strings and
        // not objects, validation will fail after the for-loop above.  This
        // skips validation and calls the callback directly.
        if (callback)
          callback(response);
      });

  // Also convert complex permissions back to objects for events.  The
  // dispatchToListener call happens after argument validation, which works
  // around the problem that Permissions.permissions is supposed to be a list
  // of strings.
  permissions.onAdded.dispatchToListener = function(callback, args) {
    for (var i = 0; i < args[0].permissions.length; i += 1) {
      args[0].permissions[i] = maybeConvertToObject(args[0].permissions[i]);
    }
    $Function.call(Event.prototype.dispatchToListener, this, callback, args);
  };
  permissions.onRemoved.dispatchToListener =
      permissions.onAdded.dispatchToListener;
});

exports.$set('binding', binding.generate());
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var binding = require('binding').Binding.create('printerProvider');
var printerProviderInternal = require('binding').Binding.create(
    'printerProviderInternal').generate();
var eventBindings = require('event_bindings');
var blobNatives = requireNative('blob_natives');

var printerProviderSchema =
    requireNative('schema_registry').GetSchema('printerProvider')
var utils = require('utils');
var validate = require('schemaUtils').validate;

// Custom bindings for chrome.printerProvider API.
// The bindings are used to implement callbacks for the API events. Internally
// each event is passed requestId argument used to identify the callback
// associated with the event. This argument is massaged out from the event
// arguments before dispatching the event to consumers. A callback is appended
// to the event arguments. The callback wraps an appropriate
// chrome.printerProviderInternal API function that is used to report the event
// result from the extension. The function is passed requestId and values
// provided by the extension. It validates that the values provided by the
// extension match chrome.printerProvider event callback schemas. It also
// ensures that a callback is run at most once. In case there is an exception
// during event dispatching, the chrome.printerProviderInternal function
// is called with a default error value.
//

// Handles a chrome.printerProvider event as described in the file comment.
// |eventName|: The event name.
// |prepareArgsForDispatch|: Function called before dispatching the event to
//     the extension. It's called with original event |args| list and callback
//     that should be called when the |args| are ready for dispatch. The
//     callbacks should report whether the argument preparation was successful.
//     The function should not change the first argument, which contains the
//     request id.
// |resultreporter|: The function that should be called to report event result.
//     One of chrome.printerProviderInternal API functions.
function handleEvent(eventName, prepareArgsForDispatch, resultReporter) {
  eventBindings.registerArgumentMassager(
      'printerProvider.' + eventName,
      function(args, dispatch) {
        var responded = false;

        // Validates that the result passed by the extension to the event
        // callback matches the callback schema. Throws an exception in case of
        // an error.
        var validateResult = function(result) {
          var eventSchema =
              utils.lookup(printerProviderSchema.events, 'name', eventName);
          var callbackSchema =
              utils.lookup(eventSchema.parameters, 'type', 'function');

          validate([result], callbackSchema.parameters);
        };

        // Function provided to the extension as the event callback argument.
        // It makes sure that the event result hasn't previously been returned
        // and that the provided result matches the callback schema. In case of
        // an error it throws an exception.
        var reportResult = function(result) {
          if (responded) {
            throw new Error(
                'Event callback must not be called more than once.');
          }

          var finalResult = null;
          try {
            validateResult(result);  // throws on failure
            finalResult = result;
          } finally {
            responded = true;
            resultReporter(args[0] /* requestId */, finalResult);
          }
        };

        prepareArgsForDispatch(args, function(success) {
          if (!success) {
            // Do not throw an exception since the extension should not yet be
            // aware of the event.
            resultReporter(args[0] /* requestId */, null);
            return;
          }
          dispatch(args.slice(1).concat(reportResult));
        });
      });
}

// Sets up printJob.document property for a print request.
function createPrintRequestBlobArguments(args, callback) {
  printerProviderInternal.getPrintData(args[0] /* requestId */,
                                       function(blobInfo) {
    if (chrome.runtime.lastError) {
      callback(false);
      return;
    }

    // |args[1]| is printJob.
    args[1].document = blobNatives.TakeBrowserProcessBlob(
        blobInfo.blobUuid, blobInfo.type, blobInfo.size);
    callback(true);
  });
}

handleEvent('onGetPrintersRequested',
            function(args, callback) { callback(true); },
            printerProviderInternal.reportPrinters);

handleEvent('onGetCapabilityRequested',
            function(args, callback) { callback(true); },
            printerProviderInternal.reportPrinterCapability);

handleEvent('onPrintRequested',
            createPrintRequestBlobArguments,
            printerProviderInternal.reportPrintResult);

handleEvent('onGetUsbPrinterInfoRequested',
            function(args, callback) { callback(true); },
            printerProviderInternal.reportUsbPrinterInfo);

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the runtime API.

var binding = require('binding').Binding.create('runtime');

var messaging = require('messaging');
var runtimeNatives = requireNative('runtime');
var process = requireNative('process');
var forEach = require('utils').forEach;

var backgroundPage = window;
var backgroundRequire = require;
var contextType = process.GetContextType();
if (contextType == 'BLESSED_EXTENSION' ||
    contextType == 'UNBLESSED_EXTENSION') {
  var manifest = runtimeNatives.GetManifest();
  if (manifest.app && manifest.app.background) {
    // Get the background page if one exists. Otherwise, default to the current
    // window.
    backgroundPage = runtimeNatives.GetExtensionViews(-1, 'BACKGROUND')[0];
    if (backgroundPage) {
      var GetModuleSystem = requireNative('v8_context').GetModuleSystem;
      backgroundRequire = GetModuleSystem(backgroundPage).require;
    } else {
      backgroundPage = window;
    }
  }
}

// For packaged apps, all windows use the bindFileEntryCallback from the
// background page so their FileEntry objects have the background page's context
// as their own.  This allows them to be used from other windows (including the
// background page) after the original window is closed.
if (window == backgroundPage) {
  var lastError = require('lastError');
  var fileSystemNatives = requireNative('file_system_natives');
  var GetIsolatedFileSystem = fileSystemNatives.GetIsolatedFileSystem;
  var bindDirectoryEntryCallback = function(functionName, apiFunctions) {
    apiFunctions.setCustomCallback(functionName,
        function(name, request, callback, response) {
      if (callback) {
        if (!response) {
          callback();
          return;
        }
        var fileSystemId = response.fileSystemId;
        var baseName = response.baseName;
        var fs = GetIsolatedFileSystem(fileSystemId);

        try {
          fs.root.getDirectory(baseName, {}, callback, function(fileError) {
            lastError.run('runtime.' + functionName,
                          'Error getting Entry, code: ' + fileError.code,
                          request.stack,
                          callback);
          });
        } catch (e) {
          lastError.run('runtime.' + functionName,
                        'Error: ' + e.stack,
                        request.stack,
                        callback);
        }
      }
    });
  };
} else {
  // Force the runtime API to be loaded in the background page. Using
  // backgroundPageModuleSystem.require('runtime') is insufficient as
  // requireNative is only allowed while lazily loading an API.
  backgroundPage.chrome.runtime;
  var bindDirectoryEntryCallback = backgroundRequire(
      'runtime').bindDirectoryEntryCallback;
}

binding.registerCustomHook(function(binding, id, contextType) {
  var apiFunctions = binding.apiFunctions;
  var runtime = binding.compiledApi;

  //
  // Unprivileged APIs.
  //

  if (id != '')
    runtime.id = id;

  apiFunctions.setHandleRequest('getManifest', function() {
    return runtimeNatives.GetManifest();
  });

  apiFunctions.setHandleRequest('getURL', function(path) {
    path = String(path);
    if (!path.length || path[0] != '/')
      path = '/' + path;
    return 'chrome-extension://' + id + path;
  });

  var sendMessageUpdateArguments = messaging.sendMessageUpdateArguments;
  apiFunctions.setUpdateArgumentsPreValidate('sendMessage',
      $Function.bind(sendMessageUpdateArguments, null, 'sendMessage',
                     true /* hasOptionsArgument */));
  apiFunctions.setUpdateArgumentsPreValidate('sendNativeMessage',
      $Function.bind(sendMessageUpdateArguments, null, 'sendNativeMessage',
                     false /* hasOptionsArgument */));

  apiFunctions.setHandleRequest('sendMessage',
      function(targetId, message, options, responseCallback) {
    var connectOptions = {name: messaging.kMessageChannel};
    forEach(options, function(k, v) {
      connectOptions[k] = v;
    });
    var port = runtime.connect(targetId || runtime.id, connectOptions);
    messaging.sendMessageImpl(port, message, responseCallback);
  });

  apiFunctions.setHandleRequest('sendNativeMessage',
                                function(targetId, message, responseCallback) {
    var port = runtime.connectNative(targetId);
    messaging.sendMessageImpl(port, message, responseCallback);
  });

  apiFunctions.setUpdateArgumentsPreValidate('connect', function() {
    // Align missing (optional) function arguments with the arguments that
    // schema validation is expecting, e.g.
    //   runtime.connect()   -> runtime.connect(null, null)
    //   runtime.connect({}) -> runtime.connect(null, {})
    var nextArg = 0;

    // targetId (first argument) is optional.
    var targetId = null;
    if (typeof(arguments[nextArg]) == 'string')
      targetId = arguments[nextArg++];

    // connectInfo (second argument) is optional.
    var connectInfo = null;
    if (typeof(arguments[nextArg]) == 'object')
      connectInfo = arguments[nextArg++];

    if (nextArg != arguments.length)
      throw new Error('Invalid arguments to connect.');
    return [targetId, connectInfo];
  });

  apiFunctions.setUpdateArgumentsPreValidate('connectNative',
                                             function(appName) {
    if (typeof(appName) !== 'string') {
      throw new Error('Invalid arguments to connectNative.');
    }
    return [appName];
  });

  apiFunctions.setHandleRequest('connect', function(targetId, connectInfo) {
    if (!targetId) {
      // runtime.id is only defined inside extensions. If we're in a webpage,
      // the best we can do at this point is to fail.
      if (!runtime.id) {
        throw new Error('chrome.runtime.connect() called from a webpage must ' +
                        'specify an Extension ID (string) for its first ' +
                        'argument');
      }
      targetId = runtime.id;
    }

    var name = '';
    if (connectInfo && connectInfo.name)
      name = connectInfo.name;

    var includeTlsChannelId =
      !!(connectInfo && connectInfo.includeTlsChannelId);

    var portId = runtimeNatives.OpenChannelToExtension(targetId, name,
                                                       includeTlsChannelId);
    if (portId >= 0)
      return messaging.createPort(portId, name);
  });

  //
  // Privileged APIs.
  //
  if (contextType != 'BLESSED_EXTENSION')
    return;

  apiFunctions.setHandleRequest('connectNative',
                                function(nativeAppName) {
    var portId = runtimeNatives.OpenChannelToNativeApp(runtime.id,
                                                       nativeAppName);
    if (portId >= 0)
      return messaging.createPort(portId, '');
    throw new Error('Error connecting to native app: ' + nativeAppName);
  });

  apiFunctions.setCustomCallback('getBackgroundPage',
                                 function(name, request, callback, response) {
    if (callback) {
      var bg = runtimeNatives.GetExtensionViews(-1, 'BACKGROUND')[0] || null;
      callback(bg);
    }
  });

  bindDirectoryEntryCallback('getPackageDirectoryEntry', apiFunctions);
});

exports.$set('bindDirectoryEntryCallback', bindDirectoryEntryCallback);
exports.$set('binding', binding.generate());
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This function is returned to DidInitializeServiceWorkerContextOnWorkerThread
// then executed, passing in dependencies as function arguments.
//
// |backgroundUrl| is the URL of the extension's background page.
// |wakeEventPage| is a function that wakes up the current extension's event
// page, then runs its callback on completion or failure.
// |logging| is an object equivalent to a subset of base/debug/logging.h, with
// CHECK/DCHECK/etc.
(function(backgroundUrl, wakeEventPage, logging) {
  'use strict';
  self.chrome = self.chrome || {};
  self.chrome.runtime = self.chrome.runtime || {};

  // Returns a Promise that resolves to the background page's client, or null
  // if there is no background client.
  function findBackgroundClient() {
    return self.clients.matchAll({
      includeUncontrolled: true,
      type: 'window'
    }).then(function(clients) {
      return clients.find(function(client) {
        return client.url == backgroundUrl;
      });
    });
  }

  // Returns a Promise wrapper around wakeEventPage, that resolves on success,
  // or rejects on failure.
  function makeWakeEventPagePromise() {
    return new Promise(function(resolve, reject) {
      wakeEventPage(function(success) {
        if (success)
          resolve();
        else
          reject('Failed to start background client "' + backgroundUrl + '"');
      });
    });
  }

  // The chrome.runtime.getBackgroundClient function is documented in
  // runtime.json. It returns a Promise that resolves to the background page's
  // client, or is rejected if there is no background client or if the
  // background client failed to wake.
  self.chrome.runtime.getBackgroundClient = function() {
    return findBackgroundClient().then(function(client) {
      if (client) {
        // Background client is already awake, or it was persistent.
        return client;
      }

      // Event page needs to be woken.
      return makeWakeEventPagePromise().then(function() {
        return findBackgroundClient();
      }).then(function(client) {
        if (!client) {
          return Promise.reject(
            'Background client "' + backgroundUrl + '" not found');
        }
        return client;
      });
    });
  };
});
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the webRequest API.

var binding = require('binding').Binding.create('webRequest');
var sendRequest = require('sendRequest').sendRequest;
var WebRequestEvent = require('webRequestInternal').WebRequestEvent;

binding.registerCustomHook(function(api) {
  var apiFunctions = api.apiFunctions;

  apiFunctions.setHandleRequest('handlerBehaviorChanged', function() {
    var args = $Array.slice(arguments);
    sendRequest(this.name, args, this.definition.parameters,
                {forIOThread: true});
  });
});

binding.registerCustomEvent(WebRequestEvent);

exports.$set('binding', binding.generate());
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the webRequestInternal API.

var binding = require('binding').Binding.create('webRequestInternal');
var eventBindings = require('event_bindings');
var sendRequest = require('sendRequest').sendRequest;
var validate = require('schemaUtils').validate;
var utils = require('utils');
var idGeneratorNatives = requireNative('id_generator');

var webRequestInternal;

function GetUniqueSubEventName(eventName) {
  return eventName + '/' + idGeneratorNatives.GetNextId();
}

// WebRequestEventImpl object. This is used for special webRequest events
// with extra parameters. Each invocation of addListener creates a new named
// sub-event. That sub-event is associated with the extra parameters in the
// browser process, so that only it is dispatched when the main event occurs
// matching the extra parameters.
//
// Example:
//   chrome.webRequest.onBeforeRequest.addListener(
//       callback, {urls: 'http://*.google.com/*'});
//   ^ callback will only be called for onBeforeRequests matching the filter.
function WebRequestEventImpl(eventName, opt_argSchemas, opt_extraArgSchemas,
                             opt_eventOptions, opt_webViewInstanceId) {
  if (typeof eventName != 'string')
    throw new Error('chrome.WebRequestEvent requires an event name.');

  this.eventName = eventName;
  this.argSchemas = opt_argSchemas;
  this.extraArgSchemas = opt_extraArgSchemas;
  this.webViewInstanceId = opt_webViewInstanceId || 0;
  this.subEvents = [];
  this.eventOptions = eventBindings.parseEventOptions(opt_eventOptions);
  if (this.eventOptions.supportsRules) {
    this.eventForRules =
        new eventBindings.Event(eventName, opt_argSchemas, opt_eventOptions,
                                opt_webViewInstanceId);
  }
}
$Object.setPrototypeOf(WebRequestEventImpl.prototype, null);

// Test if the given callback is registered for this event.
WebRequestEventImpl.prototype.hasListener = function(cb) {
  if (!this.eventOptions.supportsListeners)
    throw new Error('This event does not support listeners.');
  return this.findListener_(cb) > -1;
};

// Test if any callbacks are registered fur thus event.
WebRequestEventImpl.prototype.hasListeners = function() {
  if (!this.eventOptions.supportsListeners)
    throw new Error('This event does not support listeners.');
  return this.subEvents.length > 0;
};

// Registers a callback to be called when this event is dispatched. If
// opt_filter is specified, then the callback is only called for events that
// match the given filters. If opt_extraInfo is specified, the given optional
// info is sent to the callback.
WebRequestEventImpl.prototype.addListener =
    function(cb, opt_filter, opt_extraInfo) {
  if (!this.eventOptions.supportsListeners)
    throw new Error('This event does not support listeners.');
  // NOTE(benjhayden) New APIs should not use this subEventName trick! It does
  // not play well with event pages. See downloads.onDeterminingFilename and
  // ExtensionDownloadsEventRouter for an alternative approach.
  var subEventName = GetUniqueSubEventName(this.eventName);
  // Note: this could fail to validate, in which case we would not add the
  // subEvent listener.
  validate($Array.slice(arguments, 1), this.extraArgSchemas);
  webRequestInternal.addEventListener(
      cb, opt_filter, opt_extraInfo, this.eventName, subEventName,
      this.webViewInstanceId);

  var subEvent = new eventBindings.Event(subEventName, this.argSchemas);
  var subEventCallback = cb;
  if (opt_extraInfo && opt_extraInfo.indexOf('blocking') >= 0) {
    var eventName = this.eventName;
    subEventCallback = function() {
      var requestId = arguments[0].requestId;
      try {
        var result = $Function.apply(cb, null, arguments);
        webRequestInternal.eventHandled(
            eventName, subEventName, requestId, result);
      } catch (e) {
        webRequestInternal.eventHandled(
            eventName, subEventName, requestId);
        throw e;
      }
    };
  } else if (opt_extraInfo && opt_extraInfo.indexOf('asyncBlocking') >= 0) {
    var eventName = this.eventName;
    subEventCallback = function() {
      var details = arguments[0];
      var requestId = details.requestId;
      var handledCallback = function(response) {
        webRequestInternal.eventHandled(
            eventName, subEventName, requestId, response);
      };
      $Function.apply(cb, null, [details, handledCallback]);
    };
  }
  $Array.push(this.subEvents,
      {subEvent: subEvent, callback: cb, subEventCallback: subEventCallback});
  subEvent.addListener(subEventCallback);
};

// Unregisters a callback.
WebRequestEventImpl.prototype.removeListener = function(cb) {
  if (!this.eventOptions.supportsListeners)
    throw new Error('This event does not support listeners.');
  var idx;
  while ((idx = this.findListener_(cb)) >= 0) {
    var e = this.subEvents[idx];
    e.subEvent.removeListener(e.subEventCallback);
    if (e.subEvent.hasListeners()) {
      console.error(
          'Internal error: webRequest subEvent has orphaned listeners.');
    }
    $Array.splice(this.subEvents, idx, 1);
  }
};

WebRequestEventImpl.prototype.findListener_ = function(cb) {
  for (var i in this.subEvents) {
    var e = this.subEvents[i];
    if (e.callback === cb) {
      if (e.subEvent.hasListener(e.subEventCallback))
        return i;
      console.error('Internal error: webRequest subEvent has no callback.');
    }
  }

  return -1;
};

WebRequestEventImpl.prototype.addRules = function(rules, opt_cb) {
  if (!this.eventOptions.supportsRules)
    throw new Error('This event does not support rules.');
  this.eventForRules.addRules(rules, opt_cb);
};

WebRequestEventImpl.prototype.removeRules =
    function(ruleIdentifiers, opt_cb) {
  if (!this.eventOptions.supportsRules)
    throw new Error('This event does not support rules.');
  this.eventForRules.removeRules(ruleIdentifiers, opt_cb);
};

WebRequestEventImpl.prototype.getRules = function(ruleIdentifiers, cb) {
  if (!this.eventOptions.supportsRules)
    throw new Error('This event does not support rules.');
  this.eventForRules.getRules(ruleIdentifiers, cb);
};

binding.registerCustomHook(function(api) {
  var apiFunctions = api.apiFunctions;

  apiFunctions.setHandleRequest('addEventListener', function() {
    var args = $Array.slice(arguments);
    sendRequest(this.name, args, this.definition.parameters,
                {forIOThread: true});
  });

  apiFunctions.setHandleRequest('eventHandled', function() {
    var args = $Array.slice(arguments);
    sendRequest(this.name, args, this.definition.parameters,
                {forIOThread: true});
  });
});

function WebRequestEvent() {
  privates(WebRequestEvent).constructPrivate(this, arguments);
}
utils.expose(WebRequestEvent, WebRequestEventImpl, {
  functions: [
    'hasListener',
    'hasListeners',
    'addListener',
    'removeListener',
    'addRules',
    'removeRules',
    'getRules',
  ],
});

webRequestInternal = binding.generate();
exports.$set('binding', webRequestInternal);
exports.$set('WebRequestEvent', WebRequestEvent);
// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

//
// <window-controls> shadow element implementation.
//

var chrome = requireNative('chrome').GetChrome();
var forEach = require('utils').forEach;
var addTagWatcher = require('tagWatcher').addTagWatcher;
var appWindow = require('app.window');
var getHtmlTemplate =
  requireNative('app_window_natives').GetWindowControlsHtmlTemplate;

/**
 * @constructor
 */
function WindowControls(node) {
  this.node_ = node;
  this.shadowRoot_ = this.createShadowRoot_(node);
  this.setupWindowControls_();
}

/**
 * @private
 */
WindowControls.prototype.template_element = null;

/**
 * @private
 */
WindowControls.prototype.createShadowRoot_ = function(node) {
  // Initialize |template| from HTML template resource and cache result.
  var template = WindowControls.prototype.template_element;
  if (!template) {
    var element = document.createElement('div');
    element.innerHTML = getHtmlTemplate();
    WindowControls.prototype.template_element = element.firstChild;
    template = WindowControls.prototype.template_element;
  }
  // Create shadow root element with template clone as first child.
  var shadowRoot = node.createShadowRoot();
  shadowRoot.appendChild(template.content.cloneNode(true));
  return shadowRoot;
}

/**
 * @private
 */
WindowControls.prototype.setupWindowControls_ = function() {
  var self = this;
  this.shadowRoot_.querySelector("#close-control").addEventListener('click',
      function(e) {
        chrome.app.window.current().close();
      });

  this.shadowRoot_.querySelector("#maximize-control").addEventListener('click',
      function(e) {
        self.maxRestore_();
      });
}

/**
 * @private
 * Restore or maximize depending on current state
 */
WindowControls.prototype.maxRestore_ = function() {
  if (chrome.app.window.current().isMaximized()) {
    chrome.app.window.current().restore();
  } else {
    chrome.app.window.current().maximize();
  }
}

addTagWatcher('WINDOW-CONTROLS', function(addedNode) {
  new WindowControls(addedNode);
});
<template id="window-controls-template">
  <style>
    .controls {
      width:32px;
      height:32px;
      position:absolute;
      z-index:200;
    }
    #close-control {
      top:8px;
      right:10px;
    }
    #maximize-control {
      top:8px;
      right:52px;
    }
    #close {
      top:0;
      right:0;
      -webkit-mask-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAALNJREFUeNrsllEOwyAIhmVXwIu487dXKEdy2PBgWuxwm/VhkPwvSuALAhFyzmGmPcJkcwAHcAAHMAMAQGItLGSFhlB8kpmgrGKL2NbiziIWKqHK2SY+qzluBwBKcg2iTr7fjQBoQZySd1W2E0CD2LSqjAQ4Qqh9YY37zRj+5iPx4RPUZac7n6DVhHRHE74bQxo9hvUiio1FRCMXURKIeNFSKD5Pa1zwX7EDOIAD/D3AS4ABAKWdkCCeGGsrAAAAAElFTkSuQmCC');
    }
    .windowbutton {
      width:32px;
      height:32px;
      position:absolute;
      background-color:rgba(0, 0, 0, 0.49);
    }
    .windowbuttonbackground {
      width:32px;
      height:32px;
      position:absolute;
    }
    .windowbuttonbackground:hover {
      background-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAEBJREFUeNrszrENAEAIw8A8EvU32X9RGsQUaewFfM/2l9TKNBWcX10KBwAAAAAAAAAAAAAAAAAAABxggv9ZAQYAhakDi3I15kgAAAAASUVORK5CYII=');

    }
    .windowbuttonbackground:active {
      background-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAFtJREFUeNrs1zESgCAMRNEAkSZD5VW8/1k8hFaCCqfY5u9M6v/apIh2mH1hkqXba92uUvxU5Mfoe57xx0Rb7WziAQAAAAAAAAAAAAAAAAAAOcDXkzqvifrvL8AAWBcLpapo5CcAAAAASUVORK5CYII=');
    }
    #maximize {
      -webkit-mask-image:url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAACAAAAAgCAYAAABzenr0AAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAAFFJREFUeNrs1jsOACAIREHWeP8royWdnwaDj9pi2OhGubtlTrPkAQAAAIC+e1DSUWXOhlWtBGIYq+W5hAAA1GzC23f+fALiVwwAAIDvAUOAAQAv/Aw+jTHzugAAAABJRU5ErkJggg==');
    }
  </style>
  <div id="close-control" class="controls">
    <div id="close" class="windowbutton"> </div>
    <div id="close-background" class="windowbuttonbackground"> </div>
  </div>
  <div id="maximize-control" class="controls">
    <div id="maximize" class="windowbutton"></div>
    <div id="maximize-background" class="windowbuttonbackground"></div>
  </div>
</template>
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Custom binding for the webViewRequest API.

var binding = require('binding').Binding.create('webViewRequest');

var declarativeWebRequestSchema =
    requireNative('schema_registry').GetSchema('declarativeWebRequest');
var utils = require('utils');
var validate = require('schemaUtils').validate;

binding.registerCustomHook(function(api) {
  var webViewRequest = api.compiledApi;

  // Returns the schema definition of type |typeId| defined in
  // |declarativeWebRequestScheme.types|.
  function getSchema(typeId) {
    return utils.lookup(declarativeWebRequestSchema.types,
                        'id',
                        'declarativeWebRequest.' + typeId);
  }

  // Helper function for the constructor of concrete datatypes of the
  // declarative webRequest API.
  // Makes sure that |this| contains the union of parameters and
  // {'instanceType': 'declarativeWebRequest.' + typeId} and validates the
  // generated union dictionary against the schema for |typeId|.
  function setupInstance(instance, parameters, typeId) {
    for (var key in parameters) {
      if ($Object.hasOwnProperty(parameters, key)) {
        instance[key] = parameters[key];
      }
    }

    instance.instanceType = 'declarativeWebRequest.' + typeId;
    var schema = getSchema(typeId);
    validate([instance], [schema]);
  }

  // Setup all data types for the declarative webRequest API from the schema.
  for (var i = 0; i < declarativeWebRequestSchema.types.length; ++i) {
    var typeSchema = declarativeWebRequestSchema.types[i];
    var typeId = typeSchema.id.replace('declarativeWebRequest.', '');
    var action = function(typeId) {
      return function(parameters) {
        setupInstance(this, parameters, typeId);
      };
    }(typeId);
    webViewRequest[typeId] = action;
  }
});

exports.$set('binding', binding.generate());
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var normalizeArgumentsAndValidate =
    require('schemaUtils').normalizeArgumentsAndValidate
var sendRequest = require('sendRequest').sendRequest;

function extendSchema(schema) {
  var extendedSchema = $Array.slice(schema);
  $Array.unshift(extendedSchema, {'type': 'string'});
  return extendedSchema;
}

function StorageArea(namespace, schema) {
  // Binds an API function for a namespace to its browser-side call, e.g.
  // storage.sync.get('foo') -> (binds to) ->
  // storage.get('sync', 'foo').
  //
  // TODO(kalman): Put as a method on CustombindingObject and re-use (or
  // even generate) for other APIs that need to do this. Same for other
  // callers of registerCustomType().
  var self = this;
  function bindApiFunction(functionName) {
    self[functionName] = function() {
      var funSchema = this.functionSchemas[functionName];
      var args = $Array.slice(arguments);
      args = normalizeArgumentsAndValidate(args, funSchema);
      return sendRequest(
          'storage.' + functionName,
          $Array.concat([namespace], args),
          extendSchema(funSchema.definition.parameters),
          {preserveNullInObjects: true});
    };
  }
  var apiFunctions = ['get', 'set', 'remove', 'clear', 'getBytesInUse'];
  $Array.forEach(apiFunctions, bindApiFunction);
}

exports.$set('StorageArea', StorageArea);
/*
 * Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * A style sheet for Chrome apps.
 */

@namespace "http://www.w3.org/1999/xhtml";

body {
  -webkit-user-select: none;
  cursor: default;
  font-family: $FONTFAMILY;
  font-size: $FONTSIZE;
}

webview, appview {
  display: inline-block;
  width: 300px;
  height: 300px;
}

html, body {
  overflow: hidden;
}

img, a {
  -webkit-user-drag: none;
}

[contenteditable], input {
  -webkit-user-select: auto;
}

// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var logging = requireNative('logging');

/**
 * Returns a function that logs a 'not available' error to the console and
 * returns undefined.
 *
 * @param {string} messagePrefix text to prepend to the exception message.
 */
function generateDisabledMethodStub(messagePrefix, opt_messageSuffix) {
  var message = messagePrefix + ' is not available in packaged apps.';
  if (opt_messageSuffix) message = message + ' ' + opt_messageSuffix;
  return function() {
    console.error(message);
    return;
  };
}

/**
 * Returns a function that throws a 'not available' error.
 *
 * @param {string} messagePrefix text to prepend to the exception message.
 */
function generateThrowingMethodStub(messagePrefix, opt_messageSuffix) {
  var message = messagePrefix + ' is not available in packaged apps.';
  if (opt_messageSuffix) message = message + ' ' + opt_messageSuffix;
  return function() {
    throw new Error(message);
  };
}

/**
 * Replaces the given methods of the passed in object with stubs that log
 * 'not available' errors to the console and return undefined.
 *
 * This should be used on methods attached via non-configurable properties,
 * such as window.alert. disableGetters should be used when possible, because
 * it is friendlier towards feature detection.
 *
 * In most cases, the useThrowingStubs should be false, so the stubs used to
 * replace the methods log an error to the console, but allow the calling code
 * to continue. We shouldn't break library code that uses feature detection
 * responsibly, such as:
 *     if(window.confirm) {
 *       var result = window.confirm('Are you sure you want to delete ...?');
 *       ...
 *     }
 *
 * useThrowingStubs should only be true for methods that are deprecated in the
 * Web platform, and should not be used by a responsible library, even in
 * conjunction with feature detection. A great example is document.write(), as
 * the HTML5 specification recommends against using it, and says that its
 * behavior is unreliable. No reasonable library code should ever use it.
 * HTML5 spec: http://www.w3.org/TR/html5/dom.html#dom-document-write
 *
 * @param {Object} object The object with methods to disable. The prototype is
 *     preferred.
 * @param {string} objectName The display name to use in the error message
 *     thrown by the stub (this is the name that the object is commonly referred
 *     to by web developers, e.g. "document" instead of "HTMLDocument").
 * @param {Array<string>} methodNames names of methods to disable.
 * @param {Boolean} useThrowingStubs if true, the replaced methods will throw
 *     an error instead of silently returning undefined
 */
function disableMethods(object, objectName, methodNames, useThrowingStubs) {
  $Array.forEach(methodNames, function(methodName) {
    logging.DCHECK($Object.getOwnPropertyDescriptor(object, methodName),
                   objectName + ': ' + methodName);
    var messagePrefix = objectName + '.' + methodName + '()';
    $Object.defineProperty(object, methodName, {
      configurable: false,
      enumerable: false,
      value: useThrowingStubs ?
                 generateThrowingMethodStub(messagePrefix) :
                 generateDisabledMethodStub(messagePrefix)
    });
  });
}

/**
 * Replaces the given properties of the passed in object with stubs that log
 * 'not available' warnings to the console and return undefined when gotten. If
 * a property's setter is later invoked, the getter and setter are restored to
 * default behaviors.
 *
 * @param {Object} object The object with properties to disable. The prototype
 *     is preferred.
 * @param {string} objectName The display name to use in the error message
 *     thrown by the getter stub (this is the name that the object is commonly
 *     referred to by web developers, e.g. "document" instead of
 *     "HTMLDocument").
 * @param {Array<string>} propertyNames names of properties to disable.
 * @param {?string=} opt_messageSuffix An optional suffix for the message.
 * @param {boolean=} opt_ignoreMissingProperty True if we allow disabling
 *     getters for non-existent properties.
 */
function disableGetters(object, objectName, propertyNames, opt_messageSuffix,
                        opt_ignoreMissingProperty) {
  $Array.forEach(propertyNames, function(propertyName) {
    logging.DCHECK(opt_ignoreMissingProperty ||
                       $Object.getOwnPropertyDescriptor(object, propertyName),
                   objectName + ': ' + propertyName);
    var stub = generateDisabledMethodStub(objectName + '.' + propertyName,
                                          opt_messageSuffix);
    stub._is_platform_app_disabled_getter = true;
    $Object.defineProperty(object, propertyName, {
      configurable: true,
      enumerable: false,
      get: stub,
      set: function(value) {
        var descriptor = $Object.getOwnPropertyDescriptor(this, propertyName);
        if (!descriptor || !descriptor.get ||
            descriptor.get._is_platform_app_disabled_getter) {
          // The stub getter is still defined.  Blow-away the property to
          // restore default getter/setter behaviors and re-create it with the
          // given value.
          delete this[propertyName];
          this[propertyName] = value;
        } else {
          // Do nothing.  If some custom getter (not ours) has been defined,
          // there would be no way to read back the value stored by a default
          // setter. Also, the only way to clear a custom getter is to first
          // delete the property.  Therefore, the value we have here should
          // just go into a black hole.
        }
      }
    });
  });
}

/**
 * Replaces the given properties of the passed in object with stubs that log
 * 'not available' warnings to the console when set.
 *
 * @param {Object} object The object with properties to disable. The prototype
 *     is preferred.
 * @param {string} objectName The display name to use in the error message
 *     thrown by the setter stub (this is the name that the object is commonly
 *     referred to by web developers, e.g. "document" instead of
 *     "HTMLDocument").
 * @param {Array<string>} propertyNames names of properties to disable.
 */
function disableSetters(object, objectName, propertyNames, opt_messageSuffix) {
  $Array.forEach(propertyNames, function(propertyName) {
    logging.DCHECK($Object.getOwnPropertyDescriptor(object, propertyName),
                   objectName + ': ' + propertyName);
    var stub = generateDisabledMethodStub(objectName + '.' + propertyName,
                                          opt_messageSuffix);
    $Object.defineProperty(object, propertyName, {
      configurable: false,
      enumerable: false,
      get: function() {
        return;
      },
      set: stub
    });
  });
}

// Disable benign Document methods.
disableMethods(Document.prototype, 'document', ['open', 'close']);
disableMethods(HTMLDocument.prototype, 'document', ['clear']);

// Replace evil Document methods with exception-throwing stubs.
disableMethods(Document.prototype, 'document', ['write', 'writeln'], true);

// Disable history.
Object.defineProperty(window, "history", { value: {} });
// Note: we just blew away the history object, so we need to ignore the fact
// that these properties aren't defined on the object.
disableGetters(window.history, 'history',
    ['back', 'forward', 'go', 'length', 'pushState', 'replaceState', 'state'],
    null, true);

// Disable find.
disableMethods(window, 'window', ['find']);

// Disable modal dialogs. Shell windows disable these anyway, but it's nice to
// warn.
disableMethods(window, 'window', ['alert', 'confirm', 'prompt']);

// Disable window.*bar.
disableGetters(window, 'window',
    ['locationbar', 'menubar', 'personalbar', 'scrollbars', 'statusbar',
     'toolbar']);

// Disable window.localStorage.
// Sometimes DOM security policy prevents us from doing this (e.g. for data:
// URLs) so wrap in try-catch.
try {
  disableGetters(window, 'window',
      ['localStorage'],
      'Use chrome.storage.local instead.');
} catch (e) {}

// Document instance properties that we wish to disable need to be set when
// the document begins loading, since only then will the "document" reference
// point to the page's document (it will be reset between now and then).
// We can't listen for the "readystatechange" event on the document (because
// the object that it's dispatched on doesn't exist yet), but we can instead
// do it at the window level in the capturing phase.
window.addEventListener('readystatechange', function(event) {
  if (document.readyState != 'loading')
    return;

  // Deprecated document properties from
  // https://developer.mozilla.org/en/DOM/document.
  // To deprecate document.all, simply changing its getter and setter would
  // activate its cache mechanism, and degrade the performance. Here we assign
  // it first to 'undefined' to avoid this.
  document.all = undefined;
  disableGetters(document, 'document',
      ['alinkColor', 'all', 'bgColor', 'fgColor', 'linkColor', 'vlinkColor'],
      null, true);
}, true);

// Disable onunload, onbeforeunload.
disableSetters(window, 'window', ['onbeforeunload', 'onunload']);
var eventTargetAddEventListener = EventTarget.prototype.addEventListener;
EventTarget.prototype.addEventListener = function(type) {
  var args = $Array.slice(arguments);
  // Note: Force conversion to a string in order to catch any funny attempts
  // to pass in something that evals to 'unload' but wouldn't === 'unload'.
  var type = (args[0] += '');
  if (type === 'unload' || type === 'beforeunload')
    generateDisabledMethodStub(type)();
  else
    return $Function.apply(eventTargetAddEventListener, this, args);
};
/*
 * Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * This stylesheet is used to apply Chrome system fonts to all extension pages.
 */

body {
  font-family: $FONTFAMILY;
  font-size: $FONTSIZE;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

define("chrome/browser/media/router/mojo/media_router.mojom", [
    "mojo/public/js/bindings",
    "mojo/public/js/codec",
    "mojo/public/js/connection",
    "mojo/public/js/core",
    "mojo/public/js/validator",
], function(bindings, codec, connection, core, validator) {
  var RouteRequestResultCode = {};
  RouteRequestResultCode.UNKNOWN_ERROR = 0;
  RouteRequestResultCode.OK = RouteRequestResultCode.UNKNOWN_ERROR + 1;
  RouteRequestResultCode.TIMED_OUT = RouteRequestResultCode.OK + 1;

  function MediaSink(values) {
    this.initDefaults_();
    this.initFields_(values);
  }

  MediaSink.IconType = {};
  MediaSink.IconType.CAST = 0;
  MediaSink.IconType.CAST_AUDIO = MediaSink.IconType.CAST + 1;
  MediaSink.IconType.CAST_AUDIO_GROUP = MediaSink.IconType.CAST_AUDIO + 1;
  MediaSink.IconType.GENERIC = MediaSink.IconType.CAST_AUDIO_GROUP + 1;
  MediaSink.IconType.HANGOUT = MediaSink.IconType.GENERIC + 1;

  MediaSink.prototype.initDefaults_ = function() {
    this.sink_id = null;
    this.name = null;
    this.description = null;
    this.domain = null;
    this.icon_type = 0;
  };
  MediaSink.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaSink.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaSink.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaSink.sink_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaSink.name
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaSink.description
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, true)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaSink.domain
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 24, true)
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaSink.encodedSize = codec.kStructHeaderSize + 40;

  MediaSink.decode = function(decoder) {
    var packed;
    var val = new MediaSink();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.sink_id = decoder.decodeStruct(codec.String);
    val.name = decoder.decodeStruct(codec.String);
    val.description = decoder.decodeStruct(codec.NullableString);
    val.domain = decoder.decodeStruct(codec.NullableString);
    val.icon_type = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaSink.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaSink.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.sink_id);
    encoder.encodeStruct(codec.String, val.name);
    encoder.encodeStruct(codec.NullableString, val.description);
    encoder.encodeStruct(codec.NullableString, val.domain);
    encoder.encodeStruct(codec.Int32, val.icon_type);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRoute(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRoute.prototype.initDefaults_ = function() {
    this.media_route_id = null;
    this.media_source = null;
    this.media_sink_id = null;
    this.description = null;
    this.is_local = false;
    this.for_display = false;
    this.off_the_record = false;
    this.custom_controller_path = null;
  };
  MediaRoute.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRoute.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRoute.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRoute.media_route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRoute.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, true)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRoute.media_sink_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRoute.description
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 24, false)
    if (err !== validator.validationError.NONE)
        return err;




    
    // validate MediaRoute.custom_controller_path
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 40, true)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRoute.encodedSize = codec.kStructHeaderSize + 48;

  MediaRoute.decode = function(decoder) {
    var packed;
    var val = new MediaRoute();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_route_id = decoder.decodeStruct(codec.String);
    val.media_source = decoder.decodeStruct(codec.NullableString);
    val.media_sink_id = decoder.decodeStruct(codec.String);
    val.description = decoder.decodeStruct(codec.String);
    packed = decoder.readUint8();
    val.is_local = (packed >> 0) & 1 ? true : false;
    val.for_display = (packed >> 1) & 1 ? true : false;
    val.off_the_record = (packed >> 2) & 1 ? true : false;
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.custom_controller_path = decoder.decodeStruct(codec.NullableString);
    return val;
  };

  MediaRoute.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRoute.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_route_id);
    encoder.encodeStruct(codec.NullableString, val.media_source);
    encoder.encodeStruct(codec.String, val.media_sink_id);
    encoder.encodeStruct(codec.String, val.description);
    packed = 0;
    packed |= (val.is_local & 1) << 0
    packed |= (val.for_display & 1) << 1
    packed |= (val.off_the_record & 1) << 2
    encoder.writeUint8(packed);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.NullableString, val.custom_controller_path);
  };
  function Issue(values) {
    this.initDefaults_();
    this.initFields_(values);
  }

  Issue.Severity = {};
  Issue.Severity.FATAL = 0;
  Issue.Severity.WARNING = Issue.Severity.FATAL + 1;
  Issue.Severity.NOTIFICATION = Issue.Severity.WARNING + 1;
  Issue.ActionType = {};
  Issue.ActionType.DISMISS = 0;
  Issue.ActionType.LEARN_MORE = Issue.ActionType.DISMISS + 1;

  Issue.prototype.initDefaults_ = function() {
    this.route_id = null;
    this.severity = 0;
    this.is_blocking = false;
    this.title = null;
    this.message = null;
    this.default_action = 0;
    this.secondary_actions = null;
    this.help_url = null;
  };
  Issue.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  Issue.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, Issue.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Issue.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, true)
    if (err !== validator.validationError.NONE)
        return err;



    
    // validate Issue.title
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Issue.message
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 24, true)
    if (err !== validator.validationError.NONE)
        return err;


    
    // validate Issue.secondary_actions
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 40, 4, codec.Int32, true, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate Issue.help_url
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 48, true)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  Issue.encodedSize = codec.kStructHeaderSize + 56;

  Issue.decode = function(decoder) {
    var packed;
    var val = new Issue();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.NullableString);
    val.severity = decoder.decodeStruct(codec.Int32);
    val.is_blocking = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.title = decoder.decodeStruct(codec.String);
    val.message = decoder.decodeStruct(codec.NullableString);
    val.default_action = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.secondary_actions = decoder.decodeArrayPointer(codec.Int32);
    val.help_url = decoder.decodeStruct(codec.NullableString);
    return val;
  };

  Issue.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(Issue.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.NullableString, val.route_id);
    encoder.encodeStruct(codec.Int32, val.severity);
    encoder.encodeStruct(codec.Uint8, val.is_blocking);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.String, val.title);
    encoder.encodeStruct(codec.NullableString, val.message);
    encoder.encodeStruct(codec.Int32, val.default_action);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeArrayPointer(codec.Int32, val.secondary_actions);
    encoder.encodeStruct(codec.NullableString, val.help_url);
  };
  function RouteMessage(values) {
    this.initDefaults_();
    this.initFields_(values);
  }

  RouteMessage.Type = {};
  RouteMessage.Type.TEXT = 0;
  RouteMessage.Type.BINARY = RouteMessage.Type.TEXT + 1;

  RouteMessage.prototype.initDefaults_ = function() {
    this.type = 0;
    this.message = null;
    this.data = null;
  };
  RouteMessage.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  RouteMessage.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, RouteMessage.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    
    // validate RouteMessage.message
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, true)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate RouteMessage.data
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 16, 1, codec.Uint8, true, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  RouteMessage.encodedSize = codec.kStructHeaderSize + 24;

  RouteMessage.decode = function(decoder) {
    var packed;
    var val = new RouteMessage();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.type = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.message = decoder.decodeStruct(codec.NullableString);
    val.data = decoder.decodeArrayPointer(codec.Uint8);
    return val;
  };

  RouteMessage.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(RouteMessage.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Int32, val.type);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.NullableString, val.message);
    encoder.encodeArrayPointer(codec.Uint8, val.data);
  };
  function MediaRouteProvider_CreateRoute_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_CreateRoute_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
    this.sink_id = null;
    this.original_presentation_id = null;
    this.origin = null;
    this.tab_id = 0;
    this.off_the_record = false;
    this.timeout_millis = 0;
  };
  MediaRouteProvider_CreateRoute_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_CreateRoute_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_CreateRoute_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_Params.sink_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_Params.original_presentation_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_Params.origin
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 24, false)
    if (err !== validator.validationError.NONE)
        return err;




    return validator.validationError.NONE;
  };

  MediaRouteProvider_CreateRoute_Params.encodedSize = codec.kStructHeaderSize + 48;

  MediaRouteProvider_CreateRoute_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_CreateRoute_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    val.sink_id = decoder.decodeStruct(codec.String);
    val.original_presentation_id = decoder.decodeStruct(codec.String);
    val.origin = decoder.decodeStruct(codec.String);
    val.tab_id = decoder.decodeStruct(codec.Int32);
    val.off_the_record = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.timeout_millis = decoder.decodeStruct(codec.Int64);
    return val;
  };

  MediaRouteProvider_CreateRoute_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_CreateRoute_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
    encoder.encodeStruct(codec.String, val.sink_id);
    encoder.encodeStruct(codec.String, val.original_presentation_id);
    encoder.encodeStruct(codec.String, val.origin);
    encoder.encodeStruct(codec.Int32, val.tab_id);
    encoder.encodeStruct(codec.Uint8, val.off_the_record);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.Int64, val.timeout_millis);
  };
  function MediaRouteProvider_CreateRoute_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_CreateRoute_ResponseParams.prototype.initDefaults_ = function() {
    this.route = null;
    this.error_text = null;
    this.result_code = 0;
  };
  MediaRouteProvider_CreateRoute_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_CreateRoute_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_CreateRoute_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_ResponseParams.route
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, MediaRoute, true);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_CreateRoute_ResponseParams.error_text
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, true)
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_CreateRoute_ResponseParams.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouteProvider_CreateRoute_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_CreateRoute_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route = decoder.decodeStructPointer(MediaRoute);
    val.error_text = decoder.decodeStruct(codec.NullableString);
    val.result_code = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_CreateRoute_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_CreateRoute_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(MediaRoute, val.route);
    encoder.encodeStruct(codec.NullableString, val.error_text);
    encoder.encodeStruct(codec.Int32, val.result_code);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_JoinRoute_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_JoinRoute_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
    this.presentation_id = null;
    this.origin = null;
    this.tab_id = 0;
    this.off_the_record = false;
    this.timeout_millis = 0;
  };
  MediaRouteProvider_JoinRoute_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_JoinRoute_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_JoinRoute_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_JoinRoute_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_JoinRoute_Params.presentation_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_JoinRoute_Params.origin
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;




    return validator.validationError.NONE;
  };

  MediaRouteProvider_JoinRoute_Params.encodedSize = codec.kStructHeaderSize + 40;

  MediaRouteProvider_JoinRoute_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_JoinRoute_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    val.presentation_id = decoder.decodeStruct(codec.String);
    val.origin = decoder.decodeStruct(codec.String);
    val.tab_id = decoder.decodeStruct(codec.Int32);
    val.off_the_record = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.timeout_millis = decoder.decodeStruct(codec.Int64);
    return val;
  };

  MediaRouteProvider_JoinRoute_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_JoinRoute_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
    encoder.encodeStruct(codec.String, val.presentation_id);
    encoder.encodeStruct(codec.String, val.origin);
    encoder.encodeStruct(codec.Int32, val.tab_id);
    encoder.encodeStruct(codec.Uint8, val.off_the_record);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.Int64, val.timeout_millis);
  };
  function MediaRouteProvider_JoinRoute_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_JoinRoute_ResponseParams.prototype.initDefaults_ = function() {
    this.route = null;
    this.error_text = null;
    this.result_code = 0;
  };
  MediaRouteProvider_JoinRoute_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_JoinRoute_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_JoinRoute_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_JoinRoute_ResponseParams.route
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, MediaRoute, true);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_JoinRoute_ResponseParams.error_text
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, true)
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_JoinRoute_ResponseParams.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouteProvider_JoinRoute_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_JoinRoute_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route = decoder.decodeStructPointer(MediaRoute);
    val.error_text = decoder.decodeStruct(codec.NullableString);
    val.result_code = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_JoinRoute_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_JoinRoute_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(MediaRoute, val.route);
    encoder.encodeStruct(codec.NullableString, val.error_text);
    encoder.encodeStruct(codec.Int32, val.result_code);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_ConnectRouteByRouteId_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_ConnectRouteByRouteId_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
    this.route_id = null;
    this.presentation_id = null;
    this.origin = null;
    this.tab_id = 0;
    this.off_the_record = false;
    this.timeout_millis = 0;
  };
  MediaRouteProvider_ConnectRouteByRouteId_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_ConnectRouteByRouteId_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_ConnectRouteByRouteId_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_Params.presentation_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_Params.origin
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 24, false)
    if (err !== validator.validationError.NONE)
        return err;




    return validator.validationError.NONE;
  };

  MediaRouteProvider_ConnectRouteByRouteId_Params.encodedSize = codec.kStructHeaderSize + 48;

  MediaRouteProvider_ConnectRouteByRouteId_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_ConnectRouteByRouteId_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    val.route_id = decoder.decodeStruct(codec.String);
    val.presentation_id = decoder.decodeStruct(codec.String);
    val.origin = decoder.decodeStruct(codec.String);
    val.tab_id = decoder.decodeStruct(codec.Int32);
    val.off_the_record = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.timeout_millis = decoder.decodeStruct(codec.Int64);
    return val;
  };

  MediaRouteProvider_ConnectRouteByRouteId_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_ConnectRouteByRouteId_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
    encoder.encodeStruct(codec.String, val.route_id);
    encoder.encodeStruct(codec.String, val.presentation_id);
    encoder.encodeStruct(codec.String, val.origin);
    encoder.encodeStruct(codec.Int32, val.tab_id);
    encoder.encodeStruct(codec.Uint8, val.off_the_record);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.Int64, val.timeout_millis);
  };
  function MediaRouteProvider_ConnectRouteByRouteId_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.prototype.initDefaults_ = function() {
    this.route = null;
    this.error_text = null;
    this.result_code = 0;
  };
  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.route
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, MediaRoute, true);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.error_text
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, true)
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_ConnectRouteByRouteId_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route = decoder.decodeStructPointer(MediaRoute);
    val.error_text = decoder.decodeStruct(codec.NullableString);
    val.result_code = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(MediaRoute, val.route);
    encoder.encodeStruct(codec.NullableString, val.error_text);
    encoder.encodeStruct(codec.Int32, val.result_code);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_TerminateRoute_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_TerminateRoute_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
  };
  MediaRouteProvider_TerminateRoute_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_TerminateRoute_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_TerminateRoute_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_TerminateRoute_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_TerminateRoute_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_TerminateRoute_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_TerminateRoute_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_TerminateRoute_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_TerminateRoute_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
  };
  function MediaRouteProvider_SendRouteMessage_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_SendRouteMessage_Params.prototype.initDefaults_ = function() {
    this.media_route_id = null;
    this.message = null;
  };
  MediaRouteProvider_SendRouteMessage_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_SendRouteMessage_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_SendRouteMessage_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_SendRouteMessage_Params.media_route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_SendRouteMessage_Params.message
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_SendRouteMessage_Params.encodedSize = codec.kStructHeaderSize + 16;

  MediaRouteProvider_SendRouteMessage_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_SendRouteMessage_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_route_id = decoder.decodeStruct(codec.String);
    val.message = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_SendRouteMessage_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_SendRouteMessage_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_route_id);
    encoder.encodeStruct(codec.String, val.message);
  };
  function MediaRouteProvider_SendRouteMessage_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_SendRouteMessage_ResponseParams.prototype.initDefaults_ = function() {
    this.sent = false;
  };
  MediaRouteProvider_SendRouteMessage_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_SendRouteMessage_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_SendRouteMessage_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_SendRouteMessage_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_SendRouteMessage_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_SendRouteMessage_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.sent = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_SendRouteMessage_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_SendRouteMessage_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint8, val.sent);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_SendRouteBinaryMessage_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_SendRouteBinaryMessage_Params.prototype.initDefaults_ = function() {
    this.media_route_id = null;
    this.data = null;
  };
  MediaRouteProvider_SendRouteBinaryMessage_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_SendRouteBinaryMessage_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_SendRouteBinaryMessage_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_SendRouteBinaryMessage_Params.media_route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_SendRouteBinaryMessage_Params.data
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 8, 1, codec.Uint8, false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_SendRouteBinaryMessage_Params.encodedSize = codec.kStructHeaderSize + 16;

  MediaRouteProvider_SendRouteBinaryMessage_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_SendRouteBinaryMessage_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_route_id = decoder.decodeStruct(codec.String);
    val.data = decoder.decodeArrayPointer(codec.Uint8);
    return val;
  };

  MediaRouteProvider_SendRouteBinaryMessage_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_SendRouteBinaryMessage_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_route_id);
    encoder.encodeArrayPointer(codec.Uint8, val.data);
  };
  function MediaRouteProvider_SendRouteBinaryMessage_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.prototype.initDefaults_ = function() {
    this.sent = false;
  };
  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_SendRouteBinaryMessage_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.sent = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Uint8, val.sent);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_StartObservingMediaSinks_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_StartObservingMediaSinks_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
  };
  MediaRouteProvider_StartObservingMediaSinks_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_StartObservingMediaSinks_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_StartObservingMediaSinks_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_StartObservingMediaSinks_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_StartObservingMediaSinks_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_StartObservingMediaSinks_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_StartObservingMediaSinks_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_StartObservingMediaSinks_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_StartObservingMediaSinks_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
  };
  function MediaRouteProvider_StopObservingMediaSinks_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_StopObservingMediaSinks_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
  };
  MediaRouteProvider_StopObservingMediaSinks_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_StopObservingMediaSinks_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_StopObservingMediaSinks_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_StopObservingMediaSinks_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_StopObservingMediaSinks_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_StopObservingMediaSinks_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_StopObservingMediaSinks_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_StopObservingMediaSinks_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_StopObservingMediaSinks_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
  };
  function MediaRouteProvider_StartObservingMediaRoutes_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_StartObservingMediaRoutes_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
  };
  MediaRouteProvider_StartObservingMediaRoutes_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_StartObservingMediaRoutes_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_StartObservingMediaRoutes_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_StartObservingMediaRoutes_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_StartObservingMediaRoutes_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_StartObservingMediaRoutes_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_StartObservingMediaRoutes_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_StartObservingMediaRoutes_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_StartObservingMediaRoutes_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
  };
  function MediaRouteProvider_StopObservingMediaRoutes_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_StopObservingMediaRoutes_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
  };
  MediaRouteProvider_StopObservingMediaRoutes_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_StopObservingMediaRoutes_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_StopObservingMediaRoutes_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_StopObservingMediaRoutes_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_StopObservingMediaRoutes_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_StopObservingMediaRoutes_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_StopObservingMediaRoutes_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_StopObservingMediaRoutes_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_StopObservingMediaRoutes_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
  };
  function MediaRouteProvider_ListenForRouteMessages_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_ListenForRouteMessages_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
  };
  MediaRouteProvider_ListenForRouteMessages_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_ListenForRouteMessages_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_ListenForRouteMessages_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ListenForRouteMessages_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_ListenForRouteMessages_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_ListenForRouteMessages_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_ListenForRouteMessages_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_ListenForRouteMessages_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_ListenForRouteMessages_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
  };
  function MediaRouteProvider_ListenForRouteMessages_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_ListenForRouteMessages_ResponseParams.prototype.initDefaults_ = function() {
    this.messages = null;
    this.error = false;
  };
  MediaRouteProvider_ListenForRouteMessages_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_ListenForRouteMessages_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_ListenForRouteMessages_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_ListenForRouteMessages_ResponseParams.messages
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 0, 8, new codec.PointerTo(RouteMessage), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouteProvider_ListenForRouteMessages_ResponseParams.encodedSize = codec.kStructHeaderSize + 16;

  MediaRouteProvider_ListenForRouteMessages_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_ListenForRouteMessages_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.messages = decoder.decodeArrayPointer(new codec.PointerTo(RouteMessage));
    val.error = decoder.decodeStruct(codec.Uint8);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouteProvider_ListenForRouteMessages_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_ListenForRouteMessages_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeArrayPointer(new codec.PointerTo(RouteMessage), val.messages);
    encoder.encodeStruct(codec.Uint8, val.error);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouteProvider_StopListeningForRouteMessages_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_StopListeningForRouteMessages_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
  };
  MediaRouteProvider_StopListeningForRouteMessages_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_StopListeningForRouteMessages_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_StopListeningForRouteMessages_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_StopListeningForRouteMessages_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_StopListeningForRouteMessages_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_StopListeningForRouteMessages_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_StopListeningForRouteMessages_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_StopListeningForRouteMessages_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_StopListeningForRouteMessages_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
  };
  function MediaRouteProvider_DetachRoute_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_DetachRoute_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
  };
  MediaRouteProvider_DetachRoute_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_DetachRoute_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_DetachRoute_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_DetachRoute_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_DetachRoute_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_DetachRoute_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_DetachRoute_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_DetachRoute_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_DetachRoute_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
  };
  function MediaRouteProvider_EnableMdnsDiscovery_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_EnableMdnsDiscovery_Params.prototype.initDefaults_ = function() {
  };
  MediaRouteProvider_EnableMdnsDiscovery_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_EnableMdnsDiscovery_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_EnableMdnsDiscovery_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_EnableMdnsDiscovery_Params.encodedSize = codec.kStructHeaderSize + 0;

  MediaRouteProvider_EnableMdnsDiscovery_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_EnableMdnsDiscovery_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    return val;
  };

  MediaRouteProvider_EnableMdnsDiscovery_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_EnableMdnsDiscovery_Params.encodedSize);
    encoder.writeUint32(0);
  };
  function MediaRouteProvider_UpdateMediaSinks_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouteProvider_UpdateMediaSinks_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
  };
  MediaRouteProvider_UpdateMediaSinks_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouteProvider_UpdateMediaSinks_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouteProvider_UpdateMediaSinks_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouteProvider_UpdateMediaSinks_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouteProvider_UpdateMediaSinks_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouteProvider_UpdateMediaSinks_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouteProvider_UpdateMediaSinks_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouteProvider_UpdateMediaSinks_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouteProvider_UpdateMediaSinks_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
  };
  function MediaRouter_RegisterMediaRouteProvider_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_RegisterMediaRouteProvider_Params.prototype.initDefaults_ = function() {
    this.media_router_provider = null;
  };
  MediaRouter_RegisterMediaRouteProvider_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_RegisterMediaRouteProvider_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_RegisterMediaRouteProvider_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_RegisterMediaRouteProvider_Params.media_router_provider
    err = messageValidator.validateInterface(offset + codec.kStructHeaderSize + 0, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_RegisterMediaRouteProvider_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouter_RegisterMediaRouteProvider_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_RegisterMediaRouteProvider_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_router_provider = decoder.decodeStruct(codec.Interface);
    return val;
  };

  MediaRouter_RegisterMediaRouteProvider_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_RegisterMediaRouteProvider_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Interface, val.media_router_provider);
  };
  function MediaRouter_RegisterMediaRouteProvider_ResponseParams(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_RegisterMediaRouteProvider_ResponseParams.prototype.initDefaults_ = function() {
    this.instance_id = null;
  };
  MediaRouter_RegisterMediaRouteProvider_ResponseParams.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_RegisterMediaRouteProvider_ResponseParams.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_RegisterMediaRouteProvider_ResponseParams.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_RegisterMediaRouteProvider_ResponseParams.instance_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_RegisterMediaRouteProvider_ResponseParams.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouter_RegisterMediaRouteProvider_ResponseParams.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_RegisterMediaRouteProvider_ResponseParams();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.instance_id = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouter_RegisterMediaRouteProvider_ResponseParams.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_RegisterMediaRouteProvider_ResponseParams.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.instance_id);
  };
  function MediaRouter_OnSinksReceived_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnSinksReceived_Params.prototype.initDefaults_ = function() {
    this.media_source = null;
    this.sinks = null;
    this.origins = null;
  };
  MediaRouter_OnSinksReceived_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnSinksReceived_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnSinksReceived_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnSinksReceived_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnSinksReceived_Params.sinks
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 8, 8, new codec.PointerTo(MediaSink), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnSinksReceived_Params.origins
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 16, 8, codec.String, false, [0, 0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_OnSinksReceived_Params.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouter_OnSinksReceived_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnSinksReceived_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.media_source = decoder.decodeStruct(codec.String);
    val.sinks = decoder.decodeArrayPointer(new codec.PointerTo(MediaSink));
    val.origins = decoder.decodeArrayPointer(codec.String);
    return val;
  };

  MediaRouter_OnSinksReceived_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnSinksReceived_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.media_source);
    encoder.encodeArrayPointer(new codec.PointerTo(MediaSink), val.sinks);
    encoder.encodeArrayPointer(codec.String, val.origins);
  };
  function MediaRouter_OnIssue_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnIssue_Params.prototype.initDefaults_ = function() {
    this.issue = null;
  };
  MediaRouter_OnIssue_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnIssue_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnIssue_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnIssue_Params.issue
    err = messageValidator.validateStructPointer(offset + codec.kStructHeaderSize + 0, Issue, false);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_OnIssue_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouter_OnIssue_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnIssue_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.issue = decoder.decodeStructPointer(Issue);
    return val;
  };

  MediaRouter_OnIssue_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnIssue_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStructPointer(Issue, val.issue);
  };
  function MediaRouter_OnRoutesUpdated_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnRoutesUpdated_Params.prototype.initDefaults_ = function() {
    this.routes = null;
    this.media_source = null;
    this.joinable_route_ids = null;
  };
  MediaRouter_OnRoutesUpdated_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnRoutesUpdated_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnRoutesUpdated_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnRoutesUpdated_Params.routes
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 0, 8, new codec.PointerTo(MediaRoute), false, [0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnRoutesUpdated_Params.media_source
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 8, false)
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnRoutesUpdated_Params.joinable_route_ids
    err = messageValidator.validateArrayPointer(offset + codec.kStructHeaderSize + 16, 8, codec.String, false, [0, 0], 0);
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_OnRoutesUpdated_Params.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouter_OnRoutesUpdated_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnRoutesUpdated_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.routes = decoder.decodeArrayPointer(new codec.PointerTo(MediaRoute));
    val.media_source = decoder.decodeStruct(codec.String);
    val.joinable_route_ids = decoder.decodeArrayPointer(codec.String);
    return val;
  };

  MediaRouter_OnRoutesUpdated_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnRoutesUpdated_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeArrayPointer(new codec.PointerTo(MediaRoute), val.routes);
    encoder.encodeStruct(codec.String, val.media_source);
    encoder.encodeArrayPointer(codec.String, val.joinable_route_ids);
  };
  function MediaRouter_OnSinkAvailabilityUpdated_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnSinkAvailabilityUpdated_Params.prototype.initDefaults_ = function() {
    this.availability = 0;
  };
  MediaRouter_OnSinkAvailabilityUpdated_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnSinkAvailabilityUpdated_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnSinkAvailabilityUpdated_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouter_OnSinkAvailabilityUpdated_Params.encodedSize = codec.kStructHeaderSize + 8;

  MediaRouter_OnSinkAvailabilityUpdated_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnSinkAvailabilityUpdated_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.availability = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouter_OnSinkAvailabilityUpdated_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnSinkAvailabilityUpdated_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.Int32, val.availability);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouter_OnPresentationConnectionStateChanged_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnPresentationConnectionStateChanged_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
    this.state = 0;
  };
  MediaRouter_OnPresentationConnectionStateChanged_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnPresentationConnectionStateChanged_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnPresentationConnectionStateChanged_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnPresentationConnectionStateChanged_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;


    return validator.validationError.NONE;
  };

  MediaRouter_OnPresentationConnectionStateChanged_Params.encodedSize = codec.kStructHeaderSize + 16;

  MediaRouter_OnPresentationConnectionStateChanged_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnPresentationConnectionStateChanged_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    val.state = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    return val;
  };

  MediaRouter_OnPresentationConnectionStateChanged_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnPresentationConnectionStateChanged_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
    encoder.encodeStruct(codec.Int32, val.state);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
  };
  function MediaRouter_OnPresentationConnectionClosed_Params(values) {
    this.initDefaults_();
    this.initFields_(values);
  }


  MediaRouter_OnPresentationConnectionClosed_Params.prototype.initDefaults_ = function() {
    this.route_id = null;
    this.reason = 0;
    this.message = null;
  };
  MediaRouter_OnPresentationConnectionClosed_Params.prototype.initFields_ = function(fields) {
    for(var field in fields) {
        if (this.hasOwnProperty(field))
          this[field] = fields[field];
    }
  };

  MediaRouter_OnPresentationConnectionClosed_Params.validate = function(messageValidator, offset) {
    var err;
    err = messageValidator.validateStructHeader(offset, MediaRouter_OnPresentationConnectionClosed_Params.encodedSize, 0);
    if (err !== validator.validationError.NONE)
        return err;

    
    // validate MediaRouter_OnPresentationConnectionClosed_Params.route_id
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 0, false)
    if (err !== validator.validationError.NONE)
        return err;


    
    // validate MediaRouter_OnPresentationConnectionClosed_Params.message
    err = messageValidator.validateStringPointer(offset + codec.kStructHeaderSize + 16, false)
    if (err !== validator.validationError.NONE)
        return err;

    return validator.validationError.NONE;
  };

  MediaRouter_OnPresentationConnectionClosed_Params.encodedSize = codec.kStructHeaderSize + 24;

  MediaRouter_OnPresentationConnectionClosed_Params.decode = function(decoder) {
    var packed;
    var val = new MediaRouter_OnPresentationConnectionClosed_Params();
    var numberOfBytes = decoder.readUint32();
    var version = decoder.readUint32();
    val.route_id = decoder.decodeStruct(codec.String);
    val.reason = decoder.decodeStruct(codec.Int32);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    decoder.skip(1);
    val.message = decoder.decodeStruct(codec.String);
    return val;
  };

  MediaRouter_OnPresentationConnectionClosed_Params.encode = function(encoder, val) {
    var packed;
    encoder.writeUint32(MediaRouter_OnPresentationConnectionClosed_Params.encodedSize);
    encoder.writeUint32(0);
    encoder.encodeStruct(codec.String, val.route_id);
    encoder.encodeStruct(codec.Int32, val.reason);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.skip(1);
    encoder.encodeStruct(codec.String, val.message);
  };
  var kMediaRouteProvider_CreateRoute_Name = 0;
  var kMediaRouteProvider_JoinRoute_Name = 1;
  var kMediaRouteProvider_ConnectRouteByRouteId_Name = 2;
  var kMediaRouteProvider_TerminateRoute_Name = 3;
  var kMediaRouteProvider_SendRouteMessage_Name = 4;
  var kMediaRouteProvider_SendRouteBinaryMessage_Name = 5;
  var kMediaRouteProvider_StartObservingMediaSinks_Name = 6;
  var kMediaRouteProvider_StopObservingMediaSinks_Name = 7;
  var kMediaRouteProvider_StartObservingMediaRoutes_Name = 8;
  var kMediaRouteProvider_StopObservingMediaRoutes_Name = 9;
  var kMediaRouteProvider_ListenForRouteMessages_Name = 10;
  var kMediaRouteProvider_StopListeningForRouteMessages_Name = 11;
  var kMediaRouteProvider_DetachRoute_Name = 12;
  var kMediaRouteProvider_EnableMdnsDiscovery_Name = 13;
  var kMediaRouteProvider_UpdateMediaSinks_Name = 14;

  function MediaRouteProviderProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  MediaRouteProviderProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  MediaRouteProviderProxy.prototype.createRoute = function(media_source, sink_id, original_presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    var params = new MediaRouteProvider_CreateRoute_Params();
    params.media_source = media_source;
    params.sink_id = sink_id;
    params.original_presentation_id = original_presentation_id;
    params.origin = origin;
    params.tab_id = tab_id;
    params.timeout_millis = timeout_millis;
    params.off_the_record = off_the_record;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_CreateRoute_Name,
          codec.align(MediaRouteProvider_CreateRoute_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_CreateRoute_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_CreateRoute_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.joinRoute = function(media_source, presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    var params = new MediaRouteProvider_JoinRoute_Params();
    params.media_source = media_source;
    params.presentation_id = presentation_id;
    params.origin = origin;
    params.tab_id = tab_id;
    params.timeout_millis = timeout_millis;
    params.off_the_record = off_the_record;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_JoinRoute_Name,
          codec.align(MediaRouteProvider_JoinRoute_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_JoinRoute_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_JoinRoute_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.connectRouteByRouteId = function(media_source, route_id, presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    var params = new MediaRouteProvider_ConnectRouteByRouteId_Params();
    params.media_source = media_source;
    params.route_id = route_id;
    params.presentation_id = presentation_id;
    params.origin = origin;
    params.tab_id = tab_id;
    params.timeout_millis = timeout_millis;
    params.off_the_record = off_the_record;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_ConnectRouteByRouteId_Name,
          codec.align(MediaRouteProvider_ConnectRouteByRouteId_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_ConnectRouteByRouteId_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_ConnectRouteByRouteId_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.terminateRoute = function(route_id) {
    var params = new MediaRouteProvider_TerminateRoute_Params();
    params.route_id = route_id;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_TerminateRoute_Name,
        codec.align(MediaRouteProvider_TerminateRoute_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_TerminateRoute_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.sendRouteMessage = function(media_route_id, message) {
    var params = new MediaRouteProvider_SendRouteMessage_Params();
    params.media_route_id = media_route_id;
    params.message = message;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_SendRouteMessage_Name,
          codec.align(MediaRouteProvider_SendRouteMessage_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_SendRouteMessage_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_SendRouteMessage_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.sendRouteBinaryMessage = function(media_route_id, data) {
    var params = new MediaRouteProvider_SendRouteBinaryMessage_Params();
    params.media_route_id = media_route_id;
    params.data = data;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_SendRouteBinaryMessage_Name,
          codec.align(MediaRouteProvider_SendRouteBinaryMessage_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_SendRouteBinaryMessage_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_SendRouteBinaryMessage_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.startObservingMediaSinks = function(media_source) {
    var params = new MediaRouteProvider_StartObservingMediaSinks_Params();
    params.media_source = media_source;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_StartObservingMediaSinks_Name,
        codec.align(MediaRouteProvider_StartObservingMediaSinks_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_StartObservingMediaSinks_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.stopObservingMediaSinks = function(media_source) {
    var params = new MediaRouteProvider_StopObservingMediaSinks_Params();
    params.media_source = media_source;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_StopObservingMediaSinks_Name,
        codec.align(MediaRouteProvider_StopObservingMediaSinks_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_StopObservingMediaSinks_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.startObservingMediaRoutes = function(media_source) {
    var params = new MediaRouteProvider_StartObservingMediaRoutes_Params();
    params.media_source = media_source;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_StartObservingMediaRoutes_Name,
        codec.align(MediaRouteProvider_StartObservingMediaRoutes_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_StartObservingMediaRoutes_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.stopObservingMediaRoutes = function(media_source) {
    var params = new MediaRouteProvider_StopObservingMediaRoutes_Params();
    params.media_source = media_source;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_StopObservingMediaRoutes_Name,
        codec.align(MediaRouteProvider_StopObservingMediaRoutes_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_StopObservingMediaRoutes_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.listenForRouteMessages = function(route_id) {
    var params = new MediaRouteProvider_ListenForRouteMessages_Params();
    params.route_id = route_id;
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouteProvider_ListenForRouteMessages_Name,
          codec.align(MediaRouteProvider_ListenForRouteMessages_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouteProvider_ListenForRouteMessages_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouteProvider_ListenForRouteMessages_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouteProviderProxy.prototype.stopListeningForRouteMessages = function(route_id) {
    var params = new MediaRouteProvider_StopListeningForRouteMessages_Params();
    params.route_id = route_id;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_StopListeningForRouteMessages_Name,
        codec.align(MediaRouteProvider_StopListeningForRouteMessages_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_StopListeningForRouteMessages_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.detachRoute = function(route_id) {
    var params = new MediaRouteProvider_DetachRoute_Params();
    params.route_id = route_id;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_DetachRoute_Name,
        codec.align(MediaRouteProvider_DetachRoute_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_DetachRoute_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.enableMdnsDiscovery = function() {
    var params = new MediaRouteProvider_EnableMdnsDiscovery_Params();
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_EnableMdnsDiscovery_Name,
        codec.align(MediaRouteProvider_EnableMdnsDiscovery_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_EnableMdnsDiscovery_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouteProviderProxy.prototype.updateMediaSinks = function(media_source) {
    var params = new MediaRouteProvider_UpdateMediaSinks_Params();
    params.media_source = media_source;
    var builder = new codec.MessageBuilder(
        kMediaRouteProvider_UpdateMediaSinks_Name,
        codec.align(MediaRouteProvider_UpdateMediaSinks_Params.encodedSize));
    builder.encodeStruct(MediaRouteProvider_UpdateMediaSinks_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };

  function MediaRouteProviderStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  MediaRouteProviderStub.prototype = Object.create(bindings.StubBase.prototype);
  MediaRouteProviderStub.prototype.createRoute = function(media_source, sink_id, original_presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.createRoute && bindings.StubBindings(this).delegate.createRoute(media_source, sink_id, original_presentation_id, origin, tab_id, timeout_millis, off_the_record);
  }
  MediaRouteProviderStub.prototype.joinRoute = function(media_source, presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.joinRoute && bindings.StubBindings(this).delegate.joinRoute(media_source, presentation_id, origin, tab_id, timeout_millis, off_the_record);
  }
  MediaRouteProviderStub.prototype.connectRouteByRouteId = function(media_source, route_id, presentation_id, origin, tab_id, timeout_millis, off_the_record) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.connectRouteByRouteId && bindings.StubBindings(this).delegate.connectRouteByRouteId(media_source, route_id, presentation_id, origin, tab_id, timeout_millis, off_the_record);
  }
  MediaRouteProviderStub.prototype.terminateRoute = function(route_id) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.terminateRoute && bindings.StubBindings(this).delegate.terminateRoute(route_id);
  }
  MediaRouteProviderStub.prototype.sendRouteMessage = function(media_route_id, message) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.sendRouteMessage && bindings.StubBindings(this).delegate.sendRouteMessage(media_route_id, message);
  }
  MediaRouteProviderStub.prototype.sendRouteBinaryMessage = function(media_route_id, data) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.sendRouteBinaryMessage && bindings.StubBindings(this).delegate.sendRouteBinaryMessage(media_route_id, data);
  }
  MediaRouteProviderStub.prototype.startObservingMediaSinks = function(media_source) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.startObservingMediaSinks && bindings.StubBindings(this).delegate.startObservingMediaSinks(media_source);
  }
  MediaRouteProviderStub.prototype.stopObservingMediaSinks = function(media_source) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.stopObservingMediaSinks && bindings.StubBindings(this).delegate.stopObservingMediaSinks(media_source);
  }
  MediaRouteProviderStub.prototype.startObservingMediaRoutes = function(media_source) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.startObservingMediaRoutes && bindings.StubBindings(this).delegate.startObservingMediaRoutes(media_source);
  }
  MediaRouteProviderStub.prototype.stopObservingMediaRoutes = function(media_source) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.stopObservingMediaRoutes && bindings.StubBindings(this).delegate.stopObservingMediaRoutes(media_source);
  }
  MediaRouteProviderStub.prototype.listenForRouteMessages = function(route_id) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.listenForRouteMessages && bindings.StubBindings(this).delegate.listenForRouteMessages(route_id);
  }
  MediaRouteProviderStub.prototype.stopListeningForRouteMessages = function(route_id) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.stopListeningForRouteMessages && bindings.StubBindings(this).delegate.stopListeningForRouteMessages(route_id);
  }
  MediaRouteProviderStub.prototype.detachRoute = function(route_id) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.detachRoute && bindings.StubBindings(this).delegate.detachRoute(route_id);
  }
  MediaRouteProviderStub.prototype.enableMdnsDiscovery = function() {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.enableMdnsDiscovery && bindings.StubBindings(this).delegate.enableMdnsDiscovery();
  }
  MediaRouteProviderStub.prototype.updateMediaSinks = function(media_source) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.updateMediaSinks && bindings.StubBindings(this).delegate.updateMediaSinks(media_source);
  }

  MediaRouteProviderStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kMediaRouteProvider_TerminateRoute_Name:
      var params = reader.decodeStruct(MediaRouteProvider_TerminateRoute_Params);
      this.terminateRoute(params.route_id);
      return true;
    case kMediaRouteProvider_StartObservingMediaSinks_Name:
      var params = reader.decodeStruct(MediaRouteProvider_StartObservingMediaSinks_Params);
      this.startObservingMediaSinks(params.media_source);
      return true;
    case kMediaRouteProvider_StopObservingMediaSinks_Name:
      var params = reader.decodeStruct(MediaRouteProvider_StopObservingMediaSinks_Params);
      this.stopObservingMediaSinks(params.media_source);
      return true;
    case kMediaRouteProvider_StartObservingMediaRoutes_Name:
      var params = reader.decodeStruct(MediaRouteProvider_StartObservingMediaRoutes_Params);
      this.startObservingMediaRoutes(params.media_source);
      return true;
    case kMediaRouteProvider_StopObservingMediaRoutes_Name:
      var params = reader.decodeStruct(MediaRouteProvider_StopObservingMediaRoutes_Params);
      this.stopObservingMediaRoutes(params.media_source);
      return true;
    case kMediaRouteProvider_StopListeningForRouteMessages_Name:
      var params = reader.decodeStruct(MediaRouteProvider_StopListeningForRouteMessages_Params);
      this.stopListeningForRouteMessages(params.route_id);
      return true;
    case kMediaRouteProvider_DetachRoute_Name:
      var params = reader.decodeStruct(MediaRouteProvider_DetachRoute_Params);
      this.detachRoute(params.route_id);
      return true;
    case kMediaRouteProvider_EnableMdnsDiscovery_Name:
      var params = reader.decodeStruct(MediaRouteProvider_EnableMdnsDiscovery_Params);
      this.enableMdnsDiscovery();
      return true;
    case kMediaRouteProvider_UpdateMediaSinks_Name:
      var params = reader.decodeStruct(MediaRouteProvider_UpdateMediaSinks_Params);
      this.updateMediaSinks(params.media_source);
      return true;
    default:
      return false;
    }
  };

  MediaRouteProviderStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kMediaRouteProvider_CreateRoute_Name:
      var params = reader.decodeStruct(MediaRouteProvider_CreateRoute_Params);
      return this.createRoute(params.media_source, params.sink_id, params.original_presentation_id, params.origin, params.tab_id, params.timeout_millis, params.off_the_record).then(function(response) {
        var responseParams =
            new MediaRouteProvider_CreateRoute_ResponseParams();
        responseParams.route = response.route;
        responseParams.error_text = response.error_text;
        responseParams.result_code = response.result_code;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_CreateRoute_Name,
            codec.align(MediaRouteProvider_CreateRoute_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_CreateRoute_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMediaRouteProvider_JoinRoute_Name:
      var params = reader.decodeStruct(MediaRouteProvider_JoinRoute_Params);
      return this.joinRoute(params.media_source, params.presentation_id, params.origin, params.tab_id, params.timeout_millis, params.off_the_record).then(function(response) {
        var responseParams =
            new MediaRouteProvider_JoinRoute_ResponseParams();
        responseParams.route = response.route;
        responseParams.error_text = response.error_text;
        responseParams.result_code = response.result_code;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_JoinRoute_Name,
            codec.align(MediaRouteProvider_JoinRoute_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_JoinRoute_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMediaRouteProvider_ConnectRouteByRouteId_Name:
      var params = reader.decodeStruct(MediaRouteProvider_ConnectRouteByRouteId_Params);
      return this.connectRouteByRouteId(params.media_source, params.route_id, params.presentation_id, params.origin, params.tab_id, params.timeout_millis, params.off_the_record).then(function(response) {
        var responseParams =
            new MediaRouteProvider_ConnectRouteByRouteId_ResponseParams();
        responseParams.route = response.route;
        responseParams.error_text = response.error_text;
        responseParams.result_code = response.result_code;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_ConnectRouteByRouteId_Name,
            codec.align(MediaRouteProvider_ConnectRouteByRouteId_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_ConnectRouteByRouteId_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMediaRouteProvider_SendRouteMessage_Name:
      var params = reader.decodeStruct(MediaRouteProvider_SendRouteMessage_Params);
      return this.sendRouteMessage(params.media_route_id, params.message).then(function(response) {
        var responseParams =
            new MediaRouteProvider_SendRouteMessage_ResponseParams();
        responseParams.sent = response.sent;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_SendRouteMessage_Name,
            codec.align(MediaRouteProvider_SendRouteMessage_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_SendRouteMessage_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMediaRouteProvider_SendRouteBinaryMessage_Name:
      var params = reader.decodeStruct(MediaRouteProvider_SendRouteBinaryMessage_Params);
      return this.sendRouteBinaryMessage(params.media_route_id, params.data).then(function(response) {
        var responseParams =
            new MediaRouteProvider_SendRouteBinaryMessage_ResponseParams();
        responseParams.sent = response.sent;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_SendRouteBinaryMessage_Name,
            codec.align(MediaRouteProvider_SendRouteBinaryMessage_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_SendRouteBinaryMessage_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    case kMediaRouteProvider_ListenForRouteMessages_Name:
      var params = reader.decodeStruct(MediaRouteProvider_ListenForRouteMessages_Params);
      return this.listenForRouteMessages(params.route_id).then(function(response) {
        var responseParams =
            new MediaRouteProvider_ListenForRouteMessages_ResponseParams();
        responseParams.messages = response.messages;
        responseParams.error = response.error;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouteProvider_ListenForRouteMessages_Name,
            codec.align(MediaRouteProvider_ListenForRouteMessages_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouteProvider_ListenForRouteMessages_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateMediaRouteProviderRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kMediaRouteProvider_CreateRoute_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_CreateRoute_Params;
      break;
      case kMediaRouteProvider_JoinRoute_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_JoinRoute_Params;
      break;
      case kMediaRouteProvider_ConnectRouteByRouteId_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_ConnectRouteByRouteId_Params;
      break;
      case kMediaRouteProvider_TerminateRoute_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_TerminateRoute_Params;
      break;
      case kMediaRouteProvider_SendRouteMessage_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_SendRouteMessage_Params;
      break;
      case kMediaRouteProvider_SendRouteBinaryMessage_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_SendRouteBinaryMessage_Params;
      break;
      case kMediaRouteProvider_StartObservingMediaSinks_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_StartObservingMediaSinks_Params;
      break;
      case kMediaRouteProvider_StopObservingMediaSinks_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_StopObservingMediaSinks_Params;
      break;
      case kMediaRouteProvider_StartObservingMediaRoutes_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_StartObservingMediaRoutes_Params;
      break;
      case kMediaRouteProvider_StopObservingMediaRoutes_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_StopObservingMediaRoutes_Params;
      break;
      case kMediaRouteProvider_ListenForRouteMessages_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouteProvider_ListenForRouteMessages_Params;
      break;
      case kMediaRouteProvider_StopListeningForRouteMessages_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_StopListeningForRouteMessages_Params;
      break;
      case kMediaRouteProvider_DetachRoute_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_DetachRoute_Params;
      break;
      case kMediaRouteProvider_EnableMdnsDiscovery_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_EnableMdnsDiscovery_Params;
      break;
      case kMediaRouteProvider_UpdateMediaSinks_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouteProvider_UpdateMediaSinks_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateMediaRouteProviderResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kMediaRouteProvider_CreateRoute_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_CreateRoute_ResponseParams;
        break;
      case kMediaRouteProvider_JoinRoute_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_JoinRoute_ResponseParams;
        break;
      case kMediaRouteProvider_ConnectRouteByRouteId_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_ConnectRouteByRouteId_ResponseParams;
        break;
      case kMediaRouteProvider_SendRouteMessage_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_SendRouteMessage_ResponseParams;
        break;
      case kMediaRouteProvider_SendRouteBinaryMessage_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_SendRouteBinaryMessage_ResponseParams;
        break;
      case kMediaRouteProvider_ListenForRouteMessages_Name:
        if (message.isResponse())
          paramsClass = MediaRouteProvider_ListenForRouteMessages_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var MediaRouteProvider = {
    name: 'media_router::interfaces::MediaRouteProvider',
    proxyClass: MediaRouteProviderProxy,
    stubClass: MediaRouteProviderStub,
    validateRequest: validateMediaRouteProviderRequest,
    validateResponse: validateMediaRouteProviderResponse,
  };
  MediaRouteProviderStub.prototype.validator = validateMediaRouteProviderRequest;
  MediaRouteProviderProxy.prototype.validator = validateMediaRouteProviderResponse;
  var kMediaRouter_RegisterMediaRouteProvider_Name = 0;
  var kMediaRouter_OnSinksReceived_Name = 1;
  var kMediaRouter_OnIssue_Name = 2;
  var kMediaRouter_OnRoutesUpdated_Name = 3;
  var kMediaRouter_OnSinkAvailabilityUpdated_Name = 4;
  var kMediaRouter_OnPresentationConnectionStateChanged_Name = 5;
  var kMediaRouter_OnPresentationConnectionClosed_Name = 6;

  function MediaRouterProxy(receiver) {
    bindings.ProxyBase.call(this, receiver);
  }
  MediaRouterProxy.prototype = Object.create(bindings.ProxyBase.prototype);
  MediaRouterProxy.prototype.registerMediaRouteProvider = function(media_router_provider) {
    var params = new MediaRouter_RegisterMediaRouteProvider_Params();
    params.media_router_provider = core.isHandle(media_router_provider) ? media_router_provider : connection.bindImpl(media_router_provider, MediaRouteProvider);
    return new Promise(function(resolve, reject) {
      var builder = new codec.MessageWithRequestIDBuilder(
          kMediaRouter_RegisterMediaRouteProvider_Name,
          codec.align(MediaRouter_RegisterMediaRouteProvider_Params.encodedSize),
          codec.kMessageExpectsResponse, 0);
      builder.encodeStruct(MediaRouter_RegisterMediaRouteProvider_Params, params);
      var message = builder.finish();
      this.receiver_.acceptAndExpectResponse(message).then(function(message) {
        var reader = new codec.MessageReader(message);
        var responseParams =
            reader.decodeStruct(MediaRouter_RegisterMediaRouteProvider_ResponseParams);
        resolve(responseParams);
      }).catch(function(result) {
        reject(Error("Connection error: " + result));
      });
    }.bind(this));
  };
  MediaRouterProxy.prototype.onSinksReceived = function(media_source, sinks, origins) {
    var params = new MediaRouter_OnSinksReceived_Params();
    params.media_source = media_source;
    params.sinks = sinks;
    params.origins = origins;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnSinksReceived_Name,
        codec.align(MediaRouter_OnSinksReceived_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnSinksReceived_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouterProxy.prototype.onIssue = function(issue) {
    var params = new MediaRouter_OnIssue_Params();
    params.issue = issue;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnIssue_Name,
        codec.align(MediaRouter_OnIssue_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnIssue_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouterProxy.prototype.onRoutesUpdated = function(routes, media_source, joinable_route_ids) {
    var params = new MediaRouter_OnRoutesUpdated_Params();
    params.routes = routes;
    params.media_source = media_source;
    params.joinable_route_ids = joinable_route_ids;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnRoutesUpdated_Name,
        codec.align(MediaRouter_OnRoutesUpdated_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnRoutesUpdated_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouterProxy.prototype.onSinkAvailabilityUpdated = function(availability) {
    var params = new MediaRouter_OnSinkAvailabilityUpdated_Params();
    params.availability = availability;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnSinkAvailabilityUpdated_Name,
        codec.align(MediaRouter_OnSinkAvailabilityUpdated_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnSinkAvailabilityUpdated_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouterProxy.prototype.onPresentationConnectionStateChanged = function(route_id, state) {
    var params = new MediaRouter_OnPresentationConnectionStateChanged_Params();
    params.route_id = route_id;
    params.state = state;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnPresentationConnectionStateChanged_Name,
        codec.align(MediaRouter_OnPresentationConnectionStateChanged_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnPresentationConnectionStateChanged_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };
  MediaRouterProxy.prototype.onPresentationConnectionClosed = function(route_id, reason, message) {
    var params = new MediaRouter_OnPresentationConnectionClosed_Params();
    params.route_id = route_id;
    params.reason = reason;
    params.message = message;
    var builder = new codec.MessageBuilder(
        kMediaRouter_OnPresentationConnectionClosed_Name,
        codec.align(MediaRouter_OnPresentationConnectionClosed_Params.encodedSize));
    builder.encodeStruct(MediaRouter_OnPresentationConnectionClosed_Params, params);
    var message = builder.finish();
    this.receiver_.accept(message);
  };

  function MediaRouterStub(delegate) {
    bindings.StubBase.call(this, delegate);
  }
  MediaRouterStub.prototype = Object.create(bindings.StubBase.prototype);
  MediaRouterStub.prototype.registerMediaRouteProvider = function(media_router_provider) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.registerMediaRouteProvider && bindings.StubBindings(this).delegate.registerMediaRouteProvider(connection.bindHandleToProxy(media_router_provider, MediaRouteProvider));
  }
  MediaRouterStub.prototype.onSinksReceived = function(media_source, sinks, origins) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onSinksReceived && bindings.StubBindings(this).delegate.onSinksReceived(media_source, sinks, origins);
  }
  MediaRouterStub.prototype.onIssue = function(issue) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onIssue && bindings.StubBindings(this).delegate.onIssue(issue);
  }
  MediaRouterStub.prototype.onRoutesUpdated = function(routes, media_source, joinable_route_ids) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onRoutesUpdated && bindings.StubBindings(this).delegate.onRoutesUpdated(routes, media_source, joinable_route_ids);
  }
  MediaRouterStub.prototype.onSinkAvailabilityUpdated = function(availability) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onSinkAvailabilityUpdated && bindings.StubBindings(this).delegate.onSinkAvailabilityUpdated(availability);
  }
  MediaRouterStub.prototype.onPresentationConnectionStateChanged = function(route_id, state) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onPresentationConnectionStateChanged && bindings.StubBindings(this).delegate.onPresentationConnectionStateChanged(route_id, state);
  }
  MediaRouterStub.prototype.onPresentationConnectionClosed = function(route_id, reason, message) {
    return bindings.StubBindings(this).delegate && bindings.StubBindings(this).delegate.onPresentationConnectionClosed && bindings.StubBindings(this).delegate.onPresentationConnectionClosed(route_id, reason, message);
  }

  MediaRouterStub.prototype.accept = function(message) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kMediaRouter_OnSinksReceived_Name:
      var params = reader.decodeStruct(MediaRouter_OnSinksReceived_Params);
      this.onSinksReceived(params.media_source, params.sinks, params.origins);
      return true;
    case kMediaRouter_OnIssue_Name:
      var params = reader.decodeStruct(MediaRouter_OnIssue_Params);
      this.onIssue(params.issue);
      return true;
    case kMediaRouter_OnRoutesUpdated_Name:
      var params = reader.decodeStruct(MediaRouter_OnRoutesUpdated_Params);
      this.onRoutesUpdated(params.routes, params.media_source, params.joinable_route_ids);
      return true;
    case kMediaRouter_OnSinkAvailabilityUpdated_Name:
      var params = reader.decodeStruct(MediaRouter_OnSinkAvailabilityUpdated_Params);
      this.onSinkAvailabilityUpdated(params.availability);
      return true;
    case kMediaRouter_OnPresentationConnectionStateChanged_Name:
      var params = reader.decodeStruct(MediaRouter_OnPresentationConnectionStateChanged_Params);
      this.onPresentationConnectionStateChanged(params.route_id, params.state);
      return true;
    case kMediaRouter_OnPresentationConnectionClosed_Name:
      var params = reader.decodeStruct(MediaRouter_OnPresentationConnectionClosed_Params);
      this.onPresentationConnectionClosed(params.route_id, params.reason, params.message);
      return true;
    default:
      return false;
    }
  };

  MediaRouterStub.prototype.acceptWithResponder =
      function(message, responder) {
    var reader = new codec.MessageReader(message);
    switch (reader.messageName) {
    case kMediaRouter_RegisterMediaRouteProvider_Name:
      var params = reader.decodeStruct(MediaRouter_RegisterMediaRouteProvider_Params);
      return this.registerMediaRouteProvider(params.media_router_provider).then(function(response) {
        var responseParams =
            new MediaRouter_RegisterMediaRouteProvider_ResponseParams();
        responseParams.instance_id = response.instance_id;
        var builder = new codec.MessageWithRequestIDBuilder(
            kMediaRouter_RegisterMediaRouteProvider_Name,
            codec.align(MediaRouter_RegisterMediaRouteProvider_ResponseParams.encodedSize),
            codec.kMessageIsResponse, reader.requestID);
        builder.encodeStruct(MediaRouter_RegisterMediaRouteProvider_ResponseParams,
                             responseParams);
        var message = builder.finish();
        responder.accept(message);
      });
    default:
      return Promise.reject(Error("Unhandled message: " + reader.messageName));
    }
  };

  function validateMediaRouterRequest(messageValidator) {
    var message = messageValidator.message;
    var paramsClass = null;
    switch (message.getName()) {
      case kMediaRouter_RegisterMediaRouteProvider_Name:
        if (message.expectsResponse())
          paramsClass = MediaRouter_RegisterMediaRouteProvider_Params;
      break;
      case kMediaRouter_OnSinksReceived_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnSinksReceived_Params;
      break;
      case kMediaRouter_OnIssue_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnIssue_Params;
      break;
      case kMediaRouter_OnRoutesUpdated_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnRoutesUpdated_Params;
      break;
      case kMediaRouter_OnSinkAvailabilityUpdated_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnSinkAvailabilityUpdated_Params;
      break;
      case kMediaRouter_OnPresentationConnectionStateChanged_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnPresentationConnectionStateChanged_Params;
      break;
      case kMediaRouter_OnPresentationConnectionClosed_Name:
        if (!message.expectsResponse() && !message.isResponse())
          paramsClass = MediaRouter_OnPresentationConnectionClosed_Params;
      break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  function validateMediaRouterResponse(messageValidator) {
   var message = messageValidator.message;
   var paramsClass = null;
   switch (message.getName()) {
      case kMediaRouter_RegisterMediaRouteProvider_Name:
        if (message.isResponse())
          paramsClass = MediaRouter_RegisterMediaRouteProvider_ResponseParams;
        break;
    }
    if (paramsClass === null)
      return validator.validationError.NONE;
    return paramsClass.validate(messageValidator, messageValidator.message.getHeaderNumBytes());
  }

  var MediaRouter = {
    name: 'media_router::interfaces::MediaRouter',
    proxyClass: MediaRouterProxy,
    stubClass: MediaRouterStub,
    validateRequest: validateMediaRouterRequest,
    validateResponse: validateMediaRouterResponse,
  };
  MediaRouter.SinkAvailability = {};
  MediaRouter.SinkAvailability.UNAVAILABLE = 0;
  MediaRouter.SinkAvailability.PER_SOURCE = MediaRouter.SinkAvailability.UNAVAILABLE + 1;
  MediaRouter.SinkAvailability.AVAILABLE = MediaRouter.SinkAvailability.PER_SOURCE + 1;
  MediaRouter.PresentationConnectionState = {};
  MediaRouter.PresentationConnectionState.CONNECTING = 0;
  MediaRouter.PresentationConnectionState.CONNECTED = MediaRouter.PresentationConnectionState.CONNECTING + 1;
  MediaRouter.PresentationConnectionState.CLOSED = MediaRouter.PresentationConnectionState.CONNECTED + 1;
  MediaRouter.PresentationConnectionState.TERMINATED = MediaRouter.PresentationConnectionState.CLOSED + 1;
  MediaRouter.PresentationConnectionCloseReason = {};
  MediaRouter.PresentationConnectionCloseReason.CONNECTION_ERROR = 0;
  MediaRouter.PresentationConnectionCloseReason.CLOSED = MediaRouter.PresentationConnectionCloseReason.CONNECTION_ERROR + 1;
  MediaRouter.PresentationConnectionCloseReason.WENT_AWAY = MediaRouter.PresentationConnectionCloseReason.CLOSED + 1;
  MediaRouterStub.prototype.validator = validateMediaRouterRequest;
  MediaRouterProxy.prototype.validator = validateMediaRouterResponse;

  var exports = {};
  exports.RouteRequestResultCode = RouteRequestResultCode;
  exports.MediaSink = MediaSink;
  exports.MediaRoute = MediaRoute;
  exports.Issue = Issue;
  exports.RouteMessage = RouteMessage;
  exports.MediaRouteProvider = MediaRouteProvider;
  exports.MediaRouter = MediaRouter;

  return exports;
});// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var mediaRouter;

define('media_router_bindings', [
    'mojo/public/js/bindings',
    'mojo/public/js/core',
    'content/public/renderer/frame_service_registry',
    'chrome/browser/media/router/mojo/media_router.mojom',
    'extensions/common/mojo/keep_alive.mojom',
    'mojo/public/js/connection',
    'mojo/public/js/router',
], function(bindings,
            core,
            serviceProvider,
            mediaRouterMojom,
            keepAliveMojom,
            connector,
            routerModule) {
  'use strict';

  /**
   * Converts a media sink to a MediaSink Mojo object.
   * @param {!MediaSink} sink A media sink.
   * @return {!mediaRouterMojom.MediaSink} A Mojo MediaSink object.
   */
  function sinkToMojo_(sink) {
    return new mediaRouterMojom.MediaSink({
      'name': sink.friendlyName,
      'description': sink.description,
      'domain': sink.domain,
      'sink_id': sink.id,
      'icon_type': sinkIconTypeToMojo(sink.iconType),
    });
  }

  /**
   * Converts a media sink's icon type to a MediaSink.IconType Mojo object.
   * @param {!MediaSink.IconType} type A media sink's icon type.
   * @return {!mediaRouterMojom.MediaSink.IconType} A Mojo MediaSink.IconType
   *     object.
   */
  function sinkIconTypeToMojo(type) {
    switch (type) {
      case 'cast':
        return mediaRouterMojom.MediaSink.IconType.CAST;
      case 'cast_audio':
        return mediaRouterMojom.MediaSink.IconType.CAST_AUDIO;
      case 'cast_audio_group':
        return mediaRouterMojom.MediaSink.IconType.CAST_AUDIO_GROUP;
      case 'generic':
        return mediaRouterMojom.MediaSink.IconType.GENERIC;
      case 'hangout':
        return mediaRouterMojom.MediaSink.IconType.HANGOUT;
      default:
        console.error('Unknown sink icon type : ' + type);
        return mediaRouterMojom.MediaSink.IconType.GENERIC;
    }
  }

  /**
   * Returns a Mojo MediaRoute object given a MediaRoute and a
   * media sink name.
   * @param {!MediaRoute} route
   * @return {!mojo.MediaRoute}
   */
  function routeToMojo_(route) {
    return new mediaRouterMojom.MediaRoute({
      'media_route_id': route.id,
      'media_source': route.mediaSource,
      'media_sink_id': route.sinkId,
      'description': route.description,
      'icon_url': route.iconUrl,
      'is_local': route.isLocal,
      'custom_controller_path': route.customControllerPath,
      // Begin newly added properties, followed by the milestone they were
      // added.  The guard should be safe to remove N+2 milestones later.
      'for_display': route.forDisplay, // M47
      'off_the_record': !!route.offTheRecord  // M50
    });
  }

  /**
   * Converts a route message to a RouteMessage Mojo object.
   * @param {!RouteMessage} message
   * @return {!mediaRouterMojom.RouteMessage} A Mojo RouteMessage object.
   */
  function messageToMojo_(message) {
    if ("string" == typeof message.message) {
      return new mediaRouterMojom.RouteMessage({
        'type': mediaRouterMojom.RouteMessage.Type.TEXT,
        'message': message.message,
      });
    } else {
      return new mediaRouterMojom.RouteMessage({
        'type': mediaRouterMojom.RouteMessage.Type.BINARY,
        'data': message.message,
      });
    }
  }

  /**
   * Converts presentation connection state to Mojo enum value.
   * @param {!string} state
   * @return {!mediaRouterMojom.MediaRouter.PresentationConnectionState}
   */
  function presentationConnectionStateToMojo_(state) {
    var PresentationConnectionState =
        mediaRouterMojom.MediaRouter.PresentationConnectionState;
    switch (state) {
      case 'connecting':
        return PresentationConnectionState.CONNECTING;
      case 'connected':
        return PresentationConnectionState.CONNECTED;
      case 'closed':
        return PresentationConnectionState.CLOSED;
      case 'terminated':
        return PresentationConnectionState.TERMINATED;
      default:
        console.error('Unknown presentation connection state: ' + state);
        return PresentationConnectionState.TERMINATED;
    }
  }

  /**
   * Converts presentation connection close reason to Mojo enum value.
   * @param {!string} reason
   * @return {!mediaRouterMojom.MediaRouter.PresentationConnectionCloseReason}
   */
  function presentationConnectionCloseReasonToMojo_(reason) {
    var PresentationConnectionCloseReason =
        mediaRouterMojom.MediaRouter.PresentationConnectionCloseReason;
    switch (reason) {
      case 'error':
        return PresentationConnectionCloseReason.CONNECTION_ERROR;
      case 'closed':
        return PresentationConnectionCloseReason.CLOSED;
      case 'went_away':
        return PresentationConnectionCloseReason.WENT_AWAY;
      default:
        console.error('Unknown presentation connection close reason : ' +
            reason);
        return PresentationConnectionCloseReason.CONNECTION_ERROR;
    }
  }

  /**
   * Parses the given route request Error object and converts it to the
   * corresponding result code.
   * @param {!Error} error
   * @return {!mediaRouterMojom.RouteRequestResultCode}
   */
  function getRouteRequestResultCode_(error) {
    if (error.message.startsWith('timeout'))
      return mediaRouterMojom.RouteRequestResultCode.TIMED_OUT;
    else
      return mediaRouterMojom.RouteRequestResultCode.UNKNOWN_ERROR;
  }

  /**
   * Creates and returns a successful route response from given route.
   * @param {!MediaRoute} route
   * @return {!Object}
   */
  function toSuccessRouteResponse_(route) {
    return {
        route: routeToMojo_(route),
        result_code: mediaRouterMojom.RouteRequestResultCode.OK
    };
  }

  /**
   * Creates and returns a error route response from given Error object
   * @param {!Error} error
   * @return {!Object}
   */
  function toErrorRouteResponse_(error) {
    return {
        error_text: 'Error creating route: ' + error.message,
        result_code: getRouteRequestResultCode_(error)
    };
  }

  /**
   * Creates a new MediaRouter.
   * Converts a route struct to its Mojo form.
   * @param {!MediaRouterService} service
   * @constructor
   */
  function MediaRouter(service) {
    /**
     * The Mojo service proxy. Allows extension code to call methods that reside
     * in the browser.
     * @type {!MediaRouterService}
     */
    this.service_ = service;

    /**
     * The provider manager service delegate. Its methods are called by the
     * browser-resident Mojo service.
     * @type {!MediaRouter}
     */
    this.mrpm_ = new MediaRouteProvider(this);

    /**
     * The message pipe that connects the Media Router to mrpm_ across
     * browser/renderer IPC boundaries. Object must remain in scope for the
     * lifetime of the connection to prevent the connection from closing
     * automatically.
     * @type {!mojo.MessagePipe}
     */
    this.pipe_ = core.createMessagePipe();

    /**
     * Handle to a KeepAlive service object, which prevents the extension from
     * being suspended as long as it remains in scope.
     * @type {boolean}
     */
    this.keepAlive_ = null;

    /**
     * The stub used to bind the service delegate to the Mojo interface.
     * Object must remain in scope for the lifetime of the connection to
     * prevent the connection from closing automatically.
     * @type {!mojom.MediaRouter}
     */
    this.mediaRouteProviderStub_ = connector.bindHandleToStub(
        this.pipe_.handle0, mediaRouterMojom.MediaRouteProvider);

    // Link mediaRouteProviderStub_ to the provider manager delegate.
    bindings.StubBindings(this.mediaRouteProviderStub_).delegate = this.mrpm_;
  }

  /**
   * Registers the Media Router Provider Manager with the Media Router.
   * @return {!Promise<string>} Instance ID for the Media Router.
   */
  MediaRouter.prototype.start = function() {
    return this.service_.registerMediaRouteProvider(this.pipe_.handle1).then(
        function(result) {
          return result.instance_id;
        }.bind(this));
  }

  /**
   * Sets the service delegate methods.
   * @param {Object} handlers
   */
  MediaRouter.prototype.setHandlers = function(handlers) {
    this.mrpm_.setHandlers(handlers);
  }

  /**
   * The keep alive status.
   * @return {boolean}
   */
  MediaRouter.prototype.getKeepAlive = function() {
    return this.keepAlive_ != null;
  };

  /**
   * Called by the provider manager when a sink list for a given source is
   * updated.
   * @param {!string} sourceUrn
   * @param {!Array<!MediaSink>} sinks
   * @param {Array<string>=} opt_origins
   */
  MediaRouter.prototype.onSinksReceived = function(sourceUrn, sinks,
      opt_origins) {
    // TODO(imcheng): Make origins required in M52+.
    this.service_.onSinksReceived(sourceUrn, sinks.map(sinkToMojo_),
        opt_origins || []);
  };

  /**
   * Called by the provider manager to keep the extension from suspending
   * if it enters a state where suspension is undesirable (e.g. there is an
   * active MediaRoute.)
   * If keepAlive is true, the extension is kept alive.
   * If keepAlive is false, the extension is allowed to suspend.
   * @param {boolean} keepAlive
   */
  MediaRouter.prototype.setKeepAlive = function(keepAlive) {
    if (keepAlive === false && this.keepAlive_) {
      this.keepAlive_.close();
      this.keepAlive_ = null;
    } else if (keepAlive === true && !this.keepAlive_) {
      this.keepAlive_ = new routerModule.Router(
          serviceProvider.connectToService(
              keepAliveMojom.KeepAlive.name));
    }
  };

  /**
   * Called by the provider manager to send an issue from a media route
   * provider to the Media Router, to show the user.
   * @param {!Object} issue The issue object.
   */
  MediaRouter.prototype.onIssue = function(issue) {
    function issueSeverityToMojo_(severity) {
      switch (severity) {
        case 'fatal':
          return mediaRouterMojom.Issue.Severity.FATAL;
        case 'warning':
          return mediaRouterMojom.Issue.Severity.WARNING;
        case 'notification':
          return mediaRouterMojom.Issue.Severity.NOTIFICATION;
        default:
          console.error('Unknown issue severity: ' + severity);
          return mediaRouterMojom.Issue.Severity.NOTIFICATION;
      }
    }

    function issueActionToMojo_(action) {
      switch (action) {
        case 'ok':
          return mediaRouterMojom.Issue.ActionType.OK;
        case 'cancel':
          return mediaRouterMojom.Issue.ActionType.CANCEL;
        case 'dismiss':
          return mediaRouterMojom.Issue.ActionType.DISMISS;
        case 'learn_more':
          return mediaRouterMojom.Issue.ActionType.LEARN_MORE;
        default:
          console.error('Unknown issue action type : ' + action);
          return mediaRouterMojom.Issue.ActionType.OK;
      }
    }

    var secondaryActions = (issue.secondaryActions || []).map(function(e) {
      return issueActionToMojo_(e);
    });
    this.service_.onIssue(new mediaRouterMojom.Issue({
      'route_id': issue.routeId,
      'severity': issueSeverityToMojo_(issue.severity),
      'title': issue.title,
      'message': issue.message,
      'default_action': issueActionToMojo_(issue.defaultAction),
      'secondary_actions': secondaryActions,
      'help_url': issue.helpUrl,
      'is_blocking': issue.isBlocking
    }));
  };

  /**
   * Called by the provider manager when the set of active routes
   * has been updated.
   * @param {!Array<MediaRoute>} routes The active set of media routes.
   * @param {string=} opt_sourceUrn The sourceUrn associated with this route
   *     query. This parameter is optional and can be empty.
   * @param {Array<string>=} opt_joinableRouteIds The active set of joinable
   *     media routes. This parameter is optional and can be empty.
   */
  MediaRouter.prototype.onRoutesUpdated =
      function(routes, opt_sourceUrn, opt_joinableRouteIds) {
    // TODO(boetger): This check allows backward compatibility with the Cast SDK
    // and can be removed when the Cast SDK is updated.
    if (typeof(opt_sourceUrn) != 'string') {
      opt_sourceUrn = '';
    }

    this.service_.onRoutesUpdated(
        routes.map(routeToMojo_),
        opt_sourceUrn || '',
        opt_joinableRouteIds || []);
  };

  /**
   * Called by the provider manager when sink availability has been updated.
   * @param {!mediaRouterMojom.MediaRouter.SinkAvailability} availability
   *     The new sink availability.
   */
  MediaRouter.prototype.onSinkAvailabilityUpdated = function(availability) {
    this.service_.onSinkAvailabilityUpdated(availability);
  };

  /**
   * Called by the provider manager when the state of a presentation connected
   * to a route has changed.
   * @param {string} routeId
   * @param {string} state
   */
  MediaRouter.prototype.onPresentationConnectionStateChanged =
      function(routeId, state) {
    this.service_.onPresentationConnectionStateChanged(
        routeId, presentationConnectionStateToMojo_(state));
  };

  /**
   * Called by the provider manager when the state of a presentation connected
   * to a route has closed.
   * @param {string} routeId
   * @param {string} reason
   * @param {string} message
   */
  MediaRouter.prototype.onPresentationConnectionClosed =
      function(routeId, reason, message) {
    this.service_.onPresentationConnectionClosed(
        routeId, presentationConnectionCloseReasonToMojo_(reason), message);
  };

  /**
   * Object containing callbacks set by the provider manager.
   *
   * @constructor
   * @struct
   */
  function MediaRouterHandlers() {
    /**
     * @type {function(!string, !string, !string, !string, !number}
     */
    this.createRoute = null;

    /**
     * @type {function(!string, !string, !string, !number)}
     */
    this.joinRoute = null;

    /**
     * @type {function(string)}
     */
    this.terminateRoute = null;

    /**
     * @type {function(string)}
     */
    this.startObservingMediaSinks = null;

    /**
     * @type {function(string)}
     */
    this.stopObservingMediaSinks = null;

    /**
     * @type {function(string, string): Promise}
     */
    this.sendRouteMessage = null;

    /**
     * @type {function(string, Uint8Array): Promise}
     */
    this.sendRouteBinaryMessage = null;

    /**
     * @type {function(string):
     *     Promise.<{messages: Array.<RouteMessage>, error: boolean}>}
     */
    this.listenForRouteMessages = null;

    /**
     * @type {function(string)}
     */
    this.stopListeningForRouteMessages = null;

    /**
     * @type {function(string)}
     */
    this.detachRoute = null;

    /**
     * @type {function()}
     */
    this.startObservingMediaRoutes = null;

    /**
     * @type {function()}
     */
    this.stopObservingMediaRoutes = null;

    /**
     * @type {function()}
     */
    this.connectRouteByRouteId = null;

    /**
     * @type {function()}
     */
    this.enableMdnsDiscovery = null;

    /**
     * @type {function()}
     */
    this.updateMediaSinks = null;
  };

  /**
   * Routes calls from Media Router to the provider manager extension.
   * Registered with the MediaRouter stub.
   * @param {!MediaRouter} MediaRouter proxy to call into the
   * Media Router mojo interface.
   * @constructor
   */
  function MediaRouteProvider(mediaRouter) {
    mediaRouterMojom.MediaRouteProvider.stubClass.call(this);

    /**
     * Object containing JS callbacks into Provider Manager code.
     * @type {!MediaRouterHandlers}
     */
    this.handlers_ = new MediaRouterHandlers();

    /**
     * Proxy class to the browser's Media Router Mojo service.
     * @type {!MediaRouter}
     */
    this.mediaRouter_ = mediaRouter;
  }
  MediaRouteProvider.prototype = Object.create(
      mediaRouterMojom.MediaRouteProvider.stubClass.prototype);

  /*
   * Sets the callback handler used to invoke methods in the provider manager.
   *
   * @param {!MediaRouterHandlers} handlers
   */
  MediaRouteProvider.prototype.setHandlers = function(handlers) {
    this.handlers_ = handlers;
    var requiredHandlers = [
      'stopObservingMediaRoutes',
      'startObservingMediaRoutes',
      'sendRouteMessage',
      'sendRouteBinaryMessage',
      'listenForRouteMessages',
      'stopListeningForRouteMessages',
      'detachRoute',
      'terminateRoute',
      'joinRoute',
      'createRoute',
      'stopObservingMediaSinks',
      'startObservingMediaRoutes',
      'connectRouteByRouteId',
      'enableMdnsDiscovery',
      'updateMediaSinks',
    ];
    requiredHandlers.forEach(function(nextHandler) {
      if (handlers[nextHandler] === undefined) {
        console.error(nextHandler + ' handler not registered.');
      }
    });
  }

  /**
   * Starts querying for sinks capable of displaying the media source
   * designated by |sourceUrn|.  Results are returned by calling
   * OnSinksReceived.
   * @param {!string} sourceUrn
   */
  MediaRouteProvider.prototype.startObservingMediaSinks =
      function(sourceUrn) {
    this.handlers_.startObservingMediaSinks(sourceUrn);
  };

  /**
   * Stops querying for sinks capable of displaying |sourceUrn|.
   * @param {!string} sourceUrn
   */
  MediaRouteProvider.prototype.stopObservingMediaSinks =
      function(sourceUrn) {
    this.handlers_.stopObservingMediaSinks(sourceUrn);
  };

  /**
   * Requests that |sinkId| render the media referenced by |sourceUrn|. If the
   * request is from the Presentation API, then origin and tabId will
   * be populated.
   * @param {!string} sourceUrn Media source to render.
   * @param {!string} sinkId Media sink ID.
   * @param {!string} presentationId Presentation ID from the site
   *     requesting presentation. TODO(mfoltz): Remove.
   * @param {!string} origin Origin of site requesting presentation.
   * @param {!number} tabId ID of tab requesting presentation.
   * @param {!number} timeoutMillis If positive, the timeout duration for the
   *     request, measured in seconds. Otherwise, the default duration will be
   *     used.
   * @param {!boolean} offTheRecord If true, the route is being requested by
   *     an off the record (incognito) profile.
   * @return {!Promise.<!Object>} A Promise resolving to an object describing
   *     the newly created media route, or rejecting with an error message on
   *     failure.
   */
  MediaRouteProvider.prototype.createRoute =
      function(sourceUrn, sinkId, presentationId, origin, tabId,
          timeoutMillis, offTheRecord) {
    return this.handlers_.createRoute(
        sourceUrn, sinkId, presentationId, origin, tabId, timeoutMillis,
        offTheRecord)
        .then(function(route) {
          return toSuccessRouteResponse_(route);
        },
        function(err) {
          return toErrorRouteResponse_(err);
        });
  };

  /**
   * Handles a request via the Presentation API to join an existing route given
   * by |sourceUrn| and |presentationId|. |origin| and |tabId| are used for
   * validating same-origin/tab scope.
   * @param {!string} sourceUrn Media source to render.
   * @param {!string} presentationId Presentation ID to join.
   * @param {!string} origin Origin of site requesting join.
   * @param {!number} tabId ID of tab requesting join.
   * @param {!number} timeoutMillis If positive, the timeout duration for the
   *     request, measured in seconds. Otherwise, the default duration will be
   *     used.
   * @param {!boolean} offTheRecord If true, the route is being requested by
   *     an off the record (incognito) profile.
   * @return {!Promise.<!Object>} A Promise resolving to an object describing
   *     the newly created media route, or rejecting with an error message on
   *     failure.
   */
  MediaRouteProvider.prototype.joinRoute =
      function(sourceUrn, presentationId, origin, tabId, timeoutMillis,
               offTheRecord) {
    return this.handlers_.joinRoute(
        sourceUrn, presentationId, origin, tabId, timeoutMillis, offTheRecord)
        .then(function(route) {
          return toSuccessRouteResponse_(route);
        },
        function(err) {
          return toErrorRouteResponse_(err);
        });
  };

  /**
   * Handles a request via the Presentation API to join an existing route given
   * by |sourceUrn| and |routeId|. |origin| and |tabId| are used for
   * validating same-origin/tab scope.
   * @param {!string} sourceUrn Media source to render.
   * @param {!string} routeId Route ID to join.
   * @param {!string} presentationId Presentation ID to join.
   * @param {!string} origin Origin of site requesting join.
   * @param {!number} tabId ID of tab requesting join.
   * @param {!number} timeoutMillis If positive, the timeout duration for the
   *     request, measured in seconds. Otherwise, the default duration will be
   *     used.
   * @param {!boolean} offTheRecord If true, the route is being requested by
   *     an off the record (incognito) profile.
   * @return {!Promise.<!Object>} A Promise resolving to an object describing
   *     the newly created media route, or rejecting with an error message on
   *     failure.
   */
  MediaRouteProvider.prototype.connectRouteByRouteId =
      function(sourceUrn, routeId, presentationId, origin, tabId,
               timeoutMillis, offTheRecord) {
    return this.handlers_.connectRouteByRouteId(
        sourceUrn, routeId, presentationId, origin, tabId, timeoutMillis,
        offTheRecord)
        .then(function(route) {
          return toSuccessRouteResponse_(route);
        },
        function(err) {
          return toErrorRouteResponse_(err);
        });
  };

  /**
   * Terminates the route specified by |routeId|.
   * @param {!string} routeId
   */
  MediaRouteProvider.prototype.terminateRoute = function(routeId) {
    this.handlers_.terminateRoute(routeId);
  };

  /**
   * Posts a message to the route designated by |routeId|.
   * @param {!string} routeId
   * @param {!string} message
   * @return {!Promise.<boolean>} Resolved with true if the message was sent,
   *    or false on failure.
   */
  MediaRouteProvider.prototype.sendRouteMessage = function(
      routeId, message) {
    return this.handlers_.sendRouteMessage(routeId, message)
        .then(function() {
          return {'sent': true};
        }, function() {
          return {'sent': false};
        });
  };

  /**
   * Sends a binary message to the route designated by |routeId|.
   * @param {!string} routeId
   * @param {!Uint8Array} data
   * @return {!Promise.<boolean>} Resolved with true if the data was sent,
   *    or false on failure.
   */
  MediaRouteProvider.prototype.sendRouteBinaryMessage = function(
      routeId, data) {
    return this.handlers_.sendRouteBinaryMessage(routeId, data)
        .then(function() {
          return {'sent': true};
        }, function() {
          return {'sent': false};
        });
  };

  /**
   * Listen for next batch of messages from one of the routeIds.
   * @param {!string} routeId
   * @return {!Promise.<{messages: Array.<RouteMessage>, error: boolean}>}
   *     Resolved with a list of messages, and a boolean indicating if an error
   *     occurred.
   */
  MediaRouteProvider.prototype.listenForRouteMessages = function(routeId) {
    return this.handlers_.listenForRouteMessages(routeId)
        .then(function(messages) {
          return {'messages': messages.map(messageToMojo_), 'error': false};
        }, function() {
          return {'messages': [], 'error': true};
        });
  };

  /**
   * If there is an outstanding |listenForRouteMessages| promise for
   * |routeId|, resolve that promise with an empty array.
   * @param {!string} routeId
   */
  MediaRouteProvider.prototype.stopListeningForRouteMessages = function(
      routeId) {
    return this.handlers_.stopListeningForRouteMessages(routeId);
  };

  /**
   * Indicates that the presentation connection that was connected to |routeId|
   * is no longer connected to it.
   * @param {!string} routeId
   */
  MediaRouteProvider.prototype.detachRoute = function(
      routeId) {
    this.handlers_.detachRoute(routeId);
  };

  /**
   * Requests that the provider manager start sending information about active
   * media routes to the Media Router.
   * @param {!string} sourceUrn
   */
  MediaRouteProvider.prototype.startObservingMediaRoutes = function(sourceUrn) {
    this.handlers_.startObservingMediaRoutes(sourceUrn);
  };

  /**
   * Requests that the provider manager stop sending information about active
   * media routes to the Media Router.
   * @param {!string} sourceUrn
   */
  MediaRouteProvider.prototype.stopObservingMediaRoutes = function(sourceUrn) {
    this.handlers_.stopObservingMediaRoutes(sourceUrn);
  };

  /**
   * Enables mDNS device discovery.
   */
  MediaRouteProvider.prototype.enableMdnsDiscovery = function() {
    this.handlers_.enableMdnsDiscovery();
  };

  /**
   * Requests that the provider manager update media sinks.
   * @param {!string} sourceUrn
   */
  MediaRouteProvider.prototype.updateMediaSinks = function(sourceUrn) {
    this.handlers_.updateMediaSinks(sourceUrn);
  };

  mediaRouter = new MediaRouter(connector.bindHandleToProxy(
      serviceProvider.connectToService(
          mediaRouterMojom.MediaRouter.name),
      mediaRouterMojom.MediaRouter));

  return mediaRouter;
});

/*
 * Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 *
 * This stylesheet is used to apply Chrome styles to extension pages that opt in
 * to using them.
 *
 * These styles have been copied from ui/webui/resources/css/chrome_shared.css
 * and ui/webui/resources/css/widgets.css *with CSS class logic removed*, so
 * that it's as close to a user-agent stylesheet as possible.
 *
 * For example, extensions shouldn't be able to set a .link-button class and
 * have it do anything.
 *
 * Other than that, keep this file and chrome_shared.css/widgets.cc in sync as
 * much as possible.
 */

body {
  color: #333;
  cursor: default;
  /* Note that the correct font-family and font-size are set in
   * extension_fonts.css. */
  /* This top margin of 14px matches the top padding on the h1 element on
   * overlays (see the ".overlay .page h1" selector in overlay.css), which
   * every dialogue has.
   *
   * Similarly, the bottom 14px margin matches the bottom padding of the area
   * which hosts the buttons (see the ".overlay .page * .action-area" selector
   * in overlay.css).
   *
   * Both have a padding left/right of 17px.
   *
   * Note that we're putting this here in the Extension content, rather than
   * the WebUI element which contains the content, so that scrollbars in the
   * Extension content don't get a 6px margin, which looks quite odd.
   */
  margin: 14px 17px;
}

p {
  line-height: 1.8em;
}

h1,
h2,
h3 {
  -webkit-user-select: none;
  font-weight: normal;
  /* Makes the vertical size of the text the same for all fonts. */
  line-height: 1;
}

h1 {
  font-size: 1.5em;
}

h2 {
  font-size: 1.3em;
  margin-bottom: 0.4em;
}

h3 {
  color: black;
  font-size: 1.2em;
  margin-bottom: 0.8em;
}

a {
  color: rgb(17, 85, 204);
  text-decoration: underline;
}

a:active {
  color: rgb(5, 37, 119);
}

/* Default state **************************************************************/

:-webkit-any(button,
             input[type='button'],
             input[type='submit']),
select,
input[type='checkbox'],
input[type='radio'] {
  -webkit-appearance: none;
  -webkit-user-select: none;
  background-image: linear-gradient(#ededed, #ededed 38%, #dedede);
  border: 1px solid rgba(0, 0, 0, 0.25);
  border-radius: 2px;
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.08),
      inset 0 1px 2px rgba(255, 255, 255, 0.75);
  color: #444;
  font: inherit;
  margin: 0 1px 0 0;
  outline: none;
  text-shadow: 0 1px 0 rgb(240, 240, 240);
}

:-webkit-any(button,
             input[type='button'],
             input[type='submit']),
select {
  min-height: 2em;
  min-width: 4em;
/* The following platform-specific rule is necessary to get adjacent
   * buttons, text inputs, and so forth to align on their borders while also
   * aligning on the text's baselines. */
  padding-bottom: 1px;
}

:-webkit-any(button,
             input[type='button'],
             input[type='submit']) {
  -webkit-padding-end: 10px;
  -webkit-padding-start: 10px;
}

select {
  -webkit-appearance: none;
  -webkit-padding-end: 20px;
  -webkit-padding-start: 6px;
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      linear-gradient(#ededed, #ededed 38%, #dedede);
  background-position: right center;
  background-repeat: no-repeat;
}

html[dir='rtl'] select {
  background-position: center left;
}

input[type='checkbox'] {
  height: 13px;
  position: relative;
  vertical-align: middle;
  width: 13px;
}

input[type='radio'] {
  /* OVERRIDE */
  border-radius: 100%;
  height: 15px;
  position: relative;
  vertical-align: middle;
  width: 15px;
}

/* TODO(estade): add more types here? */
input[type='number'],
input[type='password'],
input[type='search'],
input[type='text'],
input[type='url'],
input:not([type]),
textarea {
  border: 1px solid #bfbfbf;
  border-radius: 2px;
  box-sizing: border-box;
  color: #444;
  font: inherit;
  margin: 0;
  /* Use min-height to accommodate addditional padding for touch as needed. */
  min-height: 2em;
  padding: 3px;
  outline: none;
/* For better alignment between adjacent buttons and inputs. */
  padding-bottom: 4px;
}

input[type='search'] {
  -webkit-appearance: textfield;
  /* NOTE: Keep a relatively high min-width for this so we don't obscure the end
   * of the default text in relatively spacious languages (i.e. German). */
  min-width: 160px;
}

/* Remove when https://bugs.webkit.org/show_bug.cgi?id=51499 is fixed.
 * TODO(dbeam): are there more types that would benefit from this? */
input[type='search']::-webkit-textfield-decoration-container {
  direction: inherit;
}

/* Checked ********************************************************************/

input[type='checkbox']:checked::before {
  -webkit-user-select: none;
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAALCAQAAAADpb+tAAAAaElEQVR4Xl3PIQoCQQCF4Y8JW42D1bDZ4iVEjDbxFpstYhC7eIVBZHkXFGw734sv/TqDQQ8Xb1udja/I8igeIm7Aygj2IpoKTGZnVRNxAHYi4iPiDlA9xX+aNQDFySziqDN6uSp6y7ofEMwZ05uUZRkAAAAASUVORK5CYII=);
  background-size: 100% 100%;
  content: '';
  display: block;
  height: 100%;
  width: 100%;
}

input[type='radio']:checked::before {
  background-color: #666;
  border-radius: 100%;
  bottom: 3px;
  content: '';
  display: block;
  left: 3px;
  position: absolute;
  right: 3px;
  top: 3px;
}

/* Hover **********************************************************************/

:enabled:hover:-webkit-any(
    select,
    input[type='checkbox'],
    input[type='radio'],
    :-webkit-any(
        button,
        input[type='button'],
        input[type='submit'])) {
  background-image: linear-gradient(#f0f0f0, #f0f0f0 38%, #e0e0e0);
  border-color: rgba(0, 0, 0, 0.3);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.12),
      inset 0 1px 2px rgba(255, 255, 255, 0.95);
  color: black;
}

:enabled:hover:-webkit-any(select) {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      linear-gradient(#f0f0f0, #f0f0f0 38%, #e0e0e0);
}

/* Active *********************************************************************/

:enabled:active:-webkit-any(
    select,
    input[type='checkbox'],
    input[type='radio'],
    :-webkit-any(
        button,
        input[type='button'],
        input[type='submit'])) {
  background-image: linear-gradient(#e7e7e7, #e7e7e7 38%, #d7d7d7);
  box-shadow: none;
  text-shadow: none;
}

:enabled:active:-webkit-any(select) {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      linear-gradient(#e7e7e7, #e7e7e7 38%, #d7d7d7);
}

/* Disabled *******************************************************************/

:disabled:-webkit-any(
    button,
    input[type='button'],
    input[type='submit']),
select:disabled {
  background-image: linear-gradient(#f1f1f1, #f1f1f1 38%, #e6e6e6);
  border-color: rgba(80, 80, 80, 0.2);
  box-shadow: 0 1px 0 rgba(80, 80, 80, 0.08),
      inset 0 1px 2px rgba(255, 255, 255, 0.75);
  color: #aaa;
}

select:disabled {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAASklEQVQY02P4z0AMRGZGMaShwCisyhITG/4jw8RErMr+KyxYiFC0YOF/BeyWIikEKWLA4Ta4QogiPMpACt82QRThVQYUYYWz4BAAGr6Ii6kEPacAAAAASUVORK5CYII=),
      linear-gradient(#f1f1f1, #f1f1f1 38%, #e6e6e6);
}

input:disabled:-webkit-any([type='checkbox'],
                           [type='radio']) {
  opacity: .75;
}

input:disabled:-webkit-any([type='password'],
                           [type='search'],
                           [type='text'],
                           [type='url'],
                           :not([type])) {
  color: #999;
}

/* Focus **********************************************************************/

:enabled:focus:-webkit-any(
    select,
    input[type='checkbox'],
    input[type='number'],
    input[type='password'],
    input[type='radio'],
    input[type='search'],
    input[type='text'],
    input[type='url'],
    input:not([type]),
    :-webkit-any(
         button,
         input[type='button'],
         input[type='submit'])) {
  /* OVERRIDE */
  -webkit-transition: border-color 200ms;
  /* We use border color because it follows the border radius (unlike outline).
   * This is particularly noticeable on mac. */
  border-color: rgb(77, 144, 254);
  outline: none;
}

/* Checkbox/radio helpers ******************************************************
 *
 * .checkbox and .radio classes wrap labels. Checkboxes and radios should use
 * these classes with the markup structure:
 *
 *   <div class="checkbox">
 *     <label>
 *       <input type="checkbox"></input>
 *       <span>
 *     </label>
 *   </div>
 */

:-webkit-any(.checkbox, .radio) label {
  /* Don't expand horizontally: <http://crbug.com/112091>. */
  align-items: center;
  display: inline-flex;
  padding-bottom: 7px;
  padding-top: 7px;
}

:-webkit-any(.checkbox, .radio) label input {
  flex-shrink: 0;
}

:-webkit-any(.checkbox, .radio) label input ~ span {
  -webkit-margin-start: 0.6em;
  /* Make sure long spans wrap at the same horizontal position they start. */
  display: block;
}

:-webkit-any(.checkbox, .radio) label:hover {
  color: black;
}

label > input:disabled:-webkit-any([type='checkbox'], [type='radio']) ~ span {
  color: #999;
}
