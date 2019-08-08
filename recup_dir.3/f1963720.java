mbolToString;
C=F.Uint16x8ToString;
D=F.Uint32x4ToString;
E=F.Uint8x16ToString;
});
var G;
var H;
var I;
var J;
var K;
var L;
var M;
function NoSideEffectsObjectToString(){
if((this===(void 0)))return"[object Undefined]";
if((this===null))return"[object Null]";
var N=(%_ToObject(this));
var O=%_ClassOf(N);
var P=%GetDataProperty(N,B);
if(!(typeof(P)==='string')){
P=O;
}
return`[object ${P}]`;
}
function IsErrorObject(Q){
return(%_Call(t,Q,w));
}
function NoSideEffectsErrorToString(){
var R=%GetDataProperty(this,"name");
var S=%GetDataProperty(this,"message");
R=(R===(void 0))?"Error":NoSideEffectsToString(R);
S=(S===(void 0))?"":NoSideEffectsToString(S);
if(R=="")return S;
if(S=="")return R;
return`${R}: ${S}`;
}
function NoSideEffectsToString(Q){
if((typeof(Q)==='string'))return Q;
if((typeof(Q)==='number'))return %_NumberToString(Q);
if((typeof(Q)==='boolean'))return Q?'true':'false';
if((Q===(void 0)))return'undefined';
if((Q===null))return'null';
if((%IsFunction(Q))){
var T=%FunctionToString(Q);
if(T.length>128){
T=%_SubString(T,0,111)+"...<omitted>..."+
%_SubString(T,T.length-2,T.length);
}
return T;
}
if((typeof(Q)==='symbol'))return %_Call(A,Q);
if((%IsSimdValue(Q))){
switch(typeof(Q)){
case'float32x4':return %_Call(k,Q);
case'int32x4':return %_Call(o,Q);
case'int16x8':return %_Call(n,Q);
case'int8x16':return %_Call(p,Q);
case'uint32x4':return %_Call(D,Q);
case'uint16x8':return %_Call(C,Q);
case'uint8x16':return %_Call(E,Q);
case'bool32x4':return %_Call(e,Q);
case'bool16x8':return %_Call(d,Q);
case'bool8x16':return %_Call(f,Q);
}
}
if((%_IsJSReceiver(Q))){
if(IsErrorObject(Q)||
%GetDataProperty(Q,"toString")===ErrorToString){
return %_Call(NoSideEffectsErrorToString,Q);
}
if(%GetDataProperty(Q,"toString")===u){
var U=%GetDataProperty(Q,"constructor");
if((%IsFunction(U))){
var V=%FunctionGetName(U);
if(V!="")return`#<${V}>`;
}
}
}
return %_Call(NoSideEffectsObjectToString,Q);
}
function MakeGenericError(U,W,X,Y,Z){
var aa=new U(FormatMessage(W,X,Y,Z));
aa[r]=true;
return aa;
}
%FunctionSetInstanceClassName(v,'Script');
%AddNamedProperty(v.prototype,'constructor',v,
2|4|1);
%SetCode(v,function(ab){
throw MakeError(6);
});
function FormatMessage(W,X,Y,Z){
var X=NoSideEffectsToString(X);
var Y=NoSideEffectsToString(Y);
var Z=NoSideEffectsToString(Z);
try{
return %FormatMessageString(W,X,Y,Z);
}catch(e){
return"<error>";
}
}
function GetLineNumber(S){
var ac=%MessageGetStartPosition(S);
if(ac==-1)return 0;
var ad=%MessageGetScript(S);
var ae=ad.locationFromPosition(ac,true);
if(ae==null)return 0;
return ae.line+1;
}
function GetColumnNumber(S){
var ad=%MessageGetScript(S);
var ac=%MessageGetStartPosition(S);
var ae=ad.locationFromPosition(ac,true);
if(ae==null)return-1;
return ae.column;
}
function GetSourceLine(S){
var ad=%MessageGetScript(S);
var ac=%MessageGetStartPosition(S);
var ae=ad.locationFromPosition(ac,true);
if(ae==null)return"";
return ae.sourceText();
}
function ScriptLineFromPosition(af){
var ag=0;
var ah=this.lineCount()-1;
var ai=this.line_ends;
if(af>ai[ah]){
return-1;
}
if(af<=ai[0]){
return 0;
}
while(ah>=1){
var aj=(ag+ah)>>1;
if(af>ai[aj]){
ag=aj+1;
}else if(af<=ai[aj-1]){
ah=aj-1;
}else{
return aj;
}
}
return-1;
}
function ScriptLocationFromPosition(af,
include_resource_offset){
var ak=this.lineFromPosition(af);
if(ak==-1)return null;
var ai=this.line_ends;
var al=ak==0?0:ai[ak-1]+1;
var am=ai[ak];
if(am>0&&%_Call(x,this.source,am-1)=='\r'){
am--;
}
var an=af-al;
if(include_resource_offset){
ak+=this.line_offset;
if(ak==this.line_offset){
an+=this.column_offset;
}
}
return new SourceLocation(this,af,ak,an,al,am);
}
function ScriptLocationFromLine(ao,ap,aq){
var ak=0;
if(!(ao===(void 0))){
ak=ao-this.line_offset;
}
var an=ap||0;
if(ak==0){
an-=this.column_offset;
}
var ar=aq||0;
if(ak<0||an<0||ar<0)return null;
if(ak==0){
return this.locationFromPosition(ar+an,false);
}else{
var as=this.lineFromPosition(ar);
if(as==-1||as+ak>=this.lineCount()){
return null;
}
return this.locationFromPosition(
this.line_ends[as+ak-1]+1+an);
}
}
function ScriptSourceSlice(at,au){
var av=(at===(void 0))?this.line_offset
:at;
var aw=(au===(void 0))?this.line_offset+this.lineCount()
:au;
av-=this.line_offset;
aw-=this.line_offset;
if(av<0)av=0;
if(aw>this.lineCount())aw=this.lineCount();
if(av>=this.lineCount()||
aw<0||
av>aw){
return null;
}
var ai=this.line_ends;
var ax=av==0?0:ai[av-1]+1;
var ay=aw==0?0:ai[aw-1]+1;
return new SourceSlice(this,
av+this.line_offset,
aw+this.line_offset,
ax,ay);
}
function ScriptSourceLine(ao){
var ak=0;
if(!(ao===(void 0))){
ak=ao-this.line_offset;
}
if(ak<0||this.lineCount()<=ak){
return null;
}
var ai=this.line_ends;
var al=ak==0?0:ai[ak-1]+1;
var am=ai[ak];
return %_Call(z,this.source,al,am);
}
function ScriptLineCount(){
return this.line_ends.length;
}
function ScriptLineEnd(az){
return this.line_ends[az];
}
function ScriptNameOrSourceURL(){
if(this.source_url)return this.source_url;
return this.name;
}
b.SetUpLockedPrototype(v,[
"source",
"name",
"source_url",
"source_mapping_url",
"line_ends",
"line_offset",
"column_offset"
],[
"lineFromPosition",ScriptLineFromPosition,
"locationFromPosition",ScriptLocationFromPosition,
"locationFromLine",ScriptLocationFromLine,
"sourceSlice",ScriptSourceSlice,
"sourceLine",ScriptSourceLine,
"lineCount",ScriptLineCount,
"nameOrSourceURL",ScriptNameOrSourceURL,
"lineEnd",ScriptLineEnd
]
);
function SourceLocation(ad,af,ak,an,al,am){
this.script=ad;
this.position=af;
this.line=ak;
this.column=an;
this.start=al;
this.end=am;
}
function SourceLocationSourceText(){
return %_Call(z,this.script.source,this.start,this.end);
}
b.SetUpLockedPrototype(SourceLocation,
["script","position","line","column","start","end"],
["sourceText",SourceLocationSourceText]
);
function SourceSlice(ad,av,aw,ax,ay){
this.script=ad;
this.from_line=av;
this.to_line=aw;
this.from_position=ax;
this.to_position=ay;
}
function SourceSliceSourceText(){
return %_Call(z,
this.script.source,
this.from_position,
this.to_position);
}
b.SetUpLockedPrototype(SourceSlice,
["script","from_line","to_line","from_position","to_position"],
["sourceText",SourceSliceSourceText]
);
function GetStackTraceLine(aA,aB,aC,aD){
return new CallSite(aA,aB,aC,false).toString();
}
function CallSite(aE,aB,aC,aF){
if(!(%IsFunction(aB))){
throw MakeTypeError(19,typeof aB);
}
if((new.target===(void 0))){
return new CallSite(aE,aB,aC,aF);
}
(this[g]=aE);
(this[h]=aB);
(this[i]=((aC)|0));
(this[j]=(!!(aF)));
}
function CheckCallSite(Q,R){
if(!(%_IsJSReceiver(Q))||!(%_Call(t,Q,h))){
throw MakeTypeError(20,R);
}
}
function CallSiteGetThis(){
CheckCallSite(this,"getThis");
return(this[j])
?(void 0):(this[g]);
}
function CallSiteGetFunction(){
CheckCallSite(this,"getFunction");
return(this[j])
?(void 0):(this[h]);
}
function CallSiteGetPosition(){
CheckCallSite(this,"getPosition");
return(this[i]);
}
function CallSiteGetTypeName(){
CheckCallSite(this,"getTypeName");
return GetTypeName((this[g]),false);
}
function CallSiteIsToplevel(){
CheckCallSite(this,"isTopLevel");
return %CallSiteIsToplevelRT(this);
}
function CallSiteIsEval(){
CheckCallSite(this,"isEval");
return %CallSiteIsEvalRT(this);
}
function CallSiteGetEvalOrigin(){
CheckCallSite(this,"getEvalOrigin");
var ad=%FunctionGetScript((this[h]));
return FormatEvalOrigin(ad);
}
function CallSiteGetScriptNameOrSourceURL(){
CheckCallSite(this,"getScriptNameOrSourceURL");
return %CallSiteGetScriptNameOrSourceUrlRT(this);
}
function CallSiteGetFunctionName(){
CheckCallSite(this,"getFunctionName");
return %CallSiteGetFunctionNameRT(this);
}
function CallSiteGetMethodName(){
CheckCallSite(this,"getMethodName");
return %CallSiteGetMethodNameRT(this);
}
function CallSiteGetFileName(){
CheckCallSite(this,"getFileName");
return %CallSiteGetFileNameRT(this);
}
function CallSiteGetLineNumber(){
CheckCallSite(this,"getLineNumber");
return %CallSiteGetLineNumberRT(this);
}
function CallSiteGetColumnNumber(){
CheckCallSite(this,"getColumnNumber");
return %CallSiteGetColumnNumberRT(this);
}
function CallSiteIsNative(){
CheckCallSite(this,"isNative");
return %CallSiteIsNativeRT(this);
}
function CallSiteIsConstructor(){
CheckCallSite(this,"isConstructor");
return %CallSiteIsConstructorRT(this);
}
function CallSiteToString(){
var aG;
var aH="";
if(this.isNative()){
aH="native";
}else{
aG=this.getScriptNameOrSourceURL();
if(!aG&&this.isEval()){
aH=this.getEvalOrigin();
aH+=", ";
}
if(aG){
aH+=aG;
}else{
aH+="<anonymous>";
}
var aI=this.getLineNumber();
if(aI!=null){
aH+=":"+aI;
var aJ=this.getColumnNumber();
if(aJ){
aH+=":"+aJ;
}
}
}
var ak="";
var aK=this.getFunctionName();
var aL=true;
var aM=this.isConstructor();
var aN=!(this.isToplevel()||aM);
if(aN){
var aO=GetTypeName((this[g]),true);
var aP=this.getMethodName();
if(aK){
if(aO&&%_Call(y,aK,aO)!=0){
ak+=aO+".";
}
ak+=aK;
if(aP&&
(%_Call(y,aK,"."+aP)!=
aK.length-aP.length-1)){
ak+=" [as "+aP+"]";
}
}else{
ak+=aO+"."+(aP||"<anonymous>");
}
}else if(aM){
ak+="new "+(aK||"<anonymous>");
}else if(aK){
ak+=aK;
}else{
ak+=aH;
aL=false;
}
if(aL){
ak+=" ("+aH+")";
}
return ak;
}
b.SetUpLockedPrototype(CallSite,["receiver","fun","pos"],[
"getThis",CallSiteGetThis,
"getTypeName",CallSiteGetTypeName,
"isToplevel",CallSiteIsToplevel,
"isEval",CallSiteIsEval,
"getEvalOrigin",CallSiteGetEvalOrigin,
"getScriptNameOrSourceURL",CallSiteGetScriptNameOrSourceURL,
"getFunction",CallSiteGetFunction,
"getFunctionName",CallSiteGetFunctionName,
"getMethodName",CallSiteGetMethodName,
"getFileName",CallSiteGetFileName,
"getLineNumber",CallSiteGetLineNumber,
"getColumnNumber",CallSiteGetColumnNumber,
"isNative",CallSiteIsNative,
"getPosition",CallSiteGetPosition,
"isConstructor",CallSiteIsConstructor,
"toString",CallSiteToString
]);
function FormatEvalOrigin(ad){
var aQ=ad.nameOrSourceURL();
if(aQ){
return aQ;
}
var aR="eval at ";
if(ad.eval_from_function_name){
aR+=ad.eval_from_function_name;
}else{
aR+="<anonymous>";
}
var aS=ad.eval_from_script;
if(aS){
if(aS.compilation_type==1){
aR+=" ("+FormatEvalOrigin(aS)+")";
}else{
if(aS.name){
aR+=" ("+aS.name;
var ae=aS.locationFromPosition(
ad.eval_from_script_position,true);
if(ae){
aR+=":"+(ae.line+1);
aR+=":"+(ae.column+1);
}
aR+=")";
}else{
aR+=" (unknown source)";
}
}
}
return aR;
}
function FormatErrorString(aa){
try{
return %_Call(ErrorToString,aa);
}catch(e){
try{
return"<error: "+e+">";
}catch(ee){
return"<error>";
}
}
}
function GetStackFrames(aT){
var aU=new q();
%MoveArrayContents(aT,aU);
var aV=new q();
var aW=aU[0];
for(var aj=1;aj<aU.length;aj+=4){
var aA=aU[aj];
var aB=aU[aj+1];
var aX=aU[aj+2];
var aY=aU[aj+3];
var aC=%_IsSmi(aX)?aX:%FunctionGetPositionForOffset(aX,aY);
aW--;
aV.push(new CallSite(aA,aB,aC,(aW<0)));
}
return aV;
}
var aZ=false;
function FormatStackTrace(Q,aT){
var aV=GetStackFrames(aT);
if((%IsFunction(G.prepareStackTrace))&&
!aZ){
var ba=[];
%MoveArrayContents(aV,ba);
aZ=true;
var bb=(void 0);
try{
bb=G.prepareStackTrace(Q,ba);
}catch(e){
throw e;
}finally{
aZ=false;
}
return bb;
}
var bc=new q();
bc.push(FormatErrorString(Q));
for(var aj=0;aj<aV.length;aj++){
var bd=aV[aj];
var ak;
try{
ak=bd.toString();
}catch(e){
try{
ak="<error: "+e+">";
}catch(ee){
ak="<error>";
}
}
bc.push("    at "+ak);
}
return %_Call(c,bc,"\n");
}
function GetTypeName(aE,be){
if((aE==null))return null;
if((%_IsJSProxy(aE)))return"Proxy";
var U=%GetDataProperty((%_ToObject(aE)),"constructor");
if(!(%IsFunction(U))){
return be?null:%_Call(NoSideEffectsToString,aE);
}
return %FunctionGetName(U);
}
var bf=function(){
var bg=(void 0);
var bh=this;
while(bh){
var bg=
(bh[l]);
if((bg===(void 0))){
var bb=(bh[w]);
if((bb===(void 0))){
bh=%_GetPrototype(bh);
continue;
}
bg=FormatStackTrace(bh,bb);
(bh[w]=(void 0));
(bh[l]=bg);
}
return bg;
}
return(void 0);
};
var bi=function(bj){
if(IsErrorObject(this)){
(this[w]=(void 0));
(this[l]=bj);
}
};
var bk=function(){};
function SetUpError(bl){
%FunctionSetInstanceClassName(bl,'Error');
var R=bl.name;
var bm=new m();
if(R!=='Error'){
%InternalSetPrototype(bl,G);
%InternalSetPrototype(bm,G.prototype);
}
%FunctionSetPrototype(bl,bm);
%AddNamedProperty(bl.prototype,'name',R,2);
%AddNamedProperty(bl.prototype,'message','',2);
%AddNamedProperty(
bl.prototype,'constructor',bl,2);
%SetCode(bl,function(bn){
if((new.target===(void 0)))return new bl(bn);
try{bk(this,bl);}catch(e){}
if(!(bn===(void 0))){
%AddNamedProperty(this,'message',(%_ToString(bn)),2);
}
});
%SetNativeFlag(bl);
return bl;
};
G=SetUpError(a.Error);
M=SetUpError(a.EvalError);
I=SetUpError(a.RangeError);
L=SetUpError(a.ReferenceError);
K=SetUpError(a.SyntaxError);
H=SetUpError(a.TypeError);
J=SetUpError(a.URIError);
b.InstallFunctions(G.prototype,2,
['toString',ErrorToString]);
function ErrorToString(){
if(!(%_IsJSReceiver(this))){
throw MakeTypeError(17,"Error.prototype.toString");
}
var R=this.name;
R=(R===(void 0))?"Error":(%_ToString(R));
var S=this.message;
S=(S===(void 0))?"":(%_ToString(S));
if(R=="")return S;
if(S=="")return R;
return`${R}: ${S}`
}
function MakeError(W,X,Y,Z){
return MakeGenericError(G,W,X,Y,Z);
}
function MakeRangeError(W,X,Y,Z){
return MakeGenericError(I,W,X,Y,Z);
}
function MakeSyntaxError(W,X,Y,Z){
return MakeGenericError(K,W,X,Y,Z);
}
function MakeTypeError(W,X,Y,Z){
return MakeGenericError(H,W,X,Y,Z);
}
function MakeURIError(){
return MakeGenericError(J,277);
}
var bo=MakeRangeError(186);
b.InstallGetterSetter(bo,'stack',
bf,bi)
bk=function captureStackTrace(Q,bp){
s(Q,'stack',{get:bf,
set:bi,
configurable:true});
%CollectStackTrace(Q,bp?bp:bk);
};
G.captureStackTrace=bk;
%InstallToContext([
"get_stack_trace_line_fun",GetStackTraceLine,
"make_error_function",MakeGenericError,
"make_range_error",MakeRangeError,
"make_type_error",MakeTypeError,
"message_get_column_number",GetColumnNumber,
"message_get_line_number",GetLineNumber,
"message_get_source_line",GetSourceLine,
"no_side_effects_to_string_fun",NoSideEffectsToString,
"stack_overflow_boilerplate",bo,
]);
b.Export(function(bq){
bq.ErrorToString=ErrorToString;
bq.MakeError=MakeError;
bq.MakeRangeError=MakeRangeError;
bq.MakeSyntaxError=MakeSyntaxError;
bq.MakeTypeError=MakeTypeError;
bq.MakeURIError=MakeURIError;
});
});

