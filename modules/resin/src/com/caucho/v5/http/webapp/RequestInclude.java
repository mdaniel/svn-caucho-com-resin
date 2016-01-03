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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.dispatch.InvocationServlet;
import com.caucho.v5.http.protocol.Form;
import com.caucho.v5.http.protocol.RequestCauchoDispatch;
import com.caucho.v5.util.HashMapImpl;
import com.caucho.v5.util.IntMap;
import com.caucho.v5.util.ModulePrivate;
import com.caucho.v5.vfs.Encoding;

@ModulePrivate
public class RequestInclude extends RequestCauchoDispatch {
  private static final IntMap _includeAttributeMap = new IntMap();

  private static Enumeration<String> _emptyEnum;

  private static final int REQUEST_URI_CODE = 1;
  private static final int CONTEXT_PATH_CODE = 2;
  private static final int SERVLET_PATH_CODE = 3;
  private static final int PATH_INFO_CODE = 4;
  private static final int QUERY_STRING_CODE = 5;

  // the wrapped request
  private InvocationServlet _invocation;

  private ResponseInclude _response;

  private HashMapImpl<String,String[]> _filledForm;
  private ArrayList<String> _headerNames;
  
  public RequestInclude()
  {
    _response = new ResponseInclude(this);
  }
  
  public RequestInclude(HttpServletRequest request,
                        HttpServletResponse response,
                        InvocationServlet invocation)
  {
    super(request);

    _response = new ResponseInclude(this, response);
    setResponse(_response);

    _invocation = invocation;
  }

  @Override
  protected InvocationServlet getInvocation()
  {
    return _invocation;
  }

  @Override
  public ResponseInclude getResponse()
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
    return DispatcherType.INCLUDE;
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
  public String getContextPath()
  {
    return _invocation.getContextPath();
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
  
  @Override
  public String getPageQueryString()
  {
    return _invocation.getQueryString();
  }

  @Override
  public String getMethod()
  {
    String method = getRequest().getMethod();

    // server/10jk
    if ("POST".equalsIgnoreCase(method))
      return method;
    else
      return "GET";
  }
  
  @Override
  public WebApp getWebApp()
  {
    return _invocation.getWebApp();
  }

  @Override
  public boolean isSyntheticCacheHeader()
  {
    // server/137b
    
    return true;
  }
  
  @Override
  public void setHeader(String name, String value)
  {
    // server/13r4
  }

  @Override
  public String getHeader(String name)
  {
    if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name))
      return null;

    return super.getHeader(name);
  }

  @Override
  public Enumeration<String> getHeaders(String name)
  {
    if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name))
      return _emptyEnum;

    return super.getHeaders(name);
  }

  @Override
  public Enumeration<String> getHeaderNames()
  {
    // jsp/17eh jsp/17ek
    if (_headerNames == null) {
      _headerNames = new ArrayList<String>();

      Enumeration<String> names = super.getHeaderNames();
      while (names.hasMoreElements()) {
        String name = (String) names.nextElement();
        if ("If-Modified-Since".equals(name) || "If-None-Match".equals(name)) {
        } else {
          _headerNames.add(name);
        }
      }
    }

    return Collections.enumeration(_headerNames);
  }
  
  /*
  public ServletResponse getServletResponse()
  {
    CauchoRequest cRequest = (CauchoRequest) _request;

    return cRequest.getServletResponse();
  }
  */

  //
  // parameters
  //


  //
  // parameter/form
  //

  /**
   * Returns an enumeration of the form names.
   */
  @Override
  public Enumeration<String> getParameterNames()
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return Collections.enumeration(_filledForm.keySet());
  }

  /**
   * Returns a map of the form.
   */
  @Override
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
  @Override
  public String []getParameterValues(String name)
  {
    if (_filledForm == null)
      _filledForm = parseQuery();

    return _filledForm.get(name);
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

    Map<String, String[]> map = getParameterMapImpl();

    form.putAll(map);

    map = getRequest().getParameterMap();

    mergeParameters(map, form);
    
    return form;

    /*
    String javaEncoding = Encoding.getJavaName(getCharacterEncoding());

    HashMapImpl<String,String[]> form = new HashMapImpl<String,String[]>();
    Form formParser = Form.allocate();

    try {
      String queryString = _invocation.getQueryString();
      
      if (queryString != null) {
        formParser.parseQueryString(form, queryString, javaEncoding, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return form;
    */
  }

  @Override
  protected void parseGetQueryImpl(HashMapImpl<String,String[]> form)
  {
    // server/053n
    String javaEncoding = Encoding.getJavaName(getCharacterEncoding());

    Form formParser = Form.allocate();

    try {
      String queryString = _invocation.getQueryString();
      
      if (queryString != null) {
        formParser.parseQueryString(form, queryString, javaEncoding, false);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void parsePostQueryImpl(HashMapImpl<String,String[]> form)
  {
    if (isMultipartEnabled() && ! isDelegateMultipartEnabled()) {
      // server/1650
      super.parsePostQueryImpl(form);
    }
  }

  //
  // attributes
  //

  @Override
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

  @Override
  public Enumeration<String> getAttributeNames()
  {
    ArrayList<String> list = new ArrayList<String>();

    Enumeration<String> e = super.getAttributeNames();
    
    while (e.hasMoreElements()) {
      list.add(e.nextElement());
    }

    if (! list.contains(RequestDispatcher.INCLUDE_REQUEST_URI)) {
      list.add(RequestDispatcher.INCLUDE_REQUEST_URI);
      list.add(RequestDispatcher.INCLUDE_CONTEXT_PATH);
      list.add(RequestDispatcher.INCLUDE_SERVLET_PATH);
      list.add(RequestDispatcher.INCLUDE_PATH_INFO);
      list.add(RequestDispatcher.INCLUDE_QUERY_STRING);
    }

    return Collections.enumeration(list);
  }

  //
  // lifecycle
  //

  /**
   * Starts the request
   */
  void startRequest()
  {
    _response.startRequest();
  }

  @Override
  protected void finishRequest()
    throws IOException
  {
    super.finishRequest();
    
    _response.finishRequest();
  }

  static {
    _includeAttributeMap.put(RequestDispatcher.INCLUDE_REQUEST_URI,
                             REQUEST_URI_CODE);
    _includeAttributeMap.put(RequestDispatcher.INCLUDE_CONTEXT_PATH,
                             CONTEXT_PATH_CODE);
    _includeAttributeMap.put(RequestDispatcher.INCLUDE_SERVLET_PATH,
                             SERVLET_PATH_CODE);
    _includeAttributeMap.put(RequestDispatcher.INCLUDE_PATH_INFO,
                             PATH_INFO_CODE);
    _includeAttributeMap.put(RequestDispatcher.INCLUDE_QUERY_STRING,
                             QUERY_STRING_CODE);

    _emptyEnum = new Enumeration<String>() {
      public boolean hasMoreElements() {
        return false;
      }

      public String nextElement() {
        throw new NoSuchElementException();
      }
    };
  }
}
