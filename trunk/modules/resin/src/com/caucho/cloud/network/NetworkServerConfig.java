/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.cloud.network;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.types.Period;
import com.caucho.network.listen.AbstractProtocol;
import com.caucho.network.listen.TcpSocketLinkListener;
import com.caucho.server.cluster.ProtocolPort;
import com.caucho.server.cluster.ProtocolPortConfig;
import com.caucho.server.http.HttpProtocol;

public class NetworkServerConfig {
  private NetworkListenSystem _listenService;
  
  private ContainerProgram _listenerDefaults = new ContainerProgram();
  private boolean _isKeepaliveSelectEnable = true;

  /**
   * Creates a new servlet server.
   */
  NetworkServerConfig(NetworkListenSystem listenService)
  {
    _listenService = listenService;
  }
  
  private NetworkListenSystem getListenService()
  {
    return _listenService;
  }

  @Configurable
  public TcpSocketLinkListener createClusterPort()
  {
    return getListenService().getClusterListener();
  }
  
  protected TcpSocketLinkListener getClusterListener()
  {
    return getListenService().getClusterListener();
  }
  
  @Configurable
  public void setClusterIdleTime(Period period)
  {
    // XXX: non-configurable
    // getClusterListener().setKeepaliveTimeout(period);
  }
  
  @Configurable
  public void setClusterSocketTimeout(Period period)
  {
    // XXX: non-configurable
    // getClusterListener().setSocketTimeout(period);
  }
  
  @Configurable
  public void setKeepaliveSelectEnable(boolean isEnable)
  {
    _isKeepaliveSelectEnable = isEnable;
    
    if (getClusterListener() != null)
      getClusterListener().setKeepaliveSelectEnable(isEnable);
  }
  
  public boolean isKeepaliveSelectEnable()
  {
    return _isKeepaliveSelectEnable;
  }
  
  @Configurable
  public TcpSocketLinkListener createHttp()
    throws ConfigException
  {
    TcpSocketLinkListener listener = new TcpSocketLinkListener();
    
    applyPortDefaults(listener);

    HttpProtocol protocol = new HttpProtocol();
    listener.setProtocol(protocol);

    getListenService().addListener(listener);

    return listener;
  }

  @Configurable
  public TcpSocketLinkListener createProtocol()
  {
    ProtocolPortConfig port = new ProtocolPortConfig();

    getListenService().addListener(port);

    return port;
  }

  @Configurable
  public TcpSocketLinkListener createListen()
  {
    ProtocolPortConfig listener = new ProtocolPortConfig();

    getListenService().addListener(listener);

    return listener;
  }

  @Configurable
  public void add(ProtocolPort protocolPort)
  {
    TcpSocketLinkListener listener = new TcpSocketLinkListener();

    AbstractProtocol protocol = protocolPort.getProtocol();
    listener.setProtocol(protocol);

    applyPortDefaults(listener);

    protocolPort.getConfigProgram().configure(listener);

    getListenService().addListener(listener);
  }

  /**
   * Adds a port-default
   */
  @Configurable
  public void addPortDefault(ContainerProgram program)
  {
    addListenDefault(program);
  }

  /**
   * Adds a listen-default
   */
  @Configurable
  public void addListenDefault(ConfigProgram program)
  {
    _listenerDefaults.addProgram(program);
  }

  /**
   * If true, ports are bound at end.
   */
  @Configurable
  public void setBindPortsAfterStart(boolean bindAtEnd)
  {
    getListenService().setBindPortsAfterStart(bindAtEnd);
  }
  
  @Configurable
  public void addContentProgram(ConfigProgram builder)
  {
    _listenerDefaults.addProgram(builder);
  }

  private void applyPortDefaults(TcpSocketLinkListener port)
  {
    _listenerDefaults.configure(port);
    
    port.setKeepaliveSelectEnable(isKeepaliveSelectEnable());
  }
}
