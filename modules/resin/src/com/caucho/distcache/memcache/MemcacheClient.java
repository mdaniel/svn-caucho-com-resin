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

package com.caucho.distcache.memcache;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.cache.Cache;
import javax.cache.CacheConfiguration;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.CacheStatistics;
import javax.cache.Status;
import javax.cache.event.CacheEntryListener;
import javax.cache.event.NotificationScope;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.network.balance.ClientSocket;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.WriteStream;
import com.caucho.vfs.TempStream;

/**
 * Custom serialization for the cache
 */
public class MemcacheClient implements Cache
{
  private static final Logger log
    = Logger.getLogger(MemcacheClient.class.getName());
  
  private ClientSocketFactory _factory;
  
  private CharBuffer _cb = new CharBuffer();
  private Hessian2Input _hIn = new Hessian2Input();
  
  public void addServer(String address, int port)
  {
    _factory = new ClientSocketFactory(address, port);
    _factory.setLoadBalanceIdleTime(30000);
    _factory.setLoadBalanceRecoverTime(0);
    _factory.init();
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#containsKey(java.lang.Object)
   */
  @Override
  public boolean containsKey(Object key) throws CacheException
  {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Object get(Object key) throws CacheException
  {
    ClientSocket client = _factory.open();
    
    if (client == null)
      throw new CacheException("Cannot open client");

    boolean isValid = false;
    long idleStartTime = Alarm.getCurrentTime();
    
    try {
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      out.print("get ");
      out.print(key);
      out.print("\r\n");
      out.flush();
      
      // ts.writeToStream(out);
      readString(is, _cb);
      
      if (! _cb.matches("VALUE")) {
        System.out.println("V: " + _cb);
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
        return false;
      
      if (! skipToEndOfLine(is))
        return false;
      
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
  
  private void readString(ReadStream is, CharBuffer cb)
    throws IOException
  {
    _cb.clear();
    
    int ch;
  
    while((ch = is.read()) >= 0 && ch == ' ') {
    }

    for (; ch >= 0 && ! Character.isWhitespace(ch); ch = is.read()) {
      cb.append((char) ch);
    }
  
    if (ch >= 0)
      is.unread();
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
   * @see javax.cache.Cache#getAll(java.util.Collection)
   */
  @Override
  public Map getAll(Collection keys) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
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
   * @see javax.cache.Cache#getCacheManager()
   */
  @Override
  public CacheManager getCacheManager()
  {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#getCacheStatistics()
   */
  @Override
  public CacheStatistics getCacheStatistics()
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
  public Future loadAll(Collection keys) throws CacheException
  {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void put(Object key, Object value) throws CacheException
  {
    ClientSocket client = null;
    long idleStartTime = Alarm.getCurrentTime();
    
    
    boolean isValid = false;
    
    try {
      client = _factory.open();
      
      if (client == null)
        throw new CacheException("Cannot put memcache");
      
      WriteStream out = client.getOutputStream();
      ReadStream is = client.getInputStream();
      
      //TempStream ts = serialize(value);
      // long length = ts.getLength();
      long length = ((String) value).length();
      
      out.print("set ");
      out.print(key);
      long flags = 0;
      out.print(" ");
      out.print(flags);
      out.print(" ");
      long expTime = 0;
      out.print(expTime);
      out.print(" ");
      // out.print(ts.getLength());
      out.print(length);
      out.print("\r\n");
      
      out.print(value);
      // ts.writeToStream(out);
      
      System.out.println("SET-LEN: " + length);
      
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
   * @see javax.cache.Cache#registerCacheEntryListener(javax.cache.event.CacheEntryListener, javax.cache.event.NotificationScope, boolean)
   */
  @Override
  public boolean registerCacheEntryListener(CacheEntryListener listener,
                                            NotificationScope scope,
                                            boolean synchronous)
  {
    // TODO Auto-generated method stub
    return false;
  }

  /* (non-Javadoc)
   * @see javax.cache.Cache#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object key) throws CacheException
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
      
      if (length < sublen)
        sublen = length;
      
      sublen = _is.read(buffer, offset, sublen);
      
      if (sublen >= 0)
        _length -= sublen;
      
      return sublen;
    }
  }
}
