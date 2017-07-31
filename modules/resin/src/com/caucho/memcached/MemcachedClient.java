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

package com.caucho.memcached;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.cache.Cache;
import com.caucho.cache.CacheException;
import com.caucho.cache.CacheMXBean;
import com.caucho.cache.CacheManager;
import com.caucho.cache.CacheStatistics;
import com.caucho.cache.Configuration;
import com.caucho.cache.Status;
import com.caucho.cache.event.CacheEntryEventFilter;
import com.caucho.cache.event.CacheEntryListener;
import com.caucho.cloud.loadbalance.LoadBalanceBuilder;
import com.caucho.cloud.loadbalance.LoadBalanceManager;
import com.caucho.cloud.loadbalance.LoadBalanceService;
import com.caucho.cloud.loadbalance.StickyRequestHashGenerator;
import com.caucho.config.Configurable;
import com.caucho.config.types.Period;
import com.caucho.distcache.LocalCache;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.network.balance.ClientSocket;
import com.caucho.server.distcache.CacheConfig;
import com.caucho.server.distcache.CacheImpl;
import com.caucho.server.distcache.DistCacheEntry;
import com.caucho.server.distcache.DistCacheSystem;
import com.caucho.server.distcache.MnodeUpdate;
import com.caucho.util.CharBuffer;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.TempStream;
import com.caucho.vfs.WriteStream;

/**
 * Custom serialization for the cache
 */
public class MemcachedClient implements Cache
{
  private static final L10N L = new L10N(MemcachedClient.class);
  private static final Logger log
    = Logger.getLogger(MemcachedClient.class.getName());
  
  private LoadBalanceBuilder _loadBalanceBuilder;
  private LoadBalanceManager _loadBalancer;
  
  private String _name;
  
  private CharBuffer _cb = new CharBuffer();
  private Hessian2Input _hIn = new Hessian2Input();
  
  private Boolean _isResin;
  
  private long _modifiedExpireTimeout = 3600 * 1000L;
  
  private AtomicReference<CacheImpl> _localCache
    = new AtomicReference<CacheImpl>();
  
  private MemcachedCacheEngine _cacheEngine;
  
  public MemcachedClient()
  {
    _name = "default";
    
    DistCacheSystem cacheSystem = DistCacheSystem.getCurrent();
    
    if (cacheSystem != null) {
      _cacheEngine = new MemcachedCacheEngine(cacheSystem.getDistCacheManager(),
                                             this);
    }
    
    LoadBalanceService loadBalanceService = LoadBalanceService.getCurrent();
    
    if (loadBalanceService == null)
      throw new IllegalStateException(L.l("'{0}' requires an active {1}",
                                          this, 
                                          LoadBalanceService.class.getSimpleName()));
    
    _loadBalanceBuilder = loadBalanceService.createBuilder();
    
    _loadBalanceBuilder.setStickyRequestHashGenerator(new StickyGenerator());
    _loadBalanceBuilder.setMeterCategory("Resin|WebSocket");
    _loadBalanceBuilder.setIdleTime(120 * 1000);
    // _loadBalancer = builquerder.create();
  }
  
  public MemcachedClient(String name)
  {
    this();
    
    _name = name;
  }
  
  public void addServer(String address, int port)
  {
    _loadBalanceBuilder.addAddress(address + ":" + port);
  }
  
  public void addAddress(String address)
  {
    _loadBalanceBuilder.addAddress(address);
  }
  
  public void setCluster(String cluster)
  {
    _loadBalanceBuilder.setTargetCluster(cluster);
  }
  
  public void setPort(int port)
  {
    _loadBalanceBuilder.setTargetPort(port);
  }
  
  @Configurable
  public void setModifiedExpireTimeout(Period timeout)
  {
    _modifiedExpireTimeout = timeout.getPeriod();
  }

  @Override
  public boolean containsKey(Object key) throws CacheException
  {
    return get(key) != null;
  }

  @Override
  public Object get(Object key) throws CacheException
  {
    if (_isResin == null)
      initResin();
    
    boolean isResin = _isResin != null && _isResin;
    
    if (! isResin)
      return getImpl(String.valueOf(key));
    
    CacheImpl cache = getLocalCache();
    
    Object value = cache.get(key);
    
    if (value != null) {
      return value;
    }
    
    value = getImpl(String.valueOf(key));
    
    cache.put(key, value);
    
    return value;
  }
    
  Object getImpl(String key) 
    throws CacheException
  {
    ClientSocket client = _loadBalancer.openSticky(null, key, null);
    
    if (client == null)
      throw new CacheException("Cannot open client");

    boolean isValid = false;
    long idleStartTime = CurrentTime.getCurrentTime();
    
    try {
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      out.print("get ");
      out.print(key);
      out.print("\r\n");
      out.flush();

      // ts.writeToStream(out);
      readString(is, _cb);
      
      if (_cb.matches("END")) {
        if (skipToEndOfLine(is))
          isValid = true;
        
        return null;
      }
      
      if (! _cb.matches("VALUE")) {
        System.out.println("Expected value: " + _cb);
        return null;
      }
      
      readString(is, _cb);

      long flags = readInt(is);
      long length = readInt(is);
      long hash = readInt(is);
      
      if (! skipToEndOfLine(is)) {
        System.out.println("EOLF:");
        return null;
      }
      
      GetInputStream gis = new GetInputStream(is, length);
      
      Hessian2Input hIn = _hIn;
      hIn.init(gis);
      
      Object value = hIn.readObject();

      /*
      _cb.clear();
      is.readAll(_cb, (int) length);
      Object value = _cb.toString();
      */
      
      skipToEndOfLine(is);
      
      readString(is, _cb);
      
      if (! _cb.matches("END"))
        return null;
      
      if (! skipToEndOfLine(is))
        return null;
      
      isValid = true;
      
      return value;
    } catch (IOException e) {
      e.printStackTrace();
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (isValid) {
        client.free(idleStartTime);
      }
      else
        client.close();
    }

    return null;
  }
  
  MnodeUpdate getResinIfModified(String key, 
                                 long oldValue,
                                 DistCacheEntry entry)
    throws CacheException
  {
    try {
      return resinGetIfModifiedImpl(key, oldValue, entry);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    try {
      return resinGetIfModifiedImpl(key, oldValue, entry);
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    }

    return null;
  }
  
  private MnodeUpdate resinGetIfModifiedImpl(String key,
                                             long oldValueHash,
                                             DistCacheEntry entry)
    throws IOException
  {
    CacheImpl cache = getLocalCache();
  
    long version = entry.getMnodeEntry().getVersion();
  
    long now = CurrentTime.getCurrentTime();
  
    if (version < now)
      version = now;
    else
      version = version + 1;
    
    ClientSocket client = _loadBalancer.openSticky(null, key, null);

    if (client == null)
      throw new CacheException("Cannot open client");

    boolean isValid = false;
    long idleStartTime = CurrentTime.getCurrentTime();
    
    try {
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      out.print("get_if_modified ");
      out.print(key);
      out.print(" ");
      out.print(oldValueHash);
      out.print("\r\n");
      out.flush();
      
      // ts.writeToStream(out);
      if (! readString(is, _cb))
        throw new IOException("unexpected end of file");

      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " get_if_modified " + key
                  + "\n  " + client
                  + "\n  " + _cb);
      }
      
      if (_cb.matches("END")) {
        skipToEndOfLine(is);
        isValid = true;
        
        CacheConfig config = entry.getConfig();

        MnodeUpdate update = new MnodeUpdate(0, 0, version, config);

        return update;
      }
      else if (_cb.matches("NOT_MODIFIED")) {
        if (skipToEndOfLine(is)
            && readString(is, _cb)
            && _cb.matches("END")
            && skipToEndOfLine(is)) {
          isValid = true;
        }

        return null;
      }
      else if (! _cb.matches("VALUE")) {
        System.out.println("VALUE: " + _cb);
        return null;
      }

      readString(is, _cb);

      long flags = readInt(is);
      long length = readInt(is);
      long serverHash = readInt(is);
      
      if (! skipToEndOfLine(is)) {
        System.out.println("EOLF:");
        return null;
      }

      GetInputStream gis = new GetInputStream(is, length);
      
      MnodeUpdate update = null; // cache.saveData(gis, length, version, config);
      /*

      MnodeUpdate update = new MnodeUpdate(valueHash,
                                           valueDataId,
                                           length,
                                           version,
                                           config);
                                           */

      /*
      _cb.clear();
      is.readAll(_cb, (int) length);
      Object value = _cb.toString();
      */
      
      skipToEndOfLine(is);
      
      readString(is, _cb);
      
      if (! _cb.matches("END"))
        return null;
      
      if (! skipToEndOfLine(is))
        return null;
      
      isValid = true;
      
      return update;
    } finally {
      if (isValid) {
        client.free(idleStartTime);
      }
      else
        client.close();
    }
  }

  @Override
  public void put(Object key, Object value) throws CacheException
  {
    if (_isResin == null)
      initResin();
    
    boolean isResin = _isResin != null && _isResin;
    
    if (isResin) {
      CacheImpl cache = getLocalCache();
      
      cache.put(key, value);
    }
    
    putImpl(key, value);
  }
  
  private void putImpl(Object key, Object value) throws CacheException
  {
    ClientSocket client = null;
    long idleStartTime = CurrentTime.getCurrentTime();
    
    
    boolean isValid = false;
    
    try {
      client = _loadBalancer.openSticky(null, key, null);
      
      if (client == null)
        throw new CacheException("Cannot put memcache");
      
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      TempStream ts = serialize(value);
      long length = ts.getLength();
      // long length = ((String) value).length();
      
      out.print("set ");
      out.print(key);
      long flags = 0;
      out.print(" ");
      out.print(flags);
      out.print(" ");
      long expTime = _modifiedExpireTimeout;
      out.print(expTime);
      out.print(" ");
      // out.print(ts.getLength());
      out.print(length);
      out.print("\r\n");
      
      //out.print(value);
      ts.writeToStream(out);
      
      // System.out.println("SET-LEN: " + length);
      
      out.print("\r\n");
      out.flush();
      
      String line = is.readLine();
      
      if (! "STORED".equals(line)) {
        System.out.println("BAD: " + line);
        throw new IllegalStateException("Expected 'STORED' at " + line);
      }
      
      isValid = true;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (client == null) {
      }
      else if (isValid)
        client.free(idleStartTime);
      else
        client.close();
    }
  }
  
  void putResin(String key,
                MnodeUpdate update,
                long valueDataId) throws CacheException
  {
    ClientSocket client = null;
    long idleStartTime = CurrentTime.getCurrentTime();
    
    long valueDataTime = 0;
    
    CacheImpl cache = getLocalCache();
    
    boolean isValid = false;
    
    try {
      client = _loadBalancer.openSticky(null, key, null);
      
      if (client == null)
        throw new CacheException("Cannot put memcache");
      
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      long length = update.getValueLength();
      // long length = ((String) value).length();
      
      out.print("set ");
      out.print(key);
      long flags = update.getUserFlags();
      out.print(" ");
      out.print(flags);
      out.print(" ");
      long expTime = update.getModifiedExpireTimeout();
      out.print(expTime / 1000L);
      out.print(" ");
      // out.print(ts.getLength());
      out.print(length);
      out.print("\r\n");
      
      out.setDisableClose(true);
      
      //out.print(value);
      boolean v = cache.loadData(valueDataId, valueDataTime, out);
      
      out.setDisableClose(false);
      
      // System.out.println("SET-LEN: " + length);
      
      out.print("\r\n");
      out.flush();

      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " resin_set " + key
                  + " " + Long.toHexString(update.getValueHash())
                  + "\n  " + client
                  + "\n  " + _cb);
      }
      
      String line = is.readLine();
      
      if (! "STORED".equals(line)) {
        System.out.println("BAD: " + line);
        throw new IllegalStateException("Expected 'STORED' at " + line);
      }
      
      isValid = true;
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (client == null) {
      }
      else if (isValid)
        client.free(idleStartTime);
      else
        client.close();
    }
  }

  @Override
  public boolean remove(Object key) throws CacheException
  {
    if (_isResin == null)
      initResin();
    
    boolean isResin = _isResin != null && _isResin;
    
    if (isResin) {
      CacheImpl cache = getLocalCache();
      
      cache.remove(key);
    }
    else {
      removeImpl(key);
    }
    
    return true;
  }
  
  void removeImpl(Object key) throws CacheException
  {
    ClientSocket client = null;
    long idleStartTime = CurrentTime.getCurrentTime();
    
    
    boolean isValid = false;
    
    try {
      client = _loadBalancer.openSticky(null, key, null);
      
      if (client == null)
        throw new CacheException("Cannot put memcache");
      
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      out.print("delete ");
      out.print(key);
      long timeout = 0;
      out.print(" ");
      out.print(timeout);
      //out.print(" noreply\r\n");
      out.print("\r\n");
      out.flush();
      
      String line = is.readLine();

      if (log.isLoggable(Level.FINER)) {
        log.finer(this + " delete " + key
                  + "\n  " + client
                  + "\n  " + line);
      }
      
      if (line.equals("DELETED")) {
        isValid = true;
      }
      else if (line.equals("NOT_FOUND")) {
        isValid = true;
      }
      else {
        System.out.println("UKNOWNK :" + line);
      }
    } catch (IOException e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (client == null) {
      }
      else if (isValid)
        client.free(idleStartTime);
      else
        client.close();
    }
  }

  private static long getCasKey(HashKey valueKey)
  {
    if (valueKey == null)
      return 0;
    
    byte []valueHash = valueKey.getHash();
    
    return (((valueHash[0] & 0x7fL) << 56)
        | ((valueHash[1] & 0xffL) << 48)
        | ((valueHash[2] & 0xffL) << 40)
        | ((valueHash[3] & 0xffL) << 32)
        | ((valueHash[4] & 0xffL) << 24)
        | ((valueHash[5] & 0xffL) << 16)
        | ((valueHash[6] & 0xffL) << 8)
        | ((valueHash[7] & 0xffL)));
  }
  
  private CacheImpl getLocalCache()
  {
    CacheImpl cache = _localCache.get();
    
    if (cache == null) {
      LocalCache localCache = new LocalCache();
      localCache.setName("memcache:" + _name);
      localCache.setModifiedExpireTimeoutMillis(_modifiedExpireTimeout);
      localCache.setLocalExpireTimeoutMillis(1000);
      localCache.setEngine(_cacheEngine);
      
      cache = localCache.createIfAbsent();
      
      _localCache.compareAndSet(null, cache);
    }
    
    return _localCache.get();
  }
  
  @PostConstruct
  public void init()
  {
    if (_loadBalancer != null)
      return;
    
    synchronized (this) {
      if (_loadBalancer == null)
        _loadBalancer = _loadBalanceBuilder.create();
    }
  }

  private void initResin()
  {
    if (_isResin != null)
      return;

    init();
    
    DistCacheSystem cacheSystem = DistCacheSystem.getCurrent();
    
    if (cacheSystem == null) {
      _isResin = false;
      return;
    }
    
    ClientSocket client = _loadBalancer.open();
    
    if (client == null)
      return;

    long idleStartTime = CurrentTime.getCurrentTime();
    boolean isValid = false;
    
    try {
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      out.print("stats resin\r\n");
      out.flush();
      
      while (true) {
        String line = is.readLine();
        
        if (line == null)
          break;
        
        line = line.trim();
        
        if (line.equals("END") || line.indexOf("ERROR") >= 0) {
          break;
        }
        
        if (line.equals("STAT enable_get_if_modified 1"))
          _isResin = true;
      }
      
      isValid = true;
      
      if (_isResin == null)
        _isResin = false;
    } catch (IOException e) {
      e.printStackTrace();
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (isValid) {
        client.free(idleStartTime);
      }
      else
        client.close();
    }
  }
  
  private long readInt(ReadStream is)
    throws IOException
  {
    int ch;
    
    long value = 0;
    
    while((ch = is.read()) >= 0 && ch == ' ') {
    }

    for (; '0' <= ch && ch <= '9'; ch = is.read()) {
      value = 10 * value + ch - '0';
    }
    
    is.unread();
    
    return value;
  }
  
  private boolean readString(ReadStream is, CharBuffer cb)
    throws IOException
  {
    _cb.clear();
    
    int ch = is.read();
  
    for (; ch >= 0 && ch == ' '; ch = is.read()) {
    }
    
    if (ch < 0)
      return false;

    for (; ch >= 0 && ! Character.isWhitespace(ch); ch = is.read()) {
      cb.append((char) ch);
    }
  
    if (ch >= 0)
      is.unread();
    
    return true;
  }

  private boolean skipToEndOfLine(ReadStream is)
    throws IOException
  {
    int ch;
    
    while ((ch = is.read()) >= 0 && ch != '\r') {
    }
    
    if (ch == '\r') {
      ch = is.read();
      
      if (ch == '\n')
        return true;
    }
    
    return false;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getAndPut(java.lang.Object, java.lang.Object)
   */
  @Override
  public Object getAndPut(Object key, Object value) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getAndRemove(java.lang.Object)
   */
  @Override
  public Object getAndRemove(Object key) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getAndReplace(java.lang.Object, java.lang.Object)
   */
  @Override
  public Object getAndReplace(Object key, Object value) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getCacheStatistics()
   */
  @Override
  public CacheStatistics getStatistics()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getConfiguration()
   */
  @Override
  public Configuration getConfiguration()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getName()
   */
  @Override
  public String getName()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#load(java.lang.Object)
   */
  @Override
  public Future load(Object key) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#loadAll(java.util.Collection)
   */
  @Override
  public Future loadAll(Set keys) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }
  
  private TempStream serialize(Object value)
    throws IOException
  {
    TempStream ts = new TempStream();
    
    WriteStream out = new WriteStream(ts);
    
    Hessian2Output hOut = new Hessian2Output(out);
    
    hOut.writeObject(value);
    
    hOut.close();
    out.close();
    
    return ts;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#putAll(java.util.Map)
   */
  @Override
  public void putAll(Map map) throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#putIfAbsent(java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean putIfAbsent(Object key, Object value) throws CacheException
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#remove(java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean remove(Object key, Object oldValue) throws CacheException
  {
    // TODO Auto-generated method stub
    return false;
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
   * @see javax.cache.Cache#replace(java.lang.Object, java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean replace(Object key, Object oldValue, Object newValue)
      throws CacheException
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#replace(java.lang.Object, java.lang.Object)
   */
  @Override
  public boolean replace(Object key, Object value) throws CacheException
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#unregisterCacheEntryListener(javax.cache.event.CacheEntryListener)
   */
  @Override
  public boolean unregisterCacheEntryListener(CacheEntryListener listener)
  {
    // TODO Auto-generated method stub
    return false;
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
   * @see javax.cache.CacheLifecycle#getStatus()
   */
  @Override
  public Status getStatus()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.CacheLifecycle#start()
   */
  @Override
  public void start() throws CacheException
  {
    // TODO Auto-generated method stub
    
  }

  @Override
  public CacheMXBean getMBean()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /* (non-Javadoc)
   * @see javax.cache.CacheLifecycle#stop()
   */
  @Override
  public void stop() throws CacheException
  {
    // TODO Auto-generated method stub
    
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
  
  static class GetInputStream extends InputStream {
    private ReadStream _is;
    private long _length;
    
    GetInputStream(ReadStream is, long length)
    {
      _is = is;
      _length = length;
    }

    @Override
    public int read()
      throws IOException
    {
      if (_length <= 0)
        return -1;
      
      int ch = _is.read();
      _length -= 1;
      
      return ch;
    }

    @Override
    public int read(byte []buffer, int offset, int length)
      throws IOException
    {
      int sublen = (int) _length;
      
      if (sublen <= 0)
        return -1;
      
      if (length < sublen)
        sublen = length;
      
      sublen = _is.read(buffer, offset, sublen);
      
      if (sublen >= 0)
        _length -= sublen;
      
      return sublen;
    }
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
  
  static class StickyGenerator implements StickyRequestHashGenerator {
    @Override
    public String getHash(Object requestInfo)
    {
      return (String) requestInfo;
    }
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getAll(java.util.Set)
   */
  @Override
  public Map getAll(Set keys)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#invokeEntryProcessor(java.lang.Object, javax.cache.Cache.EntryProcessor)
   */
  @Override
  public Object invokeEntryProcessor(Object key, EntryProcessor entryProcessor)
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#removeAll(java.util.Set)
   */
  @Override
  public void removeAll(Set keys)
  {
    // TODO Auto-generated method stub
    
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getCacheManager()
   */
  @Override
  public CacheManager getCacheManager()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#registerCacheEntryListener(javax.cache.event.CacheEntryListener, boolean, javax.cache.event.CacheEntryEventFilter, boolean)
   */
  @Override
  public boolean registerCacheEntryListener(CacheEntryListener listener,
                                            boolean requireOldValue,
                                            CacheEntryEventFilter filter,
                                            boolean synchronous)
  {
    // TODO Auto-generated method stub
    return false;
  }
}
