/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import io.baratine.config.Configurable;
import io.baratine.service.Startup;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import com.caucho.v5.config.ConfigException;
import com.caucho.v5.health.action.HealthAction;
import com.caucho.v5.util.L10N;

@Startup
@Configurable
public abstract class HealthPredicateCombiningBase 
  extends HealthPredicateBase
  implements HealthActionAware
{
  private static final L10N L = new L10N(HealthPredicateCombiningBase.class);

  private List<HealthPredicate> _predicates
    = new ArrayList<HealthPredicate>();
  
  @PostConstruct
  public void init()
  {
    if (_predicates.isEmpty())
      throw new ConfigException(L.l("<health:{0}> requires child predicates",
                                    getClass().getSimpleName()));
  }

  @Override
  public void setAction(HealthAction action)
  {
    for (HealthPredicate predicate : _predicates) {
      if (predicate instanceof HealthActionAware) {
        ((HealthActionAware)predicate).setAction(action);
      }
    }
  }

  @Configurable
  public void add(HealthPredicate predicate)
  {
    _predicates.add(predicate);
  }
  
  public List<HealthPredicate> getPredicates()
  {
    return _predicates;
  }
}
