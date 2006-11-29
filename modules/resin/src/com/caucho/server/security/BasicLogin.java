/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.security;

import com.caucho.util.Base64;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;

/**
 * Implements the "basic" auth-method.  Basic uses the
 * HTTP authentication with WWW-Authenticate and SC_UNAUTHORIZE.
 */
public class BasicLogin extends AbstractLogin {
  protected String _realm;
  
  /**
   * Sets the login realm.
   */
  public void setRealmName(String realm)
  {
    _realm = realm;
  }

  /**
   * Gets the realm.
   */
  public String getRealmName()
  {
    return _realm;
  }

  /**
   * Returns the authentication type.
   */
  public String getAuthType()
  {
    return "Basic";
  }
  
  /**
   * Logs a user in with a user name and a password.  Basic authentication
   * extracts the user and password from the authorization header.  If
   * the user/password is missing, authenticate will send a basic challenge.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal authenticate(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application)
    throws ServletException, IOException
  {
    Principal user;

    ServletAuthenticator auth = getAuthenticator();
    
    // If the user is already logged-in, return the user
    user = auth.getUserPrincipal(request, response, application);
    if (user != null)
      return user;
    
    user = getBasicPrincipal(request, response, application);

    if (user != null)
      return user;

    sendBasicChallenge(response);
    
    return null;
  }
  
  /**
   * Returns the current user with the user name and password.
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException
  {
    ServletAuthenticator auth = getAuthenticator();
    
    Principal user = auth.getUserPrincipal(request, response, application);

    if (user != null)
      return user;
    
    return getBasicPrincipal(request, response, application);
  }

  /**
   * Sends a challenge for basic authentication.
   */
  protected void sendBasicChallenge(HttpServletResponse res)
    throws ServletException, IOException
  {
    String realm = getRealmName();
    if (realm == null)
      realm = "resin";

    res.setHeader("WWW-Authenticate", "Basic realm=\"" + realm + "\"");
    res.sendError(res.SC_UNAUTHORIZED);
  }

  /**
   * Returns the principal from a basic authentication
   *
   * @param auth the authenticator for this application.
   */
  protected Principal getBasicPrincipal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        ServletContext application)
    throws ServletException
  {
    Principal principal;

    // Principal from runner
    principal = (Principal) request.getAttribute(AbstractAuthenticator.LOGIN_NAME);
    if (principal != null)
      return principal;
      
    String value = request.getHeader("authorization");
    if (value == null)
      return null;
    
    int i = value.indexOf(' ');
    if (i <= 0)
      return null;

    String decoded = Base64.decode(value.substring(i + 1));

    int index = decoded.indexOf(':');
    if (index < 0)
      return null;

    String user = decoded.substring(0, index);
    String password = decoded.substring(index + 1);

    ServletAuthenticator auth = getAuthenticator();
    principal = auth.login(request, response, application, user, password);

    if (log.isLoggable(Level.FINE))
      log.fine("basic: " + user + " -> " + principal); 

    return principal;
  }
}
