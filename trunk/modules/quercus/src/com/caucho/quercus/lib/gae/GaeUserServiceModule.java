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
import com.caucho.quercus.env.ArrayValue;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.module.AbstractQuercusModule;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GaeUserServiceModule extends AbstractQuercusModule {
  @Override
  public String []getLoadedExtensions()
  {
    return new String[] { "gae_users" };
  }

  public static String gae_users_create_login_url(Env env,
                                                  String destinationUrl,
                                                  @Optional String authDomain,
                                                  @Optional String federatedIdentity,
                                                  @Optional Value attributesRequest ) {
    Set<String> attributeSet = null;

    if (! attributesRequest.isDefault()) {
      attributeSet = new HashSet<String>();

      ArrayValue array = attributesRequest.toArrayValue(env);

      for (Map.Entry<Value,Value> entrySet : array.entrySet()) {
        attributeSet.add(entrySet.getValue().toString());
      }
    }

    return GaeUserService.createLoginURL(destinationUrl,
                                         authDomain,
                                         federatedIdentity,
                                         attributeSet);
  }

  public static String gae_users_create_logout_url(Env env,
                                                   String destinationUrl,
                                                   @Optional String authDomain) {
    return GaeUserService.createLogoutURL(destinationUrl, authDomain);
  }

  public static GaeUser gae_users_get_current_user(Env env) {
    return GaeUserService.getCurrentUser();
  }

  public static boolean gae_users_is_user_admin(Env env) {
    return GaeUserService.isUserAdmin();
  }

  public static boolean gae_users_is_user_logged_in(Env env) {
    return GaeUserService.isUserLoggedIn();
  }
}
