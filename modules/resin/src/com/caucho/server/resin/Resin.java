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

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import java.lang.reflect.Constructor;

import java.security.AllPermission;

import com.caucho.vfs.*;
import com.caucho.util.RandomUtil;
import com.caucho.util.L10N;
import com.caucho.util.Alarm;
import com.caucho.util.QDate;
import com.caucho.util.CauchoSystem;
import com.caucho.util.CompileException;

import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;

import com.caucho.log.Log;

import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.loader.TreeLoader;
import com.caucho.loader.SimpleLoader;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;

import com.caucho.license.LicenseCheck;

public class Resin implements ResinServerListener {
  private static final L10N L = new L10N(Resin.class);
  private static final Logger log = Log.open(Resin.class);

  private static Resin _resin;
  
  private String _resinConf = "conf/resin.conf";
  private String _configServer;

  private String _serverId = "";

  private ClassLoader _systemClassLoader;
  private Thread _mainThread;
  
  private ResinServer _server;
  private EnvironmentClassLoader _classLoader;

  private long _startTime;
  
  private volatile boolean _isClosed;
  private volatile boolean _isRestarting;

  private InputStream _waitIn;
  
  private Socket _pingSocket;
  
  /**
   * Create a new Resin server.
   *
   * @param argv the command-line to initialize Resin with
   * @param isHttp default to http
   */
  public Resin(String []argv)
    throws Exception
  {
    _systemClassLoader = Thread.currentThread().getContextClassLoader();
    _startTime = Alarm.getCurrentTime();
    
    parseCommandLine(argv);
  }

  /**
   * Returns true if the server has shut down.
   */
  public boolean isClosed()
  {
    return _isClosed;
  }

  /**
   * Returns the server.
   */
  public ResinServer getServer()
  {
    return _server;
  }

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
	WriteStream out = stream.getStream();
	out.setDisableClose(true);

        EnvironmentStream.setStderr(out);
        
	i += 2;
      }
      else if (i + 1 < len && argv[i].equals("-conf")) {
	_resinConf = argv[i + 1];
	i += 2;
      }
      else if (i + 1 < len && argv[i].equals("-server")) {
	_serverId = argv[i + 1];
	i += 2;
      }
      else if (argv[i].equals("-version")) {
	System.out.println(com.caucho.Version.FULL_VERSION);
	System.exit(66);
      }
      else if (argv[i].equals("-config-server")) {
	_configServer = argv[i + 1];
	i += 2;
      }
      else if (argv[i].equals("-socketwait") ||
	       argv[i].equals("-pingwait")) {
	int socketport = Integer.parseInt(argv[i + 1]);

        Socket socket = null;
        for (int k = 0; k < 15 && socket == null; k++) {
          try {
            socket = new Socket("127.0.0.1", socketport);
          } catch (Exception e) {
          }
          if (socket == null)
            Thread.sleep(1000);
        }
        
        if (socket == null) {
          System.err.println("Can't connect to parent process through socket " + socket);
          System.err.println("Resin needs to connect to its parent.");
          System.exit(0);
        }

	if (argv[i].equals("-socketwait"))
	  _waitIn = socket.getInputStream();
	else
	  _pingSocket = socket;
	
        socket.setSoTimeout(60000);
        
	i += 2;
      }
      /*
      else if (compileURLs != null) {
        for (; i < len; i++)
          compileURLs.add(argv[i]);
        break;
      }
      */
      else {
        System.out.println(L.l("unknown argument `{0}'", argv[i]));
        System.out.println();
	usage();
	System.exit(66);
      }
    }
  }

  private static void usage()
  {
    System.err.println(L.l("usage: Resin [-conf resin.conf] [-server id]"));
  }

  /**
   * Initialize the server, binding to TCP and starting the threads.
   */
  public void init()
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

	SchemeMap.initJNI();
      
	// license.doLogging(1);
      } catch (Throwable e) {
	String msg;
	
	if (e instanceof ConfigException)
	  msg = e.getMessage() + "\n";
	else {
	  e.printStackTrace();
	  
	  msg = e.toString() + "\n";
	  
	  log.log(Level.WARNING, e.toString(), e);
	}
	  
	log.log(Level.FINE, e.toString(), e);
      
	msg += L.l("\n" +
		   "Using Resin Open Source under the GNU Public License (GPL).\n" +
		   "\n" +
		   "  See http://www.caucho.com for information on Resin Professional.\n");
    
	log.warning(msg);
	System.err.println(msg);
      }
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
      
      String msg = L.l("  Using Resin(R) Open Source under the GNU Public License (GPL).\n" +
		       "\n" +
		       "  See http://www.caucho.com for information on Resin Professional,\n" +
		       "  including caching, clustering, JNI acceleration, and OpenSSL integration.\n");
    
      log.warning(msg);
      System.err.println(msg);
    }
    
    System.out.println("Starting Resin on " + QDate.formatLocal(Alarm.getCurrentTime()));
    System.out.println();

    EnvironmentClassLoader.initializeEnvironment();

    // buildResinClassLoader();

    // validateEnvironment();

    if (_classLoader != null)
      _mainThread.setContextClassLoader(_classLoader);

    if (isResinProfessional && _configServer != null) {
      Path dbDir = Vfs.lookup("work/config");
	
      Class cl = Class.forName("com.caucho.vfs.remote.RemotePath");
      Constructor ctor = cl.getConstructor(new Class[] { String.class,
							 Path.class,
							  String.class });

      Path path = (Path) ctor.newInstance(_configServer, dbDir, _serverId);
	
      ConfigPath.setRemote(path);
      log.info("Using configuration from " + _configServer);
    }

    ResinServer server = new ResinServer();

    Path resinConf = Vfs.lookup(_resinConf);

    server.setInitialStartTime(_startTime);
    server.setConfigFile(resinConf.getNativePath());
    server.setServerId(_serverId);
    server.addListener(this);
    server.setResinProfessional(isResinProfessional);
    server.setRestartOnClose(_waitIn == null);

    _mainThread.setContextClassLoader(_systemClassLoader);

    Config config = new Config();
    // server/10hc
    // config.setResinInclude(true);
    
    config.configure(server, Vfs.lookup(_resinConf), server.getSchema());
    
    _server = server;
    server.start();
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
   * Called when the server restarts.
   */
  public void closeEvent(ResinServer server)
  {
    try {
      if (_server != null && _server.isRestartOnClose()) {
	_isRestarting = true;
	_server = null;

	log.info("restarting Resin");

	init();
      }
      else
	_isClosed = true;
    } catch (Throwable e) {
      _isClosed = true;
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  /**
   * Thread to wait until Resin should be stopped.
   */
  public void waitForExit()
  {
    int socketExceptionCount = 0;
    Integer memoryTest;
    Runtime runtime = Runtime.getRuntime();
    ResinServer server;
    InputStream pingIn = null;
    OutputStream pingOut = null;

    if (_pingSocket != null) {
      try {
	pingIn = _pingSocket.getInputStream();
	pingOut = _pingSocket.getOutputStream();

	HessianInput in = new HessianInput(pingIn);
	HessianOutput out = new HessianOutput(pingOut);
	
	pingOut.write('C');

	out.writeString(_serverId);

	String status = in.readString();

	// System.out.println("STATUS:" + status);

	int errorCount = 0;
	while (errorCount < 100) {
	  int code = 'P';
	  
	  try {
	    code = pingIn.read();
	  } catch (Exception e) {
	    errorCount++;
	    continue;
	  }

	  if (code < 0)
	    return;
	  else if (code == 'P') {
	    errorCount = 0;
	    pingOut.write('P');
	  }
	  else {
	    log.warning("unknown code: " + (char) code + " " + code);
	    return;
	  }
	}
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
	
	return;
      } finally {
	try {
	  _pingSocket.close();
	} catch (Throwable e) {
	}
      }
    }
    
    /*
     * If the server has a parent process watching over us, close
     * gracefully when the parent dies.
     */
    while (! _isClosed && (_isRestarting ||
			   (server = _server) != null && ! server.isClosed())) {
      try {
	Thread.sleep(10);

	if (_isRestarting)
	  continue;
	
	long minFreeMemory = _server.getMinFreeMemory();

	if (minFreeMemory <= 0) {
	  // memory check disabled
	}
	else if (2 * minFreeMemory < getFreeMemory(runtime)) {
	  // plenty of free memory
	}
	else {
	  if (log.isLoggable(Level.FINER)) {
	    log.finer(L.l("free memory {0} max:{1} total:{2} free:{3}",
			  "" + getFreeMemory(runtime),
			  "" + runtime.maxMemory(),
			  "" + runtime.totalMemory(),
			  "" + runtime.freeMemory()));
	  }
	  
	  log.info(L.l("Forcing GC due to low memory. {0} free bytes.",
		       getFreeMemory(runtime)));
	  
	  runtime.gc();
	  
	  if (getFreeMemory(runtime) < minFreeMemory) {
	    _isClosed = true;
	    log.severe(L.l("Restarting due to low free memory. {0} free bytes",
			   getFreeMemory(runtime)));
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
	  log.severe(L.l("Restarting due to frozen Resin timer manager thread (Alarm).  This error generally indicates a JVM freeze, not an application deadlock."));
	  Runtime.getRuntime().halt(1);
	}
	
	if (pingIn != null) {
          int code;
          if ((code = pingIn.read()) == 'P') {
            socketExceptionCount = 0;
	    pingOut.write('P');
	  }
	  else
	    _isClosed = true;
	}
        else if (_waitIn != null) {
          int len;
          if ((len = _waitIn.read()) >= 0) {
            socketExceptionCount = 0;
          }
          _isClosed = true;
        }
        else {
          Thread.sleep(10000);
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
          log.log(Level.FINE, e.toString(), e);
        }
        else if (socketExceptionCount > 100)
          _isClosed = true;
      } catch (OutOfMemoryError e) {
        _isClosed = true;

	try {
	  System.err.println("Out of memory");
	} finally {
	  Runtime.getRuntime().halt(1);
	}
      } catch (Throwable e) {
        _isClosed = true;
	
        log.log(Level.FINE, e.toString(), e);
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
    Resin resin = _resin;

    if (resin != null) {
      ResinServer server = resin.getServer();

      if (server != null)
	server.destroy();
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

      Resin resin = new Resin(argv);

      _resin = resin;

      resin.init();

      resin.waitForExit();

      System.err.println(L.l("closing server"));

      final ResinServer server = resin.getServer();

      new Thread() {
	public void run()
	{
	  setName("resin-destroy");

	  if (server != null)
	    server.destroy();
	}
      }.start();

      long stopTime = System.currentTimeMillis();
      long endTime = stopTime +	15000L;
      
      if (server != null) {
	endTime = stopTime + server.getShutdownWaitMax() ;
	
	while (System.currentTimeMillis() < endTime && ! server.isClosed()) {
	  try {
	    Thread.interrupted();
	    Thread.sleep(100);
	  } catch (Throwable e) {
	  }
	}
      }

      if (server != null && ! server.isClosed())
	Runtime.getRuntime().halt(1);
      // resin.destroy();

      System.exit(0);
    } catch (BindException e) {
      System.out.println(e);

      log.log(Level.FINE, e.toString(), e);
      
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
	log.log(Level.CONFIG, e.toString(), e);
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
      throw new ConfigException(L.l("The following system property must be set:\n  -Djava.util.logging.manager=com.caucho.log.LogManagerImpl\nThe JDK 1.4 Logging manager must be set to Resin's log manager."));
    }

    validatePackage("javax.servlet.Servlet", new String[] {"2.4", "1.4"});
    validatePackage("javax.servlet.jsp.jstl.core.Config", new String[] {"1.1"});
    validatePackage("javax.management.MBeanServer", new String[] {"1.2" });
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
      throw new ConfigException(L.l("class {0} is not loadable on startup.  Resin requires {0} to be in the classpath on startup.",
				    className));
      
    }
    
    Package pkg = cl.getPackage();

    if (pkg == null)
      throw new ConfigException(L.l("package for class {0} is missing.  Resin requires class {0} in the classpath on startup.",
				    className));

    if (pkg.getSpecificationVersion() == null)
      throw new ConfigException(L.l("{0} has no specification version.  Resin {1} requires version {2}.",
				    pkg, com.caucho.Version.VERSION,
				    versions[0]));

    for (int i = 0; i < versions.length; i++) {
      if (versions[i].compareTo(pkg.getSpecificationVersion()) <= 0)
	return;
    }
      
    throw new ConfigException(L.l("Specification version {0} of {1} is not compatible with Resin {2}.  Resin {2} requires version {3}.",
				  pkg.getSpecificationVersion(),
				  pkg, com.caucho.Version.VERSION,
				  versions[0]));
  }
      

  public class ResinContainer {
    private ResinServer _resin;

    public ResinContainer(ResinServer resin)
    {
      _resin = resin;
    }
    
    public ResinServer createResin()
    {
      return _resin;
    }

    public ResinServer createCauchoCom()
    {
      return _resin;
    }
  }
}
