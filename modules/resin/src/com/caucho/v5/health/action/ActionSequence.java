/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.service.Startup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.predicate.HealthPredicateBase;
import com.caucho.v5.util.L10N;

/**
 * Sequence of health actions.
 * <p>
 * <pre>{@code
 * <health:ActionSequence>
 *   <health:FailSafeRestart timeout="10m">
 *   <health:DumpThreads/>
 *   <health:DumpHeap/>
 *   <health:StartProfiler active-time="5m"/>
 *   <health:Restart/>
 *   
 *   <health:IfHealthCritical time="5m"/>
 * </health:ActionSequence>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class ActionSequence extends HealthActionBase
{
  private static final L10N L = new L10N(ActionSequence.class);

  public ArrayList<HealthAction> _actionList = new ArrayList<HealthAction>();
  
  private AtomicReference<Thread> _actionThread = new AtomicReference<Thread>();
  
  @PostConstruct
  public void init()
  {
    if (false && _actionList.isEmpty()) {
      throw new ConfigException(L.l("<health:{0}> requires at least 1 child action",
                                    getClass().getSimpleName()));
    }
    
    super.init();
  }
  
  public void add(HealthAction action)
  {
    if (action instanceof HealthActionBase)
      ((HealthActionBase)action).add(new IfExecuting());
    
    _actionList.add(action);
  }
  
  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
  {
    _actionThread.set(Thread.currentThread());
    
    Map<HealthAction, HealthActionResult> results = 
      new HashMap<HealthAction, HealthActionResult>();
    
    try {
      for (HealthAction action : _actionList) {
        HealthActionResult result = null;
        try {
          result = action.doAction(healthEvent);
        } catch (Exception e) {
          result = new HealthActionResult(ResultStatus.FAILED, 
                                          L.l("{0} execution failed: {1}", 
                                              this, e.toString()));
          result.setException(e);
        }
        
        results.put(action, result);
      }
    } finally {
      _actionThread.set(null);
    }
    
    ResultStatus overallStatus = ResultStatus.OK;
    StringBuilder sb = new StringBuilder();
    
    for(Map.Entry<HealthAction, HealthActionResult> entry : results.entrySet()) {
      HealthActionResult result = entry.getValue();
      if (result.getStatus().compareTo(overallStatus) > 0)
        overallStatus = result.getStatus();
      
      sb.append(entry.getKey().toString());
      sb.append(" ");
      sb.append(result.getDescription());
      sb.append("\n");
    }
    
    return new HealthActionResult(overallStatus, sb.toString());
  }
  
  // This prevents extra executions of the child actions except when executed 
  // as a result of the action sequence.
  // currentThread checking is necessary because of the possibility of  
  // concurrent execution from an EventPredicate
  protected class IfExecuting extends HealthPredicateBase 
  {
    @Override
    public boolean isMatch(HealthEvent healthEvent)
    {
      Thread t = _actionThread.get();
      return t != null && t.equals(Thread.currentThread());
    }
  }
}
