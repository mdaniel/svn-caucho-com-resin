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

package com.caucho.v5.http.pod;

import java.util.ArrayList;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.deploy2.DeployHandle2;
import com.caucho.v5.deploy2.DeployInstance2;
import com.caucho.v5.vfs.PathImpl;

import io.baratine.service.Cancel;
import io.baratine.service.Result;


/**
 * Deploy pod-node controller.
 * 
 * Manages the activation of a pod node.
 */
class DeployNodePod<I extends DeployInstance2> implements DeployNode
{
  private static final Logger log
    = Logger.getLogger(DeployNodePod.class.getName());
  
  private final String _id;
  private final PodDeploy _pod;
  private final DeployHandle2<I> _handle;
  
  private final PathImpl _rootDir;
  
  //private PodControllerBase<?> _controller;
  
  private ArrayList<Cancel> _watchList = new ArrayList<>();
  
  private PodConfigApp _podConfig;
  
  DeployNodePod(String id,
                PodDeploy pod,
                DeployHandle2<I> handle)
  {
    _id = id;
    _pod = pod;
    _handle = handle;

    int p = id.indexOf('/');
    String name = id.substring(p + 1);
    
    _rootDir = pod.getContainer().getPodExpandPath().lookup(name);
  }
  
  public String getId()
  {
    return _id;
  }
  
  protected PodDeploy getPodDeploy()
  {
    return _pod;
  }
  
  protected boolean isPodNodeActive()
  {
    return true;
  }
  
  protected PodBartender getPod()
  {
    return _pod.getPod();
  }
  
  protected PodContainer getPodContainer()
  {
    return _pod.getContainer();
  }
  
  public void setConfig(PodConfigApp podConfig)
  {
    if (_podConfig == podConfig
        || _podConfig != null && _podConfig.equals(podConfig)) {
      return;
    }
    
    _podConfig = podConfig;
  }
  
  @Override
  public void update()
  {
    if (! isPodNodeActive()) {
      stop();
      return;
    }
    
    PodConfigApp podConfig = getPodDeploy().getConfig();
    
    if (podConfig == null) {
      _podConfig = null;
      //_handle.getService().setController(null);
      stop();
      return;
    }
    
    if (podConfig.equals(_podConfig)) {
      start();
      return;
    }

    create(podConfig);

    start();
  }
  
  private void create(PodConfigApp podConfig)
  {
    /*
    PodControllerBase<I> controller = createController(_id, _rootDir);
    
    controller.setStartupMode(DeployMode.MANUAL);
    controller.setRedeployMode(DeployMode.AUTOMATIC);
    
    for (PodConfig config : getPodContainer().getPodDefaultList()) {
      controller.addConfigDefault(new PodConfigApp(config));
    }

    controller.addConfigDefault(podConfig);
    
    _podConfig = podConfig;
    
    _handle.getService().setController(controller);
    */
  }
  
  /*
  protected PodControllerBase<I> createController(String id, PathImpl root)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  */

  public void start()
  {
    
    if (_podConfig != null && _podConfig.isApplication()) {
      _handle.start();
    }
    else {
      _handle.shutdown(ShutdownModeAmp.GRACEFUL, Result.ignore());
    }
  }

  public void stop()
  {
    _handle.shutdown(ShutdownModeAmp.GRACEFUL, Result.ignore());
  }

  public void shutdown(ShutdownModeAmp mode)
  {
    // _handle.stopAndWait(mode);
    
    closeHandles();
  }

  public void close()
  {
    //PodControllerBase<?> controller = _controller;
    
    DeployHandle2<?> handle = _handle;
    
    if (handle != null) {
      handle.shutdown(ShutdownModeAmp.GRACEFUL, Result.ignore());
    }

    closeHandles();
  }
  
  private void closeHandles()
  {
    for (Cancel watchHandle : _watchList) {
      watchHandle.cancel();
    }
  }
  
  DeployHandle2<?> getHandle()
  {
    return _handle;
  }

  @Override
  public PodAppHandle getPodAppHandle()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public int hashCode()
  {
    return _id.hashCode();
  }
  
  @Override
  public boolean equals(Object o)
  {
    if (! (o instanceof DeployNodePod)) {
      return false;
    }
    
    DeployNodePod<?> node = (DeployNodePod) o;
    
    return _id.equals(node._id);
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
