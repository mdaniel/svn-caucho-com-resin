/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import io.baratine.service.Startup;

import javax.inject.Singleton;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.env.health.HealthCheckResult;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.health.HealthActionResult.ResultStatus;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.warning.WarningSystem;
import com.caucho.v5.util.L10N;

/**
 * Health action to send health status to the Resin watchdog.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:WarnWatchdog>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:IfRechecked/>
 * </health:WarnWatchdog>
 * }</pre>
 *
 */
@Startup
@Singleton
@Configurable
public class WarnWatchdog extends HealthActionBase
{
  private static final L10N L = new L10N(WarnWatchdog.class);

  private WarningSystem _warningService;
  
  public WarnWatchdog()
  {
    _warningService = SystemManager.getCurrentSystem(WarningSystem.class);
    if (_warningService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          WarnWatchdog.class.getSimpleName(),
                                          WarningSystem.class.getSimpleName()));
    }
  }

  @Override
  public HealthActionResult doActionImpl(HealthEvent healthEvent)
    throws Exception
  {
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    HealthCheckResult summaryResult = healthService.getSummaryResult();
    _warningService.sendWarning(healthService, L.l("Health {0}", 
                                                   summaryResult.getDescription()));
    
    return new HealthActionResult(ResultStatus.OK, 
                                  L.l("Warning sent to Watchdog: {0}",
                                      summaryResult.getDescription()));
  }
}
