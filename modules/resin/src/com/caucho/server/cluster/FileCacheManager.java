/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.server.cluster;

import com.caucho.cache.CacheEntry;
import com.caucho.cache.CacheSerializer;
import com.caucho.config.ConfigException;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.resin.Resin;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Manages the distributed cache
 */
public class FileCacheManager extends DistributedCacheManager
{
  private static final L10N L = new L10N(FileCacheManager.class);
  
  private TempFileManager _tempFileManager;

  private CacheMapBacking _cacheMapBacking;
  private DataBacking _dataBacking;

  private final LruCache<HashKey,Entry> _entryCache
    = new LruCache<HashKey,Entry>(8 * 1024);
  
  FileCacheManager(Server server)
  {
    super(server.getCluster());

    try {
      _tempFileManager = Resin.getCurrent().getTempFileManager();

      Resin resin = server.getResin();
      Path adminPath = resin.getManagement().getPath();
      String serverId = server.getServerId();

      _dataBacking = new DataBacking(adminPath, serverId);

      _cacheMapBacking = new CacheMapBacking(adminPath, serverId);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Gets a cache entry
   */
  public Object get(HashKey key, CacheSerializer serializer)
  {
    Entry entry = _entryCache.get(key);

    if (entry == null) {
      HashKey valueHash = _cacheMapBacking.load(key);

      entry = new Entry();
      entry.setValueHash(valueHash);

      entry = _entryCache.putIfNew(key, entry);
    }
    
    Object value = entry.getValue();

    if (value != null)
      return value;

    HashKey valueHash;

    do {
      valueHash = entry.getValueHash();

      if (valueHash == null)
	return null;

      value = readData(valueHash, serializer);
    } while (! entry.compareAndSetValue(valueHash, value));

    return value;
  }

  /**
   * Sets a cache entry
   */
  public void put(HashKey key, Object value, CacheSerializer serializer)
  {
    long timeout = 60000L;
    
    Entry entry = _entryCache.get(key);

    HashKey oldValueHash = entry != null ? entry.getValueHash() : null;
    
    HashKey valueHash = writeData(oldValueHash, value, serializer);

    boolean isNew = entry == null || entry.getValueHash() == null;

    if (entry == null) {
      Entry newEntry = new Entry();
      newEntry.setValueHash(valueHash);
      newEntry.setValue(value);

      entry = _entryCache.putIfNew(key, newEntry);

      if (entry != newEntry && entry.getValueHash() != null)
	isNew = true;
    }

    entry.setValueHash(valueHash);
    entry.setValue(value);

    if (! isNew || ! _cacheMapBacking.insert(key, valueHash, timeout)) {
      if (! _cacheMapBacking.updateSave(key, valueHash, timeout)) {
	_cacheMapBacking.insert(key, valueHash, timeout);
      }
    }

    // XXX: the use isn't handled properly because matching data
    // doesn't get and increment and we need to decrement the old use

    _dataBacking.incrementUse(valueHash);
  }

  protected HashKey writeData(HashKey oldValueHash,
			      Object value,
			      CacheSerializer serializer)
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      MessageDigest digest = MessageDigest.getInstance("SHA");
      DigestOutputStream mOut = new DigestOutputStream(os, digest);
      DeflaterOutputStream gzOut = new DeflaterOutputStream(mOut);

      serializer.serialize(value, gzOut);

      gzOut.finish();
      mOut.close();
      
      byte []hash = digest.digest();

      HashKey valueHash = new HashKey(hash);

      if (valueHash.equals(oldValueHash))
	return valueHash;

      int length = os.getLength();

      if (! _dataBacking.save(valueHash, os.openInputStream(), length))
	throw new RuntimeException(L.l("Can't save the data '{0}'",
				       valueHash));

      return valueHash;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
	if (os != null)
	  os.close();
      } catch (IOException e) {
      }
    }
  }

  protected Object readData(HashKey valueKey, CacheSerializer serializer)
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      WriteStream out = Vfs.openWrite(os);

      if (! _dataBacking.load(valueKey, out)) {
	out.close();
	// XXX: error?  since we have the value key, it should exist
	return null;
      }

      out.close();

      InputStream is = os.openInputStream();

      try {
	InflaterInputStream gzIn = new InflaterInputStream(is);

	Object value = serializer.deserialize(gzIn);

	gzIn.close();

	return value;
      } finally {
	is.close();
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      try {
	if (os != null)
	  os.close();
      } catch (IOException e) {
      }
    }
  }

  static class Entry {
    HashKey _valueHash;
    SoftReference _valueRef;

    public HashKey getValueHash()
    {
      return _valueHash;
    }
    
    public void setValueHash(HashKey valueHash)
    {
      _valueHash = valueHash;
    }

    public void setValue(Object value)
    {
      _valueRef = new SoftReference(value);
    }

    public boolean compareAndSetValue(HashKey valueHash, Object value)
    {
      if (valueHash.equals(_valueHash)) {
	_valueRef = new SoftReference(value);
	return true;
      }
      else
	return false;
    }

    public Object getValue()
    {
      SoftReference valueRef = _valueRef;

      if (valueRef != null)
	return valueRef.get();
      else
	return null;
    }
  }
}
