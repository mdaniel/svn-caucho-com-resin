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
 * Qualifies an action to match OK health status.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <mail name="healthMailer">
 *   <from>resin@yourdomain.com</from>
 *   <smtp-host>localhost</smtp-host>
 *   <smtp-port>25</smtp-port>
 * </mail>
 * 
 * <health:SendMail mail="${healthMailer}">
 *   <to>admin@yourdomain.com</to>
 *   <to>another_admin@yourdomain.com</to>
 *   <health:IfHealthOk healthCheck="${httpStatusCheck}"/>
 * </health:SendMail>
 * }</pre>
 *
 */
@Configurable
public class IfHealthOk extends IfHealthStatus
{
  public IfHealthOk()
  {
    super(HealthStatus.OK);
  }
  
  public IfHealthOk(HealthCheck healthCheck)
  {
    super(healthCheck, HealthStatus.OK);
  }
}
