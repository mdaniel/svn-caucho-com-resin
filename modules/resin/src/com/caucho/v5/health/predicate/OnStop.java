/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.StopHealthEvent;

/**
 * Qualifies an action to match only when Resin is stopping.
 * <p>
 * <pre>{@code
 * <health:SendMail mail="${healthMailer}">
 *   <to>admin@yourdomain.com</to>
 *   <health:OnStop/>
 * </health:SendMail> 
 * }</pre>
 *
 */
@Startup
@Configurable
public class OnStop extends HealthPredicateBase
{
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (healthEvent instanceof StopHealthEvent)
      return true;
    
    return false;
  }
}
