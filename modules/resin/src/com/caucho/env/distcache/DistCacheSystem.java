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

package com.caucho.env.distcache;

import com.caucho.distcache.CacheManager;
import com.caucho.env.service.*;
import com.caucho.server.distcache.AbstractCacheManager;

/**
 * The local cache repository.
 */
public class DistCacheSystem extends AbstractResinSubSystem 
{
  public static final int START_PRIORITY = START_PRIORITY_CACHE_SERVICE;

  private CacheManager _cacheManager;
  private AbstractCacheManager<?> _distCacheManager;
  
  public DistCacheSystem(AbstractCacheManager<?> distCacheManager)
  {
    if (distCacheManager == null)
      throw new NullPointerException();

    _cacheManager = new CacheManager();
    _distCacheManager = distCacheManager;
  }
  
  public static DistCacheSystem 
    createAndAddService(AbstractCacheManager<?> distCacheManager)
  {
    ResinSystem system = preCreate(DistCacheSystem.class);

    DistCacheSystem service = new DistCacheSystem(distCacheManager);
    system.addService(DistCacheSystem.class, service);

    return service;
  }

  public static DistCacheSystem getCurrent()
  {
    return ResinSystem.getCurrentService(DistCacheSystem.class);
  }
  
  public AbstractCacheManager<?> getDistCacheManager()
  {
    return _distCacheManager;
  }
  
  public CacheManager getCacheManager()
  {
    return _cacheManager;
  }
  
  public CacheBuilder createBuilder(String name)
  {
    return new CacheBuilder(name, _cacheManager, _distCacheManager);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  @Override
  public void start()
  {
    _distCacheManager.start();
  }
  
  @Override
  public void stop()
  {
    _distCacheManager.close();
    // _cacheManager.close();
  }
  
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _distCacheManager + "]";
  }
}
