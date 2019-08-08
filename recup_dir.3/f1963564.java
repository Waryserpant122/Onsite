ator_symbol");
var M=b.ImportNow("to_string_tag_symbol");
var N=a.Uint8Array;

var O=a.Int8Array;

var P=a.Uint16Array;

var Q=a.Int16Array;

var R=a.Uint32Array;

var S=a.Int32Array;

var T=a.Float32Array;

var U=a.Float64Array;

var V=a.Uint8ClampedArray;


b.Import(function(W){
c=W.AddIndexedProperty;
e=W.ArrayValues;
f=W.GetIterator;
g=W.GetMethod;
n=W.InnerArrayCopyWithin;
o=W.InnerArrayEvery;
p=W.InnerArrayFill;
q=W.InnerArrayFilter;
r=W.InnerArrayFind;
s=W.InnerArrayFindIndex;
t=W.InnerArrayForEach;
u=W.InnerArrayIncludes;
v=W.InnerArrayIndexOf;
w=W.InnerArrayJoin;
x=W.InnerArrayLastIndexOf;
y=W.InnerArrayReduce;
z=W.InnerArrayReduceRight;
A=W.InnerArraySome;
B=W.InnerArraySort;
C=W.InnerArrayToLocaleString;
D=W.IsNaN;
E=W.MakeRangeError;
F=W.MakeTypeError;
G=W.MaxSimple;
H=W.MinSimple;
I=W.PackedArrayReverse;
J=W.SpeciesConstructor;
K=W.ToPositiveInteger;
});
function TypedArrayDefaultConstructor(X){
switch(%_ClassOf(X)){
case"Uint8Array":
return N;

case"Int8Array":
return O;

case"Uint16Array":
return P;

case"Int16Array":
return Q;

case"Uint32Array":
return R;

case"Int32Array":
return S;

case"Float32Array":
return T;

case"Float64Array":
return U;

case"Uint8ClampedArray":
return V;


}
throw F(44,
"TypedArrayDefaultConstructor",this);
}
function TypedArrayCreate(Y,Z,aa,ab){
if((aa===(void 0))){
var ac=new Y(Z);
}else{
var ac=new Y(Z,aa,ab);
}
if(!(%_IsTypedArray(ac)))throw F(72);
if((typeof(Z)==='number')&&%_TypedArrayGetLength(ac)<Z){
throw F(253);
}
return ac;
}
function TypedArraySpeciesCreate(ad,Z,aa,ab,ae){
var af=TypedArrayDefaultConstructor(ad);
var Y=J(ad,af,
ae);
return TypedArrayCreate(Y,Z,aa,ab);
}
function Uint8ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 1!==0){
throw E(178,
"start offset","Uint8Array",1);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 1!==0){
throw E(178,
"byte length","Uint8Array",1);
}
am=ak-al;
an=am/1;
}else{
var an=aj;
am=an*1;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,1,ah,al,am,true);
}
function Uint8ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*1;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,1,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,1,null,0,ap,true);
}
}
function Uint8ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*1;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,1,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,1,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Uint8ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Uint8ArrayConstructByArrayLike(ag,av,av.length);
}
function Uint8ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*1;
Uint8ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Uint8ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Uint8ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Uint8ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Uint8ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Uint8ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Uint8ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Uint8Array")
}
}
function Uint8ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*1;
return new N(%TypedArrayGetBuffer(this),aI,an);
}

function Int8ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 1!==0){
throw E(178,
"start offset","Int8Array",1);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 1!==0){
throw E(178,
"byte length","Int8Array",1);
}
am=ak-al;
an=am/1;
}else{
var an=aj;
am=an*1;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,2,ah,al,am,true);
}
function Int8ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*1;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,2,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,2,null,0,ap,true);
}
}
function Int8ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*1;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,2,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,2,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Int8ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Int8ArrayConstructByArrayLike(ag,av,av.length);
}
function Int8ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*1;
Int8ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Int8ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Int8ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Int8ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Int8ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Int8ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Int8ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Int8Array")
}
}
function Int8ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*1;
return new O(%TypedArrayGetBuffer(this),aI,an);
}

function Uint16ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 2!==0){
throw E(178,
"start offset","Uint16Array",2);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 2!==0){
throw E(178,
"byte length","Uint16Array",2);
}
am=ak-al;
an=am/2;
}else{
var an=aj;
am=an*2;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,3,ah,al,am,true);
}
function Uint16ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*2;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,3,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,3,null,0,ap,true);
}
}
function Uint16ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*2;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,3,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,3,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Uint16ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Uint16ArrayConstructByArrayLike(ag,av,av.length);
}
function Uint16ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*2;
Uint16ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Uint16ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Uint16ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Uint16ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Uint16ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Uint16ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Uint16ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Uint16Array")
}
}
function Uint16ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*2;
return new P(%TypedArrayGetBuffer(this),aI,an);
}

function Int16ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 2!==0){
throw E(178,
"start offset","Int16Array",2);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 2!==0){
throw E(178,
"byte length","Int16Array",2);
}
am=ak-al;
an=am/2;
}else{
var an=aj;
am=an*2;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,4,ah,al,am,true);
}
function Int16ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*2;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,4,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,4,null,0,ap,true);
}
}
function Int16ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*2;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,4,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,4,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Int16ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Int16ArrayConstructByArrayLike(ag,av,av.length);
}
function Int16ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*2;
Int16ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Int16ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Int16ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Int16ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Int16ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Int16ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Int16ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Int16Array")
}
}
function Int16ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*2;
return new Q(%TypedArrayGetBuffer(this),aI,an);
}

function Uint32ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 4!==0){
throw E(178,
"start offset","Uint32Array",4);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 4!==0){
throw E(178,
"byte length","Uint32Array",4);
}
am=ak-al;
an=am/4;
}else{
var an=aj;
am=an*4;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,5,ah,al,am,true);
}
function Uint32ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*4;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,5,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,5,null,0,ap,true);
}
}
function Uint32ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*4;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,5,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,5,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Uint32ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Uint32ArrayConstructByArrayLike(ag,av,av.length);
}
function Uint32ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*4;
Uint32ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Uint32ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Uint32ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Uint32ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Uint32ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Uint32ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Uint32ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Uint32Array")
}
}
function Uint32ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*4;
return new R(%TypedArrayGetBuffer(this),aI,an);
}

function Int32ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 4!==0){
throw E(178,
"start offset","Int32Array",4);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 4!==0){
throw E(178,
"byte length","Int32Array",4);
}
am=ak-al;
an=am/4;
}else{
var an=aj;
am=an*4;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,6,ah,al,am,true);
}
function Int32ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*4;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,6,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,6,null,0,ap,true);
}
}
function Int32ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*4;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,6,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,6,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Int32ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Int32ArrayConstructByArrayLike(ag,av,av.length);
}
function Int32ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*4;
Int32ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Int32ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Int32ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Int32ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Int32ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Int32ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Int32ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Int32Array")
}
}
function Int32ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*4;
return new S(%TypedArrayGetBuffer(this),aI,an);
}

function Float32ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 4!==0){
throw E(178,
"start offset","Float32Array",4);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 4!==0){
throw E(178,
"byte length","Float32Array",4);
}
am=ak-al;
an=am/4;
}else{
var an=aj;
am=an*4;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,7,ah,al,am,true);
}
function Float32ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*4;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,7,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,7,null,0,ap,true);
}
}
function Float32ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*4;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,7,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,7,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Float32ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Float32ArrayConstructByArrayLike(ag,av,av.length);
}
function Float32ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*4;
Float32ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Float32ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Float32ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Float32ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Float32ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Float32ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Float32ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Float32Array")
}
}
function Float32ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*4;
return new T(%TypedArrayGetBuffer(this),aI,an);
}

function Float64ArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 8!==0){
throw E(178,
"start offset","Float64Array",8);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 8!==0){
throw E(178,
"byte length","Float64Array",8);
}
am=ak-al;
an=am/8;
}else{
var an=aj;
am=an*8;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,8,ah,al,am,true);
}
function Float64ArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*8;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,8,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,8,null,0,ap,true);
}
}
function Float64ArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*8;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,8,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,8,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Float64ArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Float64ArrayConstructByArrayLike(ag,av,av.length);
}
function Float64ArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*8;
Float64ArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Float64ArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Float64ArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Float64ArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Float64ArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Float64ArrayConstructByArrayLike(this,aa,aa.length);
}else{
Float64ArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Float64Array")
}
}
function Float64ArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*8;
return new U(%TypedArrayGetBuffer(this),aI,an);
}

function Uint8ClampedArrayConstructByArrayBuffer(ag,ah,ai,aj){
if(!(ai===(void 0))){
ai=K(ai,179);
}
if(!(aj===(void 0))){
aj=K(aj,179);
}
var ak=%_ArrayBufferGetByteLength(ah);
var al;
if((ai===(void 0))){
al=0;
}else{
al=ai;
if(al % 1!==0){
throw E(178,
"start offset","Uint8ClampedArray",1);
}
if(al>ak){
throw E(180);
}
}
var am;
var an;
if((aj===(void 0))){
if(ak % 1!==0){
throw E(178,
"byte length","Uint8ClampedArray",1);
}
am=ak-al;
an=am/1;
}else{
var an=aj;
am=an*1;
}
if((al+am>ak)
||(an>%_MaxSmi())){
throw E(179);
}
%_TypedArrayInitialize(ag,9,ah,al,am,true);
}
function Uint8ClampedArrayConstructByLength(ag,aj){
var ao=(aj===(void 0))?
0:K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ap=ao*1;
if(ap>%_TypedArrayMaxSizeInHeap()){
var ah=new i(ap);
%_TypedArrayInitialize(ag,9,ah,0,ap,true);
}else{
%_TypedArrayInitialize(ag,9,null,0,ap,true);
}
}
function Uint8ClampedArrayConstructByArrayLike(ag,aq,aj){
var ao=K(aj,179);
if(ao>%_MaxSmi()){
throw E(179);
}
var ar=false;
var ap=ao*1;
if(ap<=%_TypedArrayMaxSizeInHeap()){
%_TypedArrayInitialize(ag,9,null,0,ap,false);
}else{
ar=
%TypedArrayInitializeFromArrayLike(ag,9,aq,ao);
}
if(!ar){
for(var as=0;as<ao;as++){
ag[as]=aq[as];
}
}
}
function Uint8ClampedArrayConstructByIterable(ag,at,au){
var av=new m();
var aw=%_Call(au,at);
var ax={
__proto__:null
};
ax[L]=function(){return aw;}
for(var ay of ax){
av.push(ay);
}
Uint8ClampedArrayConstructByArrayLike(ag,av,av.length);
}
function Uint8ClampedArrayConstructByTypedArray(ag,X){
var az=%TypedArrayGetBuffer(X);
var aj=%_TypedArrayGetLength(X);
var ap=%_ArrayBufferViewGetByteLength(X);
var am=aj*1;
Uint8ClampedArrayConstructByArrayLike(ag,X,aj);
var aA=J(az,i);
var aB=aA.prototype;
if((%_IsJSReceiver(aB))&&aB!==j){
%InternalSetPrototype(%TypedArrayGetBuffer(ag),aB);
}
}
function Uint8ClampedArrayConstructor(aa,ab,aC){
if(!(new.target===(void 0))){
if((%_ClassOf(aa)==='ArrayBuffer')||(%_ClassOf(aa)==='SharedArrayBuffer')){
Uint8ClampedArrayConstructByArrayBuffer(this,aa,ab,aC);
}else if((typeof(aa)==='number')||(typeof(aa)==='string')||
(typeof(aa)==='boolean')||(aa===(void 0))){
Uint8ClampedArrayConstructByLength(this,aa);
}else if((%_IsTypedArray(aa))){
Uint8ClampedArrayConstructByTypedArray(this,aa);
}else{
var au=aa[L];
if((au===(void 0))||au===e){
Uint8ClampedArrayConstructByArrayLike(this,aa,aa.length);
}else{
Uint8ClampedArrayConstructByIterable(this,aa,au);
}
}
}else{
throw F(28,"Uint8ClampedArray")
}
}
function Uint8ClampedArraySubArray(aD,aE){
var aF=(%_ToInteger(aD));
if(!(aE===(void 0))){
var aG=(%_ToInteger(aE));
var aH=%_TypedArrayGetLength(this);
}else{
var aH=%_TypedArrayGetLength(this);
var aG=aH;
}
if(aF<0){
aF=G(0,aH+aF);
}else{
aF=H(aF,aH);
}
if(aG<0){
aG=G(0,aH+aG);
}else{
aG=H(aG,aH);
}
if(aG<aF){
aG=aF;
}
var an=aG-aF;
var aI=
%_ArrayBufferViewGetByteOffset(this)+aF*1;
return new V(%TypedArrayGetBuffer(this),aI,an);
}


function TypedArraySubArray(aD,aE){
switch(%_ClassOf(this)){
case"Uint8Array":
return %_Call(Uint8ArraySubArray,this,aD,aE);

case"Int8Array":
return %_Call(Int8ArraySubArray,this,aD,aE);

case"Uint16Array":
return %_Call(Uint16ArraySubArray,this,aD,aE);

case"Int16Array":
return %_Call(Int16ArraySubArray,this,aD,aE);

case"Uint32Array":
return %_Call(Uint32ArraySubArray,this,aD,aE);

case"Int32Array":
return %_Call(Int32ArraySubArray,this,aD,aE);

case"Float32Array":
return %_Call(Float32ArraySubArray,this,aD,aE);

case"Float64Array":
return %_Call(Float64ArraySubArray,this,aD,aE);

case"Uint8ClampedArray":
return %_Call(Uint8ClampedArraySubArray,this,aD,aE);


}
throw F(44,
"get TypedArray.prototype.subarray",this);
}
%SetForceInlineFlag(TypedArraySubArray);
function TypedArrayGetBuffer(){
if(!(%_IsTypedArray(this))){
throw F(44,
"get TypedArray.prototype.buffer",this);
}
return %TypedArrayGetBuffer(this);
}
%SetForceInlineFlag(TypedArrayGetBuffer);
function TypedArrayGetByteLength(){
if(!(%_IsTypedArray(this))){
throw F(44,
"get TypedArray.prototype.byteLength",this);
}
return %_ArrayBufferViewGetByteLength(this);
}
%SetForceInlineFlag(TypedArrayGetByteLength);
function TypedArrayGetByteOffset(){
if(!(%_IsTypedArray(this))){
throw F(44,
"get TypedArray.prototype.byteOffset",this);
}
return %_ArrayBufferViewGetByteOffset(this);
}
%SetForceInlineFlag(TypedArrayGetByteOffset);
function TypedArrayGetLength(){
if(!(%_IsTypedArray(this))){
throw F(44,
"get TypedArray.prototype.length",this);
}
return %_TypedArrayGetLength(this);
}
%SetForceInlineFlag(TypedArrayGetLength);
function TypedArraySetFromArrayLike(aJ,aK,aL,al){
if(al>0){
for(var as=0;as<aL;as++){
aJ[al+as]=aK[as];
}
}
else{
for(var as=0;as<aL;as++){
aJ[as]=aK[as];
}
}
}
function TypedArraySetFromOverlappingTypedArray(aJ,aK,al){
var aM=aK.BYTES_PER_ELEMENT;
var aN=aJ.BYTES_PER_ELEMENT;
var aL=aK.length;
function CopyLeftPart(){
var aO=aJ.byteOffset+(al+1)*aN;
var aP=aK.byteOffset;
for(var aQ=0;
aQ<aL&&aO<=aP;
aQ++){
aJ[al+aQ]=aK[aQ];
aO+=aN;
aP+=aM;
}
return aQ;
}
var aQ=CopyLeftPart();
function CopyRightPart(){
var aO=
aJ.byteOffset+(al+aL-1)*aN;
var aP=
aK.byteOffset+aL*aM;
for(var aR=aL-1;
aR>=aQ&&aO>=aP;
aR--){
aJ[al+aR]=aK[aR];
aO-=aN;
aP-=aM;
}
return aR;
}
var aR=CopyRightPart();
var aS=new h(aR+1-aQ);
for(var as=aQ;as<=aR;as++){
aS[as-aQ]=aK[as];
}
for(as=aQ;as<=aR;as++){
aJ[al+as]=aS[as-aQ];
}
}
function TypedArraySet(ag,al){
var aT=(al===(void 0))?0:(%_ToInteger(al));
if(aT<0)throw F(189);
if(aT>%_MaxSmi()){
throw E(190);
}
switch(%TypedArraySetFastCases(this,ag,aT)){
case 0:
return;
case 1:
TypedArraySetFromOverlappingTypedArray(this,ag,aT);
return;
case 2:
TypedArraySetFromArrayLike(this,ag,ag.length,aT);
return;
case 3:
var ao=ag.length;
if((ao===(void 0))){
if((typeof(ag)==='number')){
throw F(47);
}
return;
}
ao=(%_ToLength(ao));
if(aT+ao>this.length){
throw E(190);
}
TypedArraySetFromArrayLike(this,ag,ao,aT);
return;
}
}
%FunctionSetLength(TypedArraySet,1);
function TypedArrayGetToStringTag(){
if(!(%_IsTypedArray(this)))return;
var aU=%_ClassOf(this);
if((aU===(void 0)))return;
return aU;
}
function TypedArrayCopyWithin(aJ,aV,aE){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return n(aJ,aV,aE,this,aj);
}
%FunctionSetLength(TypedArrayCopyWithin,2);
function TypedArrayEvery(aW,aX){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return o(aW,aX,this,aj);
}
%FunctionSetLength(TypedArrayEvery,1);
function TypedArrayForEach(aW,aX){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
t(aW,aX,this,aj);
}
%FunctionSetLength(TypedArrayForEach,1);
function TypedArrayFill(ay,aV,aE){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return p(ay,aV,aE,this,aj);
}
%FunctionSetLength(TypedArrayFill,1);
function TypedArrayFilter(aW,aY){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
if(!(typeof(aW)==='function'))throw F(15,aW);
var aZ=new m();
q(aW,aY,this,aj,aZ);
var ba=aZ.length;
var bb=TypedArraySpeciesCreate(this,ba);
for(var as=0;as<ba;as++){
bb[as]=aZ[as];
}
return bb;
}
%FunctionSetLength(TypedArrayFilter,1);
function TypedArrayFind(bc,aY){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return r(bc,aY,this,aj);
}
%FunctionSetLength(TypedArrayFind,1);
function TypedArrayFindIndex(bc,aY){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return s(bc,aY,this,aj);
}
%FunctionSetLength(TypedArrayFindIndex,1);
function TypedArrayReverse(){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return I(this,aj);
}
function TypedArrayComparefn(bd,be){
if(bd===0&&bd===be){
bd=1/bd;
be=1/be;
}
if(bd<be){
return-1;
}else if(bd>be){
return 1;
}else if(D(bd)&&D(be)){
return D(be)?0:1;
}else if(D(bd)){
return 1;
}
return 0;
}
function TypedArraySort(bf){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
if((bf===(void 0))){
bf=TypedArrayComparefn;
}
return B(this,aj,bf);
}
function TypedArrayIndexOf(bg,bh){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return v(this,bg,bh,aj);
}
%FunctionSetLength(TypedArrayIndexOf,1);
function TypedArrayLastIndexOf(bg,bh){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return x(this,bg,bh,aj,
arguments.length);
}
%FunctionSetLength(TypedArrayLastIndexOf,1);
function TypedArrayMap(aW,aY){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
var aZ=TypedArraySpeciesCreate(this,aj);
if(!(typeof(aW)==='function'))throw F(15,aW);
for(var as=0;as<aj;as++){
var bg=this[as];
aZ[as]=%_Call(aW,aY,bg,as,this);
}
return aZ;
}
%FunctionSetLength(TypedArrayMap,1);
function TypedArraySome(aW,aX){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return A(aW,aX,this,aj);
}
%FunctionSetLength(TypedArraySome,1);
function TypedArrayToLocaleString(){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return C(this,aj);
}
function TypedArrayJoin(bi){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return w(bi,this,aj);
}
function TypedArrayReduce(bj,bk){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return y(bj,bk,this,aj,
arguments.length);
}
%FunctionSetLength(TypedArrayReduce,1);
function TypedArrayReduceRight(bj,bk){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return z(bj,bk,this,aj,
arguments.length);
}
%FunctionSetLength(TypedArrayReduceRight,1);
function TypedArraySlice(aV,aE){
if(!(%_IsTypedArray(this)))throw F(72);
var bl=%_TypedArrayGetLength(this);
var bm=(%_ToInteger(aV));
var bn;
if(bm<0){
bn=G(bl+bm,0);
}else{
bn=H(bm,bl);
}
var bo;
if((aE===(void 0))){
bo=bl;
}else{
bo=(%_ToInteger(aE));
}
var bp;
if(bo<0){
bp=G(bl+bo,0);
}else{
bp=H(bo,bl);
}
var bq=G(bp-bn,0);
var br=TypedArraySpeciesCreate(this,bq);
var bs=0;
while(bn<bp){
var bt=this[bn];
br[bs]=bt;
bn++;
bs++;
}
return br;
}
function TypedArrayIncludes(bu,bv){
if(!(%_IsTypedArray(this)))throw F(72);
var aj=%_TypedArrayGetLength(this);
return u(bu,bv,this,aj);
}
%FunctionSetLength(TypedArrayIncludes,1);
function TypedArrayOf(){
var aj=arguments.length;
var br=TypedArrayCreate(this,aj);
for(var as=0;as<aj;as++){
br[as]=arguments[as];
}
return br;
}
function IterableToArrayLike(bw){
var at=g(bw,L);
if(!(at===(void 0))){
var bx=new m();
var as=0;
for(var ay of
{[L](){return f(bw,at)}}){
bx[as]=ay;
as++;
}
var br=[];
%MoveArrayContents(bx,br);
return br;
}
return(%_ToObject(bw));
}
function TypedArrayFrom(aK,by,aY){
if(!%IsConstructor(this))throw F(66,this);
var bz;
if(!(by===(void 0))){
if(!(typeof(by)==='function'))throw F(15,this);
bz=true;
}else{
bz=false;
}
var aq=IterableToArrayLike(aK);
var aj=(%_ToLength(aq.length));
var bA=TypedArrayCreate(this,aj);
var ay,bB;
for(var as=0;as<aj;as++){
ay=aq[as];
if(bz){
bB=%_Call(by,aY,ay,as);
}else{
bB=ay;
}
bA[as]=bB;
}
return bA;
}
%FunctionSetLength(TypedArrayFrom,1);
function TypedArray(){
if((new.target===(void 0))){
throw F(27,"TypedArray");
}
if(new.target===TypedArray){
throw F(25,"TypedArray");
}
}
%FunctionSetPrototype(TypedArray,new l());
%AddNamedProperty(TypedArray.prototype,
"constructor",TypedArray,2);
b.InstallFunctions(TypedArray,2,[
"from",TypedArrayFrom,
"of",TypedArrayOf
]);
b.InstallGetter(TypedArray.prototype,"buffer",TypedArrayGetBuffer);
b.InstallGetter(TypedArray.prototype,"byteOffset",TypedArrayGetByteOffset,
2|4);
b.InstallGetter(TypedArray.prototype,"byteLength",
TypedArrayGetByteLength,2|4);
b.InstallGetter(TypedArray.prototype,"length",TypedArrayGetLength,
2|4);
b.InstallGetter(TypedArray.prototype,M,
TypedArrayGetToStringTag);
b.InstallFunctions(TypedArray.prototype,2,[
"subarray",TypedArraySubArray,
"set",TypedArraySet,
"copyWithin",TypedArrayCopyWithin,
"every",TypedArrayEvery,
"fill",TypedArrayFill,
"filter",TypedArrayFilter,
"find",TypedArrayFind,
"findIndex",TypedArrayFindIndex,
"includes",TypedArrayIncludes,
"indexOf",TypedArrayIndexOf,
"join",TypedArrayJoin,
"lastIndexOf",TypedArrayLastIndexOf,
"forEach",TypedArrayForEach,
"map",TypedArrayMap,
"reduce",TypedArrayReduce,
"reduceRight",TypedArrayReduceRight,
"reverse",TypedArrayReverse,
"slice",TypedArraySlice,
"some",TypedArraySome,
"sort",TypedArraySort,
"toLocaleString",TypedArrayToLocaleString
]);
%AddNamedProperty(TypedArray.prototype,"toString",d,
2);
%SetCode(N,Uint8ArrayConstructor);
%FunctionSetPrototype(N,new l());
%InternalSetPrototype(N,TypedArray);
%InternalSetPrototype(N.prototype,TypedArray.prototype);
%AddNamedProperty(N,"BYTES_PER_ELEMENT",1,
1|2|4);
%AddNamedProperty(N.prototype,
"constructor",a.Uint8Array,2);
%AddNamedProperty(N.prototype,
"BYTES_PER_ELEMENT",1,
1|2|4);

%SetCode(O,Int8ArrayConstructor);
%FunctionSetPrototype(O,new l());
%InternalSetPrototype(O,TypedArray);
%InternalSetPrototype(O.prototype,TypedArray.prototype);
%AddNamedProperty(O,"BYTES_PER_ELEMENT",1,
1|2|4);
%AddNamedProperty(O.prototype,
"constructor",a.Int8Array,2);
%AddNamedProperty(O.prototype,
"BYTES_PER_ELEMENT",1,
1|2|4);

%SetCode(P,Uint16ArrayConstructor);
%FunctionSetPrototype(P,new l());
%InternalSetPrototype(P,TypedArray);
%InternalSetPrototype(P.prototype,TypedArray.prototype);
%AddNamedProperty(P,"BYTES_PER_ELEMENT",2,
1|2|4);
%AddNamedProperty(P.prototype,
"constructor",a.Uint16Array,2);
%AddNamedProperty(P.prototype,
"BYTES_PER_ELEMENT",2,
1|2|4);

%SetCode(Q,Int16ArrayConstructor);
%FunctionSetPrototype(Q,new l());
%InternalSetPrototype(Q,TypedArray);
%InternalSetPrototype(Q.prototype,TypedArray.prototype);
%AddNamedProperty(Q,"BYTES_PER_ELEMENT",2,
1|2|4);
%AddNamedProperty(Q.prototype,
"constructor",a.Int16Array,2);
%AddNamedProperty(Q.prototype,
"BYTES_PER_ELEMENT",2,
1|2|4);

%SetCode(R,Uint32ArrayConstructor);
%FunctionSetPrototype(R,new l());
%InternalSetPrototype(R,TypedArray);
%InternalSetPrototype(R.prototype,TypedArray.prototype);
%AddNamedProperty(R,"BYTES_PER_ELEMENT",4,
1|2|4);
%AddNamedProperty(R.prototype,
"constructor",a.Uint32Array,2);
%AddNamedProperty(R.prototype,
"BYTES_PER_ELEMENT",4,
1|2|4);

%SetCode(S,Int32ArrayConstructor);
%FunctionSetPrototype(S,new l());
%InternalSetPrototype(S,TypedArray);
%InternalSetPrototype(S.prototype,TypedArray.prototype);
%AddNamedProperty(S,"BYTES_PER_ELEMENT",4,
1|2|4);
%AddNamedProperty(S.prototype,
"constructor",a.Int32Array,2);
%AddNamedProperty(S.prototype,
"BYTES_PER_ELEMENT",4,
1|2|4);

%SetCode(T,Float32ArrayConstructor);
%FunctionSetPrototype(T,new l());
%InternalSetPrototype(T,TypedArray);
%InternalSetPrototype(T.prototype,TypedArray.prototype);
%AddNamedProperty(T,"BYTES_PER_ELEMENT",4,
1|2|4);
%AddNamedProperty(T.prototype,
"constructor",a.Float32Array,2);
%AddNamedProperty(T.prototype,
"BYTES_PER_ELEMENT",4,
1|2|4);

%SetCode(U,Float64ArrayConstructor);
%FunctionSetPrototype(U,new l());
%InternalSetPrototype(U,TypedArray);
%InternalSetPrototype(U.prototype,TypedArray.prototype);
%AddNamedProperty(U,"BYTES_PER_ELEMENT",8,
1|2|4);
%AddNamedProperty(U.prototype,
"constructor",a.Float64Array,2);
%AddNamedProperty(U.prototype,
"BYTES_PER_ELEMENT",8,
1|2|4);

%SetCode(V,Uint8ClampedArrayConstructor);
%FunctionSetPrototype(V,new l());
%InternalSetPrototype(V,TypedArray);
%InternalSetPrototype(V.prototype,TypedArray.prototype);
%AddNamedProperty(V,"BYTES_PER_ELEMENT",1,
1|2|4);
%AddNamedProperty(V.prototype,
"constructor",a.Uint8ClampedArray,2);
%AddNamedProperty(V.prototype,
"BYTES_PER_ELEMENT",1,
1|2|4);


function DataViewGetBufferJS(){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,'DataView.buffer',this);
}
return %DataViewGetBuffer(this);
}
function DataViewGetByteOffset(){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.byteOffset',this);
}
return %_ArrayBufferViewGetByteOffset(this);
}
function DataViewGetByteLength(){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.byteLength',this);
}
return %_ArrayBufferViewGetByteLength(this);
}
function DataViewGetInt8JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getInt8',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetInt8(this,al,!!bC);
}
%FunctionSetLength(DataViewGetInt8JS,1);
function DataViewSetInt8JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setInt8',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetInt8(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetInt8JS,2);

function DataViewGetUint8JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getUint8',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetUint8(this,al,!!bC);
}
%FunctionSetLength(DataViewGetUint8JS,1);
function DataViewSetUint8JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setUint8',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetUint8(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetUint8JS,2);

function DataViewGetInt16JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getInt16',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetInt16(this,al,!!bC);
}
%FunctionSetLength(DataViewGetInt16JS,1);
function DataViewSetInt16JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setInt16',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetInt16(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetInt16JS,2);

function DataViewGetUint16JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getUint16',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetUint16(this,al,!!bC);
}
%FunctionSetLength(DataViewGetUint16JS,1);
function DataViewSetUint16JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setUint16',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetUint16(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetUint16JS,2);

function DataViewGetInt32JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getInt32',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetInt32(this,al,!!bC);
}
%FunctionSetLength(DataViewGetInt32JS,1);
function DataViewSetInt32JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setInt32',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetInt32(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetInt32JS,2);

function DataViewGetUint32JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getUint32',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetUint32(this,al,!!bC);
}
%FunctionSetLength(DataViewGetUint32JS,1);
function DataViewSetUint32JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setUint32',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetUint32(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetUint32JS,2);

function DataViewGetFloat32JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getFloat32',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetFloat32(this,al,!!bC);
}
%FunctionSetLength(DataViewGetFloat32JS,1);
function DataViewSetFloat32JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setFloat32',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetFloat32(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetFloat32JS,2);

function DataViewGetFloat64JS(al,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.getFloat64',this);
}
if(arguments.length<1)throw F(47);
al=K(al,169);
return %DataViewGetFloat64(this,al,!!bC);
}
%FunctionSetLength(DataViewGetFloat64JS,1);
function DataViewSetFloat64JS(al,ay,bC){
if(!(%_ClassOf(this)==='DataView')){
throw F(44,
'DataView.setFloat64',this);
}
if(arguments.length<2)throw F(47);
al=K(al,169);
%DataViewSetFloat64(this,al,(%_ToNumber(ay)),!!bC);
}
%FunctionSetLength(DataViewSetFloat64JS,2);


%FunctionSetPrototype(k,new l);
%AddNamedProperty(k.prototype,"constructor",k,
2);
%AddNamedProperty(k.prototype,M,"DataView",
1|2);
b.InstallGetter(k.prototype,"buffer",DataViewGetBufferJS);
b.InstallGetter(k.prototype,"byteOffset",
DataViewGetByteOffset);
b.InstallGetter(k.prototype,"byteLength",
DataViewGetByteLength);
b.InstallFunctions(k.prototype,2,[
"getInt8",DataViewGetInt8JS,
"setInt8",DataViewSetInt8JS,
"getUint8",DataViewGetUint8JS,
"setUint8",DataViewSetUint8JS,
"getInt16",DataViewGetInt16JS,
"setInt16",DataViewSetInt16JS,
"getUint16",DataViewGetUint16JS,
"setUint16",DataViewSetUint16JS,
"getInt32",DataViewGetInt32JS,
"setInt32",DataViewSetInt32JS,
"getUint32",DataViewGetUint32JS,
"setUint32",DataViewSetUint32JS,
"getFloat32",DataViewGetFloat32JS,
"setFloat32",DataViewSetFloat32JS,
"getFloat64",DataViewGetFloat64JS,
"setFloat64",DataViewSetFloat64JS
]);
})

Hiterator-prototype