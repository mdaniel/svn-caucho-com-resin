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

package com.caucho.v5.cli.boot;

import com.caucho.v5.cli.daemon.ArgsDaemon;
import com.caucho.v5.cli.server.BootArgumentException;
import com.caucho.v5.cli.server.StartCommandBase;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.config.ConfigBoot;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.server.watchdog.WatchdogService;

/**
 * Command to start Resin server in gui mode
 * bin/resin.sh gui -server a
 */
public class StartCommandGui extends StartCommandBase
{
  @Override
  public String getDescription()
  {
    return "starts a Resin server with a GUI control";
  }

  @Override
  public ExitCode doCommand(ArgsDaemon args, 
                       ConfigBoot boot,
                       ServerConfigBoot watchdogServer,
                       WatchdogService watchdog)
    throws BootArgumentException
  {
    try {
      throw new UnsupportedOperationException(getClass().getName());
      // return client.startGui(this);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /*
  @Override
  public boolean isRetry()
  {
    return true;
  }
  */
}