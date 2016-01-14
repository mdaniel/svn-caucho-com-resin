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
import java.util.List;
import java.util.Objects;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.bartender.pod.PodBartenderProxy;
import com.caucho.v5.deploy2.DeployHandle2;


/**
 * Deploy pod-node controller.
 * 
 * Manages the activation of a pod node.
 */
class PodDeploy
{
  private final String _id;
  private final PodBartender _pod;
  private final DeployNodeLoader _podLoader;
  private PodContainer _podContainer;
  private PodConfigApp _config;
  
  private List<DeployNode> _nodeList = new ArrayList<>();
  
  PodDeploy(String id,
            PodBartender pod,
            PodContainer container,
            DeployHandle2<PodLoader> loaderHandle)
  {
    Objects.requireNonNull(pod);
    Objects.requireNonNull(container);
    Objects.requireNonNull(loaderHandle);
    
    _id = id;
    _pod = pod;
    
    _podContainer = container;
    _podLoader = new DeployNodeLoader(id, this, loaderHandle);
    
    if (! (pod instanceof PodBartenderProxy)) {
      Thread.dumpStack();
    }
  }
  
  public String getId()
  {
    return _id;
  }

  public PodBartender getPod()
  {
    return _pod;
  }
  
  public PodContainer getContainer()
  {
    return _podContainer;
  }

  public void updateConfig(PodConfigApp config)
  {
    _config = config;
    
    update();
  }

  public PodConfigApp getConfig()
  {
    return _config;
  }
  
  public void update()
  {
    _podLoader.update();
    
    for (DeployNode node : _nodeList) {
      node.update();
    }
  }

  public void update(List<DeployNode> nodeAppList)
  {
    List<DeployNode> oldNodeList = _nodeList;
    
    _nodeList = nodeAppList;
    
    for (DeployNode node : oldNodeList) {
      node.update();
    }
    
    for (DeployNode node : _nodeList) {
      node.update();
    }
  }

  public void stop(ShutdownModeAmp mode)
  {
    _podLoader.stop();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
