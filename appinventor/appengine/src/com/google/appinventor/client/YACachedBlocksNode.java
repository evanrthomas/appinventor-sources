package com.google.appinventor.client;

import com.google.appinventor.shared.rpc.project.ChecksumedFileException;
import com.google.appinventor.shared.rpc.project.ChecksumedLoadFile;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.common.collect.Maps;

import java.util.Map;

import static com.google.appinventor.client.Ode.MESSAGES;

public class YACachedBlocksNode {
  private static final Map<YoungAndroidBlocksNode, YACachedBlocksNode> blocksNodeToCachedNode =
          Maps.newHashMap();
  private final  YoungAndroidBlocksNode realNode;
  private String content;

   YACachedBlocksNode(YoungAndroidBlocksNode node) {
    realNode = node;
  }

  public static YACachedBlocksNode getOrCreateCachedNode(YoungAndroidBlocksNode node) {
    if (getCachedNode(node.getProjectId(), node.getFileId()) == null) {
      blocksNodeToCachedNode.put(node, new YACachedBlocksNode(node));
    }
    return getCachedNode(node.getProjectId(), node.getFileId());
  }

  public static YACachedBlocksNode getCachedNode(long projectId, String fileId) {
    for (YoungAndroidBlocksNode node: blocksNodeToCachedNode.keySet())  {
      if (node.getProjectId() == projectId && node.getFileId().equals(fileId)) {
        return blocksNodeToCachedNode.get(node);
      }
    }
    return null;
  }

  public YoungAndroidBlocksNode getRealNode() {
    return realNode;
  }

  public void init() {
    Ode.getInstance().getProjectService().load2(realNode.getProjectId(), realNode.getFileId(),
            new OdeAsyncCallback<ChecksumedLoadFile>(MESSAGES.loadError()) {
              @Override
              public void onSuccess(ChecksumedLoadFile result) {
                try {
                  content = result.getContent();
                } catch (ChecksumedFileException e) {
                  onFailure(e);
                }
              }
            });
  }

  public void save(String s, boolean force, OdeAsyncCallback<Long> callback) {
    content = s;
    Ode.getInstance().getProjectService().save2(Ode.getInstance().getSessionId(),
            realNode.getProjectId(), realNode.getFileId(), force, content, callback);
  }

  public void load(OdeAsyncCallback<ChecksumedLoadFile> callback) {
    if (content == null) { //TODO (evan): set content here
      Ode.getInstance().getProjectService().load2(realNode.getProjectId(), realNode.getFileId(), callback);
    } else {
      try {
        ChecksumedLoadFile file = new ChecksumedLoadFile();
        file.setContent(content);
        callback.onSuccess(file);
      } catch (ChecksumedFileException e) {
        callback.onFailure(e);
      }
    }
  }

  public String getFileId() {
    return realNode.getFileId();
  }

  public long getProjectId() {
    return realNode.getProjectId();
  }

  public String getName() {
    return realNode.getName();
  }

  public String getFileName() {
    return realNode.getFormName();
  }

}