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

import javax.annotation.PostConstruct;

import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.program.ContainerProgram;
import com.caucho.v5.util.L10N;

@Configurable
public class PodConfigBoot
{
  private static final L10N L = new L10N(PodConfigBoot.class);
  
  public static final String DEFAULT_NAME = "web";
  
  private String _id;
  
  private PodBartender.PodType _type; // = PodBartender.PodType.solo;
  
  private ServerConfigContainer _serverContainer
    = new ServerConfigContainer();
  
  @ConfigArg(0)
  public void setId(String id)
  {
    _id = id;
  }
  
  public String getId()
  {
    return _id;
  }
  
  @ConfigArg(1)
  public void setType(PodBartender.PodType type)
  {
    Objects.requireNonNull(type);
    
    _type = type;
  }
  
  public PodBartender.PodType getType()
  {
    return _type;
  }
  
  /**
   * Adds a server to the pod.
   */
  @Configurable
  public void addServer(ServerConfigBootProgram server)
  {
    _serverContainer.addServer(server);
  }
  
  /**
   * Adds default server configuration to the pod.
   */
  @Configurable
  public void addServerDefault(ConfigProgram program)
  {
    _serverContainer.addServerDefault(program.toContainer());
  }
  
  @PostConstruct
  public void init()
  {
    if (_id == null) {
      throw new ConfigException(L.l("pod requires an id"));
    }
  }
  
  void initServers(ClusterConfigBoot cluster)
  {
    _serverContainer.initServers(cluster);
  }

  public ArrayList<ServerConfigBoot> getServers()
  {
    return _serverContainer.getServers();
  }

  public ContainerProgram getServerDefault()
  {
    return _serverContainer.getServerDefault();
  }
}
