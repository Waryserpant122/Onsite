if(!(%_IsJSReceiver(p))||
!(!(p[d]===(void 0)))){
throw i(44,
'Array Iterator.prototype.next',this);
}
var m=(p[e]);
if(!(m===(void 0))){
var s=(p[d]);
var t=(p[c]);
var u=((m.length)>>>0);
if(s>=u){
(p[e]=(void 0));
}else{
(p[d]=s+1);
if(t==2){
q=m[s];
}else if(t==3){
q=[s,m[s]];
}else{
q=s;
}
r=false;
}
}
return %_CreateIterResultObject(q,r);
}
function ArrayEntries(){
return CreateArrayIterator(this,3);
}
function ArrayValues(){
return CreateArrayIterator(this,2);
}
function ArrayKeys(){
return CreateArrayIterator(this,1);
}
function TypedArrayEntries(){
if(!(%_IsTypedArray(this)))throw i(72);
return %_Call(ArrayEntries,this);
}
function TypedArrayValues(){
if(!(%_IsTypedArray(this)))throw i(72);
return %_Call(ArrayValues,this);
}
function TypedArrayKeys(){
if(!(%_IsTypedArray(this)))throw i(72);
return %_Call(ArrayKeys,this);
}
%FunctionSetPrototype(ArrayIterator,{__proto__:g});
%FunctionSetInstanceClassName(ArrayIterator,'Array Iterator');
b.InstallFunctions(ArrayIterator.prototype,2,[
'next',ArrayIteratorNext
]);
b.SetFunctionName(ArrayIteratorIterator,h);
%AddNamedProperty(ArrayIterator.prototype,j,
"Array Iterator",1|2);
b.InstallFunctions(f.prototype,2,[
'entries',ArrayEntries,
'keys',ArrayKeys
]);
b.SetFunctionName(ArrayValues,'values');
%AddNamedProperty(f.prototype,h,ArrayValues,
2);
b.InstallFunctions(k.prototype,2,[
'entries',TypedArrayEntries,
'keys',TypedArrayKeys,
'values',TypedArrayValues
]);
%AddNamedProperty(k.prototype,
h,TypedArrayValues,2);
b.Export(function(v){
v.ArrayValues=ArrayValues;
});
%InstallToContext(["array_values_iterator",ArrayValues]);
})

<string-iterator•