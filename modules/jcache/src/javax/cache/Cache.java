/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import java.util.Map;
import java.util.Set;

/**
 * The persistent or distributed cache is usable like a normal map, but loads
 * and stores data across the cluster.
 */
public interface Cache<K,V> extends Map<K,V> {
    /**
     * Returns the object specified by the given key.
     * <p/>
     * If the item does not exist and a CacheLoader has been specified,
     * the CacheLoader will be used to create a cache value.
     */
    public V get(Object key);

    /**
     * Puts a new item in the cache.
     *
     * @param key   the key of the item to put
     * @param value the value of the item to put
     */
    public V put(K key, V value);

    /**
     * Removes the entry from the cache
     */
    public V remove(Object key);
    
    //
    // extra operations
    
    Map<K,V> getAll(Collection<Object> keys) throws CacheException;
    
    void load(Object key) throws CacheException;
    
    void loadAll(Collection keys) throws CacheException;
    
    V peek(Object key);
    
    CacheEntry<K,V> getCacheEntry(Object key);
    
    CacheStatistics getCacheStatistics();
    
    void evict();
    
    void addListener(CacheListener listener);
    
    void removeListener(CacheListener listener);
}
