package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.boxes.BlockSelectorBox;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.components.MockForm;
import com.google.appinventor.client.editor.simple.palette.SimpleComponentDescriptor;
import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.appinventor.client.explorer.SourceStructureExplorerItem;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.Utils;
import com.google.appinventor.client.linker.Linker;
import com.google.appinventor.client.properties.json.ClientJsonParser;
import com.google.appinventor.client.widgets.TextButton;
import com.google.appinventor.client.youngandroid.YoungAndroidFormUpgrader;
import com.google.appinventor.shared.properties.json.JSONObject;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
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

  public YaSharedPageEditor(YaProjectEditor projectEditor, YoungAndroidBlocksNode blocksNode) {
    super(projectEditor, blocksNode);
    Linker.getInstance().loadLinkedContent(blocksNode, new Callback<String>() {
      @Override
      public void call(String content) {
        if (!content.equals("")) addComponentsFromHeader(Utils.textToDom(content));
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
    Element newcontents = setDemandedComponentsHeader(Utils.textToDom(super.getRawFileContent()),
            componentsXmlArr);
    String s = Utils.domToText(newcontents);
    return s;
  }

  @Override
  protected void updateSourceStructureExplorer() {
    //TODO (evan): not really sure how updateSourceStructureExplorer and updateBlocksTree are different
    updateBlocksTree(null);
  }

  @Override
  protected void updateBlocksTree(SourceStructureExplorerItem itemToSelect) {
    TreeItem items[] = new TreeItem[2];
    items[0] = BlockSelectorBox.getBlockSelectorBox().getBuiltInBlocksTree();
    items[1] = getAnyComponentsTree();

    TextButton button = new TextButton("Add Component");
    //tree.setState(false);
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
    items[1].addItem(button);

    sourceStructureExplorer.updateTree(items, itemToSelect);
    BlockSelectorBox.getBlockSelectorBox().setContent(sourceStructureExplorer);
  }

  private native Element setDemandedComponentsHeader(
          Node blocklyXml, JsArray<Node> arr) /*-{
    return $wnd.exported.setDemandedComponentsHeader(blocklyXml, arr);
  }-*/;

  @Override
  public String getUpgraderJson() {
    String encodedProperties = encodeFormAsJsonString();
    JSONObject propertiesObject = new ClientJsonParser().parse(encodedProperties).asObject();
    if (YoungAndroidFormUpgrader.upgradeSourceProperties(propertiesObject.getProperties())) {
      String upgradedContent = YoungAndroidSourceAnalyzer.generateSourceFile(propertiesObject);
      return upgradedContent;
    }
    return propertiesObject.toJson();
  }

  @Override
  public String encodeFormAsJsonString() {
    MockForm form = new MockForm(this);
    for (MockComponent comp: components.getComponents()) {
      form.addComponent(comp);
    }
    return MockComponent.encodeFormAsJsonString(form);
  }

}
