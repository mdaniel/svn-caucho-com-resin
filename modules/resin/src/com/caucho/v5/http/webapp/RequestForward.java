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

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.MultipartConfigElement;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.protocol.RequestCauchoDispatch;
import com.caucho.v5.http.protocol.RequestHttpBase;
import com.caucho.v5.util.CharSegment;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.util.L10N;

public class RequestForward extends RequestCauchoDispatch
{
  private static final IntMap _forwardAttributeMap = new IntMap();
  private static final L10N L = new L10N(RequestForward.class);

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;
  
  // the wrapped request
  private InvocationServlet _invocation;

  private ResponseForward _response;

  private HashMapImpl<String,String[]> _fwdFilledForm;
  
  public RequestForward()
  {
    _response = new ResponseForward(this);
  }
  
  public RequestForward(HttpServletRequest request,
                        HttpServletResponse response,
                        InvocationServlet invocation)
  {
    super(request);

    _response = new ResponseForward(this, response);
    setResponse(_response);

    _invocation = invocation;
  }

  @Override
  protected InvocationServlet getInvocation()
  {
    return _invocation;
  }

  /**
   * Starts the request
   */
  void startRequest()
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
  public ResponseForward getResponse()
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
  
  @Override
  public String getPageURI()
  {
    return _invocation.getURI();
  }
  
  @Override
  public String getPageContextPath()
  {
    return _invocation.getContextPath();
  }
  
  @Override
  public String getPageServletPath()
  {
    return _invocation.getServletPath();
  }
  
  @Override
  public String getPagePathInfo()
  {
    return _invocation.getPathInfo();
  }
  
  public String getPageQueryString()
  {
    return getQueryString();
  }
  
  public WebAppResinBase getWebApp()
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

  //
  // attributes
  //

  public Object getAttribute(String name)
  {
    switch (_forwardAttributeMap.get(name)) {
    case REQUEST_URI_CODE:
      return unwrapRequest().getRequestURI();
      
    case CONTEXT_PATH_CODE:
      return unwrapRequest().getContextPath();
      
    case SERVLET_PATH_CODE:
      return unwrapRequest().getServletPath();
      
    case PATH_INFO_CODE:
      return unwrapRequest().getPathInfo();
      
    case QUERY_STRING_CODE:
      return unwrapRequest().getQueryString();
      
    default:
      return super.getAttribute(name);
    }
  }

  public HttpServletRequest unwrapRequest()
  {
    HttpServletRequest request = this.getRequest();

    while (request instanceof RequestForward) {
      request = ((RequestForward) request).getRequest();
    }

    return request;
  }

  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  @Override
  public Enumeration<String> getParameterNames()
  {
    if (_fwdFilledForm == null) {
      _fwdFilledForm = parseQuery();
    }

    return Collections.enumeration(_fwdFilledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  @Override
  public Map<String,String[]> getParameterMap()
  {
    if (_fwdFilledForm == null) {
      _fwdFilledForm = parseQuery();
    }

    return Collections.unmodifiableMap(_fwdFilledForm);
  }

  /**
   * Returns the form's values for the given name.
   *
   * @param name key in the form
   * @return value matching the key
   */
  @Override
  public String []getParameterValues(String name)
  {
    if (_fwdFilledForm == null) {
      _fwdFilledForm = parseQuery();
    }

    return _fwdFilledForm.get(name);
  }

  /**
   * Returns the form primary value for the given name.
   */
  @Override
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
    HashMapImpl<String,String[]> form = new HashMapImpl<String,String[]>();

    Map <String,String[]> map = getParameterMapImpl();

    //server/162r
    form.putAll(map);

    map = getRequest().getParameterMap();

    mergeParameters(map, form);

    /*
    String javaEncoding = Encoding.getJavaName(getCharacterEncoding());

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
    */

    return form;
  }

  @Override
  protected void parsePostQueryImpl(HashMapImpl<String,String[]> form)
  {
    // server/1637, server/162r
    RequestHttpBase request = getAbstractHttpRequest();

    if (request == null)
      return;

    CharSegment contentType = request.getContentTypeBuffer();
    
    if (contentType == null || ! "POST".equalsIgnoreCase(getMethod())) {
      return;
    }

    if (getWebApp().isMultipartFormEnabled()) {
      // server/1637 - original request would have handle it
      return;
    }

    InvocationServlet invocation = getInvocation();

    MultipartConfigElement multipartConfig = invocation.getMultipartConfig();


    if ((getWebApp().isMultipartFormEnabled() || multipartConfig != null)
        && contentType.startsWith("multipart/form-data")) {
      // server/162r
      super.parsePostQueryImpl(form);
    }

  }

  static {
    _forwardAttributeMap.put(RequestDispatcher.FORWARD_REQUEST_URI,
                             REQUEST_URI_CODE);
    _forwardAttributeMap.put(RequestDispatcher.FORWARD_CONTEXT_PATH,
                             CONTEXT_PATH_CODE);
    _forwardAttributeMap.put(RequestDispatcher.FORWARD_SERVLET_PATH,
                             SERVLET_PATH_CODE);
    _forwardAttributeMap.put(RequestDispatcher.FORWARD_PATH_INFO,
                             PATH_INFO_CODE);
    _forwardAttributeMap.put(RequestDispatcher.FORWARD_QUERY_STRING,
                             QUERY_STRING_CODE);
  }
}
