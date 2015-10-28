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

package com.caucho.http.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.network.balance.ClientSocket;
import com.caucho.v5.network.balance.ClientSocketFactory;
import com.caucho.v5.util.Crc64;

/**
 * Manages a load balancer.
 */
public class LoadBalanceManagerCluster extends LoadBalanceManager {
  private static final Logger log
    = Logger.getLogger(LoadBalanceManagerCluster.class.getName());

  private final static int DECODE[];

  private final ClientGroupLoadBalance _clientGroup;
  private final HashGeneratorStickyRequest _sticky; 
  private final StrategyLoadBalanceBase _strategy;

  public LoadBalanceManagerCluster(ClientGroupLoadBalance clientGroup,
                                   HashGeneratorStickyRequest sticky,
                                   StrategyLoadBalance strategyType)
  {
    _clientGroup = clientGroup;
    _sticky = sticky;
    
    StrategyLoadBalanceBase strategy = null;
    
    switch (strategyType) {
    case ROUND_ROBIN:
      strategy = new StrategyLoadBalanceRoundRobin(clientGroup);
      break;

    case ADAPTIVE:
    default:
      strategy = new StrategyLoadBalanceLeastConnection(clientGroup);
      break;
    }
    
    _strategy = strategy;
  }

  /**
   * Opens the next available server.
   */
  @Override
  public ClientSocket openSticky(String sessionId,
                                 Object requestInfo,
                                 ClientSocketFactory oldSrun)
  {
    ClientSocket stream = openStickyServer(sessionId, oldSrun);

    if (stream != null) {
      if (log.isLoggable(Level.FINE))
        log.finer("Using sticky server " + stream.getDebugId() 
                  + " for session " + sessionId);
      return stream;
    }
    
    if (_sticky != null) {
      String hash = _sticky.getHash(requestInfo);
      
      if (hash != null) {
        stream = openHashServer(hash, oldSrun);
        if (stream != null) {
          if (log.isLoggable(Level.FINE))
            log.finer("Using hash server " + stream.getDebugId() 
                      + " for session " + sessionId);
          return stream;
        }
      }
    }

    return _strategy.openServer(oldSrun);
  }

  /**
   * Opens the sticky session server.
   *
   * @param sessionId the request session cookie
   * @param oldSrun on failover, the original server
   */
  private ClientSocket openStickyServer(String sessionId,
                                        ClientSocketFactory oldSrun)
  {
    if (sessionId == null || sessionId.length() < 3) {
      return null;
    }

    int index = decode(sessionId.charAt(0));

    ClientSocket stream = openStream(index, oldSrun);

    if (stream != null)
      return stream;
    
    return openHashServer(sessionId, oldSrun);
  }

  private ClientSocket openHashServer(String key,
                                      ClientSocketFactory oldSrun)
  {
    if (key == null || key.length() == 0)
      return null;
    
    long hash = Crc64.generate(0, key);
    hash = hash ^ Long.reverse(hash);
    ClientSocket stream = null;
    
    for (int i = 0; i < 3; i++) {
      stream = openStream(hash, oldSrun);

      if (stream != null) {
        return stream;
      }

      hash = hash >> 6;
    }

    return null;
  }

  private ClientSocket openStream(long hash, ClientSocketFactory oldServer)
  {
    ClientSocketFactory []clientList = getClientList();
    
    if (hash < 0)
      hash = - hash;
    
    int index = (int) (hash % clientList.length);

    ClientSocketFactory client = clientList[index];
    
    if (client == null) {
      return null;
    }
    else if (client == oldServer) {
      return null;
    }

    ClientSocket stream = client.openSticky();

    if (stream == null && log.isLoggable(Level.FINE)) {
      log.fine(this + " open for " + client.getDebugId()
               + " connection failed.");
    }

    return stream;
  }

  @Override
  public void close()
  {
    _clientGroup.close();
  }
  
  private ClientSocketFactory []getClientList()
  {
    return _clientGroup.getClientList();
  }

  private static int decode(int code)
  {
    return DECODE[code & 0x7f];
  }

  /**
   * Converts an integer to a printable character
   */
  private static char convert(long code)
  {
    code = code & 0x3f;

    if (code < 26)
      return (char) ('a' + code);
    else if (code < 52)
      return (char) ('A' + code - 26);
    else if (code < 62)
      return (char) ('0' + code - 52);
    else if (code == 62)
      return '_';
    else
      return '-';
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _clientGroup + "]";
  }

  static {
    DECODE = new int[128];
    for (int i = 0; i < 64; i++)
      DECODE[(int) convert(i)] = i;
  }
}
