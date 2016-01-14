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

package com.caucho.v5.http.pod;

import java.util.logging.Logger;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.util.ModulePrivate;

/**
 * Configuration for a pod.cf server
 */
@ModulePrivate
public class ServerPodConfig
{
  private static final Logger log = Logger.getLogger(ServerPodConfig.class.getName());
  
  private String _address;
  private int _port;

  private String _machineHash = "";
  
  public ServerPodConfig()
  {
  }

  @ConfigArg(0)
  public void setArg0(String value)
  {
    for (int i = 0; i < value.length(); i++) {
      if (! Character.isDigit(value.charAt(i))) {
        setAddress(value);
        return;
      }
    }
    
    _port = Integer.parseInt(value);
  }

  public void setAddress(String address)
  {
    _address = address;
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
  
  public void setMachineHash(String machineHash)
  {
    if (machineHash == null) {
      machineHash = "";
    }
    
    _machineHash = machineHash;
  }
  
  public String getMachine()
  {
    return _machineHash;
  }

  /**
   * Adds any other item as a config program for the PodAppConfig 
   */
  @Configurable
  public void addBuilderProgram(ConfigProgram program)
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getAddress() + "," + getPort() + "," + _machineHash + "]";
  }
}
