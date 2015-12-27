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

import java.util.Arrays;

import com.caucho.v5.cli.baratine.ArgsCli;
import com.caucho.v5.env.shutdown.ExitCode;
import com.caucho.v5.server.admin.AddUserQueryReply;
import com.caucho.v5.server.admin.ManagerClientApi;
import com.caucho.v5.server.config.ServerConfigBoot;
import com.caucho.v5.util.L10N;

public class UserAddCommand extends ManagementCommandBase
{
  private static final L10N L = new L10N(UserAddCommand.class);

  @Override
  protected void initBootOptions()
  {
    addValueOption("u", "new user name", "specifies name for a new user.");
    addValueOption("p", "new user password", "specifies password for a new user.");
    
    super.initBootOptions();
  }

  @Override
  public String getDescription()
  {
    return "adds an administration user and password";
  }

  @Override
  public ExitCode doCommand(ArgsCli args,
                            ServerConfigBoot server,
                            ManagerClientApi managerClient)
  {
    final String user = args.getArg("-u");

    if (user == null) {
      System.err.println(usage(args));

      return ExitCode.FAIL;
    }

    String passwordString = args.getArg("-p");
    char []password = null;

    if (passwordString != null)
      password = passwordString.toCharArray();

    while (password == null) {
      char []passwordEntry = System.console().readPassword("%s",
                                                           "enter password:");
      if (passwordEntry.length <= 8) {
        System.out.println("password must be greater then 8 characters");

        continue;
      }

      char []passwordConfirm = System.console().readPassword("%s",
                                                             "re-enter password:");

      if (Arrays.equals(passwordEntry, passwordConfirm))
        password = passwordEntry;
      else
        System.out.println("passwords do not match");
    }

    String []roles = args.getDefaultArgs();

    AddUserQueryReply result = managerClient.addUser(user, password, roles);

    System.out.println(L.l("user `{0}' added",
                           result.getUser().getName()));
    return ExitCode.OK;
  }

  @Override
  public boolean isTailArgsAccepted()
  {
    return true;
  }

  @Override
  public String getUsageTailArgs()
  {
    return " [<role>] [<role>] ...";
  }
}
