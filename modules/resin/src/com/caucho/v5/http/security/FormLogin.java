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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.protocol.ResponseCaucho;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.util.L10N;

/**
 * Used to authenticate users in a servlet request.  Applications will
 * implement the Authenticator interface with a bean for authentication.
 *
 * @since Resin 2.0.2
 */

//@InlineConfig
@Singleton
public class FormLogin extends LoginBase
{
  private static final L10N L = new L10N(FormLogin.class);
  private static final Logger log = Logger.getLogger(FormLogin.class.getName());

  public static final String LOGIN_CHECK
    = "com.caucho.security.form.login";

  public static final String LOGIN_SAVED_PATH
    = "com.caucho.servlet.login.path";
  public static final String LOGIN_SAVED_QUERY
    = "com.caucho.servlet.login.query";

  protected String _loginPage;
  protected String _errorPage;
  protected boolean _internalForward;
  protected boolean _formURIPriority;

  private WebApp _webApp = WebApp.getCurrent();

  /**
   * Sets the login page.
   */
  public void setFormLoginPage(String formLoginPage)
    throws ConfigException
  {
    int colon = formLoginPage.indexOf(':');
    int slash = formLoginPage.indexOf('/');

    if (colon > 0 && colon < slash) {
    }
    else if (slash != 0)
      throw new ConfigException(L.l("form-login-page '{0}' must start with '/'.  The form-login-page is relative to the web-app root.", formLoginPage));

    _loginPage = formLoginPage;
  }

  public void setLoginPage(String loginPage)
  {
    setFormLoginPage(loginPage);
  }

  /**
   * Gets the login page.
   */
  public String getFormLoginPage()
  {
    return _loginPage;
  }
  
  @Override
  public boolean isPasswordBased()
  {
    return true;
  }

  /**
   * Sets the error page.
   */
  public void setFormErrorPage(String formErrorPage)
    throws ConfigException
  {
    if (! formErrorPage.startsWith("/"))
      throw new ConfigException(L.l("form-error-page '{0}' must start with '/'.  The form-error-page is relative to the web-app root.", formErrorPage));

    _errorPage = formErrorPage;
  }

  public void setErrorPage(String errorPage)
  {
    setFormErrorPage(errorPage);
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
  @Override
  public void init()
    throws ServletException
  {
    super.init();

    if (_errorPage == null)
      _errorPage = _loginPage;

    if (_loginPage == null)
      _loginPage = _errorPage;

    if (_loginPage == null)
      throw new ConfigException(L.l("FormLogin needs an form-login-page"));
  }

  /**
   * Returns the authentication type.
   */
  @Override
  public String getAuthType()
  {
    return "Form";
  }

  /**
   * Returns true if the request has a matching login.
   */
  @Override
  public boolean isLoginUsedForRequest(HttpServletRequest request)
  {
    return request.getServletPath().indexOf("j_security_check") >= 0;
  }

  /**
   * Logs a user in with a user name and a password.
   *
   * @param request servlet request
   *
   * @return the logged in principal on success, null on failure.
   */
  @Override
  public Principal getUserPrincipalImpl(HttpServletRequest request)
  {
    Principal user;

    AuthenticatorRole auth = getAuthenticator();

    if (auth instanceof CookieAuthenticator) {
      CookieAuthenticator cookieAuth = (CookieAuthenticator) auth;

      Cookie resinAuth = ((RequestCaucho) request).getCookie("resinauthid");

      if (resinAuth != null) {
        user = cookieAuth.authenticateByCookie(resinAuth.getValue());

        if (user != null)
          return user;
      }
    }

    String userName = request.getParameter("j_username");
    String passwordString = request.getParameter("j_password");
    
    if (userName == null) {
      userName = (String) request.getAttribute(LOGIN_USER_NAME);
    }
    
    if (passwordString == null) {
      passwordString = (String) request.getAttribute(LOGIN_PASSWORD);
    }

    if (userName == null || passwordString == null) {
      return null;
    }

    char []password = passwordString.toCharArray();

    BasicPrincipal basicUser = new BasicPrincipal(userName);

    Credentials credentials = new PasswordCredentials(password);

    user = auth.authenticate(basicUser, credentials, request);

    return user;
  }

  /**
   * Returns true if a new login overrides the saved user
   */
  @Override
  protected boolean isSavedUserValid(HttpServletRequest request,
                                     Principal savedUser)
  {
    String userName = request.getParameter("j_username");

    // server/135j
    return userName == null;// || userName.equals(savedUser.getName());
  }

  /**
   * Updates after a successful login
   *
   * @param request servlet request
   * @param response servlet response, in case any cookie need sending.
   *
   * @return the logged in principal on success, null on failure.
   */
  @Override
  public void loginSuccessResponse(Principal user,
                                   HttpServletRequest request,
                                   HttpServletResponse response)
    throws ServletException, IOException
  {
    if (request.getAttribute(LOGIN_CHECK) != null)
      return;
    request.setAttribute(LOGIN_CHECK, "login");

    WebApp webApp = _webApp;

    String jUseCookieAuth = (String) request.getParameter("j_use_cookie_auth");

    AuthenticatorRole auth = getAuthenticator();

    if (auth instanceof CookieAuthenticator
        && ((CookieAuthenticator) auth).isCookieSupported(jUseCookieAuth)) {
      CookieAuthenticator cookieAuth = (CookieAuthenticator) auth;

      generateCookie(user, cookieAuth, webApp, request, response);
    }

    String path = request.getServletPath();

    if (path == null)
      path = request.getPathInfo();
    else if (request.getPathInfo() != null)
      path = path + request.getPathInfo();

    if (path.equals("")) {
      // Forward?
      path = request.getContextPath() + "/";
      response.sendRedirect(response.encodeRedirectURL(path));
      return;
    }

    if (path.endsWith("/j_security_check")) {
      RequestDispatcher disp;
      disp = webApp.getNamedDispatcher("j_security_check");

      if (disp == null)
        throw new ServletException(L.l("j_security_check servlet must be defined to use form-based login."));

      disp.forward(request, response);
      return;
    }
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
  @Override
  public void loginChallenge(HttpServletRequest request,
                             HttpServletResponse response)
    throws ServletException, IOException
  {
    String path = request.getServletPath();

    if (path == null)
      path = request.getPathInfo();
    else if (request.getPathInfo() != null)
      path = path + request.getPathInfo();
    
    if (path.equals("")) {
      // Forward?
      path = request.getContextPath() + "/";
      response.sendRedirect(response.encodeRedirectURL(path));
      return;
    }

    WebApp webApp = _webApp;

    String uri = request.getRequestURI();

    if (path.endsWith("/j_security_check")) {
      // server/12d8, server/12bs

      if (response instanceof ResponseCaucho) {
        ((ResponseCaucho) response).setNoCache(true);
      }
      else {
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
      }

      RequestDispatcher disp = webApp.getRequestDispatcher(_errorPage);
      disp.forward(request, response);
      return;
    }
    else if (uri.equals(_loginPage) || uri.equals(_errorPage)) {
      request.getRequestDispatcher(path).forward(request, response);
      return;
    }

    HttpSession session = request.getSession();

    session.setAttribute(LOGIN_SAVED_PATH, path);
    session.setAttribute(LOGIN_SAVED_QUERY, request.getQueryString());

    if (response instanceof ResponseCaucho) {
      ((ResponseCaucho) response).killCache();
      ((ResponseCaucho) response).setNoCache(true);
    }
    else {
      response.setHeader("Cache-Control", "no-cache");
    }

    // In case where the authenticator is something like https:/
    if (! _loginPage.startsWith("/")) {
      response.sendRedirect(response.encodeRedirectURL(_loginPage));
      return;
    }

    // Forwards to the loginPage, never redirects according to the spec.
    request.setAttribute(LOGIN_CHECK, "login");
    //RequestDispatcher disp = app.getLoginDispatcher(loginPage);
    RequestDispatcher disp = webApp.getRequestDispatcher(_loginPage);
    disp.forward(request, response);

    if (log.isLoggable(Level.FINE))
      log.fine(this + " request '" + uri + "' has no authenticated user");
  }

  private void generateCookie(Principal user,
                              CookieAuthenticator auth,
                              WebApp webApp,
                              HttpServletRequest request,
                              HttpServletResponse response)
  {
    if (webApp == null)
      return;

    SessionManager manager = webApp.getSessionManager();
    String value = manager.createCookieValue();

    Cookie cookie = new Cookie("resinauthid", value);
    cookie.setVersion(1);

    long cookieMaxAge = 365L * 24L * 3600L * 1000L;

    cookie.setMaxAge((int) (cookieMaxAge / 1000L));
    cookie.setPath("/");
    cookie.setDomain(webApp.generateCookieDomain(request));

    auth.associateCookie(user, value);

    response.addCookie(cookie);
  }
}
