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

package com.caucho.v5.http.proxy;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.network.balance.ClientSocketFactory;
import com.caucho.v5.util.L10N;


/**
 * Manages a load balancer.
 */
abstract public class LoadBalanceManagerCustom extends LoadBalanceManager {
  private static final L10N L = new L10N(LoadBalanceManagerCustom.class);

  private String _meterCategory;

  protected LoadBalanceManagerCustom(String meterCategory)
  {
    _meterCategory = meterCategory;
  }

  abstract public void addAddress(String address);

  protected String getProbeCategory()
  {
    return _meterCategory;
  }

  protected ClientSocketFactory createServerPool(String address)
  {
    int p = address.lastIndexOf(':');
    int q = address.lastIndexOf(']');

    if (p < 0 && q <= p)
      throw new ConfigException(L.l("'{0}' is an invalid address because it does not specify the port.",
                                    address));

    String host = address.substring(0, p);
    int port = Integer.parseInt(address.substring(p + 1));

    HttpContainer httpContainer = HttpContainer.current();

    boolean isSecure = false;
    
    BartenderSystem bartender = BartenderSystem.getCurrent();
    
    ServerBartender server = bartender.getServerHandle(host, port);

    return new ClientSocketFactory(httpContainer.getServerId(),
                          address,
                          getProbeCategory(),
                          address,
                          server, isSecure);
  }
}
