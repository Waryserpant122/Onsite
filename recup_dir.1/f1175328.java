jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.controllers.CustomSpeedDialog = (function () {
	function Class(rootApi, apiSettings, settingsCtrl) {
		this._rootApi = rootApi;
		this._apiSettings = apiSettings;

		this.collections = {};

		this.collections.speedValues = new Backbone.Collection([
			{value: 32 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 32}},
			{value: 64 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 64}},
			{value: 128 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 128}},
			{value: 256 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 256}},
			{value: 512 * fdmApp.bytesInKB, text_tpl: {tpl: "%1 KB/s", value: 512}},
			{value: 1 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 MB/s", value: "1.0"}},
			{value: 1.5 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 MB/s", value: "1.5"}},
			{value: 2 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 MB/s", value: "2.0"}},
			{value: 4 * fdmApp.bytesInKB * fdmApp.bytesInKB, text_tpl: {tpl: "%1 MB/s", value: "4.0"}},
			{value: undefined, disable: true, text_tpl: {tpl: "