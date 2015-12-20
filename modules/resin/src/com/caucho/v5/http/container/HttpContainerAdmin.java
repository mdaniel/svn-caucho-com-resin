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

package com.caucho.v5.http.container;

import com.caucho.v5.bartender.network.NetworkSystem;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.check.MemoryTenuredHealthCheck;
import com.caucho.v5.network.listen.PollTcpManagerBase;

public class HttpContainerAdmin extends HttpAdmin
{
  public HttpContainerAdmin(HttpContainerServlet http)
  {
    super(http);
  }

  @Override
  public long getMemoryFreeMin()
  {
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    
    MemoryTenuredHealthCheck healthCheck
      = healthService.getHealthCheck(MemoryTenuredHealthCheck.class);
    
    if (healthCheck != null)
      return healthCheck.getMemoryFreeMin();
    else 
      return 0;
  }

  @Override
  public long getPermGenFreeMin()
  {
    return 0;
  }

  @Override
  public boolean isSelectManagerEnabled()
  {
    PollTcpManagerBase selectManager 
    = NetworkSystem.getCurrentSelectManager();
  
  return selectManager != null;
  }

  /**
   * Returns the current number of connections that are in the keepalive
   * state and are using select to maintain the connection.
   */
  @Override
  public int getSelectKeepaliveCount()
  {
    PollTcpManagerBase selectManager 
      = NetworkSystem.getCurrentSelectManager();

    if (selectManager != null)
      return selectManager.getSelectCount();
    else
      return 0-1;
  }
}
