/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.management.server;

import java.beans.ConstructorProperties;

/**
 * Value for a statistics data point.
 */
public class StatServiceValue implements java.io.Serializable
{
  private final long _time;
  private final int _count;
  private final double _sum;
  private final double _min;
  private final double _max;

  /**
   * Null constructor for Hessian
   */
  private StatServiceValue()
  {
    _time = 0;
    _count = 0;
    _sum = 0;
    _min = 0;
    _max = 0;
  }

  /**
   * Standard constructor
   */
  @ConstructorProperties({"time", "count", "sum", "min", "max"})
  public StatServiceValue(long time, 
                          int count,
                          double sum,
                          double min,
                          double max)
  {
    _time = time;
    _count = count;
    _sum = sum;
    _min = min;
    _max = max;
  }
  
  public StatServiceValue(long time, double value)
  {
    _time = time;
    _count = 1;
    _sum = value;
    _min = value;
    _max = value;
  }

  /**
   * Returns the value's time
   */
  public long getTime()
  {
    return _time;
  }

  /**
   * Returns the value's value
   */
  public int getCount()
  {
    return _count;
  }

  /**
   * Returns the value's value
   */
  public double getSum()
  {
    return _sum;
  }

  /**
   * Returns the value's value
   */
  public double getAverage()
  {
    if (_count > 0)
      return _sum / _count;
    else
      return _sum;
  }

  /**
   * Returns the value's minimum
   */
  public double getMin()
  {
    return _min;
  }

  /**
   * Returns the value's maximum
   */
  public double getMax()
  {
    return _max;
  }

  /**
   * Returns the value's average
   */
  public double getValue()
  {
    return getAverage();
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + _time + "," + getValue() + "]";
  }
}
