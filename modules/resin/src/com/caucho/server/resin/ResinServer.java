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
import com.caucho.config.SchemaBean;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.InitProgram;
import com.caucho.config.types.Period;
import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;
import com.caucho.el.SystemPropertiesResolver;
import com.caucho.jmx.Jmx;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.LifecycleState;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.loader.EnvironmentProperties;
import com.caucho.management.j2ee.*;
import com.caucho.management.server.*;
import com.caucho.server.dispatch.DispatchServer;
import com.caucho.server.dispatch.ServerListener;
import com.caucho.transaction.cfg.TransactionManagerConfig;
import com.caucho.util.Alarm;
import com.caucho.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.management.ObjectName;
import javax.servlet.jsp.el.VariableResolver;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResinServer
  implements EnvironmentBean, SchemaBean,
             ServerListener {
  private static final Logger log = Log.open(ResinServer.class);
  private static final L10N L = new L10N(ResinServer.class);

  private static final String OBJECT_NAME= "resin:type=Resin";

  private static ResinServer _resinServer;

  private final EnvironmentLocal<String> _serverIdLocal =
    new EnvironmentLocal<String>("caucho.server-id");

  private ObjectName _objectName;

  private ClassLoader _classLoader;

  private String _serverId;
  private String _configFile;

  private String _userName;
  private String _groupName;

  private boolean _isGlobalSystemProperties;
  private boolean _isResinProfessional;

  private long _minFreeMemory = 2 * 1024L * 1024L;
  private long _shutdownWaitMax = 60000L;

  private boolean _isRestartOnClose;

  private SecurityManager _securityManager;

  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  private ArrayList<ServerController> _servers
    = new ArrayList<ServerController>();

  private ArrayList<ResinServerListener> _listeners =
    new ArrayList<ResinServerListener>();

  private final Lifecycle _lifecycle = new Lifecycle(log, "Resin[]");

  private long _initialStartTime;
  private long _startTime;
  private J2EEDomain _j2eeDomainManagedObject;
  private JVM _jvmManagedObject;

  /**
   * Creates a new resin server.
   */
  public ResinServer()
  {
    _resinServer = this;

    _classLoader = Thread.currentThread().getContextClassLoader();

    if (_classLoader == null)
      _classLoader = ClassLoader.getSystemClassLoader();

    Environment.init();

    _startTime = Alarm.getCurrentTime();

    _variableMap.put("resin", new Var());
    _variableMap.put("java", new JavaVar());

    _variableMap.put("resinHome", CauchoSystem.getResinHome());
    _variableMap.put("serverRoot", CauchoSystem.getServerRoot());

    _variableMap.put("resin-home", CauchoSystem.getResinHome());
    _variableMap.put("server-root", CauchoSystem.getServerRoot());

    Vfs.setPwd(CauchoSystem.getServerRoot());

    VariableResolver varResolver = new SystemPropertiesResolver();
    varResolver = new MapVariableResolver(_variableMap, varResolver);
    EL.setEnvironment(varResolver);
    EL.setVariableMap(_variableMap, _classLoader);
    _variableMap.put("fmt", new com.caucho.config.functions.FmtFunctions());

    try {
      _objectName = new ObjectName("resin:type=Resin");

      Jmx.register(new ResinAdmin(this), OBJECT_NAME);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Returns the resin server.
   */
  public static ResinServer getResinServer()
  {
    return _resinServer;
  }

  /**
   * Returns the classLoader
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  public ObjectName getObjectName()
  {
    return _objectName;
  }

  /**
   * Sets the classLoader
   */
  public void setEnvironmentClassLoader(EnvironmentClassLoader loader)
  {
    _classLoader = loader;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/resin.rnc";
  }

  /**
   * Sets the server id.
   */
  public void setServerId(String serverId)
  {
    _serverId = serverId;
    _serverIdLocal.set(serverId);
  }

  /**
   * Returns the server id.
   */
  public String getServerId()
  {
    return _serverId;
  }

  /**
   * Sets the config file.
   */
  public void setConfigFile(String configFile)
  {
    _configFile = configFile;
  }

  /**
   * Gets the config file.
   */
  public String getConfigFile()
  {
    return _configFile;
  }

  /**
   * Set true for Resin pro.
   */
  void setResinProfessional(boolean isPro)
  {
    _isResinProfessional = isPro;
  }

  /**
   * Set true for Resin pro.
   */
  public boolean isProfessional()
  {
    return _isResinProfessional;
  }

  /**
   * Returns the cluster names.
   */
  public ClusterMXBean []getClusters()
  {
    return new ClusterMXBean[0];
  }

  /**
   * Set true if the server should restart rather than exit when
   * the instance shuts down.
   */
  public boolean isRestartOnClose()
  {
    return _isRestartOnClose;
  }

  /**
   * Set true if the server should restart rather than exit when
   * the instance shuts down.
   */
  public void setRestartOnClose(boolean isRestartOnClose)
  {
    _isRestartOnClose = isRestartOnClose;
  }

  /**
   * Set true if the server should enable environment-based
   * system properties.
   */
  public void setEnvironmentSystemProperties(boolean isEnable)
  {
    EnvironmentProperties.enableEnvironmentSystemProperties(isEnable);
  }

  /**
   * Creates the server.
   */
  public void addServer(ServerConfig config)
    throws Exception
  {
    addDomain(config);
  }

  /**
   * Creates the domain.
   */
  public void addDomain(ServerConfig config)
    throws Exception
  {
    if (Alarm.isTest() && _servers.size() == 1) {
      _servers.get(0).addConfigDefault(config);
    }
    else {
      String id = config.getId();

      if (id != null && ! id.equals("")) {
      }
      else
        id = String.valueOf(_servers.size());

      ServerController controller = new ServerController(config);

      _servers.add(controller);

      // XXX: controller.addServerListener(this);

      controller.setServerId(_serverId);
      controller.setConfig(config);
      controller.setResinServer(this);

      controller.init();
    }
  }

  /**
   * Returns the servers.
   */
  public ArrayList<ServerController> getServerList()
  {
    return _servers;
  }

  /**
   * Configures the thread pool
   */
  public ThreadPoolConfig createThreadPool()
    throws Exception
  {
    return new ThreadPoolConfig();
  }

  /**
   * Sets the user name for setuid.
   */
  public void setUserName(String userName)
  {
    _userName = userName;
  }

  /**
   * Sets the group name for setuid.
   */
  public void setGroupName(String groupName)
  {
    _groupName = groupName;
  }

  /**
   * Sets the minimum free memory allowed.
   */
  public void setMinFreeMemory(Bytes minFreeMemory)
  {
    _minFreeMemory = minFreeMemory.getBytes();
  }

  /**
   * Gets the minimum free memory allowed.
   */
  public long getMinFreeMemory()
  {
    return _minFreeMemory;
  }

  /**
   * Sets the shutdown time
   */
  public void setShutdownWaitMax(Period shutdownWaitMax)
  {
    _shutdownWaitMax = shutdownWaitMax.getPeriod();
  }

  /**
   * Gets the minimum free memory allowed.
   */
  public long getShutdownWaitMax()
  {
    return _shutdownWaitMax;
  }

  /**
   * Set true if system properties are global.
   */
  public void setGlobalSystemProperties(boolean isGlobal)
  {
    _isGlobalSystemProperties = isGlobal;
  }

  /**
   * Configures the TM.
   */
  public void addTransactionManager(TransactionManagerConfig tm)
    throws ConfigException
  {
    // the start is necessary to handle the QA tests

    tm.start();
  }

  public SecurityManagerConfig createSecurityManager()
  {
    return new SecurityManagerConfig();
  }

  /**
   * Adds a new server (backwards compatibility).
   */
  public void addHttpServer(ServerConfig config)
    throws Exception
  {
    addServer(config);
  }

  /**
   * Adds a new security provider
   */
  public void addSecurityProvider(Class providerClass)
    throws Exception
  {
    if (! Provider.class.isAssignableFrom(providerClass))
      throw new ConfigException(L.l("security-provider {0} must implement java.security.Provider",
                                    providerClass.getName()));

    Security.addProvider((Provider) providerClass.newInstance());
  }

  /**
   * Configures JSP (backwards compatibility).
   */
  public JspPropertyGroup createJsp()
  {
    return new JspPropertyGroup();
  }

  /**
   * Ignore the boot configuration
   */
  public void addBoot(InitProgram program)
    throws Exception
  {
  }

  /**
   * Sets the initial start time.
   */
  void setInitialStartTime(long now)
  {
    _initialStartTime = now;
  }

  /**
   * Returns the initial start time.
   */
  public Date getInitialStartTime()
  {
    return new Date(_initialStartTime);
  }

  /**
   * Returns the start time.
   */
  public Date getStartTime()
  {
    return new Date(_startTime);
  }

  /**
   * Returns the current lifecycle state.
   */
  public LifecycleState getLifecycleState()
  {
    return _lifecycle;
  }

  /**
   * Initialize the server.
   */
  public void init()
  {
    _lifecycle.toInit();
  }

  /**
   * Sets the user id.
   */
  public void setuid()
  {
    if (_userName != null) {
      try {
        int uid = CauchoSystem.setUser(_userName, _groupName);
        if (uid >= 0)
          log.info(L.l("Running as {0}(uid={1})", _userName, "" + uid));
        else
          log.warning(L.l("Can't run as {0}(uid={1}), running as root.",
                          _userName, "" + uid));
      } catch (Exception e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  /**
   * Starts the server.
   */
  public void start()
    throws Throwable
  {
    if (! _lifecycle.toActive())
      return;

    long start = Alarm.getCurrentTime();

    _j2eeDomainManagedObject = J2EEManagedObject.register(new J2EEDomain());
    _jvmManagedObject = J2EEManagedObject.register(new JVM());

    // force a GC on start
    System.gc();

    ArrayList<ServerController> servers = _servers;

    for (int i = 0; i < servers.size(); i++) {
      ServerController server = servers.get(i);

      server.start();
    }

    Environment.start(getClassLoader());

    if (! hasListeningPort()) {
      log.warning(L.l("-server \"{0}\" has no matching http or srun ports.  Check the resin.conf and -server values.",
                      _serverId));
    }

    log.info("Resin started in " + (Alarm.getCurrentTime() - start) + "ms");
  }

  /**
   * Returns true for a listening port.
   */
  private boolean hasListeningPort()
  {
    for (int i = 0; i < _servers.size(); i++) {
      ServerController server = _servers.get(i);

      if (server.hasListeningPort())
        return true;
    }

    return false;
  }

  /**
   * Adds a listener.
   */
  public void addListener(ResinServerListener listener)
  {
    _listeners.add(listener);
  }

  /**
   * When one ServletServer closes, close everything.
   */
  public void closeEvent(DispatchServer server)
  {
    // XXX:
    log.info("Received close event");
    destroy();
  }

  /**
   * Returns true if the server is closing.
   */
  public boolean isClosing()
  {
    return _lifecycle.isDestroying();
  }

  /**
   * Returns true if the server is closed.
   */
  public boolean isClosed()
  {
    return _lifecycle.isDestroyed();
  }

  /**
   * Closes the server.
   */
  public void destroy()
  {
    if (! _lifecycle.toDestroying())
      return;

    ArrayList<ResinServerListener> listeners;
    ArrayList<ServerController> servers;
    J2EEManagedObject jvmManagedObject;
    J2EEManagedObject j2eeDomainManagedObject;

    jvmManagedObject = _jvmManagedObject;
    _jvmManagedObject = null;
    
    j2eeDomainManagedObject = _j2eeDomainManagedObject;
    _j2eeDomainManagedObject = null;

    listeners = new ArrayList<ResinServerListener>(_listeners);
    _listeners.clear();

    servers = new ArrayList<ServerController>(_servers);
    _servers.clear();

    J2EEManagedObject.unregister(jvmManagedObject);
    J2EEManagedObject.unregister(j2eeDomainManagedObject);

    try {
      for (ServerController server : servers) {
        server.destroy();
      }

      for (int i = 0; i < listeners.size(); i++) {
        ResinServerListener listener = listeners.get(i);

        listener.closeEvent(this);
      }

      Environment.closeGlobal();
    } finally {
      _lifecycle.toDestroy();
    }
  }

  /**
   * EL variables
   */
  public class Var {
    /**
     * Returns the resin.id
     */
    public String getId()
    {
      return _serverId;
    }

    /**
     * Returns the resin home.
     */
    public Path getHome()
    {
      return CauchoSystem.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return server-root
     */
    public Path getRoot()
    {
      return CauchoSystem.getServerRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return resin.home
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }

    /**
     * Returns true for Resin professional.
     */
    public boolean isProfessional()
    {
      return _isResinProfessional;
    }
  }

  /**
   * Java variables
   */
  public class JavaVar {
    /**
     * Returns true for JDK 5
     */
    public boolean isJava5()
    {
      return CauchoSystem.isJdk15();
    }
  }

  class SecurityManagerConfig {
    private boolean _isEnable;

    SecurityManagerConfig()
    {
      if (_securityManager == null)
        _securityManager = new SecurityManager();
    }

    public void setEnable(boolean enable)
    {
      _isEnable = enable;
    }

    public void setValue(boolean enable)
    {
      setEnable(enable);
    }

    public void setPolicyFile(Path path)
      throws ConfigException
    {
      if (! path.canRead())
        throw new ConfigException(L.l("policy-file '{0}' must be readable.",
                                      path));

    }

    public void init()
    {
      if (_isEnable)
        System.setSecurityManager(_securityManager);
    }
  }
}
