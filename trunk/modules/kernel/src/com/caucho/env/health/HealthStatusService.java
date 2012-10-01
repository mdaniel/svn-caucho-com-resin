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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.*;

import com.caucho.env.service.*;
import com.caucho.util.L10N;

/**
 * A service that any component can use to send notifications about health 
 * status changes.  Listeners can register to receive these events.
 * 
 * @author paul
 *
 */
public class HealthStatusService extends AbstractResinSubSystem 
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
    ResinSystem system = preCreate(HealthStatusService.class);
    
    HealthStatusService service = new HealthStatusService();
    system.addService(HealthStatusService.class, service);
    
    return service;
  }
  
  public static HealthStatusService getCurrent()
  {
    return ResinSystem.getCurrentService(HealthStatusService.class);
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
