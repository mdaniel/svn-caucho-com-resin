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

import javax.servlet.http.Cookie;
import java.util.Enumeration;
import java.util.Map;
import java.util.Locale;
import java.security.Principal;

public interface WidgetInvocation
  extends VarContext
{
  public final static String BASIC_AUTH = "BASIC";
  public final static String CLIENT_CERT_AUTH = "CLIENT_CERT";
  public final static String DIGEST_AUTH = "DIGEST";
  public final static String FORM_AUTH = "FORM";

  public String getParameter();

  /**
   */
  public String[] getParameterValues();

  /**
   */
  public String getParameter(String name);

  /**
   */
  public String[] getParameterValues(String name);

  /**
   */
  public Enumeration<String> getParameterNames();

  /**
   */
  public Map<String, String[]> getParameterMap();

  /**
   * Find a widget within the same namespace.
   */
  public <T> T find(String name);

  /**
   * @see javax.servlet.http.HttpServletRequest#getPathInfo()
   */
  public String getPathInfo();

  /**
   * @see javax.servlet.http.HttpServletRequest#getCookies()
   */
  public Cookie[] getCookies();

  /**
   * @see javax.servlet.http.HttpServletRequest#getRequestedSessionId()
   */
  public String getRequestedSessionId();

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdValid()
   */
  public boolean isRequestedSessionIdValid();


  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromCookie()
   */
  public boolean isRequestedSessionIdFromCookie();

  /**
   * @see javax.servlet.http.HttpServletRequest#isRequestedSessionIdFromURL()
   */
  public boolean isRequestedSessionIdFromURL();

  /**
   * @see javax.servlet.http.HttpServletRequest#isSecure()
   */
  public boolean isSecure();

  /**
   * @see javax.servlet.http.HttpServletRequest#getAuthType()
   */
  public String getAuthType();

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteUser()
   */
  public String getRemoteUser();

  /**
   * @see javax.servlet.http.HttpServletRequest#isUserInRole(String role)
   */
  public boolean isUserInRole(String role);

  /**
   * @see javax.servlet.http.HttpServletRequest#getUserPrincipal()
   */
  public Principal getUserPrincipal();

  /**
   * @see javax.servlet.http.HttpServletRequest#getProtocol()
   */
  public String getProtocol();

  /**
   * @see javax.servlet.http.HttpServletRequest#getScheme()
   */
  public String getScheme();

  /**
   * @see javax.servlet.http.HttpServletRequest#getServerName()
   */
  public String getServerName();

  /**
   * @see javax.servlet.http.HttpServletRequest#getServerPort()
   */
  public int getServerPort();

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteAddr()
   */
  public String getRemoteAddr();

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemoteHost()
   */
  public String getRemoteHost();

  /**
   * @see javax.servlet.http.HttpServletRequest#getRemotePort()
   */
  public int getRemotePort();

  /**
   * @see javax.servlet.http.HttpServletRequest#getLocalAddr()
   */
  public String getLocalAddr();

  /**
   * @see javax.servlet.http.HttpServletRequest#getLocalName()
   */
  public String getLocalName();

  /**
   * @see javax.servlet.http.HttpServletRequest#getLocalPort()
   */
  public int getLocalPort();

  /**
   * @see javax.servlet.http.HttpServletRequest#getLocale()
   */
  public Locale getLocale();

  /**
   * @see javax.servlet.http.HttpServletRequest#getLocales()
   */
  public Enumeration<Locale> getLocales();

  /**
   * @see WidgetInit#setApplicationAttribute(String,Object)
   */
  public void setApplicationAttribute(String name, Object value);

  /**
   * @see WidgetInit#getApplicationAttribute(String)
   */
  public <T> T getApplicationAttribute(String name);

  /**
   * @see WidgetInit#getApplicationAttributeNames()
   */
  public Enumeration<String> getApplicationAttributeNames();

  /**
   * @see WidgetInit#removeApplicationAttribute(String name)
   */
  public void removeApplicationAttribute(String name);

  public WidgetInvocationChain getInvocationChain();

  public void finish();
}
