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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.security;

import java.io.IOException;
import java.security.Principal;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.network.listen.ConnectionTcp;

/**
 * Backwards compatibility
 *
 * @since Resin 2.0.2
 * @deprecated
 * @see com.caucho.security.LoginBase
 */
public abstract class AbstractLogin extends LoginBase {
  /**
   * Authentication
   */
  @Override
  public Principal getUserPrincipalImpl(HttpServletRequest request)
  {
    ServletContext app = request.getServletContext();

    HttpServletResponse response = null;
    
    return getUserPrincipal(request, response, app);
  }

  protected Principal getUserPrincipal(HttpServletRequest request,
                                       HttpServletResponse response,
                                       ServletContext app)
  {
    return null;
  }
  
  /**
   * Authentication
   */
  @Override
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         boolean isFail)
  {
    try {
      ServletContext app = request.getServletContext();
    
      return authenticate(request, response, app);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected Principal authenticate(HttpServletRequest request,
                                   HttpServletResponse response,
                                   ServletContext app)
    throws ServletException, IOException
  {
    return null;
  }

  /**
   * Returns true if the current user plays the named role.
   * <code>isUserInRole</code> is called in response to the
   * <code>HttpServletRequest.isUserInRole</code> call.
   *
   * @param user the logged in user
   * @param role the role to check
   *
   * @return true if the user plays the named role
   */
  @Override
  public boolean isUserInRole(Principal user, String role)
  {
    RequestCaucho request
      = (RequestCaucho) ConnectionTcp.getCurrentRequest();

    return isUserInRole(request,
                        null, // request.getResponse(),
                        request.getServletContext(),
                        user,
                        role);
  }

  protected boolean isUserInRole(HttpServletRequest request,
                                 HttpServletResponse response,
                                 ServletContext app,
                                 Principal user, String role)
  {
    return false;
  }
}

