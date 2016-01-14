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

package com.caucho.v5.server.config;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.NoAspect;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;

/**
 * Saves server configuration so server-defaults are always applied to
 * all servers.
 */
@NoAspect
@Configurable
public class ServerConfigBootProgram
{
  private String _id = "";
  private String _address = "";
  private int _port = -1;
  
  private ContainerProgram _program = new ContainerProgram();

  @Configurable
  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }
  
  @ConfigArg(0)
  public void setAddress(String address)
  {
    if (address == null || "".equals(address)) {
      return;
    }
    
    int p = address.lastIndexOf(':');
    int ipv6tail = address.lastIndexOf(']');
    
    if (p > 0 && ipv6tail < p) {
      String port = address.substring(p + 1);
      
      _address = address.substring(0, p);
      _port = Integer.parseInt(port);
    }
    else if (address.indexOf('.') >= 0 || ipv6tail > 0) {
      _address = address;
    }
    else if (Character.isDigit(address.charAt(0))) {
      _port = Integer.parseInt(address);
    }
    else {
      _address = address;
    }
    
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  @ConfigArg(1)
  public void setPort(int port)
  {
    _port = port;
  }
  
  public int getPort()
  {
    return _port;
  }
  
  public void addContentProgram(ConfigProgram program)
  {
    _program.addProgram(program);
  }
  
  public ConfigProgram getProgram()
  {
    return _program;
  }
  
  //
  // initialization
  //

  public ServerConfigBoot createServerConfig(ClusterConfigBoot cluster)
  {
    return new ServerConfigBoot(cluster, getId(), _address, _port);
  }
  
  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _id 
            + "," + getAddress()
            + ":" + getPort()
            + "]");
  }
}
