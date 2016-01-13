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

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.server.container.ServerContainerConfig;

/**
 * ServletContainer configration from the <server> tags.
 */
@Configurable
public class ServerConfig 
{
  private ServerContainerConfig _config;

  /**
   * Creates a new servlet server.
   */
  public ServerConfig(ServerContainerConfig config)
  {
    _config = config;
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setMemoryFreeMin(Bytes min)
  {
    _config.setMemoryFreeMin(min);
  }

  /**
   * Sets the minimum free memory after a GC
   */
  @Configurable
  public void setPermGenFreeMin(Bytes min)
  {
    _config.setPermGenFreeMin(min);
  }

  /**
   * Sets the max wait time for shutdown.
   */
  @Configurable
  public void setShutdownWaitMax(Period waitTime)
  {
    _config.setShutdownWaitMax(waitTime);
  }

  /**
   * Sets the maximum thread-based keepalive
   */
  @Configurable
  public void setThreadMax(int max)
  {
    _config.setThreadMax(max);
  }

  /**
   * Sets the maximum executor (background) thread.
   */
  @Configurable
  public void setThreadExecutorTaskMax(int max)
  {
    _config.setThreadExecutorTaskMax(max);
  }
  
  @Configurable
  public void setSendfileEnable(boolean isEnable)
  {
    _config.setSendfileEnable(isEnable);
  }
  
  @Configurable
  public void setSendfileMinLength(Bytes bytes)
  {
    _config.setSendfileMinLength(bytes);
  }
  
  public void addContentProgram(ConfigProgram program)
  {
  }
}
