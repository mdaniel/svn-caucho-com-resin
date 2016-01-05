/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.event.HealthEvent;

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
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:Restart> 
 * }</pre>
 *
 * @see com.caucho.v5.health.HealthSystem
 *
 */
@Configurable
public class IfRechecked extends HealthPredicateScheduledBase
{
  public IfRechecked()
  {
    
  }
  
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    return healthService.getCurrentRecheckCount() >= healthService.getRecheckMax();
  }
}
