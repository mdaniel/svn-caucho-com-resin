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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * Represents the final servlet in a filter chain.
 */
public class ErrorPageFilter implements Filter {
  private static final Logger log
    = Logger.getLogger(ErrorPageFilter.class.getName());
  
  public static String REQUEST_URI = "javax.servlet.include.request_uri";
  public static String CONTEXT_PATH = "javax.servlet.include.context_path";
  public static String SERVLET_PATH = "javax.servlet.include.servlet_path";
  public static String PATH_INFO = "javax.servlet.include.path_info";
  public static String QUERY_STRING = "javax.servlet.include.query_string";
  
  public static String STATUS_CODE = "javax.servlet.error.status_code";
  public static String EXCEPTION_TYPE = "javax.servlet.error.exception_type";
  public static String MESSAGE = "javax.servlet.error.message";
  public static String EXCEPTION = "javax.servlet.error.exception";
  public static String ERROR_URI = "javax.servlet.error.request_uri";
  public static String SERVLET_NAME = "javax.servlet.error.servlet_name";
  
  public static String JSP_EXCEPTION = "javax.servlet.jsp.jspException";
  
  public static String SHUTDOWN = "com.caucho.shutdown";
  
  private FilterConfig _config;
  private WebApp _app;
  private ErrorPageManager _errorPageManager;

  /**
   * Create error page filter.
   */
  public ErrorPageFilter()
  {
  }

  /**
   * Create error page filter.
   */
  public ErrorPageFilter(ErrorPageManager manager)
  {
    _errorPageManager = manager;
  }

  /**
   * Dummy init.
   */
  public void init(FilterConfig config)
  {
    _config = config;
    _app = (WebApp) config.getServletContext();
  }
  
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response,
                       FilterChain next)
    throws ServletException, IOException
  {
    try {
      next.doFilter(request, response);
    } catch (Throwable e) {
      _errorPageManager.sendServletError(e, request, response);
    }
  }

  /**
   * Dummy destroy
   */
  public void destroy()
  {
  }
}
