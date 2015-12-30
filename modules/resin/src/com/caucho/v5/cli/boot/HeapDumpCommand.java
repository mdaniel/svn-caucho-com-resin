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

import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.server.admin.ManagerClientApi;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;

/**
 * Makes the remote server produce heap dump. Heap dump is written to a file
 * local to a remote server as it can be big.
 */
public class HeapDumpCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(HeapDumpCommand.class);

  @Override
  protected void initBootOptions()
  {
    addFlagOption("raw", "creates a JVM hprof file");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "produces a JVM memory heap dump";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    boolean isRaw = args.hasOption("-raw");

    String result = managerClient.doHeapDump(isRaw);

    System.out.println(result);

    return ExitCode.OK;
  }
}
