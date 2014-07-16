/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

import com.caucho.cli.baratine.ArgsCli;
import com.caucho.env.shutdown.ExitCode;
import com.caucho.server.admin.ManagerClientApi;
import com.caucho.server.admin.StringQueryReply;
import com.caucho.server.config.ServerConfigBoot;
import com.caucho.util.L10N;

public class ProfileCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(ProfileCommand.class);
  
  @Override
  protected void initBootOptions()
  {
    addValueOption("active-time", "time", "profiling time span, defualts to 5 sec");
    addValueOption("sampling-rate", "ms", "the sample rate defaults to 10ms");
    addValueOption("depth", "count", "the stack trace depth (default 16)");
    addFlagOption("start", "starts profiling");
    addFlagOption("stop", "finishes profiling");
    
    super.initBootOptions();
  }
  
  @Override
  public String getDescription()
  {
    return "gathers a CPU profile of a running server";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    long activeTime = 5 * 1000; // 5 seconds
    String activeTimeArg = args.getArg("-active-time");

    if (activeTimeArg != null)
      activeTime = Long.parseLong(activeTimeArg);

    long period = 10;// sampling period
    String periodArg = args.getArg("-sampling-rate");
    if (periodArg != null)
      period = Long.parseLong(periodArg);

    int depth = args.getArgInt("depth", 16);
    
    boolean isStart = args.getArgFlag("start");
    boolean isEnd = args.getArgFlag("stop");

    StringQueryReply result = managerClient.profile(activeTime,
                                                     period,
                                                     depth);

    System.out.println(result.getValue());

    return ExitCode.OK;
  }
}