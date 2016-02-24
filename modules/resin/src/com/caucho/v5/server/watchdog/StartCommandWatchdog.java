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

package com.caucho.v5.server.watchdog;

import io.baratine.service.ResultFuture;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerId;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerPort;
import com.caucho.v5.cli.daemon.ArgsDaemon.WatchdogPort;
import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.StartCommand;
import com.caucho.v5.cli.spi.OptionCommandLine;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ArgsServerBase;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.Version;

/**
 * Command to start watchdog.
 * 
 * baratine start
 */
public class StartCommandWatchdog
  extends StartCommand
{
  private static final L10N L = new L10N(StartCommandWatchdog.class);
  private static final Logger log
    = Logger.getLogger(StartCommandWatchdog.class.getName());
  
  @Override
  public String name()
  {
    return "start";
  }
  
  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();
    
    addFlagOption("package", "extract packages");
  }

  @Override
  public ExitCode doCommandImpl(ArgsDaemon args)
    throws BootArgumentException
  {
    try {
      ConfigBoot boot = null;
      
      ServerConfigBoot server = null;//boot.findServer(args);
      
      ArgsWatchdog argsWatchdog = (ArgsWatchdog) args;

      WatchdogManager manager = new WatchdogManager(argsWatchdog, server);
      
      startServers(args, boot, server, manager);
    
      manager.waitForExit();
      
      return ExitCode.OK;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  protected void startServers(ArgsDaemon args,
                              ConfigBoot config,
                              ServerConfigBoot server,
                              WatchdogManager manager)
  {
    try {
      ResultFuture<String> future = new ResultFuture<>();
      
      manager.startServerAll(args.getArgv(), future);
      
      String msg = future.get(120, TimeUnit.SECONDS);
    } catch (Exception e) {
      String eMsg;

      if (e instanceof ConfigException)
        eMsg = e.getMessage();
      else
        eMsg = e.toString();
      
      //log.log(Level.FINE, e.toString(), e);
      
      ConfigException exn;
      exn = new ConfigException(L.l("{0}/{1} can't start -server '{2}' for watchdog at {3}:{4}.\n  {5}",
                             args.getDisplayName(),
                               Version.getVersion(),
                               server.getId(),
                               server.getWatchdogAddress(args),
                               server.getWatchdogPort(args),
                               eMsg));
      
      manager.setStartException(exn);

    }

  }
}
