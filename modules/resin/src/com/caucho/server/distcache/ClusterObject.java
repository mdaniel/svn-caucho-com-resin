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

import com.caucho.server.cluster.Cluster;
import com.caucho.server.cluster.ClusterServer;
import com.caucho.server.cluster.Server;
import com.caucho.util.Alarm;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.ref.WeakReference;

/**
 * Data for the cluster's object.
 */
public class ClusterObject {
  private static final Logger log
    = Logger.getLogger(ClusterObject.class.getName());

  private final HashKey _id;
  private final HashKey _storeId;

  private final StoreManager _storeManager;
  
  private WeakReference<Store> _storeRef;

  private Object _objectManagerKey; // key for the object manager
  
  private int _primary;
  private int _secondary;
  private int _tertiary;

  private long _maxIdleTime;
  
  private long _expireInterval = -1;

  private long _accessTime;

  private byte []_dataHash;

  // the object's obj is current with the persistent objectStore
  private boolean _isValid = false;
  // the object's obj has been modified, but not saved
  private boolean _isDirty = false;
  // the object has been removed
  private boolean _isDead = false;
  
  // unserializable objects are skipped
  private boolean _isSerializable = true;
  
  /**
   * Creates a new cluster object
   * 
   * @param objectStore the owning objectStore
   * @param id the object's unique identity
   * @param primary the primary owning server
   * @param secondary the secondary backup
   * @param tertiary the tertiary backup
   */
  ClusterObject(Store store,
		HashKey id,
                int primary,
                int secondary,
                int tertiary)
  {
    _id = id;
    _storeId = store.getId();
    
    _storeManager = store.getStoreManager();
    _storeRef = new WeakReference<Store>(store);
    _maxIdleTime = store.getMaxIdleTime();
    
    _primary = primary;
    _secondary = secondary;
    _tertiary = tertiary;
  
    _expireInterval = getMaxIdleTime() + getAccessWindow();
  }

  ClusterObject(StoreManager storeManager,
		HashKey storeId,
		HashKey objectId,
                int primary,
                int secondary,
                int tertiary)
  {
    _storeManager = storeManager;
    
    _primary = primary;
    _secondary = secondary;
    _tertiary = tertiary;

    _maxIdleTime = _storeManager.getMaxIdleTime();

    _storeId = storeId;
    _id = objectId;

    _expireInterval = getMaxIdleTime() + getAccessWindow();
  }

  // XXX: move to objectStore manager?
  public boolean isPrimary()
  {
    if (getStore() != null && getStore().isAlwaysLoad())
      return false;
    
    else if (getStore() == null && _storeManager.isAlwaysLoad())
      return false;
    
    Server server = _storeManager.getServer();
    
    return server.getSelfServer().getIndex() == _primary;
 }

  /**
   * Returns the store.
   */
  public Store getStore()
  {
    WeakReference<Store> storeRef = _storeRef;

    Store store;

    if (storeRef != null) {
      store = storeRef.get();

      if (store != null)
	return store;
    }

    store = _storeManager.getStore(_storeId);
    if (store != null)
      _storeRef = new WeakReference<Store>(store);
    
    return store;
  }

  public ObjectManager getObjectManager()
  {
    Store store = getStore();

    if (store != null)
      return store.getObjectManager();
    else
      return null;
  }

  /**
   * Returns the objectStore manager.
   */
  public StoreManager getStoreManager()
  {
    return _storeManager;
  }

  /**
   * Returns the objectStore id.
   */
  public HashKey getStoreId()
  {
    return _storeId;
  }

  /**
   * Returns the object id.
   */
  public HashKey getObjectId()
  {
    return _id;
  }

  /**
   * Returns the object manager key
   */
  public Object getObjectManagerKey()
  {
    return _objectManagerKey;
  }

  /**
   * Sets the object manager key
   */
  public void setObjectManagerKey(Object key)
  {
    _objectManagerKey = key;
  }

  /**
   * Sets the objectAccess time.
   */
  public void setAccessTime(long accessTime)
  {
    _accessTime = accessTime;
  }

  /**
   * Sets the max objectAccess time.
   */
  public long getExpireInterval()
  {
    return _expireInterval;
  }

  /**
   * Sets the max objectAccess time.
   */
  public void setExpireInterval(long expireInterval)
  {
    try {
      _expireInterval = expireInterval;
      
      _storeManager.setExpireInterval(getObjectId(), expireInterval);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }


  /**
   * Returns the max idle time.
   */
  public long getMaxIdleTime()
  {
    return _maxIdleTime;
  }

  /**
   * Sets the max idle time.
   */
  public void setMaxIdleTime(long maxIdleTime)
  {
    _maxIdleTime = maxIdleTime;
  }

  /**
   * Returns the objectAccess window.
   */
  public long getAccessWindow()
  {
    long window = _maxIdleTime / 4;

    if (window < 60000L)
      return 60000L;
    else
      return window;
  }

  /**
   * Returns true if the object has up-to-date loaded values
   */
  boolean isValid()
  {
    return _isValid;
  }
  
  /**
   * Returns the primary index for the object
   */
  public int getPrimaryIndex()
  {
    return _primary;
  }
  
  /**
   * Returns the secondary server for the object
   */
  public int getSecondaryIndex()
  {
    return _secondary;
  }
  
  /**
   * Returns the tertiary server for the object
   */
  public int getTertiaryIndex()
  {
    return _tertiary;
  }

   
  /**
   * Returns the primary server for the object
   */
  public ClusterServer getPrimaryServer()
  {
    Cluster cluster = _storeManager.getCluster();
    ClusterServer []serverList = cluster.getTriadList()[0].getServerList();
    
    if (serverList.length > 0)
      return serverList[_primary % serverList.length];
    else
      return null;
  }
  
  /**
   * Returns the secondary server for the object
   */
  public ClusterServer getSecondaryServer()
  {
    Cluster cluster = _storeManager.getCluster();
    ClusterServer []serverList = cluster.getTriadList()[0].getServerList();
    
    if (serverList.length > 1)
      return serverList[_secondary % serverList.length];
    else
      return null;
  }
  
  /**
   * Returns the tertiary server for the object
   */
  public ClusterServer getTertiaryServer()
  {
    Cluster cluster = _storeManager.getCluster();
    ClusterServer []serverList = cluster.getTriadList()[0].getServerList();
    
    if (serverList.length > 2)
      return serverList[_tertiary % serverList.length];
    else
      return null;
  }
  
  //
  // Object API
  // Called by the object itself (e.g. the session) to invalidate its own state
  //

  /**
   * Called by the object when it is accessed to invalidate the access time.
   */
  public void objectAccess()
  {
    long now = Alarm.getCurrentTime();

    if (getAccessWindow() <= now - _accessTime) {
      try {
	_storeManager.accessImpl(this);
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      _accessTime = now;
    }
  }

 
  /**
   * Called when the object is newly created.
   */
  public void objectCreate()
  {
    _isValid = true;
  }
  
  /**
   * Marks that the object no longer contains valid data 
   */
  public void objectInvalidated()
  {
    _isValid = false;
  }

  /**
   * Called by the object to load itself from the cluster. 
   * If the object fails to load, its contents may be in an
   * inconsistent state.
   *
   * @return true on success.
   */
  public boolean objectLoad(Object obj)
  {
    if (! _isSerializable)
      return true;

    if (_isDead)
      return false;

    // if the object is valid and the authoritative copy, it can
    // be returned directly
    if (_isValid && isPrimary())
      return true;

    try {
      if (_storeManager.load(this, obj)) {
	_isValid = true;
	
	return true;
      }
      else {
	_dataHash = null;
	return false;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      _dataHash = null;

      return false;
    } finally {
      _isDirty = false;
    }
  }

  /**
   * Signals that the object has been changed by the application, i.e.
   * that the object needs to be saved to the persistent objectStore.
   */
  public void objectModified()
  {
    _isDirty = true;
  }

  /**
   * Called by the object to remove itself from the cluster.
   */
  public void objectRemove()
  {
    try {
      if (_isDead)
	return;
      _isDead = true;

      _storeManager.remove(this);

      /*
      if (_objectManager != null)
	_objectManager.remove(this);
      */
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }
  
  /**
   * Called by the object to save itself to the cluster.
   */
  public void objectStore(Object obj)
    throws IOException
  {
    if (! _isSerializable)
      return;

    // if the object is not the owner, the value is no longer valid
    if (! isPrimary()) {
      _isValid = false;
    }

    if (! _isDirty && ! getStore().isAlwaysSave())
      return;

    _isDirty = false;

    // XXX: quercus
    if (getObjectManager() == null)
      return;
    
    TempOutputStream tempStream = new TempOutputStream();

    try {
      DigestOutputStream digestStream = new DigestOutputStream(tempStream);

      getObjectManager().store(digestStream, obj);

      digestStream.close();
 
      byte []dataHash = digestStream.getDigest();

      if (isMatch(dataHash, _dataHash))
	return;

      byte []oldDataHash = _dataHash;
      
      _dataHash = dataHash;

      _storeManager.store(this, tempStream, dataHash, oldDataHash);

      if (isPrimary())
	_isValid = true;

      _accessTime = Alarm.getCurrentTime();
    } catch (NotSerializableException e) {
      log.warning(e.toString());
      _isSerializable = false;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      _isValid = false;
    } finally {
      tempStream.destroy();
    }
  }
  
  private static boolean isMatch(byte []a, byte []b)
  {
    if (a == null || b == null)
      return false;
    
    int len = a.length;
    
    if (len != b.length)
      return false;
    
    for (int i = len - 1; i >= 0; i--) {
      if (a[i] != b[i])
        return false;
    }
    
    return true;
  }
  
  //
  // Cluster API
  // Called by the clustering to invalidate the object's state
  //

  /**
   * Loads the object, called from the objectStore.
   */
  boolean loadImpl(InputStream is, Object value, byte []dataHash)
    throws IOException
  {
    getObjectManager().load(is, value);

    _dataHash = dataHash;
 
    return true;
  }

  /**
   * Signals that the object has been updated externally, i.e.
   * that the persistent objectStore now has a more current version of
   * the object's data.
   */
  public void updateImpl()
  {
    _isValid = false;
  }
  
  /**
   * Called when the object is accessed to updateImpl the objectAccess time.
   */
  public void accessImpl()
  {
    long now = Alarm.getCurrentTime();

    if (getAccessWindow() <= now - _accessTime) {
      try {
	_storeManager.accessImpl(this);
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      _accessTime = now;
    }
  }

  /**
   * Removes the object from the cluster.
   */
  public void removeImpl()
  {
    ObjectManager objectManager = getObjectManager();
    if (objectManager != null && getObjectManagerKey() != null)
      objectManager.notifyRemove(getObjectManagerKey());

    _isDead = true; // XXX: ? 
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }
}
