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

import com.caucho.config.program.ConfigProgram;
import com.caucho.config.ConfigException;
import com.caucho.server.port.Port;
import com.caucho.util.*;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

import java.lang.reflect.*;
import java.net.*;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Thread responsible for watching a backend server.
 */
public class WatchdogConfig
{
  private static final L10N L
    = new L10N(WatchdogConfig.class);
  private static final Logger log
    = Logger.getLogger(WatchdogConfig.class.getName());

  private static final int WATCHDOG_PORT_DEFAULT = 6600;
  
  private String _id = "";

  private WatchdogArgs _args;

  private Path _javaHome;
  private Path _javaExe;
  private ArrayList<String> _jvmArgs = new ArrayList<String>();
  private ArrayList<String> _watchdogJvmArgs
    = new ArrayList<String>();
  private Path _resinHome;
  private Path _resinRoot;
  private Path _resinConf;
  private Path _logPath;

  private boolean _is64bit;
  private boolean _hasXss;
  private boolean _hasXmx;

  private Path _pwd;
  private int _watchdogPort;

  private String _userName;
  private String _groupName;

  private ArrayList<Port> _ports = new ArrayList<Port>();
  
  private long _shutdownWaitTime = 60000L;

  private boolean _isVerbose;
  private boolean _isWatchdog64bit;
  private boolean _hasWatchdogXss;
  private boolean _hasWatchdogXmx;

  WatchdogConfig(WatchdogArgs args)
  {
    _args = args;
    
    _pwd = Vfs.getPwd();

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
  
  String []getArgv()
  {
    return _args.getArgv();
  }

  public void setVerbose(boolean isVerbose)
  {
    _isVerbose = isVerbose;
  }
  
  public void setJavaExe(Path javaExe)
  {
    _javaExe = javaExe;
  }
  
  public void setJavaHome(Path javaHome)
  {
    _javaHome = javaHome;
  }
  
  public Path getJavaHome()
  {
    if (_javaHome != null)
      return _javaHome;
    else
      return _args.getJavaHome();
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
  
  public ArrayList<String> getJvmArgs()
  {
    return _jvmArgs;
  }

  
  public void addWatchdogJvmArg(String arg)
  {
    _watchdogJvmArgs.add(arg);

    if (arg.equals("-d64"))
      _isWatchdog64bit = true;
    else if (arg.startsWith("-Xss"))
      _hasWatchdogXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasWatchdogXmx = true;
  }
  
  public ArrayList<String> getWatchdogJvmArgs()
  {
    return _watchdogJvmArgs;
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
  
  /**
   * Adds a watchdog managed port
   */
  public void addOpenPort(Port port)
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
  
  public Path getLogPath()
  {
    if (_logPath != null)
      return _logPath;
    
    String name;
    
    if ("".equals(_id))
      name = "jvm-default.log";
    else
      name = "jvm-" + _id + ".log";

    return getLogDirectory().lookup(name);
  }

  public Path getRootDirectory()
  {
    return _args.getRootDirectory();
  }

  public Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null)
      return logDirectory;
    else
      return getRootDirectory().lookup("log");
  }
  
  public long getShutdownWaitTime()
  {
    return _shutdownWaitTime;
  }
  
  public int getWatchdogPort()
  {
    if (_args.getWatchdogPort() > 0)
      return _args.getWatchdogPort();
    else if (_watchdogPort > 0)
      return _watchdogPort;
    else
      return WATCHDOG_PORT_DEFAULT;
  }
  
  public void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addBuilderProgram(ConfigProgram program)
  {
  }

  WatchdogArgs getArgs()
  {
    return _args;
  }

  Iterable<Port> getPorts()
  {
    return _ports;
  }

  Path getPwd()
  {
    return _pwd;
  }

  Path getResinHome()
  {
    if (_resinHome != null)
      return _resinHome;
    else
      return _args.getResinHome();
  }

  Path getResinRoot()
  {
    if (_resinRoot != null)
      return _resinRoot;
    else
      return _args.getRootDirectory();
  }
  
  /**
   * Sets the resin.conf
   */
  public void setResinConf(Path resinConf)
  {
    _resinConf = resinConf;
  }
   
  /**
   * Returns the resin.conf
   */
  public Path getResinConf()
  {
    if (_resinConf != null)
      return _resinConf;
    else
      return _args.getResinConf();
  }

  boolean hasWatchdogXmx()
  {
    return _hasWatchdogXmx;
  }

  boolean hasWatchdogXss()
  {
    return _hasWatchdogXss;
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
    if (_isVerbose)
      return _isVerbose;
    else
      return _args.isVerbose();
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
}
