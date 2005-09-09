/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.resin.mbean;

import java.util.Date;

import com.caucho.server.resin.ResinServer;

import javax.management.ObjectName;

/**
 * Management interface for the server.
 * There is one ResinServer global for the entire JVM.
 */
public interface ResinServerMBean {
  /**
   * Returns the ip address of the machine that is running this ResinServer.
   */
  public String getLocalHost();

  /**
   * Returns the server id, the value of "-server id"
   */
  public String getServerId();

  /**
   * The Resin home directory used when starting this instance of Resin.
   * This is the location of the Resin program files.
   */
  public String getResinHome();

  /**
   * The server root directory used when starting this instance of Resin.
   * This is the root directory of the web server files.
   */
  public String getServerRoot();

  /**
   * Returns the config file, the value of "-conf foo.conf"
   */
  public String getConfigFile();

  /**
   * The current lifecycle state.
   */
  public String getState();

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime();

  /**
   * Returns the last restart time.
   */
  public Date getStartTime();

  /**
   * Restart this Resin server.
   */
  public void restart();


  public ObjectName[] getServerObjectNames();


  public ObjectName getThreadPoolObjectName();
}
