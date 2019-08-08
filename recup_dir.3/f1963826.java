
if(j>0){
if(%_DebugIsActive()!=0)%DebugPrepareStepInIfStepping(this);
return %_GeneratorReturn(this,i);
}else if(j==0){
return %_CreateIterResultObject(i,true);
}else{
throw f(42);
}
}
function GeneratorObjectThrow(k){
if(!(%_ClassOf(this)==='Generator')){
throw f(44,
'[Generator].prototype.throw',this);
}
var j=%GeneratorGetContinuation(this);
if(j>0){
if(%_DebugIsActive()!=0)%DebugPrepareStepInIfStepping(this);
return %_GeneratorThrow(this,k);
}else if(j==0){
throw k;
}else{
throw f(42);
}
}
%NeverOptimizeFunction(GeneratorObjectNext);
%NeverOptimizeFunction(GeneratorObjectReturn);
%NeverOptimizeFunction(GeneratorObjectThrow);
var l=c.prototype;
b.InstallFunctions(l,
2,
["next",GeneratorObjectNext,
"return",GeneratorObjectReturn,
"throw",GeneratorObjectThrow]);
%AddNamedProperty(l,"constructor",
c,2|1);
%AddNamedProperty(l,
g,"Generator",2|1);
%InternalSetPrototype(c,e.prototype);
%AddNamedProperty(c,
g,"GeneratorFunction",2|1);
%AddNamedProperty(c,"constructor",
d,2|1);
%InternalSetPrototype(d,e);
})

<harmony-atomicsu8
(function(a,b){
"use strict";
%CheckIsBootstrapping();
var c=a.Object;
var d;
var e;
var f;
var g=b.ImportNow("to_string_tag_symbol");
b.Import(function(h){
e=h.MakeTypeError;
d=h.MakeRangeError;
f=h.MaxSimple;
});
function CheckSharedIntegerTypedArray(i){
if(!%IsSharedIntegerTypedArray(i)){
throw e(74,i);
}
}
function CheckSharedInteger32TypedArray(i){
CheckSharedIntegerTypedArray(i);
if(!%IsSharedInteger32TypedArray(i)){
throw e(75,i);
}
}
function ValidateIndex(j,k){
var l=(%_ToNumber(j));
var m=(%_ToInteger(l));
if(l!==m){
throw d(165);
}
if(m<0||m>=k){
throw d(165);
}
return m;
}
function AtomicsCompareExchangeJS(n,j,o,p){
CheckSharedIntegerTypedArray(n);
j=ValidateIndex(j,%_TypedArrayGetLength(n));
o=(%_ToNumber(o));
p=(%_ToNumber(p));
return %_AtomicsCompareExchange(n,j,o,p);
}
function AtomicsLoadJS(n,j){
CheckSharedIntegerTypedArray(n);
j=ValidateIndex(j,%_TypedArrayGetLength(n));
return %_AtomicsLoad(n,j);
}
function AtomicsStoreJS(n,j,q){
CheckSharedIntegerTypedArray(n);
j=ValidateIndex(j,%_TypedArrayGetLength(n));
q=(%_ToNumber(q));
return %_AtomicsStore(n,j,q);
}
function AtomicsAddJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsAdd(i,j,q);
}
function AtomicsSubJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsSub(i,j,q);
}
function AtomicsAndJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsAnd(i,j,q);
}
function AtomicsOrJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsOr(i,j,q);
}
function AtomicsXorJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsXor(i,j,q);
}
function AtomicsExchangeJS(i,j,q){
CheckSharedIntegerTypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
q=(%_ToNumber(q));
return %_AtomicsExchange(i,j,q);
}
function AtomicsIsLockFreeJS(r){
return %_AtomicsIsLockFree(r);
}
function AtomicsFutexWaitJS(i,j,q,s){
CheckSharedInteger32TypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
if((s===(void 0))){
s=(1/0);
}else{
s=(%_ToNumber(s));
if((!%_IsSmi(%IS_VAR(s))&&!(s==s))){
s=(1/0);
}else{
s=f(0,s);
}
}
return %AtomicsFutexWait(i,j,q,s);
}
function AtomicsFutexWakeJS(i,j,t){
CheckSharedInteger32TypedArray(i);
j=ValidateIndex(j,%_TypedArrayGetLength(i));
t=f(0,(%_ToInteger(t)));
return %AtomicsFutexWake(i,j,t);
}
function AtomicsFutexWakeOrRequeueJS(i,u,t,q,v){
CheckSharedInteger32TypedArray(i);
u=ValidateIndex(u,%_TypedArrayGetLength(i));
t=f(0,(%_ToInteger(t)));
q=((q)|0);
v=ValidateIndex(v,%_TypedArrayGetLength(i));
if(u<0||u>=%_TypedArrayGetLength(i)||
v<0||v>=%_TypedArrayGetLength(i)){
return(void 0);
}
return %AtomicsFutexWakeOrRequeue(i,u,t,q,v);
}
function AtomicsConstructor(){}
var w=new AtomicsConstructor();
%InternalSetPrototype(w,c.prototype);
%AddNamedProperty(a,"Atomics",w,2);
%FunctionSetInstanceClassName(AtomicsConstructor,'Atomics');
%AddNamedProperty(w,g,"Atomics",1|2);
b.InstallConstants(w,[
"OK",0,
"NOTEQUAL",-1,
"TIMEDOUT",-2,
]);
b.InstallFunctions(w,2,[
"compareExchange",AtomicsCompareExchangeJS,
"load",AtomicsLoadJS,
"store",AtomicsStoreJS,
"add",AtomicsAddJS,
"sub",AtomicsSubJS,
"and",AtomicsAndJS,
"or",AtomicsOrJS,
"xor",AtomicsXorJS,
"exchange",AtomicsExchangeJS,
"isLockFree",AtomicsIsLockFreeJS,
"futexWait",AtomicsFutexWaitJS,
"futexWake",AtomicsFutexWakeJS,
"futexWakeOrRequeue",AtomicsFutexWakeOrRequeueJS,
]);
})

Lharmony-regexp-execa