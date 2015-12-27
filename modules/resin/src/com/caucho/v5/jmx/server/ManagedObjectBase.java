/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Baratine(TM)(TM)
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Baratine is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Baratine is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Baratine; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.v5.jmx.server;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.management.MXBean;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.caucho.v5.jmx.Description;
import com.caucho.v5.jmx.JmxUtilResin;

/**
 * Parent mbean of all managed objects.
 */
@MXBean(false)
abstract public class ManagedObjectBase implements ManagedObjectMXBean {
  private static final Logger log
    = Logger.getLogger(ManagedObjectBase.class.getName());
  
  private transient ClassLoader _classLoader;
  private ObjectName _objectName;

  protected ManagedObjectBase()
  {
    this(Thread.currentThread().getContextClassLoader());
  }

  protected ManagedObjectBase(ClassLoader loader)
  {
    _classLoader = loader;
  }
  
  /**
   * Returns the {@link ObjectName} of the mbean.
   */
  @Description("The JMX ObjectName for the MBean")
  public ObjectName getObjectName()
  {
    if (_objectName == null) {

      try {
        Map<String,String> props = JmxUtilResin.copyContextProperties(_classLoader);

        String type = getType();
        if (type != null) {
          props.put("type", getType());
        }

        String name = getName();
        if (name != null) {
          if (name.indexOf(':') >= 0)
            name = ObjectName.quote(name);

          props.put("name", name);
        }

        addObjectNameProperties(props);

        _objectName = JmxUtilResin.getObjectName(JmxUtilResin.DOMAIN, props);
      } catch (MalformedObjectNameException e) {
        throw new RuntimeException(e);
      }
    }

    return _objectName;
  }

  protected void addObjectNameProperties(Map<String,String> props)
    throws MalformedObjectNameException
  {
  }

  /**
   * The JMX name property of the mbean.
   */
  @Override
  abstract public String getName();

  /**
   * The JMX type of this MBean, defaults to the prefix of the FooMXBean..
   */
  @Override
  public String getType()
  {
    Class<?> []interfaces = getClass().getInterfaces();

    for (int i = 0; i < interfaces.length; i++) {
      String className = interfaces[i].getName();
      
      if (className.endsWith("MXBean")) {
        int p = className.lastIndexOf('.');
        int q = className.indexOf("MXBean");

        return className.substring(p + 1, q);
      }
    }

    int p = getClass().getName().lastIndexOf('.');

    return getClass().getName().substring(p + 1);
  }

  /**
   * Registers the object with JMX.
   */
  protected boolean registerSelf()
  {
    try {
      JmxUtilResin.register(this, getObjectName(), _classLoader);

      return true;
      /*
    } catch (RuntimeException e) {
      throw e;
      */
    } catch (Exception e) {
      // System.out.println(e);
      log.fine(e.toString());
      log.log(Level.FINEST, e.toString(), e);

      return false;
    }
  }

  /**
   * Unregisters the object with JMX.
   */
  protected boolean unregisterSelf()
  {
    try {
      JmxUtilResin.unregister(getObjectName(), _classLoader);

      return true;
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);

      return false;
    }
  }
  
  @Override
  public String toString()
  {
    return getClass().getSimpleName() + "[" + getObjectName() + "]";
  }
}
