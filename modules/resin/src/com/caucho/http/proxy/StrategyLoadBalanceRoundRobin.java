/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.http.proxy;

import com.caucho.network.balance.ClientSocket;
import com.caucho.network.balance.ClientSocketFactory;

/**
 * Strategy for selecting the next server
 */
public class StrategyLoadBalanceRoundRobin extends StrategyLoadBalanceBase {
  protected StrategyLoadBalanceRoundRobin(ClientGroupLoadBalance clientGroup)
  {
    super(clientGroup);
  }
  
  /**
   * Opens the best load balance server.
   */
  @Override
  protected ClientSocket openBestServer(int startIndex,
                                        ClientSocketFactory oldSrun)
  {
    ClientSocketFactory []clientList = getClientList();
    
    if (clientList.length <= startIndex)
      return null;
    
    ClientSocketFactory client = clientList[startIndex];

    if (client == null)
      return null;
    else if (client.isDead())
      return null;

    return client.openWarm();
  }
}
