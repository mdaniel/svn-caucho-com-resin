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
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.BamServiceMXBean;
import com.caucho.util.L10N;


/**
 * Statistics for BAM service registered in the Resin network.
 */
public class BamServiceAdmin 
  extends AbstractManagedObject 
  implements BamServiceMXBean
{
  private final BamSystem _bamSystem;
  
  BamServiceAdmin(BamSystem bamSystem)
  {
    _bamSystem = bamSystem;
  }
  
  void register()
  {
    registerSelf();
  }

  @Override
  public String getName()
  {
    return null;
  }
  
  @Override
  public long getExternalMessageReadCount()
  {
    return _bamSystem.getExternalMessageReadCount();
  }

  @Override
  public long getExternalMessageWriteCount()
  {
    return _bamSystem.getExternalMessageWriteCount();
  }
}
