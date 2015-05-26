/*
 * Copyright (c) 1998-2015 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R)
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
 */

package com.caucho.admin.action;

import java.util.*;

import javax.management.*;

public class AbstractJmxAction implements AdminAction
{
  private static Map<String, Class<?>> classMap = 
    new HashMap<String, Class<?>>();
  
  static {
    classMap.put("boolean", boolean.class);
    classMap.put(Boolean.class.getName(), Boolean.class);
    classMap.put("byte", byte.class);
    classMap.put(Byte.class.getName(), Byte.class);
    classMap.put("short", Short.class);
    classMap.put(Short.class.getName(), Short.class);
    classMap.put("int", int.class);
    classMap.put(Integer.class.getName(), Integer.class);
    classMap.put("long", long.class);
    classMap.put(Long.class.getName(), Long.class);
    classMap.put("float", long.class);
    classMap.put(Float.class.getName(), Float.class);
    classMap.put("double", long.class);
    classMap.put(Double.class.getName(), Double.class);
    classMap.put(String.class.getName(), String.class);
  }
  
  protected static void sort(List<MBeanOperationInfo> operations)
  {
    Collections.sort(operations, new Comparator<MBeanOperationInfo>()
    {
      @Override
      public int compare(MBeanOperationInfo o1, MBeanOperationInfo o2)
      {
        String signature1 = getSignature(o1);
        String signature2 = getSignature(o2);

        return signature1.compareTo(signature2);
      }
    });
  }

  protected static String getSignature(MBeanOperationInfo operation)
  {
    StringBuilder builder = new StringBuilder();

    builder.append(operation.getName()).append('(');

    MBeanParameterInfo []params = operation.getSignature();

    for (int i = 0; i < params.length; i++) {
      MBeanParameterInfo param = params[i];
      builder.append(param.getType());

      if (i + 1 < params.length)
        builder.append(", ");
    }

    builder.append(')');

    return builder.toString();
  }
  
  protected static Object toValue(String typeName, String value)
    throws ClassNotFoundException
  {
    Class type = classMap.get(typeName);
    
    if (type == null)
      type = Class.forName(typeName);
    
    if (boolean.class.equals(type) || Boolean.class.equals(type)) {
      return Boolean.parseBoolean(value);
    }
    else if (byte.class.equals(type) || Byte.class.equals(type)) {
      return Byte.parseByte(value);
    }
    else if (short.class.equals(type) || Short.class.equals(type)) {
      return Short.parseShort(value);
    }
    else if (char.class.equals(type) || Character.class.equals(type)) {
      return new Character((char) Integer.parseInt(value));
    }
    else if (int.class.equals(type) || Integer.class.equals(type)) {
      return Integer.parseInt(value);
    }
    else if (long.class.equals(type) || Long.class.equals(type)) {
      return Long.parseLong(value);
    }
    else if (float.class.equals(type) || Float.class.equals(type)) {
      return Float.parseFloat(value);
    }
    else if (double.class.equals(type) || Double.class.equals(type)) {
      return Double.parseDouble(value);
    }
    else if (type.isEnum()) {
      return Enum.valueOf(type, value);
    }
    else {
      return value;
    }
  }
}
