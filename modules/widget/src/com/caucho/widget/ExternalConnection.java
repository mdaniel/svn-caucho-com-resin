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

package com.caucho.widget;

import com.caucho.vfs.XmlWriter;

import javax.servlet.http.Cookie;
import java.io.*;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public interface ExternalConnection
  extends ExternalContext
{
   public void setApplicationAttribute(String name, Object value);

   public <T> T getApplicationAttribute(String name);

   public Enumeration<String> getApplicationAttributeNames();

   public void removeApplicationAttribute(String name);

   public Map<String, String[]> getParameterMap();

   public String getPathInfo();

   public Cookie[] getCookies();

   public String getRequestedSessionId();

   public boolean isRequestedSessionIdValid();

   public boolean isRequestedSessionIdFromCookie();

   public boolean isRequestedSessionIdFromURL();

   public String getAuthType();

   public String getRemoteUser();

   public boolean isUserInRole(String role);

   public Principal getUserPrincipal();

   public String getProtocol();

   public String getScheme();

   public String getServerName();

   public int getServerPort();

   public String getRemoteAddr();

   public String getRemoteHost();

   public int getRemotePort();

   public String getLocalAddr();

   public String getLocalName();

   public int getLocalPort();

   public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException;

   public String getRequestCharacterEncoding();

   public InputStream getInputStream()
    throws IOException;

   public int getRequestContentLength();

   public String getRequestContentType();

   public BufferedReader getReader()
    throws IOException, IllegalStateException;

   public Locale getRequestLocale();

   public Enumeration<Locale> getRequestLocales();

   public boolean isSecure();

   public <T> T getRequestAttribute(String name);

   public void setRequestAttribute(String name, Object value);

   public Enumeration<String> getRequestAttributeNames();

   public void removeRequestAttribute(String name);

   public <T> T getSessionAttribute(String name);

   public void setSessionAttribute(String name, Object value);

   public Enumeration<String> getSessionAttributeNames();

   public void removeSessionAttribute(String name);

   public void addCookie(Cookie cookie);

   public XmlWriter getWriter()
    throws IOException;

   public OutputStream getOutputStream()
    throws IOException;

   public void setResponseContentType(String type);

   public String getResponseContentType();

   public String getResponseCharacterEncoding();

   public void setResponseCharacterEncoding(String charset);

   public void setResponseLocale(Locale locale);

   public Locale getResponseLocale();

   public String createURL(String pathInfo, Map<String, String[]> parameterMap);

   public String createURL(String pathInfo, Map<String, String[]> parameterMap, boolean isSecure);

  public String createSubmitURL(String submitPrefix, String pathInfo, Map<String, String[]> parameterMap);

  public String createSubmitURL(String submitPrefix, String pathInfo, Map<String, String[]> parameterMap, boolean isSecure);

   public String createResourceURL(String path);
}
