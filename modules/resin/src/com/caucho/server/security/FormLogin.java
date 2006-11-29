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

import com.caucho.config.ConfigException;
import com.caucho.server.connection.CauchoResponse;
import com.caucho.server.webapp.Application;
import com.caucho.util.L10N;

import javax.annotation.PostConstruct;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.security.Principal;
import java.util.logging.Level;

/**
 * Used to authenticate users in a servlet request.  Applications will
 * implement the Authenticator interface with a bean for authentication.
 *
 * @since Resin 2.0.2
 */
public class FormLogin extends AbstractLogin {
  static L10N L = new L10N(FormLogin.class);
  
  public static final String LOGIN_SAVED_PATH = "com.caucho.servlet.login.path";
  public static final String LOGIN_SAVED_QUERY = "com.caucho.servlet.login.query";
  
  protected String _loginPage;
  protected String _errorPage;
  protected boolean _internalForward;
  protected boolean _formURIPriority;
  
  /**
   * Sets the login page.
   */
  public void setFormLoginPage(String formLoginPage)
    throws ConfigException
  {
    _loginPage = formLoginPage;

    int colon = formLoginPage.indexOf(':');
    int slash = formLoginPage.indexOf('/');

    if (colon > 0 && colon < slash) {
    }
    else if (slash != 0)
      throw new ConfigException(L.l("form-login-page `{0}' must start with '/'.  The form-login-page is relative to the web-app root.", formLoginPage));
  }
  
  /**
   * Gets the login page.
   */
  public String getFormLoginPage()
  {
    return _loginPage;
  }
  
  /**
   * Sets the error page.
   */
  public void setFormErrorPage(String formErrorPage)
    throws ConfigException
  {
    _errorPage = formErrorPage;

    if (! formErrorPage.startsWith("/"))
      throw new ConfigException(L.l("form-error-page `{0}' must start with '/'.  The form-error-page is relative to the web-app root.", formErrorPage));
  }
  
  /**
   * Gets the error page.
   */
  public String getFormErrorPage()
  {
    return _errorPage;
  }

  /**
   * Returns true if a successful login allows an internal forward
   * instead of a redirect.
   */
  public boolean getInternalForward()
  {
    return _internalForward;
  }

  /**
   * Set true if a successful login allows an internal forward
   * instead of a redirect.
   */
  public void setInternalForward(boolean internalForward)
  {
    _internalForward = internalForward;
  }

  /**
   * Returns true if the form's j_uri has priority over the saved
   * URL.
   */
  public boolean getFormURIPriority()
  {
    return _formURIPriority;
  }

  /**
   * True if the form's j_uri has priority over the saved URL.
   */
  public void setFormURIPriority(boolean formPriority)
  {
    _formURIPriority = formPriority;
  }

  /**
   * Initialize
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    super.init();
    
    if (_errorPage == null)
      _errorPage = _loginPage;
    
    if (_loginPage == null)
      _loginPage = _errorPage;

    if (_loginPage == null)
      throw new ServletException("FormLogin needs an form-login-page");
  }

  /**
   * Returns the authentication type.
   */
  public String getAuthType()
  {
    return "Form";
  }

  /**
   * Logs a user in with a user name and a password.
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
    Principal user = getUserPrincipal(request, response, application);

    if (user != null)
      return user;

    String path = request.getServletPath();
    if (path == null)
      path = request.getPathInfo();
    else if (request.getPathInfo() != null)
      path = path + request.getPathInfo();

    if (path.equals("")) {
      // Forward?
      path = request.getContextPath() + "/";
      response.sendRedirect(response.encodeRedirectURL(path));
      return null;
    }

    Application app = (Application) application;
      
    String uri = request.getRequestURI();

    if (path.endsWith("/j_security_check")) {
      RequestDispatcher disp;
      disp = application.getNamedDispatcher("j_security_check");

      if (disp == null)
        throw new ServletException(L.l("j_security_check servlet must be defined to use form-based login."));
      
      disp.forward(request, response);
      return null;
    }
    else if (uri.equals(_loginPage) || uri.equals(_errorPage)) {
      request.getRequestDispatcher(path).forward(request, response);
      return null;
    }

    HttpSession session = request.getSession();

    session.putValue(LOGIN_SAVED_PATH, path);
    session.putValue(LOGIN_SAVED_QUERY, request.getQueryString());

    if (response instanceof CauchoResponse) {
      ((CauchoResponse) response).killCache();
      ((CauchoResponse) response).setNoCache(true);
    }
    else {
      response.setHeader("Cache-Control", "no-cache");
    }

    // In case where the authenticator is somethin like https:/
    if (! _loginPage.startsWith("/")) {
      response.sendRedirect(response.encodeRedirectURL(_loginPage));
      return null;
    }

    // Forwards to the loginPage, never redirects according to the spec.
    request.setAttribute("caucho.login", "login");
    //RequestDispatcher disp = app.getLoginDispatcher(loginPage);
    RequestDispatcher disp = app.getRequestDispatcher(_loginPage);
    disp.forward(request, response);

    if (log.isLoggable(Level.FINE))
      log.fine("the form request has no authenticated user");
      
    return null;
  }
}
