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

import com.caucho.vfs.*;
import com.caucho.server.webapp.WebApp;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class CauchoRequestWrapper implements CauchoRequest {
  // the wrapped request
  private HttpServletRequest _request;

  //
  // ServletRequest
  //
  
  public CauchoRequestWrapper()
  {
  }
  
  public CauchoRequestWrapper(HttpServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  public void setRequest(HttpServletRequest request)
  {
    if (request == null)
      throw new IllegalArgumentException();
    
    _request = request;
  }
  
  public HttpServletRequest getRequest()
  {
    return _request;
  }
  
  public String getProtocol()
  {
    return _request.getProtocol();
  }
  
  public String getScheme()
  {
    return _request.getScheme();
  }
  
  public String getServerName()
  {
    return _request.getServerName();
  }
  
  public int getServerPort()
  {
    return _request.getServerPort();
  }
  
  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }
  
  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }
  
  public int getRemotePort()
  {
    return _request.getRemotePort();
  }
  
  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }
  
  public String getLocalName()
  {
    return _request.getLocalName();
  }
  
  public int getLocalPort()
  {
    return _request.getLocalPort();
  }
  
  public String getParameter(String name)
  {
    return _request.getParameter(name);
  }
  
  public Map getParameterMap()
  {
    return _request.getParameterMap();
  }
  
  public String []getParameterValues(String name)
  {
    return _request.getParameterValues(name);
  }
  
  public Enumeration getParameterNames()
  {
    return _request.getParameterNames();
  }
  
  public ServletInputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }
  
  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }
  
  public String getCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }
  
  public void setCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }
  
  public int getContentLength()
  {
    return _request.getContentLength();
  }
  
  public String getContentType()
  {
    return _request.getContentType();
  }
  
  public Locale getLocale()
  {
    return _request.getLocale();
  }
  
  public Enumeration getLocales()
  {
    return _request.getLocales();
  }
  
  public boolean isSecure()
  {
    return _request.isSecure();
  }
  
  public Object getAttribute(String name)
  {
    return _request.getAttribute(name);
  }
  
  public void setAttribute(String name, Object o)
  {
    _request.setAttribute(name, o);
  }
  
  public Enumeration getAttributeNames()
  {
    return _request.getAttributeNames();
  }
  
  public void removeAttribute(String name)
  {
    _request.removeAttribute(name);
  }
  
  public RequestDispatcher getRequestDispatcher(String uri)
  {
    return _request.getRequestDispatcher(uri);
  }

  public String getRealPath(String uri)
  {
    return _request.getRealPath(uri);
  }

  public ServletContext getServletContext()
  {
    return _request.getServletContext();
  }

  public void addAsyncListener(AsyncListener listener)
  {
    _request.addAsyncListener(listener);
  }

  public void addAsyncListener(AsyncListener listener,
			       ServletRequest request,
			       ServletResponse response)
  {
    _request.addAsyncListener(listener, request, response);
  }

  public AsyncContext getAsyncContext()
  {
    return _request.getAsyncContext();
  }

  public boolean isAsyncStarted()
  {
    return _request.isAsyncStarted();
  }

  public boolean isAsyncSupported()
  {
    return _request.isAsyncSupported();
  }

  public void setAsyncTimeout(long timeout)
  {
    _request.setAsyncTimeout(timeout);
  }

  public long getAsyncTimeout()
  {
    return _request.getAsyncTimeout();
  }

  public AsyncContext startAsync()
    throws IllegalStateException
  {
    return _request.startAsync();
  }

  public AsyncContext startAsync(ServletRequest servletRequest,
                                 ServletResponse servletResponse)
    throws IllegalStateException
  {
    return _request.startAsync(servletRequest, servletResponse);
  }

  public boolean isWrapperFor(ServletRequest wrapped)
  {
    return _request == wrapped;
  }

  public boolean isWrapperFor(Class wrappedType)
  {
    return wrappedType.isAssignableFrom(_request.getClass());
  }

  public DispatcherType getDispatcherType()
  {
    return _request.getDispatcherType();
  }

  //
  // HttpServletRequest
  //
  
  public String getMethod()
  {
    return _request.getMethod();
  }
  
  public String getRequestURI()
  {
    return _request.getRequestURI();
  }
  
  public StringBuffer getRequestURL()
  {
    return _request.getRequestURL();
  }
  
  public String getContextPath()
  {
    return _request.getContextPath();
  }
  
  public String getServletPath()
  {
    return _request.getServletPath();
  }
  
  public String getPathInfo()
  {
    return _request.getPathInfo();
  }
  
  public String getPathTranslated()
  {
    return _request.getPathTranslated();
  }
  
  public String getQueryString()
  {
    return _request.getQueryString();
  }
  
  public String getHeader(String name)
  {
    return _request.getHeader(name);
  }
  
  public Enumeration getHeaders(String name)
  {
    return _request.getHeaders(name);
  }
  
  public Enumeration getHeaderNames()
  {
    return _request.getHeaderNames();
  }
  
  public int getIntHeader(String name)
  {
    return _request.getIntHeader(name);
  }
  
  public long getDateHeader(String name)
  {
    return _request.getDateHeader(name);
  }
  
  public Cookie []getCookies()
  {
    return _request.getCookies();
  }
  
  public HttpSession getSession(boolean create)
  {
    return _request.getSession(create);
  }
  
  public HttpSession getSession()
  {
    return getSession(true);
  }
  
  public String getRequestedSessionId()
  {
    return _request.getRequestedSessionId();
  }
  
  public boolean isRequestedSessionIdValid()
  {
    return _request.isRequestedSessionIdValid();
  }
  
  public boolean isRequestedSessionIdFromCookie()
  {
    return _request.isRequestedSessionIdFromCookie();
  }
  
  public boolean isRequestedSessionIdFromURL()
  {
    return _request.isRequestedSessionIdFromURL();
  }
  
  public String getAuthType()
  {
    return _request.getAuthType();
  }
  
  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }
  
  public boolean isUserInRole(String role)
  {
    return _request.isUserInRole(role);
  }
  
  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }
  
  public boolean isRequestedSessionIdFromUrl()
  {
    return _request.isRequestedSessionIdFromUrl();
  }

  public boolean authenticate(HttpServletResponse response)
    throws IOException, ServletException
  {
    return _request.authenticate(response);
  }

  public Part getPart(String name)
    throws IOException, ServletException
  {
    return _request.getPart(name);
  }

  public Iterable<Part> getParts()
    throws IOException, ServletException
  {
    return _request.getParts();
  }

  public void login(String username, String password)
    throws ServletException
  {
    _request.login(username, password);
  }

  public void logout()
    throws ServletException
  {
    _request.logout();
  }

  //
  // CauchoRequest
  //
  
  public String getPageURI()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageURI();
  }
  
  public String getPageContextPath()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageContextPath();
  }
  
  public String getPageServletPath()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageServletPath();
  }
  
  public String getPagePathInfo()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPagePathInfo();
  }
  
  public String getPageQueryString()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getPageQueryString();
  }
  
  public WebApp getWebApp()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getWebApp();
  }
  
  public ReadStream getStream() throws IOException
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getStream();
  }
  
  public int getRequestDepth(int depth)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getRequestDepth(depth + 1);
  }
  
  public void setHeader(String key, String value)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setHeader(key, value);
  }
  
  public boolean getVaryCookies()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getVaryCookies();
  }
  
  public void setVaryCookie(String cookie)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setVaryCookie(cookie);
  }
  
  public boolean getHasCookie()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getHasCookie();
  }
  

  public boolean isTop()
  {
    return false;
  }
  

  public boolean hasRequest()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.hasRequest();
  }
  
  
  public HttpSession getMemorySession()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getMemorySession();
  }
  
  public Cookie getCookie(String name)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getCookie(name);
  }
  
  public void setHasCookie()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.setHasCookie();
  }
  
  public void killKeepalive()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.killKeepalive();
  }
  
  public boolean isSuspend()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isSuspend();
  }
  
  public boolean isComet()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isComet();
  }
  
  public boolean isDuplex()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isDuplex();
  }
  
  public boolean allowKeepalive()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.allowKeepalive();
  }
  
  public boolean isClientDisconnect()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isClientDisconnect();
  }
  
  public void clientDisconnect()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    cRequest.clientDisconnect();
  }

  public boolean isLoginRequested()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.isLoginRequested();
  }
  
  public boolean login(boolean isFail)
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.login(isFail);
  }
  
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  
  public AbstractHttpRequest getAbstractHttpRequest()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getAbstractHttpRequest();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _request + "]";
  }
}
