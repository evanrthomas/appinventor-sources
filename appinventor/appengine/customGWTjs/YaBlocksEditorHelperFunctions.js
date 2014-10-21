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
    throw 'text is not a valid xml tree';
  }
  return dom.firstChild;
}

var domToText = function(dom) {
  //TODO (evan): this function is copy pasted form  blockly/.../xml.js, use blocklies instead
  var oSerializer = new XMLSerializer();
  return oSerializer.serializeToString(dom);
};

var blocklyXmlContainer = function() {
  //TODO (evan): allow changing the ya-version and language-version numbers
  return textToDom(
      '<xml xmlns="http://www.w3.org/1999/xhtml"> <yacodeblocks ya-version="104" language-version="17"></yacodeblocks> </xml>');
}

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

var attachAttributes = function(blocks, json) {
  for (var i=0; i<blocks.length; i++) {
    for (key in json) {
      blocks[i].setAttribute(key, json[key]);
    }
  }
}

var appendChildrenToParent = function(children, parent) {
  //iterate in reverse order because appending a child to the parent removes the child from the array, changing the size of the array
  for (var i=children.length - 1; i>=0; i--) { 
    parent.appendChild(children[i]);
  }
}

var filterOutImportedBlocks = function(text) {
  var xml = textToDom(text);
  for (var i=xml.children.length -1; i>=0; i--) {
    var child = xml.children[i];
    if (child.getAttribute('isimported') && child.getAttribute('isimported').toLowerCase() == 'true') {
      xml.removeChild(child);
    } 
  }
  
  //the function wipe wipes the attribute isimported from all blocks recursivley.
  //it shouldn't be necessary after you've already removed all top-level blocks that have isimported=true
  //there shouldn't be anything else that has isimported set, but just in case
  (function wipe(xml) {
    xml.removeAttribute('isimported');
    for (var i=0; i<xml.children.length; i++) {
      wipe(xml.children[i]);
    }
  })(xml);
  return domToText(xml);
}

window.exported = window.exported || {};
window.exported.textToDom = textToDom;
window.exported.domToText = domToText;
window.exported.blocklyXmlContainer = blocklyXmlContainer;
window.exported.getTopLevelBlocks = getTopLevelBlocks;
window.exported.attachAttributes = attachAttributes;
window.exported.appendChildrenToParent = appendChildrenToParent;
window.exported.filterOutImportedBlocks = filterOutImportedBlocks;
