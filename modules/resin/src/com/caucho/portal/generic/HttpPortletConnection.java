/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic;

import java.io.*;

import java.security.Principal;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.portlet.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;


/**
 * A connection to an HttpServletRequest and HttpServletResponse.
 */
public class HttpPortletConnection
  extends PortletConnection
{
  /** 
   * request attribute for the HttpServletRequest
   */
  final static public String HTTP_SERVLET_REQUEST
    = HttpPortletRequestDispatcher.HTTP_SERVLET_REQUEST;

  /** 
   * request attribute for the HttpServletResponse
   */
  final static public String HTTP_SERVLET_RESPONSE
    = HttpPortletRequestDispatcher.HTTP_SERVLET_RESPONSE;

  public static HttpServletRequest getHttpRequest(PortletRequest request)
  {
    return (HttpServletRequest) request.getAttribute(HTTP_SERVLET_REQUEST);
  }

  public static HttpServletResponse getHttpResponse(PortletRequest request)
  {
    return (HttpServletResponse) request.getAttribute(HTTP_SERVLET_RESPONSE);
  }

  private PortletContext _portletContext;
  private HttpServletRequest _httpRequest;
  private HttpServletResponse _httpResponse;

  private Object _oldHttpRequest;
  private Object _oldHttpResponse;

  private String _servletUrl;

  private MapBasedInvocationFactory _createdInvocationFactory;

  private HttpPortletSession _portletSession;

  private Set<Locale> _clientLocales;
  private Set<String> _clientCharacterEncodings;
  private Set<String> _clientContentTypes;

  private boolean _isLocaleEstablished;
  private boolean _isContentTypeEstablished;

  public HttpPortletConnection()
  {
  }

  public void start(Portal portal,
                    PortletContext portletContext,
                    HttpServletRequest httpRequest, 
                    HttpServletResponse httpResponse,
                    boolean useParameters)
  {
    if (_createdInvocationFactory != null)
      throw new IllegalStateException("missing finish?");

    _createdInvocationFactory = new MapBasedInvocationFactory();

    if (useParameters)
      _createdInvocationFactory.start(httpRequest.getParameterMap());
    else
      _createdInvocationFactory.start(null);

    start(portal, 
          portletContext, 
          httpRequest, 
          httpResponse, 
          _createdInvocationFactory);
  }

  public void start(Portal portal,
                    PortletContext portletContext,
                    HttpServletRequest httpRequest, 
                    HttpServletResponse httpResponse,
                    InvocationFactory invocationFactory)
  {
    super.start(portal, invocationFactory);

    _portletContext = portletContext;
    _httpRequest = httpRequest;
    _httpResponse = httpResponse;

    _oldHttpRequest = _httpRequest.getAttribute(HTTP_SERVLET_REQUEST);

    if (_oldHttpRequest != null)
      _oldHttpResponse=_httpRequest.getAttribute(HTTP_SERVLET_RESPONSE);

    _httpRequest.setAttribute(HTTP_SERVLET_REQUEST, _httpRequest);
    _httpRequest.setAttribute(HTTP_SERVLET_RESPONSE, _httpResponse);

    _servletUrl = makeServletUrl(_httpRequest);
  }


  protected String makeServletUrl(HttpServletRequest request)
  {
    String scheme = request.getScheme();
    String serverName = request.getServerName();
    int port = request.getServerPort();

    if (port == 80 && scheme.equals("http"))
      port = -1;

    if (port == 443 && scheme.equals("https"))
      port = -1;

    String contextPath 
      = (String) request.getAttribute("javax.servlet.include.context_path");

    String servletPath;

    if (contextPath == null) {
      contextPath = request.getContextPath();
      servletPath = request.getServletPath();
    }
    else {
      servletPath = (String) request.getAttribute("javax.servlet.include.servlet_path");
    }

    StringBuffer buf = new StringBuffer(256);

    buf.append(scheme);
    buf.append("://");
    buf.append(serverName);
    if (port > 0) {
      buf.append(':');
      buf.append(port);
    }
    buf.append(contextPath);
    buf.append(servletPath);

    return buf.toString();
  }

  public void finish()
  {
    int expirationCache = getExpirationCache();

    if (expirationCache == 0) {
      _httpResponse.setHeader( "Cache-Control", 
                               "no-cache,post-check=0,pre-check=0" );

      _httpResponse.setHeader("Pragma", "no-cache");
      _httpResponse.setHeader("Expires", "Thu,01Dec199416:00:00GMT");
    }
    else {
      if (isPrivate()) {
        _httpResponse.setHeader( "Cache-Control", 
                                 "private,max-age=" + expirationCache );
      }
      else {
        _httpResponse.setHeader( "Cache-Control", 
                                 "max-age=" + expirationCache );
      }
    }

    _isLocaleEstablished = false;
    _isContentTypeEstablished = false;

    _clientLocales = null;
    _clientCharacterEncodings = null;
    _clientContentTypes = null;

    PortletContext portletContext = _portletContext;
    MapBasedInvocationFactory createdInvocationFactory 
      = _createdInvocationFactory;
    HttpServletRequest httpRequest = _httpRequest;
    Object oldHttpRequest = _oldHttpRequest;
    HttpServletResponse httpResponse = _httpResponse;
    Object oldHttpResponse = _oldHttpResponse;
    HttpPortletSession portletSession = _portletSession;

    _servletUrl = null;

    _portletContext = null;
    _portletSession = null;
    _oldHttpRequest = null;
    _oldHttpResponse = null;
    _createdInvocationFactory = null;
    _httpRequest = null;
    _httpResponse = null;

    httpRequest.setAttribute(HTTP_SERVLET_RESPONSE, oldHttpResponse);
    httpRequest.setAttribute(HTTP_SERVLET_REQUEST, oldHttpRequest);

    super.finish();

    if (portletSession != null)
      portletSession.finish();

    if (createdInvocationFactory != null)
      createdInvocationFactory.finish();
  }

  public HttpServletRequest getHttpRequest()
  {
    return _httpRequest;
  }

  public HttpServletResponse getHttpResponse()
  {
    return _httpResponse;
  }


  /**
   * Get the content types acceptable to the client.  The returned Set 
   * is ordered, the most preferrable content types appear before the least
   * preferred.
   *
   * This implementation returns the content types that appear in the
   * String returned by getProperty("Accept") in the order
   * they appear in the string.
   *
   * @return the Set, null if client content types cannot be determined 
   */
  public Set<String> getClientContentTypes()
  {
    if (_clientContentTypes != null)
      return _clientContentTypes;

    _clientContentTypes = HttpUtil.getHeaderElements(getProperty("Accept"));

    return _clientContentTypes;
  }

  /**
   * Get the locales acceptable to the client.  The returned Set is ordered,
   * the most preferrable locale appears before the least preferred.  If the
   * client supports all locales, then a Locale("","","") will be present  in
   * the returned Set.
   *
   * This implementation returns the locales that appear in the String returned
   * by getProperty("Accept-Language") in the order they appear in the
   * string.  If the "*" element is present in the header, then a new
   * Locale("","","") is present in the set.
   *
   * @return the Set, null if client locales cannot be determined 
   */
  public Set<Locale> getClientLocales()
  {
    if (_clientLocales != null)
      return _clientLocales;

    _clientLocales = new LinkedHashSet<Locale>();
    _clientLocales.add(_httpRequest.getLocale());
    
    Enumeration en = _httpRequest.getLocales();

    while (en.hasMoreElements()) {
      _clientLocales.add( (Locale) en.nextElement() );
    }


    return _clientLocales;
  }

  /**
   * Get the character encodings acceptable to the client.  The returned Set is
   * order, the most preferrable character encoding appears before the least
   * preferred.
   *
   * This implementation returns the character encodings that appear in the
   * String returned by getProperty("Accept-Charset") in the order
   * they appear in the string.
   *
   * @return the Set, null if client character encodings cannot be determined 
   */
  public Set<String> getClientCharacterEncodings()
  {
    if (_clientCharacterEncodings != null)
      return _clientCharacterEncodings;

    _clientCharacterEncodings
      = HttpUtil.getHeaderElements(getProperty("Accept-Charset"));

    return _clientCharacterEncodings;
  }

  public String resolveURL(String url)
  {
    StringBuffer buf = new StringBuffer(256);
    appendUrlPrefix(_httpRequest, buf);
    buf.append(url);
    return buf.toString();
  }

  /**
   * Resolve the url with the given security level and encode it.
   *
   * This implementation calls resolveURL(String) if <i>secure</i>
   * is <code>false</code>.
   *
   * If <i>secure</i> is <code>true</code>, the prefix set with
   * setSecureUrlPrefix() is prepended.
   *
   * @throws PortletSecurityException if secure is true but the url
   * cannot be made secure because setSecureUrlPrefix() has not been called.
   */
  public String resolveURL(String url, boolean secure)
    throws PortletSecurityException
  {
    if (secure == false)
      return resolveURL(url);
    else {
      StringBuffer buf = new StringBuffer(256);

      appendSecureUrlPrefix(_httpRequest, buf);
      buf.append(url);
      return buf.toString();
    }
  }

  private String appendUrlPrefix(HttpServletRequest request, StringBuffer buf)
  {
    buf.append(_servletUrl);
    return buf.toString();
  }

  private String appendSecureUrlPrefix( HttpServletRequest request, 
                                        StringBuffer buf )
    throws PortletSecurityException
  {
    // XXX:

    if (request.isSecure())
      return appendUrlPrefix(request, buf);
    else
      throw new PortletSecurityException("cannot make url secure");
  }

  public boolean handleConstraintFailure( Constraint constraint, 
                                          int failureCode )
    throws IOException
  {
    if (failureCode == Constraint.SC_FORBIDDEN) {
      _httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
    else {
      _httpResponse.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
    }

    return true;
  }

  public boolean handleException(Exception exception)
  {
    return false;
  }
  
  /**
   * Return true if the connection can guarantee integrity 
   * (preventing data tampering in the communication process).
   */
  public boolean canGuaranteeIntegrity()
  {
    return isSecure();
  }

  /**
   * Return true if the connection can guarantee confidentiality (preventing
   * reading while in transit).
   */
  public boolean canGuaranteeConfidentiality()
  {
    return isSecure();
  }

  /**
   * Attributes for the connection are HttpServletRequest attributes.
   */
  public Object getAttribute(String name)
  {
    return _httpRequest.getAttribute(name);
  }

  /**
   * Attributes for the connection are HttpServletRequest attributes.
   */
  public void setAttribute(String name, Object o)
  {
    _httpRequest.setAttribute(name,o);
  }

  /**
   * Attributes for the connection are HttpServletRequest attributes.
   */
  public void removeAttribute(String name)
  {
    _httpRequest.removeAttribute(name);
  }

  /**
   * Attributes for the connection are HttpServletRequest attributes.
   */
  public Enumeration getAttributeNames()
  {
    return _httpRequest.getAttributeNames();
  }

  public PortletSession getPortletSession(boolean create)
  {
    if (_portletSession != null)
      return _portletSession;

    HttpSession httpSession = _httpRequest.getSession(create);

    if (httpSession != null) {
      // XXX: pool these
      _portletSession = new HttpPortletSession();
      _portletSession.start(_portletContext, httpSession);
    }

    return _portletSession;
  }

  public String getScheme()
  {
    return _httpRequest.getScheme();
  }

  public String getServerName()
  {
    return _httpRequest.getServerName();
  }

  public int getServerPort()
  {
    return _httpRequest.getServerPort();
  }

  public String getContextPath()
  {
    return _httpRequest.getContextPath();
  }

  public String getAuthType()
  {
    String authType = _httpRequest.getAuthType();
    
    if (authType == null)
      return null;
    else if (authType == HttpServletRequest.BASIC_AUTH)
      return PortletRequest.BASIC_AUTH;
    /** XXX: bug in caucho impl of jsdk
    else if (authType == HttpServletRequest.CLIENT_CERT_AUTH)
      return PortletRequest.CLIENT_CERT_AUTH;
      */
    else if (authType == HttpServletRequest.DIGEST_AUTH)
      return PortletRequest.DIGEST_AUTH;
    else if (authType == HttpServletRequest.FORM_AUTH)
      return PortletRequest.FORM_AUTH;
    else if (authType.equals(HttpServletRequest.BASIC_AUTH))
      return PortletRequest.BASIC_AUTH;
    else if (authType.equals("CLIENT_CERT")) // XXX: bug in caucho impl of jsdk
      return PortletRequest.CLIENT_CERT_AUTH;
    else if (authType.equals(HttpServletRequest.DIGEST_AUTH))
      return PortletRequest.DIGEST_AUTH;
    else if (authType.equals(HttpServletRequest.FORM_AUTH))
      return PortletRequest.FORM_AUTH;
    else
      return authType;
  }

  public boolean isSecure()
  {
    return _httpRequest.isSecure();
  }

  public String getRequestedSessionId()
  {
    return _httpRequest.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return _httpRequest.isRequestedSessionIdValid();
  }

  public String getRemoteUser()
  {
    return _httpRequest.getRemoteUser();
  }

  public Principal getUserPrincipal()
  {
    return _httpRequest.getUserPrincipal();
  }

  public boolean isUserInRole(String role)
  {
    return _httpRequest.isUserInRole(role);
  }

  public String getProperty(String propertyName)
  {
    return _httpRequest.getHeader(propertyName);
  }

  public Enumeration getProperties(String propertyName)
  {
    return _httpRequest.getHeaders(propertyName);
  }

  public Enumeration getPropertyNames()
  {
    return _httpRequest.getHeaderNames();
  }

  public String getSubmitContentType()
  {
    return _httpRequest.getContentType();
  }

  public int getSubmitContentLength()
  {
    return _httpRequest.getContentLength();
  }

  public InputStream getSubmitInputStream()
    throws IOException
  {
    return _httpRequest.getInputStream();
  }

  public void setSubmitCharacterEncoding(String enc)
    throws UnsupportedEncodingException, IllegalStateException
  {
    _httpRequest.setCharacterEncoding(enc);
  }

  public String getSubmitCharacterEncoding()
  {
    return _httpRequest.getCharacterEncoding();
  }

  public BufferedReader getSubmitReader()
    throws UnsupportedEncodingException, IOException
  {
    return _httpRequest.getReader();
  }

  /**
   * A path with a schem is encoded only.
   *
   * A relative location (does not start with slash) is resolved relative to
   * the servlet path and then encoded.  
   *
   * An absolute url (begins with slash) is resolved relative to 
   * the context path and then encoded.
   */
  public String encodeURL(String location)
  {
    int slash = location.indexOf('/');
    int colon = location.indexOf(':');

    if (colon == -1 || slash < colon ) {

      String scheme = _httpRequest.getScheme();
      String serverName = _httpRequest.getServerName();
      int port = _httpRequest.getServerPort();

      if (port == 80 && scheme.equals("http"))
        port = -1;

      if (port == 443 && scheme.equals("https"))
        port = -1;

      String contextPath = (String) _httpRequest.getAttribute("javax.servlet.include.context_path");
      String servletPath = null;

      if (contextPath == null) {
        contextPath = _httpRequest.getContextPath();
        servletPath = _httpRequest.getServletPath();
      }

      StringBuffer buf = new StringBuffer();

      buf.append(scheme);
      buf.append("://");
      buf.append(serverName);

      if (port > 0) {
        buf.append(':');
        buf.append(port);
      }

      buf.append(contextPath);

      if ( slash != 0 ) {

        if (servletPath == null)
          servletPath = (String) _httpRequest.getAttribute("javax.servlet.include.servlet_path");

        buf.append(servletPath);

        buf.append('/');

        buf.append(location);
      }
      else {
        buf.append(location);
      }

    
      location = buf.toString();
    }

    return _httpResponse.encodeURL(location);
  }

  public void sendRedirect(String location)
    throws IOException
  {
    String url = _httpResponse.encodeRedirectURL(location);

    _httpResponse.sendRedirect(url);
  }

  public void setProperty(String name, String value)
  {
    _httpResponse.setHeader(name, value);
  }

  public void addProperty(String name, String value)
  {
    _httpResponse.addHeader(name, value);
  }

  public void setContentType(String contentType)
  {
    _isContentTypeEstablished = true;
    _httpResponse.setContentType(contentType);
  }

  /** 
   * Return the content type established with setContentType(), or null if
   * setContentType() has not been called.
   */ 
  public String getContentType()
  {
    if (_isContentTypeEstablished)
      return _httpResponse.getContentType();
    else
      return null;
  }

  public void setLocale(Locale locale)
  {
    _isLocaleEstablished = true;
    _httpResponse.setLocale(locale);
  }

  /**
   * Return the Locale established with setLocale(), or null if setLocale()
   * has not been called.
   */
  public Locale getLocale()
  {
    if (_isLocaleEstablished)
      return _httpResponse.getLocale();
    else
      return null;
  }

  public void setBufferSize(int size)
  {
    _httpResponse.setBufferSize(size);
  }

  public int getBufferSize()
  {
    return _httpResponse.getBufferSize();
  }

  public void flushBuffer() 
    throws IOException
  {
    _httpResponse.flushBuffer();
  }

  public void resetBuffer()
  {
    _httpResponse.resetBuffer();
  }

  public void reset()
  {
    _httpResponse.reset();
  }

  public boolean isCommitted()
  {
    return _httpResponse.isCommitted();
  }

  public OutputStream getOutputStream() 
    throws IOException
  {
    return _httpResponse.getOutputStream();
  }

  public String getCharacterEncoding()
  {
    return _httpResponse.getCharacterEncoding();
  }

  public void setCharacterEncoding(String enc) 
    throws UnsupportedEncodingException
  {
    _httpResponse.setCharacterEncoding(enc);
  }

  public PrintWriter getWriter() 
    throws IOException
  {
    return _httpResponse.getWriter();
  }

}

