
if(!(%_IsJSReceiver(m))){
throw h(52,m);
}
%_Call(l,this,m[0],m[1]);
}
}
}
function WeakMapGet(n){
if(!(%_ClassOf(this)==='WeakMap')){
throw h(44,
'WeakMap.prototype.get',this);
}
if(!(%_IsJSReceiver(n)))return(void 0);
var o=c(n);
if((o===(void 0)))return(void 0);
return %WeakCollectionGet(this,n,o);
}
function WeakMapSet(n,p){
if(!(%_ClassOf(this)==='WeakMap')){
throw h(44,
'WeakMap.prototype.set',this);
}
if(!(%_IsJSReceiver(n)))throw h(174);
return %WeakCollectionSet(this,n,p,d(n));
}
function WeakMapHas(n){
if(!(%_ClassOf(this)==='WeakMap')){
throw h(44,
'WeakMap.prototype.has',this);
}
if(!(%_IsJSReceiver(n)))return false;
var o=c(n);
if((o===(void 0)))return false;
return %WeakCollectionHas(this,n,o);
}
function WeakMapDelete(n){
if(!(%_ClassOf(this)==='WeakMap')){
throw h(44,
'WeakMap.prototype.delete',this);
}
if(!(%_IsJSReceiver(n)))return false;
var o=c(n);
if((o===(void 0)))return false;
return %WeakCollectionDelete(this,n,o);
}
%SetCode(f,WeakMapConstructor);
%FunctionSetLength(f,0);
%FunctionSetPrototype(f,new e());
%AddNamedProperty(f.prototype,"constructor",f,
2);
%AddNamedProperty(f.prototype,i,"WeakMap",
2|1);
b.InstallFunctions(f.prototype,2,[
"get",WeakMapGet,
"set",WeakMapSet,
"has",WeakMapHas,
"delete",WeakMapDelete
]);
function WeakSetConstructor(k){
if((new.target===(void 0))){
throw h(28,"WeakSet");
}
%WeakCollectionInitialize(this);
if(!(k==null)){
var l=this.add;
if(!(typeof(l)==='function')){
throw h(96,l,'add',this);
}
for(var p of k){
%_Call(l,this,p);
}
}
}
function WeakSetAdd(p){
if(!(%_ClassOf(this)==='WeakSet')){
throw h(44,
'WeakSet.prototype.add',this);
}
if(!(%_IsJSReceiver(p)))throw h(175);
return %WeakCollectionSet(this,p,true,d(p));
}
function WeakSetHas(p){
if(!(%_ClassOf(this)==='WeakSet')){
throw h(44,
'WeakSet.prototype.has',this);
}
if(!(%_IsJSReceiver(p)))return false;
var o=c(p);
if((o===(void 0)))return false;
return %WeakCollectionHas(this,p,o);
}
function WeakSetDelete(p){
if(!(%_ClassOf(this)==='WeakSet')){
throw h(44,
'WeakSet.prototype.delete',this);
}
if(!(%_IsJSReceiver(p)))return false;
var o=c(p);
if((o===(void 0)))return false;
return %WeakCollectionDelete(this,p,o);
}
%SetCode(g,WeakSetConstructor);
%FunctionSetLength(g,0);
%FunctionSetPrototype(g,new e());
%AddNamedProperty(g.prototype,"constructor",g,
2);
%AddNamedProperty(g.prototype,i,"WeakSet",
2|1);
b.InstallFunctions(g.prototype,2,[
"add",WeakSetAdd,
"has",WeakSetHas,
"delete",WeakSetDelete
]);
})

Lcollection-iterator