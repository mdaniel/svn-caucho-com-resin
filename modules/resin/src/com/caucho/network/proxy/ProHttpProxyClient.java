package com.caucho.network.proxy;
/*
 * Copyright (c) 1998-2013 Caucho Technology -- all rights reserved
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
 * @author Paul Cowan
 */

import java.util.logging.Logger;

import javax.servlet.http.*;

import com.caucho.http.proxy.LoadBalanceManager;
import com.caucho.v5.network.balance.*;
import com.caucho.v5.util.L10N;

public class ProHttpProxyClient extends HttpProxyClient
{
  private static final Logger log = 
    Logger.getLogger(ProHttpProxyClient.class.getName());
  private static final L10N L = new L10N(ProHttpProxyClient.class);
  
  public ProHttpProxyClient(LoadBalanceManager loadBalancer)
  {
    super(loadBalancer);
  }
  
  public void handleRequest(HttpServletRequest req, 
                            HttpServletResponse res)
  {
    String sessionId = getSessionId(req);
    
    ClientSocket client = getLoadBalancer().openSticky(sessionId, req, null);
    if (client == null) {
      proxyFailure(req, res, null, "no backend servers available", true);
      return;
    }
    
    String uri = constructURI(req);
    
    long requestStartTime = System.currentTimeMillis();
    ProxyResult result = proxy(req, res, uri, sessionId, client);
  
    if (result.isKeepAlive()) {
      client.free(requestStartTime);
    } else {
      client.close();
    }
    
    ClientSocketFactory socketFactory = client.getPool();
    
    switch(result.getStatus()) {
      case BUSY:
        proxyFailure(req, res, client, "busy", false);
        socketFactory.busy(requestStartTime);
        break;
      case FAIL:
        proxyFailure(req, res, client, result.getFailureMessage(), false);
        socketFactory.failSocket(requestStartTime);
        break;
      default: // OK
        return;
    }
    
    // now in potential failover situation
    
    if (res.isCommitted()) {
      proxyFailure(req, res, client, "cannot failover commited response", true);
      return;
    }
    
    if (! "GET".equals(req.getMethod())) {
      proxyFailure(req, res, client, L.l("cannot failover {0} request", 
                                         req.getMethod()), true);
      return;
    }
    
    ClientSocket client2 = getLoadBalancer().openSticky(sessionId, req, socketFactory);
    if (client2 == null) {
      proxyFailure(req, res, client, "cannot failover: no backend servers available", true);
      return;
    }
    
    log.info(L.l("{0}: failing over from {1} to {2} due to {3}",
                 this,
                 client.getDebugId(),
                 client2.getDebugId(),
                 result.getFailureMessage()));
    
    res.reset();
    
    long requestStartTime2 = System.currentTimeMillis();
    ProxyResult result2 = proxy(req, res, uri, sessionId, client2);
  
    if (result2.isKeepAlive()) {
      client2.free(requestStartTime);
    } else {
      client2.close();
    }
    
    ClientSocketFactory socketFactory2 = client2.getPool();
    
    switch(result2.getStatus()) {
      case BUSY:
        proxyFailure(req, res, client2, "busy", true);
        socketFactory2.busy(requestStartTime2);
        break;
      case FAIL:
        proxyFailure(req, res, client2, result2.getFailureMessage(), true);
        socketFactory2.failSocket(requestStartTime2);
        break;
      default: // OK
        // nothing to do
    }
  }
}
