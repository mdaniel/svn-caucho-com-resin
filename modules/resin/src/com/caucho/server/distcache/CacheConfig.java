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

import java.util.concurrent.TimeUnit;

import com.caucho.cache.CacheBuilder;
import com.caucho.cache.CacheLoader;
import com.caucho.cache.CacheWriter;
import com.caucho.cache.Configuration;
import com.caucho.cache.ExpiryPolicy;
import com.caucho.cache.transaction.IsolationLevel;
import com.caucho.cache.transaction.Mode;
import com.caucho.config.Configurable;
import com.caucho.distcache.AbstractCache;
import com.caucho.distcache.CacheSerializer;
import com.caucho.distcache.HessianSerializer;
import com.caucho.distcache.ResinCacheBuilder.Scope;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;

/**
 * Manages the distributed cache
 */
public class CacheConfig implements Configuration
{
  public static final long TIME_INFINITY  = Long.MAX_VALUE / 2;
  public static final long TIME_HOUR  = 3600 * 1000L;
  public static final int FLAG_TRANSIENT  = 0x01;
  public static final int FLAG_BACKUP = 0x02;
  public static final int FLAG_TRIPLICATE = 0x04;
  
  public static final int FLAG_CLUSTER = 0x08;
  public static final int FLAG_GLOBAL = 0x10;

  private String _guid;
  private int _guidHash;
  
  private CacheHandle _cache;

  private int _flags = (FLAG_BACKUP | FLAG_TRIPLICATE);

  private long _modifiedExpireTimeout = TIME_INFINITY;
  private long _modifiedExpireTimeoutWindow = 0;

  private long _accessedExpireTimeout = TIME_INFINITY;
  private long _accessedExpireTimeoutWindow = -1;

  private long _localExpireTimeout
    = CurrentTime.isTest() ? -1 : 250L; // 250ms default timeout, except for QA

  private long _leaseExpireTimeout = 5 * 60 * 1000; // 5 min lease timeout

  private AbstractCache.Scope _scope = Scope.CLUSTER;

  private boolean _isReadThrough;
  private CacheLoaderExt _cacheLoader;
  private long _readThroughExpireTimeout = TIME_INFINITY;
  
  private boolean _isWriteThrough;
  private CacheWriterExt _cacheWriter;
  
  private boolean _isStoreByValue = true;
  private boolean _isStatisticsEnabled;
  private boolean _isTransactionEnabled;

  private CacheSerializer _keySerializer;
  private CacheSerializer _valueSerializer;

  private CacheEngine _engine = new AbstractCacheEngine();
  private ExpiryPolicy _expiryPolicy;
  
  public CacheConfig()
  {
    _expiryPolicy = new ExpiryPolicy.Default();
  }
  
  public CacheConfig(Configuration cfg)
  {
    setStoreByValue(cfg.isStoreByValue());
    _expiryPolicy = cfg.getExpiryPolicy();
    
    setWriteThrough(cfg.isWriteThrough());
    setCacheWriter(cfg.getCacheWriter());
    
    setReadThrough(cfg.isReadThrough());
    setCacheLoader(cfg.getCacheLoader());
    
    setStatisticsEnabled(cfg.isStatisticsEnabled());
  }

  /**
   * The Cache will use a CacheLoader to populate cache misses.
   */
  @Override
  public CacheLoader getCacheLoader()
  {
    return _cacheLoader;
  }

  /**
   * The Cache will use a CacheLoader to populate cache misses.
   */
  public CacheLoaderExt getCacheLoaderExt()
  {
    return _cacheLoader;
  }

  /**
   * Sets the CacheLoader that the Cache can then use to
   * populate cache misses for a reference store (database)
   */
  public void setCacheLoader(CacheLoader cacheLoader)
  {
    if (cacheLoader == null) {
      _cacheLoader = null;
    }
    else if (cacheLoader instanceof CacheLoaderExt) {
      _cacheLoader = (CacheLoaderExt) cacheLoader;
    }
    else {
      _cacheLoader = new CacheLoaderAdapter(cacheLoader);
    }
  }

  @Override
  public boolean isReadThrough()
  {
    return _isReadThrough;
  }
  
  public void setReadThrough(boolean isReadThrough)
  {
    _isReadThrough = isReadThrough;
  }

  public long getReadThroughExpireTimeout()
  {
    return _readThroughExpireTimeout;
  }

  public void setReadThroughExpireTimeout(long timeout)
  {
    _readThroughExpireTimeout = timeout;
  }

  @Override
  public CacheWriter getCacheWriter()
  {
    return _cacheWriter;
  }
  
  public void setCacheWriter(CacheWriter cacheWriter)
  {
    if (cacheWriter == null) {
      _cacheWriter = null;
    }
    else if (cacheWriter instanceof CacheWriterExt) {
      _cacheWriter = (CacheWriterExt) cacheWriter;
    }
    else {
      _cacheWriter = new CacheWriterAdapter(cacheWriter);
    }
  }

  /**
   * The Cache will use a CacheWriter to write-through on puts
   */
  public CacheWriterExt getCacheWriterExt()
  {
    return _cacheWriter;
  }

  @Override
  public boolean isWriteThrough()
  {
    return _isWriteThrough;
  }

  public void setWriteThrough(boolean isWriteThrough)
  {
    _isWriteThrough = isWriteThrough;
  }

  /**
   * Returns the globally-unique id for the cache.
   */
  public String getGuid()
  {
    return _guid;
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public void setGuid(String guid)
  {
    _guid = guid;
    _guidHash = guid.hashCode();
  }
  
  public int getGuidHash()
  {
    return _guidHash;
  }

  /**
   * Returns the globally-unique id for the cache.
   */
  public HashKey getCacheKey()
  {
    return _cache.getCacheKey();
  }

  /**
   * Sets the globally-unique id for the cache
   */
  public void setCache(CacheHandle cache)
  {
    _cache = cache;
  }
  
  public CacheHandle getCache()
  {
    return _cache;
  }

  /**
   * Returns internal flags
   */
  public int getFlags()
  {
    return _flags;
  }

  /**
   * Sets internal flags
   */
  public void setFlags(int flags)
  {
    _flags = flags;
  }

  /**
   * The maximum valid time for an item after a modification.
   * Items stored in the cache for longer than the expire time
   * are no longer valid and will return null from a get.
   *
   * Default is infinite.
   */
  public long getModifiedExpireTimeout()
  {
    return _modifiedExpireTimeout;
  }

  /**
   * The maximum valid time for an item.  Items stored in the cache
   * for longer than the expire time are no longer valid and will
   * return null from a get.
   *
   * Default is infinite.
   */
  @Configurable
  public void setModifiedExpireTimeout(long expireTimeout)
  {
    if (expireTimeout < 0 || TIME_INFINITY <= expireTimeout)
      expireTimeout = TIME_INFINITY;

    _modifiedExpireTimeout = expireTimeout;
  }
  
  /**
   * Returns the expire check window, i.e. the precision of the expire
   * check.  Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  public long getModifiedExpireTimeoutWindow()
  {
    return (_modifiedExpireTimeoutWindow > 0
            ? _modifiedExpireTimeoutWindow
            : _modifiedExpireTimeout / 4);
  }

  /**
   * Provides the opportunity to control the expire check window,
   * i.e. the precision of the expirecheck.
   * <p/>
   * Since an expired item can cause a massive cascade of
   * attempted loads from the backup, the actual expiration is randomized.
   */
  @Configurable
  public void setModifiedExpireTimeoutWindow(long expireTimeoutWindow)
  {
    _modifiedExpireTimeoutWindow = expireTimeoutWindow;
  }

  /**
   * The maximum time that an item can remain in cache without being referenced.
   * For example, session data could be configured to be removed if idle for more than 30 minutes.
   * <p/>
   * Cached data would typically have infinite idle time because
   * it doesn't depend on how often it's accessed.
   *
   * Default is infinite.
   */
  public long getAccessedExpireTimeout()
  {
    return _accessedExpireTimeout;
  }

  /**
   * The maximum time that an item can remain in cache without being referenced.
   * For example, session data could be configured to be removed if idle for more than 30 minutes.
   *
   * Cached data would typically use an infinite idle time because
   * it doesn't depend on how often it's accessed.
   */
  public void setAccessedExpireTimeout(long timeout)
  {
    if (timeout < 0 || TIME_INFINITY <= timeout)
      timeout = TIME_INFINITY;

    _accessedExpireTimeout = timeout;
  }
  
  /**
   * Returns the idle check window, i.e. the precision of the idle
   * check.
   */
  public long getAccessedExpireTimeoutWindow()
  {
    return (_accessedExpireTimeoutWindow > 0
            ? _accessedExpireTimeoutWindow
            : _accessedExpireTimeout / 4);
  }

  /**
   * Provides the option to set the idle check window,  the amount of time
   * in which the idle time limit can be spread out to smooth performance.
   * <p/>
   * If this optional value is not set, the system  uses a fraction of the
   * idle time.
   */
  public void setAccessedExpireTimeoutWindow(long idleTimeoutWindow)
  {
    _accessedExpireTimeoutWindow = idleTimeoutWindow;
  }

  /**
   * Returns the lease timeout, which is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public long getLeaseExpireTimeout()
  {
    return _leaseExpireTimeout;
  }

  /**
   * The lease timeout is the time a server can use the local version
   * if it owns it, before a timeout.
   */
  public void setLeaseExpireTimeout(long timeout)
  {
    _leaseExpireTimeout = timeout;
  }

  /**
   * The local read timeout is the time a local copy of the
   * cache is considered valid without checking the backing store.
   */
  public long getLocalExpireTimeout()
  {
    return _localExpireTimeout;
  }

  /**
   * The local expire time is the time a local copy of the
   * cache is considered valid.
   */
  public void setLocalExpireTimeout(long timeout)
  {
    _localExpireTimeout = timeout;
  }
  
  /**
   * Returns true if all gets are synchronous, i.e. must be checked with
   * the owning server on each request.
   */
  public boolean isSynchronousGet()
  {
    return getLocalExpireTimeout() <= 0;
  }

  /**
   * Returns the key serializer
   */
  public CacheSerializer getKeySerializer()
  {
    return _keySerializer;
  }

  /**
   * Returns the value serializer
   */
  public CacheSerializer getValueSerializer()
  {
    return _valueSerializer;
  }

  /**
   * Sets the value serializer
   */
  public void setValueSerializer(CacheSerializer serializer)
  {
    _valueSerializer = serializer;
  }

  public boolean isBackup()
  {
    return (getFlags() & CacheConfig.FLAG_BACKUP) != 0;
  }
  
  public static boolean isBackup(int flags)
  {
    return (flags & CacheConfig.FLAG_BACKUP) != 0;
  }

  /**
   * Sets the backup mode.  If backups are enabled, copies of the
   * cache item will be sent to the owning triad server.
   * <p/>
   * Defaults to true.
   */
  private void setBackup(boolean isBackup)
  {
    if (isBackup)
      setFlags(getFlags() | CacheConfig.FLAG_BACKUP);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_BACKUP);
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters.
   * <p/>
   * Defaults to false.
   */
  public boolean isGlobal()
  {
    return (getFlags() & CacheConfig.FLAG_GLOBAL) != 0;
  }
  
  public static boolean isGlobal(int flags)
  {
    return (flags & CacheConfig.FLAG_GLOBAL) != 0;
  }

  /**
   * Sets the global mode.  If global is enabled, copies of the
   * cache item will be sent to all clusters.
   * <p/>
   * Defaults to false.
   */
  public void setGlobal(boolean isGlobal)
  {
    if (isGlobal)
      setFlags(getFlags() | CacheConfig.FLAG_GLOBAL);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_GLOBAL);
  }

  /**
   * Returns true is the triplicate backup mode enabled so that
   * all triad servers have a copy of the cache item.
   * <p/>
   * Defaults is true.
   */
  public boolean isTriplicate()
  {
    return (getFlags() & CacheConfig.FLAG_TRIPLICATE) != 0;
  }
  
  public static boolean isTriplicate(int flags)
  {
    return (flags & CacheConfig.FLAG_TRIPLICATE) != 0;
  }

  /**
   * Sets the transient mode.
   */
  private void setTransient(boolean isTransient)
  {
    if (isTransient)
      setFlags(getFlags() | CacheConfig.FLAG_TRANSIENT);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_TRANSIENT);
  }

  /**
   * Sets the triplicate backup mode.  If triplicate backups is set,
   * all triad servers have a copy of each cached item.
   * <p/>
   * Defaults to true.
   */
  private void setTriplicate(boolean isTriplicate)
  {
    if (isTriplicate)
      setFlags(getFlags() | CacheConfig.FLAG_TRIPLICATE);
    else
      setFlags(getFlags() & ~CacheConfig.FLAG_TRIPLICATE);
  }

  /**
   * Sets the {@link AbstractCache.Scope} of this cache.
   */
  public void setScopeMode(AbstractCache.Scope scope)
  {
    _scope = scope;
  }

  /**
   * Returns the {@link AbstractCache.Scope} defined for this cache.
   * @return
   */
  public AbstractCache.Scope getScopeMode()
  {
    return _scope;
  }
  
  public void setEngine(CacheEngine engine)
  {
    if (engine == null)
      throw new NullPointerException();
    
    _engine = engine;
  }
  
  public CacheEngine getEngine()
  {
    return _engine;
  }
  
  public int getServerIndex()
  {
    return getEngine().getServerIndex();
  }

  /**
   * Initializes the CacheConfig.
   */
  public void init()
  {
    if (_keySerializer == null) {
      _keySerializer = new HessianSerializer();
    }

    if (_valueSerializer == null) {
      _valueSerializer = new HessianSerializer();
    }
    
    switch (_scope) {
    case TRANSIENT:
      setTransient(true);
      setTriplicate(false);
      setBackup(false);
      /*
      if (getEngine() == null) {
        setEngine(new AbstractCacheEngine());
      }
      */
      setEngine(new AbstractCacheEngine());
      break;
      
    case LOCAL:
      setTriplicate(false);
      setBackup(false);
      // cloud/6d00
      /*
      if (getEngine() == null) {
        setEngine(new AbstractCacheEngine());
      }
      */
      setEngine(new AbstractCacheEngine());
      break;
      
    case CLUSTER:
      setTriplicate(true);
      setBackup(true);
      break;
      
      /*
    case CLUSTER_SINGLE:
      setTriplicate(false);
      setBackup(false);
      break;
      
    case CLUSTER_BACKUP:
      setTriplicate(false);
      setBackup(true);
      break;
      */
    }
    // _accuracy = CacheStatistics.STATISTICS_ACCURACY_BEST_EFFORT;
    
  }

  public void setExpiry(ExpiryType type, Duration duration)
  {
    TimeUnit unit = duration.getTimeUnit();
    
    long timeout = unit.toMillis(duration.getDurationAmount());
    
    switch (type) {
    case ACCESSED:
      setAccessedExpireTimeout(timeout);
      break;
      
    case MODIFIED:
      setModifiedExpireTimeout(timeout);
      break;
      
    default:
      throw new UnsupportedOperationException(String.valueOf(type));
    }
  }

  /*
  @Override
  public Duration getExpiry(ExpiryType type)
  {
    long timeout;
    
    switch (type) {
    case ACCESSED:
      timeout = getAccessedExpireTimeout();
      break;
      
    case MODIFIED:
      timeout = getModifiedExpireTimeout();
      break;
      
    default:
      throw new UnsupportedOperationException(String.valueOf(type));
    }

    return new Duration(TimeUnit.MILLISECONDS, timeout);
  }
  */

  @Override
  public boolean isStatisticsEnabled()
  {
    return _isStatisticsEnabled;
  }

  public void setStatisticsEnabled(boolean isEnabled)
  {
    _isStatisticsEnabled = isEnabled;
  }

  @Override
  public boolean isStoreByValue()
  {
    return _isStoreByValue;
  }
  
  public void setStoreByValue(boolean isStoreByValue)
  {
    _isStoreByValue = isStoreByValue;
  }

  @Override
  public boolean isTransactionsEnabled()
  {
    return _isTransactionEnabled;
  }

  @Override
  public IsolationLevel getTransactionIsolationLevel()
  {
    return IsolationLevel.NONE;
  }

  @Override
  public Mode getTransactionMode()
  {
    return Mode.NONE;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  /* (non-Javadoc)
   * @see javax.cache.Configuration#getCacheEntryListenerRegistrations()
   */
  @Override
  public Iterable getCacheEntryListenerRegistrations()
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ExpiryPolicy getExpiryPolicy()
  {
    return _expiryPolicy ;
  }
}
