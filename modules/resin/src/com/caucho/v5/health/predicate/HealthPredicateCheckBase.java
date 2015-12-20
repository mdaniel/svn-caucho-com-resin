/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.config.*;
import com.caucho.v5.env.health.*;
import com.caucho.v5.health.check.HealthCheck;
import com.caucho.v5.util.L10N;

@Configurable
public abstract class HealthPredicateCheckBase 
  extends HealthPredicateScheduledBase
{
  private static final L10N L = new L10N(HealthPredicateCheckBase.class);
  
  private HealthCheck _healthCheck;
  
  public HealthPredicateCheckBase()
  {
  }
  
  public HealthPredicateCheckBase(HealthCheck healthCheck)
  {
    setHealthCheck(healthCheck);
  }

  /**
   * Set health check - optional, if not present summary status will be used
   */
  @Configurable 
  public void setHealthCheck(HealthCheck healthCheck)
  {
    // XXX:
    if (false && healthCheck == null) {
      throw new ConfigException(L.l("referenced health check not found"));
    }
    
    
    _healthCheck = healthCheck;
  }
  
  public HealthCheck getHealthCheck()
  {
    return _healthCheck;
  }
  
//  @SuppressWarnings("unused")
//  @PostConstruct
//  public void init()
//  {
//    HealthService healthService = HealthService.getCurrent();
//    
//    if (_healthCheck != null && healthService != null) {
//      if (! healthService.containsHealthCheck(_healthCheck))
//        log.warning(this + " healthCheck='" + _healthCheck + "' is an unknown health check"); 
//    }
//  }
  
  protected HealthCheckResult getLastResult(HealthSubSystem healthService)
  {
    if (_healthCheck != null)
      return _healthCheck.getLastResult(healthService);
    else
      return healthService.getSummaryResult();
  }
 
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _healthCheck + "]";
  }
}
