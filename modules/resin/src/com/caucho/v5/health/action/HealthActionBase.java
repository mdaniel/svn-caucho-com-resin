/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.service.Startup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.predicate.HealthActionAware;
import com.caucho.v5.health.predicate.HealthPredicate;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
public abstract class HealthActionBase implements HealthAction
{
  private static final Logger log 
    = Logger.getLogger(HealthAction.class.getName());
  private static final L10N L = new L10N(HealthActionBase.class);
  
  private List<HealthPredicate> _predicates
    = new CopyOnWriteArrayList<HealthPredicate>();
  
  private List<HealthActionListener> _listeners
    = new ArrayList<HealthActionListener>();
  
  public HealthActionBase()
  {
  }
  
  @Configurable
  public void add(HealthPredicate predicate)
  {
    if (predicate == null)
      throw new ConfigException(L.l("referenced health predicate not found"));
    
    _predicates.add(predicate);
    
    if (predicate instanceof HealthActionAware)
      ((HealthActionAware)predicate).setAction(this);
  }
  
  public List<HealthPredicate> getPredicates()
  {
    return _predicates;
  }
  
  public void addListener(HealthActionListener listener)
  {
    _listeners.add(listener);
  }
  
  @PostConstruct
  public void init()
  {
    HealthSubSystem healthService = HealthSubSystem.getCurrent();
    if (healthService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                           this.getClass().getSimpleName(),
                                           HealthSubSystem.class.getSimpleName()));
    }
    
    healthService.addHealthAction(this);
  }
  
  @Override
  public final HealthActionResult doAction(HealthEvent healthEvent)
  {
    for (HealthActionListener listener : _listeners)
      listener.beforeAction(this, healthEvent.getHealthSystem());

    for (HealthPredicate predicate : _predicates) {
      if (! predicate.isMatch(healthEvent)) {
        return new HealthActionResult(ResultStatus.SKIPPED);
      }
    }
    
    HealthActionResult result = null;
    
    try {
      result = doActionImpl(healthEvent);
    } catch (Exception e) {
      result = new HealthActionResult(ResultStatus.FAILED, 
                                      L.l("Execution failed: {0}", 
                                          e.toString()));
      result.setException(e);
    }
    
    for (HealthActionListener listener : _listeners)
      listener.afterAction(this, healthEvent.getHealthSystem());
    
    return result;
  }
  
  public abstract HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception;
  
  @Override
  public void start()
  {
  }
  
  @Override
  public void stop()
  {
  }

  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
