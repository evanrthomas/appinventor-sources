package com.google.appinventor.client.linker;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.explorer.project.Project;
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

  public static void loadLinkedContent(final YoungAndroidBlocksNode realNode, final Callback<String> onload) {
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

  public static void removeAllLinksForNode(final YoungAndroidBlocksNode node) {
    //TODO (evan): should remove links form the linkSet of the form (parent -> node)
    //loadChildren(parentNode, new Callback<Collection<YoungAndroidBlocksNode>>() {
    //  @Override
    //  public void call(Collection<YoungAndroidBlocksNode> youngAndroidBlocksNodes) {
    //    for (YoungAndroidBlocksNode childNode: youngAndroidBlocksNodes) {
    //      removeLink(parentNode, childNode);
    //    }
    //  }
    //});
//    Ode.getInstance().getEditorManager().scheduleAutoSave(YaCodePageEditor.getOrCreateEditor(parentNode));
  }

  public String unlinkContent(YoungAndroidBlocksNode realNode, String content) {
    YACachedBlocksNode cachedNode = YACachedBlocksNode.getCachedNode(realNode);
    JavaScriptObject filteredXml = filterOutImportedBlocks(Utils.textToDom(content));
    Collection<YACachedBlocksNode> children = linkSet.get(cachedNode);
    children = (children == null ? new HashSet<YACachedBlocksNode>() : children);
    Element xml = setChildrenHeader(filteredXml, makeChildrenXmlArray(children));
    return Utils.domToText(xml);
  }

  public static void getHeader(YoungAndroidBlocksNode node, final Callback<Element> onload) {
    YACachedBlocksNode.getCachedNode(node).load(new Callback<String>() {
      @Override
      public void call(String s) {
        Element header =  querySelector(Utils.textToDom(s),
                Utils.eval("\"header\"")); //TODO (evan): figure out how to make a javascript string
        if (header != null) onload.call(header);
      }
    });

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
            if (depth > 0) qualifyFunctions(blocks, node.getFormName());
            addAllBlocks(finalDom, blocks);

            thisLinked.call(finalDom); //!!! links up to here up level 2
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
        final Collection<Tuple<Long, String>> ids = getFileIds(childrenXml);

        Set<Project> childProjects = new HashSet<Project>();
        for (Tuple<Long, String> tup : ids) {
          Project p = Ode.getInstance().getProjectManager().getProject(tup.fst);
          if (p != null) childProjects.add(p);
        }

        Callback onChildrenProjectsLoaded = new Callback<Collection<Project>>() {
                  @Override
                  public void call(Collection<Project> avoid) {
                    for (Tuple<Long, String> id : ids) {
                      YACachedBlocksNode child = YACachedBlocksNode.getCachedNode(id.fst, id.snd);
                      linkSet.get(node).add(child); //linkSet.get(node) is a Set, if it already has this child, the new child won't be added
                    }
                    onload.call(linkSet.get(node)); //error is here!!! goes up to llc (private)
                  }
                };

        if (childProjects.size() == 0) {
          onChildrenProjectsLoaded.call(new ArrayList<Project>());
        } else {
          CollectorCallback<Project> countdown = new CollectorCallback<Project>(childProjects.size(), onChildrenProjectsLoaded);
          for (Project p : childProjects) {
            Project.onLoadProjectNodes(p, countdown);
          }
        }
      }
    });
    Helper.unindent();
  }

  private static Collection<Tuple<Long, String>> getFileIds(JsArray<Element> children) {
    ArrayList<Tuple<Long, String>> ids = new ArrayList<Tuple<Long, String>>();
    for (int i = 0; i < children.length(); i++) {
      Element childXml = children.get(i);
      String projectIdString = childXml.getAttribute("projectId");
      if (projectIdString.length() == 0) projectIdString = childXml.getAttribute("projectid");  //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
      long projectId = Long.parseLong(projectIdString);
      String fileId = childXml.getAttribute("fileId");
      if (fileId.length() == 0) fileId = childXml.getAttribute("fileid"); //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
      ids.add(new Tuple<Long, String>(projectId, fileId));
    }
    return ids;
  }


  private static void labelBlocks(JsArray<Element> blocks, int depth) {
    //TODO (evan): when you add components, label each function with the version of it (ie what components it's using)
    for (int i = 0; i<blocks.length(); i++) {
      blocks.get(i).setAttribute("depth", depth + "");
    }
  }

  private static void qualifyFunctions(JsArray<Element> blocks, String name) {
    for (int i = 0; i<blocks.length(); i++) {
      Element elm = blocks.get(i);
      if (!(elm.getAttribute("type").equals("procedures_defnoreturn") ||
              elm.getAttribute("type").equals("procedures_defreturn"))) continue;
      Element nameField = querySelector(elm, Utils.eval("\"field[name=NAME]\""));
      if (nameField == null) {
        Helper.consolePrint(elm);
        Helper.debugger();
        throw new RuntimeException("procedure has no name field!!!");
      }
      nameField.setInnerHTML(name + "_" + nameField.getInnerHTML());
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

  private static native Element querySelector(Element xml, JavaScriptObject s) /*-{
    return xml.querySelector(s);
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

  private static class Tuple<S, T> {
    final S fst;
    final T snd;
    public Tuple(S s, T t) {
      fst = s;
      snd = t;
    }

    public String toString() {
      return fst.toString() + "_" + snd.toString();
    }
  }
}
