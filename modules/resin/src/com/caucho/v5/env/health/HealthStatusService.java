/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.subsystem.SubSystemBase;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.L10N;

/**
 * A service that any component can use to send notifications about health 
 * status changes.  Listeners can register to receive these events.
 * 
 * @author paul
 *
 */
public class HealthStatusService extends SubSystemBase 
{
  public static final int START_PRIORITY = 1;

  private static final Logger log = 
    Logger.getLogger(HealthStatusService.class.getName());
  private static final L10N L = new L10N(HealthStatusService.class);

  private List<HealthStatusListener> _listeners = 
    new CopyOnWriteArrayList<HealthStatusListener>();
  
  private HealthStatusService()
  {
    
  }
  
  public static HealthStatusService createAndAddService()
  {
    SystemManager system = preCreate(HealthStatusService.class);
    
    HealthStatusService service = new HealthStatusService();
    system.addSystem(HealthStatusService.class, service);
    
    return service;
  }
  
  public static HealthStatusService getCurrent()
  {
    return SystemManager.getCurrentSystem(HealthStatusService.class);
  }
  
  /**
   * Notify all HealthStatusListeners about a change in health status.
   * @param source object generating the notification; usually "this"
   * @param status new health status
   * @param message health status message
   */
  public void updateHealthStatus(Object source, 
                                 HealthStatus status, String message)
  {
    for (HealthStatusListener listener : _listeners)
      listener.updateHealthStatus(source, status, message);
    
    if (log.isLoggable(Level.FINE)) {
      String msg = L.l("Health status {0} from {1}: {2}",
                       status, source.getClass().getSimpleName(), message); 
      log.log(Level.FINE, msg);
    }
  }

  /**
   * Notify all HealthStatusListeners about a change in health status.
   * @param source object generating the notification; usually "this"
   * @param status new health status
   * @param message health status message
   */
  public static void updateCurrentHealthStatus(Object source, 
                                               HealthStatus status, 
                                               String message)
  {
    HealthStatusService service = getCurrent();
    
    if (service != null)
      service.updateHealthStatus(source, status, message);
    else {
      // this will only happen if HealthStatusService is not registered for some reason
      String msg = L.l("Health status {0} from {1}: {2}",
                       status, source.getClass().getSimpleName(), message); 
      log.warning(msg);
    }
  }

  /**
   * Registers a HealthStatusListener to receive health status notifications
   * @param listener
   */
  public void addHealthStatusListener(HealthStatusListener listener)
  {
    _listeners.add(listener);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
}
