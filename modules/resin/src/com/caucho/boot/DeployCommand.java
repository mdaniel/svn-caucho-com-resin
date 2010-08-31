/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.boot;

import java.util.ArrayList;
import java.util.HashMap;

import com.caucho.config.ConfigException;
import com.caucho.network.listen.SocketLinkListener;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.util.L10N;
import com.caucho.vfs.Path;
import com.caucho.vfs.Vfs;

public class DeployCommand extends AbstractBootCommand {
  private static final L10N L = new L10N(DeployCommand.class);
  
  @Override
  public void doCommand(WatchdogArgs args,
                        WatchdogClient client)
  {
    WebAppDeployClient deployClient = getDeployClient(args, client);
    
    String war = findWar(args);
    
    if (war == null) {
      throw new ConfigException(L.l("Cannot find .war argument in command line"));
    }
    
    if (! war.endsWith(".war")) {
      throw new ConfigException(L.l("Deploy expects to be used with a *.war file at {0}",
                                    war));
    }
    
    int p = war.lastIndexOf('.');
    
    String name = args.getArg("-name");
    
    if (name == null)
      name = war.substring(0, p);
    
    String host = args.getArg("-host");
    
    if (host == null)
      host = "default";
    
    String stage = args.getArg("-stage");
    
    if (stage == null)
      stage = "production";
    
    String tag = stage + "/webapp/" + host + "/" + name;
    
    Path path = Vfs.lookup(war);
    
    if (! path.isFile()) {
      throw new ConfigException(L.l("'{0}' is not a readable file.",
                                    path.getFullPath()));
    }
    
    String message = args.getArg("-m");
    
    if (message == null)
      args.getArg("-message");
    
    if (message == null)
      message = "deploy " + war + " from command line";
    
    HashMap<String,String> attributes = new HashMap<String,String>();
    
    deployClient.putTagArchive(tag, path, message, attributes);
    
    deployClient.close();
    
    System.out.println("Deployed " + tag + " as " + war + " to "
                       + deployClient.getUrl());
  }
  
  private String findWar(WatchdogArgs args)
  {
    ArrayList<String> tailArgs = args.getTailArgs();
    
    for (int i = 0; i < tailArgs.size(); i++) {
      String arg = tailArgs.get(i);
      
      if (arg.startsWith("-")) {
        i++;
        continue;
      }
      
      return arg;
    }
    
    return null;
  }
  
  private WebAppDeployClient getDeployClient(WatchdogArgs args,
                                             WatchdogClient client)
  {
    String address = client.getConfig().getAddress();
    
    int port = findPort(client);
    
    if (port == 0) {
      throw new ConfigException(L.l("HTTP listener {0}:{1} was not found",
                                    address, port));
    }
    
    String user = args.getArg("-user");
    String password = args.getArg("-password");
    
    /*
    if (user == null) {
      user = "";
      password = client.getResinSystemAuthKey();
    }
    */
    
    return new WebAppDeployClient(address, port, user, password);
  }
  
  private int findPort(WatchdogClient client)
  {
    for (SocketLinkListener listener : client.getConfig().getPorts()) {
      if (listener instanceof OpenPort) {
        OpenPort openPort = (OpenPort) listener;
        
        if ("http".equals(openPort.getProtocolName()))
          return openPort.getPort();
      }
    }
    
    return 0;
  }
}
