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

import com.caucho.server.admin.AddUserQueryReply;
import com.caucho.server.admin.ManagerClient;
import com.caucho.util.L10N;

import java.util.Arrays;

public class UserAddCommand extends AbstractManagementCommand
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
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    final String user = args.getArg("-u");

    if (user == null) {
      usage(false);

      return 3;
    }

    String passwordString = args.getArg("-p");
    char []password = null;

    if (passwordString != null)
      password = passwordString.toCharArray();

    while (password == null) {
      char []passwordEntry = System.console().readPassword("%s",
                                                           "enter password:");
      if (passwordEntry.length <= 8) {
        System.out.println("password must be greater than 8 characters");

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
    return 0;
  }

  @Override
  public boolean isDefaultArgsAccepted()
  {
    return true;
  }

  @Override
  public String getUsageArgs()
  {
    return " [<role>] [<role>] ...";
  }
}
