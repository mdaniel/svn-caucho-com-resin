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

package com.caucho.network.listen;

import com.caucho.cloud.topology.TopologyService;
import com.caucho.env.service.*;

/**
 * The socket poll service, provides nio-style socket listening.
 */
public class SocketPollService extends AbstractResinSubSystem
{
  public static final int START_PRIORITY = TopologyService.START_PRIORITY + 1;

  public SocketPollService()
  {
    
  }
  
  public static SocketPollService createAndAddService()
  {
    ResinSystem system = preCreate(SocketPollService.class);
    
    SocketPollService service = new SocketPollService();
    system.addService(SocketPollService.class, service);
    
    return service;
  }
  
  public static SocketPollService getCurrent()
  {
    return ResinSystem.getCurrentService(SocketPollService.class);
  }
  
  public static AbstractSelectManager getCurrentSelectManager()
  {
    SocketPollService pollService = getCurrent();

    if (pollService != null)
      return pollService.getSelectManager();
    else
      return null;
  }
  
  public AbstractSelectManager getSelectManager()
  {
    return null;
  }
 
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
}
