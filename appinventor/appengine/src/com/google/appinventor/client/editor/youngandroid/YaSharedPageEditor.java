package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentSet;
import com.google.appinventor.client.Helper;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.simple.components.MockButton;
import com.google.appinventor.client.editor.simple.components.MockCanvas;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.palette.SimpleComponentDescriptor;
import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.appinventor.client.widgets.TextButton;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YaSharedPageEditor extends YaCodePageEditor {

  public YaSharedPageEditor(YaProjectEditor projectEditor, YACachedBlocksNode blocksNode) {
    super(projectEditor, blocksNode);
    addComponent(new MockCanvas(this));
    addComponent(new MockButton(this));
  }

  @Override
  public Map<String, MockComponent> getComponents() {
    //TODO (evan): I don't like the idea of returning this map. Instead there should be a getComponentByName method. Doing this because it's required by grandfather class SimpleEditor
    Map<String, MockComponent> map = new HashMap<String, MockComponent>();
    for (MockComponent comp: components.getComponents()) {
      map.put(comp.getName(), comp);
    }
    return map;
  }

  public List<String> getComponentNames() {
    ArrayList<String> names = new ArrayList<String>();
    names.addAll(getComponents().keySet());
    return names;
  }

  public boolean isScreen1() {
    return false;
  }

  @Override
  public String getRawFileContent() {
    JsArray<JavaScriptObject> componentsXmlArr = JavaScriptObject.createArray().cast();
    for (MockComponent comp: components.getComponents()) {
      componentsXmlArr.push(comp.toXmlRepr());
    }
    JavaScriptObject newcontents = setDemandedComponentsHeader(textToDom(super.getRawFileContent()),
            componentsXmlArr);
    String s = domToText(newcontents);
    Helper.println("YaSharedPageEditor.getRawFileContent() " +  s);
    return s;
  }

  @Override
  protected void updateSourceStructureExplorer() {
    //TODO (evan): not really sure how updateSourceStructureExplorer and updateBlocksTree are different
    updateBlocksTree(null);
  }

  protected TreeItem getComponentsTree() {
    TreeItem tree = ComponentSet.buildTree(components);
    TextButton button = new TextButton("Add Component");
    YoungAndroidPalettePanel palettePanel =  new YoungAndroidPalettePanel(YaSharedPageEditor.this);
    final PopupPanel panel = new PopupPanel(true, true);
    panel.setWidget(palettePanel);

    palettePanel.loadComponents(null, new YoungAndroidPalettePanel.YoungAndroidPalettePanelClickHandler() {
      @Override
      public void onClick(SimpleComponentDescriptor descriptor, ClickEvent event) {
        addComponent(descriptor.createMockComponentFromPalette());
        updateSourceStructureExplorer();
        panel.hide();
      }
    });

    button.addClickHandler(new ClickHandler() {
      @Override
      public void onClick(ClickEvent event) {
        panel.show();
      }
    });

    tree.addItem(button);

    return tree;
  }

  private native JavaScriptObject setDemandedComponentsHeader(
          JavaScriptObject blocklyXml, JsArray<JavaScriptObject> arr) /*-{
    return $wnd.exported.setDemandedComponentsHeader(blocklyXml, arr);
  }-*/;

}
