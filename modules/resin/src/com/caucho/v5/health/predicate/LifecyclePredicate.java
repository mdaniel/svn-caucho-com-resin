/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.lifecycle.LifecycleListener;
import com.caucho.v5.lifecycle.LifecycleState;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
public abstract class LifecyclePredicate 
  extends EventPredicate 
  implements LifecycleListener
{
  private static final L10N L = new L10N(LifecyclePredicate.class);

  @PostConstruct
  public void init()
  {
    SystemManager resinSystem = SystemManager.getCurrent();
    if (resinSystem == null) 
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          this.getClass().getSimpleName(),
                                          SystemManager.class.getSimpleName()));
    
    resinSystem.addLifecycleListener(this);
    
    super.init();
  }
  
  @Override
  public void lifecycleEvent(LifecycleState oldState, LifecycleState newState)  
  {
    if (isMatch(_healthService, oldState, newState))
      doAction();
  }
  
  public abstract boolean isMatch(HealthSubSystem healthService, 
                                  LifecycleState oldState, 
                                  LifecycleState newState);
}
