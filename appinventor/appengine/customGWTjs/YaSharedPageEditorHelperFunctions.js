var setDemandedComponentsHeader = function(blocklyDom, components) {
  var demanded_components = (function getDemandedComponentsHeader() {
    var demanded_components, header;
    if (!(header = blocklyDom.querySelector('header'))) {
      header = document.createElement('header');
      demanded_components = document.createElement('demanded_components');
      header.appendChild(demanded_components);
      blocklyDom.appendChild(header);
      return demanded_components;
    } else {
      demanded_components = header.querySelector('demanded_components');
      if (demanded_components) {
        header.removeChild(demanded_components);
      }
      demanded_components = document.createElement('demanded_components');
      header.appendChild(demanded_components);
      return demanded_components;
    } 
  })();
  
  for (var i = 0; i < components.length ; i++) {
    components[i].setAttribute('position', i);
    demanded_components.appendChild(components[i]);
  }
  return blocklyDom;
}

var componentInfoToXml = function(name, type) {
  var component = document.createElement('component');
  component.setAttribute('name', name);
  component.setAttribute('type', type);
  return component;
}



window.exported.setDemandedComponentsHeader = setDemandedComponentsHeader;
window.exported.componentInfoToXml = componentInfoToXml;
