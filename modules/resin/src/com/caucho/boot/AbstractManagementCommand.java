/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
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

import com.caucho.bam.NotAuthorizedException;
import com.caucho.network.listen.TcpSocketLinkListener;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

public abstract class AbstractManagementCommand extends AbstractBootCommand {
  private static final L10N L = new L10N(AbstractManagementCommand.class);
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client)
    throws BootArgumentException
  {
    ManagerClient managerClient = null;

    try {
      managerClient = getManagerClient(args, client);

      return doCommand(args, client, managerClient);
    } catch (Exception e) {
      if (args.isVerbose())
        e.printStackTrace();
      else
        System.out.println(e.toString());

      if (e instanceof NotAuthorizedException)
        return 1;
      else
        return 2;
    } finally {
      if (managerClient != null)
        managerClient.close();
    }
  }

  protected abstract int doCommand(WatchdogArgs args,
                                   WatchdogClient client,
                                   ManagerClient managerClient);

  protected ManagerClient getManagerClient(WatchdogArgs args,
                                           WatchdogClient client)
  {
    String address = args.getArg("-address");

    if (address == null || address.isEmpty())
      address = client.getConfig().getAddress();

    int port = -1;

    String portArg = args.getArg("-port");

    try {
      if (portArg != null && ! portArg.isEmpty())
        port = Integer.parseInt(portArg);
    } catch (NumberFormatException e) {
      NumberFormatException e1 = new NumberFormatException(
        "-port argument is not a number '" + portArg + "'");
      e1.setStackTrace(e.getStackTrace());

      throw e;
    }
    
    int httpPort = port;
    
    if (port < 0)
      port = client.getConfig().getPort();
    
    if (httpPort < 0)
      httpPort = findPort(client);

    /*
    if (port == 0) {
      throw new ConfigException(L.l("HTTP listener {0}:{1} was not found",
                                    address, port));
    }*/

    String user = args.getArg("-user");
    String password = args.getArg("-password");
    
    if (user == null && password == null) {
      password = client.getResinSystemAuthKey();
    }

    return new ManagerClient(address, port, httpPort, user, password);
    
    // return new ManagerClient(address, port, user, password);
  }
  
  private int findPort(WatchdogClient client)
  {
    for (TcpSocketLinkListener listener : client.getConfig().getPorts()) {
      if (listener instanceof OpenPort) {
        OpenPort openPort = (OpenPort) listener;
        
        if ("http".equals(openPort.getProtocolName()))
          return openPort.getPort();
      }
    }
    
    return 0;
  }
}
