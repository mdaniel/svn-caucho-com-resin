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

package com.caucho.admin.action;

import com.caucho.security.AdminAuthenticator;
import com.caucho.v5.http.security.PasswordUser2;
import com.caucho.v5.util.L10N;

import java.util.Hashtable;
import java.util.logging.Logger;

public class ListUsersAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(ListUsersAction.class.getName());

  private static final L10N L = new L10N(ListUsersAction.class);

  private final AdminAuthenticator _adminAuth;

  public ListUsersAction(AdminAuthenticator adminAuth)
  {
    _adminAuth = adminAuth;
  }

  public Hashtable<String,PasswordUser2> execute()
  {
    /*
    Hashtable<String,PasswordUser> userMap = _adminAuth.getUserMap();

    return userMap;
    */
    return null;
  }
}
