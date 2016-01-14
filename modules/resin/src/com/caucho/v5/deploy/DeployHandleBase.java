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

import io.baratine.service.Result;

import java.util.Objects;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;

/**
 * DeployHandle returns the currently deployed instance
 */
public class DeployHandleBase<I extends DeployInstance> 
  implements DeployHandle<I>
{
  private static final Logger log = Logger.getLogger(DeployHandleBase.class.getName());
  
  // private DeployController<I> _controller;
  
  private String _id;
  private DeployControllerService<I> _service;
  
  private I _instance;
  
  public DeployHandleBase(String id, DeployControllerService<I> service)
  {
    Objects.requireNonNull(id);
    Objects.requireNonNull(service);
    
    _id = id;
    _service = service;
  }
  
  /*
  protected DeployController<I> getController()
  {
    return _controller;
  }
  */
  
  @Override
  public DeployControllerService<I> getService()
  {
    //return getController().getService();
    return _service;
  }
  
  /**
   * Returns the controller's id, typically a tag value like
   * production/webapp/default/ROOT
   */
  @Override
  public String getId()
  {
    //return getController().getId();
    return _id;
  }
  
  /**
   * Returns the state name.
   */
  @Override
  public LifecycleState getState()
  {
    return getService().getState();
  }

  /**
   * Returns the current instance.
   */
  @Override
  public I getDeployInstance()
  {
    I instance = _instance;
    
    if (instance == null || instance.isModified()) {
      _instance = instance = getService().getDeployInstance();
    }
    
    return instance;
  }

  /**
   * Returns the current instance, waiting for active.
   */
  @Override
  public I getActiveDeployInstance()
  {
    I instance = _instance;
    
    if (instance == null || instance.isModified()) {
      instance = getService().getActiveDeployInstance();
    }
    
    return instance;
  }

  /**
   * Check for modification updates, generally from an admin command when
   * using "manual" redeployment.
   */
  @Override
  public void update()
  {
    getService().update();
  }

  /**
   * Check for modification updates, generally from an admin command when
   * using "manual" redeployment.
   */
  @Override
  public void alarm()
  {
    getService().alarm(Result.ignore());
  }

  /**
   * Returns the instance for a top-level request
   * @return the request object or null for none.
   */
  @Override
  public I request()
  {
    I instance = _instance;
    
    if (instance == null || instance.isModified()) {
      _instance = instance = getService().requestSafe();
    }
    
    return instance;
  }

  /**
   * Returns the instance for a subrequest.
   *
   * @return the request object or null for none.
   */
  @Override
  public I subrequest()
  {
    I instance = _instance;
    
    if (instance == null || instance.isModified()) {
      _instance = instance = getService().subrequest();
    }
    
    return instance;
  }
  
  /**
   * External lifecycle listeners, so applications can detect deployment
   * and redeployment.
   */
  @Override
  public void addLifecycleListener(LifecycleListener listener)
  {
    getService().addLifecycleListener(listener);
  }
  
  @Override
  public void startOnInit()
  {
    getService().startOnInit(Result.ignore());
  }
  
  @Override
  public void start()
  {
    getService().start(Result.ignore());
  }
  
  @Override
  public void stop(ShutdownModeAmp mode)
  {
    getService().stop(mode, Result.ignore());
  }
  
  @Override
  public void stopAndWait(ShutdownModeAmp mode)
  {
    getService().stop(mode);
  }
  
  @Override
  public void destroy()
  {
    getService().destroy(Result.ignore());
  }
  
  @Override
  public Throwable getConfigException()
  {
    return getService().getConfigException();
  }
  
  /**
   * Returns the state name.
   */
  @Override
  public boolean isModified()
  {
    return getService().isModified();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
