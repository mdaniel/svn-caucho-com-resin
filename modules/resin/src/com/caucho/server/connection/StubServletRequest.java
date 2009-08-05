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

package com.caucho.server.connection;

import com.caucho.util.NullEnumeration;
import com.caucho.vfs.ReadStream;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import java.io.BufferedReader;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.*;

/**
 * Used when there isn't any actual request object, e.g. for calling
 * run-at servlets.
 */
public class StubServletRequest extends AbstractHttpRequest {
  private static final Logger log
    = Logger.getLogger(StubServletRequest.class.getName());
  
  private HashMap _attributes;

  public StubServletRequest()
  {
    super(null, null);

    try {
      startRequest(null);
    } catch (Throwable e) {
      log.log(Level.FINER, e.toString(), e);
    }
  }

  @Override
  protected AbstractHttpResponse createResponse()
  {
    return null;
  }
  
  @Override
  public Object getAttribute(String name)
  {
    if (_attributes != null)
      return _attributes.get(name);
    else
      return null;
  }
  
  public Enumeration<String> getAttributeNames()
  {
    if (_attributes != null)
      return Collections.enumeration(_attributes.keySet());
    else
      return (Enumeration) NullEnumeration.create();
  }
  
  public void setAttribute(String name, Object value)
  {
    if (_attributes == null)
      _attributes = new HashMap();

    _attributes.put(name, value);
  }
  
  public void removeAttribute(String name)
  {
    if (_attributes != null)
      _attributes.remove(name);
  }
    
  public boolean initStream(ReadStream rawStream, ReadStream realStream)
  {
    return false;
  }
  
  public String getCharacterEncoding() { return "UTF-8"; }
  public void setCharacterEncoding(String encoding) { }
  public int getContentLength() { return -1; }
  public String getContentType() { return "application/octet-stream"; }

  public String getParameter(String name) { return null; }
  public Enumeration<String> getParameterNames()
  {
    return (Enumeration) NullEnumeration.create();
  }
  public String []getParameterValues(String name) { return null; }
  public Map<String,String[]> getParameterMap() { return null; }
  public String getProtocol() { return "none"; }
  
  public String getRemoteAddr() { return "127.0.0.1"; }
  public String getRemoteHost() { return "127.0.0.1"; }
  public int getRemotePort() { return 6666; }
  public String getScheme() { return "cron"; }
  public String getServerName() { return "127.0.0.1"; }
  public int getServerPort() { return 0; }
    
  public String getRealPath(String path) { return null; }
  public Locale getLocale() { return null; }
  public Enumeration<Locale> getLocales()
  { return (Enumeration) NullEnumeration.create(); }
  public boolean isSecure() { return true; }
  public RequestDispatcher getRequestDispatcher(String uri) { return null; }

  public String getMethod() { return "GET"; }
  public String getServletPath() { return null; }
  public String getContextPath() { return null; }
  public String getPathInfo() { return null; }
  public String getPathTranslated() { return null; }
  public String getRequestURI () { return null; }
  public StringBuffer getRequestURL ()
  {
    return new StringBuffer("http://localhost");
  }
  public int getUriLength() { return 0; }
  public byte []getUriBuffer() { return null; }
  
  public String getQueryString() { return null; }

  public String getHeader(String header) { return null; }
  public int getIntHeader(String header) { return 0; }
  public long getDateHeader(String header) { return 0; }
  
  public Enumeration getHeaders(String header)
  {
    return (Enumeration) NullEnumeration.create();
  }
  
  public Enumeration<String> getHeaderNames()
  {
    return (Enumeration) NullEnumeration.create();
  }

  public String getAuthType() { return null; }
  public String getRemoteUser() { return null; }
  public java.security.Principal getUserPrincipal() { return null; }

  public boolean isUserInRole(String str) { return false; }
}
  
