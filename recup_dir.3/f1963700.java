alue=(void 0);
n.done=true;
break;
case 2:
n.value=m[0];
break;
case 3:
m[1]=m[0];
break;
}
return n;
}
function SetEntries(){
if(!(%_ClassOf(this)==='Set')){
throw f(44,
'Set.prototype.entries',this);
}
return new i(this,3);
}
function SetValues(){
if(!(%_ClassOf(this)==='Set')){
throw f(44,
'Set.prototype.values',this);
}
return new i(this,2);
}
%SetCode(i,SetIteratorConstructor);
%FunctionSetInstanceClassName(i,'Set Iterator');
b.InstallFunctions(i.prototype,2,[
'next',SetIteratorNextJS
]);
%AddNamedProperty(i.prototype,h,
"Set Iterator",1|2);
b.InstallFunctions(d.prototype,2,[
'entries',SetEntries,
'keys',SetValues,
'values',SetValues
]);
%AddNamedProperty(d.prototype,e,SetValues,2);
function MapIteratorConstructor(o,l){
%MapIteratorInitialize(this,o,l);
}
function MapIteratorNextJS(){
if(!(%_ClassOf(this)==='Map Iterator')){
throw f(44,
'Map Iterator.prototype.next',this);
}
var m=[(void 0),(void 0)];
var n=%_CreateIterResultObject(m,false);
switch(%MapIteratorNext(this,m)){
case 0:
n.value=(void 0);
n.done=true;
break;
case 1:
n.value=m[0];
break;
case 2:
n.value=m[1];
break;
}
return n;
}
function MapEntries(){
if(!(%_ClassOf(this)==='Map')){
throw f(44,
'Map.prototype.entries',this);
}
return new g(this,3);
}
function MapKeys(){
if(!(%_ClassOf(this)==='Map')){
throw f(44,
'Map.prototype.keys',this);
}
return new g(this,1);
}
function MapValues(){
if(!(%_ClassOf(this)==='Map')){
throw f(44,
'Map.prototype.values',this);
}
return new g(this,2);
}
%SetCode(g,MapIteratorConstructor);
%FunctionSetInstanceClassName(g,'Map Iterator');
b.InstallFunctions(g.prototype,2,[
'next',MapIteratorNextJS
]);
%AddNamedProperty(g.prototype,h,
"Map Iterator",1|2);
b.InstallFunctions(c.prototype,2,[
'entries',MapEntries,
'keys',MapKeys,
'values',MapValues
]);
%AddNamedProperty(c.prototype,e,MapEntries,2);
b.Export(function(p){
p.MapEntries=MapEntries;
p.MapIteratorNext=MapIteratorNextJS;
p.SetIteratorNext=SetIteratorNextJS;
p.SetValues=SetValues;
});
})

