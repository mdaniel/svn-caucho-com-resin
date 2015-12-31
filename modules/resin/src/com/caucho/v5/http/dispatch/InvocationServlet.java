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

package com.caucho.v5.http.dispatch;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.MultipartConfigElement;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestFacade;
import com.caucho.v5.http.protocol.ConnectionHttp;
import com.caucho.v5.http.protocol.ResponseFacade;
import com.caucho.v5.http.webapp.WebApp;
import com.caucho.v5.network.port.ConnectionTcp;
import com.caucho.v5.network.port.ConnectionProtocol;
import com.caucho.v5.vfs.Dependency;

/**
 * A repository for request information gleaned from the uri.
 */
public class InvocationServlet extends Invocation
{
  private static final Logger log
    = Logger.getLogger(InvocationServlet.class.getName());

  private final boolean _isFiner;

  private String _sessionId;

  private String _contextPath = "";

  private String _contextUri;
  private String _servletPath;
  private String _pathInfo;

  private WebApp _webApp;

  private String _servletName;
  private FilterChain _filterChain;

  private boolean _isAsyncSupported = true;
  private MultipartConfigElement _multipartConfig;

  private AtomicLong _requestCount = new AtomicLong();

  private HashMap<String,String> _securityRoleMap;

  public InvocationServlet()
  {
    _isFiner = log.isLoggable(Level.FINER);
  }

  /**
   * Returns the mapped context-path.
   */
  public final String getContextPath()
  {
    return _contextPath;
  }

  /**
   * Sets the context-path.
   */
  public void setContextPath(String path)
  {
    _contextPath = path;
  }

  public void setContextURI(String contextURI)
  {
    _contextUri = contextURI;
    _servletPath = contextURI;
  }

  /**
   * Returns the URI tail, i.e. everything after the context path.
   */
  public final String getContextURI()
  {
    return _contextUri;
  }

  /**
   * Returns the mapped servlet path.
   */
  public final String getServletPath()
  {
    return _servletPath;
  }

  /**
   * Sets the mapped servlet path.
   */
  public void setServletPath(String servletPath)
  {
    _servletPath = servletPath;
  }

  /**
   * Returns the mapped path info.
   */
  public final String getPathInfo()
  {
    return _pathInfo;
  }

  /**
   * Sets the mapped path info
   */
  public void setPathInfo(String pathInfo)
  {
    _pathInfo = pathInfo;
  }

  /**
   * Returns a URL-based session id.
   */
  public final String getSessionId()
  {
    return _sessionId;
  }

  /**
   * Sets the URL-based session id.
   */
  public final void setSessionId(String sessionId)
  {
    _sessionId = sessionId;
  }

  /**
   * Returns the mapped webApp.
   */
  public final WebApp getWebApp()
  {
    return _webApp;
  }

  /**
   * Sets the mapped webApp.
   */
  public void setWebApp(WebApp app)
  {
    _webApp = app;
  }

  /**
   * Returns true if the invocation has been modified.  Generally only
   * true if the webApp has been modified.
   */
  @Override
  public boolean isModified()
  {
    Dependency depend = getDependency();

    if (depend != null && depend.isModified()) {
      return true;
    }

    WebApp webApp = _webApp;

    if (webApp != null) {
      depend = webApp.getInvocationDependency();

      if (depend != null) {
        return depend.isModified();
      }
    }

    return true;
  }

  /**
   * Log the reason for modification.
   */
  @Override
  public boolean logModified(Logger log)
  {
    Dependency depend = getDependency();

    if (depend != null && depend.logModified(log)) {
      return true;
    }

    WebApp app = _webApp;

    if (app != null) {
      depend = app.getInvocationDependency();

      if (depend != null) {
        return depend.logModified(log);
      }
    }

    return true;
  }

  /**
   * Sets the servlet name
   */
  public void setServletName(String servletName)
  {
    _servletName = servletName;
  }

  /**
   * Gets the servlet name
   */
  public String getServletName()
  {
    return _servletName;
  }

  /**
   * Sets the filter chain
   */
  public void setFilterChain(FilterChain chain)
  {
    _filterChain = chain;
  }

  /**
   * Gets the filter chain
   */
  public FilterChain getFilterChain()
  {
    return _filterChain;
  }

  /**
   * Gets the security role map.
   */
  public HashMap<String,String> getSecurityRoleMap()
  {
    return _securityRoleMap;
  }

  /**
   * Sets the security role map.
   */
  public void setSecurityRoleMap(HashMap<String,String> roleMap)
  {
    _securityRoleMap = roleMap;
  }

  /**
   * Returns the number of requests.
   */
  public long getRequestCount()
  {
    return _requestCount.get();
  }

  /**
   * True if the invocation chain supports async (comet) requets.
   */
  public boolean isAsyncSupported()
  {
    return _isAsyncSupported;
  }

  /**
   * Mark the invocation chain as not supporting async.
   */
  public void clearAsyncSupported()
  {
    _isAsyncSupported = false;
  }

  public MultipartConfigElement getMultipartConfig() 
  {
    return _multipartConfig;
  }

  public void setMultipartConfig(MultipartConfigElement multipartConfig)
  {
    _multipartConfig = multipartConfig;
  }

  /**
   * Returns the thread request.
   */
  public static ServletRequest getContextRequest()
  {
    ConnectionProtocol req = ConnectionTcp.getCurrentRequest();

    if (req instanceof ConnectionHttp) {
      return (ServletRequest) ((ConnectionHttp) req).request();
    }
    else if (req instanceof ServletRequest) {
      return (ServletRequest) req;
    }
    else {
      return null;
    }
  }

  /**
   * Returns the versioned invocation based on this request.
   *
   * @param request the servlet request
   */
  public InvocationServlet getRequestInvocation(RequestFacade request)
  {
    return this;
  }

  /**
   * Service a request.
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public void service(RequestFacade request, ResponseFacade response)
    throws IOException, ServletException
  {
    _requestCount.incrementAndGet();

    if (_isFiner) {
      log.finer("Dispatch '" + _contextUri + "' to " + _filterChain);
    }
    
    ServletRequest requestServlet = (ServletRequest) request;
    ServletResponse responseServlet = (ServletResponse) response;

    _filterChain.doFilter(requestServlet, responseServlet);
  }

  /**
   * Copies from the invocation.
   */
  public void copyFrom(InvocationServlet invocation)
  {
    super.copyFrom(invocation);

    _servletName = invocation._servletName;
    _filterChain = invocation._filterChain;

    _securityRoleMap = invocation._securityRoleMap;
    
    _contextPath = invocation._contextPath;

    _contextUri = invocation._contextUri;
    _servletPath = invocation._servletPath;
    _pathInfo = invocation._pathInfo;

    // server/1h25
    _sessionId = invocation._sessionId;

    _webApp = invocation._webApp;
  }

  void close()
  {
    _webApp = null;
  }

  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder();

    sb.append(getClass().getSimpleName());
    sb.append("[");
    sb.append(getContextPath());

    if (getQueryString() != null)
      sb.append("?").append(getQueryString());
    
    sb.append(",").append(_webApp);

    sb.append("]");

    return sb.toString();
  }
}
