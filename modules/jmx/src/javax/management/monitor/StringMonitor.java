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
 * Management interface of a string.  The counter
 * monitor continually checks the value of a string attribute.
 */
public class StringMonitor extends Monitor implements StringMonitorMBean {
  private boolean _notifyDiffer;
  private boolean _notifyMatch;
  private String _stringToCompare = "";
  private long _sequence;

  /**
   * Zero-arg constructor.
   */
  public StringMonitor()
  {
  }
  
  /**
   * Returns the derived gauge
   */
  public String getDerivedGauge(ObjectName name)
  {
    ObjectValue value = getObjectValue(name);

    if (value == null)
      return null;
    else
      return (String) value.getValue();
  }
  
  /**
   * Returns the string to compare.
   */
  public String getStringToCompare()
  {
    return _stringToCompare;
  }
  
  /**
   * Sets the string to compare.
   */
  public void setStringToCompare(String value)
    throws IllegalArgumentException
  {
    _stringToCompare = value;
  }
  
  /**
   * Returns the notify flag
   */
  public boolean getNotifyMatch()
  {
    return _notifyMatch;
  }
  
  /**
   * Sets the notify flag
   */
  public void setNotifyMatch(boolean value)
    throws IllegalArgumentException
  {
    _notifyMatch = value;
  }
  
  /**
   * Returns the notify flag
   */
  public boolean getNotifyDiffer()
  {
    return _notifyDiffer;
  }
  
  /**
   * Sets the notify flag
   */
  public void setNotifyDiffer(boolean value)
    throws IllegalArgumentException
  {
    _notifyDiffer = value;
  }
  
  /**
   * Returns the derived gauge
   *
   * @deprecated
   */
  public String getDerivedGauge()
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

  protected void checkUpdate(ObjectValue value, Object newValue)
  {
    String matchType = null;

    if (_stringToCompare.equals(newValue)) {
      if (_notifyMatch)
	matchType = MonitorNotification.STRING_TO_COMPARE_VALUE_MATCHED;
    }
    else {
      if (_notifyDiffer)
	matchType = MonitorNotification.STRING_TO_COMPARE_VALUE_DIFFERED;
    }

    if (matchType == null)
      return;

    MonitorNotification notif;

    notif = new MonitorNotification(matchType, getName(), _sequence,
				    JobManager.getCurrentTime(),
				    "change message",
				    value.getName(),
				    getObservedAttribute(),
				    "gauge", "trigger");

    sendNotification(notif);
  }
}

  
