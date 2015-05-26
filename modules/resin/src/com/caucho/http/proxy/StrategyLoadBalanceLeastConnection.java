/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.http.proxy;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.caucho.network.balance.ClientSocket;
import com.caucho.network.balance.ClientSocketFactory;
import com.caucho.util.CurrentTime;

/**
 * Strategy for selecting the next server
 */
public class StrategyLoadBalanceLeastConnection extends StrategyLoadBalanceBase
{
  private static final Logger log
    = Logger.getLogger(StrategyLoadBalanceLeastConnection.class.getName());
  
  private static long MAX_COST = Integer.MAX_VALUE / 2;
  
  private int _cpuWeight = 0;
  private int _connectionWeight = 100;
  private int _roundRobin;
  
  public StrategyLoadBalanceLeastConnection(ClientGroupLoadBalance clientGroup)
  {
    super(clientGroup);
  }

  public void setCpuWeight(int weight)
  {
    _cpuWeight = weight;
  }

  public void setConnectionWeight(int weight)
  {
    _connectionWeight = weight;
  }

  /**
   * Opens the best load balance server.
   */
  @Override
  protected ClientSocket openBestServer(int startIndex,
                                         ClientSocketFactory oldSrun)
  {
    ClientSocketFactory []clientList = getClientList();
    int length = clientList.length;

    ClientSocket stream = null;
    ClientSocketFactory srun = null;

    double bestCost = MAX_COST;
    srun = null;
    long now = CurrentTime.getCurrentTime();

    synchronized (this) {
      int maxGreen = length;

      startIndex = _roundRobin;

      for (int i = 0; i < length; i++) {
        ClientSocketFactory client = clientList[i];

        // skip servers in warmup phase
        if (client == null || ! client.canOpenWarmOrRecycle()) {
          continue;
        }
        
        if (client.isBusy(now)) {
          continue;
        }

        long connections = (client.getActiveCount()
                            + client.getLoadBalanceAllocateCount());

        if (connections < client.getLoadBalanceConnectionMin()) {
          maxGreen = i;
          break;
        }
      }

      // first use the srun with the least number of active connections
      for (int i = 0; i < length; i++) {
        int index = (startIndex + i) % length;
        ClientSocketFactory client = clientList[index];

        if (client == null)
          continue;

        // skip servers in warmup phase
        if (! client.canOpenWarmOrRecycle()) {
          continue;
        }

        if (client == oldSrun) {
          continue;
        }

        // green load balancing
        if (maxGreen < index) {
          continue;
        }

        long weight = client.getLoadBalanceWeight();
        
        if (weight <= 0) {
          continue;
        }

        long connections = (client.getActiveCount()
                            + client.getLoadBalanceAllocateCount());
        
        double cpu = client.getCpuLoadAvg();

        double cost = 0.0;

        cost += _connectionWeight * connections;
        cost += _cpuWeight * cpu;
        
        double latencyFactor = client.getLatencyFactor();

        if (connections > 0) {
          cost += _connectionWeight * latencyFactor / 100;
        }

        cost = cost / (double) weight;
        
        if (client.isBusy(now)) {
          cost = MAX_COST - 1;
        }

        if (cost < bestCost) {
          srun = client;
          bestCost = cost;

          _roundRobin = (index + 1) % length;
        }
        
        if (log.isLoggable(Level.FINER)) {
          log.finer("load balance cost: " + cost
                    + " conn:" + connections
                    + " cpu:" + cpu
                    + " latency:" + latencyFactor
                    + " " + client);
        }
      }

      if (srun != null) {
        srun.allocateLoadBalance();
      }
    }

    if (srun != null) {
      try {
        stream = srun.openWarm();

        return stream;
      } finally {
        srun.freeLoadBalance();
      }
    }
    else {
      return null;
    }
  }
}
