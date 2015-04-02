// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt
package com.google.appinventor.client.editor.youngandroid;

import com.google.appinventor.client.ComponentList;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.YACachedBlocksNode;
import com.google.appinventor.client.boxes.AssetListBox;
import com.google.appinventor.client.boxes.BlockSelectorBox;
import com.google.appinventor.client.boxes.PaletteBox;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.simple.SimpleComponentDatabase;
import com.google.appinventor.client.editor.simple.SimpleEditor;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.explorer.SourceStructureExplorer;
import com.google.appinventor.client.explorer.SourceStructureExplorerItem;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.CountDownCallback;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YAFormPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.TreeItem;

import java.util.*;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * Editor for Young Android Blocks (.blk) files.
 *
 * TODO(sharon): blocks file loading and saving is not implemented yet!!
 *
 * @author lizlooney@google.com (Liz Looney)
 * @author sharon@google.com (Sharon Perl) added Blockly functionality
 */
public abstract class YaCodePageEditor extends SimpleEditor
    implements BlockDrawerSelectionListener {

  // A constant to substract from the total height of the Viewer window, set through
  // the computed height of the user's window (Window.getClientHeight())
  // This is an approximation of the size of the header navigation panel
  private static final int VIEWER_WINDOW_OFFSET = 170;

  // Database of component type descriptions
  private static final SimpleComponentDatabase COMPONENT_DATABASE =
      SimpleComponentDatabase.getInstance();

  // Keep a map from projectid_formname -> YaBlocksEditor for handling blocks workspace changed
  // callbacks from the BlocklyPanel objects. This has to be static because it is used by
  // static methods that are called from the Javascript Blockly world.
  private static final Map<String, YaCodePageEditor> nameToCodePageEditor = Maps.newHashMap();

  // projectid_formname for this blocks editor. Our index into the static nameToCodePageEditor map.
  private String fullName;

  protected final YACachedBlocksNode blocksNode;

  // References to other panels that we need to control.
  private final SourceStructureExplorer sourceStructureExplorer;

  protected final ComponentList components;
  private final Collection<YaSharedPageEditor> children;


  // Blocks area. Note that the blocks area is a part of the "document" in the
  // browser (via the deckPanel in the ProjectEditor). So if the document changes (which happens
  // when we switch projects) we will lose the blocks editor state, even though
  // YaBlocksEditor objects are kept around when switching projects. If we come
  // back to this blocks editor after having switched projects, the blocksArea
  // will get reinitialized.
  protected final BlocklyPanel blocksArea;

  // True once we've finished loading the current file.
  protected boolean loadComplete = false;

  // if selectedDrawer != null, it is either "component_" + instance name or
  // "builtin_" + drawer name
  protected String selectedDrawer = null;

  // Keep a list of components that we know about. Need this to detect when a call to add a
  // component is adding one that we already have (which can happen when a component gets
  // moved from one container to another). In that case we do not want to add it to the
  // blocks area again.
  private Set<String> componentUuids = new HashSet<String>();

  //Timer used to poll blocks editor to check if it is initialized
  private static Timer timer;

  protected YaCodePageEditor(YaProjectEditor projectEditor, YACachedBlocksNode blocksNode) {
    super(projectEditor, blocksNode.getRealNode());

    this.blocksNode = blocksNode;
    components = new ComponentList();
    //TODO (evan): make abstract method initComponents(components) and override in children

    children = new HashSet<YaSharedPageEditor>();
    addChildrenFromHeader();

    fullName = blocksNode.getProjectId() + "_" + blocksNode.getFormName();

    nameToCodePageEditor.put(fullName, this);
    blocksArea = new BlocklyPanel(fullName);
    blocksArea.setWidth("100%");
    // This code seems to be using a rather old layout, so we cannot simply pass 100% for height.
    // Instead, it needs to be calculated from the client's window, and a listener added to Window
    // We use VIEWER_WINDOW_OFFSET as an approximation of the size of the top navigation bar
    // New layouts don't need all this messing; see comments on selected answer at:
    // http://stackoverflow.com/questions/86901/creating-a-fluid-panel-in-gwt-to-fill-the-page
    blocksArea.setHeight(Window.getClientHeight() - VIEWER_WINDOW_OFFSET + "px");
    Window.addResizeHandler(new ResizeHandler() {
     public void onResize(ResizeEvent event) {
       int height = event.getHeight();
       blocksArea.setHeight(height - VIEWER_WINDOW_OFFSET + "px");
     }
    });
    initWidget(blocksArea);

    // Get references to the source structure explorer
    sourceStructureExplorer = BlockSelectorBox.getBlockSelectorBox().getSourceStructureExplorer();

    // Listen for selection events for built-in drawers
    BlockSelectorBox.getBlockSelectorBox().addBlockDrawerSelectionListener(this);

  }

  public static YaCodePageEditor getOrCreateEditor(YaProjectEditor project, YoungAndroidBlocksNode sourceNode) {
    String fullName = project.getProjectId() + "_"  + sourceNode.getFormName();
    YaCodePageEditor editor;
    if ((editor = nameToCodePageEditor.get(fullName)) != null) {
      return editor;
    }

    YACachedBlocksNode cachedNode =  YACachedBlocksNode.getOrCreateCachedNode(sourceNode);
    if (sourceNode instanceof YAFormPageBlocksNode) {
      return new YaFormPageEditor(project, cachedNode);
    } else {
      return new YaSharedPageEditor(project, cachedNode);
    }
  }

  public static YaCodePageEditor getCodePageEditorByFileName(long projectId, String fileName) {
    //FOR DEBUGGING PURPOSES ONLY!!!
    Project project = Ode.getInstance().getProjectManager().getProject(projectId);
    if (project == null) return null; //invalid projectId, project does not exist
    ProjectRootNode root = project.getRootNode();
    if (root == null) return null; //have not called project.loadNodes() yet

    YoungAndroidBlocksNode fileNode = null;
    for (ProjectNode potentialNode : root.getAllSourceNodes()) {
      if (potentialNode.getName().equals(fileName) && potentialNode instanceof YoungAndroidBlocksNode) {
        fileNode = (YoungAndroidBlocksNode)potentialNode;
      }
    }
    if (fileNode == null) return null; //either invalid projectName, or have not called project.loadNodes() yet
    ProjectEditor projeditor = Ode.getInstance().getEditorManager().openProject(root);
    return YaCodePageEditor.getOrCreateEditor((YaProjectEditor) projeditor, fileNode);

  }

  public static YaCodePageEditor getCodePageEditorByFileId(long projectId, String fileId) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectId);
    if (project == null) return null; //invalid projectId, project does not exist
    ProjectRootNode root = project.getRootNode();
    if (root == null) return null; //have not called project.loadNodes() yet

    ProjectNode node = root.getSourceNode(fileId);
    if (node instanceof YoungAndroidBlocksNode) {
      ProjectEditor projeditor = Ode.getInstance().getEditorManager().openProject(root);
      return YaCodePageEditor.getOrCreateEditor((YaProjectEditor) projeditor,
              (YoungAndroidBlocksNode) node);
    }
    return null;
  }

  private void addChildrenFromHeader() {
    blocksNode.load(new OdeAsyncCallback<ChecksumedLoadFile>() {
      @Override
      public void onSuccess(ChecksumedLoadFile result) {
        try {
          if (result.getContent() != "") {
            JsArray<Element> childrenXml = getChildrenFromHeader(textToDom(result.getContent()));
            Element childXml;
            for (int i = 0; i < childrenXml.length(); i++) {
              childXml = childrenXml.get(i);
              String projectIdString = childXml.getAttribute("projectId");
              if (projectIdString.length() == 0) { //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
                projectIdString = childXml.getAttribute("projectid");
              }
              long projectId = Long.parseLong(projectIdString);
              String fileId = childXml.getAttribute("fileId");
              if (fileId.length() == 0) { //TODO (evan): fix this. Silly hack because gwt for some reason makes the I in projectId lowercase
                fileId = childXml.getAttribute("fileid");
              }
              YaCodePageEditor child = getCodePageEditorByFileId(projectId, fileId);

              if (child == null) {
                //TODO (evan): child was deleted but is still in header
              } else if (child instanceof YaSharedPageEditor) {
                addChild((YaSharedPageEditor) child); //TODO (evan): get rid of this cast and instanceof
              } else {
                String message = "header child is not a shared page " +
                        "\n\t child == null " + (child == null) +
                        "\n\t result.getContent() " + result.getContent();
                throw new RuntimeException(message);
              }
            }
          }
        } catch (ChecksumedFileException e) {
          onFailure(e);
        }
      }
    });
  }

  public void addChild(YaSharedPageEditor child) {
    children.add(child);
  }

  public Collection<YaSharedPageEditor> getChildren() {
    return children;
  }

  private native JsArray<Element> getChildrenFromHeader(JavaScriptObject xml) /*-{
    return xml.querySelectorAll('header > children > child');
  }-*/;

  public boolean isFormPageEditor()  {
    return false;
  }

  //SimpleEditor methods
  @Override
  public boolean isLoadComplete() {
    return loadComplete;
  }

  @Override
  public void loadFile(final Command afterFileLoaded) {
    Element dom = blocklyXmlContainer();
    linkAndCompile(dom, new HashSet<YaCodePageEditor>(), 0, new Callback<Element>() {
      @Override
      public void call(Element dom) {
        blocksArea.setBlocksContent(domToText(dom));
        loadComplete = true;
        selectedDrawer = null;
        if (afterFileLoaded != null) {
          afterFileLoaded.execute();
        }
      }
    });
  }

  protected void linkAndCompile(Element dom, Set<YaCodePageEditor> visited, int depth, Callback<Element> onComplete) {
    Helper.println("linkAndCompile() " + Helper.editorDescriptor(this) + " num children:: " + children.size());
    if (visited.contains(this)) {
      return;
    }
    visited.add(this);
    CountDownCallback callback = new CountDownCallback(children.size() + 1, onComplete); //TODO (evan): this callback spagettii is messy. Re design
    addAndLabelAllBlocks(dom, depth, callback);
    for (YaSharedPageEditor child: children) {
      child.linkAndCompile(dom, visited, depth + 1, callback);
    }

  }

  private void addAndLabelAllBlocks(final Element dom, final int depth, final Callback<Element> onComplete) {
    blocksNode.load(new OdeAsyncCallback<ChecksumedLoadFile>() {
      @Override
      public void onSuccess(ChecksumedLoadFile result) {
        try {
          String content = result.getContent();
          JsArray<Element> blocks;
          if (content.equals("")) {
            blocks = JavaScriptObject.createArray().cast();
          } else {
            blocks = getTopLevelBlocks(textToDom(content));
          }

          labelBlocks(blocks, depth);
          addAllBlocks(dom, blocks);
          onComplete.call(dom);
        } catch (ChecksumedFileException e) {
          onFailure(e);
        }
      }
    });
  }

  private void labelBlocks(JsArray<Element> blocks, int depth) {
    //TODO (evan): when you add components, label each function with the version of it (ie what components it's using)
    for (int i = 0; i<blocks.length(); i++) {
      blocks.get(i).setAttribute("depth", depth +"");
    }
  }

  private void addAllBlocks(Element dom, JsArray<Element> blocks) {
    //traverse in reverse order because the size of the array elements delete themselves as you add them to dom
    for (int i = blocks.length() - 1; i>=0; i--) {
      Element block = blocks.get(i);
      dom.appendChild(block);
    }
  }

  private native JavaScriptObject filterOutImportedBlocks(Element e) /*-{
    return $wnd.exported.filterOutImportedBlocks(e);
  }-*/;

  protected native Element textToDom(String s) /*-{
    return $wnd.exported.textToDom(s);
  }-*/;

  protected native String domToText(Element xml) /*-{
    return $wnd.exported.domToText(xml);
  }-*/;

  private native JsArray<Element> getTopLevelBlocks(JavaScriptObject root) /*-{
    return $wnd.exported.getTopLevelBlocks(root);
  }-*/;

  private native Element blocklyXmlContainer() /*-{
    return $wnd.exported.blocklyXmlContainer();
  }-*/;

  private native void appendChildrenToParent(JsArray<Element> children, Element parent) /*-{
    return $wnd.exported.appendChildrenToParent(children, parent);
  }-*/;

  private native String evalJS(String s) /*-{
     return eval(s);
  }-*/;



  @Override
  public String getTabText() {
    return MESSAGES.blocksEditorTabName(blocksNode.getFormName());
  }

  @Override
  public void onShow() {
    OdeLog.log("YaBlocksEditor: got onShow() for " + getFileId());
    super.onShow();
    showWhenInitialized();
  }

  public void showWhenInitialized() {
    //check if blocks are initialized
    if(BlocklyPanel.blocksInited(fullName)) {
      updateBlocksTree(null);
      blocksArea.showDifferentForm(fullName);
      loadBlocksEditor();
      if (this instanceof YaFormPageEditor) {
        ((YaFormPageEditor) this).sendComponentData();  // Send Blockly the component information for generating Yail
      }
      blocksArea.renderBlockly(); //Re-render Blockly due to firefox bug
    } else {
      //timer calls this function again if the blocks are not initialized
      if(timer == null) {
        timer = new Timer() {
          public void run() {
            showWhenInitialized();
          }
        };
      }
      timer.schedule(200); // Run every 200 milliseconds
    }
  }

  /*
   * Updates the the whole designer: form, palette, source structure explorer, assets list, and
   * properties panel.
   */
  private void loadBlocksEditor() {
    //before you switch to a new blocks editor,
    //make sure all the pending changes from other shared pages that this editor might depend on are
    //saved and loaded into this editor
    Ode.getInstance().getEditorManager().saveDirtyEditors(null);
    loadFile(null);
    PaletteBox.getPaletteBox().setVisible(false);

      // Update the source structure explorer with the tree of this form's components.
      // start with no component selected in sourceStructureExplorer. We
      // don't want a component drawer open in the blocks editor when we
      // come back to it.
      updateBlocksTree(null);

      Ode.getInstance().getWorkColumns().remove(Ode.getInstance().getStructureAndAssets()
          .getWidget(2));
      Ode.getInstance().getWorkColumns().insert(Ode.getInstance().getStructureAndAssets(), 1);
      Ode.getInstance().getStructureAndAssets().insert(BlockSelectorBox.getBlockSelectorBox(), 0);
      BlockSelectorBox.getBlockSelectorBox().setVisible(true);
      AssetListBox.getAssetListBox().setVisible(true);
      hideComponentBlocks();
  }

  @Override
  public void onHide() {
    // When an editor is detached, if we are the "current" editor,
    // set the current editor to null and clean up the UI.
    // Note: I'm not sure it is possible that we would not be the "current"
    // editor when this is called, but we check just to be safe.
    OdeLog.log("YaBlocksEditor: got onHide() for " + getFileId());
    if (Ode.getInstance().getCurrentFileEditor() == this) {
      super.onHide();
      unloadBlocksEditor();
    } else {
      OdeLog.wlog("YaBlocksEditor.onHide: Not doing anything since we're not the "
          + "current file editor!");
    }
  }

  @Override
  public void onClose() {
    // our partner YaFormEditor added us as a FormChangeListener, but we remove ourself.
    BlockSelectorBox.getBlockSelectorBox().removeBlockDrawerSelectionListener(this);
    nameToCodePageEditor.remove(fullName);
  }

  public static void toggleWarning() {
    BlocklyPanel.switchWarningVisibility();
    for(Object formName : nameToCodePageEditor.keySet().toArray()){
      BlocklyPanel.toggleWarning((String) formName);
    }
  }

  private void unloadBlocksEditor() {
    // TODO(sharon): do something about form change listener?

    Ode.getInstance().getWorkColumns().remove(Ode.getInstance().getStructureAndAssets().getWidget(0));
    Ode.getInstance().getWorkColumns().insert(Ode.getInstance().getStructureAndAssets(), 3);
    Ode.getInstance().getStructureAndAssets().insert(BlockSelectorBox.getBlockSelectorBox(), 0);
    BlockSelectorBox.getBlockSelectorBox().setVisible(false);
    AssetListBox.getAssetListBox().setVisible(true);

    // Clear and hide the blocks selector tree
    sourceStructureExplorer.clearTree();
    hideComponentBlocks();
  }

  public static void onBlocksAreaChanged(String formName) {
    YaCodePageEditor editor = nameToCodePageEditor.get(formName);
    if (editor != null) {
      OdeLog.log("Got blocks area changed for " + formName);
      Ode.getInstance().getEditorManager().scheduleAutoSave(editor);
      if (editor instanceof YaFormPageEditor)
        ((YaFormPageEditor)editor).sendComponentData();
    }
  }

  protected void updateBlocksTree(SourceStructureExplorerItem itemToSelect) {
    TreeItem items[] = new TreeItem[3];
    items[0] = BlockSelectorBox.getBlockSelectorBox().getBuiltInBlocksTree();
    items[1] = getComponentsTree();
    items[2] = getAnyComponentsTree();
    sourceStructureExplorer.updateTree(items, itemToSelect);
    BlockSelectorBox.getBlockSelectorBox().setContent(sourceStructureExplorer);
  }

  protected abstract TreeItem getComponentsTree();

  private TreeItem getAnyComponentsTree() {
    return BlockSelectorBox.getBlockSelectorBox().
            getGenericComponentsTree(new ComponentList(ComponentList.flatten(components.getComponents())));
  }



  // Do whatever is needed to save Blockly state when our project is about to be
  // detached from the parent document. Note that this is not for saving the blocks file itself.
  // We use EditorManager.scheduleAutoSave for that.
  public void prepareForUnload() {
    blocksArea.saveComponentsAndBlocks();
  }

  @Override
  public String getRawFileContent() {
    String content = blocksArea.getBlocksContent();
    content = content.equals("") ? domToText(blocklyXmlContainer())  : content;
    return domToText(setChildrenHeader(
            filterOutImportedBlocks(textToDom(content)), makeChildrenXmlArray(children)));
  }


  private JsArray<Element> makeChildrenXmlArray(Collection<YaSharedPageEditor> children) {
    JsArray<Element> childrenXmlArr = JavaScriptObject.createArray().cast();
    for (YaSharedPageEditor child: children) {
      Element xmlchild = createElement("child"); //hack because I can't figure out to make an Element
      xmlchild.setAttribute("projectId", child.getProjectId()+"");
      xmlchild.setAttribute("fileId", child.getFileId());
      childrenXmlArr.push(xmlchild);
    }
    return childrenXmlArr;
  }

  private native Element createElement(String name) /*-{
    return document.createElement(name);
  }-*/;
  private native Element setChildrenHeader(JavaScriptObject xml, JsArray<Element> children) /*-{
    return $wnd.exported.setChildrenHeader(xml, children);
  }-*/;
  @Override
  public void onSave() {
    // Nothing to do after blocks are saved.
  }

  public static String getComponentInfo(String typeName) {
    return COMPONENT_DATABASE.getTypeDescription(typeName);
  }

  public static String getComponentsJSONString() {
    return COMPONENT_DATABASE.getComponentsJSONString();
  }

  public static String getComponentInstanceTypeName(String formName, String instanceName) {
      //use form name to get blocks editor
      ComponentList componentsForForm = nameToCodePageEditor.get(formName).components;
      return componentsForForm.getComponentByName(instanceName).getType();
  }

  public void addComponent(MockComponent comp) {
    if (componentUuids.add(comp.getUuid())) {
      components.addComponent(comp);
      blocksArea.addComponent(comp);
    }
    //recursivley add all children elements
    for (MockComponent child: comp.getChildren()) {
      addComponent(child);
    }
  }

  public void removeComponent(MockComponent component) {
    if (componentUuids.remove(component.getUuid())) {
      blocksArea.removeComponent(component);
      //TODO (evan): components.remove(typeName, instanceName, uuid)
    }
  }

  public void renameComponent(String oldName, MockComponent comp) {
    blocksArea.renameComponent(oldName, comp);
  }

  public void showComponentBlocks(String instanceName) {
    String instanceDrawer = "component_" + instanceName;
    if (selectedDrawer == null || !blocksArea.drawerShowing()
        || !selectedDrawer.equals(instanceDrawer)) {
      blocksArea.showComponentBlocks(instanceName);
      selectedDrawer = instanceDrawer;
    } else {
      blocksArea.hideComponentBlocks();
      selectedDrawer = null;
    }
  }

  public void hideComponentBlocks() {
    blocksArea.hideComponentBlocks();
    selectedDrawer = null;
  }

  public void showBuiltinBlocks(String drawerName) {
    OdeLog.log("Showing built-in drawer " + drawerName);
    String builtinDrawer = "builtin_" + drawerName;
    if (selectedDrawer == null || !blocksArea.drawerShowing()
        || !selectedDrawer.equals(builtinDrawer)) {
      blocksArea.showBuiltinBlocks(drawerName);
      selectedDrawer = builtinDrawer;
    } else {
      blocksArea.hideBuiltinBlocks();
      selectedDrawer = null;
    }
  }

  public void showGenericBlocks(String drawerName) {
    OdeLog.log("Showing generic drawer " + drawerName);
    String genericDrawer = "generic_" + drawerName;
    if (selectedDrawer == null || !blocksArea.drawerShowing()
        || !selectedDrawer.equals(genericDrawer)) {
      blocksArea.showGenericBlocks(drawerName);
      selectedDrawer = genericDrawer;
    } else {
      blocksArea.hideGenericBlocks();
      selectedDrawer = null;
    }
  }

  public void hideBuiltinBlocks() {
    blocksArea.hideBuiltinBlocks();
  }

  protected abstract void updateSourceStructureExplorer();

  // BlockDrawerSelectionListener implementation

  /*
   * @see com.google.appinventor.client.editor.youngandroid.BlockDrawerSelectionListener#
   * onBlockDrawerSelected(java.lang.String)
   */
  @Override
  public void onBuiltinDrawerSelected(String drawerName) {
    // Only do something if we are the current file editor
    if (Ode.getInstance().getCurrentFileEditor() == this) {
      showBuiltinBlocks(drawerName);
    }
  }

  /*
   * @see com.google.appinventor.client.editor.youngandroid.BlockDrawerSelectionListener#
   * onBlockDrawerSelected(java.lang.String)
   */
  @Override
  public void onGenericDrawerSelected(String drawerName) {
    // Only do something if we are the current file editor
    if (Ode.getInstance().getCurrentFileEditor() == this) {
      showGenericBlocks(drawerName);
    }
  }

  /*
   * Start up the Repl (call into the Blockly.ReplMgr via the BlocklyPanel.
   */
  @Override
  public void startRepl(boolean alreadyRunning, boolean forEmulator, boolean forUsb) {
    blocksArea.startRepl(alreadyRunning, forEmulator, forUsb);
  }

  /*
   * Perform a Hard Reset of the Emulator
   */
  public void hardReset() {
    blocksArea.hardReset();
  }

  // Static Function. Find the associated editor for formName and
  // set its "damaged" bit. This will cause the editor manager's scheduleAutoSave
  // method to ignore this blocks file and not save it out.

  public static void setBlocksDamaged(String formName) {
    YaCodePageEditor editor = nameToCodePageEditor.get(formName);
    if (editor != null) {
      editor.setDamaged(true);
    }
  }

  public String getFileName() {
    return blocksNode.getFormName();
  }

  public String getName() {
    //TODO(evan): get name from header instead of random
    return blocksNode.getName();
  }
  /*
   * Switch language to the specified language if applicable
   */
  @Override
  public void switchLanguage(String newLanguage) {
    blocksArea.switchLanguage(newLanguage);
  }

  // --- FOR DEBUGGING PURPOSES ONLY ---
  public String getCachedContent() {
    return blocksNode.getContent();
  }
}

