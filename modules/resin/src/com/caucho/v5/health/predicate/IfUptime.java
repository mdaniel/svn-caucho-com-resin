/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.subsystem.SystemManager;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Qualifies an action to match an amount of time after startup.
 * <p>
 * <pre>{@code
 * <health:Restart>
 *   <health:IfUptime after="12h"/>
 * </health:Restart> 
 * }</pre>
 *
 */
@Startup
@Configurable
public class IfUptime extends HealthPredicateScheduledBase
{
  private static final L10N L = new L10N(IfUptime.class);

  private long _after = -1;
  
  @PostConstruct
  public void init()
  {
    if (_after < 0)
      throw new ConfigException(L.l("<health:{0}> requires 'after' attribute",
                                    getClass().getSimpleName()));
  }

  @Configurable
  public void setAfter(Period period)
  {
    _after = period.getPeriod();
  }
  
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    long et = CurrentTime.getCurrentTime() - SystemManager.getCurrent().getStartTime();
    
    return (_after >= 0 && _after < et);
  }
  
  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "[" + _after + "]";
  }
}
