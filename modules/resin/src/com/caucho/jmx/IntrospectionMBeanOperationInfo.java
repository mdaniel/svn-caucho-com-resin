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
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanParameterInfo;
import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

public class IntrospectionMBeanOperationInfo
  extends MBeanOperationInfo
  implements Comparable<IntrospectionMBeanOperationInfo>
{
  public IntrospectionMBeanOperationInfo(Method method)
    throws IllegalArgumentException
  {
    super(method.getName(),
          createDescription(method),
          createSignature(method),
          createType(method),
          createImpact(method));
  }

  @SuppressWarnings({"unchecked"})
  private static String createDescription(Method method)
  {
    MBeanOperation ann = (MBeanOperation) method.getAnnotation(MBeanOperation.class);

    if (ann != null)
      return ann.description();
    else
      return "";
  }

  @SuppressWarnings({"unchecked"})
  private static MBeanParameterInfo[] createSignature(Method method)
  {
    Class[] types = method.getParameterTypes();
    Annotation[][] anns = method.getParameterAnnotations();
    MBeanParameterInfo[] parameters = new MBeanParameterInfo[types.length];

    for (int i = 0; i < types.length; i++) {
      String parameterName;
      String description;

      MBeanParameter ann = null;

      for (Annotation a : anns[i]) {
        if (a instanceof MBeanParameter) {
          ann = (MBeanParameter) a;
          break;
        }
      }

      if (ann != null) {
        parameterName = ann.name();
        description = ann.description();
      }
      else {
        parameterName = "p" + (i + 1);
        description = "";
      }

      parameters[i] = new MBeanParameterInfo(parameterName, types[i].getName(), description);
    }


    return parameters;
  }

  private static String createType(Method method)
  {
    return method.getReturnType().getName();
  }

  @SuppressWarnings({"unchecked"})
  private static int createImpact(Method method)
  {
    MBeanOperation ann = (MBeanOperation) method.getAnnotation(MBeanOperation.class);

    if (ann != null)
      return ann.impact();
    else
      return MBeanOperationInfo.UNKNOWN;
  }

  public int compareTo(IntrospectionMBeanOperationInfo o)
  {
    return getName().compareTo(o.getName());
  }
}
