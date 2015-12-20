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

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestCauchoBase;
import com.caucho.v5.http.protocol.RequestCauchoWrapper;

/**
 * Represents the next filter in a filter chain.  The final filter will
 * be the servlet itself.
 */
public class FilterChainDispatch implements FilterChain {
  // Next filter chain
  private FilterChain _next;

  // app
  private WebApp _webApp;
  // class loader
  private ClassLoader _classLoader;

  private ServletRequestListener []_requestListeners;
  
  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param webApp the webApp
   */
  public FilterChainDispatch(FilterChain next,
                             WebApp webApp)
  {
    _next = next;
    _webApp = webApp;
    
    if (webApp != null) {
      _classLoader = webApp.getClassLoader();
      _requestListeners = webApp.getRequestListeners();
    } else {
      // server/1d01
      _classLoader = Thread.currentThread().getContextClassLoader();
      _requestListeners = new ServletRequestListener[0];
    }
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
    
    // server/10gf, server/10gv - listeners on web-app change
    ServletContext webApp;
    if (request instanceof RequestCauchoWrapper) {
      RequestCauchoWrapper cReq = (RequestCauchoWrapper) request;
    
      webApp = cReq.getRequest().getServletContext();
    }
    else if (request instanceof RequestCauchoBase) {
      // server/10gf
      RequestCauchoBase cReq = (RequestCauchoBase) request;
    
      if (cReq.getRequest() != null)
        webApp = cReq.getRequest().getServletContext();
      else
        webApp = cReq.getServletContext();
    }
    else {
      webApp = request.getServletContext();
    }
    
    try {
      thread.setContextClassLoader(_classLoader);

      if (webApp != _webApp) {
        for (int i = 0; i < _requestListeners.length; i++) {
          ServletRequestEvent event
            = new ServletRequestEvent(_webApp, request);
          
          _requestListeners[i].requestInitialized(event);
        }
      }
      
      _next.doFilter(request, response);
    } catch (FileNotFoundException e) {
      // server/106c
      /*
      log.log(Level.FINER, e.toString(), e);
      
      HttpServletResponse res = (HttpServletResponse) response;
      
      res.sendError(404);
      */
      
      throw e;
    } finally {
      if (webApp != _webApp) {
        for (int i = _requestListeners.length - 1; i >= 0; i--) {
          ServletRequestEvent event
            = new ServletRequestEvent(_webApp, request);
          
          _requestListeners[i].requestDestroyed(event);
        }
      }

      thread.setContextClassLoader(oldLoader);
    }
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _next + "]";
  }
}
