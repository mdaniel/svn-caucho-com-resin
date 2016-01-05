/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.*;
import com.caucho.v5.health.check.HealthCheck;

/**
 * Qualifies an action to match warning health status.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthWarning healthCheck="${httpStatusCheck}"/>
 * </health:Restart> 
 * }</pre>
 *
 */
@Configurable
public class IfHealthWarning extends IfHealthStatus
{
  public IfHealthWarning()
  {
    super(HealthStatus.WARNING);
  }
  
  public IfHealthWarning(HealthCheck healthCheck)
  {
    super(healthCheck, HealthStatus.WARNING);
  }
}
