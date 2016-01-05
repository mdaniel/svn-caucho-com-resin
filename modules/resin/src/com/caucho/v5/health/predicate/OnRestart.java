/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.RestartHealthEvent;

/**
 * Qualifies an action to match only when Resin is restarted by the watchdog.  
 * This generally only occurs during an error condition.  OnStart will fire 
 * during this event also.
 * <p>
 * <pre>{@code
 * <health:SendMail mail="${healthMailer}">
 *   <to>admin@yourdomain.com</to>
 *   <health:OnRestart/>
 * </health:SendMail> 
 * }</pre>
 *
 */
@Startup
@Configurable
public class OnRestart extends HealthPredicateBase
{
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (healthEvent instanceof RestartHealthEvent)
      return true;
    
    return false;
  }
}
