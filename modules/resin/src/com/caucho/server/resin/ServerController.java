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

package com.caucho.server.resin;

import com.caucho.config.ConfigException;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.log.Log;
import com.caucho.mbeans.server.ServerMBean;
import com.caucho.mbeans.server.ThreadPoolMBean;
import com.caucho.mbeans.j2ee.J2EEManagedObject;
import com.caucho.mbeans.j2ee.J2EEServer;
import com.caucho.naming.Jndi;
import com.caucho.server.cluster.Cluster;
import com.caucho.server.deploy.DeployControllerAdmin;
import com.caucho.server.deploy.EnvironmentDeployController;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

import javax.management.JMException;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Controls the server.
 */
public class ServerController
  extends EnvironmentDeployController<ServletServer,ServerConfig> {
  private static final L10N L = new L10N(ServerController.class);
  private static final Logger log = Log.open(ServerController.class);

  private ResinServer _resinServer;

  // The cluster server id
  private String _serverId = "";

  private ServerAdmin _admin;
  private ThreadPoolMBean _threadPool = new ThreadPoolAdmin();

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

    _admin = new ServerAdmin(this);

    getVariableMap().put("server", new Var());

    try {
      Method method = Jndi.class.getMethod("lookup",
                                           new Class[] { String.class });
      getVariableMap().put("jndi:lookup", method);
    } catch (Throwable e) {
    }
  }

  /**
   * Sets the resin server.
   */
  public void setResinServer(ResinServer resinServer)
  {
    _resinServer = resinServer;
  }

  /**
   * Sets the resin server.
   */
  public ResinServer getResinServer()
  {
    return _resinServer;
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
   * Returns the thread pool.
   */
  public ThreadPoolMBean getThreadPool()
  {
    return _threadPool;
  }

  /**
   * Returns true if there's a listening port.
   */
  public boolean hasListeningPort()
  {
    ServletServer server = getDeployInstance();

    return server != null && server.hasListeningPort();
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
    return new IntrospectionMBean(getDeployAdmin(),
                                  ServerMBean.class);
  }

  /**
   * Returns the owning cluster.
   */
  public Cluster getCluster()
  {
    return getDeployInstance().getCluster();
  }

  /**
   * Returns the deploy admin.
   */
  protected DeployControllerAdmin getDeployAdmin()
  {
    return _admin;
  }

  protected void initEnd()
  {
    super.initEnd();

    J2EEManagedObject.register(new J2EEServer(this));
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
   * Changes the configured user.
   */
  public void setuid()
  {
    if (_resinServer != null)
      _resinServer.setuid();
  }

  /**
   * Creates the object name.  The default is to use getId() as
   * the 'name' property, and the classname as the 'type' property.
   */
  protected ObjectName createObjectName(Map<String,String> properties)
    throws MalformedObjectNameException
  {
    return new ObjectName("resin:type=Server");
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

    public Path getRoot()
    {
      return ServerController.this.getRootDirectory();
    }

    public Path getRootDir()
    {
      return getRoot();
    }

    public Path getRootDirectory()
    {
      return getRoot();
    }

    public String toString()
    {
      return "Server[" + getId() + "]";
    }
  }
}
