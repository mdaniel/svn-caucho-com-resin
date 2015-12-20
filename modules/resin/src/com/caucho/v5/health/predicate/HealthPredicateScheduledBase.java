/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.health.event.HealthEvent;
import com.caucho.v5.health.event.ScheduledCheckHealthEvent;

/**
 * HealthPredicate that only matches during a scheduled event.
 */
public abstract class HealthPredicateScheduledBase
  extends HealthPredicateBase
{
  @Override
  public boolean isMatch(HealthEvent event)
  {
    return (event instanceof ScheduledCheckHealthEvent);
  }
}
