/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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

package com.caucho.mbeans.j2ee;

import com.caucho.util.CauchoSystem;
import com.caucho.server.resin.ResinServer;

/**
 * Management interface for the JVM.
 */
public class JVM extends J2EEManagedObject {
  public JVM()
  {
  }

  protected String getName()
  {
    String name =  ResinServer.getResinServer().getServerId();

    return name == null ? "default" : name;
  }

  protected String getJ2EEApplication()
  {
    return null;
  }

  /**
   * Returns the java version
   */
  public String getJavaVersion()
  {
    return System.getProperty("java.version");
  }

  /**
   * Returns the java vendor
   */
  public String getJavaVendor()
  {
    return System.getProperty("java.vendor");
  }

  /**
   * Returns the machine the JVM is running on, i.e. the fully
   * qualified hostname.
   */
  public String getNode()
  {
    return CauchoSystem.getLocalHost();
  }
}
