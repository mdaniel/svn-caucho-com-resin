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

package com.caucho.server.dispatch;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Does an internal forward of the request.
 */
public class ForwardErrorFilterChain extends ForwardFilterChain
{
  private static final Logger log
    = Logger.getLogger(ForwardErrorFilterChain.class.getName());
  
  private int _code;

  /**
   * Create the forward filter chain servlet.
   *
   * @param disp the request dispatcher to forward to.
   */
  public ForwardErrorFilterChain(RequestDispatcher disp, int code)
  {
    super(disp);
    
    _code = code;
  }

  /**
   * Forwards to the dispatch
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  @Override
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletResponse res = (HttpServletResponse) response;
    
    res.setStatus(_code);
    
    super.doFilter(request, response);
  }
}
