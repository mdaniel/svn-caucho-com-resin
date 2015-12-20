/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import io.baratine.core.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.ResinHealthCheckImpl;

/**
 * This check aggregates all the other health checks, resulting in the current 
 * overall health.  
 * <p>
 * Generates CRITICAL if any checks are currently reporting CRITICAL.
 * <p>
 * Generates WARNING if any checks are currently reporting WARNING.
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class SystemHealthCheck extends ResinHealthCheckImpl
{
  public SystemHealthCheck()
  {
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
}
