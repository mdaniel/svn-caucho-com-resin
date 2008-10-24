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

package com.caucho.server.security;

import com.caucho.webbeans.component.*;
import com.caucho.webbeans.manager.*;

import javax.annotation.PostConstruct;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.webbeans.*;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Used to authenticate users in a servlet request.  AbstractLogin handles
 * the different login types like "basic" or "form".  Normally, a Login
 * will delegate the actual authentication to a ServletAuthenticator.
 *
 * <p>The Login is primarily responsible for extracting the credentials
 * from the request (typically username and password) and passing those
 * to the ServletAuthenticator.
 *
 * <p>The Servlet API calls the Login in two contexts: directly from
 * <code>ServletRequest.getUserPrincipal()</code>, and during 
 * security checking.   When called from the Servlet API, the login class
 * can't change the response.  In other words, if an application
 * calls getUserPrincipal(), the Login class can't return a forbidden
 * error page.  When the servlet engine calls authenticate(), the login class
 * can return an error page (or forward internally.)
 *
 * <p>Normally, Login implementations will defer the actual authentication
 * to a ServletAuthenticator class.  That way, both "basic" and "form" login
 * can use the same JdbcAuthenticator.  Some applications, like SSL
 * client certificate login, may want to combine the Login and authentication
 * into one class.
 *
 * <p>Login instances are configured through bean introspection.  Adding
 * a public <code>setFoo(String foo)</code> method will be configured with
 * the following login-config:
 *
 * <code><pre>
 * &lt;login-config>
 *   &lt;class-name>test.CustomLogin&lt/class-name>
 *   &lt;foo>bar&lt;/bar>
 * &lt;/login-config>
 * </pre></code>
 *
 * @since Resin 2.0.2
 */
public abstract class AbstractLogin implements LoginFilter {
  protected final static Logger log
    = Logger.getLogger(AbstractLogin.class.getName());

  /**
   * The configured authenticator for the login.  Implementing classes will
   * typically delegate calls to the authenticator after extracting the
   * username and password.
   */
  protected ServletAuthenticator _auth;

  /**
   * Sets the authenticator.
   */
  public void setAuthenticator(ServletAuthenticator auth)
  {
    _auth = auth;
  }

  /**
   * Gets the authenticator.
   */
  public ServletAuthenticator getAuthenticator()
  {
    if (_auth == null) {
      try {
	WebBeansContainer webBeans = WebBeansContainer.create();

	_auth = webBeans.getInstanceByType(ServletAuthenticator.class);
      } catch (Exception e) {
        log.log(Level.FINER, e.toString(), e);
      }

      if (_auth == null)
        _auth = new NullAuthenticator();

      if (log.isLoggable(Level.FINE))
	log.fine(toString() + " using " + _auth);
    }
    
    return _auth;
  }
  
  /**
   * Initialize the login.  <code>init()</code> will be called after all
   * the bean parameters have been set.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
  }

  /**
   * Returns the authentication type.  <code>getAuthType</code> is called
   * by <code>HttpServletRequest.getAuthType</code>.
   */
  public String getAuthType()
  {
    return "none";
  }
  
  /**
   * Logs a user in.  The authenticate method is called during the
   * security check.  If the user does not exist, <code>authenticate</code>
   * sets the reponse error page and returns null.
   *
   * @param request servlet request
   * @param response servlet response for a failed authentication.
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal authenticate(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application)
    throws ServletException, IOException
  {
    // Most login classes will extract the user and password (or some other
    // credentials) from the request and call auth.login.
    Principal user = getUserPrincipal(request, response, application);

    if (user == null)
      response.sendError(HttpServletResponse.SC_FORBIDDEN);

    return user;
  }
  
  /**
   * Returns the Principal associated with the current request.
   * getUserPrincipal is called in response to the Request.getUserPrincipal
   * call.  Login.getUserPrincipal can't modify the response or return
   * an error page.
   *
   * <p/>authenticate is used for the security checks.
   *
   * @param request servlet request
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException
  {
    return getAuthenticator().getUserPrincipal(request, response, application);
  }
  
  /**
   * Returns true if the current user plays the named role.
   * <code>isUserInRole</code> is called in response to the
   * <code>HttpServletRequest.isUserInRole</code> call.
   *
   * @param request servlet request
   * @param application servlet application
   *
   * @return the logged in principal on success, null on failure.
   */
  public boolean isUserInRole(HttpServletRequest request,
                              HttpServletResponse response,
                              ServletContext application,
                              Principal user, String role)
    throws ServletException
  {
    return getAuthenticator().isUserInRole(request, response,
                                           application, user, role);
  }
  
  /**
   * Logs the user out from the given request.
   *
   * <p>Since there is no servlet API for logout, this must be called
   * directly from user code.  Resin stores the web-app's login object
   * in the ServletContext attribute "caucho.login".
   */
  public void logout(HttpServletRequest request,
                     HttpServletResponse response,
                     ServletContext application)
    throws ServletException
  {
    Principal principal = getUserPrincipal(request, response, application);

    if (principal != null)
      getAuthenticator().logout(application,
				null,
				request.getRequestedSessionId(),
                                principal);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
