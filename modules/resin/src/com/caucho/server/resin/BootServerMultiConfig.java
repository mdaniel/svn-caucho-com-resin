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

package com.caucho.server.resin;

import java.util.ArrayList;

import javax.annotation.PostConstruct;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.annotation.NoAspect;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.util.L10N;

/**
 * The BootServerConfig is the first-pass configuration of the server.
 * 
 * It matches the &lt;server-multi> tag in the resin.xml
 */

@NoAspect
public class BootServerMultiConfig 
{
  private static final L10N L = new L10N(BootServerMultiConfig.class);
  
  private String _idPrefix = null;
  
  private String _addresses;
  private int _port = -1;
  private ArrayList<String> _addressList = new ArrayList<String>();
  private boolean _isSecure;
  private boolean _isAllowNonReservedIp;

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  /**
   * Creates a new resin server.
   */
  public BootServerMultiConfig()
  {
  }
  
  /**
   * Returns the server's id-prefix
   */
  public String getIdPrefix()
  {
    return _idPrefix;
  }
  
  /**
   * Sets the cluster's id
   */
  @Configurable
  public void setIdPrefix(String id)
  {
    _idPrefix = id;
  }
  
  @Configurable
  public void setAddressList(String addresses)
  {
    _addresses = addresses;
  }
  
  public ArrayList<String> getAddressList()
  {
    return _addressList;
  }
  
  public int getPort()
  {
    return _port;
  }
  
  @Configurable
  public void setPort(int port)
  {
    _port = port;
  }
  
  @Configurable
  public void setClusterPort(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
    
    ClusterPortConfig cfg = new ClusterPortConfig();
    
    program.configure(cfg);

    if (cfg.getPort() >= 0) {
      setPort(cfg.getPort());
    }
    
  }
  
  public boolean isSecure()
  {
    return _isSecure;
  }
  
  public void setAllowNonReservedIp(boolean isAllow)
  {
    _isAllowNonReservedIp = isAllow;
  }
  
  public boolean isAllowNonReservedIp()
  {
    return _isAllowNonReservedIp;
  }
  
  public void addBuilderProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }
  
  public ConfigProgram getServerProgram()
  {
    return _serverProgram;
  }
  
  @PostConstruct
  public void init()
  {
    if (_idPrefix == null)
      throw new ConfigException(L.l("server-multi requires an id-prefix"));
    
    fillAddresses();
  }
  
  private void fillAddresses()
  {
    if (_addresses == null) {
      return;
    }
    
    ArrayList<String> addressList = new ArrayList<String>();
    
    for (String item : _addresses.split("[\\s;]+")) {
      if ("".equals(item))
        continue;
      
      int p = item.lastIndexOf(':');
      
      if (p > 0) {
        addressList.add(item);
      }
      else if (_port <= 0) {
        throw new ConfigException(L.l("port is required in server-multi when address-list {0} does not define the port.",
                                      _addresses));
      }
      else {
        addressList.add(item + ":" + _port);
      }
    }
    
    _addressList = addressList;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _idPrefix + "]";
  }
  
  static class ClusterPortConfig {
    private int _port = -1;
    
    public int getPort()
    {
      return _port;
    }
    
    public void setPort(int port)
    {
      _port = port;
    }
    
    public ClusterPortConfig createClusterPort()
    {
      return this;
    }
    
    public void addBuilderProgram(ConfigProgram program)
    {
    }
  }
}