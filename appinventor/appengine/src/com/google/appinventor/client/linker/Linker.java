package com.google.appinventor.client.linker;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.CountDownCallback;
import com.google.appinventor.client.helper.Utils;
import com.google.appinventor.shared.rpc.project.youngandroid.YASharedPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;

import java.util.*;

public class Linker {
  private static final Linker INSTANCE = new Linker();
  private static final Map<YACachedBlocksNode, Set<YACachedBlocksNode>> linkSet =
          new HashMap<YACachedBlocksNode, Set<YACachedBlocksNode>>();

  private Linker() {

  }

  public static Linker getInstance() {
    return INSTANCE;
  }

  public static void loadLinkedContent(YoungAndroidBlocksNode realNode, final Callback<String> onload) {
    YACachedBlocksNode node = YACachedBlocksNode.getCachedNode(realNode);
    Element dom = Utils.blocklyXmlContainer();
    Set<YACachedBlocksNode> visited = new HashSet<YACachedBlocksNode>();
    loadLinkedContent(node, dom, visited, 0, new Callback<Element>() {
      @Override
      public void call(Element element) {
        onload.call(Utils.domToText(element));
      }
    });
  }

  public static void newLink(YoungAndroidBlocksNode parentRealNode, YASharedPageBlocksNode childRealNode) {
    YACachedBlocksNode parent = YACachedBlocksNode.getCachedNode(parentRealNode);
    YACachedBlocksNode child = YACachedBlocksNode.getCachedNode(childRealNode);
    if (linkSet.get(parent) == null) {
      linkSet.put(parent, new HashSet<YACachedBlocksNode>());
    }
    linkSet.get(parent).add(child);
    YaCodePageEditor.getOrCreateEditor(parentRealNode).relinkBlocksArea(null);
    Ode.getInstance().getEditorManager().scheduleAutoSave(YaCodePageEditor.getOrCreateEditor(parentRealNode));
  }


  public static void removeLink(YoungAndroidBlocksNode parentNode, YoungAndroidBlocksNode childNode) {
    YACachedBlocksNode parent = YACachedBlocksNode.getCachedNode(parentNode);
    YACachedBlocksNode child = YACachedBlocksNode.getCachedNode(childNode);
    if (linkSet.get(parent) != null) {
      linkSet.get(parent).remove(child);
    }
    YaCodePageEditor.getOrCreateEditor(parentNode).relinkBlocksArea(null);
    Ode.getInstance().getEditorManager().scheduleAutoSave(YaCodePageEditor.getOrCreateEditor(parentNode));
  }

  public String unlinkContent(YoungAndroidBlocksNode realNode, String content) {
    YACachedBlocksNode cachedNode = YACachedBlocksNode.getCachedNode(realNode);
    JavaScriptObject filteredXml = filterOutImportedBlocks(Utils.textToDom(content));
    Collection<YACachedBlocksNode> children = linkSet.get(cachedNode);
    children = (children == null ? new HashSet<YACachedBlocksNode>() : children);
    Element xml = setChildrenHeader(filteredXml, makeChildrenXmlArray(children));
    return Utils.domToText(xml);
  }

  public static void loadChildren(final YoungAndroidBlocksNode node,
                                  final Callback<Collection<YoungAndroidBlocksNode>> onload) {
    loadChildren(YACachedBlocksNode.getCachedNode(node), new Callback<Collection<YACachedBlocksNode>>() {
      @Override
      public void call(Collection<YACachedBlocksNode> childrenAsCachedNodes) {
        Collection<YoungAndroidBlocksNode> childrenAsBlocksNode = new ArrayList<YoungAndroidBlocksNode>();
        for (YACachedBlocksNode child: childrenAsCachedNodes) {
          childrenAsBlocksNode.add(child.getRealNode());
        }
        onload.call(childrenAsBlocksNode);
      }
    });
  }

  private static void loadLinkedContent(final YACachedBlocksNode node, final Element finalDom, final Set<YACachedBlocksNode> visited,
                               final int depth, final Callback<Element> onLinked) {

    if (visited.contains(node)) return;
    visited.add(node);

    loadChildren(node, new Callback<Collection<YACachedBlocksNode>>() {
      @Override
      public void call(Collection<YACachedBlocksNode> children) {
        final CountDownCallback thisLinked = new CountDownCallback(children.size() + 1, onLinked); //TODO (evan): this callback  spaghetti is messy. Re design

        node.load(new Callback<String>() {
          @Override
          public void call(String content) {
            Element thisUnlinkedDom = Utils.textToDom(content);
            JsArray<Element> blocks = content.equals("") ? (JsArray<Element>) JavaScriptObject.createArray().cast() :
                    getTopLevelBlocks(thisUnlinkedDom);
            labelBlocks(blocks, depth);
            addAllBlocks(finalDom, blocks);
            thisLinked.call(finalDom);
          }
        });

        for (YACachedBlocksNode child : children) {
          loadLinkedContent(child, finalDom, visited, depth + 1, thisLinked);
        }
      }
    });
  }

  private static void loadChildren(final YACachedBlocksNode node,
                                   final Callback<Collection<YACachedBlocksNode>> onload) {
    if (linkSet.containsKey(node)) {
      onload.call(linkSet.get(node));
      return;
    }

    node.load(new Callback<String>() {
      @Override
      public void call(String s) {
        if (linkSet.get(node) == null) {
          linkSet.put(node, new HashSet<YACachedBlocksNode>());
        }

        Element header = Utils.textToDom(s);
        JsArray<Element> childrenXml = getChildrenFromHeader(header);
        Element childXml;
        for (int i = 0; i < childrenXml.length(); i++) {
          childXml = childrenXml.get(i);
          String projectIdString = childXml.getAttribute("projectId");
          if (projectIdString.length() == 0) projectIdString = childXml.getAttribute("projectid");  //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
          long projectId = Long.parseLong(projectIdString);
          String fileId = childXml.getAttribute("fileId");
          if (fileId.length() == 0) fileId = childXml.getAttribute("fileid"); //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
          YACachedBlocksNode child = YACachedBlocksNode.getCachedNode(projectId, fileId);
          linkSet.get(node).add(child); //linkSet.get(node) is a Set, if it already has this child, the new child won't be added
        }
        onload.call(linkSet.get(node));
      }
    });
  }

  private static void labelBlocks(JsArray<Element> blocks, int depth) {
    //TODO (evan): when you add components, label each function with the version of it (ie what components it's using)
    for (int i = 0; i<blocks.length(); i++) {
      blocks.get(i).setAttribute("depth", depth + "");
    }
  }

  private static void addAllBlocks(Element dom, JsArray<Element> blocks) {
    //traverse in reverse order because the size of the array elements delete themselves as you add them to dom
    for (int i = blocks.length() - 1; i>=0; i--) {
      Element block = blocks.get(i);
      dom.appendChild(block);
    }
  }

  private static JsArray<Element> makeChildrenXmlArray(Collection<YACachedBlocksNode> children) {
    JsArray<Element> childrenXmlArr = JavaScriptObject.createArray().cast();
    for (YACachedBlocksNode child: children) {
      Element xmlchild = createElement("child"); //hack because I can't figure out to make an Element
      xmlchild.setAttribute("projectId", child.getProjectId()+"");
      xmlchild.setAttribute("fileId", child.getFileId());
      childrenXmlArr.push(xmlchild);
    }
    return childrenXmlArr;
  }

  private static native JsArray<Element> getChildrenFromHeader(JavaScriptObject xml) /*-{
    return xml.querySelectorAll('header > children > child');
  }-*/;

  private static native Element createElement(String name) /*-{
    return document.createElement(name);
  }-*/;

  private static native Element setChildrenHeader(JavaScriptObject xml, JsArray<Element> children) /*-{
    return $wnd.exported.setChildrenHeader(xml, children);
  }-*/;

  private native JavaScriptObject filterOutImportedBlocks(Element e) /*-{
    return $wnd.exported.filterOutImportedBlocks(e);
  }-*/;


  private static native JsArray<Element> getTopLevelBlocks(JavaScriptObject root) /*-{
    return $wnd.exported.getTopLevelBlocks(root);
  }-*/;
}
