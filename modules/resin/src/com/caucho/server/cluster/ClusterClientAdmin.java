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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.server.cluster;

import com.caucho.mbeans.server.ClusterClientMBean;
import com.caucho.lifecycle.Lifecycle;

import javax.management.ObjectName;

public class ClusterClientAdmin
  implements ClusterClientMBean
{
  private final ClusterServer _clusterServer;

  public ClusterClientAdmin(ClusterServer clusterServer)
  {
    _clusterServer = clusterServer;
  }

  public String getObjectName()
  {
    return _clusterServer.getObjectName();
  }

  public String getServerId()
  {
    return _clusterServer.getId();
  }

  public int getIndex()
  {
    return _clusterServer.getIndex();
  }

  public String getHost()
  {
    return _clusterServer.getHost();
  }

  public int getPort()
  {
    return _clusterServer.getPort();
  }

  public boolean isBackup()
  {
    return _clusterServer.isBackup();
  }

  public long getReadTimeout()
  {
    return _clusterServer.getReadTimeout();
  }

  public long getWriteTimeout()
  {
    return _clusterServer.getWriteTimeout();
  }

  public long getMaxIdleTime()
  {
    return _clusterServer.getMaxIdleTime();
  }

  public long getFailRecoverTime()
  {
    return _clusterServer.getFailRecoverTime();
  }

  public String getState()
  {
    if (_clusterServer.isActive())
      return Lifecycle.getStateName(Lifecycle.IS_ACTIVE);
    else
      return Lifecycle.getStateName(Lifecycle.IS_STOPPED);
  }

  public boolean isDead()
  {
    return ! _clusterServer.isActive();
  }

  public int getActiveConnectionCount()
  {
    return _clusterServer.getActiveCount();
  }

  public int getIdleConnectionCount()
  {
    return _clusterServer.getIdleCount();
  }

  public long getLifetimeConnectionCount()
  {
    return _clusterServer.getClient().getLifetimeConnectionCount();
  }

  public long getLifetimeKeepaliveCount()
  {
    return _clusterServer.getClient().getLifetimeKeepaliveCount();
  }

  public void start()
  {
    _clusterServer.enable();
  }

  public void stop()
  {
    _clusterServer.disable();
  }

  public boolean canConnect()
  {
    return _clusterServer.canConnect();
  }

  public String toString()
  {
    return "ClusterClientAdmin[" + getObjectName() + "]";
  }
}
