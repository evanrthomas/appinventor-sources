// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2009-2011 Google, All Rights reserved
// Copyright 2011-2012 MIT, All rights reserved
// Released under the MIT License https://raw.github.com/mit-cml/app-inventor/master/mitlicense.txt

package com.google.appinventor.client.boxes;

import com.google.appinventor.client.explorer.youngandroid.ProjectList;
import com.google.appinventor.client.widgets.boxes.Box;

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

    plist = new ProjectList();
    setContent(plist);
  }

  /**
   * Returns project list associated with projects explorer box.
   *
   * @return  project list
   */
  public ProjectList getProjectList() {
     return plist;
  }
}
