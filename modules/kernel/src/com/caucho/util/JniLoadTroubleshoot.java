/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.util;

import com.caucho.vfs.*;

/**
 * Common error management for JNI loading
 */
public class JniLoadTroubleshoot {
  private String _name;

  private Throwable _cause;

  public JniLoadTroubleshoot(String name, Throwable cause)
  {
    _name = name;
    _cause = cause;
  }

  public String getMessage()
  {
    StringBuilder sb = new StringBuilder();

    sb.append("JNI library '" + _name + "' could not be loaded.\n");
    sb.append("  The JVM exception message was: " + _cause.getMessage());

    String libexec = getLibexec();
    String lib = getJniPrefix() + _name + getJniSuffix();

    Path path = Vfs.lookup(lib);

    if (! path.exists()) {
      sb.append("  The JNI file " + path.getNativePath() + " does not exist.");
    }
    else if (! path.canRead()) {
      sb.append("  The JNI file " + path.getNativePath() + " cannot be read.");
    }

    return sb.toString();
  }

  private boolean is64()
  {
    return "64".equals(System.getProperty("sun.arch.data.model"));
  }

  private boolean isWin()
  {
    return System.getProperty("os.name").startsWith("win");
  }

  private String getResinHome()
  {
    return System.getProperty("resin.home");
  }

  private String getJniPrefix()
  {
    if (isWin())
      return "";
    else
      return "lib";
  }

  private String getJniSuffix()
  {
    if (isWin())
      return ".dll";
    else
      return ".so";
  }

  private String getLibexec()
  {
    String resinHome = System.getProperty("resin.home");

    if (isWin()) {
      if (is64())
        return resinHome + "/win64";
      else
        return resinHome + "/win32";
    }
    else {
      if (is64())
        return resinHome + "/libexec";
      else
        return resinHome + "/libexec64";
    }
  }
}
