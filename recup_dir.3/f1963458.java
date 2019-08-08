ch_symbol");
var p=b.ImportNow("replace_symbol");
var q=b.ImportNow("search_symbol");
var r=b.ImportNow("split_symbol");
b.Import(function(s){
c=s.ArrayIndexOf;
d=s.ArrayJoin;
i=s.IsRegExp;
j=s.MakeRangeError;
k=s.MakeTypeError;
l=s.MaxSimple;
m=s.MinSimple;
n=s.RegExpInitialize;
});
function StringToString(){
if(!(typeof(this)==='string')&&!(%_ClassOf(this)==='String')){
throw k(69,'String.prototype.toString');
}
return %_ValueOf(this);
}
function StringValueOf(){
if(!(typeof(this)==='string')&&!(%_ClassOf(this)==='String')){
throw k(69,'String.prototype.valueOf');
}
return %_ValueOf(this);
}
function StringCharAtJS(t){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.charAt");
var u=%_StringCharAt(this,t);
if(%_IsSmi(u)){
u=%_StringCharAt((%_ToString(this)),(%_ToInteger(t)));
}
return u;
}
function StringCharCodeAtJS(t){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.charCodeAt");
var u=%_StringCharCodeAt(this,t);
if(!%_IsSmi(u)){
u=%_StringCharCodeAt((%_ToString(this)),(%_ToInteger(t)));
}
return u;
}
function StringConcat(v){
"use strict";
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.concat");
var w=(%_ToString(this));
var x=arguments.length;
for(var y=0;y<x;++y){
w=w+(%_ToString(arguments[y]));
}
return w;
}
function StringIndexOf(z,A){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.indexOf");
var B=(%_ToString(this));
z=(%_ToString(z));
var C=(%_ToInteger(A));
if(C<0)C=0;
if(C>B.length)C=B.length;
return %StringIndexOf(B,z,C);
}
%FunctionSetLength(StringIndexOf,1);
function StringLastIndexOf(D,t){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.lastIndexOf");
var E=(%_ToString(this));
var F=E.length;
var D=(%_ToString(D));
var G=D.length;
var C=F-G;
var A=(%_ToNumber(t));
if(!(!%_IsSmi(%IS_VAR(A))&&!(A==A))){
A=(%_ToInteger(A));
if(A<0){
A=0;
}
if(A+G<F){
C=A;
}
}
if(C<0){
return-1;
}
return %StringLastIndexOf(E,D,C);
}
%FunctionSetLength(StringLastIndexOf,1);
function StringLocaleCompareJS(v){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.localeCompare");
return %StringLocaleCompare((%_ToString(this)),(%_ToString(v)));
}
function StringMatchJS(z){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.match");
if(!(z==null)){
var H=z[o];
if(!(H===(void 0))){
return %_Call(H,z,this);
}
}
var B=(%_ToString(this));
var I=%_NewObject(e,e);
n(I,z);
return I[o](B);
}
function StringNormalize(J){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.normalize");
var w=(%_ToString(this));
var K=(J===(void 0))?'NFC':(%_ToString(J));
var L=['NFC','NFD','NFKC','NFKD'];
var M=%_Call(c,L,K);
if(M===-1){
throw j(183,
%_Call(d,L,', '));
}
return w;
}
%FunctionSetLength(StringNormalize,0);
var N=[2,"","",-1,-1];
function StringReplace(O,P){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.replace");
if(!(O==null)){
var Q=O[p];
if(!(Q===(void 0))){
return %_Call(Q,O,this,P);
}
}
var B=(%_ToString(this));
O=(%_ToString(O));
if(O.length==1&&
B.length>0xFF&&
(typeof(P)==='string')&&
%StringIndexOf(P,'$',0)<0){
return %StringReplaceOneCharWithString(B,O,P);
}
var R=%StringIndexOf(B,O,0);
if(R<0)return B;
var S=R+O.length;
var u=%_SubString(B,0,R);
if((typeof(P)==='function')){
u+=P(O,R,B);
}else{
N[3]=R;
N[4]=S;
u=ExpandReplacement((%_ToString(P)),
B,
N,
u);
}
return u+%_SubString(B,S,B.length);
}
function ExpandReplacement(T,B,U,u){
var V=T.length;
var W=%StringIndexOf(T,'$',0);
if(W<0){
if(V>0)u+=T;
return u;
}
if(W>0)u+=%_SubString(T,0,W);
while(true){
var X='$';
var A=W+1;
if(A<V){
var Y=%_StringCharCodeAt(T,A);
if(Y==36){
++A;
u+='$';
}else if(Y==38){
++A;
u+=
%_SubString(B,U[3],U[4]);
}else if(Y==96){
++A;
u+=%_SubString(B,0,U[3]);
}else if(Y==39){
++A;
u+=%_SubString(B,U[4],B.length);
}else if(Y>=48&&Y<=57){
var Z=(Y-48)<<1;
var aa=1;
var ab=((U)[0]);
if(A+1<T.length){
var W=%_StringCharCodeAt(T,A+1);
if(W>=48&&W<=57){
var ac=Z*10+((W-48)<<1);
if(ac<ab){
Z=ac;
aa=2;
}
}
}
if(Z!=0&&Z<ab){
var R=U[(3+(Z))];
if(R>=0){
u+=
%_SubString(B,R,U[(3+(Z+1))]);
}
A+=aa;
}else{
u+='$';
}
}else{
u+='$';
}
}else{
u+='$';
}
W=%StringIndexOf(T,'$',A);
if(W<0){
if(A<V){
u+=%_SubString(T,A,V);
}
return u;
}
if(W>A){
u+=%_SubString(T,A,W);
}
}
return u;
}
function StringSearch(z){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.search");
if(!(z==null)){
var ad=z[q];
if(!(ad===(void 0))){
return %_Call(ad,z,this);
}
}
var B=(%_ToString(this));
var I=%_NewObject(e,e);
n(I,z);
return %_Call(I[q],I,B);
}
function StringSlice(R,S){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.slice");
var w=(%_ToString(this));
var ae=w.length;
var af=(%_ToInteger(R));
var ag=ae;
if(!(S===(void 0))){
ag=(%_ToInteger(S));
}
if(af<0){
af+=ae;
if(af<0){
af=0;
}
}else{
if(af>ae){
return'';
}
}
if(ag<0){
ag+=ae;
if(ag<0){
return'';
}
}else{
if(ag>ae){
ag=ae;
}
}
if(ag<=af){
return'';
}
return %_SubString(w,af,ag);
}
function StringSplitJS(ah,ai){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.split");
if(!(ah==null)){
var aj=ah[r];
if(!(aj===(void 0))){
return %_Call(aj,ah,this,ai);
}
}
var B=(%_ToString(this));
ai=((ai===(void 0)))?4294967295:((ai)>>>0);
var V=B.length;
var ak=(%_ToString(ah));
if(ai===0)return[];
if((ah===(void 0)))return[B];
var al=ak.length;
if(al===0)return %StringToArray(B,ai);
return %StringSplit(B,ak,ai);
}
function StringSubstring(R,S){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.subString");
var w=(%_ToString(this));
var ae=w.length;
var af=(%_ToInteger(R));
if(af<0){
af=0;
}else if(af>ae){
af=ae;
}
var ag=ae;
if(!(S===(void 0))){
ag=(%_ToInteger(S));
if(ag>ae){
ag=ae;
}else{
if(ag<0)ag=0;
if(af>ag){
var am=ag;
ag=af;
af=am;
}
}
}
return %_SubString(w,af,ag);
}
function StringSubstr(R,an){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.substr");
var w=(%_ToString(this));
var x;
if((an===(void 0))){
x=w.length;
}else{
x=(%_ToInteger(an));
if(x<=0)return'';
}
if((R===(void 0))){
R=0;
}else{
R=(%_ToInteger(R));
if(R>=w.length)return'';
if(R<0){
R+=w.length;
if(R<0)R=0;
}
}
var S=R+x;
if(S>w.length)S=w.length;
return %_SubString(w,R,S);
}
function StringToLowerCaseJS(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.toLowerCase");
return %StringToLowerCase((%_ToString(this)));
}
function StringToLocaleLowerCase(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.toLocaleLowerCase");
return %StringToLowerCase((%_ToString(this)));
}
function StringToUpperCaseJS(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.toUpperCase");
return %StringToUpperCase((%_ToString(this)));
}
function StringToLocaleUpperCase(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.toLocaleUpperCase");
return %StringToUpperCase((%_ToString(this)));
}
function StringTrimJS(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.trim");
return %StringTrim((%_ToString(this)),true,true);
}
function StringTrimLeft(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.trimLeft");
return %StringTrim((%_ToString(this)),true,false);
}
function StringTrimRight(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.trimRight");
return %StringTrim((%_ToString(this)),false,true);
}
function HtmlEscape(ao){
return %_Call(StringReplace,(%_ToString(ao)),/"/g,"&quot;");
}
function StringAnchor(ap){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.anchor");
return"<a name=\""+HtmlEscape(ap)+"\">"+(%_ToString(this))+
"</a>";
}
function StringBig(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.big");
return"<big>"+(%_ToString(this))+"</big>";
}
function StringBlink(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.blink");
return"<blink>"+(%_ToString(this))+"</blink>";
}
function StringBold(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.bold");
return"<b>"+(%_ToString(this))+"</b>";
}
function StringFixed(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.fixed");
return"<tt>"+(%_ToString(this))+"</tt>";
}
function StringFontcolor(aq){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.fontcolor");
return"<font color=\""+HtmlEscape(aq)+"\">"+(%_ToString(this))+
"</font>";
}
function StringFontsize(ar){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.fontsize");
return"<font size=\""+HtmlEscape(ar)+"\">"+(%_ToString(this))+
"</font>";
}
function StringItalics(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.italics");
return"<i>"+(%_ToString(this))+"</i>";
}
function StringLink(w){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.link");
return"<a href=\""+HtmlEscape(w)+"\">"+(%_ToString(this))+"</a>";
}
function StringSmall(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.small");
return"<small>"+(%_ToString(this))+"</small>";
}
function StringStrike(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.strike");
return"<strike>"+(%_ToString(this))+"</strike>";
}
function StringSub(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.sub");
return"<sub>"+(%_ToString(this))+"</sub>";
}
function StringSup(){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.sup");
return"<sup>"+(%_ToString(this))+"</sup>";
}
function StringRepeat(as){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.repeat");
var w=(%_ToString(this));
var an=(%_ToInteger(as));
if(an<0||an===(1/0))throw j(167);
if(w.length===0)return"";
if(an>%_MaxSmi())throw j(167);
var at="";
while(true){
if(an&1)at+=w;
an>>=1;
if(an===0)return at;
w+=w;
}
}
function StringStartsWith(au,A){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.startsWith");
var w=(%_ToString(this));
if(i(au)){
throw k(40,"String.prototype.startsWith");
}
var av=(%_ToString(au));
var t=(%_ToInteger(A));
var ae=w.length;
var R=m(l(t,0),ae);
var aw=av.length;
if(aw+R>ae){
return false;
}
return %_SubString(w,R,R+aw)===av;
}
%FunctionSetLength(StringStartsWith,1);
function StringEndsWith(au,A){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.endsWith");
var w=(%_ToString(this));
if(i(au)){
throw k(40,"String.prototype.endsWith");
}
var av=(%_ToString(au));
var ae=w.length;
var t=!(A===(void 0))?(%_ToInteger(A)):ae
var S=m(l(t,0),ae);
var aw=av.length;
var R=S-aw;
if(R<0){
return false;
}
return %_SubString(w,R,R+aw)===av;
}
%FunctionSetLength(StringEndsWith,1);
function StringIncludes(au,A){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.includes");
var T=(%_ToString(this));
if(i(au)){
throw k(40,"String.prototype.includes");
}
au=(%_ToString(au));
var t=(%_ToInteger(A));
var ax=T.length;
if(t<0)t=0;
if(t>ax)t=ax;
var ay=au.length;
if(ay+t>ax){
return false;
}
return %StringIndexOf(T,au,t)!==-1;
}
%FunctionSetLength(StringIncludes,1);
function StringCodePointAt(t){
if((%IS_VAR(this)===null)||(this===(void 0)))throw k(18,"String.prototype.codePointAt");
var T=(%_ToString(this));
var ar=T.length;
t=(%_ToInteger(t));
if(t<0||t>=ar){
return(void 0);
}
var az=%_StringCharCodeAt(T,t);
if(az<0xD800||az>0xDBFF||t+1==ar){
return az;
}
var aA=%_StringCharCodeAt(T,t+1);
if(aA<0xDC00||aA>0xDFFF){
return az;
}
return(az-0xD800)*0x400+aA+0x2400;
}
function StringFromCodePoint(aB){
"use strict";
var aC;
var V=arguments.length;
var C;
var u="";
for(C=0;C<V;C++){
aC=arguments[C];
if(!%_IsSmi(aC)){
aC=(%_ToNumber(aC));
}
if(aC<0||aC>0x10FFFF||aC!==(%_ToInteger(aC))){
throw j(166,aC);
}
if(aC<=0xFFFF){
u+=%_StringCharFromCode(aC);
}else{
aC-=0x10000;
u+=%_StringCharFromCode((aC>>>10)&0x3FF|0xD800);
u+=%_StringCharFromCode(aC&0x3FF|0xDC00);
}
}
return u;
}
function StringRaw(aD){
"use strict";
var aE=arguments.length;
var aF=(%_ToObject(aD));
var aG=(%_ToObject(aF.raw));
var aH=(%_ToLength(aG.length));
if(aH<=0)return"";
var u=(%_ToString(aG[0]));
for(var y=1;y<aH;++y){
if(y<aE){
u+=(%_ToString(arguments[y]));
}
u+=(%_ToString(aG[y]));
}
return u;
}
%FunctionSetPrototype(f,new f());
%AddNamedProperty(
f.prototype,"constructor",f,2);
b.InstallFunctions(f,2,[
"fromCodePoint",StringFromCodePoint,
"raw",StringRaw
]);
b.InstallFunctions(f.prototype,2,[
"valueOf",StringValueOf,
"toString",StringToString,
"charAt",StringCharAtJS,
"charCodeAt",StringCharCodeAtJS,
"codePointAt",StringCodePointAt,
"concat",StringConcat,
"endsWith",StringEndsWith,
"includes",StringIncludes,
"indexOf",StringIndexOf,
"lastIndexOf",StringLastIndexOf,
"localeCompare",StringLocaleCompareJS,
"match",StringMatchJS,
"normalize",StringNormalize,
"repeat",StringRepeat,
"replace",StringReplace,
"search",StringSearch,
"slice",StringSlice,
"split",StringSplitJS,
"substring",StringSubstring,
"substr",StringSubstr,
"startsWith",StringStartsWith,
"toLowerCase",StringToLowerCaseJS,
"toLocaleLowerCase",StringToLocaleLowerCase,
"toUpperCase",StringToUpperCaseJS,
"toLocaleUpperCase",StringToLocaleUpperCase,
"trim",StringTrimJS,
"trimLeft",StringTrimLeft,
"trimRight",StringTrimRight,
"link",StringLink,
"anchor",StringAnchor,
"fontcolor",StringFontcolor,
"fontsize",StringFontsize,
"big",StringBig,
"blink",StringBlink,
"bold",StringBold,
"fixed",StringFixed,
"italics",StringItalics,
"small",StringSmall,
"strike",StringStrike,
"sub",StringSub,
"sup",StringSup
]);
b.Export(function(aI){
aI.ExpandReplacement=ExpandReplacement;
aI.StringCharAt=StringCharAtJS;
aI.StringIndexOf=StringIndexOf;
aI.StringLastIndexOf=StringLastIndexOf;
aI.StringMatch=StringMatchJS;
aI.StringReplace=StringReplace;
aI.StringSlice=StringSlice;
aI.StringSplit=StringSplitJS;
aI.StringSubstr=StringSubstr;
aI.StringSubstring=StringSubstring;
});
})

