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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.cluster;

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.program.ContainerProgram;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.config.types.Period;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.server.port.Port;
import com.caucho.server.resin.Resin;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;
import com.caucho.webbeans.manager.*;

import javax.management.ObjectName;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.*;

/**
 * Defines a set of clustered servers.
 */
public class Cluster
  implements EnvironmentListener, EnvironmentBean, SchemaBean
{
  private static final L10N L = new L10N(Cluster.class);
  private static final Logger log = Logger.getLogger(Cluster.class.getName());

  static protected final EnvironmentLocal<String> _serverIdLocal
    = new EnvironmentLocal<String>("caucho.server-id");

  static protected final EnvironmentLocal<Cluster> _clusterLocal
    = new EnvironmentLocal<Cluster>("caucho.cluster");

  private static final int DECODE[];
  
  private String _id = "";

  private String _serverId = "";

  private EnvironmentClassLoader _classLoader;
  
  private Resin _resin;

  private Path _rootDirectory;

  private ClusterAdmin _admin;
  private ObjectName _objectName;

  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();

  private ArrayList<Machine> _machineList
    = new ArrayList<Machine>();

  private ArrayList<ClusterServer> _serverList
    = new ArrayList<ClusterServer>();

  private ClusterServer[] _serverArray = new ClusterServer[0];

  private StoreManager _clusterStore;

  private boolean _isDynamicServerEnable = false;

  // compatibility with 3.0
  private long _clientMaxIdleTime = 30000L;
  private long _clientFailRecoverTime = 15000L;
  private long _clientWarmupTime = 60000L;

  private long _clientReadTimeout = 60000L;
  private long _clientConnectTimeout = 5000L;

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  private Server _server;

  private long _version;

  private volatile boolean _isClosed;

  public Cluster(Resin resin)
  {
    this();

    _resin = resin;
  }
    
  public Cluster()
  {
    _classLoader = new EnvironmentClassLoader("cluster:??");

    _clusterLocal.set(this, _classLoader);
  
    Environment.addEnvironmentListener(this, _classLoader);

    WebBeansContainer.create().addSingletonByName(new Var(), "cluster");

    _rootDirectory = Vfs.getPwd();
  }

  /**
   * Returns the currently active local cluster.
   */
  public static Cluster getLocal()
  {
    Cluster cluster = _clusterLocal.get();

    return cluster;
  }

  /**
   * Returns the currently active local cluster.
   */
  public static Cluster getCurrent()
  {
    Cluster cluster = _clusterLocal.get();

    return cluster;
  }

  /**
   * Returns the currently active local cluster.
   */
  public static Cluster getCluster(ClassLoader loader)
  {
    Cluster cluster = _clusterLocal.get(loader);

    return cluster;
  }

  /**
   * Sets the cluster id.
   */
  public void setId(String id)
  {
    if (id == null)
      throw new NullPointerException();
    
    _id = id;

    _classLoader.setId("cluster:" + _id);
  }

  /**
   * Gets the cluster id.
   */
  public String getId()
  {
    return _id;
  }

  /**
   * Returns the owning resin server.
   */
  public Resin getResin()
  {
    return _resin;
  }

  /**
   * Returns the environment class loader.
   */
  public ClassLoader getClassLoader()
  {
    return _classLoader;
  }

  /**
   * Returns the relax schema.
   */
  public String getSchema()
  {
    return "com/caucho/server/resin/cluster.rnc";
  }

  /**
   * Gets the root directory.
   */
  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  /**
   * Sets the root directory.
   */
  public void setRootDirectory(Path rootDirectory)
  {
    Vfs.setPwd(rootDirectory);
    
    _rootDirectory = rootDirectory;
  }

  /**
   * Enables dynamic servers
   */
  public void setDynamicServerEnable(boolean isEnable)
  {
    _isDynamicServerEnable = isEnable;
  }

  /**
   * Enables dynamic servers
   */
  public boolean isDynamicServerEnable()
  {
    return _isDynamicServerEnable;
  }

  /**
   * Returns the version
   */
  public long getVersion()
  {
    return _version;
  }

  /**
   * Returns the admin.
   */
  public ClusterMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String id)
  {
    for (int i = _serverList.size() - 1; i >= 0; i--) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getId().equals(id))
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String address, int port)
  {
    for (int i = _serverList.size() - 1; i >= 0; i--) {
      ClusterServer server = _serverList.get(i);

      if (server == null)
	continue;

      ClusterPort clusterPort = server.getClusterPort();
      
      if (clusterPort.getAddress().equals(address)
	  && clusterPort.getPort() == port) {
        return server;
      }
    }

    return null;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServerDefault(ContainerProgram program)
  {
    _serverDefaultList.add(program);
  }

  /**
   * Adds a new server to the cluster.
   */
  public Machine createMachine()
  {
    Machine machine = new Machine(this);

    _machineList.add(machine);

    return machine;
  }

  /**
   * Adds a new server to the cluster.
   */
  public ClusterServer createServer()
  {
    Machine machine = createMachine();
  
    return machine.createServer();
  }

  ClusterServer createServer(ClusterServer server)
  {
    server.setIndex(_serverList.size());

    for (int i = 0; i < _serverDefaultList.size(); i++)
      _serverDefaultList.get(i).configure(server);

    return server;
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addServer(ClusterServer server)
    throws ConfigException
  {
    ClusterServer oldServer = findServer(server.getId());

    if (oldServer != null)
      log.warning(L.l("duplicate <server> with id='{0}'",
                      server.getId()));

    _serverList.add(server);
    _serverArray = new ClusterServer[_serverList.size()];
    _serverList.toArray(_serverArray);

    ClusterServer selfServer = getSelfServer();

    if (selfServer == server) {
      WebBeansContainer webBeans = WebBeansContainer.create();
      
      webBeans.addSingletonByName(new ServerVar(server), "server");
    }
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addDynamicServer(String serverId, String address, int port)
    throws ConfigException
  {
    if (! isDynamicServerEnable()) {
      log.warning(this + " forbidden dynamic-server add id=" + serverId
		  + " " + address + ":" + port);
      return;
    }
    
    try {
      ClusterServer oldServer = findServer(serverId);

      if (oldServer != null) {
	throw new ConfigException(L.l("duplicate server with id='{0}'",
				      serverId));
      }
      
      oldServer = findServer(address, port);

      if (oldServer != null) {
	throw new ConfigException(L.l("duplicate server with '{0}:{1}'",
				      address, port));
      }

      ClusterServer server = createServer();
      server.setId(serverId);

      server.setAddress(address);
      server.setPort(port);

      server.setDynamic(true);

      addServer(server);
      
      server.init();

      log.info(this + " add dynamic server " + server);
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addDynamicServer(ClusterServer server)
    throws ConfigException
  {
    try {
      synchronized (this) {
	for (ConfigProgram program : _serverDefaultList)
	  program.configure(server);
    
	server.init();
      
	// XXX: default config
	addServer(server);

	_version++;
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " add dynamic server " + server);
  }

  /**
   * Adds a new server to the cluster.
   */
  public void removeDynamicServer(ClusterServer server)
    throws ConfigException
  {
    if (! isDynamicServerEnable()) {
      log.warning(this + " forbidden dynamic-server remove " + server);
      return;
    }
    
    try {
      synchronized (this) {
	// XXX: default config
	
	_serverList.remove(server);
	_serverArray = new ClusterServer[_serverList.size()];
	_serverList.toArray(_serverArray);

	_version++;
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    if (log.isLoggable(Level.FINE))
      log.fine(this + " remove dynamic server " + server);
  }

  /**
   * Adds a srun server.
   */
  public ServerConnector findConnector(String address, int port)
  {
    for (int i = _serverList.size() - 1; i >= 0; i--) {
      ClusterServer server = _serverList.get(i);
      ClusterPort clusterPort = server.getClusterPort();

      if (address.equals(clusterPort.getAddress())
	  && port == clusterPort.getPort()) {
	// XXX:
	//return server.getClient();
	return null;
      }
    }

    return null;
  }

  /**
   * Returns the cluster store.
   */
  public StoreManager getStore()
  {
    return _clusterStore;
  }

  /**
   * Sets the cluster store.
   */
  void setStore(StoreManager store)
  {
    _clusterStore = store;
  }

  /**
   * Sets the max-idle time.
   */
  public void setClientMaxIdleTime(Period period)
  {
    _clientMaxIdleTime = period.getPeriod();
  }

  /**
   * Gets the live time.
   */
  public long getClientMaxIdleTime()
  {
    return _clientMaxIdleTime;
  }

  /**
   * Sets the live time.
   *
   * @deprecated
   */
  public void setClientLiveTime(Period period)
  {
    setClientMaxIdleTime(period);
  }

  /**
   * Sets the client connection fail-recover time.
   */
  public void setClientFailRecoverTime(Period period)
  {
    _clientFailRecoverTime = period.getPeriod();
  }

  /**
   * Gets the client fail-recover time.
   */
  public long getClientFailRecoverTime()
  {
    return _clientFailRecoverTime;
  }

  /**
   * Sets the dead time.
   *
   * @deprecated
   */
  public void setClientDeadTime(Period period)
  {
    setClientFailRecoverTime(period);
  }

  /**
   * Sets the client warmup time.
   */
  public void setClientWarmupTime(Period period)
  {
    _clientWarmupTime = period.getPeriod();
  }

  /**
   * Gets the client warmup time.
   */
  public long getClientWarmupTime()
  {
    return _clientWarmupTime;
  }

  /**
   * Sets the connect timeout.
   */
  public void setClientConnectTimeout(Period period)
  {
    _clientConnectTimeout = period.getPeriod();
  }

  /**
   * Gets the connect timeout.
   */
  public long getClientConnectTimeout()
  {
    return _clientConnectTimeout;
  }

  /**
   * Sets the read timeout.
   */
  public void setClientReadTimeout(Period period)
  {
    _clientReadTimeout = period.getPeriod();
  }

  /**
   * Gets the read timeout.
   */
  public long getClientReadTimeout()
  {
    return _clientReadTimeout;
  }

  /**
   * Sets the write timeout.
   */
  public void setClientWriteTimeout(Period period)
  {
  }

  public StoreManager createJdbcStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple jdbc stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.JdbcStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'jdbc' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  public StoreManager createPrivateFileStore()
    throws ConfigException
  {
    StoreManager store = createFileStore();

    setStore(null);

    return store;
  }

  public StoreManager createFileStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple file stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.FileStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'file' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  public StoreManager createClusterStore()
    throws ConfigException
  {
    if (getStore() != null)
      throw new ConfigException(L.l("multiple cluster stores are not allowed in a cluster."));

    StoreManager store = null;

    try {
      Class cl = Class.forName("com.caucho.server.cluster.ClusterStore");

      store = (StoreManager) cl.newInstance();

      store.setCluster(this);

      setStore(store);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }

    if (store == null)
      throw new ConfigException(L.l("'cluster' persistent sessions are available in Resin Professional.  See http://www.caucho.com for information and licensing."));

    return store;
  }

  /**
   * Adds a program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }

  /**
   * Initializes the cluster.
   */
  public void start()
    throws ConfigException
  {
    String serverId = _serverIdLocal.get();

    if (serverId == null)
      serverId = "";

    boolean isActive;

    ClusterServer self = findServer(serverId);

    if (self != null) {
      _clusterLocal.set(this);
      isActive = true;
    }
    else if (_clusterLocal.get() == null && _serverList.size() == 0) {
      // if it's the empty cluster, add it
      _clusterLocal.set(this);
      isActive = true;
    }
    else
      isActive = false;

    try {
      String name = _id;

      if (name == null)
        name = "";

      ObjectName objectName = Jmx.getObjectName("type=Cluster,name=" + name);

      _admin = new ClusterAdmin(this);

      Jmx.register(_admin, objectName);

      _objectName = objectName;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    }

    for (ClusterServer server : _serverList) {
      try {
        server.init();
      } catch (Exception e) {
        throw ConfigException.create(e);
      }
    }
  }

  /**
   * Returns the server id.
   */
  public static String getServerId()
  {
    return _serverIdLocal.get();
  }

  /**
   * Returns the JMX object name.
   */
  public ObjectName getObjectName()
  {
    return _objectName == null ? null : _objectName;
  }

  /**
   * Returns the server corresponding to the current server-id.
   */
  public ClusterServer getSelfServer()
  {
    _serverId = _serverIdLocal.get();

    return getServer(_serverId);
  }

  /**
   * Returns the server list.
   */
  public ClusterServer []getServerList()
  {
    return _serverArray;
  }

  /**
   * Returns the machine list.
   */
  public ArrayList<Machine> getMachineList()
  {
    return _machineList;
  }

  /**
   * Returns the server in the cluster with the given server-id.
   */
  public ClusterServer getServer(String serverId)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getId().equals(serverId))
        return server;
    }

    return null;
  }

  /**
   * Returns the server with the matching index.
   */
  public ClusterServer getServer(int index)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getIndex() == index)
        return server;
    }

    return null;
  }

  /**
   * Returns the matching ports.
   */
  public ArrayList<ClusterPort> getServerPorts(String serverId)
  {
    ArrayList<ClusterPort> ports = new ArrayList<ClusterPort>();

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null) {
        ClusterPort port = server.getClusterPort();

        if (port.getServerId().equals(serverId))
          ports.add(port);
      }
    }

    return ports;
  }

  /**
   * Starts the server.
   */
  Server startServer(ClusterServer clusterServer)
    throws StartLifecycleException
  {
    synchronized (this) {
      if (_server != null)
	return _server;

      Server server = null;

      try {
	Class proServer = Class.forName("com.caucho.server.cluster.ProServer");

	Constructor ctor = proServer.getConstructor(new Class[] { ClusterServer.class });

	server = (Server) ctor.newInstance(clusterServer);
      } catch (Exception e) {
	log.log(Level.FINER, e.toString(), e);
      }

      if (server == null)
	server = new Server(clusterServer);

      _serverProgram.configure(server);

      server.start();

      _server = server;

      return server;
    }
  }

  /**
   * Generate the primary, secondary, tertiary, returning the value encoded
   * in a long.
   */
  public long generateBackupCode(int index)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;
    ArrayList<Machine> machineList = getMachineList();
    int machineLength = machineList.size();

    long backupCode = index;

    long backupLength = srunLength;
    if (backupLength < 3)
      backupLength = 3;
    int backup;

    if (srunLength <= 1) {
      backup = 0;
      backupCode |= 1L << 16;
    }
    else if (srunLength == 2) {
      backup = 0;
      
      backupCode |= ((index + 1L) % 2) << 16;
    }
    else if (machineLength == 1) {
      int sublen = srunLength - 1;
      if (sublen > 7)
	sublen = 7;
	
      backup = RandomUtil.nextInt(sublen);
      
      backupCode |= ((index + backup + 1L) % backupLength) << 16;
    }
    else {
      ClusterServer primaryServer = srunList[index];
      int machineIndex = primaryServer.getMachine().getIndex();
      int sublen = machineLength - 1;
      if (sublen > 7)
	sublen = 7;
	
      int backupMachine = ((machineIndex + RandomUtil.nextInt(sublen) + 1)
			   % machineLength);

      Machine machine = machineList.get(backupMachine);
      ArrayList<ClusterServer> serverList = machine.getServerList();

      ClusterServer server;

      if (serverList.size() > 1)
	server = serverList.get(RandomUtil.nextInt(serverList.size()));
      else
	server = serverList.get(0);

      backup = (int) (server.getIndex() - index + srunLength) % srunLength - 1;
      
      backupCode |= ((index + backup + 1L) % backupLength) << 16;
    }

    if (srunLength <= 2)
      backupCode |= 2L << 32;
    else {
      int sublen = srunLength - 2;
      if (sublen > 6)
	sublen = 6;

      int third = RandomUtil.nextInt(sublen);

      if (backup <= third)
	third += 1;

      backupCode |= ((index + third + 1) % backupLength) << 32;
    }

    return backupCode;
  }

  /**
   * Adds the primary/backup/third digits to the id.
   */
  public void generateBackupCode(StringBuilder cb, long backupCode)
  {
    addDigit(cb, (int) (backupCode & 0xffff));
    addDigit(cb, (int) ((backupCode >> 16) & 0xffff));
    addDigit(cb, (int) ((backupCode >> 32) & 0xffff));
  }

  /**
   * Returns the primary server.
   */
  public ClusterServer getPrimary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength < 64) {
      index = decode(id.charAt(offset + 0));
    }
    else {
      int d1 = decode(id.charAt(offset + 0));
      int d2 = decode(id.charAt(offset + 1));
      
      index = d1 * 64 + d2;
    }

    if (index < srunLength)
      return srunList[index];
    else
      return null;
  }

  /**
   * Returns the secondary server.
   */
  public ClusterServer getSecondary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength < 64) {
      index = decode(id.charAt(offset + 1));
    }
    else {
      int d1 = decode(id.charAt(offset + 2));
      int d2 = decode(id.charAt(offset + 3));
      
      index = d1 * 64 + d2;
    }

    if (index < srunLength)
      return srunList[index];
    else
      return null;
  }

  /**
   * Returns the tertiary server.
   */
  public ClusterServer getTertiary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength < 64) {
      index = decode(id.charAt(offset + 2));
    }
    else {
      int d1 = decode(id.charAt(offset + 4));
      int d2 = decode(id.charAt(offset + 5));
      
      index = d1 * 64 + d2;
    }

    if (index < srunLength)
      return srunList[index];
    else
      return null;
  }

  private void addDigit(StringBuilder cb, int digit)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;
    
    if (srunLength < 64)
      cb.append(convert(digit));
    else {
      cb.append(convert(digit / 64));
      cb.append(convert(digit));
    }
  }

  /**
   * Handles the case where a class loader has completed initialization
   */
  public void classLoaderInit(DynamicClassLoader loader)
  {
  }

  /**
   * Handles the case where a class loader is dropped.
   */
  public void classLoaderDestroy(DynamicClassLoader loader)
  {
  }

  public void startPersistentStore()
  {
    try {
      if (_clusterStore != null)
        _clusterStore.start();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentBind(EnvironmentClassLoader loader)
  {
  }
  
 /**
   * Handles the case where the environment is starting (after init).
   */
  public void environmentStart(EnvironmentClassLoader loader)
  {
  }

  /**
   * Handles the case where the environment is stopping
   */
  public void environmentStop(EnvironmentClassLoader loader)
  {
    try {
      close();
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Closes the cluster.
   */
  public void close()
  {
    synchronized (this) {
      if (_isClosed)
        return;

      _isClosed = true;
    }

    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      try {
        if (server != null)
          server.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  public String toString()
  {
    return "Cluster[" + _id + "]";
  }


  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;
    
    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
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
      return _id;
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRoot()
    {
      return Cluster.this.getRootDirectory();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDir()
    {
      return getRoot();
    }

    /**
     * Returns the root directory.
     *
     * @return root directory
     */
    public Path getRootDirectory()
    {
      return getRoot();
    }
  }

  public class ServerVar {
    private final ClusterServer _server;

    public ServerVar(ClusterServer server)
    {
      _server = server;
    }

    public String getId()
    {
      return _server.getId();
    }

    private int getPort(Port port)
    {
      if (port == null)
        return 0;

      return port.getPort();
    }

    private String getAddress(Port port)
    {
      if (port == null)
        return null;

      String address = port.getAddress();

      if (address == null || address.length() == 0)
        address = "INADDR_ANY";

      return address;
    }

    private Port getFirstPort(String protocol, boolean isSSL)
    {
      if (_server.getPorts() == null)
        return null;

      for (Port port : _server.getPorts()) {
        if (protocol.equals(port.getProtocolName()) && (port.isSSL() == isSSL))
          return port;
      }

      return null;
    }

    public String getAddress()
    {
      return getAddress(_server.getClusterPort());
    }

    public int getPort()
    {
      return getPort(_server.getClusterPort());
    }

    public String getHttpAddress()
    {
      return getAddress(getFirstPort("http", false));
    }

    public int getHttpPort()
    {
      return getPort(getFirstPort("http", false));
    }


    public String getHttpsAddress()
    {
      return getAddress(getFirstPort("http", true));
    }

    public int getHttpsPort()
    {
      return getPort(getFirstPort("http", true));
    }

    /**
     * @deprecated backwards compat.
     */
    public Path getRoot()
    {
      Resin resin =  Resin.getLocal();

      return resin == null ? Vfs.getPwd() : resin.getRootDirectory();
    }
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
