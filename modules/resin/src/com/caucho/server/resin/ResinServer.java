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

import com.caucho.config.*;
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
import com.caucho.server.cluster.*;
import com.caucho.transaction.cfg.TransactionManagerConfig;
import com.caucho.util.Alarm;
import com.caucho.util.CauchoSystem;
import com.caucho.util.L10N;
import com.caucho.util.Log;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import javax.el.*;
import javax.management.ObjectName;
import java.security.Provider;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResinServer
  implements EnvironmentBean, SchemaBean {
  private static final Logger log = Log.open(ResinServer.class);
  private static final L10N L = new L10N(ResinServer.class);

  private static final String OBJECT_NAME= "resin:type=Resin";

  private static final EnvironmentLocal<ResinServer> _resinLocal =
    new EnvironmentLocal<ResinServer>();

  private static ResinServer _resinServer;

  private final EnvironmentLocal<String> _serverIdLocal =
    new EnvironmentLocal<String>("caucho.server-id");

  private ObjectName _objectName;

  private ClassLoader _classLoader;

  private String _serverId;
  private String _configFile;

  private Path _resinHome;
  private Path _rootDirectory;

  private String _userName;
  private String _groupName;

  private boolean _isGlobalSystemProperties;
  private boolean _isResinProfessional;

  private long _minFreeMemory = 2 * 1024L * 1024L;
  private long _shutdownWaitMax = 60000L;

  private boolean _isRestartOnClose;

  private SecurityManager _securityManager;

  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  private ArrayList<ResinServerListener> _listeners
    = new ArrayList<ResinServerListener>();

  private ArrayList<InitProgram> _clusterDefaults
    = new ArrayList<InitProgram>();

  private ArrayList<Cluster> _clusters
    = new ArrayList<Cluster>();

  private final Lifecycle _lifecycle = new Lifecycle(log, "Resin[]");

  private Server _server;

  private long _initialStartTime;
  private long _startTime;
  private J2EEDomain _j2eeDomainManagedObject;
  private JVM _jvmManagedObject;

  /**
   * Creates a new resin server.
   */
  public ResinServer()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new resin server.
   */
  public ResinServer(ClassLoader loader)
  {
    _resinServer = this;

    Environment.init();

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    if (loader instanceof EnvironmentClassLoader)
      _classLoader = (EnvironmentClassLoader) loader;
    else
      _classLoader = new EnvironmentClassLoader();

    _resinLocal.set(this, _classLoader);

    _startTime = Alarm.getCurrentTime();

    _variableMap.put("resin", new Var());
    _variableMap.put("java", new JavaVar());

    setResinHome(Vfs.getPwd());
    setRootDirectory(Vfs.getPwd());

    ELResolver varResolver = new SystemPropertiesResolver();
    ConfigELContext elContext = new ConfigELContext(varResolver);
    elContext.push(new MapVariableResolver(_variableMap));
    
    EL.setEnvironment(elContext);
    EL.setVariableMap(_variableMap, _classLoader);
    _variableMap.put("fmt", new com.caucho.config.functions.FmtFunctions());

    new ResinAdmin(this);
  }

  /**
   * Returns the resin server.
   */
  public static ResinServer getResinServer()
  {
    return _resinServer;
  }

  /**
   * Returns the resin server.
   */
  public static ResinServer getLocal()
  {
    return _resinLocal.get();
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
   * Sets resin.home
   */
  public void setResinHome(Path home)
  {
    _resinHome = home;

    _variableMap.put("resinHome", _resinHome);
    _variableMap.put("resin-home", _resinHome);
  }

  /**
   * Returns resin.home.
   */
  public Path getResinHome()
  {
    return _resinHome;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path root)
  {
    _rootDirectory = root;
    
    _variableMap.put("serverRoot", root);
    _variableMap.put("server-root", root);
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
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
    ClusterMXBean []clusters = new ClusterMXBean[_clusters.size()];

    for (int i = 0; i < _clusters.size(); i++)
      clusters[i] = _clusters.get(i).getAdmin();

    return clusters;
  }

  public void addClusterDefault(InitProgram program)
  {
    _clusterDefaults.add(program);
  }

  public Cluster createCluster()
    throws Throwable
  {
    Cluster cluster = new Cluster(this);
    
    for (int i = 0; i < _clusterDefaults.size(); i++)
      _clusterDefaults.get(i).configure(cluster);

    return cluster;
  }

  public void addCluster(Cluster cluster)
  {
    _clusters.add(cluster);
  }
  
  public ArrayList<Cluster> getClusterList()
  {
    return _clusters;
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
   * Creates the compatibility server.
   */
  public ServerCompatConfig createServer()
  {
    return new ServerCompatConfig(this);
    /*
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
      controller.setResinServer(this);

      _servers.add(controller);

      // XXX: controller.addServerListener(this);

      controller.setServerId(_serverId);
      controller.setConfig(config);

      controller.init();
    }
    */
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
   * Returns the active server.
   */
  public Server getServer()
  {
    return _server;
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

    // XXX: get the server

    ClusterServer clusterServer = findClusterServer(_serverId);

    if (clusterServer == null)
      throw new ConfigException(L.l("server-id '{0}' has no matching <server> definition.",
				    _serverId));
    

    _server = clusterServer.startServer();
    
    /*
    ArrayList<ServerController> servers = _servers;

    for (int i = 0; i < servers.size(); i++) {
      ServerController server = servers.get(i);

      server.start();
    }
    */

    Environment.start(getClassLoader());

    /*
    if (! hasListeningPort()) {
      log.warning(L.l("-server \"{0}\" has no matching http or srun ports.  Check the resin.conf and -server values.",
                      _serverId));
    }
    */

    log.info("Resin started in " + (Alarm.getCurrentTime() - start) + "ms");
  }

  public Cluster findCluster(String id)
  {
    for (int i = 0; i < _clusters.size(); i++) {
      Cluster cluster = _clusters.get(i);

      if (cluster.getId().equals(id))
	return cluster;
    }

    return null;
  }

  public ClusterServer findClusterServer(String id)
  {
    for (int i = 0; i < _clusters.size(); i++) {
      Cluster cluster = _clusters.get(i);

      ClusterServer server = cluster.findServer(id);

      if (server != null)
	return server;
    }

    return null;
  }

  /**
   * Returns true if active.
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
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
  public void close()
  {
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
    J2EEManagedObject jvmManagedObject;
    J2EEManagedObject j2eeDomainManagedObject;

    jvmManagedObject = _jvmManagedObject;
    _jvmManagedObject = null;
    
    j2eeDomainManagedObject = _j2eeDomainManagedObject;
    _j2eeDomainManagedObject = null;

    listeners = new ArrayList<ResinServerListener>(_listeners);
    _listeners.clear();

    J2EEManagedObject.unregister(jvmManagedObject);
    J2EEManagedObject.unregister(j2eeDomainManagedObject);

    try {
      _server.destroy();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    try {
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
      return ResinServer.this.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return server-root
     */
    public Path getRoot()
    {
      return ResinServer.this.getRootDirectory();
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
