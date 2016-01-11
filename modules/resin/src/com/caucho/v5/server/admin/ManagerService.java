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
 * @author Alex Rojkov
 */

package com.caucho.v5.server.admin;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.config.ResinService;
import com.caucho.v5.loader.EnvironmentLocal;

@ResinService
public class ManagerService
{
  private static EnvironmentLocal<ManagerService> _localManagerService
    = new EnvironmentLocal<>();
  
  private AtomicBoolean _isInit = new AtomicBoolean();
  
  private ManagerServiceImpl _managerActor;
  private ManagerProxyActor _managerProxyBean;

  public ManagerService()
  {
    if (_localManagerService.get() == null) {
      _managerActor = new ManagerServiceImpl();
      _managerProxyBean = new ManagerProxyActor();
      
      _localManagerService.set(this);
    }
  }

  public void setHprofDir(String dir)
  {
    getCurrentManagerActor().setHprofDir(dir);
  }
  
  public ManagerServiceImpl getCurrentManagerActor()
  {
    return _localManagerService.get().getManagerActor();
  }
  
  private ManagerServiceImpl getManagerActor()
  {
    return _managerActor;
  }
  
  @PostConstruct
  public void init()
  {
    _localManagerService.get().initActors();
  }
  
  private void initActors()
  {
    if (_isInit.getAndSet(true)) {
      return;
    }
    

    ManagerServiceImpl managerActor = _managerActor;
    
    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    rampManager.newService(managerActor).address("/manager").ref();
  }
}
