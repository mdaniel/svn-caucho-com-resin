/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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

package com.caucho.server.connection;

import java.util.Map;
import java.util.Enumeration;
import java.util.Locale;

import java.io.InputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.security.Principal;

import javax.servlet.ServletRequest;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Cookie;

/**
 * Wraps a servlet request in another request.  Filters may
 * use ServletRequestWrapper to modify the headers passed to the servlet.
 *
 * <p/>The default methods just call the wrapped request methods.
 *
 * @since servlet 2.3
 */
public class RequestWrapper implements ServletRequest {
  // the wrapped request
  protected HttpServletRequest _request;
  
  /**
   * Create a new ServletRequestWrapper wrapping the enclosed request.
   */
  public RequestWrapper()
  {
  }
  
  /**
   * Create a new ServletRequestWrapper wrapping the enclosed request.
   */
  public RequestWrapper(HttpServletRequest request)
  {
    _request = request;
  }
  
  /**
   * Sets the request object being wrapped.
   *
   * @exception IllegalArgumentException if the request is null
   */
  public void setRequest(HttpServletRequest request)
  {
    _request = request;
  }
  
  /**
   * Gets the request object being wrapped.
   *
   * @return the wrapped response
   */
  public HttpServletRequest getRequest()
  {
    return _request;
  }
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
   */
  public String getServerName()
  {
    return _request.getServerName();
  }
  /**
   * Returns the server port handling the request, e.g. 80.
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
   * Returns the remote port
   *
   * @since 2.4
   */
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }
  
  /**
   * Returns the IP address of the local host, i.e. the server.
   */
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }
  
  /**
   * Returns the local host name.
   */
  public String getLocalName()
  {
    return _request.getLocalName();
  }
  
  /**
   * Returns the local port
   */
  public int getLocalPort()
  {
    return _request.getLocalPort();
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
   * Returns the parameter map request parameters.  By default, returns
   * the underlying request's map.
   */
  public Map getParameterMap()
  {
    return _request.getParameterMap();
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
   * Enumeration e = _request.getParameterNames();
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
   * Sets the character encoding to be used for forms and getReader.
   */
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }
  /**
   * Returns the content length of the data.  This value may differ from
   * the actual length of the data.  For newer browsers, i.e.
   * those supporting HTTP/1.1, can support "chunked" encoding which does
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
   * Returns the request's preferred locale.
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
    _request.setAttribute(name, o);
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
    _request.removeAttribute(name);
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
   * Returns the real path.
   */
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }
  
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
   * long mod = _request.getDateHeader("If-Modified-Since");
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
    return getSession(true);
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
   * Returns the auth type, e.g. basic.
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

  /**
   * Clears the wrapper.
   */
  protected void free()
  {
    _request = null;
  }
}
