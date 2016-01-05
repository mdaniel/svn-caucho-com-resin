/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.util.L10N;

/**
 * Matches if the child predicate is false.
 * <p>
 * Complex tests can be built using &lt;health:Not>,
 * &lt;health:And> and &lt;helth:Or> on top of simpler primary
 * predicates. 
 * <p> 
 * <pre>{@code
 * <health:HttpStatusHealthCheck ee:Named="httpStatusCheck">
 *   <url>http://localhost:8080/test-ping.jsp</url>
 * </health:HttpStatusHealthCheck>
 * 
 * <health:Restart>
 *   <health:IfHealthCritical healthCheck="${httpStatusCheck}"/>
 *   <health:Not>
 *     <health:IfRecovered/>
 *   </health:Not>
 * </health:Restart>
 * }</pre>
 *
 */
@Startup
@Configurable
public class Not extends HealthPredicateBase
{
  private static final L10N L = new L10N(Not.class);
  
  private HealthPredicate _predicate;
  
  public Not()
  {
    
  }
  
  public Not(HealthPredicate predicate)
  {
    add(predicate);
  }

  /**
   * Add a child predicate.  The child must fail for Not to pass.
   *
   * @param predicate the child predicate
   */
  public void add(HealthPredicate predicate)
  {
    if (_predicate != null)
      throw new ConfigException(L.l("<health:{0}> requires a single value",
                                    this.getClass().getSimpleName()));
      
    _predicate = predicate;
  }

  @PostConstruct
  public void init()
  {
    if (_predicate == null)
      throw new ConfigException(L.l("<health:{0}> requires a child predicate",
                                    this.getClass().getSimpleName()));
  }

  /**
   * True if child predicate does not match.
   *
   * @param request the servlet request to test
   */
  @Override
  public boolean isMatch(HealthEvent healthEvent)
  {
    return ! _predicate.isMatch(healthEvent);
  }
}
