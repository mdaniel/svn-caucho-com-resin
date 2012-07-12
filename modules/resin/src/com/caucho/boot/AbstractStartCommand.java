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

import java.util.ArrayList;


public abstract class AbstractStartCommand extends AbstractBootCommand
{
  protected AbstractStartCommand()
  {
    addFlagOption("verbose", "log command-line and environment information");
    addFlagOption("preview", "run as a preview (staging) server");
    addFlagOption("elastic", "join a cluster as an elastic server (pro)");
    
    addValueOption("data-directory", "dir", "override the working directory");
    addValueOption("cluster", "id", "cluster to join as an elastic server (pro)");
    addValueOption("root-directory", "dir", "set the root directory");
    addValueOption("log-directory", "dir", "set the log directory");
    addValueOption("server", "id", "select a configured server");
    addValueOption("stage", "stage", "select a configuration stage (production, preview)");

    addIntValueOption("watchdog-port", "port", "set watchdog port to listen to");
    addIntValueOption("debug-port", "port", "listen to a JVM debug port");
    addIntValueOption("jmx-port", "port", "listen to an unauthenticated JMX port");

    /*
    _options.add("-verbose");
    _options.add("--verbose");
    _options.add("-preview");
    _options.add("--preview");

    _valueKeys.add("-conf");
    _valueKeys.add("--conf");
    _valueKeys.add("-data-directory");
    _valueKeys.add("--data-directory");
    _valueKeys.add("-join");
    _valueKeys.add("--join");
    _valueKeys.add("-join-cluster");
    _valueKeys.add("--join--cluster");
    _valueKeys.add("-log-directory");
    _valueKeys.add("--log-directory");
    _valueKeys.add("-resin-home");
    _valueKeys.add("--resin-home");
    _valueKeys.add("-root-directory");
    _valueKeys.add("--root-directory");
    _valueKeys.add("-server");
    _valueKeys.add("--server");
    _valueKeys.add("-stage");
    _valueKeys.add("--stage");
    _valueKeys.add("-watchdog-port");
    _valueKeys.add("--watchdog-port");
    _valueKeys.add("-debug-port");
    _valueKeys.add("--debug-port");
    _valueKeys.add("-jmx-port");
    _valueKeys.add("--jmx-port");

    _intValueKeys.add("-watchdog-port");
    _intValueKeys.add("--watchdog-port");
    _intValueKeys.add("-debug-port");
    _intValueKeys.add("--debug-port");
    _intValueKeys.add("-jmx-port");
    _intValueKeys.add("--jmx-port");
    */
  }

  @Override
  protected WatchdogClient findLocalClient(ResinBoot boot, WatchdogArgs args)
  {
    if (args.isElasticServer())
      return super.findLocalClient(boot, args);
    else
      return findUniqueLocalClient(boot, args);
  }

  @Override
  protected WatchdogClient findWatchdogClient(ResinBoot boot, WatchdogArgs args)
  {
    // server/6e09
    if (args.isElasticServer()) {
      return super.findWatchdogClient(boot, args);
    }
    else {
      return null;
    }
  }

  protected String getServerUsageArg(WatchdogArgs args, String clientId)
  {
    if (args.getServerId() != null)
      return " -server '" + args.getServerId() + "'";
    else if (args.isElasticServer())
      return " -server '" + args.getElasticServerId() + "'";
    else
      return " -server '" + clientId + "'";
  }

  @Override
  public boolean isRetry()
  {
    return true;
  }
}
