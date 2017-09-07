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
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.bootjni.JniProcess;
import com.caucho.config.ConfigException;
import com.caucho.env.health.HealthSystemFacade;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.env.thread.ThreadPool;
import com.caucho.hmtp.HmtpLinkWorker;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.loader.EnvironmentClassLoader;
import com.caucho.log.RotateLog;
import com.caucho.log.RotateStream;
import com.caucho.network.listen.TcpPort;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.QServerSocket;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.WriteStream;

/**
 * Encapsulation of the process running resin.
 *
 * Each WatchdogProcess instance corresponds to a Resin JVM.  When the
 * JVM exits, the WatchdogProcess will finish.  It is not reused on a
 * restart.
 */
class WatchdogChildProcess
{
  private static final L10N L = new L10N(WatchdogChildProcess.class);
  private static final Logger log
    = Logger.getLogger(WatchdogChildProcess.class.getName());
  
  private static HashSet<String> _resinProperties
    = new HashSet<String>();

  private static Boot _jniBoot;

  private final String _id;
  private final ResinSystem _system;
  private final WatchdogChild _watchdog;
  private final Lifecycle _lifecycle = new Lifecycle();

  private WatchdogChildActor _watchdogActor;
  private WatchdogChildTask _task;

  private Socket _childSocket;
  private AtomicReference<Process> _processRef
    = new AtomicReference<Process>();
  private OutputStream _stdOs;
  private int _pid;

  private long _startTime;
  private int _status = -1;
  private String _exitMessage;
  private ExitCode _exitCode;

  WatchdogChildProcess(String id,
                       ResinSystem system,
                       WatchdogChild watchdog,
                       WatchdogChildTask task)
  {
    _id = id;
    _system = system;
    _watchdog = watchdog;
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
  
  /**
   * General queries of the Resin instance.
   */
  Serializable queryGet(Serializable payload)
  {
    if (_watchdogActor != null)
      return _watchdogActor.query(payload);
    else
      return null;
  }
  
  /**
   * General message to the Resin instance.
   */
  void message(Serializable payload)
  {
    if (_watchdogActor != null) {
      _watchdogActor.message(payload);
    }
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

  public void run()
  {
    if (! _lifecycle.toActive())
      return;

    EnvironmentClassLoader envLoader
      = EnvironmentClassLoader.create(_system.getClassLoader());
    
    Thread thread = Thread.currentThread();
    
    WriteStream jvmOut = null;
    ServerSocket ss = null;
    Socket s = null;

    try {
      _startTime = CurrentTime.getCurrentTime();
      
      thread.setContextClassLoader(envLoader);
      envLoader.start();

      ss = new ServerSocket(0, 5, InetAddress.getByName("127.0.0.1"));

      int port = ss.getLocalPort();

      log.warning("Watchdog starting Resin[" + _watchdog.getId() + "]");

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

        ThreadPool.getCurrent().start(logThread);

        s = connectToChild(ss);
        
        message(new StartInfoMessage(_watchdog.isRestart(),
                                     _watchdog.getRestartMessage(),
                                     _watchdog.getPreviousExitCode(),
                                     _task.getShutdownMessage()));

        _status = process.waitFor();

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

      if (jvmOut != null && ! _watchdog.isConsole()) {
        try {
          synchronized (jvmOut) {
            jvmOut.close();
          }
        } catch (Exception e) {
        }
      }

      synchronized (this) {
        if (_status < 0)
          _status = 666;

        notifyAll();
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

    String msg = ("Watchdog detected close of "
                  + "Resin[" + _watchdog.getId() + ",pid=" + _pid + "]"
                  + "\n  exit reason: " + type + code);
    
    log.warning(msg);
    
    _exitMessage = msg;
  }

  /**
   * Stops the instance, waiting for the completion.
   */
  void stop()
  {
    _lifecycle.toDestroy();

    if (_watchdogActor != null)
      _watchdogActor.sendShutdown();
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

  void waitForExit()
  {
    synchronized (this) {
      if (_status < 0) {
        try {
          wait(60000);
        } catch (Exception e) {
        }
      }
    }
  }

  /**
   * Waits for a socket connection from the child, returning the socket
   *
   * @param ss TCP ServerSocket from the watchdog for the child to connect to
   */
  private Socket connectToChild(ServerSocket ss)
    throws IOException
  {
    Socket s = null;

    try {
      ss.setSoTimeout(60000);

      for (int i = 0; _lifecycle.isActive() && i < 120 && s == null; i++) {
        try {
          s = ss.accept();
        } catch (SocketTimeoutException e) {
        }
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

    return s;
  }

  /**
   * Starts the BAM actor to communicate with the child.
   *
   * @param s the socket to the child.
   */
  private void startWatchdogActor(Socket s)
    throws IOException
  {
    InputStream watchdogIs = s.getInputStream();
    OutputStream watchdogOs = s.getOutputStream();

    _watchdogActor = new WatchdogChildActor(this);

    HmtpLinkWorker link = new HmtpLinkWorker(_watchdogActor, watchdogIs, watchdogOs);

    try {
      ThreadPool.getCurrent().schedule(link);
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
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
    Path chroot = _watchdog.getChroot();
    Path processPwd = _watchdog.getPwd();

    HashMap<String,String> env = buildEnv();

    ArrayList<String> jvmArgs = buildJvmArgs();

    ArrayList<String> resinArgs = buildResinArgs(socketPort);

    addCommandLineArguments(jvmArgs, resinArgs);

    jvmArgs.add("com.caucho.server.resin.Resin");

    jvmArgs.addAll(resinArgs);

    if (_watchdog.isVerbose()) {
      logVerboseArguments(out, jvmArgs);

      logVerboseEnvironment(out, env);
    }

    Boot boot = getJniBoot();
    if (boot != null) {
      boot.clearSaveOnExec();

      ArrayList<QServerSocket> boundSockets = new ArrayList<QServerSocket>();

      try {
        if (_watchdog.getUserName() != null) {
          for (OpenPort port : _watchdog.getPorts()) {
            QServerSocket ss = port.bindForWatchdog();

            if (ss == null)
              continue;

            boundSockets.add(ss);

            if (ss.setSaveOnExec()) {
              jvmArgs.add("-port");
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
                                    _watchdog.getUserName(),
                                    _watchdog.getGroupName());

        if (process != null) {
          return process;
        }
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
      if (_watchdog.isConsole()) {
        throw new ConfigException(L.l("<user-name> requires compiled JNI started with 'start'.  Resin cannot use <user-name> when started as a console process."));
      }
      else {
        throw new ConfigException(L.l("<user-name> '{0}' requires compiled JNI and a valid operating-system user name.", 
                                      _watchdog.getUserName()));
      }
    }

    if (_watchdog.getGroupName() != null) {
      if (_watchdog.isConsole()) {
        throw new ConfigException(L.l("<group-name> requires compiled JNI started with 'start'.  Resin cannot use <group-name> when started as a console process."));
      }
      else {
        throw new ConfigException(L.l("<group-name> requires compiled JNI."));
      }
    }

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(processPwd.getNativePath()));

    builder.environment().putAll(env);

    builder = builder.command(jvmArgs);

    builder.redirectErrorStream(true);

    return builder.start();
  }

  private HashMap<String,String> buildEnv()
    throws IOException
  {
    HashMap<String,String> env = new HashMap<String,String>();

    env.putAll(System.getenv());
    
    Path resinHome = _watchdog.getResinHome();

    ArrayList<String> classPathList = new ArrayList<String>();
    classPathList.addAll(_watchdog.getJvmClasspath());
    String classPath
      = WatchdogArgs.calculateClassPath(classPathList, resinHome);

    env.put("CLASSPATH", classPath);

    if (_watchdog.is64bit()) {
      WatchdogClient.appendEnvPath(env,
                                   "LD_LIBRARY_PATH",
                                   resinHome.lookup("libexec64").getNativePath());
      WatchdogClient.appendEnvPath(env,
                                   "LD_LIBRARY_PATH_64",
                                   resinHome.lookup("libexec64").getNativePath());
      WatchdogClient.appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec64").getNativePath());
      if (CauchoSystem.isWindows())
        WatchdogClient.appendEnvPath(env,
                      "Path",
                      resinHome.lookup("win64").getNativePath());
    }
    else {
      WatchdogClient.appendEnvPath(env,
                    "LD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());
      WatchdogClient.appendEnvPath(env,
                    "DYLD_LIBRARY_PATH",
                    resinHome.lookup("libexec").getNativePath());

      if (CauchoSystem.isWindows())
        WatchdogClient.appendEnvPath(env,
                      "Path",
                      resinHome.lookup("win32").getNativePath());
    }

    return env;
  }

  private ArrayList<String> buildJvmArgs()
  {
    ArrayList<String> jvmArgs = new ArrayList<String>();

    jvmArgs.add(_watchdog.getJavaExe());
    
    boolean isEndorsed = false;

    // user args are first so they're displayed by ps
    for (String arg : _watchdog.getJvmArgs()) {
      if (! isResinProperty(arg)) {
        jvmArgs.add(arg);
      }
      
      if (arg.startsWith("-Djava.endorsed.dirs")) {
        isEndorsed = true;
      }
    }

    jvmArgs.add("-Dresin.server=" + _id);

    jvmArgs.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");

    // This is needed for JMX to work correctly.
    String systemClassLoader = _watchdog.getSystemClassLoader();
    if (systemClassLoader != null && ! "".equals(systemClassLoader)) {
      jvmArgs.add("-Djava.system.class.loader=" + systemClassLoader);
    }
    
    Path resinHome = _watchdog.getResinHome();
    Path resinRoot = _watchdog.getResinRoot();
    
    if (! isEndorsed) {
      String endorsed = System.getProperty("java.endorsed.dirs");
      
      String resinEndorsed = resinHome.getNativePath() + File.separator + "endorsed";
      
      resinEndorsed += (File.pathSeparator
                        + resinRoot .getNativePath() + File.separator + "endorsed");
      
      if (endorsed != null)
        endorsed = endorsed + File.pathSeparator + resinEndorsed;
      else
        endorsed = resinEndorsed;
      
      jvmArgs.add("-Djava.endorsed.dirs=" + endorsed);
    }
    
    // #2567
    jvmArgs.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    jvmArgs.add("-Djava.awt.headless=true");
    jvmArgs.add("-Djava.awt.headlesslib=true");

    jvmArgs.add("-Dresin.home=" + resinHome.getFullPath());
    
    /*
    if (! _watchdog.hasXss())
      jvmArgs.add("-Xss1m");

    if (! _watchdog.hasXmx())
      jvmArgs.add("-Xmx256m");
      */
    
    // #4308, #4585
    if (CauchoSystem.isWindows())
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
      jvmArgs.add("-D" + HealthSystemFacade.RESIN_EXIT_MESSAGE
                  + "=" + _task.getShutdownMessage());
    }

    String[] argv = _watchdog.getArgv();

    for (int i = 0; i < argv.length; i++) {
      String arg = argv[i];

      if (isResinProperty(arg)) {
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
        && _watchdog.is64bit() && ! CauchoSystem.isWindows()) {
      jvmArgs.add("-d64");
    }

    String jvmMode = _watchdog.getJvmMode();
    
    if ((jvmMode == null || "".equals(jvmMode))
      && ! CauchoSystem.isWindows() && ! "none".equals(jvmMode)) {
      jvmMode = "-server";
    }
    
    /*
    if (! jvmArgs.contains("-server")
        && ! jvmArgs.contains("-client")
        && ! CauchoSystem.isWindows() && ! "none".equals(jvmMode)
        && jvmMode != null 
        && ! "".equals(jvmMode)) {
      // #3331, windows can't add -server automatically
      jvmArgs.add(jvmMode);
    }
    */
    
    if (! CauchoSystem.isWindows() && ! "none".equals(jvmMode)
        && jvmMode != null 
        && ! "".equals(jvmMode)) {
      // #3331, windows can't add -server automatically
      jvmArgs.add(jvmMode);
    }
    
    return jvmArgs;
  }

  private ArrayList<String> buildResinArgs(int socketPort)
  {
    ArrayList<String> resinArgs = new ArrayList<String>();

    Path resinRoot = _watchdog.getResinRoot();

    if (resinRoot != null) {
      resinArgs.add("--root-directory");
      resinArgs.add(resinRoot.getFullPath());
    }
    
    if (_watchdog.getResinConf() != null) {
      resinArgs.add("-conf");
      resinArgs.add(_watchdog.getResinConf().getNativePath());
    }
    
    if (_watchdog.getId() != null) {
      resinArgs.add("-server");
      if ("".equals(_watchdog.getId()) || _watchdog.getId() == null)
        resinArgs.add("default");
      else
        resinArgs.add(_watchdog.getId());
    }

    // server/2k54
    if (_watchdog.isElasticServer()) {
      resinArgs.add("--elastic-server");
      
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
    
    resinArgs.add("-socketwait");
    resinArgs.add(String.valueOf(socketPort));

    return resinArgs;
  }

  private void addCommandLineArguments(ArrayList<String> jvmArgs,
                                       ArrayList<String> resinArgs)
  {
    String []argv = _watchdog.getArgv();
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-conf")) {
        // resin conf handled below
        i++;
      }
      else if (isResinProperty(argv[i])) {
        
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
      else if (CauchoSystem.isWindows() && "".equals(argv[i])) {
        resinArgs.add("\"\"");
      }
      else {
        resinArgs.add(argv[i]);
      }
    }
  }
  
  private boolean isResinProperty(String arg)
  {
    int p;
    if (arg.startsWith("-D") && (p = arg.indexOf('=')) >= 0) {
      String key = arg.substring(0, p);

      return _resinProperties.contains(key);
    }
    else if (arg.startsWith("-J-D") && (p = arg.indexOf('=')) >= 0) {
      String key = arg.substring(2, p);

      return _resinProperties.contains(key);
    }
    
    return false;
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
    if (_jniBoot != null)
      return _jniBoot.isValid() ? _jniBoot : null;

    try {
      ClassLoader loader = Thread.currentThread().getContextClassLoader();

      Class<?> cl = Class.forName("com.caucho.bootjni.JniBoot", false, loader);
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
    if (_watchdog.isConsole()) {
      return Vfs.openWrite(System.out);
    }

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
      log.log(Level.WARNING, e.toString(), e);
    }

    RotateLog log = new RotateLog();
    log.setPath(jvmPath);
    log.setRolloverSizeBytes(64L * 1024 * 1024);

    if (_watchdog.getStdoutLog() != null) {
      _watchdog.getStdoutLog().configure(log);
    }

    log.init();
    
    RotateStream rotateStream = log.getRotateStream();

    // server/6e82
    if (_watchdog.getStdoutLog() == null) {
      String logTail = jvmPath.getTail();
      int p = logTail.lastIndexOf('.');
      String logName = logTail.substring(0, p);
      
      System.out.println("LN: " + logName);
      _watchdog.getConfig().logInit(logName, rotateStream.getRolloverLog());
      rotateStream.getRolloverLog().init();
      rotateStream.init();
      System.out.println("LN0: " + logName);
    }
    
    return rotateStream.getStream();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _watchdog + "," + _id + "]";
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
          if (! _watchdog.isConsole()) {
            synchronized (out) {
              out.close();
            }
          }
        } catch (IOException e) {
        }

        kill();
      }
    }
  }
  
  static {
    _resinProperties.add("-Djava.awt.headless");
    _resinProperties.add("-Djava.awt.headlesslib");
    _resinProperties.add("-Djava.class.path");
    _resinProperties.add("-Dresin.home");
    _resinProperties.add("-Dresin.root");
    _resinProperties.add("-Dresin.watchdog");
    _resinProperties.add("-Djavax.management.builder.initial");
    _resinProperties.add("-Djava.util.logging.manager");
  }
}
