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

import com.caucho.portal.generic.PortalResponse;
import com.caucho.portal.generic.PortalURL;

import javax.portlet.PortletMode;
import javax.portlet.PortletModeException;
import javax.portlet.WindowState;
import javax.portlet.WindowStateException;
import java.util.Map;


/**
 */
class PortalResponseImpl
  implements PortalResponse
{
  ConnectionContext _context;

  public PortalResponseImpl(ConnectionContext context)
  {
    _context = context;
  }

  public void setPortletMode(String namespace, PortletMode portletMode)
    throws PortletModeException
  {
    _context.setPortletMode(namespace, portletMode);
  }

  public void setWindowState(String namespace, WindowState windowState)
    throws WindowStateException
  {
    _context.setWindowState(namespace, windowState);
  }

  public void setParameters(String namespace, Map<String,String[]> srcMap)
  {
    _context.setRenderParameters(namespace, srcMap);
  }

  public void setParameter(String namespace, String name, String value)
  {
    _context.setRenderParameter(namespace, name, value);
  }

  public void setParameter(String namespace, String name, String[] values)
  {
    _context.setRenderParameter(namespace, name, values);
  }

  public PortalURL createRenderURL(String namespace, boolean keepParameters)
  {
    return _context.createRenderURL(namespace, keepParameters);
  }

  public PortalURL createActionURL(String namespace, boolean keepParameters)
  {
    return _context.createActionURL(namespace, keepParameters);
  }
}
