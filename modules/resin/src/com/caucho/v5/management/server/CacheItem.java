/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.management.server;

import java.beans.ConstructorProperties;

/**
 * Cache entry
 */
@SuppressWarnings("serial")
public class CacheItem implements java.io.Serializable
{
  private String _url;

  private boolean _isCacheable;
  private boolean _isCached;
  private long _hitCount;
  private long _missCount;

  public CacheItem()
  {
  }

  @ConstructorProperties({"url", "cacheable", "cached","hitCount", "missCount"})
  public CacheItem(String url,
                   boolean isCacheable,
                   boolean isCached,
                   long hitCount,
                   long missCount)
  {
    _url = url;
    _isCacheable = isCacheable;
    _isCached = isCached;
    _hitCount = hitCount;
    _missCount = missCount;
  }

  public String getUrl()
  {
    return _url;
  }

  public void setUrl(String url)
  {
    _url = url;
  }
  
  public boolean isCacheable()
  {
    return _isCacheable;
  }
  
  public void setCacheable(boolean isCacheable)
  {
    _isCacheable = isCacheable;
  }
  
  public boolean isCached()
  {
    return _isCached;
  }
  
  public void setCached(boolean isCached)
  {
    _isCached = isCached;
  }

  public long getHitCount()
  {
    return _hitCount;
  }

  public void setHitCount(long hitCount)
  {
    _hitCount = hitCount;
  }

  public long getMissCount()
  {
    return _missCount;
  }

  public void setMissCount(long missCount)
  {
    _missCount = missCount;
  }

  public String toString()
  {
    return "CacheItem[" + _url + ",hit:" + _hitCount + ",miss:" + _missCount + "]";
  }
}
