/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.action;

import com.caucho.v5.env.health.HealthActionResult;
import com.caucho.v5.health.event.HealthEvent;

public interface HealthAction
{
  public HealthActionResult doAction(HealthEvent healthEvent);
  
  public void start();
  
  public void stop();
}
