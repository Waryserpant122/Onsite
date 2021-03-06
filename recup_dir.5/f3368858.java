="hidden" data-localization="invalidCardNumber"></span>
      <input id="cc-number" type="text" required="required" minlength="9" pattern="[- 0-9]+"/>
      <span data-localization="cardNumber" class="label-text"/>
    </label>
    <label id="cc-exp-month-container" class="container">
      <select id="cc-exp-month" required="required">
        <option/>
      </select>
      <span data-localization="cardExpiresMonth" class="label-text"/>
    </label>
    <label id="cc-exp-year-container" class="container">
      <select id="cc-exp-year" required="required">
        <option/>
      </select>
      <span data-localization="cardExpiresYear" class="label-text"/>
    </label>
    <label id="cc-name-container" class="container">
      <input id="cc-name" type="text" required="required"/>
      <span data-localization="nameOnCard" class="label-text"/>
    </label>
    <label id="cc-type-container" class="container">
      <select id="cc-type" required="required">
      </select>
      <span data-localization="cardNetwork" class="label-text"/>
    </label>
    <label id="cc-csc-container" class="container" hidden="hidden">
      <!-- The CSC container will get filled in by forms that need a CSC (using csc-input.js) -->
    </label>
    <div id="billingAddressGUID-container" class="billingAddressRow container rich-picker">
      <select id="billingAddressGUID" required="required">
      </select>
      <label for="billingAddressGUID" data-localization="billingAddress" class="label-text"/>
    </div>
  </form>
  <div id="controls-container">
    <button id="cancel" data-localization="cancelBtnLabel"/>
    <button id="save" data-localization="saveBtnLabel"/>
  </div>
  <script type="application/javascript"><![CDATA[
    "use strict";

    /* import-globals-from l10n.js */

    (async () => {
      let {
        getAddressLabel,
        isCCNumber,
        getCreditCardNetworks,
      } = FormAutofillUtils;
      let args = window.arguments || [];
      let {
        record,
      } = args[0] || {};

      let addresses = {};
      for (let address of await formAutofillStorage.addresses.getAll()) {
        addresses[address.guid] = address;
      }

      /* import-globals-from autofillEditForms.js */
      let fieldContainer = new EditCreditCard({
        form: document.getElementById("form"),
      }, record, addresses,
        {
          getAddressLabel: getAddressLabel.bind(FormAutofillUtils),
          isCCNumber: isCCNumber.bind(FormAutofillUtils),
          getSupportedNetworks: getCreditCardNetworks.bind(FormAutofillUtils),
        });

      /* import-globals-from editDialog.js */
      new EditCreditCardDialog({
        title: document.querySelector("title"),
        fieldContainer,
        controlsContainer: document.getElementById("controls-container"),
        cancel: document.getElementById("cancel"),
        save: document.getElementById("save"),
      }, record);
    })();
  ]]></script>
</body>
</html>
PK