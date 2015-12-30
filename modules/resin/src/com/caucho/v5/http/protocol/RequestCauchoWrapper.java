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
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;

import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.util.CharBuffer;
import com.caucho.v5.vfs.ReadStream;

public class RequestCauchoWrapper implements RequestCaucho
{
  // the wrapped request
  private HttpServletRequest _request;
  private ResponseCaucho _response;

  //
  // ServletRequest
  //
  
  public RequestCauchoWrapper()
  {
  }
  
  public RequestCauchoWrapper(HttpServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  public void setRequest(HttpServletRequest request)
  {
    if (request == null || request == this)
      throw new IllegalArgumentException();
    
    _request = request;
  }

  /*
  @Override
  public HttpSession getSession()
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getSession();
    else
      return null;
  }
  */

  public HttpServletRequest getRequest()
  {
    return _request;
  }
  
  public void setResponse(ResponseCaucho response)
  {
    _response = response;
  }
  
  public ResponseCaucho getResponse()
  {
    return _response;
  }
  
  @Override
  public String getProtocol()
  {
    return _request.getProtocol();
  }

  @Override
  public String getScheme()
  {
    return _request.getScheme();
  }
  
  @Override
  public String getServerName()
  {
    return _request.getServerName();
  }
  
  @Override
  public int getServerPort()
  {
    return _request.getServerPort();
  }
  
  @Override
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }
  
  @Override
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }
  
  @Override
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }
  
  @Override
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }
  
  @Override
  public String getLocalName()
  {
    return _request.getLocalName();
  }
  
  @Override
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }
  
  @Override
  public String getParameter(String name)
  {
    return _request.getParameter(name);
  }
  
  @Override
  public Map<String,String[]> getParameterMap()
  {
    return _request.getParameterMap();
  }
  
  @Override
  public String []getParameterValues(String name)
  {
    return _request.getParameterValues(name);
  }
  
  @Override
  public Enumeration<String> getParameterNames()
  {
    return _request.getParameterNames();
  }
  
  @Override
  public ServletInputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }
  
  @Override
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }
  
  @Override
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }
  
  @Override
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }
  
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
  
  @Override
  public String getContentType()
  {
    return _request.getContentType();
  }
  
  @Override
  public Locale getLocale()
  {
    return _request.getLocale();
  }
  
  @Override
  public Enumeration<Locale> getLocales()
  {
    return _request.getLocales();
  }
  
  @Override
  public boolean isSecure()
  {
    return _request.isSecure();
  }
  
  @Override
  public Object getAttribute(String name)
  {
    return _request.getAttribute(name);
  }
  
  @Override
  public void setAttribute(String name, Object o)
  {
    _request.setAttribute(name, o);
  }
  
  @Override
  public Enumeration<String> getAttributeNames()
  {
    return _request.getAttributeNames();
  }
  
  @Override
  public void removeAttribute(String name)
  {
    _request.removeAttribute(name);
  }
  
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

  /*
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }
  */

  @Override
  public ServletContext getServletContext()
  {
    return getWebApp();
  }

  @Override
  public AsyncContext startAsync()
    throws IllegalStateException
  {
    return _request.startAsync();
  }

  @Override
  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException
  {
    return _request.startAsync(servletRequest, servletResponse);
  }

  @Override
  public AsyncContext getAsyncContext()
  {
    return _request.getAsyncContext();
  }

  @Override
  public boolean isAsyncStarted()
  {
    return _request.isAsyncStarted();
  }

  @Override
  public boolean isAsyncSupported()
  {
    return _request.isAsyncSupported();
  }

  public boolean isWrapperFor(ServletRequest wrapped)
  {
    return _request == wrapped;
  }

  public boolean isWrapperFor(Class<?> wrappedType)
  {
    return wrappedType.isAssignableFrom(_request.getClass());
  }

  @Override
  public DispatcherType getDispatcherType()
  {
    return _request.getDispatcherType();
  }

  //
  // HttpServletRequest
  //
  
  @Override
  public String getMethod()
  {
    return _request.getMethod();
  }
  
  @Override
  public String getRequestURI()
  {
    return _request.getRequestURI();
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
  
  @Override
  public String getContextPath()
  {
    return _request.getContextPath();
  }
  
  @Override
  public String getServletPath()
  {
    return _request.getServletPath();
  }
  
  @Override
  public String getPathInfo()
  {
    return _request.getPathInfo();
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
  public String getQueryString()
  {
    return _request.getQueryString();
  }
  
  @Override
  public String getHeader(String name)
  {
    return _request.getHeader(name);
  }
  
  @Override
  public Enumeration<String> getHeaders(String name)
  {
    return _request.getHeaders(name);
  }
  
  @Override
  public Enumeration<String> getHeaderNames()
  {
    return _request.getHeaderNames();
  }
  
  @Override
  public int getIntHeader(String name)
  {
    return _request.getIntHeader(name);
  }
  
  @Override
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }
  
  @Override
  public Cookie []getCookies()
  {
    return _request.getCookies();
  }

  @Override
  public String getRequestedSessionId()
  {
    return _request.getRequestedSessionId();
  }

  @Override
  public boolean isRequestedSessionIdValid()
  {
    return _request.isRequestedSessionIdValid();
  }
  
  @Override
  public boolean isRequestedSessionIdFromCookie()
  {
    return _request.isRequestedSessionIdFromCookie();
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    return _request.isRequestedSessionIdFromURL();
  }

  @Override
  public void setSessionId(String sessionId)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setSessionId(sessionId);
  }

  @Override
  public String getSessionId()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getSessionId();
    else
      return null;
  }

  @Override
  public boolean isSessionIdFromCookie()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isSessionIdFromCookie();
    else
      return ! _request.isRequestedSessionIdFromURL();
  }
  
  @Override
  public String getAuthType()
  {
    return _request.getAuthType();
  }
  
  @Override
  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }
  
  /*
  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }
  */
  
  @Override
  @SuppressWarnings("deprecation")
  public boolean isRequestedSessionIdFromUrl()
  {
    return _request.isRequestedSessionIdFromUrl();
  }

  /*
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return _request.authenticate(response);
  }
  */

  @Override
  public Part getPart(String name)
    throws IOException, ServletException
  {
    return _request.getPart(name);
  }

  @Override
  public Collection<Part> getParts()
    throws IOException, ServletException
  {
    return _request.getParts();
  }
  
  @Override
  public boolean isMultipartEnabled()
  {
    if (_request instanceof RequestCaucho) {
      return ((RequestCaucho) _request).isMultipartEnabled();
    }

    return false;
  }

  @Override
  public void logout()
    throws ServletException
  {
    _request.logout();
  }
  
  //
  // CauchoRequest
  //
  
  @Override
  public String getPageURI()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getPageURI();
    else
      return _request.getRequestURI();
  }
  
  @Override
  public String getPageContextPath()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getPageContextPath();
    else
      return _request.getContextPath();
  }
  
  @Override
  public String getPageServletPath()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getPageServletPath();
    else
      return _request.getServletPath();
  }
  
  @Override
  public String getPagePathInfo()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getPagePathInfo();
    else
      return _request.getPathInfo();
  }
  
  @Override
  public String getPageQueryString()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getPageQueryString();
    else
      return _request.getQueryString();
  }
  
  @Override
  public WebApp getWebApp()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getWebApp();
    else
      return (WebApp) _request.getServletContext();
  }
  
  @Override
  public ReadStream getStream() throws IOException
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getStream();
    else
      throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public int getRequestDepth(int depth)
  {
    if (_request instanceof RequestCaucho) {
      RequestCaucho cRequest = (RequestCaucho) _request;

      return cRequest.getRequestDepth(depth + 1);
    }
    else
      return 0;
  }
  
  @Override
  public void setHeader(String key, String value)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setHeader(key, value);
  }
  
  @Override
  public boolean isSyntheticCacheHeader()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isSyntheticCacheHeader();
    else
      return false;
  }
  
  @Override
  public void setSyntheticCacheHeader(boolean isSynthetic)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setSyntheticCacheHeader(isSynthetic);
  }
  
  @Override
  public boolean getVaryCookies()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getVaryCookies();
    else
      return false;
  }
  
  @Override
  public void setVaryCookie(String cookie)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setVaryCookie(cookie);
  }
  
  @Override
  public boolean getHasCookie()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getHasCookie();
    else
      return false;
  }
  

  @Override
  public boolean isTop()
  {
    return false;
  }
  

  @Override
  public boolean hasRequest()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.hasRequest();
    else
      return false;
  }
  
  @Override
  public HttpSession getMemorySession()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getMemorySession();
    else
      throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public Cookie getCookie(String name)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getCookie(name);
    else
      throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public void setHasCookie()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.setHasCookie();
  }
  
  @Override
  public void killKeepalive(String reason)
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.killKeepalive(reason);
  }
  
  @Override
  public boolean isSuspend()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isSuspend();
    else
      return false;
  }
  
  @Override
  public boolean isComet()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isComet();
    else
      return false;
  }
  
  @Override
  public void completeCache()
  {
  }
  
  @Override
  public boolean isDuplex()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isDuplex();
    else
      return false;
  }
  
  @Override
  public boolean isConnectionClosed()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isConnectionClosed();
    else
      return false;
  }

  @Override
  public boolean isLoginRequested()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.isLoginRequested();
    else
      return false;
  }

  @Override
  public void requestLogin()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      cRequest.requestLogin();
  }
 
  /*
  public boolean login(boolean isFail)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.login(isFail);
  }
  */
  
  @Override
  public ServletResponse getServletResponse()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getServletResponse();
    else
      throw new UnsupportedOperationException(getClass().getName());
  }
  
  @Override
  public RequestHttpBase getAbstractHttpRequest()
  {
    RequestCaucho cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getAbstractHttpRequest();
    else
      throw new UnsupportedOperationException(getClass().getName());
  }

  private RequestCaucho getCauchoRequest()
  {
    ServletRequest request = _request;
    
    while (request instanceof ServletRequestWrapper) {
      if (request instanceof RequestCaucho)
        return (RequestCaucho) request;

      request = ((ServletRequestWrapper) request).getRequest();
    }

    if (request instanceof RequestCaucho)
      return (RequestCaucho) request;

    return null;
  }

  @Override
  public HttpSession getSession(boolean create)
  {
    return _request.getSession(create);
  }

  @Override
  public HttpSession getSession()
  {
    return getSession(true);
  }

  @Override
  public boolean isUserInRole(String role)
  {
    return _request.isUserInRole(role);
  }

  @Override
  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }

  @Override
  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return _request.authenticate(response);
  }

  @Override
  public void login(String username, String password)
    throws ServletException
  {
    _request.login(username,  password);
  }

  @Override
  @SuppressWarnings("deprecation")
  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }

  @Override
  public boolean login(boolean isFail)
  {
    RequestCaucho request = getCauchoRequest();
    
    if (request != null) {
      return request.login(isFail);
    }
    
    return false;
  }

  @Override
  public ConnectionTcp getSocketLink()
  {
    RequestCaucho request = getCauchoRequest();
    
    if (request != null) {
      return request.getSocketLink();
    }
    
    return null;
  }

  @Override
  public String changeSessionId()
  {
    return _request.changeSessionId();
  }

  @Override
  public <T extends HttpUpgradeHandler> T upgrade(Class<T> handlerClass)
  {
    return _request.upgrade(handlerClass);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getPageURI() + "," + _request + "]";
  }
}
