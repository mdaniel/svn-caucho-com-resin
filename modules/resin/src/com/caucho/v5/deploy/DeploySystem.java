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

package com.caucho.v5.deploy;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.deploy2.DeployHandle2;
import com.caucho.v5.env.system.SubSystemBase;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.util.L10N;


/**
 * Baratine master system registered in the Resin network.
 */
public class DeploySystem extends SubSystemBase
{
  private static final Logger log = Logger.getLogger(DeploySystem.class.getName());
  private static final L10N L = new L10N(DeploySystem.class);
  
  // priority must be before network so it's available to handle incoming
  // messages
  public static final int START_PRIORITY = START_PRIORITY_ENV_SYSTEM;
  
  private final SystemManager _systemManager;
  
  private final ConcurrentHashMap<String,DeployHandle<?>> _handleMap
    = new ConcurrentHashMap<>();
  
  private final ConcurrentHashMap<String,DeployHandle2<?>> _handleMap2
    = new ConcurrentHashMap<>();

  public DeploySystem(SystemManager systemManager)
  {
    _systemManager = systemManager;
  }
  
  public static DeploySystem createAndAddSystem()
  {
    SystemManager system = preCreate(DeploySystem.class);
      
    DeploySystem subSystem = new DeploySystem(system);
    system.addSystem(subSystem);
    
    return subSystem;
  }
  
  public static DeploySystem getCurrent()
  {
    return SystemManager.getCurrentSystem(DeploySystem.class);
  }
  
  public <I extends DeployInstance> DeployHandle<I> 
    createHandle(String id, Logger log)
  {
    DeployHandle<I> handle = (DeployHandle) _handleMap.get(id);
    
    if (handle == null) {
      ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
      Objects.requireNonNull(ampManager);
      
      DeployControllerServiceImpl<I> serviceImpl
        = new DeployControllerServiceImpl<>(id, log);
      
      DeployControllerService<I> service
        = ampManager.newService(serviceImpl).as(DeployControllerService.class);
      
      handle = new DeployHandleBase<I>(id, service);
      
      _handleMap.putIfAbsent(id, handle);
      
      handle = (DeployHandle) _handleMap.get(id);
    }
    
    return handle;
  }
  
  public <I extends DeployInstance> DeployHandle<I> 
    createHandle2(String id, Logger log)
  {
    DeployHandle<I> handle = (DeployHandle) _handleMap.get(id);
    
    if (handle == null) {
      ServiceManagerAmp ampManager = AmpSystem.getCurrentManager();
      Objects.requireNonNull(ampManager);
      
      DeployControllerServiceImpl<I> serviceImpl
        = new DeployControllerServiceImpl<>(id, log);
      
      DeployControllerService<I> service
        = ampManager.newService(serviceImpl).as(DeployControllerService.class);
      
      handle = new DeployHandleBase<I>(id, service);
      
      _handleMap.putIfAbsent(id, handle);
      
      handle = (DeployHandle) _handleMap.get(id);
    }
    
    return handle;
  }
  
  public <I extends DeployInstance> DeployHandle<I> 
    getHandle(String id)
  {
    return (DeployHandle) _handleMap.get(id);
  }

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}

