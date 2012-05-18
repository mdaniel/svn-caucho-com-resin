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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.caucho.config.ConfigException;
import com.caucho.db.jdbc.DataSourceImpl;
import com.caucho.env.distcache.CacheDataBacking;
import com.caucho.env.health.HealthSystemFacade;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.service.RootDirectorySystem;
import com.caucho.server.distcache.MnodeStore.ExpiredMnode;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
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
  
  private Alarm _reaperAlarm = new Alarm(new ReaperListener());
  private long _reaperTimeout = 3600 * 1000;
  
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

  /**
   * Returns the local value from the database
   */
  @Override
  public MnodeEntry loadLocalEntryValue(HashKey key)
  {
    return _mnodeStore.load(key);
  }

  /**
   * Sets a cache entry
   */
  @Override
  public MnodeEntry insertLocalValue(HashKey key, 
                                     MnodeEntry mnodeUpdate,
                                     MnodeEntry oldEntryValue)
  {
    // MnodeUpdate mnodeUpdate = new MnodeUpdate(mnodeValue);
    
    if (oldEntryValue == null
        || oldEntryValue.isImplicitNull()
        || oldEntryValue == MnodeEntry.NULL) {
      if (_mnodeStore.insert(key, mnodeUpdate, mnodeUpdate.getValueDataId())) {
        return mnodeUpdate;
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ")");

        return oldEntryValue;
      }
    } else {
      if (_mnodeStore.updateSave(key.getHash(), 
                                 mnodeUpdate,
                                 mnodeUpdate.getValueDataId())) {
        return mnodeUpdate;
      }
      else if (_mnodeStore.insert(key, mnodeUpdate, 
                                  mnodeUpdate.getValueDataId())) {
        return mnodeUpdate;
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ")");

        return oldEntryValue;
      }
    }
  }

  @Override
  public boolean putLocalValue(MnodeEntry mnodeEntry,
                               HashKey key,
                               MnodeEntry oldEntryEntry,
                               MnodeValue mnodeUpdate)
  {
    boolean isSave = false;
    
    if (oldEntryEntry == null
        || oldEntryEntry.isImplicitNull()
        || oldEntryEntry == MnodeEntry.NULL) {
      if (_mnodeStore.insert(key, mnodeUpdate, mnodeEntry.getValueDataId())) {
        isSave = true;
        
        addCreateCount();
      } else {
        log.fine(this + " db insert failed due to timing conflict"
                 + "(key=" + key + ", version=" + mnodeUpdate.getVersion() + ")");
      }
    } else {
      if (_mnodeStore.updateSave(key.getHash(), 
                                 mnodeUpdate,
                                 mnodeEntry.getValueDataId())) {
        isSave = true;
      }
      else if (_mnodeStore.insert(key, 
                                  mnodeUpdate,
                                  mnodeEntry.getValueDataId())) {
        isSave = true;
        
        addCreateCount();
      }
      else {
        log.fine(this + " db update failed due to timing conflict"
                 + "(key=" + key + ", version=" + mnodeUpdate.getVersion() + ")");
      }
    }
    
    if (isSave && oldEntryEntry != null) {
      long oldDataId = oldEntryEntry.getValueDataId();

      // XXX: create delete queue?
      if (oldDataId > 0 && mnodeEntry.getValueDataId() != oldDataId) {
        removeData(oldDataId);
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
                          WriteStream os)
    throws IOException
  {
    return _dataStore.load(valueDataId, os);
  }

  @Override
  public java.sql.Blob loadBlob(long valueDataId)
  {
    return _dataStore.loadBlob(valueDataId);
  }
  
  public long saveData(StreamSource source, int length)
  {
    try {
      return _dataStore.save(source, length);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  @Override
  public long saveData(InputStream is, int length)
    throws IOException
  {
    return _dataStore.save(is, length);
  }
  
  @Override
  public boolean removeData(long dataId)
  {
    // return _dataStore.remove(dataId);
    
    _removeActor.offer(dataId);
    
    return true;
  }

  @Override
  public boolean isDataAvailable(long valueIndex)
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

  public void start()
  {
    try {
      Path dataDirectory = RootDirectorySystem.getCurrentDataDirectory();

      String serverId = ResinSystem.getCurrentId();

      if (serverId.isEmpty())
        serverId = "default";
      
      DataSource dataSource = createDataSource(dataDirectory, serverId);
      
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
      _mnodeStore = new MnodeStore(dataSource, tableName, serverId);
      _mnodeStore.init();
      
      _dataStore = new DataStore(serverId, _mnodeStore);
      _dataStore.init();
      
      _removeActor = new DataRemoveActor(_dataStore);
      
      updateCreateReaperCount();
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
    
    _reaperAlarm.queue(0);
  }
  
  private DataSource createDataSource(Path dataDirectory, String serverId)
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
  

  /**
   * Clears the expired data
   */
  private synchronized void removeExpiredData()
  {
    int removeCount = 0;
    boolean isExpire;
    
    long preCount = _mnodeStore.getCount();
    
    do {
      isExpire = false;
      
      for (ExpiredMnode data : _mnodeStore.selectExpiredData()) {
        try {
          if (removeData(data.getKey(), data.getDataId())) {
            removeCount++;
            isExpire = true;
          }
        } catch (Exception e) {
          log.log(Level.FINER, e.toString(), e);
        }
      }
    } while (isExpire);
    
    if (log.isLoggable(Level.FINE)) {
      log.fine(this + " removed " + removeCount + " expired entries");
    }
        
    /*
    System.out.println("REMOVED: " + removeCount
                       + " pre " + preCount
                       + " entries " + _mnodeStore.getCount());
                       */
    
  }
  private boolean removeData(byte []key, long dataId)
  {
    DistCacheEntry distEntry = _manager.getCacheEntry(HashKey.create(key));
    
    distEntry.clear();

    if (! _mnodeStore.remove(key)) {
      return false;
    }
    
    if (dataId > 0) {
      removeData(dataId);
    }
    
    return true;
  }

  private void updateCreateReaperCount()
  {
    long entryCount = _mnodeStore.getCount();
    long createCount = _createCount.get();
    
    int delta = Math.max(1024, (int) (entryCount / 8));
    
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
    
    if (removeActor != null)
      removeActor.close();
    
    if (mnodeStore != null)
      mnodeStore.close();
    
    if (dataStore != null)
      dataStore.destroy();
  }
  
  class ReaperListener implements AlarmListener {
    @Override
    public void handleAlarm(Alarm alarm)
    {
      updateCreateReaperCount();
      
      synchronized (this) {
        try {
          removeExpiredData();
        } finally {
          if (_mnodeStore != null) {
            alarm.queue(_reaperTimeout);
          }
        }
      }
    }
  }
}
