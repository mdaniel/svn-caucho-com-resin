/*
 * Copyright (c) 1998-2010 Caucho Technology -- all rights reserved
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

package com.caucho.server.webapp;

import java.security.Principal;

import javax.xml.ws.EndpointReference;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Element;

import com.caucho.util.L10N;

/**
 * Proxy for the WebServiceContext.
 */
public class WebServiceContextProxy implements WebServiceContext {
  private static final L10N L = new L10N(WebServiceContextProxy.class);
  
  private static final ThreadLocal<WebServiceContext> _localContext
    = new ThreadLocal<WebServiceContext>();
  
  public static WebServiceContext setContext(WebServiceContext context)
  {
    WebServiceContext oldContext = _localContext.get();
    
    _localContext.set(context);
    
    return oldContext;
  }

  @Override
  public MessageContext getMessageContext()
  {
    return getContext().getMessageContext();
  }

  @Override
  public Principal getUserPrincipal()
  {
    return getContext().getUserPrincipal();
  }

  @Override
  public boolean isUserInRole(String role)
  {
    return getContext().isUserInRole(role);
  }

  @Override
  public EndpointReference getEndpointReference(Element... arg0)
  {
    return getContext().getEndpointReference(arg0);
  }

  @Override
  public <T extends EndpointReference> T getEndpointReference(Class<T> arg0,
                                                              Element... arg1)
  {
    return getContext().getEndpointReference(arg0, arg1);
  }
  
  private WebServiceContext getContext()
  {
    WebServiceContext context = _localContext.get();
    
    if (context != null)
      return context;
    else
      throw new IllegalStateException(L.l("WebServiceContext is not available here"));
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _localContext.get() + "]";
  }
}
