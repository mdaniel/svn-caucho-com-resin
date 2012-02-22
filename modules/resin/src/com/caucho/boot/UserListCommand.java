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

import com.caucho.server.admin.ListUsersQueryReply;
import com.caucho.server.admin.ManagerClient;
import com.caucho.server.admin.UserQueryReply;
import com.caucho.util.L10N;

public class UserListCommand extends AbstractManagementCommand
{
  private static final L10N L = new L10N(UserListCommand.class);

  @Override
  public String getDescription()
  {
    return "lists the administration users";
  }
  
  @Override
  public int doCommand(WatchdogArgs args,
                       WatchdogClient client,
                       ManagerClient managerClient)
  {
    ListUsersQueryReply result = managerClient.listUsers();

    if (result.getUsers().length == 0) {
      System.out.println("no users found");

      return 0;
    }

    for (UserQueryReply.User user : result.getUsers()) {
      System.out.print(user.getName());

      String []roles = user.getRoles();
      if (roles == null || roles.length == 0) {
        System.out.println();

        continue;
      }

      System.out.print(": ");
      for (int i = 0; i < roles.length;i++) {
        System.out.print(roles[i]);

        if (i + 1 < roles.length) {
          System.out.print(", ");
        }
      }
      System.out.println();
    }

    return 0;
  }
}
