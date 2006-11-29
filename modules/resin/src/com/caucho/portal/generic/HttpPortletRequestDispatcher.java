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

import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

/**
 * An adapter to a {@link javax.servlet.RequestDispatcher},
 * based on a HttpServletRequest and HttpServletResponse stored
 * as request attributes
 */
public class HttpPortletRequestDispatcher 
  implements PortletRequestDispatcher 
{
  static protected final Logger log = 
    Logger.getLogger(HttpPortletRequestDispatcher.class.getName());

  /** 
   * request attribute for the HttpServletRequest
   */
  final static public String HTTP_SERVLET_REQUEST
    = "com.caucho.portal.generic.HttpServletRequest";

  /** 
   * request attribute for the HttpServletResponse
   */
  final static public String HTTP_SERVLET_RESPONSE
    = "com.caucho.portal.generic.HttpServletResponse";


  private RequestDispatcher _dispatcher;
  private Map<String, String[]> _parameterMap;

  private HttpPortletRequestWrapper _wrappedRequest
    = new HttpPortletRequestWrapper();

  private HttpPortletResponseWrapper _wrappedResponse
    = new HttpPortletResponseWrapper();

  public HttpPortletRequestDispatcher()
  {
  }

  /**
   * @return true if the servletContext has a dispatcher for the path
   */
  public boolean startWithPath(ServletContext servletContext, String path)
  {
    _dispatcher = servletContext.getRequestDispatcher(path);

    if (_dispatcher == null)
      return false;

    int paramIndex = path.indexOf('?');

    if (paramIndex != -1) {
      Map<String, String[]> parameterMap = new HashMap<String, String[]>();
      path = HttpUtil.extractParameters(parameterMap, path, paramIndex);
      _parameterMap = parameterMap;
    }


    return true;
  }

  /**
   * @return true if the servletContext has a dispatcher for the name
   */
  public boolean startWithName(ServletContext servletContext, String name)
  {
    _dispatcher = servletContext.getNamedDispatcher(name);

    return _dispatcher != null; 
  }

  public void finish()
  {
    _dispatcher = null;
    _parameterMap = null;
  }

  public void include(RenderRequest renderRequest, 
                      RenderResponse renderResponse)
    throws PortletException, IOException
  {
    HttpServletRequest httpRequest = 
      (HttpServletRequest) renderRequest.getAttribute(HTTP_SERVLET_REQUEST);

    if (httpRequest == null)
      throw new PortletException(
          "HttpServletRequest not found in request attribute " 
          + HTTP_SERVLET_REQUEST);

    HttpServletResponse httpResponse = 
      (HttpServletResponse) renderRequest.getAttribute(HTTP_SERVLET_RESPONSE);

    if (httpResponse == null)
      throw new PortletException(
          "HttpServletResponse not found in request attribute " 
          + HTTP_SERVLET_RESPONSE);

    _wrappedRequest.start(httpRequest, renderRequest, _parameterMap);
    _wrappedResponse.start(httpResponse, renderResponse);

    try {
      _dispatcher.include(_wrappedRequest, _wrappedResponse);
    } 
    catch (ServletException ex) {
      throw new PortletException(ex);
    }
    finally {
      _wrappedResponse.finish();
      _wrappedRequest.finish();
    }
  }

  /**
   * This can't extend HttpServletRequestWrapper because of some bizarre
   * behaviour mandated by the Servlet spec.
   */
  static protected class HttpPortletRequestWrapper 
      implements HttpServletRequest 
  {
    private RenderRequest _renderRequest;
    private HttpServletRequest _httpRequest;
    private Map<String, String[]> _parameterMap;

    private Map<String, String[]> _fullParameterMap;

    public HttpPortletRequestWrapper()
    {
    }

    public void start(HttpServletRequest httpRequest, 
                      RenderRequest renderRequest,
                      Map<String, String[]> parameterMap)

    {
      _httpRequest = httpRequest;
      _renderRequest = renderRequest;
      _parameterMap = parameterMap;
    }

    public void finish()
    {
      _renderRequest = null;
      _httpRequest = null;
      _parameterMap = null;
      _fullParameterMap = null;
    }

    public String getParameter(String name)
    {
      String[] values = getParameterValues(name);

      return values == null || values.length == 0 ? null : values[0];
    }

    public String []getParameterValues(String name)
    {
      if (_fullParameterMap != null)
        return _fullParameterMap.get(name);

      String[] values = null;

      if (_parameterMap != null)
        values = _parameterMap.get(name);

      if (values == null)
        values = _renderRequest.getParameterValues(name);
      
      return values;
    }

    public Map getParameterMap()
    {
      if (_fullParameterMap != null)
        return _fullParameterMap;

      if (_parameterMap == null) {
        _fullParameterMap = _renderRequest.getParameterMap();
      }
      else {
        _fullParameterMap = new HashMap<String, String[]>();
        _fullParameterMap.putAll(_renderRequest.getParameterMap());
        _fullParameterMap.putAll(_parameterMap);
      }

      return _fullParameterMap;
    }

    public Enumeration getParameterNames()
    {
      return Collections.enumeration(getParameterMap().keySet());
    }

    public String getScheme()
    {
      return _renderRequest.getScheme();
    }
    public String getServerName()
    {
      return _renderRequest.getServerName();
    }
    public int getServerPort()
    {
      return _renderRequest.getServerPort();
    }
    public Object getAttribute(String name)
    {
      return _renderRequest.getAttribute(name);
    }
    public void setAttribute(String name, Object o)
    {
      _renderRequest.setAttribute(name, o);
    }
    public Enumeration getAttributeNames()
    {
      return _renderRequest.getAttributeNames();
    }
    public void removeAttribute(String name)
    {
      _renderRequest.removeAttribute(name);
    }
    public Locale getLocale()
    {
      return _renderRequest.getLocale();
    }
    public Enumeration getLocales()
    {
      return _renderRequest.getLocales();
    }
    public boolean isSecure()
    {
      return _renderRequest.isSecure();
    }
    public String getAuthType()
    {
      return _renderRequest.getAuthType();
    }
    public String getRequestedSessionId()
    {
      return _renderRequest.getRequestedSessionId();
    }
    public boolean isRequestedSessionIdValid()
    {
      return _renderRequest.isRequestedSessionIdValid();
    }
    public String getRemoteUser()
    {
      return _renderRequest.getRemoteUser();
    }
    public boolean isUserInRole(String role)
    {
      return _renderRequest.isUserInRole(role);
    }
    public Principal getUserPrincipal()
    {
      return _renderRequest.getUserPrincipal();
    }

    public String getHeader(String name)
    {
      return _httpRequest.getHeader(name);
    }
    public Enumeration getHeaders(String name)
    {
      return _httpRequest.getHeaders(name);
    }
    public Enumeration getHeaderNames()
    {
      return _httpRequest.getHeaderNames();
    }
    public int getIntHeader(String name)
    {
      return _httpRequest.getIntHeader(name);
    }
    public long getDateHeader(String name)
    {
      return _httpRequest.getDateHeader(name);
    }
    public Cookie []getCookies()
    {
      return _httpRequest.getCookies();
    }

    public String getProtocol()
    {
      return null;
    }
    public String getRealPath(String uri)
    {
      return null;
    }
    public StringBuffer getRequestURL()
    {
      return null;
    }
    public String getRemoteAddr()
    {
      return null;
    }
    public String getRemoteHost()
    {
      return null;
    }
    public String getCharacterEncoding()
    {
      return null;
    }
    public void setCharacterEncoding(String encoding)
      throws UnsupportedEncodingException
    {
    }
    public ServletInputStream getInputStream()
      throws IOException
    {
      return null;
    }
    public BufferedReader getReader()
      throws IOException, IllegalStateException
    {
      return null;
    }
    public int getContentLength()
    {
      return 0;
    }

    public String getMethod()
    {
      return "GET";
    }

    public int getRemotePort()
    {
      return _httpRequest.getRemotePort();
    }
    public String getLocalAddr()
    {
      return _httpRequest.getLocalAddr();
    }
    public String getLocalName()
    {
      return _httpRequest.getLocalName();
    }
    public int getLocalPort()
    {
      return _httpRequest.getLocalPort();
    }
    public String getContentType()
    {
      return _httpRequest.getContentType();
    }
    public RequestDispatcher getRequestDispatcher(String uri)
    {
      return _httpRequest.getRequestDispatcher(uri);
    }
    public String getRequestURI()
    {
      return _httpRequest.getRequestURI();
    }
    public String getContextPath()
    {
      return _httpRequest.getContextPath();
    }
    public String getServletPath()
    {
      return _httpRequest.getServletPath();
    }
    public String getPathInfo()
    {
      return _httpRequest.getPathInfo();
    }
    public String getPathTranslated()
    {
      return _httpRequest.getPathTranslated();
    }
    public String getQueryString()
    {
      return _httpRequest.getQueryString();
    }
    public HttpSession getSession(boolean create)
    {
      return _httpRequest.getSession(create);
    }
    public HttpSession getSession()
    {
      return getSession(true);
    }
    public boolean isRequestedSessionIdFromCookie()
    {
      return _httpRequest.isRequestedSessionIdFromCookie();
    }
    public boolean isRequestedSessionIdFromURL()
    {
      return _httpRequest.isRequestedSessionIdFromURL();
    }
    /**
     * @deprecated
     */
    public boolean isRequestedSessionIdFromUrl()
    {
      return _httpRequest.isRequestedSessionIdFromUrl();
    }
  }

  static protected class HttpPortletResponseWrapper 
      implements HttpServletResponse
  {
    private HttpServletResponse _httpResponse;
    private RenderResponse _renderResponse;
    private ServletOutputStreamWrapper _servletOutputStream;

    public HttpPortletResponseWrapper()
    {
    }

    public void start(HttpServletResponse httpResponse, 
                      RenderResponse renderResponse)

    {
      _httpResponse = httpResponse;
      _renderResponse = renderResponse;
    }

    public void finish()
    {
      _servletOutputStream = null;
      _renderResponse = null;
      _httpResponse = null;
    }

    public void setBufferSize(int size)
    {
      _renderResponse.setBufferSize(size);
    }

    public int getBufferSize()
    {
      return _renderResponse.getBufferSize();
    }

    public void flushBuffer() 
      throws IOException
    {
      _renderResponse.flushBuffer();
    }

    public void resetBuffer()
    {
      _renderResponse.resetBuffer();
    }

    public void reset()
    {
      _renderResponse.reset();
    }

    public boolean isCommitted()
    {
      return _renderResponse.isCommitted();
    }

    public ServletOutputStream getOutputStream() 
      throws IOException
    {
      if (_servletOutputStream == null) {
        OutputStream portletOutputStream 
          = _renderResponse.getPortletOutputStream();

        _servletOutputStream 
          = new ServletOutputStreamWrapper(portletOutputStream);
      }

      return _servletOutputStream;
    }

    public String getCharacterEncoding()
    {
      return _renderResponse.getCharacterEncoding();
    }

    public String getContentType()
    {
      return _renderResponse.getContentType();
    }
    public void setCharacterEncoding(String enc) 
    {
    }

    public PrintWriter getWriter() 
      throws IOException
    {
      return _renderResponse.getWriter();
    }

    public String encodeURL(String path)
    {
      return _renderResponse.encodeURL(path);
    }

    public String encodeUrl(String path)
    {
      return _renderResponse.encodeURL(path);
    }

    public Locale getLocale()
    {
      return _renderResponse.getLocale();
    }

    public String encodeRedirectURL(String url)
    {
      return null;
    }
    public String encodeRedirectUrl(String url)
    {
      return null;
    }
    public void setContentType(String type)
    {
    }
    public void setContentLength(int len)
    {
    }
    public void setLocale(Locale locale)
    {
    }
    public void addCookie(Cookie cookie)
    {
    }
    public void sendError(int sc)
      throws IOException
    {
    }
    public void sendError(int sc, String msg)
      throws IOException
    {
    }
    public void sendRedirect(String location)
      throws IOException
    {
    }
    public void setDateHeader(String name, long date)
    {
    }
    public void addDateHeader(String name, long date)
    {
    }
    public void setHeader(String name, String value)
    {
    }
    public void addHeader(String name, String value)
    {
    }
    public void setIntHeader(String name, int value)
    {
    }
    public void addIntHeader(String name, int value)
    {
    }
    public boolean containsHeader(String name)
    {
      return false;
    }
    public void setStatus(int sc)
    {
    }
    public void setStatus(int sc, String msg)
    {
    }
  }

  static final protected class ServletOutputStreamWrapper 
      extends ServletOutputStream
  {
    private OutputStream _out;

    public ServletOutputStreamWrapper(OutputStream out)
    {
      _out = out;
    }

    public void write(int b) 
      throws IOException
    {
      _out.write(b);
    }

    public void write(byte b[], int off, int len) 
      throws IOException 
    {
      _out.write(b, off, len);
    }

    public void write(byte b[]) 
      throws IOException 
    {
      _out.write(b);
    }

    public void flush() 
      throws IOException 
    {
      _out.flush();
    }

    public void close() 
      throws IOException 
    {
      _out.close();
    }
  }
}


