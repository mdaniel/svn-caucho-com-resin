/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.health.event.HealthEvent;

public abstract class HealthPredicateBase implements HealthPredicate
{
  /**
   * The abstract match returns true, so a child can combine its own
   * matches with the parent. See AbstractScheduledCheckPredicate for a
   * case where the parent <code>isMatch</code> doesn't return true.
   */
  @Override
  public boolean isMatch(HealthEvent event)
  {
    return true;
  }
  
  @Override
  public String toString()
  {
    return this.getClass().getSimpleName() + "[]";
  }
}
