// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client.explorer.commands;

import com.google.appinventor.client.DesignToolbar;
import com.google.appinventor.client.Helper;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.gwt.core.client.Scheduler;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * A command that creates a new form.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class AddSharedPageCommand extends ChainableCommand {

  private static final int MAX_FORM_COUNT = 10;

  /**
   * Creates a new command for creating a new form
   */
  public AddSharedPageCommand() {
  }

  @Override
  public boolean willCallExecuteNextCommand() {
    return true;
  }

  @Override
  public void execute(ProjectNode node) {
    OdeLog.log("AddSharedPageCommand.execute()");
    if (node instanceof YoungAndroidProjectNode) {
      OdeLog.log("AddSharedPageCommand.execute() instanceof success");
      addBlocksNode((YoungAndroidProjectNode) node, "SharedPage");
    } else {
      OdeLog.log("AddSharedPageCommand.execute() illegal argument");
      executionFailedOrCanceled();
      throw new IllegalArgumentException("node must be a YoungAndroidProjectNode");
    }
  }


  /**
   * Adds a new shared page to the project.
   */
  private void addBlocksNode(final YoungAndroidProjectNode projectRootNode,
                               final String name) {
    final Ode ode = Ode.getInstance();
    final YoungAndroidPackageNode packageNode = projectRootNode.getPackageNode();
    String qualifiedName = packageNode.getPackageName() + '.' + name;
    final String blocksFileId = YoungAndroidBlocksNode.getBlocklyFileId(qualifiedName);

    OdeAsyncCallback<Long> callback = new OdeAsyncCallback<Long>(
            // failure message
            MESSAGES.addFormError()) {
      @Override
      public void onSuccess(Long modDate) {
        final Ode ode = Ode.getInstance();
        ode.updateModificationDate(projectRootNode.getProjectId(), modDate);

        // Add the new blocks node to the project
        final Project project = ode.getProjectManager().getProject(projectRootNode);
        project.addNode(packageNode, new YoungAndroidBlocksNode(blocksFileId));

        // Add the screen to the DesignToolbar and select the new form editor.
        // We need to do this once the form editor and blocks editor have been
        // added to the project editor (after the files are completely loaded).
        //
        // TODO(sharon): if we create YaProjectEditor.addScreen() and merge
        // that with the current work done in YaProjectEditor.addFormEditor,
        // consider moving this deferred work to the explicit command for
        // after the form file is loaded.

        Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
          @Override
          public void execute() {
            //TODO (evan): consider making a YoungAndroidSharedBlocksNode here, and adding that to the screen instead
            ProjectEditor projectEditor =
                    ode.getEditorManager().getOpenProjectEditor(project.getProjectId());
            FileEditor blocksEditor = projectEditor.getFileEditor(blocksFileId);
            if (blocksEditor != null && !ode.screensLocked()) {
              DesignToolbar designToolbar = Ode.getInstance().getDesignToolbar();
              long projectId = blocksEditor.getProjectId();
              designToolbar.addScreen(projectId, new DesignToolbar.Screen(name, blocksEditor));
              executeNextCommand(projectRootNode);
            } else {
              // The form editor and/or blocks editor is still not there. Try again later.
              Scheduler.get().scheduleDeferred(this);
            }
          }
        });
      }

      @Override
      public void onFailure(Throwable caught) {
        super.onFailure(caught);
        executionFailedOrCanceled();
      }
    };

    // Create the new blocks file (.bky) on the backend
    Helper.println("AddSharedPage adding to backend " + blocksFileId);
    ode.getProjectService().addFile(projectRootNode.getProjectId(), blocksFileId, callback);
  }
}

