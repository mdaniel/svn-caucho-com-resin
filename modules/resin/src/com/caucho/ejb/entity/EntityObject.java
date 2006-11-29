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

package com.caucho.ejb.entity;

import com.caucho.ejb.AbstractEJBObject;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.xa.TransactionContext;
import com.caucho.util.Log;

import javax.ejb.EJBHome;
import javax.ejb.EJBLocalHome;
import javax.ejb.EJBLocalObject;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract base class for an EntityObject.
 */
abstract public class EntityObject extends AbstractEJBObject
  implements Serializable, EJBLocalObject
{
  protected static final Logger log = Log.open(EntityObject.class);

  public static final byte _CAUCHO_IS_DEAD = QEntity._CAUCHO_IS_DEAD;
  public static final byte _CAUCHO_IS_ACTIVE = QEntity._CAUCHO_IS_ACTIVE;
  public static final byte _CAUCHO_IS_LOADED = QEntity._CAUCHO_IS_LOADED;
  public static final byte _CAUCHO_IS_DIRTY = QEntity._CAUCHO_IS_DIRTY;

  protected abstract QEntityContext getEntityContext();

  /**
   * Returns the Entity bean's primary key
   */
  public Object getPrimaryKey()
  {
    return getEntityContext().getPrimaryKey();
  }
  
  public Object getEJBObject()
  {
    return getEntityContext().getEJBObject();
  }
  
  public Object getEJBLocalObject()
  {
    return getEntityContext().getEJBLocalObject();
  }

  /**
   * Returns the server.
   */
  public EntityServer _caucho_getEntityServer()
  {
    return getEntityContext().getEntityServer();
  }

  /**
   * Return if matching context.
   */
  public boolean isMatch(EntityServer server, Object key)
  {
    return server == _caucho_getEntityServer() && key.equals(getPrimaryKey());
  }

  /**
   * Returns the handle.
   */
  public Handle getHandle()
  {
    return getEntityContext().getHandle();
  }
  
  /**
   * Returns an underlying bean
   */
  public Object _caucho_getBean(TransactionContext trans, boolean doLoad)
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns an underlying bean
   */
  public Object _caucho_getBean()
  {
    throw new UnsupportedOperationException();
  }

  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBHome getEJBHome()
  {
    try {
      return _caucho_getEntityServer().getEJBHome();
    } catch (Exception e) {
      return null;
    }
  }
  
  /**
   * Returns the EJBHome stub for the container.
   */
  public EJBLocalHome getEJBLocalHome()
  {
    try {
      return (EJBLocalHome) _caucho_getEntityServer().getEJBLocalHome();
    } catch (Exception e) {
      return null;
    }
  }

  /**
   * Returns true if the two objects are identical.
   */
  public boolean isIdentical(EJBObject obj) throws RemoteException
  {
    return getHandle().equals(obj.getHandle());
  }

  /**
   * Returns true if the two objects are identical.
   */
  public boolean isIdentical(EJBLocalObject o)
  {
    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    EntityObject obj = (EntityObject) o;

    try {
      Object key = getPrimaryKey();
      Object objKey = obj.getPrimaryKey();
      
      if (key != null)
        return key.equals(objKey);
      else
        return objKey == null;
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Remove the object.
   */
  /*
  public void remove() throws RemoveException
  {
    _caucho_getServer().remove(getHandle());
  }
  */

  /**
   * Returns the server.
   */
  public AbstractServer __caucho_getServer()
  {
    return _caucho_getEntityServer();
  }

  /**
   * The home id is null.
   */
  public String __caucho_getId()
  {
    return _caucho_getEntityServer().encodeId(getPrimaryKey());
  }

  /**
   * Returns a hash code for the object.
   */
  public int hashCode()
  {
    try {
      Object key = getPrimaryKey();
      
      if (key != null)
        return key.hashCode();
      else
        return 0;
    } catch (Throwable e) {
      return 0;
    }
  }

  /**
   * Returns true if this object equals the test object.
   */

  public boolean equals(Object o)
  {
    if (this == o)
      return true;

    if (o == null || ! getClass().equals(o.getClass()))
      return false;

    EntityObject obj = (EntityObject) o;

    try {
      Object key = getPrimaryKey();
      Object objKey = obj.getPrimaryKey();
      
      if (key != null)
        return key.equals(objKey);
      else
        return objKey == null;
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
      
      return false;
    }
  }

  /**
   * Returns the string value.
   */
  public String toString()
  {
    Object key = "";
    try {
      key = getPrimaryKey();
    } catch (Throwable e) {
    }

    return getClass().getName() + "[" + key + "]";
  }
}
