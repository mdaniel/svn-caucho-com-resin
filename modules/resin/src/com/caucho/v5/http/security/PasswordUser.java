/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.security;

import java.security.Principal;
import java.util.logging.Logger;

/**
 * PasswordUser is used by PasswordAuthenticator implementations.
 */
public class PasswordUser
{
  private static final Logger log =
    Logger.getLogger(PasswordUser.class.getName());

  private final Principal _principal;
  private final char []_password;
  
  private final boolean _isDisabled;
  private final boolean _isAnonymous;
  private final String []_roles;

  public PasswordUser(Principal principal,
                      char []password,
                      boolean isDisabled,
                      boolean isAnonymous,
                      String []roles)
  {
    _principal = principal;
    _password = password;
    
    _isDisabled = isDisabled;
    _isAnonymous = isAnonymous;

    _roles = roles;
  }

  public PasswordUser(Principal principal,
                      char []password,
                      String []roles)
  {
    this(principal, password, false, false, roles);
  }

  public PasswordUser(String user,
                      char []password,
                      String []roles)
  {
    this(new BasicPrincipal(user), password, false, false, roles);
  }

  public PasswordUser(String user,
                      char []password)
  {
    this(new BasicPrincipal(user), password,
         false, false, new String[] { "user" });
  }

  public PasswordUser(String user, String password)
  {
    this(user, password.toCharArray());
  }

  /**
   * Returns the logged-in user principal
   */
  public Principal getPrincipal()
  {
    return _principal;
  }

  /**
   * Returns true if the user is disabled
   */
  public boolean isDisabled()
  {
    return _isDisabled;
  }

  /**
   * Returns true if the user is anonymous, i.e. no password
   */
  public boolean isAnonymous()
  {
    return _isAnonymous;
  }

  /**
   * Returns the password
   */
  public char []getPassword()
  {
    return _password;
  }

  /**
   * Clears the password
   */
  public void clearPassword()
  {
    for (int i = _password.length - 1; i >= 0; i--)
      _password[i] = 0;
  }

  /**
   * Returns the user's roles
   */
  public String []getRoles()
  {
    return _roles;
  }

  /**
   * Returns true if the user is in one of the roles
   */
  public boolean isUserInRole(String testRole)
  {
    if (_roles == null)
      return false;

    for (String role : _roles) {
      if (role.equals(testRole))
        return true;
    }

    return false;
  }

  /**
   * Creates a copy
   */
  public PasswordUser copy()
  {
    return new PasswordUser(_principal, _password,
                            _isDisabled, _isAnonymous,
                            _roles);
  }

  public String toString()
  {
    if (isDisabled())
      return getClass().getSimpleName() + "[" + _principal + ",disabled]";
    else
      return getClass().getSimpleName() + "[" + _principal + "]";
  }
}
