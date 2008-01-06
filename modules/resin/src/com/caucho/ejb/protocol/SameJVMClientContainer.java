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

package com.caucho.ejb.protocol;

import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.RemoteExceptionWrapper;
import com.caucho.ejb.gen.JVMHomeStubGenerator;
import com.caucho.ejb.gen.JVMObjectStubGenerator;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;

import javax.ejb.EJBException;
import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import javax.ejb.Handle;
import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Hashtable;

/**
 * Container for EJB clients in the same JVM, but not the same class loader.
 */
public class SameJVMClientContainer {
  protected static final L10N L = new L10N(SameJVMClientContainer.class);
  
  private static final EnvironmentLocal<Hashtable<String,SameJVMClientContainer>> _jvmClient =
    new EnvironmentLocal<Hashtable<String,SameJVMClientContainer>>("caucho.jvm.client");
  
  String _serverId;
  // The server container in the JVM
  AbstractServer _server;
  
  // the home stub
  EJBHome _ejbHome;
  
  // the home stub class
  Class _homeStubClass;
  // the remote stub class
  Class _remoteStubClass;

  /**
   * Creates a client container for same-JVM connections.
   *
   * @param serverId the server id
   */
  public SameJVMClientContainer(AbstractServer server, String serverId)
  {
    _serverId = serverId;
    _server = server;

    Class beanSkelClass = server.getBeanSkelClass();

    Class cl = server.getHomeStubClass();

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      if (cl != null)
        _homeStubClass = Class.forName(cl.getName(), false, loader);
    } catch (ClassNotFoundException e) {
    }
    
    cl = server.getRemoteStubClass();

    try {
      if (cl != null)
        _remoteStubClass = Class.forName(cl.getName(), false, loader);
    } catch (ClassNotFoundException e) {
    }
  }

  public static SameJVMClientContainer find(String serverId)
  {
    try {
      Hashtable<String,SameJVMClientContainer> map;
      map = _jvmClient.getLevel();
      SameJVMClientContainer client = null;

      if (map != null)
        client = (SameJVMClientContainer) map.get(serverId);

      // sync doesn't matter since it's okay to load a dup
      if (client == null) {
        AbstractServer server = EjbProtocolManager.getJVMServer(serverId);

        if (server != null) {
          client = new SameJVMClientContainer(server, serverId);
          if (map == null)
            map = new Hashtable<String,SameJVMClientContainer>();
          map.put(serverId, client);
          _jvmClient.set(map);
        }
      }
      
      return client;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Returns the object key from a handle.
   */
  public Class getPrimaryKeyClass()
  {
    return _server.getPrimaryKeyClass();
  }

  public EJBHome getEJBHome()
    throws RemoteException
  {
    try {
      return createHomeStub();
    } catch (Exception e) {
      throw RemoteExceptionWrapper.create(e);
    }
  }

  /**
   * Finds the stub corresponding to the given URL.
   *
   * @param handle the handle for the bean's home
   *
   * @return the bean's home stub
   */
  public EJBHome createHomeStub()
    throws Exception
  {
    if (_homeStubClass == null && _server.getRemoteHomeClass() != null) {
      Class cl = _server.getRemoteHomeClass();
      Class remoteHomeClass = null;
      try {
        if (cl != null)
          remoteHomeClass = CauchoSystem.loadClass(cl.getName(), false, null);
      } catch (ClassNotFoundException e) {
      }

      if (remoteHomeClass == null) {
      }
      else if (remoteHomeClass.equals(_server.getRemoteHomeClass())) {
        _homeStubClass = _server.getHomeStubClass();
      }
      else {
        JVMHomeStubGenerator gen;
        gen = new JVMHomeStubGenerator(remoteHomeClass, true);
        gen.setClassDir(CauchoSystem.getWorkPath());
        _homeStubClass = gen.generateStub();
      }
    }
    
    if (_homeStubClass == null)
      throw new EJBException(L.l("`{0}' is a local bean with no remote interface.  Local beans must be looked up with a local context.",
                                 _serverId));

    JVMHome homeStub = (JVMHome) _homeStubClass.newInstance();

    homeStub._init(_server);

    return homeStub;
  }

  /**
   * Finds the stub for the remote object for the given Handle.
   *
   * @param handle the handle for the remote object
   *
   * @return the bean's remote stub
   */
  public EJBObject createObjectStub(Object primaryKey)
    throws Exception
  {
    AbstractServer server = getServer();
    
    if (_remoteStubClass == null && _server.getRemoteObjectClass() != null) {
      Class cl = _server.getRemoteObjectClass();
      Class remoteObjectClass = null;
      try {
        if (cl != null)
          remoteObjectClass = CauchoSystem.loadClass(cl.getName(), false, null);
      } catch (ClassNotFoundException e) {
      }

      if (remoteObjectClass == null) {
      } else if (remoteObjectClass.equals(_server.getRemoteObjectClass())) {
        _remoteStubClass = server.getRemoteStubClass();
      }
      else {
        JVMObjectStubGenerator gen;
        gen = new JVMObjectStubGenerator(remoteObjectClass, true);
        gen.setClassDir(CauchoSystem.getWorkPath());
        _remoteStubClass = gen.generateStub();
      }
    }
    
    if (_remoteStubClass == null)
      throw new EJBException(L.l("`{0}' is a local bean with no remote interface.  Local beans must be looked up with a local context.",
                                 _serverId));

    JVMObject objStub = (JVMObject) _remoteStubClass.newInstance();

    objStub._init(server, primaryKey);
    
    return objStub;
  }

  public HandleEncoder getHandleEncoder(AbstractHandle handle)
  {
    try {
      AbstractServer server = getServer();

      if (server != null)
        return server.getHandleEncoder();
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }

    throw new EJBException("can't find server " + handle);
  }
  

  /**
   * Returns the current server.
   */
  public AbstractServer getServer()
    throws RemoteException
  {
    AbstractServer jvmServer = EjbProtocolManager.getJVMServer(_serverId);

    if (jvmServer != null)
      return jvmServer;
    else
      throw new NoSuchObjectException(_serverId);
  }

  public EJBObject getObject(Handle handle)
  {
    return null;
  }

  /**
   * Returns a printable version of the client container
   */
  public String toString()
  {
    return "SameJVMClientContainer[" + _serverId + "]";
  }
}
