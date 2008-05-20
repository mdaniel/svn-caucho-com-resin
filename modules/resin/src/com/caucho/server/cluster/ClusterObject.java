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

import com.caucho.util.Alarm;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data for the cluster's object.
 */
public class ClusterObject {
  private static final Logger log
    = Logger.getLogger(ClusterObject.class.getName());

  private final HashKey _id;
  private final HashKey _storeId;
  
  private final StoreManager _storeManager;
  private final Store _store;

  private ObjectManager _objectManager;

  private boolean _isPrimary;
  private long _maxIdleTime;
  
  private long _expireInterval = -1;

  private long _accessTime;

  private long _crc = -1;

  private boolean _isSerializable = true;
  // true if the current data is valid and up to date
  private boolean _isValid = true;
  private boolean _isChanged = false;
  private boolean _isDead = false;

  ClusterObject(StoreManager storeManager,
		Store store,
		HashKey id)
  {
    _id = id;
    _storeId = store.getId();
    
    _storeManager = storeManager;
    _objectManager = store.getObjectManager();
    _store = store;
    _maxIdleTime = _store.getMaxIdleTime();

    _isPrimary = false; // XXX: isPrimary(_objectId);
    
    _expireInterval = getMaxIdleTime() + getAccessWindow();
  }

  ClusterObject(StoreManager storeManager,
		HashKey storeId,
		HashKey objectId)
  {
    _storeManager = storeManager;
    _objectManager = null;
    _store = null;

    _maxIdleTime = _storeManager.getMaxIdleTime();

    _storeId = storeId;
    _id = objectId;

    _isPrimary = false; // XXX: isPrimary(_objectId);

    _expireInterval = getMaxIdleTime() + getAccessWindow();
  }

  public void setObjectManager(ObjectManager objectManager)
  {
    _objectManager = objectManager;
  }

  // XXX: move to store manager?
  private boolean isPrimary(String id)
  {
    if (_store != null && _store.isAlwaysLoad())
      return false;
    
    else if (_store == null && _storeManager.isAlwaysLoad())
      return false;

    return _storeManager.isPrimary(id);
  }

  /**
   * Returns the store.
   */
  public Store getStore()
  {
    return _store;
  }

  /**
   * Returns the store manager.
   */
  public StoreManager getStoreManager()
  {
    return _storeManager;
  }

  /**
   * Returns the store id.
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
   * Returns the access window.
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
   * Sets true for the primary server.
   */
  public void setPrimary(boolean primary)
  {
    _isPrimary = primary;
  }

  /**
   * Returns the object's saved CRC value.
   */
  long getCRC()
  {
    return _crc;
  }

  /**
   * Sets the object's saved CRC value.
   */
  void setCRC(long crc)
  {
    _crc = crc;
  }

  /**
   * Returns true if the object has up-to-date loaded values
   */
  boolean isValid()
  {
    return _isValid;
  }

  /**
   * Sets the object's saved update count
   */
  void setValid(boolean isValid)
  {
    _isValid = isValid;
  }

  /**
   * Sets the object's saved update count
   */
  public void setValid()
  {
    _isValid = true;
  }
  
  /**
   * Returns the primary index for the object
   */
  public int getPrimaryIndex()
  {
    return 0;
  }
  
  /**
   * Returns the secondary server for the object
   */
  public int getSecondaryIndex()
  {
    return 0;
  }
  
  /**
   * Returns the tertiary server for the object
   */
  public int getTertiaryIndex()
  {
    return 0;
  }

   
  /**
   * Returns the primary server for the object
   */
  public ClusterServer getPrimaryServer()
  {
    return null;
  }
  
  /**
   * Returns the secondary server for the object
   */
  public ClusterServer getSecondaryServer()
  {
    return null;
  }
  
  /**
   * Returns the tertiary server for the object
   */
  public ClusterServer getTertiaryServer()
  {
    return null;
  }

  /**
   * Loads the object from the cluster.  If the object fails to load,
   * its contents may be in an inconsistent state.
   *
   * @return true on success.
   */
  public boolean load(Object obj)
  {
    if (! _isSerializable)
      return true;

    if (_isDead)
      return false;

    if (_isPrimary && _isValid)
      return true;

    try {
      if (_storeManager.load(this, obj)) {
	_isValid = true;
	
	return true;
      }
      else {
	_crc = -1;
	return false;
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
      _crc = -1;

      return false;
    } finally {
      _isChanged = false;
    }
  }

  /**
   * Loads the object, called from the store.
   */
  boolean load(InputStream is, Object obj)
    throws IOException
  {
    VfsStream streamImpl = new VfsStream(is, null);

    Crc64Stream crcStream = new Crc64Stream(streamImpl);

    ReadStream crcIs = new ReadStream(crcStream);

    _objectManager.load(crcIs, obj);

    _isValid = true;
    _crc = crcStream.getCRC();

    crcIs.close();

    return true;
  }

  /**
   * Signals that the object has been updated externally, i.e.
   * that the persistent store now has a more current version of
   * the object's data.
   */
  public void update()
  {
    _isValid = false;
  }

  /**
   * Signals that the object has been changed by the application, i.e.
   * that the object needs to be saved to the persistent store.
   */
  public void change()
  {
    _isChanged = true;
  }

  /**
   * Marks the object as accessed.
   */
  public void access()
  {
    long now = Alarm.getCurrentTime();

    if (getAccessWindow() <= now - _accessTime) {
      try {
	_storeManager.accessImpl(getObjectId());
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }

      _accessTime = now;
    }
  }

  /**
   * Sets the access time.
   */
  public void setAccessTime(long accessTime)
  {
    _accessTime = accessTime;
  }

  /**
   * Sets the max access time.
   */
  public long getExpireInterval()
  {
    return _expireInterval;
  }

  /**
   * Sets the max access time.
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
   * Saves the object to the cluster.
   */
  public void store(Object obj)
    throws IOException
  {
    if (! _isSerializable)
      return;

    boolean isValid = _isValid;

    if (! _isPrimary) {
      _isValid = false;
    }

    if (! _isChanged && ! _store.isAlwaysSave())
      return;

    _isChanged = false;

    TempStream tempStream = new TempStream();

    try {
      Crc64Stream crcStream = new Crc64Stream(tempStream);
      WriteStream os = new WriteStream(crcStream);

      _objectManager.store(os, obj);

      os.close();
      os = null;

      long crc = crcStream.getCRC();

      if (crc == _crc)
	return;

      _crc = crc;

      _storeManager.store(this, tempStream, crc);

      if (_isPrimary)
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

  /**
   * Removes the object from the cluster.
   */
  public void remove()
  {
    try {
      if (_isDead)
	return;
      _isDead = true;

      _storeManager.remove(this);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Removes the object from the cluster.
   */
  public void removeImpl()
  {
  }
}
