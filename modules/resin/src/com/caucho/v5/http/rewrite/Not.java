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

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.util.ConcurrentArrayList;
import com.caucho.v5.util.L10N;

import io.baratine.config.Configurable;

/**
 * True if the child predicate is false.
 *
 * <p>Complex tests can be built using &lt;resin:Not>,
 * &lt;resin:And> and &lt;resin:Or> on top of simpler primary
 * predicates.
 *
 * <pre>
 * &lt;web-app xmlns:resin="urn:java:com.caucho.resin">
 *
 *   &lt;resin:Forbidden regexp="^/local/">
 *     &lt;resin:Not>
 *       &lt;resin:IfAddress value="192.168.1.10"/&gt;
 *     &lt;/resin:Not>
 *   &lt;/resin:Forbidden>
 *
 * &lt;/web-app>
 * </pre>
 *
 * <p>Predicates may be used for security and rewrite actions.
 */
@Configurable
public class Not implements RequestPredicate {
  private static final L10N L = new L10N(Not.class);
  
  private ConcurrentArrayList<RequestPredicate> _predicate
    = new ConcurrentArrayList<>(RequestPredicate.class);

  public Not()
  {
  }

  public Not(RequestPredicate predicate)
  {
    add(predicate);
  }

  /**
   * Add a child predicate.  The child must fail for Not to pass.
   *
   * @param predicate the child predicate
   */
  @Configurable
  public void add(RequestPredicate predicate)
  {
    _predicate.add(predicate);
    /*
    if (_predicate != null)
      throw new ConfigException(L.l("&lt;resin:Not> requires a single value"));
      
    _predicate = predicate;
    */
  }

  @PostConstruct
  public void init()
  {
    /*
    if (_predicate.size() == 0) {
      throw new ConfigException(L.l("&lt;resin:Not> requires a value"));
    }
    */
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    for (RequestPredicate predicate : _predicate) {
      if (predicate.isMatch(request)) {
        return false;
      }
    }
    
    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _predicate + "]";
  }
}
