/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import com.caucho.v5.health.event.HealthEvent;

/**
 * Matches if all of the child predicates fail.
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
 *   <health:Nand>
 *     <health:IfHealthOk healthCheck="${httpCheck1}"/>
 *     <health:IfHealthOk healthCheck="${httpCheck2}"/>
 *   </health:Nand>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Configurable
public class Nand extends HealthPredicateCombiningBase
{
  /**
   * Matches children with logical NAND operation
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    for (HealthPredicate predicate : getPredicates()) {
      if (! predicate.isMatch(healthEvent))
        return true;
    }

    return false;
  }
}
