package com.google.appinventor.client.explorer.youngandroid;

import com.google.appinventor.client.ErrorReporter;
import com.google.appinventor.client.Helper;
import com.google.appinventor.client.Ode;
import com.google.appinventor.client.OdeAsyncCallback;
import com.google.appinventor.client.boxes.LibraryListBox;
import com.google.appinventor.client.boxes.ViewerBox;
import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.tracking.Tracking;
import com.google.appinventor.client.widgets.Toolbar;
import com.google.appinventor.client.wizards.youngandroid.NewYoungAndroidProjectWizard;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Window;

import java.util.List;

import static com.google.appinventor.client.Ode.MESSAGES;

public class LibraryToolbar extends Toolbar {
  private static final String WIDGET_NAME_NEW = "New Book";
  private static final String WIDGET_NAME_DELETE = "Delete Book";

  /**
   * Initializes and assembles all commands into buttons in the toolbar.
   */
  public LibraryToolbar() {
    super();

    addButton(new ToolbarItem(WIDGET_NAME_NEW, MESSAGES.newBookMenuItem(),
        new NewAction()));

    addButton(new ToolbarItem(WIDGET_NAME_DELETE, MESSAGES.deleteBookButton(),
        new DeleteAction()));

    updateButtons();
  }

  private static class NewAction implements Command {
    @Override
    public void execute() {
      if (Ode.getInstance().screensLocked()) {
        return;                 // Refuse to switch if locked (save file happening)
      }
      new NewYoungAndroidProjectWizard(
              YoungAndroidProjectNode.YOUNG_ANDROID_BOOK_PROJECT_TYPE).center();
      // The wizard will switch to the design view when the new
      // project is created.
    }
  }

  private static class DeleteAction implements Command {
    @Override
    public void execute() {
      List<Project> selectedProjects =
        LibraryListBox.getLibraryListBox().getProjectList().getSelectedProjects();
      if (selectedProjects.size() > 0) {
        // Show one confirmation window for selected projects.
        if (deleteConfirmation(selectedProjects)) {
          for (Project project : selectedProjects) {
            deleteProject(project);
          }
        }
      } else {
        // The user can select a project to resolve the
        // error.
        ErrorReporter.reportInfo(MESSAGES.noProjectSelectedForDelete());
      }
    }

    private boolean deleteConfirmation(List<Project> projects) {
      String message;
      if (projects.size() == 1) {
        message = MESSAGES.confirmDeleteSingleProject(projects.get(0).getProjectName());
      } else {
        StringBuilder sb = new StringBuilder();
        String separator = "";
        for (Project project : projects) {
          sb.append(separator).append(project.getProjectName());
          separator = ", ";
        }
        String projectNames = sb.toString();
        message = MESSAGES.confirmDeleteManyProjects(projectNames);
      }
      return Window.confirm(message);
    }

    private void deleteProject(Project project) {
      Tracking.trackEvent(Tracking.PROJECT_EVENT,
          Tracking.PROJECT_ACTION_DELETE_PROJECT_YA, project.getProjectName());

      final long projectId = project.getProjectId();

      Ode ode = Ode.getInstance();
      boolean isCurrentProject = (projectId == ode.getCurrentYoungAndroidProjectId());
      ode.getEditorManager().closeProjectEditor(projectId);
      if (isCurrentProject) {
        // If we're deleting the project that is currently open in the Designer we
        // need to clear the ViewerBox first.
        ViewerBox.getViewerBox().clear();
      }
      // Make sure that we delete projects even if they are not open.
      doDeleteProject(projectId);
    }

    private void doDeleteProject(final long projectId) {
      Ode.getInstance().getProjectService().deleteProject(projectId,
          new OdeAsyncCallback<Void>(
              // failure message
              MESSAGES.deleteProjectError()) {
        @Override
        public void onSuccess(Void result) {
          Ode.getInstance().getProjectManager().removeProject(projectId);
          // Show a welcome dialog in case there are no
          // projects saved.
          if (Ode.getInstance().getProjectManager().getProjects().size() == 0) {
            Ode.getInstance().createWelcomeDialog(false);
          }
        }
      });
    }

  }

  /**
   * Enables and/or disables buttons based on how many projects exist
   * (in the case of "Download All Projects") or are selected (in the case
   * of "Delete" and "Download Source").
   */
  public void updateButtons() {
    ProjectList projectList = LibraryListBox.getLibraryListBox().getProjectList();
    int numSelectedProjects = projectList.getNumSelectedProjects();
    Helper.println("\tnumSelectedProjects::" + numSelectedProjects);
    setButtonEnabled(WIDGET_NAME_DELETE, numSelectedProjects > 0);
    Ode.getInstance().getTopToolbar().fileDropDown.setItemEnabled(MESSAGES.exportProjectMenuItem(),
        numSelectedProjects > 0);
    Ode.getInstance().getTopToolbar().fileDropDown.setItemEnabled(MESSAGES.exportAllProjectsMenuItem(),
            numSelectedProjects > 0);
  }
}
