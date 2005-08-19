/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

import com.caucho.log.Log;
import com.caucho.util.Alarm;
import com.caucho.vfs.*;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Data for the cluster's object.
 */
public class ClusterObject {
  private static final Logger log = Log.open(ClusterObject.class);

  private final StoreManager _storeManager;
  private final Store _store;
  private final String _storeId;
  private final ObjectManager _objectManager;
  private final String _objectId;

  private final String _uniqueId;

  private boolean _isPrimary;
  private long _maxIdleTime;

  private long _accessTime;

  private long _crc = -1;
  private int _updateCount = -1;

  private boolean _isSerializable = true;
  private boolean _isChanged = false;
  private boolean _isDead = false;

  ClusterObject(StoreManager storeManager,
		Store store,
		String objectId)
  {
    _storeManager = storeManager;
    _objectManager = store.getObjectManager();
    _store = store;
    _maxIdleTime = _store.getMaxIdleTime();

    _storeId = store.getId();
    _objectId = objectId;
    _uniqueId = _storeId + ';' + objectId;

    _isPrimary = isPrimary(_objectId);
  }

  ClusterObject(StoreManager storeManager,
		String storeId,
		String objectId)
  {
    _storeManager = storeManager;
    _objectManager = null;
    _store = null;

    _maxIdleTime = _storeManager.getMaxIdleTime();

    _storeId = storeId;
    _objectId = objectId;
    _uniqueId = _storeId + ';' + objectId;

    _isPrimary = isPrimary(_objectId);
  }

  private boolean isPrimary(String id)
  {
    Cluster cluster = Cluster.getLocal();

    if (cluster == null)
      return ! _storeManager.isAlwaysLoad();
      
    ClusterServer selfServer = cluster.getSelfServer();

    if (selfServer == null)
      return ! _storeManager.isAlwaysLoad();
    else
      return _storeManager.getOwnerIndex(id) == selfServer.getIndex();
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
  public String getStoreId()
  {
    return _storeId;
  }

  /**
   * Returns the object id.
   */
  public String getObjectId()
  {
    return _objectId;
  }

  /**
   * Returns the unique id.
   */
  public String getUniqueId()
  {
    return _uniqueId;
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
   * Returns the expire time.
   */
  public long getExpireInterval()
  {
    return getMaxIdleTime() + getAccessWindow();
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
   * Returns the object's update count
   */
  int getUpdateCount()
  {
    return _updateCount;
  }

  /**
   * Sets the object's saved update count
   */
  void setUpdateCount(int count)
  {
    _updateCount = count;
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
      throw new IllegalStateException();

    if (_isPrimary && _updateCount >= 0)
      return true;

    try {
      if (_storeManager.load(this, obj)) {
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

    ObjectInputStream in = new DistributedObjectInputStream(crcIs);

    _objectManager.load(in, obj);

    _crc = crcStream.getCRC();

    in.close();
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
    _updateCount = -1;
  }

  /**
   * Signals that the object has been changed by the application, i.e.
   * that the object needs to be saved to the persistent store.
   */
  public void change()
  {
    if (! _isChanged)
      _updateCount++;

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
	_storeManager.accessImpl(getUniqueId());
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
   * Saves the object to the cluster.
   */
  public void store(Object obj)
    throws IOException
  {
    if (! _isSerializable)
      return;

    int updateCount = _updateCount;

    if (! _isPrimary)
      _updateCount = -1;

    if (! _isChanged && ! _storeManager.isAlwaysSave())
      return;

    _isChanged = false;

    TempStream tempStream = new TempStream(null);
    Crc64Stream crcStream = new Crc64Stream(tempStream);

    try {
      WriteStream os = new WriteStream(crcStream);

      ObjectOutputStream out = new ObjectOutputStream(os);

      _objectManager.store(out, obj);

      out.flush();
      long crc = crcStream.getCRC();

      out.close();

      os.close();
      os = null;

      if (crc == _crc)
	return;

      _crc = crc;
      updateCount++;

      _storeManager.store(this, tempStream, crc, updateCount);

      if (_isPrimary)
	_updateCount = updateCount;

      _accessTime = Alarm.getCurrentTime();
    } catch (NotSerializableException e) {
      log.warning(e.toString());
      _isSerializable = false;
    } catch (Throwable e) {
      log.warning(e.toString());
      log.log(Level.FINE, e.toString(), e);

      _updateCount = -1;
    } finally {
      tempStream.destroy();
    }
  }

  /**
   * Writes updated values
   */
  public void write(InputStream is)
    throws IOException
  {
  }

  /**
   * Reads the current value
   */
  public ReadStream openRead()
    throws IOException
  {
    return null;
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

  static class DistributedObjectInputStream extends ObjectInputStream {
    DistributedObjectInputStream(InputStream is)
      throws IOException
    {
      super(is);
    }

    protected Class resolveClass(ObjectStreamClass v)
      throws IOException, ClassNotFoundException
    {
      String name = v.getName();

      Thread thread = Thread.currentThread();
      ClassLoader loader = thread.getContextClassLoader();

      return Class.forName(name, false, loader);
    }
  }
}
