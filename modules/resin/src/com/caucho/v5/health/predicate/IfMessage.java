/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import java.util.regex.Pattern;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.health.*;
import com.caucho.v5.health.event.HealthEvent;

/**
 * Qualifies an action to match health result message to a regular expression.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical/>
 *   <health:IfMessage healthCheck="${httpStatusCheck}" regexp="Exception"/>
 * <health:Restart>
 * }</pre>
 *
 */
@Configurable
public class IfMessage extends HealthPredicateCheckBase
{
  protected Pattern _regexp = Pattern.compile(".*");
  
  public IfMessage()
  {
    
  }
  
  public IfMessage(Pattern regexp)
  {
    setRegexp(regexp);
  }
  
  /**
   * Set health message match regular expression
   */
  @Configurable
  public void setRegexp(Pattern regexp)
  {
    _regexp = regexp;
  }

  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    if (! super.isMatch(healthEvent))
      return false;
    
    HealthSubSystem healthService = healthEvent.getHealthSystem();
    
    HealthCheckResult result = getLastResult(healthService);
    return result != null && _regexp.matcher(result.getMessage()).find();
  }
}
