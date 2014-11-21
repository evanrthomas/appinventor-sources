package com.google.appinventor.client.editor.simple;

import com.google.appinventor.client.editor.simple.palette.SimplePalettePanel;

public interface SimpleComponentPropertyEditor {
  /**
   * Returns the component palette panel
   *
   * @return  component palette panel
   */
  public SimplePalettePanel getComponentPalettePanel();

  /**
   * Returns the non-visible components panel
   *
   * @return  non-visible components panel
   */
  public SimpleNonVisibleComponentsPanel getNonVisibleComponentsPanel();
}
