(function() {
var network, nodes, edges,
  currentPage,
  firstTimeOpened = true;

//for debugging
window.network = network;
window.nodes = nodes;
window.edges = edges;
window.currentPage = currentPage;
window.firstTimeOpened = firstTimeOpened;

var networkOptions = {
  //dragNodes : false,
  //stabilize : false,
  dataManipulation : {
    enabled: true,
    initiallyVisible: true,
    extraButtons : [{
      name:"Import From Library",
      initialize:initializeLibrariesDropDown
    }],
  },
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
    window.exported.importNewPage(parent, child,
        function onSuccess() {
          connect(data);
        }, function onFail(message) {
          alert(message);
        });
  }
}

function clearOverlay() {
  document.getElementById('fade').style.display = 'none';
  document.getElementById('overlay').style.display = 'none';
}

function initializeLibrariesDropDown(target) {
  var mainUl = document.createElement('ul');
  mainUl.classList.add('libraries-dropdown');

  new Drop({
    target: target,
      content:mainUl,
      position:'bottom right',
      openOn: 'click',
  });

  window.exported.getEachBookWhenLoaded(function(book) {
    var bookli = document.createElement('li');
    var submenu = document.createElement('ul');
    submenu.classList.add('libraries-dropdown');
    bookli.innerHTML = book.name;
    mainUl.appendChild(bookli);
    for (var j = 0; j<book.pages.length; j++) {
      var page = book.pages[j];
      var pageli = document.createElement('li');
      pageli.onclick = (function(page) {
        return function() {
          nodes.add(nodeOptions(page, page.projectId == currentPage.projectId));
        } //closure so page doesn't get mutated before onclick is called
      })(page);
      submenu.appendChild(pageli);
      pageli.innerHTML = page.name;
    }
    new Drop({
      target: bookli,
        content:submenu,
        position:'right middle',
        openOn: 'hover',
    });

  });
}

function initializeOverlay() {
  document.getElementById('new_shared_page_btn').onclick =
    window.exported.newSharedPage;
  document.getElementById('fade').addEventListener("click", clearOverlay);
  window.onkeyup = function(e) { //TODO (evan): I think this will overwrite any other onkeyup functions, make it add instead of replace
    if (e.keyCode == 27) { //escape key code
      clearOverlay();
    }
  }
}

function clearNetwork() {
  if (network) network.destroy();
  if (nodes) nodes.clear();
  if (edges) edges.clear();
}

var getId = function(pageinfo) {
  return pageinfo.projectId + "_" + pageinfo.fileName;
}

function nodeOptions(pageinfo, isFromCurrentProject) {
  var projectPrefix = isFromCurrentProject ?
    "" : pageinfo.projectName + "::";
  return {
      id: getId(pageinfo),
      shape: 'box',
      info: pageinfo,
      label:projectPrefix + pageinfo.name,
      color:pageinfo.type == "sharedPage" ? '#C3FAF5' : '#88F77E',
  }
}

function newDataSet() {
  var pages = window.exported.getProjectPages();
  nodes = new vis.DataSet();
  edges = new vis.DataSet();
  currentPage = pages.currentPage;
  var formPages = pages.formPages;
  var sharedPages = pages.sharedPages;

  formPages.forEach(function(thisFormPage) {
    nodes.add(nodeOptions(thisFormPage,
        currentPage.projectId == thisFormPage.projectId));
    thisFormPage.children.forEach(function(child)  {
      edges.add({
        from:getId(thisFormPage),
        to:getId(child),
      });
    });
  });

  sharedPages.forEach(function(thisSharedPage) {
    nodes.add(nodeOptions(thisSharedPage,
      currentPage.projectId == thisSharedPage.projectId));
    thisSharedPage.children.forEach(function(child) {
      edges.add({
        from:getId(thisSharedPage),
        to:getId(child),
      });
    });
  });

  return {
    nodes: nodes,
    edges: edges,
  };
}

function openSharedPagesOverlay() {
  if (firstTimeOpened) {
    initializeOverlay();
  }
  firstTimeOpened = false;

  document.getElementById('fade').style.display = 'block';
  document.getElementById('overlay').style.display = 'block';

  clearNetwork();
  var container = document.getElementById('vis_network_area');
  var dataset = newDataSet();
  network = new vis.Network(container, dataset, networkOptions);
};

window.exported = window.exported || {};
window.exported.openSharedPagesOverlay = openSharedPagesOverlay;
})();
