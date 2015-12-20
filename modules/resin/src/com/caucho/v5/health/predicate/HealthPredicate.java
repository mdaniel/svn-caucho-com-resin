/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.predicate;

import com.caucho.v5.health.event.HealthEvent;

public interface HealthPredicate
{
  public boolean isMatch(HealthEvent healthEvent);
}
