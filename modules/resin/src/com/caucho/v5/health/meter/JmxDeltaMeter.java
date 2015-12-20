/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.meter;

import io.baratine.core.Startup;

import com.caucho.v5.config.Configurable;
import com.caucho.v5.env.meter.MeterService;

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
