/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.http.proxy;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.v5.network.balance.ClientSocket;
import com.caucho.v5.network.balance.ClientSocketFactory;
import com.caucho.v5.util.L10N;

/**
 * Strategy for selecting the next server
 */
abstract public class StrategyLoadBalanceBase 
{
  static protected final Logger log
    = Logger.getLogger(StrategyLoadBalanceBase.class.getName());

  static final L10N L = new L10N(StrategyLoadBalanceBase.class);
  
  private final ClientGroupLoadBalance _clientGroup;
  
  private final AtomicInteger _roundRobin = new AtomicInteger(0);

  protected StrategyLoadBalanceBase(ClientGroupLoadBalance clientGroup)
  {
    _clientGroup = clientGroup;
  }
  
  protected ClientSocketFactory []getClientList()
  {
    return _clientGroup.getClientList();
  }
  
  /**
   * Opens the next available server.
   *
   * @param oldClient on failover, the client which failed with 503
   *
   * @return the next available server
   */
  public final ClientSocket openServer(ClientSocketFactory oldClient)
  {
    int startIndex = nextRoundRobin();

    ClientSocket stream = null;

    stream = openBestServer(startIndex, oldClient);

    if (stream != null) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("Using best available server: {0}", stream.getDebugId()));
      return stream;
    }

    stream = openBackupServer(startIndex,  oldClient);

    if (stream != null) {
      if (log.isLoggable(Level.FINE))
        log.fine(L.l("Using backup server: {0}", stream.getDebugId()));
    }
    
    return stream;
  }
  
  /**
   * Opens the best load balance server.
   *
   * @param oldClient on failover, the client which failed with 503
   */
  protected ClientSocket openBestServer(int startIndex,
                                        ClientSocketFactory oldClient)
  {
    return null;
  }

  /**
   * Open a backup server.
   */
  private ClientSocket openBackupServer(int startIndex, 
                                        ClientSocketFactory oldClient)
  {
    ClientSocketFactory []clientList = getClientList();
    
    int length = clientList.length;

    ClientSocketFactory client;
    ClientSocket stream;
    
    // second time, try the first client that works.
    for (int i = 0; i < length; i++) {
      int index = (startIndex + i) % length;
      client = clientList[index];

      if (client == null || client == oldClient)
        continue;

      stream = client.openWarm();
      if (stream != null)
        return stream;
    }

    // third time force the open
    for (int i = 0; i < length; i++) {
      int index = (startIndex + i) % length;
      client = clientList[index];

      if (client == null || client == oldClient)
        continue;

      stream = client.open();
      if (stream != null)
        return stream;
    }

    return null;
  }

  /**
   * Returns the index of the next round-robin server.
   */
  protected int nextRoundRobin()
  {
    ClientSocketFactory []clientList = getClientList();
    
    int length = clientList.length;

    for (int i = 0; i < length; i++) {
      int index = nextRoundRobin(length);

      ClientSocketFactory server = clientList[index];

      if (server == null)
        continue;
      
      // env/0512
      if (server.canOpen())
        return index;
    }

    return _roundRobin.get();
  }
  
  private int nextRoundRobin(int length)
  {
    int roundRobin = _roundRobin.get();
    
    if (length > 0) {
      int nextRoundRobin = (roundRobin + 1) % length;
    
      _roundRobin.compareAndSet(roundRobin, nextRoundRobin);
    }
    
    return roundRobin % length;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
