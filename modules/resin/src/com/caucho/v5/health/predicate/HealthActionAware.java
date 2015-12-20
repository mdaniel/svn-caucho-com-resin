package com.caucho.v5.health.predicate;

import com.caucho.v5.health.action.HealthAction;

public interface HealthActionAware
{
  public void setAction(HealthAction action);
}
