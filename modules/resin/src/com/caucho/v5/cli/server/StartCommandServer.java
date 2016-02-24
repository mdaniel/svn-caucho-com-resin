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
 * @author Scott Ferguson
 */

package com.caucho.v5.cli.server;

import java.util.ArrayList;

import com.caucho.v5.cli.daemon.ArgsDaemon.ServerId;
import com.caucho.v5.cli.daemon.ArgsDaemon.ServerPort;
import com.caucho.v5.cli.daemon.ArgsDaemon.WatchdogPort;
import com.caucho.v5.cli.spi.OptionCommandLine;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.health.warning.WarningSystem;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.container.ArgsServerBase;
import com.caucho.v5.server.container.ServerBaseOld;

/**
 * Command to start Resin.
 */
public class StartCommandServer extends ServerCommandBase<ArgsServerBase>
{
  @Override
  public String name()
  {
    return "start";
  }
  
  @Override
  protected void initBootOptions()
  {
    super.initBootOptions();
    
    addOption(new ServerId()).tiny("s").alias("server").alias("-server");
    addOption(new WatchdogPort()).tiny("wp").hide();
    addOption(new ServerPort()).tiny("p").tiny("sp"); // .hide();
    
    addOption(new ArgsServerBase.ClusterOption());
    addOption(new ArgsServerBase.BoundPortOption());
    addOption(new ArgsServerBase.SeedOption());
    addOption(new ServerId());
    addOption(new ArgsServerBase.SocketWaitOption());
    //addOption(new Stage());
    
    addValueOption("deploy", "file", "file to be deployed");
    
    addValueOption("pod", "pod", "pod to restrict service");
    addFlagOption("pod-any", "pod can be used for all services");
    
    addFlagOption("package", "extract packages");
    addFlagOption("remove-data", "removes the data directory");
    
    addOption(new OptionCommandLine.IgnoreValue("debug-port", ""));
    addOption(new OptionCommandLine.IgnoreValue("jmx-port", ""));
    addOption(new OptionCommandLine.IgnoreValue("verbose", ""));
    addOption(new OptionCommandLine.IgnoreValue("watchdog-port", ""));
  }
  
  /*
  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }
  */

  @Override
  public ExitCode doCommandImpl(ArgsServerBase args)
      throws BootArgumentException
  {
    ConfigBoot resinBoot = null;
    ServerBaseOld server = null;
    
    try {
      ArrayList<ServerConfigBoot> servers = resinBoot.findStartServers(args);
      
      if (servers.size() == 0) {
        throw error(args, "Can't find a local server for server-id {0}",
                    args.getServerId());
      }
      
      if (servers.size() > 1) {
        throw error(args, "Can't find a unique local server for server-id {0}",
                    args.getServerId(),
                    toIdList(servers));
      }
      
      ServerConfigBoot serverConfig = servers.get(0);
    
      //server = args.createServer(args.getProgramName(), serverConfig);

      // resin.initMain();

      // resin.getHttpContainer();

      server.waitForExit();

      if (! server.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
            "Resin shutdown from unknown reason");
      }
    } catch (Throwable e) {
      e.printStackTrace();
      
      if (server != null && ! server.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
                                      "Resin shutdown: " + e);
        
        WarningSystem.sendCurrentWarning(this, e);
      }
      
      throw ConfigException.wrap(e);
    } finally {
      if (server != null && ! server.isClosing()) {
        ShutdownSystem.shutdownActive(ExitCode.FAIL_SAFE_HALT,
                           "Resin shutdown from unknown reason");
      }

      if (server != null) {
        server.close();
      }
    }
    
    return ExitCode.OK;
  }
  
  private ArrayList<String> toIdList(ArrayList<ServerConfigBoot> servers)
  {
    ArrayList<String> list = new ArrayList<>();
    
    for (ServerConfigBoot server : servers) {
      list.add(server.getDisplayName());
    }
    
    return list;
  }
}
