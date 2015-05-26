/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.server.admin;

import io.baratine.core.ServiceRef;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.amp.AmpSystem;
import com.caucho.amp.ServiceManagerAmp;
import com.caucho.config.ResinService;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.deploy.DeployServiceImpl;

@ResinService
public class DeployService
{
  private static final Logger log 
    = Logger.getLogger(DeployService.class.getName());
  
  private static EnvironmentLocal<DeployServiceImpl> _localDeployActor
    = new EnvironmentLocal<DeployServiceImpl>();

  public DeployService()
  {
    if (_localDeployActor.get() == null) {
      DeployServiceImpl deployActor = createDeployActor(); 
      _localDeployActor.set(deployActor);
      
      ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
      
      ServiceRef service = rampManager.service(deployActor);
      service.bind("public://" + DeployServiceImpl.ADDRESS);
    }
  }
  
  /*
  public DeployActor getCurrentDeployActor()
  {
    return _localDeployActor.get();
  }
  */
  
  @PostConstruct
  public void init()
  {
    _localDeployActor.get().init();
  }
  
  private DeployServiceImpl createDeployActor()
  {
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class<?> cl = Class.forName("com.caucho.server.deploy.ProDeployActor",
                                  false,
                                  loader);
      
      return (DeployServiceImpl) cl.newInstance();
    } catch (Exception e) {
      log.log(Level.FINEST, e.toString(), e);
    }
    
    return new DeployServiceImpl();
  }
}
