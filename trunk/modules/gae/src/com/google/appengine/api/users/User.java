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

package com.google.appengine.api.users;

import java.io.Serializable;

public final class User implements Serializable, Comparable<User>
{
  private final String _email;
  private final String _authDomain;
  private final String _userId;

  private final String _federatedIdentity;

  public User(String email, String authDomain)
  {
    this(email, authDomain, null, null);
  }

  public User(String email, String authDomain, String userId)
  {
    this(email, authDomain, userId, null);
  }

  public User(String email, String authDomain, String userId, String federatedIdentity)
  {
    _email = email;
    _authDomain = authDomain;
    _userId = userId;

    _federatedIdentity = federatedIdentity;
  }

  public String getEmail()
  {
    return _email;
  }

  public String getAuthDomain()
  {
    return _authDomain;
  }

  public String getUserId()
  {
    return _userId;
  }

  public String getFederatedIdentity()
  {
    return _federatedIdentity;
  }

  public String getNickname()
  {
    return getEmail();
  }

  @Override
  public int compareTo(User user)
  {
    if (_email == user.getEmail()) {
      return 0;
    }

    return _email.compareTo(user.getEmail());
  }
}
