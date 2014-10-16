window.exported = window.exported || {};
window.exported.openSharedPagesOverlay =  function() {
  var width = 900;
  var height = 600;
  document.getElementById("canvas").width =  width;
  document.getElementById("canvas").height =  height - 100;
  var c = document.getElementById("canvas");
  var ctx = c.getContext("2d");
  var pages = [];
  var pageWidth = 140;
  var pageHeight = 1.3*140;
  var error = new Image();
  error.src = "images/error.png";
  error.setAttribute('width', '12px');
  error.setAttribute('height', '12px');

  var drawPage = function(x,y,title, color, components) {
    pages.push({i:pages.length, x:x, y:y, title:title});
    ctx.beginPath();
    ctx.lineWidth="5";
    ctx.strokeStyle = color || "black";
    ctx.rect(x,y, pageWidth, pageHeight);
    ctx.font="17px Georgia";
    ctx.fillText(title,x + 10,y + 20);
    ctx.stroke();

    components = components || [];
    components.sort();
    for (var i=0; i<components.length; i++) {
      var comp;
      var alpha = 1;
      ctx.save();
      if (typeof components[i] == 'object') {
        comp = components[i][0];
        if (!components[i][1]) {
          ctx.drawImage(error, x - 25, y - 5 , 20, 20);
          ctx.globalAlpha = 0.5;
        }

      } else {
        comp = components[i];
      }
      //console.log('alpha');

      var img = new Image();
      img.src ='images/' + comp + '.png';
      img.setAttribute('width', '12px');
      img.setAttribute('height', '12px');
      ctx.font="15px Georgia";
      ctx.drawImage(img, x+ 10, y + (i+2)*20 -5);
      ctx.beginPath();
      ctx.fillText(comp + i,x + 30,y + (i+2)*20 + 10);
      ctx.stroke();
      ctx.restore();
    }
  }

  function connect(pagei, pagej, pos1, pos2) {
    var fromX;
    var fromY;
    function set(pagei, pos1) {
      var fromX;
      var fromY;
      if (pos1 == 'l') {
        fromX = pages[pagei].x;
        fromY = pages[pagei].y + pageHeight/2;
      } else if (pos1 == 'r') {
        fromX = pages[pagei].x + pageWidth;
        fromY = pages[pagei].y + pageHeight/2;
      } else if(pos1 == 't') {
        fromX = pages[pagei].x + pageWidth/2;
        fromY = pages[pagei].y;
      } else if(pos1 == 'b') {
        fromX = pages[pagei].x +pageWidth/2;
        fromY = pages[pagei].y + pageHeight;
      }
      return {x:fromX, y:fromY};
    }
    var from = set(pagei, pos1);
    var to = set(pagej, pos2);
    ctx.beginPath();
    ctx.strokeStyle = "black";
    ctx.moveTo(from.x, from.y);
    ctx.lineTo(to.x, to.y);
    ctx.stroke();
  }


  (function fullProject() {
    drawPage(5, 5, "screen1", undefined, ["label", "button", "button", "image", "listPicker"]);
    drawPage(55, 255, "screen2", undefined, ["canvas", "clock"]);
    drawPage(358, 38, "screen3", undefined, ["label", "label", "label", "button", "notifier"]);


    drawPage(708, 30, "library_page_1", "green");
    drawPage(289, 308, "shared_page_1", "green");
    drawPage(657, 295, "library_page_2", "green");

    connect(0, 4, 'r', 't');
    connect(1, 4, 'r', 'l');
    connect(2, 4, 'b', 't');
    connect(2, 3, 'r', 'l');
    connect(4, 5, 'r', 'l');
  });
  (function screen3() {
      drawPage(10, 200, "screen3", undefined, ["label", "label", "label", "button", "notifier"]);
      drawPage(400, 10, "library_page_1", "green");
      drawPage(380, 300, "shared_page_1", "green", [["label", false], ["button", true]]);
      connect(0, 2, 'r', 'l');
      connect(0, 1, 'r', 'l');
    })();

 document.getElementById('fade').style.display = 'block';
 document.getElementById('overlay').style.display = 'block';

 var clearOverlay = function() {
   document.getElementById('fade').style.display = 'none';
  document.getElementById('overlay').style.display = 'none';
 }
 document.getElementById('fade').addEventListener("click", clearOverlay);

 window.onkeyup = function(e) { //TODO (evan): I think this will overwrite any other onkeyup functions, make it add instead of replace
   if (e.keyCode == 27) { //escape key code
     clearOverlay();
   }
   //TODO (evan): remove the event click listeners you just added so you don't get a million listeners
 }
};
