/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.shutdown.ShutdownSystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;

/**
 * Health action to trigger a timed restart of Resin, normally used in
 * conjunction with an ActionSequence to gather shutdown information.
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
public class FailSafeRestart extends HealthActionBase
{
  private static final L10N L = new L10N(FailSafeRestart.class);

  private ShutdownSystem _shutdownSystem;
  private long _timeout = -1;
  
  public FailSafeRestart()
  {
    _shutdownSystem = SystemManager.getCurrentSystem(ShutdownSystem.class);
    
    if (_shutdownSystem == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          FailSafeRestart.class.getSimpleName(),
                                          ShutdownSystem.class.getSimpleName()));
    }
  }
  
  /**
   * Time to force a restart if one has not yet occurred
   * @param timeout as a Period
   */
  @Configurable
  public void setTimeout(Period timeout)
  {
    setTimeoutMillis(timeout.getPeriod());
  }
  
  /**
   * Time to force a restart if one has not yet occurred
   * @param timeout in milliseconds
   */
  @Configurable
  public void setTimeoutMillis(long timeout)
  {
    _timeout = timeout;
  }
  
  public long getTimeoutMillis()
  {
    return _timeout;
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    _shutdownSystem.startFailSafeShutdown(L.l("starting fail-safe restart from health"),
                                              _timeout);
    return new HealthActionResult(ResultStatus.OK, 
                                  L.l("Started fail-safe restart with timeout {0}",
                                      _timeout));
  }
}
