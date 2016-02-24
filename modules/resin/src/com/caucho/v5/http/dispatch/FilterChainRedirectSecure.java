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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

import com.caucho.v5.http.host.*;
import com.caucho.v5.http.webapp.*;

/**
 * Represents the final servlet in a filter chain.
 */
public class FilterChainRedirectSecure implements FilterChain {
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   *
   * @since Servlet 2.3
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    WebAppResinBase webApp = (WebAppResinBase) req.getServletContext();
    String path = req.getContextPath();
    String servletPath = req.getServletPath();
    String pathInfo = req.getPathInfo();

    if (servletPath != null)
      path += servletPath;

    if (pathInfo != null)
      path += pathInfo;
    
    String queryString = req.getQueryString();

    if (queryString != null)
      path += "?" + queryString;

    Host host = webApp.getHost();
    String secureHostName = req.getServerName();

    if (host != null && host.getSecureHostName() != null)
      secureHostName = host.getSecureHostName();
    
    res.sendRedirect(res.encodeURL("https://" + secureHostName + path));
  }
}
