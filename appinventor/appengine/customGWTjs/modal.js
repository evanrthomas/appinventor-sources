var getPageDrawer = function(title, color, components) {
  return function drawPage(ctx, width, height) {
    var x = 0;
    var y = 0;
    var pageWidth = 140;
    var pageHeight = pageWidth*1.3;
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
}


var clearOverlay = function() {
  document.getElementById('fade').style.display = 'none';
  document.getElementById('overlay').style.display = 'none';
}

var openSharedPagesOverlay =  function() {

  (function initializeOverlay() {
    document.getElementById('fade').style.display = 'block';
    document.getElementById('overlay').style.display = 'block';
    document.getElementById('new_shared_page_btn').onclick = window.exported.newSharedPage;
    document.getElementById('fade').addEventListener("click", clearOverlay);

    window.onkeyup = function(e) { //TODO (evan): I think this will overwrite any other onkeyup functions, make it add instead of replace
      if (e.keyCode == 27) { //escape key code
        clearOverlay();
      }
      //TODO (evan): remove the event click listeners you just added so you don't get a million listeners
    }
  })();



  var pages = window.exported.getProjectPages();

  var nodes = [];
  var id = 0;
  var formPages = pages.formPages;
  var sharedPages = pages.sharedPages;

  for (var i = 0; i < formPages.length; i ++) {
    nodes.push({id: id++, 
      shape: 'custom',
      radius:200,
      customDraw:getPageDrawer(formPages[i].name, 'green'), 
    });
  }
  for (var i = 0; i < sharedPages.length; i ++) {
    var name = sharedPages[i].name;
    nodes.push({id: id++, 
      shape: 'custom',
      radius:200,
      customDraw:getPageDrawer(sharedPages[i].name, 'blue'), 
    });
  }

  // create an array with edges
  var edges = [
    {from: 1, to: 2},
  ];

  // create a network
  var container = document.getElementById('vis_network_area');
  var data = {
    nodes: nodes,
    edges: edges
  };
  var network = new vis.Network(container, data, {});


};

window.exported = window.exported || {};
window.exported.openSharedPagesOverlay = openSharedPagesOverlay;
