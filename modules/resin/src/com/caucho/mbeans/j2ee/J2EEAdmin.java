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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Sam
 */


package com.caucho.mbeans.j2ee;

import com.caucho.jmx.IntrospectionMBean;
import com.caucho.jmx.Jmx;

import javax.management.ObjectName;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility class for registering and unregistering J2EEManagedObject
 * management beans.
 */
public class J2EEAdmin {
  private static final Logger log = Logger.getLogger(J2EEAdmin.class.getName());

  private J2EEAdmin()
  {
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

  public static void unregister(String name)
  {
    ObjectName objectName;

    try {
      objectName = new ObjectName(name);

      Jmx.unregister(objectName);
    }
    catch (Throwable ex) {
      if (log.isLoggable(Level.FINER))
        log.log(Level.FINER, name + " " + ex.toString(), ex);
    }
  }
}
