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

package com.caucho.rewrite;

import java.util.ArrayList;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.caucho.v5.http.rewrite.FilterChainMatch;
import com.caucho.v5.http.rewrite.RequestPredicate;
import com.caucho.v5.http.rewrite.RewriteFilter;
import com.caucho.v5.http.rewrite.RewriteFilterAdapter;

/**
 * Implements general extended behavior for rewrite filters like 
 * resin:SetHeader. RewriteFilters act like standard servlet filters,
 * but are configured using Resin's rewrite syntax.
 * 
 * <ul>
 * <li>regexp</li>
 * <li>predicates</li>
 * <li>sub-rewrite-filters</li>
 * <li>sub-servlet-filters</li>
 * </ul>
 * 
 * <p>Abstract filters have a URL regexp pattern which only
 * applies the filter to matching URLs. If the regexp is missing, all 
 * URLs will match.
 * 
 * <p>Any rewrite/security predicate like IfUser or IfAddress can be used
 * to restrict requests. Only requests that match all the predicates will
 * be filtered.
 * 
 * <p>The AbstractRewriteFilter can have child RewriteFilters and child
 * servlet Filters. The children will only be added to the filter chain if
 * the predicates match.
 * 
 * <pre><code>
 * &lt;resin:SetHeader regexp="\.pdf$"
 *                     name="Cache-Control" value="max-age=15">
 *   &lt;resin:IfUserInRole role="admin"/>
 * &lt;resin:SetHeader>
 * </code></pre>
 */
abstract public class RewriteFilterBase implements RewriteFilter
{
  private Pattern _regexp;

  private ArrayList<RequestPredicate> _predicateList
    = new ArrayList<RequestPredicate>();

  private RequestPredicate []_predicates = new RequestPredicate[0];

  private ArrayList<RewriteFilter> _filterList
    = new ArrayList<RewriteFilter>();

  private RewriteFilter []_filters = new RewriteFilter[0];

  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }

  @Override
  public boolean isRequest()
  {
    return true;
  }

  @Override
  public boolean isInclude()
  {
    return false;
  }

  @Override
  public boolean isForward()
  {
    return false;
  }

  public void add(RequestPredicate predicate)
  {
    _predicateList.add(predicate);
    _predicates = new RequestPredicate[_predicateList.size()];
    _predicateList.toArray(_predicates);
  }
  
  public void add(RewriteFilter filter)
  {
    _filterList.add(filter);
    _filters = new RewriteFilter[_filterList.size()];
    _filterList.toArray(_filters);
  }
  
  public void add(Filter filter)
    throws ServletException
  {
    add(new RewriteFilterAdapter(filter));
  }
  
  @Override
  public FilterChain map(String uri,
                         String queryString,
                         FilterChain next)
    throws ServletException
  {
    if (_regexp == null || (_regexp.matcher(uri)).find()) {
      FilterChain chain = createFilterChain(uri, queryString, next);

      for (int i = _filters.length - 1; i >= 0; i--) {
        chain = _filters[i].map(uri, queryString, chain);
      }

      if (_predicates.length > 0)
        chain = new FilterChainMatch(_predicates, chain, next);

      return chain;
    }
    else
      return next;
  }

  protected FilterChain createFilterChain(String uri,
                                          String queryString,
                                          FilterChain next)
  {
    return next;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[regexp=" + _regexp + "]";
  }
}
