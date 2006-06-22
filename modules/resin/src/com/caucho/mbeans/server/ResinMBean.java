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
 * @author Sam
 */

package com.caucho.mbeans.server;

import com.caucho.jmx.Description;

import javax.management.ObjectName;
import java.util.Date;

/**
 * Management interface for the server.
 * There is one ResinServer global for the entire JVM.
 */
@Description("A single Resin for each JVM provides a global environment for Resin")
public interface ResinMBean {
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @Description("The JMX ObjectName for the MBean")
  public String getObjectName();

  /**
   * Returns the version.
   */
  @Description("The Resin Version")
  public String getVersion();

  /**
   * Returns true for the professional version.
   */
  @Description("True for Resin Professional")
  public boolean isProfessional();

  /**
   * The Resin home directory used when starting this instance of Resin.
   * This is the location of the Resin program files.
   */
  @Description("The Resin home directory used when starting"
               + " this instance of Resin. This is the location"
               + " of the Resin program files")
  public String getResinHome();
  
  /**
   * The server root directory used when starting this instance of Resin.
   * This is the root directory of the web server files.
   */
  @Description("The server root directory used when starting"
               + " this instance of Resin. This is the root"
               + " directory of the web server files")
  public String getServerRoot();

  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  @Description("The configuration file used when starting this"
               + " instance of Resin, the value of `-conf'")
  public String getConfigFile();

  /**
   * Returns the server for this instance.
   */
  @Description("The current Server instance")
  public String getServer();
}
