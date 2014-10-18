/**
 * @license
 * Visual Blocks Editor
 *
 * Copyright 2012 Google Inc.
 * https://blockly.googlecode.com/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * @fileoverview XML reader and writer.
 * @author fraser@google.com (Neil Fraser)
 */
'use strict';

goog.provide('Blockly.Xml');

goog.require('Blockly.Instrument'); // lyn's instrumentation code

// TODO(scr): Fix circular dependencies
// goog.require('Blockly.Block');


/**
 * Encode a block tree as XML.
 * @param {!Object} workspace The SVG workspace.
 * @return {!Element} XML document.
 */
Blockly.Xml.workspaceToDom = function(workspace) {
  var width;  // Not used in LTR.
  if (Blockly.RTL) {
    width = workspace.getMetrics().viewWidth;
  }
  var xml = goog.dom.createDom('xml');
  var blocks = workspace.getTopBlocks(true);
  for (var i = 0, block; block = blocks[i]; i++) {
    var element = Blockly.Xml.blockToDom_(block);
    var xy = block.getRelativeToSurfaceXY();
    element.setAttribute('x', Blockly.RTL ? width - xy.x : xy.x);
    element.setAttribute('y', xy.y);
    xml.appendChild(element);
  }
  return xml;
};

/**
 * Encode a block subtree as XML.
 * @param {!Blockly.Block} block The root block to encode.
 * @return {!Element} Tree of XML elements.
 * @private
 */
Blockly.Xml.blockToDom_ = function(block) {
  var element = goog.dom.createDom('block');
  element.setAttribute('type', block.type);
  element.setAttribute('id', block.id);
  if (block.mutationToDom) {
    // Custom data for an advanced block.
    var mutation = block.mutationToDom();
    if (mutation) {
      element.appendChild(mutation);
    }
  }
  function fieldToDom(field) {
    if (field.name && field.EDITABLE) {
      var container = goog.dom.createDom('field', null, field.getValue());
      container.setAttribute('name', field.name);
      element.appendChild(container);
    }
  }
  for (var x = 0, input; input = block.inputList[x]; x++) {
    for (var y = 0, field; field = input.fieldRow[y]; y++) {
      fieldToDom(field);
    }
  }

  if (block.comment) {
    var commentElement = goog.dom.createDom('comment', null,
        block.comment.getText());
    commentElement.setAttribute('pinned', block.comment.isVisible());
    var hw = block.comment.getBubbleSize();
    commentElement.setAttribute('h', hw.height);
    commentElement.setAttribute('w', hw.width);
    element.appendChild(commentElement);
  }

  var hasValues = false;
  for (var i = 0, input; input = block.inputList[i]; i++) {
    var container;
    var empty = true;
    if (input.type == Blockly.DUMMY_INPUT) {
      continue;
    } else {
      var childBlock = input.connection.targetBlock();
      if (input.type == Blockly.INPUT_VALUE) {
        container = goog.dom.createDom('value');
        hasValues = true;
      } else if (input.type == Blockly.NEXT_STATEMENT) {
        container = goog.dom.createDom('statement');
      }
      if (childBlock) {
        container.appendChild(Blockly.Xml.blockToDom_(childBlock));
        empty = false;
      }
    }
    container.setAttribute('name', input.name);
    if (!empty) {
      element.appendChild(container);
    }
  }
  if (hasValues) {
    element.setAttribute('inline', block.inputsInline);
  }
  if (block.isCollapsed()) {
    element.setAttribute('collapsed', true);
  }
  if (block.disabled) {
    element.setAttribute('disabled', true);
  }
  if (!block.isDeletable()) {
    element.setAttribute('deletable', false);
  }
  if (!block.isMovable()) {
    element.setAttribute('movable', false);
  }
  if (!block.isEditable()) {
    element.setAttribute('editable', false);
  }

  var nextBlock = block.getNextBlock();
  if (nextBlock) {
    var container = goog.dom.createDom('next', null,
        Blockly.Xml.blockToDom_(nextBlock));
    element.appendChild(container);
  }

  return element;
};

/**
 * Converts a DOM structure into plain text.
 * Currently the text format is fairly ugly: all one line with no whitespace.
 * @param {!Element} dom A tree of XML elements.
 * @return {string} Text representation.
 */
Blockly.Xml.domToText = function(dom) {
  var oSerializer = new XMLSerializer();
  return oSerializer.serializeToString(dom);
};

/**
 * Converts a DOM structure into properly indented text.
 * @param {!Element} dom A tree of XML elements.
 * @return {string} Text representation.
 */
Blockly.Xml.domToPrettyText = function(dom) {
  // This function is not guaranteed to be correct for all XML.
  // But it handles the XML that Blockly generates.
  var blob = Blockly.Xml.domToText(dom);
  // Place every open and close tag on its own line.
  var lines = blob.split('<');
  // Indent every line.
  var indent = '';
  for (var x = 1; x < lines.length; x++) {
    var line = lines[x];
    if (line[0] == '/') {
      indent = indent.substring(2);
    }
    lines[x] = indent + '<' + line;
    if (line[0] != '/' && line.slice(-2) != '/>') {
      indent += '  ';
    }
  }
  // Pull simple tags back together.
  // E.g. <foo></foo>
  var text = lines.join('\n');
  text = text.replace(/(<(\w+)\b[^>]*>[^\n]*)\n *<\/\2>/g, '$1</$2>');
  // Trim leading blank line.
  return text.replace(/^\n/, '');
};

/**
 * Converts plain text into a DOM structure.
 * Throws an error if XML doesn't parse.
 * @param {string} text Text representation.
 * @return {!Element} A tree of XML elements.
 */
Blockly.Xml.textToDom = function(text) {
  var oParser = new DOMParser();
  var dom = oParser.parseFromString(text, 'text/xml');
  // The DOM should have one and only one top-level node, an XML tag.
  if (!dom || !dom.firstChild ||
      dom.firstChild.nodeName.toLowerCase() != 'xml' ||
      dom.firstChild !== dom.lastChild) {
    // Whatever we got back from the parser is not XML.
    throw 'Blockly.Xml.textToDom did not obtain a valid XML tree.';
  }
  return dom.firstChild;
};

/**
 * Decode an XML DOM and create blocks on the workspace.
 * @param {!Blockly.Workspace} workspace The SVG workspace.
 * @param {!Element} xml XML DOM.
 */
Blockly.Xml.domToWorkspace = function(workspace, xml) {
  Blockly.Instrument.timer (
      function () {
        var width;  // Not used in LTR.
        if (Blockly.RTL) {
          width = workspace.getMetrics().viewWidth;
        }
        for (var x = 0, xmlChild; xmlChild = xml.childNodes[x]; x++) {
          if (xmlChild.nodeName.toLowerCase() == 'block') {
            var block = Blockly.Xml.domToBlock(workspace, xmlChild);
            var blockX = parseInt(xmlChild.getAttribute('x'), 10);
            var blockY = parseInt(xmlChild.getAttribute('y'), 10);
            if (!isNaN(blockX) && !isNaN(blockY)) {
              block.moveBy(Blockly.RTL ? width - blockX : blockX, blockY);
            }
          }
        }
      },
      function (result, timeDiff) {
        Blockly.Instrument.stats.domToWorkspaceCalls++;
        Blockly.Instrument.stats.domToWorkspaceTime = timeDiff;
      }
  );
};

/**
 * Wrapper for domToBlockInner that renders blocks.
 * Decode an XML block tag and create a block (and possibly sub blocks) on the
 * workspace.
 * @param {!Blockly.Workspace} workspace The workspace.
 * @param {!Element} xmlBlock XML block element.
 * @param {boolean=} opt_reuseBlock Optional arg indicating whether to
 *     reinitialize an existing block.
 * @return {!Blockly.Block} The root block created.
 * @private
 */
Blockly.Xml.domToBlock = function(workspace, xmlBlock, opt_reuseBlock) {
  var block =  Blockly.Xml.domToBlockInner(workspace, xmlBlock, opt_reuseBlock);
  if (Blockly.Instrument.useRenderDown) {
    block.renderDown();
  }
  // [lyn, 07/03/2014] Special case to handle renaming of event parameters in i8n
  if (block && block.type == "component_event") {
    // Create a dictionary mapping default event parameter names appearing in body
    //   to their possibly translated names in some source language
    var eventParamDict = Blockly.LexicalVariable.eventParameterDict(block);
    var sourceEventParams = []; // Event parameter names in source language
    var targetEventParams = []; // Event parameter names in target language
    for (var key in eventParamDict) {
      var sourceEventParam = eventParamDict[key];
      var targetEventParam = window.parent.BlocklyPanel_getLocalizedParameterName(key);
      if (sourceEventParam != targetEventParam) { // Only add to translation if they're different
        sourceEventParams.push(sourceEventParam);
        targetEventParams.push(targetEventParam);
      }
    }
    if (sourceEventParams.length > 0) { // Do we need to translated some source event parameters?
      var childBlocks = block.getChildren(); // should be at most one body block
      for (var j= 0, childBlock; childBlock = childBlocks[j]; j++) {
        var freeSubstitution = new Blockly.Substitution(sourceEventParams, targetEventParams);
        // renameFree does the translation.
        Blockly.LexicalVariable.renameFree(childBlock, freeSubstitution);
      }
    }
  }
  return block;
}

/**
 * Version of domToBlock that does not render blocks.
 * Decode an XML block tag and create a block (and possibly sub blocks) on the
 * workspace.
 * @param {!Blockly.Workspace} workspace The workspace.
 * @param {!Element} xmlBlock XML block element.
 * @param {boolean=} opt_reuseBlock Optional arg indicating whether to
 *     reinitialize an existing block.
 * @return {!Blockly.Block} The root block created.
 * @private
 */
Blockly.Xml.domToBlockInner = function(workspace, xmlBlock, opt_reuseBlock) {
  Blockly.Instrument.stats.domToBlockInnerCalls++;
  var block = null;
  var prototypeName = xmlBlock.getAttribute('type');
  if (!prototypeName) {
    throw 'Block type unspecified: \n' + xmlBlock.outerHTML;
  }
  var id = xmlBlock.getAttribute('id');
  if (opt_reuseBlock && id) {
    block = Blockly.Block.getById(id, workspace);
    // TODO: The following is for debugging.  It should never actually happen.
    if (!block) {
      throw 'Couldn\'t get Block with id: ' + id;
    }
    var parentBlock = block.getParent();
    // If we've already filled this block then we will dispose of it and then
    // re-fill it.
    if (block.workspace) {
      block.dispose(true, false, true);
    }
    block.fill(workspace, prototypeName);
    block.parent_ = parentBlock;
  } else {
    block = Blockly.Block.obtain(workspace, prototypeName);
  }
  if (!block.svg_) {
    block.initSvg();
  }

  var inline = xmlBlock.getAttribute('inline');
  if (inline) {
    block.setInputsInline(inline == 'true');
  }
  var disabled = xmlBlock.getAttribute('disabled');
  if (disabled) {
    block.setDisabled(disabled == 'true');
  }
  var deletable = xmlBlock.getAttribute('deletable');
  if (deletable) {
    block.setDeletable(deletable == 'true');
  }
  var movable = xmlBlock.getAttribute('movable');
  if (movable) {
    block.setMovable(movable == 'true');
  }
  var editable = xmlBlock.getAttribute('editable');
  if (editable) {
    block.setEditable(editable == 'true');
  }

  var blockChild = null;
  for (var x = 0, xmlChild; xmlChild = xmlBlock.childNodes[x]; x++) {
    if (xmlChild.nodeType == 3 && xmlChild.data.match(/^\s*$/)) {
      // Extra whitespace between tags does not concern us.
      continue;
    }
    var input;

    // Find the first 'real' grandchild node (that isn't whitespace).
    var firstRealGrandchild = null;
    for (var y = 0, grandchildNode; grandchildNode = xmlChild.childNodes[y];
         y++) {
      if (grandchildNode.nodeType != 3 || !grandchildNode.data.match(/^\s*$/)) {
        firstRealGrandchild = grandchildNode;
      }
    }

    var name = xmlChild.getAttribute('name');
    switch (xmlChild.nodeName.toLowerCase()) {
      case 'mutation':
        // Custom data for an advanced block.
        if (block.domToMutation) {
          block.domToMutation(xmlChild);
        }
        break;
      case 'comment':
        block.setCommentText(xmlChild.textContent);
        var visible = xmlChild.getAttribute('pinned');
        if (visible) {
          // Give the renderer a millisecond to render and position the block
          // before positioning the comment bubble.
          setTimeout(function() {
            block.comment.setVisible(visible == 'true');
          }, 1);
        }
        var bubbleW = parseInt(xmlChild.getAttribute('w'), 10);
        var bubbleH = parseInt(xmlChild.getAttribute('h'), 10);
        if (!isNaN(bubbleW) && !isNaN(bubbleH)) {
          block.comment.setBubbleSize(bubbleW, bubbleH);
        }
        break;
      case 'title':
        // Titles were renamed to field in December 2013.
        // Fall through.
      case 'field':
        block.setFieldValue(xmlChild.textContent, name);
        break;
      case 'value':
      case 'statement':
        input = block.getInput(name);
        if (!input) {
          throw 'Input ' + name + ' does not exist in block ' + prototypeName;
        }
        if (firstRealGrandchild &&
            firstRealGrandchild.nodeName.toLowerCase() == 'block') {
          blockChild = Blockly.Xml.domToBlockInner(workspace, firstRealGrandchild,
              opt_reuseBlock);
          if (blockChild.outputConnection) {
            input.connection.connect(blockChild.outputConnection);
          } else if (blockChild.previousConnection) {
            input.connection.connect(blockChild.previousConnection);
          } else {
            throw 'Child block does not have output or previous statement.';
          }
        }
        break;
      case 'next':
        if (firstRealGrandchild &&
            firstRealGrandchild.nodeName.toLowerCase() == 'block') {
          if (!block.nextConnection) {
            throw 'Next statement does not exist.';
          } else if (block.nextConnection.targetConnection) {
            // This could happen if there is more than one XML 'next' tag.
            throw 'Next statement is already connected.';
          }
          blockChild = Blockly.Xml.domToBlockInner(workspace, firstRealGrandchild,
              opt_reuseBlock);
          if (!blockChild.previousConnection) {
            throw 'Next block does not have previous statement.';
          }
          block.nextConnection.connect(blockChild.previousConnection);
        }
        break;
      default:
        // Unknown tag; ignore.  Same principle as HTML parsers.
    }
  }

  // [lyn, 10/25/13] collapsing and friends need to be done *after* connections are made to sublocks.
  // Otherwise, the subblocks won't be properly processed by block.setCollapsed and friends.
  var inline = xmlBlock.getAttribute('inline');
  if (inline) {
    block.setInputsInline(inline == 'true');
  }
  var disabled = xmlBlock.getAttribute('disabled');
  if (disabled) {
    block.setDisabled(disabled == 'true');
  }
  var deletable = xmlBlock.getAttribute('deletable');
  if (deletable) {
    block.setDeletable(deletable == 'true');
  }
  var movable = xmlBlock.getAttribute('movable');
  if (movable) {
    block.setMovable(movable == 'true');
  }
  var editable = xmlBlock.getAttribute('editable');
  if (editable) {
    block.setEditable(editable == 'true');
  }

  if (! Blockly.Instrument.useRenderDown) {
    // Neil's original rendering code
    var next = block.getNextBlock();
    if (next) {
      // Next block in a stack needs to square off its corners.
      // Rendering a child will render its parent.
      next.render();
    } else {
      block.render();
    }
  }
  var collapsed = xmlBlock.getAttribute('collapsed');
  if (collapsed) {
    block.setCollapsed(collapsed == 'true');
  }
  return block;
};

/**
 * Remove any 'next' block (statements in a stack).
 * @param {!Element} xmlBlock XML block element.
 */
Blockly.Xml.deleteNext = function(xmlBlock) {
  for (var x = 0, child; child = xmlBlock.childNodes[x]; x++) {
    if (child.nodeName.toLowerCase() == 'next') {
      xmlBlock.removeChild(child);
      break;
    }
  }
};

// Export symbols that would otherwise be renamed by Closure compiler.
if (!window['Blockly']) {
  window['Blockly'] = {};
}
if (!window['Blockly']['Xml']) {
  window['Blockly']['Xml'] = {};
}
window['Blockly']['Xml']['domToText'] = Blockly.Xml.domToText;
window['Blockly']['Xml']['domToWorkspace'] = Blockly.Xml.domToWorkspace;
window['Blockly']['Xml']['textToDom'] = Blockly.Xml.textToDom;
window['Blockly']['Xml']['workspaceToDom'] = Blockly.Xml.workspaceToDom;
