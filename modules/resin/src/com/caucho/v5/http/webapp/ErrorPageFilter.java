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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.webapp;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Represents the final servlet in a filter chain.
 */
public class ErrorPageFilter implements Filter {
  
  public static String MESSAGE = "javax.servlet.error.message";

  public static String SHUTDOWN = "com.caucho.shutdown";
  
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
  @Override
  public void init(FilterConfig config)
  {
  }
  
  /**
   * Invokes the final servlet at the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  @Override
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
  @Override
  public void destroy()
  {
  }
}
