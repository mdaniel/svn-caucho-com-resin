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


package com.caucho.server.port;

import com.caucho.mbeans.server.PortMBean;

import javax.management.ObjectName;

public class PortAdmin
  implements PortMBean
{
  private Port _port;

  public PortAdmin(Port port)
  {
    _port = port;
  }

  public String getObjectName()
  {
    return _port.getObjectName();
  }

  public String getProtocolName()
  {
    return _port.getProtocolName();
  }

  public String getHost()
  {
    return _port.getHost();
  }

  public int getPort()
  {
    return _port.getPort();
  }

  public int getConnectionMax()
  {
    return _port.getConnectionMax();
  }

  public int getKeepaliveMax()
  {
    return _port.getKeepaliveMax();
  }

  public boolean isSSL()
  {
    return _port.isSSL();
  }

  public long getReadTimeout()
  {
    return _port.getReadTimeout();
  }

  public long getWriteTimeout()
  {
    return _port.getWriteTimeout();
  }

  public String getState()
  {
    return _port.getLifecycleState().getStateName();
  }

  public int getActiveThreadCount()
  {
    return _port.getActiveThreadCount();
  }

  public int getIdleThreadCount()
  {
    return _port.getIdleThreadCount();
  }

  public int getKeepaliveThreadCount()
  {
    return _port.getKeepaliveConnectionCount();
  }

  public int getKeepaliveSelectCount()
  {
    return _port.getSelectConnectionCount();
  }

  public long getLifetimeRequestCount()
  {
    return _port.getLifetimeRequestCount();
  }

  public long getLifetimeKeepaliveCount()
  {
    return _port.getLifetimeKeepaliveCount();
  }

  public long getLifetimeClientDisconnectCount()
  {
    return _port.getLifetimeClientDisconnectCount();
  }

  public long getLifetimeRequestTime()
  {
    return _port.getLifetimeRequestTime();
  }

  public long getLifetimeReadBytes()
  {
    return _port.getLifetimeReadBytes();
  }

  public long getLifetimeWriteBytes()
  {
    return _port.getLifetimeWriteBytes();
  }

  public String toString()
  {
    return "PortAdmin[" + getObjectName() + "]";
  }

}
