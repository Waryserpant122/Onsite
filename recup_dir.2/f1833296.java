found in the LICENSE file.
 */

body {
  font-size: 80%;
}

button {
  display: block;
  font-size: 110%;
  font-weight: bold;
  margin: 10px auto;
  min-height: 48px;
  width: 200px;
}

.radio-button-div {
  margin: 7px auto;
}

.warning {
  color: red;
  font-size: 90%;
  font-weight: normal;
}

#net-export-main {
  margin: 8px;
}

#export-view-file-path-text {
  white-space: pre-wrap;
  word-wrap: break-word;
}
</style>
</head>
<body>
  <h2>Network Log Export</h2>
  <div id="net-export-main">
    <div>
      <button id="export-view-start-data" disabled>
        Start Logging to Disk
        <div class="warning" id="export-view-deletes-log-text" hidden>
          Deletes old log
        </div>
      </button>
    </div>
    <div>
      <button id="export-view-stop-data" disabled>Stop Logging</button>
    </div>
    <div>
      <button id="export-view-send-data" disabled>
        Email Log
        <div class="warning" id="export-view-private-data-text" hidden>
          Log contains private information
        </div>
        <div class="warning" id="export-view-send-old-log-text" hidden>
          Log file from previous session
        </div>
      </button>
    </div>
    <p>
      <b>INSTRUCTIONS</b>: Start logging, reproduce the problem,
      and then stop logging.  Make sure to send the email before
      starting to log again. Otherwise, the log will be deleted.
    </p>
    <p>
      Logs can be loaded in
      <a href="chrome://net-internals" target="_blank">net-internals</a>
      of desktop Chrome.
    </p>
    <p>
      <b><span class="warning">WARNING</span></b>: Logs contain a list of sites
      visited from when logging started to when logging stopped.  They may also
      contain general network configuration information, such as DNS and proxy
      configuration. If private information is not stripped, the logs also
      contain cookies and credentials.
    </p>
    <p>
      <b>ADVANCED</b>:
      <span class="warning">This section should normally be left alone.</span>
      <div class="radio-button-div">
        <label>
          <input id="export-view-strip-private-data-button" type="radio"
                 name="log-mode" value="STRIP_PRIVATE_DATA" checked disabled>
          Strip private information
        </label>
      </div>
      <div class="radio-button-div">
        <label>
          <input id="export-view-include-private-data-button" type="radio"
                 name="log-mode" value="NORMAL" disabled>
          Include cookies and credentials
        </label>
      </div>
      <div class="radio-button-div">
        <label>
          <input id="export-view-log-bytes-button" type="radio"
                 name="log-mode" value="LOG_BYTES" disabled>
          Include raw bytes (will include cookies and credentials)
        </label>
      </div>
    </p>
  </div>
  <pre id="export-view-file-path-text"></pre>
</body>
</html>
// Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Main entry point called once the page has loaded.
 */
function onLoad() {
  NetExportView.getInstance();
}

document.addEventListener('DOMContentLoaded', onLoad);

/**
 * This class handles the presentation of our profiler view. Used as a
 * singleton.
 */
var NetExportView = (function() {
  'use strict';

  /**
   * Delay in milliseconds between updates of certain browser information.
   */
  /** @const */ var POLL_INTERVAL_MS = 5000;

  // --------------------------------------------------------------------------

  /**
   * @constructor
   */
  function NetExportView() {
    $('export-view-start-data').onclick = this.onStartData_.bind(this);
    $('export-view-stop-data').onclick = this.onStopData_.bind(this);
    $('export-view-send-data').onclick = this.onSendData_.bind(this);

    window.setInterval(function() { chrome.send('getExportNetLogInfo'); },
                       POLL_INTERVAL_MS);

    chrome.send('getExportNetLogInfo');
  }

  cr.addSingletonGetter(NetExportView);

  NetExportView.prototype = {
    /**
     * Starts saving NetLog data to a file.
     */
    onStartData_: function() {
      var logMode =
          document.querySelector('input[name="log-mode"]:checked').value;
      chrome.send('startNetLog', [logMode]);
    },

    /**
     * Stops saving NetLog data to a file.
     */
    onStopData_: function() {
      chrome.send('stopNetLog');
    },

    /**
     * Sends NetLog data via email from browser.
     */
    onSendData_: function() {
      chrome.send('sendNetLog');
    },

    /**
     * Updates the UI to reflect the current state. Displays the path name of
     * the file where NetLog data is collected.
     */
    onExportNetLogInfoChanged: function(exportNetLogInfo) {
      if (exportNetLogInfo.file) {
        var message = '';
        if (exportNetLogInfo.state == 'LOGGING')
          message = 'NetLog data is collected in: ';
        else if (exportNetLogInfo.logType != 'NONE')
          message = 'NetLog data to send is in: ';
        $('export-view-file-path-text').textContent =
            message + exportNetLogInfo.file;
      } else {
        $('export-view-file-path-text').textContent = '';
      }

      // Disable all controls.  Useable controls are enabled below.
      var controls = document.querySelectorAll('button, input');
      for (var i = 0; i < controls.length; ++i) {
        controls[i].disabled = true;
      }

      $('export-view-deletes-log-text').hidden = true;
      $('export-view-private-data-text').hidden = true;
      $('export-view-send-old-log-text').hidden = true;
      if (exportNetLogInfo.state == 'NOT_LOGGING') {
        // Allow making a new log.
        $('export-view-strip-private-data-button').disabled = false;
        $('export-view-include-private-data-button').disabled = false;
        $('export-view-log-bytes-button').disabled = false;
        $('export-view-start-data').disabled = false;

        // If there's an existing log, allow sending it.
        if (exportNetLogInfo.logType != 'NONE') {
          $('export-view-deletes-log-text').hidden = false;
          $('export-view-send-data').disabled = false;
          if (exportNetLogInfo.logType == 'UNKNOWN') {
            $('export-view-send-old-log-text').hidden = false;
          } else if (exportNetLogInfo.logType == 'NORMAL') {
            $('export-view-private-data-text').hidden = false;
          }
        }
      } else if (exportNetLogInfo.state == 'LOGGING') {
        // Only possible to stop logging. Radio buttons reflects current state.
        document.querySelector('input[name="log-mode"][value="' +
                               exportNetLogInfo.logType + '"]').checked = true;
        $('export-view-stop-data').disabled = false;
      } else if (exportNetLogInfo.state == 'UNINITIALIZED') {
        $('export-view-file-path-text').textContent =
            'Unable to initialize NetLog data file.';
      }
    }
  };

  return NetExportView;
})();
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0,
                                 maximum-scale=1.0, user-scalable=no">
  <title i18n-content="title"></title>
  <style>/* Copyright 2014 The Chromium Authors. All rights reserved.
   Use of this source code is governed by a BSD-style license that can be
   found in the LICENSE file. */

a {
  color: #585858;
}

.bad-clock .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,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) 1x,
      url(data:image/png;base64,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) 2x);
}

body {
  background-color: #f7f7f7;
  color: #646464;
}

body.safe-browsing {
  background-color: rgb(206, 52, 38);
  color: white;
}

button {
  -webkit-user-select: none;
  background: rgb(66, 133, 244);
  border: 0;
  border-radius: 2px;
  box-sizing: border-box;
  color: #fff;
  cursor: pointer;
  float: right;
  font-size: .875em;
  margin: 0;
  padding: 10px 24px;
  transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

[dir='rtl'] button {
  float: left;
}

button:active {
  background: rgb(50, 102, 213);
  outline: 0;
}

button:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, .50);
}

#debugging {
  display: inline;
  overflow: auto;
}

.debugging-content {
  line-height: 1em;
  margin-bottom: 0;
  margin-top: 1em;
}

.debugging-title {
  font-weight: bold;
}

#details {
  color: #696969;
  margin: 45px 0 50px;
}

#details p:not(:first-of-type) {
  margin-top: 20px;
}

#details-button {
  background: inherit;
  border: 0;
  float: none;
  margin: 0;
  padding: 10px 0;
  text-transform: uppercase;
}

#details-button:hover {
  box-shadow: inherit;
  text-decoration: underline;
}

.error-code {
  color: #696969;
  display: inline;
  font-size: .86667em;
  margin-top: 15px;
  opacity: .5;
  text-transform: uppercase;
}

#error-debugging-info {
  font-size: 0.8em;
}

h1 {
  color: #333;
  font-size: 1.6em;
  font-weight: normal;
  line-height: 1.25em;
  margin-bottom: 16px;
}

h2 {
  font-size: 1.2em;
  font-weight: normal;
}

.hidden {
  display: none;
}

html {
  -webkit-text-size-adjust: 100%;
  font-size: 125%;
}

.icon {
  background-repeat: no-repeat;
  background-size: 100%;
  height: 72px;
  margin: 0 0 40px;
  width: 72px;
}

input[type=checkbox] {
  opacity: 0;
}

input[type=checkbox]:focus ~ .checkbox {
  outline: -webkit-focus-ring-color auto 5px;
}

.interstitial-wrapper {
  box-sizing: border-box;
  font-size: 1em;
  line-height: 1.6em;
  margin: 100px auto 0;
  max-width: 600px;
  width: 100%;
}

#main-message > p {
  display: inline;
}

#extended-reporting-opt-in {
  font-size: .875em;
  margin-top: 39px;
}

#extended-reporting-opt-in label {
  position: relative;
}

.nav-wrapper {
  margin-top: 51px;
}

.nav-wrapper::after {
  clear: both;
  content: '';
  display: table;
  width: 100%;
}

.safe-browsing :-webkit-any(
    a, #details, #details-button, h1, h2, p, .small-link) {
  color: white;
}

.safe-browsing button {
  background-color: rgba(255, 255, 255, .15);
}

.safe-browsing button:active {
  background-color: rgba(255, 255, 255, .25);
}

.safe-browsing button:hover {
  box-shadow: 0 2px 3px rgba(0, 0, 0, .5);
}

.safe-browsing .error-code {
  display: none;
}

.safe-browsing .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAMAAABiM0N1AAACFlBMVEX////19fX////////39/f39/f29vb09PQAAAD8/Pz29vbu7u7t7e3bRDfv7+/r6+vcRTjq6ur09PTy8vL86efp6en8/Pzz8/Pw8PDqUEPj4+Ps7OzdRjnfRzrx8fHdRTjl5eXf39/aQzb7+/ve3t7mTUDSPTDpT0L19fX////gSDvZVEneRjnkSz76+vrm5ubVPzLZQjXTPjHKNirNOCzn1dPrUEPoTkHbVkvOOi3jSj3QOy7RPC/o6Ojd3d3cRDfeRzni4uL39/fqUEL29vb5+fntZlrZxsTPOi3RT0TXVEjcV0zWQDPlYFTWU0f86ejYQjX96ejoYVb14uH96ujhSTzUPjHUUUb14uDq19bk5OTlTD/n5+fMOCva2trm1NLp1tXey8riSTziSj3wfHLZ2dnYxcPrZFnQTkLTUEXnTUDaVUrXQTTKNyruZ1rNOSzQOy/hzsznTkHROy/hSTvYQTThSDvsZlnWUkfeRzrqY1jgzczcRTfnYVXlX1PZVUrdWEzlYFPLNyvfSDrfzMvXU0fkX1LYVEnc3NzkX1Ph4eHNTEHPOy7pdm3oT0Hbycfj0M/POi7YQTXgW0/PTUHVUUbZQzbuZlrodmzl0tDOOS3lTD7LNyrmYVXSY1n76OfeWU3l09HaaV/tZlnsZVnm09HqYlfNS0HcysjUPzLOTEHKNinST0XbaV/o1dTTY1ng4ODrUUPxo4TUAAAAC3RSTlMAABDKAMoAAAAAyh18qQ0AAAPjSURBVHhe7dbjmiRZEAbgHq2SKNuutm3bY9vm2rZt6w43IjE13VFTfXpyf05cwPtExIn8qqoe+5/qIbSmHkJbt2XvW9u2VjEXOvYl6lDJjkMl+w5Kdhwq2XdQsuNQyb6Dkh2HSvYdlOw6VKKOfQkd+xJ12CXq2JHsOyhVdhaPLFLA8aajolTOOdc8TCTHU7n3F8pId6EyzrB3wAvSeicYfKaMVIKo0+yFWis5XgWnEJ5Y3QR0ZMALHf0L0lrnZiEgy98NsUK4IWDQ+mHurvNsMBjoDfcIrccSQ4wQSh97sdJvHZ+x+oGxAnfkntMjR/dFqxkhlL73Ng+kW1qStxyW0xuWBTl/7Oi+7m5fNSuE+06n06dOJXfPO4z9FMKyHGtFJ5GIRqoZIZSut1xKJj8vFr90QD+9gUBYEF4eQScafc23J84MZeeOJ5O7i8X2Q1P6fmRBOG30A06kRoozQ9mZn8A5vLSUez4YCPT0xCynry9S0+X0x5mhrGO+vf3wFf0MoZ88zPUKzNXnQ8cpgcQGofTkoWumg3OhA3P59nQ5FUlRuDgjhNJULof7kc250NmFjqJIkp9zMUP4XnjOsVh+BO4ngXNFsB8nODwvutggdPT7EWJ1s+NvQz+4Zyc42I/EcZzHxbbsl9ABBpz3zkwfLM2lSH4/z3G8iBKFaI4VcD/CR3V1ZyZra2/c1t8LFJzLz3Gi6PGEXASic4EThvvJPz17FaDl+g8vwlxO3A/2Y0Kai0DEgfcK4/2MjE+iU586+aOxH4XndQYdt9v1uFkUsvIHvgv93X+Zrv11OZW63PnHfpBgLr0fkEKhkNutPmoWgUr5A3eI99x98MX6VKrzwujoWJMxFy+KHPTj0dyVISt/BCN/otEXVlKdHW+0tTWeaNLnQgb6cUNpFSArf0rfqe+dlY6Od8+fb3zugwMcj2MBpGkIZTJbzKLQJyR/dtV8erLts8a9XzQ0fGXtR9MhtRL09Tckf5zOb8egn7NnX99pzRUy+skMPmIWhbILEyR/FGn/ib0NDT8f4ETe4xFD+PDYjzrYv90s+MmmUivJH0Vq+u137EcUrT2rbuinf4cFlf0TsfonyR9J+uvvnfohitiOBhAOtmODvyNDCZI/+Jka/Vhrzqj96Gwg/UPyR78fEecyoUHTqSxV+0j+gAOllfaDDoMUIflj3qHmVrGfDDpMUhfJH9HcD0BGP2xSXCL5g5DRj/5ezJKf5A+URhwWieQPPBdxWCSO5I9K7odRWpc/5J5ZJRe3Ln9U8l6skmdN/tD9sEuhUv6Qe96kdG/+PEEddol8Xw8skfx5YEldlz+0/gOZkEIssMdljwAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,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) 2x);
}

.small-link {
  color: #696969;
  font-size: .875em;
}

.ssl .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,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) 1x,
      url(data:image/png;base64,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) 2x);
}

.captive-portal .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAQAAAD/5HvMAAAEyElEQVR4Xu3afWhVZRwH8LvdmjVSjKyQbKazia1k1sL6YzXoZdy28/J8f5XRC7hwEVk0hZQCXY2SipCgYgappXMQ5Ya9ELZqoEM3VHqxBkapm21jOBWmbLfudk+0Peeu2+mc3zk9514WbN9/zz18ds75Pc/vPM+JTP9NZzrT+b9Fn2nGxHrainb8ggEaRgLn0IOjaMFroloU/Ssg1yxBLZpxBD00QufpODrRiFX6IiUKXY8XcRAJsryC3nRL7EpsQJ/r0V3iSS0/MKU4D6uoQwLY/P1q4i0aYX9xVqyPRH1jbr8UtfSbE8CDqIK60wBjOEb7aDf24CBO/gN12LjJF4dM+dPAoNgMdNkUxLENQltAFViJdbTWfFTcaRbiMXyCpE1CM4vR5uAzdwB/hcxCDI5jXqGFeAadNJoGH0ErVhg3YPs46Ad9JldNS3GCAbDPkFGG/eJGWktn3PA4IUB3oxPzGY4AXSBLCSTLgfZxx2MLV1Q5tFHeWxWQDNalHulWeo4q9GW4VVShng5J0KgZ8+bkYpcEqIJk6H2y0CiW4H58gGMYQhyn8DlqUUltZIk1XF01kKUW5yhm3gVB3Q76MDaRyXBQR1aIIJnyi6jNZUgs8uTQfUiGDJIxZ9OPfxEwhAO0F70To5NR5skxrsUgWaGDZLQCHBHVxnKxWjwuivRS7McK7na1kpUZkEwUzXbFiZfZcVk8pE6xZ3u34AV5TFJUsV2ObBHU0+Q5wu0eJ21krw+eD4lzBvOYf7wLeyI5DEfLx2l1DPqoSXK8SItis/gm41mlago/9P2UApklZE0pkHhdAv6YIiB8NwEQL9HvUwCkzZHz15g5Gw8jmR0QtsizvOoACUjAIdlOZQUkHpCgDqd1gwRslhX3djZAmDsBwpATJPtDUZPqGFuyUWU453IWu7fFHZOvhjiQ+aqiDheQ/bJjXpf2oP+caRA+cgPJpkxckeZfiIHMgmirC8gee4rz0qF6KV3IJEi86R8kg0oazQ6Iv2Uyoiajt4x/qJ1BfcCe6JQZC/BQ82XvDLYHJPUEK3tmYHTmlotpbzCSwsDomDpc+mD6VgHETB3M5Or2c5xUBvGTq7P98DjBEjqrBOLbD2eDZj7o+aJdhnhooJxUbd/r1cJ+yl1mJMMBGWUSP6zlezT5SIirGNKacEB4bwKEXcxrEN7gThUGSCtILaZXMC+KiGN+5kHYITndkSj7Ko3GTIPMEvtJFKt9LDYgiZtVQTLnqUPUOJcV8KWE95df4m85ps0+CQ/ig68xN72lscF4yveCFTa5g9AXnDR5lczC1Jr+4UhuCsAv6YlH3EDURFbgPCG3Y2altmPG9FIJ8LnoOYLbXNDzgi+Myvkqii9sIuocAHZZuN9tAKi6hnaiBwka9f942y2rzFcut4tZOO+3r5J6xcVmYEeKc9xlPuC3FhDHynBA6LQ5GGA2ftnNl82RqDrIDgb1ZU4AnxzUTc7raDeWhwPCUW0BA/C7gYeP9cWqILSUX8YAgmxxIoF3tYL/DkK9Y/znwm8CI4lvRLV+dVAQThOpOPht8p+ogZ4W9/jijFFD5eXhWPgPCXjMh/pSVYD6pxaywPGOKOIBWfkYhXYKozgvy5/rYBva8avzcx19sSrgT2jJnjJMVMiUAAAAAElFTkSuQmCC) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJAAAACQCAQAAABNTyozAAAKSUlEQVR4Xu2de2wUxx3Hfxhj5dkSIgOhpICDWilH1AjTGEwdnaCNdbi+2/n+EGlKAvSBopRIKa+KSC5tCU+ZoKaKQ9WSNihqgsXDqRMkHBRBDDi04Q9kGQFG4JAEqF1TIkwSP+BcjBXK7zC3551Ze3zdz/7NnffD3uz8HjND/hAQEBAQEBAQEBAQEBAQEBAQEOAMjeWpOVjF5djFB7gWDWjmNm5DMxq4lg9gF5djlZoTy3OGkmceuzNWoBaiDDu5Dmf4M26/en2GM1yHnShTC2MFj91JNhG+y4lwKVejkTtTv9DI1VzqRMJ3UaoMxlS1Bof5iuunX8FhtQZTaTD1J6EsNU2tRA06uNP7hQ7UkCuxB3kDN/X605t4Q+xB6g8wGWV8njvNXJQUnoH9Op+O/TyD+g5nLEpwXAjwURCK+JCR7ziEIvKf6CRUIC4E+CiIc7DT5LdgJ+eQf8QKuEoI8FdQBi/jL41/05e8jDLIPM50rhYCfBaE+/Cei4DL+Ce2ogy/4We4i2fwG5RhK3/Il5NLwnu4j0yC0dgqBfgvKKmeI/wSopGv0S34/tcRxR/4SDJFZIpwplrCLdzZ54LG9DinalG/Lx5H1+AQP4dNqOSDfIovXb1O8UFUYhM/x6GvRjB+iS/1NP/CGDIDHuU6DQFaY5CaglZxW2d5WdE9RMV3qDn8Op9LKvwcv67mTLmdqOgePI+zQk+rmmLo2eG1Ou8r/beY8+R1AW14PpRFFMlWv0NzqtLxb/w2kk0UysLz3PaVIOdJMkHsfo2pmbHXPFZ1z2A4RPTDb/BG/qLX6r/AK9FRRBzqnk1hFZkAReL/qd8E0SC8iZJwJhGewgXP+v/T9dSEM1GCN2kQaZOBdTo/LfOhhhqOCt3Pxw413FD4iS1CgAWCsNvEN2A36RO9W/wxlggq/uYt0yjH+DW1Ri1Us69eC9Ua/JWPCS1mX+1quAgLrRFEFMvnNnG7cbyDeRh9i2ntPLyTMEi0YSrpgjGo5057BEkw74ab/Ut3hieSjZn8MvbyEW7idm7iI9jLL2NmJPta9ujV/0lVPzHw9Ag9VgiScGl3Zsd5gCh3iPMEasRTIp+vGvWjcKbzAPZ16cF6A2OP+HFZKYgyeLtaQhmUwc/ypylI/5SfDWdiMW+nDO03lxiaLRMk4RA+SF07PuAQaZMhXuyWC8KOXqrfTrpgHXfaLUgWfHo1GBzSLv+gCHHbBUlmjMRHqenBRzNGaoekIuayWJAsAN0Ql7VjL1bgaWcWnsYLeJ/br+u5oF3uCWeKiN1qQRIn3DXHQTN+XXQPCQqH8fJrxag2J0y68FohwHJBEjUblWo4USwfr+Aot3ALjqKsOxUWHcFvq9kGsoWIWyPoEnmgS0SiaFRGR5jKF9bZ8/zwKfIA3u9JNvaSCXipRXo6sYM8oCb0kJK/pCYYKeRwiw1idINJPH6T6sfJBNhmkx6cLb6DPKJeFKJfNFUl7bTpUj8l7wzmPdcF7THUDcTVNunBJtIiko1PuvTgk0i2oRYEm/Twn3KHkCbOI2hFq/MImYGrbJGDj/EUGUHNV/ON9fdYoKYF9diCH4dvI/tAhQWhg704YxEPBCUBJckFBILqA0FJwOTkAgJBZYGgpMUdPh8ISoKallxAIGilbM7HvkCQADVCwKLCYXwsEHTDgiW5Isd5+Nq08V/pJ8h5WK4kSnGxlRMR/6y5u1fPyeVL6SaIBslanxOhVOBSIWjbDQuNOqwWpJ8vLfWQJFML6Dr4eboJUguEoGpKBdnlJzP/WGFaDy70q6AJsjsxpaW2cuBKzOLxa4YFvU/9SO4QOWyksHw4licE1ffwke8aFNSG79oUlMfyyA01V9zA2z02AB82Iucc/s4h6mdkSVrNITewWjxB66kHoqPwcboEElgv7ne1u9FyYXT+Lfv/LqSHIDVf3EF5bysZhcn6btJBEBcKQVW9jMOSLShznkA8DZ6gKWJIqXE3WitG9YcoCfjVwBcUe0jcQS25gQYRnYzVzTzaX70RT1ADuSHDN3Wva0/7WwNbkLo3ITR3Qw69oSxyYcrtfHAgCwplyYmrIUGyZ4JPpJkg/Z+YJDqem9LvJ6Y/SMsI7os0G6T1X/MSFeMr/xeveT4gBOVTyuAXPheIzmEr5xgXlC8EHSA3sEuGGpb147eob/kZamCXTrDqziB+w29FqPQ3WNVNd7i/NPf4rKjJ13THKnejc1wSZq4pW78XLvibMPOQcvWwsuyMrYLcU64aSfvUUd/BxYEhKHcIX5ZJey9lHw+oH3C7HYL0yz7uhUNPqLlWCNIvHLqXnm1rA/Wz9KzRvOAF/NkCQfrNC+7tLxpLgXdaIEi3/cW9gYo8IBb3WyqIF8mEvdcWvH2kQXQEGmwVJFsL1UrvTZw5Woq+zedtFMQ5Urya5r0NeDlpob7HX1ooaLkQdD6U5b2R/ARpgpl8xTZBMpOOMq2lCLF80gS/tEtQLF9qx2TqHXIfcWwmbewShM1Cz3HNeTA6iselk6DicXKuhxL9BXUb00kQ/ij0xJ2x2ksy0RodlS6CoqPQKgRVmFnUuyFdBPEGqTw6yciycHyOMekgCGMSipxV5jYWeIs08G3h+HF+w5mV+jQvsR8lVmBwawoU2SNIXjjJTCmAogTB1WY3NznlfYm//6sYsd5tJ83wbXxKCnKmkx6Jx0BgBXkE//BbkHsdDy9IPdhqfIMldHg4KUCkcn2+OFnDJjrk+IXRpI9aIq3z6cJh5AEnty8E4aQoVMltAU9LmWqJ0U3e9OvjeLcvFDmzqEdQmSCzLpxJAoPbBGKxtyoUWv0XhL9RD2Cx1IM4HiWB2Y0m272N/mp2Hwg63uPbOLGQuZYExrcqxUVMJA/gZ9zudw8RJYCJiaVw7Hf9eelvdovG6HiP+4J86K+ixBZTNEo9aI7dT+7ob5eMkx43GR7kRPhVHMVFvsKX/RU0Y2Ti1BBxl3jA6IbbtUKRJSGI0FObqA/ryAWjW7bjZHS8rYKi43HyJj1bXMIR85v+oxETbRSEiWi8Sc9ut6jfl2MjcNGZbpsgZ3oPTVyHoneTBjoHj7TzIpsE8aKbJxOo1zr1Sf/oGlQWDrND0PWgQujRyIcaO/zotJqiIcjP65DG02Py+Cx0YEX4NtsEYbfO2GP+ALZTKLJJELbov7nMH+H3FsbYIAhxrNOY9/h5CCQ+5w3RUf0rCM26QYXPx4iilTcWj+svQdhvIiT1/SBadGBzLL+vBSHOa00mNPw/yvgEL+ecPhNUJ7OF9jxHS5MfMYF9vMh5mAb5KqiFl4YzyVYwGttSGDq3qQVqQu4Q84KwDaPJDvQP5EcH6vltrDemp1qEzHYTK+jjAwOqYgU00IhOQoX/274jjgrR3zPgdsYvQb1vcupR4oylgQ8mo8xwb8d5lGEypROhLDVNrUSN3nan6ECNWqmmhbIoXQnf5US4lKvR2CsxjVzNpU5ELFhKd5yhsTw1F6u5nKtQw7VoQDO3cRua0cC1qOEqLsdqNTeW5wwla/gvpXzJeo7GTncAAAAASUVORK5CYII=) 2x);
}

.checkbox {
  background: transparent;
  border: 1px solid white;
  border-radius: 2px;
  display: block;
  height: 14px;
  left: 0;
  position: absolute;
  right: 0;
  top: -1px;
  width: 14px;
}

.checkbox::before {
  background: transparent;
  border: 2px solid white;
  border-right-width: 0;
  border-top-width: 0;
  content: '';
  height: 4px;
  left: 2px;
  opacity: 0;
  position: absolute;
  top: 3px;
  transform: rotate(-45deg);
  width: 9px;
}

.ssl-opt-in .checkbox {
  border-color: #696969;
}

.ssl-opt-in .checkbox::before {
  border-color: #696969;
}

input[type=checkbox]:checked ~ .checkbox::before {
  opacity: 1;
}

@media (max-width: 700px) {
  .interstitial-wrapper {
    padding: 0 10%;
  }

  #error-debugging-info {
    overflow: auto;
  }
}

@media (max-height: 600px) {
  .error-code {
    margin-top: 10px;
  }
}

@media (max-width: 420px) {
  button,
  [dir='rtl'] button,
  .small-link {
    float: none;
    font-size: .825em;
    font-weight: 400;
    margin: 0;
    text-transform: uppercase;
    width: 100%;
  }

  #details {
    margin: 20px 0 20px 0;
  }

  #details p:not(:first-of-type) {
    margin-top: 10px;
  }

  #details-button {
    display: block;
    margin-top: 20px;
    text-align: center;
    width: 100%;
  }

  .interstitial-wrapper {
    padding: 0 5%;
  }

  #extended-reporting-opt-in {
    margin-top: 24px;
  }

  .nav-wrapper {
    margin-top: 30px;
  }
}

/**
 * Mobile specific styling.
 * Navigation buttons are anchored to the bottom of the screen.
 * Details message replaces the top content in its own scrollable area.
 */

@media (max-width: 420px) and (max-height: 736px) and (orientation: portrait) {
  #details-button {
    border: 0;
    margin: 8px 0 0;
  }

  .secondary-button {
    -webkit-margin-end: 0;
    margin-top: 16px;
  }
}

/* Fixed nav. */
@media (min-width: 240px) and (max-width: 420px) and
       (min-height: 401px) and (max-height: 736px) and (orientation:portrait),
       (min-width: 421px) and (max-width: 736px) and (min-height: 240px) and
       (max-height: 420px) and (orientation:landscape) {
  body .nav-wrapper {
    background: #f7f7f7;
    bottom: 0;
    box-shadow: 0 -22px 40px rgb(247, 247, 247);
    left: 0;
    margin: 0;
    max-width: 736px;
    padding-left: 24px;
    padding-right: 24px;
    position: fixed;
    z-index: 1;
  }

  body.safe-browsing .nav-wrapper {
    background: rgb(206, 52, 38);
    box-shadow: 0 -22px 40px rgb(206, 52, 38);
  }

  .interstitial-wrapper {
    max-width: 736px;
  }

  #details,
  #main-content {
    padding-bottom: 40px;
  }
}

@media (max-width: 420px) and (max-height: 736px) and (orientation: portrait),
       (max-width: 736px) and (max-height: 420px) and (orientation: landscape) {
  body {
    margin: 0 auto;
  }

  button,
  [dir='rtl'] button,
  button.small-link {
    font-family: Roboto-Regular,Helvetica;
    font-size: .933em;
    font-weight: 600;
    margin: 6px 0;
    text-transform: uppercase;
  }

  .nav-wrapper {
    box-sizing: border-box;
    padding-bottom: 8px;
    width: 100%;
  }

  .error-code {
    margin-top: 0;
  }

  #details {
    box-sizing: border-box;
    height: auto;
    margin: 0;
    opacity: 1;
    transition: opacity 250ms cubic-bezier(0.4, 0, 0.2, 1);
  }

  #details.hidden,
  #main-content.hidden {
    display: block;
    height: 0;
    opacity: 0;
    overflow: hidden;
    transition: none;
  }

  #details-button {
    padding-bottom: 16px;
    padding-top: 16px;
  }

  h1 {
    font-size: 1.5em;
    margin-bottom: 8px;
  }

  .icon {
    margin-bottom: 12px;
  }

  .interstitial-wrapper {
    box-sizing: border-box;
    margin: 24px auto 12px;
    padding: 0 24px;
    position: relative;
  }

  .interstitial-wrapper p {
    font-size: .95em;
    line-height: 1.61em;
    margin-top: 8px;
  }

  #main-content {
    margin: 0;
    transition: opacity 100ms cubic-bezier(0.4, 0, 0.2, 1);
  }

  .small-link {
    border: 0;
  }

  .suggested-left > #control-buttons,
  .suggested-right > #control-buttons {
    float: none;
    margin: 0;
  }
}

@media (min-height: 400px) and (orientation:portrait) {
  .interstitial-wrapper {
    margin-bottom: 145px;
  }
}

@media (min-height: 299px) and (orientation:portrait) {
  .nav-wrapper {
    padding-bottom: 16px;
  }
}

@media (min-height: 405px) and (max-height: 736px) and
       (max-width: 420px) and (orientation:portrait) {
  .icon {
    margin-bottom: 24px;
  }

  .interstitial-wrapper {
    margin-top: 64px;
  }
}

@media (min-height: 480px) and (max-width: 420px) and
       (max-height: 736px) and (orientation: portrait),
       (min-height: 338px) and (max-height: 420px) and (max-width: 736px) and
       (orientation: landscape) {
  .icon {
    margin-bottom: 24px;
  }

  .nav-wrapper {
    padding-bottom: 24px;
  }
}

@media (min-height: 500px) and (max-width: 414px) and (orientation: portrait) {
  .interstitial-wrapper {
    margin-top: 96px;
  }
}

/* Phablet sizing */
@media (min-width: 375px) and (min-height: 641px) and (max-height: 736px) and
       (max-width: 414px) and (orientation: portrait) {
  button,
  [dir='rtl'] button,
  .small-link {
    font-size: 1em;
    padding-bottom: 12px;
    padding-top: 12px;
  }

  body:not(.offline) .icon {
    height: 80px;
    width: 80px;
  }

  #details-button {
    margin-top: 28px;
  }

  h1 {
    font-size: 1.7em;
  }

  .icon {
    margin-bottom: 28px;
  }

  .interstitial-wrapper {
    padding: 28px;
  }

  .interstitial-wrapper p {
    font-size: 1.05em;
  }

  .nav-wrapper {
    padding: 28px;
  }
}

@media (min-width: 420px) and (max-width: 736px) and
       (min-height: 240px) and (max-height: 298px) and
       (orientation:landscape) {
  body:not(.offline) .icon {
    height: 50px;
    width: 50px;
  }

  .icon {
    padding-top: 0;
  }

  .interstitial-wrapper {
    margin-top: 16px;
  }

  .nav-wrapper {
    padding: 0 24px 8px;
  }
}

@media (min-width: 420px) and (max-width: 736px) and
       (min-height: 240px) and (max-height: 420px) and
       (orientation:landscape) {
  #details-button {
    margin: 0;
  }

  .interstitial-wrapper {
    margin-bottom: 70px;
  }

  .nav-wrapper {
    margin-top: 0;
  }

  #extended-reporting-opt-in {
    margin-top: 0;
  }
}

/* Phablet landscape */
@media (min-width: 680px) and (max-height: 414px) {
  .interstitial-wrapper {
    margin: 24px auto;
  }

  .nav-wrapper {
    margin: 16px auto 0;
  }
}

@media (max-height: 240px) and (orientation: landscape),
       (max-height: 480px) and (orientation: portrait),
       (max-width: 419px) and (max-height: 323px) {
  body:not(.offline) .icon {
    height: 56px;
    width: 56px;
  }

  .icon {
    margin-bottom: 16px;
  }
}

/* Small mobile screens. No fixed nav. */
@media (max-height: 400px) and (orientation: portrait),
       (max-height: 239px) and (orientation: landscape),
       (max-width: 419px) and (max-height: 399px) {
  .interstitial-wrapper {
    display: flex;
    flex-direction: column;
    margin-bottom: 0;
  }

  #details {
    flex: 1 1 auto;
    order: 0;
  }

  #main-content {
    flex: 1 1 auto;
    order: 0;
  }

  .nav-wrapper {
    flex: 0 1 auto;
    margin-top: 8px;
    order: 1;
    padding-left: 0;
    padding-right: 0;
    position: relative;
    width: 100%;
  }
}

@media (max-width: 239px) and (orientation: portrait) {
  .nav-wrapper {
    padding-left: 0;
    padding-right: 0;
  }
}
</style>
  <style>/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

/* Don't use the main frame div when the error is in a subframe. */
html[subframe] #main-frame-error {
  display: none;
}

/* Don't use the subframe error div when the error is in a main frame. */
html:not([subframe]) #sub-frame-error {
  display: none;
}

#diagnose-button {
  -webkit-margin-start: 0;
  float: none;
  margin-bottom: 10px;
  margin-top: 20px;
}

h1 {
  margin-top: 0;
  word-wrap: break-word;
}

h1 span {
  font-weight: 500;
}

h2 {
  color: #666;
  font-size: 1.2em;
  font-weight: normal;
  margin: 10px 0;
}

a {
  color: rgb(17, 85, 204);
  text-decoration: none;
}

.icon {
  -webkit-user-select: none;
  display: inline-block;
}

.icon-generic {
  /**
   * Can't access chrome://theme/IDR_ERROR_NETWORK_GENERIC from an untrusted
   * renderer process, so embed the resource manually.
   */
  content: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABIAQMAAABvIyEEAAAABlBMVEUAAABTU1OoaSf/AAAAAXRSTlMAQObYZgAAAENJREFUeF7tzbEJACEQRNGBLeAasBCza2lLEGx0CxFGG9hBMDDxRy/72O9FMnIFapGylsu1fgoBdkXfUHLrQgdfrlJN1BdYBjQQm3UAAAAASUVORK5CYII=) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJAAAACQAQMAAADdiHD7AAAABlBMVEUAAABTU1OoaSf/AAAAAXRSTlMAQObYZgAAAFJJREFUeF7t0cENgDAMQ9FwYgxG6WjpaIzCCAxQxVggFuDiCvlLOeRdHR9yzjncHVoq3npu+wQUrUuJHylSTmBaespJyJQoObUeyxDQb3bEm5Au81c0pSCD8HYAAAAASUVORK5CYII=) 2x);
}

.icon-offline {
  content: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABIAQMAAABvIyEEAAAABlBMVEUAAABTU1OoaSf/AAAAAXRSTlMAQObYZgAAAGxJREFUeF7tyMEJwkAQRuFf5ipMKxYQiJ3Z2nSwrWwBA0+DQZcdxEOueaePp9+dQZFB7GpUcURSVU66yVNFj6LFICatThZB6r/ko/pbRpUgilY0Cbw5sNmb9txGXUKyuH7eV25x39DtJXUNPQGJtWFV+BT/QAAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJAAAACQBAMAAAAVaP+LAAAAGFBMVEUAAABTU1NNTU1TU1NPT09SUlJSUlJTU1O8B7DEAAAAB3RSTlMAoArVKvVgBuEdKgAAAJ1JREFUeF7t1TEOwyAMQNG0Q6/UE+RMXD9d/tC6womIFSL9P+MnAYOXeTIzMzMzMzMzaz8J9Ri6HoITmuHXhISE8nEh9yxDh55aCEUoTGbbQwjqHwIkRAEiIaG0+0AA9VBMaE89Rogeoww936MQrWdBr4GN/z0IAdQ6nQ/FIpRXDwHcA+JIJcQowQAlFUA0MfQpXLlVQfkzR4igS6ENjknm/wiaGhsAAAAASUVORK5CYII=) 2x);
  position: relative;
}

.icon-disabled {
  content: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAHAAAABICAMAAAAZF4G5AAAABlBMVEVMaXFTU1OXUj8tAAAAAXRSTlMAQObYZgAAASZJREFUeAHd11Fq7jAMRGGf/W/6PoWB67YMqv5DybwG/CFjRuR8JBw3+ByiRjgV9W/TJ31P0tBfC6+cj1haUFXKHmVJo5wP98WwQ0ZCbfUc6LQ6VuUBz31ikADkLMkDrfUC4rR6QGW+gF6rx7NaHWCj1Y/W6lf4L7utvgBSt3rBFSS/XBMPUILcJINHCBWYUfpWn4NBi1ZfudIc3rf6/NGEvEA+AsYTJozmXemjXeLZAov+mnkN2HfzXpMSVQDnGw++57qNJ4D1xitA2sJ+VAWMygSEaYf2mYPTjZfk2K8wmP7HLIH5Mg4/pP+PEcDzUvDMvYbs/2NWwPO5vBdMZE4EE5UTQLiBFDaUlTDPBRoJ9HdAYIkIo06og3BNXtCzy7zA1aXk5x+tJARq63eAygAAAABJRU5ErkJggg==) 1x,
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAOAAAACQAQMAAAArwfVjAAAABlBMVEVMaXFTU1OXUj8tAAAAAXRSTlMAQObYZgAAAYdJREFUeF7F1EFqwzAUBNARAmVj0FZe5QoBH6BX+dn4GlY2PYNzGx/A0CvkCIJuvIraKJKbgBvzf2g62weDGD7CYggpfFReis4J0ey9EGFIiEQQojFSlA9kSIiqd0KkFjKsewgRbStEN19mxUPTtmW9HQ/h6tyqNQ8NlSMZdzyE6qkoE0trVYGFm0n1WYeBhduzwbwBC7voS+vIxfeMjeaiLxsMMtQNwMPtuew+DjzcTHk8YMfDknEcIUOtf2lVfgVH3K4Xv5PRYAXRVMtItIJ3rfaCIVn9DsTH2NxisAVRex2Hh3hX+/mRUR08bAwPEYsI51ZxWH4Q0SpicQRXeyEaIug48FEdegARfMz/tADVsRciwTAxW308ehmC2gLraC+YCbV3QoTZexa+zegAEW5PhhgYfmbvJgcRqngGByOSXdFJcLk2JeDPEN0kxe1JhIt5FiFA+w+ItMELsUyPF2IaJ4aILqb4FbxPwhImwj6JauKgDUCYaxmYIsd4KXdMjIC9ItB5Bn4BNRwsG0XM2nwAAAAASUVORK5CYII=) 2x);
  width: 112px;
}

.error-code {
  display: block;
  font-size: .8em;
}

#content-top {
  margin: 20px;
}

#help-box-inner {
  background-color: #f9f9f9;
  border-top: 1px solid #EEE;
  color: #444;
  padding: 20px;
  text-align: start;
}

.hidden {
  display: none;
}

#suggestion {
  margin-top: 15px;
}

#suggestions-list p {
  -webkit-margin-after: 0;
}

#suggestions-list ul {
  margin-top: 0;
}

.single-suggestion {
  list-style-type: none;
  padding-left: 0;
}

#short-suggestion {
  margin-top: 5px;
}

#sub-frame-error-details {

  color: #8F8F8F;
/* Not done on mobile for performance reasons. */
  text-shadow: 0 1px 0 rgba(255,255,255,0.3);
}

[jscontent=hostName],
[jscontent=failedUrl] {
  overflow-wrap: break-word;
}

#search-container {
  /* Prevents a space between controls. */
  display: flex;
  margin-top: 20px;
}

#search-box {
  border: 1px solid #cdcdcd;
  flex-grow: 1;
  font-size: 1em;
  height: 26px;
  margin-right: 0;
  padding: 1px 9px;
}

#search-box:focus {
  border: 1px solid rgb(93, 154, 255);
  outline: none;
}

#search-button {
  border: none;
  border-bottom-left-radius: 0;
  border-top-left-radius: 0;
  box-shadow: none;
  display: flex;
  height: 30px;
  margin: 0;
  padding: 0;
  width: 60px;
}

#search-image {
  content:
      -webkit-image-set(
          url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAPCAQAAAB+HTb/AAAArElEQVR4Xn3NsUoCUBzG0XvB3U0chR4geo5qihpt6gkCx0bXFsMERWj2KWqIanAvmlUUoQapwU6g4l8H5bd9Z/iSPS0hu/RqZqrncBuzLl7U3Rn4cSpQFTeroejJl1Lgs7f4ceDPdeBMXYp86gaONYJkY83AnqHiGk9wHnjk16PKgo5N9BUCkzPf5j6M0PfuVg5MymoetFwoaKAlB26WdXAvJ7u5mezitqtkT//7Sv/u96CaLQAAAABJRU5ErkJggg==) 1x,
          url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABwAAAAeCAQAAACVzLYUAAABYElEQVR4Xr3VMUuVURzH8XO98jgkGikENkRD0KRGDUVDQy0h2SiC4IuIiktL4AvQt1CDBJUJwo1KXXS6cWdHw7tcjWwoC5Hrx+UZgnNO5CXiO/75jD/+QZf9MzjskVU7DrU1zRv9G9ir5hsA4Nii83+GA9ZI1nI1D6tWAE1TRlQMuuuFDthzMQefgo4nKr+f3dIGDdUUHPYD1ISoMQdgJgUfgqaKEOcxWE/BVTArJBvwC0cGY7gNLgiZNsD1GP4EPVn4EtyLYRuczcJ34HYMP4E7GdajDS7FcB48z8AJ8FmI4TjouBkzZ2yBuRQMlsButIZ+dfDVUBqOaIHvavpLVHXfFmAqv45r9gEHNr3y3hcAfLSgSMPgiiZR+6Z9AMuKNAwqpjUcA2h55pxgAfBWkYRlQ254YMJloaxPHbCkiGCymL5RlLA7GnRDXyuC7uhicLoKdRyaDE5Pl00K//93nABqPgBDK8sfWgAAAABJRU5ErkJggg==) 2x);
  margin: auto;
}

.secondary-button {
  -webkit-margin-end: 16px;
  background: #d9d9d9;
  color: #696969;
}

.snackbar {
  background: #323232;
  border-radius: 2px;
  bottom: 24px;
  box-sizing: border-box;
  color: #fff;
  font-size: .87em;
  left: 24px;
  max-width: 568px;
  min-width: 288px;
  opacity: 0;
  padding: 16px 24px 12px;
  position: fixed;
  transform: translateY(90px);
  will-change: opacity, transform;
  z-index: 999;
}

.snackbar-show {
  -webkit-animation:
    show-snackbar .25s cubic-bezier(0.0, 0.0, 0.2, 1) forwards,
    hide-snackbar .25s cubic-bezier(0.4, 0.0, 1, 1) forwards 5s;
}

@-webkit-keyframes show-snackbar {
  100% {
    opacity: 1;
    transform: translateY(0);
  }
}

@-webkit-keyframes hide-snackbar {
  0% {
    opacity: 1;
    transform: translateY(0);
  }
  100% {
    opacity: 0;
    transform: translateY(90px);
  }
}

.suggestions {
  margin-top: 18px;
}

.suggestion-header {
  font-weight: bold;
  margin-bottom: 4px;
}

.suggestion-body {
  color: #777;
}

/* Increase line height at higher resolutions. */
@media (min-width: 641px) and (min-height: 641px) {
  #help-box-inner {
    line-height: 18px;
  }
}

/* Decrease padding at low sizes. */
@media (max-width: 640px), (max-height: 640px) {
  h1 {
    margin: 0 0 15px;
  }
  #content-top {
    margin: 15px;
  }
  #help-box-inner {
    padding: 20px;
  }
  .suggestions {
    margin-top: 10px;
  }
  .suggestion-header {
    margin-bottom: 0;
  }
}

/* Don't allow overflow when in a subframe. */
html[subframe] body {
  overflow: hidden;
}

#sub-frame-error {
  -webkit-align-items: center;
  background-color: #DDD;
  display: -webkit-flex;
  -webkit-flex-flow: column;
  height: 100%;
  -webkit-justify-content: center;
  left: 0;
  position: absolute;
  top: 0;
  transition: background-color .2s ease-in-out;
  width: 100%;
}

#sub-frame-error:hover {
  background-color: #EEE;
}

#sub-frame-error .icon-generic {
  margin: 0 0 16px;
}

#sub-frame-error-details {
  margin: 0 10px;
  text-align: center;
  visibility: hidden;
}

/* Show details only when hovering. */
#sub-frame-error:hover #sub-frame-error-details {
  visibility: visible;
}

/* If the iframe is too small, always hide the error code. */
/* TODO(mmenke): See if overflow: no-display works better, once supported. */
@media (max-width: 200px), (max-height: 95px) {
  #sub-frame-error-details {
    display: none;
  }
}

/* Adjust icon for small embedded frames in apps. */
@media (max-height: 100px) {
  #sub-frame-error .icon-generic {
    height: auto;
    margin: 0;
    padding-top: 0;
    width: 25px;
  }
}

/* details-button is special; it's a <button> element that looks like a link. */
#details-button {
  box-shadow: none;
  min-width: 0;
}

/* Styles for platform dependent separation of controls and details button. */
.suggested-left > #control-buttons,
.suggested-left #stale-load-button,
.suggested-right > #details-button {
  float: left;
}

.suggested-right > #control-buttons,
.suggested-right #stale-load-button,
.suggested-left > #details-button {
  float: right;
}

.suggested-left .secondary-button {
  -webkit-margin-end: 0px;
  -webkit-margin-start: 16px;
}

#details-button.singular {
  float: none;
}

#buttons::after {
  clear: both;
  content: '';
  display: block;
  width: 100%;
}

/* Offline page */
.offline {
  transition: -webkit-filter 1.5s cubic-bezier(0.65, 0.05, 0.36, 1),
              background-color 1.5s cubic-bezier(0.65, 0.05, 0.36, 1);
  will-change: -webkit-filter, background-color;
}

.offline.inverted {
  -webkit-filter: invert(100%);
  background-color: #000;
}

.offline .interstitial-wrapper {
  color: #2b2b2b;
  font-size: 1em;
  line-height: 1.55;
  margin: 0 auto;
  max-width: 600px;
  padding-top: 100px;
  width: 100%;
}

.offline .runner-container {
  height: 150px;
  max-width: 600px;
  overflow: hidden;
  position: absolute;
  top: 35px;
  width: 44px;
}

.offline .runner-canvas {
  height: 150px;
  max-width: 600px;
  opacity: 1;
  overflow: hidden;
  position: absolute;
  top: 0;
  z-index: 2;
}

.offline .controller {
  background: rgba(247,247,247, .1);
  height: 100vh;
  left: 0;
  position: absolute;
  top: 0;
  width: 100vw;
  z-index: 1;
}

#offline-resources {
  display: none;
}

@media (max-width: 420px) {
  .suggested-left > #control-buttons,
  .suggested-right > #control-buttons {
    float: none;
  }

  .snackbar {
    left: 0;
    bottom: 0;
    width: 100%;
    border-radius: 0;
  }
}

@media (max-height: 350px) {
  h1 {
    margin: 0 0 15px;
  }

  .icon-offline {
    margin: 0 0 10px;
  }

  .interstitial-wrapper {
    margin-top: 5%;
  }

  .nav-wrapper {
    margin-top: 30px;
  }
}

@media (min-width: 600px) and (max-width: 736px) and (orientation: landscape) {
  .offline .interstitial-wrapper {
    margin-left: 0;
    margin-right: 0;
  }
}

@media (min-width: 420px) and (max-width: 736px) and
       (min-height: 240px) and (max-height: 420px) and
       (orientation:landscape) {
  .interstitial-wrapper {
    margin-bottom: 100px;
  }
}

@media (min-height: 240px) and (orientation: landscape) {
  .offline .interstitial-wrapper {
    margin-bottom: 90px;
  }

  .icon-offline {
    margin-bottom: 20px;
  }
}

@media (max-height: 320px) and (orientation: landscape) {
  .icon-offline {
    margin-bottom: 0;
  }

  .offline .runner-container {
    top: 10px;
  }
}

@media (max-width: 240px) {
  button {
    padding-left: 12px;
    padding-right: 12px;
  }

  .interstitial-wrapper {
    overflow: inherit;
    padding: 0 8px;
  }
}

@media (max-width: 120px) {
  button {
    width: auto;
  }
}
</style>
  <script>// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var mobileNav = false;

/**
 * For small screen mobile the navigation buttons are moved
 * below the advanced text.
 */
function onResize() {
  var helpOuterBox = document.querySelector('#details');
  var mainContent = document.querySelector('#main-content');
  var mediaQuery = '(min-width: 240px) and (max-width: 420px) and ' +
      '(max-height: 736px) and (min-height: 401px) and ' +
      '(orientation: portrait), (max-width: 736px) and ' +
      '(max-height: 420px) and (min-height: 240px) and ' +
      '(min-width: 421px) and (orientation: landscape)';

  var detailsHidden = helpOuterBox.classList.contains('hidden');
  var runnerContainer = document.querySelector('.runner-container');

  // Check for change in nav status.
  if (mobileNav != window.matchMedia(mediaQuery).matches) {
    mobileNav = !mobileNav;

    // Handle showing the top content / details sections according to state.
    if (mobileNav) {
      mainContent.classList.toggle('hidden', !detailsHidden);
      helpOuterBox.classList.toggle('hidden', detailsHidden);
      if (runnerContainer) {
        runnerContainer.classList.toggle('hidden', !detailsHidden);
      }
    } else if (!detailsHidden) {
      // Non mobile nav with visible details.
      mainContent.classList.remove('hidden');
      helpOuterBox.classList.remove('hidden');
      if (runnerContainer) {
        runnerContainer.classList.remove('hidden');
      }
    }
  }
}

function setupMobileNav() {
  window.addEventListener('resize', onResize);
  onResize();
}

document.addEventListener('DOMContentLoaded', setupMobileNav);
</script>
  <script>// Copyright 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

function toggleHelpBox() {
  var helpBoxOuter = document.getElementById('details');
  helpBoxOuter.classList.toggle('hidden');
  var detailsButton = document.getElementById('details-button');
  if (helpBoxOuter.classList.contains('hidden'))
    detailsButton.innerText = detailsButton.detailsText;
  else
    detailsButton.innerText = detailsButton.hideDetailsText;

  // Details appears over the main content on small screens.
  if (mobileNav) {
    document.getElementById('main-content').classList.toggle('hidden');
    var runnerContainer = document.querySelector('.runner-container');
    if (runnerContainer) {
      runnerContainer.classList.toggle('hidden');
    }
  }
}

function diagnoseErrors() {
if (window.errorPageController)
      errorPageController.diagnoseErrorsButtonClick();

}

// Subframes use a different layout but the same html file.  This is to make it
// easier to support platforms that load the error page via different
// mechanisms (Currently just iOS).
if (window.top.location != window.location)
  document.documentElement.setAttribute('subframe', '');

// Re-renders the error page using |strings| as the dictionary of values.
// Used by NetErrorTabHelper to update DNS error pages with probe results.
function updateForDnsProbe(strings) {
  var context = new JsEvalContext(strings);
  jstProcess(context, document.getElementById('t'));
}

// Given the classList property of an element, adds an icon class to the list
// and removes the previously-
function updateIconClass(classList, newClass) {
  var oldClass;

  if (classList.hasOwnProperty('last_icon_class')) {
    oldClass = classList['last_icon_class'];
    if (oldClass == newClass)
      return;
  }

  classList.add(newClass);
  if (oldClass !== undefined)
    classList.remove(oldClass);

  classList['last_icon_class'] = newClass;

  if (newClass == 'icon-offline') {
    document.body.classList.add('offline');
    new Runner('.interstitial-wrapper');
  } else {
    document.body.classList.add('neterror');
  }
}

// Does a search using |baseSearchUrl| and the text in the search box.
function search(baseSearchUrl) {
  var searchTextNode = document.getElementById('search-box');
  document.location = baseSearchUrl + searchTextNode.value;
  return false;
}

// Use to track clicks on elements generated by the navigation correction
// service.  If |trackingId| is negative, the element does not come from the
// correction service.
function trackClick(trackingId) {
  // This can't be done with XHRs because XHRs are cancelled on navigation
  // start, and because these are cross-site requests.
  if (trackingId >= 0 && errorPageController)
    errorPageController.trackClick(trackingId);
}

// Called when an <a> tag generated by the navigation correction service is
// clicked.  Separate function from trackClick so the resources don't have to
// be updated if new data is added to jstdata.
function linkClicked(jstdata) {
  trackClick(jstdata.trackingId);
}

// Implements button clicks.  This function is needed during the transition
// between implementing these in trunk chromium and implementing them in
// iOS.
function reloadButtonClick(url) {
  if (window.errorPageController) {
    errorPageController.reloadButtonClick();
  } else {
    location = url;
  }
}

function showSavedCopyButtonClick() {
  if (window.errorPageController) {
    errorPageController.showSavedCopyButtonClick();
  }
}

function showOfflinePagesButtonClick() {
  if (window.errorPageController) {
    errorPageController.showOfflinePagesButtonClick();
  }
}

function detailsButtonClick() {
  if (window.errorPageController)
    errorPageController.detailsButtonClick();
}

/**
 * Replace the reload button with the Google cached copy suggestion.
 */
function setUpCachedButton(buttonStrings) {
  var reloadButton = document.getElementById('reload-button');

  reloadButton.textContent = buttonStrings.msg;
  var url = buttonStrings.cacheUrl;
  var trackingId = buttonStrings.trackingId;
  reloadButton.onclick = function(e) {
    e.preventDefault();
    trackClick(trackingId);
    if (window.errorPageController) {
      errorPageController.trackCachedCopyButtonClick();
    }
    location = url;
  };
  reloadButton.style.display = '';
  document.getElementById('control-buttons').hidden = false;
}

var primaryControlOnLeft = true;


function onDocumentLoad() {
  var controlButtonDiv = document.getElementById('control-buttons');
  var reloadButton = document.getElementById('reload-button');
  var detailsButton = document.getElementById('details-button');
  var showSavedCopyButton = document.getElementById('show-saved-copy-button');
  var showOfflinePagesButton =
      document.getElementById('show-offline-pages-button');

  var reloadButtonVisible = loadTimeData.valueExists('reloadButton') &&
      loadTimeData.getValue('reloadButton').msg;
  var showSavedCopyButtonVisible =
      loadTimeData.valueExists('showSavedCopyButton') &&
      loadTimeData.getValue('showSavedCopyButton').msg;
  var showOfflinePagesButtonVisible =
      loadTimeData.valueExists('showOfflinePagesButton') &&
      loadTimeData.getValue('showOfflinePagesButton').msg;

  var primaryButton, secondaryButton;
  if (showSavedCopyButton.primary) {
    primaryButton = showSavedCopyButton;
    secondaryButton = reloadButton;
  } else {
    primaryButton = reloadButton;
    secondaryButton = showSavedCopyButton;
  }

  // Sets up the proper button layout for the current platform.
  if (primaryControlOnLeft) {
    buttons.classList.add('suggested-left');
    controlButtonDiv.insertBefore(secondaryButton, primaryButton);
  } else {
    buttons.classList.add('suggested-right');
    controlButtonDiv.insertBefore(primaryButton, secondaryButton);
  }

  // Check for Google cached copy suggestion.
  if (loadTimeData.valueExists('cacheButton')) {
    setUpCachedButton(loadTimeData.getValue('cacheButton'));
  }

  if (reloadButton.style.display == 'none' &&
      showSavedCopyButton.style.display == 'none' &&
      showOfflinePagesButton.style.display == 'none') {
    detailsButton.classList.add('singular');
  }

  // Show control buttons.
  if (reloadButtonVisible || showSavedCopyButtonVisible ||
      showOfflinePagesButtonVisible) {
    controlButtonDiv.hidden = false;

    // Set the secondary button state in the cases of two call to actions.
    if ((reloadButtonVisible || showOfflinePagesButtonVisible) &&
        showSavedCopyButtonVisible) {
      secondaryButton.classList.add('secondary-button');
    }
  }
}

document.addEventListener('DOMContentLoaded', onDocumentLoad);
</script>
  <script>// Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
(function() {
'use strict';
/**
 * T-Rex runner.
 * @param {string} outerContainerId Outer containing element id.
 * @param {Object} opt_config
 * @constructor
 * @export
 */
function Runner(outerContainerId, opt_config) {
  // Singleton
  if (Runner.instance_) {
    return Runner.instance_;
  }
  Runner.instance_ = this;

  this.outerContainerEl = document.querySelector(outerContainerId);
  this.containerEl = null;
  this.snackbarEl = null;
  this.detailsButton = this.outerContainerEl.querySelector('#details-button');

  this.config = opt_config || Runner.config;

  this.dimensions = Runner.defaultDimensions;

  this.canvas = null;
  this.canvasCtx = null;

  this.tRex = null;

  this.distanceMeter = null;
  this.distanceRan = 0;

  this.highestScore = 0;

  this.time = 0;
  this.runningTime = 0;
  this.msPerFrame = 1000 / FPS;
  this.currentSpeed = this.config.SPEED;

  this.obstacles = [];

  this.started = false;
  this.activated = false;
  this.crashed = false;
  this.paused = false;
  this.inverted = false;
  this.invertTimer = 0;
  this.resizeTimerId_ = null;

  this.playCount = 0;

  // Sound FX.
  this.audioBuffer = null;
  this.soundFx = {};

  // Global web audio context for playing sounds.
  this.audioContext = null;

  // Images.
  this.images = {};
  this.imagesLoaded = 0;

  if (this.isDisabled()) {
    this.setupDisabledRunner();
  } else {
    this.loadImages();
  }
}
window['Runner'] = Runner;


/**
 * Default game width.
 * @const
 */
var DEFAULT_WIDTH = 600;

/**
 * Frames per second.
 * @const
 */
var FPS = 60;

/** @const */
var IS_HIDPI = window.devicePixelRatio > 1;

/** @const */
var IS_IOS = window.navigator.userAgent.indexOf('CriOS') > -1 ||
    window.navigator.userAgent == 'UIWebViewForStaticFileContent';

/** @const */
var IS_MOBILE = window.navigator.userAgent.indexOf('Mobi') > -1 || IS_IOS;

/** @const */
var IS_TOUCH_ENABLED = 'ontouchstart' in window;

/**
 * Default game configuration.
 * @enum {number}
 */
Runner.config = {
  ACCELERATION: 0.001,
  BG_CLOUD_SPEED: 0.2,
  BOTTOM_PAD: 10,
  CLEAR_TIME: 3000,
  CLOUD_FREQUENCY: 0.5,
  GAMEOVER_CLEAR_TIME: 750,
  GAP_COEFFICIENT: 0.6,
  GRAVITY: 0.6,
  INITIAL_JUMP_VELOCITY: 12,
  INVERT_FADE_DURATION: 12000,
  INVERT_DISTANCE: 700,
  MAX_CLOUDS: 6,
  MAX_OBSTACLE_LENGTH: 3,
  MAX_OBSTACLE_DUPLICATION: 2,
  MAX_SPEED: 13,
  MIN_JUMP_HEIGHT: 35,
  MOBILE_SPEED_COEFFICIENT: 1.2,
  RESOURCE_TEMPLATE_ID: 'audio-resources',
  SPEED: 6,
  SPEED_DROP_COEFFICIENT: 3
};


/**
 * Default dimensions.
 * @enum {string}
 */
Runner.defaultDimensions = {
  WIDTH: DEFAULT_WIDTH,
  HEIGHT: 150
};


/**
 * CSS class names.
 * @enum {string}
 */
Runner.classes = {
  CANVAS: 'runner-canvas',
  CONTAINER: 'runner-container',
  CRASHED: 'crashed',
  ICON: 'icon-offline',
  INVERTED: 'inverted',
  SNACKBAR: 'snackbar',
  SNACKBAR_SHOW: 'snackbar-show',
  TOUCH_CONTROLLER: 'controller'
};


/**
 * Sprite definition layout of the spritesheet.
 * @enum {Object}
 */
Runner.spriteDefinition = {
  LDPI: {
    CACTUS_LARGE: {x: 332, y: 2},
    CACTUS_SMALL: {x: 228, y: 2},
    CLOUD: {x: 86, y: 2},
    HORIZON: {x: 2, y: 54},
    MOON: {x: 484, y: 2},
    PTERODACTYL: {x: 134, y: 2},
    RESTART: {x: 2, y: 2},
    TEXT_SPRITE: {x: 655, y: 2},
    TREX: {x: 848, y: 2},
    STAR: {x: 645, y: 2}
  },
  HDPI: {
    CACTUS_LARGE: {x: 652, y: 2},
    CACTUS_SMALL: {x: 446, y: 2},
    CLOUD: {x: 166, y: 2},
    HORIZON: {x: 2, y: 104},
    MOON: {x: 954, y: 2},
    PTERODACTYL: {x: 260, y: 2},
    RESTART: {x: 2, y: 2},
    TEXT_SPRITE: {x: 1294, y: 2},
    TREX: {x: 1678, y: 2},
    STAR: {x: 1276, y: 2}
  }
};


/**
 * Sound FX. Reference to the ID of the audio tag on interstitial page.
 * @enum {string}
 */
Runner.sounds = {
  BUTTON_PRESS: 'offline-sound-press',
  HIT: 'offline-sound-hit',
  SCORE: 'offline-sound-reached'
};


/**
 * Key code mapping.
 * @enum {Object}
 */
Runner.keycodes = {
  JUMP: {'38': 1, '32': 1},  // Up, spacebar
  DUCK: {'40': 1},  // Down
  RESTART: {'13': 1}  // Enter
};


/**
 * Runner event names.
 * @enum {string}
 */
Runner.events = {
  ANIM_END: 'webkitAnimationEnd',
  CLICK: 'click',
  KEYDOWN: 'keydown',
  KEYUP: 'keyup',
  MOUSEDOWN: 'mousedown',
  MOUSEUP: 'mouseup',
  RESIZE: 'resize',
  TOUCHEND: 'touchend',
  TOUCHSTART: 'touchstart',
  VISIBILITY: 'visibilitychange',
  BLUR: 'blur',
  FOCUS: 'focus',
  LOAD: 'load'
};


Runner.prototype = {
  /**
   * Whether the easter egg has been disabled. CrOS enterprise enrolled devices.
   * @return {boolean}
   */
  isDisabled: function() {
    return loadTimeData && loadTimeData.valueExists('disabledEasterEgg');
  },

  /**
   * For disabled instances, set up a snackbar with the disabled message.
   */
  setupDisabledRunner: function() {
    this.containerEl = document.createElement('div');
    this.containerEl.className = Runner.classes.SNACKBAR;
    this.containerEl.textContent = loadTimeData.getValue('disabledEasterEgg');
    this.outerContainerEl.appendChild(this.containerEl);

    // Show notification when the activation key is pressed.
    document.addEventListener(Runner.events.KEYDOWN, function(e) {
      if (Runner.keycodes.JUMP[e.keyCode]) {
        this.containerEl.classList.add(Runner.classes.SNACKBAR_SHOW);
        document.querySelector('.icon').classList.add('icon-disabled');
      }
    }.bind(this));
  },

  /**
   * Setting individual settings for debugging.
   * @param {string} setting
   * @param {*} value
   */
  updateConfigSetting: function(setting, value) {
    if (setting in this.config && value != undefined) {
      this.config[setting] = value;

      switch (setting) {
        case 'GRAVITY':
        case 'MIN_JUMP_HEIGHT':
        case 'SPEED_DROP_COEFFICIENT':
          this.tRex.config[setting] = value;
          break;
        case 'INITIAL_JUMP_VELOCITY':
          this.tRex.setJumpVelocity(value);
          break;
        case 'SPEED':
          this.setSpeed(value);
          break;
      }
    }
  },

  /**
   * Cache the appropriate image sprite from the page and get the sprite sheet
   * definition.
   */
  loadImages: function() {
    if (IS_HIDPI) {
      Runner.imageSprite = document.getElementById('offline-resources-2x');
      this.spriteDef = Runner.spriteDefinition.HDPI;
    } else {
      Runner.imageSprite = document.getElementById('offline-resources-1x');
      this.spriteDef = Runner.spriteDefinition.LDPI;
    }

    this.init();
  },

  /**
   * Load and decode base 64 encoded sounds.
   */
  loadSounds: function() {
    if (!IS_IOS) {
      this.audioContext = new AudioContext();

      var resourceTemplate =
          document.getElementById(this.config.RESOURCE_TEMPLATE_ID).content;

      for (var sound in Runner.sounds) {
        var soundSrc =
            resourceTemplate.getElementById(Runner.sounds[sound]).src;
        soundSrc = soundSrc.substr(soundSrc.indexOf(',') + 1);
        var buffer = decodeBase64ToArrayBuffer(soundSrc);

        // Async, so no guarantee of order in array.
        this.audioContext.decodeAudioData(buffer, function(index, audioData) {
            this.soundFx[index] = audioData;
          }.bind(this, sound));
      }
    }
  },

  /**
   * Sets the game speed. Adjust the speed accordingly if on a smaller screen.
   * @param {number} opt_speed
   */
  setSpeed: function(opt_speed) {
    var speed = opt_speed || this.currentSpeed;

    // Reduce the speed on smaller mobile screens.
    if (this.dimensions.WIDTH < DEFAULT_WIDTH) {
      var mobileSpeed = speed * this.dimensions.WIDTH / DEFAULT_WIDTH *
          this.config.MOBILE_SPEED_COEFFICIENT;
      this.currentSpeed = mobileSpeed > speed ? speed : mobileSpeed;
    } else if (opt_speed) {
      this.currentSpeed = opt_speed;
    }
  },

  /**
   * Game initialiser.
   */
  init: function() {
    // Hide the static icon.
    document.querySelector('.' + Runner.classes.ICON).style.visibility =
        'hidden';

    this.adjustDimensions();
    this.setSpeed();

    this.containerEl = document.createElement('div');
    this.containerEl.className = Runner.classes.CONTAINER;

    // Player canvas container.
    this.canvas = createCanvas(this.containerEl, this.dimensions.WIDTH,
        this.dimensions.HEIGHT, Runner.classes.PLAYER);

    this.canvasCtx = this.canvas.getContext('2d');
    this.canvasCtx.fillStyle = '#f7f7f7';
    this.canvasCtx.fill();
    Runner.updateCanvasScaling(this.canvas);

    // Horizon contains clouds, obstacles and the ground.
    this.horizon = new Horizon(this.canvas, this.spriteDef, this.dimensions,
        this.config.GAP_COEFFICIENT);

    // Distance meter
    this.distanceMeter = new DistanceMeter(this.canvas,
          this.spriteDef.TEXT_SPRITE, this.dimensions.WIDTH);

    // Draw t-rex
    this.tRex = new Trex(this.canvas, this.spriteDef.TREX);

    this.outerContainerEl.appendChild(this.containerEl);

    if (IS_MOBILE) {
      this.createTouchController();
    }

    this.startListening();
    this.update();

    window.addEventListener(Runner.events.RESIZE,
        this.debounceResize.bind(this));
  },

  /**
   * Create the touch controller. A div that covers whole screen.
   */
  createTouchController: function() {
    this.touchController = document.createElement('div');
    this.touchController.className = Runner.classes.TOUCH_CONTROLLER;
  },

  /**
   * Debounce the resize event.
   */
  debounceResize: function() {
    if (!this.resizeTimerId_) {
      this.resizeTimerId_ =
          setInterval(this.adjustDimensions.bind(this), 250);
    }
  },

  /**
   * Adjust game space dimensions on resize.
   */
  adjustDimensions: function() {
    clearInterval(this.resizeTimerId_);
    this.resizeTimerId_ = null;

    var boxStyles = window.getComputedStyle(this.outerContainerEl);
    var padding = Number(boxStyles.paddingLeft.substr(0,
        boxStyles.paddingLeft.length - 2));

    this.dimensions.WIDTH = this.outerContainerEl.offsetWidth - padding * 2;

    // Redraw the elements back onto the canvas.
    if (this.canvas) {
      this.canvas.width = this.dimensions.WIDTH;
      this.canvas.height = this.dimensions.HEIGHT;

      Runner.updateCanvasScaling(this.canvas);

      this.distanceMeter.calcXPos(this.dimensions.WIDTH);
      this.clearCanvas();
      this.horizon.update(0, 0, true);
      this.tRex.update(0);

      // Outer container and distance meter.
      if (this.activated || this.crashed || this.paused) {
        this.containerEl.style.width = this.dimensions.WIDTH + 'px';
        this.containerEl.style.height = this.dimensions.HEIGHT + 'px';
        this.distanceMeter.update(0, Math.ceil(this.distanceRan));
        this.stop();
      } else {
        this.tRex.draw(0, 0);
      }

      // Game over panel.
      if (this.crashed && this.gameOverPanel) {
        this.gameOverPanel.updateDimensions(this.dimensions.WIDTH);
        this.gameOverPanel.draw();
      }
    }
  },

  /**
   * Play the game intro.
   * Canvas container width expands out to the full width.
   */
  playIntro: function() {
    if (!this.started && !this.crashed) {
      this.playingIntro = true;
      this.tRex.playingIntro = true;

      // CSS animation definition.
      var keyframes = '@-webkit-keyframes intro { ' +
            'from { width:' + Trex.config.WIDTH + 'px }' +
            'to { width: ' + this.dimensions.WIDTH + 'px }' +
          '}';
      document.styleSheets[0].insertRule(keyframes, 0);

      this.containerEl.addEventListener(Runner.events.ANIM_END,
          this.startGame.bind(this));

      this.containerEl.style.webkitAnimation = 'intro .4s ease-out 1 both';
      this.containerEl.style.width = this.dimensions.WIDTH + 'px';

      if (this.touchController) {
        this.outerContainerEl.appendChild(this.touchController);
      }
      this.activated = true;
      this.started = true;
    } else if (this.crashed) {
      this.restart();
    }
  },


  /**
   * Update the game status to started.
   */
  startGame: function() {
    this.runningTime = 0;
    this.playingIntro = false;
    this.tRex.playingIntro = false;
    this.containerEl.style.webkitAnimation = '';
    this.playCount++;

    // Handle tabbing off the page. Pause the current game.
    document.addEventListener(Runner.events.VISIBILITY,
          this.onVisibilityChange.bind(this));

    window.addEventListener(Runner.events.BLUR,
          this.onVisibilityChange.bind(this));

    window.addEventListener(Runner.events.FOCUS,
          this.onVisibilityChange.bind(this));
  },

  clearCanvas: function() {
    this.canvasCtx.clearRect(0, 0, this.dimensions.WIDTH,
        this.dimensions.HEIGHT);
  },

  /**
   * Update the game frame.
   */
  update: function() {
    this.drawPending = false;

    var now = getTimeStamp();
    var deltaTime = now - (this.time || now);
    this.time = now;

    if (this.activated) {
      this.clearCanvas();

      if (this.tRex.jumping) {
        this.tRex.updateJump(deltaTime);
      }

      this.runningTime += deltaTime;
      var hasObstacles = this.runningTime > this.config.CLEAR_TIME;

      // First jump triggers the intro.
      if (this.tRex.jumpCount == 1 && !this.playingIntro) {
        this.playIntro();
      }

      // The horizon doesn't move until the intro is over.
      if (this.playingIntro) {
        this.horizon.update(0, this.currentSpeed, hasObstacles);
      } else {
        deltaTime = !this.started ? 0 : deltaTime;
        this.horizon.update(deltaTime, this.currentSpeed, hasObstacles,
            this.inverted);
      }

      // Check for collisions.
      var collision = hasObstacles &&
          checkForCollision(this.horizon.obstacles[0], this.tRex);

      if (!collision) {
        this.distanceRan += this.currentSpeed * deltaTime / this.msPerFrame;

        if (this.currentSpeed < this.config.MAX_SPEED) {
          this.currentSpeed += this.config.ACCELERATION;
        }
      } else {
        this.gameOver();
      }

      var playAchievementSound = this.distanceMeter.update(deltaTime,
          Math.ceil(this.distanceRan));

      if (playAchievementSound) {
        this.playSound(this.soundFx.SCORE);
      }

      // Night mode.
      if (this.invertTimer > this.config.INVERT_FADE_DURATION) {
        this.invertTimer = 0;
        this.invertTrigger = false;
        this.invert();
      } else if (this.invertTimer) {
        this.invertTimer += deltaTime;
      } else {
        var actualDistance =
            this.distanceMeter.getActualDistance(Math.ceil(this.distanceRan));

        if (actualDistance > 0) {
          this.invertTrigger = !(actualDistance %
              this.config.INVERT_DISTANCE);

          if (this.invertTrigger && this.invertTimer === 0) {
            this.invertTimer += deltaTime;
            this.invert();
          }
        }
      }
    }

    if (!this.crashed) {
      this.tRex.update(deltaTime);
      this.raq();
    }
  },

  /**
   * Event handler.
   */
  handleEvent: function(e) {
    return (function(evtType, events) {
      switch (evtType) {
        case events.KEYDOWN:
        case events.TOUCHSTART:
        case events.MOUSEDOWN:
          this.onKeyDown(e);
          break;
        case events.KEYUP:
        case events.TOUCHEND:
        case events.MOUSEUP:
          this.onKeyUp(e);
          break;
      }
    }.bind(this))(e.type, Runner.events);
  },

  /**
   * Bind relevant key / mouse / touch listeners.
   */
  startListening: function() {
    // Keys.
    document.addEventListener(Runner.events.KEYDOWN, this);
    document.addEventListener(Runner.events.KEYUP, this);

    if (IS_MOBILE) {
      // Mobile only touch devices.
      this.touchController.addEventListener(Runner.events.TOUCHSTART, this);
      this.touchController.addEventListener(Runner.events.TOUCHEND, this);
      this.containerEl.addEventListener(Runner.events.TOUCHSTART, this);
    } else {
      // Mouse.
      document.addEventListener(Runner.events.MOUSEDOWN, this);
      document.addEventListener(Runner.events.MOUSEUP, this);
    }
  },

  /**
   * Remove all listeners.
   */
  stopListening: function() {
    document.removeEventListener(Runner.events.KEYDOWN, this);
    document.removeEventListener(Runner.events.KEYUP, this);

    if (IS_MOBILE) {
      this.touchController.removeEventListener(Runner.events.TOUCHSTART, this);
      this.touchController.removeEventListener(Runner.events.TOUCHEND, this);
      this.containerEl.removeEventListener(Runner.events.TOUCHSTART, this);
    } else {
      document.removeEventListener(Runner.events.MOUSEDOWN, this);
      document.removeEventListener(Runner.events.MOUSEUP, this);
    }
  },

  /**
   * Process keydown.
   * @param {Event} e
   */
  onKeyDown: function(e) {
    // Prevent native page scrolling whilst tapping on mobile.
    if (IS_MOBILE) {
      e.preventDefault();
    }

    if (e.target != this.detailsButton) {
      if (!this.crashed && (Runner.keycodes.JUMP[e.keyCode] ||
           e.type == Runner.events.TOUCHSTART)) {
        if (!this.activated) {
          this.loadSounds();
          this.activated = true;
          errorPageController.trackEasterEgg();
        }

        if (!this.tRex.jumping && !this.tRex.ducking) {
          this.playSound(this.soundFx.BUTTON_PRESS);
          this.tRex.startJump(this.currentSpeed);
        }
      }

      if (this.crashed && e.type == Runner.events.TOUCHSTART &&
          e.currentTarget == this.containerEl) {
        this.restart();
      }
    }

    if (this.activated && !this.crashed && Runner.keycodes.DUCK[e.keyCode]) {
      e.preventDefault();
      if (this.tRex.jumping) {
        // Speed drop, activated only when jump key is not pressed.
        this.tRex.setSpeedDrop();
      } else if (!this.tRex.jumping && !this.tRex.ducking) {
        // Duck.
        this.tRex.setDuck(true);
      }
    }
  },


  /**
   * Process key up.
   * @param {Event} e
   */
  onKeyUp: function(e) {
    var keyCode = String(e.keyCode);
    var isjumpKey = Runner.keycodes.JUMP[keyCode] ||
       e.type == Runner.events.TOUCHEND ||
       e.type == Runner.events.MOUSEDOWN;

    if (this.isRunning() && isjumpKey) {
      this.tRex.endJump();
    } else if (Runner.keycodes.DUCK[keyCode]) {
      this.tRex.speedDrop = false;
      this.tRex.setDuck(false);
    } else if (this.crashed) {
      // Check that enough time has elapsed before allowing jump key to restart.
      var deltaTime = getTimeStamp() - this.time;

      if (Runner.keycodes.RESTART[keyCode] || this.isLeftClickOnCanvas(e) ||
          (deltaTime >= this.config.GAMEOVER_CLEAR_TIME &&
          Runner.keycodes.JUMP[keyCode])) {
        this.restart();
      }
    } else if (this.paused && isjumpKey) {
      // Reset the jump state
      this.tRex.reset();
      this.play();
    }
  },

  /**
   * Returns whether the event was a left click on canvas.
   * On Windows right click is registered as a click.
   * @param {Event} e
   * @return {boolean}
   */
  isLeftClickOnCanvas: function(e) {
    return e.button != null && e.button < 2 &&
        e.type == Runner.events.MOUSEUP && e.target == this.canvas;
  },

  /**
   * RequestAnimationFrame wrapper.
   */
  raq: function() {
    if (!this.drawPending) {
      this.drawPending = true;
      this.raqId = requestAnimationFrame(this.update.bind(this));
    }
  },

  /**
   * Whether the game is running.
   * @return {boolean}
   */
  isRunning: function() {
    return !!this.raqId;
  },

  /**
   * Game over state.
   */
  gameOver: function() {
    this.playSound(this.soundFx.HIT);
    vibrate(200);

    this.stop();
    this.crashed = true;
    this.distanceMeter.acheivement = false;

    this.tRex.update(100, Trex.status.CRASHED);

    // Game over panel.
    if (!this.gameOverPanel) {
      this.gameOverPanel = new GameOverPanel(this.canvas,
          this.spriteDef.TEXT_SPRITE, this.spriteDef.RESTART,
          this.dimensions);
    } else {
      this.gameOverPanel.draw();
    }

    // Update the high score.
    if (this.distanceRan > this.highestScore) {
      this.highestScore = Math.ceil(this.distanceRan);
      this.distanceMeter.setHighScore(this.highestScore);
    }

    // Reset the time clock.
    this.time = getTimeStamp();
  },

  stop: function() {
    this.activated = false;
    this.paused = true;
    cancelAnimationFrame(this.raqId);
    this.raqId = 0;
  },

  play: function() {
    if (!this.crashed) {
      this.activated = true;
      this.paused = false;
      this.tRex.update(0, Trex.status.RUNNING);
      this.time = getTimeStamp();
      this.update();
    }
  },

  restart: function() {
    if (!this.raqId) {
      this.playCount++;
      this.runningTime = 0;
      this.activated = true;
      this.crashed = false;
      this.distanceRan = 0;
      this.setSpeed(this.config.SPEED);
      this.time = getTimeStamp();
      this.containerEl.classList.remove(Runner.classes.CRASHED);
      this.clearCanvas();
      this.distanceMeter.reset(this.highestScore);
      this.horizon.reset();
      this.tRex.reset();
      this.playSound(this.soundFx.BUTTON_PRESS);
      this.invert(true);
      this.update();
    }
  },

  /**
   * Pause the game if the tab is not in focus.
   */
  onVisibilityChange: function(e) {
    if (document.hidden || document.webkitHidden || e.type == 'blur' ||
      document.visibilityState != 'visible') {
      this.stop();
    } else if (!this.crashed) {
      this.tRex.reset();
      this.play();
    }
  },

  /**
   * Play a sound.
   * @param {SoundBuffer} soundBuffer
   */
  playSound: function(soundBuffer) {
    if (soundBuffer) {
      var sourceNode = this.audioContext.createBufferSource();
      sourceNode.buffer = soundBuffer;
      sourceNode.connect(this.audioContext.destination);
      sourceNode.start(0);
    }
  },

  /**
   * Inverts the current page / canvas colors.
   * @param {boolean} Whether to reset colors.
   */
  invert: function(reset) {
    if (reset) {
      document.body.classList.toggle(Runner.classes.INVERTED, false);
      this.invertTimer = 0;
      this.inverted = false;
    } else {
      this.inverted = document.body.classList.toggle(Runner.classes.INVERTED,
          this.invertTrigger);
    }
  }
};


/**
 * Updates the canvas size taking into
 * account the backing store pixel ratio and
 * the device pixel ratio.
 *
 * See article by Paul Lewis:
 * http://www.html5rocks.com/en/tutorials/canvas/hidpi/
 *
 * @param {HTMLCanvasElement} canvas
 * @param {number} opt_width
 * @param {number} opt_height
 * @return {boolean} Whether the canvas was scaled.
 */
Runner.updateCanvasScaling = function(canvas, opt_width, opt_height) {
  var context = canvas.getContext('2d');

  // Query the various pixel ratios
  var devicePixelRatio = Math.floor(window.devicePixelRatio) || 1;
  var backingStoreRatio = Math.floor(context.webkitBackingStorePixelRatio) || 1;
  var ratio = devicePixelRatio / backingStoreRatio;

  // Upscale the canvas if the two ratios don't match
  if (devicePixelRatio !== backingStoreRatio) {
    var oldWidth = opt_width || canvas.width;
    var oldHeight = opt_height || canvas.height;

    canvas.width = oldWidth * ratio;
    canvas.height = oldHeight * ratio;

    canvas.style.width = oldWidth + 'px';
    canvas.style.height = oldHeight + 'px';

    // Scale the context to counter the fact that we've manually scaled
    // our canvas element.
    context.scale(ratio, ratio);
    return true;
  } else if (devicePixelRatio == 1) {
    // Reset the canvas width / height. Fixes scaling bug when the page is
    // zoomed and the devicePixelRatio changes accordingly.
    canvas.style.width = canvas.width + 'px';
    canvas.style.height = canvas.height + 'px';
  }
  return false;
};


/**
 * Get random number.
 * @param {number} min
 * @param {number} max
 * @param {number}
 */
function getRandomNum(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}


/**
 * Vibrate on mobile devices.
 * @param {number} duration Duration of the vibration in milliseconds.
 */
function vibrate(duration) {
  if (IS_MOBILE && window.navigator.vibrate) {
    window.navigator.vibrate(duration);
  }
}


/**
 * Create canvas element.
 * @param {HTMLElement} container Element to append canvas to.
 * @param {number} width
 * @param {number} height
 * @param {string} opt_classname
 * @return {HTMLCanvasElement}
 */
function createCanvas(container, width, height, opt_classname) {
  var canvas = document.createElement('canvas');
  canvas.className = opt_classname ? Runner.classes.CANVAS + ' ' +
      opt_classname : Runner.classes.CANVAS;
  canvas.width = width;
  canvas.height = height;
  container.appendChild(canvas);

  return canvas;
}


/**
 * Decodes the base 64 audio to ArrayBuffer used by Web Audio.
 * @param {string} base64String
 */
function decodeBase64ToArrayBuffer(base64String) {
  var len = (base64String.length / 4) * 3;
  var str = atob(base64String);
  var arrayBuffer = new ArrayBuffer(len);
  var bytes = new Uint8Array(arrayBuffer);

  for (var i = 0; i < len; i++) {
    bytes[i] = str.charCodeAt(i);
  }
  return bytes.buffer;
}


/**
 * Return the current timestamp.
 * @return {number}
 */
function getTimeStamp() {
  return IS_IOS ? new Date().getTime() : performance.now();
}


//******************************************************************************


/**
 * Game over panel.
 * @param {!HTMLCanvasElement} canvas
 * @param {Object} textImgPos
 * @param {Object} restartImgPos
 * @param {!Object} dimensions Canvas dimensions.
 * @constructor
 */
function GameOverPanel(canvas, textImgPos, restartImgPos, dimensions) {
  this.canvas = canvas;
  this.canvasCtx = canvas.getContext('2d');
  this.canvasDimensions = dimensions;
  this.textImgPos = textImgPos;
  this.restartImgPos = restartImgPos;
  this.draw();
};


/**
 * Dimensions used in the panel.
 * @enum {number}
 */
GameOverPanel.dimensions = {
  TEXT_X: 0,
  TEXT_Y: 13,
  TEXT_WIDTH: 191,
  TEXT_HEIGHT: 11,
  RESTART_WIDTH: 36,
  RESTART_HEIGHT: 32
};


GameOverPanel.prototype = {
  /**
   * Update the panel dimensions.
   * @param {number} width New canvas width.
   * @param {number} opt_height Optional new canvas height.
   */
  updateDimensions: function(width, opt_height) {
    this.canvasDimensions.WIDTH = width;
    if (opt_height) {
      this.canvasDimensions.HEIGHT = opt_height;
    }
  },

  /**
   * Draw the panel.
   */
  draw: function() {
    var dimensions = GameOverPanel.dimensions;

    var centerX = this.canvasDimensions.WIDTH / 2;

    // Game over text.
    var textSourceX = dimensions.TEXT_X;
    var textSourceY = dimensions.TEXT_Y;
    var textSourceWidth = dimensions.TEXT_WIDTH;
    var textSourceHeight = dimensions.TEXT_HEIGHT;

    var textTargetX = Math.round(centerX - (dimensions.TEXT_WIDTH / 2));
    var textTargetY = Math.round((this.canvasDimensions.HEIGHT - 25) / 3);
    var textTargetWidth = dimensions.TEXT_WIDTH;
    var textTargetHeight = dimensions.TEXT_HEIGHT;

    var restartSourceWidth = dimensions.RESTART_WIDTH;
    var restartSourceHeight = dimensions.RESTART_HEIGHT;
    var restartTargetX = centerX - (dimensions.RESTART_WIDTH / 2);
    var restartTargetY = this.canvasDimensions.HEIGHT / 2;

    if (IS_HIDPI) {
      textSourceY *= 2;
      textSourceX *= 2;
      textSourceWidth *= 2;
      textSourceHeight *= 2;
      restartSourceWidth *= 2;
      restartSourceHeight *= 2;
    }

    textSourceX += this.textImgPos.x;
    textSourceY += this.textImgPos.y;

    // Game over text from sprite.
    this.canvasCtx.drawImage(Runner.imageSprite,
        textSourceX, textSourceY, textSourceWidth, textSourceHeight,
        textTargetX, textTargetY, textTargetWidth, textTargetHeight);

    // Restart button.
    this.canvasCtx.drawImage(Runner.imageSprite,
        this.restartImgPos.x, this.restartImgPos.y,
        restartSourceWidth, restartSourceHeight,
        restartTargetX, restartTargetY, dimensions.RESTART_WIDTH,
        dimensions.RESTART_HEIGHT);
  }
};


//******************************************************************************

/**
 * Check for a collision.
 * @param {!Obstacle} obstacle
 * @param {!Trex} tRex T-rex object.
 * @param {HTMLCanvasContext} opt_canvasCtx Optional canvas context for drawing
 *    collision boxes.
 * @return {Array<CollisionBox>}
 */
function checkForCollision(obstacle, tRex, opt_canvasCtx) {
  var obstacleBoxXPos = Runner.defaultDimensions.WIDTH + obstacle.xPos;

  // Adjustments are made to the bounding box as there is a 1 pixel white
  // border around the t-rex and obstacles.
  var tRexBox = new CollisionBox(
      tRex.xPos + 1,
      tRex.yPos + 1,
      tRex.config.WIDTH - 2,
      tRex.config.HEIGHT - 2);

  var obstacleBox = new CollisionBox(
      obstacle.xPos + 1,
      obstacle.yPos + 1,
      obstacle.typeConfig.width * obstacle.size - 2,
      obstacle.typeConfig.height - 2);

  // Debug outer box
  if (opt_canvasCtx) {
    drawCollisionBoxes(opt_canvasCtx, tRexBox, obstacleBox);
  }

  // Simple outer bounds check.
  if (boxCompare(tRexBox, obstacleBox)) {
    var collisionBoxes = obstacle.collisionBoxes;
    var tRexCollisionBoxes = tRex.ducking ?
        Trex.collisionBoxes.DUCKING : Trex.collisionBoxes.RUNNING;

    // Detailed axis aligned box check.
    for (var t = 0; t < tRexCollisionBoxes.length; t++) {
      for (var i = 0; i < collisionBoxes.length; i++) {
        // Adjust the box to actual positions.
        var adjTrexBox =
            createAdjustedCollisionBox(tRexCollisionBoxes[t], tRexBox);
        var adjObstacleBox =
            createAdjustedCollisionBox(collisionBoxes[i], obstacleBox);
        var crashed = boxCompare(adjTrexBox, adjObstacleBox);

        // Draw boxes for debug.
        if (opt_canvasCtx) {
          drawCollisionBoxes(opt_canvasCtx, adjTrexBox, adjObstacleBox);
        }

        if (crashed) {
          return [adjTrexBox, adjObstacleBox];
        }
      }
    }
  }
  return false;
};


/**
 * Adjust the collision box.
 * @param {!CollisionBox} box The original box.
 * @param {!CollisionBox} adjustment Adjustment box.
 * @return {CollisionBox} The adjusted collision box object.
 */
function createAdjustedCollisionBox(box, adjustment) {
  return new CollisionBox(
      box.x + adjustment.x,
      box.y + adjustment.y,
      box.width,
      box.height);
};


/**
 * Draw the collision boxes for debug.
 */
function drawCollisionBoxes(canvasCtx, tRexBox, obstacleBox) {
  canvasCtx.save();
  canvasCtx.strokeStyle = '#f00';
  canvasCtx.strokeRect(tRexBox.x, tRexBox.y, tRexBox.width, tRexBox.height);

  canvasCtx.strokeStyle = '#0f0';
  canvasCtx.strokeRect(obstacleBox.x, obstacleBox.y,
      obstacleBox.width, obstacleBox.height);
  canvasCtx.restore();
};


/**
 * Compare two collision boxes for a collision.
 * @param {CollisionBox} tRexBox
 * @param {CollisionBox} obstacleBox
 * @return {boolean} Whether the boxes intersected.
 */
function boxCompare(tRexBox, obstacleBox) {
  var crashed = false;
  var tRexBoxX = tRexBox.x;
  var tRexBoxY = tRexBox.y;

  var obstacleBoxX = obstacleBox.x;
  var obstacleBoxY = obstacleBox.y;

  // Axis-Aligned Bounding Box method.
  if (tRexBox.x < obstacleBoxX + obstacleBox.width &&
      tRexBox.x + tRexBox.width > obstacleBoxX &&
      tRexBox.y < obstacleBox.y + obstacleBox.height &&
      tRexBox.height + tRexBox.y > obstacleBox.y) {
    crashed = true;
  }

  return crashed;
};


//******************************************************************************

/**
 * Collision box object.
 * @param {number} x X position.
 * @param {number} y Y Position.
 * @param {number} w Width.
 * @param {number} h Height.
 */
function CollisionBox(x, y, w, h) {
  this.x = x;
  this.y = y;
  this.width = w;
  this.height = h;
};


//******************************************************************************

/**
 * Obstacle.
 * @param {HTMLCanvasCtx} canvasCtx
 * @param {Obstacle.type} type
 * @param {Object} spritePos Obstacle position in sprite.
 * @param {Object} dimensions
 * @param {number} gapCoefficient Mutipler in determining the gap.
 * @param {number} speed
 * @param {number} opt_xOffset
 */
function Obstacle(canvasCtx, type, spriteImgPos, dimensions,
    gapCoefficient, speed, opt_xOffset) {

  this.canvasCtx = canvasCtx;
  this.spritePos = spriteImgPos;
  this.typeConfig = type;
  this.gapCoefficient = gapCoefficient;
  this.size = getRandomNum(1, Obstacle.MAX_OBSTACLE_LENGTH);
  this.dimensions = dimensions;
  this.remove = false;
  this.xPos = dimensions.WIDTH + (opt_xOffset || 0);
  this.yPos = 0;
  this.width = 0;
  this.collisionBoxes = [];
  this.gap = 0;
  this.speedOffset = 0;

  // For animated obstacles.
  this.currentFrame = 0;
  this.timer = 0;

  this.init(speed);
};

/**
 * Coefficient for calculating the maximum gap.
 * @const
 */
Obstacle.MAX_GAP_COEFFICIENT = 1.5;

/**
 * Maximum obstacle grouping count.
 * @const
 */
Obstacle.MAX_OBSTACLE_LENGTH = 3,


Obstacle.prototype = {
  /**
   * Initialise the DOM for the obstacle.
   * @param {number} speed
   */
  init: function(speed) {
    this.cloneCollisionBoxes();

    // Only allow sizing if we're at the right speed.
    if (this.size > 1 && this.typeConfig.multipleSpeed > speed) {
      this.size = 1;
    }

    this.width = this.typeConfig.width * this.size;

    // Check if obstacle can be positioned at various heights.
    if (Array.isArray(this.typeConfig.yPos))  {
      var yPosConfig = IS_MOBILE ? this.typeConfig.yPosMobile :
          this.typeConfig.yPos;
      this.yPos = yPosConfig[getRandomNum(0, yPosConfig.length - 1)];
    } else {
      this.yPos = this.typeConfig.yPos;
    }

    this.draw();

    // Make collision box adjustments,
    // Central box is adjusted to the size as one box.
    //      ____        ______        ________
    //    _|   |-|    _|     |-|    _|       |-|
    //   | |<->| |   | |<--->| |   | |<----->| |
    //   | | 1 | |   | |  2  | |   | |   3   | |
    //   |_|___|_|   |_|_____|_|   |_|_______|_|
    //
    if (this.size > 1) {
      this.collisionBoxes[1].width = this.width - this.collisionBoxes[0].width -
          this.collisionBoxes[2].width;
      this.collisionBoxes[2].x = this.width - this.collisionBoxes[2].width;
    }

    // For obstacles that go at a different speed from the horizon.
    if (this.typeConfig.speedOffset) {
      this.speedOffset = Math.random() > 0.5 ? this.typeConfig.speedOffset :
          -this.typeConfig.speedOffset;
    }

    this.gap = this.getGap(this.gapCoefficient, speed);
  },

  /**
   * Draw and crop based on size.
   */
  draw: function() {
    var sourceWidth = this.typeConfig.width;
    var sourceHeight = this.typeConfig.height;

    if (IS_HIDPI) {
      sourceWidth = sourceWidth * 2;
      sourceHeight = sourceHeight * 2;
    }

    // X position in sprite.
    var sourceX = (sourceWidth * this.size) * (0.5 * (this.size - 1)) +
        this.spritePos.x;

    // Animation frames.
    if (this.currentFrame > 0) {
      sourceX += sourceWidth * this.currentFrame;
    }

    this.canvasCtx.drawImage(Runner.imageSprite,
      sourceX, this.spritePos.y,
      sourceWidth * this.size, sourceHeight,
      this.xPos, this.yPos,
      this.typeConfig.width * this.size, this.typeConfig.height);
  },

  /**
   * Obstacle frame update.
   * @param {number} deltaTime
   * @param {number} speed
   */
  update: function(deltaTime, speed) {
    if (!this.remove) {
      if (this.typeConfig.speedOffset) {
        speed += this.speedOffset;
      }
      this.xPos -= Math.floor((speed * FPS / 1000) * deltaTime);

      // Update frame
      if (this.typeConfig.numFrames) {
        this.timer += deltaTime;
        if (this.timer >= this.typeConfig.frameRate) {
          this.currentFrame =
              this.currentFrame == this.typeConfig.numFrames - 1 ?
              0 : this.currentFrame + 1;
          this.timer = 0;
        }
      }
      this.draw();

      if (!this.isVisible()) {
        this.remove = true;
      }
    }
  },

  /**
   * Calculate a random gap size.
   * - Minimum gap gets wider as speed increses
   * @param {number} gapCoefficient
   * @param {number} speed
   * @return {number} The gap size.
   */
  getGap: function(gapCoefficient, speed) {
    var minGap = Math.round(this.width * speed +
          this.typeConfig.minGap * gapCoefficient);
    var maxGap = Math.round(minGap * Obstacle.MAX_GAP_COEFFICIENT);
    return getRandomNum(minGap, maxGap);
  },

  /**
   * Check if obstacle is visible.
   * @return {boolean} Whether the obstacle is in the game area.
   */
  isVisible: function() {
    return this.xPos + this.width > 0;
  },

  /**
   * Make a copy of the collision boxes, since these will change based on
   * obstacle type and size.
   */
  cloneCollisionBoxes: function() {
    var collisionBoxes = this.typeConfig.collisionBoxes;

    for (var i = collisionBoxes.length - 1; i >= 0; i--) {
      this.collisionBoxes[i] = new CollisionBox(collisionBoxes[i].x,
          collisionBoxes[i].y, collisionBoxes[i].width,
          collisionBoxes[i].height);
    }
  }
};


/**
 * Obstacle definitions.
 * minGap: minimum pixel space betweeen obstacles.
 * multipleSpeed: Speed at which multiples are allowed.
 * speedOffset: speed faster / slower than the horizon.
 * minSpeed: Minimum speed which the obstacle can make an appearance.
 */
Obstacle.types = [
  {
    type: 'CACTUS_SMALL',
    width: 17,
    height: 35,
    yPos: 105,
    multipleSpeed: 4,
    minGap: 120,
    minSpeed: 0,
    collisionBoxes: [
      new CollisionBox(0, 7, 5, 27),
      new CollisionBox(4, 0, 6, 34),
      new CollisionBox(10, 4, 7, 14)
    ]
  },
  {
    type: 'CACTUS_LARGE',
    width: 25,
    height: 50,
    yPos: 90,
    multipleSpeed: 7,
    minGap: 120,
    minSpeed: 0,
    collisionBoxes: [
      new CollisionBox(0, 12, 7, 38),
      new CollisionBox(8, 0, 7, 49),
      new CollisionBox(13, 10, 10, 38)
    ]
  },
  {
    type: 'PTERODACTYL',
    width: 46,
    height: 40,
    yPos: [ 100, 75, 50 ], // Variable height.
    yPosMobile: [ 100, 50 ], // Variable height mobile.
    multipleSpeed: 999,
    minSpeed: 8.5,
    minGap: 150,
    collisionBoxes: [
      new CollisionBox(15, 15, 16, 5),
      new CollisionBox(18, 21, 24, 6),
      new CollisionBox(2, 14, 4, 3),
      new CollisionBox(6, 10, 4, 7),
      new CollisionBox(10, 8, 6, 9)
    ],
    numFrames: 2,
    frameRate: 1000/6,
    speedOffset: .8
  }
];


//******************************************************************************
/**
 * T-rex game character.
 * @param {HTMLCanvas} canvas
 * @param {Object} spritePos Positioning within image sprite.
 * @constructor
 */
function Trex(canvas, spritePos) {
  this.canvas = canvas;
  this.canvasCtx = canvas.getContext('2d');
  this.spritePos = spritePos;
  this.xPos = 0;
  this.yPos = 0;
  // Position when on the ground.
  this.groundYPos = 0;
  this.currentFrame = 0;
  this.currentAnimFrames = [];
  this.blinkDelay = 0;
  this.animStartTime = 0;
  this.timer = 0;
  this.msPerFrame = 1000 / FPS;
  this.config = Trex.config;
  // Current status.
  this.status = Trex.status.WAITING;

  this.jumping = false;
  this.ducking = false;
  this.jumpVelocity = 0;
  this.reachedMinHeight = false;
  this.speedDrop = false;
  this.jumpCount = 0;
  this.jumpspotX = 0;

  this.init();
};


/**
 * T-rex player config.
 * @enum {number}
 */
Trex.config = {
  DROP_VELOCITY: -5,
  GRAVITY: 0.6,
  HEIGHT: 47,
  HEIGHT_DUCK: 25,
  INIITAL_JUMP_VELOCITY: -10,
  INTRO_DURATION: 1500,
  MAX_JUMP_HEIGHT: 30,
  MIN_JUMP_HEIGHT: 30,
  SPEED_DROP_COEFFICIENT: 3,
  SPRITE_WIDTH: 262,
  START_X_POS: 50,
  WIDTH: 44,
  WIDTH_DUCK: 59
};


/**
 * Used in collision detection.
 * @type {Array<CollisionBox>}
 */
Trex.collisionBoxes = {
  DUCKING: [
    new CollisionBox(1, 18, 55, 25)
  ],
  RUNNING: [
    new CollisionBox(22, 0, 17, 16),
    new CollisionBox(1, 18, 30, 9),
    new CollisionBox(10, 35, 14, 8),
    new CollisionBox(1, 24, 29, 5),
    new CollisionBox(5, 30, 21, 4),
    new CollisionBox(9, 34, 15, 4)
  ]
};


/**
 * Animation states.
 * @enum {string}
 */
Trex.status = {
  CRASHED: 'CRASHED',
  DUCKING: 'DUCKING',
  JUMPING: 'JUMPING',
  RUNNING: 'RUNNING',
  WAITING: 'WAITING'
};

/**
 * Blinking coefficient.
 * @const
 */
Trex.BLINK_TIMING = 7000;


/**
 * Animation config for different states.
 * @enum {Object}
 */
Trex.animFrames = {
  WAITING: {
    frames: [44, 0],
    msPerFrame: 1000 / 3
  },
  RUNNING: {
    frames: [88, 132],
    msPerFrame: 1000 / 12
  },
  CRASHED: {
    frames: [220],
    msPerFrame: 1000 / 60
  },
  JUMPING: {
    frames: [0],
    msPerFrame: 1000 / 60
  },
  DUCKING: {
    frames: [262, 321],
    msPerFrame: 1000 / 8
  }
};


Trex.prototype = {
  /**
   * T-rex player initaliser.
   * Sets the t-rex to blink at random intervals.
   */
  init: function() {
    this.blinkDelay = this.setBlinkDelay();
    this.groundYPos = Runner.defaultDimensions.HEIGHT - this.config.HEIGHT -
        Runner.config.BOTTOM_PAD;
    this.yPos = this.groundYPos;
    this.minJumpHeight = this.groundYPos - this.config.MIN_JUMP_HEIGHT;

    this.draw(0, 0);
    this.update(0, Trex.status.WAITING);
  },

  /**
   * Setter for the jump velocity.
   * The approriate drop velocity is also set.
   */
  setJumpVelocity: function(setting) {
    this.config.INIITAL_JUMP_VELOCITY = -setting;
    this.config.DROP_VELOCITY = -setting / 2;
  },

  /**
   * Set the animation status.
   * @param {!number} deltaTime
   * @param {Trex.status} status Optional status to switch to.
   */
  update: function(deltaTime, opt_status) {
    this.timer += deltaTime;

    // Update the status.
    if (opt_status) {
      this.status = opt_status;
      this.currentFrame = 0;
      this.msPerFrame = Trex.animFrames[opt_status].msPerFrame;
      this.currentAnimFrames = Trex.animFrames[opt_status].frames;

      if (opt_status == Trex.status.WAITING) {
        this.animStartTime = getTimeStamp();
        this.setBlinkDelay();
      }
    }

    // Game intro animation, T-rex moves in from the left.
    if (this.playingIntro && this.xPos < this.config.START_X_POS) {
      this.xPos += Math.round((this.config.START_X_POS /
          this.config.INTRO_DURATION) * deltaTime);
    }

    if (this.status == Trex.status.WAITING) {
      this.blink(getTimeStamp());
    } else {
      this.draw(this.currentAnimFrames[this.currentFrame], 0);
    }

    // Update the frame position.
    if (this.timer >= this.msPerFrame) {
      this.currentFrame = this.currentFrame ==
          this.currentAnimFrames.length - 1 ? 0 : this.currentFrame + 1;
      this.timer = 0;
    }

    // Speed drop becomes duck if the down key is still being pressed.
    if (this.speedDrop && this.yPos == this.groundYPos) {
      this.speedDrop = false;
      this.setDuck(true);
    }
  },

  /**
   * Draw the t-rex to a particular position.
   * @param {number} x
   * @param {number} y
   */
  draw: function(x, y) {
    var sourceX = x;
    var sourceY = y;
    var sourceWidth = this.ducking && this.status != Trex.status.CRASHED ?
        this.config.WIDTH_DUCK : this.config.WIDTH;
    var sourceHeight = this.config.HEIGHT;

    if (IS_HIDPI) {
      sourceX *= 2;
      sourceY *= 2;
      sourceWidth *= 2;
      sourceHeight *= 2;
    }

    // Adjustments for sprite sheet position.
    sourceX += this.spritePos.x;
    sourceY += this.spritePos.y;

    // Ducking.
    if (this.ducking && this.status != Trex.status.CRASHED) {
      this.canvasCtx.drawImage(Runner.imageSprite, sourceX, sourceY,
          sourceWidth, sourceHeight,
          this.xPos, this.yPos,
          this.config.WIDTH_DUCK, this.config.HEIGHT);
    } else {
      // Crashed whilst ducking. Trex is standing up so needs adjustment.
      if (this.ducking && this.status == Trex.status.CRASHED) {
        this.xPos++;
      }
      // Standing / running
      this.canvasCtx.drawImage(Runner.imageSprite, sourceX, sourceY,
          sourceWidth, sourceHeight,
          this.xPos, this.yPos,
          this.config.WIDTH, this.config.HEIGHT);
    }
  },

  /**
   * Sets a random time for the blink to happen.
   */
  setBlinkDelay: function() {
    this.blinkDelay = Math.ceil(Math.random() * Trex.BLINK_TIMING);
  },

  /**
   * Make t-rex blink at random intervals.
   * @param {number} time Current time in milliseconds.
   */
  blink: function(time) {
    var deltaTime = time - this.animStartTime;

    if (deltaTime >= this.blinkDelay) {
      this.draw(this.currentAnimFrames[this.currentFrame], 0);

      if (this.currentFrame == 1) {
        // Set new random delay to blink.
        this.setBlinkDelay();
        this.animStartTime = time;
      }
    }
  },

  /**
   * Initialise a jump.
   * @param {number} speed
   */
  startJump: function(speed) {
    if (!this.jumping) {
      this.update(0, Trex.status.JUMPING);
      // Tweak the jump velocity based on the speed.
      this.jumpVelocity = this.config.INIITAL_JUMP_VELOCITY - (speed / 10);
      this.jumping = true;
      this.reachedMinHeight = false;
      this.speedDrop = false;
    }
  },

  /**
   * Jump is complete, falling down.
   */
  endJump: function() {
    if (this.reachedMinHeight &&
        this.jumpVelocity < this.config.DROP_VELOCITY) {
      this.jumpVelocity = this.config.DROP_VELOCITY;
    }
  },

  /**
   * Update frame for a jump.
   * @param {number} deltaTime
   * @param {number} speed
   */
  updateJump: function(deltaTime, speed) {
    var msPerFrame = Trex.animFrames[this.status].msPerFrame;
    var framesElapsed = deltaTime / msPerFrame;

    // Speed drop makes Trex fall faster.
    if (this.speedDrop) {
      this.yPos += Math.round(this.jumpVelocity *
          this.config.SPEED_DROP_COEFFICIENT * framesElapsed);
    } else {
      this.yPos += Math.round(this.jumpVelocity * framesElapsed);
    }

    this.jumpVelocity += this.config.GRAVITY * framesElapsed;

    // Minimum height has been reached.
    if (this.yPos < this.minJumpHeight || this.speedDrop) {
      this.reachedMinHeight = true;
    }

    // Reached max height
    if (this.yPos < this.config.MAX_JUMP_HEIGHT || this.speedDrop) {
      this.endJump();
    }

    // Back down at ground level. Jump completed.
    if (this.yPos > this.groundYPos) {
      this.reset();
      this.jumpCount++;
    }

    this.update(deltaTime);
  },

  /**
   * Set the speed drop. Immediately cancels the current jump.
   */
  setSpeedDrop: function() {
    this.speedDrop = true;
    this.jumpVelocity = 1;
  },

  /**
   * @param {boolean} isDucking.
   */
  setDuck: function(isDucking) {
    if (isDucking && this.status != Trex.status.DUCKING) {
      this.update(0, Trex.status.DUCKING);
      this.ducking = true;
    } else if (this.status == Trex.status.DUCKING) {
      this.update(0, Trex.status.RUNNING);
      this.ducking = false;
    }
  },

  /**
   * Reset the t-rex to running at start of game.
   */
  reset: function() {
    this.yPos = this.groundYPos;
    this.jumpVelocity = 0;
    this.jumping = false;
    this.ducking = false;
    this.update(0, Trex.status.RUNNING);
    this.midair = false;
    this.speedDrop = false;
    this.jumpCount = 0;
  }
};


//******************************************************************************

/**
 * Handles displaying the distance meter.
 * @param {!HTMLCanvasElement} canvas
 * @param {Object} spritePos Image position in sprite.
 * @param {number} canvasWidth
 * @constructor
 */
function DistanceMeter(canvas, spritePos, canvasWidth) {
  this.canvas = canvas;
  this.canvasCtx = canvas.getContext('2d');
  this.image = Runner.imageSprite;
  this.spritePos = spritePos;
  this.x = 0;
  this.y = 5;

  this.currentDistance = 0;
  this.maxScore = 0;
  this.highScore = 0;
  this.container = null;

  this.digits = [];
  this.acheivement = false;
  this.defaultString = '';
  this.flashTimer = 0;
  this.flashIterations = 0;
  this.invertTrigger = false;

  this.config = DistanceMeter.config;
  this.maxScoreUnits = this.config.MAX_DISTANCE_UNITS;
  this.init(canvasWidth);
};


/**
 * @enum {number}
 */
DistanceMeter.dimensions = {
  WIDTH: 10,
  HEIGHT: 13,
  DEST_WIDTH: 11
};


/**
 * Y positioning of the digits in the sprite sheet.
 * X position is always 0.
 * @type {Array<number>}
 */
DistanceMeter.yPos = [0, 13, 27, 40, 53, 67, 80, 93, 107, 120];


/**
 * Distance meter config.
 * @enum {number}
 */
DistanceMeter.config = {
  // Number of digits.
  MAX_DISTANCE_UNITS: 5,

  // Distance that causes achievement animation.
  ACHIEVEMENT_DISTANCE: 100,

  // Used for conversion from pixel distance to a scaled unit.
  COEFFICIENT: 0.025,

  // Flash duration in milliseconds.
  FLASH_DURATION: 1000 / 4,

  // Flash iterations for achievement animation.
  FLASH_ITERATIONS: 3
};


DistanceMeter.prototype = {
  /**
   * Initialise the distance meter to '00000'.
   * @param {number} width Canvas width in px.
   */
  init: function(width) {
    var maxDistanceStr = '';

    this.calcXPos(width);
    this.maxScore = this.maxScoreUnits;
    for (var i = 0; i < this.maxScoreUnits; i++) {
      this.draw(i, 0);
      this.defaultString += '0';
      maxDistanceStr += '9';
    }

    this.maxScore = parseInt(maxDistanceStr);
  },

  /**
   * Calculate the xPos in the canvas.
   * @param {number} canvasWidth
   */
  calcXPos: function(canvasWidth) {
    this.x = canvasWidth - (DistanceMeter.dimensions.DEST_WIDTH *
        (this.maxScoreUnits + 1));
  },

  /**
   * Draw a digit to canvas.
   * @param {number} digitPos Position of the digit.
   * @param {number} value Digit value 0-9.
   * @param {boolean} opt_highScore Whether drawing the high score.
   */
  draw: function(digitPos, value, opt_highScore) {
    var sourceWidth = DistanceMeter.dimensions.WIDTH;
    var sourceHeight = DistanceMeter.dimensions.HEIGHT;
    var sourceX = DistanceMeter.dimensions.WIDTH * value;
    var sourceY = 0;

    var targetX = digitPos * DistanceMeter.dimensions.DEST_WIDTH;
    var targetY = this.y;
    var targetWidth = DistanceMeter.dimensions.WIDTH;
    var targetHeight = DistanceMeter.dimensions.HEIGHT;

    // For high DPI we 2x source values.
    if (IS_HIDPI) {
      sourceWidth *= 2;
      sourceHeight *= 2;
      sourceX *= 2;
    }

    sourceX += this.spritePos.x;
    sourceY += this.spritePos.y;

    this.canvasCtx.save();

    if (opt_highScore) {
      // Left of the current score.
      var highScoreX = this.x - (this.maxScoreUnits * 2) *
          DistanceMeter.dimensions.WIDTH;
      this.canvasCtx.translate(highScoreX, this.y);
    } else {
      this.canvasCtx.translate(this.x, this.y);
    }

    this.canvasCtx.drawImage(this.image, sourceX, sourceY,
        sourceWidth, sourceHeight,
        targetX, targetY,
        targetWidth, targetHeight
      );

    this.canvasCtx.restore();
  },

  /**
   * Covert pixel distance to a 'real' distance.
   * @param {number} distance Pixel distance ran.
   * @return {number} The 'real' distance ran.
   */
  getActualDistance: function(distance) {
    return distance ? Math.round(distance * this.config.COEFFICIENT) : 0;
  },

  /**
   * Update the distance meter.
   * @param {number} distance
   * @param {number} deltaTime
   * @return {boolean} Whether the acheivement sound fx should be played.
   */
  update: function(deltaTime, distance) {
    var paint = true;
    var playSound = false;

    if (!this.acheivement) {
      distance = this.getActualDistance(distance);
      // Score has gone beyond the initial digit count.
      if (distance > this.maxScore && this.maxScoreUnits ==
        this.config.MAX_DISTANCE_UNITS) {
        this.maxScoreUnits++;
        this.maxScore = parseInt(this.maxScore + '9');
      } else {
        this.distance = 0;
      }

      if (distance > 0) {
        // Acheivement unlocked
        if (distance % this.config.ACHIEVEMENT_DISTANCE == 0) {
          // Flash score and play sound.
          this.acheivement = true;
          this.flashTimer = 0;
          playSound = true;
        }

        // Create a string representation of the distance with leading 0.
        var distanceStr = (this.defaultString +
            distance).substr(-this.maxScoreUnits);
        this.digits = distanceStr.split('');
      } else {
        this.digits = this.defaultString.split('');
      }
    } else {
      // Control flashing of the score on reaching acheivement.
      if (this.flashIterations <= this.config.FLASH_ITERATIONS) {
        this.flashTimer += deltaTime;

        if (this.flashTimer < this.config.FLASH_DURATION) {
          paint = false;
        } else if (this.flashTimer >
            this.config.FLASH_DURATION * 2) {
          this.flashTimer = 0;
          this.flashIterations++;
        }
      } else {
        this.acheivement = false;
        this.flashIterations = 0;
        this.flashTimer = 0;
      }
    }

    // Draw the digits if not flashing.
    if (paint) {
      for (var i = this.digits.length - 1; i >= 0; i--) {
        this.draw(i, parseInt(this.digits[i]));
      }
    }

    this.drawHighScore();
    return playSound;
  },

  /**
   * Draw the high score.
   */
  drawHighScore: function() {
    this.canvasCtx.save();
    this.canvasCtx.globalAlpha = .8;
    for (var i = this.highScore.length - 1; i >= 0; i--) {
      this.draw(i, parseInt(this.highScore[i], 10), true);
    }
    this.canvasCtx.restore();
  },

  /**
   * Set the highscore as a array string.
   * Position of char in the sprite: H - 10, I - 11.
   * @param {number} distance Distance ran in pixels.
   */
  setHighScore: function(distance) {
    distance = this.getActualDistance(distance);
    var highScoreStr = (this.defaultString +
        distance).substr(-this.maxScoreUnits);

    this.highScore = ['10', '11', ''].concat(highScoreStr.split(''));
  },

  /**
   * Reset the distance meter back to '00000'.
   */
  reset: function() {
    this.update(0);
    this.acheivement = false;
  }
};


//******************************************************************************

/**
 * Cloud background item.
 * Similar to an obstacle object but without collision boxes.
 * @param {HTMLCanvasElement} canvas Canvas element.
 * @param {Object} spritePos Position of image in sprite.
 * @param {number} containerWidth
 */
function Cloud(canvas, spritePos, containerWidth) {
  this.canvas = canvas;
  this.canvasCtx = this.canvas.getContext('2d');
  this.spritePos = spritePos;
  this.containerWidth = containerWidth;
  this.xPos = containerWidth;
  this.yPos = 0;
  this.remove = false;
  this.cloudGap = getRandomNum(Cloud.config.MIN_CLOUD_GAP,
      Cloud.config.MAX_CLOUD_GAP);

  this.init();
};


/**
 * Cloud object config.
 * @enum {number}
 */
Cloud.config = {
  HEIGHT: 14,
  MAX_CLOUD_GAP: 400,
  MAX_SKY_LEVEL: 30,
  MIN_CLOUD_GAP: 100,
  MIN_SKY_LEVEL: 71,
  WIDTH: 46
};


Cloud.prototype = {
  /**
   * Initialise the cloud. Sets the Cloud height.
   */
  init: function() {
    this.yPos = getRandomNum(Cloud.config.MAX_SKY_LEVEL,
        Cloud.config.MIN_SKY_LEVEL);
    this.draw();
  },

  /**
   * Draw the cloud.
   */
  draw: function() {
    this.canvasCtx.save();
    var sourceWidth = Cloud.config.WIDTH;
    var sourceHeight = Cloud.config.HEIGHT;

    if (IS_HIDPI) {
      sourceWidth = sourceWidth * 2;
      sourceHeight = sourceHeight * 2;
    }

    this.canvasCtx.drawImage(Runner.imageSprite, this.spritePos.x,
        this.spritePos.y,
        sourceWidth, sourceHeight,
        this.xPos, this.yPos,
        Cloud.config.WIDTH, Cloud.config.HEIGHT);

    this.canvasCtx.restore();
  },

  /**
   * Update the cloud position.
   * @param {number} speed
   */
  update: function(speed) {
    if (!this.remove) {
      this.xPos -= Math.ceil(speed);
      this.draw();

      // Mark as removeable if no longer in the canvas.
      if (!this.isVisible()) {
        this.remove = true;
      }
    }
  },

  /**
   * Check if the cloud is visible on the stage.
   * @return {boolean}
   */
  isVisible: function() {
    return this.xPos + Cloud.config.WIDTH > 0;
  }
};


//******************************************************************************

/**
 * Nightmode shows a moon and stars on the horizon.
 */
function NightMode(canvas, spritePos, containerWidth) {
  this.spritePos = spritePos;
  this.canvas = canvas;
  this.canvasCtx = canvas.getContext('2d');
  this.xPos = containerWidth - 50;
  this.yPos = 30;
  this.currentPhase = 0;
  this.opacity = 0;
  this.containerWidth = containerWidth;
  this.stars = [];
  this.drawStars = false;
  this.placeStars();
};

/**
 * @enum {number}
 */
NightMode.config = {
  FADE_SPEED: 0.035,
  HEIGHT: 40,
  MOON_SPEED: 0.25,
  NUM_STARS: 2,
  STAR_SIZE: 9,
  STAR_SPEED: 0.3,
  STAR_MAX_Y: 70,
  WIDTH: 20
};

NightMode.phases = [140, 120, 100, 60, 40, 20, 0];

NightMode.prototype = {
  /**
   * Update moving moon, changing phases.
   * @param {boolean} activated Whether night mode is activated.
   * @param {number} delta
   */
  update: function(activated, delta) {
    // Moon phase.
    if (activated && this.opacity == 0) {
      this.currentPhase++;

      if (this.currentPhase >= NightMode.phases.length) {
        this.currentPhase = 0;
      }
    }

    // Fade in / out.
    if (activated && (this.opacity < 1 || this.opacity == 0)) {
      this.opacity += NightMode.config.FADE_SPEED;
    } else if (this.opacity > 0) {
      this.opacity -= NightMode.config.FADE_SPEED;
    }

    // Set moon positioning.
    if (this.opacity > 0) {
      this.xPos = this.updateXPos(this.xPos, NightMode.config.MOON_SPEED);

      // Update stars.
      if (this.drawStars) {
         for (var i = 0; i < NightMode.config.NUM_STARS; i++) {
            this.stars[i].x = this.updateXPos(this.stars[i].x,
                NightMode.config.STAR_SPEED);
         }
      }
      this.draw();
    } else {
      this.opacity = 0;
      this.placeStars();
    }
    this.drawStars = true;
  },

  updateXPos: function(currentPos, speed) {
    if (currentPos < -NightMode.config.WIDTH) {
      currentPos = this.containerWidth;
    } else {
      currentPos -= speed;
    }
    return currentPos;
  },

  draw: function() {
    var moonSourceWidth = this.currentPhase == 3 ? NightMode.config.WIDTH * 2 :
         NightMode.config.WIDTH;
    var moonSourceHeight = NightMode.config.HEIGHT;
    var moonSourceX = this.spritePos.x + NightMode.phases[this.currentPhase];
    var moonOutputWidth = moonSourceWidth;
    var starSize = NightMode.config.STAR_SIZE;
    var starSourceX = Runner.spriteDefinition.LDPI.STAR.x;

    if (IS_HIDPI) {
      moonSourceWidth *= 2;
      moonSourceHeight *= 2;
      moonSourceX = this.spritePos.x +
          (NightMode.phases[this.currentPhase] * 2);
      starSize *= 2;
      starSourceX = Runner.spriteDefinition.HDPI.STAR.x;
    }

    this.canvasCtx.save();
    this.canvasCtx.globalAlpha = this.opacity;

    // Stars.
    if (this.drawStars) {
      for (var i = 0; i < NightMode.config.NUM_STARS; i++) {
        this.canvasCtx.drawImage(Runner.imageSprite,
            starSourceX, this.stars[i].sourceY, starSize, starSize,
            Math.round(this.stars[i].x), this.stars[i].y,
            NightMode.config.STAR_SIZE, NightMode.config.STAR_SIZE);
      }
    }

    // Moon.
    this.canvasCtx.drawImage(Runner.imageSprite, moonSourceX,
        this.spritePos.y, moonSourceWidth, moonSourceHeight,
        Math.round(this.xPos), this.yPos,
        moonOutputWidth, NightMode.config.HEIGHT);

    this.canvasCtx.globalAlpha = 1;
    this.canvasCtx.restore();
  },

  // Do star placement.
  placeStars: function() {
    var segmentSize = Math.round(this.containerWidth /
        NightMode.config.NUM_STARS);

    for (var i = 0; i < NightMode.config.NUM_STARS; i++) {
      this.stars[i] = {};
      this.stars[i].x = getRandomNum(segmentSize * i, segmentSize * (i + 1));
      this.stars[i].y = getRandomNum(0, NightMode.config.STAR_MAX_Y);

      if (IS_HIDPI) {
        this.stars[i].sourceY = Runner.spriteDefinition.HDPI.STAR.y +
            NightMode.config.STAR_SIZE * 2 * i;
      } else {
        this.stars[i].sourceY = Runner.spriteDefinition.LDPI.STAR.y +
            NightMode.config.STAR_SIZE * i;
      }
    }
  },

  reset: function() {
    this.currentPhase = 0;
    this.opacity = 0;
    this.update(false);
  }

};


//******************************************************************************

/**
 * Horizon Line.
 * Consists of two connecting lines. Randomly assigns a flat / bumpy horizon.
 * @param {HTMLCanvasElement} canvas
 * @param {Object} spritePos Horizon position in sprite.
 * @constructor
 */
function HorizonLine(canvas, spritePos) {
  this.spritePos = spritePos;
  this.canvas = canvas;
  this.canvasCtx = canvas.getContext('2d');
  this.sourceDimensions = {};
  this.dimensions = HorizonLine.dimensions;
  this.sourceXPos = [this.spritePos.x, this.spritePos.x +
      this.dimensions.WIDTH];
  this.xPos = [];
  this.yPos = 0;
  this.bumpThreshold = 0.5;

  this.setSourceDimensions();
  this.draw();
};


/**
 * Horizon line dimensions.
 * @enum {number}
 */
HorizonLine.dimensions = {
  WIDTH: 600,
  HEIGHT: 12,
  YPOS: 127
};


HorizonLine.prototype = {
  /**
   * Set the source dimensions of the horizon line.
   */
  setSourceDimensions: function() {

    for (var dimension in HorizonLine.dimensions) {
      if (IS_HIDPI) {
        if (dimension != 'YPOS') {
          this.sourceDimensions[dimension] =
              HorizonLine.dimensions[dimension] * 2;
        }
      } else {
        this.sourceDimensions[dimension] =
            HorizonLine.dimensions[dimension];
      }
      this.dimensions[dimension] = HorizonLine.dimensions[dimension];
    }

    this.xPos = [0, HorizonLine.dimensions.WIDTH];
    this.yPos = HorizonLine.dimensions.YPOS;
  },

  /**
   * Return the crop x position of a type.
   */
  getRandomType: function() {
    return Math.random() > this.bumpThreshold ? this.dimensions.WIDTH : 0;
  },

  /**
   * Draw the horizon line.
   */
  draw: function() {
    this.canvasCtx.drawImage(Runner.imageSprite, this.sourceXPos[0],
        this.spritePos.y,
        this.sourceDimensions.WIDTH, this.sourceDimensions.HEIGHT,
        this.xPos[0], this.yPos,
        this.dimensions.WIDTH, this.dimensions.HEIGHT);

    this.canvasCtx.drawImage(Runner.imageSprite, this.sourceXPos[1],
        this.spritePos.y,
        this.sourceDimensions.WIDTH, this.sourceDimensions.HEIGHT,
        this.xPos[1], this.yPos,
        this.dimensions.WIDTH, this.dimensions.HEIGHT);
  },

  /**
   * Update the x position of an indivdual piece of the line.
   * @param {number} pos Line position.
   * @param {number} increment
   */
  updateXPos: function(pos, increment) {
    var line1 = pos;
    var line2 = pos == 0 ? 1 : 0;

    this.xPos[line1] -= increment;
    this.xPos[line2] = this.xPos[line1] + this.dimensions.WIDTH;

    if (this.xPos[line1] <= -this.dimensions.WIDTH) {
      this.xPos[line1] += this.dimensions.WIDTH * 2;
      this.xPos[line2] = this.xPos[line1] - this.dimensions.WIDTH;
      this.sourceXPos[line1] = this.getRandomType() + this.spritePos.x;
    }
  },

  /**
   * Update the horizon line.
   * @param {number} deltaTime
   * @param {number} speed
   */
  update: function(deltaTime, speed) {
    var increment = Math.floor(speed * (FPS / 1000) * deltaTime);

    if (this.xPos[0] <= 0) {
      this.updateXPos(0, increment);
    } else {
      this.updateXPos(1, increment);
    }
    this.draw();
  },

  /**
   * Reset horizon to the starting position.
   */
  reset: function() {
    this.xPos[0] = 0;
    this.xPos[1] = HorizonLine.dimensions.WIDTH;
  }
};


//******************************************************************************

/**
 * Horizon background class.
 * @param {HTMLCanvasElement} canvas
 * @param {Object} spritePos Sprite positioning.
 * @param {Object} dimensions Canvas dimensions.
 * @param {number} gapCoefficient
 * @constructor
 */
function Horizon(canvas, spritePos, dimensions, gapCoefficient) {
  this.canvas = canvas;
  this.canvasCtx = this.canvas.getContext('2d');
  this.config = Horizon.config;
  this.dimensions = dimensions;
  this.gapCoefficient = gapCoefficient;
  this.obstacles = [];
  this.obstacleHistory = [];
  this.horizonOffsets = [0, 0];
  this.cloudFrequency = this.config.CLOUD_FREQUENCY;
  this.spritePos = spritePos;
  this.nightMode = null;

  // Cloud
  this.clouds = [];
  this.cloudSpeed = this.config.BG_CLOUD_SPEED;

  // Horizon
  this.horizonLine = null;
  this.init();
};


/**
 * Horizon config.
 * @enum {number}
 */
Horizon.config = {
  BG_CLOUD_SPEED: 0.2,
  BUMPY_THRESHOLD: .3,
  CLOUD_FREQUENCY: .5,
  HORIZON_HEIGHT: 16,
  MAX_CLOUDS: 6
};


Horizon.prototype = {
  /**
   * Initialise the horizon. Just add the line and a cloud. No obstacles.
   */
  init: function() {
    this.addCloud();
    this.horizonLine = new HorizonLine(this.canvas, this.spritePos.HORIZON);
    this.nightMode = new NightMode(this.canvas, this.spritePos.MOON,
        this.dimensions.WIDTH);
  },

  /**
   * @param {number} deltaTime
   * @param {number} currentSpeed
   * @param {boolean} updateObstacles Used as an override to prevent
   *     the obstacles from being updated / added. This happens in the
   *     ease in section.
   * @param {boolean} showNightMode Night mode activated.
   */
  update: function(deltaTime, currentSpeed, updateObstacles, showNightMode) {
    this.runningTime += deltaTime;
    this.horizonLine.update(deltaTime, currentSpeed);
    this.nightMode.update(showNightMode);
    this.updateClouds(deltaTime, currentSpeed);

    if (updateObstacles) {
      this.updateObstacles(deltaTime, currentSpeed);
    }
  },

  /**
   * Update the cloud positions.
   * @param {number} deltaTime
   * @param {number} currentSpeed
   */
  updateClouds: function(deltaTime, speed) {
    var cloudSpeed = this.cloudSpeed / 1000 * deltaTime * speed;
    var numClouds = this.clouds.length;

    if (numClouds) {
      for (var i = numClouds - 1; i >= 0; i--) {
        this.clouds[i].update(cloudSpeed);
      }

      var lastCloud = this.clouds[numClouds - 1];

      // Check for adding a new cloud.
      if (numClouds < this.config.MAX_CLOUDS &&
          (this.dimensions.WIDTH - lastCloud.xPos) > lastCloud.cloudGap &&
          this.cloudFrequency > Math.random()) {
        this.addCloud();
      }

      // Remove expired clouds.
      this.clouds = this.clouds.filter(function(obj) {
        return !obj.remove;
      });
    } else {
      this.addCloud();
    }
  },

  /**
   * Update the obstacle positions.
   * @param {number} deltaTime
   * @param {number} currentSpeed
   */
  updateObstacles: function(deltaTime, currentSpeed) {
    // Obstacles, move to Horizon layer.
    var updatedObstacles = this.obstacles.slice(0);

    for (var i = 0; i < this.obstacles.length; i++) {
      var obstacle = this.obstacles[i];
      obstacle.update(deltaTime, currentSpeed);

      // Clean up existing obstacles.
      if (obstacle.remove) {
        updatedObstacles.shift();
      }
    }
    this.obstacles = updatedObstacles;

    if (this.obstacles.length > 0) {
      var lastObstacle = this.obstacles[this.obstacles.length - 1];

      if (lastObstacle && !lastObstacle.followingObstacleCreated &&
          lastObstacle.isVisible() &&
          (lastObstacle.xPos + lastObstacle.width + lastObstacle.gap) <
          this.dimensions.WIDTH) {
        this.addNewObstacle(currentSpeed);
        lastObstacle.followingObstacleCreated = true;
      }
    } else {
      // Create new obstacles.
      this.addNewObstacle(currentSpeed);
    }
  },

  removeFirstObstacle: function() {
    this.obstacles.shift();
  },

  /**
   * Add a new obstacle.
   * @param {number} currentSpeed
   */
  addNewObstacle: function(currentSpeed) {
    var obstacleTypeIndex = getRandomNum(0, Obstacle.types.length - 1);
    var obstacleType = Obstacle.types[obstacleTypeIndex];

    // Check for multiples of the same type of obstacle.
    // Also check obstacle is available at current speed.
    if (this.duplicateObstacleCheck(obstacleType.type) ||
        currentSpeed < obstacleType.minSpeed) {
      this.addNewObstacle(currentSpeed);
    } else {
      var obstacleSpritePos = this.spritePos[obstacleType.type];

      this.obstacles.push(new Obstacle(this.canvasCtx, obstacleType,
          obstacleSpritePos, this.dimensions,
          this.gapCoefficient, currentSpeed, obstacleType.width));

      this.obstacleHistory.unshift(obstacleType.type);

      if (this.obstacleHistory.length > 1) {
        this.obstacleHistory.splice(Runner.config.MAX_OBSTACLE_DUPLICATION);
      }
    }
  },

  /**
   * Returns whether the previous two obstacles are the same as the next one.
   * Maximum duplication is set in config value MAX_OBSTACLE_DUPLICATION.
   * @return {boolean}
   */
  duplicateObstacleCheck: function(nextObstacleType) {
    var duplicateCount = 0;

    for (var i = 0; i < this.obstacleHistory.length; i++) {
      duplicateCount = this.obstacleHistory[i] == nextObstacleType ?
          duplicateCount + 1 : 0;
    }
    return duplicateCount >= Runner.config.MAX_OBSTACLE_DUPLICATION;
  },

  /**
   * Reset the horizon layer.
   * Remove existing obstacles and reposition the horizon line.
   */
  reset: function() {
    this.obstacles = [];
    this.horizonLine.reset();
    this.nightMode.reset();
  },

  /**
   * Update the canvas width and scaling.
   * @param {number} width Canvas width.
   * @param {number} height Canvas height.
   */
  resize: function(width, height) {
    this.canvas.width = width;
    this.canvas.height = height;
  },

  /**
   * Add a new cloud to the horizon.
   */
  addCloud: function() {
    this.clouds.push(new Cloud(this.canvas, this.spritePos.CLOUD,
        this.dimensions.WIDTH));
  }
};
})();
</script>
</head>
<body id="t" i18n-values=".style.fontFamily:fontfamily;.style.fontSize:fontsize">
  <div id="main-frame-error" class="interstitial-wrapper">
    <div id="main-content">
      <div class="icon"
          jseval="updateIconClass(this.classList, iconClass)"></div>
      <div id="main-message">
        <h1 jsselect="heading" jsvalues=".innerHTML:msg"></h1>
        <p jsselect="summary" jsvalues=".innerHTML:msg"></p>
        <div id="suggestions-list" jsdisplay="(suggestionsSummaryList && suggestionsSummaryList.length)">
          <p jsvalues=".innerHTML:suggestionsSummaryListHeader"></p>
          <ul jsvalues=".className:suggestionsSummaryList.length == 1 ? 'single-suggestion' : ''">
            <li jsselect="suggestionsSummaryList" jsvalues=".innerHTML:summary"></li>
          </ul>
        </div>
        <div class="error-code" jscontent="errorCode"></div>
      </div>
    </div>
    <div id="buttons" class="nav-wrapper">
      <div id="control-buttons" hidden>
        <button id="reload-button"
            class="blue-button text-button"
            onclick="trackClick(this.trackingId);
                     reloadButtonClick(this.url);"
            jsselect="reloadButton"
            jsvalues=".url:reloadUrl; .trackingId:reloadTrackingId"
            jscontent="msg"></button>
        <button id="show-saved-copy-button"
            class="blue-button text-button"
            onclick="showSavedCopyButtonClick()"
            jsselect="showSavedCopyButton"
            jscontent="msg" jsvalues="title:title; .primary:primary">
        </button>
        <button id="show-offline-pages-button"
            class="gray-button text-button"
            onclick="showOfflinePagesButtonClick()"
            jsselect="showOfflinePagesButton"
            jscontent="msg">
        </button>
      </div>
      <button id="details-button" class="text-button small-link"
         onclick="detailsButtonClick(); toggleHelpBox()" jscontent="details"
         jsdisplay="(suggestionsDetails && suggestionsDetails.length > 0) || diagnose"
         jsvalues=".detailsText:details; .hideDetailsText:hideDetails;"></button>
    </div>
    <div id="details" class="hidden">
      <div class="suggestions" jsselect="suggestionsDetails">
        <div class="suggestion-header" jsvalues=".innerHTML:header"></div>
        <div class="suggestion-body" jsvalues=".innerHTML:body"></div>
      </div>
      <button class="text-button" id="diagnose-button"
          onclick="diagnoseErrors()" jscontent="diagnose"
          jsdisplay="diagnose"></button>
      <div id="diagnose-frame" class="hidden"></div>
    </div>
  </div>
  <div id="sub-frame-error">
    <!-- Show details when hovering over the icon, in case the details are
         hidden because they're too large. -->
    <img class="icon" jseval="updateIconClass(this.classList, iconClass)"
        jsvalues=".title:errorDetails">
    <div id="sub-frame-error-details" jsvalues=".innerHTML:errorDetails"></div>
  </div>

  <div id="offline-resources">
    <img id="offline-resources-1x" src="data:image/png;base64,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">
    <img id="offline-resources-2x" src="data:image/png;base64,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">
    <template id="audio-resources">
      <audio id="offline-sound-press" src="data:audio/mpeg;base64,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"></audio>
      <audio id="offline-sound-hit" src="data:audio/mpeg;base64,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"></audio>
      <audio id="offline-sound-reached" src="data:audio/mpeg;base64,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"></audio>
    </template>
  </div>
</body>
</html>
<!doctype html>
<head>
<link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
<style>
  body {
    margin: 0px;
    width: 0px;
  }
  .row {
    display: table-row;
    vertical-align: inherit;
  }
  #header, #footer {
    display: table;
    table-layout:fixed;
    width: inherit;
  }
  #header {
    vertical-align: top;
  }
  #footer {
    vertical-align: bottom;
  }
  .text {
    display: table-cell;
    font-size: 8px;
    vertical-align: inherit;
    white-space: nowrap;
  }
  #page_number {
    text-align: right;
  }
  #title {
    text-align: center;
  }
  #date, #url {
    padding-left: 0.7cm;
    padding-right: 0.1cm;
  }
  #title, #page_number {
    padding-left: 0.1cm;
    padding-right: 0.7cm;
  }
  #title, #url {
    overflow: hidden;
    text-overflow: ellipsis;
  }
  #title, #date {
    padding-bottom: 0cm;
    padding-top: 0.4cm;
  }
  #page_number, #url {
    padding-bottom: 0.4cm;
    padding-top: 0cm;
  }
</style>
<script>

function pixels(value) {
  return value + 'px';
}
  
function setup(options) {
  var body = document.querySelector('body');
  var header = document.querySelector('#header');
  var content = document.querySelector('#content');
  var footer = document.querySelector('#footer');

  body.style.width = pixels(options['width']);
  body.style.height = pixels(options['height']);
  header.style.height = pixels(options['topMargin']);
  content.style.height = pixels(options['height'] - options['topMargin'] -
                                options['bottomMargin']);
  footer.style.height = pixels(options['bottomMargin']);

  document.querySelector('#date span').innerText =
      new Date(options['date']).toLocaleDateString();
  document.querySelector('#title span').innerText = options['title'];

  document.querySelector('#url span').innerText = options['url'];
  document.querySelector('#page_number span').innerText = options['pageNumber'];

  // Reduce date and page number space to give more space to title and url.
  document.querySelector('#date').style.width =
      pixels(document.querySelector('#date span').offsetWidth);
  document.querySelector('#page_number').style.width =
      pixels(document.querySelector('#page_number span').offsetWidth);

  // Hide text if it doesn't fit into expected margins.
  if (header.offsetHeight > options['topMargin'] + 1) {
    header.style.display = 'none';
    content.style.height = pixels(options['height'] - options['bottomMargin']);
  }
  if (footer.offsetHeight > options['bottomMargin'] + 1) {
    footer.style.display = 'none';
  }
}

</script>
</head>
<body>
  <div id="header">
    <div class="row">
      <div id="date" class="text"><span/></div>
      <div id="title" class="text"><span/></div>
    </div>
  </div>
  <div id="content">
  </div>
  <div id="footer">
    <div class="row">
      <div id="url" class="text"><span/></div>
      <div id="page_number" class="text"><span/></div>
    </div>
  </div>
</body>
</html>
<!DOCTYPE HTML>
<html>
<head>
  <meta charset="utf-8">
  <title>Proximity Auth Debug</title>
  <meta name="viewport"
        content="width=device-width, initial-scale=1">
  <link href="proximity_auth.css" rel="stylesheet">
  <link href="chrome://resources/css/roboto.css" rel="stylesheet">
  <link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
  <link href="content-panel.html" rel="import">
  <link href="log-panel.html" rel="import">
  <script src="cryptauth_interface.js"></script>
</head>
<body class="layout horizontal fullbleed">
  <content-panel id="content" class="flex"></content-panel>
  <log-panel id="logs"></log-panel>
</body>
</html>
/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

html, body {
  width: 100%;
  height: 100%;
  margin: 0;
  padding: 0;
  overflow: hidden;
  font-family: "Roboto", sans-serif;
}

#logs {
  width: 40%;
  border-left: 1px solid rgba(0,0,0,0.12);
}
<link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/communication-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-icon-button/paper-icon-button.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-toolbar/paper-toolbar.html" rel="import">
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">
<link href="log-buffer.html" rel="import">

<dom-module id="log-panel">
  <style>
    :host {
      height: 100vh;
      display: flex;
      flex-direction: column;
    }

    paper-toolbar {
      background-color: #069BDE;
      box-shadow: 0px 3px 2px rgba(0, 0, 0, 0.2);
      height: 48px;
      margin: 0;
    }

    paper-toolbar paper-icon-button {
      padding: 0;
    }

    #list {
      overflow-y: scroll;
    }

    .list-item {
      border-bottom: 1px solid rgba(0, 0, 0, 0.12);
      font-family: monospace;
      font-size: 12px;
      padding: 15px 30px;
    }

    .list-item[severity="1"] {
      background-color: #fffcef;
      color: #312200;
    }

    .list-item[severity="2"] {
      background-color: #fff1f1;
      color: #ef0000;
    }

    .item-metadata {
      color: #888888;
      font-size: 10px;
    }

    .item-log {
      margin: 0;
      overflow: hidden;
    }

    .list-item:hover .item-log {
      overflow: auto;
      text-overflow: clip;
    }
  </style>

  <template>
    <paper-toolbar class="layout horizontal end-justified center"
                  on-click="clearLogs_">
      <paper-icon-button icon="communication:clear-all"></paper-icon-button>
    </paper-toolbar>

    <log-buffer id='logBuffer' logs="{{logs}}"></log-buffer>
    <div id="list" class="flex">
      <template is="dom-repeat" items="[[logs]]">
        <div class="list-item" severity$="[[item.severity]]">
          <div class="item-metadata layout horizontal">
            <div>[[item.time]]</div>
            <div class="flex"></div>
            <div>[[computeFileAndLine_(item)]]</div>
          </div>
          <pre class="item-log flex">[[item.text]]</pre>
        </div>
      </template>
    </div>
  </template>
  <script src="log-panel.js"></script>
</dom-module>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'log-panel',
  properties: {
    /**
     * List of displayed logs.
     * @type {Array<{{
     *    text: string,
     *    date: string,
     *    source: string
     * }}>}
     */
    logs: Array,
  },

  observers: [
    'logsChanged_(logs.*)',
  ],

  /**
   * @type {boolean}
   * @private
   */
  isScrollAtBottom_: true,

  /**
   * Called after the Polymer element is initialized.
   */
  ready: function() {
    this.$.list.onscroll = this.onScroll_.bind(this);
    this.async(this.scrollToBottom_);
  },

  /**
   * Called when the list of logs change.
   */
  logsChanged_: function() {
    if (this.isScrollAtBottom_)
      this.async(this.scrollToBottom_);
  },

  /**
   * Clears the logs.
   * @private
   */
  clearLogs_: function() {
    this.$.logBuffer.clearLogs();
  },

  /**
   * Event handler when the list is scrolled.
   * @private
   */
  onScroll_: function() {
    var list = this.$.list;
    this.isScrollAtBottom_ =
        list.scrollTop + list.offsetHeight == list.scrollHeight;
  },

  /**
   * Scrolls the logs container to the bottom.
   * @private
   */
  scrollToBottom_: function() {
    this.$.list.scrollTop = this.$.list.scrollHeight;
  },

  /**
   * @param {LogMessage} log
   * @return {string} The filename stripped of its preceeding path concatenated
   *     with the line number of the log.
   * @private
   */
  computeFileAndLine_: function(log) {
    var directories = log.file.split('/');
    return directories[directories.length - 1] + ':' + log.line;
  },
});
<link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
<link href="chrome://resources/polymer/v1_0/neon-animation/animations/fade-in-animation.html" rel="import">
<link href="chrome://resources/polymer/v1_0/neon-animation/animations/fade-out-animation.html" rel="import">
<link href="chrome://resources/polymer/v1_0/neon-animation/neon-animatable.html" rel="import">
<link href="chrome://resources/polymer/v1_0/neon-animation/neon-animated-pages.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-material/paper-material.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-tabs/paper-tabs.html" rel="import">
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">
<link href="device-list.html" rel="import">
<link href="eligible-devices.html" rel="import">
<link href="local-state.html" rel="import">
<link href="reachable-devices.html" rel="import">

<dom-module id="content-panel">
  <style>
    :host {
      background-color: #ececec;
      display: flex;
      flex-direction: column;
    }

    #pages {
      overflow-y: auto;
    }

    paper-tabs {
      background-color: rgb(3, 169, 244);
      box-shadow: 0 3px 2px rgba(0, 0, 0, 0.2);
      color: white;
      font-size: 14px;
      font-weight: 500;
      width: 100%;
    }

    local-state,
    eligible-devices,
    reachable-devices {
      width: 80%;
    }
  </style>

  <template>
    <paper-tabs id="tabs" selected="{{selected_}}">
      <paper-tab>LOCAL STATE</paper-tab>
      <paper-tab>ELIGIBLE PHONES</paper-tab>
      <paper-tab>REACHABLE PHONES</paper-tab>
    </paper-tabs>

    <neon-animated-pages id="pages" selected="[[selected_]]"
        entry-animation="fade-in-animation"
        exit-animation="fade-out-animation"
        on-selected-item-changed="onSelectedPageChanged_"
        class="flex">
      <neon-animatable class="layout vertical center">
        <local-state id="local-state" class="flex"></local-state>
      </neon-animatable>

      <neon-animatable class="layout vertical center">
        <eligible-devices id="eligible-devices" class="flex"></eligible-devices>
        </eligible-devices>
      </neon-animatable>

      <neon-animatable class="layout vertical center">
        <reachable-devices id="reachable-devices" class="flex">
        </reachable-devices>
      </neon-animatable>
    </neon-animated-pages>
  </template>
  <script src="content-panel.js"></script>
</dom-module>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'content-panel',

  properties: {
    /**
     * The index of the selected page that is currently shown.
     * @private
     */
    selected_: {
      type: Number,
      value: 0,
    }
  },

  /**
   * Called when a page transition event occurs.
   * @param {Event} event
   * @private
   */
  onSelectedPageChanged_: function(event) {
    var newPage = event.detail.value instanceof Element &&
                  event.detail.value.children[0];
    if (newPage && newPage.activate != null) {
      newPage.activate();
    }
  }
});
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-material/paper-material.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-button/paper-button.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-icon-button/paper-icon-button.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icon/iron-icon.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/iron-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/notification-icons.html" rel="import">
<link href="device-list.html" rel="import">

<dom-module id="local-state">
  <style>
    paper-material {
      background-color: white;
    }

    #card-row {
      margin-top: 22px;
    }

    #enrollment-card {
      margin-right: 30px;
    }

    .card-content {
      margin: 24px 16px 0 16px;
    }

    .card-title {
      font-size: 20px;
    }

    .card-subtitle {
      color: #767676;
      font-size: 14px;
      margin-bottom: 16px;
    }

    paper-button {
      margin: 8px;
    }

    .card-icon {
      color: green;
      height: 90px;
      margin: 16px 16px 0 0;
      width: 90px;
    }

    .next-sync-icon {
      color: black;
      margin-right: 4px;
    }

    .next-refresh {
      height: 40px;
    }

    iron-icon[error-icon] {
      color: orange;
    }
  </style>

  <template>
    <div id="card-row" class="layout horizontal">

      <!-- CryptAuth Enrollment Info Card -->
      <paper-material id="enrollment-card" class="layout vertical flex">
        <div class="layout horizontal">
          <div class="card-content layout vertical flex">
            <div class="card-title">Enrollment</div>
            <div class="card-subtitle">
              <span>[[getLastSyncTimeString_(enrollmentState_, "Never enrolled")]]</span>
            </div>
            <div class="next-refresh layout horizontal center flex">
              <iron-icon class="next-sync-icon"
                         icon="[[getNextSyncIcon_(enrollmentState_)]]">
              </iron-icon>
              <span>[[getNextEnrollmentString_(enrollmentState_)]]</span>
            </div>
          </div>
          <iron-icon class="card-icon"
                     icon="[[getIconForSuccess_(enrollmentState_)]]"
                     error-icon$="[[enrollmentState_.recoveringFromFailure]]">
          </iron-icon>
        </div>
        <paper-button class="self-start" on-click="forceEnrollment_">
            Force Enroll
        </paper-button>
      </paper-material>

      <!-- Device Sync Info Card -->
      <paper-material id="device-card" class="layout vertical flex">
        <div class="layout horizontal flex">
          <div class="card-content layout vertical flex">
            <div class="card-title">Device Sync</div>
            <div class="card-subtitle">
              <span>[[getLastSyncTimeString_(deviceSyncState_, "Never synced")]]</span>
            </div>
            <div class="layout horizontal center flex">
              <iron-icon class="next-sync-icon"
                         icon="[[getNextSyncIcon_(deviceSyncState_)]]">
              </iron-icon>
              <span>[[getNextEnrollmentString_(deviceSyncState_)]]</span>
            </div>
          </div>
          <iron-icon class="card-icon"
                     icon="[[getIconForSuccess_(deviceSyncState_)]]"
                     error-icon$="[[deviceSyncState_.recoveringFromFailure]]">
          </iron-icon>
        </div>
        <paper-button class="self-start" on-click="forceDeviceSync_">
          Force Sync
        </paper-button>
      </paper-material>
   </div>

    <device-list label="Unlock Keys" devices="[[unlockKeys_]]"></device-list>
  </template>
  <script src="local-state.js"></script>
</local-state>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'local-state',
  properties: {
    /**
     * The current CryptAuth enrollment status.
     * @type {{
     *   lastSuccessTime: ?number,
     *   nextRefreshTime: ?number,
     *   recoveringFromFailure: boolean,
     *   operationInProgress: boolean,
     * }} SyncState
     */
    enrollmentState_: {
      type: Object,
      value: {
        lastSuccessTime: null,
        nextRefreshTime: null,
        recoveringFromFailure: true,
        operationInProgress: false,
      },
    },

    /**
     * The current CryptAuth device sync status.
     * @type {SyncState}
     */
    deviceSyncState_: {
      type: Object,
      value: {
        lastSuccessTime: null,
        nextRefreshTime: null,
        recoveringFromFailure: true,
        operationInProgress: false,
      },
    },

    /**
     * List of unlock keys that can unlock the local device.
     * @type {Array<DeviceInfo>}
     */
    unlockKeys_: {
      type: Array,
      value: [
        {
         publicKey: 'CAESRQogOlH8DgPMQu7eAt-b6yoTXcazG8mAl6SPC5Ds-LTULIcSIQDZ' +
                    'DMqsoYRO4tNMej1FBEl1sTiTiVDqrcGq-CkYCzDThw==',
         friendlyDeviceName: 'LGE Nexus 4',
         bluetoothAddress: 'C4:43:8F:12:07:07',
         unlockKey: true,
         unlockable: false,
         connectionStatus: 'connected',
         remoteState: {
           userPresent: true,
           secureScreenLock: true,
           trustAgent: true
         },
        },
      ],
    },
  },

  /**
   * Called when the page is about to be shown.
   */
  activate: function() {
    LocalStateInterface = this;
    chrome.send('getLocalState');
  },

  /**
   * Immediately forces an enrollment attempt.
   */
  forceEnrollment_: function() {
    chrome.send('forceEnrollment');
  },

  /**
   * Immediately forces an device sync attempt.
   */
  forceDeviceSync_: function() {
    chrome.send('forceDeviceSync');
  },

  /**
   * Called when the enrollment state changes.
   * @param {SyncState} enrollmentState
   */
  onEnrollmentStateChanged: function(enrollmentState) {
    this.enrollmentState_ = enrollmentState;
  },

  /**
   * Called when the device sync state changes.
   * @param {SyncState} deviceSyncState
   */
  onDeviceSyncStateChanged: function(deviceSyncState) {
    this.deviceSyncState_ = deviceSyncState;
  },

  /**
   * Called when the locally stored unlock keys change.
   * @param {Array<DeviceInfo>} unlockKeys
   */
  onUnlockKeysChanged: function(unlockKeys) {
    this.unlockKeys_ = unlockKeys;
  },

  /**
   * Called for the chrome.send('getSyncStates') response.
   * @param {SyncState} enrollmentState
   * @param {SyncState} deviceSyncState
   * @param {Array<DeviceInfo>} unlockKeys
   */
  onGotLocalState: function(enrollmentState, deviceSyncState, unlockKeys) {
    this.enrollmentState_ = enrollmentState;
    this.deviceSyncState_ = deviceSyncState;
    this.unlockKeys_ = unlockKeys;
  },

  /**
   * @param {SyncState} syncState The enrollment or device sync state.
   * @param {string} neverSyncedString String returned if there has never been a
   *     last successful sync.
   * @return {string} The formatted string of the last successful sync time.
   */
  getLastSyncTimeString_: function(syncState, neverSyncedString) {
    if (syncState.lastSuccessTime == 0)
      return neverSyncedString;
    var date = new Date(syncState.lastSuccessTime);
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
  },

  /**
   * @param {SyncState} syncState The enrollment or device sync state.
   * @return {string} The formatted string to be displayed.
   */
  getNextEnrollmentString_: function(syncState) {
    var deltaMillis = syncState.nextRefreshTime;
    if (deltaMillis == null)
      return 'unknown';
    if (deltaMillis == 0)
      return 'sync in progress...';

    var seconds = deltaMillis / 1000;
    if (seconds < 60)
      return Math.round(seconds) + ' seconds to refresh';

    var minutes = seconds / 60;
    if (minutes < 60)
      return Math.round(minutes) + ' minutes to refresh';

    var hours = minutes / 60;
    if (hours < 24)
      return Math.round(hours) + ' hours to refresh';

    var days = hours / 24;
    return Math.round(days) + ' days to refresh';
  },

  /**
   * @param {SyncState} syncState The enrollment or device sync state.
   * @return {string} The icon to show for the current state.
   */
  getNextSyncIcon_: function(syncState) {
    return syncState.operationInProgress ? 'icons:refresh' : 'icons:schedule';
 },

  /**
   * @param {SyncState} syncState The enrollment or device sync state.
   * @return {string} The icon id representing whether the last sync is
   *     successful.
   */
  getIconForSuccess_: function(syncState) {
    return syncState.recoveringFromFailure ?
        'icons:error' : 'icons:cloud-done';
  },
});

// Interface with the native WebUI component for getting the local state and
// being notified when the local state changes.
// The local state refers to state stored on the device rather than online in
// CryptAuth. This state includes the enrollment and device sync states, as well
// as the list of unlock keys.
LocalStateInterface = {
  /**
   * Called when the enrollment state changes. For example, when a new
   * enrollment is initiated.
   * @type {function(SyncState)}
   */
  onEnrollmentStateChanged: function(enrollmentState) {},

  /**
   * Called when the device state changes. For example, when a new device sync
   * is initiated.
   * @type {function(DeviceSyncState)}
   */
  onDeviceSyncStateChanged: function(deviceSyncState) {},

  /**
   * Called when the locally stored unlock keys changes.
   * @type {function(Array<DeviceInfo>)}
   */
  onUnlockKeysChanged: function(unlockKeys) {},

  /**
   * Called in response to chrome.send('getLocalState') with the local state.
   * @type {function(SyncState, SyncState, Array<DeviceInfo>)}
   */
  onGotLocalState: function(enrollmentState, deviceSyncState, unlockKeys) {},
};
<link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icon/iron-icon.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/device-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/hardware-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/iron-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/iron-icons/notification-icons.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-button/paper-button.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-dialog/paper-dialog.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-icon-button/paper-icon-button.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-material/paper-material.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-spinner/paper-spinner.html" rel="import">
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">

<dom-module id="device-list">
  <style>
    .devices-label {
      color: rgb(153, 153, 153);
      font-size: 16px;
      margin-top: 20px;
      padding: 10px 22px;
    }

    paper-material {
      background-color: white;
    }

    .item {
      border-bottom: 1px solid rgba(0, 0, 0, 0.12);
      height: 72px;
    }

    .name {
      margin: 0 8px 2px 0;
    }

    .public-key {
      color: #767676;
      font-size: 13px;
      height: 16px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      width: 300px;
    }

    .phone-lock {
      height: 16px;
      width: 16px;
      vertical-align: top;
    }

    .end-icon {
      margin: 0 14px;
    }

    paper-icon-button, iron-icon {
      opacity: 0.25;
    }

    paper-icon-button:hover, iron-icon:hover {
      opacity: 1;
    }

    .ineligibility-icons {
      margin: 0 22px;
    }

    .ineligibility-icon {
      color: orange;
      opacity: 0.5;
    }

    iron-tooltip::shadow .iron-tooltip {
      font-size: 14px;
      line-height: 18px;
      padding: 8px 16px;
    }

    #unlock-key-dialog {
      width: 500px;
    }

    #dialog-text {
      color: #7E7E7E;
    }

    #dialog-buttons {
      height: 36px;
      display: flex;
      flex-direction: row;
      justify-content: flex-end;
      margin: 8px;
      padding: 0px;
      color: #377EF3;
    }

    #dialog-spinner {
      width: 20px;
      height: 20px;
      padding-right: 14px;
    }
  </style>

  <template>
    <div class="devices-label">[[label]]</div>

    <paper-dialog id="unlock-key-dialog"
        no-cancel-on-outside-click="true"
        with-backdrop="true"
        no-cancel-on-esc-key="true">
      <div id="dialog-text">
        <span hidden$="[[deviceForDialog_.unlockKey]]">
          Make <span>[[deviceForDialog_.friendlyDeviceName]]</span> an unlock
          key.
        </span>
        <span hidden$="[[!deviceForDialog_.unlockKey]]">
          Remove <span>[[deviceForDialog_.friendlyDeviceName]]</span> as an
          unlock key.
        </span>
      </div>
      <div id="dialog-buttons">
        <paper-button dialog-dismiss disabled$="[[toggleUnlockKeyInProgress_]]">
          Cancel
        </paper-button>
        <paper-button id="unlock-key-button" on-click="toggleUnlockKey_"
            disabled$="[[toggleUnlockKeyInProgress_]]">
          <span hidden$="[[deviceForDialog_.unlockKey]]">
            Make Unlock Key
          </span>
          <span hidden$="[[!deviceForDialog_.unlockKey]]">
            Remove Unlock Key
          </span>
        </paper-button>
      </div>
    </paper-dialog>

    <paper-material>
      <template is="dom-repeat" items="[[devices]]">
        <div class="item layout horizontal center">
          <paper-icon-button class="end-icon"
              icon="[[getIconForUnlockKey_(item)]]"
              on-click="showUnlockKeyDialog_">
          </paper-icon-button>

          <div class="info">
            <div class="layout horizontal center">
              <span class="name">[[item.friendlyDeviceName]]</span>
              <core-tooltip position="top" hidden$="[[!item.remoteState]]">
                <iron-icon icon="[[getIconForRemoteState_(item.remoteState)]]"
                           class="phone-lock flex"></iron-icon>
                <!--TODO(tengs): Reimplement the tooltip after it is ported to
                                 Polymer 1.0-->
                <div hidden>
                  <div>
                    User Present:
                    <span>
                      [[getUserPresenceText_(item.remoteState.userPresent)]]
                    </span>
                  </div>
                  <div>
                    Secure Screen Lock:
                    <span>
                      [[getScreenLockText_(item.remoteState.secureScreenLock)]]
                    </span>
                  </div>
                  <div>
                    Trust Agent:
                    <span>
                      [[getTrustAgentText_(item.remoteState.trustAgent)]]
                    </span>
                  </div>
                </div>
              </core-tooltip>
            </div>
            <div class="public-key">[[item.publicKey]]</div>
          </div>

          <div class="flex"></div>
          <div class="ineligibility-icons"
              hidden$="[[!item.ineligibilityReasons]]">
            <template is="dom-repeat" items="[[item.ineligibilityReasons]]">
              <core-tooltip label="[[prettifyReason_(item)]]" position="top">
                <iron-icon icon="[[getIconForIneligibilityReason_(item)]]")
                    class="ineligibility-icon">
                </iron-icon>
              </core-tooltip>
            </template>
          </div>
          <paper-icon-button class="end-icon"
              bluetooth-address="[[item.bluetoothAddress]]"
              on-click="toggleConnection_"
              icon="[[getIconForConnection_(item.connectionStatus)]]"
              hidden$="{{!item.unlockKey}}">
          </paper-icon-button>
        </div>
      </template>
    </paper-material>
  </template>
  <script src="device-list.js"></script>
</dom-module>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'device-list',

  properties: {
    /**
     * The label of the list to be displayed.
     * @type {string}
     */
    label: {
      type: String,
      value: 'Device List',
    },

    /**
     * Info of the devices contained in the list.
     * @type {Array<DeviceInfo>}
     */
    devices: Array,

    /**
     * Set with the selected device when the unlock key dialog is opened.
     */
    deviceForDialog_: {
      type: Object,
      value: null
    },

    /**
     * True if currently toggling a device as an unlock key.
     */
    toggleUnlockKeyInProgress_: {
      type: Boolean,
      value: false,
    },
  },

  /**
   * Shows the toggle unlock key dialog when the toggle button is pressed for an
   * item.
   * @param {Event} event
   */
  showUnlockKeyDialog_: function(event) {
    this.deviceForDialog_ = event.model.item;
    var dialog = this.querySelector('#unlock-key-dialog');
    dialog.open();
  },

  /**
   * Called when the unlock key dialog button is clicked to make the selected
   * device an unlock key or remove it as an unlock key.
   * @param {Event} event
   */
  toggleUnlockKey_: function(event) {
    if (!this.deviceForDialog_)
      return;
    this.toggleUnlockKeyInProgress_ = true;
    CryptAuthInterface.addObserver(this);

    var publicKey = this.deviceForDialog_.publicKey;
    var makeUnlockKey = !this.deviceForDialog_.unlockKey;
    CryptAuthInterface.toggleUnlockKey(publicKey, makeUnlockKey);
  },

  /**
   * Called when the toggling the unlock key completes, so we can close the
   * dialog.
   */
  onUnlockKeyToggled: function() {
    this.toggleUnlockKeyInProgress_ = false;
    CryptAuthInterface.removeObserver(this);
    var dialog = this.querySelector('#unlock-key-dialog');
    dialog.close();
  },

  /**
   * Handles when the toggle connection button is clicked for a list item.
   * @param {Event} event
   */
  toggleConnection_: function(event) {
    var deviceInfo = event.model.item;
    chrome.send('toggleConnection', [deviceInfo.publicKey]);
  },

  /**
   * @param {string} reason The device ineligibility reason.
   * @return {string} The prettified ineligibility reason.
   * @private
   */
  prettifyReason_: function(reason) {
    if (reason == null || reason == '')
      return '';
    var reasonWithSpaces = reason.replace(/([A-Z])/g, ' $1');
    return reasonWithSpaces[0].toUpperCase() + reasonWithSpaces.slice(1);
  },

  /**
   * @param {string} connectionStatus The Bluetooth connection status.
   * @return {string} The icon id to be shown for the connection state.
   * @private
   */
  getIconForConnection_: function(connectionStatus) {
    switch (connectionStatus) {
      case 'connected':
        return 'device:bluetooth-connected';
      case 'disconnected':
        return 'device:bluetooth';
      case 'connecting':
        return 'device:bluetooth-searching';
      default:
        return 'device:bluetooth-disabled';
    }
  },

  /**
   * @param {DeviceInfo} device
   * @return {string} The icon id to be shown for the unlock key state of the
   *     device.
   */
  getIconForUnlockKey_: function(device) {
    return 'hardware:phonelink' + (!device.unlockKey ? '-off' : '');
  },

  /**
   * @param {Object} remoteState The remote state of the device.
   * @return {string} The icon representing the state.
   */
  getIconForRemoteState_: function(remoteState) {
    if (remoteState != null && remoteState.userPresent &&
        remoteState.secureScreenLock && remoteState.trustAgent) {
      return 'icons:lock-open';
    } else {
      return 'icons:lock-outline';
    }
  },

  /**
   * @param {string} reason The device ineligibility reason.
   * @return {string} The icon id to be shown for the ineligibility reason.
   * @private
   */
  getIconForIneligibilityReason_: function(reason) {
    switch (reason) {
      case 'badOsVersion':
        return 'notification:system-update';
      case 'bluetoothNotSupported':
        return 'device:bluetooth-disabled';
      case 'deviceOffline':
        return 'device:signal-cellular-off';
      case 'invalidCredentials':
        return 'notification:sync-problem';
      default:
        return 'error';
    };
  },

  /**
   * @param {number} userPresence
   * @return {string}
   */
  getUserPresenceText_: function(userPresence) {
    var userPresenceMap = {
      0: 'User Present',
      1: 'User Absent',
      2: 'User Presence Unknown',
    };
    return userPresenceMap[userPresence];
  },

  /**
   * @param {number} screenLock
   * @return {string}
   */
  getScreenLockText_: function(screenLock) {
    var screenLockMap = {
      0: 'Secure Screen Lock Enabled',
      1: 'Secure Screen Lock Disabled',
      2: 'Secure Screen Lock State Unknown',
    };
    return screenLockMap[screenLock];
  },

  /**
   * @param {number} trustAgent
   * @return {string}
   */
  getTrustAgentText_: function(trustAgent) {
    var trustAgentMap = {
      0: 'Trust Agent Enabled',
      1: 'Trust Agent Disabled',
      2: 'Trust Agent Unsupported',
    };
    return trustAgentMap[trustAgent];
  },
});
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">

<dom-module id="log-buffer">
  <template></template>
  <script src="log-buffer.js"></script>
</polymer-element>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'log-buffer',

  properties: {
    /**
     * List of displayed logs.
     * @type {?Array<{{
     *    text: string,
     *    time: string,
     *    file: string,
     *    line: number,
     *    severity: number,
     * }}>} LogMessage
     */
    logs: {
      type: Array,
      value: [],
      notify: true,
    }
  },

  /**
   * Called when an instance is initialized.
   */
  ready: function() {
    // We assume that only one instance of log-buffer is ever created.
    LogBufferInterface = this;
    chrome.send('getLogMessages');
  },

  // Clears the native LogBuffer.
  clearLogs: function() {
    chrome.send('clearLogBuffer');
  },

  // Handles when a new log message is added.
  onLogMessageAdded: function(log) {
    this.push('logs', log);
  },

  // Handles when the logs are cleared.
  onLogBufferCleared: function() {
    this.logs = [];
  },

  // Handles when the logs are returned in response to the 'getLogMessages'
  // request.
  onGotLogMessages: function(logs) {
    this.logs = logs;
  }
});

// Interface with the native WebUI component for LogBuffer events. The functions
// contained in this object will be invoked by the browser for each operation
// performed on the native LogBuffer.
LogBufferInterface = {
  /**
   * Called when a new log message is added.
   * @type {function(LogMessage)}
   */
  onLogMessageAdded: function(log) {},

  /**
   * Called when the log buffer is cleared.
   * @type {function()}
   */
  onLogBufferCleared: function() {},

  /**
   * Called in response to chrome.send('getLogMessages') with the log messages
   * currently in the buffer.
   * @type {function(Array<LogMessage>)}
   */
  onGotLogMessages: function(messages) {},
};
<link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-spinner/paper-spinner.html" rel="import">
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">
<link href="device-list.html" rel="import">

<dom-module id="eligible-devices">
  <style>
    paper-spinner {
      margin-top: 40px;
    }
  </style>

  <template>
    <div class="layout vertical">
      <paper-spinner
          hidden$="[[!requestInProgress_]]" class="self-center" active>
      </paper-spinner>

      <device-list
          label="Eligible Phones" devices="[[eligibleDevices_]]"
          hidden$="[[!eligibleDevices_.length]]">
      </device-list>

      <device-list
          label="Ineligible Phones" devices="[[ineligibleDevices_]]"
          hidden$="[[!ineligibleDevices_.length]]">
      </device-list>
    </div>
  </template>
  <script src="eligible-devices.js"></script>
</dom-module>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'eligible-devices',

  properties: {
    /**
     * List of devices that are eligible to be used as an unlock key.
     * @type {Array<DeviceInfo>}
     * @private
     */
    eligibleDevices_: {
      type: Array,
      value: null,
    },

    /**
     * List of devices that are ineligible to be used as an unlock key.
     * @type {Array<DeviceInfo>}
     * @private
     */
    ineligibleDevices_: {
      type: Array,
      value: null,
    },

    /**
     * Whether the findEligibleUnlockDevices request is in progress.
     * @type {boolean}
     * @private
     */
    requestInProgress_: Boolean,
  },

  /**
   * Called when this element is added to the DOM.
   */
  attached: function() {
    CryptAuthInterface.addObserver(this);
  },

  /**
   * Called when this element is removed from the DOM.
   */
  detatched: function() {
    CryptAuthInterface.removeObserver(this);
  },

  /**
   * Called when the page is about to be shown.
   */
  activate: function() {
    this.requestInProgress_ = true;
    this.eligibleDevices_ = null;
    this.ineligibleDevices_ = null;
    CryptAuthInterface.findEligibleUnlockDevices();
  },

  /**
   * Called when eligible devices are found.
   * @param {Array<EligibleDevice>} eligibleDevices
   * @param {Array<IneligibleDevice>} ineligibleDevices_
   */
  onGotEligibleDevices: function(eligibleDevices, ineligibleDevices) {
    this.requestInProgress_ = false;
    this.eligibleDevices_ = eligibleDevices;
    this.ineligibleDevices_ = ineligibleDevices;
  },

  /**
   * Called when the CryptAuth request fails.
   * @param {string} errorMessage
   */
  onCryptAuthError: function(errorMessage) {
    console.error('CryptAuth request failed: ' + errorMessage);
    this.requestInProgress_ = false;
    this.eligibleDevices_ = null;
    this.ineligibleDevices_ = null;
  },
});
<link href="chrome://resources/polymer/v1_0/iron-flex-layout/iron-flex-layout.html" rel="import">
<link href="chrome://resources/polymer/v1_0/paper-spinner/paper-spinner.html" rel="import">
<link href="chrome://resources/polymer/v1_0/polymer/polymer.html" rel="import">
<link href="device-list.html" rel="import">

<dom-module id="reachable-devices">
  <style>
    paper-spinner {
      margin-top: 40px;
    }
  </style>

  <template>
    <div class="layout vertical">
      <paper-spinner
          hidden$="[[!requestInProgress_]]" class="self-center" active>
      </paper-spinner>

      <device-list
          label="Reachable Phones" devices="[[reachableDevices_]]"
          hidden$="[[!reachableDevices_.length]]">
      </device-list>
    </div>
  </template>
  <script src="reachable-devices.js"></script>
</dom-module>
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

Polymer({
  is: 'reachable-devices',

  properties: {
    /**
     * List of devices that recently responded to a CryptAuth ping.
     * @type {Array<DeviceInfo>}
     * @private
     */
    reachableDevices_: {
      type: Array,
      value: null,
    },

    /**
     * Whether the findEligibleUnlockDevices request is in progress.
     * @type {boolean}
     * @private
     */
    requestInProgress_: Boolean,
  },

  /**
   * Called when this element is added to the DOM.
   */
  attached: function() {
    CryptAuthInterface.addObserver(this);
  },

  /**
   * Called when this element is removed from the DOM.
   */
  detatched: function() {
    CryptAuthInterface.removeObserver(this);
  },

  /**
   * Called when the page is about to be shown.
   */
  activate: function() {
    this.requestInProgress_ = true;
    this.reachableDevices_ = null;
    CryptAuthInterface.findReachableDevices();
  },

  /**
   * Called when reachable devices are found.
   * @param {Array<EligibleDevice>} reachableDevices
   */
  onGotReachableDevices: function(reachableDevices) {
    this.requestInProgress_ = false;
    this.reachableDevices_ = reachableDevices;
  },

  /**
   * Called when the CryptAuth request fails.
   * @param {string} errorMessage
   */
  onCryptAuthError: function(errorMessage) {
    console.error('CryptAuth request failed: ' + errorMessage);
    this.requestInProgress_ = false;
    this.reachableDevices_ = null;
  },
});
// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Responsible for interfacing with the native component of the WebUI to make
 * CryptAuth API requests and handling the responses.
 */
CryptAuthInterface = {
  /**
   * A list of observers of CryptAuth events.
   */
  observers_: [],

  /**
   * Adds an observer.
   */
  addObserver: function(observer) {
    CryptAuthInterface.observers_.push(observer);
  },

  /**
   * Removes an observer.
   */
  removeObserver: function(observer) {
    var index = CryptAuthInterface.observers_.indexOf(observer);
    if (observer)
      CryptAuthInterface.observers_.splice(index, 1);
  },

  /**
   * Starts the findEligibleUnlockDevices API call.
   * The onGotEligibleDevices() function will be called upon success.
   */
  findEligibleUnlockDevices: function() {
    chrome.send('findEligibleUnlockDevices');
  },

  /**
   * Starts the flow to find reachable devices. Reachable devices are those that
   * respond to a CryptAuth ping.
   * The onGotReachableDevices() function will be called upon success.
   */
  findReachableDevices: function() {
    chrome.send('findReachableDevices');
  },

  /**
   * Makes the device with |publicKey| an unlock key if |makeUnlockKey| is true.
   * Otherwise, the device will be removed as an unlock key.
   */
  toggleUnlockKey: function(publicKey, makeUnlockKey) {
    chrome.send('toggleUnlockKey', [publicKey, makeUnlockKey]);
  },

  /**
   * Called by the browser when the API request fails.
   */
  onError: function(errorMessage) {
    CryptAuthInterface.observers_.forEach(function(observer) {
      if (observer.onCryptAuthError != null)
        observer.onCryptAuthError(errorMessage);
    });
  },

  /**
   * Called by the browser when a findEligibleUnlockDevices completes
   * successfully.
   * @param {Array<DeviceInfo>} eligibleDevices
   * @param {Array<DeviceInfo>} ineligibleDevices
   */
  onGotEligibleDevices: function(eligibleDevices, ineligibleDevices) {
    CryptAuthInterface.observers_.forEach(function(observer) {
      if (observer.onGotEligibleDevices != null)
        observer.onGotEligibleDevices(eligibleDevices, ineligibleDevices);
    });
  },

  /*
   * Called by the browser when the reachable devices flow completes
   * successfully.
   * @param {Array<DeviceInfo>} reachableDevices
   */
  onGotReachableDevices: function(reachableDevices) {
    CryptAuthInterface.observers_.forEach(function(observer) {
      if (observer.onGotReachableDevices != null)
        observer.onGotReachableDevices(reachableDevices);
    });
  },

  /**
   * Called by the browser when an unlock key is toggled.
   */
  onUnlockKeyToggled: function() {
    CryptAuthInterface.observers_.forEach(function(observer) {
      if (observer.onUnlockKeyToggled != null)
        observer.onUnlockKeyToggled();
    });
  },
};

// This message tells the native WebUI handler that the WebContents backing the
// WebUI has been iniitalized. This signal allows the native handler to execute
// JavaScript inside the page.
chrome.send('onWebContentsInitialized');
<html>
<head>
  <title>Interstitials</title>
</head>
<body>
  <h2>Choose an interstitial</h2>
  <h3>SSL</h3>
  <div>
    <a href="ssl?overridable=1&strict_enforcement=0">example.com (generic, overridable)</a>
  </div>
  <div>
    <a href="ssl?overridable=0&strict_enforcement=0">
      example.com (generic, non-overridable)
    </a>
  </div>
  <div>
    <a href="ssl?overridable=0&strict_enforcement=1">
      example.com (HSTS, non-overridable)
    </a>
  </div>
  <div>
    <a href="clock?clock_manipulation=2">Clock is ahead</a>
  </div>
  <div>
    <a href="clock?clock_manipulation=-2">Clock is behind</a>
  </div>
  <h3>SafeBrowsing</h3>
  <div>
    <a href="safebrowsing?type=malware">Malware</a>
  </div>
  <div>
    <a href="safebrowsing?type=phishing">Phishing</a>
  </div>
  <div>
    <a href="safebrowsing?type=clientside_malware">Client Side Malware</a>
  </div>
  <div>
    <a href="safebrowsing?type=clientside_phishing">Client Side Phishing</a>
  </div>
  <h3>Captive Portal</h3>
  <div>
    <a href="captiveportal">Captive Portal, Non-WiFi</a>
  </div>
  <div>
    <a href="captiveportal?is_wifi=1">
      Captive Portal, WiFi
    </a>
  </div>
  <div>
    <a href="captiveportal?is_wifi=1&wifi_name=CoffeeShopWiFi">
      Captive Portal, WiFi with network name "CoffeeShopWiFi"
    </a>
  </div>
</body>
</html>
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <meta name="viewport"
      content="initial-scale=1, minimum-scale=1, width=device-width">
  <title i18n-content="tabTitle"></title>
  <style>/* Copyright 2014 The Chromium Authors. All rights reserved.
   Use of this source code is governed by a BSD-style license that can be
   found in the LICENSE file. */

a {
  color: #585858;
}

.bad-clock .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,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) 1x,
      url(data:image/png;base64,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) 2x);
}

body {
  background-color: #f7f7f7;
  color: #646464;
}

body.safe-browsing {
  background-color: rgb(206, 52, 38);
  color: white;
}

button {
  -webkit-user-select: none;
  background: rgb(66, 133, 244);
  border: 0;
  border-radius: 2px;
  box-sizing: border-box;
  color: #fff;
  cursor: pointer;
  float: right;
  font-size: .875em;
  margin: 0;
  padding: 10px 24px;
  transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);
}

[dir='rtl'] button {
  float: left;
}

button:active {
  background: rgb(50, 102, 213);
  outline: 0;
}

button:hover {
  box-shadow: 0 1px 3px rgba(0, 0, 0, .50);
}

#debugging {
  display: inline;
  overflow: auto;
}

.debugging-content {
  line-height: 1em;
  margin-bottom: 0;
  margin-top: 1em;
}

.debugging-title {
  font-weight: bold;
}

#details {
  color: #696969;
  margin: 45px 0 50px;
}

#details p:not(:first-of-type) {
  margin-top: 20px;
}

#details-button {
  background: inherit;
  border: 0;
  float: none;
  margin: 0;
  padding: 10px 0;
  text-transform: uppercase;
}

#details-button:hover {
  box-shadow: inherit;
  text-decoration: underline;
}

.error-code {
  color: #696969;
  display: inline;
  font-size: .86667em;
  margin-top: 15px;
  opacity: .5;
  text-transform: uppercase;
}

#error-debugging-info {
  font-size: 0.8em;
}

h1 {
  color: #333;
  font-size: 1.6em;
  font-weight: normal;
  line-height: 1.25em;
  margin-bottom: 16px;
}

h2 {
  font-size: 1.2em;
  font-weight: normal;
}

.hidden {
  display: none;
}

html {
  -webkit-text-size-adjust: 100%;
  font-size: 125%;
}

.icon {
  background-repeat: no-repeat;
  background-size: 100%;
  height: 72px;
  margin: 0 0 40px;
  width: 72px;
}

input[type=checkbox] {
  opacity: 0;
}

input[type=checkbox]:focus ~ .checkbox {
  outline: -webkit-focus-ring-color auto 5px;
}

.interstitial-wrapper {
  box-sizing: border-box;
  font-size: 1em;
  line-height: 1.6em;
  margin: 100px auto 0;
  max-width: 600px;
  width: 100%;
}

#main-message > p {
  display: inline;
}

#extended-reporting-opt-in {
  font-size: .875em;
  margin-top: 39px;
}

#extended-reporting-opt-in label {
  position: relative;
}

.nav-wrapper {
  margin-top: 51px;
}

.nav-wrapper::after {
  clear: both;
  content: '';
  display: table;
  width: 100%;
}

.safe-browsing :-webkit-any(
    a, #details, #details-button, h1, h2, p, .small-link) {
  color: white;
}

.safe-browsing button {
  background-color: rgba(255, 255, 255, .15);
}

.safe-browsing button:active {
  background-color: rgba(255, 255, 255, .25);
}

.safe-browsing button:hover {
  box-shadow: 0 2px 3px rgba(0, 0, 0, .5);
}

.safe-browsing .error-code {
  display: none;
}

.safe-browsing .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,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) 1x,
      url(data:image/png;base64,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) 2x);
}

.small-link {
  color: #696969;
  font-size: .875em;
}

.ssl .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAMAAABiM0N1AAACClBMVEUAAADbRTfrTjvcRjraQjbcRDjbRDjbRTfaRDXZQDPZQTTbQzfaRDbcRDfbQzbVKyvZQzXaQzbaRDbIPjLaRDbYQzfXQTfaQzbZQzbbRDi/QADbRDfbQDfbJCTcRTjbQzbIPjPbQzfbQzfbRTfTQyzcRzvbQzbaRDbaQjfbQzbaQzbaQzbaRDfYQTTaRDfbQzfaRDbaQzbbQjbbQjbZQjTZQzbaQzbYQTTVQTXbRDbPQDDbQzbIPzPbQzfbRDfbNzfZRDaAAADVOSvYQDbbRDa/QCDZRDbqVUDaQTPbRDfGPDLbQjXHPjTVQEDJPTLGPTHKPTPYTjvGPDHbRDe+Oi+6OS64OC7LPzLHPTL7+/urNSv5+fm/OjD4+PjEPDHFPDG5OC67OS/DOzG8OS+9Oi/COzDrn5nAOzDtoZvBOzD9/f36+vq3OC62Ny339/fIPjLsoJr+/v6xNizx8fHFPDCnMymjMii1NyyfMSfz8/PUlI+uNivLlI+oMynDPDDUlY+zNyylMiipNCrOlI/JPjLHPTHKPjKhMijPlI+3OC2+Oy/FPTH29vaqNSq5OS319fW8Oi7AOy/BOy+sNSv////VlZD8/PzQlZDKlI+iMijCPDDYmJO0NyykMiiwNiy2OC27OS69Oi6gMSfYl5K4OC3MPzPempXBPDDqnpjy8vL09PTHPjLRlZDbmZMWYj36AAAAUnRSTlMAgQ1CaODzz4soSuj4/tkGV9303/FBM9ic8gTpHAffhc+MKtAXQbDHdMaudtc7rX7q+n93Nl/VJyu4EK9B9vwOXgISNOIIgAw32vJNgAz+84ENOFEUuAAAA25JREFUeAHsz0lPwmAQxvEWSxdaWiAIyAIBkQXc9yXuezw+3/+7mAkc1MxrZho9kPR3fZL/ZKxM5n80a05jXA+jKKyPG06tmTIT+C180/IDS689AmPU1nacKlhVR9fpwain6RxiabtfGgw7neGg5J9iaU/eKWDhYsf64m2GhYK0k7yDXJ/8HD72QeJEGJqAHDCHz0OQiTAUgxxx0+MNTbGsE4A8PbNjH+RMFJqDvPDjZUTjXBS6Alk3rLs0zkShKcitYb2jcSoKlUESw3pMY1kUqoDcG9YHGiuiEEB+n1cvlLM9FyLupp0zd7a6UMivGTtFqBQNpdculPL8dzbUbDbkQc1jQy7UNtgQUshCfxb67Lw+ltMGoygALzLJLiuvsvNMXsNPkElJZAkQIIxASHRsesEUXOy49zi99+Qdc84vBXuBPJLYefPNOfdezfzOZq//ZZpBoezZ2TXJjMW+BYPgrKxkrxwtpn0OCEHKL0/zxDTtUg9WDU6ekpOnfNlu7gQbNiEVklkQzqfmeNUKtP7lfP5CLZjsRWdr3JhEvweAIF2oaqHA8WjCiUZ71d1AB2mqKuIAwnyYp/duVNoIAEESeei8bUz2T6utUiu0EQgSecp95Inu96qjN8ViKFTxDzlOeW8N8zlFLzqh5G+/kL0vOHuH2+g1KpXAPE8mlXV/kLhDrd+Hc/jrBfO8Zx5F0dObfiC7V7+9tUaoM3R6JQ/0dNoIe4ece26/HDe24XQ6QzAMpOvpI1kOe4XoMI/Y1x84P2tdzEeBYxiyHI+HPUI/WAx5muIOh3Bq5+ilHKAXnTgyeav2l72YJ8o7PK/ZgfS0YRzRsazMV4/Dfs3vHffM76JV7HbFeJgHgWQrl8mkvK5fYR7hFHE+nDPzyHae40zKM7S4szqZfhf2/XBflJgnkfD+iViYD/Pwp+B+PiDPR+ahM/ABLb6iU0Sv6d5lOLlj5vGRCL9d556/sJdg0CuXSmUSiSXJBwRJnHPSmU+c+8phznCW6jOhe25ShY7TC9LUkaQHPl9sFfZSeIcsBoe9BpJUn/f7hqwozh1azHPi5JEiD2e/1m+7S+tw/g8I++J8IvXI3KPZ7+Nbd92lTYOMfYepAR0p8nhhtgPphkxhQNN9JaS6FJlbuOl/kft3XCVxzydg4EhP5p8+c0H+Affig2wpFL3DAAAAAElFTkSuQmCC) 1x,
      url(data:image/png;base64,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) 2x);
}

.captive-portal .icon {
  background-image: -webkit-image-set(
      url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAEgAAABICAQAAAD/5HvMAAAEyElEQVR4Xu3afWhVZRwH8LvdmjVSjKyQbKazia1k1sL6YzXoZdy28/J8f5XRC7hwEVk0hZQCXY2SipCgYgappXMQ5Ya9ELZqoEM3VHqxBkapm21jOBWmbLfudk+0Peeu2+mc3zk9514WbN9/zz18ds75Pc/vPM+JTP9NZzrT+b9Fn2nGxHrainb8ggEaRgLn0IOjaMFroloU/Ssg1yxBLZpxBD00QufpODrRiFX6IiUKXY8XcRAJsryC3nRL7EpsQJ/r0V3iSS0/MKU4D6uoQwLY/P1q4i0aYX9xVqyPRH1jbr8UtfSbE8CDqIK60wBjOEb7aDf24CBO/gN12LjJF4dM+dPAoNgMdNkUxLENQltAFViJdbTWfFTcaRbiMXyCpE1CM4vR5uAzdwB/hcxCDI5jXqGFeAadNJoGH0ErVhg3YPs46Ad9JldNS3GCAbDPkFGG/eJGWktn3PA4IUB3oxPzGY4AXSBLCSTLgfZxx2MLV1Q5tFHeWxWQDNalHulWeo4q9GW4VVShng5J0KgZ8+bkYpcEqIJk6H2y0CiW4H58gGMYQhyn8DlqUUltZIk1XF01kKUW5yhm3gVB3Q76MDaRyXBQR1aIIJnyi6jNZUgs8uTQfUiGDJIxZ9OPfxEwhAO0F70To5NR5skxrsUgWaGDZLQCHBHVxnKxWjwuivRS7McK7na1kpUZkEwUzXbFiZfZcVk8pE6xZ3u34AV5TFJUsV2ObBHU0+Q5wu0eJ21krw+eD4lzBvOYf7wLeyI5DEfLx2l1DPqoSXK8SItis/gm41mlago/9P2UApklZE0pkHhdAv6YIiB8NwEQL9HvUwCkzZHz15g5Gw8jmR0QtsizvOoACUjAIdlOZQUkHpCgDqd1gwRslhX3djZAmDsBwpATJPtDUZPqGFuyUWU453IWu7fFHZOvhjiQ+aqiDheQ/bJjXpf2oP+caRA+cgPJpkxckeZfiIHMgmirC8gee4rz0qF6KV3IJEi86R8kg0oazQ6Iv2Uyoiajt4x/qJ1BfcCe6JQZC/BQ82XvDLYHJPUEK3tmYHTmlotpbzCSwsDomDpc+mD6VgHETB3M5Or2c5xUBvGTq7P98DjBEjqrBOLbD2eDZj7o+aJdhnhooJxUbd/r1cJ+yl1mJMMBGWUSP6zlezT5SIirGNKacEB4bwKEXcxrEN7gThUGSCtILaZXMC+KiGN+5kHYITndkSj7Ko3GTIPMEvtJFKt9LDYgiZtVQTLnqUPUOJcV8KWE95df4m85ps0+CQ/ig68xN72lscF4yveCFTa5g9AXnDR5lczC1Jr+4UhuCsAv6YlH3EDURFbgPCG3Y2altmPG9FIJ8LnoOYLbXNDzgi+Myvkqii9sIuocAHZZuN9tAKi6hnaiBwka9f942y2rzFcut4tZOO+3r5J6xcVmYEeKc9xlPuC3FhDHynBA6LQ5GGA2ftnNl82RqDrIDgb1ZU4AnxzUTc7raDeWhwPCUW0BA/C7gYeP9cWqILSUX8YAgmxxIoF3tYL/DkK9Y/znwm8CI4lvRLV+dVAQThOpOPht8p+ogZ4W9/jijFFD5eXhWPgPCXjMh/pSVYD6pxaywPGOKOIBWfkYhXYKozgvy5/rYBva8avzcx19sSrgT2jJnjJMVMiUAAAAAElFTkSuQmCC) 1x,
      url(data:image/png;base64,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) 2x);
}

.checkbox {
  background: transparent;
  border: 1px solid white;
  border-radius: 2px;
  display: block;
  height: 14px;
  left: 0;
  position: absolute;
  right: 0;
  top: -1px;
  width: 14px;
}

.checkbox::before {
  background: transparent;
  border: 2px solid white;
  border-right-width: 0;
  border-top-width: 0;
  content: '';
  height: 4px;
  left: 2px;
  opacity: 0;
  position: absolute;
  top: 3px;
  transform: rotate(-45deg);
  width: 9px;
}

.ssl-opt-in .checkbox {
  border-color: #696969;
}

.ssl-opt-in .checkbox::before {
  border-color: #696969;
}

input[type=checkbox]:checked ~ .checkbox::before {
  opacity: 1;
}

@media (max-width: 700px) {
  .interstitial-wrapper {
    padding: 0 10%;
  }

  #error-debugging-info {
    overflow: auto;
  }
}

@media (max-height: 600px) {
  .error-code {
    margin-top: 10px;
  }
}

@media (max-width: 420px) {
  button,
  [dir='rtl'] button,
  .small-link {
    float: none;
    font-size: .825em;
    font-weight: 400;
    margin: 0;
    text-transform: uppercase;
    width: 100%;
  }

  #details {
    margin: 20px 0 20px 0;
  }

  #details p:not(:first-of-type) {
    margin-top: 10px;
  }

  #details-button {
    display: block;
    margin-top: 20px;
    text-align: center;
    width: 100%;
  }

  .interstitial-wrapper {
    padding: 0 5%;
  }

  #extended-reporting-opt-in {
    margin-top: 24px;
  }

  .nav-wrapper {
    margin-top: 30px;
  }
}

/**
 * Mobile specific styling.
 * Navigation buttons are anchored to the bottom of the screen.
 * Details message replaces the top content in its own scrollable area.
 */

@media (max-width: 420px) and (max-height: 736px) and (orientation: portrait) {
  #details-button {
    border: 0;
    margin: 8px 0 0;
  }

  .secondary-button {
    -webkit-margin-end: 0;
    margin-top: 16px;
  }
}

/* Fixed nav. */
@media (min-width: 240px) and (max-width: 420px) and
       (min-height: 401px) and (max-height: 736px) and (orientation:portrait),
       (min-width: 421px) and (max-width: 736px) and (min-height: 240px) and
       (max-height: 420px) and (orientation:landscape) {
  body .nav-wrapper {
    background: #f7f7f7;
    bottom: 0;
    box-shadow: 0 -22px 40px rgb(247, 247, 247);
    left: 0;
    margin: 0;
    max-width: 736px;
    padding-left: 24px;
    padding-right: 24px;
    position: fixed;
    z-index: 1;
  }

  body.safe-browsing .nav-wrapper {
    background: rgb(206, 52, 38);
    box-shadow: 0 -22px 40px rgb(206, 52, 38);
  }

  .interstitial-wrapper {
    max-width: 736px;
  }

  #details,
  #main-content {
    padding-bottom: 40px;
  }
}

@media (max-width: 420px) and (max-height: 736px) and (orientation: portrait),
       (max-width: 736px) and (max-height: 420px) and (orientation: landscape) {
  body {
    margin: 0 auto;
  }

  button,
  [dir='rtl'] button,
  button.small-link {
    font-family: Roboto-Regular,Helvetica;
    font-size: .933em;
    font-weight: 600;
    margin: 6px 0;
    text-transform: uppercase;
  }

  .nav-wrapper {
    box-sizing: border-box;
    padding-bottom: 8px;
    width: 100%;
  }

  .error-code {
    margin-top: 0;
  }

  #details {
    box-sizing: border-box;
    height: auto;
    margin: 0;
    opacity: 1;
    transition: opacity 250ms cubic-bezier(0.4, 0, 0.2, 1);
  }

  #details.hidden,
  #main-content.hidden {
    display: block;
    height: 0;
    opacity: 0;
    overflow: hidden;
    transition: none;
  }

  #details-button {
    padding-bottom: 16px;
    padding-top: 16px;
  }

  h1 {
    font-size: 1.5em;
    margin-bottom: 8px;
  }

  .icon {
    margin-bottom: 12px;
  }

  .interstitial-wrapper {
    box-sizing: border-box;
    margin: 24px auto 12px;
    padding: 0 24px;
    position: relative;
  }

  .interstitial-wrapper p {
    font-size: .95em;
    line-height: 1.61em;
    margin-top: 8px;
  }

  #main-content {
    margin: 0;
    transition: opacity 100ms cubic-bezier(0.4, 0, 0.2, 1);
  }

  .small-link {
    border: 0;
  }

  .suggested-left > #control-buttons,
  .suggested-right > #control-buttons {
    float: none;
    margin: 0;
  }
}

@media (min-height: 400px) and (orientation:portrait) {
  .interstitial-wrapper {
    margin-bottom: 145px;
  }
}

@media (min-height: 299px) and (orientation:portrait) {
  .nav-wrapper {
    padding-bottom: 16px;
  }
}

@media (min-height: 405px) and (max-height: 736px) and
       (max-width: 420px) and (orientation:portrait) {
  .icon {
    margin-bottom: 24px;
  }

  .interstitial-wrapper {
    margin-top: 64px;
  }
}

@media (min-height: 480px) and (max-width: 420px) and
       (max-height: 736px) and (orientation: portrait),
       (min-height: 338px) and (max-height: 420px) and (max-width: 736px) and
       (orientation: landscape) {
  .icon {
    margin-bottom: 24px;
  }

  .nav-wrapper {
    padding-bottom: 24px;
  }
}

@media (min-height: 500px) and (max-width: 414px) and (orientation: portrait) {
  .interstitial-wrapper {
    margin-top: 96px;
  }
}

/* Phablet sizing */
@media (min-width: 375px) and (min-height: 641px) and (max-height: 736px) and
       (max-width: 414px) and (orientation: portrait) {
  button,
  [dir='rtl'] button,
  .small-link {
    font-size: 1em;
    padding-bottom: 12px;
    padding-top: 12px;
  }

  body:not(.offline) .icon {
    height: 80px;
    width: 80px;
  }

  #details-button {
    margin-top: 28px;
  }

  h1 {
    font-size: 1.7em;
  }

  .icon {
    margin-bottom: 28px;
  }

  .interstitial-wrapper {
    padding: 28px;
  }

  .interstitial-wrapper p {
    font-size: 1.05em;
  }

  .nav-wrapper {
    padding: 28px;
  }
}

@media (min-width: 420px) and (max-width: 736px) and
       (min-height: 240px) and (max-height: 298px) and
       (orientation:landscape) {
  body:not(.offline) .icon {
    height: 50px;
    width: 50px;
  }

  .icon {
    padding-top: 0;
  }

  .interstitial-wrapper {
    margin-top: 16px;
  }

  .nav-wrapper {
    padding: 0 24px 8px;
  }
}

@media (min-width: 420px) and (max-width: 736px) and
       (min-height: 240px) and (max-height: 420px) and
       (orientation:landscape) {
  #details-button {
    margin: 0;
  }

  .interstitial-wrapper {
    margin-bottom: 70px;
  }

  .nav-wrapper {
    margin-top: 0;
  }

  #extended-reporting-opt-in {
    margin-top: 0;
  }
}

/* Phablet landscape */
@media (min-width: 680px) and (max-height: 414px) {
  .interstitial-wrapper {
    margin: 24px auto;
  }

  .nav-wrapper {
    margin: 16px auto 0;
  }
}

@media (max-height: 240px) and (orientation: landscape),
       (max-height: 480px) and (orientation: portrait),
       (max-width: 419px) and (max-height: 323px) {
  body:not(.offline) .icon {
    height: 56px;
    width: 56px;
  }

  .icon {
    margin-bottom: 16px;
  }
}

/* Small mobile screens. No fixed nav. */
@media (max-height: 400px) and (orientation: portrait),
       (max-height: 239px) and (orientation: landscape),
       (max-width: 419px) and (max-height: 399px) {
  .interstitial-wrapper {
    display: flex;
    flex-direction: column;
    margin-bottom: 0;
  }

  #details {
    flex: 1 1 auto;
    order: 0;
  }

  #main-content {
    flex: 1 1 auto;
    order: 0;
  }

  .nav-wrapper {
    flex: 0 1 auto;
    margin-top: 8px;
    order: 1;
    padding-left: 0;
    padding-right: 0;
    position: relative;
    width: 100%;
  }
}

@media (max-width: 239px) and (orientation: portrait) {
  .nav-wrapper {
    padding-left: 0;
    padding-right: 0;
  }
}
</style>
  <script>// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// // Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Assertion support.
 */

/**
 * Verify |condition| is truthy and return |condition| if so.
 * @template T
 * @param {T} condition A condition to check for truthiness.  Note that this
 *     may be used to test whether a value is defined or not, and we don't want
 *     to force a cast to Boolean.
 * @param {string=} opt_message A message to show on failure.
 * @return {T} A non-null |condition|.
 */
function assert(condition, opt_message) {
  if (!condition) {
    var message = 'Assertion failed';
    if (opt_message)
      message = message + ': ' + opt_message;
    var error = new Error(message);
    var global = function() { return this; }();
    if (global.traceAssertionsForTesting)
      console.warn(error.stack);
    throw error;
  }
  return condition;
}

/**
 * Call this from places in the code that should never be reached.
 *
 * For example, handling all the values of enum with a switch() like this:
 *
 *   function getValueFromEnum(enum) {
 *     switch (enum) {
 *       case ENUM_FIRST_OF_TWO:
 *         return first
 *       case ENUM_LAST_OF_TWO:
 *         return last;
 *     }
 *     assertNotReached();
 *     return document;
 *   }
 *
 * This code should only be hit in the case of serious programmer error or
 * unexpected input.
 *
 * @param {string=} opt_message A message to show when this is hit.
 */
function assertNotReached(opt_message) {
  assert(false, opt_message || 'Unreachable code hit');
}

/**
 * @param {*} value The value to check.
 * @param {function(new: T, ...)} type A user-defined constructor.
 * @param {string=} opt_message A message to show when this is hit.
 * @return {T}
 * @template T
 */
function assertInstanceof(value, type, opt_message) {
  // We don't use assert immediately here so that we avoid constructing an error
  // message if we don't have to.
  if (!(value instanceof type)) {
    assertNotReached(opt_message || 'Value ' + value +
                     ' is not a[n] ' + (type.name || typeof type));
  }
  return value;
}


/**
 * Alias for document.getElementById. Found elements must be HTMLElements.
 * @param {string} id The ID of the element to find.
 * @return {HTMLElement} The found element or null if not found.
 */
function $(id) {
  var el = document.getElementById(id);
  return el ? assertInstanceof(el, HTMLElement) : null;
}

// TODO(devlin): This should return SVGElement, but closure compiler is missing
// those externs.
/**
 * Alias for document.getElementById. Found elements must be SVGElements.
 * @param {string} id The ID of the element to find.
 * @return {Element} The found element or null if not found.
 */
function getSVGElement(id) {
  var el = document.getElementById(id);
  return el ? assertInstanceof(el, Element) : null;
}

/**
 * Add an accessible message to the page that will be announced to
 * users who have spoken feedback on, but will be invisible to all
 * other users. It's removed right away so it doesn't clutter the DOM.
 * @param {string} msg The text to be pronounced.
 */
function announceAccessibleMessage(msg) {
  var element = document.createElement('div');
  element.setAttribute('aria-live', 'polite');
  element.style.position = 'relative';
  element.style.left = '-9999px';
  element.style.height = '0px';
  element.innerText = msg;
  document.body.appendChild(element);
  window.setTimeout(function() {
    document.body.removeChild(element);
  }, 0);
}

/**
 * Returns the scale factors supported by this platform for webui
 * resources.
 * @return {Array} The supported scale factors.
 */
function getSupportedScaleFactors() {
  var supportedScaleFactors = [];
  if (cr.isMac || cr.isChromeOS || cr.isWindows || cr.isLinux) {
    // All desktop platforms support zooming which also updates the
    // renderer's device scale factors (a.k.a devicePixelRatio), and
    // these platforms has high DPI assets for 2.0x. Use 1x and 2x in
    // image-set on these platforms so that the renderer can pick the
    // closest image for the current device scale factor.
    supportedScaleFactors.push(1);
    supportedScaleFactors.push(2);
  } else {
    // For other platforms that use fixed device scale factor, use
    // the window's device pixel ratio.
    // TODO(oshima): Investigate if Android/iOS need to use image-set.
    supportedScaleFactors.push(window.devicePixelRatio);
  }
  return supportedScaleFactors;
}

/**
 * Generates a CSS url string.
 * @param {string} s The URL to generate the CSS url for.
 * @return {string} The CSS url string.
 */
function url(s) {
  // http://www.w3.org/TR/css3-values/#uris
  // Parentheses, commas, whitespace characters, single quotes (') and double
  // quotes (") appearing in a URI must be escaped with a backslash
  var s2 = s.replace(/(\(|\)|\,|\s|\'|\"|\\)/g, '\\$1');
  // WebKit has a bug when it comes to URLs that end with \
  // https://bugs.webkit.org/show_bug.cgi?id=28885
  if (/\\\\$/.test(s2)) {
    // Add a space to work around the WebKit bug.
    s2 += ' ';
  }
  return 'url("' + s2 + '")';
}

/**
 * Returns the URL of the image, or an image set of URLs for the profile avatar.
 * Default avatars have resources available for multiple scalefactors, whereas
 * the GAIA profile image only comes in one size.
 *
 * @param {string} path The path of the image.
 * @return {string} The url, or an image set of URLs of the avatar image.
 */
function getProfileAvatarIcon(path) {
  var chromeThemePath = 'chrome://theme';
  var isDefaultAvatar =
      (path.slice(0, chromeThemePath.length) == chromeThemePath);
  return isDefaultAvatar ? imageset(path + '@scalefactorx'): url(path);
}

/**
 * Generates a CSS -webkit-image-set for a chrome:// url.
 * An entry in the image set is added for each of getSupportedScaleFactors().
 * The scale-factor-specific url is generated by replacing the first instance of
 * 'scalefactor' in |path| with the numeric scale factor.
 * @param {string} path The URL to generate an image set for.
 *     'scalefactor' should be a substring of |path|.
 * @return {string} The CSS -webkit-image-set.
 */
function imageset(path) {
  var supportedScaleFactors = getSupportedScaleFactors();

  var replaceStartIndex = path.indexOf('scalefactor');
  if (replaceStartIndex < 0)
    return url(path);

  var s = '';
  for (var i = 0; i < supportedScaleFactors.length; ++i) {
    var scaleFactor = supportedScaleFactors[i];
    var pathWithScaleFactor = path.substr(0, replaceStartIndex) + scaleFactor +
        path.substr(replaceStartIndex + 'scalefactor'.length);

    s += url(pathWithScaleFactor) + ' ' + scaleFactor + 'x';

    if (i != supportedScaleFactors.length - 1)
      s += ', ';
  }
  return '-webkit-image-set(' + s + ')';
}

/**
 * Parses query parameters from Location.
 * @param {Location} location The URL to generate the CSS url for.
 * @return {Object} Dictionary containing name value pairs for URL
 */
function parseQueryParams(location) {
  var params = {};
  var query = unescape(location.search.substring(1));
  var vars = query.split('&');
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');
    params[pair[0]] = pair[1];
  }
  return params;
}

/**
 * Creates a new URL by appending or replacing the given query key and value.
 * Not supporting URL with username and password.
 * @param {Location} location The original URL.
 * @param {string} key The query parameter name.
 * @param {string} value The query parameter value.
 * @return {string} The constructed new URL.
 */
function setQueryParam(location, key, value) {
  var query = parseQueryParams(location);
  query[encodeURIComponent(key)] = encodeURIComponent(value);

  var newQuery = '';
  for (var q in query) {
    newQuery += (newQuery ? '&' : '?') + q + '=' + query[q];
  }

  return location.origin + location.pathname + newQuery + location.hash;
}

/**
 * @param {Node} el A node to search for ancestors with |className|.
 * @param {string} className A class to search for.
 * @return {Element} A node with class of |className| or null if none is found.
 */
function findAncestorByClass(el, className) {
  return /** @type {Element} */(findAncestor(el, function(el) {
    return el.classList && el.classList.contains(className);
  }));
}

/**
 * Return the first ancestor for which the {@code predicate} returns true.
 * @param {Node} node The node to check.
 * @param {function(Node):boolean} predicate The function that tests the
 *     nodes.
 * @return {Node} The found ancestor or null if not found.
 */
function findAncestor(node, predicate) {
  var last = false;
  while (node != null && !(last = predicate(node))) {
    node = node.parentNode;
  }
  return last ? node : null;
}

function swapDomNodes(a, b) {
  var afterA = a.nextSibling;
  if (afterA == b) {
    swapDomNodes(b, a);
    return;
  }
  var aParent = a.parentNode;
  b.parentNode.replaceChild(a, b);
  aParent.insertBefore(b, afterA);
}

/**
 * Disables text selection and dragging, with optional whitelist callbacks.
 * @param {function(Event):boolean=} opt_allowSelectStart Unless this function
 *    is defined and returns true, the onselectionstart event will be
 *    surpressed.
 * @param {function(Event):boolean=} opt_allowDragStart Unless this function
 *    is defined and returns true, the ondragstart event will be surpressed.
 */
function disableTextSelectAndDrag(opt_allowSelectStart, opt_allowDragStart) {
  // Disable text selection.
  document.onselectstart = function(e) {
    if (!(opt_allowSelectStart && opt_allowSelectStart.call(this, e)))
      e.preventDefault();
  };

  // Disable dragging.
  document.ondragstart = function(e) {
    if (!(opt_allowDragStart && opt_allowDragStart.call(this, e)))
      e.preventDefault();
  };
}

/**
 * TODO(dbeam): DO NOT USE. THIS IS DEPRECATED. Use an action-link instead.
 * Call this to stop clicks on <a href="#"> links from scrolling to the top of
 * the page (and possibly showing a # in the link).
 */
function preventDefaultOnPoundLinkClicks() {
  document.addEventListener('click', function(e) {
    var anchor = findAncestor(/** @type {Node} */(e.target), function(el) {
      return el.tagName == 'A';
    });
    // Use getAttribute() to prevent URL normalization.
    if (anchor && anchor.getAttribute('href') == '#')
      e.preventDefault();
  });
}

/**
 * Check the directionality of the page.
 * @return {boolean} True if Chrome is running an RTL UI.
 */
function isRTL() {
  return document.documentElement.dir == 'rtl';
}

/**
 * Get an element that's known to exist by its ID. We use this instead of just
 * calling getElementById and not checking the result because this lets us
 * satisfy the JSCompiler type system.
 * @param {string} id The identifier name.
 * @return {!HTMLElement} the Element.
 */
function getRequiredElement(id) {
  return assertInstanceof($(id), HTMLElement,
                          'Missing required element: ' + id);
}

/**
 * Query an element that's known to exist by a selector. We use this instead of
 * just calling querySelector and not checking the result because this lets us
 * satisfy the JSCompiler type system.
 * @param {string} selectors CSS selectors to query the element.
 * @param {(!Document|!DocumentFragment|!Element)=} opt_context An optional
 *     context object for querySelector.
 * @return {!HTMLElement} the Element.
 */
function queryRequiredElement(selectors, opt_context) {
  var element = (opt_context || document).querySelector(selectors);
  return assertInstanceof(element, HTMLElement,
                          'Missing required element: ' + selectors);
}

// Handle click on a link. If the link points to a chrome: or file: url, then
// call into the browser to do the navigation.
document.addEventListener('click', function(e) {
  if (e.defaultPrevented)
    return;

  var el = e.target;
  if (el.nodeType == Node.ELEMENT_NODE &&
      el.webkitMatchesSelector('A, A *')) {
    while (el.tagName != 'A') {
      el = el.parentElement;
    }

    if ((el.protocol == 'file:' || el.protocol == 'about:') &&
        (e.button == 0 || e.button == 1)) {
      chrome.send('navigateToUrl', [
        el.href,
        el.target,
        e.button,
        e.altKey,
        e.ctrlKey,
        e.metaKey,
        e.shiftKey
      ]);
      e.preventDefault();
    }
  }
});

/**
 * Creates a new URL which is the old URL with a GET param of key=value.
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
 * Creates a CSS -webkit-image-set for a favicon request.
 * @param {string} url The url for the favicon.
 * @param {number=} opt_size Optional preferred size of the favicon.
 * @param {string=} opt_type Optional type of favicon to request. Valid values
 *     are 'favicon' and 'touch-icon'. Default is 'favicon'.
 * @return {string} -webkit-image-set for the favicon.
 */
function getFaviconImageSet(url, opt_size, opt_type) {
  var size = opt_size || 16;
  var type = opt_type || 'favicon';
  return imageset(
      'chrome://' + type + '/size/' + size + '@scalefactorx/' + url);
}

/**
 * Creates a new URL for a favicon request for the current device pixel ratio.
 * The URL must be updated when the user moves the browser to a screen with a
 * different device pixel ratio. Use getFaviconImageSet() for the updating to
 * occur automatically.
 * @param {string} url The url for the favicon.
 * @param {number=} opt_size Optional preferred size of the favicon.
 * @param {string=} opt_type Optional type of favicon to request. Valid values
 *     are 'favicon' and 'touch-icon'. Default is 'favicon'.
 * @return {string} Updated URL for the favicon.
 */
function getFaviconUrlForCurrentDevicePixelRatio(url, opt_size, opt_type) {
  var size = opt_size || 16;
  var type = opt_type || 'favicon';
  return 'chrome://' + type + '/size/' + size + '@' +
      window.devicePixelRatio + 'x/' + url;
}

/**
 * Creates an element of a specified type with a specified class name.
 * @param {string} type The node type.
 * @param {string} className The class name to use.
 * @return {Element} The created element.
 */
function createElementWithClassName(type, className) {
  var elm = document.createElement(type);
  elm.className = className;
  return elm;
}

/**
 * webkitTransitionEnd does not always fire (e.g. when animation is aborted
 * or when no paint happens during the animation). This function sets up
 * a timer and emulate the event if it is not fired when the timer expires.
 * @param {!HTMLElement} el The element to watch for webkitTransitionEnd.
 * @param {number=} opt_timeOut The maximum wait time in milliseconds for the
 *     webkitTransitionEnd to happen. If not specified, it is fetched from |el|
 *     using the transitionDuration style value.
 */
function ensureTransitionEndEvent(el, opt_timeOut) {
  if (opt_timeOut === undefined) {
    var style = getComputedStyle(el);
    opt_timeOut = parseFloat(style.transitionDuration) * 1000;

    // Give an additional 50ms buffer for the animation to complete.
    opt_timeOut += 50;
  }

  var fired = false;
  el.addEventListener('webkitTransitionEnd', function f(e) {
    el.removeEventListener('webkitTransitionEnd', f);
    fired = true;
  });
  window.setTimeout(function() {
    if (!fired)
      cr.dispatchSimpleEvent(el, 'webkitTransitionEnd', true);
  }, opt_timeOut);
}

/**
 * Alias for document.scrollTop getter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @return {number} The Y document scroll offset.
 */
function scrollTopForDocument(doc) {
  return doc.documentElement.scrollTop || doc.body.scrollTop;
}

/**
 * Alias for document.scrollTop setter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @param {number} value The target Y scroll offset.
 */
function setScrollTopForDocument(doc, value) {
  doc.documentElement.scrollTop = doc.body.scrollTop = value;
}

/**
 * Alias for document.scrollLeft getter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @return {number} The X document scroll offset.
 */
function scrollLeftForDocument(doc) {
  return doc.documentElement.scrollLeft || doc.body.scrollLeft;
}

/**
 * Alias for document.scrollLeft setter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @param {number} value The target X scroll offset.
 */
function setScrollLeftForDocument(doc, value) {
  doc.documentElement.scrollLeft = doc.body.scrollLeft = value;
}

/**
 * Replaces '&', '<', '>', '"', and ''' characters with their HTML encoding.
 * @param {string} original The original string.
 * @return {string} The string with all the characters mentioned above replaced.
 */
function HTMLEscape(original) {
  return original.replace(/&/g, '&amp;')
                 .replace(/</g, '&lt;')
                 .replace(/>/g, '&gt;')
                 .replace(/"/g, '&quot;')
                 .replace(/'/g, '&#39;');
}

/**
 * Shortens the provided string (if necessary) to a string of length at most
 * |maxLength|.
 * @param {string} original The original string.
 * @param {number} maxLength The maximum length allowed for the string.
 * @return {string} The original string if its length does not exceed
 *     |maxLength|. Otherwise the first |maxLength| - 1 characters with '...'
 *     appended.
 */
function elide(original, maxLength) {
  if (original.length <= maxLength)
    return original;
  return original.substring(0, maxLength - 1) + '\u2026';
}

/**
 * Quote a string so it can be used in a regular expression.
 * @param {string} str The source string.
 * @return {string} The escaped string.
 */
function quoteString(str) {
  return str.replace(/([\\\.\+\*\?\[\^\]\$\(\)\{\}\=\!\<\>\|\:])/g, '\\$1');
}
</script>
  <script>// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var CAPTIVEPORTAL_CMD_OPEN_LOGIN_PAGE = 'openLoginPage';
</script>
  <script>// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

function setupSSLDebuggingInfo() {
  if (loadTimeData.getString('type') != 'SSL')
    return;

  // The titles are not internationalized because this is debugging information
  // for bug reports, help center posts, etc.
  appendDebuggingField('Subject', loadTimeData.getString('subject'));
  appendDebuggingField('Issuer', loadTimeData.getString('issuer'));
  appendDebuggingField('Expires on', loadTimeData.getString('expirationDate'));
  appendDebuggingField('Current date', loadTimeData.getString('currentDate'));
  appendDebuggingField('PEM encoded chain', loadTimeData.getString('pem'));

  $('error-code').addEventListener('click', toggleDebuggingInfo);
}
</script>
  <script>// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

// Other constants defined in security_interstitial_page.h.
var SB_BOX_CHECKED = 'boxchecked';
var SB_DISPLAY_CHECK_BOX = 'displaycheckbox';

// This sets up the Extended Safe Browsing Reporting opt-in, either for
// reporting malware or invalid certificate chains. Does nothing if the
// interstitial type is not SAFEBROWSING or SSL or CAPTIVE_PORTAL.
function setupExtendedReportingCheckbox() {
  var interstitialType = loadTimeData.getString('type');
  if (interstitialType != 'SAFEBROWSING' && interstitialType != 'SSL' &&
      interstitialType != 'CAPTIVE_PORTAL') {
    return;
  }

  if (!loadTimeData.getBoolean(SB_DISPLAY_CHECK_BOX)) {
    return;
  }

  $('opt-in-label').innerHTML = loadTimeData.getString('optInLink');
  $('opt-in-checkbox').checked = loadTimeData.getBoolean(SB_BOX_CHECKED);
  $('extended-reporting-opt-in').classList.remove('hidden');

  var className = interstitialType == 'SAFEBROWSING' ?
                  'safe-browsing-opt-in' :
                  'ssl-opt-in';
  $('extended-reporting-opt-in').classList.add(className);

  $('body').classList.add('extended-reporting-has-checkbox');

  $('opt-in-checkbox').addEventListener('click', function() {
    sendCommand($('opt-in-checkbox').checked ?
                CMD_DO_REPORT :
                CMD_DONT_REPORT);
  });
}
</script>
  <script>// Copyright 2015 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var mobileNav = false;

/**
 * For small screen mobile the navigation buttons are moved
 * below the advanced text.
 */
function onResize() {
  var helpOuterBox = document.querySelector('#details');
  var mainContent = document.querySelector('#main-content');
  var mediaQuery = '(min-width: 240px) and (max-width: 420px) and ' +
      '(max-height: 736px) and (min-height: 401px) and ' +
      '(orientation: portrait), (max-width: 736px) and ' +
      '(max-height: 420px) and (min-height: 240px) and ' +
      '(min-width: 421px) and (orientation: landscape)';

  var detailsHidden = helpOuterBox.classList.contains('hidden');
  var runnerContainer = document.querySelector('.runner-container');

  // Check for change in nav status.
  if (mobileNav != window.matchMedia(mediaQuery).matches) {
    mobileNav = !mobileNav;

    // Handle showing the top content / details sections according to state.
    if (mobileNav) {
      mainContent.classList.toggle('hidden', !detailsHidden);
      helpOuterBox.classList.toggle('hidden', detailsHidden);
      if (runnerContainer) {
        runnerContainer.classList.toggle('hidden', !detailsHidden);
      }
    } else if (!detailsHidden) {
      // Non mobile nav with visible details.
      mainContent.classList.remove('hidden');
      helpOuterBox.classList.remove('hidden');
      if (runnerContainer) {
        runnerContainer.classList.remove('hidden');
      }
    }
  }
}

function setupMobileNav() {
  window.addEventListener('resize', onResize);
  onResize();
}

document.addEventListener('DOMContentLoaded', setupMobileNav);
</script>
  <script>// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This is the shared code for the new (Chrome 37) security interstitials. It is
// used for both SSL interstitials and Safe Browsing interstitials.

var expandedDetails = false;
var keyPressState = 0;

// Should match SecurityInterstitialCommands in security_interstitial_page.h
var CMD_DONT_PROCEED = 0;
var CMD_PROCEED = 1;
// Ways for user to get more information
var CMD_SHOW_MORE_SECTION = 2;
var CMD_OPEN_HELP_CENTER = 3;
var CMD_OPEN_DIAGNOSTIC = 4;
// Primary button actions
var CMD_RELOAD = 5;
var CMD_OPEN_DATE_SETTINGS = 6;
var CMD_OPEN_LOGIN = 7;
// Safe Browsing Extended Reporting
var CMD_DO_REPORT = 8;
var CMD_DONT_REPORT = 9;
var CMD_OPEN_REPORTING_PRIVACY = 10;
// Report a phishing error.
var CMD_REPORT_PHISHING_ERROR = 11;

/**
 * A convenience method for sending commands to the parent page.
 * @param {string} cmd  The command to send.
 */
function sendCommand(cmd) {
window.domAutomationController.setAutomationId(1);
  window.domAutomationController.send(cmd);

}

/**
 * This allows errors to be skippped by typing a secret phrase into the page.
 * @param {string} e The key that was just pressed.
 */
function handleKeypress(e) {
  var BYPASS_SEQUENCE = 'badidea';
  if (BYPASS_SEQUENCE.charCodeAt(keyPressState) == e.keyCode) {
    keyPressState++;
    if (keyPressState == BYPASS_SEQUENCE.length) {
      sendCommand(CMD_PROCEED);
      keyPressState = 0;
    }
  } else {
    keyPressState = 0;
  }
}

/**
 * This appends a piece of debugging information to the end of the warning.
 * When complete, the caller must also make the debugging div
 * (error-debugging-info) visible.
 * @param {string} title  The name of this debugging field.
 * @param {string} value  The value of the debugging field.
 */
function appendDebuggingField(title, value) {
  // The values input here are not trusted. Never use innerHTML on these
  // values!
  var spanTitle = document.createElement('span');
  spanTitle.classList.add('debugging-title');
  spanTitle.innerText = title + ': ';

  var spanValue = document.createElement('span');
  spanValue.classList.add('debugging-value');
  spanValue.innerText = value;

  var pElem = document.createElement('p');
  pElem.classList.add('debugging-content');
  pElem.appendChild(spanTitle);
  pElem.appendChild(spanValue);
  $('error-debugging-info').appendChild(pElem);
}

function toggleDebuggingInfo() {
  $('error-debugging-info').classList.toggle('hidden');
}

function setupEvents() {
  var overridable = loadTimeData.getBoolean('overridable');
  var interstitialType = loadTimeData.getString('type');
  var ssl = interstitialType == 'SSL';
  var captivePortal = interstitialType == 'CAPTIVE_PORTAL';
  var badClock = ssl && loadTimeData.getBoolean('bad_clock');
  var hidePrimaryButton = badClock && loadTimeData.getBoolean(
      'hide_primary_button');

  if (ssl) {
    $('body').classList.add(badClock ? 'bad-clock' : 'ssl');
    $('error-code').textContent = loadTimeData.getString('errorCode');
    $('error-code').classList.remove('hidden');
  } else if (captivePortal) {
    $('body').classList.add('captive-portal');
  } else {
    $('body').classList.add('safe-browsing');
  }

  if (hidePrimaryButton) {
    $('primary-button').classList.add('hidden');
  } else {
    $('primary-button').addEventListener('click', function() {
      switch (interstitialType) {
        case 'CAPTIVE_PORTAL':
          sendCommand(CMD_OPEN_LOGIN);
          break;

        case 'SSL':
          if (badClock)
            sendCommand(CMD_OPEN_DATE_SETTINGS);
          else if (overridable)
            sendCommand(CMD_DONT_PROCEED);
          else
            sendCommand(CMD_RELOAD);
          break;

        case 'SAFEBROWSING':
          sendCommand(CMD_DONT_PROCEED);
          break;

        default:
          throw 'Invalid interstitial type';
      }
    });
  }

  if (overridable) {
    // Captive portal page isn't overridable.
    $('proceed-link').addEventListener('click', function(event) {
      sendCommand(CMD_PROCEED);
    });
  } else if (!ssl) {
    $('final-paragraph').classList.add('hidden');
  }

  if (ssl && overridable) {
    $('proceed-link').classList.add('small-link');
  } else if ($('help-link')) {
    // Overridable SSL page doesn't have this link.
    $('help-link').addEventListener('click', function(event) {
      if (ssl || loadTimeData.getBoolean('phishing'))
        sendCommand(CMD_OPEN_HELP_CENTER);
      else
        sendCommand(CMD_OPEN_DIAGNOSTIC);
    });
  }

  if (captivePortal) {
    // Captive portal page doesn't have details button.
    $('details-button').classList.add('hidden');
  } else {
    $('details-button').addEventListener('click', function(event) {
      var hiddenDetails = $('details').classList.toggle('hidden');

      if (mobileNav) {
        // Details appear over the main content on small screens.
        $('main-content').classList.toggle('hidden', !hiddenDetails);
      } else {
        $('main-content').classList.remove('hidden');
      }

      $('details-button').innerText = hiddenDetails ?
          loadTimeData.getString('openDetails') :
          loadTimeData.getString('closeDetails');
      if (!expandedDetails) {
        // Record a histogram entry only the first time that details is opened.
        sendCommand(CMD_SHOW_MORE_SECTION);
        expandedDetails = true;
      }
    });
  }

  // TODO(felt): This should be simplified once the Finch trial is no longer
  // needed.
  if (interstitialType == 'SAFEBROWSING' &&
      loadTimeData.getBoolean('phishing') && $('report-error-link')) {
    $('report-error-link').addEventListener('click', function(event) {
      sendCommand(CMD_REPORT_PHISHING_ERROR);
    });
  }

  preventDefaultOnPoundLinkClicks();
  setupExtendedReportingCheckbox();
  setupSSLDebuggingInfo();
  document.addEventListener('keypress', handleKeypress);
}

document.addEventListener('DOMContentLoaded', setupEvents);
</script>
</head>
<body id="body">
  <div class="interstitial-wrapper">
    <div id="main-content">
      <div class="icon" id="icon"></div>
      <div id="main-message">
        <h1 i18n-content="heading"></h1>
        <p i18n-values=".innerHTML:primaryParagraph"></p>
        <div id="debugging">
          <div id="error-code" class="error-code"></div>
          <div id="error-debugging-info" class="hidden"></div>
        </div>
      </div>
      <div id="extended-reporting-opt-in" class="hidden">
        <label>
          <input type="checkbox" id="opt-in-checkbox">
          <span class="checkbox"></span>
          <span id="opt-in-label"></span>
        </label>
      </div>
    </div>
    <div class="nav-wrapper">
      <button i18n-content="primaryButtonText" id="primary-button"></button>
      <button id="details-button" class="small-link"
          i18n-content="openDetails"></button>
    </div>
    <div id="details" class="hidden">
      <p i18n-values=".innerHTML:explanationParagraph"></p>
      <p i18n-values=".innerHTML:finalParagraph" id="final-paragraph"></p>
    </div>
  </div>
</body>
</html>
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
  <meta charset="utf-8">
  <title>Signin Internals</title>
  <script src="chrome://resources/js/cr.js"></script>
  <script src="chrome://resources/js/util.js"></script>
  <script src="chrome://resources/js/load_time_data.js"></script>
  <script src="chrome://signin-internals/strings.js"></script>
  
  <link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
  <style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

h2 {
  color: rgb(74, 142, 230);
  font-size: 100%;
  margin-bottom: 0;
}

.zero {
  color: rgb(127, 127, 127);
}

.ok {
  background: rgb(204, 255, 204);
}

tr.header {
  font-weight: bold;
}

div {
  -webkit-column-break-inside: avoid;
  display: inline-block;
  width: 100%;
}

div#signin-info {
  -webkit-columns: 2;
}

div#token-info,
div#cookie-info {
  -webkit-columns: 1;
}

table.signin-details {
  width: 100%;
}

tr:nth-child(odd) {
  background: rgb(239, 243, 255);
}

table.signin-details tr:nth-child(odd).ok {
  background: rgb(204, 243, 204);
}

@-webkit-keyframes highlight1 {
  0% {
    background: rgb(255, 255, 0);
  }
  100% {
    background: rgb(255, 255, 255);
  }
}

@-webkit-keyframes highlight2 {
  0% {
    background: rgb(155, 158, 166);
  }
  100% {
    background: rgb(239, 243, 255);
  }
}

tr[highlighted] {
  -webkit-animation-duration: 3s;
  -webkit-animation-name: highlight1;
  -webkit-animation-timing-function: linear;
}

tr[highlighted]:nth-child(odd) {
  -webkit-animation-duration: 3s;
  -webkit-animation-name: highlight2;
  -webkit-animation-timing-function: linear;
}
</style>
</head>
<body>
  <div id='signin-info'>
    <div class="section" jsselect="signin_info">
      <h2 jscontent="title"></h2>
      <table class="signin-details">
        <tr jsselect="data"
            jsvalues="class:chrome.signin.setClassFromValue($this.value)"
            jseval="chrome.signin.highlightIfChanged(this,
                      this.children[1].innerText, value)">
          <td jscontent="label"></td>
          <td jscontent="status"></td>
          <td jscontent="time" jsdisplay="time"></td>
          <td jsdisplay="time.length==0">&nbsp;</td>
        </tr>
      </table>
    </div>
  </div>
  <div id='token-info'>
    <h2>Access Token Details By Account</h2>
    <div class="tokenSection" jsselect="token_info">
      <h3 jscontent="title"></h3>
      <table class="signin-details">
        <tr class="header">
          <td>Service</td>
          <td>Requested Scopes</td>
          <td>Request Time</td>
          <td>Request Status</td>
        </tr>
        <tr jsselect="data"
            jsvalues="class:chrome.signin.setClassFromValue($this.status)"
            jseval="chrome.signin.highlightIfAnyChanged(this,
                      [[this.children[1].innerText, scopes],
                      [this.children[2].innerText, request_time],
                      [this.children[3].innerText, status]])">
          <td jscontent="service"></td>
          <td jsvalues=".innerHTML: scopes"></td>
          <td jscontent="request_time"></td>
          <td jsvalues=".innerHTML: status"></td>
        </tr>
      </table>
    </div>
  </div>
  <div id='cookie-info'>
    <h2>Accounts in Cookie Jar</h2>
    <div class="cookieSection">
      <table class="signin-details">
        <tr class="header">
          <td>Email Address</td>
          <td>Gaia ID</td>
          <td>Validity</td>
        </tr>
        <tr jsselect="cookie_info">
          <td jscontent="email"></td>
          <td jscontent="gaia_id"></td>
          <td jscontent="valid"></td>
        </tr>
      </table>
    </div>
  </div>
  <div id="account-info">
    <h2>Accounts in Token Service</h2>
    <div class="account-section">
      <table class="signin-details">
        <tr class="header">
          <td>Accound Id</td>
        </tr>
        <tr jsselect="accountInfo">
          <td jscontent="accountId"></td>
        </tr>
      </table>
    </div>
  </div>
  <script src="chrome://resources/js/i18n_template.js"></script>
  <script src="chrome://resources/js/jstemplate_compiled.js"></script>
  <script src="chrome://signin-internals/signin_internals.js"></script>
</body>
</html>
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var chrome = chrome || {};

/**
 * Organizes all signin event listeners and asynchronous requests.
 * This object has no public constructor.
 * @type {Object}
 */
chrome.signin = chrome.signin || {};

(function() {

// TODO(vishwath): This function is identical to the one in sync_internals.js
// Merge both if possible.
// Accepts a DOM node and sets its highlighted attribute oldVal != newVal
function highlightIfChanged(node, oldVal, newVal) {
  var oldStr = oldVal.toString();
  var newStr = newVal.toString();
  if (oldStr != '' && oldStr != newStr) {
    // Note the addListener function does not end up creating duplicate
    // listeners.  There can be only one listener per event at a time.
    // Reference: https://developer.mozilla.org/en/DOM/element.addEventListener
    node.addEventListener('webkitAnimationEnd',
                          function() { this.removeAttribute('highlighted'); },
                          false);
    node.setAttribute('highlighted', '');
  }
}

// Wraps highlightIfChanged for multiple conditions.
function highlightIfAnyChanged(node, oldToNewValList) {
  for (var i = 0; i < oldToNewValList.length; i++)
    highlightIfChanged(node, oldToNewValList[i][0], oldToNewValList[i][1]);
}

function setClassFromValue(value) {
  if (value == 0)
    return 'zero';
  if (value == 'Successful')
    return 'ok';

  return '';
}

// Allow signin_index.html to access the functions above using the
// corresponding chrome.signin<method> calls.
chrome.signin['highlightIfChanged'] = highlightIfChanged;
chrome.signin['highlightIfAnyChanged'] = highlightIfAnyChanged;
chrome.signin['setClassFromValue'] = setClassFromValue;

// Simplified Event class, borrowed (ok, stolen) from chrome_sync.js
function Event() {
  this.listeners_ = [];
}

// Add a new listener to the list.
Event.prototype.addListener = function(listener) {
  this.listeners_.push(listener);
};

// Remove a listener from the list.
Event.prototype.removeListener = function(listener) {
  var i = this.findListener_(listener);
  if (i == -1) {
    return;
  }
  this.listeners_.splice(i, 1);
};

// Check if the listener has already been registered so we can prevent
// duplicate registrations.
Event.prototype.hasListener = function(listener) {
  return this.findListener_(listener) > -1;
};

// Are there any listeners registered yet?
Event.prototype.hasListeners = function() {
  return this.listeners_.length > 0;
};

// Returns the index of the given listener, or -1 if not found.
Event.prototype.findListener_ = function(listener) {
  for (var i = 0; i < this.listeners_.length; i++) {
    if (this.listeners_[i] == listener) {
      return i;
    }
  }
  return -1;
};

// Fires the event.  Called by the actual event callback.  Any
// exceptions thrown by a listener are caught and logged.
Event.prototype.fire = function() {
  var args = Array.prototype.slice.call(arguments);
  for (var i = 0; i < this.listeners_.length; i++) {
    try {
      this.listeners_[i].apply(null, args);
    } catch (e) {
      if (e instanceof Error) {
        // Non-standard, but useful.
        console.error(e.stack);
      } else {
        console.error(e);
      }
    }
  }
};

// These are the events that will be registered.
chrome.signin.events = {
  'signin_manager': [
    'onSigninInfoChanged',
    'onCookieAccountsFetched'
 ]
};

for (var eventType in chrome.signin.events) {
  var events = chrome.signin.events[eventType];
  for (var i = 0; i < events.length; ++i) {
    var event = events[i];
    chrome.signin[event] = new Event();
  }
}

// Creates functions that call into SigninInternalsUI.
function makeSigninFunction(name) {
  var callbacks = [];

  // Calls the function, assuming the last argument is a callback to be
  // called with the return value.
  var fn = function() {
    var args = Array.prototype.slice.call(arguments);
    callbacks.push(args.pop());
    chrome.send(name, args);
  };

  // Handle a reply, assuming that messages are processed in FIFO order.
  // Called by SigninInternalsUI::HandleJsReply().
  fn.handleReply = function() {
    var args = Array.prototype.slice.call(arguments);
    // Remove the callback before we call it since the callback may
    // throw.
    var callback = callbacks.shift();
    callback.apply(null, args);
  };

  return fn;
}

// The list of js functions that call into SigninInternalsUI
var signinFunctions = [
  // Signin Summary Info
  'getSigninInfo'
];

for (var i = 0; i < signinFunctions.length; ++i) {
  var signinFunction = signinFunctions[i];
  chrome.signin[signinFunction] = makeSigninFunction(signinFunction);
}

chrome.signin.internalsInfo = {};

// Replace the displayed values with the latest fetched ones.
function refreshSigninInfo(signinInfo) {
  chrome.signin.internalsInfo = signinInfo;
  jstProcess(new JsEvalContext(signinInfo), $('signin-info'));
  jstProcess(new JsEvalContext(signinInfo), $('token-info'));
  jstProcess(new JsEvalContext(signinInfo), $('account-info'));
}

// Replace the cookie information with the fetched values.
function updateCookieAccounts(cookieAccountsInfo) {
  jstProcess(new JsEvalContext(cookieAccountsInfo), $('cookie-info'));
}

// On load, do an initial refresh and register refreshSigninInfo to be invoked
// whenever we get new signin information from SigninInternalsUI.
function onLoad() {
  chrome.signin.getSigninInfo(refreshSigninInfo);

  chrome.signin.onSigninInfoChanged.addListener(refreshSigninInfo);
  chrome.signin.onCookieAccountsFetched.addListener(updateCookieAccounts);
}

document.addEventListener('DOMContentLoaded', onLoad, false);
})();
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
<meta name="viewport" content="width=device-width">
<meta charset="utf-8">
<title i18n-content="blockPageTitle"></title>
<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

/* This file defines styles for form controls. The order of rule blocks is
 * important as there are some rules with equal specificity that rely on order
 * as a tiebreaker. These are marked with OVERRIDE. */

/* Copyright 2015 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

[is='action-link'] {
  cursor: pointer;
  display: inline-block;
  text-decoration: none;
}

[is='action-link']:hover {
  text-decoration: underline;
}

[is='action-link']:active {
  color: rgb(5, 37, 119);
  text-decoration: underline;
}

[is='action-link'][disabled] {
  color: #999;
  cursor: default;
  pointer-events: none;
  text-decoration: none;
}

[is='action-link'].no-outline {
  outline: none;
}


/* Default state **************************************************************/

:-webkit-any(button,
             input[type='button'],
             input[type='submit']):not(.custom-appearance),
select,
input[type='checkbox'],
input[type='radio'] {
  -webkit-appearance: none;
  -webkit-user-select: none;
  background-image: -webkit-linear-gradient(#ededed, #ededed 38%, #dedede);
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
             input[type='submit']):not(.custom-appearance),
select {
  min-height: 2em;
  min-width: 4em;
  padding-top: 1px;
  padding-bottom: 1px;
/* The following platform-specific rule is necessary to get adjacent
   * buttons, text inputs, and so forth to align on their borders while also
   * aligning on the text's baselines. */
  padding-bottom: 2px;
}

:-webkit-any(button,
             input[type='button'],
             input[type='submit']):not(.custom-appearance) {
  -webkit-padding-end: 10px;
  -webkit-padding-start: 10px;
}

select {
  -webkit-appearance: none;
  -webkit-padding-end: 24px;
  -webkit-padding-start: 10px;
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      -webkit-linear-gradient(#ededed, #ededed 38%, #dedede);
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
        input[type='submit']):not(.custom-appearance)) {
  background-image: -webkit-linear-gradient(#f0f0f0, #f0f0f0 38%, #e0e0e0);
  border-color: rgba(0, 0, 0, 0.3);
  box-shadow: 0 1px 0 rgba(0, 0, 0, 0.12),
      inset 0 1px 2px rgba(255, 255, 255, 0.95);
  color: black;
}

:enabled:hover:-webkit-any(select) {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      -webkit-linear-gradient(#f0f0f0, #f0f0f0 38%, #e0e0e0);
}

/* Active *********************************************************************/

:enabled:active:-webkit-any(
    select,
    input[type='checkbox'],
    input[type='radio'],
    :-webkit-any(
        button,
        input[type='button'],
        input[type='submit']):not(.custom-appearance)) {
  background-image: -webkit-linear-gradient(#e7e7e7, #e7e7e7 38%, #d7d7d7);
  box-shadow: none;
  text-shadow: none;
}

:enabled:active:-webkit-any(select) {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAAUklEQVQY02P4z0AMRGZGMaShwCisyhITmb8huMzfEhOxKvuvsGAh208Ik+3ngoX/FbBbClcIUcSAw21QhXxfIIrwKAMpfNsEUYRXGVCEFc6CQwBqq4CCCtU4VgAAAABJRU5ErkJggg==),
      -webkit-linear-gradient(#e7e7e7, #e7e7e7 38%, #d7d7d7);
}

/* Disabled *******************************************************************/

:disabled:-webkit-any(
    button,
    input[type='button'],
    input[type='submit']):not(.custom-appearance),
select:disabled {
  background-image: -webkit-linear-gradient(#f1f1f1, #f1f1f1 38%, #e6e6e6);
  border-color: rgba(80, 80, 80, 0.2);
  box-shadow: 0 1px 0 rgba(80, 80, 80, 0.08),
      inset 0 1px 2px rgba(255, 255, 255, 0.75);
  color: #aaa;
}

select:disabled {
  /* OVERRIDE */
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABMAAAAICAQAAACxSAwfAAAASklEQVQY02P4z0AMRGZGMaShwCisyhITG/4jw8RErMr+KyxYiFC0YOF/BeyWIikEKWLA4Ta4QogiPMpACt82QRThVQYUYYWz4BAAGr6Ii6kEPacAAAAASUVORK5CYII=),
      -webkit-linear-gradient(#f1f1f1, #f1f1f1 38%, #e6e6e6);
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
         input[type='submit']):not(.custom-appearance)) {
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
  -webkit-user-select: none;
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

extensionview {
  display: inline-block;
  height: 300px;
  width: 300px;
}
</style>
<style>/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */
body {
  background-color: rgb(247, 247, 247);
  font-size: 10pt;
  margin: 150px 60px 0 30px;
}

#main-frame-blocked {
  margin: auto;
  max-width: 600px;
  min-width: 200px;
}

h1 {
  font-size: 1.8em;
  font-weight: normal;
  margin: 5px 0 25px 0;
}

.avatar-img {
  -webkit-user-select: none;
  border: 3px solid rgb(251, 251, 251);
  border-radius: 50%;
  content: -webkit-image-set(
      url(data:image/png;base64,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) 1x,
      url(data:image/png;base64,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) 2x);
  margin-bottom: 5px;
  margin-right: 15px;
  margin-top: 5px;
  max-width: 45px;
  position: relative;
}

#feedback {
  margin-top: 50px;
}

#feedback-link {
  color: rgb(66, 133, 244);
}

#request-access-button {
  background-color: rgb(66, 133, 244);
  color: rgb(255, 255, 255);
  cursor: pointer;
  font-size: 12px;
  font-weight: bold;
  min-width: 88px;
  padding: 10px 15px;
  transition: box-shadow 200ms cubic-bezier(0.4, 0, 0.2, 1);
  transition-delay: 200ms;
}

#request-access-button:hover {
  background-color: rgb(30, 136, 229);
}

#request-access-button:active {
  background-color: rgb(25,118,210);
  box-shadow: 0 8px 17px 0 rgba(0, 0, 0, 0.2);
  transition-delay: 0s;
}

#details-button-container {
  color: rgb(97,97,97);
  cursor: pointer;
  display: inline;
  font-size: 12px;
  text-decoration: underline;
}

#button-container {
  align-items: baseline;
  display: flex;
  justify-content: space-between;
  margin-top: 60px;
}

#details {
  color: rgb(97,97,97);
  font-size: 14px;
}

#details-header {
  font-weight: bold;
}

.custodian-information {
  align-items: center;
  display: flex;
  font-size: 12px;
}

.custodian-name {
  color: rgb(97,97,97);
  padding: 1px 0;
}

.custodian-email {
  color: rgb(183, 183, 183);
  padding: 1px 0;
}

@media (max-width: 600px) {
  #button-container {
    display: flex;
    flex-flow: column;
    justify-content: flex-start;
    order: 2;
    text-transform: uppercase;
  }

  #details-button-container {
    font-weight: bold;
    margin: auto;
    order: 2;
  }

  #request-access-button {
    margin-bottom: 30px;
    order: 1;
    text-align: center
  }

  .button {
    width: 100%;
  }

  #details {
    margin: auto;
    order: 1;
  }

  .hidden-on-mobile {
    display: none;
  }

  #main-frame-blocked {
    display: flex;
    flex-flow: column;
  }

  #feedback {
    margin-top: 35px;
  }
}
</style>
<script>// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * The global object.
 * @type {!Object}
 * @const
 */
var global = this;

/** @typedef {{eventName: string, uid: number}} */
var WebUIListener;

/** Platform, package, object property, and Event support. **/
var cr = cr || function() {
  'use strict';

  /**
   * Builds an object structure for the provided namespace path,
   * ensuring that names that already exist are not overwritten. For
   * example:
   * "a.b.c" -> a = {};a.b={};a.b.c={};
   * @param {string} name Name of the object that this file defines.
   * @param {*=} opt_object The object to expose at the end of the path.
   * @param {Object=} opt_objectToExportTo The object to add the path to;
   *     default is {@code global}.
   * @return {!Object} The last object exported (i.e. exportPath('cr.ui')
   *     returns a reference to the ui property of window.cr).
   * @private
   */
  function exportPath(name, opt_object, opt_objectToExportTo) {
    var parts = name.split('.');
    var cur = opt_objectToExportTo || global;

    for (var part; parts.length && (part = parts.shift());) {
      if (!parts.length && opt_object !== undefined) {
        // last part and we have an object; use it
        cur[part] = opt_object;
      } else if (part in cur) {
        cur = cur[part];
      } else {
        cur = cur[part] = {};
      }
    }
    return cur;
  }

  /**
   * Fires a property change event on the target.
   * @param {EventTarget} target The target to dispatch the event on.
   * @param {string} propertyName The name of the property that changed.
   * @param {*} newValue The new value for the property.
   * @param {*} oldValue The old value for the property.
   */
  function dispatchPropertyChange(target, propertyName, newValue, oldValue) {
    var e = new Event(propertyName + 'Change');
    e.propertyName = propertyName;
    e.newValue = newValue;
    e.oldValue = oldValue;
    target.dispatchEvent(e);
  }

  /**
   * Converts a camelCase javascript property name to a hyphenated-lower-case
   * attribute name.
   * @param {string} jsName The javascript camelCase property name.
   * @return {string} The equivalent hyphenated-lower-case attribute name.
   */
  function getAttributeName(jsName) {
    return jsName.replace(/([A-Z])/g, '-$1').toLowerCase();
  }

  /**
   * The kind of property to define in {@code defineProperty}.
   * @enum {string}
   * @const
   */
  var PropertyKind = {
    /**
     * Plain old JS property where the backing data is stored as a "private"
     * field on the object.
     * Use for properties of any type. Type will not be checked.
     */
    JS: 'js',

    /**
     * The property backing data is stored as an attribute on an element.
     * Use only for properties of type {string}.
     */
    ATTR: 'attr',

    /**
     * The property backing data is stored as an attribute on an element. If the
     * element has the attribute then the value is true.
     * Use only for properties of type {boolean}.
     */
    BOOL_ATTR: 'boolAttr'
  };

  /**
   * Helper function for defineProperty that returns the getter to use for the
   * property.
   * @param {string} name The name of the property.
   * @param {PropertyKind} kind The kind of the property.
   * @return {function():*} The getter for the property.
   */
  function getGetter(name, kind) {
    switch (kind) {
      case PropertyKind.JS:
        var privateName = name + '_';
        return function() {
          return this[privateName];
        };
      case PropertyKind.ATTR:
        var attributeName = getAttributeName(name);
        return function() {
          return this.getAttribute(attributeName);
        };
      case PropertyKind.BOOL_ATTR:
        var attributeName = getAttributeName(name);
        return function() {
          return this.hasAttribute(attributeName);
        };
    }

    // TODO(dbeam): replace with assertNotReached() in assert.js when I can coax
    // the browser/unit tests to preprocess this file through grit.
    throw 'not reached';
  }

  /**
   * Helper function for defineProperty that returns the setter of the right
   * kind.
   * @param {string} name The name of the property we are defining the setter
   *     for.
   * @param {PropertyKind} kind The kind of property we are getting the
   *     setter for.
   * @param {function(*, *):void=} opt_setHook A function to run after the
   *     property is set, but before the propertyChange event is fired.
   * @return {function(*):void} The function to use as a setter.
   */
  function getSetter(name, kind, opt_setHook) {
    switch (kind) {
      case PropertyKind.JS:
        var privateName = name + '_';
        return function(value) {
          var oldValue = this[name];
          if (value !== oldValue) {
            this[privateName] = value;
            if (opt_setHook)
              opt_setHook.call(this, value, oldValue);
            dispatchPropertyChange(this, name, value, oldValue);
          }
        };

      case PropertyKind.ATTR:
        var attributeName = getAttributeName(name);
        return function(value) {
          var oldValue = this[name];
          if (value !== oldValue) {
            if (value == undefined)
              this.removeAttribute(attributeName);
            else
              this.setAttribute(attributeName, value);
            if (opt_setHook)
              opt_setHook.call(this, value, oldValue);
            dispatchPropertyChange(this, name, value, oldValue);
          }
        };

      case PropertyKind.BOOL_ATTR:
        var attributeName = getAttributeName(name);
        return function(value) {
          var oldValue = this[name];
          if (value !== oldValue) {
            if (value)
              this.setAttribute(attributeName, name);
            else
              this.removeAttribute(attributeName);
            if (opt_setHook)
              opt_setHook.call(this, value, oldValue);
            dispatchPropertyChange(this, name, value, oldValue);
          }
        };
    }

    // TODO(dbeam): replace with assertNotReached() in assert.js when I can coax
    // the browser/unit tests to preprocess this file through grit.
    throw 'not reached';
  }

  /**
   * Defines a property on an object. When the setter changes the value a
   * property change event with the type {@code name + 'Change'} is fired.
   * @param {!Object} obj The object to define the property for.
   * @param {string} name The name of the property.
   * @param {PropertyKind=} opt_kind What kind of underlying storage to use.
   * @param {function(*, *):void=} opt_setHook A function to run after the
   *     property is set, but before the propertyChange event is fired.
   */
  function defineProperty(obj, name, opt_kind, opt_setHook) {
    if (typeof obj == 'function')
      obj = obj.prototype;

    var kind = /** @type {PropertyKind} */ (opt_kind || PropertyKind.JS);

    if (!obj.__lookupGetter__(name))
      obj.__defineGetter__(name, getGetter(name, kind));

    if (!obj.__lookupSetter__(name))
      obj.__defineSetter__(name, getSetter(name, kind, opt_setHook));
  }

  /**
   * Counter for use with createUid
   */
  var uidCounter = 1;

  /**
   * @return {number} A new unique ID.
   */
  function createUid() {
    return uidCounter++;
  }

  /**
   * Returns a unique ID for the item. This mutates the item so it needs to be
   * an object
   * @param {!Object} item The item to get the unique ID for.
   * @return {number} The unique ID for the item.
   */
  function getUid(item) {
    if (item.hasOwnProperty('uid'))
      return item.uid;
    return item.uid = createUid();
  }

  /**
   * Dispatches a simple event on an event target.
   * @param {!EventTarget} target The event target to dispatch the event on.
   * @param {string} type The type of the event.
   * @param {boolean=} opt_bubbles Whether the event bubbles or not.
   * @param {boolean=} opt_cancelable Whether the default action of the event
   *     can be prevented. Default is true.
   * @return {boolean} If any of the listeners called {@code preventDefault}
   *     during the dispatch this will return false.
   */
  function dispatchSimpleEvent(target, type, opt_bubbles, opt_cancelable) {
    var e = new Event(type, {
      bubbles: opt_bubbles,
      cancelable: opt_cancelable === undefined || opt_cancelable
    });
    return target.dispatchEvent(e);
  }

  /**
   * Calls |fun| and adds all the fields of the returned object to the object
   * named by |name|. For example, cr.define('cr.ui', function() {
   *   function List() {
   *     ...
   *   }
   *   function ListItem() {
   *     ...
   *   }
   *   return {
   *     List: List,
   *     ListItem: ListItem,
   *   };
   * });
   * defines the functions cr.ui.List and cr.ui.ListItem.
   * @param {string} name The name of the object that we are adding fields to.
   * @param {!Function} fun The function that will return an object containing
   *     the names and values of the new fields.
   */
  function define(name, fun) {
    var obj = exportPath(name);
    var exports = fun();
    for (var propertyName in exports) {
      // Maybe we should check the prototype chain here? The current usage
      // pattern is always using an object literal so we only care about own
      // properties.
      var propertyDescriptor = Object.getOwnPropertyDescriptor(exports,
                                                               propertyName);
      if (propertyDescriptor)
        Object.defineProperty(obj, propertyName, propertyDescriptor);
    }
  }

  /**
   * Adds a {@code getInstance} static method that always return the same
   * instance object.
   * @param {!Function} ctor The constructor for the class to add the static
   *     method to.
   */
  function addSingletonGetter(ctor) {
    ctor.getInstance = function() {
      return ctor.instance_ || (ctor.instance_ = new ctor());
    };
  }

  /**
   * Forwards public APIs to private implementations.
   * @param {Function} ctor Constructor that have private implementations in its
   *     prototype.
   * @param {Array<string>} methods List of public method names that have their
   *     underscored counterparts in constructor's prototype.
   * @param {string=} opt_target Selector for target node.
   */
  function makePublic(ctor, methods, opt_target) {
    methods.forEach(function(method) {
      ctor[method] = function() {
        var target = opt_target ? document.getElementById(opt_target) :
                     ctor.getInstance();
        return target[method + '_'].apply(target, arguments);
      };
    });
  }

  /**
   * The mapping used by the sendWithPromise mechanism to tie the Promise
   * returned to callers with the corresponding WebUI response. The mapping is
   * from ID to the PromiseResolver helper; the ID is generated by
   * sendWithPromise and is unique across all invocations of said method.
   * @type {!Object<!PromiseResolver>}
   */
  var chromeSendResolverMap = {};

  /**
   * The named method the WebUI handler calls directly in response to a
   * chrome.send call that expects a response. The handler requires no knowledge
   * of the specific name of this method, as the name is passed to the handler
   * as the first argument in the arguments list of chrome.send. The handler
   * must pass the ID, also sent via the chrome.send arguments list, as the
   * first argument of the JS invocation; additionally, the handler may
   * supply any number of other arguments that will be included in the response.
   * @param {string} id The unique ID identifying the Promise this response is
   *     tied to.
   * @param {boolean} isSuccess Whether the request was successful.
   * @param {*} response The response as sent from C++.
   */
  function webUIResponse(id, isSuccess, response) {
    var resolver = chromeSendResolverMap[id];
    delete chromeSendResolverMap[id];

    if (isSuccess)
      resolver.resolve(response);
    else
      resolver.reject(response);
  }

  /**
   * A variation of chrome.send, suitable for messages that expect a single
   * response from C++.
   * @param {string} methodName The name of the WebUI handler API.
   * @param {...*} var_args Varibale number of arguments to be forwarded to the
   *     C++ call.
   * @return {!Promise}
   */
  function sendWithPromise(methodName, var_args) {
    var args = Array.prototype.slice.call(arguments, 1);
    var promiseResolver = new PromiseResolver();
    var id = methodName + '_' + createUid();
    chromeSendResolverMap[id] = promiseResolver;
    chrome.send(methodName, [id].concat(args));
    return promiseResolver.promise;
  }

  /**
   * A map of maps associating event names with listeners. The 2nd level map
   * associates a listener ID with the callback function, such that individual
   * listeners can be removed from an event without affecting other listeners of
   * the same event.
   * @type {!Object<!Object<!Function>>}
   */
  var webUIListenerMap = {};

  /**
   * The named method the WebUI handler calls directly when an event occurs.
   * The WebUI handler must supply the name of the event as the first argument
   * of the JS invocation; additionally, the handler may supply any number of
   * other arguments that will be forwarded to the listener callbacks.
   * @param {string} event The name of the event that has occurred.
   * @param {...*} var_args Additional arguments passed from C++.
   */
  function webUIListenerCallback(event, var_args) {
    var eventListenersMap = webUIListenerMap[event];
    if (!eventListenersMap) {
      // C++ event sent for an event that has no listeners.
      // TODO(dpapad): Should a warning be displayed here?
      return;
    }

    var args = Array.prototype.slice.call(arguments, 1);
    for (var listenerId in eventListenersMap) {
      eventListenersMap[listenerId].apply(null, args);
    }
  }

  /**
   * Registers a listener for an event fired from WebUI handlers. Any number of
   * listeners may register for a single event.
   * @param {string} eventName The event to listen to.
   * @param {!Function} callback The callback run when the event is fired.
   * @return {!WebUIListener} An object to be used for removing a listener via
   *     cr.removeWebUIListener. Should be treated as read-only.
   */
  function addWebUIListener(eventName, callback) {
    webUIListenerMap[eventName] = webUIListenerMap[eventName] || {};
    var uid = createUid();
    webUIListenerMap[eventName][uid] = callback;
    return {eventName: eventName, uid: uid};
  }

  /**
   * Removes a listener. Does nothing if the specified listener is not found.
   * @param {!WebUIListener} listener The listener to be removed (as returned by
   *     addWebUIListener).
   * @return {boolean} Whether the given listener was found and actually
   *     removed.
   */
  function removeWebUIListener(listener) {
    var listenerExists = webUIListenerMap[listener.eventName] &&
        webUIListenerMap[listener.eventName][listener.uid];
    if (listenerExists) {
      delete webUIListenerMap[listener.eventName][listener.uid];
      return true;
    }
    return false;
  }

  return {
    addSingletonGetter: addSingletonGetter,
    createUid: createUid,
    define: define,
    defineProperty: defineProperty,
    dispatchPropertyChange: dispatchPropertyChange,
    dispatchSimpleEvent: dispatchSimpleEvent,
    exportPath: exportPath,
    getUid: getUid,
    makePublic: makePublic,
    PropertyKind: PropertyKind,

    // C++ <-> JS communication related methods.
    addWebUIListener: addWebUIListener,
    removeWebUIListener: removeWebUIListener,
    sendWithPromise: sendWithPromise,
    webUIListenerCallback: webUIListenerCallback,
    webUIResponse: webUIResponse,

    get doc() {
      return document;
    },

    /** Whether we are using a Mac or not. */
    get isMac() {
      return /Mac/.test(navigator.platform);
    },

    /** Whether this is on the Windows platform or not. */
    get isWindows() {
      return /Win/.test(navigator.platform);
    },

    /** Whether this is on chromeOS or not. */
    get isChromeOS() {
      return /CrOS/.test(navigator.userAgent);
    },

    /** Whether this is on vanilla Linux (not chromeOS). */
    get isLinux() {
      return /Linux/.test(navigator.userAgent);
    },

    /** Whether this is on Android. */
    get isAndroid() {
      return /Android/.test(navigator.userAgent);
    }
  };
}();
</script>
<script>// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// // Copyright (c) 2013 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * @fileoverview Assertion support.
 */

/**
 * Verify |condition| is truthy and return |condition| if so.
 * @template T
 * @param {T} condition A condition to check for truthiness.  Note that this
 *     may be used to test whether a value is defined or not, and we don't want
 *     to force a cast to Boolean.
 * @param {string=} opt_message A message to show on failure.
 * @return {T} A non-null |condition|.
 */
function assert(condition, opt_message) {
  if (!condition) {
    var message = 'Assertion failed';
    if (opt_message)
      message = message + ': ' + opt_message;
    var error = new Error(message);
    var global = function() { return this; }();
    if (global.traceAssertionsForTesting)
      console.warn(error.stack);
    throw error;
  }
  return condition;
}

/**
 * Call this from places in the code that should never be reached.
 *
 * For example, handling all the values of enum with a switch() like this:
 *
 *   function getValueFromEnum(enum) {
 *     switch (enum) {
 *       case ENUM_FIRST_OF_TWO:
 *         return first
 *       case ENUM_LAST_OF_TWO:
 *         return last;
 *     }
 *     assertNotReached();
 *     return document;
 *   }
 *
 * This code should only be hit in the case of serious programmer error or
 * unexpected input.
 *
 * @param {string=} opt_message A message to show when this is hit.
 */
function assertNotReached(opt_message) {
  assert(false, opt_message || 'Unreachable code hit');
}

/**
 * @param {*} value The value to check.
 * @param {function(new: T, ...)} type A user-defined constructor.
 * @param {string=} opt_message A message to show when this is hit.
 * @return {T}
 * @template T
 */
function assertInstanceof(value, type, opt_message) {
  // We don't use assert immediately here so that we avoid constructing an error
  // message if we don't have to.
  if (!(value instanceof type)) {
    assertNotReached(opt_message || 'Value ' + value +
                     ' is not a[n] ' + (type.name || typeof type));
  }
  return value;
}


/**
 * Alias for document.getElementById. Found elements must be HTMLElements.
 * @param {string} id The ID of the element to find.
 * @return {HTMLElement} The found element or null if not found.
 */
function $(id) {
  var el = document.getElementById(id);
  return el ? assertInstanceof(el, HTMLElement) : null;
}

// TODO(devlin): This should return SVGElement, but closure compiler is missing
// those externs.
/**
 * Alias for document.getElementById. Found elements must be SVGElements.
 * @param {string} id The ID of the element to find.
 * @return {Element} The found element or null if not found.
 */
function getSVGElement(id) {
  var el = document.getElementById(id);
  return el ? assertInstanceof(el, Element) : null;
}

/**
 * Add an accessible message to the page that will be announced to
 * users who have spoken feedback on, but will be invisible to all
 * other users. It's removed right away so it doesn't clutter the DOM.
 * @param {string} msg The text to be pronounced.
 */
function announceAccessibleMessage(msg) {
  var element = document.createElement('div');
  element.setAttribute('aria-live', 'polite');
  element.style.position = 'relative';
  element.style.left = '-9999px';
  element.style.height = '0px';
  element.innerText = msg;
  document.body.appendChild(element);
  window.setTimeout(function() {
    document.body.removeChild(element);
  }, 0);
}

/**
 * Returns the scale factors supported by this platform for webui
 * resources.
 * @return {Array} The supported scale factors.
 */
function getSupportedScaleFactors() {
  var supportedScaleFactors = [];
  if (cr.isMac || cr.isChromeOS || cr.isWindows || cr.isLinux) {
    // All desktop platforms support zooming which also updates the
    // renderer's device scale factors (a.k.a devicePixelRatio), and
    // these platforms has high DPI assets for 2.0x. Use 1x and 2x in
    // image-set on these platforms so that the renderer can pick the
    // closest image for the current device scale factor.
    supportedScaleFactors.push(1);
    supportedScaleFactors.push(2);
  } else {
    // For other platforms that use fixed device scale factor, use
    // the window's device pixel ratio.
    // TODO(oshima): Investigate if Android/iOS need to use image-set.
    supportedScaleFactors.push(window.devicePixelRatio);
  }
  return supportedScaleFactors;
}

/**
 * Generates a CSS url string.
 * @param {string} s The URL to generate the CSS url for.
 * @return {string} The CSS url string.
 */
function url(s) {
  // http://www.w3.org/TR/css3-values/#uris
  // Parentheses, commas, whitespace characters, single quotes (') and double
  // quotes (") appearing in a URI must be escaped with a backslash
  var s2 = s.replace(/(\(|\)|\,|\s|\'|\"|\\)/g, '\\$1');
  // WebKit has a bug when it comes to URLs that end with \
  // https://bugs.webkit.org/show_bug.cgi?id=28885
  if (/\\\\$/.test(s2)) {
    // Add a space to work around the WebKit bug.
    s2 += ' ';
  }
  return 'url("' + s2 + '")';
}

/**
 * Returns the URL of the image, or an image set of URLs for the profile avatar.
 * Default avatars have resources available for multiple scalefactors, whereas
 * the GAIA profile image only comes in one size.
 *
 * @param {string} path The path of the image.
 * @return {string} The url, or an image set of URLs of the avatar image.
 */
function getProfileAvatarIcon(path) {
  var chromeThemePath = 'chrome://theme';
  var isDefaultAvatar =
      (path.slice(0, chromeThemePath.length) == chromeThemePath);
  return isDefaultAvatar ? imageset(path + '@scalefactorx'): url(path);
}

/**
 * Generates a CSS -webkit-image-set for a chrome:// url.
 * An entry in the image set is added for each of getSupportedScaleFactors().
 * The scale-factor-specific url is generated by replacing the first instance of
 * 'scalefactor' in |path| with the numeric scale factor.
 * @param {string} path The URL to generate an image set for.
 *     'scalefactor' should be a substring of |path|.
 * @return {string} The CSS -webkit-image-set.
 */
function imageset(path) {
  var supportedScaleFactors = getSupportedScaleFactors();

  var replaceStartIndex = path.indexOf('scalefactor');
  if (replaceStartIndex < 0)
    return url(path);

  var s = '';
  for (var i = 0; i < supportedScaleFactors.length; ++i) {
    var scaleFactor = supportedScaleFactors[i];
    var pathWithScaleFactor = path.substr(0, replaceStartIndex) + scaleFactor +
        path.substr(replaceStartIndex + 'scalefactor'.length);

    s += url(pathWithScaleFactor) + ' ' + scaleFactor + 'x';

    if (i != supportedScaleFactors.length - 1)
      s += ', ';
  }
  return '-webkit-image-set(' + s + ')';
}

/**
 * Parses query parameters from Location.
 * @param {Location} location The URL to generate the CSS url for.
 * @return {Object} Dictionary containing name value pairs for URL
 */
function parseQueryParams(location) {
  var params = {};
  var query = unescape(location.search.substring(1));
  var vars = query.split('&');
  for (var i = 0; i < vars.length; i++) {
    var pair = vars[i].split('=');
    params[pair[0]] = pair[1];
  }
  return params;
}

/**
 * Creates a new URL by appending or replacing the given query key and value.
 * Not supporting URL with username and password.
 * @param {Location} location The original URL.
 * @param {string} key The query parameter name.
 * @param {string} value The query parameter value.
 * @return {string} The constructed new URL.
 */
function setQueryParam(location, key, value) {
  var query = parseQueryParams(location);
  query[encodeURIComponent(key)] = encodeURIComponent(value);

  var newQuery = '';
  for (var q in query) {
    newQuery += (newQuery ? '&' : '?') + q + '=' + query[q];
  }

  return location.origin + location.pathname + newQuery + location.hash;
}

/**
 * @param {Node} el A node to search for ancestors with |className|.
 * @param {string} className A class to search for.
 * @return {Element} A node with class of |className| or null if none is found.
 */
function findAncestorByClass(el, className) {
  return /** @type {Element} */(findAncestor(el, function(el) {
    return el.classList && el.classList.contains(className);
  }));
}

/**
 * Return the first ancestor for which the {@code predicate} returns true.
 * @param {Node} node The node to check.
 * @param {function(Node):boolean} predicate The function that tests the
 *     nodes.
 * @return {Node} The found ancestor or null if not found.
 */
function findAncestor(node, predicate) {
  var last = false;
  while (node != null && !(last = predicate(node))) {
    node = node.parentNode;
  }
  return last ? node : null;
}

function swapDomNodes(a, b) {
  var afterA = a.nextSibling;
  if (afterA == b) {
    swapDomNodes(b, a);
    return;
  }
  var aParent = a.parentNode;
  b.parentNode.replaceChild(a, b);
  aParent.insertBefore(b, afterA);
}

/**
 * Disables text selection and dragging, with optional whitelist callbacks.
 * @param {function(Event):boolean=} opt_allowSelectStart Unless this function
 *    is defined and returns true, the onselectionstart event will be
 *    surpressed.
 * @param {function(Event):boolean=} opt_allowDragStart Unless this function
 *    is defined and returns true, the ondragstart event will be surpressed.
 */
function disableTextSelectAndDrag(opt_allowSelectStart, opt_allowDragStart) {
  // Disable text selection.
  document.onselectstart = function(e) {
    if (!(opt_allowSelectStart && opt_allowSelectStart.call(this, e)))
      e.preventDefault();
  };

  // Disable dragging.
  document.ondragstart = function(e) {
    if (!(opt_allowDragStart && opt_allowDragStart.call(this, e)))
      e.preventDefault();
  };
}

/**
 * TODO(dbeam): DO NOT USE. THIS IS DEPRECATED. Use an action-link instead.
 * Call this to stop clicks on <a href="#"> links from scrolling to the top of
 * the page (and possibly showing a # in the link).
 */
function preventDefaultOnPoundLinkClicks() {
  document.addEventListener('click', function(e) {
    var anchor = findAncestor(/** @type {Node} */(e.target), function(el) {
      return el.tagName == 'A';
    });
    // Use getAttribute() to prevent URL normalization.
    if (anchor && anchor.getAttribute('href') == '#')
      e.preventDefault();
  });
}

/**
 * Check the directionality of the page.
 * @return {boolean} True if Chrome is running an RTL UI.
 */
function isRTL() {
  return document.documentElement.dir == 'rtl';
}

/**
 * Get an element that's known to exist by its ID. We use this instead of just
 * calling getElementById and not checking the result because this lets us
 * satisfy the JSCompiler type system.
 * @param {string} id The identifier name.
 * @return {!HTMLElement} the Element.
 */
function getRequiredElement(id) {
  return assertInstanceof($(id), HTMLElement,
                          'Missing required element: ' + id);
}

/**
 * Query an element that's known to exist by a selector. We use this instead of
 * just calling querySelector and not checking the result because this lets us
 * satisfy the JSCompiler type system.
 * @param {string} selectors CSS selectors to query the element.
 * @param {(!Document|!DocumentFragment|!Element)=} opt_context An optional
 *     context object for querySelector.
 * @return {!HTMLElement} the Element.
 */
function queryRequiredElement(selectors, opt_context) {
  var element = (opt_context || document).querySelector(selectors);
  return assertInstanceof(element, HTMLElement,
                          'Missing required element: ' + selectors);
}

// Handle click on a link. If the link points to a chrome: or file: url, then
// call into the browser to do the navigation.
document.addEventListener('click', function(e) {
  if (e.defaultPrevented)
    return;

  var el = e.target;
  if (el.nodeType == Node.ELEMENT_NODE &&
      el.webkitMatchesSelector('A, A *')) {
    while (el.tagName != 'A') {
      el = el.parentElement;
    }

    if ((el.protocol == 'file:' || el.protocol == 'about:') &&
        (e.button == 0 || e.button == 1)) {
      chrome.send('navigateToUrl', [
        el.href,
        el.target,
        e.button,
        e.altKey,
        e.ctrlKey,
        e.metaKey,
        e.shiftKey
      ]);
      e.preventDefault();
    }
  }
});

/**
 * Creates a new URL which is the old URL with a GET param of key=value.
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
 * Creates a CSS -webkit-image-set for a favicon request.
 * @param {string} url The url for the favicon.
 * @param {number=} opt_size Optional preferred size of the favicon.
 * @param {string=} opt_type Optional type of favicon to request. Valid values
 *     are 'favicon' and 'touch-icon'. Default is 'favicon'.
 * @return {string} -webkit-image-set for the favicon.
 */
function getFaviconImageSet(url, opt_size, opt_type) {
  var size = opt_size || 16;
  var type = opt_type || 'favicon';
  return imageset(
      'chrome://' + type + '/size/' + size + '@scalefactorx/' + url);
}

/**
 * Creates a new URL for a favicon request for the current device pixel ratio.
 * The URL must be updated when the user moves the browser to a screen with a
 * different device pixel ratio. Use getFaviconImageSet() for the updating to
 * occur automatically.
 * @param {string} url The url for the favicon.
 * @param {number=} opt_size Optional preferred size of the favicon.
 * @param {string=} opt_type Optional type of favicon to request. Valid values
 *     are 'favicon' and 'touch-icon'. Default is 'favicon'.
 * @return {string} Updated URL for the favicon.
 */
function getFaviconUrlForCurrentDevicePixelRatio(url, opt_size, opt_type) {
  var size = opt_size || 16;
  var type = opt_type || 'favicon';
  return 'chrome://' + type + '/size/' + size + '@' +
      window.devicePixelRatio + 'x/' + url;
}

/**
 * Creates an element of a specified type with a specified class name.
 * @param {string} type The node type.
 * @param {string} className The class name to use.
 * @return {Element} The created element.
 */
function createElementWithClassName(type, className) {
  var elm = document.createElement(type);
  elm.className = className;
  return elm;
}

/**
 * webkitTransitionEnd does not always fire (e.g. when animation is aborted
 * or when no paint happens during the animation). This function sets up
 * a timer and emulate the event if it is not fired when the timer expires.
 * @param {!HTMLElement} el The element to watch for webkitTransitionEnd.
 * @param {number=} opt_timeOut The maximum wait time in milliseconds for the
 *     webkitTransitionEnd to happen. If not specified, it is fetched from |el|
 *     using the transitionDuration style value.
 */
function ensureTransitionEndEvent(el, opt_timeOut) {
  if (opt_timeOut === undefined) {
    var style = getComputedStyle(el);
    opt_timeOut = parseFloat(style.transitionDuration) * 1000;

    // Give an additional 50ms buffer for the animation to complete.
    opt_timeOut += 50;
  }

  var fired = false;
  el.addEventListener('webkitTransitionEnd', function f(e) {
    el.removeEventListener('webkitTransitionEnd', f);
    fired = true;
  });
  window.setTimeout(function() {
    if (!fired)
      cr.dispatchSimpleEvent(el, 'webkitTransitionEnd', true);
  }, opt_timeOut);
}

/**
 * Alias for document.scrollTop getter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @return {number} The Y document scroll offset.
 */
function scrollTopForDocument(doc) {
  return doc.documentElement.scrollTop || doc.body.scrollTop;
}

/**
 * Alias for document.scrollTop setter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @param {number} value The target Y scroll offset.
 */
function setScrollTopForDocument(doc, value) {
  doc.documentElement.scrollTop = doc.body.scrollTop = value;
}

/**
 * Alias for document.scrollLeft getter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @return {number} The X document scroll offset.
 */
function scrollLeftForDocument(doc) {
  return doc.documentElement.scrollLeft || doc.body.scrollLeft;
}

/**
 * Alias for document.scrollLeft setter.
 * @param {!HTMLDocument} doc The document node where information will be
 *     queried from.
 * @param {number} value The target X scroll offset.
 */
function setScrollLeftForDocument(doc, value) {
  doc.documentElement.scrollLeft = doc.body.scrollLeft = value;
}

/**
 * Replaces '&', '<', '>', '"', and ''' characters with their HTML encoding.
 * @param {string} original The original string.
 * @return {string} The string with all the characters mentioned above replaced.
 */
function HTMLEscape(original) {
  return original.replace(/&/g, '&amp;')
                 .replace(/</g, '&lt;')
                 .replace(/>/g, '&gt;')
                 .replace(/"/g, '&quot;')
                 .replace(/'/g, '&#39;');
}

/**
 * Shortens the provided string (if necessary) to a string of length at most
 * |maxLength|.
 * @param {string} original The original string.
 * @param {number} maxLength The maximum length allowed for the string.
 * @return {string} The original string if its length does not exceed
 *     |maxLength|. Otherwise the first |maxLength| - 1 characters with '...'
 *     appended.
 */
function elide(original, maxLength) {
  if (original.length <= maxLength)
    return original;
  return original.substring(0, maxLength - 1) + '\u2026';
}

/**
 * Quote a string so it can be used in a regular expression.
 * @param {string} str The source string.
 * @return {string} The escaped string.
 */
function quoteString(str) {
  return str.replace(/([\\\.\+\*\?\[\^\]\$\(\)\{\}\=\!\<\>\|\:])/g, '\\$1');
}
</script>
<script>// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

function sendCommand(cmd) {
  window.domAutomationController.setAutomationId(1);
  window.domAutomationController.send(cmd);
}

function makeImageSet(url1x, url2x) {
  return '-webkit-image-set(url(' + url1x + ') 1x, url(' + url2x + ') 2x)';
}

function initialize() {
  if (loadTimeData.getBoolean('allowAccessRequests')) {
    $('request-access-button').onclick = function(event) {
      $('request-access-button').hidden = true;
      sendCommand('request');
    };
  } else {
    $('request-access-button').hidden = true;
  }
  var avatarURL1x = loadTimeData.getString('avatarURL1x');
  var avatarURL2x = loadTimeData.getString('avatarURL2x');
  var custodianName = loadTimeData.getString('custodianName');
  if (custodianName) {
    $('custodians-information').hidden = false;
    if (avatarURL1x) {
      $('custodian-avatar-img').style.content =
          makeImageSet(avatarURL1x, avatarURL2x);
    }
    $('custodian-name').innerHTML = custodianName;
    $('custodian-email').innerHTML = loadTimeData.getString('custodianEmail');
    var secondAvatarURL1x = loadTimeData.getString('secondAvatarURL1x');
    var secondAvatarURL2x = loadTimeData.getString('secondAvatarURL2x');
    var secondCustodianName = loadTimeData.getString('secondCustodianName');
    if (secondCustodianName) {
      $('second-custodian-information').hidden = false;
      $('second-custodian-avatar-img').hidden = false;
      if (secondAvatarURL1x) {
        $('second-custodian-avatar-img').style.content =
            makeImageSet(secondAvatarURL1x, secondAvatarURL2x);
      }
      $('second-custodian-name').innerHTML = secondCustodianName;
      $('second-custodian-email').innerHTML = loadTimeData.getString(
          'secondCustodianEmail');
    }
  }
  var showDetailsLink = loadTimeData.getString('showDetailsLink');
  $('show-details-link').hidden = !showDetailsLink;
  $('back-button').hidden = showDetailsLink;
  $('back-button').onclick = function(event) {
    sendCommand('back');
  };
  $('show-details-link').onclick = function(event) {
    $('details').hidden = false;
    $('show-details-link').hidden = true;
    $('hide-details-link').hidden = false;
    $('information-container').classList.add('hidden-on-mobile');
    $('request-access-button').classList.add('hidden-on-mobile');
  };
  $('hide-details-link').onclick = function(event) {
    $('details').hidden = true;
    $('show-details-link').hidden = false;
    $('hide-details-link').hidden = true;
    $('information-container').classList.remove('hidden-on-mobile');
    $('request-access-button').classList.remove('hidden-on-mobile');
  };
  if (loadTimeData.getBoolean('showFeedbackLink')) {
    $('feedback-link').onclick = function(event) {
      sendCommand('feedback');
    };
  } else {
    $('feedback').hidden = true;
  }
}

/**
 * Updates the interstitial to show that the request failed or was sent.
 * @param {boolean} isSuccessful Whether the request was successful or not.
 */
function setRequestStatus(isSuccessful) {
  $('block-page-message').hidden = true;
  if (isSuccessful) {
    $('request-failed-message').hidden = true;
    $('request-sent-message').hidden = false;
    $('show-details-link').hidden = true;
    $('hide-details-link').hidden = true;
    $('details').hidden = true;
    $('back-button').hidden = false;
    $('request-access-button').hidden = true;
  } else {
    $('request-failed-message').hidden = false;
    $('request-access-button').hidden = false;
  }
}

document.addEventListener('DOMContentLoaded', initialize);
</script>
</head>

<body>
<div id="main-frame-blocked">
  <div id="information-container">
    <h1>
      <div id="block-page-message" i18n-content="blockPageMessage"></div>
      <div id="request-failed-message" i18n-content="requestFailedMessage"
          hidden></div>
      <div id="request-sent-message" i18n-content="requestSentMessage" hidden>
      </div>
    </h1>
    <div id="custodians-information" hidden>
      <div id="custodian-information" class="custodian-information">
        <img id="custodian-avatar-img" class="avatar-img">
        <div id="custodian-contact">
          <div id="custodian-name" class="custodian-name"></div>
          <div id="custodian-email" class="custodian-email"></div>
        </div>
      </div>
      <div id="second-custodian-information" class="custodian-information" hidden>
        <img id="second-custodian-avatar-img" class="avatar-img" hidden>
        <div id="second-custodian-contact">
          <div id="second-custodian-name" class="custodian-name"></div>
          <div id="second-custodian-email" class="custodian-email"></div>
        </div>
      </div>
    </div>
  </div>
  <div id="button-container">
    <div id="details-button-container">
      <a id="show-details-link" i18n-content="showDetailsLink"
          hidden class="button">
      </a>
      <a id="hide-details-link" i18n-content="hideDetailsLink"
          hidden class="button"></a>
      <a id="back-button" i18n-content="backButton"
          hidden class="button"></a>
    </div>
    <div id="request-access-button" class="button"
        i18n-content="requestAccessButton">
    </div> 
  </div>
  <div id="details" hidden>
    <p id="details-header" i18n-content="blockReasonHeader"></p>
    <p id="details-message" i18n-content="blockReasonMessage"></p>
    <p id="feedback" i18n-values=".innerHTML:feedbackLink"></p>
  </div>
</div>
</body>
</html>
<!doctype html>
<html i18n-values="dir:textdirection;lang:language">
<head>
<!-- If you change the title, make sure you also update
chrome/test/functional/special_tabs.py. -->
<meta charset="utf-8">
<title>Sync Internals</title>
<link rel="stylesheet" href="chrome://resources/css/text_defaults.css">
<link rel="stylesheet" href="chrome://resources/css/list.css">
<link rel="stylesheet" href="chrome://resources/css/tabs.css">
<link rel="stylesheet" href="chrome://resources/css/tree.css">
<style>/* Copyright 2013 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#about-info {
  -webkit-column-width: 350px;
}

#about-info > div {
  -webkit-column-break-inside: avoid;
  width: 350px;
}

#about-info h2 {
  color: rgb(74, 142, 230);
  font-size: 100%;
  margin-bottom: 0;
}

#about-info .err {
  color: red;
}

#about-info .section {
  display: inline-block;
  margin-left: auto;
  margin-right: auto;
}

.about-details {
  width: 100%;
}

.about-details tr:nth-child(odd) {
  background: rgb(239, 243, 255);
}

#typeInfo .error {
 background: rgb(255, 204, 204);
}

#typeInfo .warning {
 background: rgb(255, 255, 204);
}

#typeInfo .disabled {
 background: rgb(224, 224, 224);
}

#typeInfo .ok {
 background: rgb(204, 255, 204);
}

@-webkit-keyframes highlight1 {
  0% {
    background: rgb(255, 255, 0);
  }
  100% {
    background: #fff;
  }
}

@-webkit-keyframes highlight2 {
  0% {
    background: rgb(155, 158, 166);
  }
  100% {
    background: rgb(239, 243, 255);
  }
}

.about-details [highlighted] {
  -webkit-animation-duration: 3s;
  -webkit-animation-name: highlight1;
  -webkit-animation-timing-function: linear;
}

.about-details [highlighted]:nth-child(odd) {
  -webkit-animation-duration: 3s;
  -webkit-animation-name: highlight2;
  -webkit-animation-timing-function: linear;
}

.about-details .uninitialized {
  color: #7f7f7f;
}

#status {
  margin-left: auto;
  margin-right: auto;
  text-align: center;
  width: 300px;
}

#dump-status {
  margin: 2px;
}

#import-status {
  margin: 2px;
}

#traffic-event-container {
  border: 1px solid;
  height: 500px;
  max-width: 500px;
  overflow-y: auto;
}

.traffic-event-entry {
  border: 2px outset;
  padding: 0.5em;
}

.traffic-event-entry:hover {
  background-color: #eee;
}

.traffic-event-entry .time {
  color: #222;
}

.traffic-event-entry .type {
  font-weight: bold;
  margin: 0.5em;
  white-space: nowrap;
}

.traffic-event-entry .details {
  margin: 0.5em;
  overflow-x: auto;
}

.traffic-event-entry .proto {
  display: none;
}

.traffic-event-entry-expanded .proto {
  background-color: #fff;
  border: 1px solid #222;
  display: block;
  max-height: 300px;
  overflow-x: auto;
  overflow-y: auto;
}
</style>
<style>/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#sync-events-table,
#sync-events-table th,
#sync-events-table td {
  border: 1px black solid;
}

#sync-events-table {
  table-layout: fixed;
  width: 100%;
}

#sync-events > tr {
  vertical-align: top;
}

.expanded .attrib-column {
  display: none;
}
</style>
<style>/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

#type-counters-table {
  table-layout: fixed;
}

#type-counters-table th {
  max-width: 80px;
  width: 80px;
}

#type-counters-table th.type {
  max-width: 200px;
  width: 200px;
}

#type-counters-table tr:nth-child(odd) {
  background: rgb(239, 243, 255);
}

</style>
<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#sync-search-query {
  width: 16em;
}

#sync-search-query[error] {
  background-color: rgb(255,170,170);
}

.sync-search-quicklink {
  background-color: rgb(239,243,255);
  padding-left: 10px;
  padding-right: 10px;
}

#sync-search-status {
  color: rgb(51,51,51);
  font-style: italic;
}

#sync-results-container {
  display: -webkit-box;
  /* Should be > #sync-page's min-height. */
  /* TODO(akalin): Find a less hacky way to do this. */
  height: 750px;
}

#sync-results-list {
  -webkit-padding-start: 10px;
  box-sizing: border-box;
  height: 100%;
  /* min-width and max-width are used by the split pane. */
  max-width: 50%;
  min-width: 50px;
  overflow: auto;
  padding: 5px;
  width: 275px;
}

#sync-results-splitter {
  background-color: rgb(235, 239, 249);
  cursor: col-resize;
/* TODO(akalin): Make the BMM also use this style. */
  cursor: e-resize;
  width: 5px;
}

#sync-result-details-container {
  -webkit-box-flex: 1;
  height: 100%;
  overflow: auto;
  /* TODO(akalin): Figure out why this is needed, even with box-flex: 1. */
  width: 100%;
}
</style>
<style>/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

#sync-node-browser-refresher {
  border-bottom: 1px rgb(160,160,160) solid;
}

#sync-node-browser-refresher > * {
  display: inline-block;
}

#sync-node-browser-container {
  display: -webkit-box;
  height: 750px;
}

#sync-node-tree-container {
  -webkit-padding-start: 10px;
  box-sizing: border-box;
  height: 100%;
  /* min-width and max-width are used by the split pane. */
  max-width: 50%;
  min-width: 50px;
  overflow: auto;
  width: 200px;
}

#sync-node-tree {
  display: inline-block;
  min-width: 100%;
  overflow: visible; /* let the container do the scrolling */
}

/* TODO(akalin): Find a better icon to use for leaf nodes. */
#sync-node-tree .leaf .tree-label {
  background-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAsAAAALCAYAAACprHcmAAABr0lEQVQY02NgQAOFJU96jI1PfA0Kud/DgA8sWfFWMzn16sVZU7P/+/ntu9jW+VoTQ9GcBa9FJ0x9JVrb9C6jvGzr//evZP/nZnX/T818lzFhymvReYveiIIVBgbfri6reva/qOTxf1//5//nzKj5//8/6//+9tD/tvb3/hcUP/6fk//kv6/f7WoGVbVj//u6qv8fOBDxf8/2tP9vXpj9//eT8/+bp3L/d26N+L9/f9j/4tyg/zIyu/4z5BY8ehTg1/Z/5yZFoIlM////5P7/9ws/kOb6/+8b4/8t67j+e3vk/E9Ju/yIYfP2d0rV9a9mSklufb17i8X//3+4/v//DlLM/n/2FOX/4mKTX2fnv5y5Yu1bJbgnvX1vn1q1LBzoBKb/754w/v/7len/3Glq/zW1zp5CCY2d+z7XxSec/f34psr/eVOl/wf4xPyfN032/9VTLP/d3ab/njLjfR1ccVrGi97QkOb/tWWq/83MFvw3NH641s529f/EWIP/ft6x/909H/bCFc+a+7rF2nrbVw+vw1+B4drh63+TaeHSzx3unqe/qmts/lpZ+6QFpA4AtfnoAC8Qt2wAAAAASUVORK5CYII=);
}

#sync-node-splitter {
  background-color: rgb(235, 239, 249);
  cursor: col-resize;
  width: 5px;
/* TODO(akalin): Make the BMM also use this style. */
  cursor: e-resize;
}

#sync-node-details-container {
  -webkit-box-flex: 1;
  height: 100%;
  overflow: auto;
  visibility: hidden;  /* Element is invisible until first refresh. */
}

#node-details {
  width: 100%;
}

#node-details td {
  vertical-align: top;
  white-space: nowrap;
}

#node-details tr:nth-child(odd) {
  background: rgb(239, 243, 255);
}
</style>



<script src="chrome://resources/js/event_tracker.js"></script>
<script src="chrome://resources/js/cr.js"></script>
<script src="chrome://resources/js/cr/event_target.js"></script>
<script src="chrome://resources/js/cr/ui/touch_handler.js"></script>
<script src="chrome://resources/js/cr/ui.js"></script>
<script src="chrome://resources/js/cr/ui/focus_outline_manager.js"></script>
<script src="chrome://resources/js/cr/ui/splitter.js"></script>
<script src="chrome://resources/js/load_time_data.js"></script>

<!-- List stuff. -->
<script src="chrome://resources/js/cr/ui/array_data_model.js"></script>
<script src="chrome://resources/js/cr/ui/list_item.js"></script>
<script src="chrome://resources/js/cr/ui/list_selection_controller.js"></script>
<script src="chrome://resources/js/cr/ui/list_selection_model.js"></script>
<script src="chrome://resources/js/cr/ui/list.js"></script>
<script src="chrome://resources/js/cr/ui/tabs.js"></script>
<script src="chrome://resources/js/cr/ui/tree.js"></script>
<script src="chrome://resources/js/util.js"></script>
<script src="chrome://sync-internals/chrome_sync.js"></script>
<script src="chrome://sync-internals/about.js"></script>
<script src="chrome://sync-internals/events.js"></script>
<script src="chrome://sync-internals/types.js"></script>
<script src="chrome://sync-internals/sync_log.js"></script>
<script src="chrome://sync-internals/sync_node_browser.js"></script>
<script src="chrome://sync-internals/sync_search.js"></script>
<script src="chrome://sync-internals/strings.js"></script>
</head>
<body>

<style>
#sync-page {
  /* TODO(akalin): Figure out a better way to make the tab box the
     same height no matter which tab is selected. */
  min-height: 650px;
}
</style>

<tabbox id="sync-page">
  <tabs>
    <tab id="sync-about-tab">About</tab>
    <tab id="sync-types-tab">Types</tab>
    <tab id="sync-data-tab">Data</tab>
    <tab id="sync-events-tab">Events</tab>
    <tab id="sync-browser-tab">Sync Node Browser</tab>
    <tab id="sync-search-tab">Search</tab>
  </tabs>
  <tabpanels>
    <tabpanel>
      
<div id="status">
  <div id="dump">
    <button id="dump-status">Dump status</button>
    <input type="checkbox" id="include-ids">
      Include Identifiers
    </input>
  </div>
  <div id="import">
    <button id="import-status">Import status</button>
  </div>
  <div id="status-data">
    <textarea rows="10" cols="30" id="status-text"></textarea>
  </div>
</div>

<div id='about-info'>
  <div class="section" jsselect="details">
    <h2 jscontent="title"></h2>
    <table class="about-details">
      <tr jsselect="data"
            jsvalues="class:$this.is_valid ? '' : 'uninitialized'"
            jseval='chrome.sync.about_tab.highlightIfChanged(this, this.children[1].innerText, stat_value)'>
        <td class="detail" jscontent="stat_name" width=50%></td>
        <td class="value" jscontent="stat_value" width=50%></td>
      </tr>
    </table>
  </div>

  <div id="traffic-event-container-wrapper" jsskip="true">
    <h2>Sync Protocol Log</h2>
    <div id="traffic-event-container">
      <div class="traffic-event-entry"
           jsselect="events"
           jseval="chrome.sync.about_tab.addExpandListener(this)">
        <span class="time" jscontent="(new Date(time)).toLocaleString()"></span>
        <span class="type" jscontent="type"></span>
        <pre class="details" jscontent="details"></pre>
        <pre class="proto" jscontent="JSON.stringify(proto, null, 2)"></pre>
      </div>
    </div>
  </div>

  <div class="section" style="overflow-x: auto">
    <h2>Type Info</h2>
    <table id="typeInfo">
      <tr jsselect="type_status" jsvalues="class:$this.status">
        <td jscontent="name" width=50%></td>
        <td jscontent="value" width=30%></td>
        <td jscontent="num_entries" width=10%></td>
        <td jscontent="num_live" width=10%></td>
      </tr>
    </table>
  </div>

  <div class="section" jsdisplay="unrecoverable_error_detected">
    <p>
      <span class="err" jscontent="unrecoverable_error_message"></span>
    </p>
  </div>

  <div class="section" jsdisplay="actionable_error_detected">
    <p>
      <h2>Actionable Error</h2>
      <table id="actionableError">
        <tr jsselect="actionable_error">
          <td jscontent="stat_name"></td>
          <td jscontent="stat_value"></td>
        </tr>
      </table>
    </p>
  </div>
</div>

    </tabpanel>
    <tabpanel>
        <div id="type-counters-container-wrapper" jsskip="true">
    <div class="section">
      <h2>Type Counters</h2>
      <table id="type-counters-table">
        <thead>
          <tr>
            <th class='type'>Type</th>
            <th>Total Entries</th>

            <th>Updates Received</th>
            <th>Reflected Updates Received</th>
            <th>Tombstone Updates Received</th>

            <th>Updates Applied</th>
            <th>Hierarchy Conflict Application Failures</th>
            <th>Encryption Conflict Application Failures</th>

            <th>Server Overwrite Conflicts</th>
            <th>Local Overwrite Conflicts</th>

            <th>Commit Attempts</th>
            <th>Commit Successes</th>
            <th>Commit Conflicts</th>
            <th>Commit Errors</th>
          </tr>
        </thead>
        <tbody>
          <tr jsselect="rows">
            <td jscontent="type"></td>
            <td jscontent="counters.numEntries || 0">0</td>

            <td jscontent="counters.numUpdatesReceived || 0">0</td>
            <td jscontent="counters.numReflectedUpdatesReceived || 0">0</td>
            <td jscontent="counters.numTombstoneUpdatesReceived || 0">0</td>

            <td jscontent="counters.numUpdatesApplied || 0">0</td>
            <td jscontent="counters.numHierarchyConflictApplicationFailures || 0">0</td>
            <td jscontent="counters.numEncryptionConflictApplicationFailures || 0">0</td>

            <td jscontent="counters.numServerOverwrites || 0">0</td>
            <td jscontent="counters.numLocalOverwrites || 0">0</td>

            <td jscontent="counters.numCommitsAttempted || 0">0</td>
            <td jscontent="counters.numCommitsSuccess || 0">0</td>
            <td jscontent="counters.numCommitsConflict || 0">0</td>
            <td jscontent="counters.numCommitsError || 0">0</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>

    </tabpanel>
    <tabpanel>
      <p><strong>Some personal info may be in the events dump. Be
careful about posting data dumps on bug reports.</strong></p>

<button id="dump-to-text">Dump sync events to text</button>

<pre id="data-dump"></pre>

<hr>

<div id="node-type-checkboxes">
</div>


<button id="dump-to-file">Dump sync nodes to file</button>

<input type="checkbox" id="include-specifics">include node content <font color="red">WARNING: This is likely to include personal information.</font><br>

<a style="display: none" id="dump-to-file-anchor"></a>

<script src="chrome://sync-internals/data.js"></script>

    </tabpanel>
    <tabpanel>
      <table id="sync-events-table">
  <thead>
    <th>Details</th>
    <th>Submodule</th>
    <th>Event</th>
    <th>Time</th>
  </thead>
  <tbody id="sync-events">
    <tr jsselect="eventList">
      <td>
        <button class="toggle-button">Toggle Display</button>
        <pre jscontent="textDetails" class="details" hidden></pre>
      </td>
      <td jscontent="submodule" class="attrib-column"></td>
      <td jscontent="event" class="attrib-column"></td>
      <td jscontent="date" class="attrib-column"></td>
    </tr>
  </tbody>
</table>

    </tabpanel>
    <tabpanel>
      <!-- TODO(akalin): Move to a three-pane view; node tree on the left
(minus leaf nodes), tree contents list on the upper right, selected
item detail on the lower right. -->

<div id="sync-node-main">
  <!-- TODO(akalin): Figure out how to get this element to be as tall
       as its container (style.height=100% doesn't work).  Also fix
       behavior when tree is too tall (currently it makes you scroll the
       entire page). -->
  <div id="sync-node-browser-refresher">
    <button id="node-browser-refresh-button">Refresh</button>
    <div id="node-refresh-status">
      Last refresh time: <span id="node-browser-refresh-time">Never</span>
    </div>
  </div>
  <div id="sync-node-browser-container">
    <div id="sync-node-tree-container">
    </div>
    <div id="sync-node-splitter"></div>
    <div id="node-details">
      <table>
        <tr>
          <td>Title</td>
          <td jscontent="NON_UNIQUE_NAME"></td>
        </tr>
        <tr>
          <td>ID</td>
          <td jscontent="ID"></td>
        </tr>
        <tr>
          <td>Modification Time</td>
          <td jscontent="MTIME"></td>
        </tr>
        <tr>
          <td>Parent</td>
          <td jscontent="PARENT_ID"></td>
        </tr>
        <tr>
          <td>Is Folder</td>
          <td jscontent="IS_DIR"></td>
        </tr>
        <tr>
          <td>Type</td>
          <td jscontent="modelType"></td>
        </tr>
        <tr>
          <td>External ID</td>
          <td jscontent="LOCAL_EXTERNAL_ID"></td>
        </tr>
        <tr jsdisplay="$this.hasOwnProperty('positionIndex')">
          <td>Position Index</td>
          <td jscontent="positionIndex"></td>
        </tr>
      </table>
      <pre jscontent="JSON.stringify($this, null, 2)"></pre></td>
    </div>
  </div>
</div>

    </tabpanel>
    <tabpanel>
      <p>
<input id="sync-search-query" type="search"
       placeholder="Search Sync Data">
<button id="sync-search-submit">Search</button>
  <span id="sync-search-quicklink-container">Quick Search:
    <a class='sync-search-quicklink' data-query="&quot;IS_UNAPPLIED_UPDATE&quot;: true"
      href="#">Unapplied Updates</a>
    <a class='sync-search-quicklink' data-query="&quot;IS_UNSYNCED&quot;: true"
      href="#">Unsynced</a>
    <a class='sync-search-quicklink' data-query="((&quot;IS_UNAPPLIED_UPDATE&quot;: true)[\s\S]*(&quot;IS_UNSYNCED&quot;: true))|((&quot;IS_UNSYNCED&quot;: true)[\s\S]*(&quot;IS_UNAPPLIED_UPDATE&quot;: true))"
      href="#">Conflicted</a>
    <a class='sync-search-quicklink' data-query="&quot;IS_DEL&quot;: true"
      href="#">Deleted</a>
  </span>
</p>
<p>
<span id="sync-search-status"></span>
</p>

<div id="sync-results-container">
  <list id="sync-results-list"></list>
  <div id="sync-results-splitter"></div>
  <div id="sync-result-details-container">
    <pre id="sync-result-details"></pre>
  </div>
</div>

<script src="chrome://sync-internals/search.js"></script>

    </tabpanel>
  </tabpanels>
</tabbox>

<script src="chrome://resources/js/i18n_template.js"></script>
<script src="chrome://resources/js/jstemplate_compiled.js"></script>
<script src="chrome://sync-internals/sync_index.js"></script>
</body>
</html>
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// Allow platform specific CSS rules.
//
// TODO(akalin): BMM and options page does something similar, too.
// Move this to util.js.
if (cr.isWindows)
  document.documentElement.setAttribute('os', 'win');

cr.ui.decorate('tabbox', cr.ui.TabBox);
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// require cr.js
// require cr/event_target.js
// require cr/util.js

cr.define('chrome.sync', function() {
  'use strict';

  /**
   * A simple timer to measure elapsed time.
   * @constructor
   */
  function Timer() {
    /**
     * The time that this Timer was created.
     * @type {number}
     * @private
     * @const
     */
    this.start_ = Date.now();
  }

  /**
   * @return {number} The elapsed seconds since this Timer was created.
   */
  Timer.prototype.getElapsedSeconds = function() {
    return (Date.now() - this.start_) / 1000;
  };

  /** @return {!Timer} An object which measures elapsed time. */
  var makeTimer = function() {
    return new Timer;
  };

  /**
   * @param {string} name The name of the event type.
   * @param {!Object} details A collection of event-specific details.
   */
  var dispatchEvent = function(name, details) {
    var e = new Event(name);
    e.details = details;
    chrome.sync.events.dispatchEvent(e);
  };

  /**
   * Registers to receive a stream of events through
   * chrome.sync.dispatchEvent().
   */
  var registerForEvents = function() {
    chrome.send('registerForEvents');
  };

  /**
   * Registers to receive a stream of status counter update events
   * chrome.sync.dispatchEvent().
   */
  var registerForPerTypeCounters = function() {
    chrome.send('registerForPerTypeCounters');
  }

  /**
   * Asks the browser to refresh our snapshot of sync state.  Should result
   * in an onAboutInfoUpdated event being emitted.
   */
  var requestUpdatedAboutInfo = function() {
    chrome.send('requestUpdatedAboutInfo');
  };

  /**
   * Asks the browser to send us the list of registered types.  Should result
   * in an onReceivedListOfTypes event being emitted.
   */
  var requestListOfTypes = function() {
    chrome.send('requestListOfTypes');
  };

  /**
   * Counter to uniquely identify requests while they're in progress.
   * Used in the implementation of GetAllNodes.
   */
  var requestId = 0;

  /**
   * A map from counter values to asynchronous request callbacks.
   * Used in the implementation of GetAllNodes.
   * @type {{number: !Function}}
   */
  var requestCallbacks = {};

  /**
   * Asks the browser to send us a copy of all existing sync nodes.
   * Will eventually invoke the given callback with the results.
   *
   * @param {function(!Object)} callback The function to call with the response.
   */
  var getAllNodes = function(callback) {
    requestId++;
    requestCallbacks[requestId] = callback;
    chrome.send('getAllNodes', [requestId]);
  };

  /**
   * Called from C++ with the response to a getAllNodes request.
   * @param {number} id The requestId passed in with the request.
   * @param {Object} response The response to the request.
   */
  var getAllNodesCallback = function(id, response) {
    requestCallbacks[id](response);
    requestCallbacks[id] = undefined;
  };

  return {
    makeTimer: makeTimer,
    dispatchEvent: dispatchEvent,
    events: new cr.EventTarget(),
    getAllNodes: getAllNodes,
    getAllNodesCallback: getAllNodesCallback,
    registerForEvents: registerForEvents,
    registerForPerTypeCounters: registerForPerTypeCounters,
    requestUpdatedAboutInfo: requestUpdatedAboutInfo,
    requestListOfTypes: requestListOfTypes,
  };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('chrome.sync.types', function() {
  var typeCountersMap = {};

  /**
   * Redraws the counters table taking advantage of the most recent
   * available information.
   *
   * Makes use of typeCountersMap, which is defined in the containing scope.
   */
  var refreshTypeCountersDisplay = function() {
    var typeCountersArray = [];

    // Transform our map into an array to make jstemplate happy.
    Object.keys(typeCountersMap).sort().forEach(function(t) {
      typeCountersArray.push({
        type: t,
        counters: typeCountersMap[t],
      });
    });

    jstProcess(
        new JsEvalContext({ rows: typeCountersArray }),
        $('type-counters-table'));
  };

  /**
   * Helps to initialize the table by picking up where initTypeCounters() left
   * off.  That function registers this listener and requests that this event
   * be emitted.
   *
   * @param {!Object} e An event containing the list of known sync types.
   */
  var onReceivedListOfTypes = function(e) {
    var types = e.details.types;
    types.map(function(type) {
      if (!typeCountersMap.hasOwnProperty(type)) {
        typeCountersMap[type] = {};
      }
    });
    chrome.sync.events.removeEventListener(
        'onReceivedListOfTypes',
        onReceivedListOfTypes);
    refreshTypeCountersDisplay();
  };

  /**
   * Callback for receipt of updated per-type counters.
   *
   * @param {!Object} e An event containing an updated counter.
   */
  var onCountersUpdated = function(e) {
    var details = e.details;

    var modelType = details.modelType;
    var counters = details.counters;

    if (typeCountersMap.hasOwnProperty(modelType))
      for (k in counters) {
        typeCountersMap[modelType][k] = counters[k];
      }
    refreshTypeCountersDisplay();
  };

  /**
   * Initializes state and callbacks for the per-type counters and status UI.
   */
  var initTypeCounters = function() {
    chrome.sync.events.addEventListener(
        'onCountersUpdated',
        onCountersUpdated);
    chrome.sync.events.addEventListener(
        'onReceivedListOfTypes',
        onReceivedListOfTypes);

    chrome.sync.requestListOfTypes();
    chrome.sync.registerForPerTypeCounters();
  };

  var onLoad = function() {
    initTypeCounters();
  };

  return {
    onLoad: onLoad
  };
});

document.addEventListener('DOMContentLoaded', chrome.sync.types.onLoad, false);
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// require: cr.js
// require: cr/event_target.js

/**
 * @fileoverview This creates a log object which listens to and
 * records all sync events.
 */

cr.define('chrome.sync', function() {
  'use strict';

  var eventsByCategory = {
    notifier: [
      'onIncomingNotification',
      'onNotificationStateChange',
    ],

    manager: [
      'onActionableError',
      'onChangesApplied',
      'onChangesComplete',
      'onClearServerDataFailed',
      'onClearServerDataSucceeded',
      'onConnectionStatusChange',
      'onEncryptedTypesChanged',
      'onEncryptionComplete',
      'onInitializationComplete',
      'onPassphraseAccepted',
      'onPassphraseRequired',
      'onStopSyncingPermanently',
      'onSyncCycleCompleted',
    ],

    transaction: [
      'onTransactionWrite',
    ],

    protocol: [
      'onProtocolEvent',
    ]
  };

  /**
   * Creates a new log object which then immediately starts recording
   * sync events.  Recorded entries are available in the 'entries'
   * property and there is an 'append' event which can be listened to.
   * @constructor
   * @extends {cr.EventTarget}
   */
  var Log = function() {
    var self = this;

    /**
     * Creates a callback function to be invoked when an event arrives.
     */
    var makeCallback = function(categoryName, eventName) {
      return function(e) {
        self.log_(categoryName, eventName, e.details);
      };
    };

    for (var categoryName in eventsByCategory) {
      for (var i = 0; i < eventsByCategory[categoryName].length; ++i) {
        var eventName = eventsByCategory[categoryName][i];
        chrome.sync.events.addEventListener(
            eventName,
            makeCallback(categoryName, eventName));
      }
    }
  }

  Log.prototype = {
    __proto__: cr.EventTarget.prototype,

    /**
     * The recorded log entries.
     * @type {array}
     */
    entries: [],

    /**
     * Records a single event with the given parameters and fires the
     * 'append' event with the newly-created event as the 'detail'
     * field of a custom event.
     * @param {string} submodule The sync submodule for the event.
     * @param {string} event The name of the event.
     * @param {dictionary} details A dictionary of event-specific details.
     */
    log_: function(submodule, event, details) {
      var entry = {
        submodule: submodule,
        event: event,
        date: new Date(),
        details: details,
        textDetails: ''
      };
      entry.textDetails = JSON.stringify(entry.details, null, 2);
      this.entries.push(entry);
      // Fire append event.
      var e = cr.doc.createEvent('CustomEvent');
      e.initCustomEvent('append', false, false, entry);
      this.dispatchEvent(e);
    }
  };

  return {
    log: new Log()
  };
});
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// require: cr.js
// require: cr/ui.js
// require: cr/ui/tree.js

(function() {
  /**
   * A helper function to determine if a node is the root of its type.
   *
   * @param {!Object} node The node to check.
   */
  var isTypeRootNode = function(node) {
    return node.PARENT_ID == 'r' && node.UNIQUE_SERVER_TAG != '';
  };

  /**
   * A helper function to determine if a node is a child of the given parent.
   *
   * @param {!Object} parent node.
   * @param {!Object} node The node to check.
   */
  var isChildOf = function(parentNode, node) {
    if (node.PARENT_ID != '') {
      return node.PARENT_ID == parentNode.ID;
    }
    else {
      return node.modelType == parentNode.modelType;
    }
  };

  /**
   * A helper function to sort sync nodes.
   *
   * Sorts by position index if possible, falls back to sorting by name, and
   * finally sorting by METAHANDLE.
   *
   * If this proves to be slow and expensive, we should experiment with moving
   * this functionality to C++ instead.
   */
  var nodeComparator = function(nodeA, nodeB) {
    if (nodeA.hasOwnProperty('positionIndex') &&
        nodeB.hasOwnProperty('positionIndex')) {
      return nodeA.positionIndex - nodeB.positionIndex;
    } else if (nodeA.NON_UNIQUE_NAME != nodeB.NON_UNIQUE_NAME) {
      return nodeA.NON_UNIQUE_NAME.localeCompare(nodeB.NON_UNIQUE_NAME);
    } else {
      return nodeA.METAHANDLE - nodeB.METAHANDLE;
    }
  };

  /**
   * Updates the node detail view with the details for the given node.
   * @param {!Object} node The struct representing the node we want to display.
   */
  function updateNodeDetailView(node) {
    var nodeDetailsView = $('node-details');
    nodeDetailsView.hidden = false;
    jstProcess(new JsEvalContext(node.entry_), nodeDetailsView);
  }

  /**
   * Updates the 'Last refresh time' display.
   * @param {string} The text to display.
   */
  function setLastRefreshTime(str) {
    $('node-browser-refresh-time').textContent = str;
  }

  /**
   * Creates a new sync node tree item.
   *
   * @constructor
   * @param {!Object} node The nodeDetails object for the node as returned by
   *     chrome.sync.getAllNodes().
   * @extends {cr.ui.TreeItem}
   */
  var SyncNodeTreeItem = function(node) {
    var treeItem = new cr.ui.TreeItem();
    treeItem.__proto__ = SyncNodeTreeItem.prototype;

    treeItem.entry_ = node;
    treeItem.label = node.NON_UNIQUE_NAME;
    if (node.IS_DIR) {
      treeItem.mayHaveChildren_ = true;

      // Load children on expand.
      treeItem.expanded_ = false;
      treeItem.addEventListener('expand',
                                treeItem.handleExpand_.bind(treeItem));
    } else {
      treeItem.classList.add('leaf');
    }
    return treeItem;
  };

  SyncNodeTreeItem.prototype = {
    __proto__: cr.ui.TreeItem.prototype,

    /**
     * Finds the children of this node and appends them to the tree.
     */
    handleExpand_: function(event) {
      var treeItem = this;

      if (treeItem.expanded_) {
        return;
      }
      treeItem.expanded_ = true;

      var children = treeItem.tree.allNodes.filter(
          isChildOf.bind(undefined, treeItem.entry_));
      children.sort(nodeComparator);

      children.forEach(function(node) {
        treeItem.add(new SyncNodeTreeItem(node));
      });
    },
  };

  /**
   * Creates a new sync node tree.  Technically, it's a forest since it each
   * type has its own root node for its own tree, but it still looks and acts
   * mostly like a tree.
   *
   * @param {Object=} opt_propertyBag Optional properties.
   * @constructor
   * @extends {cr.ui.Tree}
   */
  var SyncNodeTree = cr.ui.define('tree');

  SyncNodeTree.prototype = {
    __proto__: cr.ui.Tree.prototype,

    decorate: function() {
      cr.ui.Tree.prototype.decorate.call(this);
      this.addEventListener('change', this.handleChange_.bind(this));
      this.allNodes = [];
    },

    populate: function(nodes) {
      var tree = this;

      // We store the full set of nodes in the SyncNodeTree object.
      tree.allNodes = nodes;

      var roots = tree.allNodes.filter(isTypeRootNode);
      roots.sort(nodeComparator);

      roots.forEach(function(typeRoot) {
        tree.add(new SyncNodeTreeItem(typeRoot));
      });
    },

    handleChange_: function(event) {
      if (this.selectedItem) {
        updateNodeDetailView(this.selectedItem);
      }
    }
  };

  /**
   * Clears any existing UI state.  Useful prior to a refresh.
   */
  function clear() {
    var treeContainer = $('sync-node-tree-container');
    while (treeContainer.firstChild) {
      treeContainer.removeChild(treeContainer.firstChild);
    }

    var nodeDetailsView = $('node-details');
    nodeDetailsView.hidden = true;
  }

  /**
   * Fetch the latest set of nodes and refresh the UI.
   */
  function refresh() {
    $('node-browser-refresh-button').disabled = true;

    clear();
    setLastRefreshTime('In progress since ' + (new Date()).toLocaleString());

    chrome.sync.getAllNodes(function(nodeMap) {
      // Put all nodes into one big list that ignores the type.
      var nodes = nodeMap.
          map(function(x) { return x.nodes; }).
          reduce(function(a, b) { return a.concat(b); });

      var treeContainer = $('sync-node-tree-container');
      var tree = document.createElement('tree');
      tree.setAttribute('id', 'sync-node-tree');
      tree.setAttribute('icon-visibility', 'parent');
      treeContainer.appendChild(tree);

      cr.ui.decorate(tree, SyncNodeTree);
      tree.populate(nodes);

      setLastRefreshTime((new Date()).toLocaleString());
      $('node-browser-refresh-button').disabled = false;
    });
  }

  document.addEventListener('DOMContentLoaded', function(e) {
    $('node-browser-refresh-button').addEventListener('click', refresh);
    cr.ui.decorate('#sync-node-splitter', cr.ui.Splitter);

    // Automatically trigger a refresh the first time this tab is selected.
    $('sync-browser-tab').addEventListener('selectedChange', function f(e) {
      if (this.selected) {
        $('sync-browser-tab').removeEventListener('selectedChange', f);
        refresh();
      }
    });
  });

})();
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// require: cr.js

cr.define('chrome.sync', function() {
  var currSearchId = 0;

  var setQueryString = function(queryControl, query) {
    queryControl.value = query;
  };

  var createDoQueryFunction = function(queryControl, submitControl, query) {
    return function() {
      setQueryString(queryControl, query);
      submitControl.click();
    };
  };

  /**
   * Decorates the quick search controls
   *
   * @param {Array of DOM elements} quickLinkArray The <a> object which
   *     will be given a link to a quick filter option.
   * @param {!HTMLInputElement} queryControl The <input> object of
   *     type=search where user's query is typed.
   */
  var decorateQuickQueryControls = function(quickLinkArray, submitControl,
                                            queryControl) {
    for (var index = 0; index < allLinks.length; ++index) {
      var quickQuery = allLinks[index].getAttribute('data-query');
      var quickQueryFunction = createDoQueryFunction(queryControl,
          submitControl, quickQuery);
      allLinks[index].addEventListener('click', quickQueryFunction);
    }
  };

  /**
   * Runs a search with the given query.
   *
   * @param {string} query The regex to do the search with.
   * @param {function} callback The callback called with the search results;
   *     not called if doSearch() is called again while the search is running.
   */
  var doSearch = function(query, callback) {
    var searchId = ++currSearchId;
    try {
      var regex = new RegExp(query);
      chrome.sync.getAllNodes(function(node_map) {
        // Put all nodes into one big list that ignores the type.
        var nodes = node_map.
            map(function(x) { return x.nodes; }).
            reduce(function(a, b) { return a.concat(b); });

        if (currSearchId != searchId) {
          return;
        }
        callback(nodes.filter(function(elem) {
          return regex.test(JSON.stringify(elem, null, 2));
        }), null);
      });
    } catch (err) {
      // Sometimes the provided regex is invalid.  This and other errors will
      // be caught and handled here.
      callback([], err);
    }
  };

  /**
   * Decorates the various search controls.
   *
   * @param {!HTMLInputElement} queryControl The <input> object of
   *     type=search where the user's query is typed.
   * @param {!HTMLButtonElement} submitControl The <button> object
   *     where the user can click to submit the query.
   * @param {!HTMLElement} statusControl The <span> object display the
   *     search status.
   * @param {!HTMLElement} listControl The <list> object which holds
   *     the list of returned results.
   * @param {!HTMLPreElement} detailsControl The <pre> object which
   *     holds the details of the selected result.
   */
  function decorateSearchControls(queryControl, submitControl, statusControl,
                                  resultsControl, detailsControl) {
    var resultsDataModel = new cr.ui.ArrayDataModel([]);

    var searchFunction = function() {
      var query = queryControl.value;
      statusControl.textContent = '';
      resultsDataModel.splice(0, resultsDataModel.length);
      if (!query) {
        return;
      }
      statusControl.textContent = 'Searching for ' + query + '...';
      queryControl.removeAttribute('error');
      var timer = chrome.sync.makeTimer();
      doSearch(query, function(nodes, error) {
        if (error) {
          statusControl.textContent = 'Error: ' + error;
          queryControl.setAttribute('error', '');
        } else {
          statusControl.textContent =
            'Found ' + nodes.length + ' nodes in ' +
            timer.getElapsedSeconds() + 's';
          queryControl.removeAttribute('error');

          // TODO(akalin): Write a nicer list display.
          for (var i = 0; i < nodes.length; ++i) {
            nodes[i].toString = function() {
              return this.NON_UNIQUE_NAME;
            };
          }
          resultsDataModel.push.apply(resultsDataModel, nodes);
          // Workaround for http://crbug.com/83452 .
          resultsControl.redraw();
        }
      });
    };

    submitControl.addEventListener('click', searchFunction);
    // Decorate search box.
    queryControl.onsearch = searchFunction;
    queryControl.value = '';

    // Decorate results list.
    cr.ui.List.decorate(resultsControl);
    resultsControl.dataModel = resultsDataModel;
    resultsControl.selectionModel.addEventListener('change', function(event) {
      detailsControl.textContent = '';
      var selected = resultsControl.selectedItem;
      if (selected) {
        detailsControl.textContent = JSON.stringify(selected, null, 2);
      }
    });
  }

  return {
    decorateSearchControls: decorateSearchControls,
    decorateQuickQueryControls: decorateQuickQueryControls
  };
});
// Copyright (c) 2012 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('chrome.sync.about_tab', function() {
  // Contains the latest snapshot of sync about info.
  chrome.sync.aboutInfo = {};

  function highlightIfChanged(node, oldVal, newVal) {
    function clearHighlight() {
      this.removeAttribute('highlighted');
    }

    var oldStr = oldVal.toString();
    var newStr = newVal.toString();
    if (oldStr != '' && oldStr != newStr) {
      // Note the addListener function does not end up creating duplicate
      // listeners.  There can be only one listener per event at a time.
      // Reference: https://developer.mozilla.org/en/DOM/element.addEventListener
      node.addEventListener('webkitAnimationEnd', clearHighlight, false);
      node.setAttribute('highlighted', '');
    }
  }

  function refreshAboutInfo(aboutInfo) {
    chrome.sync.aboutInfo = aboutInfo;
    var aboutInfoDiv = $('about-info');
    jstProcess(new JsEvalContext(aboutInfo), aboutInfoDiv);
  }

  function onAboutInfoUpdatedEvent(e) {
    refreshAboutInfo(e.details);
  }

  /**
   * Helper to determine if an element is scrolled to its bottom limit.
   * @param {Element} elem element to check
   * @return {boolean} true if the element is scrolled to the bottom
   */
  function isScrolledToBottom(elem) {
    return elem.scrollHeight - elem.scrollTop == elem.clientHeight;
  }

  /**
   * Helper to scroll an element to its bottom limit.
   * @param {Element} elem element to be scrolled
   */
  function scrollToBottom(elem) {
    elem.scrollTop = elem.scrollHeight - elem.clientHeight;
  }

  /** Container for accumulated sync protocol events. */
  var protocolEvents = [];

  /** We may receive re-delivered events.  Keep a record of ones we've seen. */
  var knownEventTimestamps = {};

  /**
   * Callback for incoming protocol events.
   * @param {Event} e The protocol event.
   */
  function onReceivedProtocolEvent(e) {
    var details = e.details;

    // Return early if we've seen this event before.  Assumes that timestamps
    // are sufficiently high resolution to uniquely identify an event.
    if (knownEventTimestamps.hasOwnProperty(details.time)) {
      return;
    }

    knownEventTimestamps[details.time] = true;
    protocolEvents.push(details);

    var trafficContainer = $('traffic-event-container');

    // Scroll to the bottom if we were already at the bottom.  Otherwise, leave
    // the scrollbar alone.
    var shouldScrollDown = isScrolledToBottom(trafficContainer);

    var context = new JsEvalContext({ events: protocolEvents });
    jstProcess(context, trafficContainer);

    if (shouldScrollDown)
      scrollToBottom(trafficContainer);
  }

  /**
   * Initializes state and callbacks for the protocol event log UI.
   */
  function initProtocolEventLog() {
    chrome.sync.events.addEventListener(
        'onProtocolEvent', onReceivedProtocolEvent);

    // Make the prototype jscontent element disappear.
    jstProcess({}, $('traffic-event-container'));
  }

  /**
   * Initializes listeners for status dump and import UI.
   */
  function initStatusDumpButton() {
    $('status-data').hidden = true;

    var dumpStatusButton = $('dump-status');
    dumpStatusButton.addEventListener('click', function(event) {
      var aboutInfo = chrome.sync.aboutInfo;
      if (!$('include-ids').checked) {
        aboutInfo.details = chrome.sync.aboutInfo.details.filter(function(el) {
          return !el.is_sensitive;
        });
      }
      var data = '';
      data += new Date().toString() + '\n';
      data += '======\n';
      data += 'Status\n';
      data += '======\n';
      data += JSON.stringify(aboutInfo, null, 2) + '\n';

      $('status-text').value = data;
      $('status-data').hidden = false;
    });

    var importStatusButton = $('import-status');
    importStatusButton.addEventListener('click', function(event) {
      $('status-data').hidden = false;
      if ($('status-text').value.length == 0) {
        $('status-text').value =
            'Paste sync status dump here then click import.';
        return;
      }

      // First remove any characters before the '{'.
      var data = $('status-text').value;
      var firstBrace = data.indexOf('{');
      if (firstBrace < 0) {
        $('status-text').value = 'Invalid sync status dump.';
        return;
      }
      data = data.substr(firstBrace);

      // Remove listeners to prevent sync events from overwriting imported data.
      chrome.sync.events.removeEventListener(
          'onAboutInfoUpdated',
          onAboutInfoUpdatedEvent);

      var aboutInfo = JSON.parse(data);
      refreshAboutInfo(aboutInfo);
    });
  }

  /**
   * Toggles the given traffic event entry div's "expanded" state.
   * @param {MouseEvent} e the click event that triggered the toggle.
   */
  function expandListener(e) {
    e.target.classList.toggle('traffic-event-entry-expanded');
  }

  /**
   * Attaches a listener to the given traffic event entry div.
   * @param {HTMLElement} element the element to attach the listener to.
   */
  function addExpandListener(element) {
    element.addEventListener('click', expandListener, false);
  }

  function onLoad() {
    initStatusDumpButton();
    initProtocolEventLog();

    chrome.sync.events.addEventListener(
        'onAboutInfoUpdated',
        onAboutInfoUpdatedEvent);

    // Register to receive a stream of event notifications.
    chrome.sync.registerForEvents();

    // Request an about info update event to initialize the page.
    chrome.sync.requestUpdatedAboutInfo();
  }

  return {
    onLoad: onLoad,
    addExpandListener: addExpandListener,
    highlightIfChanged: highlightIfChanged
  };
});

document.addEventListener(
    'DOMContentLoaded', chrome.sync.about_tab.onLoad, false);
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

(function() {
var dumpToTextButton = $('dump-to-text');
var dataDump = $('data-dump');
dumpToTextButton.addEventListener('click', function(event) {
  // TODO(akalin): Add info like Chrome version, OS, date dumped, etc.

  var data = '';
  data += '======\n';
  data += 'Status\n';
  data += '======\n';
  data += JSON.stringify(chrome.sync.aboutInfo, null, 2);
  data += '\n';
  data += '\n';

  data += '=============\n';
  data += 'Notifications\n';
  data += '=============\n';
  data += JSON.stringify(chrome.sync.notifications, null, 2);
  data += '\n';
  data += '\n';

  data += '===\n';
  data += 'Log\n';
  data += '===\n';
  data += JSON.stringify(chrome.sync.log.entries, null, 2);
  data += '\n';

  dataDump.textContent = data;
});

var allFields = [
  'ID',
  'IS_UNSYNCED',
  'IS_UNAPPLIED_UPDATE',
  'BASE_VERSION',
  'BASE_VERSION_TIME',
  'SERVER_VERSION',
  'SERVER_VERSION_TIME',
  'PARENT_ID',
  'SERVER_PARENT_ID',
  'IS_DEL',
  'SERVER_IS_DEL',
  'modelType',
  'SERVER_SPECIFICS',
  'SPECIFICS',
];

function versionToDateString(version) {
  // TODO(mmontgomery): ugly? Hacky? Is there a better way?
  var epochLength = Date.now().toString().length;
  var epochTime = parseInt(version.slice(0, epochLength));
  var date = new Date(epochTime);
  return date.toString();
}

/**
 * @param {!Object} node A JavaScript represenation of a sync entity.
 * @return {string} A string representation of the sync entity.
 */
function serializeNode(node) {
  return allFields.map(function(field) {
    var fieldVal;
    if (field == 'SERVER_VERSION_TIME') {
      var version = node['SERVER_VERSION'];
      fieldVal = versionToDateString(version);
    } if (field == 'BASE_VERSION_TIME') {
      var version = node['BASE_VERSION'];
      fieldVal = versionToDateString(version);
    } else if ((field == 'SERVER_SPECIFICS' || field == 'SPECIFICS') &&
            (!$('include-specifics').checked)) {
      fieldVal = 'REDACTED';
    } else if ((field == 'SERVER_SPECIFICS' || field == 'SPECIFICS') &&
            $('include-specifics').checked) {
      fieldVal = JSON.stringify(node[field]);
    } else {
      fieldVal = node[field];
    }
    return fieldVal;
  });
}

/**
 * @param {string} type The name of a sync model type.
 * @return {boolean} True if the type's checkbox is selected.
 */
function isSelectedDatatype(type) {
  var typeCheckbox = $(type);
  // Some types, such as 'Top level folder', appear in the list of nodes
  // but not in the list of selectable items.
  if (typeCheckbox == null) {
    return false;
  }
  return typeCheckbox.checked;
}

function makeBlobUrl(data) {
  var textBlob = new Blob([data], {type: 'octet/stream'});
  var blobUrl = window.URL.createObjectURL(textBlob);
  return blobUrl;
}

function makeDownloadName() {
  // Format is sync-data-dump-$epoch-$year-$month-$day-$OS.csv.
  var now = new Date();
  var friendlyDate = [now.getFullYear(),
                      now.getMonth() + 1,
                      now.getDate()].join('-');
  var name = ['sync-data-dump',
              friendlyDate,
              Date.now(),
              navigator.platform].join('-');
  return [name, 'csv'].join('.');
}

function makeDateUserAgentHeader() {
  var now = new Date();
  var userAgent = window.navigator.userAgent;
  var dateUaHeader = [now.toISOString(), userAgent].join(',');
  return dateUaHeader;
}

/**
 * Builds a summary of current state and exports it as a downloaded file.
 *
 * @param {!Array<{type: string, nodes: !Array<!Object>}>} nodesMap
 *     Summary of local state by model type.
 */
function triggerDataDownload(nodesMap) {
  // Prepend a header with ISO date and useragent.
  var output = [makeDateUserAgentHeader()];
  output.push('=====');

  var aboutInfo = JSON.stringify(chrome.sync.aboutInfo, null, 2);
  output.push(aboutInfo);

  // Filter out non-selected types.
  var selectedTypesNodes = nodesMap.filter(function(x) {
    return isSelectedDatatype(x.type);
  });

  // Serialize the remaining nodes and add them to the output.
  selectedTypesNodes.forEach(function(typeNodes) {
    output.push('=====');
    output.push(typeNodes.nodes.map(serializeNode).join('\n'));
  });

  output = output.join('\n');

  var anchor = $('dump-to-file-anchor');
  anchor.href = makeBlobUrl(output);
  anchor.download = makeDownloadName();
  anchor.click();
}

function createTypesCheckboxes(types) {
  var containerElt = $('node-type-checkboxes');

  types.map(function(type) {
    var div = document.createElement('div');

    var checkbox = document.createElement('input');
    checkbox.id = type;
    checkbox.type = 'checkbox';
    checkbox.checked = 'yes';
    div.appendChild(checkbox);

    var label = document.createElement('label');
    // Assigning to label.for doesn't work.
    label.setAttribute('for', type);
    label.innerText = type;
    div.appendChild(label);

    containerElt.appendChild(div);
  });
}

function onReceivedListOfTypes(e) {
  var types = e.details.types;
  types.sort();
  createTypesCheckboxes(types);
  chrome.sync.events.removeEventListener(
      'onReceivedListOfTypes',
      onReceivedListOfTypes);
}

document.addEventListener('DOMContentLoaded', function() {
  chrome.sync.events.addEventListener(
      'onReceivedListOfTypes',
      onReceivedListOfTypes);
  chrome.sync.requestListOfTypes();
});

var dumpToFileLink = $('dump-to-file');
dumpToFileLink.addEventListener('click', function(event) {
  chrome.sync.getAllNodes(triggerDataDownload);
});
})();
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

cr.define('chrome.sync.events_tab', function() {
  'use strict';

  function toggleDisplay(event) {
    var originatingButton = event.target;
    if (originatingButton.className != 'toggle-button') {
      return;
    }
    var detailsNode = originatingButton.parentNode.getElementsByClassName(
        'details')[0];
    var detailsColumn = detailsNode.parentNode;
    var detailsRow = detailsColumn.parentNode;

    if (!detailsRow.classList.contains('expanded')) {
      detailsRow.classList.toggle('expanded');
      detailsColumn.setAttribute('colspan', 4);
      detailsNode.removeAttribute('hidden');
    } else {
      detailsNode.setAttribute('hidden', '');
      detailsColumn.removeAttribute('colspan');
      detailsRow.classList.toggle('expanded');
    }
  };

  function displaySyncEvents() {
    var entries = chrome.sync.log.entries;
    var eventTemplateContext = {
      eventList: entries,
    };
    var context = new JsEvalContext(eventTemplateContext);
    jstProcess(context, $('sync-events'));
  };

  function onLoad() {
    $('sync-events').addEventListener('click', toggleDisplay);
    chrome.sync.log.addEventListener('append', function(event) {
      displaySyncEvents();
    });
  }

  return {
    onLoad: onLoad
  };
});

document.addEventListener(
    'DOMContentLoaded', chrome.sync.events_tab.onLoad, false);
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// require: cr/ui.js
// require: util.js

cr.ui.decorate('#sync-results-splitter', cr.ui.Splitter);

var allLinks = document.getElementsByClassName('sync-search-quicklink');

chrome.sync.decorateQuickQueryControls(
  allLinks,
  $('sync-search-submit'),
  $('sync-search-query'));

chrome.sync.decorateSearchControls(
  $('sync-search-query'),
  $('sync-search-submit'),
  $('sync-search-status'),
  $('sync-results-list'),
  $('sync-result-details'));
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

// This code is used in conjunction with the Google Translate Element script.
// It is executed in an isolated world of a page to translate it from one
// language to another.
// It should be included in the page before the Translate Element script.

var cr = cr || {};

/**
 * An object to provide functions to interact with the Translate library.
 * @type {object}
 */
cr.googleTranslate = (function() {
  /**
   * The Translate Element library's instance.
   * @type {object}
   */
  var lib;

  /**
   * A flag representing if the Translate Element library is initialized.
   * @type {boolean}
   */
  var libReady = false;

  /**
   * Error definitions for |errorCode|. See chrome/common/translate_errors.h
   * to modify the definition.
   * @const
   */
  var ERROR = {
    'NONE': 0,
    'INITIALIZATION_ERROR': 2,
    'UNSUPPORTED_LANGUAGE': 4,
    'TRANSLATION_ERROR': 6,
    'TRANSLATION_TIMEOUT': 7,
    'UNEXPECTED_SCRIPT_ERROR': 8,
    'BAD_ORIGIN': 9,
    'SCRIPT_LOAD_ERROR': 10
  };

  /**
   * Error code map from te.dom.DomTranslator.Error to |errorCode|.
   * See also go/dom_translator.js in google3.
   * @const
   */
  var TRANSLATE_ERROR_TO_ERROR_CODE_MAP = {
    0: ERROR['NONE'],
    1: ERROR['TRANSLATION_ERROR'],
    2: ERROR['UNSUPPORTED_LANGUAGE']
  };

  /**
   * An error code happened in translate.js and the Translate Element library.
   */
  var errorCode = ERROR['NONE'];

  /**
   * A flag representing if the Translate Element has finished a translation.
   * @type {boolean}
   */
  var finished = false;

  /**
   * Counts how many times the checkLibReady function is called. The function
   * is called in every 100 msec and counted up to 6.
   * @type {number}
   */
  var checkReadyCount = 0;

  /**
   * Time in msec when this script is injected.
   * @type {number}
   */
  var injectedTime = performance.now();

  /**
   * Time in msec when the Translate Element library is loaded completely.
   * @type {number}
   */
  var loadedTime = 0.0;

  /**
   * Time in msec when the Translate Element library is initialized and ready
   * for performing translation.
   * @type {number}
   */
  var readyTime = 0.0;

  /**
   * Time in msec when the Translate Element library starts a translation.
   * @type {number}
   */
  var startTime = 0.0;

  /**
   * Time in msec when the Translate Element library ends a translation.
   * @type {number}
   */
  var endTime = 0.0;

  function checkLibReady() {
    if (lib.isAvailable()) {
      readyTime = performance.now();
      libReady = true;
      return;
    }
    if (checkReadyCount++ > 5) {
      errorCode = ERROR['TRANSLATION_TIMEOUT'];
      return;
    }
    setTimeout(checkLibReady, 100);
  }

  function onTranslateProgress(progress, opt_finished, opt_error) {
    finished = opt_finished;
    // opt_error can be 'undefined'.
    if (typeof opt_error == 'boolean' && opt_error) {
      // TODO(toyoshim): Remove boolean case once a server is updated.
      errorCode = ERROR['TRANSLATION_ERROR'];
      // We failed to translate, restore so the page is in a consistent state.
      lib.restore();
    } else if (typeof opt_error == 'number' && opt_error != 0) {
      errorCode = TRANSLATE_ERROR_TO_ERROR_CODE_MAP[opt_error];
      lib.restore();
    }
    if (finished)
      endTime = performance.now();
  }

  // Public API.
  return {
    /**
     * Whether the library is ready.
     * The translate function should only be called when |libReady| is true.
     * @type {boolean}
     */
    get libReady() {
      return libReady;
    },

    /**
     * Whether the current translate has finished successfully.
     * @type {boolean}
     */
    get finished() {
      return finished;
    },

    /**
     * Whether an error occured initializing the library of translating the
     * page.
     * @type {boolean}
     */
    get error() {
      return errorCode != ERROR['NONE'];
    },

    /**
     * Returns a number to represent error type.
     * @type {number}
     */
    get errorCode() {
      return errorCode;
    },

    /**
     * The language the page translated was in. Is valid only after the page
     * has been successfully translated and the original language specified to
     * the translate function was 'auto'. Is empty otherwise.
     * Some versions of Element library don't provide |getDetectedLanguage|
     * function. In that case, this function returns 'und'.
     * @type {boolean}
     */
    get sourceLang() {
      if (!libReady || !finished || errorCode != ERROR['NONE'])
        return '';
      if (!lib.getDetectedLanguage)
        return 'und'; // Defined as translate::kUnknownLanguageCode in C++.
      return lib.getDetectedLanguage();
    },

    /**
     * Time in msec from this script being injected to all server side scripts
     * being loaded.
     * @type {number}
     */
    get loadTime() {
      if (loadedTime == 0)
        return 0;
      return loadedTime - injectedTime;
    },

    /**
     * Time in msec from this script being injected to the Translate Element
     * library being ready.
     * @type {number}
     */
    get readyTime() {
      if (!libReady)
        return 0;
      return readyTime - injectedTime;
    },

    /**
     * Time in msec to perform translation.
     * @type {number}
     */
    get translationTime() {
      if (!finished)
        return 0;
      return endTime - startTime;
    },

    /**
     * Translate the page contents.  Note that the translation is asynchronous.
     * You need to regularly check the state of |finished| and |errorCode| to
     * know if the translation finished or if there was an error.
     * @param {string} originalLang The language the page is in.
     * @param {string} targetLang The language the page should be translated to.
     * @return {boolean} False if the translate library was not ready, in which
     *                   case the translation is not started.  True otherwise.
     */
    translate: function(originalLang, targetLang) {
      finished = false;
      errorCode = ERROR['NONE'];
      if (!libReady)
        return false;
      startTime = performance.now();
      try {
        lib.translatePage(originalLang, targetLang, onTranslateProgress);
      } catch (err) {
        console.error('Translate: ' + err);
        errorCode = ERROR['UNEXPECTED_SCRIPT_ERROR'];
        return false;
      }
      return true;
    },

    /**
     * Reverts the page contents to its original value, effectively reverting
     * any performed translation.  Does nothing if the page was not translated.
     */
    revert: function() {
      lib.restore();
    },

    /**
     * Entry point called by the Translate Element once it has been injected in
     * the page.
     */
    onTranslateElementLoad: function() {
      loadedTime = performance.now();
      try {
        lib = google.translate.TranslateService({
          // translateApiKey is predefined by translate_script.cc.
          'key': translateApiKey,
          'useSecureConnection': true
        });
        translateApiKey = undefined;
      } catch (err) {
        errorCode = ERROR['INITIALIZATION_ERROR'];
        translateApiKey = undefined;
        return;
      }
      // The TranslateService is not available immediately as it needs to start
      // Flash.  Let's wait until it is ready.
      checkLibReady();
    },

    /**
     * Entry point called by the Translate Element when it want to load an
     * external CSS resource into the page.
     * @param {string} url URL of an external CSS resource to load.
     */
    onLoadCSS: function(url) {
      var element = document.createElement('link');
      element.type = 'text/css';
      element.rel = 'stylesheet';
      element.charset = 'UTF-8';
      element.href = url;
      document.head.appendChild(element);
    },

    /**
     * Entry point called by the Translate Element when it want to load and run
     * an external JavaScript on the page.
     * @param {string} url URL of an external JavaScript to load.
     */
    onLoadJavascript: function(url) {
      // securityOrigin is predefined by translate_script.cc.
      if (url.indexOf(securityOrigin) != 0) {
        console.error('Translate: ' + url + ' is not allowed to load.');
        errorCode = ERROR['BAD_ORIGIN'];
        return;
      }
      var xhr = new XMLHttpRequest();
      xhr.open('GET', url, true);
      xhr.onreadystatechange = function() {
        if (this.readyState != this.DONE)
          return;
        if (this.status != 200) {
          errorCode = ERROR['SCRIPT_LOAD_ERROR'];
          return;
        }
        eval(this.responseText);
      }
      xhr.send();
    }
  };
})();
/* Copyright (c) 2012 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file. */

body {
  background-color: white;
  color: black;
  font-size: 100%;
  margin: 0;
}

#outer {
  margin-left: auto;
  margin-right: auto;
  margin-top: 10px;
  width: 800px;
}

#inner {
  padding-top: 10px;
  word-break: break-word;
}

.label {
  -webkit-padding-end: 5px;
  font-size: 0.9em;
  font-weight: bold;
  text-align: end;
  vertical-align: top;
  white-space: nowrap;
}

.label::after {
  content: ':';
}

#logo {
  float: right;
  margin-left: 40px;
  text-align: right;
  width: 180px;
}

#company {
  font-size: 0.7em;
  text-align: right;
}

#copyright {
  font-size: 0.7em;
  text-align: right;
}

#useragent {
  font-family: monospace;
}

.version {
  font-family: monospace;
  max-width: 430px;
  padding-left: 5px;
  vertical-align: bottom;
}
<!doctype html>

<!--
about:version template page
-->

<html id="t" i18n-values="dir:textdirection;lang:language">
  <head>
    <meta charset="utf-8">
    <title i18n-content="title"></title>
    <link rel="stylesheet" href="chrome://resources/css/text_defaults.css">

    <link rel="stylesheet" href="chrome://version/about_version.css">




    <script src="chrome://resources/js/cr.js"></script>
    <script src="chrome://resources/js/load_time_data.js"></script>
    <script src="chrome://resources/js/parse_html_subset.js"></script>
    <script src="chrome://resources/js/util.js"></script>
    <script src="chrome://version/version.js"></script>
    <script src="chrome://version/strings.js"></script>
  </head>

  <body>
    <div id="outer">
      <div id="logo">
<img src="chrome://theme/IDR_PRODUCT_LOGO">

        <div id="company" i18n-content="company"></div>
        <div id="copyright" i18n-content="copyright"></div>
      </div>
      <table id="inner" cellpadding="0" cellspacing="0" border="0">
        <tr><td class="label" i18n-content="application_label"></td>
          <td class="version" id="version">
            <span i18n-content="version"></span>
            (<span i18n-content="official"></span>)
            <span i18n-content="version_modifier"></span>
            <span i18n-content="version_bitsize"></span>
          </td>
        </tr>
        <tr>
          <td class="label" i18n-content="revision"></td>
          <td class="version">
            <span i18n-content="cl"></span>
          </td>
        </tr>
<tr><td class="label" i18n-content="os_name"></td>
          <td class="version" id="os_type">
            <span i18n-content="os_type"></span>
            <span id="os_version" i18n-content="os_version"></span>
          </td>

        </tr>
<tr><td class="label">Blink</td>
          <td class="version" id="blink_version" i18n-content="blink_version"></td>
        </tr>
        <tr><td class="label">JavaScript</td>
          <td class="version" id="js_engine">
            <span i18n-content="js_engine"></span>
            <span i18n-content="js_version"></span>
          </td>
        </tr>
<tr><td class="label" i18n-content="flash_plugin"></td>
          <td class="version" id="flash_version" i18n-content="flash_version"></td>
        </tr>
        <tr><td class="label" i18n-content="user_agent_name"></td>
          <td class="version" id="useragent" i18n-content="useragent"></td>
        </tr>
        <tr><td class="label" i18n-content="command_line_name"></td>
          <td class="version" id="command_line" i18n-content="command_line"></td>
        </tr>
<tr><td class="label" i18n-content="executable_path_name"></td>
          <td class="version" id="executable_path" i18n-content="executable_path"></td>
        </tr>
        <tr><td class="label" i18n-content="profile_path_name"></td>
          <td class="version" id="profile_path" i18n-content="profile_path"></td>
        </tr>
        <tr id="variations-section">
          <td class="label" i18n-content="variations_name"></td>
          <td class="version" id="variations-list"></td>
        </tr>

<tr id="compiler-section">
          <td class="label">Compiler</td>
          <td class="version" id="compiler" i18n-content="compiler"></td>
        </tr>
      </table>
    </div>
    <script src="chrome://resources/js/i18n_template.js"></script>
  </body>
</html>
// Copyright (c) 2011 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

/**
 * Callback from the backend with the list of variations to display.
 * This call will build the variations section of the version page, or hide that
 * section if there are none to display.
 * @param {!Array<string>} variationsList The list of variations.
 */
function returnVariationInfo(variationsList) {
  $('variations-section').hidden = !variationsList.length;
  $('variations-list').appendChild(
      parseHtmlSubset(variationsList.join('<br>'), ['BR']));
}

/**
 * Callback from the backend with the executable and profile paths to display.
 * @param {string} execPath The executable path to display.
 * @param {string} profilePath The profile path to display.
 */
function returnFilePaths(execPath, profilePath) {
  $('executable_path').textContent = execPath;
  $('profile_path').textContent = profilePath;
}

/**
 * Callback from the backend with the Flash version to display.
 * @param {string} flashVersion The Flash version to display.
 */
function returnFlashVersion(flashVersion) {
  $('flash_version').textContent = flashVersion;
}

/**
 * Callback from the backend with the OS version to display.
 * @param {string} osVersion The OS version to display.
 */
function returnOsVersion(osVersion) {
  $('os_version').textContent = osVersion;
}

/* All the work we do onload. */
function onLoadWork() {
  chrome.send('requestVersionInfo');
}

document.addEventListener('DOMContentLoaded', onLoadWork);
/*
 * The default style sheet used to render HTML.
 *
 * Copyright (C) 2000 Lars Knoll (knoll@kde.org)
 * Copyright (C) 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

@namespace "http://www.w3.org/1999/xhtml";

html {
    display: block
}

/* children of the <head> element all have display:none */
head {
    display: none
}

meta {
    display: none
}

title {
    display: none
}

link {
    display: none
}

style {
    display: none
}

script {
    display: none
}

/* generic block-level elements */

body {
    display: block;
    margin: 8px
}

body:-webkit-full-page-media {
    background-color: rgb(0, 0, 0)
}

p {
    display: block;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1__qem;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
}

div {
    display: block
}

layer {
    display: block
}

article, aside, footer, header, hgroup, main, nav, section {
    display: block
}

marquee {
    display: inline-block;
}

address {
    display: block
}

blockquote {
    display: block;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 40px;
    -webkit-margin-end: 40px;
}

figcaption {
    display: block
}

figure {
    display: block;
    -webkit-margin-before: 1em;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 40px;
    -webkit-margin-end: 40px;
}

q {
    display: inline
}

q:before {
    content: open-quote;
}

q:after {
    content: close-quote;
}

center {
    display: block;
    /* special centering to be able to emulate the html4/netscape behaviour */
    text-align: -webkit-center
}

hr {
    display: block;
    -webkit-margin-before: 0.5em;
    -webkit-margin-after: 0.5em;
    -webkit-margin-start: auto;
    -webkit-margin-end: auto;
    border-style: inset;
    border-width: 1px
}

map {
    display: inline
}

video {
    object-fit: contain;
}

/* heading elements */

h1 {
    display: block;
    font-size: 2em;
    -webkit-margin-before: 0.67__qem;
    -webkit-margin-after: 0.67em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

:-webkit-any(article,aside,nav,section) h1 {
    font-size: 1.5em;
    -webkit-margin-before: 0.83__qem;
    -webkit-margin-after: 0.83em;
}

:-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) h1 {
    font-size: 1.17em;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
}

:-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) h1 {
    font-size: 1.00em;
    -webkit-margin-before: 1.33__qem;
    -webkit-margin-after: 1.33em;
}

:-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) h1 {
    font-size: .83em;
    -webkit-margin-before: 1.67__qem;
    -webkit-margin-after: 1.67em;
}

:-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) :-webkit-any(article,aside,nav,section) h1 {
    font-size: .67em;
    -webkit-margin-before: 2.33__qem;
    -webkit-margin-after: 2.33em;
}

h2 {
    display: block;
    font-size: 1.5em;
    -webkit-margin-before: 0.83__qem;
    -webkit-margin-after: 0.83em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

h3 {
    display: block;
    font-size: 1.17em;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

h4 {
    display: block;
    -webkit-margin-before: 1.33__qem;
    -webkit-margin-after: 1.33em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

h5 {
    display: block;
    font-size: .83em;
    -webkit-margin-before: 1.67__qem;
    -webkit-margin-after: 1.67em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

h6 {
    display: block;
    font-size: .67em;
    -webkit-margin-before: 2.33__qem;
    -webkit-margin-after: 2.33em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    font-weight: bold
}

/* tables */

table {
    display: table;
    border-collapse: separate;
    border-spacing: 2px;
    border-color: gray
}

thead {
    display: table-header-group;
    vertical-align: middle;
    border-color: inherit
}

tbody {
    display: table-row-group;
    vertical-align: middle;
    border-color: inherit
}

tfoot {
    display: table-footer-group;
    vertical-align: middle;
    border-color: inherit
}

/* for tables without table section elements (can happen with XHTML or dynamically created tables) */
table > tr {
    vertical-align: middle;
}

col {
    display: table-column
}

colgroup {
    display: table-column-group
}

tr {
    display: table-row;
    vertical-align: inherit;
    border-color: inherit
}

td, th {
    display: table-cell;
    vertical-align: inherit
}

th {
    font-weight: bold
}

caption {
    display: table-caption;
    text-align: -webkit-center
}

/* lists */

ul, menu, dir {
    display: block;
    list-style-type: disc;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    -webkit-padding-start: 40px
}

ol {
    display: block;
    list-style-type: decimal;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
    -webkit-padding-start: 40px
}

li {
    display: list-item;
    text-align: -webkit-match-parent;
}

ul ul, ol ul {
    list-style-type: circle
}

ol ol ul, ol ul ul, ul ol ul, ul ul ul {
    list-style-type: square
}

dd {
    display: block;
    -webkit-margin-start: 40px
}

dl {
    display: block;
    -webkit-margin-before: 1__qem;
    -webkit-margin-after: 1em;
    -webkit-margin-start: 0;
    -webkit-margin-end: 0;
}

dt {
    display: block
}

ol ul, ul ol, ul ul, ol ol {
    -webkit-margin-before: 0;
    -webkit-margin-after: 0
}

/* form elements */

form {
    display: block;
    margin-top: 0__qem;
}

label {
    cursor: default;
}

legend {
    display: block;
    -webkit-padding-start: 2px;
    -webkit-padding-end: 2px;
    border: none
}

fieldset {
    display: block;
    -webkit-margin-start: 2px;
    -webkit-margin-end: 2px;
    -webkit-padding-before: 0.35em;
    -webkit-padding-start: 0.75em;
    -webkit-padding-end: 0.75em;
    -webkit-padding-after: 0.625em;
    border: 2px groove ThreeDFace;
    min-width: -webkit-min-content;
}

button {
    -webkit-appearance: button;
}

/* Form controls don't go vertical. */
input, textarea, keygen, select, button, meter, progress {
    -webkit-writing-mode: horizontal-tb !important;
}

input, textarea, keygen, select, button {
    margin: 0__qem;
    font: -webkit-small-control;
    text-rendering: auto; /* FIXME: Remove when tabs work with optimizeLegibility. */
    color: initial;
    letter-spacing: normal;
    word-spacing: normal;
    line-height: normal;
    text-transform: none;
    text-indent: 0;
    text-shadow: none;
    display: inline-block;
    text-align: start;
}

input[type="hidden" i] {
    display: none
}

input {
    -webkit-appearance: textfield;
    padding: 1px;
    background-color: white;
    border: 2px inset;
    -webkit-rtl-ordering: logical;
    -webkit-user-select: text;
    cursor: auto;
}

input[type="search" i] {
    -webkit-appearance: searchfield;
    box-sizing: border-box;
}

input::-webkit-textfield-decoration-container {
    display: flex;
    align-items: center;
    -webkit-user-modify: read-only !important;
    content: none !important;
}

input[type="search" i]::-webkit-textfield-decoration-container {
    direction: ltr;
}

input::-webkit-clear-button {
    -webkit-appearance: searchfield-cancel-button;
    display: inline-block;
    flex: none;
    -webkit-user-modify: read-only !important;
    -webkit-margin-start: 2px;
    opacity: 0;
    pointer-events: none;
}

input:enabled:read-write:-webkit-any(:focus,:hover)::-webkit-clear-button {
    opacity: 1;
    pointer-events: auto;
}

input[type="search" i]::-webkit-search-cancel-button {
    -webkit-appearance: searchfield-cancel-button;
    display: block;
    flex: none;
    -webkit-user-modify: read-only !important;
    -webkit-margin-start: 1px;
    opacity: 0;
    pointer-events: none;
}

input[type="search" i]:enabled:read-write:-webkit-any(:focus,:hover)::-webkit-search-cancel-button {
    opacity: 1;
    pointer-events: auto;
}

input[type="search" i]::-webkit-search-decoration {
    -webkit-appearance: searchfield-decoration;
    display: block;
    flex: none;
    -webkit-user-modify: read-only !important;
    -webkit-align-self: flex-start;
    margin: auto 0;
}

input[type="search" i]::-webkit-search-results-decoration {
    -webkit-appearance: searchfield-results-decoration;
    display: block;
    flex: none;
    -webkit-user-modify: read-only !important;
    -webkit-align-self: flex-start;
    margin: auto 0;
}

input::-webkit-inner-spin-button {
    -webkit-appearance: inner-spin-button;
    display: inline-block;
    cursor: default;
    flex: none;
    align-self: stretch;
    -webkit-user-select: none;
    -webkit-user-modify: read-only !important;
    opacity: 0;
    pointer-events: none;
}

input:enabled:read-write:-webkit-any(:focus,:hover)::-webkit-inner-spin-button {
    opacity: 1;
    pointer-events: auto;
}

keygen, select {
    border-radius: 5px;
}

keygen::-webkit-keygen-select {
    margin: 0px;
}

textarea {
    -webkit-appearance: textarea;
    background-color: white;
    border: 1px solid;
    -webkit-rtl-ordering: logical;
    -webkit-user-select: text;
    flex-direction: column;
    resize: auto;
    cursor: auto;
    padding: 2px;
    white-space: pre-wrap;
    word-wrap: break-word;
}

::-webkit-input-placeholder {
    -webkit-text-security: none;
    color: darkGray;
    pointer-events: none !important;
}

input::-webkit-input-placeholder {
    line-height: initial;
    white-space: pre;
    word-wrap: normal;
    overflow: hidden;
    -webkit-user-modify: read-only !important;
}

input[type="password" i] {
    -webkit-text-security: disc !important;
}

input[type="hidden" i], input[type="image" i], input[type="file" i] {
    -webkit-appearance: initial;
    padding: initial;
    background-color: initial;
    border: initial;
}

input[type="file" i] {
    align-items: baseline;
    color: inherit;
    text-align: start !important;
}

input:-webkit-autofill, textarea:-webkit-autofill, select:-webkit-autofill {
    background-color: #FAFFBD !important;
    background-image:none !important;
    color: #000000 !important;
}

input[type="radio" i], input[type="checkbox" i] {
    margin: 3px 0.5ex;
    padding: initial;
    background-color: initial;
    border: initial;
}

input[type="button" i], input[type="submit" i], input[type="reset" i] {
    -webkit-appearance: push-button;
    -webkit-user-select: none;
    white-space: pre
}

input[type="file" i]::-webkit-file-upload-button {
    -webkit-appearance: push-button;
    -webkit-user-modify: read-only !important;
    white-space: nowrap;
    margin: 0;
    font-size: inherit;
}

input[type="button" i], input[type="submit" i], input[type="reset" i], input[type="file" i]::-webkit-file-upload-button, button {
    align-items: flex-start;
    text-align: center;
    cursor: default;
    color: ButtonText;
    padding: 2px 6px 3px 6px;
    border: 2px outset ButtonFace;
    background-color: ButtonFace;
    box-sizing: border-box
}

input[type="range" i] {
    -webkit-appearance: slider-horizontal;
    padding: initial;
    border: initial;
    margin: 2px;
    color: #909090;
}

input[type="range" i]::-webkit-slider-container, input[type="range" i]::-webkit-media-slider-container {
    flex: 1;
    min-width: 0;
    box-sizing: border-box;
    -webkit-user-modify: read-only !important;
    display: flex;
}

input[type="range" i]::-webkit-slider-runnable-track {
    flex: 1;
    min-width: 0;
    -webkit-align-self: center;

    box-sizing: border-box;
    -webkit-user-modify: read-only !important;
    display: block;
}

input[type="range" i]::-webkit-slider-thumb, input[type="range" i]::-webkit-media-slider-thumb {
    -webkit-appearance: sliderthumb-horizontal;
    box-sizing: border-box;
    -webkit-user-modify: read-only !important;
    display: block;
}

input[type="button" i]:disabled, input[type="submit" i]:disabled, input[type="reset" i]:disabled,
input[type="file" i]:disabled::-webkit-file-upload-button, button:disabled,
select:disabled, keygen:disabled, optgroup:disabled, option:disabled,
select[disabled]>option {
    color: GrayText
}

input[type="button" i]:active, input[type="submit" i]:active, input[type="reset" i]:active, input[type="file" i]:active::-webkit-file-upload-button, button:active {
    border-style: inset
}

input[type="button" i]:active:disabled, input[type="submit" i]:active:disabled, input[type="reset" i]:active:disabled, input[type="file" i]:active:disabled::-webkit-file-upload-button, button:active:disabled {
    border-style: outset
}

input:disabled, textarea:disabled {
    color: #545454;
}

option:-internal-spatial-navigation-focus {
    outline: black dashed 1px;
    outline-offset: -1px;
}

datalist {
    display: none
}

area {
    display: inline;
    cursor: pointer;
}

param {
    display: none
}

input[type="checkbox" i] {
    -webkit-appearance: checkbox;
    box-sizing: border-box;
}

input[type="radio" i] {
    -webkit-appearance: radio;
    box-sizing: border-box;
}

input[type="color" i] {
    -webkit-appearance: square-button;
    width: 44px;
    height: 23px;
    background-color: ButtonFace;
    /* Same as native_theme_base. */
    border: 1px #a9a9a9 solid;
    padding: 1px 2px;
}

input[type="color" i]::-webkit-color-swatch-wrapper {
    display:flex;
    padding: 4px 2px;
    box-sizing: border-box;
    -webkit-user-modify: read-only !important;
    width: 100%;
    height: 100%
}

input[type="color" i]::-webkit-color-swatch {
    background-color: #000000;
    border: 1px solid #777777;
    flex: 1;
    min-width: 0;
    -webkit-user-modify: read-only !important;
}

input[type="color" i][list] {
    -webkit-appearance: menulist;
    width: 88px;
    height: 23px
}

input[type="color" i][list]::-webkit-color-swatch-wrapper {
    padding-left: 8px;
    padding-right: 24px;
}

input[type="color" i][list]::-webkit-color-swatch {
    border-color: #000000;
}

input::-webkit-calendar-picker-indicator {
    display: inline-block;
    width: 0.66em;
    height: 0.66em;
    padding: 0.17em 0.34em;
    -webkit-user-modify: read-only !important;
    opacity: 0;
    pointer-events: none;
}

input::-webkit-calendar-picker-indicator:hover {
    background-color: #eee;
}

input:enabled:read-write:-webkit-any(:focus,:hover)::-webkit-calendar-picker-indicator,
input::-webkit-calendar-picker-indicator:focus {
    opacity: 1;
    pointer-events: auto;
}

input[type="date" i]:disabled::-webkit-clear-button,
input[type="date" i]:disabled::-webkit-inner-spin-button,
input[type="datetime-local" i]:disabled::-webkit-clear-button,
input[type="datetime-local" i]:disabled::-webkit-inner-spin-button,
input[type="month" i]:disabled::-webkit-clear-button,
input[type="month" i]:disabled::-webkit-inner-spin-button,
input[type="week" i]:disabled::-webkit-clear-button,
input[type="week" i]:disabled::-webkit-inner-spin-button,
input:disabled::-webkit-calendar-picker-indicator,
input[type="date" i][readonly]::-webkit-clear-button,
input[type="date" i][readonly]::-webkit-inner-spin-button,
input[type="datetime-local" i][readonly]::-webkit-clear-button,
input[type="datetime-local" i][readonly]::-webkit-inner-spin-button,
input[type="month" i][readonly]::-webkit-clear-button,
input[type="month" i][readonly]::-webkit-inner-spin-button,
input[type="week" i][readonly]::-webkit-clear-button,
input[type="week" i][readonly]::-webkit-inner-spin-button,
input[readonly]::-webkit-calendar-picker-indicator {
    visibility: hidden;
}

select {
    -webkit-appearance: menulist;
    box-sizing: border-box;
    align-items: center;
    border: 1px solid;
    white-space: pre;
    -webkit-rtl-ordering: logical;
    color: black;
    background-color: white;
    cursor: default;
}

select:not(:-internal-list-box) {
    overflow: visible !important;
}

select:-internal-list-box {
    -webkit-appearance: listbox;
    align-items: flex-start;
    border: 1px inset gray;
    border-radius: initial;
    overflow-x: hidden;
    overflow-y: scroll;
    vertical-align: text-bottom;
    -webkit-user-select: none;
    white-space: nowrap;
}

optgroup {
    font-weight: bolder;
    display: block;
}

option {
    font-weight: normal;
    display: block;
    padding: 0 2px 1px 2px;
    white-space: pre;
    min-height: 1.2em;
}

select:-internal-list-box optgroup option:before {
    content: "\00a0\00a0\00a0\00a0";;
}

select:-internal-list-box option,
select:-internal-list-box optgroup {
    line-height: initial !important;
}

select:-internal-list-box:focus option:checked {
    background-color: -internal-active-list-box-selection !important;
    color: -internal-active-list-box-selection-text !important;
}

select:-internal-list-box:focus option:checked:disabled {
    background-color: -internal-inactive-list-box-selection !important;
}

select:-internal-list-box option:checked {
    background-color: -internal-inactive-list-box-selection !important;
    color: -internal-inactive-list-box-selection-text !important;
}

select:-internal-list-box:disabled option:checked,
select:-internal-list-box option:checked:disabled {
    color: gray !important;
}

select:-internal-list-box hr {
    border-style: none;
}

output {
    display: inline;
}

/* meter */

meter {
    -webkit-appearance: meter;
    box-sizing: border-box;
    display: inline-block;
    height: 1em;
    width: 5em;
    vertical-align: -0.2em;
}

meter::-webkit-meter-inner-element {
    -webkit-appearance: inherit;
    box-sizing: inherit;
    -webkit-user-modify: read-only !important;
    height: 100%;
    width: 100%;
}

meter::-webkit-meter-bar {
    background: linear-gradient(to bottom, #ddd, #eee 20%, #ccc 45%, #ccc 55%, #ddd);
    height: 100%;
    width: 100%;
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

meter::-webkit-meter-optimum-value {
    background: linear-gradient(to bottom, #ad7, #cea 20%, #7a3 45%, #7a3 55%, #ad7);
    height: 100%;
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

meter::-webkit-meter-suboptimum-value {
    background: linear-gradient(to bottom, #fe7, #ffc 20%, #db3 45%, #db3 55%, #fe7);
    height: 100%;
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

meter::-webkit-meter-even-less-good-value {
    background: linear-gradient(to bottom, #f77, #fcc 20%, #d44 45%, #d44 55%, #f77);
    height: 100%;
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

/* progress */

progress {
    -webkit-appearance: progress-bar;
    box-sizing: border-box;
    display: inline-block;
    height: 1em;
    width: 10em;
    vertical-align: -0.2em;
}

progress::-webkit-progress-inner-element {
    -webkit-appearance: inherit;
    box-sizing: inherit;
    -webkit-user-modify: read-only;
    height: 100%;
    width: 100%;
}

progress::-webkit-progress-bar {
    background-color: gray;
    height: 100%;
    width: 100%;
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

progress::-webkit-progress-value {
    background-color: green;
    height: 100%;
    width: 50%; /* should be removed later */
    -webkit-user-modify: read-only !important;
    box-sizing: border-box;
}

/* inline elements */

u, ins {
    text-decoration: underline
}

strong, b {
    font-weight: bold
}

i, cite, em, var, address, dfn {
    font-style: italic
}

tt, code, kbd, samp {
    font-family: monospace
}

pre, xmp, plaintext, listing {
    display: block;
    font-family: monospace;
    white-space: pre;
    margin: 1__qem 0
}

mark {
    background-color: yellow;
    color: black
}

big {
    font-size: larger
}

small {
    font-size: smaller
}

s, strike, del {
    text-decoration: line-through
}

sub {
    vertical-align: sub;
    font-size: smaller
}

sup {
    vertical-align: super;
    font-size: smaller
}

nobr {
    white-space: nowrap
}

/* states */

:focus { 
    outline: auto 5px -webkit-focus-ring-color
}

/* Read-only text fields do not show a focus ring but do still receive focus */
html:focus, body:focus, input[readonly]:focus { 
    outline: none
}

embed:focus, iframe:focus, object:focus {
    outline: none
}
  
input:focus, textarea:focus, keygen:focus, select:focus {
    outline-offset: -2px
}

input[type="button" i]:focus,
input[type="checkbox" i]:focus,
input[type="file" i]:focus,
input[type="hidden" i]:focus,
input[type="image" i]:focus,
input[type="radio" i]:focus,
input[type="reset" i]:focus,
input[type="submit" i]:focus,
input[type="file" i]:focus::-webkit-file-upload-button {
    outline-offset: 0
}
    
a:-webkit-any-link {
    color: -webkit-link;
    text-decoration: underline;
    cursor: auto;
}

a:-webkit-any-link:active {
    color: -webkit-activelink
}

/* HTML5 ruby elements */

ruby, rt {
    text-indent: 0; /* blocks used for ruby rendering should not trigger this */
}

rt {
    line-height: normal;
    -webkit-text-emphasis: none;
}

ruby > rt {
    display: block;
    font-size: 50%;
    text-align: start;
}

ruby > rp {
    display: none;
}

/* other elements */

noframes {
    display: none
}

frameset, frame {
    display: block
}

frameset {
    border-color: inherit
}

iframe {
    border: 2px inset
}

details {
    display: block
}

summary {
    display: block
}

summary::-webkit-details-marker {
    display: inline-block;
    width: 0.66em;
    height: 0.66em;
    -webkit-margin-end: 0.4em;
}

template {
    display: none
}

bdi, output {
    unicode-bidi: -webkit-isolate;
}

bdo {
    unicode-bidi: bidi-override;
}

textarea[dir=auto i] {
    unicode-bidi: -webkit-plaintext;
}

dialog:not([open]) {
    display: none
}

dialog {
    position: absolute;
    left: 0;
    right: 0;
    width: -webkit-fit-content;
    height: -webkit-fit-content;
    margin: auto;
    border: solid;
    padding: 1em;
    background: white;
    color: black
}

dialog::backdrop {
    position: fixed;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
    background: rgba(0,0,0,0.1)
}

/* page */

@page {
    /* FIXME: Define the right default values for page properties. */
    size: auto;
    margin: auto;
    padding: 0px;
    border-width: 0px;
}

/* noscript is handled internally, as it depends on settings. */

/* 
 * Additonal style sheet used to render HTML pages in quirks mode.
 *
 * Copyright (C) 2000-2003 Lars Knoll (knoll@kde.org)
 * Copyright (C) 2004, 2006, 2007 Apple Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 2 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 *
 */

/* Give floated images margins of 3px */
img[align="left" i] {
    margin-right: 3px;
}
img[align="right" i] {
    margin-left: 3px;
}

/* Tables reset both line-height and white-space in quirks mode. */
/* Compatible with WinIE. Note that font-family is *not* reset. */
table {
    white-space: normal;
    line-height: normal;
    font-weight: normal;
    font-size: medium;
    font-variant: normal;
    font-style: normal;
    color: -internal-quirk-inherit;
    text-align: start;
}

/* This will apply only to text fields, since all other inputs already use border box sizing */
input:not([type=image i]), textarea {
    box-sizing: border-box;
}

/* Set margin-bottom for form element in quirks mode. */
/* Compatible with Gecko. (Doing this only for quirks mode is a fix for bug 17696.) */
form {
    margin-bottom: 1em
}
/*
 * Copyright (C) 2006 Apple Computer, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

body {
    margin: 0
}

table {
    width: 100%;
    border-spacing: 0;
    white-space: pre-wrap !important;
    margin: 0;
    word-break: break-word;
    font-size: initial;
    font-family: monospace;
    tab-size: 4;
}

td {
    padding: 0 !important;
    vertical-align: baseline
}

.line-gutter-backdrop, .line-number {
    /* Keep this in sync with inspector.css (.webkit-line-gutter-backdrop) */
    box-sizing: border-box;
    padding: 0 4px !important;
    width: 31px;
    background-color: rgb(240, 240, 240);
    border-right: 1px solid rgb(187, 187, 187) !important;
    -webkit-user-select: none;
}

.line-gutter-backdrop {
    /* Keep this in sync with inspector.css (.webkit-line-gutter-backdrop) */
    position: absolute;
    z-index: -1;
    left: 0;
    top: 0;
    height: 100%
}

.line-number {
    text-align: right;
    color: rgb(128, 128, 128);
    word-break: normal;
    white-space: nowrap;
    font-size: 9px;
    font-family: Helvetica;
    -webkit-user-select: none;
}

.line-number::before {
    content: attr(value);
}

tbody:last-child .line-content:empty:before {
    content: " ";
}

.line-content {
    padding: 0 5px !important;
}

.highlight {
    background-color: rgb(100%, 42%, 42%);
    border: 2px solid rgb(100%, 31%, 31%);
}

.html-tag {
    /* Keep this in sync with inspector.css (.webkit-html-tag) */
    color: rgb(136, 18, 128);
}

.html-attribute-name {
    /* Keep this in sync with inspector.css (.webkit-html-attribute-name) */
    color: rgb(153, 69, 0);
}

.html-attribute-value {
    /* Keep this in sync with inspector.css (.webkit-html-attribute-value) */
    color: rgb(26, 26, 166);
}

.html-external-link, .html-resource-link {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-external-link, .webkit-html-resource-link) */
    color: #00e;
}

.html-external-link {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-external-link) */
    text-decoration: none;
}

.html-external-link:hover {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-external-link:hover) */
    text-decoration: underline;
}

.html-comment {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-comment) */
    color: rgb(35, 110, 37);
}

.html-doctype {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-doctype) */
    color: rgb(192, 192, 192);
}

.html-end-of-file {
    /* Keep this in sync with inspectorSyntaxHighlight.css (.webkit-html-end-of-file) */
    color: rgb(255, 0, 0);
    font-weight: bold;
}
/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

input[type="date" i],
input[type="datetime-local" i],
input[type="month" i],
input[type="time" i],
input[type="week" i] {
    align-items: center;
    display: -webkit-inline-flex;
    font-family: monospace;
    overflow: hidden;
    padding: 0;
    -webkit-padding-start: 1px;
}

input::-webkit-datetime-edit {
    flex: 1;
    min-width: 0;
    -webkit-user-modify: read-only !important;
    display: inline-block;
    overflow: hidden;
}

input::-webkit-datetime-edit-fields-wrapper {
    -webkit-user-modify: read-only !important;
    display: inline-block;
    padding: 1px 0;
    white-space: pre;
}

/* If you update padding, border, or margin in the following ruleset, update
   DateTimeFieldElement::maximumWidth too. */
input::-webkit-datetime-edit-ampm-field,
input::-webkit-datetime-edit-day-field,
input::-webkit-datetime-edit-hour-field,
input::-webkit-datetime-edit-millisecond-field,
input::-webkit-datetime-edit-minute-field,
input::-webkit-datetime-edit-month-field,
input::-webkit-datetime-edit-second-field,
input::-webkit-datetime-edit-week-field,
input::-webkit-datetime-edit-year-field {
    -webkit-user-modify: read-only !important;
    border: none;
    display: inline;
    font: inherit !important;
    padding: 1px;
}

/* Remove focus ring from fields and use highlight color */
input::-webkit-datetime-edit-ampm-field:focus,
input::-webkit-datetime-edit-day-field:focus,
input::-webkit-datetime-edit-hour-field:focus,
input::-webkit-datetime-edit-millisecond-field:focus,
input::-webkit-datetime-edit-minute-field:focus,
input::-webkit-datetime-edit-month-field:focus,
input::-webkit-datetime-edit-second-field:focus,
input::-webkit-datetime-edit-week-field:focus,
input::-webkit-datetime-edit-year-field:focus {
    background-color: highlight;
    color: highlighttext;
    outline: none;
}

input::-webkit-datetime-edit-year-field[disabled],
input::-webkit-datetime-edit-month-field[disabled],
input::-webkit-datetime-edit-week-field[disabled],
input::-webkit-datetime-edit-day-field[disabled],
input::-webkit-datetime-edit-ampm-field[disabled],
input::-webkit-datetime-edit-hour-field[disabled],
input::-webkit-datetime-edit-millisecond-field[disabled],
input::-webkit-datetime-edit-minute-field[disabled],
input::-webkit-datetime-edit-second-field[disabled] {
    color: GrayText;
}

/* If you update padding, border, or margin in the following ruleset, update
   DateTimeEditElement::customStyelForRenderer too. */
input::-webkit-datetime-edit-text {
    -webkit-user-modify: read-only !important;
    display: inline;
    font: inherit !important;
}

input[type="date" i]::-webkit-inner-spin-button,
input[type="datetime" i]::-webkit-inner-spin-button,
input[type="datetime-local" i]::-webkit-inner-spin-button,
input[type="month" i]::-webkit-inner-spin-button,
input[type="time" i]::-webkit-inner-spin-button,
input[type="week" i]::-webkit-inner-spin-button {
    /* FIXME: Remove height. */
    height: 1.5em;
    -webkit-margin-start: 2px;
}
/*
 * Copyright (C) 2008 Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* These styles override the default styling for HTML elements as defined in
   WebCore/css/html.css. So far we have used this file exclusively for
   making our form elements match Firefox's. */

input:not([type]), 
input[type="email" i],
input[type="number" i],
input[type="password" i],
input[type="tel" i],
input[type="url" i],
input[type="text" i] {
    padding:1px 0;
}

input[type="search" i] {
    padding:1px;
}

input[type="checkbox" i] {
    margin:3px 3px 3px 4px;
}

input[type="radio" i] {
    margin:3px 3px 0 5px;
}

input[type="range" i] {
    color: #c4c4c4;
}

/* Not sure this is the right color. #EBEBE4 is what Firefox uses.
   FIXME: Figure out how to support legacy input rendering. 
   FIXME: Add input[type="file" i] once we figure out our file inputs.
   FIXME: Add input[type="image" i] once we figure out our image inputs.
   FIXME: We probably do the wrong thing if you put an invalid input type.
          do we care?
*/
textarea:disabled,
input:not([type]):disabled, 
input[type="color" i]:disabled,
input[type="date" i]:disabled,
input[type="datetime" i]:disabled,
input[type="datetime-local" i]:disabled,
input[type="email" i]:disabled,
input[type="month" i]:disabled,
input[type="password" i]:disabled,
input[type="number" i]:disabled,
input[type="search" i]:disabled,
input[type="tel" i]:disabled,
input[type="text" i]:disabled,
input[type="time" i]:disabled,
input[type="url" i]:disabled,
input[type="week" i]:disabled {
    background-color: #EBEBE4; 
}

input[type="search" i]::-webkit-search-cancel-button {
    margin-right: 3px;
}

input[type="search" i]::-webkit-search-results-decoration {
    margin: auto 3px auto 2px;
}

input[type="button" i], input[type="submit" i], input[type="reset" i], input[type="file" i]::-webkit-file-upload-button, button {
    padding: 1px 6px;
}

/* Windows selects are not rounded. Custom borders for them shouldn't be either. */
keygen, 
select, 
select[size="0"],
select[size="1"] {
    border-radius: 0;
    /* Same as native_theme_base. */
    border-color: #a9a9a9;
}

select[size],
select[multiple],
select[size][multiple] {
    /* Same as native_theme_base. */
    border: 1px solid #a9a9a9;
}

textarea {
    font-family: monospace;
    /* Same as native_theme_base. */
    border-color: #a9a9a9;
}
/*
 * Copyright (C) 2008 Google Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* These styles override the default styling for HTML elements in quirks-mode
   as defined in WebCore/css/quirks.css. So far we have used this file exclusively for
   making our form elements match Firefox's. */

textarea {
  /* Matches IE's text offsets in quirksmode (causes text to wrap the same as IE). */
  padding: 2px 0 0 2px;
}
/*
 * The default style sheet used to render SVG.
 *
 * Copyright (C) 2005, 2006 Apple Computer, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */

@namespace "http://www.w3.org/2000/svg";
@namespace html "http://www.w3.org/1999/xhtml";

/*
    When an outermost SVG 'svg' element is stand-alone or embedded inline within a parent XML grammar 
    which does not use CSS layout [CSS2-LAYOUT] or XSL formatting [XSL], the 'overflow' property on the 
    outermost 'svg' element is ignored for the purposes of visual rendering and the initial clipping path is set 
    to the bounds of the initial viewport.

    When an outermost 'svg' element is embedded inline within a parent XML grammar which uses CSS layout
    [CSS2-LAYOUT] or XSL formatting [XSL], if the 'overflow' property has the value hidden or scroll, then
    the user agent will establish an initial clipping path equal to the bounds of the initial viewport; otherwise,
    the initial clipping path is set according to the clipping rules as defined in [CSS2-overflow].

    Opera/Firefox & WebKit agreed on NOT setting "overflow: hidden" for the outermost svg element - SVG 1.1 Errata
    contains these changes as well as all future SVG specifications: see http://lists.w3.org/Archives/Public/public-svg-wg/2008JulSep/0347.html
*/

svg:not(:root), symbol, image, marker, pattern, foreignObject {
    overflow: hidden
}

svg:root {
    width: 100%;
    height: 100%;
}

text, foreignObject {
    display: block
}

text {
   white-space: nowrap
}

tspan, textPath {
   white-space: inherit
}

/* states */

:focus {
    outline: auto 5px -webkit-focus-ring-color
}

/* CSS transform specification: "transform-origin 0 0 for SVG elements without associated CSS layout box, 50% 50% for all other elements". */
 
* {
    -webkit-transform-origin: 0 0;
}

html|* > svg {
    -webkit-transform-origin: 50% 50%;
}
/*
 * The default style sheet used to render MathML.
 *
 * Copyright (C) 2014 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

@namespace "http://www.w3.org/1998/Math/MathML";

/* By default, we only display the MathML formulas without any formatting other than the one specified by the display attribute. */
math {
    display: inline;
}

math[display="block"] {
    display: block;
    text-align: center;
}

/* We hide the PresentationExpression constructions that are children of a <semantics> element.
   http://www.w3.org/TR/MathML/appendixa.html#parsing_PresentationExpression */
semantics > mi, semantics > mn, semantics > mo, semantics > mtext, semantics > mspace, semantics > ms, semantics > maligngroup, semantics > malignmark, semantics > mrow, semantics > mfrac, semantics > msqrt, semantics > mroot, semantics > mstyle, semantics > merror, semantics > mpadded, semantics > mphantom, semantics > mfenced, semantics > menclose, semantics > msub, semantics > msup, semantics > msubsup, semantics > munder, semantics > mover, semantics > munderover, semantics > mmultiscripts, semantics > mtable, semantics > mstack, semantics > mlongdiv, semantics > maction {
    display: none;
}

/* However, we display all the annotations. */
annotation, annotation-xml {
    display: inline-block;
}
/*
 * Copyright (C) 2009 Apple Inc.  All rights reserved.
 * Copyright (C) 2009 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* Chromium default media controls */

/* WARNING: This css file can only style <audio> and <video> elements */

audio:not([controls]) {
    display: none !important;
}

audio {
    width: 300px;
    height: 30px;
}

audio::-webkit-media-controls,
video::-webkit-media-controls {
    width: inherit;
    height: inherit;
    position: relative;
    direction: ltr;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
}

audio::-webkit-media-controls-enclosure, video::-webkit-media-controls-enclosure {
    width: 100%;
    max-width: 800px;
    height: 30px;
    flex-shrink: 0;
    bottom: 0;
    text-indent: 0;
    padding: 0;
    box-sizing: border-box;
}

video::-webkit-media-controls-enclosure {
    padding: 0px 5px 5px 5px;
    height: 35px;
    flex-shrink: 0;
}

audio::-webkit-media-controls-panel, video::-webkit-media-controls-panel {
    display: flex;
    flex-direction: row;
    align-items: center;
    /* We use flex-start here to ensure that the play button is visible even
     * if we are too small to show all controls.
     */
    justify-content: flex-start;
    -webkit-user-select: none;
    position: relative;
    width: 100%;
    z-index: 0;
    overflow: hidden;
    text-align: right;
    bottom: auto;
    height: 30px;
    background-color: rgba(20, 20, 20, 0.8);
    border-radius: 5px;
    /* The duration is also specified in MediaControlElements.cpp and LayoutTests/media/media-controls.js */
    transition: opacity 0.3s;
}

audio:-webkit-full-page-media, video:-webkit-full-page-media {
    max-height: 100%;
    max-width: 100%;
}

audio:-webkit-full-page-media::-webkit-media-controls-panel,
video:-webkit-full-page-media::-webkit-media-controls-panel {
    bottom: 0px;
}

audio::-webkit-media-controls-mute-button, video::-webkit-media-controls-mute-button {
    -webkit-appearance: media-mute-button;
    display: flex;
    flex: none;
    border: none;
    box-sizing: border-box;
    width: 35px;
    height: 30px;
    line-height: 30px;
    margin: 0 6px 0 0;
    padding: 0;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-overlay-enclosure {
    display: none;
}

video::-webkit-media-controls-overlay-enclosure {
    display: flex;
    position: relative;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
    flex: 1 1;
    min-height: 0;
    width: 100%;
    text-indent: 0;
    box-sizing: border-box;
    overflow: hidden;
}

video::-webkit-media-controls-overlay-play-button {
    -webkit-appearance: media-overlay-play-button;
    display: flex;
    position: absolute;
    top: 50%;
    left: 50%;
    margin-left: -40px;
    margin-top: -40px;
    border: none;
    box-sizing: border-box;
    background-color: transparent;
    width: 80px;
    height: 80px;
    padding: 0;
}

video::-internal-media-controls-overlay-cast-button {
    -webkit-appearance: -internal-media-overlay-cast-off-button;
    display: flex;
    position: absolute;
    top: 5%;
    left: 5%;
    margin-left: 0px;
    margin-top: 0px;
    border: none;
    box-sizing: border-box;
    background-color: transparent;
    width: 30px;
    height: 30px;
    padding: 0;
}

audio::-webkit-media-controls-play-button, video::-webkit-media-controls-play-button {
    -webkit-appearance: media-play-button;
    display: flex;
    flex: none;
    border: none;
    box-sizing: border-box;
    width: 30px;
    height: 30px;
    line-height: 30px;
    margin-left: 9px;
    margin-right: 9px;
    padding: 0;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-timeline-container, video::-webkit-media-controls-timeline-container {
    -webkit-appearance: media-controls-background;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-end;
    flex: 1 1;
    -webkit-user-select: none;
    height: 16px;
    min-width: 0;
}

audio::-webkit-media-controls-current-time-display, video::-webkit-media-controls-current-time-display,
audio::-webkit-media-controls-time-remaining-display, video::-webkit-media-controls-time-remaining-display {
    -webkit-appearance: media-current-time-display;
    -webkit-user-select: none;
    flex: none;
    display: flex;
    border: none;
    cursor: default;

    height: 30px;
    margin: 0 9px 0 0;
    padding: 0;

    line-height: 30px;
    font-family: Arial, Helvetica, sans-serif;
    font-size: 13px;
    font-weight: bold;
    font-style: normal;
    color: white;

    letter-spacing: normal;
    word-spacing: normal;
    text-transform: none;
    text-indent: 0;
    text-shadow: none;
    text-decoration: none;
}

audio::-webkit-media-controls-timeline, video::-webkit-media-controls-timeline {
    -webkit-appearance: media-slider;
    display: flex;
    flex: 1 1 auto;
    height: 8px;
    margin: 0 15px 0 0;
    padding: 0;
    background-color: transparent;
    min-width: 25px;
    border: initial;
    color: inherit;
}

audio::-webkit-media-controls-volume-slider, video::-webkit-media-controls-volume-slider {
    -webkit-appearance: media-volume-slider;
    display: flex;
    /* The 1.9 value was empirically chosen to match old-flexbox behaviour
     * and be aesthetically pleasing.
     */
    flex: 1 1.9 auto;
    height: 8px;
    max-width: 70px;
    margin: 0 15px 0 0;
    padding: 0;
    background-color: transparent;
    min-width: 15px;
    border: initial;
    color: inherit;
}

/* FIXME these shouldn't use special pseudoShadowIds, but nicer rules.
   https://code.google.com/p/chromium/issues/detail?id=112508
   https://bugs.webkit.org/show_bug.cgi?id=62218
*/
input[type="range" i]::-webkit-media-slider-container {
    display: flex;
    align-items: center;
    flex-direction: row; /* This property is updated by C++ code. */
    box-sizing: border-box;
    height: 100%;
    width: 100%;
    border: 1px solid rgba(230, 230, 230, 0.35);
    border-radius: 4px;
    background-color: transparent; /* Background drawing is managed by C++ code to draw ranges. */
}

/* The negative right margin causes the track to overflow its container. */
input[type="range" i]::-webkit-media-slider-container > div {
    margin-right: -14px;
}

input[type="range" i]::-webkit-media-slider-thumb {
    margin-left: -7px;
    margin-right: -7px;
}

audio::-webkit-media-controls-fullscreen-button, video::-webkit-media-controls-fullscreen-button {
    -webkit-appearance: media-enter-fullscreen-button;
    display: flex;
    flex: none;
    border: none;
    box-sizing: border-box;
    width: 30px;
    height: 30px;
    line-height: 30px;
    margin-left: -5px;
    margin-right: 9px;
    padding: 0;
    background-color: initial;
    color: inherit;
}

audio::-internal-media-controls-cast-button, video::-internal-media-controls-cast-button {
    -webkit-appearance: -internal-media-cast-off-button;
    display: flex;
    flex: none;
    border: none;
    box-sizing: border-box;
    width: 30px;
    height: 30px;
    line-height: 30px;
    margin-left: -5px;
    margin-right: 9px;
    padding: 0;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-toggle-closed-captions-button {
    display: none;
}

video::-webkit-media-controls-toggle-closed-captions-button {
    -webkit-appearance: media-toggle-closed-captions-button;
    display: flex;
    flex: none;
    border: none;
    box-sizing: border-box;
    width: 30px;
    height: 30px;
    line-height: 30px;
    margin-left: -5px;
    margin-right: 9px;
    padding: 0;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-fullscreen-volume-slider, video::-webkit-media-controls-fullscreen-volume-slider {
    display: none;
}

audio::-webkit-media-controls-fullscreen-volume-min-button, video::-webkit-media-controls-fullscreen-volume-min-button {
    display: none;
}

audio::-webkit-media-controls-fullscreen-volume-max-button, video::-webkit-media-controls-fullscreen-volume-max-button {
    display: none;
}

video::-webkit-media-text-track-container {
    position: relative;
    width: inherit;
    height: inherit;
    overflow: hidden;

    font: 22px sans-serif;
    text-align: center;
    color: rgba(255, 255, 255, 1);

    letter-spacing: normal;
    word-spacing: normal;
    text-transform: none;
    text-indent: 0;
    text-decoration: none;
    pointer-events: none;
    -webkit-user-select: none;
    word-break: break-word;
}

video::cue {
    display: inline;

    background-color: rgba(0, 0, 0, 0.8);
    padding: 2px 2px;
}

video::-webkit-media-text-track-region {
    position: absolute;
    line-height: 5.33vh;
    writing-mode: horizontal-tb;
    background: rgba(0, 0, 0, 0.8);
    color: rgba(255, 255, 255, 1);
    word-wrap: break-word;
    overflow-wrap: break-word;
    overflow: hidden;
}

video::-webkit-media-text-track-region-container {
    position: relative;

    display: flex;
    flex-flow: column;
    flex-direction: column;
}

video::-webkit-media-text-track-region-container.scrolling {
    transition: top 433ms linear;
}


video::-webkit-media-text-track-display {
    position: absolute;
    overflow: hidden;
    white-space: pre-wrap;
    -webkit-box-sizing: border-box;
    flex: 0 0 auto;
}

video::cue(:future) {
    color: gray;
}

video::cue(b) {
    font-weight: bold;
}

video::cue(u) {
    text-decoration: underline;
}

video::cue(i) {
    font-style: italic;
}
/*
 * Copyright (C) 2009 Apple Inc.  All rights reserved.
 * Copyright (C) 2015 Google Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE COMPUTER, INC. ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL APPLE COMPUTER, INC. OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* Chromium default media controls */

/* WARNING: This css file can only style <audio> and <video> elements */

audio:not([controls]) {
    display: none !important;
}

audio {
    width: 300px;
    height: 48px;
}

audio::-webkit-media-controls,
video::-webkit-media-controls {
    width: inherit;
    height: inherit;
    position: relative;
    direction: ltr;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
}

audio::-webkit-media-controls-enclosure, video::-webkit-media-controls-enclosure {
    width: 100%;
    height: 48px;
    flex-shrink: 0;
    bottom: 0;
    text-indent: 0;
    padding: 0;
    margin: 0;
    box-sizing: border-box;
}

audio::-webkit-media-controls-panel, video::-webkit-media-controls-panel {
    display: flex;
    flex-direction: row;
    align-items: center;
    /* We use flex-start here to ensure that the play button is visible even
     * if we are too small to show all controls.
     */
    justify-content: flex-start;
    -webkit-user-select: none;
    position: relative;
    width: 100%;
    z-index: 0;
    overflow: hidden;
    text-align: right;
    bottom: auto;
    height: 48px;
    min-width: 48px;
    background-color: #fafafa;
    /* The duration is also specified in MediaControlElements.cpp and LayoutTests/media/media-controls.js */
    transition: opacity 0.3s;

    font-family: Segoe, "Helvetica Neue", Roboto, Arial, Helvetica, sans-serif ;
    font-size: 14px;
    font-weight: normal;  /* Make sure that we don't inherit non-defaults. */
    font-style: normal;
}

audio:-webkit-full-page-media, video:-webkit-full-page-media {
    max-height: 100%;
    max-width: 100%;
}

audio:-webkit-full-page-media::-webkit-media-controls-panel,
video:-webkit-full-page-media::-webkit-media-controls-panel {
    bottom: 0px;
}

audio::-webkit-media-controls-mute-button, video::-webkit-media-controls-mute-button {
    -webkit-appearance: media-mute-button;
    display: flex;
    flex: none;
    border: none;
    width: 48px;
    height: 48px;
    line-height: 48px;
    padding: 12px;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-overlay-enclosure {
    display: none;
}

video::-webkit-media-controls-overlay-enclosure {
    display: flex;
    position: relative;
    flex-direction: column;
    justify-content: flex-end;
    align-items: center;
    flex: 1 1;
    min-height: 0;
    width: 100%;
    /* prevent disambiguation zooms with the panel */
    margin-bottom: 10px;
    text-indent: 0;
    box-sizing: border-box;
    overflow: hidden;
}

video::-webkit-media-controls-overlay-play-button {
    -webkit-appearance: media-overlay-play-button;
    display: flex;
    position: absolute;
    top: 0;
    left: 0;
    margin: 0;
    border: none;
    background-color: transparent;
    width: 100%;
    height: 100%;
    padding: 0;
}

video::-internal-media-controls-overlay-cast-button {
    -webkit-appearance: -internal-media-overlay-cast-off-button;
    display: flex;
    position: absolute;
    top: 8px;
    left: 8px;
    margin-left: 0px;
    margin-top: 0px;
    border: none;
    background-color: transparent;
    width: 48px;
    height: 48px;
    padding: 0;
    transition: opacity 0.3s;
}

audio::-webkit-media-controls-play-button, video::-webkit-media-controls-play-button {
    -webkit-appearance: media-play-button;
    display: flex;
    flex: none;
    border-sizing: border-box;
    width: 48px;
    height: 48px;
    line-height: 48px;
    padding: 12px;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-timeline-container, video::-webkit-media-controls-timeline-container {
    -webkit-appearance: media-controls-background;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: flex-end;
    flex: 1 1;
    -webkit-user-select: none;
    height: 48px;
    padding: 0;
    min-width: 0;
}

audio::-webkit-media-controls-current-time-display, video::-webkit-media-controls-current-time-display {
    -webkit-appearance: media-current-time-display;
    -webkit-user-select: none;
    flex: none;
    display: flex;
    border: none;
    cursor: default;

    height: 48px;

    /* text runs right to the edge of the container */
    padding: 0;

    line-height: 48px;
    color: #5a5a5a;

    letter-spacing: normal;
    word-spacing: normal;
    text-transform: none;
    text-indent: 0;
    text-shadow: none;
    text-decoration: none;
}

audio::-webkit-media-controls-time-remaining-display, video::-webkit-media-controls-time-remaining-display {
    -webkit-appearance: media-current-time-display;
    -webkit-user-select: none;
    flex: none;
    display: flex;
    border: none;
    cursor: default;

    height: 48px;

    /* text runs right to the edge of the container, plus a little on
     * the left to pad the leading "/" */
    padding: 0 0 0 4px;

    line-height: 48px;
    color: #5a5a5a;

    letter-spacing: normal;
    word-spacing: normal;
    text-transform: none;
    text-indent: 0;
    text-shadow: none;
    text-decoration: none;
}

audio::-webkit-media-controls-timeline, video::-webkit-media-controls-timeline {
    -webkit-appearance: media-slider;
    display: flex;
    flex: 1 1 auto;
    height: 2px;

    /* Leave 6px on either side for the thumb.  Use margin so that
     * the slider doesn't extend into it.  We also add 12px border.
     */
    padding: 0;
    margin: 0 18px 0 18px;
    background-color: transparent;
    min-width: 25px;
    border: initial;
    color: inherit;
}

audio::-webkit-media-controls-volume-slider, video::-webkit-media-controls-volume-slider {
    -webkit-appearance: media-volume-slider;
    display: flex;
    /* The 1.9 value was empirically chosen to match old-flexbox behaviour
     * and be aesthetically pleasing.
     */
    flex: 1 1.9 auto;
    height: 2px;
    max-width: 70px;
    /* Leave room for the thumb, which has 6px radius.  Use margin rather
     * than padding so that the slider doesn't extend into it.  We also
     * leave an addition 12px margin.
     */
    padding: 0;
    margin: 0 18px 0 18px;
    background-color: transparent;
    min-width: 25px;
    border: initial;
    color: inherit;
}

/* FIXME these shouldn't use special pseudoShadowIds, but nicer rules.
   https://code.google.com/p/chromium/issues/detail?id=112508
   https://bugs.webkit.org/show_bug.cgi?id=62218
*/
input[type="range" i]::-webkit-media-slider-container {
    display: flex;
    align-items: center;
    flex-direction: row; /* This property is updated by C++ code. */
    box-sizing: border-box;
    /** this positions the slider thumb for both time and volume. */
    height: 2px;
    width: 100%;
    background-color: transparent; /* Background drawing is managed by C++ code to draw ranges. */
}

/* The negative right margin causes the track to overflow its container. */
input[type="range" i]::-webkit-media-slider-container > div {
    margin-right: -18px;  /* box is 36px wide, get to the middle */
    margin-left:  -18px;
}

input[type="range" i]::-webkit-media-slider-thumb {
    box-sizing: border-box;
    width: 48px;
    height: 48px;
    padding: 0px;
}

audio::-webkit-media-controls-fullscreen-button, video::-webkit-media-controls-fullscreen-button {
    -webkit-appearance: media-enter-fullscreen-button;
    display: flex;
    flex: none;
    overflow: hidden;
    border: none;
    width: 48px;
    height: 48px;
    line-height: 48px;
    background-color: initial;
    color: inherit;
}

audio::-internal-media-controls-cast-button, video::-internal-media-controls-cast-button {
    -webkit-appearance: -internal-media-cast-off-button;
    display: flex;
    flex: none;
    border: none;
    width: 48px;
    height: 48px;
    line-height: 48px;
    margin-left: 0px;
    margin-right: 0px;
    padding: 12px;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-toggle-closed-captions-button {
    display: none;
}

video::-webkit-media-controls-toggle-closed-captions-button {
    -webkit-appearance: media-toggle-closed-captions-button;
    display: flex;
    flex: none;
    border: none;
    width: 48px;
    height: 48px;
    line-height: 48px;
    margin-left: 0px;
    margin-right: 0px;
    padding: 12px;
    background-color: initial;
    color: inherit;
}

audio::-webkit-media-controls-fullscreen-volume-slider, video::-webkit-media-controls-fullscreen-volume-slider {
    display: none;
}

audio::-webkit-media-controls-fullscreen-volume-min-button, video::-webkit-media-controls-fullscreen-volume-min-button {
    display: none;
}

audio::-webkit-media-controls-fullscreen-volume-max-button, video::-webkit-media-controls-fullscreen-volume-max-button {
    display: none;
}

video::-webkit-media-text-track-container {
    position: relative;
    width: inherit;
    height: inherit;
    overflow: hidden;

    font: 22px sans-serif;
    text-align: center;
    color: rgba(255, 255, 255, 1);

    letter-spacing: normal;
    word-spacing: normal;
    text-transform: none;
    text-indent: 0;
    text-decoration: none;
    pointer-events: none;
    -webkit-user-select: none;
    word-break: break-word;
}

video::cue {
    display: inline;

    background-color: rgba(0, 0, 0, 0.8);
    padding: 2px 2px;
}

video::-webkit-media-text-track-region {
    position: absolute;
    line-height: 5.33vh;
    writing-mode: horizontal-tb;
    background: rgba(0, 0, 0, 0.8);
    color: rgba(255, 255, 255, 1);
    word-wrap: break-word;
    overflow-wrap: break-word;
    overflow: hidden;
}

video::-webkit-media-text-track-region-container {
    position: relative;

    display: flex;
    flex-flow: column;
    flex-direction: column;
}

video::-webkit-media-text-track-region-container.scrolling {
    transition: top 433ms linear;
}


video::-webkit-media-text-track-display {
    position: absolute;
    overflow: hidden;
    white-space: pre-wrap;
    -webkit-box-sizing: border-box;
    flex: 0 0 auto;
}

video::cue(:future) {
    color: gray;
}

video::cue(b) {
    font-weight: bold;
}

video::cue(u) {
    text-decoration: underline;
}

video::cue(i) {
    font-style: italic;
}
:-webkit-full-screen {
    background-color: white;
    z-index: 2147483647 !important;
}

:root:-webkit-full-screen-ancestor {
    overflow: hidden !important;
}

:-webkit-full-screen-ancestor:not(iframe) {
    z-index: auto !important;
    position: static !important;
    opacity: 1 !important;
    transform: none !important;
    -webkit-mask: none !important;
    clip: none !important;
    -webkit-filter: none !important;
    transition: none !important;
    -webkit-box-reflect: none !important;
    -webkit-perspective: none !important;
    -webkit-transform-style: flat !important;
    will-change: none !important;
}

video:-webkit-full-screen, audio:-webkit-full-screen {
    background-color: transparent !important;
    position: relative !important;
    left: 0 !important;
    top: 0 ! important;
    margin: 0 !important;
    min-width: 0 !important;
    max-width: none !important;
    min-height: 0 !important;
    max-height: none !important;
    width: 100% !important;
    height: 100% !important;
    flex: 1 !important;
    display: block !important;
    transform: none !important;
}

img:-webkit-full-screen {
    width: auto;
    height: 100%;
    max-width: 100%;
}

iframe:-webkit-full-screen {
    margin: 0 !important;
    padding: 0 !important;
    border: 0 !important;
    position: fixed !important;
    min-width: 0 !important;
    max-width: none !important;
    min-height: 0 !important;
    max-height: none !important;
    width: 100% !important;
    height: 100% !important;
    left: 0 !important;
    top: 0 !important;
}
/*
 * Copyright (c) 2013, Opera Software ASA. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Opera Software ASA nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* Default styles for XHTML Mobile Profile documents where they differ from html.css */

@viewport {
    width: auto;
    /* Ideally these should be removed. Currently here to avoid test result regressions. */
    min-zoom: 0.25;
    max-zoom: 5;
}
/*
 * Copyright (C) 2013 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/* These styles override other user-agent styles for Chromium on Android. */

@viewport {
    min-width: 980px;
}
<!--
 Copyright (C) 2012 Google Inc. All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 1.  Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
 2.  Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
 3.  Neither the name of Apple Computer, Inc. ("Apple") nor the names of
     its contributors may be used to endorse or promote products derived
     from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY APPLE AND ITS CONTRIBUTORS "AS IS" AND ANY
 EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL APPLE OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
-->
<!DOCTYPE html>
<html>
<head>
<style>
body {
    margin: 0;
    padding: 0;
}

body.platform-mac {
    font-size: 11px;
    font-family: Menlo, Monaco;
}

body.platform-windows {
    font-size: 12px;
    font-family: Consolas, Lucida Console;
}

body.platform-linux {
    font-size: 11px;
    font-family: dejavu sans mono;
}

.fill {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
}

.dimmed {
    background-color: rgba(0, 0, 0, 0.31);
}

#canvas, #layout-editor-matched-nodes-canvas {
    pointer-events: none;
}

.controls-line {
    display: flex;
    justify-content: center;
    margin: 10px 0;
}

.message-box {
    padding: 2px 4px;
    display: flex;
    align-items: center;
    cursor: default;
}

.controls-line > * {
    background-color: rgb(255, 255, 194);
    border: 1px solid rgb(202, 202, 202);
    height: 22px;
    box-sizing: border-box;
}

.controls-line .button {
    width: 26px;
    margin-left: -1px;
    margin-right: 0;
    padding: 0;
}

.controls-line .button:hover {
    cursor: pointer;
}

.controls-line .button .glyph {
    width: 100%;
    height: 100%;
    background-color: rgba(0, 0, 0, 0.75);
    opacity: 0.8;
    -webkit-mask-repeat: no-repeat;
    -webkit-mask-position: center;
    position: relative;
}

.controls-line .button:active .glyph {
    top: 1px;
    left: 1px;
}

#resume-button .glyph {
    -webkit-mask-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA0AAAAKCAYAAABv7tTEAAAAAXNSR0IArs4c6QAAAFJJREFUKM+10bEJgGAMBeEPbR3BLRzEVdzEVRzELRzBVohVwEJ+iODBlQfhBeJhsmHU4C0KnFjQV6J0x1SNAhdWDJUoPTB3PvLLeaUhypM3n3sD/qc7lDrdpIEAAAAASUVORK5CYII=);
    -webkit-mask-size: 13px 10px;
    background-color: rgb(66, 129, 235);
}

#step-over-button .glyph {
    -webkit-mask-image: url(data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABIAAAAKCAYAAAC5Sw6hAAAAAXNSR0IArs4c6QAAAOFJREFUKM+N0j8rhXEUB/DPcxW35CqhvIBrtqibkklhV8qkTHe4ZbdblcXgPVhuMdqUTUl5A2KRRCF5LGc4PT1P7qnfcr5/zu/8KdTHLFaxjHnc4RZXKI0QYxjgLQTVd42l/0wmg5iFX3iq5H6w22RS4DyRH7CB8cAXcBTGJT6xUmd0mEwuMdFQcA3fwXvGTAan8BrgPabTL9fRRyfx91PRMwyjGwcJ2EyCfsrfpPw2Pipz24NT/MZciiQYVshzOKnZ5Hturxt3k2MnCpS4SPkeHpPR8Sh3tYgttBoW9II2/AHiaEqvD2Fc0wAAAABJRU5ErkJggg==);
    -webkit-mask-size: 18px 10px;
}

.px {
    color: rgb(128, 128, 128);
}

#element-title {
    position: absolute;
    z-index: 10;
}

#tag-name {
    /* Keep this in sync with view-source.css (.html-tag) */
    color: rgb(136, 18, 128);
}

#node-id {
    /* Keep this in sync with view-source.css (.html-attribute-value) */
    color: rgb(26, 26, 166);
}

#class-name {
    /* Keep this in sync with view-source.css (.html-attribute-name) */
    color: rgb(153, 69, 0);
}

.wall {
    position: absolute;
    z-index: -2;
    opacity: 0.2;
    pointer-events: none;
}

.wall.horizontal {
    width: 8px;
    height: 16px;
}

.wall.vertical {
    height: 8px;
    width: 16px;
}

.wall.padding {
    background: repeating-linear-gradient(45deg, #FFFFFF 2px, #FFFFFF 4px, rgba(147, 196, 125, 1) 2px, rgba(147, 196, 125, 1) 10px)

}

.wall.margin {
    background: repeating-linear-gradient(45deg, #FFFFFF 2px, #FFFFFF 4px, rgba(246, 178, 107, 1) 4px, rgba(246, 178, 107, 1) 10px)
}

.wall.highlighted {
    z-index: 1;
    opacity: 1;
}

.blur-rect {
    position: absolute;
    background-color: rgba(0, 0, 0, 0.1);
}

.control-lane {
    position: absolute;
}

.control-lane.padding {
    background-color: rgba(147, 196, 125, 0.1);
}

.control-lane.margin {
    background-color: rgba(246, 178, 107, 0.1);
}

.editor-anchor {
    position: absolute;
    -webkit-filter: drop-shadow(0px 1px 1px rgba(0, 0, 0, 0.34));
}

.editor-anchor.vertical {
    height: 6px;
    width: 16px;
}

.editor-anchor.horizontal {
    width: 6px;
    height: 16px;
}

.editor-anchor.horizontal::before {
    content: "";
    position: absolute;
    height: 8px;
    border-right: 2px dotted rgba(255, 255, 255, 0.4);
    top: 4px;
    left: 2px;
}

.editor-anchor.vertical::before {
    content: "";
    position: absolute;
    width: 8px;
    border-bottom: 2px dotted rgba(255, 255, 255, 0.4);
    top: 2px;
    left: 4px;
}

.editor-anchor.vertical:hover {
    cursor: ns-resize;
}

.editor-anchor.horizontal:hover {
    cursor: ew-resize;
}

.editor-anchor.padding {
    background-color: rgb(107, 213, 0);
}

.editor-anchor.margin {
    background-color: rgb(246, 167, 35);
}

.editor-anchor:hover {
    z-index: 3;
}

.editor-anchor.padding.highlighted {
    background-color: rgba(147, 196, 125, 1);
}

.editor-anchor.margin.highlighted {
    background-color: rgba(246, 178, 107, 1);
}

.guide-line.horizontal {
    border-top: dashed 1px;
}

.guide-line.vertical {
    border-left: dashed 1px;
}

.guide-line.padding {
    border-color: rgba(147, 196, 125, 0.56);
}

.guide-line.margin {
    border-color: rgba(246, 178, 107, 0.56);
}

.guide-line.content {
    border-color: rgba(147, 147, 147, 0.56)
}

.guide-line {
    position: absolute;
    pointer-events: none;
}

.label {
    position: absolute;
    font-size: 10px;
    font-family: Arial, Roboto;
    color: white;
    line-height: 1em;
    padding: 2px 5px;
    -webkit-filter: drop-shadow(0px 1px 1px rgba(0, 0, 0, 0.34));
    border-radius: 2px;
    min-width: 30px;
    z-index: 5;
}

.label.padding {
    background-color: rgb(91, 181, 0);
}

.label.margin {
    background-color: rgb(246, 167, 35);
}

.label.disabled {
    background-color: rgb(159, 188, 191);
}

.label .dimension {
    color: rgba(255, 255, 255, 0.7);
}

.label .name {
    color: rgba(255, 255, 255, 0.7);
    display: none;
    border-radius: 4px;
}

.label.full .name {
    display: inline;
    z-index: 5;
}

.label::before {
    content: '';
    display: block;
    position: absolute;
    width: 0;
    height: 0;
    border-top: 6px solid transparent;
    border-bottom: 6px solid transparent;
    top: 1px;
}

.label.right-arrow::before {
    border-left-width: 6px;
    border-left-style: solid;
    border-right: none;
    left: auto;
    right: -6px;
}

.label.left-arrow::before {
    border-left: none;
    border-right-width: 6px;
    border-right-style: solid;
    left: -6px;
    right: auto;
}

.label.padding::before {
    border-left-color: rgb(91, 181, 0);
    border-right-color: rgb(91, 181, 0);
}

.label.margin::before {
    border-left-color: rgb(246, 167, 35);
    border-right-color: rgb(246, 167, 35);
}

/* Material */
.hidden {
    display: none !important;
}

.tooltip-content,
.material-tooltip-arrow {
    position: absolute;
    z-index: 10;
    -webkit-user-select: none;
}

.tooltip-content {
    background-color: #333740;
    font-size: 11px;
    line-height: 14px;
    padding: 5px 8px;
    border-radius: 3px;
    color: white;
    box-sizing: border-box;
    max-width: calc(100% - 4px);
    border: 1px solid hsla(0, 0%, 100%, 0.3);
    z-index: 1;
    background-clip: padding-box;
    will-change: transform;
    text-rendering: optimizeLegibility;
    pointer-events: none;
}

.element-info {
    display: flex;
    align-content: stretch;
}

.material-tooltip-arrow {
    border: solid;
    border-color: #333740 transparent;
    border-width: 0 8px 8px 8px;
    z-index: 2;
    margin-top: 1px;
}

.material-tooltip-arrow.tooltip-arrow-top {
    border-width: 8px 8px 0 8px;
    margin-top: -1px;
}

.element-description {
    flex: 1 1;
    word-wrap: break-word;
    word-break: break-all;
}

.dimensions {
    border-left: 1px solid hsl(0, 0%, 50%);
    padding-left: 7px;
    margin-left: 7px;
    float: right;
    flex: 0 0 auto;
    white-space: nowrap;
    display: flex;
    align-items: center;
    color: hsl(0, 0%, 85%);
}

.material-node-width {
    margin-right: 2px;
}

.material-node-height {
    margin-left: 2px;
}

.material-tag-name {
    color: hsl(304, 77%, 70%);
}

.material-node-id {
    color: hsl(27, 100%, 70%);
}

.material-class-name {
    color: hsl(202,92%,77%);
}

.layout-editor-media-tooltip {
    color: hsl(0, 0%, 85%);
}

.layout-editor-selector-tooltip {
    color: hsl(202,92%,77%);
}

</style>
<script>
const lightGridColor = "rgba(0,0,0,0.2)";
const darkGridColor = "rgba(0,0,0,0.7)";
const transparentColor = "rgba(0, 0, 0, 0)";
const gridBackgroundColor = "rgba(255, 255, 255, 0.8)";

function drawPausedInDebuggerMessage(message)
{
    window._controlsVisible = true;
    document.querySelector(".controls-line").style.visibility = "visible";
    document.getElementById("paused-in-debugger").textContent = message;
    document.body.classList.add("dimmed");
}

function _drawGrid(context, rulerAtRight, rulerAtBottom)
{
    if (window._gridPainted)
        return;
    window._gridPainted = true;

    context.save();

    var pageFactor = pageZoomFactor * pageScaleFactor;
    var scrollX = window.scrollX * pageScaleFactor;
    var scrollY = window.scrollY * pageScaleFactor;
    function zoom(x)
    {
        return Math.round(x * pageFactor);
    }
    function unzoom(x)
    {
        return Math.round(x / pageFactor);
    }

    var width = canvasWidth / pageFactor;
    var height = canvasHeight / pageFactor;

    const gridSubStep = 5;
    const gridStep = 50;

    {
        // Draw X grid background
        context.save();
        context.fillStyle = gridBackgroundColor;
        if (rulerAtBottom)
            context.fillRect(0, zoom(height) - 15, zoom(width), zoom(height));
        else
            context.fillRect(0, 0, zoom(width), 15);

        // Clip out backgrounds intersection
        context.globalCompositeOperation = "destination-out";
        context.fillStyle = "red";
        if (rulerAtRight)
            context.fillRect(zoom(width) - 15, 0, zoom(width), zoom(height));
        else
            context.fillRect(0, 0, 15, zoom(height));
        context.restore();

        // Draw Y grid background
        context.fillStyle = gridBackgroundColor;
        if (rulerAtRight)
            context.fillRect(zoom(width) - 15, 0, zoom(width), zoom(height));
        else
            context.fillRect(0, 0, 15, zoom(height));
    }

    context.lineWidth = 1;
    context.strokeStyle = darkGridColor;
    context.fillStyle = darkGridColor;
    {
        // Draw labels.
        context.save();
        context.translate(-scrollX, 0.5 - scrollY);
        var maxY = height + unzoom(scrollY);
        for (var y = 2 * gridStep; y < maxY; y += 2 * gridStep) {
            context.save();
            context.translate(scrollX, zoom(y));
            context.rotate(-Math.PI / 2);
            context.fillText(y, 2, rulerAtRight ? zoom(width) - 7 : 13);
            context.restore();
        }
        context.translate(0.5, -0.5);
        var maxX = width + unzoom(scrollX);
        for (var x = 2 * gridStep; x < maxX; x += 2 * gridStep) {
            context.save();
            context.fillText(x, zoom(x) + 2, rulerAtBottom ? scrollY + zoom(height) - 7 : scrollY + 13);
            context.restore();
        }
        context.restore();
    }

    {
        // Draw vertical grid
        context.save();
        if (rulerAtRight) {
            context.translate(zoom(width), 0);
            context.scale(-1, 1);
        }
        context.translate(-scrollX, 0.5 - scrollY);
        var maxY = height + unzoom(scrollY);
        for (var y = gridStep; y < maxY; y += gridStep) {
            context.beginPath();
            context.moveTo(scrollX, zoom(y));
            var markLength = (y % (gridStep * 2)) ? 5 : 8;
            context.lineTo(scrollX + markLength, zoom(y));
            context.stroke();
        }
        context.strokeStyle = lightGridColor;
        for (var y = gridSubStep; y < maxY; y += gridSubStep) {
            if (!(y % gridStep))
                continue;
            context.beginPath();
            context.moveTo(scrollX, zoom(y));
            context.lineTo(scrollX + gridSubStep, zoom(y));
            context.stroke();
        }
        context.restore();
    }

    {
        // Draw horizontal grid
        context.save();
        if (rulerAtBottom) {
            context.translate(0, zoom(height));
            context.scale(1, -1);
        }
        context.translate(0.5 - scrollX, -scrollY);
        var maxX = width + unzoom(scrollX);
        for (var x = gridStep; x < maxX; x += gridStep) {
            context.beginPath();
            context.moveTo(zoom(x), scrollY);
            var markLength = (x % (gridStep * 2)) ? 5 : 8;
            context.lineTo(zoom(x), scrollY + markLength);
            context.stroke();
        }
        context.strokeStyle = lightGridColor;
        for (var x = gridSubStep; x < maxX; x += gridSubStep) {
            if (!(x % gridStep))
                continue;
            context.beginPath();
            context.moveTo(zoom(x), scrollY);
            context.lineTo(zoom(x), scrollY + gridSubStep);
            context.stroke();
        }
        context.restore();
    }

    context.restore();
}

function drawViewSize()
{
    var text = viewportSize.width + "px \u00D7 " + viewportSize.height + "px";
    context.save();
    context.font = "20px ";
    switch (platform) {
    case "windows": context.font = "14px Consolas, Lucida Console"; break;
    case "mac": context.font = "14px Menlo, Monaco"; break;
    case "linux": context.font = "14px dejavu sans mono"; break;
    }

    var frameWidth = canvasWidth;
    var textWidth = context.measureText(text).width;
    context.fillStyle = gridBackgroundColor;
    context.fillRect(frameWidth - textWidth - 12, 0, frameWidth, 25);
    context.fillStyle = darkGridColor;
    context.fillText(text, frameWidth - textWidth - 6, 18);
    context.restore();
}

function resetCanvas(canvasElement)
{
    canvasElement.width = deviceScaleFactor * viewportSize.width;
    canvasElement.height = deviceScaleFactor * viewportSize.height;
    canvasElement.style.width = viewportSize.width + "px";
    canvasElement.style.height = viewportSize.height + "px";
    var context = canvasElement.getContext("2d");
    context.scale(deviceScaleFactor, deviceScaleFactor);
}

function reset(resetData)
{
    window.viewportSize = resetData.viewportSize;
    window.deviceScaleFactor = resetData.deviceScaleFactor;
    window.pageScaleFactor = resetData.pageScaleFactor;
    window.pageZoomFactor = resetData.pageZoomFactor;
    window.scrollX = Math.round(resetData.scrollX);
    window.scrollY = Math.round(resetData.scrollY);

    window.canvas = document.getElementById("canvas");
    window.context = canvas.getContext("2d");
    resetCanvas(canvas);

    window.canvasWidth = viewportSize.width;
    window.canvasHeight = viewportSize.height;

    window._controlsVisible = false;
    document.querySelector(".controls-line").style.visibility = "hidden";
    document.getElementById("element-title").style.visibility = "hidden";
    document.getElementById("tooltip-container").removeChildren();

    document.body.classList.remove("dimmed");

    window._gridPainted = false;

    if (window.layoutEditor)
        window.layoutEditor.reset();
}

function _drawElementTitle(context, elementInfo, bounds)
{
    document.getElementById("tag-name").textContent = elementInfo.tagName;
    document.getElementById("node-id").textContent = elementInfo.idValue ? "#" + elementInfo.idValue : "";
    document.getElementById("class-name").textContent = (elementInfo.className || "").trimEnd(50);
    document.getElementById("node-width").textContent = elementInfo.nodeWidth;
    document.getElementById("node-height").textContent = elementInfo.nodeHeight;
    var elementTitle = document.getElementById("element-title");

    var titleWidth = elementTitle.offsetWidth + 6;
    var titleHeight = elementTitle.offsetHeight + 4;

    var anchorTop = bounds.minY;
    var anchorBottom = bounds.maxY;
    var anchorLeft = bounds.minX;

    const arrowHeight = 7;
    var renderArrowUp = false;
    var renderArrowDown = false;

    var boxX = Math.max(2, anchorLeft);
    if (boxX + titleWidth > canvasWidth)
        boxX = canvasWidth - titleWidth - 2;

    var boxY;
    if (anchorTop > canvasHeight) {
        boxY = canvasHeight - titleHeight - arrowHeight;
        renderArrowDown = true;
    } else if (anchorBottom < 0) {
        boxY = arrowHeight;
        renderArrowUp = true;
    } else if (anchorBottom + titleHeight + arrowHeight < canvasHeight) {
        boxY = anchorBottom + arrowHeight - 4;
        renderArrowUp = true;
    } else if (anchorTop - titleHeight - arrowHeight > 0) {
        boxY = anchorTop - titleHeight - arrowHeight + 3;
        renderArrowDown = true;
    } else
        boxY = arrowHeight;

    context.save();
    context.translate(0.5, 0.5);
    context.beginPath();
    context.moveTo(boxX, boxY);
    if (renderArrowUp) {
        context.lineTo(boxX + 2 * arrowHeight, boxY);
        context.lineTo(boxX + 3 * arrowHeight, boxY - arrowHeight);
        context.lineTo(boxX + 4 * arrowHeight, boxY);
    }
    context.lineTo(boxX + titleWidth, boxY);
    context.lineTo(boxX + titleWidth, boxY + titleHeight);
    if (renderArrowDown) {
        context.lineTo(boxX + 4 * arrowHeight, boxY + titleHeight);
        context.lineTo(boxX + 3 * arrowHeight, boxY + titleHeight + arrowHeight);
        context.lineTo(boxX + 2 * arrowHeight, boxY + titleHeight);
    }
    context.lineTo(boxX, boxY + titleHeight);
    context.closePath();
    context.fillStyle = "rgb(255, 255, 194)";
    context.fill();
    context.strokeStyle = "rgb(128, 128, 128)";
    context.stroke();

    context.restore();

    elementTitle.style.visibility = "visible";
    elementTitle.style.top = (boxY + 3) + "px";
    elementTitle.style.left = (boxX + 3) + "px";
}

function _createElementDescription(elementInfo)
{
    var elementInfoElement = createElement("div", "element-info");
    var descriptionElement = elementInfoElement.createChild("div", "element-description monospace");
    var tagNameElement = descriptionElement.createChild("b").createChild("span", "material-tag-name");
    tagNameElement.textContent = elementInfo.tagName;
    var nodeIdElement = descriptionElement.createChild("span", "material-node-id");
    nodeIdElement.textContent = elementInfo.idValue ? "#" + elementInfo.idValue : "";
    nodeIdElement.classList.toggle("hidden", !elementInfo.idValue);

    var classNameElement = descriptionElement.createChild("span", "material-class-name");
    classNameElement.textContent = (elementInfo.className || "").trim(50);
    classNameElement.classList.toggle("hidden", !elementInfo.className);
    var dimensionsElement = elementInfoElement.createChild("div", "dimensions");
    dimensionsElement.createChild("span", "material-node-width").textContent = Math.round(elementInfo.nodeWidth * 100) / 100;
    dimensionsElement.createTextChild("\u00d7");
    dimensionsElement.createChild("span", "material-node-height").textContent = Math.round(elementInfo.nodeHeight * 100) / 100;
    return elementInfoElement;
}

function _drawMaterialElementTitle(elementInfo, bounds)
{
    var tooltipContainer = document.getElementById("tooltip-container");
    tooltipContainer.removeChildren();
    _createMaterialTooltip(tooltipContainer, bounds, _createElementDescription(elementInfo), true);
}

function _createMaterialTooltip(parentElement, bounds, contentElement, withArrow)
{
    var tooltipContainer = parentElement.createChild("div");
    var tooltipContent = tooltipContainer.createChild("div", "tooltip-content");
    tooltipContent.appendChild(contentElement);

    var titleWidth = tooltipContent.offsetWidth;
    var titleHeight = tooltipContent.offsetHeight;
    var arrowRadius = 8;
    var pageMargin = 2;

    var boxX = Math.min(bounds.minX, canvasWidth - titleWidth - pageMargin);

    var boxY = bounds.minY - arrowRadius - titleHeight;
    var onTop = true;
    if (boxY < 0) {
        boxY = Math.min(canvasHeight - titleHeight, bounds.maxY + arrowRadius);
        onTop = false;
    } else if (bounds.minY > canvasHeight) {
        boxY = canvasHeight - arrowRadius - titleHeight;
    }

    tooltipContent.style.top = boxY + "px";
    tooltipContent.style.left = boxX + "px";
    if (!withArrow)
        return;

    var tooltipArrow = tooltipContainer.createChild("div", "material-tooltip-arrow");
    // Left align arrow to the tooltip but ensure it is pointing to the element.
    var tooltipBorderRadius = 2;
    var arrowX = boxX + arrowRadius + tooltipBorderRadius;
    if (arrowX < bounds.minX)
        arrowX = bounds.minX + arrowRadius;
    // Hide arrow if element is completely off the sides of the page.
    var arrowHidden = arrowX < pageMargin + tooltipBorderRadius || arrowX + arrowRadius * 2 > canvasWidth - pageMargin - tooltipBorderRadius;
    tooltipArrow.classList.toggle("hidden", arrowHidden);
    if (!arrowHidden) {
        tooltipArrow.classList.toggle("tooltip-arrow-top", onTop);
        tooltipArrow.style.top = (onTop ? boxY + titleHeight : boxY - arrowRadius) + "px";
        tooltipArrow.style.left = arrowX + "px";
    }
}

function _drawRulers(context, bounds, rulerAtRight, rulerAtBottom)
{
    context.save();
    var width = canvasWidth;
    var height = canvasHeight;
    context.strokeStyle = "rgba(128, 128, 128, 0.3)";
    context.lineWidth = 1;
    context.translate(0.5, 0.5);

    if (rulerAtRight) {
        for (var y in bounds.rightmostXForY) {
            context.beginPath();
            context.moveTo(width, y);
            context.lineTo(bounds.rightmostXForY[y], y);
            context.stroke();
        }
    } else {
        for (var y in bounds.leftmostXForY) {
            context.beginPath();
            context.moveTo(0, y);
            context.lineTo(bounds.leftmostXForY[y], y);
            context.stroke();
        }
    }

    if (rulerAtBottom) {
        for (var x in bounds.bottommostYForX) {
            context.beginPath();
            context.moveTo(x, height);
            context.lineTo(x, bounds.topmostYForX[x]);
            context.stroke();
        }
    } else {
        for (var x in bounds.topmostYForX) {
            context.beginPath();
            context.moveTo(x, 0);
            context.lineTo(x, bounds.topmostYForX[x]);
            context.stroke();
        }
    }

    context.restore();
}

function drawPath(context, commands, fillColor, outlineColor, bounds)
{
    var commandsIndex = 0;

    function extractPoints(count)
    {
        var points = [];

        for (var i = 0; i < count; ++i) {
            var x = Math.round(commands[commandsIndex++]);
            bounds.maxX = Math.max(bounds.maxX, x);
            bounds.minX = Math.min(bounds.minX, x);

            var y = Math.round(commands[commandsIndex++]);
            bounds.maxY = Math.max(bounds.maxY, y);
            bounds.minY = Math.min(bounds.minY, y);

            bounds.leftmostXForY[y] = Math.min(bounds.leftmostXForY[y] || Number.MAX_VALUE, x);
            bounds.rightmostXForY[y] = Math.max(bounds.rightmostXForY[y] || Number.MIN_VALUE, x);
            bounds.topmostYForX[x] = Math.min(bounds.topmostYForX[x] || Number.MAX_VALUE, y);
            bounds.bottommostYForX[x] = Math.max(bounds.bottommostYForX[x] || Number.MIN_VALUE, y);
            points.push(x, y);
        }
        return points;
    }

    context.save();
    var commandsLength = commands.length;
    var path = new Path2D();
    while (commandsIndex < commandsLength) {
        switch (commands[commandsIndex++]) {
        case "M":
            path.moveTo.apply(path, extractPoints(1));
            break;
        case "L":
            path.lineTo.apply(path, extractPoints(1));
            break;
        case "C":
            path.bezierCurveTo.apply(path, extractPoints(3));
            break;
        case "Q":
            path.quadraticCurveTo.apply(path, extractPoints(2));
            break;
        case "Z":
            path.closePath();
            break;
        }
    }
    path.closePath();
    context.lineWidth = 0;
    context.fillStyle = fillColor;
    context.fill(path);

    if (outlineColor) {
        context.lineWidth = 2;
        context.strokeStyle = outlineColor;
        context.stroke(path);
    }

    context.restore();
    return path;
}

function emptyBounds()
{
    var bounds = {
        minX: Number.MAX_VALUE,
        minY: Number.MAX_VALUE,
        maxX: Number.MIN_VALUE,
        maxY: Number.MIN_VALUE,
        leftmostXForY: {},
        rightmostXForY: {},
        topmostYForX: {},
        bottommostYForX: {}
    };
    return bounds;
}

function drawHighlight(highlight, context)
{
    context = context || window.context;
    context.save();

    var bounds = emptyBounds();

    for (var paths = highlight.paths.slice(); paths.length;) {
        var path = paths.pop();
        drawPath(context, path.path, path.fillColor, path.outlineColor, bounds);
        if (paths.length) {
            context.save();
            context.globalCompositeOperation = "destination-out";
            drawPath(context, paths[paths.length - 1].path, "red", null, bounds);
            context.restore();
        }
    }

    var rulerAtRight = highlight.paths.length && highlight.showRulers && bounds.minX < 20 && bounds.maxX + 20 < canvasWidth;
    var rulerAtBottom = highlight.paths.length && highlight.showRulers && bounds.minY < 20 && bounds.maxY + 20 < canvasHeight;

    if (highlight.showRulers)
        _drawGrid(context, rulerAtRight, rulerAtBottom);

    if (highlight.paths.length) {
        if (highlight.showExtensionLines)
            _drawRulers(context, bounds, rulerAtRight, rulerAtBottom);

        if (highlight.elementInfo && highlight.displayAsMaterial)
           _drawMaterialElementTitle(highlight.elementInfo, bounds);
        else if (highlight.elementInfo)
           _drawElementTitle(context, highlight.elementInfo, bounds);
    }
    context.restore();

    return { bounds: bounds };
}

function setPlatform(platform)
{
    window.platform = platform;
    document.body.classList.add("platform-" + platform);
}

function dispatch(message)
{
    var functionName = message.shift();
    window[functionName].apply(null, message);
}

function log(text)
{
    document.getElementById("log").createChild("div").textContent = text;
}

function onResumeClick()
{
    InspectorOverlayHost.resume();
}

function onStepOverClick()
{
    InspectorOverlayHost.stepOver();
}

function onLoaded()
{
    document.getElementById("resume-button").addEventListener("click", onResumeClick);
    document.getElementById("step-over-button").addEventListener("click", onStepOverClick);
}

function eventHasCtrlOrMeta(event)
{
    return window.platform == "mac" ? (event.metaKey && !event.ctrlKey) : (event.ctrlKey && !event.metaKey);
}

function onDocumentKeyDown(event)
{
    if (!window._controlsVisible)
        return;
    if (event.keyIdentifier == "F8" || eventHasCtrlOrMeta(event) && event.keyCode == 220 /* backslash */)
        InspectorOverlayHost.resume();
    else if (event.keyIdentifier == "F10" || eventHasCtrlOrMeta(event) && event.keyCode == 222 /* single quote */)
        InspectorOverlayHost.stepOver();
}

function showLayoutEditor(info)
{
    if (!window.layoutEditor)
        window.layoutEditor = new LayoutEditor();

    window.layoutEditor.setState(info);
}

function setSelectorInLayoutEditor(selectorInfo)
{
    if (!window.layoutEditor)
        return;

    window.layoutEditor.setSelector(selectorInfo);
}

/**
 * @constructor
 */
function LayoutEditor()
{
    this._boundConsumeEvent = this._consumeEvent.bind(this);
    this._boundStrayClick = this._onStrayClick.bind(this);
    this._boundKeyDown = this._onKeyDown.bind(this);
    this._boundMouseWheel = this._onMouseWheel.bind(this);

    this._paddingBounds = this._defaultBounds();
    this._contentBounds = this._defaultBounds();
    this._marginBounds = this._defaultBounds();
    this._element = document.getElementById("editor");
    this._editorElement = this._element.createChild("div");
    this._selectorTooltipElement = this._element.createChild("div", "selector-tooltip-element");
    this._matchedNodesCanvas = createCanvas("layout-editor-matched-nodes-canvas");
    this._editorElement.parentElement.insertBefore(this._matchedNodesCanvas, this._editorElement);

    this._wallsElements = new Map();
    this._labelsElements = new Map();
    this._anchorsElements = new Map();
    this._anchorsInfo = new Map();
}

/**
 * @typedef {{
 *      type: string,
 *      propertyName: number,
 *      propertyValue: {value: number, unit: string, growInside: boolean, mutable: boolean}
 * }}
 */
LayoutEditor.AnchorInfo;

/**
 * @typedef {{
 *      left: number,
 *      top: number,
 *      right: number,
 *      bottom: number
 * }}
 */
LayoutEditor.Bounds;

/**
 * @typedef {{
 *      orientation: string,
 *      border1: string,
 *      border2: string,
 *      dimension1: string,
 *      dimension2: string
 * }}
 */
LayoutEditor.Bounds;

/**
 * @typedef {{
 *      p1: !Point,
 *      p2: !Point,
 *      p3: !Point,
 *      p4: !Point
 * }}
 */
LayoutEditor.Quad;

/**
 * @typedef {{
 *      contentQuad: !LayoutEditor.Quad,
 *      marginQuad: !LayoutEditor.Quad,
 *      paddingQuad: !LayoutEditor.Quad,
 *      anchors: !Array.<!LayoutEditor.AnchorInfo>
 * }}
 */
LayoutEditor.Info;

LayoutEditor._controlLaneWidth = 16;
LayoutEditor._wallWidth = 8;
LayoutEditor._handleWidth = 8;
LayoutEditor._labelOffset = 12;

LayoutEditor.prototype = {
    /**
     * @return {!LayoutEditor.Bounds}
     */
    _defaultBounds: function ()
    {
        var bounds = {
            left: Number.MAX_VALUE,
            top: Number.MAX_VALUE,
            right: Number.MIN_VALUE,
            bottom: Number.MIN_VALUE,
        };
        return bounds;
    },

    reset: function()
    {
        this._anchorsInfo.clear();
        this._anchorsElements.clear();
        this._wallsElements.clear();
        this._labelsElements.clear();
        this._paddingBounds = this._defaultBounds();
        this._contentBounds = this._defaultBounds();
        this._marginBounds = this._defaultBounds();

        resetCanvas(this._matchedNodesCanvas);
        document.body.style.cursor = "";

        this._editorElement.removeChildren();
        this._selectorTooltipElement.removeChildren();

        document.removeEventListener("mousedown", this._boundConsumeEvent);
        document.removeEventListener("mouseup", this._boundConsumeEvent);
        document.removeEventListener("click", this._boundStrayClick);
        document.removeEventListener("keydown", this._boundKeyDown);
        document.removeEventListener("mousewheel", this._boundMouseWheel);
        document.removeEventListener("mousemove", this._boundConsumeEvent);
    },

    /**
     * @param {!LayoutEditor.Info} info
     */
    setState: function(info)
    {
        this._editorElement.style.visibility = "visible";

        function buildBounds(quad)
        {
            var bounds = this._defaultBounds();
            for (var i = 1; i <= 4; ++i) {
                var point = quad["p" + i];
                bounds.left = Math.min(bounds.left, point.x);
                bounds.right = Math.max(bounds.right, point.x);
                bounds.top = Math.min(bounds.top, point.y);
                bounds.bottom = Math.max(bounds.bottom, point.y);
            }
            return bounds;
        }

        this._contentBounds = buildBounds.call(this, info.contentQuad);
        this._paddingBounds = buildBounds.call(this, info.paddingQuad);
        this._marginBounds = buildBounds.call(this, info.marginQuad);

        this._anchorsInfo = new Map();
        for (var i = 0; i < info.anchors.length; ++i)
            this._anchorsInfo.set(info.anchors[i].propertyName, info.anchors[i])

        document.addEventListener("mousedown", this._boundConsumeEvent);
        document.addEventListener("mouseup", this._boundConsumeEvent);
        document.addEventListener("click", this._boundStrayClick);
        document.addEventListener("keydown", this._boundKeyDown);
        document.addEventListener("mousewheel", this._boundMouseWheel);

        this._createBlurWindow();
        this._createGuideLines();
        this._createControlLanes(info.anchors);
        document.addEventListener("mousemove", this._boundConsumeEvent);

        if (this._draggedPropertyName) {
            document.body.style.cursor = (this._draggedPropertyName.endsWith("left") || this._draggedPropertyName.endsWith("right")) ? "ew-resize" : "ns-resize";
            this._toggleHighlightedState(this._draggedPropertyName, true);
        }
    },

    _createBlurWindow: function()
    {
        var left = this._editorElement.createChild("div", "blur-rect");
        left.style.height = canvasHeight + "px";
        left.style.width = this._marginBounds.left + "px";
        var top = this._editorElement.createChild("div", "blur-rect");
        top.style.left = this._marginBounds.left + "px";
        top.style.width = canvasWidth - this._marginBounds.left + "px";
        top.style.height = this._marginBounds.top  + "px";
        var right = this._editorElement.createChild("div", "blur-rect");
        right.style.left = this._marginBounds.right + "px";
        right.style.top = this._marginBounds.top + "px";
        right.style.width = (canvasWidth - this._marginBounds.right) + "px";
        right.style.height = (canvasHeight - this._marginBounds.top) + "px";
        var bottom = this._editorElement.createChild("div", "blur-rect");
        bottom.style.left = this._marginBounds.left + "px";
        bottom.style.top = this._marginBounds.bottom + "px";
        bottom.style.width = this._marginBounds.right - this._marginBounds.left + "px";
        bottom.style.height = canvasHeight - this._marginBounds.bottom + "px";
        top.addEventListener("click", this._boundStrayClick);
        bottom.addEventListener("click", this._boundStrayClick);
        left.addEventListener("click", this._boundStrayClick);
        right.addEventListener("click", this._boundStrayClick);
    },

    _createGuideLines: function()
    {
        /**
         * @param {number} x
         * @param {string} type
         * @this {LayoutEditor}
         */
        function verticalLine(x, type)
        {
            var verticalElement = this._editorElement.createChild("div", "guide-line vertical");
            verticalElement.classList.add(type);
            verticalElement.style.height = canvasHeight + "px";
            verticalElement.style.top = "0px";
            verticalElement.style.left = x + "px";
        }

        /**
         * @param {number} y
         * @param {string} type
         * @this {LayoutEditor}
         */
        function horizontalLine(y, type)
        {
            var horizontalElement = this._editorElement.createChild("div", "guide-line horizontal");
            horizontalElement.classList.add(type);
            horizontalElement.style.width = canvasWidth + "px";
            horizontalElement.style.left = "0px";
            horizontalElement.style.top = y + "px";
        }

        /**
         * @param {!LayoutEditor.Bounds} bounds
         * @param {string} type
         * @this {LayoutEditor}
         */
        function guideLines(bounds, type)
        {
            verticalLine.call(this, bounds.left, type);
            verticalLine.call(this, bounds.right, type);
            horizontalLine.call(this, bounds.top, type);
            horizontalLine.call(this, bounds.bottom, type);
        }

        guideLines.call(this, this._contentBounds, "content");
        guideLines.call(this, this._paddingBounds, "padding");
        guideLines.call(this, this._marginBounds, "margin");
    },

    /**
     * @param {!Element} parent
     * @param {!Element} anchorElement
     * @param {!LayoutEditor.AnchorInfo} anchorInfo
     */
    _createLabel: function(parent, anchorElement, anchorInfo)
    {
        var label = parent.createChild("div", "label " + anchorInfo.type);
        var nameElement = label.createChild("span", "name");
        var anchorName = anchorInfo.propertyName;
        nameElement.textContent = anchorName + ": ";
        var valueElement = label.createChild("span", "value");
        valueElement.textContent = String(parseFloat(anchorInfo.propertyValue.value.toFixed(2)));
        var dimensionElement = label.createChild("span", "dimension");
        dimensionElement.textContent = "\u2009" + anchorInfo.propertyValue.unit;

        var leftX = anchorElement.offsetLeft - LayoutEditor._labelOffset - label.offsetWidth;
        var fitLeft = leftX > 0;
        var rightX = anchorElement.offsetLeft + anchorElement.offsetWidth  + LayoutEditor._labelOffset;
        var fitRight = rightX + label.offsetWidth < canvasWidth;
        var growsInside = anchorInfo.propertyValue.growInside;
        var toLeft = anchorName.endsWith("right") && growsInside ||  anchorName.endsWith("left") && !growsInside;
        var startPosition;
        if (!fitLeft || (!toLeft && fitRight)) {
            startPosition = rightX;
            label.classList.add("left-arrow");
        } else {
            startPosition = leftX;
            label.classList.add("right-arrow");
        }

        var y = anchorElement.offsetTop + anchorElement.offsetHeight / 2;
        label.style.left = (startPosition | 0) + "px";
        label.style.top = ((y - label.offsetHeight / 2) | 0) + "px";

        label.classList.add("hidden");
        return label;
    },

    /**
     * @param {string} anchorName
     * @param {boolean} highlighted
     */
    _toggleHighlightedState: function(anchorName, highlighted)
    {
        this._anchorsElements.get(anchorName).classList.toggle("highlighted", highlighted);
        this._wallsElements.get(anchorName).classList.toggle("highlighted", highlighted);
        this._labelsElements.get(anchorName).classList.toggle("hidden", !highlighted);
    },

    _createControlLanes: function()
    {
        var descriptionH = {orientation: "horizontal", border1: "left", border2: "top", dimension1: "width", dimension2: "height"};
        var descriptionV = {orientation: "vertical", border1: "top", border2: "left", dimension1: "height", dimension2: "width"};
        var sidesByOrientation = {horizontal : ["left", "right"], vertical : ["top", "bottom"]};

        /**
         * @param {!Element} parent
         * @param {!LayoutEditor.Bounds} innerBox
         * @param {!LayoutEditor.Bounds} outerBox
         * @param {string} type
         * @param {string} side
         * @param {!LayoutEditor.Description} description
         * @param {number} border2Position
         * @this {LayoutEditor}
         */
        function createAnchorWithWall(parent, innerBox, outerBox, type, side, description, border2Position)
        {
            var anchorName = type + "-" + side;
            var anchorInfo = this._anchorsInfo.get(anchorName);
            var growsInside = anchorInfo.propertyValue.growInside;

            var anchorPosition = (growsInside ? innerBox[side] : outerBox[side]);
            var anchorElement = parent.createChild("div", "editor-anchor " + description.orientation + " " + type);
            var handleHalf = LayoutEditor._handleWidth / 2;
            anchorElement.style[description.border1] = (anchorPosition - handleHalf) + "px";
            anchorElement.style[description.border2] = border2Position + "px";
            this._anchorsElements.set(anchorName, anchorElement);

            if (anchorInfo.propertyValue.mutable)
                anchorElement.addEventListener("mousedown", this._onAnchorMouseDown.bind(this, anchorName));

            anchorElement.addEventListener("mouseenter", this._toggleHighlightedState.bind(this, anchorName, true));
            anchorElement.addEventListener("mouseleave", this._toggleHighlightedState.bind(this, anchorName, false));

            var wallElement = parent.createChild("div", "wall " + description.orientation + " " + type);
            var wallPosition = (growsInside ? outerBox[side] : innerBox[side]);
            var minSide = (side === "left" || side === "top");
            wallPosition += (minSide === growsInside) ? - handleHalf - LayoutEditor._wallWidth : handleHalf;
            wallElement.style[description.border1] = wallPosition + "px";
            wallElement.style[description.border2] = border2Position + "px";
            
            this._wallsElements.set(anchorName, wallElement);

            var labelElement = this._createLabel(parent, anchorElement, anchorInfo);
            this._labelsElements.set(anchorName, labelElement);
        }

        /**
         * @param {!Element} parent
         * @param {!LayoutEditor.Bounds} innerBox
         * @param {!LayoutEditor.Bounds} outerBox
         * @param {string} type
         * @param {!LayoutEditor.Description} description
         * @param {number} border2Position
         * @this {LayoutEditor}
         */
        function createLane(parent, innerBox, outerBox, type, description, border2Position)
        {
            var verticalElement = parent.createChild("div", "control-lane " + type);
            var sides = sidesByOrientation[description.orientation];
            verticalElement.style[description.border1] = outerBox[sides[0]] + "px";
            verticalElement.style[description.border2] = border2Position + "px";
            verticalElement.style[description.dimension1] = (outerBox[sides[1]] - outerBox[sides[0]]) + "px";
            verticalElement.style[description.dimension2] = LayoutEditor._controlLaneWidth + "px";
            createAnchorWithWall.call(this, parent, innerBox, outerBox, type, sides[0], description, border2Position);
            createAnchorWithWall.call(this, parent, innerBox, outerBox, type, sides[1], description, border2Position);
        }

        var yPosition = 0;
        var xPosition = 0;
        if (this._marginBounds.bottom + 2 * LayoutEditor._controlLaneWidth <= canvasHeight)
            yPosition = this._marginBounds.bottom;
        else if (this._marginBounds.top - 2 * LayoutEditor._controlLaneWidth >= 0)
            yPosition = this._marginBounds.top - 2 * LayoutEditor._controlLaneWidth;
        else
            yPosition = (this._contentBounds.top + this._contentBounds.bottom) / 2;

        if (this._marginBounds.left - 2 * LayoutEditor._controlLaneWidth >= 0)
            xPosition = this._marginBounds.left - 2 * LayoutEditor._controlLaneWidth;
        else if (this._marginBounds.right + 2 * LayoutEditor._controlLaneWidth <= canvasWidth)
            xPosition = this._marginBounds.right;
        else
            xPosition = (this._contentBounds.right + this._contentBounds.left) / 2;

        createLane.call(this, this._editorElement, this._contentBounds, this._paddingBounds, "padding", descriptionH, yPosition);
        createLane.call(this, this._editorElement, this._paddingBounds, this._marginBounds, "margin", descriptionH, yPosition + LayoutEditor._controlLaneWidth);
        createLane.call(this, this._editorElement, this._contentBounds, this._paddingBounds, "padding", descriptionV, xPosition);
        createLane.call(this, this._editorElement, this._paddingBounds, this._marginBounds, "margin", descriptionV, xPosition + LayoutEditor._controlLaneWidth);
    },

    setSelector: function(selectorInfo)
    {
        this._selectorTooltipElement.removeChildren();
        var containerElement = createElement("div");

        for (var i = (selectorInfo.medias || []).length - 1; i >= 0; --i)
            containerElement.createChild("div", "layout-editor-media-tooltip").textContent = ("@media " + selectorInfo.medias[i]).trim(50);

        var selectorElement = containerElement.createChild("div", "layout-editor-selector-tooltip");
        selectorElement.textContent = selectorInfo.selector.trimEnd(50);

        var margin = 40;
        var bounds = {minX: this._marginBounds.left, maxX: this._marginBounds.right,
                minY: this._marginBounds.top - margin, maxY: this._marginBounds.bottom + margin};
        _createMaterialTooltip(this._selectorTooltipElement, bounds, containerElement);
        resetCanvas(this._matchedNodesCanvas);

        if (!selectorInfo.nodes)
            return;

        for (var nodeHighlight of selectorInfo.nodes)
            drawHighlight(nodeHighlight, this._matchedNodesCanvas.getContext("2d"));
    },

    _calculateDelta: function(deltaVector, moveDelta)
    {
        return scalarProduct(deltaVector, moveDelta) / Math.sqrt(scalarProduct(deltaVector, deltaVector));
    },

    /**
     * @param {string} anchorName
     * @return {!Point}
     */
    _defaultDeltaVector: function(anchorName)
    {
        if (anchorName.endsWith("right"))
            return new Point(1, 0);
        if (anchorName.endsWith("left"))
            return new Point(-1, 0);
        if (anchorName.endsWith("top"))
            return new Point(0, -1);
        if (anchorName.endsWith("bottom"))
            return new Point(0, 1);
    },

    /**
     * @param {string} anchorName
     * @param {!Event} event
     */
    _onAnchorMouseDown: function(anchorName, event)
    {
        // Only drag upon left button. Right will likely cause a context menu. So will ctrl-click on mac.
        if (event.button || (window.platform == "mac" && event.ctrlKey))
            return;

        event.preventDefault();
        var deltaVector = this._defaultDeltaVector(anchorName);
        var anchorInfo = this._anchorsInfo.get(anchorName);
        if (anchorInfo.propertyValue.growInside)
            deltaVector = new Point(-deltaVector.x, -deltaVector.y);

        this._boundDragMove = this._onDragMove.bind(this, new Point(event.screenX, event.screenY), deltaVector);
        this._boundDragEnd = this._onDragEnd.bind(this);
        document.addEventListener("mousemove", this._boundDragMove);
        document.addEventListener("mouseup", this._boundDragEnd);
        InspectorOverlayHost.startPropertyChange(anchorName);
        this._preciseDrag = !!event.shiftKey;
        this._draggedPropertyName = anchorName;
        this._toggleHighlightedState(anchorName, true);
    },

    /**
     * @param {!Point} mouseDownPoint
     * @param {!Point} deltaVector
     * @param {!Event} event
     */
    _onDragMove: function(mouseDownPoint, deltaVector, event)
    {
        if (event.buttons !== 1) {
            this._onDragEnd(event);
            return;
        }
        event.preventDefault();
        if (this._preciseDrag !== event.shiftKey) {
            InspectorOverlayHost.endPropertyChange();
            document.removeEventListener("mousemove", this._boundDragMove);
            mouseDownPoint = new Point(event.screenX, event.screenY);
            this._boundDragMove = this._onDragMove.bind(this, mouseDownPoint, deltaVector);
            document.addEventListener("mousemove", this._boundDragMove);
            this._preciseDrag = event.shiftKey;
            InspectorOverlayHost.startPropertyChange(this._draggedPropertyName);
        }

        var preciseFactor = this._preciseDrag ? 5 : 1;
        InspectorOverlayHost.changeProperty(this._calculateDelta(deltaVector, new Point(event.screenX - mouseDownPoint.x, event.screenY - mouseDownPoint.y)) / preciseFactor);
    },

    /**
     * @param {!Event} event
     */
    _onDragEnd: function(event)
    {
        document.removeEventListener("mousemove", this._boundDragMove);
        document.removeEventListener("mouseup", this._boundDragEnd);
        delete this._boundDragMove;
        delete this._boundDragEnd;
        this._toggleHighlightedState(this._draggedPropertyName, false);
        document.body.style.cursor = "";
        delete this._draggedPropertyName;
        delete this._preciseDrag;
        event.preventDefault();
        InspectorOverlayHost.endPropertyChange();
    },

    /**
     * @param {!Event} event
     */
    _onStrayClick: function(event)
    {
        event.preventDefault();
        InspectorOverlayHost.clearSelection(true);
    },

    /**
     * @param {!Event} event
     */
    _onKeyDown: function(event)
    {
        if (this._draggedPropertyName)
            return;

        // Clear selection on Esc.
        if (event.keyIdentifier === "U+001B") {
            event.preventDefault();
            InspectorOverlayHost.clearSelection(false);
        }
    },

    /**
     * @param {!Event} event
     */
    _onMouseWheel: function(event)
    {
        event.preventDefault();
        this._mouseWheelDelta = (this._mouseWheelDelta || 0) + event.wheelDelta;
        if (this._mouseWheelDelta >= 120) {
            InspectorOverlayHost.nextSelector();
            this._mouseWheelDelta = 0;
        } else if (this._mouseWheelDelta <= -120) {
            InspectorOverlayHost.previousSelector();
            this._mouseWheelDelta = 0;
        }
    },

    /**
     * @param {!Event} event
     */
    _consumeEvent: function(event)
    {
        event.preventDefault();
    }
}

/**
 * @constructor
 * @param {number} x
 * @param {number} y
 */
function Point(x, y)
{
    this.x = x;
    this.y = y;
}

function createCanvas(id)
{
    var canvas = createElement("canvas", "fill");
    canvas.id = id;
    resetCanvas(canvas);
    return canvas;
}

function scalarProduct(v1, v2)
{
    return v1.x * v2.x + v1.y * v2.y;
}

Element.prototype.createChild = function(tagName, className)
{
    var element = createElement(tagName, className);
    element.addEventListener("click", function(e) { e.stopPropagation(); }, false);
    this.appendChild(element);
    return element;
}

Element.prototype.createTextChild = function(text)
{
    var element = document.createTextNode(text);
    this.appendChild(element);
    return element;
}

Element.prototype.removeChildren = function()
{
    if (this.firstChild)
        this.textContent = "";
}

function createElement(tagName, className)
{
    var element = document.createElement(tagName);
    if (className)
        element.className = className;
    return element;
}

String.prototype.trimEnd = function(maxLength)
{
    if (this.length <= maxLength)
        return String(this);
    return this.substr(0, maxLength - 1) + "\u2026";
}

window.addEventListener("DOMContentLoaded", onLoaded);
document.addEventListener("keydown", onDocumentKeyDown);
</script>
</head>
<body class="fill">
</body>
<canvas id="canvas" class="fill"></canvas>
<div id="element-title">
  <span id="tag-name"></span><span id="node-id"></span><span id="class-name"></span>
  <span id="node-width"></span><span class="px">px</span><span class="px"> &#xD7; </span><span id="node-height"></span><span class="px">px</span>
</div>
<div id="tooltip-container"></div>
<div id="editor" class="fill"></div>
<div class="controls-line">
    <div class="message-box"><div id="paused-in-debugger"></div></div>
    <div class="button" id="resume-button" title="Resume script execution (F8)."><div class="glyph"></div></div>
    <div class="button" id="step-over-button" title="Step over next function call (F10)."><div class="glyph"></div></div>
</div>
<div id="log"></div>
</html>
/**
 * This is a placeholder to create the
 * IDR_PRIVATE_SCRIPT_DOCUMENTEXECCOMMAND_JS resource.
 *
 * It will be replaced by a complete file in:
 *   https://codereview.chromium.org/530663002/
 */
/* Copyright 2014 The Chromium Authors. All rights reserved.
 * Use of this source code is governed by a BSD-style license that can be
 * found in the LICENSE file.
 */

div.header {
    border-bottom: 2px solid black;
    padding-bottom: 5px;
    margin: 10px;
}

div.collapsible > div.hidden {
    display:none;
}

.pretty-print {
    margin-top: 1em;
    margin-left: 20px;
    font-family: monospace;
    font-size: 13px;
}

#webkit-xml-viewer-source-xml {
    display: none;
}

.collapsible-content {
    margin-left: 1em;
}
.comment {
    white-space: pre;
}

.button {
    -webkit-user-select: none;
    cursor: pointer;
    display: inline-block;
    margin-left: -10px;
    width: 10px;
    background-repeat: no-repeat;
    background-position: left top;
    vertical-align: bottom;
}

.collapse-button {
    background: url("data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' fill='#909090' width='10' height='10'><path d='M0 0 L8 0 L4 7 Z'/></svg>");
    height: 10px;
}

.expand-button {
    background: url("data:image/svg+xml,<svg xmlns='http://www.w3.org/2000/svg' fill='#909090' width='10' height='10'><path d='M0 0 L0 8 L7 4 Z'/></svg>");
    height: 10px;
}
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

"use strict";

var xmlTreeViewerCSS = privateScriptController.import("DocumentXMLTreeViewer.css");

privateScriptController.installClass("Document", function(DocumentPrototype) {
    var nodeParentPairs = [];
    var tree;

    function prepareWebKitXMLViewer(noStyleMessage)
    {
        var html = createHTMLElement('html');
        var head = createHTMLElement('head');
        html.appendChild(head);
        var style = createHTMLElement('style');
        style.id = 'xml-viewer-style';
        style.appendChild(document.createTextNode(xmlTreeViewerCSS));
        head.appendChild(style);
        var body = createHTMLElement('body');
        html.appendChild(body);
        var sourceXML = createHTMLElement('div');
        sourceXML.id = 'webkit-xml-viewer-source-xml';
        body.appendChild(sourceXML);

        var child;
        while (child = document.firstChild) {
            document.removeChild(child);
            if (child.nodeType != Node.DOCUMENT_TYPE_NODE)
                sourceXML.appendChild(child);
        }
        document.appendChild(html);

        var header = createHTMLElement('div');
        body.appendChild(header);
        header.classList.add('header');
        var headerSpan = createHTMLElement('span');
        header.appendChild(headerSpan);
        headerSpan.textContent = noStyleMessage;
        header.appendChild(createHTMLElement('br'));

        tree = createHTMLElement('div');
        body.appendChild(tree);
        tree.classList.add('pretty-print');
        window.onload = sourceXMLLoaded;
    }

    function sourceXMLLoaded()
    {
        var sourceXML = document.getElementById('webkit-xml-viewer-source-xml');
        if (!sourceXML)
            return; // Stop if some XML tree extension is already processing this document

        for (var child = sourceXML.firstChild; child; child = child.nextSibling)
            nodeParentPairs.push({parentElement: tree, node: child});

        for (var i = 0; i < nodeParentPairs.length; i++)
            processNode(nodeParentPairs[i].parentElement, nodeParentPairs[i].node);

        initButtons();

        return false;
    }

    // Tree processing.

    function processNode(parentElement, node)
    {
        var map = processNode.processorsMap;
        if (!map) {
            map = {};
            processNode.processorsMap = map;
            map[Node.PROCESSING_INSTRUCTION_NODE] = processProcessingInstruction;
            map[Node.ELEMENT_NODE] = processElement;
            map[Node.COMMENT_NODE] = processComment;
            map[Node.TEXT_NODE] = processText;
            map[Node.CDATA_SECTION_NODE] = processCDATA;
        }
        if (processNode.processorsMap[node.nodeType])
            processNode.processorsMap[node.nodeType].call(this, parentElement, node);
    }

    function processElement(parentElement, node)
    {
        if (!node.firstChild)
            processEmptyElement(parentElement, node);
        else {
            var child = node.firstChild;
            if (child.nodeType == Node.TEXT_NODE && isShort(child.nodeValue) && !child.nextSibling)
                processShortTextOnlyElement(parentElement, node);
            else
                processComplexElement(parentElement, node);
        }
    }

    function processEmptyElement(parentElement, node)
    {
        var line = createLine();
        line.appendChild(createTag(node, false, true));
        parentElement.appendChild(line);
    }

    function processShortTextOnlyElement(parentElement, node)
    {
        var line = createLine();
        line.appendChild(createTag(node, false, false));
        for (var child = node.firstChild; child; child = child.nextSibling)
            line.appendChild(createText(child.nodeValue));
        line.appendChild(createTag(node, true, false));
        parentElement.appendChild(line);
    }

    function processComplexElement(parentElement, node)
    {
        var collapsible = createCollapsible();

        collapsible.expanded.start.appendChild(createTag(node, false, false));
        for (var child = node.firstChild; child; child = child.nextSibling)
            nodeParentPairs.push({parentElement: collapsible.expanded.content, node: child});
        collapsible.expanded.end.appendChild(createTag(node, true, false));

        collapsible.collapsed.content.appendChild(createTag(node, false, false));
        collapsible.collapsed.content.appendChild(createText('...'));
        collapsible.collapsed.content.appendChild(createTag(node, true, false));
        parentElement.appendChild(collapsible);
    }

    function processComment(parentElement, node)
    {
        if (isShort(node.nodeValue)) {
            var line = createLine();
            line.appendChild(createComment('<!-- ' + node.nodeValue + ' -->'));
            parentElement.appendChild(line);
        } else {
            var collapsible = createCollapsible();

            collapsible.expanded.start.appendChild(createComment('<!--'));
            collapsible.expanded.content.appendChild(createComment(node.nodeValue));
            collapsible.expanded.end.appendChild(createComment('-->'));

            collapsible.collapsed.content.appendChild(createComment('<!--'));
            collapsible.collapsed.content.appendChild(createComment('...'));
            collapsible.collapsed.content.appendChild(createComment('-->'));
            parentElement.appendChild(collapsible);
        }
    }

    function processCDATA(parentElement, node)
    {
        if (isShort(node.nodeValue)) {
            var line = createLine();
            line.appendChild(createText('<![CDATA[ ' + node.nodeValue + ' ]]>'));
            parentElement.appendChild(line);
        } else {
            var collapsible = createCollapsible();

            collapsible.expanded.start.appendChild(createText('<![CDATA['));
            collapsible.expanded.content.appendChild(createText(node.nodeValue));
            collapsible.expanded.end.appendChild(createText(']]>'));

            collapsible.collapsed.content.appendChild(createText('<![CDATA['));
            collapsible.collapsed.content.appendChild(createText('...'));
            collapsible.collapsed.content.appendChild(createText(']]>'));
            parentElement.appendChild(collapsible);
        }
    }

    function processProcessingInstruction(parentElement, node)
    {
        if (isShort(node.nodeValue)) {
            var line = createLine();
            line.appendChild(createComment('<?' + node.nodeName + ' ' + node.nodeValue + '?>'));
            parentElement.appendChild(line);
        } else {
            var collapsible = createCollapsible();

            collapsible.expanded.start.appendChild(createComment('<?' + node.nodeName));
            collapsible.expanded.content.appendChild(createComment(node.nodeValue));
            collapsible.expanded.end.appendChild(createComment('?>'));

            collapsible.collapsed.content.appendChild(createComment('<?' + node.nodeName));
            collapsible.collapsed.content.appendChild(createComment('...'));
            collapsible.collapsed.content.appendChild(createComment('?>'));
            parentElement.appendChild(collapsible);
        }
    }

    function processText(parentElement, node)
    {
        parentElement.appendChild(createText(node.nodeValue));
    }

    // Processing utils.

    function trim(value)
    {
        return value.replace(/^\s\s*/, '').replace(/\s\s*$/, '');
    }

    function isShort(value)
    {
        return trim(value).length <= 50;
    }

    // Tree rendering.

    function createHTMLElement(elementName)
    {
        return document.createElementNS('http://www.w3.org/1999/xhtml', elementName)
    }

    function createCollapsible()
    {
        var collapsible = createHTMLElement('div');
        collapsible.classList.add('collapsible');
        collapsible.expanded = createHTMLElement('div');
        collapsible.expanded.classList.add('expanded');
        collapsible.appendChild(collapsible.expanded);

        collapsible.expanded.start = createLine();
        collapsible.expanded.start.appendChild(createCollapseButton());
        collapsible.expanded.appendChild(collapsible.expanded.start);

        collapsible.expanded.content = createHTMLElement('div');
        collapsible.expanded.content.classList.add('collapsible-content');
        collapsible.expanded.appendChild(collapsible.expanded.content);

        collapsible.expanded.end = createLine();
        collapsible.expanded.appendChild(collapsible.expanded.end);

        collapsible.collapsed = createHTMLElement('div');
        collapsible.collapsed.classList.add('collapsed');
        collapsible.collapsed.classList.add('hidden');
        collapsible.appendChild(collapsible.collapsed);
        collapsible.collapsed.content = createLine();
        collapsible.collapsed.content.appendChild(createExpandButton());
        collapsible.collapsed.appendChild(collapsible.collapsed.content);

        return collapsible;
    }

    function createButton()
    {
        var button = createHTMLElement('span');
        button.classList.add('button');
        return button;
    }

    function createCollapseButton(str)
    {
        var button = createButton();
        button.classList.add('collapse-button');
        return button;
    }

    function createExpandButton(str)
    {
        var button = createButton();
        button.classList.add('expand-button');
        return button;
    }

    function createComment(commentString)
    {
        var comment = createHTMLElement('span');
        comment.classList.add('comment');
        comment.classList.add('html-comment');
        comment.textContent = commentString;
        return comment;
    }

    function createText(value)
    {
        var text = createHTMLElement('span');
        text.textContent = trim(value);
        text.classList.add('text');
        return text;
    }

    function createLine()
    {
        var line = createHTMLElement('div');
        line.classList.add('line');
        return line;
    }

    function createTag(node, isClosing, isEmpty)
    {
        var tag = createHTMLElement('span');
        tag.classList.add('html-tag');

        var stringBeforeAttrs = '<';
        if (isClosing)
            stringBeforeAttrs += '/';
        stringBeforeAttrs += node.nodeName;
        var textBeforeAttrs = document.createTextNode(stringBeforeAttrs);
        tag.appendChild(textBeforeAttrs);

        if (!isClosing) {
            for (var i = 0; i < node.attributes.length; i++)
                tag.appendChild(createAttribute(node.attributes[i]));
        }

        var stringAfterAttrs = '';
        if (isEmpty)
            stringAfterAttrs += '/';
        stringAfterAttrs += '>';
        var textAfterAttrs = document.createTextNode(stringAfterAttrs);
        tag.appendChild(textAfterAttrs);

        return tag;
    }

    function createAttribute(attributeNode)
    {
        var attribute = createHTMLElement('span');
        attribute.classList.add('html-attribute');

        var attributeName = createHTMLElement('span');
        attributeName.classList.add('html-attribute-name');
        attributeName.textContent = attributeNode.name;

        var textBefore = document.createTextNode(' ');
        var textBetween = document.createTextNode('="');

        var attributeValue = createHTMLElement('span');
        attributeValue.classList.add('html-attribute-value');
        attributeValue.textContent = attributeNode.value;

        var textAfter = document.createTextNode('"');

        attribute.appendChild(textBefore);
        attribute.appendChild(attributeName);
        attribute.appendChild(textBetween);
        attribute.appendChild(attributeValue);
        attribute.appendChild(textAfter);
        return attribute;
    }

    function expandFunction(sectionId)
    {
        return function()
        {
            document.querySelector('#' + sectionId + ' > .expanded').className = 'expanded';
            document.querySelector('#' + sectionId + ' > .collapsed').className = 'collapsed hidden';
        };
    }

    function collapseFunction(sectionId)
    {
        return function()
        {
            document.querySelector('#' + sectionId + ' > .expanded').className = 'expanded hidden';
            document.querySelector('#' + sectionId + ' > .collapsed').className = 'collapsed';
        };
    }

    function initButtons()
    {
        var sections = document.querySelectorAll('.collapsible');
        for (var i = 0; i < sections.length; i++) {
            var sectionId = 'collapsible' + i;
            sections[i].id = sectionId;

            var expandedPart = sections[i].querySelector('#' + sectionId + ' > .expanded');
            var collapseButton = expandedPart.querySelector('.collapse-button');
            collapseButton.onclick = collapseFunction(sectionId);
            collapseButton.onmousedown = handleButtonMouseDown;

            var collapsedPart = sections[i].querySelector('#' + sectionId + ' > .collapsed');
            var expandButton = collapsedPart.querySelector('.expand-button');
            expandButton.onclick = expandFunction(sectionId);
            expandButton.onmousedown = handleButtonMouseDown;
        }

    }

    function handleButtonMouseDown(e)
    {
       // To prevent selection on double click
       e.preventDefault();
    }

    DocumentPrototype.transformDocumentToTreeView = function(noStyleMessage) {
        prepareWebKitXMLViewer(noStyleMessage);
    }
});

// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

'use strict';

privateScriptController.installClass('HTMLMarqueeElement', function(HTMLMarqueeElementPrototype) {

    var kDefaultScrollAmount = 6;
    var kDefaultScrollDelayMS = 85;
    var kMinimumScrollDelayMS = 60;

    var kDefaultLoopLimit = -1;

    var kBehaviorScroll = 'scroll';
    var kBehaviorSlide = 'slide';
    var kBehaviorAlternate = 'alternate';

    var kDirectionLeft = 'left';
    var kDirectionRight = 'right';
    var kDirectionUp = 'up';
    var kDirectionDown = 'down';

    var kPresentationalAttributes = [
        'bgcolor',
        'height',
        'hspace',
        'vspace',
        'width',
    ];

    function convertHTMLLengthToCSSLength(value) {
        var match = value.match(/^\s*([\d.]+)(%)?\s*/);
        if (match)
            return match[2] === '%' ? match[1] + '%' : match[1] + 'px';
        return null;
    }

    // FIXME: Consider moving these utility functions to PrivateScriptRunner.js.
    var kInt32Max = Math.pow(2, 31);

    function convertToLong(n) {
        // Using parseInt() is wrong but this aligns with the existing behavior of StringImpl::toInt().
        // FIXME: Implement a correct algorithm of the Web IDL value conversion.
        var value = parseInt(n);
        if (!isNaN(value) && -kInt32Max <= value && value < kInt32Max)
            return value;
        return NaN;
    }

    function reflectAttribute(prototype, attributeName, propertyName) {
        Object.defineProperty(prototype, propertyName, {
            get: function() {
                return this.getAttribute(attributeName) || '';
            },
            set: function(value) {
                this.setAttribute(attributeName, value);
            },
            configurable: true,
            enumerable: true,
        });
    }

    function reflectBooleanAttribute(prototype, attributeName, propertyName) {
        Object.defineProperty(prototype, propertyName, {
            get: function() {
                return this.getAttribute(attributeName);
            },
            set: function(value) {
                if (value)
                    this.setAttribute(attributeName, '');
                else
                    this.removeAttribute(attributeName);
            },
        });
    }

    reflectAttribute(HTMLMarqueeElementPrototype, 'behavior', 'behavior');
    reflectAttribute(HTMLMarqueeElementPrototype, 'bgcolor', 'bgColor');
    reflectAttribute(HTMLMarqueeElementPrototype, 'direction', 'direction');
    reflectAttribute(HTMLMarqueeElementPrototype, 'height', 'height');
    reflectAttribute(HTMLMarqueeElementPrototype, 'hspace', 'hspace');
    reflectAttribute(HTMLMarqueeElementPrototype, 'vspace', 'vspace');
    reflectAttribute(HTMLMarqueeElementPrototype, 'width', 'width');
    reflectBooleanAttribute(HTMLMarqueeElementPrototype, 'truespeed', 'trueSpeed');

    HTMLMarqueeElementPrototype.createdCallback = function() {
        var shadow = this.createShadowRoot();
        var style = document.createElement('style');
        style.textContent = ':host { display: inline-block; width: -webkit-fill-available; overflow: hidden; text-align: initial; white-space: nowrap; }'
            + ':host([direction="up"]), :host([direction="down"]) { overflow: initial; overflow-y: hidden; white-space: initial; }';
        shadow.appendChild(style);

        var mover = document.createElement('div');
        shadow.appendChild(mover);

        mover.appendChild(document.createElement('content'));

        this.loopCount_ = 0;
        this.mover_ = mover;
        this.player_ = null;
        this.continueCallback_ = null;
    };

    HTMLMarqueeElementPrototype.attachedCallback = function() {
        for (var i = 0; i < kPresentationalAttributes.length; ++i) {
            this.initializeAttribute_(kPresentationalAttributes[i]);
        }

        this.start();
    };

    HTMLMarqueeElementPrototype.detachedCallback = function() {
        this.stop();
    };

    HTMLMarqueeElementPrototype.attributeChangedCallback = function(name, oldValue, newValue) {
        switch (name) {
        case 'bgcolor':
            this.style.backgroundColor = newValue;
            break;
        case 'height':
            this.style.height = convertHTMLLengthToCSSLength(newValue);
            break;
        case 'hspace':
            var margin = convertHTMLLengthToCSSLength(newValue);
            this.style.marginLeft = margin;
            this.style.marginRight = margin;
            break;
        case 'vspace':
            var margin = convertHTMLLengthToCSSLength(newValue);
            this.style.marginTop = margin;
            this.style.marginBottom = margin;
            break;
        case 'width':
            this.style.width = convertHTMLLengthToCSSLength(newValue);
            break;
        case 'behavior':
        case 'direction':
        case 'loop':
        case 'scrollAmount':
        case 'scrollDelay':
        case 'trueSpeed':
            // FIXME: Not implemented.
            break;
        }
    };

    HTMLMarqueeElementPrototype.initializeAttribute_ = function(name) {
        var value = this.getAttribute(name);
        if (value === null)
            return;
        this.attributeChangedCallback(name, null, value);
    };

    Object.defineProperty(HTMLMarqueeElementPrototype, 'scrollAmount', {
        get: function() {
            var value = this.getAttribute('scrollamount');
            var scrollAmount = convertToLong(value);
            if (isNaN(scrollAmount) || scrollAmount < 0)
                return kDefaultScrollAmount;
            return scrollAmount;
        },
        set: function(value) {
            if (value < 0)
                privateScriptController.throwException(privateScriptController.DOMException.IndexSizeError, "The provided value (" + value + ") is negative.");
            this.setAttribute('scrollamount', value);
        },
    });

    Object.defineProperty(HTMLMarqueeElementPrototype, 'scrollDelay', {
        get: function() {
            var value = this.getAttribute('scrolldelay');
            var scrollDelay = convertToLong(value);
            if (isNaN(scrollDelay) || scrollDelay < 0)
                return kDefaultScrollDelayMS;
            return scrollDelay;
        },
        set: function(value) {
            if (value < 0)
                privateScriptController.throwException(privateScriptController.DOMException.IndexSizeError, "The provided value (" + value + ") is negative.");
            this.setAttribute('scrolldelay', value);
        },
    });

    Object.defineProperty(HTMLMarqueeElementPrototype, 'loop', {
        get: function() {
            var value = this.getAttribute('loop');
            var loop = convertToLong(value);
            if (isNaN(loop) || loop <= 0)
                return kDefaultLoopLimit;
            return loop;
        },
        set: function(value) {
            if (value <= 0 && value != -1)
                privateScriptController.throwException(privateScriptController.DOMException.IndexSizeError, "The provided value (" + value + ") is neither positive nor -1.");
            this.setAttribute('loop', value);
        },
    });

    HTMLMarqueeElementPrototype.getGetMetrics_ = function() {
        var direction = this.direction.toLowerCase();
        if (direction === 'up' || direction === 'down')
            this.mover_.style.height = '-webkit-max-content';
        else
            this.mover_.style.width = '-webkit-max-content';

        var moverStyle = getComputedStyle(this.mover_);
        var marqueeStyle = getComputedStyle(this);

        var metrics = {};
        metrics.contentWidth = parseInt(moverStyle.width);
        metrics.contentHeight = parseInt(moverStyle.height);
        metrics.marqueeWidth = parseInt(marqueeStyle.width);
        metrics.marqueeHeight = parseInt(marqueeStyle.height);

        if (direction === 'up' || direction === 'down')
            this.mover_.style.height = '';
        else
            this.mover_.style.width = '';
        return metrics;
    };

    HTMLMarqueeElementPrototype.getAnimationParameters_ = function() {
        var metrics = this.getGetMetrics_();

        var totalWidth = metrics.marqueeWidth + metrics.contentWidth;
        var totalHeight = metrics.marqueeHeight + metrics.contentHeight;

        var innerWidth = metrics.marqueeWidth - metrics.contentWidth;
        var innerHeight = metrics.marqueeHeight - metrics.contentHeight;

        var parameters = {};
        var direction = this.direction.toLowerCase();

        switch (this.behavior) {
        case kBehaviorScroll:
        default:
            switch (direction) {
            case kDirectionLeft:
            default:
                parameters.transformBegin = 'translateX(' + metrics.marqueeWidth + 'px)';
                parameters.transformEnd = 'translateX(-' + metrics.contentWidth + 'px)';
                parameters.distance = totalWidth;
                break;
            case kDirectionRight:
                parameters.transformBegin = 'translateX(-' + metrics.contentWidth + 'px)';
                parameters.transformEnd = 'translateX(' + metrics.marqueeWidth + 'px)';
                parameters.distance = totalWidth;
                break;
            case kDirectionUp:
                parameters.transformBegin = 'translateY(' + metrics.marqueeHeight + 'px)';
                parameters.transformEnd = 'translateY(-' + metrics.contentHeight + 'px)';
                parameters.distance = totalHeight;
                break;
            case kDirectionDown:
                parameters.transformBegin = 'translateY(-' + metrics.contentHeight + 'px)';
                parameters.transformEnd = 'translateY(' + metrics.marqueeHeight + 'px)';
                parameters.distance = totalHeight;
                break;
            }
            break;
        case kBehaviorAlternate:
            switch (direction) {
            case kDirectionLeft:
            default:
                parameters.transformBegin = 'translateX(' + (innerWidth >= 0 ? innerWidth : 0) + 'px)';
                parameters.transformEnd = 'translateX(' + (innerWidth >= 0 ? 0 : innerWidth) + 'px)';
                parameters.distance = Math.abs(innerWidth);
                break;
            case kDirectionRight:
                parameters.transformBegin = 'translateX(' + (innerWidth >= 0 ? 0 : innerWidth) + 'px)';
                parameters.transformEnd = 'translateX(' + (innerWidth >= 0 ? innerWidth : 0) + 'px)';
                parameters.distance = Math.abs(innerWidth);
                break;
            case kDirectionUp:
                parameters.transformBegin = 'translateY(' + (innerHeight >= 0 ? innerHeight : 0) + 'px)';
                parameters.transformEnd = 'translateY(' + (innerHeight >= 0 ? 0 : innerHeight) + 'px)';
                parameters.distance = Math.abs(innerHeight);
                break;
            case kDirectionDown:
                parameters.transformBegin = 'translateY(' + (innerHeight >= 0 ? 0 : innerHeight) + 'px)';
                parameters.transformEnd = 'translateY(' + (innerHeight >= 0 ? innerHeight : 0) + 'px)';
                parameters.distance = Math.abs(innerHeight);
                break;
            }

            if (this.loopCount_ % 2) {
                var transform = parameters.transformBegin;
                parameters.transformBegin = parameters.transformEnd;
                parameters.transformEnd = transform;
            }

            break;
        case kBehaviorSlide:
            switch (direction) {
            case kDirectionLeft:
            default:
                parameters.transformBegin = 'translateX(' + metrics.marqueeWidth + 'px)';
                parameters.transformEnd = 'translateX(0)';
                parameters.distance = metrics.marqueeWidth;
                break;
            case kDirectionRight:
                parameters.transformBegin = 'translateX(-' + metrics.contentWidth + 'px)';
                parameters.transformEnd = 'translateX(' + innerWidth + 'px)';
                parameters.distance = metrics.marqueeWidth;
                break;
            case kDirectionUp:
                parameters.transformBegin = 'translateY(' + metrics.marqueeHeight + 'px)';
                parameters.transformEnd = 'translateY(0)';
                parameters.distance = metrics.marqueeHeight;
                break;
            case kDirectionDown:
                parameters.transformBegin = 'translateY(-' + metrics.contentHeight + 'px)';
                parameters.transformEnd = 'translateY(' + innerHeight + 'px)';
                parameters.distance = metrics.marqueeHeight;
                break;
            }
            break;
        }

        return parameters
    };

    function animationFinished_(event) {
        var player = event.target;
        var marquee = player.marquee_;
        marquee.loopCount_++;
        marquee.start();
    };

    HTMLMarqueeElementPrototype.shouldContinue_ = function() {
        var loop = this.loop;

        // By default, slide loops only once.
        if (loop <= 0 && this.behavior === kBehaviorSlide)
            loop = 1;

        if (loop <= 0)
            return true;
        return this.loopCount_ < loop;
    };

    HTMLMarqueeElementPrototype.continue_ = function() {
        if (!this.shouldContinue_()) {
            return;
        }

        if (this.player_ && this.player_.playState === 'paused') {
            this.player_.play();
            return;
        }

        var parameters = this.getAnimationParameters_();
        var scrollDelay = this.scrollDelay;
        if (scrollDelay < kMinimumScrollDelayMS && !this.trueSpeed)
            scrollDelay = kDefaultScrollDelayMS;
        var player = this.mover_.animate([
            { transform: parameters.transformBegin },
            { transform: parameters.transformEnd },
        ], {
            duration: this.scrollAmount == 0 ? 0 : parameters.distance * scrollDelay / this.scrollAmount,
            fill: 'forwards',
        });
        player.marquee_ = this;
        player.onfinish = animationFinished_;

        this.player_ = player;
    };

    HTMLMarqueeElementPrototype.start = function() {
        if (this.continueCallback_)
            return;
        this.continueCallback_ = requestAnimationFrame(function() {
            this.continueCallback_ = null;
            this.continue_();
        }.bind(this));
    };

    HTMLMarqueeElementPrototype.stop = function() {
        if (this.continueCallback_) {
            cancelAnimationFrame(this.continueCallback_);
            this.continueCallback_ = null;
            return;
        }

        if (this.player_) {
            this.player_.pause();
        }
    };
});
// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

"use strict";

function PrivateScriptController()
{
    this._installedClasses = {};
    this._DOMException = {};
    this._JSError = {};
    // This list must be in sync with the enum in ExceptionCode.h. The order matters.
    var domExceptions = [
        "IndexSizeError",
        "HierarchyRequestError",
        "WrongDocumentError",
        "InvalidCharacterError",
        "NoModificationAllowedError",
        "NotFoundError",
        "NotSupportedError",
        "InUseAttributeError", // Historical. Only used in setAttributeNode etc which have been removed from the DOM specs.

        // Introduced in DOM Level 2:
        "InvalidStateError",
        "SyntaxError",
        "InvalidModificationError",
        "NamespaceError",
        "InvalidAccessError",

        // Introduced in DOM Level 3:
        "TypeMismatchError", // Historical; use TypeError instead

        // XMLHttpRequest extension:
        "SecurityError",

        // Others introduced in HTML5:
        "NetworkError",
        "AbortError",
        "URLMismatchError",
        "QuotaExceededError",
        "TimeoutError",
        "InvalidNodeTypeError",
        "DataCloneError",

        // These are IDB-specific.
        "UnknownError",
        "ConstraintError",
        "DataError",
        "TransactionInactiveError",
        "ReadOnlyError",
        "VersionError",

        // File system
        "NotReadableError",
        "EncodingError",
        "PathExistsError",

        // SQL
        "SQLDatabaseError", // Naming conflict with DatabaseError class.

        // Web Crypto
        "OperationError",

        // Push API
        "PermissionDeniedError",

        // Pointer Events
        "InvalidPointerId",
    ];

    // This list must be in sync with the enum in ExceptionCode.h. The order matters.
    var jsErrors = [
        "Error",
        "TypeError",
        "RangeError",
        "SyntaxError",
        "ReferenceError",
    ];

    var code = 1;
    domExceptions.forEach(function (exception) {
        this._DOMException[exception] = code;
        ++code;
    }.bind(this));

    var code = 1000;
    jsErrors.forEach(function (exception) {
        this._JSError[exception] = code;
        ++code;
    }.bind(this));
}

PrivateScriptController.prototype = {
    get installedClasses()
    {
        return this._installedClasses;
    },

    get DOMException()
    {
        return this._DOMException;
    },

    get JSError()
    {
        return this._JSError;
    },

    installClass: function(className, implementation)
    {
        function PrivateScriptClass()
        {
        }

        if (!(className in this._installedClasses))
            this._installedClasses[className] = new PrivateScriptClass();
        implementation(this._installedClasses[className]);
    },

    // Private scripts can throw JS errors and DOM exceptions as follows:
    //     throwException(privateScriptController.DOMException.IndexSizeError, "...");
    //     throwException(privateScriptController.JSError.TypeError, "...");
    //
    // Note that normal JS errors thrown by private scripts are treated
    // as real JS errors caused by programming mistake and the execution crashes.
    // If you want to intentially throw JS errors from private scripts,
    // you need to use throwException(privateScriptController.JSError.TypeError, "...").
    throwException: function(code, message)
    {
        function PrivateScriptException()
        {
        }

        var exception = new PrivateScriptException();
        exception.code = code;
        exception.message = message;
        exception.name = "PrivateScriptException";
        throw exception;
    },
}

if (typeof window.privateScriptController === 'undefined')
    window.privateScriptController = new PrivateScriptController();

// This line must be the last statement of this JS file.
// A parenthesis is needed, because the caller of this script (PrivateScriptRunner.cpp)
// is depending on the completion value of this script.
(privateScriptController.installedClasses);
"use strict";
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @param {!string} id
 */
function $(id) {
    return document.getElementById(id);
}

/**
 * @param {!string} tagName
 * @param {string=} opt_class
 * @param {string=} opt_text
 * @return {!Element}
 */
function createElement(tagName, opt_class, opt_text) {
    var element = document.createElement(tagName);
    if (opt_class)
        element.setAttribute("class", opt_class);
    if (opt_text)
        element.appendChild(document.createTextNode(opt_text));
    return element;
}

/**
 * @constructor
 * @param {!number|Rectangle|Object} xOrRect
 * @param {!number} y
 * @param {!number} width
 * @param {!number} height
 */
function Rectangle(xOrRect, y, width, height) {
    if (typeof xOrRect === "object") {
        y = xOrRect.y;
        width = xOrRect.width;
        height = xOrRect.height;
        xOrRect = xOrRect.x;
    }
    this.x = xOrRect;
    this.y = y;
    this.width = width;
    this.height = height;
}

Rectangle.prototype = {
    get maxX() { return this.x + this.width; },
    get maxY() { return this.y + this.height; },
    toString: function() { return "Rectangle(" + this.x + "," + this.y + "," + this.width + "," + this.height + ")"; }
};

/**
 * @param {!Rectangle} rect1
 * @param {!Rectangle} rect2
 * @return {?Rectangle}
 */
Rectangle.intersection = function(rect1, rect2) {
    var x = Math.max(rect1.x, rect2.x);
    var maxX = Math.min(rect1.maxX, rect2.maxX);
    var y = Math.max(rect1.y, rect2.y);
    var maxY = Math.min(rect1.maxY, rect2.maxY);
    var width = maxX - x;
    var height = maxY - y;
    if (width < 0 || height < 0)
        return null;
    return new Rectangle(x, y, width, height);
};

/**
 * @param {!number} width in CSS pixel
 * @param {!number} height in CSS pixel
 */
function resizeWindow(width, height) {
    var zoom = global.params.zoomFactor ? global.params.zoomFactor : 1;
    setWindowRect(adjustWindowRect(width * zoom, height * zoom, width * zoom, height * zoom));
}

/**
 * @param {!number} width in physical pixel
 * @param {!number} height in physical pixel
 * @param {?number} minWidth in physical pixel
 * @param {?number} minHeight in physical pixel
 * @return {!Rectangle} Adjusted rectangle with physical pixels
 */
function adjustWindowRect(width, height, minWidth, minHeight) {
    if (typeof minWidth !== "number")
        minWidth = 0;
    if (typeof minHeight !== "number")
        minHeight = 0;

    var windowRect = new Rectangle(0, 0, Math.ceil(width), Math.ceil(height));

    if (!global.params.anchorRectInScreen)
        return windowRect;

    var anchorRect = new Rectangle(global.params.anchorRectInScreen);
    var availRect = new Rectangle(window.screen.availLeft, window.screen.availTop, window.screen.availWidth, window.screen.availHeight);

    _adjustWindowRectVertically(windowRect, availRect, anchorRect, minHeight);
    _adjustWindowRectHorizontally(windowRect, availRect, anchorRect, minWidth);

    return windowRect;
}

/**
 * Arguments are physical pixels.
 */
function _adjustWindowRectVertically(windowRect, availRect, anchorRect, minHeight) {
    var availableSpaceAbove = anchorRect.y - availRect.y;
    availableSpaceAbove = Math.max(0, Math.min(availRect.height, availableSpaceAbove));

    var availableSpaceBelow = availRect.maxY - anchorRect.maxY;
    availableSpaceBelow = Math.max(0, Math.min(availRect.height, availableSpaceBelow));
    if (windowRect.height > availableSpaceBelow && availableSpaceBelow < availableSpaceAbove) {
        windowRect.height = Math.min(windowRect.height, availableSpaceAbove);
        windowRect.height = Math.max(windowRect.height, minHeight);
        windowRect.y = anchorRect.y - windowRect.height;
    } else {
        windowRect.height = Math.min(windowRect.height, availableSpaceBelow);
        windowRect.height = Math.max(windowRect.height, minHeight);
        windowRect.y = anchorRect.maxY;
    }
}

/**
 * Arguments are physical pixels.
 */
function _adjustWindowRectHorizontally(windowRect, availRect, anchorRect, minWidth) {
    windowRect.width = Math.min(windowRect.width, availRect.width);
    windowRect.width = Math.max(windowRect.width, minWidth);
    windowRect.x = anchorRect.x;
    // If we are getting clipped, we want to switch alignment to the right side
    // of the anchor rect as long as doing so will make the popup not clipped.
    var rightAlignedX = windowRect.x + anchorRect.width - windowRect.width;
    if (rightAlignedX >= availRect.x && (windowRect.maxX > availRect.maxX || global.params.isRTL))
        windowRect.x = rightAlignedX;
}

/**
 * @param {!Rectangle} rect Window position and size with physical pixels.
 */
function setWindowRect(rect) {
    if (window.frameElement) {
        window.frameElement.style.width = rect.width + "px";
        window.frameElement.style.height = rect.height + "px";
    } else {
        window.pagePopupController.setWindowRect(rect.x, rect.y, rect.width, rect.height);
    }
}

function hideWindow() {
    setWindowRect(adjustWindowRect(1, 1, 1, 1));
}

/**
 * @return {!boolean}
 */
function isWindowHidden() {
    // window.innerWidth and innerHeight are zoom-adjusted values.  If we call
    // setWindowRect with width=100 and the zoom-level is 2.0, innerWidth will
    // return 50.
    return window.innerWidth <= 1 && window.innerHeight <= 1;
}

window.addEventListener("resize", function() {
    if (isWindowHidden())
        window.dispatchEvent(new CustomEvent("didHide"));
    else
        window.dispatchEvent(new CustomEvent("didOpenPicker"));
}, false);

/**
 * @return {!number}
 */
function getScrollbarWidth() {
    if (typeof window.scrollbarWidth === "undefined") {
        var scrollDiv = document.createElement("div");
        scrollDiv.style.opacity = "0";
        scrollDiv.style.overflow = "scroll";
        scrollDiv.style.width = "50px";
        scrollDiv.style.height = "50px";
        document.body.appendChild(scrollDiv);
        window.scrollbarWidth = scrollDiv.offsetWidth - scrollDiv.clientWidth;
        scrollDiv.parentNode.removeChild(scrollDiv);
    }
    return window.scrollbarWidth;
}

/**
 * @param {!string} className
 * @return {?Element}
 */
function enclosingNodeOrSelfWithClass(selfNode, className)
{
    for (var node = selfNode; node && node !== selfNode.ownerDocument; node = node.parentNode) {
        if (node.nodeType === Node.ELEMENT_NODE && node.classList.contains(className))
            return node;
    }
    return null;
}

/**
 * @constructor
 */
function EventEmitter() {
};

/**
 * @param {!string} type
 * @param {!function({...*})} callback
 */
EventEmitter.prototype.on = function(type, callback) {
    console.assert(callback instanceof Function);
    if (!this._callbacks)
        this._callbacks = {};
    if (!this._callbacks[type])
        this._callbacks[type] = [];
    this._callbacks[type].push(callback);
};

EventEmitter.prototype.hasListener = function(type) {
    if (!this._callbacks)
        return false;
    var callbacksForType = this._callbacks[type];
    if (!callbacksForType)
        return false;
    return callbacksForType.length > 0;
};

/**
 * @param {!string} type
 * @param {!function(Object)} callback
 */
EventEmitter.prototype.removeListener = function(type, callback) {
    if (!this._callbacks)
        return;
    var callbacksForType = this._callbacks[type];
    if (!callbacksForType)
        return;
    callbacksForType.splice(callbacksForType.indexOf(callback), 1);
    if (callbacksForType.length === 0)
        delete this._callbacks[type];
};

/**
 * @param {!string} type
 * @param {...*} var_args
 */
EventEmitter.prototype.dispatchEvent = function(type) {
    if (!this._callbacks)
        return;
    var callbacksForType = this._callbacks[type];
    if (!callbacksForType)
        return;
    callbacksForType = callbacksForType.slice(0);
    for (var i = 0; i < callbacksForType.length; ++i) {
        callbacksForType[i].apply(this, Array.prototype.slice.call(arguments, 1));
    }
};

/**
 * @constructor
 * @extends EventEmitter
 * @param {!Element} element
 * @param {!Object} config
 */
function Picker(element, config) {
    this._element = element;
    this._config = config;
}

Picker.prototype = Object.create(EventEmitter.prototype);

/**
 * @enum {number}
 */
Picker.Actions = {
    SetValue: 0,
    Cancel: -1,
    ChooseOtherColor: -2
};

/**
 * @param {!string} value
 */
Picker.prototype.submitValue = function(value) {
    window.pagePopupController.setValue(value);
    window.pagePopupController.closePopup();
};

Picker.prototype.handleCancel = function() {
    window.pagePopupController.closePopup();
};

Picker.prototype.chooseOtherColor = function() {
    window.pagePopupController.setValueAndClosePopup(Picker.Actions.ChooseOtherColor, "");
};

Picker.prototype.cleanup = function() {};

window.addEventListener("keyup", function(event) {
    // JAWS dispatches extra Alt events and unless we handle them they move the
    // focus and close the popup.
    if (event.keyIdentifier === "Alt")
        event.preventDefault();
}, true);
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

body {
    -webkit-user-select: none;
    background-color: white;
    font: -webkit-small-control;
    margin: 0;
    overflow: hidden;
}

.rtl {
    direction: rtl;
}
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

body {
    -webkit-user-select: none;
    background-color: white;
    font: -webkit-control;
    font-size: 12px;
}

.rtl {
    direction: rtl;
}

.scroll-view {
    overflow: hidden;
    width: 0;
    height: 0;
}

.list-cell {
    position: absolute;
    left: 0;
    top: 0;
    width: 0;
    height: 0;
}

.list-cell.hidden {
    display: none;
}

.week-number-cell,
.day-cell {
    position: static;
    text-align: center;
    box-sizing: border-box;
    display: inline-block;
    cursor: default;
    transition: color 1s;
    padding: 1px;
}

.week-number-cell {
    box-sizing: border-box;
    color: black;
    padding-right: 0;
    box-shadow: 1px 0 0 #bfbfbf;
    margin-right: 1px;
}

.day-cell {
    color: #bfbfbf;
}

.day-cell.highlighted.today,
.day-cell.today {
    border: 1px solid #bfbfbf;
    padding: 0;
}

.week-number-cell.highlighted,
.day-cell.highlighted {
    background-color: #e5ecf8;
}

.week-number-cell.highlighted.disabled,
.day-cell.highlighted.disabled {
    border: 1px solid #e5ecf8;
    padding: 0;
}

.week-number-cell.selected,
.day-cell.selected {
    background-color: #bccdec;
}

.week-number-cell.disabled,
.day-cell.disabled {
    background-color: #f5f5f5;
}

.day-cell.current-month {
    color: #000000;
}

.calendar-table-view {
    border: 1px solid #bfbfbf;
    outline: none;
}

.week-number-label,
.week-day-label {
    text-align: center;
    display: inline-block;
    line-height: 23px;
    padding-top: 1px;
    box-sizing: padding-box;
}

.week-number-label {
    box-sizing: border-box;
    border-right: 1px solid #bfbfbf;
}

.calendar-table-header-view {
    background-color: #f5f5f5;
    border-bottom: 1px solid #bfbfbf;
    height: 24px;
}

.calendar-picker {
    border: 1px solid #bfbfbf;
    border-radius: 2px;
    position: absolute;
    padding: 10px;
    background-color: white;
    overflow: hidden;
    cursor: default;
}

.calendar-header-view {
    margin-bottom: 10px;
    display: flex;
    flex-flow: row;
}

.calendar-title {
    -webkit-align-self: center;
    flex: 1;
    text-align: left;
}

.rtl .calendar-title {
    text-align: right;
}

.month-popup-button,
.month-popup-button:hover,
.month-popup-button:disabled {
    background-color: transparent !important;
    background-image: none !important;
    box-shadow: none !important;
    color: black;
}

.month-popup-button:disabled {
    opacity: 0.7;
}

.month-popup-button {
    font-size: 12px;
    padding: 4px;
    display: inline-block;
    cursor: default;
    border: 1px solid transparent !important;
    height: 24px !important;
}

.month-popup-button .disclosure-triangle {
    margin: 0 6px;
}

.month-popup-button .disclosure-triangle svg {
    padding-bottom: 2px;
}

.today-button::after {
    content: "";
    display: block;
    border-radius: 3px;
    width: 6px;
    height: 6px;
    background-color: #6e6e6e;
    margin: 0 auto;
}

.calendar-navigation-button {
    -webkit-align-self: center;
    width: 24px;
    height: 24px;
    min-width: 0 !important;
    padding-left: 0 !important;
    padding-right: 0 !important;
    -webkit-margin-start: 4px !important;
}

.year-list-view {
    border: 1px solid #bfbfbf;
    background-color: white;
    position: absolute;
}

.year-list-cell {
    box-sizing: border-box;
    border-bottom: 1px solid #bfbfbf;
    background-color: white;
    overflow: hidden;
}

.year-list-cell .label {
    height: 24px;
    line-height: 24px;
    -webkit-padding-start: 8px;
    background-color: #f5f5f5;
    border-bottom: 1px solid #bfbfbf;
}

.year-list-cell .month-chooser {
    padding: 0;
}

.month-buttons-row {
    display: flex;
}

.month-button {
    flex: 1;
    height: 32px;
    line-height: 32px;
    padding: 0 !important;
    margin: 0 !important;
    background-image: none !important;
    background-color: #ffffff;
    border-width: 0 !important;
    box-shadow: none !important;
    text-align: center;
}

.month-button.highlighted {
    background-color: #e5ecf8;
}

.month-button[aria-disabled="true"] {
    color: GrayText;
}

.scrubby-scroll-bar {
    width: 14px;
    height: 60px;
    background-color: white;
    border-left: 1px solid #bfbfbf;
    position: absolute;
    top: 0;
}

.scrubby-scroll-thumb {
    width: 10px;
    margin: 2px;
    height: 30px;
    background-color: #d8d8d8;
    position: absolute;
    left: 0;
    top: 0;
}

.month-popup-view {
    position: absolute;
    top: 0;
    right: 0;
    bottom: 0;
    left: 0;
}

.year-list-view .scrubby-scroll-bar {
    right: 0;
}

.rtl .year-list-view .scrubby-scroll-bar {
    left: 0;
    right: auto;
    border-left-width: 0;
    border-right: 1px solid #bfbfbf;
}

.year-month-button {
    width: 24px;
    height: 24px;
    min-width: 0;
    padding: 0;
}

.month-popup-button:focus,
.year-list-view:focus,
.calendar-table-view:focus {
    transition: border-color 200ms;
    /* We use border color because it follows the border radius (unlike outline).
    * This is particularly noticeable on mac. */
    border-color: rgb(77, 144, 254) !important;
    outline: none;
}

.preparing button:focus,
.preparing .year-list-view:focus,
.preparing .calendar-table-view:focus {
    transition: none;
}
"use strict";
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


/**
 * @enum {number}
 */
var WeekDay = {
    Sunday: 0,
    Monday: 1,
    Tuesday: 2,
    Wednesday: 3,
    Thursday: 4,
    Friday: 5,
    Saturday: 6
};

/**
 * @type {Object}
 */
var global = {
    picker: null,
    params: {
        locale: "en-US",
        weekStartDay: WeekDay.Sunday,
        dayLabels: ["S", "M", "T", "W", "T", "F", "S"],
        shortMonthLabels: ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sept", "Oct", "Nov", "Dec"],
        isLocaleRTL: false,
        mode: "date",
        weekLabel: "Week",
        anchorRectInScreen: new Rectangle(0, 0, 0, 0),
        currentValue: null
    }
};

// ----------------------------------------------------------------
// Utility functions

/**
 * @return {!boolean}
 */
function hasInaccuratePointingDevice() {
    return matchMedia("(pointer: coarse)").matches;
}

/**
 * @return {!string} lowercase locale name. e.g. "en-us"
 */
function getLocale() {
    return (global.params.locale || "en-us").toLowerCase();
}

/**
 * @return {!string} lowercase language code. e.g. "en"
 */
function getLanguage() {
    var locale = getLocale();
    var result = locale.match(/^([a-z]+)/);
    if (!result)
        return "en";
    return result[1];
}

/**
 * @param {!number} number
 * @return {!string}
 */
function localizeNumber(number) {
    return window.pagePopupController.localizeNumberString(number);
}

/**
 * @const
 * @type {number}
 */
var ImperialEraLimit = 2087;

/**
 * @param {!number} year
 * @param {!number} month
 * @return {!string}
 */
function formatJapaneseImperialEra(year, month) {
    // We don't show an imperial era if it is greater than 99 becase of space
    // limitation.
    if (year > ImperialEraLimit)
        return "";
    if (year > 1989)
        return "(\u5e73\u6210" + localizeNumber(year - 1988) + "\u5e74)";
    if (year == 1989)
        return "(\u5e73\u6210\u5143\u5e74)";
    if (year >= 1927)
        return "(\u662d\u548c" + localizeNumber(year - 1925) + "\u5e74)";
    if (year > 1912)
        return "(\u5927\u6b63" + localizeNumber(year - 1911) + "\u5e74)";
    if (year == 1912 && month >= 7)
        return "(\u5927\u6b63\u5143\u5e74)";
    if (year > 1868)
        return "(\u660e\u6cbb" + localizeNumber(year - 1867) + "\u5e74)";
    if (year == 1868)
        return "(\u660e\u6cbb\u5143\u5e74)";
    return "";
}

function createUTCDate(year, month, date) {
    var newDate = new Date(0);
    newDate.setUTCFullYear(year);
    newDate.setUTCMonth(month);
    newDate.setUTCDate(date);
    return newDate;
}

/**
 * @param {string} dateString
 * @return {?Day|Week|Month}
 */
function parseDateString(dateString) {
    var month = Month.parse(dateString);
    if (month)
        return month;
    var week = Week.parse(dateString);
    if (week)
        return week;
    return Day.parse(dateString);
}

/**
 * @const
 * @type {number}
 */
var DaysPerWeek = 7;

/**
 * @const
 * @type {number}
 */
var MonthsPerYear = 12;

/**
 * @const
 * @type {number}
 */
var MillisecondsPerDay = 24 * 60 * 60 * 1000;

/**
 * @const
 * @type {number}
 */
var MillisecondsPerWeek = DaysPerWeek * MillisecondsPerDay;

/**
 * @constructor
 */
function DateType() {
}

/**
 * @constructor
 * @extends DateType
 * @param {!number} year
 * @param {!number} month
 * @param {!number} date
 */
function Day(year, month, date) {
    var dateObject = createUTCDate(year, month, date);
    if (isNaN(dateObject.valueOf()))
        throw "Invalid date";
    /**
     * @type {number}
     * @const
     */
    this.year = dateObject.getUTCFullYear();   
     /**
     * @type {number}
     * @const
     */  
    this.month = dateObject.getUTCMonth();
    /**
     * @type {number}
     * @const
     */
    this.date = dateObject.getUTCDate();
};

Day.prototype = Object.create(DateType.prototype);

Day.ISOStringRegExp = /^(\d+)-(\d+)-(\d+)/;

/**
 * @param {!string} str
 * @return {?Day}
 */
Day.parse = function(str) {
    var match = Day.ISOStringRegExp.exec(str);
    if (!match)
        return null;
    var year = parseInt(match[1], 10);
    var month = parseInt(match[2], 10) - 1;
    var date = parseInt(match[3], 10);
    return new Day(year, month, date);
};

/**
 * @param {!number} value
 * @return {!Day}
 */
Day.createFromValue = function(millisecondsSinceEpoch) {
    return Day.createFromDate(new Date(millisecondsSinceEpoch))
};

/**
 * @param {!Date} date
 * @return {!Day}
 */
Day.createFromDate = function(date) {
    if (isNaN(date.valueOf()))
        throw "Invalid date";
    return new Day(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate());
};

/**
 * @param {!Day} day
 * @return {!Day}
 */
Day.createFromDay = function(day) {
    return day;
};

/**
 * @return {!Day}
 */
Day.createFromToday = function() {
    var now = new Date();
    return new Day(now.getFullYear(), now.getMonth(), now.getDate());
};

/**
 * @param {!DateType} other
 * @return {!boolean}
 */
Day.prototype.equals = function(other) {
    return other instanceof Day && this.year === other.year && this.month === other.month && this.date === other.date;
};

/**
 * @param {!number=} offset
 * @return {!Day}
 */
Day.prototype.previous = function(offset) {
    if (typeof offset === "undefined")
        offset = 1;
    return new Day(this.year, this.month, this.date - offset);
};

/**
 * @param {!number=} offset
 * @return {!Day}
 */
Day.prototype.next = function(offset) {
 if (typeof offset === "undefined")
     offset = 1;
    return new Day(this.year, this.month, this.date + offset);
};

/**
 * @return {!Date}
 */
Day.prototype.startDate = function() {
    return createUTCDate(this.year, this.month, this.date);
};

/**
 * @return {!Date}
 */
Day.prototype.endDate = function() {
    return createUTCDate(this.year, this.month, this.date + 1);
};

/**
 * @return {!Day}
 */
Day.prototype.firstDay = function() {
    return this;
};

/**
 * @return {!Day}
 */
Day.prototype.middleDay = function() {
    return this;
};

/**
 * @return {!Day}
 */
Day.prototype.lastDay = function() {
    return this;
};

/**
 * @return {!number}
 */
Day.prototype.valueOf = function() {
    return createUTCDate(this.year, this.month, this.date).getTime();
};

/**
 * @return {!WeekDay}
 */
Day.prototype.weekDay = function() {
    return createUTCDate(this.year, this.month, this.date).getUTCDay();
};

/**
 * @return {!string}
 */
Day.prototype.toString = function() {
    var yearString = String(this.year);
    if (yearString.length < 4)
        yearString = ("000" + yearString).substr(-4, 4);
    return yearString + "-" + ("0" + (this.month + 1)).substr(-2, 2) + "-" + ("0" + this.date).substr(-2, 2);
};

/**
 * @return {!string}
 */
Day.prototype.format = function() {
    if (!Day.formatter) {
        Day.formatter = new Intl.DateTimeFormat(getLocale(), {
            weekday: "long", year: "numeric", month: "long", day: "numeric", timeZone: "UTC"
        });
    }
    return Day.formatter.format(this.startDate());
};

// See WebCore/platform/DateComponents.h.
Day.Minimum = Day.createFromValue(-62135596800000.0);
Day.Maximum = Day.createFromValue(8640000000000000.0);

// See WebCore/html/DayInputType.cpp.
Day.DefaultStep = 86400000;
Day.DefaultStepBase = 0;

/**
 * @constructor
 * @extends DateType
 * @param {!number} year
 * @param {!number} week
 */
function Week(year, week) { 
    /**
     * @type {number}
     * @const
     */
    this.year = year;
    /**
     * @type {number}
     * @const
     */
    this.week = week;
    // Number of years per year is either 52 or 53.
    if (this.week < 1 || (this.week > 52 && this.week > Week.numberOfWeeksInYear(this.year))) {
        var normalizedWeek = Week.createFromDay(this.firstDay());
        this.year = normalizedWeek.year;
        this.week = normalizedWeek.week;
    }
}

Week.ISOStringRegExp = /^(\d+)-[wW](\d+)$/;

// See WebCore/platform/DateComponents.h.
Week.Minimum = new Week(1, 1);
Week.Maximum = new Week(275760, 37);

// See WebCore/html/WeekInputType.cpp.
Week.DefaultStep = 604800000;
Week.DefaultStepBase = -259200000;

Week.EpochWeekDay = createUTCDate(1970, 0, 0).getUTCDay();

/**
 * @param {!string} str
 * @return {?Week}
 */
Week.parse = function(str) {
    var match = Week.ISOStringRegExp.exec(str);
    if (!match)
        return null;
    var year = parseInt(match[1], 10);
    var week = parseInt(match[2], 10);
    return new Week(year, week);
};

/**
 * @param {!number} millisecondsSinceEpoch
 * @return {!Week}
 */
Week.createFromValue = function(millisecondsSinceEpoch) {
    return Week.createFromDate(new Date(millisecondsSinceEpoch))
};

/**
 * @param {!Date} date
 * @return {!Week}
 */
Week.createFromDate = function(date) {
    if (isNaN(date.valueOf()))
        throw "Invalid date";
    var year = date.getUTCFullYear();
    if (year <= Week.Maximum.year && Week.weekOneStartDateForYear(year + 1).getTime() <= date.getTime())
        year++;
    else if (year > 1 && Week.weekOneStartDateForYear(year).getTime() > date.getTime())
        year--;
    var week = 1 + Week._numberOfWeeksSinceDate(Week.weekOneStartDateForYear(year), date);
    return new Week(year, week);
};

/**
 * @param {!Day} day
 * @return {!Week}
 */
Week.createFromDay = function(day) {
    var year = day.year;
    if (year <= Week.Maximum.year && Week.weekOneStartDayForYear(year + 1) <= day)
        year++;
    else if (year > 1 && Week.weekOneStartDayForYear(year) > day)
        year--;
    var week = Math.floor(1 + (day.valueOf() - Week.weekOneStartDayForYear(year).valueOf()) / MillisecondsPerWeek);
    return new Week(year, week);
};

/**
 * @return {!Week}
 */
Week.createFromToday = function() {
    var now = new Date();
    return Week.createFromDate(createUTCDate(now.getFullYear(), now.getMonth(), now.getDate()));
};

/**
 * @param {!number} year
 * @return {!Date}
 */
Week.weekOneStartDateForYear = function(year) {
    if (year < 1)
        return createUTCDate(1, 0, 1);
    // The week containing January 4th is week one.
    var yearStartDay = createUTCDate(year, 0, 4).getUTCDay();
    return createUTCDate(year, 0, 4 - (yearStartDay + 6) % DaysPerWeek);
};

/**
 * @param {!number} year
 * @return {!Day}
 */
Week.weekOneStartDayForYear = function(year) {
    if (year < 1)
        return Day.Minimum;
    // The week containing January 4th is week one.
    var yearStartDay = createUTCDate(year, 0, 4).getUTCDay();
    return new Day(year, 0, 4 - (yearStartDay + 6) % DaysPerWeek);
};

/**
 * @param {!number} year
 * @return {!number}
 */
Week.numberOfWeeksInYear = function(year) {
    if (year < 1 || year > Week.Maximum.year)
        return 0;
    else if (year === Week.Maximum.year)
        return Week.Maximum.week;
    return Week._numberOfWeeksSinceDate(Week.weekOneStartDateForYear(year), Week.weekOneStartDateForYear(year + 1));
};

/**
 * @param {!Date} baseDate
 * @param {!Date} date
 * @return {!number}
 */
Week._numberOfWeeksSinceDate = function(baseDate, date) {
    return Math.floor((date.getTime() - baseDate.getTime()) / MillisecondsPerWeek);
};

/**
 * @param {!DateType} other
 * @return {!boolean}
 */
Week.prototype.equals = function(other) {
    return other instanceof Week && this.year === other.year && this.week === other.week;
};

/**
 * @param {!number=} offset
 * @return {!Week}
 */
Week.prototype.previous = function(offset) {
    if (typeof offset === "undefined")
        offset = 1;
    return new Week(this.year, this.week - offset);
};

/**
 * @param {!number=} offset
 * @return {!Week}
 */
Week.prototype.next = function(offset) {
    if (typeof offset === "undefined")
        offset = 1;
    return new Week(this.year, this.week + offset);
};

/**
 * @return {!Date}
 */
Week.prototype.startDate = function() {
    var weekStartDate = Week.weekOneStartDateForYear(this.year);
    weekStartDate.setUTCDate(weekStartDate.getUTCDate() + (this.week - 1) * 7);
    return weekStartDate;
};

/**
 * @return {!Date}
 */
Week.prototype.endDate = function() {
    if (this.equals(Week.Maximum))
        return Day.Maximum.startDate();
    return this.next().startDate();
};

/**
 * @return {!Day}
 */
Week.prototype.firstDay = function() {
    var weekOneStartDay = Week.weekOneStartDayForYear(this.year);
    return weekOneStartDay.next((this.week - 1) * DaysPerWeek);
};

/**
 * @return {!Day}
 */
Week.prototype.middleDay = function() {
    return this.firstDay().next(3);
};

/**
 * @return {!Day}
 */
Week.prototype.lastDay = function() {
    if (this.equals(Week.Maximum))
        return Day.Maximum;
    return this.next().firstDay().previous();
};

/**
 * @return {!number}
 */
Week.prototype.valueOf = function() {
    return this.firstDay().valueOf() - createUTCDate(1970, 0, 1).getTime();
};

/**
 * @return {!string}
 */
Week.prototype.toString = function() {
    var yearString = String(this.year);
    if (yearString.length < 4)
        yearString = ("000" + yearString).substr(-4, 4);
    return yearString + "-W" + ("0" + this.week).substr(-2, 2);
};

/**
 * @constructor
 * @extends DateType
 * @param {!number} year
 * @param {!number} month
 */
function Month(year, month) { 
    /**
     * @type {number}
     * @const
     */
    this.year = year + Math.floor(month / MonthsPerYear);
    /**
     * @type {number}
     * @const
     */
    this.month = month % MonthsPerYear < 0 ? month % MonthsPerYear + MonthsPerYear : month % MonthsPerYear;
};

Month.ISOStringRegExp = /^(\d+)-(\d+)$/;

// See WebCore/platform/DateComponents.h.
Month.Minimum = new Month(1, 0);
Month.Maximum = new Month(275760, 8);

// See WebCore/html/MonthInputType.cpp.
Month.DefaultStep = 1;
Month.DefaultStepBase = 0;

/**
 * @param {!string} str
 * @return {?Month}
 */
Month.parse = function(str) {
    var match = Month.ISOStringRegExp.exec(str);
    if (!match)
        return null;
    var year = parseInt(match[1], 10);
    var month = parseInt(match[2], 10) - 1;
    return new Month(year, month);
};

/**
 * @param {!number} value
 * @return {!Month}
 */
Month.createFromValue = function(monthsSinceEpoch) {
    return new Month(1970, monthsSinceEpoch)
};

/**
 * @param {!Date} date
 * @return {!Month}
 */
Month.createFromDate = function(date) {
    if (isNaN(date.valueOf()))
        throw "Invalid date";
    return new Month(date.getUTCFullYear(), date.getUTCMonth());
};

/**
 * @param {!Day} day
 * @return {!Month}
 */
Month.createFromDay = function(day) {
    return new Month(day.year, day.month);
};

/**
 * @return {!Month}
 */
Month.createFromToday = function() {
    var now = new Date();
    return new Month(now.getFullYear(), now.getMonth());
};

/**
 * @return {!boolean}
 */
Month.prototype.containsDay = function(day) {
    return this.year === day.year && this.month === day.month;
};

/**
 * @param {!Month} other
 * @return {!boolean}
 */
Month.prototype.equals = function(other) {
    return other instanceof Month && this.year === other.year && this.month === other.month;
};

/**
 * @param {!number=} offset
 * @return {!Month}
 */
Month.prototype.previous = function(offset) {
    if (typeof offset === "undefined")
        offset = 1;
    return new Month(this.year, this.month - offset);
};

/**
 * @param {!number=} offset
 * @return {!Month}
 */
Month.prototype.next = function(offset) {
    if (typeof offset === "undefined")
        offset = 1;
    return new Month(this.year, this.month + offset);
};

/**
 * @return {!Date}
 */
Month.prototype.startDate = function() {
    return createUTCDate(this.year, this.month, 1);
};

/**
 * @return {!Date}
 */
Month.prototype.endDate = function() {
    if (this.equals(Month.Maximum))
        return Day.Maximum.startDate();
    return this.next().startDate();
};

/**
 * @return {!Day}
 */
Month.prototype.firstDay = function() {
    return new Day(this.year, this.month, 1);
};

/**
 * @return {!Day}
 */
Month.prototype.middleDay = function() {
    return new Day(this.year, this.month, this.month === 2 ? 14 : 15);
};

/**
 * @return {!Day}
 */
Month.prototype.lastDay = function() {
    if (this.equals(Month.Maximum))
        return Day.Maximum;
    return this.next().firstDay().previous();
};

/**
 * @return {!number}
 */
Month.prototype.valueOf = function() {
    return (this.year - 1970) * MonthsPerYear + this.month;
};

/**
 * @return {!string}
 */
Month.prototype.toString = function() {
    var yearString = String(this.year);
    if (yearString.length < 4)
        yearString = ("000" + yearString).substr(-4, 4);
    return yearString + "-" + ("0" + (this.month + 1)).substr(-2, 2);
};

/**
 * @return {!string}
 */
Month.prototype.toLocaleString = function() {
    if (global.params.locale === "ja")
        return "" + this.year + "\u5e74" + formatJapaneseImperialEra(this.year, this.month) + " " + (this.month + 1) + "\u6708";
    return window.pagePopupController.formatMonth(this.year, this.month);
};

/**
 * @return {!string}
 */
Month.prototype.toShortLocaleString = function() {
    return window.pagePopupController.formatShortMonth(this.year, this.month);
};

// ----------------------------------------------------------------
// Initialization

/**
 * @param {Event} event
 */
function handleMessage(event) {
    if (global.argumentsReceived)
        return;
    global.argumentsReceived = true;
    initialize(JSON.parse(event.data));
}

/**
 * @param {!Object} params
 */
function setGlobalParams(params) {
    var name;
    for (name in global.params) {
        if (typeof params[name] === "undefined")
            console.warn("Missing argument: " + name);
    }
    for (name in params) {
        global.params[name] = params[name];
    }
};

/**
 * @param {!Object} args
 */
function initialize(args) { 
    setGlobalParams(args);
    if (global.params.suggestionValues && global.params.suggestionValues.length)
        openSuggestionPicker();
    else
        openCalendarPicker();
}

function closePicker() {
    if (global.picker)
        global.picker.cleanup();
    var main = $("main");
    main.innerHTML = "";
    main.className = "";
};

function openSuggestionPicker() {
    closePicker();
    global.picker = new SuggestionPicker($("main"), global.params);
};

function openCalendarPicker() {
    closePicker();
    global.picker = new CalendarPicker(global.params.mode, global.params);
    global.picker.attachTo($("main"));
};

// Parameter t should be a number between 0 and 1.
var AnimationTimingFunction = {
    Linear: function(t){
        return t;
    },
    EaseInOut: function(t){
        t *= 2;
        if (t < 1)
            return Math.pow(t, 3) / 2;
        t -= 2;
        return Math.pow(t, 3) / 2 + 1;
    }
};

/**
 * @constructor
 * @extends EventEmitter
 */
function AnimationManager() {
    EventEmitter.call(this);

    this._isRunning = false;
    this._runningAnimatorCount = 0;
    this._runningAnimators = {};
    this._animationFrameCallbackBound = this._animationFrameCallback.bind(this);
}

AnimationManager.prototype = Object.create(EventEmitter.prototype);

AnimationManager.EventTypeAnimationFrameWillFinish = "animationFrameWillFinish";

AnimationManager.prototype._startAnimation = function() {
    if (this._isRunning)
        return;
    this._isRunning = true;
    window.requestAnimationFrame(this._animationFrameCallbackBound);
};

AnimationManager.prototype._stopAnimation = function() {
    if (!this._isRunning)
        return;
    this._isRunning = false;
};

/**
 * @param {!Animator} animator
 */
AnimationManager.prototype.add = function(animator) {
    if (this._runningAnimators[animator.id])
        return;
    this._runningAnimators[animator.id] = animator;
    this._runningAnimatorCount++;
    if (this._needsTimer())
        this._startAnimation();
};

/**
 * @param {!Animator} animator
 */
AnimationManager.prototype.remove = function(animator) {
    if (!this._runningAnimators[animator.id])
        return;
    delete this._runningAnimators[animator.id];
    this._runningAnimatorCount--;
    if (!this._needsTimer())
        this._stopAnimation();
};

AnimationManager.prototype._animationFrameCallback = function(now) {
    if (this._runningAnimatorCount > 0) {
        for (var id in this._runningAnimators) {
            this._runningAnimators[id].onAnimationFrame(now);
        }
    }
    this.dispatchEvent(AnimationManager.EventTypeAnimationFrameWillFinish);
    if (this._isRunning)
        window.requestAnimationFrame(this._animationFrameCallbackBound);
};

/**
 * @return {!boolean}
 */
AnimationManager.prototype._needsTimer = function() {
    return this._runningAnimatorCount > 0 || this.hasListener(AnimationManager.EventTypeAnimationFrameWillFinish);
};

/**
 * @param {!string} type
 * @param {!Function} callback
 * @override
 */
AnimationManager.prototype.on = function(type, callback) {
    EventEmitter.prototype.on.call(this, type, callback);
    if (this._needsTimer())
        this._startAnimation();
};

/**
 * @param {!string} type
 * @param {!Function} callback
 * @override
 */
AnimationManager.prototype.removeListener = function(type, callback) {
    EventEmitter.prototype.removeListener.call(this, type, callback);
    if (!this._needsTimer())
        this._stopAnimation();
};

AnimationManager.shared = new AnimationManager();

/**
 * @constructor
 * @extends EventEmitter
 */
function Animator() {
    EventEmitter.call(this);

    /**
     * @type {!number}
     * @const
     */
    this.id = Animator._lastId++;
    /**
     * @type {!number}
     */
    this.duration = 100;
    /**
     * @type {?function}
     */
    this.step = null;
    /**
     * @type {!boolean}
     * @protected
     */
    this._isRunning = false;
    /**
     * @type {!number}
     */
    this.currentValue = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._lastStepTime = 0;
}

Animator.prototype = Object.create(EventEmitter.prototype);

Animator._lastId = 0;

Animator.EventTypeDidAnimationStop = "didAnimationStop";

/**
 * @return {!boolean}
 */
Animator.prototype.isRunning = function() {
    return this._isRunning;
};

Animator.prototype.start = function() {
    this._lastStepTime = performance.now();
    this._isRunning = true;
    AnimationManager.shared.add(this);
};

Animator.prototype.stop = function() {
    if (!this._isRunning)
        return;
    this._isRunning = false;
    AnimationManager.shared.remove(this);
    this.dispatchEvent(Animator.EventTypeDidAnimationStop, this);
};

/**
 * @param {!number} now
 */
Animator.prototype.onAnimationFrame = function(now) {
    this._lastStepTime = now;
    this.step(this);
};

/**
 * @constructor
 * @extends Animator
 */
function TransitionAnimator() {
    Animator.call(this);
    /**
     * @type {!number}
     * @protected
     */
    this._from = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._to = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._delta = 0;
    /**
     * @type {!number}
     */
    this.progress = 0.0;
    /**
     * @type {!function}
     */
    this.timingFunction = AnimationTimingFunction.Linear;
}

TransitionAnimator.prototype = Object.create(Animator.prototype);

/**
 * @param {!number} value
 */
TransitionAnimator.prototype.setFrom = function(value) {
    this._from = value;
    this._delta = this._to - this._from;
};

TransitionAnimator.prototype.start = function() {
    console.assert(isFinite(this.duration));
    this.progress = 0.0;
    this.currentValue = this._from;
    Animator.prototype.start.call(this);
};

/**
 * @param {!number} value
 */
TransitionAnimator.prototype.setTo = function(value) {
    this._to = value;
    this._delta = this._to - this._from;
};

/**
 * @param {!number} now
 */
TransitionAnimator.prototype.onAnimationFrame = function(now) {
    this.progress += (now - this._lastStepTime) / this.duration;
    this.progress = Math.min(1.0, this.progress);
    this._lastStepTime = now;
    this.currentValue = this.timingFunction(this.progress) * this._delta + this._from;
    this.step(this);
    if (this.progress === 1.0) {
        this.stop();
        return;
    }
};

/**
 * @constructor
 * @extends Animator
 * @param {!number} initialVelocity
 * @param {!number} initialValue
 */
function FlingGestureAnimator(initialVelocity, initialValue) {
    Animator.call(this);
    /**
     * @type {!number}
     */
    this.initialVelocity = initialVelocity;
    /**
     * @type {!number}
     */
    this.initialValue = initialValue;
    /**
     * @type {!number}
     * @protected
     */
    this._elapsedTime = 0;
    var startVelocity = Math.abs(this.initialVelocity);
    if (startVelocity > this._velocityAtTime(0))
        startVelocity = this._velocityAtTime(0);
    if (startVelocity < 0)
        startVelocity = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._timeOffset = this._timeAtVelocity(startVelocity);
    /**
     * @type {!number}
     * @protected
     */
    this._positionOffset = this._valueAtTime(this._timeOffset);
    /**
     * @type {!number}
     */
    this.duration = this._timeAtVelocity(0);
}

FlingGestureAnimator.prototype = Object.create(Animator.prototype);

// Velocity is subject to exponential decay. These parameters are coefficients
// that determine the curve.
FlingGestureAnimator._P0 = -5707.62;
FlingGestureAnimator._P1 = 0.172;
FlingGestureAnimator._P2 = 0.0037;

/**
 * @param {!number} t
 */
FlingGestureAnimator.prototype._valueAtTime = function(t) {
    return FlingGestureAnimator._P0 * Math.exp(-FlingGestureAnimator._P2 * t) - FlingGestureAnimator._P1 * t - FlingGestureAnimator._P0;
};

/**
 * @param {!number} t
 */
FlingGestureAnimator.prototype._velocityAtTime = function(t) {
    return -FlingGestureAnimator._P0 * FlingGestureAnimator._P2 * Math.exp(-FlingGestureAnimator._P2 * t) - FlingGestureAnimator._P1;
};

/**
 * @param {!number} v
 */
FlingGestureAnimator.prototype._timeAtVelocity = function(v) {
    return -Math.log((v + FlingGestureAnimator._P1) / (-FlingGestureAnimator._P0 * FlingGestureAnimator._P2)) / FlingGestureAnimator._P2;
};

FlingGestureAnimator.prototype.start = function() {
    this._lastStepTime = performance.now();
    Animator.prototype.start.call(this);
};

/**
 * @param {!number} now
 */
FlingGestureAnimator.prototype.onAnimationFrame = function(now) {
    this._elapsedTime += now - this._lastStepTime;
    this._lastStepTime = now;
    if (this._elapsedTime + this._timeOffset >= this.duration) {
        this.stop();
        return;
    }
    var position = this._valueAtTime(this._elapsedTime + this._timeOffset) - this._positionOffset;
    if (this.initialVelocity < 0)
        position = -position;
    this.currentValue = position + this.initialValue;
    this.step(this);
};

/**
 * @constructor
 * @extends EventEmitter
 * @param {?Element} element
 * View adds itself as a property on the element so we can access it from Event.target.
 */
function View(element) {
    EventEmitter.call(this);
    /**
     * @type {Element}
     * @const
     */
    this.element = element || createElement("div");
    this.element.$view = this;
    this.bindCallbackMethods();
}

View.prototype = Object.create(EventEmitter.prototype);

/**
 * @param {!Element} ancestorElement
 * @return {?Object}
 */
View.prototype.offsetRelativeTo = function(ancestorElement) {
    var x = 0;
    var y = 0;
    var element = this.element;
    while (element) {
        x += element.offsetLeft  || 0;
        y += element.offsetTop || 0;
        element = element.offsetParent;
        if (element === ancestorElement)
            return {x: x, y: y};
    }
    return null;
};

/**
 * @param {!View|Node} parent
 * @param {?View|Node=} before
 */
View.prototype.attachTo = function(parent, before) {
    if (parent instanceof View)
        return this.attachTo(parent.element, before);
    if (typeof before === "undefined")
        before = null;
    if (before instanceof View)
        before = before.element;
    parent.insertBefore(this.element, before);
};

View.prototype.bindCallbackMethods = function() {
    for (var methodName in this) {
        if (!/^on[A-Z]/.test(methodName))
            continue;
        if (this.hasOwnProperty(methodName))
            continue;
        var method = this[methodName];
        if (!(method instanceof Function))
            continue;
        this[methodName] = method.bind(this);
    }
};

/**
 * @constructor
 * @extends View
 */
function ScrollView() {
    View.call(this, createElement("div", ScrollView.ClassNameScrollView));
    /**
     * @type {Element}
     * @const
     */
    this.contentElement = createElement("div", ScrollView.ClassNameScrollViewContent);
    this.element.appendChild(this.contentElement);
    /**
     * @type {number}
     */
    this.minimumContentOffset = -Infinity;
    /**
     * @type {number}
     */
    this.maximumContentOffset = Infinity;
    /**
     * @type {number}
     * @protected
     */
    this._contentOffset = 0;
    /**
     * @type {number}
     * @protected
     */
    this._width = 0;
    /**
     * @type {number}
     * @protected
     */
    this._height = 0;
    /**
     * @type {Animator}
     * @protected
     */
    this._scrollAnimator = null;
    /**
     * @type {?Object}
     */
    this.delegate = null;
    /**
     * @type {!number}
     */
    this._lastTouchPosition = 0;
    /**
     * @type {!number}
     */
    this._lastTouchVelocity = 0;
    /**
     * @type {!number}
     */
    this._lastTouchTimeStamp = 0;

    this.element.addEventListener("mousewheel", this.onMouseWheel, false);
    this.element.addEventListener("touchstart", this.onTouchStart, false);

    /**
     * The content offset is partitioned so the it can go beyond the CSS limit
     * of 33554433px.
     * @type {number}
     * @protected
     */
    this._partitionNumber = 0;
}

ScrollView.prototype = Object.create(View.prototype);

ScrollView.PartitionHeight = 100000;
ScrollView.ClassNameScrollView = "scroll-view";
ScrollView.ClassNameScrollViewContent = "scroll-view-content";

/**
 * @param {!Event} event
 */
ScrollView.prototype.onTouchStart = function(event) {
    var touch = event.touches[0];
    this._lastTouchPosition = touch.clientY;
    this._lastTouchVelocity = 0;
    this._lastTouchTimeStamp = event.timeStamp;
    if (this._scrollAnimator)
        this._scrollAnimator.stop();
    window.addEventListener("touchmove", this.onWindowTouchMove, false);
    window.addEventListener("touchend", this.onWindowTouchEnd, false);
};

/**
 * @param {!Event} event
 */
ScrollView.prototype.onWindowTouchMove = function(event) {
    var touch = event.touches[0];
    var deltaTime = event.timeStamp - this._lastTouchTimeStamp;
    var deltaY = this._lastTouchPosition - touch.clientY;
    this.scrollBy(deltaY, false);
    this._lastTouchVelocity = deltaY / deltaTime;
    this._lastTouchPosition = touch.clientY;
    this._lastTouchTimeStamp = event.timeStamp;
    event.stopPropagation();
    event.preventDefault();
};

/**
 * @param {!Event} event
 */
ScrollView.prototype.onWindowTouchEnd = function(event) {
    if (Math.abs(this._lastTouchVelocity) > 0.01) {
        this._scrollAnimator = new FlingGestureAnimator(this._lastTouchVelocity, this._contentOffset);
        this._scrollAnimator.step = this.onFlingGestureAnimatorStep;
        this._scrollAnimator.start();
    }
    window.removeEventListener("touchmove", this.onWindowTouchMove, false);
    window.removeEventListener("touchend", this.onWindowTouchEnd, false);
};

/**
 * @param {!Animator} animator
 */
ScrollView.prototype.onFlingGestureAnimatorStep = function(animator) {
    this.scrollTo(animator.currentValue, false);
};

/**
 * @return {!Animator}
 */
ScrollView.prototype.scrollAnimator = function() {
    return this._scrollAnimator;
};

/**
 * @param {!number} width
 */
ScrollView.prototype.setWidth = function(width) {
    console.assert(isFinite(width));
    if (this._width === width)
        return;
    this._width = width;
    this.element.style.width = this._width + "px";
};

/**
 * @return {!number}
 */
ScrollView.prototype.width = function() {
    return this._width;
};

/**
 * @param {!number} height
 */
ScrollView.prototype.setHeight = function(height) {
    console.assert(isFinite(height));
    if (this._height === height)
        return;
    this._height = height;
    this.element.style.height = height + "px";
    if (this.delegate)
        this.delegate.scrollViewDidChangeHeight(this);
};

/**
 * @return {!number}
 */
ScrollView.prototype.height = function() {
    return this._height;
};

/**
 * @param {!Animator} animator
 */
ScrollView.prototype.onScrollAnimatorStep = function(animator) {
    this.setContentOffset(animator.currentValue);
};

/**
 * @param {!number} offset
 * @param {?boolean} animate
 */
ScrollView.prototype.scrollTo = function(offset, animate) {
    console.assert(isFinite(offset));
    if (!animate) {
        this.setContentOffset(offset);
        return;
    }
    if (this._scrollAnimator)
        this._scrollAnimator.stop();
    this._scrollAnimator = new TransitionAnimator();
    this._scrollAnimator.step = this.onScrollAnimatorStep;
    this._scrollAnimator.setFrom(this._contentOffset);
    this._scrollAnimator.setTo(offset);
    this._scrollAnimator.duration = 300;
    this._scrollAnimator.start();
};

/**
 * @param {!number} offset
 * @param {?boolean} animate
 */
ScrollView.prototype.scrollBy = function(offset, animate) {
    this.scrollTo(this._contentOffset + offset, animate);
};

/**
 * @return {!number}
 */
ScrollView.prototype.contentOffset = function() {
    return this._contentOffset;
};

/**
 * @param {?Event} event
 */
ScrollView.prototype.onMouseWheel = function(event) {
    this.setContentOffset(this._contentOffset - event.wheelDelta / 30);
    event.stopPropagation();
    event.preventDefault();
};


/**
 * @param {!number} value
 */
ScrollView.prototype.setContentOffset = function(value) {
    console.assert(isFinite(value));
    value = Math.min(this.maximumContentOffset - this._height, Math.max(this.minimumContentOffset, Math.floor(value)));
    if (this._contentOffset === value)
        return;
    this._contentOffset = value;
    this._updateScrollContent();
    if (this.delegate)
        this.delegate.scrollViewDidChangeContentOffset(this);
};

ScrollView.prototype._updateScrollContent = function() {
    var newPartitionNumber = Math.floor(this._contentOffset / ScrollView.PartitionHeight);
    var partitionChanged = this._partitionNumber !== newPartitionNumber;
    this._partitionNumber = newPartitionNumber;
    this.contentElement.style.webkitTransform = "translate(0, " + (-this.contentPositionForContentOffset(this._contentOffset)) + "px)";
    if (this.delegate && partitionChanged)
        this.delegate.scrollViewDidChangePartition(this);
};

/**
 * @param {!View|Node} parent
 * @param {?View|Node=} before
 * @override
 */
ScrollView.prototype.attachTo = function(parent, before) {
    View.prototype.attachTo.call(this, parent, before);
    this._updateScrollContent();
};

/**
 * @param {!number} offset
 */
ScrollView.prototype.contentPositionForContentOffset = function(offset) {
    return offset - this._partitionNumber * ScrollView.PartitionHeight;
};

/**
 * @constructor
 * @extends View
 */
function ListCell() {
    View.call(this, createElement("div", ListCell.ClassNameListCell));
    
    /**
     * @type {!number}
     */
    this.row = NaN;
    /**
     * @type {!number}
     */
    this._width = 0;
    /**
     * @type {!number}
     */
    this._position = 0;
}

ListCell.prototype = Object.create(View.prototype);

ListCell.DefaultRecycleBinLimit = 64;
ListCell.ClassNameListCell = "list-cell";
ListCell.ClassNameHidden = "hidden";

/**
 * @return {!Array} An array to keep thrown away cells.
 */
ListCell.prototype._recycleBin = function() {
    console.assert(false, "NOT REACHED: ListCell.prototype._recycleBin needs to be overridden.");
    return [];
};

ListCell.prototype.throwAway = function() {
    this.hide();
    var limit = typeof this.constructor.RecycleBinLimit === "undefined" ? ListCell.DefaultRecycleBinLimit : this.constructor.RecycleBinLimit;
    var recycleBin = this._recycleBin();
    if (recycleBin.length < limit)
        recycleBin.push(this);
};

ListCell.prototype.show = function() {
    this.element.classList.remove(ListCell.ClassNameHidden);
};

ListCell.prototype.hide = function() {
    this.element.classList.add(ListCell.ClassNameHidden);
};

/**
 * @return {!number} Width in pixels.
 */
ListCell.prototype.width = function(){
    return this._width;
};

/**
 * @param {!number} width Width in pixels.
 */
ListCell.prototype.setWidth = function(width){
    if (this._width === width)
        return;
    this._width = width;
    this.element.style.width = this._width + "px";
};

/**
 * @return {!number} Position in pixels.
 */
ListCell.prototype.position = function(){
    return this._position;
};

/**
 * @param {!number} y Position in pixels.
 */
ListCell.prototype.setPosition = function(y) {
    if (this._position === y)
        return;
    this._position = y;
    this.element.style.webkitTransform = "translate(0, " + this._position + "px)";
};

/**
 * @param {!boolean} selected
 */
ListCell.prototype.setSelected = function(selected) {
    if (this._selected === selected)
        return;
    this._selected = selected;
    if (this._selected)
        this.element.classList.add("selected");
    else
        this.element.classList.remove("selected");
};

/**
 * @constructor
 * @extends View
 */
function ListView() {
    View.call(this, createElement("div", ListView.ClassNameListView));
    this.element.tabIndex = 0;
    this.element.setAttribute("role", "grid");

    /**
     * @type {!number}
     * @private
     */
    this._width = 0;
    /**
     * @type {!Object}
     * @private
     */
    this._cells = {};

    /**
     * @type {!number}
     */
    this.selectedRow = ListView.NoSelection;

    /**
     * @type {!ScrollView}
     */
    this.scrollView = new ScrollView();
    this.scrollView.delegate = this;
    this.scrollView.minimumContentOffset = 0;
    this.scrollView.setWidth(0);
    this.scrollView.setHeight(0);
    this.scrollView.attachTo(this);

    this.element.addEventListener("click", this.onClick, false);

    /**
     * @type {!boolean}
     * @private
     */
    this._needsUpdateCells = false;
}

ListView.prototype = Object.create(View.prototype);

ListView.NoSelection = -1;
ListView.ClassNameListView = "list-view";

ListView.prototype.onAnimationFrameWillFinish = function() {
    if (this._needsUpdateCells)
        this.updateCells();
};

/**
 * @param {!boolean} needsUpdateCells
 */
ListView.prototype.setNeedsUpdateCells = function(needsUpdateCells) {
    if (this._needsUpdateCells === needsUpdateCells)
        return;
    this._needsUpdateCells = needsUpdateCells;
    if (this._needsUpdateCells)
        AnimationManager.shared.on(AnimationManager.EventTypeAnimationFrameWillFinish, this.onAnimationFrameWillFinish);
    else
        AnimationManager.shared.removeListener(AnimationManager.EventTypeAnimationFrameWillFinish, this.onAnimationFrameWillFinish);
};

/**
 * @param {!number} row
 * @return {?ListCell}
 */
ListView.prototype.cellAtRow = function(row) {
    return this._cells[row];
};

/**
 * @param {!number} offset Scroll offset in pixels.
 * @return {!number}
 */
ListView.prototype.rowAtScrollOffset = function(offset) {
    console.assert(false, "NOT REACHED: ListView.prototype.rowAtScrollOffset needs to be overridden.");
    return 0;
};

/**
 * @param {!number} row
 * @return {!number} Scroll offset in pixels.
 */
ListView.prototype.scrollOffsetForRow = function(row) {
    console.assert(false, "NOT REACHED: ListView.prototype.scrollOffsetForRow needs to be overridden.");
    return 0;
};

/**
 * @param {!number} row
 * @return {!ListCell}
 */
ListView.prototype.addCellIfNecessary = function(row) {
    var cell = this._cells[row];
    if (cell)
        return cell;
    cell = this.prepareNewCell(row);
    cell.attachTo(this.scrollView.contentElement);
    cell.setWidth(this._width);
    cell.setPosition(this.scrollView.contentPositionForContentOffset(this.scrollOffsetForRow(row)));
    this._cells[row] = cell;
    return cell;
};

/**
 * @param {!number} row
 * @return {!ListCell}
 */
ListView.prototype.prepareNewCell = function(row) {
    console.assert(false, "NOT REACHED: ListView.prototype.prepareNewCell should be overridden.");
    return new ListCell();
};

/**
 * @param {!ListCell} cell
 */
ListView.prototype.throwAwayCell = function(cell) {
    delete this._cells[cell.row];
    cell.throwAway();
};

/**
 * @return {!number}
 */
ListView.prototype.firstVisibleRow = function() {
    return this.rowAtScrollOffset(this.scrollView.contentOffset());
};

/**
 * @return {!number}
 */
ListView.prototype.lastVisibleRow = function() {
    return this.rowAtScrollOffset(this.scrollView.contentOffset() + this.scrollView.height() - 1);
};

/**
 * @param {!ScrollView} scrollView
 */
ListView.prototype.scrollViewDidChangeContentOffset = function(scrollView) {
    this.setNeedsUpdateCells(true);
};

/**
 * @param {!ScrollView} scrollView
 */
ListView.prototype.scrollViewDidChangeHeight = function(scrollView) {
    this.setNeedsUpdateCells(true);
};

/**
 * @param {!ScrollView} scrollView
 */
ListView.prototype.scrollViewDidChangePartition = function(scrollView) {
    this.setNeedsUpdateCells(true);
};

ListView.prototype.updateCells = function() {
    var firstVisibleRow = this.firstVisibleRow();
    var lastVisibleRow = this.lastVisibleRow();
    console.assert(firstVisibleRow <= lastVisibleRow);
    for (var c in this._cells) {
        var cell = this._cells[c];
        if (cell.row < firstVisibleRow || cell.row > lastVisibleRow)
            this.throwAwayCell(cell);
    }
    for (var i = firstVisibleRow; i <= lastVisibleRow; ++i) {
        var cell = this._cells[i];
        if (cell)
            cell.setPosition(this.scrollView.contentPositionForContentOffset(this.scrollOffsetForRow(cell.row)));
        else
            this.addCellIfNecessary(i);
    }
    this.setNeedsUpdateCells(false);
};

/**
 * @return {!number} Width in pixels.
 */
ListView.prototype.width = function() {
    return this._width;
};

/**
 * @param {!number} width Width in pixels.
 */
ListView.prototype.setWidth = function(width) {
    if (this._width === width)
        return;
    this._width = width;
    this.scrollView.setWidth(this._width);
    for (var c in this._cells) {
        this._cells[c].setWidth(this._width);
    }
    this.element.style.width = this._width + "px";
    this.setNeedsUpdateCells(true);
};

/**
 * @return {!number} Height in pixels.
 */
ListView.prototype.height = function() {
    return this.scrollView.height();
};

/**
 * @param {!number} height Height in pixels.
 */
ListView.prototype.setHeight = function(height) {
    this.scrollView.setHeight(height);
};

/**
 * @param {?Event} event
 */
ListView.prototype.onClick = function(event) {
    var clickedCellElement = enclosingNodeOrSelfWithClass(event.target, ListCell.ClassNameListCell);
    if (!clickedCellElement)
        return;
    var clickedCell = clickedCellElement.$view;
    if (clickedCell.row !== this.selectedRow)
        this.select(clickedCell.row);
};

/**
 * @param {!number} row
 */
ListView.prototype.select = function(row) {
    if (this.selectedRow === row)
        return;
    this.deselect();
    if (row === ListView.NoSelection)
        return;
    this.selectedRow = row;
    var selectedCell = this._cells[this.selectedRow];
    if (selectedCell)
        selectedCell.setSelected(true);
};

ListView.prototype.deselect = function() {
    if (this.selectedRow === ListView.NoSelection)
        return;
    var selectedCell = this._cells[this.selectedRow];
    if (selectedCell)
        selectedCell.setSelected(false);
    this.selectedRow = ListView.NoSelection;
};

/**
 * @param {!number} row
 * @param {!boolean} animate
 */
ListView.prototype.scrollToRow = function(row, animate) {
    this.scrollView.scrollTo(this.scrollOffsetForRow(row), animate);
};

/**
 * @constructor
 * @extends View
 * @param {!ScrollView} scrollView
 */
function ScrubbyScrollBar(scrollView) {
    View.call(this, createElement("div", ScrubbyScrollBar.ClassNameScrubbyScrollBar));

    /**
     * @type {!Element}
     * @const
     */
    this.thumb = createElement("div", ScrubbyScrollBar.ClassNameScrubbyScrollThumb);
    this.element.appendChild(this.thumb);

    /**
     * @type {!ScrollView}
     * @const
     */
    this.scrollView = scrollView;

    /**
     * @type {!number}
     * @protected
     */
    this._height = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._thumbHeight = 0;
    /**
     * @type {!number}
     * @protected
     */
    this._thumbPosition = 0;

    this.setHeight(0);
    this.setThumbHeight(ScrubbyScrollBar.ThumbHeight);

    /**
     * @type {?Animator}
     * @protected
     */
    this._thumbStyleTopAnimator = null;

    /** 
     * @type {?number}
     * @protected
     */
    this._timer = null;
    
    this.element.addEventListener("mousedown", this.onMouseDown, false);
    this.element.addEventListener("touchstart", this.onTouchStart, false);
}

ScrubbyScrollBar.prototype = Object.create(View.prototype);

ScrubbyScrollBar.ScrollInterval = 16;
ScrubbyScrollBar.ThumbMargin = 2;
ScrubbyScrollBar.ThumbHeight = 30;
ScrubbyScrollBar.ClassNameScrubbyScrollBar = "scrubby-scroll-bar";
ScrubbyScrollBar.ClassNameScrubbyScrollThumb = "scrubby-scroll-thumb";

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onTouchStart = function(event) {
    var touch = event.touches[0];
    this._setThumbPositionFromEventPosition(touch.clientY);
    if (this._thumbStyleTopAnimator)
        this._thumbStyleTopAnimator.stop();
    this._timer = setInterval(this.onScrollTimer, ScrubbyScrollBar.ScrollInterval);
    window.addEventListener("touchmove", this.onWindowTouchMove, false);
    window.addEventListener("touchend", this.onWindowTouchEnd, false);
    event.stopPropagation();
    event.preventDefault();
};

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onWindowTouchMove = function(event) {
    var touch = event.touches[0];
    this._setThumbPositionFromEventPosition(touch.clientY);
    event.stopPropagation();
    event.preventDefault();
};

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onWindowTouchEnd = function(event) {
    this._thumbStyleTopAnimator = new TransitionAnimator();
    this._thumbStyleTopAnimator.step = this.onThumbStyleTopAnimationStep;
    this._thumbStyleTopAnimator.setFrom(this.thumb.offsetTop);
    this._thumbStyleTopAnimator.setTo((this._height - this._thumbHeight) / 2);
    this._thumbStyleTopAnimator.timingFunction = AnimationTimingFunction.EaseInOut;
    this._thumbStyleTopAnimator.duration = 100;
    this._thumbStyleTopAnimator.start();

    window.removeEventListener("touchmove", this.onWindowTouchMove, false);
    window.removeEventListener("touchend", this.onWindowTouchEnd, false);
    clearInterval(this._timer);
};

/**
 * @return {!number} Height of the view in pixels.
 */
ScrubbyScrollBar.prototype.height = function() {
    return this._height;
};

/**
 * @param {!number} height Height of the view in pixels.
 */
ScrubbyScrollBar.prototype.setHeight = function(height) {
    if (this._height === height)
        return;
    this._height = height;
    this.element.style.height = this._height + "px";
    this.thumb.style.top = ((this._height - this._thumbHeight) / 2) + "px";
    this._thumbPosition = 0;
};

/**
 * @param {!number} height Height of the scroll bar thumb in pixels.
 */
ScrubbyScrollBar.prototype.setThumbHeight = function(height) {
    if (this._thumbHeight === height)
        return;
    this._thumbHeight = height;
    this.thumb.style.height = this._thumbHeight + "px";
    this.thumb.style.top = ((this._height - this._thumbHeight) / 2) + "px";
    this._thumbPosition = 0;
};

/**
 * @param {number} position
 */
ScrubbyScrollBar.prototype._setThumbPositionFromEventPosition = function(position) {
    var thumbMin = ScrubbyScrollBar.ThumbMargin;
    var thumbMax = this._height - this._thumbHeight - ScrubbyScrollBar.ThumbMargin * 2;
    var y = position - this.element.getBoundingClientRect().top - this.element.clientTop + this.element.scrollTop;
    var thumbPosition = y - this._thumbHeight / 2;
    thumbPosition = Math.max(thumbPosition, thumbMin);
    thumbPosition = Math.min(thumbPosition, thumbMax);
    this.thumb.style.top = thumbPosition + "px";
    this._thumbPosition = 1.0 - (thumbPosition - thumbMin) / (thumbMax - thumbMin) * 2;
};

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onMouseDown = function(event) {
    this._setThumbPositionFromEventPosition(event.clientY);

    window.addEventListener("mousemove", this.onWindowMouseMove, false);
    window.addEventListener("mouseup", this.onWindowMouseUp, false);
    if (this._thumbStyleTopAnimator)
        this._thumbStyleTopAnimator.stop();
    this._timer = setInterval(this.onScrollTimer, ScrubbyScrollBar.ScrollInterval);
    event.stopPropagation();
    event.preventDefault();
};

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onWindowMouseMove = function(event) {
    this._setThumbPositionFromEventPosition(event.clientY);
};

/**
 * @param {?Event} event
 */
ScrubbyScrollBar.prototype.onWindowMouseUp = function(event) {
    this._thumbStyleTopAnimator = new TransitionAnimator();
    this._thumbStyleTopAnimator.step = this.onThumbStyleTopAnimationStep;
    this._thumbStyleTopAnimator.setFrom(this.thumb.offsetTop);
    this._thumbStyleTopAnimator.setTo((this._height - this._thumbHeight) / 2);
    this._thumbStyleTopAnimator.timingFunction = AnimationTimingFunction.EaseInOut;
    this._thumbStyleTopAnimator.duration = 100;
    this._thumbStyleTopAnimator.start();
    
    window.removeEventListener("mousemove", this.onWindowMouseMove, false);
    window.removeEventListener("mouseup", this.onWindowMouseUp, false);
    clearInterval(this._timer);
};

/**
 * @param {!Animator} animator
 */
ScrubbyScrollBar.prototype.onThumbStyleTopAnimationStep = function(animator) {
    this.thumb.style.top = animator.currentValue + "px";
};

ScrubbyScrollBar.prototype.onScrollTimer = function() {
    var scrollAmount = Math.pow(this._thumbPosition, 2) * 10;
    if (this._thumbPosition > 0)
        scrollAmount = -scrollAmount;
    this.scrollView.scrollBy(scrollAmount, false);
};

/**
 * @constructor
 * @extends ListCell
 * @param {!Array} shortMonthLabels
 */
function YearListCell(shortMonthLabels) {
    ListCell.call(this);
    this.element.classList.add(YearListCell.ClassNameYearListCell);
    this.element.style.height = YearListCell.Height + "px";

    /**
     * @type {!Element}
     * @const
     */
    this.label = createElement("div", YearListCell.ClassNameLabel, "----");
    this.element.appendChild(this.label);
    this.label.style.height = (YearListCell.Height - YearListCell.BorderBottomWidth) + "px";
    this.label.style.lineHeight = (YearListCell.Height - YearListCell.BorderBottomWidth) + "px";

    /**
     * @type {!Array} Array of the 12 month button elements.
     * @const
     */
    this.monthButtons = [];
    var monthChooserElement = createElement("div", YearListCell.ClassNameMonthChooser);
    for (var r = 0; r < YearListCell.ButtonRows; ++r) {
        var buttonsRow = createElement("div", YearListCell.ClassNameMonthButtonsRow);
        buttonsRow.setAttribute("role", "row");
        for (var c = 0; c < YearListCell.ButtonColumns; ++c) {
            var month = c + r * YearListCell.ButtonColumns;
            var button = createElement("div", YearListCell.ClassNameMonthButton, shortMonthLabels[month]);
            button.setAttribute("role", "gridcell");
            button.dataset.month = month;
            buttonsRow.appendChild(button);
            this.monthButtons.push(button);
        }
        monthChooserElement.appendChild(buttonsRow);
    }
    this.element.appendChild(monthChooserElement);

    /**
     * @type {!boolean}
     * @private
     */
    this._selected = false;
    /**
     * @type {!number}
     * @private
     */
    this._height = 0;
}

YearListCell.prototype = Object.create(ListCell.prototype);

YearListCell.Height = hasInaccuratePointingDevice() ? 31 : 25;
YearListCell.BorderBottomWidth = 1;
YearListCell.ButtonRows = 3;
YearListCell.ButtonColumns = 4;
YearListCell.SelectedHeight = hasInaccuratePointingDevice() ? 127 : 121;
YearListCell.ClassNameYearListCell = "year-list-cell";
YearListCell.ClassNameLabel = "label";
YearListCell.ClassNameMonthChooser = "month-chooser";
YearListCell.ClassNameMonthButtonsRow = "month-buttons-row";
YearListCell.ClassNameMonthButton = "month-button";
YearListCell.ClassNameHighlighted = "highlighted";

YearListCell._recycleBin = [];

/**
 * @return {!Array}
 * @override
 */
YearListCell.prototype._recycleBin = function() {
    return YearListCell._recycleBin;
};

/**
 * @param {!number} row
 */
YearListCell.prototype.reset = function(row) {
    this.row = row;
    this.label.textContent = row + 1;
    for (var i = 0; i < this.monthButtons.length; ++i) {
        this.monthButtons[i].classList.remove(YearListCell.ClassNameHighlighted);
    }
    this.show();
};

/**
 * @return {!number} The height in pixels.
 */
YearListCell.prototype.height = function() {
    return this._height;
};

/**
 * @param {!number} height Height in pixels.
 */
YearListCell.prototype.setHeight = function(height) {
    if (this._height === height)
        return;
    this._height = height;
    this.element.style.height = this._height + "px";
};

/**
 * @constructor
 * @extends ListView
 * @param {!Month} minimumMonth
 * @param {!Month} maximumMonth
 */
function YearListView(minimumMonth, maximumMonth) {
    ListView.call(this);
    this.element.classList.add("year-list-view");

    /**
     * @type {?Month}
     */
    this.highlightedMonth = null;
    /**
     * @type {!Month}
     * @const
     * @protected
     */
    this._minimumMonth = minimumMonth;
    /**
     * @type {!Month}
     * @const
     * @protected
     */
    this._maximumMonth = maximumMonth;

    this.scrollView.minimumContentOffset = (this._minimumMonth.year - 1) * YearListCell.Height;
    this.scrollView.maximumContentOffset = (this._maximumMonth.year - 1) * YearListCell.Height + YearListCell.SelectedHeight;
    
    /**
     * @type {!Object}
     * @const
     * @protected
     */
    this._runningAnimators = {};
    /**
     * @type {!Array}
     * @const
     * @protected
     */
    this._animatingRows = [];
    /**
     * @type {!boolean}
     * @protected
     */
    this._ignoreMouseOutUntillNextMouseOver = false;
    
    /**
     * @type {!ScrubbyScrollBar}
     * @const
     */
    this.scrubbyScrollBar = new ScrubbyScrollBar(this.scrollView);
    this.scrubbyScrollBar.attachTo(this);
    
    this.element.addEventListener("mouseover", this.onMouseOver, false);
    this.element.addEventListener("mouseout", this.onMouseOut, false);
    this.element.addEventListener("keydown", this.onKeyDown, false);
    this.element.addEventListener("touchstart", this.onTouchStart, false);
}

YearListView.prototype = Object.create(ListView.prototype);

YearListView.Height = YearListCell.SelectedHeight - 1;
YearListView.EventTypeYearListViewDidHide = "yearListViewDidHide";
YearListView.EventTypeYearListViewDidSelectMonth = "yearListViewDidSelectMonth";

/**
 * @param {?Event} event
 */
YearListView.prototype.onTouchStart = function(event) {
    var touch = event.touches[0];
    var monthButtonElement = enclosingNodeOrSelfWithClass(touch.target, YearListCell.ClassNameMonthButton);
    if (!monthButtonElement)
        return;
    var cellElement = enclosingNodeOrSelfWithClass(monthButtonElement, YearListCell.ClassNameYearListCell);
    var cell = cellElement.$view;
    this.highlightMonth(new Month(cell.row + 1, parseInt(monthButtonElement.dataset.month, 10)));
};

/**
 * @param {?Event} event
 */
YearListView.prototype.onMouseOver = function(event) {
    var monthButtonElement = enclosingNodeOrSelfWithClass(event.target, YearListCell.ClassNameMonthButton);
    if (!monthButtonElement)
        return;
    var cellElement = enclosingNodeOrSelfWithClass(monthButtonElement, YearListCell.ClassNameYearListCell);
    var cell = cellElement.$view;
    this.highlightMonth(new Month(cell.row + 1, parseInt(monthButtonElement.dataset.month, 10)));
    this._ignoreMouseOutUntillNextMouseOver = false;
};

/**
 * @param {?Event} event
 */
YearListView.prototype.onMouseOut = function(event) {
    if (this._ignoreMouseOutUntillNextMouseOver)
        return;
    var monthButtonElement = enclosingNodeOrSelfWithClass(event.target, YearListCell.ClassNameMonthButton);
    if (!monthButtonElement) {
        this.dehighlightMonth();
    }
};

/**
 * @param {!number} width Width in pixels.
 * @override
 */
YearListView.prototype.setWidth = function(width) {
    ListView.prototype.setWidth.call(this, width - this.scrubbyScrollBar.element.offsetWidth);
    this.element.style.width = width + "px";
};

/**
 * @param {!number} height Height in pixels.
 * @override
 */
YearListView.prototype.setHeight = function(height) {
    ListView.prototype.setHeight.call(this, height);
    this.scrubbyScrollBar.setHeight(height);
};

/**
 * @enum {number}
 */
YearListView.RowAnimationDirection = {
    Opening: 0,
    Closing: 1
};

/**
 * @param {!number} row
 * @param {!YearListView.RowAnimationDirection} direction
 */
YearListView.prototype._animateRow = function(row, direction) {
    var fromValue = direction === YearListView.RowAnimationDirection.Closing ? YearListCell.SelectedHeight : YearListCell.Height;
    var oldAnimator = this._runningAnimators[row];
    if (oldAnimator) {
        oldAnimator.stop();
        fromValue = oldAnimator.currentValue;
    }
    var cell = this.cellAtRow(row);
    var animator = new TransitionAnimator();
    animator.step = this.onCellHeightAnimatorStep;
    animator.setFrom(fromValue);
    animator.setTo(direction === YearListView.RowAnimationDirection.Opening ? YearListCell.SelectedHeight : YearListCell.Height);
    animator.timingFunction = AnimationTimingFunction.EaseInOut;
    animator.duration = 300;
    animator.row = row;
    animator.on(Animator.EventTypeDidAnimationStop, this.onCellHeightAnimatorDidStop);
    this._runningAnimators[row] = animator;
    this._animatingRows.push(row);
    this._animatingRows.sort();
    animator.start();
};

/**
 * @param {?Animator} animator
 */
YearListView.prototype.onCellHeightAnimatorDidStop = function(animator) {
    delete this._runningAnimators[animator.row];
    var index = this._animatingRows.indexOf(animator.row);
    this._animatingRows.splice(index, 1);
};

/**
 * @param {!Animator} animator
 */
YearListView.prototype.onCellHeightAnimatorStep = function(animator) {
    var cell = this.cellAtRow(animator.row);
    if (cell)
        cell.setHeight(animator.currentValue);
    this.updateCells();
};

/**
 * @param {?Event} event
 */
YearListView.prototype.onClick = function(event) {
    var oldSelectedRow = this.selectedRow;
    ListView.prototype.onClick.call(this, event);
    var year = this.selectedRow + 1;
    if (this.selectedRow !== oldSelectedRow) {
        var month = this.highlightedMonth ? this.highlightedMonth.month : 0;
        this.dispatchEvent(YearListView.EventTypeYearListViewDidSelectMonth, this, new Month(year, month));
        this.scrollView.scrollTo(this.selectedRow * YearListCell.Height, true);
    } else {
        var monthButton = enclosingNodeOrSelfWithClass(event.target, YearListCell.ClassNameMonthButton);
        if (!monthButton || monthButton.getAttribute("aria-disabled") == "true")
            return;
        var month = parseInt(monthButton.dataset.month, 10);
        this.dispatchEvent(YearListView.EventTypeYearListViewDidSelectMonth, this, new Month(year, month));
        this.hide();
    }
};

/**
 * @param {!number} scrollOffset
 * @return {!number}
 * @override
 */
YearListView.prototype.rowAtScrollOffset = function(scrollOffset) {
    var remainingOffset = scrollOffset;
    var lastAnimatingRow = 0;
    var rowsWithIrregularHeight = this._animatingRows.slice();
    if (this.selectedRow > -1 && !this._runningAnimators[this.selectedRow]) {
        rowsWithIrregularHeight.push(this.selectedRow);
        rowsWithIrregularHeight.sort();
    }
    for (var i = 0; i < rowsWithIrregularHeight.length; ++i) {
        var row = rowsWithIrregularHeight[i];
        var animator = this._runningAnimators[row];
        var rowHeight = animator ? animator.currentValue : YearListCell.SelectedHeight;
        if (remainingOffset <= (row - lastAnimatingRow) * YearListCell.Height) {
            return lastAnimatingRow + Math.floor(remainingOffset / YearListCell.Height);
        }
        remainingOffset -= (row - lastAnimatingRow) * YearListCell.Height;
        if (remainingOffset <= (rowHeight - YearListCell.Height))
            return row;
        remainingOffset -= rowHeight - YearListCell.Height;
        lastAnimatingRow = row;
    }
    return lastAnimatingRow + Math.floor(remainingOffset / YearListCell.Height);
};

/**
 * @param {!number} row
 * @return {!number}
 * @override
 */
YearListView.prototype.scrollOffsetForRow = function(row) {
    var scrollOffset = row * YearListCell.Height;
    for (var i = 0; i < this._animatingRows.length; ++i) {
        var animatingRow = this._animatingRows[i];
        if (animatingRow >= row)
            break;
        var animator = this._runningAnimators[animatingRow];
        scrollOffset += animator.currentValue - YearListCell.Height;
    }
    if (this.selectedRow > -1 && this.selectedRow < row && !this._runningAnimators[this.selectedRow]) {
        scrollOffset += YearListCell.SelectedHeight - YearListCell.Height;
    }
    return scrollOffset;
};

/**
 * @param {!number} row
 * @return {!YearListCell}
 * @override
 */
YearListView.prototype.prepareNewCell = function(row) {
    var cell = YearListCell._recycleBin.pop() || new YearListCell(global.params.shortMonthLabels);
    cell.reset(row);
    cell.setSelected(this.selectedRow === row);
    for (var i = 0; i < cell.monthButtons.length; ++i) {
        var month = new Month(row + 1, i);
        cell.monthButtons[i].id = month.toString();
        cell.monthButtons[i].setAttribute("aria-disabled", this._minimumMonth > month || this._maximumMonth < month ? "true" : "false");
        cell.monthButtons[i].setAttribute("aria-label", month.toLocaleString());
    }
    if (this.highlightedMonth && row === this.highlightedMonth.year - 1) {
        var monthButton = cell.monthButtons[this.highlightedMonth.month];
        monthButton.classList.add(YearListCell.ClassNameHighlighted);
        // aria-activedescendant assumes both elements have layoutObjects, and
        // |monthButton| might have no layoutObject yet.
        var element = this.element;
        setTimeout(function() {
            element.setAttribute("aria-activedescendant", monthButton.id);
        }, 0);
    }
    var animator = this._runningAnimators[row];
    if (animator)
        cell.setHeight(animator.currentValue);
    else if (row === this.selectedRow)
        cell.setHeight(YearListCell.SelectedHeight);
    else
        cell.setHeight(YearListCell.Height);
    return cell;
};

/**
 * @override
 */
YearListView.prototype.updateCells = function() {
    var firstVisibleRow = this.firstVisibleRow();
    var lastVisibleRow = this.lastVisibleRow();
    console.assert(firstVisibleRow <= lastVisibleRow);
    for (var c in this._cells) {
        var cell = this._cells[c];
        if (cell.row < firstVisibleRow || cell.row > lastVisibleRow)
            this.throwAwayCell(cell);
    }
    for (var i = firstVisibleRow; i <= lastVisibleRow; ++i) {
        var cell = this._cells[i];
        if (cell)
            cell.setPosition(this.scrollView.contentPositionForContentOffset(this.scrollOffsetForRow(cell.row)));
        else
            this.addCellIfNecessary(i);
    }
    this.setNeedsUpdateCells(false);
};

/**
 * @override
 */
YearListView.prototype.deselect = function() {
    if (this.selectedRow === ListView.NoSelection)
        return;
    var selectedCell = this._cells[this.selectedRow];
    if (selectedCell)
        selectedCell.setSelected(false);
    this._animateRow(this.selectedRow, YearListView.RowAnimationDirection.Closing);
    this.selectedRow = ListView.NoSelection;
    this.setNeedsUpdateCells(true);
};

YearListView.prototype.deselectWithoutAnimating = function() {
    if (this.selectedRow === ListView.NoSelection)
        return;
    var selectedCell = this._cells[this.selectedRow];
    if (selectedCell) {
        selectedCell.setSelected(false);
        selectedCell.setHeight(YearListCell.Height);
    }
    this.selectedRow = ListView.NoSelection;
    this.setNeedsUpdateCells(true);
};

/**
 * @param {!number} row
 * @override
 */
YearListView.prototype.select = function(row) {
    if (this.selectedRow === row)
        return;
    this.deselect();
    if (row === ListView.NoSelection)
        return;
    this.selectedRow = row;
    if (this.selectedRow !== ListView.NoSelection) {
        var selectedCell = this._cells[this.selectedRow];
        this._animateRow(this.selectedRow, YearListView.RowAnimationDirection.Opening);
        if (selectedCell)
            selectedCell.setSelected(true);
        var month = this.highlightedMonth ? this.highlightedMonth.month : 0;
        this.highlightMonth(new Month(this.selectedRow + 1, month));
    }
    this.setNeedsUpdateCells(true);
};

/**
 * @param {!number} row
 */
YearListView.prototype.selectWithoutAnimating = function(row) {
    if (this.selectedRow === row)
        return;
    this.deselectWithoutAnimating();
    if (row === ListView.NoSelection)
        return;
    this.selectedRow = row;
    if (this.selectedRow !== ListView.NoSelection) {
        var selectedCell = this._cells[this.selectedRow];
        if (selectedCell) {
            selectedCell.setSelected(true);
            selectedCell.setHeight(YearListCell.SelectedHeight);
        }
        var month = this.highlightedMonth ? this.highlightedMonth.month : 0;
        this.highlightMonth(new Month(this.selectedRow + 1, month));
    }
    this.setNeedsUpdateCells(true);
};

/**
 * @param {!Month} month
 * @return {?HTMLDivElement}
 */
YearListView.prototype.buttonForMonth = function(month) {
    if (!month)
        return null;
    var row = month.year - 1;
    var cell = this.cellAtRow(row);
    if (!cell)
        return null;
    return cell.monthButtons[month.month];
};

YearListView.prototype.dehighlightMonth = function() {
    if (!this.highlightedMonth)
        return;
    var monthButton = this.buttonForMonth(this.highlightedMonth);
    if (monthButton) {
        monthButton.classList.remove(YearListCell.ClassNameHighlighted);
    }
    this.highlightedMonth = null;
    this.element.removeAttribute("aria-activedescendant");
};

/**
 * @param {!Month} month
 */
YearListView.prototype.highlightMonth = function(month) {
    if (this.highlightedMonth && this.highlightedMonth.equals(month))
        return;
    this.dehighlightMonth();
    this.highlightedMonth = month;
    if (!this.highlightedMonth)
        return;
    var monthButton = this.buttonForMonth(this.highlightedMonth);
    if (monthButton) {
        monthButton.classList.add(YearListCell.ClassNameHighlighted);
        this.element.setAttribute("aria-activedescendant", monthButton.id);
    }
};

/**
 * @param {!Month} month
 */
YearListView.prototype.show = function(month) {
    this._ignoreMouseOutUntillNextMouseOver = true;
    
    this.scrollToRow(month.year - 1, false);
    this.selectWithoutAnimating(month.year - 1);
    this.highlightMonth(month);
};

YearListView.prototype.hide = function() {
    this.dispatchEvent(YearListView.EventTypeYearListViewDidHide, this);
};

/**
 * @param {!Month} month
 */
YearListView.prototype._moveHighlightTo = function(month) {
    this.highlightMonth(month);
    this.select(this.highlightedMonth.year - 1);

    this.dispatchEvent(YearListView.EventTypeYearListViewDidSelectMonth, this, month);
    this.scrollView.scrollTo(this.selectedRow * YearListCell.Height, true);
    return true;
};

/**
 * @param {?Event} event
 */
YearListView.prototype.onKeyDown = function(event) {
    var key = event.keyIdentifier;
    var eventHandled = false;
    if (key == "U+0054") // 't' key.
        eventHandled = this._moveHighlightTo(Month.createFromToday());
    else if (this.highlightedMonth) {
        if (global.params.isLocaleRTL ? key == "Right" : key == "Left")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.previous());
        else if (key == "Up")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.previous(YearListCell.ButtonColumns));
        else if (global.params.isLocaleRTL ? key == "Left" : key == "Right")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.next());
        else if (key == "Down")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.next(YearListCell.ButtonColumns));
        else if (key == "PageUp")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.previous(MonthsPerYear));
        else if (key == "PageDown")
            eventHandled = this._moveHighlightTo(this.highlightedMonth.next(MonthsPerYear));
        else if (key == "Enter") {
            this.dispatchEvent(YearListView.EventTypeYearListViewDidSelectMonth, this, this.highlightedMonth);
            this.hide();
            eventHandled = true;
        }
    } else if (key == "Up") {
        this.scrollView.scrollBy(-YearListCell.Height, true);
        eventHandled = true;
    } else if (key == "Down") {
        this.scrollView.scrollBy(YearListCell.Height, true);
        eventHandled = true;
    } else if (key == "PageUp") {
        this.scrollView.scrollBy(-this.scrollView.height(), true);
        eventHandled = true;
    } else if (key == "PageDown") {
        this.scrollView.scrollBy(this.scrollView.height(), true);
        eventHandled = true;
    }

    if (eventHandled) {
        event.stopPropagation();
        event.preventDefault();
    }
};

/**
 * @constructor
 * @extends View
 * @param {!Month} minimumMonth
 * @param {!Month} maximumMonth
 */
function MonthPopupView(minimumMonth, maximumMonth) {
    View.call(this, createElement("div", MonthPopupView.ClassNameMonthPopupView));

    /**
     * @type {!YearListView}
     * @const
     */
    this.yearListView = new YearListView(minimumMonth, maximumMonth);
    this.yearListView.attachTo(this);

    /**
     * @type {!boolean}
     */
    this.isVisible = false;

    this.element.addEventListener("click", this.onClick, false);
}

MonthPopupView.prototype = Object.create(View.prototype);

MonthPopupView.ClassNameMonthPopupView = "month-popup-view";

MonthPopupView.prototype.show = function(initialMonth, calendarTableRect) {
    this.isVisible = true;
    document.body.appendChild(this.element);
    this.yearListView.setWidth(calendarTableRect.width - 2);
    this.yearListView.setHeight(YearListView.Height);
    if (global.params.isLocaleRTL)
        this.yearListView.element.style.right = calendarTableRect.x + "px";
    else
        this.yearListView.element.style.left = calendarTableRect.x + "px";
    this.yearListView.element.style.top = calendarTableRect.y + "px";
    this.yearListView.show(initialMonth);
    this.yearListView.element.focus();
};

MonthPopupView.prototype.hide = function() {
    if (!this.isVisible)
        return;
    this.isVisible = false;
    this.element.parentNode.removeChild(this.element);
    this.yearListView.hide();
};

/**
 * @param {?Event} event
 */
MonthPopupView.prototype.onClick = function(event) {
    if (event.target !== this.element)
        return;
    this.hide();
};

/**
 * @constructor
 * @extends View
 * @param {!number} maxWidth Maximum width in pixels.
 */
function MonthPopupButton(maxWidth) {
    View.call(this, createElement("button", MonthPopupButton.ClassNameMonthPopupButton));
    this.element.setAttribute("aria-label", global.params.axShowMonthSelector);

    /**
     * @type {!Element}
     * @const
     */
    this.labelElement = createElement("span", MonthPopupButton.ClassNameMonthPopupButtonLabel, "-----");
    this.element.appendChild(this.labelElement);

    /**
     * @type {!Element}
     * @const
     */
    this.disclosureTriangleIcon = createElement("span", MonthPopupButton.ClassNameDisclosureTriangle);
    this.disclosureTriangleIcon.innerHTML = "<svg width='7' height='5'><polygon points='0,1 7,1 3.5,5' style='fill:#000000;' /></svg>";
    this.element.appendChild(this.disclosureTriangleIcon);

    /**
     * @type {!boolean}
     * @protected
     */
    this._useShortMonth = this._shouldUseShortMonth(maxWidth);
    this.element.style.maxWidth = maxWidth + "px";

    this.element.addEventListener("click", this.onClick, false);
}

MonthPopupButton.prototype = Object.create(View.prototype);

MonthPopupButton.ClassNameMonthPopupButton = "month-popup-button";
MonthPopupButton.ClassNameMonthPopupButtonLabel = "month-popup-button-label";
MonthPopupButton.ClassNameDisclosureTriangle = "disclosure-triangle";
MonthPopupButton.EventTypeButtonClick = "buttonClick";

/**
 * @param {!number} maxWidth Maximum available width in pixels.
 * @return {!boolean}
 */
MonthPopupButton.prototype._shouldUseShortMonth = function(maxWidth) {
    document.body.appendChild(this.element);
    var month = Month.Maximum;
    for (var i = 0; i < MonthsPerYear; ++i) {
        this.labelElement.textContent = month.toLocaleString();
        if (this.element.offsetWidth > maxWidth)
            return true;
        month = month.previous();
    }
    document.body.removeChild(this.element);
    return false;
};

/**
 * @param {!Month} month
 */
MonthPopupButton.prototype.setCurrentMonth = function(month) {
    this.labelElement.textContent = this._useShortMonth ? month.toShortLocaleString() : month.toLocaleString();
};

/**
 * @param {?Event} event
 */
MonthPopupButton.prototype.onClick = function(event) {
    this.dispatchEvent(MonthPopupButton.EventTypeButtonClick, this);
};

/**
 * @constructor
 * @extends View
 */
function CalendarNavigationButton() {
    View.call(this, createElement("button", CalendarNavigationButton.ClassNameCalendarNavigationButton));
    /**
     * @type {number} Threshold for starting repeating clicks in milliseconds.
     */
    this.repeatingClicksStartingThreshold = CalendarNavigationButton.DefaultRepeatingClicksStartingThreshold;
    /**
     * @type {number} Interval between reapeating clicks in milliseconds.
     */
    this.reapeatingClicksInterval = CalendarNavigationButton.DefaultRepeatingClicksInterval;
    /**
     * @type {?number} The ID for the timeout that triggers the repeating clicks.
     */
    this._timer = null;
    this.element.addEventListener("click", this.onClick, false);
    this.element.addEventListener("mousedown", this.onMouseDown, false);
    this.element.addEventListener("touchstart", this.onTouchStart, false);
};

CalendarNavigationButton.prototype = Object.create(View.prototype);

CalendarNavigationButton.DefaultRepeatingClicksStartingThreshold = 600;
CalendarNavigationButton.DefaultRepeatingClicksInterval = 300;
CalendarNavigationButton.LeftMargin = 4;
CalendarNavigationButton.Width = 24;
CalendarNavigationButton.ClassNameCalendarNavigationButton = "calendar-navigation-button";
CalendarNavigationButton.EventTypeButtonClick = "buttonClick";
CalendarNavigationButton.EventTypeRepeatingButtonClick = "repeatingButtonClick";

/**
 * @param {!boolean} disabled
 */
CalendarNavigationButton.prototype.setDisabled = function(disabled) {
    this.element.disabled = disabled;
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onClick = function(event) {
    this.dispatchEvent(CalendarNavigationButton.EventTypeButtonClick, this);
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onTouchStart = function(event) {
    if (this._timer !== null)
        return;
    this._timer = setTimeout(this.onRepeatingClick, this.repeatingClicksStartingThreshold);
    window.addEventListener("touchend", this.onWindowTouchEnd, false);
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onWindowTouchEnd = function(event) {
    if (this._timer === null)
        return;
    clearTimeout(this._timer);
    this._timer = null;
    window.removeEventListener("touchend", this.onWindowMouseUp, false);
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onMouseDown = function(event) {
    if (this._timer !== null)
        return;
    this._timer = setTimeout(this.onRepeatingClick, this.repeatingClicksStartingThreshold);
    window.addEventListener("mouseup", this.onWindowMouseUp, false);
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onWindowMouseUp = function(event) {
    if (this._timer === null)
        return;
    clearTimeout(this._timer);
    this._timer = null;
    window.removeEventListener("mouseup", this.onWindowMouseUp, false);
};

/**
 * @param {?Event} event
 */
CalendarNavigationButton.prototype.onRepeatingClick = function(event) {
    this.dispatchEvent(CalendarNavigationButton.EventTypeRepeatingButtonClick, this);
    this._timer = setTimeout(this.onRepeatingClick, this.reapeatingClicksInterval);
};

/**
 * @constructor
 * @extends View
 * @param {!CalendarPicker} calendarPicker
 */
function CalendarHeaderView(calendarPicker) {
    View.call(this, createElement("div", CalendarHeaderView.ClassNameCalendarHeaderView));
    this.calendarPicker = calendarPicker;
    this.calendarPicker.on(CalendarPicker.EventTypeCurrentMonthChanged, this.onCurrentMonthChanged);
    
    var titleElement = createElement("div", CalendarHeaderView.ClassNameCalendarTitle);
    this.element.appendChild(titleElement);

    /**
     * @type {!MonthPopupButton}
     */
    this.monthPopupButton = new MonthPopupButton(this.calendarPicker.calendarTableView.width() - CalendarTableView.BorderWidth * 2 - CalendarNavigationButton.Width * 3 - CalendarNavigationButton.LeftMargin * 2);
    this.monthPopupButton.attachTo(titleElement);

    /**
     * @type {!CalendarNavigationButton}
     * @const
     */
    this._previousMonthButton = new CalendarNavigationButton();
    this._previousMonthButton.attachTo(this);
    this._previousMonthButton.on(CalendarNavigationButton.EventTypeButtonClick, this.onNavigationButtonClick);
    this._previousMonthButton.on(CalendarNavigationButton.EventTypeRepeatingButtonClick, this.onNavigationButtonClick);
    this._previousMonthButton.element.setAttribute("aria-label", global.params.axShowPreviousMonth);

    /**
     * @type {!CalendarNavigationButton}
     * @const
     */
    this._todayButton = new CalendarNavigationButton();
    this._todayButton.attachTo(this);
    this._todayButton.on(CalendarNavigationButton.EventTypeButtonClick, this.onNavigationButtonClick);
    this._todayButton.element.classList.add(CalendarHeaderView.ClassNameTodayButton);
    var monthContainingToday = Month.createFromToday();
    this._todayButton.setDisabled(monthContainingToday < this.calendarPicker.minimumMonth || monthContainingToday > this.calendarPicker.maximumMonth);
    this._todayButton.element.setAttribute("aria-label", global.params.todayLabel);

    /**
     * @type {!CalendarNavigationButton}
     * @const
     */
    this._nextMonthButton = new CalendarNavigationButton();
    this._nextMonthButton.attachTo(this);
    this._nextMonthButton.on(CalendarNavigationButton.EventTypeButtonClick, this.onNavigationButtonClick);
    this._nextMonthButton.on(CalendarNavigationButton.EventTypeRepeatingButtonClick, this.onNavigationButtonClick);
    this._nextMonthButton.element.setAttribute("aria-label", global.params.axShowNextMonth);

    if (global.params.isLocaleRTL) {
        this._nextMonthButton.element.innerHTML = CalendarHeaderView._BackwardTriangle;
        this._previousMonthButton.element.innerHTML = CalendarHeaderView._ForwardTriangle;
    } else {
        this._nextMonthButton.element.innerHTML = CalendarHeaderView._ForwardTriangle;
        this._previousMonthButton.element.innerHTML = CalendarHeaderView._BackwardTriangle;
    }
}

CalendarHeaderView.prototype = Object.create(View.prototype);

CalendarHeaderView.Height = 24;
CalendarHeaderView.BottomMargin = 10;
CalendarHeaderView._ForwardTriangle = "<svg width='4' height='7'><polygon points='0,7 0,0, 4,3.5' style='fill:#6e6e6e;' /></svg>";
CalendarHeaderView._BackwardTriangle = "<svg width='4' height='7'><polygon points='0,3.5 4,7 4,0' style='fill:#6e6e6e;' /></svg>";
CalendarHeaderView.ClassNameCalendarHeaderView = "calendar-header-view";
CalendarHeaderView.ClassNameCalendarTitle = "calendar-title";
CalendarHeaderView.ClassNameTodayButton = "today-button";

CalendarHeaderView.prototype.onCurrentMonthChanged = function() {
    this.monthPopupButton.setCurrentMonth(this.calendarPicker.currentMonth());
    this._previousMonthButton.setDisabled(this.disabled || this.calendarPicker.currentMonth() <= this.calendarPicker.minimumMonth);
    this._nextMonthButton.setDisabled(this.disabled || this.calendarPicker.currentMonth() >= this.calendarPicker.maximumMonth);
};

CalendarHeaderView.prototype.onNavigationButtonClick = function(sender) {
    if (sender === this._previousMonthButton)
        this.calendarPicker.setCurrentMonth(this.calendarPicker.currentMonth().previous(), CalendarPicker.NavigationBehavior.WithAnimation);
    else if (sender === this._nextMonthButton)
        this.calendarPicker.setCurrentMonth(this.calendarPicker.currentMonth().next(), CalendarPicker.NavigationBehavior.WithAnimation);
    else
        this.calendarPicker.selectRangeContainingDay(Day.createFromToday());
};

/**
 * @param {!boolean} disabled
 */
CalendarHeaderView.prototype.setDisabled = function(disabled) {
    this.disabled = disabled;
    this.monthPopupButton.element.disabled = this.disabled;
    this._previousMonthButton.setDisabled(this.disabled || this.calendarPicker.currentMonth() <= this.calendarPicker.minimumMonth);
    this._nextMonthButton.setDisabled(this.disabled || this.calendarPicker.currentMonth() >= this.calendarPicker.maximumMonth);
    var monthContainingToday = Month.createFromToday();
    this._todayButton.setDisabled(this.disabled || monthContainingToday < this.calendarPicker.minimumMonth || monthContainingToday > this.calendarPicker.maximumMonth);
};

/**
 * @constructor
 * @extends ListCell
 */
function DayCell() {
    ListCell.call(this);
    this.element.classList.add(DayCell.ClassNameDayCell);
    this.element.style.width = DayCell.Width + "px";
    this.element.style.height = DayCell.Height + "px";
    this.element.style.lineHeight = (DayCell.Height - DayCell.PaddingSize * 2) + "px";
    this.element.setAttribute("role", "gridcell");
    /**
     * @type {?Day}
     */
    this.day = null;
};

DayCell.prototype = Object.create(ListCell.prototype);

DayCell.Width = 34;
DayCell.Height = hasInaccuratePointingDevice() ? 34 : 20;
DayCell.PaddingSize = 1;
DayCell.ClassNameDayCell = "day-cell";
DayCell.ClassNameHighlighted = "highlighted";
DayCell.ClassNameDisabled = "disabled";
DayCell.ClassNameCurrentMonth = "current-month";
DayCell.ClassNameToday = "today";

DayCell._recycleBin = [];

DayCell.recycleOrCreate = function() {
    return DayCell._recycleBin.pop() || new DayCell();
};

/**
 * @return {!Array}
 * @override
 */
DayCell.prototype._recycleBin = function() {
    return DayCell._recycleBin;
};

/**
 * @override
 */
DayCell.prototype.throwAway = function() {
    ListCell.prototype.throwAway.call(this);
    this.day = null;
};

/**
 * @param {!boolean} highlighted
 */
DayCell.prototype.setHighlighted = function(highlighted) {
    if (highlighted) {
        this.element.classList.add(DayCell.ClassNameHighlighted);
        this.element.setAttribute("aria-selected", "true");
    } else {
        this.element.classList.remove(DayCell.ClassNameHighlighted);
        this.element.setAttribute("aria-selected", "false");
    }
};

/**
 * @param {!boolean} disabled
 */
DayCell.prototype.setDisabled = function(disabled) {
    if (disabled)
        this.element.classList.add(DayCell.ClassNameDisabled);
    else
        this.element.classList.remove(DayCell.ClassNameDisabled);
};

/**
 * @param {!boolean} selected
 */
DayCell.prototype.setIsInCurrentMonth = function(selected) {
    if (selected)
        this.element.classList.add(DayCell.ClassNameCurrentMonth);
    else
        this.element.classList.remove(DayCell.ClassNameCurrentMonth);
};

/**
 * @param {!boolean} selected
 */
DayCell.prototype.setIsToday = function(selected) {
    if (selected)
        this.element.classList.add(DayCell.ClassNameToday);
    else
        this.element.classList.remove(DayCell.ClassNameToday);
};

/**
 * @param {!Day} day
 */
DayCell.prototype.reset = function(day) {
    this.day = day;
    this.element.textContent = localizeNumber(this.day.date.toString());
    this.element.setAttribute("aria-label", this.day.format());
    this.element.id = this.day.toString();
    this.show();
};

/**
 * @constructor
 * @extends ListCell
 */
function WeekNumberCell() {
    ListCell.call(this);
    this.element.classList.add(WeekNumberCell.ClassNameWeekNumberCell);
    this.element.style.width = (WeekNumberCell.Width - WeekNumberCell.SeparatorWidth) + "px";
    this.element.style.height = WeekNumberCell.Height + "px";
    this.element.style.lineHeight = (WeekNumberCell.Height - WeekNumberCell.PaddingSize * 2) + "px";
    /**
     * @type {?Week}
     */
    this.week = null;
};

WeekNumberCell.prototype = Object.create(ListCell.prototype);

WeekNumberCell.Width = 48;
WeekNumberCell.Height = DayCell.Height;
WeekNumberCell.SeparatorWidth = 1;
WeekNumberCell.PaddingSize = 1;
WeekNumberCell.ClassNameWeekNumberCell = "week-number-cell";
WeekNumberCell.ClassNameHighlighted = "highlighted";
WeekNumberCell.ClassNameDisabled = "disabled";

WeekNumberCell._recycleBin = [];

/**
 * @return {!Array}
 * @override
 */
WeekNumberCell.prototype._recycleBin = function() {
    return WeekNumberCell._recycleBin;
};

/**
 * @return {!WeekNumberCell}
 */
WeekNumberCell.recycleOrCreate = function() {
    return WeekNumberCell._recycleBin.pop() || new WeekNumberCell();
};

/**
 * @param {!Week} week
 */
WeekNumberCell.prototype.reset = function(week) {
    this.week = week;
    this.element.id = week.toString();
    this.element.setAttribute("role", "gridcell");
    this.element.setAttribute("aria-label", window.pagePopupController.formatWeek(week.year, week.week, week.firstDay().format()));
    this.element.textContent = localizeNumber(this.week.week.toString());
    this.show();
};

/**
 * @override
 */
WeekNumberCell.prototype.throwAway = function() {
    ListCell.prototype.throwAway.call(this);
    this.week = null;
};

WeekNumberCell.prototype.setHighlighted = function(highlighted) {
    if (highlighted) {
        this.element.classList.add(WeekNumberCell.ClassNameHighlighted);
        this.element.setAttribute("aria-selected", "true");
    } else {
        this.element.classList.remove(WeekNumberCell.ClassNameHighlighted);
        this.element.setAttribute("aria-selected", "false");
    }
};

WeekNumberCell.prototype.setDisabled = function(disabled) {
    if (disabled)
        this.element.classList.add(WeekNumberCell.ClassNameDisabled);
    else
        this.element.classList.remove(WeekNumberCell.ClassNameDisabled);
};

/**
 * @constructor
 * @extends View
 * @param {!boolean} hasWeekNumberColumn
 */
function CalendarTableHeaderView(hasWeekNumberColumn) {
    View.call(this, createElement("div", "calendar-table-header-view"));
    if (hasWeekNumberColumn) {
        var weekNumberLabelElement = createElement("div", "week-number-label", global.params.weekLabel);
        weekNumberLabelElement.style.width = WeekNumberCell.Width + "px";
        this.element.appendChild(weekNumberLabelElement);
    }
    for (var i = 0; i < DaysPerWeek; ++i) {
        var weekDayNumber = (global.params.weekStartDay + i) % DaysPerWeek;
        var labelElement = createElement("div", "week-day-label", global.params.dayLabels[weekDayNumber]);
        labelElement.style.width = DayCell.Width + "px";
        this.element.appendChild(labelElement);
        if (getLanguage() === "ja") {
            if (weekDayNumber === 0)
                labelElement.style.color = "red";
            else if (weekDayNumber === 6)
                labelElement.style.color = "blue";
        }
    }
}

CalendarTableHeaderView.prototype = Object.create(View.prototype);

CalendarTableHeaderView.Height = 25;

/**
 * @constructor
 * @extends ListCell
 */
function CalendarRowCell() {
    ListCell.call(this);
    this.element.classList.add(CalendarRowCell.ClassNameCalendarRowCell);
    this.element.style.height = CalendarRowCell.Height + "px";
    this.element.setAttribute("role", "row");

    /**
     * @type {!Array}
     * @protected
     */
    this._dayCells = [];
    /**
     * @type {!number}
     */
    this.row = 0;
    /**
     * @type {?CalendarTableView}
     */
    this.calendarTableView = null;
}

CalendarRowCell.prototype = Object.create(ListCell.prototype);

CalendarRowCell.Height = DayCell.Height;
CalendarRowCell.ClassNameCalendarRowCell = "calendar-row-cell";

CalendarRowCell._recycleBin = [];

/**
 * @return {!Array}
 * @override
 */
CalendarRowCell.prototype._recycleBin = function() {
    return CalendarRowCell._recycleBin;
};

/**
 * @param {!number} row
 * @param {!CalendarTableView} calendarTableView
 */
CalendarRowCell.prototype.reset = function(row, calendarTableView) {
    this.row = row;
    this.calendarTableView = calendarTableView;
    if (this.calendarTableView.hasWeekNumberColumn) {
        var middleDay = this.calendarTableView.dayAtColumnAndRow(3, row);
        var week = Week.createFromDay(middleDay);
        this.weekNumberCell = this.calendarTableView.prepareNewWeekNumberCell(week);
        this.weekNumberCell.attachTo(this);
    }
    var day = calendarTableView.dayAtColumnAndRow(0, row);
    for (var i = 0; i < DaysPerWeek; ++i) {
        var dayCell = this.calendarTableView.prepareNewDayCell(day);
        dayCell.attachTo(this);
        this._dayCells.push(dayCell);
        day = day.next();
    }
    this.show();
};

/**
 * @override
 */
CalendarRowCell.prototype.throwAway = function() {
    ListCell.prototype.throwAway.call(this);
    if (this.weekNumberCell)
        this.calendarTableView.throwAwayWeekNumberCell(this.weekNumberCell);
    this._dayCells.forEach(this.calendarTableView.throwAwayDayCell, this.calendarTableView);
    this._dayCells.length = 0;
};

/**
 * @constructor
 * @extends ListView
 * @param {!CalendarPicker} calendarPicker
 */
function CalendarTableView(calendarPicker) {
    ListView.call(this);
    this.element.classList.add(CalendarTableView.ClassNameCalendarTableView);
    this.element.tabIndex = 0;

    /**
     * @type {!boolean}
     * @const
     */
    this.hasWeekNumberColumn = calendarPicker.type === "week";
    /**
     * @type {!CalendarPicker}
     * @const
     */
    this.calendarPicker = calendarPicker;
    /**
     * @type {!Object}
     * @const
     */
    this._dayCells = {};
    var headerView = new CalendarTableHeaderView(this.hasWeekNumberColumn);
    headerView.attachTo(this, this.scrollView);

    if (this.hasWeekNumberColumn) {
        this.setWidth(DayCell.Width * DaysPerWeek + WeekNumberCell.Width);
        /**
         * @type {?Array}
         * @const
         */
        this._weekNumberCells = [];
    } else {
        this.setWidth(DayCell.Width * DaysPerWeek);
    }
    
    /**
     * @type {!boolean}
     * @protected
     */
    this._ignoreMouseOutUntillNextMouseOver = false;

    this.element.addEventListener("click", this.onClick, false);
    this.element.addEventListener("mouseover", this.onMouseOver, false);
    this.element.addEventListener("mouseout", this.onMouseOut, false);

    // You shouldn't be able to use the mouse wheel to scroll.
    this.scrollView.element.removeEventListener("mousewheel", this.scrollView.onMouseWheel, false);
    // You shouldn't be able to do gesture scroll.
    this.scrollView.element.removeEventListener("touchstart", this.scrollView.onTouchStart, false);
}

CalendarTableView.prototype = Object.create(ListView.prototype);

CalendarTableView.BorderWidth = 1;
CalendarTableView.ClassNameCalendarTableView = "calendar-table-view";

/**
 * @param {!number} scrollOffset
 * @return {!number}
 */
CalendarTableView.prototype.rowAtScrollOffset = function(scrollOffset) {
    return Math.floor(scrollOffset / CalendarRowCell.Height);
};

/**
 * @param {!number} row
 * @return {!number}
 */
CalendarTableView.prototype.scrollOffsetForRow = function(row) {
    return row * CalendarRowCell.Height;
};

/**
 * @param {?Event} event
 */
CalendarTableView.prototype.onClick = function(event) {
    if (this.hasWeekNumberColumn) {
        var weekNumberCellElement = enclosingNodeOrSelfWithClass(event.target, WeekNumberCell.ClassNameWeekNumberCell);
        if (weekNumberCellElement) {
            var weekNumberCell = weekNumberCellElement.$view;
            this.calendarPicker.selectRangeContainingDay(weekNumberCell.week.firstDay());
            return;
        }
    }
    var dayCellElement = enclosingNodeOrSelfWithClass(event.target, DayCell.ClassNameDayCell);
    if (!dayCellElement)
        return;
    var dayCell = dayCellElement.$view;
    this.calendarPicker.selectRangeContainingDay(dayCell.day);
};

/**
 * @param {?Event} event
 */
CalendarTableView.prototype.onMouseOver = function(event) {
    if (this.hasWeekNumberColumn) {
        var weekNumberCellElement = enclosingNodeOrSelfWithClass(event.target, WeekNumberCell.ClassNameWeekNumberCell);
        if (weekNumberCellElement) {
            var weekNumberCell = weekNumberCellElement.$view;
            this.calendarPicker.highlightRangeContainingDay(weekNumberCell.week.firstDay());
            this._ignoreMouseOutUntillNextMouseOver = false;
            return;
        }
    }
    var dayCellElement = enclosingNodeOrSelfWithClass(event.target, DayCell.ClassNameDayCell);
    if (!dayCellElement)
        return;
    var dayCell = dayCellElement.$view;
    this.calendarPicker.highlightRangeContainingDay(dayCell.day);
    this._ignoreMouseOutUntillNextMouseOver = false;
};

/**
 * @param {?Event} event
 */
CalendarTableView.prototype.onMouseOut = function(event) {
    if (this._ignoreMouseOutUntillNextMouseOver)
        return;
    var dayCellElement = enclosingNodeOrSelfWithClass(event.target, DayCell.ClassNameDayCell);
    if (!dayCellElement) {
        this.calendarPicker.highlightRangeContainingDay(null);
    }
};

/**
 * @param {!number} row
 * @return {!CalendarRowCell}
 */
CalendarTableView.prototype.prepareNewCell = function(row) {
    var cell = CalendarRowCell._recycleBin.pop() || new CalendarRowCell();
    cell.reset(row, this);
    return cell;
};

/**
 * @return {!number} Height in pixels.
 */
CalendarTableView.prototype.height = function() {
    return this.scrollView.height() + CalendarTableHeaderView.Height + CalendarTableView.BorderWidth * 2;
};

/**
 * @param {!number} height Height in pixels.
 */
CalendarTableView.prototype.setHeight = function(height) {
    this.scrollView.setHeight(height - CalendarTableHeaderView.Height - CalendarTableView.BorderWidth * 2);
};

/**
 * @param {!Month} month
 * @param {!boolean} animate
 */
CalendarTableView.prototype.scrollToMonth = function(month, animate) {
    var rowForFirstDayInMonth = this.columnAndRowForDay(month.firstDay()).row;
    this.scrollView.scrollTo(this.scrollOffsetForRow(rowForFirstDayInMonth), animate);
};

/**
 * @param {!number} column
 * @param {!number} row
 * @return {!Day}
 */
CalendarTableView.prototype.dayAtColumnAndRow = function(column, row) {
    var daysSinceMinimum = row * DaysPerWeek + column + global.params.weekStartDay - CalendarTableView._MinimumDayWeekDay;
    return Day.createFromValue(daysSinceMinimum * MillisecondsPerDay + CalendarTableView._MinimumDayValue);
};

CalendarTableView._MinimumDayValue = Day.Minimum.valueOf();
CalendarTableView._MinimumDayWeekDay = Day.Minimum.weekDay();

/**
 * @param {!Day} day
 * @return {!Object} Object with properties column and row.
 */
CalendarTableView.prototype.columnAndRowForDay = function(day) {
    var daysSinceMinimum = (day.valueOf() - CalendarTableView._MinimumDayValue) / MillisecondsPerDay;
    var offset = daysSinceMinimum + CalendarTableView._MinimumDayWeekDay - global.params.weekStartDay;
    var row = Math.floor(offset / DaysPerWeek);
    var column = offset - row * DaysPerWeek;
    return {
        column: column,
        row: row
    };
};

CalendarTableView.prototype.updateCells = function() {
    ListView.prototype.updateCells.call(this);

    var selection = this.calendarPicker.selection();
    var firstDayInSelection;
    var lastDayInSelection;
    if (selection) {
        firstDayInSelection = selection.firstDay().valueOf();
        lastDayInSelection = selection.lastDay().valueOf();
    } else {
        firstDayInSelection = Infinity;
        lastDayInSelection = Infinity;
    }
    var highlight = this.calendarPicker.highlight();
    var firstDayInHighlight;
    var lastDayInHighlight;
    if (highlight) {
        firstDayInHighlight = highlight.firstDay().valueOf();
        lastDayInHighlight = highlight.lastDay().valueOf();
    } else {
        firstDayInHighlight = Infinity;
        lastDayInHighlight = Infinity;
    }
    var currentMonth = this.calendarPicker.currentMonth();
    var firstDayInCurrentMonth = currentMonth.firstDay().valueOf();
    var lastDayInCurrentMonth = currentMonth.lastDay().valueOf();
    var activeCell = null;
    for (var dayString in this._dayCells) {
        var dayCell = this._dayCells[dayString];
        var day = dayCell.day;
        dayCell.setIsToday(Day.createFromToday().equals(day));
        dayCell.setSelected(day >= firstDayInSelection && day <= lastDayInSelection);
        var isHighlighted = day >= firstDayInHighlight && day <= lastDayInHighlight;
        dayCell.setHighlighted(isHighlighted);
        if (isHighlighted) {
            if (firstDayInHighlight == lastDayInHighlight)
                activeCell = dayCell;
            else if (this.calendarPicker.type == "month" && day == firstDayInHighlight)
                activeCell = dayCell;
        }
        dayCell.setIsInCurrentMonth(day >= firstDayInCurrentMonth && day <= lastDayInCurrentMonth);
        dayCell.setDisabled(!this.calendarPicker.isValidDay(day));
    }
    if (this.hasWeekNumberColumn) {
        for (var weekString in this._weekNumberCells) {
            var weekNumberCell = this._weekNumberCells[weekString];
            var week = weekNumberCell.week;
            var isWeekHighlighted = highlight && highlight.equals(week);
            weekNumberCell.setSelected(selection && selection.equals(week));
            weekNumberCell.setHighlighted(isWeekHighlighted);
            if (isWeekHighlighted)
                activeCell = weekNumberCell;
            weekNumberCell.setDisabled(!this.calendarPicker.isValid(week));
        }
    }
    if (activeCell) {
        // Ensure a layoutObject because an element with no layoutObject doesn't post
        // activedescendant events. This shouldn't run in the above |for| loop
        // to avoid CSS transition.
        activeCell.element.offsetLeft;
        this.element.setAttribute("aria-activedescendant", activeCell.element.id);
    }
};

/**
 * @param {!Day} day
 * @return {!DayCell}
 */
CalendarTableView.prototype.prepareNewDayCell = function(day) {
    var dayCell = DayCell.recycleOrCreate();
    dayCell.reset(day);
    if (this.calendarPicker.type == "month")
        dayCell.element.setAttribute("aria-label", Month.createFromDay(day).toLocaleString());
    this._dayCells[dayCell.day.toString()] = dayCell;
    return dayCell;
};

/**
 * @param {!Week} week
 * @return {!WeekNumberCell}
 */
CalendarTableView.prototype.prepareNewWeekNumberCell = function(week) {
    var weekNumberCell = WeekNumberCell.recycleOrCreate();
    weekNumberCell.reset(week);
    this._weekNumberCells[weekNumberCell.week.toString()] = weekNumberCell;
    return weekNumberCell;
};

/**
 * @param {!DayCell} dayCell
 */
CalendarTableView.prototype.throwAwayDayCell = function(dayCell) {
    delete this._dayCells[dayCell.day.toString()];
    dayCell.throwAway();
};

/**
 * @param {!WeekNumberCell} weekNumberCell
 */
CalendarTableView.prototype.throwAwayWeekNumberCell = function(weekNumberCell) {
    delete this._weekNumberCells[weekNumberCell.week.toString()];
    weekNumberCell.throwAway();
};

/**
 * @constructor
 * @extends View
 * @param {!Object} config
 */
function CalendarPicker(type, config) {
    View.call(this, createElement("div", CalendarPicker.ClassNameCalendarPicker));
    this.element.classList.add(CalendarPicker.ClassNamePreparing);

    /**
     * @type {!string}
     * @const
     */
    this.type = type;
    if (this.type === "week")
        this._dateTypeConstructor = Week;
    else if (this.type === "month")
        this._dateTypeConstructor = Month;
    else
        this._dateTypeConstructor = Day;
    /**
     * @type {!Object}
     * @const
     */
    this.config = {};
    this._setConfig(config);
    /**
     * @type {!Month}
     * @const
     */
    this.minimumMonth = Month.createFromDay(this.config.minimum.firstDay());
    /**
     * @type {!Month}
     * @const
     */
    this.maximumMonth = Month.createFromDay(this.config.maximum.lastDay());
    if (global.params.isLocaleRTL)
        this.element.classList.add("rtl");
    /**
     * @type {!CalendarTableView}
     * @const
     */
    this.calendarTableView = new CalendarTableView(this);
    this.calendarTableView.hasNumberColumn = this.type === "week";
    /**
     * @type {!CalendarHeaderView}
     * @const
     */
    this.calendarHeaderView = new CalendarHeaderView(this);
    this.calendarHeaderView.monthPopupButton.on(MonthPopupButton.EventTypeButtonClick, this.onMonthPopupButtonClick);
    /**
     * @type {!MonthPopupView}
     * @const
     */
    this.monthPopupView = new MonthPopupView(this.minimumMonth, this.maximumMonth);
    this.monthPopupView.yearListView.on(YearListView.EventTypeYearListViewDidSelectMonth, this.onYearListViewDidSelectMonth);
    this.monthPopupView.yearListView.on(YearListView.EventTypeYearListViewDidHide, this.onYearListViewDidHide);
    this.calendarHeaderView.attachTo(this);
    this.calendarTableView.attachTo(this);
    /**
     * @type {!Month}
     * @protected
     */
    this._currentMonth = new Month(NaN, NaN);
    /**
     * @type {?DateType}
     * @protected
     */
    this._selection = null;
    /**
     * @type {?DateType}
     * @protected
     */
    this._highlight = null;
    this.calendarTableView.element.addEventListener("keydown", this.onCalendarTableKeyDown, false);
    document.body.addEventListener("keydown", this.onBodyKeyDown, false);

    window.addEventListener("resize", this.onWindowResize, false);

    /**
     * @type {!number}
     * @protected
     */
    this._height = -1;

    var initialSelection = parseDateString(config.currentValue);
    if (initialSelection) {
        this.setCurrentMonth(Month.createFromDay(initialSelection.middleDay()), CalendarPicker.NavigationBehavior.None);
        this.setSelection(initialSelection);
    } else
        this.setCurrentMonth(Month.createFromToday(), CalendarPicker.NavigationBehavior.None);
}

CalendarPicker.prototype = Object.create(View.prototype);

CalendarPicker.Padding = 10;
CalendarPicker.BorderWidth = 1;
CalendarPicker.ClassNameCalendarPicker = "calendar-picker";
CalendarPicker.ClassNamePreparing = "preparing";
CalendarPicker.EventTypeCurrentMonthChanged = "currentMonthChanged";
CalendarPicker.commitDelayMs = 100;

/**
 * @param {!Event} event
 */
CalendarPicker.prototype.onWindowResize = function(event) {
    this.element.classList.remove(CalendarPicker.ClassNamePreparing);
    window.removeEventListener("resize", this.onWindowResize, false);
};

/**
 * @param {!YearListView} sender
 */
CalendarPicker.prototype.onYearListViewDidHide = function(sender) {
    this.monthPopupView.hide();
    this.calendarHeaderView.setDisabled(false);
    this.adjustHeight();
};

/**
 * @param {!YearListView} sender
 * @param {!Month} month
 */
CalendarPicker.prototype.onYearListViewDidSelectMonth = function(sender, month) {
    this.setCurrentMonth(month, CalendarPicker.NavigationBehavior.None);
};

/**
 * @param {!View|Node} parent
 * @param {?View|Node=} before
 * @override
 */
CalendarPicker.prototype.attachTo = function(parent, before) {
    View.prototype.attachTo.call(this, parent, before);
    this.calendarTableView.element.focus();
};

CalendarPicker.prototype.cleanup = function() {
    window.removeEventListener("resize", this.onWindowResize, false);
    this.calendarTableView.element.removeEventListener("keydown", this.onBodyKeyDown, false);
    // Month popup view might be attached to document.body.
    this.monthPopupView.hide();
};

/**
 * @param {?MonthPopupButton} sender
 */
CalendarPicker.prototype.onMonthPopupButtonClick = function(sender) {
    var clientRect = this.calendarTableView.element.getBoundingClientRect();
    var calendarTableRect = new Rectangle(clientRect.left + document.body.scrollLeft, clientRect.top + document.body.scrollTop, clientRect.width, clientRect.height);
    this.monthPopupView.show(this.currentMonth(), calendarTableRect);
    this.calendarHeaderView.setDisabled(true);
    this.adjustHeight();
};

CalendarPicker.prototype._setConfig = function(config) {
    this.config.minimum = (typeof config.min !== "undefined" && config.min) ? parseDateString(config.min) : this._dateTypeConstructor.Minimum;
    this.config.maximum = (typeof config.max !== "undefined" && config.max) ? parseDateString(config.max) : this._dateTypeConstructor.Maximum;
    this.config.minimumValue = this.config.minimum.valueOf();
    this.config.maximumValue = this.config.maximum.valueOf();
    this.config.step = (typeof config.step !== undefined) ? Number(config.step) : this._dateTypeConstructor.DefaultStep;
    this.config.stepBase = (typeof config.stepBase !== "undefined") ? Number(config.stepBase) : this._dateTypeConstructor.DefaultStepBase;
};

/**
 * @return {!Month}
 */
CalendarPicker.prototype.currentMonth = function() {
    return this._currentMonth;
};

/**
 * @enum {number}
 */
CalendarPicker.NavigationBehavior = {
    None: 0,
    WithAnimation: 1
};

/**
 * @param {!Month} month
 * @param {!CalendarPicker.NavigationBehavior} animate
 */
CalendarPicker.prototype.setCurrentMonth = function(month, behavior) {
    if (month > this.maximumMonth)
        month = this.maximumMonth;
    else if (month < this.minimumMonth)
        month = this.minimumMonth;
    if (this._currentMonth.equals(month))
        return;
    this._currentMonth = month;
    this.calendarTableView.scrollToMonth(this._currentMonth, behavior === CalendarPicker.NavigationBehavior.WithAnimation);
    this.adjustHeight();
    this.calendarTableView.setNeedsUpdateCells(true);
    this.dispatchEvent(CalendarPicker.EventTypeCurrentMonthChanged, {
        target: this
    });
};

CalendarPicker.prototype.adjustHeight = function() {
    var rowForFirstDayInMonth = this.calendarTableView.columnAndRowForDay(this._currentMonth.firstDay()).row;
    var rowForLastDayInMonth = this.calendarTableView.columnAndRowForDay(this._currentMonth.lastDay()).row;
    var numberOfRows = rowForLastDayInMonth - rowForFirstDayInMonth + 1;
    var calendarTableViewHeight = CalendarTableHeaderView.Height + numberOfRows * DayCell.Height + CalendarTableView.BorderWidth * 2;
    var height = (this.monthPopupView.isVisible ? YearListView.Height : calendarTableViewHeight) + CalendarHeaderView.Height + CalendarHeaderView.BottomMargin + CalendarPicker.Padding * 2 + CalendarPicker.BorderWidth * 2;
    this.setHeight(height);
};

CalendarPicker.prototype.selection = function() {
    return this._selection;
};

CalendarPicker.prototype.highlight = function() {
    return this._highlight;
};

/**
 * @return {!Day}
 */
CalendarPicker.prototype.firstVisibleDay = function() {
    var firstVisibleRow = this.calendarTableView.columnAndRowForDay(this.currentMonth().firstDay()).row;
    var firstVisibleDay = this.calendarTableView.dayAtColumnAndRow(0, firstVisibleRow);
    if (!firstVisibleDay)
        firstVisibleDay = Day.Minimum;
    return firstVisibleDay;
};

/**
 * @return {!Day}
 */
CalendarPicker.prototype.lastVisibleDay = function() { 
    var lastVisibleRow = this.calendarTableView.columnAndRowForDay(this.currentMonth().lastDay()).row;
    var lastVisibleDay = this.calendarTableView.dayAtColumnAndRow(DaysPerWeek - 1, lastVisibleRow);
    if (!lastVisibleDay)
        lastVisibleDay = Day.Maximum;
    return lastVisibleDay;
};

/**
 * @param {?Day} day
 */
CalendarPicker.prototype.selectRangeContainingDay = function(day) {
    var selection = day ? this._dateTypeConstructor.createFromDay(day) : null;
    this.setSelectionAndCommit(selection);
};

/**
 * @param {?Day} day
 */
CalendarPicker.prototype.highlightRangeContainingDay = function(day) {
    var highlight = day ? this._dateTypeConstructor.createFromDay(day) : null;
    this._setHighlight(highlight);
};

/**
 * Select the specified date.
 * @param {?DateType} dayOrWeekOrMonth
 */
CalendarPicker.prototype.setSelection = function(dayOrWeekOrMonth) {
    if (!this._selection && !dayOrWeekOrMonth)
        return;
    if (this._selection && this._selection.equals(dayOrWeekOrMonth))
        return;
    var firstDayInSelection = dayOrWeekOrMonth.firstDay();    
    var lastDayInSelection = dayOrWeekOrMonth.lastDay();
    var candidateCurrentMonth = Month.createFromDay(firstDayInSelection);
    if (this.firstVisibleDay() > lastDayInSelection || this.lastVisibleDay() < firstDayInSelection) {
        // Change current month if the selection is not visible at all.
        this.setCurrentMonth(candidateCurrentMonth, CalendarPicker.NavigationBehavior.WithAnimation);
    } else if (this.firstVisibleDay() < firstDayInSelection || this.lastVisibleDay() > lastDayInSelection) {
        // If the selection is partly visible, only change the current month if
        // doing so will make the whole selection visible.
        var firstVisibleRow = this.calendarTableView.columnAndRowForDay(candidateCurrentMonth.firstDay()).row;
        var firstVisibleDay = this.calendarTableView.dayAtColumnAndRow(0, firstVisibleRow);
        var lastVisibleRow = this.calendarTableView.columnAndRowForDay(candidateCurrentMonth.lastDay()).row;
        var lastVisibleDay = this.calendarTableView.dayAtColumnAndRow(DaysPerWeek - 1, lastVisibleRow);
        if (firstDayInSelection >= firstVisibleDay && lastDayInSelection <= lastVisibleDay)
            this.setCurrentMonth(candidateCurrentMonth, CalendarPicker.NavigationBehavior.WithAnimation);
    }
    this._setHighlight(dayOrWeekOrMonth);
    if (!this.isValid(dayOrWeekOrMonth))
        return;
    this._selection = dayOrWeekOrMonth;
    this.calendarTableView.setNeedsUpdateCells(true);
};

/**
 * Select the specified date, commit it, and close the popup.
 * @param {?DateType} dayOrWeekOrMonth
 */
CalendarPicker.prototype.setSelectionAndCommit = function(dayOrWeekOrMonth) {
    this.setSelection(dayOrWeekOrMonth);
    // Redraw the widget immidiately, and wait for some time to give feedback to
    // a user.
    this.element.offsetLeft;
    var value = this._selection.toString();
    if (CalendarPicker.commitDelayMs == 0) {
        // For testing.
        window.pagePopupController.setValueAndClosePopup(0, value);
    } else if (CalendarPicker.commitDelayMs < 0) {
        // For testing.
        window.pagePopupController.setValue(value);
    } else {
        setTimeout(function() {
            window.pagePopupController.setValueAndClosePopup(0, value);
        }, CalendarPicker.commitDelayMs);
    }
};

/**
 * @param {?DateType} dayOrWeekOrMonth
 */
CalendarPicker.prototype._setHighlight = function(dayOrWeekOrMonth) {
    if (!this._highlight && !dayOrWeekOrMonth)
        return;
    if (!dayOrWeekOrMonth && !this._highlight)
        return;
    if (this._highlight && this._highlight.equals(dayOrWeekOrMonth))
        return;
    this._highlight = dayOrWeekOrMonth;
    this.calendarTableView.setNeedsUpdateCells(true);
};

/**
 * @param {!number} value
 * @return {!boolean}
 */
CalendarPicker.prototype._stepMismatch = function(value) {
    var nextAllowedValue = Math.ceil((value - this.config.stepBase) / this.config.step) * this.config.step + this.config.stepBase;
    return nextAllowedValue >= value + this._dateTypeConstructor.DefaultStep;
};

/**
 * @param {!number} value
 * @return {!boolean}
 */
CalendarPicker.prototype._outOfRange = function(value) {
    return value < this.config.minimumValue || value > this.config.maximumValue;
};

/**
 * @param {!DateType} dayOrWeekOrMonth
 * @return {!boolean}
 */
CalendarPicker.prototype.isValid = function(dayOrWeekOrMonth) {
    var value = dayOrWeekOrMonth.valueOf();
    return dayOrWeekOrMonth instanceof this._dateTypeConstructor && !this._outOfRange(value) && !this._stepMismatch(value);
};

/**
 * @param {!Day} day
 * @return {!boolean}
 */
CalendarPicker.prototype.isValidDay = function(day) {
    return this.isValid(this._dateTypeConstructor.createFromDay(day));
};

/**
 * @param {!DateType} dateRange
 * @return {!boolean} Returns true if the highlight was changed.
 */
CalendarPicker.prototype._moveHighlight = function(dateRange) {
    if (!dateRange)
        return false;
    if (this._outOfRange(dateRange.valueOf()))
        return false;
    if (this.firstVisibleDay() > dateRange.middleDay() || this.lastVisibleDay() < dateRange.middleDay())
        this.setCurrentMonth(Month.createFromDay(dateRange.middleDay()), CalendarPicker.NavigationBehavior.WithAnimation);
    this._setHighlight(dateRange);
    return true;
};

/**
 * @param {?Event} event
 */
CalendarPicker.prototype.onCalendarTableKeyDown = function(event) {
    var key = event.keyIdentifier;
    var eventHandled = false;
    if (key == "U+0054") { // 't' key.
        this.selectRangeContainingDay(Day.createFromToday());
        eventHandled = true;
    } else if (key == "PageUp") {
        var previousMonth = this.currentMonth().previous();
        if (previousMonth && previousMonth >= this.config.minimumValue) {
            this.setCurrentMonth(previousMonth, CalendarPicker.NavigationBehavior.WithAnimation);
            eventHandled = true;
        }
    } else if (key == "PageDown") {
        var nextMonth = this.currentMonth().next();
        if (nextMonth && nextMonth >= this.config.minimumValue) {
            this.setCurrentMonth(nextMonth, CalendarPicker.NavigationBehavior.WithAnimation);
            eventHandled = true;
        }
    } else if (this._highlight) {
        if (global.params.isLocaleRTL ? key == "Right" : key == "Left") {
            eventHandled = this._moveHighlight(this._highlight.previous());
        } else if (key == "Up") {
            eventHandled = this._moveHighlight(this._highlight.previous(this.type === "date" ? DaysPerWeek : 1));
        } else if (global.params.isLocaleRTL ? key == "Left" : key == "Right") {
            eventHandled = this._moveHighlight(this._highlight.next());
        } else if (key == "Down") {
            eventHandled = this._moveHighlight(this._highlight.next(this.type === "date" ? DaysPerWeek : 1));
        } else if (key == "Enter") {
            this.setSelectionAndCommit(this._highlight);
        }
    } else if (key == "Left" || key == "Up" || key == "Right" || key == "Down") {
        // Highlight range near the middle.
        this.highlightRangeContainingDay(this.currentMonth().middleDay());
        eventHandled = true;
    }

    if (eventHandled) {
        event.stopPropagation();
        event.preventDefault();
    }
};

/**
 * @return {!number} Width in pixels.
 */
CalendarPicker.prototype.width = function() {
    return this.calendarTableView.width() + (CalendarTableView.BorderWidth + CalendarPicker.BorderWidth + CalendarPicker.Padding) * 2;
};

/**
 * @return {!number} Height in pixels.
 */
CalendarPicker.prototype.height = function() {
    return this._height;
};

/**
 * @param {!number} height Height in pixels.
 */
CalendarPicker.prototype.setHeight = function(height) {
    if (this._height === height)
        return;
    this._height = height;
    resizeWindow(this.width(), this._height);
    this.calendarTableView.setHeight(this._height - CalendarHeaderView.Height - CalendarHeaderView.BottomMargin - CalendarPicker.Padding * 2 - CalendarTableView.BorderWidth * 2);
};

/**
 * @param {?Event} event
 */
CalendarPicker.prototype.onBodyKeyDown = function(event) {
    var key = event.keyIdentifier;
    var eventHandled = false;
    var offset = 0;
    switch (key) {
    case "U+001B": // Esc key.
        window.pagePopupController.closePopup();
        eventHandled = true;
        break;
    case "U+004D": // 'm' key.
        offset = offset || 1; // Fall-through.
    case "U+0059": // 'y' key.
        offset = offset || MonthsPerYear; // Fall-through.
    case "U+0044": // 'd' key.
        offset = offset || MonthsPerYear * 10;
        var oldFirstVisibleRow = this.calendarTableView.columnAndRowForDay(this.currentMonth().firstDay()).row;
        this.setCurrentMonth(event.shiftKey ? this.currentMonth().previous(offset) : this.currentMonth().next(offset), CalendarPicker.NavigationBehavior.WithAnimation);
        var newFirstVisibleRow = this.calendarTableView.columnAndRowForDay(this.currentMonth().firstDay()).row;
        if (this._highlight) {
            var highlightMiddleDay = this._highlight.middleDay();
            this.highlightRangeContainingDay(highlightMiddleDay.next((newFirstVisibleRow - oldFirstVisibleRow) * DaysPerWeek));
        }
        eventHandled  =true;
        break;
    }
    if (eventHandled) {
        event.stopPropagation();
        event.preventDefault();
    }
};

if (window.dialogArguments) {
    initialize(dialogArguments);
} else {
    window.addEventListener("message", handleMessage, false);
}
/*
 * Copyright (C) 2013 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

input[type='button'],
button {
    -webkit-appearance: none;
    -webkit-user-select: none;
    background-image: linear-gradient(#ededed, #ededed 38%, #dedede);
    border: 1px solid rgba(0, 0, 0, 0.25);
    border-radius: 2px;
    box-shadow: 0 1px 0 rgba(0, 0, 0, 0.08),
    inset 0 1px 2px rgba(255, 255, 255, 0.75);
    color: #444;
    font: inherit;
    text-shadow: 0 1px 0 rgb(240, 240, 240);
    min-height: 2em;
    min-width: 4em;
    -webkit-padding-end: 10px;
    -webkit-padding-start: 10px;
    margin: 0;
}

:enabled:hover:-webkit-any(button, input[type='button']) {
    background-image: linear-gradient(#f0f0f0, #f0f0f0 38%, #e0e0e0);
    border-color: rgba(0, 0, 0, 0.3);
    box-shadow: 0 1px 0 rgba(0, 0, 0, 0.12), inset 0 1px 2px rgba(255, 255, 255, 0.95);
    color: black;
}

:enabled:active:-webkit-any(button, input[type='button']) {
    background-image: linear-gradient(#e7e7e7, #e7e7e7 38%, #d7d7d7);
    box-shadow: none;
    text-shadow: none;
}

:disabled:-webkit-any(button, input[type='button']) {
    background-image: linear-gradient(#f1f1f1, #f1f1f1 38%, #e6e6e6);
    border-color: rgba(80, 80, 80, 0.2);
    box-shadow: 0 1px 0 rgba(80, 80, 80, 0.08), inset 0 1px 2px rgba(255, 255, 255, 0.75);
    color: #aaa;
}

:enabled:focus:-webkit-any(button, input[type='button']) {
    transition: border-color 200ms;
    /* We use border color because it follows the border radius (unlike outline).
    * This is particularly noticeable on mac. */
    border-color: rgb(77, 144, 254);
    outline: none;
}
.suggestion-list {
    list-style: none;
    padding: 0;
    margin: 0;
    font: -webkit-small-control;
    border: 1px solid #7f9db9;
    background-color: white;
    overflow: hidden;
}

.suggestion-list-entry {
    white-space: nowrap;
    height: 1.73em;
    line-height: 1.73em;
    -webkit-select: none;
    cursor: default;
}

.suggestion-list-entry:focus {
    outline: none;
}

.suggestion-list-entry .content {
    padding: 0 4px;
}

.suggestion-list-entry .label {
    text-align: right;
    color: #737373;
    float: right;
    padding: 0 4px 0 20px;
}

.rtl .suggestion-list-entry .label {
    float: left;
    padding: 0 20px 0 4px;
}

.suggestion-list-entry .title {
    direction: ltr;
    display: inline-block;
}

.locale-rtl .suggestion-list-entry .title {
    direction: rtl;
}

.measuring-width .suggestion-list-entry .label {
    float: none;
    margin-right: 0;
}

.suggestion-list .separator {
    border-top: 1px solid #dcdcdc;
    height: 0;
}
"use strict";
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

/**
 * @constructor
 * @param {!Element} element
 * @param {!Object} config
 */
function SuggestionPicker(element, config) {
    Picker.call(this, element, config);
    this._isFocusByMouse = false;
    this._containerElement = null;
    this._setColors();
    this._layout();
    this._fixWindowSize();
    this._handleBodyKeyDownBound = this._handleBodyKeyDown.bind(this);
    document.body.addEventListener("keydown", this._handleBodyKeyDownBound);
    this._element.addEventListener("mouseout", this._handleMouseOut.bind(this), false);
}
SuggestionPicker.prototype = Object.create(Picker.prototype);

SuggestionPicker.NumberOfVisibleEntries = 20;

// An entry needs to be at least this many pixels visible for it to be a visible entry.
SuggestionPicker.VisibleEntryThresholdHeight = 4;

SuggestionPicker.ActionNames = {
    OpenCalendarPicker: "openCalendarPicker"
};

SuggestionPicker.ListEntryClass = "suggestion-list-entry";

SuggestionPicker.validateConfig = function(config) {
    if (config.showOtherDateEntry && !config.otherDateLabel)
        return "No otherDateLabel.";
    if (config.suggestionHighlightColor && !config.suggestionHighlightColor)
        return "No suggestionHighlightColor.";
    if (config.suggestionHighlightTextColor && !config.suggestionHighlightTextColor)
        return "No suggestionHighlightTextColor.";
    if (config.suggestionValues.length !== config.localizedSuggestionValues.length)
        return "localizedSuggestionValues.length must equal suggestionValues.length.";
    if (config.suggestionValues.length !== config.suggestionLabels.length)
        return "suggestionLabels.length must equal suggestionValues.length.";
    if (typeof config.inputWidth === "undefined")
        return "No inputWidth.";
    return null;
};

SuggestionPicker.prototype._setColors = function() {
    var text = "." + SuggestionPicker.ListEntryClass + ":focus {\
        background-color: " + this._config.suggestionHighlightColor + ";\
        color: " + this._config.suggestionHighlightTextColor + "; }";
    text += "." + SuggestionPicker.ListEntryClass + ":focus .label { color: " + this._config.suggestionHighlightTextColor + "; }";
    document.head.appendChild(createElement("style", null, text));
};

SuggestionPicker.prototype.cleanup = function() {
    document.body.removeEventListener("keydown", this._handleBodyKeyDownBound, false);
};

/**
 * @param {!string} title
 * @param {!string} label
 * @param {!string} value
 * @return {!Element}
 */
SuggestionPicker.prototype._createSuggestionEntryElement = function(title, label, value) {
    var entryElement = createElement("li", SuggestionPicker.ListEntryClass);
    entryElement.tabIndex = 0;
    entryElement.dataset.value = value;
    var content = createElement("span", "content");
    entryElement.appendChild(content);
    var titleElement = createElement("span", "title", title);
    content.appendChild(titleElement);
    if (label) {
        var labelElement = createElement("span", "label", label);
        content.appendChild(labelElement);
    }
    entryElement.addEventListener("mouseover", this._handleEntryMouseOver.bind(this), false);
    return entryElement;
};

/**
 * @param {!string} title
 * @param {!string} actionName
 * @return {!Element}
 */
SuggestionPicker.prototype._createActionEntryElement = function(title, actionName) {
    var entryElement = createElement("li", SuggestionPicker.ListEntryClass);
    entryElement.tabIndex = 0;
    entryElement.dataset.action = actionName;
    var content = createElement("span", "content");
    entryElement.appendChild(content);
    var titleElement = createElement("span", "title", title);
    content.appendChild(titleElement);
    entryElement.addEventListener("mouseover", this._handleEntryMouseOver.bind(this), false);
    return entryElement;
};

/**
* @return {!number}
*/
SuggestionPicker.prototype._measureMaxContentWidth = function() {
    // To measure the required width, we first set the class to "measuring-width" which
    // left aligns all the content including label.
    this._containerElement.classList.add("measuring-width");
    var maxContentWidth = 0;
    var contentElements = this._containerElement.getElementsByClassName("content");
    for (var i=0; i < contentElements.length; ++i) {
        maxContentWidth = Math.max(maxContentWidth, contentElements[i].offsetWidth);
    }
    this._containerElement.classList.remove("measuring-width");
    return maxContentWidth;
};

SuggestionPicker.prototype._fixWindowSize = function() {
    var ListBorder = 2;
    var zoom = this._config.zoomFactor;
    var desiredWindowWidth = (this._measureMaxContentWidth() + ListBorder) * zoom;
    if (typeof this._config.inputWidth === "number")
        desiredWindowWidth = Math.max(this._config.inputWidth, desiredWindowWidth);
    var totalHeight = ListBorder;
    var maxHeight = 0;
    var entryCount = 0;
    for (var i = 0; i < this._containerElement.childNodes.length; ++i) {
        var node = this._containerElement.childNodes[i];
        if (node.classList.contains(SuggestionPicker.ListEntryClass))
            entryCount++;
        totalHeight += node.offsetHeight;
        if (maxHeight === 0 && entryCount == SuggestionPicker.NumberOfVisibleEntries)
            maxHeight = totalHeight;
    }
    var desiredWindowHeight = totalHeight * zoom;
    if (maxHeight !== 0 && totalHeight > maxHeight * zoom) {
        this._containerElement.style.maxHeight = (maxHeight - ListBorder) + "px";
        desiredWindowWidth += getScrollbarWidth() * zoom;
        desiredWindowHeight = maxHeight * zoom;
        this._containerElement.style.overflowY = "scroll";
    }
    var windowRect = adjustWindowRect(desiredWindowWidth, desiredWindowHeight, desiredWindowWidth, 0);
    this._containerElement.style.height = (windowRect.height / zoom - ListBorder) + "px";
    setWindowRect(windowRect);
};

SuggestionPicker.prototype._layout = function() {
    if (this._config.isRTL)
        this._element.classList.add("rtl");
    if (this._config.isLocaleRTL)
        this._element.classList.add("locale-rtl");
    this._containerElement = createElement("ul", "suggestion-list");
    this._containerElement.addEventListener("click", this._handleEntryClick.bind(this), false);
    for (var i = 0; i < this._config.suggestionValues.length; ++i) {
        this._containerElement.appendChild(this._createSuggestionEntryElement(this._config.localizedSuggestionValues[i], this._config.suggestionLabels[i], this._config.suggestionValues[i]));
    }
    if (this._config.showOtherDateEntry) {
        // Add separator
        var separator = createElement("div", "separator");
        this._containerElement.appendChild(separator);

        // Add "Other..." entry
        var otherEntry = this._createActionEntryElement(this._config.otherDateLabel, SuggestionPicker.ActionNames.OpenCalendarPicker);
        this._containerElement.appendChild(otherEntry);
    }
    this._element.appendChild(this._containerElement);
};

/**
 * @param {!Element} entry
 */
SuggestionPicker.prototype.selectEntry = function(entry) {
    if (typeof entry.dataset.value !== "undefined") {
        this.submitValue(entry.dataset.value);
    } else if (entry.dataset.action === SuggestionPicker.ActionNames.OpenCalendarPicker) {
        window.addEventListener("didHide", SuggestionPicker._handleWindowDidHide, false);
        hideWindow();
    }
};

SuggestionPicker._handleWindowDidHide = function() {
    openCalendarPicker();
    window.removeEventListener("didHide", SuggestionPicker._handleWindowDidHide);
};

/**
 * @param {!Event} event
 */
SuggestionPicker.prototype._handleEntryClick = function(event) {
    var entry = enclosingNodeOrSelfWithClass(event.target, SuggestionPicker.ListEntryClass);
    if (!entry)
        return;
    this.selectEntry(entry);
    event.preventDefault();
};

/**
 * @return {?Element}
 */
SuggestionPicker.prototype._findFirstVisibleEntry = function() {
    var scrollTop = this._containerElement.scrollTop;
    var childNodes = this._containerElement.childNodes;
    for (var i = 0; i < childNodes.length; ++i) {
        var node = childNodes[i];
        if (node.nodeType !== Node.ELEMENT_NODE || !node.classList.contains(SuggestionPicker.ListEntryClass))
            continue;
        if (node.offsetTop + node.offsetHeight - scrollTop > SuggestionPicker.VisibleEntryThresholdHeight)
            return node;
    }
    return null;
};

/**
 * @return {?Element}
 */
SuggestionPicker.prototype._findLastVisibleEntry = function() {
    var scrollBottom = this._containerElement.scrollTop + this._containerElement.offsetHeight;
    var childNodes = this._containerElement.childNodes;
    for (var i = childNodes.length - 1; i >= 0; --i){
        var node = childNodes[i];
        if (node.nodeType !== Node.ELEMENT_NODE || !node.classList.contains(SuggestionPicker.ListEntryClass))
            continue;
        if (scrollBottom - node.offsetTop > SuggestionPicker.VisibleEntryThresholdHeight)
            return node;
    }
    return null;
};

/**
 * @param {!Event} event
 */
SuggestionPicker.prototype._handleBodyKeyDown = function(event) {
    var eventHandled = false;
    var key = event.keyIdentifier;
    if (key === "U+001B") { // ESC
        this.handleCancel();
        eventHandled = true;
    } else if (key == "Up") {
        if (document.activeElement && document.activeElement.classList.contains(SuggestionPicker.ListEntryClass)) {
            for (var node = document.activeElement.previousElementSibling; node; node = node.previousElementSibling) {
                if (node.classList.contains(SuggestionPicker.ListEntryClass)) {
                    this._isFocusByMouse = false;
                    node.focus();
                    break;
                }
            }
        } else {
            this._element.querySelector("." + SuggestionPicker.ListEntryClass + ":last-child").focus();
        }
        eventHandled = true;
    } else if (key == "Down") {
        if (document.activeElement && document.activeElement.classList.contains(SuggestionPicker.ListEntryClass)) {
            for (var node = document.activeElement.nextElementSibling; node; node = node.nextElementSibling) {
                if (node.classList.contains(SuggestionPicker.ListEntryClass)) {
                    this._isFocusByMouse = false;
                    node.focus();
                    break;
                }
            }
        } else {
            this._element.querySelector("." + SuggestionPicker.ListEntryClass + ":first-child").focus();
        }
        eventHandled = true;
    } else if (key === "Enter") {
        this.selectEntry(document.activeElement);
        eventHandled = true;
    } else if (key === "PageUp") {
        this._containerElement.scrollTop -= this._containerElement.clientHeight;
        // Scrolling causes mouseover event to be called and that tries to move the focus too.
        // To prevent flickering we won't focus if the current focus was caused by the mouse.
        if (!this._isFocusByMouse)
            this._findFirstVisibleEntry().focus();
        eventHandled = true;
    } else if (key === "PageDown") {
        this._containerElement.scrollTop += this._containerElement.clientHeight;
        if (!this._isFocusByMouse)
            this._findLastVisibleEntry().focus();
        eventHandled = true;
    }
    if (eventHandled)
        event.preventDefault();
};

/**
 * @param {!Event} event
 */
SuggestionPicker.prototype._handleEntryMouseOver = function(event) {
    var entry = enclosingNodeOrSelfWithClass(event.target, SuggestionPicker.ListEntryClass);
    if (!entry)
        return;
    this._isFocusByMouse = true;
    entry.focus();
    event.preventDefault();
};

/**
 * @param {!Event} event
 */
SuggestionPicker.prototype._handleMouseOut = function(event) {
    if (!document.activeElement.classList.contains(SuggestionPicker.ListEntryClass))
        return;
    this._isFocusByMouse = false;
    document.activeElement.blur();
    event.preventDefault();
};
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

body {
    -webkit-user-select: none;
    background-color: white;
    font: -webkit-small-control;
    margin: 0;
    overflow: hidden;
}

#main {
    background-color: white;
    border: solid 1px #8899aa;
    box-shadow: inset 2px 2px 2px white,
        inset -2px -2px 1px rgba(0,0,0,0.1);
    padding: 6px;
    float: left;
}

.color-swatch {
    float: left;
    width: 20px;
    height: 20px;
    margin: 1px;
    padding: 0;
    border: 1px solid #e0e0e0;
    box-sizing: content-box;
}

.color-swatch:focus {
    border: 1px solid #000000;
    outline: none;
}

.color-swatch-container {
    width: 100%;
    max-height: 104px;
    overflow: auto;
    display: flex;
    flex-flow: row wrap;
    align-items: center;
}

.other-color {
    width: 100%;
    margin: 4px 0 0 0;
}
"use strict";
/*
 * Copyright (C) 2012 Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY APPLE INC. AND ITS CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL APPLE INC. OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

var global = {
    argumentsReceived: false,
    params: null
};

/**
 * @param {Event} event
 */
function handleMessage(event) {
    initialize(JSON.parse(event.data));
    global.argumentsReceived = true;
}

/**
 * @param {!Object} args
 */
function initialize(args) { 
    global.params = args;
    var main = $("main");
    main.innerHTML = "";
    var errorString = validateArguments(args);
    if (errorString) {
        main.textContent = "Internal error: " + errorString;
        resizeWindow(main.offsetWidth, main.offsetHeight);
    } else
        new ColorPicker(main, args);
}

// The DefaultColorPalette is used when the list of values are empty. 
var DefaultColorPalette = ["#000000", "#404040", "#808080", "#c0c0c0",
    "#ffffff", "#980000", "#ff0000", "#ff9900", "#ffff00", "#00ff00", "#00ffff",
    "#4a86e8", "#0000ff", "#9900ff", "#ff00ff"];

function handleArgumentsTimeout() {
    if (global.argumentsReceived)
        return;
    var args = {
        values : DefaultColorPalette,
        otherColorLabel: "Other..."
    };
    initialize(args);
}

/**
 * @param {!Object} args
 * @return {?string} An error message, or null if the argument has no errors.
 */
function validateArguments(args) {
    if (!args.values)
        return "No values.";
    if (!args.otherColorLabel)
        return "No otherColorLabel.";
    return null;
}

function ColorPicker(element, config) {
    Picker.call(this, element, config);
    this._config = config;
    if (this._config.values.length === 0)
        this._config.values = DefaultColorPalette;
    this._container = null;
    this._layout();
    document.body.addEventListener("keydown", this._handleKeyDown.bind(this));
    this._element.addEventListener("mousemove", this._handleMouseMove.bind(this));
    this._element.addEventListener("mousedown", this._handleMouseDown.bind(this));
}
ColorPicker.prototype = Object.create(Picker.prototype);

var SwatchBorderBoxWidth = 24; // keep in sync with CSS
var SwatchBorderBoxHeight = 24; // keep in sync with CSS
var SwatchesPerRow = 5;
var SwatchesMaxRow = 4;

ColorPicker.prototype._layout = function() {
    var container = createElement("div", "color-swatch-container");
    container.addEventListener("click", this._handleSwatchClick.bind(this), false);
    for (var i = 0; i < this._config.values.length; ++i) {
        var swatch = createElement("button", "color-swatch");
        swatch.dataset.index = i;
        swatch.dataset.value = this._config.values[i];
        swatch.title = this._config.values[i];
        swatch.style.backgroundColor = this._config.values[i];
        container.appendChild(swatch);
    }
    var containerWidth = SwatchBorderBoxWidth * SwatchesPerRow;
    if (this._config.values.length > SwatchesPerRow * SwatchesMaxRow)
        containerWidth += getScrollbarWidth();
    container.style.width = containerWidth + "px";
    container.style.maxHeight = (SwatchBorderBoxHeight * SwatchesMaxRow) + "px";
    this._element.appendChild(container);
    var otherButton = createElement("button", "other-color", this._config.otherColorLabel);
    otherButton.addEventListener("click", this.chooseOtherColor.bind(this), false);
    this._element.appendChild(otherButton);
    this._container = container;
    this._otherButton = otherButton;
    var elementWidth = this._element.offsetWidth;
    var elementHeight = this._element.offsetHeight;
    resizeWindow(elementWidth, elementHeight);
};

ColorPicker.prototype.selectColorAtIndex = function(index) {
    index = Math.max(Math.min(this._container.childNodes.length - 1, index), 0);
    this._container.childNodes[index].focus();
};

ColorPicker.prototype._handleMouseMove = function(event) {
    if (event.target.classList.contains("color-swatch"))
        event.target.focus();
};

ColorPicker.prototype._handleMouseDown = function(event) {
    // Prevent blur.
    if (event.target.classList.contains("color-swatch"))
        event.preventDefault();
};

ColorPicker.prototype._handleKeyDown = function(event) {
    var key = event.keyIdentifier;
    if (key === "U+001B") // ESC
        this.handleCancel();
    else if (key == "Left" || key == "Up" || key == "Right" || key == "Down") {
        var selectedElement = document.activeElement;
        var index = 0;
        if (selectedElement.classList.contains("other-color")) {
            if (key != "Right" && key != "Up")
                return;
            index = this._container.childNodes.length - 1;
        } else if (selectedElement.classList.contains("color-swatch")) {
            index = parseInt(selectedElement.dataset.index, 10);
            switch (key) {
            case "Left":
                index--;
                break;
            case "Right":
                index++;
                break;
            case "Up":
                index -= SwatchesPerRow;
                break;
            case "Down":
                index += SwatchesPerRow;
                break;
            }
            if (index > this._container.childNodes.length - 1) {
                this._otherButton.focus();
                return;
            }
        }
        this.selectColorAtIndex(index);
    }
    event.preventDefault();
};

ColorPicker.prototype._handleSwatchClick = function(event) {
    if (event.target.classList.contains("color-swatch"))
        this.submitValue(event.target.dataset.value);
};

if (window.dialogArguments) {
    initialize(dialogArguments);
} else {
    window.addEventListener("message", handleMessage, false);
    window.setTimeout(handleArgumentsTimeout, 1000);
}
select {
    display: block;
    overflow-y: auto;
}

select hr {
    border-style: none;
    border-bottom: 1px solid black;
    margin: 0 0.5em;
}

option, optgroup {
    -webkit-padding-end: 2px;
}

.wrap option {
    white-space: pre-wrap;
}
"use strict";
// Copyright (c) 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

var global = {
    argumentsReceived: false,
    params: null,
    picker: null
};

/**
 * @param {Event} event
 */
function handleMessage(event) {
    window.removeEventListener("message", handleMessage, false);
    initialize(JSON.parse(event.data));
    global.argumentsReceived = true;
}

/**
 * @param {!Object} args
 */
function initialize(args) {
    global.params = args;
    var main = $("main");
    main.innerHTML = "";
    global.picker = new ListPicker(main, args);
}

function handleArgumentsTimeout() {
    if (global.argumentsReceived)
        return;
    initialize({});
}

/**
 * @constructor
 * @param {!Element} element
 * @param {!Object} config
 */
function ListPicker(element, config) {
    Picker.call(this, element, config);
    window.pagePopupController.selectFontsFromOwnerDocument(document);
    this._selectElement = createElement("select");
    this._selectElement.size = 20;
    this._element.appendChild(this._selectElement);
    this._delayedChildrenConfig = null;
    this._delayedChildrenConfigIndex = 0;
    this._layout();
    this._selectElement.addEventListener("mouseup", this._handleMouseUp.bind(this), false);
    this._selectElement.addEventListener("touchstart", this._handleTouchStart.bind(this), false);
    this._selectElement.addEventListener("keydown", this._handleKeyDown.bind(this), false);
    this._selectElement.addEventListener("change", this._handleChange.bind(this), false);
    window.addEventListener("message", this._handleWindowMessage.bind(this), false);
    window.addEventListener("mousemove", this._handleWindowMouseMove.bind(this), false);
    this._handleWindowTouchMoveBound = this._handleWindowTouchMove.bind(this);
    this._handleWindowTouchEndBound = this._handleWindowTouchEnd.bind(this);
    this._handleTouchSelectModeScrollBound = this._handleTouchSelectModeScroll.bind(this);
    this.lastMousePositionX = Infinity;
    this.lastMousePositionY = Infinity;
    this._selectionSetByMouseHover = false;

    this._trackingTouchId = null;

    this._handleWindowDidHide();
    this._selectElement.focus();
    this._selectElement.value = this._config.selectedIndex;
}
ListPicker.prototype = Object.create(Picker.prototype);

ListPicker.prototype._handleWindowDidHide = function() {
    this._fixWindowSize();
    var selectedOption = this._selectElement.options[this._selectElement.selectedIndex];
    if (selectedOption)
        selectedOption.scrollIntoView(false);
    window.removeEventListener("didHide", this._handleWindowDidHideBound, false);
};

ListPicker.prototype._handleWindowMessage = function(event) {
    eval(event.data);
    if (window.updateData.type === "update") {
        this._config.baseStyle = window.updateData.baseStyle;
        this._config.children = window.updateData.children;
        this._update();
        if (this._config.anchorRectInScreen.x !== window.updateData.anchorRectInScreen.x ||
            this._config.anchorRectInScreen.y !== window.updateData.anchorRectInScreen.y ||
            this._config.anchorRectInScreen.width !== window.updateData.anchorRectInScreen.width ||
            this._config.anchorRectInScreen.height !== window.updateData.anchorRectInScreen.height) {
            this._config.anchorRectInScreen = window.updateData.anchorRectInScreen;
            this._fixWindowSize();
        }
    }
    delete window.updateData;
};

// This should be matched to the border width of the internal listbox
// SELECT. See listPicker.css and html.css.
ListPicker.ListboxSelectBorder = 1;

ListPicker.prototype._handleWindowMouseMove = function (event) {
    var visibleTop = ListPicker.ListboxSelectBorder;
    var visibleBottom = this._selectElement.offsetHeight - ListPicker.ListboxSelectBorder;
    var optionBounds = event.target.getBoundingClientRect();
    if (optionBounds.height >= 1.0) {
        // If the height of the visible part of event.target is less than 1px,
        // ignore this event because it may be an error by sub-pixel layout.
        if (optionBounds.top < visibleTop) {
            if (optionBounds.bottom - visibleTop < 1.0)
                return;
        } else if (optionBounds.bottom > visibleBottom) {
            if (visibleBottom - optionBounds.top < 1.0)
                return;
        }
    }
    this.lastMousePositionX = event.clientX;
    this.lastMousePositionY = event.clientY;
    this._highlightOption(event.target);
    this._selectionSetByMouseHover = true;
    // Prevent the select element from firing change events for mouse input.
    event.preventDefault();
};

ListPicker.prototype._handleMouseUp = function(event) {
    if (event.target.tagName !== "OPTION")
        return;
    window.pagePopupController.setValueAndClosePopup(0, this._selectElement.value);
};

ListPicker.prototype._handleTouchStart = function(event) {
    if (this._trackingTouchId !== null)
        return;
    // Enter touch select mode. In touch select mode the highlight follows the
    // finger and on touchend the highlighted item is selected.
    var touch = event.touches[0];
    this._trackingTouchId = touch.identifier;
    this._highlightOption(touch.target);
    this._selectionSetByMouseHover = false;
    this._selectElement.addEventListener("scroll", this._handleTouchSelectModeScrollBound, false);
    window.addEventListener("touchmove", this._handleWindowTouchMoveBound, false);
    window.addEventListener("touchend", this._handleWindowTouchEndBound, false);
};

ListPicker.prototype._handleTouchSelectModeScroll = function(event) {
    this._exitTouchSelectMode();
};

ListPicker.prototype._exitTouchSelectMode = function(event) {
    this._trackingTouchId = null;
    this._selectElement.removeEventListener("scroll", this._handleTouchSelectModeScrollBound, false);
    window.removeEventListener("touchmove", this._handleWindowTouchMoveBound, false);
    window.removeEventListener("touchend", this._handleWindowTouchEndBound, false);
};

ListPicker.prototype._handleWindowTouchMove = function(event) {
    if (this._trackingTouchId === null)
        return;
    var touch = this._getTouchForId(event.touches, this._trackingTouchId);
    if (!touch)
        return;
    this._highlightOption(document.elementFromPoint(touch.clientX, touch.clientY));
    this._selectionSetByMouseHover = false;
};

ListPicker.prototype._handleWindowTouchEnd = function(event) {
    if (this._trackingTouchId === null)
        return;
    var touch = this._getTouchForId(event.changedTouches, this._trackingTouchId);
    if (!touch)
        return;
    var target = document.elementFromPoint(touch.clientX, touch.clientY)
    if (target.tagName === "OPTION")
        window.pagePopupController.setValueAndClosePopup(0, this._selectElement.value);
    this._exitTouchSelectMode();
};

ListPicker.prototype._getTouchForId = function (touchList, id) {
    for (var i = 0; i < touchList.length; i++) {
        if (touchList[i].identifier === id)
            return touchList[i];
    }
    return null;
};

ListPicker.prototype._highlightOption = function(target) {
    if (target.tagName !== "OPTION" || target.selected)
        return;
    var savedScrollTop = this._selectElement.scrollTop;
    // TODO(tkent): Updating HTMLOptionElement::selected is not efficient. We
    // should optimize it, or use an alternative way.
    target.selected = true;
    this._selectElement.scrollTop = savedScrollTop;
};

ListPicker.prototype._handleChange = function(event) {
    window.pagePopupController.setValue(this._selectElement.value);
    this._selectionSetByMouseHover = false;
};

ListPicker.prototype._handleKeyDown = function(event) {
    var key = event.keyIdentifier;
    if (key === "U+001B") { // ESC
        window.pagePopupController.closePopup();
        event.preventDefault();
    } else if (key === "U+0009" /* TAB */ || key === "Enter") {
        window.pagePopupController.setValueAndClosePopup(0, this._selectElement.value);
        event.preventDefault();
    } else if (event.altKey && (key === "Down" || key === "Up")) {
        // We need to add a delay here because, if we do it immediately the key
        // press event will be handled by HTMLSelectElement and this popup will
        // be reopened.
        setTimeout(function () {
            window.pagePopupController.closePopup();
        }, 0);
        event.preventDefault();
    }
};

ListPicker.prototype._fixWindowSize = function() {
    this._selectElement.style.height = "";
    var zoom = this._config.zoomFactor;
    var maxHeight = this._selectElement.offsetHeight * zoom;
    var noScrollHeight = (this._calculateScrollHeight() + ListPicker.ListboxSelectBorder * 2) * zoom;
    var scrollbarWidth = getScrollbarWidth() * zoom;
    var elementOffsetWidth = this._selectElement.offsetWidth * zoom;
    var desiredWindowHeight = noScrollHeight;
    var desiredWindowWidth = elementOffsetWidth;
    var expectingScrollbar = false;
    if (desiredWindowHeight > maxHeight) {
        desiredWindowHeight = maxHeight;
        // Setting overflow to auto does not increase width for the scrollbar
        // so we need to do it manually.
        desiredWindowWidth += scrollbarWidth;
        expectingScrollbar = true;
    }
    desiredWindowWidth = Math.max(this._config.anchorRectInScreen.width, desiredWindowWidth);
    var windowRect = adjustWindowRect(desiredWindowWidth, desiredWindowHeight, elementOffsetWidth, 0);
    // If the available screen space is smaller than maxHeight, we will get an unexpected scrollbar.
    if (!expectingScrollbar && windowRect.height < noScrollHeight) {
        desiredWindowWidth = windowRect.width + scrollbarWidth;
        windowRect = adjustWindowRect(desiredWindowWidth, windowRect.height, windowRect.width, windowRect.height);
    }
    this._selectElement.style.width = (windowRect.width / zoom) + "px";
    this._selectElement.style.height = (windowRect.height / zoom) + "px";
    this._element.style.height = (windowRect.height / zoom) + "px";
    setWindowRect(windowRect);
};

ListPicker.prototype._calculateScrollHeight = function() {
    // Element.scrollHeight returns an integer value but this calculate the
    // actual fractional value.
    var top = Infinity;
    var bottom = -Infinity;
    for (var i = 0; i < this._selectElement.children.length; i++) {
        var rect = this._selectElement.children[i].getBoundingClientRect();
        // Skip hidden elements.
        if (rect.width === 0 && rect.height === 0)
            continue;
        top = Math.min(top, rect.top);
        bottom = Math.max(bottom, rect.bottom);
    }
    return Math.max(bottom - top, 0);
};

ListPicker.prototype._listItemCount = function() {
    return this._selectElement.querySelectorAll("option,optgroup,hr").length;
};

ListPicker.prototype._layout = function() {
    if (this._config.isRTL)
        this._element.classList.add("rtl");
    this._selectElement.style.backgroundColor = this._config.baseStyle.backgroundColor;
    this._selectElement.style.color = this._config.baseStyle.color;
    this._selectElement.style.textTransform = this._config.baseStyle.textTransform;
    this._selectElement.style.fontSize = this._config.baseStyle.fontSize + "px";
    this._selectElement.style.fontFamily = this._config.baseStyle.fontFamily.join(",");
    this._selectElement.style.fontStyle = this._config.baseStyle.fontStyle;
    this._selectElement.style.fontVariant = this._config.baseStyle.fontVariant;
    this._updateChildren(this._selectElement, this._config);
};

ListPicker.prototype._update = function() {
    var scrollPosition = this._selectElement.scrollTop;
    var oldValue = this._selectElement.value;
    this._layout();
    this._selectElement.value = this._config.selectedIndex;
    this._selectElement.scrollTop = scrollPosition;
    var optionUnderMouse = null;
    if (this._selectionSetByMouseHover) {
        var elementUnderMouse = document.elementFromPoint(this.lastMousePositionX, this.lastMousePositionY);
        optionUnderMouse = elementUnderMouse && elementUnderMouse.closest("option");
    }
    if (optionUnderMouse)
        optionUnderMouse.selected = true;
    else
        this._selectElement.value = oldValue;
    this._selectElement.scrollTop = scrollPosition;
    this.dispatchEvent("didUpdate");
};

ListPicker.DelayedLayoutThreshold = 1000;

/**
 * @param {!Element} parent Select element or optgroup element.
 * @param {!Object} config
 */
ListPicker.prototype._updateChildren = function(parent, config) {
    var outOfDateIndex = 0;
    var fragment = null;
    var inGroup = parent.tagName === "OPTGROUP";
    var lastListIndex = -1;
    var limit = Math.max(this._config.selectedIndex, ListPicker.DelayedLayoutThreshold);
    var i;
    for (i = 0; i < config.children.length; ++i) {
        if (!inGroup && lastListIndex >= limit)
            break;
        var childConfig = config.children[i];
        var item = this._findReusableItem(parent, childConfig, outOfDateIndex) || this._createItemElement(childConfig);
        this._configureItem(item, childConfig, inGroup);
        lastListIndex = item.value ? Number(item.value) : -1;
        if (outOfDateIndex < parent.children.length) {
            parent.insertBefore(item, parent.children[outOfDateIndex]);
        } else {
            if (!fragment)
                fragment = document.createDocumentFragment();
            fragment.appendChild(item);
        }
        outOfDateIndex++;
    }
    if (fragment) {
        parent.appendChild(fragment);
    } else {
        var unused = parent.children.length - outOfDateIndex;
        for (var j = 0; j < unused; j++) {
            parent.removeChild(parent.lastElementChild);
        }
    }
    if (i < config.children.length) {
        // We don't bind |config.children| and |i| to _updateChildrenLater
        // because config.children can get invalid before _updateChildrenLater
        // is called.
        this._delayedChildrenConfig = config.children;
        this._delayedChildrenConfigIndex = i;
        // Needs some amount of delay to kick the first paint.
        setTimeout(this._updateChildrenLater.bind(this), 100);
    }
};

ListPicker.prototype._updateChildrenLater = function(timeStamp) {
    if (!this._delayedChildrenConfig)
        return;
    var fragment = document.createDocumentFragment();
    var startIndex = this._delayedChildrenConfigIndex;
    for (; this._delayedChildrenConfigIndex < this._delayedChildrenConfig.length; ++this._delayedChildrenConfigIndex) {
        var childConfig = this._delayedChildrenConfig[this._delayedChildrenConfigIndex];
        var item = this._createItemElement(childConfig);
        this._configureItem(item, childConfig, false);
        fragment.appendChild(item);
    }
    this._selectElement.appendChild(fragment);
    this._selectElement.classList.add("wrap");
    this._delayedChildrenConfig = null;
};

ListPicker.prototype._findReusableItem = function(parent, config, startIndex) {
    if (startIndex >= parent.children.length)
        return null;
    var tagName = "OPTION";
    if (config.type === "optgroup")
        tagName = "OPTGROUP";
    else if (config.type === "separator")
        tagName = "HR";
    for (var i = startIndex; i < parent.children.length; i++) {
        var child = parent.children[i];
        if (tagName === child.tagName) {
            return child;
        }
    }
    return null;
};

ListPicker.prototype._createItemElement = function(config) {
    var element;
    if (!config.type || config.type === "option")
        element = createElement("option");
    else if (config.type === "optgroup")
        element = createElement("optgroup");
    else if (config.type === "separator")
        element = createElement("hr");
    return element;
};

ListPicker.prototype._applyItemStyle = function(element, styleConfig) {
    if (!styleConfig)
        return;
    var style = element.style;
    style.visibility = styleConfig.visibility ? styleConfig.visibility : "";
    style.display = styleConfig.display ? styleConfig.display : "";
    style.direction = styleConfig.direction ? styleConfig.direction : "";
    style.unicodeBidi = styleConfig.unicodeBidi ? styleConfig.unicodeBidi : "";
    style.color = styleConfig.color ? styleConfig.color : "";
    style.backgroundColor = styleConfig.backgroundColor ? styleConfig.backgroundColor : "";
    style.fontSize = styleConfig.fontSize ? styleConfig.fontSize + "px" : "";
    style.fontWeight = styleConfig.fontWeight ? styleConfig.fontWeight : "";
    style.fontFamily = styleConfig.fontFamily ? styleConfig.fontFamily.join(",") : "";
    style.fontStyle = styleConfig.fontStyle ? styleConfig.fontStyle : "";
    style.fontVariant = styleConfig.fontVariant ? styleConfig.fontVariant : "";
    style.textTransform = styleConfig.textTransform ? styleConfig.textTransform : "";
};

ListPicker.prototype._configureItem = function(element, config, inGroup) {
    if (!config.type || config.type === "option") {
        element.label = config.label;
        element.value = config.value;
        if (config.title)
            element.title = config.title;
        else
            element.removeAttribute("title");
        element.disabled = !!config.disabled
        if (config.ariaLabel)
            element.setAttribute("aria-label", config.ariaLabel);
        else
            element.removeAttribute("aria-label");
        element.style.webkitPaddingStart = this._config.paddingStart + "px";
        if (inGroup) {
            element.style.webkitMarginStart = (- this._config.paddingStart) + "px";
            // Should be synchronized with padding-end in listPicker.css.
            element.style.webkitMarginEnd = "-2px";
        }
    } else if (config.type === "optgroup") {
        element.label = config.label;
        element.title = config.title;
        element.disabled = config.disabled;
        element.setAttribute("aria-label", config.ariaLabel);
        this._updateChildren(element, config);
        element.style.webkitPaddingStart = this._config.paddingStart + "px";
    } else if (config.type === "separator") {
        element.title = config.title;
        element.disabled = config.disabled;
        element.setAttribute("aria-label", config.ariaLabel);
        if (inGroup) {
            element.style.webkitMarginStart = (- this._config.paddingStart) + "px";
            // Should be synchronized with padding-end in listPicker.css.
            element.style.webkitMarginEnd = "-2px";
        }
    }
    this._applyItemStyle(element, config.style);
};

if (window.dialogArguments) {
    initialize(dialogArguments);
} else {
    window.addEventListener("message", handleMessage, false);
    window.setTimeout(handleArgumentsTimeout, 1000);
}
RIFF$