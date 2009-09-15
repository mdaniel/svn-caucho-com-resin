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

import com.caucho.config.scope.ScopeRemoveListener;
import com.caucho.i18n.CharacterEncoding;
import com.caucho.security.AbstractLogin;
import com.caucho.security.RoleMapManager;
import com.caucho.security.Login;
import com.caucho.server.cluster.Server;
import com.caucho.server.connection.ConnectionCometController;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.session.SessionImpl;
import com.caucho.server.session.SessionManager;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.*;
import com.caucho.vfs.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import javax.servlet.*;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * User facade for http requests.
 */
public class HttpServletRequestImpl implements CauchoRequest
{
  private static final Logger log
    = Logger.getLogger(HttpServletRequestImpl.class.getName());

  private static final L10N L = new L10N(HttpServletRequestImpl.class);

  public static final String MULTIPARTCONFIG = "com.caucho.multipart-config";

  private static final String CHAR_ENCODING = "resin.form.character.encoding";
  private static final String FORM_LOCALE = "resin.form.local";
  private static final String CAUCHO_CHAR_ENCODING = "caucho.form.character.encoding";

  private AbstractHttpRequest _request;

  private final HttpServletResponseImpl _response;

  private boolean _isSecure;

  private Invocation _invocation;

  // form
  private HashMapImpl<String,String[]> _filledForm;
  private List<Part> _parts;

  // session/cookies
  private Cookie []_cookiesIn;

  private boolean _varyCookies;   // True if the page depends on cookies
  private boolean _hasCookie;
  private boolean _isSessionIdFromCookie;

  protected int _sessionGroup = -1;

  private boolean _sessionIsLoaded;
  private SessionImpl _session;

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
  private boolean _isAsyncSupported = true;
  private AsyncListenerNode _asyncListenerNode;
  private long _asyncTimeout = 10000;
  private ConnectionCometController _comet;

  private ArrayList<Path> _closeOnExit;

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

    _response = new HttpServletResponseImpl(this,
                                            request.getAbstractHttpResponse());

    _isSecure = request.isSecure();
  }

  public HttpServletResponseImpl getResponse()
  {
    return _response;
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
   * Returns an InputStream to retrieve POST data from the request.
   * The stream will automatically end when the end of the POST data
   * is complete.
   */
  public ServletInputStream getInputStream()
    throws IOException
  {
    if (_hasReader)
      throw new IllegalStateException(L.l("getInputStream() can't be called after getReader()"));

    _hasInputStream = true;

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
    if (_hasInputStream)
      throw new IllegalStateException(L.l("getReader() can't be called after getInputStream()"));

    _hasReader = true;

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
    return _isSecure;
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

  /**
   * Returns an enumeration of the request attribute names.
   */
  public Enumeration<String> getAttributeNames()
  {
    HashMapImpl<String,Object> attributes = _attributes;
    
    if (attributes != null) {
      return Collections.enumeration(attributes.keySet());
    }
    else if (isSecure()) {
      _attributes = new HashMapImpl<String,Object>();
      _attributes = attributes;
      _request.initAttributes(this);
      
      return Collections.enumeration(attributes.keySet());
    }
    else
      return (Enumeration<String>) NullEnumeration.create();
  }

  /**
   * Sets the value of the named request attribute.
   *
   * @param name the attribute name.
   * @param value the new attribute value.
   */
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
    else
      removeAttribute(name);
  }

  /**
   * Removes the value of the named request attribute.
   *
   * @param name the attribute name.
   */
  public void removeAttribute(String name)
  {
    HashMapImpl<String,Object> attributes = _attributes;

    if (attributes == null)
      return;
    
    Object oldValue = attributes.remove(name);
    
    WebApp webApp = getWebApp();

    for (ServletRequestAttributeListener listener
           : webApp.getRequestAttributeListeners()) {
      ServletRequestAttributeEvent event;

      event = new ServletRequestAttributeEvent(webApp, this,
                                               name, oldValue);

      listener.attributeRemoved(event);
    }

    if (oldValue instanceof ScopeRemoveListener) {
      ((ScopeRemoveListener) oldValue).removeEvent(this, name);
    }
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
   * @param uri path relative to <code>getRequestURI()</code>
   * (including query string) for the included file.
   * @return RequestDispatcher for later inclusion or forwarding.
   */
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
  public ServletContext getServletContext()
  {
    Invocation invocation = _invocation;

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
    if (_comet == null) {
      _comet = _request.getConnection().toComet(true, this, _response);
      _comet.setAsyncListenerNode(_asyncListenerNode);
    }

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
   * Returns the URI for the request
   */
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

  /*
  public abstract byte []getUriBuffer();

  public abstract int getUriLength();
  */

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
   * @deprecated As of JSDK 2.1
   */
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

  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  public Map<String,String[]> getParameterMap()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return (String []) _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  public String getParameter(String name)
  {
    String []values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  /**
   * @since Servlet 3.0
   */
  public Iterable<Part> getParts()
    throws IOException, ServletException
  {
    if (! getContentType().startsWith("multipart/form-data"))
      throw new ServletException("Content-Type must be of 'multipart/form-data'.");

    if (_filledForm == null)
      _filledForm = parseQuery();

    return _parts;
  }

  public Part createPart(String name, Map<String, List<String>> headers)
  {
    return new PartImpl(name, headers);
  }

  /**
   * @since Servlet 3.0
   */
  public Part getPart(String name)
    throws IOException, ServletException
  {
    if (! getContentType().startsWith("multipart/form-data"))
      throw new ServletException("Content-Type must be of 'multipart/form-data'.");

    if (_filledForm == null)
      _filledForm = parseQuery();

    for (Part part : _parts) {
      if (name.equals(part.getName()))
        return part;
    }

    return null;
  }

  /**
   * Parses the query, either from the GET or the post.
   *
   * <p/>The character encoding is somewhat tricky.  If it's a post, then
   * assume the encoded form uses the same encoding as
   * getCharacterEncoding().
   *
   * <p/>If the request doesn't provide the encoding, use the
   * character-encoding parameter from the webApp.
   *
   * <p/>Otherwise use the default system encoding.
   */
  private HashMapImpl<String,String[]> parseQuery()
  {
    HashMapImpl<String,String[]> form = _request.getForm();

    try {
      String query = getQueryString();
      CharSegment contentType = _request.getContentTypeBuffer();

      if (query == null && contentType == null)
        return form;

      Form formParser = _request.getFormParser();
      long contentLength = _request.getLongContentLength();

      String charEncoding = getCharacterEncoding();
      if (charEncoding == null)
        charEncoding = (String) getAttribute(CAUCHO_CHAR_ENCODING);
      if (charEncoding == null)
        charEncoding = (String) getAttribute(CHAR_ENCODING);
      if (charEncoding == null) {
        Locale locale = (Locale) getAttribute(FORM_LOCALE);
        if (locale != null)
          charEncoding = Encoding.getMimeName(locale);
      }

      if (query != null) {
        String queryEncoding = charEncoding;

        if (queryEncoding == null && getServer() != null)
          queryEncoding = getServer().getURLCharacterEncoding();

        if (queryEncoding == null)
          queryEncoding = CharacterEncoding.getLocalEncoding();

        String javaEncoding = Encoding.getJavaName(queryEncoding);

        formParser.parseQueryString(form, query, javaEncoding, true);
      }

      if (charEncoding == null)
        charEncoding = CharacterEncoding.getLocalEncoding();

      String javaEncoding = Encoding.getJavaName(charEncoding);

      if (contentType == null || ! "POST".equalsIgnoreCase(getMethod())) {
      }

      else if (contentType.startsWith("application/x-www-form-urlencoded")) {
        formParser.parsePostData(form, getInputStream(), javaEncoding);
      }

      else if (getWebApp().doMultipartForm()
               && contentType.startsWith("multipart/form-data")) {
        int length = contentType.length();
        int i = contentType.indexOf("boundary=");

        if (i < 0)
          return form;

        long formUploadMax = getWebApp().getFormUploadMax();

        Object uploadMax = getAttribute("caucho.multipart.form.upload-max");
        if (uploadMax instanceof Number)
          formUploadMax = ((Number) uploadMax).longValue();

        // XXX: should this be an error?
        if (formUploadMax >= 0 && formUploadMax < contentLength) {
          setAttribute("caucho.multipart.form.error",
                       L.l("Multipart form upload of '{0}' bytes was too large.",
                           String.valueOf(contentLength)));
          setAttribute("caucho.multipart.form.error.size",
                       new Long(contentLength));

          return form;
        }

        MultipartConfig multipartConfig
          = (MultipartConfig) getAttribute(MULTIPARTCONFIG);

        long fileUploadMax = -1;

        if (multipartConfig != null) {
          formUploadMax = multipartConfig.maxRequestSize();
          fileUploadMax = multipartConfig.maxFileSize();
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
          MultipartForm.parsePostData(form,
                                      _parts,
                                      getStream(false), boundary.toString(),
                                      this,
                                      javaEncoding,
                                      formUploadMax,
                                      fileUploadMax);
        } catch (IOException e) {
          log.log(Level.FINE, e.toString(), e);
          setAttribute("caucho.multipart.form.error", e.getMessage());
        }
      }
    } catch (IOException e) {
      log.log(Level.FINE, e.toString(), e);
    }

    return form;
  }

  //
  // session/cookie management
  //

  /**
   * Returns an array of all cookies sent by the client.
   */
  public Cookie []getCookies()
  {
    if (_cookiesIn == null) {
      _cookiesIn = _request.getCookies();

      SessionManager sessionManager = getSessionManager();
      String sessionCookieName = getSessionCookie(sessionManager);

      for (int i = 0; i < _cookiesIn.length; i++) {
        Cookie cookie = _cookiesIn[i];

        if (cookie.getName().equals(sessionCookieName)
          && sessionManager.isSecure()) {
          cookie.setSecure(true);
          break;
        }
      }

      /*
      // The page varies depending on the presense of any cookies
      setVaryCookie(null);

      // If any cookies actually exist, the page is not anonymous
      if (_cookiesIn != null && _cookiesIn.length > 0)
        setHasCookie();
      */
    }
    
    if (_cookiesIn == null || _cookiesIn.length == 0)
      return null;
    else
      return _cookiesIn;
  }

  /**
   * Returns the named cookie from the browser
   */
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
   * Returns the memory session.
   */
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
  public HttpSession getSession(boolean create)
  {
    if (_session != null) {
      if (_session.isValid())
        return _session;
    }
    else if (! create && _sessionIsLoaded)
      return null;

    _sessionIsLoaded = true;

    _session = createSession(create);

    return _session;
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

  /**
   * Returns the current session.
   *
   * XXX: duplicated in RequestAdapter
   *
   * @param create true if a new session should be created
   *
   * @return the current session
   */
  private SessionImpl createSession(boolean create)
  {
    SessionManager manager = getSessionManager();

    if (manager == null)
      return null;

    String id = getRequestedSessionId();

    long now = Alarm.getCurrentTime();

    SessionImpl session
      = manager.createSession(create, this, id, now, _isSessionIdFromCookie);

    if (session != null
        && (id == null || ! session.getId().equals(id))
        && manager.enableSessionCookies()) {
      getResponse().setSessionId(session.getId());
    }

    return session;
  }

  /**
   * Returns the session id in the HTTP request cookies.
   * Because the webApp might use the cookie to change
   * the page contents, the caching sets vary: JSESSIONID.
   */
  private String findSessionIdFromCookie()
  {
    SessionManager manager = getSessionManager();

    if (manager == null || ! manager.enableSessionCookies())
      return null;

    Cookie cookie = getCookie(getSessionCookie(manager));

    if (cookie != null) {
      _isSessionIdFromCookie = true;
      return cookie.getValue();
    }
    else
      return null;
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
   * Returns true if the HTTP request's session id refers to a valid
   * session.
   */
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
   * Returns true if the current sessionId came from a cookie.
   */
  public boolean isRequestedSessionIdFromCookie()
  {
    return findSessionIdFromCookie() != null;
  }

  /**
   * Returns true if the current sessionId came from the url.
   */
  public boolean isRequestedSessionIdFromURL()
  {
    return findSessionIdFromUrl() != null;
  }

  /**
   * @deprecated
   */
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

  /**
   * Returns the session id in the HTTP request.  The cookie has
   * priority over the URL.  Because the webApp might be using
   * the cookie to change the page contents, the caching sets
   * vary: JSESSIONID.
   */
  public String getRequestedSessionId()
  {
    SessionManager manager = getSessionManager();

    String cookieName = null;

    if (manager != null && manager.enableSessionCookies()) {
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

    if (manager != null && manager.enableSessionCookies())
      return null;
    else
      return _request.findSessionIdFromConnection();
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

  /**
   * Returns true if the user represented by the current request
   * plays the named role.
   *
   * @param role the named role to test.
   * @return true if the user plays the role.
   */
  public boolean isUserInRole(String role)
  {
    HashMap<String,String> roleMap = _invocation.getSecurityRoleMap();

    if (roleMap != null) {
      String linkRole = roleMap.get(role);

      if (linkRole != null)
        role = linkRole;
    }

    if (_runAs != null)
      return _runAs.equals(role);

    WebApp app = getWebApp();

    Principal user = getUserPrincipal();

    if (user == null) {
      if (log.isLoggable(Level.FINE))
        log.fine(this + " no user for isUserInRole");

      return false;
    }

    RoleMapManager roleManager = app != null ? app.getRoleMapManager() : null;

    if (roleManager != null) {
      Boolean result = roleManager.isUserInRole(role, user);

      if (result != null) {
        if (log.isLoggable(Level.FINE))
          log.fine(this + " userInRole(" + role + ")->" + result);

        return result;
      }
    }

    Login login = app == null ? null : app.getLogin();

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

  /**
   * Gets the authorization type
   */
  public String getAuthType()
  {
    Object login = getAttribute(AbstractLogin.LOGIN_NAME);

    if (login instanceof X509Certificate)
      return HttpServletRequest.CLIENT_CERT_AUTH;

    WebApp app = getWebApp();

    if (app != null && app.getLogin() != null && getUserPrincipal() != null)
      return app.getLogin().getAuthType();
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
  public boolean isLoginRequested()
  {
    return _isLoginRequested;
  }

  /**
   * @since Servlet 3.0
   */
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    Login login = getLogin();

    if (login == null)
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));

    Principal principal = login.login(this, response, true);

    if (principal != null)
      return true;

    return false;
  }

  /**
   * @since Servlet 3.0
   */
  public void login(String username, String password)
    throws ServletException
  {
    Login login = getLogin();

    if (login == null)
      throw new ServletException(L.l("No authentication mechanism is configured for '{0}'", getWebApp()));

    if (! login.isPasswordBased())
      throw new ServletException(L.l("Authentication mechanism '{0}' does not support password authentication", login));

    removeAttribute(Login.LOGIN_USER);
    removeAttribute(Login.LOGIN_PASSWORD);

    Principal principal = login.getUserPrincipal(this);

    if (principal != null)
      throw new ServletException(L.l("UserPrincipal object has already been established"));

    setAttribute(Login.LOGIN_USER, username);
    setAttribute(Login.LOGIN_PASSWORD, password);

    try {
      login.login(this, getResponse(), true);
    }
    finally {
      removeAttribute(Login.LOGIN_USER);
      removeAttribute(Login.LOGIN_PASSWORD);
    }
  }

  /**
   * Authenticate the user.
   */
  public boolean login(boolean isFail)
  {
    try {
      /*
      Principal user = null;
      user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

      if (user != null)
        return true;
      */

      WebApp app = getWebApp();
      if (app == null) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no web-app found");

        _response.sendError(HttpServletResponse.SC_FORBIDDEN);

        return false;
      }

      // If the authenticator can find the user, return it.
      Login login = app.getLogin();

      if (login != null) {
        Principal user = login.login(this, getResponse(), isFail);

        return user != null;
        /*
        if (user == null)
          return false;

        setAttribute(AbstractLogin.LOGIN_NAME, user);

        return true;
        */
      }
      else if (isFail) {
        if (log.isLoggable(Level.FINE))
          log.finer("authentication failed, no login module found for "
                    + app);

        _response.sendError(HttpServletResponse.SC_FORBIDDEN);

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
   * Gets the remote user from the authorization type
   */
  public String getRemoteUser()
  {
    Principal principal = getUserPrincipal();

    if (principal != null)
      return principal.getName();
    else
      return null;
  }

  /**
   * Returns the Principal representing the logged in user.
   */
  public Principal getUserPrincipal()
  {
    _isLoginRequested = true;

    Principal user;
    user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

    if (user != null)
      return user;

    WebApp app = getWebApp();
    if (app == null)
      return null;

    // If the authenticator can find the user, return it.
    Login login = app.getLogin();

    if (login != null) {
      user = login.getUserPrincipal(this);

      if (user != null) {
        _response.setPrivateCache(true);
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
   * Logs out the principal.
   */
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

  /**
   * Internal logging return to get the remote user.  If the request already
   * knows the user, get it, otherwise just return null.
   */
  public String getRemoteUser(boolean create)
  {
    if (_session == null)
      return null;

    Principal user = (Principal) getAttribute(AbstractLogin.LOGIN_NAME);

    if (user == null && create)
      user = getUserPrincipal();

    if (user != null)
      return user.getName();
    else
      return null;
  }

  //
  // deprecated
  //

  public ReadStream getStream()
    throws IOException
  {
    return _request.getStream();
  }

  public ReadStream getStream(boolean isFlush)
    throws IOException
  {
    return _request.getStream(isFlush);
  }

  public int getRequestDepth(int depth)
  {
    return 0;
  }

  public void setHeader(String key, String value)
  {
    _request.setHeader(key, value);
  }

  public void setSyntheticCacheHeader(boolean isTop)
  {
    _isSyntheticCacheHeader = isTop;
  }

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
    return _request.isComet();
  }

  public ConnectionCometController toComet()
  {
    if (_comet == null) {
      _comet = _request.getConnection().toComet(true, this, _response);
      _comet.setAsyncListenerNode(_asyncListenerNode);
    }

    return _comet;
  }

  public ConnectionCometController getCometController()
  {
    return _comet;
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

  public void setInvocation(Invocation invocation)
  {
    _invocation = invocation;
  }

  public Invocation getInvocation()
  {
    return _invocation;
  }

  public long getStartTime()
  {
    return _request.getStartTime();
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
    if (_comet == null) {
      _asyncListenerNode
        = new AsyncListenerNode(listener, this, _response, _asyncListenerNode);
    }
    else
      _comet.addAsyncListener(listener, this, _response);
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
    if (_comet == null) {
      _asyncListenerNode
        = new AsyncListenerNode(listener, request, response,
                                _asyncListenerNode);
    }
    else
      _comet.addAsyncListener(listener, request, response);
  }

  /**
   * Returns the async context for the request
   *
   * @since Servlet 3.0
   */
  public AsyncContext getAsyncContext()
  {
    if (_comet != null)
      return (AsyncContext) _comet;
    else
      throw new IllegalStateException(L.l("getAsyncContext() must be called after asyncStarted() has started a new AsyncContext."));
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
    Invocation invocation = _invocation;

    if (invocation != null)
      return invocation.isAsyncSupported();
    else
      return false;
  }

  /**
   * Sets the async timeout
   *
   * @since Servlet 3.0
   */
  public void setAsyncTimeout(long timeout)
  {
    _asyncTimeout = timeout;
  }

  public long getAsyncTimeout()
  {
    return _asyncTimeout;
  }

  /**
   * Starts an async mode
   *
   * @since Servlet 3.0
   */
  public AsyncContext startAsync()
  {
    if (! _isAsyncSupported)
      throw new IllegalStateException(L.l("The servlet '{0}' at '{1}' does not support async because the servlet or one of the filters does not have an @AsyncSupported annotation.",
                                          getServletName(), getServletPath()));

    if (_comet == null) {
      _comet = _request.getConnection().toComet(true, this, _response);
      if (_asyncTimeout > 0)
        _comet.setMaxIdleTime(_asyncTimeout);
      _comet.setAsyncListenerNode(_asyncListenerNode);
    }

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
    if (_comet == null) {
      _comet = _request.getConnection().toComet(false, request, response);
      _comet.setAsyncListenerNode(_asyncListenerNode);
    }

    _comet.suspend();

    return (AsyncContext) _comet;
  }

  public DispatcherType getDispatcherType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  void finishRequest()
  {
    SessionImpl session = _session;

    if (session != null)
      session.finishRequest();

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
  }

  public void cleanup()
  {
    HashMapImpl<String,Object> attributes = _attributes;
    
    if (attributes != null) {
      for (Map.Entry<String,Object> entry : attributes.entrySet()) {
        Object value = entry.getValue();

        if (value instanceof ScopeRemoveListener) {
          ((ScopeRemoveListener) value).removeEvent(this, entry.getKey());
        }
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

  public final Server getServer()
  {
    return _request.getServer();
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

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }

  private class PartImpl implements Part {
    private String _name;
    private Map<String, List<String>> _headers;
    private Object _value;
    private Path _newPath;

    private PartImpl(String name, Map<String, List<String>> headers)
    {
      _name = name;
      _headers = headers;
    }

    public void delete()
      throws IOException
    {
      if (_newPath != null)
        _newPath.remove();

      Object value = getValue();

      if (! (value instanceof FilePath))
        throw new IOException(L.l("Part.delete() is not applicable to part '{0}':'{1}'", _name, value));

      ((FilePath)value).remove();
    }

    public String getContentType()
    {
      String[] value = _filledForm.get(_name + ".content-type");

      if (value != null && value.length > 0)
        return value[0];

      return null;
    }

    public String getHeader(String name)
    {
      List<String> values = _headers.get(name);

      if (values != null && values.size() > 0)
        return values.get(0);

      return null;
    }

    public Iterable<String> getHeaderNames()
    {
      return _headers.keySet();
    }

    public Iterable<String> getHeaders(String name)
    {
      return _headers.get(name);
    }

    public InputStream getInputStream()
      throws IOException
    {
      Object value = getValue();

      if (value instanceof FilePath)
        return ((FilePath)value).openRead();

      throw new IOException(L.l("Part.getInputStream() is not applicable to part '{0}':'{1}'", _name, value));
    }

    public String getName()
    {
      return _name;
    }

    public long getSize()
    {
      Object value = getValue();

      if (value instanceof FilePath) {
        return ((Path) value).getLength();
      }
      else if (value instanceof String) {
        return -1;
      }
      else if (value == null) {
        return -1;
      }
      else {
        log.finest(L.l("Part.getSize() is not applicable to part'{0}':'{1}'",
                       _name, value));

        return -1;
      }
    }

    public void write(String fileName)
      throws IOException
    {
      if (_newPath != null)
        throw new IOException(L.l(
          "Contents of part '{0}' has already been written to '{1}'",
          _name,
          _newPath));

      Path path;

      Object value = getValue();

      if (!(value instanceof FilePath))
        throw new IOException(L.l(
          "Part.write() is not applicable to part '{0}':'{1}'",
          _name,
          value));
      else
        path = (Path) value;

      MultipartConfig mc = (MultipartConfig) getAttribute(MULTIPARTCONFIG);
      String location = mc.location().replace('\\', '/');
      fileName = fileName.replace('\\', '/');

      String file;

      if (location.charAt(location.length() -1) != '/' && fileName.charAt(fileName.length() -1) != '/')
        file = location + '/' + fileName;
      else
        file = location + fileName;

      _newPath = Vfs.lookup(file);

      if (_newPath.exists())
        throw new IOException(L.l("File '{0}' already exists.", _newPath));

      Path parent = _newPath.getParent();

      if (! parent.exists())
        if (! parent.mkdirs())
          throw new IOException(L.l("Unable to create path '{0}'. Check permissions.", parent));

      if (! path.renameTo(_newPath)) {
        WriteStream out = null;

        try {
          out = _newPath.openWrite();

          path.writeToStream(out);

          out.flush();

          out.close();
        } catch (IOException e) {
          log.log(Level.SEVERE, L.l("Cannot write contents of '{0}' to '{1}'", path, _newPath), e);

          throw e;
        } finally {
          if (out != null)
            out.close();
        }
      }
    }

    private Object getValue()
    {
      if (_value != null)
        return _value;

      String []values = _filledForm.get(_name + ".file");

      if (values != null && values.length > 0) {
        _value = Vfs.lookup(values[0]);
      } else {
        values = _filledForm.get(_name);

        if (values != null && values.length > 0)
          _value = values[0];
      }

      return _value;
    }
  }
}
