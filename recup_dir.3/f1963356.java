ions(k,m,p){
%CheckIsBootstrapping();
%OptimizeObjectForAddingMultipleProperties(k,p.length>>1);
for(var n=0;n<p.length;n+=2){
var q=p[n];
var h=p[n+1];
SetFunctionName(h,q);
%FunctionRemovePrototype(h);
%AddNamedProperty(k,q,h,m);
%SetNativeFlag(h);
}
%ToFastProperties(k);
}
function InstallGetter(k,i,r,m,j){
%CheckIsBootstrapping();
if((m===(void 0)))m=2;
SetFunctionName(r,i,(j===(void 0))?"get":j);
%FunctionRemovePrototype(r);
%DefineGetterPropertyUnchecked(k,i,r,m);
%SetNativeFlag(r);
}
function InstallGetterSetter(k,i,r,s,m){
%CheckIsBootstrapping();
if((m===(void 0)))m=2;
SetFunctionName(r,i,"get");
SetFunctionName(s,i,"set");
%FunctionRemovePrototype(r);
%FunctionRemovePrototype(s);
%DefineAccessorPropertyUnchecked(k,i,r,s,2);
%SetNativeFlag(r);
%SetNativeFlag(s);
}
function OverrideFunction(k,i,h,t){
%CheckIsBootstrapping();
%ObjectDefineProperty(k,i,{value:h,
writeable:true,
configurable:true,
enumerable:false});
SetFunctionName(h,i);
if(!t)%FunctionRemovePrototype(h);
%SetNativeFlag(h);
}
function SetUpLockedPrototype(
constructor,fields,methods){
%CheckIsBootstrapping();
var u=constructor.prototype;
var v=(methods.length>>1)+(fields?fields.length:0);
if(v>=4){
%OptimizeObjectForAddingMultipleProperties(u,v);
}
if(fields){
for(var n=0;n<fields.length;n++){
%AddNamedProperty(u,fields[n],
(void 0),2|4);
}
}
for(var n=0;n<methods.length;n+=2){
var q=methods[n];
var h=methods[n+1];
%AddNamedProperty(u,q,h,2|4|1);
%SetNativeFlag(h);
}
%InternalSetPrototype(u,null);
%ToFastProperties(u);
}
function PostNatives(b){
%CheckIsBootstrapping();
for(;!(d===(void 0));d=d.next){
d(f);
}
var w=[
"ArrayToString",
"ErrorToString",
"GetIterator",
"GetMethod",
"IsNaN",
"MakeError",
"MakeRangeError",
"MakeTypeError",
"MapEntries",
"MapIterator",
"MapIteratorNext",
"MaxSimple",
"MinSimple",
"NumberIsInteger",
"ObjectDefineProperty",
"ObserveArrayMethods",
"ObserveObjectMethods",
"PromiseChain",
"PromiseDeferred",
"PromiseResolved",
"RegExpSubclassExecJS",
"RegExpSubclassMatch",
"RegExpSubclassReplace",
"RegExpSubclassSearch",
"RegExpSubclassSplit",
"RegExpSubclassTest",
"SetIterator",
"SetIteratorNext",
"SetValues",
"SymbolToString",
"ToPositiveInteger",
"is_concat_spreadable_symbol",
"iterator_symbol",
"promise_status_symbol",
"promise_value_symbol",
"object_freeze",
"object_is_frozen",
"object_is_sealed",
"reflect_apply",
"reflect_construct",
"regexp_flags_symbol",
"to_string_tag_symbol",
"object_to_string",
"species_symbol",
"match_symbol",
"replace_symbol",
"search_symbol",
"split_symbol",
];
var x={};
%OptimizeObjectForAddingMultipleProperties(
x,w.length);
for(var q of w){
x[q]=f[q];
}
%ToFastProperties(x);
f=x;
b.PostNatives=(void 0);
b.ImportFromExperimental=(void 0);
}
function PostExperimentals(b){
%CheckIsBootstrapping();
%ExportExperimentalFromRuntime(f);
for(;!(d===(void 0));d=d.next){
d(f);
}
for(;!(e===(void 0));
e=e.next){
e(f);
}
b.CreateDoubleResultArray();
b.CreateDoubleResultArray=(void 0);
b.Export=(void 0);
b.PostDebug=(void 0);
b.PostExperimentals=(void 0);
g=(void 0);
}
function PostDebug(b){
for(;!(d===(void 0));d=d.next){
d(f);
}
b.CreateDoubleResultArray();
b.CreateDoubleResultArray=(void 0);
f=(void 0);
b.Export=(void 0);
b.Import=(void 0);
b.ImportNow=(void 0);
b.PostDebug=(void 0);
b.PostExperimentals=(void 0);
g=(void 0);
}
function InitializeBuiltinTypedArrays(b,y,z){
var A=g;
for(;!(A===(void 0));A=A.next){
A(y,z);
}
}
%OptimizeObjectForAddingMultipleProperties(b,14);
b.Import=Import;
b.ImportNow=ImportNow;
b.Export=Export;
b.ImportFromExperimental=ImportFromExperimental;
b.SetFunctionName=SetFunctionName;
b.InstallConstants=InstallConstants;
b.InstallFunctions=InstallFunctions;
b.InstallGetter=InstallGetter;
b.InstallGetterSetter=InstallGetterSetter;
b.OverrideFunction=OverrideFunction;
b.SetUpLockedPrototype=SetUpLockedPrototype;
b.PostNatives=PostNatives;
b.PostExperimentals=PostExperimentals;
b.PostDebug=PostDebug;
%ToFastProperties(b);
%OptimizeObjectForAddingMultipleProperties(c,5);
c.logStackTrace=function logStackTrace(){
%DebugTrace();
};
c.log=function log(){
let message='';
for(const arg of arguments){
message+=arg;
}
%GlobalPrint(message);
};
c.createPrivateSymbol=function createPrivateSymbol(i){
return %CreatePrivateSymbol(i);
};
c.simpleBind=function simpleBind(B,C){
return function(...args){
return %reflect_apply(B,C,args);
};
};
c.uncurryThis=function uncurryThis(B){
return function(C,...args){
return %reflect_apply(B,C,args);
};
};
%ToFastProperties(c);
})

