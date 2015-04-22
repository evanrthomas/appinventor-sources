var network, nodes, edges,
  currentProjectId, submenus,
  firstTimeOpened = true;


var networkOptions = {
  //dragNodes : false,
  stabilize : false,
  dataManipulation : {
    enabled: true,
    initiallyVisible: true,
    extraButtons : [{
      name:"Import From Library",
      initialize:initializeLibrariesDropDown,
    }],
  },
  physics : {
    barnesHut : {
    //enabled: false,
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
    //TODO (evan): both here and in importNewPage, check to make sure isn't importing itself
    window.exported.newLink(parent, child,
        function onSuccess() {
          connect(data);
        }, function onFail(message) {
          alert(message);
        });
  },

  onAdd: function(data, callback) {
    //data and callback are provided by vis js. We don't use them here
    window.exported.newSharedPage(function onSuccess(newPageInfo) {
      nodes.add(nodeOptions(newPageInfo,
            currentProjectId == newPageInfo.projectId));
    });
  },
  onDelete: function(data, callback) {
    data.edges.forEach(function(edgeid) {
      var edge = edges.get(edgeid);
      var from = nodes.get(edge.from).info;
      var to = nodes.get(edge.to).info;
      window.exported.removeLink(from, to, (function(edgeid) { //wierd closure so
        return function() {
          edges.remove(edgeid);
        }
      })(edgeid));
    });

    var nodeInfos = data.nodes.map(function(nodeid) {
      return nodes.get(nodeid).info;
    });
    nodeInfos.forEach(function(nodeinfo) {
      window.exported.removeNode(nodeinfo, function onSuccess() {
        nodes.remove(nodeinfo.id);
      })
    });
  }
}

function openModal() {
  if (firstTimeOpened) {
    initializeOverlay();
    //initializeLibrariesDropdown is called by vis.js when you call new vis.Network
  }
  firstTimeOpened = false;

  currentProjectId = window.exported.getCurrentProjectId();
  clearNetwork();
  document.getElementById('fade').style.display = 'block';
  document.getElementById('overlay').style.display = 'block';

  var container = document.getElementById('vis_network_area');
  network = new vis.Network(container, {nodes:nodes, edges:edges}, networkOptions); //not really sure why this line is necessary???
};

function renderProjectPage(page) {
  nodes.add(nodeOptions(page, true));
}

function renderLibraryPage(page) {
  var bookli = librariesUl.querySelector("#" + librariesUl.id + " > li.book-" + page.projectId),
      submenu = submenus[page.projectId];
  if (!bookli) {
    bookli = document.createElement('li');
    submenu = document.createElement('ul');
    librariesUl.appendChild(bookli);
    new Drop({
      target: bookli,
        content:submenu,
        position:'right middle',
        openOn: 'hover',
    });

    bookli.innerHTML = page.projectName;
    submenus[page.projectId] = submenu;
    bookli.classList.add('book-'+page.projectId);
    submenu.classList.add('libraries-dropdown');
    submenu.classList.add('book-'+page.projectId);
  }

  var pageli = document.createElement('li');
  pageli.onclick = function() {
    nodes.add(nodeOptions(page, page.projectId == currentProjectId));
  };
  submenu.appendChild(pageli);
  pageli.innerHTML = page.name;
}

function renderLink(link) {
  edges.add({
    from:getId(link.parent),
    to:getId(link.child),
  });
}

function initializeLibrariesDropDown(target) {
  librariesUl = document.createElement('ul');
  librariesUl.classList.add('libraries-dropdown');
  librariesUl.id = "libraries-dropdown";
  new Drop({
      target: target,
      content:librariesUl,
      position:'bottom right',
      openOn: 'click',
  });
}

function initializeOverlay() {
  document.getElementById('fade').addEventListener("click", function() {
    document.getElementById('fade').style.display = 'none';
    document.getElementById('overlay').style.display = 'none';
  });
  window.onkeyup = function(e) { //TODO (evan): I think this will overwrite any other onkeyup functions, make it add instead of replace
    if (e.keyCode == 27) { //escape key code
      clearOverlay();
    }
  }

  var container = document.getElementById('vis_network_area');
  nodes = new vis.DataSet();
  edges = new vis.DataSet();
  var dataset = {
    nodes: nodes,
    edges: edges,
  }
  network = new vis.Network(container, dataset, networkOptions);
}

function clearNetwork() {
  console.log("clear");
  nodes.clear();
  edges.clear();
  librariesUl.innerHTML = "";
  submenus = {};
}

function getId(pageinfo) {
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

window.exported = window.exported || {};
window.exported.openModal = openModal;
window.exported.renderProjectPage = renderProjectPage;
window.exported.renderLibraryPage = renderLibraryPage;
window.exported.renderLink = renderLink;
