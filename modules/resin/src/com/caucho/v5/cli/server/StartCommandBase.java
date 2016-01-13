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

import java.util.logging.Logger;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.spi.OptionCommandLine.ArgsType;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;


public abstract class StartCommandBase extends ServerCommandBase<ArgsDaemon>
{
  private static final L10N L = new L10N(StartCommandBase.class);
  private static final Logger log
    = Logger.getLogger(StartCommandBase.class.getName());
  
  
  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();

    //ClientWatchdog.addBootOptions(this);

    addValueOption("cluster", 
                   "id", "cluster to join as an dynamic server");

    // needs to be moved to non-run
    addIntValueOption("debug-port", "port", "listen to a JVM debug port")
                    .type(ArgsType.DEBUG).deprecated();
    addIntValueOption("jmx-port", 
                      "port", "listen to an unauthenticated JMX port")
                     .type(ArgsType.DEBUG).deprecated();
    
    addFlagOption("remove-data", 
                  "remove persistent data prior to start")
                     .type(ArgsType.DEBUG).hide();
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args, ConfigBoot boot)
    throws BootArgumentException
  {
    ServerConfigBoot server = boot.findWatchdogServer(args);
    
    if (server == null) {
      String clusterId = boot.getHomeCluster(args);
      
      throw new ConfigException(L.l("Can't find a watchdog server matching a local IP address in cluster '{0}'",
                                    clusterId));
    }
    
    if (true) throw new UnsupportedOperationException();
    
    return ExitCode.FAIL;
    /*
    try (ClientWatchdog client = new ClientWatchdog(args, server)) {
      WatchdogService watchdog = client.getWatchdogService();
      
      return doCommand(args, boot, server, watchdog);
    } catch (ConfigException e) {
      System.out.println(e.getMessage());
      
      if (args.isVerbose()) {
        e.printStackTrace();
      }
      
      return ExitCode.EXIT_1; // was 2;
    } catch (ServiceException e) {
      System.out.println(e.toString());
      
      if (args.isVerbose()) {
        e.printStackTrace();
      }
      
      return ExitCode.EXIT_1; // was 2
    } catch (Exception e) {
      Throwable cause = e;
      
      while (cause.getCause() != null) {
        cause = cause.getCause();
      }
      
      System.out.println(cause.toString());

      if (args.isVerbose()) {
        e.printStackTrace();
      }

      return ExitCode.EXIT_1; // was 2;
    }
  */
  }

  /*
  abstract public ExitCode doCommand(ArgsDaemon args, 
                                     ConfigBoot boot,
                                     ServerConfigBoot watchdogServer,
                                     WatchdogService client)
    throws BootArgumentException;
    */
}
