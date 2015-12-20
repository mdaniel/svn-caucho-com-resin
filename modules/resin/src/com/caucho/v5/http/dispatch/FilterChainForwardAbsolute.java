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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import com.caucho.v5.http.webapp.*;

/**
 * Does an internal forward of the request based on the host's root, not
 * relative
 */
public class FilterChainForwardAbsolute implements FilterChain {
  // servlet
  private String _url;
  private WebApp _webApp;

  /**
   * Create the forward filter chain servlet.
   *
   * @param url the request dispatcher to forward to.
   */
  public FilterChainForwardAbsolute(String url, WebApp webApp)
  {
    _url = url;
    _webApp = webApp;
  }

  /**
   * Forwards to the dispatch
   *
   * @param request the servlet request
   * @param response the servlet response
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    ServletContext root = _webApp.getContext("/");

    RequestDispatcher disp = root.getRequestDispatcher(_url);

    disp.forward(request, response);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _url + "]";
  }
}
