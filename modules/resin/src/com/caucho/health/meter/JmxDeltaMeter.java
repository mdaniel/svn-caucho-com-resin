/*
 * Copyright (c) 1998-2014 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.health.meter;

import io.baratine.core.Startup;

import com.caucho.config.Configurable;
import com.caucho.env.meter.MeterService;

@Startup
@Configurable
public class JmxDeltaMeter extends JmxMeter
{  
  @Override
  protected void createMeter()
  {
    MeterService.createJmxDelta(getName(),
                                getObjectName(),
                                getAttribute(),
                                isOptional());
  }
}
