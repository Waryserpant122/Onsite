/*
colpick Color Picker / colpick.com
*/
/*Main container*/
.colpick {
	position: absolute;
	width: 346px;
	height: 178px;
	overflow: hidden;
	display: none;
	font-family: Arial, Helvetica, sans-serif;
	background:#ebebeb;
	border: 1px solid #bbb;
	
	/*Prevents selecting text when dragging the selectors*/
	-webkit-user-select: none;
	-moz-user-select: none;
	-ms-user-select: none;
	-o-user-select: none;
	user-select: none;
}
/*Color selection box with gradients*/
.colpick_color {
	position: absolute;
	left: 7px;
	top: 7px;
	width: 156px;
	height: 156px;
	overflow: hidden;
	outline: 1px solid #aaa;
	cursor: crosshair;
}
.colpick_color_overlay1 {
	position: absolute;
	left:0;
	top:0;
	width: 156px;
	height: 156px;
	-ms-filter: "progid:DXImageTransform.Microsoft.gradient(GradientType=1,startColorstr='#ffffff', endColorstr='#00ffffff')"; /* IE8 */
	background: -moz-linear-gradient(left, rgba(255,255,255,1) 0%, rgba(255,255,255,0) 100%); /* FF3.6+ */
	background: -webkit-gradient(linear, left top, right top, color-stop(0%,rgba(255,255,255,1)), color-stop(100%,rgba(255,255,255,0))); /* Chrome,Safari4+ */
	background: -webkit-linear-gradient(left, rgba(255,255,255,1) 0%,rgba(255,255,255,0) 100%); /* Chrome10+,Safari5.1+ */
	background: -o-linear-gradient(left, rgba(255,255,255,1) 0%,rgba(255,255,255,0) 100%); /* Opera 11.10+ */
	background: -ms-linear-gradient(left, rgba(255,255,255,1) 0%,rgba(255,255,255,0) 100%); /* IE10+ */
	background: linear-gradient(to right, rgba(255,255,255,1) 0%, rgba(255,255,255,0) 100%);
	filter:  progid:DXImageTransform.Microsoft.gradient(GradientType=1,startColorstr='#ffffff', endColorstr='#00ffffff'); /* IE6 & IE7 */
}
.colpick_color_overlay2 {
	position: absolute;
	left:0;
	top:0;
	width: 156px;
	height: 156px;
	-ms-filter: "progid:DXImageTransform.Microsoft.gradient(GradientType=0,startColorstr='#00000000', endColorstr='#000000')"; /* IE8 */
	background: -moz-linear-gradient(top, rgba(0,0,0,0) 0%, rgba(0,0,0,1) 100%); /* FF3.6+ */
	background: -webkit-gradient(linear, left top, left bottom, color-stop(0%,rgba(0,0,0,0)), color-stop(100%,rgba(0,0,0,1))); /* Chrome,Safari4+ */
	background: -webkit-linear-gradient(top, rgba(0,0,0,0) 0%,rgba(0,0,0,1) 100%); /* Chrome10+,Safari5.1+ */
	background: -o-linear-gradient(top, rgba(0,0,0,0) 0%,rgba(0,0,0,1) 100%); /* Opera 11.10+ */
	background: -ms-linear-gradient(top, rgba(0,0,0,0) 0%,rgba(0,0,0,1) 100%); /* IE10+ */
	background: linear-gradient(to bottom, rgba(0,0,0,0) 0%,rgba(0,0,0,1) 100%); /* W3C */
	filter: progid:DXImageTransform.Microsoft.gradient( startColorstr='#00000000', endColorstr='#000000',GradientType=0 ); /* IE6-9 */
}
/*Circular color selector*/
.colpick_selector_outer {
	background:none;
	position: absolute;
	width: 11px;
	height: 11px;
	margin: -6px 0 0 -6px;
	border: 1px solid black;
	border-radius: 50%;
}
.colpick_selector_inner{
	position: absolute;
	width: 9px;
	height: 9px;
	border: 1px solid white;
	border-radius: 50%;
}
/*Vertical hue bar*/
.colpick_hue {
	position: absolute;
	top: 6px;
	left: 175px;
	width: 19px;
	height: 156px;
	border: 1px solid #aaa;
	cursor: n-resize;
}
/*Hue bar sliding indicator*/
.colpick_hue_arrs {
	position: absolute;
	left: -8px;
	width: 35px;
	height: 7px;
	margin: -7px 0 0 0;
}
.colpick_hue_larr {
	position:absolute;
	width: 0; 
	height: 0; 
	border-top: 6px solid transparent;
	border-bottom: 6px solid transparent;
	border-left: 7px solid #858585;
}
.colpick_hue_rarr {
	position:absolute;
	right:0;
	width: 0; 
	height: 0; 
	border-top: 6px solid transparent;
	border-bottom: 6px solid transparent; 
	border-right: 7px solid #858585; 
}
/*New color box*/
.colpick_new_color {
	position: absolute;
	left: 207px;
	top: 6px;
	width: 60px;
	height: 27px;
	background: #f00;
	border: 1px solid #8f8f8f;
}
/*Current color box*/
.colpick_current_color {
	position: absolute;
	left: 277px;
	top: 6px;
	width: 60px;
	height: 27px;
	background: #f00;
	border: 1px solid #8f8f8f;
}
/*Input field containers*/
.colpick_field, .colpick_hex_field  {
	position: absolute;
	height: 20px;
	width: 60px;
	overflow:hidden;
	background:#f3f3f3;
	color:#b8b8b8;
	font-size:12px;
	border:1px solid #bdbdbd;
}
.colpick_rgb_r {
	top: 40px;
	left: 207px;
}
.colpick_rgb_g {
	top: 67px;
	left: 207px;
}
.colpick_rgb_b {
	top: 94px;
	left: 207px;
}
.colpick_hsb_h {
	top: 40px;
	left: 277px;
}
.colpick_hsb_s {
	top: 67px;
	left: 277px;
}
.colpick_hsb_b {
	top: 94px;
	left: 277px;
}
.colpick_hex_field {
	width: 68px;
	left: 207px;
	top: 121px;
}
/*Text field container on focus*/
.colpick_focus {
	border-color: #999;
}
/*Field label container*/
.colpick_field_letter {
	position: absolute;
	width: 12px;
	height: 20px;
	line-height: 20px;
	padding-left: 4px;
	background: #efefef;
	border-right: 1px solid #bdbdbd;
	font-weight: bold;
	color:#777;
}
/*Text inputs*/
.colpick_field input, .colpick_hex_field input {
	position: absolute;
	right: 11px;
	margin: 0;
	padding: 0;
	height: 20px;
	line-height: 20px;
	background: transparent;
	border: none;
	font-size: 12px;
	font-family: Arial, Helvetica, sans-serif;
	color: #555;
	text-align: right;
	outline: none;
}
.windows .colpick_field input:focus, .windows .colpick_hex_field input:focus {
	border-color: transparent;
	-webkit-box-shadow: none;
	-moz-box-shadow: none;
	box-shadow: none;
}
.colpick_hex_field input {
	right: 4px;
}
/*Field up/down arrows*/
.colpick_field_arrs {
	position: absolute;
	top: 0;
	right: 0;
	width: 9px;
	height: 21px;
	cursor: n-resize;
}
.colpick_field_uarr {
	position: absolute;
	top: 5px;
	width: 0; 
	height: 0; 
	border-left: 4px solid transparent;
	border-right: 4px solid transparent;
	border-bottom: 4px solid #959595;
}
.colpick_field_darr {
	position: absolute;
	bottom:5px;
	width: 0; 
	height: 0; 
	border-left: 4px solid transparent;
	border-right: 4px solid transparent;
	border-top: 4px solid #959595;
}
/*Submit/Select button*/
.colpick_submit {
	position: absolute;
	left: 207px;
	top: 149px;
	width: 130px;
	height: 22px;
	line-height:22px;
	background: #efefef;
	text-align: center;
	color: #555;
	font-size: 12px;
	font-weight:bold;
	border: 1px solid #bdbdbd;
}
.colpick_submit:hover {
	background:#f3f3f3;
	border-color:#999;
	cursor: pointer;
}

/*full layout with no submit button*/
.colpick_full_ns  .colpick_submit, .colpick_full_ns .colpick_current_color{
	display:none;
}
.colpick_full_ns .colpick_new_color {
	width: 130px;
	height: 25px;
}
.colpick_full_ns .colpick_rgb_r, .colpick_full_ns .colpick_hsb_h {
	top: 42px;
}
.colpick_full_ns .colpick_rgb_g, .colpick_full_ns .colpick_hsb_s {
	top: 73px;
}
.colpick_full_ns .colpick_rgb_b, .colpick_full_ns .colpick_hsb_b {
	top: 104px;
}
.colpick_full_ns .colpick_hex_field {
	top: 135px;
}

/*rgbhex layout*/
.colpick_rgbhex .colpick_hsb_h, .colpick_rgbhex .colpick_hsb_s, .colpick_rgbhex .colpick_hsb_b {
	display:none;
}
.colpick_rgbhex {
	width:282px;
}
.colpick_rgbhex .colpick_field, .colpick_rgbhex .colpick_submit {
	width:68px;
}
.colpick_rgbhex .colpick_new_color {
	width:34px;
	border-right:none;
}
.colpick_rgbhex .colpick_current_color {
	width:34px;
	left:240px;
	border-left:none;
}

/*rgbhex layout, no submit button*/
.colpick_rgbhex_ns  .colpick_submit, .colpick_rgbhex_ns .colpick_current_color{
	display:none;
}
.colpick_rgbhex_ns .colpick_new_color{
	width:68px;
	border: 1px solid #8f8f8f;
}
.colpick_rgbhex_ns .colpick_rgb_r {
	top: 42px;
}
.colpick_rgbhex_ns .colpick_rgb_g {
	top: 73px;
}
.colpick_rgbhex_ns .colpick_rgb_b {
	top: 104px;
}
.colpick_rgbhex_ns .colpick_hex_field {
	top: 135px;
}

/*hex layout*/
.colpick_hex .colpick_hsb_h, .colpick_hex .colpick_hsb_s, .colpick_hex .colpick_hsb_b, .colpick_hex .colpick_rgb_r, .colpick_hex .colpick_rgb_g, .colpick_hex .colpick_rgb_b {
	display:none;
}
.colpick_hex {
	width:206px;
	height:201px;
}
.colpick_hex .colpick_hex_field {
	width:72px;
	height:25px;
	top:168px;
	left:80px;
}
.colpick_hex .colpick_hex_field div, .colpick_hex .colpick_hex_field input {
	height: 25px;
	line-height: 25px;
}
.colpick_hex .colpick_new_color {
	left:9px;
	top:168px;
	width:30px;
	border-right:none;
}
.colpick_hex .colpick_current_color {
	left:39px;
	top:168px;
	width:30px;
	border-left:none;
}
.colpick_hex .colpick_submit {
	left:164px;
	top: 168px;
	width:30px;
	height:25px;
	line-height: 25px;
}

/*hex layout, no submit button*/
.colpick_hex_ns  .colpick_submit, .colpick_hex_ns .colpick_current_color {
	display:none;
}
.colpick_hex_ns .colpick_hex_field {
	width:80px;
}
.colpick_hex_ns .colpick_new_color{
	width:60px;
	border: 1px solid #8f8f8f;
}

/*Dark color scheme*/
.colpick_dark {
	background: #161616;
	border-color: #2a2a2a;
}
.colpick_dark .colpick_color {
	outline-color: #333;
}
.colpick_dark .colpick_hue {
	border-color: #555;
}
.colpick_dark .colpick_field, .colpick_dark .colpick_hex_field {
	background: #101010;
	border-color: #2d2d2d;
}
.colpick_dark .colpick_field_letter {
	background: #131313;
	border-color: #2d2d2d;
	color: #696969;
}
.colpick_dark .colpick_field input, .colpick_dark .colpick_hex_field input {
	color: #7a7a7a;
}
.colpick_dark .colpick_field_uarr {
	border-bottom-color:#696969;
}
.colpick_dark .colpick_field_darr {
	border-top-color:#696969;
}
.colpick_dark .colpick_focus {
	border-color:#444;
}
.colpick_dark .colpick_submit {
	background: #131313;
	border-color:#2d2d2d;
	color:#7a7a7a;
}
.colpick_dark .colpick_submit:hover {
	background-color:#101010;
	border-color:#444;
}

.color-box {
	float:left;
	width:10px;
	height:10px;
	margin:5px;
	border: 1px solid black;
}