/*
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001-2004 Caucho Technology, Inc.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Caucho Technology (http://www.caucho.com/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "Hessian", "Resin", and "Caucho" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    info@caucho.com.
 *
 * 5. Products derived from this software may not be called "Resin"
 *    nor may "Resin" appear in their names without prior written
 *    permission of Caucho Technology.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL CAUCHO TECHNOLOGY OR ITS CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT
 * OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
 * IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * @author Sam
 */


package com.caucho.portal.generic.context;

import javax.portlet.*;

import java.util.*;

class PortletRequestImpl 
  extends PortalRequestImpl
  implements PortletRequest
{
  public PortletRequestImpl(ConnectionContext context)
  {
    super(context);
  }

  public WindowState getWindowState()
  {
    return _context.getWindowState();
  }
  
  public PortletMode getPortletMode()
  {
    return _context.getPortletMode();
  }

  public Map getParameterMap()
  {
    return _context.getRenderParameterMap();
  }

  public String getParameter(String name)
  {
    return _context.getRenderParameter(name);
  }

  public Enumeration getParameterNames()
  {
    return _context.getRenderParameterNames();
  }

  public String[] getParameterValues(String name)
  {
    return _context.getRenderParameterValues(name);
  }

  public boolean isWindowStateAllowed(WindowState state)
  {
    return _context.isWindowStateAllowed(state);
  }

  public boolean isPortletModeAllowed(PortletMode portletMode)
  {
    return _context.isPortletModeAllowed(portletMode);
  }

  public Enumeration getResponseContentTypes()
  {
    return Collections.enumeration(_context.getResponseContentTypesSet());
  }

  public String getResponseContentType()
  {
    return _context.getResponseContentType();
  }

  public Enumeration getLocales()
  {
    return Collections.enumeration(_context.getResponseLocalesSet());
  }

  public Locale getLocale()
  {
    return _context.getResponseLocale();
  }

  public PortalContext getPortalContext()
  {
    return _context.getPortalContext();
  }

  public PortletPreferences getPreferences()
  {
    return _context.getPreferences();
  }

  public String getContextPath()
  {
    return _context.getContextPath();
  }

  public String getServerName()
  {
    return _context.getServerName();
  }

  public int getServerPort()
  {
    return _context.getServerPort();
  }

  public String getScheme()
  {
    return _context.getScheme();
  }

  public String getAuthType()
  {
    return _context.getAuthType();
  }

  public boolean isSecure()
  {
    return _context.isSecure();
  }

  public String getRequestedSessionId()
  {
    return _context.getRequestedSessionId();
  }

  public boolean isRequestedSessionIdValid()
  {
    return _context.isRequestedSessionIdValid();
  }

  public String getRemoteUser()
  {
    return _context.getRemoteUser();
  }

  public java.security.Principal getUserPrincipal()
  {
    return _context.getUserPrincipal();
  }

  public boolean isUserInRole(String role)
  {
    return _context.isUserInRole(role);
  }

  public String getProperty(String name)
  {
    return _context.getProperty(name);
  }

  public Enumeration getProperties(String name)
  {
    return _context.getProperties(name);
  }

  public Enumeration getPropertyNames()
  {
    return _context.getPropertyNames();
  }

  public Enumeration getAttributeNames()
  {
    return _context.getAttributeNames();
  }

  public Object getAttribute(String name)
  {
    return _context.getAttribute(name);
  }

  public void setAttribute(String name, Object o)
  {
    _context.setAttribute(name, o);
  }

  public void removeAttribute(String name)
  {
    _context.removeAttribute(name);
  }

  public PortletSession getPortletSession()
  {
    return _context.getPortletSession();
  }

  public PortletSession getPortletSession(boolean create)
  {
    return _context.getPortletSession(create);
  }
}
