ar m=%_ArrayBufferGetByteLength(this);
if(k<0){
l=e(m+k,0);
}else{
l=f(k,m);
}
var n=(j===(void 0))?m:j;
var o;
if(n<0){
o=e(m+n,0);
}else{
o=f(n,m);
}
if(o<l){
o=l;
}
var p=o-l;
var q=g(this,c,true);
var r=new q(p);
if(!(%_ClassOf(r)==='ArrayBuffer')){
throw d(44,
'ArrayBuffer.prototype.slice',r);
}
if(r===this){
throw d(11);
}
if(%_ArrayBufferGetByteLength(r)<p){
throw d(10);
}
%ArrayBufferSliceImpl(this,r,l,p);
return r;
}
b.InstallGetter(c.prototype,"byteLength",
ArrayBufferGetByteLen);
b.InstallFunctions(c.prototype,2,[
"slice",ArrayBufferSlice
]);
})

(typedarrayf