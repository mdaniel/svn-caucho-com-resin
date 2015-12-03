/**
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
 * @author Fred Zappert (fred@caucho.com)
 */

package com.caucho.server.distcache;

import javax.cache.event.CacheEntryListener;

/**
 * Provides a base class that can extended to override the method or methods of
 * interest.
 * 
 * @note The listener is only when events occur on an entry that has been
 *       leased.
 */

public abstract class AbstractCacheListener<K> implements CacheEntryListener {
  protected AbstractCacheListener()
  {
  }

  /*
  @Override
  public void onLoad(Object key)
  {
  }

  @Override
  public void onEvict(Object key)
  {

  }

  @Override
  public void onClear()
  {
  }

  @Override
  public void onPut(Object key)
  {
  }

  @Override
  public void onRemove(Object key)
  {
  }
  */
}
