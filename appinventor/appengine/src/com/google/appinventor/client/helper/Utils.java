package com.google.appinventor.client.helper;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;

public class Utils {
  public static native JavaScriptObject callJSFunc(JavaScriptObject callback, JavaScriptObject arg) /*-{
      return callback(arg);
    }-*/;

  public static Element blocklyXmlContainer() {
    //TODO (evan): allow changing the ya-version and language-version numbers
    return textToDom("<xml xmlns=\"http://www.w3.org/1999/xhtml\"> <yacodeblocks ya-version=\"104\" language-version=\"17\"></yacodeblocks> </xml>");
  }


  public static Element textToDom(String s) {
    if (s.equals("")) {
      return blocklyXmlContainer();
    }
    return myTextToDom(s);
  }
  public static native Element myTextToDom(String s) /*-{
    return $wnd.exported.textToDom(s);
  }-*/;

  public static native String domToText(Element xml) /*-{
    return $wnd.exported.domToText(xml);
  }-*/;
}
