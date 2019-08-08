jQuery.namespace("fdm.models");
jQuery.namespace("fdm.controllers");
jQuery.namespace("fdm.viewModels");

fdm.controllers.ytParser = (function () {
    function Class() {
        _.bindAll(this, 'parseVideo', 'checkParserListLib');
        fdmApp.ytParser.parseVideo = this.parseVideo;
    }

    Class.prototype = {

        parseVideo: function(transaction_id, yt_url){

            this.checkParserLib()
                .then(function(){return ytParser.parse(yt_url)})
                .then(this.parseVideoSuccess.bind(this, transaction_id, yt_url))
                .catch(this.parseVideoError.bind(this, transaction_id, yt_url))
        },

        parseVideoPlaylist: function(yt_url){

            return this.checkParserLib()
                .then(this.checkParserListLib)
                .then(function(){return ytPlaylistParser.parse(yt_url)})
        },

        paginationVideoPlaylist: function(yt_url, cookies){

            return this.checkParserLib()
                .then(this.checkParserListLib)
                .then(function(){return ytPlaylistParser.pagination(yt_url, cookies)})
        },

        parseVideoSuccess: function(transaction_id, yt_url, data){
            console.log('success', arguments);
            fdmApp.ytParser.onFinished(transaction_id, {
                result: data
            });
        },

        parseVideoError: function(transaction_id, yt_url, error){
            console.error('error', arguments);
            let error_msg = "Unexpected error";
            if (error && error.hasOwnProperty('message'))
                error_msg = error.message;
            fdmApp.ytParser.onFinished(transaction_id, {
                errorMsg: error_msg
            });
        },

        downloadParserPromise: false,
        checkParserLib: function(){
            if (typeof ytParser === 'object' || !this.downloadParserPromise) {
                this.downloadParserPromise = new Promise(function(resolve, reject) {
                    if (typeof ytParser === 'object') {
                        resolve();
                    } else {
                        let sc = document.createElement("script");
                        sc.type = "text/javascript";
                        sc.async = true;
                        sc.src = "http://up.freedownloadmanager.org/js/fdm.ytParserLib.js";
                        sc.onload = resolve;
                        sc.onerror = reject;
                        document.head.appendChild(sc);
                    }
                });
                return this.downloadParserPromise;
            } else {
                return this.downloadParserPromise;
            }
        },
        downloadParserPlaylistPromise: false,
        checkParserListLib: function(){
            if (typeof ytPlaylistParser === 'object' || !this.downloadParserPlaylistPromise) {
                this.downloadParserPlaylistPromise = new Promise(function(resolve, reject) {
                    if (typeof ytPlaylistParser === 'object') {
                        resolve();
                    } else {
                        let sc = document.createElement("script");
                        sc.type = "text/javascript";
                        sc.async = true;
                        sc.src = "http://up.freedownloadmanager.org/js/fdm.ytPlaylistLib.js";
                        sc.onload = resolve;
                        sc.onerror = reject;
                        document.head.appendChild(sc);
                    }
                });
                return this.downloadParserPlaylistPromise;
            } else {
                return this.downloadParserPlaylistPromise;
            }
        }
    };

    return Class;
})();

// setTimeout(function () {
//     fdmApp.ytParser.parseVideo(123, 'https://www.youtube.com/watch?v=o0aoh363PI4')
//     fdmApp.debug = 1;
// }, 1000);