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

package com.caucho.ejb.hessian;

import com.caucho.hessian.client.*;
import com.caucho.hessian.io.HessianRemoteResolver;
import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.EJBExceptionWrapper;
import com.caucho.ejb.protocol.EjbProtocolManager;
import com.caucho.ejb.protocol.HandleEncoder;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.ejb.EJBHome;
import javax.ejb.EJBObject;
import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStream;
import java.io.IOException;
import java.util.Hashtable;
import java.util.logging.*;

/**
 * Container for Hessian clients in the same JVM, but not the same
 * class loader.
 */
public class HessianClientContainer implements HessianRemoteResolver {
  private static final Logger log
    = Logger.getLogger(HessianClientContainer.class.getName());
  
  protected static L10N L = new L10N(HessianClientContainer.class);

  private static EnvironmentLocal _hessianClient
    = new EnvironmentLocal("caucho.hessian.client");
  
  private String _serverId;
  private HessianHandleEncoder _handleEncoder;

  private HessianProxyFactory _proxyFactory;
  // the home stub
  EJBHome _ejbHome;

  Class _homeClass;
  Class _remoteClass;
  // the home stub class
  Class _homeStubClass;
  // the remote stub class
  Class _remoteStubClass;
  // the primary key class
  Class _primaryKeyClass;

  private String _basicAuth;

  /**
   * Creates a client container for same-JVM connections.
   *
   * @param _serverId the server id
   */
  HessianClientContainer(String serverId)
    throws ConfigException
  {
    _serverId = serverId;

    _proxyFactory = new HessianProxyFactory();
    _proxyFactory.setHessian2Request(true); // force Hessian 2.0

    Environment.addCloseListener(this);
   }

  static HessianClientContainer find(String serverId)
  {
    try {
      Hashtable map = (Hashtable) _hessianClient.getLevel();
      HessianClientContainer client = null;

      if (map != null)
        client = (HessianClientContainer) map.get(serverId);

      // sync doesn't matter since it's okay to load a dup
      if (client == null) {
        client = new HessianClientContainer(serverId);
        if (map == null)
          map = new Hashtable();
        map.put(serverId, client);
        _hessianClient.set(map);

	try {
	  URL url = new URL(serverId);
	  
	  if (log.isLoggable(Level.FINER))
	    log.finer(client + " clearing keepalive connection " + url);
	  
	  HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	  conn.disconnect();
	  /*
	  InputStream is = url.openStream();
	  is.close();
	  */
	} catch (IOException e) {
	  log.log(Level.FINEST, e.toString(), e);
	}
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
      return (EJBHome) _proxyFactory.create(getHomeClass(), _serverId);
    } catch (Exception e) {
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
  protected Object createObjectStub(String url)
    throws ConfigException
  {
    try {
      return _proxyFactory.create(getRemoteClass(), url);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  HessianHomeHandle getHomeHandle()
  {
    return new HessianHomeHandle(_ejbHome, _serverId);
  }

  HessianHandle createHandle(String url)
  {
    return new HessianHandle(url);
  }
  
  public HandleEncoder getHandleEncoder()
  {
    try {
      if (_handleEncoder == null)
        _handleEncoder = new HessianHandleEncoder(null, _serverId,
                                                 getPrimaryKeyClass());

      return _handleEncoder;
    } catch (Exception e) {
      throw EJBExceptionWrapper.createRuntime(e);
    }
  }
  
  /**
   * Returns the bean's home interface class.  If unknown, call the server
   * for the class name.
   */
  Class getHomeClass()
    throws ConfigException
  {
    if (_homeClass != null)
      return _homeClass;

    try {
      synchronized (this) {
        if (_homeClass != null)
          return _homeClass;

        String className = getHomeClassName();

        _homeClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
    
    return _homeClass;
  }

  /**
   * Returns the classname of the home interface.
   */
  String getHomeClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(_serverId);

    if (server != null) {
      Class cl = server.getRemoteHomeClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("'{0}' has no remote interface.",
                                      _serverId));
    }
        
    try {
      Path path = Vfs.lookup(_serverId);

      return (String) MetaStub.call(path, "_hessian_getAttribute", "java.home.class");
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
    if (_remoteClass != null)
      return _remoteClass;

    try {
      synchronized (this) {
        if (_remoteClass != null)
          return _remoteClass;

        String className = getRemoteClassName();

        if (className == null || className.equals("null"))
          return null;

        _remoteClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }

    return _remoteClass;
  }

  /**
   * Returns the classname of the remote interface.
   */
  String getRemoteClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(_serverId);

    if (server != null) {
      Class cl = server.getRemoteObjectClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("`{0}' has no remote interface.",
                                      _serverId));
    }
    
    try {
      Path path = Vfs.lookup(_serverId);

      return (String) MetaStub.call(path, "_hessian_getAttribute",
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
    if (_primaryKeyClass != null)
      return _primaryKeyClass;

    try {
      synchronized (this) {
        if (_primaryKeyClass != null)
          return _primaryKeyClass;

        String className = getPrimaryKeyClassName();

        _primaryKeyClass = CauchoSystem.loadClass(className, false, null);
      }
    } catch (ClassNotFoundException e) {
      throw ConfigException.create(e);
    }
    
    return _primaryKeyClass;
  }

  /**
   * Returns the classname of the home interface.
   */
  String getPrimaryKeyClassName()
    throws ConfigException
  {
    AbstractServer server = EjbProtocolManager.getJVMServer(_serverId);

    if (server != null) {
      Class cl = server.getPrimaryKeyClass();
      if (cl != null)
        return cl.getName();
      else
        throw new ConfigException(L.l("`{0}' has no remote interface.",
                                      _serverId));
    }
        
    try {
      Path path = Vfs.lookup(_serverId);

      return (String) MetaStub.call(path, "_hessian_getAttribute", "primary-key-class");
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
    return _proxyFactory.create(api, url);
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

  public void close()
  {
    //_proxyFactory.close();
  }

  /**
   * Returns a printable version of the client container
   */
  public String toString()
  {
    return "HessianClientContainer[" + _serverId + "]";
  }
}
