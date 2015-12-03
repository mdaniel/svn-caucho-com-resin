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

import com.caucho.server.admin.JsonQueryReply;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.util.IoUtil;
import com.caucho.util.L10N;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

public class JmxDumpCommand extends JmxCommand
{
  private static final L10N L = new L10N(JmxDumpCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("file", "file", "file where the JMX dump will be saved");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "dumps all JMX values from a Resin server";
  }

  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    JsonQueryReply result = managerClient.doJmxDump();

    JsonQueryReply queryResult = result;

    String fileName = args.getArg("-file");

    if (fileName == null) {
      System.out.println(queryResult.getValue());
      return 0;
    }

    Writer out = null;

    try {
      File file = new File(fileName);
      out = new FileWriter(file);
      out.write(queryResult.getValue());
      out.flush();

      System.out.println(L.l("JMX dump was written to '{0}'",
                             file.getCanonicalPath()));

      return 0;
    } catch (IOException e) {
      e.printStackTrace();
      return 4;
    } finally {
      IoUtil.close(out);
    }
  }
}
