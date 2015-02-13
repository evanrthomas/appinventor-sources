package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentList;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.palette.SimpleComponentDescriptor;
import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.appinventor.client.widgets.TextButton;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
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
    blocksNode.load(new OdeAsyncCallback<ChecksumedLoadFile>() {
      @Override
      public void onSuccess(ChecksumedLoadFile result) {
        try {
          String content;
          if ((content = result.getContent()) != "") {
            addComponentsFromHeader(textToDom(content));
          }
        } catch (ChecksumedFileException e) {
          onFailure(e);
        }
      }
    });
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

  private void addComponentsFromHeader(JavaScriptObject blocklyXml) {
    JsArray<Element> comps = getComponentsFromHeader(blocklyXml);
    for (int i =0; i<comps.length(); i++) {
      MockComponent comp  = SimpleComponentDescriptor.createMockComponent(
              comps.get(i).getAttribute("type"),
              this);
      comp.changeProperty("name", comps.get(i).getAttribute("name"));
      addComponent(comp);
    }
  }

  private native JsArray<Element> getComponentsFromHeader(JavaScriptObject blocklyXml) /*-{
    return blocklyXml.querySelectorAll('header > demanded_components > *');
  }-*/;

  public boolean isScreen1() {
    return false;
  }

  @Override
  public String getRawFileContent() {
    JsArray<Node> componentsXmlArr = JavaScriptObject.createArray().cast();
    for (MockComponent comp: components.getComponents()) {
      componentsXmlArr.push(comp.toXmlRepr());
    }
    Node newcontents = setDemandedComponentsHeader(textToDom(super.getRawFileContent()),
            componentsXmlArr);
    String s = domToText(newcontents);
    return s;
  }

  @Override
  protected void updateSourceStructureExplorer() {
    //TODO (evan): not really sure how updateSourceStructureExplorer and updateBlocksTree are different
    updateBlocksTree(null);
  }

  protected TreeItem getComponentsTree() {
    TreeItem tree = ComponentList.buildTree(components);
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

  private native Node setDemandedComponentsHeader(
          Node blocklyXml, JsArray<Node> arr) /*-{
    return $wnd.exported.setDemandedComponentsHeader(blocklyXml, arr);
  }-*/;

}
