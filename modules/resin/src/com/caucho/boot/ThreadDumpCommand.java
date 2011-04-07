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

import com.caucho.server.admin.ManagerClient;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class ThreadDumpCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(ThreadDumpCommand.class);

  @Override
  public void doCommand(WatchdogArgs args, WatchdogClient client)
  {
    ManagerClient manager = getManagerClient(args, client);

    String dump = manager.doThreadDump();

    String fileName = args.getArg("-file");

    if (fileName == null) {
      System.out.println(dump);

      return;
    }

    Writer out = null;

    try {
      File file = new File(fileName);
      out = new FileWriter(file);
      out.write(dump);
      out.flush();

      System.out.println("Thread dump was written to `"
                         + file.getCanonicalPath()
                         + "'");
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (out != null)
        IoUtil.close(out);
    }
  }

  @Override
  public void usage()
  {
    System.err.println(L.l(
      "usage: java -jar resin.jar [-conf <file>] thread-dump -user <user> -password <password> [-file <file>]"));
    System.err.println(L.l(""));
    System.err.println(L.l("description:"));
    System.err.println(L.l("   prints a thread dump taken on remote server"));
    System.err.println(L.l(""));
    System.err.println(L.l("options:"));
    System.err.println(L.l(
      "   -file <file>          : file name where thread dump will be stored"));
  }
}
