/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import com.caucho.servlet.comet.CometFilterChain;
import com.caucho.server.http.AbstractHttpRequest;
import com.caucho.server.http.AbstractHttpResponse;
import com.caucho.server.http.CauchoResponse;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.server.log.AbstractAccessLog;
import com.caucho.transaction.TransactionManagerImpl;
import com.caucho.transaction.UserTransactionImpl;
import com.caucho.transaction.UserTransactionProxy;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents the filter chain calling listeners.
 */
public class WebAppListenerFilterChain implements CauchoFilterChain {
  private static final Logger log
    = Logger.getLogger(WebAppListenerFilterChain.class.getName());

  // Next filter chain
  private FilterChain _next;

  // app
  private WebApp _webApp;

  private ServletRequestListener []_requestListeners;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public WebAppListenerFilterChain(FilterChain next, 
                                   WebApp webApp,
                                   ServletRequestListener []requestListeners)
  {
    _next = next;
    _webApp = webApp;
    _requestListeners = requestListeners;
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
    try {
      for (int i = 0; i < _requestListeners.length; i++) {
        ServletRequestEvent event = new ServletRequestEvent(_webApp, request);

        _requestListeners[i].requestInitialized(event);
      }

      _next.doFilter(request, response);
    } finally {
      for (int i = _requestListeners.length - 1; i >= 0; i--) {
        try {
          ServletRequestEvent event = new ServletRequestEvent(_webApp, request);

          _requestListeners[i].requestDestroyed(event);
        } catch (Throwable e) {
          log.log(Level.WARNING, e.toString(), e);
        }
      }
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
