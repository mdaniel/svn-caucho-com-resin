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

import javax.management.Notification;
import javax.management.ObjectName;

/**
 * Implementation of the monitor interface.
 */
public class MonitorNotification extends Notification {
  public static final String OBSERVED_OBJECT_ERROR =
    "jmx.monitor.error.mbean";
  public static final String OBSERVED_ATTRIBUTE_ERROR =
    "jmx.monitor.error.attribute";
  public static final String OBSERVED_ATTRIBUTE_TYPE_ERROR =
    "jmx.monitor.error.type";
  public static final String THRESHOLD_ERROR =
    "jmx.monitor.error.threshold";
  public static final String RUNTIME_ERROR =
    "jmx.monitor.error.runtime";
  public static final String THRESHOLD_VALUE_EXCEEDED =
    "jmx.monitor.counter.threshold";
  public static final String THRESHOLD_HIGH_VALUE_EXCEEDED =
    "jmx.monitor.gauge.high";
  public static final String THRESHOLD_LOW_VALUE_EXCEEDED =
    "jmx.monitor.gauge.low";
  public static final String STRING_TO_COMPARE_VALUE_MATCHED =
    "jmx.monitor.string.matches";
  public static final String STRING_TO_COMPARE_VALUE_DIFFERED =
    "jmx.monitor.string.differs";

  private Object _derivedGauge;
  private String _observedAttribute;
  private ObjectName _observedObject;
  private Object _trigger;
  
  protected MonitorNotification(String type, Object source,
				long sequenceNumber, long timeStamp,
				String message,
				ObjectName object, String attribute,
				Object gauge, Object trigger)
  {
    super(type, source, sequenceNumber, timeStamp, message);
        
    _observedObject = object;
    _observedAttribute = attribute;
    _derivedGauge = gauge;
    _trigger = trigger;
  }

  /**
   * Returns the observed object for the monitor.
   */
  public ObjectName getObservedObject()
  {
    return _observedObject;
  }

  /**
   * Returns the observed attribute for the monitor.
   */
  public String getObservedAttribute()
  {
    return _observedAttribute;
  }

  /**
   * Returns the derived gauge.
   */
  public Object getDerivedGauge()
  {
    return _derivedGauge;
  }

  /**
   * Returns the trigger object
   */
  public Object getTrigger()
  {
    return _trigger;
  }
}

  
