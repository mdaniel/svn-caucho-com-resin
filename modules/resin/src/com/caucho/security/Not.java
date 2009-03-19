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
 *   &lt;sec:Not>
 *     &lt;sec:Address value="192.168.1.10"/&gt;
 *   &lt;/sec:Not>
 * &lt;/sec:Allow>
 * </pre>
 */
public class Not implements ServletRequestPredicate {
  public static final L10N L = new L10N(Not.class);
  
  private ServletRequestPredicate _predicate;

  public Not()
  {
  }

  public Not(ServletRequestPredicate predicate)
  {
    add(predicate);
  }

  /**
   * Add a sub-predicate
   */
  public void add(ServletRequestPredicate predicate)
  {
    if (_predicate != null)
      throw new ConfigException(L.l("security:Not requires a single value"));
      
    _predicate = predicate;
  }

  @PostConstruct
  public void init()
  {
    if (_predicate == null)
      throw new ConfigException(L.l("security:Not requires a value"));
  }

  /**
   * Returns true if the user is authorized for the resource.
   */
  public boolean isMatch(HttpServletRequest request)
  {
    return ! _predicate.isMatch(request);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _predicate + "]";
  }
}
