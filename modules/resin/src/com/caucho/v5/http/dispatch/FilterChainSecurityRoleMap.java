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

package com.caucho.v5.http.dispatch;

import java.io.IOException;
import java.util.HashMap;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletRequestWrapper;
import javax.servlet.ServletResponse;

import com.caucho.v5.http.protocol.RequestCaucho;

/**
 * Represents the next filter in a filter chain.  The final filter will
 * be the servlet itself.
 */
public class FilterChainSecurityRoleMap implements FilterChain {
  // Next filter chain
  private FilterChain _next;

  // app
  private HashMap<String,String> _roleMap;

  /**
   * Creates a new FilterChainFilter.
   *
   * @param next the next filterChain
   * @param filter the user's filter
   */
  public FilterChainSecurityRoleMap(FilterChain next,
                                    HashMap<String,String> roleMap)
  {
    _next = next;
    if (_next == null)
      throw new NullPointerException();
    
    _roleMap = roleMap;
  }
  
  /**
   * Invokes the next filter in the chain or the final servlet at
   * the end of the chain.
   *
   * @param request the servlet request
   * @param response the servlet response
   * @since Servlet 2.3
   */
  public void doFilter(ServletRequest request,
                       ServletResponse response)
    throws ServletException, IOException
  {
    // HashMap<String,String> oldMap = null;
    ServletRequest ptr = request;
    
    while (ptr instanceof ServletRequestWrapper) {
      ServletRequestWrapper wrapper;
      wrapper = (ServletRequestWrapper) ptr;

      if (ptr instanceof RequestCaucho) {
        break;
      }
      else if (wrapper.getRequest() != null)
        ptr = wrapper.getRequest();
      else
        break;
    }

    if (ptr instanceof RequestCaucho) {

      // oldMap = req.setRoleMap(_roleMap);
    }
    
    try {
      _next.doFilter(request, response);
    } finally {
      /*
      if (req != null)
        req.setRoleMap(oldMap);
      */
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _roleMap + "," + _next + "]";
  }
}
