in the same element in
        order to get proper label styling with :focus and :moz-ui-invalid.
      -->
    <div id="name-container" class="container">
      <label id="given-name-container">
        <input id="given-name" type="text" required="required"/>
        <span data-localization="givenName" class="label-text"/>
      </label>
      <label id="additional-name-container">
        <input id="additional-name" type="text"/>
        <span data-localization="additionalName" class="label-text"/>
      </label>
      <label id="family-name-container">
        <input id="family-name" type="text" required="required"/>
        <span data-localization="familyName" class="label-text"/>
      </label>
    </div>
    <label id="organization-container" class="container">
      <input id="organization" type="text"/>
      <span data-localization="organization2" class="label-text"/>
    </label>
    <label id="street-address-container" class="container">
      <textarea id="street-address" rows="3"/>
      <span data-localization="streetAddress" class="label-text"/>
    </label>
    <label id="address-level3-container" class="container">
      <input id="address-level3" type="text"/>
      <span class="label-text"/>
    </label>
    <label id="address-level2-container" class="container">
      <input id="address-level2" type="text"/>
      <span class="label-text"/>
    </label>
    <label id="address-level1-container" class="container">
      <!-- The address-level1 input will get replaced by a select dropdown
           by autofillEditForms.js when the selected country has provided
           specific options. -->
      <input id="address-level1" type="text"/>
      <span class="label-text"/>
    </label>
    <label id="postal-code-container" class="container">
      <input id="postal-code" type="text"/>
      <span class="label-text"/>
    </label>
    <label id="country-container" class="container">
      <select id="country" required="required">
        <option/>
      </select>
      <span data-localization="country" class="label-text"/>
    </label>
    <label id="tel-container" class="container">
      <input id="tel" type="tel"/>
      <span data-localization="tel" class="label-text"/>
    </label>
    <label id="email-container" class="container">
      <input id="email" type="email" required="required"/>
      <span data-localization="email" class="label-text"/>
    </label>
  </form>
  <div id="controls-container">
    <button id="cancel" data-localization="cancelBtnLabel"/>
    <button id="save" data-localization="saveBtnLabel"/>
    <span id="country-warning-message" data-localization="countryWarningMessage2"/>
  </div>
  <script type="application/javascript"><![CDATA[
    "use strict";

    /* import-globals-from l10n.js */

    let {
      DEFAULT_REGION,
      countries,
    } = FormAutofill;
    let {
      getFormFormat,
      findAddressSelectOption,
    } = FormAutofillUtils;
    let args = window.arguments || [];
    let {
      record,
      noValidate,
    } = args[0] || {};

    /* import-globals-from autofillEditForms.js */
    var fieldContainer = new EditAddress({
      form: document.getElementById("form"),
    }, record, {
      DEFAULT_REGION,
      getFormFormat: getFormFormat.bind(FormAutofillUtils),
      findAddressSelectOption: findAddressSelectOption.bind(FormAutofillUtils),
      countries,
      noValidate,
    });

    /* import-globals-from editDialog.js */
    new EditAddressDialog({
      title: document.querySelector("title"),
      fieldContainer,
      controlsContainer: document.getElementById("controls-container"),
      cancel: document.getElementById("cancel"),
      save: document.getElementById("save"),
    }, record);
  ]]></script>
</body>
</html>
PK