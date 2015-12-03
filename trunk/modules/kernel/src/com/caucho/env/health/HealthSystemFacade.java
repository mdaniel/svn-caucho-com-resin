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

package com.caucho.env.health;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.shutdown.ShutdownSystem;

/**
 * A facade for sending health events.
 */
public class HealthSystemFacade {
  private static final Logger log
    = Logger.getLogger(HealthSystemFacade.class.getName());
  
  public static final String RESIN_EXIT_MESSAGE = "resin.exit.message";
  
  private static final HealthSystemFacade _facade;
  
  protected HealthSystemFacade()
  {
  }
  
  public static String getExitMessage()
  {
    String msg = System.getProperty(RESIN_EXIT_MESSAGE);
    
    if (msg != null)
      return msg;
    else
      return "";
  }
  
  public static void fireEvent(String eventName, String eventMessage)
  {
    _facade.fireEventImpl(eventName, eventMessage);
  }
  
  public static void fireFatalEvent(String eventName, String eventMessage)
  {
    _facade.fireFatalEventImpl(eventName, eventMessage);
  }
  
  protected void fireEventImpl(String eventName, String eventMessage)
  {
  }
  
  protected void fireFatalEventImpl(String eventName, String eventMessage)
  {
    ShutdownSystem.shutdownActive(ExitCode.HEALTH, eventName + ": " + eventMessage);
  }
  
  static {
    HealthSystemFacade facade = new HealthSystemFacade();
    
    try {
      Class<?> cl = Class.forName("com.caucho.env.health.ProHealthSystemFacade");
      
      facade = (HealthSystemFacade) cl.newInstance();
    } catch (ClassNotFoundException e) {
      log.log(Level.ALL, e.toString(), e);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
    
    _facade = facade;
  }
}
