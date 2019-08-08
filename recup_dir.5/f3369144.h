lUtils.jsm/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

"use strict";

var EXPORTED_SYMBOLS = ["FormAutofillUtils", "AddressDataLoader"];

const ADDRESS_METADATA_PATH = "resource://formautofill/addressmetadata/";
const ADDRESS_REFERENCES = "addressReferences.js";
const ADDRESS_REFERENCES_EXT = "addressReferencesExt.js";

const ADDRESSES_COLLECTION_NAME = "addresses";
const CREDITCARDS_COLLECTION_NAME = "creditCards";
const MANAGE_ADDRESSES_KEYWORDS = ["manageAddressesTitle", "addNewAddressTitle"];
const EDIT_ADDRESS_KEYWORDS = [
  "givenName", "additionalName", "familyName", "organization2", "streetAddress",
  "state", "province", "city", "country", "zip", "postalCode", "email", "tel",
];
const MANAGE_CREDITCARDS_KEYWORDS = ["manageCreditCardsTitle", "addNewCreditCardTitle"];
const EDIT_CREDITCARD_KEYWORDS = ["cardNumber", "nameOnCard", "cardExpiresMonth", "cardExpiresYear", "cardNetwork"];
const FIELD_STATES = {
  NORMAL: "NORMAL",
  AUTO_FILLED: "AUTO_FILLED",
  PREVIEW: "PREVIEW",
};
const SECTION_TYPES = {
  ADDRESS: "address",
  CREDIT_CARD: "creditCard",
};

// The maximum length of data to be saved in a single field for preventing DoS
// attacks that fill the user's hard drive(s).
const MAX_FIELD_VALUE_LENGTH = 200;

const {XPCOMUtils} = ChromeUtils.import("resource://gre/modules/XPCOMUtils.jsm");
const {Services} = ChromeUtils.import("resource://gre/modules/Services.jsm");
const {FormAutofill} = ChromeUtils.import("resource://formautofill/FormAutofill.jsm");
ChromeUtils.defineModuleGetter(this, "CreditCard",
  "resource://gre/modules/CreditCard.jsm");

let AddressDataLoader = {
  // Status of address data loading. We'll load all the countries with basic level 1
  // information while requesting conutry information, and set country to true.
  // Level 1 Set is for recording which country's level 1/level 2 data is loaded,
  // since we only load this when getCountryAddressData called with level 1 parameter.
  _dataLoaded: {
    country: false,
    level1: new Set(),
  },

  /**
   * Load address data and extension script into a sandbox from different paths.
   * @param   {string} path
   *          The path for address data and extension script. It could be root of the address
   *          metadata folder(addressmetadata/) or under specific country(addressmetadata/TW/).
   * @returns {object}
   *          A sandbox that contains address data object with properties from extension.
   */
  _loadScripts(path) {
    let sandbox = {};
    let extSandbox = {};

    try {
      sandbox = FormAutofillUtils.loadDataFromScript(path + ADDRESS_REFERENCES);
      extSandbox = FormAutofillUtils.loadDataFromScript(path + ADDRESS_REFERENCES_EXT);
    } catch (e) {
      // Will return only address references if extension loading failed or empty sandbox if
      // address references loading failed.
      return sandbox;
    }

    if (extSandbox.addressDataExt) {
      for (let key in extSandbox.addressDataExt) {
        let addressDataForKey = sandbox.addressData[key];
        if (!addressDataForKey) {
          addressDataForKey = sandbox.addressData[key] = {};
        }

        Object.assign(addressDataForKey, extSandbox.addressDataExt[key]);
      }
    }
    return sandbox;
  },

  /**
   * Convert certain properties' string value into array. We should make sure
   * the cached data is parsed.
   * @param   {object} data Original metadata from addressReferences.
   * @returns {object} parsed metadata with property value that converts to array.
   */
  _parse(data) {
    if (!data) {
      return null;
    }

    const properties = ["languages", "sub_keys", "sub_isoids", "sub_names", "sub_lnames"];
    for (let key of properties) {
      if (!data[key]) {
        continue;
      }
      // No need to normalize data if the value is array already.
      if (Array.isArray(data[key])) {
        return data;
      }

      data[key] = data[key].split("~");
    }
    return data;
  },

  /**
   * We'll cache addressData in the loader once the data loaded from scripts.
   * It'll become the example below after loading addressReferences with extension:
   * addressData: {
   *               "data/US": {"lang": ["en"], ...// Data defined in libaddressinput metadata
   *                           "alternative_names": ... // Data defined in extension }
   *               "data/CA": {} // Other supported country metadata
   *               "data/TW": {} // Other supported country metadata
   *               "data/TW/