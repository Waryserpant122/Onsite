/*** *** *** RESETS: START *** *** ***/
* { -webkit-user-select: none; outline: none; cursor: default; }

.macosx * { cursor: default !important; }

blockquote, body, dd, div, dl, dt, embed, fieldset, form, h1, h2, h3, h4, h5, h6, input, li, object, ol, p, pre, table, td, th, ul { padding: 0; margin: 0; }

a { text-decoration: none; cursor: pointer; }

address, caption, cite, code, dfn, em, h1, h2, h3, h4, h5, h6, strong, th, var { font-weight: 400; }

table { border-collapse: collapse; border-spacing: 0; }

ol, ul { list-style: none; }

abbr, acronym, fieldset, img { border: 0; }

input[type=text], input[type=search], input[type=number], input[type=password] { cursor: text !important; font: 14px "Helvetica", Arial, sans-serif; }

input[type=text]:focus, input[type=password]:focus { -webkit-user-select: auto; outline: auto; }

input[type=search]::-webkit-search-cancel-button { cursor: default !important; }

/*** *** *** RESETS: END *** *** ***/
/*extend classes: start*/
.tag_element { float: left; margin: 0 5px 12px 0; padding: 2px 6px; min-width: 40px; height: 18px; font-size: 11px; text-align: center; position: relative; overflow: hidden; color: #4c4c4c; white-space: nowrap; background-color: #ebebeb; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tag_element.active:before { content: ''; display: block; height: 1px; background-color: #16a4fa; position: absolute; left: 0; right: 0; bottom: 0; }
.tag_element.last_in_block { margin-right: 11px; }
.tag_element .tag_color { width: 10px; height: 9px; display: inline-block; margin-right: 5px; z-index: 2; position: relative; }
.tag_element .tag_color:hover:before { content: ''; position: absolute; top: 3px; left: 2px; width: 0; height: 0; border-left: 3px solid transparent; border-right: 3px solid transparent; border-top: 4px solid #fff; }
.tag_element .tag_color.system:hover:before { display: none; }
.tag_element .tag_name { position: relative; z-index: 3; }

.tag_hidden_name { font-size: 11px; }

.wrapper_inselect { float: left; height: auto; width: 106px; margin-right: 7px; background-color: #fff; border: 1px solid #d4d4d4; position: relative; padding: 0 36px 0 8px; line-height: 28px; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; z-index: 1000; }
.wrapper_inselect .list { top: 28px; }
.wrapper_inselect .dropdown_button { position: absolute; top: 0; right: 0; width: 30px; height: 28px; cursor: pointer; border-left: 1px solid #ededed; }
.wrapper_inselect .dropdown_button:before { content: ''; display: block; width: 0; height: 0; border-style: solid; border-width: 5px 3px 0 3px; border-color: #929292 transparent transparent transparent; position: absolute; top: 50%; left: 50%; margin-top: -2px; margin-left: -3px; }
.wrapper_inselect .input_wrapper { margin-right: 30px; }

input[type=text].tag_element:focus { outline: none; }

.display_flex { display: -webkit-flex; display: flex; }

.relative { position: relative; }

.display_none { display: none; }

.header-nav .button_menu:before, .header-nav .update_button:before, .button_back:before, .header-nav .add-download, .header-nav .start-download, .header-nav .pause-download, .header-nav .cancel-download, .header-nav .action_folder, .header-nav .header-search input, .header-nav .update_button .update_icon { background: url("../v2_images/header_button.svg") no-repeat; }

.compact-view-info ul li.sort-down:after, .compact-view-info ul li.sort-up:after, .table-headers li.sort-down:after, .table-headers li.sort-up:after, .block_error:before, .macosx .popup .header .close_button, .macosx .delete .header .close_button, .macosx .add_url .header .close_button, .macosx .scheduler .header .close_button, .macosx .change_url_wrapper .header .close_button, .macosx .popup_exist .add_url .header .close_button, .macosx .simple .simple_dialog .header .close_button, .bottom-panel-opener, .close-icon, .header-tags .manage_btn, .header-tags .show_more, .header-tags .wrapper_tags .choose_color, .header-tags .triangle, .wrapper_inselect.top .list div.active:before, .wrap_default_client .cancel_btn, .download-list .row.download-ending .download-complete, .download-list .tags span.show_tags, .download-list .error-message, .tab-general .folder_icon, .tab-general .general-tab-wrap .wrapper-user-tags .close, .tab-general .general-tab-wrap .error-message, .files-table li.folder > .triangle, .files-table li .pad .error-message, .traffic-bar > ul:after, .traffic-bar > ul .down-speed-chooser:before, .traffic-bar > ul .up-speed-chooser:before, .traffic-bar .speed-chooser li.selected span:before, .traffic-bar .download_title.error, .close_button, .popup .close_button, .delete .close_button, .add_url .close_button, .scheduler .close_button, .change_url_wrapper .close_button, .popup_exist .add_url .close_button, .simple .simple_dialog .close_button, .popup .wrap_tags .manage_btn, .popup .wrapper_tree ins, .dropdown-settings .rows-group .menu-row.shutdown:before, .wrapper_updates:before, .wrapper_updates .cancel_btn, .settings_wrapper .info:before { background: url("../v2_images/elements.svg?51120") no-repeat; }

.download-state, .settings_wrapper .right_panel .button_folder { background: url("../v2_images/actions.svg") no-repeat; }

input[type=checkbox] + label:before, .compact-view-info ul li input[type=checkbox] + label:before, .checkbox-fake, .files-table li .fake_checkbox, .settings_wrapper .downloads_tab input[type=radio] + label:before, .settings_wrapper .connection_tab input[type=radio] + label:before, .settings_wrapper .monitoring_tab input[type=radio] + label:before { background: url("../v2_images/checkbox.svg") no-repeat; }

.settings_wrapper .language_block .lang_l span:before { background: url("../v2_images/flags.svg") no-repeat; }

.download-list .compact-download-title, .no-size span, .tab-general ul li span:first-child, .files-table li span, .trackers-info li span, .peers-info li span, .log-info li span, .popup .title, .delete .title, .add_url .title, .scheduler .title, .change_url_wrapper .title, .popup_exist .add_url .title, .simple .simple_dialog .title, .popup .link_name, .delete .link_name, .add_url .link_name, .scheduler .link_name, .change_url_wrapper .link_name, .popup_exist .add_url .link_name, .simple .simple_dialog .link_name, .popup .wrapper_tree .file_name, .rubber-ellipsis { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; }

.center-choose-speed select, .youtube_wrapper select { -webkit-appearance: none; display: inline-block; border: 1px solid #b8b8b8; height: 32px; background: url("../v2_images/arrow_combobox.svg") no-repeat right 5px center; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }

.header-nav .button_menu, .header-nav .update_button, .button_back { width: 40px; height: 22px; border: 1px solid #b3b3b3; cursor: pointer; background-color: #f7f8f7; -moz-border-radius: 4px; -webkit-border-radius: 4px; border-radius: 4px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.header-nav .button_menu:before, .header-nav .update_button:before, .button_back:before { content: ''; display: block; position: absolute; left: 50%; }

.resizer { display: block; width: 6px; height: 22px; border-right: 1px solid #e5e5e5; position: absolute; right: 0; top: 1px; cursor: ew-resize !important; }

.float_none { float: none; clear: both; }

input[type=checkbox] + label:before, .compact-view-info ul li input[type=checkbox] + label:before, .checkbox-fake, .files-table li .fake_checkbox, .settings_wrapper .downloads_tab input[type=radio] + label:before, .settings_wrapper .connection_tab input[type=radio] + label:before, .settings_wrapper .monitoring_tab input[type=radio] + label:before { content: ''; display: block; width: 12px; height: 12px; position: absolute; }

.no-size div, .tab-general .general-tab-wrap .progress-wrap.unknown_size .progress-line-wrap, .wrapper_updates .checking .compact-progress-line { background: url(../v2_images/line_small.gif) repeat-x; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }

.compact-view-info ul li.sort-down:after, .compact-view-info ul li.sort-up:after, .table-headers li.sort-down:after, .table-headers li.sort-up:after { content: ''; display: inline-block; width: 10px; height: 10px; margin-left: 5px; position: relative; z-index: 100; }

.compact-view-info ul li.sort-up:after, .table-headers li.sort-up:after { background-position: -38px -104px; }

.compact-view-info ul li.sort-down:after, .table-headers li.sort-down:after { background-position: -18px -104px; }

.compact-view-shown .compact-progress-wrap .no-size.downloading_paused_line div, .tab-general .general-tab-wrap .progress-wrap.unknown_size.downloading_paused_line .progress-line-wrap, .compact-view-shown .is_queued .compact-progress-wrap .no-size div, .tab-general .general-tab-wrap .progress-wrap.unknown_size.is_queued .progress-line-wrap { background-color: #bfbfbf; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuMCIgeTE9IjAuMCIgeDI9IjEuMCIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIyNSUiIHN0b3AtY29sb3I9IiNlMWUxZTEiLz48c3RvcCBvZmZzZXQ9IjI1JSIgc3RvcC1jb2xvcj0iIzAwMDAwMCIgc3RvcC1vcGFjaXR5PSIwLjAiLz48c3RvcCBvZmZzZXQ9IjUwJSIgc3RvcC1jb2xvcj0iIzAwMDAwMCIgc3RvcC1vcGFjaXR5PSIwLjAiLz48c3RvcCBvZmZzZXQ9IjUwJSIgc3RvcC1jb2xvcj0iI2UxZTFlMSIvPjxzdG9wIG9mZnNldD0iNzUlIiBzdG9wLWNvbG9yPSIjZTFlMWUxIi8+PHN0b3Agb2Zmc2V0PSI3NSUiIHN0b3AtY29sb3I9IiMwMDAwMDAiIHN0b3Atb3BhY2l0eT0iMC4wIi8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdG9wLWNvbG9yPSIjMDAwMDAwIiBzdG9wLW9wYWNpdHk9IjAuMCIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -moz-linear-gradient(315deg, #e1e1e1 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #e1e1e1 50%, #e1e1e1 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); background-image: -webkit-linear-gradient(315deg, #e1e1e1 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #e1e1e1 50%, #e1e1e1 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); background-image: linear-gradient(135deg, #e1e1e1 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #e1e1e1 50%, #e1e1e1 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); -moz-animation: none; -webkit-animation: none; animation: none; background-size: 30px 30px; }

/*extend classes: end*/
/*mixins: start*/
@-moz-keyframes animate-stripes { 0% { background-position: 0 0; }
  100% { background-position: 60px 0; } }
@-webkit-keyframes animate-stripes { 0% { background-position: 0 0; }
  100% { background-position: 60px 0; } }
@keyframes animate-stripes { 0% { background-position: 0 0; }
  100% { background-position: 60px 0; } }
@-webkit-keyframes spinner { from { -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -webkit-transform: rotate(0deg); transform: rotate(0deg); }
  to { -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg); } }
@-moz-keyframes spinner { from { -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -webkit-transform: rotate(0deg); transform: rotate(0deg); }
  to { -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg); } }
@-ms-keyframes spinner { from { -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -webkit-transform: rotate(0deg); transform: rotate(0deg); }
  to { -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg); } }
@-o-keyframes spinner { from { -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -webkit-transform: rotate(0deg); transform: rotate(0deg); }
  to { -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg); } }
@keyframes spinner { from { -moz-transform: rotate(0deg); -ms-transform: rotate(0deg); -webkit-transform: rotate(0deg); transform: rotate(0deg); }
  to { -moz-transform: rotate(360deg); -ms-transform: rotate(360deg); -webkit-transform: rotate(360deg); transform: rotate(360deg); } }
/*mixins: end*/
body.windows { font: 14px Arial, sans-serif; }

.windows input[type=text]:focus, .windows input[type=password]:focus { outline: none; border-color: #00b1ff; }
.windows input[type=text].tag_element:focus { border-color: transparent; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.windows input[type=search]::-webkit-search-cancel-button { cursor: pointer !important; }
.windows .wrapper_inselect:focus { border-color: #00b1ff; }
.windows .wrapper_strt_dwlnd .start_add_download { height: -webkit-calc(100% - 6px); height: calc(100% - 6px); }
.windows .header-nav { padding: 0; background-color: #333c4e; border-bottom: none; }
.windows .header-nav.opacity { opacity: 1; }
.windows .header-nav.opacity .disable_block { display: none; }
.windows .header-nav .add-download, .windows .header-nav .start-download, .windows .header-nav .pause-download, .windows .header-nav .cancel-download, .windows .header-nav .action_folder { width: 60px; height: 60px; margin-right: 0; position: relative; background-image: none; }
.windows .header-nav .add-download:before, .windows .header-nav .start-download:before, .windows .header-nav .pause-download:before, .windows .header-nav .cancel-download:before, .windows .header-nav .action_folder:before { content: ''; display: block; position: absolute; top: 0; bottom: 0; left: 0; right: 0; background: url(../v2_images/header_button_win.svg) no-repeat; }
.windows .header-nav .add-download.disable, .windows .header-nav .start-download.disable, .windows .header-nav .pause-download.disable, .windows .header-nav .cancel-download.disable, .windows .header-nav .action_folder.disable { opacity: 1; }
.windows .header-nav .add-download.disable:before, .windows .header-nav .start-download.disable:before, .windows .header-nav .pause-download.disable:before, .windows .header-nav .cancel-download.disable:before, .windows .header-nav .action_folder.disable:before { opacity: 0.3; }
.windows .header-nav .add-download:after, .windows .header-nav .start-download:after, .windows .header-nav .pause-download:after, .windows .header-nav .cancel-download:after, .windows .header-nav .action_folder:after { content: ''; display: block; height: 34px; width: 1px; position: absolute; top: 13px; right: 0; background-color: #3e4a5f; }
.windows .header-nav .add-download { -moz-border-radius: 0; -webkit-border-radius: 0; border-radius: 0; background-color: #16a4fa; }
.windows .header-nav .add-download:before { background-position: 20px 19px; }
.windows .header-nav .add-download:after { display: none; }
.windows .header-nav .start-download:before { background-position: 24px -43px; }
.windows .header-nav .start-download.selected:before { background-position: -26px -44px; }
.windows .header-nav .pause-download:before { background-position: 24px -101px; }
.windows .header-nav .pause-download.selected:before { background-position: -26px -101px; }
.windows .header-nav .cancel-download:before { background-position: 21px -161px; }
.windows .header-nav .cancel-download.selected:before { background-position: -26px -161px; }
.windows .header-nav .action_folder:before { background-position: 19px -222px; }
.windows .header-nav .action_folder.selected:before { background-position: -23px -222px; }
.windows .header-nav .button_menu { width: 60px; height: 60px; top: 0; right: 0; border: none; background: url(../v2_images/header_button_win.svg) no-repeat 20px -341px; -moz-border-radius: 0; -webkit-border-radius: 0; border-radius: 0; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.windows .header-nav .button_menu:before { display: none; }
.windows .header-nav .button_menu.notification:after { top: 16px; right: 14px; }
.windows .header-nav .input-text { display: none; }
.windows .header-nav .header-search { width: 60px; height: 60px; padding: 0; margin-right: 60px; background: url(../v2_images/header_button_win.svg) no-repeat 20px -281px; position: relative; cursor: pointer; }
.windows .header-nav .header-search:after { content: ''; display: block; height: 34px; width: 1px; position: absolute; top: 13px; right: 0; background-color: #3e4a5f; }
.windows .header-nav .header-search.show_search { width: auto; background: none; padding-top: 14px; padding-right: 18px; cursor: default; }
.windows .header-nav .header-search.show_search .input-text { display: block; border: none; padding: 0 10px 0 30px; height: 30px; line-height: 30px; margin-top: 0; opacity: 1; width: 200px; -moz-transition: width 0.15s ease-in; -o-transition: width 0.15s ease-in; -webkit-transition: width 0.15s ease-in; transition: width 0.15s ease-in; background: url("../v2_images/header_button_win.svg") no-repeat 8px -416px #fff; }
.windows .header-nav .header-search.show_search .input-text::-webkit-input-placeholder { color: #333c4e; }
.windows .header-nav .header-search.show_search .input-text::-webkit-search-cancel-button { cursor: pointer; }
.windows .header-nav .header-search.show_search .input-text::-webkit-search-decoration, .windows .header-nav .header-search.show_search .input-text::-webkit-search-results-button, .windows .header-nav .header-search.show_search .input-text::-webkit-search-results-decoration { display: none; }
.windows .header-nav .header-search .input-text { display: block; opacity: 0; width: 0; padding: 0; border: none; }
.windows .header-nav .update_button { width: 60px; height: 60px; padding: 0; position: relative; border: none; margin: 0; background: transparent; -moz-border-radius: 0; -webkit-border-radius: 0; border-radius: 0; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.windows .header-nav .update_button:after, .windows .header-nav .update_button:before { content: ''; display: block; height: 34px; width: 1px; position: absolute; top: 13px; right: 0; background: #3e4a5f; }
.windows .header-nav .update_button:before { right: auto; left: 0; margin: 0; }
.windows .header-nav .update_button .update_icon { position: absolute; left: 0; right: 0; top: 0; bottom: 0; height: auto; width: auto; border: none; margin: 0; background: url(../v2_images/header_button_win.svg) no-repeat 21px -457px; }
.windows .header-tags .new_tag { padding-top: 2px; }
.windows .block_error { top: 3px; padding-top: 8px; }
.windows.show_settings .update_button { right: 17px; }
.windows .download-list .row > div { padding-top: 12px; }
.windows .download-list .row.paused .download-state { background-position: 2px -34px; }
.windows .download-list .row.timer_state .download-state { background-position: 0 -157px; }
.windows .wrapper_updates { top: 52px; left: -72px; }
.windows .wrapper_updates:before { top: -6px; left: 98px; }
.windows .button_back { width: 60px; height: 60px; padding: 0; background: url(../v2_images/header_button_win.svg) no-repeat 20px -519px; position: relative; border: none; margin: 0; -moz-border-radius: 0; -webkit-border-radius: 0; border-radius: 0; }
.windows .button_back:before { display: none; }
.windows .button_back:after { content: ''; display: block; height: 34px; width: 1px; position: absolute; top: 13px; right: 0; background-color: #3e4a5f; }
.windows .settings_wrapper .tum_tab table .wrapper_inselect input[type=text]:focus { border-color: #39a7d6; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.windows .scheduler .wrapper_times input[type=number] { font: 14px Arial, sans-serif; }
.windows .mount, .windows .transparent_updates, .windows .popup .transparent_select { top: 0; }
.windows .add_url, .windows .choose-speed, .windows .popup { border: 1px solid #9d9d9d; }
.windows .popup .top_info .file_lunk { color: #7e7d80; }
.windows .popup .wrapper_tree input[type="checkbox"] + label:before { top: 0; }
.windows .share_modal .wrapper_modal .dont_label input[type=checkbox] + label:before { top: 1px; }
.windows .popup .center_right.batch .file_name { line-height: 16px; }
.windows .popup .inselect div input, .windows .popup input[type=text].path_field, .windows .hash_popup input[type=text] { font: 14px "Arial", sans-serif; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.windows .youtube.popup input { font: 14px "Arial", sans-serif; }
.windows .tab-general.completed .wrap_completed_info, .windows .tab-general.download-ending .wrap_completed_info { margin-bottom: 9px; }
.windows .tab-general .general-tab-wrap .wrapper-user-tags { top: 6px; }
.windows .tab-general .general-tab-wrap .wrap-title-dwnld.without_tags { min-height: 30px; padding-top: 2px; margin-bottom: 6px; }
.windows .tab-general .general-tab-wrap .wrap_completed { margin-bottom: 7px; }
.windows .tab-general .general-tab-wrap .error-message:first-child { margin-bottom: 6px; }
.windows .tab-general .wrap_completed_error { margin-bottom: 7px; }
.windows .tab-general .wrapper_table { margin-bottom: 7px; }
.windows .peer > div .download-speed span.arrow_up, .windows .peer > div .download-speed span.arrow_dwn { top: -2px; }
.windows .tab-general .general-tab-wrap .wrapper-user-tags .tag_element { vertical-align: baseline; }

.de.windows .wrapper_updates { left: -130px; }
.de.windows .wrapper_updates:before { left: 154px; }
.de .wrapper_updates { width: 275px; }
.de .wrapper_updates .btn.left { margin-left: 0; }
.de .info-time .percents { font-size: 12px; margin-bottom: 2px; }
.de .tab-general .wrapper_table table td { min-width: 130px; }
.de .tab-general .wrapper_table table td.title:first-child { width: 150px; max-width: none; }
.de .traffic-bar .speed-chooser { width: 227px; }
.de .traffic-bar > ul .down-speed-chooser, .de .traffic-bar .traffic-bar > ul .up-speed-chooser { width: 70px; }
.de .traffic-bar > ul .up-speed-chooser { width: 80px; }
.de .center-choose-speed .s_margin_right { width: 175px; }
.de .alt_popup .alt_text { margin-top: 0; margin-bottom: 22px; }
.de .group_button .left_button, .de .group_button .right_button { width: auto; min-width: 110px; }
.de .popup .total a { margin-right: 4px; }
.de .settings_wrapper .tum_tab table { width: 610px; }
.de .settings_wrapper .tum_tab table .wrapper_inselect { max-width: 136px; }
.de .popup.scheduler_on .bottom .error-message { max-width: 260px; }

.fr .wrapper_updates .btn.left { margin-left: 4%; }
.fr .info-time .percents { font-size: 12px; margin-bottom: 2px; }
.fr .tab-general .wrapper_table table td.title:first-child { width: 150px; max-width: none; }
.fr .center-choose-speed .s_margin_right { width: 190px; }
.fr .popup .total a { margin-right: 3px; }
.fr .group_button .left_button.btn_anyway, .fr .fr .group_button .right_button.btn_anyway { width: 170px; }
.fr .wrapper_proxy input[type=text], .fr .wrapper_proxy input[type=password] { width: 110px; }
.fr .wrapper_proxy .wrap_form input[type=checkbox] + label:before { top: 2px; }
.fr .popup .wrapper_proxy .wrap_form input[type=checkbox] + label:before { top: 50%; }
.fr .popup .wrapper_proxy .wrap_form input[type=text], .fr .popup .wrapper_proxy .wrap_form input[type=password] { width: 100px; }

.ru.windows .wrapper_updates { left: -130px; }
.ru.windows .wrapper_updates:before { left: 154px; }
.ru .wrapper_updates { width: 275px; }
.ru .wrapper_updates .btn.left { margin-left: 0; }
.ru .compact-view-info ul li.compact-view-date { width: 101px; }
.ru .download-list .row.completed.without_upload:not(.error) .compact-download-title { width: -webkit-calc(100% - 302px); width: calc(100% - 302px); }
.ru .download-list .row.completed .compact-download-title { width: -webkit-calc(100% - 452px); width: calc(100% - 452px); }
.ru .compact-view-info ul li.compact-view-name, .ru .download-list .row.partially.completed .compact-download-title, .ru .download-list .row.completed.is_moving .compact-download-title, .ru .download-list .row.error.is_moving .compact-download-title, .ru .download-list .row.error .compact-download-title, .ru .download-list .compact-download-title { width: -webkit-calc(100% - 602px); width: calc(100% - 602px); }
.ru .info-time .percents { font-size: 11px; margin-bottom: 4px; }
.ru .modal_title { font-size: 30px; }
.ru .center-choose-speed .s_margin_right { width: 145px; }
.ru .group_button .left_button, .ru .group_button .right_button { width: auto; min-width: 90px; }
.ru .settings_wrapper .tum_tab table { width: 610px; }
.ru .settings_wrapper .tum_tab table .wrapper_inselect { max-width: 136px; }
.ru .popup.scheduler_on .bottom .error-message { max-width: 260px; }
.ru .tab-general .wrapper_table table td { min-width: 105px; }

.sl .info-time .download-time { font-size: 11px; padding-top: 2px; }
.sl .wrapper_updates .btn.left { margin-left: 0; margin-right: 12px; }

@font-face { font-family: 'Proxima Nova'; src: url(../fonts/ProximaNova-Regular.ttf); }
.b_vic { font: 35px/40px "Proxima Nova", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }

.b_vic .b_block { position: absolute; width: 570px; height: 316px; top: 50%; left: 50%; margin-top: -158px; margin-left: -285px; background-color: #fff; -moz-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -webkit-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); }

.b_vic .b_close { position: absolute; top: 14px; right: 10px; width: 32px; height: 32px; cursor: pointer; z-index: 20; background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3EVector%20Smart%20Object21%3C%2Ftitle%3E%3Cpolygon%20points%3D%2223.5%2022.12%2017.38%2016%2023.5%209.88%2022.12%208.5%2016%2014.62%209.88%208.5%208.5%209.88%2014.62%2016%208.5%2022.13%209.88%2023.5%2016%2017.38%2022.13%2023.5%2023.5%2022.12%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }

.b_vic .b_body { padding: 20px 28px 27px; position: relative; }

.b_vic .b_body img, .b_vic .b_body div { position: relative; z-index: 10; }

.b_vic .b_body:after { content: ''; display: block; width: 202px; height: 246px; position: absolute; bottom: 26px; right: 28px; background: url("../v2_images/bg_b2.png") no-repeat; }

.b_vic .b_logo { display: block; margin-bottom: 40px; }

.b_vic .b_logo .txt { font: 20px/32px "Proxima Nova", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }

.b_vic .b_logo_title { color: #2c77f9; display: block; font: 700 23px "Proxima Nova Bold", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }

.b_vic .b_title { max-width: 400px; margin-bottom: 50px; font-size: 28px; line-height: 34px; }

.b_vic .b_title b { font-weight: 700; color: #555; font-family: "Proxima Nova Bold", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }

.b_vic .b_btn { display: inline-block; cursor: pointer; color: #fff; background-color: #3f78fb; text-align: center; width: 165px; font: 20px/50px "Arial", sans-serif; -webkit-border-radius: 9px; -moz-border-radius: 9px; border-radius: 9px; }
.b_vic .b_btn img { position: relative; top: 8px; }

.b_vic .b_body .s_err { color: red; position: absolute; font-size: 14px; margin-top: 42px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 90%; }

.b_vic .b_body .s_ins { position: absolute; font-size: 14px; margin-top: 42px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; width: 90%; }

.progress_line { position: absolute; bottom: 0; left: 0; right: 0; height: 4px; background-color: #2b78f9; }

.b_tutorial { font: 16px/22px "Proxima Nova", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }
.b_tutorial .b_block { position: absolute; width: 600px; height: 418px; top: 50%; left: 50%; margin-top: -209px; margin-left: -300px; background-color: #f8f8f8; -moz-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -webkit-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); }
.b_tutorial .b_close { position: absolute; top: 6px; right: 5px; width: 32px; height: 32px; cursor: pointer; z-index: 20; background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3EVector%20Smart%20Object21%3C%2Ftitle%3E%3Cpolygon%20points%3D%2223.5%2022.12%2017.38%2016%2023.5%209.88%2022.12%208.5%2016%2014.62%209.88%208.5%208.5%209.88%2014.62%2016%208.5%2022.13%209.88%2023.5%2016%2017.38%2022.13%2023.5%2023.5%2022.12%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_tutorial .b_body { padding: 20px; position: relative; }
.b_tutorial .b_logo { color: #222f3a; overflow: hidden; font: 700 22px/24px "Proxima Nova Bold", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }
.b_tutorial .b_logo img { display: block; width: 50px; height: 50px; float: left; margin-right: 12px; }
.b_tutorial .slides_container { position: relative; height: 246px; overflow: hidden; }
.b_tutorial .slides_container .b_content { overflow-x: auto; white-space: nowrap; height: 450px; }
.b_tutorial .slides_container .b_content > div { display: inline-block; vertical-align: top; width: 100%; white-space: normal; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.b_tutorial .slides_container .inner { margin: 0 auto; padding-left: 34px; padding-right: 26px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.b_tutorial .slides_container .step_pic { width: 246px; height: 246px; display: block; float: right; margin-top: -60px; margin-left: 20px; }
.b_tutorial .slides_container .step1 .step_pic { background: url(../v2_images/tutorial/step1.svg) no-repeat; }
.b_tutorial .slides_container .step2 .step_pic { background: url(../v2_images/tutorial/step2.svg) no-repeat; }
.b_tutorial .slides_container .step3 .step_pic { background: url(../v2_images/tutorial/step3.svg) no-repeat; }
.b_tutorial .slides_container .step4 .step_pic { background: url(../v2_images/tutorial/step4.svg) no-repeat; }
.b_tutorial .slides_container .title { font-size: 20px; line-height: 26px; margin-top: 60px; margin-bottom: 3px; }
.b_tutorial .nav_bar { text-align: center; font-size: 0; margin-bottom: 20px; }
.b_tutorial .nav_bar .circle { width: 10px; height: 10px; display: inline-block; margin-right: 7px; }
.b_tutorial .nav_bar .circle:last-child { margin-right: 0; }
.b_tutorial .nav_bar .circle:before { content: ''; display: block; width: 8px; height: 8px; margin: 1px auto; border: 2px solid #d6d6d6; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-transition: all 0.1s linear; -o-transition: all 0.1s linear; -webkit-transition: all 0.1s linear; transition: all 0.1s linear; }
.b_tutorial .nav_bar .circle.active:before { width: 10px; height: 10px; margin: 0; background-color: #d6d6d6; -moz-transition: all 0.1s linear; -o-transition: all 0.1s linear; -webkit-transition: all 0.1s linear; transition: all 0.1s linear; }
.b_tutorial .btn { color: #fff; font-size: 20px; line-height: 40px; cursor: pointer; width: 134px; text-align: center; margin: 0 auto; overflow: hidden; background-color: #00af9d; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.b_tutorial .next, .b_tutorial .prev { width: 22px; height: 22px; position: absolute; top: 115px; cursor: pointer; }
.b_tutorial .prev { background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2044%2044%22%3E%3Ctitle%3EVector%20Smart%20Object3%3C%2Ftitle%3E%3Cpath%20d%3D%22M-65.83%2C53l-2.67%2C2.65-15-15%2C15-15%2C2.65%2C2.64L-78.23%2C40.69Z%22%20transform%3D%22translate(96.67%20-18.69)%22%20style%3D%22fill%3A%23323232%22%2F%3E%3Crect%20width%3D%2244%22%20height%3D%2244%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; left: -4px; }
.b_tutorial .next { background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2044%2044%22%3E%3Ctitle%3EVector%20Smart%20Object3%3C%2Ftitle%3E%3Cpath%20d%3D%22M-83.5%2C53l2.67%2C2.65%2C15-15-15-15-2.65%2C2.64%2C12.37%2C12.36Z%22%20transform%3D%22translate(96.67%20-18.69)%22%20style%3D%22fill%3A%23323232%22%2F%3E%3Crect%20width%3D%2244%22%20height%3D%2244%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; right: -4px; }
.b_tutorial .s_err { color: red; position: absolute; font-size: 14px; text-align: center; width: 100%; margin-top: -21px; left: 0; right: 0; }
.b_tutorial .loading_tutorial { display: block; margin: 7px auto; height: 26px; }
.b_tutorial .progress_line { position: absolute; bottom: 0; left: 0; right: 0; height: 4px; background-color: #00af9d; }
.b_tutorial .show_again { position: absolute; left: 54px; bottom: 30px; }
.b_tutorial .show_again input[type=checkbox] { display: none; }
.b_tutorial .show_again input[type=checkbox] + label { position: relative; padding-left: 25px; font-size: 14px; color: #666; }
.b_tutorial .show_again input[type=checkbox] + label:before { content: ''; display: block; width: 14px; height: 14px; position: absolute; top: 1px; left: 0; background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2014%2014%22%3E%3Ctitle%3EVector%20Smart%20Object5%3C%2Ftitle%3E%3Crect%20x%3D%220.5%22%20y%3D%220.5%22%20width%3D%2213%22%20height%3D%2213%22%20style%3D%22fill%3A%23fff%22%2F%3E%3Cpath%20d%3D%22M67.91-104.84v12h-12v-12h12m1-1h-14v14h14v-14Z%22%20transform%3D%22translate(-54.91%20105.84)%22%20style%3D%22fill%3A%23666%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_tutorial .show_again input[type=checkbox]:checked + label:before { background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2014%2014%22%3E%3Ctitle%3EVector%20Smart%20Object5%3C%2Ftitle%3E%3Crect%20x%3D%220.5%22%20y%3D%220.5%22%20width%3D%2213%22%20height%3D%2213%22%20style%3D%22fill%3A%23fff%22%2F%3E%3Cpath%20d%3D%22M67.91-104.84v12h-12v-12h12m1-1h-14v14h14v-14Z%22%20transform%3D%22translate(-54.91%20105.84)%22%20style%3D%22fill%3A%23666%22%2F%3E%3Cpolygon%20points%3D%225.45%2010.73%202%207.28%202.9%206.38%205.45%208.93%2011.1%203.27%2012%204.17%205.45%2010.73%22%20style%3D%22fill%3A%234fc7c3%22%2F%3E%3C%2Fsvg%3E") no-repeat; }

@font-face { font-family: 'Proxima Nova'; src: url(../fonts/ProximaNova-Regular.ttf); }
@font-face { font-family: 'Proxima Nova Bold'; src: url(../fonts/ProximaNova-Regular.ttf); }
.b_v3 { font: 400 16px/27px "Proxima Nova", "Helvetica Neue", "Helvetica", "Arial", sans-serif; }
.b_v3 .b_block { position: absolute; width: 600px; height: 418px; top: 50%; left: 50%; margin-top: -209px; margin-left: -300px; color: #fff; padding: 30px; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9Ii0wLjA5OTIwMSIgeTE9IjAuMTk0NjkyIiB4Mj0iMS4wOTkyMDEiIHkyPSIwLjgwNTMwOCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzMxMjI1ZCIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzFjMTUzZiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -moz-linear-gradient(333deg, #31225d 0%, #1c153f 100%); background-image: -webkit-linear-gradient(333deg, #31225d 0%, #1c153f 100%); background-image: linear-gradient(117deg, #31225d 0%, #1c153f 100%); -moz-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -webkit-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.b_v3 .b_block:after { content: ''; display: block; width: 245px; height: 272px; position: absolute; right: 0; top: 30px; background: url(../v2_images/box.png) no-repeat; }
.b_v3 .b_close { position: absolute; top: 6px; right: 5px; width: 32px; height: 32px; cursor: pointer; z-index: 20; background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3E%D0%92%D0%B5%D0%BA%D1%82%D0%BE%D1%80%D0%BD%D1%8B%D0%B9%20%D1%81%D0%BC%D0%B0%D1%80%D1%82-%D0%BE%D0%B1%D1%8A%D0%B5%D0%BA%D1%82%3C%2Ftitle%3E%3Cline%20x1%3D%2210%22%20y1%3D%2210%22%20x2%3D%2222%22%20y2%3D%2222%22%20style%3D%22fill%3Anone%3Bstroke%3A%23fff%3Bstroke-miterlimit%3A10%22%2F%3E%3Cline%20x1%3D%2210%22%20y1%3D%2222%22%20x2%3D%2222%22%20y2%3D%2210%22%20style%3D%22fill%3Anone%3Bstroke%3A%23fff%3Bstroke-miterlimit%3A10%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_v3 .b_logo { overflow: hidden; font-size: 20px; line-height: 26px; margin-bottom: 56px; }
.b_v3 .b_logo img { display: block; width: 48px; height: 48px; float: left; margin-right: 12px; }
.b_v3 .b_title { font: 700 30px/40px "Proxima Nova Bold", "Helvetica Neue", "Helvetica", "Arial", sans-serif; margin-bottom: 10px; max-width: 235px; }
.b_v3 .b_title, .b_v3 .b_text { padding-left: 10px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.b_v3 .b_text { max-width: 290px; }
.b_v3 .b_btn_wrap { position: absolute; left: 0; right: 0; bottom: 16px; font-size: 0; text-align: center; }
.b_v3 .b_btn_wrap .b_btn { display: inline-block; position: relative; overflow: hidden; width: 114px; text-align: center; cursor: pointer; background-color: #dd4584; font: 700 20px/40px "Proxima Nova Bold", "Helvetica Neue", "Helvetica", "Arial", sans-serif; -moz-border-radius: 6px; -webkit-border-radius: 6px; border-radius: 6px; }
.b_v3 .b_btn_wrap .b_btn.is_loading { cursor: default; }
.b_v3 .b_btn_wrap .b_btn.is_loading:hover:before { background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIgc3RvcC1vcGFjaXR5PSIwLjMiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZmZmZmYiIHN0b3Atb3BhY2l0eT0iMC4wIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNncmFkKSIgLz48L3N2Zz4g'); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, rgba(255, 255, 255, 0.3)), color-stop(100%, rgba(255, 255, 255, 0))); background-image: -moz-linear-gradient(top, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); background-image: -webkit-linear-gradient(top, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); background-image: linear-gradient(to bottom, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); }
.b_v3 .b_btn_wrap .b_btn:before { content: ''; display: block; height: 29px; position: absolute; top: 0; left: 0; right: 0; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIgc3RvcC1vcGFjaXR5PSIwLjMiLz48c3RvcCBvZmZzZXQ9IjEwMCUiIHN0b3AtY29sb3I9IiNmZmZmZmYiIHN0b3Atb3BhY2l0eT0iMC4wIi8+PC9saW5lYXJHcmFkaWVudD48L2RlZnM+PHJlY3QgeD0iMCIgeT0iMCIgd2lkdGg9IjEwMCUiIGhlaWdodD0iMTAwJSIgZmlsbD0idXJsKCNncmFkKSIgLz48L3N2Zz4g'); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, rgba(255, 255, 255, 0.3)), color-stop(100%, rgba(255, 255, 255, 0))); background-image: -moz-linear-gradient(top, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); background-image: -webkit-linear-gradient(top, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); background-image: linear-gradient(to bottom, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 100%); }
.b_v3 .b_btn_wrap .b_btn:hover:before { background-image: none; }
.b_v3 .b_btn_wrap .b_loading { display: block; margin: 7px auto; width: 26px; }
.b_v3 .b_btn_wrap .s_err { color: #fff; font-size: 12px; }
.b_v3 .progress_line { background-color: #ff4487; }

.b_universal_bnr.popup__overlay { display: flex; justify-content: center; align-items: center; }
.b_universal_bnr .flex_item { margin: auto; max-height: 70vh; max-width: 70vw; }
.b_universal_bnr .b_block { position: relative; }
.b_universal_bnr .b_close { position: absolute; top: 6px; right: 5px; width: 32px; height: 32px; cursor: pointer; z-index: 20; background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3EVector%20Smart%20Object21%3C%2Ftitle%3E%3Cpolygon%20points%3D%2223.5%2022.12%2017.38%2016%2023.5%209.88%2022.12%208.5%2016%2014.62%209.88%208.5%208.5%209.88%2014.62%2016%208.5%2022.13%209.88%2023.5%2016%2017.38%2022.13%2023.5%2023.5%2022.12%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_universal_bnr .bnr_image { max-width: 70vw; max-height: 70vh; display: block; object-fit: contain; }
.b_universal_bnr .click_area { position: absolute; top: 0; bottom: 0; left: 0; right: 0; cursor: pointer !important; }
.b_universal_bnr .b_close.white { background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3EVector%20Smart%20Object21%3C%2Ftitle%3E%3Cpolygon%20fill%3D%22white%22%20points%3D%2223.5%2022.12%2017.38%2016%2023.5%209.88%2022.12%208.5%2016%2014.62%209.88%208.5%208.5%209.88%2014.62%2016%208.5%2022.13%209.88%2023.5%2016%2017.38%2022.13%2023.5%2023.5%2022.12%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_universal_bnr .b_close.red { background: url("data:image/svg+xml;charset=utf-8,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20viewBox%3D%220%200%2032%2032%22%3E%3Ctitle%3EVector%20Smart%20Object21%3C%2Ftitle%3E%3Cpolygon%20fill%3D%22%23d65656%22%20points%3D%2223.5%2022.12%2017.38%2016%2023.5%209.88%2022.12%208.5%2016%2014.62%209.88%208.5%208.5%209.88%2014.62%2016%208.5%2022.13%209.88%2023.5%2016%2017.38%2022.13%2023.5%2023.5%2022.12%22%2F%3E%3Crect%20width%3D%2232%22%20height%3D%2232%22%20style%3D%22fill%3Anone%22%2F%3E%3C%2Fsvg%3E") no-repeat; }
.b_universal_bnr .custom_clickable { display: none; cursor: pointer !important; }

.dark_theme { background-color: #272829; color: #d8d8d8; }
.dark_theme.mac_cef #react-toolbar-container { background: #1a2025; }
.dark_theme.mac_cef .header-nav .update_button, .dark_theme.mac_cef .header-nav .button_menu, .dark_theme.mac_cef .header-nav .button_back { background-image: none; }
.dark_theme.mac_cef .header-nav .header-search input { background-color: #272d32; border-color: #51565a; color: #dedede; }
.dark_theme .delete .files_list li, .dark_theme .hash_popup .delete .files_list li, .dark_theme .delete input[type=checkbox] + label { color: #d8d8d8; }
.dark_theme .bottom-row-header ul li.tab-active { color: #eaeaea; }
.dark_theme .tab-general .general-tab-wrap .download-detalization-title { color: #d8d8d8; }
.dark_theme .compact-view-info ul li.sort-down:after, .compact-view-info ul .dark_theme li.sort-down:after, .dark_theme .compact-view-info ul li.sort-up:after, .compact-view-info ul .dark_theme li.sort-up:after, .dark_theme .table-headers li.sort-down:after, .table-headers .dark_theme li.sort-down:after, .dark_theme .table-headers li.sort-up:after, .table-headers .dark_theme li.sort-up:after, .dark_theme .block_error:before, .dark_theme .macosx .popup .header .close_button, .macosx .popup .header .dark_theme .close_button, .dark_theme .macosx .delete .header .close_button, .macosx .delete .header .dark_theme .close_button, .dark_theme .macosx .add_url .header .close_button, .macosx .add_url .header .dark_theme .close_button, .dark_theme .macosx .scheduler .header .close_button, .macosx .scheduler .header .dark_theme .close_button, .dark_theme .macosx .change_url_wrapper .header .close_button, .macosx .change_url_wrapper .header .dark_theme .close_button, .dark_theme .macosx .simple .simple_dialog .header .close_button, .macosx .simple .simple_dialog .header .dark_theme .close_button, .dark_theme .bottom-panel-opener, .dark_theme .close-icon, .dark_theme .header-tags .manage_btn, .header-tags .dark_theme .manage_btn, .dark_theme .header-tags .show_more, .header-tags .dark_theme .show_more, .dark_theme .header-tags .wrapper_tags .choose_color, .header-tags .wrapper_tags .dark_theme .choose_color, .dark_theme .header-tags .triangle, .header-tags .dark_theme .triangle, .dark_theme .wrapper_inselect.top .list div.active:before, .wrapper_inselect.top .list .dark_theme div.active:before, .dark_theme .wrap_default_client .cancel_btn, .wrap_default_client .dark_theme .cancel_btn, .dark_theme .download-list .row.download-ending .download-complete, .download-list .row.download-ending .dark_theme .download-complete, .dark_theme .download-list .tags span.show_tags, .download-list .tags .dark_theme span.show_tags, .dark_theme .download-list .error-message, .download-list .dark_theme .error-message, .dark_theme .tab-general .folder_icon, .tab-general .dark_theme .folder_icon, .dark_theme .tab-general .general-tab-wrap .wrapper-user-tags .close, .tab-general .general-tab-wrap .wrapper-user-tags .dark_theme .close, .dark_theme .tab-general .general-tab-wrap .error-message, .tab-general .general-tab-wrap .dark_theme .error-message, .dark_theme .files-table li.folder > .triangle, .files-table .dark_theme li.folder > .triangle, .dark_theme .files-table li .pad .error-message, .files-table li .pad .dark_theme .error-message, .dark_theme .traffic-bar > ul:after, .dark_theme .traffic-bar > ul .down-speed-chooser:before, .traffic-bar > ul .dark_theme .down-speed-chooser:before, .dark_theme .traffic-bar > ul .up-speed-chooser:before, .traffic-bar > ul .dark_theme .up-speed-chooser:before, .dark_theme .traffic-bar .speed-chooser li.selected span:before, .traffic-bar .speed-chooser li.selected .dark_theme span:before, .dark_theme .traffic-bar .download_title.error, .traffic-bar .dark_theme .download_title.error, .dark_theme .close_button, .dark_theme .popup_exist .add_url .close_button, .popup_exist .add_url .dark_theme .close_button, .dark_theme .simple .simple_dialog .close_button, .simple .simple_dialog .dark_theme .close_button, .dark_theme .popup .wrap_tags .manage_btn, .popup .wrap_tags .dark_theme .manage_btn, .dark_theme .popup .wrapper_tree ins, .popup .wrapper_tree .dark_theme ins, .dark_theme .dropdown-settings .rows-group .menu-row.shutdown:before, .dropdown-settings .rows-group .dark_theme .menu-row.shutdown:before, .dark_theme .wrapper_updates:before, .dark_theme .wrapper_updates .cancel_btn, .wrapper_updates .dark_theme .cancel_btn, .dark_theme .settings_wrapper .info:before, .settings_wrapper .dark_theme .info:before { background-image: url("../v2_images/dark_theme/elements.svg?51120"); }
.dark_theme.mac_cef .header-nav .button_menu:before, .header-nav .dark_theme.mac_cef .button_menu:before, .dark_theme.mac_cef .header-nav .update_button:before, .header-nav .dark_theme.mac_cef .update_button:before, .dark_theme.mac_cef .button_back:before, .dark_theme.mac_cef .header-nav .add-download, .header-nav .dark_theme.mac_cef .add-download, .dark_theme.mac_cef .header-nav .start-download, .header-nav .dark_theme.mac_cef .start-download, .dark_theme.mac_cef .header-nav .pause-download, .header-nav .dark_theme.mac_cef .pause-download, .dark_theme.mac_cef .header-nav .cancel-download, .header-nav .dark_theme.mac_cef .cancel-download, .dark_theme.mac_cef .header-nav .action_folder, .header-nav .dark_theme.mac_cef .action_folder, .dark_theme.mac_cef .header-nav .header-search input, .header-nav .header-search .dark_theme.mac_cef input, .dark_theme.mac_cef .header-nav .update_button .update_icon, .header-nav .update_button .dark_theme.mac_cef .update_icon { background-image: url("../v2_images/dark_theme/header_button.svg"); }
.dark_theme input[type=checkbox] + label:before, .dark_theme .compact-view-info ul li input[type=checkbox] + label:before, .compact-view-info ul li .dark_theme input[type=checkbox] + label:before, .dark_theme .checkbox-fake, .dark_theme .files-table li .fake_checkbox, .files-table li .dark_theme .fake_checkbox, .dark_theme .settings_wrapper .downloads_tab input[type=radio] + label:before, .settings_wrapper .downloads_tab .dark_theme input[type=radio] + label:before, .dark_theme .settings_wrapper .connection_tab input[type=radio] + label:before, .settings_wrapper .connection_tab .dark_theme input[type=radio] + label:before, .dark_theme .settings_wrapper .monitoring_tab input[type=radio] + label:before, .settings_wrapper .monitoring_tab .dark_theme input[type=radio] + label:before { background-image: url("../v2_images/dark_theme/checkbox.svg"); }
.dark_theme .download-state, .dark_theme .settings_wrapper .right_panel .button_folder, .settings_wrapper .right_panel .dark_theme .button_folder { background-image: url("../v2_images/dark_theme/actions.svg"); }
.dark_theme .content { background-color: #262c30; }
.dark_theme .first_view img, .dark_theme .loading img, .dark_theme .loader img, .dark_theme .share_modal.pre_loader .wrapper_popup img { content: url("../v2_images/dark_theme/preloading_FDM.gif"); }
.dark_theme .yt_channel_spinner img { width: 40px; height: 40px; content: url("../v2_images/dark_theme/preloading_batch_FDM.gif"); }
.dark_theme .header-nav { border-bottom: none; }
.dark_theme .header-nav .add-download { color: #007dc2; }
.dark_theme .header-nav .button_menu, .dark_theme .header-nav .update_button, .dark_theme .button_back { border-color: #51565a; background-color: #1a2025; }
.dark_theme.windows .header-nav { background-color: #1a2025; }
.dark_theme .dropdown-settings .border-block { background-color: #1a2025; }
.dark_theme .dropdown-settings .rows-group .shutdown_group { border: none; }
.dark_theme .dropdown-settings .rows-group hr { background-color: #262c30; }
.dark_theme .dropdown-settings .rows-group .menu-row:hover, .dark_theme .block-edit-tags a:hover, .dark_theme .dropdown-settings .rows-group .shutdown_group, .dark_theme .right_panel .wrapper_inselect .list div:hover, .dark_theme .popup .list div:hover, .dark_theme .delete .list div:hover, .dark_theme .add_url .list div:hover, .dark_theme .delete .list div:hover, .dark_theme .scheduler .list div:hover, .dark_theme .change_url_wrapper .list div:hover, .dark_theme .popup_exist .add_url .list div:hover, .dark_theme .simple .simple_dialog .list div:hover { background-color: #3a3939; }
.dark_theme .tag_element, .dark_theme .header-tags .manage_btn, .dark_theme .header-tags .show_more { background-color: #20262a; color: #8f9295; }
.dark_theme .tag_element.active { color: #eaeaea; }
.dark_theme .header-tags .wrapper_tags .name_fortag { background-color: #e5e5e5; }
.dark_theme .compact-view-info:after { width: 20px; height: 24px; right: 9px; top: 1px; }
.dark_theme .header-tags .wrapper_tags .wrap_new_tag { border-color: #51565a; }
.dark_theme .download-list, .dark_theme .compact-view-info, .dark_theme .compact-view-info:after, .dark_theme .content .filter-no-results, .dark_theme .header-tags .wrapper_tags, .dark_theme .block-edit-tags, .dark_theme .traffic-bar .speed-chooser, .dark_theme .wrapper_strt_dwlnd .start_add_download { background-color: #272829; }
.dark_theme .content .filter-no-results .notification, .dark_theme .block-edit-tags a, .dark_theme .info-time .download-time, .dark_theme .popup .title-input { color: #d8d8d8; }
.dark_theme .compact-view-info, .dark_theme .download-list { border-color: #51565a; }
.dark_theme .compact-view-info { border-bottom: 1px solid #262c30; }
.dark_theme .compact-view-info ul li { border-right-color: #262c30; color: #8f9295; }
.dark_theme .download-list .row { border-top-color: #262c30; }
.dark_theme .download-list .row:last-child { border-bottom-color: #262c30; }
.dark_theme .download-list .row.current:before, .dark_theme .files-table li.current:before { border-color: #007dc2; }
.dark_theme .download-list .row.selected, .dark_theme .download-list .row.drop_is_active { background-color: rgba(134, 134, 134, 0.45); }
.dark_theme .files-table li.tree-node-selected { background-color: rgba(90, 85, 85, 0.45); }
.dark_theme .content .main-column.innactive .row.selected, .dark_theme .content .main-column.innactive .row.drop_is_active { background-color: rgba(242, 242, 242, 0.45); }
.dark_theme .download-list .row.is_queued .compact-progress-line, .dark_theme .compact-progress-line, .dark_theme .download-list .row.move_progress .compact-progress-line, .dark_theme .download-list .row.paused .compact-progress-line, .dark_theme .files-table li .progress-wrap, .dark_theme .tab-general .general-tab-wrap .progress-line-wrap, .dark_theme .tab-general .general-tab-wrap .downloading_paused_line .progress-line-wrap, .dark_theme .tab-general .general-tab-wrap .is_queued .progress-line-wrap { background-color: #777; }
.dark_theme .download-list .row.move_progress .compact-download-progress, .dark_theme .compact-download-progress, .dark_theme .files-table li .progress-wrap .progress, .dark_theme .tab-general .general-tab-wrap .progress-line-wrap .progress, .dark_theme .tab-general .general-tab-wrap .progress-line-wrap .progress { background-color: #007dc2; }
.dark_theme .no-size span, .dark_theme .settings_wrapper .right_panel, .dark_theme .settings_wrapper .tum_tab table tbody td:first-child, .dark_theme .popup .enable_scheduler input[type=checkbox] + label, .dark_theme .scheduler .wrapper_times > span, .dark_theme .popup input[type=checkbox] + label, .dark_theme .scheduler input[type=checkbox] + label, .dark_theme .youtube_channel .quality_wrapper label { color: #dedede; }
.dark_theme .no-size div, .dark_theme .tab-general .general-tab-wrap .progress-wrap.unknown_size .progress-line-wrap, .dark_theme .wrapper_updates .checking .compact-progress-line { background-image: url("../v2_images/dark_theme/line_small.gif"); }
.dark_theme.compact-view-shown .compact-progress-wrap .no-size.downloading_paused_line div, .dark_theme .tab-general .general-tab-wrap .progress-wrap.unknown_size.downloading_paused_line .progress-line-wrap, .dark_theme.compact-view-shown .is_queued .compact-progress-wrap .no-size div, .dark_theme .tab-general .general-tab-wrap .progress-wrap.unknown_size.is_queued .progress-line-wrap { background-color: #777; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuMCIgeTE9IjAuMCIgeDI9IjEuMCIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIyNSUiIHN0b3AtY29sb3I9IiNhY2FjYWMiLz48c3RvcCBvZmZzZXQ9IjI1JSIgc3RvcC1jb2xvcj0iIzAwMDAwMCIgc3RvcC1vcGFjaXR5PSIwLjAiLz48c3RvcCBvZmZzZXQ9IjUwJSIgc3RvcC1jb2xvcj0iIzAwMDAwMCIgc3RvcC1vcGFjaXR5PSIwLjAiLz48c3RvcCBvZmZzZXQ9IjUwJSIgc3RvcC1jb2xvcj0iI2FjYWNhYyIvPjxzdG9wIG9mZnNldD0iNzUlIiBzdG9wLWNvbG9yPSIjYWNhY2FjIi8+PHN0b3Agb2Zmc2V0PSI3NSUiIHN0b3AtY29sb3I9IiMwMDAwMDAiIHN0b3Atb3BhY2l0eT0iMC4wIi8+PHN0b3Agb2Zmc2V0PSIxMDAlIiBzdG9wLWNvbG9yPSIjMDAwMDAwIiBzdG9wLW9wYWNpdHk9IjAuMCIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -moz-linear-gradient(315deg, #acacac 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #acacac 50%, #acacac 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); background-image: -webkit-linear-gradient(315deg, #acacac 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #acacac 50%, #acacac 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); background-image: linear-gradient(135deg, #acacac 25%, rgba(0, 0, 0, 0) 25%, rgba(0, 0, 0, 0) 50%, #acacac 50%, #acacac 75%, rgba(0, 0, 0, 0) 75%, rgba(0, 0, 0, 0)); background-size: 30px 30px; }
.dark_theme .wrap_default_client { background-color: #63848E; }
.dark_theme .wrap_default_client.about_sleep { background-color: #63848E; color: #dedede; }
.dark_theme .wrap_default_client a { background-color: #dedede; color: #272829; }
.dark_theme .wrapper_strt_dwlnd .wrapper { color: rgba(216, 216, 216, 0.84); }
.dark_theme .wrapper_strt_dwlnd .wrapper .big_title { color: #d8d8d8; }
.dark_theme .wrapper_strt_dwlnd .start_add_download { border: 2px dashed #05596f; background-image-source: url("../v2_images/dark_theme/border.png"); }
.dark_theme .content .filter-no-results .notification a { color: #0c7eba; }
.dark_theme .traffic-bar { background-color: #0f1519; border-top: none; }
.dark_theme .traffic-bar .download_title { color: #8f9295; }
.dark_theme .traffic-bar > ul { border-left: none; border-right: none; color: #fff; }
.dark_theme .traffic-bar > ul .down-speed-chooser.high, .dark_theme .traffic-bar > ul .up-speed-chooser.high { background-color: rgba(0, 176, 42, 0.1); }
.dark_theme .traffic-bar > ul .down-speed-chooser.high:after, .dark_theme .traffic-bar > ul .up-speed-chooser.high:after { background-color: #039429; }
.dark_theme .traffic-bar > ul .down-speed-chooser.low:after, .dark_theme .traffic-bar > ul .down-speed-chooser.medium:after, .dark_theme .traffic-bar > ul .down-speed-chooser.high:after, .dark_theme .traffic-bar > ul .down-speed-chooser.manual:after, .dark_theme .traffic-bar > ul .up-speed-chooser.low:after, .dark_theme .traffic-bar > ul .up-speed-chooser.medium:after, .dark_theme .traffic-bar > ul .up-speed-chooser.high:after, .dark_theme .traffic-bar > ul .up-speed-chooser.manual:after { bottom: 8px; }
.dark_theme .traffic-bar .speed-chooser { border: none; }
.dark_theme .bottom-row { border-color: #51565a; background-color: #272829; color: #8f9295; }
.dark_theme .bottom-row-header { background-color: #0f1519; border-bottom-color: #0f1519; }
.dark_theme .bottom-row-header ul li.tab-active:before { background-color: #007dc2; }
.dark_theme .tab-files .table-headers, .dark_theme .tab-trackers .table-headers, .dark_theme .tab-peers .table-headers, .dark_theme .tab-log .table-headers { border-bottom-color: #262c30; }
.dark_theme .resizer { border-right-color: #262c30; }
.dark_theme .files-table li .percents, .dark_theme .tab-general .general-tab-wrap .process, .dark_theme .tab-general .wrapper_table table td, .dark_theme .tab-general .info_comment .cmnt { color: #8f9295; }
.dark_theme .tab-general .info_comment .cmnt.value a, .dark_theme .bottom-row .tab-content.error .process.moving, .dark_theme .tab-general .general-tab-wrap .percents { color: #007dc2; }
.dark_theme .tab-general .general-tab-wrap .is_queued .progress, .dark_theme .tab-general .general-tab-wrap .downloading_paused_line .progress { background-color: #ACACAC; }
.dark_theme .progress_background { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 10 10"><title>white</title><path d="M8,2V8H2V2H8M9,1H1V9H9Z" style="fill:#8f9295"/><rect width="10" height="10" style="fill:none"/></svg>'); }
.dark_theme .progress_downloaded { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 10 10"><title>blue</title><path d="M9,1H1V9H9Z" style="fill:#8bd0fb"/><path d="M8,2V8H2V2H8M9,1H1V9H9Z" style="fill:#007dc2"/><rect width="10" height="10" style="fill:none"/></svg>'); }
.dark_theme .tab-general .general-tab-wrap .progress-wrap.unknown_size .progress-line-wrap { background-image: url("../v2_images/dark_theme/line.gif"); }
.dark_theme .right_panel .wrapper_inselect .list, .dark_theme .first_view { background-color: #272d32; }
.dark_theme .settings_wrapper { background-color: #272d32; }
.dark_theme .settings_wrapper .right_panel .group_title { border-bottom-color: #1b2026; color: #d8d8d8; }
.dark_theme .settings_wrapper .right_panel .tab_title { color: #999; }
.dark_theme .settings_wrapper .right_panel .wrapper_inselect, .dark_theme .settings_wrapper .right_panel .wrapper_inselect .list, .dark_theme .settings_wrapper .right_panel .settings_wrapper .right_panel input[type=text].simple_input, .dark_theme .settings_wrapper .right_panel .settings_wrapper .right_panel input[type=password].simple_input { border-color: #51565a; }
.dark_theme .settings_wrapper .info { color: #737373; }
.dark_theme .settings_wrapper .info a { color: #007dc2; }
.dark_theme .settings_wrapper .connection_tab .wrap_fake_table.disabled, .dark_theme .settings_wrapper .connection_tab .wrap_fake_table .disabled { color: rgba(134, 134, 134, 0.5); }
.dark_theme .settings_wrapper button.associate { background-color: #2A8CC1; }
.dark_theme .popup .select_buttons, .dark_theme .delete .select_buttons, .dark_theme .add_url .select_buttons, .dark_theme .delete .select_buttons, .dark_theme .scheduler .select_buttons, .dark_theme .change_url_wrapper .select_buttons, .dark_theme .popup_exist .add_url .select_buttons, .dark_theme .simple .simple_dialog .select_buttons { color: #007dc2; }
.dark_theme .wrapper_updates .btn { background-color: #2A8CC1; }
.dark_theme .settings_wrapper .right_panel input[type=text], .dark_theme .settings_wrapper .right_panel input[type=password] { background-color: #272d32; border-color: #51565a; color: #dedede; }
.dark_theme input[type=checkbox]:disabled + label, .dark_theme input[type=checkbox]:disabled + label .absolute, .dark_theme input[type=checkbox]:disabled + label .absolute input[type=text], .dark_theme .add_url input[type=checkbox]:disabled + label { color: rgba(222, 222, 222, 0.3); }
.dark_theme .scheduler input[type=checkbox]:disabled + label, .dark_theme .scheduler .wrapper_times.disabled span, .dark_theme .scheduler .wrapper_times.disabled span { color: rgba(222, 222, 222, 0.3); }
.dark_theme .settings_wrapper .right_panel input[type=text]:disabled, .dark_theme .settings_wrapper .right_panel input[type=password]:disabled { border-color: #505659; color: rgba(222, 222, 222, 0.3); background-color: rgba(136, 136, 136, 0.3); }
.dark_theme .settings_wrapper .right_panel span.disable { color: rgba(222, 222, 222, 0.3); }
.dark_theme .mount { background-color: rgba(0, 0, 0, 0.65); }
.dark_theme.macosx .popup .header, .dark_theme.macosx .delete .header, .dark_theme.macosx .add_url .header, .dark_theme.macosx .delete .header, .dark_theme.macosx .scheduler .header, .dark_theme.macosx .change_url_wrapper .header, .dark_theme.macosx .popup_exist .add_url .header, .dark_theme.macosx .simple .simple_dialog .header, .dark_theme .popup .center_right { background: #1b2026; color: #dedede; }
.dark_theme .download-wiz-source-info .popup, .dark_theme .hash_popup .delete, .dark_theme .popup .add_url, .dark_theme .popup.choose-speed, .dark_theme .popup__overlay .delete, .dark_theme .scheduler, .dark_theme .change_url_wrapper, .dark_theme .popup_exist .add_url, .dark_theme .simple .simple_dialog, .dark_theme .popup_extension .popup, .dark_theme .wrapper_updates { background-color: #262c30; color: #dedede; }
.dark_theme .download-wiz-source-info .popup, .dark_theme .hash_popup .delete, .dark_theme .popup .add_url, .dark_theme .popup.choose-speed, .dark_theme .popup__overlay .delete, .dark_theme .scheduler, .dark_theme .change_url_wrapper, .dark_theme .popup_exist .add_url, .dark_theme .simple .simple_dialog, .dark_theme .popup_extension .popup, .dark_theme .b_v3 .b_block, .dark_theme .wrapper_updates { -moz-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); -webkit-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); }
.dark_theme .wrapper_inselect .dropdown_button { border-left-color: #272d32; }
.dark_theme .popup .inselect, .dark_theme .popup .center_right { border-color: #51565a; }
.dark_theme .center-choose-speed .wrapper_inselect, .dark_theme .popup .inselect .dropdown_button, .dark_theme .popup input[type=text].path_field, .dark_theme .popup input[type=text], .dark_theme .delete input[type=text].path_field, .dark_theme .delete input[type=text], .dark_theme .add_url input[type=text].path_field, .dark_theme .add_url input[type=text], .dark_theme .delete input[type=text].path_field, .dark_theme .delete input[type=text], .dark_theme .scheduler input[type=text].path_field, .dark_theme .scheduler input[type=text], .dark_theme .change_url_wrapper input[type=text].path_field, .dark_theme .change_url_wrapper input[type=text], .dark_theme .popup_exist .add_url input[type=text].path_field, .dark_theme .popup_exist .add_url input[type=text], .dark_theme .simple .simple_dialog input[type=text].path_field, .dark_theme .simple .simple_dialog input[type=text], .dark_theme .popup .list, .dark_theme .delete .list, .dark_theme .add_url .list, .dark_theme .delete .list, .dark_theme .scheduler .list, .dark_theme .change_url_wrapper .list, .dark_theme .popup_exist .add_url .list, .dark_theme .simple .simple_dialog .list { background-color: #272d32; border-color: #51565a; color: #dedede; }
.dark_theme .popup .wrapper_tree input[type="checkbox"]:checked + label, .dark_theme .popup .wrapper_tree .file_size, .dark_theme .popup .total { color: #686a6b; }
.dark_theme .popup .wrapper_tree input[type="checkbox"] + label, .dark_theme .youtube_channel .file_date { color: #8e9192; }
.dark_theme .popup input[type=checkbox] + label:before, .dark_theme .delete input[type=checkbox] + label:before, .dark_theme .add_url input[type=checkbox] + label:before, .dark_theme .delete input[type=checkbox] + label:before, .dark_theme .scheduler input[type=checkbox] + label:before, .dark_theme .change_url_wrapper input[type=checkbox] + label:before, .dark_theme .popup_exist .add_url input[type=checkbox] + label:before, .dark_theme .simple .simple_dialog input[type=checkbox] + label:before, .dark_theme .popup .wrapper_tree input[type=checkbox] + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>1</title><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#b6b7b7"/></svg>') no-repeat; }
.dark_theme .popup input[type=checkbox]:checked + label:before, .dark_theme .delete input[type=checkbox]:checked + label:before, .dark_theme .add_url input[type=checkbox]:checked + label:before, .dark_theme .delete input[type=checkbox]:checked + label:before, .dark_theme .scheduler input[type=checkbox]:checked + label:before, .dark_theme .change_url_wrapper input[type=checkbox]:checked + label:before, .dark_theme .popup_exist .add_url input[type=checkbox]:checked + label:before, .dark_theme .simple .simple_dialog input[type=checkbox]:checked + label:before, .dark_theme .popup .wrapper_tree input[type=checkbox]:checked + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>1</title><rect width="12" height="12" style="fill:none"/><path d="M22.65,12.37l4.5-4.5,1.13,1-5.62,5.7L20,11.87l1.2-1Z" transform="translate(-17.92 -5.25)" style="fill:#7c7c7c"/><path d="M29.17,6V16.5H18.67V6h10.5m.75-.75h-12v12h12Z" transform="translate(-17.92 -5.25)" style="fill:#838383"/></svg>') no-repeat; }
.dark_theme .popup.scheduler_on .scheduler { background-color: #3e464c; border: none; }
.dark_theme .wrapper_inselect { background-color: #282d32; border: 1px solid #505659; }
.dark_theme .single-file.with_note .note_info, .dark_theme .change_path.with_note .note_info { background-color: #8ba9bb; }
.dark_theme .single-file.with_note .note_info:before, .dark_theme .change_path.with_note .note_info:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><title>i</title><path d="M10,0A10,10,0,1,0,20,10,10,10,0,0,0,10,0Z" style="fill:#007dc2"/><rect x="9" y="9" width="2" height="6" style="fill:#fff"/><rect x="9" y="5" width="2" height="2" style="fill:#fff"/></svg>') no-repeat; }
.dark_theme .popup .total span.error_state { color: #bf2f2f; }
.dark_theme.macosx .popup .header .close_button, .dark_theme.macosx .delete .header .close_button, .dark_theme.macosx .add_url .header .close_button, .dark_theme.macosx .delete .header .close_button, .dark_theme.macosx .scheduler .header .close_button, .dark_theme.macosx .change_url_wrapper .header .close_button, .dark_theme.macosx .popup_exist .add_url .header .close_button, .dark_theme.macosx .simple .simple_dialog .header .close_button { background-image: url("../v2_images/dark_theme/elements.svg?51120"); }
.dark_theme .share_modal.pre_loader .wrapper_popup { background-color: #272d32; }
.dark_theme .wrapper_updates.error { color: #bc3737; background-color: #262c30; border-color: transparent; }
.dark_theme .wrapper_updates.error:before { background-position: 0 -216px; }
.dark_theme.windows .header-nav .add-download, .dark_theme.windows .header-nav .start-download, .dark_theme.windows .header-nav .pause-download, .dark_theme.windows .header-nav .cancel-download, .dark_theme.windows .header-nav .action_folder { background-image: none; }
.dark_theme.windows .header-nav .add-download:before, .dark_theme.windows .header-nav .start-download:before, .dark_theme.windows .header-nav .pause-download:before, .dark_theme.windows .header-nav .cancel-download:before, .dark_theme.windows .header-nav .action_folder:before { background-image: url("../v2_images/dark_theme/header_button_win.svg"); }
.dark_theme.windows .header-nav .header-search.show_search .input-text { border-color: #51565a; color: #dedede; background-color: #272d32; background-image: url("../v2_images/dark_theme/header_button_win.svg"); }
.dark_theme.windows .header-nav .header-search.show_search .input-text::-webkit-input-placeholder { color: #dedede; }
.dark_theme .share_modal .wrapper_popup .balloon { background-color: #262c30; color: #dedede; }
.dark_theme .share_modal .wrapper_popup .balloon:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 42 26"><defs><style>.a{fill:#262c30;}</style></defs><title>no_difference</title><polygon class="a" points="0 8.96 42 0 16.24 26 0 8.96"/></svg>') no-repeat; }
.dark_theme ::-webkit-scrollbar { width: 15px; background-color: #3B3B3B; }
.dark_theme ::-webkit-scrollbar-thumb { border-width: 1px 1px 1px 2px; border-color: #777; background-color: #4F5153; }

@font-face { font-family: "Roboto Condensed Light"; src: url(../RobotoCondensed-Light.ttf); }
@font-face { font-family: "Roboto Condensed"; src: url(../RobotoCondensed-Regular.ttf); }
#right-panel, .sort { display: none !important; }

/*main styles: start*/
body { overflow: hidden; height: 100%; min-height: 100%; font: 14px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; }

body.drag_n_drop_in_progress *, body.drag_n_drop_in_progress .log-info .log-row span { cursor: no-drop !important; }

body.drag_n_drop_in_progress .tab-general *, body.drag_n_drop_in_progress .tab-general .info_path .value.path, body.drag_n_drop_in_progress .download-list * { cursor: copy !important; }

body.shut-down-when-done-message .wrap_default_client { position: static; }

body.shut-down-when-done-message .wrapper_strt_dwlnd { top: 124px; }

.for_copy { cursor: text !important; -webkit-user-select: auto; }

.first_view { position: fixed; top: 60px; bottom: 20px; background-color: #fff; width: 100%; z-index: 220; }
.first_view img { position: absolute; top: 50%; left: 50%; margin-top: -27px; margin-left: -27px; }

.block_error { background-color: #fde3e3; border: 1px solid #cfa6a9; color: #8f272b; font-size: 13px; padding: 7px 12px 8px; height: 32px; position: absolute; white-space: nowrap; top: -6px; z-index: 40; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-shadow: rgba(151, 151, 151, 0.6) 0 1px 4px 0; -webkit-box-shadow: rgba(151, 151, 151, 0.6) 0 1px 4px 0; box-shadow: rgba(151, 151, 151, 0.6) 0 1px 4px 0; }
.block_error:before { content: ''; display: block; width: 7px; height: 12px; position: absolute; top: 10px; }
.block_error.left { right: -webkit-calc(100% + 10px); right: calc(100% + 10px); margin-top: -1px; }
.block_error.left:before { right: -6px; background-position: -10px -150px; }
.block_error.right { position: fixed; top: auto; left: auto; margin-top: -27px; margin-left: 50px; }
.block_error.right.on_panel { position: absolute; top: -9px; left: 100%; margin-top: 0; margin-left: 7px; }
.block_error.right:before { left: -6px; background-position: 0 -150px; }
.block_error.top { top: -40px; left: 50%; -moz-transform: translate(-50%, 0); -ms-transform: translate(-50%, 0); -webkit-transform: translate(-50%, 0); transform: translate(-50%, 0); margin-top: 0 !important; }
.block_error.top:before { left: 50%; margin-left: -6px; width: 12px; top: auto; bottom: -6px; background-position: -20px -150px; }

.transparent_updates, .transparent_colors, .transparent_tags, .transparent_sort { position: fixed; display: none; top: 0; right: 0; left: 0; bottom: 0; z-index: 1800; }

.transparent_updates { z-index: 2500; top: 59px; }

.top_trsprnt_scroll, .bottom_trsprnt_scroll { position: fixed; left: 0; right: 0; z-index: 3000; }

.top_trsprnt_scroll { height: 118px; top: 0; }

.bottom_trsprnt_scroll { bottom: 0; height: 40px; }

.macosx_10_9 .wrapper_strt_dwlnd .start_add_download { height: -webkit-calc(100% - 2px); height: calc(100% - 2px); }

/*main styles: end*/
.mac_cef #react-toolbar-container { background: linear-gradient(#eaeaea, #d3d3d3); }

.mac_cef .header-nav .header-search input { background-color: #fff; border: 1px solid #ccc; -moz-border-radius: 4px; -webkit-border-radius: 4px; border-radius: 4px; font-size: 13px; height: 22px; }

.macosx .popup .header, .macosx .delete .header, .macosx .add_url .header, .macosx .delete .header, .macosx .scheduler .header, .macosx .change_url_wrapper .header, .macosx .popup_exist .add_url .header, .macosx .simple .simple_dialog .header { color: #444; font-size: 13px; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2VjZWNlYyIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2RkZGNkYyIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #ececec), color-stop(100%, #dddcdc)); background-image: -moz-linear-gradient(top, #ececec 0%, #dddcdc 100%); background-image: -webkit-linear-gradient(top, #ececec 0%, #dddcdc 100%); background-image: linear-gradient(to bottom, #ececec 0%, #dddcdc 100%); }
.macosx .popup .header .close_button, .macosx .delete .header .close_button, .macosx .add_url .header .close_button, .macosx .delete .header .close_button, .macosx .scheduler .header .close_button, .macosx .change_url_wrapper .header .close_button, .macosx .popup_exist .add_url .header .close_button, .macosx .simple .simple_dialog .header .close_button { background-position: 7px -365px; }

/*header: start*/
.header-nav { height: 60px; width: 100%; padding: 10px 10px 0; border-bottom: 1px solid #b8b8b8; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.header-nav.opacity { opacity: .3; }
.header-nav.opacity .disable_block { display: block; }
.header-nav .disable_block { display: none; position: absolute; top: 0; left: 0; right: 0; height: 59px; z-index: 1800; }
.header-nav .add-download, .header-nav .start-download, .header-nav .pause-download, .header-nav .cancel-download, .header-nav .action_folder { width: 38px; height: 38px; cursor: pointer; float: left; margin-right: 20px; }
.header-nav .add-download.disable, .header-nav .start-download.disable, .header-nav .pause-download.disable, .header-nav .cancel-download.disable, .header-nav .action_folder.disable { opacity: 0.3; cursor: default; }
.header-nav .add-download { background-color: #16a4fa; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; background-position: 10px 10px; }
.header-nav .start-download { background-position: 13px -30px; }
.header-nav .start-download.selected { background-position: -27px -30px; }
.header-nav .pause-download { background-position: 13px -70px; }
.header-nav .pause-download.selected { background-position: -27px -70px; }
.header-nav .cancel-download { background-position: 10px -110px; }
.header-nav .cancel-download.selected { background-position: -30px -110px; }
.header-nav .action_folder { background-position: 5px -150px; }
.header-nav .action_folder.selected { background-position: -35px -150px; }
.header-nav .header-search { float: right; margin-right: 50px; padding-top: 5px; }
.header-nav .header-search input { width: 200px; height: 26px; font-size: 16px; padding: 0 10px 0 25px; margin-top: 2px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -webkit-user-select: auto; background-position: 4px -200px; }
.header-nav .header-search input::-webkit-input-placeholder { color: #acacac; }
.header-nav .header-search input:focus { outline: auto; }
.header-nav .header-search input::-webkit-search-results-decoration { display: none; }
.header-nav .button_menu { position: absolute; right: 10px; top: 17px; }
.header-nav .button_menu:before { width: 18px; height: 12px; background-position: 0 -247px; top: 4px; margin-left: -9px; }
.header-nav .button_menu.notification:after { content: ''; display: block; width: 11px; height: 11px; position: absolute; top: 2px; right: 4px; background-color: #40ca0a; border: 1px solid #eaeaea; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.header-nav .pushed { border-color: #b1b1b1; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2UzZTRlNSIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2UwZTFlMiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #e3e4e5), color-stop(100%, #e0e1e2)); background-image: -moz-linear-gradient(#e3e4e5, #e0e1e2); background-image: -webkit-linear-gradient(#e3e4e5, #e0e1e2); background-image: linear-gradient(#e3e4e5, #e0e1e2); -moz-box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); -webkit-box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); }
.header-nav .update_button { float: right; margin-right: 10px; margin-top: 7px; position: relative; }
.header-nav .update_button .update_icon { position: absolute; left: 50%; right: 0; top: 50%; bottom: 0; margin-top: -8px; margin-left: -8px; width: 16px; height: 16px; background-position: -42px -212px; background-size: 97px 228px; }
.header-nav .update_button .update_icon.waiting { -moz-animation: spinner 1.8s linear infinite; -webkit-animation: spinner 1.8s linear infinite; animation: spinner 1.8s linear infinite; }

.errors_msg { white-space: nowrap; text-overflow: ellipsis; overflow: hidden; color: #bc3737; padding-top: 5px; }

/*header: end*/
/*content: start*/
.content { background-color: #fff; position: absolute; top: 60px; bottom: 31px; width: 100%; display: -webkit-flex; display: flex; -webkit-flex-direction: column; flex-direction: column; }
.content .main-column { overflow: hidden; -webkit-flex: 2; flex: 2; position: relative; }
.content .main-column.innactive .row.current:before { border-color: #b8b8b8; }
.content .main-column.innactive .row.selected, .content .main-column.innactive .row.drop_is_active { background-color: #f2f2f2; }
.content .main-column.innactive .row .checkbox-fake { background-position: -160px -44px; }
.content .main-column.innactive .row.selected .checkbox-fake, .content .main-column.innactive .row.drop_is_active.selected .checkbox-fake { background-position: -180px -44px; }
.content .filter-no-results { position: absolute; left: 9px; right: 9px; width: auto; top: 1px; bottom: 9px; height: auto; background-color: #fff; z-index: 200; }
.content .filter-no-results .notification { display: block; position: absolute; top: 50%; left: 50%; margin-top: -43px; height: 85px; width: 90%; margin-left: -45%; text-align: center; color: #333c4e; padding: 0 20px; font: 24px "Roboto Condensed Light", "Helvetica Neue", Arial, sans-serif; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.content .filter-no-results .notification .user_text { word-wrap: break-word; -webkit-line-clamp: 2; -webkit-box-orient: vertical; max-height: 60px; overflow: hidden; }
.content .filter-no-results .notification a { color: #16a4fa; font: 14px -webkit-system-font, "Helvetica Neue", Arial, sans-serif; display: table; margin: 15px auto 0; cursor: pointer !important; }

.bottom-panel-opener, .close-icon { width: 21px; height: 21px; position: absolute; top: 6px; cursor: pointer; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; }

.bottom-panel-opener { background-position: -35px -19px; right: 9px; }

.close-icon { background-position: 5px -366px; right: 2px; top: 1px; }

input[type=checkbox] { display: none; }

input[type=checkbox] + label { position: relative; padding-left: 25px; }

input[type=checkbox]:disabled + label, input[type=checkbox]:disabled + label .absolute, input[type=checkbox]:disabled + label .absolute input[type=text] { color: rgba(58, 57, 59, 0.5); }

input[type=checkbox] + label:before { background-position: 0 -20px; top: 2px; }

.add_url.tutorial input[type=checkbox] + label:before { background-position: 0 -44px; top: 2px; }
.add_url.tutorial input[type=checkbox]:checked + label:before { background-position: -40px -44px; }
.add_url.tutorial input[type=checkbox]:disabled + label:before { background-position: -60px -44px; }
.add_url.tutorial input[type=checkbox]:disabled:checked + label:before { background-position: -80px -44px; }

input[type=checkbox]:checked + label:before { background-position: 0 0; }

input[type=checkbox]:disabled + label:before { background-position: -40px -20px; }

input[type=checkbox]:disabled:checked + label:before { background-position: -40px 0; }

input[type=text]:disabled { border-color: #e3e3e3; color: #757575; background-color: #fff; }

/*content: end*/
/*drag&drop: start*/
.wrapper_strt_dwlnd { position: fixed; top: 94px; bottom: 33px; left: 0; right: 0; z-index: 2000; }
.wrapper_strt_dwlnd .start_add_download { position: absolute; top: 0; bottom: 0; left: 8px; right: 10px; background-color: #fff; border: 2px dashed #17a2f7; border-image-source: url("../v2_images/border.png"); border-image-slice: 3; border-image-repeat: round; display: table; width: -webkit-calc(100% - 16px); width: calc(100% - 16px); height: -webkit-calc(100% - 6px); height: calc(100% - 6px); table-layout: fixed; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.wrapper_strt_dwlnd .wrapper { font: 26px "Roboto Condensed Light", "Helvetica Neue", Arial, sans-serif; color: rgba(22, 164, 250, 0.7); text-align: center; display: table-cell; vertical-align: middle; }
.wrapper_strt_dwlnd .wrapper .big_title { color: #16a4fa; font-size: 44px; }

/*drag&drop: end*/
/*tags panel: start*/
#react-tag-panel-container { height: 34px; }

.header-tags { width: 100%; height: 24px; padding: 8px 8px 0; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.header-tags .tag_element, .header-tags .tag_element span { cursor: pointer; }
.header-tags .wrap_manage, .header-tags .wrap_new_tag { float: left; position: relative; z-index: 2500; }
.header-tags .wrap_manage { float: none; }
.header-tags .manage_btn, .header-tags .show_more { background-color: #ebebeb; width: 18px; height: 18px; min-width: 0; }
.header-tags .manage_btn { background-position: 4px 4px; margin-bottom: 10px !important; }
.header-tags .new_tag { -webkit-appearance: none; border: none; width: 40px; font-size: 11px; line-height: 15px; padding-top: 4px; }
.header-tags .show_more { position: relative; background-position: -56px 8px; overflow: visible; }
.header-tags .wrapper_tags { background-color: #fff; padding: 12px; position: absolute; top: 29px; left: -170px; z-index: 2100; font-size: 13px; width: auto; min-width: 120px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 6px; -webkit-border-radius: 6px; border-radius: 6px; -moz-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); -webkit-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); }
.header-tags .wrapper_tags.empty_list .wrap_manage { display: none; }
.header-tags .wrapper_tags.empty_list .block_error.left { margin-top: 0; }
.header-tags .wrapper_tags .wrap_manage { max-height: 114px; overflow-y: auto; overflow-x: hidden; margin-bottom: 10px; text-align: left; padding-right: 20px; }
.header-tags .wrapper_tags .wrap_manage .wrap_new_tag { border: none; margin: 0 0 8px; padding: 0; }
.header-tags .wrapper_tags .wrap_manage .wrap_new_tag .block_error { position: fixed; top: auto; right: auto; margin-left: -274px; margin-top: -24px; }
.header-tags .wrapper_tags .wrap { margin-bottom: 8px; }
.header-tags .wrapper_tags .wrap:last-child { margin-bottom: 0; }
.header-tags .wrapper_tags .wrap_new_tag { float: none; border-top: 1px solid #e5e5e5; padding: 10px 12px 0; margin: 0 -10px 0; position: relative; }
.header-tags .wrapper_tags .wrap_new_tag.empty_list { margin-bottom: 0; border-top: 0; padding: 0 12px; }
.header-tags .wrapper_tags .wrap_new_tag.empty_list .choose_color { top: 5px; }
.header-tags .wrapper_tags .tag_element { display: inline-block; margin: 0 0 12px; float: none; padding-bottom: 0; }
.header-tags .wrapper_tags .tag_element:last-child { margin-bottom: 0; }
.header-tags .wrapper_tags .tag_element.new_tag { display: inline-block; }
.header-tags .wrapper_tags .name_fortag { height: 20px; margin-left: 18px; font-size: 13px; padding: 0 5px; border: 1px solid #e5e5e5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }
.header-tags .wrapper_tags .choose_color { width: 12px; height: 12px; display: block; position: absolute; top: 14px; left: 10px; background-position: 3px -314px; }
.header-tags .wrap_new_tag .tag_name { display: inline-block; }
.header-tags .wrap_new_tag .new_tag { height: 15px; padding: 2px 0; margin: 0; min-width: 0; }
.header-tags .wrap_new_tag .tag_element { overflow: visible; }
.header-tags .triangle { display: block; width: 11px; height: 6px; background-position: 0 -216px; position: absolute; top: 23px; left: 5px; z-index: 2500; }

.block-edit-tags { position: absolute; border: 1px solid #b9b9b9; background-color: #fff; top: 24px; left: 0; width: 128px; padding: 6px 0; text-align: center; z-index: 2100; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-box-shadow: rgba(137, 137, 137, 0.43) 2px 2px 6px; -webkit-box-shadow: rgba(137, 137, 137, 0.43) 2px 2px 6px; box-shadow: rgba(137, 137, 137, 0.43) 2px 2px 6px; }
.block-edit-tags .main-palette { padding: 0 12px; }
.block-edit-tags a { font-size: 13px; line-height: 22px; color: #585759; cursor: pointer; display: block; float: none; clear: both; width: 100%; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.block-edit-tags a:hover { background-color: #e6f7fe; }
.block-edit-tags hr { height: 1px; margin: 5px 0; border: none; background-color: #e6e6e6; }
.block-edit-tags span { display: block; float: left; width: 12px; height: 12px; margin-right: 6px; margin-bottom: 6px; cursor: pointer; }
.block-edit-tags span:nth-child(6n) { margin-right: 0; }
.block-edit-tags span.selected_color { float: none; margin-left: 12px; }

.trnsprnt_for_tags { position: absolute; top: -60px; bottom: 0; left: 0; right: 0; z-index: 2100; }

.wrapper_inselect.top { max-width: 125px; float: right; font-size: 12px; padding-left: 18px; padding-top: 4px; }
.wrapper_inselect.top.selected { border-color: #8bd0fb; }
.wrapper_inselect.top .list { z-index: 2100; padding: 5px 0; }
.wrapper_inselect.top .list div { padding-left: 18px; position: relative; }
.wrapper_inselect.top .list div.active:before { content: ''; display: block; position: absolute; left: 5px; top: 5px; width: 10px; height: 8px; background-position: 0 -294px; }
.wrapper_inselect.top .count { float: right; margin-right: 5px; font-size: 11px; line-height: 16px; color: #aaa; }

/*tags panel: end*/
/*block default client: start*/
.wrap_default_client { position: absolute; top: 60px; left: 0; right: 0; height: 30px; padding: 7px 44px 0 10px; font-size: 13px; background-color: #d7e8ed; z-index: 5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.wrap_default_client.about_sleep { padding-right: 10px; background-color: #006ea0; color: #fff; }
.wrap_default_client a { display: block; float: right; color: #333c4e; padding: 3px 10px; background-color: #fff; margin-left: 10px; height: 22px; margin-top: -3px; cursor: pointer !important; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.wrap_default_client .cancel_btn { display: block; float: left; width: 30px; height: 30px; background-position: 11px -224px; position: absolute; top: 0; right: 6px; cursor: pointer !important; }

/*block default client: end*/
/*downloads list: start*/
.compact-view-info { border: 1px solid #e5e5e5; height: 24px; margin: 0 8px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.compact-view-info:after { content: ''; display: block; position: absolute; background-color: #fafbfd; border-bottom: 1px solid #e5e5e5; width: 20px; height: 22px; right: 11px; top: 1px; }
.compact-view-info ul { width: 100%; overflow-y: scroll; }
.compact-view-info ul li { display: block; position: relative; width: 200px; float: left; font-size: 13px; line-height: 23px; color: #000; padding: 0 6px; cursor: default; border-right: 1px solid #e5e5e5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.compact-view-info ul li.compact-view-checkbox-wrap { width: 93px; height: 23px; padding-left: 0; }
.compact-view-info ul li.compact-view-name { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.compact-view-info ul li.compact-view-progress { width: 150px; }
.compact-view-info ul li.compact-view-speed { width: 150px; }
.compact-view-info ul li.compact-view-size { width: 108px; }
.compact-view-info ul li.compact-view-date { width: 99px; padding-right: 0; }
.compact-view-info ul li .resizer { display: none; }
.compact-view-info ul li input[type=checkbox] { display: none; }
.compact-view-info ul li input[type=checkbox] + label { position: relative; width: 40px; height: 23px; display: block; margin-top: -2px; padding-left: 0; }
.compact-view-info ul li input[type=checkbox] + label:before { left: 12px; top: 7px; background-position: -100px -44px; }
.compact-view-info ul li input[type=checkbox]:checked + label:before { background-position: -120px -44px; }
.compact-view-info ul li input[type=checkbox].indeterminate + label:before { background-position: -140px -44px; }
.compact-view-info ul li span { display: block; float: left; max-width: 76%; }

.download-list { overflow-y: scroll; position: absolute; top: 24px; bottom: 8px; left: 8px; right: 8px; border: 1px solid #d7e8ed; border-top: none; }
.download-list.has_scroll .row:last-child { border-bottom: none; }
.download-list .row { display: block; position: relative; height: 40px; width: 100%; border-top: 1px solid #d7e8ed; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.download-list .row:first-child { border-top: none; }
.download-list .row:last-child { height: 42px; border-bottom: 1px solid #d7e8ed; }
.download-list .row:last-child:before { height: 42px !important; }
.download-list .row > div { height: 100%; float: left; padding: 11px 6px 0; position: relative; z-index: 10; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.download-list .row .priority_buttons { display: none; }
.download-list .row.completed:not(.error) .compact-progress-wrap { display: none; }
.download-list .row.completed:not(.error) .compact-download-title { width: -webkit-calc(100% - 450px); width: calc(100% - 450px); }
.download-list .row.completed:not(.error) .download-state { background-position: 0 7px; }
.download-list .row.completed:not(.error).without_upload .compact-download-speed { display: none; }
.download-list .row.completed:not(.error).without_upload .compact-download-title { width: -webkit-calc(100% - 300px); width: calc(100% - 300px); }
.download-list .row.completed:not(.error).without_upload.partially .compact-progress-wrap { width: 300px; }
.download-list .row.completed:not(.error).without_upload.partially .compact-download-speed { display: none; }
.download-list .row.completed:not(.error).partially .compact-download-speed { display: block; }
.download-list .row.completed:not(.error).partially .compact-progress-wrap { display: block; width: 150px; padding-right: 0; }
.download-list .row.completed:not(.error).partially .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.completed:not(.error).partially .no-size { font-size: 13px; text-align: left; padding-top: 3px; }
.download-list .row.completed:not(.error).partially .no-size span { text-overflow: ellipsis; overflow: hidden; white-space: nowrap; }
.download-list .row.enable_seeding .compact-download-speed:hover .cancel { display: block; }
.download-list .row.enable_seeding .compact-download-speed:hover .arrow_up, .download-list .row.enable_seeding .compact-download-speed:hover .txt { display: none; }
.download-list .row.action_btn_disabled .download-state { opacity: .3; }
.download-list .row.is_moving .no-size { font-size: 13px; text-align: left; padding-top: 6px; }
.download-list .row.is_moving .download-state { opacity: .3; }
.download-list .row.is_moving .compact-progress-wrap { padding-top: 0; }
.download-list .row.completed.is_moving .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.completed.is_moving .compact-progress-wrap { display: block; }
.download-list .row.completed.is_moving.without_upload .compact-download-speed { display: block; }
.download-list .row.completed.move_progress .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.completed.move_progress .compact-progress-wrap { display: block; }
.download-list .row.completed.move_progress.without_upload .compact-download-speed { display: block; }
.download-list .row.error.is_moving .compact-progress-wrap { display: block; }
.download-list .row.error.is_moving .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.error.is_moving .compact-download-speed { display: none; }
.download-list .row.error.is_moving .compact-progress-wrap { padding-top: 6px; }
.download-list .row.error.move_progress .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.error.move_progress .compact-progress-wrap { display: block; }
.download-list .row.error.move_progress.without_upload .compact-download-speed { display: block; }
.download-list .row.download-ending .compact-progress-line, .download-list .row.download-ending .percents, .download-list .row.download-ending .compact-download-time, .download-list .row.download-ending .time, .download-list .row.download-ending .info-time { display: none; }
.download-list .row.download-ending .download-complete { display: inline-block; padding-top: 6px; font-size: 13px; padding-left: 12px; background-position: 0 -283px; }
.download-list .row.download-ending .download-state { background-position: 0 7px; }
.download-list .row.download-ending.partially .no-size { font-size: 13px; text-align: left; padding-top: 7px; white-space: nowrap; }
.download-list .row.error:not(.move_progress) .compact-download-speed, .download-list .row.error:not(.move_progress) .info-time, .download-list .row.error:not(.move_progress) .compact-progress-line { display: none; }
.download-list .row.error:not(.move_progress) .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row.error:not(.move_progress) .compact-progress-wrap { width: 300px; }
.download-list .row.error .download-state { background-position: 0 -114px; }
.download-list .row.paused .download-state { background-position: 2px -34px; }
.download-list .row.paused .compact-progress-line { background-color: #e5e5e5; }
.download-list .row.paused .compact-download-progress { background-color: #acacac; }
.download-list .row.is_queued .compact-progress-line { background-color: #e5e5e5; }
.download-list .row.is_queued .compact-download-progress { background-color: #acacac; }
.download-list .row.move_progress .compact-progress-line { background-color: #d5e2e8; }
.download-list .row.move_progress .compact-download-progress { background-color: #17a2f7; }
.download-list .row.downloading .download-state { background-position: 1px -74px; }
.download-list .row.downloading.without_upload .compact-download-speed { display: none; }
.download-list .row.downloading.without_upload .compact-progress-wrap { width: 300px; }
.download-list .row.downloading .error_wrap { padding-top: 6px; }
.download-list .row:first-child.current:before { top: 0; }
.download-list .row.current:before { content: ''; display: block; width: 100%; height: 41px; position: absolute; top: -1px; left: 0; border: 1px solid #8bd0fb; z-index: 5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.download-list .row.selected, .download-list .row.drop_is_active { background-color: #e6f7fe; }
.download-list .row.selected.current:before, .download-list .row.drop_is_active.current:before { display: block; }
.download-list .row.selected:before, .download-list .row.drop_is_active:before { display: none; }
.download-list .row.selected .checkbox-fake, .download-list .row.drop_is_active .checkbox-fake { background-position: -40px -44px; }
.download-list .row.drop_is_active.selected .checkbox-fake { background-position: -40px -44px; }
.download-list .row.drop_is_active .checkbox-fake { background-position: 0 -44px; }
.download-list .row.timer_state .download-state { background-position: 0 -157px; }
.download-list .row .high_priority, .download-list .row .low_priority { position: absolute; top: 0; left: 0; bottom: 0; width: 4px; display: block; float: none; padding: 0; }
.download-list .row .high_priority { background-color: #7ed350; }
.download-list .row .low_priority { background-color: #d54744; }
.download-list .row > div.compact-checkbox-wrap { width: 24px; padding: 0; margin-left: 6px; }
.download-list .row > div.compact-download-state { width: 29px; padding: 0; position: relative; cursor: pointer; }
.download-list .row > div.compact-preview-img { width: 32px; padding: 4px 0; text-align: center; margin-right: 2px; }
.download-list .row > div.compact-preview-img img { width: 100%; height: auto; }
.download-list .compact-download-title { width: -webkit-calc(100% - 600px); width: calc(100% - 600px); }
.download-list .row > div.compact-progress-wrap { width: 150px; padding-top: 6px; }
.download-list .compact-download-speed { width: 150px; white-space: nowrap; }
.download-list .compact-download-size { width: 108px; }
.download-list .compact-download-date { width: 72px; }
.download-list .row > div.compact-download-size, .download-list .row > div.compact-download-date, .download-list .row > div.compact-download-speed { font-size: 13px; padding-top: 12px; }
.download-list .download-complete { display: none; }
.download-list .download-title { display: block; float: left; overflow: hidden; text-overflow: ellipsis; line-height: 16px; max-width: -webkit-calc(100% - 56px); max-width: calc(100% - 56px); }
.download-list .tags { float: left; margin-top: 5px; padding-left: 12px; max-width: 56px; overflow: hidden; height: 8px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.download-list .tags span { width: 8px; height: 8px; display: block; float: left; margin-right: 3px; }
.download-list .tags span.show_tags { display: none; cursor: pointer; background-position: 1px -448px; }
.download-list .error_wrap { white-space: nowrap; text-overflow: ellipsis; overflow: hidden; color: #bc3737; padding-top: 5px; }
.download-list .error-message { padding-left: 20px; color: #bc3737; font-size: 13px; display: inline; margin-top: 6px; background-position: -40px -268px; }
.download-list .error-message.left { margin-top: 0; }
.download-list .arrow_dwn, .download-list .arrow_up { position: relative; top: 1px; width: 8px; height: 11px; display: inline-block; }
.download-list .arrow_dwn { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6.52 9.65"><defs><style>.a{fill:#6b6b6b;}</style></defs><title>Vector Smart Object8</title><path class="a" d="M6.32,4.76a0.5,0.5,0,0,0-.7.1L3.8,7.33V0.5a0.5,0.5,0,0,0-1,0V7.33L0.9,4.86a0.5,0.5,0,0,0-.79.61L3.32,9.65l3.1-4.19A0.5,0.5,0,0,0,6.32,4.76Z" transform="translate(0)"/></svg>') no-repeat; }
.download-list .arrow_up { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6.52 9.65"><defs><style>.a{fill:#6b6b6b;}</style></defs><title>Vector Smart Object8</title><path class="a" d="M0.2,4.89a0.5,0.5,0,0,0,.7-0.1L2.72,2.32V9.15a0.5,0.5,0,0,0,1,0V2.32l1.9,2.47a0.5,0.5,0,0,0,.79-0.61L3.2,0,0.1,4.19A0.5,0.5,0,0,0,.2,4.89Z"/></svg>') no-repeat; }
.download-list .cancel { display: none; position: absolute; top: 0; left: 0; width: 100%; height: 100%; z-index: 5; font-size: 11px; padding-left: 25px; padding-top: 14px; color: #58595b; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.download-list .cancel .wrapper_cancel { padding-left: 17px; margin-left: -17px; cursor: pointer !important; }
.download-list .cancel .wrapper_cancel span { cursor: pointer !important; }
.download-list .cancel .cancel_button { display: block; width: 15px; height: 15px; position: absolute; top: 13px; left: 8px; background-color: #e4e5e7; cursor: pointer !important; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; }
.download-list .cancel .cancel_button:before { content: ''; display: block; width: 7px; height: 7px; margin-top: 4px; margin-left: 4px; background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 7 7"><defs><style>.a{fill:#414648;}</style></defs><polygon class="a" points="7 0.73 6.27 0 3.5 2.77 0.73 0 0 0.73 2.77 3.5 0 6.27 0.73 7 3.5 4.23 6.27 7 7 6.27 4.23 3.5 7 0.73"/></svg>') no-repeat; }

.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .download-speed, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .download-speed, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .download-speed, .download-list .row:not(.move_progress).paused .tree_node.file:hover .download-speed, .bottom-row .compact-download-speed:hover .download-speed, .bottom-row .tree_node.file:hover .download-speed { display: none; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons, .bottom-row .compact-download-speed:hover .priority_buttons, .bottom-row .tree_node.file:hover .priority_buttons { display: block; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_up, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_up, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_up, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_up, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_up, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_up { width: 16px; height: 16px; background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2216%22%20height%3D%2216%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23525354%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%2210%2011%208%208%206%2011%204%2011%208%205%2012%2011%2010%2011%22/%3E%3C/svg%3E") no-repeat; cursor: pointer; margin-right: 4px; display: inline-block; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_up:hover, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_up:hover, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_up:hover, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_up:hover, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_up:hover, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_up:hover { background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%2316a4fa%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%2210%2011%208%208%206%2011%204%2011%208%205%2012%2011%2010%2011%22/%3E%3C/svg%3E") no-repeat; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_up.disabled, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_up.disabled, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_up.disabled, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_up.disabled, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_up.disabled, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_up.disabled { opacity: .3; cursor: default; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_up.disabled:hover, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_up.disabled:hover, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_up.disabled:hover, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_up.disabled:hover, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_up.disabled:hover, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_up.disabled:hover { background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20width%3D%2216%22%20height%3D%2216%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23525354%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%2210%2011%208%208%206%2011%204%2011%208%205%2012%2011%2010%2011%22/%3E%3C/svg%3E") no-repeat; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_down, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_down, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_down, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_down, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_down, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_down { width: 16px; height: 16px; background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23525354%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%226%205%208%208%2010%205%2012%205%208%2011%204%205%206%205%22/%3E%3C/svg%3E") no-repeat; cursor: pointer; display: inline-block; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_down:hover, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_down:hover, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_down:hover, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_down:hover, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_down:hover, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_down:hover { background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%2316a4fa%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%226%205%208%208%2010%205%2012%205%208%2011%204%205%206%205%22/%3E%3C/svg%3E") no-repeat; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_down.disabled, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_down.disabled, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_down.disabled, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_down.disabled, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_down.disabled, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_down.disabled { opacity: .3; cursor: default; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_down.disabled:hover, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_down.disabled:hover, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_down.disabled:hover, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_down.disabled:hover, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_down.disabled:hover, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_down.disabled:hover { background: url("data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A//www.w3.org/2000/svg%22%20viewBox%3D%220%200%2016%2016%22%3E%3Cdefs%3E%3Cstyle%3E.cls-1%7Bfill%3A%23525354%3B%7D%3C/style%3E%3C/defs%3E%3Cpath%20class%3D%22cls-1%22%20d%3D%22M13%2C1a2%2C2%2C0%2C0%2C1%2C2%2C2V13a2%2C2%2C0%2C0%2C1-2%2C2H3a2%2C2%2C0%2C0%2C1-2-2V3A2%2C2%2C0%2C0%2C1%2C3%2C1H13m0-1H3A3%2C3%2C0%2C0%2C0%2C0%2C3V13a3%2C3%2C0%2C0%2C0%2C3%2C3H13a3%2C3%2C0%2C0%2C0%2C3-3V3a3%2C3%2C0%2C0%2C0-3-3Z%22%20transform%3D%22translate%280%29%22/%3E%3Cpolygon%20class%3D%22cls-1%22%20points%3D%226%205%208%208%2010%205%2012%205%208%2011%204%205%206%205%22/%3E%3C/svg%3E") no-repeat; }
.download-list .row:not(.move_progress).downloading .compact-download-speed:hover .priority_buttons .priority_button_text, .download-list .row:not(.move_progress).downloading .tree_node.file:hover .priority_buttons .priority_button_text, .download-list .row:not(.move_progress).paused .compact-download-speed:hover .priority_buttons .priority_button_text, .download-list .row:not(.move_progress).paused .tree_node.file:hover .priority_buttons .priority_button_text, .bottom-row .compact-download-speed:hover .priority_buttons .priority_button_text, .bottom-row .tree_node.file:hover .priority_buttons .priority_button_text { display: inline-block; margin-left: 6px; position: absolute; line-height: 16px; color: #58595b; font-size: 11px; }

.bottom-row .tree_node.file .priority_buttons { margin-top: -16px; margin-left: 88px; display: none; }

.macosx .bottom-row .tree_node.file .priority_buttons { margin-left: 92px; }

.bottom-row .tree_node.file:hover .priority_buttons .priority_button_text { display: none; }

.info-time { overflow: hidden; width: 100%; margin-bottom: 3px; }
.info-time .percents { font-size: 13px; float: left; }
.info-time .download-time { float: right; font-size: 13px; color: rgba(0, 0, 0, 0.7); }

.no-size { font-size: 12px; padding-top: 2px; }
.no-size span { color: #727a77; display: block; margin: 0 0 4px; }
.no-size .left.moving { float: left; margin-right: 10px; }
.no-size div { width: 100%; height: 5px; -moz-background-size: 11px; -o-background-size: 11px; -webkit-background-size: 11px; background-size: 11px; }

.compact-progress-line { width: 100%; height: 6px; overflow: hidden; background-color: #d5e2e8; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; }

.compact-download-progress { height: 6px; background-color: #17a2f7; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; }

.download-state { display: block; position: absolute; top: 5px; left: 6px; width: 17px; height: 28px; cursor: pointer; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }

.checkbox-fake { left: 6px; top: 13px; background-position: 0 -44px; }

/*downloads list: end*/
/*bottom panel: start*/
.bottom-row-shown .bottom-row { display: block; position: relative; z-index: 210; }

.bottom-row-shown .bottom-panel-opener { background-position: 5px -19px; }

.bottom-row { display: none; width: auto; margin: 0 8px 8px; border: 1px solid #d7e8ed; }
.bottom-row .tab-content { position: relative; }
.bottom-row .tab-content.error .process { color: #8f272b; }
.bottom-row .tab-content.error .process.moving { color: rgba(0, 0, 0, 0.7); font-size: 13px; }

.bottom-row-header { position: relative; background: #eee; height: 25px; text-align: center; border-bottom: 1px solid #d7e8ed; }
.bottom-row-header ul li { display: inline-block; font-size: 13px; padding: 5px 7px 5px; margin-right: 15px; cursor: pointer; -moz-border-radius: 4px; -webkit-border-radius: 4px; border-radius: 4px; }
.bottom-row-header ul li:last-child { margin-right: 0; }
.bottom-row-header ul li.tab-active { position: relative; }
.bottom-row-header ul li.tab-active:before { content: ''; display: block; position: absolute; left: 0; right: 0; bottom: -1px; height: 2px; background-color: #16a4fa; }

.bottom-row-header ul li.tab-active::before { bottom: 0; }

.resize-block { height: 10px; width: 100%; position: absolute; top: -11px; left: 0; right: 0; cursor: ns-resize !important; }

.tab-general { display: block; height: -webkit-calc(100% - 26px); height: calc(100% - 26px); overflow-y: auto; padding: 10px 10px 0; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-general.completed .wrap_completed_info, .tab-general.download-ending .wrap_completed_info { display: block; overflow: hidden; position: relative; top: 2px; margin-bottom: 8px; padding-bottom: 2px; }
.tab-general.completed .wrap_completed_info span, .tab-general.download-ending .wrap_completed_info span { display: block; float: left; }
.tab-general.completed .wrap_completed_info span.completed, .tab-general.download-ending .wrap_completed_info span.completed { font-size: 14px; }
.tab-general.completed .wrap_completed_info .count, .tab-general.download-ending .wrap_completed_info .count { width: 33.3%; max-width: 210px; }
.tab-general.completed .wrap_completed_info .seeding, .tab-general.download-ending .wrap_completed_info .seeding { margin-left: 240px; position: absolute; }
.tab-general.completed .wrap_completed_info input[type=checkbox] + label, .tab-general.download-ending .wrap_completed_info input[type=checkbox] + label { padding-left: 18px; }
.tab-general.completed .wrap_completed_info input[type=checkbox] + label:before, .tab-general.download-ending .wrap_completed_info input[type=checkbox] + label:before { left: 0; top: 2px; width: 12px; height: 12px; background-position: 0 -44px; }
.tab-general.completed .wrap_completed_info input[type=checkbox]:checked + label:before, .tab-general.download-ending .wrap_completed_info input[type=checkbox]:checked + label:before { background-position: -40px -44px; }
.tab-general .info_path, .tab-general .info_comment { font-size: 13px; }
.tab-general .info_comment { color: #737373; }
.tab-general .info_comment .cmnt { color: #000; cursor: text !important; -webkit-user-select: auto; }
.tab-general .info_comment .cmnt.value a { color: #18a3f7; text-decoration: underline; cursor: pointer !important; }
.tab-general .info_path { overflow: hidden; white-space: nowrap; text-overflow: ellipsis; display: block; position: relative; padding-left: 20px; margin-bottom: 5px; }
.tab-general .info_path .value.path { -webkit-user-select: auto; cursor: text !important; }
.tab-general .folder_icon { width: 18px; height: 18px; position: absolute; left: 0; top: 0; background-position: 0 -329px; cursor: pointer; }
.tab-general .wrapper_table { overflow: hidden; margin-bottom: 6px; padding-bottom: 2px; }
.tab-general .wrapper_table table td { color: #000; font-size: 13px; max-width: 148px; table-layout: fixed; min-width: 125px; white-space: nowrap; padding-right: 10px; box-sizing: border-box; }
.tab-general .wrapper_table table td:nth-child(4) { min-width: 40px; padding-right: 10px; }
.tab-general .wrapper_table table .title { color: #737373; min-width: 0; padding-right: 10px; }
.tab-general .wrapper_table .value.domain { -webkit-user-select: auto; cursor: text !important; }
.tab-general .wrap_completed_info { display: none; }
.tab-general .wrap_completed { padding-top: 2px; margin-bottom: 6px; }
.tab-general .wrap_completed_error, .tab-general .wrap_completed_percents { display: block; padding-top: 2px; float: left; margin-bottom: 6px; }
.tab-general .bottom-panel-img { display: block; float: left; text-align: center; }
.tab-general .bottom-panel-img img { max-width: 100%; max-height: 100%; }
.tab-general .general-tab-wrap { max-width: 100%; padding-left: 28px; display: -webkit-flex; display: flex; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-general .general-tab-wrap .stretch-element { width: 100%; }
.tab-general .general-tab-wrap .wrap-title-dwnld { overflow: hidden; margin-bottom: 8px; }
.tab-general .general-tab-wrap .wrap-title-dwnld.without_tags { margin-bottom: 8px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-general .general-tab-wrap .download-detalization-title { display: block; float: left; max-width: 100%; margin-right: 15px; overflow: hidden; font-size: 18px; line-height: 26px; -webkit-user-select: auto; }
.tab-general .general-tab-wrap .download-detalization-title .title { padding-right: 10px; word-wrap: break-word; -webkit-user-select: auto; cursor: text !important; }
.tab-general .general-tab-wrap .wrapper-user-tags { position: relative; top: 0; }
.tab-general .general-tab-wrap .wrapper-user-tags .tag_element { margin-bottom: 3px; float: none; display: inline-block; line-height: 15px; vertical-align: middle; }
.tab-general .general-tab-wrap .wrapper-user-tags .close { width: 10px; height: 10px; background-position: -28px -233px; margin-left: 5px; position: relative; cursor: pointer; z-index: 3; }
.tab-general .general-tab-wrap .wrapper-user-tags .close.invert { background-position: -58px -233px; }
.tab-general .general-tab-wrap .wrapper-user-tags .tag_color:hover:before { display: none; }
.tab-general .general-tab-wrap .process { color: rgba(0, 0, 0, 0.7); padding-left: 10px; position: relative; top: 2px; vertical-align: top; display: table; }
.tab-general .general-tab-wrap .progress-line-wrap { display: block; position: relative; margin: 5px 10px 10px 0; float: left; height: 10px; width: 300px; overflow: hidden; background-color: #d5e2e8; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }
.tab-general .general-tab-wrap .progress-line-wrap .progress { background-color: #17a2f7; height: 10px; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }
.tab-general .general-tab-wrap .downloading_paused_line .progress-line-wrap, .tab-general .general-tab-wrap .is_queued .progress-line-wrap { background-color: #e5e5e5; }
.tab-general .general-tab-wrap .downloading_paused_line .progress, .tab-general .general-tab-wrap .is_queued .progress { background-color: #acacac; }
.tab-general .general-tab-wrap .percents { display: block; float: left; color: rgba(0, 0, 0, 0.7); padding-top: 2px; padding-right: 10px; width: 46px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-general .general-tab-wrap .error-message { padding-left: 20px; font-size: 13px; margin-left: 10px; color: #bc3737; position: relative; top: 2px; background-position: -40px -268px; display: inline-block; overflow: hidden; max-width: 90%; }
.tab-general .general-tab-wrap .error-message:first-child { margin-bottom: 7px; margin-left: 0; }
.tab-general .general-tab-wrap .error-message.spec_error { float: none; display: block; margin: 9px 0 7px; max-width: 100%; }
.tab-general .general-tab-wrap .general-info { float: none; overflow: hidden; width: 100%; }
.tab-general .general-tab-wrap .progress-wrap.unknown_size .percents, .tab-general .general-tab-wrap .progress-wrap.unknown_size .progress-line-wrap .progress { display: none; }
.tab-general .general-tab-wrap .progress-wrap.unknown_size .progress-line-wrap { -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-background-size: 22px; -o-background-size: 22px; -webkit-background-size: 22px; background-size: 22px; background: url(../v2_images/line.gif) repeat-x; }
.tab-general .general-tab-wrap .is_queued.progress-wrap .percents { display: block; }
.tab-general ul { width: 30%; max-width: 210px; float: left; padding-right: 5px; overflow: visible; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-general ul:first-child { width: 40%; max-width: 240px; white-space: nowrap; }
.tab-general ul:first-child li.nowrap { white-space: nowrap; overflow: visible; }
.tab-general ul:nth-child(1) li span { min-width: 100px; }
.tab-general ul li { height: 17px; display: block; }
.tab-general ul li span { width: 86px; display: inline-block; color: #737373; font-size: 13px; vertical-align: top; }
.tab-general ul li .value { color: #000; display: inline-block; min-width: 0; }
.tab-general ul li .value.path, .tab-general ul li .value.cmnt { -webkit-user-select: auto; cursor: text !important; }

.tab-files, .tab-trackers, .tab-peers, .tab-log { -webkit-flex-direction: column; flex-direction: column; height: -webkit-calc(100% - 26px); height: calc(100% - 26px); }
.tab-files .table-headers, .tab-trackers .table-headers, .tab-peers .table-headers, .tab-log .table-headers { width: 100%; height: 23px; border-bottom: 1px solid #e5e5e5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-files .table-headers li, .tab-trackers .table-headers li, .tab-peers .table-headers li, .tab-log .table-headers li { float: left; display: block; font-size: 13px; line-height: 23px; position: relative; padding: 0 6px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.tab-files .table-headers li:first-child, .tab-trackers .table-headers li:first-child, .tab-peers .table-headers li:first-child, .tab-log .table-headers li:first-child { padding-left: 10px; }
.tab-files .table-headers li:last-child, .tab-trackers .table-headers li:last-child, .tab-peers .table-headers li:last-child, .tab-log .table-headers li:last-child { padding-right: 10px; }
.tab-files .table-headers li .rubber-ellepsis, .tab-trackers .table-headers li .rubber-ellepsis, .tab-peers .table-headers li .rubber-ellepsis, .tab-log .table-headers li .rubber-ellepsis { display: block; }

.files-table li, .trackers-info li, .peers-info li, .log-info li { display: block; position: relative; overflow: hidden; clear: both; }
.files-table li span, .trackers-info li span, .peers-info li span, .log-info li span { padding: 6px 0 0 6px; display: block; line-height: 1.3em; font-size: 13px; }
.files-table li div, .trackers-info li div, .peers-info li div, .log-info li div { float: left; position: relative; height: 24px; }

.trackers-info, .peers-info, .log-info, .progress-info { overflow-y: scroll; -webkit-flex: 2; flex: 2; }

.progress-info { overflow-y: hidden; }

.table-headers li.file-name { width: 400px; min-width: 136px; }
.table-headers li.file-size { width: 120px; min-width: 68px; }
.table-headers li.file-progress { width: 197px; min-width: 130px; }
.table-headers li.file-priority { width: 151px; min-width: 67px; }

.tab-files > .files-table { overflow-y: scroll; -webkit-flex: 2; flex: 2; /* generate 'nesting levels' for file-tree: start */ /* generate 'nesting levels' for file-tree: end */ }
.tab-files > .files-table li .pad { padding-left: 30px; }
.tab-files > .files-table li li .pad { padding-left: 45px; }
.tab-files > .files-table li li li .pad { padding-left: 60px; }
.tab-files > .files-table li li li li .pad { padding-left: 75px; }
.tab-files > .files-table li li li li li .pad { padding-left: 90px; }
.tab-files > .files-table li li li li li li .pad { padding-left: 105px; }
.tab-files > .files-table li li li li li li li .pad { padding-left: 120px; }
.tab-files > .files-table li li li li li li li li .pad { padding-left: 135px; }
.tab-files > .files-table li li li li li li li li li .pad { padding-left: 150px; }
.tab-files > .files-table li li li li li li li li li li .pad { padding-left: 165px; }
.tab-files > .files-table li li li li li li li li li li li .pad { padding-left: 180px; }
.tab-files > .files-table li li li li li li li li li li li li .pad { padding-left: 195px; }
.tab-files > .files-table li li li li li li li li li li li li li .pad { padding-left: 210px; }
.tab-files > .files-table li li li li li li li li li li li li li li .pad { padding-left: 225px; }
.tab-files > .files-table li li li li li li li li li li li li li li li .pad { padding-left: 240px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li .pad { padding-left: 255px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li .pad { padding-left: 270px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li .pad { padding-left: 285px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li li .pad { padding-left: 300px; }
.tab-files > .files-table li .triangle { left: 3px; }
.tab-files > .files-table li li .triangle { left: 18px; }
.tab-files > .files-table li li li .triangle { left: 33px; }
.tab-files > .files-table li li li li .triangle { left: 48px; }
.tab-files > .files-table li li li li li .triangle { left: 63px; }
.tab-files > .files-table li li li li li li .triangle { left: 78px; }
.tab-files > .files-table li li li li li li li .triangle { left: 93px; }
.tab-files > .files-table li li li li li li li li .triangle { left: 108px; }
.tab-files > .files-table li li li li li li li li li .triangle { left: 123px; }
.tab-files > .files-table li li li li li li li li li li .triangle { left: 138px; }
.tab-files > .files-table li li li li li li li li li li li .triangle { left: 153px; }
.tab-files > .files-table li li li li li li li li li li li li .triangle { left: 168px; }
.tab-files > .files-table li li li li li li li li li li li li li .triangle { left: 183px; }
.tab-files > .files-table li li li li li li li li li li li li li li .triangle { left: 198px; }
.tab-files > .files-table li li li li li li li li li li li li li li li .triangle { left: 213px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li .triangle { left: 228px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li .triangle { left: 243px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li .triangle { left: 258px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li li .triangle { left: 273px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li li li .triangle { left: 288px; }
.tab-files > .files-table li li li li li li li li li li li li li li li li li li li li li .triangle { left: 303px; }

.files-table li.once_file { margin-left: -20px; }
.files-table li.folder { position: relative; }
.files-table li.folder.opened > .triangle { background-position: -48px -184px; }
.files-table li.folder > .triangle { display: block; width: 28px; height: 24px; position: absolute; top: 0; left: 0; z-index: 10; background-position: -28px -186px; }
.files-table li.folder > .file-name { width: 100% !important; }
.files-table li.file .file-name { min-width: 136px; width: 400px; }
.files-table li.file .file-size { width: 120px; min-width: 68px; }
.files-table li.file .file-progress { min-width: 130px; width: 197px; }
.files-table li.file .file-priority { width: 151px; min-width: 67px; }
.files-table li.file .file-priority span { padding-right: 10px; }
.files-table li.file .pad { border-bottom: none; }
.files-table li.tree-node-selected { background-color: #e6f7fe; }
.files-table li.current:before { content: ''; position: absolute; top: 0; left: 0; right: 0; height: 22px; border: 1px solid #8bd0fb; }
.files-table li .triangle { display: none; }
.files-table li .pad { padding-left: 30px; }
.files-table li .pad span { line-height: inherit; padding: 0; }
.files-table li .pad .error-message { padding-left: 20px; font-size: 13px; color: #bc3737; display: block; overflow: hidden; max-width: 100%; background-position: -40px -268px; }
.files-table li .percents { font-size: 12px; line-height: 10px; padding: 1px 0 0 6px; color: rgba(0, 0, 0, 0.7); }
.files-table li .progress-pre-wrap { padding: 0; }
.files-table li .progress-wrap { width: 100%; height: 6px; margin-right: 4px; float: left; margin-top: 5px; background-color: #d5e2e8; padding: 0; overflow: hidden; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; width: -webkit-calc(100% - 48px); width: calc(100% - 48px); }
.files-table li .progress-wrap .progress { height: 6px; padding: 0; background-color: #17a2f7; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; }
.files-table li .file-progress .pad { padding-left: 6px !important; }
.files-table li span { padding-top: 4px; }
.files-table li .fake_checkbox { width: 18px; height: 18px; overflow: hidden; float: left; padding: 0; margin: 0 6px 0 0; position: relative; top: -2px; background-position: 3px -40px; }
.files-table li .fake_checkbox.checked { background-position: -37px -40px; }
.files-table li .fake_checkbox.indeterminate { background-position: -17px -40px; }
.files-table li .fake_checkbox.disabled { background-position: -157px -40px; }
.files-table li .fake_checkbox.disabled.checked { background-position: -177px -40px; }
.files-table li .fake_checkbox.disabled.indeterminate { background-position: -197px -40px; }
.files-table li .yt_preview_wr { display: none; width: 196px; height: 111px; justify-content: center; align-items: center; position: fixed; z-index: 1000000000; background-color: whitesmoke; border: 2px solid #fff; margin-top: -120px; margin-left: 50px; z-index: 500; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); -webkit-box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); }
.files-table li .yt_preview_wr img { max-width: 196px; max-height: 111px; }

.tab-files .files-table li.timer_state .fake_checkbox { background: url("../v2_images/actions.svg") no-repeat 3px -130px; background-size: 14px; }

.table-headers li.tracker-name, .tracker > div.tracker-name { width: 350px; min-width: 150px; }
.table-headers li.tracker-status, .tracker > div.tracker-status { width: 235px; min-width: 85px; }
.table-headers li.tracker-update-in, .table-headers li.tracker-update, .tracker > div.tracker-update-in, .tracker > div.tracker-update { width: 145px; min-width: 117px; }

.tracker > div.tracker-name span { padding-left: 10px; }
.tracker > div.tracker-update span { padding-right: 10px; }

.table-headers li.peer-ip, .peer > div.peer-ip { width: 152px; min-width: 98px; }
.table-headers li.peer-client, .peer > div.peer-client { width: 136px; min-width: 102px; }
.table-headers li.peer-flags, .peer > div.peer-flags { width: 99px; min-width: 50px; }
.table-headers li.peer-percents, .tab-peers .table-headers li.peer-reqs, .peer > div.peer-percents, .tab-peers .peer > div.peer-reqs { width: 50px; min-width: 50px; }
.table-headers li.peer-down-speed, .table-headers li.peer-up-speed, .peer > div.peer-down-speed, .peer > div.peer-up-speed { width: 101px; min-width: 95px; }
.table-headers li.peer-downloaded, .table-headers li.peer-uploaded, .peer > div.peer-downloaded, .peer > div.peer-uploaded { width: 115px; min-width: 97px; }

.peer > div.peer-ip span { padding-left: 10px; }
.peer > div.peer-uploaded span { padding-right: 10px; }
.peer > div .download-speed, .peer > div .download-speed span { display: inline-block; }
.peer > div .download-speed { padding: 0; }
.peer > div .download-speed span { padding: 0 2px 0 0; }
.peer > div .download-speed span.arrow_up, .peer > div .download-speed span.arrow_dwn { position: relative; top: 0; width: 8px; height: 11px; display: inline-block; padding: 0; }
.peer > div .arrow_dwn { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6.52 9.65"><defs><style>.a{fill:#6b6b6b;}</style></defs><title>Vector Smart Object8</title><path class="a" d="M6.32,4.76a0.5,0.5,0,0,0-.7.1L3.8,7.33V0.5a0.5,0.5,0,0,0-1,0V7.33L0.9,4.86a0.5,0.5,0,0,0-.79.61L3.32,9.65l3.1-4.19A0.5,0.5,0,0,0,6.32,4.76Z" transform="translate(0)"/></svg>') no-repeat; }
.peer > div .arrow_up { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 6.52 9.65"><defs><style>.a{fill:#6b6b6b;}</style></defs><title>Vector Smart Object8</title><path class="a" d="M0.2,4.89a0.5,0.5,0,0,0,.7-0.1L2.72,2.32V9.15a0.5,0.5,0,0,0,1,0V2.32l1.9,2.47a0.5,0.5,0,0,0,.79-0.61L3.2,0,0.1,4.19A0.5,0.5,0,0,0,.2,4.89Z"/></svg>') no-repeat; }

.table-headers li.log-date, .log-row > div.log-date { width: 158px; min-width: 96px; }
.table-headers li.log-time, .log-row > div.log-time { width: 70px; min-width: 61px; }

.log-row > div.log-date span { padding-left: 10px; padding-right: 0; white-space: nowrap; }
.log-row > div.log-information span { padding-right: 10px; }

.log-info .log-row { display: -webkit-flex; display: flex; }
.log-info .log-row:last-child { margin-bottom: 4px; }
.log-info .log-row .log-date { float: none; }
.log-info .log-row .log-information { -webkit-flex: 2; flex: 2; float: none; height: auto; }
.log-info .log-row span { white-space: normal; -webkit-user-select: auto; cursor: text !important; }

.log_content { height: auto !important; min-height: 20px; width: 100%; }
.log_content span { display: inline-block !important; vertical-align: top; padding-bottom: 1px !important; line-height: 1.1em !important; }
.log_content span.date { width: 180px; }
.log_content span.info { padding-right: 6px; width: -webkit-calc(100% - 198px); width: calc(100% - 198px); }

/*bottom panel: end*/
/*traffic-bar: start*/
.modal-dialog-layer { position: absolute; top: 0; bottom: 0; left: 0; right: 0; z-index: 2600; }

.drag-n-drop-dialog-layer { position: absolute; top: 0; bottom: 0; left: 0; right: 0; z-index: 30000; }

.traffic-bar { height: 31px; background-color: #fff; position: fixed; bottom: 0; left: 0; right: 0; width: 100%; border-top: 1px solid #cbcbcb; z-index: 2700; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.traffic-bar.show_panel > ul:after { background-position: 4px -21px; }
.traffic-bar .snail { position: relative; float: left; }
.traffic-bar .snail .snail_text { float: left; line-height: 28px; }
.traffic-bar .snail .snail_button { float: left; margin: 3px 9px 0 8px; width: 32px; height: 23px; border: 1px solid #b8b8b8; cursor: pointer; background-size: 100%; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZiZmJmYiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2UzZTRlNCIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #fbfbfb), color-stop(100%, #e3e4e4)); background-image: -moz-linear-gradient(#fbfbfb, #e3e4e4); background-image: -webkit-linear-gradient(#fbfbfb, #e3e4e4); background-image: linear-gradient(#fbfbfb, #e3e4e4); -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.traffic-bar .snail .snail_button:before { content: ''; padding: 8px 15px; margin: 3px 4px; position: absolute; background: url("../v2_images/elements.svg") no-repeat 0 -398px; }
.traffic-bar .snail .snail_button.pushed { border-color: #b1b1b1; background-size: 100%; background-image: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2UzZTRlNSIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2UwZTFlMiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background-size: 100%; background-image: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #e3e4e5), color-stop(100%, #e0e1e2)); background-image: -moz-linear-gradient(#e3e4e5, #e0e1e2); background-image: -webkit-linear-gradient(#e3e4e5, #e0e1e2); background-image: linear-gradient(#e3e4e5, #e0e1e2); -moz-box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); -webkit-box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); box-shadow: inset 0 1px 2px 0 rgba(161, 161, 161, 0.7); }
.traffic-bar .snail .snail_button.pushed:before { background-position: 0 -423px; }
.traffic-bar .transparent_colors { height: 31px; bottom: 0; top: auto; }
.traffic-bar > ul { display: block; float: left; height: 100%; position: relative; border-right: 1px solid #cbcbcb; border-left: 1px solid #cbcbcb; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.traffic-bar > ul:after { content: ''; display: block; width: 20px; height: 20px; position: absolute; right: 6px; top: 8px; cursor: pointer; background-position: -36px -21px; }
.traffic-bar > ul .down-speed-chooser, .traffic-bar > ul .up-speed-chooser { float: left; padding-left: 14px; padding-right: 15px; height: 33px; padding-top: 6px; width: 50px; white-space: nowrap; display: block; cursor: pointer; position: relative; z-index: 10; }
.traffic-bar > ul .down-speed-chooser:before, .traffic-bar > ul .up-speed-chooser:before { content: ''; padding: 8px 5px; position: absolute; }
.traffic-bar > ul .down-speed-chooser.low:after, .traffic-bar > ul .down-speed-chooser.medium:after, .traffic-bar > ul .down-speed-chooser.high:after, .traffic-bar > ul .down-speed-chooser.manual:after, .traffic-bar > ul .up-speed-chooser.low:after, .traffic-bar > ul .up-speed-chooser.medium:after, .traffic-bar > ul .up-speed-chooser.high:after, .traffic-bar > ul .up-speed-chooser.manual:after { content: ''; display: block; height: 3px; position: absolute; left: 0; right: 0; bottom: 9px; }
.traffic-bar > ul .down-speed-chooser.low, .traffic-bar > ul .up-speed-chooser.low { background-color: rgba(213, 71, 68, 0.2); }
.traffic-bar > ul .down-speed-chooser.medium, .traffic-bar > ul .up-speed-chooser.medium { background-color: rgba(255, 192, 0, 0.2); }
.traffic-bar > ul .down-speed-chooser.high, .traffic-bar > ul .up-speed-chooser.high { background-color: rgba(126, 211, 80, 0.2); }
.traffic-bar > ul .down-speed-chooser.manual, .traffic-bar > ul .up-speed-chooser.manual { background-color: rgba(23, 162, 247, 0.2); }
.traffic-bar > ul .down-speed-chooser.low:after, .traffic-bar > ul .up-speed-chooser.low:after { background-color: #d54744; }
.traffic-bar > ul .down-speed-chooser.medium:after, .traffic-bar > ul .up-speed-chooser.medium:after { background-color: #ffc000; }
.traffic-bar > ul .down-speed-chooser.high:after, .traffic-bar > ul .up-speed-chooser.high:after { background-color: #7ed350; }
.traffic-bar > ul .down-speed-chooser.manual:after, .traffic-bar > ul .up-speed-chooser.manual:after { background-color: #17a2f7; }
.traffic-bar > ul .down-speed-chooser span, .traffic-bar > ul .up-speed-chooser span { cursor: pointer; }
.traffic-bar > ul .down-speed-chooser { padding-left: 25px; }
.traffic-bar > ul .up-speed-chooser { padding-right: 22px; width: 70px; }
.traffic-bar > ul .down-speed-chooser:before { background-position: 0 -76px; top: 6px; left: 8px; }
.traffic-bar > ul .up-speed-chooser:before { background-position: 0 -56px; top: 5px; left: 0; }
.traffic-bar > ul .down-speed-chooser.open .speed-chooser, .traffic-bar > ul .up-speed-chooser.open .speed-chooser { display: block; }
.traffic-bar .speed-chooser { position: absolute; display: none; bottom: 40px; left: 0; width: 197px; height: auto; background-color: #fff; border-top: 1px solid #b5b5b5; border-right: 1px solid #b5b5b5; border-left: 1px solid #b5b5b5; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-box-shadow: rgba(137, 137, 137, 0.43) 1px -2px 6px; -webkit-box-shadow: rgba(137, 137, 137, 0.43) 1px -2px 6px; box-shadow: rgba(137, 137, 137, 0.43) 1px -2px 6px; }
.traffic-bar .speed-chooser li { display: block; height: 30px; margin-bottom: 5px; line-height: 30px; padding-left: 30px; border-left: 4px solid transparent; width: 100%; cursor: pointer; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.traffic-bar .speed-chooser li:last-child { margin-bottom: 0; }
.traffic-bar .speed-chooser li.low { border-color: #d54744; }
.traffic-bar .speed-chooser li.low:hover { background-color: rgba(213, 71, 68, 0.2); }
.traffic-bar .speed-chooser li.medium { border-color: #ffc000; }
.traffic-bar .speed-chooser li.medium:hover { background-color: rgba(255, 192, 0, 0.2); }
.traffic-bar .speed-chooser li.high { border-color: #7ed350; }
.traffic-bar .speed-chooser li.high:hover { background-color: rgba(126, 211, 80, 0.2); }
.traffic-bar .speed-chooser li.manual { border-color: #17a2f7; }
.traffic-bar .speed-chooser li.manual:hover { background-color: rgba(23, 162, 247, 0.2); }
.traffic-bar .speed-chooser li .unlimited { border-color: #b7bebb; }
.traffic-bar .speed-chooser li .unlimited:hover { background-color: #7a8480; }
.traffic-bar .speed-chooser li span { cursor: pointer; position: relative; }
.traffic-bar .speed-chooser li.selected span:before { content: ''; display: block; width: 11px; height: 8px; position: absolute; top: 5px; left: -19px; background-position: 0 -124px; }
.traffic-bar .wrapper_right { float: right; overflow: hidden; margin-top: 7px; margin-right: 30px; padding-right: 5px; cursor: pointer; max-width: -webkit-calc(100% - 290px); max-width: calc(100% - 290px); }
.traffic-bar .download_title { font-size: 13px; white-space: nowrap; text-overflow: ellipsis; overflow: hidden; max-width: 100%; cursor: pointer; }
.traffic-bar .download_title.error { color: #bc3737; padding-left: 20px; background-position: -40px -268px; cursor: default; }

/*traffic-bar: end*/
/*popup: start*/
.popup__overlay, .popup__overlay_adddownload, .popup-choose-speed, .trnsprtnt_for_updates { position: fixed; left: 0; top: 0; right: 0; bottom: 0; z-index: 3000; }

.trnsprtnt_for_updates { z-index: 2500; }

.files_tree .popup .center_right { bottom: 37px; }
.files_tree .popup .total { bottom: 12px; }
.files_tree .popup .enable_scheduler { bottom: 12px; position: absolute; top: auto; }
.files_tree .popup.scheduler_on .center_right { bottom: 110px; }
.files_tree .popup.scheduler_on .total { bottom: 86px; }
.files_tree .popup.scheduler_on .enable_scheduler { bottom: 86px; }
.files_tree .popup.scheduler_on .error-message { bottom: 3px; }
.files_tree .popup.scheduler_on .scheduler { position: absolute; margin-left: 0; left: 10px; right: 10px; width: auto; bottom: -6px; top: auto; }

.mount { position: absolute; left: 0; top: 59px; right: 0; bottom: 0; background-color: rgba(255, 255, 255, 0.6); }

.popup__overlay_adddownload .error-message { margin-top: 10px; position: absolute; color: #db5155; overflow: hidden; text-overflow: ellipsis; max-width: 300px; }

.modal_title { color: #16a4fa; margin-bottom: 20px; font: 32px "Roboto Condensed Light", "Helvetica Neue", Arial, sans-serif; }

.catch_block { position: absolute; margin-top: -20px; }
.catch_block input[type=checkbox] + label { font-size: 12px; padding-left: 20px; }
.catch_block input[type=checkbox] + label:before { background-position: 0 -44px; top: 0; }
.catch_block input[type=checkbox]:checked + label:before { background-position: -40px -44px; }

.group_button { float: right; }
.group_button .left_button, .group_button .right_button { display: inline-block; height: 30px; width: 110px; text-align: center; border: none; cursor: pointer; font: 13px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; }
.group_button .left_button:disabled, .group_button .right_button:disabled { opacity: .4; cursor: default; }
.group_button .left_button.btn_anyway, .group_button .right_button.btn_anyway { color: #fff; background-color: #ff8b00; min-width: 130px; width: auto; }
.group_button .left_button { margin-right: 10px; color: #272e2c; background-color: #dfdedf; }
.group_button .right_button { color: #fff; background-color: #16a4fa; }
.group_button .right_button.tutorial_p2 { width: 171px; }

.error_msg { display: none; }

.title_tag { float: left; color: #585759; font-size: 15px; margin-right: 12px; }

.close_button { display: block; width: 40px; height: 40px; background-position: -47px -55px; position: absolute; top: 0; right: 0; }

.popup .header, .delete .header, .add_url .header, .delete .header, .scheduler .header, .change_url_wrapper .header, .popup_exist .add_url .header, .simple .simple_dialog .header { width: 100%; height: 36px; color: #fff; font-size: 14px; line-height: 36px; padding: 0 10px; background-color: #333c4e; position: relative; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .close_button, .delete .close_button, .add_url .close_button, .delete .close_button, .scheduler .close_button, .change_url_wrapper .close_button, .popup_exist .add_url .close_button, .simple .simple_dialog .close_button { width: 24px; height: 24px; position: absolute; top: 6px; right: 6px; cursor: pointer; background-position: 7px -365px; }
.popup .center, .delete .center, .add_url .center, .delete .center, .scheduler .center, .change_url_wrapper .center, .popup_exist .add_url .center, .simple .simple_dialog .center { position: absolute; top: 36px; bottom: 50px; left: 0; right: 0; font-size: 14px; padding: 18px 10px; word-wrap: break-word; }
.popup .title, .delete .title, .add_url .title, .delete .title, .scheduler .title, .change_url_wrapper .title, .popup_exist .add_url .title, .simple .simple_dialog .title { margin-bottom: 10px; }
.popup .block_element, .popup .center_left, .delete .block_element, .delete .center_left, .add_url .block_element, .add_url .center_left, .delete .block_element, .delete .center_left, .scheduler .block_element, .scheduler .center_left, .change_url_wrapper .block_element, .change_url_wrapper .center_left, .popup_exist .add_url .block_element, .popup_exist .add_url .center_left, .simple .simple_dialog .block_element, .simple .simple_dialog .center_left { margin-bottom: 10px; }
.popup .block_element:after, .popup .center_left:after, .delete .block_element:after, .delete .center_left:after, .add_url .block_element:after, .add_url .center_left:after, .delete .block_element:after, .delete .center_left:after, .scheduler .block_element:after, .scheduler .center_left:after, .change_url_wrapper .block_element:after, .change_url_wrapper .center_left:after, .popup_exist .add_url .block_element:after, .popup_exist .add_url .center_left:after, .simple .simple_dialog .block_element:after, .simple .simple_dialog .center_left:after { content: ''; display: block; clear: both; }
.popup .link_name, .delete .link_name, .add_url .link_name, .delete .link_name, .scheduler .link_name, .change_url_wrapper .link_name, .popup_exist .add_url .link_name, .simple .simple_dialog .link_name { margin-top: 4px; display: block; color: #757575; }
.popup input[type=text], .delete input[type=text], .add_url input[type=text], .delete input[type=text], .scheduler input[type=text], .change_url_wrapper input[type=text], .popup_exist .add_url input[type=text], .simple .simple_dialog input[type=text] { width: 100%; height: 28px; border: none; outline: none; display: block; }
.popup input[type=checkbox] + label, .delete input[type=checkbox] + label, .add_url input[type=checkbox] + label, .delete input[type=checkbox] + label, .scheduler input[type=checkbox] + label, .change_url_wrapper input[type=checkbox] + label, .popup_exist .add_url input[type=checkbox] + label, .simple .simple_dialog input[type=checkbox] + label { padding-left: 17px; color: #444; }
.popup input[type=checkbox]:disabled + label, .delete input[type=checkbox]:disabled + label, .add_url input[type=checkbox]:disabled + label, .delete input[type=checkbox]:disabled + label, .scheduler input[type=checkbox]:disabled + label, .change_url_wrapper input[type=checkbox]:disabled + label, .popup_exist .add_url input[type=checkbox]:disabled + label, .simple .simple_dialog input[type=checkbox]:disabled + label { color: rgba(58, 57, 59, 0.5); }
.popup input[type=checkbox] + label:before, .delete input[type=checkbox] + label:before, .add_url input[type=checkbox] + label:before, .delete input[type=checkbox] + label:before, .scheduler input[type=checkbox] + label:before, .change_url_wrapper input[type=checkbox] + label:before, .popup_exist .add_url input[type=checkbox] + label:before, .simple .simple_dialog input[type=checkbox] + label:before { width: 12px; height: 12px; top: 2px; }
.popup input[type=checkbox] + label:before, .delete input[type=checkbox] + label:before, .add_url input[type=checkbox] + label:before, .delete input[type=checkbox] + label:before, .scheduler input[type=checkbox] + label:before, .change_url_wrapper input[type=checkbox] + label:before, .popup_exist .add_url input[type=checkbox] + label:before, .simple .simple_dialog input[type=checkbox] + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>fff</title><rect width="12" height="12" style="fill:#fff"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383"/></svg>') no-repeat; }
.popup input[type=checkbox]:checked + label:before, .delete input[type=checkbox]:checked + label:before, .add_url input[type=checkbox]:checked + label:before, .delete input[type=checkbox]:checked + label:before, .scheduler input[type=checkbox]:checked + label:before, .change_url_wrapper input[type=checkbox]:checked + label:before, .popup_exist .add_url input[type=checkbox]:checked + label:before, .simple .simple_dialog input[type=checkbox]:checked + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>gg</title><rect width="12" height="12" style="fill:#fff"/><rect width="12" height="12" style="fill:#fff"/><path d="M4.73,7.12l4.5-4.5,1.13,1L4.74,9.32,2.11,6.62l1.2-1Z" style="fill:#7c7c7c"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383"/></svg>') no-repeat; }
.popup input[type=checkbox]:disabled + label:before, .delete input[type=checkbox]:disabled + label:before, .add_url input[type=checkbox]:disabled + label:before, .delete input[type=checkbox]:disabled + label:before, .scheduler input[type=checkbox]:disabled + label:before, .change_url_wrapper input[type=checkbox]:disabled + label:before, .popup_exist .add_url input[type=checkbox]:disabled + label:before, .simple .simple_dialog input[type=checkbox]:disabled + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>4</title><rect width="12" height="12" style="fill:#fff;opacity:0.6"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383;opacity:0.6"/></svg>') no-repeat; }
.popup input[type=checkbox]:disabled:checked + label:before, .delete input[type=checkbox]:disabled:checked + label:before, .add_url input[type=checkbox]:disabled:checked + label:before, .delete input[type=checkbox]:disabled:checked + label:before, .scheduler input[type=checkbox]:disabled:checked + label:before, .change_url_wrapper input[type=checkbox]:disabled:checked + label:before, .popup_exist .add_url input[type=checkbox]:disabled:checked + label:before, .simple .simple_dialog input[type=checkbox]:disabled:checked + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>55</title><rect width="12" height="12" style="fill:#fff;opacity:0.6"/><rect width="12" height="12" style="fill:#fff;opacity:0.6"/><path d="M4.73,7.12l4.5-4.5,1.13,1L4.74,9.32,2.11,6.62l1.2-1Z" style="fill:#7c7c7c;opacity:0.6"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383;opacity:0.6"/></svg>') no-repeat; }
.popup input[type=text].path_field, .popup input[type=text], .delete input[type=text].path_field, .delete input[type=text], .add_url input[type=text].path_field, .add_url input[type=text], .delete input[type=text].path_field, .delete input[type=text], .scheduler input[type=text].path_field, .scheduler input[type=text], .change_url_wrapper input[type=text].path_field, .change_url_wrapper input[type=text], .popup_exist .add_url input[type=text].path_field, .popup_exist .add_url input[type=text], .simple .simple_dialog input[type=text].path_field, .simple .simple_dialog input[type=text] { display: block; background-color: #fff; border: 1px solid #d4d4d4; position: relative; padding: 0 8px; margin-top: 5px; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .select_buttons, .delete .select_buttons, .add_url .select_buttons, .delete .select_buttons, .scheduler .select_buttons, .change_url_wrapper .select_buttons, .popup_exist .add_url .select_buttons, .simple .simple_dialog .select_buttons { color: #0f8fdd; font-size: 0; float: right; text-align: right; }
.popup .select_buttons div, .delete .select_buttons div, .add_url .select_buttons div, .delete .select_buttons div, .scheduler .select_buttons div, .change_url_wrapper .select_buttons div, .popup_exist .add_url .select_buttons div, .simple .simple_dialog .select_buttons div { display: inline-block; font-size: 14px; cursor: pointer; margin-right: 20px; }
.popup .select_buttons div:last-child, .delete .select_buttons div:last-child, .add_url .select_buttons div:last-child, .delete .select_buttons div:last-child, .scheduler .select_buttons div:last-child, .change_url_wrapper .select_buttons div:last-child, .popup_exist .add_url .select_buttons div:last-child, .simple .simple_dialog .select_buttons div:last-child { margin-right: 0; }
.popup .list, .delete .list, .add_url .list, .delete .list, .scheduler .list, .change_url_wrapper .list, .popup_exist .add_url .list, .simple .simple_dialog .list { position: absolute; background-color: #fff; top: 28px; left: -1px; right: -1px; border: 1px solid #d4d4d4; max-height: 140px; overflow-y: auto; z-index: 2500; -moz-box-shadow: 0 0 3px 0 rgba(64, 64, 64, 0.23); -webkit-box-shadow: 0 0 3px 0 rgba(64, 64, 64, 0.23); box-shadow: 0 0 3px 0 rgba(64, 64, 64, 0.23); }
.popup .list div, .delete .list div, .add_url .list div, .delete .list div, .scheduler .list div, .change_url_wrapper .list div, .popup_exist .add_url .list div, .simple .simple_dialog .list div { padding: 0 6px; line-height: 28px; white-space: nowrap; }
.popup .list div:hover, .delete .list div:hover, .add_url .list div:hover, .delete .list div:hover, .scheduler .list div:hover, .change_url_wrapper .list div:hover, .popup_exist .add_url .list div:hover, .simple .simple_dialog .list div:hover { cursor: pointer; background-color: #b9e4fd; }
.popup .bottom, .delete .bottom, .add_url .bottom, .delete .bottom, .scheduler .bottom, .change_url_wrapper .bottom, .popup_exist .add_url .bottom, .simple .simple_dialog .bottom { position: absolute; left: 0; right: 0; bottom: 0; padding: 10px; text-align: right; font-size: 0; }
.popup .bottom button, .delete .bottom button, .add_url .bottom button, .delete .bottom button, .scheduler .bottom button, .change_url_wrapper .bottom button, .popup_exist .add_url .bottom button, .simple .simple_dialog .bottom button { display: inline-block; cursor: pointer; margin-right: 8px; margin-left: 0; font-size: 14px; font-weight: 400; padding: 0 16px; height: auto; line-height: 26px; width: auto; min-width: 75px; text-align: center; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .bottom button:last-child, .delete .bottom button:last-child, .add_url .bottom button:last-child, .delete .bottom button:last-child, .scheduler .bottom button:last-child, .change_url_wrapper .bottom button:last-child, .popup_exist .add_url .bottom button:last-child, .simple .simple_dialog .bottom button:last-child { margin-right: 0; }
.popup .bottom button.right_button, .delete .bottom button.right_button, .add_url .bottom button.right_button, .delete .bottom button.right_button, .scheduler .bottom button.right_button, .change_url_wrapper .bottom button.right_button, .popup_exist .add_url .bottom button.right_button, .simple .simple_dialog .bottom button.right_button { color: #fff; background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzRjYjlmYyIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzQ3YWVlZCIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #4cb9fc), color-stop(100%, #47aeed)); background: -moz-linear-gradient(top, #4cb9fc 0%, #47aeed 100%); background: -webkit-linear-gradient(top, #4cb9fc 0%, #47aeed 100%); background: linear-gradient(to bottom, #4cb9fc 0%, #47aeed 100%); border: 1px solid #2fa4ed; }
.popup .bottom button.right_button:hover, .delete .bottom button.right_button:hover, .add_url .bottom button.right_button:hover, .delete .bottom button.right_button:hover, .scheduler .bottom button.right_button:hover, .change_url_wrapper .bottom button.right_button:hover, .popup_exist .add_url .bottom button.right_button:hover, .simple .simple_dialog .bottom button.right_button:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzdmY2VmZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzY3YzRmZiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #7fceff), color-stop(100%, #67c4ff)); background: -moz-linear-gradient(top, #7fceff 0%, #67c4ff 100%); background: -webkit-linear-gradient(top, #7fceff 0%, #67c4ff 100%); background: linear-gradient(to bottom, #7fceff 0%, #67c4ff 100%); }
.popup .bottom button.right_button.pushed:hover, .delete .bottom button.right_button.pushed:hover, .add_url .bottom button.right_button.pushed:hover, .delete .bottom button.right_button.pushed:hover, .scheduler .bottom button.right_button.pushed:hover, .change_url_wrapper .bottom button.right_button.pushed:hover, .popup_exist .add_url .bottom button.right_button.pushed:hover, .simple .simple_dialog .bottom button.right_button.pushed:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzM0YThlZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzJhOWFkZiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #34a8ef), color-stop(100%, #2a9adf)); background: -moz-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: -webkit-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: linear-gradient(to bottom, #34a8ef 0%, #2a9adf 100%); border-color: #148dd9; }
.popup .bottom button.right_button:disabled, .delete .bottom button.right_button:disabled, .add_url .bottom button.right_button:disabled, .delete .bottom button.right_button:disabled, .scheduler .bottom button.right_button:disabled, .change_url_wrapper .bottom button.right_button:disabled, .popup_exist .add_url .bottom button.right_button:disabled, .simple .simple_dialog .bottom button.right_button:disabled { border-color: #0365a4; }
.popup .bottom button.right_button:disabled:hover, .delete .bottom button.right_button:disabled:hover, .add_url .bottom button.right_button:disabled:hover, .delete .bottom button.right_button:disabled:hover, .scheduler .bottom button.right_button:disabled:hover, .change_url_wrapper .bottom button.right_button:disabled:hover, .popup_exist .add_url .bottom button.right_button:disabled:hover, .simple .simple_dialog .bottom button.right_button:disabled:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzM0YThlZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzJhOWFkZiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #34a8ef), color-stop(100%, #2a9adf)); background: -moz-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: -webkit-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: linear-gradient(to bottom, #34a8ef 0%, #2a9adf 100%); }
.popup .bottom button.right_button:disabled.pushed:hover, .delete .bottom button.right_button:disabled.pushed:hover, .add_url .bottom button.right_button:disabled.pushed:hover, .delete .bottom button.right_button:disabled.pushed:hover, .scheduler .bottom button.right_button:disabled.pushed:hover, .change_url_wrapper .bottom button.right_button:disabled.pushed:hover, .popup_exist .add_url .bottom button.right_button:disabled.pushed:hover, .simple .simple_dialog .bottom button.right_button:disabled.pushed:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzM0YThlZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzJhOWFkZiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #34a8ef), color-stop(100%, #2a9adf)); background: -moz-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: -webkit-linear-gradient(top, #34a8ef 0%, #2a9adf 100%); background: linear-gradient(to bottom, #34a8ef 0%, #2a9adf 100%); }
.popup .bottom button.left_button, .delete .bottom button.left_button, .add_url .bottom button.left_button, .delete .bottom button.left_button, .scheduler .bottom button.left_button, .change_url_wrapper .bottom button.left_button, .popup_exist .add_url .bottom button.left_button, .simple .simple_dialog .bottom button.left_button { color: #000; background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2YwZjBmMCIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2U1ZTVlNSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #f0f0f0), color-stop(100%, #e5e5e5)); background: -moz-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: -webkit-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: linear-gradient(to bottom, #f0f0f0 0%, #e5e5e5 100%); border: 1px solid #d4d4d4; }
.popup .bottom button.left_button:hover, .delete .bottom button.left_button:hover, .add_url .bottom button.left_button:hover, .delete .bottom button.left_button:hover, .scheduler .bottom button.left_button:hover, .change_url_wrapper .bottom button.left_button:hover, .popup_exist .add_url .bottom button.left_button:hover, .simple .simple_dialog .bottom button.left_button:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2Y1ZjVmNSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #ffffff), color-stop(100%, #f5f5f5)); background: -moz-linear-gradient(top, #ffffff 0%, #f5f5f5 100%); background: -webkit-linear-gradient(top, #ffffff 0%, #f5f5f5 100%); background: linear-gradient(to bottom, #ffffff 0%, #f5f5f5 100%); }
.popup .bottom button.left_button.pushed:hover, .delete .bottom button.left_button.pushed:hover, .add_url .bottom button.left_button.pushed:hover, .delete .bottom button.left_button.pushed:hover, .scheduler .bottom button.left_button.pushed:hover, .change_url_wrapper .bottom button.left_button.pushed:hover, .popup_exist .add_url .bottom button.left_button.pushed:hover, .simple .simple_dialog .bottom button.left_button.pushed:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2U1ZTVlNSIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2RiZGJkYiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #e5e5e5), color-stop(100%, #dbdbdb)); background: -moz-linear-gradient(top, #e5e5e5 0%, #dbdbdb 100%); background: -webkit-linear-gradient(top, #e5e5e5 0%, #dbdbdb 100%); background: linear-gradient(to bottom, #e5e5e5 0%, #dbdbdb 100%); }
.popup .bottom button.left_button:disabled, .delete .bottom button.left_button:disabled, .add_url .bottom button.left_button:disabled, .delete .bottom button.left_button:disabled, .scheduler .bottom button.left_button:disabled, .change_url_wrapper .bottom button.left_button:disabled, .popup_exist .add_url .bottom button.left_button:disabled, .simple .simple_dialog .bottom button.left_button:disabled { border-color: #a2a2a2; }
.popup .bottom button.left_button:disabled:hover, .delete .bottom button.left_button:disabled:hover, .add_url .bottom button.left_button:disabled:hover, .delete .bottom button.left_button:disabled:hover, .scheduler .bottom button.left_button:disabled:hover, .change_url_wrapper .bottom button.left_button:disabled:hover, .popup_exist .add_url .bottom button.left_button:disabled:hover, .simple .simple_dialog .bottom button.left_button:disabled:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2YwZjBmMCIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2U1ZTVlNSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #f0f0f0), color-stop(100%, #e5e5e5)); background: -moz-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: -webkit-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: linear-gradient(to bottom, #f0f0f0 0%, #e5e5e5 100%); }
.popup .bottom button.left_button:disabled.pushed:hover, .delete .bottom button.left_button:disabled.pushed:hover, .add_url .bottom button.left_button:disabled.pushed:hover, .delete .bottom button.left_button:disabled.pushed:hover, .scheduler .bottom button.left_button:disabled.pushed:hover, .change_url_wrapper .bottom button.left_button:disabled.pushed:hover, .popup_exist .add_url .bottom button.left_button:disabled.pushed:hover, .simple .simple_dialog .bottom button.left_button:disabled.pushed:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iIzRjYjlmYyIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iIzQ3YWVlZCIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #4cb9fc), color-stop(100%, #47aeed)); background: -moz-linear-gradient(top, #4cb9fc 0%, #47aeed 100%); background: -webkit-linear-gradient(top, #4cb9fc 0%, #47aeed 100%); background: linear-gradient(to bottom, #4cb9fc 0%, #47aeed 100%); }

.tutorial { top: 50%; height: 450px; width: 702px; margin-left: -351px; margin-top: -221px; padding-top: 20px; left: 50%; right: auto; }

.tutorial_img { margin-top: 15px; }

.tutorial_img.tutorial_p2 { margin-top: 31px; margin-bottom: 3px; }

.tutorial_img.tutorial_p1 { padding-bottom: 3px; }

.popup .wrapper_proxy { margin-bottom: 30px; margin-top: 53px; }
.popup .wrapper_proxy input[type=checkbox] + label { padding-left: 18px; }
.popup .wrapper_proxy input[type=checkbox] + label:before { top: 50%; margin-top: -6px; left: 0; }
.popup .wrapper_proxy input[type=text], .popup .wrapper_proxy input[type=password] { margin: 0 20px 0 5px; padding: 0 2px; width: 126px; height: 18px; border: 1px solid #b8b8b8; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .wrapper_proxy .wrap_form { margin-top: 8px; font-size: 14px; }
.popup .wrapper_proxy .wrap_form.disabled { color: rgba(58, 57, 59, 0.5); }
.popup .wrapper_proxy .wrap_form.disabled input[type=text]:disabled, .popup .wrapper_proxy .wrap_form.disabled input[type=password]:disabled { border-color: #e3e3e3; color: #99999a; background-color: #fff; cursor: default !important; }
.popup .wrapper_proxy .wrap_form label { float: left; line-height: 30px; }
.popup .wrapper_proxy .wrap_form input[type=text], .popup .wrapper_proxy .wrap_form input[type=password] { width: 136px; height: 30px; padding: 0 8px; font-size: 14px; float: left; margin-left: 8px; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; }

.loading { position: absolute; left: 10px; right: 270px; display: -webkit-flex; display: flex; -webkit-align-items: center; align-items: center; }
.loading img { width: 22px; height: 22px; }
.loading span { padding-left: 10px; }

.download-wiz-source-info .popup, .hash_popup .delete, .popup .add_url, .popup.choose-speed, .popup__overlay .delete, .scheduler, .change_url_wrapper, .popup_exist .add_url, .simple .simple_dialog, .popup_extension .popup { width: 542px; height: 444px; position: absolute; top: 50%; left: 50%; margin-top: -222px; margin-left: -271px; background-color: #f5f5f5; border: none; -moz-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -webkit-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); }

.simple .simple_dialog { height: 120px; margin-top: -60px; }

.single-file.download-wiz-source-info .popup { height: 281px; margin-top: -140px; top: 50%; }

.single-file.download-wiz-source-info .catch_block { margin-top: 52px; }

.single-file.download-wiz-source-info .popup.youtube { height: 350px; margin-top: -175px; top: 50%; }

.single-file .popup .enable_scheduler { top: 1px; }

.single-file .popup.scheduler_on .total { bottom: 99px; }

.single-file .popup .link_name { margin-bottom: 10px; }

.single-file .popup.scheduler_on { height: 373px !important; margin-top: -186px !important; }

.single-file.with_note .popup, .change_path.with_note .popup { height: 373px; margin-top: -186px; }
.single-file.with_note .popup .total, .change_path.with_note .popup .total { bottom: 112px; }
.single-file.with_note .popup.scheduler_on .total, .change_path.with_note .popup.scheduler_on .total { bottom: 98px; }
.single-file.with_note .note_info, .change_path.with_note .note_info { margin-top: 22px; font-size: 14px; line-height: 18px; padding: 18px 10px 18px 48px; color: #000; position: relative; background-color: #cdecff; }
.single-file.with_note .note_info:before, .change_path.with_note .note_info:before { content: ''; display: block; width: 20px; height: 20px; position: absolute; top: 50%; margin-top: -10px; left: 16px; background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 20 20"><title>Vector Smart Object51</title><path d="M12,2A10,10,0,1,0,22,12,10,10,0,0,0,12,2Z" transform="translate(-2 -2)" style="fill:#4ab4f5"/><rect x="9" y="9" width="2" height="6" style="fill:#fff"/><rect x="9" y="5" width="2" height="2" style="fill:#fff"/></svg>') no-repeat center; }

.change_path.with_note .popup { height: 267px; margin-top: -134px; }
.change_path.with_note .popup .clear { float: none; clear: both; width: 100%; min-height: 30px; }
.change_path.with_note .popup .note_info { margin-top: 10px; }

.popup.waiting { height: 300px !important; }
.popup.waiting .loader img { margin-top: 6px; }
.popup.waiting .top_info { padding-left: 7px; }
.popup.scheduler_on .scheduler { position: relative; margin-left: 0; background-color: #fff; margin-top: 10px; width: 100%; padding: 12px; height: 84px; left: 0; top: 0; z-index: 100; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; border: 1px solid #d4d4d4; }
.popup.scheduler_on .total { bottom: 84px; }
.popup.scheduler_on .center { bottom: 64px; }
.popup.scheduler_on .error-message { left: 10px; bottom: -47px; z-index: 10; }
.popup.scheduler_on .bottom .error-message { bottom: 11px; height: 32px; line-height: 16px; display: table; }
.popup.scheduler_on .bottom .error-message span { display: table-cell; vertical-align: middle; }
.popup .title { font-weight: 600; }
.popup .empty_list select { visibility: hidden; }
.popup .empty_list .inselect div { margin-right: 0; }
.popup .loader { text-align: center; margin-top: 55px; }
.popup .wrap_tags { float: none; clear: both; overflow: hidden; margin-top: 10px; }
.popup .wrap_tags .wrap_manage { float: left; }
.popup .wrap_tags .manage_btn { background-color: #ceeffd; background-position: 14px 4px; }
.popup .top_left { overflow: hidden; position: relative; min-height: 54px; }
.popup .popup_top_icon { min-width: 40px; height: 40px; margin-right: 15px; float: left; overflow: hidden; display: table; vertical-align: middle; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .popup_top_icon .popup_top_icon_inner { display: table-cell; vertical-align: middle; }
.popup .popup_top_icon .popup_top_icon_inner img { max-height: 40px; }
.popup .top_info { float: left; margin-right: 12px; min-width: 60px; height: 40px; }
.popup .top_info .stretch_element { position: absolute; height: 40px; }
.popup .top_info .table_cell { display: table-cell; width: 1%; position: relative; top: 0; }
.popup .top_info .name { width: 100%; border: 1px solid #fff; color: #16a4fa; font: 22px/26px "Roboto Condensed Light", "Helvetica Neue", Arial, sans-serif; overflow: hidden; background: none; margin-left: -7px; margin-top: 2px; padding: 0 5px; max-height: 55px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }
.popup .top_info .name:disabled { border: none; -webkit-user-select: auto; }
.popup .top_info .name.edit { border: #16a4fa 1px solid; }
.popup .top_info input[type=text].name { -webkit-user-select: auto; cursor: text !important; line-height: 31px; border: 1px solid #e3e3e3; margin-bottom: 2px; width: -webkit-calc(100% - 6px); width: calc(100% - 6px); }
.popup .top_info .file_link { color: #3d3c3e; font-size: 13px; text-decoration: none; float: none; width: 100%; background: none; border: none; padding: 0 0 2px 0; -webkit-user-select: auto; }
.popup .top_info .popup_speed { margin-left: -7px; }
.popup .tooltip { display: none; background-color: whitesmoke; border: 2px solid #fff; position: fixed; z-index: 500; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); -webkit-box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); box-shadow: 0 0 10px 0 rgba(0, 0, 0, 0.5); }
.popup .image_downloading_wr { width: 196px; height: 111px; display: flex; justify-content: center; align-items: center; }
.popup .image_downloading { max-width: 196px; max-height: 111px; }
.popup .image-downloaded { max-width: 196px; max-height: 111px; }
.popup .add_channel { padding-top: 42px; }
.popup .title-input { display: block; margin-bottom: 4px; font-size: 14px; color: #444; }
.popup .transparent_select { display: none; position: fixed; top: 59px; bottom: 0; left: 0; right: 0; z-index: 2500; }
.popup .inselect { float: left; height: auto; width: -webkit-calc(100% - 46px); width: calc(100% - 46px); background-color: #fff; border: 1px solid #d4d4d4; position: relative; padding: 0 36px 0 6px; line-height: 28px; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .inselect.wrapper_inselect { padding: 0; z-index: 500; }
.popup .inselect .dropdown_button { position: absolute; top: 0; right: 0; width: 30px; height: 28px; cursor: pointer; border-left: 1px solid #ededed; }
.popup .inselect .dropdown_button:before { content: ''; display: block; width: 0; height: 0; border-style: solid; border-width: 5px 3px 0 3px; border-color: #929292 transparent transparent transparent; position: absolute; top: 50%; left: 50%; margin-top: -2px; margin-left: -3px; }
.popup .inselect .input_wrapper { margin-right: 30px; }
.popup .inselect div input { width: 100%; height: 28px; border: none; margin: 0; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; outline: none; padding: 0 0 0 8px; }
.popup .inselect div input:focus { outline: none; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.popup .inselect select { width: 100%; height: 29px; padding-right: 30px; background: url("../v2_images/arrow_combobox.svg") no-repeat right 11px center; -webkit-appearance: none; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; border: none; font: 14px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; }
.popup .button_folder { width: 39px; height: 30px; float: right; cursor: pointer; border: 1px solid #d4d4d4; position: relative; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2YwZjBmMCIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2U1ZTVlNSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #f0f0f0), color-stop(100%, #e5e5e5)); background: -moz-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: -webkit-linear-gradient(top, #f0f0f0 0%, #e5e5e5 100%); background: linear-gradient(to bottom, #f0f0f0 0%, #e5e5e5 100%); -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .button_folder:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2ZmZmZmZiIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2Y1ZjVmNSIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #ffffff), color-stop(100%, #f5f5f5)); background: -moz-linear-gradient(top, #ffffff 0%, #f5f5f5 100%); background: -webkit-linear-gradient(top, #ffffff 0%, #f5f5f5 100%); background: linear-gradient(to bottom, #ffffff 0%, #f5f5f5 100%); }
.popup .button_folder.pushed:hover { background: url('data:image/svg+xml;base64,PD94bWwgdmVyc2lvbj0iMS4wIiBlbmNvZGluZz0idXRmLTgiPz4gPHN2ZyB2ZXJzaW9uPSIxLjEiIHhtbG5zPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwL3N2ZyI+PGRlZnM+PGxpbmVhckdyYWRpZW50IGlkPSJncmFkIiBncmFkaWVudFVuaXRzPSJvYmplY3RCb3VuZGluZ0JveCIgeDE9IjAuNSIgeTE9IjAuMCIgeDI9IjAuNSIgeTI9IjEuMCI+PHN0b3Agb2Zmc2V0PSIwJSIgc3RvcC1jb2xvcj0iI2U1ZTVlNSIvPjxzdG9wIG9mZnNldD0iMTAwJSIgc3RvcC1jb2xvcj0iI2RiZGJkYiIvPjwvbGluZWFyR3JhZGllbnQ+PC9kZWZzPjxyZWN0IHg9IjAiIHk9IjAiIHdpZHRoPSIxMDAlIiBoZWlnaHQ9IjEwMCUiIGZpbGw9InVybCgjZ3JhZCkiIC8+PC9zdmc+IA=='); background: -webkit-gradient(linear, 50% 0%, 50% 100%, color-stop(0%, #e5e5e5), color-stop(100%, #dbdbdb)); background: -moz-linear-gradient(top, #e5e5e5 0%, #dbdbdb 100%); background: -webkit-linear-gradient(top, #e5e5e5 0%, #dbdbdb 100%); background: linear-gradient(to bottom, #e5e5e5 0%, #dbdbdb 100%); }
.popup .button_folder:before { content: ''; background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 37 30"><title>wwww</title><circle cx="11.5" cy="20.5" r="1.5" style="fill:#000"/><circle cx="18.5" cy="20.5" r="1.5" style="fill:#000"/><circle cx="25.5" cy="20.5" r="1.5" style="fill:#000"/><rect width="37" height="30" style="fill:none"/></svg>') no-repeat center; height: 30px; width: 39px; position: absolute; top: 0; left: 0; }
.popup .error-message { position: absolute; bottom: 4px; left: 10px; height: 40px; max-width: 280px; font-size: 13px; color: #db5155; text-align: left; display: table; }
.popup .error-message span { display: table-cell; vertical-align: middle; }
.popup .center_right { background-color: #fff; border: 1px solid #d4d4d4; -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; overflow-y: auto; margin-top: 12px; position: absolute; top: 173px; bottom: 100px; left: 10px; right: 10px; }
.popup .center_right.batch { top: 150px; }
.popup .center_right.batch .file_name { line-height: 19px; max-width: -webkit-calc(100% - 10px); max-width: calc(100% - 10px); -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .wrapper_tree { max-height: 100%; height: auto; padding: 0 8px 10px 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .wrapper_tree > .tree > ul > .leaf { margin-left: -16px; }
.popup .wrapper_tree > .tree > ul > .leaf.no_margin { margin-left: 0; }
.popup .wrapper_tree .tree > ul > li { margin-left: 0; }
.popup .wrapper_tree ul { display: block; margin: 0; padding: 0; list-style-type: none; }
.popup .wrapper_tree li, .popup .wrapper_tree ins { background-repeat: no-repeat; background-color: transparent; }
.popup .wrapper_tree ins { display: inline-block; text-decoration: none; padding: 0; width: 15px; height: 15px; float: left; margin-top: 4px; }
.popup .wrapper_tree li { display: block; min-height: 18px; white-space: nowrap; min-width: 18px; margin-left: 18px; }
.popup .wrapper_tree li.last { background: transparent; }
.popup .wrapper_tree li.open > ul { display: block; }
.popup .wrapper_tree li.closed > ul { display: none; }
.popup .wrapper_tree .open > ins { background-position: -17px -189px; }
.popup .wrapper_tree .closed > ins { background-position: 5px -191px; }
.popup .wrapper_tree .leaf > ins { visibility: hidden; }
.popup .wrapper_tree .file_size { float: right; font-size: 13px; padding-top: 4px; color: #000; }
.popup .wrapper_tree .file_name { max-width: 82%; display: inline-block; padding-left: 19px; font-size: 13px; position: relative; top: 4px; }
.popup .wrapper_tree input[type="checkbox"] + label { overflow: hidden; position: relative; display: inline-block; padding-left: 0; width: -webkit-calc(100% - 16px); width: calc(100% - 16px); }
.popup .wrapper_tree input[type="checkbox"] { display: none; }
.popup .wrapper_tree input[type="checkbox"] + label { color: #444; }
.popup .wrapper_tree input[type="checkbox"]:checked + label { color: #000; }
.popup .wrapper_tree input[type="checkbox"] + label:before { content: ""; height: 12px; margin: 6px -6px -6px 3px; background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>fff</title><rect width="12" height="12" style="fill:#fff"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383"/></svg>') no-repeat; padding-left: 12px; display: inline-block; width: 0; top: 1px; }
.popup .wrapper_tree input[type="checkbox"]:checked + label:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>gg</title><rect width="12" height="12" style="fill:#fff"/><rect width="12" height="12" style="fill:#fff"/><path d="M4.73,7.12l4.5-4.5,1.13,1L4.74,9.32,2.11,6.62l1.2-1Z" style="fill:#7c7c7c"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#838383"/></svg>') no-repeat; }
.popup .wrapper_tree input[type="checkbox"] + label.indeterminate:before { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 12 12"><title>1</title><rect width="12" height="12" style="fill:#fff"/><rect x="3" y="3" width="6" height="6" style="fill:#7c7c7c"/><path d="M11.25.75v10.5H.75V.75h10.5M12,0H0V12H12Z" style="fill:#848484"/></svg>') no-repeat; }
.popup .wrapper_tree .yt_channel_spinner { display: flex; justify-content: center; margin: 10px; }
.popup .wrapper_tree .yt_channel_spinner .error { color: #bc3737; overflow: hidden; white-space: nowrap; text-overflow: ellipsis; max-width: -webkit-calc(100% - 100px); max-width: calc(100% - 100px); }
.popup .wrapper_tree .yt_channel_spinner button { display: block; float: right; color: #333c4e; padding: 3px 10px; background-color: #fff; margin-left: 10px; height: 22px; margin-top: -3px; cursor: pointer !important; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.popup .total { font-size: 14px; color: #444; text-align: left; position: absolute; bottom: 21px; left: 140px; right: 10px; }
.popup .total a { display: block; float: left; margin-right: 10px; color: #00A4F8; }
.popup .total span { display: block; float: right; }
.popup .total span.error_state { color: red; }
.popup .total .count { float: left; }
.popup .enable_scheduler { position: relative; z-index: 150; top: 12px; }
.popup .enable_scheduler input[type=checkbox] + label { color: #444; font-size: 14px; padding-left: 18px; }
.popup .enable_scheduler input[type=checkbox] + label:before { left: 0; top: 2px; width: 12px; height: 12px; }
.popup .enable_scheduler input[type=checkbox]:disabled + label:before { background-position: -60px -44px; }
.popup .enable_scheduler input[type=checkbox]:disabled:checked + label:before { background-position: -80px -44px; }

.single-file.with_note .popup .error-message { right: 390px; bottom: 11px; line-height: 16px; display: table; }
.single-file.with_note .popup .error-message span { display: table-cell; vertical-align: middle; width: 160px; }
.single-file.with_note .popup.scheduler_on { height: 467px !important; margin-top: -233px !important; }
.single-file.with_note .popup.scheduler_on .total { bottom: 192px; }

.popup.choose-speed { height: 175px; margin-top: -83px; }

.choose-speed { width: 500px; height: 276px; margin-left: -250px; margin-top: -138px; }

.center-choose-speed { margin-bottom: 42px; }
.center-choose-speed .row-wrap { float: left; width: 180px; margin-right: 8px; }
.center-choose-speed span { color: #585759; font-size: 15px; }
.center-choose-speed select { width: 180px; padding-left: 5px; font: 13px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; }
.center-choose-speed .s_margin_right { margin-right: 10px; width: 125px; display: inline-block; }
.center-choose-speed .choose { display: inline-block; }
.center-choose-speed .choose.bottom { position: relative; z-index: 10; }
.center-choose-speed .wrapper_inselect { width: 180px; height: 30px; z-index: 3000; }
.center-choose-speed .wrapper_inselect .list { top: 31px; }
.center-choose-speed .wrapper_inselect .list div { height: 22px; }
.center-choose-speed .wrapper_inselect .list div.disable { height: 14px; margin-top: -9px; }
.center-choose-speed .wrapper_inselect .list div.disable:hover { background-color: transparent; }
.center-choose-speed .wrapper_inselect .list div.disable span { cursor: default !important; }
.center-choose-speed .wrapper_inselect .list div span { cursor: pointer !important; }
.center-choose-speed .transparent_select { display: block; position: fixed; top: 0; bottom: 0; left: 0; right: 0; z-index: 500; }

.popup .add_url { height: 172px; margin-top: -86px; }

.popup_window .center_add_ul input[type=text] { width: -webkit-calc(100% - 46px); width: calc(100% - 46px); float: left; padding: 0 8px; font-size: 14px; height: 30px; width: -webkit-calc(100% - 46px); width: calc(100% - 46px); -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; }

.popup_window .center_add_ul .button_folder { margin-top: 5px; }

.show_proxy .center_add_ul { margin-bottom: 20px; }

.popup .show_proxy { height: 252px; margin-top: -126px; }
.popup .show_proxy input[type=text] { padding: 0 8px; float: left; height: 30px; width: -webkit-calc(100% - 46px); width: calc(100% - 46px); -moz-border-radius: 1px; -webkit-border-radius: 1px; border-radius: 1px; }
.popup .show_proxy .button_folder { margin-top: 5px; }

.show_proxy .error-message { left: 10px; }

.single-file .youtube { height: 370px; margin-top: -185px; }
.single-file .youtube.scheduler_on { height: 423px !important; margin-top: -210px !important; }
.single-file .youtube.scheduler_on .scheduler { margin-top: 8px; }
.single-file .youtube .total { display: none; }
.single-file .youtube.popup input { padding: 0 8px; font-size: 14px; }
.single-file .youtube .youtube_wrapper { min-height: 50px; }
.single-file .youtube .enable_scheduler { float: none; clear: both; margin-top: 8px; }

.single-file.is_trt .total { bottom: 15px; }

.single-file.is_trt .popup.scheduler_on .total { bottom: 93px; }

.youtube_wrapper select { width: auto; max-width: 100%; padding-right: 20px; padding-left: 5px; background-position: right 12px top 12px; color: #656466; font: 13px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; }
.youtube_wrapper .youtube-files-select { margin: 5px 0 10px; }
.youtube_wrapper .transparent_select { display: none; position: fixed; top: 0; bottom: 0; left: 0; right: 0; z-index: 500; }

.loader { text-align: center; position: absolute; top: 50%; left: 0; right: 0; margin-top: -38px; }
.loader img { display: block; margin: 0 auto 20px; }
.loader span { padding-right: 20px; }
.loader a { background-color: #dfdedf; padding: 4px 10px; font-size: 13px; color: #272e2c; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }

.channel_info { margin-bottom: 12px; }
.channel_info .channel_pic { width: 30px; height: 30px; object-fit: cover; float: left; margin-right: 10px; }
.channel_info .channel_name { font-weight: 700; margin-bottom: 1px; }
.channel_info .count { font-size: 12px; }

.youtube_channel .popup { height: 550px; margin-top: -275px; width: 562px; margin-left: -281px; }
.youtube_channel .popup.no_load_more .center_right.batch { bottom: 76px; }
.youtube_channel .popup .center_right.batch { top: 191px; bottom: 88px; }
.youtube_channel .popup .enable_scheduler { bottom: 22px; }
.youtube_channel .popup .total { bottom: 67px; }
.youtube_channel .popup .link_name.for_copy { margin-top: 7px; max-width: 76%; }
.youtube_channel .popup .select_buttons { margin-top: -26px; }
.youtube_channel .popup.scheduler_on .center_right { bottom: 110px; }
.youtube_channel .add_date { position: absolute; bottom: 47px; }
.youtube_channel .quality_wrapper { position: absolute; bottom: 11px; right: 10px; width: 200px; }
.youtube_channel .quality_wrapper label { display: block; margin-bottom: 5px; color: #444; }
.youtube_channel .quality_wrapper .wrapper_inselect { width: 100%; float: none; z-index: 3000; }
.youtube_channel .file_date { float: right; font-size: 13px; padding-top: 4px; color: #000; width: 80px; }
.youtube_channel .file_size { width: 80px; text-align: right; }
.youtube_channel .file_name { width: -webkit-calc(100% - 170px); width: calc(100% - 170px); }
.youtube_channel.youtube_playlist .file_name { width: -webkit-calc(100% - 100px); width: calc(100% - 100px); }
.youtube_channel .load_more { -webkit-appearance: none; border: none; background-color: transparent; color: #0f8fdd; font-size: 14px; position: absolute; right: 4px; bottom: 64px; cursor: pointer !important; }

.popup__overlay .delete { height: 164px; margin-top: -82px; }
.popup__overlay .delete .group_button { width: 100%; }
.popup__overlay .delete .group_button .right_button { float: left; }
.popup__overlay .delete label { position: absolute; bottom: 55px; left: 10px; font-size: 14px; }
.popup__overlay .delete label:before { position: absolute; left: 0; }

.delete, .hash_popup .delete { width: 542px; height: 258px; margin-left: -271px; margin-top: -129px; }
.delete.disk input[type=checkbox] + label, .delete.change_url_wrapper input[type=checkbox] + label, .hash_popup .delete.disk input[type=checkbox] + label, .hash_popup .delete.change_url_wrapper input[type=checkbox] + label { font-size: 13px; padding-left: 18px; }
.delete.disk input[type=checkbox] + label:before, .delete.change_url_wrapper input[type=checkbox] + label:before, .hash_popup .delete.disk input[type=checkbox] + label:before, .hash_popup .delete.change_url_wrapper input[type=checkbox] + label:before { left: 0; top: 2px; width: 12px; height: 12px; background-position: 0 -44px; }
.delete.disk input[type=checkbox]:checked + label:before, .delete.change_url_wrapper input[type=checkbox]:checked + label:before, .hash_popup .delete.disk input[type=checkbox]:checked + label:before, .hash_popup .delete.change_url_wrapper input[type=checkbox]:checked + label:before { background-position: -40px -44px; }
.delete.disk .group_button, .delete.change_url_wrapper .group_button, .hash_popup .delete.disk .group_button, .hash_popup .delete.change_url_wrapper .group_button { float: none; margin-top: 20px; }
.delete.disk .group_button .left_button, .delete.disk .group_button .right_button, .delete.change_url_wrapper .group_button .left_button, .delete.change_url_wrapper .group_button .right_button, .hash_popup .delete.disk .group_button .left_button, .hash_popup .delete.disk .group_button .right_button, .hash_popup .delete.change_url_wrapper .group_button .left_button, .hash_popup .delete.change_url_wrapper .group_button .right_button { width: auto; }
.delete.disk .right_button, .delete.change_url_wrapper .right_button, .hash_popup .delete.disk .right_button, .hash_popup .delete.change_url_wrapper .right_button { float: left; }
.delete.disk .left_button, .delete.change_url_wrapper .left_button, .hash_popup .delete.disk .left_button, .hash_popup .delete.change_url_wrapper .left_button { margin-right: 0; float: right; }
.delete.disk .from_disk, .delete.change_url_wrapper .from_disk, .hash_popup .delete.disk .from_disk, .hash_popup .delete.change_url_wrapper .from_disk { margin-right: 10px; }
.delete .files_list, .hash_popup .delete .files_list { padding-left: 11px; margin-left: 8px; list-style: inherit; }
.delete .files_list li, .hash_popup .delete .files_list li { color: #000; font-size: 13px; line-height: 18px; }
.delete.delete_files, .hash_popup .delete.delete_files { height: 276px; margin-top: -138px; }
.delete.delete_files .files_list, .hash_popup .delete.delete_files .files_list { position: absolute; width: auto; left: 10px; right: 10px; max-height: 140px; overflow-y: auto; list-style: none; margin-left: 0; padding-left: 0; }
.delete.delete_files .files_list li, .hash_popup .delete.delete_files .files_list li { display: list-item; list-style: disc; margin-left: 19px; }

.delete .files_list li > span { white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 100%; display: inline-block; vertical-align: bottom; }

.hash_popup .delete { height: 288px; }

.change_url .delete.change_url_wrapper .group_button { float: right; margin-top: 0; }
.change_url .delete.change_url_wrapper .group_button .left_button { float: left; }
.change_url .delete.change_url_wrapper input[type=checkbox] + label { position: absolute; margin-top: 7px; }
.change_url .change_url_wrapper { height: 194px; margin-top: -97px; }
.change_url .change_url_wrapper .f_text { font-size: 14px; color: #757575; }
.change_url .change_url_wrapper .f_text .grey { color: #757575; }
.change_url .change_url_wrapper .file_title { margin-top: 4px; }
.change_url .wrapper_label { float: left; padding-top: 6px; }
.change_url .wrapper_label label { font-size: 14px; }
.change_url .wrapper_label label:before { left: 0; }

.popup_exist .add_url { width: 400px; height: 130px; margin-left: -200px; margin-top: -65px; left: 50%; right: auto; }
.popup_exist .txt { margin-bottom: 24px; }

.reset_settings .modal_title { font-size: 25px; }
.reset_settings .delete.disk { width: 450px; margin-left: -225px; height: 140px; margin-top: -70px; }
.reset_settings .delete.disk label { position: static; }
.reset_settings .delete.disk .group_button { float: right; margin-top: 0; }
.reset_settings .delete.disk .group_button .left_button { float: none; margin-right: 10px; }
.reset_settings .delete.disk .group_button .right_button { float: none; }

.popup_extension .popup { height: 200px; margin-top: -100px; width: 385px; margin-left: -193px; color: #424242; }
.popup_extension .note { font-weight: 700; margin-bottom: 15px; color: #000; }
.popup_extension ul { padding-top: 8px; }
.popup_extension li { padding-left: 20px; margin-bottom: 10px; overflow: hidden; }
.popup_extension li:last-child { margin-bottom: 0; }
.popup_extension li.disabled, .popup_extension li.disabled a { color: #aaa; }
.popup_extension li.disabled a { cursor: default !important; }
.popup_extension li a { cursor: pointer !important; text-decoration: underline; color: #00a7f5; }
.popup_extension .info { margin-top: 10px; display: block; }
.popup_extension .info.disabled { color: #aaa; }
.popup_extension .error { color: #bc3737; font-size: 13px; }
.popup_extension .error > span { display: block; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.hash_popup .top_add_ul { margin-bottom: 45px; }
.hash_popup .file_title { color: #757575; font-size: 15px; margin-bottom: 14px; }
.hash_popup .file_title .size { color: #757575; font-size: 14px; }
.hash_popup .blue_btn { display: block; float: left; color: #fff; background-color: #16a4fa; height: 30px; width: 90px; padding: 7px 0; text-align: center; border: none; cursor: pointer !important; font: 14px -webkit-system-font, "Helvetica Neue", Arial, sans-serif; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; }
.hash_popup .target_folder { display: block; color: #757575; margin-bottom: 8px; }
.hash_popup label { display: block; margin-bottom: 5px; }
.hash_popup input[type=text] { display: block; height: 30px; width: 100%; padding: 0 8px; cursor: text !important; margin-top: 0; font: 14px "Helvetica", Arial, sans-serif; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.hash_popup input[type=text].error_state { border-color: #bc3737; }
.hash_popup input[type=text].success_state { border-color: #299100; }
.hash_popup .group_button .left_button { margin-right: 0; }
.hash_popup .transparent_select { display: block; position: fixed; top: 0; bottom: 0; left: 0; right: 0; z-index: 500; }
.hash_popup .calculating { width: 140px; float: left; color: #9d9d9d; font-size: 13px; }
.hash_popup .calculating .compact-progress-line { margin-top: 5px; }
.hash_popup .error, .hash_popup .error_txt, .hash_popup .success_txt { color: #bc3737; font-size: 13px; line-height: 29px; }
.hash_popup .success_txt { color: #299100; }
.hash_popup .error_txt, .hash_popup .success_txt { position: absolute; line-height: 33px; }
.hash_popup input[type=text].inp_txt { padding: 0 8px; float: left; width: 409px; margin-top: 0; -webkit-user-select: auto; }
.hash_popup .delete label { position: static; }

.change_url .file_title { margin-top: 10px; }
.change_url .file_title .size { word-wrap: break-word; }
.change_url .group_button .left_button { margin-right: 10px; }
.change_url .error_message { margin-top: 10px; position: absolute; color: #db5155; overflow: hidden; text-overflow: ellipsis; max-width: 300px; }

.catch_text div { margin-bottom: 10px; }

.catch_text a { color: #16a4fa; text-decoration: underline; }

.alt_popup .add_url { height: 182px; width: 526px; left: 50%; top: 50%; margin-top: -91px; margin-left: -263px; }
.alt_popup .alt_text { font-size: 18px; margin-bottom: 28px; margin-top: 15px; color: #16a4fa; }
.alt_popup img { width: 89px; margin: 0 5px; position: relative; top: 5px; }

.scheduler { height: 240px; margin-top: -120px; }
.scheduler .modal_title { margin-bottom: 8px; }
.scheduler .note { margin-bottom: 15px; }
.scheduler .wrapper_days { overflow: hidden; margin-bottom: 11px; }
.scheduler .wrapper_days > div { margin-bottom: 17px; }
.scheduler .wrapper_days label { display: inline-block; margin-right: 10px; font-size: 14px; padding-left: 20px; margin-bottom: 0; }
.scheduler .wrapper_days input[type=checkbox] + label:before { left: 0; top: 2px; }
.scheduler .wrapper_times { margin-bottom: 84px; }
.scheduler .wrapper_times.disabled span { color: rgba(58, 57, 59, 0.5); }
.scheduler .wrapper_times > span { font-size: 14px; color: #444; display: block; float: left; margin-right: 10px; line-height: 30px; }
.scheduler .wrapper_times .choose { float: left; font-size: 14px; margin-right: 16px; min-width: 110px; }
.scheduler .wrapper_times .choose .wrapper_inselect { margin-right: 0; }
.scheduler .wrapper_times .transparent_select { display: block; position: fixed; top: 0; bottom: 0; left: 0; right: 0; z-index: 500; }
.scheduler .wrapper_times .wrap_numbers { position: absolute; top: -1px; left: -1px; right: -4px; bottom: 0; }
.scheduler .wrapper_times .wrap_numbers span { display: inline-block; padding: 0 1px; }
.scheduler .wrapper_times input[type=number]::-webkit-outer-spin-button, .scheduler .wrapper_times input[type=number]::-webkit-inner-spin-button { -webkit-appearance: none; }
.scheduler .wrapper_times input[type=number] { border: 1px solid #d4d4d4; -webkit-user-select: auto; width: 34px; height: 28px; padding: 0 8px; font: 14px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.scheduler .wrapper_times input[type=number]:focus { border-color: #15a1f8; }
.scheduler .error-message { left: 10px; bottom: 17px; z-index: 500; }

/*gifs*/
.share_modal { position: fixed; top: 0; left: 0; right: 0; bottom: 0; z-index: 5000; }
.share_modal.pre_loader .wrapper_popup { background-color: #fff; text-align: center; font-size: 24px; }
.share_modal.pre_loader .wrapper_popup img { margin: 0 auto 20px; }
.share_modal.pre_loader .wrapper_popup .absolute { position: absolute; left: 0; right: 0; top: 50%; margin-top: -74px; }
.share_modal.var1 .wrapper_popup .balloon { background-color: transparent; width: 224px; height: 185px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 0; -webkit-border-radius: 0; border-radius: 0; -moz-box-shadow: none; -webkit-box-shadow: none; box-shadow: none; }
.share_modal.var1 .wrapper_popup .balloon:before { display: none; }
.share_modal.var2 .wrapper_popup { padding: 15px 15px 95px; background-color: #4a87d8; }
.share_modal.var2 .wrapper_popup .social_buttons { bottom: 95px; }
.share_modal.var2 .wrapper_popup .text_info { color: #fefefe; font-size: 28px; line-height: 38px; position: absolute; bottom: 8px; left: 0; right: 0; text-align: center; max-width: 420px; margin: 0 auto; }
.share_modal.var3 .bottom_text { position: absolute; bottom: 17px; left: 15px; right: 15px; margin: 0 auto; color: #fefefe; font: 700 25px/30px "Roboto Condensed", "Helvetica Neue", "Arial", sans-serif; text-align: center; text-shadow: #000 1px 0, #000 1px 1px, #000 0 1px, #000 -1px 1px, #000 -1px 0, #000 -1px -1px, #000 0 -1px, #000 1px -1px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px, #000 0 0 3px; }
.share_modal .wrapper_modal { position: absolute; left: 50%; top: 50%; }
.share_modal .wrapper_modal .dont_label { margin-top: 6px; text-align: left; }
.share_modal .wrapper_modal .dont_label input[type=checkbox] + label { padding-left: 20px; color: #999; font-size: 13px; }
.share_modal .wrapper_modal .dont_label input[type=checkbox] + label:before { width: 12px; height: 12px; left: 0; background-position: -160px -44px; }
.share_modal .wrapper_modal .dont_label input[type=checkbox]:checked + label:before { background-position: -180px -44px; }
.share_modal .wrapper_popup { position: relative; -moz-box-shadow: 0 1px 9px 0 rgba(0, 0, 0, 0.35); -webkit-box-shadow: 0 1px 9px 0 rgba(0, 0, 0, 0.35); box-shadow: 0 1px 9px 0 rgba(0, 0, 0, 0.35); }
.share_modal .wrapper_popup img { display: block; }
.share_modal .wrapper_popup .close_button { position: absolute; top: 0; right: 0; cursor: pointer; background-color: rgba(0, 0, 0, 0.5); }
.share_modal .wrapper_popup .close_button:before { content: ''; display: block; width: 14px; height: 14px; position: absolute; top: 50%; left: 50%; margin-top: -7px; margin-left: -7px; background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 14 14"><title>Vector Smart Object171</title><path d="M19,6.41,17.59,5,12,10.59,6.41,5,5,6.41,10.59,12,5,17.59,6.41,19,12,13.41,17.59,19,19,17.59,13.41,12Z" transform="translate(-5 -5)" style="fill:#fff"/></svg>') no-repeat; }
.share_modal .wrapper_popup .social_buttons { display: none; position: absolute; bottom: 0; left: 0; right: 0; font-size: 0; text-align: center; }
.share_modal .wrapper_popup .social_buttons div { display: inline-block; width: 50px; height: 50px; margin-right: 10px; margin-bottom: 18px; cursor: pointer !important; }
.share_modal .wrapper_popup .social_buttons div:last-child { margin-right: 0; }
.share_modal .wrapper_popup .social_buttons .google { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><title>Vector Smart Object20</title><rect width="60" height="60" style="fill:#ea4335"/><path d="M35.87,26.88a15.19,15.19,0,0,1,.26,2.8c0,7.62-5.1,13-12.8,13a13.33,13.33,0,0,1,0-26.67,12.8,12.8,0,0,1,8.92,3.47l-3.76,3.76h0a7.3,7.3,0,0,0-5.16-2,8.12,8.12,0,0,0,0,16.24c4,0,6.71-2.28,7.27-5.42H23.33v-5.2Zm10,.83V23.54H42.5v4.17H38.33V31H42.5v4.17h3.33V31H50V27.71Z" style="fill:#fff"/></svg>') no-repeat; }
.share_modal .wrapper_popup .social_buttons .facebook { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><title>Vector Smart Object21</title><rect width="60" height="60" style="fill:#3a5897"/><path d="M39.29,22.91H33V18.82a1.68,1.68,0,0,1,1.73-1.89h4.41V10H33c-6.93,0-8.35,5.2-8.35,8.35v4.57H20.71V30h3.94V50H33V30h5.67Z" style="fill:#fff"/></svg>') no-repeat; }
.share_modal .wrapper_popup .social_buttons .tweeter { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 60 60"><title>Vector Smart Object22</title><rect width="60" height="60" style="fill:#55acee"/><path d="M46,19.25a10,10,0,0,1-3.75,1.12,7,7,0,0,0,2.87-3.88,14.46,14.46,0,0,1-4.25,1.75A6.38,6.38,0,0,0,36.12,16a6.87,6.87,0,0,0-6.62,7.12,7.13,7.13,0,0,0,.13,1.62A18.3,18.3,0,0,1,16,17.37,6.78,6.78,0,0,0,15.12,21,7.11,7.11,0,0,0,18,26.87,6,6,0,0,1,15,26v.13a7.16,7.16,0,0,0,5.25,7,7.38,7.38,0,0,1-1.75.25,4.25,4.25,0,0,1-1.25-.13,6.5,6.5,0,0,0,6.12,4.87,12.76,12.76,0,0,1-8.12,3A7.69,7.69,0,0,1,13.63,41a17.76,17.76,0,0,0,10.12,3.25c12.13,0,18.75-10.75,18.75-20.13v-.88A14.93,14.93,0,0,0,46,19.25Z" style="fill:#fff"/></svg>') no-repeat; }
.share_modal .wrapper_popup .balloon { position: absolute; margin-top: -50px; background-color: #fff; padding: 14px 32px; text-align: center; font: 700 26px/34px "Roboto Condensed", "Helvetica Neue", "Arial", sans-serif; -moz-border-radius: 45px; -webkit-border-radius: 45px; border-radius: 45px; -moz-box-shadow: 0 5px 7px 0 rgba(0, 0, 0, 0.35); -webkit-box-shadow: 0 5px 7px 0 rgba(0, 0, 0, 0.35); box-shadow: 0 5px 7px 0 rgba(0, 0, 0, 0.35); }
.share_modal .wrapper_popup .balloon:before { content: ''; display: block; width: 42px; height: 26px; position: absolute; top: 0; right: -8px; background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 42 26"><defs><style>.a{fill:#fff;}</style></defs><title>no_difference</title><polygon class="a" points="0 8.96 42 0 16.24 26 0 8.96"/></svg>') no-repeat; }

.macosx .single-file.is_trt .total { bottom: 11px; }

.macosx .single-file.with_note .popup .total { bottom: 109px; }

.macosx .simple-file .popup .total { bottom: 17px; }

.hash_popup .top_add_ul .title { font-weight: 600; }

.hash_popup .wrapper_inselect { width: 126px; }

.hash_popup input[type=text].inp_txt { width: 389px; }

/*popup: end*/
/*menu: start*/
.dropdown-settings { position: absolute; right: 48px; top: 48px; width: 200px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.dropdown-settings .border-block { width: 240px; display: block; position: absolute; overflow: hidden; background-color: #fff; -moz-border-radius: 6px; -webkit-border-radius: 6px; border-radius: 6px; -moz-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); -webkit-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); }
.dropdown-settings .border-block:first-child { padding-top: 6px; }
.dropdown-settings .border-block:last-child { padding-bottom: 6px; }
.dropdown-settings .rows-group { padding: 0; position: relative; }
.dropdown-settings .rows-group.disabled .menu-row { opacity: .5; }
.dropdown-settings .rows-group.disabled .menu-row:hover { cursor: default; background-color: transparent; }
.dropdown-settings .rows-group .menu-row { padding: 3px 20px; margin: 0; }
.dropdown-settings .rows-group .menu-row:hover { background-color: #e6f7fe; cursor: pointer; }
.dropdown-settings .rows-group .menu-row.shutdown { position: relative; }
.dropdown-settings .rows-group .menu-row.shutdown:before { content: ''; display: block; width: 11px; height: 10px; position: absolute; top: 7px; left: 5px; background-position: 0 -124px; }
.dropdown-settings .rows-group .menu-row.shutdown_group_opened { margin-bottom: 6px; }
.dropdown-settings .rows-group .menu-row.shutdown_group_opened:before, .dropdown-settings .rows-group .menu-row.shutdown_group_closed:before { content: ''; display: block; width: 7px; height: 5px; position: absolute; top: 9px; right: 12px; background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 7 4.32"><title>Vector Smart Object21</title><path d="M12.18,13.16,9.5,10.49,6.82,13.16,6,12.34l3.5-3.5,3.5,3.5Z" transform="translate(-6 -8.84)"/></svg>') no-repeat; }
.dropdown-settings .rows-group .menu-row.shutdown_group_closed:before { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 7 4.32"><title>Vector Smart Object21</title><path d="M6.82,8.84,9.5,11.51l2.68-2.67.82.82-3.5,3.5L6,9.66Z" transform="translate(-6 -8.84)"/></svg>') no-repeat; }
.dropdown-settings .rows-group .menu-row.new_notification { position: relative; }
.dropdown-settings .rows-group .menu-row.new_notification:before { content: ''; display: block; width: 8px; height: 8px; position: absolute; top: 7px; left: 7px; background-color: #40ca0a; -moz-border-radius: 100%; -webkit-border-radius: 100%; border-radius: 100%; }
.dropdown-settings .rows-group .shutdown_group { background-color: #f9f9f9; border: 1px solid #e6e6e6; margin-bottom: -7px; }
.dropdown-settings .rows-group hr { height: 1px; background-color: #e6e6e6; border: none; margin: 6px 0; }

/*menu: end*/
/*updates fdm: start*/
.wrapper_updates { background-color: #fff; padding: 15px 12px; position: absolute; top: 34px; left: 0; z-index: 2600; font-size: 13px; width: 218px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; -moz-border-radius: 6px; -webkit-border-radius: 6px; border-radius: 6px; -moz-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); -webkit-box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); box-shadow: 0 1px 10px 0 rgba(165, 165, 165, 0.57); }
.wrapper_updates:before { content: ''; display: block; width: 11px; height: 6px; background-position: 0 -216px; position: absolute; top: -6px; left: 15px; }
.wrapper_updates.error { background-color: #ffebeb; color: #8f272b; border: 1px solid rgba(143, 39, 43, 0.2); }
.wrapper_updates.error:before { background-position: -20px -216px; }
.wrapper_updates .btn { color: #fff; background-color: #17a2f7; height: 25px; padding: 5px 12px; margin: 12px auto 0; overflow: hidden; display: table; cursor: pointer; -moz-border-radius: 3px; -webkit-border-radius: 3px; border-radius: 3px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.wrapper_updates .btn.left { float: left; margin-right: 20px; margin-left: 16%; }
.wrapper_updates .btn.right { float: left; }
.wrapper_updates .info-time { width: 100%; font-size: 13px; margin-bottom: 5px; margin-top: 5px; }
.wrapper_updates .info-time .percents { display: block; float: left; }
.wrapper_updates .info-time .download-time { display: block; float: right; color: rgba(0, 0, 0, 0.7); }
.wrapper_updates .checking .compact-progress-line { -moz-background-size: 12px 12px; -o-background-size: 12px 12px; -webkit-background-size: 12px 12px; background-size: 12px 12px; }
.wrapper_updates .compact-progress-line { width: -webkit-calc(100% - 20px); width: calc(100% - 20px); float: left; }
.wrapper_updates .cancel_btn { display: block; float: left; width: 12px; height: 12px; background-position: 2px -232px; position: absolute; right: 11px; margin-top: -3px; }

/*updates fdm: end*/
/*settings: start*/
.button_back { display: none; position: relative; float: left; margin-top: 7px; }
.button_back:before { width: 9px; height: 14px; background-position: -99px -248px; top: 5px; margin-left: -6px; }

.settings_wrapper { display: none; position: absolute; left: 0; right: 0; top: 60px; bottom: 31px; background-color: #fff; z-index: 2500; }
.settings_wrapper .left_panel { width: 190px; height: 100%; padding: 24px 0; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.settings_wrapper .left_panel .title { font: 24px "Roboto Condensed Light", "Helvetica Neue", Arial, sans-serif; margin-bottom: 18px; margin-left: 22px; }
.settings_wrapper .left_panel ul li { display: block; font-size: 14px; color: #999; cursor: pointer; padding: 7px 0 7px 22px; }
.settings_wrapper .left_panel ul li.active { position: relative; cursor: default; }
.settings_wrapper .left_panel ul li.active:before { content: ''; display: block; width: 6px; height: 100%; position: absolute; top: 0; left: 0; background-color: #4e5764; }
.settings_wrapper .right_panel { height: 100%; padding: 24px 30px; overflow-y: auto; color: #303942; -webkit-flex: 2; flex: 2; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.settings_wrapper .right_panel .monitoring_tab, .settings_wrapper .right_panel .connection_tab, .settings_wrapper .right_panel .tum_tab { margin-bottom: 40px; }
.settings_wrapper .right_panel .general_tab { margin-bottom: 30px; }
.settings_wrapper .right_panel .group_title { border-bottom: 1px solid #eee; color: #4e5764; padding-bottom: 10px; margin-bottom: 18px; font: 24px "Roboto Condensed", "Helvetica Neue", Arial, sans-serif; }
.settings_wrapper .right_panel .tab_title { clear: both; color: #000; font-size: 14px; margin-bottom: 12px; }
.settings_wrapper .right_panel .sub_title { font-size: 13px; margin-bottom: 8px; }
.settings_wrapper .right_panel .search { background-color: #fff49d; }
.settings_wrapper .right_panel .float { float: left; }
.settings_wrapper .right_panel span { font-size: 12px; }
.settings_wrapper .right_panel a { font-size: 12px; }
.settings_wrapper .right_panel span.disable { color: rgba(58, 57, 59, 0.5); }
.settings_wrapper .right_panel .wrap_group { padding-left: 18px; margin-bottom: 26px; }
.settings_wrapper .right_panel input[type=checkbox] + label { display: table; margin-bottom: 13px; font-size: 13px; padding-left: 22px; }
.settings_wrapper .right_panel input[type=checkbox] + label:before { left: 0; width: 12px; height: 12px; background-position: 0 -44px; }
.settings_wrapper .right_panel input[type=checkbox]:checked + label:before { background-position: -40px -44px; }
.settings_wrapper .right_panel input[type=checkbox]:disabled + label:before { background-position: -60px -44px; }
.settings_wrapper .right_panel input[type=checkbox]:disabled:checked + label:before { background-position: -80px -44px; }
.settings_wrapper .right_panel input[type=text], .settings_wrapper .right_panel input[type=password] { width: 30px; height: 23px; padding: 0 2px; margin: 0 6px; text-align: center; font: 12px -webkit-system-font,  "Helvetica Neue", Arial, sans-serif; border: 1px solid #cfcfcf; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.settings_wrapper .right_panel input[type=text].simple_input, .settings_wrapper .right_panel input[type=password].simple_input { width: 100%; max-width: 108px; margin-left: 0; text-align: left; padding-left: 6px; border-color: #e5e5e5; }
.settings_wrapper .right_panel input[type=text]:disabled, .settings_wrapper .right_panel input[type=password]:disabled { border-color: #e3e3e3; color: #757575; background-color: #fff; cursor: default; }
.settings_wrapper .right_panel .margin_in_group { margin: 20px 0 0; }
.settings_wrapper .right_panel .margin_in_group .block_error { left: -100px; }
.settings_wrapper .right_panel .sub_group { margin-left: 25px; margin-top: 5px; float: none; clear: both; }
.settings_wrapper .right_panel .wrapper_group { float: left; }
.settings_wrapper .right_panel .wrapper_group input[type=checkbox] + label { display: block; float: left; }
.settings_wrapper .right_panel .monitors { display: block; }
.settings_wrapper .right_panel .monitors a { cursor: pointer !important; color: #18a3f7; text-decoration: underline; }
.settings_wrapper .right_panel input[type=text].default_folder_input { text-align: left; width: 350px; float: left; }
.settings_wrapper .right_panel .button_folder { height: 28px; width: 28px; float: left; cursor: pointer; border: none; margin-top: -3px; background-position: 6px 7px; }
.settings_wrapper .right_panel .clear { float: none; clear: both; }
.settings_wrapper .general_tab .wrapper_group { height: 17px; float: none; }
.settings_wrapper .general_tab .wrapper_group input[type=checkbox] + label { margin-bottom: 0; }
.settings_wrapper .general_tab .absolute { top: -5px; }
.settings_wrapper .floats { padding-bottom: 20px; }
.settings_wrapper .floats input[type=checkbox] + label { display: block; float: left; margin-right: 25px; position: relative; }
.settings_wrapper .info { display: block; margin-left: 35px; top: -7px; left: 100%; white-space: nowrap; color: #3a393b; font-size: 11px; position: absolute; }
.settings_wrapper .info:before { content: ''; display: block; position: absolute; left: -22px; top: 6px; width: 16px; height: 16px; background-position: 0 -267px; }
.settings_wrapper .info a { color: #18a3f7; text-decoration: underline; cursor: pointer !important; }
.settings_wrapper .spec_wrapper { position: relative; display: inline; }
.settings_wrapper .absolute { position: relative; top: -4px; float: left; }
.settings_wrapper .absolute.error .block_error { display: block; }
.settings_wrapper .absolute.error input[type=text] { border-color: #d2a9aa; }
.settings_wrapper .absolute > span { position: relative; top: 0; }
.settings_wrapper .absolute input[type=text] { margin-top: 1px; }
.settings_wrapper .absolute input[type=text].width60 { width: 60px; }
.settings_wrapper .absolute .block_error { display: none; }
.settings_wrapper button.associate { color: #fff; background-color: #00a8f3; border: none; display: block; float: none; clear: both; margin-top: 12px; margin-left: 22px; height: 22px; cursor: pointer !important; -moz-border-radius: 2px; -webkit-border-radius: 2px; border-radius: 2px; }
.settings_wrapper button.associate:disabled { opacity: 0.4; cursor: default !important; }
.settings_wrapper .grey_btn { color: #272e2c; background-color: #dfdedf; text-align: center; font: 13px -webkit-system-font, "Helvetica Neue", Arial, sans-serif; border: none; display: block; height: 22px; cursor: pointer !important; -moz-border-radius: 2px; -webkit-border-radius: 2px; border-radius: 2px; }
.settings_wrapper .relative .block_error, .settings_wrapper .wrap_input .block_error { display: none; }
.settings_wrapper .relative.error .block_error, .settings_wrapper .wrap_input.error .block_error { display: block; }
.settings_wrapper .relative.error input[type=text], .settings_wrapper .wrap_input.error input[type=text] { border-color: #d2a9aa; }
.settings_wrapper .wrap_group .margin_in_group { margin-top: 8px; }
.settings_wrapper .monitoring_tab .wrapper_group { float: none; clear: both; }
.settings_wrapper .monitoring_tab input[type=checkbox] + label { margin-bottom: 11px; }
.settings_wrapper .monitoring_tab input[type=text].value { text-align: left; }
.settings_wrapper .monitoring_tab input[type=radio] + label { font-size: 13px !important; }
.settings_wrapper .history_tab .sub_group input[type=checkbox] + label { margin-bottom: 0; }
.settings_wrapper .history_tab div { float: left; }
.settings_wrapper .history_tab .wrapper_group { float: none; }
.settings_wrapper .history_tab .wrap_group { padding-left: 0; }
.settings_wrapper .tum_tab table { width: 507px; table-layout: fixed; font-size: 15px; margin-left: 8px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.settings_wrapper .tum_tab table td { padding-left: 10px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.settings_wrapper .tum_tab table thead td { width: 120px; height: 12px; vertical-align: middle; font-size: 12px; }
.settings_wrapper .tum_tab table thead td:first-child { width: 160px; }
.settings_wrapper .tum_tab table tbody td { height: 40px; line-height: 14px; font-size: 12px; }
.settings_wrapper .tum_tab table tbody td:first-child { padding-right: 10px; padding-top: 2px; color: #303942; }
.settings_wrapper .tum_tab table tbody td.two_lines { padding-top: 0; }
.settings_wrapper .tum_tab table .wrapper_inselect { max-width: 110px; }
.settings_wrapper .tum_tab table .wrapper_inselect.disable { opacity: .6; }
.settings_wrapper .tum_tab table .wrapper_inselect input[type=text] { width: 45px; padding: 0 6px; }
.settings_wrapper .tum_tab .clear { margin-bottom: 26px; }
.settings_wrapper .downloads_tab .wrap_group, .settings_wrapper .connection_tab .wrap_group, .settings_wrapper .monitoring_tab .wrap_group { margin-bottom: 0; }
.settings_wrapper .downloads_tab .wrap_group.alt_group, .settings_wrapper .connection_tab .wrap_group.alt_group, .settings_wrapper .monitoring_tab .wrap_group.alt_group { margin-bottom: 22px; }
.settings_wrapper .downloads_tab input[type=radio], .settings_wrapper .connection_tab input[type=radio], .settings_wrapper .monitoring_tab input[type=radio] { display: none; }
.settings_wrapper .downloads_tab input[type=radio] + label, .settings_wrapper .connection_tab input[type=radio] + label, .settings_wrapper .monitoring_tab input[type=radio] + label { display: table; margin-bottom: 11px; font-size: 13px; padding-left: 18px; position: relative; }
.settings_wrapper .downloads_tab input[type=radio] + label:before, .settings_wrapper .connection_tab input[type=radio] + label:before, .settings_wrapper .monitoring_tab input[type=radio] + label:before { left: 0; top: 2px; width: 12px; height: 12px; background-position: 0 -64px; }
.settings_wrapper .downloads_tab input[type=radio]:checked + label:before, .settings_wrapper .connection_tab input[type=radio]:checked + label:before, .settings_wrapper .monitoring_tab input[type=radio]:checked + label:before { background-position: -20px -64px; }
.settings_wrapper .downloads_tab .wrap_group { margin-bottom: 26px; }
.settings_wrapper .connection_tab .wrap_fake_table { width: 100%; max-width: 530px; margin-top: 20px; font-size: 12px; min-height: 125px; }
.settings_wrapper .connection_tab .wrap_fake_table.disabled, .settings_wrapper .connection_tab .wrap_fake_table .disabled { color: rgba(58, 57, 59, 0.5); }
.settings_wrapper .connection_tab .wrap_fake_table.disabled input[type=text], .settings_wrapper .connection_tab .wrap_fake_table.disabled input[type=password], .settings_wrapper .connection_tab .wrap_fake_table .disabled input[type=text], .settings_wrapper .connection_tab .wrap_fake_table .disabled input[type=password] { cursor: default !important; }
.settings_wrapper .connection_tab .wrap_fake_table .column { float: left; }
.settings_wrapper .connection_tab .wrap_fake_table .column > div { height: 20px; margin-bottom: 10px; margin-right: 10px; }
.settings_wrapper .connection_tab .wrap_fake_table .first_col > div:first-child { margin-top: 28px; }
.settings_wrapper .connection_tab .wrap_fake_table .second_col > div:before { content: ':'; position: absolute; left: 148px; margin-top: 3px; }
.settings_wrapper .connection_tab .wrap_fake_table .second_col > div { position: relative; }
.settings_wrapper .connection_tab .wrap_fake_table .second_col > div:first-child:before { display: none; }
.settings_wrapper .connection_tab .wrap_fake_table .second_col > div:first-child, .settings_wrapper .connection_tab .wrap_fake_table .third_col > div:first-child, .settings_wrapper .connection_tab .wrap_fake_table .fourth_col > div:first-child, .settings_wrapper .connection_tab .wrap_fake_table .fifth_col > div:first-child { margin-bottom: 4px; }
.settings_wrapper .connection_tab .wrap_fake_table .absolute { top: 0; }
.settings_wrapper .connection_tab .wrap_fake_table input[type=text], .settings_wrapper .connection_tab .wrap_fake_table input[type=password] { margin: 0; width: 145px; text-align: left; padding: 0 8px; }
.settings_wrapper .connection_tab .wrap_fake_table input[type=text].short, .settings_wrapper .connection_tab .wrap_fake_table input[type=password].short { width: 55px; }
.settings_wrapper .connection_tab .wrap_fake_table input[type=text].medium, .settings_wrapper .connection_tab .wrap_fake_table input[type=password].medium { width: 118px; }
.settings_wrapper .advanced_tab { overflow: hidden; }
.settings_wrapper .advanced_tab .tab_title { margin-bottom: 9px; }
.settings_wrapper .advanced_tab .wrapper_group { float: none; clear: both; min-height: 14px; }
.settings_wrapper .advanced_tab .wrap_group { padding-top: 3px; }
.settings_wrapper .advanced_tab .power input[type=checkbox] + label { float: none; }
.settings_wrapper .language_block .choose { max-width: 160px; margin-left: 18px; margin-bottom: 20px; }
.settings_wrapper .language_block .transparent_select { position: fixed; top: 0; bottom: 0; left: 0; right: 0; z-index: 500; }
.settings_wrapper .language_block .wrapper_inselect { padding: 3px 5px; z-index: 600; }
.settings_wrapper .language_block .wrapper_inselect.top_position .list { top: auto; bottom: 25px; }
.settings_wrapper .language_block .wrapper_inselect > span:before { left: 1px; }
.settings_wrapper .language_block .wrapper_inselect .list { overflow-y: auto; }
.settings_wrapper .language_block .lang_l span { position: relative; padding-left: 25px; }
.settings_wrapper .language_block .lang_l span:before { content: ''; display: block; width: 18px; height: 10px; position: absolute; top: 2px; left: 0; }
.settings_wrapper .language_block .lang_l span.en:before { background-position: 0 0; }
.settings_wrapper .language_block .lang_l span.es:before { background-position: 0 -10px; }
.settings_wrapper .language_block .lang_l span.de:before { background-position: 0 -20px; }
.settings_wrapper .language_block .lang_l span.fr:before { background-position: 0 -30px; }
.settings_wrapper .language_block .lang_l span.it:before { background-position: 0 -50px; }
.settings_wrapper .language_block .lang_l span.ro:before { background-position: 0 -60px; }
.settings_wrapper .language_block .lang_l span.pl:before { background-position: 0 -70px; }
.settings_wrapper .language_block .lang_l span.nl:before { background-position: 0 -80px; }
.settings_wrapper .language_block .lang_l span.sv:before { background-position: 0 -90px; }
.settings_wrapper .language_block .lang_l span.da:before { background-position: 0 -100px; }
.settings_wrapper .language_block .lang_l span.ru:before { background-position: 0 -110px; }
.settings_wrapper .language_block .lang_l span.el:before { background-position: 0 -120px; }
.settings_wrapper .language_block .lang_l span.zh:before { background-position: 0 -130px; }
.settings_wrapper .language_block .lang_l span.zh_TW:before { background-position: 0 -130px; }
.settings_wrapper .language_block .lang_l span.sl:before { background-position: 0 -140px; }
.settings_wrapper .language_block .lang_l span.ja:before { background-position: 0 -150px; }
.settings_wrapper .language_block .lang_l span.tr:before { background-position: 0 -160px; }
.settings_wrapper .language_block .lang_l span.ar:before { background-position: 0 -170px; }
.settings_wrapper .language_block .lang_l span.id:before { background-position: 0 -180px; }
.settings_wrapper .language_block .lang_l span.vi:before { background-position: 0 -190px; }
.settings_wrapper .language_block .lang_l span.pt:before { background-position: 0 -200px; }

.show_settings .add-download, .show_settings .wrap_actions, .show_settings .action_folder, .show_settings .sort { display: none !important; }
.show_settings .button_back { display: block; }
.show_settings .settings_wrapper { display: -webkit-flex; display: flex; }

.right_panel .wrapper_inselect { position: relative; width: 100%; height: 24px; line-height: normal; margin: 0; float: none; border: 1px solid #e5e5e5; padding: 5px; z-index: inherit; background: url("../v2_images/arrow_combobox_small.svg") no-repeat right 9px center; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.right_panel .wrapper_inselect.top_position .list { top: auto; bottom: 25px; }
.right_panel .wrapper_inselect .list { position: absolute; top: 23px; left: -1px; z-index: 5; width: -webkit-calc(100% + 2px); width: calc(100% + 2px); background-color: #fff; overflow: hidden; font-size: 14px; border: 1px solid #e5e5e5; -moz-border-radius: 5px; -webkit-border-radius: 5px; border-radius: 5px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.right_panel .wrapper_inselect .list div { height: 18px; font-size: 12px; padding-left: 6px; padding-top: 2px; -moz-box-sizing: border-box; -webkit-box-sizing: border-box; box-sizing: border-box; }
.right_panel .wrapper_inselect .list div:hover { cursor: pointer; background-color: #b9e4fd; }
.right_panel .wrapper_inselect .list div.unlim { position: relative; padding: 5px 0 5px 6px; height: auto; margin-top: 2px; }
.right_panel .wrapper_inselect .list div.unlim:before { content: ''; display: block; position: absolute; top: 0; left: 6px; right: 16px; height: 1px; background-color: #e5e5e5; }
.right_panel .wrapper_inselect .wrap_input { position: absolute; left: -7px; top: -1px; height: -webkit-calc(100% + 2px); height: calc(100% + 2px); }
.right_panel .wrapper_inselect .wrap_input input[type=text] { border: 1px solid #39a7d6; height: 100%; float: left; margin-right: 3px; text-align: left; outline: none; -moz-border-radius: 5px 0 0 5px; -webkit-border-radius: 5px; border-radius: 5px 0 0 5px; }
.right_panel .wrapper_inselect .wrap_input div { float: left; margin-top: 6px; }

/*settings: end*/
/*bottom panel, progress tab: start */
.progress_downloaded { height: 10px; position: absolute; background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 10 10"><defs><style>.cls-1{fill:#8bd0fb;}.cls-2{fill:#16a4fa;}.cls-3{fill:none;}</style></defs><title>2</title><path class="cls-1" d="M9,1H1V9H9Z"/><path class="cls-2" d="M8,2V8H2V2H8M9,1H1V9H9Z"/><rect class="cls-3" width="10" height="10"/></svg>'); }

.progress_background { background: url('data:image/svg+xml;utf8,<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 10 10"><defs><style>.cls-1{fill:#d7e8ed;}.cls-2{fill:none;}</style></defs><path class="cls-1" d="M8,2V8H2V2H8M9,1H1V9H9Z"/><rect class="cls-2" width="10" height="10"/></svg>'); position: absolute; left: 50%; bottom: 4px; }

.progress_background_wrapper { width: calc(100% - 1px); height: calc(100% - 1px); position: absolute; }

/*bottom panel, progress tab: end */
/*modal test*/
.modal_test .modal { width: 542px; height: 200px; position: absolute; top: 50%; left: 50%; margin-top: -100px; margin-left: -271px; background-color: #f5f5f5; border: none; -moz-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); -webkit-box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); box-shadow: 0 0 12px 0 rgba(64, 64, 64, 0.45); }
.modal_test .close_button:hover { background: url('data:image/svg+xml;charset=utf-8,<svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 36 36"><line x1="13.41" y1="13.41" x2="22.59" y2="22.59" style="fill:none;stroke:#b2b2b2;stroke-miterlimit:10"/><rect x="5.93" y="11.86" width="12.99" height="1.15" transform="translate(0.42 18) rotate(-45)" style="fill:#b2b2b2"/><rect width="36" height="36" style="fill:none"/></svg>') no-repeat center; }

/*# sourceMappingURL=style.css.map */
