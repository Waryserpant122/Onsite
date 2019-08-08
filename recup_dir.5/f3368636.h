/mozilla.org/MPL/2.0/. */

/*
 * Form Autofill field Heuristics RegExp.
 */

/* exported HeuristicsRegExp */

"use strict";

var HeuristicsRegExp = {
  // These regular expressions are from Chromium source codes [1]. Most of them
  // converted to JS format have the same meaning with the original ones except
  // the first line of "address-level1".
  // [1] https://cs.chromium.org/chromium/src/components/autofill/core/browser/autofill_regex_constants.cc
  RULES: {
    // ==== Email ====
    "email": new RegExp(
      "e.?mail" +
      "|courriel" + // fr
      "|