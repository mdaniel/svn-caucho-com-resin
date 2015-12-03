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

package com.caucho.servlets;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

import javax.servlet.*;
import javax.servlet.http.*;

import com.caucho.cloud.loadbalance.*;
import com.caucho.config.ConfigException;
import com.caucho.config.types.Period;
import com.caucho.network.proxy.*;
import com.caucho.util.L10N;

@SuppressWarnings("serial")
public class HttpProxyServlet extends GenericServlet 
{
  private static final Logger log =
    Logger.getLogger(HttpProxyServlet.class.getName());
  private static final L10N L = new L10N(HttpProxyServlet.class);
  
  private List<LoadBalanceBackend> _backends 
    = new ArrayList<LoadBalanceBackend>();

  private LoadBalanceBuilder _loadBalanceBuilder;
  private HttpProxyClient _proxyClient;

  public HttpProxyServlet()
  {
    LoadBalanceService loadBalanceService = LoadBalanceService.getCurrent();
    
    if (loadBalanceService == null) {
      throw new IllegalStateException(L.l("'{0}' requires an active {1}",
                                          this,
                                          LoadBalanceService.class.getSimpleName()));
    }
    
    _loadBalanceBuilder = loadBalanceService.createBuilder();
  }
  
  public void addHost(String host)
  {
    addAddress(host);
  }

  public void addAddress(String address)
  {
    addBackend(new LoadBalanceBackend(address));
  }
  
  public void addBackend(LoadBalanceBackend backend)
  {
    _backends.add(backend);
  }
  
  public void setConnectTimeout(Period connectTimeout)
  {
    _loadBalanceBuilder.setConnectTimeout(connectTimeout.getPeriod());
  }
  
  public void setConnectionMin(int connectionMin)
  {
    _loadBalanceBuilder.setConnectionMin(connectionMin);
  }

  public void setSocketTimeout(Period socketTimeout)
  {
    _loadBalanceBuilder.setSocketTimeout(socketTimeout.getPeriod());
  }

  public void setIdleTime(Period idleTime)
  {
    _loadBalanceBuilder.setIdleTime(idleTime.getPeriod());
  }

  public void setRecoverTime(Period recoverTime)
  {
    _loadBalanceBuilder.setRecoverTime(recoverTime.getPeriod());
  }
  
  public void setWarmupTime(Period warmupTime)
  {
    _loadBalanceBuilder.setWarmupTime(warmupTime.getPeriod());
  }
  
  public void setStrategy(String strategy)
  {
    if ("round-robin".equalsIgnoreCase(strategy))
      _loadBalanceBuilder.setStrategy(LoadBalanceStrategy.ROUND_ROBIN);
    else if ("least-connection".equalsIgnoreCase(strategy))
      _loadBalanceBuilder.setStrategy(LoadBalanceStrategy.ADAPTIVE);
    else if ("adaptive".equalsIgnoreCase(strategy))
      _loadBalanceBuilder.setStrategy(LoadBalanceStrategy.ADAPTIVE);
    else
      throw new ConfigException(L.l("'{0}' is an unknown load-balance strategy.  'round-robin' and 'least-connection' are the known values.",
                                    strategy));
  }
  
  // for QA
  public LoadBalanceBuilder getLoadBalanceBuilder()
  {
    return _loadBalanceBuilder;
  }
  
  /**
   * Initialize the servlet with the server's sruns.
   */
  @Override
  public void init()
    throws ServletException
  {
    if (_proxyClient != null)
      return;
    
    for(LoadBalanceBackend backend : _backends) {
      _loadBalanceBuilder.addBackend(backend);
    }
    
    HttpProxyClientFactory factory = new HttpProxyClientFactory();
    _proxyClient = factory.create(_loadBalanceBuilder.create());
  }
  
  /**
   * Handle the request.
   */
  @Override
  public void service(ServletRequest request, ServletResponse response)
    throws ServletException, IOException
  {
    HttpServletRequest req = (HttpServletRequest) request;
    HttpServletResponse res = (HttpServletResponse) response;
    
    _proxyClient.handleRequest(req, res);
  }
}
