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
 */

package com.caucho.ejb.protocol;

import java.io.Serializable;
import java.io.ObjectStreamException;

import java.rmi.RemoteException;
import java.rmi.NoSuchObjectException;

import javax.ejb.Handle;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.FinderException;
import javax.ejb.RemoveException;

import com.caucho.util.L10N;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.AbstractEJBObject;
import com.caucho.ejb.AbstractContext;
import com.caucho.ejb.EjbServerManager;
import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.ejb.NoSuchObjectExceptionWrapper;

/**
 * Interface for stubs inside the same JVM.
 */
public abstract class JVMObject extends AbstractEJBObject
  implements EJBObject, Serializable {
  protected static final L10N L = new L10N(JVMObject.class);

  protected Object _primaryKey;
  protected ObjectSkeletonWrapper _skeletonWrapper;

  protected AbstractServer _server;
  protected Object _object;

  /**
   * Initialization code.
   */
  public void _init(AbstractServer server, Object primaryKey)
  {
    if (primaryKey == null)
      throw new NullPointerException();
    
    _server = server;
    _primaryKey = primaryKey;
  }
  
  /**
   * Returns the implemented class.
   */
  public Class getAPIClass()
  {
    return getServer().getRemoteObjectClass();
  }
  
  /**
   * Returns the URL for the given protocol.
   */
  public String getURL(String protocol)
  {
    HandleEncoder encoder = getServer().getHandleEncoder(protocol);

    return encoder.getURL(getServer().encodeId(_primaryKey));
  }

  /**
   * Returns the serializable handle for the remote object
   */
  public Handle getHandle()
    throws RemoteException
  {
    HandleEncoder encoder = getServer().getHandleEncoder();

    return encoder.createHandle(getServer().encodeId(_primaryKey));
  }

  /**
   * Returns the EJBHome stub for the remote object.
   */
  public EJBHome getEJBHome()
    throws RemoteException
  {
    try {
      return getServer().getEJBHome();
    } catch (Exception e) {
      throw new RemoteExceptionWrapper(e);
    }
  }

  /**
   * Returns the underlying object
   */
  protected Object _caucho_getObject()
    throws RemoteException
  {
    if (_server == null || _server.isDead() || _object == null) {
      try {
        AbstractContext context;
        context = getServer().getContext(_primaryKey);

        if (context != null)
          _object = context.getRemoteView();
      } catch (FinderException e) {
        throw new NoSuchObjectExceptionWrapper(e);
      }

      if (_object == null)
        throw new NoSuchObjectException(L.l("`{0}' is not a valid bean.  The bean may have been deleted or the server moved.", _primaryKey));

      _caucho_init_methods(_object.getClass());
    }

    return _object;
  }

  protected void _caucho_init_methods(Class cl)
  {
  }

  /**
   * Returns the server's class loader
   */
  protected ClassLoader _caucho_getClassLoader()
    throws RemoteException
  {
    ClassLoader loader = getServer().getClassLoader();

    return loader;
  }

  /**
   * Returns the currently active server.  The server can change over time
   * because of class reloading.
   */
  private AbstractServer getServer()
  {
    if (_server.isDead()) {
      String serverId = _server.getServerId();
      _object = null;
      _server = EjbProtocolManager.getJVMServer(serverId);
    }

    return _server;
  }

  /**
   * Returns true if the test object is identical to this object.
   *
   * @param obj the object to test for identity
   */
  public boolean isIdentical(EJBObject obj)
    throws RemoteException
  {
    return getHandle().equals(obj.getHandle());
  }

  public void remove()
    throws RemoveException, RemoteException
  {
  }

  public Object getPrimaryKey()
    throws RemoteException
  {
    return _primaryKey;
  }

  /**
   * Serialize the HomeSkeletonWrapper in place of this object.
   *
   * @return the matching skeleton wrapper.
   */
  public Object writeReplace() throws ObjectStreamException
  {
    if (_skeletonWrapper == null) {
      try {
        _skeletonWrapper = new ObjectSkeletonWrapper(getHandle());
      } catch (Exception e) {
      }
    }
    
    return _skeletonWrapper;
  }

  //
  // Static utility methods.
  //
  
  /**
   * Convert the object to an boolean.
   */
  public static boolean to_boolean(Object o)
  {
    if (o == null)
      return false;
    else
      return ((Boolean) o).booleanValue();
  }
  
  /**
   * Convert the object to an byte.
   */
  public static byte to_byte(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Byte) o).byteValue();
  }
  
  /**
   * Convert the object to an short.
   */
  public static short to_short(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Short) o).shortValue();
  }
  
  /**
   * Convert the object to an char.
   */
  public static char to_char(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Character) o).charValue();
  }
  
  /**
   * Convert the object to an int.
   */
  public static int to_int(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Integer) o).intValue();
  }
  
  /**
   * Convert the object to an long.
   */
  public static long to_long(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Long) o).longValue();
  }
  
  /**
   * Convert the object to an float.
   */
  public static float to_float(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Float) o).floatValue();
  }
  
  /**
   * Convert the object to an double.
   */
  public static double to_double(Object o)
  {
    if (o == null)
      return 0;
    else
      return ((Double) o).doubleValue();
  }
}
