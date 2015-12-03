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
 * @author Alex Rojkov
 */

package com.caucho.server.admin;

import java.util.concurrent.atomic.AtomicBoolean;

import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.mailbox.MultiworkerMailbox;
import com.caucho.bam.proxy.ProxyActor;
import com.caucho.cloud.bam.BamSystem;
import com.caucho.config.Service;
import com.caucho.loader.EnvironmentLocal;

import javax.annotation.PostConstruct;

@Service
public class ManagerService
{
  private static EnvironmentLocal<ManagerService> _localManagerService
    = new EnvironmentLocal<ManagerService>();
  
  private AtomicBoolean _isInit = new AtomicBoolean();
  
  private ManagerActor _managerActor;
  private ManagerProxyActor _managerProxyBean;

  public ManagerService()
  {
    if (_localManagerService.get() == null) {
      _managerActor = new ManagerActor();
      _managerProxyBean = new ManagerProxyActor();
      
      _localManagerService.set(this);
    }
  }

  public void setHprofDir(String dir)
  {
    getCurrentManagerActor().setHprofDir(dir);
  }
  
  public ManagerActor getCurrentManagerActor()
  {
    return _localManagerService.get().getManagerActor();
  }
  
  private ManagerActor getManagerActor()
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
    
    ManagedBroker broker = BamSystem.getCurrentBroker();
    
    ManagerActor managerActor = _managerActor;
    
    managerActor.init();
    
    ProxyActor actor = new ProxyActor(_managerProxyBean, 
                                      "manager-proxy@resin.caucho",
                                      broker);
    String address = actor.getAddress();
    MultiworkerMailbox mailbox
      = new MultiworkerMailbox(actor.getAddress(),
                               actor, broker, 2);

    broker.addMailbox(address, mailbox);
  }
}
