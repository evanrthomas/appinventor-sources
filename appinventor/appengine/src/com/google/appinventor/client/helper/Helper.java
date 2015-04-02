package com.google.appinventor.client.helper;

import com.google.appinventor.client.Ode;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.editor.youngandroid.YaSharedPageEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import java.util.Collection;

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

  public static native String jsonToString(JavaScriptObject obj) /*-{
    return JSON.stringify(obj);
  }-*/;

  public static native String jsonToString(JavaScriptObject obj, int tabs) /*-{
    return JSON.stringify(obj, undefined, tabs);
  }-*/;

  public static String getProjectName(YaCodePageEditor editor) {
    return editor.getProjectRootNode().getName();

  }

  public static String editorDescriptor(YaCodePageEditor editor) {
    return editor.getProjectId() + "_"  + editor.getName();
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




  // ---------------------------------------------------------
  //              helper exported functions
  // ---------------------------------------------------------



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
    Project project = Ode.getInstance().getProjectManager().getProject(projectName);
    YaCodePageEditor editor = YaCodePageEditor.getCodePageEditorByFileName(project.getProjectId(), fileName);
    if (editor == null) return null;

    JSONObject editorblob = new JSONObject();
    JSONArray children = new JSONArray();
    editorblob.put("content", new JSONString(editor.getCachedContent()));
    for (YaCodePageEditor child: editor.getChildren()) {
      JSONObject childblob = new JSONObject();
      childblob.put("name",   new JSONString(child.getName()));
      childblob.put("fileId", new JSONString(child.getFileId()));
      childblob.put("projectId", new JSONNumber(child.getProjectId()));
      children.set(children.size(), childblob);
    }
    editorblob.put("children", children);
    return editorblob.getJavaScriptObject();
  }



  public static native void exportHelperMethods() /*-{
      console.log("HELPER METHODS ARE BEING EXPORTED!!!");
      $wnd.exported = $wnd.exported || {};
      $wnd.exported.getAllProjects = $entry(@com.google.appinventor.client.helper.Helper::getAllProjects());
      $wnd.exported.inspectProject = $entry(@com.google.appinventor.client.helper.Helper::inspectProject(Ljava/lang/String;));
      $wnd.exported.inspectFile = $entry(@com.google.appinventor.client.helper.Helper::inspectFile(Ljava/lang/String;Ljava/lang/String;));

      //the following puts stuff on global namespace, might be unsafe ...
      $wnd.printProjects = function() {
        console.log(JSON.stringify($wnd.exported.getAllProjects(), undefined, 2));
      };
      $wnd.printProject = function(projectId) {
        console.log(JSON.stringify($wnd.exported.inspectProject(projectId), undefined, 2));
      };
      $wnd.printFile = function(projectName, fileName) {
        console.log(JSON.stringify($wnd.exported.inspectFile(projectName, fileName), undefined, 2));
      };

      $wnd.projects = $wnd.exported.getAllProjects;
      $wnd.ip = $wnd.exported.inspectProject;
      $wnd.ifl = $wnd.exported.inspectFile;
      $wnd.pp = $wnd.printProject;
      $wnd.pf = $wnd.printFile;
      $wnd.pps = $wnd.printProjects;
  }-*/;
}