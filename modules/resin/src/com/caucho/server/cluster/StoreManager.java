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

import com.caucho.config.types.Period;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.ClassLoaderListener;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.management.server.PersistentStoreMXBean;
import com.caucho.util.Alarm;
import com.caucho.util.AlarmListener;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.vfs.TempOutputStream;
import com.caucho.vfs.TempStream;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class for distributed stores.
 */
abstract public class StoreManager
  implements AlarmListener, EnvironmentListener, ClassLoaderListener 
{
  private static final Logger log
    = Logger.getLogger(StoreManager.class.getName());
  private static final L10N L = new L10N(StoreManager.class);
  
  private static int DECODE[];

  private Cluster _cluster;
  private String _serverId;
  
  private HashManager _hashManager;
  
  protected int _selfIndex;
  private ClusterServer []_serverList;
  
  private Alarm _alarm;
  
  // protected long _maxIdleTime = 24 * 3600 * 1000L;
  protected long _maxIdleTime = 3 * 3600 * 1000L;
  protected long _idleCheckInterval = 5 * 60 * 1000L;

  protected boolean _isAlwaysLoad;
  protected boolean _isAlwaysSave;
  
  protected HashMap<HashKey,Store> _storeMap;
  protected LruCache<HashKey,ClusterObject> _clusterObjects;

  private final Lifecycle _lifecycle = new Lifecycle(log, toString());

  //
  // statistics
  //
  
  protected volatile long _loadCount;
  protected volatile long _loadFailCount;

  protected volatile long _saveCount;
  protected volatile long _saveFailCount;

  protected StoreManager()
  {
    _hashManager = new HashManager();
    _storeMap = new HashMap<HashKey,Store>();
    
    _clusterObjects = new LruCache<HashKey,ClusterObject>(4096);
    _clusterObjects.setEnableListeners(false);
    
    _alarm = new Alarm(this);

    Environment.addClassLoaderListener(this);
  }

  /**
   * Sets the cluster.
   */
  public void setCluster(Cluster cluster)
  {
    _cluster = cluster;
  }

  /**
   * Gets the cluster.
   */
  public Cluster getCluster()
  {
    return _cluster;
  }

  /**
   * Returns the admin.
   */
  public PersistentStoreMXBean getAdmin()
  {
    return null;
  }

  /**
   * Set true if the objectStore should always try to loadImpl the object.
   */
  public void setAlwaysLoad(boolean alwaysLoad)
  {
    _isAlwaysLoad = alwaysLoad;
  }

  /**
   * Set true if the objectStore should always try to loadImpl the object.
   */
  public boolean isAlwaysLoad()
  {
    return _isAlwaysLoad;
  }

  /**
   * Set true if the objectStore should always try to objectStore the object.
   */
  public void setAlwaysSave(boolean alwaysSave)
  {
    _isAlwaysSave = alwaysSave;
  }

  /**
   * Set true if the objectStore should always try to objectStore the object.
   */
  public boolean isAlwaysSave()
  {
    return _isAlwaysSave;
  }

  /**
   * Returns the length of time an idle object can remain in the objectStore before
   * being cleaned.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  /**
   * Sets the length of time an idle object can remain in the objectStore before
   * being cleaned.
   */
  public void setMaxIdleTime(Period maxIdleTime)
  {
    _maxIdleTime = maxIdleTime.getPeriod();
  }

  /**
   * Sets the idle check interval for the alarm.
   */
  public void updateIdleCheckInterval(long idleCheckInterval)
  {
    if (_idleCheckInterval > 0 && idleCheckInterval > 0
	&& idleCheckInterval < _idleCheckInterval) {
      _idleCheckInterval = idleCheckInterval;
      _alarm.queue(idleCheckInterval);
    }

    if (_idleCheckInterval >= 0 && _idleCheckInterval < 1000)
      _idleCheckInterval = 1000;
  }

  /**
   * Sets the idle check interval for the alarm.
   */
  public long getIdleCheckTime()
  {
    if (_idleCheckInterval > 0)
      return _idleCheckInterval;
    else
      return _maxIdleTime;
  }

  /**
   * Returns the length of time an idle object can remain in the objectStore before
   * being cleaned.
   */
  public long getAccessWindowTime()
  {
    long window = _maxIdleTime / 4;

    if (window < 60000L)
      return 60000L;
    else
      return window;
  }

  //
  // statistics
  //

  /**
   * Returns the objects in the objectStore
   */
  public long getObjectCount()
  {
    return -1;
  }

  /**
   * Returns the total objects loaded.
   */
  public long getLoadCount()
  {
    return _loadCount;
  }

  /**
   * Returns the objects which failed to loadImpl.
   */
  public long getLoadFailCount()
  {
    return _loadFailCount;
  }

  /**
   * Returns the total objects saved.
   */
  public long getSaveCount()
  {
    return _saveCount;
  }

  /**
   * Returns the objects which failed to save.
   */
  public long getSaveFailCount()
  {
    return _saveFailCount;
  }

  /**
   * Creates a Store.  The Store manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store createStore(String storeId, ObjectManager objectManager)
  {
    HashKey storeKey = _hashManager.generateHash(storeId);
    
    Store store = getStore(storeKey);
    store.setName(storeId);
    store.setObjectManager(objectManager);

    return store;
  }

  /**
   * Removes a Store.  The Store manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store removeStore(HashKey storeKey)
  {
    Store store = getStore(storeKey);

    store.setObjectManager(null);

    return store;
  }

  /**
   * Creates a ClusterObjectManager.  The ClusterObjectManager manages
   * persistent objects for a particular domain, like the sesions
   * for the /foo URL.
   *
   * @param storeId the persistent domain.
   */
  public Store getStore(HashKey storeKey)
  {
    synchronized (_storeMap) {
      Store store = _storeMap.get(storeKey);
      
      if (store == null) {
	store = new Store(storeKey, this);
        
	_storeMap.put(storeKey, store);
      }

      return store;
    }
  }

  /**
   * Called after any factory settings.
   */
  @PostConstruct
  public boolean init()
  {
    if (! _lifecycle.toInit())
      return false;

    _lifecycle.setName(toString());

    if (_cluster == null)
      _cluster = Cluster.getLocal();
    
    if (_cluster != null) {
      _serverId = Cluster.getServerId();
      ClusterServer selfServer = _cluster.getSelfServer();

      if (selfServer != null)
	_selfIndex = selfServer.getIndex();
      else if (_cluster.getServerList().length > 1) {
	// XXX: error?
	log.warning(L.l("cluster-store for '{0}' needs an <srun> configuration for it.",
			_serverId));
      }

      ClusterServer []serverList = _cluster.getServerList();
      
      _serverList = new ClusterServer[serverList.length];

      for (int i = 0; i < serverList.length; i++) {
	_serverList[i] = serverList[i];
      }
    }

    Environment.addEnvironmentListener(this);

    return true;
  }

  /**
   * Called to start the objectStore.
   */
  public boolean start()
    throws Exception
  {
    if (! _lifecycle.toActive())
      return false;
    
    // notify the siblings that we're awake
    if (_serverList != null) {
      ClusterServer []serverList = _serverList;

      for (int i = 0; i < serverList.length; i++) {
	ServerPool server = serverList[i].getServerPool();

	if (server == null)
	  continue;

	try {
	  ClusterStream s = server.open();
	  s.close();
	} catch (Throwable e) {
	}
      }
      
    }

    handleAlarm(_alarm);

    return true;
  }

  /**
   * Called to start any invalidate processing
   */
  public boolean startUpdate()
    throws Exception
  {
    return true;
  }

  /**
   * Cleans old objects.  Living objects corresponding to the old
   * objects are not cleared, since their timeout should be less than
   * the objectStore timeout.
   */
  public void clearOldObjects()
    throws Exception
  {
  }

  /**
   * Returns true if this server is a primary for the given object id.
   */
  protected boolean isPrimary(String id)
  {
    return getPrimaryIndex(id, 0) == getSelfIndex();
  }

  /**
   * Returns the owning index.
   */
  public int getPrimaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Returns the backup index.
   */
  public int getSecondaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Returns the backup index.
   */
  public int getTertiaryIndex(String id, int offset)
  {
    return 0;
  }

  /**
   * Loads an object from the backing objectStore.
   *
   * @param obj the object to updateImpl.
   */
  abstract protected boolean load(ClusterObject clusterObject, Object obj)
    throws Exception;

  /**
   * Updates the object's objectAccess time.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to updateImpl.
   */
  public void access(HashKey objectId)
    throws Exception
  {
    ClusterObject obj = getClusterObject(objectId);

    if (obj != null)
      obj.accessImpl();
    else
      accessImpl(obj.getObjectId());
  }

  /**
   * Updates the object's objectAccess time.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to updateImpl.
   */
  public void access(Store store, String id)
    throws Exception
  {
    getClusterObject(store, id).objectAccess();
  }
  
  /**
   * Updates the object's objectAccess time in the persistent objectStore.
   *
   * @param uniqueId the identifier of the object.
   */
  abstract public void accessImpl(HashKey objectId)
    throws Exception;
  
  /**
   * Updates the object's objectAccess time in the persistent objectStore.
   *
   * @param uniqueId the identifier of the object.
   */
  public void accessImpl(ClusterObject obj)
    throws Exception
  {
    accessImpl(obj.getObjectId());
  }
  
  /**
   * Sets the timef for the expires interval.
   *
   * @param uniqueId the identifier of the object.
   * @param long the time in ms for the expire
   */
  public void setExpireInterval(HashKey uniqueId, long expires)
    throws Exception
  {
  }

  /**
   * Notify the object that the data has changed.
   *
    * @param objectId the identifier of the object to notify
   */
  public void invalidate(HashKey objectId)
    throws Exception
  {
    ClusterObject obj = getClusterObject(objectId);

    if (obj != null)
      obj.updateImpl();
  }

  /**
   * Updates the owner object.
   *
   * @param uniqueId the identifier of the storage group
   */
  public void updateOwner(ClusterObject objectId)
    throws Exception
  {
    
  }
  
  /**
   * Saves the object to the cluster.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to objectStore.
   */
  /*
  public void objectStore(Store objectStore, HashKey objectId, Object value)
    throws IOException
  {
    ClusterObject clusterObj = getClusterObject(objectId);

    if (clusterObj != null) {
    }
    else if (objectStore.getObjectManager().isEmpty(value))
      return;
    else
      clusterObj = createClusterObject(objectStore, objectId);
    
    clusterObj.objectStore(value);
  }
   */

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to objectStore.
   */
  /*
  ClusterObject createClusterObject(Store objectStore, String id)
  {
    HashKey key = _hashManager.generateHash(objectStore.getId(), id);

    return createClusterObject(objectStore, key);
  }
   */

  /**
   * Creates the cluster object given the objectStore and id
   *
   * @param objectStore the owning persistent objectStore
   * @param id the object's unique identifier in the objectStore
   * @param primary the primary owning server
   * @param secondary the secondary backup
   * @param tertiary the tertiary backup
   */
  ClusterObject createClusterObject(Store store,
                                    String id,
                                    int primary,
                                    int secondary,
                                    int tertiary)
  {
    HashKey key = _hashManager.generateHash(store.getId(), id);

    ClusterObject obj = createClusterObject(store, key,
					    primary, secondary, tertiary);
    
    obj.setObjectManagerKey(id);

    return obj;
  }
 
  /**
   * Returns the cluster object.
   *
   * @param objectStore the owning cluster objectStore
   * @param id the object's unique identifier
   * @param primary the object's owning server
   * @param secondary the object's secondary backup
   * @param tertiary the object's tertiary backup
   */
  ClusterObject createClusterObject(Store store, 
                                    HashKey key,
                                    int primary,
                                    int secondary,
                                    int tertiary)
  {
    try {
      synchronized (_clusterObjects) {
	ClusterObject object = _clusterObjects.get(key);
	if (object == null) {
	  object = create(store, key, primary, secondary, tertiary);
	  _clusterObjects.put(key, object);
	}

	return object;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    }
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to objectStore.
   */
  ClusterObject getClusterObject(Store store, String id)
  {
    return getClusterObject(makeUniqueId(store, id));
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to objectStore.
   */
  ClusterObject getClusterObject(HashKey key)
  {
    synchronized (_clusterObjects) {
      return _clusterObjects.get(key);
    }
  }

  /**
   * Returns the cluster object.
   *
   * @param storeId the identifier of the storage group
   * @param obj the object to objectStore.
   */
  ClusterObject removeClusterObject(HashKey key)
  {
    synchronized (_clusterObjects) {
      return _clusterObjects.remove(key);
    }
  }
  
  /**
   * Creates the cluster object.
   */
  protected ClusterObject create(Store store, 
                                 HashKey key,
                                 int primary,
                                 int secondary,
                                 int tertiary)
  {
    return new ClusterObject(store, key, primary, secondary, tertiary);
  }
  
  /**
   * Save the object to the objectStore.
   *
   * @param clusterObject the distributed handle
   * @param tempStream the byte stream to the saved data
   * @param dataHash the sha-1 hash of the data
   * @param oldDataHash the previous hash value for the data
   */
  abstract protected void store(ClusterObject clusterObject,
				TempOutputStream tempStream,
				byte []dataHash, byte []oldDataHash)
    throws Exception;
  
  /**
   * Handles a callback from an alarm, scheduling the timeout.
   */
  public void handleAlarm(Alarm alarm)
  {
    if (! _lifecycle.isActive())
      return;

    try {
      clearOldObjects();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _alarm.queue(getIdleCheckTime());
    }
  }

  /**
   * When the object is no longer valid, objectRemove it from the backing objectStore.
   *
   * @param obj the object to objectRemove
   */
  public void remove(ClusterObject obj)
    throws Exception
  {
    
  }

  /**
   * When the object is no longer valid, objectRemove it from the backing objectStore.
   *
   * @param objectStore the identifier of the storeage group
   * @param objectId the identifier of the object to objectRemove
   */
  public void remove(Store store, String objectId)
    throws Exception
  {
  }

  /**
   * Returns the unique id.
   */
  private HashKey makeUniqueId(Store store, String objectId)
  {
    return _hashManager.generateHash(store.getId(), objectId);
  }

  /**
   * Returns the unique id.
   */
  private String makeUniqueId(String storeId, String objectId)
  {
    return storeId + ';' + objectId;
  }

  /**
   * Returns the self servers.
   */
  protected int getSelfIndex()
  {
    return _selfIndex;
  }

  /**
   * Returns the list of cluster servers.
   */
  protected ClusterServer []getServerList()
  {
    return _serverList;
  }

  /**
   * Returns the cluster server which owns the object
   */
  protected ClusterServer getOwningServer(String objectId)
  {
    if (_cluster == null)
      return null;
    
    char ch = objectId.charAt(0);
    
    ClusterServer []serverList = _serverList;

    if (serverList.length > 0) {
      int srunIndex = decode(ch) % serverList.length;

      return serverList[srunIndex];
    }
    else
      return null;
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
    try {
      start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Handles the case where the environment loader is stops
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment is activated.
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }
  
  /**
   * Handles the case where the environment loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
    destroy();
  }
  
  /**
   * Called at end of life.
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroy())
      return;
    
    _alarm.dequeue();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _serverId + "]";
  }

  static class ObjectKey {
    private String _storeId;
    private String _objectId;

    ObjectKey()
    {
    }
    
    ObjectKey(String storeId, String objectId)
    {
      init(storeId, objectId);
    }

    void init(String storeId, String objectId)
    {
      _storeId = storeId;
      _objectId = objectId;
    }

    @Override
    public int hashCode()
    {
      return _storeId.hashCode() * 65521 + _objectId.hashCode();
    }

    @Override
    public boolean equals(Object o)
    {
      if (this == o)
	return true;
      else if (! (o instanceof ObjectKey))
	return false;

      ObjectKey key = (ObjectKey) o;

      return _objectId.equals(key._objectId) && _storeId.equals(key._storeId);
    }
  }

  static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;
    
    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
