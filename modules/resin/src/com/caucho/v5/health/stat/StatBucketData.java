/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

/**
 * bucket for a section of data.
 */
@SuppressWarnings("serial")
public final class StatBucketData implements java.io.Serializable
{
  private int _count;
  private double _sum;
  private double _min = Double.MAX_VALUE;
  private double _max= Double.MIN_VALUE;
  
  public void add(double value)
  {
    _count++;
    _sum += value;
    
    if (value < _min)
      _min = value;
    
    if (_max < value)
      _max = value;
  }
  
  public int getCount()
  {
    return _count;
  }
  
  public double getSum()
  {
    return _sum;
  }
  
  public double getAverage()
  {
    int count = _count;
    
    if (count == 0)
      return 0;
    else
      return _sum / count;
  }
  
  public double getMin()
  {
    return _min;
  }
  
  public double getMax()
  {
    return _max;
  }
  

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
