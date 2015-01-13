package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentSet;
import com.google.appinventor.client.Helper;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.simple.SimpleNonVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.SimpleVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.components.MockCanvas;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YaSharedPageEditor extends YaCodePageEditor {
  private final ComponentSet components;
  private final SimpleVisibleComponentsPanel visibleComponentsPanel;
  private final SimpleNonVisibleComponentsPanel nonVisibleComponentsPanel;
  private final YoungAndroidPalettePanel palettePanel;

  public YaSharedPageEditor(YaProjectEditor projectEditor, YACachedBlocksNode blocksNode) {
    super(projectEditor, blocksNode);

    nonVisibleComponentsPanel = new SimpleNonVisibleComponentsPanel();
    visibleComponentsPanel = new SimpleVisibleComponentsPanel(this, nonVisibleComponentsPanel);
    palettePanel = new YoungAndroidPalettePanel(null);

    components = new ComponentSet();
    addComponent(new MockCanvas(this));
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
  protected void updateSourceStructureExplorer() {
    updateBlocksTree(null);
  }

  protected TreeItem getComponentsTree() {
    Helper.println("YaSharedPageEditor.getComponentsTree() has any copmonents ??? " + components.getComponents().size());
    return ComponentSet.buildTree(components);
  }
}
