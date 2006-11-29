/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import com.caucho.util.LruCache;

import java.util.Hashtable;

/**
 * Manages the object backing for a context.
 */
public class BackingManager {
  private Hashtable<String,BackingContext> _contextMap =
    new Hashtable<String,BackingContext>();
  
  private LruCache<BackingKey,ObjectBacking> _backingCache =
    new LruCache<BackingKey,ObjectBacking>(1024);
  
  private BackingKey _key = new BackingKey();

  /**
   * Returns the object backing for a given contextId, objectId pair.
   */
  public ObjectBacking getBacking(String contextId, String objectId)
  {
    synchronized (this) {
      _key.init(contextId, objectId);
      
      ObjectBacking backing = _backingCache.get(_key);

      if (backing != null)
        return backing;

      BackingContext context = getContext(contextId);
      
      if (context == null)
        return null;
      
      backing = context.getBacking(objectId);

      _backingCache.put(new BackingKey(contextId, objectId), backing);

      return backing;
    }
  }

  /**
   * Returns the named backing context.
   */
  public BackingContext getContext(String contextId)
  {
    return _contextMap.get(contextId);
  }
  
  /**
   * Sets the named backing context.
   */
  public void setContext(String contextId, BackingContext context)
  {
    _contextMap.put(contextId, context);
  }
  
  /**
   * Removes the named backing context.
   */
  public void removeContext(String contextId)
  {
    _contextMap.remove(contextId);
  }

  static class BackingKey {
    private String _contextId;
    private String _objectId;

    BackingKey()
    {
    }
    
    BackingKey(String contextId, String objectId)
    {
      _contextId = contextId;
      _objectId = objectId;
    }

    void init(String contextId, String objectId)
    {
      _contextId = contextId;
      _objectId = objectId;
    }

    public int hashCode()
    {
      return _contextId.hashCode() * 65521 + _objectId.hashCode();
    }

    public boolean equals(Object o)
    {
      if (! (o instanceof BackingKey))
        return false;

      BackingKey key = (BackingKey) o;

      return (_contextId.equals(key._contextId) &&
              _objectId.equals(key._objectId));
    }
  }
}
