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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.entity;

import com.caucho.amber.entity.EntityItem;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.EjbTransactionManager;

import javax.ejb.EJBObject;
import javax.ejb.EntityContext;
import javax.ejb.FinderException;
import javax.ejb.Handle;
import javax.ejb.HomeHandle;
import javax.ejb.RemoveException;
import javax.transaction.UserTransaction;
import java.util.logging.Level;

/**
 * Abstract base class for an EntityHome.
 */
public abstract class QEntityContext extends AbstractContext
  implements EntityContext {
  protected final EntityServer _server;
  public Object _primaryKey;
  
  private Object _remoteView;
  private EJBObject _remote;

  protected QEntityContext(EntityServer server)
  {
    _server = server;
  }

  /**
   * Returns the owning server.
   */
  public AbstractServer getServer()
  {
    return _server;
  }

  /**
   * Returns the owning server.
   */
  public EntityServer getEntityServer()
  {
    return _server;
  }

  /**
   * Returns the transaction manager.
   */
  public EjbTransactionManager getTransactionManager()
  {
    return getEntityServer().getTransactionManager();
  }

  /**
   * Returns the home handle for the home bean.
   */
  public HomeHandle getHomeHandle()
  {
    return getServer().getHomeHandle();
  }

  public void setPrimaryKey(Object key)
  {
    _primaryKey = key;
  }

  /**
   * Returns null, since primary key isn't available to the home.
   */
  public Object getPrimaryKey()
    throws IllegalStateException
  {
    if (_primaryKey == null) {
      throw new IllegalStateException("Can't get primary key.  The context may belong to a home instance or it may be called before ejbActivate or ejbCreate.");
    }
    else
      return _primaryKey;
  }

  /**
   * Returns null, since primary key isn't available to the home.
   */
  public Object _caucho_getPrimaryKey()
  {
    return _primaryKey;
  }

  /**
   * Returns the current UserTransaction.  Only Session beans with
   * bean-managed transactions may use this.
   */
  public UserTransaction getUserTransaction()
    throws IllegalStateException
  {
    throw new IllegalStateException("Entity beans may not call getUserTransaction()");
  }

  /**
   * Returns the object's handle.
   */
  public Handle getHandle()
  {
    return getEntityServer().createHandle(_primaryKey);
  }

  /**
   * Returns null, since the ejb object isn't available to the home.
   */
  public EJBObject getEJBObject()
    throws IllegalStateException
  {
    /*
    if (_remote == null)
      _remote = getEntityServer().createEJBObject(getPrimaryKey());
    
    return _remote;
    */
    return getRemoteView();
  }

  /**
   * Finish any caching for the entity.
   */
  public void postCreate(Object primaryKey)
  {
    _primaryKey = primaryKey;

    getEntityServer().postCreate(primaryKey, this);
  }

  /**
   * Remove the object specified by the handle.
   */
  public void remove(Handle handle)
  {
    getServer().remove(handle);
  }
  
  /**
   * Remove the object specified by the primary key.
   */
  public void remove(Object primaryKey)
  {
    getServer().remove(primaryKey);
  }

  public void _caucho_remove_callback(Class listenClass, Object primaryKey)
    throws RemoveException
  {
  }

  /**
   * Invalidate the cache the entity server.
   */
  public void invalidateHomeCache()
  {
    getServer().invalidateCache();
  }

  /**
   * Update the context.
   */
  public void update()
  {
  }

  /**
   * Sets the Amber entity item.
   */
  protected void __caucho_setAmber(EntityItem item)
  {
  }

  public abstract void _caucho_load() throws FinderException;
  
  /**
   * Callback when removed from the cache.
   */
  public void removeEvent()
  {
    try {
      destroy();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Destroy the context.
   */
  public void destroy() throws Exception
  {
    super.destroy();
    
    _remote = null;
    _remoteView = null;
  }
}
