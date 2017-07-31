/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * This interface is defined in JSR 107.
 *
 * It may be used to access both local and cluster caches.
 *
 * Some bulk operations will act only upon the local cache, and will not affect a cluster cache, as noted in the
 * JavaDoc entry for each method.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.caucho.cache.event.CacheEntryEventFilter;
import com.caucho.cache.event.CacheEntryListener;

/**
 * The persistent or distributed cache is usable like a normal map, but loads
 * and stores data across the cluster.
 */
public interface Cache<K,V> extends Iterable<Cache.Entry<K,V>>, CacheLifecycle {
  public V get(Object key);

  public Map<K,V> getAll(Set<? extends K> keys);
  
  public boolean containsKey(K key);
  
  public void put(K key, V value);
  
  public V getAndPut(K key, V value);
  
  public void putAll(Map<? extends K, ? extends V> map);
  
  public boolean putIfAbsent(K key, V value);

  public boolean remove(K key);

  public boolean remove(K key, V oldValue);
  
  public V getAndRemove(K key);
  
  public boolean replace(K key, V oldValue, V newValue);
  
  public boolean replace(K key, V value);
  
  public V getAndReplace(K key, V value);
  
  public void removeAll(Set<? extends K> keys);
  
  public void removeAll();
  
  //
  // preload
  //
  
  public Future<V> load(K key);
  
  public Future<Map<K,? extends V>> loadAll(Set<? extends K> keys);
  
  //
  // update operations
  // 
  
  public Object invokeEntryProcessor(K key, EntryProcessor<K, V> entryProcessor);
  
  //
  // listeners
  //
  
  public boolean 
  registerCacheEntryListener(CacheEntryListener<? super K,? super V> listener,
                             boolean requireOldValue,
                             CacheEntryEventFilter<? super K, ? super V> filter,
                             boolean synchronous);
  
  public boolean unregisterCacheEntryListener(CacheEntryListener<?,?> listener);
  
  //
  // management
  //
  
  public String getName();
  
  public CacheManager getCacheManager();
  
  public Configuration<K,V> getConfiguration();
    
  public CacheStatistics getStatistics();

  Iterator<Cache.Entry<K,V>> iterator();
  
  public CacheMXBean getMBean();

  public <T> T unwrap(Class<T> cl);
  
  public interface Entry<K,V> {
    public K getKey();
    public V getValue();      
  }
  
  public interface MutableEntry<K,V> extends Entry<K,V> {
    public boolean exists();
    public void remove();
    public void setValue(V value);
  }
  
  public interface EntryProcessor<K,V> {
    public Object process(Cache.MutableEntry<K,V> entry);
  }
}
