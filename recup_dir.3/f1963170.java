orCache(v){
u=v;
ClearMirrorCache();
}
function ClearMirrorCache(v){
r=0;
t=[];
}
function ObjectIsPromise(v){
return(%_IsJSReceiver(v))&&
!(%DebugGetProperty(v,k)===(void 0));
}
function MakeMirror(v,w){
var x;
if(!w&&u){
for(var y in t){
x=t[y];
if(x.value()===v){
return x;
}
if(x.isNumber()&&e(x.value())&&
typeof v=='number'&&e(v)){
return x;
}
}
}
if((v===(void 0))){
x=new UndefinedMirror();
}else if((v===null)){
x=new NullMirror();
}else if((typeof(v)==='boolean')){
x=new BooleanMirror(v);
}else if((typeof(v)==='number')){
x=new NumberMirror(v);
}else if((typeof(v)==='string')){
x=new StringMirror(v);
}else if((typeof(v)==='symbol')){
x=new SymbolMirror(v);
}else if((%_IsArray(v))){
x=new ArrayMirror(v);
}else if((%IsDate(v))){
x=new DateMirror(v);
}else if((%IsFunction(v))){
x=new FunctionMirror(v);
}else if((%_IsRegExp(v))){
x=new RegExpMirror(v);
}else if((%_ClassOf(v)==='Error')){
x=new ErrorMirror(v);
}else if((%_ClassOf(v)==='Script')){
x=new ScriptMirror(v);
}else if((%_ClassOf(v)==='Map')||(%_ClassOf(v)==='WeakMap')){
x=new MapMirror(v);
}else if((%_ClassOf(v)==='Set')||(%_ClassOf(v)==='WeakSet')){
x=new SetMirror(v);
}else if((%_ClassOf(v)==='Map Iterator')||(%_ClassOf(v)==='Set Iterator')){
x=new IteratorMirror(v);
}else if(ObjectIsPromise(v)){
x=new PromiseMirror(v);
}else if((%_ClassOf(v)==='Generator')){
x=new GeneratorMirror(v);
}else{
x=new ObjectMirror(v,q.OBJECT_TYPE,w);
}
if(u)t[x.handle()]=x;
return x;
}
function LookupMirror(z){
if(!u){
throw g(2,"Mirror cache is disabled");
}
return t[z];
}
function GetUndefinedMirror(){
return MakeMirror((void 0));
}
function inherits(A,B){
var C=function(){};
C.prototype=B.prototype;
A.super_=B.prototype;
A.prototype=new C();
A.prototype.constructor=A;
}
var D=80;
var E={};
E.Data=0;
E.DataConstant=2;
E.AccessorConstant=3;
var F={};
F.None=0;
F.ReadOnly=1;
F.DontEnum=2;
F.DontDelete=4;
var G={Global:0,
Local:1,
With:2,
Closure:3,
Catch:4,
Block:5,
Script:6};
function Mirror(H){
this.type_=H;
}
Mirror.prototype.type=function(){
return this.type_;
};
Mirror.prototype.isValue=function(){
return this instanceof ValueMirror;
};
Mirror.prototype.isUndefined=function(){
return this instanceof UndefinedMirror;
};
Mirror.prototype.isNull=function(){
return this instanceof NullMirror;
};
Mirror.prototype.isBoolean=function(){
return this instanceof BooleanMirror;
};
Mirror.prototype.isNumber=function(){
return this instanceof NumberMirror;
};
Mirror.prototype.isString=function(){
return this instanceof StringMirror;
};
Mirror.prototype.isSymbol=function(){
return this instanceof SymbolMirror;
};
Mirror.prototype.isObject=function(){
return this instanceof ObjectMirror;
};
Mirror.prototype.isFunction=function(){
return this instanceof FunctionMirror;
};
Mirror.prototype.isUnresolvedFunction=function(){
return this instanceof UnresolvedFunctionMirror;
};
Mirror.prototype.isArray=function(){
return this instanceof ArrayMirror;
};
Mirror.prototype.isDate=function(){
return this instanceof DateMirror;
};
Mirror.prototype.isRegExp=function(){
return this instanceof RegExpMirror;
};
Mirror.prototype.isError=function(){
return this instanceof ErrorMirror;
};
Mirror.prototype.isPromise=function(){
return this instanceof PromiseMirror;
};
Mirror.prototype.isGenerator=function(){
return this instanceof GeneratorMirror;
};
Mirror.prototype.isProperty=function(){
return this instanceof PropertyMirror;
};
Mirror.prototype.isInternalProperty=function(){
return this instanceof InternalPropertyMirror;
};
Mirror.prototype.isFrame=function(){
return this instanceof FrameMirror;
};
Mirror.prototype.isScript=function(){
return this instanceof ScriptMirror;
};
Mirror.prototype.isContext=function(){
return this instanceof ContextMirror;
};
Mirror.prototype.isScope=function(){
return this instanceof ScopeMirror;
};
Mirror.prototype.isMap=function(){
return this instanceof MapMirror;
};
Mirror.prototype.isSet=function(){
return this instanceof SetMirror;
};
Mirror.prototype.isIterator=function(){
return this instanceof IteratorMirror;
};
Mirror.prototype.allocateHandle_=function(){
if(u)this.handle_=r++;
};
Mirror.prototype.allocateTransientHandle_=function(){
this.handle_=s--;
};
Mirror.prototype.toText=function(){
return"#<"+this.constructor.name+">";
};
function ValueMirror(H,v,I){
%_Call(Mirror,this,H);
this.value_=v;
if(!I){
this.allocateHandle_();
}else{
this.allocateTransientHandle_();
}
}
inherits(ValueMirror,Mirror);
Mirror.prototype.handle=function(){
return this.handle_;
};
ValueMirror.prototype.isPrimitive=function(){
var H=this.type();
return H==='undefined'||
H==='null'||
H==='boolean'||
H==='number'||
H==='string'||
H==='symbol';
};
ValueMirror.prototype.value=function(){
return this.value_;
};
function UndefinedMirror(){
%_Call(ValueMirror,this,q.UNDEFINED_TYPE,(void 0));
}
inherits(UndefinedMirror,ValueMirror);
UndefinedMirror.prototype.toText=function(){
return'undefined';
};
function NullMirror(){
%_Call(ValueMirror,this,q.NULL_TYPE,null);
}
inherits(NullMirror,ValueMirror);
NullMirror.prototype.toText=function(){
return'null';
};
function BooleanMirror(v){
%_Call(ValueMirror,this,q.BOOLEAN_TYPE,v);
}
inherits(BooleanMirror,ValueMirror);
BooleanMirror.prototype.toText=function(){
return this.value_?'true':'false';
};
function NumberMirror(v){
%_Call(ValueMirror,this,q.NUMBER_TYPE,v);
}
inherits(NumberMirror,ValueMirror);
NumberMirror.prototype.toText=function(){
return %_NumberToString(this.value_);
};
function StringMirror(v){
%_Call(ValueMirror,this,q.STRING_TYPE,v);
}
inherits(StringMirror,ValueMirror);
StringMirror.prototype.length=function(){
return this.value_.length;
};
StringMirror.prototype.getTruncatedValue=function(J){
if(J!=-1&&this.length()>J){
return this.value_.substring(0,J)+
'... (length: '+this.length()+')';
}
return this.value_;
};
StringMirror.prototype.toText=function(){
return this.getTruncatedValue(D);
};
function SymbolMirror(v){
%_Call(ValueMirror,this,q.SYMBOL_TYPE,v);
}
inherits(SymbolMirror,ValueMirror);
SymbolMirror.prototype.description=function(){
return %SymbolDescription(%_ValueOf(this.value_));
}
SymbolMirror.prototype.toText=function(){
return %_Call(o,this.value_);
}
function ObjectMirror(v,H,I){
H=H||q.OBJECT_TYPE;
%_Call(ValueMirror,this,H,v,I);
}
inherits(ObjectMirror,ValueMirror);
ObjectMirror.prototype.className=function(){
return %_ClassOf(this.value_);
};
ObjectMirror.prototype.constructorFunction=function(){
return MakeMirror(%DebugGetProperty(this.value_,'constructor'));
};
ObjectMirror.prototype.prototypeObject=function(){
return MakeMirror(%DebugGetProperty(this.value_,'prototype'));
};
ObjectMirror.prototype.protoObject=function(){
return MakeMirror(%DebugGetPrototype(this.value_));
};
ObjectMirror.prototype.hasNamedInterceptor=function(){
var K=%GetInterceptorInfo(this.value_);
return(K&2)!=0;
};
ObjectMirror.prototype.hasIndexedInterceptor=function(){
var K=%GetInterceptorInfo(this.value_);
return(K&1)!=0;
};
ObjectMirror.prototype.propertyNames=function(){
return %GetOwnPropertyKeys(this.value_,0);
};
ObjectMirror.prototype.properties=function(){
var L=this.propertyNames();
var M=new d(L.length);
for(var N=0;N<L.length;N++){
M[N]=this.property(L[N]);
}
return M;
};
ObjectMirror.prototype.internalProperties=function(){
return ObjectMirror.GetInternalProperties(this.value_);
}
ObjectMirror.prototype.property=function(O){
var P=%DebugGetPropertyDetails(this.value_,(%_ToName(O)));
if(P){
return new PropertyMirror(this,O,P);
}
return GetUndefinedMirror();
};
ObjectMirror.prototype.lookupProperty=function(v){
var M=this.properties();
for(var N=0;N<M.length;N++){
var Q=M[N];
if(Q.propertyType()!=E.AccessorConstant){
if(Q.value_===v.value_){
return Q;
}
}
}
return GetUndefinedMirror();
};
ObjectMirror.prototype.referencedBy=function(R){
var S=%DebugReferencedBy(this.value_,
Mirror.prototype,R||0);
for(var N=0;N<S.length;N++){
S[N]=MakeMirror(S[N]);
}
return S;
};
ObjectMirror.prototype.toText=function(){
var O;
var A=this.constructorFunction();
if(!A.isFunction()){
O=this.className();
}else{
O=A.name();
if(!O){
O=this.className();
}
}
return'#<'+O+'>';
};
ObjectMirror.GetInternalProperties=function(v){
var M=%DebugGetInternalProperties(v);
var S=[];
for(var N=0;N<M.length;N+=2){
S.push(new InternalPropertyMirror(M[N],M[N+1]));
}
return S;
}
function FunctionMirror(v){
%_Call(ObjectMirror,this,v,q.FUNCTION_TYPE);
this.resolved_=true;
}
inherits(FunctionMirror,ObjectMirror);
FunctionMirror.prototype.resolved=function(){
return this.resolved_;
};
FunctionMirror.prototype.name=function(){
return %FunctionGetName(this.value_);
};
FunctionMirror.prototype.debugName=function(){
return %FunctionGetDebugName(this.value_);
}
FunctionMirror.prototype.inferredName=function(){
return %FunctionGetInferredName(this.value_);
};
FunctionMirror.prototype.source=function(){
if(this.resolved()){
return %FunctionToString(this.value_);
}
};
FunctionMirror.prototype.script=function(){
if(this.resolved()){
if(this.script_){
return this.script_;
}
var T=%FunctionGetScript(this.value_);
if(T){
return this.script_=MakeMirror(T);
}
}
};
FunctionMirror.prototype.sourcePosition_=function(){
if(this.resolved()){
return %FunctionGetScriptSourcePosition(this.value_);
}
};
FunctionMirror.prototype.sourceLocation=function(){
if(this.resolved()){
var T=this.script();
if(T){
return T.locationFromPosition(this.sourcePosition_(),true);
}
}
};
FunctionMirror.prototype.constructedBy=function(U){
if(this.resolved()){
var S=%DebugConstructedBy(this.value_,U||0);
for(var N=0;N<S.length;N++){
S[N]=MakeMirror(S[N]);
}
return S;
}else{
return[];
}
};
FunctionMirror.prototype.scopeCount=function(){
if(this.resolved()){
if((this.scopeCount_===(void 0))){
this.scopeCount_=%GetFunctionScopeCount(this.value());
}
return this.scopeCount_;
}else{
return 0;
}
};
FunctionMirror.prototype.scope=function(V){
if(this.resolved()){
return new ScopeMirror((void 0),this,V);
}
};
FunctionMirror.prototype.toText=function(){
return this.source();
};
FunctionMirror.prototype.context=function(){
if(this.resolved()){
if(!this._context)
this._context=new ContextMirror(%FunctionGetContextData(this.value_));
return this._context;
}
};
function UnresolvedFunctionMirror(v){
%_Call(ValueMirror,this,q.FUNCTION_TYPE,v);
this.propertyCount_=0;
this.elementCount_=0;
this.resolved_=false;
}
inherits(UnresolvedFunctionMirror,FunctionMirror);
UnresolvedFunctionMirror.prototype.className=function(){
return'Function';
};
UnresolvedFunctionMirror.prototype.constructorFunction=function(){
return GetUndefinedMirror();
};
UnresolvedFunctionMirror.prototype.prototypeObject=function(){
return GetUndefinedMirror();
};
UnresolvedFunctionMirror.prototype.protoObject=function(){
return GetUndefinedMirror();
};
UnresolvedFunctionMirror.prototype.name=function(){
return this.value_;
};
UnresolvedFunctionMirror.prototype.inferredName=function(){
return(void 0);
};
UnresolvedFunctionMirror.prototype.propertyNames=function(W,X){
return[];
};
function ArrayMirror(v){
%_Call(ObjectMirror,this,v);
}
inherits(ArrayMirror,ObjectMirror);
ArrayMirror.prototype.length=function(){
return this.value_.length;
};
ArrayMirror.prototype.indexedPropertiesFromRange=function(opt_from_index,
opt_to_index){
var Y=opt_from_index||0;
var Z=opt_to_index||this.length()-1;
if(Y>Z)return new d();
var aa=new d(Z-Y+1);
for(var N=Y;N<=Z;N++){
var P=%DebugGetPropertyDetails(this.value_,(%_ToString(N)));
var v;
if(P){
v=new PropertyMirror(this,N,P);
}else{
v=GetUndefinedMirror();
}
aa[N-Y]=v;
}
return aa;
};
function DateMirror(v){
%_Call(ObjectMirror,this,v);
}
inherits(DateMirror,ObjectMirror);
DateMirror.prototype.toText=function(){
var ab=f(this.value_);
return ab.substring(1,ab.length-1);
};
function RegExpMirror(v){
%_Call(ObjectMirror,this,v,q.REGEXP_TYPE);
}
inherits(RegExpMirror,ObjectMirror);
RegExpMirror.prototype.source=function(){
return this.value_.source;
};
RegExpMirror.prototype.global=function(){
return this.value_.global;
};
RegExpMirror.prototype.ignoreCase=function(){
return this.value_.ignoreCase;
};
RegExpMirror.prototype.multiline=function(){
return this.value_.multiline;
};
RegExpMirror.prototype.sticky=function(){
return this.value_.sticky;
};
RegExpMirror.prototype.unicode=function(){
return this.value_.unicode;
};
RegExpMirror.prototype.toText=function(){
return"/"+this.source()+"/";
};
function ErrorMirror(v){
%_Call(ObjectMirror,this,v,q.ERROR_TYPE);
}
inherits(ErrorMirror,ObjectMirror);
ErrorMirror.prototype.message=function(){
return this.value_.message;
};
ErrorMirror.prototype.toText=function(){
var ac;
try{
ac=%_Call(c,this.value_);
}catch(e){
ac='#<Error>';
}
return ac;
};
function PromiseMirror(v){
%_Call(ObjectMirror,this,v,q.PROMISE_TYPE);
}
inherits(PromiseMirror,ObjectMirror);
function PromiseGetStatus_(v){
var ad=%DebugGetProperty(v,k);
if(ad==0)return"pending";
if(ad==1)return"resolved";
return"rejected";
}
function PromiseGetValue_(v){
return %DebugGetProperty(v,l);
}
PromiseMirror.prototype.status=function(){
return PromiseGetStatus_(this.value_);
};
PromiseMirror.prototype.promiseValue=function(){
return MakeMirror(PromiseGetValue_(this.value_));
};
function MapMirror(v){
%_Call(ObjectMirror,this,v,q.MAP_TYPE);
}
inherits(MapMirror,ObjectMirror);
MapMirror.prototype.entries=function(ae){
var S=[];
if((%_ClassOf(this.value_)==='WeakMap')){
var af=%GetWeakMapEntries(this.value_,ae||0);
for(var N=0;N<af.length;N+=2){
S.push({
key:af[N],
value:af[N+1]
});
}
return S;
}
var ag=%_Call(h,this.value_);
var ah;
while((!ae||S.length<ae)&&
!(ah=ag.next()).done){
S.push({
key:ah.value[0],
value:ah.value[1]
});
}
return S;
};
function SetMirror(v){
%_Call(ObjectMirror,this,v,q.SET_TYPE);
}
inherits(SetMirror,ObjectMirror);
function IteratorGetValues_(ag,ai,ae){
var S=[];
var ah;
while((!ae||S.length<ae)&&
!(ah=%_Call(ai,ag)).done){
S.push(ah.value);
}
return S;
}
SetMirror.prototype.values=function(ae){
if((%_ClassOf(this.value_)==='WeakSet')){
return %GetWeakSetValues(this.value_,ae||0);
}
var ag=%_Call(n,this.value_);
return IteratorGetValues_(ag,m,ae);
};
function IteratorMirror(v){
%_Call(ObjectMirror,this,v,q.ITERATOR_TYPE);
}
inherits(IteratorMirror,ObjectMirror);
IteratorMirror.prototype.preview=function(ae){
if((%_ClassOf(this.value_)==='Map Iterator')){
return IteratorGetValues_(%MapIteratorClone(this.value_),
i,
ae);
}else if((%_ClassOf(this.value_)==='Set Iterator')){
return IteratorGetValues_(%SetIteratorClone(this.value_),
m,
ae);
}
};
function GeneratorMirror(v){
%_Call(ObjectMirror,this,v,q.GENERATOR_TYPE);
}
inherits(GeneratorMirror,ObjectMirror);
function GeneratorGetStatus_(v){
var aj=%GeneratorGetContinuation(v);
if(aj<0)return"running";
if(aj==0)return"closed";
return"suspended";
}
GeneratorMirror.prototype.status=function(){
return GeneratorGetStatus_(this.value_);
};
GeneratorMirror.prototype.sourcePosition_=function(){
return %GeneratorGetSourcePosition(this.value_);
};
GeneratorMirror.prototype.sourceLocation=function(){
var ak=this.sourcePosition_();
if(!(ak===(void 0))){
var T=this.func().script();
if(T){
return T.locationFromPosition(ak,true);
}
}
};
GeneratorMirror.prototype.func=function(){
if(!this.func_){
this.func_=MakeMirror(%GeneratorGetFunction(this.value_));
}
return this.func_;
};
GeneratorMirror.prototype.receiver=function(){
if(!this.receiver_){
this.receiver_=MakeMirror(%GeneratorGetReceiver(this.value_));
}
return this.receiver_;
};
function PropertyMirror(x,O,P){
%_Call(Mirror,this,q.PROPERTY_TYPE);
this.mirror_=x;
this.name_=O;
this.value_=P[0];
this.details_=P[1];
this.is_interceptor_=P[2];
if(P.length>3){
this.exception_=P[3];
this.getter_=P[4];
this.setter_=P[5];
}
}
inherits(PropertyMirror,Mirror);
PropertyMirror.prototype.isReadOnly=function(){
return(this.attributes()&F.ReadOnly)!=0;
};
PropertyMirror.prototype.isEnum=function(){
return(this.attributes()&F.DontEnum)==0;
};
PropertyMirror.prototype.canDelete=function(){
return(this.attributes()&F.DontDelete)==0;
};
PropertyMirror.prototype.name=function(){
return this.name_;
};
PropertyMirror.prototype.isIndexed=function(){
for(var N=0;N<this.name_.length;N++){
if(this.name_[N]<'0'||'9'<this.name_[N]){
return false;
}
}
return true;
};
PropertyMirror.prototype.value=function(){
return MakeMirror(this.value_,false);
};
PropertyMirror.prototype.isException=function(){
return this.exception_?true:false;
};
PropertyMirror.prototype.attributes=function(){
return %DebugPropertyAttributesFromDetails(this.details_);
};
PropertyMirror.prototype.propertyType=function(){
return %DebugPropertyTypeFromDetails(this.details_);
};
PropertyMirror.prototype.insertionIndex=function(){
return %DebugPropertyIndexFromDetails(this.details_);
};
PropertyMirror.prototype.hasGetter=function(){
return this.getter_?true:false;
};
PropertyMirror.prototype.hasSetter=function(){
return this.setter_?true:false;
};
PropertyMirror.prototype.getter=function(){
if(this.hasGetter()){
return MakeMirror(this.getter_);
}else{
return GetUndefinedMirror();
}
};
PropertyMirror.prototype.setter=function(){
if(this.hasSetter()){
return MakeMirror(this.setter_);
}else{
return GetUndefinedMirror();
}
};
PropertyMirror.prototype.isNative=function(){
return this.is_interceptor_||
((this.propertyType()==E.AccessorConstant)&&
!this.hasGetter()&&!this.hasSetter());
};
function InternalPropertyMirror(O,v){
%_Call(Mirror,this,q.INTERNAL_PROPERTY_TYPE);
this.name_=O;
this.value_=v;
}
inherits(InternalPropertyMirror,Mirror);
InternalPropertyMirror.prototype.name=function(){
return this.name_;
};
InternalPropertyMirror.prototype.value=function(){
return MakeMirror(this.value_,false);
};
var al=0;
var am=1;
var an=2;
var ao=3;
var ap=4;
var aq=5;
var ar=6;
var as=7;
var at=8;
var au=9;
var av=0;
var aw=1;
var ax=2;
var ay=1<<0;
var az=1<<1;
var aA=7<<2;
function FrameDetails(aB,V){
this.break_id_=aB;
this.details_=%GetFrameDetails(aB,V);
}
FrameDetails.prototype.frameId=function(){
%CheckExecutionState(this.break_id_);
return this.details_[al];
};
FrameDetails.prototype.receiver=function(){
%CheckExecutionState(this.break_id_);
return this.details_[am];
};
FrameDetails.prototype.func=function(){
%CheckExecutionState(this.break_id_);
return this.details_[an];
};
FrameDetails.prototype.isConstructCall=function(){
%CheckExecutionState(this.break_id_);
return this.details_[ar];
};
FrameDetails.prototype.isAtReturn=function(){
%CheckExecutionState(this.break_id_);
return this.details_[as];
};
FrameDetails.prototype.isDebuggerFrame=function(){
%CheckExecutionState(this.break_id_);
var aC=ay;
return(this.details_[at]&aC)==aC;
};
FrameDetails.prototype.isOptimizedFrame=function(){
%CheckExecutionState(this.break_id_);
var aC=az;
return(this.details_[at]&aC)==aC;
};
FrameDetails.prototype.isInlinedFrame=function(){
return this.inlinedFrameIndex()>0;
};
FrameDetails.prototype.inlinedFrameIndex=function(){
%CheckExecutionState(this.break_id_);
var aC=aA;
return(this.details_[at]&aC)>>2;
};
FrameDetails.prototype.argumentCount=function(){
%CheckExecutionState(this.break_id_);
return this.details_[ao];
};
FrameDetails.prototype.argumentName=function(V){
%CheckExecutionState(this.break_id_);
if(V>=0&&V<this.argumentCount()){
return this.details_[au+
V*ax+
av];
}
};
FrameDetails.prototype.argumentValue=function(V){
%CheckExecutionState(this.break_id_);
if(V>=0&&V<this.argumentCount()){
return this.details_[au+
V*ax+
aw];
}
};
FrameDetails.prototype.localCount=function(){
%CheckExecutionState(this.break_id_);
return this.details_[ap];
};
FrameDetails.prototype.sourcePosition=function(){
%CheckExecutionState(this.break_id_);
return this.details_[aq];
};
FrameDetails.prototype.localName=function(V){
%CheckExecutionState(this.break_id_);
if(V>=0&&V<this.localCount()){
var aD=au+
this.argumentCount()*ax;
return this.details_[aD+
V*ax+
av];
}
};
FrameDetails.prototype.localValue=function(V){
%CheckExecutionState(this.break_id_);
if(V>=0&&V<this.localCount()){
var aD=au+
this.argumentCount()*ax;
return this.details_[aD+
V*ax+
aw];
}
};
FrameDetails.prototype.returnValue=function(){
%CheckExecutionState(this.break_id_);
var aE=
au+
(this.argumentCount()+this.localCount())*ax;
if(this.details_[as]){
return this.details_[aE];
}
};
FrameDetails.prototype.scopeCount=function(){
if((this.scopeCount_===(void 0))){
this.scopeCount_=%GetScopeCount(this.break_id_,this.frameId());
}
return this.scopeCount_;
};
function FrameMirror(aB,V){
%_Call(Mirror,this,q.FRAME_TYPE);
this.break_id_=aB;
this.index_=V;
this.details_=new FrameDetails(aB,V);
}
inherits(FrameMirror,Mirror);
FrameMirror.prototype.details=function(){
return this.details_;
};
FrameMirror.prototype.index=function(){
return this.index_;
};
FrameMirror.prototype.func=function(){
if(this.func_){
return this.func_;
}
var aC=this.details_.func();
if((%IsFunction(aC))){
return this.func_=MakeMirror(aC);
}else{
return new UnresolvedFunctionMirror(aC);
}
};
FrameMirror.prototype.receiver=function(){
return MakeMirror(this.details_.receiver());
};
FrameMirror.prototype.isConstructCall=function(){
return this.details_.isConstructCall();
};
FrameMirror.prototype.isAtReturn=function(){
return this.details_.isAtReturn();
};
FrameMirror.prototype.isDebuggerFrame=function(){
return this.details_.isDebuggerFrame();
};
FrameMirror.prototype.isOptimizedFrame=function(){
return this.details_.isOptimizedFrame();
};
FrameMirror.prototype.isInlinedFrame=function(){
return this.details_.isInlinedFrame();
};
FrameMirror.prototype.inlinedFrameIndex=function(){
return this.details_.inlinedFrameIndex();
};
FrameMirror.prototype.argumentCount=function(){
return this.details_.argumentCount();
};
FrameMirror.prototype.argumentName=function(V){
return this.details_.argumentName(V);
};
FrameMirror.prototype.argumentValue=function(V){
return MakeMirror(this.details_.argumentValue(V));
};
FrameMirror.prototype.localCount=function(){
return this.details_.localCount();
};
FrameMirror.prototype.localName=function(V){
return this.details_.localName(V);
};
FrameMirror.prototype.localValue=function(V){
return MakeMirror(this.details_.localValue(V));
};
FrameMirror.prototype.returnValue=function(){
return MakeMirror(this.details_.returnValue());
};
FrameMirror.prototype.sourcePosition=function(){
return this.details_.sourcePosition();
};
FrameMirror.prototype.sourceLocation=function(){
var aF=this.func();
if(aF.resolved()){
var T=aF.script();
if(T){
return T.locationFromPosition(this.sourcePosition(),true);
}
}
};
FrameMirror.prototype.sourceLine=function(){
var aG=this.sourceLocation();
if(aG){
return aG.line;
}
};
FrameMirror.prototype.sourceColumn=function(){
var aG=this.sourceLocation();
if(aG){
return aG.column;
}
};
FrameMirror.prototype.sourceLineText=function(){
var aG=this.sourceLocation();
if(aG){
return aG.sourceText();
}
};
FrameMirror.prototype.scopeCount=function(){
return this.details_.scopeCount();
};
FrameMirror.prototype.scope=function(V){
return new ScopeMirror(this,(void 0),V);
};
FrameMirror.prototype.allScopes=function(aH){
var aI=%GetAllScopesDetails(this.break_id_,
this.details_.frameId(),
this.details_.inlinedFrameIndex(),
!!aH);
var S=[];
for(var N=0;N<aI.length;++N){
S.push(new ScopeMirror(this,(void 0),N,aI[N]));
}
return S;
};
FrameMirror.prototype.evaluate=function(source,disable_break,
opt_context_object){
return MakeMirror(%DebugEvaluate(this.break_id_,
this.details_.frameId(),
this.details_.inlinedFrameIndex(),
source,
(!!(disable_break)),
opt_context_object));
};
FrameMirror.prototype.invocationText=function(){
var S='';
var aF=this.func();
var aJ=this.receiver();
if(this.isConstructCall()){
S+='new ';
S+=aF.name()?aF.name():'[anonymous]';
}else if(this.isDebuggerFrame()){
S+='[debugger]';
}else{
var aK=
!aJ.className||(aJ.className()!='global');
if(aK){
S+=aJ.toText();
}
var Q=GetUndefinedMirror();
if(aJ.isObject()){
for(var aL=aJ;
!aL.isNull()&&Q.isUndefined();
aL=aL.protoObject()){
Q=aL.lookupProperty(aF);
}
}
if(!Q.isUndefined()){
if(!Q.isIndexed()){
if(aK){
S+='.';
}
S+=Q.name();
}else{
S+='[';
S+=Q.name();
S+=']';
}
if(aF.name()&&aF.name()!=Q.name()){
S+='(aka '+aF.name()+')';
}
}else{
if(aK){
S+='.';
}
S+=aF.name()?aF.name():'[anonymous]';
}
}
if(!this.isDebuggerFrame()){
S+='(';
for(var N=0;N<this.argumentCount();N++){
if(N!=0)S+=', ';
if(this.argumentName(N)){
S+=this.argumentName(N);
S+='=';
}
S+=this.argumentValue(N).toText();
}
S+=')';
}
if(this.isAtReturn()){
S+=' returning ';
S+=this.returnValue().toText();
}
return S;
};
FrameMirror.prototype.sourceAndPositionText=function(){
var S='';
var aF=this.func();
if(aF.resolved()){
var T=aF.script();
if(T){
if(T.name()){
S+=T.name();
}else{
S+='[unnamed]';
}
if(!this.isDebuggerFrame()){
var aG=this.sourceLocation();
S+=' line ';
S+=!(aG===(void 0))?(aG.line+1):'?';
S+=' column ';
S+=!(aG===(void 0))?(aG.column+1):'?';
if(!(this.sourcePosition()===(void 0))){
S+=' (position '+(this.sourcePosition()+1)+')';
}
}
}else{
S+='[no source]';
}
}else{
S+='[unresolved]';
}
return S;
};
FrameMirror.prototype.localsText=function(){
var S='';
var aM=this.localCount();
if(aM>0){
for(var N=0;N<aM;++N){
S+='      var ';
S+=this.localName(N);
S+=' = ';
S+=this.localValue(N).toText();
if(N<aM-1)S+='\n';
}
}
return S;
};
FrameMirror.prototype.restart=function(){
var S=%LiveEditRestartFrame(this.break_id_,this.index_);
if((S===(void 0))){
S="Failed to find requested frame";
}
return S;
};
FrameMirror.prototype.toText=function(aN){
var S='';
S+='#'+(this.index()<=9?'0':'')+this.index();
S+=' ';
S+=this.invocationText();
S+=' ';
S+=this.sourceAndPositionText();
if(aN){
S+='\n';
S+=this.localsText();
}
return S;
};
var aO=0;
var aP=1;
var aQ=2;
var aR=3;
var aS=4;
var aT=5;
function ScopeDetails(aU,aV,V,aW){
if(aU){
this.break_id_=aU.break_id_;
this.details_=aW||
%GetScopeDetails(aU.break_id_,
aU.details_.frameId(),
aU.details_.inlinedFrameIndex(),
V);
this.frame_id_=aU.details_.frameId();
this.inlined_frame_id_=aU.details_.inlinedFrameIndex();
}else{
this.details_=aW||%GetFunctionScopeDetails(aV.value(),V);
this.fun_value_=aV.value();
this.break_id_=(void 0);
}
this.index_=V;
}
ScopeDetails.prototype.type=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aO];
};
ScopeDetails.prototype.object=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aP];
};
ScopeDetails.prototype.name=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aQ];
};
ScopeDetails.prototype.startPosition=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aR];
}
ScopeDetails.prototype.endPosition=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aS];
}
ScopeDetails.prototype.func=function(){
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
}
return this.details_[aT];
}
ScopeDetails.prototype.setVariableValueImpl=function(O,aX){
var aY;
if(!(this.break_id_===(void 0))){
%CheckExecutionState(this.break_id_);
aY=%SetScopeVariableValue(this.break_id_,this.frame_id_,
this.inlined_frame_id_,this.index_,O,aX);
}else{
aY=%SetScopeVariableValue(this.fun_value_,null,null,this.index_,
O,aX);
}
if(!aY)throw g(2,"Failed to set variable value");
};
function ScopeMirror(aU,aV,V,aW){
%_Call(Mirror,this,q.SCOPE_TYPE);
if(aU){
this.frame_index_=aU.index_;
}else{
this.frame_index_=(void 0);
}
this.scope_index_=V;
this.details_=new ScopeDetails(aU,aV,V,aW);
}
inherits(ScopeMirror,Mirror);
ScopeMirror.prototype.details=function(){
return this.details_;
};
ScopeMirror.prototype.frameIndex=function(){
return this.frame_index_;
};
ScopeMirror.prototype.scopeIndex=function(){
return this.scope_index_;
};
ScopeMirror.prototype.scopeType=function(){
return this.details_.type();
};
ScopeMirror.prototype.scopeObject=function(){
var I=this.scopeType()==G.Local||
this.scopeType()==G.Closure||
this.scopeType()==G.Script;
return MakeMirror(this.details_.object(),I);
};
ScopeMirror.prototype.setVariableValue=function(O,aX){
this.details_.setVariableValueImpl(O,aX);
};
function ScriptMirror(T){
%_Call(Mirror,this,q.SCRIPT_TYPE);
this.script_=T;
this.context_=new ContextMirror(T.context_data);
this.allocateHandle_();
}
inherits(ScriptMirror,Mirror);
ScriptMirror.prototype.value=function(){
return this.script_;
};
ScriptMirror.prototype.name=function(){
return this.script_.name||this.script_.nameOrSourceURL();
};
ScriptMirror.prototype.id=function(){
return this.script_.id;
};
ScriptMirror.prototype.source=function(){
return this.script_.source;
};
ScriptMirror.prototype.setSource=function(aZ){
%DebugSetScriptSource(this.script_,aZ);
};
ScriptMirror.prototype.lineOffset=function(){
return this.script_.line_offset;
};
ScriptMirror.prototype.columnOffset=function(){
return this.script_.column_offset;
};
ScriptMirror.prototype.data=function(){
return this.script_.data;
};
ScriptMirror.prototype.scriptType=function(){
return this.script_.type;
};
ScriptMirror.prototype.compilationType=function(){
return this.script_.compilation_type;
};
ScriptMirror.prototype.lineCount=function(){
return this.script_.lineCount();
};
ScriptMirror.prototype.locationFromPosition=function(
position,include_resource_offset){
return this.script_.locationFromPosition(position,include_resource_offset);
};
ScriptMirror.prototype.sourceSlice=function(ba,bb){
return this.script_.sourceSlice(ba,bb);
};
ScriptMirror.prototype.context=function(){
return this.context_;
};
ScriptMirror.prototype.evalFromScript=function(){
return MakeMirror(this.script_.eval_from_script);
};
ScriptMirror.prototype.evalFromFunctionName=function(){
return MakeMirror(this.script_.eval_from_function_name);
};
ScriptMirror.prototype.evalFromLocation=function(){
var bc=this.evalFromScript();
if(!bc.isUndefined()){
var bd=this.script_.eval_from_script_position;
return bc.locationFromPosition(bd,true);
}
};
ScriptMirror.prototype.toText=function(){
var S='';
S+=this.name();
S+=' (lines: ';
if(this.lineOffset()>0){
S+=this.lineOffset();
S+='-';
S+=this.lineOffset()+this.lineCount()-1;
}else{
S+=this.lineCount();
}
S+=')';
return S;
};
function ContextMirror(be){
%_Call(Mirror,this,q.CONTEXT_TYPE);
this.data_=be;
this.allocateHandle_();
}
inherits(ContextMirror,Mirror);
ContextMirror.prototype.data=function(){
return this.data_;
};
function MakeMirrorSerializer(P,bf){
return new JSONProtocolSerializer(P,bf);
}
function JSONProtocolSerializer(P,bf){
this.details_=P;
this.options_=bf;
this.mirrors_=[];
}
JSONProtocolSerializer.prototype.serializeReference=function(x){
return this.serialize_(x,true,true);
};
JSONProtocolSerializer.prototype.serializeValue=function(x){
var bg=this.serialize_(x,false,true);
return bg;
};
JSONProtocolSerializer.prototype.serializeReferencedObjects=function(){
var bh=[];
var bi=this.mirrors_.length;
for(var N=0;N<bi;N++){
bh.push(this.serialize_(this.mirrors_[N],false,false));
}
return bh;
};
JSONProtocolSerializer.prototype.includeSource_=function(){
return this.options_&&this.options_.includeSource;
};
JSONProtocolSerializer.prototype.inlineRefs_=function(){
return this.options_&&this.options_.inlineRefs;
};
JSONProtocolSerializer.prototype.maxStringLength_=function(){
if((this.options_===(void 0))||
(this.options_.maxStringLength===(void 0))){
return D;
}
return this.options_.maxStringLength;
};
JSONProtocolSerializer.prototype.add_=function(x){
for(var N=0;N<this.mirrors_.length;N++){
if(this.mirrors_[N]===x){
return;
}
}
this.mirrors_.push(x);
};
JSONProtocolSerializer.prototype.serializeReferenceWithDisplayData_=
function(x){
var bj={};
bj.ref=x.handle();
bj.type=x.type();
switch(x.type()){
case q.UNDEFINED_TYPE:
case q.NULL_TYPE:
case q.BOOLEAN_TYPE:
case q.NUMBER_TYPE:
bj.value=x.value();
break;
case q.STRING_TYPE:
bj.value=x.getTruncatedValue(this.maxStringLength_());
break;
case q.SYMBOL_TYPE:
bj.description=x.description();
break;
case q.FUNCTION_TYPE:
bj.name=x.name();
bj.inferredName=x.inferredName();
if(x.script()){
bj.scriptId=x.script().id();
}
break;
case q.ERROR_TYPE:
case q.REGEXP_TYPE:
bj.value=x.toText();
break;
case q.OBJECT_TYPE:
bj.className=x.className();
break;
}
return bj;
};
JSONProtocolSerializer.prototype.serialize_=function(x,reference,
P){
if(reference&&
(x.isValue()||x.isScript()||x.isContext())){
if(this.inlineRefs_()&&x.isValue()){
return this.serializeReferenceWithDisplayData_(x);
}else{
this.add_(x);
return{'ref':x.handle()};
}
}
var bh={};
if(x.isValue()||x.isScript()||x.isContext()){
bh.handle=x.handle();
}
bh.type=x.type();
switch(x.type()){
case q.UNDEFINED_TYPE:
case q.NULL_TYPE:
break;
case q.BOOLEAN_TYPE:
bh.value=x.value();
break;
case q.NUMBER_TYPE:
bh.value=NumberToJSON_(x.value());
break;
case q.STRING_TYPE:
if(this.maxStringLength_()!=-1&&
x.length()>this.maxStringLength_()){
var bk=x.getTruncatedValue(this.maxStringLength_());
bh.value=bk;
bh.fromIndex=0;
bh.toIndex=this.maxStringLength_();
}else{
bh.value=x.value();
}
bh.length=x.length();
break;
case q.SYMBOL_TYPE:
bh.description=x.description();
break;
case q.OBJECT_TYPE:
case q.FUNCTION_TYPE:
case q.ERROR_TYPE:
case q.REGEXP_TYPE:
case q.PROMISE_TYPE:
case q.GENERATOR_TYPE:
this.serializeObject_(x,bh,P);
break;
case q.PROPERTY_TYPE:
case q.INTERNAL_PROPERTY_TYPE:
throw g(2,
'PropertyMirror cannot be serialized independently');
break;
case q.FRAME_TYPE:
this.serializeFrame_(x,bh);
break;
case q.SCOPE_TYPE:
this.serializeScope_(x,bh);
break;
case q.SCRIPT_TYPE:
if(x.name()){
bh.name=x.name();
}
bh.id=x.id();
bh.lineOffset=x.lineOffset();
bh.columnOffset=x.columnOffset();
bh.lineCount=x.lineCount();
if(x.data()){
bh.data=x.data();
}
if(this.includeSource_()){
bh.source=x.source();
}else{
var bl=x.source().substring(0,80);
bh.sourceStart=bl;
}
bh.sourceLength=x.source().length;
bh.scriptType=x.scriptType();
bh.compilationType=x.compilationType();
if(x.compilationType()==1&&
x.evalFromScript()){
bh.evalFromScript=
this.serializeReference(x.evalFromScript());
var bm=x.evalFromLocation();
if(bm){
bh.evalFromLocation={line:bm.line,
column:bm.column};
}
if(x.evalFromFunctionName()){
bh.evalFromFunctionName=x.evalFromFunctionName();
}
}
if(x.context()){
bh.context=this.serializeReference(x.context());
}
break;
case q.CONTEXT_TYPE:
bh.data=x.data();
break;
}
bh.text=x.toText();
return bh;
};
JSONProtocolSerializer.prototype.serializeObject_=function(x,bh,
P){
bh.className=x.className();
bh.constructorFunction=
this.serializeReference(x.constructorFunction());
bh.protoObject=this.serializeReference(x.protoObject());
bh.prototypeObject=this.serializeReference(x.prototypeObject());
if(x.hasNamedInterceptor()){
bh.namedInterceptor=true;
}
if(x.hasIndexedInterceptor()){
bh.indexedInterceptor=true;
}
if(x.isFunction()){
bh.name=x.name();
if(!(x.inferredName()===(void 0))){
bh.inferredName=x.inferredName();
}
bh.resolved=x.resolved();
if(x.resolved()){
bh.source=x.source();
}
if(x.script()){
bh.script=this.serializeReference(x.script());
bh.scriptId=x.script().id();
serializeLocationFields(x.sourceLocation(),bh);
}
bh.scopes=[];
for(var N=0;N<x.scopeCount();N++){
var bn=x.scope(N);
bh.scopes.push({
type:bn.scopeType(),
index:N
});
}
}
if(x.isGenerator()){
bh.status=x.status();
bh.func=this.serializeReference(x.func())
bh.receiver=this.serializeReference(x.receiver())
serializeLocationFields(x.sourceLocation(),bh);
}
if(x.isDate()){
bh.value=x.value();
}
if(x.isPromise()){
bh.status=x.status();
bh.promiseValue=this.serializeReference(x.promiseValue());
}
var M=x.propertyNames();
for(var N=0;N<M.length;N++){
var bo=x.property(M[N]);
M[N]=this.serializeProperty_(bo);
if(P){
this.add_(bo.value());
}
}
bh.properties=M;
var bp=x.internalProperties();
if(bp.length>0){
var bq=[];
for(var N=0;N<bp.length;N++){
bq.push(this.serializeInternalProperty_(bp[N]));
}
bh.internalProperties=bq;
}
};
function serializeLocationFields(aG,bh){
if(!aG){
return;
}
bh.position=aG.position;
var br=aG.line;
if(!(br===(void 0))){
bh.line=br;
}
var bs=aG.column;
if(!(bs===(void 0))){
bh.column=bs;
}
}
JSONProtocolSerializer.prototype.serializeProperty_=function(bo){
var S={};
S.name=bo.name();
var bt=bo.value();
if(this.inlineRefs_()&&bt.isValue()){
S.value=this.serializeReferenceWithDisplayData_(bt);
}else{
if(bo.attributes()!=F.None){
S.attributes=bo.attributes();
}
S.propertyType=bo.propertyType();
S.ref=bt.handle();
}
return S;
};
JSONProtocolSerializer.prototype.serializeInternalProperty_=
function(bo){
var S={};
S.name=bo.name();
var bt=bo.value();
if(this.inlineRefs_()&&bt.isValue()){
S.value=this.serializeReferenceWithDisplayData_(bt);
}else{
S.ref=bt.handle();
}
return S;
};
JSONProtocolSerializer.prototype.serializeFrame_=function(x,bh){
bh.index=x.index();
bh.receiver=this.serializeReference(x.receiver());
var aF=x.func();
bh.func=this.serializeReference(aF);
var T=aF.script();
if(T){
bh.script=this.serializeReference(T);
}
bh.constructCall=x.isConstructCall();
bh.atReturn=x.isAtReturn();
if(x.isAtReturn()){
bh.returnValue=this.serializeReference(x.returnValue());
}
bh.debuggerFrame=x.isDebuggerFrame();
var K=new d(x.argumentCount());
for(var N=0;N<x.argumentCount();N++){
var bu={};
var bv=x.argumentName(N);
if(bv){
bu.name=bv;
}
bu.value=this.serializeReference(x.argumentValue(N));
K[N]=bu;
}
bh.arguments=K;
var K=new d(x.localCount());
for(var N=0;N<x.localCount();N++){
var bw={};
bw.name=x.localName(N);
bw.value=this.serializeReference(x.localValue(N));
K[N]=bw;
}
bh.locals=K;
serializeLocationFields(x.sourceLocation(),bh);
var bx=x.sourceLineText();
if(!(bx===(void 0))){
bh.sourceLineText=bx;
}
bh.scopes=[];
for(var N=0;N<x.scopeCount();N++){
var bn=x.scope(N);
bh.scopes.push({
type:bn.scopeType(),
index:N
});
}
};
JSONProtocolSerializer.prototype.serializeScope_=function(x,bh){
bh.index=x.scopeIndex();
bh.frameIndex=x.frameIndex();
bh.type=x.scopeType();
bh.object=this.inlineRefs_()?
this.serializeValue(x.scopeObject()):
this.serializeReference(x.scopeObject());
};
function NumberToJSON_(v){
if(e(v)){
return'NaN';
}
if(!(%_IsSmi(%IS_VAR(v))||((v==v)&&(v!=1/0)&&(v!=-1/0)))){
if(v>0){
return'Infinity';
}else{
return'-Infinity';
}
}
return v;
}
b.InstallFunctions(a,2,[
"MakeMirror",MakeMirror,
"MakeMirrorSerializer",MakeMirrorSerializer,
"LookupMirror",LookupMirror,
"ToggleMirrorCache",ToggleMirrorCache,
"MirrorCacheIsEmpty",MirrorCacheIsEmpty,
]);
b.InstallConstants(a,[
"ScopeType",G,
"PropertyType",E,
"PropertyAttribute",F,
"Mirror",Mirror,
"ValueMirror",ValueMirror,
"UndefinedMirror",UndefinedMirror,
"NullMirror",NullMirror,
"BooleanMirror",BooleanMirror,
"NumberMirror",NumberMirror,
"StringMirror",StringMirror,
"SymbolMirror",SymbolMirror,
"ObjectMirror",ObjectMirror,
"FunctionMirror",FunctionMirror,
"UnresolvedFunctionMirror",UnresolvedFunctionMirror,
"ArrayMirror",ArrayMirror,
"DateMirror",DateMirror,
"RegExpMirror",RegExpMirror,
"ErrorMirror",ErrorMirror,
"PromiseMirror",PromiseMirror,
"MapMirror",MapMirror,
"SetMirror",SetMirror,
"IteratorMirror",IteratorMirror,
"GeneratorMirror",GeneratorMirror,
"PropertyMirror",PropertyMirror,
"InternalPropertyMirror",InternalPropertyMirror,
"FrameMirror",FrameMirror,
"ScriptMirror",ScriptMirror,
"ScopeMirror",ScopeMirror,
"FrameDetails",FrameDetails,
]);
b.InstallFunctions(b,2,[
"ClearMirrorCache",ClearMirrorCache
]);
b.Export(function(by){
by.MirrorType=q;
});
})

