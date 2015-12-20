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

package com.caucho.v5.http.webapp;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestServlet;

/**
 * Represents the next filter in a filter chain.  The final filter will
 * be the servlet itself.
 */
public class FilterChainResume implements FilterChainCaucho
{
  private static final Logger log
    = Logger.getLogger(FilterChainResume.class.getName());

  // Next filter chain
  private FilterChain _next;

  // app
  private WebApp _webApp;
  
  // error page manager
  private ErrorPageManager _errorPageManager;

  private HashMap<String,String> _securityRoleMap;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public FilterChainResume(FilterChain next, WebApp webApp)
  {
    _next = next;
    _webApp = webApp;
    _errorPageManager = webApp.getErrorPageManager();
  }

  /**
   * Sets the security map.
   */
  public void setSecurityRoleMap(HashMap<String,String> map)
  {
    _securityRoleMap = map;
  }

  /**
   * Returns true if cacheable.
   */
  public FilterChain getNext()
  {
    return _next;
  }

  /**
   * Invokes the next filter in the chain or the final servlet at
   * the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   * @since Servlet 2.3
   */
  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    Thread thread = Thread.currentThread();
    ClassLoader oldLoader = thread.getContextClassLoader();

    WebApp webApp = _webApp;

    try {
      thread.setContextClassLoader(webApp.getClassLoader());

      if (! webApp.enterWebApp() && webApp.getConfigException() == null) {
        throw new IllegalStateException("Cannot enter web-app");
      }

      _next.doFilter(request, response);
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      webApp.exitWebApp();

      // put finish() before access log so the session isn't tied up while
      // logging

      // needed for things like closing the session
      if (request instanceof RequestServlet)
        ((RequestServlet) request).finishInvocation();

      // ((CauchoResponse) response).close();

      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[" + _webApp.getURL()
            + ", next=" + _next + "]");
  }
}
