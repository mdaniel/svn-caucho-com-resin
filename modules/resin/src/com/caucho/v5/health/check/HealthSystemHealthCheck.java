/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.check;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthServiceHealthCheckImpl;
import com.caucho.v5.env.health.HealthSubSystem;

/**
 * Monitors the health system itself by using a separate thread to determine 
 * if health checking is frozen or taking too long.
 * <p>
 * Generates FATAL if health checking has not occurred within 
 * freeze timeout (default 15 minutes)
 * <p>
 * Generates FATAL if health checking has not complete within acceptable 
 * time based on HealthSystem startup delay, period, and recheck parameters.
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class HealthSystemHealthCheck 
  extends HealthServiceHealthCheckImpl
{
  /**
   * Set the max time for no health checks to occur to determine the 
   * health system is frozen (default 15 minutes)
   */
  @Configurable
  public void setFreezeTimeout(Period freezeTimeout)
  {
    HealthSystemHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setFreezeTimeoutImpl(freezeTimeout.getPeriod());
    else
      super.setFreezeTimeoutImpl(freezeTimeout.getPeriod());
  }

  /**
   * Set the polling frequency of the independent thread (default 1 minute)
   */
  @Configurable
  public void setThreadCheckPeriod(Period threadCheckPeriod)
  {
    HealthSystemHealthCheck delegate = getDelegate();
    
    if (delegate != null)
      delegate.setThreadCheckPeriodImpl(threadCheckPeriod.getPeriod());
    else
      super.setThreadCheckPeriodImpl(threadCheckPeriod.getPeriod());
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
  
  @Override
  protected HealthSystemHealthCheck getDelegate()
  {
    return (HealthSystemHealthCheck) super.getDelegate();
  }
}
