/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.config.ConfigException;
import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.health.HealthSystemFacade;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.service.RootDirectorySystem;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.server.distcache.DataStore.DataItem;
import com.caucho.server.distcache.MnodeStore.ExpiredMnode;
import com.caucho.server.distcache.MnodeStore.ExpiredState;
import com.caucho.server.distcache.MnodeStore.Mnode;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.CurrentTime;
import com.caucho.util.HashKey;
import com.caucho.vfs.Path;
import com.caucho.vfs.StreamSource;
import com.caucho.vfs.WriteStream;

/**
 * Manages the distributed cache
 */
public class CacheDataBackingImpl implements CacheDataBacking {
  private static final Logger log
    = Logger.getLogger(CacheDataBackingImpl.class.getName());

  private CacheStoreManager _manager;
  private DataStore _dataStore;
  private MnodeStore _mnodeStore;

  private DataRemoveActor _removeActor;

  private final AtomicLong _createCount = new AtomicLong();
  private long _createReaperCount;

  private Alarm _reaperAlarm;

  private DataSourceImpl _dataSource;
  
  private Lifecycle _lifecycle = new Lifecycle();
  
  //private long _reaperTimeout = 5 * 60 * 1000;
  //private long _reaperTimeout = 5 * 60 * 1000;
  private long _reaperTimeout = 1 * 60 * 1000;

  private long _reaperCycleMaxActiveDurationMs = 1 * 1000;
  private double _reaperCycleIdleToActiveUtilizationRatio = 2.0;

  public CacheDataBackingImpl(CacheStoreManager storeManager)
  {
    _manager = storeManager;
  }

  public void setDataStore(DataStore dataStore)
  {
    _dataStore = dataStore;
  }

  public void setMnodeStore(MnodeStore mnodeStore)
  {
    _mnodeStore = mnodeStore;
  }

  @Override
  public DataStore getDataStore()
  {
    return _dataStore;
  }

  @Override
  public MnodeStore getMnodeStore()
  {
    return _mnodeStore;
  }

  public long getReaperTimeout()
  {
    return _reaperTimeout;
  }

  public void setReaperTimeout(long ms)
  {
    _reaperTimeout = ms;
  }

  public long getReaperCycleMaxActiveTime()
  {
    return _reaperCycleMaxActiveDurationMs;
  }

  public void setReaperCycleMaxActiveTime(long ms)
  {
    _reaperCycleMaxActiveDurationMs = ms;
  }

  public double getReaperCycleIdleToActiveUtilizationRatio()
  {
    return _reaperCycleIdleToActiveUtilizationRatio;
  }

  public void setReaperCycleIdleToActiveUtilizationRatio(double ratio)
  {
    _reaperCycleIdleToActiveUtilizationRatio = ratio;
  }

  /**
   * Returns the local value from the database
   */
  @Override
  public MnodeEntry loadLocalEntryValue(HashKey key)
  {
    MnodeStore mnodeStore = _mnodeStore;
    
    if (mnodeStore != null) {
      // #5633
      return mnodeStore.load(key);
    }
    else {
      return null;
    }
  }

  /**
   * Sets a cache entry
   */
  @Override
  public MnodeEntry insertLocalValue(HashKey key,
                                     HashKey cacheKey,
                                     MnodeEntry mnodeUpdate,
                                     MnodeEntry oldEntryValue)
  {
    // MnodeUpdate mnodeUpdate = new MnodeUpdate(mnodeValue);
    MnodeEntry entry = null;
    boolean isSave = false;
    
    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeEntry.NULL) {
      if (_mnodeStore.insert(key, cacheKey, mnodeUpdate,
                             mnodeUpdate.getValueDataId(),
                             mnodeUpdate.getValueDataTime(),
                             mnodeUpdate.getLastAccessedTime(),
                             mnodeUpdate.getLastModifiedTime())) {
        entry = mnodeUpdate;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ")");

        entry = oldEntryValue;
      }
    } else {
      if (_mnodeStore.updateSave(key.getHash(),
                                 cacheKey.getHash(),
                                 mnodeUpdate,
                                 mnodeUpdate.getValueDataId(),
                                 mnodeUpdate.getValueDataTime(),
                                 mnodeUpdate.getLastAccessedTime(),
                                 mnodeUpdate.getLastModifiedTime())) {
        isSave = true;
        entry = mnodeUpdate;
      }
      else if (_mnodeStore.insert(key, cacheKey, mnodeUpdate,
                                  mnodeUpdate.getValueDataId(),
                                  mnodeUpdate.getValueDataTime(),
                                  mnodeUpdate.getLastAccessedTime(),
                                  mnodeUpdate.getLastModifiedTime())) {
        isSave = true;
        entry = mnodeUpdate;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ")");

        entry = oldEntryValue;
      }
    }
    
    if (isSave && oldEntryValue != null) {
      long oldDataId = oldEntryValue.getValueDataId();
      long oldDataTime = oldEntryValue.getValueDataTime();

      // XXX: create delete queue?
      if (oldDataId > 0 && mnodeUpdate.getValueDataId() != oldDataId) {
        removeData(oldDataId, oldDataTime);
      }
    }
    
    return entry;
  }

  @Override
  public boolean putLocalValue(MnodeEntry mnodeEntry,
                               HashKey key,
                               HashKey cacheKey,
                               MnodeEntry oldEntryEntry,
                               MnodeUpdate mnodeUpdate)
  {
    boolean isSave = false;
    
    MnodeStore mnodeStore = _mnodeStore;
    
    if (mnodeStore == null) {
      return true;
    }

    if (oldEntryEntry == null
        || oldEntryEntry.isImplicitNull()
        || oldEntryEntry == MnodeEntry.NULL) {
      // long now = CurrentTime.getCurrentTime();
      long lastAccessTime = mnodeUpdate.getLastAccessTime();
      long lastModifiedTime = mnodeUpdate.getLastAccessTime();

      if (mnodeStore.insert(key, cacheKey,
                             mnodeUpdate, 
                             mnodeEntry.getValueDataId(),
                             mnodeEntry.getValueDataTime(),
                             lastAccessTime, lastModifiedTime)) {
        isSave = true;

        addCreateCount();
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ", version=" + mnodeUpdate.getVersion() + ")");
      }
    } else {
      if (mnodeStore.updateSave(key.getHash(),
                                cacheKey.getHash(),
                                mnodeUpdate,
                                mnodeEntry.getValueDataId(),
                                mnodeEntry.getValueDataTime(),
                                mnodeEntry.getLastAccessedTime(),
                                mnodeEntry.getLastModifiedTime())) {
       isSave = true;
     }
      else if (mnodeStore.insert(key,
                                  cacheKey,
                                  mnodeUpdate,
                                  mnodeEntry.getValueDataId(),
                                  mnodeEntry.getValueDataTime(),
                                  mnodeEntry.getLastAccessedTime(),
                                  mnodeEntry.getLastModifiedTime())) {
        isSave = true;

        addCreateCount();
      }
      else if (mnodeStore.updateSave(key.getHash(),
                                cacheKey.getHash(),
                                mnodeUpdate,
                                mnodeEntry.getValueDataId(),
                                mnodeEntry.getValueDataTime(),
                                mnodeEntry.getLastAccessedTime(),
                                mnodeEntry.getLastModifiedTime())) {
       isSave = true;
     }
      else {
        log.info(this + " db update failed due to timing conflict"
                 + "(key=" + key + ", version=" + mnodeUpdate.getVersion() + ")");
      }
    }

    if (isSave && oldEntryEntry != null) {
      long oldDataId = oldEntryEntry.getValueDataId();
      long oldDataTime = oldEntryEntry.getValueDataTime();

      // XXX: create delete queue?
      if (oldDataId > 0 && mnodeEntry.getValueDataId() != oldDataId) {
        removeData(oldDataId, oldDataTime);
      }
    }

    return isSave;
  }

  private void addCreateCount()
  {
    _createCount.incrementAndGet();

    if (_createReaperCount < _createCount.get()) {
      updateCreateReaperCount();

      _reaperAlarm.queue(0);
    }
  }

  @Override
  public MnodeEntry saveLocalUpdateTime(HashKey keyHash,
                                        MnodeEntry mnodeValue,
                                        MnodeEntry oldMnodeValue)
  {
    if (_mnodeStore.updateAccessTime(keyHash,
                                     mnodeValue.getVersion(),
                                     mnodeValue.getAccessedExpireTimeout(),
                                     mnodeValue.getLastAccessedTime())) {
      return mnodeValue;
    } else {
      log.fine(this + " db updateTime failed due to timing conflict"
               + "(key=" + keyHash + ", version=" + mnodeValue.getVersion() + ")");

      return oldMnodeValue;
    }
  }

  @Override
  public boolean loadData(long valueDataId,
                          long valueDataTime,
                          WriteStream os)
    throws IOException
  {
    return _dataStore.load(valueDataId, valueDataTime, os);
  }

  @Override
  public java.sql.Blob loadBlob(long valueDataId, long valueDataTime)
  {
    return _dataStore.loadBlob(valueDataId, valueDataTime);
  }

  public DataItem saveData(StreamSource source, int length)
  {
    try {
      DataStore dataStore = _dataStore;
      
      if (dataStore != null) {
        return dataStore.save(source, length);
      }
      else {
        return null;
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DataItem saveData(InputStream is, int length)
    throws IOException
  {
    return _dataStore.save(is, length);
  }

  @Override
  public boolean removeData(long dataId, long dataTime)
  {
    // return _dataStore.remove(dataId);

    _removeActor.offer(new DataItem(dataId, dataTime));

    return true;
  }

  @Override
  public boolean isDataAvailable(long valueIndex,
                                 long valueDataTime)
  {
    return valueIndex > 0;
    /*
    if (valueKey == null || valueKey == HashManager.NULL)
      return false;

    return _dataStore.isDataAvailable(valueKey, valueIndex);
    */
  }

  /**
   * Returns the last update time on server startup.
   */
  @Override
  public long getStartupLastUpdateTime()
  {
    return _mnodeStore.getStartupLastUpdateTime();
  }

  /**
   * Returns the last update time on server startup.
   */
  @Override
  public long getStartupLastUpdateTime(HashKey cacheKey)
  {
    return _mnodeStore.getStartupLastUpdateTime(cacheKey);
  }

  /**
   * Returns a set of entries since an access time.
   */
  @Override
  public ArrayList<CacheData> getUpdates(long accessTime, int offset)
  {
    return _mnodeStore.getUpdates(accessTime, offset);
  }

  /**
   * Returns a set of entries since an access time.
   */
  @Override
  public ArrayList<CacheData> getUpdates(HashKey cacheKey,
                                         long accessTime,
                                         int offset)
  {
    return _mnodeStore.getUpdates(cacheKey, accessTime, offset);
  }

  public Iterator<HashKey> getEntries(HashKey cacheKey)
  {
    return _mnodeStore.getKeys(cacheKey);
  }

  @Override
  public void start()
  {
    try {
      if (! _lifecycle.toActive()) {
        return;
      }
      
      Path dataDirectory = RootDirectorySystem.getCurrentDataDirectory();

      String serverId = ResinSystem.getCurrentId();

      if (serverId.isEmpty())
        serverId = "default";

      _dataSource = createDataSource(dataDirectory, serverId);

      Path mnodeDb = dataDirectory.lookup(serverId).lookup("mnode.db");
      Path dataDb = dataDirectory.lookup(serverId).lookup("data.db");

      String exitMessage = HealthSystemFacade.getExitMessage();

      if (exitMessage.indexOf(mnodeDb.getFullPath()) >= 0
          || exitMessage.indexOf(dataDb.getFullPath()) >= 0) {
        log.warning("removing cache database " + mnodeDb.getFullPath() + " because of corruption");

        try {
          mnodeDb.remove();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }

        try {
          dataDb.remove();
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      String tableName = "mnode";
      _mnodeStore = new MnodeStore(_dataSource, tableName, serverId);
      _mnodeStore.init();

      _dataStore = new DataStore(serverId, _mnodeStore);
      _dataStore.init();

      _removeActor = new DataRemoveActor(_dataStore);
      
      _reaperAlarm = new Alarm(new ReaperListener());
      
      updateCreateReaperCount();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    _reaperAlarm.queue(0);
  }

  private DataSourceImpl createDataSource(Path dataDirectory, String serverId)
  {
    Path path = dataDirectory.lookup("distcache");

    if (path == null)
      throw new NullPointerException();

    try {
      path.mkdirs();
    } catch (IOException e) {
    }

    try {
      DataSourceImpl dataSource = new DataSourceImpl();
      dataSource.setPath(path);
      dataSource.setRemoveOnError(true);
      dataSource.init();

      return dataSource;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private boolean removeData(byte []key,
                             byte []cacheHash,
                             long dataId,
                             long dataTime)
  {
    DistCacheEntry distEntry = _manager.getCacheEntry(HashKey.create(key),
                                                      HashKey.create(cacheHash));

    distEntry.clear();

    boolean isRemove = _mnodeStore.remove(key);

    if (dataId > 0) {
      removeData(dataId, dataTime);
    }

    return isRemove;
  }

  private void updateCreateReaperCount()
  {
    long entryCount = _mnodeStore.getCount();
    long createCount = _createCount.get();

    int delta = Math.max(64 * 1024, (int) (entryCount / 8));

    _createReaperCount = createCount + delta;
  }


  @Override
  public void close()
  {
    _reaperAlarm.dequeue();

    MnodeStore mnodeStore = _mnodeStore;
    _mnodeStore = null;

    DataStore dataStore = _dataStore;
    _dataStore = null;

    DataRemoveActor removeActor = _removeActor;
    _removeActor = null;

    DataSourceImpl dataSource = _dataSource;
    _dataSource = null;
    
    if (removeActor != null) {
      removeActor.close();
    }
    
    if (mnodeStore != null) {
      mnodeStore.close();
    }

    if (dataStore != null) {
      dataStore.destroy();
    }
    
    if (dataSource != null) {
      dataSource.close();
    }
  }

  class ReaperListener implements AlarmListener {
    private ExpiredState _expireState;
    
    ReaperListener()
    {
      _expireState = _mnodeStore.createExpiredState();
    }
    
    @Override
    public void handleAlarm(Alarm alarm)
    {
      updateCreateReaperCount();

      synchronized (this) {
        try {
          removeExpiredData();
        }
        finally {
          if (_mnodeStore != null) {
            alarm.queue(_reaperTimeout);
          }
        }
      }
    }

    /**
     * Clears the expired data
     */
    private void removeExpiredData()
    {
      Throttler throttler
        = new Throttler(_reaperCycleMaxActiveDurationMs,
                        _reaperCycleIdleToActiveUtilizationRatio);

      Level level = Level.FINER;
      if (log.isLoggable(level)) {
        log.log(level, this + " starting mnode cache reaper run"
                            + ", current mnode count=" + _mnodeStore.getCount()
                            + ", reaperTimeout=" + _reaperTimeout + "ms"
                            + ", reaperCycleMaxActiveDuration=" + _reaperCycleMaxActiveDurationMs + "ms"
                            + ", reaperCycleIdleToActiveUtilizationRatio=" + _reaperCycleIdleToActiveUtilizationRatio);
      }

      // long oid = 0;
      // long mnodeCount = 0;
      long removeCount = 0;

      ArrayList<Mnode> mnodeList = _expireState.selectExpiredData();
      
      // mnodeCount += mnodeList.size();

      for (Mnode mnode : mnodeList) {
        if (! (mnode instanceof ExpiredMnode)) {
          continue;
        }

        ExpiredMnode expiredMnode = (ExpiredMnode) mnode;

        try {
          if (removeData(expiredMnode.getKey(),
                         expiredMnode.getCacheHash(),
                         expiredMnode.getDataId(),
                         expiredMnode.getDataTime())) {
            removeCount++;
          }

          // throttle the deletions
          // throttle(throttler, mnodeCount, removeCount);

        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
          
          log.warning(e.toString());
        }
      }
      
      if (mnodeList.size() > 0) {
        log.info(getClass().getSimpleName() + " removed " + mnodeList.size() + " expired items (removed=" + removeCount + ")");
      }

        // throttle the select query
        // throttle(throttler, mnodeCount, removeCount);

      long endTime = CurrentTime.getCurrentTime();

      if (log.isLoggable(level)) {
        log.log(level, this + " finished mnode cache reaper run"
                            + ", removed " + removeCount + " expired entries in " + (endTime - throttler._startTime) + "ms"
                            + ", current mnode count=" + _mnodeStore.getCount()
                            + ", total sleep duration=" + throttler._totalSleepDurationMs + "ms");
      }
    }

    private void throttle(Throttler throttler, long mnodeCount, long removeCount)
    {
      long sleepDurationMs = throttler.throttle();

      if (sleepDurationMs >= 0) {
        if (log.isLoggable(Level.FINEST)) {
          log.log(Level.FINEST, this + " idled mnode cache reaper for " + sleepDurationMs + "ms"
                                     + ", mnode scanned count=" + mnodeCount
                                     + ", current remove count=" + removeCount
                                     + ", last batch duration=" + throttler._lastBatchDurationMs + " ms");
        }
      }
    }
  }

  static class Throttler {
    final long _startTime;
    final long _maxActiveDurationMs;
    final double _idleToActiveUtilizationRatio;

    long _batchStartTime;
    long _totalSleepDurationMs;

    long _lastBatchDurationMs;

    public Throttler(long maxActiveDurationMs,
                     double idleToActiveUtilizationRatio)
    {
      _startTime = CurrentTime.getCurrentTime();
      _maxActiveDurationMs = maxActiveDurationMs;
      _idleToActiveUtilizationRatio = idleToActiveUtilizationRatio;

      _batchStartTime = _startTime;
    }

    public long throttle()
    {
      long batchEndTime = CurrentTime.getCurrentTime();
      long batchDurationMs = batchEndTime - _batchStartTime;

      // okay to be slightly under so multiply by 1.25
      if (_maxActiveDurationMs <= batchDurationMs * 1.25) {
        _lastBatchDurationMs = batchDurationMs;

        long sleepDurationMs = sleep(batchDurationMs, batchEndTime);

        resetBatch();

        return sleepDurationMs;
      }
      else {
        return -1;
      }
    }

    private void resetBatch()
    {
      _batchStartTime = CurrentTime.getCurrentTime();
    }

    private long sleep(long batchDurationMs, long batchEndTime)
    {
      try {
        long salt = batchDurationMs * (batchEndTime % 10 + 1) / 10;

        long sleepDurationMs
          = (long) (_idleToActiveUtilizationRatio * batchDurationMs) + salt;

        if (sleepDurationMs < 0) {
          sleepDurationMs = batchDurationMs * 2;
        }

        Thread.sleep(sleepDurationMs);
      }
      catch (InterruptedException e) {
      }

      long actualSleepDurationMs = CurrentTime.getCurrentTime() - batchEndTime;
      _totalSleepDurationMs += actualSleepDurationMs;

      return actualSleepDurationMs;
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _dataStore + "]";
  }
}
