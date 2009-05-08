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

package com.caucho.server.connection;

import com.caucho.server.connection.ConnectionCometController;
import com.caucho.server.port.TcpConnection;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.L10N;
import com.caucho.vfs.ReadStream;

import java.io.BufferedReader;
import java.io.IOException;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * User facade for http requests.
 */
public class HttpServletRequestImpl implements CauchoRequest
{
  private static final Logger log
    = Logger.getLogger(HttpServletRequestImpl.class.getName());

  private static final L10N L = new L10N(HttpServletRequestImpl.class);

  private AbstractHttpRequest _request;

  private HttpServletResponseImpl _response;

  private ConnectionCometController _comet;

  private boolean _isSuspend;
  
  /**
   * Create a new Request.  Because the actual initialization occurs with
   * the start() method, this just allocates statics.
   *
   * @param request
   */
  public HttpServletRequestImpl(AbstractHttpRequest request)
  {
    _request = request;
  }

  public void setResponse(HttpServletResponseImpl response)
  {
    _response = response;
  }

  //
  // ServletRequest methods
  //

  /**
   * Returns the prococol, e.g. "HTTP/1.1"
   */
  public String getProtocol()
  {
    return _request.getProtocol();
  }

  /**
   * Returns the request scheme, e.g. "http"
   */
  public String getScheme()
  {
    return _request.getScheme();
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
  public String getServerName()
  {
    return _request.getServerName();
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
  public int getServerPort()
  {
    return _request.getServerPort();
  }

  /**
   * Returns the IP address of the remote host, i.e. the client browser.
   */
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }

  /**
   * Returns the DNS hostname of the remote host, i.e. the client browser.
   */
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }

  /**
   * Returns the port of the remote host, i.e. the client browser.
   *
   * @since 2.4
   */
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }

  /**
   * This call returns the ip of the host actually used to connect to the Resin
   * server,  which means that if ipchains, load balancing, or proxying is
   * involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }

  /**
   * Returns the IP address of the local host, i.e. the server.
   *
   * This call returns the name of the host actaully used to connect to the
   * Resin server,  which means that if ipchains, load balancing, or proxying
   * is involved this call <i>does not</i> return the correct host for
   * forming urls.
   *
   * @since 2.4
   */
  public String getLocalName()
  {
    return _request.getLocalAddr();
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
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }

  /**
   * Overrides the character encoding specified in the request.
   * <code>setCharacterEncoding</code> must be called before calling
   * <code>getReader</code> or reading any parameters.
   */
  public void setCharacterEncoding(String encoding)
    throws java.io.UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  /**
   * Returns a form parameter.  When the form contains several parameters
   * of the same name, <code>getParameter</code> returns the first.
   *
   * <p>For example, calling <code>getParameter("a")</code> with the
   * the query string <code>a=1&a=2</code> will return "1".
   *
   * @param name the form parameter to return
   * @return the form value or null if none matches.
   */
  public String getParameter(String name)
  {
    return _request.getParameter(name);
  }

  /**
   * Returns all values of a form parameter.
   *
   * <p>For example, calling <code>getParameterValues("a")</code>
   * with the the query string <code>a=1&a=2</code> will
   * return ["1", "2"].
   *
   * @param name the form parameter to return
   * @return an array of matching form values or null if none matches.
   */
  public String []getParameterValues(String name)
  {
    return _request.getParameterValues(name);
  }

  /**
   * Returns an enumeration of all form parameter names.
   *
   * <code><pre>
   * Enumeration e = request.getParameterNames();
   * while (e.hasMoreElements()) {
   *   String name = (String) e.nextElement();
   *   out.println(name + ": " + request.getParameter(name));
   * }
   * </pre></code>
   */
  public Enumeration getParameterNames()
  {
    return _request.getParameterNames();
  }

  /**
   * Returns a Map of the form parameters.  The map is immutable.
   * The keys are the form keys as returned by <code>getParameterNames</code>
   * and the values are String arrays as returned by
   * <code>getParameterValues</code>.
   */
  public Map getParameterMap()
  {
    return _request.getParameterMap();
  }

  /**
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  public ServletInputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }

  /**
   * Returns a reader to read POSTed data.  Character encoding is
   * based on the request data and is the same as
   * <code>getCharacterEncoding()</code>
   */
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }

  /**
   * Returns the character encoding of the POSTed data.
   */
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
  public int getContentLength()
  {
    return _request.getContentLength();
  }

  /**
   * Returns the request's mime-type.
   */
  public String getContentType()
  {
    return _request.getContentType();
  }

  /**
   * Returns the request's preferred locale, based on the Accept-Language
   * header.  If unspecified, returns the server's default locale.
   */
  public Locale getLocale()
  {
    return _request.getLocale();
  }

  /**
   * Returns an enumeration of all locales acceptable by the client.
   */
  public Enumeration getLocales()
  {
    return _request.getLocales();
  }

  /**
   * Returns true if the connection is secure, e.g. it uses SSL.
   */
  public boolean isSecure()
  {
    return _request.isSecure();
  }

  /**
   * Returns an attribute value.
   *
   * @param name the attribute name
   * @return the attribute value
   */
  public Object getAttribute(String name)
  {
    return _request.getAttribute(name);
  }

  /**
   * Sets an attribute value.
   *
   * @param name the attribute name
   * @param o the attribute value
   */
  public void setAttribute(String name, Object o)
  {
    _request.setAttribute(this, name, o);
  }

  /**
   * Enumerates all attribute names in the request.
   */
  public Enumeration getAttributeNames()
  {
    return _request.getAttributeNames();
  }

  /**
   * Removes the given attribute.
   *
   * @param name the attribute name
   */
  public void removeAttribute(String name)
  {
    _request.removeAttribute(this, name);
  }

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
   * @param uri path relative to <code>getRequestURI()</code>
   * (including query string) for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
  public RequestDispatcher getRequestDispatcher(String uri)
  {
    return _request.getRequestDispatcher(uri);
  }

  /**
   * Returns the path of the URI.
   */
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }

  /**
   * Returns the servlet context for the request
   *
   * @since Servlet 3.0
   */
  public ServletContext getServletContext()
  {
    return _request.getServletContext();
  }

  /**
   * Returns the servlet response for the request
   *
   * @since Servlet 3.0
   */
  public ServletResponse getServletResponse()
  {
    return _response;
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend(long timeout)
  {
    suspend();

    // _comet.setTimeout(timeout);
  }

  /**
   * Suspend the request
   *
   * @since Servlet 3.0
   */
  public void suspend()
  {
    if (_comet == null)
      _comet = _request.getConnection().toComet(true, this, _response);

    _comet.suspend();
  }

  /**
   * Resume the request
   *
   * @since Servlet 3.0
   */
  public void resume()
  {
    if (_comet != null)
      _comet.wake();
  }

  /**
   * Complete the request
   *
   * @since Servlet 3.0
   */
  public void complete()
  {
    if (_comet != null) {
      _comet.complete();
    }
  }

  /**
   * Returns true if the servlet is suspended
   *
   * @since Servlet 3.0
   */
  public boolean isSuspended()
  {
    if (_comet != null)
      return _comet.isSuspended();
    else
      return false;
  }

  /**
   * Returns true if the servlet is resumed
   *
   * @since Servlet 3.0
   */
  public boolean isResumed()
  {
    return _comet != null && ! _comet.isInitial() && ! _comet.isComplete();
  }

  /**
   * Returns true if the servlet timed out
   *
   * @since Servlet 3.0
   */
  public boolean isTimeout()
  {
    return _comet != null && _comet.isTimeout();
  }

  /**
   * Returns true for the initial dispatch
   *
   * @since Servlet 3.0
   */
  public boolean isInitial()
  {
    if (_comet != null)
      return _comet.isInitial();
    else
      return true;
  }

  //
  // HttpServletRequest APIs
  //

  /**
   * Returns the HTTP method, e.g. "GET" or "POST"
   *
   * <p/>Equivalent to CGI's <code>REQUEST_METHOD</code>
   */
  public String getMethod()
  {
    return _request.getMethod();
  }
  
  /**
   * Returns the entire request URI
   */
  public String getRequestURI()
  {
    return _request.getRequestURI();
  }
  
  /**
   * Reconstructs the URL the client used for the request.
   *
   * @since Servlet 2.3
   */
  public StringBuffer getRequestURL()
  {
    return _request.getRequestURL();
  }
  
  /**
   * Returns the part of the URI corresponding to the application's
   * prefix.  The first part of the URI selects applications
   * (ServletContexts).
   *
   * <p><code>getContextPath()</code> is /myapp for the uri
   * /myapp/servlet/Hello,
   */
  public String getContextPath()
  {
    return _request.getContextPath();
  }
  
  /**
   * Returns the URI part corresponding to the selected servlet.
   * The URI is relative to the application.
   *
   * <p/>Corresponds to CGI's <code>SCRIPT_NAME</code>
   *
   * <code>getServletPath()</code> is /servlet/Hello for the uri
   * /myapp/servlet/Hello/foo.
   *
   * <code>getServletPath()</code> is /dir/hello.jsp
   * for the uri /myapp/dir/hello.jsp/foo,
   */
  public String getServletPath()
  {
    return _request.getServletPath();
  }
  
  /**
   * Returns the URI part after the selected servlet and null if there
   * is no suffix.
   *
   * <p/>Corresponds to CGI's <code>PATH_INFO</code>
   *
   * <p><code>getPathInfo()</code> is /foo for
   * the uri /myapp/servlet/Hello/foo.
   *
   * <code>getPathInfo()</code> is /hello.jsp for for the uri
   * /myapp/dir/hello.jsp/foo.
   */
  public String getPathInfo()
  {
    return _request.getPathInfo();
  }
  
  /**
   * Returns the physical path name for the path info.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   *
   * @return null if there is no path info.
   */
  public String getPathTranslated()
  {
    return _request.getPathTranslated();
  }
  
  /**
   * Returns the request's query string.  Form based servlets will use
   * <code>ServletRequest.getParameter()</code> to decode the form values.
   *
   * <p/>Corresponds to CGI's <code>PATH_TRANSLATED</code>
   */
  public String getQueryString()
  {
    return _request.getQueryString();
  }
  
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
  public Enumeration getHeaders(String name)
  {
    return _request.getHeaders(name);
  }
  
  /**
   * Returns an enumeration of all headers sent by the client.
   */
  public Enumeration getHeaderNames()
  {
    return _request.getHeaderNames();
  }
  
  /**
   * Converts a header value to an integer.
   *
   * @param name the header name
   * @return the header value converted to an integer
   */
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
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }
  
  /**
   * Returns an array of all cookies sent by the client.
   */
  public Cookie []getCookies()
  {
    return _request.getCookies();
  }
  
  /**
   * Returns a session.  If no session exists and create is true, then
   * create a new session, otherwise return null.
   *
   * @param create If true, then create a new session if none exists.
   */
  public HttpSession getSession(boolean create)
  {
    return _request.getSession(create);
  }
  
  /**
   * Returns the current session, creating one if necessary.
   * Sessions are a convenience for keeping user state
   * across requests.
   */
  public HttpSession getSession()
  {
    return _request.getSession();
  }
  
  /**
   * Returns the session id.  Sessions are a convenience for keeping
   * user state across requests.
   *
   * <p/>The session id is the value of the JSESSION cookie.
   */
  public String getRequestedSessionId()
  {
    return _request.getRequestedSessionId();
  }
  
  /**
   * Returns true if the session is valid.
   */
  public boolean isRequestedSessionIdValid()
  {
    return _request.isRequestedSessionIdValid();
  }
  
  /**
   * Returns true if the session came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return _request.isRequestedSessionIdFromCookie();
  }
  
  /**
   * Returns true if the session came URL-encoding.
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return _request.isRequestedSessionIdFromURL();
  }
  
  /**
   * Returns the auth type, i.e. BASIC, CLIENT-CERT, DIGEST, or FORM.
   */
  public String getAuthType()
  {
    return _request.getAuthType();
  }
  
  /**
   * Returns the remote user if authenticated.
   */
  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }
  
  /**
   * Returns true if the user is in the given role.
   */
  public boolean isUserInRole(String role)
  {
    return _request.isUserInRole(role);
  }
  
  /**
   * Returns the equivalent principal object for the authenticated user.
   */
  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }
  
  /**
   * @deprecated
   */
  public boolean isRequestedSessionIdFromUrl()
  {
    return _request.isRequestedSessionIdFromUrl();
  }

  //
  // CauchoRequest methods
  //

  public String getPageURI()
  {
    return _request.getPageURI();
  }
  
  public String getPageContextPath()
  {
    return _request.getPageContextPath();
  }
  
  public String getPageServletPath()
  {
    return _request.getPageServletPath();
  }
  
  public String getPagePathInfo()
  {
    return _request.getPagePathInfo();
  }
  
  public String getPageQueryString()
  {
    return _request.getPageQueryString();
  }

  public WebApp getWebApp()
  {
    return _request.getWebApp();
  }
  
  public ReadStream getStream()
    throws IOException
  {
    return _request.getStream();
  }
  
  public int getRequestDepth(int depth)
  {
    return _request.getRequestDepth(depth);
  }
  
  public void setHeader(String key, String value)
  {
    _request.setHeader(key, value);
  }
  
  public boolean getVaryCookies()
  {
    return _request.getVaryCookies();
  }
  
  public String getVaryCookie()
  {
    return _request.getVaryCookie();
  }
  
  public void setVaryCookie(String cookie)
  {
    _request.setVaryCookie(cookie);
  }
  
  public boolean getHasCookie()
  {
    return _request.getHasCookie();
  }

  public boolean isTop()
  {
    return _request.isTop();
  }

  public HttpSession getMemorySession()
  {
    return _request.getMemorySession();
  }
  
  public Cookie getCookie(String name)
  {
    return _request.getCookie(name);
  }
  
  public void setHasCookie()
  {
    _request.setHasCookie();
  }

  public boolean isComet()
  {
    return _request.isComet();
  }

  public ConnectionCometController toComet()
  {
    if (_comet == null)
      _comet = _request.getConnection().toComet(true, this, _response);
    
    return _comet;
  }

  public ConnectionCometController getCometController()
  {
    return _comet;
  }

  public boolean isDuplex()
  {
    return _request.isDuplex();
  }
  
  public void killKeepalive()
  {
    _request.killKeepalive();
  }
  
  public boolean allowKeepalive()
  {
    return _request.allowKeepalive();
  }

  public boolean isClientDisconnect()
  {
    return _request.isClientDisconnect();
  }

  public void clientDisconnect()
  {
    _request.clientDisconnect();
  }

  public boolean isLoginRequested()
  {
    return _request.isLoginRequested();
  }
  
  public boolean login(boolean isFail)
  {
    return _request.login(isFail);
  }

  public Connection getConnection()
  {
    return _request.getConnection();
  }

  //
  // HttpServletRequestImpl methods
  //

  public AbstractHttpRequest getAbstractHttpRequest()
  {
    return _request;
  }

  public boolean isSuspend()
  {
    return _request.isSuspend();
  }

  public boolean hasRequest()
  {
    return _request.hasRequest();
  }

  public void finishInvocation()
  {
    _request.finishInvocation();
  }

  //
  // servlet 3.0
  //

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Adds an async listener for this request
   *
   * @since Servlet 3.0
   */
  public void addAsyncListener(AsyncListener listener,
			       ServletRequest request,
			       ServletResponse response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  public AsyncContext getAsyncContext()
  {
    return (AsyncContext) _comet;
  }

  /**
   * Returns true if the request is in async.
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncStarted()
  {
    return _comet != null;
  }

  /**
   * Returns true if the request supports async
   *
   * @since Servlet 3.0
   */
  public boolean isAsyncSupported()
  {
    return true;
  }

  /**
   * Sets the async timeout
   *
   * @since Servlet 3.0
   */
  public void setAsyncTimeout(long timeout)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public long getAsyncTimeout()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync()
  {
    if (_comet == null)
      _comet = _request.getConnection().toComet(true, this, _response);

    _comet.suspend();

    return (AsyncContext) _comet;
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync(ServletRequest request,
				 ServletResponse response)
  {
    if (_comet == null)
      _comet = _request.getConnection().toComet(false, request, response);

    _comet.suspend();
    
    return (AsyncContext) _comet;
  }

  public DispatcherType getDispatcherType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }
}
