/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.health.action.HealthAction;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
public abstract class EventPredicate 
  extends HealthPredicateBase
  implements HealthActionAware
{
  private static final Logger log 
    = Logger.getLogger(EventPredicate.class.getName());
  private static final L10N L = new L10N(EventPredicate.class);

  protected HealthSubSystem _healthService;
  
  private HealthAction _action;
  private Thread _eventThread = null;
  
  @PostConstruct
  public void init()
  {
    _healthService = HealthSubSystem.getCurrent();
    if (_healthService == null) 
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          this.getClass().getSimpleName(),
                                          HealthSubSystem.class.getSimpleName()));
  }
  
  @Override
  public void setAction(HealthAction action)
  {
    _action = action;
  }

  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (_eventThread != null && Thread.currentThread().equals(_eventThread))
      return true;
    
    return false;
  }

  protected void doAction()
  {
    _eventThread = Thread.currentThread();
    
    try {
      // _action.doAction(_healthService); 
    } catch (Throwable e) {
      log.log(Level.WARNING, L.l("{0} failed: {1}",
                                 _action, e.toString()), e);
    }
    
    _eventThread = null;
  }
}
