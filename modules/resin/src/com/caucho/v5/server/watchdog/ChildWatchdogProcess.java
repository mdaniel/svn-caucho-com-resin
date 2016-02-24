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

import io.baratine.service.Result;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.ServiceManagerAmp;
import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.amp.thread.ThreadPool;
import com.caucho.v5.bartender.hamp.LinkHamp;
import com.caucho.v5.cli.server.OpenPort;
import com.caucho.v5.cli.spi.CommandManager;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.HealthSystemFacade;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.io.ServerSocketBar;
import com.caucho.v5.jmx.MBeanServerBuilderImpl;
import com.caucho.v5.jni.Boot;
import com.caucho.v5.jni.JniBoot;
import com.caucho.v5.jni.JniProcess;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.loader.EnvironmentClassLoader;
import com.caucho.v5.log.LogManagerImpl;
import com.caucho.v5.log.impl.RotateLog;
import com.caucho.v5.log.impl.RotateStream;
import com.caucho.v5.server.container.LinkChildService;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.WaitFuture;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.VfsOld;
import com.caucho.v5.vfs.WriteStream;

/**
 * Encapsulation of the process running resin.
 *
 * Each WatchdogProcess instance corresponds to a Resin JVM.  When the
 * JVM exits, the WatchdogProcess will finish.  It is not reused on a
 * restart.
 */
class ChildWatchdogProcess
{
  private static final L10N L = new L10N(ChildWatchdogProcess.class);
  private static final Logger log
    = Logger.getLogger(ChildWatchdogProcess.class.getName());
  
  private static HashSet<String> _daemonProperties
    = new HashSet<>();
  
  private static Boot _jniBoot;
  private static CommandManager<? extends ArgsWatchdog> _serverOptions;

  private final String _id;
  private final SystemManager _system;
  private final ChildWatchdog _child;
  private final Lifecycle _lifecycle = new Lifecycle();

  private LinkWatchdogServiceImpl _linkWatchdogImpl;
  private LinkWatchdogService _linkWatchdog;
  
  private ChildWatchdogTask _task;

  private Socket _childSocket;
  private AtomicReference<Process> _processRef
    = new AtomicReference<>();
  private OutputStream _stdOs;
  private int _pid;

  private long _startTime;
  private int _status = -1;
  private String _exitMessage;
  private ExitCode _exitCode;
  
  private WaitFuture<Integer> _exitFuture = new WaitFuture<>();

  ChildWatchdogProcess(String id,
                       SystemManager system,
                       ChildWatchdog child,
                       ChildWatchdogTask task)
  {
    _id = id;
    _system = system;
    _child = child;
    _task = task;
  }

  int getPid()
  {
    return _pid;
  }
  
  public String getId()
  {
    return _id;
  }

  public int getStatus()
  {
    return _status;
  }
  
  public String getExitMessage()
  {
    return _exitMessage;
  }
  
  public void setShutdownMessage(String msg)
  {
    _task.setShutdownMessage(msg);
  }
  
  public long getUptime()
  {
    long now = CurrentTime.getCurrentTime();
    long start = _startTime;
    
    if (_startTime > 0)
      return now - start;
    else
      return 0;
  }
  
  private ArgsWatchdog getArgs()
  {
    return _child.getArgs();
  }

  public void run()
  {
    if (! _lifecycle.toActive()) {
      return;
    }

    EnvironmentClassLoader envLoader
      = EnvironmentClassLoader.create(_system.getClassLoader());
    
    Thread thread = Thread.currentThread();
    
    WriteStream jvmOut = null;
    ServerSocket ss = null;
    Socket s = null;

    try {
      _startTime = CurrentTime.getCurrentTime();
      
      thread.setContextClassLoader(envLoader);
      
      envLoader.setAdminEnable(false);
      envLoader.start();

      ss = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));

      int port = ss.getLocalPort();
      
      if (port <= 0) {
        throw new IllegalStateException("Invalid server socket: " + ss);
      }

      ThreadPool.current().start(new InfoThread(ss));
      
      String program = getArgs().getDisplayName();
      
      log.warning("Watchdog starting " + program + "[" + _child.getId() + "] with local-port=" + port);

      jvmOut = createJvmOut();
      
      Process process = createProcess(port, jvmOut); 
      
      if (process != null) {
        _processRef.compareAndSet(null, process);
        
        if (process instanceof JniProcess)
          _pid = ((JniProcess) process).getPid();
        else
          _pid = 0;

        InputStream stdIs = process.getInputStream();
        _stdOs = process.getOutputStream();

        WatchdogProcessLogThread logThread
          = new WatchdogProcessLogThread(stdIs, jvmOut);

        ThreadPool.current().start(logThread);

        // ThreadPool.getCurrent().start(new InfoThread(ss));

        _status = process.waitFor();
        
        _exitFuture.completed(_status);
        
        logStatus(_status);
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      log.log(Level.WARNING, e.toString(), e);

      try {
        Thread.sleep(5000);
      } catch (Exception e1) {
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      _startTime = 0;

      if (_status < 0) {
        _status = 666;
        
        _exitFuture.completed(_status);
      }
      
      if (_linkWatchdog != null) {
        _linkWatchdog.linkClosed();
      }
      
      if (ss != null) {
        try {
          ss.close();
        } catch (Throwable e) {
        }
      }

      try {
        if (s != null)
          s.close();
      } catch (Throwable e) {
        log.log(Level.FINER, e.toString(), e);
      }

      kill();

      if (jvmOut != null) {
        try {
          synchronized (jvmOut) {
            jvmOut.close();
          }
        } catch (Exception e) {
        }
      }

      thread.setContextClassLoader(_system.getClassLoader());
    }
  }
  
  private void logStatus(int status)
  {
    String type = "unknown";
    String code = " (exit code=" + status + ")";
    
    _exitCode = ExitCode.UNKNOWN;
    
    if (status == 0) {
      type = "normal exit";
      
      _exitCode = ExitCode.OK;
    }
    else if (status >= 0 && status < ExitCode.values().length) {
      _exitCode = ExitCode.values()[status];
      
      type = _exitCode.toString();
    }
    else if (status >= 128 && status < 128 + 32) {
      _exitCode = ExitCode.SIGNAL;
      
      switch (status - 128) {
      case 1:
        type = "SIGHUP";
        break;
      case 2:
        type = "SIGINT";
        break;
      case 3:
        type = "SIGQUIT";
        break;
      case 4:
        type = "SIGILL";
        break;
      case 5:
        type = "SIGTRAP";
        break;
      case 6:
        type = "SIGABRT";
        break;
      case 7:
        type = "SIGBUS";
        break;
      case 8:
        type = "SIGFPE";
        break;
      case 9:
        type = "SIGKILL";
        break;
      case 10:
        type = "SIGUSR1";
        break;
      case 11:
        type = "SIGSEGV";
        break;
      case 12:
        type = "SIGUSR2";
        break;
      case 13:
        type = "SIGPIPE";
        break;
      case 14:
        type = "SIGALRM";
        break;
      case 15:
        type = "SIGTERM";
        break;
      case 19:
        type = "SIGSTOP";
        break;
      default:
        type = "signal=" + (status - 128);
        break;
      }
      
      code = " (signal=" + (status - 128) + ")";
    }
    
    String program = _child.getArgs().getProgramName();

    String msg = ("Watchdog detected close of "
                  + program + "[" + _child.getId() + ",pid=" + _pid + "]"
                  + "\n  exit reason: " + type + code);
    
    log.warning(msg);
    
    _exitMessage = msg;
    
    _child.getService().onChildExit(msg);
  }

  /**
   * Stops the instance, waiting for the completion.
   */
  void stop(ShutdownModeAmp mode,
            Result<String> result)
  {
    _lifecycle.toDestroy();

    if (_linkWatchdogImpl != null) {
      _linkWatchdogImpl.sendShutdown(mode, result);
    }
    else {
      result.ok(L.l("server is already shut down"));
    }
  }

  void kill()
  {
    _lifecycle.toDestroy();

    Process process = _processRef.getAndSet(null);

    if (process != null) {
      try {
        process.destroy();
      } catch (Exception e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    OutputStream stdOs = _stdOs;
    _stdOs = null;

    if (stdOs != null) {
      try {
        stdOs.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }

    Socket childSocket = _childSocket;
    _childSocket = null;

    if (childSocket != null) {
      try {
        childSocket.close();
      } catch (Throwable e) {
        log.log(Level.FINE, e.toString(), e);
      }
    }
    
    if (process != null) {
      try {
        process.waitFor();
      } catch (Exception e) {
        log.log(Level.INFO, e.toString(), e);
      }
    }
  }

  void waitForExit(long timeout, TimeUnit unit)
  {
    try {
      _exitFuture.get(timeout, unit);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Waits for a socket connection from the child, returning the socket
   *
   * @param ss TCP ServerSocket from the watchdog for the child to connect to
   */
  private void connectToChild(ServerSocket ss)
    throws IOException
  {
    try {
      Socket s = null;
      
      ss.setSoTimeout(60000);

      for (int i = 0; _lifecycle.isActive() && i < 120 && s == null; i++) {
        try {
          s = ss.accept();
        } catch (SocketTimeoutException e) {
        }
      }
      
      if (log.isLoggable(Level.FINER)) {
        log.finer("Accepted socket '" + s + "' from child server *:" + _child.getPort());
      }

      if (s != null) {
        _childSocket = s;

        startWatchdogActor(s);
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      ss.close();
    }
  }

  /**
   * Starts the HAMP actor to communicate with the child.
   *
   * @param s the socket to the child.
   */
  private void startWatchdogActor(Socket s)
    throws IOException
  {
    InputStream watchdogIs = s.getInputStream();
    OutputStream watchdogOs = s.getOutputStream();
    
    ServiceManagerAmp manager
      = ServiceManagerAmp.newManager()
                         .name("watchdog-baratine-link-" + _child.getPort())
                         .start();

    LinkHamp link = new LinkHamp(manager,
                                 "remote://",
                                 watchdogIs, 
                                 watchdogOs);
    
    _linkWatchdogImpl = new LinkWatchdogServiceImpl(this, _child.getService(), link);

    _linkWatchdog = link.newService(_linkWatchdogImpl)
                        .address("public:///watchdog")
                        .as(LinkWatchdogService.class);
    
    try {
      ThreadPool.current().schedule(link);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
  }

  public LinkChildService getLinkServiceServer()
  {
    if (_linkWatchdogImpl != null) {
      return _linkWatchdogImpl.getServerLinkService();
    }
    else {
      return null;
    }
  }
  /**
   * Creates a new Process for the Resin JVM, initializing the environment
   * and passing value to the new process.
   *
   * @param socketPort the watchdog socket port
   * @param out the debug log jvm-default.log
   */
  private Process createProcess(int socketPort,
                                WriteStream out)
    throws IOException
  {
    // watchdog/0210
    // Path pwd = rootDirectory;
    PathImpl chroot = _child.getChroot();
    PathImpl processPwd = _child.getPwd();

    HashMap<String,String> env = buildEnv();

    ArrayList<String> jvmArgs = buildJvmArgs();

    ArrayList<String> cliArgs = new ArrayList<>();
    
    ArrayList<String> resinOptions = buildResinOptions(socketPort);

    addCommandLineArguments(cliArgs, jvmArgs, resinOptions);

    Class<?> mainClass = getMainClass();
    
    jvmArgs.add(mainClass.getName());

    jvmArgs.addAll(cliArgs);

    if (_child.isVerbose()) {
      logVerboseArguments(out, jvmArgs);

      logVerboseEnvironment(out, env);
      
      log.finer(getArgs().getProgramName() + " launching: " + jvmArgs);
    }

    Boot boot = getJniBoot();
    if (boot != null) {
      boot.clearSaveOnExec();

      ArrayList<ServerSocketBar> boundSockets = new ArrayList<ServerSocketBar>();

      try {
        if (_child.getUserName() != null) {
          for (OpenPort port : _child.getPorts()) {
            ServerSocketBar ss = port.bindForWatchdog();

            if (ss == null)
              continue;

            boundSockets.add(ss);

            if (ss.setSaveOnExec()) {
              jvmArgs.add("--bound-port");
              jvmArgs.add(String.valueOf(ss.getSystemFD()));
              jvmArgs.add(String.valueOf(port.getAddress()));
              jvmArgs.add(String.valueOf(port.getPort()));
            }
            else {
              ss.close();
            }
          }
        }

        String chrootPath = null;

        if (chroot != null)
          chrootPath = chroot.getNativePath();

        Process process = boot.exec(jvmArgs, env,
                                    chrootPath,
                                    processPwd.getNativePath(),
                                    _child.getUserName(),
                                    _child.getGroupName());

        if (process != null) {
          return process;
        }
      } catch (ConfigException e) {
        log.warning(e.getMessage());
        
        if (log.isLoggable(Level.FINER)) {
          log.log(Level.FINER, e.toString(), e);
        }
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

    if (_child.getUserName() != null) {
      throw new ConfigException(L.l("<user-name> '{0}' requires compiled JNI and a valid operating-system user name.", 
                                    _child.getUserName()));
    }

    if (_child.getGroupName() != null) {
      throw new ConfigException(L.l("<group-name> requires compiled JNI."));
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(processPwd.getNativePath()));

    builder.environment().putAll(env);

    builder = builder.command(jvmArgs);

    builder.redirectErrorStream(true);

    return builder.start();
  }
  
  private Class<?> getMainClass()
  {
    try {
      String programName = getArgs().getProgramName();
      
      String name = (Character.toUpperCase(programName.charAt(0)) +
                     programName.substring(1));
      
      String className = "com.caucho.v5.server.main." + name + "Server";
      
      return Class.forName(className);
    } catch (Exception e) {
      //log.log(Level.FINER, e.toString(), e);
      throw ConfigException.wrap(e);
    }
    
    //return Resin.class;
  }

  private HashMap<String,String> buildEnv()
    throws IOException
  {
    HashMap<String,String> env = new HashMap<String,String>();

    env.putAll(System.getenv());
    
    PathImpl homeDir = _child.getHomeDir();

    ArrayList<String> classPathList = new ArrayList<String>();
    classPathList.addAll(_child.getJvmClasspath());
    String classPath
      = ArgsWatchdog.calculateClassPath(classPathList, homeDir, 
                                        getArgs().getProgramName());

    env.put("CLASSPATH", classPath);
    
    if ("Mac OS X".equals(System.getProperty("os.name"))) {
      buildMacosxLibraryPath(env);
    }

    return env;
  }
  
  /**
   * The newer versions of openssl for MacOS-X are in /usr/local/opt/openssl
   * 
   * The newer version is required for HTTP/2.0 (NPN is required)
   */
  private void buildMacosxLibraryPath(HashMap<String,String> env)
  {
    PathImpl path = VfsOld.lookup("/usr/local/opt/openssl/lib");
    
    if (path.isDirectory()) {
      String libpath = env.get("DYLD_LIBRARY_PATH");
      
      if (libpath == null || "".equals(libpath)) {
        libpath = path.getNativePath();
      }
      else {
        libpath = path.getNativePath() + ":" + libpath;
      }
      
      env.put("DYLD_LIBRARY_PATH", libpath);
    }
  }

  public static void appendEnvPath(Map<String,String> env,
                             String prop,
                             String value)
  {
    String oldValue = env.get(prop);

    if (oldValue == null && CauchoUtil.isWindows()) {
      String winProp = prop.toUpperCase(Locale.ENGLISH);
      oldValue = env.get(winProp);

      if (oldValue != null)
        prop = winProp;
    }

    if (oldValue != null && ! "".equals(oldValue))
      value = value + File.pathSeparator + oldValue;

    env.put(prop, value);
  }

  private ArrayList<String> buildJvmArgs()
  {
    ArrayList<String> jvmArgs = new ArrayList<String>();

    jvmArgs.add(_child.getJavaExe());
    
    boolean isEndorsed = false;

    // user args are first so they're displayed by ps
    for (String arg : _child.getJvmArgs()) {
      if (! isDaemonProperty(arg)) {
        jvmArgs.add(arg);
      }
      
      if (arg.startsWith("-Djava.endorsed.dirs")) {
        isEndorsed = true;
      }
    }
    
    jvmArgs.add("-Dresin.server=" + _id);

    jvmArgs.add("-Djava.util.logging.manager=" + LogManagerImpl.class.getName());

    // This is needed for JMX to work correctly.
    String systemClassLoader = _child.getSystemClassLoader();
    if (systemClassLoader != null && ! "".equals(systemClassLoader)) {
      jvmArgs.add("-Djava.system.class.loader=" + systemClassLoader);
    }
    
    PathImpl resinHome = _child.getHomeDir();
    PathImpl rootDir = _child.getRootDir();
    
    if (! isEndorsed) {
      String endorsed = System.getProperty("java.endorsed.dirs");
      
      String resinEndorsed = resinHome.getNativePath() + File.separator + "endorsed";
      
      resinEndorsed += (File.pathSeparator
                        + rootDir.getNativePath() + File.separator + "endorsed");
      
      if (endorsed != null)
        endorsed = endorsed + File.pathSeparator + resinEndorsed;
      else
        endorsed = resinEndorsed;
      
      jvmArgs.add("-Djava.endorsed.dirs=" + endorsed);
    }
    
    // #2567
    jvmArgs.add("-Djavax.management.builder.initial=" + MBeanServerBuilderImpl.class.getName());
    jvmArgs.add("-Djava.awt.headless=true");

    jvmArgs.add("-Dresin.home=" + resinHome.getFullPath());
    
    /*
    if (! _child.hasXss()) {
      jvmArgs.add("-Xss1m");
    }

    if (! _child.hasXmx()) {
      jvmArgs.add("-Xmx256m");
    }
    */
    
    // #4308, #4585
    if (CauchoUtil.isWindows())
      jvmArgs.add("-Xrs");

    if (_task.getPreviousExitCode() != null)
      jvmArgs.add("-Dresin.exit.code=" + _task.getPreviousExitCode().toString());

    /*
    if (_task.getRestartMessage() != null) {
      jvmArgs.add("-D" + HealthSystemFacade.RESIN_EXIT_MESSAGE
                  + "=" + _task.getRestartMessage());
    }
    */
    
    if (_task.getShutdownMessage() != null) {
      jvmArgs.add("-D" + HealthSystemFacade.EXIT_MESSAGE
                  + "=" + _task.getShutdownMessage());
    }

    String[] argv = _child.getArgv();

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      if (isDaemonProperty(arg)) {
      }
      else if (arg.startsWith("-D") || arg.startsWith("-X")) {
        jvmArgs.add(arg);
      }
      else if (arg.startsWith("-J")) {
        jvmArgs.add(arg.substring(2));
      }
      else if (arg.equals("-d64") || arg.equals("-d32")) {
        jvmArgs.add(arg);
      }
      else if ("--debug-port".equals(arg) || "-debug-port".equals(arg)) {
        jvmArgs.add("-Xdebug");
        jvmArgs.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address="
                    + argv[i + 1]);
        i++;
      }
      else if ("--jmx-port".equals(arg) || "-jmx-port".equals(arg)) {
        jvmArgs.add("-Dcom.sun.management.jmxremote.port=" + argv[i + 1]);
        jvmArgs.add("-Dcom.sun.management.jmxremote.authenticate=false");
        jvmArgs.add("-Dcom.sun.management.jmxremote.ssl=false");
        i++;
      }
    }

    if (! jvmArgs.contains("-d32") && ! jvmArgs.contains("-d64")
        && _child.is64bit() && ! CauchoUtil.isWindows()) {
      jvmArgs.add("-d64");
    }

    String jvmMode = _child.getJvmMode();
    
    String osName = System.getProperty("os.name");
    String osArch = System.getProperty("os.arch");
    
    if ((jvmMode == null || "".equals(jvmMode))
        && ! CauchoUtil.isWindows() && ! "none".equals(jvmMode)
        && (("Linux".equals(osName) && ("x86_64".equals(osArch)))
            || "Mac OS X".equals(osName))) {
      jvmMode = "-server";
    }
    // System.out.println("OSNA: " + osName + " " + osArch);
    
    if (! jvmArgs.contains("-server")
        && ! jvmArgs.contains("-client")
        && ! CauchoUtil.isWindows() && ! "none".equals(jvmMode)
        && jvmMode != null && ! "".equals(jvmMode)) {
      // #3331, windows can't add -server automatically
      jvmArgs.add(jvmMode);
    }

    return jvmArgs;
  }

  private ArrayList<String> buildResinOptions(int socketPort)
  {
    ArrayList<String> resinArgs = new ArrayList<String>();

    PathImpl resinRoot = _child.getRootDir();

    if (resinRoot != null) {
      resinArgs.add("--root-dir");
      resinArgs.add(resinRoot.getFullPath());
    }
    
    if (_child.getConfigPath() != null) {
      resinArgs.add("--conf");
      resinArgs.add(_child.getConfigPath().getURL());
    }
    
    if (_child.getPort() > 0) {
      resinArgs.add("--port");
      resinArgs.add(String.valueOf(_child.getPort()));
    }

    /*
    // server/2k54
    if (_watchdog.isDynamic()) {
      resinArgs.add("--dynamic-server");
      
      if (_watchdog.getElasticServerCluster() == null) {
        throw new IllegalStateException(_watchdog.toString());
      }
      
      resinArgs.add("--cluster");
      resinArgs.add(_watchdog.getElasticServerCluster());
      
      if (_watchdog.getElasticServerPort() == 0) {
        throw new IllegalStateException(_watchdog.toString());
      }
      
      resinArgs.add("--elastic-server-port");
      resinArgs.add(Integer.toString(_watchdog.getElasticServerPort()));
    }
    */
    
    resinArgs.add("--socketwait");
    resinArgs.add(String.valueOf(socketPort));

    return resinArgs;
  }

  private void addCommandLineArguments(ArrayList<String> cliArgs,
                                       ArrayList<String> jvmArgs,
                                       ArrayList<String> resinArgs)
  {
    boolean isCommand = false;
    String []argv = _child.getArgv();
    
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("--conf") || argv[i].equals("-conf")) {
        // resin conf handled below
        i++;
      }
      else if (isDaemonProperty(argv[i])) {
      }
      else if (isDaemonFlag(argv[i])) {
        cliArgs.add(argv[i]);
      }
      else if (isDaemonOption(argv[i])) {
        cliArgs.add(argv[i]);
        cliArgs.add(argv[i + 1]);
        i++;
      }
      else if (argv[i].startsWith("-Djava.class.path=")) {
        // IBM JDK startup issues
      }
      else if (argv[i].startsWith("-J")) {
        jvmArgs.add(argv[i].substring(2));
      }
      else if (argv[i].startsWith("-Djava.class.path")) {
      }
      else if (argv[i].startsWith("-D") || argv[i].startsWith("-X")) {
      }
      else if (argv[i].equals("-d64") || argv[i].startsWith("-d32")) {
      }
      else if (CauchoUtil.isWindows() && "".equals(argv[i])) {
        cliArgs.add("\"\"");
      }
      else if (! isCommand) {
        if (argv[i].startsWith("-")) {
          throw new IllegalStateException(L.l("unexpected argument {0}", argv[i]));
        }
        
        isCommand = true;
        cliArgs.add(argv[i]);
        cliArgs.addAll(resinArgs);
      }
      else {
        cliArgs.add(argv[i]);
      }
    }
    
  }
  
  private boolean isDaemonProperty(String arg)
  {
    int p;
    if (arg.startsWith("-D") && (p = arg.indexOf('=')) >= 0) {
      String key = arg.substring(0, p);

      return _daemonProperties.contains(key);
    }
    else if (arg.startsWith("-J-D") && (p = arg.indexOf('=')) >= 0) {
      String key = arg.substring(2, p);

      return _daemonProperties.contains(key);
    }
    
    return false;
  }
  
  private boolean isDaemonFlag(String arg)
  {
    return _serverOptions.isFlag(arg);
  }
  
  private boolean isDaemonOption(String arg)
  {
    return _serverOptions.isOption(arg);
  }

  private void logVerboseArguments(WriteStream out, ArrayList<String> list)
    throws IOException
  {
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

  private void logVerboseEnvironment(WriteStream out, Map<String,String> env)
    throws IOException
  {
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

  Boot getJniBoot()
  {
    Boot jniBoot = _jniBoot;
    
    if (jniBoot != null) {
      return jniBoot.isValid() ? jniBoot : null;
    }

    try {
      _jniBoot = jniBoot = new JniBoot();
    } catch (Throwable e) {
      // Throwable because JniBoot can throw link errors
      log.log(Level.FINE, e.toString(), e);
    }

    return jniBoot != null && jniBoot.isValid() ? jniBoot : null;
  }

  //
  // logging utilities
  //

  /**
   * Creates the log/jvm-default.log file where all the Resin log messages
   * go.
   */
  private WriteStream createJvmOut()
    throws IOException
  {
    PathImpl jvmPath = _child.getLogPath();
    
    try {
      PathImpl dir = jvmPath.getParent();

      if (! dir.exists()) {
        dir.mkdirs();

        String userName = _child.getUserName();
        
        if (userName != null) {
          dir.changeOwner(userName);
        }

        String groupName = _child.getGroupName();
        
        if (groupName != null) {
          dir.changeGroup(groupName);
        }
      }
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    RotateLog log = new RotateLog();
    //log.setPath(jvmPath);
    log.setRolloverSizeBytes(64L * 1024 * 1024);
    
    if (_child.getStdoutLog() != null) {
      _child.getStdoutLog().configure(log);
    }

    log.init();
    
    RotateStream rotateStream = log.getRotateStream();

    // _watchdog.getConfig().logInit(rotateStream);
    
    // rotateStream.init();
    //return rotateStream.getStream();
    return null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _child + "," + _id + "]";
  }

  /**
   * Watchdog thread responsible for writing jvm-default.log by reading the
   * JVM's stdout and copying it to the log.
   */
  class WatchdogProcessLogThread implements Runnable {
    private InputStream _is;
    private WriteStream _out;

    /**
     * @param is the stdout stream from the Resin
     * @param out stream to the log/jvm-default.log file
     */
    WatchdogProcessLogThread(InputStream is, WriteStream out)
    {
      _is = is;
      _out = out;
    }

    @Override
    public void run()
    {
      Thread thread = Thread.currentThread();
      thread.setName("watchdog-process-log-" + _pid + "-" + _id);

      thread.setContextClassLoader(ClassLoader.getSystemClassLoader());
      
      WriteStream out = _out;

      try {
        int len;

        byte []data = new byte[4096];

        while ((len = _is.read(data, 0, data.length)) > 0) {
          out.write(data, 0, len);
          out.flush();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
      } finally {
        try {
          synchronized (out) {
            out.close();
          }
        } catch (IOException e) {
        }

        kill();
      }
    }
  }
  
  private class InfoThread implements Runnable {
    private ServerSocket _ss;
    
    InfoThread(ServerSocket ss)
    {
      Objects.requireNonNull(ss);
      
      _ss = ss;
    }
    
    @Override
    public void run()
    {
      try {
        connectToChild(_ss);
  
        LinkChildService resinService = getLinkServiceServer();

        if (resinService != null) {
          getLinkServiceServer().startInfo(_child.isRestart(),
                                      _child.getRestartMessage(),
                                      _child.getPreviousExitCode(),
                                      _task.getShutdownMessage());
        }
        else {
          log.warning("Socket failed to connect: " + _child + " " + _ss);
          kill();
        }
      } catch (Throwable e) {
        log.log(Level.WARNING, e.toString(), e);
        e.printStackTrace();
        kill();
      }
    }
  }
  
  static {
    _daemonProperties.add("-Djava.awt.headless");
    _daemonProperties.add("-Djava.class.path");
    _daemonProperties.add("-Dhome.dir");
    _daemonProperties.add("-Droot.dir");
    _daemonProperties.add("-Dwatchdog");
    _daemonProperties.add("-Djavax.management.builder.initial");
    _daemonProperties.add("-Djava.util.logging.manager");
    
    ArgsWatchdog serverArgs = new ArgsWatchdog();
    
    _serverOptions = serverArgs.getCommandManager();
  }
}
