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
 * @author Sam
 */


package com.caucho.network.listen;

import com.caucho.inject.Module;
import com.caucho.management.server.*;

@Module
public class TcpPortAdmin extends AbstractManagedObject
  implements PortMXBean
{
  private TcpPort _port;

  public TcpPortAdmin(TcpPort port)
  {
    _port = port;
  }

  @Override
  public String getName()
  {
    String addr = _port.getAddress();

    if (addr == null)
      addr = "INADDR_ANY";
    
    return addr + '-' + _port.getPort();
  }

  @Override
  public String getProtocolName()
  {
    return _port.getProtocolName();
  }

  @Override
  public String getAddress()
  {
    return _port.getAddress();
  }

  @Override
  public int getPort()
  {
    return _port.getPort();
  }

  @Override
  public boolean isSSL()
  {
    return _port.isSSL();
  }
  
  //
  // Config
  //

  @Override
  public int getAcceptThreadMin()
  {
    return _port.getAcceptThreadMin();
  }

  @Override
  public int getAcceptThreadMax()
  {
    return _port.getAcceptThreadMax();
  }

  @Override
  public int getAcceptListenBacklog()
  {
    return _port.getAcceptListenBacklog();
  }

  @Override
  public int getConnectionMax()
  {
    return _port.getConnectionMax();
  }

  @Override
  public int getPortThreadMax()
  {
    return _port.getPortThreadMax();
  }

  @Override
  public int getKeepaliveMax()
  {
    return _port.getKeepaliveMax();
  }

  @Override
  public int getKeepaliveSelectMax()
  {
    return _port.getKeepaliveSelectMax();
  }

  @Override
  public long getKeepaliveConnectionTimeMax()
  {
    return _port.getKeepaliveConnectionTimeMax();
  }
  
  @Override
  public long getKeepaliveThreadTimeout()
  {
    return _port.getKeepaliveThreadTimeout();
  }

  @Override
  public long getKeepaliveTimeout()
  {
    return _port.getKeepaliveTimeout();
  }

  @Override
  public long getSocketTimeout()
  {
    return _port.getSocketTimeout();
  }
  
  @Override
  public boolean isTcpKeepalive()
  {
    return _port.isTcpKeepalive();
  }
  
  @Override
  public boolean isTcpNoDelay()
  {
    return _port.isTcpNoDelay();
  }

  @Override
  public long getSuspendTimeMax()
  {
    return _port.getSuspendTimeMax();
  }

  @Override
  public String getState()
  {
    return _port.getLifecycleState().getStateName();
  }

  @Override
  public int getThreadCount()
  {
    return _port.getThreadCount();
  }

  @Override
  public int getThreadActiveCount()
  {
    return _port.getActiveThreadCount();
  }

  @Override
  public int getThreadIdleCount()
  {
    return _port.getIdleThreadCount();
  }

  @Override
  public int getThreadStartCount()
  {
    return _port.getStartThreadCount();
  }
  
  @Override
  public boolean isJniEnabled()
  {
    return _port.isJniEnabled();
  }

  @Override
  public int getKeepaliveCount()
  {
    return _port.getKeepaliveConnectionCount();
  }

  @Override
  public int getKeepaliveThreadCount()
  {
    return _port.getKeepaliveThreadCount();
  }

  @Override
  public int getKeepaliveSelectCount()
  {
    return _port.getSelectConnectionCount();
  }

  @Override
  public int getCometIdleCount()
  {
    return _port.getCometIdleCount();
  }
  
  @Override
  public long getRequestCountTotal()
  {
    return _port.getLifetimeRequestCount();
  }

  @Override
  public long getKeepaliveCountTotal()
  {
    return _port.getLifetimeKeepaliveCount();
  }

  @Override
  public long getKeepaliveSelectCountTotal()
  {
    return _port.getLifetimeKeepaliveSelectCount();
  }

  @Override
  public long getClientDisconnectCountTotal()
  {
    return _port.getLifetimeClientDisconnectCount();
  }
  
  @Override
  public long getThrottleDisconnectCountTotal()
  {
    return _port.getLifetimeThrottleDisconnectCount();
  }

  @Override
  public long getRequestTimeTotal()
  {
    return _port.getLifetimeRequestTime();
  }

  @Override
  public long getReadBytesTotal()
  {
    return _port.getLifetimeReadBytes();
  }

  @Override
  public long getWriteBytesTotal()
  {
    return _port.getLifetimeWriteBytes();
  }

  //
  // Operations
  //
  
  /**
   * Enable the port, letting it listening to new requests.
   */
  @Override
  public void start()
  {
    _port.enable();
  }
  
  /**
   * Disable the port, stopping it from listening to new requests.
   */
  @Override
  public void stop()
  {
    _port.disable();
  }

  /**
   * returns information for all the port's connections
   */
  @Override
  public TcpConnectionInfo []connectionInfo()
  {
    return _port.getActiveConnections();
  }

  void register()
  {
    registerSelf();
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
