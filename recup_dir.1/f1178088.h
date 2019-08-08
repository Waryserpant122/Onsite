jQuery.namespace("fdm");

/*
* @version    0.4.6
* @date       2014-01-27
* @stability  2 - Unstable
* @author     Lauri Rooden (https://github.com/litejs/natural-compare-lite)
* @license    MIT License
*/
String.naturalCompare = function(a, b) {

	if (a != b) for (var i, ca, cb = 1, ia = 0, ib = 0; cb;) {
		ca = a.charCodeAt(ia++) || 0
		cb = b.charCodeAt(ib++) || 0

		if (ca < 58 && ca > 47 && cb < 58 && cb > 47) {
			for (i = ia; ca = a.charCodeAt(ia), ca < 58 && ca > 47; ia++);
			ca = (a.slice(i - 1, ia) | 0) + 1
			for (i = ib; cb = b.charCodeAt(ib), cb < 58 && cb > 47; ib++);
			cb = (b.slice(i - 1, ib) | 0) + 1
		}

		if (ca != cb) return (ca < cb) ? -1 : 1
	}
	return 0
};
// https://developer.mozilla.org/en/docs/Web/JavaScript/Reference/Global_Objects/String/endsWith
if (!String.prototype.endsWith) {
	Object.defineProperty(String.prototype, 'endsWith', {
		value: function (searchString, position) {
			var subjectString = this.toString();
			if (position === undefined || position > subjectString.length) {
				position = subjectString.length;
			}
			position -= searchString.length;
			var lastIndex = subjectString.indexOf(searchString, position);
			return lastIndex !== -1 && lastIndex === position;
		}
	});
}

fdm.Math = {
	round: function(value, significant){
		//var f = value-Math.floor(value);
		//var roundedValue = parseFloat((value.toFixed(/*(a>1e3) && */(f >= 0.1) && (f < 0.9) ? 2 : 0)));
		return value.toPrecision(significant) * 1;
	},

	// calculates how many symbols are in the target number after decimal point
	decimalPlaces: function(num, significant){
		// http://stackoverflow.com/a/10454560
		var match = (num.toPrecision(significant)).match(/(?:[\.,](\d+))?(?:[eE]([+-]?\d+))?$/);
		if (!match) { return 0; }
		return Math.max(
			0,
			// Number of digits right of decimal point.
			(match[1] ? match[1].length : 0)
				// Adjust for scientific notation.
				- (match[2] ? +match[2] : 0));
	},

	calcRoundPow: function(value, significant){
		var result = Math.log(value)/Math.log(fdmApp.bytesInKB) | 0;
		var rounded = value/Math.pow(fdmApp.bytesInKB,result);
		if(rounded > Math.pow(10, significant)){
			result++;
		}
		return result;
	},

    roundPrecision: function(number, count){

        number = number.toString();
        var result = "";

        var n=0;
        for (var i = 0; i < number.length; i++){
            var v = number[i];
            if (parseInt(v) > 0)
                n++;
            if (n > count && parseInt(v) > 0){
                v = '0';
            }
            result = result + v;
        }
        return parseFloat(result);
    }
};

fdm.fileUtils = {
	unitNameByPow: function(e){
		return (e?'KMGTPEZY'[--e]+'B': 'B'/*bytes*/);
	},
	fileSizeIEC: function(a,b,c,d,e) {
		// based on http://stackoverflow.com/a/20463021
		var r = (c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e));
		var f = r-Math.floor(r);
		return r.toFixed((a>1e3) && (f >= 0.1) && (f < 0.9) ? 2 : 0)
			+' '+ fdm.fileUtils.unitNameByPow(e);
	},

	fileSizeIECUnit: function(a,b,c,d,e) {
		c=Math.log;d=fdmApp.bytesInKB;e=c(a)/c(d)|0;
		return fdm.fileUtils.unitNameByPow(e);
	},

	fileSizeIECUnitless: function(a,b,c,d,e) {
		var r = (c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e));
		return parseFloat((c=Math.log, d=fdmApp.bytesInKB, e=c(a)/c(d)|0, a / Math.pow(d,e)).toPrecision(r > 99.5 ? 4 : 3));
	},

	roundFileSizeIEC: function(a,b,c,d,e) {
		if(a == 0) return "0 " + fdm.fileUtils.unitNameByPow(e);
		return parseFloat((c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e)).toPrecision(2))
			+' '+ fdm.fileUtils.unitNameByPow(e);
	},

	roundFileSizeIEC2: function(a,b,c,d,e) {
		var r = (c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e));
		return parseFloat((c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e)).toFixed(r > 99.5 ? 0 : 2))
			+' '+ fdm.fileUtils.unitNameByPow(e);
	},

	roundFileSizeIECUnitless: function(a,reference,b,c,d,e) {
		var r;
		if(reference === undefined)
			r = (c=Math.log,d=fdmApp.bytesInKB,e=c(a)/c(d)|0,a/Math.pow(d,e));
		else
			r = (c=Math.log,d=fdmApp.bytesInKB,e=c(reference)/c(d)|0,a/Math.pow(d,e));  // use the same unit as in reference #1971
		return parseFloat(r).toFixed(r > 99.5 ? 0 : 1);
	},

	extractExtension: function(filePath)
	{
		return (/[.]/.exec(filePath)) ? /[^.]+$/.exec(filePath).toString() : undefined;
	},

	fileListToFileTree: function(files) {
		// the algorithm was created by I.Grygoriev and copied from tools.js with some improvements
		var aTree = [files.length];
		var root = [];

		for (var i = 0; i < files.length; i++) {
			var fileItem = _.clone(files[i]);
			var tmpParent = aTree[fileItem.parentIndex];
			var tmpNode = {
				parentIndex: fileItem.parentIndex,
				node: { index: i, children: [], data: fileItem, checked: fileItem.isChecked, name: fileItem.name/*, parent: tmpParent ? tmpParent.node : null*/ }
			};

			if (tmpNode.parentIndex == -1) {
				root.push(tmpNode.node);
			}
			else { // if (treeNode.parentIndex != -1)
				var parentChildren = aTree[tmpNode.parentIndex].node.children;
				parentChildren.push(tmpNode.node);
				parentChildren.sort(function(a,b){
					return String.naturalCompare(a.name.toLowerCase(), b.name.toLowerCase());
				});
			}

			aTree[i] = tmpNode;
		}

		return root;
	}
};

fdm.speedUtils = {

	formatFloatSpeed: function(num){ num = parseFloat(num) || 0; return parseFloat((Math.round(num * 100) / 100).toFixed(2));},

    speed2SignDigits: function(speed, need_precision) {

        // round to 2 significant digits
        //http://www.quora.com/Google-Sheets/How-can-I-round-to-x-significant-digits
        if (need_precision == null)
            need_precision = true;
		var sizeText = "0";
		var speedPow = Math.log(speed)/Math.log(fdmApp.bytesInKB) | 0;

		if(speed > 0){
            var sizeValue = speed/Math.pow(fdmApp.bytesInKB, speedPow);
            if(sizeValue >= 1000){
                speedPow++;
                sizeValue = speed/Math.pow(fdmApp.bytesInKB, speedPow);
            }
			if (sizeValue < 0.01)
				sizeValue = 0;
			var sizeText = fdm.fileUtils.fileSizeIECUnitless(sizeValue);
            if (need_precision)
            {
                sizeText = fdm.Math.roundPrecision(sizeText, 2);
                sizeText = sizeText.toPrecision(2);
                if (sizeText > 9)
                    sizeText = sizeText*1;
            }
		}
		var units = fdm.fileUtils.unitNameByPow(speedPow);
		// var result = __("%1 " + units +"/s", sizeText);
		var result = sizeText + " " + units +"/s";
		return result;
    },
	//twoSpeedsAsText: function(dlSpeed, ulSpeed, seeding_enabled)
	//{
	//	seeding_enabled = seeding_enabled || false;
    //
	//	if (dlSpeed == 0 && ulSpeed == 0 && seeding_enabled)
	//		return '<span class="arrow_up"></span>' + fdm.speedUtils.speed2SignDigits(ulSpeed);
    //
	//	var resultSpeedText = '';
	//	if (dlSpeed > 0)
	//		resultSpeedText += '<span class="arrow_dwn"></span>' + fdm.speedUtils.speed2SignDigits(dlSpeed);
    //
	//	if (dlSpeed > 0 && ulSpeed > 0)
	//		resultSpeedText += '; ';
    //
	//	if (ulSpeed > 0)
	//		resultSpeedText += '<span class="arrow_up"></span>' + fdm.speedUtils.speed2SignDigits(ulSpeed);
    //
	//	return resultSpeedText;
	//}
};

fdm.dateUtils = {
	downloadDateText: function(valueUnwrapped){

		var downloadDateText = '';
		if(valueUnwrapped == null ||
			(typeof valueUnwrapped === 'object' && valueUnwrapped.getTime() == 0))
		{
			downloadDateText = '';
		}
		else
		{
			var m = moment(valueUnwrapped);
			var now = moment();
			if(Math.abs(m.diff(now, 'hours')) < 24 && m.day() == now.day())
				downloadDateText = m.format("H:mm");
			else{

				if (Strings.current_lang_id == 'ru')
					downloadDateText = m.format("D MMM");
				else
					downloadDateText = m.format("MMM D");
			}
		}

		return downloadDateText;
	},
	downloadDateTitle: function(valueUnwrapped){

		//moment.locale();
		var m = moment(valueUnwrapped);
		return m.format('llll');
	}
};

fdm.timeUtils = {

	roundUp: function(x, step) { return x <= 1 ? 1 : x; step = step || 5; var r = Math.round(x/step)*step; return r <= 1 ? 1 : r; },
	remainingTime: function(remainingTime){

		var seconds, minutes, hours, days, weeks, years, text;

		var valueUnwrapped = remainingTime;

		if (!valueUnwrapped || valueUnwrapped < 0)
			return "";

		seconds = Math.floor(valueUnwrapped / 1000);

		if (seconds >= 3153600000)
			return "