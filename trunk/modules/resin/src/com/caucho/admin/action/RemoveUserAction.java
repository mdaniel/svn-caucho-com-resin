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

package com.caucho.admin.action;

import com.caucho.security.AdminAuthenticator;
import com.caucho.security.PasswordUser;
import com.caucho.util.L10N;

import java.util.logging.Logger;

public class RemoveUserAction implements AdminAction
{
  private static final Logger log
    = Logger.getLogger(RemoveUserAction.class.getName());

  private static final L10N L = new L10N(RemoveUserAction.class);

  private AdminAuthenticator _adminAuth;
  private String _user;

  public RemoveUserAction(AdminAuthenticator adminAuth, String user)
  {
    _adminAuth = adminAuth;
    _user = user;
  }

  public PasswordUser execute()
  {
    /*
    PasswordUser user = _adminAuth.getUserMap().get(_user);

    _adminAuth.removeUser(_user);

    return user;
    */
    return null;
  }
}
