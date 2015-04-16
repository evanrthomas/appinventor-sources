
package com.google.appinventor.client;

import com.google.appinventor.client.explorer.commands.AddSharedPageCommand;
import com.google.appinventor.client.explorer.commands.ChainableCommand;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.explorer.project.ProjectChangeAdapter;
import com.google.appinventor.client.explorer.project.ProjectManager;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.Closure;
import com.google.appinventor.client.helper.Helper;
import com.google.appinventor.client.helper.Utils;
import com.google.appinventor.client.linker.Linker;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YAFormPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YASharedPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class SharedPagesOverlay {
  private final static SharedPagesOverlay INSTANCE = new SharedPagesOverlay();
  private final static ProjectManager projectManager = Ode.getInstance().getProjectManager();

  private SharedPagesOverlay() {}

  public static SharedPagesOverlay getInstance() {
    return INSTANCE;
  }

  public static void openOverlay() {
    exportMethods();
    openModal();
    loadThisProjectsPages();
    loadPagesFromLibrary();
    loadLinks();
  }

  public static void newSharedPage(JavaScriptObject onSuccess, JavaScriptObject onFail) { //called from javascript
    ProjectRootNode rootNode = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
    ChainableCommand cmd = new AddSharedPageCommand(onSuccess, onFail);
    cmd.startExecuteChain(Tracking.PROJECT_ACTION_ADD_SHARED_PAGE_YA, rootNode);
  }

  public static void newLink(final JavaScriptObject parentObject, final JavaScriptObject childObject,
                      final JavaScriptObject onSuccess, final JavaScriptObject onFail) {
    Callback<YoungAndroidBlocksNode[]> addChildToParent = new Callback<YoungAndroidBlocksNode[]>() {
      @Override
      public void call(YoungAndroidBlocksNode[] nodes) {
        YoungAndroidBlocksNode parentNode = nodes[0],
                               childNode = nodes[1];

        if (!(childNode instanceof YASharedPageBlocksNode)) {
          Utils.callJSFunc(onFail, Helper.eval("\"child must be a shared page\""));
          return;
        }
        Linker.getInstance().newLink(parentNode, (YASharedPageBlocksNode) childNode);
        Utils.callJSFunc(onSuccess, null);
      }
    };
    descriptorsToNodesAsync(addChildToParent, new JSONObject(parentObject), new JSONObject(childObject));
  }


  public static void removeLink(JavaScriptObject parentObject, JavaScriptObject childObject, final JavaScriptObject onSuccess) {
    descriptorsToNodesAsync(new Callback<YoungAndroidBlocksNode[]>() {
      @Override
      public void call(YoungAndroidBlocksNode[] nodes) {
        YoungAndroidBlocksNode parentNode = nodes[0],
                childNode = nodes[1];

        Linker.getInstance().removeLink(parentNode, childNode);
        Utils.callJSFunc(onSuccess, null);

      }
    }, new JSONObject(parentObject), new JSONObject(childObject));

  }

  private static void descriptorsToNodesAsync(final Callback<YoungAndroidBlocksNode[]> onceAllLoaded,
                                             final JSONObject ... nodeDescriptors) {
    final YoungAndroidBlocksNode nodes[] = new YoungAndroidBlocksNode[nodeDescriptors.length];
    final MutableInt timesCalled = new MutableInt(0);
    for (int i = 0; i<nodeDescriptors.length; i++) {
      final JSONObject descriptor = nodeDescriptors[i];
      long projectId = decodeLong(descriptor.get("projectId"));
      Project project = projectManager.getProject(projectId);
      if (project == null) throw new RuntimeException("invalid projectId " + projectId);

      onLoadProjectNodes(project, new Closure<Integer, Project>(i) {
        @Override
        public void call(Project project) {
          timesCalled.value += 1;
          nodes[this.env] = descriptorToNode(descriptor);
          if (timesCalled.value == nodeDescriptors.length) onceAllLoaded.call(nodes);
        }
      });
    }
  }

  private static YoungAndroidBlocksNode descriptorToNode(JSONObject descriptor) {
    long projectId = decodeLong(descriptor.get("projectId"));
    String fileId = descriptor.get("fileId").isString().stringValue();

    Project project = projectManager.getProject(projectId);
    if (project == null) throw new RuntimeException("invalid projectId");
    YoungAndroidBlocksNode node = project.getRootNode().getBlocksFile(fileId);
    if (node == null) throw new RuntimeException("invalid fileId");
    return node;

  }

  public static String getCurrentProjectId() {
    return Ode.getInstance().getCurrentYoungAndroidProjectId() + "";
  }

  private static void loadThisProjectsPages() {
    final JavaScriptObject renderPageFunction = getRenderProjectPageFunction();
    ProjectRootNode currentProject = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
    final Set<YoungAndroidBlocksNode> rendered = new HashSet<YoungAndroidBlocksNode>();
    for (final YoungAndroidBlocksNode page : currentProject.getAllBlocksNodes()) {
      if (!rendered.contains(page)) { //avoid adding a page that has already been added because it's the child of some other page in the project
        Utils.callJSFunc(renderPageFunction, pageDescriptor(page).getJavaScriptObject());
        rendered.add(page);
      }
      Linker.getInstance().loadChildren(page, new Callback<Collection<YoungAndroidBlocksNode>>() {
        @Override
        public void call(Collection<YoungAndroidBlocksNode> children) {
          for (YoungAndroidBlocksNode child : children) {
            if (!rendered.contains(child)) {  //avoid adding children that are the children of two pages twice
              Utils.callJSFunc(renderPageFunction, pageDescriptor(child).getJavaScriptObject());
              rendered.add(child);
            }
          }
        }
      });
    }
  }

  private static void loadPagesFromLibrary() { //called from javascript
    final JavaScriptObject renderLibaryPageFunction = getRenderLibraryPageFunction();
    Collection<Project> books = Ode.getInstance().getProjectManager().getLibrary();
    for (final Project book : books) {
      onLoadProjectNodes(book, new Callback<Project>() {
        @Override
        public void call(Project book) {
          for (YoungAndroidBlocksNode page : book.getRootNode().getAllBlocksNodes()) {
            Utils.callJSFunc(renderLibaryPageFunction, pageDescriptor(page).getJavaScriptObject());
          }
        }
      });
    }
  }

  private static void loadLinks() {
    final JavaScriptObject renderLink = getRenderLinkFunction();
    ProjectRootNode currentProject = Ode.getInstance().getCurrentYoungAndroidProjectRootNode();
    for (final YoungAndroidBlocksNode projectPage : currentProject.getAllBlocksNodes()) {
      Linker.getInstance().loadChildren(projectPage, new Callback<Collection<YoungAndroidBlocksNode>>() {
        @Override
        public void call(Collection<YoungAndroidBlocksNode> children) {
          JSONObject link = new JSONObject();
          for (YoungAndroidBlocksNode child : children) {
            link.put("parent", pageDescriptor(projectPage));
            link.put("child", pageDescriptor(child));
            Utils.callJSFunc(renderLink, link.getJavaScriptObject());
          }
        }
      });
    }
  }

  private static void onLoadProjectNodes(final Project project, final Callback<Project> callback) {
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
    JSONObject jsonBlob = new JSONObject();
    String name = node.getFormName();
    if (name.endsWith(YoungAndroidSourceAnalyzer.FORM_PAGE_SOURCE_EXTENSION)) {
      name = name.substring(0, name.length() - YoungAndroidSourceAnalyzer.FORM_PAGE_SOURCE_EXTENSION.length());
    }
    jsonBlob.put("name", new JSONString(name));
    jsonBlob.put("fileName", new JSONString(node.getFormName()));
    jsonBlob.put("projectName", new JSONString(node.getProjectRoot().getName()));
    jsonBlob.put("fileId", new JSONString(node.getFileId()));
    jsonBlob.put("projectId", encodeLong(node.getProjectId()));
    jsonBlob.put("type", node instanceof YAFormPageBlocksNode ?
            new JSONString("formPage") :
            new JSONString("sharedPage"));
    return jsonBlob;
  }

  private static JSONValue encodeLong(long l) { //gwt json doesn't let you put longs as a jsonvalue
    return new JSONString(l+"");
  }

  private static Long decodeLong(JSONValue v)  {
    String s = v.isString().toString();
    if (s.charAt(0) == '"') {
      return Long.parseLong(s.substring(1, s.length() - 1));
    }
    return Long.parseLong(s);
  }

  private static native JavaScriptObject openModal() /*-{
    $wnd.exported.openModal();
  }-*/;

  private static native JavaScriptObject getRenderProjectPageFunction() /*-{
    return $wnd.exported.renderProjectPage;
  }-*/;

  private static native JavaScriptObject getRenderLibraryPageFunction() /*-{
    return $wnd.exported.renderLibraryPage;
  }-*/;

  private static native JavaScriptObject getRenderLinkFunction() /*-{
    return $wnd.exported.renderLink;
  }-*/;

  private static native void exportMethods() /*-{
      $wnd.exported.newSharedPage = $entry(function(onSuccess, onFail) {
        return @com.google.appinventor.client.SharedPagesOverlay::newSharedPage(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(onSuccess, onFail);
       });

      $wnd.exported.newLink = $entry(function(parent, child, onSuccess, onFail) {
        return @com.google.appinventor.client.SharedPagesOverlay::newLink(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(parent, child, onSuccess, onFail);
      });

      $wnd.exported.removeLink = $entry(function(parent, child, onSuccess) {
        return @com.google.appinventor.client.SharedPagesOverlay::removeLink(Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;Lcom/google/gwt/core/client/JavaScriptObject;)(parent, child, onSuccess);
      });

      $wnd.exported.getCurrentProjectId = $entry(@com.google.appinventor.client.SharedPagesOverlay::getCurrentProjectId());
    }-*/;


  private static class MutableInt {
    protected int value;
    public MutableInt(int i) {
      value = i;
    }
  }
}
