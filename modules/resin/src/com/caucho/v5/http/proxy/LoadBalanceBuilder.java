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

import java.util.ArrayList;
import java.util.Objects;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.bartender.pod.PodBartender;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.http.container.HttpContainer;
import com.caucho.v5.http.container.HttpContainerServlet;
import com.caucho.v5.network.balance.ClientSocketFactory;
import com.caucho.v5.util.L10N;

/**
 * LoadBalanceService distributes requests across a group of clients.
 */
public class LoadBalanceBuilder
{
  private static final L10N L = new L10N(LoadBalanceBuilder.class);
  
  private StrategyLoadBalance _strategy = StrategyLoadBalance.ADAPTIVE;
  private String _meterCategory = null;
  
  private LoadBalanceBackend _defaults = new LoadBalanceBackend();
  
  private ArrayList<ClientSocketFactory> _clientList 
    = new ArrayList<>();
    
  private PodBartender _pod;
  private int _targetPort;
  private HashGeneratorStickyRequest _sticky;

  /**
   * Sets the load balance strategy.
   */
  public void setStrategy(StrategyLoadBalance strategy)
  {
    Objects.requireNonNull(strategy);
    
    _strategy = strategy;
  }

  /**
   * The load balance strategy.
   */
  public StrategyLoadBalance getStrategy()
  {
    return _strategy;
  }
  
  /**
   * The request-sticky generator
   */
  /*
  public void setStickyRequestHashGenerator(StickyRequestHashGenerator gen)
  {
    
  }
  
  public void setTargetCluster(String clusterId)
  {
    throw new IllegalStateException(L.l("{0}: setCluster requires Resin Professional.",
                                        this));
  }

  public void setTargetCluster(RackHeartbeat pod)
  {
    throw new IllegalStateException(L.l("{0}: setCluster requires Resin Professional.",
                                        this));
  }
  
  public void setTargetPort(int port)
  {
    throw new IllegalStateException(L.l("{0}: setPort requires Resin Professional.",
                                        this));
  }
  */
  
  public void setConnectTimeout(long connectTimeout)
  {
    _defaults.setConnectTimeoutMs(connectTimeout);
  }
  
  public void setConnectionMin(int connectionMin)
  {
    _defaults.setConnectionMin(connectionMin);
  }

  public void setSocketTimeout(long socketTimeout)
  {
    _defaults.setSocketTimeoutMs(socketTimeout);
  }

  public void setIdleTime(long idleTime)
  {
    _defaults.setIdleTimeMs(idleTime);
  }

  public void setRecoverTime(long recoverTime)
  {
    _defaults.setRecoverTimeMs(recoverTime);
  }
  
  public void setWarmupTime(long warmupTime)
  {
    _defaults.setWarmupTimeMs(warmupTime);
  }
  
  public LoadBalanceBackend getDefaults()
  {
    return _defaults;
  }
  
  /**
   * The statistics meter category.
   */
  public void setMeterCategory(String category)
  {
    _meterCategory = category;
  }
  
  /**
   * The statistics meter category.
   */
  public String getMeterCategory()
  {
    return _meterCategory;
  }
  
  public void addAddress(String address)
  {
    addBackend(new LoadBalanceBackend(address));
  }
  
  public void addBackend(LoadBalanceBackend backend)
  {
    addClient(createClientSocketFactory(backend));
  }
  
  /**
   * Adds a client pool factory.
   */
  public void addClient(ClientSocketFactory client)
  {
    client.init();
    client.start();
    
    _clientList.add(client);
  }
  
  public ArrayList<ClientSocketFactory> getClientList()
  {
    return _clientList;
  }
  
  /**
   * Returns the load balance manager.
   */
  /*
  public LoadBalanceManager create()
  {
    ClientSocketFactory socketFactory = null;
    if (getClientList().size() > 0)
      socketFactory = getClientList().get(0);

    return new SingleLoadBalanceManager(socketFactory, getMeterCategory());
  }
  */
  
  protected ClientSocketFactory createClientSocketFactory(String address)
  {
    int p = address.lastIndexOf(':');
    int q = address.lastIndexOf(']');

    if (p < 0 && q <= p)
      throw new ConfigException(L.l("'{0}' is an invalid address because it does not specify the port.",
                                    address));

    String host = address.substring(0, p);
    int port = Integer.parseInt(address.substring(p + 1));

    HttpContainerServlet httpContainer = HttpContainer.current();
    
    BartenderSystem bartender = BartenderSystem.getCurrent();
    ServerBartender server = bartender.getServerHandle(host, port);

    boolean isSecure = false;

    ClientSocketFactory factory
      = new ClientSocketFactory(httpContainer.getServerId(),
                                address,
                                getMeterCategory(),
                                address,
                                server,
                                isSecure);
    
    applyDefaults(factory);
    
    return factory;
  }
  
  protected ClientSocketFactory createClientSocketFactory(LoadBalanceBackend backend)
  {
    ClientSocketFactory factory = createClientSocketFactory(backend.getAddress());
    applyBackendConfig(factory, backend);
    
    return factory;
  }
  
  protected void applyDefaults(ClientSocketFactory factory)
  {
    applyBackendConfig(factory, _defaults);
  }
  
  protected void applyBackendConfig(ClientSocketFactory factory, LoadBalanceBackend backend)
  {
    if (backend.hasConnectionMin())
      factory.setLoadBalanceConnectionMin(backend.getConnectionMin());
    
    if (backend.hasConnectTimeout())
      factory.setLoadBalanceConnectTimeout(backend.getConnectTimeout());
    
    if (backend.hasIdleTime())
      factory.setLoadBalanceIdleTime(backend.getIdleTime());
    
    if (backend.hasRecoverTime())
      factory.setLoadBalanceRecoverTime(backend.getRecoverTime());
    
    if (backend.hasBusyRecoverTime())
      factory.setLoadBalanceBusyRecoverTime(backend.getBusyRecoverTime());
    
    if (backend.hasSocketTimeout())
      factory.setLoadBalanceSocketTimeout(backend.getSocketTimeout());
    
    if (backend.hasWarmupTime())
      factory.setLoadBalanceWarmupTime(backend.getWarmupTime());
    
    if (backend.hasWeight())
      factory.setLoadBalanceWeight(backend.getWeight());    
  }
  
  /*
  public void setTargetCluster(String clusterId)
  {
    BartenderSystem bartender = BartenderSystem.getCurrent();
    
    PodBartender pod = bartender.createPodStub(clusterId);
    
    if (pod == null) {
      throw new ConfigException(L.l("'{0}' is an unknown cluster for load balancing.",
                                    clusterId));
    }
   
    // setTargetCluster(cluster.getRacks()[0]);
    
    _pod = pod;
  }
  */
  
  public void setTargetPort(int port)
  {
    _targetPort = port;
  }
  
  public void setTargetPod(PodBartender pod)
  {
    _pod = pod;
  }
  
  public void setStickyRequestHashGenerator(HashGeneratorStickyRequest sticky)
  {
    _sticky = sticky;
  }
  
  /**
   * Returns the load balance manager.
   */
  public LoadBalanceManager create()
  {
    ClientGroupLoadBalance clientGroup;
    
    if (_pod != null)
      clientGroup = new ClientGroupLoadBalanceCluster(_pod, _targetPort);
    else if (getClientList().size() > 0)
      clientGroup = new ClientGroupLoadBalanceStatic(getClientList());
    else {
      throw new ConfigException(L.l("LoadBalanceBuilder needs either a client list or a cluster"));
    }
    
    return new LoadBalanceManagerCluster(clientGroup, _sticky, getStrategy());
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
