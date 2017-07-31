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

package com.caucho.server.distcache;

import java.util.Date;

import com.caucho.cache.CacheStatistics;
import com.caucho.util.HashKey;
import com.caucho.util.Hex;

/**
 * Implements the distributed cache
 */

public class CacheStatisticsImpl implements CacheStatistics
{
  private final CacheImpl<?,?> _cache;
  
  public CacheStatisticsImpl(CacheImpl<?,?> cache)
  {
    _cache = cache;
  }

  @Override
  public void clearStatistics()
  {

  }

  @Override
  public Date getStartAccumulationDate()
  {
    return null;
  }

  @Override
  public long getCacheHits()
  {
    return 0;
  }

  @Override
  public float getCacheHitPercentage()
  {
    return 0;
  }

  @Override
  public long getCacheMisses()
  {
    return 0;
  }

  @Override
  public float getCacheMissPercentage()
  {
    return 0;
  }

  @Override
  public long getCacheGets()
  {
    return 0;
  }

  @Override
  public long getCachePuts()
  {
    return 0;
  }

  @Override
  public long getCacheRemovals()
  {
    return 0;
  }

  @Override
  public long getCacheEvictions()
  {
    return 0;
  }

  @Override
  public float getAverageGetMillis()
  {
    return 0;
  }

  @Override
  public float getAveragePutMillis()
  {
    return 0;
  }

  @Override
  public float getAverageRemoveMillis()
  {
    return 0;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _cache + "]";
  }
}
