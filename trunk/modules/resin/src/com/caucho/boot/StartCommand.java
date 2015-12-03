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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to start Resin server
 * bin/resin.sh start -server a
 */
public class StartCommand extends AbstractStartCommand
{
  private static Logger _log;
  private static L10N _L;

  @Override
  public String getDescription()
  {
    return "starts a Resin server";
  }
  
  @Override
  public boolean isStart()
  {
    return true;
  }

  @Override
  public int doCommand(WatchdogArgs args, WatchdogClient client)
    throws BootArgumentException
  {
    try {
      client.startWatchdog(args.getArgv(), true);

      System.out.println(L().l("Resin/{0} started{1} with watchdog at {2}:{3}",
                               VersionFactory.getVersion(),
                               getServerUsageArg(args, client.getId()),
                               client.getWatchdogAddress(),
                               client.getWatchdogPort()));
    } catch (Exception e) {
      String eMsg;

      if (e instanceof ConfigException)
        eMsg = e.getMessage();
      else
        eMsg = e.toString();

      System.out.println(L().l(
        "Resin/{0} can't start -server '{1}' for watchdog at {2}:{3}.\n  {4}",
        VersionFactory.getVersion(),
        client.getId(),
        client.getWatchdogAddress(),
        client.getWatchdogPort(),
        eMsg));

      log().log(Level.FINE, e.toString(), e);

      System.exit(1);
    }

    return 0;
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }

  private static Logger log()
  {
    if (_log == null)
      _log = Logger.getLogger(StartCommand.class.getName());

    return _log;
  }

  private static L10N L()
  {
    if (_L == null)
      _L = new L10N(StartCommand.class);

    return _L;
  }
}
