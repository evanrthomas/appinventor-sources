package com.google.appinventor.client;

import com.google.appinventor.client.output.OdeLog;

import java.util.Collection;

public class Helper {
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

  public static native void consolePrint(String s) /*-{
     $wnd.console.log(s);
    }-*/;

  public static native void eval(String s) /*-{
     $wnd.eval(s);
    }-*/;
}