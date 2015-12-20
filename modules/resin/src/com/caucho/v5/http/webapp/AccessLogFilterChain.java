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
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.log.AccessLogBase;
import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.http.protocol.RequestServlet;
import com.caucho.v5.http.protocol.ResponseServlet;
import com.caucho.v5.network.listen.ConnectionSocket;

/**
 * Logs the request.
 */
public class AccessLogFilterChain implements FilterChainCaucho {
  private static final Logger log
    = Logger.getLogger(AccessLogFilterChain.class.getName());

  // Next filter chain
  private final FilterChain _next;

  // app
  private final WebApp _webApp;
  private final AccessLogBase _accessLog;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public AccessLogFilterChain(FilterChain next, WebApp webApp)
  {
    _next = next;
    _webApp = webApp;
    _accessLog = webApp.getAccessLog();
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
    _next.doFilter(request, response);
    
    ConnectionSocket socketLink = null;
    
    if (request instanceof RequestCaucho) {
      socketLink = ((RequestCaucho) request).getSocketLink();
    }
    
    try {
      if (socketLink != null) {
        socketLink.requestShutdownBegin();
      }
      
      _accessLog.log((RequestServlet) request, (ResponseServlet) response);
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      if (socketLink != null)
        socketLink.requestShutdownEnd();
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
