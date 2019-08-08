// Avoid `console` errors in browsers that lack a console.
(function() {
    var method;
    var noop = function () {};
    var methods = [
        'assert', 'clear', 'count', 'debug', 'dir', 'dirxml', 'error',
        'exception', 'group', 'groupCollapsed', 'groupEnd', 'info', 'log',
        'markTimeline', 'profile', 'profileEnd', 'table', 'time', 'timeEnd',
        'timeStamp', 'trace', 'warn'
    ];
    var length = methods.length;
    var console = (window.console = window.console || {});

    while (length--) {
        method = methods[length];

        // Only stub undefined methods.
        if (!console[method]) {
            console[method] = noop;
        }
    }
}());

// Adds namespace support in javascript files
jQuery.namespace = function() {
    var a=arguments, o=null, i, j, d;
    for (i=0; i<a.length; i=i+1) {
        d=a[i].split(".");
        o=window;
        for (j=0; j<d.length; j=j+1) {
            o[d[j]]=o[d[j]] || {};
            o=o[d[j]];
        }
    }
    return o;
};

// Place any jQuery/helper plugins in here.
jQuery.fn.verticalScroll = function(container) {
	if(this.isBehindTopBorder(container)) {
		this[0].scrollIntoView(true);
//        container[0].scrollTop = this[0].offsetTop - $('#toolbar').height() + 2
	}
	else if(this.isBehindBottomBorder(container)) {
		this[0].scrollIntoView(false);
	}
	return this; // for chaining...
};

jQuery.fn.isBehindTopBorder = function(container) {
	var parentTop = $(container).offset().top// + $('#toolbar').height() + 2;

	var elemTop = this.offset().top;
	return elemTop < parentTop;
};

jQuery.fn.isBehindBottomBorder = function(container) {
	var parentTop = $(container).offset().top;
	var parentBottom = parentTop + $(container).height();

	var elemTop = this.offset().top;
	var elemBottom = elemTop + this.height();
	return elemBottom > parentBottom;
};