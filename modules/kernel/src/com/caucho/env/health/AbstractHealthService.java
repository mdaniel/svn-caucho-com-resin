/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;

abstract public class AbstractHealthService extends AbstractResinService {
  private static final Logger log 
    = Logger.getLogger(AbstractHealthService.class.getName());
  
  private static final Class<? extends AbstractHealthService> _healthServiceClass;
  
  private static AbstractHealthService create()
  {
    if (_healthServiceClass == null)
      return null;

    ResinSystem system = ResinSystem.getCurrent();

    AbstractHealthService healthService = null;
    
    if (system != null) {
      healthService = system.getService(_healthServiceClass);
      
      if (healthService == null) {
        try {
          healthService = (AbstractHealthService) _healthServiceClass.newInstance();
          
          system.addService(healthService);
          
          healthService = system.getService(_healthServiceClass);
        } catch (Exception e) {
          // exception might be thrown for License failure
          
          log.log(Level.FINER, e.toString(), e);
        }
      }
    }
    
    return healthService;
  }
  
  public static <T extends HealthCheck> T
  getCurrentHealthCheck(Class<T> healthCheck)
  {
    AbstractHealthService healthService = create();
    
    if (healthService != null)
      return healthService.getHealthCheck(healthCheck);
    else
      return null;
  }
  
  public static <T extends HealthCheck>
  T addCurrentHealthCheck(T healthCheck)
  {
    AbstractHealthService healthService = create();
    
    if (healthService != null)
      return healthService.addHealthCheck(healthCheck);
    else
      return healthCheck;
  }
  
  abstract protected <T extends HealthCheck>
  T getHealthCheck(Class<T> healthCheckClass);
  
  abstract protected <T extends HealthCheck>
  T addHealthCheck(T healthCheck);
  
  static {
    _healthServiceClass = loadHealthServiceClass();
  }
  
  @SuppressWarnings("unchecked")
  private static Class<? extends AbstractHealthService>
  loadHealthServiceClass()
  {

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      return (Class) Class.forName("com.caucho.env.health.HealthService",
                                   false,
                                   loader);
    } catch (Exception e) {
      log.log(Level.ALL, e.toString(), e);
    }
    
    return null;
    
  }
}
