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

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.logging.*;
import java.net.*;

import javax.annotation.*;
import javax.el.*;
import javax.management.ObjectName;

import com.caucho.config.*;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.InitProgram;
import com.caucho.config.types.Period;
import com.caucho.el.EL;
import com.caucho.el.MapVariableResolver;
import com.caucho.el.SystemPropertiesResolver;
import com.caucho.jsp.cfg.JspPropertyGroup;
import com.caucho.license.*;
import com.caucho.lifecycle.*;
import com.caucho.loader.*;
import com.caucho.management.j2ee.*;
import com.caucho.management.server.*;
import com.caucho.naming.*;
import com.caucho.server.cluster.*;
import com.caucho.transaction.cfg.TransactionManagerConfig;
import com.caucho.util.*;
import com.caucho.log.*;
import com.caucho.vfs.*;
import com.caucho.server.vfs.*;

/**
 * The Resin class represents the top-level container for Resin.
 * It exactly matches the &lt;resin> tag in the resin.conf
 */
public class Resin implements EnvironmentBean, SchemaBean
{
  private static Logger _log;
  private static L10N _L;

  private static final String OBJECT_NAME= "resin:type=Resin";

  private static final EnvironmentLocal<Resin> _resinLocal =
    new EnvironmentLocal<Resin>();

  private final EnvironmentLocal<String> _serverIdLocal =
    new EnvironmentLocal<String>("caucho.server-id");

  private ObjectName _objectName;

  private EnvironmentClassLoader _classLoader;
  private boolean _isGlobal;

  private String _serverId = "";
  private String _configFile = "conf/resin.conf";

  private Path _resinHome;
  private Path _rootDirectory;

  private boolean _isGlobalSystemProperties;
  private boolean _isResinProfessional;

  private long _minFreeMemory = 2 * 1024L * 1024L;
  private long _shutdownWaitMax = 60000L;

  private SecurityManager _securityManager;

  private HashMap<String,Object> _variableMap = new HashMap<String,Object>();

  private ArrayList<InitProgram> _clusterDefaults
    = new ArrayList<InitProgram>();

  private ArrayList<Cluster> _clusters
    = new ArrayList<Cluster>();

  private Lifecycle _lifecycle;

  private Server _server;

  private long _initialStartTime;
  private long _startTime;
  private J2EEDomain _j2eeDomainManagedObject;
  private JVM _jvmManagedObject;

  private String _resinConf = "conf/resin.conf";
  private String _configServer;

  private Path _resinRoot;

  private ClassLoader _systemClassLoader;
  private Thread _mainThread;

  private ArrayList<BoundPort> _boundPortList
    = new ArrayList<BoundPort>();

  private InputStream _waitIn;

  private Socket _pingSocket;

  /**
   * Creates a new resin server.
   */
  public Resin()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  /**
   * Creates a new resin server.
   */
  public Resin(ClassLoader loader)
  {
    Environment.init();

    if (loader == null)
      loader = ClassLoader.getSystemClassLoader();

    _isGlobal = (loader == ClassLoader.getSystemClassLoader());

    if (loader instanceof EnvironmentClassLoader)
      _classLoader = (EnvironmentClassLoader) loader;
    else
      _classLoader = new EnvironmentClassLoader();

    _resinLocal.set(this, _classLoader);

    _lifecycle = new Lifecycle(log(), "Resin[]");

    _startTime = Alarm.getCurrentTime();

    String resinHome = System.getProperty("resin.home");

    if (resinHome != null)
      setResinHome(Vfs.lookup(resinHome));
    else
      setResinHome(Vfs.getPwd());


    String resinRoot = System.getProperty("resin.root");

    if (resinRoot != null)
      setRootDirectory(Vfs.lookup(resinRoot));
    else {
      String serverRoot = System.getProperty("server.root");

      if (serverRoot != null)
        setRootDirectory(Vfs.lookup(serverRoot));
      else
        setRootDirectory(Vfs.getPwd());
    }

    _variableMap.put("resin", new Var());
    _variableMap.put("server", new Var());
    _variableMap.put("java", new JavaVar());

    ELResolver varResolver = new SystemPropertiesResolver();
    ConfigELContext elContext = new ConfigELContext(varResolver);
    elContext.push(new MapVariableResolver(_variableMap));

    EL.setEnvironment(elContext);
    EL.setVariableMap(_variableMap, _classLoader);

    _variableMap.put("fmt", new com.caucho.config.functions.FmtFunctions());

    try {
      _variableMap.put("jndi", Jndi.class.getMethod("lookup", new Class[] { String.class }));
      _variableMap.put("jndi:lookup", Jndi.class.getMethod("lookup", new Class[] { String.class }));
    } catch (Exception e) {
      throw new ConfigException(e);
    }

    ThreadPoolAdmin.create();

    new ResinAdmin(this);
  }

  /**
   * Returns the resin server.
   */
  public static Resin getLocal()
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
    _variableMap.put("serverId", serverId);

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
      controller.setResin(this);

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
  }

  /**
   * Sets the group name for setuid.
   */
  public void setGroupName(String groupName)
  {
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
      throw new ConfigException(L().l("security-provider {0} must implement java.security.Provider",
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
  @PostConstruct
  public void init()
  {
    _lifecycle.toInit();
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
      throw new ConfigException(L().l("server-id '{0}' has no matching <server> definition.",
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
      log().warning(L().l("-server \"{0}\" has no matching http or srun ports.  Check the resin.conf and -server values.",
                      _serverId));
    }
    */

    log().info("Resin started in " + (Alarm.getCurrentTime() - _startTime) + "ms");
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

    try {
      // notify watchdog thread before starting shutdown
      synchronized (this) {
	notifyAll();
      }

      Socket socket = _pingSocket;

      if (socket != null)
	socket.setSoTimeout(1000);
    } catch (Throwable e) {
      log().log(Level.WARNING, e.toString(), e);
    }

    try {
      Server server = _server;
      _server = null;

      if (server != null)
	server.destroy();
    } catch (Throwable e) {
      log().log(Level.WARNING, e.toString(), e);
    }

    try {
      if (_isGlobal)
	Environment.closeGlobal();
      else
	_classLoader.destroy();
    } finally {
      _lifecycle.toDestroy();
    }
  }

  /**
   * Create a new Resin server.
   *
   * @param argv the command-line to initialize Resin with
   * @param isHttp default to http
   */
  /*
  public Resin(String []argv)
    throws Exception
  {
    _systemClassLoader = Thread.currentThread().getContextClassLoader();
    _startTime = Alarm.getCurrentTime();

    _resinHome = CauchoSystem.getResinHome();
    _serverRoot = _resinHome;

    parseCommandLine(argv);
  }
  */

  private void parseCommandLine(String []argv)
    throws Exception
  {
    int len = argv.length;
    int i = 0;

    while (i < len) {
      RandomUtil.addRandom(argv[i]);

      if (i + 1 < len &&
          (argv[i].equals("-stdout") ||
           argv[i].equals("--stdout"))) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
	stream.init();
	WriteStream out = stream.getStream();
	out.setDisableClose(true);

        EnvironmentStream.setStdout(out);

	i += 2;
      }
      else if (i + 1 < len &&
               (argv[i].equals("-stderr") ||
                argv[i].equals("--stderr"))) {
        Path path = Vfs.lookup(argv[i + 1]);

        RotateStream stream = RotateStream.create(path);
	stream.init();
	WriteStream out = stream.getStream();
	out.setDisableClose(true);

        EnvironmentStream.setStderr(out);

	i += 2;
      }
      else if (i + 1 < len &&
               (argv[i].equals("-conf") ||
                argv[i].equals("--conf"))) {
        _resinConf = argv[i + 1];
	i += 2;
      }
      else if (i + 1 < len &&
               (argv[i].equals("-server") ||
                argv[i].equals("--server"))) {
	_serverId = argv[i + 1];
	i += 2;
      }
      else if (argv[i].equals("-version")
	       || argv[i].equals("--version")) {
	System.out.println(com.caucho.Version.FULL_VERSION);
	System.exit(66);
      }
      else if (argv[i].equals("-verbose")
	       || argv[i].equals("--verbose")) {
	i += 1;
      }
      else if (argv[i].equals("-resin-home")
	       || argv[i].equals("--resin-home")) {
	_resinHome = Vfs.lookup(argv[i + 1]);

	i += 2;
      }
      else if (argv[i].equals("-resin-root")
               || argv[i].equals("--resin-root")) {
        _resinRoot = _resinHome.lookup(argv[i + 1]);

        i += 2;
      }
      else if (argv[i].equals("-server-root")
	       || argv[i].equals("--server-root")) {
	_resinRoot = _resinHome.lookup(argv[i + 1]);

	i += 2;
      }
      else if (argv[i].equals("-config-server") ||
               argv[i].equals("--config-server")) {
        _configServer = argv[i + 1];
        i += 2;
      }
      else if (argv[i].equals("-socketwait") ||
               argv[i].equals("--socketwait") ||
               argv[i].equals("-pingwait") ||
               argv[i].equals("--pingwait")) {
        int socketport = Integer.parseInt(argv[i + 1]);

        Socket socket = null;
        for (int k = 0; k < 15 && socket == null; k++) {
          try {
            socket = new Socket("127.0.0.1", socketport);
          } catch (Throwable e) {
	    System.out.println(new Date());
	    e.printStackTrace();
          }

          if (socket == null)
            Thread.sleep(1000);
        }

        if (socket == null) {
          System.err.println("Can't connect to parent process through socket " + socketport);
          System.err.println("Resin needs to connect to its parent.");
          System.exit(0);
        }

	if (argv[i].equals("-socketwait") || argv[i].equals("--socketwait"))
          _waitIn = socket.getInputStream();
	else
	  _pingSocket = socket;

        socket.setSoTimeout(60000);

	i += 2;
      }
      else if ("-port".equals(argv[i]) || "--port".equals(argv[i])) {
        int fd = Integer.parseInt(argv[i + 1]);
	String addr = argv[i + 2];
	if ("null".equals(addr))
	  addr = null;
	int port = Integer.parseInt(argv[i + 3]);

	_boundPortList.add(new BoundPort(QJniServerSocket.openJNI(fd, port),
					 addr,
					 port));

	i += 4;
      }
      else if ("start".equals(argv[i])
	       || "restart".equals(argv[i])) {
	i++;
      }
      else {
        System.out.println(L().l("unknown argument `{0}'", argv[i]));
        System.out.println();
	usage();
	System.exit(66);
      }
    }
  }

  private static void usage()
  {
    System.err.println(L().l("usage: Resin [-conf resin.conf] [-server id]"));
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void initMain()
    throws Throwable
  {
    _mainThread = Thread.currentThread();
    _mainThread.setContextClassLoader(_systemClassLoader);

    addRandom();

    System.out.println(com.caucho.Version.FULL_VERSION);
    System.out.println(com.caucho.Version.COPYRIGHT);
    System.out.println();

    boolean isResinProfessional = false;

    try {
      Class cl = Class.forName("com.caucho.license.LicenseCheckImpl",
			       false,
			       ClassLoader.getSystemClassLoader());

      LicenseCheck license = (LicenseCheck) cl.newInstance();

      try {
	license.validate(0);

	license.doLogging(1);

	license.validate(1);

	isResinProfessional = true;
	System.setProperty("isResinProfessional", "true");

	Vfs.initJNI();

	// license.doLogging(1);
      } catch (Throwable e) {
	String msg;

	if (e instanceof ConfigException)
	  msg = e.getMessage() + "\n";
	else {
	  e.printStackTrace();

	  msg = e.toString() + "\n";

	  log().log(Level.WARNING, e.toString(), e);
	}

	log().log(Level.FINE, e.toString(), e);

	msg += L().l("\n" +
		     "Using Resin Open Source under the GNU Public License (GPL).\n" +
		     "\n" +
		     "  See http://www.caucho.com for information on Resin Professional.\n");

	log().warning(msg);
	System.err.println(msg);
      }
    } catch (Throwable e) {
      log().log(Level.FINER, e.toString(), e);

      String msg = L().l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n" +
			 "\n" +
			 "  See http://www.caucho.com for information on Resin Professional,\n" +
			 "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n");

      log().warning(msg);
      System.err.println(msg);
    }

    System.out.println("Starting Resin on " + QDate.formatLocal(Alarm.getCurrentTime()));
    System.out.println();

    EnvironmentClassLoader.initializeEnvironment();

    // buildResinClassLoader();

    // validateEnvironment();

    if (_classLoader != null)
      _mainThread.setContextClassLoader(_classLoader);

    /*
    if (isResinProfessional && _configServer != null) {
      Path dbDir = Vfs.lookup("work/config");

      Class cl = Class.forName("com.caucho.vfs.remote.RemotePath");
      Constructor ctor = cl.getConstructor(new Class[] { String.class,
							 Path.class,
							  String.class });

      Path path = (Path) ctor.newInstance(_configServer, dbDir, _serverId);

      ConfigPath.setRemote(path);
      log().info("Using configuration from " + _configServer);
    }
    */

    Path resinConf = _resinHome.lookup(_resinConf);

    Vfs.setPwd(_resinRoot);
    // server.setServerRoot(_serverRoot);

    setResinProfessional(isResinProfessional);

    _mainThread.setContextClassLoader(_systemClassLoader);

    Config config = new Config();
    // server/10hc
    // config.setResinInclude(true);

    config.configure(this, Vfs.lookup(_resinConf), getSchema());

    ClusterServer clusterServer = findClusterServer(_serverId);
    for (int i = 0; i < _boundPortList.size(); i++) {
      BoundPort port = _boundPortList.get(i);

      clusterServer.bind(port.getAddress(),
			 port.getPort(),
			 port.getServerSocket());
    }

    start();
  }

  private void addRandom()
  {
    RandomUtil.addRandom(System.currentTimeMillis());
    RandomUtil.addRandom(Runtime.getRuntime().freeMemory());

    RandomUtil.addRandom(System.identityHashCode(_mainThread));
    RandomUtil.addRandom(System.identityHashCode(_systemClassLoader));
    RandomUtil.addRandom(com.caucho.Version.FULL_VERSION);

    try {
      RandomUtil.addRandom(InetAddress.getLocalHost().toString());
    } catch (Throwable e) {
    }

    // for systems with /dev/urandom, read more bits from it.
    try {
      InputStream is = new FileInputStream("/dev/urandom");

      for (int i = 0; i < 16; i++)
	RandomUtil.addRandom(is.read());

      is.close();
    } catch (Throwable e) {
    }

    RandomUtil.addRandom(System.currentTimeMillis());
  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  public void waitForExit()
  {
    int socketExceptionCount = 0;
    Integer memoryTest;
    Runtime runtime = Runtime.getRuntime();

    /*
     * If the server has a parent process watching over us, close
     * gracefully when the parent dies.
     */
    while (! isClosing()) {
      try {
	Thread.sleep(10);

	long minFreeMemory = getMinFreeMemory();

	if (minFreeMemory <= 0) {
	  // memory check disabled
	}
	else if (2 * minFreeMemory < getFreeMemory(runtime)) {
	  // plenty of free memory
	}
	else {
	  if (log().isLoggable(Level.FINER)) {
	    log().finer(L().l("free memory {0} max:{1} total:{2} free:{3}",
			  "" + getFreeMemory(runtime),
			  "" + runtime.maxMemory(),
			  "" + runtime.totalMemory(),
			  "" + runtime.freeMemory()));
	  }

	  log().info(L().l("Forcing GC due to low memory. {0} free bytes.",
		       getFreeMemory(runtime)));

	  runtime.gc();

	  Thread.sleep(1000);

	  runtime.gc();

	  if (getFreeMemory(runtime) < minFreeMemory) {
	    log().severe(L().l("Restarting due to low free memory. {0} free bytes",
			   getFreeMemory(runtime)));
	    destroy();
	    return;
	  }
	}

        // second memory check
        memoryTest = new Integer(0);

	long alarmTime = Alarm.getCurrentTime();
	long systemTime = System.currentTimeMillis();

	long diff = alarmTime - systemTime;
	if (diff < 0)
	  diff = -diff;

	// The time difference needs to be fairly large because
	// GC might take a good deal of time.
	if (10 * 60000L < diff) {
	  log().severe(L().l("Restarting due to frozen Resin timer manager thread (Alarm).  This error generally indicates a JVM freeze, not an application deadlock."));
	  Runtime.getRuntime().halt(1);
	}

	if (_waitIn != null) {
          int len;
          if ((len = _waitIn.read()) >= 0) {
            socketExceptionCount = 0;
          }

	  return;
        }
        else {
	  synchronized (this) {
	    wait(10000);
	  }
        }
      } catch (SocketTimeoutException e) {
        socketExceptionCount = 0;
      } catch (InterruptedIOException e) {
        socketExceptionCount = 0;
      } catch (InterruptedException e) {
        socketExceptionCount = 0;
      } catch (SocketException e) {
        // The Solaris JVM will throw SocketException periodically
        // instead of interrupted exception, so those exceptions need to
        // be ignored.

        // However, the Windows JVMs will throw connection reset by peer
        // instead of returning an end of file in the read.  So those
        // need to be trapped to close the socket.
        if (socketExceptionCount++ == 0) {
          log().log(Level.FINE, e.toString(), e);
        }
        else if (socketExceptionCount > 100)
          return;
      } catch (OutOfMemoryError e) {
	try {
	  System.err.println("Out of memory");
	} finally {
	  Runtime.getRuntime().halt(1);
	}
      } catch (Throwable e) {
        log().log(Level.FINE, e.toString(), e);

        return;
      }
    }
  }

  private static long getFreeMemory(Runtime runtime)
  {
    long maxMemory = runtime.maxMemory();
    long totalMemory = runtime.totalMemory();
    long freeMemory = runtime.freeMemory();

    // Some JDKs (JRocket) return 0 for the maxMemory
    if (maxMemory < totalMemory)
      return freeMemory;
    else
      return maxMemory - totalMemory + freeMemory;
  }

  /**
   * Shuts the server down.
   */
  public static void shutdown()
  {
    Resin resin = getLocal();

    if (resin != null) {
      resin.destroy();
    }
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.conf   : alternate configuration file
   * -port port         : set the server's portt
   * <pre>
   */
  public static void main(String []argv)
  {
    try {
      validateEnvironment();

      final Resin resin = new Resin();

      resin.parseCommandLine(argv);

      resin.initMain();

      resin.waitForExit();

      System.err.println(L().l("closing server"));

      new Thread() {
	public void run()
	{
	  setName("resin-destroy");

	  resin.destroy();
	}
      }.start();

      Server server = resin.getServer();

      long stopTime = System.currentTimeMillis();
      long endTime = stopTime +	15000L;

      if (server != null)
	endTime = stopTime + server.getShutdownWaitMax() ;

      while (System.currentTimeMillis() < endTime && ! resin.isClosed()) {
	try {
	  Thread.interrupted();
	  Thread.sleep(100);
	} catch (Throwable e) {
	}
      }

      if (! resin.isClosed())
	Runtime.getRuntime().halt(1);

      System.exit(0);
    } catch (BindException e) {
      System.out.println(e);

      log().log(Level.FINE, e.toString(), e);

      System.exit(67);
    } catch (Throwable e) {
      boolean isCompile = false;
      Throwable cause;

      for (cause = e; cause != null; cause = cause.getCause()) {
	if (cause instanceof CompileException) {
	  System.err.println(cause.getMessage());
	  isCompile = true;
	  break;
	}
      }

      if (! isCompile)
	e.printStackTrace(System.err);
      else
	log().log(Level.CONFIG, e.toString(), e);
    } finally {
      System.exit(1);
    }
  }

  /**
   * Validates the environment.
   */
  private static void validateEnvironment()
    throws ConfigException
  {
    String loggingManager = System.getProperty("java.util.logging.manager");

    if (loggingManager == null ||
	! loggingManager.equals("com.caucho.log.LogManagerImpl")) {
      throw new ConfigException(L().l("The following system property must be set:\n  -Djava.util.logging.manager=com.caucho.log.LogManagerImpl\nThe JDK 1.4 Logging manager must be set to Resin's log manager."));
    }

    validatePackage("javax.servlet.Servlet", new String[] {"2.5", "1.5"});
    validatePackage("javax.servlet.jsp.jstl.core.Config", new String[] {"1.1"});
    validatePackage("javax.management.MBeanServer", new String[] { "1.2", "1.5" });
    validatePackage("javax.resource.spi.ResourceAdapter", new String[] {"1.5", "1.4"});
  }

  /**
   * Validates a package version.
   */
  private static void validatePackage(String className, String []versions)
    throws ConfigException
  {
    Class cl = null;

    try {
      cl = Class.forName(className);
    } catch (Throwable e) {
      throw new ConfigException(L().l("class {0} is not loadable on startup.  Resin requires {0} to be in the classpath on startup.",
				    className));

    }

    Package pkg = cl.getPackage();

    if (pkg == null) {
      log().warning(L().l("package for class {0} is missing.  Resin requires class {0} in the classpath on startup.",
			className));

      return;
    }
    else if (pkg.getSpecificationVersion() == null) {
      log().warning(L().l("{0} has no specification version.  Resin {1} requires version {2}.",
				    pkg, com.caucho.Version.VERSION,
				    versions[0]));

      return;
    }

    for (int i = 0; i < versions.length; i++) {
      if (versions[i].compareTo(pkg.getSpecificationVersion()) <= 0)
	return;
    }

    log().warning(L().l("Specification version {0} of {1} is not compatible with Resin {2}.  Resin {2} requires version {3}.",
		      pkg.getSpecificationVersion(),
		      pkg, com.caucho.Version.VERSION,
		      versions[0]));
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(Resin.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(Resin.class.getName());

    return _log;
  }

  static class BoundPort {
    private QServerSocket _ss;
    private String _address;
    private int _port;

    BoundPort(QServerSocket ss, String address, int port)
    {
      if (ss == null)
	throw new NullPointerException();

      _ss = ss;
      _address = address;
      _port = port;
    }

    public QServerSocket getServerSocket()
    {
      return _ss;
    }

    public int getPort()
    {
      return _port;
    }

    public String getAddress()
    {
      return _address;
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
     * Returns the local address
     *
     * @return IP address
     */
    public String getAddress()
    {
      try {
	if (Alarm.isTest())
	  return "127.0.0.1";
	else
	  return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
	log().log(Level.FINE, e.toString(), e);

	return "localhost";
      }
    }

    /**
     * Returns the resin config.
     */
    public Path getConf()
    {
      return getHome().lookup(Resin.this.getConfigFile());
    }

    /**
     * Returns the resin home.
     */
    public Path getHome()
    {
      return Resin.this.getResinHome();
    }

    /**
     * Returns the root directory.
     *
     * @return the root directory
     */
    public Path getRoot()
    {
      return Resin.this.getRootDirectory();
    }

    /**
     * Returns the version
     *
     * @return version
     */
    public String getVersion()
    {
      if (Alarm.isTest())
	return "3.1.test";
      else
	return com.caucho.Version.VERSION;
    }

    /**
     * Returns the version
     *
     * @return version
     */
    public String getVersionDate()
    {
      if (Alarm.isTest())
	return "19980508T0251";
      else
	return com.caucho.Version.VERSION_DATE;
    }

    /**
     * Returns the local hostname
     *
     * @return version
     */
    public String getHostName()
    {
      try {
	if (Alarm.isTest())
	  return "localhost";
	else
	  return InetAddress.getLocalHost().getHostName();
      } catch (Exception e) {
	log().log(Level.FINE, e.toString(), e);

	return "localhost";
      }
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

    /**
     * Returns the -server id
     */
    public String getServerId()
    {
      return _serverId;
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
      return true;
    }
    /**
     * Returns the JDK version
     */
    public String getVersion()
    {
      return System.getProperty("java.version");
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
        throw new ConfigException(L().l("policy-file '{0}' must be readable.",
                                      path));

    }

    public void init()
    {
      if (_isEnable)
        System.setSecurityManager(_securityManager);
    }
  }
}
