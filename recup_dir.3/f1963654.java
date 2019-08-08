bjectThrow(k){
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

8object-observe