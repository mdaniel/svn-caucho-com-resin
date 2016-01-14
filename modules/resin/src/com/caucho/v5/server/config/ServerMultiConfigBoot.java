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

import java.util.ArrayList;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;

/**
 * Configures a set of servers.
 */
public class ServerMultiConfigBoot
{
  private String _idPrefix;
  private ArrayList<String> _addressList = new ArrayList<>();
  private int _port;
  private ContainerProgram _serverProgram = new ContainerProgram();
  
  @Configurable
  public void addAddresses(String []list)
  {
    if (list != null) {
      for (String address : list) {
        _addressList.add(address);
      }
    }
  }
  
  @Configurable
  public void addAddressList(String []list)
  {
    addAddresses(list);
  }

  public Iterable<String>  getAddressList()
  {
    return _addressList;
  }
  
  @Configurable
  public void setIdPrefix(String idPrefix)
  {
    _idPrefix = idPrefix;
  }

  public String  getIdPrefix()
  {
    return _idPrefix;
  }
  
  public void setPort(int port)
  {
    _port = port;
  }

  public int getPort()
  {
    return _port;
  }

  public ConfigProgram getServerProgram()
  {
    return _serverProgram;
  }
  
  public void addProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }
}
