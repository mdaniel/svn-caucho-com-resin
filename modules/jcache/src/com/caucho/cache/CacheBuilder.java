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

package com.caucho.cache;

import com.caucho.cache.event.CacheEntryListener;
import com.caucho.cache.transaction.IsolationLevel;
import com.caucho.cache.transaction.Mode;

/**
 * Provides the capability of dynamically creating a cache.
 *
 * See  the  default implementation of this interface in {@link com.caucho.cluster.CacheTemplate}
 * for additional methods.
 */
public interface CacheBuilder<K,V> {
  public Cache<K,V> build();
  
  public CacheBuilder<K,V> setCacheLoader(CacheLoader<K,? extends V> cacheLoader);
  
  public CacheBuilder<K,V> setCacheWriter(CacheWriter<? super K,? super V> cacheWriter);
  
  public CacheBuilder<K,V> 
  registerCacheEntryListener(CacheEntryListener<K,V> listener);
  
  public CacheBuilder<K,V> setStoreByValue(boolean storeByValue);
  
  public CacheBuilder<K,V> setTransactionEnabled(IsolationLevel isolationLevel,
                                                 Mode mode);
  
  public CacheBuilder<K,V> setStatisticsEnabled(boolean isEnable);
  
  public CacheBuilder<K,V> setReadThrough(boolean readThrough);
  
  public CacheBuilder<K,V> setWriteThrough(boolean writeThrough);
  
  public CacheBuilder<K,V> setExpiry(Configuration.ExpiryType type,
                                     Configuration.Duration timeToLive);
}