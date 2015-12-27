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

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestAttributeEvent;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.dispatch.Invocation;
import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.security.Login;
import com.caucho.v5.http.security.LoginBase;
import com.caucho.v5.http.session.SessionManager;
import com.caucho.v5.http.webapp.RequestDispatcherImpl;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.listen.ConnectionSocket;
import com.caucho.v5.network.listen.NextState;
import com.caucho.v5.network.listen.RequestProtocol;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.L10N;
import com.caucho.v5.util.NullEnumeration;
import com.caucho.v5.vfs.Path;
import com.caucho.v5.vfs.ReadStream;

/**
 * User facade for http requests.
 */
public final class RequestServlet extends RequestCauchoBase
  implements RequestCaucho, RequestCache
{
  private static final Logger log
    = Logger.getLogger(RequestServlet.class.getName());

  private static final L10N L = new L10N(RequestServlet.class);

  private RequestHttpBase _request;

  private final ResponseServlet _response;

  private Boolean _isSecure;

  private InvocationServlet _invocation;

  // session/cookies
  private Cookie []_cookiesIn;

  private boolean _varyCookies;   // True if the page depends on cookies
  private boolean _hasCookie;

  private boolean _isSessionIdFromCookie;

  // security
  private String _runAs;
  private boolean _isLoginRequested;

  // input stream management
  private boolean _hasReader;
  private boolean _hasInputStream;

  // servlet attributes
  private HashMapImpl<String,Object> _attributes;

  // proxy caching
  private boolean _isSyntheticCacheHeader;

  // comet
  private long _asyncTimeout;
  private AsyncContextImpl _asyncContext;

  private ArrayList<Path> _closeOnExit;

  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param request
   */
  public RequestServlet(RequestHttpBase request)
  {
    _request = request;

    _response = new ResponseServlet(request.getAbstractHttpResponse(),
                                    this);
                                    
  }

  @Override
  public ResponseServlet getResponse()
  {
    return _response;
  }

  //
  // ServletRequest methods
  //

  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  @Override
  public String getProtocol()
  {
    return _request.getProtocol();
  }

  /**
   * Returns the request scheme, e.g. "http"
   */
  @Override
  public String getScheme()
  {
    String scheme = _request.getScheme();

    // server/12j2, server/1kkg
    if ("http".equals(scheme) || "https".equals(scheme))
      return isSecure() ? "https" : "http";
    else
      return scheme;
  }

  /**
   * Returns the server name handling the request.  When using virtual hosts,
   * this returns the virtual host name, e.g. "vhost1.caucho.com".
   *
   * This call returns the host name as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not contain the host that Resin is
   * actually listening on.
   */
  @Override
  public String getServerName()
  {
    RequestHttpBase request = _request;

    return request != null ? request.getServerName() : null;
  }

  /**
   * Returns the server port used by the client, e.g. 80.
   *
   * This call returns the port number as the client sees it, which means that
   * if ipchains, load balancing, or proxying is involved this call returns the
   * correct call for forming urls, but may not return the actual port that
   * Resin is listening on.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for
   * that purpose.
   */
  @Override
  public int getServerPort()
  {
    RequestHttpBase request = _request;
    
    return request != null ? request.getServerPort() : 0;
  }

  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  @Override
  public String getRemoteAddr()
  {
    RequestHttpBase request = _request;

    return request != null ? request.getRemoteAddr() : null;
  }

  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  @Override
  public String getRemoteHost()
  {
    RequestHttpBase request = _request;

    return request != null ? request.getRemoteHost() : null;
  }

  /**
   * Returns the port of the remote host, i.e. the client browser.
   *
   * @since 2.4
   */
  @Override
  public int getRemotePort()
  {
    RequestHttpBase request = _request;

    return request != null ? request.getRemotePort() : 0;
  }

  /**
   * This call returns the ip of the host actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  @Override
  public String getLocalAddr()
  {
    return _request.getLocalHost();
  }

  /**
   * Returns the IP address of the local host, i.e. the server.
   *
   * This call returns the name of the host actually used to connect to the
   * Resin server,  which means that if ipchains, load balancing, or proxying
   * is involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  @Override
  public String getLocalName()
  {
    return _request.getLocalHost();
  }

  /**
   * Returns the port of the local host.
   *
   * This call returns the port number actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct port for
   * forming urls.
   *
   * This call should not be used to test for an ssl connection
   * (getServerPort() == 443), {@link #isSecure()} is provided for that purpose.
   *
   * @since 2.4
   */
  @Override
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }

  /**
   * Overrides the character encoding specified in the request.
   * <code>setCharacterEncoding</code> must be called before calling
   * <code>getReader</code> or reading any parameters.
   */
  @Override
  public void setCharacterEncoding(String encoding)
    throws java.io.UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  @Override
  public ServletInputStream getInputStream()
    throws IOException
  {    
    if (_hasReader) {
      throw new IllegalStateException(L.l("getInputStream() can't be called after getReader()"));
    }

    _hasInputStream = true;

    throw new UnsupportedOperationException();
    //return _request.getInputStream();
  }

  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  @Override
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    if (_hasInputStream)
      throw new IllegalStateException(L.l("getReader() can't be called after getInputStream()"));

    _hasReader = true;

    return _request.getReader();
  }

  /**
   * Returns the character encoding of the POSTed data.
   */
  @Override
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }

  /**
   * Returns the content length of the data.  This value may differ from
   * the actual length of the data.  Newer browsers
   * supporting HTTP/1.1 may use "chunked" encoding which does
   * not make the content length available.
   *
   * <p>The upshot is, rely on the input stream to end when the data
   * completes.
   */
  @Override
  public int getContentLength()
  {
    return _request.getContentLength();
  }
  
  @Override
  public long getContentLengthLong()
  {
    return _request.getContentLengthLong();
  }

  /**
   * Returns the request's mime-type.
   */
  @Override
  public String getContentType()
  {
    return _request.getContentType();
  }

  /**
   * Returns the request's preferred locale, based on the Accept-Language
   * header.  If unspecified, returns the server's default locale.
   */
  @Override
  public Locale getLocale()
  {
    return _request.getLocale();
  }

  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  @Override
  public Enumeration<Locale> getLocales()
  {
    return _request.getLocales();
  }

  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  @Override
  public boolean isSecure()
  {
    if (_isSecure != null)
      return _isSecure;

    RequestHttpBase request = _request;

    if (request == null) {
      return false;
    }
    
    WebApp webApp = getWebApp();
      
    if (webApp != null) {
      Boolean isSecure = webApp.isRequestSecure();
        
      if (isSecure != null)
        return isSecure;
    }
      
    return request.isSecure();
  }
  
  @Override
  public ConnectionSocket getSocketLink()
  {
    return _request.getConnection();
  }

  //
  // request attributes
  //

  /**
   * Returns the value of the named request attribute.
   *
   * @param name the attribute name.
   *
   * @return the attribute value.
   */
  @Override
  public Object getAttribute(String name)
  {
    HashMapImpl<String,Object> attributes = _attributes;
    
    if (attributes != null)
      return attributes.get(name);
    else if (isSecure()) {
      _attributes = new HashMapImpl<String,Object>();
      attributes = _attributes;
      _request.initAttributes(this);

      return attributes.get(name);
    }
    else
      return null;
  }
  
  private boolean isAttributesEmpty()
  {
    return _attributes == null;
  }

  /**
   * Returns an enumeration of the request attribute names.
   */
  @Override
  public Enumeration<String> getAttributeNames()
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes != null) {
      return Collections.enumeration(attributes.keySet());
    }
    else if (isSecure()) {
      _attributes = new HashMapImpl<String,Object>();
      attributes = _attributes;
      _request.initAttributes(this);

      return Collections.enumeration(attributes.keySet());
    }
    else
      return NullEnumeration.create();
  }
  
  /**
   * Sets the value of the named request attribute.
   *
   * @param name the attribute name.
   * @param value the new attribute value.
   */
  @Override
  public void setAttribute(String name, Object value)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (value != null) {
      if (attributes == null) {
        attributes = new HashMapImpl<String,Object>();
        _attributes = attributes;
        _request.initAttributes(this);
      }

      Object oldValue = attributes.put(name, value);

      WebApp webApp = getWebApp();

      if (webApp != null) {
        for (ServletRequestAttributeListener listener
               : webApp.getRequestAttributeListeners()) {
          ServletRequestAttributeEvent event;

          if (oldValue != null) {
            event = new ServletRequestAttributeEvent(webApp, this,
                                                     name, oldValue);

            listener.attributeReplaced(event);
          }
          else {
            event = new ServletRequestAttributeEvent(webApp, this,
                                                     name, value);

            listener.attributeAdded(event);
          }
        }
      }
    }
    else
      removeAttribute(name);
  }

  /**
   * Removes the value of the named request attribute.
   *
   * @param name the attribute name.
   */
  @Override
  public void removeAttribute(String name)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes == null)
      return;

    Object oldValue = attributes.remove(name);

    WebApp webApp = getWebApp();
    
    if (webApp == null)
      return;

    for (ServletRequestAttributeListener listener
           : webApp.getRequestAttributeListeners()) {
      ServletRequestAttributeEvent event;

      event = new ServletRequestAttributeEvent(webApp, this,
                                               name, oldValue);

      listener.attributeRemoved(event);
    }

    /*
    if (oldValue instanceof ScopeRemoveListener) {
      ((ScopeRemoveListener) oldValue).removeEvent(this, name);
    }
    */
  }

  //
  // request dispatching
  //

  /**
   * Returns a request dispatcher for later inclusion or forwarding.  This
   * is the servlet API equivalent to SSI includes.  <code>uri</code>
   * is relative to the request URI.  Absolute URIs are relative to
   * the application prefix (<code>getContextPath()</code>).
   *
   * <p>If <code>getRequestURI()</code> is /myapp/dir/test.jsp and the
   * <code>uri</code> is "inc.jsp", the resulting page is
   * /myapp/dir/inc.jsp.

   * <code><pre>
   *   RequestDispatcher disp;
   *   disp = getRequestDispatcher("inc.jsp?a=b");
   *   disp.include(request, response);
   * </pre></code>
   *
   * @param path path relative to <code>getRequestURI()</code>
   * (including query string) for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
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

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public ServletContext getServletContext()
  {
    InvocationServlet invocation = _invocation;

    if (invocation != null)
      return invocation.getWebApp();
    else
      return null;
  }

  /**
   * Returns the servlet response for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public ServletResponse getServletResponse()
  {
    return _response;
  }

  //
  // HttpServletRequest APIs
  //

  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  @Override
  public String getMethod()
  {
    return _request.getMethod();
  }

  /**
   * Returns the URI for the request
   */
  @Override
  public String getRequestURI()
  {
    if (_invocation != null)
      return _invocation.getRawURI();
    else
      return "";
  }

  /**
   * Returns the URI for the page.  getPageURI and getRequestURI differ
   * for included files.  getPageURI gets the URI for the included page.
   * getRequestURI returns the original URI.
   */
  public String getPageURI()
  {
    return _invocation.getRawURI();
  }

  /**
   * Returns the context part of the uri.  The context part is the part
   * that maps to an webApp.
   */
  public String getContextPath()
  {
    if (_invocation != null)
      return _invocation.getContextPath();
    else
      return "";
  }

  /**
   * Returns the context part of the uri.  For included files, this will
   * return the included context-path.
   */
  public String getPageContextPath()
  {
    return getContextPath();
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the original
   * request.
   */
  public String getServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri mapped to the servlet for the current
   * page.
   */
  public String getPageServletPath()
  {
    if (_invocation != null)
      return _invocation.getServletPath();
    else
      return "";
  }

  /**
   * Returns the portion of the uri after the servlet path for the original
   * request.
   */
  public String getPathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
  }

  /**
   * Returns the portion of the uri after the servlet path for the current
   * page.
   */
  public String getPagePathInfo()
  {
    if (_invocation != null)
      return _invocation.getPathInfo();
    else
      return null;
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

    if (port > 0
        && port != 80
        && port != 443) {
      sb.append(":");
      sb.append(port);
    }

    sb.append(getRequestURI());

    return sb;
  }

  /**
   * @deprecated As of JSDK 2.1
   */
  @Override
  public String getRealPath(String path)
  {
    if (path == null)
      return null;
    if (path.length() > 0 && path.charAt(0) == '/')
      return _invocation.getWebApp().getRealPath(path);

    String uri = getPageURI();
    String context = getPageContextPath();
    if (context != null)
      uri = uri.substring(context.length());

    int p = uri.lastIndexOf('/');
    if (p >= 0)
      path = uri.substring(0, p + 1) + path;

    return _invocation.getWebApp().getRealPath(path);
  }

  /**
   * Returns the real path of pathInfo.
   */
  @Override
  public String getPathTranslated()
  {
    String pathInfo = getPathInfo();

    if (pathInfo == null)
      return null;
    else
      return getRealPath(pathInfo);
  }

  /**
   * Returns the current page's query string.
   */
  @Override
  public String getQueryString()
  {
    if (_invocation != null)
      return _invocation.getQueryString();
    else
      return null;
  }

  /**
   * Returns the current page's query string.
   */
  public String getPageQueryString()
  {
    return getQueryString();
  }

  //
  // header management
  //

  /**
   * Returns the first value for a request header.
   *
   * <p/>Corresponds to CGI's <code>HTTP_*</code>
   *
   * <code><pre>
   * String userAgent = request.getHeader("User-Agent");
   * </pre></code>
   *
   * @param name the header name
   * @return the header value
   */
  @Override
  public String getHeader(String name)
  {
    return _request.getHeader(name);
  }

  /**
   * Returns all the values for a request header.  In some rare cases,
   * like cookies, browsers may return multiple headers.
   *
   * @param name the header name
   * @return an enumeration of the header values.
   */
  @Override
  public Enumeration<String> getHeaders(String name)
  {
    return _request.getHeaders(name);
  }

  /**
   * Returns an enumeration of all headers sent by the client.
   */
  @Override
  public Enumeration<String> getHeaderNames()
  {
    return _request.getHeaderNames();
  }

  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
  @Override
  public int getIntHeader(String name)
  {
    return _request.getIntHeader(name);
  }

  /**
   * Converts a date header to milliseconds since the epoch.
   *
   * <pre><code>
   * long mod = request.getDateHeader("If-Modified-Since");
   * </code></pre>
   *
   * @param name the header name
   * @return the header value converted to an date
   */
  @Override
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }

  //
  // session/cookie management
  //

  /**
   * Returns an array of all cookies sent by the client.
   */
  @Override
  public Cookie []getCookies()
  {
    Cookie []cookiesIn = _cookiesIn;
    
    if (cookiesIn == null) {
      RequestHttpBase request = _request;
      
      if (request == null) {
        return null;
      }
      
      if (true) throw new UnsupportedOperationException();
      cookiesIn = new Cookie[0]; // request.getCookies();

      SessionManager sessionManager = getSessionManager();
      String sessionCookieName = getSessionCookie(sessionManager);

      for (int i = 0; i < cookiesIn.length; i++) {
        Cookie cookie = cookiesIn[i];

        if (cookie.getName().equals(sessionCookieName)
          && sessionManager.isSecure()) {
          cookie.setSecure(true);
          break;
        }
      }
      
      _cookiesIn = cookiesIn;

      /*
      // The page varies depending on the presense of any cookies
      setVaryCookie(null);

      // If any cookies actually exist, the page is not anonymous
      if (_cookiesIn != null && _cookiesIn.length > 0)
        setHasCookie();
      */
    }

    if (cookiesIn != null && cookiesIn.length > 0) {
      return cookiesIn;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the named cookie from the browser
   */
  @Override
  public Cookie getCookie(String name)
  {
    /*
    // The page varies depending on the presense of any cookies
    setVaryCookie(name);
    */

    return findCookie(name);
  }

  private Cookie findCookie(String name)
  {
    Cookie []cookies = getCookies();

    if (cookies == null)
      return null;

    int length = cookies.length;
    for (int i = 0; i < length; i++) {
      Cookie cookie = cookies[i];

      if (cookie.getName().equals(name)) {
        setHasCookie();
        return cookie;
      }
    }

    return null;
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  @Override
  public String getRequestedSessionId()
  {
    SessionManager manager = getSessionManager();

    if (manager != null && manager.isEnableSessionCookies()) {
      setVaryCookie(getSessionCookie(manager));

      String id = findSessionIdFromCookie();

      if (id != null) {
        _isSessionIdFromCookie = true;
        setHasCookie();
        return id;
      }
    }

    String id = findSessionIdFromUrl();
    if (id != null) {
      return id;
    }

    if (manager != null && manager.isEnableSessionCookies())
      return null;
    else
      return _request.findSessionIdFromConnection();
  }

  /**
   * Returns the session id in the HTTP request cookies.
   * Because the webApp might use the cookie to change
   * the page contents, the caching sets vary: JSESSIONID.
   */
  protected String findSessionIdFromCookie()
  {
    SessionManager manager = getSessionManager();

    if (manager == null || ! manager.isEnableSessionCookies())
      return null;

    Cookie cookie = getCookie(getSessionCookie(manager));

    if (cookie != null) {
      _isSessionIdFromCookie = true;
      return cookie.getValue();
    }
    else
      return null;
  }

  @Override
  public boolean isSessionIdFromCookie()
  {
    return _isSessionIdFromCookie;
  }

  @Override
  public String getSessionId()
  {
    String sessionId = getResponse().getSessionId();

    if (sessionId != null)
      return sessionId;
    else
      return getRequestedSessionId();
  }

  @Override
  public void setSessionId(String sessionId)
  {
    getResponse().setSessionId(sessionId);
  }

  /**
   * Returns the session id in the HTTP request from the url.
   */
  private String findSessionIdFromUrl()
  {
    // server/1319
    // setVaryCookie(getSessionCookie(manager));

    String id = _invocation != null ? _invocation.getSessionId() : null;
    if (id != null)
      setHasCookie();

    return id;
  }

  /**
   * Returns true if the current sessionId came from a cookie.
   */
  @Override
  public boolean isRequestedSessionIdFromCookie()
  {
    return findSessionIdFromCookie() != null;
  }

  /**
   * Returns true if the current sessionId came from the url.
   */
  @Override
  public boolean isRequestedSessionIdFromURL()
  {
    return findSessionIdFromUrl() != null;
  }

  /**
   * @deprecated
   */
  @Override
  public boolean isRequestedSessionIdFromUrl()
  {
    return isRequestedSessionIdFromURL();
  }

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  public String getRequestedSessionIdNoVary()
  {
    boolean varyCookies = _varyCookies;
    boolean hasCookie = _hasCookie;
    boolean privateCache = _response.getPrivateCache();

    String id = getRequestedSessionId();

    _varyCookies = varyCookies;
    _hasCookie = hasCookie;
    _response.setPrivateOrResinCache(privateCache);

    return id;
  }

  //
  // security
  //

  @Override
  protected String getRunAs()
  {
    return _runAs;
  }

  /**
   * Gets the authorization type
   */
  @Override
  public String getAuthType()
  {
    Object login = getAttribute(LoginBase.LOGIN_USER_NAME);

    if (login instanceof X509Certificate)
      return HttpServletRequest.CLIENT_CERT_AUTH;

    WebApp webApp = getWebApp();

    if (webApp != null && webApp.getLogin() != null && getUserPrincipal() != null)
      return webApp.getLogin().getAuthType();
    else
      return null;
  }

  /**
   * Returns the login for the request.
   */
  protected Login getLogin()
  {
    WebApp webApp = getWebApp();

    if (webApp != null)
      return webApp.getLogin();
    else
      return null;
  }
  /**
   * Returns true if any authentication is requested
   */
  @Override
  public boolean isLoginRequested()
  {
    return _isLoginRequested;
  }

  @Override
  public void requestLogin()
  {
    _isLoginRequested = true;
  }

  /**
   * Gets the remote user from the authorization type
   */
  @Override
  public String getRemoteUser()
  {
    Principal principal = getUserPrincipal();

    if (principal != null)
      return principal.getName();
    else
      return null;
  }

  /**
   * Internal logging return to get the remote user.  If the request already
   * knows the user, get it, otherwise just return null.
   */
  public String getRemoteUser(boolean create)
  {
    /*
    if (getSession(false) == null)
      return null;
    */
    
    if (isAttributesEmpty() && ! create) {
      return null;
    }

    Principal user = (Principal) getAttribute(LoginBase.LOGIN_USER);

    if (user == null && create)
      user = getUserPrincipal();

    if (user != null)
      return user.getName();
    else
      return null;
  }

  /**
   * Logs out the principal.
   */
  @Override
  public void logout()
  {
    Login login = getLogin();

    if (login != null) {
      login.logout(getUserPrincipal(), this, getResponse());
    }
  }

  /**
   * Clear the principal from the request object.
   */
  public void logoutUserPrincipal()
  {
    // XXX:
    /*
    if (_session != null)
      _session.logout();
    */
  }

  /**
   * Sets the overriding role.
   */
  public String runAs(String role)
  {
    String oldRunAs = _runAs;

    _runAs = role;

    return oldRunAs;
  }

  public void setSecure(Boolean isSecure)
  {
    // server/12ds
    _isSecure = isSecure;
  }

  //
  // deprecated
  //

  public ReadStream getStream()
    throws IOException
  {
    return _request.getStream();
  }

  @Override
  public ReadStream getStream(boolean isFlush)
    throws IOException
  {
    return _request.getStream(isFlush);
  }

  public int getRequestDepth(int depth)
  {
    return depth;
  }

  public void setHeader(String key, String value)
  {
    _request.setHeader(key, value);
  }

  @Override
  public void setSyntheticCacheHeader(boolean isSynthetic)
  {
    _isSyntheticCacheHeader = isSynthetic;
  }

  @Override
  public boolean isSyntheticCacheHeader()
  {
    return _isSyntheticCacheHeader;
  }

  /**
   * Called if the page depends on a cookie.  If the cookie is null, then
   * the page depends on all cookies.
   *
   * @param cookie the cookie the page depends on.
   */
  public void setVaryCookie(String cookie)
  {
    _varyCookies = true;

    // XXX: server/1315 vs 2671
    // _response.setPrivateOrResinCache(true);
  }

  /**
   * Returns true if the page depends on cookies.
   */
  public boolean getVaryCookies()
  {
    return _varyCookies;
  }

  /**
   * Set when the page actually has a cookie.
   */
  public void setHasCookie()
  {
    _hasCookie = true;

    // XXX: 1171 vs 1240
    // _response.setPrivateOrResinCache(true);
  }

  /**
   * True if this page uses cookies.
   */
  public boolean getHasCookie()
  {
    if (_hasCookie)
      return true;
    else if (_invocation != null)
      return _invocation.getSessionId() != null;
    else
      return false;
  }

  public boolean isTop()
  {
    return true;
  }

  public boolean isComet()
  {
    return _request.isSuspend();
  }
  
  @Override
  public void completeCache()
  {
    _request.completeAsync();
  }

  /**
   * Adds a file to be removed at the end.
   */
  public void addCloseOnExit(Path path)
  {
    if (_closeOnExit == null)
      _closeOnExit = new ArrayList<Path>();

    _closeOnExit.add(path);
  }

  @Override
  public boolean isDuplex()
  {
    return _request.isDuplex();
  }

  @Override
  public void killKeepalive(String reason)
  {
    _request.killKeepalive(reason);
  }

  @Override
  public boolean isConnectionClosed()
  {
    return _request.isConnectionClosed();
  }

  public ConnectionSocket getConnection()
  {
    return _request.getConnection();
  }

  //
  // HttpServletRequestImpl methods
  //
  @Override
  public RequestHttpBase getAbstractHttpRequest()
  {
    return _request;
  }

  @Override
  public boolean isSuspend()
  {
    return _request.isSuspend();
  }

  @Override
  public boolean hasRequest()
  {
    return _request.hasRequest();
  }

  @Override
  public void setInvocation(Invocation invocation)
  {
    _invocation = (InvocationServlet) invocation;
  }

  @Override
  public InvocationServlet getInvocation()
  {
    return _invocation;
  }

  public long getStartTime()
  {
    return _request.getStartTime();
  }

  @Override
  public NextState service()
  {
    try {
      getInvocation().service(this, getResponse());
    } catch (Exception e) {
      log.log(Level.WARNING, e.toString(), e);
    }
    
    return NextState.READ;
  }

  @Override
  public NextState resume()
    throws Exception
  {
    if (isAsyncComplete()) {
      return NextState.READ;
    }
    
    AsyncContextImpl asyncContext = getAsyncContext();
    
    // completeAsync(); // servlet/1lc3
    
    ServletContext webApp = asyncContext.getDispatchContext();
    String url = asyncContext.getDispatchPath();
    
    /* XXX:
    if (_tcpConn != null && _tcpConn.isAsyncComplete()) {
      return false;
    }
    */

    if (url != null) {
      if (webApp == null) {
        webApp = getWebApp();
      }
      
      RequestDispatcherImpl disp
        = (RequestDispatcherImpl) webApp.getRequestDispatcher(url);

      if (disp != null) {
        disp.dispatchResume(this, getResponse());
        
        return NextState.CLOSE;
      }
    }
    
    return service();
  }
  
  public void finishInvocation()
  {
    // server/11d4
    /*
    AsyncContextImpl asyncContext = _asyncContext;

    if (asyncContext != null)
      asyncContext.onComplete();
      */

    RequestHttpBase request = _request;
    
    if (request != null) {
      request.finishInvocation();
    }
  }
  
  @Override
  public void setCipherSuite(String cipher)
  {
    setAttribute("javax.servlet.request.cipher_suite", cipher);
  }
  
  @Override
  public void setCipherKeySize(int keySize)
  {
    setAttribute("javax.servlet.request.key_size", new Integer(keySize));
  }
  
  @Override
  public void setCipherCertificate(X509Certificate []certs)
  {
    if (certs == null || certs.length == 0) {
      return;
    }
    
    setAttribute("javax.servlet.request.X509Certificate",
                 certs);

    setAttribute(LoginBase.LOGIN_USER, certs[0].getSubjectDN());
  }

  //
  // servlet 3.0 async support
  //

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncStarted()
  {
    AsyncContextImpl asyncContext = _asyncContext;

    if (asyncContext == null) {
      return false;
    }

    return asyncContext.isAsyncStarted();
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  @Override
  public boolean isAsyncSupported()
  {
    InvocationServlet invocation = _invocation;
    
    if (invocation != null)
      return invocation.isAsyncSupported();
    else
      return false;
  }
  
  @Override
  public boolean isAsyncComplete()
  {
    AsyncContextImpl asyncContext = getAsyncContext();
    
    if (asyncContext != null) {
      return asyncContext.isAsyncComplete();
    }
    else {
      return false;
    }
  }


  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync()
  {
    return startAsync(this, _response);
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContext startAsync(ServletRequest request,
                                 ServletResponse response)
  {

    if (! isAsyncSupported()) {
      throw new IllegalStateException(L.l("The servlet '{0}' at '{1}' does not support async because the servlet or one of the filters does not support asynchronous mode.  The servlet should be annotated with a @WebServlet(asyncSupported=true) annotation or have a <async-supported> tag in the web.xml.",
                                          getServletName(), getServletPath()));
    }

    if (_request.isSuspend()) {
      // servlet/1lci (tck)
      throw new IllegalStateException(L.l("startAsync may not be called twice on the same dispatch."));
    }

    boolean isOriginal = (request == this && response == _response);
    
    _asyncContext = new AsyncContextImpl(_request);
    
    if (_asyncTimeout > 0) {
      _asyncContext.setTimeout(_asyncTimeout);
    }

    /*
    if (_asyncContext == null) {
      _asyncContext = new AsyncContextImpl(_request);
      
      if (_asyncTimeout > 0) {
        _asyncContext.setTimeout(_asyncTimeout);
      }
    }
    else {
      // _asyncContext.restart();
    }
    */
    
    _asyncContext.start(request, response, isOriginal);

    return _asyncContext;
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  @Override
  public AsyncContextImpl getAsyncContext()
  {
    if (_asyncContext != null)
      return _asyncContext;
    else
      throw new IllegalStateException(L.l("getAsyncContext() must be called after asyncStarted() has started a new AsyncContext."));
  }

  //
  // WebSocket
  //
  
  /**
   * Starts duplex mode.
   */
  /*
  private DuplexController startDuplex(DuplexListener handler)
  {
    DuplexController duplex = _request.startDuplex(handler);
    
    _response.onStartDuplex();
    
    return duplex;
  }
  */
  
  /**
   * Starts duplex mode.
   */
  public void startDuplex(RequestProtocol request)
  {
    _request.startDuplex(request);
    
    _response.onStartDuplex();
  }

  public int getAvailable()
    throws IOException
  {
    if (_request != null)
      return _request.getAvailable();
    else
      return -1;
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.REQUEST;
  }

  @Override
  public void finishRequest()
    throws IOException
  {
    AsyncContextImpl async = _asyncContext;
    _asyncContext = null;

    /* server/1ld5
    if (comet != null) {
      comet.onComplete();
    }
    */
    
    if (async != null) {
      async.onComplete();
    }

    super.finishRequest();

    // ioc/0a10
    cleanup();

    if (_closeOnExit != null) {
      for (int i = _closeOnExit.size() - 1; i >= 0; i--) {
        Path path = _closeOnExit.get(i);

        try {
          path.remove();
        } catch (Throwable e) {
          log.log(Level.FINE, e.toString(), e);
        }
      }
    }
    
    _cookiesIn = null;
    _request = null;

    // server/1lg0
    _response.closeImpl();
  }

  public void cleanup()
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes != null) {
      for (Map.Entry<String,Object> entry : attributes.entrySet()) {
        Object value = entry.getValue();

        /*
        if (value instanceof ScopeRemoveListener) {
          ((ScopeRemoveListener) value).removeEvent(this, entry.getKey());
        }
        */
      }
    }
  }

  //
  // XXX: unsorted
  //

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    if (_invocation != null) {
      return _invocation.getServletName();
    }
    else
      return null;
  }

  public final HttpContainer getServer()
  {
    return _request.getHttp();
  }

  /**
   * Returns the invocation's webApp.
   */
  public final WebApp getWebApp()
  {
    if (_invocation != null)
      return _invocation.getWebApp();
    else
      return null;
  }

  public boolean isClosed()
  {
    return _request == null;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }

  /**
   * @param asyncContextImpl
   */
  public void startAsync(AsyncContextImpl asyncContextImpl)
  {
    // TODO Auto-generated method stub
    
  }
}
