/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.*;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.Alarm;
import com.caucho.v5.util.CurrentTime;

@Configurable
public class IfHealthStatus extends HealthPredicateCheckBase
{
  private HealthStatus _status = HealthStatus.UNKNOWN;
  
  private boolean _isSystemRecheckTime;
  private long _recheckTime = -1;
  private int _recheckCount = -1;
  
  public IfHealthStatus(HealthStatus status)
  {
    setStatus(status);
  }
  
  public IfHealthStatus(HealthCheck healthCheck, HealthStatus status)
  {
    super(healthCheck);
    
    setStatus(status);
  }
  
  @Configurable
  public void setStatus(HealthStatus status)
  {
    _status = status;
  }
  
  public HealthStatus getStatus()
  {
    return _status;
  }
  
  @Configurable
  public void setTime(Period recheckTime)
  {
    setTimeMillis(recheckTime.getPeriod());
  }
  
  public void setTimeMillis(long recheckTime)
  {
    _recheckTime = recheckTime;
  }
  
  public void setSystemRecheckTime(boolean isSystemRecheck)
  {
    _isSystemRecheckTime = isSystemRecheck;
  }
  
  public void setCount(int recheckCount)
  {
    _recheckCount = recheckCount;
  }

  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    HealthStatus status = HealthStatus.UNKNOWN;
    
    HealthCheckResult result = getLastResult(healthService);
    if (result != null)
      status = result.getStatus();

    if (status != getStatus())
      return false;
    
    long recheckTime = _recheckTime;

    if (recheckTime <= 0) {
      if (! _isSystemRecheckTime)
        return true;

      recheckTime = healthService.getSystemRecheckTimeout();

      if (recheckTime <= 0)
        return true;
    }
    long startTime = getStartTime(healthService);

    if (startTime <= 0)
      return false;

    return startTime + recheckTime <= CurrentTime.getCurrentTime();
  }
  
  private long getStartTime(HealthSubSystem healthService)
  {
    switch (getStatus()) {
    case WARNING:
      return healthService.getWarningStartTime(getHealthCheck());
      
    case CRITICAL:
      return healthService.getCriticalStartTime(getHealthCheck());
      
    case FATAL:
      return healthService.getFatalStartTime(getHealthCheck());
      
    default:
      return -1;
    }
  }
  
  private int getRecheckCount(HealthSubSystem healthService)
  {
    switch (getStatus()) {
    case WARNING:
      return healthService.getWarningCount(getHealthCheck());
      
    case CRITICAL:
      return healthService.getCriticalCount(getHealthCheck());
      
    case FATAL:
      return healthService.getFatalCount(getHealthCheck());
      
    default:
      return -1;
    }
  }
}
