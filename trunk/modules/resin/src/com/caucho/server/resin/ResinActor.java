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

package com.caucho.server.resin;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.bam.BamError;
import com.caucho.bam.Message;
import com.caucho.bam.Query;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.boot.PidQuery;
import com.caucho.boot.StartInfoMessage;
import com.caucho.boot.WatchdogStopQuery;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;
import com.caucho.jmx.Jmx;
import com.caucho.util.L10N;

/**
 * Actor communicating with the watchdog.
 */
public class ResinActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(ResinActor.class.getName());

  private static final L10N L = new L10N(ResinActor.class);

  private Resin _resin;
  
  private ShutdownSystem _shutdown;
  
  ResinActor(Resin resin)
  {
    _resin = resin;
    
    setAddress("resin");
    
    _shutdown = ShutdownSystem.getCurrent();
    
    if (_shutdown == null)
      throw new IllegalStateException(L.l("'{0}' requires an active {1}.",
                                          this,
                                          ShutdownSystem.class.getSimpleName()));
  }
  
  /**
   * Sends a warning message to the watchdog
   */
  public void sendWarning(String msg)
  {
    try {
      getBroker().message("watchdog", getAddress(), new WarningMessage(msg));
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  //
  // bam callbacks
  //
  
  /**
   * Startup information
   */
  @Message
  public void startInfo(String to, String from, StartInfoMessage msg)
  {
    _resin.setStartInfo(msg.isRestart(), 
                        msg.getRestartMessage(),
                        msg.getPreviousExitCode());
  }
  
  /**
   * Query for the process pid.
   */
  @Query
  public void queryPid(long id,
                       String to,
                       String from,
                       PidQuery query)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " " + query);
    
    try {
      MBeanServer server = Jmx.getGlobalMBeanServer();
      ObjectName objName = new ObjectName("java.lang:type=Runtime");
      
      String runtimeName = (String) server.getAttribute(objName, "Name");
      
      if (runtimeName == null) {
        getBroker().queryError(id, from, to, query, 
                                   new BamError("null runtime name"));
        return;
      }
      
      int p = runtimeName.indexOf('@');
      
      if (p > 0) {
        int pid = Integer.parseInt(runtimeName.substring(0, p));
      
        getBroker().queryResult(id, from, to, new PidQuery(pid));
        return;
      }
      
      getBroker().queryError(id, from, to, query,
                                 new BamError("malformed name=" + runtimeName));
   
    } catch (Exception e) {
      getBroker().queryError(id, from, to, query,
                                 BamError.create(e));
    }
   }

  @Query
  public void stop(long id,
                   String to,
                   String from,
                   WatchdogStopQuery query)
  {
    log.info(_resin + " stop request from watchdog '" + from + "'");
    
    String msg = L.l("Resin shutdown from watchdog stop '" + from + "'");

    getBroker().queryResult(id, from, to, query);
    
    ShutdownSystem.shutdownActive(ExitCode.OK, msg);
  }

  public void destroy()
  {
    String msg = L.l("Resin shutdown from unexpected watchdog exit.");
    
    ShutdownSystem.shutdownActive(ExitCode.WATCHDOG_EXIT, msg);
  }
}
