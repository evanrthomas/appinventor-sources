package com.google.appinventor.shared.rpc.project.youngandroid;

import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;

public final class YAFormPageBlocksNode extends YoungAndroidBlocksNode {

  /**
   * Default constructor (for serialization only).
   */
  public YAFormPageBlocksNode() {
  }


  /**
   * Creates a new Young Android blocks source file project node.
   *
   * @param fileId  file id
   */
  public YAFormPageBlocksNode(String fileId) {
    super(fileId);
  }

  public static String getBlocklyFileId(String qualifiedName) {
    return SRC_PREFIX + qualifiedName.replace('.', '/')
            + YoungAndroidSourceAnalyzer.FORM_PAGE_SOURCE_EXTENSION;
  }
}
