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
import java.lang.ref.SoftReference;
import java.security.Principal;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caucho.v5.http.session.SessionImpl;

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
 * can use the same DatabaseAuthenticator.  Some applications, like SSL
 * client certificate login, may want to combine the Login and authentication
 * into one class.
 *
 * <p>Login instances are configured through bean introspection.  Adding
 * a public <code>setFoo(String foo)</code> method will be configured with
 * the following login-config:
 *
 * <code><pre>
 * &lt;myfoo:CustomLogin xmlns:myfoo="urn:java:com.foo.myfoo">
 *   &lt;foo>bar&lt;/foo>
 * &lt;/myfoo:CustomLogin>
 * </pre></code>
 *
 * @since Resin 4.0.0
 */
public abstract class LoginBase implements Login
{
  private final static Logger log
    = Logger.getLogger(LoginBase.class.getName());

  
  /**
   * The configured authenticator for the login.  Implementing classes will
   * typically delegate calls to the authenticator after extracting the
   * username and password.
   */
  private AuthenticatorRole _auth;
  // protected SingleSignon _singleSignon;

  //private @Inject Instance<AuthenticatorRole> _authInstance;
  private @Inject AuthenticatorRole _authInstance;
  // private @Inject Instance<SingleSignon> _signonInstance;

  private boolean _isSessionSaveLogin = true;
  private boolean _isLogoutOnTimeout = true;

  protected LoginBase()
  {
  }

  /**
   * Sets the authenticator.
   */
  public void setAuthenticator(AuthenticatorRole auth)
  {
    _auth = auth;
  }

  /**
   * Gets the authenticator.
   */
  @Override
  public AuthenticatorRole getAuthenticator()
  {
    if (_auth == null) {
      /*
      if (! _authInstance.isUnsatisfied()) {
        _auth = _authInstance.get();
      }
      */
      _auth = _authInstance;

      if (_auth == null) {
        _auth = new NullAuthenticator();
      }

      if (log.isLoggable(Level.FINE))
        log.fine(toString() + " using " + _auth);
    }

    return _auth;
  }

  /*
  protected SingleSignon getSingleSignon()
  {
    if (_singleSignon == null) {
      AuthenticatorRole auth = getAuthenticator();

      if (_auth instanceof AbstractAuthenticator2) {
        AbstractAuthenticator2 abstractAuth
          = (AbstractAuthenticator2) auth;

        _singleSignon = abstractAuth.getSingleSignon();
      }
      
      // server/1al4
      if (_singleSignon == null) {
        try {
          _singleSignon = new ClusterSingleSignon("login");
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }

    return _singleSignon;
  }
  */

  /**
   * Returns true if the user should be logged out on a session timeout.
   */
  public boolean isLogoutOnSessionTimeout()
  {
    return _isLogoutOnTimeout;
  }

  /**
   * Sets true if the principal should logout when the session times out.
   */
  public void setLogoutOnSessionTimeout(boolean logout)
  {
    _isLogoutOnTimeout = logout;
  }

  /**
   * Sets true if the user should be saved in the session.
   */
  public void setSessionSaveLogin(boolean isSave)
  {
    _isSessionSaveLogin = isSave;
  }

  /**
   * Sets true if the user should be saved in the session.
   */
  public boolean isSessionSaveLogin()
  {
    return _isSessionSaveLogin;
  }

  /**
   * Initialize the login.  <code>init()</code> will be called after all
   * the bean parameters have been set.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    // server/12cc - XXX: this should be allowed, though

    /*
    // XXX: order
    if (_singleSignon == null && ! _signonInstance.isUnsatisfied()) {
      _singleSignon = _signonInstance.get();
    }
    */
  }

  /**
   * Returns the authentication type.  <code>getAuthType</code> is called
   * by <code>HttpServletRequest.getAuthType</code>.
   */
  @Override
  public String getAuthType()
  {
    return "none";
  }

  /**
   * Returns true if the login can be used for this request. This lets
   * webapps use multiple login methods.
   */
  @Override
  public boolean isLoginUsedForRequest(HttpServletRequest request)
  {
    return true;
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
   *
   * @return the logged in principal on success, null on failure.
   */
  @Override
  public Principal getUserPrincipal(HttpServletRequest request)
  {
    return getUserPrincipal(request, false);
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
   *
   * @return the logged in principal on success, null on failure.
   */
  private Principal getUserPrincipal(HttpServletRequest request, boolean isLogin)
  {
    Principal user = (Principal) request.getAttribute(LOGIN_USER);

    if (user == null) {
    }
    else if (user != AbstractAuthenticator2.NULL_USER) {
      return user;
    }
    else if (! isLogin) {
      return null;
    }

    Principal savedUser = findSavedUser(request);
    
    // server/12c9 - new login overrides old
    if (savedUser != null && isSavedUserValid(request, savedUser)) {
      request.setAttribute(LOGIN_USER, savedUser);

      return savedUser;
    }

    // server/12d2
    if (isLogin)
      user = getLoginPrincipalImpl(request);
    else
      user = getUserPrincipalImpl(request);

    if (user != null) {
      request.setAttribute(LOGIN_USER, user);
      
      saveUser(request, user);
    }
    else if (savedUser != null) {
      // clear the saved user
      request.setAttribute(LOGIN_USER, AbstractAuthenticator2.NULL_USER);
      
      saveUser(request, null);
    }
    else {
      request.setAttribute(LOGIN_USER, AbstractAuthenticator2.NULL_USER);
    }

    return user;
  }

  /**
   * Logs a user in.  The authenticate method is called during the
   * security check.  If the user does not exist, <code>authenticate</code>
   * sets the reponse error page and returns null.
   *
   * @param request servlet request
   * @param response servlet response for a failed authentication.
   * @param isFail if true send a challenge (Form|HTTP Basic,etc.)
   *
   * @return the logged in principal on success, null on failure.
   */
  @Override
  public Principal login(HttpServletRequest request,
                         HttpServletResponse response,
                         boolean isFail)
  {
    try {
      // server/123c, 1a25, 1as0
      Principal savedUser = null;
      
      savedUser = findSavedUser(request);

      // server/12c9 - new login overrides old
      if (savedUser != null && isSavedUserValid(request, savedUser)) {
        request.setAttribute(LOGIN_USER, savedUser);

        return savedUser;
      }

      Principal user = login(request, response);

      if (user != null || savedUser != null) {
        // server/12h7
        saveUser(request, user);
      }

      if (user != null) {
        loginSuccessResponse(user, request, response);

        return user;
      }

      if (isFail) {
        log.fine(this + " sending login challenge");

        loginChallenge(request, response);
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      // server/12d5
      throw new LoginException(e);
    }

    return null;
  }
  
  /**
   * Attempts to login the user if the user cannot be found in the 
   * session or the single-signon. 
   */
  protected Principal login(HttpServletRequest request,
                            HttpServletResponse response)
  {
    // Most login classes will extract the user and password (or some other
    // credentials) from the request and call auth.login.

    // return getLoginPrincipalImpl(request);
    // server/1a26
    // server/123c, 1a25, 1as0
    /*
    Principal user = (Principal) request.getAttribute(LOGIN_USER);

    if (user == null) {
      return getUserPrincipal(request, true);
    }
    else if (user == AbstractAuthenticator.NULL_USER) {
      return null;
    }
    else {
      return user;
    }
    */
    return getUserPrincipal(request, true);
  }
  
  /**
   * Looks up the user based on session or single signon.
   */
  protected Principal findSavedUser(HttpServletRequest request)
  {
    //SingleSignon singleSignon = getSingleSignon();

    SessionImpl session = (SessionImpl) request.getSession(false);

    String sessionId;

    if (session != null) {
      sessionId = session.getId();
    }
    else {
      sessionId = request.getRequestedSessionId();
    }

    if (sessionId == null) {
      return null;
    }
    /*
    else if (singleSignon != null) {
      Principal user = singleSignon.get(sessionId);
      
      if (user != null && log.isLoggable(Level.FINER))
        log.finer(this + " load user '" + user + "' from " + singleSignon);
      
      return user;
    }
    */
    else if (isSessionSaveLogin() && session != null) {
      Principal user = (Principal) session.getAttribute(LOGIN_USER);
      
      if (user != null && log.isLoggable(Level.FINER))
        log.finer(this + " load user '" + user + "' from session");
      
      return user;
    }
    else
      return null;
  }

  /**
   * Saves the user based on session or single signon.
   */
  protected void saveUser(HttpServletRequest request,
                          Principal user)
  {
    // SingleSignon singleSignon = getSingleSignon();

    SessionImpl session;

    if (isSessionSaveLogin())
      session = (SessionImpl) request.getSession(true);
    else
      session = (SessionImpl) request.getSession(false);

    String sessionId;

    if (session != null)
      sessionId = session.getId();
    else
      sessionId = request.getRequestedSessionId();

    if (sessionId == null) {
    }
    /*
    else if (singleSignon != null) {
      singleSignon.put(sessionId, user);
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " save user '" + user +"' in single signon " + singleSignon);
    }
    */
    else if (isSessionSaveLogin()) {
      session.setAttribute(LOGIN_USER, user);
      
      if (log.isLoggable(Level.FINER))
        log.finer(this + " save user '" + user + "' in session " + session);
    }
  }

  @Override
  public boolean isPasswordBased()
  {
    return false;
  }

  /**
   * Gets the user from a persistent cookie, using authenticateCookie
   * to actually look the cookie up.
   */
  protected Principal getUserPrincipalImpl(HttpServletRequest request)
  {
    return null;
  }

  /**
   * Returns the non-authenticated principal for the user request
   */
  protected boolean isSavedUserValid(HttpServletRequest request,
                                     Principal savedUser)
  {
    return true;
  }

  /**
   * Gets the user from a persistent cookie, using authenticateCookie
   * to actually look the cookie up.
   */
  protected Principal getLoginPrincipalImpl(HttpServletRequest request)
  {
    return getUserPrincipalImpl(request);
  }

  /**
   * Implementation of the login challenge
   */
  protected void loginChallenge(HttpServletRequest request,
                                HttpServletResponse response)
    throws ServletException, IOException
  {
  }

  /**
   * HTTP updates after a successful login
   */
  protected void loginSuccessResponse(Principal user,
                                      HttpServletRequest request,
                                      HttpServletResponse response)
    throws ServletException, IOException
  {
  }

  /**
   * Returns true if the current user plays the named role.
   * <code>isUserInRole</code> is called in response to the
   * <code>HttpServletRequest.isUserInRole</code> call.
   *
   * @param user UserPrincipal object associated with request
   * @param role to be tested
   *
   * @return the logged in principal on success, null on failure.
   */
  @Override
  public boolean isUserInRole(Principal user, String role)
  {
    return getAuthenticator().isUserInRole(user, role);
  }

  /**
   * Logs the user out from the given request.
   *
   * <p>Since there is no servlet API for logout, this must be called
   * directly from user code.  Resin stores the web-app's login object
   * in the ServletContext attribute "caucho.login".
   */
  @Override
  public void logout(Principal user,
                     HttpServletRequest request,
                     HttpServletResponse response)
  {
    String sessionId = request.getRequestedSessionId();

    logoutImpl(user, request, response);
    
    HttpSession session = request.getSession(false);
    
    if (session != null)
      session.removeAttribute(LOGIN_USER);
    
    request.removeAttribute(LOGIN_USER);

    /*
    SingleSignon singleSignon = getSingleSignon();

    if (singleSignon != null)
      singleSignon.remove(sessionId);
      */
  }

  /**
   * Called when the session invalidates.
   */
  @Override
  public void sessionInvalidate(HttpSession session,
                                boolean isTimeout)
  {
    //LoginPrincipal login = (LoginPrincipal) session.getAttribute(LOGIN_NAME);

    if (session != null) {
      /*
      SingleSignon singleSignon = getSingleSignon();

      // server/12cg
      if (singleSignon != null
          && (! isTimeout || isLogoutOnSessionTimeout())) {
        singleSignon.remove(session.getId());
      }
      */
    }
  }

  /**
   * Logs the user out from the given request.
   *
   * <p>Since there is no servlet API for logout, this must be called
   * directly from user code.  Resin stores the web-app's login object
   * in the ServletContext attribute "caucho.login".
   */
  protected void logoutImpl(Principal user,
                            HttpServletRequest request,
                            HttpServletResponse response)
  {
  }

  /**
   * Logs the user out from the session.
   *
   * @param user the logged in user
   */
  /*
  public void logout(Principal user)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " logout " + user);

    if (sessionId != null) {
      if (_principalCache == null) {
      }
      else if (timeoutSession != null) {
        PrincipalEntry entry =  _principalCache.get(sessionId);

        if (entry != null && entry.logout(timeoutSession)) {
          _principalCache.remove(sessionId);
        }
      }
      else {
        PrincipalEntry entry =  _principalCache.remove(sessionId);

        if (entry != null)
          entry.logout();
      }

      Application app = (Application) application;
      SessionManager manager = app.getSessionManager();

      if (manager != null) {
        try {
          SessionImpl session = manager.getSession(sessionId,
                                                   CurrentTime.getCurrentTime(),
                                                   false, true);

          if (session != null) {
            session.finish();
            session.logout();
          }
        } catch (Exception e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }
  */

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }

  static class PrincipalEntry {
    private Principal _principal;
    private ArrayList<SoftReference<SessionImpl>> _sessions;

    PrincipalEntry(Principal principal)
    {
      _principal = principal;
    }

    Principal getPrincipal()
    {
      return _principal;
    }

    void addSession(SessionImpl session)
    {
      if (_sessions == null)
        _sessions = new ArrayList<SoftReference<SessionImpl>>();

      _sessions.add(new SoftReference<SessionImpl>(session));
    }

    /**
     * Logout only the given session, returning true if it's the
     * last session to logout.
     */
    boolean logout(HttpSession timeoutSession)
    {
      ArrayList<SoftReference<SessionImpl>> sessions = _sessions;

      if (sessions == null)
        return true;

      boolean isEmpty = true;
      for (int i = sessions.size() - 1; i >= 0; i--) {
        SoftReference<SessionImpl> ref = sessions.get(i);
        SessionImpl session = ref.get();

        try {
          if (session == timeoutSession) {
            sessions.remove(i);
            // session.logout();
            // XXX: invalidate?
          }
          else if (session == null)
            sessions.remove(i);
          else
            isEmpty = false;
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }

      return isEmpty;
    }

    void logout()
    {
      ArrayList<SoftReference<SessionImpl>> sessions = _sessions;
      _sessions = null;

      for (int i = 0; sessions != null && i < sessions.size(); i++) {
        SoftReference<SessionImpl> ref = sessions.get(i);
        SessionImpl session = ref.get();

        try {
          if (session != null) {
            // session.logout();
            session.invalidateLogout();  // #599,  server/12i3
          }
        } catch (Exception e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
    }
  }

  @SuppressWarnings("serial")
  static class LoginPrincipal implements java.io.Serializable {
    // server/12ci - XXX: this was transient before.
    private Principal _user;

    LoginPrincipal(Principal user)
    {
      _user = user;
    }

    public Principal getUser()
    {
      return _user;
    }

    public String toString()
    {
      return getClass().getSimpleName() + "[" + _user + "]";
    }
  }
}
