/*
 * Copyright (c) 1998-2005 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.jmx;

import javax.management.MBeanInfo;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;

public class IntrospectionMBeanInfo
  extends MBeanInfo
{
  private static final Class _descriptionAnn;
  
  public IntrospectionMBeanInfo(Class cl,
                                MBeanAttributeInfo[] attributes,
                                MBeanConstructorInfo[] constructors,
                                MBeanOperationInfo[] operations,
                                MBeanNotificationInfo[] notifications)
    throws IllegalArgumentException
  {
    super(cl.getName(),
          createDescription(cl),
          attributes,
          constructors,
          operations,
          notifications);
  }

  private static String createDescription(Class cl)
  {
    String description = getDescription(cl);

    if (! "".equals(description))
      return description;
    else
      return "Introspected " + cl.getName() + " MBean";
  }

  /**
   * Returns the method's description.
   */
  private static String getDescription(Class cl)
  {
    try {
      Description desc = (Description) cl.getAnnotation(_descriptionAnn);

      if (desc != null)
	return desc.value();
      else
	return "";
    } catch (Throwable e) {
      return "";
    }
  }

  private static Class findClass(String name)
  {
    try {
      return Class.forName(name);
    } catch (Throwable e) {
      return null;
    }
  }

  static {
    _descriptionAnn = findClass("com.caucho.jmx.Description");
  }
}
