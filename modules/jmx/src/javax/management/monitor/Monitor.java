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

import java.util.ArrayList;
import java.util.TimerTask;
import java.util.Date;

import java.util.logging.Logger;
import java.util.logging.Level;

import javax.management.ObjectName;
import javax.management.MBeanServer;
import javax.management.NotificationBroadcasterSupport;
import javax.management.MBeanRegistration;
import javax.management.MBeanException;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;

import com.caucho.jmx.JobManager;

/**
 * Implementation of the monitor interface.
 */
abstract public class Monitor extends NotificationBroadcasterSupport
  implements MonitorMBean, MBeanRegistration, java.io.Serializable {
  private final static Logger log = Logger.getLogger(Monitor.class.getName());
  
  /**
   * flag for the alreadyNotified.
   */
  final static protected int RESET_FLAGS_ALREADY_NOTIFIED = 0x1;
  /**
   * flag for the alreadyNotified.
   */
  final static protected int OBSERVED_OBJECT_ERROR_NOTIFIED = 0x2;
  /**
   * flag for the alreadyNotified.
   */
  final static protected int OBSERVED_ATTRIBUTE_ERROR_NOTIFIED = 0x4;
  /**
   * flag for the alreadyNotified.
   */
  final static protected int OBSERVED_ATTRIBUTE_TYPE_ERROR_NOTIFIED = 0x8;
  /**
   * flag for the alreadyNotified.
   */
  final static protected int RUNTIME_ERROR_NOTIFIED = 0x8;
  
  /**
   * The monitor's owning server.
   */
  protected MBeanServer server;

  /**
   * The name.
   */
  private ObjectName _name;
  
  /**
   * Records which have been notified.
   */
  protected int _alreadyNotified;

  /**
   * The granularity period
   */
  private long _granularityPeriod = 10000L;

  /**
   * The observed object
   */
  private ArrayList _objects = new ArrayList();
  private ArrayList _runValues = new ArrayList();

  /**
   * The observed attribute
   */
  private String _attribute;

  /**
   * True if active.
   */
  private boolean _isActive;

  /**
   * The job task.
   */
  private Job _job;

  /**
   * Zero-arg constructor
   */
  public Monitor()
  {
    _job = new Job(this);
  }
  
  /**
   * Start the monitor.
   */
  public void start()
  {
    if (_isActive)
      return;

    _isActive = true;

    _job.run();
  }
  
  /**
   * Stops the monitor.
   */
  public void stop()
  {
    if (! _isActive)
      return;

    _isActive = false;

    JobManager.dequeue(_job);
  }
  
  /**
   * Adds an observed object.
   *
   * @param name the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public void addObservedObject(ObjectName name)
  {
    try {
      Object newValue = this.server.getAttribute(name, _attribute);
      ObjectValue value = createObjectValue(name, newValue);
      
      synchronized (_objects) {
	_objects.add(value);
      }
    } catch (MBeanException e) {
      sendNotification(MonitorNotification.OBSERVED_OBJECT_ERROR,
		       String.valueOf(e), name);
    } catch (InstanceNotFoundException e) {
      sendNotification(MonitorNotification.OBSERVED_OBJECT_ERROR,
		       String.valueOf(e), name);
    } catch (ReflectionException e) {
      sendNotification(MonitorNotification.OBSERVED_OBJECT_ERROR,
		       String.valueOf(e.getCause()), name);
    } catch (AttributeNotFoundException e) {
      sendNotification(MonitorNotification.OBSERVED_ATTRIBUTE_ERROR,
		       String.valueOf(e), name);
    }
  }

  protected ObjectValue createObjectValue(ObjectName name, Object initValue)
  {
    ObjectValue value = new ObjectValue(name);
    value.setValue(initValue);
    value.setTimestamp(JobManager.getCurrentTime());

    return value;
  }

  private void sendNotification(String type, String msg, ObjectName name)
  {
    MonitorNotification notif;

    notif = new MonitorNotification(MonitorNotification.OBSERVED_OBJECT_ERROR,
				    _name, 0, new Date().getTime(),
				    msg, name, _attribute, null, null);
    sendNotification(notif);
  }
  
  /**
   * Removes an observed object.
   *
   * @param object the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public void removeObservedObject(ObjectName name)
  {
    synchronized (_objects) {
      for (int i = _objects.size() - 1; i >= 0; i--) {
	ObjectValue value = (ObjectValue) _objects.get(i);

	if (value.getName().equals(name)) {
	  _objects.remove(i);
	  return;
	}
      }
    }
  }
  
  /**
   * Returns true if the object is observed.
   *
   * @param object the object name of the observed object.
   *
   * @return the name of the observed object
   */
  public boolean containsObservedObject(ObjectName name)
  {
    synchronized (_objects) {
      for (int i = _objects.size() - 1; i >= 0; i--) {
	ObjectValue value = (ObjectValue) _objects.get(i);

	if (value.getName().equals(name))
	  return true;
      }
    }
    
    return false;
  }
  
  /**
   * Returns the observed objects.
   *
   * @return the name of the observed object
   */
  public ObjectName []getObservedObjects()
  {
    ObjectName []values = new ObjectName[_objects.size()];

    synchronized (_objects) {
      for (int i = _objects.size() - 1; i >= 0; i--) {
	values[i] = ((ObjectValue) _objects.get(i)).getName();
      }
    }
    
    return values;
  }
  
  /**
   * Returns the observed attribute
   *
   * @return the name of the observed attribute
   */
  public String getObservedAttribute()
  {
    return _attribute;
  }
  
  /**
   * Sets the attribute to observe.
   *
   * @param name the name of the observed attribute
   */
  public void setObservedAttribute(String name)
  {
    _attribute = name;
  }
  
  /**
   * Gets the period of observation in milliseconds.
   *
   * @return the granularity period
   */
  public long getGranularityPeriod()
  {
    return _granularityPeriod;
  }
  
  /**
   * Sets the period of observation in milliseconds.
   *
   * @param period the granularity period
   */
  public void setGranularityPeriod(long period)
  {
    _granularityPeriod = period;
  }
  
  /**
   * Tests if the monitor is active
   *
   * @return true if the monitor is active.
   */
  public boolean isActive()
  {
    return _isActive;
  }

  protected ObjectName getName()
  {
    return _name;
  }
  
  /**
   * Called before the registration.
   *
   * @param server the mbean server to be registered
   * @param name the client's name to be registered
   *
   * @return the name the object wans the be registered as
   */
  public ObjectName preRegister(MBeanServer server, ObjectName name)
    throws Exception
  {
    this.server = server;
    _name = name;
    
    return name;
  }
  
  /**
   * Called after the registration.
   *
   * @param registrationDone true if the registration was successful.
   */
  public void postRegister(Boolean registrationDone)
  {
    // XXX: register timer
  }
  
  /**
   * Called before deregistration.
   */
  public void preDeregister()
    throws Exception
  {
    stop();
  }
  
  /**
   * Called after the deregistration.
   */
  public void postDeregister()
  {
  }
  
  /**
   * Returns the observed object.
   *
   * @return the name of the observed object
   */
  public ObjectName getObservedObject()
  {
    if (_objects.size() > 0)
      return ((ObjectValue) _objects.get(0)).getName();
    else
      return null;
  }
  
  /**
   * Sets the observed object.
   *
   * @param name the name of the observed object
   */
  public void setObservedObject(ObjectName name)
  {
    addObservedObject(name);
  }

  /**
   * Executes the task.
   */
  void run()
  {
    _runValues.clear();

    synchronized (_objects) {
      _runValues.addAll(_objects);
    }

    for (int i = 0; i < _runValues.size(); i++) {
      ObjectValue value = (ObjectValue) _runValues.get(i);
      Object oldValue = value.getValue();

      try {
	Object newValue = this.server.getAttribute(value.getName(), _attribute);	value.setValue(newValue);

	if (oldValue == newValue)
	  continue;
	else if (oldValue != null && oldValue.equals(newValue))
	  continue;

	value.setTimestamp(JobManager.getCurrentTime());

	checkUpdate(value, newValue);
      } catch (Throwable e) {
	log.log(Level.WARNING, e.toString(), e);
      }
    }
  }

  protected ObjectValue getObjectValue(ObjectName name)
  {
    ObjectValue value = null;

    synchronized (_objects) {
      for (int i = _runValues.size() - 1; i >= 0; i--) {
	ObjectValue testValue = (ObjectValue) _runValues.get(i);

	if (testValue.getName().equals(name)) {
	  value = testValue;
	  break;
	}
      }
    }

    return value;
  }

  /**
   * Returns the time the gauge changed.
   */
  public long getDerivedGaugeTimeStamp(ObjectName name)
  {
    synchronized (_objects) {
      for (int i = _runValues.size() - 1; i >= 0; i--) {
	ObjectValue testValue = (ObjectValue) _runValues.get(i);

	if (testValue.getName().equals(name)) {
	  return testValue.getTimestamp();
	}
      }
    }

    return 0;
  }

  protected void checkUpdate(ObjectValue value, Object newValue)
  {
  }

  static class ObjectValue {
    private ObjectName _name;
    private Object _value;
    private long _timestamp;

    ObjectValue(ObjectName name)
    {
      _name = name;
    }

    /**
     * Returns the object name.
     */
    ObjectName getName()
    {
      return _name;
    }

    /**
     * Returns the object value.
     */
    Object getValue()
    {
      return _value;
    }

    /**
     * Sets the object value.
     */
    void setValue(Object value)
    {
      _value = value;
    }

    /**
     * Sets the timestamp.
     */
    void setTimestamp(long time)
    {
      _timestamp = time;
    }

    /**
     * Gets the timestamp.
     */
    long getTimestamp()
    {
      return _timestamp;
    }
  }

  static class Job extends TimerTask {
    private Monitor _monitor;

    Job(Monitor monitor)
    {
      _monitor = monitor;
    }
    
    public void run()
    {
      try {
	_monitor.run();
      } finally {
	try {
	  if (_monitor.isActive()) {
	    JobManager.queueRelative(this, _monitor.getGranularityPeriod());
	  }
	} catch (Throwable e) {
	  log.log(Level.WARNING, e.toString(), e);
	}
      }
    }
  }
}

  
