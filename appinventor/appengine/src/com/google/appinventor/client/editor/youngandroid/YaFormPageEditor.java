
package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.editor.simple.components.FormChangeListener;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.components.MockForm;
import com.google.appinventor.shared.rpc.project.FileDescriptorWithContent;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.List;
import java.util.Map;

public final class YaFormPageEditor extends YaCodePageEditor implements FormChangeListener  {

  // The form editor associated with this blocks editor
  private YaFormEditor myFormEditor;
  protected YaFormPageEditor(YaProjectEditor projectEditor, YACachedBlocksNode blocksNode) {
    super(projectEditor, blocksNode);
    myFormEditor = projectEditor.getFormFileEditor(blocksNode.getFormName());
  }

  @Override
  public Map<String, MockComponent> getComponents() {
    return myFormEditor.getComponents();
  }

  @Override
  public List<String> getComponentNames() {
    return myFormEditor.getComponentNames();
  }


  @Override
  public boolean isFormPageEditor() {
    return true;
  }

  @Override
  public boolean isScreen1() {
    return myFormEditor.isScreen1();
  }

  @Override
  protected TreeItem getComponentsTree() {
     return getForm().buildComponentsTree();
  }


  @Override
  public void onClose() {
    getForm().removeFormChangeListener(this);
    super.onClose();
  }


  // FormChangeListener implementation
  // Note: our companion YaFormEditor adds us as a listener on the form

  /*
   * @see com.google.appinventor.client.editor.simple.components.FormChangeListener#
   * onComponentPropertyChanged
   * (com.google.appinventor.client.editor.simple.components.MockComponent, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void onComponentPropertyChanged(
          MockComponent component, String propertyName, String propertyValue) {
    // nothing to do here
  }

  /*
   * @see
   * com.google.appinventor.client.editor.simple.components.FormChangeListener#onComponentRemoved
   * (com.google.appinventor.client.editor.simple.components.MockComponent, boolean)
   */
  @Override
  public void onComponentRemoved(MockComponent component, boolean permanentlyDeleted) {
    if (permanentlyDeleted) {
      removeComponent(component);
      if (loadComplete) {
        updateSourceStructureExplorer();
      }
    }
  }

  /*
   * @see
   * com.google.appinventor.client.editor.simple.components.FormChangeListener#onComponentAdded
   * (com.google.appinventor.client.editor.simple.components.MockComponent)
   */
  @Override
  public void onComponentAdded(MockComponent component) {
    addComponent(component);
    if (loadComplete) {
      // Update source structure panel
      updateSourceStructureExplorer();
    }
  }

  /*
   * @see
   * com.google.appinventor.client.editor.simple.components.FormChangeListener#onComponentRenamed
   * (com.google.appinventor.client.editor.simple.components.MockComponent, java.lang.String)
   */
  @Override
  public void onComponentRenamed(MockComponent component, String oldName) {
    renameComponent(oldName, component);
    if (loadComplete) {
      updateSourceStructureExplorer();
      // renaming could potentially confuse an open drawer so close just in case
      hideComponentBlocks();
      selectedDrawer = null;
    }
  }


  /*
   * @see com.google.appinventor.client.editor.simple.components.FormChangeListener#
   * onComponentSelectionChange
   * (com.google.appinventor.client.editor.simple.components.MockComponent, boolean)
   */
  @Override
  public void onComponentSelectionChange(MockComponent component, boolean selected) {
    // not relevant for blocks editor - this happens on clicks in the mock form areas
  }

  @Override
  protected void updateSourceStructureExplorer() {
    MockForm form = getForm();
    if (form != null) {
      updateBlocksTree(form.getSelectedComponent().getSourceStructureExplorerItem());
    }
  }


  @Override
  public boolean isLoadComplete() {
    if (getForm() != null) {
      return loadComplete;
    } else {
      return false;
    }
  }

  public MockForm getForm() {
    YaProjectEditor yaProjectEditor = (YaProjectEditor) projectEditor;
    YaFormEditor myFormEditor = yaProjectEditor.getFormFileEditor(blocksNode.getFormName());
    if (myFormEditor != null) {
      return myFormEditor.getForm();
    } else {
      throw new ScreenPageHasNoFormException();
    }
  }

  public synchronized void sendComponentData() {
    try {
      blocksArea.sendComponentData(myFormEditor.encodeFormAsJsonString(),
              packageNameFromPath(getFileId()));
    } catch (YailGenerationException e) {
      e.printStackTrace();
    }
  }


  public FileDescriptorWithContent getYail() throws YailGenerationException {
    return new FileDescriptorWithContent(getProjectId(), yailFileName(),
            blocksArea.getYail(myFormEditor.encodeFormAsJsonString(),
                    packageNameFromPath(getFileId())));
  }

  private String yailFileName() {
    String fileId = getFileId();
    return fileId.replace(YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION,
            YoungAndroidSourceAnalyzer.YAIL_FILE_EXTENSION);
  }


  /**
   * Converts a source file path (e.g.,
   * src/com/gmail/username/project1/Form.extension) into a package
   * name (e.g., com.gmail.username.project1.Form)
   * @param path the path to convert.
   * @return a dot separated package name.
   */
  private static String packageNameFromPath(String path) {
    path = path.replaceFirst("src/", "");
    int extensionIndex = path.lastIndexOf(".");
    if (extensionIndex != -1) {
      path = path.substring(0, extensionIndex);
    }
    return path.replaceAll("/", ".");
  }

  private class ScreenPageHasNoFormException extends RuntimeException {}
}