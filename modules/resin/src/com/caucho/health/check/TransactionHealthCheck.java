/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.check;

import io.baratine.core.Startup;

import javax.inject.Named;
import javax.inject.Singleton;

import com.caucho.config.Configurable;
import com.caucho.env.health.HealthSubSystem;
import com.caucho.transaction.TransactionHealthCheckImpl;

/**
 * Monitors the transaction manager for commit failures.
 * <p>
 * Generates WARNING if there were commit failures since the last check.
 *
 */
@Startup
@Singleton
@Configurable
@Named
public class TransactionHealthCheck extends TransactionHealthCheckImpl
{
  public TransactionHealthCheck()
  {
  }
  
  @Override
  protected AbstractHealthCheck findDelegate(HealthSubSystem healthService)
  {
    return healthService.getHealthCheck(getClass());
  }
}
