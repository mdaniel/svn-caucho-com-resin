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

package com.caucho.distcache;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.AbstractMap;
import java.util.AbstractSet;
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
import javax.cache.CacheStatisticsMBean;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Period;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.distcache.DistCacheSystem;
import com.caucho.loader.Environment;
import com.caucho.server.distcache.AbstractCacheManager;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.DataStore;
import com.caucho.server.distcache.DistCacheEntry;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.distcache.MnodeStore;
import com.caucho.server.distcache.MnodeValue;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * Implements the distributed cache
 */

public class AbstractCache
  implements ObjectCache, ByteStreamCache, Closeable
{
  private static final L10N L = new L10N(AbstractCache.class);

  private CacheManagerImpl _localManager;
  private DistributedCacheManager _manager;

  private String _name = null;

  private String _guid;

  private Collection<CacheEntryListener> _listeners
    = new ConcurrentLinkedQueue<CacheEntryListener>();

  private CacheConfig _config = new CacheConfig();

  private LruCache<Object,DistCacheEntry> _entryCache;

  private boolean _isInit;
  private boolean _isClosed;

  private long _priorMisses = 0;
  private long _priorHits = 0;

  private String _scopeName = Scope.CLUSTER.toString();
  private String _persistenceOption = Persistence.TRIPLE.toString();

  public AbstractCache()
  {
  }

  /**
   * Returns the name of the cache.
   */
  public String getName()
  {
    return _name;
  }

  /**
   * Assigns the name of the cache.
   * A name is mandatory and must be unique among open caches.
   */
  @Configurable
  public void setName(String name)
  {
    _name = name;
  }

  public void setGuid(String guid)
  {
    _guid = guid;
  }
  
  public String getGuid()
  {
    return _guid;
  }

  /**
   * Sets the CacheLoader that the Cache can then use to populate
   * cache misses from a reference store (database).
   */
  @Configurable
  public void setCacheLoader(CacheLoader loader)
  {
    _config.setCacheLoader(loader);
  }

  /**
   * Assign the serializer used on values.
   *
   * @Note: This setting should not be changed after
   * a cache is created.
   */
  @Configurable
  public void setSerializer(CacheSerializer serializer)
  {
    _config.setValueSerializer(serializer);
  }

  /**
   * Sets the backup mode.  If backups are enabled, copies of the
   * cache item will be sent to the owning triad server.
   * <p/>
   * Defaults to true.
   */
  @Configurable
  public void setBackup(boolean isBackup)
  {
    _config.setBackup(isBackup);
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters
   * <p/>
   * Defaults to false.
   */
  @Configurable
  public void setGlobal(boolean isGlobal)
  {
    _config.setGlobal(isGlobal);
  }

  /**
   * Sets the triplicate backup mode.  If triplicate backups is set,
   * all triad servers have a copy of the cache item.
   * <p/>
   * Defaults to true.
   */
  @Configurable
  public void setTriplicate(boolean isTriplicate)
  {
    _config.setTriplicate(isTriplicate);
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
    return _config.getExpireTimeout();
  }

  /**
   * The maximum valid time for a cached item before it expires.
   * Items stored in the cache for longer than the expire time are
   * no longer valid and will return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setExpireTimeout(Period expireTimeout)
  {
    setExpireTimeoutMillis(expireTimeout.getPeriod());
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   * <p/>
   * Default is infinite.
   */
  @Configurable
  public void setExpireTimeoutMillis(long expireTimeout)
  {
    _config.setExpireTimeout(expireTimeout);
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
  @Configurable
  public void setIdleTimeout(Period period)
  {
    setIdleTimeoutMillis(period.getPeriod());
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
    return _config.getIdleTimeout();
  }

  /**
   * Sets the idle timeout in milliseconds
   */
  @Configurable
  public void setIdleTimeoutMillis(long timeout)
  {
    _config.setIdleTimeout(timeout);
  }

  /**
   * Returns the idle check window, used to minimize traffic when
   * updating access times.
   */
  public long getIdleTimeoutWindow()
  {
    return _config.getIdleTimeoutWindow();
  }

  /**
   * Sets the idle timeout windows
   */
  public void setIdleTimeoutWindow(Period period)
  {
    _config.setIdleTimeoutWindow(period.getPeriod());
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseTimeout()
  {
    return _config.getLeaseTimeout();
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseTimeout(Period period)
  {
    setLeaseTimeoutMillis(period.getPeriod());
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  @Configurable
  public void setLeaseTimeoutMillis(long timeout)
  {
    _config.setLeaseTimeout(timeout);
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
    return _config.getLocalReadTimeout();
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalReadTimeout(Period period)
  {
    setLocalReadTimeoutMillis(period.getPeriod());
  }

  /**
   * The local read timeout sets how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  @Configurable
  public void setLocalReadTimeoutMillis(long period)
  {
    _config.setLocalReadTimeout(period);
  }

  public void setScopeMode(Scope scope)
  {
    _config.setScopeMode(scope);
  }

  /**
   * Sets the {@link Scope} of the cache.
   */
  @Configurable
  public void setScope(String scopeName)
  {
    _scopeName = scopeName;
  }

  /**
   * Returns the name of the Scope of the cache.
   * @return
   */
  public String getScope()
  {
    return _config.getScopeMode().toString().toLowerCase(Locale.ENGLISH);
  }

  public void setPersistence(String persistenceOption)
  {
    _persistenceOption = persistenceOption;
  }
  
  public void setCacheManager(CacheManagerImpl cacheManager)
  {
    if (_localManager != null && _localManager != cacheManager)
      throw new IllegalStateException();
    
    _localManager = cacheManager;
  }

  public static AbstractCache getMatchingCache(String name)
  {
    DistCacheSystem cacheService = DistCacheSystem.getCurrent();
    
    CacheManagerImpl localManager = cacheService.getCacheManager();

    String contextId = Environment.getEnvironmentName();

    String guid = contextId + ":" + name;
  
    return localManager.get(guid);
  }

  /**
   * Initialize the cache.
   */
  @PostConstruct
  public void init()
  {
    init(false);
  }
  
  public AbstractCache createIfAbsent()
  {
    init(true);
    
    return _localManager.get(_guid);
  }
  
  

  private void init(boolean ifAbsent)
  {
    synchronized (this) {
      if (_isInit)
        return;

      _isInit = true;

      _config.init();

      initServer();

      initName(_name);

      initScope(_scopeName);

      initPersistence(_persistenceOption);

      _config.setCacheKey(_manager.createSelfHashKey(_config.getGuid(),
                                                     _config.getKeySerializer()));

      if (_localManager == null) {
        DistCacheSystem cacheService = DistCacheSystem.getCurrent();
        
        _localManager = cacheService.getCacheManager();
      }
      
      if (_localManager.putIfAbsent(_guid, this) != null) {
        if (ifAbsent) {
          close();
          return;
        }
        
        throw new ConfigException(L.l("'{0}' with full name '{1}' is an invalid Cache name because it's already used by another cache.",
                                      this, _guid));
      }

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
    return getDistCacheEntry(key).get(_config);
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
    return getDistCacheEntry(key).getMnodeValue();
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
   */
  @Override
  public ExtCacheEntry put(Object key,
                           InputStream is,
                           long idleTimeout)
    throws IOException
  {
    return getDistCacheEntry(key).put(is, _config, idleTimeout);
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

  public void compareAndPut(HashKey key, HashKey value, long version)
  {
    getDistCacheEntry(key).compareAndPut(version, value, _config);
    
    notifyPut(key);
  }

  @Override
  public boolean putIfAbsent(Object key, Object value) throws CacheException
  {
    HashKey NULL = MnodeValue.NULL_KEY;
    
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
    
    return result != null && result.equals(oldHash);
  }

  @Override
  public boolean replace(Object key, Object value) throws CacheException
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    HashKey oldHash = MnodeValue.ANY_KEY;
    
    HashKey result = entry.compareAndPut(oldHash, value, _config);
    
    return result != null && ! result.isNull();
  }

  @Override
  public Object getAndReplace(Object key, Object value) throws CacheException
  {
    DistCacheEntry entry = getDistCacheEntry(key);
    
    HashKey oldHash = MnodeValue.ANY_KEY;
    
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
    DistCacheEntry cacheEntry = _entryCache.get(key);

    if (cacheEntry == null) {
      cacheEntry = _manager.getCacheEntry(key, _config);

      _entryCache.put(key, cacheEntry);
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
                                            NotificationScope scope)
  {
    _listeners.add(listener);
    
    return true;
  }

  /**
   * Removes a listener from the cache.
   */
  @Override
  public boolean unregisterCacheEntryListener(CacheEntryListener listener,
                                              NotificationScope scope)
  {
    _listeners.remove(listener);
    
    return true;
  }

  /**
   * Returns the CacheStatistics for this cache.
   */
  @Override
  public CacheStatisticsMBean getCacheStatistics()
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

    value = (loader != null) ? loader.load(key, arg) : null;

    if (value != null)
      put(key, value);
    notifyLoad(key);

    return value;
  }

  protected  void setPersistenceMode(Persistence persistence)
  {
      switch (persistence) {
      case NONE:
        setTriplicate(false);
        setBackup(false);
        break;

      case SINGLE:
        setTriplicate(false);
        setBackup(true);
        break;

      case TRIPLE:
      default:
        setTriplicate(true);
    }
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
  }

  private void initName(String name)
    throws ConfigException
  {
    if (_name == null || _name.length() == 0)
      throw new ConfigException(L.l("Each Cache must have a name."));

    String contextId = Environment.getEnvironmentName();

    if (_guid == null)
      _guid = contextId + ":" + _name;

    _config.setGuid(_guid);
  }

  private void initPersistence(String persistence)
    throws ConfigException
  {
    Persistence result  = Persistence.TRIPLE;

    if (persistence != null) {
      try {
        result = Persistence.valueOf(persistence.toUpperCase(Locale.ENGLISH));
      }
      catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is not a valid Persistence option", persistence));
      }
    }

    setPersistenceMode(result);
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
    return ((AbstractCacheManager) _manager).getMnodeStore();
  }
  
  @SuppressWarnings("unchecked")
  public DataStore getDataStore()
  {
    return ((AbstractCacheManager) _manager).getDataStore();
  }
  
  public void saveData(Object value)
  {
    ((AbstractCacheManager) _manager).writeData(null, value, 
                                                _config.getValueSerializer());
  }

  private void initScope(String scopeName)
    throws ConfigException
  {
    Scope scope = null;

    if (_scopeName != null) {
      try {
        scope = Scope.valueOf(_scopeName.toUpperCase(Locale.ENGLISH));
      }
      catch (Exception e) {
        throw new ConfigException(L.l("'{0}' is not a valid Scope option", scopeName));
      }
    }

    setScopeMode(scope);
  }

  public void setManager(DistributedCacheManager manager)
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
      throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a clustered environment",
                                    getClass().getSimpleName()));

    _manager = cacheService.getDistCacheManager();

    if (_manager == null)
      throw new IllegalStateException("distributed cache manager not available");
  }


  /**
   * Defines the scope options for a cache.
   */
  public enum Scope {

    /** Not distributed, no persistence.*/
    LOCAL,

    /** Not distributed, single or no persistence */
    SERVER,

    /** Distributed across a pod, persistence required.*/
    POD,

    /** Accessible across a multi-pod cluster*/
    CLUSTER,

    /** Support CRUD operation with basic access control*/
    GLOBAL
  }

  /**
   * Defines the persistence options for a cache.
   */
  public enum Persistence {

    /**
     * No persistence.
     */
    NONE,

    /**
     * A single copy of the cache is persisted on one server in the cluster.
     */
    SINGLE,

    /**
     * Three copies of the cache and its entrys are saved on three servers.
     */
    TRIPLE
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
   * @see javax.cache.Cache#getCacheName()
   */
  @Override
  public String getCacheName()
  {
    // TODO Auto-generated method stub
    return null;
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

  /* (non-Javadoc)
   * @see javax.cache.Cache#load(java.lang.Object, javax.cache.CacheLoader, java.lang.Object)
   */
  @Override
  public Future load(Object key, CacheLoader loader, Object arg)
      throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#loadAll(java.util.Collection, javax.cache.CacheLoader, java.lang.Object)
   */
  @Override
  public Future loadAll(Collection keys, CacheLoader loader, Object arg)
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
}
