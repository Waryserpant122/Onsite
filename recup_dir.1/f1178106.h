
						if (years > 0 || weeks >= 52){
							time_type = 'years';
						}
					}
				}
			}
		}

		switch (time_type){
			case 'seconds':

				text = __("%ns", this.roundUp(seconds));
				break;
			case 'minutes':

				var s = seconds % 60;
				text = __("%nm", this.roundUp(minutes)) + (s > 0 ? ' ' + __("%ns", s) : '');
				break;
			case 'hours':

				if (hours > 23)
					hours = 23;
				if (hours < 1)
					hours = 1;

				var m = minutes % 60;
				text = __("%nh", hours) + (m > 0 ? ' ' + __("%nm", m) : '');
				break;
			case 'days':

				var h = hours % 24;
				text = __("%nd", days) + (h > 0 ? ' ' + __('%nh', h) : '');

				break;
			case 'weeks':

				var d = days % 7;
				text = __("%nw", weeks) + (d > 0 ? ' ' + __('%nd', d): '');

				break;
			case 'years':

				var w = weeks % 52;
				text = __("%ny", years) + (w > 0 ? ' ' + __('%nw', w) : '');

				break;
		}

		return text;

		/*
		if(seconds > 86400)
		{
			var days = Math.floor(seconds/86400);
			text = days == 1 ? "1 day" : days + " days";
		} else
		if(seconds > 60*10)
		{
			var hours = Math.floor(seconds / (60*60));
			var minutes = Math.floor ((seconds - hours*60*60)/60);
			if(minutes < 0) minutes = 0;
			if(hours != 0)
				text = hours + "h ";
			minutes = this.roundUp(minutes);
			if(minutes != 0)
				text += minutes + "m";
		} else
		{
			var minutes = Math.floor(seconds/60);
			var s = seconds - minutes*60;
			if(s < 0) s = 0;
			s = this.roundUp(s);
			if(minutes != 0)
			{
				text = minutes + "m ";
				if(s != 0)
					text += s + "s";
			}
			else
			{
				if(valueUnwrapped < 1500)
					text = "1s";
				else if (valueUnwrapped < 5000)
					text = "5s";
				else
					text = s + "s";
			}
		}

		return text;
		*/
	}
};

fdm.sizeUtils = {
    getSizeText: function(bytes, sizePow){

        var sizeValue = bytes/Math.pow(fdmApp.bytesInKB, sizePow);

		if (sizeValue < 0.01)
			sizeValue = 0;
        // round to 3 significant digits
        //http://www.quora.com/Google-Sheets/How-can-I-round-to-x-significant-digits
        var sizeText = fdm.fileUtils.fileSizeIECUnitless(sizeValue);

        if (sizePow != 0){

            if (sizeText >= 1000)
                sizeText = sizeText.toPrecision(4);
            else{
                sizeText = fdm.Math.roundPrecision(sizeText, 3);
                sizeText = sizeText.toPrecision(3);
            }
        }
        return sizeText;

    },
    bytesAsText: function(bytes) {

        var sizePow = Math.log(bytes)/Math.log(fdmApp.bytesInKB) | 0;
        var sizeText = fdm.sizeUtils.getSizeText(bytes, sizePow);

		if (sizeText >= fdmApp.bytesInKB){
			sizeText = 1;
			sizePow++;
		}

        var units = fdm.fileUtils.unitNameByPow(sizePow);

        return sizeText + " " + units;
        // return __('%1 ' + units, sizeText);
    },
    bytesAsTextObj: function(bytes) {

        var sizePow = Math.log(bytes)/Math.log(fdmApp.bytesInKB) | 0;
        var sizeText = fdm.sizeUtils.getSizeText(bytes, sizePow);

		if (sizeText >= fdmApp.bytesInKB){
			sizeText = 1;
			sizePow++;
		}

        var units = fdm.fileUtils.unitNameByPow(sizePow);

        return {
			size: sizeText,
			units: units
		};
    },
	allBytesAsText: function(done, all) {

		var d = this.bytesAsTextObj(done);

		if (all < 0)
			return d.size + " " + d.units;

		var a = this.bytesAsTextObj(all);

		if (d.units == a.units && d.size == a.size)
			return d.size + " " + d.units;

		if (d.units == a.units)
			return __('%1 of %2', [d.size, a.size + " " + d.units]);
			// return __('%1 of %2', [d.size, __('%1 ' + d.units, a.size)]);

		return __('%1 of %2', [ d.size + " " + d.units, a.size + " " + a.units ]);
		// return __('%1 of %2', [ __('%1 ' + d.units, d.size) , __('%1 ' + a.units, a.size) ]);
    },
    byteProgressAsText: function(done, all){

		if (done == 0 && all < 0)
			return "0 B / \u2014";

		if (all < 0 || !all)
			all = 0;

		if (done == 0 && all == 0)
			return "0 B";

        var ln=Math.log, d = fdmApp.bytesInKB;
        var maxPow = ln(all)/ln(d)|0;
        var base = Math.pow(d, maxPow);
        //var resultDone = fdm.Math.round(done / base, 3);
        //var resultAll = fdm.Math.round(all / base, 3);
		var resultDone = parseFloat(fdm.sizeUtils.getSizeText(done, maxPow));
		var resultAll = parseFloat(fdm.sizeUtils.getSizeText(all, maxPow));

		if (resultAll >= d){
			maxPow++;
			base = Math.pow(d, maxPow);
			//resultDone = fdm.Math.round(done / base, 3);
			//resultAll = fdm.Math.round(all / base, 3);
			resultDone = parseFloat(fdm.sizeUtils.getSizeText(done, maxPow));
			resultAll = parseFloat(fdm.sizeUtils.getSizeText(all, maxPow));
		}

        var decimalPlaces = fdm.Math.decimalPlaces(resultAll, 3);
        var resultDoneText = "";
        if(resultDone == 0 || maxPow == 0){
            resultDoneText = resultDone + "";
        }
        else if(resultDone < 0.01){
            resultDoneText = "0";
        }
        else{
            resultDoneText = resultDone.toFixed(decimalPlaces)
        }

        var resultText = ((resultDone < resultAll) ? resultDoneText + " / " : "") +
            resultAll.toFixed(decimalPlaces) +
            " " + fdm.fileUtils.unitNameByPow(maxPow);
        // var resultText = ((resultDone < resultAll) ? resultDoneText + " / " : "") +
        //     __('%1 ' + fdm.fileUtils.unitNameByPow(maxPow), resultAll.toFixed(decimalPlaces));
        // If total size is unknown, then show downloaded bytes without total size.
        if (resultDone >= 0 && resultAll == 0)
        {
            maxPow = ln(done)/ln(d)|0;
            base = Math.pow(d, maxPow);
            resultDone = fdm.Math.round(done / base, 3);
            decimalPlaces = fdm.Math.decimalPlaces(resultDone, 3);

            resultDoneText = maxPow == 0 ? resultDone + "" : resultDone.toFixed(decimalPlaces);
            resultText = resultDoneText + " " +  fdm.fileUtils.unitNameByPow(maxPow) + " / \u2014";// \u2014 long dash code symbol
            // resultText = __("%1 " +  fdm.fileUtils.unitNameByPow(maxPow), resultDoneText) + " / \u2014";// \u2014 long dash code symbol
        }
        return resultText;
    }
};

fdm.urlUtils = {
	isValidTrtUrl: function(sUrl)
	{
		var nColonPosition = sUrl.indexOf(":");
		if (nColonPosition != 1 && nColonPosition != -1)
			return false;

		var sIllegalPathSymbols = "*?\"<>|";
		for (var i = 0; i < sIllegalPathSymbols.length; i++) {
			var sChar = sIllegalPathSymbols.substr(i, 1);
			if (sUrl.indexOf(sChar) != -1)
				return false;
		}
		return true;
		
	},
    
    //Check if url like this:
    //https://www.youtube.com/user/HAMAHAtrader/videos
    //or like this:
    //https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ/videos
    isYoutubeChannelVideosUrl: function(url)
    {
        if (url && url.length < 10 || !this.isValidURL_2(url)) {
            return false;
        }

        var urlObj = new URL(url);
        var hostname = urlObj.hostname;
        var protocol = urlObj.protocol;
        
        if (protocol !== 'https:') {
            return false;
        }
        
        if (hostname.endsWith('youtube.com')) {
            var pathname = (urlObj).pathname;
            var dirs = pathname.split('/');
            
            if (dirs.length !== 4) {
                return false;
            } else if (dirs[1] !== 'user' && dirs[1] !== 'channel') {
                return false;
            } else if (dirs[3] !== 'videos') {
                return false;
            }
            
            return true;
        } else {
            return false;
        }
    },
    isYoutubePlaylistUrl: function(url)
    {
        return this.isYoutubePlaylistOnVideoPage(url) || this.isYoutubePlaylistPage(url);
    },
    isYoutubePlaylistOnVideoPage: function(url) {
        if (url && url.length < 10 || !this.isValidURL_2(url)) {
            return false;
        }

        var urlObj = new URL(url);
        var hostname = urlObj.hostname;
        var protocol = urlObj.protocol;

        if (protocol !== 'https:') {
            return false;
        }

        if (hostname.endsWith('youtube.com')) {
            const reg = /^.*((youtu.be\/)|(v\/)|(\/u\/\w\/)|(embed\/)|(watch\?))\??v?=?([^#&?]*).*list=([^#&?]*).*/;
            let m = url.match(reg);
            if (!m || m.length < 9) {
                return false;
            } else {
                return true;
            }
        }
        return false;
	},
    isYoutubePlaylistPage: function(url)
    {
        if (url && url.length < 10 || !this.isValidURL_2(url)) {
            return false;
        }

        var urlObj = new URL(url);
        var hostname = urlObj.hostname;
        var protocol = urlObj.protocol;

        if (protocol !== 'https:') {
            return false;
        }

        if (hostname.endsWith('youtube.com')) {
            const reg = /^.*www.youtube.com\/playlist\?.*list=([^#&?]*).*/;
            let m = url.match(reg);
            if (!m || m.length < 2) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    },
    
    isValidURL: function(str) {
        var pattern = new RegExp('^(https?:\\/\\/)?'+ // protocol
        '((([a-z\\d]([a-z\\d-]*[a-z\\d])*)\\.?)+[a-z]{2,}|'+ // domain name
        '((\\d{1,3}\\.){3}\\d{1,3}))'+ // OR ip (v4) address
        '(\\:\\d+)?(\\/[-a-z\\d%_.~+]*)*'+ // port and path
        '(\\?[;&a-z\\d%_.~+=-]*)?'+ // query string
        '(\\#[-a-z\\d_]*)?$','i'); // fragment locator
        return pattern.test(str);
    },  
    
    isValidURL_2: function (url) {
        var urlPattern = "(https?|ftp)://(www\\.)?(((([a-zA-Z0-9.-]+\\.){1,}[a-zA-Z]{2,4}|localhost))|((\\d{1,3}\\.){3}(\\d{1,3})))(:(\\d+))?(/([a-zA-Z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?(\\?([a-zA-Z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)?(#([a-zA-Z0-9._-]|%[0-9A-F]{2})*)?";

        urlPattern = "^" + urlPattern + "$";
        var regex = new RegExp(urlPattern);

        return regex.test(url);
    },    
	
	// the method is got from tools.js as is
	getDownloadType: function(sUrl)
	{
		if (sUrl.length >= 7) {
			var sProtocol = sUrl.substr(0, 7);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "HTTP://")
				return fdm.models.DownloadType.Regular; // ordernary download
		}

		if (sUrl.length >= 8) {
			var sProtocol = sUrl.substr(0, 8);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "HTTPS://")
				return fdm.models.DownloadType.Regular; // ordernary download
		}

		if (sUrl.length >= 6) {
			var sProtocol = sUrl.substr(0, 6);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "FTP://")
				return fdm.models.DownloadType.Regular; // ordinary download
		}

		if (sUrl.length >= 7) {
			var sProtocol = sUrl.substr(0, 7);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "RTSP://" || sProtocol == "MMSH://" || sProtocol == "MMST://")
				return 2; // rtsp
		}

		if (sUrl.length >= 6) {
			var sProtocol = sUrl.substr(0, 6);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "MMS://")
				return 2; // mms
		}

		if (sUrl.length < 8)
			return -1;

		if (fdm.urlUtils.isMagnetLink(sUrl))
			return fdm.models.DownloadType.Trt; // Magnet

		var sExt = sUrl.substr(sUrl.length - 8, 8);
		sExt = sExt.toUpperCase();
		if (sExt == ".TORRENT")
		{
			if (fdm.urlUtils.isValidTrtUrl(sUrl))
				return fdm.models.DownloadType.Trt; // bitorrent
		}
		
		return -1;
	},

	isMagnetLink: function(sUrl) {
		if (sUrl == undefined) {
			return false;
		}
		if (sUrl.length >= 7) {
			var sProtocol = sUrl.substr(0, 7);
			sProtocol = sProtocol.toUpperCase();
			if (sProtocol == "MAGNET:")
				return true;
		}
		return false;
	},

	extractDomain: function(url) {
		if(!url){
			return undefined;
		}
		if(url.search(/^https?\:\/\//) != -1)
			url = url.match(/^https?\:\/\/([^\/?#]+)(?:[\/?#]|$)/i, "");
		else
			url = url.match(/^([^\/?#]+)(?:[\/?#]|$)/i, "");
		return url[1];
	}
};

fdm.htmlUtils = {
	checkInterval:null,
	//setFocus: function(element, callback) {
	//	if (typeof element == "string")	{
	//		element = document.getElementById(element);
	//	}
     //   if (!element)
     //       return;
	//	if(this.checkInterval) {
	//		clearInterval(fdm.htmlUtils.checkInterval);
	//	}
	//	this.checkInterval = setInterval(function() {
	//		if (element.offsetWidth && element.offsetHeight
	//			&& document.activeElement && document.activeElement != element)
	//		{
	//			element.focus();
	//			ko.utils.triggerEvent(element, "focusin"); // For IE, which doesn't reliably fire "focus" or "blur" events synchronously
	//
	//			clearInterval(fdm.htmlUtils.checkInterval);
	//			this.checkInterval = null;
	//			if (typeof callback =="function"){
	//				callback();
	//			}
	//		}
	//	}, 10);
	//},
	//lostFocus: function() {
	//	if(this.checkInterval) {
	//		clearInterval(this.checkInterval);
	//		this.checkInterval = null;
	//	}
	//},
	_uniqueIdCounter: 0,
	uniqueId: function(el, prefix) {
		if(el.id != ""){
			return el.id;
		}
		if(!prefix || prefix === "") {
		   prefix = "uid";
		}
		el.id = prefix + "-" + fdm.htmlUtils._uniqueIdCounter++;
		return el.id;
	},
	setCaretPosition: function(ctrl, pos)
	{
		if(ctrl.setSelectionRange)
		{
			ctrl.focus();
			ctrl.setSelectionRange(pos,pos);
		}
		else if (ctrl.createTextRange) {
			var range = ctrl.createTextRange();
			range.collapse(true);
			range.moveEnd('character', pos);
			range.moveStart('character', pos);
			range.select();
		}
	}
};

fdm.domUtils = {
	initChangeEvent: function(element)
	{
		if ("createEvent" in document) {
			var evt = document.createEvent("HTMLEvents");
			evt.initEvent("change", false, true);
			element.dispatchEvent(evt);
		}
		else
			element.fireEvent("onchange");
	}
};

fdm.DEBUG = (function(){
	// http://stackoverflow.com/q/18410119/749922
	var timestamp = function(){};
	timestamp.toString = function(){
		return "[" + (new Date).toLocaleTimeString("en-US", {hour12: false}) + "]";
	};

	var result = {
		// console log
		error: console.error.bind(console, '%s', timestamp),
		info: console.info.bind(console, '%s', timestamp),
		warn: console.warn.bind(console, '%s', timestamp),
		log: console.log.bind(console, '%s', timestamp),

		// log enter leave methods
		logInOutAll: function(obj){
			var funcs = [].slice.call(arguments, 1);
			if (funcs.length === 0) throw new Error("logInOut must be passed function names");
			_.each(funcs, function(f) { obj[f] = fdm.DEBUG.logInOut(f, obj); });
		},
		logInOut: function(funcName, obj){
			var func = obj[funcName];
			return _.wrap(func, function(func) {
				var args = [].slice.call(arguments, 1);
				console.log(funcName + "(%o) {", args);
				var time = new Date();
				var result = func.apply(obj, args);
				time = (new Date()) - time;
				console.log(" } // " + funcName + " has worked %o milliseconds.", time);
				return result;
			});

		}
	};
	return result;
})();

fdm.statUtils = {
	mosaicStat: function(uuid, greenMenuMarker, mosaicClick) {
		var url = 'http://up.freedownloadmanager.org/js_stat.php?stat_type=mosaic_menu&uuid='
			+ encodeURIComponent(uuid) + '&green_menu_marker=' + (greenMenuMarker ? '1' : '0')
			+ '&mosaic_click=' + (mosaicClick ? '1' : '0');

		this.sendStat(url)
	},
	sendStat: function (statUrl) {
		var sc = document.createElement("script");
		sc.type = "text/javascript";
		sc.async = true;
		sc.src = statUrl;
		sc.onload = function () {
			document.head.removeChild(sc);
		};
		sc.onerror = function () {
			document.head.removeChild(sc);
		};
		document.head.appendChild(sc);
	}
}