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

package com.caucho.cli.boot;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.cli.daemon.ArgsDaemon;
import com.caucho.cli.server.BootArgumentException;
import com.caucho.cli.server.StartCommandBase;
import com.caucho.config.ConfigException;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.config.ConfigBoot;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.server.watchdog.WatchdogService;
import com.caucho.util.L10N;
import com.caucho.util.Version;

/**
 * Command to start Resin server in gui mode
 * bin/resin.sh start-with-background -server a
 */
public class StartWithForegroundCommand extends StartCommandBase
{
  private static final Logger log
    = Logger.getLogger(StartWithForegroundCommand.class.getName());
  private static final L10N L = new L10N(StartWithForegroundCommand.class);

  @Override
  public String getName()
  {
    return "start-with-foreground";
  }

  @Override
  public String getDescription()
  {
    return "starts the watchdog in foreground mode (for MacOS-X)";
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args,
                       ConfigBoot boot,
                       ServerConfigBoot watchdogServer,
                       WatchdogService watchdog)
    throws BootArgumentException
  {
    try {
      Process process = null; // server.startWatchdog(args.getArgv(), true);

      final String message;
      if (process != null)
        message
          = "Resin/{0} started -server '{1}' for watchdog at {2}:{3} with foreground";
      else
        message
          = "Resin/{0} started -server '{1}' for watchdog at {2}:{3} with no foreground";

      System.out.println(L.l(
        message,
        Version.getVersion(),
        watchdogServer.getId(),
        watchdogServer.getWatchdogAddress(args),
        watchdogServer.getWatchdogPort(args)));

      if (process != null) {
        process.waitFor();

        int value = process.exitValue();
        
        if (value == 0) {
          return ExitCode.OK;
        }
        else {
          return ExitCode.EXIT_1; // process.exitValue();
        }
      }
      else {
        return ExitCode.OK;
      }
    } catch (Exception e) {
      String eMsg;

      if (e instanceof ConfigException)
        eMsg = e.getMessage();
      else
        eMsg = e.toString();

      System.out.println(L.l(
        "Resin/{0} can't start-with-foreground -server '{1}' for watchdog at {2}:{3}.\n  {4}",
        Version.getVersion(),
        watchdogServer.getId(),
        watchdogServer.getWatchdogAddress(args),
        watchdogServer.getWatchdogPort(args),
        eMsg));

      log.log(Level.FINE, e.toString(), e);

      System.exit(1);
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
