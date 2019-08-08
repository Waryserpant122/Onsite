jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");


fdm.controllers.Sharer = (function () {
	function Class() {

		fdm.models.Sharer = Backbone.Model.extend({
			defaults: {
                // uuid: false,
                time_share_flag_modified: 0,
                shareFunc: 0,

                waitingAlreadyStarted: false,

                dialogOpened: false,
                gif: false,
                innerHtml: false,
                style: {},
                showPreLoader: false,
                userMenuClick: false,
                hideButton: false,
                msgVariant: 0
			}

		});
		this.model = new fdm.models.Sharer;

        fdmApp.system.getUuid(function (uuid) {

            this.model.set(uuid);
        }.bind(this));

        this.model.set(app.appViewManager.getSharerState());
	}

	Class.prototype = {

        showDialog: function (user_click) {

            user_click = user_click || false;

            console.log('showSharerDialog');

            var sc = document.createElement("script");
            sc.type = "text/javascript";
            sc.async = true;
            sc.src = "http://up.freedownloadmanager.org/sharer.php?get_sharer=1&uuid=" + encodeURIComponent(this.model.get('uuid'));
            sc.onload = this.setJsonpArgs.bind(this, user_click);
            sc.onerror = function () {

                if (user_click){

                    global_jsonp_value = {
                        gif:"http://static.freedownloadmanager.org/gifs/please1.gif",
                        style:{
                            wrapper_popup:{
                                width:"386px",
                                height:"345px",
                                marginTop:"-172px",
                                marginLeft:"-193px"
                            },
                            balloon:{
                                left:"-150px",
                                top:"145px"
                            }
                        },
                        innerHtml:"Please tell your <br \/> friends about FDM!"
                    };
                    this.setJsonpArgs(user_click);
                }
            }.bind(this);
            document.head.appendChild(sc);

            fdmApp.sharer.dialogIsShown();
        },

		testSharerDialog: function (user_click, gif_id, message_id, dialog_id) {

            user_click = user_click || false;

            console.log('showSharerDialog');

            var sc = document.createElement("script");
            sc.type = "text/javascript";
            sc.async = true;
            sc.src = "http://up.freedownloadmanager.org/sharer.php?get_sharer=1&uuid=" + encodeURIComponent(this.model.get('uuid'))
                + '&gif_id=' + gif_id + '&message_id=' + message_id + '&dialog_id=' + dialog_id;
            sc.onload = this.setJsonpArgs.bind(this, user_click);
            sc.onerror = function () {

                if (user_click){

                    global_jsonp_value = {
                        gif:"http://static.freedownloadmanager.org/gifs/please1.gif",
                        style:{
                            wrapper_popup:{
                                width:"386px",
                                height:"345px",
                                marginTop:"-172px",
                                marginLeft:"-193px"
                            },
                            balloon:{
                                left:"-150px",
                                top:"145px"
                            }
                        },
                        innerHtml:"Please tell your <br \/> friends about FDM!"
                    };
                    this.setJsonpArgs(user_click);
                }
            }.bind(this);
            document.head.appendChild(sc);

            fdmApp.sharer.dialogIsShown();
        },

		setJsonpArgs: function (user_click) {

			var changes = global_jsonp_value;

            if (user_click){

                changes.dialogOpened = true;
                changes.showPreLoader = true;

                this.model.set(changes);
            }

            var loader = new Image();
            loader.src = changes.gif;
            loader.onload = function () {

                if (user_click)
                    changes.dialogOpened = this.model.get('dialogOpened');
                else
                    changes.dialogOpened = true;

                changes.showPreLoader = false;

                this.model.set(changes);

            }.bind(this);
        },

		share: function (type) {

            fdmApp.system.browseUrl('http://up.freedownloadmanager.org/sharer.php?share=1&type=' + encodeURIComponent(type)
                + '&uuid=' + encodeURIComponent(this.model.get('uuid')));
        },

        needClosedDialog: function () {

	        if (this.model.get('dialogOpened'))
	            this.close();
        },

        close: function () {

			this.sendSharerStat({action: 'close'});
            this.model.set(this.model.defaults);

            var sc = document.createElement("script");
            sc.type = "text/javascript";
            sc.async = true;
            sc.src = "http://up.freedownloadmanager.org/sharer.php?close_sharer=1&uuid=" + encodeURIComponent(this.model.get('uuid'));
            document.head.appendChild(sc);

            if (this.model.get('doNotShowAgain'))
                app.appViewManager.setSharerState('doNotShowAgain', true);
        },

		sendSharerStat: function (json) {

			// TODO: send stat
        },

	    showSharerFromMenu: function () {

            app.controllers.adsModal.showFromMenu();
	        this.model.set({
                userMenuClick: true
            });

	        this.showDialog(true);
        }
	};

	return Class;
})();

