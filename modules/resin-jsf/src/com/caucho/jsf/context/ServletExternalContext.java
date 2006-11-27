/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation.
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

package com.caucho.jsf.context;

import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.*;
import java.security.*;

import javax.faces.*;
import javax.faces.context.*;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.server.connection.*;

public class ServletExternalContext extends ExternalContext {
  private ServletContext _webApp;
  private HttpServletRequest _request;
  private HttpServletResponse _response;

  ServletExternalContext(ServletContext webApp,
			 HttpServletRequest request,
			 HttpServletResponse response)
  {
    _webApp = webApp;
    _request = request;
    _response = response;
  }
  
  public void dispatch(String path)
    throws IOException
  {
    try {
      _request.getRequestDispatcher(path).include(_request, _response);
    } catch (ServletException e) {
      throw new FacesException(e);
    }
  }

  public String encodeActionURL(String url)
  {
    throw new UnsupportedOperationException();
  }

  public String encodeNamespace(String name)
  {
    throw new UnsupportedOperationException();
  }

  public String encodeResourceURL(String url)
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,Object> getApplicationMap()
  {
    throw new UnsupportedOperationException();
  }

  public String getAuthType()
  {
    throw new UnsupportedOperationException();
  }

  public Object getContext()
  {
    return _webApp;
  }

  public String getInitParameter(String name)
  {
    throw new UnsupportedOperationException();
  }

  public Map getInitParameterMap()
  {
    throw new UnsupportedOperationException();
  }

  public String getRemoteUser()
  {
    throw new UnsupportedOperationException();
  }

  public Object getRequest()
  {
    return _request;
  }

  /**
   * @Since 1.2
   */
  public void setRequest(Object request)
  {
    _request = (HttpServletRequest) request;
  }

  /**
   * @Since 1.2
   */
  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  public String getRequestContextPath()
  {
    return _request.getContextPath();
  }

  public Map<String,Object> getRequestCookieMap()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,String> getRequestHeaderMap()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,String[]> getRequestHeaderValuesMap()
  {
    throw new UnsupportedOperationException();
  }

  public Locale getRequestLocale()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<Locale> getRequestLocales()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,Object> getRequestMap()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,String> getRequestParameterMap()
  {
    throw new UnsupportedOperationException();
  }

  public Iterator<String> getRequestParameterNames()
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,String[]> getRequestParameterValuesMap()
  {
    throw new UnsupportedOperationException();
  }

  public String getRequestPathInfo()
  {
    if (_request instanceof CauchoRequest) {
      return ((CauchoRequest) _request).getPagePathInfo();
    }
    else {
      // XXX: include
      
      return _request.getPathInfo();
    }
  }

  public String getRequestServletPath()
  {
    if (_request instanceof CauchoRequest) {
      return ((CauchoRequest) _request).getPageServletPath();
    }
    else {
      // XXX: include
      
      return _request.getServletPath();
    }
  }

  /**
   * @Since 1.2
   */
  public String getRequestCharacterEncoding()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getRequestContentType()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getResponseCharacterEncoding()
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public String getResponseContentType()
  {
    return _response.getContentType();
  }

  public URL getResource(String path)
    throws MalformedURLException
  {
    return _webApp.getResource(path);
  }

  public InputStream getResourceAsStream(String path)
  {
    return _webApp.getResourceAsStream(path);
  }

  public Set<String> getResourcePath(String path)
  {
    throw new UnsupportedOperationException();
  }

  public Object getResponse()
  {
    return _response;
  }

  /**
   * @Since 1.2
   */
  public void setResponse(Object response)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * @Since 1.2
   */
  public void setResponseCharacterEncoding(String encoding)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  public Object getSession(boolean create)
  {
    throw new UnsupportedOperationException();
  }

  public Map<String,Object> getSessionMap()
  {
    throw new UnsupportedOperationException();
  }

  public Principal getUserPrincipal()
  {
    throw new UnsupportedOperationException();
  }

  public boolean isUserInRole(String role)
  {
    throw new UnsupportedOperationException();
  }

  public void log(String message)
    {
    throw new UnsupportedOperationException();
  }

  public void log(String message, Throwable exn)
  {
    throw new UnsupportedOperationException();
  }

  public void redirect(String url)
    throws IOException
  {
    throw new UnsupportedOperationException();
  }
}

