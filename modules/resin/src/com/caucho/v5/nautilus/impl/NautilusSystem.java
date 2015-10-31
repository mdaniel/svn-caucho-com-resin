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

package com.caucho.v5.nautilus.impl;

import java.util.Objects;

import com.caucho.v5.amp.AmpSystem;
import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.env.system.SubSystemBase;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.kraken.KrakenSystem;

/**
 * The local cache repository.
 */
public class NautilusSystem extends SubSystemBase 
{
  public static final int START_PRIORITY = START_PRIORITY_CLUSTER_SERVICE;

  // private DataSource _jdbcDataSource;
  
  private BrokerService _brokerService;
  private BrokerServiceImpl _brokerServiceImpl;

  private NautilusServiceImpl _nautilusServiceImpl;

  private NautilusSystem()
  {
  }
  
  public static NautilusSystem createAndAddSystem()
  {
    SystemManager system = preCreate(NautilusSystem.class);

    NautilusSystem nautilusSystem = new NautilusSystem();
    
    system.addSystem(NautilusSystem.class, nautilusSystem);
    
    // KrakenSystemCluster.createAndAddSystem(krakenSystem);

    return nautilusSystem;
  }

  public static NautilusSystem getCurrent()
  {
    return SystemManager.getCurrentSystem(NautilusSystem.class);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    ServiceManagerAmp rampManager = AmpSystem.getCurrentManager();
    
    KrakenSystem kraken = KrakenSystem.getCurrent();
    
    Objects.requireNonNull(kraken);
    
    _brokerServiceImpl = new BrokerServiceImpl(kraken);
    _brokerService = rampManager.service(_brokerServiceImpl)
                                .as(BrokerService.class);
    
    _nautilusServiceImpl = new NautilusServiceImpl(this);
    
    rampManager.service(_nautilusServiceImpl)
               .bind("public://" + NautilusService.PATH);
  }
  
  @Override
  public void stop(ShutdownModeAmp mode)
  {
  }

  public BrokerService getBrokerService()
  {
    return _brokerService;
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
