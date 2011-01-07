/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

package com.caucho.env.service;

import com.caucho.config.inject.InjectManager;

public class CdiService extends AbstractResinService 
{
  public static final int START_PRIORITY = 1;
  
  private InjectManager _cdiManager;
  
  private CdiService()
  {
    ResinSystem system = ResinSystem.getCurrent();
    _cdiManager = InjectManager.create(system.getClassLoader());
  }

  public static CdiService createAndAddService()
  {
    ResinSystem system = preCreate(CdiService.class);
      
    CdiService service = new CdiService();
    system.addService(CdiService.class, service);
    
    return service;
  }

  public static CdiService getCurrent()
  {
    return ResinSystem.getCurrentService(CdiService.class);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  public void start()
  {
    _cdiManager.start();
  }
}
