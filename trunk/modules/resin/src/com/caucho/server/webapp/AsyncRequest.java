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

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;

import com.caucho.server.dispatch.Invocation;
import com.caucho.server.http.CauchoRequestWrapper;
import com.caucho.server.http.Form;
import com.caucho.server.http.HttpServletRequestImpl;
import com.caucho.server.http.HttpServletResponseImpl;
import com.caucho.util.HashMapImpl;
import com.caucho.util.IntMap;
import com.caucho.util.L10N;
import com.caucho.vfs.Encoding;

public class AsyncRequest extends CauchoRequestWrapper {
  private static final IntMap _asyncAttributeMap = new IntMap();
  private static final L10N L = new L10N(AsyncRequest.class);

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;
  
  // the wrapped request
  private Invocation _invocation;

  private HashMapImpl<String,String[]> _filledForm;
  
  public AsyncRequest(HttpServletRequestImpl request,
                      HttpServletResponseImpl response,
                      Invocation invocation)
  {
    super(request);

    _invocation = invocation;
  }

  protected Invocation getInvocation()
  {
    return _invocation;
  }

  /**
   * Starts the request
   */
  void startRequest()
  {
    // _response.startRequest();
  }

  /*
  void finishRequest(boolean isValid)
    throws IOException
  {
    finishRequest();

    if (isValid)
      _response.finishRequest();
  }
  */

  /*
  ForwardResponse getResponse()
  {
    return _response;
  }
  */

  public ServletContext getServletContext()
  {
    return _invocation.getWebApp();
  }

  public DispatcherType getDispatcherType()
  {
    return DispatcherType.ASYNC;
  }

  //
  // HttpServletRequest
  //

  @Override
  public String getRequestURI()
  {
    return _invocation.getURI();
  }

  @Override
  public String getContextPath()
  {
    return _invocation.getContextPath();
  }
  
  @Override
  public String getServletPath()
  {
    return _invocation.getServletPath();
  }
  
  @Override
  public String getPathInfo()
  {
    return _invocation.getPathInfo();
  }
  
  @Override
  public String getQueryString()
  {
    return calculateQueryString();
  }

  protected String calculateQueryString()
  {
    // server/10j2
    // server/1ks7 vs server/1233

    String queryString = _invocation.getQueryString();

    if (queryString != null)
      return queryString;

    return getRequest().getQueryString();
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
    return getQueryString();
  }
  
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

  //
  // attributes
  //

  public Object getAttribute(String name)
  {
    switch (_asyncAttributeMap.get(name)) {
    case REQUEST_URI_CODE:
      return getRequest().getRequestURI();
      
    case CONTEXT_PATH_CODE:
      return getRequest().getContextPath();
      
    case SERVLET_PATH_CODE:
      return getRequest().getServletPath();
      
    case PATH_INFO_CODE:
      return getRequest().getPathInfo();
      
    case QUERY_STRING_CODE:
      return getRequest().getQueryString();
      
    default:
      return super.getAttribute(name);
    }
  }

  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  public Map<String,String[]> getParameterMap()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.unmodifiableMap(_filledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return (String []) _filledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  public String getParameter(String name)
  {
    String []values = getParameterValues(name);

    if (values != null && values.length > 0)
      return values[0];
    else
      return null;
  }

  private HashMapImpl<String,String[]> parseQuery()
  {
    String javaEncoding = Encoding.getJavaName(getCharacterEncoding());

    HashMapImpl<String,String[]> form = new HashMapImpl<String,String[]>();

    form.putAll(getRequest().getParameterMap());
    
    Form formParser = Form.allocate();

    try {
      String queryString = _invocation.getQueryString();
      String oldQueryString = getRequest().getQueryString();
      
      if (queryString != null && ! queryString.equals(oldQueryString)) {
        formParser.parseQueryString(form, queryString, javaEncoding, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return form;
  }

  static {
    _asyncAttributeMap.put(AsyncContext.ASYNC_REQUEST_URI,
                           REQUEST_URI_CODE);
    _asyncAttributeMap.put(AsyncContext.ASYNC_CONTEXT_PATH,
                           CONTEXT_PATH_CODE);
    _asyncAttributeMap.put(AsyncContext.ASYNC_SERVLET_PATH,
                           SERVLET_PATH_CODE);
    _asyncAttributeMap.put(AsyncContext.ASYNC_PATH_INFO,
                           PATH_INFO_CODE);
    _asyncAttributeMap.put(AsyncContext.ASYNC_QUERY_STRING,
                           QUERY_STRING_CODE);
  }
}
