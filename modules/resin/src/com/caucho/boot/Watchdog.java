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

package com.caucho.boot;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import com.caucho.config.*;
import com.caucho.config.types.*;
import com.caucho.lifecycle.*;
import com.caucho.management.server.*;
import com.caucho.util.*;
import com.caucho.server.admin.*;
import com.caucho.server.port.*;
import com.caucho.vfs.*;

/**
 * Thread responsible for watching a backend server.
 */
public class Watchdog extends AbstractManagedObject
  implements Runnable, WatchdogMXBean
{
  private static final Logger log
    = Logger.getLogger(Watchdog.class.getName());

  private ClusterConfig _cluster;
  
  private String _id = "";

  private String []_argv;

  private ArrayList<String> _jvmArgs = new ArrayList<String>();
  private ArrayList<String> _watchdogArgs = new ArrayList<String>();
  
  private boolean _is64bit;
  private boolean _hasXss;
  private boolean _hasXmx;

  private Path _serverRoot;

  private Boot _jniBoot;
  private String _userName;
  private String _groupName;

  private InetAddress _address;
  private int _watchdogPort;

  private ArrayList<Port> _ports = new ArrayList<Port>();
  
  private final Lifecycle _lifecycle = new Lifecycle();

  private long _shutdownWaitTime = 60000L;

  private boolean _isVerbose;

  private boolean _isSingle;

  private Thread _thread;

  // statistics
  private Date _initialStartTime;
  private Date _lastStartTime;
  private int _startCount;
  
  Watchdog(ClusterConfig cluster)
  {
    _cluster = cluster;

    try {
      _address = InetAddress.getByName("127.0.0.1");
    } catch (Exception e) {
      throw new ConfigException(e);
    }
    
    _watchdogPort = 6600;

    try {
      Class cl = Class.forName("com.caucho.boot.JniBoot");
      
      _jniBoot = (Boot) cl.newInstance();
    } catch (ClassNotFoundException e) {
      log.finer(e.toString());
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public void setVerbose(boolean isVerbose)
  {
    _isVerbose = isVerbose;
  }

  public void setAddress(String address)
    throws UnknownHostException
  {
    _address = InetAddress.getByName(address);
  }

  public InetAddress getAddress()
  {
    return _address;
  }

  public void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }

  public int getWatchdogPort()
  {
    return _watchdogPort;
  }
  
  public void addJvmArg(String arg)
  {
    _jvmArgs.add(arg);

    if (arg.equals("-d64"))
      _is64bit = true;
    else if (arg.startsWith("-Xss"))
      _hasXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasXmx = true;
  }
  
  public void addWatchdogArg(String arg)
  {
    _watchdogArgs.add(arg);
  }

  public ArrayList<String> getJvmArgs()
  {
    return _jvmArgs;
  }

  /**
   * Adds a http.
   */
  public void addHttp(Port port)
    throws ConfigException
  {
    _ports.add(port);
  }

  /**
   * Adds a custom-protocol port.
   */
  public void addProtocol(Port port)
    throws ConfigException
  {
    _ports.add(port);
  }

  public void setUserName(String user)
  {
    _userName = user;
  }

  public void setGroupName(String group)
  {
    _groupName = group;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
  }

  /*
  ServerWatchdog(String serverId,
		 String []argv,
		 Path resinHome,
		 Path serverRoot)
  {
    _serverId = serverId;
    _argv = argv;

    _resinHome = resinHome;
    _serverRoot = serverRoot;
  }
  */

  public boolean startWatchdog(String []argv)
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.start(getId(), argv);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    launchManager(argv);

    return true;
  }

  public boolean restartWatchdog(String []argv)
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      watchdog.stop(getId());
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    try {
      return watchdog.start(getId(), argv);
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    launchManager(argv);

    return true;
  }

  public boolean stopWatchdog()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.stop(getId());
    } catch (ConfigException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  public boolean shutdown()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.shutdown();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  private WatchdogAPI getProxy()
  {
    String url = ("hmux://" + getAddress().getHostAddress()
		  + ":" + getWatchdogPort()
		  + "/watchdog");
    
    HashMap<String,Object> attr = new HashMap<String,Object>();
    attr.put("host", "resin-admin");
    
    Path path = Vfs.lookup(url, attr);

    return HessianHmuxProxy.create(path, WatchdogAPI.class);
  }
  
  public void launchManager(String []argv)
    throws IOException
  {
    Path resinHome = _cluster.getResin().getResinHome();
    Path serverRoot = _cluster.getResin().getRootDirectory();
    
    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(serverRoot.getNativePath()));

    Map<String,String> env = builder.environment();

    String classPath = WatchdogManager.calculateClassPath(resinHome);

    env.put("CLASSPATH", classPath);

    if (_is64bit) {
      env.put("LD_LIBRARY_PATH",
	      resinHome.lookup("libexec64").getNativePath());
      env.put("DYLD_LIBRARY_PATH",
	      resinHome.lookup("libexec64").getNativePath());
    }
    else {
      env.put("LD_LIBRARY_PATH",
	      resinHome.lookup("libexec").getNativePath());
      env.put("DYLD_LIBRARY_PATH",
	      resinHome.lookup("libexec").getNativePath());
    }

    ArrayList<String> list = new ArrayList<String>();

    list.add(getJavaExe());
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());

    if (! _hasXss)
      list.add("-Xss1m");

    list.addAll(_watchdogArgs);
    
    list.add("com.caucho.boot.WatchdogManager");

    for (int i = 0; i < argv.length; i++)
      list.add(argv[i]);

    builder = builder.command(list);

    builder.redirectErrorStream(true);

    Process process = builder.start();

    InputStream stdIs = process.getInputStream();
    OutputStream stdOs = process.getOutputStream();

    stdIs.close();
    stdOs.close();
  }

  public void start()
  {
    if (! _lifecycle.toActive())
      return;

    registerSelf();

    _thread = new Thread(this, "watchdog-" + _id);
    _thread.setDaemon(true);

    _thread.start();
  }

  public int startSingle(String []argv, Path serverRoot)
  {
    if (! _lifecycle.toActive())
      return -1;

    _argv = argv;
    _serverRoot = serverRoot;
    _isSingle = true;

    _thread = new Thread(this, "watchdog-" + _id);
    _thread.start();

    while (_lifecycle.isActive()) {
      synchronized (_lifecycle) {
	try {
	  _lifecycle.wait(60000);
	} catch (Exception e) {
	  log.log(Level.FINER, e.toString(), e);
	}
      }
    }

    return 1;
  }

  public boolean start(String []argv, Path serverRoot)
  {
    if (! _lifecycle.toActive())
      return false;

    registerSelf();

    _argv = argv;
    _serverRoot = serverRoot;

    _thread = new Thread(this, "watchdog-" + _id);

    _thread.start();

    return true;
  }

  public void stop()
  {
    if (! _lifecycle.toStop())
      return;

    Thread thread = _thread;
    _thread = null;

    synchronized (_lifecycle) {
      _lifecycle.toStop();

      _lifecycle.notifyAll();
    }

    try {
      unregisterSelf();
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public void run()
  {
    _initialStartTime = new Date();
    
    while (_lifecycle.isActive()) {
      InputStream watchdogIs = null;
      WriteStream jvmOut = null;

      try {
	watchdogIs = null;

	ServerSocket ss = new ServerSocket(0, 5,
					   InetAddress.getByName("127.0.0.1"));
	int port = ss.getLocalPort();

	Path resinHome = _cluster.getResin().getResinHome();
	Path serverRoot = _cluster.getResin().getRootDirectory();

	if (! _isSingle) {
	  String name;

	  if ("".equals(_id))
	    name = "jvm-default.log";
	  else
	    name = "jvm-" + _id + ".log";

	  Path jvmPath = serverRoot.lookup("log/" + name);

	  try {
	    jvmPath.getParent().mkdirs();
	  } catch (Exception e) {
	    log.log(Level.FINE, e.toString(), e);
	  }

	  RotateStream rotateStream = RotateStream.create(jvmPath);
	  rotateStream.init();
	  jvmOut = rotateStream.getStream();
	}
	else
	  jvmOut = Vfs.openWrite(System.out);

	_lastStartTime = new Date();
	_startCount++;

	log.info("starting Resin " + this);
	Process process = createProcess(resinHome, serverRoot, port,
					jvmOut);

	ss.setSoTimeout(60000);

	Socket s = null;
	try {
	  s = ss.accept();
	} catch (Exception e) {
	} finally {
	  ss.close();
	}

	if (s != null)
	  watchdogIs = s.getInputStream();
	
	InputStream stdIs = process.getInputStream();
	OutputStream stdOs = process.getOutputStream();

	byte []data = new byte[1024];
	int len;
	boolean isLive = true;
	int stdoutTimeoutMax = 10;
	int stdoutTimeout = stdoutTimeoutMax;

	while (isLive && _lifecycle.isActive()) {
	  int available = 0;
	  
	  while ((available = stdIs.available()) > 0) {
	    len = stdIs.read(data, 0, data.length);

	    if (len <= 0)
	      break;
	    
	    stdoutTimeout = stdoutTimeoutMax;
	    
	    jvmOut.write(data, 0, len);
	    jvmOut.flush();
	  }

	  try {
	    int status = process.exitValue();

	    isLive = false;
	  } catch (IllegalThreadStateException e) {
	  }

	  try {
	    synchronized (_lifecycle) {
	      if (stdoutTimeout-- > 0)
		_lifecycle.wait(100 * (stdoutTimeoutMax - stdoutTimeout));
	      else
		_lifecycle.wait(100 * stdoutTimeoutMax);
	    }
	  } catch (Exception e) {
	  }
	}

	try {
	  watchdogIs.close();
	} catch (Exception e) {
	}

	try {
	  stdOs.close();
	} catch (Exception e) {
	}

	long endTime = Alarm.getCurrentTime() + _shutdownWaitTime;
	isLive = true;

	log.info(this + " stopping Resin");

	while (isLive && Alarm.getCurrentTime() < endTime) {
	  try {
	    while (stdIs.available() > 0) {
	      len = stdIs.read(data, 0, data.length);

 	      if (len <= 0) {
		isLive = false;
		break;
	      }
	  
	      jvmOut.write(data, 0, len);
	      jvmOut.flush();
	    }
	  } catch (IOException e) {
	    log.log(Level.FINER, e.toString(), e);
	  }

	  try {
	    int status = process.exitValue();

	    isLive = false;
	  } catch (IllegalThreadStateException e) {
	  }
	}

	try {
	  stdIs.close();
	} catch (Exception e) {
	}

	if (isLive) {
	  try {
	    process.destroy();
	  } catch (Exception e) {
	    log.log(Level.FINE, e.toString(), e);
	  }
	}

	try {
	  int status = process.waitFor();
	} catch (Exception e) {
	  log.log(Level.INFO, e.toString(), e);
	}
      } catch (Exception e) {
	log.log(Level.INFO, e.toString(), e);
      } finally {
	if (watchdogIs != null) {
	  try {
	    watchdogIs.close();
	  } catch (IOException e) {
	  }
	}
	
	if (jvmOut != null) {
	  try {
	    jvmOut.close();
	  } catch (IOException e) {
	  }
	}
      }

      if (_isSingle) {
	_lifecycle.toStop();

	synchronized (_lifecycle) {
	  _lifecycle.notify();
	}
      }
    }
  }

  private Process createProcess(Path resinHome,
				Path serverRoot,
				int socketPort,
				WriteStream out)
    throws IOException
  {
    String classPath = WatchdogManager.calculateClassPath(resinHome);

    HashMap<String,String> env = new HashMap<String,String>();

    env.put("CLASSPATH", classPath);

    if (_is64bit) {
      env.put("LD_LIBRARY_PATH",
	      resinHome.lookup("libexec64").getNativePath());
      env.put("DYLD_LIBRARY_PATH",
	      resinHome.lookup("libexec64").getNativePath());
    }
    else {
      env.put("LD_LIBRARY_PATH",
	      resinHome.lookup("libexec").getNativePath());
      env.put("DYLD_LIBRARY_PATH",
	      resinHome.lookup("libexec").getNativePath());
    }

    ArrayList<String> list = new ArrayList<String>();

    list.add(getJavaExe());
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());
    list.add("-Dserver.root=" + _serverRoot.getPath());

    if (! _hasXss)
      list.add("-Xss1m");
    
    if (! _hasXmx)
      list.add("-Xmx256m");

    for (String arg : getJvmArgs()) {
      list.add(arg);
    }

    ArrayList<String> resinArgs = new ArrayList<String>();
    for (int i = 0; i < _argv.length; i++) {
      if (_argv[i].startsWith("-J")) {
	list.add(_argv[i].substring(2));
      }
      else
	resinArgs.add(_argv[i]);
    }
    
    list.add("com.caucho.server.resin.Resin");
    list.add("-socketwait");
    list.add(String.valueOf(socketPort));

    list.addAll(resinArgs);

    if (_isVerbose) {
      for (int i = 0; i < list.size(); i++) {
	if (i > 0)
	  out.print("  ");
	
	out.print(list.get(i));

	if (i + 1 < list.size())
	  out.println(" \\");
	else
	  out.println();
      }
    }

    if (_jniBoot != null) {
      ArrayList<QServerSocket> boundSockets = new ArrayList<QServerSocket>();

      try {
	if (_userName != null) {
	  for (int j = 0; j < _ports.size(); j++) {
	    Port port = _ports.get(j);

	    QServerSocket ss = port.bindForWatchdog();

	    boundSockets.add(ss);
	    
	    if (ss.setSaveOnExec()) {
	      list.add("-port");
	      list.add(String.valueOf(ss.getSystemFD()));
	      list.add(String.valueOf(port.getAddress()));
	      list.add(String.valueOf(port.getPort()));
	    }
	  }
	}

	Process process = _jniBoot.exec(list, env,
					resinHome.getNativePath(),
					_userName, _groupName);

	if (process != null)
	  return process;
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      } finally {
	for (int i = 0; i < boundSockets.size(); i++) {
	  try {
	    boundSockets.get(i).close();
	  } catch (Throwable e) {
	  }
	}
      }
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(serverRoot.getNativePath()));

    builder.environment().putAll(env);
    
    builder = builder.command(list);

    builder.redirectErrorStream(true);

    return builder.start();
  }

  private String getJavaExe()
  {
    Path javaHome = Vfs.lookup(System.getProperty("java.home"));

    if (javaHome.getTail().equals("jre"))
      javaHome = javaHome.getParent();

    if (javaHome.lookup("bin/javaw.exe").canRead())
      return javaHome.lookup("bin/javaw").getNativePath();
    else if (javaHome.lookup("bin/java.exe").canRead())
      return javaHome.lookup("bin/java").getNativePath();
    else if (javaHome.lookup("bin/java").canRead())
      return javaHome.lookup("bin/java").getNativePath();

    javaHome = Vfs.lookup(System.getProperty("java.home"));

    if (javaHome.lookup("bin/javaw.exe").canRead())
      return javaHome.lookup("bin/javaw").getNativePath();
    else if (javaHome.lookup("bin/java.exe").canRead())
      return javaHome.lookup("bin/java").getNativePath();
    else if (javaHome.lookup("bin/java").canRead())
      return javaHome.lookup("bin/java").getNativePath();

    return "java";
  }

  //
  // management
  //

  public String getName()
  {
    return getId();
  }

  public String getType()
  {
    return "Watchdog";
  }

  public String getState()
  {
    return _lifecycle.getStateName();
  }

  public Date getInitialStartTime()
  {
    return _initialStartTime;
  }

  public Date getStartTime()
  {
    return _lastStartTime;
  }

  public int getStartCount()
  {
    return _startCount;
  }
  
  public String toString()
  {
    return "Watchdog[" + getId() + "]";
  }

  //
  // main
  //
  
  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.conf   : alternate configuration file
   * -port port         : set the server's port
   * <pre>
   */
  public static void main(String []argv)
  {
    try {
      ResinBoot boot = new ResinBoot(argv);

      while (boot.start()) {
	try {
	  synchronized (boot) {
	    boot.wait(5000);
	  }
	} catch (Throwable e) {
	}
      }
    } catch (ConfigException e) {
      System.out.println(e.getMessage());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
