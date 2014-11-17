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

package com.caucho.server.webapp;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.CauchoDispatchRequest;
import com.caucho.server.http.CauchoRequest;
import com.caucho.util.HashMapImpl;
import com.caucho.util.L10N;

public class ForwardNamedRequest extends CauchoDispatchRequest {
  private static final L10N L = new L10N(ForwardNamedRequest.class);

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;
  
  // the wrapped request
  private Invocation _invocation;

  private ForwardNamedResponse _response;

  private HashMapImpl<String,String[]> _fwdFilledForm;
  
  public ForwardNamedRequest()
  {
    _response = new ForwardNamedResponse(this);
  }
  
  public ForwardNamedRequest(HttpServletRequest request,
                        HttpServletResponse response,
                        Invocation invocation)
  {
    super(request);

    _response = new ForwardNamedResponse(this, response);
    setResponse(_response);

    _invocation = invocation;
  }

  @Override
  protected Invocation getInvocation()
  {
    return _invocation;
  }

  /**
   * Starts the request
   */
  protected void startRequest()
  {
    _response.startRequest();
  }
  
  void finishRequest(boolean isValid)
    throws IOException
  {
    finishRequest();

    if (isValid)
      _response.finishRequest();
  }

  @Override
  public ForwardNamedResponse getResponse()
  {
    return _response;
  }

  @Override
  public ServletContext getServletContext()
  {
    return _invocation.getWebApp();
  }

  @Override
  public DispatcherType getDispatcherType()
  {
    return DispatcherType.FORWARD;
  }

  //
  // HttpServletRequest
  //
  
  @Override
  public HttpSession getSession()
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getSession();
    else
      return null;
  }
  
  @Override
  public HttpSession getSession(boolean isNew)
  {
    CauchoRequest cRequest = getCauchoRequest();

    if (cRequest != null)
      return cRequest.getSession(isNew);
    else
      return null;
  }
 
  @Override
  public String getParameter(String name)
  {
    return getRequest().getParameter(name);
  }
  
  @Override
  public Map<String,String[]> getParameterMap()
  {
    return getRequest().getParameterMap();
  }
  
  @Override
  public String []getParameterValues(String name)
  {
    return getRequest().getParameterValues(name);
  }
  
  @Override
  public Enumeration<String> getParameterNames()
  {
    return getRequest().getParameterNames();
  }

  //
  // CauchoRequest
  //
  
  public WebApp getWebApp()
  {
    return _invocation.getWebApp();
  }

  @Override
  public boolean isAsyncSupported()
  {
    return _invocation.isAsyncSupported() && getRequest().isAsyncSupported();
  }

  @Override
  public AsyncContext startAsync()
    throws IllegalStateException
  {
    if (! isAsyncSupported())
      throw new IllegalStateException(L.l("The servlet '{0}' at '{1}' does not support async because the servlet or one of the filters does not support asynchronous mode.",
                                          getServletName(), getServletPath()));
    return super.startAsync();
  }

  public String getServletName()
  {
    if (_invocation != null) {
      return _invocation.getServletName();
    }
    else
      return null;
  }

  /*
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  */
}
