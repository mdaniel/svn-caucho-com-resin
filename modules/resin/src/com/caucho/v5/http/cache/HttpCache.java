/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.servlet.FilterChain;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.env.system.RootDirectorySystem;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.webapp.FilterChainCaucho;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.loader.EnvLoader;
import com.caucho.v5.loader.EnvironmentLocal;
import com.caucho.v5.management.server.CacheItem;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.LruCache;
import com.caucho.v5.vfs.MemoryPath;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;

/**
 * Cached response.
 */
public class HttpCache extends HttpCacheBase
{
  private static final L10N L = new L10N(HttpCache.class);
  private static final Logger log
    = Logger.getLogger(HttpCache.class.getName());

  private static EnvironmentLocal<HttpCache> _cacheLocal =
    new EnvironmentLocal<HttpCache>("caucho.server.cache");
  
  private static final int DEFAULT_ENTRIES = 64 * 1024;

  private LruCache<FilterChainHttpCache,FilterChainHttpCache> _cacheLruEntries;

  private PathImpl _path;
  // private Database _database;
  
  private BlockManagerHttpCache _store;
  private boolean _isEnabled = true;
  private boolean _enableRange = true;
  private boolean _isRewriteVaryAsPrivate = false;
  private Boolean _isEnableMmap;
  private long _memorySize = 16 * 1024 * 1024;
  private long _diskSize = 1024L * 1024 * 1024;

  private int _maxEntrySize = 1 * 1024 * 1024;

  // private ServerBase _server;
  @SuppressWarnings("unused")
  private ProxyCacheAdmin _admin;

  private AtomicLong _hitCount = new AtomicLong();
  private AtomicLong _missCount = new AtomicLong();
  private HttpContainerServlet _httpContainer;

  public HttpCache(HttpContainerServlet httpContainer)
    throws ConfigException
  {
    Objects.requireNonNull(httpContainer);
    
    _httpContainer = httpContainer;
    // _server = ServerBase.getCurrent();
    _cacheLocal.set(this);

    _admin = new ProxyCacheAdmin(this);
  }

  /**
   * Returns the local cache.
   */
  public static HttpCache getLocalCache()
  {
    return _cacheLocal.get();
  }

  /**
   * Sets the path to the cache directory.
   */
  @Override
  public void setPath(PathImpl path)
  {
    _path = path;
  }

  /**
   * Returns the path from the cache directory.
   */
  @Override
  public PathImpl getPath()
  {
    return _path;
  }

  /**
   * Sets the memory size of the cache
   */
  @Override
  public void setMemorySize(Bytes size)
  {
    if (size != null) {
      _memorySize = size.getBytes();
    }
  }

  /**
   * Sets the disk size of the cache
   */
  @Override
  public void setDiskSize(Bytes size)
  {
    if (size != null) {
      _diskSize = size.getBytes();
    }
  }
  
  @Configurable
  @Override
  public void setEnableMmap(boolean isEnable)
  {
    _isEnableMmap = isEnable;
  }

  /**
   * Sets the maximum entry size for cache values.
   */
  public void setMaxEntrySize(Bytes size)
  {
    _maxEntrySize = (int) size.getBytes();
  }

  /**
   * Sets the maximum entry size for cache values.
   */
  @Override
  public int getMaxEntrySize()
  {
    return _maxEntrySize;
  }

  /**
   * Set true if enabled.
   */
  public void setEnable(boolean isEnabled)
  {
    _isEnabled = isEnabled;
  }

  /**
   * Return true if enabled.
   */
  @Override
  public boolean isEnable()
  {
    return _isEnabled;
  }

  /**
   * Sets the max number of entries.
   */
  /*
  public void setEntries(int entries)
  {
    _entries = entries;
  }
  */

  /**
   * Sets the path to the cache directory (backwards compatibility).
   */
  public void setDir(PathImpl path)
  {
    setPath(path);
  }

  /**
   * Sets the size of the the cache (backwards compatibility).
   */
  public void setSize(Bytes size)
  {
    setMemorySize(size);
  }

  /**
   * True if range handling is enabled.
   */
  public void setEnableRange(boolean enableRange)
  {
    _enableRange = enableRange;
  }

  /**
   * True if range handling is enabled.
   */
  public boolean isEnableRange()
  {
    return _enableRange;
  }

  /**
   * True if Vary headers should be rewritten as Cache-Control: private to
   * work around IE's Vary handling bug.
   */
  public void setRewriteVaryAsPrivate(boolean rewrite)
  {
    _isRewriteVaryAsPrivate = rewrite;
  }

  /**
   * True if Vary headers should be rewritten as Cache-Control: private to
   * work around IE's Vary handling bug.
   */
  public boolean isRewriteVaryAsPrivate()
  {
    return _isRewriteVaryAsPrivate;
  }

  /**
   * Initialize the cache.
   */
  // @PostConstruct
  @Override
  public void start()
  {
    int entries = getEntries();
    
    if (! _isEnabled || entries == 0) {
      return;
    }
    
    if (entries < 0) {
      entries = DEFAULT_ENTRIES;
    }

    if (_path == null) {
      _path = RootDirectorySystem.getCurrentDataDirectory();

      // special cased for testing
      if (_path instanceof MemoryPath) {
        String userName = System.getProperty("user.name");

        _path = VfsOld.lookup("file:/tmp/" + userName + "/qa/cache");
      }
    }

    try {
      _path.mkdirs();
    } catch (Exception e) {
    }

    if (! _path.isDirectory())
      throw new ConfigException(L.l("Cache path '{0}' must be a directory.",
                                    _path));

    String server = (String) EnvLoader.getAttribute("caucho.server-id");

    if (server == null || server.equals(""))
      server = "cache";
    else
      server = server.replace(':', '_'); //windows

    PathImpl path = _path.lookup("http-cache.store");

    try {
      if (path.exists())
        path.remove();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    if (path.exists()) {
      throw new ConfigException(L.l("Proxy Cache at '{0}' cannot be removed on startup.  The proxy cache must be removable on Resin start.",
                                path.getNativePath()));
    }

    _cacheLruEntries
      = new LruCache<FilterChainHttpCache,FilterChainHttpCache>(entries);

    _store = new BlockManagerHttpCache(path);
  }

  /**
   * Creates the filter.
   */
  @Override
  public FilterChain createFilterChain(FilterChain next, WebApp app)
  {
    if (isEnable() && _store != null) // XXX: store init issue
      return new FilterChainHttpCache(this, next, app);
    else
      return next;
  }

  /**
   * Mark the cache filter chain as active
   */
  void activateEntry(FilterChainHttpCache chain)
  {
    _cacheLruEntries.putIfNew(chain, chain);

    /*
    // remove overfull entries
    for (int i = 64;
         i > 0
           && _diskSize <= _store.getTotalFragmentSize()
           && _cacheLruEntries.removeLongestTail();
         i--) {
    }
    */
  }

  /**
   * Mark the cache filter chain as active
   */
  boolean useEntry(FilterChainHttpCache chain)
  {
    return _cacheLruEntries.get(chain) != null;
  }

  /**
   * Clears the cache.
   */
  @Override
  public void clear()
  {
    LruCache<FilterChainHttpCache,FilterChainHttpCache> cache;
    
    cache = _cacheLruEntries;
    
    if (cache != null) {
      int entries = getEntries();
      
      if (entries < 0) {
        entries = DEFAULT_ENTRIES;
      }
      
      _cacheLruEntries
        = new LruCache<FilterChainHttpCache,FilterChainHttpCache>(entries);
    
      cache.clear();
    }
  }

  /**
   * Clears the cache by a pattern.
   */
  void clearCacheByPattern(Pattern hostRegexp, Pattern urlRegexp)
  {
    ArrayList<FilterChainHttpCache> clearEntries
      = new ArrayList<FilterChainHttpCache>();

    synchronized (_cacheLruEntries) {
      Iterator<FilterChainHttpCache> iter = _cacheLruEntries.keys();
      while (iter.hasNext()) {
        FilterChainHttpCache entry = iter.next();

        boolean isMatch = true;

        if (hostRegexp != null) {
          WebApp webApp = entry.getWebApp();
          if (! hostRegexp.matcher(webApp.getHostName()).find())
            isMatch = false;
        }

        if (urlRegexp != null) {
          if (! urlRegexp.matcher(entry.getUri()).find())
            isMatch = false;
        }

        if (isMatch)
          clearEntries.add(entry);
      }
    }

    for (int i = 0; i < clearEntries.size(); i++)
      _cacheLruEntries.remove(clearEntries.get(i));
  }

  /**
   * Clears the cache expires
   */
  void clearExpires()
  {
    /*
    ArrayList<ProxyCacheFilterChain> clearEntries
      = new ArrayList<ProxyCacheFilterChain>();
      */

    synchronized (_cacheLruEntries) {
      Iterator<FilterChainHttpCache> iter = _cacheLruEntries.keys();
      while (iter.hasNext()) {
        FilterChainHttpCache entry = iter.next();

        entry.clearExpires();
      }
    }
  }

  /**
   * Creates a new writing inode.
   */
  public InodeHttpCache createInode()
  {
    return new InodeHttpCache(_store);
  }

  /**
   * Returns the hit count.
   */
  @Override
  public long getHitCount()
  {
    return _hitCount.get();
  }

  /**
   * Returns the miss count.
   */
  @Override
  public long getMissCount()
  {
    return _missCount.get();
  }

  /**
   * Returns the block hit count.
   */
  public long getMemoryBlockHitCount()
  {
    // return BlockManager.create().getHitCountTotal();
    
    return 0;
  }

  /**
   * Returns the miss count.
   */
  public long getMemoryBlockMissCount()
  {
    // return BlockManager.create().getMissCountTotal();
    
    return 0;
  }

  /**
   * Return most used connections.
   */
  public CacheItem []getCacheableEntries(int max)
  {
    ArrayList<CacheItem> items = getItems();

    Collections.sort(items, new CacheHitCompare());

    ArrayList<CacheItem> cacheableItems = new ArrayList<CacheItem>();

    int i;
    for (i = 0; i < items.size(); i++) {
      CacheItem item = items.get(i);

      if (item.isCacheable()) {
        cacheableItems.add(item);
      }
    }

    CacheItem []itemArray = new CacheItem[cacheableItems.size()];
    cacheableItems.toArray(itemArray);

    return itemArray;
  }

  /**
   * Return most used connections.
   */
  public CacheItem []getUncacheableEntries(int max)
  {
    ArrayList<CacheItem> items = getItems();

    Collections.sort(items, new CacheMissCompare());

    ArrayList<CacheItem> uncacheableItems = new ArrayList<CacheItem>();

    int i;
    for (i = 0; i < items.size(); i++) {
      CacheItem item = items.get(i);

      if (! item.isCacheable()) {
        uncacheableItems.add(item);
      }
    }

    CacheItem []itemArray = new CacheItem[uncacheableItems.size()];
    uncacheableItems.toArray(itemArray);

    return itemArray;
  }

  /**
   * Return most used connections.
   */
  public CacheItem []getCachedEntries(int max)
  {
    ArrayList<CacheItem> items = getItems();

    Collections.sort(items, new CacheHitCompare());

    ArrayList<CacheItem> cachedItems = new ArrayList<CacheItem>();

    int i;
    for (i = 0; i < items.size(); i++) {
      CacheItem item = items.get(i);

      if (item.isCached()) {
        cachedItems.add(item);
      }
    }

    CacheItem []itemArray = new CacheItem[cachedItems.size()];
    cachedItems.toArray(itemArray);

    return itemArray;
  }

  /**
   * Return most used connections.
   */
  public CacheItem []getUncachedEntries(int max)
  {
    ArrayList<CacheItem> items = getItems();

    Collections.sort(items, new CacheMissCompare());

    ArrayList<CacheItem> uncachedItems = new ArrayList<CacheItem>();

    int i;
    for (i = 0; i < items.size(); i++) {
      CacheItem item = items.get(i);

      if (! item.isCached()) {
        uncachedItems.add(item);
      }
    }

    CacheItem []itemArray = new CacheItem[uncachedItems.size()];
    uncachedItems.toArray(itemArray);

    return itemArray;
  }

  HttpContainerServlet getHttpContainer()
  {
    return _httpContainer;
  }

  private ArrayList<CacheItem> getItems()
  {
    HttpContainerServlet server = getHttpContainer();

    ArrayList<InvocationServlet> invocations
      = server.getInvocationManager().getInvocations();

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocations.size(); i++) {
      InvocationServlet inv = invocations.get(i);

      String url = inv.getURI();

      int p = url.indexOf('?');
      if (p > 0)
        url = url.substring(0, p);

      CacheItem item = itemMap.get(url);

      if (item == null) {
        item = new CacheItem();
        item.setUrl(url);

        itemMap.put(url, item);
      }

      boolean isCacheable = false;
      boolean isCached = false;
      long hitCount = 0;

      FilterChainHttpCache cacheChain = findCacheChain(inv.getFilterChain());
      
      if (cacheChain != null) {
        isCacheable = cacheChain.isCacheable();
        isCached = cacheChain.isCached();
        hitCount = cacheChain.getHitCount();
      }
      
      item.setHitCount(hitCount);
      item.setMissCount(inv.getRequestCount() - hitCount);

      if (isCacheable) {
        item.setCacheable(true);
      }
      
      if (isCached) {
        item.setCached(true);
      }
    }

    return new ArrayList<CacheItem>(itemMap.values());
  }

  private FilterChainHttpCache findCacheChain(FilterChain chain)
  {
    for (;
         chain instanceof FilterChainCaucho;
         chain = ((FilterChainCaucho) chain).getNext()) {
      if (chain instanceof FilterChainHttpCache)
        return (FilterChainHttpCache) chain;
    }

    return null;
  }

  /**
   * Adds a hit.
   */
  final void hit()
  {
    _hitCount.incrementAndGet();
  }

  /**
   * Adds a miss.
   */
  final void miss()
  {
    _missCount.incrementAndGet();
  }

  /**
   * Closes the cache.
   */
  public void close()
  {
    BlockManagerHttpCache store = _store;
    _store = null;
    
    if (store != null) {
      store.close();
    }
  }

  private static class CacheHitCompare implements Comparator<CacheItem>
  {
    public int compare(CacheItem a, CacheItem b)
    {
      long diff = b.getHitCount() - a.getHitCount();

      if (diff < 0)
        return -1;
      else if (diff == 0)
        return 0;
      else
        return 1;
    }
  }

  private static class CacheMissCompare implements Comparator<CacheItem>
  {
    public int compare(CacheItem a, CacheItem b)
    {
      long diff = b.getMissCount() - a.getMissCount();

      if (diff < 0)
        return -1;
      else if (diff == 0)
        return 0;
      else
        return 1;
    }
  }
}
