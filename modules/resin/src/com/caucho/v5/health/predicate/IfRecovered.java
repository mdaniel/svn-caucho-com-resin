/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.*;
import com.caucho.v5.health.event.HealthEvent;

/**
 * Qualifies an action to match upon recovery of Resin health.  Recovery is
 * defined as the state change from CRITICAL or WARNING to OK.
 * <p>
 * <pre>{@code
 * <health:SendMail mail="${healthMailer}">
 *   <to>admin@yourdomain.com</to>
 *   <to>another_admin@yourdomain.com</to>
 *   <health:IfRecovered/>
 * </health:SendMail>
 * }</pre>
 *
 */
@Configurable
public class IfRecovered extends HealthPredicateCheckBase
{
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    HealthCheckResult result = getLastResult(healthService);
    return result != null && result.isRecover();
  }
}
