/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.health.check;

import io.baratine.core.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.config.Configurable;
import com.caucho.env.dbpool.ConnectionPoolHealthCheckImpl;
import com.caucho.env.health.HealthSubSystem;

/**
 * Monitors the health of Resin database connection pools (&lt;database&gt).  
 * See Resin documentation on &lt;database&gt for additional configuration.
 * <p>
 * Generates WARNING upon exceeding max-connections.
 * <p>
 * Generates CRITICAL upon exceeding max-overflow-connections.
 * 
 */
@Startup
@Singleton
@Configurable
@Named
public class ConnectionPoolHealthCheck extends ConnectionPoolHealthCheckImpl
{
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
}
