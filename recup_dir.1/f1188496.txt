var Strings = {
        available_languages: ["da", "de", "es", "fr", "it", "nl", "pt", "ro", "ru", "zh", "ar"],
        default_lang_id: "en",
        current_lang_id: "en",
        strings: {
       },//end of strings

    get: function(id, n) {
        var lang = this.current_lang_id;

        var rpc_id = '\u0004' + id;

        if (this.strings[lang][rpc_id] != undefined && this.strings[lang][rpc_id].length >= 1) {
            return this.strings[lang][rpc_id];
        }
        else if (this.strings[lang][id] != undefined && this.strings[lang][id].length >=1) {
            return this.strings[lang][id];
        }
        else {
            return [];
        }
    },

    setAllStrings: function(locale, strings){

        this.current_lang_id = locale;
        this.strings[locale] = strings;

        if (locale == 'zh')
            moment.locale('zh-cn');
        else
            moment.locale(locale);
    },

    setCurrentLanguage: function(language) {
        var defPos = language.indexOf("-");
        if (defPos > 0) {
            language = language.substring(0, defPos);
        }
        console.log("Setting current language to: "+language);
        var index = this.available_languages.indexOf(language);
        if (index >= 0) {
            this.current_lang_id = this.available_languages[index];
        } else {
            this.current_lang_id = this.default_lang_id;
        }
    },

    entityMap: {
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': '&quot;',
        "'": '&#39;',
        "/": '&#x2F;'
    },

    escapeHtml: function(string) {
        return String(string).replace(/[&<>"'\/]/g, function (s) {
            return entityMap[s];
        });
    }
};

var __ = function() {

    var stringId = arguments[0];

    var s_args = [];
    if (typeof arguments[1] != "undefined"){
        if (typeof arguments[1] == "object" &&  arguments[1].length){
            s_args = arguments[1];
        }
        else{
            s_args.push(arguments[1]);
        }
    }

    var has_plural = false;

    if (['days', 'Clear history older than'].indexOf(stringId) >= 0){

        has_plural = true;
    }
    else{

        var regN = new RegExp("%n", "gm");
        if (regN.test(stringId) && typeof s_args[0] != "undefined" ){

            has_plural = true;
        }
    }

    var string_context = '';
    if (typeof arguments[2] != 'undefined')
        string_context = arguments[2];

    var i = 1;
    var theString;
    if (has_plural){

        theString = fdmApp.localization.tr(stringId, string_context, s_args[0]);
        i = 2;
    }
    else if (string_context && string_context != ''){

        theString = fdmApp.localization.tr(stringId, string_context);
    }
    else
        theString = Strings.get(stringId);

    if (theString == undefined || theString == "") {

        // if (!fdmApp.isFake)
        //     console.error('localization stringId not found "'+ stringId + '"');
        theString = stringId;
    }

    // start with the second argument (i = 1)
    for (; i <= s_args.length; i++) {
        // "gm" = RegEx options for Global search (more than one instance)
        // and for Multiline search
        var regEx = new RegExp("%" + i, "gm");
        theString = theString.replace(regEx, s_args[i-1]);
    }

    return theString;
};

/*
var __old = function() {
    // The string containing the format items (e.g. "{0}")
    // will and always has to be the first argument.
    var plural;
    var stringId = arguments[0];
    var theString = Strings.get(stringId);
    if (theString.length >= 2) {
        if (arguments.length <= 1) {
            //theString = theString;
        } else {
            var n = arguments[1];
            var index = 1;
            if (n != undefined && n != "" && n % 1 === 0) {
                switch (Strings.current_lang_id) {
                    case "ja":
                    case "zh":
                        break;
                    case "tr":
                    case "fr":
                        if (n > 1) {
                            index = 2;
                        } else {
                            index = 1;
                        }
                        break;
                    case "el":
                    case "es":
                    case "hi":
                    case "it":
                    case "pt":
                    case "nl":
                    case "de":
                    case "en":
                        if (n != 1) {
                            index = 2;
                        } else {
                            index = 1;
                        }
                        break;
                    case "ro":
                        plural = (n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2);
                        index = plural + 1;
                        break;
                    case "ru":
                        plural = (n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2);
                        index = plural + 1;
                        break;
                }
            }
            //if (theString[index] != undefined && theString[index] != null) {
            //    theString = theString[index];
            //} else {
            //    theString = theString[1];
            //}
        }
    } else {
        theString = "";
    }


    if (theString == undefined || theString == "") {

        console.error('localization stringId not found "'+ stringId + '"');
        return stringId;
        //return theString;
    }
    else{
        delete Strings.strings.en_debug[stringId];
    }

    var i = 1;
    var regN = new RegExp("%n", "gm");
    if (regN.test(theString) && typeof arguments[1] != "undefined" ){

        theString = fdmApp.localization.tr(stringId, '', arguments[1]);
        //theString = theString.replace(regN, arguments[1]);
        i = 2;
    }

    // start with the second argument (i = 1)
    for (; i < arguments.length; i++) {
        // "gm" = RegEx options for Global search (more than one instance)
        // and for Multiline search
        var regEx = new RegExp("%" + i, "gm");
        theString = theString.replace(regEx, arguments[i]);
    }

    return theString;
};

var __old2 = function() {
    // The string containing the format items (e.g. "{0}")
    // will and always has to be the first argument.
    var plural;
    var stringId = arguments[0];
    var theString = Strings.get(stringId);
    if (theString.length >= 2) {
        if (theString[0] == null || arguments.length <= 1) {
            theString = theString[1];
        } else {
            var n = arguments[1];
            var index = 1;
            if (n != undefined && n != "" && n % 1 === 0) {
                switch (Strings.current_lang_id) {
                    case "ja":
                    case "zh":
                        break;
                    case "tr":
                    case "fr":
                        if (n > 1) {
                            index = 2;
                        } else {
                            index = 1;
                        }
                        break;
                    case "el":
                    case "es":
                    case "hi":
                    case "it":
                    case "pt":
                    case "nl":
                    case "de":
                    case "en":
                        if (n != 1) {
                            index = 2;
                        } else {
                            index = 1;
                        }
                        break;
                    case "ro":
                        plural = (n==1 ? 0 : (n==0 || (n%100 > 0 && n%100 < 20)) ? 1 : 2);
                        index = plural + 1;
                        break;
                    case "ru":
                        plural = (n%10==1 && n%100!=11 ? 0 : n%10>=2 && n%10<=4 && (n%100<10 || n%100>=20) ? 1 : 2);
                        index = plural + 1;
                        break;
                }
            }
            if (theString[index] != undefined && theString[index] != null) {
                theString = theString[index];
            } else {
                theString = theString[1];
            }
        }
    } else {
        theString = "";
    }


    if (theString == undefined || theString == "") {

        console.error('localization stringId not found "'+ stringId + '"');
        return stringId;
        //return theString;
    }
    else{
        delete Strings.strings.en_debug[stringId];
    }

    var i = 1;
    var regN = new RegExp("%n", "gm");
    if (regN.test(theString) && typeof arguments[1] != "undefined" ){

        theString = theString.replace(regN, arguments[1]);
        i = 2;
    }

    // start with the second argument (i = 1)
    for (; i < arguments.length; i++) {
        // "gm" = RegEx options for Global search (more than one instance)
        // and for Multiline search
        var regEx = new RegExp("%" + i, "gm");
        theString = theString.replace(regEx, arguments[i]);
    }

    return theString;
};
*/

var not_capitalize = ["a", "an", "the", "at", "in", "by", "to", "for", "from", "or", "nor"];

function __upperCapitalize(){

    var string = __.apply(null, arguments);

    if (Strings.current_lang_id != 'en')
        return string;

    var a = string.split(/\s+/);

    for (var i = 0; i < a.length; i++){
        var w = a[i];

        if (w.length > 2 && not_capitalize.indexOf(w) < 0)
            a[i]  = w.slice(0, 1).toUpperCase() + w.slice(1);
    }

    var new_string = a.join(' ');

    return new_string;
}


// Cache locale text.
var cacheText = {};
/*	The binding localization strings via "boost::locale::gettext" function.
 This extender can be used in data binding as data-bind='localeText:"text"'
 */

function localeText()
{
    return __.apply(undefined, arguments);
}

function getLocaleText(strTranslate)
{
    return __.apply(undefined, arguments);
    //returnSting = fdmApp.getLocaleText(strTranslate);
}
