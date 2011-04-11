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

import com.caucho.config.types.Period;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

import java.util.logging.Level;

public class LogLevelCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(LogLevelCommand.class);

  @Override
  public void doCommand(WatchdogArgs args, WatchdogClient client)
  {
    String level = args.getDefaultArg();

    if (level == null) {
      usage();

      return;
    }

    Level logLevel;

    try {
      logLevel = Level.parse(level.toUpperCase());
    } catch (IllegalArgumentException e) {
      System.err.println(e.getMessage());
      System.err.println();

      usage();

      return;
    }

    long period = 1000 * 60;

    String time = args.getArg("-active-time");

    if (time != null)
      period = Period.toPeriod(time);

    String logger = args.getArg("-log-name");

    if (logger == null)
      logger = "";

    ManagerClient manager = getManagerClient(args, client);

    String message = manager.setLogLevel(logger, logLevel, period);

    System.out.println(message);
  }

  @Override
  public void usage()
  {
    System.err.println(L.l("usage: java -jar resin.jar [-conf <file>] log-level -user <user> -password <password> [-active-time <time-period>] [-log-name <name>] <level>"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   sets level for root logger to <level> (severe, warning, info, config, fine, finer, finest)"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l("   -log-name            : specifies name of the logger. Defaults to root logger"));
    System.err.println(L.l("   -active-time         : specifies new level active time (default 60s). e.g. 5s"));
  }
}
