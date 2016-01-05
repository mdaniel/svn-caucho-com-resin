/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.StopHealthEvent;
import com.caucho.v5.health.shutdown.ExitCode;
import com.caucho.v5.health.shutdown.ShutdownSystem;
import com.caucho.v5.util.L10N;

/**
 * Qualifies an action to match only when Resin is stopping with an non-OK
 * exit code.
 * <p>
 * <pre>{@code
 * <health:Snapshot>
 *   <health:OnAbnormalStop/>
 * </health:Snapshot 
 * }</pre>
 *
 */
@Startup
@Configurable
public class OnAbnormalStop extends OnStop
{
  private static final L10N L = new L10N(OnAbnormalStop.class);

  private ShutdownSystem _shutdownSystem;
  
  private List<ExitCode> _normalCodes = new ArrayList<ExitCode>();
  
  @PostConstruct
  public void init()
  {
    _shutdownSystem = ShutdownSystem.getCurrent(); 
    if (_shutdownSystem == null) 
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          this.getClass().getSimpleName(),
                                          ShutdownSystem.class.getSimpleName()));
    
    if (_normalCodes.isEmpty()) {
      _normalCodes.add(ExitCode.OK);
      _normalCodes.add(ExitCode.MODIFIED);
      _normalCodes.add(ExitCode.WATCHDOG_EXIT);
    }
  }
  
  @Configurable
  public void addNormalExitCode(ExitCode exitCode)
  {
    _normalCodes.add(exitCode);
  }
  
  @Override
  public boolean isMatch(HealthEvent event)
  {
    if (! (event instanceof StopHealthEvent))
      return false;
    
    ExitCode exitCode = _shutdownSystem.getExitCode();
    
    return (exitCode != null && ! _normalCodes.contains(exitCode));
    
  }
}
