package com.google.appinventor.client.helper;

import com.google.appinventor.client.output.OdeLog;
import com.google.gwt.core.client.JavaScriptObject;

import java.util.Collection;

public class Helper {
  private static int tablevel  = 0;

  public static void indent() {
    tablevel += 1;
  }

  public static void unindent() {
    tablevel -= 1;
    if (tablevel < 0) {
      tablevel = 0;
    }
  }

  private static String repeat(String s, int n) {
    String s2 = "";
    for (int i = 0; i<n; i++) {
      s2 += s;
    }
    return s2;
  }

  public static void println(String s) {
    consolePrint(repeat("\t", tablevel) + s);
    OdeLog.log(repeat("\t", tablevel) + s);
  }

  public static<T> void println(Collection<T> items) {
    String s = "";
    for (T item: items) {
      s += (item.toString() + " ");
    }
    println(s);
  }


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
}