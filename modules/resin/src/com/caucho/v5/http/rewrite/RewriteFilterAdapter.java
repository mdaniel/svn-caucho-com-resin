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

package com.caucho.v5.http.rewrite;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.v5.http.dispatch.FilterChainFilter;
import com.caucho.v5.http.dispatch.FilterConfigImpl;
import com.caucho.v5.http.webapp.WebAppResinBase;

/**
 * Wraps a Java filter in a RewriteFilter
 */
public class RewriteFilterAdapter implements RewriteFilter
{
  private Filter _filter;

  public RewriteFilterAdapter(Filter filter)
    throws ServletException
  {
    WebAppResinBase webApp = WebAppResinBase.getCurrent();
    FilterConfigImpl filterConfig = new FilterConfigImpl();
    filterConfig.setServletContext(webApp);

    filter.init(filterConfig);

    _filter = filter;
  }
  
  public boolean isRequest()
  {
    return true;
  }
  
  public boolean isInclude()
  {
    return false;
  }
  
  public boolean isForward()
  {
    return false;
  }

  public FilterChain map(String uri,
                         String queryString,
                         FilterChain next)
  {
    return new FilterChainFilter(next, _filter);
  }

  public String toString()
  {
    return getClass().getSimpleName() + "[" + _filter + "]";
  }
}
