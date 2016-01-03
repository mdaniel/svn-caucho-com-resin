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

import io.baratine.service.ServiceExceptionConnect;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.jmx.MBeanServerBuilderImpl;
import com.caucho.v5.jni.Boot;
import com.caucho.v5.jni.JniBoot;
import com.caucho.v5.lifecycle.Lifecycle;
import com.caucho.v5.log.LogManagerImpl;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.CauchoUtil;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;
import com.caucho.v5.vfs.PathImpl;

/**
 * Client to a watchdog-manager, i.e. ResinBoot code to ask the
 * watchdog-manager to do something.
 */
public class WatchdogLauncher
{
  private static final L10N L = new L10N(WatchdogLauncher.class);
  private static final Logger log
    = Logger.getLogger(WatchdogLauncher.class.getName());

  // private static final long BAM_TIMEOUT = 3 * 60 * 1000; //3 minutes

  public static final String WATCHDOG_ADDRESS = "/watchdog";

  private String _id;

  private final ArgsDaemon _args;
  private final ServerConfigBoot _serverConfig;
  private WatchdogService _watchdog;
  
  private Lifecycle _lifecycle = new Lifecycle();

  private Boot _jniBoot;
  
  public WatchdogLauncher(ArgsDaemon args,
                          ServerConfigBoot server,
                          WatchdogService watchdog)
  {
    _args = args;
    _serverConfig= server;
    _id = server.getId();
    _watchdog = watchdog;
  }

  public ServerConfigBoot getConfig()
  {
    return _serverConfig;
  }
  
  public ArgsDaemon getArgs()
  {
    return _args;
  }

  public String getId()
  {
    return _id;
  }

  public boolean isActive()
  {
    return _lifecycle.isActive();
  }

  public boolean start(ServerConfigBoot server,
                       ArgsDaemon args, 
                       WatchdogService service)
  {
    try {
      service.start(server.getPort(), args.getArgv());
      
      return true;
    } catch (ServiceExceptionConnect e) {
      log.fine(e.toString());
      
      log.log(Level.FINEST, e.toString(), e);
      
      return false;
    } catch (Exception e) {
      System.out.println("  start " + server.getDisplayName() + " failed " + e);
      log.fine(e.toString());
      
      log.log(Level.FINER, e.toString(), e);
      
      return true;
    }
  }

  public void restartWatchdog(String []argv)
    throws IOException
  {
    try {
      String id = getId();
      
      int port = 0;
      
      String status = _watchdog.restart(0, argv);
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  private Class<?> getMainClass()
  {
    String className = null;
    
    try {
      String programName = getArgs().getProgramName();
      
      programName = (Character.toUpperCase(programName.charAt(0))
                     + programName.substring(1));
      
      String watchdogMain = WatchdogMain.class.getName();
      
      int p = watchdogMain.lastIndexOf('.');
      
      String pkg = watchdogMain.substring(0, p + 1);
      
      className = pkg + "Watchdog" + programName;
      
      return Class.forName(className);
    } catch (Exception e) {
      throw new IllegalStateException(L.l("Can't start watchdog {0}", className), e);
    }
  }
  
  public Process launchManager(String []argv)
    throws IOException
  {
    System.out.println(L.l("{0}/{1} launching watchdog at {2}:{3}",
                           getArgs().getDisplayName(),
                           Version.getVersion(),
                           _serverConfig.getWatchdogAddress(_args),
                           _serverConfig.getWatchdogPort(_args)));
    
    Class<?> mainClass = getMainClass();
    
    log.fine(this + " starting " + mainClass.getSimpleName());

    PathImpl homeDir = _args.getHomeDirectory();
    PathImpl rootDir = _serverConfig.getRootDirectory(_args);
    
    ProcessBuilder builder = new ProcessBuilder();

    builder.directory(new File(rootDir.getNativePath()));

    Map<String,String> env = builder.environment();

    env.putAll(System.getenv());

    String classPath = ArgsWatchdog.calculateClassPath(homeDir, _args.getProgramName());
    
    env.put("CLASSPATH", classPath);

    ArrayList<String> list = new ArrayList<String>();

    //list.add(_server.getJavaExe());
    list.add("java");

    /**
     * list.add("-Xdebug");
     * list.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=4948");
    */
    // #3759 - user args are first so they're displayed by ps

    //list.addAll(_server.getWatchdogJvmArgs());
    // list.add("-Dwatchdog=" + _id);
    list.add("-Djava.util.logging.manager=" + LogManagerImpl.class.getName());
    list.add("-Djavax.management.builder.initial=" + MBeanServerBuilderImpl.class.getName());
    list.add("-Djava.awt.headless=true");
    //list.add("-Dresin.home=" + homeDir.getFullPath());
    //list.add("-Dresin.root=" + rootDir.getFullPath());
    
    for (int i = 0; i < argv.length; i++) {
      if (argv[i].startsWith("-Djava.class.path=")) {
        // IBM JDK startup issues
      }
      // #5053, server/6e0f
      else if (argv[i].startsWith("-J")
               && ! argv[i].startsWith("-J-X")) {
        list.add(argv[i].substring(2));
      }
    }

    // #2566
    list.add("-Xrs");

    /*
    if (! _server.hasWatchdogXss())
      list.add("-Xss256k");
    if (! _server.hasWatchdogXmx())
      list.add("-Xmx32m");
      */

    // XXX: can this just be copied from original args?
    if (! list.contains("-d32") && ! list.contains("-d64")
        && _args.is64Bit() && ! CauchoUtil.isWindows()) {
      list.add("-d64");
    }

    /*
    if (! list.contains("-server")
        && ! list.contains("-client")
        && ! CauchoUtil.isWindows()) {
      // #3331, windows can't add -server automatically
      list.add("-server");
    }
    */
    
    // WatchdogArgs args = _bootManager.getArgs();

    list.add(mainClass.getName());

    for (int i = 0; i < argv.length; i++) {
      if (argv[i].equals("-conf")
          || argv[i].equals("--conf")) {
        list.add(argv[i]);
        list.add(homeDir.lookup(argv[i + 1]).getNativePath());
        i++;
      }
      else if ("".equals(argv[i]) && CauchoUtil.isWindows())
        list.add("\"\"");
      else
        list.add(argv[i]);
    }
    
    builder = builder.command(list);

    // builder.redirectErrorStream(true);

    Process process = null;

    try {
      process = builder.start();
    } catch(Exception e) {
      e.printStackTrace();
    }
    
    _lifecycle.toActive();

    InputStream stdIs = process.getInputStream();
    InputStream stdErr = process.getErrorStream();
    OutputStream stdOs = process.getOutputStream();

    ProcessThreadReader reader = new ProcessThreadReader(stdIs, _lifecycle);
    reader.setDaemon(true);
    reader.start();

    ProcessThreadReader errorReader = new ProcessThreadReader(stdErr, _lifecycle);
    errorReader.setDaemon(true);
    errorReader.start();

    /*
    try {
      Thread.sleep(1000);
    } catch (Exception e) {
      
    }
    */

    // stdIs.close();
    stdOs.close();

    return process;
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

      _jniBoot = new JniBoot();
    } catch (IllegalStateException e) {
      log.fine(e.toString());
      
      log.log(Level.FINEST, e.toString(), e);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return _jniBoot != null && _jniBoot.isValid() ? _jniBoot : null;
  }

  static class ProcessThreadReader extends Thread {
    private Lifecycle _lifecycle;
    private InputStream _is;

    ProcessThreadReader(InputStream is, Lifecycle lifecycle)
    {
      _is = is;
      _lifecycle = lifecycle;
    }

    public void run()
    {
      try {
        int ch;

        while ((ch = _is.read()) >= 0) {
          System.out.print((char) ch);
        }
      } catch (Exception e) {
      } finally {
        _lifecycle.toDestroy();
      }
    }
  }
}
