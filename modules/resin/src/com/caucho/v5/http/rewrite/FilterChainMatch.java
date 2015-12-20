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
 * @author Sam
 */

package com.caucho.v5.http.rewrite;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.caucho.v5.http.protocol.ResponseServlet;

public class FilterChainMatch
  implements FilterChain
{
  private static final Logger log
    = Logger.getLogger(FilterChainMatch.class.getName());

  private final RequestPredicate []_predicates;
  private final FilterChain _passChain;
  private final FilterChain _failChain;
  
  private final boolean _isNoCacheUnlessVary;

  public FilterChainMatch(RequestPredicate []predicates,
                          FilterChain passChain,
                          FilterChain failChain)
  {
    _predicates = predicates;
    _passChain = passChain;
    _failChain = failChain;
    
    boolean isNoCacheUnlessVary = false;
    
    for (RequestPredicate pred : predicates) {
      if (! (pred instanceof PredicateCacheable)) {
        isNoCacheUnlessVary = true;
      }
    }
    
    _isNoCacheUnlessVary = isNoCacheUnlessVary;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;

    if (response instanceof ResponseServlet) {
      ResponseServlet resServlet
        = (ResponseServlet) response;

      // server/1k67
      if (_isNoCacheUnlessVary)
        resServlet.setNoCacheUnlessVary(true);
    }

    for (RequestPredicate predicate : _predicates) {
      if (predicate instanceof RequestPredicateVary) {
        RequestPredicateVary varyPredicate = (RequestPredicateVary) predicate;
        
        varyPredicate.addVaryHeader(res);
      }
      
      if (! predicate.isMatch(req)) {
        if (log.isLoggable(Level.FINEST))
          log.finest(this + " not match");

        _failChain.doFilter(request, response);
        return;
      }
    }
    
    if (log.isLoggable(Level.FINER))
      log.finest(this + " match");

    _passChain.doFilter(request, response);
  }
  
  public String toString()
  {
    return (getClass().getSimpleName()
            + "[test=" + Arrays.asList(_predicates)
            + ",pass=" + _passChain + ",fail=" + _failChain + "]"); 
  }
}
