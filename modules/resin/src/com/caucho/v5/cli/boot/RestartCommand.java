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

package com.caucho.v5.cli.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.StartCommandBase;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.watchdog.WatchdogService;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;

/**
 * Command to stop Resin server
 * bin/resin.sh restart -server a
 */
public class RestartCommand extends StartCommandBase
{
  private static final Logger log
    = Logger.getLogger(RestartCommand.class.getName());
  private static final L10N L = new L10N(RestartCommand.class);

  @Override
  public String getDescription()
  {
    return "restarts a Resin server";
  }
  
  /*
  @Override
  public boolean isStart()
  {
    return true;
  }
  */

  //@Override
  public ExitCode doCommand(ArgsDaemon args, 
                            ConfigBoot boot,
                            ServerConfigBoot watchdogServer)
    throws BootArgumentException
  {
    try {
      // if (id == null)
      // id = _client.getId();
      
      //watchdog.restart(watchdogServer.getPort(), args.getArgv());

      System.out.println(L.l(
        "Resin/{0} restarted port {1} for watchdog at {2}:{3}",
        Version.getVersion(),
        watchdogServer.getPort(),
        watchdogServer.getWatchdogAddress(args),
        watchdogServer.getWatchdogPort(args)));
    } catch (Exception e) {
      System.out.println(L.l("Resin/{0} can't restart{1}.\n{2}",
                               Version.getVersion(), 
                               watchdogServer.getPort(),
                               e.toString()));

      log.log(Level.FINE, e.toString(), e);

      return ExitCode.FAIL;
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