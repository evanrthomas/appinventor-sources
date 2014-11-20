
package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.editor.simple.SimpleNonVisibleComponentsPanel;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.simple.components.MockForm;
import com.google.appinventor.client.editor.simple.palette.SimplePalettePanel;
import com.google.appinventor.shared.rpc.project.FileDescriptorWithContent;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.List;
import java.util.Map;

public final class YaScreenPageEditor extends YaCodePageEditor {

  // The form editor associated with this blocks editor
  private YaFormEditor myFormEditor;
  public YaScreenPageEditor(YaProjectEditor projectEditor, YoungAndroidBlocksNode blocksNode) {
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
  public SimplePalettePanel getComponentPalettePanel() {
    return  myFormEditor.getComponentPalettePanel();
  }

  @Override
  public SimpleNonVisibleComponentsPanel getNonVisibleComponentsPanel() {
    return myFormEditor.getNonVisibleComponentsPanel();
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