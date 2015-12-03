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

import java.io.Serializable;
import java.util.logging.Logger;

import com.caucho.bam.Message;
import com.caucho.bam.actor.SimpleActor;
import com.caucho.server.resin.WarningMessage;

/**
 * Service for handling the distributed cache
 */
public class WatchdogChildActor extends SimpleActor
{
  private static final Logger log
    = Logger.getLogger(WatchdogChildActor.class.getName());
  
  private WatchdogChildProcess _child;
  
  WatchdogChildActor(WatchdogChildProcess watchdog)
  {
    setAddress("watchdog");
    
    _child = watchdog;
  }
  
  public Serializable query(Serializable payload)
  {
    return getSender().query("resin@admin.resin.caucho", payload);
  }
  
  public void message(Serializable payload)
  {
    getSender().message("resin@admin.resin.caucho", payload);
  }

  public void sendShutdown()
  {
    getBroker().query(1,
                      "resin@admin.resin.caucho",
                      "watchdog@admin.resin.caucho",
                      new WatchdogStopQuery(""));
  }
  
  @Message
  public void onWarning(String to, String from, WarningMessage warning)
  {
    String msg = warning.getMessage();
    
    log.warning("Watchdog received warning "
                + "from Resin[" + _child.getId() + ",pid=" + _child.getPid() + "]:"
                + "\n  " + warning.getMessage());
    
    if (msg.startsWith("Shutdown:"))
      _child.setShutdownMessage(msg);
  }
  
  /*
  public void destroy()
  {
    _resin.destroy();
  }
  */
}
