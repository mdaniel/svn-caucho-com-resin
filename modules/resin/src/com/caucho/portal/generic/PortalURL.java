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


package com.caucho.portal.generic;

import com.caucho.portal.generic.context.ConnectionContext;

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.PortletSecurityException;
import javax.portlet.PortletURL;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import java.util.Map;

public class PortalURL
  implements PortletURL
{
  private ConnectionContext _context;
  private InvocationURL _invocationURL;

  private String _url;

  public PortalURL( ConnectionContext context,
                    InvocationURL invocationURL)
  {
    _context = context;
    _invocationURL = invocationURL;
  }

  protected void checkWindowState(String namespace, WindowState windowState)
    throws WindowStateException
  {
    if (!_context.isWindowStateAllowed(namespace, windowState))
      throw new WindowStateException(
          "WindowState `" + windowState 
          + "' not allowed for namespace `" + namespace + "'",
          windowState);
  }

  public void setWindowState(WindowState windowState)
    throws WindowStateException
  {
    _url = null;
    checkWindowState(_invocationURL.getNamespace(), windowState);
    _invocationURL.setWindowState(windowState);
  }

  public void setWindowState(String namespace, WindowState windowState)
    throws WindowStateException
  {
    _url = null;
    checkWindowState(namespace, windowState);
    _invocationURL.setWindowState(namespace, windowState);
  }

  protected void checkPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException
  {
    if (!_context.isPortletModeAllowed(namespace, portletMode))
      throw new PortletModeException(
          "PortletMode `" + portletMode 
          + "' not allowed for namespace `" + namespace + "'",
          portletMode);
  }

  public void setPortletMode(PortletMode portletMode)
    throws PortletModeException
  {
    _url = null;
    checkPortletMode(_invocationURL.getNamespace(), portletMode);
    _invocationURL.setPortletMode(portletMode);
  }

  public void setPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException
  {
    _url = null;
    checkPortletMode(namespace, portletMode);
    _invocationURL.setPortletMode(namespace, portletMode);
  }

  public void setParameters(Map parameters)
  {
    _url = null;
    _invocationURL.setParameters(parameters);
  }

  public void setParameter(String name, String value)
  {
    _url = null;
    _invocationURL.setParameter(name, value);
  }

  public void setParameter(String name, String[] values)
  {
    _url = null;
    _invocationURL.setParameter(name, values);
  }

  public void setParameter(String namespace, String name, String value)
  {
    _url = null;
    _invocationURL.setParameter(namespace, name, value);
  }

  public void setParameter(String namespace, String name, String[] values)
  {
    _url = null;
    _invocationURL.setParameter(namespace, name, values);
  }

  public void setParameters(String namespace, Map<String, String[]> parameters)
  {
    _url = null;
    _invocationURL.setParameters(namespace, parameters);
  }

  public void setSecure(boolean secure) 
    throws PortletSecurityException
  {
    _url = null;

    _invocationURL.setSecure(secure);

    if (secure == true)
      getURL();
  }

  /**
   * True if setSecure() was called for this url, even if it was
   * called with setSecure(false)
   */
  protected boolean isSecureSpecified()
  {
    return _invocationURL.isSecureSpecified();
  }

  protected boolean isSecure()
  {
    return _invocationURL.isSecure();
  }

  protected String getURL() 
    throws PortletSecurityException
  {
    if (_url != null)
      return _url;

    String url = _invocationURL.getURL();
 
 
    if (isSecureSpecified())
      _url = _context.resolveURL(url, isSecure());
    else
      _url = _context.resolveURL(url);
 
    return _url;
  }
 
  public String toString()
  {
    try {
      return getURL();
    }
    catch (PortletSecurityException ex) {
      throw new RuntimeException(ex);
    }
  }
}
