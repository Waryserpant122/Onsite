
var i=0;


function capture()
{   
    i++;
    var body =document.getElementsByTagName('body');
    var b=body[0]
    var bodyc=[];
    bodyc[i]=b.innerHTML;
    console.log(bodyc[i])
   
    var li = document.createElement("li");
    var d= new Date();
    var t = i+".  " + d.getHours() + ":" + d.getMinutes() + ":" + d.getSeconds();
    li.innerHTML= t;
    //console.log(image);
    document.getElementById("myUL").appendChild(li);
    
    var btn = document.createElement("BUTTON");
    var btnarr = [];
    btn.innerHTML="View"


    btn.id = "btnid"+i;
    li.appendChild(btn);
    
  
}


