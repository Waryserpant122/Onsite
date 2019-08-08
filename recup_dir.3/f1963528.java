(u));
}
function RegExpInitialize(w,x,y){
x=(x===(void 0))?'':(%_ToString(x));
y=(y===(void 0))?'':(%_ToString(y));
%RegExpInitializeAndCompile(w,x,y);
return w;
}
function PatternFlags(x){
return((%_RegExpFlags(x)&1)?'g':'')+
((%_RegExpFlags(x)&2)?'i':'')+
((%_RegExpFlags(x)&4)?'m':'')+
((%_RegExpFlags(x)&16)?'u':'')+
((%_RegExpFlags(x)&8)?'y':'');
}
function RegExpConstructor(x,y){
var z=new.target;
var A=IsRegExp(x);
if((z===(void 0))){
z=g;
if(A&&(y===(void 0))&&
x.constructor===z){
return x;
}
}
if((%_IsRegExp(x))){
if((y===(void 0)))y=PatternFlags(x);
x=(%_RegExpSource(x));
}else if(A){
var B=x;
x=x.source;
if((y===(void 0)))y=B.flags;
}
var w=%_NewObject(g,z);
return RegExpInitialize(w,x,y);
}
function RegExpCompileJS(x,y){
if(!(%_IsRegExp(this))){
throw k(44,
"RegExp.prototype.compile",this);
}
if((%_IsRegExp(x))){
if(!(y===(void 0)))throw k(132);
y=PatternFlags(x);
x=(%_RegExpSource(x));
}
RegExpInitialize(this,x,y);
}
function DoRegExpExec(C,D,E){
return %_RegExpExec(C,D,E,t);
}
function RegExpExecNoTests(C,D,F){
var G=%_RegExpExec(C,D,F,t);
if(G!==null){
if((%_RegExpFlags(C)&8))C.lastIndex=G[4];
var H=((G)[0])>>1;
var F=G[3];
var I=G[4];
var J=%_SubString(D,F,I);
var K=%_RegExpConstructResult(H,F,D);
K[0]=J;
if(H==1)return K;
var L=3+2;
for(var M=1;M<H;M++){
F=G[L++];
if(F!=-1){
I=G[L];
K[M]=%_SubString(D,F,I);
}
L++;
}
return K;
;
}
C.lastIndex=0;
return null;
}
function RegExpSubclassExecJS(D){
if(!(%_IsRegExp(this))){
throw k(44,
'RegExp.prototype.exec',this);
}
D=(%_ToString(D));
var N=this.lastIndex;
var M=(%_ToLength(N));
var a=(!!((%_RegExpFlags(this)&1)));
var O=(!!((%_RegExpFlags(this)&8)));
var P=a||O;
if(P){
if(M>D.length){
this.lastIndex=0;
return null;
}
}else{
M=0;
}
var Q=%_RegExpExec(this,D,M,t);
if((Q===null)){
this.lastIndex=0;
return null;
}
if(P){
this.lastIndex=t[4];
}
var H=((Q)[0])>>1;
var F=Q[3];
var I=Q[4];
var J=%_SubString(D,F,I);
var K=%_RegExpConstructResult(H,F,D);
K[0]=J;
if(H==1)return K;
var L=3+2;
for(var M=1;M<H;M++){
F=Q[L++];
if(F!=-1){
I=Q[L];
K[M]=%_SubString(D,F,I);
}
L++;
}
return K;
;
}
%FunctionRemovePrototype(RegExpSubclassExecJS);
function RegExpExecJS(D){
if(!(%_IsRegExp(this))){
throw k(44,
'RegExp.prototype.exec',this);
}
D=(%_ToString(D));
var N=this.lastIndex;
var M=(%_ToLength(N));
var P=(%_RegExpFlags(this)&1)||(%_RegExpFlags(this)&8);
if(P){
if(M<0||M>D.length){
this.lastIndex=0;
return null;
}
}else{
M=0;
}
var Q=%_RegExpExec(this,D,M,t);
if((Q===null)){
this.lastIndex=0;
return null;
}
if(P){
this.lastIndex=t[4];
}
var H=((Q)[0])>>1;
var F=Q[3];
var I=Q[4];
var J=%_SubString(D,F,I);
var K=%_RegExpConstructResult(H,F,D);
K[0]=J;
if(H==1)return K;
var L=3+2;
for(var M=1;M<H;M++){
F=Q[L++];
if(F!=-1){
I=Q[L];
K[M]=%_SubString(D,F,I);
}
L++;
}
return K;
;
}
function RegExpSubclassExec(C,D,R){
if((R===(void 0))){
R=C.exec;
}
if((typeof(R)==='function')){
var K=%_Call(R,C,D);
if(!(%_IsJSReceiver(K))&&!(K===null)){
throw k(49);
}
return K;
}
return %_Call(RegExpExecJS,C,D);
}
%SetForceInlineFlag(RegExpSubclassExec);
var S;
var T;
function RegExpTest(D){
if(!(%_IsRegExp(this))){
throw k(44,
'RegExp.prototype.test',this);
}
D=(%_ToString(D));
var N=this.lastIndex;
var M=(%_ToLength(N));
if((%_RegExpFlags(this)&1)||(%_RegExpFlags(this)&8)){
if(M<0||M>D.length){
this.lastIndex=0;
return false;
}
var Q=%_RegExpExec(this,D,M,t);
if((Q===null)){
this.lastIndex=0;
return false;
}
this.lastIndex=t[4];
return true;
}else{
var C=this;
var U=(%_RegExpSource(C));
if(C.length>=3&&
%_StringCharCodeAt(C,0)==46&&
%_StringCharCodeAt(C,1)==42&&
%_StringCharCodeAt(C,2)!=63){
C=TrimRegExp(C);
}
var Q=%_RegExpExec(C,D,0,t);
if((Q===null)){
this.lastIndex=0;
return false;
}
return true;
}
}
function RegExpSubclassTest(D){
if(!(%_IsJSReceiver(this))){
throw k(44,
'RegExp.prototype.test',this);
}
D=(%_ToString(D));
var V=RegExpSubclassExec(this,D);
return!(V===null);
}
%FunctionRemovePrototype(RegExpSubclassTest);
function TrimRegExp(C){
if(S!==C){
S=C;
T=
new g(
%_SubString((%_RegExpSource(C)),2,(%_RegExpSource(C)).length),
((%_RegExpFlags(C)&2)?(%_RegExpFlags(C)&4)?"im":"i"
:(%_RegExpFlags(C)&4)?"m":""));
}
return T;
}
function RegExpToString(){
if(!(%_IsJSReceiver(this))){
throw k(
44,'RegExp.prototype.toString',this);
}
if(this===h){
%IncrementUseCounter(12);
}
return'/'+(%_ToString(this.source))+'/'+(%_ToString(this.flags));
}
function AtSurrogatePair(W,E){
if(E+1>=W.length)return false;
var J=%_StringCharCodeAt(W,E);
if(J<0xD800||J>0xDBFF)return false;
var X=%_StringCharCodeAt(W,E+1);
return X>=0xDC00||X<=0xDFFF;
}
function RegExpSplit(D,Y){
if(!(%_IsRegExp(this))){
throw k(44,
"RegExp.prototype.@@split",this);
}
var Z=this;
var W=(%_ToString(D));
Y=((Y===(void 0)))?4294967295:((Y)>>>0);
var aa=W.length;
if(Y===0)return[];
if(aa===0){
if(DoRegExpExec(Z,W,0,0)!==null)return[];
return[W];
}
var ab=0;
var ac=0;
var ad=0;
var K=new i();
outer_loop:
while(true){
if(ac===aa){
K[K.length]=%_SubString(W,ab,aa);
break;
}
var G=DoRegExpExec(Z,W,ac);
if(G===null||aa===(ad=G[3])){
K[K.length]=%_SubString(W,ab,aa);
break;
}
var ae=G[4];
if(ac===ae&&ae===ab){
if((%_RegExpFlags(this)&16)&&AtSurrogatePair(W,ac)){
ac+=2;
}else{
ac++;
}
continue;
}
K[K.length]=%_SubString(W,ab,ad);
if(K.length===Y)break;
var af=((G)[0])+3;
for(var M=3+2;M<af;){
var F=G[M++];
var I=G[M++];
if(I!=-1){
K[K.length]=%_SubString(W,F,I);
}else{
K[K.length]=(void 0);
}
if(K.length===Y)break outer_loop;
}
ac=ab=ae;
}
var ag=[];
%MoveArrayContents(K,ag);
return ag;
}
function RegExpSubclassSplit(D,Y){
if(!(%_IsJSReceiver(this))){
throw k(44,
"RegExp.prototype.@@split",this);
}
D=(%_ToString(D));
var ah=r(this,g);
var y=(%_ToString(this.flags));
var R;
if((%_IsRegExp(this))&&ah===g){
R=this.exec;
if(R===RegExpSubclassExecJS){
return %_Call(RegExpSplit,this,D,Y);
}
}
var ai=%StringIndexOf(y,'u',0)>=0;
var O=%StringIndexOf(y,'y',0)>=0;
var aj=O?y:y+"y";
var ak=new ah(this,aj);
var al=new e();
var am=0;
var an=((Y===(void 0)))?4294967295:((Y)>>>0);
var ao=D.length;
var ap=0;
if(an===0)return al;
var K;
if(ao===0){
K=RegExpSubclassExec(ak,D);
if((K===null))c(al,0,D);
return al;
}
var aq=ap;
while(aq<ao){
ak.lastIndex=aq;
K=RegExpSubclassExec(ak,D,R);
R=(void 0);
if((K===null)){
aq+=AdvanceStringIndex(D,aq,ai);
}else{
var I=m((%_ToLength(ak.lastIndex)),ao);
if(I===aq){
aq+=AdvanceStringIndex(D,aq,ai);
}else{
c(
al,am,
%_SubString(D,ap,aq));
am++;
if(am===an)return al;
ap=I;
var ar=l((%_ToLength(K.length)),0);
for(var M=1;M<ar;M++){
c(al,am,K[M]);
am++;
if(am===an)return al;
}
aq=ap;
}
}
}
c(al,am,
%_SubString(D,ap,ao));
return al;
}
%FunctionRemovePrototype(RegExpSubclassSplit);
function RegExpMatch(D){
if(!(%_IsRegExp(this))){
throw k(44,
"RegExp.prototype.@@match",this);
}
var W=(%_ToString(D));
if(!(%_RegExpFlags(this)&1))return RegExpExecNoTests(this,W,0);
this.lastIndex=0;
var K=%StringMatch(W,this,t);
return K;
}
function RegExpSubclassMatch(D){
if(!(%_IsJSReceiver(this))){
throw k(44,
"RegExp.prototype.@@match",this);
}
D=(%_ToString(D));
var a=this.global;
if(!a)return RegExpSubclassExec(this,D);
var ai=this.unicode;
this.lastIndex=0;
var al=new i();
var as=0;
var K;
while(true){
K=RegExpSubclassExec(this,D);
if((K===null)){
if(as===0)return null;
break;
}
var at=(%_ToString(K[0]));
al[as]=at;
if(at==="")SetAdvancedStringIndex(this,D,ai);
as++;
}
var au=[];
%MoveArrayContents(al,au);
return au;
}
%FunctionRemovePrototype(RegExpSubclassMatch);
var av=new i(4);
function StringReplaceGlobalRegExpWithFunction(W,C,aw){
var au=av;
if(au){
av=null;
}else{
au=new i(16);
}
var ax=%RegExpExecMultiple(C,
W,
t,
au);
C.lastIndex=0;
if((ax===null)){
av=au;
return W;
}
var ay=ax.length;
if(((t)[0])==2){
var az=0;
for(var M=0;M<ay;M++){
var aA=ax[M];
if(%_IsSmi(aA)){
if(aA>0){
az=(aA>>11)+(aA&0x7ff);
}else{
az=ax[++M]-aA;
}
}else{
var aB=aw(aA,az,W);
ax[M]=(%_ToString(aB));
az+=aA.length;
}
}
}else{
for(var M=0;M<ay;M++){
var aA=ax[M];
if(!%_IsSmi(aA)){
var aB=%reflect_apply(aw,(void 0),aA);
ax[M]=(%_ToString(aB));
}
}
}
var K=%StringBuilderConcat(ax,ay,W);
au.length=0;
av=au;
return K;
}
function CaptureString(D,aC,E){
var aD=E<<1;
var F=aC[(3+(aD))];
if(F<0)return;
var I=aC[(3+(aD+1))];
return %_SubString(D,F,I);
}
function StringReplaceNonGlobalRegExpWithFunction(W,C,aw){
var G=DoRegExpExec(C,W,0);
if((G===null)){
C.lastIndex=0;
return W;
}
var E=G[3];
var K=%_SubString(W,0,E);
var aE=G[4];
var aF=((G)[0])>>1;
var aG;
if(aF==1){
var aH=%_SubString(W,E,aE);
aG=aw(aH,E,W);
}else{
var aI=new i(aF+2);
for(var L=0;L<aF;L++){
aI[L]=CaptureString(W,G,L);
}
aI[L]=E;
aI[L+1]=W;
aG=%reflect_apply(aw,(void 0),aI);
}
K+=aG;
return K+%_SubString(W,aE,W.length);
}
function RegExpReplace(D,aw){
if(!(%_IsRegExp(this))){
throw k(44,
"RegExp.prototype.@@replace",this);
}
var W=(%_ToString(D));
var aJ=this;
if(!(typeof(aw)==='function')){
aw=(%_ToString(aw));
if(!(%_RegExpFlags(aJ)&1)){
var V=DoRegExpExec(aJ,W,0);
if(V==null){
aJ.lastIndex=0
return W;
}
if(aw.length==0){
return %_SubString(W,0,V[3])+
%_SubString(W,V[4],W.length)
}
return d(aw,W,t,
%_SubString(W,0,V[3]))+
%_SubString(W,V[4],W.length);
}
aJ.lastIndex=0;
return %StringReplaceGlobalRegExpWithString(
W,aJ,aw,t);
}
if((%_RegExpFlags(aJ)&1)){
return StringReplaceGlobalRegExpWithFunction(W,aJ,aw);
}
return StringReplaceNonGlobalRegExpWithFunction(W,aJ,aw);
}
function GetSubstitution(aK,D,aL,aM,aG){
var aN=aK.length;
var aO=D.length;
var aP=aM.length;
var aQ=aL+aN;
var K="";
var aR,aS,aT,aU,aV,aW,aX;
var aU=%StringIndexOf(aG,'$',0);
if(aU<0){
K+=aG;
return K;
}
if(aU>0)K+=%_SubString(aG,0,aU);
while(true){
aS='$';
aR=aU+1;
if(aR<aG.length){
aT=%_StringCharCodeAt(aG,aR);
if(aT==36){
++aR;
K+='$';
}else if(aT==38){
++aR;
K+=aK;
}else if(aT==96){
++aR;
K+=%_SubString(D,0,aL);
}else if(aT==39){
++aR;
K+=%_SubString(D,aQ,aO);
}else if(aT>=48&&aT<=57){
aV=(aT-48);
aW=1;
if(aR+1<aG.length){
aU=%_StringCharCodeAt(aG,aR+1);
if(aU>=48&&aU<=57){
aX=aV*10+((aU-48));
if(aX<aP){
aV=aX;
aW=2;
}
}
}
if(aV!=0&&aV<aP){
var aY=aM[aV];
if(!(aY===(void 0)))K+=aY;
aR+=aW;
}else{
K+='$';
}
}else{
K+='$';
}
}else{
K+='$';
}
aU=%StringIndexOf(aG,'$',aR);
if(aU<0){
if(aR<aG.length){
K+=%_SubString(aG,aR,aG.length);
}
return K;
}
if(aU>aR){
K+=%_SubString(aG,aR,aU);
}
}
return K;
}
function AdvanceStringIndex(D,E,ai){
var aZ=1;
if(ai){
var J=%_StringCharCodeAt(D,E);
if(J>=0xD800&&J<=0xDBFF&&D.length>E+1){
var X=%_StringCharCodeAt(D,E+1);
if(X>=0xDC00&&X<=0xDFFF){
aZ=2;
}
}
}
return aZ;
}
function SetAdvancedStringIndex(C,D,ai){
var N=C.lastIndex;
C.lastIndex=N+
AdvanceStringIndex(D,N,ai);
}
function RegExpSubclassReplace(D,aw){
if(!(%_IsJSReceiver(this))){
throw k(44,
"RegExp.prototype.@@replace",this);
}
D=(%_ToString(D));
var aa=D.length;
var ba=(typeof(aw)==='function');
if(!ba)aw=(%_ToString(aw));
var a=(!!(this.global));
if(a){
var ai=(!!(this.unicode));
this.lastIndex=0;
}
var R;
if((%_IsRegExp(this))){
R=this.exec;
if(R===RegExpSubclassExecJS){
return %_Call(RegExpReplace,this,D,aw);
}
}
var bb=new i();
var K,aG;
while(true){
K=RegExpSubclassExec(this,D,R);
R=(void 0);
if((K===null)){
break;
}else{
bb.push(K);
if(!a)break;
var at=(%_ToString(K[0]));
if(at==="")SetAdvancedStringIndex(this,D,ai);
}
}
var bc="";
var bd=0;
for(var M=0;M<bb.length;M++){
K=bb[M];
var aP=l((%_ToLength(K.length)),0);
var aK=(%_ToString(K[0]));
var be=aK.length;
var aL=l(m((%_ToInteger(K.index)),aa),0);
var aM=new i();
for(var as=0;as<aP;as++){
var aY=K[as];
if(!(aY===(void 0)))aY=(%_ToString(aY));
aM[as]=aY;
}
if(ba){
var aI=new i(aP+2);
for(var L=0;L<aP;L++){
aI[L]=aM[L];
}
aI[L]=aL;
aI[L+1]=D;
aG=%reflect_apply(aw,(void 0),aI,0,
aI.length);
}else{
aG=GetSubstitution(aK,D,aL,aM,
aw);
}
if(aL>=bd){
bc+=
%_SubString(D,bd,aL)+aG;
bd=aL+be;
}
}
if(bd>=aa)return bc;
return bc+%_SubString(D,bd,aa);
}
%FunctionRemovePrototype(RegExpSubclassReplace);
function RegExpSearch(D){
if(!(%_IsRegExp(this))){
throw k(44,
"RegExp.prototype.@@search",this);
}
var V=DoRegExpExec(this,(%_ToString(D)),0);
if(V)return V[3];
return-1;
}
function RegExpSubclassSearch(D){
if(!(%_IsJSReceiver(this))){
throw k(44,
"RegExp.prototype.@@search",this);
}
D=(%_ToString(D));
var bf=this.lastIndex;
this.lastIndex=0;
var K=RegExpSubclassExec(this,D);
this.lastIndex=bf;
if((K===null))return-1;
return K.index;
}
%FunctionRemovePrototype(RegExpSubclassSearch);
function RegExpGetLastMatch(){
var bg=((t)[1]);
return %_SubString(bg,
t[3],
t[4]);
}
function RegExpGetLastParen(){
var aa=((t)[0]);
if(aa<=2)return'';
var bg=((t)[1]);
var F=t[(3+(aa-2))];
var I=t[(3+(aa-1))];
if(F!=-1&&I!=-1){
return %_SubString(bg,F,I);
}
return"";
}
function RegExpGetLeftContext(){
var bh;
var W;
bh=t[3];
W=((t)[1]);
return %_SubString(W,0,bh);
}
function RegExpGetRightContext(){
var bh;
var W;
bh=t[4];
W=((t)[1]);
return %_SubString(W,bh,W.length);
}
function RegExpMakeCaptureGetter(as){
return function foo(){
var E=as*2;
if(E>=((t)[0]))return'';
var bi=t[(3+(E))];
var bj=t[(3+(E+1))];
if(bi==-1||bj==-1)return'';
return %_SubString(((t)[1]),bi,bj);
};
}
function RegExpGetFlags(){
if(!(%_IsJSReceiver(this))){
throw k(
133,"RegExp.prototype.flags",(%_ToString(this)));
}
var K='';
if(this.global)K+='g';
if(this.ignoreCase)K+='i';
if(this.multiline)K+='m';
if(this.unicode)K+='u';
if(this.sticky)K+='y';
return K;
}
function RegExpGetGlobal(){
if(!(%_IsRegExp(this))){
if(this===h){
%IncrementUseCounter(31);
return(void 0);
}
throw k(134,"RegExp.prototype.global");
}
return(!!((%_RegExpFlags(this)&1)));
}
%SetForceInlineFlag(RegExpGetGlobal);
function RegExpGetIgnoreCase(){
if(!(%_IsRegExp(this))){
if(this===h){
%IncrementUseCounter(31);
return(void 0);
}
throw k(134,"RegExp.prototype.ignoreCase");
}
return(!!((%_RegExpFlags(this)&2)));
}
function RegExpGetMultiline(){
if(!(%_IsRegExp(this))){
if(this===h){
%IncrementUseCounter(31);
return(void 0);
}
throw k(134,"RegExp.prototype.multiline");
}
return(!!((%_RegExpFlags(this)&4)));
}
function RegExpGetSource(){
if(!(%_IsRegExp(this))){
if(this===h){
%IncrementUseCounter(30);
return"(?:)";
}
throw k(134,"RegExp.prototype.source");
}
return(%_RegExpSource(this));
}
function RegExpGetSticky(){
if(!(%_IsRegExp(this))){
if(this===h){
%IncrementUseCounter(11);
return(void 0);
}
throw k(134,"RegExp.prototype.sticky");
}
return(!!((%_RegExpFlags(this)&8)));
}
%SetForceInlineFlag(RegExpGetSticky);
%FunctionSetInstanceClassName(g,'RegExp');
h=new f();
%FunctionSetPrototype(g,h);
%AddNamedProperty(
g.prototype,'constructor',g,2);
%SetCode(g,RegExpConstructor);
b.InstallFunctions(g.prototype,2,[
"exec",RegExpExecJS,
"test",RegExpTest,
"toString",RegExpToString,
"compile",RegExpCompileJS,
n,RegExpMatch,
o,RegExpReplace,
p,RegExpSearch,
q,RegExpSplit,
]);
b.InstallGetter(g.prototype,'flags',RegExpGetFlags);
b.InstallGetter(g.prototype,'global',RegExpGetGlobal);
b.InstallGetter(g.prototype,'ignoreCase',RegExpGetIgnoreCase);
b.InstallGetter(g.prototype,'multiline',RegExpGetMultiline);
b.InstallGetter(g.prototype,'source',RegExpGetSource);
b.InstallGetter(g.prototype,'sticky',RegExpGetSticky);
var bk=function(){
var bl=((t)[2]);
return(bl===(void 0))?"":bl;
};
var bm=function(D){
((t)[2])=(%_ToString(D));
};
%OptimizeObjectForAddingMultipleProperties(g,22);
b.InstallGetterSetter(g,'input',bk,bm,
4);
b.InstallGetterSetter(g,'$_',bk,bm,
2|4);
var bn=function(bo){};
b.InstallGetterSetter(g,'lastMatch',RegExpGetLastMatch,
bn,4);
b.InstallGetterSetter(g,'$&',RegExpGetLastMatch,bn,
2|4);
b.InstallGetterSetter(g,'lastParen',RegExpGetLastParen,
bn,4);
b.InstallGetterSetter(g,'$+',RegExpGetLastParen,bn,
2|4);
b.InstallGetterSetter(g,'leftContext',RegExpGetLeftContext,
bn,4);
b.InstallGetterSetter(g,'$`',RegExpGetLeftContext,bn,
2|4);
b.InstallGetterSetter(g,'rightContext',RegExpGetRightContext,
bn,4);
b.InstallGetterSetter(g,"$'",RegExpGetRightContext,bn,
2|4);
for(var M=1;M<10;++M){
b.InstallGetterSetter(g,'$'+M,RegExpMakeCaptureGetter(M),
bn,4);
}
%ToFastProperties(g);
var bp=new j(2,"",(void 0),0,0);
function InternalRegExpMatch(C,W){
var G=%_RegExpExec(C,W,0,bp);
if(!(G===null)){
var H=((G)[0])>>1;
var F=G[3];
var I=G[4];
var J=%_SubString(W,F,I);
var K=%_RegExpConstructResult(H,F,W);
K[0]=J;
if(H==1)return K;
var L=3+2;
for(var M=1;M<H;M++){
F=G[L++];
if(F!=-1){
I=G[L];
K[M]=%_SubString(W,F,I);
}
L++;
}
return K;
;
}
return null;
}
function InternalRegExpReplace(C,W,aG){
return %StringReplaceGlobalRegExpWithString(
W,C,aG,bp);
}
b.Export(function(bq){
bq.InternalRegExpMatch=InternalRegExpMatch;
bq.InternalRegExpReplace=InternalRegExpReplace;
bq.IsRegExp=IsRegExp;
bq.RegExpExec=DoRegExpExec;
bq.RegExpInitialize=RegExpInitialize;
bq.RegExpLastMatchInfo=t;
bq.RegExpSubclassExecJS=RegExpSubclassExecJS;
bq.RegExpSubclassMatch=RegExpSubclassMatch;
bq.RegExpSubclassReplace=RegExpSubclassReplace;
bq.RegExpSubclassSearch=RegExpSubclassSearch;
bq.RegExpSubclassSplit=RegExpSubclassSplit;
bq.RegExpSubclassTest=RegExpSubclassTest;
bq.RegExpTest=RegExpTest;
});
})

,arraybuffer