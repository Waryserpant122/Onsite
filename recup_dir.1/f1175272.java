jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.controllers.AdsModal = (function () {
    function Class() {

        _.bindAll(this, 'waitingMouseMove', 'waitingStart', 'startWaitingListener', 'stopWaitingListener',
            'checkWaitingStart', 'onSettingsUpdated', 'waitingStop');

        fdm.models.AdsModal = Backbone.Model.extend({
            defaults: {

                // uuid: false,
                showShareDialog: false,
                shareFunc: 0,

                waitingStartTime: 0,
                waitingAlreadyStarted: false,

            }

        });
        this.model = new fdm.models.AdsModal;

        this.model.set({timeStart: (+ new Date)});

        this.updateShowShareDialog();

        this.model.set(app.appViewManager.getSharerState());
    }

    Class.prototype = {

        updateShowShareDialog: function () {

            var info = fdmApp.settings['update-server-provided-info'];

            if (typeof info == 'string')
                info = JSON.parse(info);

            this.model.set(info);

            this.onSettingsUpdated();
        },

        showModalDialog: function () {

            var fn = this.model.get('shareFunc');

            if (fn === 2) {
                if (!app.controllers.settings.model.get('settings')['cgbBundleDontShowAgain']
                    && app.controllers.settings.model.get('settings')['show-special-offers'])
                    app.controllers.bundles.showDialog();
            } else if (fn === 1) {
                app.controllers.sharer.showDialog();
            } else if (fn === 3) {
                if (app.controllers.settings.model.get('settings')['show-special-offers']) {
                    app.controllers.bundles.showAllPurposeDialogAds();
                }
            }

            fdmApp.sharer.dialogIsShown();
            this.model.set({showShareDialog: false});
        },

        onSettingsUpdated: function () {

            console.log('onSettingsUpdated');

            this.checkWaitingStart();
        },

        checkWaitingStart: function () {

            if (this.model.get('waitingAlreadyStarted'))
                return;
            if (!this.model.get('showShareDialog'))
                return;
            if (this.model.get('doNotShowAgain'))
                return;

            var time_start = this.model.get('timeStart');

            var min_time = 10 * 1000;// 10 min

            if ((+ new Date) - time_start < min_time)
            {
                var time = min_time - ((+ new Date) - time_start);

                console.log('< 10 min');
                setTimeout(this.checkWaitingStart, time );
                return;
            }

            if (this.model.get('shareFunc') === 3 && app.controllers.downloads.collections.downloads.length < 1) {
                console.log('downloads.length < 1');
                setTimeout(this.checkWaitingStart, 10 * 60 * 1000);
                return;
            }

            if (app.controllers.downloads.collections.downloads.length < 3)
            {
                console.log('downloads.length < 3');
                setTimeout(this.checkWaitingStart, 10 * 60 * 1000);
                return;
            }

            console.log('waitingStart timeout');
            setTimeout(this.waitingStart, 2000);
        },

        waitingStart: function () {

            console.log('waitingStart');

            this.model.set({
                waitingStartTime: (+ new Date),
                waitingAlreadyStarted: true
            });

            this.startWaitingListener();
            setTimeout(this.waitingStop, 10*60*1000);
        },

        waitingStop: function () {

            console.log('waitingStop');

            this.model.set({
                waitingStartTime: 0,
                waitingAlreadyStarted: false
            });

            this.stopWaitingListener();

            setTimeout(this.checkWaitingStart, 10*60*1000)
        },

        startWaitingListener: function () {

            if ((+ new Date) - this.model.get('waitingStartTime') > 30 * 60 * 1000) { // 30 min

                this.waitingStop();
                return;
            }

            console.log('startWaitingListener');
            document.body.addEventListener('mousemove', this.waitingMouseMove);
        },

        stopWaitingListener: function () {

            console.log('stopWaitingListener');
            document.body.removeEventListener('mousemove', this.waitingMouseMove);
        },

        waitingMouseMove: function () {

            console.log('waitingMouseMove');

            this.stopWaitingListener();

            if (document.activeElement && document.activeElement.nodeName == 'INPUT' || app.getActiveWindow() != 'main'){

                console.log('waitingMouseMove add interval');
                setTimeout(this.startWaitingListener, 10000);
            }
            else{

                this.showModalDialog();
            }
        },

        showFromMenu: function () {

            this.model.set({
                showShareDialog: false
            });
        }
    };

    return Class;
})();

