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
 */

package com.caucho.cli.boot;

import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;

public class ScoreboardCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(ScoreboardCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("type", "resin", "scoreboard report type");
    addValueOption("greedy", "true", 
                   "threads can not be in more than one scoreboard");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "produces a concise thread activity report for groups of related threads";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    String greedy = args.getArg("-greedy");
    String type = args.getArg("-type");
    
    boolean isGreedy = true;
    if ("no".equals(greedy) || "false".equals(greedy))
      isGreedy = false;
    
    if(type == null)
      type = "resin";
    
    StringQueryReply result = managerClient.scoreboard(type, isGreedy);
    System.out.println(result.getValue());

    return ExitCode.OK;
  }
}
