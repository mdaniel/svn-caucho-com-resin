/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.action.HealthAction;
import com.caucho.v5.health.action.HealthActionBase;
import com.caucho.v5.health.action.HealthActionListener;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.CurrentTime;
import com.caucho.v5.util.L10N;

/**
 * Qualifies an action to match at most an amount of time after the last execution.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:DumpHeap>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfNotRecent time='5m'/>
 * </health:DumpHeap>
 * }</pre>
 * 
 */
@Startup
@Configurable
public class IfNotRecent 
  extends HealthPredicateBase 
  implements HealthActionAware, HealthActionListener
{
  private static final L10N L = new L10N(IfNotRecent.class);

  private long _time = -1;
  
  private long _lastExecutionTime = -1;
  
  @PostConstruct
  public void init()
  {
    if (_time < 0)
      throw new ConfigException(L.l("<health:{0}> requires 'time' attribute",
                                    getClass().getSimpleName()));
  }
  
  @Override
  public void setAction(HealthAction action)
  {
    if (action instanceof HealthActionBase) // create eventsource interface?
      ((HealthActionBase)action).addListener(this);
  }

  /**
   * The time before the action can execute again.
   * @param time
   */
  @Configurable
  public void setTime(Period time)
  {
    _time = time.getPeriod();
  }
  
  @Override
  public void beforeAction(HealthAction action, HealthSubSystem healthService)
  {
  }

  @Override
  public void afterAction(HealthAction action, HealthSubSystem healthService)
  {
    _lastExecutionTime = CurrentTime.getCurrentTime();
  }
  
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    return (CurrentTime.getCurrentTime() - _lastExecutionTime) > _time;
  }
  
  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "[" + _time + "]";
  }
}
