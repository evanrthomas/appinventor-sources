var getPageDrawer = function(title, color, components) {
  return function drawPage(ctx, x, y, node) {
    var pageWidth = 100;
    var pageHeight = pageWidth*1.3;
    var x = x - pageWidth/2;
    var y = y - pageHeight/2;

    ctx.beginPath();
    ctx.lineWidth="5";
    ctx.strokeStyle = node.selected ? 'yellow' : color || "black";
    ctx.rect(x,y, pageWidth, pageHeight);
    ctx.font="17px Georgia";
    ctx.fillText(title,x + 10,y + 20);
    ctx.stroke();

    for (var i=0; i<components.length; i++) {
      var comp = components[i];
      ctx.save();

      var img = new Image();
      img.src = comp.iconUrl;
      img.setAttribute('width', '12px');
      img.setAttribute('height', '12px');
      ctx.font='15px Georgia';
      ctx.drawImage(img, x+ 10, y + (i+2)*20 -5);
      ctx.beginPath();
      ctx.fillText(comp.name, x + 30, y + (i+2)*20 + 10);
      ctx.stroke();
      ctx.restore();

    }
    return {top:y, bottom:y + pageHeight, left:x, right:x + pageWidth};
  }
}

var clearOverlay = function() {
  document.getElementById('fade').style.display = 'none';
  document.getElementById('overlay').style.display = 'none';
}

var initializeLibrariesDropDown = function(target) {

  var books = window.exported.libraryDescriptor();

  var mainUl = document.createElement('ul');
  mainUl.classList.add('libraries-dropdown');
  for (var i = 0; i<books.length; i++) {
    var book = books[i];
    var bookli = document.createElement('li');
    var bookUl = document.createElement('ul');
    bookUl.classList.add('libraries-dropdown');
    bookli.innerHTML = book.name;
    mainUl.appendChild(bookli);
    for (var j = 0; j<book.pages.length; j++) {
      var page = book.pages[j];
      var pageli = document.createElement('li');
      bookUl.appendChild(pageli);
      pageli.innerHTML = page.name;
    }
    new Drop({
        target: bookli,
        content:bookUl,
        position:'right middle',
        openOn: 'hover',
    });
  }

  new Drop({
      target: target,
      content:mainUl,
      position:'bottom right',
      openOn: 'click',
  });
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

  var nodes = new vis.DataSet();
  var edges = new vis.DataSet();
  var formPages = pages.formPages;
  var sharedPages = pages.sharedPages;

  formPages.forEach(function(info) {
    var id = info.projectId + "_" + info.fileName;
    nodes.add({id: id,
      shape: 'custom',
      info: info,
      customDraw:getPageDrawer(info.name,  //TODO (evan): change the name of customDraw to drawFunction
        'green',
        info.components),
    });
    info.children.forEach(function(child)  {
      edges.add({
        from:id,
        to:child.projectId + "_" + child.fileName,
      });
    });
  });

  sharedPages.forEach(function(info) {
    var id = info.projectId + "_" + info.fileName;
    nodes.add({id: id,
      shape: 'custom',
      info: info,
      customDraw:getPageDrawer(info.name,
        'blue',
        info.components),
    });
    info.children.forEach(function(child) {
      edges.add({
        from:id,
        to:child.projectId + "_" + child.fileName,
      });
    });
  });


  // create a network
  var container = document.getElementById('vis_network_area');
  var data = {
    nodes: nodes,
    edges: edges,
  };

  var options = {
    dragNodes : false,
    stabilize : false,
    dataManipulation : {
      enabled: true,
      initiallyVisible: true,
      extraButtons : [{name:"Import From Library", initialize:initializeLibrariesDropDown}] ,

    },
    //hierarchicalLayout: {
    //  levelSeperation: 200,
    //  direction: 'LR',
    //  enabled:true,
    //},
    physics : {
      barnesHut : {
        enabled: false,
      },
      repulsion: {
        centralGravity:1,
        nodeDistance:120,
      },
    },
    edges: {
      style: 'arrow',
    },
    onConnect: function(data, connect) {
      var parent = nodes.get(data.from).info;
      var child = nodes.get(data.to).info;
      child = {
        projectId: child["projectId"],
        fileName: child["fileName"],
      }
      parent = {
        projectId: parent["projectId"],
        fileName: parent["fileName"],
      }
      if (window.exported.importNewPage(parent, child)) {
        connect(data);
      }
    }
  };
  var network = new vis.Network(container, data, options);
};

window.exported = window.exported || {};
window.exported.openSharedPagesOverlay = openSharedPagesOverlay;



  //var makeMenu = function(arr) {
  //  var ul = document.createElement("ul");
  //  arr.forEach(function(elm) {
  //    var li = document.createElement("li");
  //    li.innerHTML = elm.name,
  //    ul.appendChild(li)
  //  });
  //  ul.classList.add('libraries-dropdown');
  //  return ul;
  //};

  //var books = [{name:"option 1", pages:[{name:"page1"}, {name:"page2"}]},
  //    {name:"option 2", pages:[{name:"page2"},]},
  //    {name:"option 3", pages:[]},
  //    {name:"option 4", pages:[{name:"page3"}, {name:"page4"}, {name:"page5"}]},]

  //var mainMenu = makeMenu(books);
  //books.forEach(function (book) {
  //  var subMenu = makeMenu(book);
  //  new Drop({
  //    target:
  //    makeMenu(book.pages);
  //});
  //var drop = new Drop({
  //  target: target,
  //    content:makeMenu(books),
  //    position:'bottom right',
  //    openOn: 'click',
  //});
