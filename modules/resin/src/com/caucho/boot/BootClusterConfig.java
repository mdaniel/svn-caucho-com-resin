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
 * @author Scott Ferguson
 */

package com.caucho.boot;

import java.util.ArrayList;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.env.service.ResinSystem;
import com.caucho.server.resin.BootServerMultiConfig;
import com.caucho.util.L10N;
import com.caucho.xml.QName;

public class BootClusterConfig {
  private static final L10N L = new L10N(BootClusterConfig.class);
  
  private ResinSystem _system;
  private BootResinConfig _resin;
  
  private String _id = "";
  private boolean _isDynamicServerEnable;
  
  private ConfigProgram _stdoutLog;
  
  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();
  
  private ArrayList<ConfigProgram> _elasticServerDefaultList
    = new ArrayList<ConfigProgram>();
  
  private ArrayList<WatchdogClient> _serverList
    = new ArrayList<WatchdogClient>();

  BootClusterConfig(ResinSystem system,
                    BootResinConfig resin)
  {
    _system = system;
    _resin = resin;
  }
  
  public BootResinConfig getResin()
  {
    return _resin;
  }

  @Configurable
  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public void setDynamicServerEnable(boolean isEnabled)
  {
    _isDynamicServerEnable = isEnabled;
  }
  
  public boolean isClusterServerEnable()
  {
    return _isDynamicServerEnable;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServerDefault(ContainerProgram program)
  {
    _serverDefaultList.add(program);
  }

  public void addManagement(BootManagementConfig management)
  {
    _resin.setManagement(management);
  }

  public WatchdogConfigHandle createServer()
  {
    WatchdogConfigHandle config
      = new WatchdogConfigHandle(this, _resin.getArgs(), 
                                 _resin.getRootDirectory(),
                                 _serverList.size());
    
    for (int i = 0; i < _serverDefaultList.size(); i++) {
      _serverDefaultList.get(i).configure(config);
    }
    
    /*
    if (_resin.isElasticServer()) {
      for (int i = 0; i < _elasticServerDefaultList.size(); i++) {
        _elasticServerDefaultList.get(i).configure(config);
      }
    }
    */

    return config;
  }

  public WatchdogConfig addServer(WatchdogConfigHandle configHandle)
    throws ConfigException
  {
    WatchdogConfig config = configHandle.configure();
    
    // #5412, server/6e1c
    for (int i = 0; i < _elasticServerDefaultList.size(); i++) {
      _elasticServerDefaultList.get(i).configure(config);
    }
    
    addServerImpl(config);
    
    return config;
  }
    
  public void addServerImpl(WatchdogConfig config)
  {
    // server/6e09
    /*
    if (_resin.isWatchdogManagerConfig())
      return;
      */
    
    if (_resin.findClient(config.getId()) != null) {
      throw new ConfigException(L.l("<server id='{0}'> is a duplicate server.  servers must have unique ids.",
                                    config.getId()));
    }
    
    WatchdogClient client;
    
    if ((client = _resin.findClientByAddress(config.getAddress(), config.getPort())) != null) {
      throw new ConfigException(L.l("<server id='{0}'> has a duplicate address {1}:{2} to server '{3}'.\nServers must have unique addresses.",
                                    config.getId(),
                                    config.getAddress(),
                                    config.getPort(),
                                    client.getId()));
    }
      
    _resin.addServer(config);
    
    client = new WatchdogClient(_system, _resin, config);
    _resin.addClient(client);
    
    _serverList.add(client);
  }
  
  public ArrayList<WatchdogClient> getServerList()
  {
    return _serverList;
  }
  
  public void addServerMulti(BootServerMultiConfig multiServer)
  {
    int index = 0;
    
    _elasticServerDefaultList.add(multiServer.getServerProgram());

    for (String address : multiServer.getAddressList()) {
      WatchdogConfigHandle serverHandle = createServer();
      
      serverHandle.setId(multiServer.getIdPrefix() + index++);
      
      boolean isExternal = false;
      
      if (address.startsWith("ext:")) {
        isExternal = true;
        address = address.substring("ext:".length());
      }
      
      int p = address.lastIndexOf(':');
      int port = multiServer.getPort();
      
      if (p > 0) {
        port = Integer.parseInt(address.substring(p + 1));
        address = address.substring(0, p);
      }
      
      boolean isAllowNonReservedIp = multiServer.isAllowNonReservedIp();
      
      serverHandle.setAddress(address);
      serverHandle.setPort(port);
      // server.setExternalAddress(isExternal);
      
      /*
      if (isAllowNonReservedIp) {
        server.setAllowNonReservedIp(true);
      }
      */
      
      //multiServer.getServerProgram().configure(serverHandle);
      
      WatchdogConfig server = addServer(serverHandle);
      
      multiServer.getServerProgram().configure(server);

      
      // server.setExternalAddress(isExternal);
      /*
      if (isAllowNonReservedIp) {
        server.setAllowNonReservedIp(true);
      }
      */
    }
  }
  
  public ArrayList<WatchdogClient> getClients()
  {
    return _serverList;
  }
  
  public void addStdoutLog(ContainerProgram config)
  {
    _stdoutLog = config;
  }
  
  public ConfigProgram getStdoutLog()
  {
    return _stdoutLog;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
    QName qName = program.getQName();
    
    if (qName != null && qName.getLocalName().equals("ElasticCloudService"))
      _isDynamicServerEnable = true;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
