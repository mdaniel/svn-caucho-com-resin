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

package com.caucho.ejb.burlap;

import com.caucho.burlap.io.BurlapRemoteResolver;
import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import java.io.IOException;
import java.util.Hashtable;

/**
 * Container for Burlap clients in the same JVM, but not the same
 * class loader.
 */
class BurlapClientContainer implements BurlapRemoteResolver {
  protected static L10N L = new L10N(BurlapClientContainer.class);

  private static EnvironmentLocal burlapClient =
  new EnvironmentLocal("caucho.burlap.client");
  
  private String serverId;
  private BurlapHandleEncoder handleEncoder;
  // the home stub
  EJBHome ejbHome;

  Class homeClass;
  Class remoteClass;
  // the home stub class
  Class homeStubClass;
  // the remote stub class
  Class remoteStubClass;
  // the primary key class
  Class primaryKeyClass;

  private String _basicAuth;

  /**
   * Creates a client container for same-JVM connections.
   *
   * @param serverId the server id
   */
  BurlapClientContainer(String serverId)
    throws ConfigException
  {
    this.serverId = serverId;

    remoteStubClass = getRemoteStubClass();
    homeStubClass = getHomeStubClass();
  }

  static BurlapClientContainer find(String serverId)
  {
    try {
      Hashtable map = (Hashtable) burlapClient.getLevel();
      BurlapClientContainer client = null;

      if (map != null)
        client = (BurlapClientContainer) map.get(serverId);

      // sync doesn't matter since it's okay to load a dup
      if (client == null) {
        client = new BurlapClientContainer(serverId);
        if (map == null)
          map = new Hashtable();
        map.put(serverId, client);
        burlapClient.set(map);
      }
      
      return client;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Finds the stub corresponding to the given URL.
   *
   * @return the bean's home stub
   */
  protected EJBHome getHomeStub()
    throws ConfigException
  {
    try {
      HomeStub homeStub = (HomeStub) homeStubClass.newInstance();

      homeStub._init(serverId, this);

      return homeStub;
    } catch (IllegalAccessException e) {
      throw ConfigException.create(e);
    } catch (InstantiationException e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Finds the stub for the remote object for the given Handle.
   *
   * @param handle the handle for the remote object
   *
   * @return the bean's remote stub
   */
  protected EJBObject createObjectStub(String url)
    throws ConfigException
  {
    try {
      ObjectStub objStub = (ObjectStub) remoteStubClass.newInstance();
      objStub._init(url, this);
      return objStub;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  BurlapHomeHandle getHomeHandle()
  {
    return new BurlapHomeHandle(ejbHome, serverId);
  }

  BurlapHandle createHandle(String url)
  {
    return new BurlapHandle(url);
  }
  
  public HandleEncoder getHandleEncoder()
  {
    try {
      if (handleEncoder == null)
        handleEncoder = new BurlapHandleEncoder(null, serverId,
                                                 getPrimaryKeyClass());

      return handleEncoder;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }

  /**
   * Returns the bean's home stub class, creating it if necessary
   */
  Class getHomeStubClass()
    throws ConfigException
  {
    if (homeStubClass != null)
      return homeStubClass;

    synchronized (this) {
      if (homeStubClass != null)
        return homeStubClass;

      StubGenerator gen = new StubGenerator();
      
      homeStubClass = gen.createHomeStub(getHomeClass());
    }

    return homeStubClass;
  }

  /**
   * Returns the bean's remote stub class, creating it if necessary
   */
  Class getRemoteStubClass()
    throws ConfigException
  {
    if (remoteStubClass != null)
      return remoteStubClass;

    synchronized (this) {
      if (remoteStubClass != null)
        return remoteStubClass;

      Class remoteClass = getRemoteClass();
      if (remoteClass == null)
        return null;

      StubGenerator gen = new StubGenerator();

      remoteStubClass = gen.createObjectStub(remoteClass);
    }

    return remoteStubClass;
  }
  
  /**
   * Returns the bean's home interface class.  If unknown, call the server
   * for the class name.
   */
  Class getHomeClass()
    throws ConfigException
  {
    if (homeClass != null)
      return homeClass;

    try {
      synchronized (this) {
        if (homeClass != null)
          return homeClass;

        String className = getHomeClassName();

        homeClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
    
    return homeClass;
  }

  /**
   * Returns the classname of the home interface.
   */
  String getHomeClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(serverId);

    if (server != null) {
      Class cl = server.getRemoteHomeClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("`{0}' has no remote interface.",
                                      serverId));
    }
        
    try {
      Path path = Vfs.lookup(serverId);

      return (String) MetaStub.call(path, "_burlap_getAttribute", "java.home.class");
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Returns the bean's remote interface class.  If unknown, call the server
   * for the class name.
   */
  Class getRemoteClass()
    throws ConfigException
  {
    if (remoteClass != null)
      return remoteClass;

    try {
      synchronized (this) {
        if (remoteClass != null)
          return remoteClass;

        String className = getRemoteClassName();

        if (className == null || className.equals("null"))
          return null;

        remoteClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }

    return remoteClass;
  }

  /**
   * Returns the classname of the remote interface.
   */
  String getRemoteClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(serverId);

    if (server != null) {
      Class cl = server.getRemoteObjectClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("`{0}' has no remote interface.",
                                      serverId));
    }
    
    try {
      Path path = Vfs.lookup(serverId);

      return (String) MetaStub.call(path, "_burlap_getAttribute",
                                    "java.object.class");
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Returns the bean's primary key class.  If unknown, call the server
   * for the class name.
   */
  Class getPrimaryKeyClass()
    throws ConfigException
  {
    if (primaryKeyClass != null)
      return primaryKeyClass;

    try {
      synchronized (this) {
        if (primaryKeyClass != null)
          return primaryKeyClass;

        String className = getPrimaryKeyClassName();

        primaryKeyClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
    
    return primaryKeyClass;
  }

  /**
   * Returns the classname of the home interface.
   */
  String getPrimaryKeyClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(serverId);

    if (server != null) {
      Class cl = server.getPrimaryKeyClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("`{0}' has no remote interface.",
                                      serverId));
    }
        
    try {
      Path path = Vfs.lookup(serverId);

      return (String) MetaStub.call(path, "_burlap_getAttribute", "primary-key-class");
    } catch (Throwable e) {
      throw ConfigException.create(e);
    }
  }
  
  /**
   * Looks up a proxy object.
   */
  public Object lookup(String type, String url)
    throws IOException
  {
    try {
      Class api = CauchoSystem.loadClass(type);

      return create(api, url);
    } catch (Exception e) {
      throw new IOException(String.valueOf(e));
    }
  }

  /**
   * Creates a new proxy with the specified URL.  The returned object
   * is a proxy with the interface specified by api.
   *
   * <pre>
   * String url = "http://localhost:8080/ejb/hello");
   * HelloHome hello = (HelloHome) factory.create(HelloHome.class, url);
   * </pre>
   *
   * @param api the interface the proxy class needs to implement
   * @param url the URL where the client object is located.
   *
   * @return a proxy to the object with the specified interface.
   */
  public Object create(Class api, String url)
    throws Exception
  {
    StubGenerator gen = new StubGenerator();
    gen.setClassDir(CauchoSystem.getWorkPath());
      
    Class cl = gen.createStub(api);

    BurlapStub stub = (BurlapStub) cl.newInstance();

    stub._init(url, this);

    return stub;
  }

  /**
   * Returns the basic auth.
   */
  String getBasicAuthentication()
  {
    return _basicAuth;
  }

  /**
   * Sets the basic auth.
   */
  void setBasicAuthentication(String auth)
  {
    if (auth != null)
      _basicAuth = "Basic " + auth;
    else
      _basicAuth = auth;
  }

  /**
   * Returns a printable version of the client container
   */
  public String toString()
  {
    return "BurlapClientContainer[" + serverId + "]";
  }
}
