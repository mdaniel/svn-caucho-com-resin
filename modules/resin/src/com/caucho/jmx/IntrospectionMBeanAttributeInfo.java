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

import javax.management.MBeanAttributeInfo;
import javax.management.IntrospectionException;
import java.lang.reflect.Method;

public class IntrospectionMBeanAttributeInfo
  extends MBeanAttributeInfo
  implements Comparable<IntrospectionMBeanAttributeInfo>
{
  private final MBeanAttributeCategory _category;

  public IntrospectionMBeanAttributeInfo(Method method,
                                         String name,
                                         String type,
                                         boolean isReadable,
                                         boolean isWritable,
                                         boolean isIs)
    throws IllegalArgumentException
  {
    super(name, type, createDescription(method, null), isReadable, isWritable, isIs);
    _category = createCategory(method, null);
  }

  public IntrospectionMBeanAttributeInfo(String name,
                                         Method getter,
                                         Method setter)
    throws IntrospectionException
  {
    super(name, createDescription(getter, setter), getter, setter);
    _category = createCategory(getter, setter);
  }

  @SuppressWarnings({"unchecked"})
  private static String createDescription(Method getter, Method setter)
  {
    String description = null;

    if (getter != null) {
      MBeanAttribute ann = getter.getAnnotation(MBeanAttribute.class);

      if (ann != null)
        description = ann.description();
    }

    if (description == null && setter != null) {
      MBeanAttribute ann = setter.getAnnotation(MBeanAttribute.class);

      if (ann != null)
        description = ann.description();
    }

    if (description == null)
      description = "";

    return description;
  }

  private static MBeanAttributeCategory createCategory(Method getter, Method setter)
  {
    MBeanAttributeCategory category = null;

    if (getter != null) {
      MBeanAttribute ann = (MBeanAttribute) getter.getAnnotation(MBeanAttribute.class);

      if (ann != null)
        category = ann.category();
    }

    if (category == null && setter != null) {
      MBeanAttribute ann = (MBeanAttribute) setter.getAnnotation(MBeanAttribute.class);

      if (ann != null)
        category = ann.category();
    }

    if (category == null)
      category = MBeanAttributeCategory.CONFIGURATION;

    return category;
  }

  public MBeanAttributeCategory getCategory()
  {
    return _category;
  }

  public int compareTo(IntrospectionMBeanAttributeInfo o)
  {
    if (o._category != _category) {
      switch (_category) {
        case CONFIGURATION:
          return -1;
        case STATISTIC:
          return 1;
        default:
          throw new AssertionError(_category);
      }
    }

    String name = getName();
    String oName = o.getName();

    // if the point of difference occurs at the word `Lifetime',
    // then then one with `Lifetime' comes after
    for (int i = 0; i < name.length() && i < oName.length(); i++) {
      char ch = name.charAt(i);
      char oCh = oName.charAt(i);

      if (ch != oCh) {
        if (name.regionMatches(i, "Lifetime", 0, 8))
          return 1;
        else if (oName.regionMatches(i, "Lifetime", 0, 8))
          return -1;
        else
          return ch < oCh ? -1 : 1;
      }
    }

    if (name.length() == oName.length())
      return 0;
    else if (name.length() < oName.length())
      return -1;
    else
      return 1;
  }
}
