package com.google.appinventor.client;

import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.helper.Callback;
import com.google.appinventor.client.helper.Helper;
import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.ProjectRootNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YAFormPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YASharedPageBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.common.collect.Maps;

import java.util.Map;

public class YACachedBlocksNode {
  private static final Map<YoungAndroidBlocksNode, YACachedBlocksNode> blocksNodeToCachedNode =
          Maps.newHashMap();
  private final  YoungAndroidBlocksNode realNode;
  private String content;

  private YACachedBlocksNode(YoungAndroidBlocksNode node) {
    realNode = node;
    blocksNodeToCachedNode.put(node, this);
  }

  //returns a cached node if the blocks node exists, otherwise null
  public static YACachedBlocksNode getCachedNode(long projectId, String fileId) {
    Project project = Ode.getInstance().getProjectManager().getProject(projectId);
    if (project == null) {
      Helper.println("Error :: project is null!!!");
      return null;
    }
    if (project.getRootNode() == null) Helper.debugger();
    ProjectNode sourceNode = project.getRootNode().findNode(fileId);
    if (sourceNode == null) {
      Helper.println("Error :: sourceNode does not exist");
      return null;
    }
    if (!(sourceNode instanceof YoungAndroidBlocksNode)) {
      Helper.println("Error :: sourceNode is not a YoungAndroidBlocksNode");
      return null;
    }
    return getCachedNode((YoungAndroidBlocksNode)sourceNode);
  }

  public static YACachedBlocksNode getCachedNode(YoungAndroidBlocksNode sourceNode) {
    if (blocksNodeToCachedNode.get(sourceNode) != null) {
      return blocksNodeToCachedNode.get(sourceNode);
    }
    return new YACachedBlocksNode(sourceNode);
  }

  public YoungAndroidBlocksNode getRealNode() {
    return realNode;
  }

  public void save(String s, boolean force, OdeAsyncCallback<Long> callback) {
    saveToCache(s);
    Ode.getInstance().getProjectService().save2(Ode.getInstance().getSessionId(),
            realNode.getProjectId(), realNode.getFileId(), force, content, callback);
  }

  public void saveToCache(String s) {
    content = s;
  }

  public void load(final Callback<String> callback) {
    if (content == null) { //first time being called
      Ode.getInstance().getProjectService().load2(realNode.getProjectId(), realNode.getFileId(), new OdeAsyncCallback<ChecksumedLoadFile>() {
        @Override
        public void onSuccess(ChecksumedLoadFile result) {
          try {
            content = result.getContent();
            callback.call(content);
          } catch (ChecksumedFileException e) {
            onFailure(e);
          }
        }
      });
    } else {
      callback.call(content);
    }
  }

  public String getFileId() {
    return realNode.getFileId();
  }

  public long getProjectId() {
    return realNode.getProjectId();
  }

  public String getFileName() {
    return realNode.getFormName();
  }

  public String getName() {
    return realNode.getName();
  }

  public String getFormName() {
    return realNode.getFormName();
  }

  public ProjectRootNode getProjectRootNode() {
    return realNode.getProjectRoot();
  }

  public boolean isSharedPageNode() {
    return realNode instanceof YASharedPageBlocksNode;
  }

  public boolean isFormPageNode() {
    return realNode instanceof YAFormPageBlocksNode;
  }


  // --- FOR DEBUGGING PURPOSES ONLY!!! ---
  public String getContent() {
    return content;
  }
}
