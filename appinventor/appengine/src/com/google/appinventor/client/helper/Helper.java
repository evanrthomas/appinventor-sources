package com.google.appinventor.client.helper;

import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.editor.youngandroid.YaSharedPageEditor;
import com.google.appinventor.client.output.OdeLog;
import com.google.gwt.core.client.JavaScriptObject;

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

  public static native void eval(String s) /*-{
     $wnd.eval(s);
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
}