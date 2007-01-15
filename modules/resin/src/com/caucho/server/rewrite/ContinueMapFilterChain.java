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

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

public class ContinueMapFilterChain
  implements FilterChain
{
  private final RewriteContext  _rewriteContext;
  private final String  _uri;
  private final Invocation  _invocation;
  private final FilterChain  _next;
  private final int _start;

  public ContinueMapFilterChain(RewriteContext rewriteContext,
                                String uri,
                                Invocation invocation,
                                FilterChain next,
                                int start)
  {
    _rewriteContext = rewriteContext;
    _uri = uri;
    _invocation = invocation;
    _next = next;
    _start = start;
  }

  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    FilterChain next
      = _rewriteContext.map(_uri, _invocation, _next, _start);

    next.doFilter(request, response);
  }
}
