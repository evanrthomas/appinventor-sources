var network, nodes, edges,
  currentProjectId
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
    //TODO (evan): implement
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
      submenu = librariesUl.querySelector("#" + librariesUl.id + " > li.book-" + page.projectId + " ul.book-" + page.projectId);
  if (!bookli) {
    var bookli = document.createElement('li');
    bookli.innerHTML = page.projectName;
    var submenu = document.createElement('ul');
    librariesUl.appendChild(bookli);
    bookli.appendChild(submenu);
    bookli.classList.add('book-'+page.projectId);
    submenu.classList.add('libraries-dropdown');
    submenu.classList.add('book-'+page.projectId);
    new Drop({
      target: bookli,
        content:submenu,
        position:'right middle',
        openOn: 'hover',
    });
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
