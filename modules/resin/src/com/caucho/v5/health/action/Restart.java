/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.core.Startup;

import javax.inject.Singleton;

import com.caucho.v5.amp.spi.ShutdownModeAmp;
import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.util.L10N;

/**
 * Health action to trigger a restart of Resin.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class Restart extends HealthActionBase
{
  private static final L10N L = new L10N(Restart.class);

  private ShutdownSystem _shutdownSystem;
  
  public Restart()
  {
    _shutdownSystem = SystemManager.getCurrentSystem(ShutdownSystem.class);
    
    if (_shutdownSystem == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          Restart.class.getSimpleName(),
                                          ShutdownSystem.class.getSimpleName()));
    }
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    HealthCheckResult summaryResult = healthService.getSummaryResult();
    
    String message = L.l("Restart with health {0}", 
                         summaryResult.getDescription());

    _shutdownSystem.shutdown(ShutdownModeAmp.IMMEDIATE, ExitCode.HEALTH, message);
    
    return new HealthActionResult(ResultStatus.OK, message);
  }
}
