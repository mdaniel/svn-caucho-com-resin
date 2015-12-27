/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.watchdog;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import com.caucho.v5.cli.server.OpenPort;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.program.ConfigProgram;
import com.caucho.v5.config.types.Bytes;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.loader.SystemClassLoader;
import com.caucho.v5.log.impl.RolloverLogBase;
import com.caucho.v5.log.impl.RotateStream;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;

/**
 * Thread responsible for watching a backend server.
 */
public class ChildWatchdogConfig
{
  private static final int WATCHDOG_PORT_DEFAULT = 6600;
  
  private Path _javaHome;
  private Path _javaExe;
  private String _jvmMode;
  private ArrayList<String> _jvmArgs = new ArrayList<String>();
  private ArrayList<String> _jvmClasspath = new ArrayList<String>();
  private ArrayList<String> _watchdogJvmArgs = new ArrayList<String>();
  
  private Path _homeDirectory;
  private Path _rootDir;
  private Path _resinConf;
  private String _systemClassLoader = SystemClassLoader.class.getName();

  private boolean _is64bit;
  private boolean _hasXss;
  private boolean _hasXmx;

  private Path _chroot;
  private Path _pwd;

  private String _watchdogAddress = "127.0.0.1";
  private int _watchdogPort;
  
  private String _serverAddress = "127.0.0.1";
  private int _serverPort;
  private boolean _isRequireExplicitId;

  private WatchdogLog _watchdogLog;
  private Path _logPath;

  private String _userName;
  private String _groupName;

  private ArrayList<OpenPort> _ports = new ArrayList<>();
  
  private long _shutdownWaitTime = 60000L;

  private boolean _isVerbose;
  private boolean _hasWatchdogXss;
  private boolean _hasWatchdogXmx;

  private ServerConfigBoot _server;

  private ArgsWatchdog _args;

  ChildWatchdogConfig(ServerConfigBoot server,
                      ArgsWatchdog args)
  {
    Objects.requireNonNull(server);
    Objects.requireNonNull(args);
    
    _server = server;
    _args = args;
    
    server.configure(this);
  }
  
  public ServerConfigBoot getServer()
  {
    return _server;
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
  
  @Configurable
  public void addJvmArg(String arg)
  {
    _jvmArgs.add(arg);

    if (arg.equals("-d64")) {
      _is64bit = true;
    }
    else if (arg.startsWith("-Xss")) {
      _hasXss = true;
    }
    else if (arg.startsWith("-Xmx")) {
      _hasXmx = true;
    }
  }

  @Configurable
  public void addJvmArgLine(String argLine)
  {
    for (String arg : argLine.split("\\s+")) {
      if (! "".equals(arg))
        addJvmArg(arg);
    }
  }
  
  public ArrayList<String> getJvmArgs()
  {
    return _jvmArgs;
  }
  
  @Configurable
  public void setJvmMode(String mode)
  {
    _jvmMode = mode;
  }
  
  @Configurable
  public String getJvmMode()
  {
    return _jvmMode;
  }

  public void addJvmClasspath(String item)
  {
    if (item == null)
      return;

    for (String cp : item.split("[" + File.pathSeparatorChar + "]")) {
      if (! _jvmClasspath.contains(cp))
        _jvmClasspath.add(cp);
    }
  }
  
  public ArrayList<String> getJvmClasspath()
  {
    return _jvmClasspath;
  }
  
  public String getHomeCluster()
  {
    return _server.getRoot().getHomeCluster();
  }
  
  /**
   * Adds a http.
   */
  public void addHttp(OpenPort port)
    throws ConfigException
  {
    port.setProtocolName("http");
    
    _ports.add(port);
  }

  /**
   * Adds a custom-protocol port.
   */
  public OpenPort createProtocol()
    throws ConfigException
  {
    OpenPort port = new OpenPort();

    _ports.add(port);

    return port;
  }
  
  /**
   * Adds a watchdog managed port
   */
  public void addOpenPort(OpenPort port)
  {
    _ports.add(port);
  }

  public void setUserName(String user)
  {
    if ("".equals(user))
      user = null;
    
    _userName = user;
  }

  public String getUserName()
  {
    return _userName;
  }

  public void setGroupName(String group)
  {
    if ("".equals(group))
      group = null;
    
    _groupName = group;
  }

  public String getGroupName()
  {
    return _groupName;
  }
  
  public Path getLogPath()
  {
    if (_logPath != null) {
      return _logPath;
    }
    
    String name;
    
    String id = _server.getDisplayName();
    int port = _server.getPort();
    
    String programName = getArgs().getProgramName();
    
    if ("".equals(id)) {
      name = programName + "-default.log";
    }
    else if (port <= 0) {
      name = programName + "-" + id.replace(':', '_') + ".log";
    }
    else {
      // name = programName + "-" + id.replace(':', '_') + ".log";
      name = programName + "-" + port + ".log";
    }

    return getLogDirectory().lookup(name);
  }

  public Path getRootDirectory()
  {
    return _server.getRoot().getRootDirectory(_args);
  }

  public Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null) {
      return logDirectory;
    }
    
    if (_watchdogLog != null && _watchdogLog.getLogDirectory() != null) {
      return _watchdogLog.getLogDirectory();
    }
    else {
      return getRootDirectory().lookup("log");
    }
  }
  
  void logInit(RotateStream log)
  {
    if (_watchdogLog != null) {
      _watchdogLog.logInit(log);
    }
  }
  
  public long getShutdownWaitTime()
  {
    return _shutdownWaitTime;
  }

  public void setSystemClassLoader(String loader)
  {
    _systemClassLoader = loader;
  }

  public String getSystemClassLoader()
  {
    return _systemClassLoader;
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
  
  public String getWatchdogAddress()
  {
    if (_watchdogAddress != null)
      return _watchdogAddress;
    else
      return "127.0.0.1";
  }
  
  public void setWatchdogAddress(String addr)
  {
    _watchdogAddress = addr;
  }
  
  public void addWatchdogArg(String arg)
  {
    addWatchdogJvmArg(arg);
  }
  
  public void addWatchdogJvmArg(String arg)
  {
    _watchdogJvmArgs.add(arg);

    if (arg.equals("-d64")) {
    } else if (arg.startsWith("-Xss")) {
      _hasWatchdogXss = true;
    }
    else if (arg.startsWith("-Xmx")) {
      _hasWatchdogXmx = true;
    }
  }
  
  public ArrayList<String> getWatchdogJvmArgs()
  {
    return _watchdogJvmArgs;
  }

  public WatchdogLog createWatchdogLog()
  {
    if (_watchdogLog == null)
      _watchdogLog = new WatchdogLog();

    return _watchdogLog;
  }

  public void setAddress(String address)
  {
    _serverAddress = address;
  }
  
  public String getAddress()
  {
    return _serverAddress;
  }

  public void setPort(int port)
  {
    _serverPort = port;
  }
  
  public int getPort()
  {
    return _serverPort;
  }
  
  public void setRequireExplicitId(boolean isRequire)
  {
    _isRequireExplicitId = isRequire;
  }
  
  public boolean isRequireExplicitId()
  {
    return _isRequireExplicitId;
  }
  
  /**
   * Ignore items we can't understand.
   */
  public void addContentProgram(ConfigProgram program)
  {
  }

  ArgsWatchdog getArgs()
  {
    return _args;
  }

  public Iterable<OpenPort> getPorts()
  {
    return _ports;
  }

  Path getChroot()
  {
    return _chroot;
  }

  public void setChroot(Path chroot)
  {
    _chroot = chroot;
  }
  
  Path getPwd()
  {
    // return _pwd;
    return getRootDir();
  }

  Path getHomeDirectory()
  {
    if (_homeDirectory != null)
      return _homeDirectory;
    else
      return _args.getHomeDirectory();
  }

  @Configurable
  public void setResinRoot(Path root)
  {
    setRootDirectory(root);
  }

  @Configurable
  public void setRootDirectory(Path root)
  {
    _rootDir = root;
  }

  Path getRootDir()
  {
    if (_rootDir != null)
      return _rootDir;
    else
      return getRootDirectory();
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
      return _args.getConfigPath();
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
    return getClass().getSimpleName() + "[" + _server + "]";
  }

  public class WatchdogLog {
    private Path _logDirectory;
    
    private Integer _rolloverCount;
    private Period _rolloverPeriod;
    private Bytes _rolloverSize;

    public void setLogDirectory(Path dir)
    {
      _logDirectory = dir;
    }

    Path getLogDirectory()
    {
      return _logDirectory;
    }

    public void setRolloverCount(int count)
    {
      _rolloverCount = count;
    }

    public void setRolloverPeriod(Period period)
    {
      _rolloverPeriod = period;
    }

    public void setRolloverSize(Bytes size)
    {
      _rolloverSize = size;
    }
    
    /**
     * Initialize a log with the watchdog-log parameters
     * 
     * @param stream the log to initialize
     */
    void logInit(RotateStream stream)
    {
      RolloverLogBase log = stream.getRolloverLog();
      
      if (_rolloverCount != null)
        log.setRolloverCount(_rolloverCount);
      
      if (_rolloverPeriod != null)
        log.setRolloverPeriod(_rolloverPeriod);
      
      if (_rolloverSize != null)
        log.setRolloverSize(_rolloverSize);
    }
  }
}
