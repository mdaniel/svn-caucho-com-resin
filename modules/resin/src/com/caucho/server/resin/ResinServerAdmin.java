/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.resin;

import com.caucho.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.mbeans.server.ResinServerMBean;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import java.util.Date;
import java.util.ArrayList;

public class ResinServerAdmin
  implements ResinServerMBean
{
  private static final L10N L = new L10N(ResinServerAdmin.class);

  private static final ObjectName THREAD_POOL_OBJECT_NAME;

  static {
    try {
      THREAD_POOL_OBJECT_NAME = new ObjectName("resin:type=ThreadPool");
    }
    catch (MalformedObjectNameException e) {
      throw new AssertionError(e);
    }
  }

  private final ResinServer _resinServer;

  /**
   * Creates the admin object and registers with JMX.
   */
  public ResinServerAdmin(ResinServer resinServer)
  {
    _resinServer = resinServer;
  }

  /**
   * Returns the JMX {@link ObjectName}.
   */
  public ObjectName getObjectName()
  {
    return _resinServer.getObjectName();
  }

  public String getLocalHost()
  {
    return CauchoSystem.getLocalHost();
  }

  public String getServerId()
  {
    return _resinServer.getServerId();
  }

  public String getConfigFile()
  {
    return _resinServer.getConfigFile();
  }

  public String getResinHome()
  {
    return CauchoSystem.getResinHome().getNativePath();
  }

  public String getServerRoot()
  {
    return CauchoSystem.getServerRoot().getNativePath();
  }

  public boolean isDetailedStatistics()
  {
    return CauchoSystem.isDetailedStatistics();
  }

  public ObjectName getThreadPoolObjectName()
  {
    return THREAD_POOL_OBJECT_NAME;
  }

  public ObjectName[] getServerObjectNames()
  {
    ArrayList<ServerController> servers = _resinServer.getServerList();

    ObjectName[] objectNames = new ObjectName[servers.size()];

    int i = 0;

    for (ServerController server : servers) {
      objectNames[i++] = server.getObjectName();
    }

    return objectNames;
  }

  public String getState()
  {
    return _resinServer.getLifecycleState().getStateName();
  }

  public Date getInitialStartTime()
  {
    return _resinServer.getInitialStartTime();
  }

  public Date getStartTime()
  {
    return _resinServer.getStartTime();
  }

  public long getTotalMemory()
  {
    return Runtime.getRuntime().totalMemory();
  }

  public long getFreeMemory()
  {
    return Runtime.getRuntime().freeMemory();
  }

  public void restart()
  {
    _resinServer.destroy();
  }

  public String toString()
  {
    return "ResinServerAdmin[" + getObjectName() + "]";
  }
}
