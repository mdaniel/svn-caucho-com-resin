/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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
 * @author Nam Nguyen
 */

package com.caucho.quercus.servlet.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Map;

public interface QuercusHttpServletRequest
{
  public String getMethod();
  public String getHeader(String name);
  public Enumeration getHeaderNames();
  public String getParameter(String name);
  public String []getParameterValues(String name);
  public Map<String,String[]> getParameterMap();
  public String getContentType();
  public String getCharacterEncoding();

  public String getRequestURI();
  public String getQueryString();
  public QuercusCookie []getCookies();

  public String getContextPath();
  public String getServletPath();
  public String getPathInfo();
  public String getRealPath(String path);

  public InputStream getInputStream()
    throws IOException;

  public QuercusHttpSession getSession(boolean isCreate);

  public String getLocalAddr();
  public String getServerName();
  public int getServerPort();
  public String getRemoteHost();
  public String getRemoteAddr();
  public int getRemotePort();
  public String getRemoteUser();

  public boolean isSecure();
  public String getProtocol();

  public Object getAttribute(String name);
  public String getIncludeRequestUri();
  public String getForwardRequestUri();
  public String getIncludeContextPath();
  public String getIncludeServletPath();
  public String getIncludePathInfo();
  public String getIncludeQueryString();

  public QuercusRequestDispatcher getRequestDispatcher(String url);

  public <T> T toRequest(Class<T> cls);
}
