/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import io.baratine.service.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthSubSystem;

/**
 * Monitors for heartbeats from other members of the cluster.
 * <p>
 * Generates WARNING if no heartbeat has been received from a know member of 
 * the cluster.
 * <p>
 * Generates WARNING if a heartbeat has not been received in the last 180 
 * seconds from a known member of the cluster.
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class HeartbeatHealthCheck extends HeartbeatHealthCheckImpl
{
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
}
