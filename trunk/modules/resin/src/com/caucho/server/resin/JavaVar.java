/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import java.util.Properties;

import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Java variables in Resin configuration ${java}.
 */
public class JavaVar {
  /**
   * Returns true for JDK 5
   */
  public boolean isJava5()
  {
    return true;
  }

  /**
   * Returns the JDK home
   */
  public Path getHome()
  {
    return Vfs.lookup(System.getProperty("java.home"));
  }

  /**
   * Returns the JDK properties
   */
  public Properties getProperties()
  {
    return System.getProperties();
  }

  /**
   * Returns the user name
   */
  public String getUserName()
  {
    return System.getProperty("user.name");
  }

  /**
   * Returns the JDK version
   */
  public String getVersion()
  {
    return System.getProperty("java.version");
  }
}
