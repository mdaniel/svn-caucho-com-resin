/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.event;

import com.caucho.v5.env.health.HealthSubSystem;

public class HealthEvent
{
  private HealthSubSystem _healthSystem;
  private String _eventName;
  
  public HealthEvent(HealthSubSystem healthSystem,
                     String eventName)
  {
    _healthSystem = healthSystem;
    _eventName = eventName;
  }
  
  public HealthSubSystem getHealthSystem()
  {
    return _healthSystem;
  }
  
  public String getEventName()
  {
    return _eventName;
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _eventName + "]";
  }
}
