/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.server.httpcache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.servlet.FilterChain;

import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.types.Bytes;
import com.caucho.db.Database;
import com.caucho.db.block.BlockManager;
import com.caucho.db.block.BlockStore;
import com.caucho.loader.CloseListener;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.CacheItem;
import com.caucho.server.cluster.ServletService;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.resin.Resin;
import com.caucho.server.webapp.CauchoFilterChain;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.MemoryPath;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Cached response.
 */
public class ProxyCache extends AbstractProxyCache
{
  private static final L10N L = new L10N(ProxyCache.class);
  private static final Logger log
    = Logger.getLogger(ProxyCache.class.getName());

  private static EnvironmentLocal<ProxyCache> _cacheLocal =
    new EnvironmentLocal<ProxyCache>("caucho.server.cache");

  private LruCache<ProxyCacheFilterChain,ProxyCacheFilterChain> _cacheLruEntries;

  private Path _path;
  private Database _database;
  private BlockStore _store;
  private boolean _isEnabled = true;
  private boolean _enableRange = true;
  private boolean _isRewriteVaryAsPrivate = false;
  private Boolean _isEnableMmap;
  private long _memorySize = 16 * 1024 * 1024;
  private long _diskSize = 1024L * 1024 * 1024;

  private int _maxEntrySize = 1 * 1024 * 1024;
  private int _entries = 8192;


  private Resin _resin;
  @SuppressWarnings("unused")
  private ProxyCacheAdmin _admin;

  private AtomicLong _hitCount = new AtomicLong();
  private AtomicLong _missCount = new AtomicLong();

  public ProxyCache()
    throws ConfigException
  {
    _resin = Resin.getCurrent();
    _cacheLocal.set(this);

    _admin = new ProxyCacheAdmin(this);

    Environment.addClassLoaderListener(new CloseListener(this));
  }

  /**
   * Returns the local cache.
   */
  public static ProxyCache getLocalCache()
  {
    return _cacheLocal.get();
  }

  /**
   * Sets the path to the cache directory.
   */
  @Override
  public void setPath(Path path)
  {
    _path = path;
  }

  /**
   * Returns the path from the cache directory.
   */
  @Override
  public Path getPath()
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
  public boolean isEnable()
  {
    return _isEnabled;
  }

  /**
   * Sets the max number of entries.
   */
  public void setEntries(int entries)
  {
    _entries = entries;
  }

  /**
   * Sets the path to the cache directory (backwards compatibility).
   */
  public void setDir(Path path)
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
  @PostConstruct
  public void init()
    throws ConfigException, IOException, java.sql.SQLException
  {
    if (! _isEnabled || _entries <= 0)
      return;

    if (_path == null) {
      _path = Resin.getCurrent().getResinDataDirectory();

      // special cased for testing
      if (_path instanceof MemoryPath) {
        String userName = System.getProperty("user.name");

        _path = Vfs.lookup("file:/tmp/" + userName + "/qa/cache");
      }
    }

    try {
      _path.mkdirs();
    } catch (Exception e) {
    }

    if (! _path.isDirectory())
      throw new ConfigException(L.l("Cache path '{0}' must be a directory.",
                                    _path));

    String server = (String) Environment.getAttribute("caucho.server-id");

    if (server == null || server.equals(""))
      server = "cache";

    Path path = _path.lookup(server + ".cache");

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
      = new LruCache<ProxyCacheFilterChain,ProxyCacheFilterChain>(_entries);

    _database = new Database();
    _database.ensureMemoryCapacity(_memorySize);
    _database.init();
    
    BlockManager blockManager = BlockManager.getBlockManager();
    
    if (_isEnableMmap != null) {
      blockManager.setEnableMmap(_isEnableMmap);
    }

    _store = new BlockStore(_database, "proxy-cache", null, path);
    _store.setFlushDirtyBlocksOnCommit(false);
    _store.create();

    long memorySize = blockManager.getBlockCacheMemoryCapacity();
    
    log.info("Proxy Cache disk-size=" + (_diskSize / (1024 * 1024)) + "M" +
             " memory-size=" + (memorySize / (1024 * 1024)) + "M");
  }

  /**
   * Creates the filter.
   */
  @Override
  public FilterChain createFilterChain(FilterChain next, WebApp app)
  {
    if (isEnable())
      return new ProxyCacheFilterChain(this, next, app);
    else
      return next;
  }

  /**
   * Mark the cache filter chain as active
   */
  void activateEntry(ProxyCacheFilterChain chain)
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
  boolean useEntry(ProxyCacheFilterChain chain)
  {
    return _cacheLruEntries.get(chain) != null;
  }

  /**
   * Clears the cache.
   */
  @Override
  public void clear()
  {
    LruCache<ProxyCacheFilterChain,ProxyCacheFilterChain> cache;
    
    cache = _cacheLruEntries;
    
    if (cache != null) {
      _cacheLruEntries
        = new LruCache<ProxyCacheFilterChain,ProxyCacheFilterChain>(_entries);
    
      cache.clear();
    }
  }

  /**
   * Clears the cache by a pattern.
   */
  void clearCacheByPattern(Pattern hostRegexp, Pattern urlRegexp)
  {
    ArrayList<ProxyCacheFilterChain> clearEntries
      = new ArrayList<ProxyCacheFilterChain>();

    synchronized (_cacheLruEntries) {
      Iterator<ProxyCacheFilterChain> iter = _cacheLruEntries.keys();
      while (iter.hasNext()) {
        ProxyCacheFilterChain entry = iter.next();

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
      Iterator<ProxyCacheFilterChain> iter = _cacheLruEntries.keys();
      while (iter.hasNext()) {
        ProxyCacheFilterChain entry = iter.next();

        entry.clearExpires();
      }
    }
  }

  /**
   * Creates a new writing inode.
   */
  public ProxyCacheInode createInode()
  {
    return new ProxyCacheInode(_store);
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
    return BlockManager.create().getHitCountTotal();
  }

  /**
   * Returns the miss count.
   */
  public long getMemoryBlockMissCount()
  {
    return BlockManager.create().getMissCountTotal();
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

  ServletService getServer()
  {
    return _resin.getServer();
  }

  private ArrayList<CacheItem> getItems()
  {
    ServletService server = _resin.getServer();

    ArrayList<Invocation> invocations
      = server.getInvocationServer().getInvocations();

    HashMap<String,CacheItem> itemMap = new HashMap<String,CacheItem>();

    for (int i = 0; i < invocations.size(); i++) {
      Invocation inv = invocations.get(i);

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

      ProxyCacheFilterChain cacheChain = findCacheChain(inv.getFilterChain());
      
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

  private ProxyCacheFilterChain findCacheChain(FilterChain chain)
  {
    for (;
         chain instanceof CauchoFilterChain;
         chain = ((CauchoFilterChain) chain).getNext()) {
      if (chain instanceof ProxyCacheFilterChain)
        return (ProxyCacheFilterChain) chain;
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
  public void destroy()
  {
    BlockStore store = _store;
    _store = null;

    Database database = _database;
    _database = null;

    if (store != null)
      store.close();

    if (database != null)
      database.close();
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
