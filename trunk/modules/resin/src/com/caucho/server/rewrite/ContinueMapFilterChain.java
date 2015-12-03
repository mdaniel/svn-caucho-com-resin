/*
 * Copyright (c) 1998-2012 Caucho Technology -- all rights reserved
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

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public class ContinueMapFilterChain
  implements FilterChain
{
  private final String  _uri;
  private final String  _queryString;
  private final FilterChain _accept;
  private final FilterChainMapper _nextFilterChainMapper;

  public ContinueMapFilterChain(String uri,
                                String queryString,
                                FilterChain accept,
                                FilterChainMapper nextFilterChainMapper)
  {
    _uri = uri;
    _queryString = queryString;
    _accept = accept;
    _nextFilterChainMapper = nextFilterChainMapper;
  }

  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    FilterChain next = _nextFilterChainMapper.map(_uri,
                                                  _queryString,
                                                  _accept);

    next.doFilter(request, response);
  }
}
