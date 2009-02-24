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
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.SchemaBean;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.lifecycle.StartLifecycleException;
import com.caucho.loader.DynamicClassLoader;
import com.caucho.loader.Environment;
import com.caucho.loader.EnvironmentBean;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.EnvironmentListener;
import com.caucho.loader.EnvironmentLocal;
import com.caucho.management.server.ClusterMXBean;
import com.caucho.management.server.EnvironmentMXBean;
import com.caucho.server.distcache.DistributedCacheManager;
import com.caucho.server.port.Port;
import com.caucho.server.resin.Resin;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.RandomUtil;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.reflect.*;
import javax.annotation.PostConstruct;

/**
 * Defines a set of clustered servers.
 */
abstract public class Cluster
  implements EnvironmentBean, EnvironmentListener, SchemaBean
{
  private static final L10N L = new L10N(Cluster.class);
  private static final Logger log = Logger.getLogger(Cluster.class.getName());

  // private static final int DECODE[];
  
  private String _id = "";
  
  private Resin _resin;

  private EnvironmentClassLoader _classLoader;
  private Path _rootDirectory;

  private ClusterAdmin _admin;

  private ArrayList<ContainerProgram> _serverDefaultList
    = new ArrayList<ContainerProgram>();

  private ContainerProgram _serverProgram
    = new ContainerProgram();

  private final Lifecycle _lifecycle = new Lifecycle();

  protected Cluster(Resin resin)
  {
    if (resin == null)
      throw new NullPointerException(L.l("resin argument is required"));
    
    _resin = resin;
    
    _classLoader = EnvironmentClassLoader.create("cluster:??");

    Environment.addEnvironmentListener(this, resin.getClassLoader());

    Config.setProperty("cluster", new Var(), _classLoader);

    _rootDirectory = Vfs.getPwd();
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
   * Returns the owning resin container.
   */
  public Resin getResin()
  {
    return _resin;
  }

  /**
   * Returns the server
   */
  public Server getServer()
  {
    return getResin().getServer();
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
    log.warning(L.l("{0}: dynamic-server-enable requires Resin Professional",
		    this));
  }

  /**
   * Enables dynamic servers
   */
  public boolean isDynamicServerEnable()
  {
    return false;
  }

  /**
   * Returns the version
   */
  public long getVersion()
  {
    return 0;
  }

  /**
   * Returns the admin.
   */
  public ClusterMXBean getAdmin()
  {
    return _admin;
  }

  /**
   * Returns the list of pods for the cluster
   */
  abstract public ClusterPod []getPodList();

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String id)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServer(id);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(int podIndex,
				  int index)
  {
    for (ClusterPod pod : getPodList()) {
      if (pod.getIndex() == podIndex) {
	for (ClusterServer server : pod.getServerList()) {
	  if (server.getIndex() == index)
	    return server;
	}

	return null;
      }
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterPod findPod(int podIndex)
  {
    for (ClusterPod pod : getPodList()) {
      if (pod.getIndex() == podIndex) {
	return pod;
      }
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServerByPrefix(String prefix)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServerByPrefix(prefix);

      if (server != null)
        return server;
    }

    return null;
  }

  /**
   * Finds the first server with the given server-id.
   */
  public ClusterServer findServer(String address, int port)
  {
    for (ClusterPod pod : getPodList()) {
      ClusterServer server = pod.findServer(address, port);

      if (server != null)
        return server;
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
   * Adds a new pod to the cluster
   */
  public ClusterPod createPod()
  {
    throw new UnsupportedOperationException(L.l("<pod> requires Resin Professional"));
  }

  /**
   * Adds a new server to the cluster.
   */
  public Machine createMachine()
  {
    throw new UnsupportedOperationException(L.l("<machine> requires Resin Professional"));
  }

  /**
   * Adds a new server to the cluster during configuration.
   */
  abstract public ClusterServer createServer();

  /**
   * Adds a new server to the cluster during configuration.
   */
  public void addServer(ClusterServer server)
  {
  }
  
  /*
  protected ClusterServer createDynamicServer()
  {
    throw new UnsupportedOperationException(L.l("{0}: createDynamicServer requires Resin Professional",
						this));
  }

  ClusterServer createStaticServer(ClusterServer server)
  {
    if (_lifecycle.isActive())
      throw new IllegalStateException(L.l("{0}: can't create static server after initialization", this));
    
    server.setIndex(_staticServerList.size());

    configureServerDefault(server);

    return server;
  }

  protected ClusterServer createDynamicServer(ClusterServer server)
  {
    throw new UnsupportedOperationException(L.l("{0}: createDynamicServer requires Resin Professional",
						this));
  }
  */

  /**
   * Configure the default values for the server
   */
  protected void configureServerDefault(ClusterServer server)
  {
    for (int i = 0; i < _serverDefaultList.size(); i++)
      _serverDefaultList.get(i).configure(server);
  }

  /**
   * Adds a new server to the cluster.
   */
  public void addDynamicServer(String serverId, String address, int port)
    throws ConfigException
  {
    throw new UnsupportedOperationException(L.l("{0}: dynamic servers require Resin Professional", this));
  }

  /**
   * Adds a new static server to the cluster.
   */
  /*
  protected void addServerImpl(ClusterServer server)
    throws ConfigException
  {
    ClusterServer oldServer = findServer(server.getId());

    if (oldServer != null)
      throw new ConfigException(L.l("{0}: duplicate <server> with id='{1}'",
				    this, server.getId()));
    
    _serverList.add(server);
    _serverArray = new ClusterServer[_serverList.size()];
    _serverList.toArray(_serverArray);

    if (server.getId().equals(_serverId)) {
      _selfServer = server;

      Config.setProperty("server", new ServerVar(server));
    }
  }
  */

  protected void setSelfServer(ClusterServer server)
  {
    // XXX: move to Server
    
    /*
    if (! _serverId.equals(server.getId()))
      throw new IllegalStateException(L.l("{0}: self server {1} does not match server id {2}",
					  this, server, _serverId));
    
    _selfServer = server;

    Config.setProperty("server", new ServerVar(server), _classLoader);
    */
  }

  /*
  protected void removeServerImpl(int index)
    throws ConfigException
  {
    try {
      synchronized (this) {
	_serverList.set(index, null);
	_serverArray = new ClusterServer[_serverList.size()];
	_serverList.toArray(_serverArray);
      }
    } catch (Exception e) {
      throw ConfigException.create(e);
    }
  }
  */

  /**
   * Adds a new server to the cluster.
   */
  public void removeDynamicServer(ClusterServer server)
    throws ConfigException
  {
    throw new UnsupportedOperationException(L.l("{0}: dynamic servers require Resin Professional", this));
  }

  /**
   * Adds a srun server.
   */
  /*
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
  */
  
  /**
   * Returns the owning pod for a cluster server.
   * 
   * @return the corresponding pod
   */
  /*
  public ClusterPod getPod(ClusterServer server)
  {
    return _pod;
  }
  */

  /**
   * Returns the distributed cache manager.
   */
  public DistributedCacheManager getDistributedCacheManager()
  {
    return getResin().getServer().getDistributedCacheManager();
  }

  /**
   * Adds a program.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
    _serverProgram.addProgram(program);
  }

  //
  // lifecycle
  //

  /**
   * Returns true if the cluster is active
   */
  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  /**
   * Initializes the cluster.
   */
  @PostConstruct
  public void init()
    throws ConfigException
  {
    _lifecycle.toInit();

    /*
    String serverId = _serverIdLocal.get();

    if (serverId == null)
      serverId = "";

    ClusterServer self = findServer(serverId);
    */

    /*
    if (self != null) {
      _clusterLocal.set(this);
    }
    else if (_clusterLocal.get() == null && "".equals(serverId)) {
      // if it's the empty cluster, add it
      _clusterLocal.set(this);
    }
    else if (_clusterLocal.get() == null && _serverList.size() == 0) {
      // if it's the empty cluster, add it
      _clusterLocal.set(this);
    }
    */

    _admin = new ClusterAdmin(this);
    _admin.register();

    for (ClusterPod pod : getPodList()) {
      pod.init();
    }
  }

  /**
   * Start the cluster.
   */
  public void start()
    throws ConfigException
  {
    _lifecycle.toActive();

    for (ClusterPod pod : getPodList()) {
      pod.start();
    }
  }

  /**
   * Returns the server list.
   */
  /*
  public ClusterServer []getServerList()
  {
    return _serverArray;
  }
  */

  /**
   * Returns the server with the matching index.
   */
  /*
  public ClusterServer getServer(int index)
  {
    for (int i = 0; i < _serverList.size(); i++) {
      ClusterServer server = _serverList.get(i);

      if (server != null && server.getIndex() == index)
        return server;
    }

    return null;
  }
  */

  /**
   * Returns the matching ports.
   */
  /*
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
  */
  
  /**
   * Starts the server.
   */
  Server startServer(ClusterServer clusterServer)
    throws StartLifecycleException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    try {
      thread.setContextClassLoader(getClassLoader());
      
      Server server = createResinServer(clusterServer);

      _serverProgram.configure(server);

      return server;
    } finally {
      thread.setContextClassLoader(oldLoader);
    }
  }

  protected Server createResinServer(ClusterServer clusterServer)
  {
    return new Server(clusterServer);
  }
  
  //
  // persistent store support
  //

  /**
   * Generate the primary, secondary, tertiary, returning the value encoded
   * in a long.
   */
  /*
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
  */

  /**
   * Adds the primary/backup/third digits to the id.
   */
  /*
  public void generateBackupCode(StringBuilder cb, long backupCode)
  {
    addDigit(cb, (int) (backupCode & 0xffff));
    addDigit(cb, (int) ((backupCode >> 16) & 0xffff));
    addDigit(cb, (int) ((backupCode >> 32) & 0xffff));
  }
  */

  /*
  public void generateBackup(StringBuilder sb, int index)
  {
    generateBackupCode(sb, generateBackupCode(index));
  }
  */

  /**
   * Returns the primary server.
   */
  /*
  public ClusterServer getPrimary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
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
  */

  /**
   * Returns the secondary server.
   */
  /*
  public ClusterServer getSecondary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
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
  */

  /**
   * Returns the tertiary server.
   */
  /*
  public ClusterServer getTertiary(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
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
  */

  /**
   * Returns the primary server.
   */
  /*
  public int getPrimaryIndex(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
      index = decode(id.charAt(offset + 0));
    }
    else {
      int d1 = decode(id.charAt(offset + 0));
      int d2 = decode(id.charAt(offset + 1));
      
      index = d1 * 64 + d2;
    }

    return index;
  }
  */

  /**
   * Returns the secondary server.
   */
  /*
  public int getSecondaryIndex(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
      index = decode(id.charAt(offset + 1));
    }
    else {
      int d1 = decode(id.charAt(offset + 2));
      int d2 = decode(id.charAt(offset + 3));
      
      index = d1 * 64 + d2;
    }

    return index;
  }
  */

  /**
   * Returns the tertiary server.
   */
  /*
  public int getTertiaryIndex(String id, int offset)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;

    int index = 0;

    if (srunLength <= 64) {
      index = decode(id.charAt(offset + 2));
    }
    else {
      int d1 = decode(id.charAt(offset + 4));
      int d2 = decode(id.charAt(offset + 5));
      
      index = d1 * 64 + d2;
    }

    return index;
  }
  */

  /*
  private void addDigit(StringBuilder cb, int digit)
  {
    ClusterServer []srunList = getServerList();
    int srunLength = srunList.length;
    
    if (srunLength <= 64)
      cb.append(convert(digit));
    else {
      cb.append(convert(digit / 64));
      cb.append(convert(digit));
    }
  }
  */

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

  /**
   * Start any work in notifying other members in the cluster
   * that the server is active.
   */
  public void startRemote()
  {
  }

  /**
   * Handles the case where the environment is configured (after init).
   */
  public void environmentConfigure(EnvironmentClassLoader loader)
  {
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
    if (! _lifecycle.toDestroy())
      return;

    for (ClusterPod pod : getPodList()) {
      try {
        if (pod != null)
          pod.close();
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _id + "]";
  }

  /**
   * Converts an integer to a printable character
   */
  /*
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
  */

  /*
  public static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }
  */
  
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

  /*
  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
  */
}
