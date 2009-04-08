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

import com.caucho.bootjni.JniProcess;
import com.caucho.config.ConfigException;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.log.RotateStream;
import com.caucho.server.port.Port;
import com.caucho.util.*;
import com.caucho.server.util.*;
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
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Encapsulation of the process running resin.
 */
class WatchdogProcess
{
  private static final L10N L = new L10N(WatchdogProcess.class);
  private static final Logger log
    = Logger.getLogger(WatchdogProcess.class.getName());

  private static Boot _jniBoot;

  private final String _id;
  private final Watchdog _watchdog;
  private final Lifecycle _lifecycle = new Lifecycle();

  private ServerSocket _ss;
  private Process _process;
  private int _pid;

  WatchdogProcess(String id, Watchdog watchdog)
  {
    _id = id;
    _watchdog = watchdog;
  }

  int getPid()
  {
    return _pid;
  }

  public void run()
  {
    if (! _lifecycle.toActive())
      return;
    
    WriteStream jvmOut = null;

    try {
      _ss = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));

      int port = _ss.getLocalPort();
      
      log.config(this + " starting Resin");

      jvmOut = createJvmOut();

      _process = createProcess(port, jvmOut);

      if (_process != null) {
	try {
	  if (_process instanceof JniProcess)
	    _pid = ((JniProcess) _process).getPid();
	  else
	    _pid = 0;
	  
	  runInstance(jvmOut, _ss, _process);
	} finally {
	  destroy();
	}
      }
    } catch (Exception e) {
      log.log(Level.INFO, e.toString(), e);

      try {
	Thread.sleep(5000);
      } catch (Exception e1) {
      }
    } finally {
      if (jvmOut != null && ! _watchdog.isSingle()) {
	try {
	  jvmOut.close();
	} catch (IOException e) {
	}
      }
    }
  }

  void stop()
  {
    _lifecycle.toDestroy();
  }

  void destroy()
  {
    if (_process != null) {
      try {
	_process.destroy();
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }

      try {
	_process.waitFor();
	_process = null;
      } catch (Exception e) {
	log.log(Level.INFO, e.toString(), e);
      }
    }
  }

  private void runInstance(WriteStream jvmOut,
			   ServerSocket ss,
			   Process process)
    throws IOException
  {
    InputStream stdIs = null; 
    OutputStream stdOs = null;
    InputStream watchdogIs = null;
    Socket s = null;

    try {
      stdIs = process.getInputStream();
      stdOs = process.getOutputStream();
      ss.setSoTimeout(1000);
	
      boolean isLive = true;
      int stdoutTimeoutMax = 10;
      int stdoutTimeout = stdoutTimeoutMax;
      byte []data = new byte[1024];
      int len;

      s = connectToChild(ss, stdIs, jvmOut, process, data);

      if (s == null)
	log.warning(this + " watchdog socket timed out");
	  
      if (s != null)
	watchdogIs = s.getInputStream();

      runInstance(stdIs, jvmOut, process, data);

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

      log.info(this + " stopping Resin");

      closeInstance(stdIs, jvmOut, process, data);
    } finally {
      if (watchdogIs != null) {
	try {
	  watchdogIs.close();
	} catch (IOException e) {
	}
      }
	
      try {
	if (s != null)
	  s.close();
      } catch (Exception e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  private WriteStream createJvmOut()
    throws IOException
  {
    if (! _watchdog.isSingle()) {
      String name;
      String id = _watchdog.getId();
      Path jvmPath = _watchdog.getLogPath();

      try {
	Path dir = jvmPath.getParent();
	    
	if (! dir.exists()) {
	  dir.mkdirs();

	  String userName = _watchdog.getUserName();
	  if (userName != null)
	    dir.changeOwner(userName);

	  String groupName = _watchdog.getGroupName();
	  if (groupName != null)
	    dir.changeGroup(groupName);
	}
      } catch (Exception e) {
	log.log(Level.FINE, e.toString(), e);
      }

      RotateStream rotateStream = RotateStream.create(jvmPath);
      rotateStream.getRolloverLog().setRolloverSizeBytes(64L * 1024 * 1024);
      _watchdog.getConfig().logInit(rotateStream);
      rotateStream.init();
      return rotateStream.getStream();
    }
    else
      return Vfs.openWrite(System.out);
  }

  private Socket connectToChild(ServerSocket ss,
				InputStream stdIs,
				WriteStream jvmOut,
				Process process,
				byte []data)
    throws IOException
  {
    try {
      Socket s = null;

      for (int i = 0; i < 120 && s == null; i++) {
	try {
	  s = ss.accept();
	} catch (SocketTimeoutException e) {
	}

	while (stdIs.available() > 0) {
	  int len = stdIs.read(data, 0, data.length);

	  if (len < 0)
	    break;
	      
	  jvmOut.write(data, 0, len);
	  jvmOut.flush();
	}

	try {
	  int status = process.exitValue();

	  if (s != null)
	    s.close();

	  return null;
	} catch (IllegalThreadStateException e) {
	}
      }

      return s;
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);

      return null;
    } finally {
      ss.close();
    }
  }

  private void closeInstance(InputStream stdIs,
			     WriteStream jvmOut,
			     Process process,
			     byte []data)
  {
    long endTime = Alarm.getCurrentTime() + _watchdog.getShutdownWaitTime();
    boolean isLive = true;

    while (isLive && Alarm.getCurrentTime() < endTime) {
      try {
	while (stdIs.available() > 0) {
	  int len = stdIs.read(data, 0, data.length);

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
  }

  private void runInstance(InputStream stdIs,
			   WriteStream jvmOut,
			   Process process,
			   byte []data)
    throws IOException
  {
    boolean isLive = true;
    int stdoutTimeoutMax = 10;
    int stdoutTimeout = stdoutTimeoutMax;
    
    while (isLive && _lifecycle.isActive()) {
      while (stdIs.available() > 0) {
	int len = stdIs.read(data, 0, data.length);

	if (len <= 0)
	  break;
	    
	stdoutTimeout = stdoutTimeoutMax;
	    
	jvmOut.write(data, 0, len);
	jvmOut.flush();
      }

      try {
	process.exitValue();

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
  }

  private Process createProcess(int socketPort, WriteStream out)
    throws IOException
  {
    // watchdog/0210
    // Path pwd = rootDirectory;
    Path chroot = _watchdog.getChroot();
    Path processPwd = _watchdog.getPwd();

    Path resinHome = _watchdog.getResinHome();
    Path resinRoot = _watchdog.getResinRoot();

    ArrayList<String> classPathList = new ArrayList<String>();
    classPathList.addAll(_watchdog.getJvmClasspath());
    String classPath
      = WatchdogArgs.calculateClassPath(classPathList, resinHome);

    HashMap<String,String> env = new HashMap<String,String>();

    env.putAll(System.getenv());
    
    env.put("CLASSPATH", classPath);

    if (_watchdog.is64bit()) {
      appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      appendEnvPath(env,
                    "PATH",
                    resinHome.lookup("win64").getNativePath());
    }
    else {
      appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
      appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
      appendEnvPath(env,
                    "PATH",
                    resinHome.lookup("win32").getNativePath());
    }

    ArrayList<String> list = new ArrayList<String>();

    list.add(_watchdog.getJavaExe());
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    
    // This is needed for JMX to work correctly.
    String systemClassLoader = _watchdog.getSystemClassLoader();
    if (systemClassLoader != null && ! "".equals(systemClassLoader)) {
      list.add("-Djava.system.class.loader=" + systemClassLoader);
    }
    // #2567
    list.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getFullPath());

    if (! _watchdog.hasXss())
      list.add("-Xss1m");
    
    if (! _watchdog.hasXmx())
      list.add("-Xmx256m");

    for (String arg : _watchdog.getJvmArgs()) {
      if (! arg.startsWith("-Djava.class.path"))
	list.add(arg);
    }

    for (String arg : _watchdog.getArgv()) {
      if (arg.startsWith("-D") || arg.startsWith("-X"))
	list.add(arg);
      else if (arg.startsWith("-J"))
	list.add(arg.substring(2));
    }

    ArrayList<String> resinArgs = new ArrayList<String>();
    String []argv = _watchdog.getArgv();
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-conf")) {
	// resin conf handled below
	i++;
      }
      else if (argv[i].startsWith("-Djava.class.path=")) {
	// IBM JDK startup issues
      }
      else if (argv[i].startsWith("-J")) {
	list.add(argv[i].substring(2));
      }
      else if (argv[i].startsWith("-Djava.class.path")) {
      }
      else
	resinArgs.add(argv[i]);
    }

    if (! list.contains("-d32") && ! list.contains("-d64")
	&& _watchdog.is64bit() && ! CauchoSystem.isWindows()) {
      list.add("-d64");
    }

    if (! list.contains("-server")
	&& ! list.contains("-client")
	&& ! CauchoSystem.isWindows()) {
      // #3331, windows can't add -server automatically
      list.add("-server");
    }
    
    list.add("com.caucho.server.resin.Resin");
    
    if (resinRoot != null) {
      list.add("--root-directory");
      list.add(resinRoot.getFullPath());
    }

    if (_watchdog.getResinConf() != null) {
      list.add("-conf");
      list.add(_watchdog.getResinConf().getNativePath());
    }
      
    list.add("-socketwait");
    list.add(String.valueOf(socketPort));

    list.addAll(resinArgs);

    if (_watchdog.isVerbose()) {
      for (int i = 0; i < list.size(); i++) {
	if (i > 0)
	  out.print("  ");
	
	out.print(list.get(i));

	if (i + 1 < list.size())
	  out.println(" \\");
	else
	  out.println();
      }

      for (Map.Entry<String, String> envEntry : env.entrySet()) {
	String key = envEntry.getKey();
	String value = envEntry.getValue();

	if ("CLASSPATH".equals(key)
	    || "LD_LIBRARY_PATH".equals(key)
	    || "DYLD_LIBRARY_PATH".equals(key)) {
	  out.println(key + ": ");
	  
	  int len = (key + ": ").length();

	  for (String v : value.split("[" + File.pathSeparatorChar + "]")) {
	    for (int i = 0; i < len; i++)
	      out.print(" ");

	    out.println(v);
	  }
	}
	else
	  out.println("" + key + ": " + value);
      }
    }

    Boot boot = getJniBoot();
    if (boot != null) {
      boot.clearSaveOnExec();
      
      ArrayList<QServerSocket> boundSockets = new ArrayList<QServerSocket>();

      try {
	if (_watchdog.getUserName() != null) {
	  for (Port port : _watchdog.getPorts()) {
	    QServerSocket ss = port.bindForWatchdog();

	    if (ss == null)
	      continue;

	    boundSockets.add(ss);
	    
	    if (ss.setSaveOnExec()) {
	      list.add("-port");
	      list.add(String.valueOf(ss.getSystemFD()));
	      list.add(String.valueOf(port.getAddress()));
	      list.add(String.valueOf(port.getPort()));
	    }
	  }
	}

	String chrootPath = null;

	if (chroot != null)
	  chrootPath = chroot.getNativePath();

	Process process = boot.exec(list, env,
				    chrootPath,
				    processPwd.getNativePath(),
				    _watchdog.getUserName(),
				    _watchdog.getGroupName());

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

    if (_watchdog.getUserName() != null) {
      if (_watchdog.isSingle())
	throw new ConfigException(L.l("<user-name> requires compiled JNI started with 'start'.  Resin cannot use <user-name> when started as a foreground process."));
      else
	throw new ConfigException(L.l("<user-name> requires compiled JNI."));
    }
      
    if (_watchdog.getGroupName() != null) {
      if (_watchdog.isSingle())
	throw new ConfigException(L.l("<group-name> compiled JNI started with 'start'.  Resin cannot use <group-name> when started as a foreground process."));
      else
	throw new ConfigException(L.l("<group-name> compiled JNI."));
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

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _watchdog + "," + _id + "]";
  }

  Boot getJniBoot()
  {
    if (_jniBoot != null)
      return _jniBoot.isValid() ? _jniBoot : null;
    
    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();
      
      Class cl = Class.forName("com.caucho.bootjni.JniBoot", false, loader);
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
}
