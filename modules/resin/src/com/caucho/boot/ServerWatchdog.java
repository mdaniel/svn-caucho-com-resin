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
import com.caucho.util.*;
import com.caucho.server.admin.*;
import com.caucho.vfs.*;

/**
 * Thread responsible for watching a backend server.
 */
public class ServerWatchdog implements Runnable {
  private static final Logger log
    = Logger.getLogger(ServerWatchdog.class.getName());

  private ClusterConfig _cluster;
  
  private String _id = "";

  private String []_argv;

  private Path _serverRoot;

  private Boot _jniBoot;
  private String _userName;
  private String _groupName;

  private InetAddress _address;
  private int _watchdogPort;

  private final Lifecycle _lifecycle = new Lifecycle();

  private long _shutdownWaitTime = 60000L;

  private boolean _isSingle;

  private Thread _thread;
  
  ServerWatchdog(ClusterConfig cluster)
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
    } catch (Exception e) {
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
    } catch (Exception e) {
      e.printStackTrace();
      
      log.log(Level.FINE, e.toString(), e);
    }

    launchManager();

    return true;
  }

  public boolean stopWatchdog()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.stop(getId());
    } catch (Exception e) {
      e.printStackTrace();
      
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
      e.printStackTrace();
      
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
  
  public void launchManager()
    throws IOException
  {
    Path resinHome = _cluster.getResin().getResinHome();
    
    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(resinHome.getNativePath()));

    Map<String,String> env = builder.environment();

    String classPath = WatchdogManager.calculateClassPath(resinHome);

    env.put("CLASSPATH", classPath);
    env.put("LD_LIBRARY_PATH", resinHome.lookup("libexec").getNativePath());
    env.put("DYLD_LIBRARY_PATH", resinHome.lookup("libexec").getNativePath());

    ArrayList<String> list = new ArrayList<String>();

    list.add("java");
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());
    list.add("-Xrs");
    list.add("-Xss1m");
    list.add("com.caucho.boot.WatchdogManager");
    
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

    _thread = new Thread(this, "watchdog-" + _id);

    _thread.start();

    while (_lifecycle.isActive()) {
      synchronized (_lifecycle) {
	try {
	  _lifecycle.wait(60000);
	} catch (Exception e) {
	  e.printStackTrace();
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
  }

  public void run()
  {
    while (_lifecycle.isActive()) {
      try {
	ServerSocket ss = new ServerSocket(0, 5,
					   InetAddress.getByName("127.0.0.1"));
	int port = ss.getLocalPort();

	Path resinHome = _cluster.getResin().getResinHome();

	Process process = createProcess(resinHome, port);

	ss.setSoTimeout(60000);

	Socket s = null;
	try {
	  s = ss.accept();
	} catch (Exception e) {
	} finally {
	  ss.close();
	}

	InputStream watchdogIs = s.getInputStream();
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

	    if (len > 0) {
	      stdoutTimeout = stdoutTimeoutMax;
	      System.out.print(new String(data, 0, len));
	    }
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
	  stdOs.close();
	} catch (Exception e) {
	}

	long endTime = Alarm.getCurrentTime() + _shutdownWaitTime;
	isLive = true;

	while (isLive && Alarm.getCurrentTime() < endTime) {
	  try {
	    while (stdIs.available() > 0) {
	      len = stdIs.read(data, 0, data.length);

	      if (len <= 0) {
		isLive = false;
		break;
	      }
	  
	      System.out.print(new String(data, 0, len));
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
      }

      if (_isSingle) {
	_lifecycle.toStop();

	synchronized (_lifecycle) {
	  _lifecycle.notify();
	}
      }
    }
  }

  private Process createProcess(Path resinHome, int socketPort)
    throws IOException
  {
    String classPath = WatchdogManager.calculateClassPath(resinHome);

    HashMap<String,String> env = new HashMap<String,String>();

    env.put("CLASSPATH", classPath);
    env.put("LD_LIBRARY_PATH", resinHome.lookup("libexec").getNativePath());
    env.put("DYLD_LIBRARY_PATH", resinHome.lookup("libexec").getNativePath());

    ArrayList<String> list = new ArrayList<String>();

    list.add("java");
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djava.system.class.loader=com.caucho.loader.SystemClassLoader");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());
    list.add("-Dserver.root=" + _serverRoot.getPath());
    list.add("-Xrs");
    list.add("-Xss1m");
    list.add("com.caucho.server.resin.ResinMain");
    list.add("-socketwait");
    list.add(String.valueOf(socketPort));

    for (int i = 0; i < _argv.length; i++)
      list.add(_argv[i]);

    if (_jniBoot != null) {
      try {
	Process process = _jniBoot.exec(list, env,
					resinHome.getNativePath(),
					_userName, _groupName);

	if (process != null)
	  return process;
      } catch (Throwable e) {
	e.printStackTrace();
	log.log(Level.WARNING, e.toString(), e);
      }
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(resinHome.getNativePath()));

    builder.environment().putAll(env);
    
    builder = builder.command(list);

    builder.redirectErrorStream(true);

    return builder.start();
  }
  
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
