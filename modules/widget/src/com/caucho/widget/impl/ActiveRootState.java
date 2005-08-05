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

import com.caucho.widget.*;
import com.caucho.util.L10N;
import com.caucho.vfs.XmlWriter;

import javax.servlet.http.Cookie;
import java.io.*;
import java.security.Principal;
import java.util.*;

public class ActiveRootState
  extends ActiveState
{
  private static L10N L = new L10N(ActiveRootState.class);

  private ExternalConnection _externalConnection;
  private URLStateWalker _URLStateWalker;

  enum UrlType { NORMAL, SUBMIT, PARAMETER, TARGET };

  public void setExternalConnection(ExternalConnection externalConnection)
  {
    _externalConnection = externalConnection;
  }

  protected ExternalConnection getExternalConnection()
  {
    return _externalConnection;
  }

  public void init()
  {
    if (_externalConnection == null)
      throw new IllegalStateException(L.l("`{0}' is required", "external-connection"));

    super.init();
  }

  public void destroy()
  {
    _externalConnection = null;

    try {
      if (_URLStateWalker != null && _URLStateWalker.isInit())
        _URLStateWalker.destroy();
    }
    finally {
      super.destroy();
    }
  }

  protected String getId()
  {
    return null;
  }

  public String getLocalParameterMapPrefix()
  {
    return null;
  }

  public String getNamedParameterMapPrefix()
  {
    return null;
  }

  public String getNamespaceParameterMapPrefix()
  {
    return null;
  }

  protected Widget getWidget()
  {
    return null;
  }

  protected ActiveState getParentState()
  {
    return null;
  }

  protected ActiveRootState getWidgetRootState()
  {
    return this;
  }

  public String[] getParameterValues()
  {
    throw new UnsupportedOperationException();
  }

  public WidgetURL createURL(ActiveState activeState, UrlType urlType)
    throws WidgetException
  {
    if (urlType == UrlType.TARGET)
      throw new UnimplementedException("the idea is that a target url is for an iframe, it goes directly to the target");

    if (_URLStateWalker == null)
      _URLStateWalker = new URLStateWalker();

    _URLStateWalker.destroy();

    _URLStateWalker.setExternalConnection(getExternalConnection());
    _URLStateWalker.setActiveStateWalker(getActiveStateWalker());
    _URLStateWalker.setWidget(activeState.getWidget());

    if (urlType == UrlType.PARAMETER)
      _URLStateWalker.setSubmitPrefix(activeState.getNamedParameterMapPrefix());
    else if (urlType == UrlType.SUBMIT)
      _URLStateWalker.setSubmitPrefix(activeState.getNamespaceParameterMapPrefix());

    _URLStateWalker.init();

    return _URLStateWalker.walk();
  }

  public Map<String, String[]> getNamespaceParameterMap()
  {
    return getExternalConnection().getParameterMap();
  }

  public Map<String, String[]> getNamedParameterMap()
  {
    return getExternalConnection().getParameterMap();
  }

  public String getPathInfo()
  {
    return getExternalConnection().getPathInfo();
  }

  public Cookie[] getCookies()
  {
    return getExternalConnection().getCookies();
  }

  public String getRequestedSessionId()
  {
    return getExternalConnection().getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return getExternalConnection().isRequestedSessionIdValid();
  }

  public boolean isRequestedSessionIdFromCookie()
  {
    return getExternalConnection().isRequestedSessionIdFromCookie();
  }

  public boolean isRequestedSessionIdFromURL()
  {
    return getExternalConnection().isRequestedSessionIdFromURL();
  }

  public String getAuthType()
  {
    return getExternalConnection().getAuthType();
  }

  public String getRemoteUser()
  {
    return getExternalConnection().getRemoteUser();
  }

  public boolean isUserInRole(String role)
  {
    return getExternalConnection().isUserInRole(role);
  }

  public Principal getUserPrincipal()
  {
    return getExternalConnection().getUserPrincipal();
  }

  public String getProtocol()
  {
    return getExternalConnection().getProtocol();
  }

  public String getScheme()
  {
    return getExternalConnection().getScheme();
  }

  public String getServerName()
  {
    return getExternalConnection().getServerName();
  }

  public int getServerPort()
  {
    return getExternalConnection().getServerPort();
  }

  public String getRemoteAddr()
  {
    return getExternalConnection().getRemoteAddr();
  }

  public String getRemoteHost()
  {
    return getExternalConnection().getRemoteHost();
  }

  public int getRemotePort()
  {
    return getExternalConnection().getRemotePort();
  }

  public String getLocalAddr()
  {
    return getExternalConnection().getLocalAddr();
  }

  public String getLocalName()
  {
    return getExternalConnection().getLocalName();
  }

  public int getLocalPort()
  {
    return getExternalConnection().getLocalPort();
  }

  public void setRequestCharacterEncoding(String encoding)
    throws UnsupportedEncodingException
  {
    getExternalConnection().setRequestCharacterEncoding(encoding);
  }

  public String getRequestCharacterEncoding()
  {
    return getExternalConnection().getRequestCharacterEncoding();
  }

  public InputStream getInputStream()
    throws IOException
  {
    return getExternalConnection().getInputStream();
  }

  public int getRequestContentLength()
  {
    return getExternalConnection().getRequestContentLength();
  }

  public String getRequestContentType()
  {
    return getExternalConnection().getRequestContentType();
  }

  public BufferedReader getReader()
    throws IOException, IllegalStateException
  {
    return getExternalConnection().getReader();
  }

  public Locale getRequestLocale()
  {
    return getExternalConnection().getRequestLocale();
  }

  public Enumeration<Locale> getRequestLocales()
  {
    return getExternalConnection().getRequestLocales();
  }

  public boolean isSecure()
  {
    return getExternalConnection().isSecure();
  }

  public <T> T getRequestAttribute(String name)
  {
    return (T) getExternalConnection().getRequestAttribute(name);
  }

  public void setRequestAttribute(String name, Object value)
  {
    getExternalConnection().setRequestAttribute(name, value);
  }

  public Enumeration<String> getRequestAttributeNames()
  {
    return getExternalConnection().getRequestAttributeNames();
  }

  public void removeRequestAttribute(String name)
  {
    getExternalConnection().removeRequestAttribute(name);
  }

  public <T> T getSessionAttribute(String name)
  {
    return (T) getExternalConnection().getSessionAttribute(name);
  }

  public void setSessionAttribute(String name, Object value)
  {
    getExternalConnection().setSessionAttribute(name, value);
  }

  public Enumeration<String> getSessionAttributeNames()
  {
    return getExternalConnection().getSessionAttributeNames();
  }

  public void removeSessionAttribute(String name)
  {
    getExternalConnection().removeSessionAttribute(name);
  }

  public void addCookie(Cookie cookie)
  {
    getExternalConnection().addCookie(cookie);
  }

  public XmlWriter getWriter()
    throws IOException
  {

    return getExternalConnection().getWriter();
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    return getExternalConnection().getOutputStream();
  }

  public void setResponseContentType(String type)
  {
    getExternalConnection().setResponseContentType(type);
  }

  public String getResponseContentType()
  {
    return getExternalConnection().getResponseContentType();
  }

  public String getResponseCharacterEncoding()
  {
    return getExternalConnection().getResponseCharacterEncoding();
  }

  public void setResponseCharacterEncoding(String charset)
  {
    getExternalConnection().setResponseCharacterEncoding(charset);
  }

  public void setResponseLocale(Locale locale)
  {
    getExternalConnection().setResponseLocale(locale);
  }

  public Locale getResponseLocale()
  {

    return getExternalConnection().getResponseLocale();
  }

  public String createResourceURL(String path)
  {
    return getExternalConnection().createResourceURL(path);
  }

  public void finishInvocation()
  {
  }

  public void finishRequest()
  {
  }

  public void finishResponse()
  {
  }

  public String toString()
  {
    return "ActiveRootState[]";
  }
}
