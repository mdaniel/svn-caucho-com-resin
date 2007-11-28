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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.ejb.protocol;

import com.caucho.config.ConfigException;
import com.caucho.ejb.AbstractServer;
import com.caucho.ejb.manager.EjbContainer;
import com.caucho.server.e_app.EnterpriseApplication;
import com.caucho.naming.Jndi;
import com.caucho.util.L10N;

import javax.naming.NamingException;
import java.lang.reflect.Method;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class EjbProtocolManager {
  private static final L10N L = new L10N(EjbProtocolManager.class);
  protected static final Logger log
    = Logger.getLogger(EjbProtocolManager.class.getName());

  private static ThreadLocal<String> _protocolLocal
    = new ThreadLocal<String>();

  private static Hashtable<String,WeakReference<AbstractServer>> _staticServerMap
    = new Hashtable<String,WeakReference<AbstractServer>>();

  private final EjbContainer _ejbContainer;
  private final ClassLoader _loader;

  private String _localJndiPrefix; // = "java:comp/env/cmp";
  private String _remoteJndiPrefix; // = "java:comp/env/ejb";

  private String _jndiPrefix; // java:comp/env/ejb/FooBean/local
  
  private HashMap<String,AbstractServer> _serverMap
    = new HashMap<String,AbstractServer>();

  // handles remote stuff
  protected ProtocolContainer _protocolContainer;
  protected HashMap<String,ProtocolContainer> _protocolMap
    = new HashMap<String,ProtocolContainer>();

  /**
   * Create a server with the given prefix name.
   */
  public EjbProtocolManager(EjbContainer ejbContainer)
    throws ConfigException
  {
    _ejbContainer = ejbContainer;
    _loader = _ejbContainer.getClassLoader();

     ProtocolContainer iiop = IiopProtocolContainer.createProtocolContainer();

     if (iiop != null)
       _protocolMap.put("iiop", iiop);
   }

   public void setJndiPrefix(String name)
   {
     _jndiPrefix = name;
   }

   public String getJndiPrefix()
   {
     return _jndiPrefix;
   }

   public void setLocalJndiPrefix(String name)
   {
     _localJndiPrefix = name;
   }

   public String getLocalJndiPrefix()
   {
     return _localJndiPrefix;
   }

   public void setRemoteJndiPrefix(String name)
   {
     _remoteJndiPrefix = name;
   }

   public String getRemoteJndiPrefix()
   {
     return _remoteJndiPrefix;
   }

   /**
    * Returns the EJB server.
    */
  public EjbContainer getEjbContainer()
  {
    return _ejbContainer;
  }

  /**
   * Initialize the protocol manager.
   */
  public void init()
    throws NamingException
  {
  }

  /**
   * Gets the current protocol.
   */
  public static String getThreadProtocol()
  {
    return _protocolLocal.get();
  }

  /**
   * Gets the current protocol.
   */
  public static String setThreadProtocol(String protocol)
  {
    String oldProtocol = _protocolLocal.get();

    _protocolLocal.set(protocol);

    return oldProtocol;
  }

  public void setProtocolContainer(ProtocolContainer protocol)
  {
    _protocolContainer = protocol;

    synchronized (_protocolMap) {
      _protocolMap.put(protocol.getName(), protocol);
    }


    addProtocolServers(protocol);
  }

  public void addProtocolContainer(ProtocolContainer protocol)
  {
    if (_protocolContainer == null)
      _protocolContainer = protocol;

    addProtocolContainer(protocol.getName(), protocol);
  }

  public void addProtocolContainer(String name, ProtocolContainer protocol)
  {
    synchronized (_protocolMap) {
      if (_protocolMap.get(name) == null)
        _protocolMap.put(name, protocol);
    }

    addProtocolServers(protocol);
  }

  public ProtocolContainer getProtocol(String name)
  {
    synchronized (_protocolMap) {
      return _protocolMap.get(name);
    }
  }

  private void addProtocolServers(ProtocolContainer protocol)
  {
    for (AbstractServer server : _serverMap.values()) {
      protocol.addServer(server);
    }
  }

  /**
   * Returns the named server if it's in the same JVM.
   */
  public static AbstractServer getJVMServer(String serverId)
  {
    WeakReference<AbstractServer> serverRef = _staticServerMap.get(serverId);

    return serverRef != null ? serverRef.get() : null;
  }

  /**
   * Adds a server.
   */
  public void addServer(AbstractServer server)
  {
    _serverMap.put(server.getProtocolId(), server);

    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.addServer(server);
    }

    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(_loader);
      String ejbName = server.getEJBName();
      String mappedName = server.getMappedName();

      // ejb/0g11
      // remote without a local interface should not get bound
      // with the local prefix

      bindDefaultJndi(_jndiPrefix, server);

      // backward compat
      if (server.isLocal() && _localJndiPrefix != null) {
        Object localHome = server.getClientLocalHome();

	Class api = null;

	String jndiName = Jndi.getFullName(_localJndiPrefix + "/" + ejbName);
	
        if (server.getLocal21() != null) {
	  Jndi.bindDeep(jndiName, localHome);
        } else {
          api = server.getLocalApiList().get(0);
	  bindServer(jndiName, server, api);
        }
      }

      // backward compat
      if (server.isRemote() && _remoteJndiPrefix != null) {
        Object remoteHome = server.getEJBHome();

	Class api = null;

	String jndiName = Jndi.getFullName(_remoteJndiPrefix + "/" + ejbName);
	
        if (server.getRemote21() != null) {
	  Jndi.bindDeep(jndiName, remoteHome);
        } else {
          api = server.getRemoteApiList().get(0);
	  bindRemoteServer(jndiName, server, api);
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new ConfigException(e);
    }
    finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
  }

  private void bindDefaultJndi(String prefix, AbstractServer server)
  {
    EnterpriseApplication eApp = EnterpriseApplication.getLocal();

    if (prefix == null)
      prefix = "";
    else if (! prefix.endsWith("/"))
      prefix = prefix + "/";
    
    if (eApp != null)
      prefix = prefix + eApp.getName() + "/";

    prefix = prefix + server.getEJBName();

    ArrayList<Class> apiList = server.getLocalApiList();
    if (apiList != null && apiList.size() > 0) {
      String jndiName = prefix + "/local";

      try {
	Jndi.bindDeep(jndiName, new ServerLocalProxy(server, apiList.get(0)));

	log.fine(server + " local binding to '" + jndiName + "'");
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }
    }
  }

  private void bindServer(String jndiName,
			  AbstractServer server,
			  Class api)
    throws NamingException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(_loader);

      Jndi.bindDeep(jndiName, new ServerLocalProxy(server, api));
    }
    finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
  }

  private void bindRemoteServer(String jndiName,
				AbstractServer server,
				Class api)
    throws NamingException
  {
    Thread thread = Thread.currentThread();
    ClassLoader loader = thread.getContextClassLoader();

    try {
      Thread.currentThread().setContextClassLoader(_loader);

      Jndi.bindDeep(jndiName, new ServerRemoteProxy(server, api));
    }
    finally {
      Thread.currentThread().setContextClassLoader(loader);
    }
  }

  /**
   * Adds a server.
   */
  public void removeServer(AbstractServer server)
    throws NamingException
  {
    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.removeServer(server);
    }
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServerByEJBName(String ejbName)
  {
    if (! ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    return _serverMap.get(ejbName);
  }

  /**
   * Returns the server specified by the serverId.
   */
  public AbstractServer getServerByServerId(String protocolId)
  {
    for (AbstractServer server : _serverMap.values()) {
      if (protocolId.equals(server.getProtocolId()))
        return server;
    }

    return null;
  }

  public Iterator getLocalNames()
  {
    return _serverMap.keySet().iterator();
  }

  /**
   * Returns a list of child EJB names.
   *
   * @param ejbName the name which might be a prefix.
   */
  public ArrayList<String> getLocalChildren(String ejbName)
  {
    if (! ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    if (! ejbName.endsWith("/"))
      ejbName = ejbName + "/";

    ArrayList<String> children = new ArrayList<String>();

    Iterator<String> iter = _serverMap.keySet().iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      AbstractServer server = _serverMap.get(name);

      if (server.getClientObject(null) == null)
        continue;

      if (name.startsWith(ejbName)) {
        int prefixLength = ejbName.length();
        int p = name.indexOf('/', prefixLength);

        if (p > 0)
          name = name.substring(prefixLength, p);
        else
          name = name.substring(prefixLength);

        if (! children.contains(name))
          children.add(name);
      }
    }

    return children;
  }

  /**
   * Returns a list of child EJB names.
   *
   * @param ejbName the name which might be a prefix.
   */
  public ArrayList<String> getRemoteChildren(String ejbName)
  {
    if (! ejbName.startsWith("/"))
      ejbName = "/" + ejbName;

    ArrayList<String> children = new ArrayList<String>();

    Iterator<String> iter = _serverMap.keySet().iterator();
    while (iter.hasNext()) {
      String name = iter.next();

      AbstractServer server = _serverMap.get(name);

      if (server.getRemoteObjectClass() == null)
        continue;

      if (name.startsWith(ejbName)) {
        int prefixLength = ejbName.length();
        int p = name.indexOf('/', prefixLength);

        if (p > 0)
          name = name.substring(prefixLength, p);
        else
          name = name.substring(prefixLength);

        if (! children.contains(name))
          children.add(name);
      }
    }

    if (children.size() == 0)
      return null;
    else
      return children;
  }

  public HandleEncoder createHandleEncoder(AbstractServer server,
                                           Class primaryKeyClass,
                                           String protocolName)
    throws ConfigException
  {
    ProtocolContainer protocol = null;

    synchronized (_protocolMap) {
      protocol = _protocolMap.get(protocolName);
    }

    if (protocol != null)
      return protocol.createHandleEncoder(server, primaryKeyClass);
    else if (_protocolContainer != null)
      return _protocolContainer.createHandleEncoder(server, primaryKeyClass);
    else
      return new HandleEncoder(server, server.getProtocolId());
  }

  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    if (_protocolContainer != null)
      return _protocolContainer.createHandleEncoder(server, primaryKeyClass);
    else
      return new HandleEncoder(server, server.getProtocolId());
  }

  /**
   * Removes an object.
   */
  protected void remove(AbstractHandle handle)
  {
  }

  /**
   * Destroys the manager.
   */
  public void destroy()
  {
  }
}

