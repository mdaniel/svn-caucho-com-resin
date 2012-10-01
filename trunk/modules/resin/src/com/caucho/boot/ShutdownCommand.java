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
import com.caucho.util.L10N;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Command to shutdown Resin server
 * bin/resin.sh shutdown -server a
 */
public class ShutdownCommand extends AbstractStopCommand
{
  private static final L10N L = new L10N(ShutdownCommand.class);
  private static final Logger log
    = Logger.getLogger(ShutdownCommand.class.getName());

  @Override
  public String getDescription()
  {
    return "shuts down the watchdog and all its managed Resin servers";
  }

  @Override
  public int doCommand(WatchdogArgs args, WatchdogClient client)
    throws BootArgumentException
  {
    try {
      client.shutdown();

      System.out.println(L.l("Resin/{0} shutdown watchdog at {1}:{2}",
                               VersionFactory.getVersion(),
                               client.getWatchdogAddress(),
                               client.getWatchdogPort()));
    } catch (Exception e) {
      System.out.println(L.l(
        "Resin/{0} can't shutdown watchdog at {1}:{2}.\n{3}",
        VersionFactory.getVersion(),
        client.getWatchdogAddress(),
        client.getWatchdogPort(),
        e.toString()));

      log.log(Level.FINE, e.toString(), e);

      System.exit(1);
    }

    return 0;
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }
}