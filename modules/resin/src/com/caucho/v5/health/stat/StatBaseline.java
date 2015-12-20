/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

import java.util.HashMap;

/**
 * all the baseline data
 */
@SuppressWarnings("serial")
public final class StatBaseline implements java.io.Serializable
{
  private final HashMap<Long,StatBucketGroup> _groupMap
    = new HashMap<Long,StatBucketGroup>();
  
  private final long _timeMin;
  private final long _timeMax;
  private final long _timeInterval;
  
  public StatBaseline(long timeMin, long timeMax, long timeInterval)
  {
    _timeMin = timeMin;
    _timeMax = timeMax;
    _timeInterval = timeInterval;
  }

  public long getTimeMin()
  {
    return _timeMin;
  }
  
  public void add(long id, long time, double value)
  {
    StatBucketGroup group = _groupMap.get(id);
    
    if (group == null) {
      group = new StatBucketGroup(id, _timeMin, _timeMax, _timeInterval);
      
      _groupMap.put(id, group);
    }
    
    group.add(time, value);
  }
  
  public double getAverage(long id, long timeMin, long timeMax)
  {
    StatBucketGroup group = _groupMap.get(id);

    if (group == null) {
      return Double.NaN;
    }
    
    return group.getAverage(timeMin, timeMax);
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[]";
  }
}
