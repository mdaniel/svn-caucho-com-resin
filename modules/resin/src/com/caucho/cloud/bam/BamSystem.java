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

package com.caucho.cloud.bam;

import java.util.concurrent.atomic.AtomicLong;

import com.caucho.bam.broker.ManagedBroker;
import com.caucho.bam.manager.BamManager;
import com.caucho.bam.manager.SimpleBamManager;
import com.caucho.config.ConfigException;
import com.caucho.env.service.AbstractResinSubSystem;
import com.caucho.env.service.ResinSystem;
import com.caucho.hemp.broker.DomainManager;
import com.caucho.hemp.broker.HempBroker;
import com.caucho.hemp.broker.HempBrokerManager;
import com.caucho.hemp.servlet.ServerAuthManager;
import com.caucho.util.L10N;


/**
 * The BAM service registered in the Resin network.
 */
public class BamSystem extends AbstractResinSubSystem
{
  private static final L10N L = new L10N(BamSystem.class);
  
  // priority must be before network so it's available to handle incoming
  // messages
  public static final int START_PRIORITY = START_PRIORITY_ENV_SYSTEM;
  
  private String _address;
  
  private final ResinSystem _resinSystem;
  private final HempBrokerManager _hempBrokerManager;
  private final HempBroker _broker;
  private final BamManager _brokerManager;
  
  private final AtomicLong _externalMessageReadCount = new AtomicLong();
  private final AtomicLong _externalMessageWriteCount = new AtomicLong();
  
  private ServerAuthManager _linkManager;
  
  public BamSystem(String address)
  {
    _resinSystem = ResinSystem.getCurrent();
    
    if (_resinSystem == null) {
      throw new ConfigException(L.l("{0} requires an active {1}",
                                    getClass().getSimpleName(),
                                    ResinSystem.class.getSimpleName()));
    }
    
    _address = address;
    
    _hempBrokerManager = new HempBrokerManager(_resinSystem);

    _broker = new HempBroker(_hempBrokerManager, getAddress());
    _brokerManager = new SimpleBamManager(_broker);

    if (getAddress() != null)
      _hempBrokerManager.addBroker(getAddress(), _broker);
    
    _hempBrokerManager.addBroker("resin.caucho", _broker);
  }
  
  public static BamSystem createAndAddService(String address)
  {
    ResinSystem system = preCreate(BamSystem.class);
      
    BamSystem service = new BamSystem(address);
    system.addService(service);
    
    return service;
  }
  
  public static BamSystem getCurrent()
  {
    return ResinSystem.getCurrentService(BamSystem.class);
  }
  
  public static ManagedBroker getCurrentBroker()
  {
    return getCurrent().getBroker();
  }
  
  public static BamManager getCurrentManager()
  {
    return getCurrent().getBamManager();
  }
  
  public String getAddress()
  {
    return _address;
  }
  
  public ManagedBroker getBroker()
  {
    return _broker;
  }
  
  public BamManager getBamManager()
  {
    return _brokerManager;
  }
  
  public void setDomainManager(DomainManager manager)
  {
    _broker.setDomainManager(manager);
  }
  
  public void setLinkManager(ServerAuthManager linkManager)
  {
    _linkManager = linkManager;
  }
  
  public ServerAuthManager getLinkManager()
  {
    return _linkManager;
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    new BamServiceAdmin(this).register();
  }
  
  @Override
  public void stop()
  {
    _broker.close();
  }

  public void addExternalMessageRead()
  {
    _externalMessageReadCount.incrementAndGet();
  }
  
  public long getExternalMessageReadCount()
  {
    return _externalMessageReadCount.get();
  }

  public void addExternalMessageWrite()
  {
    _externalMessageWriteCount.incrementAndGet();
  }
  
  public long getExternalMessageWriteCount()
  {
    return _externalMessageWriteCount.get();
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _address + "]";
  }
}
