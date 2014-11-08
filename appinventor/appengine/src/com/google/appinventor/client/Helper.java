package com.google.appinventor.client;

import com.google.appinventor.client.output.OdeLog;

public class Helper {
  public static void printJS(String s) {
    consolePrint(s);
    OdeLog.log(s);
  }
  public static native void consolePrint(String s) /*-{
     $wnd.console.log(s);
    }-*/;

  public static native void eval(String s) /*-{
     $wnd.eval(s);
    }-*/;
}