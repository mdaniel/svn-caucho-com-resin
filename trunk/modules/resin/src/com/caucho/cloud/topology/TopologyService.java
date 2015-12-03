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

package com.caucho.cloud.topology;

import com.caucho.env.service.*;
import com.caucho.util.L10N;

/**
 * Interface for a service registered with the Resin Server.
 */
public class TopologyService extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = 
    ResinSubSystem.START_PRIORITY_NETWORK_CLUSTER;

  private static final L10N L = new L10N(TopologyService.class);
  
  private final String _serverId;
  private final CloudSystem _system;
  
  public TopologyService(String serverId)
  {
    _serverId = serverId;
    _system = new CloudSystem(serverId);
  }
  
  public static TopologyService createAndAddService(String serverId)
  {
    ResinSystem system = preCreate(TopologyService.class);
    
    TopologyService service = new TopologyService(serverId);
    system.addService(TopologyService.class, service);
    
    return service;
  }
  
  public static TopologyService getCurrent()
  {
    return ResinSystem.getCurrentService(TopologyService.class);
  }
  
  public static CloudSystem getCurrentSystem()
  {
    TopologyService topology = getCurrent();
    
    if (topology == null)
      throw new IllegalStateException(L.l("{0} needs an active {1}",
                                          TopologyService.class.getSimpleName(),
                                          ResinSystem.class.getSimpleName()));
    
    return topology.getSystem();
  }
  
  public static CloudCluster findCluster(String id)
  {
    TopologyService topology = getCurrent();
    
    if (topology == null)
      throw new IllegalStateException(L.l("TopologyService must be active in the ResinSystem"));
    
    return topology.getSystem().findCluster(id);
  }
  
  /**
   * Returns the server with the given id in the active cloud system.
   * 
   * @param id the server id within the system.
   */
  public static CloudServer findServer(String id)
  {
    TopologyService topology = getCurrent();
    
    if (topology == null)
      throw new IllegalStateException(L.l("TopologyService must be active in the ResinSystem"));
    
    return topology.getSystem().findServer(id);
  }

  public CloudSystem getSystem()
  {
    return _system;
  }
  
  public String getId()
  {
    return _serverId;
  }
  
  public CloudServer getSelfServer()
  {
    return _system.findServer(_serverId);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    _system.start();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _system + "]";
  }
}
