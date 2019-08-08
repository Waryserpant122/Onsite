 youtubeChannelParser = {
    youtube_channel_helper_url: 'http://up.freedownloadmanager.org/js/youtube-channel-helper.js',
    all_videos: [],
    videos_max: 100, //max quantity of videoclips for download
    delay: 1000, //pause between requests to YouTube
    cookies: '',
    channel_name: '',
    channel_icon_url: '',
    videosObj: {},
    stop_createYoutubeVideosList: false,
    
    //Get info about videos of one page
     getVideosInfoPage: function (youtubeChannelPageUrl, cookies, getVideosInfoFinishCallback) {
        //Code for case when youtube-channel-helper.js is remote
        //Download YouTube channel parsing methods  
        var script = document.createElement('script');
        script.src = this.youtube_channel_helper_url;
        document.body.appendChild(script);      
        script.onload = function() {            
            //add methods from youtubeChannelHelper
            this.__proto__ = youtubeChannelHelper;
                      
            this.downloadPage(youtubeChannelPageUrl, cookies, this.extractVideosInfo.bind(this), getVideosInfoFinishCallback);        
        }.bind(this);            

        //Code for case when youtube-channel-helper.js is local 
        /*
        this.__proto__ = youtubeChannelHelper;                      
        this.downloadPage(youtubeChannelPageUrl, cookies, this.extractVideosInfo.bind(this), getVideosInfoFinishCallback);        
        */
    },
    
    downloadPage: function (url, cookies, downloadFinishCallback, getVideosInfoFinishCallback) {
        // download pages using core --------------------------------------------------- BEGIN --------------
        if (fdmApp.isFake) {
            return this.downloadPageFake(url, cookies, downloadFinishCallback, getVideosInfoFinishCallback);
        }
        
console.log('url: ' + url);  
//console.log('cookies TRANSMITTED: ' + cookies); 
        var id = fdmApp.httpRequests.create(url);
        if (cookies.lenght > 0) {
            fdmApp.httpRequests.setCookies(id, cookies);
        } 

        
        //function onFinishedCallback(id) {
        var onFinishedCallback = function(callback_id) {
            if (callback_id === id) {
console.log('httpRequests.onFinished TRIGGERED');             
                var ok = fdmApp.httpRequests.succeeded(id);
console.log('httpRequests.succeeded: ' + ok);            
                if (ok) {
                    response_content = fdmApp.httpRequests.data(id);
//console.log('response_content: ' + response_content);                
                    cookies = fdmApp.httpRequests.cookies(id);
console.log('received cookies: ' + cookies);                
                    downloadFinishCallback(response_content, cookies, getVideosInfoFinishCallback);
                }

                fdmApp.httpRequests.close(id);

                fdmApp.httpRequests.removeEventListener('onFinished', onFinishedCallback);
            }
        };
                
        fdmApp.httpRequests.addEventListener('onFinished', onFinishedCallback);
        
        fdmApp.httpRequests.start(id);
console.log('httpRequests.start EXECUTED');  
        
        // download pages using core --------------------------------------------------- END --------------      
    },
    downloadPageFake: function (url, cookies, downloadFinishCallback, getVideosInfoFinishCallback) {

        $.ajax({
            url: 'http://vi-dn.infcdn.net/test_proxy.php',
            data: {url: url},
            success: function(body){
                downloadFinishCallback(body, cookies, getVideosInfoFinishCallback);
            }
        })
    }
};    