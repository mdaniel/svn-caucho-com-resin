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

import java.util.Hashtable;
import java.util.Set;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.server.host.Host;
import com.caucho.server.webapp.Application;

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

  private final Lifecycle _lifecycle = new Lifecycle();

  protected ObjectName _objectName;

  public J2EEManagedObject()
  {
  }

  /**
   * Returns the value to use for the the `name' key of the
   * ObjectName.
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

  /**
   * Format a String for use as a JMX value, escaping any special characters.
   * @param value The value to format
   * @param deflt A default to return if the value is null or the empty string.
   * @return
   */
  protected String escapeForObjectName(String value, String deflt)
  {
    if (value == null || value.length() == 0)
      return deflt;

    // XXX: s/b ObjectName.quote
    return value.replace(':', '-');
  }

  /**
   * Creates the object name.
   */
  protected ObjectName createObjectName(Hashtable<String,String> properties)
    throws MalformedObjectNameException
  {
    // reverse order of what appears in ObjectName

    Application application = Application.getLocal();

    if (application != null)
      properties.put("WebModule", escapeForObjectName(application.getContextPath(), "/"));

    Host host = Host.getLocal();

    if (isJ2EEApplication()) {
      J2EEApplication j2eeApplication = J2EEApplication.getLocal();

      if (j2eeApplication == null)
        properties.put("J2EEApplication", "null");
      else
        properties.put("J2EEApplication", j2eeApplication.getName());
    }

    if (host != null)
      properties.put("Host", escapeForObjectName(host.getName(), "default"));

    if (isJ2EEServer()) {
      J2EEServer j2eeServer = J2EEServer.getLocal();

      if (j2eeServer != null)
        properties.put("J2EEServer", j2eeServer.getName());
    }

    String j2eeType;

    String className = getClass().getName();

    int lastDot = className.lastIndexOf('.');

    j2eeType = className.substring(lastDot + 1);

    properties.put("j2eeType", j2eeType);

    properties.put("name", escapeForObjectName(getName(), null));

    return new ObjectName("j2ee", properties);
  }

  /**
   * Returns a list of ObjectNames that match the specified pattern(s).
   * The context of this mbean is added to the patterns, which means that if
   * there is a J2EEServer, Host, J2EEApplication, and/or WebModule for this mbean their
   * key/value pairs are added to the pattern.
   * The pattern does not need to include ",*", it is added automatically.
   */
  protected String[] queryObjectNames(String ... patterns)
  {
    TreeSet<String> objectNames = new TreeSet<String>();

    for (String pattern : patterns) {
      StringBuilder patternBuilder = new StringBuilder();

      if (pattern.indexOf(':') < 0)
        patternBuilder.append("j2ee:");

      for (String contextKey : CONTEXT_KEYS) {
        if (pattern.indexOf(contextKey) >= 0)
          continue;

        String value = _objectName.getKeyProperty(contextKey);

        if (value != null) {
          patternBuilder.append(',');
          patternBuilder.append(contextKey);
          patternBuilder.append('=');
          patternBuilder.append(value);
        }
      }

      patternBuilder.append(",*");

      try {
        ObjectName queryObjectName = new ObjectName(patternBuilder.toString());

        Set<ObjectName> matchingObjectNames
          = Jmx.getGlobalMBeanServer().queryNames(queryObjectName, null);

        for (ObjectName matchingObjectName : matchingObjectNames)
          objectNames.add(matchingObjectName.toString());
      }
      catch (MalformedObjectNameException ex) {
        if (log.isLoggable(Level.FINE))
          log.log(Level.FINE, ex.toString(), ex);
      }
    }

    return objectNames.toArray(new String[objectNames.size()]);
  }

  void start()
  {
    if (!_lifecycle.toStarting())
      return;

    if (_objectName == null) {
      Hashtable<String,String> properties = new Hashtable<String, String>();

      try {
        _objectName = createObjectName(properties);
      }
      catch (MalformedObjectNameException ex) {
        _lifecycle.toError();

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

    try {
      Object mbean = new IntrospectionMBean(this, getClass(), true);

      Jmx.getGlobalMBeanServer().registerMBean(mbean, _objectName);
    }
    catch (Exception ex) {
      _lifecycle.toError();

      if (log.isLoggable(Level.FINE))
        log.log(Level.FINE, _objectName.toString() + " " + ex.toString(), ex);

      return;
    }

    _lifecycle.toActive();
  }

  boolean isActive()
  {
    return _lifecycle.isActive();
  }

  void stop()
  {
    if (!_lifecycle.toStopping())
      return;

    if (_objectName != null) {
      try {
        Jmx.getGlobalMBeanServer().unregisterMBean(_objectName);
      }
      catch (Exception ex) {
        _lifecycle.toError();

        if (log.isLoggable(Level.FINER))
          log.log(Level.FINER, _objectName.toString() + " " + ex.toString(), ex);
      }
    }

    _lifecycle.toStop();
  }

  public String getObjectName()
  {
    return _objectName.toString();
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
}
