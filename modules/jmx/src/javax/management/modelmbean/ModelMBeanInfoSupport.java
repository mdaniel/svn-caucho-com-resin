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

package javax.management.modelmbean;

import javax.management.Descriptor;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;

import javax.management.RuntimeOperationsException;
import javax.management.InstanceNotFoundException;

/**
 * Implemented for each model MBean.
 */
public class ModelMBeanInfoSupport extends MBeanInfo
  implements ModelMBeanInfo {
  private Descriptor descriptor;
  
  /**
   * Clone constructor.
   */
  public ModelMBeanInfoSupport(ModelMBeanInfo info)
  {
    super(info.getClassName(), info.getDescription(),
          info.getAttributes(), info.getConstructors(),
          info.getOperations(), info.getNotifications());

    try {
      this.descriptor = info.getMBeanDescriptor();
    } catch (Exception e) {
    }
  }
  
  /**
   * constructor.
   */
  public ModelMBeanInfoSupport(String className,
                               String description,
                               ModelMBeanAttributeInfo []attributes,
                               ModelMBeanConstructorInfo []constructors,
                               ModelMBeanOperationInfo []operations,
                               ModelMBeanNotificationInfo []notifications)
  {
    super(className, description,
          attributes, constructors, operations, notifications);
  }
  
  /**
   * constructor.
   */
  public ModelMBeanInfoSupport(String className,
                               String description,
                               ModelMBeanAttributeInfo []attributes,
                               ModelMBeanConstructorInfo []constructors,
                               ModelMBeanOperationInfo []operations,
                               ModelMBeanNotificationInfo []notifications,
                               Descriptor descriptor)
  {
    super(className, description,
          attributes, constructors, operations, notifications);

    this.descriptor = descriptor;
  }
  
  /**
   * Returns descriptors for the mbean attributes and methods.
   */
  public Descriptor []getDescriptors(String descriptorType)
    throws MBeanException, RuntimeOperationsException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets descriptors for the mbean attributes and methods.
   */
  public void setDescriptors(Descriptor []descriptors)
    throws MBeanException, RuntimeOperationsException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Gets matching descriptor.
   *
   * @param name the name of the descriptor
   * @param type the type of the descriptor
   */
  public Descriptor getDescriptor(String name, String type)
    throws MBeanException, RuntimeOperationsException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Sets the descriptor for the type.
   *
   * @param descriptor the descriptor
   * @param type the type of the descriptor
   */
  public void setDescriptor(Descriptor descriptor, String type)
    throws MBeanException, RuntimeOperationsException
  {
    throw new UnsupportedOperationException();
  }
  
  /**
   * Returns the Descriptor for the entire bean.
   */
  public Descriptor getMBeanDescriptor()
    throws MBeanException, RuntimeOperationsException
  {
    return descriptor;
  }
  
  /**
   * Sets the Descriptor for the entire bean.
   */
  public void setMBeanDescriptor(Descriptor descriptor)
    throws MBeanException, RuntimeOperationsException
  {
    this.descriptor = descriptor;
  }
  
  /**
   * Gets the information for the named attribute.
   *
   * @param name the name of the attribute
   */
  public ModelMBeanAttributeInfo getAttribute(String name)
    throws MBeanException, RuntimeOperationsException
  {
    ModelMBeanAttributeInfo []attributes;
    attributes = (ModelMBeanAttributeInfo []) getAttributes();

    for (int i = 0; i < attributes.length; i++) {
      if (attributes[i].getName().equals(name))
	return attributes[i];
    }

    return null;
  }
  
  /**
   * Gets the information for the named operation.
   *
   * @param name the name of the operation
   */
  public ModelMBeanOperationInfo getOperation(String name)
    throws MBeanException, RuntimeOperationsException
  {
    ModelMBeanOperationInfo []ops;
    ops = (ModelMBeanOperationInfo []) getOperations();

    for (int i = 0; i < ops.length; i++) {
      if (ops[i].getName().equals(name))
	return ops[i];
    }

    return null;
  }
  
  /**
   * Gets the information for the named notification
   *
   * @param name the name of the notification
   */
  public ModelMBeanNotificationInfo getNotification(String name)
    throws MBeanException, RuntimeOperationsException
  {
    ModelMBeanNotificationInfo []notifs;
    notifs = (ModelMBeanNotificationInfo []) getNotifications();

    for (int i = 0; i < notifs.length; i++) {
      if (notifs[i].getName().equals(name))
	return notifs[i];
    }

    return null;
  }
  
  /**
   * Clones the info.
   */
  public Object clone()
  {
    return new ModelMBeanInfoSupport(this);
  }
}

  

