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

package com.caucho.server.distcache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheLoader;
import javax.cache.CacheManager;
import javax.cache.CacheStatistics;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Period;
import com.caucho.distcache.ByteStreamCache;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.ExtCacheEntry;
import com.caucho.distcache.ObjectCache;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.loader.Environment;
import com.caucho.server.distcache.CacheStoreManager.DataItem;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * Implements the distributed cache
 */

public class CacheImpl
  implements ObjectCache, ByteStreamCache, Closeable
{
  private static final L10N L = new L10N(CacheImpl.class);

  private CacheManagerImpl _localManager;
  private CacheStoreManager _manager;

  private final String _name;
  private final String _guid;

  private final CacheConfig _config;

  private Collection<CacheEntryListener> _listeners
    = new ConcurrentLinkedQueue<CacheEntryListener>();

  private LruCache<Object,DistCacheEntry> _entryCache;

  private boolean _isInit;
  private boolean _isClosed;

  private long _priorMisses = 0;
  private long _priorHits = 0;

  public CacheImpl(CacheManagerImpl localManager,
                   String name,
                   String guid,
                   CacheConfig config)
  {
    _localManager = localManager;
    _name = name;
    _guid = guid;
    _config = config;
    
    init(true);
  }

  /**
   * Returns the name of the cache.
   */
  public String getName()
  {
    return _name;
  }
  
  public String getGuid()
  {
    return _guid;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and z null value
   * will be returned for a get.
   * <p/>
   * Default is infinite.
   */
  public long getExpireTimeout()
  {
    return _config.getModifiedExpireTimeout();
  }

  /**
   * The maximum idle time for an item, which is typically used for
   * temporary data like sessions.  For example, session
   * data might be removed if idle over 30 minutes.
   * <p/>
   * Cached data would have infinite idle time because
   * it doesn't depend on how often it's accessed.
   * <p/>
   * Default is infinite.
   */
  public long getIdleTimeout()
  {
    return _config.getAccessedExpireTimeout();
  }

  /**
   * Returns the idle check window, used to minimize traffic when
   * updating access times.
   */
  public long getIdleTimeoutWindow()
  {
    return _config.getAccessedExpireTimeoutWindow();
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseTimeout()
  {
    return _config.getLeaseExpireTimeout();
  }

  /**
   * The local read timeout is how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quickly changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  public long getLocalReadTimeout()
  {
    return _config.getLocalExpireTimeout();
  }
  
  public CacheImpl createIfAbsent()
  {
    init(true);
    
    return _localManager.getCache(_guid);
  }

  private void init(boolean ifAbsent)
  {
    synchronized (this) {
      if (_isInit)
        return;

      _isInit = true;

      _config.init();

      initServer();
      
      if (_config.getEngine() == null)
        _config.setEngine(_manager.getCacheEngine());

      _config.setCacheKey(_manager.createSelfHashKey(_config.getGuid(),
                                                     _config.getKeySerializer()));

      _entryCache = new LruCache<Object,DistCacheEntry>(512);
      
      Environment.addCloseListener(this);
    }
    
    _manager.initCache(this);
  }

  /**
   * Returns the object with the given key without checking the backing store.
   */
  public Object peek(Object key)
  {
    DistCacheEntry cacheEntry = _entryCache.get(key);

    return (cacheEntry != null) ? cacheEntry.peek() : null;
  }
  
  /**
   * Returns the hash of the given key
   */
  public HashKey getKeyHash(Object key)
  {
    return getDistCacheEntry(key).getKeyHash();
  }

  /**
   * Returns the object with the given key, checking the backing
   * store if necessary.
   */
  @Override
  public Object get(Object key)
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    Object value = entry.get(_config);
    
    return value;
  }

  /**
   * Returns the object with the given key always checking the backing
   * store .
   */
  public Object getExact(Object key)
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    Object value = entry.getExact(_config);
    
    return value;
  }

  /**
   * Returns the object with the given key, updating the backing
   * store if necessary.
   */
  /*
  @Override
  public Object getLazy(Object key)
  {
    return getDistCacheEntry(key).getLazy(_config);
  }
  */

  /**
   * Fills an output stream with the value for a key.
   */
  @Override
  public boolean get(Object key, OutputStream os)
    throws IOException
  {
    return getDistCacheEntry(key).getStream(os, _config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  @Override
  public ExtCacheEntry getExtCacheEntry(Object key)
  {
    return getDistCacheEntry(key).getMnodeValue(_config);
  }
  
  public ExtCacheEntry getExtCacheEntry(HashKey key)
  {
    return getDistCacheEntry(key).getMnodeValue(_config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  @Override
  public ExtCacheEntry peekExtCacheEntry(Object key)
  {
    return getDistCacheEntry(key).getMnodeEntry();
  }
  
  public ExtCacheEntry getStatCacheEntry(Object key)
  {
    return getDistCacheEntry(key);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public Cache.Entry getCacheEntry(Object key)
  {
    return getExtCacheEntry(key);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  @Override
  public void put(Object key, Object value)
  {
    getDistCacheEntry(key).put(value, _config);

    notifyPut(key);
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   * @param flags       the flags value (for memcache)
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout,
                           int flags)
    throws IOException
  {
    return getDistCacheEntry(key).put(is, _config, 
                                      accessedExpireTimeout,
                                      modifiedExpireTimeout,
                                      flags);
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long accessedExpireTimeout,
                           long modifiedExpireTimeout)
    throws IOException
  {
    return getDistCacheEntry(key).put(is, _config, 
                                      accessedExpireTimeout,
                                      modifiedExpireTimeout);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  @Override
  public Object getAndPut(Object key, Object value)
  {
    return getDistCacheEntry(key).getAndPut(value, _config);
    
    // notifyPut(key);
  }

  /**
   * Updates the cache if the old version matches the current version.
   * A zero value for the old value hash only adds the entry if it's new.
   *
   * @param key     the key to compare
   * @param version the version of the old value, returned by getEntry
   * @param value   the new value
   * @return true if the update succeeds, false if it fails
   */
  @Override
  public boolean compareAndPut(Object key,
                               long version,
                               Object value)
  {
    put(key, value);

    return true;
  }

  /**
   * Updates the cache if the old version matches the current value.
   * A zero value for the old version only adds the entry if it's new.
   *
   * @param key         the key to compare
   * @param version     the hash of the old version, returned by getEntry
   * @param inputStream the new value
   * @return true if the update succeeds, false if it fails
   */
  @Override
  public boolean compareAndPut(Object key,
                               long version,
                               InputStream inputStream)
    throws IOException
  {
    put(key, inputStream);

    return true;
  }

  public void compareAndPut(HashKey key, 
                            HashKey value,
                            long valueLength,
                            long version)
  {
    getDistCacheEntry(key).compareAndPut(version, value, valueLength, _config);
    
    notifyPut(key);
  }

  @Override
  public boolean putIfAbsent(Object key, Object value) throws CacheException
  {
    HashKey NULL = MnodeEntry.NULL_KEY;
    
    HashKey result
      = getDistCacheEntry(key).compareAndPut(NULL, value, _config);
    
    return result != null && result.isNull();
  }

  @Override
  public boolean replace(Object key, Object oldValue, Object value)
    throws CacheException
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    HashKey oldHash = entry.getValueHash(oldValue, _config);
    
    HashKey result = entry.compareAndPut(oldHash, value, _config);
    
    if (oldHash == null || oldHash.isNull()) {
      return result == null || result.isNull();
    }
    else
      return oldHash.equals(result);
  }

  @Override
  public boolean replace(Object key, Object value) throws CacheException
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    HashKey oldHash = MnodeEntry.ANY_KEY;
    
    HashKey result = entry.compareAndPut(oldHash, value, _config);
    
    return result != null && ! result.isNull();
  }

  @Override
  public Object getAndReplace(Object key, Object value) throws CacheException
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    HashKey oldHash = MnodeEntry.ANY_KEY;
    
    HashKey result = entry.compareAndPut(oldHash, value, _config);
    
    // return result != null && ! result.isNull();
    
    return null;
  }

  /**
   * Removes the entry from the cache.
   *
   * @return true if the object existed
   */
  @Override
  public boolean remove(Object key)
  {
    notifyRemove(key);
    
    getDistCacheEntry(key).remove(_config);
    
    return true;
  }

  /**
   * Removes the entry from the cache.
   *
   * @return true if the object existed
   */
  @Override
  public boolean remove(Object key, Object oldValue)
  {
    notifyRemove(key);
    
    getDistCacheEntry(key).remove(_config);
    
    return true;
  }

  @Override
  public Object getAndRemove(Object key) throws CacheException
  {
    notifyRemove(key);
    
    return getDistCacheEntry(key).remove(_config);
  }

  /**
   * Removes the entry from the cache if the current entry matches the version.
   */
  @Override
  public boolean compareAndRemove(Object key, long version)
  {
    DistCacheEntry cacheEntry = getDistCacheEntry(key);

    if (cacheEntry.getVersion() == version) {
      remove(key);

      return true;
    }

    return false;
  }
  
  /**
   * Returns the entry for the given key, returning the live item.
   */
  public ExtCacheEntry getLiveCacheEntry(Object key)
  {
    return getDistCacheEntry(key);
  }

  /**
   * Returns the CacheKeyEntry for the given key.
   */
  protected DistCacheEntry getDistCacheEntry(Object key)
  {
    DistCacheEntry cacheEntry = null;
    
    // cacheEntry = _entryCache.get(key);

    if (cacheEntry == null) {
      cacheEntry = _manager.getCacheEntry(key, _config);

      // _entryCache.put(key, cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns the CacheKeyEntry for the given key.
   */
  protected DistCacheEntry getDistCacheEntry(HashKey key)
  {
    return _manager.getCacheEntry(key, _config);
  }

  /**
   * Returns a new map of the items found in the central cache.
   *
   * @note If a cacheLoader is configured if an item is not found in the cache.
   */
  public Map getAll(Collection keys)
  {
    Map result = new HashMap();

    for (Object key : keys) {
      Object value = get(key);

      if (value != null) {
        result.put(key, value);
      }
    }

    return result;
  }

  /**
   * Loads an item into the cache if not already there and was returned from  in the optional cache loader.
   *
   * @param key
   */
  /*
  @Override
  public void load(Object key)
  {
    if (containsKey(key) || get(key) != null)
      return;

    Object loaderValue = cacheLoader(key);

    if (loaderValue != null)
      put(key, loaderValue);

    notifyLoad(key);
  }
  */

  /**
   * Implements the loadAll method for a collection of keys.
   */
  /*
  public void loadAll(Collection keys)
  {
    Map<Object, Object> entries = null;
    CacheLoader loader = _config.getCacheLoader();

    if (loader == null || keys == null || keys.size() == 0)
      return;

    entries = loader.loadAll(keys);

    if (entries.isEmpty())
      return;

    for (Entry loaderEntry : entries.entrySet()) {
      Object loaderKey = loaderEntry.getKey();
      if (!containsKey(loaderKey) && (get(loaderKey) != null)) {
        put(loaderKey, loaderEntry.getValue());
        notifyLoad(loaderKey);
      }
    }
  }
  */

  /**
   * Adds a listener to the cache.
   */
  @Override
  public boolean registerCacheEntryListener(CacheEntryListener listener,
                                            NotificationScope scope,
                                            boolean synchronous)
  {
    _listeners.add(listener);
    
    return true;
  }

  /**
   * Removes a listener from the cache.
   */
  @Override
  public boolean unregisterCacheEntryListener(CacheEntryListener listener)
  {
    _listeners.remove(listener);
    
    return true;
  }

  /**
   * Returns the CacheStatistics for this cache.
   */
  @Override
  public CacheStatistics getStatistics()
  {
    return null; // this;
  }

  /**
   * Puts each item in the map into the cache.
   */
  @Override
  public void putAll(Map map)
  {
    if (map == null || map.size() == 0)
      return;
    Set entries = map.entrySet();

    for (Object item : entries) {
      Map.Entry entry = (Map.Entry) item;
      put(entry.getKey(), entry.getValue());
    }
  }

  /**
   * Returns true if an entry for the item is found in  the cache.
   *
   * @param key
   * @return
   */
  @Override
  public boolean containsKey(Object key)
  {
    return _entryCache.get(key) != null;
  }

  public boolean isBackup()
  {
    return _config.isBackup();
  }

  public boolean isTriplicate()
  {
    return _config.isTriplicate();
  }
  
  public HashKey getCacheKey()
  {
    return _config.getCacheKey();
  }

  /**
   * Places an item in the cache from the loader unless the item is in cache already.
   */
  protected Object cacheLoader(Object key)
  {
    Object value = get(key);

    if (value != null)
      return value;

    CacheLoader loader = _config.getCacheLoader();
    
    Object arg = null;

    value = (loader != null) ? loader.load(key) : null;

    if (value != null)
      put(key, value);
    notifyLoad(key);

    return value;
  }

  protected void notifyLoad(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onLoad(key);
    }
    */
  }

  protected void notifyEvict(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onEvict(key);
    }
    */
  }

  protected void notifyClear(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onClear();
    }
    */
  }

  protected void notifyPut(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onPut(key);
    }
    */
  }

  protected void notifyRemove(Object key)
  {
    /*
    for (CacheEntryListener listener : _listeners) {
      listener.onRemove(key);
    }
    */
  }

  @Override
  public boolean isClosed()
  {
    return _isClosed;
  }

  @Override
  public void close()
  {
    _isClosed = true;
    
    _localManager.remove(_guid);
    
    _manager.closeCache(_guid);
  }
  
  public boolean loadData(HashKey valueHash, WriteStream os)
    throws IOException
  {
    return getDataBacking().loadData(valueHash, os);
  }

  public boolean saveData(HashKey valueHash, StreamSource source, int length)
    throws IOException
  {
    return getDataBacking().saveData(valueHash, source, length);
  }

  public boolean isDataAvailable(HashKey valueKey)
  {
    return getDataBacking().isDataAvailable(valueKey);
  }
  
  private CacheDataBacking getDataBacking()
  {
    return _manager.getDataBacking();
  }

  //
  // QA
  //
  
  public byte []getKeyHash(String name)
  {
    return getDistCacheEntry(name).getKeyHash().getHash();
  }
  
  public byte []getValueHash(Object value)
  {
    return _manager.calculateValueHash(value, _config);
  }
  
  @SuppressWarnings("unchecked")
  public MnodeStore getMnodeStore()
  {
    return ((CacheStoreManager) _manager).getMnodeStore();
  }
  
  @SuppressWarnings("unchecked")
  public DataStore getDataStore()
  {
    return ((CacheStoreManager) _manager).getDataStore();
  }
  
  public void saveData(Object value)
  {
    ((CacheStoreManager) _manager).writeData(null, value, 
                                                _config.getValueSerializer());
  }
  
  public HashKey saveData(InputStream is)
    throws IOException
  {
    DataItem dataItem = ((CacheStoreManager) _manager).writeData(null, is);
    
    return dataItem.getValue();
  }

  public void setManager(CacheStoreManager manager)
  {
    if (_manager != null)
      throw new IllegalStateException();
    
    _manager = manager;
  }
  
  private void initServer()
    throws ConfigException
  {
    if (_manager != null)
      return;
    
    DistCacheSystem cacheService = DistCacheSystem.getCurrent();

    if (cacheService == null)
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a Resin environment",
                                    getClass().getSimpleName()));

    _manager = cacheService.getDistCacheManager();

    if (_manager == null)
      throw new IllegalStateException("distributed cache manager not available");
  }

  /**
   * Provides an iterator over the entries in the the local cache.
   */
  protected static class CacheEntrySetIterator<K, V>
    implements Iterator<Cache.Entry<K, V>>
  {
    private Iterator<LruCache.Entry<K, V>> _iterator;

    protected CacheEntrySetIterator(LruCache<K, V> lruCache)
    {
      _iterator = lruCache.iterator();
    }

    public Cache.Entry<K,V> next()
    {
      if (!hasNext())
        throw new NoSuchElementException();

      LruCache.Entry<K, V> entry = _iterator.next();
      Cache.Entry<K,V> cacheEntry = (Cache.Entry<K, V>) entry.getValue();

      return new EntryImpl<K, V>(cacheEntry.getKey(),
                                 cacheEntry.getValue());
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    /**
     *
     */
    public void remove()
    {
      throw new UnsupportedOperationException(getClass().getName());
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }
  
  static class EntryImpl<K,V> implements Cache.Entry<K,V>
  {
    private final K _key;
    private final V _value;
    
    EntryImpl(K key, V value)
    {
      _key = key;
      _value = value;
    }

    @Override
    public K getKey()
    {
      return _key;
    }

    @Override
    public V getValue()
    {
      return _value;
    }
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getConfiguration()
   */
  @Override
  public CacheConfiguration getConfiguration()
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  @Override
  public CacheManager getCacheManager()
  {
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#load(java.lang.Object, javax.cache.CacheLoader, java.lang.Object)
   */
  @Override
  public Future load(Object key)
      throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#loadAll(java.util.Collection, javax.cache.CacheLoader, java.lang.Object)
   */
  @Override
  public Future loadAll(Collection keys)
      throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#removeAll(java.util.Collection)
   */
  @Override
  public void removeAll(Collection keys) throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#removeAll()
   */
  @Override
  public void removeAll() throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see java.lang.Iterable#iterator()
   */
  @Override
  public Iterator iterator()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Lifecycle#getStatus()
   */
  @Override
  public Status getStatus()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Lifecycle#start()
   */
  @Override
  public void start() throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Lifecycle#stop()
   */
  @Override
  public void stop() throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#unwrap(java.lang.Class)
   */
  @Override
  public Object unwrap(Class cl)
  {
    // TODO Auto-generated method stub
    return null;
  }
}
