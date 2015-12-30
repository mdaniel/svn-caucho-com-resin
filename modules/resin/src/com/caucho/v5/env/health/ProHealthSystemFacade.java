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

package com.caucho.v5.env.health;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.caucho.v5.health.HealthSystemFacade;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * A facade for sending health events.
 */
public class ProHealthSystemFacade extends HealthSystemFacade 
{
  private static final Logger log
    = Logger.getLogger(ProHealthSystemFacade.class.getName());
  private static final L10N L = new L10N(ProHealthSystemFacade.class);
  
  private long _lastWarningTime;
  private long _warningTimeout = TimeUnit.SECONDS.toMillis(60);
  
  @Override
  protected void fireEventImpl(String eventName, String eventMessage)
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();
    
    try {
      ClassLoader loader = oldLoader;
      
      if (! (loader instanceof EnvironmentClassLoader))
        loader = EnvLoader.getEnvironmentClassLoader();
      
      HealthSubSystem healthSystem = HealthSubSystem.getCurrent();
    
      if (healthSystem != null) {
        healthSystem.fireEvent(eventName);
      }
      else if (_lastWarningTime + _warningTimeout < CurrentTime.getCurrentTime()) {
        _lastWarningTime = CurrentTime.getCurrentTime();
        
        log.fine(L.l("Failed to fire HealthEvent {0}: HealthSystem is not started", eventName));
      }
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }
  
  @Override
  protected void fireFatalEventImpl(String eventName,
                                    String eventMessage)
  {
    ShutdownSystem.startFailsafe(eventMessage);
    
    fireEventImpl(eventName, eventMessage);
    
    ShutdownSystem.shutdownActive(ExitCode.HEALTH,
                                  eventName + ": " + eventMessage);
  }
}
