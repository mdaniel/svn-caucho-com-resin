/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.http.dispatch;

//import com.caucho.soa.servlet.ProtocolServlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.UnavailableException;

import com.caucho.v5.http.dispatch.ServletBuilder;

/**
 * Represents the final servlet in a filter chain.
 */
public class WebServiceFilterChain implements FilterChain {
  private static final Logger log
    = Logger.getLogger(WebServiceFilterChain.class.getName());
  
  public static final String SERVLET_NAME
    = "javax.servlet.error.servlet_name";
  
  // servlet config
  private ServletBuilderResin _config;
  
  // protocol skeleton
  // private ProtocolServlet _skeleton;
  private Servlet _skeleton;

  /**
   * Create the filter chain servlet.
   *
   * @param servlet the underlying servlet
   */
  public WebServiceFilterChain(ServletBuilder config)
  {
    Objects.requireNonNull(config);
    
    _config = (ServletBuilderResin) config;
  }

  /**
   * Returns the servlet name.
   */
  public String getServletName()
  {
    return _config.getServletName();
  }

  /**
   * Returns the role map.
   */
  public HashMap<String,String> getRoleMap()
  {
    return _config.getRoleMap();
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
    if (_skeleton == null) {
      _skeleton = _config.createProtocolServlet();
    }
    
    try {
      _skeleton.service(request, response);
    } catch (UnavailableException e) {
      _skeleton = null;
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
    } catch (RuntimeException e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw e;
    } catch (Exception e) {
      request.setAttribute(SERVLET_NAME, _config.getServletName());
      throw new ServletException(e);
    }
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _skeleton + "]";
  }
}
