package com.caucho.v5.health.action;

import com.caucho.v5.env.health.HealthSubSystem;

public interface HealthActionListener
{
  public void beforeAction(HealthAction action, HealthSubSystem healthService);
  
  public void afterAction(HealthAction action, HealthSubSystem healthService);
}
