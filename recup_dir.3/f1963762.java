f((%IS_VAR(k)===null)||(k===(void 0)))throw f(18,'String.prototype[Symbol.iterator]');
var l=(%_ToString(k));
var m=new StringIterator;
(m[g]=l);
(m[h]=0);
return m;
}
function StringIteratorNext(){
var m=this;
var n=(void 0);
var o=true;
if(!(%_IsJSReceiver(m))||
!(!(m[h]===(void 0)))){
throw f(44,
'String Iterator.prototype.next');
}
var l=(m[g]);
if(!(l===(void 0))){
var p=(m[h]);
var q=((l.length)>>>0);
if(p>=q){
(m[g]=(void 0));
}else{
var r=%_StringCharCodeAt(l,p);
n=%_StringCharFromCode(r);
o=false;
p++;
if(r>=0xD800&&r<=0xDBFF&&p<q){
var s=%_StringCharCodeAt(l,p);
if(s>=0xDC00&&s<=0xDFFF){
n+=%_StringCharFromCode(s);
p++;
}
}
(m[h]=p);
}
}
return %_CreateIterResultObject(n,o);
}
function StringPrototypeIterator(){
return CreateStringIterator(this);
}
%FunctionSetPrototype(StringIterator,{__proto__:d});
%FunctionSetInstanceClassName(StringIterator,'String Iterator');
b.InstallFunctions(StringIterator.prototype,2,[
'next',StringIteratorNext
]);
%AddNamedProperty(StringIterator.prototype,i,
"String Iterator",1|2);
b.SetFunctionName(StringPrototypeIterator,e);
%AddNamedProperty(c.prototype,e,
StringPrototypeIterator,2);
})

$templates