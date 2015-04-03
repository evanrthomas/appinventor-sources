
package com.google.appinventor.client;

import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.editor.youngandroid.YaSharedPageEditor;
import com.google.appinventor.client.explorer.commands.AddSharedPageCommand;
import com.google.appinventor.client.explorer.commands.ChainableCommand;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.explorer.project.ProjectChangeAdapter;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.CountDownCallback;
import com.google.appinventor.client.helper.Helper;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SharedPagesOverlay {
  private static SharedPagesOverlay INSTANCE = new SharedPagesOverlay();

  private SharedPagesOverlay() {}

  public static SharedPagesOverlay getInstance() {
    return INSTANCE;
  }

  private void newSharedPage(JavaScriptObject onSuccess, JavaScriptObject onFail) { //called from javascript
    ProjectRootNode rootNode = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
    ChainableCommand cmd = new AddSharedPageCommand(onSuccess, onFail);
    cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADD_SHARED_PAGE_YA, rootNode);
  }

  private void getEachBookWhenLoaded(final JavaScriptObject callback) { //called from javascript
    Collection<Project> books = Ode.getInstance().getProjectManager().getLibrary();
    for (final Project book : books) {
      onLoadProjectNodes(book, new Callback<Project>() {
        @Override
        public void call(Project book) {
          JSONObject bookjson = new JSONObject();
          bookjson.put("name", new JSONString(book.getProjectName()));
          JSONArray pages = new JSONArray();

          for (ProjectNode page : book.getRootNode().getAllSourceNodes()) {
            if (!YoungAndroidSourceAnalyzer.isBlocksNodeSourceFileId(page.getFileId())) {
              continue;
            }
            JSONObject pagejson = new JSONObject();
            pages.set(pages.size(), pageDescriptor((YoungAndroidBlocksNode) page));
          }
          bookjson.put("pages", pages);
          Helper.callJSFunc(callback, bookjson.getJavaScriptObject());

        }
      });

    }


  }

  private void onLoadProjectNodes(final Project project, final Callback<Project> callback) {

    ProjectRootNode root = project.getRootNode();
    if (root == null) {

      project.addProjectChangeListener(new ProjectChangeAdapter() {
        @Override
        public void onProjectLoaded(Project projectLoaded) {
          project.removeProjectChangeListener(this);
          onLoadProjectNodes(project, callback);
        }
      });
      project.loadProjectNodes();

    } else {
      callback.call(project);

    }
  }

  public static JSONObject pageDescriptor(YoungAndroidBlocksNode node) {
    YaCodePageEditor editor =
            YaCodePageEditor.getCodePageEditorByFileId(node.getProjectId(), node.getFileId());
    //I don't think it should be possible for editor to be null if node exists.
    if (editor == null) throw new RuntimeException("somehow, editor for existing YABlocksNode is null");
    return pageDescriptor(editor);
  }

  public static JSONObject pageDescriptor(YaCodePageEditor editor) {
    JSONObject jsonBlob = new JSONObject();
    String name = editor.getFileName();
    if (name.endsWith(YoungAndroidSourceAnalyzer.FORM_PAGE_SOURCE_EXTENSION)) {
      name = name.substring(0, name.length() - YoungAndroidSourceAnalyzer.FORM_PAGE_SOURCE_EXTENSION.length());
    }
    jsonBlob.put("name", new JSONString(name));
    jsonBlob.put("projectId", new JSONNumber((double) editor.getProjectId()));
    jsonBlob.put("projectName", new JSONString(editor.getProjectRootNode().getName()));
    jsonBlob.put("fileName", new JSONString(editor.getFileName()));
    jsonBlob.put("fileId", new JSONString(editor.getFileId()));
    jsonBlob.put("type", editor.isFormPageEditor() ?
            new JSONString("formPage") :
            new JSONString("sharedPage"));

    JSONArray children = new JSONArray();
    for (YaSharedPageEditor child : editor.getChildren()) {
      JSONObject childBlob = new JSONObject();
      childBlob.put("projectId", new JSONNumber(child.getProjectId()));
      childBlob.put("fileName", new JSONString(child.getFileName()));
      childBlob.put("fileId", new JSONString(child.getFileId()));
      children.set(children.size(), childBlob);
    }
    jsonBlob.put("children", children);
    return jsonBlob;
  }

  private JavaScriptObject getProjectPages() { //called from javascript
    JSONObject json = new JSONObject();
    DesignToolbar.DesignProject currentProject = Ode.getInstance().getDesignToolbar().getCurrentProject();
    YaCodePageEditor currentPage = currentProject.screens.get(currentProject.currentScreen).blocksEditor;
    json.put("currentPage", pageDescriptor(currentPage));

    JSONArray formPages = new JSONArray();
    JSONArray sharedPages = new JSONArray();
    Set<YaCodePageEditor> added = new HashSet<YaCodePageEditor>();
    YaCodePageEditor page;
    for (String name : currentProject.screens.keySet()) {
      page = currentProject.screens.get(name).blocksEditor;
      added.add(page);
      if (page.isFormPageEditor()) {
        formPages.set(formPages.size(), pageDescriptor(page));
      } else {
        sharedPages.set(sharedPages.size(), pageDescriptor(page));
      }
    }
    Collection<YaSharedPageEditor> children = currentPage.getChildren();
    for (YaSharedPageEditor child : children) {
      if (!added.contains(child)) { //add all things from library
        sharedPages.set(sharedPages.size(), pageDescriptor(child));
      }
    }

    json.put("formPages", formPages);
    json.put("sharedPages", sharedPages);

    return json.getJavaScriptObject();
  }

  private void importNewPage(final JavaScriptObject parentObj, final JavaScriptObject childObj,
                             final JavaScriptObject onSuccess, final JavaScriptObject onFail) { //called from javascript
    final JSONObject jsonParent = new JSONObject(parentObj);
    final JSONObject jsonChild = new JSONObject(childObj);

    CountDownCallback<Project> addChildToParent =
            new CountDownCallback<Project>(2, new Callback<Project>() {
              @Override
              public void call(Project proj) {
                YaCodePageEditor parent = YaCodePageEditor.getCodePageEditorByFileId(
                        (long) jsonParent.get("projectId").isNumber().doubleValue(),
                        jsonParent.get("fileId").isString().stringValue());

                YaCodePageEditor child = YaCodePageEditor.getCodePageEditorByFileId(
                        (long) jsonChild.get("projectId").isNumber().doubleValue(),
                        jsonChild.get("fileId").isString().stringValue());


                if (child instanceof YaSharedPageEditor) {
                  parent.addChild((YaSharedPageEditor) child); //TODO (evan): get rid of this cast
                  Helper.callJSFunc(onSuccess, null);
                } else {
                  Helper.callJSFunc(onFail, Helper.eval("\"child must be a shared page\""));
                }
              }

            });

    long parentProjectId = Long.parseLong(jsonParent.get("projectId").toString());
    Project parentproj = Ode.getInstance().getProjectManager().getProject(parentProjectId);
    onLoadProjectNodes(parentproj, addChildToParent);

    long childProjectId = Long.parseLong(jsonChild.get("projectId").toString());
    Project childproj = Ode.getInstance().getProjectManager().getProject(childProjectId);
    onLoadProjectNodes(childproj, addChildToParent);
  }


  public native void exportMethods() /*-{
      var that = this;
      $wnd.exported.newSharedPage = $entry(function(onSuccess, onFail) {
        return that.@com.google.appinventor.client.SharedPagesOverlay::newSharedPage(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(onSuccess, onFail);
       });

      $wnd.exported.getEachBookWhenLoaded = $entry(function(callback) {
        return that.@com.google.appinventor.client.SharedPagesOverlay::getEachBookWhenLoaded(Lcom/google/gwt/core/client/JavaScriptObject;)(callback);
       });

      $wnd.exported.getProjectPages = $entry(function() {
        return that.@com.google.appinventor.client.SharedPagesOverlay::getProjectPages()();
      });
      $wnd.exported.importNewPage = $entry(function(parent, child, onSuccess, onFail) {
        return that.@com.google.appinventor.client.SharedPagesOverlay::importNewPage(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(parent, child, onSuccess, onFail);
      });
    }-*/;

  public native void openOverlay() /*-{
    $wnd.exported.openSharedPagesOverlay();
  }-*/;
}
