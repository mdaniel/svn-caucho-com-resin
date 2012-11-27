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

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.VersionFactory;
import com.caucho.boot.BootResinConfig.ElasticServer;
import com.caucho.config.Config;
import com.caucho.config.ConfigException;
import com.caucho.config.core.ResinProperties;
import com.caucho.config.inject.InjectManager;
import com.caucho.config.lib.ResinConfigLibrary;
import com.caucho.env.service.ResinSystem;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.loader.Environment;
import com.caucho.loader.LibraryLoader;
import com.caucho.server.resin.ResinELContext;
import com.caucho.server.webbeans.ResinServerConfigLibrary;
import com.caucho.util.L10N;
import com.caucho.vfs.NullPath;
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
public class ResinBoot
{
  private static final Logger log = Logger.getLogger(ResinBoot.class.getName());
  private static L10N _L;

  private WatchdogArgs _args;
  private BootResinConfig _resinConfig;

  ResinBoot(String []argv)
    throws Exception
  {
    _args = new WatchdogArgs(argv);
    
    if (_args.isVerbose()) {
      setLoggingLevel(Level.FINER);
    }

    Path resinHome = _args.getResinHome();

    // required for license check
    System.setProperty("resin.home", resinHome.getNativePath());

    ClassLoader loader = ProLoader.create(resinHome, _args.is64Bit());

    if (loader != null) {
      Thread.currentThread().setContextClassLoader(loader);

      Environment.init();

      Vfs.initJNI();

      resinHome = Vfs.lookup(resinHome.getFullPath());

      _args.setResinHome(resinHome);
    }
    else {
      Environment.init();
    }

    // watchdog/0210
    // Vfs.setPwd(_rootDirectory);

    if (! _args.getResinConf().canRead()) {
      throw new ConfigException(L().l("Resin/{0} can't open configuration file '{1}'",
                                      VersionFactory.getVersion(),
                                      _args.getResinConf().getNativePath()));
    }
    
    Path rootDirectory = _args.getRootDirectory();
    Path dataDirectory = rootDirectory.lookup("watchdog-data");
    
    // required for license check
    System.setProperty("resin.root", rootDirectory.getNativePath());

    ResinSystem system;
    
    if (_args.getCommand().isConsole()) {
      system = new ResinSystem("watchdog",
                               rootDirectory,
                               dataDirectory);
    }
    else {
      system = new ResinSystem("watchdog", 
                               rootDirectory,
                               new NullPath("boot-temp"));
    }

    Thread thread = Thread.currentThread();
    thread.setContextClassLoader(system.getClassLoader());
    
    LibraryLoader libLoader = new LibraryLoader();
    libLoader.setPath(rootDirectory.lookup("lib"));
    libLoader.init();

    Config config = new Config();
    _resinConfig = new BootResinConfig(system, _args);

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
    // server/4342
    Config.setProperty("server_id", _args.getServerId());
    Config.setProperty("rvar0", _args.getServerId());
    
    ResinConfigLibrary.configure(beanManager);
    ResinServerConfigLibrary.configure(beanManager);
    
    // read $HOME/.resin
    if (_args.getUserProperties() != null && _args.getUserProperties().canRead()) {
      ResinProperties properties = new ResinProperties();
      properties.setPath(_args.getUserProperties());
      
      String mode = _args.getMode();
      if (mode == null && _args.getCommand().isRemote(_args)) {
        mode = "remote";
      }
      properties.setMode(_args.getMode());
      
      properties.init();
    }

    config.configure(_resinConfig, _args.getResinConf(),
                     "com/caucho/server/resin/resin.rnc");
    
    if (! _args.isHelp())
      initClient();
  }
  
  public String getServerId()
  {
    String serverId = _args.getServerId();
    
    if (serverId == null) {
      serverId = _resinConfig.getHomeServer();
    }

    
    return serverId;
  }
  
  private WatchdogArgs getArgs()
  {
    return _args;
  }
  
  private void initClient()
  {
    if (! _resinConfig.isElasticServer(_args)) {
      return;
    }
    
    WatchdogClient client = _resinConfig.addElasticClient(_args);

    if (client != null) {
      _args.setElasticServerId(client.getId());
    }
  }
  
  boolean isElasticServer(WatchdogArgs args)
  {
    return _resinConfig.isElasticServer(args);
  }
  
  ArrayList<ElasticServer> getElasticServerList()
  {
    return _resinConfig.getElasticServerList();
  }
  
  public boolean isElasticIp(WatchdogArgs args)
  {
    return _resinConfig.isElasticDns(args);
  }
  
  String getHomeCluster(WatchdogArgs args)
  {
    String clusterId = args.getClusterId();
    
    if (clusterId != null)
      return clusterId;
    else
      return _resinConfig.getHomeCluster();
  }

  BootClusterConfig findCluster(String clusterId)
  {
    return _resinConfig.findCluster(clusterId);
  }

  WatchdogClient findClient(String serverId, WatchdogArgs args)
  {
    return _resinConfig.findClient(serverId, args);
  }

  WatchdogClient findWatchdogClient(WatchdogArgs args)
  {
    return _resinConfig.findWatchdogClient(args.getClusterId());
  }
 
  ArrayList<WatchdogClient> findLocalClients()
  {
    return _resinConfig.findLocalClients(getServerId());
  }

  ArrayList<String> findLocalClientIds()
  {
    return _resinConfig.findLocalClientIds(getServerId());
  }

  BootCommand getCommand()
  {
    return _args.getCommand();
  }

  boolean start()
    throws Exception
  {
    BootCommand command = getCommand();

    if (command != null && _args.isHelp()) {
      command.usage(_args.isVerbose());

      return false;
    }
    else if (command != null && command.isRetry()) {
      int code = command.doCommand(this, _args);

      return code != 0;
    }
    else if (command != null) {
      if (! command.isProOnly() || (command.isProOnly() && isPro())) {
        int code = command.doCommand(this, _args);

        System.exit(code);
      }
      else {
        System.err.println(L().l(
          "command '{0}' is only available with Resin Pro",
          command.getName()));

        System.exit(ExitCode.UNKNOWN.ordinal());
      }
    }

    throw new IllegalStateException(L().l("Unknown start mode"));
  }

  private boolean isPro()
  {
    return _args.isResinProfessional();
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
    final String jvmVersion = System.getProperty("java.runtime.version");

    if ("1.6".compareTo(jvmVersion) > 0) {
      throw new ConfigException(L().l("Resin requires Java 1.6 or later but was started with {0}",
                                      jvmVersion));
    }

    ResinBoot boot = null;
    BootCommand command = null;
    try {
      boot = new ResinBoot(argv);

      if (System.getProperty("log.level") != null
          || boot.getArgs().isVerbose()) {
      }
      else {
        for (Handler handler : Logger.getLogger("").getHandlers()) {
          if (handler instanceof ConsoleHandler) {
            handler.setLevel(Level.FINER);
            Logger.getLogger("").removeHandler(handler);
          }
        }
      }

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
      printException(e, boot);

      if (command != null)
        command.usage(false);

      System.exit(ExitCode.UNKNOWN_ARGUMENT.ordinal());
    } catch (ConfigException e) {
      printException(e, boot);

      System.exit(ExitCode.BAD_CONFIG.ordinal());
    } catch (Exception e) {
      printException(e, boot);

      System.exit(ExitCode.UNKNOWN.ordinal());
    }
  }
  
  private static void setLoggingLevel(Level level)
  {
    Logger.getLogger("").setLevel(level);
  
    for (Handler handler : Logger.getLogger("").getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        handler.setLevel(level);
      }
    }
  }
  
  private static void printException(Throwable e, ResinBoot boot)
  {
    System.err.println(e.getMessage());
  
    if (e.getMessage() == null
        || boot != null && boot.getArgs().isVerbose()
        || log.isLoggable(Level.FINER)) {
      e.printStackTrace();
    }
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(ResinBoot.class);

    return _L;
  }
}
