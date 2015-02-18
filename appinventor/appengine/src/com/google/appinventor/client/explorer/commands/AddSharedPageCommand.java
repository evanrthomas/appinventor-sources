// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client.explorer.commands;

import com.google.appinventor.client.DesignToolbar;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.editor.FileEditor;
import com.google.appinventor.client.editor.ProjectEditor;
import com.google.appinventor.client.editor.youngandroid.YaCodePageEditor;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.output.OdeLog;
import com.google.appinventor.client.widgets.LabeledTextBox;
import com.google.appinventor.client.youngandroid.TextValidators;
import com.google.appinventor.shared.rpc.project.ProjectNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidBlocksNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidPackageNode;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.appinventor.shared.youngandroid.YoungAndroidSourceAnalyzer;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

import java.util.HashSet;
import java.util.Set;

import static com.google.appinventor.client.Ode.MESSAGES;

/**
 * A command that creates a new form.
 *
 * @author lizlooney@google.com (Liz Looney)
 */
public final class AddSharedPageCommand extends ChainableCommand {

  @Override
  public boolean willCallExecuteNextCommand() {
    return true;
  }

  @Override
  public void execute(ProjectNode node) {
    OdeLog.log("AddSharedPageCommand.execute()");
    if (node instanceof YoungAndroidProjectNode) {
      OdeLog.log("AddSharedPageCommand.execute() instanceof success");
      NewSharedPageDialog dialogBox = new NewSharedPageDialog((YoungAndroidProjectNode)node);
      dialogBox.show();
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
              designToolbar.addScreen(projectId, new DesignToolbar.Screen(name, (YaCodePageEditor)blocksEditor));
              executeNextCommand(projectRootNode);
              reloadOverlay();
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
    ode.getProjectService().addFile(projectRootNode.getProjectId(), blocksFileId, callback);
  }

  public native void reloadOverlay() /*-{
     $wnd.exported.openSharedPagesOverlay();
    }-*/;
  private class NewSharedPageDialog extends DialogBox {
    // UI elements
    private final LabeledTextBox textBox;

    private final Set<String> otherBlocksEditors;


    NewSharedPageDialog(final YoungAndroidProjectNode projectRootNode) {
      super(false, true);
      getElement().getStyle().setZIndex(101); //set z-index to be above the shared page modal overlay

      setText(MESSAGES.newSharedPageTitle());
      VerticalPanel contentPanel = new VerticalPanel();

      // Collect the existing names so we can prevent duplicate form names.
      otherBlocksEditors = new HashSet<String>();

      for (ProjectNode source : projectRootNode.getAllSourceNodes()) {
        if (source instanceof YoungAndroidBlocksNode) {
          String name = ((YoungAndroidBlocksNode) source).getName();
          otherBlocksEditors.add(name);
        }
      }

      textBox = new LabeledTextBox(MESSAGES.sharedPageNameLabel());
      textBox.getTextBox().addKeyUpHandler(new KeyUpHandler() {
        @Override
        public void onKeyUp(KeyUpEvent event) {
          int keyCode = event.getNativeKeyCode();
          if (keyCode == KeyCodes.KEY_ENTER) {
            handleOkClick(projectRootNode);
          } else if (keyCode == KeyCodes.KEY_ESCAPE) {
            hide();
            executionFailedOrCanceled();
          }
        }
      });
      contentPanel.add(textBox);

      String cancelText = MESSAGES.cancelButton();
      String okText = MESSAGES.okButton();

      Button cancelButton = new Button(cancelText);
      cancelButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          hide();
          executionFailedOrCanceled();
        }
      });
      Button okButton = new Button(okText);
      okButton.addClickHandler(new ClickHandler() {
        @Override
        public void onClick(ClickEvent event) {
          handleOkClick(projectRootNode);
        }
      });
      HorizontalPanel buttonPanel = new HorizontalPanel();
      buttonPanel.add(cancelButton);
      buttonPanel.add(okButton);
      buttonPanel.setSize("100%", "24px");
      contentPanel.add(buttonPanel);
      contentPanel.setSize("320px", "100%");

      add(contentPanel);
    }

    private void handleOkClick(YoungAndroidProjectNode projectRootNode) {
      String name = textBox.getText();
      if (validate(name)) {
        hide();
        addBlocksNode(projectRootNode, name);
      } else {
        textBox.setFocus(true);
      }
    }


    private boolean validate(String name) {
      // Check that it meets the formatting requirements.
      if (!TextValidators.isValidIdentifier(name)) {
        Window.alert(MESSAGES.malformedFormNameError());
        return false;
      }

      // Check that it's unique.
      if (otherBlocksEditors.contains(name + YoungAndroidSourceAnalyzer.BLOCKLY_SOURCE_EXTENSION)) {
        Window.alert(MESSAGES.duplicateSharedPageError());
        return false;
      }

      return true;
    }

    @Override
    public void show() {
      super.show();
      Scheduler.get().scheduleDeferred(new Scheduler.ScheduledCommand() {
        @Override
        public void execute() {
          textBox.setFocus(true);
        }
      });
    }
  }
}
