MakeTypeError;
});
function SymbolToPrimitive(l){
if(!((typeof(this)==='symbol')||(%_ClassOf(this)==='Symbol'))){
throw g(44,
"Symbol.prototype [ @@toPrimitive ]",this);
}
return %_ValueOf(this);
}
function SymbolToString(){
if(!((typeof(this)==='symbol')||(%_ClassOf(this)==='Symbol'))){
throw g(44,
"Symbol.prototype.toString",this);
}
return %SymbolDescriptiveString(%_ValueOf(this));
}
function SymbolValueOf(){
if(!((typeof(this)==='symbol')||(%_ClassOf(this)==='Symbol'))){
throw g(44,
"Symbol.prototype.valueOf",this);
}
return %_ValueOf(this);
}
function SymbolFor(m){
m=(%_ToString(m));
var n=%SymbolRegistry();
if((n.for[m]===(void 0))){
var o=%CreateSymbol(m);
n.for[m]=o;
n.keyFor[o]=m;
}
return n.for[m];
}
function SymbolKeyFor(o){
if(!(typeof(o)==='symbol'))throw g(147,o);
return %SymbolRegistry().keyFor[o];
}
b.InstallConstants(c,[
"hasInstance",d,
"isConcatSpreadable",e,
"iterator",f,
"toPrimitive",h,
"toStringTag",i,
"unscopables",j,
]);
b.InstallFunctions(c,2,[
"for",SymbolFor,
"keyFor",SymbolKeyFor
]);
%AddNamedProperty(
c.prototype,i,"Symbol",2|1);
b.InstallFunctions(c.prototype,2|1,[
h,SymbolToPrimitive
]);
b.InstallFunctions(c.prototype,2,[
"toString",SymbolToString,
"valueOf",SymbolValueOf
]);
b.Export(function(p){
p.SymbolToString=SymbolToString;
})
})

