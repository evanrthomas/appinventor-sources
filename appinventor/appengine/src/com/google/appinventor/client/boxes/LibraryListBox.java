// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client.boxes;

import com.google.appinventor.client.explorer.project.Project;
import com.google.appinventor.client.explorer.youngandroid.ProjectList;
import com.google.appinventor.client.widgets.boxes.Box;
import com.google.appinventor.shared.rpc.project.youngandroid.YoungAndroidProjectNode;

import static com.google.appinventor.client.Ode.MESSAGES;


/**
 * Box implementation for project list.
 *
 */
public final class LibraryListBox extends Box {

  // Singleton project explorer box instance (only one project explorer allowed)
  private static final LibraryListBox INSTANCE = new LibraryListBox();

  // Project list for young android
  private final ProjectList plist;

  /**
   * Returns the singleton projects list box.
   *
   * @return  project list box
   */
  public static LibraryListBox getLibraryListBox() {
    return INSTANCE;
  }

  /**
   * Creates new library list box.
   */
  private LibraryListBox() {
    super(MESSAGES.libraryListBoxCaption(),
        300,    // height
        false,  // minimizable
        false); // removable

    plist = new ProjectList(new ProjectList.ProjectFilter() {
      @Override
      public boolean filter(Project project) {
        return project.getProjectType().equals(YoungAndroidProjectNode.YOUNG_ANDROID_BOOK_PROJECT_TYPE);
      }
    });
    setContent(plist);
  }
}
