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

package javax.cache;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import javax.cache.event.CacheEntryListener;

/**
 * The persistent or distributed cache is usable like a normal map, but loads
 * and stores data across the cluster.
 */
public interface Cache<K,V> extends Iterable<Cache.Entry<K,V>>, CacheLifecycle {
  /**
   * Returns the object specified by the given key.
   * <p/>
   * If the item does not exist and a CacheLoader has been specified,
   * the CacheLoader will be used to create a cache value.
   */
  public V get(Object key);

  public Map<K,V> getAll(Set<? extends K> keys);
  
  public boolean containsKey(K key);
  
  public Future<V> load(K key);
  
  public Future<Map<K,? extends V>> loadAll(Collection<? extends K> keys);
  
  public CacheStatistics getStatistics();
  
  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
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
  
  public CacheConfiguration<K,V> getConfiguration();
  
  public boolean registerCacheEntryListener(CacheEntryListener<? super K,? super V> listener,
                                            boolean synchronous);
  
  public boolean unregisterCacheEntryListener(CacheEntryListener<?,?> listener);
  
  public Object invokeEntryProcessor(K key, EntryProcessor<K, V> entryProcessor);
  
  public String getName();
  
  public <T> T unwrap(Class<T> cl);
  
  Iterator<Cache.Entry<K,V>> iterator();
  
  // CacheMXBean getMBean();

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
