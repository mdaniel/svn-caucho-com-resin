/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.cli.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cli.daemon.ArgsDaemon;
import com.caucho.cli.server.BootArgumentException;
import com.caucho.cli.server.ClientWatchdog;
import com.caucho.cli.server.ServerCommandBase;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.config.ConfigBoot;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.server.watchdog.WatchdogService;
import com.caucho.util.L10N;
import com.caucho.util.Version;

/**
 * Command to stop Resin server
 * bin/resin.sh kill -server a
 */
public class KillCommand extends ServerCommandBase<ArgsDaemon>
{
  private static final L10N L = new L10N(KillCommand.class);
  private static final Logger log
  = Logger.getLogger(KillCommand.class.getName());
  
  @Override
  public String getDescription()
  {
    return "forces a kill of a server";
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args, 
                            ConfigBoot boot)
    throws BootArgumentException
  {
    ServerConfigBoot server = boot.findWatchdogServer(args);
    
    try (ClientWatchdog client = new ClientWatchdog(args, server)) {
      WatchdogService watchdog = client.getWatchdogService();
      
      watchdog.kill(server.getPort());

      System.out.println(L.l(
        "Resin/{0} killed --port {1} for watchdog at {2}:{3}",
        Version.getVersion(),
        server.getPort(),
        server.getWatchdogAddress(args),
        server.getWatchdogPort(args)));
    } catch (Exception e) {
      System.out.println(L.l(
        "Resin/{0} can't kill -server '{1}' (client {2}) for watchdog at {3}:{4}.\n{5}",
        Version.getVersion(),
        args.getServerId(), server,
        server.getWatchdogAddress(args),
        server.getWatchdogPort(args),
        e.toString()));

      log.log(Level.FINE, e.toString(), e);

      return ExitCode.EXIT_1;
    }

    return ExitCode.OK;
  }

  /*
  @Override
  public boolean isRetry()
  {
    return true;
  }
  */
}