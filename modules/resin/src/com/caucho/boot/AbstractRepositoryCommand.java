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

import com.caucho.bam.BamException;
import com.caucho.bam.ErrorPacketException;
import com.caucho.bam.NotAuthorizedException;
import com.caucho.bam.actor.RemoteActorSender;
import com.caucho.config.ConfigException;
import com.caucho.server.admin.WebAppDeployClient;
import com.caucho.vfs.Path;

public abstract class AbstractRepositoryCommand extends AbstractRemoteCommand 
{
  @Override
  protected void initBootOptions()
  {
    addValueOption("name", "name", "name of the deployment context");
    
    super.initBootOptions();
  }

  @Override
  public final int doCommand(WatchdogArgs args,
                             WatchdogClient client)
    throws BootArgumentException
  {
    WebAppDeployClient deployClient = null;

    try {
      deployClient = getDeployClient(args, client);

      return doCommand(args, client, deployClient);
    } catch (Exception e) {
      Throwable cause = e;

      if (cause instanceof ConfigException || 
        cause instanceof ErrorPacketException) {
        System.out.println(cause.getMessage());
      } else if (cause instanceof BamException) {
        BamException bamException = (BamException) cause;
        if (bamException.getActorError() != null) 
          System.out.println(bamException.getActorError().getText());
        else
          System.out.println(bamException.getMessage());
      } else {
        while (cause.getCause() != null)
          cause = cause.getCause();
        
        System.out.println(cause.toString());
      }

      if (args.isVerbose())
        e.printStackTrace();

      if (e instanceof NotAuthorizedException)
        return 1;
      else
        return 2;
    } finally {
      if (deployClient != null)
        deployClient.close();
    }
  }

  protected abstract int doCommand(WatchdogArgs args,
                                   WatchdogClient client,
                                   WebAppDeployClient deployClient);

  protected WebAppDeployClient getDeployClient(WatchdogArgs args,
                                               WatchdogClient client)
  {
    RemoteActorSender sender = createBamClient(args, client);
    
    // return new WebAppDeployClient(address, port, user, password);
    
    return new WebAppDeployClient(sender.getUrl(), sender);
  }
  
  String getName(WatchdogArgs args, Path path)
  {
    return getWebAppName(args, path);
  }
  
  static String getWebAppName(WatchdogArgs args, Path path)
  {
    String name = args.getArg("-name");
    
    String webapp = args.getArg("-web-app");
    
    if (webapp != null) {
      name = webapp;
    }
    
    if (name == null && path != null) {
      String tail = path.getTail();
    
      int p = tail.lastIndexOf('.');

      if (p > 0)
        name = tail.substring(0, p);
      else
        name = tail;
    }
    
    if (name == null && args.getDefaultArg() != null)
      name = args.getDefaultArg();
    
    if (name == null || name.equals("/")) {
      name = "ROOT";
    }
    else if (name.startsWith("/")) {
      name = name.substring(1);
    }
    
    return name;
  }
}
