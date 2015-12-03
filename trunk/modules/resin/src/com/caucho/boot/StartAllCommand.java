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
 * @author Alex Rojkov
 */

package com.caucho.boot;

import com.caucho.VersionFactory;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to start Resin server
 * bin/resin.sh start-all
 */
public class StartAllCommand extends AbstractStartCommand
{
  private static Logger _log;
  private static L10N _L;
  
  protected StartAllCommand()
  {
    addFlagOption("elastic-dns",
                  "retry DNS address lookup on start until success");
  }

  @Override
  public String getDescription()
  {
    return "starts all servers listening to the machine's IP interfaces";
  }
  
  @Override
  public boolean isStart()
  {
    return true;
  }
  
  @Override
  public boolean isStartAll()
  {
    return true;
  }

  @Override
  public int doCommand(ResinBoot boot, WatchdogArgs args)
    throws BootArgumentException
  {
    ArrayList<WatchdogClient> clientList = boot.findLocalClients();
    
    if (clientList.size() == 0 && ! boot.isElasticIp(args)) {
      System.out.println(L().l("Resin/{0} cannot find any local servers to start in the configuration.",
                               VersionFactory.getVersion()));
      
      return 0;
    }
    
    return super.doCommand(boot, args);
  }

  @Override
  protected WatchdogClient findLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    /*
    if (boot.isElasticIp(args))
      return findLocalClientImpl(boot, args);
    else
      return super.findLocalClient(boot, args);
      */
    return findLocalClientImpl(boot, args);
  }

  @Override
  protected WatchdogClient findWatchdogClient(ResinBoot boot, WatchdogArgs args)
  {
    if (boot.isElasticIp(args))
      return findWatchdogClientImpl(boot, args);
    else
      return super.findWatchdogClient(boot, args);
  }

  @Override
  public int doCommand(WatchdogArgs args, WatchdogClient client)
    throws BootArgumentException
  {
    try {
      client.startAllWatchdog(args.getArgv(), true);

      System.out.println(L().l("Resin/{0} start-all with watchdog at {1}:{2}",
                             VersionFactory.getVersion(),
                             client.getWatchdogAddress(),
                             client.getWatchdogPort()));
    } catch (Exception e) {
      String eMsg;

      if (e instanceof ConfigException)
        eMsg = e.getMessage();
      else
        eMsg = e.toString();

      System.out.println(L().l(
        "Resin/{0} can't start-all for watchdog at {1}:{2}.\n  {3}",
        VersionFactory.getVersion(),
        client.getWatchdogAddress(),
        client.getWatchdogPort(),
        eMsg));

      log().log(Level.FINE, e.toString(), e);

      System.exit(1);
    }

    return 0;
  }

  @Override
  public void doWatchdogStart(WatchdogManager manager)
  {
    BootResinConfig boot = manager.getManagerConfig();
    WatchdogArgs args = manager.getArgs();
    
    String serverId = args.getClientServerId();
    
    if (serverId != null && boot.isElasticServer(args)) {
      manager.startServerAll(serverId, args.getArgv());
      return;
    }
    
    ArrayList<WatchdogClient> clientList;
    
    do {
      clientList = boot.findLocalClients(null);
      
      if (clientList.size() == 0 && boot.isElasticDns(args)) {
        try {
          log().info("No local IP address found, waiting...");
          Thread.sleep(10000);
        } catch (Exception e) {
        }
      }
    } while (clientList.size() == 0 && boot.isElasticDns(args));

    for (WatchdogClient client : clientList) {
      try {
        manager.startServer(client.getId(), args.getArgv());
      } catch (Exception e) {
        String eMsg;

        if (e instanceof ConfigException)
          eMsg = e.getMessage();
        else
          eMsg = e.toString();

        System.out.println(L().l("Resin/{0} can't start -server '{1}' for watchdog at {2}:{3}.\n  {4}",
                                 VersionFactory.getVersion(),
                                 client.getId(),
                                 client.getWatchdogAddress(),
                                 client.getWatchdogPort(),
                                 eMsg));

        log().log(Level.FINE, e.toString(), e);
      }
    }
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(StartAllCommand.class.getName());

    return _log;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(StartAllCommand.class);

    return _L;
  }
}
