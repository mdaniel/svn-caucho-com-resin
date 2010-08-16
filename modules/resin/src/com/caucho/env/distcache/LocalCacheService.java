/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
import com.caucho.env.service.AbstractResinService;
import com.caucho.env.service.ResinSystem;
import com.caucho.server.distcache.AbstractCacheManager;
import com.caucho.server.distcache.FileCacheEntry;
import com.caucho.server.distcache.FileCacheManager;

/**
 * The local cache repository.
 */
public class LocalCacheService extends AbstractResinService {
  public static final int START_PRIORITY = START_PRIORITY_ENV_SERVICE;
  
  private CacheManager _cacheManager;
  private FileCacheManager _fileCacheManager;
  
  public LocalCacheService(ResinSystem resinSystem)
  {
    _fileCacheManager = new FileCacheManager(resinSystem);
  }
  
  public static LocalCacheService getCurrent()
  {
    return ResinSystem.getCurrentService(LocalCacheService.class);
  }
  
  @Override
  public int getStartPriority()
  {
    return START_PRIORITY;
  }
  
  public AbstractCacheManager<FileCacheEntry> getBackingManager()
  {
    return _fileCacheManager;
  }
  
  public CacheBuilder createBuilder(String name)
  {
    return new CacheBuilder(name, _cacheManager, _fileCacheManager);
  }
  
  @Override
  public void start()
  {
    _fileCacheManager.start();
    
    _cacheManager = new CacheManager();
  }
  
  @Override
  public void stop()
  {
    _fileCacheManager.close();
  }
}
