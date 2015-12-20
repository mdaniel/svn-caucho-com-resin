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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.protocol.RequestCaucho;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.util.L10N;

public class RequestForwardNamed extends RequestForward
{
  private static final IntMap _forwardAttributeMap = new IntMap();
  private static final L10N L = new L10N(RequestForwardNamed.class);

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;
  
  public RequestForwardNamed()
  {
    super();
  }
  
  public RequestForwardNamed(HttpServletRequest request,
                        HttpServletResponse response,
                        InvocationServlet invocation)
  {
    super(request, response, invocation);
  }

  //
  // HttpServletRequest
  //

  @Override
  public String getRequestURI()
  {
    return getRequest().getRequestURI();
  }

  @Override
  public String getContextPath()
  {
    return getRequest().getContextPath();
  }
  
  @Override
  public String getServletPath()
  {
    return getRequest().getContextPath();
  }
  
  @Override
  public String getPathInfo()
  {
    return getRequest().getPathInfo();
  }
  
  @Override
  public String getQueryString()
  {
    return getRequest().getQueryString();
  }

  //
  // CauchoRequest
  //
  
  private RequestCaucho getCauchoRequest()
  {
    return (RequestCaucho) getRequest();
  }
  
  @Override
  public String getPageURI()
  {
    return getCauchoRequest().getPageURI();
  }
  
  @Override
  public String getPageContextPath()
  {
    return getCauchoRequest().getPageContextPath();
  }
  
  @Override
  public String getPageServletPath()
  {
    return getCauchoRequest().getPageServletPath();
  }
  
  @Override
  public String getPagePathInfo()
  {
    return getCauchoRequest().getPathInfo();
  }
  
  public String getPageQueryString()
  {
    return getQueryString();
  }
}
