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
 * @author Scott Ferguson
 */

package com.caucho.rewrite;

import java.util.ArrayList;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import com.caucho.config.Configurable;


/**
 * Matches if all of the child predicates fail. The predicate may be used
 * for security and rewrite actions.
 *
 * <pre>
 * &lt;resin:Forbidden url-pattern="/admin/*"
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:NotAnd>
 *     &lt;resin:IfRole name="admin"/&gt;
 *     &lt;resin:IfNetwork value="192.168/16"/&gt;
 *   &lt;/resin:NotAnd>
 * &lt;/resin:Allow>
 * </pre>
 */
public class NotAnd implements RequestPredicate {
  private ArrayList<RequestPredicate> _predicateList
    = new ArrayList<RequestPredicate>();

  private RequestPredicate []_predicates;

  /**
   * Add a child predicate.  One child must fail for NotAnd to pass.
   *
   * @param predicate the child predicate
   */
  @Configurable
  public void add(RequestPredicate predicate)
  {
    _predicateList.add(predicate);
  }

  @PostConstruct
  public void init()
  {
    _predicates = new RequestPredicate[_predicateList.size()];
    _predicateList.toArray(_predicates);
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    for (RequestPredicate predicate : _predicates) {
      if (! predicate.isMatch(request))
        return true;
    }

    return false;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + _predicateList;
  }
}
