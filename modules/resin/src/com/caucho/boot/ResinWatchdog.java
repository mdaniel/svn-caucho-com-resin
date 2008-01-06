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

package com.caucho.boot;

import com.caucho.config.BuilderProgram;
import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.log.RotateStream;
import com.caucho.management.server.AbstractManagedObject;
import com.caucho.management.server.ResinWatchdogMXBean;
import com.caucho.server.admin.HessianHmuxProxy;
import com.caucho.server.port.Port;
import com.caucho.util.*;
import com.caucho.Version;
import com.caucho.vfs.Path;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread responsible for watching a backend server.
 */
public class ResinWatchdog extends AbstractManagedObject
  implements Runnable, ResinWatchdogMXBean
{
  private static final L10N L
    = new L10N(ResinWatchdog.class);
  private static final Logger log
    = Logger.getLogger(ResinWatchdog.class.getName());

  private ClusterConfig _cluster;
  
  private String _id = "";

  private String []_argv;

  private String _javaExe;
  private ArrayList<String> _jvmArgs = new ArrayList<String>();
  private ArrayList<String> _watchdogJvmArgs = new ArrayList<String>();

  private boolean _is64bit;
  private boolean _hasXss;
  private boolean _hasXmx;
  
  private boolean _hasWatchdogXss;
  private boolean _hasWatchdogXmx;

  private Path _pwd;
  private Path _rootDirectory;

  private Boot _jniBoot;
  private String _userName;
  private String _groupName;

  private InetAddress _address;
  private int _watchdogPort = 6600;
  
  private String _password = "";

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

  ResinWatchdog(ClusterConfig cluster)
  {
    _pwd = Vfs.getPwd();

    _cluster = cluster;

    try {
      _address = InetAddress.getByName("127.0.0.1");
    } catch (Exception e) {
      throw ConfigException.create(e);
    }

    _is64bit = "64".equals(System.getProperty("sun.arch.data.model"));
  }

  public void setId(String id)
  {
    _id = id;
  }

  public String getId()
  {
    return _id;
  }

  public ResinWatchdogManager getManager()
  {
    return _cluster.getResin().getManager();
  }

  public void setVerbose(boolean isVerbose)
  {
    _isVerbose = isVerbose;
  }

  public void setAddress(String address)
    throws UnknownHostException
  {
    if (! "*".equals(address))
      _address = InetAddress.getByName(address);
  }

  public InetAddress getAddress()
  {
    return _address;
  }

  public void setWatchdogPassword(String password)
  {
    _password = password;
  }

  public String getWatchdogPassword()
  {
    return _password;
  }

  public void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }

  public int getWatchdogPort()
  {
    return _watchdogPort;
  }
  
  public void setJavaExe(String javaExe)
  {
    _javaExe = javaExe;
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
    addWatchdogJvmArg(arg);
  }
  
  public void addWatchdogJvmArg(String arg)
  {
    _watchdogJvmArgs.add(arg);
    
    if (arg.startsWith("-Xss"))
      _hasWatchdogXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasWatchdogXmx = true;
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

  public String getUserName()
  {
    return _userName;
  }

  public void setGroupName(String group)
  {
    _groupName = group;
  }

  public String getGroupName()
  {
    return _groupName;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(BuilderProgram program)
  {
  }

  public void startWatchdog(String []argv)
    throws ConfigException, IOException
  {
    if (_userName != null && ! hasBoot()) {
	throw new ConfigException(L.l("<user-name> requires Resin Professional and compiled JNI.  Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin.so and check for a valid license in $RESIN_HOME/licenses."));
    }

    if (_groupName != null && ! hasBoot()) {
      throw new ConfigException(L.l("<group-name> requires Resin Professional and compiled JNI.  Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin.so and check for a valid license in $RESIN_HOME/licenses."));
    }
    
    WatchdogAPI watchdog = getProxy();

    try {
      watchdog.start(_password, argv);

      return;
    } catch (ConfigException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    launchManager(argv);
  }

  public void stopWatchdog()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      watchdog.stop(_password, getId());
    } catch (ConfigException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (IOException e) {
      throw new IllegalStateException(L.l("Can't connect to ResinWatchdogManager.\n{1}",
					  Version.VERSION, e.toString()),
				      e);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void restartWatchdog(String []argv)
    throws IOException
  {
    try {
      stopWatchdog();
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    startWatchdog(argv);
  }

  public boolean shutdown()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.shutdown(_password);
    } catch (ConfigException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (IOException e) {
      throw new IllegalStateException(L.l("Can't connect to ResinWatchdogManager.\n{1}",
					  Version.VERSION, e.toString()),
				      e);
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  private WatchdogAPI getProxy()
  {
    String url = ("hmux://127.0.0.1:"
		  + getWatchdogPort()
		  + "/watchdog");
    
    HashMap<String,Object> attr = new HashMap<String,Object>();
    attr.put("host", "resin-admin");
    
    Path path = Vfs.lookup(url, attr);

    return HessianHmuxProxy.create(path, WatchdogAPI.class);
  }
  
  public void launchManager(String []argv)
    throws IOException
  {
    log.fine(this + " starting ResinWatchdogManager");
    
    Path resinHome = _cluster.getResin().getResinHome();
    Path resinRoot = _cluster.getResin().getRootDirectory();
    
    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(resinRoot.getNativePath()));

    Map<String,String> env = builder.environment();

    env.putAll(System.getenv());

    String classPath = ResinWatchdogManager.calculateClassPath(resinHome);

    env.put("CLASSPATH", classPath);

    String ldLibraryPath = env.get("LD_LIBRARY_PATH");
    String dyldLibraryPath = env.get("DYLD_LIBRARY_PATH");

    String libexecPath;

    if (_is64bit)
      libexecPath = resinHome.lookup("libexec64").getNativePath();
    else
      libexecPath = resinHome.lookup("libexec").getNativePath();

    if (ldLibraryPath == null || "".equals(ldLibraryPath))
      ldLibraryPath += libexecPath;
    else if (ldLibraryPath.indexOf(libexecPath) < 0)
      ldLibraryPath += File.pathSeparatorChar + libexecPath;

    if (dyldLibraryPath == null || "".equals(dyldLibraryPath))
      dyldLibraryPath += libexecPath;
    else if (ldLibraryPath.indexOf(libexecPath) < 0)
      dyldLibraryPath += File.pathSeparatorChar + libexecPath;
  
    env.put("LD_LIBRARY_PATH", ldLibraryPath);
    env.put("DYLD_LIBRARY_PATH", dyldLibraryPath);

    ArrayList<String> list = new ArrayList<String>();

    list.add(getJavaExe());
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());
    list.add("-Dresin.root=" + resinRoot.getPath());

    if (! _hasWatchdogXss)
      list.add("-Xss256k");
    if (! _hasWatchdogXmx)
      list.add("-Xmx32m");

    list.addAll(_watchdogJvmArgs);

    if (! list.contains("-d32") && ! list.contains("-d64") && _is64bit)
      list.add("-d64");

    list.add("com.caucho.boot.ResinWatchdogManager");

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-conf")
          || argv[i].equals("--conf")) {
        list.add(argv[i]);
        list.add(resinHome.lookup(argv[i + 1]).getNativePath());
	i++;
      }
      else
        list.add(argv[i]);
    }

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
    _thread.setDaemon(false);

    _thread.start();
  }

  public int startSingle(String []argv, Path rootDirectory)
  {
    if (! _lifecycle.toActive())
      return -1;

    _argv = argv;
    _rootDirectory = rootDirectory;
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

  /**
   * Starts the watchdog instance.
   */
  public boolean start(String []argv, Path rootDirectory)
  {
    if (! _lifecycle.toActive())
      return false;

    registerSelf();

    _argv = argv;
    _rootDirectory = rootDirectory;

    _thread = new Thread(this, "watchdog-" + _id);
    _thread.setDaemon(false);

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
    long retry = Long.MAX_VALUE;
    
    while (_lifecycle.isActive() && retry-- >= 0) {
      InputStream watchdogIs = null;
      WriteStream jvmOut = null;

      try {
	watchdogIs = null;

	ServerSocket ss = new ServerSocket(0, 5,
                                           InetAddress.getByName("127.0.0.1"));

	int port = ss.getLocalPort();

	Path resinHome = _cluster.getResin().getResinHome();
	Path rootDirectory = _cluster.getResin().getRootDirectory();

	if (! _isSingle) {
	  String name;

	  if ("".equals(_id))
	    name = "jvm-default.log";
	  else
	    name = "jvm-" + _id + ".log";

	  Path jvmPath = getManager().getLogDirectory().lookup(name);

	  try {
	    Path dir = jvmPath.getParent();
	    
	    if (! dir.exists()) {
	      dir.mkdirs();

	      if (_userName != null)
		dir.changeOwner(_userName);

	      if (_groupName != null)
		dir.changeGroup(_groupName);
	    }
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

	if (! _isSingle)
	  log.info("starting Resin " + this);

        // watchdog/0210
        // Path pwd = rootDirectory;
        Path pwd = _pwd;

        Process process = createProcess(pwd, resinHome, rootDirectory,
                                        port, jvmOut);

	InputStream stdIs = process.getInputStream();
	OutputStream stdOs = process.getOutputStream();
	
	ss.setSoTimeout(1000);
	
	boolean isLive = true;
	int stdoutTimeoutMax = 10;
	int stdoutTimeout = stdoutTimeoutMax;
	byte []data = new byte[1024];
	int len;

	Socket s = null;

	try {
	  for (int i = 0; i < 120 && s == null && isLive; i++) {
	    try {
	      s = ss.accept();
	    } catch (SocketTimeoutException e) {
	    }

	    while (stdIs.available() > 0) {
	      len = stdIs.read(data, 0, data.length);

	      if (len < 0)
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
	  }
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	} finally {
	  ss.close();
	}

	if (s == null)
	  log.warning("watchdog socket timed out");
	  
	if (s != null)
	  watchdogIs = s.getInputStream();

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

	    /*
	    // if bind failure, only retry 3 times
	    if (status == 67 && retry > 3)
	      retry = 3;
	    */

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
	  if (watchdogIs != null)
	    watchdogIs.close();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}

	try {
	  if (s != null)
	    s.close();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}

	try {
	  stdOs.close();
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
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

	try {
	  Thread.sleep(5000);
	} catch (Exception e1) {
	}
      } finally {
	if (watchdogIs != null) {
	  try {
	    watchdogIs.close();
	  } catch (IOException e) {
	  }
	}
	
	if (jvmOut != null && ! _isSingle) {
	  try {
	    jvmOut.close();
	  } catch (IOException e) {
	  }
	}
      }
    }
  }

  private Process createProcess(Path processPwd,
                                Path resinHome,
				Path resinRoot,
				int socketPort,
				WriteStream out)
    throws IOException
  {
    String classPath = ResinWatchdogManager.calculateClassPath(resinHome);

    HashMap<String,String> env = new HashMap<String,String>();

    env.putAll(System.getenv());
    
    env.put("CLASSPATH", classPath);

    if (_is64bit) {
      appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
    }
    else {
      appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
      appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
    }

    ArrayList<String> list = new ArrayList<String>();

    list.add(getJavaExe());
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    // #1970 - this confuses Terracotta
    // This is needed for JMX to work correctly.
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());

    if (! _hasXss)
      list.add("-Xss1m");
    
    if (! _hasXmx)
      list.add("-Xmx256m");

    for (String arg : getJvmArgs()) {
      if (! arg.startsWith("-Djava.class.path"))
	list.add(arg);
    }

    ArrayList<String> resinArgs = new ArrayList<String>();
    for (int i = 0; i < _argv.length; i++) {
      if (_argv[i].startsWith("-Djava.class.path=")) {
	// IBM JDK startup issues
      }
      else if (_argv[i].startsWith("-J")) {
	list.add(_argv[i].substring(2));
      }
      else if (! _argv[i].startsWith("-Djava.class.path"))
	resinArgs.add(_argv[i]);
    }

    if (! list.contains("-d32") && ! list.contains("-d64") && _is64bit)
      list.add("-d64");
    
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

      for (Map.Entry<String, String> envEntry : env.entrySet())
        out.println("" + envEntry.getKey() + ": " + envEntry.getValue());
    }

    if (getJniBoot() != null) {
      getJniBoot().clearSaveOnExec();
      
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

	Process process = getJniBoot().exec(list, env,
					    processPwd.getNativePath(),
					    _userName, _groupName);

	if (process != null)
	  return process;
      } catch (ConfigException e) {
	log.warning(e.getMessage());
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

    if (_userName != null) {
      if (_isSingle)
	throw new ConfigException(L.l("<user-name> requires Resin Professional and compiled JNI started with 'start'.  Resin cannot use <user-name> when started as a foreground process."));
      else
	throw new ConfigException(L.l("<user-name> requires Resin Professional and compiled JNI."));
    }
      
    if (_groupName != null) {
      if (_isSingle)
	throw new ConfigException(L.l("<group-name> requires Resin Professional and compiled JNI started with 'start'.  Resin cannot use <group-name> when started as a foreground process."));
      else
	throw new ConfigException(L.l("<group-name> requires Resin Professional and compiled JNI."));
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(processPwd.getNativePath()));

    builder.environment().putAll(env);
    
    builder = builder.command(list);

    builder.redirectErrorStream(true);

    return builder.start();
  }

  private void appendEnvPath(Map<String,String> env,
                             String prop,
                             String value)
  {
    String oldValue = env.get(prop);

    if (oldValue != null && ! "".equals(oldValue))
      value = value + File.pathSeparator + oldValue;

    env.put(prop, value);
  }

  private String getJavaExe()
  {
    if (_javaExe != null)
      return _javaExe;

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

  private Boot getJniBoot()
  {
    if (_jniBoot != null)
      return _jniBoot.isValid() ? _jniBoot : null;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName("com.caucho.boot.JniBoot", false, loader);

      _jniBoot = (Boot) cl.newInstance();
    } catch (ClassNotFoundException e) {
      log.fine(e.toString());
    } catch (IllegalStateException e) {
      log.fine(e.toString());
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return _jniBoot != null && _jniBoot.isValid() ? _jniBoot : null;
  }

  private boolean hasBoot()
  {
    try {
      Boot boot = getJniBoot();

      return boot.isValid();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
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
