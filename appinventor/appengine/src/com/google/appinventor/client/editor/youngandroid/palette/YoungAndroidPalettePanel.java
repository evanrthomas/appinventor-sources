// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2014 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client.editor.youngandroid.palette;


import com.google.appinventor.client.TranslationDesignerPallete;
import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.palette.DropTargetProvider;
import com.google.appinventor.client.editor.simple.palette.SimpleComponentDescriptor;
import com.google.appinventor.client.editor.simple.palette.SimplePaletteItem;
import com.google.appinventor.client.editor.simple.palette.SimplePalettePanel;
import com.google.appinventor.common.version.AppInventorFeatures;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.StackPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.HashMap;
import java.util.Map;

/**
 * Panel showing Simple components which can be dropped onto the Young Android
 * visual designer panel.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public class YoungAndroidPalettePanel extends Composite implements SimplePalettePanel {

  // Component database: information about components (including their properties and events)
  private static final SimpleComponentDatabase COMPONENT_DATABASE =
    SimpleComponentDatabase.getInstance();

  // Associated editor
  private final SimpleEditor editor;

  private final StackPanel stackPalette;
  private final Map<ComponentCategory, VerticalPanel> categoryPanels;

  /**
   * Creates a new component palette panel.
   *
   * @param editor parent editor of this panel
   */
  public YoungAndroidPalettePanel(SimpleEditor editor) {
    this.editor = editor;
    stackPalette = new StackPanel();
    categoryPanels = new HashMap<ComponentCategory, VerticalPanel>();

    for (ComponentCategory category : ComponentCategory.values()) {
      if (showCategory(category)) {
        VerticalPanel categoryPanel = new VerticalPanel();
        categoryPanel.setWidth("100%");
        categoryPanels.put(category, categoryPanel);
        stackPalette.add(categoryPanel,
            TranslationDesignerPallete.getCorrespondingString(category.getName()));
      }
    }

    stackPalette.setWidth("100%");
    initWidget(stackPalette);
  }

  @Override
  public void configureComponent(MockComponent mockComponent) { //TODO (evan): remove this method. It's only here because SimplePalettePanel demands it
    mockComponent.configureComponent();

  }

  private static boolean showCategory(ComponentCategory category) {
    if (category == ComponentCategory.UNINITIALIZED) {
      return false;
    }
    if (category == ComponentCategory.INTERNAL &&
      !AppInventorFeatures.showInternalComponentsCategory()) {
      return false;
    }
    return true;
  }

  /**
   * Loads all components to be shown on this palette.  Specifically, for
   * each component (except for those whose category is UNINITIALIZED, or
   * whose category is INTERNAL and we're running on a production server,
   * or who are specifically marked as not to be shown on the palette),
   * this creates a corresponding {@link SimplePaletteItem} with the passed
   * {@link DropTargetProvider} and adds it to the panel corresponding to
   * its category.
   *
   * @param dropTargetProvider provider of targets that palette items can be
   *                           dropped on
   */
  @Override
  public void loadComponents(DropTargetProvider dropTargetProvider) {
    loadComponents(dropTargetProvider, null);
  }

  public void loadComponents(DropTargetProvider dropTargetProvider, final YoungAndroidPalettePanelClickHandler handler) {
    for (String component : COMPONENT_DATABASE.getComponentNames()) {
      String categoryString = COMPONENT_DATABASE.getCategoryString(component);
      String helpString = COMPONENT_DATABASE.getHelpString(component);
      String categoryDocUrlString = COMPONENT_DATABASE.getCategoryDocUrlString(component);
      Boolean showOnPalette = COMPONENT_DATABASE.getShowOnPalette(component);
      Boolean nonVisible = COMPONENT_DATABASE.getNonVisible(component);
      ComponentCategory category = ComponentCategory.valueOf(categoryString);
      if (showOnPalette && showCategory(category)) {
        final SimpleComponentDescriptor descriptor =   new SimpleComponentDescriptor(component, editor, helpString,
              categoryDocUrlString, showOnPalette, nonVisible);
        SimplePaletteItem item = new SimplePaletteItem(descriptor, dropTargetProvider);
        if (handler != null) {
          item.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
              handler.onClick(descriptor, event);

            }
          });
        }
        addPaletteItem(item, category);
      }
    }
  }


  /*
   * Adds a component entry to the palette.
   */
  private void addPaletteItem(SimplePaletteItem component, ComponentCategory category) {
    VerticalPanel panel = categoryPanels.get(category);
    panel.add(component);
  }

  public interface YoungAndroidPalettePanelClickHandler {
    public void onClick(SimpleComponentDescriptor descriptor, ClickEvent event);
  }
}
