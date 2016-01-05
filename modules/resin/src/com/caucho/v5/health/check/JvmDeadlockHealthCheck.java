/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.check;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.JvmDeadlockHealthCheckImpl;

/**
 * Monitors for deadlocked threads, as determined by the JVM.
 * <p>
 * Generates FATAL if deadlocked threads are detected.
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class JvmDeadlockHealthCheck extends JvmDeadlockHealthCheckImpl
{
  public JvmDeadlockHealthCheck()
  {
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
}
