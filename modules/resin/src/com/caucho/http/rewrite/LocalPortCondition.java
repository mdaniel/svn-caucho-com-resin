/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
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

package com.caucho.http.rewrite;

import com.caucho.util.L10N;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
* A rewrite condition that passes if the value of
 * {@link HttpServletRequest#getLocalPort()} matches a regexp.
*/
public class LocalPortCondition
  extends AbstractCondition
{
  private static final L10N L = new L10N(LocalPortCondition.class);

  private final int _localPort;

  LocalPortCondition(int localPort)
  {
    _localPort = localPort;
  }

  public String getTagName()
  {
    return "local-port";
  }

  public boolean isMatch(HttpServletRequest request,
                         HttpServletResponse response)
  {
    return request.getLocalPort() == _localPort;
  }
}
