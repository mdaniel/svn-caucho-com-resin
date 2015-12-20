/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.health.check.*;
import com.caucho.v5.jmx.server.*;
import com.caucho.v5.management.server.HealthCheckMXBean;

public class HealthCheckAdmin extends ManagedObjectBase 
  implements HealthCheckMXBean
{
  private final String _name;
  private final HealthCheck _healthCheck;
  private HealthSubSystem _healthService;

  HealthCheckAdmin(String name, 
                   HealthCheck healthCheck, 
                   HealthSubSystem healthService)
  {
    _name = name;
    _healthCheck = healthCheck;
    _healthService = healthService;
  }

  @Override
  public String getStatus()
  {
    return getLastResultSafe().getStatus().toString();
  }
  
  @Override
  public int getStatusOrdinal()
  {
    return getLastResultSafe().getStatus().ordinal();
  }
  
  @Override
  public String getMessage()
  {
    return getLastResultSafe().getMessage();
  }
  
  private HealthCheckResult getLastResultSafe()
  {
    HealthCheckResult lastResult = _healthService.getLastResult(_healthCheck);
    
    if (lastResult != null)
      return lastResult;
    else
      return new HealthCheckResult(HealthStatus.UNKNOWN);
  }
  
  @Override
  public void silenceForPeriodMs(long periodMs)
  {
    _healthCheck.silenceFor(new Period(periodMs));
  }

  @Override
  public void setLogPeriodMs(long periodMs)
  {
    _healthCheck.setLogPeriod(new Period(periodMs));
  }
  
  @Override
  public long getLogPeriodMs()
  {
    return ((AbstractHealthCheck)_healthCheck).getLogPeriod();
  }
  
  //
  // jmx
  //

  @Override
  public String getName()
  {
    return _name;
  }

  void register()
  {
    registerSelf();
  }
  
  void unregister()
  {
    unregisterSelf();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
