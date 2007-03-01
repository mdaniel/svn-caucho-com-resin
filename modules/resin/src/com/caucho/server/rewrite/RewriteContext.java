/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */

package com.caucho.server.rewrite;

import com.caucho.server.dispatch.Invocation;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public class RewriteContext
  extends ELContext
{
  private RewriteDispatch _rewriteDispatch;

  public RewriteContext()
  {
  }

  public RewriteContext(RewriteDispatch rewriteDispatch)
  {
    _rewriteDispatch = rewriteDispatch;
  }

  public RewriteContext(HttpServletRequest request)
  {
    _rewriteDispatch = null;
  }

  public ELResolver getELResolver()
  {
    return null;
  }

  public FunctionMapper getFunctionMapper()
  {
    return null;
  }

  public VariableMapper getVariableMapper()
  {
    return null;
  }

  FilterChain map(String uri,
                  Invocation invocation,
                  FilterChain next,
                  int start)
    throws ServletException
  {
    return _rewriteDispatch.map(this, uri, invocation, next, start);
  }
}
