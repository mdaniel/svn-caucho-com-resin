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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.util.*;
import com.caucho.vfs.*;
import com.caucho.log.Log;

/**
 * Application view of the store.
 */
public class Store {
  static protected final Logger log = Log.open(Store.class);
  static final L10N L = new L10N(Store.class);

  private StoreManager _storeManager;
  private ObjectManager _objectManager;
  private String _storeId;
  private long _maxIdleTime;

  /**
   * Creates the new application view of the store.
   *
   * @param storeId the application identifiers
   * @param objectManager the application's object manager, e.g. the SessionManager
   * @param storeManager the persistent store manater
   */
  Store(String storeId, StoreManager storeManager)
  {
    _storeId = mangleId(storeId);
    _storeManager = storeManager;

    _maxIdleTime = storeManager.getMaxIdleTime();
  }

  /**
   * Gets the store identifier.
   */
  public String getId()
  {
    return _storeId;
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
   * Returns a ClusterObject.
   */
  public ClusterObject createClusterObject(String objectId)
  {
    return _storeManager.createClusterObject(this, objectId);
  }
  
  /**
   * Updates the object's access time.
   *
   * @param obj the object to update.
   */
  public void access(String objectId)
    throws Exception
  {
    _storeManager.access(this, objectId);
  }

  /**
   * When the object is no longer valid, remove it from the backing store.
   *
   * @param key the object's id
   */
  public void remove(String objectId)
    throws Exception
  {
    _storeManager.remove(this, objectId);
  }

  /**
   * When the object is no longer valid, remove it from the backing store.
   *
   * @param key the object's id
   */
  void notifyRemove(String objectId)
    throws Exception
  {
    if (_objectManager != null)
      _objectManager.notifyRemove(objectId);
  }

  /**
   * Returns the mangled id.
   */
  static private String mangleId(String id)
  {
    StringBuilder cb = new StringBuilder();

    for (int i = 0; i < id.length(); i++) {
      char ch = id.charAt(i);

      if (ch == '/')
	cb.append("__");
      else if (ch == ':')
	cb.append("_0");
      else if (ch == '_')
	cb.append("_1");
      else
	cb.append(ch);
    }

    return cb.toString();
  }
}
