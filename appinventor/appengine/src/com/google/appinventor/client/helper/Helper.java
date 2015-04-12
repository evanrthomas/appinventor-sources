package com.google.appinventor.client.helper;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.editor.youngandroid.YaSharedPageEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.linker.Linker;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.shared.rpc.project.FileNode;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Helper {

  public static native void indent() /*-{
    $wnd.console.group();
  }-*/;

  public static void indent(String msg) {
    Helper.println(msg);
    Helper.indent();
  }


  public static native void unindent() /*-{
    $wnd.console.groupEnd();
  }-*/;

  public static native void groupCollapsed() /*-{
     $wnd.console.groupCollapsed();
  }-*/;

  public static void groupCollapsed(String msg) {
    Helper.println(msg);
    Helper.groupCollapsed();
  }

  public static void println(String s) {
    consolePrint(s);
    OdeLog.log(s);
  }

  public static<T> void println(Collection<T> items) {
    String s = "";
    for (T item: items) {
      s += (item.toString() + " ");
    }
    println(s);
  }

  public static<T> void println(String msg, Collection<T> items) {
    println(msg);
    println(items);
  }


  public static native void consolePrint(JavaScriptObject s) /*-{
     $wnd.console.log(s);
    }-*/;

  public static native void consolePrint(String s) /*-{
     $wnd.console.log(s);
    }-*/;

  public static native JavaScriptObject eval(String s) /*-{
     return $wnd.eval(s);
    }-*/;

  public static native JavaScriptObject printJson(JavaScriptObject json) /*-{
    console.log(JSON.stringify(json, undefined, 2));
  }-*/;

  public static native void set(Object o) /*-{
    $wnd.obj = o;
  }-*/;

  public static void debugger(String msg) {
    println(msg);
    debugger();
  }

  public static native void debugger() /*-{
    debugger;
  }-*/;



  public static String getProjectName(YaCodePageEditor editor) {
    return editor.getProjectRootNode().getName();

  }

  public static String editorDescriptor(FileEditor editor) {
    return editor.getProjectRootNode().getName() + "_"  + editor.getFileNode().getName();
  }

  public static void printChildrenDescriptor(Collection<YaSharedPageEditor> children) {
    Helper.println("childrenDescriptor " + children.size());
    Helper.indent();
    for (YaSharedPageEditor child: children) {
      Helper.println(child.getProjectId() + "_" + child.getName());
    }
     Helper.unindent();
  }

  public static void printChildrenDescriptor(String message, Collection<YaSharedPageEditor> children) {
    Helper.println(message);
    Helper.printChildrenDescriptor(children);
  }

  public static String getNodeName(FileNode node) {
    return node.getProjectRoot().getName() + "_" + node.getName();
  }

  public static String getNodeName(YACachedBlocksNode node) {
    return getNodeName(node.getRealNode());
  }


  // ---------------------------------------------------------
  //              helper exported functions
  // ---------------------------------------------------------



  private static FileNode getFileNodeByFileName(long projectId, String fileName) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectId);
    if (project == null) {
      Helper.println("invalid projectId, project does not exist");
      return null;
    }
    ProjectRootNode root = project.getRootNode();
    if (root == null) {
      Helper.println("have not called project.loadNodes() yet");
      return null;
    }

    for (ProjectNode potentialNode : root.getAllSourceNodes()) {
      if (potentialNode.getName().equals(fileName) && potentialNode instanceof YoungAndroidBlocksNode) {
        return (FileNode)potentialNode;
      }
    }
    return null;

  }

  public static JavaScriptObject getAllProjects() {
    Collection<Project> projects = Ode.getInstance().getProjectManager().getProjects();
    JSONArray allJsonProjects = new JSONArray();
    for (Project p: projects) {
      JSONObject thisjsonproject = new JSONObject();
      thisjsonproject.put("name", new JSONString(p.getProjectName()));
      thisjsonproject.put("projectId", new JSONNumber(p.getProjectId()));
      thisjsonproject.put("type", new JSONString(p.getProjectType()));
      allJsonProjects.set(allJsonProjects.size(), thisjsonproject);
    }
    return allJsonProjects.getJavaScriptObject();
  }

  public static JavaScriptObject inspectProject(String projectIdAsStringOrProjectName) {
    Project project;
    try {
      long projectId = Long.parseLong(projectIdAsStringOrProjectName); //assume this is a long formatted as string
      project = Ode.getInstance().getProjectManager().getProject(projectId);
      if (project == null) {
        Helper.println("Error getting project :: invalid projectId " + projectIdAsStringOrProjectName);
        return null;
      }
    } catch (NumberFormatException e){
      //assume the string is the name of the project
      project = Ode.getInstance().getProjectManager().getProject(projectIdAsStringOrProjectName);
      if (project == null) {
        Helper.println("Error getting project :: not long and not valid project name " + projectIdAsStringOrProjectName);
        return null;
      }
    }
    JSONObject jsonblob = new JSONObject();
    jsonblob.put("name", new JSONString(project.getProjectName()));
    jsonblob.put("type", new JSONString(project.getProjectType()));
    jsonblob.put("projectId", new JSONNumber(project.getProjectId()));

    ProjectRootNode root;
    if ((root = project.getRootNode()) != null) {
      JSONArray files = new JSONArray();
      for (ProjectNode pnode :root.getAllSourceNodes()){
        JSONObject fileblob = new JSONObject();
        fileblob.put("name", new JSONString(pnode.getName()));
        fileblob.put("fileId", new JSONString(pnode.getFileId()));
        fileblob.put("projectId", new JSONNumber(pnode.getProjectId()));
        files.set(files.size(), fileblob);
      }

      jsonblob.put("files", files);
    }
    jsonblob.put("loadProjectNodes",  new JSONObject(getLoadProjectNodesFunction(project)));
    return jsonblob.getJavaScriptObject();
  }

  public static native JavaScriptObject getLoadProjectNodesFunction(Project project) /*-{
    return $entry(function() {
      project.@com.google.appinventor.client.explorer.project.Project::loadProjectNodes()();
    });
  }-*/;

  public static JavaScriptObject inspectFile(String projectName, String fileName) {
    return Helper.eval("\"deprecated\"");
    /*
    Project project = Ode.getInstance().getProjectManager().getProject(projectName);
    project.getRootNode().getSourceNode()
    YACachedBlocksNode cachedNode = YACachedBlocksNode.getCachedNode(project.getProjectId(), fileName);
    if (cachedNode == null) {
      Helper.println("error :: editor is null");
      return null;
    }

    JSONObject editorblob = new JSONObject();
    JSONArray children = new JSONArray();
    editorblob.put("content", new JSONString(cachedNode.getContent()));
    for (YACachedBlocksNode child: Linker.getInstance().getChildren(cachedNode)) {
      JSONObject childblob = new JSONObject();
      childblob.put("name",   new JSONString(child.getName()));
      childblob.put("fileId", new JSONString(child.getFileId()));
      childblob.put("projectId", new JSONNumber(child.getProjectId()));
      children.set(children.size(), childblob);
    }
    editorblob.put("children", children);
    return editorblob.getJavaScriptObject();
    */
  }

  public static String getCachedContent(String projectName, String fileName) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectName);
    if (project == null) return "cannot find project for " + projectName;
    FileNode fileNode = getFileNodeByFileName(project.getProjectId(), fileName);
    if (fileNode == null) return "cannot find file " + projectName + " " + fileName;
    YACachedBlocksNode cachedNode =  YACachedBlocksNode.getCachedNode(project.getProjectId(), fileNode.getFileId());
    if (cachedNode == null) return "cannot find cachedContent of " + projectName + " " + fileName;
    return cachedNode.getContent();
  }

  public static void printLinkedContent(String projectName, String fileName) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectName);
    if (project == null) {
      println("cannot find project for " + projectName);
      return;
    }
    FileNode fileNode = getFileNodeByFileName(project.getProjectId(), fileName);
    if (fileNode == null) {
      println( "cannot find file " + projectName + " " + fileName);
      return;
    }
    Linker.getInstance().loadLinkedContent((YoungAndroidBlocksNode)fileNode, new Callback<String>() {
      @Override
      public void call(String s) {
        println("LINKED CONTENT\n" +s);
      }
    });
  }

  public static void printRawContent(String projectName, String fileName) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectName);
    if (project == null) {
      println("cannot find project for " + projectName);
      return;
    }
    FileNode fileNode = getFileNodeByFileName(project.getProjectId(), fileName);
    if (fileNode == null) {
      println( "cannot find file " + projectName + " " + fileName);
      return;
    }
    println( YaCodePageEditor.getOrCreateEditor((YoungAndroidBlocksNode)fileNode).getRawFileContent());
  }
  public static JavaScriptObject getDirtyEditors() {
    Collection<FileEditor> editors = Ode.getInstance().getEditorManager().getDirtyEditors();
    JSONArray arr = new JSONArray();
    for (FileEditor editor: editors) {
      arr.set(arr.size(), new JSONString(editorDescriptor(editor)));
    }
    return arr.getJavaScriptObject();
  }

  private static Map<String, Scheduler.ScheduledCommand> scheduledCommands =
          new HashMap<String, Scheduler.ScheduledCommand>();
  private static String running;
  public static void schedule(final String commandName, final Scheduler.ScheduledCommand command) {
    Scheduler.ScheduledCommand newCommand = new Scheduler.ScheduledCommand() {
      @Override
      public void execute() {
        scheduledCommands.remove(commandName);
        running = commandName;
        command.execute();
        running = null;
      }
    };
    scheduledCommands.put(commandName, newCommand);
    Scheduler.get().scheduleDeferred(newCommand);
  }

  public static JavaScriptObject getScheduledTasks() {
    JSONObject scheduedBlob = new JSONObject();
    JSONArray all = new JSONArray();
    for (String key: scheduledCommands.keySet()) {
      all.set(all.size(), new JSONString(key+""));
    }
    scheduedBlob.put("running", new JSONString(running+""));
    scheduedBlob.put("all", all);
    return scheduedBlob.getJavaScriptObject();
  }

  public static native void exportHelperMethods() /*-{
      console.log("HELPER METHODS ARE BEING EXPORTED!!!");
      $wnd.exported = $wnd.exported || {};
      $wnd.exported.getAllProjects = $entry(@com.google.appinventor.client.helper.Helper::getAllProjects());
      $wnd.exported.inspectProject = $entry(@com.google.appinventor.client.helper.Helper::inspectProject(Ljava/lang/String;));
      $wnd.exported.inspectFile = $entry(@com.google.appinventor.client.helper.Helper::inspectFile(Ljava/lang/String;Ljava/lang/String;));

      //the following puts stuff on global namespace, might be unsafe ...

      var printJson = $entry(@com.google.appinventor.client.helper.Helper::printJson(Lcom/google/gwt/core/client/JavaScriptObject;));
      $wnd.printProjects = function() {
        printJson($wnd.exported.getAllProjects());
      };

      $wnd.printProject = function(projectId) {
        printJson($wnd.exported.inspectProject(projectId));
      };

      $wnd.printFile = function(projectName, fileName) {
        printJson($wnd.exported.inspectFile(projectName, fileName), undefined, 2);
      };
      $wnd.pj = printJson;

      $wnd.printCachedContent = function(projectName, fileName) {
        console.log(@com.google.appinventor.client.helper.Helper::getCachedContent(Ljava/lang/String;Ljava/lang/String;)(projectName, fileName));
      };

      $wnd.printLinkedContent = function(projectName, fileName) {
        @com.google.appinventor.client.helper.Helper::printLinkedContent(Ljava/lang/String;Ljava/lang/String;)(projectName, fileName);
      };

      $wnd.printRawContent = function(projectName, fileName) {
        @com.google.appinventor.client.helper.Helper::printRawContent(Ljava/lang/String;Ljava/lang/String;)(projectName, fileName);
       };

      $wnd.printDirtyEditors = function() {
        printJson(@com.google.appinventor.client.helper.Helper::getDirtyEditors()());
      };

      $wnd.printScheduledTasks = function() {
        printJson(@com.google.appinventor.client.helper.Helper::getScheduledTasks()());
       }


      $wnd.pj = printJson;
      $wnd.projects = $wnd.exported.getAllProjects;
      $wnd.ip = $wnd.exported.inspectProject;
      $wnd.ifl = $wnd.exported.inspectFile;
      $wnd.pp = $wnd.printProject;
      $wnd.pf = $wnd.printFile;
      $wnd.pps = $wnd.printProjects;

      $wnd.pcc = $wnd.printCachedContent;
      $wnd.plc = $wnd.printLinkedContent;
      $wnd.pdes = $wnd.printDirtyEditors;
      $wnd.pst = $wnd.getScheduledTasks;
      $wnd.prc = $wnd.printRawContent;
  }-*/;
}
