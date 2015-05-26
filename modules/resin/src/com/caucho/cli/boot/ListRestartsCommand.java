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

import java.util.Date;

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.config.types.Period;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.util.CurrentTime;
import com.caucho.util.L10N;

public class ListRestartsCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(ListRestartsCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("period",
                   "period",
                   "specifies look back period of time. e.g. '-period 1D' will list restarts since same time yesterday.");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "lists the most recent Resin server restart times";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi client)
  {
    String listPeriod = args.getArg("-period");

    if (listPeriod == null)
      listPeriod = "7D";

    final long period = Period.toPeriod(listPeriod);

    Date since = new Date(CurrentTime.getCurrentTime() - period);

    Date []restarts = client.listRestarts(period);
    
    String message;

    if (restarts.length == 0) {
      message = L.l("Server hasn't restarted since '{1}'",
                    since);
    }
    else if (restarts.length == 1) {
      StringBuilder resultBuilder = new StringBuilder(L.l(
        "Server started 1 time since '{0}'", since));

      resultBuilder.append("\n  ");
      resultBuilder.append(restarts[0]);

      message = resultBuilder.toString();
    }
    else {
      StringBuilder resultBuilder = new StringBuilder(L.l(
        "Server restarted {0} times since '{1}'",
        restarts.length,
        since));

      for (Date restart : restarts) {
        resultBuilder.append("\n  ");
        resultBuilder.append(restart);
      }

      message = resultBuilder.toString();
    }


    System.out.println(message);

    return ExitCode.OK;
  }
}
