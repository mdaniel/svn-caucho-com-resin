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

package com.caucho.server.distcache;

import com.caucho.cluster.CacheSerializer;
import com.caucho.cluster.ExtCacheEntry;
//import com.caucho.cluster.AbstractCacheEntry;
import com.caucho.config.ConfigException;
import com.caucho.server.cache.TempFileManager;
import com.caucho.server.cluster.ClusterTriad;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Manages the distributed cache
 */
public class FileCacheManager extends DistributedCacheManager {
  private static final Logger log
    = Logger.getLogger(FileCacheManager.class.getName());

  private static final L10N L = new L10N(FileCacheManager.class);

  private TempFileManager _tempFileManager;

  private CacheMapBacking _cacheMapBacking;
  private DataBacking _dataBacking;

  private final LruCache<HashKey, FileCacheEntry> _entryCache
    = new LruCache<HashKey, FileCacheEntry>(64 * 1024);

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
  public FileCacheEntry getCacheEntry(Object key, CacheConfig config)
  {
    HashKey hashKey = createHashKey(key, config);

    FileCacheEntry cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      ClusterTriad.Owner owner = ClusterTriad.Owner.A_B;

      cacheEntry = new FileCacheEntry(key, hashKey, owner, this);

      cacheEntry = _entryCache.putIfNew(hashKey, cacheEntry);
    }

    return cacheEntry;
  }

  /**
   * Returns the key entry.
   */
  public FileCacheEntry getKey(HashKey hashKey)
  {
    FileCacheEntry cacheEntry = _entryCache.get(hashKey);

    while (cacheEntry == null) {
      ClusterTriad.Owner owner = ClusterTriad.Owner.A_B;

      cacheEntry = new FileCacheEntry(null, hashKey, owner, this);

      if (!_entryCache.compareAndPut(null, hashKey, cacheEntry))
        cacheEntry = _entryCache.get(hashKey);
    }

    return cacheEntry;
  }

  /**
   * Gets a cache entry
   */
  public ExtCacheEntry getEntry(HashKey key, CacheConfig config)
  {
    FileCacheEntry entryKey = getLocalEntry(key);
    CacheEntryValue entryValue = entryKey.getEntryValue();

    if (entryValue == null) {
      entryValue = _cacheMapBacking.load(key);

      entryKey.compareAndSet(null, entryValue);

      entryValue = entryKey.getEntryValue();
    }

    return entryValue;
  }

  /**
   * Gets a cache entry
   */
  public CacheEntryValue loadLocalEntry(FileCacheEntry cacheEntry)
  {
    HashKey key = cacheEntry.getKeyHash();
    CacheEntryValue entryValue = cacheEntry.getEntryValue();

    if (entryValue == null) {
      entryValue = _cacheMapBacking.load(key);

      cacheEntry.compareAndSet(null, entryValue);

      entryValue = cacheEntry.getEntryValue();
    }

    return entryValue;
  }

  /**
   * Gets a cache entry
   */
  public Object get(FileCacheEntry cacheEntry, CacheConfig config)
  {
    HashKey key = cacheEntry.getKeyHash();

    CacheEntryValue entryValue = loadLocalEntry(cacheEntry);

    if (entryValue == null)
      return null;

    Object value = entryValue.getValue();

    if (value != null)
      return value;

    HashKey valueHash = entryValue.getValueHashKey();

    if (valueHash == null)
      return null;

    value = readData(valueHash, config.getValueSerializer());

    // use the old value if it's been overwritten
    if (entryValue.getValue() == null)
      entryValue.setObjectValue(value);

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
  public boolean get(FileCacheEntry entry,
    OutputStream os,
    CacheConfig config)
  {
    return false;
  }

  /**
   * Sets a cache entry
   */
  public CacheEntryValue put(FileCacheEntry entry,
    Object value,
    CacheConfig config)
  {
    HashKey key = entry.getKeyHash();
    CacheEntryValue oldEntryValue = loadLocalEntry(entry);

    HashKey oldValueHash
      = oldEntryValue != null ? oldEntryValue.getValueHashKey() : null;

    HashKey valueHash = writeData(oldValueHash, value,
      config.getValueSerializer());

    if (valueHash.equals(oldValueHash))
      return oldEntryValue;

    long version = oldEntryValue != null ? oldEntryValue.getVersion() + 1 : 1;
    int flags = config.getFlags();

    long expireTimeout = config.getExpireTimeout();
    long idleTimeout = (oldEntryValue != null
      ? oldEntryValue.getIdleTimeout()
      : config.getIdleTimeout());
    long localReadTimeout = config.getLocalReadTimeout();
    long leaseTimeout = config.getLeaseTimeout();

    long accessTime = Alarm.getExactTime();
    long updateTime = Alarm.getExactTime();

    CacheEntryValue entryValue = new CacheEntryValue(valueHash, value,
      flags, version,
      expireTimeout,
      idleTimeout,
      leaseTimeout,
      localReadTimeout,
      accessTime, updateTime,
      true);

    // the failure cases are not errors because this put() could
    // be immediately followed by an overwriting put()

    if (!entry.compareAndSet(oldEntryValue, entryValue)) {
      log.fine(this + " entryValue update failed due to timing conflict"
        + " (key=" + key + ")");

      return entry.getEntryValue();
    }

    if (oldEntryValue == null) {
      if (_cacheMapBacking.insert(key, valueHash,
        flags, version,
        expireTimeout,
        idleTimeout,
        leaseTimeout,
        localReadTimeout)) {
      } else {
        log.fine(this + " db insert failed due to timing conflict"
          + "(key=" + key + ")");
      }
    } else {
      if (_cacheMapBacking.updateSave(key, valueHash, version, idleTimeout)) {
      } else {
        log.fine(this + " db update failed due to timing conflict"
          + "(key=" + key + ")");
      }
    }

    return entry.getEntryValue();
  }

  /**
   * Sets a cache entry
   */
  public ExtCacheEntry put(FileCacheEntry entry,
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
    //FileCacheEntry cacheEntry = getKey(key);

    return false;
  }

  /**
   * Removes a cache entry
   */
  public boolean remove(FileCacheEntry entry, CacheConfig config)
  {
    HashKey key = entry.getKeyHash();
    CacheEntryValue oldEntryValue = loadLocalEntry(entry);

    HashKey oldValueHash
      = oldEntryValue != null ? oldEntryValue.getValueHashKey() : null;

    long version = oldEntryValue != null ? oldEntryValue.getVersion() + 1 : 1;
    int flags = oldEntryValue != null ? oldEntryValue.getFlags() : 0;

    long expireTimeout = oldEntryValue != null ? oldEntryValue.getExpireTimeout() : 0;
    long idleTimeout = oldEntryValue != null ? oldEntryValue.getIdleTimeout() : 0;
    long leaseTimeout = (oldEntryValue != null
      ? oldEntryValue.getLeaseTimeout()
      : 0);
    long localReadTimeout = (oldEntryValue != null
      ? oldEntryValue.getLocalReadTimeout()
      : 0);

    long accessTime = Alarm.getCurrentTime();
    long updateTime = accessTime;

    long localExpireTime = accessTime + 100L;
    CacheEntryValue entryValue = new CacheEntryValue(null, null,
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

    if (!entry.compareAndSet(oldEntryValue, entryValue)) {
      log.fine(this + " entryValue remove failed due to timing conflict"
        + " (key=" + key + ")");

      return oldValueHash != null;
    }

    if (oldEntryValue == null) {
      log.fine(this + " db remove failed due to timing conflict"
        + "(key=" + key + ")");
    } else {
      long timeout = 60000;
      if (_cacheMapBacking.updateSave(key, null, timeout, version)) {
      } else {
        log.fine(this + " db remove failed due to timing conflict"
          + "(key=" + key + ")");
      }
    }

    return oldValueHash != null;
  }

  FileCacheEntry getLocalEntry(HashKey key)
  {
    FileCacheEntry entryKey = getKey(key);

    CacheEntryValue valueEntryValue = entryKey.getEntryValue();

    if (valueEntryValue != null) {
      long idleTimeout = valueEntryValue.getIdleTimeout();
      long updateTime = valueEntryValue.getLastUpdateTime();

      long now = Alarm.getCurrentTime();

      if (idleTimeout < CacheConfig.TIME_INFINITY
        && updateTime + valueEntryValue.getIdleWindow() < now) {
        /* XXX:
         CacheEntryValue newEntry
           = new CacheEntryValue(valueEntryValue, idleTimeout, now);

         saveUpdateTime(entryKey, newEntry);
         */
      }
    }

    return entryKey;
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

      byte[] hash = mOut.getDigest();

      HashKey valueHash = new HashKey(hash);

      if (valueHash.equals(oldValueHash))
        return valueHash;

      int length = os.getLength();

      StreamSource source = new StreamSource(os);
      if (!_dataBacking.save(valueHash, source, length))
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

      if (!_dataBacking.load(valueKey, out)) {
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
