var textToDom = function(text) {
  //TODO (evan): this function is copy pasted form  blockly/.../xml.js, instead find a way to access blockly from outside the iframe
  var oParser = new DOMParser();
  var dom = oParser.parseFromString(text, 'text/xml');

  // The DOM should have one and only one top-level node, an XML tag.
  if (!dom || !dom.firstChild ||
      dom.firstChild.nodeName.toLowerCase() != 'xml' ||
      dom.firstChild !== dom.lastChild) {
    // Whatever we got back from the parser is not XML.
    console.log('invalid text ' + text);
    throw 'text is not a valid xml tree';
  }
  return dom.firstChild;
}

var domToText = function(dom) {
  //TODO (evan): this function is copy pasted form  blockly/.../xml.js, instead find a way to access blockly from outside the iframe
  var oSerializer = new XMLSerializer();
  return oSerializer.serializeToString(dom);
};

var blocklyXmlContainer = function() {
  //TODO (evan): allow changing the ya-version and language-version numbers
  return textToDom('<xml xmlns="http://www.w3.org/1999/xhtml"> <yacodeblocks ya-version="104" language-version="17"></yacodeblocks> </xml>"');
}

var getBlockDescendants = function(root) {
  if (root.tagName == "block") {
    return [root];
  } else if (root.children == []) {
    return [];
  } else {
    arr = [];
    for (var i=root.children.length - 1; i>=0; i--) {
      arr = arr.concat(getBlockDescendants(root.children[i]));
    }
    return arr;
  }
}

var labelBlocksWithId = function(blocks, projectId, fileId) {
  for (var i=0; i<blocks.length; i++) {
    blocks[i].setAttribute('projectId', projectId);
    blocks[i].setAttribute('fileId', fileId);
  }
}

var appendChildrenToParent = function(children, parent) {
  //iterate in reverse order because appending a child to the parent removes the child from the array, changing the size of the array
  for (var i=children.length - 1; i>=0; i--) { 
    parent.appendChild(children[i]);
  }
}

window.exported = window.exported || {};
window.exported.textToDom = textToDom;
window.exported.domToText = domToText;
window.exported.blocklyXmlContainer = blocklyXmlContainer;
window.exported.getBlockDescendants = getBlockDescendants;
window.exported.labelBlocksWithId = labelBlocksWithId;
window.exported.appendChildrenToParent = appendChildrenToParent;
