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

import java.lang.ref.WeakReference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Hashtable;

import java.util.logging.Logger;

import javax.naming.InitialContext;
import javax.naming.Context;
import javax.naming.NamingException;

import com.caucho.util.L10N;

import com.caucho.config.ConfigException;

import com.caucho.log.Log;

import com.caucho.naming.AbstractModel;
import com.caucho.naming.MemoryModel;
import com.caucho.naming.Jndi;

import com.caucho.ejb.EnvServerManager;
import com.caucho.ejb.AbstractServer;


/**
 * Server containing all the EJBs for a given configuration.
 *
 * <p>Each protocol will extend the container to override Handle creation.
 */
public class EjbProtocolManager {
  private static final L10N L = new L10N(EjbProtocolManager.class);
  protected static final Logger log = Log.open(EjbProtocolManager.class);

  private static ThreadLocal<String> _protocolLocal
    = new ThreadLocal<String>();

  private static Hashtable<String,WeakReference<AbstractServer>> _staticServerMap
    = new Hashtable<String,WeakReference<AbstractServer>>();

  private EnvServerManager _ejbServer;
  
  private AbstractModel _localRoot;
  private AbstractModel _remoteRoot;
  
  private String _localJndiName = "java:comp/env/cmp";
  private String _remoteJndiName = "java:comp/env/ejb";
  
  private HashMap<String,AbstractServer> _serverMap
    = new HashMap<String,AbstractServer>();

  // handles remote stuff
  protected ProtocolContainer _protocolContainer;
  protected HashMap<String,ProtocolContainer> _protocolMap
    = new HashMap<String,ProtocolContainer>();

  /**
   * Create a server with the given prefix name.
   */
  public EjbProtocolManager(EnvServerManager ejbServer)
    throws ConfigException
  {
    _ejbServer = ejbServer;

    _localRoot = new MemoryModel();
    _remoteRoot = new MemoryModel();

    ProtocolContainer iiop = IiopProtocolContainer.createProtocolContainer();

    if (iiop != null)
      _protocolMap.put("iiop", iiop);
  }

  public void setLocalJndiName(String name)
  {
    _localJndiName = name;
  }

  public String getLocalJndiName()
  {
    return _localJndiName;
  }

  public void setRemoteJndiName(String name)
  {
    _remoteJndiName = name;
  }

  public String getRemoteJndiName()
  {
    return _remoteJndiName;
  }

  /**
   * Returns the EJB server.
   */
  public EnvServerManager getServerManager()
  {
    return _ejbServer;
  }

  public AbstractModel getLocalNamingModel()
  {
    return _localRoot;
  }

  /**
   * Initialize the protocol manager.
   */
  public void init()
    throws NamingException
  {
    Jndi.rebindDeep(_localJndiName, new NamingProxy(_localRoot));

    if (_localJndiName.equals(_remoteJndiName))
      _remoteRoot = _localRoot;
    else
      Jndi.rebindDeep(_remoteJndiName, new NamingProxy(_remoteRoot));
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
    throws NamingException
  {
    _serverMap.put(server.getEJBName(), server);
    
    addLocalServer(server);
    addRemoteServer(server);
  }

  /**
   * Adds a server.
   */
  public void removeServer(AbstractServer server)
    throws NamingException
  {
    //addLocalServer(server);
    removeRemoteServer(server);
  }
	   
  /**
   * Adds a server.
   */
  public void addLocalServer(AbstractServer server)
    throws NamingException
  {
    String ejbName = server.getEJBName();

    if (server.getEJBLocalHome() != null)
      addServer(_localRoot, ejbName, server.getEJBLocalHome());
    else if (server.getClientObject() != null)
      addServer(_localRoot, ejbName, server.getClientObject());
  }

  /**
   * Adds a remote server.
   */
  public void addRemoteServer(AbstractServer server)
    throws NamingException
  {
    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.addServer(server);
    }
    
    try {
      String ejbName = server.getEJBName();

      if (server.getEJBHome() != null)
	addServer(_remoteRoot, ejbName, server.getEJBHome());
    } catch (RuntimeException e) {
      throw e;
    } catch (NamingException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Removes a remote server.
   */
  public void removeRemoteServer(AbstractServer server)
    throws NamingException
  {
    String ejbName = server.getEJBName();
    
    while (ejbName.startsWith("/"))
      ejbName = ejbName.substring(1);
    
    while (ejbName.endsWith("/"))
      ejbName = ejbName.substring(ejbName.length() - 1);

    for (ProtocolContainer protocol : _protocolMap.values()) {
      protocol.removeServer(server);
    }
  }

  /**
   * DeployGenerator locally
   */
  public void deployEJB(String ejbName, AbstractServer server)
  {
    
  }

  /**
   * DeployGenerator locally
   */
  public void deployJNDI(String jndiName, AbstractServer server)
    throws Exception
  {
    /*
    Context ic = new InitialContext();
    ic.rebind(jndiName, this);
    */
  }
	   
  /**
   * Adds a server.
   */
  private void addServer(AbstractModel model,
			 String ejbName,
			 Object home)
    throws NamingException
  {
    while (ejbName.startsWith("/"))
      ejbName = ejbName.substring(1);
    
    while (ejbName.endsWith("/"))
      ejbName = ejbName.substring(ejbName.length() - 1);
    
    String []split = ejbName.split("/+");

    for (int i = 0; i < split.length - 1; i++) {
      if (model.lookup(split[i]) != null)
	model = (AbstractModel) model.lookup(split[i]);
      else
	model = model.createSubcontext(split[i]);
    }

    model.bind(split[split.length - 1], home);
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

      if (server.getClientObject() == null)
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
    
    if (! ejbName.endsWith("/"))
      ejbName = ejbName + "/";

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
      return new HandleEncoder(server, server.getEJBName());
  }

  protected HandleEncoder createHandleEncoder(AbstractServer server,
                                              Class primaryKeyClass)
    throws ConfigException
  {
    if (_protocolContainer != null)
      return _protocolContainer.createHandleEncoder(server, primaryKeyClass);
    else
      return new HandleEncoder(server, server.getEJBName());
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

