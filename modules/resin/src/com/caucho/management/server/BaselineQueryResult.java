/*
 * Copyright (c) 1998-2011 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.management.server;

public class BaselineQueryResult implements java.io.Serializable
{
  private static final long serialVersionUID = -4695711746534650807L;
  
  private String _desc;
  private int _sampleSize;
  private double _value;

  public BaselineQueryResult()
  {
    
  }
  
  public BaselineQueryResult(String desc, int sampleSize, double value)
  {
    _desc = desc;
    _sampleSize = sampleSize;
    _value = value;
  }

  public int getSampleSize()
  {
    return _sampleSize;
  }

  public void setSampleSize(int sampleSize)
  {
    _sampleSize = sampleSize;
  }

  public double getValue()
  {
    return _value;
  }

  public void setValue(double value)
  {
    _value = value;
  }

  public String getDesc()
  {
    return _desc;
  }

  public void setDesc(String desc)
  {
    _desc = desc;
  }
}
