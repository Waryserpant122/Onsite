lla.org/MPL/2.0/. */

.editCreditCardForm {
  display: grid;
  grid-template-areas:
    "cc-number          cc-exp-month       cc-exp-year"
    "cc-name            cc-type            cc-csc"
    "billingAddressGUID billingAddressGUID billingAddressGUID";
  grid-template-columns: 4fr 2fr 2fr;
  grid-row-gap: var(--grid-column-row-gap);
  grid-column-gap: var(--grid-column-row-gap);
}

.editCreditCardForm label {
  /* Remove the margin on these labels since they are styled on top of
     the input/select element. */
  margin-inline-start: 0;
  margin-inline-end: 0;
}

.editCreditCardForm .container {
  display: flex;
}

#cc-number-container {
  grid-area: cc-number;
}

#cc-exp-month-container {
  grid-area: cc-exp-month;
}

#cc-exp-year-container {
  grid-area: cc-exp-year;
}

#cc-name-container {
  grid-area: cc-name;
}

#cc-type-container {
  grid-area: cc-type;
}

#cc-csc-container {
  grid-area: cc-csc;
}

#billingAddressGUID-container {
  grid-area: billingAddressGUID;
}

#billingAddressGUID {
  grid-area: dropdown;
}
PK