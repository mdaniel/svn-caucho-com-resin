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

package com.caucho.env.service;

import com.caucho.lifecycle.Lifecycle;
import com.caucho.util.L10N;


/**
 * Interface for a service registered with the Resin Server.
 */
public class AbstractResinSubSystem implements ResinSubSystem
{
  private static final L10N L = new L10N(AbstractResinSubSystem.class);
  private final Lifecycle _lifecycle = new Lifecycle();

  @Override
  public int getStartPriority()
  {
    return START_PRIORITY_DEFAULT;
  }
  
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  @Override
  public void start()
    throws Exception
  {
    _lifecycle.toActive();
  }

  @Override
  public int getStopPriority()
  {
    return getStartPriority();
  }

  @Override
  public void stop() 
    throws Exception
  {
    _lifecycle.toStop();
  }

  @Override
  public void destroy()
  {
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  // convenience method for subclass's create methods
  protected static <E extends AbstractResinSubSystem> ResinSystem
    preCreate(Class<E> serviceClass)
  {
    ResinSystem system = ResinSystem.getCurrent();
    if (system == null)
      throw new IllegalStateException(L.l("{0} must be created before {1}",
                                          ResinSystem.class.getSimpleName(),
                                          serviceClass.getSimpleName()));
    
    if (system.getService(serviceClass) != null)
      throw new IllegalStateException(L.l("{0} was previously created",
                                          serviceClass.getSimpleName()));
    
    return system;
  }
}
