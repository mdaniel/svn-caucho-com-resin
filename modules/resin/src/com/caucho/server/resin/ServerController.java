/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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

package com.caucho.server.resin;

import java.lang.reflect.Method;

import java.util.logging.Logger;

import javax.management.JMException;

import javax.servlet.jsp.el.VariableResolver;
import javax.servlet.jsp.el.ELException;

import com.caucho.util.*;
import com.caucho.vfs.*;

import com.caucho.naming.Jndi;

import com.caucho.log.Log;

import com.caucho.config.ConfigException;

import com.caucho.jmx.IntrospectionMBean;

import com.caucho.server.cluster.Cluster;

import com.caucho.server.deploy.EnvironmentDeployController;

import com.caucho.server.resin.mbean.ServletServerMBean;

/**
 * Controls the server.
 */
public class ServerController
  extends EnvironmentDeployController<ServletServer,ServerConfig> {
  private static final L10N L = new L10N(ServerController.class);
  private static final Logger log = Log.open(ServerController.class);

  // The cluster server id
  private String _serverId = "";

  public ServerController()
  {
    this("", null);
  }

  public ServerController(ServerConfig config)
    throws ConfigException
  {
    this(config.getId(), config.calculateRootDirectory());

    setConfig(config);
  }

  public ServerController(String id, Path rootDirectory)
  {
    super(id, rootDirectory);

    getVariableMap().put("server", new Var());

    try {
      Method method = Jndi.class.getMethod("lookup",
					   new Class[] { String.class });
      getVariableMap().put("jndi:lookup", method);
    } catch (Throwable e) {
    }
  }

  /**
   * Sets the cluster server id.
   */
  public void setServerId(String serverId)
  {
    _serverId = serverId;
    
    getVariableMap().put("serverId", serverId);
  }

  /**
   * Gets the cluster server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Instantiate the application.
   */
  protected ServletServer instantiateDeployInstance()
  {
    return new ServletServer(this);
  }

  /**
   * Creates the managed object.
   */
  protected Object createMBean()
    throws JMException
  {
    return new IntrospectionMBean(new ServerAdmin(this),
				  ServletServerMBean.class);
  }

  /**
   * Creates the application.
   */
  protected void configureInstance(ServletServer server)
    throws Throwable
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    Path rootDirectory = null;
    try {
      thread.setContextClassLoader(server.getClassLoader());
      
      getVariableMap().put("root-dir", getRootDirectory());
      getVariableMap().put("server-root", getRootDirectory());

      super.configureInstance(server);

      server.setServerId(getServerId());

      rootDirectory = getRootDirectory();

      if (rootDirectory == null)
	throw new NullPointerException("Null root directory");

      if (! rootDirectory.isFile()) {
      }
      else if (rootDirectory.getPath().endsWith(".jar") ||
	       rootDirectory.getPath().endsWith(".war")) {
	throw new ConfigException(L.l("root-directory `{0}' must specify a directory.  It may not be a .jar or .war.",
				      rootDirectory.getPath()));
      }
      else
	throw new ConfigException(L.l("root-directory `{0}' may not be a file.  root-directory must specify a directory.",
				      rootDirectory.getPath()));


      server.init();
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  /**
   * Called to bind the ports.
   */
  public void bindPortsBeforeStart()
  {
  }

  protected String getMBeanTypeName()
  {
    return "Server";
  }
  
  /**
   * Returns a printable view.
   */
  public String toString()
  {
    return "ServerController$" + System.identityHashCode(this) + "[" + getId() + "]";
  }

  /**
   * EL variables for the server.
   */
  public class Var {
    public String getId()
    {
      return ServerController.this.getServerId();
    }
    
    public Path getRootDir()
    {
      return getRootDirectory();
    }
    
    public Path getRootDirectory()
    {
      return ServerController.this.getRootDirectory();
    }
    
    public String toString()
    {
      return "Server[" + getId() + "]";
    }
  }
}
