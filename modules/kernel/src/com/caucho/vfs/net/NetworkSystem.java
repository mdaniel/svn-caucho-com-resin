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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.vfs.net;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import com.caucho.loader.EnvironmentLocal;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.QSocket;

/**
 * Abstract network system.
 */
abstract public class NetworkSystem {
  private static final EnvironmentLocal<NetworkSystem> _localSystem
    = new EnvironmentLocal<NetworkSystem>();
  
  private static final TcpNetworkSystem _tcpSystem = new TcpNetworkSystem();
  
  public static void setLocal(NetworkSystem value)
  {
    _localSystem.set(value);
  }
  
  public static NetworkSystem getCurrent()
  {
    NetworkSystem system = _localSystem.get();
    
    if (system == null) {
      system = _tcpSystem;
    }
    
    return system;
  }
  
  public static NetworkSystem createSubSystem(String name)
  {
    NetworkSystem system = getCurrent();
    
    NetworkSystem subSystem = system.createSubSystemImpl(name);
    
    _localSystem.set(subSystem);
    
    return subSystem;
  }
  
  protected NetworkSystem createSubSystemImpl(String name)
  {
    return this;
  }
  
  public QServerSocket openServerSocket(String address, int port)
    throws IOException
  {
    InetAddress inetAddr = InetAddress.getByName(address);
      
    return openServerSocket(inetAddr, port, 100, true);
  }
  
  public abstract QServerSocket openServerSocket(InetAddress address,
                                                 int port,
                                                 int backlog,
                                                 boolean isJni)
    throws IOException;
  
  public QSocket connect(String address, int port)
    throws IOException
  {
    InetAddress inetAddr = InetAddress.getByName(address);
    
    return connect(inetAddr, port, -1);
  }
  
  public final QSocket connect(InetAddress address,
                               int port,
                               long timeout)
    throws IOException
  {
    return connect(new InetSocketAddress(address, port), timeout);
  }
  
  public abstract QSocket connect(InetSocketAddress address,
                                  long timeout)
    throws IOException;
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

