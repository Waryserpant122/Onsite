

/*Make resizable div by Hung Nguyen*/
function makeresizableDiv(div) {
  const element = document.querySelector(div);
  const resizers = document.querySelectorAll(div + ' .resizer')
  const font=document.getElementById('p');
  const minimum_size = 40;
  let original_width = 0;
  let original_height = 0;
  let original_x = 0;
  let original_y = 0;
  let original_mouse_x = 0;
  let original_mouse_y = 0;

   const rotate = document.querySelector(div + ' .rotate')
   rotate.addEventListener('mousedown',function(e)
   {

     e.preventDefault();
     window.addEventListener('mousemove', rotateElement)
     // window.addEventListener('mouseup', resize1)
      window.addEventListener('mouseup', closeRotElement)


   })

      function rotateElement() {
                var pos5 = 0, pos6 = 0, pos7 = 0, pos8 = 0;
                if (document.getElementById("mydivrotate")) {
                    document.getElementById("mydivrotate").onmousedown = rotateMouseDown;
                }

                function rotateMouseDown(e) {
                    console.log("4");
                    e = e || window.event;
                    e.preventDefault();
                    pos7 = e.clientX;
                    pos8 = e.clientY;
                    document.onmouseup = closeRotElement;
                    document.onmousemove = elementRot;
                }

                function elementRot(e) {
                    console.log("5");
                    e = e || window.event;
                    e.preventDefault();
                    pos5 = pos7 - e.clientX;
                    pos6 = pos8 - e.clientY;
                    pos7 = e.clientX;
                    pos8 = e.clientY;
                    var divele= document.getElementById("mydivrotate");
                    var diveleRects = divele.getBoundingClientRect();
                    var diveleX = diveleRects.left + diveleRects.width / 2;
                    var diveleY = diveleRects.top + diveleRects.height / 2;
                    element.style.transform = "rotate(" + Math.atan2(event.clientY - diveleY, event.clientX - diveleX) + "rad)";
                }

                function closeRotElement() {
                    document.onmouseup = null;
                    document.onmousemove = null;
                }
            }
  for (let i = 0;i < resizers.length; i++) {
    const currentResizer = resizers[i];
    currentResizer.addEventListener('mousedown', function(e) {
      e.preventDefault()
      original_width = parseFloat(getComputedStyle(element, null).getPropertyValue('width').replace('px', ''));
      original_height = parseFloat(getComputedStyle(element, null).getPropertyValue('height').replace('px', ''));
      original_x = element.getBoundingClientRect().left;
      original_y = element.getBoundingClientRect().top;
      original_mouse_x = e.pageX;
      original_mouse_y = e.pageY;
      window.addEventListener('mousemove', resize)
     // window.addEventListener('mouseup', resize1)
      window.addEventListener('mouseup', stopResize)
    })
    var a,b,c;
    function resize(e) {
      if (currentResizer.classList.contains('bottom-right')) {
        const width = original_width + (e.pageX - original_mouse_x);
        const height = original_height + (e.pageY - original_mouse_y);
        
        a=width/original_width;
        b=height/original_height;
        c=Math.sqrt(a*b);
        font.style.font.size*=c;
        if (width > minimum_size) {
          element.style.width = width + 'px'
        }
        if (height > minimum_size) {
          element.style.height = height + 'px'
        }
      }
      else if (currentResizer.classList.contains('top-center')) {
        const height = original_height + (e.pageY - original_mouse_y)
        //const width = original_width - (e.pageX - original_mouse_x)
         a=width/original_width;
        b=height/original_height;
        if (height > minimum_size) {
          element.style.height = height + 'px'
        }
      }
      
     // else if(currentResizer.classList.contains('rotate'))  

      else if (currentResizer.classList.contains('bottom-left')) {
        const height = original_height + (e.pageY - original_mouse_y)
        const width = original_width - (e.pageX - original_mouse_x)
         a=width/original_width;
        b=height/original_height;
        if (height > minimum_size) {
          element.style.height = height + 'px'
        }
        if (width > minimum_size) {
          element.style.width = width + 'px'
          element.style.left = original_x + (e.pageX - original_mouse_x) + 'px'
        }
      }
     
      else if (currentResizer.classList.contains('top-right')) {
        const width = original_width + (e.pageX - original_mouse_x)
        const height = original_height - (e.pageY - original_mouse_y)
         a=width/original_width;
        b=height/original_height;
        if (width > minimum_size) {
          element.style.width = width + 'px'
        }
        if (height > minimum_size) {
          element.style.height = height + 'px'
          element.style.top = original_y + (e.pageY - original_mouse_y) + 'px'
        }
      }
      else {
        const width = original_width - (e.pageX - original_mouse_x)
        const height = original_height - (e.pageY - original_mouse_y)
         a=width/original_width;
        b=height/original_height;
        if (width > minimum_size) {
          element.style.width = width + 'px'
          element.style.left = original_x + (e.pageX - original_mouse_x) + 'px'
        }
        if (height > minimum_size) {
          element.style.height = height + 'px'
          element.style.top = original_y + (e.pageY - original_mouse_y) + 'px'
        }
      }
    }
    
    function stopResize() {
      window.removeEventListener('mousemove', resize)
    }
  }
}

makeresizableDiv('.resizable')

dragElement(document.getElementById("mydiv"));

function dragElement(element) {
  var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
  if (document.getElementById(element.id + "header")) {
  
    document.getElementById(element.id + "header").onmousedown = dragMouseDown;
  } else {
  
    element.onmousedown = dragMouseDown;
  }

  function dragMouseDown(e) {
    e = e || window.event;
    e.preventDefault();
    
    pos3 = e.clientX;
    pos4 = e.clientY;
    document.onmouseup = closeDragElement;
  
    document.onmousemove = elementDrag;
  }

  function elementDrag(e) {
    e = e || window.event;
    e.preventDefault();
   
    pos1 = pos3 - e.clientX;
    pos2 = pos4 - e.clientY;
    pos3 = e.clientX;
    pos4 = e.clientY;
   
    element.style.top = (element.offsetTop - pos2) + "px";
    element.style.left = (element.offsetLeft - pos1) + "px";
  }

  function closeDragElement() {
    // stop moving when mouse button is released:
    document.onmouseup = null;
    document.onmousemove = null;
  }
}

/*rotateElement(document.getElementById("mydiv"));

function rotateElement(element) {
                var pos5 = 0, pos6 = 0, pos7 = 0, pos8 = 0;
                if (document.getElementById(element.id + "rotate")) {
                    document.getElementById(element.id + "rotate").onmousedown = rotateMouseDown;
                }

                function rotateMouseDown(e) {

                    e = e || window.event;
                    e.preventDefault();
                    pos7 = e.clientX;
                    pos8 = e.clientY;
                    window.addEventListener('mousemove',elementRot);
                    window.addEventListener('mouseup',closeRotElement);
                }

                function elementRot(e) {
                  
                    e = e || window.event;
                    e.preventDefault();
                    pos5 = pos7 - e.clientX;
                    pos6 = pos8 - e.clientY;
                    pos7 = e.clientX;
                    pos8 = e.clientY;
                    var divele= document.getElementById("mydivrotate");
                    var diveleRects = divele.getBoundingClientRect();
                    var diveleX = diveleRects.left + diveleRects.width / 2;
                    var diveleY = diveleRects.top + diveleRects.height / 2;
                    element.style.transform = "rotate(" + Math.atan2(event.clientY - diveleY, event.clientX - diveleX) + "rad)";
                }

                function closeRotElement() {
                    window.removeEventListener('mouseup',elementRot);
                    document.onmouseup = null;
                    document.onmousemove = null;
                }
            }*/