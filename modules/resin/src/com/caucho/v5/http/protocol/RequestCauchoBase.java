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

package com.caucho.v5.http.protocol;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import com.caucho.v5.http.baratine.FormBaratine;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.security.AuthenticatorRole;
import com.caucho.v5.http.security.Login;
import com.caucho.v5.http.session.SessionImpl;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.i18n.CharacterEncoding;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.CharSegment;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.L10N;
import com.caucho.v5.vfs.Encoding;
import com.caucho.v5.vfs.PathImpl;
import com.caucho.v5.vfs.ReadStream;
import com.caucho.v5.vfs.Vfs;
import com.caucho.v5.vfs.WriteStream;

abstract public class RequestCauchoBase implements RequestCaucho
{
  private static final L10N L = new L10N(RequestCauchoBase.class);
  private static final Logger log
    = Logger.getLogger(RequestCauchoBase.class.getName());

  static final String CHAR_ENCODING = "resin.form.character.encoding";
  static final String FORM_LOCALE = "resin.form.local";
  static final String CAUCHO_CHAR_ENCODING = "caucho.form.character.encoding";

  private static final Charset UTF8 = Charset.forName("UTF-8");

  private int _sessionGroup = -1;

  private boolean _sessionIsLoaded;
  private SessionImpl _session;

  private ArrayList<PathImpl> _removeOnExit;

  // form
  private HashMapImpl<String,String[]> _filledForm;
  private List<Part> _parts;

  abstract public ResponseCaucho getResponse();

  @Override
  public RequestDispatcher getRequestDispatcher(String path)
  {
    if (path == null || path.length() == 0)
      return null;
    else if (path.charAt(0) == '/')
      return getWebApp().getRequestDispatcher(path);
    else {
      CharBuffer cb = new CharBuffer();

      WebApp webApp = getWebApp();

      String servletPath = getPageServletPath();
      if (servletPath != null)
        cb.append(servletPath);
      String pathInfo = getPagePathInfo();
      if (pathInfo != null)
        cb.append(pathInfo);

      int p = cb.lastIndexOf('/');
      if (p >= 0)
        cb.setLength(p);
      cb.append('/');
      cb.append(path);

      if (webApp != null)
        return webApp.getRequestDispatcher(cb.toString());

      return null;
    }
  }

  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  @Override
  public Enumeration<String> getParameterNames()
  {
    return getParameterNamesImpl();
  }

  public final Enumeration<String> getParameterNamesImpl()
  {
    if (_filledForm == null)
      _filledForm = parseQueryImpl();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  @Override
  public Map<String,String[]> getParameterMap()
  {
    return getParameterMapImpl();
  }

  public final Map<String,String[]> getParameterMapImpl()
  {
    if (_filledForm == null) {
      _filledForm = parseQueryImpl();
    }

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  @Override
  public String []getParameterValues(String name)
  {
    return getParameterValuesImpl(name);
  }

  public final String []getParameterValuesImpl(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQueryImpl();

    return _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  @Override
  public String getParameter(String name) 
  {
    return getParameterImpl(name);
  }

  public final String getParameterImpl(String name)
  {
    String []values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  @Override
  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    InvocationServlet invocation = getInvocation();

    MultipartConfigElement multipartConfig
      = invocation.getMultipartConfig();

    if (multipartConfig == null)
      throw new ServletException(L.l("multipart-form is disabled; check @MultipartConfig annotation on '{0}'.", invocation.getServletName()));

    /*
    if (! getWebApp().doMultipartForm())
      throw new ServletException("multipart-form is disabled; check <multipart-form> configuration tag.");
      */

    String contentType = getContentType();
    if (contentType == null || ! getContentType().startsWith("multipart/form-data"))
      throw new ServletException("Content-Type must be of 'multipart/form-data'.");

    if (_filledForm == null)
      _filledForm = parseQueryImpl();

    return _parts;
  }
  
  @Override
  public boolean isMultipartEnabled()
  {
    InvocationServlet invocation = getInvocation();
    
    if (invocation == null) {
      return false;
    }

    return invocation.getMultipartConfig() != null;
  }
  /**
   * @since Servlet 3.0
   */
  @Override
  public Part getPart(String name)
    throws IOException, ServletException
  {
    Collection<Part> parts = getParts();
    
    if (parts == null) {
      return null;
    }

    for (Part part : parts) {
      if (name.equals(part.getName()))
        return part;
    }

    return null;
  }

  public abstract RequestHttpBase getAbstractHttpRequest();

  public HttpContainer getServer()
  {
    return (HttpContainer) getAbstractHttpRequest().getHttp();
  }

  protected HashMapImpl<String,String[]> parseQueryImpl()
  {
    HashMapImpl<String, String[]> form = new HashMapImpl<String, String[]>();
    
    parseGetQueryImpl(form);
    parsePostQueryImpl(form);
    
    return form;
  }

  protected void parseGetQueryImpl(HashMapImpl<String,String[]> form)
  {
    RequestHttpBase request = getAbstractHttpRequest();

    try {
      String query = getInvocation().getQueryString();//getQueryString();

      if (query == null)
        return;

      Form formParser = new Form();
      
      /*
      if (request != null)
        formParser = request.getFormParser();
      else
        formParser = new Form();
        */

      String charEncoding = getCharacterEncoding();
      if (charEncoding == null) {
        charEncoding = (String) getAttribute(CAUCHO_CHAR_ENCODING);
        if (charEncoding == null)
          charEncoding = (String) getAttribute(CHAR_ENCODING);
        if (charEncoding == null) {
          Locale locale = (Locale) getAttribute(FORM_LOCALE);
          if (locale != null)
            charEncoding = Encoding.getMimeName(locale);
        }
      }

      String queryEncoding = charEncoding;

      if (queryEncoding == null && getServer() != null)
        queryEncoding = getServer().getURLCharacterEncoding();

      if (queryEncoding == null)
        queryEncoding = CharacterEncoding.getLocalEncoding();

      String javaEncoding = Encoding.getJavaName(queryEncoding);

      formParser.parseQueryString(form, query, javaEncoding, true);
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  protected void parsePostQueryImpl(HashMapImpl<String,String[]> form)
  {
    RequestHttpBase request = getAbstractHttpRequest();

    if (request == null)
      return;

    try {
      CharSegment contentType = request.getContentTypeBuffer();

      if (contentType == null)
        return;

      Form formParser = new Form(); // request.getFormParser();
      long contentLength = request.contentLength();

      String charEncoding = getCharacterEncoding();
      if (charEncoding == null) {
        charEncoding = (String) getAttribute(CAUCHO_CHAR_ENCODING);
        if (charEncoding == null)
          charEncoding = (String) getAttribute(CHAR_ENCODING);
        if (charEncoding == null) {
          Locale locale = (Locale) getAttribute(FORM_LOCALE);
          if (locale != null)
            charEncoding = Encoding.getMimeName(locale);
        }
      }

      if (charEncoding == null)
        charEncoding = CharacterEncoding.getLocalEncoding();

      String javaEncoding = Encoding.getJavaName(charEncoding);

      InvocationServlet invocation = getInvocation();

      MultipartConfigElement multipartConfig = invocation.getMultipartConfig();

      if (contentType == null || ! "POST".equalsIgnoreCase(getMethod())) {
      }

      else if (contentType.startsWith("application/x-www-form-urlencoded")) {
        formParser.parsePostData(form, getInputStream(), javaEncoding,
                                 getWebApp().getFormParameterMax());
      }

      else if ((getWebApp().isMultipartFormEnabled() || multipartConfig != null)
               && contentType.startsWith("multipart/form-data")) {
        int length = contentType.length();
        int i = contentType.indexOf("boundary=");

        if (i < 0)
          return;

        long formUploadMax = getWebApp().getFormUploadMax();
        long parameterLengthMax = getWebApp().getFormParameterLengthMax();

        if (parameterLengthMax < 0)
          parameterLengthMax = Long.MAX_VALUE / 2;

        Object uploadMax = getAttribute("caucho.multipart.form.upload-max");
        if (uploadMax instanceof Number)
          formUploadMax = ((Number) uploadMax).longValue();

        Object paramMax = getAttribute("caucho.multipart.form.parameter-length-max");
        if (paramMax instanceof Number)
          parameterLengthMax = ((Number) paramMax).longValue();

        // XXX: should this be an error?
        if (formUploadMax >= 0 && formUploadMax < contentLength) {
          setAttribute("caucho.multipart.form.error",
                       L.l("Multipart form upload of '{0}' bytes was too large.",
                           String.valueOf(contentLength)));
          setAttribute("caucho.multipart.form.error.size",
                       new Long(contentLength));

          return;
        }

        long fileUploadMax = -1;

        if (multipartConfig != null) {
          formUploadMax = multipartConfig.getMaxRequestSize();
          fileUploadMax = multipartConfig.getMaxFileSize();
        }

        if (multipartConfig != null
            && formUploadMax > 0
            && formUploadMax < contentLength)
          throw new IllegalStateException(L.l(
            "multipart form data request's Content-Length '{0}' is greater then configured in @MultipartConfig.maxRequestSize value: '{1}'",
            contentLength,
            formUploadMax));

        i += "boundary=".length();
        char ch = contentType.charAt(i);
        CharBuffer boundary = new CharBuffer();
        if (ch == '\'') {
          for (i++; i < length && contentType.charAt(i) != '\''; i++)
            boundary.append(contentType.charAt(i));
        }
        else if (ch == '\"') {
          for (i++; i < length && contentType.charAt(i) != '\"'; i++)
            boundary.append(contentType.charAt(i));
        }
        else {
          for (;
               i < length && (ch = contentType.charAt(i)) != ' ' &&
                 ch != ';';
               i++) {
            boundary.append(ch);
          }
        }

        _parts = new ArrayList<Part>();

        try {
          FormMultipartParser.parsePostData(form,
                                      _parts,
                                      getStream(false),
                                      boundary.toString(),
                                      this,
                                      javaEncoding,
                                      formUploadMax,
                                      fileUploadMax,
                                      parameterLengthMax);
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
          setAttribute("caucho.multipart.form.error", e.getMessage());
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }
  }

  Part createPart(String name,
                  String contentType,
                  Map<String, List<String>> headers,
                  PathImpl tempFile,
                  String value)
  {
    return new PartImpl(name, contentType, headers, tempFile, value);
  }

  public final void mergeParameters(Map<String, String[]> source,
                                    Map<String, String[]> target)
  {
    Set<Map.Entry<String, String[]>> sourceEntries = source.entrySet();

    for (Map.Entry<String, String[]> sourceEntry : sourceEntries) {
      String key = sourceEntry.getKey();

      String []sourceValues = sourceEntry.getValue();
      String []targetValues = target.get(key);
      String []newTargetValues;

      if (targetValues == null) {
        newTargetValues = sourceValues;
      } else {
        newTargetValues = new String[targetValues.length + sourceValues.length];
        System.arraycopy(targetValues,
                         0,
                         newTargetValues,
                         0,
                         targetValues.length);
        System.arraycopy(sourceValues,
                         0,
                         newTargetValues,
                         targetValues.length,
                         sourceValues.length);
      }

      target.put(key, newTargetValues);
    }
  }

  public void addCloseOnExit(PathImpl path)
  {
    if (_removeOnExit == null)
      _removeOnExit = new ArrayList<PathImpl>();

    _removeOnExit.add(path);
  }

  public ReadStream getStream(boolean isFlush) throws IOException
  {
    return getAbstractHttpRequest().getStream();
  }

  @Override
  public String getRealPath(String uri)
  {
    WebApp webApp = getWebApp();

    return webApp.getRealPath(uri);
  }

  /**
   * Returns the URL for the request
   */
  @Override
  public StringBuffer getRequestURL()
  {
    StringBuffer sb = new StringBuffer();

    sb.append(getScheme());
    sb.append("://");

    sb.append(getServerName());
    int port = getServerPort();

    if (port > 0 &&
        port != 80 &&
        port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }

  /**
   * Returns the real path of pathInfo.
   */
  @Override
  public String getPathTranslated()
  {
    // server/106w
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }

  @Override
  public boolean isTop()
  {
    return false;
  }

  //
  // session management
  //

  public abstract boolean isSessionIdFromCookie();

  public abstract String getSessionId();

  public abstract void setSessionId(String sessionId);

  /**
   * Returns the memory session.
   */
  @Override
  public HttpSession getMemorySession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }

  /**
   * Returns the current session, creating one if necessary.
   * Sessions are a convenience for keeping user state
   * across requests.
   */
  @Override
  public HttpSession getSession()
  {
    return getSession(true);
  }

  /**
   * Returns the current session.
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  @Override
  public HttpSession getSession(boolean create)
  {
    if (_session != null) {
      if (_session.isValid()) {
        return _session;
      }
    }
    else if (! create && _sessionIsLoaded) {
      return null;
    }

    _sessionIsLoaded = true;

    _session = createSession(create);

    return _session;
  }
  
  /**
   * Sets the session during session initialization
   */
  public void setSession(SessionImpl session)
  {
    _session = session;
  }

  /**
   * Returns the current session.
   *
   * @return the current session
   */
  public HttpSession getLoadedSession()
  {
    if (_session != null && _session.isValid())
      return _session;
    else
      return null;
  }
  
  @Override
  public String changeSessionId()
  {
    if (_session == null) {
      throw new IllegalStateException(L.l("changeSessionId requires an active session"));
    }
    
    String sessionId = getSessionManager().changeSessionId(this, _session);
    
    setSessionId(sessionId);
    
    return sessionId;
  }

  /**
   * Returns true if the HTTP request's session id refers to a valid
   * session.
   */
  @Override
  public boolean isRequestedSessionIdValid()
  {
    String id = getRequestedSessionId();

    if (id == null)
      return false;

    SessionImpl session = _session;

    if (session == null)
      session = (SessionImpl) getSession(false);

    return session != null && session.isValid() && session.getId().equals(id);
  }

  /**
   * Returns the current session.
   *
   * XXX: duplicated in RequestAdapter
   * @param request 
   *
   * @param isCreate true if a new session should be created
   *
   * @return the current session
   */
  private SessionImpl createSession(boolean isCreate)
  {
    SessionManager manager = getSessionManager();

    if (manager == null)
      return null;

    String id = getSessionId();

    long now = CurrentTime.getCurrentTime();

    SessionImpl session
      = manager.createSession(isCreate, this, id, now,
                              isSessionIdFromCookie());

    if (session != null
        && (id == null || ! session.getId().equals(id))) {
      setSessionId(session.getId());
    }

    // server/0123 vs TCK
    /*
    if (session != null)
      session.setAccessTime(now);
      */

    return session;
  }

  /**
   * Returns the session manager.
   */
  protected final SessionManager getSessionManager()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getSessionManager();
    else
      return null;
  }

  /**
   * Returns the session cookie.
   */
  protected final String getSessionCookie(SessionManager manager)
  {
    if (isSecure())
      return manager.getSSLCookieName();
    else
      return manager.getCookieName();
  }

  public int getSessionGroup()
  {
    return _sessionGroup;
  }

  void saveSession()
  {
    SessionImpl session = _session;
    if (session != null)
      session.save();
  }

  //
  // security
  //

  protected String getRunAs()
  {
    return null;
  }

  protected InvocationServlet getInvocation()
  {
    return null;
  }

  /**
   * Returns the next request in a chain.
   */
  public HttpServletRequest getRequest()
  {
    return null;
  }

  /**
   * @since Servlet 3.0
   */
  @Override
  public void login(String username, String password)
    throws ServletException
  {
    WebApp webApp = getWebApp();

    AuthenticatorRole auth = webApp.getConfiguredAuthenticator();

    if (auth == null) {
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));
    }

    // server/1aj0
    Login login = webApp.getLogin();

    if (login == null) {
      throw new ServletException(L.l("No login mechanism is configured for '{0}'", getWebApp()));
    }

    if (! login.isPasswordBased()) {
      throw new ServletException(L.l("Authentication mechanism '{0}' does not support password authentication", login));
    }

    removeAttribute(Login.LOGIN_USER_NAME);
    removeAttribute(Login.LOGIN_USER);
    removeAttribute(Login.LOGIN_PASSWORD);

    Principal principal = login.getUserPrincipal(this);

    if (principal != null)
      throw new ServletException(L.l("UserPrincipal object has already been established"));

    removeAttribute(Login.LOGIN_USER);
    setAttribute(Login.LOGIN_USER_NAME, username);
    setAttribute(Login.LOGIN_PASSWORD, password);
    
    try {
      login.login(this, getResponse(), false);
    }
    finally {
      removeAttribute(Login.LOGIN_USER_NAME);
      removeAttribute(Login.LOGIN_PASSWORD);
    }

    principal = login.getUserPrincipal(this);
    
    if (principal == null) {
      throw new ServletException("can't authenticate a user");
    }
  }
  
  @Override
  public boolean login(boolean isFail)
  {
    try {
      WebApp webApp = getWebApp();

      if (webApp == null) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no web-app found");

        getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }
      
      if (webApp.isSecure() && ! isSecure()) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, requires secure");

        getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }

      // If the authenticator can find the user, return it.
      Login login = webApp.getLogin();

      if (login != null) {
        Principal user = login.login(this, getResponse(), isFail);

        return (user != null);
      }
      else if (isFail) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no login module found for "
                    + webApp);

        getResponse().sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }
      else {
        // if a non-failure, then missing login is fine

        return false;
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }

  /**
   * Returns true if any authentication is requested
   */
  abstract public boolean isLoginRequested();

  abstract public void requestLogin();

  /**
   * @since Servlet 3.0
   */
  @Override
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    WebApp webApp = getWebApp();

    if (webApp == null)
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));

    // server/1aj{0,1}
    AuthenticatorRole auth = webApp.getConfiguredAuthenticator();

    if (auth == null)
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));

    Login login = webApp.getLogin();

    if (login == null) {
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));
    }

    Principal principal = login.login(this, response, true);

    if (principal != null) {
      return true;
    }

    return false;
  }

  /**
   * Returns the Principal representing the logged in user.
   */
  @Override
  public Principal getUserPrincipal()
  {
    requestLogin();

    WebApp webApp = getWebApp();
    if (webApp == null)
      return null;

    Principal user = null;
    
    // If the authenticator can find the user, return it.
    Login login = webApp.getLogin();

    if (login != null) {
      user = login.getUserPrincipal(this);

      if (user != null) {
        ResponseCaucho response = getResponse();
        
        if (response != null)
          response.setPrivateCache(true);
      }
      else {
        // server/123h, server/1920
        // distinguishes between setPrivateCache and setPrivateOrResinCache
        // _response.setPrivateOrResinCache(true);
      }
    }

    return user;
  }

  /**
   * Returns true if the user represented by the current request
   * plays the named role.
   *
   * @param role the named role to test.
   * @return true if the user plays the role.
   */
  @Override
  public boolean isUserInRole(String role)
  {
    InvocationServlet invocation = getInvocation();

    if (invocation == null) {
      if (getRequest() != null)
        return getRequest().isUserInRole(role);
      else
        return false;
    }

    HashMap<String,String> roleMap = invocation.getSecurityRoleMap();

    if (roleMap != null) {
      String linkRole = roleMap.get(role);

      if (linkRole != null)
        role = linkRole;
    }

    String runAs = getRunAs();

    if (runAs != null)
      return runAs.equals(role);

    WebApp webApp = getWebApp();

    Principal user = getUserPrincipal();

    if (user == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " isUserInRole request has no getUserPrincipal value");

      return false;
    }

    /*
    RoleMapManager roleManager
      = webApp != null ? webApp.getRoleMapManager() : null;

    if (roleManager != null) {
      Boolean result = roleManager.isUserInRole(role, user);

      if (result != null) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " userInRole(" + role + ")->" + result);

        return result;
      }
    }
    */

    Login login = webApp == null ? null : webApp.getLogin();

    boolean inRole = login != null && login.isUserInRole(user, role);

    if (log.isLoggable(Level.FINE)) {
      if (login == null)
        log.fine(this + " no Login for isUserInRole");
      else if (user == null)
        log.fine(this + " no user for isUserInRole");
      else if (inRole)
        log.fine(this + " " + user + " is in role: " + role);
      else
        log.fine(this + " failed " + user + " in role: " + role);
    }

    return inRole;
  }

  @Override
  public ConnectionTcp getSocketLink()
  {
    RequestHttpBase request = getAbstractHttpRequest();
    
    if (request != null)
      return request.getConnection();
    else
      return null;
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  //
  // lifecycle
  //

  protected void finishRequest()
    throws IOException
  {
    WebApp webApp = getWebApp();
    SessionImpl session = _session;

    if (webApp != null && webApp.isActive()) {
      if (session == null && getSessionId() != null) {
        session = (SessionImpl) getSession(false);
      }
    }

    if (session != null) {
      session.finishRequest();
    }

    if (_removeOnExit != null) {
      for (int i = _removeOnExit.size() - 1; i >= 0; i--) {
        PathImpl path = _removeOnExit.get(i);

        try {
          path.remove();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getRequestURL() + "]";
  }

  public class PartImpl implements Part
  {
    private String _name;
    private Map<String, List<String>> _headers;
    private PathImpl _tempFile;
    private String _value;
    private PathImpl _newPath;
    private String _contentType;

    private PartImpl(String name,
                     String contentType,
                     Map<String, List<String>> headers,
                     PathImpl tempFile,
                     String value)
    {
      Objects.requireNonNull(headers);
      
      _name = name;
      _contentType = contentType;
      _headers = headers;
      _tempFile = tempFile;
      _value = value;
    }

    @Override
    public void delete()
      throws IOException
    {
      if (_tempFile == null)
        throw new IOException(L.l("Part.delete() is not applicable to part '{0}':'{1}'", _name, _value));

      if (_newPath != null)
        _newPath.remove();

      _tempFile.remove();
    }

    @Override
    public String getContentType()
    {
      return _contentType;
    }

    @Override
    public String getHeader(String name)
    {
      name = name.toLowerCase(Locale.ENGLISH);

      List<String> values = _headers.get(name);

      if (values != null && values.size() > 0) {
        return values.get(0);
      }

      return null;
    }

    @Override
    public Collection<String> getHeaderNames()
    {
      return _headers.keySet();
    }

    @Override
    public Collection<String> getHeaders(String name)
    {
      name = name.toLowerCase(Locale.ENGLISH);
      
      Collection<String> headers = _headers.get(name);
      
      if (headers != null) {
        return headers;
      }
      else {
        return new ArrayList<>();
      }
    }

    @Override
    public InputStream getInputStream()
      throws IOException
    {
      if (_value != null) {
        String encoding = getCharacterEncoding();
        
        ByteArrayInputStream is;
        
        if (encoding != null)
          is = new ByteArrayInputStream(_value.getBytes(encoding));
        else
          is = new ByteArrayInputStream(_value.getBytes(UTF8));

        return is;
      } else if (_tempFile != null) {
        return _tempFile.openRead();
      } else {
        throw new java.lang.IllegalStateException();
      }
    }

    @Override
    public String getName()
    {
      return _name;
    }
    
    @Override
    public String getSubmittedFileName()
    {
      String header = getHeader("content-disposition");
      
      if (header == null) {
        return null;
      }
      
      int p = header.indexOf("filename");
      
      if (p < 0) {
        return null;
      }

      p = header.indexOf('=', p);
      int q = header.indexOf(';', p);
      
      if (p < 0) {
        return null;
      }
      
      String name;
      
      if (q > 0) {
        name = header.substring(p + 1, q).trim();
      }
      else {
        name = header.substring(p + 1).trim();
      }
      
      if (name.startsWith("\"")) {
        name = name.substring(1);
      }
      
      if (name.endsWith("\"")) {
        name = name.substring(0, name.length() - 1);
      }
      
      return name;
    }

    @Override
    public long getSize()
    {
      if (_tempFile != null)
        return _tempFile.getLength();
      else if (_value != null) {
        return _value.length();
      }
      else {
        return -1;
      }
    }

    @Override
    public void write(String fileName)
      throws IOException
    {
      if (_newPath != null)
        throw new IOException(L.l(
          "Contents of part '{0}' has already been written to '{1}'",
          _name,
          _newPath));

      if (_tempFile == null)
        throw new IOException(L.l(
          "Part.write() is not applicable to part '{0}':'{1}'",
          _name,
          _value));

      InvocationServlet invocation = getInvocation();

      MultipartConfigElement mc = invocation.getMultipartConfig();
      String location = mc.getLocation().replace('\\', '/');
      fileName = fileName.replace('\\', '/');

      String file;

      if (location.charAt(location.length() -1) != '/' && fileName.charAt(fileName.length() -1) != '/')
        file = location + '/' + fileName;
      else
        file = location + fileName;

      _newPath = Vfs.lookup(file);

      if (_newPath.exists())
        throw new IOException(L.l("File '{0}' already exists.", _newPath));

      PathImpl parent = _newPath.getParent();

      if (! parent.exists())
        if (! parent.mkdirs())
          throw new IOException(L.l("Unable to create path '{0}'. Check permissions.", parent));

      if (! _tempFile.renameTo(_newPath)) {
        WriteStream out = null;

        try {
          out = _newPath.openWrite();

          _tempFile.writeToStream(out);

          out.flush();

          out.close();
        } catch (IOException e) {
          log.log(Level.SEVERE, L.l("Cannot write contents of '{0}' to '{1}'", _tempFile, _newPath), e);

          throw e;
        } finally {
          if (out != null)
            out.close();
        }
      }
    }

    public Object getValue()
    {
      if (_value != null)
        return _value;

      if (_tempFile != null)
        return _tempFile;

      throw new IllegalStateException();
    }
    
    public String toString()
    {
      return getClass().getSimpleName() + "[" + _name + "]";
    }
  }
}
