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

import com.caucho.bam.*;
import com.caucho.bam.actor.ActorSender;
import com.caucho.config.ConfigException;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

public abstract class AbstractManagementCommand extends AbstractRemoteCommand {
  public static final int RETURN_CODE_SERVER_ERROR = 32;
  private static final L10N L = new L10N(AbstractManagementCommand.class);

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client)
    throws BootArgumentException
  {
    ManagerClient managerClient = null;

    try {
      managerClient = createManagerClient(args, client);
      System.out.println("WDC: " + managerClient + " " + client);

      return doCommand(args, client, managerClient);
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

      if (args.isVerbose()) {
        e.printStackTrace();
      }

      if (e instanceof NotAuthorizedException)
        return 1;
      else
        return 2;
    } finally {
      if (managerClient != null)
        managerClient.close();
    }
  }

  @Override
  public boolean isProOnly()
  {
    return true;
  }

  protected abstract int doCommand(WatchdogArgs args,
                                   WatchdogClient client,
                                   ManagerClient managerClient);

  protected ManagerClient createManagerClient(WatchdogArgs args,
                                              WatchdogClient client)
  {
    ActorSender bamSender = createBamClient(args, client);
   
    return new ManagerClient(bamSender);
  }
}
