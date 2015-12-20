/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.event;

import com.caucho.v5.env.health.HealthSubSystem;

public class StartHealthEvent extends HealthEvent
{
  public static final String EVENT_NAME = "caucho.health.start";
  
  public StartHealthEvent(HealthSubSystem healthSystem)
  {
    super(healthSystem, EVENT_NAME);
  }
}
