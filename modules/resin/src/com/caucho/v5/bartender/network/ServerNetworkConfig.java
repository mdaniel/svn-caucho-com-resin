/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.bartender.network;

import java.util.logging.Logger;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configs;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.http.container.ProtocolPort;
import com.caucho.v5.http.protocol.ProtocolHttp;
import com.caucho.v5.network.NetworkSystemBartender;
import com.caucho.v5.network.port.PortTcp;
import com.caucho.v5.network.port.PortTcpBuilder;
import com.caucho.v5.server.config.ServerConfigBoot;

import io.baratine.config.Config;

public class ServerNetworkConfig {
  private static final Logger log
    = Logger.getLogger(ServerNetworkConfig.class.getName());
  
  private NetworkSystemBartender _networkSystem;
  
  private ContainerProgram _portDefaults = new ContainerProgram();
  private boolean _isKeepaliveSelectEnable = true;

  private ServerConfigBoot _serverConfig;

  private Config _env;

  /**
   * Creates a new servlet server.
   */
  ServerNetworkConfig(NetworkSystemBartender listenService,
                      ServerConfigBoot serverConfig)
  {
    _networkSystem = listenService;
    _serverConfig = serverConfig;
    
    _env = Configs.config().get();
  }
  
  private NetworkSystemBartender getNetworkSystem()
  {
    return _networkSystem;
  }

  /* XXX:
  @Configurable
  public TcpPort createClusterPort()
  {
    return getListenService().getClusterPort();
  }
  */
  
  protected PortTcp getClusterPort()
  {
    return getNetworkSystem().clusterPort();
  }
  
  @Configurable
  public void setClusterIdleTime(ConfigProgram program)
  {
    // XXX: getListenService().addClusterPortConfig(program);
    /*
    TcpPort clusterPort = getClusterListener();
    
    if (clusterPort != null) {
      clusterPort.setKeepaliveTimeout(period);
    }
    */
  }
  
  @Configurable
  public void setClusterSocketTimeout(ConfigProgram program)
  {
    // XXX: getListenService().addClusterPortConfig(program);
    
    /*
    TcpPort clusterPort = getClusterListener();
    
    if (clusterPort != null) {
      // server/0651
      clusterPort.setSocketTimeout(period);
    }
    */
  }
  
  public void setAllowForeignIp(boolean isAllow)
  {
    // XXX: getNetworkSystem().setAllowForeignIp(isAllow);
  }
  
  @Configurable
  public void setKeepaliveSelectEnable(boolean isEnable)
  {
    _isKeepaliveSelectEnable = isEnable;
    
    // getListenService().addClusterPortConfig(program);

    /*
    if (getClusterPort() != null) {
      getClusterPort().setPollEnable(isEnable);
    }
    */
  }
  
  public boolean isPollEnable()
  {
    return _isKeepaliveSelectEnable;
  }
  
  @Configurable
  public PortTcp createHttp()
    throws ConfigException
  {
    PortTcpBuilder portBuilder = new PortTcpBuilder(_env);
    
    portBuilder.protocol(new ProtocolHttp());

    PortTcp listener = new PortTcp(portBuilder);
    
    applyPortDefaults(listener);

    return listener;
  }
  
  public void addHttp(PortTcp listener)
  {
    if (listener.port() <= 0) { // && listener.getSocketPath() == null) {
      log.fine(listener + " skipping because port is 0.");
      return;
    }

    getNetworkSystem().addPort(listener);
  }
  
  public void addListen(PortTcp listener)
  {
    if (listener.port() <= 0) { // && listener.getSocketPath() == null) {
      log.fine(listener + " skipping because port is 0.");
      return;
    }
    
    getNetworkSystem().addPort(listener);
  }

  @Configurable
  public void add(ProtocolPort protocolPort)
  {
    PortTcpBuilder portBuilder = new PortTcpBuilder(_env);
    portBuilder.protocol(protocolPort.getProtocol());
    
    PortTcp port = new PortTcp(portBuilder);

    applyPortDefaults(port);

    protocolPort.getConfigProgram().configure(port);

    getNetworkSystem().addPort(port);
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
    _portDefaults.addProgram(program);
  }

  @Configurable
  public void addContentProgram(ConfigProgram builder)
  {
    //System.out.println("CP: " + builder);
    _portDefaults.addProgram(builder);
  }

  private void applyPortDefaults(PortTcp port)
  {
    _serverConfig.configurePort(port);

    _portDefaults.configure(port);
    
    //port.setPollEnable(isPollEnable());
  }
}
