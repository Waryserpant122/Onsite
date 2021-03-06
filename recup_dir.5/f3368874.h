> richlistitem[originaltype="autofill-profile"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-footer"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-insecureWarning"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-clear-button"] {
  display: block;
  margin: 0;
  padding: 0;
  height: auto;
  min-height: auto;
  -moz-binding: none;
}

/* Treat @collpased="true" as display: none similar to how it is for XUL elements.
 * https://developer.mozilla.org/en-US/docs/Web/CSS/visibility#Values */
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-profile"][collapsed="true"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-footer"][collapsed="true"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-insecureWarning"][collapsed="true"],
#PopupAutoComplete > richlistbox > richlistitem[originaltype="autofill-clear-button"][collapsed="true"] {
  display: none;
}

#PopupAutoComplete[firstresultstyle="autofill-profile"] {
  min-width: 150px !important;
}

#PopupAutoComplete[firstresultstyle="autofill-insecureWarning"] {
  min-width: 200px !important;
}

/* Form Autofill Doorhanger */
#autofill-address-notification popupnotificationcontent > .desc-message-box,
#autofill-credit-card-notification popupnotificationcontent > .desc-message-box {
  margin-block-end: 12px;
}
#autofill-credit-card-notification popupnotificationcontent > .desc-message-box > image {
  margin-inline-start: 6px;
  width: 16px;
  height: 16px;
  list-style-image: url(chrome://formautofill/content/icon-credit-card-generic.svg);
}
#autofill-address-notification popupnotificationcontent > .desc-message-box > description,
#autofill-credit-card-notification popupnotificationcontent > .desc-message-box > description {
  font-style: italic;
}
PK