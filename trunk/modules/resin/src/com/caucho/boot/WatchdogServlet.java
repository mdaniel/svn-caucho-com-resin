/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.boot;

import com.caucho.config.ConfigException;
import com.caucho.hessian.server.HessianServlet;
import com.caucho.util.*;

import java.util.logging.Logger;

/**
 * Process responsible for watching a backend server.
 */
public class WatchdogServlet extends HessianServlet
  implements WatchdogAPI
{
  private final static L10N L = new L10N(WatchdogServlet.class);
  private static final Logger log
    = Logger.getLogger(WatchdogServlet.class.getName());

  private WatchdogManager _watchdogManager;

  @Override
  public void init()
  {
    _watchdogManager = WatchdogManager.getWatchdog();
  }
    
  public String status(String password)
    throws ConfigException, IllegalStateException
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog status authentication failure");
      throw new ConfigException(L.l("watchdog start forbidden - authentication failed."));
    }
    
    return _watchdogManager.status();
  }
    
  public void start(String password, String []argv)
    throws ConfigException, IllegalStateException
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog start authentication failure");
      throw new ConfigException(L.l("watchdog start forbidden - authentication failed."));
    }
    
    _watchdogManager.startServer(argv);
  }
  
  public void restart(String password, String serverId, String []argv)
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog restart authentication failure");
      throw new ConfigException(L.l("watchdog restart forbidden - authentication failed"));
    }
    
    _watchdogManager.restartServer(serverId, argv);
  }

  public void stop(String password, String serverId)
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog stop authentication failure");
      throw new ConfigException(L.l("watchdog stop forbidden - authentication failed"));
    }
    
    log.info("Watchdog stop: " + serverId);
    
    _watchdogManager.stopServer(serverId);
  }

  public void kill(String password, String serverId)
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog kill authentication failure");
      throw new ConfigException(L.l("watchdog kill forbidden - authentication failed"));
    }
    
    log.info("Watchdog kill: " + serverId);
    
    _watchdogManager.killServer(serverId);
  }
  
  public boolean shutdown(String password)
  {
    if (! _watchdogManager.authenticate(password)) {
      log.warning("watchdog stop authentication failure");
      throw new ConfigException(L.l("watchdog shutdown forbidden - authentication failed"));
    }
    
    log.info("Watchdog shutdown");

    new Thread(new Shutdown()).start();

    return true;
  }

  static class Shutdown implements Runnable {
    public void run()
    {
      try {
	Thread.sleep(1000);
      } catch (Exception e) {
      }

      System.exit(0);
    }
  }
}
