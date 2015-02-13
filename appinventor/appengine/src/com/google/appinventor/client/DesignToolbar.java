// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client;

import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.simple.components.MockComponent;
import com.google.appinventor.client.editor.youngandroid.BlocklyPanel;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.editor.youngandroid.YaFormEditor;
import com.google.appinventor.client.editor.youngandroid.YaSharedPageEditor;
import com.google.appinventor.client.explorer.commands.AddFormCommand;
import com.google.appinventor.client.explorer.commands.AddSharedPageCommand;
import com.google.appinventor.client.explorer.commands.ChainableCommand;
import com.google.appinventor.client.explorer.commands.DeleteFileCommand;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.client.widgets.DropDownButton.DropDownItem;
import com.google.appinventor.client.widgets.Toolbar;
import com.google.appinventor.common.version.AppInventorFeatures;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidSourceNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * The design toolbar houses command buttons in the Young Android Design
 * tab (for the UI designer (a.k.a, Form Editor) and Blocks Editor).
 *
 */
public class DesignToolbar extends Toolbar {

  /*
   * A Screen is either a (form editor, blocks editor) tuple (if blocksEditor is a formpage)
   * or a blocksEditor singleton (if blocksEditor is a sharedpage). This is for the
   * application screen. Name is the name of the screen (form) displayed
   * in the screen's pull-down.
   */
  public static class Screen {
    public final String screenName;
    public final YaFormEditor formEditor;
    public final YaCodePageEditor blocksEditor;

    public Screen(String name, YaFormEditor formEditor, YaCodePageEditor blocksEditor) {
      this.screenName = name;
      this.formEditor = formEditor;
      this.blocksEditor = blocksEditor;
    }

    public Screen(String name, YaCodePageEditor blocksEditor) {
      this.screenName = name;
      this.formEditor = null;
      this.blocksEditor = blocksEditor;
    }
  }

  /*
   * A project as represented in the DesignToolbar. Each project has a name
   * (as displayed in the DesignToolbar on the left), a set of named screens,
   * and an indication of which screen is currently being edited.
   */
  public static class DesignProject {
    public final String name;
    public final Map<String, Screen> screens; // screen name -> Screen
    public String currentScreen; // name of currently displayed screen

    public DesignProject(String name, long projectId) {
      this.name = name;
      screens = Maps.newHashMap();
      // Screen1 is initial screen by default
      currentScreen = YoungAndroidSourceNode.SCREEN1_FORM_NAME;
      // Let BlocklyPanel know which screen to send Yail for
      BlocklyPanel.setCurrentForm(projectId + "_" + currentScreen);
    }

    // Returns true if we added the screen (it didn't previously exist), false otherwise.
    public boolean addScreen(Screen screen) {
      if (!screens.containsKey(screen.screenName)) {
        screens.put(screen.screenName, screen);
        return true;
      } else {
        return false;
      }
    }

    public void removeScreen(String name) {
      screens.remove(name);
    }

    public void setCurrentScreen(String name) {
      currentScreen = name;
    }
  }

  private static final String WIDGET_NAME_ADDFORM = "AddForm";
  private static final String WIDGET_NAME_REMOVEFORM = "RemoveForm";
  private static final String WIDGET_NAME_SCREENS_DROPDOWN = "ScreensDropdown";
  private static final String WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR = "SwitchToBlocksEditor";
  private static final String WIDGET_NAME_SWITCH_TO_FORM_EDITOR = "SwitchToFormEditor";
  private static final String WIDGET_NAME_OPEN_SHARED_PAGES_OVERLAY = "OpenSharedPagesOverlay";

  // Switch language
  private static final String WIDGET_NAME_SWITCH_LANGUAGE = "Language";
  private static final String WIDGET_NAME_SWITCH_LANGUAGE_ENGLISH = "English";
  private static final String WIDGET_NAME_SWITCH_LANGUAGE_CHINESE_CN = "Simplified Chinese";
  //private static final String WIDGET_NAME_SWITCH_LANGUAGE_GERMAN = "German";
  //private static final String WIDGET_NAME_SWITCH_LANGUAGE_VIETNAMESE = "Vietnamese";

  // Enum for type of view showing in the design tab
  public enum View {
    FORM,   // Form editor view
    BLOCKS  // Blocks editor view
  }
  public View currentView = View.FORM;

  public Label projectNameLabel;

  // Project currently displayed in designer
  private DesignProject currentProject;

  // Map of project id to project info for all projects we've ever shown
  // in the Designer in this session.
  public Map<Long, DesignProject> projectMap = Maps.newHashMap();

  // Stack of screens switched to from the Companion
  // We implement screen switching in the Companion by having it tell us
  // to switch screens. We then load into the companion the new Screen
  // We save where we were because the companion can have us return from
  // a screen. If we switch projects in the browser UI, we clear this
  // list of screens as we are effectively running a different application
  // on the device.
  public static LinkedList<String> pushedScreens = Lists.newLinkedList();

  /**
   * Initializes and assembles all commands into buttons in the toolbar.
   */
  public DesignToolbar() {
    super();

    projectNameLabel = new Label();
    projectNameLabel.setStyleName("ya-ProjectName");
    HorizontalPanel toolbar = (HorizontalPanel) getWidget();
    toolbar.insert(projectNameLabel, 0);

    // width of palette minus cellspacing/border of buttons
    toolbar.setCellWidth(projectNameLabel, "222px");

    List<DropDownItem> screenItems = Lists.newArrayList();
    addDropDownButton(WIDGET_NAME_SCREENS_DROPDOWN, MESSAGES.screensButton(), screenItems);

    if (AppInventorFeatures.allowMultiScreenApplications()) {
      addButton(new ToolbarItem(WIDGET_NAME_ADDFORM, MESSAGES.addFormButton(),
          new AddFormAction()));
      addButton(new ToolbarItem(WIDGET_NAME_REMOVEFORM, MESSAGES.removeFormButton(),
          new RemoveFormAction()));
    }

    addButton(new ToolbarItem(WIDGET_NAME_SWITCH_TO_FORM_EDITOR,
        MESSAGES.switchToFormEditorButton(), new SwitchToFormEditorAction()), true);
    addButton(new ToolbarItem(WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR,
        MESSAGES.switchToBlocksEditorButton(), new SwitchToBlocksEditorAction()), true);
    addButton(new ToolbarItem(WIDGET_NAME_OPEN_SHARED_PAGES_OVERLAY,
            MESSAGES.openSharedPagesOverlay(), new OpenSharedPagesOverlay()), true);

    // Gray out the Designer button and enable the blocks button
    toggleEditor(false);
    Ode.getInstance().getTopToolbar().updateFileMenuButtons(0);
  }

  private class AddFormAction implements Command {
    @Override
    public void execute() {
      Ode ode = Ode.getInstance();
      if (ode.screensLocked()) {
        return;                 // Don't permit this if we are locked out (saving files)
      }
      ProjectRootNode projectRootNode = ode.getCurrentYoungAndroidProjectRootNode();
      if (projectRootNode != null) {
        ChainableCommand cmd = new AddFormCommand();
        cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADDFORM_YA, projectRootNode);
      }
    }
  }

  private class RemoveFormAction implements Command {
    @Override
    public void execute() {
      Ode ode = Ode.getInstance();
      if (ode.screensLocked()) {
        return;                 // Don't permit this if we are locked out (saving files)
      }
      YoungAndroidSourceNode sourceNode = ode.getCurrentYoungAndroidSourceNode();
      if (sourceNode != null && !sourceNode.isScreen1()) {
        // DeleteFileCommand handles the whole operation, including displaying the confirmation
        // message dialog, closing the form editor and the blocks editor,
        // deleting the files in the server's storage, and deleting the
        // corresponding client-side nodes (which will ultimately trigger the
        // screen deletion in the DesignToolbar).
        final String deleteConfirmationMessage = MESSAGES.reallyDeleteForm(
            sourceNode.getFormName());
        ChainableCommand cmd = new DeleteFileCommand() {
          @Override
          protected boolean deleteConfirmation() {
            return Window.confirm(deleteConfirmationMessage);
          }
        };
        cmd.startExecuteChain(Tracking.PROJECT_ACTION_REMOVEFORM_YA, sourceNode);
      }
    }
  }

  private class SwitchScreenAction implements Command {
    private final long projectId;
    private final String name;  // screen name

    public SwitchScreenAction(long projectId, String screenName) {
      this.projectId = projectId;
      this.name = screenName;
    }

    @Override
    public void execute() {
      doSwitchScreen(projectId, name, currentView);
    }
  }

  private void doSwitchScreen(final long projectId, final String screenName, final View view) {
    Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          if (Ode.getInstance().screensLocked()) { // Wait until I/O complete
            Scheduler.get().scheduleDeferred(this);
          } else {
            doSwitchScreen1(projectId, screenName, view);
          }
        }
      });
  }

  private void doSwitchScreen1(long projectId, String screenName, View view) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar: no project with id " + projectId
          + ". Ignoring SwitchScreenAction.execute().");
      return;
    }
    DesignProject project = projectMap.get(projectId);
    if (currentProject != project) {
      // need to switch projects first. this will not switch screens.
      if (!switchToProject(projectId, project.name)) {
        return;
      }
    }
    String newScreenName = screenName;
    if (!currentProject.screens.containsKey(newScreenName)) {
      // Can't find the requested screen in this project. This shouldn't happen, but if it does
      // for some reason, try switching to Screen1 instead.
      OdeLog.wlog("Trying to switch to non-existent screen " + newScreenName +
          " in project " + currentProject.name + ". Trying Screen1 instead.");
      if (currentProject.screens.containsKey(YoungAndroidSourceNode.SCREEN1_FORM_NAME)) {
        newScreenName = YoungAndroidSourceNode.SCREEN1_FORM_NAME;
      } else {
        // something went seriously wrong!
        ErrorReporter.reportError("Something is wrong. Can't find Screen1 for project "
            + currentProject.name);
        return;
      }
    }
    currentView = view;
    Screen screen = currentProject.screens.get(newScreenName);
    ProjectEditor projectEditor = screen.blocksEditor.getProjectEditor();
    currentProject.setCurrentScreen(newScreenName);
    setDropDownButtonCaption(WIDGET_NAME_SCREENS_DROPDOWN, newScreenName);
    OdeLog.log("Setting currentScreen to " + newScreenName);
    if (currentView == View.FORM && screen.formEditor != null) {
      projectEditor.selectFileEditor(screen.formEditor);
      toggleEditor(false);
      Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
    } else {
      projectEditor.selectFileEditor(screen.blocksEditor);
      toggleEditor(true);
      Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
    }
    // Inform the Blockly Panel which project/screen (aka form) we are working on
    BlocklyPanel.setCurrentForm(projectId + "_" + newScreenName);
  }

  private class SwitchToBlocksEditorAction implements Command {
    @Override
    public void execute() {
      if (currentProject == null) {
        OdeLog.wlog("DesignToolbar.currentProject is null. "
            + "Ignoring SwitchToBlocksEditorAction.execute().");
        return;
      }
      if (currentView != View.BLOCKS) {
        long projectId = Ode.getInstance().getCurrentYoungAndroidProjectRootNode().getProjectId();
        switchToScreen(projectId, currentProject.currentScreen, View.BLOCKS);
        toggleEditor(true);       // Gray out the blocks button and enable the designer button
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    }
  }

  private class OpenSharedPagesOverlay implements Command {
    @Override
    public void execute() {
      exportMethods();
      openOverlay();
    }

    private void newSharedPage() { //called from javascript
      ProjectRootNode rootNode = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
      ChainableCommand cmd = new AddSharedPageCommand();
      cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADD_SHARED_PAGE_YA, rootNode);
    }

    private JSONObject pageDescriptor(YaCodePageEditor editor) {
      JSONObject json = new JSONObject();
      String name = editor.getFileName();
      if (name.endsWith(YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION)) {
        name = name.substring(0, name.length() - YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION.length());
      }
      json.put("name", new JSONString(name));
      json.put("projectId", new JSONNumber((double)editor.getProjectId()));
      json.put("fileName", new JSONString(editor.getFileName()));

      JSONArray componentArr = new JSONArray();
      for (String componentName: editor.getComponents().keySet()) {
        MockComponent component = editor.getComponents().get(componentName);
        JSONObject jsonComponent = new JSONObject();
        jsonComponent.put("name", new JSONString(component.getName()));
        jsonComponent.put("type", new JSONString(component.getType()));
        jsonComponent.put("iconUrl", new JSONString(component.getIconImage().getUrl()));
        componentArr.set(componentArr.size(), jsonComponent);
      }

      json.put("components", componentArr);

      JSONArray childrenArr = new JSONArray();
      for (YaSharedPageEditor child : editor.getChildren()) {
        JSONObject jsonComponent = new JSONObject();
        jsonComponent.put("projectId", new JSONNumber(child.getProjectId()));
        jsonComponent.put("fileName", new JSONString(child.getFileName()));
        childrenArr.set(childrenArr.size(), jsonComponent);
      }
      json.put("children", childrenArr);
      return json;
    }

    private JavaScriptObject getProjectPages() { //called from javascript
      JSONObject json = new JSONObject();
      YaCodePageEditor currentPage = currentProject.screens.get(currentProject.currentScreen).blocksEditor;
      json.put("currentPage",  pageDescriptor(currentPage));

      JSONArray formPages = new JSONArray();
      JSONArray sharedPages = new JSONArray();
      YaCodePageEditor page;
      for (String name: currentProject.screens.keySet()) {
        page = currentProject.screens.get(name).blocksEditor;
        if (page.isFormPageEditor()) {
          formPages.set(formPages.size(), pageDescriptor(page));
        } else {
          sharedPages.set(sharedPages.size(), pageDescriptor(page));
        }
      }

      json.put("formPages", formPages);
      json.put("sharedPages", sharedPages);

      return json.getJavaScriptObject();
    }

    //returns whether the child can be imported
    private boolean importNewPage(JavaScriptObject parentObj, JavaScriptObject childObj) { //called from javascript
      JSONObject jsonParent = new JSONObject(parentObj);
      JSONObject jsonChild = new JSONObject(childObj);

      YaCodePageEditor parent = YaCodePageEditor.getCodePageEditor(
              (long)jsonParent.get("projectId").isNumber().doubleValue(),
              jsonParent.get("fileName").isString().stringValue());

      YaCodePageEditor child = YaCodePageEditor.getCodePageEditor(
              (long) jsonChild.get("projectId").isNumber().doubleValue(),
              jsonChild.get("fileName").isString().stringValue());

      if (!child.isFormPageEditor()) {
        parent.addChild((YaSharedPageEditor)child); //TODO (evan): get rid of this cast
        Ode.getInstance().getEditorManager().scheduleAutoSave(parent);
        return true;
      } else {
        alert("child must be a shared page");
        return false;
      }
    }

    private native void alert(String msg) /*-{
      $wnd.alert(msg);
    }-*/;

    public native void exportMethods() /*-{
      var that = this;
      $wnd.exported.newSharedPage = $entry(function() {
        return that.@com.google.appinventor.client.DesignToolbar$OpenSharedPagesOverlay::newSharedPage()();
       });
      $wnd.exported.getProjectPages = $entry(function() {
        return that.@com.google.appinventor.client.DesignToolbar$OpenSharedPagesOverlay::getProjectPages()();
      });
      $wnd.exported.importNewPage = $entry(function(parent, child) {
        return that.@com.google.appinventor.client.DesignToolbar$OpenSharedPagesOverlay::importNewPage(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(parent, child);
      });
    }-*/;

    public native void openOverlay() /*-{
     $wnd.exported.openSharedPagesOverlay();
    }-*/;
  }

  private class SwitchToFormEditorAction implements Command {
    @Override
    public void execute() {
      if (currentProject == null) {
        OdeLog.wlog("DesignToolbar.currentProject is null. "
            + "Ignoring SwitchToFormEditorAction.execute().");
        return;
      }
      if (currentView != View.FORM) {
        long projectId = Ode.getInstance().getCurrentYoungAndroidProjectRootNode().getProjectId();
        switchToScreen(projectId, currentProject.currentScreen, View.FORM);
        toggleEditor(false);      // Gray out the Designer button and enable the blocks button
        Ode.getInstance().getTopToolbar().updateFileMenuButtons(1);
      }
    }
  }

  public void addProject(long projectId, String projectName) {
    if (!projectMap.containsKey(projectId)) {
      projectMap.put(projectId, new DesignProject(projectName, projectId));
      OdeLog.log("DesignToolbar added project " + projectName + " with id " + projectId);
    } else {
      OdeLog.wlog("DesignToolbar ignoring addProject for existing project " + projectName
          + " with id " + projectId);
    }
  }

  // Switch to an existing project. Note that this does not switch screens.
  // TODO(sharon): it might be better to throw an exception if the
  // project doesn't exist.
  private boolean switchToProject(long projectId, String projectName) {
    if (projectMap.containsKey(projectId)) {
      DesignProject project = projectMap.get(projectId);
      if (project == currentProject) {
        OdeLog.wlog("DesignToolbar: ignoring call to switchToProject for current project");
        return true;
      }
      pushedScreens.clear();    // Effectively switching applications clear stack of screens
      clearDropDownMenu(WIDGET_NAME_SCREENS_DROPDOWN);
      OdeLog.log("DesignToolbar: switching to existing project " + projectName + " with id "
          + projectId);
      currentProject = projectMap.get(projectId);
      // TODO(sharon): add screens to drop-down menu in the right order
      for (Screen screen : currentProject.screens.values()) {
        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(screen.screenName,
            screen.screenName, new SwitchScreenAction(projectId, screen.screenName)));
      }
      projectNameLabel.setText(projectName);
    } else {
      ErrorReporter.reportError("Design toolbar doesn't know about project " + projectName +
          " with id " + projectId);
      OdeLog.wlog("Design toolbar doesn't know about project " + projectName + " with id "
          + projectId);
      return false;
    }
    return true;
  }

  /*
   * Add a screen name to the drop-down for the project with id projectId.
   * name is the form name, formEditor is the file editor for the form UI,
   * and blocksEditor is the file editor for the form's blocks.
   */
  public void addScreen(long projectId, Screen screen) {

    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + screen.screenName + " with id " + projectId
          + ". Ignoring addScreen().");
      return;
    }
    DesignProject project = projectMap.get(projectId);
    if (project.addScreen(screen)) {
      if (currentProject == project) {

        addDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, new DropDownItem(screen.screenName,
            screen.screenName, new SwitchScreenAction(projectId, screen.screenName)));
      }
    }

  }

/*
 * PushScreen -- Static method called by Blockly when the Companion requests
 * That we switch to a new screen. We keep track of the Screen we were on
 * and push that onto a stack of Screens which we pop when requested by the
 * Companion.
 */
  public static boolean pushScreen(String screenName) {
    DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
    long projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    String currentScreen = designToolbar.currentProject.currentScreen;
    if (!designToolbar.currentProject.screens.containsKey(screenName)) // No such screen -- can happen
      return false;                                                    // because screen is user entered here.
    pushedScreens.addFirst(currentScreen);
    designToolbar.doSwitchScreen(projectId, screenName, View.BLOCKS);
    return true;
  }

  public static void popScreen() {
    DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
    long projectId = Ode.getInstance().getCurrentYoungAndroidProjectId();
    String newScreen;
    if (pushedScreens.isEmpty()) {
      return;                   // Nothing to do really
    }
    newScreen = pushedScreens.removeFirst();
    designToolbar.doSwitchScreen(projectId, newScreen, View.BLOCKS);
  }

  // Called from Javascript when Companion is disconnected
  public static void clearScreens() {
    pushedScreens.clear();
  }

  /*
   * Switch to screen name in project projectId. Also switches projects if
   * necessary.
   */
  public void switchToScreen(long projectId, String screenName, View view) {
    doSwitchScreen(projectId, screenName, view);
  }

  /*
   * Remove screen name (if it exists) from project projectId
   */
  public void removeScreen(long projectId, String name) {
    if (!projectMap.containsKey(projectId)) {
      OdeLog.wlog("DesignToolbar can't find project " + name + " with id " + projectId
          + " Ignoring removeScreen().");
      return;
    }
    OdeLog.log("DesignToolbar: got removeScreen for project " + projectId
        + ", screen " + name);
    DesignProject project = projectMap.get(projectId);
    if (!project.screens.containsKey(name)) {
      // already removed this screen
      return;
    }
    if (currentProject == project) {
      // if removing current screen, choose a new screen to show
      if (currentProject.currentScreen.equals(name)) {
        // TODO(sharon): maybe make a better choice than screen1, but for now
        // switch to screen1 because we know it is always there
        switchToScreen(projectId, YoungAndroidSourceNode.SCREEN1_FORM_NAME, View.FORM);
      }
      removeDropDownButtonItem(WIDGET_NAME_SCREENS_DROPDOWN, name);
    }
    project.removeScreen(name);
  }

  private void toggleEditor(boolean blocks) {
    setButtonEnabled(WIDGET_NAME_SWITCH_TO_BLOCKS_EDITOR, !blocks);
    setButtonEnabled(WIDGET_NAME_SWITCH_TO_FORM_EDITOR, blocks);

    if (getCurrentProject() == null || getCurrentProject().currentScreen == "Screen1") {
      setButtonEnabled(WIDGET_NAME_REMOVEFORM, false);
    } else {
      setButtonEnabled(WIDGET_NAME_REMOVEFORM, true);
    }
  }

  public DesignProject getCurrentProject() {
    return currentProject;
  }
}
