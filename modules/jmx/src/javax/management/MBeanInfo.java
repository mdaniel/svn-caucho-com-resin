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

package javax.management;

/**
 * Introspected information about an MBean.
 */
public class MBeanInfo implements Cloneable, java.io.Serializable {
  private String className;
  private String description;
  private MBeanAttributeInfo []attributes;
  private MBeanConstructorInfo []constructors;
  private MBeanOperationInfo []operations;
  private MBeanNotificationInfo []notifications;
  
  /**
   * Constructor.
   *
   * @param className the name of the MBean
   * @param description a description of the MBean
   * @param attributes attributes of the MBean
   * @param constructors constructors for the MBean
   * @param operations methods for the MBean
   * @param notifications notifications for the MBean
   */
  public MBeanInfo(String className,
                   String description,
                   MBeanAttributeInfo []attributes,
                   MBeanConstructorInfo []constructors,
                   MBeanOperationInfo []operations,
                   MBeanNotificationInfo []notifications)
  {
    this.className = className;
    this.description = description;
    this.attributes = attributes;
    this.constructors = constructors;
    this.operations = operations;
    this.notifications = notifications;
  }

  /**
   * Returns the class name of the MBean.
   */
  public String getClassName()
  {
    return className;
  }

  /**
   * Returns a description of the MBean.
   */
  public String getDescription()
  {
    return description;
  }

  /**
   * Returns the MBean's attributes.
   */
  public MBeanAttributeInfo []getAttributes()
  {
    return attributes;
  }

  /**
   * Returns the MBean's operations.
   */
  public MBeanOperationInfo []getOperations()
  {
    return operations;
  }

  /**
   * Returns the MBean's constructors.
   */
  public MBeanConstructorInfo []getConstructors()
  {
    return constructors;
  }

  /**
   * Returns the MBean's notifications.
   */
  public MBeanNotificationInfo []getNotifications()
  {
    return notifications;
  }

  /**
   * Clones the info
   */
  public Object clone()
  {
    return this;
  }
}
