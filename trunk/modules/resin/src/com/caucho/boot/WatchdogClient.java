/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.bam.RemoteConnectionFailedException;
import com.caucho.bam.actor.ActorSender;
import com.caucho.config.ConfigException;
import com.caucho.env.service.ResinSystem;
import com.caucho.hmtp.HmtpClient;
import com.caucho.server.util.CauchoSystem;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;

/**
 * Client to a watchdog-manager, i.e. ResinBoot code to ask the
 * watchdog-manager to do something.
 */
class WatchdogClient
{
  private static final L10N L = new L10N(WatchdogClient.class);
  private static final Logger log
    = Logger.getLogger(WatchdogClient.class.getName());

  private static final long BAM_TIMEOUT = 3 * 60 * 1000; //3 minutes

  public static final String WATCHDOG_ADDRESS = "watchdog@admin.resin.caucho";

  private final BootResinConfig _bootManager;
  private String _id = "";

  private ResinSystem _system;
  private WatchdogConfig _config;
  private WatchdogChild _watchdog;

  private ActorSender _conn;

  private Boot _jniBoot;
  private ResinGUI _ui;

  WatchdogClient(ResinSystem system,
                 BootResinConfig bootManager,
                 WatchdogConfig config)
  {
    _system = system;
    _bootManager = bootManager;
    _config = config;
    _id = config.getId();
  }

  public WatchdogConfig getConfig()
  {
    return _config;
  }

  public String getId()
  {
    return _id;
  }
  
  public String getClusterId()
  {
    return _config.getCluster().getId();
  }
  
  public int getIndex()
  {
    return _config.getIndex();
  }

  public String getWatchdogAddress()
  {
    return _config.getWatchdogAddress();
  }

  public int getWatchdogPort()
  {
    return _config.getWatchdogPort();
  }

  String[] getArgv()
  {
    return _config.getArgv();
  }

  Path getPwd()
  {
    return _config.getPwd();
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
    return _config.hasXmx();
  }

  boolean hasXss()
  {
    return _config.hasXss();
  }

  boolean is64bit()
  {
    return _config.is64bit();
  }

  boolean isVerbose()
  {
    return _config.isVerbose();
  }

  public String getGroupName()
  {
    return _config.getGroupName();
  }

  public String getUserName()
  {
    return _config.getUserName();
  }

  public Path getLogDirectory()
  {
    return _config.getLogDirectory();
  }

  public Path getResinDataDirectory()
  {
    return _bootManager.getResinDataDirectory();
  }

  public String getResinSystemAuthKey()
  {
    return _bootManager.getResinSystemAuthKey();
  }

  public long getShutdownWaitTime()
  {
    return _config.getShutdownWaitTime();
  }

  public int startConsole()
    throws IOException
  {
    if (_watchdog == null)
      _watchdog = new WatchdogChild(_system, _config);

    return _watchdog.startConsole();
  }

  public void stopConsole()
  {
    _watchdog.stop();
  }

  public int startGui(GuiCommand command) throws IOException
  {
    if (_ui != null && _ui.isVisible())
      return 1;
    else if (_ui != null)
      return 0;

    _ui = new ResinGUI(command, this);
    _ui.setVisible(true);

    return 1;
  }

  //
  // watchdog commands
  //

  public String statusWatchdog()
    throws IOException
  {
    ActorSender conn = getConnection();

    try {
      ResultStatus status = (ResultStatus)
        conn.query(WATCHDOG_ADDRESS, new WatchdogStatusQuery());

      if (status.isSuccess())
        return status.getMessage();

      throw new RuntimeException(L.l("{0}: watchdog status failed because of '{1}'",
                                     this, status.getMessage()));
    } catch (Exception e) {
      Throwable e1 = e;

      while (e1.getCause() != null)
        e1 = e1.getCause();

      log.log(Level.FINE, e.toString(), e);

      return e1.toString();
    }
  }

  public Process startWatchdog(String []argv)
    throws ConfigException, IOException
  {
    if (getUserName() != null && ! hasBoot()) {
      String message = getTroubleshootMessage();

      if (message == null)
        message = "Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin_os.so.";
        
      throw new ConfigException(L.l("<user-name> requires compiled JNI.\n{0}", message));
    }

    if (getGroupName() != null && ! hasBoot()) {
      String message = getTroubleshootMessage();

      if (message == null)
        message = "Check the $RESIN_HOME/libexec or $RESIN_HOME/libexec64 directory for libresin_os.so.";
        
      throw new ConfigException(L.l("<group-name> requires compiled JNI.\n{0}", message));
    }

    ActorSender conn = null;

    try {
      conn = getConnection();
      
      String serverId = getId();

      ResultStatus status = (ResultStatus)
        conn.query(WATCHDOG_ADDRESS,
                   new WatchdogStartQuery(serverId, argv),
                   BAM_TIMEOUT);

      if (status.isSuccess())
        return null;

      throw new ConfigException(L.l("{0}: watchdog start failed because of '{1}'",
                                    this, status.getMessage()));
    } catch (RemoteConnectionFailedException e) {
      log.log(Level.FINE, e.toString(), e);
    } catch (RuntimeException e) {
      throw e;
    } finally {
      if (conn != null)
        conn.close();
    }

    Process process = launchManager(argv);
    
    long timeout = 15 * 1000L;
    long expireTime = Alarm.getCurrentTimeActual() + timeout;
    
    while (Alarm.getCurrentTimeActual() <= expireTime) {
      if (pingWatchdog())
        return process;
    }
    
    return null;
  }
  
  private boolean pingWatchdog()
  {
    ActorSender conn = null;

    try {
      conn = getConnection();

      
      return true;
    } catch (Exception e) {
      log.log(Level.FINER, e.toString(), e);
    } finally {
      if (conn != null)
        conn.close();
    }
    
    return false;
  }

  public void stopWatchdog(String serverId)
  {
    ActorSender conn = getConnection();

    try {
      ResultStatus status = (ResultStatus)
        conn.query(WATCHDOG_ADDRESS, 
                   new WatchdogStopQuery(serverId),
                   BAM_TIMEOUT);

      if (! status.isSuccess())
        throw new RuntimeException(L.l("{0}: watchdog '{1}' stop failed because of '{2}'",
                                       this, serverId, status.getMessage()));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void killWatchdog(String serverId)
    throws IOException
  {
    ActorSender conn = getConnection();

    try {
      ResultStatus status = (ResultStatus)
        conn.query(WATCHDOG_ADDRESS, new WatchdogKillQuery(serverId), BAM_TIMEOUT);

      if (! status.isSuccess())
        throw new RuntimeException(L.l("{0}: watchdog kill failed because of '{1}'",
                                       this, status.getMessage()));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public void restartWatchdog(String id, String []argv)
    throws IOException
  {
    // cloud/1295
    ActorSender conn = getConnection();

    try {
      ResultStatus status = (ResultStatus)
      conn.query(WATCHDOG_ADDRESS, 
                 new WatchdogRestartQuery(id, argv),
                 BAM_TIMEOUT);

      if (! status.isSuccess())
        throw new RuntimeException(L.l("{0}: watchdog restartfailed because of '{1}'",
                                       this, status.getMessage()));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  public boolean shutdown()
    throws IOException
  {
    ActorSender conn = getConnection();

    try {
      ResultStatus status = (ResultStatus)
        conn.query(WATCHDOG_ADDRESS, new WatchdogShutdownQuery(), BAM_TIMEOUT);

      if (! status.isSuccess())
        throw new RuntimeException(L.l("{0}: watchdog shutdown failed because of '{1}'",
                                       this, status.getMessage()));
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return true;
  }

  private ActorSender getConnection()
  {
    if (_conn == null) {
      String url = ("http://" + getWatchdogAddress()
                    + ":" + getWatchdogPort()
                    + "/hmtp");
      
      HmtpClient client = new HmtpClient(url);

      client.setVirtualHost("admin.resin");
      
      String uid = "";
      
      client.setEncryptPassword(true);

      client.connect(uid, getResinSystemAuthKey());

      _conn = client;
    }

    return _conn;
  }

  public Process launchManager(String []argv)
    throws IOException
  {
    System.out.println(L.l("Resin/{0} launching watchdog at {1}:{2}",
                           VersionFactory.getVersion(),
                           getWatchdogAddress(),
                           getWatchdogPort()));

    log.fine(this + " starting ResinWatchdogManager");

    Path resinHome = getResinHome();
    Path resinRoot = getRootDirectory();

    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(resinRoot.getNativePath()));

    Map<String,String> env = builder.environment();

    env.putAll(System.getenv());

    String classPath = WatchdogArgs.calculateClassPath(resinHome);

    env.put("CLASSPATH", classPath);

    String libexecPath;

    if (is64bit()) {
      libexecPath = resinHome.lookup("libexec64").getNativePath();

      appendEnvPath(env, "LD_LIBRARY_PATH", libexecPath);
      appendEnvPath(env, "LD_LIBRARY_PATH_64", libexecPath);
      appendEnvPath(env, "DYLD_LIBRARY_PATH", libexecPath);
      if (CauchoSystem.isWindows())
        appendEnvPath(env, "Path", resinHome.lookup("win64").getNativePath());
    }
    else {
      libexecPath = resinHome.lookup("libexec").getNativePath();

      appendEnvPath(env, "LD_LIBRARY_PATH", libexecPath);
      appendEnvPath(env, "DYLD_LIBRARY_PATH", libexecPath);
      if (CauchoSystem.isWindows())
        appendEnvPath(env, "Path", resinHome.lookup("win32").getNativePath());
    }

    ArrayList<String> list = new ArrayList<String>();

    list.add(_config.getJavaExe());

    // #3759 - user args are first so they're displayed by ps
    list.addAll(_config.getWatchdogJvmArgs());

    list.add("-Dresin.watchdog=" + _id);
    list.add("-Djava.util.logging.manager=com.caucho.log.LogManagerImpl");
    list.add("-Djavax.management.builder.initial=com.caucho.jmx.MBeanServerBuilderImpl");
    list.add("-Djava.awt.headless=true");
    list.add("-Dresin.home=" + resinHome.getFullPath());
    list.add("-Dresin.root=" + resinRoot.getFullPath());

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].startsWith("-Djava.class.path=")) {
        // IBM JDK startup issues
      }
      else if (argv[i].startsWith("-J")) {
        list.add(argv[i].substring(2));
      }
    }

    // #2566
    list.add("-Xrs");

    if (! _config.hasWatchdogXss())
      list.add("-Xss256k");
    if (! _config.hasWatchdogXmx())
      list.add("-Xmx32m");

    // XXX: can this just be copied from original args?
    if (! list.contains("-d32") && ! list.contains("-d64")
        && is64bit() && ! CauchoSystem.isWindows()) {
      list.add("-d64");
    }

    if (! list.contains("-server")
        && ! list.contains("-client")
        && ! CauchoSystem.isWindows()) {
      // #3331, windows can't add -server automatically
      list.add("-server");
    }
    
    WatchdogArgs args = _bootManager.getArgs();

    list.add("com.caucho.boot.WatchdogManager");

    if ("".equals(args.getServerId())
        && ! args.isDynamicServer()
        && ! "".equals(getId())) {
      list.add("-server");
      list.add(getId());
    }

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

    list.add("--log-directory");
    list.add(getLogDirectory().getFullPath());

    builder = builder.command(list);

    // builder.redirectErrorStream(true);

    Process process = null;

    try {
      process = builder.start();
    } catch(Exception e) {
      e.printStackTrace();
    }

    InputStream stdIs = process.getInputStream();
    InputStream stdErr = process.getErrorStream();
    OutputStream stdOs = process.getOutputStream();

    ProcessThreadReader reader = new ProcessThreadReader(stdIs);
    reader.setDaemon(true);
    reader.start();

    ProcessThreadReader errorReader = new ProcessThreadReader(stdErr);
    errorReader.setDaemon(true);
    errorReader.start();

    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      
    }

    // stdIs.close();
    stdOs.close();

    return process;
  }

  public static void appendEnvPath(Map<String,String> env,
                             String prop,
                             String value)
  {
    String oldValue = env.get(prop);

    if (oldValue == null && CauchoSystem.isWindows()) {
      String winProp = prop.toUpperCase(Locale.ENGLISH);
      oldValue = env.get(winProp);

      if (oldValue != null)
        prop = winProp;
    }

    if (oldValue != null && ! "".equals(oldValue))
      value = value + File.pathSeparator + oldValue;

    env.put(prop, value);
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

  private boolean hasBoot()
  {
    try {
      Boot boot = getJniBoot();

      return boot != null && boot.isValid();
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return false;
  }
  
  private String getTroubleshootMessage()
  {
    Boot boot = getJniBoot();
    
    if (_jniBoot != null)
      boot = _jniBoot;
    
    return boot != null ? boot.getValidationMessage() : null;
  }

  static class ProcessThreadReader extends Thread {
    private InputStream _is;

    ProcessThreadReader(InputStream is)
    {
      _is = is;
    }

    public void run()
    {
      try {
        int ch;

        while ((ch = _is.read()) >= 0) {
          System.out.print((char) ch);
        }
      } catch (Exception e) {

      }
    }
  }
}
