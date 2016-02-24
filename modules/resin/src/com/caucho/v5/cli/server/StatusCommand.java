/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 * @author Alex Rojkov
 */

package com.caucho.v5.cli.server;

import java.util.HashMap;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.shell_old.EnvCliOld;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ServerBaseOld;
import com.caucho.v5.util.L10N;

/**
 * Command to kill Resin server
 * bin/resin.sh stop -server a
 */
public class StatusCommand extends ServerCommandBase<ArgsDaemon>
{
  private static Logger log = Logger.getLogger(StatusCommand.class.getName());
  private static L10N L = new L10N(StatusCommand.class);

  @Override
  public String getDescription()
  {
    return "status for a server";
  }

  @Override
  public ExitCode doCommandImpl(ArgsDaemon args)
    throws BootArgumentException
  {
    ConfigBoot boot = null;
    ServerConfigBoot server = boot.findWatchdogServer(args);
    
    if (stopBackground(args, server)) {
      return ExitCode.OK;
    }
    
    /*
    try (ClientWatchdog client = new ClientWatchdog(args, server)) {
      WatchdogService watchdog = client.getWatchdogService();
      
      String status = watchdog.status();

      System.out.println(L.l(
        "{0}/{1} status for watchdog at {3}:{4}",
        args.getDisplayName(),
        Version.getVersion(),
        server.getPort(),
        server.getWatchdogAddress(args),
        server.getWatchdogPort(args)));
      
      System.out.println(status);
    } catch (Exception e) {
      System.out.println(L.l(
        "{0}/{1} can't kill --port {2} for watchdog at {3}:{4}.\n{5}",
        args.getDisplayName(),
        Version.getVersion(),
        server.getPort(),
        server.getWatchdogAddress(args),
        server.getWatchdogPort(args),
        e.toString()));

      log.log(Level.FINE, e.toString(), e);
      
      if (args.isVerbose()) {
        e.printStackTrace();
      }

      return ExitCode.FAIL;
    }
    */

    return ExitCode.OK;
  }
  
  private boolean stopBackground(ArgsDaemon args, ServerConfigBoot serverConfig)
  {
    ServerBaseOld server = findBackgroundServer(args, serverConfig);
    
    if (server == null) {
      return false;
    }
    
    server.shutdown(ShutdownModeAmp.IMMEDIATE);
    
    System.out.println("  kill --port " + serverConfig.getPort() + " (background)");
    
    return true;
  }
  
  private ServerBaseOld findBackgroundServer(ArgsDaemon args, ServerConfigBoot serverConfig)
  {
    HashMap<Integer,ServerBaseOld> serverMap;
    
    EnvCliOld env = args.envCli();
    
    serverMap = (HashMap) env.get("baratine_servers");
    
    if (serverMap == null) {
      return null;
    }

    ServerBaseOld server = serverMap.get(serverConfig.getPort());
    
    return server;
  }

  /*
  @Override
  public boolean isRetry()
  {
    return true;
  }
  */
}