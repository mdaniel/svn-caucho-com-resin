/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
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
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.mbeans.j2ee;

import com.caucho.jmx.Jmx;
import com.caucho.jmx.IntrospectionMBean;
import com.caucho.server.host.Host;
import com.caucho.server.webapp.Application;
import com.caucho.util.Alarm;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class management interface for all managed objects.
 */
abstract public class J2EEManagedObject {
  private static final Logger log
    = Logger.getLogger(J2EEManagedObject.class.getName());

  private static final String[] CONTEXT_KEYS = {
    "J2EEServer",
    "Host",
    "J2EEApplication",
    "WebModule"
  };

  private final long _startTime;

  protected ObjectName _objectName;

  public J2EEManagedObject()
  {
    _startTime = Alarm.getCurrentTime();
  }

  public String getObjectName()
  {
    return createObjectName().getCanonicalName();
  }

  ObjectName createObjectName()
  {
    if (_objectName == null) {
      Hashtable<String,String> properties = new Hashtable<String, String>();

      try {
        _objectName = createObjectName(properties);
      }
      catch (MalformedObjectNameException ex) {
        if (log.isLoggable(Level.FINE)) {
          StringBuilder builder = new StringBuilder();

          builder.append('\'');
          for (Map.Entry<String,String> entry : properties.entrySet()) {
            if (builder.length() > 0)
              builder.append(',');

            builder.append(entry.getKey());
            builder.append('=');
            builder.append(entry.getValue());
          }

          builder.append("' ");
          builder.append(ex.toString());

          log.log(Level.FINE, builder.toString(), ex);
        }
      }
    }

    return _objectName;
  }

  /**
   * Returns the value to use for the the `name' key of the
   * ObjectName. The returned value is raw, users of the method must escape
   * the returned value for use in an ObjectName.
   */
  abstract protected String getName();

  /**
   * Returns true if the ObjectName should include the J2EEServer key.
   * The default implementation returns true,
   * derived class override to return false if there should not be a
   * J2EEServer key.
   */
  protected boolean isJ2EEServer()
  {
    return true;
  }

  /**
   * Returns true if the ObjectName should include the J2EEApplication key.
   * The default implementation returns true,
   * derived class override to return false if there should not be a
   * J2EEApplication key.
   */
  protected boolean isJ2EEApplication()
  {
    return true;
  }

  protected String quote(String value)
  {
    if (value == null)
      return "null";
    else if (value.length() == 0)
      return "default";
    else {
      for (int i = 0; i < value.length(); i++) {
        char ch = value.charAt(i);

        switch (ch) {
          case ',':
          case '=':
          case '?':
          case '"':
          case ':':
            return ObjectName.quote(value);
        }
      }

      return value;
    }
  }

  /**
   * Creates the object name.
   */
  protected ObjectName createObjectName(Hashtable<String,String> properties)
    throws MalformedObjectNameException
  {
    Application application = Application.getLocal();

    if (application != null) {
      String contextPath = application.getContextPath();

      if (contextPath == null || contextPath.length() == 0)
        contextPath = "/";

      properties.put("WebModule", quote(contextPath));
    }

    Host host = Host.getLocal();

    if (isJ2EEApplication()) {
      J2EEApplication j2eeApplication = J2EEApplication.getLocal();

      if (j2eeApplication == null)
        properties.put("J2EEApplication", quote("null"));
      else
        properties.put("J2EEApplication", quote(j2eeApplication.getName()));
    }

    if (host != null)
      properties.put("Host", quote(host.getName()));

    if (isJ2EEServer()) {
      J2EEServer j2eeServer = J2EEServer.getLocal();

      if (j2eeServer != null)
        properties.put("J2EEServer", quote(j2eeServer.getName()));
    }

    String j2eeType;

    String className = getClass().getName();

    int lastDot = className.lastIndexOf('.');

    j2eeType = className.substring(lastDot + 1);

    properties.put("j2eeType", quote(j2eeType));

    String name = getName();

    if (name == null)
      name = "null";

    properties.put("name", quote(name));

    return new ObjectName("j2ee", properties);
  }

  /**
   * Returns a list of ObjectNames that match the specified keys and values.
   * The pattern does not need to include ",*", it is added automatically.
   */
  protected String[] queryObjectNamesNew(String ... pattern)
  {

    ArrayList<String> objectNames = new ArrayList<String>();

    queryObjectNames(objectNames, pattern);

    return objectNames.toArray(new String[objectNames.size()]);
  }

  /**
   * Returns a list of ObjectNames that match the specified keys and values.
   * The pattern does not need to include ",*", it is added automatically.
   */
  protected String[] queryObjectNames(String[][] patterns)
  {
    TreeSet<String> objectNames = new TreeSet<String>();

    for (String[] pattern : patterns) {
      queryObjectNames(objectNames, pattern);
    }

    return objectNames.toArray(new String[objectNames.size()]);
  }

  private void queryObjectNames(Collection<String> objectNames, String[] pattern)
  {
    try {
      StringBuilder patternBuilder = new StringBuilder();

      patternBuilder.append("j2ee:");

      int length = pattern.length;

      for (int i =- 0; i < length; i++) {
        String key = pattern[i];
        String value = pattern[++i];

        patternBuilder.append(key);
        patternBuilder.append('=');
        patternBuilder.append(quote(value));
      }

      for (String contextKey : CONTEXT_KEYS) {
        if (patternBuilder.indexOf(contextKey) >= 0)
          continue;

        String value = _objectName.getKeyProperty(contextKey);

        if (value != null) {
          patternBuilder.append(',');
          patternBuilder.append(contextKey);
          patternBuilder.append('=');
          patternBuilder.append(quote(value));
        }
      }

      patternBuilder.append(",*");

      ObjectName queryObjectName = new ObjectName(patternBuilder.toString());

      Set<ObjectName> matchingObjectNames
        = Jmx.getGlobalMBeanServer().queryNames(queryObjectName, null);

      for (ObjectName matchingObjectName : matchingObjectNames)
        objectNames.add(matchingObjectName.getCanonicalName());
    }
    catch (Exception ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, ex.toString(), ex);
    }
  }

  /**
   * Returns true if the state is manageable
   */
  public boolean isStateManageable()
  {
    return (this instanceof StateManageable);
  }

  /**
   * Returns true if the object provides statistics
   */
  public boolean isStatisticsProvider()
  {
    return (this instanceof StatisticsProvider);
  }

  /**
   * Returns true if the object provides events
   */
  public boolean isEventProvider()
  {
    return (this instanceof EventProvider);
  }

  long getStartTime()
  {
    return _startTime;
  }

  /**
   * Register a {@link J2EEManagedObject}.
   * This method never throws an exception, any {@link Throwable} is caught
   * and logged.
   *
   * @return the managed object if it is registered, null if there is an error.
   */
  public static <T extends J2EEManagedObject> T register(T managedObject)
  {
    if (managedObject == null)
      return null;

    ObjectName objectName = null;

    try {
      objectName = managedObject.createObjectName();

      Object mbean = new IntrospectionMBean(managedObject, managedObject.getClass(), true);

      Jmx.register(mbean, objectName);

      return managedObject;
    }
    catch (Exception ex) {
      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, managedObject.getClass() + " " + objectName + " " + ex.toString(), ex);

      return null;
    }
  }

  /**
   * Unregister a {@link J2EEManagedObject}.
   * This method never throws an exception, any {@link Throwable} is caught
   * and logged.
   *
   * @param managedObject the managed object, can be null in which case
   * nothing is done.
   */
  public static void unregister(J2EEManagedObject managedObject)
  {
    if (managedObject == null)
      return;

    ObjectName objectName = null;

    try {
      objectName = managedObject.createObjectName();

      Jmx.unregister(objectName);
    }
    catch (Throwable ex) {
      if (log.isLoggable(Level.FINEST))
        log.log(Level.FINEST, managedObject.getClass() + " " + objectName + " " + ex.toString(), ex);
    }
  }
}
