/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.core.Startup;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.health.event.HealthEvent;

/**
 * Matches if all of the child predicates match.
 * <p>
 * Note: 'And' is the default and thus not strictly necessary in the 
 * example below.
 * <p>
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpCheck1">
 *   <url>http://localhost:8080/check1.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:HttpStatusHealthCheck ee:Named="httpCheck2">
 *   <url>http://localhost:8080/check2.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:And>
 *     <health:IfHealthCritical healthCheck="${httpCheck1}"/>
 *     <health:IfHealthCritical healthCheck="${httpCheck2}"/>
 *   </health:And>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Configurable
public class And extends HealthPredicateCombiningBase
{
  /**
   * Matches children with logical AND operation
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    for (HealthPredicate predicate : getPredicates()) {
      if (! predicate.isMatch(healthEvent))
        return false;
    }
  
    return true;
  }
}
