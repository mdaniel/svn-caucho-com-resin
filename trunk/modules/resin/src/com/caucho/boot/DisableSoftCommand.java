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

import com.caucho.cloud.scaling.ResinScalingClient;
import com.caucho.cloud.topology.CloudServerState;
import com.caucho.config.ConfigException;
import com.caucho.util.L10N;

public class DisableSoftCommand extends AbstractScalingCommand
{
  private static final L10N L = new L10N(DisableSoftCommand.class);

  @Override
  public String getDescription()
  {
    return "allows existing sessions to finish and disables the server";
  }

  @Override
  public int doCommand(WatchdogArgs args, WatchdogClient client)
    throws BootArgumentException
  {
    if (! isPro()) {
      System.out.println("command 'disable-soft' is only available with Resin Pro");

      return 3;
    }

    ResinScalingClient scalingClient = getScalingClient(args, client);

    String server = args.getDefaultArg();
    
    if (server == null)
      server = args.getServerId();

    if (server == null)
      throw new ConfigException("server is not specified");

    CloudServerState state = scalingClient.disableSoft(server);

    scalingClient.close();

    String message;
    if (state == null)
      message = L.l("server '{0}' is not found", server);
    else
      message = L.l("server '{0}' state: {1}", server, state);

    System.out.println(message);

    return 0;
  }

  @Override
  public String getUsageArgs()
  {
    return " [<server>]";
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }
}
