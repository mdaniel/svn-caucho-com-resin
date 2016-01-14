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

package com.caucho.v5.cli.server;

import java.util.Objects;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.config.ConfigContext;
import com.caucho.v5.vfs.PathImpl;

public class ServerELContext<T extends ArgsDaemon>
{
  private final T _args;
  
  public ServerELContext(T args)
  {
    Objects.requireNonNull(args);
    
    _args = args;
  }
  
  protected T getArgs()
  {
    return _args;
  }
  
  public PathImpl getHomeDirectory()
  {
    return getArgs().getHomeDirectory();
  }

  public PathImpl getRootDirectory()
  {
    return getArgs().getRootDirectory();
  }

  public PathImpl getDataDirectory()
  {
    return getArgs().getDataDirectory();
  }

  public PathImpl getLogDirectory()
  {
    return getArgs().getLogDirectory();
  }

  public PathImpl getConfigPath()
  {
    return getArgs().getConfigPath();
  }

  public String getServerId()
  {
    return getArgs().getServerId();
  }

  public void setProperties(ConfigContext config)
  {
    
    ConfigContext.setProperty("homeDir", getHomeDirectory());
    //Config.setProperty("java", getJavaVar());
    //Config.setProperty("resin", elContext.getResinVar());
    //Config.setProperty("server", elContext.getServerVar());
    ConfigContext.setProperty("system", System.getProperties());
    ConfigContext.setProperty("getenv", System.getenv());

    if (getArgs().getServerPort() > 0) {
      ConfigContext.setProperty("server_port", getArgs().getServerPort());
    }

    if (getArgs().getWatchdogPort() > 0) {
      ConfigContext.setProperty("watchdog_port", getArgs().getWatchdogPort());
    }
    
    // server/4342
    ConfigContext.setProperty("server_id", getArgs().getServerId());
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
