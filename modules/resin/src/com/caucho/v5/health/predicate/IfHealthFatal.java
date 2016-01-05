/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.env.health.*;
import com.caucho.v5.health.check.HealthCheck;

import io.baratine.config.Configurable;

/**
 * Qualifies an action to match critical health status.
 * <p>
 * <pre>{@code
 * <health:Restart>
 *   <health:IfHealthFatal/>
 * </health:Restart> 
 * }</pre>
 *
 */
@Configurable
public class IfHealthFatal extends IfHealthStatus
{
  public IfHealthFatal()
  {
    super(HealthStatus.FATAL);
  }
  
  public IfHealthFatal(HealthCheck healthCheck)
  {
    super(healthCheck, HealthStatus.CRITICAL);
  }
}
