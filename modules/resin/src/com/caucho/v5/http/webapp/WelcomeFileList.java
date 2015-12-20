/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.L10N;

import java.util.ArrayList;

/**
 * Configuration for welcome-file-list.
 */
public class WelcomeFileList {
  static L10N L = new L10N(WelcomeFileList.class);

  // The welcome file list.
  private ArrayList<String> _welcomeFileList = new ArrayList<String>();
  private String _id;

  /**
   * Adds a welcome file.
   */
  public void addText(String text)
  {
    int len = text.length();
    int i = 0;
    CharBuffer cb = new CharBuffer();
    while (i < len) {
      char ch = 0;
      for (;
           i < len && (ch = text.charAt(i)) == ' ' || ch == ',' || ch == '\t';
           i++) {
      }

      if (i >= len)
        break;

      cb.clear();
      for (;
           i < len && (ch = text.charAt(i)) != ' ' && ch != ',' && ch != '\t';
           i++) {
        cb.append(ch);
      }

      _welcomeFileList.add(cb.toString());
    }
  }

  /**
   * Adds a welcome file.
   */
  public void addWelcomeFile(String file)
  {
    _welcomeFileList.add(file);
  }

  /**
   * Returns the welcome file list.
   */
  public ArrayList<String> getWelcomeFileList()
  {
    return _welcomeFileList;
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }
}
