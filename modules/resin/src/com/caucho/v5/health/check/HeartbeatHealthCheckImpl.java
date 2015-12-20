/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.caucho.v5.bartender.BartenderSystem;
import com.caucho.v5.bartender.ServerBartender;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthStatus;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Health check for the heartbeat.
 */
public class HeartbeatHealthCheckImpl extends AbstractHealthCheck
{
  private static final L10N L = new L10N(HeartbeatHealthCheckImpl.class);
  private static final long TIMEOUT = 180 * 1000L; 
  
  private ServerBartender _selfServer;
  
  public HeartbeatHealthCheckImpl()
  {
    _selfServer = BartenderSystem.getCurrent().getServerSelf();
    
    if (_selfServer == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          BartenderSystem.class.getSimpleName()));
    }
    

  }
  
  /**
   * Returns the health status for this health check.
   */
  @Override
  public HealthCheckResult checkHealth()
  {
    HealthStatus status = HealthStatus.OK;
    List<String> messages = new ArrayList<String>();
    
    boolean isTriad = _selfServer.isHub();
    long now = CurrentTime.getCurrentTime();
    
    for (ServerBartender server : _selfServer.getCluster().getServers()) {
      if (server == null) {
        continue;
      }
      
      if (server.isSelf()) {
        continue;
      }
      
      if (! server.isHub() && ! isTriad) {
        continue;
      }
      
      if (! server.isUp()) {
        status = HealthStatus.WARNING;
        messages.add("no active heartbeat from " + server);
      }
      else if (server.getLastHeartbeatTime() + TIMEOUT < now) {
        status = HealthStatus.WARNING;
        messages.add("no recent active heartbeat from " 
                     + server
                     + " (" 
                     + (now - server.getLastHeartbeatTime()) / 1000 
                     + "s)");
      }
    }
    
    if (status == HealthStatus.OK) {
      return new HealthCheckResult(status);
    }
    
    StringBuilder sb = new StringBuilder();
    Iterator<String> iter = messages.iterator();
    while(iter.hasNext()) {
      sb.append(iter.next());
      if (iter.hasNext()) {
        sb.append(", ");
      }
    }
    
    return new HealthCheckResult(status, sb.toString());
  }
}
