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
import java.util.Objects;

import com.caucho.v5.config.program.ContainerProgram;

class ServerConfigContainer
{
  private ContainerProgram _serverDefault = new ContainerProgram();
  
  private ArrayList<ServerConfigBootProgram> _pendingServerList
    = new ArrayList<>();
  
  private ArrayList<ServerConfigBoot> _serverList
    = new ArrayList<>();
  
  //
  // configuration
  //

  /**
   * Adds server-default program.
   */
  void addServerDefault(ContainerProgram program)
  {
    _serverDefault.addProgram(program);
  }

  /**
   * Adds server-default program.
   */
  ContainerProgram getServerDefault()
  {
    return _serverDefault;
  }
  
  void addServer(ServerConfigBootProgram server)
  {
    Objects.requireNonNull(server);
    
    _pendingServerList.add(server);
  }
  
  /**
   * initialization
   */
  void initServers(ClusterConfigBoot cluster)
  {
    for (ServerConfigBootProgram serverProgram : _pendingServerList) {
      ServerConfigBoot server = findServer(serverProgram);
      
      if (server == null) {
        server = serverProgram.createServerConfig(cluster);
        
        _serverList.add(server);
        
        cluster.addServerForInit(server);
        
        _serverDefault.configure(server);
      }
      
      serverProgram.getProgram().configure(server);
    }

    _pendingServerList.clear();
  }
  
  private ServerConfigBoot findServer(ServerConfigBootProgram serverProgram)
  {
    return null;
  }
  
  public ArrayList<ServerConfigBoot> getServers()
  {
    return _serverList;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
