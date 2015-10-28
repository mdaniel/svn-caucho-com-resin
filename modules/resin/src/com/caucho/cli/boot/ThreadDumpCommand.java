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

import java.io.IOException;

import com.caucho.server.admin.ManagerClientApi;
import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

public class ThreadDumpCommand extends ManagementCommandBase
{
  @Override
  protected void initBootOptions()
  {
    addValueOption("file", "file", "file name where thread dump will be stored");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "displays a JVM thread dump summary";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    String result = managerClient.doThreadDump(false);

    String fileName = args.getArg("-file");

    if (fileName == null) {
      System.out.println(result);

      return ExitCode.OK;
    }

    Path path = Vfs.lookup(fileName);

    try (WriteStream out = path.openWrite()) {
      out.print(result);

      System.out.println("Thread dump was written to `"
                         + path.getFullPath()
                         + "'");

      return ExitCode.OK;
    } catch (IOException e) {
      e.printStackTrace();

      return ExitCode.EXIT_1;
    }
  }
}
