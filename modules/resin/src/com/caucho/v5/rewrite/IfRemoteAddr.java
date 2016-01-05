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

package com.caucho.v5.rewrite;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.http.rewrite.RequestPredicate;
import com.caucho.v5.util.InetNetwork;

@Configurable
public class IfRemoteAddr implements RequestPredicate
{
  private ArrayList<InetNetwork> _networkList = new ArrayList<InetNetwork>();
  private InetNetwork []_networks = new InetNetwork[0];

  public void addValue(InetNetwork value)
  {
    addNetwork(value);
  }

  @ConfigArg(0)
  public void addNetwork(InetNetwork value)
  {
    _networkList.add(value);
    _networks = new InetNetwork[_networkList.size()];
    _networkList.toArray(_networks);
  }

  @Override
  public boolean isMatch(HttpServletRequest request)
  {
    String remoteAddr = request.getRemoteAddr();

    if (remoteAddr == null)
      return false;

    for (InetNetwork network : _networks) {
      if (network.isMatch(remoteAddr))
        return true;
    }

    return false;
  }
}
