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

package com.caucho.quercus.env;

import com.caucho.quercus.QuercusModuleException;
import com.caucho.quercus.lib.UnserializeReader;
import com.caucho.server.cluster.ClusterObject;
import com.caucho.util.CacheListener;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the $_SESSION
 */
public class ResinSessionArrayValue extends SessionArrayValue
{
  static protected final Logger log
    = Logger.getLogger(ResinSessionArrayValue.class.getName());

  private ClusterObject _clusterObject;

  public ResinSessionArrayValue(String id, long now, 
				long maxInactiveInterval)
  {
    super(id, now, maxInactiveInterval);
  }
  
  public ResinSessionArrayValue(String id, long now,
				long maxInactiveInterval, ArrayValue array)
  {
    super(id, now, maxInactiveInterval, array);
  }

  public void setClusterObject(ClusterObject obj)
  {
    _clusterObject = obj;
  }

  /**
   * Copy for serialization
   */
  @Override
  public Value copy(Env env, IdentityHashMap<Value,Value> map)
  {
    long accessTime = _accessTime;

    ResinSessionArrayValue copy = 
      new ResinSessionArrayValue(getId(), accessTime, getMaxInactiveInterval(),
				 (ArrayValue) getArray().copy(env, map));

    copy.setClusterObject(_clusterObject);

    return copy;
  }

  @Override
  public boolean load()
  {
    if (_clusterObject != null)
      return _clusterObject.objectLoad(this);
    else
      return true;
  }

  protected void store()
  {
    try {
      ClusterObject clusterObject = _clusterObject;

      if (clusterObject != null) {
        // make sure the object always saves - PHP references can make changes
        // without directly calling on the session object
        clusterObject.objectModified(); 

        clusterObject.objectStore(this);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, "Can't serialize session", e);
    }
  }

  /**
   * Invalidates the session.
   */
  @Override
  protected void remove()
  {
    ClusterObject clusterObject = _clusterObject;
    _clusterObject = null;

    if (clusterObject != null)
      clusterObject.objectRemove();
  }
}
