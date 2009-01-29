/*
 * Copyright (c) 1998-2009 Caucho Technology -- all rights reserved
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

package com.caucho.cluster;

import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.loader.Environment;
import com.caucho.server.cluster.Server;
import com.caucho.server.distcache.*;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheListener;
import javax.cache.CacheLoader;
import javax.cache.CacheStatistics;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Implements the distributed cache
 */
abstract public class AbstractCache extends AbstractMap
        implements ObjectCache, ByteStreamCache
{
  public static final String CACHE_NAME = "javax.cache.cache.name";

  public static final String CACHE_CONFIGURATION = "javax.cache.cache.configuration";

  private static final L10N L = new L10N(AbstractCache.class);

  private static final Logger log
    = Logger.getLogger(AbstractCache.class.getName());

  private String _name = null;

  private String _guid;

  private Collection<CacheListener> _listeners =
          new ConcurrentLinkedQueue<CacheListener>();

  private CacheConfig _config = new CacheConfig();

  private LruCache<Object, CacheKeyEntry> _entryCache
          = new LruCache<Object, CacheKeyEntry>(512);

  private boolean _isInit;

  private DistributedCacheManager _distributedCacheManager;

  //private CacheStatisticsManager.ManagedCacheStatistics _cacheStatistics;

  private AtomicReference<CacheStatisticsManager.ManagedCacheStatistics>  _cacheStats;


  private HashManager _hashManager = new HashManager();

  /**
   * Assign the name.  The name is mandatory and must be unique.
   */
  public void setName(String name)
  {
    _name = name;
  }

  public String getName()
  {
    return _name;
  }


  /**
   * Added for testing support.
   * @param config
   */
  protected void setConfig(CacheConfig config) {
    if (config != null) {
      synchronized(this) {
        if (!_isInit) {
          _config = config;
      } else {
          throw new ConfigException("This cache has already been configured!");
        }
    }
    }
  }

  /**
   * Assign the CacheLoader to populate the cache on a miss.
   */
  public void setCacheLoader(CacheLoader loader)
  {
    _config.setCacheLoader(loader);
  }

  /**
   * Assign the serializer
   */
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
  public void setBackup(boolean isBackup)
  {
    _config.setSinglePersistence(isBackup);
  }

  /**
   * Sets the triplicate backup mode.  If triplicate backups is set,
   * all triad servers have a copy of the cache item.
   * <p/>
   * Defaults to true.
   */
  public void setTriplicate(boolean isTriplicate)
  {
    _config.setTriplePersistence(isTriplicate);
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
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
  public void setIdleTimeoutMillis(long timeout)
  {
    _config.setIdleTimeout(timeout);
  }

  /**
   * Returns the idle check window, used to minimize traffic when
   * updating access times.
   */
  public long getIdleCheckWindow()
  {
    return _config.getIdleTimeoutWindow();
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
  public void setLeaseTimeout(Period period)
  {
    setLeaseTimeoutMillis(period.getPeriod());
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public void setLeaseTimeoutMillis(long timeout)
  {
    _config.setLeaseTimeout(timeout);
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
  public void setLocalReadTimeoutMillis(long period)
  {
    _config.setLocalReadTimeout(period);
  }

  /**
   * The local read timeout is how long a local copy of
   * a cache item can be reused before checking with the master copy.
   * <p/>
   * A read-only item could be infinite (-1).  A slow changing item
   * like a list of bulletin-board comments could be 10s.  Even a relatively
   * quicky changing item can be 10ms or 100ms.
   * <p/>
   * The default is 10ms
   */
  public long getLocalReadTimeout()
  {
    return _config.getLocalReadTimeout();
  }

  /**
   * Sets the accuracy, and returns the current statistics.
   * @param accuracy
   * @return
   */
  public CacheStatistics setCacheStatisticsAccuracy(int accuracy)
  {
    return new ReadOnlyStatistics
            (_cacheStats.getAndSet(CacheStatisticsManager.getInstance(this, accuracy)));

  }


  /**
   * Initialize the cache
   */
  @PostConstruct
  public void init()
  {
    synchronized (this) {

      if (_isInit)
        return;
      _isInit = true;

      //TODO(fred): remove once injecfion sets the name.
      _name = ((_name == null) || (_name.length() == 0)) ? "default" : _name;

      if ((_name == null) || (_name.length() == 0))
        throw new ConfigException(L.l("'name' is a required attribute for any Cache"));

      String _host = (Environment.getEnvironmentName());

      String contextId = Environment.getEnvironmentName();

      _guid = contextId + ":" + _name;

      _config.setGuid(_guid);

     //DistributedCacheManager.ServerCacheRegistry.add(this);

      _config.init();

      Server server = Server.getCurrent();

      if (server == null)
        throw new ConfigException(L.l("'{0}' cannot be initialized because it is not in a clustered environment",
                getClass().getSimpleName()));

      _distributedCacheManager = server.getDistributedCacheManager();


      _cacheStats
              = new AtomicReference<CacheStatisticsManager.ManagedCacheStatistics>(
           CacheStatisticsManager.getInstance(this, _config.getCacheStatisticsAccuraccy()));

    }
  }

  /**
   * Returns the object with the given key without checking the backing store.
   */
  public Object peek(Object key)
  {
    CacheKeyEntry entry = _entryCache.get(key);

    if (entry == null)
      return null;
    else
      return entry.peek();
  }

  /**
   * Returns the object with the given key, checking the backing store if
   * necessary.
   */
  public Object get(Object key)
  {
    Object result =  getKeyEntry(key).get(_config);

    if (result == null)
      _entryCache.remove(key);
    return result;
  }

  /**
   * Fills an output stream with the value for a key.
   */
  public boolean get(Object key, OutputStream os)
          throws IOException
  {
    return getKeyEntry(key).getStream(os, _config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public ExtCacheEntry getExtCacheEntry(Object key)
  {
    return getKeyEntry(key).getEntry(_config);
  }

  /**
   * Returns the cache entry for the object with the given key.
   */
  public ExtCacheEntry getCacheEntry(Object key)
  {
    return getExtCacheEntry(key);
  }

  /**
   * Puts a new item in the cache.
   *
   * @param key   the key of the item to put
   * @param value the value of the item to put
   */
  public Object put(Object key, Object value)
  {
    Object object = getKeyEntry(key).put(value, _config);
    notifyPut(key);
    logFlow("put : ", key, value, object);
    return object;
  }

  protected static void logFlow(String msg, Object... objects) {
    StringBuilder sb = new StringBuilder().append(msg);
     for (Object object : objects) {

       if (object == null)
         sb.append("null,");
       else
         sb.append(object.toString()).append(",");
     }
     sb.setLength(sb.length() - 1);
     log.log(Level.FINE, sb.toString());
  }

  /**
   * Puts a new item in the cache with a custom idle
   * timeout (used for sessions).
   *
   * @param key         the key of the item to put
   * @param is          the value of the item to put
   * @param idleTimeout the idle timeout for the item
   */
  public ExtCacheEntry put(Object key,
          InputStream is,
          long idleTimeout)
          throws IOException
  {
    return getKeyEntry(key).put(is, _config, idleTimeout);
  }

  /**
   * Updates the cache if the old version matches the current version.
   * A zero value for the old value hash only adds the entry if it's new
   *
   * @param key     the key to compare
   * @param version the version of the old value, returned by getEntry
   * @param value   the new value
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
          long version,
          Object value)
  {
    put(key, value);
    return true;
  }

  /**
   * Updates the cache if the old version matches the current value.
   * A zero value for the old version only adds the entry if it's new
   *
   * @param key         the key to compare
   * @param version     the hash of the old version, returned by getEntry
   * @param inputStream the new value
   * @return true if the update succeeds, false if it fails
   */
  public boolean compareAndPut(Object key,
          long version,
          InputStream inputStream)
          throws IOException
  {
    put(key, inputStream);

    return true;
  }

  /**
   * Removes the entry from the cache
   *
   * @return true if the object existed
   */
  public Object remove(Object key)
  {
    Object value = get(key);

    if (value != null) {
       CacheKeyEntry keyEntry = getKeyEntry(key);
       keyEntry.remove(_config);
       keyEntry.put(null, _config);
       notifyRemove(key);
       _entryCache.remove(key);
    }
    return value;
  }

  /**
   * Removes the entry from the cache if the current entry matches the version
   */
  public boolean compareAndRemove(Object key, long version)
  {
    // HashKey hashKey = getHashKey(key);

    return false;
  }




  /**
   * Returns the CacheKeyEntry for the given key.
   */
  protected CacheKeyEntry getKeyEntry(Object key)
  {
    CacheKeyEntry entry = _entryCache.get(key);

    if (entry == null) {
      entry = _distributedCacheManager.getKey(key, _config);
      _cacheStats.get().incrementMisses(1);
      _entryCache.put(key, entry);
    } else {
      _cacheStats.get().incrementHits(1);
    }

    return entry;
  }


  public Set<Map.Entry> entrySet()    {
    return new CacheEntrySet<Entry>(_entryCache);

  }

  protected static class CacheEntrySetIterator<K, V> implements Iterator {
    private Iterator<LruCache.Entry<K, V>> _iterator;

    protected CacheEntrySetIterator(LruCache<K, V> lruCache)
    {
      _iterator = lruCache.iterator();
    }

    public Object next()
    {
      if (!hasNext())
        throw new NoSuchElementException();
      LruCache.Entry<K, V> entry = _iterator.next();
      CacheKeyEntry cacheKeyEntry = (CacheKeyEntry) entry.getValue();

      return new AbstractMap.SimpleEntry<Object, Object>(
              entry.getKey(),
              cacheKeyEntry.getEntry().getValue());
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  protected static class CacheValuesIterator extends CacheEntrySetIterator {

    public CacheValuesIterator(LruCache lruCache)
    {
      super(lruCache);
    }

    @Override
    public Object next()
    {
      return ((Entry) super.next()).getValue();
    }

  }

  protected static class CacheKeysIterator<K, V> implements Iterator {
    private Iterator<LruCache.Entry<K, V>> _iterator;

    protected CacheKeysIterator(LruCache<K, V> lruCache)
    {
      _iterator = lruCache.iterator();
    }

    public Object next()
    {
      if (!hasNext())
        throw new NoSuchElementException();
      LruCache.Entry<K, V> entry = _iterator.next();
      return entry.getKey();
    }

    public boolean hasNext()
    {
      return _iterator.hasNext();
    }

    public void remove()
    {
      throw new UnsupportedOperationException();
    }
  }

  //TODO(fred): block other collection methods.
  protected static class CacheValues extends AbstractCollection {

    private LruCache _lruCache;

    public CacheValues(LruCache lruCache)
    {
      _lruCache = lruCache;
    }

    public int size()
    {
      return (_lruCache != null) ? _lruCache.size() : 0;
    }

    public Iterator iterator() {
      return new CacheValuesIterator(_lruCache);
    }
  }

  //TODO(fred): block other set methods.
  protected static class CacheKeys extends AbstractSet{

      private LruCache _lruCache;

      public CacheKeys(LruCache lruCache)
      {
        _lruCache = lruCache;
      }

      public int size()
      {
        return (_lruCache != null) ? _lruCache.size() : 0;
      }

      public Iterator iterator() {
        return new CacheKeysIterator(_lruCache);
      }
    }

  //TODO(fred): block other set methods.
  protected static class CacheEntrySet<E> extends AbstractSet<E> {
    private LruCache _lruCache;

    protected CacheEntrySet(LruCache cache)
    {

      _lruCache = cache;
    }

    public Iterator iterator()
    {
      return new CacheEntrySetIterator(_lruCache);
    }

    public void clear()
    {
      throw new UnsupportedOperationException();
    }

    public boolean remove(Object entry)
    {
      throw new UnsupportedOperationException();
    }

    public int size()
    {
      return (_lruCache != null) ? _lruCache.size() : 0;
    }

    public boolean contains(Object entry)
    {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * Returns a map of the items if found in the central cache.
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
   * Loads an item into the cache if found by the CacheLoader.
   * @param key
   */
  public void load(Object key)
  {
    Object value = cacheLoader(key);

    if (value != null)
      notifyLoad(key);
  }

  /**
   * Implements the loadAll method for a collection of keys.
   *
   * The items are first obtained from the loader and added to the cache.
   * Items not found from the loader may be in the backing store, and are fetched from there.
   */
  public void loadAll(Collection keys)
  {
    if (keys == null || keys.size() == 0) return;
    CacheLoader loader = _config.getCacheLoader();
    Map entries = null;

    if (loader != null) {
      entries = loader.loadAll(keys);

      if (entries.size() > 0) {
        putAll(entries);
      }
    }

    for (Object key : keys) {

      if (!containsKey(key))
        get(key);
    }

    if (_listeners.size() > 0) {
      for (Object key : keySet()) {
        notifyLoad(key);
      }
    }
  }

  /**
   * Add a listener to the cache
   */
  public void addListener(CacheListener listener)
  {
      _listeners.add(listener);
  }

  /**
   * Remove a listener from the cache.
   */
  public void removeListener(CacheListener listener)
  {
      _listeners.remove(listener);
  }

  public CacheStatistics getCacheStatistics()
  {
    return _cacheStats.get();
  }

  /**
   * Ignored, since evictions are handled by the container.
   */
  public void evict()
  {
    notifyEvict(null);
  }

  public Collection values() {
    return new CacheValues(_entryCache);
  }

  public Set keySet() {
    return new CacheKeys(_entryCache);
  }

  public boolean containsValue(Object value)
  {
    /**
     *  TODO(fred): compute the hash of the value to speed this linear search
     *  Other optimizations are possible.
     */
    Iterator<CacheKeyEntry> iterator = _entryCache.values();

    while (iterator.hasNext()) {
      if (iterator.next().getEntry().getValue().equals(value)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _guid + "]";
  }

  @Override
  public void clear()
  {
    _entryCache.clear();
    notifyClear(null);
  }

  @Override
  public int size()
  {
    return _entryCache.size();
  }

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

  @Override
  public boolean containsKey(Object key)
  {
    return _entryCache.get(key) != null;
  }

  /**
   * Returns true is the local cache is empty
   * @return
   */
  @Override
  public boolean isEmpty()
  {
    return _entryCache.size() == 0;
  }

  /**
   * Places an item in the cache if found in the loader, replacing
   * an existing entry if it had been present.
   */
  protected Object cacheLoader(Object key) {
    CacheLoader loader = _config.getCacheLoader();
    Object value = (loader != null) ? loader.load(key) : null;

    if (value != null) {
      put(key, value);
    } else {
      value = get(key);
    }

    return value;
  }

  protected void notifyLoad(Object key)
  {
    if (_listeners.size() > 0) {
      CacheListeners.notifyLoad(_listeners, key);
    }
  }

  protected void notifyEvict(Object key)
  {
    if (_listeners.size() > 0) {
      CacheListeners.notifyEvict(_listeners, key);
    }
  }

  protected void notifyClear(Object key)
  {
    if (_listeners.size() > 0) {
      CacheListeners.notifyClear(_listeners, key);
    }
  }

  protected void notifyPut(Object key)
  {
    if (_listeners.size() > 0) {
      CacheListeners.notifyPut(_listeners, key);
    }
  }

  protected void notifyRemove(Object key)
  {
    if (_listeners.size() > 0) {
      CacheListeners.notifyRemove(_listeners, key);
    }
  }

  /**
   * Used in the implementation of the
   * {@link javax.cache.CacheFactory} createCache method.
   * @param request
   * @return
   */
  public static final Cache getInstance(Map request)
  {
    String cacheName = (String) request.get(CACHE_NAME);
    //TODO: return the appropriate cache type for the environment.
    //TODO: accept a CacheConfiguration
    ClusterCache cache = new ClusterCache(cacheName);
    cache.init();
    return cache;
  }

  /**
   * Process the CacheListeners defined for this cache.
   */
  //TODO(fred): Dispatch to a thread, threadpool, or thread per event type.
  public static class CacheListeners {

    public static <K> void notifyLoad(Collection<CacheListener> listeners, K key)
    {
      for (CacheListener<K> listener : listeners) {
        listener.onLoad(key);
      }
    }

    public static <K> void notifyEvict(Collection<CacheListener> listeners, K key)
    {
      for (CacheListener<K> listener : listeners) {
        listener.onEvict(key);
      }
    }

    public static <K> void notifyClear(Collection<CacheListener> listeners, K key)
    {
      for (CacheListener<K> listener : listeners) {
        listener.onClear(key);
      }
    }

    public static <K> void notifyPut(Collection<CacheListener> listeners, K key)
    {
      for (CacheListener<K> listener : listeners) {
        listener.onPut(key);
      }
    }

    public static <K> void notifyRemove(Collection<CacheListener> listeners, K key)
    {
      for (CacheListener<K> listener : listeners) {
        listener.onRemove(key);
      }
    }
  }



  /**
   * Provides instances of the appropriate CacheStatistics implementation.
   */
  public static class CacheStatisticsManager {

    /**
     * Extends the CacheStatistics to enable counting
     */
    public interface ManagedCacheStatistics extends CacheStatistics {

      public void incrementHits(int hits);

      public void incrementMisses(int hits);
    }

    private static final ManagedCacheStatistics NO_STATISTICS
            = new NoStatistics(null);

    /**
     * Provide the appropriate implementation based on the requested accuracy
     */
    public static ManagedCacheStatistics getInstance(AbstractCache cache, int accuracy)
    {
      switch (accuracy) {
        default:
        case CacheStatistics.STATISTICS_ACCURACY_NONE:
          return NO_STATISTICS;
        case CacheStatistics.STATISTICS_ACCURACY_BEST_EFFORT:
          return new GoodStatistics(cache);
        case CacheStatistics.STATISTICS_ACCURACY_GUARANTEED:
          return new GuaranteedStatistics(cache);
      }
    }

    /**
     * Block instantiation.
     */
    private CacheStatisticsManager()
    {

    }

    /**
     * Default implementation of CacheStatistics - nothing is collected.
     */
    private static class NoStatistics implements CacheStatistics, ManagedCacheStatistics {
      private AbstractCache _cache;

      private NoStatistics(AbstractCache cache)
      {
        super();
        _cache = cache;
      }

      public int getCacheHits()
      {
        return 0;
      }

      public int getCacheMisses()
      {
        return 0;
      }

      public int getObjectCount()
      {
        return 0;
      }

      public void clearStatistics()
      {
        return;
      }

      public int getStatisticsAccuracy()
      {
        return STATISTICS_ACCURACY_NONE;
      }

      public void incrementHits(int hits)
      {
        return;
      }

      public void incrementMisses(int misses)
      {
        return;
      }
    }

    /**
     * Implements CacheStatistics without worrying about synchronizing
     * counter updates.
     */
    private static class GoodStatistics implements CacheStatistics, ManagedCacheStatistics
    {
      private AbstractCache _cache;
      private int _cacheHits;
      private int _cacheMisses;

      protected GoodStatistics(AbstractCache cache)
      {
        super();
        _cache = cache;
      }

      public int getCacheHits()
      {
        return _cacheHits;
      }

      public int getCacheMisses()
      {
        return _cacheMisses;
      }

      public int getObjectCount()
      {
        return _cache != null ? _cache.size() : 0;
      }

      public void clearStatistics()
      {
        _cacheHits = 0;
        _cacheMisses = 0;
      }

      public int getStatisticsAccuracy()
      {
        return STATISTICS_ACCURACY_BEST_EFFORT;
      }

      @Override
      public void incrementHits(int hits)
      {
        _cacheHits += hits;
      }

      @Override
      public void incrementMisses(int misses)
      {
        _cacheMisses += misses;
      }
    }

    private static class GuaranteedStatistics implements CacheStatistics, ManagedCacheStatistics {
      private AbstractCache _cache;
      private AtomicInteger _cacheHits;
      private AtomicInteger _cacheMisses;

      protected GuaranteedStatistics(AbstractCache cache)
      {
        super();
        _cache = cache;
        _cacheHits = new AtomicInteger(0);
        _cacheMisses = new AtomicInteger(0);
      }

      public int getCacheHits()
      {
        return _cacheHits.get();
      }

      public int getCacheMisses()
      {
        return _cacheMisses.get();
      }

      public int getObjectCount()
      {
        return _cache != null ? _cache.size() : 0;
      }

      public void clearStatistics()
      {
        _cacheHits.set(0);
        _cacheMisses.set(0);
      }

      public int getStatisticsAccuracy()
      {
        return STATISTICS_ACCURACY_GUARANTEED;
      }

      @Override
      public void incrementHits(int hits)
      {
        _cacheHits.getAndAdd(hits);
      }

      @Override
      public void incrementMisses(int misses)
      {
        _cacheMisses.getAndAdd(misses);
      }
    }
  }
    private static class ReadOnlyStatistics implements CacheStatistics {

      private final int _cacheHits;
      private final int _cacheMisses;
      private final int _size;
      private final int _accuracy;


      public ReadOnlyStatistics(CacheStatistics old)
      {
        _cacheHits = old.getCacheHits();
        _cacheMisses = old.getCacheMisses();
        _size = old.getObjectCount();
        _accuracy = old.getStatisticsAccuracy();
      }

      public void clearStatistics()
      {

      }

      public int getCacheHits()
      {
        return _cacheHits;
      }

      public int getCacheMisses()
      {
        return _cacheMisses;
      }

      public int getObjectCount()
      {
        return _size;
      }

      public int getStatisticsAccuracy()
      {
        return _accuracy;
      }
    }
  }
