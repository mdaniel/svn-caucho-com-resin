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

import com.caucho.util.L10N;

import java.util.logging.Logger;

/**
 * Application view of the store, e.g. a specific session manager.
 */
public class Store {
  static protected final Logger log
    = Logger.getLogger(Store.class.getName());
  static final L10N L = new L10N(Store.class);

  private final HashKey _id;
  
  private StoreManager _storeManager;
  private ObjectManager _objectManager;
  private String _name;
  private long _maxIdleTime;

  private boolean _isAlwaysLoad;
  private boolean _isAlwaysSave;

  /**
   * Creates the new application view of the store.
   *
   * @param storeId the application identifiers
   * @param objectManager the application's object manager, e.g. the SessionManager
   * @param storeManager the persistent store manater
   */
  Store(HashKey id, StoreManager storeManager)
  {
    _id = id;
    _storeManager = storeManager;

    _maxIdleTime = storeManager.getMaxIdleTime();

    _isAlwaysLoad = _storeManager.isAlwaysLoad();
    _isAlwaysSave = _storeManager.isAlwaysSave();
  }

  /**
   * Gets the store identifier.
   */
  public HashKey getId()
  {
    return _id;
  }
  
  /**
   * Gets the store name
   */
  public String getName()
  {
    return _name;
  }
  
  /**
   * Sets the store name
   */
  public void setName(String name)
  {
    _name = name;
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

    _storeManager.updateIdleCheckInterval(maxIdleTime);
  }

  /**
   * Returns the length of time an idle object can remain in the store before
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

  /**
   * Returns true if the object should always be loaded.
   */
  public boolean isAlwaysLoad()
  {
    return _isAlwaysLoad;
  }

  /**
   * Set true if the object should always be loaded.
   */
  public void setAlwaysLoad(boolean isAlwaysLoad)
  {
    _isAlwaysLoad = isAlwaysLoad;
  }

  /**
   * Set true if the object should always be saved.
   */
  public void setAlwaysSave(boolean isAlwaysSave)
  {
    _isAlwaysSave = isAlwaysSave;
  }

  /**
   * Returns true if the object should always be saved.
   */
  public boolean isAlwaysSave()
  {
    return _isAlwaysSave;
  }

  /**
   * Returns the object manager.
   */
  public ObjectManager getObjectManager()
  {
    return _objectManager;
  }

  /**
   * Sets the object manager.
   */
  public void setObjectManager(ObjectManager obj)
  {
    _objectManager = obj;
  }

  /**
   * Returns the store manager.
   */
  public StoreManager getStoreManager()
  {
    return _storeManager;
  }

  /**
   * Returns a ClusterObject.
   */
  public ClusterObject createClusterObject(String objectId,
                                           int primary,
                                           int secondary,
                                           int tertiary)
  {
    return _storeManager.createClusterObject(this, objectId,
                                             primary, secondary, tertiary);
  }
}
