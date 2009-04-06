/*
 * Copyright (c) 1998-2008 Caucho Technology -- all rights reserved
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

package com.caucho.security;

import com.caucho.config.ConfigException;
import com.caucho.util.InetNetwork;
import com.caucho.util.L10N;
import com.caucho.util.LruCache;
import com.caucho.rewrite.RequestPredicate;
import com.caucho.server.security.*;

import javax.annotation.PostConstruct;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * Combines matches.
 *
 * <pre>
 * &lt;sec:Allow url-pattern="/admin/*"&gt;
 *   &lt;sec:NotOr>
 *     &lt;sec:Address value="192.168.1.10"/&gt;
 *     &lt;sec:Address value="192.168.1.11"/&gt;
 *   &lt;/sec:NotOr>
 * &lt;/sec:Allow>
 * </pre>
 */
public class NotOr implements RequestPredicate {
  private ArrayList<RequestPredicate> _predicateList
    = new ArrayList<RequestPredicate>();

  private RequestPredicate []_predicates;

  /**
   * Add a sub-predicate
   */
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
   * Returns true if the user is authorized for the resource.
   */
  public boolean isMatch(HttpServletRequest request)
  {
    for (RequestPredicate predicate : _predicates) {
      if (predicate.isMatch(request))
	return false;
    }

    return true;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + _predicateList;
  }
}
