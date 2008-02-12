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

import com.caucho.config.*;
import com.caucho.config.program.*;
import com.caucho.server.admin.HessianHmuxProxy;
import com.caucho.util.*;
import com.caucho.Version;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

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
 * Client to a watchdog-manager, i.e. ResinBoot code to ask the
 * watchdog-manager to do something.
 */
public class WatchdogClient
{
  private static final L10N L
    = new L10N(WatchdogClient.class);
  private static final Logger log
    = Logger.getLogger(WatchdogClient.class.getName());
  
  private final BootManager _bootManager;
  private String _id = "";

  private final Watchdog _watchdog;
  
  private String []_argv;

  private Path _javaExe;
  private ArrayList<String> _watchdogJvmArgs = new ArrayList<String>();

  private boolean _is64bit;
  private boolean _hasXss;
  private boolean _hasXmx;
  
  private boolean _hasWatchdogXss;
  private boolean _hasWatchdogXmx;

  private Path _pwd;

  private InetAddress _address;
  private int _watchdogPort = 6600;

  private Boot _jniBoot;

  private long _shutdownWaitTime = 60000L;

  private boolean _isVerbose;

  WatchdogClient(BootManager bootManager)
  {
    _bootManager = bootManager;
    _watchdog = new Watchdog(bootManager.getArgs());
    
    _pwd = Vfs.getPwd();

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

  public String getAdminCookie()
  {
    return _bootManager.getAdminCookie();
  }

  public void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }

  public int getWatchdogPort()
  {
    return _watchdogPort;
  }
  
  public void addWatchdogArg(String arg)
  {
    addWatchdogJvmArg(arg);
  }

  String[] getArgv()
  {
    return _argv;
  }

  Path getPwd()
  {
    return _pwd;
  }

  Path getResinHome()
  {
    return _bootManager.getResinHome();
  }

  Path getRootDirectory()
  {
    return _bootManager.getRootDirectory();
  }

  boolean hasXmx()
  {
    return _hasXmx;
  }

  boolean hasXss()
  {
    return _hasXss;
  }

  boolean is64bit()
  {
    return _is64bit;
  }

  boolean isVerbose()
  {
    return _isVerbose;
  }
  
  public void setJavaExe(Path javaExe)
  {
    _javaExe = javaExe;
  }
  
  public void addWatchdogJvmArg(String arg)
  {
    _watchdogJvmArgs.add(arg);
    
    if (arg.startsWith("-Xss"))
      _hasWatchdogXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasWatchdogXmx = true;
  }
  
  public String getGroupName()
  {
    return _watchdog.getGroupName();
  }
  
  public String getUserName()
  {
    return _watchdog.getUserName();
  }
  
  public Path getLogDirectory()
  {
    return _bootManager.getLogDirectory();
  }
  
  public long getShutdownWaitTime()
  {
    return _shutdownWaitTime;
  }

  public void addBuilderProgram(ConfigProgram program)
  {
  }

  public void startWatchdog(String []argv)
    throws ConfigException, IOException
  {
    if (getUserName() != null && ! hasBoot()) {
	throw new ConfigException(L.l("<user-name> requires Resin Professional and compiled JNI.  Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin.so and check for a valid license in $RESIN_HOME/licenses."));
    }

    if (getGroupName() != null && ! hasBoot()) {
      throw new ConfigException(L.l("<group-name> requires Resin Professional and compiled JNI.  Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin.so and check for a valid license in $RESIN_HOME/licenses."));
    }
    
    WatchdogAPI watchdog = getProxy();

    try {
      watchdog.start(getAdminCookie(), argv);

      return;
    } catch (ConfigException e) {
      throw e;
    } catch (IllegalStateException e) {
      throw e;
    } catch (ConnectException e) {
      log.log(Level.FINER, e.toString(), e);
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
      watchdog.stop(getAdminCookie(), getId());
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

  public int startSingle(String []argv, Path rootDirectory)
    throws IOException
  {
    return _watchdog.startSingle(argv, rootDirectory);
  }

  public boolean shutdown()
    throws IOException
  {
    WatchdogAPI watchdog = getProxy();

    try {
      return watchdog.shutdown(getAdminCookie());
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
    
    Path resinHome = getResinHome();
    Path resinRoot = getRootDirectory();
    
    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(resinRoot.getNativePath()));

    Map<String,String> env = builder.environment();

    env.putAll(System.getenv());

    String classPath = WatchdogArgs.calculateClassPath(resinHome);

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
    list.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getPath());
    list.add("-Dresin.root=" + resinRoot.getPath());
    
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].startsWith("-Djava.class.path=")) {
	// IBM JDK startup issues
      }
      else if (argv[i].startsWith("-J")) {
	list.add(argv[i].substring(2));
      }
    }

    if (! _hasWatchdogXss)
      list.add("-Xss256k");
    if (! _hasWatchdogXmx)
      list.add("-Xmx32m");

    list.addAll(_watchdogJvmArgs);

    if (! list.contains("-d32") && ! list.contains("-d64") && _is64bit)
      list.add("-d64");

    list.add("com.caucho.boot.WatchdogManager");

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

  String getJavaExe()
  {
    if (_javaExe != null)
      return _javaExe.getNativePath();

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
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getId() + "]";
  }

  Boot getJniBoot()
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
}
