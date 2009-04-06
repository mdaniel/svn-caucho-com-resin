/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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
 * @author Scott Ferguson
 */

package com.caucho.security;

import com.caucho.util.CharBuffer;
import com.caucho.rewrite.RequestPredicate;
import com.caucho.server.connection.CauchoRequest;

import java.security.Principal;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class IfRole implements RequestPredicate {
  private String []_roles = new String[0];

  public void addName(String role)
  {
    String []newRoles = new String[_roles.length + 1];
    System.arraycopy(_roles, 0, newRoles, 0, _roles.length);
    newRoles[_roles.length] = role;
    _roles = newRoles;
  }

  /**
   * Returns true if the user is authorized for the resource.
   */
  public boolean isMatch(HttpServletRequest request)
  {
    Principal user = request.getUserPrincipal();

    if (user == null)
      return false;
    
    for (String role : _roles) {
      if (role.equals("*"))
        return true;

      if (request.isUserInRole(role))
        return true;
    }

    return false;
  }

  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    for (int i = 0; i < _roles.length; i++) {
      if (i != 0)
        sb.append(',');
      sb.append(_roles[i]);
    }
    sb.append("]");
    
    return sb.toString();
  }
}
