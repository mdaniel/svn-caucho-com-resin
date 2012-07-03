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

package com.caucho.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bam.Query;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

/**
 * BAM service managing the watchdog
 */
class WatchdogActor
{
  private static final L10N L = new L10N(WatchdogActor.class);
  private static final Logger log
    = Logger.getLogger(WatchdogActor.class.getName());

  private final WatchdogManager _manager;
  // private final String _address;

  WatchdogActor(WatchdogManager manager)
  {
    _manager = manager;
    // _address = address;
    
    // setBroker(broker);
  }

  /**
   * Returns the server id of the watchdog.
   */
  /*
  @Override
  public String getAddress()
  {
    return _address;
  }
  */
  
  /**
   * Returns the home directory of the watchdog for validation.
   */
  public String getResinHome()
  {
    return _manager.getResinHome().getFullPath();
  }

  /**
   * Start queries
   */
  public ResultStatus start(String serverId, String []argv)
  {
    try {
      serverId = _manager.startServer(serverId, argv);

      String msg = L.l("{0}: started server '{1}'", this, serverId);
    
      return new ResultStatus(true, msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      String msg;

      if (e instanceof ConfigException)
        msg = e.getMessage();
      else
        msg = L.l("{0}: start server failed because of exception\n  {1}'",
                  this, e.toString());
    
      return new ResultStatus(false, msg);
    }
  }

  /**
   * Status queries
   */
  public ResultStatus status()
  {
    try {
      String result = _manager.status();
    
      return new ResultStatus(true, result);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: status failed because of exception\n{1}'",
                       this, e.toString());
    
      return new ResultStatus(false, msg);
    }
  }

  /**
   * Handles stop queries
   */
  public ResultStatus stop(String serverId, String []argv)
  {
    ResultStatus result;
    
    try {
      _manager.stopServer(serverId, argv);

      String msg = L.l("{0}: stopped server='{1}'", this, serverId);
    
      result = new ResultStatus(true, msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: stop server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      result = new ResultStatus(false, msg);
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return result;
  }

  /**
   * Handles stop queries
   */
  public ResultStatus restart(String serverId, String []argv)
  {
    ResultStatus result;
    
    try {
      _manager.restartServer(serverId, argv);

      String msg = L.l("{0}: restarted server='{1}'", this, serverId);
    
      result = new ResultStatus(true, msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: restart server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      result = new ResultStatus(false, msg);
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return result;
  }

  /**
   * Handles kill queries
   */
  @Query
  public ResultStatus kill(String serverId)
  {
    ResultStatus result;

    try {
      _manager.killServer(serverId);

      String msg = L.l("{0}: killed server='{1}'", this, serverId);
    
      result = new ResultStatus(true, msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: kill server='{1}' failed because of exception\n{2}'",
                       this, serverId, e.toString());
    
      result = new ResultStatus(false, msg);
    }
    
    if (_manager.isEmpty()) {
      new Thread(new Shutdown()).start();
    }
    
    return result;
  }

  /**
   * Handles shutdown queries
   */
  @Query
  public ResultStatus shutdown()
  {
    try {
      String from = " unknown"; // XXX:
      
      log.info(this + " shutdown from " + from);

      String msg = L.l("{0}: shutdown", this);
    
      _manager.shutdown();
      
      new Thread(new Shutdown()).start();
      
      return new ResultStatus(true, msg);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      
      String msg = L.l("{0}: shutdown failed because of exception\n{2}'",
                       this, e.toString());
    
      return new ResultStatus(false, msg);
    }
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class Shutdown implements Runnable {
    @Override
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
