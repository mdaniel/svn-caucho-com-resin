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

import javax.servlet.http.HttpServletRequest;

import io.baratine.config.Configurable;

/**
 * Match if the request is secure, i.e. if request.isSecure() matches.
 *
 * <pre>
 * &lt;resin:Allow url-pattern="/admin/*"&gt;
 *                  xmlns:resin="urn:java:com.caucho.resin"&gt;
 *   &lt;resin:IfSecure/>
 * &lt;/resin:Allow>
 * </pre>
 *
 * <p>RequestPredicates may be used for security and rewrite actions.
 */
@Configurable
public class IfSecure implements RequestPredicate
{
  private boolean _isSecure = true;

  /**
   * Sets the secure value to test against.  If true matches if
   * isSecure() is true
   */
  @Configurable
  public void setValue(boolean value)
  {
    _isSecure = value;
  }

  /**
   * True if the predicate matches.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    return _isSecure == request.isSecure();
  }
}
