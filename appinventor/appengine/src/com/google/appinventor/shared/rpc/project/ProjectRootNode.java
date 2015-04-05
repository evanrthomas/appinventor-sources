// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.shared.rpc.project;

import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * All projects have a subclass of this project node at their root.
 *
 */
public abstract class ProjectRootNode extends ProjectNode {

  // For serialization
  private static final long serialVersionUID = 770074523523022847L;

  // Project id
  private long projectId;

  // Type of project
  private String type;

  /**
   * Default constructor (for serialization only). Unfortunately this will
   * prevent any fields from being marked as final!
   */
  public ProjectRootNode() {
  }

  /**
   * Creates a new project root node.
   *
   * @param name project node name (can be different from file or folder
   *        represented by this node)
   * @param projectId project ID
   * @param type project type
   */
  public ProjectRootNode(String name, long projectId, String type) {
    super(name, Long.toString(projectId));

    this.projectId = projectId;
    this.type = type;
  }

  /**
   * Returns a list of all source nodes of the project.
   *
   * @return list of source project nodes
   */
  public List<ProjectNode> getAllSourceNodes() {
    List<ProjectNode> sourceNodes = new ArrayList<ProjectNode>();
    findSourceNodes(sourceNodes);
    return sourceNodes;
  }

  public List<YoungAndroidBlocksNode> getAllBlocksNodes() {
    List<YoungAndroidBlocksNode> nodes = new ArrayList<YoungAndroidBlocksNode>();
    for (ProjectNode node : getAllSourceNodes()) {
      if (YoungAndroidSourceAnalyzer.isBlocksNodeSourceFileId(node.getFileId())) {
        nodes.add((YoungAndroidBlocksNode)node);
      }
    }
    return nodes;
  }

  /**
   * Returns the source node with the given fileId, or null if there is no source node with the
   * given fileId.
   */
  public ProjectNode getSourceNode(String fileId) {
    for (ProjectNode node : getAllSourceNodes()) {
      if (node.getFileId().equals(fileId)) {
        return node;
      }
    }
    return null;
  }

  public YoungAndroidBlocksNode getBlocksFile(String fileId) {
    ProjectNode node;
    if ((node = getSourceNode(fileId)) instanceof YoungAndroidBlocksNode) {
      return (YoungAndroidBlocksNode)node;
    }
    return null;
  }

  @Override
  public ProjectRootNode getProjectRoot() {
    return this;
  }

  @Override
  public long getProjectId() {
    return projectId;
  }

//  /**
//   * Returns the ID for this project node.
//   *
//   * @return ID to identify the file/folder represented by the project node on
//   *         the backend (can be {@code null} for 'virtual' folders)
//   */
//  @Override
//  public String getFileId() {
//    throw new UnsupportedOperationException("getFileId is not supported for ProjectRootNode");
//  }

  @Override
  public String getProjectType() {
    return type;
  }
}
