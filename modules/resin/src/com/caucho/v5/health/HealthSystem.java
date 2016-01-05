/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.inject.Singleton;

import com.caucho.v5.config.types.Period;
import com.caucho.v5.env.health.HealthSubSystem;
import com.caucho.v5.env.system.SystemManager;
import com.caucho.v5.server.container.ServerBase;
import com.caucho.v5.util.L10N;

/**
 * Configures the check frequency and recheck rules of the health checking system.
 * <p>
 * <pre>{@code
 * <!-- These are the default values -->
 * <health:HealthSystem>
 *   <enabled>true</enabled>
 *   <startup-delay>15m</startup-delay>
 *   <period>5m</period>
 *   <recheck-period>30s</recheck-period>
 *   <recheck-max>10</recheck-max>
 * </health:HealthSystem>
 * }</pre>
 * 
 */
@Startup
@Singleton
@Configurable
public class HealthSystem
{
  private static final L10N L = new L10N(HealthSystem.class);

  private HealthSubSystem _healthService;
  
  public HealthSystem()
  {
    ServerBase resin = ServerBase.current();

    _healthService = SystemManager.getCurrentSystem(HealthSubSystem.class);
    if (_healthService == null && resin == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          getClass().getSimpleName(),
                                          HealthSubSystem.class.getSimpleName()));
    }
  } 
  
  /**
   * Set all health checking enabled or disabled
   */
  @Configurable
  public void setEnabled(boolean enabled)
  {
    _healthService.setEnabled(enabled);
  }
  
  /**
   * Set the time after startup before actions are triggered
   * <p>
   * delay deprecates initialSleepTime
   */
  @Configurable
  public void setStartupDelay(Period delay)
  {
    _healthService.setDelay(delay.getPeriod());
  }
  
  /**
   * Set the time after startup before actions are triggered
   * <p>
   * delay deprecates initialSleepTime
   */
  @Configurable
  public void setDelay(Period delay)
  {
    setStartupDelay(delay);
  }
  
  /**
   * Sets the time between checks
   * <p>
   * period deprecates sleepTime
   */
  @Configurable
  public void setPeriod(Period period)
  {
    _healthService.setPeriod(period.getPeriod());
  }
  
  /**
   * Sets the time between rechecks
   * <p>
   * recheckPeriod deprecates retryTime
   */
  @Configurable 
  public void setRecheckPeriod(Period recheckPeriod)
  {
    _healthService.setRecheckPeriod(recheckPeriod.getPeriod());
  }
  
  /**
   * Sets the number of rechecks before returning to the normal checking period
   * <p>
   * recheckMax deprecates retryCount
   */
  @Configurable 
  public void setRecheckMax(int recheckMax)
  {
    _healthService.setRecheckMax(recheckMax);
  }
  
  /**
   * Sets the check timeout.
   * <p>
   * recheckPeriod deprecates retryTime
   */
  @Configurable 
  public void setCheckTimeout(Period period)
  {
    _healthService.setCheckTimeout(period.getPeriod());
  }
}
