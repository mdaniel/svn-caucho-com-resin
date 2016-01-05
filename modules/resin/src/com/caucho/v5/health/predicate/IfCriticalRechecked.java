/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.env.health.HealthStatus;
import com.caucho.v5.health.check.HealthCheck;

import io.baratine.config.Configurable;

/**
 * Qualifies an action to match only after the required number of rechecks
 * have been performed.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfCriticalRechecked healthCheck="${httpStatusCheck}" count="5"/>
 * </health:Restart> 
 * }</pre>
 *
 * @see com.caucho.v5.health.HealthSystem
 *
 */
@Configurable
public class IfCriticalRechecked extends IfHealthStatus
{
  public IfCriticalRechecked()
  {
    super(HealthStatus.CRITICAL);
    
    setSystemRecheckTime(true);
  }
  
  public IfCriticalRechecked(HealthCheck healthCheck)
  {
    super(healthCheck, HealthStatus.CRITICAL);
    
    setSystemRecheckTime(true);
  }
  
  public IfCriticalRechecked(HealthCheck healthCheck, long recheckTime)
  {
    super(healthCheck, HealthStatus.CRITICAL);
    
    setTimeMillis(recheckTime);
  }
}
