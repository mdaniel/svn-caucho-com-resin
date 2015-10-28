/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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

package com.caucho.v5.http.webapp;

import javax.servlet.DispatcherType;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.v5.http.dispatch.Invocation;
import com.caucho.v5.http.rewrite.RewriteDispatch;
import com.caucho.v5.http.webapp.WebAppDispatch;

/**
 * Resin's webApp implementation.
 */
public class WebAppDispatchResin extends WebAppDispatch
{
  private WebAppBuilderResin _builder;
  
  // dispatch mapping
  private RewriteDispatch _requestRewriteDispatch;
  private RewriteDispatch _includeRewriteDispatch;
  private RewriteDispatch _forwardRewriteDispatch;
  
  /**
   * Creates the webApp with its environment loader.
   */
  WebAppDispatchResin(WebAppBuilderResin builder)
  {
    super(builder);
    
    _builder = builder;
  }
  
  @Override
  public void init()
    throws ServletException
  {
    super.init();
    
    _requestRewriteDispatch = _builder.getRequestRewriteDispatch();
    _includeRewriteDispatch = _builder.getIncludeRewriteDispatch();
    _forwardRewriteDispatch = _builder.getForwardRewriteDispatch();
  }
  
  
  @Override
  protected FilterChain applyRewrite(DispatcherType type,
                                     Invocation invocation,
                                     FilterChain chain)
  {
    switch (type) {
    case REQUEST:
      if (_requestRewriteDispatch != null) {
        chain = _requestRewriteDispatch.map(DispatcherType.REQUEST,
                                            invocation.getContextURI(),
                                            invocation.getQueryString(),
                                            chain);
      }
      break;
      
    case FORWARD:
      if (_forwardRewriteDispatch != null) {
        chain = _forwardRewriteDispatch.map(DispatcherType.FORWARD,
                                            invocation.getContextURI(),
                                            invocation.getQueryString(),
                                            chain);
      }
      break;
      
    case INCLUDE:
      if (_includeRewriteDispatch != null) {
        chain = _includeRewriteDispatch.map(DispatcherType.INCLUDE,
                                            invocation.getContextURI(),
                                            invocation.getQueryString(),
                                            chain);
      }
      break;
      
    default:
      break;
    }
    
    return chain;
  }

  public boolean isFacesServletConfigured()
  {
    return getServletManager().isFacesServletConfigured();
  }
}
