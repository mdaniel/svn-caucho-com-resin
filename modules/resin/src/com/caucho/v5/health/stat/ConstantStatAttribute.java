/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import com.caucho.v5.env.meter.MeterBase;

/**
 * Statistics gathering attribute.  Each time period, the attribute is polled.
 */
public class ConstantStatAttribute extends MeterBase
{
  private final double _value;

  ConstantStatAttribute(String name, double value)
  {
    super(name);

    _value = value;
  }
  
  @Override
  public void sample()
  {
  }

  /**
   * Polls the statistics attribute.
   */
  @Override
  public double calculate()
  {
    return _value;
  }
}
