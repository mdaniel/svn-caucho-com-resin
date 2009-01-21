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

package com.caucho.server.distcache;

import com.caucho.cluster.ExtCacheEntry;
import com.caucho.cluster.CacheSerializer;
import com.caucho.config.ConfigException;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.cluster.Server;
import com.caucho.server.cluster.ClusterTriad;
import com.caucho.server.resin.Resin;
import com.caucho.util.Alarm;
import com.caucho.util.LruCache;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.logging.*;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Manages the distributed cache
 */
public class FileCacheManager extends DistributedCacheManager
{
  private static final Logger log
    = Logger.getLogger(FileCacheManager.class.getName());
  
  private static final L10N L = new L10N(FileCacheManager.class);
  
  private TempFileManager _tempFileManager;

  private CacheMapBacking _cacheMapBacking;
  private DataBacking _dataBacking;

  private final LruCache<HashKey,CacheKeyEntry> _entryCache
    = new LruCache<HashKey,CacheKeyEntry>(64 * 1024);
  
  public FileCacheManager(Server server)
  {
    super(server);

    try {
      _tempFileManager = server.getTempFileManager();

      Path adminPath = server.getResinDataDirectory();
      String serverId = server.getServerId();

      _cacheMapBacking = new CacheMapBacking(adminPath, serverId);
      _dataBacking = new DataBacking(serverId, _cacheMapBacking);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the key entry.
   */
  @Override
  public CacheKeyEntry getKey(Object key, CacheConfig config)
  {
    HashKey hashKey = createHashKey(key, config);

    CacheKeyEntry entry = _entryCache.get(hashKey);

    while (entry == null) {
      ClusterTriad.Owner owner = ClusterTriad.Owner.A_B;
      
      entry = new FileCacheKeyEntry(key, hashKey, owner, this);

      entry = _entryCache.putIfNew(hashKey, entry);
    }

    return entry;
  }

  /**
   * Returns the key entry.
   */
  public CacheKeyEntry getKey(HashKey hashKey)
  {
    CacheKeyEntry entry = _entryCache.get(hashKey);

    while (entry == null) {
      ClusterTriad.Owner owner = ClusterTriad.Owner.A_B;
      
      entry = new FileCacheKeyEntry(null, hashKey, owner, this);

      if (! _entryCache.compareAndPut(null, hashKey, entry))
	entry = _entryCache.get(hashKey);
    }

    return entry;
  }

  /**
   * Gets a cache entry
   */
  public ExtCacheEntry getEntry(HashKey key, CacheConfig config)
  {
    CacheKeyEntry keyEntry = getLocalEntry(key);
    CacheMapEntry entry = keyEntry.getEntry();

    if (entry == null) {
      entry = _cacheMapBacking.load(key);

      keyEntry.compareAndSet(null, entry);

      entry = keyEntry.getEntry();
    }

    return entry;
  }

  /**
   * Gets a cache entry
   */
  public CacheMapEntry loadLocalEntry(CacheKeyEntry keyEntry)
  {
    HashKey key = keyEntry.getKeyHash();
    CacheMapEntry entry = keyEntry.getEntry();

    if (entry == null) {
      entry = _cacheMapBacking.load(key);

      keyEntry.compareAndSet(null, entry);

      entry = keyEntry.getEntry();
    }

    return entry;
  }

  /**
   * Gets a cache entry
   */
  public Object get(FileCacheKeyEntry keyEntry, CacheConfig config)
  {
    HashKey key = keyEntry.getKeyHash();
    
    CacheMapEntry entry = loadLocalEntry(keyEntry);

    if (entry == null)
      return null;
    
    Object value = entry.getValue();

    if (value != null)
      return value;

    HashKey valueHash = entry.getValueHashKey();

    if (valueHash == null)
      return null;

    value = readData(valueHash, config.getValueSerializer());

    // use the old value if it's been overwritten
    if (entry.getValue() == null)
      entry.setObjectValue(value);

    return value;
  }

  /**
   * Gets a cache entry
   */
  /*
  public Object peek(HashKey key, CacheConfig config)
  {
    return get(key, config);
  }
  */

  /**
   * Gets a cache entry
   */
  public boolean get(FileCacheKeyEntry keyEntry,
		     OutputStream os,
		     CacheConfig config)
  {
    return false;
  }

  /**
   * Sets a cache entry
   */
  public void put(HashKey key, Object value, CacheConfig config)
  {
    CacheKeyEntry keyEntry = getKey(key);
  }

  public CacheMapEntry put(FileCacheKeyEntry keyEntry,
			   Object value,
			   CacheConfig config)
  {
    HashKey key = keyEntry.getKeyHash();
    CacheMapEntry oldEntry = loadLocalEntry(keyEntry);

    HashKey oldValueHash
      = oldEntry != null ? oldEntry.getValueHashKey() : null;
    
    HashKey valueHash = writeData(oldValueHash, value,
				  config.getValueSerializer());

    if (valueHash.equals(oldValueHash))
      return oldEntry;

    long version = oldEntry != null ? oldEntry.getVersion() + 1 : 1;
    int flags = config.getFlags();
    
    long expireTimeout = config.getExpireTimeout();
    long idleTimeout = (oldEntry != null
			? oldEntry.getIdleTimeout()
			: config.getIdleTimeout());
    long localReadTimeout = config.getLocalReadTimeout();
    long leaseTimeout = config.getLeaseTimeout();
    
    long accessTime = Alarm.getExactTime();
    long updateTime = Alarm.getExactTime();

    CacheMapEntry entry = new CacheMapEntry(valueHash, value,
					    flags, version,
					    expireTimeout,
					    idleTimeout,
					    leaseTimeout,
					    localReadTimeout,
					    accessTime, updateTime,
					    true);

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! keyEntry.compareAndSet(oldEntry, entry)) {
      log.fine(this + " entry update failed due to timing conflict"
	       + " (key=" + key + ")");
      
      return keyEntry.getEntry();
    }

    if (oldEntry == null) {
      if (_cacheMapBacking.insert(key, valueHash,
				  flags, version,
				  expireTimeout,
				  idleTimeout,
				  leaseTimeout,
				  localReadTimeout)) {
      }
      else {
	log.fine(this + " db insert failed due to timing conflict"
		 + "(key=" + key + ")");
      }
    }
    else {
      if (_cacheMapBacking.updateSave(key, valueHash, version, idleTimeout)) {
      }
      else {
	log.fine(this + " db update failed due to timing conflict"
		 + "(key=" + key + ")");
      }
    }

    return keyEntry.getEntry();
  }

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(FileCacheKeyEntry keyEntry,
			   InputStream is,
			   CacheConfig config,
			   long idleTimeout)
    throws IOException
  {
    return null;
  }

  /**
   * Sets a cache entry
   */
  public boolean remove(HashKey key)
  {
    CacheKeyEntry keyEntry = getKey(key);

    return false;
  }
  
  /**
   * Sets a cache entry
   */
  public boolean remove(FileCacheKeyEntry keyEntry, CacheConfig config)
  {
    HashKey key = keyEntry.getKeyHash();
    CacheMapEntry oldEntry = loadLocalEntry(keyEntry);

    HashKey oldValueHash
      = oldEntry != null ? oldEntry.getValueHashKey() : null;

    long version = oldEntry != null ? oldEntry.getVersion() + 1 : 1;
    int flags = oldEntry != null ? oldEntry.getFlags() : 0;
    
    long expireTimeout = oldEntry != null ? oldEntry.getExpireTimeout() : 0;
    long idleTimeout = oldEntry != null ? oldEntry.getIdleTimeout() : 0;
    long leaseTimeout = (oldEntry != null
			     ? oldEntry.getLeaseTimeout()
			     : 0);
    long localReadTimeout = (oldEntry != null
			     ? oldEntry.getLocalReadTimeout()
			     : 0);
    
    long accessTime = Alarm.getCurrentTime();
    long updateTime = accessTime;
    
    long localExpireTime = accessTime + 100L;
    CacheMapEntry entry = new CacheMapEntry(null, null,
					    flags, version,
					    expireTimeout,
					    idleTimeout,
					    leaseTimeout,
					    localReadTimeout,
					    accessTime,
					    updateTime,
					    true);

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (! keyEntry.compareAndSet(oldEntry, entry)) {
      log.fine(this + " entry remove failed due to timing conflict"
	       + " (key=" + key + ")");
      
      return oldValueHash != null;
    }

    if (oldEntry == null) {
      log.fine(this + " db remove failed due to timing conflict"
	       + "(key=" + key + ")");
    }
    else {
      long timeout = 60000;
      if (_cacheMapBacking.updateSave(key, null, timeout, version)) {
      }
      else {
	log.fine(this + " db remove failed due to timing conflict"
		 + "(key=" + key + ")");
      }
    }

    return oldValueHash != null;
  }
  
  CacheKeyEntry getLocalEntry(HashKey key)
  {
    CacheKeyEntry keyEntry = getKey(key);

    CacheMapEntry valueEntry = keyEntry.getEntry();
    
    if (valueEntry != null) {
      long idleTimeout = valueEntry.getIdleTimeout();
      long updateTime = valueEntry.getLastUpdateTime();

      long now = Alarm.getCurrentTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
	  && updateTime + valueEntry.getIdleWindow() < now) {
	/* XXX:
	CacheMapEntry newEntry
	  = new CacheMapEntry(valueEntry, idleTimeout, now);

	saveUpdateTime(keyEntry, newEntry);
	*/
      }
    }
    
    return keyEntry;
  }

  protected HashKey writeData(HashKey oldValueHash,
			      Object value,
			      CacheSerializer serializer)
  {
    TempOutputStream os = null;

    try {
      os = new TempOutputStream();

      MessageDigestOutputStream mOut = new MessageDigestOutputStream(os);
      DeflaterOutputStream gzOut = new DeflaterOutputStream(mOut);

      serializer.serialize(value, gzOut);

      gzOut.finish();
      mOut.close();
      
      byte []hash = mOut.getDigest();

      HashKey valueHash = new HashKey(hash);

      if (valueHash.equals(oldValueHash))
	return valueHash;

      int length = os.getLength();
      
      StreamSource source = new StreamSource(os);
      if (! _dataBacking.save(valueHash, source, length))
	throw new RuntimeException(L.l("Can't save the data '{0}'",
				       valueHash));

      return valueHash;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      if (os != null)
	os.close();
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
      if (os != null)
	os.close();
    }
  }
}
