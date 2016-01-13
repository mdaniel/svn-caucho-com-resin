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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.shell.EnvCli;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ServerBaseOld;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;

/**
 * Command to shutdown Resin server
 * bin/resin.sh shutdown -server a
 */
public class ShutdownCommand extends ServerCommandBase<ArgsDaemon>
{
  private static final L10N L = new L10N(ShutdownCommand.class);
  private static final Logger log
    = Logger.getLogger(ShutdownCommand.class.getName());
  
  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();

    addFlagOption("immediate", "shutdown immediately without graceful close");
    
    //ClientWatchdog.addBootOptions(this);
  }

  @Override
  public String getDescription()
  {
    return "shuts down the watchdog and its managed servers";
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args,
                            ConfigBoot boot)
    throws BootArgumentException
  {
    if (shutdownBackground(args)) {
      return ExitCode.OK;
    }
    
    ServerConfigBoot server = boot.findWatchdogServer(args);
    
    if (server == null) {
      throw new ConfigException(L.l("Can't find a matching shutdown server."));
    }
    
    /*
    try (ClientWatchdog client = new ClientWatchdog(args, server)) {
      WatchdogService watchdog = client.getWatchdogService();
      
      ShutdownModeAmp mode = ShutdownModeAmp.GRACEFUL;
      
      if (args.getArgBoolean("immediate")) {
        mode = ShutdownModeAmp.IMMEDIATE;
      }
      
      String status = watchdog.shutdown(mode);
      
      log.fine(server + ": " + status);
      
      if ("ok".equals(status)) {
        status = "";
      }
      else {
        status = "\n  " + status;
      }

      System.out.println(L.l("{0}/{1} shutdown watchdog at {2}:{3}{4}",
                             args.getDisplayName(),
                             Version.getVersion(),
                             server.getWatchdogAddress(args),
                             server.getWatchdogPort(args),
                             status));
    } catch (Exception e) {
      System.out.println(L.l(
        "{0}/{1} can't shutdown watchdog at {2}:{3}.\n{4}",
        args.getDisplayName(),
        Version.getVersion(),
        server.getWatchdogAddress(args),
        server.getWatchdogPort(args),
        e.toString()));

      log.log(Level.FINE, e.toString(), e);
    }
    */

    return ExitCode.OK;
  }
  
  private boolean shutdownBackground(ArgsDaemon args)
  {
    HashMap<Integer,ServerBaseOld> serverMap;
    
    EnvCli env = args.envCli();
    
    serverMap = (HashMap) env.get("baratine_servers");
    
    if (serverMap == null) {
      return false;
    }
    
    ArrayList<ServerBaseOld> servers = new ArrayList<>(serverMap.values());
    
    for (ServerBaseOld server : servers) {
      server.shutdown(ShutdownModeAmp.GRACEFUL);
    }
    
    return true;
  }

  /*
  @Override
  public boolean isRetry()
  {
    return true;
  }
  */
}