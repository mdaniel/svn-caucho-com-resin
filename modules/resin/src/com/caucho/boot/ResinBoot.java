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

import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.boot.WatchdogArgs.StartMode;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.loader.Environment;
import com.caucho.loader.LibraryLoader;
import com.caucho.server.resin.ResinELContext;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

/**
 * ResinBoot is the main bootstrap class for Resin.  It parses the
 * resin.xml and looks for the &lt;server> block matching the -server
 * argument.
 *
 * <h3>Start Modes:</h3>
 *
 * The start modes are STATUS, DIRECT, START, STOP, KILL, RESTART, SHUTDOWN.
 *
 * <ul>
 * <li>DIRECT starts a <server> from the command line
 * <li>START starts a <server> with a Watchdog in the background
 * <li>STOP stop the <server> Resin in the background
 * </ul>
 */
public class ResinBoot {
  private static L10N _L;
  private static Logger _log;
  
  private static HashMap<StartMode,BootCommand> _commandMap
    = new HashMap<StartMode,BootCommand>();

  private WatchdogArgs _args;

  private WatchdogClient _client;
  private ResinGUI _ui;

  ResinBoot(String []argv)
    throws Exception
  {
    _args = new WatchdogArgs(argv);

    Path resinHome = _args.getResinHome();

    ClassLoader loader = ProLoader.create(resinHome, _args.is64Bit());

    if (loader != null) {
      System.setProperty("resin.home", resinHome.getNativePath());

      Thread.currentThread().setContextClassLoader(loader);

      Environment.init();

      Vfs.initJNI();

      resinHome = Vfs.lookup(resinHome.getFullPath());

      _args.setResinHome(resinHome);
    }
    else {
      Environment.init();
    }
    
    String jvmVersion = System.getProperty("java.runtime.version");
    
    if ("1.6".compareTo(jvmVersion) > 0) {
      throw new ConfigException(L().l("Resin requires Java 1.6 or later but was started with {0}",
                                      jvmVersion));
    }

    // required for license check
    System.setProperty("resin.home", resinHome.getNativePath());

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);

    if (! _args.getResinConf().canRead()) {
      throw new ConfigException(L().l("Resin/{0} can't open configuration file '{1}'",
                                      VersionFactory.getVersion(),
                                      _args.getResinConf().getNativePath()));
    }
    
    Path rootDirectory = _args.getRootDirectory();
    Path dataDirectory = rootDirectory.lookup("watchdog-data");

    ResinSystem system = new ResinSystem("watchdog",
                                         rootDirectory,
                                         dataDirectory);

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(system.getClassLoader());
    
    LibraryLoader libLoader = new LibraryLoader();
    libLoader.setPath(rootDirectory.lookup("lib"));
    libLoader.init();

    Config config = new Config();
    BootResinConfig bootManager = new BootResinConfig(system, _args);

    ResinELContext elContext = _args.getELContext();

    /**
     * XXX: the following setVar calls should not be necessary, but the
     * EL.setEnviornment() call above is not effective:
     */
    InjectManager beanManager = InjectManager.create();

    Config.setProperty("resinHome", elContext.getResinHome());
    Config.setProperty("java", elContext.getJavaVar());
    Config.setProperty("resin", elContext.getResinVar());
    Config.setProperty("server", elContext.getServerVar());
    Config.setProperty("system", System.getProperties());
    Config.setProperty("getenv", System.getenv());

    ResinConfigLibrary.configure(beanManager);

    config.configure(bootManager, _args.getResinConf(),
                     "com/caucho/server/resin/resin.rnc");

    if (_args.isDynamicServer()) {
      _client = bootManager.addDynamicClient(_args);
    }
    else {
      _client = bootManager.findClient(_args.getServerId());
    }
    
    if (_client == null && _args.isShutdown()) {
      _client = bootManager.findShutdownClient();
    }
    
    if (_client == null && ! (_args.isStart() || _args.isConsole())) {
      _client = bootManager.findShutdownClient();
    }

    if (_client == null) {
      throw new ConfigException(L().l("Resin/{0}: -server '{1}' does not match any defined <server>\nin {2}.",
                                      VersionFactory.getVersion(), _args.getServerId(), _args.getResinConf()));
    }
  }

  BootCommand getCommand() {
    return _commandMap.get(_args.getStartMode());
  }

  boolean start()
    throws Exception
  {
    BootCommand command = _commandMap.get(_args.getStartMode());

    if (command != null && _args.isHelp()) {
      command.usage();

      return false;
    }
    else if (command != null && command.isRetry()) {
      int code = command.doCommand(_args, _client);

      return code != 0;
    }
    else if (command != null) {
      int code = command.doCommand(_args, _client);

      System.exit(code);
    }

    throw new IllegalStateException(L().l("Unknown start mode"));
  }

  /**
   * The main start of the web server.
   *
   * <pre>
   * -conf resin.xml  : alternate configuration file
   * -server web-a    : &lt;server> to start
   * <pre>
   */
  public static void main(String []argv)
  {
    if (System.getProperty("log.level") != null) {
      Logger.getLogger("").setLevel(Level.FINER);
    }
    else {
      for (Handler handler : Logger.getLogger("").getHandlers()) {
        if (handler instanceof ConsoleHandler) {
          handler.setLevel(Level.FINER);
          Logger.getLogger("").removeHandler(handler);
        }
      }
    }

    ResinBoot boot = null;
    BootCommand command = null;
    try {
      boot = new ResinBoot(argv);

      command = boot.getCommand();

      while (boot.start()) {
        try {
          synchronized (command) {
            command.wait(5000);
          }
        } catch (Exception e) {
        }
      }
      
      System.exit(ExitCode.OK.ordinal());
    } catch (BootArgumentException e) {
      System.out.println(e.getMessage());

      if (command != null)
        command.usage();

      System.exit(ExitCode.UNKNOWN_ARGUMENT.ordinal());
    } catch (ConfigException e) {
      System.out.println(e.getMessage());

      System.exit(ExitCode.BAD_CONFIG.ordinal());
    } catch (Exception e) {
      e.printStackTrace();

      System.exit(ExitCode.UNKNOWN.ordinal());
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(ResinBoot.class.getName());

    return _log;
  }
  
  static {
    _commandMap.put(StartMode.CONSOLE, new ConsoleCommand());
    _commandMap.put(StartMode.DEPLOY_COPY, new DeployCopyCommand());
    _commandMap.put(StartMode.DEPLOY, new DeployCommand());
    _commandMap.put(StartMode.DEPLOY_CONFIG, new DeployConfigCommand());
    _commandMap.put(StartMode.DEPLOY_LIST, new DeployListCommand());
    _commandMap.put(StartMode.DEPLOY_RESTART, new DeployRestartCommand());
    _commandMap.put(StartMode.DEPLOY_START, new DeployStartCommand());
    _commandMap.put(StartMode.DEPLOY_STOP, new DeployStopCommand());
    _commandMap.put(StartMode.DISABLE, new DisableCommand());
    _commandMap.put(StartMode.DISABLE_SOFT, new DisableSoftCommand());
    _commandMap.put(StartMode.THREAD_DUMP, new DeployStartCommand());
    _commandMap.put(StartMode.ENABLE, new EnableCommand());
    _commandMap.put(StartMode.GUI, new GuiCommand());
    _commandMap.put(StartMode.HEAP_DUMP, new HeapDumpCommand());
    _commandMap.put(StartMode.JMX_CALL, new JmxCallCommand());
    _commandMap.put(StartMode.JMX_DUMP, new JmxDumpCommand());
    _commandMap.put(StartMode.JMX_LIST, new JmxListCommand());
    _commandMap.put(StartMode.JMX_SET, new JmxSetCommand());
    _commandMap.put(StartMode.JSPC, new JspcCommand());
    _commandMap.put(StartMode.KILL, new KillCommand());
    _commandMap.put(StartMode.LIST_RESTARTS, new ListRestartsCommand());
    _commandMap.put(StartMode.LOG_LEVEL, new LogLevelCommand());
    _commandMap.put(StartMode.PDF_REPORT, new PdfReportCommand());
    _commandMap.put(StartMode.PROFILE, new ProfileCommand());
    _commandMap.put(StartMode.RESTART, new RestartCommand());
    _commandMap.put(StartMode.SHUTDOWN, new ShutdownCommand());
    _commandMap.put(StartMode.START, new StartCommand());
    _commandMap.put(StartMode.START_WITH_FOREGROUND, new StartWithForegroundCommand());
    _commandMap.put(StartMode.STATUS, new StatusCommand());
    _commandMap.put(StartMode.STOP, new StopCommand());
    _commandMap.put(StartMode.THREAD_DUMP, new ThreadDumpCommand());

    _commandMap.put(StartMode.UNDEPLOY, new UnDeployCommand());

    _commandMap.put(StartMode.USER_ADD, new AddUserCommand());
    _commandMap.put(StartMode.USER_LIST, new ListUsersCommand());
    _commandMap.put(StartMode.USER_REMOVE, new RemoveUserCommand());

    _commandMap.put(StartMode.WATCHDOG, new WatchdogCommand());
  }
}
