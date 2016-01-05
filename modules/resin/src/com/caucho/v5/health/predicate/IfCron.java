/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigArg;
import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.CronType;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Matches if the current time is in an active range configured by 
 * cron-style times.  This can be used both to schedule regular actions or 
 * to prevent restarts or other actions during critical times. 
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 *   <health:Not>
 *     <health:IfCron>
 *       <enable-at>0 17 * * *</enable-at>
 *       <disable-at>0 18 * * *</disable-at>
 *     </health:IfCron>
 *   </health:Not>
 * </health:Restart> 
 * }</pre>
 *
 */
@Startup
@Configurable
public class IfCron extends HealthPredicateScheduledBase
{
  private static final L10N L = new L10N(IfCron.class);
  
  private CronType _enableAt;
  private CronType _disableAt;
  
  private final AtomicLong _lastTime = new AtomicLong();

  /**
   * Sets the cron enable times.
   */
  @Configurable
  public void setEnableAt(CronType enableAt)
  {
    _enableAt = enableAt;
  }
  
  @ConfigArg(0)
  public void setValue(CronType enableAt)
  {
    setEnableAt(enableAt);
  }

  /**
   * Sets the cron disable times.
   */
  @Configurable
  public void setDisableAt(CronType disableAt)
  {
    _disableAt = disableAt;
  }

  @PostConstruct
  public void init()
  {
    if (_enableAt == null)
      throw new ConfigException(L.l("<health:{0}> requires 'enable-at' attribute",
                                    getClass().getSimpleName()));

    /*
    if (_disableAt == null)
      throw new ConfigException(L.l("<health:{0}> requires 'disable-at' attribute",
                                    getClass().getSimpleName()));
                                    */
  }

  /**
   * True if the predicate matches.
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    long now = CurrentTime.getCurrentTime();

    if (_disableAt != null) {
      long prevEnable = _enableAt.prevTime(now);
      long prevDisable = _disableAt.prevTime(now);
    
      return prevEnable > 0 && prevDisable <= prevEnable;
    }
    else {
      long lastTime = _lastTime.getAndSet(now);
      
      long nextEnable = _enableAt.nextTime(lastTime);
      
      return (lastTime > 0 
              && lastTime < now
              && nextEnable <= now);
    }
  }
}
