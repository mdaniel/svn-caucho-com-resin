/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.server.admin;

import java.io.Serializable;

/**
 * Factory for creating statistics attributes.
 */
@SuppressWarnings("serial")
public class AddSample implements Serializable
{
  private final long _deltaTime;
  private final long []_ids;
  private final double []_values;
  
  @SuppressWarnings("unused")
  private AddSample()
  {
    _deltaTime = 0;
    _ids = null;
    _values = null;
  }
  
  public AddSample(long deltaTime, long []ids, double []values)
  {
    _deltaTime = deltaTime;
    _ids = ids;
    _values = values;
  }

  public long getDeltaTime()
  {
    return _deltaTime;
  }

  public long []getIds()
  {
    return _ids;
  }

  public double []getValues()
  {
    return _values;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
