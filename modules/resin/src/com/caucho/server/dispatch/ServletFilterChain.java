/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.dispatch;

import java.util.*;
import java.io.*;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;

import javax.servlet.http.HttpServletResponse;

/**
 * Represents the final servlet in a filter chain.
 */
public class ServletFilterChain implements FilterChain {
  public static String SERVLET_NAME = "javax.servlet.error.servlet_name";
  
  // servlet config
  private ServletConfigImpl _config;
  // servlet
  private Servlet _servlet;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the underlying servlet
   */
  public ServletFilterChain(ServletConfigImpl config)
  {
    if (config == null)
      throw new NullPointerException();
    
    _config = config;
  }

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    return _config.getServletName();
  }
  
  
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
    if (_servlet == null) {
      try {
        _servlet = _config.createServlet();
      } catch (ServletException e) {
        throw e;
      } catch (Exception e) {
        throw new ServletException(e);
      }
    }
    
    try {
      _servlet.service(request, response);
    } catch (UnavailableException e) {
      _servlet = null;
      _config.setInitException(e);
      _config.killServlet();
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (ServletException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (IOException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    }
  }
}
