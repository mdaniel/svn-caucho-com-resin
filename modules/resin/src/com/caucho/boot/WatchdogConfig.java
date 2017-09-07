/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.File;
import java.util.ArrayList;

import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.Configurable;
import com.caucho.config.program.ConfigProgram;
import com.caucho.config.types.Bytes;
import com.caucho.config.types.Period;
import com.caucho.log.AbstractRolloverLog;
import com.caucho.log.RotateStream;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * Thread responsible for watching a backend server.
 */
class WatchdogConfig
{
  private static final int WATCHDOG_PORT_DEFAULT = 6600;
  
  private String _id;
  private final int _index;
  
  private final BootClusterConfig _cluster;

  private WatchdogArgs _args;
  private Path _rootDirectory;

  private Path _javaHome;
  private Path _javaExe;
  private String _jvmMode;
  private ArrayList<String> _jvmArgs = new ArrayList<String>();
  private ArrayList<String> _jvmClasspath = new ArrayList<String>();
  private ArrayList<String> _watchdogJvmArgs = new ArrayList<String>();
  
  private Path _resinHome;
  private Path _resinRoot;
  private Path _resinConf;
  private String _systemClassLoader = "com.caucho.loader.SystemClassLoader";

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

  private ArrayList<OpenPort> _ports = new ArrayList<OpenPort>();
  
  private long _shutdownWaitTime = 60000L;

  private boolean _isVerbose;
  private boolean _hasWatchdogXss;
  private boolean _hasWatchdogXmx;
  
  private boolean _isElastic;

  private int _elasticServerPort;

  private String _elasticServerCluster;

  WatchdogConfig(String id,
                 BootClusterConfig cluster,
                 WatchdogArgs args,
                 Path rootDirectory,
                 int index)
  {
    if (id == null || "".equals(id)) {
      id = "default";
    }
    
    _id = id;
    _cluster = cluster;
    
    if (cluster != null) {
      _elasticServerCluster = cluster.getId();
    }

    _index = index;
    
    _args = args;
    _rootDirectory = rootDirectory;
    
    // #4928
    //_pwd = Vfs.lookup();
    _pwd = rootDirectory;

    _is64bit = args.is64Bit();
  }
  
  public void setId(String id)
  {
    if (id == null || "".equals(id))
      id = "default";
    
    _id = id;
  }

  public String getId()
  {
    return _id;
  }
  
  public int getIndex()
  {
    return _index;
  }
  
  String []getArgv()
  {
    return _args.getArgv();
  }

  public void setElastic(boolean isElastic)
  {
    _isElastic = isElastic;
  }
  
  public boolean isElastic()
  {
    return _isElastic;
  }
  
  public int getElasticServerPort()
  {
    return _elasticServerPort;
  }
  
  public void setElasticServerPort(int port)
  {
    _elasticServerPort = port;
  }
  
  public String getElasticServerCluster()
  {
    return _elasticServerCluster;
  }
  
  public void setElasticServerCluster(String cluster)
  {
    _elasticServerCluster = cluster;
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

    if (arg.equals("-d64"))
      _is64bit = true;
    else if (arg.startsWith("-Xss"))
      _hasXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasXmx = true;
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

  public BootClusterConfig getCluster()
  {
    return _cluster;
  }
  
  public String getHomeCluster()
  {
    if (_cluster != null)
      return _cluster.getResin().getHomeCluster();
    else
      return null;
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
    if (_logPath != null)
      return _logPath;
    
    String name;
    
    if ("".equals(_id))
      name = "jvm-default.log";
    else
      name = "jvm-" + _id.replace(':', '_') + ".log";

    return getLogDirectory().lookup(name);
  }

  public Path getRootDirectory()
  {
    return _rootDirectory;
  }

  public Path getLogDirectory()
  {
    Path logDirectory = _args.getLogDirectory();

    if (logDirectory != null)
      return logDirectory;
    if (_watchdogLog != null && _watchdogLog.getLogDirectory() != null)
      return _watchdogLog.getLogDirectory();
    else
      return getRootDirectory().lookup("log");
  }
  
  void logInit(String name, AbstractRolloverLog log)
  {
    if (_watchdogLog != null)
      _watchdogLog.logInit(name, log);
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
    if (_args.getWatchdogPort() > 0) {
      return _args.getWatchdogPort();
    }
    else if (_watchdogPort > 0) {
      return _watchdogPort;
    }
    else {
      return WATCHDOG_PORT_DEFAULT;
    }
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
    } else if (arg.startsWith("-Xss"))
      _hasWatchdogXss = true;
    else if (arg.startsWith("-Xmx"))
      _hasWatchdogXmx = true;
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

  WatchdogArgs getArgs()
  {
    return _args;
  }

  Iterable<OpenPort> getPorts()
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
    return _pwd;
  }

  Path getResinHome()
  {
    if (_resinHome != null)
      return _resinHome;
    else
      return _args.getResinHome();
  }

  public void setResinRoot(Path root)
  {
    _resinRoot = root;
  }

  Path getResinRoot()
  {
    if (_resinRoot != null)
      return _resinRoot;
    else
      return _rootDirectory;
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

  public class WatchdogLog {
    private Path _logDirectory;
    
    private String _pathFormat;
    private String _archiveFormat;
    
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
    
    public void setPathFormat(String format)
    {
      _pathFormat = format;
    }
    
    public void setArchiveFormat(String format)
    {
      _archiveFormat = format;
    }
    
    /**
     * Initialize a log with the watchdog-log parameters
     * 
     * @param stream the log to initialize
     */
    void logInit(String name, AbstractRolloverLog log)
    {
      if (_pathFormat != null) {
        String pathFormat = _pathFormat.replace("%{name}", name);
        
        log.setPathFormat(pathFormat);
      }
      
      if (_archiveFormat != null) {
        String archiveFormat = _archiveFormat.replace("%{name}", name);
        
        log.setArchiveFormat(archiveFormat);
      }
      
      if (_rolloverCount != null)
        log.setRolloverCount(_rolloverCount);
      
      if (_rolloverPeriod != null)
        log.setRolloverPeriod(_rolloverPeriod);
      
      if (_rolloverSize != null) {
        log.setRolloverSize(_rolloverSize);
      }
      
      System.out.println("LI0: " + log);
    }
  }
}
