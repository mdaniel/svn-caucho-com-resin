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
 * Qualifies an action to match unknown results.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthUnknown healthCheck="${httpStatusCheck}"/>
 * </health:Restart> 
 * }</pre>
 *
 */
@Configurable
public class IfHealthUnknown extends IfHealthStatus
{
  public IfHealthUnknown()
  {
    super(HealthStatus.UNKNOWN);
  }
  
  public IfHealthUnknown(HealthCheck healthCheck)
  {
    super(healthCheck, HealthStatus.UNKNOWN);
  }
}
