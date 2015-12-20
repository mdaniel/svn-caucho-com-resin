/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 */

package com.caucho.v5.health.check;

import java.util.logging.Logger;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.*;

public interface HealthCheck
{
  public HealthCheckResult checkHealth();
  
  public boolean isEnabled();
  
  public String getName();
  
  public void start();
  
  public void stop();
  
  public void silenceFor(Period period);
  
  public void setLogPeriod(Period period);
  
  public void logResult(HealthCheckResult result, Logger log);
  
  public HealthCheckResult getLastResult(HealthSubSystem healthService);
}
