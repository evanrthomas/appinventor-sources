package com.google.appinventor.shared.rpc.project.youngandroid;


import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;

/**
 * Young Android blocks source file node in the project tree.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class  YASharedPageBlocksNode extends YoungAndroidBlocksNode {

  /**
   * Default constructor (for serialization only).
   */
  public YASharedPageBlocksNode() {
  }


  /**
   * Creates a new Young Android blocks source file project node.
   *
   * @param fileId  file id
   */
  public YASharedPageBlocksNode(String fileId) {
    super(fileId);
  }

  public static String getBlocklyFileId(String qualifiedName) {
    return SRC_PREFIX + qualifiedName.replace('.', '/')
            + YoungAndroidSourceAnalyzer.SHARED_PAGE_SOURCE_EXTENSION;
  }

}
