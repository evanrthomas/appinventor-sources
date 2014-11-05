package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentSet;
import com.google.appinventor.client.editor.simple.SimpleNonVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.SimpleVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.components.MockCanvas;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.palette.SimplePalettePanel;
import com.google.appinventor.client.editor.youngandroid.palette.YoungAndroidPalettePanel;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class YaSharedPageEditor extends YaCodePageEditor {
  private final ComponentSet components;
  private final SimpleVisibleComponentsPanel visibleComponentsPanel;
  private final SimpleNonVisibleComponentsPanel nonVisibleComponentsPanel;
  private final YoungAndroidPalettePanel palettePanel;

  public YaSharedPageEditor(YaProjectEditor projectEditor, YoungAndroidBlocksNode blocksNode) {
    super(projectEditor, blocksNode);
    components = new ComponentSet();
    components.addComponent(new MockCanvas(this));

    nonVisibleComponentsPanel = new SimpleNonVisibleComponentsPanel();
    visibleComponentsPanel = new SimpleVisibleComponentsPanel(this, nonVisibleComponentsPanel);
    palettePanel = new YoungAndroidPalettePanel(null);

    //palettePanel.loadComponents(new DropTargetProvider() {
    //  @Override
    //  public DropTarget[] getDropTargets() {
    //  // TODO(markf): Figure out a good way to memorize the targets or refactor things so that
    //  // getDropTargets() doesn't get called for each component.
    //  // NOTE: These targets must be specified in depth-first order.
    //  List<DropTarget> dropTargets = form.getDropTargetsWithin();
    //  dropTargets.add(visibleComponentsPanel);
    //  dropTargets.add(nonVisibleComponentsPanel);
    //  return dropTargets.toArray(new DropTarget[dropTargets.size()]);
    //  }
//    });

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

  public SimplePalettePanel getComponentPalettePanel() {
    return palettePanel;
  }

  public SimpleNonVisibleComponentsPanel getNonVisibleComponentsPanel() {
    return nonVisibleComponentsPanel;
  }

  public boolean isScreen1() {
    return false;
  }
}
