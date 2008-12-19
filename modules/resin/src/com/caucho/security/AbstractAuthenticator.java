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

import com.caucho.security.BasicPrincipal;
import com.caucho.server.security.PasswordDigest;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.Application;
import com.caucho.util.Alarm;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.webbeans.component.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.lang.ref.SoftReference;
import java.security.MessageDigest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * All applications should extend AbstractAuthenticator to implement
 * their custom authenticators.  While this isn't absolutely required,
 * it protects implementations from API changes.
 *
 * <p>The AbstractAuthenticator provides a single-signon cache.  Users
 * logged into one web-app will share the same principal.
 */
public class AbstractAuthenticator
  implements Authenticator, HandleAware, java.io.Serializable
{
  private static final Logger log
    = Logger.getLogger(AbstractAuthenticator.class.getName());
  static final L10N L = new L10N(AbstractAuthenticator.class);
  
  protected int _principalCacheSize = 4096;
  protected LruCache<String,PrincipalEntry> _principalCache;

  protected String _passwordDigestAlgorithm = "MD5-base64";
  protected String _passwordDigestRealm = "resin";
  protected PasswordDigest _passwordDigest;

  private boolean _logoutOnTimeout = true;

  private Object _serializationHandle;

  /**
   * Returns the size of the principal cache.
   */
  public int getPrincipalCacheSize()
  {
    return _principalCacheSize;
  }

  /**
   * Sets the size of the principal cache.
   */
  public void setPrincipalCacheSize(int size)
  {
    _principalCacheSize = size;
  }

  /**
   * Returns the password digest
   */
  public PasswordDigest getPasswordDigest()
  {
    return _passwordDigest;
  }

  /**
   * Sets the password digest.  The password digest of the form:
   * "algorithm-format", e.g. "MD5-base64".
   */
  public void setPasswordDigest(PasswordDigest digest)
  {
    _passwordDigest = digest;
  }

  /**
   * Returns the password digest algorithm
   */
  public String getPasswordDigestAlgorithm()
  {
    return _passwordDigestAlgorithm;
  }

  /**
   * Sets the password digest algorithm.  The password digest of the form:
   * "algorithm-format", e.g. "MD5-base64".
   */
  public void setPasswordDigestAlgorithm(String digest)
  {
    _passwordDigestAlgorithm = digest;
  }

  /**
   * Returns the password digest realm
   */
  public String getPasswordDigestRealm()
  {
    return _passwordDigestRealm;
  }

  /**
   * Sets the password digest realm.
   */
  public void setPasswordDigestRealm(String realm)
  {
    _passwordDigestRealm = realm;
  }

  /**
   * Returns true if the user should be logged out on a session timeout.
   */
  public boolean getLogoutOnSessionTimeout()
  {
    return _logoutOnTimeout;
  }

  /**
   * Sets true if the principal should logout when the session times out.
   */
  public void setLogoutOnSessionTimeout(boolean logout)
  {
    _logoutOnTimeout = logout;
  }

  /**
   * Adds a role mapping.
   */
  public void addRoleMapping(Principal principal, String role)
  {
  }

  /**
   * Initialize the authenticator with the application.
   */
  @PostConstruct
  public void init()
    throws ServletException
  {
    if (_principalCacheSize > 0)
      _principalCache = new LruCache<String,PrincipalEntry>(_principalCacheSize);

    if (_passwordDigest != null) {
      if (_passwordDigest.getAlgorithm() == null
	  || _passwordDigest.getAlgorithm().equals("none"))
	_passwordDigest = null;
    }
    else if (_passwordDigestAlgorithm == null
	     || _passwordDigestAlgorithm.equals("none")) {
    }
    else {
      int p = _passwordDigestAlgorithm.indexOf('-');

      if (p > 0) {
        String algorithm = _passwordDigestAlgorithm.substring(0, p);
        String format = _passwordDigestAlgorithm.substring(p + 1);

        _passwordDigest = new PasswordDigest();
        _passwordDigest.setAlgorithm(algorithm);
        _passwordDigest.setFormat(format);
        _passwordDigest.setRealm(_passwordDigestRealm);

        _passwordDigest.init();
      }
    }
  }

  /**
   * Authenticator main call to login a user.
   *
   * @param user the Login's user, generally a BasicPrincipal just containing
   * the name, but may contain an X.509 certificate
   * @param credentials the login credentials
   * @param details extra information, e.g. HttpServletRequest
   */
  public Principal authenticate(Principal user,
				Credentials credentials,
				Object details)
  {
    if (credentials instanceof PasswordCredentials) {
      return authenticate(user, (PasswordCredentials) credentials, details);
    }
    else
      return null;
  }

  /**
   * Main authenticator API.
   */
  protected Principal authenticate(Principal principal,
				   PasswordCredentials cred,
				   Object details)
  {
    PasswordUser user = getUser(principal);

    if (user == null || user.isDisabled())
      return null;
    
    char []password = cred.getPassword();
    char []digest = getPasswordDigest(principal.getName(), password);

    if (! isMatch(digest, user.getPassword()) && ! user.isAnonymous()) {
      user = null;
    }
    
    Arrays.fill(digest, 'a');

    if (user != null)
      return user.getPrincipal();
    else
      return null;
  }

  /**
   * Returns the digest view of the password.  The default
   * uses the PasswordDigest class if available, and returns the
   * plaintext password if not.
   */
  protected char []getPasswordDigest(String user, char []password)
  {
    if (_passwordDigest != null)
      return _passwordDigest.getPasswordDigest(user, password);
    else {
      char []digest = new char[password.length];
      System.arraycopy(password, 0, digest, 0, password.length);
      
      return digest;
    }
  }

  /**
   * Authenticate (login) the user.
   */
  protected Principal loginImpl(HttpServletRequest request,
                                HttpServletResponse response,
                                ServletContext application,
                                String user, String password)
    throws ServletException
  {
    return null;
  }
  
  /**
   * Validates the user when using HTTP Digest authentication.
   * DigestLogin will call this
 method.  Most other AbstractLogin
   * implementations, like BasicLogin and FormLogin, will use
   * getUserPrincipal instead.
   *
   * <p>The HTTP Digest authentication uses the following algorithm
   * to calculate the digest.  The digest is then compared to
   * the client digest.
   *
   * <code><pre>
   * A1 = MD5(username + ':' + realm + ':' + password)
   * A2 = MD5(method + ':' + uri)
   * digest = MD5(A1 + ':' + nonce + A2)
   * </pre></code>
   *
   * @param request the request trying to authenticate.
   * @param response the response for setting headers and cookies.
   * @param app the servlet context
   * @param user the username
   * @param realm the authentication realm
   * @param nonce the nonce passed to the client during the challenge
   * @param uri te protected uri
   * @param qop
   * @param nc
   * @param cnonce the client nonce
   * @param clientDigest the client's calculation of the digest
   *
   * @return the logged in principal if successful
   */
  protected Principal loginDigest(HttpServletRequest request,
				  HttpServletResponse response,
				  ServletContext app,
				  String user, String realm,
				  String nonce, String uri,
				  String qop, String nc, String cnonce,
				  byte []clientDigest)
    throws ServletException
  {
    Principal principal = null;
    /*
    loginDigestImpl(request, response, app,
                                          user, realm, nonce, uri,
                                          qop, nc, cnonce,
                                          clientDigest);
    */

    if (principal != null) {
      SessionImpl session = (SessionImpl) request.getSession();
      session.setUser(principal);

      if (_principalCache != null) {
	PrincipalEntry entry = new PrincipalEntry(principal);
	entry.addSession(session);
	
        _principalCache.put(session.getId(), entry);
      }
    }

    return principal;
  }
  
  /**
   * Validates the user when HTTP Digest authentication.
   * The HTTP Digest authentication uses the following algorithm
   * to calculate the digest.  The digest is then compared to
   * the client digest.
   *
   * <code><pre>
   * A1 = MD5(username + ':' + realm + ':' + password)
   * A2 = MD5(method + ':' + uri)
   * digest = MD5(A1 + ':' + nonce + A2)
   * </pre></code>
   *
   * @param request the request trying to authenticate.
   * @param response the response for setting headers and cookies.
   * @param app the servlet context
   * @param user the username
   * @param realm the authentication realm
   * @param nonce the nonce passed to the client during the challenge
   * @param uri te protected uri
   * @param qop
   * @param nc
   * @param cnonce the client nonce
   * @param clientDigest the client's calculation of the digest
   *
   * @return the logged in principal if successful
   */
  protected Principal loginDigestImpl(HttpServletRequest request,
				      String user, String realm,
				      String nonce, String uri,
				      String qop, String nc, String cnonce,
				      byte []clientDigest)
    throws ServletException
  {
    
    try {
      if (clientDigest == null)
	return null;
      
      MessageDigest digest = MessageDigest.getInstance("MD5");
      
      byte []a1 = getDigestSecret(request, user, realm, "MD5");

      if (a1 == null)
        return null;

      digestUpdateHex(digest, a1);
      
      digest.update((byte) ':');
      for (int i = 0; i < nonce.length(); i++)
        digest.update((byte) nonce.charAt(i));

      if (qop != null) {
        digest.update((byte) ':');
        for (int i = 0; i < nc.length(); i++)
          digest.update((byte) nc.charAt(i));

        digest.update((byte) ':');

        for (int i = 0; cnonce != null && i < cnonce.length(); i++)
          digest.update((byte) cnonce.charAt(i));
        
        digest.update((byte) ':');
        for (int i = 0; qop != null && i < qop.length(); i++)
          digest.update((byte) qop.charAt(i));
      }
      digest.update((byte) ':');

      byte []a2 = digest(request.getMethod() + ":" + uri);

      digestUpdateHex(digest, a2);

      byte []serverDigest = digest.digest();

      if (clientDigest.length != serverDigest.length)
        return null;

      for (int i = 0; i < clientDigest.length; i++) {
        if (serverDigest[i] != clientDigest[i])
          return null;
      }

      return new BasicPrincipal(user);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Abstract method to return a user based on the name
   *
   * @param userName the string user name
   *
   * @return the populated PasswordUser value
   */
  protected PasswordUser getUser(String userName)
  {
    return null;
  }

  /**
   * Returns the user based on a principal
   */
  protected PasswordUser getUser(Principal principal)
  {
    return getUser(principal.getName());
  }

  /**
   * Returns true if the user plays the named role.
   *
   * @param request the servlet request
   * @param user the user to test
   * @param role the role to test
   */
  public boolean isUserInRole(Principal user, String role)
  {
    PasswordUser passwordUser = getUser(user);

    if (passwordUser != null)
      return passwordUser.isUserInRole(role);
    else
      return false;
  }

  /**
   * Logs the user out from the session.
   *
   * @param application the application
   * @param timeoutSession the session timing out, null if not a timeout logout
   * @param user the logged in user
   */
  public void logout(Principal user)
  {
    if (log.isLoggable(Level.FINE))
      log.fine(this + " logout " + user);

    /*
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
						   Alarm.getCurrentTime(),
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
    */
  }

  //
  // utilities
  //

  private void digestUpdateHex(MessageDigest digest, byte []bytes)
  {
    for (int i = 0; i < bytes.length; i++) {
      int b = bytes[i];
      int d1 = (b >> 4) & 0xf;
      int d2 = b & 0xf;

      if (d1 < 10)
        digest.update((byte) (d1 + '0'));
      else
        digest.update((byte) (d1 + 'a' - 10));

      if (d2 < 10)
        digest.update((byte) (d2 + '0'));
      else
        digest.update((byte) (d2 + 'a' - 10));
    }
  }

  protected byte []stringToDigest(String digest)
  {
    if (digest == null)
      return null;
    
    int len = (digest.length() + 1) / 2;
    byte []clientDigest = new byte[len];

    for (int i = 0; i + 1 < digest.length(); i += 2) {
      int ch1 = digest.charAt(i);
      int ch2 = digest.charAt(i + 1);

      int b = 0;
      if (ch1 >= '0' && ch1 <= '9')
        b += ch1 - '0';
      else if (ch1 >= 'a' && ch1 <= 'f')
        b += ch1 - 'a' + 10;

      b *= 16;
      
      if (ch2 >= '0' && ch2 <= '9')
        b += ch2 - '0';
      else if (ch2 >= 'a' && ch2 <= 'f')
        b += ch2 - 'a' + 10;

      clientDigest[i / 2] = (byte) b;
    }

    return clientDigest;
  }

  /**
   * Returns the digest secret for Digest authentication.
   */
  protected byte []getDigestSecret(HttpServletRequest request,
                                   String username, String realm,
                                   String algorithm)
    throws ServletException
  {
    String password = getDigestPassword(username, realm, request);
    
    if (password == null)
      return null;

    if (_passwordDigest != null)
      return _passwordDigest.stringToDigest(password);

    try {
      MessageDigest digest = MessageDigest.getInstance(algorithm);

      String string = username + ":" + realm + ":" + password;
      byte []data = string.getBytes("UTF8");
      return digest.digest(data);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  protected byte []digest(String value)
    throws ServletException
  {
    try {
      MessageDigest digest = MessageDigest.getInstance("MD5");

      byte []data = value.getBytes("UTF8");
      return digest.digest(data);
    } catch (Exception e) {
      throw new ServletException(e);
    }
  }

  /**
   * Returns the password for authenticators too lazy to calculate the
   * digest.
   */
  protected String getDigestPassword(String username,
				     String realm,
				     HttpServletRequest request)
  {
    return null;
  }

  /**
   * Grab the user from the request, assuming the user has
   * already logged in.  In other words, overriding methods could
   * use cookies or the session to find the logged in principal, but
   * shouldn't try to log the user in with form parameters.
   *
   * @param request the servlet request.
   *
   * @return a Principal representing the user or null if none has logged in.
   */
  public Principal getUserPrincipal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    ServletContext application)
    throws ServletException
  {
    SessionImpl session = (SessionImpl) request.getSession(false);
    Principal user = null;

    if (session != null)
      user = session.getUser();
    
    if (user != null)
      return user;

    PrincipalEntry entry = null;
    
    if (_principalCache == null) {
    }
    else if (session != null)
      entry = _principalCache.get(session.getId());
    else if (request.getRequestedSessionId() != null)
      entry = _principalCache.get(request.getRequestedSessionId());

    if (entry != null) {
      user = entry.getPrincipal();

      if (session == null)
	session = (SessionImpl) request.getSession(true);
      
      session.setUser(user);
      entry.addSession(session);
      
      return user;
    }

    user = getUserPrincipalImpl(request, application);

    if (user == null) {
    }
    else if (session != null) {
      entry = new PrincipalEntry(user);
      
      session.setUser(user);
      entry.addSession(session);
      
      _principalCache.put(session.getId(), entry);
    }
    else if (request.getRequestedSessionId() != null) {
      entry = new PrincipalEntry(user);
      
      _principalCache.put(request.getRequestedSessionId(), entry);
    }

    return user;
  }
  
  /**
   * Gets the user from a persistent cookie, uaing authenticateCookie
   * to actually look the cookie up.
   */
  protected Principal getUserPrincipalImpl(HttpServletRequest request,
                                           ServletContext application)
    throws ServletException
  {
    return null;
  }

  /**
   * Tests passwords
   */
  private boolean isMatch(char []password, char []userPassword)
  {
    int len = password.length;

    if (len != userPassword.length)
      return false;

    for (int i = 0; i < len; i++) {
      if (password[i] != userPassword[i])
	return false;
    }

    return true;
  }
  
  /**
   * Sets the serialization handle
   */
  public void setSerializationHandle(Object handle)
  {
    _serializationHandle = handle;
  }

  /**
   * Serialize to the handle
   */
  public Object writeReplace()
  {
    return _serializationHandle;
  }

  public String toString()
  {
    return (getClass().getSimpleName()
	    + "[" + _passwordDigestAlgorithm
	    + "," + _passwordDigestRealm + "]");
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
	    session.logout();
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
	    session.logout();
	    session.invalidateLogout();  // #599,  server/12i3
	  }
	} catch (Exception e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }
  }
}
