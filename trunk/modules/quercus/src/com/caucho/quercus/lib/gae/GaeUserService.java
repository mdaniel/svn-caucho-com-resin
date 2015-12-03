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
 * @author Nam Nguyen
 */

package com.caucho.quercus.lib.gae;

import com.caucho.quercus.annotation.Optional;
import com.google.appengine.api.users.User;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

import java.util.Set;

public class GaeUserService
{
  private static final UserService USER_SERVICE
    = UserServiceFactory.getUserService();

  protected static UserService getUserService() {
    return USER_SERVICE;
  }

  public static String createLoginURL(String destinationUrl,
                                      @Optional String authDomain,
                                      @Optional String federatedIdentity,
                                      @Optional Set<String> attributesRequest)
  {
    return getUserService().createLoginURL(destinationUrl,
                                           authDomain,
                                           federatedIdentity,
                                           attributesRequest);
  }

  public static String createLogoutURL(String destinationUrl,
                                       @Optional String authDomain)
  {
    return getUserService().createLogoutURL(destinationUrl, authDomain);
  }

  public static GaeUser getCurrentUser()
  {
    User user = getUserService().getCurrentUser();

    if (user == null) {
      return null;
    }
    else {
      return new GaeUser(user);
    }
  }

  public static boolean isUserAdmin()
  {
    try {
      return getUserService().isUserAdmin();
    }
    catch (IllegalStateException e) {
      // "The current user is not logged in."
      return false;
    }
  }

  public static boolean isUserLoggedIn()
  {
    return getUserService().isUserLoggedIn();
  }
}

