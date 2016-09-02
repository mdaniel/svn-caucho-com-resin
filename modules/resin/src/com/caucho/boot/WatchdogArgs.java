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
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.caucho.VersionFactory;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.license.LicenseCheck;
import com.caucho.loader.Environment;
import com.caucho.server.resin.ResinELContext;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.CurrentTime;
import com.caucho.util.HostUtil;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

class WatchdogArgs
{
  private static L10N L = new L10N(WatchdogArgs.class);

  private static final Logger log
    = Logger.getLogger(WatchdogArgs.class.getName());

  private static final HashMap<String,BootCommand> _commandMap
    = new HashMap<String,BootCommand>();

  private final String _clientServerId;
  
  private Path _javaHome;
  private Path _resinHome;
  private Path _rootDirectory;
  private Path _dataDirectory;
  private String[] _argv;
  
  private Path _resinConf;
  
  private Path _userProperties;
  private String _mode;
  
  private Path _logDirectory;
  private Path _licenseDirectory;

  private String _serverId = null;
  private String _clusterId;
  
  private int _watchdogPort;
  private boolean _isVerbose;
  private boolean _isHelp;
  private BootCommand _command;

  private ArrayList<String> _tailArgs = new ArrayList<String>();
  private String []_defaultArgs;

  private boolean _isElasticServer;
  private boolean _isElasticIp;
  private String _elasticAddress;
  private int _elasticPort;
  
  private ResinBootELContext _resinBootELContext = null;

  private boolean _is64bit;

  WatchdogArgs(String[] argv)
  {
    this(argv, true);
  }

  WatchdogArgs(String[] argv, boolean isTop)
  {
    this(argv, System.getProperty("resin.watchdog"), isTop);
  }

  WatchdogArgs(String[] argv, String serverId, boolean isTop)
  {
    _clientServerId = serverId;
    
    String logLevel = System.getProperty("resin.log.level");

    if (isTop) {
      setLogLevel(logLevel);
    }

    setResinHome(calculateResinHome());
    
    setRootDirectory(calculateResinRoot(_resinHome));
    
    _javaHome = Vfs.lookup(System.getProperty("java.home"));

    _is64bit = CauchoSystem.is64Bit();

    _resinConf = _resinHome.lookup("conf/resin.conf");
    if (! _resinConf.canRead())
      _resinConf = _resinHome.lookup("conf/resin.xml");
    
    _userProperties = Vfs.lookup(System.getProperty("user.home") + "/.resin");
    
    _argv = fillArgv(argv);

    parseCommandLine(_argv);
  }

  String []getRawArgv()
  {
    return _argv;
  }

  Path getJavaHome()
  {
    return _javaHome;
  }

  Path getResinHome()
  {
    return _resinHome;
  }

  Path getRootDirectory()
  {
    return _rootDirectory;
  }

  Path getDataDirectory()
  {
    return _dataDirectory;
  }

  Path getLogDirectory()
  {
    return _logDirectory;
  }

  Path getLicenseDirectory()
  {
    return _licenseDirectory;
  }

  Path getResinConf()
  {
    return _resinConf;
  }

  Path getUserProperties()
  {
    return _userProperties;
  }

  String getMode()
  {
    return _mode;
  }
  
  String getClientServerId()
  {
    return _clientServerId;
  }

  String getServerId()
  {
    return _serverId;
  }

  void setElasticServerId(String serverId)
  {
    _serverId = serverId;
    setElasticServer(true);
  }
  
  void setElasticServer(boolean isElastic)
  {
    _isElasticServer = isElastic;
  }

  String getClusterId()
  {
    return _clusterId;
  }

  String[] getArgv()
  {
    return _argv;
  }

  boolean isElasticServer()
  {
    return _isElasticServer;
  }

  boolean isElasticDns()
  {
    return getArgBoolean("-elastic-dns", false);
  }

  String getDynamicAddress()
  {
    if (! _isElasticServer) {
      return null;
    }
    else if (_elasticAddress != null)
      return _elasticAddress;
    else {
      try {
        return HostUtil.getLocalHostAddress();
        // return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        return null;
      }
    }
  }

  String getDynamicDisplayAddress()
  {
    if (_elasticAddress != null)
      return _elasticAddress;
    else if (CurrentTime.isTest())
      return "192.168.1.x";
    else {
      try {
        return HostUtil.getLocalHostAddress();
        // return InetAddress.getLocalHost().getHostAddress();
      } catch (Exception e) {
        return null;
      }
    }
  }

  int getDynamicPort()
  {
    return _elasticPort;
  }

  String getElasticServerId()
  {
    if (_serverId != null) {
      return _serverId;
    }
    else {
      return "dyn-" + getDynamicDisplayAddress() + ":" + getDynamicPort();
    }
  }

  boolean isVerbose()
  {
    return _isVerbose;
  }

  void setWatchdogPort(int port)
  {
    _watchdogPort = port;
  }

  int getWatchdogPort()
  {
    return _watchdogPort;
  }

  void setResinHome(Path resinHome)
  {
    _resinHome = resinHome;
    
    System.setProperty("resin.home", resinHome.getNativePath());
  }

  void setRootDirectory(Path resinRoot)
  {
    _rootDirectory = resinRoot;
    
    System.setProperty("resin.root", resinRoot.getNativePath());
  }

  boolean is64Bit()
  {
    return _is64bit;
  }

  BootCommand getCommand()
  {
    return _command;
  }

  public ArrayList<String> getTailArgs()
  {
    return _tailArgs;
  }

  public String getArg(String arg)
  {
    for (int i = 0; i + 1 < _argv.length; i++) {
      if (_argv[i].equals(arg) || _argv[i].equals("-" + arg)) {
        return _argv[i + 1];
      }
    }

    return null;
  }

  public String getArgFlag(String arg)
  {
    for (int i = 0; i < _argv.length; i++) {
      if (_argv[i].equals(arg)
          || _argv[i].equals("-" + arg))
        return _argv[i];

      else if (_argv[i].startsWith(arg + "=")) {
        return _argv[i].substring(arg.length() + 1);
      }
      else if (_argv[i].startsWith("-" + arg + "=")) {
        return _argv[i].substring(arg.length() + 2);
      }
    }

    return null;
  }

  public boolean getArgBoolean(String arg, boolean defaultValue)
  {
    String value = getArgFlag(arg);

    if (value == null)
      return defaultValue;

    if ("no".equals(value) || "false".equals(value))
      return false;
    else
      return true;
  }

  public int getArgInt(String arg, int defaultValue)
  {
    String value = getArg(arg);

    if (value == null)
      return defaultValue;

    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      NumberFormatException e1
        = new NumberFormatException(L().l("{0} argument is not a number '{1}'",
                                        arg, value));
      e1.setStackTrace(e.getStackTrace());

      throw e;
    }
  }

  public boolean hasOption(String option)
  {
    for (String arg : _argv) {
      if (arg.equals(option) || arg.equals('-' + option))
        return true;
    }

    return false;
  }

  public boolean isResinProfessional()
  {
    return getELContext().isResinProfessional();
  }

  public ResinELContext getELContext()
  {
    if (_resinBootELContext == null)
      _resinBootELContext = new ResinBootELContext();

    return _resinBootELContext;
  }

  /**
   * finds first argument that follows no dash prefixed token
   * @return
   */
  public String getDefaultArg()
  {
    String defaultArg = null;

    if (_defaultArgs.length > 0)
      defaultArg = _defaultArgs[0];

    return defaultArg;
  }

  public String []getDefaultArgs() {
    return _defaultArgs;
  }

  public boolean isHelp()
  {
    return _isHelp;
  }

  private void setLogLevel(String levelName)
  {
    Level level = Level.INFO;

    if ("off".equals(levelName))
      level = Level.OFF;
    else if ("all".equals(levelName))
      level = Level.ALL;
    else if ("severe".equals(levelName))
      level = Level.SEVERE;
    else if ("warning".equals(levelName))
      level = Level.WARNING;
    else if ("info".equals(levelName))
      level = Level.INFO;
    else if ("config".equals(levelName))
      level = Level.CONFIG;
    else if ("fine".equals(levelName))
      level = Level.FINE;
    else if ("finer".equals(levelName))
      level = Level.FINER;
    else if ("finest".equals(levelName))
      level = Level.FINEST;

    Logger.getLogger("").setLevel(level);
  }

  private void parseCommandLine(final String[] argv)
  {
    String resinConf = null;

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];
      
      String resinArg = arg;
      
      if (resinArg.startsWith("-") && ! resinArg.startsWith("--")) {
        resinArg = "-" + resinArg;
      }
      
      if ("--conf".equals(resinArg)) {
        resinConf = argv[i + 1];
        i++;
      }
      else if ("--user-properties".equals(resinArg)) {
        _userProperties = Vfs.lookup(argv[i + 1]);
        i++;
      }
      else if ("--mode".equals(resinArg)) {
        _mode = argv[i + 1];
        i++;
      }
      else if ("--join-cluster".equals(resinArg)) {
        setElasticServer(true);
        _clusterId = argv[i + 1];
        i++;
      }
      else if ("--elastic-server".equals(resinArg)) {
        setElasticServer(true);
      }
      else if ("--cluster".equals(resinArg)) {
        _clusterId = argv[i + 1];
        i++;
      }
      /* XXX: conflicts with log-level
      else if ("--fine".equals(resinArg)) {
        _isVerbose = true;
        Logger.getLogger("").setLevel(Level.FINE);
      }
      else if ("--finer".equals(resinArg)) {
        _isVerbose = true;
        Logger.getLogger("").setLevel(Level.FINER);
      }
      */
      else if ("--data-directory".equals(resinArg)) {
        _dataDirectory = Vfs.lookup(argv[i + 1]);
        argv[i + 1] = _dataDirectory.getFullPath();
        i++;
      }
      else if ("--log-directory".equals(resinArg)) {
        _logDirectory = _rootDirectory.lookup(argv[i + 1]);
        i++;
      }
      else if ("--license-directory".equals(resinArg)) {
        _licenseDirectory = _rootDirectory.lookup(argv[i + 1]);
        i++;
      }
      else if ("--resin-home".equals(resinArg)) {
        setResinHome(Vfs.lookup(argv[i + 1]));
        argv[i + 1] = _resinHome.getFullPath();
        i++;
      }
      else if ("--root-directory".equals(resinArg)
               || "--server-root".equals(resinArg)
               || "--resin-root".equals(resinArg)) {
        setRootDirectory(Vfs.lookup(argv[i + 1]));
        argv[i + 1] = _rootDirectory.getFullPath();
        i++;
      }
      else if ("--server".equals(resinArg)) {
        _serverId = argv[i + 1];
        
        if ("".equals(_serverId))
          _serverId = "default";
        
        i++;
      }
      else if ("--stage".equals(resinArg)) {
        // skip stage
        i++;
      }
      else if ("--preview".equals(resinArg)) {
        // pass to server
      }
      else if ("--watchdog-port".equals(resinArg)) {
        _watchdogPort = Integer.parseInt(argv[i + 1]);
        i++;
      }
      else if (arg.startsWith("-J")
               || arg.startsWith("-D")
               || arg.startsWith("-X")) {
        if (arg.equals("-J-d64")) {
          _is64bit = true;
        }
      }
      else if (arg.equals("-d64")) {
        _is64bit = true;
      }
      else if (arg.equals("-d32")) {
        _is64bit = false;
      }
      else if ("--debug-port".equals(resinArg)) {
        i++;
      }
      else if ("--jmx-port".equals(resinArg)) {
        i++;
      }
      else if ("--dump-heap-on-exit".equals(resinArg)) {
      }
      else if ("--verbose".equals(resinArg)) {
        _isVerbose = true;
        Logger.getLogger("").setLevel(Level.CONFIG);
      }
      else if ("help".equals(resinArg) 
               || "--help".equals(resinArg)) {
        _isHelp = true;
      }
      else if ("version".equals(resinArg)) {
        System.out.println(VersionFactory.getFullVersion());
        System.exit(0);
      }
      else if (_commandMap.get(arg) != null) {
        _command = _commandMap.get(arg);
      }
      else if (_command != null) {
        if (_command.isFlag(arg)) {
        }
        else if (_command.isValueOption(arg)) {
          i++;
        }
        else {
          _tailArgs.add(arg);
        }
      }
      else if (_isHelp) {
      }
/*
      else {
        System.out.println(L().l("unknown argument '{0}'", argv[i]));
        System.out.println();
        usage();
        System.exit(1);
      }
*/  //#4605 (support before / after command option placement)
    }

    if (_isHelp && _command == null) {
      usage();

      System.exit(1);
    } else if (_isHelp && _command != null) {
      _command.usage(isVerbose());

      System.exit(0);
    } else if (_command == null) {
      System.out.println(L().l("Resin requires a command:{0}",
                               getCommandList()));
      System.exit(1);
    } else if (! _isHelp) {

      try {
        validateArgs(argv);
      } catch (BootArgumentException e) {
        if (_command != null) {
          System.err.println(_command.getName() + ": " + e.getMessage());
        }
        else {
          System.err.println("unknown: " + e.getMessage());
        }
        
        System.err.println();

        _command.usage(isVerbose());

        System.exit(14);
      }
    }

    List<String> defaultArgs = parseDefaultArgs();

    _defaultArgs = defaultArgs.toArray(new String[defaultArgs.size()]);

    if (resinConf != null) {
      _resinConf = Vfs.getPwd().lookup(resinConf);

      if (! _resinConf.exists() && _rootDirectory != null)
        _resinConf = _rootDirectory.lookup(resinConf);

      if (! _resinConf.exists() && _resinHome != null)
        _resinConf = _resinHome.lookup(resinConf);

      if (! _resinConf.exists())
        throw new ConfigException(L().l("Resin/{0} can't find configuration file '{1}'", VersionFactory.getVersion(), _resinConf.getNativePath()));
    }
  }

  private List<String> parseDefaultArgs()
  {
    LinkedList<String> defaultArgs = new LinkedList<String>();

    for (int i = _tailArgs.size() - 1; i >= 0; i--) {
      String arg = _tailArgs.get(i);

      if (_command.isFlag(arg))
        break;

      String xarg = null;

      if (i > 0)
        xarg = _tailArgs.get(i - 1);

      if (xarg == null) {
      } else if (_command.isValueOption(xarg)) {
        break;
      } else if (_command.isIntValueOption(xarg)){
        break;
      }

      defaultArgs.addFirst(arg);
    }

    return defaultArgs;
  }

  private boolean matchName(BootCommand command, String name) {
    Set<Map.Entry<String,BootCommand>> entries = _commandMap.entrySet();

    for (Map.Entry<String,BootCommand> entry : entries) {
      if(! command.getClass().equals(entry.getValue().getClass()))
        continue;

      if (name.equals(entry.getKey()))
        return true;
    }

    return false;
  }

  private void validateArgs(String []argv) throws BootArgumentException
  {
    boolean defaultArgEncountered = false;

    for (int i = 0; i < argv.length; i++) {
      final String arg = argv[i];

      if (matchName(_command, arg))
        continue;

      if (arg.charAt(0) != '-' && _command.isDefaultArgsAccepted()) {
        defaultArgEncountered = true;

        continue;
      }

      if (defaultArgEncountered && arg.charAt(0) == '-') {
        throw new BootArgumentException(L.l(
          "Only default arguments are expected at '{0}'",
          arg));
      }

      if (arg.startsWith("-J")
          || arg.startsWith("-D")
          || arg.startsWith("-X")) {
        continue;
      }

      if (arg.equals("-d64") || arg.equals("-d32")) {
        continue;
      }

      if (_command.isFlag(arg)) {
        continue;
      }
      else if (_command.isValueOption(arg)) {
      }
      else if (_command.isIntValueOption(arg)) {
      }
      else {
        throw new BootArgumentException(L.l("unknown argument '{0}'", arg));
      }

      if (i + 1 == argv.length)
        throw new BootArgumentException(L.l("option '{0}' requires a value",
                                              arg));
      String value = argv[++i];

      if (_command.isFlag(value)
          || _command.isValueOption(value)
          || _command.isIntValueOption(value))
        throw new BootArgumentException(L.l("option '{0}' requires a value",
                                            arg));

      if (_command.isIntValueOption(arg)) {
        try {
          Long.parseLong(value);
        } catch (NumberFormatException e) {
          throw new BootArgumentException(L.l("'{0}' argument must be a number: `{1}'", arg, value));
        }
      }
    }
  }

  private static void usage()
  {
    System.err.println(L().l("usage: bin/resin.sh [-options] <command> [values]"));
    System.err.println(L().l("       bin/resin.sh help <command>"));
    System.err.println(L().l(""));
    System.err.println(L().l("where command is one of:"));
    System.err.println(getCommandList());
  }

  private static String getCommandList()
  {
    StringBuilder sb = new StringBuilder();

    ArrayList<BootCommand> commands = new ArrayList<BootCommand>();
    commands.addAll(_commandMap.values());

    Collections.sort(commands, new CommandNameComparator());

    BootCommand lastCommand = null;

    for (BootCommand command : commands) {
      if (lastCommand != null && lastCommand.getClass() == command.getClass())
        continue;

      sb.append("\n  ");
      sb.append(command.getName());
      sb.append(" - ");
      sb.append(command.getDescription());
      if (command.isProOnly())
        sb.append(" (Resin-Pro)");

      lastCommand = command;
    }

    sb.append("\n  help <command> - prints command usage message");
    sb.append("\n  version - prints version");

    return sb.toString();
  }

  private String []fillArgv(String []argv)
  {
    ArrayList<String> args = new ArrayList<String>();

    Environment.init();

    String []jvmArgs = getJvmArgs();

    if (jvmArgs != null) {
      for (int i = 0; i < jvmArgs.length; i++) {
        String arg = jvmArgs[i];

        if (args.contains(arg)) {
          continue;
        }

        if (arg.startsWith("-Djava.class.path=")) {
          // IBM JDK
        }
        else if (arg.startsWith("-D")) {
          int eqlSignIdx = arg.indexOf('=');
          if (eqlSignIdx == -1) {
            args.add("-J" + arg);
          } else {
            String key = arg.substring(2, eqlSignIdx);
            String value = System.getProperty(key);

            if (value == null)
              value = "";

            args.add("-J-D" + key + "=" + value);
          }
        }
      }
    }

    for (int i = 0; i < argv.length; i++) {
      args.add(argv[i]);
    }

    argv = new String[args.size()];

    args.toArray(argv);

    return argv;
  }
  
  private String []getJvmArgs()
  {
    try {
      MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
      ObjectName name = new ObjectName("java.lang:type=Runtime");
      
      return (String []) mbeanServer.getAttribute(name, "InputArguments");
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
      
      return null;
    }
    
  }

  private static L10N L()
  {
    if (L == null)
      L = new L10N(WatchdogArgs.class);

    return L;
  }

  //
  // Utility static methods
  //

  static Path calculateResinHome()
  {
    String resinHome = System.getProperty("resin.home");

    if (resinHome != null) {
      return Vfs.lookup(resinHome);
    }

    // find the resin.jar as described by the classpath
    // this may differ from the value given by getURL() because of
    // symbolic links
    String classPath = System.getProperty("java.class.path");

    if (classPath.indexOf("resin.jar") >= 0) {
      int q = classPath.indexOf("resin.jar") + "resin.jar".length();
      int p = classPath.lastIndexOf(File.pathSeparatorChar, q - 1);

      String resinJar;

      if (p >= 0)
        resinJar = classPath.substring(p + 1, q);
      else
        resinJar = classPath.substring(0, q);

      return Vfs.lookup(resinJar).lookup("../..");
    }

    ClassLoader loader = ClassLoader.getSystemClassLoader();

    URL url = loader.getResource("com/caucho/boot/ResinBoot.class");

    String path = url.toString();

    if (! path.startsWith("jar:"))
      throw new RuntimeException(L().l("Resin/{0}: can't find jar for ResinBoot in {1}",
                                 VersionFactory.getVersion(), path));

    int p = path.indexOf(':');
    int q = path.indexOf('!');

    path = path.substring(p + 1, q);

    Path pwd = Vfs.lookup(path).getParent().getParent();

    return pwd;
  }

  static Path calculateResinRoot(Path resinHome)
  {
    String resinRoot = System.getProperty("resin.root");

    if (resinRoot != null)
      return Vfs.lookup(resinRoot);

    resinRoot = System.getProperty("server.root");

    if (resinRoot != null)
      return Vfs.lookup(resinRoot);

    return resinHome;
  }

  static String calculateClassPath(Path resinHome)
    throws IOException
  {
    ArrayList<String> classPath = new ArrayList<String>();

    return calculateClassPath(classPath, resinHome);
  }

  static String calculateClassPath(ArrayList<String> classPath,
                                   Path resinHome)
    throws IOException
  {
    String oldClassPath = System.getProperty("java.class.path");
    if (oldClassPath != null) {
      for (String item : oldClassPath.split("[" + File.pathSeparatorChar + "]")) {
        addClassPath(classPath, item);
      }
    }

    oldClassPath = System.getenv("CLASSPATH");
    if (oldClassPath != null) {
      for (String item : oldClassPath.split("[" + File.pathSeparatorChar + "]")) {
        addClassPath(classPath, item);
      }
    }

    Path javaHome = Vfs.lookup(System.getProperty("java.home"));

    if (javaHome.lookup("lib/tools.jar").canRead())
      addClassPath(classPath, javaHome.lookup("lib/tools.jar").getNativePath());
    else if (javaHome.getTail().startsWith("jre")) {
      String tail = javaHome.getTail();
      tail = "jdk" + tail.substring(3);
      Path jdkHome = javaHome.getParent().lookup(tail);

      if (jdkHome.lookup("lib/tools.jar").canRead())
        addClassPath(classPath, jdkHome.lookup("lib/tools.jar").getNativePath());
    }

    if (javaHome.lookup("../lib/tools.jar").canRead())
      addClassPath(classPath, javaHome.lookup("../lib/tools.jar").getNativePath());

    Path resinLib = resinHome.lookup("lib");

    if (resinLib.lookup("pro.jar").canRead())
      addClassPath(classPath, resinLib.lookup("pro.jar").getNativePath());
    addClassPath(classPath, resinLib.lookup("resin.jar").getNativePath());
    //    addClassPath(classPath, resinLib.lookup("jaxrpc-15.jar").getNativePath());

    String []list = resinLib.list();

    for (int i = 0; i < list.length; i++) {
      if (! list[i].endsWith(".jar"))
        continue;

      Path item = resinLib.lookup(list[i]);

      String pathName = item.getNativePath();

      if (! classPath.contains(pathName))
        addClassPath(classPath, pathName);
    }

    String cp = "";

    for (int i = 0; i < classPath.size(); i++) {
      if (! "".equals(cp))
        cp += File.pathSeparatorChar;

      cp += classPath.get(i);
    }

    return cp;
  }

  private static void addClassPath(ArrayList<String> cp, String item)
  {
    if (! cp.contains(item))
      cp.add(item);
  }

  public class ResinBootELContext
    extends ResinELContext
  {
    private final AtomicBoolean _isLicenseCheck = new AtomicBoolean();
    private boolean _isResinProfessional;

    @Override
    public Path getResinHome()
    {
      return WatchdogArgs.this.getResinHome();
    }

    @Override
    public Path getRootDirectory()
    {
      return WatchdogArgs.this.getRootDirectory();
    }

    @Override
    public Path getLogDirectory()
    {
      return WatchdogArgs.this.getLogDirectory();
    }

    @Override
    public Path getResinConf()
    {
      return WatchdogArgs.this.getResinConf();
    }

    @Override
    public String getServerId()
    {
      String serverId = (String) Config.getProperty("rvar0");
      
      if (serverId != null) {
        return serverId;
      }
      else {
        return WatchdogArgs.this.getServerId();
      }
    }

    @Override
    public boolean isResinProfessional()
    {
      return isProfessional();
    }

    public boolean isProfessional()
    {
      loadLicenses();

      return _isResinProfessional;
    }

    private void loadLicenses()
    {
      if (_isLicenseCheck.getAndSet(true)) {
        return;
      }

      LicenseCheck license;

      try {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        Class<?> cl = Class.forName("com.caucho.license.LicenseCheckImpl",
                                    false, loader);
        
        Constructor<?> ctor = cl.getConstructor(File[].class);
        
        ArrayList<File> licensePath = new ArrayList<File>();
        
        if (_licenseDirectory != null) {
          licensePath.add(new File(_licenseDirectory.getNativePath()));
        }
        
        Path path = getResinConf().getParent().lookup("licenses");

        if (path.isDirectory()) {
          File dir = new File(path.getNativePath());
          licensePath.add(dir);
        }
        
        File []files = new File[licensePath.size()];
        licensePath.toArray(files);

        license = (LicenseCheck) ctor.newInstance(new Object[] { files });

        license.requireProfessional(1);

        Vfs.initJNI();

        _isResinProfessional = true;

        // license.doLogging(1);
      } catch (Exception e) {
        log.finer(e.toString());
        log.log(Level.FINEST, e.toString(), e);
      }
    }
  }

  private static void addCommand(BootCommand command)
  {
    _commandMap.put(command.getName(), command);
  }

  static {
    addCommand(new ConfigCatCommand());
    addCommand(new ConfigDeployCommand());
    addCommand(new ConfigLsCommand());
    addCommand(new ConfigUndeployCommand());

    addCommand(new ConsoleCommand());
    addCommand(new DeployCatCommand());
    addCommand(new DeployCopyCommand());
    addCommand(new DeployCommand());
    addCommand(new DeployListCommand());
    addCommand(new DeployLsCommand());
    addCommand(new DisableCommand());
    addCommand(new DisableSoftCommand());
    addCommand(new EnableCommand());

    addCommand(new GuiCommand());

    addCommand(new HeapDumpCommand());
    addCommand(new JmxCallCommand());
    addCommand(new JmxDumpCommand());
    addCommand(new JmxListCommand());
    addCommand(new JmxSetCommand());
    addCommand(new JspcCommand());
    addCommand(new KillCommand());
    addCommand(new LicenseAddCommand());
    addCommand(new ListRestartsCommand());
    addCommand(new LogLevelCommand());

    addCommand(new PasswordEncryptCommand());
    addCommand(new PasswordGenerateCommand());
    addCommand(new PdfReportCommand());
    addCommand(new ProfileCommand());

    addCommand(new RestartCommand());

    addCommand(new ScoreboardCommand());
    addCommand(new ShutdownCommand());
    // addCommand(new StartCloudCommand());
    addCommand(new StartCommand());
    addCommand(new StartAllCommand());
    addCommand(new StartWithForegroundCommand());
    addCommand(new StatusCommand());
    addCommand(new StopCommand());

    addCommand(new ThreadDumpCommand());

    addCommand(new UndeployCommand());
    //addCommand(new UserAddCommand());
    //addCommand(new UserListCommand());
    //addCommand(new UserRemoveCommand());

    addCommand(new WatchdogCommand());

    addCommand(new WebAppDeployCommand());
    addCommand(new WebAppRestartCommand());
    addCommand(new WebAppRestartClusterCommand());
    addCommand(new WebAppStartCommand());
    addCommand(new WebAppStopCommand());
    addCommand(new WebAppUndeployCommand());

    _commandMap.put("copy", new DeployCopyCommand());
    _commandMap.put("list", new DeployListCommand());

    _commandMap.put("deploy-start", new WebAppStartCommand());
    _commandMap.put("deploy-stop", new WebAppStopCommand());
    _commandMap.put("deploy-restart", new WebAppRestartCommand());
    _commandMap.put("deploy-restart-cluster", new WebAppRestartClusterCommand());

    _commandMap.put("generate-password", new PasswordGenerateCommand());

    _commandMap.put("start-webapp", new WebAppStartCommand());
    _commandMap.put("stop-webapp", new WebAppStopCommand());
    _commandMap.put("restart-webapp", new WebAppRestartCommand());
  }

  static class CommandNameComparator implements Comparator<BootCommand> {
    public int compare(BootCommand a, BootCommand b)
    {
      return a.getName().compareTo(b.getName());
    }
  }
}
