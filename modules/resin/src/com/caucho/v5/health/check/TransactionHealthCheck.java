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
import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.transaction.TransactionHealthCheckImpl;

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
