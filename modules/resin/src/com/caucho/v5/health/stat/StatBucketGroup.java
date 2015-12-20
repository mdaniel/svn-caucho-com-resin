/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.health.stat;

/**
 * a group of data divided into buckets.
 */
@SuppressWarnings("serial")
public final class StatBucketGroup implements java.io.Serializable
{
  private final long _id;
  
  private final long _timeMin;
  private final long _timeMax;
  private final long _timeInterval;
  
  private final int _bucketCount;
  
  private final StatBucketData []_data;
  
  public StatBucketGroup(long id, long timeMin, long timeMax, long interval)
  {
    _id = id;
    _timeMin = timeMin;
    _timeMax = timeMax;
    _timeInterval = interval;
    
    if (_timeMax <= _timeMin)
      throw new IllegalArgumentException();
    
    if (interval <= 0)
      throw new IllegalArgumentException();

    _bucketCount = (int) ((_timeMax - _timeMin + interval - 1) / interval);
    
    _data = new StatBucketData[_bucketCount];
    
    for (int i = 0; i < _bucketCount; i++) {
      _data[i] = new StatBucketData();
    }
  }
  
  public long getTimeMin()
  {
    return _timeMin;
  }
  
  public long getTimeMax()
  {
    return _timeMax;
  }
  
  public long getTimeInterval()
  {
    return _timeInterval;
  }
  
  public StatBucketData []getBuckets()
  {
    return _data;
  }
  
  public void add(long time, double value)
  {
    if (time < _timeMin)
      return;
    
    int bucketIndex = (int) ((time - _timeMin) / _timeInterval);
    
    if (_bucketCount <= bucketIndex)
      return;
    
    StatBucketData bucket = _data[bucketIndex];
    
    bucket.add(value);
  }

  public double getAverage(long timeMin, long timeMax)
  {
    int count = 0;
    double sum = 0;
    
    if (timeMin < _timeMin)
      timeMin = _timeMin;
    
    if (_timeMax < timeMax)
      timeMax = _timeMax;
    
    long timeInterval = _timeInterval;
    
    StatBucketData []buckets = _data;
    for (long time = timeMin; time < timeMax; time += timeInterval) {
      StatBucketData bucket = buckets[(int) ((time - timeMin) / timeInterval)];
      
      count += bucket.getCount();
      sum += bucket.getSum();
    }
    
    if (count <= 0) {
      return Double.NaN;
    }

    return sum / count;
  }

  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + Long.toHexString(_id) + "]";
  }
}
