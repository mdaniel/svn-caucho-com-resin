/*
 * Copyright (c) 1998-2003 Caucho Technology -- all rights reserved
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
 * Management interface of a gauge monitor.  The gauge
 * monitor continually checks the value of a numeric attribute.
 */
public class GaugeMonitor extends Monitor implements GaugeMonitorMBean {
  private boolean _differenceMode;
  private boolean _notifyLow;
  private boolean _notifyHigh;
  
  private Number _lowThreshold = new Double(Double.MIN_VALUE);
  private Number _highThreshold = new Double(Double.MAX_VALUE);

  private long _sequence;

  /**
   * Default constructor.
   */
  public GaugeMonitor()
  {
  }
  
  /**
   * Returns the derived gauge
   */
  public Number getDerivedGauge(ObjectName name)
  {
    GaugeValue value = (GaugeValue) getObjectValue(name);

    if (value == null)
      return null;
    else if (_differenceMode)
      return new Double(value.getDifferenceValue());
    else
      return new Double(value.getGaugeValue());
  }

  /**
   * Returns the upper threshold for the MBean.
   */
  public Number getHighThreshold()
  {
    return _highThreshold;
  }

  /**
   * Returns the lower threshold for the MBean.
   */
  public Number getLowThreshold()
  {
    return _lowThreshold;
  }

  /**
   * Sets the upper threshold for the MBean.
   */
  public void setThresholds(Number highValue, Number lowValue)
    throws IllegalArgumentException
  {
    if (highValue == null || lowValue == null)
      throw new IllegalArgumentException();

    if (highValue.doubleValue() < lowValue.doubleValue())
      throw new IllegalArgumentException();

    _lowThreshold = lowValue;
    _highThreshold = highValue;
  }

  /**
   * Gets the notification
   */
  public boolean getNotifyHigh()
  {
    return _notifyHigh;
  }

  /**
   * Sets the notification
   */
  public void setNotifyHigh(boolean value)
    throws IllegalArgumentException
  {
    _notifyHigh = value;
  }

  /**
   * Sets the notification
   */
  public boolean getNotifyLow()
  {
    return _notifyLow;
  }

  /**
   * Sets the notification
   */
  public void setNotifyLow(boolean value)
    throws IllegalArgumentException
  {
    _notifyLow = value;
  }

  /**
   * Gets the difference mode flag.
   */
  public boolean getDifferenceMode()
  {
    return _differenceMode;
  }

  /**
   * Sets the difference mode flag.
   */
  public void setDifferenceMode(boolean value)
    throws IllegalArgumentException
  {
    _differenceMode = value;
  }

  /**
   * Returns the derived gauge
   *
   * @deprecated
   */
  public Number getDerivedGauge()
  {
    return getDerivedGauge(getObservedObject());
  }
  
  /**
   * Returns the derived gauge timestamp
   *
   * @deprecated
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
    double gaugeValue = ((Number) initValue).longValue();

    GaugeValue value = new GaugeValue(name);
    value.setValue(initValue);
    value.setTimestamp(JobManager.getCurrentTime());
    value.setGaugeValue(gaugeValue);
    value.setDifferenceValue(0);

    value.setMatchValue((_lowThreshold.doubleValue() +
			 _highThreshold.doubleValue()) / 2);

    return value;
  }

  /**
   * Updates the value.
   */
  protected void checkUpdate(ObjectValue objValue, Object newValue)
  {
    GaugeValue gauge = (GaugeValue) objValue;

    Number numberValue = (Number) newValue;
    double value = 0;
    if (numberValue != null)
      value = numberValue.longValue();

    double oldValue = gauge.getGaugeValue();
    double oldDifference = gauge.getDifferenceValue();

    double difference = value - oldValue;

    gauge.setGaugeValue(value);
    gauge.setDifferenceValue(difference);

    double lowThreshold = _lowThreshold.doubleValue();
    double highThreshold = _highThreshold.doubleValue();

    String matchType = null;
    
    if (_differenceMode) {
      if (difference < lowThreshold &&
	  (lowThreshold < gauge.getMatchValue() ||
	   highThreshold == gauge.getMatchValue())) {
	gauge.setMatchValue(lowThreshold);
	
	if (_notifyLow)
	  matchType = MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED;
      }
      else if (highThreshold < difference &&
	       (gauge.getMatchValue() < highThreshold ||
		lowThreshold == gauge.getMatchValue())) {
	gauge.setMatchValue(highThreshold);
	
	if (_notifyHigh)
	  matchType = MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED;
      }
    }
    else {
      if (value < lowThreshold &&
	  (lowThreshold < gauge.getMatchValue() ||
	   highThreshold == gauge.getMatchValue())) {
	gauge.setMatchValue(lowThreshold);
	
	if (_notifyLow)
	  matchType = MonitorNotification.THRESHOLD_LOW_VALUE_EXCEEDED;
      }
      else if (highThreshold < value &&
	       (gauge.getMatchValue() < highThreshold ||
		lowThreshold == gauge.getMatchValue())) {
	gauge.setMatchValue(highThreshold);
	
	if (_notifyHigh)
	  matchType = MonitorNotification.THRESHOLD_HIGH_VALUE_EXCEEDED;
      }
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

  static class GaugeValue extends ObjectValue {
    private double _gaugeValue;
    private double _differenceValue;
    private double _matchValue;

    GaugeValue(ObjectName name)
    {
      super(name);
    }

    void setGaugeValue(double gaugeValue)
    {
      _gaugeValue = gaugeValue;
    }

    double getGaugeValue()
    {
      return _gaugeValue;
    }

    void setDifferenceValue(double differenceValue)
    {
      _differenceValue = differenceValue;
    }

    double getDifferenceValue()
    {
      return _differenceValue;
    }

    void setMatchValue(double matchValue)
    {
      _matchValue = matchValue;
    }

    double getMatchValue()
    {
      return _matchValue;
    }
  }
}

  
