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

import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;
import com.caucho.lifecycle.Lifecycle;
import com.caucho.server.host.Host;
import com.caucho.server.webapp.Application;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Hashtable;
import java.util.Set;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Base class management interface for all managed objects.
 */
abstract public class J2EEManagedObject {
  private static final Logger log = Logger.getLogger(J2EEManagedObject.class.getName());

  private final Lifecycle _lifecycle = new Lifecycle();

  private ObjectName _objectName;

  /**
   * Returns the value to use for the `J2EEType' key of the ObjectName.
   * Default is to return the unqualified classname.
   */
  protected String getJ2EEType()
  {
    String className = getClass().getName();

    int lastDot = className.lastIndexOf('.');

    return className.substring(lastDot + 1);
  }

  /**
   * Returns the value to use for the last portion of the `name' key of the
   * ObjectName. The first portion of the name key is composed of the name of the
   * enclosing host and application.
   */
  abstract protected String getName();

  /**
   * Returns the value to use for the `J2EEApplication' key of the ObjectName,
   * null if the ObjectName should not have a J2EEApplication key.
   * Default is to return the name of the EnterpriseApplication, or
   * the string "null".
   */
  protected String getJ2EEApplication()
  {
    // XXX: EnterpriseApplication
    return "null";
  }

  /**
   * Creates the object name.
   */
  protected ObjectName createObjectName(Hashtable<String,String> properties)
    throws MalformedObjectNameException
  {
    properties.put("j2eeType", getJ2EEType());

    Host host = Host.getLocal();

    if (host != null) {
      StringBuilder name = new StringBuilder();
      name.append(host.getName());

      Application application = Application.getLocal();

      if (application != null) {
        name.append(' ');
        name.append(application.getContextPath());
      }

      name.append(' ');
      name.append(getName());

      properties.put("name", name.toString());
    }
    else {
      properties.put("name", getName());
    }

    String j2eeApplication = getJ2EEApplication();

    if (j2eeApplication != null)
      properties.put("J2EEApplication", j2eeApplication);

    /**
     XXX:
    for (Map.Entry<String, String> entry : properties.entrySet()) {
      entry.setValue(ObjectName.quote(entry.getValue()));
    }
     */

    return new ObjectName("j2ee", properties);
  }

  @SuppressWarnings("unchecked")
  protected String[] queryObjectNames(String ... patterns)
  {
    String[] objectNames = null;
    int objectNamesIndex = 0;

    for (String pattern : patterns) {
      if (pattern.indexOf(':') < 0)
        pattern = "j2ee:" + pattern;

      try {
        Set<ObjectName> objectNamesSet
          = Jmx.getGlobalMBeanServer().queryNames(new ObjectName(pattern), null);

        int size = objectNamesSet.size();

        if (size > 0) {
          if (objectNames == null)
            objectNames = new String[size];
          else {
            String[] newObjectNames = new String[objectNames.length + size];
            System.arraycopy(objectNames,  0, newObjectNames, 0, objectNames.length);
            objectNames = newObjectNames;
          }

          for (ObjectName objectName : objectNamesSet)
            objectNames[objectNamesIndex++] = objectName.toString();

        }
      }
      catch (MalformedObjectNameException ex) {
        if (log.isLoggable(Level.WARNING))
          log.log(Level.WARNING, ex.toString(), ex);
      }
    }

    return objectNames == null ? new String[] {} : objectNames;
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

        if (log.isLoggable(Level.WARNING)) {
          StringBuilder builder = new StringBuilder();

          builder.append('`');
          for (Map.Entry<String,String> entry : properties.entrySet()) {
            if (builder.length() > 0)
              builder.append(',');

            builder.append(entry.getKey());
            builder.append('=');
            builder.append(entry.getValue());
          }

          builder.append("' ");
          builder.append(ex.toString());

          log.log(Level.WARNING, builder.toString(), ex);
        }

        return;
      }
    }

    try {
      Object mbean = new IntrospectionMBean(this, getClass(), true);

      Jmx.getGlobalMBeanServer().registerMBean(mbean, _objectName);
    }
    catch (Exception ex) {
      _lifecycle.toError();

      if (log.isLoggable(Level.WARNING))
        log.log(Level.WARNING, _objectName.toString() + " " + ex.toString(), ex);

      return;
    }

    _lifecycle.toActive();
  }

  void stop()
  {
    if (!_lifecycle.toStopping())
      return;

    try {
      Jmx.getGlobalMBeanServer().unregisterMBean(_objectName);
    }
    catch (Exception ex) {
      _lifecycle.toError();

      if (log.isLoggable(Level.WARNING))
        log.log(Level.WARNING, _objectName.toString() + " " + ex.toString(), ex);
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
