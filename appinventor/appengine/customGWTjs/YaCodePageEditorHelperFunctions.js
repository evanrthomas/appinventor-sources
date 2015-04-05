var textToDom = function(text) {
  //TODO (evan): this function is copy pasted form  blockly/.../xml.js, use blocklies instead
  var oParser = new DOMParser();
  var dom = oParser.parseFromString(text, 'text/xml');

  // The DOM should have one and only one top-level node, an XML tag.
  if (!dom || !dom.firstChild ||
      dom.firstChild.nodeName.toLowerCase() != 'xml' ||
      dom.firstChild !== dom.lastChild) {
    // Whatever we got back from the parser is not XML.
    console.log('invalid text ' + text);
    throw 'my textToDom text is not a valid xml tree';
  }
  return dom.firstChild;
}

var domToText = function(dom) {
  //TODO (evan): this function is copy pasted form  blockly/.../xml.js, use blocklies instead
  var oSerializer = new XMLSerializer();
  return oSerializer.serializeToString(dom);
};

var getTopLevelBlocks = function(root) {
  if (root.tagName == "block") {
    return [root];
  } else if (root.children == []) {
    return [];
  } else {
    arr = [];
    for (var i=root.children.length - 1; i>=0; i--) {
      arr = arr.concat(getTopLevelBlocks(root.children[i]));
    }
    return arr;
  }
}

var appendChildrenToParent = function(children, parent) {
  //iterate in reverse order because appending a child to the parent removes the child from the array, changing the size of the array
  for (var i=children.length - 1; i>=0; i--) {
    parent.appendChild(children[i]);
  }
}

var filterOutImportedBlocks = function(xml) {
  for (var i=xml.children.length -1; i>=0; i--) {
    var child = xml.children[i];
    if (child.getAttribute('depth') != 0) {
      xml.removeChild(child);
    }
  }

  //the function wipe wipes the attribute depth from all blocks recursivley.
  //it shouldn't be necessary after you've already removed all top-level blocks that have depth != 0
  //there shouldn't be anything else that has depth set, but just in case
  (function wipe(xml) {
    xml.removeAttribute('depth');
    for (var i=0; i<xml.children.length; i++) {
      wipe(xml.children[i]);
    }
  })(xml);
  return xml;
}

var setChildrenHeader = function(dom, children) {
  var container = (function getChildrenContainer() {
    var container, header;
    if (!(header = dom.querySelector('header'))) {
      header = document.createElement('header');
      dom.appendChild(header);
    }
    container = header.querySelector('children');
    if (container) {
      header.removeChild(container);
    }
    container = document.createElement('children');
    header.appendChild(container);
    return container;
  })();

  for (var i = 0; i < children.length ; i++) {
    container.appendChild(children[i]);
  }
  return dom;
}

window.exported = window.exported || {};
window.exported.textToDom = textToDom;
window.exported.domToText = domToText;
window.exported.getTopLevelBlocks = getTopLevelBlocks;
window.exported.appendChildrenToParent = appendChildrenToParent;
window.exported.filterOutImportedBlocks = filterOutImportedBlocks;
window.exported.setChildrenHeader = setChildrenHeader;
