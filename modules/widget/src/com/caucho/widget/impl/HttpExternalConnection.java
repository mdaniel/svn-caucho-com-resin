/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */

package com.caucho.widget.impl;

import com.caucho.vfs.XmlWriter;
import com.caucho.widget.ExternalConnection;

import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public class HttpExternalConnection
  implements ExternalConnection
{
  private ServletContext _servletContext;
  private HttpServletRequest _request;
  private HttpServletResponse _response;
  private XmlWriter _widgetWriter;
  private OutputStream _outputStream;

  public void start(ServletContext servletContext,
                    HttpServletRequest request,
                    HttpServletResponse response)
  {
    _servletContext = servletContext;
    _request = request;
    _response = response;
  }

  public void setApplicationAttribute(String name, Object value)
  {
    _servletContext.setAttribute(name, value);
  }

  public <T> T getApplicationAttribute(String name)
  {
    return (T) _servletContext.getAttribute(name);
  }

  public Enumeration<String> getApplicationAttributeNames()
  {
    return (Enumeration<String>) (Enumeration<?>) _servletContext.getAttributeNames();
  }

  public void removeApplicationAttribute(String name)
  {
    _servletContext.removeAttribute(name);
  }

  public Map<String, String[]> getParameterMap()
  {
    return (Map<String, String[]>) (Map<?,?>) _request.getParameterMap();
  }

  public String getPathInfo()
  {
    return _request.getPathInfo();
  }

  public Cookie[] getCookies()
  {
    return _request.getCookies();
  }

  public String getRequestedSessionId()
  {
    return _request.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return _request.isRequestedSessionIdValid();
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    return _request.isRequestedSessionIdFromCookie();
  }

  public boolean isRequestedSessionIdFromURL()
  {
    return _request.isRequestedSessionIdFromURL();
  }

  public String getAuthType()
  {
    return _request.getAuthType();
  }

  public String getRemoteUser()
  {
    return _request.getRemoteUser();
  }

  public boolean isUserInRole(String role)
  {
    return _request.isUserInRole(role);
  }

  public Principal getUserPrincipal()
  {
    return _request.getUserPrincipal();
  }

  public String getProtocol()
  {
    return _request.getProtocol();
  }

  public String getScheme()
  {
    return _request.getScheme();
  }

  public String getServerName()
  {
    return _request.getServerName();
  }

  public int getServerPort()
  {
    return _request.getServerPort();
  }

  public String getRemoteAddr()
  {
    return _request.getRemoteAddr();
  }

  public String getRemoteHost()
  {
    return _request.getRemoteHost();
  }

  public int getRemotePort()
  {
    return _request.getRemotePort();
  }

  public String getLocalAddr()
  {
    return _request.getLocalAddr();
  }

  public String getLocalName()
  {
    return _request.getLocalName();
  }

  public int getLocalPort()
  {
    return _request.getLocalPort();
  }

  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    _request.setCharacterEncoding(encoding);
  }

  public String getRequestCharacterEncoding()
  {
    return _request.getCharacterEncoding();
  }

  public int getRequestContentLength()
  {
    return _request.getContentLength();
  }

  public String getRequestContentType()
  {
    return _request.getContentType();
  }

  public InputStream getInputStream()
    throws IOException
  {
    return _request.getInputStream();
  }

  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return _request.getReader();
  }

  public Locale getRequestLocale()
  {
    return _request.getLocale();
  }

  public Enumeration<Locale> getRequestLocales()
  {
    return (Enumeration<Locale>) (Enumeration<?>) _request.getLocales();
  }

  public boolean isSecure()
  {
    return _request.isSecure();
  }

  public <T> T getRequestAttribute(String name)
  {
    return (T) _request.getAttribute(name);
  }

  public void setRequestAttribute(String name, Object value)
  {
    _request.setAttribute(name, value);
  }

  public Enumeration<String> getRequestAttributeNames()
  {
    return (Enumeration<String>) (Enumeration<?>) _request.getAttributeNames();
  }

  public void removeRequestAttribute(String name)
  {
    _request.removeAttribute(name);
  }

  public <T> T getSessionAttribute(String name)
  {
    return (T) _request.getSession().getAttribute(name);
  }

  public void setSessionAttribute(String name, Object value)
  {
    _request.getSession().setAttribute(name, value);
  }

  public Enumeration<String> getSessionAttributeNames()
  {
    return (Enumeration<String>) (Enumeration<?>) _request.getSession().getAttributeNames();
  }

  public void removeSessionAttribute(String name)
  {
    _request.getSession().removeAttribute(name);
  }

  public void addCookie(Cookie cookie)
  {
    _response.addCookie(cookie);
  }

  public void setResponseContentType(String type)
  {
    _response.setContentType(type);
  }

  public String getResponseContentType()
  {
    return _response.getContentType();
  }

  public String getResponseCharacterEncoding()
  {
    return _response.getCharacterEncoding();
  }

  public void setResponseCharacterEncoding(String charset)
  {
    _response.setCharacterEncoding(charset);
  }

  public XmlWriter getWriter()
    throws IOException
  {
    if (_widgetWriter == null)
      _widgetWriter = new XmlWriter(_response.getWriter());

    return _widgetWriter;
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    if (_outputStream == null)
      _outputStream = _response.getOutputStream();

    return _outputStream;
  }

  public void setResponseLocale(Locale locale)
  {
    _response.setLocale(locale);
  }

  public Locale getResponseLocale()
  {
    return _response.getLocale();
  }

  public String createURL(String pathInfo, Map<String, String[]> parameterMap)
  {
    throw new UnimplementedException();
  }

  public String createURL(String pathInfo,
                          Map<String, String[]> parameterMap,
                          boolean isSecure)
  {
    throw new UnimplementedException();
  }

  public String createSubmitURL(String submitPrefix,
                                String pathInfo,
                                Map<String, String[]> parameterMap)
  {
    throw new UnimplementedException();
  }

  public String createSubmitURL(String submitPrefix,
                                String pathInfo,
                                Map<String, String[]> parameterMap,
                                boolean isSecure)
  {
    throw new UnimplementedException();
  }

  public String createResourceURL(String path)
  {
    throw new UnimplementedException();
  }

  public void finish()
    throws IOException
  {
    try {
      if (_widgetWriter != null)
        _widgetWriter.flush();
    }
    finally {

      try {
        if (_outputStream != null)
          _outputStream.flush();
      }
      finally {
        _widgetWriter = null;
        _outputStream = null;
        _servletContext = null;
        _request = null;
        _response = null;
      }
    }
  }
}
