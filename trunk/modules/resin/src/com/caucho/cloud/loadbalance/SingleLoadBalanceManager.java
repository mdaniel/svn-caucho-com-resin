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

package com.caucho.cloud.loadbalance;

import com.caucho.config.ConfigException;
import com.caucho.network.balance.ClientSocket;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.util.L10N;

/**
 * Manages a load balancer.
 */
public class SingleLoadBalanceManager extends CustomLoadBalanceManager {
  private static final L10N L = new L10N(SingleLoadBalanceManager.class);

  private ClientSocketFactory _serverPool;

  public SingleLoadBalanceManager(ClientSocketFactory serverPool,
                                   String probeCategory)
  {
    super(probeCategory);

    _serverPool = serverPool;
  }
  
  public SingleLoadBalanceManager(String probeCategory)
  {
    super(probeCategory);
  }

  public void addAddress(String address)
  {
    if (_serverPool != null)
      throw new ConfigException(L.l("Multiple backend load balancing requires Resin Professional."));

    _serverPool = createServerPool(address);
  }

  public void init()
  {
    if (_serverPool == null)
      throw new ConfigException(L.l("Load-balancing requires at least one server address."));
  }

  /**
   * Opens the next available server.
   */
  public ClientSocket openSticky(String sessionId,
                                 Object requestInfo,
                                 ClientSocketFactory oldSrun)
  {
    if (_serverPool == oldSrun)
      return null;

    return _serverPool.open();
  }
}
