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
 * Matches if none of the child predicates match.
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
 *   <health:Nor>
 *     <health:IfHealthOk healthCheck="${httpCheck1}"/>
 *     <health:IfHealthOk healthCheck="${httpCheck2}"/>
 *   </health:Nor>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Configurable
public class Nor extends HealthPredicateCombiningBase
{
  /**
   * Matches children with logical NOR operation
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    for (HealthPredicate predicate : getPredicates()) {
      if (predicate.isMatch(healthEvent))
        return false;
    }

    return true;
  }
}
