/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.event;

import com.caucho.v5.env.health.HealthSubSystem;

public class ScheduledCheckHealthEvent extends HealthEvent
{
  public static final String EVENT_NAME = "caucho.health.scheduled.check";
  
  public ScheduledCheckHealthEvent(HealthSubSystem healthSystem)
  {
    super(healthSystem, EVENT_NAME);
  }
}
