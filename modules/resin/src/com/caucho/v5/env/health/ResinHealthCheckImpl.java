/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.env.health;

import com.caucho.v5.health.check.AbstractHealthCheck;
import com.caucho.v5.util.L10N;

/**
 * Top-level health check summary for all of Resin.
 */
public class ResinHealthCheckImpl extends AbstractHealthCheck
{
  public static final String NAME = "Resin";

  private static final L10N L = new L10N(ResinHealthCheckImpl.class);

  private final HealthSubSystem _healthService;
  
  public ResinHealthCheckImpl() 
  {
    _healthService = HealthSubSystem.getCurrent();
    
    if (_healthService == null) {
      throw new IllegalStateException(L.l("{0} requires an active {1}",
                                          ResinHealthCheckImpl.class.getSimpleName(),
                                          HealthSubSystem.class.getSimpleName()));
    }
  }
  
  @Override
  public HealthCheckResult checkHealth()
  {
    return _healthService.getSummaryResult();
  }
  
  @Override
  public String getName()
  {
    return NAME;
  }
}
