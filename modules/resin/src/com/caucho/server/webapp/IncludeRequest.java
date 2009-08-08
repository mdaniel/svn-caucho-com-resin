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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.server.webapp;

import com.caucho.server.connection.*;
import com.caucho.server.dispatch.Invocation;
import com.caucho.server.webapp.WebApp;
import com.caucho.util.IntMap;
import com.caucho.vfs.*;

import java.io.*;
import java.util.*;
import java.security.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class IncludeRequest extends CauchoRequestWrapper {
  private static final IntMap _includeAttributeMap = new IntMap();

  private static final String REQUEST_URI
    = "javax.servlet.include.request_uri";
  private static final String CONTEXT_PATH
    = "javax.servlet.include.context_path";
  private static final String SERVLET_PATH
    = "javax.servlet.include.servlet_path";
  private static final String PATH_INFO
    = "javax.servlet.include.path_info";
  private static final String QUERY_STRING
    = "javax.servlet.include.query_string";

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;
  
  // the wrapped request
  private Invocation _invocation;

  private IncludeResponse _response;
  
  public IncludeRequest()
  {
    _response = new IncludeResponse();
  }
  
  public IncludeRequest(HttpServletRequest request,
                        HttpServletResponse response,
                        Invocation invocation)
  {
    super(request);

    _response = new IncludeResponse(response);

    _invocation = invocation;
  }

  /**
   * Starts the request
   */
  void startRequest()
  {
    _response.startRequest();
  }
  
  void finishRequest()
    throws IOException
  {
    _response.finishRequest();
  }

  IncludeResponse getResponse()
  {
    return _response;
  }

  public ServletContext getServletContext()
  {
    return _invocation.getWebApp();
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.INCLUDE;
  }

  //
  // CauchoRequest
  //
  
  public String getPageURI()
  {
    return _invocation.getURI();
  }
  
  public String getPageContextPath()
  {
    return _invocation.getContextPath();
  }
  
  public String getPageServletPath()
  {
    return _invocation.getServletPath();
  }
  
  public String getPagePathInfo()
  {
    return _invocation.getPathInfo();
  }
  
  public String getPageQueryString()
  {
    return _invocation.getQueryString();
  }
  
  public WebApp getWebApp()
  {
    return _invocation.getWebApp();
  }

  /*
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  */

  //
  // attributes
  //

  public Object getAttribute(String name)
  {
    switch (_includeAttributeMap.get(name)) {
    case REQUEST_URI_CODE:
      return _invocation.getURI();
      
    case CONTEXT_PATH_CODE:
      return _invocation.getContextPath();
      
    case SERVLET_PATH_CODE:
      return _invocation.getServletPath();
      
    case PATH_INFO_CODE:
      return _invocation.getPathInfo();
      
    case QUERY_STRING_CODE:
      return _invocation.getQueryString();
      
    default:
      return super.getAttribute(name);
    }
  }

  static {
    _includeAttributeMap.put(REQUEST_URI, REQUEST_URI_CODE);
    _includeAttributeMap.put(CONTEXT_PATH, CONTEXT_PATH_CODE);
    _includeAttributeMap.put(SERVLET_PATH, SERVLET_PATH_CODE);
    _includeAttributeMap.put(PATH_INFO, PATH_INFO_CODE);
    _includeAttributeMap.put(QUERY_STRING, QUERY_STRING_CODE);
  }
}
