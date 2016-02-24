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
 * @author Alex Rojkov
 */

package com.caucho.v5.cli.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.shell_old.EnvCliOld;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ArgsServerBase;
import com.caucho.v5.server.container.ServerBaseOld;
import com.caucho.v5.util.L10N;
import com.caucho.v5.web.server.ServerBase;

/**
 * Command to start Resin server
 * resinctl start
 */
public class StartCommand extends StartCommandBase
{
  private static final Logger log
    = Logger.getLogger(StartCommand.class.getName());
  private static final L10N L = new L10N(StartCommand.class);
  
  private static final long START_TIMEOUT = 120 * 1000L;
  
  public StartCommand()
  {
  }
  
  /*
  @Override
  public void parser(OptionContainer<?> parser)
  {
    super.parser(parser);
  }
  */

  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();
    
    addFlagOption("foreground",
        "starts the server as a foreground task").tiny("fg");
    
    addFlagOption("background",
        "starts the server as a background thread").tiny("bg");
    
    addValueOption("deploy", "file", "adds a deploy file or directory");
    addFlagOption("remove-data", "remove the data directory before starting").hide();
    
    addValueOption("pod", "name", "restricts the server to the given pod");
    addFlagOption("pod-any", "allows the server to be used for dynamic pods");
    
    addOption(new ArgsDaemon.SeedOption());
  }

  @Override
  public String getDescription()
  {
    return "starts servers and optionally deploys applications";
  }

  @Override
  public ExitCode doCommandImpl(ArgsDaemon args) // , ConfigBoot boot)
      throws BootArgumentException
  {
    ConfigBoot boot = null;
    
    // validateRootDirectory(args, boot.getRoot());
    validateArgs(args);
    
    if (args.getArgFlag("foreground")) {
      return startForeground(args, boot);
    }
    else if (args.getArgFlag("background")) {
      return startBackground(args, boot);
    }
    else {
      return super.doCommandImpl(args);
    }
  }

  /**
   * Start the servers, launching the watchdog if necessary.
   * 
   * Any tail paths like foo.war or foo/ are treated as deploy paths.
   */
  //@Override
  public ExitCode doCommand(ArgsDaemon args, 
                            ConfigBoot boot,
                            ServerConfigBoot watchdogServer)
    throws BootArgumentException
  {
    if (true) throw new UnsupportedOperationException();
    return ExitCode.FAIL;
    /*
    ArrayList<ServerConfigBoot> servers = boot.findStartServers(args);

    if (servers.size() == 0) {
      Path path = args.getConfigPath();
      
      if (path == null) {
        path = args.getConfigPathDefault();
      }
      
      String serverId = boot.getServerId(args);
      
      if (serverId != null) {
        throw error("{0}/{1} can't find a local server named '{2}' to start in {3}.",
                    args.getDisplayName(),
                    Version.getVersion(),
                    serverId,
                    path);
        
      }
      
      throw error("{0}/{1} can't find any local servers to start in {2}.",
                  args.getDisplayName(),
                  Version.getVersion(),
                  path);
    }
    
    try {
      WatchdogLauncher launcher = new WatchdogLauncher(args, watchdogServer, watchdog);

      System.out.println(L.l("{0}/{1} start with watchdog at {2}:{3}",
                               args.getDisplayName(),
                               Version.getVersion(),
                               watchdogServer.getWatchdogAddress(args),
                               watchdogServer.getWatchdogPort(args)));
      
      boolean isLauncher = false;
      for (ServerConfigBoot server : servers) {
        if (! isLauncher && ! launcher.start(server, args, watchdog)) {
          launcher.launchManager(args.getArgv());
          isLauncher = true;
        }
        
        waitForServer(server.getPort(), watchdog, launcher);
        
        System.out.println("  starting " + server.getDebugId());
      }
      
      doDeploy(boot, args);
    } catch (Exception e) {
      String eMsg;

      if (e instanceof ConfigException)
        eMsg = e.getMessage();
      else
        eMsg = e.toString();

      System.out.println(L.l(
        "{0}/{1} can't start with watchdog at {2}:{3}.\n  {4}",
        args.getDisplayName(),
        Version.getVersion(),
        watchdogServer.getWatchdogAddress(args),
        watchdogServer.getWatchdogPort(args),
        eMsg));

      log.log(Level.FINE, e.toString(), e);
      
      if (args.isVerbose()) {
        e.printStackTrace();
      }

      return ExitCode.FAIL;
    }

    return ExitCode.OK;
    */
  }
  
  /*
  private String waitForServer(int port, 
                               WatchdogService watchdog,
                               WatchdogLauncher launcher)
  {
    if (port <= 0) {
      throw error(L.l("port {0} is an invalid server port.", port));
    }
    
    long expires = CurrentTime.getCurrentTimeActual() + START_TIMEOUT;
    
    RuntimeException exn = null;
    
    while (CurrentTime.getCurrentTimeActual() < expires && launcher.isActive()) {
      try {
        ResultFuture<String> future = new ResultFuture<>();
    
        watchdog.waitForStart(port, future);
    
        String value = null;
        
        try {
          value = future.get(15, TimeUnit.SECONDS);
          
          return value;
        } catch (ServiceExceptionFutureTimeout e) {
          log.log(Level.FINEST, e.toString(), e);
        }
        
        log.info(" ... waiting for start confirmation");
        
        try {
          value = future.get(START_TIMEOUT, TimeUnit.MILLISECONDS);
          
          return value;
        } catch (ServiceExceptionFutureTimeout e) {
          log.log(Level.FINEST, e.toString(), e);
        }
        
        // System.out.println("WFS:" + value);
    
        return value;
      } catch (RuntimeException e) {
        exn = e;
      }
      
      try {
        Thread.sleep(100);
      } catch (Exception e) {
      }
    }
    
    if (exn != null) {
      throw exn;
    }
    else
      return null;
  }
  */
  
  @Override
  protected void validateArgs(ArgsDaemon args)
  {
    args.getArgInt("debug-port", 0);
  }

  private ExitCode startForeground(ArgsDaemon args, ConfigBoot boot)
    throws BootArgumentException
  {
    ArrayList<ServerConfigBoot> servers = boot.findStartServers(args);
    
    if (servers.size() == 0) {
      throw error("Can't find any local servers to start.");
    }
    
    ServerConfigBoot serverConfig = servers.get(0);

    ArgsServerBase serverArgs = buildServerArgs(args, serverConfig);
    
    ServerBase server = null;//
    /*
    serverArgs.createServer(args.getProgramName(),
                                                serverConfig);
                                                */
    
    try {
      // resin.start();
      
      doDeploy(boot, args);
      
      server.waitForExit();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
    
    return ExitCode.OK;
  }

  private ExitCode startBackground(ArgsDaemon args, ConfigBoot boot)
    throws BootArgumentException
  {
    ArrayList<ServerConfigBoot> servers = boot.findStartServers(args);
    
    if (servers.size() == 0) {
      throw error("Can't find any local servers to start.");
    }
    
    ServerConfigBoot serverConfig = servers.get(0);

    ArgsServerBase serverArgs = buildServerArgs(args, serverConfig);
    /*
    ServerBase server = serverArgs.createServer(args.getProgramName(),
                                                serverConfig);
    */
    try {
      //addServer(args.envCli(), server);
      
      doDeploy(boot, args);
      
      // XXX: need to save the server somewhere
      // server.waitForExit();
    } catch (Exception e) {
      throw ConfigException.wrap(e);
    }
    
    return ExitCode.OK;
  }
  
  private void addServer(EnvCliOld env, ServerBaseOld server)
  {
    HashMap<Integer,ServerBaseOld> serverMap;
    
    serverMap = (HashMap) env.get("baratine_servers");
    
    if (serverMap == null) {
      serverMap = new HashMap<>();
      env.put("baratine_servers", serverMap);
    }
    
    serverMap.put(server.getServerSelf().port(), server);
  }
  
  private ArgsServerBase buildServerArgs(ArgsDaemon args,
                                         ServerConfigBoot server)
  {
    ArgsServerBase serverArgs = new ArgsServerBase(args.getArgv());
    
    serverArgs.copyFrom(args);
    
    serverArgs.setServerId(server.getId());
    
    return serverArgs;
  }

  protected ExitCode doDeploy(ConfigBoot boot, ArgsDaemon args)
  {
    String deploy = args.getArg("deploy");
    
    return doDeployPath(boot, args, deploy);
  }
  
  protected ExitCode doDeployPath(ConfigBoot boot, 
                                  ArgsDaemon args,
                                  String deploy)
  {
    if (deploy == null) {
      return ExitCode.OK;
    }
    
    ExitCode deployCode = ExitCode.FAIL; // 2;
    int retryCount = 15;
    
    RuntimeException lastException = null;
    
    /*
    try (ClientDeploy client = new ClientDeploy(args, boot)) {
      for (int i = 0; i < retryCount; i++) {
        try {
          deployCode = doDeploy(client, args, boot, deploy);
        
          if (deployCode == ExitCode.OK) {
            return ExitCode.OK;
          }
        
          Thread.sleep(1000);
        } catch (ServiceExceptionConnect exn) {
          try { Thread.sleep(1000); } catch (Exception e) {}

          lastException = exn;
        } catch (ServiceExceptionNotFound exn) {
          try { Thread.sleep(1000); } catch (Exception e) {}

          lastException = exn;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }
    */
    
    if (lastException != null) {
      throw lastException;
    }
    
    return deployCode;
  }
  
  /*
  private ExitCode doDeploy(ClientDeploy client, 
                            ArgsDaemon args,
                            ConfigBoot boot,
                            String deploy)
  {
    Objects.requireNonNull(deploy);
    
    PathImpl path = Vfs.lookup(deploy);
    
    String address = getAddress(client, args, path);
    
    if (address.endsWith(".bar")) {
      ArrayList<PathImpl> pathList = new ArrayList<>();
      pathList.add(path);
      
      ServiceManifest manifest = client.deployService(args, boot, pathList);
      
      System.out.println("  deployed " + path.getTail() + " to " + manifest.getConfigPath());
    }
    else {
      client.put(address, path);
      
      System.out.println("  deployed " + path.getTail() + " to " + address);
    }
    
    
    if (address.endsWith(".bar")) {
      int p = address.lastIndexOf('/');
      int q = address.lastIndexOf('.');
      
      String name = address.substring(p + 1, q);
      
      address = "bfs://" + PodBuilderService.CONFIG_DIR + "/" + name + ".cf"; 
    }
    
    return ExitCode.OK;
  }
  */
  
  /*
  protected String getAddress(ClientDeploy client, ArgsDaemon args, PathImpl path)
  {
    String tail = path.getTail();
    
    if (tail.endsWith(".war")) {
      return getAddressWar(client, args, path);
    }
    else if (tail.endsWith(".bar")) {
      return getAddressBar(client, args, path);
    }
    else {
      return getAddressDefault(client, args, path);
    }
  }
  
  protected String getAddressDefault(ClientDeploy client, ArgsDaemon args, PathImpl path)
  {
    return getAddressBar(client, args, path);
  }
  
  protected String getAddressWar(ClientDeploy client, ArgsDaemon args, PathImpl path)
  {
    return client.getAddressWar(args, path);
  }
  
  protected String getAddressBar(ClientDeploy client, ArgsDaemon args, PathImpl path)
  {
    ServiceManifest manifest = client.getServiceManifest(args, path);
    
    return "/usr/lib/" + manifest.getPodName() + ".bar";
  }
  */
}
