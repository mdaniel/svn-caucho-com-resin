/*
 * Copyright (c) 1998-2002 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.management.monitor;

import javax.management.ObjectName;

import com.caucho.jmx.JobManager;

/**
 * Management interface of a counter monitor.  The counter
 * monitor continually checks the value of a numeric attribute.
 */
public class CounterMonitor extends Monitor implements CounterMonitorMBean {
  private boolean _differenceMode;
  private Number _modulus = new Long(Long.MAX_VALUE);
  private boolean _notify;
  private Number _offset = new Long(0);
  private Number _threshold = new Long(-1);

  private long _sequence;

  /**
   * Zero-arg construtor
   */
  public CounterMonitor()
  {
  }
  
  /**
   * Returns the derived gauge
   */
  public Number getDerivedGauge(ObjectName name)
  {
    CounterValue value = (CounterValue) getObjectValue(name);

    if (value == null)
      return null;
    else if (_differenceMode)
      return new Long(value.getDifferenceValue());
    else
      return new Long(value.getCounterValue());
  }
  
  /**
   * Returns the derived gauge timestamp
   */
  public long getDerivedGaugeTimeStamp(ObjectName name)
  {
    CounterValue value = (CounterValue) getObjectValue(name);
    
    if (value == null)
      return 0;
    else
      return value.getTimestamp();
  }
  
  /**
   * Returns the threshold value
   */
  public Number getThreshold(ObjectName name)
  {
    CounterValue value = (CounterValue) getObjectValue(name);
    
    return new Long(value.getCounterValue());
  }
  
  /**
   * Returns the threshold value
   */
  public Number getInitThreshold()
  {
    return _threshold;
  }
  
  /**
   * Sets the threshold value
   */
  public void setInitThreshold(Number value)
    throws IllegalArgumentException
  {
    if (value == null)
      throw new IllegalArgumentException("setInitThreshold cannot be null");
    
    _threshold = value;
  }
  
  /**
   * Sets the threshold value
   */
  public void setThreshold(Number value)
    throws IllegalArgumentException
  {
    setThreshold(value);
  }
  
  /**
   * Returns the offset value
   */
  public Number getOffset()
  {
    return _offset;
  }
  
  /**
   * Sets the offset value
   */
  public void setOffset(Number value)
    throws IllegalArgumentException
  {
    if (value == null)
      throw new IllegalArgumentException("offset cannot be null");
    
    _offset = value;
  }
  
  /**
   * Returns the modulus value
   */
  public Number getModulus()
  {
    return _modulus;
  }
  
  /**
   * Sets the modulus value
   */
  public void setModulus(Number value)
    throws IllegalArgumentException
  {
    if (value == null)
      throw new IllegalArgumentException("modulus cannot be null");
    
    _modulus = value;
  }
  
  /**
   * Returns the notify flag
   */
  public boolean getNotify()
  {
    return _notify;
  }
  
  /**
   * Sets the notify flag
   */
  public void setNotify(boolean value)
    throws IllegalArgumentException
  {
    _notify = value;
  }
  
  /**
   * Returns the differenceMode flag
   */
  public boolean getDifferenceMode()
  {
    return _differenceMode;
  }
  
  /**
   * Sets the differenceMode flag
   */
  public void setDifferenceMode(boolean value)
    throws IllegalArgumentException
  {
    _differenceMode = value;
  }
  
  /**
   * Returns the derived gauge
   */
  public Number getDerivedGauge()
  {
    return getDerivedGauge(getObservedObject());
  }
  
  /**
   * Returns the threshold
   */
  public Number getThreshold()
  {
    return getThreshold(getObservedObject());
  }
  
  /**
   * Returns the derived gauge timestamp
   */
  public long getDerivedGaugeTimeStamp()
  {
    return getDerivedGaugeTimeStamp(getObservedObject());
  }

  /**
   * Returns the object value.
   */
  protected ObjectValue createObjectValue(ObjectName name, Object initValue)
  {
    long counterValue = ((Number) initValue).longValue();

    CounterValue value = new CounterValue(name);
    value.setValue(initValue);
    value.setTimestamp(JobManager.getCurrentTime());
    value.setCounterValue(counterValue);
    value.setDifferenceValue(0);

    return value;
  }

  /**
   * Updates the value.
   */
  protected void checkUpdate(ObjectValue objValue, Object newValue)
  {
    CounterValue counter = (CounterValue) objValue;

    Number numberValue = (Number) newValue;
    long value = 0;
    if (numberValue != null)
      value = numberValue.longValue();

    long oldValue = counter.getCounterValue();
    long oldDifference = counter.getDifferenceValue();

    long difference = value - oldValue;

    counter.setCounterValue(value);
    counter.setDifferenceValue(value - oldValue);

    if (! _notify)
      return;

    String matchType = null;
    
    if (oldValue < _threshold.longValue() &&
	_threshold.longValue() <= value) {
      matchType = MonitorNotification.THRESHOLD_VALUE_EXCEEDED;
    }

    if (matchType == null)
      return;
    
    MonitorNotification notif;

    notif = new MonitorNotification(matchType, getName(), _sequence++,
				    JobManager.getCurrentTime(),
				    "change message",
				    objValue.getName(),
				    getObservedAttribute(),
				    "gauge", "trigger");

    sendNotification(notif);
  }

  static class CounterValue extends ObjectValue {
    private long _counterValue;
    private long _differenceValue;

    CounterValue(ObjectName name)
    {
      super(name);
    }

    void setCounterValue(long counterValue)
    {
      _counterValue = counterValue;
    }

    long getCounterValue()
    {
      return _counterValue;
    }

    void setDifferenceValue(long differenceValue)
    {
      _differenceValue = differenceValue;
    }

    long getDifferenceValue()
    {
      return _differenceValue;
    }
  }
}

  
